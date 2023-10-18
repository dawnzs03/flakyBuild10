/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.exec;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;

public class ExecDefaultExecutor extends DefaultExecutor {

    private transient Process process;

    public ExecDefaultExecutor() {
    }

    @Override
    protected Process launch(CommandLine command, Map<String, String> env, File dir) throws IOException {
        process = super.launch(command, env, dir);
        return process;
    }

    public int getExitValue() {
        if (process != null) {
            try {
                return process.exitValue();
            } catch (IllegalThreadStateException e) {
                // Disabled the process is alive
            }
        }
        return 0;
    }
}
