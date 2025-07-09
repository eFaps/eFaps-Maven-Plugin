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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.digester3.annotations.rules.BeanPropertySetter;
import org.apache.commons.digester3.annotations.rules.CallMethod;
import org.apache.commons.digester3.annotations.rules.CallParam;
import org.apache.commons.digester3.annotations.rules.ObjectCreate;


/**
 *
 * @author The eFaps Team
 */
@ObjectCreate(pattern = "ui-form/definition")
public class FormCIDefinition
    implements UIDefintion
{

    @BeanPropertySetter(pattern = "ui-form/definition/version-expression")
    private String expression;
    @BeanPropertySetter(pattern = "ui-form/definition/name")
    private String name;


    private final List<String> fields = new ArrayList<>();
    private final List<String> profiles = new ArrayList<>();

    /**
     * Getter method for the instance variable {@link #expression}.
     *
     * @return value of instance variable {@link #expression}
     */
    public String getExpression()
    {
        return expression;
    }

    /**
     * Setter method for instance variable {@link #expression}.
     *
     * @param _expression value for instance variable {@link #expression}
     */
    public void setExpression(final String _expression)
    {
        expression = _expression;
    }

    @CallMethod(pattern = "ui-form/definition/field")
    public void addField(@CallParam(pattern = "ui-form/definition/field",
                                                    attributeName = "name") final String _name)
    {
        fields.add(_name);
    }

    @CallMethod(pattern = "ui-form/definition/profiles")
    public void addProfile(@CallParam(pattern = "ui-form/definition/profiles/profile",
                                                    attributeName = "name") final String _name)
    {
        profiles.add(_name);
    }

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

    /**
     * Getter method for the instance variable {@link #attributes}.
     *
     * @return value of instance variable {@link #attributes}
     */
    @Override
    public List<String> getFields()
    {
        return fields;
    }

    /**
     * Getter method for the instance variable {@link #profiles}.
     *
     * @return value of instance variable {@link #profiles}
     */
    @Override
    public List<String> getProfiles()
    {
        return profiles;
    }
}
