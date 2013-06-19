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

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.efaps.admin.runlevel.RunLevel;
import org.efaps.db.Context;
import org.efaps.init.StartupDatabaseConnection;
import org.efaps.init.StartupException;
import org.efaps.jaas.AppAccessHandler;
import org.efaps.maven.logger.SLF4JOverMavenLog;
import org.efaps.util.EFapsException;

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
}
