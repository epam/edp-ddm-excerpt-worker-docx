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

import com.epam.digital.data.platform.excerpt.worker.config.FreeMarkerConfiguration;
import com.epam.digital.data.platform.excerpt.worker.config.GenericConfig;
import com.epam.digital.data.platform.excerpt.worker.exception.ExcerptProcessingException;
import freemarker.template.Configuration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@Import({FreeMarkerConfiguration.class, GenericConfig.class})
class FreemarkerRendererTest {

  @Autowired
  private Configuration freemarker;

  private FreemarkerRenderer freemarkerRenderer;
  private static final String templateStr = "My name is [=name]";
  private static final Map<String, String> data = Map.of("name", "Alex");

  @BeforeEach
  void init() {
    freemarkerRenderer = new FreemarkerRenderer(freemarker);
  }

  @Test
  void templateRenderingHappyPath() {
    var result = freemarkerRenderer.render("Test", templateStr, data);

    assertThat(result).isEqualTo("My name is Alex");
  }

  @Test
  void shouldThrowExceptionWithTemplateConversionError() {
    var exception = assertThrows(ExcerptProcessingException.class,
        () -> freemarkerRenderer.render("Test", templateStr, "{}"));

    assertThat(exception.getStatus()).isEqualTo(FAILED);
  }

  @Test
  void shouldConvertTemplateExceptionToExcerptProcessingException() {
    var exception = assertThrows(ExcerptProcessingException.class,
        () -> freemarkerRenderer.render("Test", templateStr, Map.of(5, "Alex")));

    assertThat(exception.getStatus()).isEqualTo(FAILED);
  }
}
