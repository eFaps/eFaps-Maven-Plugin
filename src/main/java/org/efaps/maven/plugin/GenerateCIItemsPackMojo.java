/*
 * Copyright 2003 - 2016 The eFaps Team
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
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.digester3.Digester;
import org.apache.commons.digester3.annotations.FromAnnotationsRuleModule;
import org.apache.commons.digester3.binder.DigesterLoader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.efaps.maven.plugin.install.AbstractEFapsInstallMojo;
import org.efaps.maven.plugin.install.digester.AccessSetCI;
import org.efaps.maven.plugin.install.digester.CommandCI;
import org.efaps.maven.plugin.install.digester.FormCI;
import org.efaps.maven.plugin.install.digester.IBaseCI;
import org.efaps.maven.plugin.install.digester.MenuCI;
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
import org.efaps.update.version.Application;
import org.efaps.update.version.Dependency;
import org.joda.time.DateTime;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@Mojo(name = "generate-CIItemsPack")
public class GenerateCIItemsPackMojo
    extends AbstractEFapsInstallMojo
{

    /** The project. */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /** The target directory. */
    @Parameter(defaultValue = "${project.build.directory}")
    private File targetDirectory;

    /** The name of the generated pack file. */
    @Parameter(required = true, defaultValue = "CIItems.tar")
    private String ciItemsPackFileName;

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        try {
            final Application app = Application.getApplication(getVersionFile().toURI().toURL(),
                            getEFapsDir().toURI().toURL(), getClasspathElements());

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
                    bindRulesFrom(CommandCI.class);
                    bindRulesFrom(MenuCI.class);
                    bindRulesFrom(SearchCI.class);
                    bindRulesFrom(SQLTableCI.class);
                    bindRulesFrom(RoleCI.class);
                    //bindRulesFrom(ImageCI.class);
                    bindRulesFrom(AccessSetCI.class);
                }
            });
            final FileOutputStream out = new FileOutputStream(new File(this.targetDirectory, this.ciItemsPackFileName));
            final TarArchiveOutputStream tarOut = new TarArchiveOutputStream(out);

            final Map<String, RevItem> mapping = new HashMap<>();
            for (final Dependency dependency : app.getDependencies()) {
                dependency.resolve();
                final Application dependApp = Application.getApplicationFromJarFile(
                                dependency.getJarFile(), getClasspathElements());
                final List<InstallFile> files = dependApp.getInstall().getFiles();
                mapping.putAll(addItems(loader, dependApp.getApplication(), files, tarOut));
            }
            final Dependency dependency = new Dependency();
            dependency.setArtifactId(this.project.getArtifactId());
            dependency.setGroupId(this.project.getGroupId());
            dependency.setVersion(this.project.getVersion());
            dependency.resolve();

            final Application currentApp = Application.getApplicationFromJarFile(
                            dependency.getJarFile(), getClasspathElements());

            mapping.putAll(addItems(loader, currentApp.getApplication(), currentApp.getInstall().getFiles(), tarOut));

            final ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.registerModule(new JodaModule());
            final File revJson = new File(this.targetDirectory, "revisions.json");
            mapper.writeValue(revJson, mapping.values());

            final byte[] content = IOUtils.toByteArray(new FileInputStream(revJson));
            final TarArchiveEntry entry = new TarArchiveEntry("revisions.json");
            entry.setSize(content.length);
            tarOut.putArchiveEntry(entry);
            tarOut.write(content);
            tarOut.closeArchiveEntry();
            tarOut.close();

        } catch (final Exception e) {
            throw new MojoExecutionException("Could not execute SourceInstall script", e);
        }
    }

    /**
     * Adds the items.
     *
     * @param _loader the loader
     * @param _appName the app name
     * @param _files the files
     * @return the map
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SAXException the SAX exception
     * @throws URISyntaxException
     */
    private Map<String, RevItem> addItems(final DigesterLoader _loader,
                                          final String _appName,
                                          final List<InstallFile> _files,
                                          final TarArchiveOutputStream _tarOut)
        throws IOException, SAXException, URISyntaxException
    {
        final Map<String, RevItem> ret = new HashMap<>();
        for (final InstallFile file : _files) {
            if (FileType.XML.equals(file.getType())) {
                final Digester digester = _loader.newDigester();
                final URLConnection connection = file.getUrl().openConnection();
                connection.setUseCaches(false);
                final InputStream stream = connection.getInputStream();
                final InputSource source = new InputSource(stream);
                final IBaseCI item = digester.parse(source);
                stream.close();
                if (item != null && item.getUuid() != null) {
                    ret.put(item.getUuid(), new RevItem(item.getUuid(), _appName, file.getRevision(), file.getDate()));
                    final byte[] content = IOUtils.toByteArray(file.getUrl().openConnection().getInputStream());
                    final TarArchiveEntry entry = new TarArchiveEntry(item.getUuid() + ".xml");
                    entry.setSize(content.length);
                    _tarOut.putArchiveEntry(entry);
                    _tarOut.write(content);
                    _tarOut.closeArchiveEntry();
                } else {
                    getLog().debug("Ignoring: " + file);
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

        /** The uuid. */
        private final String uuid;

        /** The application. */
        private final String application;

        /** The revision. */
        private final String revision;

        /** The date. */
        private final DateTime date;

        /**
         * Instantiates a new rev item.
         *
         * @param _uuid the uuid
         * @param _application the application
         * @param _revision the revision
         * @param _date the date
         */
        public RevItem(final String _uuid,
                       final String _application,
                       final String _revision,
                       final DateTime _date)
        {
            this.uuid = _uuid;
            this.application = _application;
            this.revision = _revision;
            this.date = _date;
        }

        /**
         * Getter method for the instance variable {@link #application}.
         *
         * @return value of instance variable {@link #application}
         */
        public String getApplication()
        {
            return this.application;
        }

        /**
         * Getter method for the instance variable {@link #revision}.
         *
         * @return value of instance variable {@link #revision}
         */
        public String getRevision()
        {
            return this.revision;
        }

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
         * Gets the date.
         *
         * @return the date
         */
        public DateTime getDate()
        {
            return this.date;
        }

        @Override
        public String toString()
        {
            return ToStringBuilder.reflectionToString(this);
        }
    }
}
