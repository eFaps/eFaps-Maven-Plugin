/*
 * Copyright 2003 - 2010 The eFaps Team
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.DirectoryScanner;
import org.efaps.maven_java5.org.apache.maven.tools.plugin.Goal;
import org.efaps.maven_java5.org.apache.maven.tools.plugin.Parameter;
import org.efaps.maven_java5.org.apache.maven.tools.plugin.lifecycle.Phase;
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
 * @version $Id$
 */
@Goal(name = "generate-ciclass", requiresDependencyResolutionScope = "compile", defaultPhase = Phase.GENERATE_SOURCES)
public class GenerateCIClassMojo
    implements Mojo
{

    private final StringBuilder java = new StringBuilder();

    /**
     * The current Maven project.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * The directory where the DataMOdel Configuration Item files are located.
     */
    @Parameter(defaultValue = "${basedir}/src/main/efaps/DataModels")
    private File sourceDirectory;

    /**
     * The directory where the generated Class will be stored. The directory
     * will be registered as a compile source root of the project such that the
     * generated files will participate in later build phases like compiling and
     * packaging.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/ci")
    private File outputDirectory;

    @Parameter(required = true)
    private String ciName;


    @Parameter(required = true, defaultValue = "org.efaps.esjp.ci")
    private String ciPackage;


    /**
     * List of includes.
     */
    private final List<String> includes = null;

    /**
     * Default list of includes used to evaluate the files to copy.
     *
     * @see #getFiles
     */
    private static final Set<String> DEFAULT_INCLUDES = new HashSet<String>();
    static {

        GenerateCIClassMojo.DEFAULT_INCLUDES.add("**/*.xml");
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
            initJavaFile();
            for (final String fileName : getCopyFiles()) {
                final File srcFile = new File(this.sourceDirectory, fileName);

                // einen XML Reader erzeugen
                final XMLReader reader = XMLReaderFactory.createXMLReader();
                // den eigenen Sax Content Handler registrieren
                reader.setContentHandler(new TypeHandler());
                // unsere Beispiel XML Datei parsen

                final InputStream stream = new FileInputStream(srcFile);
                reader.parse(new InputSource(stream));
                stream.close();
            }
            closeJavaFile();
            this.outputDirectory.mkdir();

            final String folders = this.ciPackage.replace(".", File.separator);
            final File srcFolder = new File(this.outputDirectory, folders);
            srcFolder.mkdirs();

            final File javaFile =  new File(srcFolder, "CI" + this.ciName + ".java");

            FileUtils.writeStringToFile(javaFile, this.java.toString());

            this.project.addCompileSourceRoot(this.outputDirectory.getAbsolutePath());

        } catch (final SAXException e) {

        } catch (final FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void closeJavaFile()
    {
        this.java.append("}");
    }

    /**
     *
     */
    private void initJavaFile()
    {
        this.java.append("package ").append(this.ciPackage).append(";\n")
            .append("import org.efaps.ci.CIAttribute;\n")
            .append("import org.efaps.ci.CIType;\n")
            .append("public final class CI").append(this.ciName).append("\n{\n");
    }

    protected String[] getCopyFiles()
    {
        // scan
        final DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(this.sourceDirectory.toString());
        final String[] included = (this.includes == null)
                        ? GenerateCIClassMojo.DEFAULT_INCLUDES
                                        .toArray(new String[GenerateCIClassMojo.DEFAULT_INCLUDES.size()])
                        : this.includes.toArray(new String[this.includes.size()]);
        ds.setIncludes(included);
        ds.setCaseSensitive(true);
        ds.scan();
        return ds.getIncludedFiles();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.maven.plugin.Mojo#getLog()
     */
    @Override
    public Log getLog()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.maven.plugin.Mojo#setLog(org.apache.maven.plugin.logging.Log)
     */
    @Override
    public void setLog(final Log arg0)
    {
        // TODO Auto-generated method stub

    }

    public class TypeHandler
        extends DefaultHandler
    {

        /**
         * Has this handler been called.
         */
        private boolean called = false;

        private final List<String> attributes = new ArrayList<String>();
        /**
         * StringtbUIlder used to hold the content.
         */
        private StringBuilder content = null;

        private boolean isCiType = false;

        /**
         * Tags used in this Handler.
         */
        private final Stack<String> tag = new Stack<String>();
        private String typeName;
        private String uuid;

        private String parent;

        @Override
        public void startElement(final String _namespaceURI,
                                 final String _localName,
                                 final String _qName,
                                 final Attributes _atts)
        {
            if ("datamodel-type".equals(_qName)) {
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
            } else if ("uuid".equals(qName)) {
                this.uuid = this.content.toString().trim();
            } else if ("parent".equals(qName)) {
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
                this.typeName = this.typeName.substring(this.typeName.indexOf("_") + 1);
                String parentClass = "";
                if (this.parent != null) {
                    parentClass = this.parent.substring(0, this.parent.indexOf("_"));
                    this.parent = this.parent.substring(this.parent.indexOf("_") + 1);
                }

                GenerateCIClassMojo.this.java.append("public static final _").append(this.typeName).append(" ")
                    .append(this.typeName).append(" = new _").append(this.typeName).append("(\"")
                    .append(this.uuid).append("\");\n")
                    .append("public static class _").append(this.typeName).append(" extends ")
                    .append(this.parent == null ? "CIType" : "org.efaps.esjp.ci.CI" + parentClass + "._" + this.parent)
                    .append("\n  {\n")
                    .append("  protected _").append(this.typeName).append("(final String _uuid)\n{\n")
                    .append("    super(_uuid);").append("\n}\n");

                for (final String attribute : this.attributes) {
                    if (!"Type".equals(attribute) && !"OID".equals(attribute) && !"ID".equals(attribute)) {
                        GenerateCIClassMojo.this.java.append("    public final CIAttribute ").append(attribute)
                            .append(" = new CIAttribute(this, \"").append(attribute).append("\");\n");
                    }
                }
                GenerateCIClassMojo.this.java.append("\n}\n");
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
