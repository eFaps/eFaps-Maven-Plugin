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


/**
 *
 * @author The eFaps Team
 */
public class StatusCI
    implements Comparable<StatusCI>, IUniqueCI
{

    private String key;

    /**
     * Getter method for the instance variable {@link #name}.
     *
     * @return value of instance variable {@link #name}
     */
    public String getKey()
    {
        return key;
    }

    /**
     * Setter method for instance variable {@link #name}.
     *
     * @param _name value for instance variable {@link #name}
     */
    public void setKey(final String _key)
    {
        key = _key;
    }

    @Override
    public int compareTo(final StatusCI _arg0)
    {
        return getKey().compareTo(_arg0.getKey());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIdentifier()
    {
        return getKey();
    }
}
