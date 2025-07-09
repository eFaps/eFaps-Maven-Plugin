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
import org.apache.commons.digester3.annotations.rules.SetNext;
import org.apache.commons.digester3.annotations.rules.SetProperty;


/**
 *
 * @author The eFaps Team
 */
@ObjectCreate(pattern = "datamodel-type/definition")
public class TypeCIDefinition
    implements ITypeDefintion
{

    @BeanPropertySetter(pattern = "datamodel-type/definition/version-expression")
    private String expression;
    @BeanPropertySetter(pattern = "datamodel-type/definition/name")
    private String name;
    @BeanPropertySetter(pattern = "datamodel-type/definition/parent")
    private String parent;

    @SetProperty(pattern = "datamodel-type/definition/purpose", attributeName = "abstract" )
    private boolean abstractType;

    @SetProperty(pattern = "datamodel-type/definition/classification", attributeName = "classification" )
    private boolean classification;

    @SetProperty(pattern = "datamodel-type/definition/purpose", attributeName = "GeneralInstance" )
    private boolean generalInstance;


    private final List<String> profiles = new ArrayList<>();

    private final List<IAttributeCI> attributes = new ArrayList<>();

    /**
     * Getter method for the instance variable {@link #abstractType}.
     *
     * @return value of instance variable {@link #abstractType}
     */
    public boolean isAbstractType()
    {
        return abstractType;
    }


    /**
     * Setter method for instance variable {@link #abstractType}.
     *
     * @param _abstractType value for instance variable {@link #abstractType}
     */

    public void setAbstractType(final boolean _abstractType)
    {
        abstractType = _abstractType;
    }


    /**
     * Getter method for the instance variable {@link #classification}.
     *
     * @return value of instance variable {@link #classification}
     */
    public boolean isClassification()
    {
        return classification;
    }


    /**
     * Setter method for instance variable {@link #classification}.
     *
     * @param _classification value for instance variable {@link #classification}
     */

    public void setClassification(final boolean _classification)
    {
        classification = _classification;
    }


    /**
     * Getter method for the instance variable {@link #generalInstance}.
     *
     * @return value of instance variable {@link #generalInstance}
     */
    public boolean isGeneralInstance()
    {
        return generalInstance;
    }


    /**
     * Setter method for instance variable {@link #generalInstance}.
     *
     * @param _generalInstance value for instance variable {@link #generalInstance}
     */

    public void setGeneralInstance(final boolean _generalInstance)
    {
        generalInstance = _generalInstance;
    }

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

    @SetNext
    public void addAttribute(final AttributeCI _attribute)
    {
        attributes.add(_attribute);
    }

    @SetNext
    public void addAttributeSet(final AttributeSetCI _attributeSet)
    {
        attributes.add(_attributeSet);
    }


    @CallMethod(pattern = "datamodel-type/definition/profiles")
    public void addProfile(@CallParam(pattern = "datamodel-type/definition/profiles/profile",
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
        return attributes;
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
        return getAttributes();
    }
}
