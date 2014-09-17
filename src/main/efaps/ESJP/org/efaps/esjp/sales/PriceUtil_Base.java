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
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.sales.document.AbstractDocument_Base;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.SalesSettings;
import org.efaps.esjp.ui.html.HtmlTable;
import org.efaps.ui.wicket.util.DateUtil;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

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
     * For get the filter in the picker event.
     */
    public static final String PRODFILTER_KEY = "SALES_PRODUCT_FILTER_SESSION_KEY";

    /**
     * For get the filter in the picker event.
     */
    public static final String CACHE_KEY = "org.efaps.esjp.sales.PriceUtil.CacheKey";

    /**
     * Needed for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Method to get the Price for a product.
     *
     * @param _parameter Parameter as passed form the efaps API
     * @param _oid oid of the product the price is wanted for
     * @param _typeUUID uuid of the price type wanted
     * @return price for the product as BigDecimal
     * @throws EFapsException on error
     */
    public ProductPrice getPrice(final Parameter _parameter,
                                 final String _oid,
                                 final UUID _typeUUID)
        throws EFapsException
    {
        return getPrice(_parameter, getDateFromParameter(_parameter), Instance.get(_oid), _typeUUID, true);
    }

    /**
     * Method to get the Price for a product.
     *
     * @param _parameter    Parameter as passed form the eFaps API
     * @param _date         Date to be used for the search
     * @param _instance     Instance of the product the price is wanted for
     * @param _type         price type wanted
     * @return price for the product as BigDecimal
     * @throws EFapsException on error
     */
    public ProductPrice getPrice(final Parameter _parameter,
                                 final DateTime _date,
                                 final Instance _instance,
                                 final CIType _type)
        throws EFapsException
    {
        return getPrice(_parameter, _date, _instance, _type.getType().getUUID(), true);
    }

    /**
     * Method to get the Price for a product.
     *
     * @param _parameter    Parameter as passed form the eFaps API
     * @param _date         Date to be used for the search
     * @param _instance     Instance of the product the price is wanted for
     * @param _typeUUID     uuid of the price type wanted
     * @param _cache        allow caching
     * @return price for the product as BigDecimal
     * @throws EFapsException on error
     */
    public ProductPrice getPrice(final Parameter _parameter,
                                 final DateTime _date,
                                 final Instance _instance,
                                 final UUID _typeUUID,
                                 final boolean _cache)
        throws EFapsException
    {
        final ProductPrice ret = getProductPrice(_parameter);

        if (_typeUUID != null) {
            final QueryBuilder queryBldr = new QueryBuilder(_typeUUID);
            queryBldr.addWhereAttrEqValue(CIProducts.ProductPricelistAbstract.ProductAbstractLink, _instance.getId());
            queryBldr.addWhereAttrLessValue(CIProducts.ProductPricelistAbstract.ValidFrom,
                                    new DateTime(_date).withTime(0, 0, 0, 0).toDateTime().plusSeconds(1));
            queryBldr.addWhereAttrGreaterValue(CIProducts.ProductPricelistAbstract.ValidUntil,
                                    new DateTime(_date).withTime(0, 0, 0, 0).toDateTime().minusSeconds(1));
            add2QueryBldr4PriceList(_parameter, queryBldr);
            final InstanceQuery query;
            if (_cache) {
                query = queryBldr.getCachedQuery(PriceUtil_Base.CACHE_KEY).setLifespan(12)
                                .setLifespanUnit(TimeUnit.HOURS);
            } else {
                query = queryBldr.getQuery();
            }
            query.execute();
            if (query.next()) {
                final QueryBuilder queryBldr2 = new QueryBuilder(CIProducts.ProductPricelistPosition);
                queryBldr2.addWhereAttrEqValue(CIProducts.ProductPricelistPosition.ProductPricelist,
                                query.getCurrentValue().getId());
                final MultiPrintQuery multi;
                if (_cache) {
                    multi = queryBldr2.getCachedPrint(PriceUtil_Base.CACHE_KEY).setLifespan(12)
                                    .setLifespanUnit(TimeUnit.DAYS);
                } else {
                    multi = queryBldr2.getPrint();
                }
                multi.addAttribute(CIProducts.ProductPricelistPosition.Price,
                                CIProducts.ProductPricelistPosition.CurrencyId);
                multi.execute();
                if (multi.next()) {
                    final Instance priceInst = Instance.get(CIERP.Currency.getType(),
                                    multi.<Long>getAttribute(CIProducts.ProductPricelistPosition.CurrencyId));
                    final Instance currentInst = (Instance) Context.getThreadContext().getSessionAttribute(
                                    AbstractDocument_Base.CURRENCYINST_KEY);

                    final Instance baseInst = Currency.getBaseCurrency();

                    final BigDecimal price = multi.<BigDecimal>getAttribute(CIProducts.ProductPricelistPosition.Price);

                    ret.setCurrentCurrencyInstance(currentInst);
                    ret.setOrigCurrencyInstance(priceInst);
                    ret.setOrigPrice(price);
                    if (priceInst.equals(currentInst)) {
                        ret.setCurrentPrice(price);
                    } else {
                        if (currentInst != null) {
                            final BigDecimal[] rates = getRates(_parameter, currentInst, priceInst);
                            ret.setCurrentPrice(price.multiply(rates[2]));
                        } else {
                            ret.setCurrentPrice(price);
                        }
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
        }
        return ret;
    }

    /**
     * To be used by implementations.
     *
     * @param _parameter Parameter as passed by the eFaps API for esjp
     * @param _queryBldr QueryBuilder to add to throws EFapsException
     * @throws EFapsException on error
     */
    protected void add2QueryBldr4PriceList(final Parameter _parameter,
                                           final QueryBuilder _queryBldr)
        throws EFapsException
    {

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
     * Returns an Array of rates.:.
     * <ol>
     * <li>new Rate used for Calculation</li>
     * <li>current Rate used for Calculation</li>
     * <li>rate used to convert the current rate into the new rate</li>
     * <li>new Rate as the value for the UserInterface</li>
     * </ol>
     *
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

        final Instance baseInst = Currency.getBaseCurrency();
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
        return new BigDecimal[] { newRate, currentRate,
                        newRate.divide(currentRate, 12, RoundingMode.HALF_UP), newRateUI };
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
        final Instance curInstance = Sales.getSysConfig().getLink(SalesSettings.CURRENCY4INVOICE);
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
     * SystemConfiguration for a specific date. Returns an Array of rates:
     * <ol>
     * <li>Rate used for Calculation</li>
     * <li>Rate as the value for the UserInterface</li>
     * </ol>
     *
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
     * @return Decimal Format
     * @throws EFapsException on error
     */
    protected DecimalFormat getDigitsformater()
        throws EFapsException
    {
        final DecimalFormat formater = (DecimalFormat) NumberFormat.getInstance(Context.getThreadContext().getLocale());
        formater.setMaximumFractionDigits(2);
        formater.setMinimumFractionDigits(2);
        formater.setRoundingMode(RoundingMode.HALF_UP);
        formater.setParseBigDecimal(true);
        return formater;
    }

    /**
     * Method to get the date format.
     *
     * @param _style String with the date style or null if it's not necessary.
     * @return DateTimeFormatter with the format.
     * @throws EFapsException on error.
     */
    protected DateTimeFormatter getDateFormat(final String _style)
        throws EFapsException
    {
        final DateTimeFormatter fmt;
        if (_style == null) {
            fmt = DateTimeFormat.forStyle("S-");
        } else {
            fmt = DateTimeFormat.forPattern(_style);
        }
        fmt.withLocale(Context.getThreadContext().getLocale());
        return fmt;
    }

    /**
     * Method to construct the map with all the products and its prices.
     *
     * @param _parameter as passe from eFaps API
     * @return Return with the HTML code in a map.
     * @throws EFapsException on error.
     */
    public Return getPriceListHistory(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Map<String, List<String>> mapProd = new HashMap<String, List<String>>();
        final Map<String, String> map = new HashMap<String, String>();
        final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final Map<?, ?> others = (HashMap<?, ?>) _parameter.get(ParameterValues.OTHERS);
        final String[] childOids = (String[]) others.get("selectedRow");
        if (props.containsKey("Interval") && props.containsKey("Range")) {
            final String[] range = ((String) props.get("Range")).split(":");
            final String interval = (String) props.get("Interval");

            final List<String> heads = getPrices4Product(_parameter, range, interval, null);
            for (final String oid : childOids) {
                final PrintQuery print = new PrintQuery(oid);
                print.addAttribute(CIProducts.ProductAbstract.Name, CIProducts.ProductAbstract.Description);
                if (print.execute()) {
                    final List<String> lstPrice =
                                    getPrices4Product(_parameter, range, interval, print.getCurrentInstance());
                    mapProd.put(print.<String>getAttribute(CIProducts.ProductAbstract.Name) + " - "
                                    + print.<String>getAttribute(CIProducts.ProductAbstract.Description), lstPrice);
                }
            }

            final String html = getTable4PriceListHistory(mapProd, heads);
            map.put(EFapsKey.PICKER_JAVASCRIPT.getKey(),
                            "document.getElementsByName('priceHistory')[0].innerHTML='" + html + "';");
            ret.put(ReturnValues.VALUES, map);
        }

        return ret;
    }

    /**
     * Method to obtain the list of average prices for a product.
     *
     * @param _parameter as passed from eFaps API.
     * @param _range String[] with the range value and the type of range.
     * @param _interval String with the interval.
     * @param _instanceProduct Instance with the product instance.
     * @return List with the prices.
     * @throws EFapsException on error
     */
    protected List<String> getPrices4Product(final Parameter _parameter,
                                             final String[] _range,
                                             final String _interval,
                                             final Instance _instanceProduct)
        throws EFapsException
    {
        @SuppressWarnings("unchecked")
        final Map<String, String[]> parameters = (Map<String, String[]>) _parameter.get(ParameterValues.PARAMETERS);
        final List<String> lstPrice = new ArrayList<String>();
        final DateTimeFormatter fmt = getDateFormat(null);
        final DateTimeFormatter fmt2 = getDateFormat("MM/dd");

        if (_range[0].equals(PriceUtil_Base.RangeInterval.MONTH.getKey())) {
            final Integer months = Integer.parseInt(_range[1]);
            if (_interval.equals(PriceUtil_Base.RangeInterval.WEEK.getKey())) {
                final DateTime dateMinusRange = new DateTime().minusMonths(months);
                final DateTime dateFrom = dateMinusRange.minusDays(dateMinusRange.getDayOfMonth() - 1);
                final DateTime dateTo = new DateTime();
                DateTime dateCont = dateFrom;
                while (dateCont.getWeekOfWeekyear() <= dateTo.getWeekOfWeekyear()) {
                    final DateTime dateIni = dateCont;
                    int contDays = dateCont.getDayOfWeek();
                    final ProductPriceList priceInter = new ProductPriceList();
                    int daysLimit = 7;
                    if (dateCont.getWeekOfWeekyear() == dateTo.getWeekOfWeekyear()) {
                        daysLimit = dateTo.getDayOfWeek();
                    }
                    while (contDays <= daysLimit) {
                        parameters.put("date_eFapsDate", new String[] { dateCont.toString(fmt) });
                        if (_instanceProduct != null) {
                            final ProductPrice price = getPrice(_parameter, _instanceProduct.getOid(),
                                            CIProducts.ProductPricelistRetail.uuid);
                            priceInter.setLstInterval(price.getBasePrice());
                        }
                        dateCont = dateCont.plusDays(1);
                        contDays++;
                    }
                    if (_instanceProduct != null) {
                        final String average = priceInter.getPricesAverage();
                        lstPrice.add(average);
                    } else {
                        lstPrice.add(dateIni.toString(fmt2) + "-<br>" + dateCont.minusDays(1).toString(fmt2));
                    }
                    if (dateIni.getWeekOfWeekyear() == dateTo.getWeekOfWeekyear()) {
                        dateCont = dateCont.plusWeeks(1);
                    }
                }
            }
        } else if (_range[0].equals(PriceUtil_Base.RangeInterval.YEAR.getKey())) {
            //TODO
        }
        return lstPrice;
    }

    /**
     * Method to construct the HTML table with the prices for the products.
     *
     * @param _products Map with the products with its average prices.
     * @param _heads List with the names for the header of the table.
     * @return String with the HTML code.
     */
    protected String getTable4PriceListHistory(final Map<String, List<String>> _products,
                                               final List<String> _heads)
    {
        final HtmlTable htmlT = new HtmlTable();
        htmlT.table();
        htmlT.tr();
        htmlT.th("Producto");
        for (final String head : _heads) {
            htmlT.th(head);
        }
        htmlT.trC();
        for (final Entry<String, List<String>> entry : _products.entrySet()) {
            htmlT.tr();
            htmlT.td(entry.getKey());
            for (final String entry2 : entry.getValue()) {
                htmlT.td(entry2);
            }
            htmlT.trC();
        }
        htmlT.tableC();

        return htmlT.toString();
    }

    /**
     * Method to get the instances of the products.
     *
     * @param _parameter as passed from eFaps API
     * @return Return with the instances of products.
     * @throws EFapsException on error.
     */
    public Return getProductsList(final Parameter _parameter)
        throws EFapsException
    {
        final MultiPrint multi = new MultiPrint()
        {
            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final String input = (String) Context.getThreadContext().getSessionAttribute(
                                PriceUtil_Base.PRODFILTER_KEY);
                if (input != null) {
                    final boolean nameSearch = Character.isDigit(input.charAt(0));

                    if (nameSearch) {
                        _queryBldr.addWhereAttrMatchValue(CIProducts.ProductAbstract.Name, input + "*");
                    } else {
                        _queryBldr.addWhereAttrMatchValue(CIProducts.ProductAbstract.Description, input + "*")
                                        .setIgnoreCase(true);
                    }
                }
            }
        };
        return multi.execute(_parameter);
    }

    /**
     * Method to set the filter value to the context.
     *
     * @param _parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return updateField4Product(final Parameter _parameter)
        throws EFapsException
    {
        final String product = _parameter.getParameterValue("product");

        Context.getThreadContext().setSessionAttribute(PriceUtil_Base.PRODFILTER_KEY, product);
        return new Return();
    }

    /**
     * Method to remove the filter value from the context.
     *
     * @param _parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return removeField4Product(final Parameter _parameter)
        throws EFapsException
    {
        // clean the project of the context
        if (Context.getThreadContext().getSessionAttribute(PriceUtil_Base.PRODFILTER_KEY) != null) {
            Context.getThreadContext().setSessionAttribute(PriceUtil_Base.PRODFILTER_KEY, null);
        }

        return new Return();
    }

    /**
     * @param _parameter get a ProductPrice instance
     * @return a ProductPrice instance
     * @throws EFapsException on error
     */
    protected ProductPrice getProductPrice(final Parameter _parameter)
        throws EFapsException
    {
        return new ProductPrice();
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
         * @throws EFapsException on error.
         */
        public ProductPrice()
            throws EFapsException
        {
            final Instance baseInst = Currency.getBaseCurrency();
            this.origCurrencyInstance = baseInst;
            this.currentCurrencyInstance = baseInst;
        }

        /**
         * @param _origCurrencyInst instance of the currency
         * @param _origPrice original price
         * @param _basePrice base price
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

    /**
     * @author jorge
     *
     */
    public enum RangeInterval
    {
        /** */
        WEEK("week"),
        /** */
        MONTH("month"),
        /** */
        YEAR("year");

        /**
         * key.
         */
        private final String key;

        /**
         * @param _key key
         */
        private RangeInterval(final String _key)
        {
            this.key = _key;
        }

        /**
         * Getter method for the instance variable {@link #key}.
         *
         * @return value of instance variable {@link #key}
         */
        public String getKey()
        {
            return this.key;
        }
    }

    /**
     * Represent the interval of days for a range.
     */
    public class ProductPriceList
        implements Serializable
    {

        /**
         * Needed for serialization.
         */
        private static final long serialVersionUID = 1L;

        /**
         * List with the prices for each day of the interval.
         */
        private final List<BigDecimal> lstInterval = new ArrayList<BigDecimal>();

        /**
         * Current price.
         */
        private BigDecimal currentPrice = BigDecimal.ZERO;

        /**
         * @return List with the prices of the interval.
         */
        public List<BigDecimal> getLstInterval()
        {
            return this.lstInterval;
        }

        /**
         * @param _newPrice BigDecimal with the new price to add.
         */
        public void setLstInterval(final BigDecimal _newPrice)
        {
            if (_newPrice != null) {
                this.lstInterval.add(_newPrice);
                setCurrentPrice(_newPrice);
            } else {
                this.lstInterval.add(this.currentPrice);
            }
        }

        /**
         * @param _newPrice BigDecimal with the new price to add.
         */
        private void setCurrentPrice(final BigDecimal _newPrice)
        {
            if (_newPrice.compareTo(this.currentPrice) != 0) {
                this.currentPrice = _newPrice;
            }
        }

        /**
         * Method to calculate the average of prices in the list.
         *
         * @return String with the Average value formated.
         * @throws EFapsException on error.
         */
        public String getPricesAverage()
            throws EFapsException
        {
            BigDecimal totalPrice = BigDecimal.ZERO;
            for (final BigDecimal price : this.lstInterval) {
                totalPrice = totalPrice.add(price);
            }
            final Format formater = getDigitsformater();
            final BigDecimal averagePrice = totalPrice.divide(new BigDecimal(this.lstInterval.size()),
                            BigDecimal.ROUND_HALF_UP);
            return formater.format(averagePrice);
        }

    }
}
