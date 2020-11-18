/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.api.tasks;

import org.apache.commons.io.IOUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.Incubating;
import org.gradle.api.internal.PropertiesUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;

/**
 * Writes a {@link java.util.Properties} in a way that the results can be expected to be reproducible.
 *
 * <p>There are a number of differences compared to how properties are stored:</p>
 * <ul>
 *     <li>no timestamp comment is generated at the beginning of the file</li>
 *     <li>the lines in the resulting files are separated by a pre-set separator (defaults to
 *         {@literal '\n'}) instead of the system default line separator</li>
 *     <li>the properties are sorted alphabetically</li>
 * </ul>
 *
 * <p>Like with {@link java.util.Properties}, Unicode characters are escaped when using the
 * default Latin-1 (ISO-8559-1) encoding.</p>
 *
 * @see java.util.Properties#store(OutputStream, String)
 * @since 3.3
 */
@Incubating
@CacheableTask
@ParallelizableTask
public class WriteProperties extends DefaultTask {
    private Properties properties = new Properties();
    private String lineSeparator = "\n";
    private Object outputFile;
    private String comment;
    private String encoding = "ISO_8859_1";

    /**
     * Returns the properties to be written to the output file.
     */
    @Input
    public Properties getProperties() {
        return properties;
    }

    /**
     * Sets all properties to be written to the output file.
     */
    public void setProperties(Map<?, ?> properties) {
        this.properties.clear();
        this.properties.putAll(properties);
    }

    /**
     * Returns the line separator to be used when creating the properties file.
     * Defaults to {@literal `\n`}.
     */
    @Input
    public String getLineSeparator() {
        return lineSeparator;
    }

    /**
     * Sets the line separator to be used when creating the properties file.
     */
    public void setLineSeparator(String lineSeparator) {
        this.lineSeparator = lineSeparator;
    }

    /**
     * Returns the optional comment to add at the beginning of the properties file.
     */
    @Input
    @Optional
    public String getComment() {
        return comment;
    }

    /**
     * Sets the optional comment to add at the beginning of the properties file.
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Returns the encoding used to write the properties file. Defaults to {@literal ISO_8859_1}.
     * If set to anything different, unicode escaping is turned off.
     */
    @Input
    public String getEncoding() {
        return encoding;
    }

    /**
     * Sets the encoding used to write the properties file. Defaults to {@literal ISO_8859_1}.
     * If set to anything different, unicode escaping is turned off.
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Returns the output file to write the properties to.
     */
    @OutputFile
    public File getOutputFile() {
        return getProject().file(outputFile);
    }

    /**
     * Sets the output file to write the properties to.
     */
    public void setOutputFile(Object outputFile) {
        this.outputFile = outputFile;
    }

    @TaskAction
    public void writeProperties() throws IOException {
        Charset charset = Charset.forName(getEncoding());
        OutputStream out = new BufferedOutputStream(new FileOutputStream(getOutputFile()));
        try {
            PropertiesUtils.store(getProperties(), out, getComment(), charset, getLineSeparator());
        } finally {
            IOUtils.closeQuietly(out);
        }
    }
}
