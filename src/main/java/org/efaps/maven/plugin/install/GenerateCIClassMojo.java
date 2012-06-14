/*
 * Copyright 2003 - 2012 The eFaps Team
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.digester3.Digester;
import org.apache.commons.digester3.annotations.FromAnnotationsRuleModule;
import org.apache.commons.digester3.annotations.rules.BeanPropertySetter;
import org.apache.commons.digester3.annotations.rules.CallMethod;
import org.apache.commons.digester3.annotations.rules.CallParam;
import org.apache.commons.digester3.annotations.rules.ObjectCreate;
import org.apache.commons.digester3.annotations.rules.SetNext;
import org.apache.commons.digester3.binder.DigesterLoader;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.ContextEnabled;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.xmlbeans.impl.common.NameUtil;
import org.efaps.update.FileType;
import org.efaps.update.Install.InstallFile;
import org.efaps.update.util.InstallationException;
import org.efaps.update.version.Application;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoPhase;
import org.jfrog.maven.annomojo.annotations.MojoRequiresDependencyResolution;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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
    implements ContextEnabled
{

    /**
     * Definitions for a CI UserInterface object.
     */
    private enum CIDef4UI
    {
        /** Form. */
        FORM("CIForm", "CIForm"),
        /** Table. */
        TABLE("CITable", "CITable");

        /**
         * Class that is extended.
         */
        public final String extendClass;

        /**
         * Prefix for the class Name.
         */
        public final String classNamePrefix;

        /**
         * @param _extendClass Class that is extended
         * @param _classNamePrefix prefix for the class Name
         */
        private CIDef4UI(final String _extendClass,
                         final String _classNamePrefix)
        {
            this.extendClass = _extendClass;
            this.classNamePrefix = _classNamePrefix;
        }
    }

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

    /**
     * Set of types.
     */
    private final Map<String, ITypeCI> types = new TreeMap<String, ITypeCI>();

    /**
     * Set of Tables.
     */
    private final Set<UserInterfaceCI> uiCIs = new HashSet<UserInterfaceCI>();


    /**
     * The current Maven project.
     */
    @MojoParameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /** Plugin container context */
    @SuppressWarnings("rawtypes")
    private Map pluginContext;

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
            final DigesterLoader loader = DigesterLoader.newLoader(new FromAnnotationsRuleModule()
            {

                @Override
                protected void configureRules()
                {
                    bindRulesFrom(TypeCI.class);
                    bindRulesFrom(StatusCI.class);
                    bindRulesFrom(FormCI.class);
                    bindRulesFrom(TableCI.class);
                }
            });

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
                    final Digester digester = loader.newDigester();
                    final URLConnection connection = file.getUrl().openConnection();
                    connection.setUseCaches(false);
                    final InputStream stream = connection.getInputStream();
                    final InputSource source = new InputSource(stream);
                    final Object item = digester.parse(source);
                    stream.close();
                    if (item != null) {
                        if (item instanceof ITypeCI) {
                            this.types.put(((ITypeCI) item).getDefinitions().get(0).getName(), (ITypeCI) item);
                        } else {
                            this.uiCIs.add((UserInterfaceCI) item);
                        }
                    }
                }
            }
            buildCIType();
            buildCI4UI(CIDef4UI.FORM);
            buildCI4UI(CIDef4UI.TABLE);
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

    /*
     * (non-Javadoc)
     * @see
     * org.apache.maven.plugin.ContextEnabled#setPluginContext(java.util.Map)
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void setPluginContext(final Map _pluginContext)
    {
        this.pluginContext = _pluginContext;

    }

    /*
     * (non-Javadoc)
     * @see org.apache.maven.plugin.ContextEnabled#getPluginContext()
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Map getPluginContext()
    {
        return this.pluginContext;
    }

    /**
     * Build the CI UI File for a given CI Defintion.
     *
     * @param _ciDef CI Definition the file will e build for
     * @throws IOException if writing of the actual file fails
     */
    private void buildCI4UI(final CIDef4UI _ciDef)
        throws IOException
    {
        final StringBuilder java = new StringBuilder()
                        .append("//CHECKSTYLE:OFF\n")
                        .append("package ").append(this.ciPackage).append(";\n")
                        .append("import org.efaps.ci.*;\n\n")
                        .append(getClassComment())
                        .append("public final class ").append(_ciDef.classNamePrefix).append(this.ciName)
                        .append("\n{\n");

         for (final UserInterfaceCI uici : this.uiCIs) {
             if (uici.getCIDef().equals(_ciDef)) {
                 final String formName = uici.getName().replaceAll(this.ciUnallowedRegex,this.ciUnallowedReplacement);

                 java.append("    public static final _").append(formName).append(" ").append(formName)
                     .append(" = new _").append(formName).append("(\"").append(uici.getUuid()).append("\");\n")
                     .append("    public static class _").append(formName).append(" extends ")
                     .append(_ciDef.extendClass)
                     .append("\n    {\n")
                     .append("        protected _").append(formName).append("(final String _uuid)\n        {\n")
                     .append("            super(_uuid);")
                     .append("\n        }\n");

                final Map<String, List<String>> fields = new TreeMap<String, List<String>>();
                for (final UIDefintion uiDef : uici.getDefinitions()) {
                    for (final String field : uiDef.getFields()) {
                        List<String> profiles;
                        if (fields.containsKey(field)) {
                            profiles = fields.get(field);
                        } else {
                            profiles = new ArrayList<String>();
                        }
                        profiles.addAll(uiDef.getProfiles());
                        fields.put(field, profiles);
                    }
                }

                for (final Entry<String, List<String>> entry : fields.entrySet()) {
                    // check if the attribute name can be used in java, if not
                    // extend the
                    final String identifier = NameUtil.isValidJavaIdentifier(entry.getKey()) ?
                                    entry.getKey() : entry.getKey() + "_field";
                    java.append("        public final CIField ").append(identifier)
                                    .append(" = new CIField(this, \"").append(entry.getKey()).append("\"");
                    for (final String profile : entry.getValue()) {
                        java.append(", \"").append(profile).append("\"");
                    }
                    java.append(");\n");
                }
                java.append("    }\n\n");
             }
         }
         java.append("}\n");

        getOutputDirectory().mkdir();

        final String folders = this.ciPackage.replace(".", File.separator);
        final File srcFolder = new File(getOutputDirectory(), folders);
        srcFolder.mkdirs();

        final File javaFile = new File(srcFolder, _ciDef.classNamePrefix + this.ciName + ".java");

        FileUtils.writeStringToFile(javaFile, java.toString());
    }

    /**
     * Build the Java class for the CITypes.
     *
     * @throws IOException on error during writing of the file
     */
    private void buildCIType()
        throws IOException
    {
        // there is a not unlikely chance to produce a duplicated Type,
        // therefore it is checked here
        final Map<String, String> typeTmp = new HashMap<String, String>();
        final Set<String> duplicated = new HashSet<String>();
        for (final Entry<String, ITypeCI> entry : this.types.entrySet()) {
            final String name = entry.getValue().getDefinitions().get(0).getName();
            String typeName = name.replaceAll(this.ciUnallowedRegex, this.ciUnallowedReplacement);
            typeName = typeName.replaceAll(this.ciTypeRegex == null ? (this.ciName + "_") : this.ciTypeRegex,
                            this.ciTypeReplacement);
            if (typeTmp.containsKey(typeName)) {
                duplicated.add(name);
                duplicated.add(typeTmp.get(typeName));
            } else {
                typeTmp.put(typeName, name);
            }
        }

        final StringBuilder java = new StringBuilder()
                .append("//CHECKSTYLE:OFF\n")
                .append("package ").append(this.ciPackage).append(";\n")
                .append("import org.efaps.ci.CIAttribute;\n")
                .append("import org.efaps.ci.CIType;\n\n")
                .append(getClassComment())
                .append("public final class CI").append(this.ciName).append("\n{\n");

        for (final Entry<String, ITypeCI> entry : this.types.entrySet()) {
            final ITypeDefintion def = entry.getValue().getDefinitions().get(0);
            final String name = def.getName();
            String typeName = name.replaceAll(this.ciUnallowedRegex, this.ciUnallowedReplacement);
            if (!duplicated.contains(name)) {
                typeName = typeName.replaceAll(this.ciTypeRegex == null ? (this.ciName + "_") : this.ciTypeRegex,
                                this.ciTypeReplacement);
            }

            String parentType = null;
            if (def.getParent() != null) {
                parentType = def.getParent().replaceAll(this.ciUnallowedRegex, this.ciUnallowedReplacement);
                if (!duplicated.contains(def.getParent())) {
                    parentType = parentType.replaceAll(this.ciTypeRegex == null
                                    ? (this.ciName + "_") : this.ciTypeRegex, this.ciTypeReplacement);
                }
                parentType = "_" + parentType;
                if (!this.types.containsKey(def.getParent())) {
                    final String parentClass = def.getParent().replaceAll(this.ciParentRegex, this.ciParentReplacment);
                    parentType = "org.efaps.esjp.ci.CI" + parentClass + "." + parentType;
                }
            }

            java.append("    public static final _").append(typeName).append(" ").append(typeName)
                .append(" = new _").append(typeName).append("(\"").append(entry.getValue().getUuid())
                .append("\");\n")
                .append("    public static class _").append(typeName).append(" extends ")
                .append(parentType == null ? "CIType" : parentType)
                .append("\n    {\n")
                .append("        protected _").append(typeName).append("(final String _uuid)\n        {\n")
                .append("            super(_uuid);")
                .append("\n        }\n");

            final Map<String, List<String>> attributes = new TreeMap<String, List<String>>();
            for (final ITypeDefintion typeDef : entry.getValue().getDefinitions()) {
                for (final String attribute : typeDef.getAttributes()) {
                    List<String> profiles;
                    if (attributes.containsKey(attribute)) {
                        profiles = attributes.get(attribute);
                    } else {
                        profiles = new ArrayList<String>();
                    }
                    profiles.addAll(typeDef.getProfiles());
                    attributes.put(attribute, profiles);
                }
            }
            for (final Entry<String, List<String>> attrEntry : attributes.entrySet()) {
                if (!"Type".equals(attrEntry.getKey())
                                && !"OID".equals(attrEntry.getKey()) && !"ID".equals(attrEntry.getKey())) {
                    // check if the attribute name can be used in java, if not
                    // extend the name
                    final String identifier = NameUtil.isValidJavaIdentifier(attrEntry.getKey())
                                    ? attrEntry.getKey() : attrEntry.getKey() + "_attr";
                    java.append("        public final CIAttribute ").append(identifier)
                            .append(" = new CIAttribute(this, \"").append(attrEntry.getKey()).append("\"");
                    for (final String profile : attrEntry.getValue()) {
                        java.append(", \"").append(profile).append("\"");
                    }
                    java.append(");\n");
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
    }

    private StringBuilder getClassComment()
    {
        final PluginDescriptor descriptor = (PluginDescriptor) this.pluginContext.get("pluginDescriptor");
        final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        final Calendar cal = Calendar.getInstance();
        final StringBuilder ret = new StringBuilder()
            .append("/**\n")
            .append(" * This class is build automatically by the \"").append(descriptor.getName())
            .append("\" Version \"").append(descriptor.getVersion()).append("\".\n *\n * Date: ")
            .append(dateFormat.format(cal.getTime())).append("\n")
            .append(" *\n")
            .append(" * @author The eFaps Team\n")
            .append("*/\n");
        return ret;
    }

    public interface UserInterfaceCI
    {
        CIDef4UI getCIDef();

        /**
         * @return
         */
        Collection<? extends UIDefintion> getDefinitions();

        /**
         * @return
         */
        String getUuid();

        /**
         * @return
         */
        String getName();
    }

    public interface UIDefintion
    {

        /**
         * @return
         */
        Collection<? extends String> getFields();

        /**
         * @return
         */
        Collection<? extends String> getProfiles();
    }

    public interface ITypeCI {
        List<? extends ITypeDefintion> getDefinitions();
        public String getUuid();
    }
    public interface ITypeDefintion
    {
        public String getName();
        public String getParent();
        Collection<? extends String> getAttributes();
        Collection<? extends String> getProfiles();
    }

    @ObjectCreate(pattern = "datamodel-type/definition")
    public static class TypeCIDefinition
        implements ITypeDefintion
    {

        @BeanPropertySetter(pattern = "datamodel-type/definition/version-expression")
        private String expression;
        @BeanPropertySetter(pattern = "datamodel-type/definition/name")
        private String name;
        @BeanPropertySetter(pattern = "datamodel-type/definition/parent")
        private String parent;

        private final List<String> attributes = new ArrayList<String>();
        private final List<String> profiles = new ArrayList<String>();

        /**
         * Getter method for the instance variable {@link #expression}.
         *
         * @return value of instance variable {@link #expression}
         */
        public String getExpression()
        {
            return this.expression;
        }

        /**
         * Setter method for instance variable {@link #expression}.
         *
         * @param _expression value for instance variable {@link #expression}
         */
        public void setExpression(final String _expression)
        {
            this.expression = _expression;
        }

        @CallMethod(pattern = "datamodel-type/definition/attribute")
        public void addAttribute(@CallParam(pattern = "datamodel-type/definition/attribute/name") final String _name)
        {
            this.attributes.add(_name);
        }

        @CallMethod(pattern = "datamodel-type/definition/profiles")
        public void addProfile(@CallParam(pattern = "datamodel-type/definition/profiles/profile",
                        attributeName = "name") final String _name)
        {
            this.profiles.add(_name);
        }

        /**
         * Getter method for the instance variable {@link #name}.
         *
         * @return value of instance variable {@link #name}
         */
        public String getName()
        {
            return this.name;
        }

        /**
         * Setter method for instance variable {@link #name}.
         *
         * @param _name value for instance variable {@link #name}
         */

        public void setName(final String _name)
        {
            this.name = _name;
        }

        /**
         * Getter method for the instance variable {@link #parent}.
         *
         * @return value of instance variable {@link #parent}
         */
        public String getParent()
        {
            return this.parent;
        }

        /**
         * Setter method for instance variable {@link #parent}.
         *
         * @param _parent value for instance variable {@link #parent}
         */

        public void setParent(final String _parent)
        {
            this.parent = _parent;
        }

        /**
         * Getter method for the instance variable {@link #attributes}.
         *
         * @return value of instance variable {@link #attributes}
         */
        public List<String> getAttributes()
        {
            return this.attributes;
        }

        /**
         * Getter method for the instance variable {@link #profiles}.
         *
         * @return value of instance variable {@link #profiles}
         */
        public List<String> getProfiles()
        {
            return this.profiles;
        }
    }

    @ObjectCreate(pattern = "datamodel-statusgroup/definition")
    public static class StatusCIDefinition
        implements ITypeDefintion
    {

        @BeanPropertySetter(pattern = "datamodel-statusgroup/definition/version-expression")
        private String expression;
        @BeanPropertySetter(pattern = "datamodel-statusgroup/definition/name")
        private String name;
        @BeanPropertySetter(pattern = "datamodel-statusgroup/definition/parent")
        private String parent;

        private final List<String> attributes = new ArrayList<String>();
        private final List<String> profiles = new ArrayList<String>();

        /**
         * Getter method for the instance variable {@link #expression}.
         *
         * @return value of instance variable {@link #expression}
         */
        public String getExpression()
        {
            return this.expression;
        }

        /**
         * Setter method for instance variable {@link #expression}.
         *
         * @param _expression value for instance variable {@link #expression}
         */
        public void setExpression(final String _expression)
        {
            this.expression = _expression;
        }

        @CallMethod(pattern = "datamodel-statusgroup/definition/attribute")
        public void addAttribute(@CallParam(pattern = "datamodel-statusgroup/definition/attribute/name") final String _name)
        {
            this.attributes.add(_name);
        }

        @CallMethod(pattern = "datamodel-statusgroup/definition/profiles")
        public void addProfile(@CallParam(pattern = "datamodel-statusgroup/definition/profiles/profile",
                        attributeName = "name") final String _name)
        {
            this.profiles.add(_name);
        }

        /**
         * Getter method for the instance variable {@link #name}.
         *
         * @return value of instance variable {@link #name}
         */
        public String getName()
        {
            return this.name;
        }

        /**
         * Setter method for instance variable {@link #name}.
         *
         * @param _name value for instance variable {@link #name}
         */

        public void setName(final String _name)
        {
            this.name = _name;
        }

        /**
         * Getter method for the instance variable {@link #parent}.
         *
         * @return value of instance variable {@link #parent}
         */
        public String getParent()
        {
            return this.parent;
        }

        /**
         * Setter method for instance variable {@link #parent}.
         *
         * @param _parent value for instance variable {@link #parent}
         */

        public void setParent(final String _parent)
        {
            this.parent = _parent;
        }

        /**
         * Getter method for the instance variable {@link #attributes}.
         *
         * @return value of instance variable {@link #attributes}
         */
        public List<String> getAttributes()
        {
            return this.attributes;
        }

        /**
         * Getter method for the instance variable {@link #profiles}.
         *
         * @return value of instance variable {@link #profiles}
         */
        public List<String> getProfiles()
        {
            return this.profiles;
        }
    }



    @ObjectCreate(pattern = "ui-form/definition")
    public static class FormCIDefinition
        implements UIDefintion
    {

        @BeanPropertySetter(pattern = "ui-form/definition/version-expression")
        private String expression;
        @BeanPropertySetter(pattern = "ui-form/definition/name")
        private String name;


        private final List<String> fields = new ArrayList<String>();
        private final List<String> profiles = new ArrayList<String>();

        /**
         * Getter method for the instance variable {@link #expression}.
         *
         * @return value of instance variable {@link #expression}
         */
        public String getExpression()
        {
            return this.expression;
        }

        /**
         * Setter method for instance variable {@link #expression}.
         *
         * @param _expression value for instance variable {@link #expression}
         */
        public void setExpression(final String _expression)
        {
            this.expression = _expression;
        }

        @CallMethod(pattern = "ui-form/definition/field")
        public void addField(@CallParam(pattern = "ui-form/definition/field",
                                                        attributeName = "name") final String _name)
        {
            this.fields.add(_name);
        }

        @CallMethod(pattern = "ui-form/definition/profiles")
        public void addProfile(@CallParam(pattern = "ui-form/definition/profiles/profile",
                                                        attributeName = "name") final String _name)
        {
            this.profiles.add(_name);
        }

        /**
         * Getter method for the instance variable {@link #name}.
         *
         * @return value of instance variable {@link #name}
         */
        public String getName()
        {
            return this.name;
        }

        /**
         * Setter method for instance variable {@link #name}.
         *
         * @param _name value for instance variable {@link #name}
         */
        public void setName(final String _name)
        {
            this.name = _name;
        }

        /**
         * Getter method for the instance variable {@link #attributes}.
         *
         * @return value of instance variable {@link #attributes}
         */
        public List<String> getFields()
        {
            return this.fields;
        }

        /**
         * Getter method for the instance variable {@link #profiles}.
         *
         * @return value of instance variable {@link #profiles}
         */
        public List<String> getProfiles()
        {
            return this.profiles;
        }
    }

    @ObjectCreate(pattern = "ui-table/definition")
    public static class TableCIDefinition
        implements UIDefintion
    {
        @BeanPropertySetter(pattern = "ui-table/definition/version-expression")
        private String expression;
        @BeanPropertySetter(pattern = "ui-table/definition/name")
        private String name;
        @BeanPropertySetter(pattern = "ui-table/definition/parent")
        private String parent;

        private final List<String> fields = new ArrayList<String>();
        private final List<String> profiles = new ArrayList<String>();

        /**
         * Getter method for the instance variable {@link #expression}.
         *
         * @return value of instance variable {@link #expression}
         */
        public String getExpression()
        {
            return this.expression;
        }

        /**
         * Setter method for instance variable {@link #expression}.
         *
         * @param _expression value for instance variable {@link #expression}
         */
        public void setExpression(final String _expression)
        {
            this.expression = _expression;
        }

        @CallMethod(pattern = "ui-table/definition/field")
        public void addField(@CallParam(pattern = "ui-table/definition/field/name") final String _name)
        {
            this.fields.add(_name);
        }

        @CallMethod(pattern = "ui-table/definition/profiles")
        public void addProfile(@CallParam(pattern = "ui-table/definition/profiles/profile",
                        attributeName = "name") final String _name)
        {
            this.profiles.add(_name);
        }

        /**
         * Getter method for the instance variable {@link #name}.
         *
         * @return value of instance variable {@link #name}
         */
        public String getName()
        {
            return this.name;
        }

        /**
         * Setter method for instance variable {@link #name}.
         *
         * @param _name value for instance variable {@link #name}
         */

        public void setName(final String _name)
        {
            this.name = _name;
        }

        /**
         * Getter method for the instance variable {@link #parent}.
         *
         * @return value of instance variable {@link #parent}
         */
        public String getParent()
        {
            return this.parent;
        }

        /**
         * Setter method for instance variable {@link #parent}.
         *
         * @param _parent value for instance variable {@link #parent}
         */

        public void setParent(final String _parent)
        {
            this.parent = _parent;
        }

        /**
         * Getter method for the instance variable {@link #attributes}.
         *
         * @return value of instance variable {@link #attributes}
         */
        public List<String> getFields()
        {
            return this.fields;
        }

        /**
         * Getter method for the instance variable {@link #profiles}.
         *
         * @return value of instance variable {@link #profiles}
         */
        public List<String> getProfiles()
        {
            return this.profiles;
        }
    }

    @ObjectCreate(pattern = "datamodel-type")
    public static class TypeCI
        implements ITypeCI
    {

        private final List<TypeCIDefinition> definitions = new ArrayList<TypeCIDefinition>();

        @BeanPropertySetter(pattern = "datamodel-type/uuid")
        private String uuid;

        /**
         * Getter method for the instance variable {@link #uuid}.
         *
         * @return value of instance variable {@link #uuid}
         */
        public String getUuid()
        {
            return this.uuid;
        }

        /**
         * Setter method for instance variable {@link #uuid}.
         *
         * @param _uuid value for instance variable {@link #uuid}
         */

        public void setUuid(final String _uuid)
        {
            this.uuid = _uuid;
        }

        /**
         * Getter method for the instance variable {@link #definitions}.
         *
         * @return value of instance variable {@link #definitions}
         */
        public List<TypeCIDefinition> getDefinitions()
        {
            return this.definitions;
        }

        @SetNext
        public void addDefinition(final TypeCIDefinition definition)
        {
            this.definitions.add(definition);
        }
    }

    @ObjectCreate(pattern = "datamodel-statusgroup")
    public static class StatusCI
        implements ITypeCI
    {

        private final List<StatusCIDefinition> definitions = new ArrayList<StatusCIDefinition>();

        @BeanPropertySetter(pattern = "datamodel-statusgroup/uuid")
        private String uuid;

        /**
         * Getter method for the instance variable {@link #uuid}.
         *
         * @return value of instance variable {@link #uuid}
         */
        public String getUuid()
        {
            return this.uuid;
        }

        /**
         * Setter method for instance variable {@link #uuid}.
         *
         * @param _uuid value for instance variable {@link #uuid}
         */

        public void setUuid(final String _uuid)
        {
            this.uuid = _uuid;
        }

        /**
         * Getter method for the instance variable {@link #definitions}.
         *
         * @return value of instance variable {@link #definitions}
         */
        public List<StatusCIDefinition> getDefinitions()
        {
            return this.definitions;
        }

        @SetNext
        public void addDefinition(final StatusCIDefinition definition)
        {
            this.definitions.add(definition);
        }
    }

    @ObjectCreate(pattern = "ui-form")
    public static class FormCI
        implements UserInterfaceCI
    {

        private final List<FormCIDefinition> definitions = new ArrayList<FormCIDefinition>();

        @BeanPropertySetter(pattern = "ui-form/uuid")
        private String uuid;

        /**
         * Getter method for the instance variable {@link #uuid}.
         *
         * @return value of instance variable {@link #uuid}
         */
        public String getUuid()
        {
            return this.uuid;
        }

        /**
         * Setter method for instance variable {@link #uuid}.
         *
         * @param _uuid value for instance variable {@link #uuid}
         */

        public void setUuid(final String _uuid)
        {
            this.uuid = _uuid;
        }

        /**
         * Getter method for the instance variable {@link #definitions}.
         *
         * @return value of instance variable {@link #definitions}
         */
        public List<FormCIDefinition> getDefinitions()
        {
            return this.definitions;
        }

        @SetNext
        public void addDefinition(final FormCIDefinition definition)
        {
            this.definitions.add(definition);
        }

        @Override
        public CIDef4UI getCIDef()
        {
            return CIDef4UI.FORM;
        }

        @Override
        public String getName()
        {
            return this.definitions.get(0).getName();
        }
    }

    @ObjectCreate(pattern = "ui-table")
    public static class TableCI
        implements UserInterfaceCI
    {

        private final List<TableCIDefinition> definitions = new ArrayList<TableCIDefinition>();

        @BeanPropertySetter(pattern = "ui-table/uuid")
        private String uuid;

        /**
         * Getter method for the instance variable {@link #uuid}.
         *
         * @return value of instance variable {@link #uuid}
         */
        public String getUuid()
        {
            return this.uuid;
        }

        /**
         * Setter method for instance variable {@link #uuid}.
         *
         * @param _uuid value for instance variable {@link #uuid}
         */

        public void setUuid(final String _uuid)
        {
            this.uuid = _uuid;
        }

        /**
         * Getter method for the instance variable {@link #definitions}.
         *
         * @return value of instance variable {@link #definitions}
         */
        public List<TableCIDefinition> getDefinitions()
        {
            return this.definitions;
        }

        @SetNext
        public void addDefinition(final TableCIDefinition definition)
        {
            this.definitions.add(definition);
        }

        @Override
        public CIDef4UI getCIDef()
        {
            return CIDef4UI.TABLE;
        }

        @Override
        public String getName()
        {
            return this.definitions.get(0).getName();
        }
    }
}
