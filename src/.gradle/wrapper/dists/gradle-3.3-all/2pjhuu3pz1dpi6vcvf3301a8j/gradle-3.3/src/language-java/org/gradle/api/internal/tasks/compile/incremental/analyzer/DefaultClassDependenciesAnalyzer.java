/*
 * Copyright 2013 the original author or authors.
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

package org.gradle.api.internal.tasks.compile.incremental.analyzer;

import com.google.common.io.ByteStreams;
import org.gradle.util.GFileUtils;
import org.gradle.util.internal.Java9ClassReader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class DefaultClassDependenciesAnalyzer implements ClassDependenciesAnalyzer {

    public ClassAnalysis getClassAnalysis(String className, InputStream input) throws IOException {
        ClassRelevancyFilter filter = new ClassRelevancyFilter(className);
        ClassReader reader = new Java9ClassReader(ByteStreams.toByteArray(input));
        ClassDependenciesVisitor visitor = new ClassDependenciesVisitor();
        reader.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        Set<String> classDependencies = getClassDependencies(filter, reader);
        return new ClassAnalysis(classDependencies, visitor.dependentToAll);
    }

    private Set<String> getClassDependencies(ClassRelevancyFilter filter, ClassReader reader) {
        Set<String> out = new HashSet<String>();
        char[] charBuffer = new char[reader.getMaxStringLength()];
        for (int i = 1; i < reader.getItemCount(); i++) {
            int itemOffset = reader.getItem(i);
            if (itemOffset > 0 && reader.readByte(itemOffset - 1) == 7) {
                // A CONSTANT_Class entry, read the class descriptor
                String classDescriptor = reader.readUTF8(itemOffset, charBuffer);
                Type type = Type.getObjectType(classDescriptor);
                while (type.getSort() == Type.ARRAY) {
                    type = type.getElementType();
                }
                if (type.getSort() != Type.OBJECT) {
                    // A primitive type
                    continue;
                }
                String name = type.getClassName();
                if (filter.isRelevant(name)) {
                    out.add(name);
                }
            }
        }
        return out;
    }

    @Override
    public ClassAnalysis getClassAnalysis(String className, File classFile) {
        FileInputStream input = GFileUtils.openInputStream(classFile);
        try {
            return getClassAnalysis(className, input);
        } catch (IOException e) {
            throw new RuntimeException("Problems loading class analysis for '" + className + "' from file: " + classFile);
        } finally {
            GFileUtils.closeInputStream(input);
        }
    }
}
