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

package org.efaps.esjp.sales.tax;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.CachedMultiPrintQuery;
import org.efaps.db.Instance;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.util.EFapsException;

/**
 *
 * @author The eFaps Team
 */
@EFapsUUID("89710086-ecfa-4868-a185-cf4f6e8f290b")
@EFapsApplication("eFapsApp-Sales")
public abstract class Tax_Base
    implements Serializable
{

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * The TAXFREE Tax mining no Tax at all.
     */
    private static final Tax ZEROTAX = new Tax(null, null, "ZERO", "fb2c7c17-7e0b-4199-a616-7c8528b1730e", TaxType.ADVALOREM,
                    Integer.valueOf(1), Integer.valueOf(1), BigDecimal.ZERO, 0L);

    /**
     * Instance of this tax.
     */
    private Instance instance;

    /**
     * Name of this tax.
     */
    private String name;

    /**
     * UUID of this tax.
     */
    private UUID uuid;

    private TaxType taxType;

    private BigDecimal amount;

    private Long currencyId;

    /**
     * NUmerator of this tax.
     */
    private Integer numerator;

    /**
     * Denominator for this tax.
     */
    private Integer denominator;

    /**
     * Was this tax initialized.
     */
    private boolean initialized = false;

    /**
     * Tax category this tax belongs to.
     */
    private TaxCat_Base taxCat;

    /**
     * @param _taxcat       Tax category this tax belongs to
     * @param _instance     Instance of this tax
     * @param _name         name of this tax
     * @param _uuid         UUID of this tax
     * @param _numerator    Numerator for this tax
     * @param _denominator  Denominator for this tax
     */
    protected Tax_Base(final TaxCat_Base _taxcat,
                       final Instance _instance,
                       final String _name,
                       final String _uuid,
                       final TaxType _taxType,
                       final Integer _numerator,
                       final Integer _denominator,
                       final BigDecimal _amount,
                       final Long _currencyId)
    {
        taxCat = _taxcat;
        instance = _instance;
        name = _name;
        uuid = UUID.fromString(_uuid);
        taxType = _taxType == null ? TaxType.ADVALOREM : _taxType;
        numerator = _numerator;
        denominator = _denominator;
        amount = _amount;
        currencyId = _currencyId;
        initialized = true;
    }

    /**
     *
     */
    protected Tax_Base()
    {
    }

    /**
     * @return the factor, aka numerator/denominator
     *  @throws EFapsException on error
     */
    public BigDecimal getFactor()
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        if (TaxType.ADVALOREM.equals(taxType)) {
            final BigDecimal denom = new BigDecimal(getDenominator());
            final BigDecimal num = new BigDecimal(getNumerator());
            ret = num.divide(denom, 16, BigDecimal.ROUND_HALF_UP);
        }
        return ret;
    }

    /**
     * @return the numerator
     * @throws EFapsException on error
     */
    public int getNumerator()
        throws EFapsException
    {
        initialize();
        return numerator;
    }

    /**
     *
     * @throws EFapsException on error
     */
    private void initialize()
        throws EFapsException
    {
        if (!initialized) {
            final QueryBuilder queryBldr = new QueryBuilder(CISales.Tax);
            queryBldr.addWhereAttrEqValue(CISales.Tax.UUID, uuid.toString());
            queryBldr.addWhereAttrEqValue(CISales.Tax.TaxCategory, getTaxCat().getInstance());
            queryBldr.addOrderByAttributeDesc(CISales.Tax.ValidFrom);
            final CachedMultiPrintQuery multi = queryBldr.getCachedPrint(TaxCat.CACHEKEY);
            multi.setEnforceSorted(true);
            multi.addAttribute(CISales.Tax.Name, CISales.Tax.Numerator, CISales.Tax.Denominator, CISales.Tax.ValidFrom,
                            CISales.Tax.UUID, CISales.Tax.TaxType, CISales.Tax.Amount, CISales.Tax.CurrencyLink);
            multi.execute();
            if (multi.next()) {
                final TaxType taxTypeTmp = multi.getAttribute(CISales.Tax.TaxType);

                instance = multi.getCurrentInstance();
                taxType = taxTypeTmp == null ? TaxType.ADVALOREM : taxTypeTmp;
                name = multi.getAttribute(CISales.Tax.Name);
                numerator = multi.getAttribute(CISales.Tax.Numerator);
                denominator = multi.getAttribute(CISales.Tax.Denominator);
                amount = multi.getAttribute(CISales.Tax.Amount);
                currencyId = multi.getAttribute(CISales.Tax.CurrencyLink);
                initialized = true;
            }
        }
    }

    /**
     * @return the denominator
     * @throws EFapsException on error
     */
    public int getDenominator()
        throws EFapsException
    {
        initialize();
        return denominator;
    }

    /**
     * Getter method for the instance variable {@link #uuid}.
     *
     * @return value of instance variable {@link #uuid}
     * @throws EFapsException on error
     */
    public UUID getUUID()
        throws EFapsException
    {
        initialize();
        return uuid;
    }

    /**
     * Getter method for the instance variable {@link #instance}.
     *
     * @return value of instance variable {@link #instance}
     * @throws EFapsException on error
     */
    public Instance getInstance()
        throws EFapsException
    {
        initialize();
        return instance;
    }

    /**
     * Getter method for the instance variable {@link #name}.
     *
     * @return value of instance variable {@link #name}
     * @throws EFapsException on error
     */
    public String getName()
        throws EFapsException
    {
        initialize();
        return name;
    }

    public TaxType getTaxType()
    {
        return taxType;
    }

    public BigDecimal getAmount()
    {
        return amount;
    }

    public Long getCurrencyId()
    {
        return currencyId;
    }

    public CurrencyInst getCurrencyInst()
        throws EFapsException
    {
        return CurrencyInst.get(getCurrencyId());
    }

    /**
     * Getter method for the instance variable {@link #taxCat}.
     *
     * @return value of instance variable {@link #taxCat}
     */
    public TaxCat_Base getTaxCat()
    {
        return taxCat;
    }

    /**
     * Setter method for instance variable {@link #uuid}.
     *
     * @param _uuid value for instance variable {@link #uuid}
     */
    protected void setUUID(final UUID _uuid)
    {
        uuid = _uuid;
    }

    /**
     * Setter method for instance variable {@link #taxCat}.
     *
     * @param _taxCat value for instance variable {@link #taxCat}
     */
    protected void setTaxCat(final TaxCat_Base _taxCat)
    {
        taxCat = _taxCat;
    }

    /**
     * @param _catUUID  uuid of the taxcategory
     * @param _uuid     uuid of the tax
     * @return   a tax instance
     * @throws EFapsException on error
     */
    public static Tax get(final UUID _catUUID,
                          final UUID _uuid)
        throws EFapsException
    {
        final Tax ret = new Tax();
        ret.setUUID(_uuid);
        ret.setTaxCat(TaxCat_Base.get(_catUUID));
        return ret;
    }

    /**
     * @return the ZEROTAX
     */
    public static Tax getZeroTax()
    {
        return Tax_Base.ZEROTAX;
    }

    @Override
    public boolean equals(final Object _obj)
    {
        boolean ret = false;
        if (_obj instanceof Tax_Base) {
            try {
                ret = uuid.equals(((Tax_Base) _obj).getUUID());
            } catch (final EFapsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            ret = super.equals(_obj);
        }
        return ret;
    }

    @Override
    public int hashCode()
    {
        return uuid.hashCode();
    }
}
