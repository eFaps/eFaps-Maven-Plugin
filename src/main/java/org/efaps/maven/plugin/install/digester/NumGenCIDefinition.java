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
package org.efaps.maven.plugin.install.digester;

import org.apache.commons.digester3.annotations.rules.BeanPropertySetter;
import org.apache.commons.digester3.annotations.rules.ObjectCreate;

/**
 *
 * @author The eFaps Team
 */
@ObjectCreate(pattern = "numbergenerator/definition")
public class NumGenCIDefinition
{

    /** The name. */
    @BeanPropertySetter(pattern = "numbergenerator/definition/name")
    private String name;

    /**
     * Getter method for the instance variable {@link #name}.
     *
     * @return value of instance variable {@link #name}
     */
    public String getName()
    {
        return name;
    }

    /**
     * Setter method for instance variable {@link #name}.
     *
     * @param _name value for instance variable {@link #name}
     */
    public void setName(final String _name)
    {
        name = _name;
    }
}
