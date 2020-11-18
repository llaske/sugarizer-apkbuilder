/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.nativeplatform.toolchain.internal.gcc;

import org.gradle.api.Transformer;
import org.gradle.internal.operations.BuildOperationProcessor;
import org.gradle.nativeplatform.toolchain.internal.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class GccCompatibleNativeCompiler<T extends NativeCompileSpec> extends NativeCompiler<T> {

    GccCompatibleNativeCompiler(BuildOperationProcessor buildOperationProcessor, CommandLineToolInvocationWorker commandLineTool, CommandLineToolContext invocationContext, final ArgsTransformer<T> argsTransformer, Transformer<T, T> specTransformer, String objectFileExtension, boolean useCommandFile) {
        super(buildOperationProcessor, commandLineTool, invocationContext, argsTransformer, specTransformer, objectFileExtension, useCommandFile);
    }

    @Override
    protected List<String> getOutputArgs(File outputFile) {
        return Arrays.asList("-o", outputFile.getAbsolutePath());
    }

    @Override
    protected void addOptionsFileArgs(List<String> args, File tempDir) {
        OptionsFileArgsWriter writer = new GccOptionsFileArgsWriter(tempDir);
        // modifies args in place
        writer.execute(args);
    }

    @Override
    protected List<String> getPCHArgs(T spec) {
        List<String> pchArgs = new ArrayList<String>();
        if (spec.getPrefixHeaderFile() != null) {
            pchArgs.add("-include");
            pchArgs.add(spec.getPrefixHeaderFile().getAbsolutePath());
        }
        return pchArgs;
    }
}
