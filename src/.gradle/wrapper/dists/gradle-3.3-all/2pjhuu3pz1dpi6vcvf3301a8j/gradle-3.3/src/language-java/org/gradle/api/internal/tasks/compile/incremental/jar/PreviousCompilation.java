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

package org.gradle.api.internal.tasks.compile.incremental.jar;

import org.gradle.api.internal.tasks.compile.incremental.deps.ClassSetAnalysis;
import org.gradle.api.internal.tasks.compile.incremental.deps.DependentsSet;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class PreviousCompilation {

    private ClassSetAnalysis analysis;
    private LocalJarClasspathSnapshotStore classpathSnapshotStore;
    private final JarSnapshotCache jarSnapshotCache;
    private Map<File, JarSnapshot> jarSnapshots;

    public PreviousCompilation(ClassSetAnalysis analysis, LocalJarClasspathSnapshotStore classpathSnapshotStore, JarSnapshotCache jarSnapshotCache) {
        this.analysis = analysis;
        this.classpathSnapshotStore = classpathSnapshotStore;
        this.jarSnapshotCache = jarSnapshotCache;
    }

    public DependentsSet getDependents(Set<String> allClasses) {
        return analysis.getRelevantDependents(allClasses);
    }

    public JarSnapshot getJarSnapshot(File file) {
        if (jarSnapshots == null) {
            JarClasspathSnapshotData data = classpathSnapshotStore.get();
            jarSnapshots = jarSnapshotCache.getJarSnapshots(data.getJarHashes());
        }
        return jarSnapshots.get(file);
    }

    public DependentsSet getDependents(String className) {
        return analysis.getRelevantDependents(className);
    }
}
