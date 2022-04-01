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
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.excerpt.dao.ExcerptTemplate;
import com.epam.digital.data.platform.excerpt.worker.exception.ExcerptProcessingException;
import com.epam.digital.data.platform.excerpt.worker.repository.ExcerptTemplateRepository;
import com.epam.digital.data.platform.integration.ceph.model.CephObject;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

  private TemplateService templateService;
  private ExcerptTemplate excerptTemplate;

  @Mock
  ExcerptTemplateRepository templateRepository;
  @Mock
  CephService cephService;

  private static final String FOLDER_PATH = "docx/test-excerpt";
  private static final String TEMPLATE_PATH_IN_FOLDER = "word/document.xml";
  private static final String BUCKET = "template-bucket";
  private static final String EXCERPT_TYPE = "test-report";

  @BeforeEach
  void init() {
    templateService = new TemplateService(
        templateRepository,
        cephService,
        BUCKET,
        TEMPLATE_PATH_IN_FOLDER);

    excerptTemplate = new ExcerptTemplate();
    excerptTemplate.setTemplate(FOLDER_PATH);
    lenient().when(templateRepository.findFirstByTemplateName(EXCERPT_TYPE))
        .thenReturn(Optional.of(excerptTemplate));
  }


  @Nested
  class GetFilesTest {

    String filePath1 = "file.xml";
    String filePath2 = "word/image.img";

    Map<String, byte[]> KEYS_IN_CEPH = Map.of(
        FOLDER_PATH + "/" + filePath1, "content1".getBytes(),
        FOLDER_PATH + "/" + filePath2, "content2".getBytes()
    );
    Map<String, byte[]> KEYS_IN_FOLDER = Map.of(
        filePath1, "content1".getBytes(),
        filePath2, "content2".getBytes()
    );

    @Test
    void shouldReturnMapOfFilePathAndFileContentFromCephBucketFoundByExcerptType() {
      when(cephService.getKeys(BUCKET, FOLDER_PATH)).thenReturn(KEYS_IN_CEPH.keySet());
      var mockCephObject1 = mock(CephObject.class);
      when(mockCephObject1.getContent()).thenReturn(
          new ByteArrayInputStream("content1".getBytes()));
      when(cephService.get(BUCKET, FOLDER_PATH + "/" + filePath1)).thenReturn(
          Optional.of(mockCephObject1));

      var mockCephObject2 = mock(CephObject.class);
      when(mockCephObject2.getContent()).thenReturn(
          new ByteArrayInputStream("content2".getBytes()));
      when(cephService.get(BUCKET, FOLDER_PATH + "/" + filePath2)).thenReturn(
          Optional.of(mockCephObject2));

      var result = templateService.getFiles(EXCERPT_TYPE);

      assertThat(result.keySet()).containsAll(KEYS_IN_FOLDER.keySet());
      assertThat(result.values()).containsAll(KEYS_IN_FOLDER.values());
    }

    @Test
    void shouldThrowExceptionWhenTemplateNotFound() {
      when(templateRepository.findFirstByTemplateName(EXCERPT_TYPE)).thenReturn(Optional.empty());

      var exception = assertThrows(ExcerptProcessingException.class,
          () -> templateService.getFiles(EXCERPT_TYPE));

      assertThat(exception.getStatus()).isEqualTo(FAILED);
      assertThat(exception.getDetails()).isEqualTo("Excerpt template 'test-report' not found");
    }

    @Test
    void shouldThrowExceptionWhenFileNotFoundInCeph() {
      when(cephService.getKeys(BUCKET, FOLDER_PATH)).thenReturn(KEYS_IN_CEPH.keySet());

      var exception = assertThrows(ExcerptProcessingException.class,
          () -> templateService.getFiles(EXCERPT_TYPE));

      assertThat(exception.getStatus()).isEqualTo(FAILED);
      assertThat(exception.getDetails()).isEqualTo("File not found");
    }
  }

  @Nested
  class GetTemplateTest {

    String filePath = "file.xml";

    @Test
    void shouldReturnContentOfFileWithKeyEqTemplatePath() {
      Map<String, byte[]> FILES = Map.of(
          filePath, "content1".getBytes(),
          TEMPLATE_PATH_IN_FOLDER, "content2".getBytes()
      );

      var templateStr = templateService.getTemplate(FILES);

      assertThat(templateStr).isEqualTo("content2");
    }

    @Test
    void shouldThrowExceptionWhenMapDoesNotContainKeyEqTemplatePath() {
      Map<String, byte[]> FILES = Map.of(filePath, "content1".getBytes());

      var exception = assertThrows(ExcerptProcessingException.class,
          () -> templateService.getTemplate(FILES));

      assertThat(exception.getStatus()).isEqualTo(FAILED);
      assertThat(exception.getDetails()).isEqualTo("Excerpt template not found");
    }
  }
}
