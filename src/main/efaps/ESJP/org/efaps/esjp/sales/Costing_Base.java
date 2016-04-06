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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang3.ArrayUtils;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.user.Company;
import org.efaps.api.background.IExecutionBridge;
import org.efaps.api.background.IJob;
import org.efaps.ci.CIAdminUser;
import org.efaps.ci.CIType;
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
import org.efaps.esjp.common.background.ExecutionBridge;
import org.efaps.esjp.common.background.Service;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.RateInfo;
import org.efaps.esjp.products.Cost;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.efaps.util.cache.CacheReloadException;
import org.joda.time.DateTime;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Auto-generated Javadoc
/**
 * TODO comment!.
 *
 * @author The eFaps Team
 */
@EFapsUUID("65041308-73a6-47de-bd1d-3dacc37dbc6c")
@EFapsApplication("eFapsApp-Sales")
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
     *
     * @param _context the _context
     * @throws JobExecutionException           if there is an exception while executing the job.
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
                if (Sales.COSTINGACTIVATE.get()) {
                    for (final Instance currencyInst : getCurrencyInstances()) {
                        update(currencyInst, null);
                    }
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
                        print.<Long>getAttribute(CISales.Document2DocumentAbstract.ToAbstractLink));
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
    public Return executeAsJob(final Parameter _parameter)
        throws EFapsException
    {
        final String jobName = DBProperties.getFormatedDBProperty(Costing.class.getName() + ".JobName",
                        (Object) Context.getThreadContext().getPerson().getName());
        final IJob job = new IJob()
        {
            /** */
            private static final long serialVersionUID = 1L;

            @Override
            public void execute(final IExecutionBridge _bridge)
            {
                try {
                    final ExecutionBridge bridge = (ExecutionBridge) _bridge;
                    bridge.setJobName(jobName);
                    bridge.setTarget(0);
                    for (final Instance currencyInst : getCurrencyInstances()) {
                        bridge.registerProgress();
                        update(currencyInst, bridge);
                    }
                } catch (final EFapsException e) {
                    LOG.error("Catched error", e);
                } finally {
                    try {
                        ((ExecutionBridge) _bridge).close();
                    } catch (final EFapsException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        Service.get().launch(CISales.BackgroundJobCosting.getType(), job);
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
        for (final Instance currencyInst : getCurrencyInstances()) {
            update(currencyInst, null);
        }
        return new Return();
    }

    /**
     * Gets the currency instances.
     *
     * @return the currency instances
     * @throws EFapsException the eFaps exception
     */
    protected Set<Instance> getCurrencyInstances()
        throws EFapsException
    {
        final Set<Instance> ret = new LinkedHashSet<>();
        ret.add(Currency.getBaseCurrency());
        for (final String currencyOID : Sales.COSTINGALTINSTS.get()) {
            ret.add(Instance.get(currencyOID));
        }
        return ret;
    }

    /**
     * Gets the max transaction.
     *
     * @return the max value of transactions to be read before saving the context.
     */
    protected int getMaxTransaction()
    {
        int ret = Costing_Base.MAXTRANSACTION;
        try {
            final int tmp = Sales.COSTINGMAXTRANSACTION.get();
            if (tmp > 0) {
                ret = tmp;
            }
        } catch (final EFapsException e) {
            LOG.error("EFapsException", e);
        }
        return ret;
    }

    /**
     * Update.
     *
     * @param _currencyInstance the _currency instance
     * @param _bridge the bridge
     * @throws EFapsException on error
     */
    protected void update(final Instance _currencyInstance,
                          final ExecutionBridge _bridge)
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
            // only run it the first time
            if (runs == 1) {
                // check for costing that must be updated
                final QueryBuilder costQueryBldr = new QueryBuilder(CIProducts.CostingAbstract);
                costQueryBldr.addWhereAttrEqValue(CIProducts.CostingAbstract.UpToDate, false);
                costQueryBldr.addWhereAttrEqValue(CIProducts.CostingAbstract.CurrencyLink, _currencyInstance);
                costQueryBldr.addOrderByAttributeAsc(CIProducts.CostingAbstract.Created);
                final InstanceQuery costQuery = costQueryBldr.getCachedQuery4Request();
                costQuery.executeWithoutAccessCheck();
                while (costQuery.next()) {
                    final Instance penultimate = getPenultimate4Costing(_currencyInstance, costQuery.getCurrentValue());
                    if (penultimate != null) {
                        updateCost.add(penultimate);
                        Costing_Base.LOG.debug("Adding Instance for Update: {}", penultimate);
                    }
                }
            }
            final int maxTrans = getMaxTransaction();

            // check for new transactions and add the costing for them
            final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.CostingAbstract);
            attrQueryBldr.addWhereAttrEqValue(CIProducts.CostingAbstract.CurrencyLink, _currencyInstance);
            final AttributeQuery attrQuery = attrQueryBldr
                            .getAttributeQuery(CIProducts.CostingAbstract.TransactionAbstractLink);

            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.TransactionInOutAbstract);
            add2QueryBldr4Transaction(queryBldr);
            queryBldr.addWhereAttrNotInQuery(CIProducts.TransactionInOutAbstract.ID, attrQuery);
            queryBldr.addOrderByAttributeAsc(CIProducts.TransactionInOutAbstract.Date);
            queryBldr.addOrderByAttributeAsc(CIProducts.TransactionInOutAbstract.Position);
            final InstanceQuery query = queryBldr.getQuery();
            query.setLimit(maxTrans + 5);
            final MultiPrintQuery multi = new MultiPrintQuery(query.executeWithoutAccessCheck());
            final SelectBuilder selDocInst = new SelectBuilder()
                            .linkto(CIProducts.TransactionInOutAbstract.Document).instance();
            final SelectBuilder selProdInst = new SelectBuilder()
                            .linkto(CIProducts.TransactionInOutAbstract.Product).instance();
            multi.addSelect(selDocInst, selProdInst);
            multi.setEnforceSorted(true);
            multi.executeWithoutAccessCheck();

            final Set<TransCosting> costingTmp = new LinkedHashSet<TransCosting>();
            int count = 0;
            while (multi.next()) {
                final TransCosting transCost = getTransCosting()
                            .setTransactionInstance(multi.getCurrentInstance())
                            .setDocInstance(multi.<Instance>getSelect(selDocInst))
                            .setProductInstance(multi.<Instance>getSelect(selProdInst))
                            .setCostingQuantity(BigDecimal.ZERO)
                            .setUpToDate(false)
                            .setCurrencyInstance(_currencyInstance)
                            .insertCosting();
                costingTmp.add(transCost);
                Costing_Base.LOG.debug("Adding Number: {} for TransCosting: {}", count, transCost);
                count++;
                if (count > maxTrans) {
                    repeat = true;
                    break;
                }
            }

            for (final TransCosting transCost : costingTmp) {
                final Instance penultimate = getPenultimate4Costing(_currencyInstance, transCost.getCostingInstance());
                if (penultimate != null) {
                    updateCost.add(penultimate);
                    Costing_Base.LOG.debug("Adding TransCost for Update: {}", penultimate);
                }
            }

            final Map<Instance, TransCosting> prod2cost = new HashMap<Instance, TransCosting>();
            for (final Instance inst : updateCost) {
                final TransCosting transCost = updateCosting(_currencyInstance, inst);
                prod2cost.put(transCost.getProductInstance(), transCost);
                Costing_Base.LOG.debug(" Updated TransCosting: {}", transCost);
                if (_bridge != null) {
                    _bridge.registerProgress();
                }
            }

            for (final TransCosting transCost : prod2cost.values()) {
                updateCost(_currencyInstance, transCost);
            }

            if (repeat) {
                Context.save();
            }
            if (_bridge != null) {
                _bridge.registerProgress();
            }
        }
    }

    /**
     * Add2 query bldr4 transaction.
     *
     * @param _queryBldr queryBuilder to add to
     * @throws EFapsException on error
     */
    protected void add2QueryBldr4Transaction(final QueryBuilder _queryBldr)
        throws EFapsException
    {
        final Instance strGrpInst = Sales.COSTINGSTORAGEGROUP.get();
        if (strGrpInst != null && strGrpInst.isValid()) {
            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.StorageGroupAbstract2StorageAbstract);
            queryBldr.addWhereAttrEqValue(CIProducts.StorageGroupAbstract2StorageAbstract.FromAbstractLink, strGrpInst);
            final AttributeQuery attrQuery = queryBldr
                            .getAttributeQuery(CIProducts.StorageGroupAbstract2StorageAbstract.ToAbstractLink);
            _queryBldr.addWhereAttrInQuery(CIProducts.TransactionAbstract.Storage, attrQuery);
        }
    }

    /**
     * Update cost.
     *
     * @param _currencyInstance the currency instance
     * @param _transCost TransCosting containing the information to register the cost
     * @throws EFapsException on error
     */
    protected void updateCost(final Instance _currencyInstance,
                              final TransCosting _transCost)
        throws EFapsException
    {
        BigDecimal currPrice = BigDecimal.ZERO;

        final DateTime date = new DateTime().withTimeAtStartOfDay();

        final CIType ciType = _transCost.getCostingInstance().getType().isCIType(CIProducts.Costing)
                         ? CIProducts.ProductCost : CIProducts.ProductCostAlternative;

        final QueryBuilder costBldr = new QueryBuilder(ciType);
        costBldr.addWhereAttrEqValue(CIProducts.ProductCostAbstract.ProductLink, _transCost.getProductInstance());
        costBldr.addWhereAttrGreaterValue(CIProducts.ProductCostAbstract.ValidUntil, date.minusMinutes(1));
        costBldr.addWhereAttrLessValue(CIProducts.ProductCostAbstract.ValidFrom, date.plusMinutes(1));
        if (ciType.equals(CIProducts.ProductCostAlternative)) {
            costBldr.addWhereAttrEqValue(CIProducts.ProductCostAlternative.CurrencyLink, _currencyInstance);
        }

        final MultiPrintQuery costMulti = costBldr.getPrint();
        costMulti.addAttribute(CIProducts.ProductCostAbstract.Price);
        costMulti.executeWithoutAccessCheck();
        if (costMulti.next()) {
            currPrice = costMulti.<BigDecimal>getAttribute(CIProducts.ProductCostAbstract.Price);
        }
        if (_transCost.getResult().compareTo(BigDecimal.ZERO) != 0 && currPrice.compareTo(_transCost.getResult()
                        .setScale(currPrice.scale(), RoundingMode.HALF_UP)) != 0) {
            Costing_Base.LOG.debug(" Updating Cost for: {}", _transCost);
            final Insert insert = new Insert(ciType);
            insert.add(CIProducts.ProductCostAbstract.ProductLink, _transCost.getProductInstance());
            insert.add(CIProducts.ProductCostAbstract.Price, _transCost.getResult());
            insert.add(CIProducts.ProductCostAbstract.ValidFrom, date);
            insert.add(CIProducts.ProductCostAbstract.ValidUntil, date.plusYears(10));
            insert.add(CIProducts.ProductCostAbstract.CurrencyLink, _currencyInstance);
            insert.executeWithoutAccessCheck();
        }
    }


    /**
     * Update costing.
     *
     * @param _currencyInstance the currency instance
     * @param _costingInstance start instance (the instance with the last
     *            correct value)
     * @return last TransCosting containing the final result for the product
     * @throws EFapsException on error
     */
    protected TransCosting updateCosting(final Instance _currencyInstance,
                                         final Instance _costingInstance)
        throws EFapsException
    {
        TransCosting ret = null;
        Costing_Base.LOG.debug("Update Costing for: {}", _costingInstance);

        final List<TransCosting> tcList = new ArrayList<TransCosting>();

        final TransCosting transCosting = getTransCosting()
                        .setCostingInstance(_costingInstance)
                        .setCurrencyInstance(_currencyInstance);
        tcList.add(transCosting);

        final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.CostingAbstract);
        attrQueryBldr.addWhereAttrEqValue(CIProducts.CostingAbstract.CurrencyLink, _currencyInstance);
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
        multi.addSelect(selProdInst);
        multi.addAttribute(CIProducts.TransactionInOutAbstract.Date,
                        CIProducts.TransactionInOutAbstract.Quantity);
        multi.setEnforceSorted(true);
        multi.executeWithoutAccessCheck();
        boolean start = false;
        while (multi.next()) {
            if (start) {
                final TransCosting transCostingTmp = getTransCosting()
                        .setTransactionInstance(multi.getCurrentInstance())
                        .setDate(multi.<DateTime>getAttribute(CIProducts.TransactionInOutAbstract.Date))
                        .setTransactionQuantity(
                                        multi.<BigDecimal>getAttribute(CIProducts.TransactionInOutAbstract.Quantity))
                        .setProductInstance(multi.<Instance>getSelect(selProdInst))
                        .setCurrencyInstance(_currencyInstance);
                tcList.add(transCostingTmp);
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
                    // the previous quantity of the stock was negativ or zero, so result must be the cost
                    if (newCostQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                        result = current.getCost();
                    // the devisor is not zero
                    } else if (divisor.compareTo(BigDecimal.ZERO) != 0) {
                        result = prev.getResult().multiply(newCostQuantity)
                                        .add(current.getCost().multiply(current.getTransactionQuantity()))
                                        .setScale(12, RoundingMode.HALF_UP)
                                        .divide(divisor, RoundingMode.HALF_UP);
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
     * Gets the penultimate4 costing.
     *
     * @param _currencyInstance the currency instance
     * @param _costingInstance instance the penultimate instance is wanted for
     * @return penultimate instance
     * @throws EFapsException on error
     */
    protected Instance getPenultimate4Costing(final Instance _currencyInstance,
                                              final Instance _costingInstance)
        throws EFapsException
    {
        Costing_Base.LOG.debug("Searching Penultimate for {}", _costingInstance);
        Instance ret = null;
        Instance transInstance = null;

        final TransCosting transCosting = getTransCosting().setCostingInstance(_costingInstance);

        // in general we do not want the one which are not "UpToDate"
        final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.CostingAbstract);
        attrQueryBldr.addWhereAttrEqValue(CIProducts.CostingAbstract.CurrencyLink, _currencyInstance);
        attrQueryBldr.addWhereAttrEqValue(CIProducts.CostingAbstract.UpToDate, false);
        final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(
                        CIProducts.CostingAbstract.TransactionAbstractLink);

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
            final TransCosting transCostingRet = getTransCosting()
                            .setTransactionInstance(transInstance)
                            .setCurrencyInstance(_currencyInstance);
            final Instance retTmp = transCostingRet.getCostingInstance();
            if (retTmp != null) {
                ret = retTmp;
            }
        }
        Costing_Base.LOG.debug("Found Penultimate: {}", ret);
        return ret;
    }

    /**
     * Gets the alternative costings for a currency.
     *
     * @param _parameter the _parameter
     * @param _alterCurrencyInstance the alternative currency instance
     * @param _currencyInstance the _currency instance
     * @param _transactionInstances the _transaction instances
     * @return the costings4 currency
     * @throws EFapsException on error
     */
    protected static Map<Instance, CostingInfo> getAlternativeCostings4Currency(final Parameter _parameter,
                                                                                final Instance _alterCurrencyInstance,
                                                                                final Instance _currencyInstance,
                                                                                final Instance... _transactionInstances)
        throws EFapsException
    {
        return Costing.getAlternativeCostings4Currency(_parameter, new DateTime(), _alterCurrencyInstance,
                        _currencyInstance, _transactionInstances);
    }

    /**
     * Gets the alternative costings for a currency.
     *
     * @param _parameter the _parameter
     * @param _date the date
     * @param _alterCurrencyInstance the alternative currency instance
     * @param _currencyInstance the _currency instance
     * @param _transactionInstances the _transaction instances
     * @return the costings4 currency
     * @throws EFapsException on error
     */
    protected static Map<Instance, CostingInfo> getAlternativeCostings4Currency(final Parameter _parameter,
                                                                                final DateTime _date,
                                                                                final Instance _alterCurrencyInstance,
                                                                                final Instance _currencyInstance,
                                                                                final Instance... _transactionInstances)
        throws EFapsException
    {
        return Costing_Base.getCostings4Currency(_parameter, _date, _alterCurrencyInstance,
                        _currencyInstance, _transactionInstances);
    }

    /**
     * Gets the costings4 currency.
     *
     * @param _parameter the _parameter
     * @param _currencyInstance the _currency instance
     * @param _transactionInstances the _transaction instances
     * @return the costings4 currency
     * @throws EFapsException on error
     */
    protected static Map<Instance, CostingInfo> getCostings4Currency(final Parameter _parameter,
                                                                     final Instance _currencyInstance,
                                                                     final Instance... _transactionInstances)
        throws EFapsException
    {
        return Costing.getCostings4Currency(_parameter, new DateTime(), _currencyInstance, _transactionInstances);
    }

    /**
     * Gets the costings4 currency.
     *
     * @param _parameter the _parameter
     * @param _date the _date
     * @param _currencyInstance the _currency instance
     * @param _transactionInstances the _transaction instances
     * @return the costings4 currency
     * @throws EFapsException on error
     */
    protected static Map<Instance, CostingInfo> getCostings4Currency(final Parameter _parameter,
                                                                     final DateTime _date,
                                                                     final Instance _currencyInstance,
                                                                     final Instance... _transactionInstances)
        throws EFapsException
    {

        return Costing_Base.getCostings4Currency(_parameter, _date, null, _currencyInstance, _transactionInstances);
    }

    /**
     * Gets the costings4 currency.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _date the date
     * @param _alterCurrencyInstance the alter currency instance
     * @param _currencyInstance the currency instance
     * @param _transactionInstances the transaction instances
     * @return the costings4 currency
     * @throws EFapsException on error
     */
    protected static Map<Instance, CostingInfo> getCostings4Currency(final Parameter _parameter,
                                                                     final DateTime _date,
                                                                     final Instance _alterCurrencyInstance,
                                                                     final Instance _currencyInstance,
                                                                     final Instance... _transactionInstances)
        throws EFapsException
    {
        final Map<Instance, CostingInfo> ret = new HashMap<>();
        final QueryBuilder query;
        if (_alterCurrencyInstance != null && _alterCurrencyInstance.isValid()) {
            query = new QueryBuilder(CIProducts.CostingAlternative);
            query.addWhereAttrEqValue(CIProducts.CostingAlternative.CurrencyLink, _alterCurrencyInstance);
        } else {
            query = new QueryBuilder(CIProducts.Costing);
        }

        query.addWhereAttrEqValue(CIProducts.CostingAbstract.TransactionAbstractLink, (Object[]) _transactionInstances);

        final MultiPrintQuery multi = query.getPrint();
        final SelectBuilder selTransInst = SelectBuilder.get()
                        .linkto(CIProducts.CostingAbstract.TransactionAbstractLink).instance();
        final SelectBuilder selCurrInst = SelectBuilder.get()
                        .linkto(CIProducts.CostingAbstract.CurrencyLink).instance();
        multi.addSelect(selTransInst, selCurrInst);
        multi.addAttribute(CIProducts.CostingAbstract.Result, CIProducts.CostingAbstract.Cost,
                        CIProducts.CostingAbstract.Quantity);
        multi.execute();
        while (multi.next()) {
            final Instance transactionInst = multi.getSelect(selTransInst);
            final Instance currencyInst = multi.getSelect(selCurrInst);
            final BigDecimal quantity = multi.getAttribute(CIProducts.CostingAbstract.Quantity);
            final BigDecimal cost = multi.getAttribute(CIProducts.CostingAbstract.Cost);
            final BigDecimal result = multi.getAttribute(CIProducts.CostingAbstract.Result);
            // no rate conversion needed if not an alternative and the base currency is wanted or
            // if alternative and the wanted is equal as the currency for the transaction
            if (_alterCurrencyInstance == null && _currencyInstance.equals(Currency.getBaseCurrency())
                            || _alterCurrencyInstance != null && _currencyInstance.equals(currencyInst)) {
                final CostingInfo info = new CostingInfo().setQuantity(quantity).setCost(cost).setResult(result);
                ret.put(transactionInst, info);
            } else {
                final RateInfo[] rateInfos = new Currency().evaluateRateInfos(_parameter, _date, currencyInst,
                                _currencyInstance);
                final RateInfo rateInfo = rateInfos[2];
                final CostingInfo info = new CostingInfo().setQuantity(quantity)
                                .setCost(cost.setScale(8, BigDecimal.ROUND_HALF_UP)
                                                .divide(rateInfo.getRate(), BigDecimal.ROUND_HALF_UP))
                                .setResult(result.setScale(8, BigDecimal.ROUND_HALF_UP)
                                                .divide(rateInfo.getRate(), BigDecimal.ROUND_HALF_UP));
                ret.put(transactionInst, info);
            }
        }
        return ret;
    }

    /**
     * Gets the trans costing.
     *
     * @return new TransCosting instance
     * @throws EFapsException on error
     */
    protected TransCosting getTransCosting()
        throws EFapsException
    {
        return new TransCosting();
    }

    /**
     * Creates the historic cost. To be executed using the console to generate
     * historic data.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException the eFaps exception
     */
    public Return createHistoricCost(final Parameter _parameter)
        throws EFapsException
    {
        // Admin_Program_Java
        final String[] keyFields = _parameter.getParameterValues("keyField");
        final String[] valueFields = _parameter.getParameterValues("valueField");
        if (ArrayUtils.isEmpty(keyFields) || ArrayUtils.isEmpty(valueFields)) {
            LOG.error("Missing Parameter 'CurrencyOID'");
            throw new EFapsException(Costing.class, "MissingParameter");
        } else {
            String currencyOID = "";
            for (int i = 0; i < keyFields.length; i++) {
                switch (keyFields[i]) {
                    case "CurrencyOID":
                        currencyOID = valueFields[i];
                        break;

                    default:
                        break;
                }
            }
            final Instance currencyInstance = Instance.get(currencyOID);
            if (!currencyInstance.isValid()) {
                LOG.error("Missing Parameter 'CurrencyOID'");
                throw new EFapsException(Costing.class, "MissingParameter");
            }
            final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.TransactionInbound);
            attrQueryBldr.addType(CIProducts.TransactionInbound4StaticStorage);

            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.CostingAbstract);
            queryBldr.addWhereAttrEqValue(CIProducts.CostingAbstract.CurrencyLink, currencyInstance);
            queryBldr.addWhereAttrInQuery(CIProducts.CostingAbstract.TransactionAbstractLink,
                            attrQueryBldr.getAttributeQuery(CIProducts.TransactionInbound.ID));
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selTrans = SelectBuilder.get()
                            .linkto(CIProducts.CostingAbstract.TransactionAbstractLink);
            final SelectBuilder selTransDate = new SelectBuilder(selTrans)
                            .attribute(CIProducts.TransactionAbstract.Date);
            final SelectBuilder selTransProdInst = new SelectBuilder(selTrans)
                            .linkto(CIProducts.TransactionAbstract.Product).instance();
            multi.addSelect(selTransDate, selTransProdInst);
            multi.addAttribute(CIProducts.CostingAbstract.Result);
            multi.executeWithoutAccessCheck();
            final List<TransCosting> costings = new ArrayList<>();
            CIType costType = null;
            while (multi.next()) {
                if (costType == null) {
                    costType = multi.getCurrentInstance().getType().isCIType(CIProducts.Costing)
                                    ? CIProducts.ProductCost : CIProducts.ProductCostAlternative;
                }
                costings.add(getTransCosting()
                                .setCurrencyInstance(currencyInstance)
                                .setResult(multi.<BigDecimal>getAttribute(CIProducts.CostingAbstract.Result))
                                .setDate(multi.<DateTime>getSelect(selTransDate))
                                .setProductInstance(multi.<Instance>getSelect(selTransProdInst)));
            }

            Collections.sort(costings, new Comparator<TransCosting>() {

                @Override
                public int compare(final TransCosting _arg0,
                                   final TransCosting _arg1)
                {
                    int ret = 0;
                    try {
                        ret  =  _arg0.getDate().compareTo(_arg1.getDate());
                    } catch (final EFapsException e) {
                        LOG.error("Catched EFapsException", e);
                    }
                    return ret;
                }});

            for (final TransCosting transCost : costings) {
                final QueryBuilder costBldr = new QueryBuilder(costType);
                costBldr.addWhereAttrEqValue(CIProducts.ProductCostAbstract.ProductLink,
                                transCost.getProductInstance());
                costBldr.addWhereAttrGreaterValue(CIProducts.ProductCostAbstract.ValidUntil,
                                transCost.getDate().withTimeAtStartOfDay().minusMinutes(1));
                costBldr.addWhereAttrLessValue(CIProducts.ProductCostAbstract.ValidFrom,
                                transCost.getDate().withTimeAtStartOfDay().plusMinutes(1));
                if (costType.equals(CIProducts.ProductCostAlternative)) {
                    costBldr.addWhereAttrEqValue(CIProducts.ProductCostAlternative.CurrencyLink, currencyInstance);
                }
                BigDecimal currPrice = BigDecimal.ZERO;
                final MultiPrintQuery costMulti = costBldr.getPrint();
                costMulti.addAttribute(CIProducts.ProductCostAbstract.Price);
                costMulti.executeWithoutAccessCheck();
                if (costMulti.next()) {
                    currPrice = costMulti.<BigDecimal>getAttribute(CIProducts.ProductCostAbstract.Price);
                }
                if (transCost.getResult().compareTo(BigDecimal.ZERO) != 0 && currPrice.compareTo(transCost.getResult()
                                .setScale(currPrice.scale(), RoundingMode.HALF_UP)) != 0) {
                    Costing_Base.LOG.debug(" Updating Cost for: {}", transCost);
                    final Insert insert = new Insert(costType);
                    insert.add(CIProducts.ProductCostAbstract.ProductLink, transCost.getProductInstance());
                    insert.add(CIProducts.ProductCostAbstract.Price, transCost.getResult());
                    insert.add(CIProducts.ProductCostAbstract.ValidFrom, transCost.getDate().withTimeAtStartOfDay());
                    insert.add(CIProducts.ProductCostAbstract.ValidUntil,
                                    transCost.getDate().plusYears(10).withTimeAtStartOfDay());
                    insert.add(CIProducts.ProductCostAbstract.CurrencyLink, currencyInstance);
                    insert.executeWithoutAccessCheck();
                }
            }
        }
        return new Return();
    }

    /**
     * The Class CostingInfo.
     */
    public static class CostingInfo
    {
        /**
         * The Costing Instance.
         */
        private Instance instance;

        /**
         * Cost defined by the Costing.
         */
        private BigDecimal cost;

        /**
         * The calculated result of Costing.
         */
        private BigDecimal result;

        /**
         * Quantity of the costing.
         */
        private BigDecimal quantity;

        /**
         * Getter method for the instance variable {@link #instance}.
         *
         * @return value of instance variable {@link #instance}
         */
        public Instance getInstance()
        {
            return this.instance;
        }

        /**
         * Setter method for instance variable {@link #instance}.
         *
         * @param _instance value for instance variable {@link #instance}
         * @return the costing info
         */
        public CostingInfo setInstance(final Instance _instance)
        {
            this.instance = _instance;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #cost}.
         *
         * @return value of instance variable {@link #cost}
         */
        public BigDecimal getCost()
        {
            return this.cost;
        }

        /**
         * Setter method for instance variable {@link #cost}.
         *
         * @param _cost value for instance variable {@link #cost}
         * @return the costing info
         */
        public CostingInfo setCost(final BigDecimal _cost)
        {
            this.cost = _cost;
            return this;
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
         * @return the costing info
         */
        public CostingInfo setResult(final BigDecimal _result)
        {
            this.result = _result;
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
         * @return the costing info
         */
        public CostingInfo setQuantity(final BigDecimal _quantity)
        {
            this.quantity = _quantity;
            return this;
        }
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
         * Instance of the currency.
         */
        private Instance currencyInstance;

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
         * Inits the transaction.
         *
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
         * Inits the costing.
         *
         * @throws EFapsException on error
         */
        protected void initCosting()
            throws EFapsException
        {
            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.CostingAbstract);
            queryBldr.addWhereAttrEqValue(CIProducts.CostingAbstract.CurrencyLink, getCurrencyInstance());
            queryBldr.addWhereAttrEqValue(CIProducts.CostingAbstract.TransactionAbstractLink, getTransactionInstance());
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CIProducts.Costing.Cost, CIProducts.Costing.Quantity, CIProducts.Costing.Result,
                            CIProducts.Costing.UpToDate);
            multi.executeWithoutAccessCheck();
            if (multi.next()) {
                this.upToDate = multi.getAttribute(CIProducts.Costing.UpToDate);
                this.cost = multi.getAttribute(CIProducts.Costing.Cost);
                this.costingQuantity = multi.getAttribute(CIProducts.Costing.Quantity);
                this.result = multi.getAttribute(CIProducts.Costing.Result);
                this.costingInstance = multi.getCurrentInstance();
            }
        }

        /**
         * Update the Costing Instance.
         *
         * @return the trans costing
         * @throws EFapsException on error
         */
        public TransCosting updateCosting()
            throws EFapsException
        {
            final Update update = new Update(getCostingInstance());
            update.add(CIProducts.CostingAbstract.Cost, getCost());
            update.add(CIProducts.CostingAbstract.Quantity, getCostingQuantity());
            update.add(CIProducts.CostingAbstract.Result, getResult());
            update.add(CIProducts.CostingAbstract.UpToDate, true);
            update.add(CIProducts.CostingAbstract.CurrencyLink, getCurrencyInstance());
            update.executeWithoutAccessCheck();
            setUpToDate(true);
            return this;
        }

        /**
         * Insert a new Costing Instance.
         *
         * @return the trans costing
         * @throws EFapsException on error
         */
        public TransCosting insertCosting()
            throws EFapsException
        {
            final BigDecimal costTmp;
            if (isOutBound()) {
                costTmp = BigDecimal.ZERO;
            } else {
                costTmp = getCostFromDocument();
            }
            final Insert insert = new Insert(Currency.getBaseCurrency().equals(getCurrencyInstance())
                            ? CIProducts.Costing : CIProducts.CostingAlternative);
            insert.add(CIProducts.CostingAbstract.Quantity, getCostingQuantity());
            insert.add(CIProducts.CostingAbstract.TransactionAbstractLink, getTransactionInstance());
            insert.add(CIProducts.CostingAbstract.Cost, costTmp);
            insert.add(CIProducts.CostingAbstract.Result, costTmp);
            insert.add(CIProducts.CostingAbstract.UpToDate, isUpToDate());
            insert.add(CIProducts.CostingAbstract.CurrencyLink, getCurrencyInstance());
            insert.executeWithoutTrigger();

            setCost(costTmp);
            setResult(costTmp);
            setCostingInstance(insert.getInstance());
            return this;
        }

        /**
         * Sets the costing instance.
         *
         * @param _costingInstance instance to be set
         * @return the trans costing
         */
        public TransCosting setCostingInstance(final Instance _costingInstance)
        {
            this.costingInstance = _costingInstance;
            return this;
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
         * @return the trans costing
         */
        public TransCosting setTransactionInstance(final Instance _transactionInstance)
        {
            this.transactionInstance = _transactionInstance;
            return this;
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
         * @return the trans costing
         */
        public TransCosting setProductInstance(final Instance _productInstance)
        {
            this.productInstance = _productInstance;
            return this;
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
         * @return the trans costing
         */
        public TransCosting setDate(final DateTime _date)
        {
            this.date = _date;
            return this;
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
            final BigDecimal ret;
            if (this.transactionInstance.getType().isKindOf(CIProducts.TransactionOutbound.getType())) {
                ret = this.transactionQuantity.negate();
            } else {
                ret = this.transactionQuantity;
            }
            return ret;
        }

        /**
         * Checks if is out bound.
         *
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
         * @return the trans costing
         */
        public TransCosting setTransactionQuantity(final BigDecimal _transactionQuantity)
        {
            this.transactionQuantity = _transactionQuantity;
            return this;
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
         * Gets the quantity.
         *
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
         * @return the trans costing
         */
        public TransCosting setCostingQuantity(final BigDecimal _costingQuantity)
        {
            this.costingQuantity = _costingQuantity;
            return this;
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
         * Gets the cost from document.
         *
         * @return the Cost to be used for Costing retrieved by analyzing the related Documents
         * @throws EFapsException on error
         */
        public BigDecimal getCostFromDocument()
            throws EFapsException
        {
            Costing_Base.LOG.debug("Analysing Cost From Document for: {}", this);
            BigDecimal ret = null;
            // this is the transaction that inits a storage therefore search in
            // cost table
            if (getTransactionInstance().getType().isCIType(CIProducts.TransactionInbound4StaticStorage)) {
                final PrintQuery print = new PrintQuery(getTransactionInstance());
                print.addAttribute(CIProducts.TransactionAbstract.Date);
                print.executeWithoutAccessCheck();
                ret = Cost.getCost4Currency(new Parameter(),
                                print.<DateTime>getAttribute(CIProducts.TransactionAbstract.Date),
                                getProductInstance(), getCurrencyInstance());
            } else {
                final Instance docInstTmp = getDocInstance();
                if (docInstTmp != null && docInstTmp.isValid()) {
                    final CostDoc costDoc = new CostDoc(docInstTmp, getProductInstance());
                    ret = costDoc.getCost(new Parameter(), getCurrencyInstance());
                }
            }
            Costing_Base.LOG.debug("Result: {} for  Cost From Document for: {}", ret, this);
            return ret == null ? BigDecimal.ZERO : ret;
        }

        /**
         * Setter method for instance variable {@link #cost}.
         *
         * @param _cost value for instance variable {@link #cost}
         * @return the trans costing
         */
        public TransCosting setCost(final BigDecimal _cost)
        {
            this.cost = _cost;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #result}.
         *
         * @return value of instance variable {@link #result}
         */
        public BigDecimal getResult()
        {
            final BigDecimal ret;
            if (BigDecimal.ZERO.compareTo(this.result) > 0) {
                ret = BigDecimal.ZERO;
            } else {
                ret = this.result;
            }
            return ret;
        }

        /**
         * Setter method for instance variable {@link #result}.
         *
         * @param _result value for instance variable {@link #result}
         * @return the trans costing
         */
        public TransCosting setResult(final BigDecimal _result)
        {
            this.result = _result;
            return this;
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
         * @return the trans costing
         */
        public TransCosting setDocInstance(final Instance _docInstance)
        {
            this.docInstance = _docInstance;
            return this;
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
         * @return the trans costing
         */
        public TransCosting setUpToDate(final boolean _upToDate)
        {
            this.upToDate = _upToDate;
            return this;
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
         * @return the trans costing
         */
        public TransCosting setPosition(final Integer _position)
        {
            this.position = _position;
            return this;
        }

        /**
         * Gets the instance of the currency.
         *
         * @return the instance of the currency
         */
        public Instance getCurrencyInstance()
        {
            return this.currencyInstance;
        }

        /**
         * Sets the currency instance.
         *
         * @param _currencyInstance the currency instance
         * @return the trans costing
         */
        public TransCosting setCurrencyInstance(final Instance _currencyInstance)
        {
            this.currencyInstance = _currencyInstance;
            return this;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString()
        {
            return ToStringBuilder.reflectionToString(this);
        }
    }

    /**
     * The Class CostDoc.
     */
    public static class CostDoc
    {

        /**
         * BigDecimal cost.
         */
        private BigDecimal cost = BigDecimal.ZERO;

        /**
         * BigDecimal rate cost.
         */
        private BigDecimal rateCost = BigDecimal.ZERO;

        /** The rate currency instance. */
        private Instance rateCurrencyInstance;

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
         * Instantiates a new cost doc.
         *
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
         * Inits the.
         *
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
                final SelectBuilder rateCurrencyInstSel = SelectBuilder.get().linkto(
                                CISales.PositionSumAbstract.RateCurrencyId).instance();
                final SelectBuilder docStatusSel = SelectBuilder.get().linkto(
                                CISales.PositionAbstract.DocumentAbstractLink).status();
                if (CISales.RecievingTicket.getType().equals(getBaseDocInst().getType())) {
                    // first priority are the special relation for costing
                    // "Sales_AcquisitionCosting2RecievingTicket"
                    final QueryBuilder acRelAttrQueryBldr = new QueryBuilder(
                                    CISales.AcquisitionCosting2RecievingTicket);
                    acRelAttrQueryBldr.addWhereAttrEqValue(CISales.AcquisitionCosting2RecievingTicket.ToLink,
                                    getBaseDocInst());
                    final AttributeQuery acRelAttrQuery = acRelAttrQueryBldr
                                    .getAttributeQuery(CISales.AcquisitionCosting2RecievingTicket.FromLink);
                    final QueryBuilder acPosQueryBldr = new QueryBuilder(CISales.AcquisitionCostingPosition);
                    acPosQueryBldr.addWhereAttrInQuery(CISales.AcquisitionCostingPosition.DocumentAbstractLink,
                                    acRelAttrQuery);
                    acPosQueryBldr.addWhereAttrEqValue(CISales.AcquisitionCostingPosition.Product, getProductInst());
                    final MultiPrintQuery acPosMulti = acPosQueryBldr.getPrint();
                    acPosMulti.addSelect(docInstSel, docStatusSel, rateCurrencyInstSel);
                    acPosMulti.addAttribute(CISales.AcquisitionCostingPosition.NetUnitPrice,
                                    CISales.AcquisitionCostingPosition.RateNetUnitPrice);
                    acPosMulti.execute();
                    while (acPosMulti.next() && !found) {
                        if (validStatus(acPosMulti.<Status>getSelect(docStatusSel))) {
                            found = true;
                            setCost(acPosMulti
                                            .<BigDecimal>getAttribute(CISales.AcquisitionCostingPosition.NetUnitPrice));
                            setRateCost(acPosMulti.<BigDecimal>getAttribute(
                                            CISales.AcquisitionCostingPosition.RateNetUnitPrice));
                            setRateCurrencyInstance(acPosMulti.<Instance>getSelect(rateCurrencyInstSel));
                            setCostDocInst(acPosMulti.<Instance>getSelect(docInstSel));
                        }
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
                        posMulti.addSelect(docInstSel, docStatusSel, rateCurrencyInstSel);
                        posMulti.addAttribute(CISales.IncomingInvoicePosition.NetUnitPrice,
                                        CISales.IncomingInvoicePosition.RateNetUnitPrice);
                        posMulti.execute();
                        while (posMulti.next() && !found) {
                            if (validStatus(posMulti.<Status>getSelect(docStatusSel))) {
                                found = true;
                                setCost(posMulti.<BigDecimal>getAttribute(
                                                CISales.IncomingInvoicePosition.NetUnitPrice));
                                setRateCost(posMulti.<BigDecimal>getAttribute(
                                                CISales.IncomingInvoicePosition.RateNetUnitPrice));
                                setRateCurrencyInstance(posMulti.<Instance>getSelect(rateCurrencyInstSel));
                                setCostDocInst(posMulti.<Instance>getSelect(docInstSel));
                            }
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
                        multi.addSelect(docInstSel, docStatusSel, rateCurrencyInstSel);
                        multi.addAttribute(CISales.IncomingInvoicePosition.NetUnitPrice,
                                        CISales.IncomingInvoicePosition.RateNetUnitPrice);
                        multi.execute();
                        while (multi.next() && !found) {
                            if (validStatus(multi.<Status>getSelect(docStatusSel))) {
                                setCost(multi.<BigDecimal>getAttribute(CISales.IncomingInvoicePosition.NetUnitPrice));
                                setRateCost(multi.<BigDecimal>getAttribute(
                                                CISales.IncomingInvoicePosition.RateNetUnitPrice));
                                setRateCurrencyInstance(multi.<Instance>getSelect(rateCurrencyInstSel));
                                setCostDocInst(multi.<Instance>getSelect(docInstSel));
                                found = true;
                            }
                        }
                    }
                    // if activated use the OrderOutbound as last chance
                    if (!found && Sales.COSTINGOO4RT.get()) {
                        final QueryBuilder relAttrQueryBldr = new QueryBuilder(CISales.OrderOutbound2RecievingTicket);
                        relAttrQueryBldr.addWhereAttrEqValue(CISales.OrderOutbound2RecievingTicket.ToLink,
                                        getBaseDocInst());
                        final AttributeQuery relAttrQuery = relAttrQueryBldr
                                        .getAttributeQuery(CISales.OrderOutbound2RecievingTicket.FromLink);
                        final QueryBuilder posQueryBldr = new QueryBuilder(CISales.OrderOutboundPosition);
                        posQueryBldr.addWhereAttrInQuery(CISales.OrderOutboundPosition.DocumentAbstractLink,
                                        relAttrQuery);
                        posQueryBldr.addWhereAttrEqValue(CISales.OrderOutboundPosition.Product, getProductInst());
                        final MultiPrintQuery posMulti = posQueryBldr.getPrint();
                        posMulti.addSelect(docInstSel, docStatusSel, rateCurrencyInstSel);
                        posMulti.addAttribute(CISales.OrderOutboundPosition.NetUnitPrice,
                                        CISales.IncomingInvoicePosition.RateNetUnitPrice);
                        posMulti.execute();
                        while (posMulti.next() && !found) {
                            if (validStatus(posMulti.<Status>getSelect(docStatusSel))) {
                                found = true;
                                setCost(posMulti.<BigDecimal>getAttribute(
                                                CISales.OrderOutboundPosition.NetUnitPrice));
                                setRateCost(posMulti.<BigDecimal>getAttribute(
                                                CISales.OrderOutboundPosition.RateNetUnitPrice));
                                setRateCurrencyInstance(posMulti.<Instance>getSelect(rateCurrencyInstSel));
                                setCostDocInst(posMulti.<Instance>getSelect(docInstSel));
                            }
                        }
                    }
                } else if (CISales.IncomingInvoice.getType().equals(getBaseDocInst().getType())) {
                    final QueryBuilder queryBldr = new QueryBuilder(CISales.IncomingInvoicePosition);
                    queryBldr.addWhereAttrEqValue(CISales.IncomingInvoicePosition.DocumentAbstractLink,
                                    getBaseDocInst());
                    queryBldr.addWhereAttrEqValue(CISales.IncomingInvoicePosition.Product, getProductInst());
                    final MultiPrintQuery multi = queryBldr.getPrint();
                    multi.addSelect(docInstSel, docStatusSel, rateCurrencyInstSel);
                    multi.addAttribute(CISales.IncomingInvoicePosition.NetUnitPrice,
                                    CISales.IncomingInvoicePosition.RateNetUnitPrice);
                    multi.execute();
                    while (multi.next() && !found) {
                        if (validStatus(multi.<Status>getSelect(docStatusSel))) {
                            setCost(multi.<BigDecimal>getAttribute(CISales.IncomingInvoicePosition.NetUnitPrice));
                            setRateCost(multi.<BigDecimal>getAttribute(
                                            CISales.IncomingInvoicePosition.RateNetUnitPrice));
                            setRateCurrencyInstance(multi.<Instance>getSelect(rateCurrencyInstSel));
                            setCostDocInst(multi.<Instance>getSelect(docInstSel));
                            found = true;
                        }
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
                    posMulti.addSelect(docInstSel, docStatusSel, rateCurrencyInstSel);
                    posMulti.addAttribute(CISales.PositionSumAbstract.NetUnitPrice,
                                    CISales.PositionSumAbstract.RateNetUnitPrice);
                    posMulti.execute();
                    while (posMulti.next() && !found) {
                        if (validStatus(posMulti.<Status>getSelect(docStatusSel))) {
                            found = true;
                            setCost(posMulti.<BigDecimal>getAttribute(CISales.IncomingInvoicePosition.NetUnitPrice));
                            setRateCost(posMulti.<BigDecimal>getAttribute(
                                            CISales.PositionSumAbstract.RateNetUnitPrice));
                            setRateCurrencyInstance(posMulti.<Instance>getSelect(rateCurrencyInstSel));
                            setCostDocInst(posMulti.<Instance>getSelect(docInstSel));
                            found = true;
                        }
                    }
                } else if (CISales.ProductionReport.getType().equals(getBaseDocInst().getType())) {
                    final QueryBuilder relAttrQueryBldr = new QueryBuilder(CISales.ProductionCosting2ProductionReport);
                    relAttrQueryBldr.addWhereAttrEqValue(CISales.Document2DocumentAbstract.ToAbstractLink,
                                    getBaseDocInst());
                    final QueryBuilder posQueryBldr = new QueryBuilder(CISales.PositionSumAbstract);
                    posQueryBldr.addWhereAttrInQuery(CISales.PositionSumAbstract.DocumentAbstractLink,
                                relAttrQueryBldr.getAttributeQuery(CISales.Document2DocumentAbstract.FromAbstractLink));
                    posQueryBldr.addWhereAttrEqValue(CISales.PositionSumAbstract.Product, getProductInst());
                    final MultiPrintQuery posMulti = posQueryBldr.getPrint();
                    posMulti.addSelect(docInstSel, docStatusSel, rateCurrencyInstSel);
                    posMulti.addAttribute(CISales.PositionSumAbstract.NetUnitPrice,
                                    CISales.PositionSumAbstract.RateNetUnitPrice);
                    posMulti.execute();
                    while (posMulti.next() && !found) {
                        if (validStatus(posMulti.<Status>getSelect(docStatusSel))) {
                            found = true;
                            setCost(posMulti.<BigDecimal>getAttribute(CISales.PositionSumAbstract.NetUnitPrice));
                            setRateCost(posMulti.<BigDecimal>getAttribute(
                                            CISales.PositionSumAbstract.RateNetUnitPrice));
                            setRateCurrencyInstance(posMulti.<Instance>getSelect(rateCurrencyInstSel));
                            setCostDocInst(posMulti.<Instance>getSelect(docInstSel));
                            found = true;
                        }
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
         * Valid status.
         *
         * @param _status the status
         * @return true, if successful
         * @throws CacheReloadException the cache reload exception
         */
        protected boolean validStatus(final Status _status)
            throws CacheReloadException
        {
            return !Status.find(CISales.IncomingInvoiceStatus.Replaced).equals(_status)
                            && !Status.find(CISales.AcquisitionCostingStatus.Canceled).equals(_status)
                            && !Status.find(CISales.ProductionCostingStatus.Canceled).equals(_status)
                            && !Status.find(CISales.OrderOutboundStatus.Canceled).equals(_status);
        }

        /**
         * Analyze.
         *
         * @return boolean (true/false).
         * @throws EFapsException on error.
         */
        protected boolean analyze()
            throws EFapsException
        {
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
         * @param _parameter Parameter as passed by the eFaps API
         * @param _currencyInst the currency inst
         * @return value of instance variable {@link #cost}.
         * @throws EFapsException on error.
         */
        public BigDecimal getCost(final Parameter _parameter,
                                  final Instance _currencyInst)
            throws EFapsException
        {
            return getCost(_parameter, _currencyInst, null);
        }

        /**
         * Getter method for the instance variable {@link #cost}.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _currencyInst the currency inst
         * @param _date the date
         * @return value of instance variable {@link #cost}.
         * @throws EFapsException on error.
         */
        public BigDecimal getCost(final Parameter _parameter,
                                  final Instance _currencyInst,
                                  final DateTime _date)
            throws EFapsException
        {
            BigDecimal ret = null;
            if (Currency.getBaseCurrency().equals(_currencyInst)) {
                ret = getCost();
            } else if (getRateCurrencyInstance() != null && getRateCurrencyInstance().equals(_currencyInst)) {
                ret = getRateCost();
            } else if (getCost() != null && getCost().compareTo(BigDecimal.ZERO) != 0) {
                final DateTime date;
                if (_date == null) {
                    final PrintQuery print = new PrintQuery(getCostDocInst());
                    print.addAttribute(CIERP.DocumentAbstract.Date);
                    print.executeWithoutAccessCheck();
                    date = print.<DateTime>getAttribute(CIERP.DocumentAbstract.Date);
                } else {
                    date = _date;
                }
                final RateInfo rateInfo = new Currency().evaluateRateInfo(_parameter, date, _currencyInst);
                ret = getCost().multiply(rateInfo.getRate());
            }
            return ret == null ? BigDecimal.ZERO : ret;
        }

        /**
         * Getter method for the instance variable {@link #docInst}.
         *
         * @return value of instance variable {@link #docInst}
         * @throws EFapsException on error
         */
        public Instance getBaseDocInst()
            throws EFapsException
        {
            init();
            return this.baseDocInst;
        }

        /**
         * Setter method for instance variable {@link #docInst}.
         *
         * @param _docInst value for instance variable {@link #docInst}
         * @return the cost doc
         */
        public CostDoc setBaseDocInst(final Instance _docInst)
        {
            this.baseDocInst = _docInst;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #productInst}.
         *
         * @return value of instance variable {@link #productInst}
         * @throws EFapsException on error
         */
        public Instance getProductInst()
            throws EFapsException
        {
            init();
            return this.productInst;
        }

        /**
         * Setter method for instance variable {@link #productInst}.
         *
         * @param _productInst value for instance variable {@link #productInst}
         * @return the cost doc
         */
        public CostDoc setProductInst(final Instance _productInst)
        {
            this.productInst = _productInst;
            return this;
        }

        /**
         * Setter method for instance variable {@link #cost}.
         *
         * @param _cost value for instance variable {@link #cost}
         * @return the cost doc
         */
        public CostDoc setCost(final BigDecimal _cost)
        {
            this.cost = _cost;
            return this;
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
         * @return the cost doc
         */
        public CostDoc setCostDocInst(final Instance _costDocInst)
        {
            this.costDocInst = _costDocInst;
            return this;
        }

        /**
         * Gets the bigDecimal rate cost.
         *
         * @return the bigDecimal rate cost
         * @throws EFapsException on error
         */
        public BigDecimal getRateCost()
            throws EFapsException
        {
            init();
            return this.rateCost;
        }

        /**
         * Sets the rate cost.
         *
         * @param _rateCost the rate cost
         * @return the cost doc
         */
        public CostDoc setRateCost(final BigDecimal _rateCost)
        {
            this.rateCost = _rateCost;
            return this;
        }

        /**
         * Gets the rate currency instance.
         *
         * @return the rate currency instance
         * @throws EFapsException on error
         */
        public Instance getRateCurrencyInstance()
            throws EFapsException
        {
            init();
            return this.rateCurrencyInstance;
        }

        /**
         * Sets the rate currency instance.
         *
         * @param _rateCurrencyInstance the rate currency instance
         * @return the cost doc
         */
        public CostDoc setRateCurrencyInstance(final Instance _rateCurrencyInstance)
        {
            this.rateCurrencyInstance = _rateCurrencyInstance;
            return this;
        }
    }
}
