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

package com.epam.digital.data.platform.excerpt.worker;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.integration.ceph.model.CephObject;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TemplateCephServiceMockHelper {

  public void customizeMock(CephService mockCephService, String bucket,
      String docxPath, String folderPath) {

    var filesWithPathInFolder = getFilesWithPathInFolder(docxPath);
    var filesWithFullPath = getFilesWithFullPath(filesWithPathInFolder, folderPath);
    for (var entry : filesWithFullPath.entrySet()) {
      var cephObject = mock(CephObject.class);
      when(cephObject.getContent()).thenReturn(new ByteArrayInputStream(entry.getValue()));
      when(mockCephService.get(bucket, entry.getKey())).thenReturn(Optional.of(cephObject));
    }
    when(mockCephService.getKeys(bucket, folderPath)).thenReturn(filesWithFullPath.keySet());
  }

  public Map<String, byte[]> getFilesWithPathInFolder(String templatePath) {
    Map<String, byte[]> files = new HashMap<>();
    try (var fis = new FileInputStream("src/it/resources/" + templatePath);
        var zip = new ZipInputStream(fis)) {
      ZipEntry zipEntry;
      while ((zipEntry = zip.getNextEntry()) != null) {
        files.put(zipEntry.getName(), zip.readAllBytes());
      }
    } catch (Exception e) {
      throw new RuntimeException();
    }
    return files;
  }

  private Map<String, byte[]> getFilesWithFullPath(
      Map<String, byte[]> filesWithFolderPath, String folderPath) {

    Map<String, byte[]> result = new HashMap<>();
    filesWithFolderPath.forEach((key, value) -> result.put(folderPath + "/" + key, value));
    return result;
  }
}
