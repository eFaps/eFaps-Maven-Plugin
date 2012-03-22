/*
z * Copyright 2003 - 2011 The eFaps Team
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
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.efaps.update.FileType;
import org.efaps.update.Install.InstallFile;
import org.efaps.update.version.Application;
import org.efaps.update.version.Dependency;
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
 * @version $Id$
 */
@MojoGoal(value = "generate-jmsclass")
@MojoRequiresDependencyResolution(value = "compile")
@MojoPhase(value = "generate-sources")
public class GenerateJmsClassMojo
    extends AbstractEFapsInstallMojo
{

    /**
     * The package name.
     */
    @MojoParameter(required = true, defaultValue = "org.efaps.esjp.jms")
    private String jmsPackage;

    /**
     * This Regex will be used to replace the regex with empty string .
     */
    @MojoParameter(required = true, defaultValue = "eFaps-|eFapsApp-")
    private String jmsPackageRegex;

    /**
     * This Regex will be used to replace the regex result with with empty string .
     */
    @MojoParameter(required = true, defaultValue = "^[A-Za-z]+_")
    private String jmsClassNameRegex;

    /**
     * The current Maven project.
     */
    @MojoParameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * Mapping between the Types of eFaps and the package the responding class is in.
     */
    private final Map<String, String> type2package = new HashMap<String, String>();

    /**
     * Executes the install goal.
     *
     * @throws MojoExecutionException if installation failed
     */
    public void execute()
        throws MojoExecutionException
    {
        try {
            init(false);
            getOutputDirectory().mkdir();
            final String folders = this.jmsPackage.replace(".", File.separator);
            final File srcFolder = new File(getOutputDirectory(), folders);
            srcFolder.mkdirs();

            final Application appl = Application.getApplicationFromSource(
                            getVersionFile(),
                            getClasspathElements(),
                            getEFapsDir(),
                            getOutputDirectory(),
                            getIncludes(),
                            getExcludes(),
                            getTypeMapping());

            for (final Dependency dependency : appl.getDependencies().values()) {
                dependency.resolve();
                final Application dependApp = Application.getApplicationFromJarFile(
                                dependency.getJarFile(), getClasspathElements());
                final List<InstallFile> files = dependApp.getInstall().getFiles();
                final String applicationName = dependApp.getApplication().replaceAll(this.jmsPackageRegex, "")
                                    .toLowerCase();
                for (final InstallFile file : files) {
                    if (file.getType().equals(FileType.XML)) {
                        readFile(applicationName, srcFolder, file);
                    }
                }
            }

            final List<InstallFile> files = appl.getInstall().getFiles();
            final String applicationName = appl.getApplication().replaceAll(this.jmsPackageRegex, "").toLowerCase();
            for (final InstallFile file : files) {
                if (file.getType().equals(FileType.XML)) {
                    readFile(applicationName, srcFolder, file);
                }
            }
            this.project.addCompileSourceRoot(getOutputDirectory().getAbsolutePath());
        } catch (final Exception e) {
            throw new MojoExecutionException("Could not execute SourceInstall script", e);
        }
    }

    private void readFile(final String _applicationName,
                          final File _srcFolder,
                          final InstallFile _file)
        throws MojoExecutionException
    {
        try {

            final File folder = new File(_srcFolder, _applicationName);
            folder.mkdirs();
            final TypeHandler handler = new TypeHandler(_applicationName);
            XMLReader reader;

            reader = XMLReaderFactory.createXMLReader();
            reader.setContentHandler(handler);

            final URLConnection connection = _file.getUrl().openConnection();
            connection.setUseCaches(false);
            final InputStream stream = connection.getInputStream();
            final InputSource source = new InputSource(stream);
            reader.parse(source);
            stream.close();

            if (handler.isDmType() && !handler.isDeactivated()) {
                this.type2package.put(handler.getTypeName(), _applicationName);
                final File javaFile = new File(folder, handler.getClassName() + ".java");
                FileUtils.writeStringToFile(javaFile, handler.getJava().toString());
            }
        } catch (final SAXException e) {
            throw new MojoExecutionException("Could not execute SourceInstall script", e);
        } catch (final IOException e) {
            throw new MojoExecutionException("Could not execute SourceInstall script", e);
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
         * List of attributes.
         */
        private final List<String> attributeTypes = new ArrayList<String>();


        /**
         * StringtbUIlder used to hold the content.
         */
        private StringBuilder content = null;

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

        /**
         * Is this the handler for a eFaps Datamodel Type.
         */
        private boolean dmType;

        /**
         * The java code created.
         */
        private final StringBuilder java = new StringBuilder();

        /**
         * Name of the application = package name.
         */
        private final String applicationName;

        /**
         * Name of the class.
         */
        private String className;

        /**
         * Is the jsm generation deactivated.
         */
        private boolean deactivated;

        /**
         * Execute it or not.
         */
        private boolean exec;

        /**
         * @param _applicationName
         */
        public TypeHandler(final String _applicationName)
        {
            this.applicationName = _applicationName;
        }

        /**
         * Getter method for the instance variable {@link #typeName}.
         *
         * @return value of instance variable {@link #typeName}
         */
        public String getTypeName()
        {
            return this.typeName;
        }

        /**
         * Getter method for the instance variable {@link #className}.
         *
         * @return value of instance variable {@link #className}
         */
        public String getClassName()
        {
            return this.className;
        }

        /**
         * Getter method for the instance variable {@link #java}.
         *
         * @return value of instance variable {@link #java}
         */
        public StringBuilder getJava()
        {
            return this.java;
        }

        /**
         * Getter method for the instance variable {@link #dmType}.
         *
         * @return value of instance variable {@link #dmType}
         */
        public boolean isDmType()
        {
            return this.dmType;
        }

        /**
         * Getter method for the instance variable {@link #deactivated}.
         *
         * @return value of instance variable {@link #deactivated}
         */
        public boolean isDeactivated()
        {
            return this.deactivated;
        }

        /*
         * (non-Javadoc)
         * @see org.xml.sax.helpers.DefaultHandler#startDocument()
         */
        @Override
        public void startDocument()
            throws SAXException
        {
            this.java.append("package org.efaps.esjp.jms.").append(this.applicationName).append(";\n\n")
                .append("import java.util.ArrayList;\n")
                .append("import javax.xml.bind.annotation.XmlAccessType;\n")
                .append("import javax.xml.bind.annotation.XmlAccessorType;\n")
                .append("import javax.xml.bind.annotation.XmlElement;\n")
                .append("import javax.xml.bind.annotation.XmlElementWrapper;\n")
                .append("import javax.xml.bind.annotation.XmlElements;\n")
                .append("import javax.xml.bind.annotation.XmlRootElement;\n")
                .append("import javax.xml.bind.annotation.XmlType;\n")
                .append("import org.efaps.esjp.jms.AbstractObject;\n")
                .append("import org.efaps.esjp.jms.annotation.Attribute;\n")
                .append("import org.efaps.esjp.jms.annotation.Type;\n")
                .append("import org.efaps.esjp.jms.attributes.*;\n");
        }

        @Override
        public void startElement(final String _namespaceURI,
                                 final String _localName,
                                 final String _qName,
                                 final Attributes _atts)
        {
            if ("datamodel-type".equals(_qName) || "datamodel-statusgroup".equals(_qName)) {
                this.dmType = true;
            }
            if ("purpose".equals(_qName) && "false".equalsIgnoreCase(_atts.getValue("GeneralInstance"))) {
                this.deactivated = true;
            }
            this.called = false;
            this.content = null;
            this.tag.push(_qName);
        }

        /*
         * (non-Javadoc)
         * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
         */
        @Override
        public void endElement(final String _uri,
                               final String _localName,
                               final String _qName)
            throws SAXException
        {
            if ("uuid".equals(_qName) && this.tag.size() < 3) {
                this.uuid = this.content.toString().trim();
            }

            if ("version-expression".equals(_qName)) {
                //TODO change for a real validation of the version expression
                this.exec = this.content.toString().contains("latest");
            }
            if (this.exec) {
                if ("name".equals(_qName)) {
                    if (this.tag.size() == 3) {
                        this.typeName = this.content.toString().trim();
                        this.className = this.typeName.replaceAll(GenerateJmsClassMojo.this.jmsClassNameRegex, "");
                    } else {
                        this.attributes.add(this.content.toString().trim());
                    }
                } else if ("parent".equals(_qName) && this.tag.size() == 3) {
                    this.parent = this.content.toString().trim();
                } else if ("type".equals(_qName)) {
                    this.attributeTypes.add(this.content.toString().trim());
                }

                if (!this.called) {
                    this.called = true;
                    this.content = null;
                }
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
            if (this.dmType) {
                this.java.append("@XmlAccessorType(XmlAccessType.NONE)\n")
                    .append("@XmlRootElement(name = \"").append(this.typeName).append("\")\n")
                    .append("@XmlType(name = \"").append(this.applicationName).append(".")
                        .append(getTypeName()).append("\")\n")
                    .append("@Type(uuid = \"").append(this.uuid).append("\")\n")
                    .append("public class ").append(this.className).append("\n");

                final String extendStr;
                if (this.parent == null || this.parent.equals("Admin_Abstract")) {
                    extendStr = "AbstractObject";
                } else if (GenerateJmsClassMojo.this.type2package.containsKey(this.parent)
                            && !this.applicationName.equals(GenerateJmsClassMojo.this.type2package.get(this.parent))) {
                    extendStr = GenerateJmsClassMojo.this.jmsPackage + "."
                                    + GenerateJmsClassMojo.this.type2package.get(this.parent) + "."
                                    + this.parent.replaceAll(GenerateJmsClassMojo.this.jmsClassNameRegex, "");
                } else {
                    extendStr = this.parent.replaceAll(GenerateJmsClassMojo.this.jmsClassNameRegex, "");
                }
                this.java.append("   extends ").append(extendStr).append("\n")
                    .append("{\n");

                final StringBuilder getter = new StringBuilder();
                final Iterator<String> typeiter = this.attributeTypes.iterator();
                for (final String attribute : this.attributes) {
                    final String attrType = typeiter.next();
                    if (!"Type".equals(attribute) && !"OID".equals(attribute) && !"ID".equals(attribute)) {
                        this.java
                            .append("    @XmlElement(name = \"").append(attribute.toLowerCase()).append("\")\n")
                            .append("    private ");
                        getter
                            .append("    @Attribute(name = \"").append(attribute).append("\")\n")
                            .append("    public ");
                        final String attrTypeTmp;
                        if ("String".equals(attrType)) {
                            attrTypeTmp = "StringAttribute ";
                        } else {
                            attrTypeTmp = "StringAttribute ";
                        }

                        String instanceVariable = attribute;
                        if ("Class".equalsIgnoreCase(instanceVariable)) {
                            instanceVariable = "Clazz";
                        } else if ("Abstract".equalsIgnoreCase(instanceVariable)) {
                            instanceVariable = "AbstractV";
                        } else if ("Default".equalsIgnoreCase(instanceVariable)) {
                            instanceVariable = "DefaultV";
                        }
                        this.java
                             .append(attrTypeTmp).append(instanceVariable.toLowerCase()).append(";\n\n");
                        getter
                            .append(attrTypeTmp).append("get").append(instanceVariable).append("()\n")
                            .append("    {\n")
                            .append("        return this.").append(instanceVariable.toLowerCase()).append(";\n")
                            .append("    }\n\n");
                    }
                }
                this.java.append(getter).append("}\n");
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
