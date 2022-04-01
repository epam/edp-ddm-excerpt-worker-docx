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

import static com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus.FAILED;

import com.epam.digital.data.platform.excerpt.worker.exception.ExcerptProcessingException;
import com.epam.digital.data.platform.excerpt.worker.repository.ExcerptTemplateRepository;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TemplateService {

  private final ExcerptTemplateRepository templateRepository;
  private final CephService excerptTemplatesCephService;
  private final String templateBucket;
  private final String templatePath;

  public TemplateService(
      ExcerptTemplateRepository templateRepository,
      CephService excerptTemplatesCephService,
      @Value("${excerpt-templates-ceph.bucket}") String templateBucket,
      @Value("${template.path}") String templatePath) {
    this.templateRepository = templateRepository;
    this.excerptTemplatesCephService = excerptTemplatesCephService;
    this.templateBucket = templateBucket;
    this.templatePath = templatePath;
  }

  public Map<String, byte[]> getFiles(String excerptType) {
    String folderPath = getTemplateFolderPath(excerptType);
    var keys = excerptTemplatesCephService.getKeys(templateBucket, folderPath);
    if (keys.isEmpty()) {
      throw new ExcerptProcessingException(FAILED, "Excerpt template folder not found");
    }
    Map<String, byte[]> files = new HashMap<>();
    for (String key : keys) {
      var cephObject = excerptTemplatesCephService.get(templateBucket, key)
          .orElseThrow(() -> new ExcerptProcessingException(FAILED, "File not found"));
      key = key.substring(folderPath.length() + 1);
      try {
        files.put(key, cephObject.getContent().readAllBytes());
      } catch (IOException e) {
        throw new ExcerptProcessingException(FAILED, "Can't read file");
      }
    }
    return files;
  }

  public String getTemplate(Map<String, byte[]> files) {
    var template = files.get(templatePath);
    if (template == null) {
      throw new ExcerptProcessingException(FAILED, "Excerpt template not found");
    }
    return new String(template, StandardCharsets.UTF_8);
  }

  private String getTemplateFolderPath(String excerptType) {
    var excerptTemplate = templateRepository
        .findFirstByTemplateName(excerptType)
        .orElseThrow(() -> new ExcerptProcessingException(FAILED,
            "Excerpt template '" + excerptType + "' not found"));
    return excerptTemplate.getTemplate();
  }

  public String getTemplatePath() {
    return templatePath;
  }
}
