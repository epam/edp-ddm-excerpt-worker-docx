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

package com.epam.digital.data.platform.excerpt.worker.util;

import static com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus.FAILED;

import com.epam.digital.data.platform.excerpt.worker.exception.ExcerptProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Component;

@Component
public class ZipUtil {

  public byte[] packInZip(Map<String, byte[]> files) {
    try (var result = new ByteArrayOutputStream();
        var zip = new ZipOutputStream(result)) {

      for (var file : files.entrySet()) {
        zip.putNextEntry(new ZipEntry(file.getKey()));
        zip.write(file.getValue());
        zip.closeEntry();
      }
      zip.flush();
      zip.close();
      return result.toByteArray();
    } catch (IOException e) {
      throw new ExcerptProcessingException(FAILED, "Error while creating zip archive");
    }
  }
}
