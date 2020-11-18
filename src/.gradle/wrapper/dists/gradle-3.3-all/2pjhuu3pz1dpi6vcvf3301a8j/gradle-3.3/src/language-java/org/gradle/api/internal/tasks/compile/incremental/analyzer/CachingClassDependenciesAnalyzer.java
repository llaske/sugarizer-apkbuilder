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

package org.gradle.api.internal.tasks.compile.incremental.analyzer;

import com.google.common.hash.HashCode;
import org.gradle.api.internal.hash.FileHasher;
import org.gradle.internal.Factory;

import java.io.File;

public class CachingClassDependenciesAnalyzer implements ClassDependenciesAnalyzer {
    private final ClassDependenciesAnalyzer analyzer;
    private final FileHasher hasher;
    private final ClassAnalysisCache cache;

    public CachingClassDependenciesAnalyzer(ClassDependenciesAnalyzer analyzer, FileHasher hasher, ClassAnalysisCache cache) {
        this.analyzer = analyzer;
        this.hasher = hasher;
        this.cache = cache;
    }

    @Override
    public ClassAnalysis getClassAnalysis(final String className, final File classFile) {
        HashCode hash = hasher.hash(classFile);
        return cache.get(hash, new Factory<ClassAnalysis>() {
            public ClassAnalysis create() {
                return analyzer.getClassAnalysis(className, classFile);
            }
        });
    }
}
