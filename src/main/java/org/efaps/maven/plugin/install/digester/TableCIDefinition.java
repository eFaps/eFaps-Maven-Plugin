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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.digester3.annotations.rules.BeanPropertySetter;
import org.apache.commons.digester3.annotations.rules.CallMethod;
import org.apache.commons.digester3.annotations.rules.CallParam;
import org.apache.commons.digester3.annotations.rules.ObjectCreate;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@ObjectCreate(pattern = "ui-table/definition")
public class TableCIDefinition
    implements UIDefintion
{
    @BeanPropertySetter(pattern = "ui-table/definition/version-expression")
    private String expression;
    @BeanPropertySetter(pattern = "ui-table/definition/name")
    private String name;
    @BeanPropertySetter(pattern = "ui-table/definition/parent")
    private String parent;

    private final List<String> fields = new ArrayList<String>();
    private final List<String> profiles = new ArrayList<String>();


    /**
     * Getter method for the instance variable {@link #expression}.
     *
     * @return value of instance variable {@link #expression}
     */
    public String getExpression()
    {
        return this.expression;
    }

    /**
     * Setter method for instance variable {@link #expression}.
     *
     * @param _expression value for instance variable {@link #expression}
     */
    public void setExpression(final String _expression)
    {
        this.expression = _expression;
    }

    @CallMethod(pattern = "ui-table/definition/field")
    public void addField(@CallParam(pattern = "ui-table/definition/field/name") final String _name)
    {
        this.fields.add(_name);
    }

    @CallMethod(pattern = "ui-table/definition/profiles")
    public void addProfile(@CallParam(pattern = "ui-table/definition/profiles/profile",
                    attributeName = "name") final String _name)
    {
        this.profiles.add(_name);
    }

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
     * Getter method for the instance variable {@link #parent}.
     *
     * @return value of instance variable {@link #parent}
     */
    public String getParent()
    {
        return this.parent;
    }

    /**
     * Setter method for instance variable {@link #parent}.
     *
     * @param _parent value for instance variable {@link #parent}
     */

    public void setParent(final String _parent)
    {
        this.parent = _parent;
    }

    /**
     * Getter method for the instance variable {@link #attributes}.
     *
     * @return value of instance variable {@link #attributes}
     */
    public List<String> getFields()
    {
        return this.fields;
    }

    /**
     * Getter method for the instance variable {@link #profiles}.
     *
     * @return value of instance variable {@link #profiles}
     */
    public List<String> getProfiles()
    {
        return this.profiles;
    }
}
