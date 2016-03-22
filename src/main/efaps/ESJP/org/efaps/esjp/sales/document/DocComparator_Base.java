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

package org.efaps.esjp.sales.document;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.lang3.BooleanUtils;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;

/**
 * Compares to documents to evaluate if they have the same information,
 * e.g. is the quantity or the amount of the positions are equal. It is
 * used to change the status of one document if it is related to another one
 * like an Invoice to a DeliveryNote.
 *
 * @author The eFaps Team
 */
@EFapsUUID("2919ef7f-1aaa-428c-b58f-407ebd876ff7")
@EFapsApplication("eFapsApp-Sales")
public abstract class DocComparator_Base
    extends AbstractCommon
{

    /**
     * Instance of the underlying document.
     */
    private Instance docInstance;

    /** The rate currency instance. */
    private Instance rateCurrencyInstance;

    /** The init. */
    private boolean init = false;

    /** The inst2pos. */
    private final Map<Instance, Position> inst2pos = new HashMap<Instance, Position>();

    /**
     * Checks if is sum doc.
     *
     * @return true, if is sum doc
     */
    protected boolean isSumDoc()
    {
        return getDocInstance().getType().isKindOf(CISales.DocumentSumAbstract.getType());
    }

    /**
     * Initialize.
     *
     * @throws EFapsException the e faps exception
     */
    protected void initialize()
        throws EFapsException
    {
        if (!this.init) {
            this.init = true;
            final QueryBuilder queryBldr;
            if (isSumDoc()) {
                final PrintQuery print = new PrintQuery(getDocInstance());
                final SelectBuilder selCurInst = SelectBuilder.get()
                                .linkto(CISales.DocumentSumAbstract.RateCurrencyId).instance();
                print.addSelect(selCurInst);
                print.executeWithoutAccessCheck();
                setRateCurrencyInstance(print.<Instance>getSelect(selCurInst));

                queryBldr = new QueryBuilder(CISales.PositionSumAbstract);
            } else {
                queryBldr = new QueryBuilder(CISales.PositionAbstract);
            }
            queryBldr.addWhereAttrEqValue(CISales.PositionAbstract.DocumentAbstractLink, getDocInstance());
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selPosInst = SelectBuilder.get().linkto(CISales.PositionAbstract.Product).instance();
            multi.addSelect(selPosInst);
            multi.addAttribute(CISales.PositionAbstract.UoM, CISales.PositionAbstract.Quantity);
            if (isSumDoc()) {
                multi.addAttribute(CISales.PositionSumAbstract.NetPrice,
                                CISales.PositionSumAbstract.RateNetPrice);
            }
            multi.executeWithoutAccessCheck();
            while (multi.next()) {
                final Instance posInst = multi.getSelect(selPosInst);
                if (!this.inst2pos.containsKey(posInst)) {
                    this.inst2pos.put(posInst, new Position(posInst));
                }
                final Position pos = this.inst2pos.get(posInst);
                pos.add2Quantity(multi.<BigDecimal>getAttribute(CISales.PositionAbstract.Quantity),
                                multi.<Long>getAttribute(CISales.PositionAbstract.UoM));
                if (isSumDoc()) {
                    pos.add2NetPrice(multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.NetPrice));
                    pos.add2RateNetPrice(multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.RateNetPrice));
                }
            }
        }
    }

    /**
     * Getter method for the instance variable {@link #docInstance}.
     *
     * @return value of instance variable {@link #docInstance}
     */
    public Instance getDocInstance()
    {
        return this.docInstance;
    }

    /**
     * Setter method for instance variable {@link #docInstance}.
     *
     * @param _docInstance value for instance variable {@link #docInstance}
     * @return the doc comparator
     */
    public DocComparator setDocInstance(final Instance _docInstance)
    {
        this.docInstance = _docInstance;
        return (DocComparator) this;
    }

    /**
     * Getter method for the instance variable {@link #rateCurrencyInstance}.
     *
     * @return value of instance variable {@link #rateCurrencyInstance}
     */
    public Instance getRateCurrencyInstance()
    {
        return this.rateCurrencyInstance;
    }

    /**
     * Setter method for instance variable {@link #rateCurrencyInstance}.
     *
     * @param _rateCurrencyInstance value for instance variable {@link #rateCurrencyInstance}
     * @return the doc comparator
     */
    public DocComparator setRateCurrencyInstance(final Instance _rateCurrencyInstance)
    {
        this.rateCurrencyInstance = _rateCurrencyInstance;
        return (DocComparator) this;
    }

    /**
     * Getter method for the instance variable {@link #inst2pos}.
     *
     * @return value of instance variable {@link #inst2pos}
     * @throws EFapsException the e faps exception
     */
    public Map<Instance, Position> getInst2pos()
        throws EFapsException
    {
        initialize();
        return this.inst2pos;
    }

    /**
     * Substract quantity.
     *
     * @param _docComp the _doc comp
     * @throws EFapsException the e faps exception
     */
    public void substractQuantity(final DocComparator _docComp)
        throws EFapsException
    {
        initialize();
        for (final Entry<Instance, Position> entry : _docComp.getInst2pos().entrySet()) {
            if (getInst2pos().containsKey(entry.getKey())) {
                getInst2pos().get(entry.getKey()).substractQuantity(entry.getValue());
            }
        }
    }

    /**
     * Substract net.
     *
     * @param _docComp the _doc comp
     * @throws EFapsException the e faps exception
     */
    public void substractNet(final DocComparator _docComp)
        throws EFapsException
    {
        initialize();
        for (final Entry<Instance, Position> entry : _docComp.getInst2pos().entrySet()) {
            if (getInst2pos().containsKey(entry.getKey())) {
                getInst2pos().get(entry.getKey()).substractNet(entry.getValue());
                if (getRateCurrencyInstance().equals(_docComp.getRateCurrencyInstance())) {
                    getInst2pos().get(entry.getKey()).substractRateNet(entry.getValue());
                }
            }
        }
    }

    /**
     * Net is zero.
     *
     * @return true, if successful
     * @throws EFapsException the e faps exception
     */
    public boolean netIsZero()
        throws EFapsException
    {
        final Properties properties = Sales.COMPARATORCONFIG.get();
        boolean evalrate = false;
        if (properties.containsKey(getDocInstance().getType().getName() + ".EvaluateRateCurrency")) {
            evalrate = BooleanUtils.toBoolean(
                            properties.getProperty(getDocInstance().getType().getName() + ".EvaluateRateCurrency"));
        } else if (properties.containsKey("Default.EvaluateRateCurrency")) {
            evalrate = BooleanUtils.toBoolean(properties.getProperty("Default.EvaluateRateCurrency"));
        }

        BigDecimal net = BigDecimal.ZERO;
        BigDecimal rateNet = BigDecimal.ZERO;

        for (final Position pos : getInst2pos().values()) {
            net = net.add(pos.getNetPrice());
            rateNet = rateNet.add(pos.getRateNetPrice());
        }
        // if the evaluation of Rate Currency is permitted check if one of them is ok
        return evalrate ? net.abs().compareTo(getNetDeviation()) < 0 || rateNet.abs().compareTo(getNetDeviation()) < 0
                        : net.abs().compareTo(getNetDeviation()) < 0;
    }

    /**
     * Quantity is zero.
     *
     * @return true, if successful
     * @throws EFapsException the e faps exception
     */
    public boolean quantityIsZero()
        throws EFapsException
    {
        BigDecimal quantity = BigDecimal.ZERO;
        for (final Position pos : getInst2pos().values()) {
            quantity = quantity.add(pos.getQuantity());
        }
        return quantity.abs().compareTo(getQuantityDeviation()) < 0;
    }

    /**
     * Gets the quantity deviation.
     *
     * @return the quantity deviation
     * @throws EFapsException the e faps exception
     */
    protected BigDecimal getQuantityDeviation()
        throws EFapsException
    {
        final Properties properties = Sales.COMPARATORCONFIG.get();
        final BigDecimal ret;
        if (properties.containsKey(getDocInstance().getType().getName() + ".Deviation4Quantity")) {
            ret = new BigDecimal(properties.getProperty(getDocInstance().getType().getName() + ".Deviation4Quantity"));
        } else if (properties.containsKey("Default.Deviation4Quantity")) {
            ret = new BigDecimal(properties.getProperty("Default.Deviation4Quantity"));
        } else {
            ret = BigDecimal.ZERO;
        }
        return ret;
    }

    /**
     * Gets the net deviation.
     *
     * @return the net deviation
     * @throws EFapsException the e faps exception
     */
    protected BigDecimal getNetDeviation()
        throws EFapsException
    {
        final Properties properties = Sales.COMPARATORCONFIG.get();
        final BigDecimal ret;
        if (properties.containsKey(getDocInstance().getType().getName() + ".Deviation4Net")) {
            ret = new BigDecimal(properties.getProperty(getDocInstance().getType().getName() + ".Deviation4Net"));
        } else if (properties.containsKey("Default.Deviation4Net")) {
            ret = new BigDecimal(properties.getProperty("Default.Deviation4Net"));
        } else {
            ret = BigDecimal.ZERO;
        }
        return ret;
    }

    /**
     * The Class Position.
     */
    public static class Position
    {

        /** The pos instance. */
        private Instance posInstance;

        /** The quantity. */
        private BigDecimal quantity = BigDecimal.ZERO;

        /** The net price. */
        private BigDecimal netPrice = BigDecimal.ZERO;

        /** The rate net price. */
        private BigDecimal rateNetPrice = BigDecimal.ZERO;

        /**
         * Instantiates a new position.
         *
         * @param _posInstance the _pos instance
         */
        public Position(final Instance _posInstance)
        {
            this.posInstance = _posInstance;
        }

        /**
         * Substract net.
         *
         * @param _position the _position
         * @return the position
         */
        public Position substractNet(final Position _position)
        {
            return setNetPrice(getNetPrice().subtract(_position.getNetPrice()));
        }

        /**
         * Substract rate net.
         *
         * @param _position the _position
         * @return the position
         */
        public Position substractRateNet(final Position _position)
        {
            return setRateNetPrice(getRateNetPrice().subtract(_position.getRateNetPrice()));
        }

        /**
         * Substract quantity.
         *
         * @param _position the _position
         * @return the position
         */
        public Position substractQuantity(final Position _position)
        {
            return setQuantity(getQuantity().subtract(_position.getQuantity()));
        }

        /**
         * Add2 net price.
         *
         * @param _netPrice the _net price
         * @return the position
         */
        public Position add2NetPrice(final BigDecimal _netPrice)
        {
            setNetPrice(getNetPrice().add(_netPrice));
            return this;
        }

        /**
         * Add2 rate net price.
         *
         * @param _rateNetPrice the _rate net price
         * @return the position
         */
        public Position add2RateNetPrice(final BigDecimal _rateNetPrice)
        {
            setNetPrice(getNetPrice().add(_rateNetPrice));
            return this;
        }

        /**
         * Add2 quantity.
         *
         * @param _quantity the _quantity
         * @param _uoMId the _uo m id
         * @return the position
         */
        public Position add2Quantity(final BigDecimal _quantity,
                                     final Long _uoMId)
        {
            return add2Quantity(_quantity, Dimension.getUoM(_uoMId));
        }

        /**
         * Add2 quantity.
         *
         * @param _quantity the _quantity
         * @param _uoM the _uo m
         * @return the position
         */
        public Position add2Quantity(final BigDecimal _quantity,
                                     final UoM _uoM)
        {
            return setQuantity(getQuantity().add(_quantity.setScale(8, BigDecimal.ROUND_HALF_UP)
                            .multiply(new BigDecimal(_uoM.getNumerator()).divide(
                                            new BigDecimal(_uoM.getDenominator()), BigDecimal.ROUND_HALF_UP))));
        }

        /**
         * Getter method for the instance variable {@link #posInstance}.
         *
         * @return value of instance variable {@link #posInstance}
         */
        public Instance getPosInstance()
        {
            return this.posInstance;
        }

        /**
         * Setter method for instance variable {@link #posInstance}.
         *
         * @param _posInstance value for instance variable {@link #posInstance}
         * @return the position
         */
        public Position setPosInstance(final Instance _posInstance)
        {
            this.posInstance = _posInstance;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #quantity}.
         *
         * @return value of instance variable {@link #quantity}
         */
        public BigDecimal getQuantity()
        {
            return this.quantity;
        }

        /**
         * Setter method for instance variable {@link #quantity}.
         *
         * @param _quantity value for instance variable {@link #quantity}
         * @return the position
         */
        public Position setQuantity(final BigDecimal _quantity)
        {
            this.quantity = _quantity;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #net}.
         *
         * @return value of instance variable {@link #net}
         */
        public BigDecimal getNetPrice()
        {
            return this.netPrice;
        }

        /**
         * Setter method for instance variable {@link #net}.
         *
         * @param _net value for instance variable {@link #net}
         * @return the position
         */
        public Position setNetPrice(final BigDecimal _net)
        {
            this.netPrice = _net;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #rateNetPrice}.
         *
         * @return value of instance variable {@link #rateNetPrice}
         */
        public BigDecimal getRateNetPrice()
        {
            return this.rateNetPrice;
        }

        /**
         * Setter method for instance variable {@link #rateNetPrice}.
         *
         * @param _rateNetPrice value for instance variable {@link #rateNetPrice}
         * @return the position
         */
        public Position setRateNetPrice(final BigDecimal _rateNetPrice)
        {
            this.rateNetPrice = _rateNetPrice;
            return this;
        }
    }
}
