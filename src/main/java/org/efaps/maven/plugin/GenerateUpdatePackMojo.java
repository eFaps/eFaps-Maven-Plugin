/*
 * Copyright 2003 - 2019 The eFaps Team
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
 */

package org.efaps.maven.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipUtils;
import org.apache.commons.digester3.Digester;
import org.apache.commons.digester3.annotations.FromAnnotationsRuleModule;
import org.apache.commons.digester3.binder.DigesterLoader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.efaps.maven.plugin.install.AbstractEFapsInstallMojo;
import org.efaps.maven.plugin.install.digester.AccessSetCI;
import org.efaps.maven.plugin.install.digester.CommandCI;
import org.efaps.maven.plugin.install.digester.DBPropertiesCI;
import org.efaps.maven.plugin.install.digester.FormCI;
import org.efaps.maven.plugin.install.digester.IBaseCI;
import org.efaps.maven.plugin.install.digester.IRelatedFiles;
import org.efaps.maven.plugin.install.digester.ImageCI;
import org.efaps.maven.plugin.install.digester.JasperImageCI;
import org.efaps.maven.plugin.install.digester.MenuCI;
import org.efaps.maven.plugin.install.digester.ModuleCI;
import org.efaps.maven.plugin.install.digester.MsgPhraseCI;
import org.efaps.maven.plugin.install.digester.NumGenCI;
import org.efaps.maven.plugin.install.digester.RoleCI;
import org.efaps.maven.plugin.install.digester.SQLTableCI;
import org.efaps.maven.plugin.install.digester.SearchCI;
import org.efaps.maven.plugin.install.digester.StatusGroupCI;
import org.efaps.maven.plugin.install.digester.TableCI;
import org.efaps.maven.plugin.install.digester.TypeCI;
import org.efaps.update.FileType;
import org.efaps.update.Install.InstallFile;
import org.efaps.update.schema.program.esjp.ESJPImporter;
import org.efaps.update.schema.program.jasperreport.JasperReportImporter;
import org.efaps.update.schema.program.staticsource.CSSImporter;
import org.efaps.update.schema.program.staticsource.JavaScriptImporter;
import org.efaps.update.util.InstallationException;
import org.efaps.update.version.Application;
import org.efaps.update.version.Dependency;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;

/**
 *
 * @author The eFaps Team
 */
@Mojo(name = "generate-UpdatePack")
public class GenerateUpdatePackMojo
    extends AbstractEFapsInstallMojo
{
    private static final Logger LOG = LoggerFactory.getLogger(GenerateUpdatePackMojo.class);
    /**
     * The Enum UpdateGroup.
     *
     * @author The eFaps Team
     */
    public enum UpdateGroup
    {
        /** All CIItems. */
        ALL,
        /** Type, StatusGroup, SQLTable. */
        DATAMODEL,
        /** CIITems belongin to UserInterface. */
        UI,
        /** PROGRAM ITEMS. */
        PROGRAM;
    }

    /** The project. */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /** The target directory. */
    @Parameter(defaultValue = "${project.build.directory}")
    private File targetDirectory;

    /** The name of the generated pack file. */
    @Parameter(required = true, property = "updatePack.fileName", alias = "updatePack.fileName",
                    defaultValue = "UpdatePack")
    private String fileName;

    /** The name of the generated pack file. */
    @Parameter(required = true, property = "updatePack.group", alias = "updatePack.group",
                    defaultValue = "ALL")
    private UpdateGroup group;

    /** The compress. */
    @Parameter(property = "updatePack.compress", alias = "updatePack.compress", defaultValue = "true")
    private boolean compress;

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {

        final String tarFileName = fileName + ".tar";
        final String gzFileName = GzipUtils.getCompressedFilename(tarFileName);

        try
            (
                final FileOutputStream out = new FileOutputStream(new File(targetDirectory,
                                compress ? gzFileName : tarFileName));
                final TarArchiveOutputStream tarOut = new TarArchiveOutputStream(
                                compress ? new GzipCompressorOutputStream(out) : out);
            ) {

            final Application app = Application.getApplication(getVersionFile().toURI().toURL(),
                            getEFapsDir().toURI().toURL(), getClasspathElements());

            final DigesterLoader loader = DigesterLoader.newLoader(new FromAnnotationsRuleModule()
            {

                @Override
                protected void configureRules()
                {
                    switch (group) {
                        case PROGRAM:
                            break;
                        case DATAMODEL:
                            bindRulesFrom(TypeCI.class);
                            bindRulesFrom(StatusGroupCI.class);
                            bindRulesFrom(SQLTableCI.class);
                            break;
                        case UI:
                            bindRulesFrom(FormCI.class);
                            bindRulesFrom(TableCI.class);
                            bindRulesFrom(CommandCI.class);
                            bindRulesFrom(MenuCI.class);
                            bindRulesFrom(ModuleCI.class);
                            bindRulesFrom(SearchCI.class);
                            bindRulesFrom(DBPropertiesCI.class);
                            bindRulesFrom(ImageCI.class);
                            bindRulesFrom(JasperImageCI.class);
                            break;
                        case ALL:
                        default:
                            bindRulesFrom(TypeCI.class);
                            bindRulesFrom(StatusGroupCI.class);
                            bindRulesFrom(FormCI.class);
                            bindRulesFrom(TableCI.class);
                            bindRulesFrom(MsgPhraseCI.class);
                            bindRulesFrom(NumGenCI.class);
                            bindRulesFrom(CommandCI.class);
                            bindRulesFrom(MenuCI.class);
                            bindRulesFrom(ModuleCI.class);
                            bindRulesFrom(SearchCI.class);
                            bindRulesFrom(SQLTableCI.class);
                            bindRulesFrom(RoleCI.class);
                            bindRulesFrom(AccessSetCI.class);
                            bindRulesFrom(DBPropertiesCI.class);
                            bindRulesFrom(ImageCI.class);
                            bindRulesFrom(JasperImageCI.class);
                            break;
                    }
                }
            });

            final Map<String, RevItem> mapping = new HashMap<>();
            for (final Dependency dependency : app.getDependencies()) {
                dependency.resolve();
                final Application dependApp = Application.getApplicationFromJarFile(
                                dependency.getJarFile(), getClasspathElements());
                mapping.putAll(addItems(dependApp, tarOut, loader));
            }
            final Dependency dependency = new Dependency();
            dependency.setArtifactId(project.getArtifactId());
            dependency.setGroupId(project.getGroupId());
            dependency.setVersion(project.getVersion());
            dependency.resolve();

            final Application currentApp = Application.getApplicationFromJarFile(
                            dependency.getJarFile(), getClasspathElements());

            mapping.putAll(addItems(currentApp, tarOut, loader));

            final ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.registerModule(new JodaModule());
            final File revJson = new File(targetDirectory, "revisions.json");
            mapper.writeValue(revJson, mapping.values());

            final byte[] content = IOUtils.toByteArray(new FileInputStream(revJson));
            final TarArchiveEntry entry = new TarArchiveEntry("revisions.json");
            entry.setSize(content.length);
            tarOut.putArchiveEntry(entry);
            tarOut.write(content);
            tarOut.closeArchiveEntry();
        } catch (final Exception e) {
            throw new MojoExecutionException("Could not execute SourceInstall script", e);
        }
    }

    /**
     * Adds the items.
     *
     * @param _app the app
     * @param _tarOut the tar out
     * @param _loader the loader
     * @return the map
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SAXException the SAX exception
     * @throws URISyntaxException the URI syntax exception
     * @throws InstallationException the installation exception
     */
    private Map<String, RevItem> addItems(final Application _app,
                                          final TarArchiveOutputStream _tarOut,
                                          final DigesterLoader _loader)
        throws IOException, SAXException, URISyntaxException, InstallationException
    {
        final List<InstallFile> files = _app.getInstall().getFiles();
        final Map<String, RevItem> ret = new HashMap<>();
        for (final InstallFile file : files) {
            if (file.getType() == null) {
                LOG.error("File without FileType: {}", file);
            } else {
                switch (file.getType()) {
                    case XML:
                        final Digester digester = _loader.newDigester();
                        final URLConnection connection = file.getUrl().openConnection();
                        connection.setUseCaches(false);
                        final InputStream stream = connection.getInputStream();
                        final InputSource source = new InputSource(stream);
                        final IBaseCI item = digester.parse(source);
                        stream.close();
                        if (item != null && item.getUuid() != null) {
                            ret.put(item.getUuid(), new RevItem(FileType.XML, item.getUuid(), _app.getApplication(),
                                            file.getRevision(), file.getDate()));
                            final byte[] content = IOUtils.toByteArray(file.getUrl().openConnection().getInputStream());
                            final TarArchiveEntry entry = new TarArchiveEntry(item.getUuid());
                            entry.setSize(content.length);
                            _tarOut.putArchiveEntry(entry);
                            _tarOut.write(content);
                            _tarOut.closeArchiveEntry();
                            if (item instanceof IRelatedFiles) {
                                for (final String tmpFile : ((IRelatedFiles) item).getFiles()) {
                                    final String urlStr = file.getUrl().toString();
                                    final URL url = new URL(urlStr.substring(0, urlStr.lastIndexOf("/") + 1)
                                                    + tmpFile);
                                    final URLConnection relCon = url.openConnection();
                                    final byte[] relContent = IOUtils.toByteArray(relCon.getInputStream());
                                    final TarArchiveEntry relEntry = new TarArchiveEntry(tmpFile);
                                    relEntry.setSize(relContent.length);
                                    _tarOut.putArchiveEntry(relEntry);
                                    _tarOut.write(relContent);
                                    _tarOut.closeArchiveEntry();
                                }
                            }
                        }
                        break;
                    case JAVA:
                        if (UpdateGroup.ALL.equals(group) || UpdateGroup.PROGRAM.equals(group)) {
                            final ESJPImporter importer = new ESJPImporter(file);
                            final String identifier = importer.getProgramName();

                            ret.put(identifier, new RevItem(FileType.JAVA, identifier, _app.getApplication(),
                                            file.getRevision(), file.getDate()));
                            final byte[] content = IOUtils.toByteArray(file.getUrl().openConnection().getInputStream());
                            final TarArchiveEntry entry = new TarArchiveEntry(
                                            identifier.replace('.', '/') + ".java");
                            entry.setSize(content.length);
                            _tarOut.putArchiveEntry(entry);
                            _tarOut.write(content);
                            _tarOut.closeArchiveEntry();
                        }
                        break;
                    case CSS:
                        if (UpdateGroup.ALL.equals(group) || UpdateGroup.PROGRAM.equals(group)) {
                            final CSSImporter importer = new CSSImporter(file);
                            final String identifier = importer.getProgramName();

                            ret.put(identifier, new RevItem(FileType.CSS, identifier, _app.getApplication(),
                                            file.getRevision(), file.getDate()));
                            final byte[] content = IOUtils.toByteArray(file.getUrl().openConnection().getInputStream());
                            final TarArchiveEntry entry = new TarArchiveEntry(
                                            StringUtils.removeEnd(identifier, ".css").replace('.', '/') + ".css");
                            entry.setSize(content.length);
                            _tarOut.putArchiveEntry(entry);
                            _tarOut.write(content);
                            _tarOut.closeArchiveEntry();
                        }
                        break;
                    case JS:
                        if (UpdateGroup.ALL.equals(group) || UpdateGroup.PROGRAM.equals(group)) {
                            final JavaScriptImporter importer = new JavaScriptImporter(file);
                            final String identifier = importer.getProgramName();

                            ret.put(identifier, new RevItem(FileType.JS, identifier, _app.getApplication(),
                                            file.getRevision(), file.getDate()));
                            final byte[] content = IOUtils.toByteArray(file.getUrl().openConnection().getInputStream());
                            final TarArchiveEntry entry = new TarArchiveEntry(
                                            StringUtils.removeEnd(identifier, ".js").replace('.', '/') + ".js");
                            entry.setSize(content.length);
                            _tarOut.putArchiveEntry(entry);
                            _tarOut.write(content);
                            _tarOut.closeArchiveEntry();
                        }
                        break;
                    case JRXML:
                        if (UpdateGroup.ALL.equals(group) || UpdateGroup.PROGRAM.equals(group)) {
                            final JasperReportImporter importer = new JasperReportImporter(file);
                            final String identifier = importer.getEFapsUUID().toString();

                            ret.put(identifier, new RevItem(FileType.JRXML, identifier, _app.getApplication(),
                                            file.getRevision(), file.getDate()));
                            final byte[] content = IOUtils.toByteArray(file.getUrl().openConnection().getInputStream());
                            final TarArchiveEntry entry = new TarArchiveEntry(identifier + ".jrxml");
                            entry.setSize(content.length);
                            _tarOut.putArchiveEntry(entry);
                            _tarOut.write(content);
                            _tarOut.closeArchiveEntry();
                        }
                        break;
                    default:
                        LOG.debug("Ignoring: {}", file);
                        break;
                }
            }
        }
        return ret;
    }

    /**
     * The Class RevItem.
     *
     * @author The eFaps Team
     */
    public static class RevItem
    {

        /** The file type. */
        private final FileType fileType;

        /** The identifier. */
        private final String identifier;

        /** The application. */
        private final String application;

        /** The revision. */
        private final String revision;

        /** The date. */
        private final DateTime date;

        /**
         * Instantiates a new rev item.
         *
         * @param _fileType the file type
         * @param _identifier the identifier
         * @param _application the application
         * @param _revision the revision
         * @param _date the date
         */
        public RevItem(final FileType _fileType,
                       final String _identifier,
                       final String _application,
                       final String _revision,
                       final DateTime _date)
        {
            fileType = _fileType;
            identifier = _identifier;
            application = _application;
            revision = _revision;
            date = _date;
        }

        /**
         * Gets the file type.
         *
         * @return the file type
         */
        public FileType getFileType()
        {
            return fileType;
        }

        /**
         * Getter method for the instance variable {@link #application}.
         *
         * @return value of instance variable {@link #application}
         */
        public String getApplication()
        {
            return application;
        }

        /**
         * Getter method for the instance variable {@link #revision}.
         *
         * @return value of instance variable {@link #revision}
         */
        public String getRevision()
        {
            return revision;
        }

        /**
         * Getter method for the instance variable {@link #identifier}.
         *
         * @return value of instance variable {@link #identifier}
         */
        public String getIdentifier()
        {
            return identifier;
        }

        /**
         * Gets the date.
         *
         * @return the date
         */
        public DateTime getDate()
        {
            return date;
        }

        @Override
        public String toString()
        {
            return ToStringBuilder.reflectionToString(this);
        }
    }
}
