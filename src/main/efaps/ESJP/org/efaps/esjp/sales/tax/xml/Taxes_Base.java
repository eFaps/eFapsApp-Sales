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


package org.efaps.esjp.sales.tax.xml;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;


/**
 * @author The eFaps Team
 */
@EFapsUUID("dcce8bff-2832-4bc8-ac69-8b72322d5d1d ")
@EFapsApplication("eFapsApp-Sales")
@XmlAccessorType(XmlAccessType.NONE)
public abstract class Taxes_Base
    implements Serializable
{

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Entries for this Taxes Collection.
     */
    @XmlElementWrapper(name = "entries")
    @XmlElement(name = "entry")
    private final List<TaxEntry> entries = new ArrayList<>();

    /**
     * Getter method for the instance variable {@link #entries}.
     *
     * @return value of instance variable {@link #entries}
     */
    public List<TaxEntry> getEntries()
    {
        return entries;
    }
}
