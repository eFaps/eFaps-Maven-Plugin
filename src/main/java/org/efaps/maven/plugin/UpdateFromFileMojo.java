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
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.efaps.update.FileType;
import org.efaps.update.Install;
import org.efaps.update.Install.InstallFile;
import org.efaps.update.Profile;
import org.efaps.update.util.InstallationException;
import org.efaps.util.EFapsException;

/**
 *
 * @author The eFaps Team
 */
@Mojo(name = "updateFromFile")
public class UpdateFromFileMojo
    extends EFapsAbstractMojo
{

    /**
     * URL of the ESJP to import.
     */
    @Parameter(required = true)
    private File file;

    /**
     * Using profile.
     */
    @Parameter(property = "profile", defaultValue = "eFaps")
    private String profile;

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

            final String ending = file.getName().substring(file.getName().lastIndexOf(".") + 1);

            final FileType filetype = FileType.getFileTypeByExtension(ending);

            final Install install = new Install();
            final FileInfo fileInfo = getFileInformation(file, true);

            install.addFile(new InstallFile().setURL(file.toURI().toURL()).setType(filetype.getType())
                            .setDate(fileInfo.getDate()).setRevision(fileInfo.getRev()));
            final Set<Profile> profiles = new HashSet<>();
            profiles.add(Profile.getProfile(profile));
            install.updateLatest(profiles);
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
