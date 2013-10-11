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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * Contains method to calculate the costs for products.
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("586c03e5-1e3e-41ec-852d-6bca23559f2d")
@EFapsRevision("$Rev$")
public abstract class Costs_Base
{

    /**
     * Executed after the insertion of a Incoming Invoice. The method will only
     * be executed if the SystemConfiguration for Sales contains the attribute
     * <code>ActivateCalculateCosts</code> and is set to <code>true</code>.
     *
     * Calculation:<br/>
     * newly
     *
     * @param _parameter Parameter as a passed from the eFaps API
     * @param _docInst Instance of the Document containing the costing
     *            information
     * @return new Return
     * @throws EFapsException on error
     */
    public Return updateCosts(final Parameter _parameter,
                              final Instance _docInst)
        throws EFapsException
    {
        // Sales-Configuration
        final SystemConfiguration config = SystemConfiguration.get(
                            UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f"));
        if (config != null && config.getAttributeValueAsBoolean("ActivateCalculateCosts")) {
            if (CISales.IncomingInvoice.getType().equals(_docInst.getType())) {
                final PrintQuery print = new PrintQuery(_docInst);
                print.addAttribute(CISales.IncomingInvoice.Date);
                print.execute();
                final DateTime date = print.<DateTime>getAttribute(CISales.IncomingInvoice.Date);

                final QueryBuilder queryBldr = new QueryBuilder(CISales.IncomingInvoicePosition);
                queryBldr.addWhereAttrEqValue(CISales.IncomingInvoicePosition.IncomingInvoice, _docInst.getId());
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CISales.IncomingInvoicePosition.NetPrice,
                                   CISales.IncomingInvoicePosition.Quantity,
                                   CISales.IncomingInvoicePosition.UoM);
                final SelectBuilder sel = new SelectBuilder().linkto(CISales.IncomingInvoicePosition.Product)
                                .oid();
                final SelectBuilder currSel = new SelectBuilder().linkto(CISales.IncomingInvoicePosition.CurrencyId)
                                .oid();
                multi.addSelect(sel, currSel);
                multi.execute();

                final Map<Instance, Position> prods = new HashMap<Instance, Position>();
                final Map<Instance, Position> extraCost = new HashMap<Instance, Position>();
                BigDecimal netTotal = BigDecimal.ZERO;
                BigDecimal extra = BigDecimal.ZERO;
                while (multi.next()) {
                    BigDecimal quantity = multi.<BigDecimal>getAttribute(CISales.IncomingInvoicePosition.Quantity);
                    final Long uoMId = multi.<Long>getAttribute(CISales.IncomingInvoicePosition.UoM);
                    final UoM uoM = Dimension.getUoM(uoMId);
                    BigDecimal netprice = multi.<BigDecimal>getAttribute(CISales.IncomingInvoicePosition.NetPrice);
                    netprice = netprice.multiply(new BigDecimal(uoM.getNumerator())
                                        .divide(new BigDecimal(uoM.getDenominator()))).setScale(2,BigDecimal.ROUND_HALF_UP);
                    final Instance curInst = Instance.get(multi.<String>getSelect(currSel));
                    final Instance prodInst = Instance.get(multi.<String>getSelect(sel));
                    final Map<Instance, Position> map;
                    if (prodInst.getType().isKindOf(CIProducts.StockProductAbstract.getType())) {
                        map = prods;
                        netTotal = netTotal.add(netprice);
                    } else {
                        map = extraCost;
                        extra = extra.add(netprice);
                    }
                    Position pos;
                    if (map.containsKey(prodInst)) {
                        pos = map.get(prodInst);
                        if (!pos.curInst.equals(curInst)) {
                            final BigDecimal[] rates = new PriceUtil().getRates(date, curInst, pos.curInst);
                            netprice = netprice.divide(rates[2], BigDecimal.ROUND_HALF_UP);
                        }
                        pos.setNetprice(pos.getNetprice().add(netprice));
                        pos.setQuantity(pos.getQuantity().add(quantity));
                    } else {
                        pos = new Position(prodInst, curInst, netprice, quantity);
                    }
                    map.put(prodInst, pos);
                }

                for (final Position pos : prods.values()) {
                    final QueryBuilder costBldr = new QueryBuilder(CIProducts.ProductCost);
                    costBldr.addWhereAttrEqValue(CIProducts.ProductCost.ProductLink, pos.getProdInst().getId());
                    costBldr.addWhereAttrGreaterValue(CIProducts.ProductCost.ValidUntil, date.minusMinutes(1));
                    costBldr.addWhereAttrLessValue(CIProducts.ProductCost.ValidFrom, date.plusMinutes(1));
                    final MultiPrintQuery costMulti = costBldr.getPrint();
                    costMulti.addAttribute(CIProducts.ProductCost.Price);
                    final SelectBuilder costCurrSel = new SelectBuilder().linkto(CIProducts.ProductCost.CurrencyLink)
                                    .oid();
                    costMulti.addSelect(costCurrSel);
                    costMulti.execute();
                    BigDecimal total = BigDecimal.ZERO;
                    BigDecimal totalQuantity = BigDecimal.ZERO;
                    while (costMulti.next()) {
                        BigDecimal stockCost = costMulti.<BigDecimal>getAttribute(CIProducts.ProductCost.Price);
                        final Instance stockCurrInst = Instance.get(costMulti.<String>getSelect(costCurrSel));
                        if (!stockCurrInst.equals(pos.curInst)) {
                            final BigDecimal[] rates = new PriceUtil().getRates(date, stockCurrInst, pos.getCurInst());
                            stockCost = stockCost.divide(rates[2], BigDecimal.ROUND_HALF_UP);
                        }
                        final QueryBuilder storeBldr = new QueryBuilder(CIProducts.Warehouse);
                        final AttributeQuery storequery = storeBldr.getAttributeQuery(CIProducts.Warehouse.ID);

                        final QueryBuilder inventoryBldr = new QueryBuilder(CIProducts.Inventory);
                        inventoryBldr.addWhereAttrEqValue(CIProducts.Inventory.Product, pos.getProdInst().getId());
                        inventoryBldr.addWhereAttrInQuery(CIProducts.Inventory.Storage, storequery);
                        final MultiPrintQuery inventoryMulti = inventoryBldr.getPrint();
                        inventoryMulti.addAttribute(CIProducts.Inventory.Quantity, CIProducts.Inventory.Reserved);
                        inventoryMulti.execute();
                        BigDecimal stockQuantity = BigDecimal.ZERO;
                        while (inventoryMulti.next()) {
                            stockQuantity = inventoryMulti.<BigDecimal>getAttribute(CIProducts.Inventory.Quantity);
                            final BigDecimal stockReserved = inventoryMulti.
                                                    <BigDecimal>getAttribute(CIProducts.Inventory.Reserved);
                            stockQuantity = stockQuantity.add(stockReserved);
                        }
                        total = total.add(stockCost.multiply(stockQuantity));
                        totalQuantity = totalQuantity.add(stockQuantity);
                    }
                    if (extra.compareTo(BigDecimal.ZERO) > 0) {
                        total = total.add(pos.getNetprice().divide(netTotal, BigDecimal.ROUND_HALF_UP).multiply(extra));
                    }
                    final BigDecimal newCost;
                    if (total.compareTo(BigDecimal.ZERO) > 0) {
                        newCost = total.add(pos.getNetprice()).divide(totalQuantity.add(pos.quantity),
                                        BigDecimal.ROUND_HALF_UP);
                    } else {
                        newCost = pos.getNetprice().divide(pos.quantity, BigDecimal.ROUND_HALF_UP);
                    }

                    final Insert insert = new Insert(CIProducts.ProductCost);
                    insert.add(CIProducts.ProductCost.ProductLink, pos.getProdInst().getId());
                    insert.add(CIProducts.ProductCost.Price, newCost);
                    insert.add(CIProducts.ProductCost.ValidFrom, date);
                    insert.add(CIProducts.ProductCost.ValidUntil, date.plusYears(10));
                    insert.add(CIProducts.ProductCost.CurrencyLink, pos.getCurInst().getId());
                    insert.execute();
                }
            }
        }
        return new Return();
    }

    /**
     * Represent a position in a Document.
     */
    protected class Position
    {

        /**
         * Product Instance.
         */
        private final Instance prodInst;


        /**
         * Currency Instance.
         */
        private final Instance curInst;

        /**
         * Netprice.
         */
        private BigDecimal netprice;

        /**
         * Quantity.
         */
        private BigDecimal quantity;

        /**
         * @param _prodInst Product Instance
         * @param _curInst Currency Instance
         * @param _netprice Netprice
         * @param _quantity Quantity
         */
        protected Position(final Instance _prodInst,
                           final Instance _curInst,
                           final BigDecimal _netprice,
                           final BigDecimal _quantity)
        {
            this.prodInst = _prodInst;
            this.curInst = _curInst;
            this.netprice = _netprice;
            this.quantity = _quantity;
        }


        /**
         * Getter method for the instance variable {@link #netprice}.
         *
         * @return value of instance variable {@link #netprice}
         */
        public BigDecimal getNetprice()
        {
            return this.netprice;
        }


        /**
         * Setter method for instance variable {@link #netprice}.
         *
         * @param _netprice value for instance variable {@link #netprice}
         */

        public void setNetprice(final BigDecimal _netprice)
        {
            this.netprice = _netprice;
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
         * Getter method for the instance variable {@link #prodInst}.
         *
         * @return value of instance variable {@link #prodInst}
         */
        public Instance getProdInst()
        {
            return this.prodInst;
        }


        /**
         * Getter method for the instance variable {@link #curInst}.
         *
         * @return value of instance variable {@link #curInst}
         */
        public Instance getCurInst()
        {
            return this.curInst;
        }

    }
}
