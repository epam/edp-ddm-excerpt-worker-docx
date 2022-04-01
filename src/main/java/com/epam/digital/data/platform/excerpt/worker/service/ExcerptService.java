/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.excerpt.worker.service;

import com.epam.digital.data.platform.excerpt.model.ExcerptEventDto;
import com.epam.digital.data.platform.excerpt.worker.exception.ExcerptProcessingException;
import com.epam.digital.data.platform.excerpt.worker.repository.ExcerptRecordRepositoryFacade;
import com.epam.digital.data.platform.excerpt.worker.util.ZipUtil;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ExcerptService {
  
  private final Logger log = LoggerFactory.getLogger(ExcerptService.class);

  private final ExcerptRecordRepositoryFacade recordFacade;
  private final TemplateService templateService;
  private final StorageService storageService;
  private final Renderer renderer;
  private final ZipUtil zipUtil;


  public ExcerptService(
      ExcerptRecordRepositoryFacade recordFacade,
      TemplateService templateService,
      StorageService storageService,
      Renderer renderer,
      ZipUtil zipUtil) {
    this.recordFacade = recordFacade;
    this.templateService = templateService;
    this.storageService = storageService;
    this.renderer = renderer;
    this.zipUtil = zipUtil;
  }

  public void generateExcerpt(ExcerptEventDto event) {
    try {
      Map<String, byte[]> files = templateService.getFiles(event.getExcerptType());

      log.info("Rendering template");
      var templateStr = templateService.getTemplate(files);
      var renderedTemplate = renderer.render(event.getExcerptType(), templateStr,
          event.getExcerptInputData());

      files.put(templateService.getTemplatePath(), renderedTemplate.getBytes(StandardCharsets.UTF_8));

      log.info("Archiving files in a zip archive");
      byte[] zipFile = zipUtil.packInZip(files);

      storageService.storeFile(event.getRecordId(), zipFile);
      log.info("Excerpt generated");
    } catch (ExcerptProcessingException e) {
      log.error("Can not generate excerpt. Status: {}. Details: {}", e.getStatus(), e.getDetails());
      recordFacade.updateRecord(event.getRecordId(), e);
    }
  }
}
