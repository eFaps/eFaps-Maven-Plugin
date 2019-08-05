/*
 * Copyright 2003 - 2019 The eFaps Team
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
 */


package org.efaps.maven.plugin.install.digester;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.digester3.annotations.rules.BeanPropertySetter;
import org.apache.commons.digester3.annotations.rules.CallMethod;
import org.apache.commons.digester3.annotations.rules.ObjectCreate;

/**
 *
 * @author The eFaps Team
 */
@ObjectCreate(pattern = "jasper-image")
public class JasperImageCI
    implements IRelatedFiles
{
    /** The uuid. */
    @BeanPropertySetter(pattern = "jasper-image/uuid")
    private String uuid;

    /** The files. */
    private final List<String> files = new ArrayList<>();


    @Override
    public String getUuid()
    {
        return uuid;
    }
    /**
     * Setter method for instance variable {@link #uuid}.
     *
     * @param _uuid value for instance variable {@link #uuid}
     */
    public void setUuid(final String _uuid)
    {
        uuid = _uuid;
    }

    /**
     * Adds the definition.
     *
     * @param _file the file
     */
    @CallMethod(pattern = "jasper-image/definition/file", usingElementBodyAsArgument=true)
    public void addFile(final String _file)
    {
        files.add(_file);
    }

    @Override
    public List<String> getFiles()
    {
        return files;
    }
}
