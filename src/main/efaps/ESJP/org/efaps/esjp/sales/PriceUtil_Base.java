/*
 * Copyright 2003 - 2010 The eFaps Team
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

package org.efaps.esjp.sales;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.sales.document.AbstractDocument_Base;
import org.efaps.ui.wicket.util.DateUtil;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("3a726661-473f-48b6-82dd-9b6498561d48")
@EFapsRevision("$Rev$")
public abstract class PriceUtil_Base
    implements Serializable
{

    /**
     * Needed for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Method to get the Price for a product.
     *
     * @param _parameter    Parameter as passed form the efaps API
     * @param _oid          oid of the product the price is wanted for
     * @param _typeUUID     uuid of th eprice type wanted
     * @return price for the product as BigDecimal
     * @throws EFapsException on error
     */
    public ProductPrice getPrice(final Parameter _parameter,
                                 final String _oid,
                                 final UUID _typeUUID)
        throws EFapsException
    {
        final ProductPrice ret = new ProductPrice();

        final DateTime date = getDateFromParameter(_parameter);

        final QueryBuilder queryBldr = new QueryBuilder(_typeUUID);
        queryBldr.addWhereAttrEqValue(CIProducts.ProductPricelistPurchase.ProductAbstractLink,
                        Instance.get(_oid).getId());
        queryBldr.addWhereAttrLessValue(CIProducts.ProductPricelistPurchase.ValidFrom, date.plusSeconds(1));
        queryBldr.addWhereAttrGreaterValue(CIProducts.ProductPricelistPurchase.ValidUntil, date.minusSeconds(1));
        final InstanceQuery query = queryBldr.getQuery();
        query.execute();
        if (query.next()) {
            final QueryBuilder queryBldr2 = new QueryBuilder(CIProducts.ProductPricelistPosition);
            queryBldr2.addWhereAttrEqValue(CIProducts.ProductPricelistPosition.ProductPricelist,
                                          query.getCurrentValue().getId());
            final MultiPrintQuery multi = queryBldr2.getPrint();
            multi.addAttribute(CIProducts.ProductPricelistPosition.Price,
                               CIProducts.ProductPricelistPosition.CurrencyId);
            multi.execute();
            if (multi.next()) {
                final Instance priceInst = Instance.get(CIERP.Currency.getType(),
                                             multi.<Long>getAttribute(CIProducts.ProductPricelistPosition.CurrencyId));
                final Instance currentInst = (Instance) Context.getThreadContext().getSessionAttribute(
                                AbstractDocument_Base.CURRENCY_INSTANCE_KEY);
                // Sales-Configuration
                final Instance baseInst = SystemConfiguration.get(UUID
                                .fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f")).getLink("CurrencyBase");
                final BigDecimal price = multi.<BigDecimal>getAttribute(CIProducts.ProductPricelistPosition.Price);

                ret.setCurrentCurrencyInstance(currentInst);
                ret.setOrigCurrencyInstance(priceInst);
                ret.setOrigPrice(price);
                if (priceInst.equals(currentInst)) {
                    ret.setCurrentPrice(price);
                } else {
                    final BigDecimal[] rates = getRates(_parameter, currentInst, priceInst);
                    ret.setCurrentPrice(price.multiply(rates[2]));
                }
                if (priceInst.equals(baseInst)) {
                    ret.setBasePrice(price);
                    ret.setBaseRate(BigDecimal.ONE);
                } else {
                    final BigDecimal[] rates = getRates(_parameter, currentInst, baseInst);
                    ret.setBasePrice(price.multiply(rates[2]));
                    ret.setBaseRate(rates[2]);
                }
            }
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API for esjp
     * @param _targetCurrencyInst instance of the target Currency
     * @param _currentCurrencyInst instance of the current Currency
     * @return Array with rates for the date evaluated from the given _parameter
     *         {targetRate (rate for _targetCurrencyInst), currentRate (rate for
     *         _currentCurrencyInst, exchangeRate (targetRate/currentRate)}
     * @throws EFapsException on error
     */
    public BigDecimal[] getRates(final Parameter _parameter,
                                 final Instance _targetCurrencyInst,
                                 final Instance _currentCurrencyInst)
        throws EFapsException
    {
        return getRates(getDateFromParameter(_parameter), _targetCurrencyInst, _currentCurrencyInst);
    }

    /**
     * Returns an Array of rates.:
     * <ol>
     *  <li>new Rate used for Calculation</li>
     *  <li>current Rate used for Calculation</li>
     *  <li>rate used to convert the current rate into the new rate</li>
     *  <li>new Rate as the value for the UserInterface</li>
     * </ol>
     * @param _date date the rates will be retrieved for
     * @param _targetCurrencyInst instance of the target Currency
     * @param _currentCurrencyInst instance of the current Currency
     * @return Array with rates for the date {targetRate (rate for
     *         _targetCurrencyInst), currentRate (rate for _currentCurrencyInst,
     *         exchangeRate (targetRate/currentRate)}
     * @throws EFapsException on error
     */
    public BigDecimal[] getRates(final DateTime _date,
                                 final Instance _targetCurrencyInst,
                                 final Instance _currentCurrencyInst)
        throws EFapsException
    {
        // Sales-Configuration
        final Instance baseInst = SystemConfiguration.get(UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f"))
                        .getLink("CurrencyBase");
        BigDecimal newRate;
        BigDecimal currentRate;
        BigDecimal newRateUI;
        if (_targetCurrencyInst.equals(baseInst)) {
            newRate = BigDecimal.ONE;
            newRateUI = newRate;
        } else {
            final BigDecimal[] rates = getExchangeRate(_date, _targetCurrencyInst);
            newRate = rates[0];
            newRateUI = rates[1];
            if (newRate.compareTo(BigDecimal.ZERO) == 0) {
                newRate = BigDecimal.ONE;
                newRateUI = BigDecimal.ONE;
            }
        }
        if (_currentCurrencyInst.equals(baseInst)) {
            currentRate = BigDecimal.ONE;
        } else {
            currentRate = getExchangeRate(_date, _currentCurrencyInst)[0];
            if (currentRate.compareTo(BigDecimal.ZERO) == 0) {
                currentRate = BigDecimal.ONE;
            }
        }
        return new BigDecimal[]{ newRate, currentRate,
                        newRate.divide(currentRate, 12, RoundingMode.HALF_UP), newRateUI};
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return exchange rate
     * @throws EFapsException on error
     */
    public BigDecimal getExchangeRate(final Parameter _parameter)
        throws EFapsException
    {
        return getExchangeRate(getDateFromParameter(_parameter));
    }

    /**
     * Method to get the exchange rate for the currency from the sales
     * SystemConfiguration for a specific date.
     *
     * @param _date date the rate is wanted for.
     * @return rate
     * @throws EFapsException on error
     */
    public BigDecimal getExchangeRate(final DateTime _date)
        throws EFapsException
    {
        final Instance curInstance = SystemConfiguration.get(UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f"))
                        .getLink("Currency4Invoice");
        return getExchangeRate(_date, curInstance)[0];
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @param _curInstance instrance of the currency
     * @return exchange rate
     * @throws EFapsException on error
     */
    public BigDecimal getExchangeRate(final Parameter _parameter,
                                      final Instance _curInstance)
        throws EFapsException
    {
        return getExchangeRate(getDateFromParameter(_parameter), _curInstance)[0];
    }

    /**
     * Method to get the exchange rate for the currency from the sales
     * SystemConfiguration for a specific date.
     * Returns an Array of rates:
     * <ol>
     *  <li>Rate used for Calculation</li>
     *  <li>Rate as the value for the UserInterface</li>
     * </ol>
     * @param _date date the rate is wanted for.
     * @param _curInstance instance of a currency the rate is wanted for
     * @return rateArray
     * @throws EFapsException on error
     */
    public BigDecimal[] getExchangeRate(final DateTime _date,
                                        final Instance _curInstance)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CIERP.CurrencyRateClient);
        queryBldr.addWhereAttrEqValue(CIERP.CurrencyRateClient.CurrencyLink, _curInstance.getId());
        queryBldr.addWhereAttrLessValue(CIERP.CurrencyRateClient.ValidFrom, _date.plusSeconds(1));
        queryBldr.addWhereAttrGreaterValue(CIERP.CurrencyRateClient.ValidUntil, _date.minusSeconds(1));

        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIERP.CurrencyRateClient.Rate);
        multi.execute();
        BigDecimal rate = BigDecimal.ONE;
        BigDecimal rateUI = BigDecimal.ONE;

        if (multi.next()) {
            rate = new Currency().evalRate(multi.<Object[]>getAttribute(CIERP.CurrencyRateClient.Rate), false);
            rateUI = new Currency().evalRate(multi.<Object[]>getAttribute(CIERP.CurrencyRateClient.Rate), true);
        }
        return new BigDecimal[] { rate, rateUI };
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return new DateTime
     * @throws EFapsException on error
     */
    public DateTime getDateFromParameter(final Parameter _parameter)
        throws EFapsException
    {
        final String dateStr = _parameter.getParameterValue("date_eFapsDate");
        final DateTime date;
        if (dateStr != null && dateStr.length() > 0) {
            date = DateUtil.getDateFromParameter(dateStr);
        } else {
            date = new DateTime();
        }
        return date;
    }

    /**
     * Represent the price for one product.
     */
    public class ProductPrice
        implements Serializable
    {

        /**
         * Needed for serialization.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Instance of the original Currency.
         */
        private Instance origCurrencyInstance;

        /**
         * Original Price.
         */
        private BigDecimal origPrice = BigDecimal.ZERO;

        /**
         * Instance of the current Currency.
         */
        private Instance currentCurrencyInstance;

        /**
         * Current Price.
         */
        private BigDecimal currentPrice = BigDecimal.ZERO;

        /**
         * Base Price.
         */
        private BigDecimal basePrice = BigDecimal.ZERO;

        /**
         * Exchange rate to the base.
         */
        private BigDecimal baseRate = BigDecimal.ONE;

        /**
         *
         */
        public ProductPrice()
        {
            final Instance baseInst = SystemConfiguration.get(UUID
                            .fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f")).getLink("CurrencyBase");
            this.origCurrencyInstance = baseInst;
            this.currentCurrencyInstance = baseInst;
        }

        /**
         * @param _origCurrencyInst instance of the currency
         * @param _origPrice        original price
         * @param _basePrice        base price
         */
        public ProductPrice(final Instance _origCurrencyInst,
                            final BigDecimal _origPrice,
                            final BigDecimal _basePrice)
        {
            this.origCurrencyInstance = _origCurrencyInst;
            this.origPrice = _origPrice;
            this.basePrice = _basePrice;
        }

        /**
         * Setter method for instance variable {@link #baseRate}.
         *
         * @param _baseRate value for instance variable {@link #baseRate}
         */
        public void setBaseRate(final BigDecimal _baseRate)
        {
            this.baseRate = _baseRate;

        }

        /**
         * Getter method for the instance variable {@link #baseRate}.
         *
         * @return value of instance variable {@link #baseRate}
         */
        public BigDecimal getBaseRate()
        {
            return this.baseRate;
        }

        /**
         * Setter method for instance variable {@link #basePrice}.
         *
         * @param _basePrice value for instance variable {@link #basePrice}
         */
        public void setBasePrice(final BigDecimal _basePrice)
        {
            this.basePrice = _basePrice;
        }

        /**
         * Setter method for instance variable {@link #origCurrencyInstance}.
         *
         * @param _origCurrencyInstance value for instance variable
         *            {@link #origCurrencyInstance}
         */
        public void setOrigCurrencyInstance(final Instance _origCurrencyInstance)
        {
            this.origCurrencyInstance = _origCurrencyInstance;
        }

        /**
         * Setter method for instance variable {@link #origPrice}.
         *
         * @param _origPrice value for instance variable {@link #origPrice}
         */
        public void setOrigPrice(final BigDecimal _origPrice)
        {
            this.origPrice = _origPrice;
        }

        /**
         * Getter method for the instance variable {@link #origCurrencyInstance}
         * .
         *
         * @return value of instance variable {@link #origCurrencyInstance}
         */
        public Instance getOrigCurrencyInstance()
        {
            return this.origCurrencyInstance;
        }

        /**
         * Getter method for the instance variable {@link #origPrice}.
         *
         * @return value of instance variable {@link #origPrice}
         */
        public BigDecimal getOrigPrice()
        {
            return this.origPrice;
        }

        /**
         * Getter method for the instance variable {@link #basePrice}.
         *
         * @return value of instance variable {@link #basePrice}
         */
        public BigDecimal getBasePrice()
        {
            return this.basePrice;
        }

        /**
         * Getter method for the instance variable
         * {@link #currentCurrencyInstance}.
         *
         * @return value of instance variable {@link #currentCurrencyInstance}
         */
        public Instance getCurrentCurrencyInstance()
        {
            return this.currentCurrencyInstance;
        }

        /**
         * Setter method for instance variable {@link #currentCurrencyInstance}.
         *
         * @param _currentCurrencyInstance value for instance variable
         *            {@link #currentCurrencyInstance}
         */

        public void setCurrentCurrencyInstance(final Instance _currentCurrencyInstance)
        {
            this.currentCurrencyInstance = _currentCurrencyInstance;
        }

        /**
         * Getter method for the instance variable {@link #currentPrice}.
         *
         * @return value of instance variable {@link #currentPrice}
         */
        public BigDecimal getCurrentPrice()
        {
            return this.currentPrice;
        }

        /**
         * Setter method for instance variable {@link #currentPrice}.
         *
         * @param _currentPrice value for instance variable
         *            {@link #currentPrice}
         */
        public void setCurrentPrice(final BigDecimal _currentPrice)
        {
            this.currentPrice = _currentPrice;
        }
    }
}
