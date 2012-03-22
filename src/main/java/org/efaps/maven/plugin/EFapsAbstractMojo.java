/*
 * Copyright 2003 - 2009 The eFaps Team
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.logging.Log;
import org.efaps.admin.runlevel.RunLevel;
import org.efaps.db.Context;
import org.efaps.init.StartupDatabaseConnection;
import org.efaps.init.StartupException;
import org.efaps.jaas.AppAccessHandler;
import org.efaps.maven.logger.SLF4JOverMavenLog;
import org.efaps.util.EFapsException;
import org.jfrog.maven.annomojo.annotations.MojoParameter;

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
    @MojoParameter(required = true, expression = "${org.efaps.db.factory}")
    private String factory;

    /**
     * Holds all properties of the connection to the database. The properties
     * are separated by a comma.
     */
    @MojoParameter(expression = "${org.efaps.db.connection}", required = true)
    private String connection;

    /**
     * Stores the name of the logged in user.
     *
     * @see #login
     */
    @MojoParameter(required = true)
    private String userName;

    /**
     * Stores the name of the logged in user.
     *
     * @see #login
     */
    @MojoParameter(required = true)
    private String passWord;

    /**
     * Defines the database type (used to define database specific
     * implementations).
     */
    @MojoParameter(expression = "${org.efaps.db.type}", required = true)
    protected String type;

    /**
     * Value for the timeout of the transaction.
     */
    @MojoParameter(expression = "${org.efaps.transaction.timeout}", required = false)
    private String transactionTimeout;

    /**
     *Name of the class for the transaction manager..
     */
    @MojoParameter(expression = "${org.efaps.transaction.manager}",
               required = true,
               defaultValue = "org.objectweb.jotm.Current")
    private String transactionManager;

    /**
     * Project classpath.
     */
    @MojoParameter(expression = "${project.compileClasspathElements}", required = true, readonly = true)
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
                AppAccessHandler.init(null, new HashSet<String>());
                StartupDatabaseConnection.startup(this.type, this.factory, convertToMap(this.connection),
                                              this.transactionManager, this.transactionTimeout == null
                                                                        ? null
                                                                        : Integer.parseInt(this.transactionTimeout));
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
     * Separates all key / value pairs of given text string.<br/>
     * Evaluation algorithm:<br/>
     * Separates the text by all found commas (only if in front of the comma is
     * no back slash). This are the key / value pairs. A key / value pair is
     * separated by the first equal ('=') sign.
     *
     * @param _text text string to convert to a key / value map
     * @return Map of strings with all found key / value pairs
     */
    protected Map<String, String> convertToMap(final String _text)
    {
        final Map<String, String> properties = new HashMap<String, String>();

        // separated all key / value pairs
        final Pattern pattern = Pattern.compile("(([^\\\\,])|(\\\\,)|(\\\\))*");
        final Matcher matcher = pattern.matcher(_text);

        while (matcher.find()) {
            final String group = matcher.group().trim();
            if (group.length() > 0) {
                // separated key from value
                final int index = group.indexOf('=');
                final String key = (index > 0) ? group.substring(0, index).trim() : group.trim();
                final String value = (index > 0) ? group.substring(index + 1).trim() : "";
                properties.put(key, value);
            }
        }

        return properties;
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
