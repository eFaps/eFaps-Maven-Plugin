/*
 * Copyright 2003 - 2016 The eFaps Team
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
 * TODO comment!
 *
 * @author The eFaps Team
 */
@ObjectCreate(pattern = "dbproperties")
public class DBPropertiesCI
    implements IBaseCI
{
    @BeanPropertySetter(pattern = "dbproperties/uuid")
    private String uuid;

    /** The files. */
    private final List<String> files = new ArrayList<>();

    /**
     * Getter method for the instance variable {@link #uuid}.
     *
     * @return value of instance variable {@link #uuid}
     */
    @Override
    public String getUuid()
    {
        return this.uuid;
    }

    /**
     * Setter method for instance variable {@link #uuid}.
     *
     * @param _uuid value for instance variable {@link #uuid}
     */
    public void setUuid(final String _uuid)
    {
        this.uuid = _uuid;
    }

    @CallMethod(pattern = "dbproperties/resource/file", usingElementBodyAsArgument=true)
    public void addDefinition(final String _file)
    {
        this.files.add(_file);
    }

}
