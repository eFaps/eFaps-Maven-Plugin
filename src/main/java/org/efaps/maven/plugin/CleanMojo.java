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

import java.sql.Connection;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.efaps.db.Context;


/**
 * Delete Old Data and Data Model within eFaps of current logged in SQL
 * database user (via dropping of all tables, views, constraints etc.)..
 *
 * @author The eFaps Team
 * @version $Id$
 */
@Mojo(name = "clean")
public class CleanMojo
    extends EFapsAbstractMojo
{

    /**
     * Initializes the database connection, starts a connection, deletes all
     * tables, views etc. from current logged in database user and commits
     * transaction.
     *
     * @throws MojoExecutionException if delete of old data and data model
     *             failed
     */
    public void execute()
        throws MojoExecutionException
    {
        init(true);
        try {
            getLog().info("Delete Old Data and Data Model");
            Context.begin();
            final Connection conn = Context.getThreadContext().getConnection();
            Context.getDbType().deleteAll(conn);
            if (!conn.getAutoCommit()) {
                conn.commit();
            }
            Context.commit();
        } catch (final Exception e) {
            throw new MojoExecutionException("Delete of Old Data and Data Model "
                            + "failed", e);
        }
    }
}
