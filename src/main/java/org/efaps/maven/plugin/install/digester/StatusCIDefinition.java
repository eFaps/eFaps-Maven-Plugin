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
import java.util.Collections;
import java.util.List;

import org.apache.commons.digester3.annotations.rules.BeanPropertySetter;
import org.apache.commons.digester3.annotations.rules.CallMethod;
import org.apache.commons.digester3.annotations.rules.CallParam;
import org.apache.commons.digester3.annotations.rules.ObjectCreate;


/**
 *
 * @author The eFaps Team
 */
@ObjectCreate(pattern = "datamodel-statusgroup/definition")
public class StatusCIDefinition
    implements ITypeDefintion
{

    @BeanPropertySetter(pattern = "datamodel-statusgroup/definition/version-expression")
    private String expression;
    @BeanPropertySetter(pattern = "datamodel-statusgroup/definition/name")
    private String name;
    @BeanPropertySetter(pattern = "datamodel-statusgroup/definition/parent")
    private String parent;

    private final List<StatusCI> status = new ArrayList<>();

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

    @CallMethod(pattern = "datamodel-statusgroup/definition/status")
    public void addStatus(@CallParam(pattern = "datamodel-statusgroup/definition/status",
                    attributeName = "key") final String _key)
    {
        final StatusCI statusTmp = new StatusCI();
        statusTmp.setKey(_key);
        status.add(statusTmp);
    }

    @CallMethod(pattern = "datamodel-statusgroup/definition/profiles")
    public void addProfile(@CallParam(pattern = "datamodel-statusgroup/definition/profiles/profile",
                    attributeName = "name") final String _name)
    {
        profiles.add(_name);
    }

    /**
     * Getter method for the instance variable {@link #name}.
     *
     * @return value of instance variable {@link #name}
     */
    @Override
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
     * Getter method for the instance variable {@link #parent}.
     *
     * @return value of instance variable {@link #parent}
     */
    @Override
    public String getParent()
    {
        return parent;
    }

    /**
     * Setter method for instance variable {@link #parent}.
     *
     * @param _parent value for instance variable {@link #parent}
     */

    public void setParent(final String _parent)
    {
        parent = _parent;
    }

    /**
     * Getter method for the instance variable {@link #attributes}.
     *
     * @return value of instance variable {@link #attributes}
     */
    @Override
    public List<IAttributeCI> getAttributes()
    {
        return Collections.emptyList();
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

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends IUniqueCI> getUniques()
    {
        return status;
    }
}
