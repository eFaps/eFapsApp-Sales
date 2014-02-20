/*
 * Copyright 2003 - 2014 The eFaps Team
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("65041308-73a6-47de-bd1d-3dacc37dbc6c")
@EFapsRevision("$Rev$")
public abstract class Costing_Base
{

    public Return execute(final Parameter _parameter)
        throws EFapsException
    {

        // check for new transactions and add the costing for them
        final SelectBuilder selDocInst = new SelectBuilder()
                        .linkto(CIProducts.TransactionInOutAbstract.Document).instance();
        final SelectBuilder selProdInst = new SelectBuilder()
                        .linkto(CIProducts.TransactionInOutAbstract.Product).instance();

        final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.Costing);
        final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CIProducts.Costing.TransactionAbstractLink);

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.TransactionInOutAbstract);
        queryBldr.addWhereAttrNotInQuery(CIProducts.TransactionInOutAbstract.ID, attrQuery);
        queryBldr.addOrderByAttributeAsc(CIProducts.TransactionInOutAbstract.Date);
        queryBldr.addOrderByAttributeAsc(CIProducts.TransactionInOutAbstract.Position);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addSelect(selDocInst, selProdInst);
        multi.setEnforceSorted(true);
        multi.executeWithoutAccessCheck();

        final Map<Instance, TransCosting> prod2Costing = new HashMap<Instance, TransCosting>();
        while (multi.next()) {
            final TransCosting transCost = new TransCosting();
            transCost.setTransactionInstance(multi.getCurrentInstance());
            transCost.setDocInstance(multi.<Instance>getSelect(selDocInst));
            transCost.setProductInstance(multi.<Instance>getSelect(selProdInst));
            transCost.setCostingQuantity(BigDecimal.ZERO);
            transCost.insertCosting();

            if (!prod2Costing.containsKey(transCost.getProductInstance())) {
                prod2Costing.put(transCost.getProductInstance(), transCost);
            }
        }

        final Set<Instance> updateCost = new HashSet<Instance>();

        for (final Entry<Instance, TransCosting> entry : prod2Costing.entrySet()) {
            final Instance penultimate = getPenultimate4Costing(entry.getValue().getCostingInstance());
            if (penultimate != null) {
                updateCost.add(penultimate);
            }
        }

        final QueryBuilder costQueryBldr = new QueryBuilder(CIProducts.Costing);
        costQueryBldr.addWhereAttrEqValue(CIProducts.Costing.UpToDate, false);
        final InstanceQuery costQuery = costQueryBldr.getQuery();
        costQuery.executeWithoutAccessCheck();
        while (costQuery.next()) {
            final Instance penultimate = getPenultimate4Costing(costQuery.getCurrentValue());
            if (penultimate != null) {
                updateCost.add(penultimate);
            }
        }

        for (final Instance inst : updateCost) {
            updateCosting(inst);
        }
        return new Return();
    }

    /**
     * @param _costingInstance start instance (the instance with the last
     *            correct value)
     * @throws EFapsException on error
     */
    /**
     * @param _costingInstance
     * @throws EFapsException
     */
    protected void updateCosting(final Instance _costingInstance)
        throws EFapsException
    {
        final List<TransCosting> tcList = new ArrayList<TransCosting>();

        final TransCosting transCosting = new TransCosting();
        transCosting.setCostingInstance(_costingInstance);

        tcList.add(transCosting);

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.TransactionInOutAbstract);
        queryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Product, transCosting.getProductInstance());
        queryBldr.addWhereAttrGreaterValue(CIProducts.TransactionInOutAbstract.Date,
                        transCosting.getDate().withTimeAtStartOfDay().minusSeconds(1));
        queryBldr.addOrderByAttributeAsc(CIProducts.TransactionInOutAbstract.Date);
        queryBldr.addOrderByAttributeAsc(CIProducts.TransactionInOutAbstract.Position);
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selProdInst = SelectBuilder.get().linkto(CIProducts.TransactionInOutAbstract.Product)
                        .instance();
        final SelectBuilder selCosting = SelectBuilder.get().linkfrom(CIProducts.Costing,
                        CIProducts.Costing.TransactionAbstractLink);
        final SelectBuilder selCostingInst = new SelectBuilder(selCosting).instance();
        final SelectBuilder selCostingQuantity = new SelectBuilder(selCosting).attribute(CIProducts.Costing.Quantity);
        final SelectBuilder selCostingCost = new SelectBuilder(selCosting).attribute(CIProducts.Costing.Cost);
        final SelectBuilder selCostingResult = new SelectBuilder(selCosting).attribute(CIProducts.Costing.Result);
        final SelectBuilder selCostingUTD = new SelectBuilder(selCosting).attribute(CIProducts.Costing.UpToDate);
        multi.addSelect(selProdInst, selCostingInst, selCostingQuantity, selCostingCost, selCostingResult,
                        selCostingUTD);
        multi.addAttribute(CIProducts.TransactionInOutAbstract.Date,
                        CIProducts.TransactionInOutAbstract.Quantity);
        multi.executeWithoutAccessCheck();
        boolean start = false;
        while (multi.next()) {
            if (start) {
                final TransCosting transCostingTmp = new TransCosting();
                tcList.add(transCostingTmp);
                transCostingTmp.setTransactionInstance(multi.getCurrentInstance());
                transCostingTmp.setDate(multi.<DateTime>getAttribute(CIProducts.TransactionInOutAbstract.Date));
                transCostingTmp.setTransactionQuantity(
                                multi.<BigDecimal>getAttribute(CIProducts.TransactionInOutAbstract.Quantity));
                transCostingTmp.setProductInstance(multi.<Instance>getSelect(selProdInst));
                transCostingTmp.setCostingInstance(multi.<Instance>getSelect(selCostingInst));
                transCostingTmp.setCost(multi.<BigDecimal>getSelect(selCostingCost));
                transCostingTmp.setCostingQuantity(multi.<BigDecimal>getSelect(selCostingQuantity));
                transCostingTmp.setResult(multi.<BigDecimal>getSelect(selCostingResult));
                transCostingTmp.setUpToDate(multi.<Boolean>getSelect(selCostingUTD));
            }
            // in case that there were other the same day but before
            if (multi.getCurrentInstance().equals(transCosting.getTransactionInstance())) {
                start = true;
            }
        }

        final Iterator<TransCosting> iter = tcList.iterator();
        TransCosting prev = null;
        boolean forceCostFromDoc = false;
        while (iter.hasNext()) {
            final TransCosting current = iter.next();
            if (prev != null) {
                boolean update = false;
                if (!current.isUpToDate() || forceCostFromDoc) {
                    final BigDecimal cost = current.getCostFromDocument();
                    if (current.getCost().compareTo(cost) != 0) {
                        current.setCost(cost);
                        update = true;
                        // if the current cost was updated, the following must be update also
                        forceCostFromDoc = true;
                    }
                }
                //check if the costing has a cost assigned, if not inherit from previous
                if (current.getCost().compareTo(BigDecimal.ZERO) == 0) {
                    current.setCost(prev.getCost());
                    update = true;
                }

                final BigDecimal newCostQuantity = prev.getQuantity();
                final BigDecimal result = prev.getResult().multiply(newCostQuantity)
                                .add(current.getCost().multiply(current.getTransactionQuantity()))
                                .setScale(12)
                                .divide(newCostQuantity.add(current.getTransactionQuantity()), BigDecimal.ROUND_UP);

                if (result.compareTo(current.getResult()) != 0) {
                    current.setResult(result);
                    update = true;
                }

                if (newCostQuantity.compareTo(current.getCostingQuantity()) != 0) {
                    current.setCostingQuantity(newCostQuantity);
                    update = true;
                }
                if (update) {
                    current.updateCosting();
                }
            } else {
                // if the current was marked for update (happens only if it also was the first ever)
                if (!current.isUpToDate()) {
                    final BigDecimal cost = current.getCostFromDocument();
                    if (current.getCost().compareTo(cost) != 0) {
                        current.setCost(cost);
                        current.setResult(cost);
                        current.updateCosting();
                        // if the first has its cost from doc changed, the rest must be checked also
                        forceCostFromDoc = true;
                    }
                }
            }
            prev = current;
        }
    }

    protected Instance getPenultimate4Costing(final Instance _instance)
        throws EFapsException
    {
        Instance ret = null;
        Instance transRet = null;

        final SelectBuilder selTransInst = new SelectBuilder()
                        .linkto(CIProducts.Costing.TransactionAbstractLink).instance();

        final SelectBuilder selProdInst = new SelectBuilder()
                        .linkto(CIProducts.Costing.TransactionAbstractLink)
                        .attribute(CIProducts.TransactionInOutAbstract.Product).instance();

        final SelectBuilder selDate = new SelectBuilder()
                        .linkto(CIProducts.Costing.TransactionAbstractLink)
                        .attribute(CIProducts.TransactionInOutAbstract.Date);

        final PrintQuery print = new PrintQuery(_instance);
        print.addSelect(selTransInst, selProdInst, selDate);
        print.executeWithoutAccessCheck();

        final Instance transInst = print.<Instance>getSelect(selTransInst);

        final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.Costing);
        attrQueryBldr.addWhereAttrEqValue(CIProducts.Costing.UpToDate, false);
        final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CIProducts.Costing.TransactionAbstractLink);

        // first check if for the same date exists one
        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.TransactionInOutAbstract);
        queryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Product,
                        print.<Instance>getSelect(selProdInst));
        queryBldr.addWhereAttrNotInQuery(CIProducts.TransactionInOutAbstract.ID, attrQuery);
        queryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Date, print.<DateTime>getSelect(selDate));
        queryBldr.addOrderByAttributeAsc(CIProducts.TransactionInOutAbstract.Position);
        final InstanceQuery query = queryBldr.getQuery();
        query.executeWithoutAccessCheck();
        Instance prev = null;
        while (query.next()) {
            if (query.getCurrentValue().equals(transInst)) {
                if (prev != null) {
                    transRet = prev;
                }
            } else {
                prev = query.getCurrentValue();
            }
        }
        // if not found yet check on all dates before
        if (prev == null) {
            final QueryBuilder queryBldr2 = new QueryBuilder(CIProducts.TransactionInOutAbstract);
            queryBldr2.addWhereAttrNotInQuery(CIProducts.TransactionInOutAbstract.ID, attrQuery);
            queryBldr2.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Product,
                            print.<Instance>getSelect(selProdInst));
            queryBldr2.addWhereAttrLessValue(CIProducts.TransactionInOutAbstract.Date,
                            print.<DateTime>getSelect(selDate));
            queryBldr2.addOrderByAttributeDesc(CIProducts.TransactionInOutAbstract.Date);
            queryBldr2.addOrderByAttributeDesc(CIProducts.TransactionInOutAbstract.Position);
            final InstanceQuery query2 = queryBldr2.getQuery();
            query2.setLimit(1);
            query2.executeWithoutAccessCheck();
            while (query2.next()) {
                transRet = query2.getCurrentValue();
            }
        }
        if (transRet == null) {
            ret = _instance;
        } else {
            final QueryBuilder queryBldrRes = new QueryBuilder(CIProducts.Costing);
            queryBldrRes.addWhereAttrEqValue(CIProducts.Costing.TransactionAbstractLink, transRet);
            final InstanceQuery queryRes = queryBldrRes.getQuery();
            queryRes.executeWithoutAccessCheck();
            if (queryRes.next()) {
                ret = queryRes.getCurrentValue();
            }
        }
        return ret;
    }

    public static class TransCosting
    {

        private Instance costingInstance;

        private Instance transactionInstance;

        private Instance docInstance;

        private Instance productInstance;

        private DateTime date;

        private BigDecimal transactionQuantity;

        private BigDecimal costingQuantity;

        private BigDecimal cost;

        private BigDecimal result;

        private Boolean upToDate;

        protected void initTransaction()
            throws EFapsException
        {
            final SelectBuilder selTransInst = new SelectBuilder()
                            .linkto(CIProducts.Costing.TransactionAbstractLink).instance();
            final SelectBuilder selProdInst = new SelectBuilder()
                            .linkto(CIProducts.Costing.TransactionAbstractLink)
                            .linkto(CIProducts.TransactionInOutAbstract.Product).instance();
            final SelectBuilder selDocInst = new SelectBuilder()
                            .linkto(CIProducts.Costing.TransactionAbstractLink)
                            .linkto(CIProducts.TransactionInOutAbstract.Document).instance();
            final SelectBuilder selDate = new SelectBuilder()
                            .linkto(CIProducts.Costing.TransactionAbstractLink)
                            .attribute(CIProducts.TransactionInOutAbstract.Date);
            final SelectBuilder selQuantity = new SelectBuilder()
                            .linkto(CIProducts.Costing.TransactionAbstractLink)
                            .attribute(CIProducts.TransactionInOutAbstract.Quantity);

            final PrintQuery print = new PrintQuery(getCostingInstance());
            print.addSelect(selTransInst, selProdInst, selDate, selQuantity, selDocInst);
            print.executeWithoutAccessCheck();
            this.productInstance = print.<Instance>getSelect(selProdInst);
            this.docInstance = print.<Instance>getSelect(selDocInst);
            this.date = print.<DateTime>getSelect(selDate);
            this.transactionInstance = print.<Instance>getSelect(selTransInst);
            this.transactionQuantity = print.<BigDecimal>getSelect(selQuantity);
        }

        protected void initCosting()
            throws EFapsException
        {
            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.Costing);
            queryBldr.addWhereAttrEqValue(CIProducts.Costing.TransactionAbstractLink, getTransactionInstance());
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CIProducts.Costing.Cost, CIProducts.Costing.Quantity, CIProducts.Costing.Result,
                            CIProducts.Costing.UpToDate);
            multi.executeWithoutAccessCheck();
            multi.next();
            this.upToDate = multi.<Boolean>getAttribute(CIProducts.Costing.UpToDate);
            this.cost = multi.<BigDecimal>getAttribute(CIProducts.Costing.Cost);
            this.costingQuantity = multi.<BigDecimal>getAttribute(CIProducts.Costing.Quantity);
            this.result = multi.<BigDecimal>getAttribute(CIProducts.Costing.Result);
            this.costingInstance = multi.getCurrentInstance();
        }

        /**
         *
         */
        public void updateCosting()
            throws EFapsException
        {
            final Update update = new Update(getCostingInstance());
            update.add(CIProducts.Costing.Cost, getCost());
            update.add(CIProducts.Costing.Quantity, getCostingQuantity());
            update.add(CIProducts.Costing.Result, getResult());
            update.add(CIProducts.Costing.UpToDate, true);
            update.executeWithoutAccessCheck();
            setUpToDate(true);
        }

        public void insertCosting()
            throws EFapsException
        {
            final BigDecimal costTmp = getCostFromDocument();

            final Insert insert = new Insert(CIProducts.Costing);
            insert.add(CIProducts.Costing.Quantity, getCostingQuantity());
            insert.add(CIProducts.Costing.TransactionAbstractLink, getTransactionInstance());
            insert.add(CIProducts.Costing.Cost, costTmp);
            insert.add(CIProducts.Costing.Result, costTmp);
            insert.add(CIProducts.Costing.UpToDate, true);
            insert.executeWithoutTrigger();

            setCost(costTmp);
            setResult(costTmp);
            setCostingInstance(insert.getInstance());
            setUpToDate(true);
        }


        /**
         * @param _costingInstance
         */
        public void setCostingInstance(final Instance _costingInstance)
        {
            this.costingInstance = _costingInstance;
        }

        /**
         * Getter method for the instance variable {@link #transactionInstance}.
         *
         * @return value of instance variable {@link #transactionInstance}
         */
        public Instance getTransactionInstance()
            throws EFapsException
        {
            if (this.transactionInstance == null) {
                initTransaction();
            }
            return this.transactionInstance;
        }

        /**
         * Setter method for instance variable {@link #transactionInstance}.
         *
         * @param _transactionInstance value for instance variable
         *            {@link #transactionInstance}
         */
        public void setTransactionInstance(final Instance _transactionInstance)
        {
            this.transactionInstance = _transactionInstance;
        }

        /**
         * Getter method for the instance variable {@link #costingInstance}.
         *
         * @return value of instance variable {@link #costingInstance}
         */
        public Instance getCostingInstance() throws EFapsException
        {
            if (this.costingInstance == null) {
                initCosting();
            }
            return this.costingInstance;
        }

        /**
         * Getter method for the instance variable {@link #productInstance}.
         *
         * @return value of instance variable {@link #productInstance}
         */
        public Instance getProductInstance()
            throws EFapsException
        {
            if (this.productInstance == null) {
                initTransaction();
            }
            return this.productInstance;
        }

        /**
         * Setter method for instance variable {@link #productInstance}.
         *
         * @param _productInstance value for instance variable
         *            {@link #productInstance}
         */
        public void setProductInstance(final Instance _productInstance)
        {
            this.productInstance = _productInstance;
        }

        /**
         * Getter method for the instance variable {@link #date}.
         *
         * @return value of instance variable {@link #date}
         */
        public DateTime getDate()
            throws EFapsException
        {
            if (this.productInstance == null) {
                initTransaction();
            }
            return this.date;
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
         * Getter method for the instance variable {@link #transactionQuantity}.
         *
         * @return value of instance variable {@link #transactionQuantity}
         */
        public BigDecimal getTransactionQuantity()
            throws EFapsException
        {
            if (this.transactionInstance == null) {
                initTransaction();
            }
            BigDecimal ret;
            if (this.transactionInstance.getType().isKindOf(CIProducts.TransactionOutbound.getType())) {
                ret = this.transactionQuantity.negate();
            } else {
                ret = this.transactionQuantity;
            }
            return ret;
        }

        /**
         * Setter method for instance variable {@link #transactionQuantity}.
         *
         * @param _transactionQuantity value for instance variable
         *            {@link #transactionQuantity}
         */
        public void setTransactionQuantity(final BigDecimal _transactionQuantity)
        {
            this.transactionQuantity = _transactionQuantity;
        }


        /**
         * Getter method for the instance variable {@link #costingQuantity}.
         *
         * @return value of instance variable {@link #costingQuantity}
         */
        public BigDecimal getCostingQuantity() throws EFapsException
        {
            if (this.costingQuantity == null) {
                initCosting();
            }
            return this.costingQuantity;
        }


        public BigDecimal getQuantity() throws EFapsException
        {
            return getCostingQuantity().add(getTransactionQuantity());
        }

        /**
         * Setter method for instance variable {@link #costingQuantity}.
         *
         * @param _costingQuantity value for instance variable {@link #costingQuantity}
         */
        public void setCostingQuantity(final BigDecimal _costingQuantity)
        {
            this.costingQuantity = _costingQuantity;
        }


        /**
         * Getter method for the instance variable {@link #cost}.
         *
         * @return value of instance variable {@link #cost}
         */
        public BigDecimal getCost() throws EFapsException
        {
            if (this.cost == null) {
                initCosting();
            }
            return this.cost;
        }

        public BigDecimal getCostFromDocument()
            throws EFapsException
        {
            BigDecimal ret = BigDecimal.ZERO;
            final Instance docInstTmp = getDocInstance();
            if (docInstTmp != null && docInstTmp.isValid()) {
                if (CISales.RecievingTicket.getType().equals(docInstTmp.getType())) {
                    final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Document2DerivativeDocument);
                    attrQueryBldr.addWhereAttrEqValue(CISales.Document2DerivativeDocument.From, getDocInstance());
                    final AttributeQuery attrQuery = attrQueryBldr
                                    .getAttributeQuery(CISales.Document2DerivativeDocument.To);
                    final QueryBuilder queryBldr = new QueryBuilder(CISales.IncomingInvoicePosition);
                    queryBldr.addWhereAttrInQuery(CISales.IncomingInvoicePosition.DocumentAbstractLink, attrQuery);
                    queryBldr.addWhereAttrEqValue(CISales.IncomingInvoicePosition.Product, getProductInstance());
                    final MultiPrintQuery multi = queryBldr.getPrint();
                    multi.addAttribute(CISales.IncomingInvoicePosition.NetUnitPrice);
                    multi.execute();
                    if (multi.next()) {
                        ret = multi.<BigDecimal>getAttribute(CISales.IncomingInvoicePosition.NetUnitPrice);
                    }
                } else if (CISales.IncomingInvoice.getType().equals(docInstTmp.getType())) {
                    final QueryBuilder queryBldr = new QueryBuilder(CISales.IncomingInvoicePosition);
                    queryBldr.addWhereAttrEqValue(CISales.IncomingInvoicePosition.DocumentAbstractLink, docInstTmp);
                    queryBldr.addWhereAttrEqValue(CISales.IncomingInvoicePosition.Product, getProductInstance());
                    final MultiPrintQuery multi = queryBldr.getPrint();
                    multi.addAttribute(CISales.IncomingInvoicePosition.NetUnitPrice);
                    multi.execute();
                    if (multi.next()) {
                        ret = multi.<BigDecimal>getAttribute(CISales.IncomingInvoicePosition.NetUnitPrice);
                    }
                }
            }
            return ret;
        }

        /**
         * Setter method for instance variable {@link #cost}.
         *
         * @param _cost value for instance variable {@link #cost}
         */
        public void setCost(final BigDecimal _cost)
        {
            this.cost = _cost;
        }


        /**
         * Getter method for the instance variable {@link #result}.
         *
         * @return value of instance variable {@link #result}
         */
        public BigDecimal getResult()
        {
            return this.result;
        }

        /**
         * Setter method for instance variable {@link #result}.
         *
         * @param _result value for instance variable {@link #result}
         */
        public void setResult(final BigDecimal _result)
        {
            this.result = _result;
        }

        @Override
        public String toString()
        {
            return ToStringBuilder.reflectionToString(this);
        }


        /**
         * Getter method for the instance variable {@link #docInstance}.
         *
         * @return value of instance variable {@link #docInstance}
         */
        public Instance getDocInstance()
            throws EFapsException
        {
            if (this.docInstance == null) {
                initTransaction();
            }
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
         * Getter method for the instance variable {@link #upToDate}.
         *
         * @return value of instance variable {@link #upToDate}
         */
        public boolean isUpToDate()
            throws EFapsException
        {
            if (this.upToDate == null) {
                initCosting();
            }
            return this.upToDate;
        }

        /**
         * Setter method for instance variable {@link #upToDate}.
         *
         * @param _upToDate value for instance variable {@link #upToDate}
         */
        public void setUpToDate(final boolean _upToDate)
        {
            this.upToDate = _upToDate;
        }
    }
}
