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
package org.efaps.esjp.sales.tax.xml;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSchemaType;

/**
 * @author The eFaps Team
 */
@EFapsUUID("eec76d1d-b62b-42db-beb3-d94905a1fcf6")
@EFapsApplication("eFapsApp-Sales")
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
     * Amount of the Tax.
     */
    @XmlAttribute(name = "base")
    private BigDecimal base;

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
        return amount;
    }

    /**
     * Setter method for instance variable {@link #amount}.
     *
     * @param _amount value for instance variable {@link #amount}
     */
    public void setAmount(final BigDecimal _amount)
    {
        amount = _amount;
    }

    /**
     * Gets the amount of the Tax.
     *
     * @return the amount of the Tax
     */
    public BigDecimal getBase()
    {
        return base;
    }

    /**
     * Sets the amount of the Tax.
     *
     * @param _base the new amount of the Tax
     */
    public void setBase(final BigDecimal _base)
    {
        base = _base;
    }

    /**
     * Getter method for the instance variable {@link #uuid}.
     *
     * @return value of instance variable {@link #uuid}
     */
    public UUID getUUID()
    {
        return UUID.fromString(uuid);
    }

    /**
     * Setter method for instance variable {@link #uuid}.
     *
     * @param _uuid value for instance variable {@link #uuid}
     */
    public void setUUID(final UUID _uuid)
    {
        uuid = _uuid.toString();
    }

    /**
     * Getter method for the instance variable {@link #catUuid}.
     *
     * @return value of instance variable {@link #catUuid}
     */
    public UUID getCatUUID()
    {
        return UUID.fromString(catUuid);
    }

    /**
     * Setter method for instance variable {@link #uuid}.
     *
     * @param _uuid value for instance variable {@link #uuid}
     */
    public void setCatUUID(final UUID _uuid)
    {
        catUuid = _uuid.toString();
    }

    /**
     * Getter method for the instance variable {@link #currencyUuid}.
     *
     * @return value of instance variable {@link #currencyUuid}
     */
    public UUID getCurrencyUUID()
    {
        return currencyUuid == null ? null : UUID.fromString(currencyUuid);
    }

    /**
     * Setter method for instance variable {@link #currencyUuid}.
     *
     * @param _currencyUuid value for instance variable {@link #currencyUuid}
     */
    public void setCurrencyUUID(final UUID _currencyUuid)
    {
        currencyUuid = _currencyUuid == null ? null : _currencyUuid.toString();
    }
    /**
     * Getter method for the instance variable {@link #date}.
     *
     * @return value of instance variable {@link #date}
     */
    @JsonIgnore
    public DateTime getDateTime()
    {
        return new DateTime(date);
    }

    /**
     * Setter method for instance variable {@link #date}.
     *
     * @param _dateTime value for instance variable {@link #date}
     */
    public void setDate(final DateTime _dateTime)
    {
        date = _dateTime.toDate();
    }

    @Override
    public boolean equals(final Object _obj)
    {
        final boolean ret;
        if (_obj instanceof TaxEntry_Base) {
            ret = uuid.equals(((TaxEntry_Base) _obj).getUUID());
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
