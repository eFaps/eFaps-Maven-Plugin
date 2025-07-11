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
package org.efaps.maven.plugin.install;

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.efaps.update.Profile;
import org.efaps.update.version.Application;

/**
 * Makes an update of an eFaps application for the last version of the
 * application.
 *
 * @author The eFaps Team
 */
@Mojo(name = "update", requiresDependencyResolution = ResolutionScope.COMPILE)
public final class UpdateMojo
    extends AbstractEFapsInstallMojo
{
    /**
     * Executes the update goal.
     *
     * @throws MojoExecutionException if update failed
     */
    @Override
    public void execute()
        throws MojoExecutionException
    {
        init(true);
        try  {
            final Application appl = Application.getApplicationFromClassPath(getApplication(),
                                                                             getClasspathElements());
            if (appl != null) {
                final Set<Profile> profiles = new HashSet<>();
                if (getProfile() != null) {
                    profiles.add(Profile.getProfile(getProfile()));
                } else {
                    profiles.add(Profile.getDefaultProfile());
                }
                // update applications
                appl.updateLastVersion(getUserName(), getPassWord(), profiles);
            }
        } catch (final Exception e)  {
            throw new MojoExecutionException("Could not execute Installation script", e);
        }
    }
}
