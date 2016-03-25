/*

 * Copyright 2003 - 2016 The eFaps Team
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

package org.efaps.esjp.sales;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.CachedPrintQuery;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("00006add-415a-4bfa-b669-8dd9b6d87146")
@EFapsApplication("eFapsApp-Sales")
public abstract class Perception_Base
{
    /**
     * @param _parameter parameter as passed by the eFaps API
     * @param _instance instance of a product to be checked
     * @return true if perception applies for the product, else false
     * @throws EFapsException on error
     */
    public boolean productIsPerception(final Parameter _parameter,
                                       final Instance _instance)
        throws EFapsException
    {
        boolean ret = false;
        // only if activated by SytemConfguration
        if (Sales.PERCEPTIONCERTIFICATEACTIVATE.get() && _instance.isValid()) {
            final PerceptionInfo info = evalProduct4Perception(_parameter, _instance);
            if (info != null) {
                ret = info.isApply();
                if (ret) {
                    ret = contactApplies2Perception(_parameter);
                }
            }
        }
        return ret;
    }

    /**
     * @param _parameter parameter as passed by the eFaps API
     * @return true if the contact applies 2 perception he product, else false
     * @throws EFapsException on error
     */
    public boolean contactApplies2Perception(final Parameter _parameter)
        throws EFapsException
    {
        boolean ret = true;
        final Instance contactInst = getContactInst(_parameter);
        if (contactInst.isValid()) {
            final PrintQuery print = new PrintQuery(contactInst);
            final SelectBuilder sel = SelectBuilder.get().clazz(CISales.Contacts_ClassTaxinfo)
                            .attribute(CISales.Contacts_ClassTaxinfo.Perception);
            print.addSelect(sel);
            print.executeWithoutAccessCheck();
            final Object val = print.getSelect(sel);
            if (val != null) {
                ret = Sales.TaxPerception.CLIENT.ordinal() == (Integer) val;
            }
        }
        return ret;
    }

    /**
     * @param _parameter parameter as passed by the eFaps API
     * @return the Contact instcane that will be used for evaluation
     * @throws EFapsException on error
     */
    protected Instance getContactInst(final Parameter _parameter)
        throws EFapsException
    {
        return Instance.get(_parameter.getParameterValue("contact"));
    }

    /**
     * @param _parameter parameter as passed by the eFaps API
     * @param _instance instance of a product to be checked
     * @return PerceptionInfo
     * @throws EFapsException on error
     */
    public PerceptionInfo evalProduct4Perception(final Parameter _parameter,
                                                 final Instance _instance)
        throws EFapsException
    {
        final PerceptionInfo ret = newPerceptionInfo(_parameter);
        if (_instance.isValid()) {

            final PrintQuery print = new CachedPrintQuery(_instance, PriceUtil_Base.CACHE_KEY)
                            .setLifespan(5).setLifespanUnit(TimeUnit.MINUTES);
            final SelectBuilder numSel = SelectBuilder.get().clazz(CIProducts.ClassPerception)
                            .attribute(CIProducts.ClassPerception.Numerator);
            final SelectBuilder denSel = SelectBuilder.get().clazz(CIProducts.ClassPerception)
                            .attribute(CIProducts.ClassPerception.Denominator);
            final SelectBuilder amountSel = SelectBuilder.get().clazz(CIProducts.ClassPerception)
                            .attribute(CIProducts.ClassPerception.Amount);
            final SelectBuilder currInstSel = SelectBuilder.get().clazz(CIProducts.ClassPerception)
                            .linkto(CIProducts.ClassPerception.CurrencyLink).instance();
            print.addSelect(numSel, denSel, amountSel, currInstSel);
            if (print.execute()) {
                final Integer numerator = print.<Integer>getSelect(numSel);
                if (numerator != null && numerator.intValue() > 0) {
                    final Integer denominator = print.<Integer>getSelect(denSel);
                    final BigDecimal amount = print.<BigDecimal>getSelect(amountSel);
                    final Instance currInst = print.<Instance>getSelect(currInstSel);
                    ret.setApply(true);
                    ret.setDenominator(denominator);
                    ret.setNumerator(numerator);
                    ret.setAmount(amount);
                    ret.setCurrencyInst(currInst);
                }
            }
        }
        return ret;
    }

    /**
     * Calculate perception.
     *
     * @param _parameter parameter as passed by the eFaps API
     * @param _calculator Claculator to be used for calculation
     * @return Perception calculated
     * @throws EFapsException on error
     */
    public BigDecimal calculatePerception(final Parameter _parameter,
                                          final Calculator_Base _calculator)
        throws EFapsException
    {
        final BigDecimal ret;
        final BigDecimal cross = _calculator.getCrossPrice();
        final PerceptionInfo info = getPerceptionInfo(_parameter, Instance.get(_calculator.getOid()));
        if (info.isApply()) {
            Instance currenctCurInst = _calculator.getProductCrossPrice().getCurrentCurrencyInstance();
            if (currenctCurInst == null) {
                currenctCurInst = Currency.getBaseCurrency();
            }
            final boolean min;
            if (info.getCurrencyInst().equals(currenctCurInst)) {
                min = info.getAmount().compareTo(cross) < 0;
            } else {
                final BigDecimal[] rates = new PriceUtil()
                                .getRates(_parameter, currenctCurInst, info.getCurrencyInst());
                final BigDecimal compare = info.getAmount().setScale(8).multiply(rates[2]);
                min = compare.compareTo(cross) < 0;
            }
            if (min) {
                ret = cross.multiply(new BigDecimal(info.getNumerator()).setScale(8))
                                .divide(new BigDecimal(info.getDenominator()).setScale(8), BigDecimal.ROUND_HALF_UP);
            } else {
                ret = BigDecimal.ZERO;
            }
        } else {
            ret = BigDecimal.ZERO;
        }
        return ret;
    }

    /**
     * @param _parameter parameter as passed by the eFaps API
     * @param _prodInstance instance of the Product
     * @return new PerceptionInfo instance
     * @throws EFapsException on error
     */
    public PerceptionInfo getPerceptionInfo(final Parameter _parameter,
                                            final Instance _prodInstance)
        throws EFapsException
    {
        final PerceptionInfo ret;
        if (productIsPerception(_parameter, _prodInstance)) {
            ret = evalProduct4Perception(_parameter, _prodInstance);
        } else {
            ret = new PerceptionInfo();
        }
        return ret;
    }

    /**
     * @param _parameter parameter as passed by the eFaps API
     * @return new PerceptionInfo instance
     */
    public PerceptionInfo newPerceptionInfo(final Parameter _parameter)
    {
        return new PerceptionInfo();
    }

    /**
     * Info related to the Perception.
     */
    public static class PerceptionInfo
        implements Serializable
    {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        /**
         * Does for this product apply Perception.
         */
        private boolean apply = false;

        /**
         * Numerator.
         */
        private Integer numerator;

        /**
         * Denominator.
         */
        private Integer denominator;

        /**
         * Amount.
         */
        private BigDecimal amount;

        /**
         * Instance of the Currency.
         */
        private Instance currencyInst;

        /**
         * Getter method for the instance variable {@link #apply}.
         *
         * @return value of instance variable {@link #apply}
         */
        public boolean isApply()
        {
            return this.apply;
        }

        /**
         * Setter method for instance variable {@link #apply}.
         *
         * @param _apply value for instance variable {@link #apply}
         */
        public void setApply(final boolean _apply)
        {
            this.apply = _apply;
        }

        /**
         * Getter method for the instance variable {@link #numerator}.
         *
         * @return value of instance variable {@link #numerator}
         */
        public Integer getNumerator()
        {
            return this.numerator;
        }

        /**
         * Setter method for instance variable {@link #numerator}.
         *
         * @param _numerator value for instance variable {@link #numerator}
         */
        public void setNumerator(final Integer _numerator)
        {
            this.numerator = _numerator;
        }

        /**
         * Getter method for the instance variable {@link #denominator}.
         *
         * @return value of instance variable {@link #denominator}
         */
        public Integer getDenominator()
        {
            return this.denominator;
        }

        /**
         * Setter method for instance variable {@link #denominator}.
         *
         * @param _denominator value for instance variable {@link #denominator}
         */
        public void setDenominator(final Integer _denominator)
        {
            this.denominator = _denominator;
        }

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
         * Getter method for the instance variable {@link #currencyInst}.
         *
         * @return value of instance variable {@link #currencyInst}
         */
        public Instance getCurrencyInst()
        {
            return this.currencyInst;
        }

        /**
         * Setter method for instance variable {@link #currencyInst}.
         *
         * @param _currencyInst value for instance variable
         *            {@link #currencyInst}
         */
        public void setCurrencyInst(final Instance _currencyInst)
        {
            this.currencyInst = _currencyInst;
        }

        @Override
        public String toString()
        {
            return ToStringBuilder.reflectionToString(this);
        }
    }
}
