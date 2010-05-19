/*
 * Copyright 2003 - 2009 The eFaps Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */
package org.efaps.maven.plugin.install;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tools.ant.DirectoryScanner;
import org.efaps.maven.plugin.EFapsAbstractMojo;
import org.efaps.maven_java5.org.apache.maven.tools.plugin.Parameter;
import org.efaps.update.FileType;

/**
 * @author The eFaps Team
 * @version $Id$
 */
public abstract class AbstractEFapsInstallMojo
    extends EFapsAbstractMojo
{
    /**
     * Default Mapping of a a file extension to a Type for import and update.
     */
    private static final Map<String, String> DEFAULT_TYPE_MAPPING = new HashMap<String, String>();
    static {
        AbstractEFapsInstallMojo.DEFAULT_TYPE_MAPPING.put("css", FileType.CSS.getType());
        AbstractEFapsInstallMojo.DEFAULT_TYPE_MAPPING.put("java", FileType.JAVA.getType());
        AbstractEFapsInstallMojo.DEFAULT_TYPE_MAPPING.put("js", FileType.JS.getType());
        AbstractEFapsInstallMojo.DEFAULT_TYPE_MAPPING.put("jrxml", FileType.JRXML.getType());
        AbstractEFapsInstallMojo.DEFAULT_TYPE_MAPPING.put("wiki", FileType.WIKI.getType());
        AbstractEFapsInstallMojo.DEFAULT_TYPE_MAPPING.put("xml", FileType.XML.getType());
        AbstractEFapsInstallMojo.DEFAULT_TYPE_MAPPING.put("xsl", FileType.XSL.getType());
    }

    /**
     * Default list of includes used to evaluate the files to copy.
     *
     * @see #getFiles
     */
    private static final Set<String> DEFAULT_INCLUDES = new HashSet<String>();
    static {
        AbstractEFapsInstallMojo.DEFAULT_INCLUDES.add("**/*.css");
        AbstractEFapsInstallMojo.DEFAULT_INCLUDES.add("**/*.java");
        AbstractEFapsInstallMojo.DEFAULT_INCLUDES.add("**/*.js");
        AbstractEFapsInstallMojo.DEFAULT_INCLUDES.add("**/*.jrxml");
        AbstractEFapsInstallMojo.DEFAULT_INCLUDES.add("**/*.wiki");
        AbstractEFapsInstallMojo.DEFAULT_INCLUDES.add("**/*.xml");
        AbstractEFapsInstallMojo.DEFAULT_INCLUDES.add("**/*.xsl");
    }

    /**
     * Default list of excludes used to evaluate the files to copy.
     *
     * @see #getFiles
     */
    private static final Set<String> DEFAULT_EXCLUDES = new HashSet<String>();
    static {
        AbstractEFapsInstallMojo.DEFAULT_EXCLUDES.add("**/versions.xml");
    }

    /**
     * Location of the version file (defining all versions to install).
     */
    @Parameter(expression = "${basedir}/src/main/efaps/versions.xml")
    private File versionFile;

    /**
     * Root Directory with the XML installation files.
     */
    @Parameter(expression = "${basedir}/src/main/efaps")
    private File eFapsDir;

    /**
     * List of includes.
     */
    private final List<String> includes = null;

    /**
     * List of excludes.
     */
    private final List<String> excludes = null;

    /**
     * Map of TypeMapping.
     */
    private final Map<String, String> typeMapping = null;

    /**
     * Comma separated list of applications to install. The default value is the
     * kernel application. The value is used to define the applications to
     * install or update.
     */
    @Parameter(defaultValue = "eFaps-Kernel")
    private String applications;

    /**
     * The directory where the generated Class will be stored. The directory
     * will be registered as a compile source root of the project such that the
     * generated files will participate in later build phases like compiling and
     * packaging.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/ci")
    private File outputDirectory;



    /**
     * Getter method for the instance variable {@link #outputDirectory}.
     *
     * @return value of instance variable {@link #outputDirectory}
     */
    protected File getOutputDirectory()
    {
        return this.outputDirectory;
    }

    /**
     * Uses the {@link #includes} and {@link #excludes} together with the root
     * directory {@link #eFapsDir} to get all related and matched files.
     *
     * @see #includes defines includes; if not specified by maven, the default
     *      value is <code>**&#x002f;*.xml</code>
     * @see #excludes defines excludes; if not specified by maven , the default
     *      value is <code>**&#x002f;version.xml</code>
     * @see #DEFAULT_INCLUDES
     * @see #DEFAULT_EXCLUDES
     * @return Array of filename
     *
     */
    protected List<String> getFiles()
    {
        final List<String> ret = new ArrayList<String>();
        final DirectoryScanner ds = new DirectoryScanner();
        final String[] included = (this.includes == null)
            ? AbstractEFapsInstallMojo.DEFAULT_INCLUDES
                            .toArray(new String[AbstractEFapsInstallMojo.DEFAULT_INCLUDES.size()])
            : this.includes.toArray(new String[this.includes.size()]);
        final String[] excluded = (this.excludes == null)
            ? AbstractEFapsInstallMojo.DEFAULT_EXCLUDES
                            .toArray(new String[AbstractEFapsInstallMojo.DEFAULT_EXCLUDES.size()])
            : this.excludes.toArray(new String[this.excludes.size()]);
        ds.setIncludes(included);
        ds.setExcludes(excluded);
        ds.setBasedir(getEFapsDir().toString());
        ds.setCaseSensitive(true);
        ds.scan();

        if (this.outputDirectory.exists()) {
            ret.addAll(Arrays.asList(ds.getIncludedFiles()));
            ds.setBasedir(this.outputDirectory);
            ds.scan();
        }
        ret.addAll(Arrays.asList(ds.getIncludedFiles()));

        return ret;
    }

    /**
     * Returns the {@link #applications} to install / update.
     *
     * @return applications
     * @see #applications
     */
    protected String getApplications()
    {
        return this.applications;
    }

    /**
     * Getter method for instance variable {@see #eFapsDir}.
     *
     * @return value of instance variable eFapsDir
     * @see #eFapsDir
     */
    protected File getEFapsDir()
    {
        return this.eFapsDir;
    }

    /**
     * this method return the TypeMapping for import and update. The mapping can
     * be defined in the pom.xml with the parameter {@link #typeMapping}. In the
     * case that the mapping is not defined in the pom.xml a default
     * {@link #DEFAULT_TYPE_MAPPING} will be returned.
     *
     * @return Map containing the mapping of file extension to type
     * @see #typeMapping
     * @see #DEFAULT_TYPE_MAPPING
     */
    protected Map<String, String> getTypeMapping()
    {
        return (this.typeMapping == null) ? AbstractEFapsInstallMojo.DEFAULT_TYPE_MAPPING : this.typeMapping;
    }

    /**
     * Getter method for instance variable {@see #versionFile}.
     *
     * @return value of instance variable versionFile
     * @see #versionFile
     */
    protected File getVersionFile()
    {
        return this.versionFile;
    }
}
