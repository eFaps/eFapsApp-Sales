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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.CachedInstanceQuery;
import org.efaps.db.CachedMultiPrintQuery;
import org.efaps.db.CachedPrintQuery;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.RateInfo;
import org.efaps.esjp.products.Cost;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * Contains method to calculate the costs for products.
 *
 * @author The eFaps Team
 *
 */
@EFapsUUID("586c03e5-1e3e-41ec-852d-6bca23559f2d")
@EFapsApplication("eFapsApp-Sales")
public abstract class Costs_Base
    extends Cost
{

    /**
     * Eval acquisition cost.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _productInstance the product instance
     * @param _docInst the doc inst
     * @param _currenyInst the curreny inst
     * @return the big decimal
     * @throws EFapsException on error
     */
    protected BigDecimal evalAcquisitionCost(final Parameter _parameter,
                                             final Instance _productInstance,
                                             final Instance _docInst,
                                             final Instance _currenyInst)
        throws EFapsException
    {
        final List<Instance> reTickInsts = getRecievingTicketInsts(_parameter, _productInstance, _docInst);
        final List<CostBean> costs = new ArrayList<>();
        for (final Instance reTickInst : reTickInsts) {
            final List<CostBean> tmpCosts = eval4Costing(_parameter, reTickInst, _productInstance);
            if (tmpCosts.isEmpty()) {
                costs.addAll(eval4Invoice(_parameter, reTickInst, _productInstance));
            } else {
                costs.addAll(tmpCosts);
            }
        }
        return getCost(_parameter, costs, _currenyInst);
    }

    /**
     * Gets the cost.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _costs the costs
     * @param _currenyInst the curreny inst
     * @return the cost
     * @throws EFapsException on error
     */
    protected BigDecimal getCost(final Parameter _parameter,
                                 final List<CostBean> _costs,
                                 final Instance _currenyInst)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        // first priority have costing informations
        final List<CostBean> costings = _costs.stream().filter(new Predicate<CostBean>()
        {

            @Override
            public boolean test(final CostBean _bean)
            {
                return _bean.isCosting();
            }
        }).collect(Collectors.toList());
        Iterator<CostBean> iter;
        if (costings.isEmpty()) {
            iter = _costs.iterator();
        } else {
            iter = costings.iterator();
        }
        BigDecimal quantity = BigDecimal.ZERO;
        while (iter.hasNext()) {
            final CostBean bean = iter.next();
            final BigDecimal currentCost = getCost4Curreny(_parameter, bean, _currenyInst);
            if (quantity.compareTo(BigDecimal.ZERO) == 0) {
                ret = currentCost;
                quantity = bean.getQuantity();
            } else {
                ret = quantity.multiply(ret).add(bean.getQuantity().multiply(currentCost)).divide(quantity.add(bean
                                .getQuantity()), 8, RoundingMode.HALF_UP);
                quantity = quantity.add(bean.getQuantity());
            }
        }
        return ret;
    }

    /**
     * Gets the cost for curreny.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _costBean the cost bean
     * @param _currenyInst the curreny inst
     * @return the cost for curreny
     * @throws EFapsException on error
     */
    protected BigDecimal getCost4Curreny(final Parameter _parameter,
                                         final CostBean _costBean,
                                         final Instance _currenyInst)
        throws EFapsException
    {
        BigDecimal ret;
        if (_costBean.getRateCurInst().equals(_currenyInst)) {
            ret = _costBean.getRateNetUnitPrice();
        } else if (_costBean.getCurInst().equals(_currenyInst)) {
            ret = _costBean.getNetUnitPrice();
        } else {
            final RateInfo rateInfo = new Currency().evaluateRateInfo(_parameter, _costBean.getDate(), _currenyInst);
            final BigDecimal rate = RateInfo.getRate(_parameter, rateInfo, CISales.RecievingTicket.getType().getName());
            ret = _costBean.getNetUnitPrice().multiply(rate);
        }
        return ret;
    }

    /**
     * Eval for costing.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _reTickInst the re tick inst
     * @param _productInstance the product instance
     * @return the list< cost bean>
     * @throws EFapsException on error
     */
    protected List<CostBean> eval4Costing(final Parameter _parameter,
                                          final Instance _reTickInst,
                                          final Instance _productInstance)
        throws EFapsException
    {
        final List<CostBean> ret = new ArrayList<>();
        final QueryBuilder relAttrQueryBldr = new QueryBuilder(CISales.AcquisitionCosting2RecievingTicket);
        relAttrQueryBldr.addWhereAttrEqValue(CISales.AcquisitionCosting2RecievingTicket.ToLink, _reTickInst);

        final QueryBuilder posQueryBldr = new QueryBuilder(CISales.AcquisitionCostingPosition);
        posQueryBldr.addWhereAttrEqValue(CISales.AcquisitionCostingPosition.Product, _productInstance);
        posQueryBldr.addWhereAttrInQuery(CISales.AcquisitionCostingPosition.AcquisitionCostingLink, relAttrQueryBldr
                        .getAttributeQuery(CISales.AcquisitionCosting2RecievingTicket.FromLink));

        final CachedMultiPrintQuery multi = posQueryBldr.getCachedPrint4Request();
        final SelectBuilder selDate = SelectBuilder.get()
                        .linkto(CISales.AcquisitionCostingPosition.AcquisitionCostingLink)
                        .attribute(CISales.AcquisitionCosting.Date);
        final SelectBuilder selCurInst = SelectBuilder.get().linkto(CISales.AcquisitionCostingPosition.CurrencyId)
                        .instance();
        final SelectBuilder selRateCurInst = SelectBuilder.get().linkto(CISales.IncomingInvoicePosition.RateCurrencyId)
                        .instance();
        multi.addSelect(selDate, selCurInst, selRateCurInst);
        multi.addAttribute(CISales.IncomingInvoicePosition.NetUnitPrice,
                        CISales.IncomingInvoicePosition.RateNetUnitPrice, CISales.IncomingInvoicePosition.Quantity);
        multi.execute();
        while (multi.next()) {
            ret.add(new CostBean()
                            .setCosting(true)
                            .setDate(multi.getSelect(selDate))
                            .setCurInst(multi.getSelect(selCurInst))
                            .setRateCurInst(multi.getSelect(selRateCurInst))
                            .setQuantity(multi.getAttribute(CISales.IncomingInvoicePosition.Quantity))
                            .setNetUnitPrice(multi.getAttribute(CISales.IncomingInvoicePosition.NetUnitPrice))
                            .setRateNetUnitPrice(multi.getAttribute(CISales.IncomingInvoicePosition.RateNetUnitPrice)));
        }
        return ret;
    }

    /**
     * Eval for invoice.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _reTickInst the re tick inst
     * @param _productInstance the product instance
     * @return the list< cost bean>
     * @throws EFapsException on error
     */
    protected List<CostBean> eval4Invoice(final Parameter _parameter,
                                          final Instance _reTickInst,
                                          final Instance _productInstance)
        throws EFapsException
    {
        final List<CostBean> ret = new ArrayList<>();
        final QueryBuilder relAttrQueryBldr = new QueryBuilder(CISales.IncomingInvoice2RecievingTicket);
        relAttrQueryBldr.addWhereAttrEqValue(CISales.IncomingInvoice2RecievingTicket.ToLink, _reTickInst);

        final QueryBuilder posQueryBldr = new QueryBuilder(CISales.IncomingInvoicePosition);
        posQueryBldr.addWhereAttrEqValue(CISales.IncomingInvoicePosition.Product, _productInstance);
        posQueryBldr.addWhereAttrInQuery(CISales.IncomingInvoicePosition.IncomingInvoice, relAttrQueryBldr
                        .getAttributeQuery(CISales.IncomingInvoice2RecievingTicket.FromLink));
        final CachedMultiPrintQuery multi = posQueryBldr.getCachedPrint4Request();
        final SelectBuilder selDate = SelectBuilder.get().linkto(CISales.IncomingInvoicePosition.IncomingInvoice)
                        .attribute(CISales.IncomingInvoice.Date);
        final SelectBuilder selCurInst = SelectBuilder.get().linkto(CISales.IncomingInvoicePosition.CurrencyId)
                        .instance();
        final SelectBuilder selRateCurInst = SelectBuilder.get().linkto(CISales.IncomingInvoicePosition.RateCurrencyId)
                        .instance();
        multi.addSelect(selDate, selCurInst, selRateCurInst);
        multi.addAttribute(CISales.IncomingInvoicePosition.NetUnitPrice,
                        CISales.IncomingInvoicePosition.RateNetUnitPrice, CISales.IncomingInvoicePosition.Quantity);
        multi.execute();
        while (multi.next()) {
            ret.add(new CostBean()
                            .setDate(multi.getSelect(selDate))
                            .setCurInst(multi.getSelect(selCurInst))
                            .setRateCurInst(multi.getSelect(selRateCurInst))
                            .setQuantity(multi.getAttribute(CISales.IncomingInvoicePosition.Quantity))
                            .setNetUnitPrice(multi.getAttribute(CISales.IncomingInvoicePosition.NetUnitPrice))
                            .setRateNetUnitPrice(multi.getAttribute(CISales.IncomingInvoicePosition.RateNetUnitPrice)));
        }
        return ret;
    }

    /**
     * Gets the recieving ticket insts.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _productInstance the product instance
     * @param _docInst the doc inst
     * @return the recieving ticket insts
     * @throws EFapsException on error
     */
    protected List<Instance> getRecievingTicketInsts(final Parameter _parameter,
                                                     final Instance _productInstance,
                                                     final Instance _docInst)
        throws EFapsException
    {
        final List<Instance> ret = new ArrayList<>();
        // if the start doc is a recievingticket, verify it
        if (InstanceUtils.isType(_docInst, CISales.RecievingTicket)) {
            final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.IncomingInvoice);
            attrQueryBldr.addWhereAttrNotEqValue(CISales.IncomingInvoice.Status, Status.find(
                            CISales.IncomingInvoiceStatus.Replaced));

            final QueryBuilder queryBldr = new QueryBuilder(CISales.IncomingInvoice2RecievingTicket);
            queryBldr.addWhereAttrEqValue(CISales.IncomingInvoice2RecievingTicket.ToLink, _docInst);
            queryBldr.addWhereAttrInQuery(CISales.IncomingInvoice2RecievingTicket.FromLink, attrQueryBldr
                            .getAttributeQuery(CISales.IncomingInvoice.ID));
            final CachedInstanceQuery query = queryBldr.getCachedQuery4Request();
            query.executeWithoutAccessCheck();
            if (query.next()) {
                ret.add(_docInst);
            }
        }
        if (ret.isEmpty()) {
            final PrintQuery print = CachedPrintQuery.get4Request(_docInst);
            print.addAttribute(CIERP.DocumentAbstract.Date);
            print.executeWithoutAccessCheck();
            final DateTime docdate = print.getAttribute(CIERP.DocumentAbstract.Date);

            // must have a valid invoice or valid costing
            final QueryBuilder invAttrQueryBldr = new QueryBuilder(CISales.IncomingInvoice);
            invAttrQueryBldr.addWhereAttrNotEqValue(CISales.IncomingInvoice.Status, Status.find(
                            CISales.IncomingInvoiceStatus.Replaced));

            final QueryBuilder costAttrQueryBldr = new QueryBuilder(CISales.AcquisitionCosting);
            costAttrQueryBldr.addWhereAttrNotEqValue(CISales.AcquisitionCosting.Status, Status.find(
                            CISales.AcquisitionCostingStatus.Canceled));

            // must be related to an invoice
            final QueryBuilder relAttrQueryBldr = new QueryBuilder(CISales.AcquisitionCosting2RecievingTicket);
            relAttrQueryBldr.addType(CISales.IncomingInvoice2RecievingTicket);
            relAttrQueryBldr.setOr(true);
            relAttrQueryBldr.addWhereAttrInQuery(CISales.Document2DocumentAbstract.FromAbstractLink, invAttrQueryBldr
                            .getAttributeQuery(CISales.IncomingInvoice.ID));
            relAttrQueryBldr.addWhereAttrInQuery(CISales.Document2DocumentAbstract.FromAbstractLink, costAttrQueryBldr
                            .getAttributeQuery(CISales.AcquisitionCosting.ID));

            // must have a position with the given product
            final QueryBuilder posAttrQueryBldr = new QueryBuilder(CISales.RecievingTicketPosition);
            posAttrQueryBldr.addWhereAttrEqValue(CISales.RecievingTicketPosition.Product, _productInstance);

            // valid RecievingTicket with the same date or earlier, for the same
            // day (max 10 are evaluated)
            final QueryBuilder queryBldr = new QueryBuilder(CISales.RecievingTicket);
            queryBldr.addWhereAttrNotEqValue(CISales.RecievingTicket.Status, Status.find(
                            CISales.RecievingTicketStatus.Canceled));
            queryBldr.addWhereAttrLessValue(CISales.RecievingTicket.Date, docdate.withTimeAtStartOfDay().plusDays(1));
            queryBldr.addWhereAttrInQuery(CISales.RecievingTicket.ID, relAttrQueryBldr.getAttributeQuery(
                            CISales.Document2DocumentAbstract.ToAbstractLink));
            queryBldr.addWhereAttrInQuery(CISales.RecievingTicket.ID, posAttrQueryBldr.getAttributeQuery(
                            CISales.RecievingTicketPosition.RecievingTicket));
            queryBldr.addOrderByAttributeDesc(CISales.RecievingTicket.Date);
            queryBldr.setLimit(10);
            final CachedMultiPrintQuery multi = queryBldr.getCachedPrint4Request();
            multi.setEnforceSorted(true);
            multi.addAttribute(CISales.RecievingTicket.Date);
            multi.executeWithoutAccessCheck();
            DateTime current = null;
            while (multi.next()) {
                if (current == null) {
                    current = multi.<DateTime>getAttribute(CISales.RecievingTicket.Date).withTimeAtStartOfDay();
                    ret.add(multi.getCurrentInstance());
                } else {
                    final boolean invalid = current.isAfter(multi.<DateTime>getAttribute(CISales.RecievingTicket.Date)
                                    .withTimeAtStartOfDay());
                    if (invalid) {
                        break;
                    } else {
                        ret.add(multi.getCurrentInstance());
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Gets the acquisition cost.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _productInstance the product instance
     * @param _docInst the doc inst
     * @param _currenyInst the curreny inst
     * @return the acquisition cost
     * @throws EFapsException
     */
    protected static BigDecimal getAcquisitionCost(final Parameter _parameter,
                                                   final Instance _productInstance,
                                                   final Instance _docInst,
                                                   final Instance _currenyInst)
        throws EFapsException
    {
        return new Costs().evalAcquisitionCost(_parameter, _productInstance, _docInst, _currenyInst);
    }

    /**
     * CostBean.
     */
    public static class CostBean
    {

        /** The costing. */
        private boolean costing;

        /** The date. */
        private DateTime date;

        /** The cur inst. */
        private Instance curInst;

        /** The rate cur inst. */
        private Instance rateCurInst;

        /** The quantity. */
        private BigDecimal quantity;

        /** The net unit price. */
        private BigDecimal netUnitPrice;

        /** The rate net unit price. */
        private BigDecimal rateNetUnitPrice;

        /**
         * Getter method for the instance variable {@link #curInst}.
         *
         * @return value of instance variable {@link #curInst}
         */
        public Instance getCurInst()
        {
            return this.curInst;
        }

        /**
         * Setter method for instance variable {@link #curInst}.
         *
         * @param _curInst value for instance variable {@link #curInst}
         * @return the cost bean
         */
        public CostBean setCurInst(final Instance _curInst)
        {
            this.curInst = _curInst;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #rateCurInst}.
         *
         * @return value of instance variable {@link #rateCurInst}
         */
        public Instance getRateCurInst()
        {
            return this.rateCurInst;
        }

        /**
         * Setter method for instance variable {@link #rateCurInst}.
         *
         * @param _rateCurInst value for instance variable {@link #rateCurInst}
         * @return the cost bean
         */
        public CostBean setRateCurInst(final Instance _rateCurInst)
        {
            this.rateCurInst = _rateCurInst;
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
         * @return the cost bean
         */
        public CostBean setQuantity(final BigDecimal _quantity)
        {
            this.quantity = _quantity;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #netUnitPrice}.
         *
         * @return value of instance variable {@link #netUnitPrice}
         */
        public BigDecimal getNetUnitPrice()
        {
            return this.netUnitPrice;
        }

        /**
         * Setter method for instance variable {@link #netUnitPrice}.
         *
         * @param _netUnitPrice value for instance variable {@link #netUnitPrice}
         * @return the cost bean
         */
        public CostBean setNetUnitPrice(final BigDecimal _netUnitPrice)
        {
            this.netUnitPrice = _netUnitPrice;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #rateNetUnitPrice}.
         *
         * @return value of instance variable {@link #rateNetUnitPrice}
         */
        public BigDecimal getRateNetUnitPrice()
        {
            return this.rateNetUnitPrice;
        }

        /**
         * Setter method for instance variable {@link #rateNetUnitPrice}.
         *
         * @param _rateNetUnitPrice value for instance variable {@link #rateNetUnitPrice}
         */
        public CostBean setRateNetUnitPrice(final BigDecimal _rateNetUnitPrice)
        {
            this.rateNetUnitPrice = _rateNetUnitPrice;
            return this;
        }

        /**
         * Checks if is costing.
         *
         * @return true, if is costing
         */
        public boolean isCosting()
        {
            return this.costing;
        }

        /**
         * Sets the costing.
         *
         * @param _costing the costing
         * @return the cost bean
         */
        public CostBean setCosting(final boolean _costing)
        {
            this.costing = _costing;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #date}.
         *
         * @return value of instance variable {@link #date}
         */
        public DateTime getDate()
        {
            return this.date;
        }

        /**
         * Setter method for instance variable {@link #date}.
         *
         * @param _date value for instance variable {@link #date}
         * @return the cost bean
         */
        public CostBean setDate(final DateTime _date)
        {
            this.date = _date;
            return this;
        }
    }
}
