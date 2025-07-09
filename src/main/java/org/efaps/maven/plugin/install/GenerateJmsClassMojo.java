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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.digester3.Digester;
import org.apache.commons.digester3.annotations.FromAnnotationsRuleModule;
import org.apache.commons.digester3.binder.DigesterLoader;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.efaps.maven.plugin.install.digester.IAttributeCI;
import org.efaps.maven.plugin.install.digester.ITypeDefintion;
import org.efaps.maven.plugin.install.digester.StatusGroupCI;
import org.efaps.maven.plugin.install.digester.TypeCI;
import org.efaps.update.FileType;
import org.efaps.update.Install.InstallFile;
import org.efaps.update.version.Application;
import org.efaps.update.version.Dependency;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author The eFaps Team
 */
@Mojo(name = "generate-jmsclass", requiresDependencyResolution = ResolutionScope.COMPILE,
            defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class GenerateJmsClassMojo
    extends AbstractEFapsInstallMojo
{
    /**
     * The base package name.
     */
    @Parameter(required = true, defaultValue = "org.efaps.esjp.jms")
    private String jmsPackage;

    /**
     * This Regex will be used to replace the application name
     * with {@link #jmsPackageReplacement}.
     */
    @Parameter(required = true, defaultValue = "eFaps-|eFapsApp-|eFapsLocalizations-")
    private String jmsPackageRegex;

    /**
     * The replacement String used in conjunction with {@link #jmsPackageRegex}.
     */
    @Parameter(defaultValue = "")
    private final String jmsPackageReplacement;

    /**
     * This Regex will be used to replace the Classname result
     * with {@link #jmsClassNameReplacement}.
     */
    @Parameter(required = true, defaultValue = "^[A-Za-z]+_")
    private String jmsClassNameRegex;

    /**
     * The replacement String used in conjunction with {@link #jmsClassNameRegex}.
     */
    @Parameter(defaultValue = "")
    private final String jmsClassNameReplacement;

    /**
     * The current Maven project.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * Mapping between the Types of eFaps and the package the responding class is in.
     */
    private final Map<String, String> type2package = new HashMap<>();

    /**
     * Mapping between the Types of eFaps and the responding class.
     */
    private final Map<String, String> type2ClassName = new TreeMap<>();


    /**
     * Constructor setting empty string defautl values.
     */
    public GenerateJmsClassMojo()
    {
        jmsClassNameReplacement = "";
        jmsPackageReplacement = "";
    }

    /**
     * Executes the install goal.
     *
     * @throws MojoExecutionException if installation failed
     */
    @Override
    public void execute()
        throws MojoExecutionException
    {
        try {
            init(false);
            getOutputDirectory().mkdir();
            final String folders = jmsPackage.replace(".", File.separator);
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

            for (final Dependency dependency : appl.getDependencies()) {
                dependency.resolve();
                final Application dependApp = Application.getApplicationFromJarFile(
                                dependency.getJarFile(), getClasspathElements());
                final List<InstallFile> files = dependApp.getInstall().getFiles();
                for (final InstallFile file : files) {
                    if (file.getType() != null && file.getType().equals(FileType.XML)) {
                        readFile(srcFolder, file);
                    }
                }
            }

            final List<InstallFile> files = appl.getInstall().getFiles();
            for (final InstallFile file : files) {
                if (file.getType() != null && file.getType().equals(FileType.XML)) {
                    readFile(srcFolder, file);
                }
            }
            project.addCompileSourceRoot(getOutputDirectory().getAbsolutePath());
        } catch (final Exception e) {
            throw new MojoExecutionException("Could not execute SourceInstall script", e);
        }
    }

    private void readFile(final File _srcFolder,
                          final InstallFile _file)
        throws MojoExecutionException
    {
        try {
            final DigesterLoader loader = DigesterLoader.newLoader(new FromAnnotationsRuleModule()
            {
                @Override
                protected void configureRules()
                {
                    bindRulesFrom(TypeCI.class);
                    bindRulesFrom(StatusGroupCI.class);
                }
            });
            final Digester digester = loader.newDigester();
            final URLConnection connection = _file.getUrl().openConnection();
            connection.setUseCaches(false);
            final InputStream stream = connection.getInputStream();
            final InputSource source = new InputSource(stream);
            final Object item = digester.parse(source);
            stream.close();
            if (item != null) {
                if (item instanceof final TypeCI typeItem) {
                    final String packageName = typeItem.getPackageName(jmsPackageRegex,
                                    jmsPackageReplacement);
                    final String className = typeItem.getClassName(jmsClassNameRegex,
                                    jmsClassNameReplacement);
                    type2package.put(typeItem.getName(), packageName);
                    type2ClassName.put(typeItem.getName(), "org.efaps.esjp.jms."
                                    + packageName + "." + className);
                    final File folder = new File(_srcFolder, packageName);
                    folder.mkdirs();
                    final File javaFile = new File(folder, className + ".java");
                    FileUtils.writeStringToFile(javaFile, getJava(typeItem), StandardCharsets.UTF_8);
                }
            }


        } catch (final SAXException e) {
            throw new MojoExecutionException("Could not execute SourceInstall script", e);
        } catch (final IOException e) {
            throw new MojoExecutionException("Could not execute SourceInstall script", e);
        }
    }

    private String getJava(final TypeCI _typeCI)
    {
        final StringBuilder ret = new StringBuilder();
        final String packageName = _typeCI.getPackageName(jmsPackageRegex, jmsPackageReplacement);
        final String className = _typeCI.getClassName(jmsClassNameRegex, jmsClassNameReplacement);


        ret.append("package org.efaps.esjp.jms.").append(packageName).append(";\n\n")
        .append("import jakarta.xml.bind.annotation.XmlAccessType;\n")
        .append("import jakarta.xml.bind.annotation.XmlAccessorType;\n")
        .append("import jakarta.xml.bind.annotation.XmlElement;\n")
        .append("import jakarta.xml.bind.annotation.XmlElementWrapper;\n")
        .append("import jakarta.xml.bind.annotation.XmlElements;\n")
        .append("import jakarta.xml.bind.annotation.XmlRootElement;\n")
        .append("import jakarta.xml.bind.annotation.XmlType;\n")
        .append("import org.efaps.esjp.jms.AbstractObject;\n")
        .append("import org.efaps.esjp.jms.AbstractClassificationObject;\n")
        .append("import org.efaps.esjp.jms.annotation.*;\n")
        .append("import org.efaps.esjp.jms.attributes.*;\n\n")
        .append("@XmlAccessorType(XmlAccessType.NONE)\n")
        .append("@XmlRootElement(name = \"").append(_typeCI.getName()).append("\")\n")
        .append("@XmlType(name = \"").append(packageName).append(".")
            .append(_typeCI.getName()).append("\")\n")
        .append("@Type(uuid = \"").append(_typeCI.getUuid()).append("\")\n")
        .append("public ").append(_typeCI.isAbstract() ? "abstract " : "").append("class ")
            .append(className).append("\n");

    final String extendStr;
    if (_typeCI.getParent() == null || _typeCI.getParent().equals("Admin_Abstract")) {
        if (_typeCI.isClassification()) {
            extendStr = "AbstractClassificationObject";
        } else {
            extendStr = "AbstractObject";
        }
    } else if (GenerateJmsClassMojo.this.type2package.containsKey(_typeCI.getParent())
                && !packageName.equals(GenerateJmsClassMojo.this.type2package.get(_typeCI.getParent()))) {
        extendStr = GenerateJmsClassMojo.this.jmsPackage + "."
                        + GenerateJmsClassMojo.this.type2package.get(_typeCI.getParent()) + "."
                        + _typeCI.getParent().replaceAll(GenerateJmsClassMojo.this.jmsClassNameRegex, "");
    } else {
        extendStr = _typeCI.getParent().replaceAll(GenerateJmsClassMojo.this.jmsClassNameRegex, "");
    }
    ret.append("   extends ").append(extendStr).append("\n")
        .append("{\n");

    final StringBuilder getter = new StringBuilder();

    final Map<IAttributeCI, List<String>> attributes = new TreeMap<>();
    for (final ITypeDefintion typeDef : _typeCI.getDefinitions()) {
        for (final IAttributeCI attribute : typeDef.getAttributes()) {
            List<String> profiles;
            if (attributes.containsKey(attribute)) {
                profiles = attributes.get(attribute);
            } else {
                profiles = new ArrayList<>();
            }
            profiles.addAll(typeDef.getProfiles());
            attributes.put(attribute, profiles);
        }
    }

    for (final Entry<IAttributeCI, List<String>> entry : attributes.entrySet()) {

        if (!"Type".equals(entry.getKey().getType())
                        && !"OID".equals(entry.getKey().getName()) && !"ID".equals(entry.getKey().getName())) {

            final StringBuilder profiles = new StringBuilder();
            if (!entry.getValue().isEmpty()) {
                profiles.append(", profiles = {");
                boolean first = true;
                for (final String profile  : entry.getValue()) {
                    if (first) {
                        first = false;
                    } else {
                        profiles.append(", ");
                    }
                    profiles.append("\"").append(profile).append("\"");
                }
                profiles.append("} ");
            }
            final StringBuilder setter = new StringBuilder();
            ret
                .append("    @XmlElement(name = \"").append(entry.getKey().getName().toLowerCase()).append("\")\n")
                .append("    private ");
            getter
                .append("    @Attribute(name = \"").append(entry.getKey().getName())
                    .append("\", method = MethodType.GETTER").append(profiles.length() > 0 ? profiles : "" )
                    .append(")\n")
                 .append("    public ");

            setter
                .append("    @Attribute(name = \"").append(entry.getKey().getName())
                    .append("\", method = MethodType.SETTER").append(profiles.length() > 0 ? profiles : "" )
                    .append(")\n")
                .append("    public void ");

            final String attrTypeTmp;
            if ("String".equals(entry.getKey().getType())) {
                attrTypeTmp = "StringAttribute ";
            } else if ("Long".equals(entry.getKey().getType())){
                attrTypeTmp = "LongAttribute ";
            } else if ("Integer".equals(entry.getKey().getType())){
                attrTypeTmp = "IntegerAttribute ";
            } else if ("Link".equals(entry.getKey().getType())){
                attrTypeTmp = "LinkAttribute ";
            } else if ("LinkWithRanges".equals(entry.getKey().getType())){
                attrTypeTmp = "LinkAttribute ";
            } else if ("Decimal".equals(entry.getKey().getType())){
                attrTypeTmp = "DecimalAttribute ";
            } else if ("Date".equals(entry.getKey().getType())){
                attrTypeTmp = "DateAttribute ";
            } else if ("DateTime".equals(entry.getKey().getType())){
                attrTypeTmp = "DateTimeAttribute ";
            } else if ("Status".equals(entry.getKey().getType())){
                attrTypeTmp = "StatusAttribute ";
            } else if ("Rate".equals(entry.getKey().getType())){
                attrTypeTmp = "RateAttribute ";
            } else {
                attrTypeTmp = "StringAttribute ";
            }

            String instanceVariable = entry.getKey().getName();
            if ("Class".equalsIgnoreCase(instanceVariable)) {
                instanceVariable = "Clazz";
            } else if ("Abstract".equalsIgnoreCase(instanceVariable)) {
                instanceVariable = "AbstractV";
            } else if ("Default".equalsIgnoreCase(instanceVariable)) {
                instanceVariable = "DefaultV";
            }
            ret
                 .append(attrTypeTmp).append(instanceVariable.toLowerCase()).append(";\n\n");
            getter
                .append(attrTypeTmp).append("get").append(instanceVariable).append("()\n")
                .append("    {\n")
                .append("        return this.").append(instanceVariable.toLowerCase()).append(";\n")
                .append("    }\n\n");
            setter
                .append("set").append(instanceVariable).append("(final ").append(attrTypeTmp).append("_")
                    .append(instanceVariable.toLowerCase()).append(")\n")
                .append("    {\n")
                .append("        this.").append(instanceVariable.toLowerCase()).append(" = _")
                    .append(instanceVariable.toLowerCase()).append(";\n")
                .append("    }\n\n");

            getter.append(setter);
        }
    }
    ret.append(getter).append("}\n");

    return ret.toString();
    }

    }
