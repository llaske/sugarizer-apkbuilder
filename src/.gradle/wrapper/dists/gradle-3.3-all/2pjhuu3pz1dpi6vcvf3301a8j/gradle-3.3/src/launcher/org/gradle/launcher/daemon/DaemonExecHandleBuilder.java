/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.launcher.daemon;

import org.gradle.launcher.daemon.bootstrap.DaemonOutputConsumer;
import org.gradle.process.internal.DefaultExecHandleBuilder;
import org.gradle.process.internal.ExecHandle;

import java.io.File;
import java.util.List;

public class DaemonExecHandleBuilder {

    DefaultExecHandleBuilder builder = new DefaultExecHandleBuilder();

    public ExecHandle build(List<String> args, File workingDir, DaemonOutputConsumer outputConsumer) {
        builder.commandLine(args);
        builder.setWorkingDir(workingDir);
        builder.redirectErrorStream();
        builder.setTimeout(30000);
        builder.setDaemon(true);
        builder.setDisplayName("Gradle build daemon");
        builder.streamsHandler(outputConsumer);
        return builder.build();
    }
}
