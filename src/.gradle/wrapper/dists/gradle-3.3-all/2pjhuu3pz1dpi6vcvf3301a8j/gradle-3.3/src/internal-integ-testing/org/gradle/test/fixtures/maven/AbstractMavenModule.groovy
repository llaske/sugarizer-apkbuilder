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

package org.gradle.test.fixtures.maven

import groovy.xml.MarkupBuilder
import org.gradle.test.fixtures.AbstractModule
import org.gradle.test.fixtures.Module
import org.gradle.test.fixtures.file.TestFile

import java.text.SimpleDateFormat

abstract class AbstractMavenModule extends AbstractModule implements MavenModule {
    protected static final String MAVEN_METADATA_FILE = "maven-metadata.xml"
    final TestFile moduleDir
    final String groupId
    final String artifactId
    final String version
    Map<String, String> parentPom
    String type = 'jar'
    String packaging
    int publishCount = 1
    boolean noMetaData
    private final List dependencies = []
    private final List artifacts = []
    final updateFormat = new SimpleDateFormat("yyyyMMddHHmmss")
    final timestampFormat = new SimpleDateFormat("yyyyMMdd.HHmmss")

    AbstractMavenModule(TestFile moduleDir, String groupId, String artifactId, String version) {
        this.moduleDir = moduleDir
        this.groupId = groupId
        this.artifactId = artifactId
        this.version = version
    }

    @Override
    String getGroup() {
        return groupId
    }

    @Override
    String getModule() {
        return artifactId
    }

    MavenModule parent(String group, String artifactId, String version) {
        parentPom = [groupId: group, artifactId: artifactId, version: version]
        return this
    }

    TestFile getArtifactFile(Map options = [:]) {
        if (version.endsWith("-SNAPSHOT") && !metaDataFile.exists() && uniqueSnapshots) {
            def artifact = toArtifact(options)
            return moduleDir.file("${artifactId}-${version}${artifact.classifier ? "-${artifact.classifier}" : ""}.${artifact.type}")
        }
        return artifactFile(options)
    }

    abstract boolean getUniqueSnapshots()

    String getPublishArtifactVersion() {
        if (uniqueSnapshots && version.endsWith("-SNAPSHOT")) {
            return "${version.replaceFirst('-SNAPSHOT$', '')}-${getUniqueSnapshotVersion()}"
        }
        return version
    }

    private String getUniqueSnapshotVersion() {
        assert uniqueSnapshots && version.endsWith('-SNAPSHOT')
        if (metaDataFile.isFile()) {
            def metaData = new XmlParser().parse(metaDataFile.assertIsFile())
            def timestamp = metaData.versioning.snapshot.timestamp[0].text().trim()
            def build = metaData.versioning.snapshot.buildNumber[0].text().trim()
            return "${timestamp}-${build}"
        }
        return "${timestampFormat.format(publishTimestamp)}-${publishCount}"
    }

    MavenModule dependsOnModules(String... dependencyArtifactIds) {
        for (String id : dependencyArtifactIds) {
            dependsOn(groupId, id, '1.0')
        }
        return this
    }

    @Override
    MavenModule dependsOn(Module target) {
        dependsOn(target.group, target.module, target.version)
    }

    @Override
    MavenModule dependsOn(Map<String, ?> attributes, Module target) {
        this.dependencies << [groupId: target.group, artifactId: target.module, version: target.version, type: attributes.type, scope: attributes.scope, classifier: attributes.classifier, optional: attributes.optional, exclusions: attributes.exclusions]
        return this
    }

    MavenModule dependsOn(String group, String artifactId, String version, String type = null, String scope = null, String classifier = null, Collection<Map> exclusions = null) {
        this.dependencies << [groupId: group, artifactId: artifactId, version: version, type: type, scope: scope, classifier: classifier, exclusions: exclusions]
        return this
    }

    MavenModule hasPackaging(String packaging) {
        this.packaging = packaging
        return this
    }

    /**
     * Specifies the type of the main artifact.
     */
    MavenModule hasType(String type) {
        this.type = type
        return this
    }

    /**
     * Adds an additional artifact to this module.
     * @param options Can specify any of: type or classifier
     */
    MavenModule artifact(Map<String, ?> options) {
        artifacts << options
        return this
    }

    String getPackaging() {
        return packaging
    }

    List getDependencies() {
        return dependencies
    }

    List getArtifacts() {
        return artifacts
    }

    void assertNotPublished() {
        pomFile.assertDoesNotExist()
    }

    void assertPublished() {
        assert pomFile.assertExists()
        assert parsedPom.groupId == groupId
        assert parsedPom.artifactId == artifactId
        assert parsedPom.version == version
    }

    void assertPublishedAsPomModule() {
        assertPublished()
        assertArtifactsPublished("${artifactId}-${publishArtifactVersion}.pom")
        assert parsedPom.packaging == "pom"
    }

    void assertPublishedAsJavaModule() {
        assertPublished()
        assertArtifactsPublished("${artifactId}-${publishArtifactVersion}.jar", "${artifactId}-${publishArtifactVersion}.pom")
        assert parsedPom.packaging == null
    }

    void assertPublishedAsWebModule() {
        assertPublished()
        assertArtifactsPublished("${artifactId}-${publishArtifactVersion}.war", "${artifactId}-${publishArtifactVersion}.pom")
        assert parsedPom.packaging == 'war'
    }

    void assertPublishedAsEarModule() {
        assertPublished()
        assertArtifactsPublished("${artifactId}-${publishArtifactVersion}.ear", "${artifactId}-${publishArtifactVersion}.pom")
        assert parsedPom.packaging == 'ear'
    }

    /**
     * Asserts that exactly the given artifacts have been deployed, along with their checksum files
     */
    void assertArtifactsPublished(String... names) {
        Set allFileNames = []
        for (name in names) {
            allFileNames.addAll([name, "${name}.sha1", "${name}.md5"])
        }

        assert moduleDir.list() as Set == allFileNames
        for (name in names) {
            assertChecksumsPublishedFor(moduleDir.file(name))
        }
    }

    void assertChecksumsPublishedFor(TestFile testFile) {
        def sha1File = sha1File(testFile)
        sha1File.assertIsFile()
        assert new BigInteger(sha1File.text, 16) == getHash(testFile, "SHA1")
        def md5File = md5File(testFile)
        md5File.assertIsFile()
        assert new BigInteger(md5File.text, 16) == getHash(testFile, "MD5")
    }

    MavenPom getParsedPom() {
        return new MavenPom(pomFile)
    }

    DefaultMavenMetaData getRootMetaData() {
        new DefaultMavenMetaData(rootMetaDataFile)
    }

    TestFile getPomFile() {
        return getArtifactFile(type: 'pom')
    }

    TestFile getPomFileForPublish() {
        return moduleDir.file("$artifactId-${publishArtifactVersion}.pom")
    }

    TestFile getMetaDataFile() {
        moduleDir.file(MAVEN_METADATA_FILE)
    }

    TestFile getRootMetaDataFile() {
        moduleDir.parentFile.file(MAVEN_METADATA_FILE)
    }

    TestFile artifactFile(Map<String, ?> options) {
        def artifact = toArtifact(options)
        def fileName = "$artifactId-${publishArtifactVersion}.${artifact.type}"
        if (artifact.classifier) {
            fileName = "$artifactId-$publishArtifactVersion-${artifact.classifier}.${artifact.type}"
        }
        return moduleDir.file(fileName)
    }

    MavenModule publishWithChangedContent() {
        publishCount++
        return publish()
    }

    protected Map<String, Object> toArtifact(Map<String, ?> options) {
        options = new HashMap<String, Object>(options)
        def artifact = [type: options.remove('type') ?: type, classifier: options.remove('classifier') ?: null]
        assert options.isEmpty(): "Unknown options : ${options.keySet()}"
        return artifact
    }

    Date getPublishTimestamp() {
        return new Date(updateFormat.parse("20100101120000").time + publishCount * 1000)
    }

    MavenModule publishPom() {
        moduleDir.createDir()
        def rootMavenMetaData = getRootMetaDataFile()

        updateRootMavenMetaData(rootMavenMetaData)

        if (publishesMetaDataFile()) {
            publish(metaDataFile) { Writer writer ->
                writer << getMetaDataFileContent()
            }
        }

        publish(pomFileForPublish) { Writer writer ->
            def pomPackaging = packaging ?: type;
            new MarkupBuilder(writer).project {
                mkp.comment(artifactContent)
                modelVersion("4.0.0")
                groupId(groupId)
                artifactId(artifactId)
                version(version)
                packaging(pomPackaging)
                description("Published on ${publishTimestamp}")
                if (parentPom) {
                    parent {
                        groupId(parentPom.groupId)
                        artifactId(parentPom.artifactId)
                        version(parentPom.version)
                    }
                }
                if (dependencies) {
                    dependencies {
                        dependencies.each { dep ->
                            dependency {
                                groupId(dep.groupId)
                                artifactId(dep.artifactId)
                                if (dep.version) {
                                    version(dep.version)
                                }
                                if (dep.type) {
                                    type(dep.type)
                                }
                                if (dep.scope) {
                                    scope(dep.scope)
                                }
                                if (dep.classifier) {
                                    classifier(dep.classifier)
                                }
                                if (dep.optional) {
                                    optional(true)
                                }
                                if (dep.exclusions) {
                                    exclusions {
                                        for (exc in dep.exclusions) {
                                            exclusion {
                                                groupId(exc.groupId)
                                                artifactId(exc.artifactId)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return this
    }

    private void updateRootMavenMetaData(TestFile rootMavenMetaData) {
        def allVersions = rootMavenMetaData.exists() ? new XmlParser().parseText(rootMavenMetaData.text).versioning.versions.version*.value().flatten() : []
        allVersions << version;
        publish(rootMavenMetaData) { Writer writer ->
            def builder = new MarkupBuilder(writer)
            builder.metadata {
                groupId(groupId)
                artifactId(artifactId)
                version(allVersions.max())
                versioning {
                    versions {
                        allVersions.each {currVersion ->
                            version(currVersion)
                        }
                    }
                }
            }
        }
    }

    abstract String getMetaDataFileContent()


    MavenModule withNoMetaData() {
        noMetaData = true
        return this
    }

    MavenModule publish() {
        if(!noMetaData) {
            publishPom()
        }

        artifacts.each { artifact ->
            publishArtifact(artifact)
        }
        if (type != 'pom') {
            publishArtifact([:])
        }

        return this
    }

    File publishArtifact(Map<String, ?> artifact) {
        def artifactFile = artifactFile(artifact)

        publish(artifactFile) { Writer writer ->
            writer << "${artifactFile.name} : $artifactContent"
        }
        return artifactFile
    }

    protected String getArtifactContent() {
        // Some content to include in each artifact, so that its size and content varies on each publish
        return (0..publishCount).join("-")
    }

    protected abstract boolean publishesMetaDataFile()
    protected abstract boolean publishesHashFiles()
}
