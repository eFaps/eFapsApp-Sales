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
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.PriceUtil_Base.ProductPrice;
import org.efaps.esjp.sales.tax.Tax;
import org.efaps.esjp.sales.tax.TaxCat;
import org.efaps.esjp.sales.tax.TaxCat_Base;
import org.efaps.esjp.sales.tax.Tax_Base;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.ui.wicket.util.DateUtil;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("a9ce907c-0e76-44f9-8dbe-2fdfe2893ae0")
@EFapsApplication("eFapsApp-Sales")
public abstract class Calculator_Base
    extends AbstractCommon
    implements Serializable
{
    /**
     * Keys used in  the SytemConfiguration.
     */
    public enum Keys
    {
        /**
         * How is the price evaluated.
         * Values: <br>
         * <table>
         * <tr><th>Value</th><th>Definition</th><th>Comment</th></tr>
         * <tr>
         * <td>PriceList</td>
         * <td>Use a defined PriceList.</td>
         * <td>Requires that {@link #PRICELIST} is defined.</td>
         * </tr><tr>
         * <td>Latest</td>
         * <td>Evaluate latest price applied.</td>
         * <td>Requires that {@link #PRICEEVALTYPE} is defined.</td>
         * </tr><tr>
         * <td>Latest4Contact</td>
         * <td>Evaluate latest price applied for given contact.</td>
         * <td>Requires that {@link #PRICEEVALTYPE} is defined.</td>
         * </tr>
         * </table>
         */
        PRICEEVALUATION,

        /**
         * Which PriceList is used. (UUID of a PriceList)
         */
        PRICELIST,

        /**
         * Type of the document that is used for the price evaluation. (UUID or Name).
         */
        PRICEEVALTYPE,

        /**
         * is the product price a net price. Defaults to true.
         */
        PRICEISNET,

        /**
         * How is the CrossTotal calculated.
         * Values: "NetTotalPlusTax"
         */
        CROSSTOTAL,

        /**
         * How is the CrossTotal calculated.
         * Values: "NetPricePlusTax"
         */
        CROSSPRICE,

        /**
         * Include the evaluation for minimum price. Defaults to false.
         */
        INCLUDEMINPRICE
    }

    /**
     * Needed for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Quantity.
     */
    private BigDecimal quantity = BigDecimal.ZERO;

    /**
     * The price of the product. As retrieved from the constructor. It is
     * determined from configuration if it is a net or cross value.
     */
    private ProductPrice productPrice;

    /**
     * The price is net.
     */
    private boolean priceIsNet;

    /**
     * The minimum product price as retrieved from the constructor. It is
     * determined from configuration if it is a net or cross value.
     */
    private ProductPrice minProductPrice;

    /**
     * Net unit price.
     */
    private ProductPrice productNetUnitPrice;

    /**
     * Cross unit price.
     */
    private ProductPrice productCrossUnitPrice;

    /**
     * The discount applied to this price.
     */
    private BigDecimal discount = BigDecimal.ZERO;

    /**
     * Is of the taxcategory belonging to this calculator.
     */
    private long taxCatId;

    /**
     * The Calculation must be done without tax.
     */
    private boolean withoutTax = false;

    /**
     * Oid of the product this calculator belongs to.
     */
    private String oid;

    /**
     * Is this Calculator an empty calculator.
     */
    private boolean empty;

    /**
     * Date this Calculator is based on. Used for e.g. date of Taxes.
     */
    private DateTime date;

    /**
     * Must for this Product perception be applied.
     */
    private boolean perceptionProduct;

    /**
     * Parameter from the eFaps API.
     */
    private Parameter parameter;

    /**
     * Key used to access SystemConfigurations belonging to the Document.
     */
    private final String docKey;

    /**
     * Key used to access SystemConfigurations belonging to the Positions.
     */
    private final String posKey;

    /**
     * List of taxes for the Calculator.
     */
    private final List<Tax> taxes = new ArrayList<>();

    /** Stores if the calculator was used for Background or for the UserInterface. */
    private boolean background = false;

    /**
     * Constructor used to instantiate an empty calculator.
     *
     * @throws EFapsException on error
     */
    public Calculator_Base()
        throws EFapsException
    {
        this.taxCatId = 0;
        this.empty = true;
        this.docKey = CISales.DocumentAbstract.getType().getName();
        this.posKey = CISales.PositionAbstract.getType().getName();
        setDate(new DateTime(Context.getThreadContext().getChronology()));
    }

    /**
     * Constructor used to instantiate an empty calculator with long decimal
     * config.
     *
     * @param _parameter Parameter parameter as passed from the eFaps API
     * @param _config Config for this Calculator
     * @throws EFapsException on error
     */
    public Calculator_Base(final Parameter _parameter,
                           final ICalculatorConfig _config)
        throws EFapsException
    {
        this.taxCatId = 0;
        this.empty = true;
        this.docKey = _config.getSysConfKey4Doc(_parameter);
        this.posKey = _config.getSysConfKey4Pos(_parameter);
        setDate(new DateTime(Context.getThreadContext().getChronology()));
    }

    // CHECKSTYLE:OFF
    public Calculator_Base(final Parameter _parameter,
                           final Calculator _calc,
                           final Instance _prodInstance,
                           final BigDecimal _quantity,
                           final BigDecimal _unitPrice,
                           final BigDecimal _discount,
                           final boolean _priceFromDB,
                           final ICalculatorConfig _config)
        throws EFapsException
    // CHECKSTYLE:ON
    {
        this.parameter = _parameter;
        this.empty = false;
        this.docKey = _config.getSysConfKey4Doc(_parameter);
        this.posKey = _config.getSysConfKey4Pos(_parameter);
        final String dateStr = _parameter == null ? null : _parameter.getParameterValue(getDateFieldName(_parameter));
        if (dateStr != null && dateStr != null) {
            setDate(DateUtil.getDateFromParameter(dateStr));
        } else {
            setDate(new DateTime());
        }
        if (_calc != null && _calc.getProductInstance().equals(_prodInstance)) {
            this.taxCatId = _calc.getTaxCatId();
            this.oid = _calc.getOid();
            this.productPrice = _calc.getProductPrice();
            this.minProductPrice = _calc.getMinProductPrice();

            // check if unitprice is set from UI
            if (!_priceFromDB && _unitPrice != null) {
                setPriceFromUI(_parameter, _unitPrice);
                this.priceIsNet = _config.priceFromUIisNet(_parameter);
            } else {
                this.priceIsNet = priceIsNet(_parameter);
                if (_priceFromDB) {
                    this.productPrice = evalPriceFromDB(_parameter);
                    if (_config != null && isIncludeMinRetail(_parameter)) {
                        this.minProductPrice = new PriceUtil().getPrice(_parameter, this.oid, getMinPriceListUUID(),
                                        getPosKey());
                    }
                }
            }
        } else {
            this.oid = _prodInstance != null && _prodInstance.isValid() ? _prodInstance.getOid() : null;
            if (this.oid != null && this.oid.length() > 0) {
                // check if unitprice is set from UI
                if (!_priceFromDB && _unitPrice != null) {
                    setPriceFromUI(_parameter, _unitPrice);
                    this.priceIsNet = _config.priceFromUIisNet(_parameter);
                } else {
                    this.priceIsNet = priceIsNet(_parameter);
                    this.productPrice = evalPriceFromDB(_parameter);

                }
                final PrintQuery print = new PrintQuery(this.oid);
                print.addAttribute(CISales.ProductAbstract.TaxCategory);
                print.executeWithoutAccessCheck();
                this.taxCatId = print.<Long>getAttribute(CISales.ProductAbstract.TaxCategory);
                if (_config != null && isIncludeMinRetail(_parameter)) {
                    this.minProductPrice = new PriceUtil().getPrice(_parameter, this.oid, getMinPriceListUUID(),
                                    getPosKey());
                }
            } else {
                this.taxCatId = 0;
            }
        }
        setDiscount(_discount);

        if (_config != null && isIncludeMinRetail(_parameter) && getMinProductPrice() != null) {
            final BigDecimal discountPrice = getProductPrice().getBasePrice().subtract(getProductPrice().getBasePrice()
                            .divide(new BigDecimal(100)).multiply(getDiscount()));
            if (discountPrice.compareTo(getMinProductPrice().getBasePrice()) < 0) {
                setDiscount(BigDecimal.ZERO);
            }
        }
        setQuantity(_quantity);
        this.perceptionProduct = new Perception().productIsPerception(_parameter, Instance.get(this.oid));
    }

    /**
     * @param _parameter Parameter parameter as passed from the eFaps API
     * @param _calc calculator
     * @param _oid oid of the product
     * @param _quantity quantity
     * @param _unitPrice unit price
     * @param _discount discount
     * @param _priceFromDB must the price set from DB
     * @param _config Config for this Calculator
     * @throws EFapsException on error
     */
    // CHECKSTYLE:OFF
    public Calculator_Base(final Parameter _parameter,
                           final Calculator _calc,
                           final String _oid,
                           final String _quantity,
                           final String _unitPrice,
                           final String _discount,
                           final boolean _priceFromDB,
                           final ICalculatorConfig _config)
        throws EFapsException
    // CHECKSTYLE:ON
    {
        this.parameter = _parameter;
        this.empty = false;
        this.docKey = _config.getSysConfKey4Doc(_parameter);
        this.posKey = _config.getSysConfKey4Pos(_parameter);
        final String dateStr = _parameter == null ? null : _parameter.getParameterValue(getDateFieldName(_parameter));
        if (dateStr != null && dateStr != null) {
            setDate(DateUtil.getDateFromParameter(dateStr));
        } else {
            setDate(new DateTime());
        }
        if (_calc != null && _oid.equals(_calc.getOid())) {
            this.taxCatId = _calc.getTaxCatId();
            this.oid = _calc.getOid();
            this.productPrice = _calc.getProductPrice();
            this.minProductPrice = _calc.getMinProductPrice();

            // check if unitprice is set from UI
            if (!_priceFromDB && _unitPrice != null && _unitPrice.length() > 0) {
                setPriceFromUI(_parameter, _unitPrice);
                this.priceIsNet = _config.priceFromUIisNet(_parameter);
            } else {
                this.priceIsNet = priceIsNet(_parameter);
                if (_priceFromDB) {
                    this.productPrice = evalPriceFromDB(_parameter);
                    if (_config != null && isIncludeMinRetail(_parameter)) {
                        this.minProductPrice = new PriceUtil().getPrice(_parameter, this.oid, getMinPriceListUUID(),
                                        getPosKey());
                    }
                }
            }
        } else {
            this.oid = _oid;
            if (this.oid != null && this.oid.length() > 0) {
                // check if unitprice is set from UI
                if (!_priceFromDB && _unitPrice != null && _unitPrice.length() > 0) {
                    setPriceFromUI(_parameter, _unitPrice);
                    this.priceIsNet = _config.priceFromUIisNet(_parameter);
                } else {
                    this.priceIsNet = priceIsNet(_parameter);
                    this.productPrice = evalPriceFromDB(_parameter);
                }
                final PrintQuery print = new PrintQuery(this.oid);
                print.addAttribute(CISales.ProductAbstract.TaxCategory);
                print.execute();
                this.taxCatId = print.<Long>getAttribute(CISales.ProductAbstract.TaxCategory);
                if (_config != null && isIncludeMinRetail(_parameter)) {
                    this.minProductPrice = new PriceUtil().getPrice(_parameter, this.oid, getMinPriceListUUID(),
                                    getPosKey());
                }
            } else {
                this.taxCatId = 0;
            }
        }
        setDiscount(_discount);

        if (_config != null && isIncludeMinRetail(_parameter) && getMinProductPrice() != null) {
            final BigDecimal discountPrice = getProductPrice().getBasePrice().subtract(getProductPrice().getBasePrice()
                            .divide(new BigDecimal(100)).multiply(getDiscount()));
            if (discountPrice.compareTo(getMinProductPrice().getBasePrice()) < 0) {
                setDiscount(BigDecimal.ZERO);
            }
        }
        setQuantity(_quantity);
        this.perceptionProduct = new Perception().productIsPerception(_parameter, Instance.get(this.oid));
    }

    /**
     * Eval price from db.
     *
     * @param _parameter the _parameter
     * @return the product price
     * @throws EFapsException on error
     */
    protected ProductPrice evalPriceFromDB(final Parameter _parameter)
        throws EFapsException
    {
        final ProductPrice ret;
        final String config = getConfig(getDocKey(), Keys.PRICEEVALUATION, "PriceList");
        switch (config) {
            case "Latest":
                ret = new PriceUtil().getLatestPrice(_parameter, getProductInstance(), getPriceEvalType(),
                                this.priceIsNet, false);
                break;
            case "Latest4Contact":
                ret = new PriceUtil().getLatestPrice(_parameter, getProductInstance(), getPriceEvalType(),
                                this.priceIsNet, true);
                break;
            case "PriceList":
            default:
                ret = new PriceUtil().getPrice(_parameter, getOid(), getPriceListUUID(), getPosKey());
                break;
        }
        return ret;
    }

    /**
     * Gets the price evaluation type.
     *
     * @return the price eval type
     * @throws EFapsException on error
     */
    protected Type getPriceEvalType()
        throws EFapsException
    {
        final String typeStr = getConfig(getDocKey(), Keys.PRICEEVALTYPE,
                        CISales.DocumentSumAbstract.getType().getUUID().toString());
        final Type ret;
        if (isUUID(typeStr)) {
            ret = Type.get(UUID.fromString(typeStr));
        } else {
            ret = Type.get(typeStr);
        }
        return ret;
    }

    /**
     * Checks if is include min retail.
     *
     * @param _parameter the _parameter
     * @return true, if is include min retail
     * @throws EFapsException on error
     */
    protected boolean isIncludeMinRetail(final Parameter _parameter)
        throws EFapsException
    {
        return "true".equalsIgnoreCase(getConfig(getDocKey(), Keys.INCLUDEMINPRICE, "false"));
    }

    /**
     * Checks if is include min retail.
     *
     * @param _parameter the _parameter
     * @return true, if is include min retail
     * @throws EFapsException on error
     */
    protected boolean priceIsNet(final Parameter _parameter)
        throws EFapsException
    {
        return "true".equalsIgnoreCase(getConfig(getDocKey(), Keys.PRICEISNET, "true"));
    }

    /**
     * Gets the doc key.
     *
     * @return the doc key
     */
    protected String getDocKey()
    {
        return this.docKey;
    }

    /**
     * Gets the pos key.
     *
     * @return the pos key
     */
    protected String getPosKey()
    {
        return this.posKey;
    }

    /**
     * Gets the config.
     *
     * @param _mainKey the _main key
     * @param _propkey the _propkey
     * @return the value for the key
     * @throws EFapsException on error
     */
    protected String getConfig(final String _mainKey,
                               final Keys _propkey)
        throws EFapsException
    {
        return getConfig(_mainKey, _propkey, null);
    }

    /**
     * Gets the config.
     *
     * @param _mainKey the _main key
     * @param _propkey the _propkey
     * @param _default dafeult value
     * @return the value for the key
     * @throws EFapsException on error
     */
    protected String getConfig(final String _mainKey,
                               final Keys _propkey,
                               final String _default)
        throws EFapsException
    {
        final Properties props = Sales.CALCULATORCONFIG.get();
        final String keyStr = _mainKey + "." + _propkey.name();
        return _default == null ? props.getProperty(keyStr) : props.getProperty(keyStr, _default);
    }

    /**
     *
     * Get the name of the field that contains the date.
     *
     * @param _parameter Parmeter as passed by the eFaps API
     * @return name of the date field
     */
    protected String getDateFieldName(final Parameter _parameter)
    {
        return "date_eFapsDate";
    }

    /**
     * Set the price given by the UI.
     *
     * @param _parameter Parmeter as passed by the eFaps API
     * @param _unitPrice unit price
     * @throws EFapsException on error
     */
    protected void setPriceFromUI(final Parameter _parameter,
                                  final BigDecimal _unitPrice)
        throws EFapsException
    {
        Instance currInst = new Currency().getCurrencyFromUI(_parameter);

        final Instance baseInst = Currency.getBaseCurrency();

        if (currInst == null) {
            currInst = baseInst;
        }
        if (this.productPrice == null) {
            this.productPrice = getNewPrice();
        }
        this.productPrice.setCurrentPrice(_unitPrice);
        if (currInst.equals(baseInst)) {
            this.productPrice.setBasePrice(_unitPrice);
        } else {
            final PriceUtil priceutil = new PriceUtil();
            final BigDecimal rate = priceutil.getExchangeRate(_parameter, currInst);
            final BigDecimal newprice = _unitPrice.divide(rate, 8, BigDecimal.ROUND_HALF_UP);
            this.productPrice.setBasePrice(newprice);
            this.productPrice.setOrigPrice(newprice);
        }
    }

    /**
     * Set the price given by the UI.
     *
     * @param _parameter Parmeter as passed by the eFaps API
     * @param _unitPrice unit price
     * @throws EFapsException on error
     */
    protected void setPriceFromUI(final Parameter _parameter,
                                  final String _unitPrice)
        throws EFapsException
    {
        final DecimalFormat format = NumberFormatter.get().getFrmt4UnitPrice(getPosKey());

        final BigDecimal unitPrice = parse(_unitPrice).setScale(format.getMaximumFractionDigits(),
                        BigDecimal.ROUND_HALF_UP);
        setPriceFromUI(_parameter, unitPrice);
    }

    /**
     * @return the UUID of the type used for the pricelist
     * @throws EFapsException on error
     *
     */
    protected UUID getPriceListUUID()
        throws EFapsException
    {
        final String uuid = getConfig(getDocKey(), Keys.PRICELIST);
        UUID ret = null;
        if (uuid != null) {
            if (isUUID(uuid)) {
                ret = UUID.fromString(uuid);
            }
        } else {
            ret = CIProducts.ProductPricelistRetail.uuid;
        }
        return ret;
    }

    /**
     * @return the Type of the pricelist applied
     * @throws EFapsException on error
     */
    public Type getPriceListType()
        throws EFapsException
    {
        return Type.get(getPriceListUUID());
    }

    /**
     * @return the UUID of the type used for the minimum pricelist
     */
    protected UUID getMinPriceListUUID()
    {
        return CIProducts.ProductPricelistMinRetail.uuid;
    }

    /**
     * Getter method for the instance variable {@link #oid}.
     *
     * @return value of instance variable {@link #oid}
     */
    public String getOid()
    {
        return this.oid;
    }

    /**
     * @return the productInstance
     */
    public Instance getProductInstance()
    {
        return Instance.get(getOid());
    }

    /**
     * Getter method for instance variable {@link #withoutTax}.
     *
     * @return value of instance variable {@link #withoutTax}
     */
    public boolean isWithoutTax()
    {
        return this.withoutTax;
    }

    /**
     * Setter method for instance variable {@link #withoutTax}.
     *
     * @param _withoutTax value for instance variable {@link #withoutTax}
     */
    public void setWithoutTax(final boolean _withoutTax)
    {
        this.withoutTax = _withoutTax;
    }

    /**
     * Getter method for the instance variable {@link #perceptionProduct}.
     *
     * @return value of instance variable {@link #perceptionProduct}
     */
    public boolean isPerceptionProduct()
    {
        return this.perceptionProduct;
    }

    /**
     * Setter method for instance variable {@link #perceptionProduct}.
     *
     * @param _perceptionProduct value for instance variable
     *            {@link #perceptionProduct}
     */
    public void setPerceptionProduct(final boolean _perceptionProduct)
    {
        this.perceptionProduct = _perceptionProduct;
    }

    /**
     * Setter method for instance variable {@link #quantity}.
     *
     * @param _quantity value for instance variable {@link #quantity}
     */
    public void setQuantity(final BigDecimal _quantity)
    {
        this.quantity = _quantity;
    }

    /**
     * Set the quantity.
     *
     * @param _quantity quantity
     * @throws EFapsException on error
     */
    public void setQuantity(final String _quantity)
        throws EFapsException
    {
        this.quantity = _quantity != null && _quantity.length() > 0 ? parse(_quantity) : BigDecimal.ONE;
    }

    /**
     * Setter method for instance variable {@link #productNetUnitPrice}.
     *
     * @param _netUnitPrices value for instance variable
     *            {@link #productNetUnitPrice}
     * @throws EFapsException on error
     */
    public void setNetUnitPrice(final String _netUnitPrices)
        throws EFapsException
    {
        setNetUnitPrice(_netUnitPrices != null && _netUnitPrices.length() > 0
                        ? parse(_netUnitPrices) : BigDecimal.ZERO);
    }

    /**
     * Setter method for instance variable {@link #productNetUnitPrice}.
     *
     * @param _netUnitPrice value for instance variable
     *            {@link #productNetUnitPrice}
     * @throws EFapsException on error
     */
    public void setNetUnitPrice(final BigDecimal _netUnitPrice)
        throws EFapsException
    {
        if (this.productNetUnitPrice == null) {
            this.productNetUnitPrice = getNewPrice();
        }
        this.productNetUnitPrice.setCurrentPrice(_netUnitPrice);
        this.productNetUnitPrice.setOrigPrice(_netUnitPrice);
        this.productNetUnitPrice.setBasePrice(_netUnitPrice.multiply(getProductPrice().getBaseRate()));
        if (this.taxCatId > 0) {
            final List<Tax> taxesTmp = getTaxes();
            BigDecimal targetPrice = _netUnitPrice;
            for (final Tax tax : taxesTmp) {
                final BigDecimal factor = tax.getFactor();
                if (factor.compareTo(BigDecimal.ONE) != 0) {
                    targetPrice = targetPrice.add(_netUnitPrice.multiply(factor));
                }
            }
            if (this.productCrossUnitPrice == null) {
                this.productCrossUnitPrice = getNewPrice();
            }
            this.productCrossUnitPrice.setCurrentPrice(targetPrice);
            this.productCrossUnitPrice.setOrigPrice(targetPrice);
            this.productCrossUnitPrice.setBasePrice(targetPrice.multiply(getProductPrice().getBaseRate()));
        }
    }

    /**
     * Setter method for instance variable {@link #setCrossUnitPrice}.
     *
     * @param _crossPrice crossPrice.
     * @throws EFapsException on error.
     */
    public void setCrossPrice(final BigDecimal _crossPrice)
        throws EFapsException
    {
        setCrossUnitPrice(_crossPrice.setScale(12, BigDecimal.ROUND_HALF_UP)
                        .divide(getQuantity(), BigDecimal.ROUND_HALF_UP));
    }

    /**
     * Setter method for instance variable {@link #setCrossPrice}.
     *
     * @param _crossPrice crossPrice string.
     * @throws EFapsException on error.
     */
    public void setCrossPrice(final String _crossPrice)
        throws EFapsException
    {
        setCrossPrice(_crossPrice != null && _crossPrice.length() > 0
                        ? parse(_crossPrice) : BigDecimal.ZERO);
    }

    /**
     * Setter method for instance variable {@link #setNetUnitPrice}.
     *
     * @param _netPrice netPrice
     * @throws EFapsException on error.
     */
    public void setNetPrice(final BigDecimal _netPrice)
        throws EFapsException
    {
        setNetUnitPrice(_netPrice.setScale(12, BigDecimal.ROUND_HALF_UP)
                        .divide(getQuantity(), BigDecimal.ROUND_HALF_UP));
    }

    /**
     * Setter method for instance variable {@link #setNetPrice}.
     *
     * @param _netPrice netPrice as string.
     * @throws EFapsException on error.
     */
    public void setNetPrice(final String _netPrice)
        throws EFapsException
    {
        setNetPrice(_netPrice != null && _netPrice.length() > 0
                        ? parse(_netPrice) : BigDecimal.ZERO);
    }

    /**
     * @return get the new Price
     * @throws EFapsException on error
     */
    protected ProductPrice getNewPrice()
        throws EFapsException
    {
        final Instance baseInst = Currency.getBaseCurrency();
        final ProductPrice ret = new PriceUtil().getProductPrice(getParameter());
        ret.setBaseRate(this.productPrice == null ? BigDecimal.ONE : this.productPrice.getBaseRate());
        ret.setCurrentCurrencyInstance(this.productPrice == null
                        ? baseInst : this.productPrice.getCurrentCurrencyInstance());
        ret.setOrigCurrencyInstance(this.productPrice == null
                        ? baseInst : this.productPrice.getOrigCurrencyInstance());
        return ret;
    }

    /**
     * To be used by implementation to be able to pass Parameter.
     *
     * @return null
     */
    protected Parameter getParameter()
    {
        return this.parameter;
    }

    /**
     * @return string representation of the net unit price.
     * @throws EFapsException on error
     */
    public String getNetUnitPriceStr()
        throws EFapsException
    {
        return NumberFormatter.get().getFormatter().format(getNetUnitPrice());
    }

    /**
     * Get the net unit price formated with the given formater.
     *
     * @return formated string representation of the net unit price
     * @throws EFapsException on error
     */
    public String getNetUnitPriceFmtStr()
        throws EFapsException
    {
        return NumberFormatter.get().getFrmt4UnitPrice(getPosKey()).format(getNetUnitPrice());
    }

    /**
     * @return the net unit price
     * @throws EFapsException on error
     */
    public BigDecimal getNetUnitPrice()
        throws EFapsException
    {
        final BigDecimal ret;
        if (this.productNetUnitPrice == null) {
            ret = getProductNetUnitPrice().getCurrentPrice();
        } else {
            ret = this.productNetUnitPrice.getCurrentPrice();
        }
        return ret;
    }

    /**
     * Setter method for instance variable {@link #productNetUnitPrice}.
     *
     * @param _crossUnitPrice cross unit price
     * @throws EFapsException on error
     */
    public void setCrossUnitPrice(final String _crossUnitPrice)
        throws EFapsException
    {
        setCrossUnitPrice(_crossUnitPrice != null && _crossUnitPrice.length() > 0
                        ? parse(_crossUnitPrice) : BigDecimal.ZERO);
    }

    /**
     * Setter method for instance variable {@link #productNetUnitPrice}.
     *
     * @param _crossUnitPrice cross unit price
     * @throws EFapsException on error
     */
    public void setCrossUnitPrice(final BigDecimal _crossUnitPrice)
        throws EFapsException
    {
        if (this.productCrossUnitPrice == null) {
            this.productCrossUnitPrice = getNewPrice();
        }
        this.productCrossUnitPrice.setCurrentPrice(_crossUnitPrice);
        this.productCrossUnitPrice.setOrigPrice(_crossUnitPrice);
        this.productCrossUnitPrice.setBasePrice(_crossUnitPrice.multiply(this.productCrossUnitPrice.getBaseRate()));

        if (this.taxCatId > 0) {
            final List<Tax> taxesTmp = getTaxes();
            BigDecimal targetPrice = _crossUnitPrice;
            for (final Tax tax : taxesTmp) {
                final BigDecimal factor = tax.getFactor();
                if (factor.compareTo(BigDecimal.ONE) != 0) {
                    targetPrice = targetPrice.subtract(_crossUnitPrice.subtract(_crossUnitPrice
                                    .divide(BigDecimal.ONE.add(factor), BigDecimal.ROUND_HALF_UP)));
                }
            }
            if (this.productNetUnitPrice == null) {
                this.productNetUnitPrice = getNewPrice();
            }
            this.productNetUnitPrice.setCurrentPrice(targetPrice);
            this.productNetUnitPrice.setOrigPrice(targetPrice);
            this.productNetUnitPrice.setBasePrice(targetPrice.multiply(this.productNetUnitPrice.getBaseRate()));
        }
    }

    /**
     * @return string representation of the cross unit price.
     * @throws EFapsException on error
     */
    public String getCrossUnitPriceStr()
        throws EFapsException
    {
        return NumberFormatter.get().getFormatter().format(getCrossUnitPrice());
    }

    /**
     * Get the cross unit price formated with the given formater.
     *
     * @return formated string representation of the net unit price
     * @throws EFapsException on error
     */
    public String getCrossUnitPriceFmtStr()
        throws EFapsException
    {
        return NumberFormatter.get().getFrmt4UnitPrice(getPosKey()).format(getCrossUnitPrice());
    }

    /**
     * Get the cross unit price.
     *
     * @return cross unit price
     * @throws EFapsException on error
     */
    public BigDecimal getCrossUnitPrice()
        throws EFapsException
    {
        final BigDecimal ret;
        if (this.withoutTax) {
            ret = getNetUnitPrice();
        } else {
            if (this.productCrossUnitPrice == null) {
                ret = getProductCrossUnitPrice().getCurrentPrice();
            } else {
                ret = this.productCrossUnitPrice.getCurrentPrice();
            }
        }
        return ret;
    }

    /**
     * Get the cross price formated with the given formater.
     *
     * @return formated string representation of the net unit price
     * @throws EFapsException on error
     */
    public String getDiscountFmtStr()
        throws EFapsException
    {
        return NumberFormatter.get().getFrmt4Discount(getPosKey()).format(getDiscount());
    }

    /**
     * @return string representation of the discount.
     * @throws EFapsException on error
     */
    public String getDiscountStr()
        throws EFapsException
    {
        return NumberFormatter.get().getFormatter().format(getDiscount());
    }

    /**
     * Getter method for instance variable {@link #discount}.
     *
     * @return value of instance variable {@link #discount}
     */
    public BigDecimal getDiscount()
    {
        return this.discount;
    }

    /**
     * Method to set the discount from a String.
     *
     * @param _discount discount
     * @throws EFapsException on error
     */
    public void setDiscount(final String _discount)
        throws EFapsException
    {
        this.discount = _discount != null && _discount.length() > 0 ? parse(_discount) : BigDecimal.ZERO;
    }

    /**
     * Setter method for instance variable {@link #discount}.
     *
     * @param _discount value for instance variable {@link #discount}
     */
    public void setDiscount(final BigDecimal _discount)
    {
        this.discount = _discount;
    }

    /**
     * @return the discount net unit price
     * @throws EFapsException on error
     */
    public BigDecimal getDiscountNetUnitPrice()
        throws EFapsException
    {
        return getProductDiscountNetUnitPrice().getCurrentPrice();
    }

    /**
     * net unit price - (net unit price / 100 * discount).
     *
     * @return discount price for the product
     * @throws EFapsException on error
     */
    public ProductPrice getProductDiscountNetUnitPrice()
        throws EFapsException
    {
        final ProductPrice ret = getNewPrice();
        final ProductPrice unit = getProductNetUnitPrice();

        final DecimalFormat format = NumberFormatter.get().getFrmt4UnitPrice(getPosKey());
        final int decDigCant = format.getMaximumFractionDigits();

        ret.setBasePrice(unit.getBasePrice().subtract(unit.getBasePrice().divide(new BigDecimal(100))
                        .multiply(getDiscount())).setScale(decDigCant, BigDecimal.ROUND_HALF_UP));
        ret.setCurrentPrice(unit.getCurrentPrice().subtract(unit.getCurrentPrice().divide(new BigDecimal(100))
                        .multiply(getDiscount())).setScale(decDigCant, BigDecimal.ROUND_HALF_UP));
        ret.setOrigPrice(unit.getOrigPrice().subtract(unit.getOrigPrice().divide(new BigDecimal(100))
                        .multiply(getDiscount())).setScale(decDigCant, BigDecimal.ROUND_HALF_UP));
        return ret;
    }

    /**
     * Get the discount net unit price formated with the given formater.
     *
     * @return formated string representation of the net unit price
     * @throws EFapsException on error
     */
    public String getDiscountNetUnitPriceFmtStr()
        throws EFapsException
    {
        return NumberFormatter.get().getFrmt4UnitPrice(getPosKey()).format(getDiscountNetUnitPrice());
    }

    /**
     * @return string representation of the discount net unit price.
     * @throws EFapsException on error
     */
    public String getDiscountNetUnitPriceStr()
        throws EFapsException
    {
        return NumberFormatter.get().getFormatter().format(getDiscountNetUnitPrice());
    }

    /**
     * @param _unitprice unit price to set
     * @throws EFapsException on error
     */
    public void setUnitPrice(final String _unitprice)
        throws EFapsException
    {
        setUnitPrice(_unitprice.length() > 0 ? parse(_unitprice) : BigDecimal.ZERO);
    }

    /**
     * @param _unitprice unit price to set
     * @throws EFapsException on error
     */
    public void setUnitPrice(final BigDecimal _unitprice)
        throws EFapsException
    {
        this.productPrice.setCurrentPrice(_unitprice);
        this.productPrice.setOrigPrice(_unitprice);
        this.productPrice.setBasePrice(_unitprice.multiply(getProductPrice().getBaseRate()));
        this.productNetUnitPrice = null;
        this.productCrossUnitPrice = null;
    }

    /**
     * Getter method for the instance variable {@link #productPrice}.
     *
     * @return value of instance variable {@link #productPrice}
     * @throws EFapsException on error
     */
    public ProductPrice getProductPrice()
        throws EFapsException
    {
        if (this.productPrice == null) {
            this.productPrice = new PriceUtil().new ProductPrice();
            this.productPrice.setBasePrice(BigDecimal.ZERO);
            this.productPrice.setCurrentPrice(BigDecimal.ZERO);
            this.productPrice.setOrigPrice(BigDecimal.ZERO);
            this.productPrice.setBaseRate(BigDecimal.ONE);
        }
        return this.productPrice;
    }

    /**
     * Getter method for instance variable {@link #quantity}.
     *
     * @return value of instance variable {@link #quantity}
     */
    public BigDecimal getQuantity()
    {
        return this.quantity;
    }

    /**
     * @return string representation of the quantity.
     * @throws EFapsException on error
     */
    public String getQuantityStr()
        throws EFapsException
    {
        return NumberFormatter.get().getFormatter().format(getQuantity());
    }

    /**
     * Gets the quantity fmt str.
     *
     * @return formated Quantity
     * @throws EFapsException on error
     */
    public String getQuantityFmtStr()
        throws EFapsException
    {
        return NumberFormatter.get().getFrmt4Quantity(getPosKey()).format(getQuantity());
    }

    /**
     * @return the net price
     * @throws EFapsException on errro
     */
    public BigDecimal getNetPrice()
        throws EFapsException
    {
        return getProductNetPrice().getCurrentPrice();
    }

    /**
     * Discount net unit price * quantity.
     *
     * @return net price for the product
     * @throws EFapsException on error
     */
    public ProductPrice getProductNetPrice()
        throws EFapsException
    {
        final ProductPrice ret = getNewPrice();
        final ProductPrice unit = getProductDiscountNetUnitPrice();
        ret.setBasePrice(unit.getBasePrice().multiply(getQuantity()));
        ret.setOrigPrice(unit.getOrigPrice().multiply(getQuantity()));
        ret.setCurrentPrice(unit.getCurrentPrice().multiply(getQuantity()));
        return ret;
    }

    /**
     * Get the net price formated with the given formater.
     *
     * @return formated string representation of the net unit price
     * @throws EFapsException on error
     */
    public String getNetPriceFmtStr()
        throws EFapsException
    {
        return NumberFormatter.get().getFrmt4UnitPrice(getPosKey()).format(getNetPrice());
    }

    /**
     * @return string representation of the cross price.
     * @throws EFapsException on error
     */
    public String getCrossPriceStr()
        throws EFapsException
    {
        return NumberFormatter.get().getFormatter().format(getCrossPrice());
    }

    /**
     * @return the cross price
     * @throws EFapsException on error
     */
    public BigDecimal getCrossPrice()
        throws EFapsException
    {
        final BigDecimal ret;
        if (this.withoutTax) {
            ret = getNetPrice();
        } else {
            ret = getProductCrossPrice().getCurrentPrice();
        }
        return ret;
    }

    /**
     * @return product cross price
     * @throws EFapsException on error
     */
    public ProductPrice getProductCrossPrice()
        throws EFapsException
    {
        final ProductPrice ret;
        final String config = getConfig(getPosKey(), Keys.CROSSPRICE, "default");
        switch (config) {
            case "NetPricePlusTax":
                final ProductPrice netPrice = getProductNetPrice();
                ret = evalProductCrossUnitPrice(netPrice);
                break;
            default:
                ret = getNewPrice();
                final ProductPrice unit = getProductDiscountCrossUnitPrice();
                ret.setBasePrice(unit.getBasePrice().multiply(getQuantity()));
                ret.setOrigPrice(unit.getOrigPrice().multiply(getQuantity()));
                ret.setCurrentPrice(unit.getCurrentPrice().multiply(getQuantity()));
                break;
        }
        return ret;
    }

    /**
     * Calculate the product price with discount, but calculate the tax after
     * the discount when the factor is different of 1, because the discount
     * price has to be rounded before.
     *
     * @return @return discount price for the product, depending the tax factor.
     * @throws EFapsException on error
     */
    public ProductPrice getProductDiscountCrossUnitPrice()
        throws EFapsException
    {
        final DecimalFormat format = NumberFormatter.get().getFrmt4UnitPrice(getPosKey());
        final int decDigCant = format.getMaximumFractionDigits();

        final ProductPrice ret = getNewPrice();
        final ProductPrice unit = getProductCrossUnitPrice();
        ret.setBasePrice(unit.getBasePrice().subtract(unit.getBasePrice().divide(new BigDecimal(100))
                        .multiply(getDiscount())).setScale(decDigCant, BigDecimal.ROUND_HALF_UP));
        ret.setOrigPrice(unit.getOrigPrice().subtract(unit.getOrigPrice().divide(new BigDecimal(100))
                        .multiply(getDiscount())).setScale(decDigCant, BigDecimal.ROUND_HALF_UP));
        ret.setCurrentPrice(unit.getCurrentPrice().subtract(unit.getCurrentPrice().divide(new BigDecimal(100))
                        .multiply(getDiscount())).setScale(decDigCant, BigDecimal.ROUND_HALF_UP));
        return ret;

    }

    /**
     * Getter method for the instance variable {@link #minProductPrice}.
     *
     * @return value of instance variable {@link #minProductPrice}
     * @throws EFapsException on error
     */
    public ProductPrice getMinProductPrice()
        throws EFapsException
    {
        if (this.minProductPrice == null) {
            this.minProductPrice = new PriceUtil().new ProductPrice();
            this.minProductPrice.setBasePrice(BigDecimal.ZERO);
            this.minProductPrice.setCurrentPrice(BigDecimal.ZERO);
            this.minProductPrice.setOrigPrice(BigDecimal.ZERO);
            this.minProductPrice.setBaseRate(BigDecimal.ONE);
        }
        return this.minProductPrice;
    }

    /**
     * Setter method for instance variable {@link #minProductPrice}.
     *
     * @param _minProductPrice value for instance variable
     *            {@link #minProductPrice}
     */

    public void setMinProductPrice(final ProductPrice _minProductPrice)
    {
        this.minProductPrice = _minProductPrice;
    }

    /**
     * Get the cross price formated with the given formater.
     *
     * @return formated string representation of the net unit price
     * @throws EFapsException on error
     */
    public String getCrossPriceFmtStr()
        throws EFapsException
    {
        return NumberFormatter.get().getFrmt4UnitPrice(getPosKey()).format(getCrossPrice());
    }

    /**
     * @return string representation of the net price.
     * @throws EFapsException on error
     */
    public String getNetPriceStr()
        throws EFapsException
    {
        return NumberFormatter.get().getFormatter().format(getNetPrice());
    }

    /**
     * @return unit cross price
     * @throws EFapsException on error
     */
    public ProductPrice getProductCrossUnitPrice()
        throws EFapsException
    {
        if (this.productCrossUnitPrice == null) {
            this.productCrossUnitPrice = evalProductCrossUnitPrice(this.productPrice);
        }
        return this.productCrossUnitPrice;
    }

    /**
     * @param _price depending if it is calculating the cross unit price
     * @return eval new cross unit price
     * @throws EFapsException on error
     */
    protected ProductPrice evalProductCrossUnitPrice(final ProductPrice _price)
        throws EFapsException
    {
        final ProductPrice ret = getNewPrice();

        if (_price == null) {
            ret.setBasePrice(BigDecimal.ZERO);
            ret.setCurrentPrice(BigDecimal.ZERO);
            ret.setOrigPrice(BigDecimal.ZERO);
        } else {
            if (this.priceIsNet && this.taxCatId > 0) {
                final List<Tax> taxesTmp = getTaxes();
                final BigDecimal currentPrice = _price.getCurrentPrice() == null
                                ? BigDecimal.ZERO : _price.getCurrentPrice();
                final BigDecimal basePrice = _price.getBasePrice() == null ? BigDecimal.ZERO : _price.getBasePrice();
                final BigDecimal origPrice = _price.getOrigPrice() == null ? BigDecimal.ZERO : _price.getOrigPrice();
                BigDecimal tCurrentPrice = currentPrice;
                BigDecimal tBasePrice = basePrice;
                BigDecimal tOrigPrice = origPrice;

                for (final Tax tax : taxesTmp) {
                    final BigDecimal factor = tax.getFactor();
                    if (factor.compareTo(BigDecimal.ONE) != 0) {
                        if (currentPrice.compareTo(BigDecimal.ZERO) != 0) {
                            tCurrentPrice = tCurrentPrice.add(currentPrice.multiply(factor));
                        }
                        if (basePrice.compareTo(BigDecimal.ZERO) != 0) {
                            tBasePrice = tBasePrice.add(basePrice.multiply(factor));
                        }
                        if (origPrice.compareTo(BigDecimal.ZERO) != 0) {
                            tOrigPrice = tOrigPrice.add(origPrice.multiply(factor));
                        }
                    }
                }
                ret.setCurrentPrice(tCurrentPrice);
                ret.setBasePrice(tBasePrice);
                ret.setOrigPrice(tOrigPrice);

            } else {
                ret.setCurrentPrice(_price.getCurrentPrice() == null
                                ? BigDecimal.ZERO
                                : _price.getCurrentPrice());
                ret.setBasePrice(_price.getBasePrice() == null
                                ? BigDecimal.ZERO
                                : _price.getBasePrice());
                ret.setOrigPrice(_price.getOrigPrice() == null
                                ? BigDecimal.ZERO
                                : _price.getOrigPrice());
            }
        }
        return ret;
    }

    /**
     * @return eval new cross unit price
     * @throws EFapsException on error
     */
    protected ProductPrice evalProductNetUnitPrice()
        throws EFapsException
    {
        final ProductPrice ret = getNewPrice();

        if (this.productPrice == null) {
            ret.setBasePrice(BigDecimal.ZERO);
            ret.setCurrentPrice(BigDecimal.ZERO);
            ret.setOrigPrice(BigDecimal.ZERO);
        } else {
            if (this.priceIsNet && this.taxCatId > 0) {
                ret.setCurrentPrice(this.productPrice.getCurrentPrice() == null
                                ? BigDecimal.ZERO
                                : this.productPrice.getCurrentPrice());
                ret.setBasePrice(this.productPrice.getBasePrice() == null
                                ? BigDecimal.ZERO
                                : this.productPrice.getBasePrice());
                ret.setOrigPrice(this.productPrice.getOrigPrice() == null
                                ? BigDecimal.ZERO
                                : this.productPrice.getOrigPrice());
            } else {
                final List<Tax> taxesTmp = getTaxes();
                final BigDecimal currentPrice = this.productPrice.getCurrentPrice() == null ? BigDecimal.ZERO
                                : this.productPrice.getCurrentPrice();
                final BigDecimal basePrice = this.productPrice.getBasePrice() == null
                                ? BigDecimal.ZERO : this.productPrice.getBasePrice();
                final BigDecimal origPrice = this.productPrice.getOrigPrice() == null
                                ? BigDecimal.ZERO : this.productPrice.getOrigPrice();
                BigDecimal tCurrentPrice = currentPrice;
                BigDecimal tBasePrice = basePrice;
                BigDecimal tOrigPrice = origPrice;

                for (final Tax tax : taxesTmp) {
                    final BigDecimal factor = tax.getFactor();
                    if (factor.compareTo(BigDecimal.ONE) != 0) {
                        if (currentPrice.compareTo(BigDecimal.ZERO) != 0) {
                            tCurrentPrice = tCurrentPrice.subtract(currentPrice.subtract(currentPrice
                                            .divide(BigDecimal.ONE.add(factor), BigDecimal.ROUND_HALF_UP)));
                        }
                        if (basePrice.compareTo(BigDecimal.ZERO) != 0) {
                            tBasePrice = tBasePrice.subtract(basePrice.subtract(basePrice.divide(BigDecimal.ONE
                                            .add(factor), BigDecimal.ROUND_HALF_UP)));
                        }
                        if (origPrice.compareTo(BigDecimal.ZERO) != 0) {
                            tOrigPrice = tOrigPrice.subtract(origPrice.subtract(origPrice.divide(BigDecimal.ONE
                                            .add(factor), BigDecimal.ROUND_HALF_UP)));
                        }
                    }
                }
                ret.setCurrentPrice(tCurrentPrice);
                ret.setBasePrice(tBasePrice);
                ret.setOrigPrice(tOrigPrice);
            }
        }
        return ret;
    }

    /**
     * @return net unit price
     * @throws EFapsException on error
     */
    public ProductPrice getProductNetUnitPrice()
        throws EFapsException
    {
        if (this.productNetUnitPrice == null) {
            this.productNetUnitPrice = evalProductNetUnitPrice();
        }
        return this.productNetUnitPrice;
    }

    /**
     * @return perception
     * @throws EFapsException on error
     */
    public BigDecimal getPerception()
        throws EFapsException
    {
        final BigDecimal ret;
        if (isPerceptionProduct()) {
            ret = new Perception().calculatePerception(getParameter(), this);
        } else {
            ret = BigDecimal.ZERO;
        }
        return ret;
    }

    /**
     * @return string representation of the Perception.
     * @throws EFapsException on error
     */
    public String getPerceptionStr()
        throws EFapsException
    {
        return NumberFormatter.get().getFormatter().format(getPerception());
    }

    /**
     * Get the Perception formated with the given formater.
     *
     * @return formated string representation of the net unit price
     * @throws EFapsException on error
     */
    public String getPerceptionFmtStr()
        throws EFapsException
    {
        return NumberFormatter.get().getFrmt4UnitPrice(getPosKey()).format(getPerception());
    }

    /**
     * @return the list of taxes to be applied
     * @throws EFapsException on error
     */
    public List<Tax> getTaxes()
        throws EFapsException
    {
        if (this.taxes.isEmpty() && getTaxCatId() > 0) {
            this.taxes.addAll(getTaxCat().getTaxes(getDate()));
        }
        return this.taxes;
    }

    /**
     * Get a map of taxes for teh current position.
     * @return mapping of tax to amount
     * @throws EFapsException on error
     */
    public Map<Tax, BigDecimal> getTaxesAmounts()
        throws EFapsException
    {
        final Map<Tax, BigDecimal> ret = new HashMap<>();
        final List<Tax> taxestemp = getTaxes();
        for (final Tax tax : taxestemp) {
            final BigDecimal net = getNetPrice();
            if (tax.equals(Tax_Base.getZeroTax())) {
                ret.put(tax, BigDecimal.ZERO);
            } else {
                final DecimalFormat format = NumberFormatter.get().getFrmt4Tax(getPosKey());
                final int decDigCant = format.getMaximumFractionDigits();
                ret.put(tax, net.multiply(tax.getFactor()).setScale(decDigCant, BigDecimal.ROUND_HALF_UP));
            }
        }
        return ret;
    }

    /**
     * @return the taxcat applied
     * @throws EFapsException on error
     */
    public TaxCat getTaxCat()
        throws EFapsException
    {
        return TaxCat_Base.get(getTaxCatId());
    }

    /**
     * Getter method for the instance variable {@link #taxcatId}.
     *
     * @return value of instance variable {@link #taxcatId}
     */
    public long getTaxCatId()
    {
        return this.taxCatId;
    }

    /**
     * Setter method for instance variable {@link #taxCatId}.
     *
     * @param _taxCatId value for instance variable {@link #taxCatId}
     */
    public void setTaxCatId(final long _taxCatId)
    {
        this.taxCatId = _taxCatId;
    }

    /**
     * @param _currencyInstance intsance of the currency
     * @param _rate rate to aply
     */
    public void applyRate(final Instance _currencyInstance,
                          final BigDecimal _rate)
    {
        this.productPrice.setCurrentPrice(this.productPrice.getCurrentPrice()
                        .divide(_rate, 8, BigDecimal.ROUND_HALF_UP));
        this.productPrice.setCurrentCurrencyInstance(_currencyInstance);
        this.productNetUnitPrice = null;
        this.productCrossUnitPrice = null;
    }

    /**
     * @param _value value to be parsed to an BigDecimal
     * @return BigDecimal
     * @throws EFapsException on parse exception
     */
    public BigDecimal parse(final String _value)
        throws EFapsException
    {
        final BigDecimal ret;
        try {
            ret = (BigDecimal) NumberFormatter.get().getFormatter().parse(_value);
        } catch (final ParseException e) {
            throw new EFapsException(Calculator.class, "ParseException", e);
        }
        return ret;
    }

    /**
     * Getter method for the instance variable {@link #empty}.
     *
     * @return value of instance variable {@link #empty}
     */
    public boolean isEmpty()
    {
        return this.empty;
    }

    /**
     * Setter method for instance variable {@link #date}.
     *
     * @param _date value for instance variable {@link #date}
     */

    public void setDate(final DateTime _date)
    {
        this.date = _date;
    }

    /**
     * Getter method for the instance variable {@link #localDate}.
     *
     * @return value of instance variable {@link #localDate}
     */
    public DateTime getDate()
    {
        return this.date;
    }

    /**
     * Setter method for instance variable {@link #oid}.
     *
     * @param _oid value for instance variable {@link #oid}
     */
    public void setOid(final String _oid)
    {
        this.oid = _oid;
    }

    /**
     * Setter method for instance variable {@link #empty}.
     *
     * @param _empty value for instance variable {@link #empty}
     */
    public void setEmpty(final boolean _empty)
    {
        this.empty = _empty;
    }

    /**
     * Checks if is background.
     *
     * @return the background
     */
    public boolean isBackground()
    {
        return this.background;
    }

    /**
     * Sets the background.
     *
     * @param _background the new background
     */
    public void setBackground(final boolean _background)
    {
        this.background = _background;
    }

    /**
     * Price is net.
     *
     * @param _parameter the _parameter
     * @param _config the _config
     * @return true, if successful
     * @throws EFapsException on error
     */
    protected static boolean priceIsNet(final Parameter _parameter,
                                        final ICalculatorConfig _config)
        throws EFapsException
    {
        return new Calculator(_parameter, _config).priceIsNet(_parameter);
    }

    /**
     * Price is net.
     *
     * @param _parameter the _parameter
     * @param _config the _config
     * @return true, if successful
     * @throws EFapsException on error
     */
    protected static boolean isIncludeMinRetail(final Parameter _parameter,
                                                final ICalculatorConfig _config)
        throws EFapsException
    {
        return new Calculator(_parameter, _config).isIncludeMinRetail(_parameter);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _calcList List of calculator
     * @return crossTotal
     * @throws EFapsException on error
     */
    protected static BigDecimal getNetTotal(final Parameter _parameter,
                                            final List<Calculator> _calcList)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        for (final Calculator calc : _calcList) {
            ret = ret.add(calc.getNetPrice());
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _calcList List of calculator
     * @return crossTotal
     * @throws EFapsException on error
     */
    protected static BigDecimal getCrossTotal(final Parameter _parameter,
                                              final List<Calculator> _calcList)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        String config = "";
        if (!_calcList.isEmpty()) {
            config = _calcList.get(0).getConfig(_calcList.get(0).getDocKey(), Keys.CROSSTOTAL, "default");
        }
        switch (config) {
            case "NetTotalPlusTax":
                ret = Calculator.getNetTotal(_parameter, _calcList);
                for (final Calculator calc : _calcList) {
                    if (!calc.isWithoutTax()) {
                        for (final BigDecimal amount : calc.getTaxesAmounts().values()) {
                            ret = ret.add(amount);
                        }
                    }
                }
                break;
            default:
                for (final Calculator calc : _calcList) {
                    ret = ret.add(calc.getCrossPrice());
                }
                break;
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _calcList List of calculator
     * @return crossTotal
     * @throws EFapsException on error
     */
    protected static BigDecimal getBaseCrossTotal(final Parameter _parameter,
                                                  final List<Calculator> _calcList)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        for (final Calculator calc : _calcList) {
            ret = ret.add(calc.getProductCrossPrice().getBasePrice());
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _calcList List of calculator
     * @return crossTotal
     * @throws EFapsException on error
     */
    protected static BigDecimal getPerceptionTotal(final Parameter _parameter,
                                                   final List<Calculator> _calcList)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        for (final Calculator calc : _calcList) {
            ret = ret.add(calc.getPerception());
        }
        return ret;
    }
}
