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
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.sales.PriceUtil_Base.ProductPrice;
import org.efaps.esjp.sales.Tax_Base.TaxRate;
import org.efaps.esjp.sales.document.AbstractDocument_Base;
import org.efaps.ui.wicket.util.DateUtil;
import org.efaps.util.EFapsException;
import org.joda.time.LocalDate;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("a9ce907c-0e76-44f9-8dbe-2fdfe2893ae0")
@EFapsRevision("$Rev$")
public abstract class Calculator_Base
    implements Serializable
{
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
    private final long taxcatId;

    /**
     * Standard formatter used in this class.
     */
    private final DecimalFormat formater;

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
    private final boolean empty;

    /**
     * Date this Calculator is based on. Used for e.g. date of Taxes.
     */
    private LocalDate localDate;

    private int longDecimal;

    /**
     * Constructor used to instantiate an empty calculator.
     * @throws EFapsException on error
     */

    public Calculator_Base()
        throws EFapsException
    {
        this.taxcatId = 0;
        this.formater = Calculator_Base.getFormatInstance();
        this.empty = true;
        setLocalDate(new LocalDate(Context.getThreadContext().getChronology()));
    }

    /**
     * @param _parameter            Parameter  parameter as passed from the eFaps API
     * @param _calc                 calculator
     * @param _oid                  oid of the product
     * @param _quantity             quantity
     * @param _unitPrice            unit price
     * @param _discount             discount
     * @param _priceFromDB          must the price set from DB
     * @param _includeMinRetail     must the minimum retail price be included
     * @throws EFapsException on error
     */
    //CHECKSTYLE:OFF
    public Calculator_Base(final Parameter _parameter,
                           final Calculator _calc,
                           final String _oid,
                           final String _quantity,
                           final String _unitPrice,
                           final String _discount,
                           final boolean _priceFromDB,
                           final ICalculatorConfig _calculatorUse)
        throws EFapsException
    //CHECKSTYLE:ON
    {
        this.formater = Calculator_Base.getFormatInstance();
        this.empty = false;
        if (_calculatorUse != null) {
            this.longDecimal = _calculatorUse.isLongDecimal(_parameter);
        } else {
            this.longDecimal = 2;
        }

        final String dateStr = _parameter == null ? null : _parameter.getParameterValue(getDateFieldName(_parameter));
        if (dateStr != null && dateStr != null) {
            setLocalDate(DateUtil.getDateFromParameter(dateStr).toLocalDate());
        } else {
            setLocalDate(new LocalDate());
        }
        if (_calc != null && _oid.equals(_calc.getOid())) {
            this.taxcatId = _calc.getTaxcatId();
            this.oid = _calc.getOid();
            this.productPrice = _calc.getProductPrice();
            this.minProductPrice = _calc.getMinProductPrice();

            // check if unitprice is set from UI
            if (!_priceFromDB && _unitPrice != null && _unitPrice.length() > 0) {
                setPriceFromUI(_parameter, _unitPrice);
            } else {
                if (_priceFromDB) {
                    final PriceUtil priceutil = new PriceUtil();
                    this.productPrice = priceutil.getPrice(_parameter, this.oid, getPriceListUUID());
                    if (_calculatorUse != null && _calculatorUse.isIncludeMinRetail(_parameter)) {
                        this.minProductPrice = priceutil.getPrice(_parameter, this.oid, getMinPriceListUUID());
                    }
                }
            }
        } else {
            this.oid = _oid;
            if (this.oid != null && this.oid.length() > 0) {
                // check if unitprice is set from UI
                if (!_priceFromDB && _unitPrice != null && _unitPrice.length() > 0) {
                    setPriceFromUI(_parameter, _unitPrice);
                } else {
                    final PriceUtil priceutil = new PriceUtil();
                    this.productPrice = priceutil.getPrice(_parameter, this.oid, getPriceListUUID());
                }
                final PrintQuery print = new PrintQuery(this.oid);
                print.addAttribute(CISales.Products_ProductAbstract.TaxCategory);
                print.execute();
                this.taxcatId = print.<Long> getAttribute(CISales.Products_ProductAbstract.TaxCategory);
                if (_calculatorUse != null && _calculatorUse.isIncludeMinRetail(_parameter)) {
                    this.minProductPrice =  new PriceUtil().getPrice(_parameter, this.oid, getMinPriceListUUID());
                }
            } else {
                this.taxcatId = 0;
            }
        }
        setDiscount(_discount);

        if (getMinProductPrice() != null) {
            final BigDecimal discountPrice = getProductPrice().getBasePrice().subtract(getProductPrice().getBasePrice()
                            .divide(new BigDecimal(100)).multiply(getDiscount()));
            if (discountPrice.compareTo(getMinProductPrice().getBasePrice()) < 0) {
                setDiscount(BigDecimal.ZERO);
            }
        }
        setQuantity(_quantity);
    }

    /**
     *
     * Get the name of the field that contains the date.
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
                                  final String _unitPrice)
        throws EFapsException
    {
        int decDigCant = 2;
        if (isLongDecimal() != decDigCant) {
            decDigCant = isLongDecimal();
        }
        final BigDecimal unitPrice = parse(_unitPrice).setScale(decDigCant, BigDecimal.ROUND_HALF_UP);

        final Instance currInst = (Instance) Context.getThreadContext().getSessionAttribute(
                        AbstractDocument_Base.CURRENCY_INSTANCE_KEY);
        // Sales-Configuration
        final Instance baseInst = SystemConfiguration.get(UUID
                        .fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f"))
                        .getLink("CurrencyBase");
        if (this.productPrice == null) {
            this.productPrice = getNewPrice();
        }
        this.productPrice.setCurrentPrice(unitPrice);
        if (currInst.equals(baseInst)) {
            this.productPrice.setBasePrice(unitPrice);
        } else {
            final PriceUtil priceutil = new PriceUtil();
            final BigDecimal rate = priceutil.getExchangeRate(_parameter, currInst);
            final BigDecimal newprice = unitPrice.divide(rate, 8, BigDecimal.ROUND_HALF_UP);
            this.productPrice.setBasePrice(newprice);
            this.productPrice.setOrigPrice(newprice);
        }
    }

    /**
     * @return the UUID of the type used for the pricelist
     */
    protected UUID getPriceListUUID()
    {
        return CIProducts.ProductPricelistRetail.uuid;
    }

    /**
     * @return the UUID of the type used for the minimum pricelist
     */
    protected UUID getMinPriceListUUID()
    {
        return  CIProducts.ProductPricelistMinRetail.uuid;
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
     * @throws EFapsException
     */
    public void setNetUnitPrice(final BigDecimal _netUnitPrice)
        throws EFapsException
    {
        if (this.productNetUnitPrice == null) {
            this.productNetUnitPrice = getNewPrice();
        }
        this.productNetUnitPrice.setCurrentPrice(_netUnitPrice);
        this.productNetUnitPrice.setOrigPrice(_netUnitPrice);
        this.productNetUnitPrice.setBasePrice(_netUnitPrice.multiply(this.productPrice.getBaseRate()));
        if (this.taxcatId > 0) {
            final TaxRate rate = getTaxRate();
            final BigDecimal denom = new BigDecimal(rate.getDenominator());
            final BigDecimal num = new BigDecimal(rate.getNumerator());
            final BigDecimal factor = num.divide(denom, 16, BigDecimal.ROUND_HALF_UP);
            if (factor.compareTo(BigDecimal.ONE) == 0) {
                this.productCrossUnitPrice = this.productNetUnitPrice;
            } else {
                if (this.productCrossUnitPrice == null) {
                    this.productCrossUnitPrice = getNewPrice();
                }
                final BigDecimal cross = _netUnitPrice.multiply(BigDecimal.ONE.add(factor));
                this.productCrossUnitPrice.setCurrentPrice(cross);
                this.productCrossUnitPrice.setOrigPrice(cross);
                this.productCrossUnitPrice.setBasePrice(cross.multiply(this.productPrice.getBaseRate()));
            }
        }
    }

    /**
     * @return get the new Price
     */
    protected ProductPrice getNewPrice()
        throws EFapsException
    {
        // Sales-Configuration
        final Instance baseInst = SystemConfiguration.get(UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f"))
                        .getLink("CurrencyBase");
        final ProductPrice ret = new PriceUtil().new ProductPrice();
        ret.setBaseRate(this.productPrice == null ? BigDecimal.ONE : this.productPrice.getBaseRate());
        ret.setCurrentCurrencyInstance(this.productPrice == null
                        ? baseInst : this.productPrice.getCurrentCurrencyInstance());
        ret.setOrigCurrencyInstance(this.productPrice == null
                        ? baseInst : this.productPrice.getOrigCurrencyInstance());
        return ret;
    }

    /**
     * @return string representation of the net unit price.
     * @throws EFapsException
     */
    public String getNetUnitPriceStr()
        throws EFapsException
    {
        return this.formater.format(getNetUnitPrice());
    }

    /**
     * Get the net unit price formated with the given formater.
     *
     * @param _formater formater to use
     * @return formated string representation of the net unit price
     * @throws EFapsException
     */
    public String getNetUnitPriceFmtStr(final Format _formater)
        throws EFapsException
    {
        return _formater.format(getNetUnitPrice());
    }

    /**
     * @return the net unit price
     * @throws EFapsException
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
     * @throws EFapsException
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

        if (this.taxcatId > 0) {
            final TaxRate rate = getTaxRate();
            final BigDecimal denom = new BigDecimal(rate.getDenominator());
            final BigDecimal num = new BigDecimal(rate.getNumerator());
            final BigDecimal factor = BigDecimal.ONE.add(num.divide(denom, 16, BigDecimal.ROUND_HALF_UP));
            if (factor.compareTo(BigDecimal.ONE) == 0) {
                this.productNetUnitPrice = this.productCrossUnitPrice;
            } else {
                if (this.productNetUnitPrice == null) {
                    this.productNetUnitPrice = getNewPrice();
                }
                final BigDecimal net = _crossUnitPrice.divide(factor, 16, BigDecimal.ROUND_HALF_UP);
                this.productNetUnitPrice.setCurrentPrice(net);
                this.productNetUnitPrice.setOrigPrice(net);
                this.productNetUnitPrice.setBasePrice(net.multiply(this.productNetUnitPrice.getBaseRate()));
            }
        }
    }

    /**
     * @return string representation of the cross unit price.
     * @throws EFapsException
     */
    public String getCrossUnitPriceStr()
        throws EFapsException
    {
        return this.formater.format(getCrossUnitPrice());
    }

    /**
     * Get the cross unit price formated with the given formater.
     *
     * @param _formater formater to use
     * @return formated string representation of the net unit price
     * @throws EFapsException
     */
    public String getCrossUnitPriceFmtStr(final Format _formater)
        throws EFapsException
    {
        return _formater.format(getCrossUnitPrice());
    }

    /**
     * Get the cross unit price.
     *
     * @return cross unit price
     * @throws EFapsException
     */
    public BigDecimal getCrossUnitPrice()
        throws EFapsException
    {
        BigDecimal ret;
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
     * @return string representation of the discount.
     */
    public String getDiscountStr()
    {
        return this.formater.format(this.discount);
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
     * @throws EFapsException
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
     * @throws EFapsException
     */
    public ProductPrice getProductDiscountNetUnitPrice()
        throws EFapsException
    {
        final ProductPrice ret = getNewPrice();
        final ProductPrice unit = getProductNetUnitPrice();
        int decDigCant = 2;
        if (isLongDecimal() != decDigCant) {
            decDigCant = isLongDecimal();
        }
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
     * @param _formater formater to use
     * @return formated string representation of the net unit price
     * @throws EFapsException
     */
    public String getDiscountNetUnitPriceFmtStr(final Format _formater)
        throws EFapsException
    {
        return _formater.format(getDiscountNetUnitPrice());
    }

    /**
     * @return string representation of the discount net unit price.
     * @throws EFapsException
     */
    public String getDiscountNetUnitPriceStr()
        throws EFapsException
    {
        return getFormater().format(getDiscountNetUnitPrice());
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
     */
    public void setUnitPrice(final BigDecimal _unitprice)
    {
        this.productPrice.setCurrentPrice(_unitprice);
        this.productPrice.setOrigPrice(_unitprice);
        this.productPrice.setBasePrice(_unitprice.multiply(this.productPrice.getBaseRate()));
        this.productNetUnitPrice = null;
        this.productCrossUnitPrice = null;
    }

    /**
     * Getter method for the instance variable {@link #productPrice}.
     *
     * @return value of instance variable {@link #productPrice}
     * @throws EFapsException
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
     */
    public String getQuantityStr()
    {
        return getFormater().format(getQuantity());
    }

    public String getQuantityFmtStr(final Format _formater)
    {
        return _formater.format(getQuantity());
    }

    /**
     * @return the net price
     * @throws EFapsException
     */
    public BigDecimal getNetPrice()
        throws EFapsException
    {
        return getProductNetPrice().getCurrentPrice();
    }

    /**
     * discount net unit price * quantity.
     *
     * @return net price for the product
     * @throws EFapsException
     */
    public ProductPrice getProductNetPrice()
        throws EFapsException
    {
        final ProductPrice ret = getNewPrice();
        final ProductPrice unit = getProductDiscountNetUnitPrice();
        ret.setBasePrice(unit.getBasePrice().multiply(this.quantity));
        ret.setOrigPrice(unit.getOrigPrice().multiply(this.quantity));
        ret.setCurrentPrice(unit.getCurrentPrice().multiply(this.quantity));
        return ret;
    }

    /**
     * Get the net price formated with the given formater.
     *
     * @param _formater formater to use
     * @return formated string representation of the net unit price
     * @throws EFapsException
     */
    public String getNetPriceFmtStr(final Format _formater)
        throws EFapsException
    {
        return _formater.format(getNetPrice());
    }

    /**
     * @return string representation of the cross price.
     * @throws EFapsException
     */
    public String getCrossPriceStr()
        throws EFapsException
    {
        return this.formater.format(getCrossPrice());
    }

    /**
     * @return the cross price
     * @throws EFapsException
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
     * @throws EFapsException
     */
    public ProductPrice getProductCrossPrice()
        throws EFapsException
    {
        final ProductPrice ret = getNewPrice();
        final ProductPrice unit = getProductDiscountCrossUnitPrice();
        ret.setBasePrice(unit.getBasePrice().multiply(this.quantity));
        ret.setOrigPrice(unit.getOrigPrice().multiply(this.quantity));
        ret.setCurrentPrice(unit.getCurrentPrice().multiply(this.quantity));
        return ret;
    }

    /**
     * calculate the product price with discount, but calculate the tax after the discount
     * when the factor is different of 1, because the discount price has to be rounded before.
     *
     * @return @return discount price for the product, depending the tax factor.
     * @throws EFapsException
     */
    public ProductPrice getProductDiscountCrossUnitPrice()
        throws EFapsException
    {
        int decDigCant = 2;
        if (isLongDecimal() != decDigCant) {
            decDigCant = isLongDecimal();
        }
        ProductPrice ret = getNewPrice();
        final ProductPrice unit = getProductNetUnitPrice();
        ret.setBasePrice(unit.getBasePrice().subtract(unit.getBasePrice().divide(new BigDecimal(100))
                        .multiply(getDiscount())).setScale(decDigCant, BigDecimal.ROUND_HALF_UP));
        ret.setOrigPrice(unit.getOrigPrice().subtract(unit.getOrigPrice().divide(new BigDecimal(100))
                        .multiply(getDiscount())).setScale(decDigCant, BigDecimal.ROUND_HALF_UP));
        ret.setCurrentPrice(unit.getCurrentPrice().subtract(unit.getCurrentPrice().divide(new BigDecimal(100))
                        .multiply(getDiscount())).setScale(decDigCant, BigDecimal.ROUND_HALF_UP));
        ret = evalProductCrossUnitPrice(ret);
        return ret;

    }

    /**
     * Getter method for the instance variable {@link #minProductPrice}.
     *
     * @return value of instance variable {@link #minProductPrice}
     * @throws EFapsException
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
     * @param _minProductPrice value for instance variable {@link #minProductPrice}
     */

    public void setMinProductPrice(final ProductPrice _minProductPrice)
    {
        this.minProductPrice = _minProductPrice;
    }


    /**
     * Get the cross price formated with the given formater.
     *
     * @param _formater formater to use
     * @return formated string representation of the net unit price
     * @throws EFapsException
     */
    public String getCrossPriceFmtStr(final Format _formater)
        throws EFapsException
    {
        return _formater.format(getCrossPrice());
    }

    /**
     * @return string representation of the net price.
     * @throws EFapsException
     */
    public String getNetPriceStr()
        throws EFapsException
    {
        return this.formater.format(getNetPrice());
    }

    /**
     * @return unit cross price
     * @throws EFapsException
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
     * @throws EFapsException
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
            // get SystemConfiguration "Sales-Configuration"
            final SystemConfiguration sysconf = SystemConfiguration.get(UUID
                            .fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f"));
            if (sysconf.getAttributeValueAsBoolean("ProductPriceIsNetPrice") && this.taxcatId > 0) {
                final TaxRate rate = getTaxRate();
                final BigDecimal denom = new BigDecimal(rate.getDenominator());
                final BigDecimal num = new BigDecimal(rate.getNumerator());
                final BigDecimal factor = num.divide(denom, 16, BigDecimal.ROUND_HALF_UP);
                if (factor.compareTo(BigDecimal.ONE) == 0) {
                    ret.setCurrentPrice(_price.getCurrentPrice() == null
                                    ? BigDecimal.ZERO
                                    : _price.getCurrentPrice());
                    ret.setBasePrice(_price.getBasePrice() == null
                                    ? BigDecimal.ZERO
                                    : _price.getBasePrice());
                    ret.setOrigPrice(_price.getOrigPrice() == null
                                    ? BigDecimal.ZERO
                                    : _price.getOrigPrice());
                } else {
                    ret.setCurrentPrice(_price.getCurrentPrice() == null
                                    ? BigDecimal.ZERO
                                    : _price.getCurrentPrice().multiply(BigDecimal.ONE.add(factor)));
                    ret.setBasePrice(_price.getBasePrice() == null
                                    ? BigDecimal.ZERO
                                    : _price.getBasePrice().multiply(BigDecimal.ONE.add(factor)));
                    ret.setOrigPrice(_price.getOrigPrice() == null
                                    ? BigDecimal.ZERO
                                    : _price.getOrigPrice().multiply(BigDecimal.ONE.add(factor)));
                }
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
     * @throws EFapsException
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
            // get SystemConfiguration "Sales-Configuration"
            final SystemConfiguration sysconf = SystemConfiguration.get(UUID
                            .fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f"));
            if (sysconf.getAttributeValueAsBoolean("ProductPriceIsNetPrice") && this.taxcatId > 0) {
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
                final TaxRate rate = getTaxRate();
                final BigDecimal denom = new BigDecimal(rate.getDenominator());
                final BigDecimal num = new BigDecimal(rate.getNumerator());
                final BigDecimal factor = num.divide(denom, 16, BigDecimal.ROUND_HALF_UP);
                if (factor.compareTo(BigDecimal.ONE) == 0) {
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
                    ret.setCurrentPrice(this.productPrice.getCurrentPrice() == null
                                    ? BigDecimal.ZERO
                                    : this.productPrice.getCurrentPrice().divide(BigDecimal.ONE.add(factor)));
                    ret.setBasePrice(this.productPrice.getBasePrice() == null
                                    ? BigDecimal.ZERO
                                    : this.productPrice.getBasePrice().divide(BigDecimal.ONE.add(factor)));
                    ret.setOrigPrice(this.productPrice.getOrigPrice() == null
                                    ? BigDecimal.ZERO
                                    : this.productPrice.getOrigPrice().divide(BigDecimal.ONE.add(factor)));
                }
            }
        }
        return ret;
    }

    /**
     * @return net unit price
     * @throws EFapsException
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
     * Get the if of the current tax.
     *
     * @return tax id
     */
    public Long getTaxId()
    {
        return getTaxRate().getId();
    }

    /**
     * Get the rate.
     *
     * @return Rate
     */
    public TaxRate getTaxRate()
    {
        final Tax taxcat = Tax_Base.get(this.taxcatId);
        return taxcat == null ? TaxRate.getZeroTax() : taxcat.getTaxRate(getLocalDate());
    }

    /**
     * Getter method for the instance variable {@link #taxcatId}.
     *
     * @return value of instance variable {@link #taxcatId}
     */
    public long getTaxcatId()
    {
        return this.taxcatId;
    }

    /**
     * Getter method for instance variable {@link #formater}.
     *
     * @return value of instance variable {@link #formater}
     */
    protected DecimalFormat getFormater()
    {
        return this.formater;
    }

    /**
     * @param _currencyInstance intsance of the currency
     * @param _rate rate to aply
     */
    public void applyRate(final Instance _currencyInstance,
                          final BigDecimal _rate)
    {
        this.productPrice.setCurrentPrice(this.productPrice.getCurrentPrice().multiply(_rate));
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
            ret = (BigDecimal) this.formater.parse(_value);
        } catch (final ParseException e) {
            throw new EFapsException(Calculator.class, "ParseException", e);
        }
        return ret;
    }

    /**
     * @param _value value to be formated
     * @return value formated with {@link #formater}
     */
    public String format(final Object _value)
    {
        return this.formater.format(_value);
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
     * Method to get a <code>DecimalFormat</code> instance with the
     * <code>Locale</code> from the <code>Context</code>.
     *
     * @return DecimalFormat
     * @throws EFapsException on erro
     */
    public static DecimalFormat getFormatInstance()
        throws EFapsException
    {
        final DecimalFormat ret = (DecimalFormat) NumberFormat.getInstance(Context.getThreadContext().getLocale());
        ret.setParseBigDecimal(true);
        return ret;
    }

    /**
     * Setter method for instance variable {@link #localDate}.
     *
     * @param _localDate value for instance variable {@link #localDate}
     */

    public void setLocalDate(final LocalDate _localDate)
    {
        this.localDate = _localDate;
    }

    /**
     * Getter method for the instance variable {@link #localDate}.
     *
     * @return value of instance variable {@link #localDate}
     */
    public LocalDate getLocalDate()
    {
        return this.localDate;
    }

    /**
     * @return true if the long decimal is activated
     */
    public int isLongDecimal()
    {
        return this.longDecimal;
    }
}
