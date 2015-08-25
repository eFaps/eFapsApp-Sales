/*
 * Copyright 2003 - 2015 The eFaps Team
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

import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("2919ef7f-1aaa-428c-b58f-407ebd876ff7")
@EFapsApplication("eFapsApp-Sales")
public abstract class DocComparator_Base
{

    /**
     * Instance of the underlying document.
     */
    private Instance docInstance;

    private boolean init = false;

    private final Map<Instance, Position> inst2pos = new HashMap<Instance, Position>();

    protected boolean isSumDoc()
    {
        return getDocInstance().getType().isKindOf(CISales.DocumentSumAbstract.getType());
    }

    protected void initialize()
        throws EFapsException
    {
        if (!this.init) {
            this.init = true;
            QueryBuilder queryBldr;
            if (isSumDoc()) {
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
                multi.addAttribute(CISales.PositionSumAbstract.NetPrice);
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
     */
    public void setDocInstance(final Instance _docInstance)
    {
        this.docInstance = _docInstance;
    }

    /**
     * Getter method for the instance variable {@link #inst2pos}.
     *
     * @return value of instance variable {@link #inst2pos}
     */
    public Map<Instance, Position> getInst2pos()
        throws EFapsException
    {
        initialize();
        return this.inst2pos;
    }

    /**
     * @param _docComp
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
     * @param _docComp
     */
    public void substractNet(final DocComparator _docComp)
        throws EFapsException
    {
        initialize();
        for (final Entry<Instance, Position> entry : _docComp.getInst2pos().entrySet()) {
            if (getInst2pos().containsKey(entry.getKey())) {
                getInst2pos().get(entry.getKey()).substractNet(entry.getValue());
            }
        }
    }

    /**
     * @return
     */
    public boolean netIsZero()
        throws EFapsException
    {
        BigDecimal net = BigDecimal.ZERO;
        for (final Position pos : getInst2pos().values()) {
            net = net.add(pos.getNetPrice());
        }
        return net.abs().compareTo(getNetDeviation()) < 0;
    }

    /**
     * @return
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

    protected BigDecimal getQuantityDeviation()
    {
        return new BigDecimal("0.01");
    }

    protected BigDecimal getNetDeviation()
    {
        return new BigDecimal("0.01");
    }

    public static class Position
    {

        private Instance posInstance;

        private BigDecimal quantity = BigDecimal.ZERO;

        private BigDecimal netPrice = BigDecimal.ZERO;

        public Position(final Instance _posInstance)
        {
            this.posInstance = _posInstance;
        }

        /**
         * @param _value
         */
        public void substractNet(final Position _position)
        {
            setNetPrice(getNetPrice().subtract(_position.getNetPrice()));
        }

        /**
         * @param _value
         */
        public void substractQuantity(final Position _position)
        {
            setQuantity(getQuantity().subtract(_position.getQuantity()));
        }

        /**
         * @param _attribute
         */
        public void add2NetPrice(final BigDecimal _netPrice)
        {
            setNetPrice(getNetPrice().add(_netPrice));
        }

        /**
         * @param _attribute
         * @param _attribute2
         */
        public void add2Quantity(final BigDecimal _quantity,
                                 final Long _uoMId)
        {
            add2Quantity(_quantity, Dimension.getUoM(_uoMId));

        }

        /**
         * @param _attribute
         * @param _attribute2
         */
        public void add2Quantity(final BigDecimal _quantity,
                                 final UoM _uoM)
        {
            setQuantity(getQuantity().add(_quantity.setScale(8, BigDecimal.ROUND_HALF_UP)
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
         */
        public void setPosInstance(final Instance _posInstance)
        {
            this.posInstance = _posInstance;
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
         */
        public void setQuantity(final BigDecimal _quantity)
        {
            this.quantity = _quantity;
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
         */
        public void setNetPrice(final BigDecimal _net)
        {
            this.netPrice = _net;
        }

    }

}
