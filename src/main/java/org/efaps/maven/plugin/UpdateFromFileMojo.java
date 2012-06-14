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
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.efaps.update.FileType;
import org.efaps.update.Install;
import org.efaps.update.Profile;
import org.efaps.update.util.InstallationException;
import org.efaps.util.EFapsException;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: $
 */
@MojoGoal(value = "updateFromFile")
public class UpdateFromFileMojo
    extends EFapsAbstractMojo
{

    /**
     * URL of the ESJP to import.
     */
    @MojoParameter(required = true)
    private File file;

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        try {
            init(true);
            reloadCache();
            startTransaction();

            final String ending = this.file.getName().substring(this.file.getName().lastIndexOf(".") + 1);

            final FileType filetype = FileType.getFileTypeByExensione(ending);

            final Install install = new Install();
            install.addFile(this.file.toURI().toURL(), filetype.getType());
            final Set<Profile> profiles = new HashSet<Profile>();
            profiles.add(Profile.getDefaultProfile());
            install.updateLatest(profiles);
            commitTransaction();
        } catch (final EFapsException e) {
            throw new MojoFailureException("import failed for file: " +  this.file.getName() + "; " + e.toString());
        } catch (final MalformedURLException e) {
            throw new MojoFailureException("import failed for file: " +  this.file.getName() + "; " + e.toString());
        } catch (final InstallationException e) {
            throw new MojoFailureException("import failed for file: " +  this.file.getName() + "; " + e.toString());
        }
    }
}
