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

package org.efaps.esjp.sales.tax;

import java.math.BigDecimal;
import java.util.UUID;

import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.CachedMultiPrintQuery;
import org.efaps.db.Instance;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("89710086-ecfa-4868-a185-cf4f6e8f290b")
@EFapsRevision("$Rev$")
public abstract class Tax_Base
{

    private static final Tax ZEROTAX = new Tax(null, null, "ZERO", "e27a167c-a4e2-416f-9878-c785bfa33576",
                    Integer.valueOf(1), Integer.valueOf(1));

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
    public Tax_Base(final TaxCat_Base _taxcat,
                    final Instance _instance,
                    final String _name,
                    final String _uuid,
                    final Integer _numerator,
                    final Integer _denominator)
    {
        this.taxCat = _taxcat;
        this.instance = _instance;
        this.name = _name;
        this.uuid = UUID.fromString(_uuid);
        this.numerator = _numerator;
        this.denominator = _denominator;
        this.initialized = true;
    }

    /**
     *
     */
    public Tax_Base()
    {
    }

    /**
     *
     */
    public BigDecimal getFactor()
        throws EFapsException
    {
        final BigDecimal denom = new BigDecimal(getDenominator());
        final BigDecimal num = new BigDecimal(getNumerator());
        return num.divide(denom, 16, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * @return
     */
    public int getNumerator()
        throws EFapsException
    {
        initialize();
        return this.numerator;
    }

    /**
     *
     */
    private void initialize()
        throws EFapsException
    {
        if (!this.initialized) {
            final QueryBuilder queryBldr = new QueryBuilder(CISales.Tax);
            queryBldr.addWhereAttrEqValue(CISales.Tax.UUID, this.uuid.toString());
            queryBldr.addWhereAttrEqValue(CISales.Tax.TaxCategory, getTaxCat().getInstance());
            queryBldr.addOrderByAttributeDesc(CISales.Tax.ValidFrom);
            final CachedMultiPrintQuery multi = queryBldr.getCachedPrint(TaxCat_Base.CACHEKEY);
            multi.setEnforceSorted(true);
            multi.addAttribute(CISales.Tax.Name, CISales.Tax.Numerator, CISales.Tax.Denominator, CISales.Tax.ValidFrom,
                            CISales.Tax.UUID);
            multi.execute();
            if (multi.next()) {
                this.instance = multi.getCurrentInstance();
                this.name = multi.<String>getAttribute(CISales.Tax.Name);
                this.numerator = multi.<Integer>getAttribute(CISales.Tax.Numerator);
                this.denominator = multi.<Integer>getAttribute(CISales.Tax.Denominator);
                this.initialized = true;
            }
        }
    }

    /**
     * @return
     */
    public int getDenominator()
        throws EFapsException
    {
        initialize();
        return this.denominator;
    }

    /**
     * Getter method for the instance variable {@link #uuid}.
     *
     * @return value of instance variable {@link #uuid}
     */
    public UUID getUUID()
        throws EFapsException
    {
        initialize();
        return this.uuid;
    }

    /**
     * Getter method for the instance variable {@link #instance}.
     *
     * @return value of instance variable {@link #instance}
     */
    public Instance getInstance()
        throws EFapsException
    {
        initialize();
        return this.instance;
    }

    /**
     * Getter method for the instance variable {@link #name}.
     *
     * @return value of instance variable {@link #name}
     */
    public String getName()
        throws EFapsException
    {
        initialize();
        return this.name;
    }

    /**
     * Getter method for the instance variable {@link #taxCat}.
     *
     * @return value of instance variable {@link #taxCat}
     */
    public TaxCat_Base getTaxCat()
    {
        return this.taxCat;
    }

    /**
     * Setter method for instance variable {@link #uuid}.
     *
     * @param _uuid value for instance variable {@link #uuid}
     */
    protected void setUUID(final UUID _uuid)
    {
        this.uuid = _uuid;
    }

    /**
     * Setter method for instance variable {@link #taxCat}.
     *
     * @param _taxCat value for instance variable {@link #taxCat}
     */
    protected void setTaxCat(final TaxCat_Base _taxCat)
    {
        this.taxCat = _taxCat;
    }

    /**
     * @param _uuid
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
                ret = this.uuid.equals(((Tax_Base) _obj).getUUID());
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
        return this.uuid.hashCode();
    }
}
