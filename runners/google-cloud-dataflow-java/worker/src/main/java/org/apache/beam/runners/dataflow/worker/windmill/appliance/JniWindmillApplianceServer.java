/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.beam.runners.dataflow.worker.windmill.appliance;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.apache.beam.runners.dataflow.worker.windmill.WindmillServerBase;

/**
 * JNI Implementation of a {@link WindmillServerBase}.
 *
 * @implNote This is only for use in Streaming Appliance.
 */
@SuppressWarnings({
  "nullness" // TODO(https://github.com/apache/beam/issues/20497)
})
public class JniWindmillApplianceServer extends WindmillServerBase {
  private static final String WINDMILL_SERVER_JNI_LIBRARY_PROPERTY = "windmill.jni_library";
  private static final String DEFAULT_SHUFFLE_CLIENT_LIBRARY = "libwindmill_service_jni.so";

  static {
    try {
      // TODO: Remove the use of JNI here
      File tempfile = File.createTempFile("libwindmill_service_jni", ".so");
      InputStream input =
          ClassLoader.getSystemResourceAsStream(
              System.getProperty(
                  WINDMILL_SERVER_JNI_LIBRARY_PROPERTY, DEFAULT_SHUFFLE_CLIENT_LIBRARY));
      Files.copy(input, tempfile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      System.load(tempfile.getAbsolutePath());
    } catch (IOException e) {
      throw new RuntimeException("Loading windmill_service failed:", e);
    }
  }

  /**
   * The host should be specified as protocol://address:port to connect to a windmill server through
   * rpcz.
   */
  public JniWindmillApplianceServer(String host) {
    super(host);
  }
}
