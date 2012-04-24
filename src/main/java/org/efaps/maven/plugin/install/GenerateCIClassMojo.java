/*
 * Copyright 2003 - 2011 The eFaps Team
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.efaps.update.FileType;
import org.efaps.update.Install.InstallFile;
import org.efaps.update.util.InstallationException;
import org.efaps.update.version.Application;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoPhase;
import org.jfrog.maven.annomojo.annotations.MojoRequiresDependencyResolution;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: GenerateCIClassMojo.java 7384 2012-03-22 15:57:32Z
 *          jan@moxter.net $
 */
@MojoGoal(value = "generate-ciclass")
@MojoRequiresDependencyResolution(value = "compile")
@MojoPhase(value = "generate-sources")
public class GenerateCIClassMojo
    extends AbstractEFapsInstallMojo
{

    /**
     * The CiName.
     */
    @MojoParameter(required = true)
    private String ciName;

    /**
     * The package name.
     */
    @MojoParameter(required = true, defaultValue = "org.efaps.esjp.ci")
    private String ciPackage;

    /**
     * This Regex will be used to replace the ciName with
     * {@link #ciNameReplacement}.
     */
    @MojoParameter(defaultValue = "^([A-Za-z]*)_",
                    description = "This Regex will be used to replace the application name with ciNameReplacement")
    private String ciTypeRegex;

    /**
     * The replacement String used in conjunction with {@link #ciNameRegex}.
     */
    @MojoParameter(defaultValue = "",
                    description = "The replacement String used in conjunction with ciNameRegex")
    private final String ciTypeReplacement;

    /**
     * This Regex will be used to replace the unallowed Characters with
     * {@link #ciUnallowedReplacement}.
     */
    @MojoParameter(defaultValue = "-",
                    description = "This Regex will be used to replace the unallowed Characters in Type names")
    private final String ciUnallowedRegex;

    /**
     * The replacement String used in conjunction with {@link #ciUnallowedRegex}
     * .
     */
    @MojoParameter(defaultValue = "",
                    description = "The replacement String used in conjunction with ciNameRegex")
    private final String ciUnallowedReplacement;

    /**
     * String to be used for String.indexof.
     */
    @MojoParameter(defaultValue = "_.+",
                    description = "The replacement String used in conjunction with ciNameRegex")
    private String ciParentRegex;

    /**
     * The replacement String used in conjunction with {@link #ciParentRegex}.
     */
    @MojoParameter(defaultValue = "",
                    description = "The replacement String used in conjunction with ciParentRegex")
    private final String ciParentReplacment;

    private final Map<String, TypeHandler> types = new HashMap<String, TypeHandler>();

    /**
     * The current Maven project.
     */
    @MojoParameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * Constructor.
     */
    public GenerateCIClassMojo()
    {
        this.ciTypeReplacement = "";
        this.ciParentReplacment = "";
        this.ciUnallowedReplacement = "";
        this.ciUnallowedRegex = "-";
    }

    /**
     * Generates the installation XML file and copies all eFaps definition
     * installation files.
     *
     * @see #generateInstallFile()
     * @see #copyFiles(String)
     * @throws MojoExecutionException on error
     * @throws MojoFailureException on error
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        try {
            init(false);
            final Application appl = Application.getApplicationFromSource(
                            getVersionFile(),
                            getClasspathElements(),
                            getEFapsDir(),
                            getOutputDirectory(),
                            getIncludes(),
                            getExcludes(),
                            getTypeMapping());

            final List<InstallFile> files = appl.getInstall().getFiles();
            for (final InstallFile file : files) {
                if (file.getType().equals(FileType.XML)) {
                    final XMLReader reader = XMLReaderFactory.createXMLReader();
                    reader.setContentHandler(new TypeHandler());
                    final URLConnection connection = file.getUrl().openConnection();
                    connection.setUseCaches(false);
                    final InputStream stream = connection.getInputStream();
                    final InputSource source = new InputSource(stream);
                    reader.parse(source);
                    stream.close();
                }
            }
            // there is a not unlikely chance to produce a duplicated Type, therefore it is checked here
            final Map<String, String> typeTmp = new HashMap<String, String>();
            final Set<String> duplicated = new HashSet<String>();
            for (final Entry<String, TypeHandler> entry : this.types.entrySet()) {
                String typeName = entry.getValue().typeName.replaceAll(this.ciUnallowedRegex,
                                this.ciUnallowedReplacement);
                typeName = typeName.replaceAll(this.ciTypeRegex == null ? (this.ciName + "_") : this.ciTypeRegex,
                                this.ciTypeReplacement);
                if (typeTmp.containsKey(typeName)) {
                    duplicated.add(entry.getValue().typeName);
                    duplicated.add(typeTmp.get(typeName));
                } else {
                    typeTmp.put(typeName, entry.getValue().typeName);
                }
            }

            final StringBuilder java = new StringBuilder()
                            .append("//CHECKSTYLE:OFF\n")
                            .append("package ").append(this.ciPackage).append(";\n")
                            .append("import org.efaps.ci.CIAttribute;\n")
                            .append("import org.efaps.ci.CIType;\n\n")
                            .append("public final class CI").append(this.ciName).append("\n{\n");

            for (final Entry<String, TypeHandler> entry : this.types.entrySet()) {
                String typeName = entry.getValue().typeName.replaceAll(this.ciUnallowedRegex,
                                this.ciUnallowedReplacement);
                if (!duplicated.contains(entry.getValue().typeName)) {
                    typeName = typeName.replaceAll(this.ciTypeRegex == null ? (this.ciName + "_") : this.ciTypeRegex,
                                this.ciTypeReplacement);
                }

                String parentType = null;
                if (entry.getValue().parent != null) {
                    parentType = entry.getValue().parent.replaceAll(this.ciUnallowedRegex,
                                    this.ciUnallowedReplacement);
                    if (!duplicated.contains(entry.getValue().parent)) {
                        parentType = parentType.replaceAll(this.ciTypeRegex == null
                                        ? (this.ciName + "_") : this.ciTypeRegex, this.ciTypeReplacement);
                    }
                    parentType = "_" + parentType;
                    if (!this.types.containsKey(entry.getValue().parent)) {
                        final String parentClass = entry.getValue().parent.replaceAll(this.ciParentRegex,
                                        this.ciParentReplacment);
                        parentType = "org.efaps.esjp.ci.CI" + parentClass + "." + parentType;
                    }
                }

                java.append("    public static final _").append(typeName).append(" ").append(typeName)
                    .append(" = new _").append(typeName).append("(\"").append(entry.getValue().uuid).append("\");\n")
                    .append("    public static class _").append(typeName).append(" extends ")
                    .append(parentType == null ? "CIType" : parentType)
                    .append("\n    {\n")
                    .append("        protected _").append(typeName).append("(final String _uuid)\n        {\n")
                    .append("            super(_uuid);")
                    .append("\n        }\n");

                for (final String attribute : entry.getValue().attributes) {
                    if (!"Type".equals(attribute) && !"OID".equals(attribute) && !"ID".equals(attribute)) {
                        java.append("        public final CIAttribute ").append(attribute)
                                        .append(" = new CIAttribute(this, \"").append(attribute).append("\");\n");
                    }
                }
                java.append("    }\n\n");
            }
            java.append("}\n");

            getOutputDirectory().mkdir();

            final String folders = this.ciPackage.replace(".", File.separator);
            final File srcFolder = new File(getOutputDirectory(), folders);
            srcFolder.mkdirs();

            final File javaFile = new File(srcFolder, "CI" + this.ciName + ".java");

            FileUtils.writeStringToFile(javaFile, java.toString());

            this.project.addCompileSourceRoot(getOutputDirectory().getAbsolutePath());

        } catch (final SAXException e) {
            getLog().error("MojoExecutionException", e);
            throw new MojoExecutionException("SAXException");
        } catch (final FileNotFoundException e) {
            getLog().error("FileNotFoundException", e);
            throw new MojoExecutionException("SAXException");
        } catch (final IOException e) {
            getLog().error("IOException", e);
            throw new MojoExecutionException("SAXException");
        } catch (final InstallationException e) {
            getLog().error("InstallationException", e);
            throw new MojoExecutionException("InstallationException");
        }
    }

    public class TypeHandler
        extends DefaultHandler
    {

        /**
         * Has this handler been called.
         */
        private boolean called = false;

        /**
         * List of attributes.
         */
        private final List<String> attributes = new ArrayList<String>();

        /**
         * StringtbUIlder used to hold the content.
         */
        private StringBuilder content = null;

        /**
         * Is the currently analyzed xml a citype.
         */
        private boolean isCiType = false;

        /**
         * Tags used in this Handler.
         */
        private final Stack<String> tag = new Stack<String>();

        /**
         * Name of the type.
         */
        private String typeName;

        /**
         * UUID of the type.
         */
        private String uuid;

        /**
         * Parent of the type.
         */
        private String parent;

        @Override
        public void startElement(final String _namespaceURI,
                                 final String _localName,
                                 final String _qName,
                                 final Attributes _atts)
        {
            if ("datamodel-type".equals(_qName) || "datamodel-statusgroup".equals(_qName)) {
                this.isCiType = true;
            }
            this.called = false;
            this.content = null;
            this.tag.push(_qName);
        }

        /*
         * (non-Javadoc)
         * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
         * java.lang.String, java.lang.String)
         */
        @Override
        public void endElement(final String uri,
                               final String localName,
                               final String qName)
            throws SAXException
        {
            if ("name".equals(qName)) {
                if (this.tag.size() == 3) {
                    this.typeName = this.content.toString().trim();
                } else {
                    this.attributes.add(this.content.toString().trim());
                }
            } else if ("uuid".equals(qName) && this.tag.size() == 2) {
                this.uuid = this.content.toString().trim();
            } else if ("parent".equals(qName) && this.tag.size() == 3) {
                this.parent = this.content.toString().trim();
            }

            if (!this.called) {
                this.called = true;
                this.content = null;
            }

            if (!this.tag.isEmpty()) {
                this.tag.pop();
            }
        }

        /*
         * (non-Javadoc)
         * @see org.xml.sax.helpers.DefaultHandler#endDocument()
         */
        @Override
        public void endDocument()
            throws SAXException
        {
            if (this.isCiType) {
                GenerateCIClassMojo.this.types.put(this.typeName, this);
            }
        }

        /*
         * (non-Javadoc)
         * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
         */
        @Override
        public void characters(final char[] _ch,
                               final int _start,
                               final int _length)
            throws SAXException
        {
            if (_length > 0) {
                final String contentTmp = new String(_ch, _start, _length);
                if (!this.called && !this.tag.empty()) {
                    if (this.content == null) {
                        this.content = new StringBuilder();
                    }
                    this.content.append(contentTmp);
                }
            }
        }
    }
}
