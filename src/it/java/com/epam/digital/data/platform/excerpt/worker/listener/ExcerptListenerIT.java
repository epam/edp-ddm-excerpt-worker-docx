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

package com.epam.digital.data.platform.excerpt.worker.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.excerpt.model.ExcerptEventDto;
import com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus;
import com.epam.digital.data.platform.excerpt.model.Request;
import com.epam.digital.data.platform.excerpt.worker.BaseIT;
import com.epam.digital.data.platform.excerpt.worker.TemplateCephServiceMockHelper;
import com.epam.digital.data.platform.excerpt.worker.TestUtils;
import com.epam.digital.data.platform.excerpt.worker.util.ZipUtil;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

class ExcerptListenerIT extends BaseIT {

  private static final String EXCERPT_TYPE = "test-report";
  private static final String FOLDER_PATH = "docx/" + EXCERPT_TYPE;
  @Value("${excerpt-templates-ceph.bucket}")
  String bucket;
  @Value("${template.path}")
  String templatePath;
  private static final String TEMPLATE_DOCX = "/template.docx";

  private static final TemplateCephServiceMockHelper helper = new TemplateCephServiceMockHelper();

  @Value("${template.path}")
  String templateFolderPath;

  @Autowired
  ExcerptListener excerptListener;

  @Captor
  ArgumentCaptor<Map<String, byte[]>> captor;

  @MockBean(name = "excerptFileCephService")
  CephService excerptFileCephService;
  @MockBean(name = "excerptTemplatesCephService")
  CephService excerptTemplatesCephService;
  @MockBean
  ZipUtil zipUtil;


  @Test
  void shouldCreateExcerpt() throws IOException {
    // given
    helper.customizeMock(excerptTemplatesCephService, bucket, TEMPLATE_DOCX, FOLDER_PATH);
    when(zipUtil.packInZip(any())).thenReturn(new byte[]{});

    saveExcerptTemplateToDatabase(EXCERPT_TYPE, FOLDER_PATH);

    var requestJson = TestUtils.readClassPathResource("/json/request.json");
    var excerptEventDto = new ObjectMapper().readValue(requestJson, ExcerptEventDto.class);
    var excerptRecord = saveExcerptRecordToDatabase(excerptEventDto);

    excerptEventDto.setRecordId(excerptRecord.getId());

    // when
    excerptListener.generate(new Request<>(excerptEventDto));

    // then
    verify(zipUtil).packInZip(captor.capture());
    var resultDocumentXml = captor.getValue().get(templatePath);
    var expectedDocumentXml = helper.getFilesWithPathInFolder("/expected.docx").get(templatePath);
    var status = excerptRecordRepository.findById(excerptEventDto.getRecordId()).get().getStatus();

    assertThat(resultDocumentXml).isEqualTo(expectedDocumentXml);
    assertThat(status).isEqualTo(ExcerptProcessingStatus.COMPLETED);
  }

  @Test
  void failWhenTemplateNotFoundInDatabase() throws IOException {
    // given
    var requestJson = TestUtils.readClassPathResource("/json/request.json");
    var excerptEventDto = new ObjectMapper().readValue(requestJson, ExcerptEventDto.class);
    var excerptRecord = saveExcerptRecordToDatabase(excerptEventDto);

    excerptEventDto.setRecordId(excerptRecord.getId());

    // when
    excerptListener.generate(new Request<>(excerptEventDto));

    // then
    verify(excerptFileCephService, times(0)).put(any(), any(), any(), any(), any());

    var resultRecord = excerptRecordRepository.findById(excerptEventDto.getRecordId()).get();

    assertThat(resultRecord.getStatus()).isEqualTo(ExcerptProcessingStatus.FAILED);
    assertThat(resultRecord.getStatusDetails()).isEqualTo(
        "Excerpt template 'test-report' not found");
  }

  @Test
  void shouldFailWhenTemplateFolderNotFound() throws IOException {
    // given
    helper.customizeMock(excerptTemplatesCephService, bucket, TEMPLATE_DOCX, FOLDER_PATH);

    saveExcerptTemplateToDatabase(EXCERPT_TYPE, FOLDER_PATH + "_");

    var requestJson = TestUtils.readClassPathResource("/json/request.json");
    var excerptEventDto = new ObjectMapper().readValue(requestJson, ExcerptEventDto.class);
    var excerptRecord = saveExcerptRecordToDatabase(excerptEventDto);

    excerptEventDto.setRecordId(excerptRecord.getId());

    // when
    excerptListener.generate(new Request<>(excerptEventDto));

    // then
    verify(zipUtil, times(0)).packInZip(any());
    var result = excerptRecordRepository.findById(excerptEventDto.getRecordId()).get();

    assertThat(result.getStatus()).isEqualTo(ExcerptProcessingStatus.FAILED);
    assertThat(result.getStatusDetails()).isEqualTo("Excerpt template folder not found");
  }

  @Test
  void shouldFailWhenDocumentXmlFromTemplateFolderNotFound() throws IOException {
    // given
    helper.customizeMock(excerptTemplatesCephService, bucket, TEMPLATE_DOCX, FOLDER_PATH);
    when(zipUtil.packInZip(any())).thenReturn(new byte[]{});
    when(excerptTemplatesCephService.get(bucket,
        FOLDER_PATH + "/" + templatePath)).thenReturn(Optional.empty());

    saveExcerptTemplateToDatabase(EXCERPT_TYPE, FOLDER_PATH);

    var requestJson = TestUtils.readClassPathResource("/json/request.json");
    var excerptEventDto = new ObjectMapper().readValue(requestJson, ExcerptEventDto.class);
    var excerptRecord = saveExcerptRecordToDatabase(excerptEventDto);

    excerptEventDto.setRecordId(excerptRecord.getId());

    // when
    excerptListener.generate(new Request<>(excerptEventDto));

    // then
    verify(zipUtil, times(0)).packInZip(any());
    var result = excerptRecordRepository.findById(excerptEventDto.getRecordId()).get();

    assertThat(result.getStatus()).isEqualTo(ExcerptProcessingStatus.FAILED);
    assertThat(result.getStatusDetails()).isEqualTo("File not found");
  }
}
