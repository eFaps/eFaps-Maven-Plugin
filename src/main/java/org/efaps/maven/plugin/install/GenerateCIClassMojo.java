/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
 */
package org.efaps.maven.plugin.install;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
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
import org.efaps.maven.plugin.install.digester.MsgPhraseCI;
import org.efaps.maven.plugin.install.digester.NumGenCI;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author The eFaps Team
 */
@Mojo(name = "generate-ciclass", requiresDependencyResolution = ResolutionScope.COMPILE,
                defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class GenerateCIClassMojo
    extends AbstractEFapsInstallMojo
    implements ContextEnabled
{

    private static final Logger LOG = LoggerFactory.getLogger(GenerateCIClassMojo.class);

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
        CIDef4UI(final String _extendClass,
                         final String _classNamePrefix)
        {
            extendClass = _extendClass;
            classNamePrefix = _classNamePrefix;
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
    private final Map<String, ITypeCI> types = new TreeMap<>();

    /**
     * Set of Tables.
     */
    private final Set<UserInterfaceCI> uiCIs = new HashSet<>();

    /**
     * Set of MsgPhrases.
     */
    private final Set<MsgPhraseCI> msgPhraseCIs = new HashSet<>();

    /**
     * Set of MsgPhrases.
     */
    private final Set<NumGenCI> numGenCIs = new HashSet<>();

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
        ciTypeReplacement = "";
        ciParentReplacment = "";
        ciUnallowedReplacement = "";
        ciUnallowedRegex = "-";
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
    @Override
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
                    bindRulesFrom(MsgPhraseCI.class);
                    bindRulesFrom(NumGenCI.class);
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
                LOG.debug("reading file: {}", file);
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
                            types.put(((ITypeCI) item).getDefinitions().get(0).getName(), (ITypeCI) item);
                        } else if (item instanceof MsgPhraseCI) {
                            msgPhraseCIs.add((MsgPhraseCI) item);
                        } else if (item instanceof NumGenCI) {
                            numGenCIs.add((NumGenCI) item);
                        } else {
                            uiCIs.add((UserInterfaceCI) item);
                        }
                    }
                }
            }
            buildCIType(appl.getApplication());
            buildCI4UI(appl.getApplication(), CIDef4UI.FORM);
            buildCI4UI(appl.getApplication(), CIDef4UI.TABLE);
            buildCIMsgPhrase(appl.getApplication());
            buildCINumGen(appl.getApplication());
            project.addCompileSourceRoot(getOutputDirectory().getAbsolutePath());
        } catch (final SAXException | IOException  | InstallationException e) {
            LOG.error("Catched", e);
            throw new MojoExecutionException("SAXException");
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void setPluginContext(final Map _pluginContext)
    {
        pluginContext = _pluginContext;

    }

    @SuppressWarnings("rawtypes")
    @Override
    public Map getPluginContext()
    {
        return pluginContext;
    }

    /**
     * Build the CI UI File for a given CI Defintion.
     *
     * @param _appName the app name
     * @param _ciDef CI Definition the file will e build for
     * @throws IOException if writing of the actual file fails
     */
    private void buildCI4UI(final String _appName,
                            final CIDef4UI _ciDef)
        throws IOException
    {
        final StringBuilder java = new StringBuilder()
                        .append("//CHECKSTYLE:OFF\n")
                        .append("package ").append(ciPackage).append(";\n")
                        .append("import org.efaps.admin.program.esjp.EFapsApplication;\n")
                        .append("import org.efaps.ci.*;\n\n")
                        .append(getClassComment())
                        .append("@EFapsApplication(\"").append(_appName).append("\")\n")
                        .append("public final class ").append(_ciDef.classNamePrefix).append(ciName)
                        .append("\n{\n");

         for (final UserInterfaceCI uici : uiCIs) {
             if (uici.getCIDef().equals(_ciDef)) {
                 final String formName = uici.getName().replaceAll(ciUnallowedRegex,ciUnallowedReplacement);

                 java.append("    public static final _").append(formName).append(" ").append(formName)
                     .append(" = new _").append(formName).append("(\"").append(uici.getUuid()).append("\");\n")
                     .append("    public static class _").append(formName).append(" extends ")
                     .append(_ciDef.extendClass)
                     .append("\n    {\n")
                     .append("        protected _").append(formName).append("(final String _uuid)\n        {\n")
                     .append("            super(_uuid);")
                     .append("\n        }\n");

                final Map<String, List<String>> fields = new TreeMap<>();
                for (final UIDefintion uiDef : uici.getDefinitions()) {
                    for (final String field : uiDef.getFields()) {
                        List<String> profiles;
                        if (fields.containsKey(field)) {
                            profiles = fields.get(field);
                        } else {
                            profiles = new ArrayList<>();
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

        final String folders = ciPackage.replace(".", File.separator);
        final File srcFolder = new File(getOutputDirectory(), folders);
        srcFolder.mkdirs();

        final File javaFile = new File(srcFolder, _ciDef.classNamePrefix + ciName + ".java");

        FileUtils.writeStringToFile(javaFile, java.toString(), StandardCharsets.UTF_8);
    }

    /**
     * Build the Java class for the CITypes.
     *
     * @param _appName the app name
     * @throws IOException on error during writing of the file
     */
    private void buildCIType(final String _appName)
        throws IOException
    {
        // there is a not unlikely chance to produce a duplicated Type,
        // therefore it is checked here
        final Map<String, String> typeTmp = new HashMap<>();
        final Set<String> duplicated = new HashSet<>();
        for (final Entry<String, ITypeCI> entry : types.entrySet()) {
            final String name = entry.getValue().getDefinitions().get(0).getName();
            String typeName = name.replaceAll(ciUnallowedRegex, ciUnallowedReplacement);
            typeName = typeName.replaceAll(ciTypeRegex == null ? ciName + "_" : ciTypeRegex,
                            ciTypeReplacement);
            if (typeTmp.containsKey(typeName)) {
                duplicated.add(name);
                duplicated.add(typeTmp.get(typeName));
            } else {
                typeTmp.put(typeName, name);
            }
        }

        final StringBuilder java = new StringBuilder()
                .append("//CHECKSTYLE:OFF\n")
                .append("package ").append(ciPackage).append(";\n")
                .append("import org.efaps.admin.program.esjp.EFapsApplication;\n")
                .append("import org.efaps.ci.CIAttribute;\n")
                .append("import org.efaps.ci.CIStatus;\n")
                .append("import org.efaps.ci.CIType;\n\n")
                .append(getClassComment())
                .append("@EFapsApplication(\"").append(_appName).append("\")\n")
                .append("public final class CI").append(ciName).append("\n{\n");

        for (final Entry<String, ITypeCI> entry : types.entrySet()) {
            final ITypeDefintion def = entry.getValue().getDefinitions().get(0);
            final String name = def.getName();
            String typeName = name.replaceAll(ciUnallowedRegex, ciUnallowedReplacement);
            if (!duplicated.contains(name)) {
                typeName = typeName.replaceAll(ciTypeRegex == null ? ciName + "_" : ciTypeRegex,
                                ciTypeReplacement);
            }

            String parentType = null;
            if (def.getParent() != null) {
                parentType = def.getParent().replaceAll(ciUnallowedRegex, ciUnallowedReplacement);
                if (!duplicated.contains(def.getParent())) {
                    parentType = parentType.replaceAll(ciTypeRegex == null
                                    ? ciName + "_" : ciTypeRegex, ciTypeReplacement);
                }
                parentType = "_" + parentType;
                if (!types.containsKey(def.getParent())) {
                    final String parentClass = def.getParent().replaceAll(ciParentRegex, ciParentReplacment);
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

            final Map<String, List<String>> uniques = new TreeMap<>();
            for (final ITypeDefintion typeDef : entry.getValue().getDefinitions()) {
                for (final IUniqueCI unique : typeDef.getUniques()) {
                    List<String> profiles;
                    if (uniques.containsKey(unique.getIdentifier())) {
                        profiles = uniques.get(unique.getIdentifier());
                    } else {
                        profiles = new ArrayList<>();
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
                } else if (!"Type".equals(attrEntry.getKey())
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
            java.append("    }\n\n");
        }
        java.append("}\n");

        getOutputDirectory().mkdir();

        final String folders = ciPackage.replace(".", File.separator);
        final File srcFolder = new File(getOutputDirectory(), folders);
        srcFolder.mkdirs();

        final File javaFile = new File(srcFolder, "CI" + ciName + ".java");

        FileUtils.writeStringToFile(javaFile, java.toString(), StandardCharsets.UTF_8);
    }

    private StringBuilder getClassComment()
    {
        final PluginDescriptor descriptor = (PluginDescriptor) pluginContext.get("pluginDescriptor");
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

    /**
     * Builds the ci msg phrase.
     *
     * @param _appName the app name
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void buildCIMsgPhrase(final String _appName)
        throws IOException
    {
        final StringBuilder java = new StringBuilder()
                        .append("//CHECKSTYLE:OFF\n")
                        .append("package ").append(ciPackage).append(";\n")
                        .append("import org.efaps.admin.program.esjp.EFapsApplication;\n")
                        .append("import org.efaps.ci.*;\n\n")
                        .append(getClassComment())
                        .append("@EFapsApplication(\"").append(_appName).append("\")\n")
                        .append("public final class CIMsg").append(ciName)
                        .append("\n{\n");

        for (final MsgPhraseCI msgPhci : msgPhraseCIs) {
             String name = msgPhci.getName().replaceAll(ciUnallowedRegex, ciUnallowedReplacement);
             name = name.replaceAll(ciTypeRegex == null ? ciName + "_" : ciTypeRegex,
                             ciTypeReplacement);
             java.append("    public static final _").append(name).append(" ").append(name)
                 .append(" = new _").append(name).append("(\"").append(msgPhci.getUuid()).append("\");\n")
                 .append("    public static class _").append(name).append(" extends CIMsgPhrase")
                 .append("\n    {\n")
                 .append("        protected _").append(name).append("(final String _uuid)\n        {\n")
                 .append("            super(_uuid);")
                 .append("\n        }\n");
             java.append("    }\n\n");
        }
         java.append("}\n");

        getOutputDirectory().mkdir();

        final String folders = ciPackage.replace(".", File.separator);
        final File srcFolder = new File(getOutputDirectory(), folders);
        srcFolder.mkdirs();

        final File javaFile = new File(srcFolder, "CIMsg" + ciName + ".java");

        FileUtils.writeStringToFile(javaFile, java.toString(), StandardCharsets.UTF_8);
    }


    /**
     * Builds the ci msg phrase.
     *
     * @param _appName the app name
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void buildCINumGen(final String _appName)
        throws IOException
    {
        final StringBuilder java = new StringBuilder()
                        .append("//CHECKSTYLE:OFF\n")
                        .append("package ").append(ciPackage).append(";\n")
                        .append("import org.efaps.admin.program.esjp.EFapsApplication;\n")
                        .append("import org.efaps.ci.*;\n\n")
                        .append(getClassComment())
                        .append("@EFapsApplication(\"").append(_appName).append("\")\n")
                        .append("public final class CINumGen").append(ciName)
                        .append("\n{\n");

        for (final NumGenCI numGenci : numGenCIs) {
             String name = numGenci.getName().replaceAll(ciUnallowedRegex, ciUnallowedReplacement);
             name = name.replaceAll(ciTypeRegex == null ? ciName + "_" : ciTypeRegex,
                             ciTypeReplacement);
             java.append("    public static final _").append(name).append(" ").append(name)
                 .append(" = new _").append(name).append("(\"").append(numGenci.getUuid()).append("\");\n")
                 .append("    public static class _").append(name).append(" extends CINumGen")
                 .append("\n    {\n")
                 .append("        protected _").append(name).append("(final String _uuid)\n        {\n")
                 .append("            super(_uuid);")
                 .append("\n        }\n");
             java.append("    }\n\n");
        }
         java.append("}\n");

        getOutputDirectory().mkdir();

        final String folders = ciPackage.replace(".", File.separator);
        final File srcFolder = new File(getOutputDirectory(), folders);
        srcFolder.mkdirs();

        final File javaFile = new File(srcFolder, "CINumGen" + ciName + ".java");

        FileUtils.writeStringToFile(javaFile, java.toString(), StandardCharsets.UTF_8);
    }
}
