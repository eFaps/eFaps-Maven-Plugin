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
import org.apache.commons.digester3.annotations.rules.ObjectCreate;
import org.apache.commons.digester3.annotations.rules.SetNext;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@ObjectCreate(pattern = "numbergenerator")
public class NumGenCI
    implements IBaseCI
{

    /** The definitions. */
    private final List<NumGenCIDefinition> definitions = new ArrayList<>();

    /** The uuid. */
    @BeanPropertySetter(pattern = "numbergenerator/uuid")
    private String uuid;

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

    /**
     * Getter method for the instance variable {@link #definitions}.
     *
     * @return value of instance variable {@link #definitions}
     */
    public List<NumGenCIDefinition> getDefinitions()
    {
        return this.definitions;
    }

    /**
     * Adds the definition.
     *
     * @param _definition the _definition
     */
    @SetNext
    public void addDefinition(final NumGenCIDefinition _definition)
    {
        this.definitions.add(_definition);
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName()
    {
        return this.definitions.get(0).getName();
    }
}
