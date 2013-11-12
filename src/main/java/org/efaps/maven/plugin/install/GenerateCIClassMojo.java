/*
 * Copyright 2003 - 2013 The eFaps Team
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.digester3.Digester;
import org.apache.commons.digester3.annotations.FromAnnotationsRuleModule;
import org.apache.commons.digester3.binder.DigesterLoader;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.ContextEnabled;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.xmlbeans.impl.common.NameUtil;
import org.efaps.maven.plugin.install.digester.FormCI;
import org.efaps.maven.plugin.install.digester.ITypeCI;
import org.efaps.maven.plugin.install.digester.ITypeDefintion;
import org.efaps.maven.plugin.install.digester.IUniqueCI;
import org.efaps.maven.plugin.install.digester.StatusCIDefinition;
import org.efaps.maven.plugin.install.digester.StatusGroupCI;
import org.efaps.maven.plugin.install.digester.TableCI;
import org.efaps.maven.plugin.install.digester.TypeCI;
import org.efaps.maven.plugin.install.digester.UIDefintion;
import org.efaps.maven.plugin.install.digester.UserInterfaceCI;
import org.efaps.update.FileType;
import org.efaps.update.Install.InstallFile;
import org.efaps.update.util.InstallationException;
import org.efaps.update.version.Application;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@Mojo(name = "generate-ciclass", requiresDependencyResolution = ResolutionScope.COMPILE,
                defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class GenerateCIClassMojo
    extends AbstractEFapsInstallMojo
    implements ContextEnabled
{

    /**
     * Definitions for a CI UserInterface object.
     */
    public enum CIDef4UI
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
    @Parameter(required = true)
    private String ciName;

    /**
     * The package name.
     */
    @Parameter(required = true, defaultValue = "org.efaps.esjp.ci")
    private String ciPackage;

    /**
     * This Regex will be used to replace the ciName with
     * {@link #ciNameReplacement}.
     */
    @Parameter(defaultValue = "^([A-Za-z]*)_")
    private String ciTypeRegex;

    /**
     * The replacement String used in conjunction with {@link #ciNameRegex}.
     */
    @Parameter(defaultValue = "")
    private final String ciTypeReplacement;

    /**
     * This Regex will be used to replace the unallowed Characters with
     * {@link #ciUnallowedReplacement}.
     */
    @Parameter(defaultValue = "-")
    private final String ciUnallowedRegex;

    /**
     * The replacement String used in conjunction with {@link #ciUnallowedRegex}
     * .
     */
    @Parameter(defaultValue = "")
    private final String ciUnallowedReplacement;

    /**
     * String to be used for String.indexof.
     */
    @Parameter(defaultValue = "_.+")
    private String ciParentRegex;

    /**
     * The replacement String used in conjunction with {@link #ciParentRegex}.
     */
    @Parameter(defaultValue = "")
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
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
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
                    bindRulesFrom(StatusGroupCI.class);
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
                if (getLog().isDebugEnabled()) {
                    getLog().debug("reading file:" + file);
                }
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
                .append("import org.efaps.ci.CIStatus;\n")
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

            final Map<String, List<String>> uniques = new TreeMap<String, List<String>>();
            for (final ITypeDefintion typeDef : entry.getValue().getDefinitions()) {
                for (final IUniqueCI unique : typeDef.getUniques()) {
                    List<String> profiles;
                    if (uniques.containsKey(unique.getIdentifier())) {
                        profiles = uniques.get(unique.getIdentifier());
                    } else {
                        profiles = new ArrayList<String>();
                    }
                    profiles.addAll(typeDef.getProfiles());
                    uniques.put(unique.getIdentifier(), profiles);
                }
            }
            for (final Entry<String, List<String>> attrEntry : uniques.entrySet()) {
                if (def instanceof StatusCIDefinition) {
                    // check if the attribute name can be used in java, if not
                    // extend the name
                    final String identifier = NameUtil.isValidJavaIdentifier(attrEntry.getKey())
                                    ? attrEntry.getKey() : attrEntry.getKey() + "_ci";
                    java.append("        public final CIStatus ").append(identifier)
                            .append(" = new CIStatus(this, \"").append(attrEntry.getKey()).append("\"");
                    for (final String profile : attrEntry.getValue()) {
                        java.append(", \"").append(profile).append("\"");
                    }
                    java.append(");\n");
                } else {
                    if (!"Type".equals(attrEntry.getKey())
                                    && !"OID".equals(attrEntry.getKey()) && !"ID".equals(attrEntry.getKey())) {
                        // check if the attribute name can be used in java, if not
                        // extend the name
                        final String identifier = NameUtil.isValidJavaIdentifier(attrEntry.getKey())
                                        ? attrEntry.getKey() : attrEntry.getKey() + "_ci";
                        java.append("        public final CIAttribute ").append(identifier)
                                .append(" = new CIAttribute(this, \"").append(attrEntry.getKey()).append("\"");
                        for (final String profile : attrEntry.getValue()) {
                            java.append(", \"").append(profile).append("\"");
                        }
                        java.append(");\n");
                    }
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
}
