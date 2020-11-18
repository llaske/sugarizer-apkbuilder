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

package org.gradle.api.internal.artifacts;

import org.gradle.api.internal.artifacts.ivyservice.ArtifactCacheMetaData;
import org.gradle.api.internal.artifacts.ivyservice.CacheLockingManager;
import org.gradle.api.internal.artifacts.ivyservice.DefaultCacheLockingManager;
import org.gradle.cache.CacheRepository;
import org.gradle.internal.service.ServiceRegistration;
import org.gradle.internal.service.scopes.GradleUserHomeScopePluginServices;
import org.gradle.internal.service.scopes.PluginServiceRegistry;

public class DependencyServices implements PluginServiceRegistry, GradleUserHomeScopePluginServices {
    public void registerGlobalServices(ServiceRegistration registration) {
        registration.addProvider(new DependencyManagementGlobalScopeServices());
    }

    @Override
    public void registerGradleUserHomeServices(ServiceRegistration registration) {
        registration.addProvider(new DependencyManagementGradleUserHomeScopeServices());
    }

    public void registerBuildSessionServices(ServiceRegistration registration) {
        registration.addProvider(new Object() {
            CacheLockingManager createCacheLockingManager(CacheRepository cacheRepository, ArtifactCacheMetaData artifactCacheMetaData) {
                return new DefaultCacheLockingManager(cacheRepository, artifactCacheMetaData);
            }
        });
    }

    public void registerBuildServices(ServiceRegistration registration) {
        registration.addProvider(new DependencyManagementBuildScopeServices());
    }

    public void registerGradleServices(ServiceRegistration registration) {
    }

    public void registerProjectServices(ServiceRegistration registration) {
    }
}
