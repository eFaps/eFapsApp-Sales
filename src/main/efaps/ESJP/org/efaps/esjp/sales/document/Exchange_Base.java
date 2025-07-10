/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
 */
package org.efaps.esjp.sales.document;

import java.io.File;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.ci.CITableSales;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.erp.AbstractWarning;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.IWarning;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.Swap;
import org.efaps.esjp.sales.payment.DocPaymentInfo;
import org.efaps.esjp.sales.payment.DocumentUpdate;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.ui.wicket.util.DateUtil;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author The eFaps Team
 */
@EFapsUUID("237aaa91-4c18-413c-9718-268deb312702")
@EFapsApplication("eFapsApp-Sales")
public abstract class Exchange_Base
    extends AbstractSumOnlyDocument
{

    /**
     * Logger for this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Exchange.class);

    /**
     * Method for create a new Quotation.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final CreatedDoc createdDoc = createDoc(_parameter);
        connect2Object(_parameter, createdDoc);
        final File file = createReport(_parameter, createdDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        ret.put(ReturnValues.INSTANCE, createdDoc.getInstance());
        return ret;
    }

    @Override
    protected void add2DocCreate(final Parameter _parameter,
                                 final Insert _insert,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        super.add2DocCreate(_parameter, _insert, _createdDoc);
        if (Sales.EXCHANGE_REVSEQ.exists()) {
            final String seqKey = Sales.EXCHANGE_REVSEQ.get();
            final NumberGenerator numgen = isUUID(seqKey)
                                ? NumberGenerator.get(UUID.fromString(seqKey))
                                : NumberGenerator.get(seqKey);
            if (numgen != null) {
                final String revision = numgen.getNextVal();
                Context.getThreadContext().setSessionAttribute(IncomingInvoice_Base.REVISIONKEY, revision);
                _insert.add(CISales.Exchange.Revision, revision);
            }
        }
        add2DocUpdate(_parameter, _insert, _createdDoc);
    }

    /**
     * Method for create a new Quotation.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final EditedDoc editedDoc = editDoc(_parameter);
        updateConnection2Object(_parameter, editedDoc);
        final File file = createReport(_parameter, editedDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    @Override
    protected void add2DocEdit(final Parameter _parameter,
                               final Update _update,
                               final EditedDoc _editDoc)
        throws EFapsException
    {
        super.add2DocEdit(_parameter, _update, _editDoc);
        add2DocUpdate(_parameter, _update, _editDoc);
    }

    /**
     * Add to doc update.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _update the update
     * @param _createdDoc the created doc
     * @throws EFapsException on error
     */
    protected void add2DocUpdate(final Parameter _parameter,
                                 final Update _update,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final String onlynumber = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.Exchange.OnlyNumber.name));
        if (onlynumber != null) {
            _update.add(CISales.Exchange.OnlyNumber, onlynumber);
            _createdDoc.getValues().put(CISales.Exchange.OnlyNumber.name, onlynumber);
        }
    }

    /**
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return updateFields4AddDays(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final List<Map<String, Object>> list = new ArrayList<>();
        final Map<String, Object> map = new HashMap<>();
        final int selected = getSelectedRow(_parameter);

        final String[] addDays = _parameter.getParameterValues("addDays");
        if (ArrayUtils.isNotEmpty(addDays) && addDays.length > selected) {
            final int add = Integer.parseInt(addDays[selected]);
            final String[] dates = _parameter.getParameterValues("date_eFapsDate");
            final DateTime date = DateUtil.getDateFromParameter(dates[selected]);
            map.put("dueDate_eFapsDate", DateUtil.getDate4Parameter(date.plusDays(add)));
            list.add(map);
        }
        ret.put(ReturnValues.VALUES, list);
        return ret;
    }

    /**
     * Methods creates the Exchanges and relates them to the documents.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return createCalculated(final Parameter _parameter)
        throws EFapsException
    {
        final String[] nameAr = _parameter
                        .getParameterValues(CITableSales.Sales_ExchangeCreateCalculatedTable.name4Exchange.name);
        final String[] crossAr = _parameter
                        .getParameterValues(CITableSales.Sales_ExchangeCreateCalculatedTable.crossTotal4Exchange.name);
        final String[] currAr = _parameter
                        .getParameterValues(CITableSales.Sales_ExchangeCreateCalculatedTable.currency4Exchange.name);
        final String[] dateAr = _parameter
                        .getParameterValues(CITableSales.Sales_ExchangeCreateCalculatedTable.date.name);
        final String[] dueDateAr = _parameter
                        .getParameterValues(CITableSales.Sales_ExchangeCreateCalculatedTable.dueDate.name);

        final String[] docOids = _parameter.getParameterValues(CITableSales.Sales_SwapPayTable.document.name);
        final List<Instance> docInsts = new ArrayList<>();
        final Set<CreatedDoc> createdDocs = new HashSet<>();
        if (!ArrayUtils.isEmpty(crossAr) && !ArrayUtils.isEmpty(currAr) && !ArrayUtils.isEmpty(docOids)) {
            try {
                final Map<Instance, BigDecimal> exchangeMap = new LinkedHashMap<>();
                final Map<Instance, Map<Instance, BigDecimal>> exchangeValuesMap = new LinkedHashMap<>();

                for (int i = 0; i < crossAr.length; i++) {
                    final Instance rateCurInst = Instance.get(CIERP.Currency.getType(), currAr[i]);
                    final String rate = new Currency().evaluateRateInfo(_parameter, new DateTime(), rateCurInst)
                                    .getRateFrmt(null);
                    final Parameter parameter = ParameterUtil.clone(_parameter);
                    if (!ArrayUtils.isEmpty(nameAr) && nameAr.length > i) {
                        ParameterUtil.setParameterValues(parameter, "name4create", nameAr[i]);
                    }
                    ParameterUtil.setParameterValues(parameter, "rate", rate);
                    ParameterUtil.setParameterValues(parameter, "rateCrossTotal", crossAr[i]);
                    ParameterUtil.setParameterValues(parameter, "rateCurrencyId", currAr[i]);
                    ParameterUtil.setParameterValues(parameter, getFieldName4Attribute(_parameter,
                                    CISales.DocumentSumAbstract.Date.name), dateAr[i]);
                    ParameterUtil.setParameterValues(parameter, getFieldName4Attribute(_parameter,
                                    CISales.DocumentSumAbstract.DueDate.name), dueDateAr[i]);

                    final CreatedDoc createdDoc = createDoc(parameter);
                    createdDocs.add(createdDoc);
                    exchangeMap.put(createdDoc.getInstance(), (BigDecimal) NumberFormatter.get().getFormatter().parse(
                                    crossAr[i]));
                }
                int start = 0;
                BigDecimal missing = null;
                for (Entry<Instance, BigDecimal> entry : exchangeMap.entrySet()) {
                    BigDecimal currentTotal = BigDecimal.ZERO;
                    for (int i = start; i < docOids.length; i++) {
                        final Instance docInst = Instance.get(docOids[i]);
                        BigDecimal partial = (BigDecimal) NumberFormatter.get().getFormatter()
                                        .parse(_parameter.getParameterValues("partial")[i]);
                        if (missing != null) {
                            partial = partial.subtract(partial.subtract(missing));
                            missing = null;
                        }
                        boolean stop = false;
                        BigDecimal partialTemp = BigDecimal.ZERO;
                        if (entry.getValue().subtract(currentTotal).compareTo(partial) > 0) {
                            partialTemp = partial;
                        } else {
                            partialTemp = entry.getValue().subtract(currentTotal);
                            missing = partial.subtract(partialTemp);
                            start = i;
                            stop = true;
                        }

                        final Map<Instance, BigDecimal> innermap;
                        if (exchangeValuesMap.containsKey(entry.getKey())) {
                            innermap = exchangeValuesMap.get(entry.getKey());
                        } else {
                            innermap = new LinkedHashMap<>();
                            exchangeValuesMap.put(entry.getKey(), innermap);
                        }
                        innermap.put(docInst, partialTemp);
                        currentTotal = currentTotal.add(partialTemp);
                        if (stop) {
                            break;
                        }
                    }
                }

                for (final Entry<Instance, Map<Instance, BigDecimal>> entry : exchangeValuesMap.entrySet()) {
                    final DocPaymentInfo createDocInfo = new Swap().getNewDocPaymentInfo(_parameter, entry.getKey());
                    createDocInfo.setTargetDocInst(entry.getKey());
                    docInsts.add(entry.getKey());
                    for (final Entry<Instance, BigDecimal> innerEntry : entry.getValue().entrySet()) {
                        final Instance docInst = innerEntry.getKey();
                        if (docInst.isValid()) {
                            final Insert insert = new Insert(CISales.Document2Document4Swap);
                            if ("Collect".equalsIgnoreCase(getProperty(_parameter, "SwapType"))) {
                                insert.add(CISales.Document2Document4Swap.FromLink, entry.getKey());
                                insert.add(CISales.Document2Document4Swap.ToLink, docInst);
                            } else {
                                insert.add(CISales.Document2Document4Swap.FromLink, docInst);
                                insert.add(CISales.Document2Document4Swap.ToLink, entry.getKey());
                            }
                            insert.add(CISales.Document2Document4Swap.Date, new DateTime());
                            insert.add(CISales.Document2Document4Swap.CurrencyLink,
                                            createDocInfo.getRateCurrencyInstance());
                            insert.add(CISales.Document2Document4Swap.Amount, innerEntry.getValue());
                            insert.execute();
                            docInsts.add(docInst);
                        }
                    }
                }
            } catch (final ParseException e) {
                Exchange_Base.LOG.error("Catched ParseException", e);
            }
        }
        for (final Instance inst : docInsts) {
            new DocumentUpdate().updateDocument(_parameter, inst);
        }
        // create the reports after all data manipulation is done
        for (final CreatedDoc createdDoc : createdDocs) {
            createReport(_parameter, createdDoc);
        }
        return new Return();
    }

    /**
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return validate4CreateCalculated(final Parameter _parameter)
        throws EFapsException
    {
        final Validation val = new Validation() {
            @Override
            protected List<IWarning> validate(final Parameter _parameter,
                                              final List<IWarning> _warnings)
                throws EFapsException
            {
                final List<IWarning> ret = super.validate(_parameter, _warnings);
                final BigDecimal exchangeTotal =  getTotal4CreateCalculated(_parameter);
                final BigDecimal docTotal = new Swap().getSum4Positions(_parameter, true);
                if (exchangeTotal.compareTo(docTotal) != 0) {
                    ret.add(new ExhangeAndSwapMustHaveSameTotalWarning());
                }
                return ret;
            }
        };
        return val.validate(_parameter, this);
    }

    /**
     * Update fields4 pre calculate.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return updateFields4PreCalculate(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final BigDecimal total = new Swap().getSum4Positions(_parameter, true);
        final String divStr = _parameter.getParameterValue(
                        CIFormSales.Sales_IncomingExchangeCreateCalculatedForm.preCalculate.name);
        if (!StringUtils.isEmpty(divStr) && StringUtils.isNumeric(divStr) && total.compareTo(BigDecimal.ZERO) > 0
                        && Integer.parseInt(divStr) > 0) {
            final int div = Integer.parseInt(divStr);
            final BigDecimal[] vals = total.divideAndRemainder(new BigDecimal(div));

            final List<Map<String, Object>> strValues = new ArrayList<>();
            final StringBuilder js = new StringBuilder();
            boolean first = true;
            for (int i = 0; i < div; i++) {
                final Map<String, Object> map = new HashMap<>();
                strValues.add(map);
                final BigDecimal val;
                if (first) {
                    val = vals[0].add(vals[1]);
                    first = false;
                } else {
                    val = vals[0];
                }
                map.put(CITableSales.Sales_IncomingExchangeCreateCalculatedTable.crossTotal4Exchange.name, val);
                map.put(CITableSales.Sales_IncomingExchangeCreateCalculatedTable.date.name + "_eFapsDate",
                                DateUtil.getDate4Parameter(new DateTime()));
                map.put(CITableSales.Sales_IncomingExchangeCreateCalculatedTable.dueDate.name + "_eFapsDate",
                                DateUtil.getDate4Parameter(new DateTime().plusMonths(i + 1)));
            }
            js.append(getTableRemoveScript(_parameter, "exchangeTable", false, false))
                .append(getTableAddNewRowsScript(_parameter, "exchangeTable", strValues, null));
            final List<Map<String, Object>> values = new ArrayList<>();
            final Map<String, Object> map = new HashMap<>();
            map.put("eFapsFieldUpdateJS", js.toString());
            map.put(CIFormSales.Sales_ExchangeCreateCalculatedForm.exchangeTotal.name, total.toString());
            values.add(map);
            ret.put(ReturnValues.VALUES, values);
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return updateFields4CrossTotal(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final List<Map<String, Object>> list = new ArrayList<>();
        final Map<String, Object> map = new HashMap<>();
        final BigDecimal total = getTotal4CreateCalculated(_parameter);
        map.put(CIFormSales.Sales_ExchangeCreateCalculatedForm.exchangeTotal.name, total.toString());
        list.add(map);
        ret.put(ReturnValues.VALUES, list);
        return ret;
    }

    /**
     * Method for create a new Quotation.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @return total
     * @throws EFapsException on error.
     */
    protected BigDecimal getTotal4CreateCalculated(final Parameter _parameter)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        final String[] crossAr = _parameter
                        .getParameterValues(CITableSales.Sales_ExchangeCreateCalculatedTable.crossTotal4Exchange.name);
        if (crossAr != null) {
            try {
                for (final String cross : crossAr) {
                    ret = ret.add((BigDecimal) NumberFormatter.get().getFormatter()
                                    .parse(cross));
                }
            } catch (final ParseException e) {
                Exchange_Base.LOG.error("Catched ParseException", e);
            }
        }
        return ret;
    }

    /**
     * Reopen related.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return reopenRelated(final Parameter _parameter)
        throws EFapsException
    {
        if ("true".equalsIgnoreCase(
                        _parameter.getParameterValue(CIFormSales.Sales_IncomingExchangeSetCanceledForm.reopen.name))) {

            final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Document2Document4Swap);
            attrQueryBldr.addWhereAttrEqValue(CISales.Document2Document4Swap.ToLink, _parameter.getInstance());

            final QueryBuilder queryBldr = new QueryBuilder(CIERP.DocumentAbstract);
            queryBldr.addWhereAttrInQuery(CIERP.DocumentAbstract.ID,
                            attrQueryBldr.getAttributeQuery(CISales.Document2Document4Swap.FromLink));
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CIERP.DocumentAbstract.StatusAbstract);
            multi.executeWithoutAccessCheck();
            final Set<String> satusKeys = new HashSet<>();
            satusKeys.add("Closed");
            satusKeys.add("Paid");
            while (multi.next()) {
                final Status status = Status.get(multi.<Long>getAttribute(CIERP.DocumentAbstract.StatusAbstract));
                if (satusKeys.contains(status.getKey())) {
                    final Status newStatus = status.getStatusGroup().get("Open");
                    if (newStatus != null) {
                        final Update update = new Update(multi.getCurrentInstance());
                        update.add(CIERP.DocumentAbstract.StatusAbstract, newStatus);
                        update.executeWithoutAccessCheck();
                    }
                }
            }
        }
        return new Return();
    }

    /**
     * Warning for amount greater zero.
     */
    public static class ExhangeAndSwapMustHaveSameTotalWarning
        extends AbstractWarning
    {
        /**
         * Constructor.
         */
        public ExhangeAndSwapMustHaveSameTotalWarning()
        {
            setError(true);
        }
    }
}
