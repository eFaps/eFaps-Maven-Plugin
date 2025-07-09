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
import org.apache.commons.digester3.annotations.rules.ObjectCreate;
import org.apache.commons.digester3.annotations.rules.SetNext;


/**
 *
 * @author The eFaps Team
 */
@ObjectCreate(pattern = "datamodel-type")
public class TypeCI
    implements ITypeCI
{

    private final List<TypeCIDefinition> definitions = new ArrayList<>();

    @BeanPropertySetter(pattern = "datamodel-type/uuid")
    private String uuid;


    @BeanPropertySetter(pattern = "datamodel-type/file-application")
    private String application;

    /**
     * Getter method for the instance variable {@link #application}.
     *
     * @return value of instance variable {@link #application}
     */
    public String getApplication()
    {
        return application;
    }


    /**
     * Setter method for instance variable {@link #application}.
     *
     * @param _application value for instance variable {@link #application}
     */

    public void setApplication(final String _application)
    {
        application = _application;
    }

    /**
     * Getter method for the instance variable {@link #uuid}.
     *
     * @return value of instance variable {@link #uuid}
     */
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
     * Getter method for the instance variable {@link #definitions}.
     *
     * @return value of instance variable {@link #definitions}
     */
    @Override
    public List<TypeCIDefinition> getDefinitions()
    {
        return definitions;
    }

    @SetNext
    public void addDefinition(final TypeCIDefinition _definition)
    {
        definitions.add(_definition);
    }


    /**
     * @return
     */
    public String getName()
    {
        return definitions.get(0).getName();
    }


    /**
     * @return
     */
    public String getPackageName(final String _jmsPackageRegex, final String _jmsPackageReplacement)
    {
         return application.trim().replaceAll(_jmsPackageRegex, _jmsPackageReplacement).toLowerCase();
    }


    public boolean isAbstract()
    {
        return definitions.get(0).isAbstractType();
    }

    /**
     * @param _jmsClassNameRegex
     * @param _jmsClassNameReplacement
     * @return
     */
    public String getClassName(final String _jmsClassNameRegex,
                               final String _jmsClassNameReplacement)
    {
        return getName().replaceAll(_jmsClassNameRegex, _jmsClassNameReplacement);
    }


    /**
     * @return
     */
    public String getParent()
    {
        return definitions.get(0).getParent();
    }


    /**
     * @return
     */
    public boolean isClassification()
    {
        return definitions.get(0).isClassification();
    }
}
