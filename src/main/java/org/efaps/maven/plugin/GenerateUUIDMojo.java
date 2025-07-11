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

import java.util.UUID;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A new universally unique identifier (UUID) is created and printed out.<br>
 * By adding <code>-DUUID.count=3</code> to the calling command line the number
 * of UUID generated can be set.<br>
 * e.g.<br>
 * <code>mvn efaps:generateUUID -DUUID.count=3</code>
 *
 * @author The eFaps Team
 */
@Mojo(name = "generateUUID")
public final class GenerateUUIDMojo
    extends AbstractMojo
{
    private static final Logger LOG = LoggerFactory.getLogger(CompileMojo.class);
    /**
     * Number of UUID's to generate.
     */
    @Parameter(property = "UUID.count", defaultValue = "1")
    private int count;

    /**
     * The new universally unique identifier is created and printed out with a
     * normal call to the mojo log info.
     *
     * @throws MojoExecutionException on error
     */
    @Override
    public void execute()
        throws MojoExecutionException
    {
        for (int i = 0; i < count; i++)  {
            final UUID uuid = UUID.randomUUID();
            LOG.info("UUID[" + (i + 1) + "] = " + uuid.toString());
        }
    }
}
