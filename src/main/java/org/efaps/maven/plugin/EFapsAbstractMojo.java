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
import java.util.TimeZone;
import java.util.UUID;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
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

    private final Map<String, RevWalk> walkers = new HashMap<>();

    private final Map<String, ObjectId> heads = new HashMap<>();

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
        final FileInfo ret = new FileInfo();
        try {
            if (getLog().isDebugEnabled()) {
                getLog().debug("Searching FileInfo for: " + _file);
            }
            final RevWalk revWalk = getWalker(_file);
            final RevCommit commit = revWalk.next();
            if (commit != null) {
                ret.setRev(commit.getId().getName());
                RevCommit tmpCommit = commit;
                while (tmpCommit.getRawBuffer() == null && tmpCommit.getParentCount() > 0) {
                    tmpCommit = tmpCommit.getParent(0);
                }
                if (tmpCommit.getRawBuffer() != null) {
                    final PersonIdent authorIdent = tmpCommit.getAuthorIdent();
                    final Date authorDate = authorIdent.getWhen();
                    final TimeZone authorTimeZone = authorIdent.getTimeZone();
                    final DateTime dateTime = new DateTime(authorDate.getTime(),
                                    DateTimeZone.forTimeZone(authorTimeZone));
                    ret.setDate(dateTime);
                } else {
                    ret.setDate(new DateTime(new Date(commit.getCommitTime() * 1000L)));
                }
            } else {
                ret.setDate(new DateTime());
                ret.setRev("UNKNOWN");
            }
        } catch (RevisionSyntaxException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ret;
    }

    protected RevWalk getWalker(final File _file) throws IOException
    {
        RevWalk ret;
        final File gitDirTmp = evalGitDir(_file);
        if (this.walkers.containsKey(gitDirTmp.toString())) {
            ret = this.walkers.get(gitDirTmp.toString());
            ret.dispose();
            ret.markStart(ret.parseCommit(this.heads.get(gitDirTmp.toString())));
            ret.setTreeFilter(AndTreeFilter.create(
                            PathFilter.create(_file.getPath().replaceFirst(gitDirTmp.getParent() + "/", "")),
                            TreeFilter.ANY_DIFF));
        } else {
            final Repository repo = new FileRepository(gitDirTmp);
            final ObjectId lastCommitId = repo.resolve(Constants.HEAD);
            this.heads.put(gitDirTmp.toString(), lastCommitId);
            ret = new RevWalk(repo);
            ret.markStart(ret.parseCommit(lastCommitId));
            ret.setTreeFilter(AndTreeFilter.create(
                            PathFilter.create(_file.getPath().replaceFirst(repo.getWorkTree().getPath() + "/", "")),
                            TreeFilter.ANY_DIFF ));
            ret.sort(RevSort.TOPO, true);
            ret.sort(RevSort.COMMIT_TIME_DESC, true);
            this.walkers.put(gitDirTmp.toString(), ret);
        }
        return ret;
    }


    /**
     * @param _file
     * @return
     */
    protected File evalGitDir(final File _file)
    {
        File ret = null;
        File parent = _file.getParentFile();;
        while (parent != null) {
            ret = new File(parent, ".git");
            if (ret.exists()) {
                break;
            } else {
                parent = parent.getParentFile();
            }
        }
        return ret;
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
        public void setRev(final String _rev)
        {
            this.rev = _rev;
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
        public void setDate(final DateTime _date)
        {
            this.date = _date;
        }
    }
}
