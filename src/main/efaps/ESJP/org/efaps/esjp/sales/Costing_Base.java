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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.user.Company;
import org.efaps.ci.CIAdminUser;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.RateInfo;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.SalesSettings;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("65041308-73a6-47de-bd1d-3dacc37dbc6c")
@EFapsRevision("$Rev$")
public abstract class Costing_Base
    implements Job
{
    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Costing.class);

    /**
     * Max transaction default value.
     */
    private static int MAXTRANSACTION = 100;

    /**
     * <p>
     * Called by the <code>Scheduler</code> when a <code>Trigger</code>
     * fires that is associated with the <code>Job</code>.
     * </p>
     * @param   _context JobExecutionContext for this job
     * @throws JobExecutionException
     *           if there is an exception while executing the job.
     */
    @Override
    public void execute(final JobExecutionContext _context)
        throws JobExecutionException
    {
        try {
            final QueryBuilder queryBldr = new QueryBuilder(CIAdminUser.Company);
            final InstanceQuery query = queryBldr.getQuery();
            query.executeWithoutAccessCheck();
            while (query.next()) {
                final Company company = Company.get(query.getCurrentValue().getId());
                Context.getThreadContext().setCompany(company);
                if (Sales.getSysConfig().getAttributeValueAsBoolean(SalesSettings.ACTIVATECOSTING)) {
                    update();
                }
            }
            // remove the company to be sure
            Context.getThreadContext().setCompany(null);
        } catch (final EFapsException e) {
            Costing_Base.LOG.error("Catched error", e);
        }
    }

    /**
     * Used to mark an Costing as not "UpToDate" on a trigger.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return new empty Return
     * @throws EFapsException on error
     */
    public Return relationTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_parameter.getInstance());
        print.addAttribute(CISales.Document2DocumentAbstract.ToAbstractLink);
        print.executeWithoutAccessCheck();

        final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.TransactionInOutAbstract);
        attrQueryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Document,
                        print.getAttribute(CISales.Document2DocumentAbstract.ToAbstractLink));
        final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CIProducts.TransactionInOutAbstract.ID);

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.CostingAbstract);
        queryBldr.addWhereAttrInQuery(CIProducts.CostingAbstract.TransactionAbstractLink, attrQuery);
        queryBldr.addWhereAttrEqValue(CIProducts.CostingAbstract.UpToDate, true);
        final InstanceQuery query = queryBldr.getQuery();
        query.executeWithoutAccessCheck();
        while (query.next()) {
            final Update update = new Update(query.getCurrentValue());
            update.add(CIProducts.CostingAbstract.UpToDate, false);
            update.executeWithoutTrigger();
        }
        return new Return();
    }


    /**
     * To be able to execute the update from an UserInterface.
     * @param _parameter Parameter as passed by the eFaps API
     * @return new empty Return
     * @throws EFapsException on error
     */
    public Return execute(final Parameter _parameter)
        throws EFapsException
    {
        update();
        return new Return();
    }

    /**
     * @return the max value of transactions to be read before saving the context.
     */
    protected int getMaxTransaction()
    {
        int ret = Costing_Base.MAXTRANSACTION;
        try {
            final int tmp = Sales.getSysConfig().getAttributeValueAsInteger(SalesSettings.COSTINGMAXTRANSACTION);
            if (tmp > 0) {
                ret = tmp;
            }
        } catch (final EFapsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * @throws EFapsException on error
     */
    protected void update()
        throws EFapsException
    {
        boolean repeat = true;
        int runs = 0;
        // loop to be able to save the transactions before timeout
        while (repeat) {
            runs++;
            Costing_Base.LOG.debug("Executing run Number: {}", runs);
            repeat = false;
            final Set<Instance> updateCost = new LinkedHashSet<Instance>();

            // check for costing that must be updated
            final QueryBuilder costQueryBldr = new QueryBuilder(CIProducts.Costing);
            costQueryBldr.addWhereAttrEqValue(CIProducts.Costing.UpToDate, false);
            costQueryBldr.addOrderByAttributeAsc(CIProducts.Costing.Created);
            final InstanceQuery costQuery = costQueryBldr.getQuery();
            costQuery.executeWithoutAccessCheck();
            while (costQuery.next()) {
                final Instance penultimate = getPenultimate4Costing(costQuery.getCurrentValue());
                if (penultimate != null) {
                    updateCost.add(penultimate);
                    Costing_Base.LOG.debug("Adding Instance for Update: {}", penultimate);
                }
            }
            final int maxTrans = getMaxTransaction();

            // check for new transactions and add the costing for them
            final SelectBuilder selDocInst = new SelectBuilder()
                            .linkto(CIProducts.TransactionInOutAbstract.Document).instance();
            final SelectBuilder selProdInst = new SelectBuilder()
                            .linkto(CIProducts.TransactionInOutAbstract.Product).instance();

            final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.Costing);
            final AttributeQuery attrQuery = attrQueryBldr
                            .getAttributeQuery(CIProducts.Costing.TransactionAbstractLink);

            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.TransactionInOutAbstract);
            add2QueryBldr4Transaction(queryBldr);
            queryBldr.addWhereAttrNotInQuery(CIProducts.TransactionInOutAbstract.ID, attrQuery);
            queryBldr.addOrderByAttributeAsc(CIProducts.TransactionInOutAbstract.Date);
            queryBldr.addOrderByAttributeAsc(CIProducts.TransactionInOutAbstract.Position);
            final InstanceQuery query = queryBldr.getQuery();
            query.setLimit(maxTrans + 5);
            final MultiPrintQuery multi = new MultiPrintQuery(query.executeWithoutAccessCheck());
            multi.addSelect(selDocInst, selProdInst);
            multi.setEnforceSorted(true);
            multi.executeWithoutAccessCheck();

            final Set<TransCosting> costingTmp = new LinkedHashSet<TransCosting>();

            int count = 0;
            while (multi.next()) {
                final TransCosting transCost = getTransCosting();
                transCost.setTransactionInstance(multi.getCurrentInstance());
                transCost.setDocInstance(multi.<Instance>getSelect(selDocInst));
                transCost.setProductInstance(multi.<Instance>getSelect(selProdInst));
                transCost.setCostingQuantity(BigDecimal.ZERO);
                transCost.setUpToDate(false);
                transCost.insertCosting();
                costingTmp.add(transCost);
                Costing_Base.LOG.debug("Adding Number: {} for TransCosting: {}", count, transCost);
                count++;
                if (count > maxTrans) {
                    repeat = true;
                    break;
                }
            }

            for (final TransCosting transCost : costingTmp) {
                final Instance penultimate = getPenultimate4Costing(transCost.getCostingInstance());
                if (penultimate != null) {
                    updateCost.add(penultimate);
                    Costing_Base.LOG.debug("Adding TransCost for Update: {}", penultimate);
                }
            }

            final Map<Instance, TransCosting> prod2cost = new HashMap<Instance, TransCosting>();
            for (final Instance inst : updateCost) {
                final TransCosting transCost = updateCosting(inst);
                prod2cost.put(transCost.getProductInstance(), transCost);
                Costing_Base.LOG.debug(" Updated TransCosting: {}", transCost);
            }

            for (final TransCosting transCost : prod2cost.values()) {
                updateCost(transCost);
            }

            if (repeat) {
                Context.save();
            }
        }
    }

    /**
     * @param _queryBldr queryBuilder to add to
     * @throws EFapsException on error
     */
    protected void add2QueryBldr4Transaction(final QueryBuilder _queryBldr)
        throws EFapsException
    {
        final Instance strGrpInst = Sales.getSysConfig().getLink(SalesSettings.COSTINGSTORAGEGROUP);
        if (strGrpInst != null && strGrpInst.isValid()) {
            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.StorageGroupAbstract2StorageAbstract);
            queryBldr.addWhereAttrEqValue(CIProducts.StorageGroupAbstract2StorageAbstract.FromAbstractLink, strGrpInst);
            final AttributeQuery attrQuery = queryBldr
                            .getAttributeQuery(CIProducts.StorageGroupAbstract2StorageAbstract.ToAbstractLink);
            _queryBldr.addWhereAttrInQuery(CIProducts.TransactionAbstract.Storage, attrQuery);
        }
    }

    /**
     * @param _transCost TransCosting containing the information to register the cost
     * @throws EFapsException on error
     */
    protected void updateCost(final TransCosting _transCost)
        throws EFapsException
    {
        BigDecimal currPrice = BigDecimal.ZERO;
        final DateTime date = new DateTime();
        final QueryBuilder costBldr = new QueryBuilder(CIProducts.ProductCost);
        costBldr.addWhereAttrEqValue(CIProducts.ProductCost.ProductLink, _transCost.getProductInstance());
        costBldr.addWhereAttrGreaterValue(CIProducts.ProductCost.ValidUntil, date.minusMinutes(1));
        costBldr.addWhereAttrLessValue(CIProducts.ProductCost.ValidFrom, date.plusMinutes(1));
        final MultiPrintQuery costMulti = costBldr.getPrint();
        costMulti.addAttribute(CIProducts.ProductCost.Price);
        costMulti.executeWithoutAccessCheck();
        if (costMulti.next()) {
            currPrice = costMulti.<BigDecimal>getAttribute(CIProducts.ProductCost.Price);
        }
        if (_transCost.getResult().compareTo(BigDecimal.ZERO) != 0
                        && currPrice.compareTo(_transCost.getResult()) != 0) {
            Costing_Base.LOG.debug(" Updating Cost for: {}", _transCost);
            final Insert insert = new Insert(CIProducts.ProductCost);
            insert.add(CIProducts.ProductCost.ProductLink, _transCost.getProductInstance());
            insert.add(CIProducts.ProductCost.Price, _transCost.getResult());
            insert.add(CIProducts.ProductCost.ValidFrom, date);
            insert.add(CIProducts.ProductCost.ValidUntil, date.plusYears(10));
            insert.add(CIProducts.ProductCost.CurrencyLink, Currency.getBaseCurrency());
            insert.executeWithoutAccessCheck();
        }
    }


    /**
     * @param _costingInstance start instance (the instance with the last
     *            correct value)
     * @return last TransCosting containing the final result for the product
     * @throws EFapsException on error
     */
    protected TransCosting updateCosting(final Instance _costingInstance)
        throws EFapsException
    {
        TransCosting ret = null;
        Costing_Base.LOG.debug("Update Costing for: {}", _costingInstance);

        final List<TransCosting> tcList = new ArrayList<TransCosting>();

        final TransCosting transCosting = getTransCosting();
        transCosting.setCostingInstance(_costingInstance);

        tcList.add(transCosting);

        final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.Costing);
        final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CIProducts.Costing.TransactionAbstractLink);

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.TransactionInOutAbstract);
        add2QueryBldr4Transaction(queryBldr);
        queryBldr.addWhereAttrInQuery(CIProducts.TransactionInOutAbstract.ID, attrQuery);
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
        multi.setEnforceSorted(true);
        multi.executeWithoutAccessCheck();
        boolean start = false;
        while (multi.next()) {
            if (start) {
                final TransCosting transCostingTmp = getTransCosting();
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
            Costing_Base.LOG.debug("Verify TransactionCosting: {}", current);
            if (prev != null) {
                boolean update = false;
                final BigDecimal newCostQuantity = prev.getQuantity();
                // for outbound ensure to inherit correctly
                if (current.isOutBound()) {
                    if (current.getCost().compareTo(prev.getResult()) != 0) {
                        current.setCost(prev.getResult());
                        update = true;
                    }
                    if (current.getResult().compareTo(prev.getResult()) != 0) {
                        current.setResult(prev.getResult());
                        update = true;
                    }
                } else {
                    if (!current.isUpToDate() || forceCostFromDoc) {
                        final BigDecimal cost = current.getCostFromDocument();
                        if (current.getCost().compareTo(cost) != 0) {
                            current.setCost(cost);
                            update = true;
                            // if the current cost was updated, the following must be update also
                            forceCostFromDoc = true;
                        }
                    }
                    // check if the costing has a cost assigned, if not inherit from previous
                    if (current.getCost().compareTo(BigDecimal.ZERO) == 0) {
                        // it mus be inherited the average, but if not exist use the cost directly
                        if (prev.getResult().compareTo(BigDecimal.ZERO) == 0) {
                            current.setCost(prev.getCost());
                        } else {
                            current.setCost(prev.getResult());
                        }

                        update = true;
                    }

                    final BigDecimal divisor = newCostQuantity.add(current.getTransactionQuantity());
                    final BigDecimal result;
                    if (divisor.compareTo(BigDecimal.ZERO) != 0) {
                        result = prev.getResult().multiply(newCostQuantity)
                                        .add(current.getCost().multiply(current.getTransactionQuantity()))
                                        .setScale(12, BigDecimal.ROUND_HALF_UP)
                                        .divide(divisor, BigDecimal.ROUND_HALF_UP);
                    } else {
                        result = BigDecimal.ZERO;
                    }

                    if (result.compareTo(current.getResult()) != 0) {
                        current.setResult(result);
                        update = true;
                    }
                }

                if (newCostQuantity.compareTo(current.getCostingQuantity()) != 0) {
                    current.setCostingQuantity(newCostQuantity);
                    update = true;
                }
                if (update) {
                    current.updateCosting();
                    Costing_Base.LOG.debug("Update TransactionCosting: {}", current);
                }
            } else {
                // if the current was marked for update (happens only if it also
                // was the first ever)
                if (!current.isUpToDate() && !current.isOutBound()) {
                    final BigDecimal cost = current.getCostFromDocument();
                    if (current.getCost().compareTo(cost) != 0) {
                        current.setCost(cost);
                        current.setResult(cost);
                        // if the first has its cost from doc changed, the rest
                        // must be checked also
                        forceCostFromDoc = true;
                    }
                    current.updateCosting();
                    Costing_Base.LOG.debug("Update TransactionCosting: {}", current);
                }
            }
            prev = current;
            ret = current;
        }
        return ret;
    }

    /**
     * @param _costingInstance instance the penultimate instance is wanted for
     * @return penultimate instance
     * @throws EFapsException on error
     */
    protected Instance getPenultimate4Costing(final Instance _costingInstance)
        throws EFapsException
    {
        Costing_Base.LOG.debug("Searching Penultimate for {}", _costingInstance);
        Instance ret = null;
        Instance transInstance = null;

        final TransCosting transCosting = getTransCosting();
        transCosting.setCostingInstance(_costingInstance);

        // in general we do not want the one which are not "UpToDate"
        final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.Costing);
        attrQueryBldr.addWhereAttrEqValue(CIProducts.Costing.UpToDate, false);
        final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CIProducts.Costing.TransactionAbstractLink);

        Costing_Base.LOG.trace("Searching Penultimate in same date");
        // first check if for the same date exists one (partial update), also must verify the position
        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.TransactionInOutAbstract);
        add2QueryBldr4Transaction(queryBldr);
        queryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Product, transCosting.getProductInstance());
        queryBldr.addWhereAttrNotInQuery(CIProducts.TransactionInOutAbstract.ID, attrQuery);
        queryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Date, transCosting.getDate());
        queryBldr.addOrderByAttributeAsc(CIProducts.TransactionInOutAbstract.Position);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.setEnforceSorted(true);
        multi.addAttribute(CIProducts.TransactionInOutAbstract.Position);
        multi.executeWithoutAccessCheck();
        Instance prev = null;
        while (multi.next()) {
            final Integer position = multi.getAttribute(CIProducts.TransactionInOutAbstract.Position);
            final Integer curPos = transCosting.getPosition();
            // if the position is higher than the one given stop to iterate
            if (position != null && curPos != null && position > curPos) {
                break;
            }

            if (multi.getCurrentInstance().equals(transCosting.getTransactionInstance())) {
                if (prev != null) {
                    transInstance = prev;
                    break;
                }
            } else {
                prev = multi.getCurrentInstance();
            }
        }
        // if not found yet check on all dates before, but still only "UpToDate" ones  (partial update)
        if (prev == null) {
            Costing_Base.LOG.trace("Searching Penultimate in 'UpToDates'");
            final QueryBuilder queryBldr2 = new QueryBuilder(CIProducts.TransactionInOutAbstract);
            add2QueryBldr4Transaction(queryBldr2);
            queryBldr2.addWhereAttrNotInQuery(CIProducts.TransactionInOutAbstract.ID, attrQuery);
            queryBldr2.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Product,
                            transCosting.getProductInstance());
            queryBldr2.addWhereAttrLessValue(CIProducts.TransactionInOutAbstract.Date, transCosting.getDate());
            queryBldr2.addOrderByAttributeDesc(CIProducts.TransactionInOutAbstract.Date);
            queryBldr2.addOrderByAttributeDesc(CIProducts.TransactionInOutAbstract.Position);
            final InstanceQuery query2 = queryBldr2.getQuery();
            query2.setLimit(1);
            query2.executeWithoutAccessCheck();
            while (query2.next()) {
                transInstance = query2.getCurrentValue();
            }
        } else if (transInstance == null) {
            transInstance = prev;
        }
        // still not found yet check on all no matter of the "UpToDate" or date (update all)
        if (prev == null) {
            Costing_Base.LOG.trace("Searching Penultimate over all");
            final QueryBuilder queryBldr2 = new QueryBuilder(CIProducts.TransactionInOutAbstract);
            add2QueryBldr4Transaction(queryBldr2);
            queryBldr2.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Product,
                            transCosting.getProductInstance());
            queryBldr2.addOrderByAttributeAsc(CIProducts.TransactionInOutAbstract.Date);
            queryBldr2.addOrderByAttributeAsc(CIProducts.TransactionInOutAbstract.Position);
            final InstanceQuery query2 = queryBldr2.getQuery();
            query2.setLimit(1);
            query2.executeWithoutAccessCheck();
            while (query2.next()) {
                transInstance = query2.getCurrentValue();
            }
        }

        if (transInstance == null) {
            ret = _costingInstance;
        } else {
            final TransCosting transCostingRet = getTransCosting();
            transCostingRet.setTransactionInstance(transInstance);
            final Instance retTmp = transCostingRet.getCostingInstance();
            if (retTmp != null) {
                ret = retTmp;
            }
        }
        Costing_Base.LOG.debug("Found Penultimate: {}", ret);
        return ret;
    }

    /**
     * @return new TransCosting instance
     * @throws EFapsException on error
     */
    protected TransCosting getTransCosting()
        throws EFapsException
    {
        return new TransCosting();
    }

    /**
     * Class that represents an transaction and its related Costing.
     */
    public static class TransCosting
    {

        /**
         * The Costing Instance.
         */
        private Instance costingInstance;

        /**
         * The Transaction Instance.
         */
        private Instance transactionInstance;

        /**
         * Instance of the document linked to the Transaction.
         */
        private Instance docInstance;

        /**
         * Instance of the product linked to the Transaction.
         */
        private Instance productInstance;

        /**
         * Date of the transaction.
         */
        private DateTime date;

        /**
         * Quantity of the transaction.
         */
        private BigDecimal transactionQuantity;

        /**
         * Quantity of the costing.
         */
        private BigDecimal costingQuantity;

        /**
         * Cost defined by the Costing.
         */
        private BigDecimal cost;

        /**
         * The calculated result of Costing.
         */
        private BigDecimal result;

        /**
         * Is the Costing up to date.
         */
        private Boolean upToDate;

        /**
         * Position Number of the transaction.
         */
        private Integer position;

        /**
         * @throws EFapsException on error
         */
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
            final SelectBuilder selPos = new SelectBuilder()
                            .linkto(CIProducts.Costing.TransactionAbstractLink)
                            .attribute(CIProducts.TransactionInOutAbstract.Position);

            final PrintQuery print = new PrintQuery(getCostingInstance());
            print.addSelect(selTransInst, selProdInst, selDate, selQuantity, selDocInst, selPos);
            print.executeWithoutAccessCheck();
            this.productInstance = print.<Instance>getSelect(selProdInst);
            this.docInstance = print.<Instance>getSelect(selDocInst);
            this.date = print.<DateTime>getSelect(selDate);
            this.transactionInstance = print.<Instance>getSelect(selTransInst);
            this.transactionQuantity = print.<BigDecimal>getSelect(selQuantity);
            this.position = print.<Integer>getSelect(selPos);
        }

        /**
         * @throws EFapsException on error
         */
        protected void initCosting()
            throws EFapsException
        {
            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.Costing);
            queryBldr.addWhereAttrEqValue(CIProducts.Costing.TransactionAbstractLink, getTransactionInstance());
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CIProducts.Costing.Cost, CIProducts.Costing.Quantity, CIProducts.Costing.Result,
                            CIProducts.Costing.UpToDate);
            multi.executeWithoutAccessCheck();
            if (multi.next()) {
                this.upToDate = multi.<Boolean>getAttribute(CIProducts.Costing.UpToDate);
                this.cost = multi.<BigDecimal>getAttribute(CIProducts.Costing.Cost);
                this.costingQuantity = multi.<BigDecimal>getAttribute(CIProducts.Costing.Quantity);
                this.result = multi.<BigDecimal>getAttribute(CIProducts.Costing.Result);
                this.costingInstance = multi.getCurrentInstance();
            }
        }

        /**
         * Update the Costing Instance.
         * @throws EFapsException on error
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

        /**
         * Insert a new Costing Instance.
         * @throws EFapsException on error
         */
        public void insertCosting()
            throws EFapsException
        {
            final BigDecimal costTmp;
            if (isOutBound()) {
                costTmp = BigDecimal.ZERO;
            } else {
                costTmp = getCostFromDocument();
            }
            final Insert insert = new Insert(CIProducts.Costing);
            insert.add(CIProducts.Costing.Quantity, getCostingQuantity());
            insert.add(CIProducts.Costing.TransactionAbstractLink, getTransactionInstance());
            insert.add(CIProducts.Costing.Cost, costTmp);
            insert.add(CIProducts.Costing.Result, costTmp);
            insert.add(CIProducts.Costing.UpToDate, isUpToDate());
            insert.executeWithoutTrigger();

            setCost(costTmp);
            setResult(costTmp);
            setCostingInstance(insert.getInstance());
        }


        /**
         * @param _costingInstance instance to be set
         */
        public void setCostingInstance(final Instance _costingInstance)
        {
            this.costingInstance = _costingInstance;
        }

        /**
         * Getter method for the instance variable {@link #transactionInstance}.
         *
         * @return value of instance variable {@link #transactionInstance}
         * @throws EFapsException on error
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
         * @throws EFapsException on error
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
         * @throws EFapsException on error
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
         * @throws EFapsException on error
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
         * @throws EFapsException on error
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
         * @return true if it is an outbound transaction
         */
        public boolean isOutBound()
        {
            return this.transactionInstance.getType().isKindOf(CIProducts.TransactionOutbound.getType());
        }

        /**
         * Setter method for instance variable {@link #transactionQuantity}.
         *
         * @param _transactionQuantity value for instance variable
         *            {@link #transactionQuantity}
         *
         */
        public void setTransactionQuantity(final BigDecimal _transactionQuantity)
        {
            this.transactionQuantity = _transactionQuantity;
        }

        /**
         * Getter method for the instance variable {@link #costingQuantity}.
         *
         * @return value of instance variable {@link #costingQuantity}
         * @throws EFapsException on error
         */
        public BigDecimal getCostingQuantity()
            throws EFapsException
        {
            if (this.costingQuantity == null) {
                initCosting();
            }
            return this.costingQuantity;
        }

        /**
         * @return the Quantity of this instance = CostingQuantity + TransactionQuantity
         * @throws EFapsException on error
         */
        public BigDecimal getQuantity()
            throws EFapsException
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
         * @throws EFapsException on error
         */
        public BigDecimal getCost() throws EFapsException
        {
            if (this.cost == null) {
                initCosting();
            }
            return this.cost;
        }

        /**
         * @return the Cost to be used for Costing retrieved by analyzing the related Documents
         * @throws EFapsException on error
         */
        public BigDecimal getCostFromDocument()
            throws EFapsException
        {
            Costing_Base.LOG.debug("Analysing Cost From Document for: {}", this);
            BigDecimal ret = null;
            final Instance docInstTmp = getDocInstance();
            if (docInstTmp != null && docInstTmp.isValid()) {
                final CostDoc costDoc = new CostDoc(docInstTmp, getProductInstance());
                ret = costDoc.getCost();
            }
            Costing_Base.LOG.debug("Result: {} for  Cost From Document for: {}", ret, this);
            return ret == null ? BigDecimal.ZERO : ret;
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

        /**
         * Getter method for the instance variable {@link #docInstance}.
         *
         * @return value of instance variable {@link #docInstance}
         * @throws EFapsException on error
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
         * @throws EFapsException on error
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

        /**
         * Getter method for the instance variable {@link #position}.
         *
         * @return value of instance variable {@link #position}
         * @throws EFapsException on error
         */
        public Integer getPosition()
            throws EFapsException
        {
            if (this.position == null) {
                initTransaction();
            }
            return this.position;
        }

        /**
         * Setter method for instance variable {@link #position}.
         *
         * @param _position value for instance variable {@link #position}
         */
        public void setPosition(final Integer _position)
        {
            this.position = _position;
        }

        @Override
        public String toString()
        {
            return ToStringBuilder.reflectionToString(this);
        }
    }


    /**
     * TODO comment!
     *
     * @author The eFaps Team
     * @version $Id$
     */
    public static class CostDoc
    {

        /**
         * BigDecimal cost.
         */
        private BigDecimal cost = BigDecimal.ZERO;

        /**
         * Boolean (true/false).
         */
        private boolean initialized;

        /**
         * Instance of the Document.
         */
        private Instance baseDocInst;

        /**
         * Instance of the Costing Document.
         */
        private Instance costDocInst;

        /**
         * Instance of the product.
         */
        private Instance productInst;

        /**
         * @param _docInst Instance of the document.
         * @param _productInst Instance of the product.
         */
        public CostDoc(final Instance _docInst,
                       final Instance _productInst)
        {
            this.baseDocInst = _docInst;
            this.productInst = _productInst;
        }

        /**
         * @throws EFapsException on error.
         */
        protected void init()
            throws EFapsException
        {
            if (!this.initialized) {
                this.initialized = true;
                boolean found = false;
                final SelectBuilder docInstSel = SelectBuilder.get().linkto(
                                CISales.PositionAbstract.DocumentAbstractLink).instance();
                if (CISales.RecievingTicket.getType().equals(getBaseDocInst().getType())) {
                    // first priority are the special relation for costing
                    // "Sales_AcquisitionCosting2RecievingTicket"
                    final QueryBuilder acRelAttrQueryBldr = new QueryBuilder(CISales.AcquisitionCosting2RecievingTicket);
                    acRelAttrQueryBldr.addWhereAttrEqValue(CISales.AcquisitionCosting2RecievingTicket.ToLink,
                                    getBaseDocInst());
                    final AttributeQuery acRelAttrQuery = acRelAttrQueryBldr
                                    .getAttributeQuery(CISales.AcquisitionCosting2RecievingTicket.FromLink);
                    final QueryBuilder acPosQueryBldr = new QueryBuilder(CISales.AcquisitionCostingPosition);
                    acPosQueryBldr.addWhereAttrInQuery(CISales.AcquisitionCostingPosition.DocumentAbstractLink,
                                    acRelAttrQuery);
                    acPosQueryBldr.addWhereAttrEqValue(CISales.AcquisitionCostingPosition.Product, getProductInst());
                    final MultiPrintQuery acPosMulti = acPosQueryBldr.getPrint();
                    acPosMulti.addSelect(docInstSel);
                    acPosMulti.addAttribute(CISales.AcquisitionCostingPosition.NetUnitPrice);
                    acPosMulti.execute();
                    if (acPosMulti.next()) {
                        found = true;
                        setCost(acPosMulti.<BigDecimal>getAttribute(CISales.AcquisitionCostingPosition.NetUnitPrice));
                        setCostDocInst(acPosMulti.<Instance>getSelect(docInstSel));
                    }
                    if (!found) {
                        final QueryBuilder relAttrQueryBldr = new QueryBuilder(CISales.IncomingInvoice2RecievingTicket);
                        relAttrQueryBldr.addWhereAttrEqValue(CISales.IncomingInvoice2RecievingTicket.ToLink,
                                        getBaseDocInst());
                        final AttributeQuery relAttrQuery = relAttrQueryBldr
                                        .getAttributeQuery(CISales.IncomingInvoice2RecievingTicket.FromLink);
                        final QueryBuilder posQueryBldr = new QueryBuilder(CISales.IncomingInvoicePosition);
                        posQueryBldr.addWhereAttrInQuery(CISales.IncomingInvoicePosition.DocumentAbstractLink,
                                        relAttrQuery);
                        posQueryBldr.addWhereAttrEqValue(CISales.IncomingInvoicePosition.Product, getProductInst());
                        final MultiPrintQuery posMulti = posQueryBldr.getPrint();
                        posMulti.addSelect(docInstSel);
                        posMulti.addAttribute(CISales.IncomingInvoicePosition.NetUnitPrice);
                        posMulti.execute();
                        if (posMulti.next()) {
                            found = true;
                            setCost(posMulti.<BigDecimal>getAttribute(CISales.IncomingInvoicePosition.NetUnitPrice));
                            setCostDocInst(posMulti.<Instance>getSelect(docInstSel));
                        }
                    }
                    // if not found yet, try other relations
                    if (!found) {
                        final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Document2DerivativeDocument);
                        attrQueryBldr.addWhereAttrEqValue(CISales.Document2DerivativeDocument.From, getBaseDocInst());
                        final AttributeQuery attrQuery = attrQueryBldr
                                        .getAttributeQuery(CISales.Document2DerivativeDocument.To);
                        final QueryBuilder queryBldr = new QueryBuilder(CISales.IncomingInvoicePosition);
                        queryBldr.addWhereAttrInQuery(CISales.IncomingInvoicePosition.DocumentAbstractLink, attrQuery);
                        queryBldr.addWhereAttrEqValue(CISales.IncomingInvoicePosition.Product, getProductInst());
                        final MultiPrintQuery multi = queryBldr.getPrint();
                        multi.addSelect(docInstSel);
                        multi.addAttribute(CISales.IncomingInvoicePosition.NetUnitPrice);
                        multi.execute();
                        if (multi.next()) {
                            setCost(multi.<BigDecimal>getAttribute(CISales.IncomingInvoicePosition.NetUnitPrice));
                            setCostDocInst(multi.<Instance>getSelect(docInstSel));
                            found = true;
                        }
                    }
                } else if (CISales.IncomingInvoice.getType().equals(getBaseDocInst().getType())) {
                    final QueryBuilder queryBldr = new QueryBuilder(CISales.IncomingInvoicePosition);
                    queryBldr.addWhereAttrEqValue(CISales.IncomingInvoicePosition.DocumentAbstractLink,
                                    getBaseDocInst());
                    queryBldr.addWhereAttrEqValue(CISales.IncomingInvoicePosition.Product, getProductInst());
                    final MultiPrintQuery multi = queryBldr.getPrint();
                    multi.addSelect(docInstSel);
                    multi.addAttribute(CISales.IncomingInvoicePosition.NetUnitPrice);
                    multi.execute();
                    if (multi.next()) {
                        setCost(multi.<BigDecimal>getAttribute(CISales.IncomingInvoicePosition.NetUnitPrice));
                        setCostDocInst(multi.<Instance>getSelect(docInstSel));
                        found = true;
                    }
                } else if (CISales.TransactionDocumentShadowIn.getType().equals(getBaseDocInst().getType())) {
                    final QueryBuilder relAttrQueryBldr = new QueryBuilder(CISales.Document2DocumentAbstract);
                    relAttrQueryBldr.addWhereAttrEqValue(CISales.Document2DocumentAbstract.ToAbstractLink,
                                    getBaseDocInst());
                    final QueryBuilder posQueryBldr = new QueryBuilder(CISales.PositionSumAbstract);
                    posQueryBldr.addWhereAttrInQuery(CISales.PositionSumAbstract.DocumentAbstractLink,
                                relAttrQueryBldr.getAttributeQuery(CISales.Document2DocumentAbstract.FromAbstractLink));
                    posQueryBldr.addWhereAttrEqValue(CISales.PositionSumAbstract.Product, getProductInst());
                    final MultiPrintQuery posMulti = posQueryBldr.getPrint();
                    posMulti.addSelect(docInstSel);
                    posMulti.addAttribute(CISales.PositionSumAbstract.NetUnitPrice);
                    posMulti.execute();
                    if (posMulti.next()) {
                        found = true;
                        setCost(posMulti.<BigDecimal>getAttribute(CISales.IncomingInvoicePosition.NetUnitPrice));
                        setCostDocInst(posMulti.<Instance>getSelect(docInstSel));
                        found = true;
                    }
                }
                if (!found) {
                    found = analyze();
                }
                if (!found) {
                    setCostDocInst(getBaseDocInst());
                }
            }
        }

        /**
         * @return boolean (true/false).
         *
         * @throws EFapsException on error.
         */
        protected boolean analyze()
            throws EFapsException
        {
            // TODO Auto-generated method stub
            return false;
        }

        /**
         * Getter method for the instance variable {@link #cost}.
         *
         * @return value of instance variable {@link #cost}.
         * @throws EFapsException on error.
         */
        public BigDecimal getCost()
            throws EFapsException
        {
            init();
            return this.cost;
        }

        /**
         * Getter method for the instance variable {@link #cost}.
         *
         * @return value of instance variable {@link #cost}.
         * @throws EFapsException on error.
         */
        public BigDecimal getCost(final Parameter _parameter,
                                  final Instance _currencyInst)
            throws EFapsException
        {
            final PrintQuery print = new PrintQuery(getCostDocInst());
            print.addAttribute(CIERP.DocumentAbstract.Date);
            print.execute();

            return getCost(_parameter, _currencyInst, print.<DateTime>getAttribute(CIERP.DocumentAbstract.Date));
        }

        /**
         * Getter method for the instance variable {@link #cost}.
         *
         * @return value of instance variable {@link #cost}.
         * @throws EFapsException on error.
         */
        public BigDecimal getCost(final Parameter _parameter,
                                  final Instance _currencyInst,
                                  final DateTime _date)
            throws EFapsException
        {
            BigDecimal ret = getCost();
            if (ret != null && ret.compareTo(BigDecimal.ZERO) != 0) {
                if (!Currency.getBaseCurrency().equals(_currencyInst)) {
                    final RateInfo rateInfo = new Currency().evaluateRateInfo(_parameter, _date, _currencyInst);
                    ret = ret.multiply(rateInfo.getRate());
                }
            }
            return ret;
        }

        /**
         * Getter method for the instance variable {@link #docInst}.
         *
         * @return value of instance variable {@link #docInst}
         */
        public Instance getBaseDocInst()
        {
            return this.baseDocInst;
        }

        /**
         * Setter method for instance variable {@link #docInst}.
         *
         * @param _docInst value for instance variable {@link #docInst}
         */
        public void setBaseDocInst(final Instance _docInst)
        {
            this.baseDocInst = _docInst;
        }

        /**
         * Getter method for the instance variable {@link #productInst}.
         *
         * @return value of instance variable {@link #productInst}
         */
        public Instance getProductInst()
        {
            return this.productInst;
        }

        /**
         * Setter method for instance variable {@link #productInst}.
         *
         * @param _productInst value for instance variable {@link #productInst}
         */
        public void setProductInst(final Instance _productInst)
        {
            this.productInst = _productInst;
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
         * Getter method for the instance variable {@link #costDocInst}.
         *
         * @return value of instance variable {@link #costDocInst}.
         * @throws EFapsException on error.
         */
        public Instance getCostDocInst()
            throws EFapsException
        {
            init();
            return this.costDocInst;
        }

        /**
         * Setter method for instance variable {@link #costDocInst}.
         *
         * @param _costDocInst value for instance variable {@link #costDocInst}
         */
        public void setCostDocInst(final Instance _costDocInst)
        {
            this.costDocInst = _costDocInst;
        }
    }
}
