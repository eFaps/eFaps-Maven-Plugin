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

package org.efaps.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.efaps.admin.runlevel.RunLevel;
import org.efaps.db.Context;
import org.efaps.init.StartupDatabaseConnection;
import org.efaps.init.StartupException;
import org.efaps.jaas.AppAccessHandler;
import org.efaps.maven.logger.SLF4JOverMavenLog;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 *
 * @author The eFaps Team
 * @version $Id$
 */
public abstract class EFapsAbstractMojo
    implements Mojo
{
    /**
     * The apache maven logger is stored in this instance variable.
     *
     * @see #getLog
     * @see #setLog
     */
    private Log log = null;

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
    @Parameter(required = true)
    private String userName;

    /**
     * Stores the name of the logged in user.
     *
     * @see #login
     */
    @Parameter(required = true)
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
    @Parameter(property = "org.efaps.transaction.manager",
                    defaultValue = "com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple",
                    required = true)
    private String transactionManager;

    /**
     * Name of the class for the transaction Synchronization Registry.
     */
    @Parameter(property = "org.efaps.transaction.synchronizationRegistry",
           defaultValue = "com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple",
                    required = true)
    private String transactionSynchronizationRegistry;

    /**
     * Project classpath.
     */
    @Parameter(property = "project.compileClasspathElements", required = true, readonly = true)
    private List<String> classpathElements;

    /**
     * Root Directory with the XML installation files.
     */
    @Parameter(defaultValue = "${basedir}/.git")
    private File gitDir;

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
            Class.forName("org.efaps.maven.logger.SLF4JOverMavenLog");
            SLF4JOverMavenLog.LOGGER = getLog();
        } catch (final ClassNotFoundException e) {
        }

        try {
            if (_startupDB) {
                AppAccessHandler.init(null, new HashSet<UUID>());
                StartupDatabaseConnection.startup(this.type,
                                                  this.factory,
                                                  this.connection,
                                                  this.transactionManager,
                                                  this.transactionSynchronizationRegistry,
                                                  this.configProps);
            }
        } catch (final StartupException e) {
            getLog().error("Initialize Database Connection failed: " + e.toString());
        }
    }

    /**
     * Reloads the internal eFaps cache.
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
     * @throws EFapsException on error
     */
    protected void startTransaction()
        throws EFapsException
    {
        Context.begin(this.userName);
    }

    /**
     * Abort the transaction.
     * @throws EFapsException on error
     */
    protected void abortTransaction()
        throws EFapsException
    {
        Context.rollback();
    }

    /**
     * Commit the Transaction.
     * @throws EFapsException on error
     */
    protected void commitTransaction()
        throws EFapsException
    {
        Context.commit();
    }

     /**
     * This is the setter method for instance variable {@link #log}.
     *
     * @param _log new value for instance variable {@link #log}
     * @see #log
     * @see #getLog
     */
    @Override
    public void setLog(final Log _log)
    {
        this.log = _log;
    }

    /**
     * This is the getter method for instance variable {@link #log}.
     *
     * @return value of instance variable {@link #log}
     * @see #log
     * @see #setLog
     */
    @Override
    public Log getLog()
    {
        return this.log;
    }

    /**
     * This is the getter method for instance variable {@link #userName}.
     *
     * @return value of instance variable {@link #userName}
     * @see #userName
     */
    protected String getUserName()
    {
        return this.userName;
    }

    /**
     * This is the getter method for instance variable {@link #passWord}.
     *
     * @return value of instance variable {@link #passWord}
     * @see #passWord
     */
    protected String getPassWord()
    {
        return this.passWord;
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
        return this.classpathElements;
    }

    protected FileInfo getFileInformation(final File _file)
    {
        FileInfo ret = null;
        try {
            ret = new FileInfo();
            if (getLog().isDebugEnabled()) {
                getLog().debug("Searching FileInfo for: " + _file);
            }
            final Repository repository = getRepository(_file);
            final Git git = new Git(repository);

            for (final RevCommit commit : git.log()
                            .addPath(_file.getPath().replaceFirst(repository.getDirectory().getParent() + "/", ""))
                            .call()) {
                final PersonIdent authorIdent = commit.getAuthorIdent();
                final Date authorDate = authorIdent.getWhen();
                final TimeZone authorTimeZone = authorIdent.getTimeZone();
                final DateTime dateTime = new DateTime(authorDate.getTime(),
                                DateTimeZone.forTimeZone(authorTimeZone));
                ret.setDate(dateTime);
                ret.setRev(commit.getId().getName());
            }
        } catch (final NoHeadException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final GitAPIException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ret;
    }

    protected Map<String,FileInfo> getFileInformations(final File _file, final Set<String> _filesSet)
    {
        final Map<String, FileInfo> ret = new TreeMap<String, FileInfo>();

        try {
            final Repository repository = getRepository(_file);
            final String relPath = _file.getPath().replaceFirst(repository.getDirectory().getParent() + "/", "");
            final Map<String, String> fileMap = new HashMap<>();
            for (final String file: _filesSet) {
                fileMap.put(relPath + "/" + file, file);
            }
            final Git git = new Git(repository);
            ObjectId previous = repository.resolve("HEAD^{tree}");
            for (final RevCommit commit : git.log().call()) {

                final ObjectId older = commit.getTree();
                // prepare the two iterators to compute the diff between
                final ObjectReader reader = repository.newObjectReader();
                final CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
                oldTreeIter.reset(reader, older);
                final CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
                newTreeIter.reset(reader, previous);

                // finally get the list of changed files
                final List<DiffEntry> diffs= new Git(repository).diff()
                                .setNewTree(newTreeIter)
                                .setOldTree(oldTreeIter)
                                .call();
                for (final DiffEntry entry : diffs) {
                     if (fileMap.containsKey(entry.getNewPath())) {
                        final FileInfo info = new FileInfo();
                        final PersonIdent authorIdent = commit.getAuthorIdent();
                        final Date authorDate = authorIdent.getWhen();
                        final TimeZone authorTimeZone = authorIdent.getTimeZone();
                        final DateTime dateTime = new DateTime(authorDate.getTime(),
                                        DateTimeZone.forTimeZone(authorTimeZone));
                        info.setDate(dateTime);
                        info.setRev(commit.getId().getName());
                        ret.put(fileMap.get(entry.getNewPath()), info);
                        fileMap.remove(entry.getNewPath());
                    }
                     if (fileMap.isEmpty()) {
                         break;
                     }
                }
                previous = commit.getTree();
                if (fileMap.isEmpty()) {
                    break;
                }
            }
            if (!fileMap.isEmpty()) {
               for (final Entry<String, String> entry : fileMap.entrySet()) {
                   ret.put(entry.getValue(), new FileInfo().setDate(new DateTime()).setRev("-"));
               }
            }
        } catch (final Exception e) {
            System.out.println();
        }
        return ret;
    }

    protected Repository getRepository(final File _file)
        throws IOException
    {
        final RepositoryBuilder builder = new RepositoryBuilder();
        return builder.setWorkTree(_file).readEnvironment().findGitDir(_file).build();
    }

    public static class FileInfo
    {
        private String rev;

        private DateTime date;

        /**
         * Getter method for the instance variable {@link #rev}.
         *
         * @return value of instance variable {@link #rev}
         */
        public String getRev()
        {
            return this.rev;
        }

        /**
         * Setter method for instance variable {@link #rev}.
         *
         * @param _rev value for instance variable {@link #rev}
         */
        public FileInfo setRev(final String _rev)
        {
            this.rev = _rev;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #date}.
         *
         * @return value of instance variable {@link #date}
         */
        public DateTime getDate()
        {
            return this.date;
        }

        /**
         * Setter method for instance variable {@link #date}.
         *
         * @param _date value for instance variable {@link #date}
         */
        public FileInfo setDate(final DateTime _date)
        {
            this.date = _date;
            return this;
        }
    }
}
