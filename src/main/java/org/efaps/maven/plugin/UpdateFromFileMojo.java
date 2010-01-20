/*
 * Copyright 2003 - 2010 The eFaps Team
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
import java.net.MalformedURLException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.efaps.maven_java5.org.apache.maven.tools.plugin.Goal;
import org.efaps.maven_java5.org.apache.maven.tools.plugin.Parameter;
import org.efaps.update.FileType;
import org.efaps.update.Install;
import org.efaps.update.util.InstallationException;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: $
 */
@Goal(name = "updateFromFile")
public class UpdateFromFileMojo
    extends EFapsAbstractMojo
{

    /**
     * URL of the ESJP to import.
     */
    @Parameter(required = true)
    private File file;

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        try {
            init();
            reloadCache();
            startTransaction();

            final String ending = file.getName().substring(file.getName().lastIndexOf(".") + 1);

            final FileType filetype = FileType.getFileTypeByExensione(ending);

            final Install install = new Install();
            install.addFile(file.toURI().toURL(), filetype.type);

            install.addFile(file.toURI().toURL(), filetype.type);
            install.updateLatest();
            commitTransaction();
        } catch (final EFapsException e) {
            throw new MojoFailureException("import failed for file: " +  file.getName() + "; " + e.toString());
        } catch (final MalformedURLException e) {
            throw new MojoFailureException("import failed for file: " +  file.getName() + "; " + e.toString());
        } catch (final InstallationException e) {
            throw new MojoFailureException("import failed for file: " +  file.getName() + "; " + e.toString());
        }
    }
}
