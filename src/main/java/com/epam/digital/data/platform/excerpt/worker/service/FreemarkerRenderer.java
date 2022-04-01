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
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FreemarkerRenderer implements Renderer {

  private final Logger log = LoggerFactory.getLogger(FreemarkerRenderer.class);
  private final Configuration freemarker;

  public FreemarkerRenderer(Configuration freemarker) {
    this.freemarker = freemarker;
  }

  @Override
  public String render(String templateName, String templateStr, Object jsonData) {
    try (var result = new StringWriter()) {
      var template = new Template(templateName, templateStr, freemarker);
      template.process(jsonData, result);
      return result.toString();
    } catch (TemplateException e) {
      log.error("Template to xml conversion fails", e);
      throw new ExcerptProcessingException(FAILED, "Template to xml conversion fails");
    } catch (IOException e) {
      log.error("IOException occurred while processing the template", e);
      throw new ExcerptProcessingException(FAILED,
          "IOException occurred while processing the template");
    } catch (Exception e) {
      log.error("An unexpected error occurred while processing the template", e);
      throw new ExcerptProcessingException(FAILED,
          "An unexpected error occurred while processing the template");
    }
  }
}
