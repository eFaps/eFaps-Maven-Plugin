/*
 * Copyright 2003 - 2011 The eFaps Team
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


package org.efaps.maven.plugin.install.digester;

import org.apache.commons.digester3.annotations.rules.BeanPropertySetter;
import org.apache.commons.digester3.annotations.rules.ObjectCreate;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@ObjectCreate(pattern = "datamodel-type/definition/attributeset")
public class AttributeSetCI
    implements Comparable<AttributeSetCI>, IAttributeCI
{
    @BeanPropertySetter(pattern = "datamodel-type/definition/attributeset/name")
    private String name;

    @BeanPropertySetter(pattern = "datamodel-type/definition/attributeset/type")
    private String type;

    /**
     * Getter method for the instance variable {@link #name}.
     *
     * @return value of instance variable {@link #name}
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * Setter method for instance variable {@link #name}.
     *
     * @param _name value for instance variable {@link #name}
     */

    public void setName(final String _name)
    {
        this.name = _name;
    }

    /**
     * Getter method for the instance variable {@link #type}.
     *
     * @return value of instance variable {@link #type}
     */
    public String getType()
    {
        return this.type;
    }

    /**
     * Setter method for instance variable {@link #type}.
     *
     * @param _type value for instance variable {@link #type}
     */

    public void setType(final String _type)
    {
        this.type = _type;
    }

    @Override
    public int compareTo(final AttributeSetCI _arg0)
    {
        return getName().compareTo(_arg0.getName());
    }
}
