/*
 * Copyright 2003 - 2013 The eFaps Team
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

package org.efaps.esjp.sales.tax.xml;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;

import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */

@EFapsUUID("eec76d1d-b62b-42db-beb3-d94905a1fcf6")
@EFapsRevision("$Rev$")
@XmlAccessorType(XmlAccessType.NONE)
public abstract class TaxEntry_Base
    implements Serializable
{

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Amount of the Tax.
     */
    @XmlAttribute(name = "amount")
    private BigDecimal amount;

    /**
     * UUID of the Tax.
     */
    @XmlAttribute(name = "uuid")
    private String uuid;

    /**
     * UUID of the related Category.
     */
    @XmlAttribute(name = "catUuid")
    private String catUuid;

    /**
     * UUID of the related Category.
     */
    @XmlAttribute(name = "currencyUuid")
    private String currencyUuid;

    /**
     * Date of the tax calculation.
     */
    @XmlAttribute(name = "date")
    @XmlSchemaType(name = "date")
    private Date date;

    /**
     * Getter method for the instance variable {@link #amount}.
     *
     * @return value of instance variable {@link #amount}
     */
    public BigDecimal getAmount()
    {
        return this.amount;
    }

    /**
     * Setter method for instance variable {@link #amount}.
     *
     * @param _amount value for instance variable {@link #amount}
     */
    public void setAmount(final BigDecimal _amount)
    {
        this.amount = _amount;
    }

    /**
     * Getter method for the instance variable {@link #uuid}.
     *
     * @return value of instance variable {@link #uuid}
     */
    public UUID getUUID()
    {
        return UUID.fromString(this.uuid);
    }

    /**
     * Setter method for instance variable {@link #uuid}.
     *
     * @param _uuid value for instance variable {@link #uuid}
     */
    public void setUUID(final UUID _uuid)
    {
        this.uuid = _uuid.toString();
    }

    /**
     * Getter method for the instance variable {@link #catUuid}.
     *
     * @return value of instance variable {@link #catUuid}
     */
    public UUID getCatUUID()
    {
        return UUID.fromString(this.catUuid);
    }

    /**
     * Setter method for instance variable {@link #uuid}.
     *
     * @param _uuid value for instance variable {@link #uuid}
     */
    public void setCatUUID(final UUID _uuid)
    {
        this.catUuid = _uuid.toString();
    }

    /**
     * Getter method for the instance variable {@link #currencyUuid}.
     *
     * @return value of instance variable {@link #currencyUuid}
     */
    public UUID getCurrencyUUID()
    {
        return UUID.fromString(this.currencyUuid);
    }

    /**
     * Setter method for instance variable {@link #currencyUuid}.
     *
     * @param _currencyUuid value for instance variable {@link #currencyUuid}
     */
    public void setCurrencyUUID(final UUID _currencyUuid)
    {
        this.currencyUuid = _currencyUuid == null ? null : _currencyUuid.toString();
    }

    public DateTime getDateTime()
    {
        return new DateTime(this.date);
    }

    public void setDate(final DateTime _dateTime) {
        this.date = _dateTime.toDate();
    }

    @Override
    public boolean equals(final Object _obj)
    {
        boolean ret;
        if (_obj instanceof TaxEntry_Base) {
            ret = this.uuid.equals(((TaxEntry_Base) _obj).getUUID());
        } else {
            ret = super.equals(_obj);
        }
        return ret;
    }

    @Override
    public int hashCode()
    {
        return getUUID().hashCode();
    }
}
