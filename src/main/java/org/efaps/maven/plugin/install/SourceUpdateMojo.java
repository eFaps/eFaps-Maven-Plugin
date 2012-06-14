/*
 * Copyright 2003 - 2012 The eFaps Team
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

package org.efaps.maven.plugin.install;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.efaps.update.Profile;
import org.efaps.update.version.Application;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoRequiresDependencyResolution;

/**
 * Updates an eFaps application.
 *
 * @author The eFaps Team
 * @version $Id$
 */
@MojoGoal(value = "source-update")
@MojoRequiresDependencyResolution(value = "compile")
public final class SourceUpdateMojo
    extends AbstractEFapsInstallMojo
{
    /**
     * List of includes.
     */
    private final List<String> includes = null;

    /**
     * List of excludes.
     */
    private final List<String> excludes = null;

    /**
     * Executes the install goal.
     *
     * @throws MojoExecutionException if installation failed
     */
    public void execute()
        throws MojoExecutionException
    {
        init(true);

        try {
            final Application appl = Application.getApplicationFromSource(
                    getVersionFile(),
                    getClasspathElements(),
                    getEFapsDir(),
                    getOutputDirectory(),
                    this.includes,
                    this.excludes,
                    getTypeMapping());

            final Set<Profile> profiles = new HashSet<Profile>();
            if (getProfile() != null) {
                profiles.add(Profile.getProfile(getProfile()));
            } else {
                profiles.add(Profile.getDefaultProfile());
            }
            // install applications
            if (appl != null) {
                appl.updateLastVersion(getUserName(), getPassWord(), profiles);
            }
        } catch (final Exception e) {
            throw new MojoExecutionException("Could not execute SourceInstall script", e);
        }
    }
}
