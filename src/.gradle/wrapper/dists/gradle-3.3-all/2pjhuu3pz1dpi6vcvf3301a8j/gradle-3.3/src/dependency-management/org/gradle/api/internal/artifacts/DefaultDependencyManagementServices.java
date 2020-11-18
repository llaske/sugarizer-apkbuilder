/*
 * Copyright 2011 the original author or authors.
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

import org.gradle.StartParameter;
import org.gradle.api.artifacts.ConfigurablePublishArtifact;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler;
import org.gradle.api.artifacts.dsl.ComponentModuleMetadataHandler;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.attributes.AttributesSchema;
import org.gradle.api.internal.DomainObjectContext;
import org.gradle.api.internal.artifacts.component.ComponentIdentifierFactory;
import org.gradle.api.internal.artifacts.configurations.ConfigurationContainerInternal;
import org.gradle.api.internal.artifacts.configurations.DefaultConfigurationContainer;
import org.gradle.api.internal.artifacts.configurations.DependencyMetaDataProvider;
import org.gradle.api.internal.artifacts.dsl.DefaultArtifactHandler;
import org.gradle.api.internal.artifacts.dsl.DefaultComponentMetadataHandler;
import org.gradle.api.internal.artifacts.dsl.DefaultComponentModuleMetadataHandler;
import org.gradle.api.internal.artifacts.dsl.DefaultRepositoryHandler;
import org.gradle.api.internal.artifacts.dsl.PublishArtifactNotationParserFactory;
import org.gradle.api.internal.artifacts.dsl.dependencies.DefaultDependencyHandler;
import org.gradle.api.internal.artifacts.dsl.dependencies.DependencyFactory;
import org.gradle.api.internal.artifacts.dsl.dependencies.ProjectFinder;
import org.gradle.api.internal.artifacts.ivyservice.CacheLockingManager;
import org.gradle.api.internal.artifacts.ivyservice.DefaultConfigurationResolver;
import org.gradle.api.internal.artifacts.ivyservice.ErrorHandlingConfigurationResolver;
import org.gradle.api.internal.artifacts.ivyservice.IvyContextManager;
import org.gradle.api.internal.artifacts.ivyservice.IvyContextualArtifactPublisher;
import org.gradle.api.internal.artifacts.ivyservice.ShortCircuitEmptyConfigurationResolver;
import org.gradle.api.internal.artifacts.ivyservice.dependencysubstitution.DependencySubstitutionRules;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ResolveIvyFactory;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.parser.GradlePomModuleDescriptorParser;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionSelectorScheme;
import org.gradle.api.internal.artifacts.ivyservice.moduleconverter.ConfigurationComponentMetaDataBuilder;
import org.gradle.api.internal.artifacts.ivyservice.publisher.DefaultIvyDependencyPublisher;
import org.gradle.api.internal.artifacts.ivyservice.publisher.IvyBackedArtifactPublisher;
import org.gradle.api.internal.artifacts.ivyservice.publisher.IvyXmlModuleDescriptorWriter;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.store.ResolutionResultsStoreFactory;
import org.gradle.api.internal.artifacts.mvnsettings.LocalMavenRepositoryLocator;
import org.gradle.api.internal.artifacts.query.ArtifactResolutionQueryFactory;
import org.gradle.api.internal.artifacts.query.DefaultArtifactResolutionQueryFactory;
import org.gradle.api.internal.artifacts.repositories.DefaultBaseRepositoryFactory;
import org.gradle.api.internal.artifacts.repositories.transport.RepositoryTransportFactory;
import org.gradle.api.internal.attributes.DefaultAttributesSchema;
import org.gradle.api.internal.component.ComponentTypeRegistry;
import org.gradle.api.internal.file.FileCollectionFactory;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.filestore.ivy.ArtifactIdentifierFileStore;
import org.gradle.initialization.ProjectAccessListener;
import org.gradle.internal.authentication.AuthenticationSchemeRegistry;
import org.gradle.internal.component.external.model.ModuleComponentArtifactMetadata;
import org.gradle.internal.event.ListenerManager;
import org.gradle.internal.progress.BuildOperationExecutor;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.resource.local.LocallyAvailableResourceFinder;
import org.gradle.internal.service.DefaultServiceRegistry;
import org.gradle.internal.service.ServiceRegistration;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.internal.typeconversion.NotationParser;

public class DefaultDependencyManagementServices implements DependencyManagementServices {

    private final ServiceRegistry parent;

    public DefaultDependencyManagementServices(ServiceRegistry parent) {
        this.parent = parent;
    }

    public DependencyResolutionServices create(FileResolver fileResolver, DependencyMetaDataProvider dependencyMetaDataProvider, ProjectFinder projectFinder, DomainObjectContext domainObjectContext) {
        DefaultServiceRegistry services = new DefaultServiceRegistry(parent);
        services.add(FileResolver.class, fileResolver);
        services.add(DependencyMetaDataProvider.class, dependencyMetaDataProvider);
        services.add(ProjectFinder.class, projectFinder);
        services.add(DomainObjectContext.class, domainObjectContext);
        services.addProvider(new DependencyResolutionScopeServices());
        return services.get(DependencyResolutionServices.class);
    }

    public void addDslServices(ServiceRegistration registration) {
        registration.addProvider(new DependencyResolutionScopeServices());
    }

    private static class DependencyResolutionScopeServices {
        AttributesSchema createConfigurationAttributesSchema(Instantiator instantiator) {
            return instantiator.newInstance(DefaultAttributesSchema.class);
        }

        BaseRepositoryFactory createBaseRepositoryFactory(LocalMavenRepositoryLocator localMavenRepositoryLocator, Instantiator instantiator, FileResolver fileResolver,
                                                          RepositoryTransportFactory repositoryTransportFactory, LocallyAvailableResourceFinder<ModuleComponentArtifactMetadata> locallyAvailableResourceFinder,
                                                          ArtifactIdentifierFileStore artifactIdentifierFileStore,
                                                          VersionSelectorScheme versionSelectorScheme,
                                                          AuthenticationSchemeRegistry authenticationSchemeRegistry,
                                                          IvyContextManager ivyContextManager) {
            return new DefaultBaseRepositoryFactory(
                    localMavenRepositoryLocator,
                    fileResolver,
                    instantiator,
                    repositoryTransportFactory,
                    locallyAvailableResourceFinder,
                    artifactIdentifierFileStore,
                    new GradlePomModuleDescriptorParser(versionSelectorScheme),
                    authenticationSchemeRegistry,
                    ivyContextManager
            );
        }

        RepositoryHandler createRepositoryHandler(Instantiator instantiator, BaseRepositoryFactory baseRepositoryFactory) {
            return instantiator.newInstance(DefaultRepositoryHandler.class, baseRepositoryFactory, instantiator);
        }

        ConfigurationContainerInternal createConfigurationContainer(Instantiator instantiator, ConfigurationResolver configurationResolver, DomainObjectContext domainObjectContext,
                                                                    ListenerManager listenerManager, DependencyMetaDataProvider metaDataProvider, ProjectAccessListener projectAccessListener,
                                                                    ProjectFinder projectFinder, ConfigurationComponentMetaDataBuilder metaDataBuilder, FileCollectionFactory fileCollectionFactory,
                                                                    GlobalDependencyResolutionRules globalDependencyResolutionRules, ComponentIdentifierFactory componentIdentifierFactory, BuildOperationExecutor buildOperationExecutor) {
            return instantiator.newInstance(DefaultConfigurationContainer.class,
                    configurationResolver,
                    instantiator,
                    domainObjectContext,
                    listenerManager,
                    metaDataProvider,
                    projectAccessListener,
                    projectFinder,
                    metaDataBuilder,
                    fileCollectionFactory,
                    globalDependencyResolutionRules.getDependencySubstitutionRules(),
                    componentIdentifierFactory,
                    buildOperationExecutor
                );
        }

        DependencyHandler createDependencyHandler(Instantiator instantiator, ConfigurationContainerInternal configurationContainer, DependencyFactory dependencyFactory,
                                                  ProjectFinder projectFinder, ComponentMetadataHandler componentMetadataHandler, ComponentModuleMetadataHandler componentModuleMetadataHandler, ArtifactResolutionQueryFactory resolutionQueryFactory) {
            return instantiator.newInstance(DefaultDependencyHandler.class,
                    configurationContainer,
                    dependencyFactory,
                    projectFinder,
                    componentMetadataHandler,
                    componentModuleMetadataHandler,
                    resolutionQueryFactory);
        }

        DefaultComponentMetadataHandler createComponentMetadataHandler(Instantiator instantiator) {
            return instantiator.newInstance(DefaultComponentMetadataHandler.class, instantiator);
        }

        DefaultComponentModuleMetadataHandler createComponentModuleMetadataHandler(Instantiator instantiator) {
            return instantiator.newInstance(DefaultComponentModuleMetadataHandler.class);
        }

        ArtifactHandler createArtifactHandler(Instantiator instantiator, DependencyMetaDataProvider dependencyMetaDataProvider, ConfigurationContainerInternal configurationContainer) {
            NotationParser<Object, ConfigurablePublishArtifact> publishArtifactNotationParser = new PublishArtifactNotationParserFactory(instantiator, dependencyMetaDataProvider).create();
            return instantiator.newInstance(DefaultArtifactHandler.class, configurationContainer, publishArtifactNotationParser);
        }

        GlobalDependencyResolutionRules createModuleMetadataHandler(ComponentMetadataProcessor componentMetadataProcessor, ComponentModuleMetadataProcessor moduleMetadataProcessor, ServiceRegistry serviceRegistry) {
            return new DefaultGlobalDependencyResolutionRules(componentMetadataProcessor, moduleMetadataProcessor, serviceRegistry.getAll(DependencySubstitutionRules.class));
        }

        ConfigurationResolver createDependencyResolver(ArtifactDependencyResolver artifactDependencyResolver,
                                                       RepositoryHandler repositories,
                                                       GlobalDependencyResolutionRules metadataHandler,
                                                       ComponentIdentifierFactory componentIdentifierFactory,
                                                       CacheLockingManager cacheLockingManager,
                                                       ResolutionResultsStoreFactory resolutionResultsStoreFactory,
                                                       StartParameter startParameter,
                                                       AttributesSchema attributesSchema) {
            return new ErrorHandlingConfigurationResolver(
                    new ShortCircuitEmptyConfigurationResolver(
                        new DefaultConfigurationResolver(
                            artifactDependencyResolver,
                            repositories,
                            metadataHandler,
                            cacheLockingManager,
                            resolutionResultsStoreFactory,
                            startParameter.isBuildProjectDependencies(), attributesSchema),
                        componentIdentifierFactory)
            );
        }

        ArtifactPublicationServices createArtifactPublicationServices(ServiceRegistry services) {
            return new DefaultArtifactPublicationServices(services);
        }

        DependencyResolutionServices createDependencyResolutionServices(ServiceRegistry services) {
            return new DefaultDependencyResolutionServices(services);
        }

        ArtifactResolutionQueryFactory createArtifactResolutionQueryFactory(ConfigurationContainerInternal configurationContainer, RepositoryHandler repositoryHandler,
                                                                            ResolveIvyFactory ivyFactory, GlobalDependencyResolutionRules metadataHandler,
                                                                            CacheLockingManager cacheLockingManager, ComponentTypeRegistry componentTypeRegistry) {
            return new DefaultArtifactResolutionQueryFactory(configurationContainer, repositoryHandler, ivyFactory, metadataHandler, cacheLockingManager, componentTypeRegistry);

        }
    }

    private static class DefaultDependencyResolutionServices implements DependencyResolutionServices {
        private final ServiceRegistry services;

        private DefaultDependencyResolutionServices(ServiceRegistry services) {
            this.services = services;
        }

        public RepositoryHandler getResolveRepositoryHandler() {
            return services.get(RepositoryHandler.class);
        }

        public ConfigurationContainerInternal getConfigurationContainer() {
            return services.get(ConfigurationContainerInternal.class);
        }

        public DependencyHandler getDependencyHandler() {
            return services.get(DependencyHandler.class);
        }
    }

    private static class DefaultArtifactPublicationServices implements ArtifactPublicationServices {
        private final ServiceRegistry services;

        public DefaultArtifactPublicationServices(ServiceRegistry services) {
            this.services = services;
        }

        public RepositoryHandler createRepositoryHandler() {
            Instantiator instantiator = services.get(Instantiator.class);
            BaseRepositoryFactory baseRepositoryFactory = services.get(BaseRepositoryFactory.class);
            return instantiator.newInstance(DefaultRepositoryHandler.class, baseRepositoryFactory, instantiator);
        }

        public ArtifactPublisher createArtifactPublisher() {
            IvyBackedArtifactPublisher publisher = new IvyBackedArtifactPublisher(
                services.get(ConfigurationComponentMetaDataBuilder.class),
                new DefaultIvyDependencyPublisher(),
                new IvyXmlModuleDescriptorWriter()
            );
            return new IvyContextualArtifactPublisher(services.get(IvyContextManager.class), publisher);
        }
    }
}
