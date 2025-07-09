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
package org.efaps.maven.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;

import org.apache.commons.digester3.Digester;
import org.apache.commons.digester3.annotations.FromAnnotationsRuleModule;
import org.apache.commons.digester3.binder.DigesterLoader;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.efaps.admin.runlevel.RunLevel;
import org.efaps.db.Context;
import org.efaps.init.StartupDatabaseConnection;
import org.efaps.init.StartupException;
import org.efaps.jaas.AppAccessHandler;
import org.efaps.maven.plugin.install.digester.DBPropertiesCI;
import org.efaps.maven.plugin.install.digester.IRelatedFiles;
import org.efaps.maven.plugin.install.digester.ImageCI;
import org.efaps.maven.plugin.install.digester.JasperImageCI;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author The eFaps Team
 */
public abstract class EFapsAbstractMojo
    extends AbstractMojo
{

    private static final Logger LOG = LoggerFactory.getLogger(EFapsAbstractMojo.class);

    /**
     * Class name of the SQL database factory (implementing interface
     * {@link #javax.sql.DataSource}).
     *
     * @see javax.sql.DataSource
     * @see #initDatabase
     */
    @Parameter(required = true, property = "org.efaps.db.factory")
    private String factory;

    /**
     * Holds all properties of the connection to the database. The properties
     * are separated by a comma.
     */
    @Parameter(property = "org.efaps.db.connection", required = true)
    private String connection;

    /**
     * Stores the name of the logged in user.
     *
     * @see #login
     */
    @Parameter(required = true,  defaultValue = "-")
    private String userName;

    /**
     * Stores the name of the logged in user.
     *
     * @see #login
     */
    @Parameter(required = true, defaultValue = "-")
    private String passWord;

    /**
     * Defines the database type (used to define database specific
     * implementations).
     */
    @Parameter(property = "org.efaps.db.type", required = true)
    protected String type;

    /**
     * Value for the timeout of the transaction.
     */
    @Parameter(property = "org.efaps.configuration.properties", required = false)
    private String configProps;

    /**
     * Name of the class for the transaction manager.
     */
    @Parameter(property = "org.efaps.transaction.manager", defaultValue = "com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple", required = true)
    private String transactionManager;

    /**
     * Name of the class for the transaction Synchronization Registry.
     */
    @Parameter(property = "org.efaps.transaction.synchronizationRegistry", defaultValue = "com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple", required = true)
    private String transactionSynchronizationRegistry;

    /**
     * Project classpath.
     */
    @Parameter(property = "project.compileClasspathElements", required = true, readonly = true)
    private List<String> classpathElements;

    /**
     * Instantiates a new eFaps abstract mojo.
     */
    protected EFapsAbstractMojo()
    {
    }

    /**
     * @todo better way instead of catching class not found exception (needed
     *       for the shell!)
     * @param _startupDB start up the Database
     * @see #initStores
     * @see #convertToMap used to convert the connection string to a property
     *      map
     * @see #type database class
     * @see #factory factory class name
     * @see #connection connection properties
     */
    protected void init(final boolean _startupDB)
    {
        try {
            if (_startupDB) {
                AppAccessHandler.init(null, new HashSet<>());
                StartupDatabaseConnection.startup(type,
                                factory,
                                connection,
                                transactionManager,
                                transactionSynchronizationRegistry,
                                configProps);
            }
        } catch (final StartupException e) {
            LOG.error("Initialize Database Connection failed: " + e.toString());
        }
    }

    /**
     * Reloads the internal eFaps cache.
     *
     * @throws EFapsException on error
     */
    protected void reloadCache()
        throws EFapsException
    {
        Context.begin();
        RunLevel.init("shell");
        RunLevel.execute();
        abortTransaction();
    }

    /**
     * Start the transaction.
     *
     * @throws EFapsException on error
     */
    protected void startTransaction()
        throws EFapsException
    {
        Context.begin(userName);
    }

    /**
     * Abort the transaction.
     *
     * @throws EFapsException on error
     */
    protected void abortTransaction()
        throws EFapsException
    {
        Context.rollback();
    }

    /**
     * Commit the Transaction.
     *
     * @throws EFapsException on error
     */
    protected void commitTransaction()
        throws EFapsException
    {
        Context.commit();
    }

    /**
     * This is the getter method for instance variable {@link #userName}.
     *
     * @return value of instance variable {@link #userName}
     * @see #userName
     */
    protected String getUserName()
    {
        return userName;
    }

    /**
     * This is the getter method for instance variable {@link #passWord}.
     *
     * @return value of instance variable {@link #passWord}
     * @see #passWord
     */
    protected String getPassWord()
    {
        return passWord;
    }

    /**
     * This is the getter method for instance variable
     * {@link #classpathElements}.
     *
     * @return value of instance variable {@link #classpathElements}
     * @see #classpathElements
     */
    protected List<String> getClasspathElements()
    {
        return classpathElements;
    }

    /**
     * Gets the file information.
     *
     * @param _file the file
     * @return the file information
     */
    protected FileInfo getFileInformation(final File _file,
                                          final boolean _evalRelated)
    {
        FileInfo ret = null;
        try {
            ret = new FileInfo();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Searching FileInfo for: " + _file);
            }
            final Repository repository = getRepository(_file);
            final Git git = new Git(repository);

            final Iterator<RevCommit> iter = git.log().addPath(_file.getPath().replaceFirst(repository.getDirectory()
                            .getParent() + "/", "")).call().iterator();

            if (iter.hasNext()) {
                final RevCommit commit = iter.next();
                final PersonIdent authorIdent = commit.getAuthorIdent();
                final Date authorDate = authorIdent.getWhen();
                final TimeZone authorTimeZone = authorIdent.getTimeZone();
                final DateTime dateTime = new DateTime(authorDate.getTime(), DateTimeZone.forTimeZone(authorTimeZone));
                ret.setDate(dateTime).setRev(commit.getId().getName());
            }
        } catch (final GitAPIException | IOException e) {
            LOG.error("Catched", e);
        }
        return ret;
    }

    /**
     * Gets the file informations.
     *
     * @param _file the file
     * @param _filesSet the files set
     * @param _evalRel the eval rel
     * @return the file informations
     */
    protected Map<String, FileInfo> getFileInformations(final File baseDir,
                                                        final File efapsDir,
                                                        final Set<String> filesSet)
    {
        final Map<String, FileInfo> ret = new TreeMap<>();
        try {
            final var relativePath = baseDir.toPath().relativize(efapsDir.toPath());

            final Repository repository = getRepository(baseDir);
            try (var git = new Git(repository)) {
                for (final var file : filesSet) {
                    final var gitLogs = git.log()
                                    .addPath(relativePath.toString() + "/" + file)
                                    .setMaxCount(1)
                                    .call()
                                    .iterator();
                    if (gitLogs.hasNext()) {
                        final var commit = gitLogs.next();
                        final var authorIdent = commit.getAuthorIdent();
                        final var authorDate = authorIdent.getWhen();
                        final var authorTimeZone = authorIdent.getTimeZone();
                        final var dateTime = new DateTime(authorDate.getTime(),
                                        DateTimeZone.forTimeZone(authorTimeZone));
                        ret.put(file, new FileInfo().setDate(dateTime).setRev(commit.getId().getName()));
                        LOG.info("Added: {}", file);
                    } else {
                        LOG.warn("Could not find any commit for: {}", file);
                        ret.put(file, new FileInfo().setDate(new DateTime()).setRev("-"));
                    }
                }
            } catch (final GitAPIException e) {

            }
        } catch (final IOException e) {
            LOG.error("Catched IOException on accesssing repository", e);
        }
        return ret;
    }

    /**
     * Eval related.
     *
     * @param _info the info
     * @param _file the file
     */
    protected void evalRelated(final FileInfo _info,
                               final File _file)
    {
        try {
            if (_file.exists() && FilenameUtils.isExtension(_file.getName(), "xml")) {
                final DigesterLoader loader = DigesterLoader.newLoader(new FromAnnotationsRuleModule()
                {

                    @Override
                    protected void configureRules()
                    {
                        bindRulesFrom(DBPropertiesCI.class);
                        bindRulesFrom(ImageCI.class);
                        bindRulesFrom(JasperImageCI.class);
                    }
                });
                final Digester digester = loader.newDigester();
                final InputStream stream = new FileInputStream(_file);
                final InputSource source = new InputSource(stream);
                final IRelatedFiles item = digester.parse(source);
                if (item != null) {
                    for (final String tmpFile : item.getFiles()) {
                        final String path = FilenameUtils.normalize(_file.getParent() + "/" + tmpFile);
                        final File relFile = new File(path);
                        if (relFile.exists()) {
                            final FileInfo relInfo = getFileInformation(relFile, false);
                            if (relInfo.getDate().isAfter(_info.getDate())) {
                                _info.setDate(relInfo.getDate()).setRev(relInfo.getRev());
                            }
                        }
                    }
                }
                stream.close();
            }
        } catch (final IOException | SAXException e) {
            LOG.error("Catched error in : " + _file, e);
        }
    }

    /**
     * Gets the repository.
     *
     * @param _file the file
     * @return the repository
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected Repository getRepository(final File file)
        throws IOException
    {
        final RepositoryBuilder builder = new RepositoryBuilder();
        return builder.setWorkTree(file).readEnvironment().findGitDir().build();
    }

    /**
     * The Class FileInfo.
     *
     * @author The eFaps Team
     */
    public static class FileInfo
    {

        /** The rev. */
        private String rev;

        /** The date. */
        private DateTime date;

        /**
         * Getter method for the instance variable {@link #rev}.
         *
         * @return value of instance variable {@link #rev}
         */
        public String getRev()
        {
            return rev;
        }

        /**
         * Setter method for instance variable {@link #rev}.
         *
         * @param _rev value for instance variable {@link #rev}
         */
        public FileInfo setRev(final String _rev)
        {
            rev = _rev;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #date}.
         *
         * @return value of instance variable {@link #date}
         */
        public DateTime getDate()
        {
            return date;
        }

        /**
         * Setter method for instance variable {@link #date}.
         *
         * @param _date value for instance variable {@link #date}
         */
        public FileInfo setDate(final DateTime _date)
        {
            date = _date;
            return this;
        }
    }
}
