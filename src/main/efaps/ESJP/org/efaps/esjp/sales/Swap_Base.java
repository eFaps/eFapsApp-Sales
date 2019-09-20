/*
 * Copyright 2003 - 2019 The eFaps Team
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
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.ui.RateUI;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractCommand;
import org.efaps.db.CachedPrintQuery;
import org.efaps.db.Context;
import org.efaps.db.Delete;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.ci.CITableSales;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.AbstractWarning;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.IWarning;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.erp.RateFormatter;
import org.efaps.esjp.erp.RateInfo;
import org.efaps.esjp.sales.document.AbstractDocument_Base;
import org.efaps.esjp.sales.document.Validation;
import org.efaps.esjp.sales.payment.DocPaymentInfo;
import org.efaps.esjp.sales.payment.DocumentUpdate;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author The eFaps Team
 */
@EFapsUUID("310e13ba-6fdb-49a7-8612-5c0f3802e550")
@EFapsApplication("eFapsApp-Sales")
public abstract class Swap_Base
    extends CommonDocument
{
    /**
     * Key used for storing information during request.
     */
    protected static final String REQUESTKEY = Swap.class.getName() + ".Key4Request";

    /**
     * Logger for this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Swap.class);

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return containing map for AutoComplete
     * @throws EFapsException on error
     */
    public Return autoComplete4Document(final Parameter _parameter)
        throws EFapsException
    {
        final String req = (String) _parameter.get(ParameterValues.OTHERS);

        final List<Map<String, String>> list = new ArrayList<>();
        final Map<String, Map<String, String>> tmpMap = new TreeMap<>();

        final QueryBuilder queryBldr = getQueryBldr4Documents(_parameter);

        InterfaceUtils.addMaxResult2QueryBuilder4AutoComplete(_parameter, queryBldr);
        queryBldr.addWhereAttrMatchValue(CISales.DocumentSumAbstract.Name, req + "*").setIgnoreCase(true);

        final String key = containsProperty(_parameter, "Key") ? getProperty(_parameter, "Key") : "OID";
        final boolean showContact = !"false".equalsIgnoreCase(getProperty(_parameter, "ShowContact"));

        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selContactName = SelectBuilder.get().linkto(CISales.DocumentSumAbstract.Contact)
                        .attribute(CIContacts.Contact.Name);
        if (showContact) {
            multi.addSelect(selContactName);
        }
        multi.addAttribute(key);
        multi.addAttribute(CISales.DocumentSumAbstract.Name, CISales.DocumentSumAbstract.Date);
        multi.execute();
        while (multi.next()) {
            final String name = multi.<String>getAttribute(CISales.DocumentAbstract.Name);
            final DateTime date = multi.<DateTime>getAttribute(CISales.DocumentAbstract.Date);
            String choice = multi.getCurrentInstance().getType().getLabel() + " - "
                            + name + " - " + date.toString(DateTimeFormat.forStyle("S-").withLocale(
                                            Context.getThreadContext().getLocale()));
            if (showContact) {
                choice = choice + " - " + multi.getSelect(selContactName);
            }
            final Map<String, String> map = new HashMap<>();
            map.put(EFapsKey.AUTOCOMPLETE_KEY.getKey(), multi.getAttribute(key).toString());
            map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), choice);
            map.put(EFapsKey.AUTOCOMPLETE_CHOICE.getKey(), choice);
            tmpMap.put(choice, map);
        }
        list.addAll(tmpMap.values());
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * Gets the QueryBuilder for documents.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the query bldr4 documents
     * @throws EFapsException on error
     */
    protected QueryBuilder getQueryBldr4Documents(final Parameter _parameter)
        throws EFapsException
    {
        final Properties props = Sales.SWAPCONFIG.get();
        String propkey = null;

        if (_parameter.getCallInstance() != null) {
            propkey = _parameter.getCallInstance().getType().getName();
        } else if (_parameter.get(ParameterValues.CALL_CMD) != null) {
            final AbstractCommand cmd = (AbstractCommand) _parameter.get(ParameterValues.CALL_CMD);
            if (cmd.getTargetCreateType() != null) {
                propkey = cmd.getTargetCreateType().getName();
            }
        }

        if (propkey == null && containsProperty(_parameter, "SwapType")) {
            propkey = getProperty(_parameter, "SwapType");
        }
        QueryBuilder ret = getQueryBldrFromProperties(_parameter, props, propkey);
        if (ret == null) {
            ret = getQueryBldrFromProperties(_parameter);
        }
        if ("true".equalsIgnoreCase(props.getProperty(propkey + ".Filter4Contact"))) {
            final Instance callInst = (Instance) _parameter.get(ParameterValues.CALL_INSTANCE);
            if (callInst != null && callInst.isValid()) {
                final PrintQuery print = new PrintQuery(callInst);
                print.addAttribute(CISales.DocumentSumAbstract.Contact);
                print.executeWithoutAccessCheck();
                ret.addWhereAttrEqValue(CISales.DocumentSumAbstract.Contact, print.<Long>getAttribute(
                                CISales.DocumentSumAbstract.Contact));
            } else if (_parameter.getParameterValue("contact") != null) {
                final Instance inst = Instance.get(_parameter.getParameterValue("contact"));
                ret.addWhereAttrEqValue(CISales.DocumentSumAbstract.Contact, inst.isValid() ? inst : 0);
            }
        }
        final Map<Integer, String> extraParas = analyseProperty(_parameter, "ExtraParameter");
        if (extraParas.containsValue("deactivateFilter4Contact")
            && !"true".equalsIgnoreCase(_parameter.getParameterValue("deactivateFilter4Contact"))) {
            final Instance docInst = Instance.get(_parameter.getParameterValue("createDocument"));
            if (InstanceUtils.isValid(docInst)) {
                final PrintQuery print = new PrintQuery(docInst);
                print.addAttribute(CISales.DocumentSumAbstract.Contact);
                print.executeWithoutAccessCheck();
                ret.addWhereAttrEqValue(CISales.DocumentSumAbstract.Contact, print.<Long>getAttribute(
                                CISales.DocumentSumAbstract.Contact));
            }
        }
        if (BooleanUtils.toBoolean(_parameter.getParameterValue("excludeSwapedDocs"))) {
            final QueryBuilder queryBldr = new QueryBuilder(CISales.Document2Document4Swap);
            ret.addWhereAttrNotInQuery(CIERP.DocumentAbstract.ID,
                            queryBldr.getAttributeQuery(CISales.Document2Document4Swap.FromLink));

            final QueryBuilder queryBldr2 = new QueryBuilder(CISales.Document2Document4Swap);
            ret.addWhereAttrNotInQuery(CIERP.DocumentAbstract.ID,
                            queryBldr2.getAttributeQuery(CISales.Document2Document4Swap.ToLink));
        }
        return ret;
    }

    /**
     * Picker multi print.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return pickerMultiPrint(final Parameter _parameter)
        throws EFapsException
    {
        final MultiPrint multi = new MultiPrint() {
            @Override
            public List<Instance> getInstances(final Parameter _parameter)
                throws EFapsException
            {
                final List<Instance> ret = new ArrayList<>();
                final QueryBuilder queryBldr = getQueryBldr4Documents(_parameter);
                if (queryBldr != null) {
                    ret.addAll(queryBldr.getQuery().execute());
                }
                return ret;
            }
        };
        return multi.execute(_parameter);
    }

    /**
     * Picker4 document.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return picker4Document(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final Map<String, String> map = new HashMap<>();
        retVal.put(ReturnValues.VALUES, map);

        final StringBuilder js = new StringBuilder();
        js.append(getTableRemoveScript(_parameter, "swapTable"));
        final List<Map<String, Object>> values = new ArrayList<>();

        final List<Instance> docInsts = getSelectedInstances(_parameter);
        if (!docInsts.isEmpty()) {
            final Parameter parameter = ParameterUtil.clone(_parameter, ParameterValues.PARAMETERS,
                            _parameter.get(ParameterValues.PARENTPARAMETERS));
            for (final Instance docInst : docInsts) {
                values.add(getPositionUpdateMap4Doc(parameter, docInst, true));
            }
        }
        final List<Map<String, Object>> sums = new ArrayList<>();
        sums.add(getSumUpdateMap(_parameter, values, false));
        js.append(getSetFieldValuesScript(_parameter, sums, null));
        js.append(getTableAddNewRowsScript(_parameter, "swapTable", values, null));
        map.put(EFapsKey.PICKER_JAVASCRIPT.getKey(), js.toString());
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return containing map for fieldupdate
     * @throws EFapsException on error
     */
    public Return updateFields4Document(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, Object>> values = new ArrayList<>();
        final Map<String, Object> map = new HashMap<>();
        final int idx = getSelectedRow(_parameter);
        final Instance docInst = Instance.get(_parameter.getParameterValues("document")[idx]);
        if (docInst.isValid()) {
            map.putAll(getPositionUpdateMap4Doc(_parameter, docInst, false));
            map.put(EFapsKey.FIELDUPDATE_USEIDX.getKey(), idx);
        }
        values.add(map);
        final Map<String, Object> sum = getSumUpdateMap(_parameter, values, true);
        sum.put(EFapsKey.FIELDUPDATE_USEIDX.getKey(), 0);
        values.add(sum);
        return new Return().put(ReturnValues.VALUES, values);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return empty Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Instance createInst = getTargetInstance(_parameter);
        final String[] docOids = _parameter.getParameterValues(CITableSales.Sales_SwapPayTable.document.name);
        final List<Instance> docInsts = new ArrayList<>();
        if (createInst.isValid() && docOids != null) {
            final DocPaymentInfo createDocInfo = getNewDocPaymentInfo(_parameter, createInst);
            createDocInfo.setTargetDocInst(createInst);
            docInsts.add(createInst);
            int i = 0;
            for (final String docOid : docOids) {
                final Instance docInst = Instance.get(docOid);
                if (docInst.isValid()) {
                    try {
                        final DateTime date = new DateTime(_parameter.getParameterValue("date"));
                        final BigDecimal partial = (BigDecimal) NumberFormatter.get().getFormatter()
                                        .parse(_parameter.getParameterValues("partial")[i]);
                        final Insert insert = new Insert(CISales.Document2Document4Swap);
                        if ("Collect".equalsIgnoreCase(getProperty(_parameter, "SwapType"))) {
                            insert.add(CISales.Document2Document4Swap.FromLink, createInst);
                            insert.add(CISales.Document2Document4Swap.ToLink, docInst);
                        } else {
                            insert.add(CISales.Document2Document4Swap.FromLink, docInst);
                            insert.add(CISales.Document2Document4Swap.ToLink, createInst);
                        }
                        insert.add(CISales.Document2Document4Swap.CurrencyLink,
                                        createDocInfo.getRateCurrencyInstance());
                        insert.add(CISales.Document2Document4Swap.Amount, partial);
                        insert.add(CISales.Document2Document4Swap.Date, date);
                        insert.execute();
                        docInsts.add(docInst);
                    } catch (final ParseException e) {
                        Swap_Base.LOG.error("Catched ParseException", e);
                    }
                }
                i++;
            }
        }

        for (final Instance inst : docInsts) {
            new DocumentUpdate().updateDocument(_parameter, inst);
        }
        return new Return();
    }

    /**
     * Update fields for percentage.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException the eFaps exception
     */
    public Return updateFields4Percentage(final Parameter _parameter)
        throws EFapsException
    {
        Return ret = new Return();
        final int idx = getSelectedRow(_parameter);
        final Instance docInst = Instance.get(_parameter.getParameterValues("document")[idx]);
        final String perStr = _parameter.getParameterValues("percentage")[idx];
        if (docInst.isValid() && StringUtils.isNotEmpty(perStr)) {
            BigDecimal percentage = BigDecimal.ONE;
            try {
                percentage = (BigDecimal) NumberFormatter.get().getFormatter().parse(perStr);
            } catch (final ParseException e) {
                Swap_Base.LOG.error("Catched ParseException", e);
            }
            final DocPaymentInfo docInfo = getNewDocPaymentInfo(_parameter, docInst).setTargetDocInst(docInst);
            final DecimalFormat frmt = docInfo.getFormatter();
            final BigDecimal total4Doc = docInfo.getCrossTotalInCurrency(_parameter, docInfo.getRateCurrencyInstance());
            final BigDecimal payments4Doc = docInfo.getPaidInCurrency(docInfo.getRateCurrencyInstance(), true);
            final BigDecimal partial = total4Doc.subtract(payments4Doc)
                            .multiply(percentage.setScale(8, RoundingMode.HALF_UP)
                                            .divide(new BigDecimal(100), RoundingMode.HALF_UP))
                            .setScale(2, RoundingMode.HALF_UP);
            final String[] partialArray = _parameter.getParameterValues("partial");
            partialArray[idx] = frmt.format(partial);
            final Parameter parameter = ParameterUtil.clone(_parameter);
            ParameterUtil.setParameterValues(parameter, "partial", partialArray);
            ret = updateFields4Partial(parameter);
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return containing map for fieldupdate
     * @throws EFapsException on error
     */
    public Return updateFields4Partial(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, Object>> list = new ArrayList<>();
        final int selected = getSelectedRow(_parameter);
        final Instance targetInstance = getTargetInstance(_parameter);
        final Instance docInstance = Instance.get(_parameter.getParameterValues("document")[selected]);

        if (docInstance.isValid()) {
            try {
                final Object[] rateObj = getRateObject(_parameter, "rate", selected);
                final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                                BigDecimal.ROUND_HALF_UP);
                final BigDecimal rateUI = (BigDecimal) NumberFormatter.get().getFormatter()
                                .parse(_parameter.getParameterValues("rate")[selected]);
                final BigDecimal partial = (BigDecimal) NumberFormatter.get().getFormatter()
                                .parse(_parameter.getParameterValues("partial")[selected]);

                final DocPaymentInfo docInfo = getNewDocPaymentInfo(_parameter, docInstance);
                docInfo.setTargetDocInst(targetInstance != null && targetInstance.isValid()
                                ? targetInstance : docInstance);
                docInfo.getRateInfo4Target().setRate(rate);
                docInfo.getRateInfo4Target().setRateUI(rateUI);
                docInfo.getRateInfo4Target().setSaleRate(rate);
                docInfo.getRateInfo4Target().setSaleRateUI(rateUI);

                docInfo.setRateCrossTotal(partial);
                // the user has the right to overwrite the value to be applied
                if (InstanceUtils.isValid(targetInstance)) {
                    docInfo.setRateCurrencyInstance(docInfo.getTargetInfo().getCurrencyInstance());
                }
                final Map<String, Object> map = getPositionUpdateMap(_parameter, docInfo, true);
                map.put(EFapsKey.FIELDUPDATE_USEIDX.getKey(), selected);
                list.add(map);
                final Map<String, Object> sum = getSumUpdateMap(_parameter, list, true);
                sum.put(EFapsKey.FIELDUPDATE_USEIDX.getKey(), 0);
                list.add(sum);
            } catch (final ParseException e) {
                Swap_Base.LOG.error("Catched ParseException", e);
            }
        }
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return TargetInstance
     * @throws EFapsException on error
     */
    protected Instance getTargetInstance(final Parameter _parameter)
        throws EFapsException
    {
        final Instance ret;
        if (_parameter.getParameterValue("createDocument") == null) {
            ret = _parameter.getCallInstance();
        } else {
            ret = Instance.get(_parameter.getParameterValue("createDocument"));
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return containing map for fieldupdate
     * @throws EFapsException on error
     */
    public Return updateFields4Rate(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, Object>> list = new ArrayList<>();
        final int selected = getSelectedRow(_parameter);
        final Instance targetInstance = getTargetInstance(_parameter);
        final Instance docInstance = Instance.get(_parameter.getParameterValues("document")[selected]);

        if (docInstance.isValid()) {
            try {
                final Object[] rateObj = getRateObject(_parameter, "rate", selected);
                final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                                BigDecimal.ROUND_HALF_UP);
                final BigDecimal rateUI = (BigDecimal) NumberFormatter.get().getFormatter()
                                .parse(_parameter.getParameterValues("rate")[selected]);
                final DocPaymentInfo docInfo = getNewDocPaymentInfo(_parameter, docInstance);
                docInfo.setTargetDocInst(targetInstance != null && targetInstance.isValid()
                                ? targetInstance : docInstance);
                docInfo.getRateInfo4Target().setRate(rate);
                docInfo.getRateInfo4Target().setRateUI(rateUI);
                docInfo.getRateInfo4Target().setSaleRate(rate);
                docInfo.getRateInfo4Target().setSaleRateUI(rateUI);
                if (rate.compareTo(BigDecimal.ONE) != 0 && docInfo.getTargetInfo().getCurrencyInstance().equals(docInfo
                                .getRateCurrencyInstance())) {
                    docInfo.getTargetInfo().setCurrencyInstance(docInfo.getCurrencyInstance());
                }

                final Map<String, Object> map = getPositionUpdateMap(_parameter, docInfo, false);
                map.put(EFapsKey.FIELDUPDATE_USEIDX.getKey(), selected);
                list.add(map);

                final Map<String, Object> sum = getSumUpdateMap(_parameter, list, true);
                sum.put(EFapsKey.FIELDUPDATE_USEIDX.getKey(), 0);
                list.add(sum);

            } catch (final ParseException e) {
                Swap_Base.LOG.error("Catched ParseException", e);
            }
        }
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * Gets the position update map4 doc.
     *
     * @param _parameter parameter as passed by the eFasp API
     * @param _docInst instance of the document the map is wanted for
     * @param _addDocInfo the add doc info
     * @return map containing values for update
     * @throws EFapsException on error
     */
    protected Map<String, Object> getPositionUpdateMap4Doc(final Parameter _parameter,
                                                           final Instance _docInst,
                                                           final boolean _addDocInfo)
        throws EFapsException
    {
        final Map<String, Object> ret = new HashMap<>();
        final PrintQuery print = new PrintQuery(_docInst);
        final SelectBuilder selContactName = SelectBuilder.get().linkto(CISales.DocumentSumAbstract.Contact)
                        .attribute(CIContacts.Contact.Name);
        final SelectBuilder selCurrInst = SelectBuilder.get().linkto(CISales.DocumentSumAbstract.CurrencyId)
                        .instance();
        final SelectBuilder selRateCurrInst = SelectBuilder.get()
                        .linkto(CISales.DocumentSumAbstract.RateCurrencyId)
                        .instance();
        print.addSelect(selContactName, selCurrInst, selRateCurrInst);
        print.addAttribute(CISales.DocumentSumAbstract.Name,
                        CISales.DocumentSumAbstract.CrossTotal, CISales.DocumentSumAbstract.NetTotal,
                        CISales.DocumentSumAbstract.RateNetTotal, CISales.DocumentSumAbstract.RateCrossTotal,
                        CISales.DocumentSumAbstract.StatusAbstract);
        print.execute();
        final CurrencyInst currInst = CurrencyInst.get(print.<Instance>getSelect(selCurrInst));
        final CurrencyInst rateCurrInst = CurrencyInst.get(print.<Instance>getSelect(selRateCurrInst));
        if (_addDocInfo) {
            ret.put("document", new String[] { _docInst.getOid(),
                            print.getAttribute(CISales.DocumentSumAbstract.Name) });
        }

        ret.put("contact4Doc", StringEscapeUtils.escapeEcmaScript(print.getSelect(selContactName)));
        ret.put("crossTotal", print.getAttribute(CISales.DocumentSumAbstract.CrossTotal) + currInst.getSymbol());
        ret.put("netTotal", print.getAttribute(CISales.DocumentSumAbstract.NetTotal) + currInst.getSymbol());
        ret.put("rateCrossTotal",
                        print.getAttribute(CISales.DocumentSumAbstract.RateCrossTotal) + rateCurrInst.getSymbol());
        ret.put("rateNetTotal",
                        print.getAttribute(CISales.DocumentSumAbstract.RateNetTotal) + rateCurrInst.getSymbol());
        ret.put("status", Status.get(
                        print.<Long>getAttribute(CISales.DocumentSumAbstract.StatusAbstract)).getLabel());
        final Parameter parameter = ParameterUtil.clone(_parameter);
        ParameterUtil.setProperty(parameter, "SwapFrom", "true");
        ret.put("swapBalanceFrom", getSwapBalance(parameter, _docInst));
        ParameterUtil.setProperty(parameter, "SwapFrom", "false");
        ret.put("swapBalanceTo", getSwapBalance(parameter, _docInst));
        ret.putAll(getPositionUpdateMap(_parameter, _docInst));
        return ret;
    }

    /**
     * @param _parameter parameter as passed by the eFasp API
     * @param _docInst instance of the document the map is wanted for
     * @return map containing values for update
     * @throws EFapsException on error
     */
    protected Map<String, Object> getPositionUpdateMap(final Parameter _parameter,
                                                       final Instance _docInst)
        throws EFapsException
    {
        final Instance targetInstance = getTargetInstance(_parameter);
        final DocPaymentInfo docInfo = getNewDocPaymentInfo(_parameter, _docInst);
        docInfo.setTargetDocInst(targetInstance == null ? _docInst : targetInstance);
        return getPositionUpdateMap(_parameter, docInfo, false);
    }

    /**
     * @param _parameter parameter as passed by the eFasp API
     * @param _docInfo DocPaymentInfo the map is wanted for
     * @param _setFromUI thevalue was set from the UI and therefore most be set as is
     * @return map containing values for update
     * @throws EFapsException on error
     */
    protected Map<String, Object> getPositionUpdateMap(final Parameter _parameter,
                                                       final DocPaymentInfo _docInfo,
                                                       final boolean _setFromUI)
        throws EFapsException
    {
        final Map<String, Object> ret = new HashMap<>();
        final DecimalFormat frmt = _docInfo.getFormatter();
        final BigDecimal total4Doc = _docInfo.getCrossTotalInCurrency(_parameter, _docInfo.getRateCurrencyInstance());
        final BigDecimal payments4Doc = _docInfo.getPaidInCurrency(_docInfo.getRateCurrencyInstance(), true);
        ret.put("partial", frmt.format(_setFromUI ? total4Doc : total4Doc.subtract(payments4Doc)));
        ret.put("rate", RateInfo.getRateUIFrmt(_parameter, _docInfo.getRateInfo4Target(),
                        _docInfo.getInstance().getType().getName()));
        ret.put("rate" + RateUI.INVERTEDSUFFIX, "" + _docInfo.getRateInfo4Target().isInvert());
        ret.put("documentInfo", _docInfo.getInfoField());
        return ret;
    }

    /**
     * @param _parameter parameter as passed by the eFasp API
     * @param _maps list of maps to be analyzed for the sum
     * @param _includeUI inlude the values from the UI also
     * @return map of sum
     * @throws EFapsException on error
     */
    protected Map<String, Object> getSumUpdateMap(final Parameter _parameter,
                                                  final Collection<Map<String, Object>> _maps,
                                                  final boolean _includeUI)
        throws EFapsException
    {
        final Map<String, Object> ret = new HashMap<>();
        try {
            BigDecimal total = BigDecimal.ZERO;
            for (final Map<String, Object> map : _maps) {
                total = total.add((BigDecimal) NumberFormatter.get().getFormatter()
                                .parse((String) map.get("partial")));
            }
            if (_includeUI) {
                total = total.add(getSum4Positions(_parameter, false));
            }
            ret.put("total", NumberFormatter.get().getTwoDigitsFormatter().format(total));

        } catch (final ParseException e) {
            Swap_Base.LOG.error("Catched ParseException", e);
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @param _field rate field name
     * @param _index inde to be used
     * @return Object from UI
     * @throws EFapsException on error
     */
    protected Object[] getRateObject(final Parameter _parameter,
                                     final String _field,
                                     final int _index)
        throws EFapsException
    {
        BigDecimal rate = BigDecimal.ONE;
        try {
            rate = (BigDecimal) RateFormatter.get().getFrmt4Rate(null).parse(_parameter.getParameterValues(_field)[_index]);
        } catch (final ParseException e) {
            throw new EFapsException(AbstractDocument_Base.class, "analyzeRate.ParseException", e);
        }
        final boolean rInv = "true"
                        .equalsIgnoreCase(_parameter.getParameterValues(_field + RateUI.INVERTEDSUFFIX)[_index]);
        return new Object[] { rInv ? BigDecimal.ONE : rate, rInv ? rate : BigDecimal.ONE };
    }

    /**
     * @param _parameter parameter as passed by the eFasp API
     * @param _includeCurrent include the current position
     * @return sum of the positions
     * @throws EFapsException on error
     */
    public BigDecimal getSum4Positions(final Parameter _parameter,
                                       final boolean _includeCurrent)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        final String[] paymentAmounts = _parameter.getParameterValues("partial");
        final int selected = getSelectedRow(_parameter);
        for (int i = 0; i < paymentAmounts.length; i++) {
            try {
                if (!_includeCurrent && selected != i || _includeCurrent) {
                    ret = ret.add((BigDecimal) NumberFormatter.get().getFormatter().parse(paymentAmounts[i]));
                }
            } catch (final ParseException e) {
                // only show that error during debug, because it is likely that
                // the user did just used invalid strings
                Swap_Base.LOG.debug("Catched ParseException", e);
            }
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return containing map for fieldupdate
     * @throws EFapsException on error
     */
    public Return updateFields4CreateDocument(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, Object>> values = new ArrayList<>();
        final Map<String, Object> map = new HashMap<>();

        final Instance docInst = Instance.get(_parameter.getParameterValue("createDocument"));
        if (docInst.isValid()) {
            final DocPaymentInfo docInfo = getNewDocPaymentInfo(_parameter, docInst);
            docInfo.setTargetDocInst(docInst);
            map.put("createInfo", docInfo.getInfoField());
        }
        values.add(map);
        return new Return().put(ReturnValues.VALUES, values);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst  Instance of the document
     * @return DocPaymentInfo
     * @throws EFapsException on erro
     */
    public DocPaymentInfo getNewDocPaymentInfo(final Parameter _parameter,
                                               final Instance _docInst)
        throws EFapsException
    {
        return new DocPaymentInfo(_docInst).setParameter(_parameter);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return containing map for fieldupdate
     * @throws EFapsException on error
     */
    public Return getDocumentInfoFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        return new Return().put(ReturnValues.VALUES, getDocumentInfo(_parameter, _parameter.getCallInstance()));
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst  Instance of the document
     * @return String for the Interface
     * @throws EFapsException on error
     */
    protected String getDocumentInfo(final Parameter _parameter,
                                     final Instance _docInst)
        throws EFapsException
    {
        final StringBuilder ret = new StringBuilder();
        final PrintQuery print = new PrintQuery(_docInst);
        print.addAttribute(CISales.DocumentSumAbstract.Name, CISales.DocumentSumAbstract.Rate);
        print.execute();

        final RateInfo rateinfo = new Currency().evaluateRateInfo(_parameter,
                        (Object[]) print.getAttribute(CISales.DocumentSumAbstract.Rate));
        final DocPaymentInfo payInfo = getNewDocPaymentInfo(_parameter, _docInst);
        payInfo.setTargetDocInst(_docInst);
        //if set by user sales and buy are equal
        payInfo.getRateInfo4Target().setRate(rateinfo.getRate());
        payInfo.getRateInfo4Target().setRateUI(rateinfo.getRateUI());
        payInfo.getRateInfo4Target().setSaleRate(rateinfo.getSaleRate());
        payInfo.getRateInfo4Target().setSaleRateUI(rateinfo.getSaleRateUI());

        ret.append(_docInst.getType().getLabel()).append(" ")
                        .append(print.<String>getAttribute(CISales.DocumentSumAbstract.Name)).append(" ")
                        .append(payInfo.getInfoField());
        return ret.toString();
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return containing result
     * @throws EFapsException on error
     */
    public Return getSwapDocumentFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret;
        if (_parameter.getInstance().getType().isKindOf(CIERP.DocumentAbstract)) {
            ret = new Return().put(ReturnValues.VALUES, getDocumentInfo(_parameter, _parameter.getInstance()));
        } else {
            final SwapInfo info = getInfos(_parameter).get(_parameter.getInstance());
            ret = new Return().put(ReturnValues.VALUES, info == null ? "" : info.getDocument());
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return map wirth infos
     * @throws EFapsException on error
     */
    @SuppressWarnings("unchecked")
    protected Map<Instance, SwapInfo> getInfos(final Parameter _parameter)
        throws EFapsException
    {
        final Map<Instance, SwapInfo> ret;
        if (Context.getThreadContext().containsRequestAttribute(Swap.REQUESTKEY)) {
            ret = (Map<Instance, SwapInfo>) Context.getThreadContext().getRequestAttribute(Swap.REQUESTKEY);
        } else {
            final Instance callInst = _parameter.getCallInstance();
            final List<Instance> relInsts = (List<Instance>) _parameter.get(ParameterValues.REQUEST_INSTANCES);
            ret = Swap_Base.getSwapInfos(_parameter, callInst, relInsts);
            Context.getThreadContext().setRequestAttribute(Swap.REQUESTKEY, ret);
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return containing result
     * @throws EFapsException on error
     */
    public Return getSwapDirectionFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final SwapInfo info  = getInfos(_parameter).get(_parameter.getInstance());
        return new Return().put(ReturnValues.VALUES, info == null ? "" : info.getDirection());
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return containing result
     * @throws EFapsException on error
     */
    public Return getSwapDocumentFieldInstance(final Parameter _parameter)
        throws EFapsException
    {
        final SwapInfo info  = getInfos(_parameter).get(_parameter.getInstance());
        return new Return().put(ReturnValues.INSTANCE, info == null ? null : info.getDocInst());
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return containing result
     * @throws EFapsException on error
     */
    public Return validate(final Parameter _parameter)
        throws EFapsException
    {
        return new SwapValidation().validate(_parameter, (AbstractDocument_Base) null);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return containing result
     * @throws EFapsException on error
     */
    public Return getSwapTotalFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        return new Return().put(ReturnValues.VALUES, getSwapTotal(_parameter, _parameter.getInstance()));
    }

    /**
     * Gets the swap balance field value.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the swap balance field value
     * @throws EFapsException on error
     */
    public Return getSwapBalanceFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        return new Return().put(ReturnValues.VALUES, getSwapBalance(_parameter, _parameter.getInstance()));
    }

    /**
     * Gets the swap balance.
     *
     * @param _parameter the parameter
     * @param _docInst the doc inst
     * @return the swap balance
     * @throws EFapsException the e faps exception
     */
    protected BigDecimal getSwapBalance(final Parameter _parameter,
                                        final Instance _docInst)
        throws EFapsException
    {
        final BigDecimal swapTotal = getSwapTotal(_parameter, _docInst);
        final PrintQuery print = CachedPrintQuery.get4Request(_docInst);
        print.addAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
        print.execute();
        final BigDecimal total = print.getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
        return total.subtract(swapTotal);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst  Instance of the document
     * @return Return containing result
     * @throws EFapsException on error
     */
    public BigDecimal getSwapTotal(final Parameter _parameter,
                                   final Instance _docInst)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        final boolean from = "true".equalsIgnoreCase(getProperty(_parameter, "SwapFrom"));
        final QueryBuilder swapQueryBldr = new QueryBuilder(CISales.Document2Document4Swap);
        swapQueryBldr.setOr(true);
        swapQueryBldr.addWhereAttrEqValue(CISales.Document2Document4Swap.FromLink, _docInst);
        swapQueryBldr.addWhereAttrEqValue(CISales.Document2Document4Swap.ToLink, _docInst);
        final InstanceQuery swapQuery = swapQueryBldr.getQuery();
        final List<Instance> relInst = swapQuery.execute();
        if (!relInst.isEmpty()) {
            for (final SwapInfo info : Swap_Base.getSwapInfos(_parameter, _docInst, relInst).values()) {
                if (info.isFrom() == from) {
                    ret = ret.add(info.getAmount());
                }
            }
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return containing result
     * @throws EFapsException on error
     */
    public Return deleteOverwriteTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final Instance swapRelInst = _parameter.getInstance();
        final PrintQuery print = new PrintQuery(swapRelInst);
        final SelectBuilder selFromStatus = SelectBuilder.get().linkto(CISales.Document2Document4Swap.FromLink)
                        .status();
        final SelectBuilder selFromInst = SelectBuilder.get().linkto(CISales.Document2Document4Swap.FromLink)
                        .instance();
        final SelectBuilder selToStatus = SelectBuilder.get().linkto(CISales.Document2Document4Swap.ToLink).status();
        final SelectBuilder selToInst = SelectBuilder.get().linkto(CISales.Document2Document4Swap.ToLink).instance();
        print.addSelect(selFromStatus, selFromInst, selToStatus, selToInst);
        print.execute();
        final Status fromStatus = print.getSelect(selFromStatus);
        final Status toStatus = print.getSelect(selToStatus);
        if (statusCheck4Delete(_parameter, fromStatus) && statusCheck4Delete(_parameter, toStatus)) {
            final Instance fromInst = print.getSelect(selFromInst);
            final Instance toInst = print.getSelect(selToInst);
            updateStatus4Delete(_parameter, fromInst, fromStatus);
            updateStatus4Delete(_parameter, toInst, toStatus);
            new Delete(swapRelInst).executeWithoutTrigger();
        }
        return new Return();
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst instance of the document tobe updated
     * @param _status Status to be checked
     * @throws EFapsException on error
     */
    protected void updateStatus4Delete(final Parameter _parameter,
                                       final Instance _docInst,
                                       final Status _status)
        throws EFapsException
    {
        if ("Paid".equals(_status.getKey()) || "Closed".equals(_status.getKey())) {
            Status newStatus = null;
            if (_status.getStatusGroup().containsKey("Open")) {
                newStatus = _status.getStatusGroup().get("Open");
            }
            if (newStatus != null) {
                final Update update = new Update(_docInst);
                update.add(CIERP.DocumentAbstract.StatusAbstract, newStatus);
                update.execute();
            }
        }
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _status Status to be checked
     * @return true if delete is permitted
     * @throws EFapsException on error
     */
    protected boolean statusCheck4Delete(final Parameter _parameter,
                                         final Status _status)
        throws EFapsException
    {
        // permit it if not booked
        return !"Booked".equals(_status.getKey());
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _callInstance instance that called and will not be shown
     * @param _relInsts relation instances
     * @return map with instance
     * @throws EFapsException on error
     */
    protected static Map<Instance, SwapInfo> getSwapInfos(final Parameter _parameter,
                                                          final Instance _callInstance,
                                                          final List<Instance> _relInsts)
        throws EFapsException
    {
        final Map<Instance, SwapInfo> ret = new HashMap<>();
        final MultiPrintQuery multi = new MultiPrintQuery(_relInsts);
        final SelectBuilder selFrom = SelectBuilder.get().linkto(CISales.Document2Document4Swap.FromLink);
        final SelectBuilder selFromInst = new SelectBuilder(selFrom).instance();
        final SelectBuilder selFromName = new SelectBuilder(selFrom).attribute(CISales.DocumentSumAbstract.Name);
        final SelectBuilder selTo = SelectBuilder.get().linkto(CISales.Document2Document4Swap.ToLink);
        final SelectBuilder selToInst = new SelectBuilder(selTo).instance();
        final SelectBuilder selToName = new SelectBuilder(selTo).attribute(CISales.DocumentSumAbstract.Name);
        final SelectBuilder selCurrInst = SelectBuilder.get().linkto(CISales.Document2Document4Swap.CurrencyLink)
                        .instance();
        multi.addSelect(selCurrInst, selFromInst, selToInst, selFromName, selToName);
        multi.addAttribute(CISales.Document2Document4Swap.Amount);
        multi.execute();
        while (multi.next()) {
            final SwapInfo info = new SwapInfo();
            final Instance fromInst = multi.getSelect(selFromInst);
            if (_callInstance.equals(fromInst)) {
                info.setFrom(true)
                    .setDocInstance(multi.<Instance>getSelect(selToInst))
                    .setDocName(multi.<String>getSelect(selToName));
            } else {
                info.setFrom(false)
                    .setDocInstance(multi.<Instance>getSelect(selFromInst))
                    .setDocName(multi.<String>getSelect(selFromName));
            }
            info.setAmount(multi.<BigDecimal>getAttribute(CISales.Document2Document4Swap.Amount));
            info.setCurrencyInstance(multi.<Instance>getSelect(selCurrInst));
            ret.put(multi.getCurrentInstance(), info);
        }
        return ret;
    }

    /**
     * Gets the swap infos for documents.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInstances the doc instances
     * @return the swap infos 4 documents
     * @throws EFapsException on error
     */
    protected static Map<Instance, Set<SwapInfo>> getSwapInfos4Documents(final Parameter _parameter,
                                                                         final Instance... _docInstances)
        throws EFapsException
    {
        final Map<Instance, Set<SwapInfo>> ret = new HashMap<>();
        if (ArrayUtils.isNotEmpty(_docInstances)) {
            final Swap swap = new Swap();
            final QueryBuilder queryBldr = new QueryBuilder(CISales.Document2Document4Swap);
            queryBldr.setOr(true);
            queryBldr.addWhereAttrEqValue(CISales.Document2Document4Swap.FromLink, (Object[]) _docInstances);
            queryBldr.addWhereAttrEqValue(CISales.Document2Document4Swap.ToLink, (Object[]) _docInstances);
            final MultiPrintQuery multi = queryBldr.getCachedPrint4Request();
            final SelectBuilder selFrom = SelectBuilder.get().linkto(CISales.Document2Document4Swap.FromLink);
            final SelectBuilder selFromInst = new SelectBuilder(selFrom).instance();
            final SelectBuilder selFromStatus = new SelectBuilder(selFrom).attribute(
                            CISales.DocumentAbstract.StatusAbstract);
            final SelectBuilder selFromName = new SelectBuilder(selFrom).attribute(CISales.DocumentSumAbstract.Name);
            final SelectBuilder selTo = SelectBuilder.get().linkto(CISales.Document2Document4Swap.ToLink);
            final SelectBuilder selToInst = new SelectBuilder(selTo).instance();
            final SelectBuilder selToName = new SelectBuilder(selTo).attribute(CISales.DocumentSumAbstract.Name);
            final SelectBuilder selToStatus = new SelectBuilder(selFrom).attribute(
                            CISales.DocumentAbstract.StatusAbstract);
            final SelectBuilder selCurrInst = SelectBuilder.get().linkto(CISales.Document2Document4Swap.CurrencyLink)
                            .instance();
            multi.addSelect(selCurrInst, selFromInst, selFromStatus, selToInst, selFromName, selToName, selToStatus);
            multi.addAttribute(CISales.Document2Document4Swap.Amount, CISales.Document2Document4Swap.Date);
            multi.execute();
            while (multi.next()) {
                final Instance fromInst = multi.<Instance>getSelect(selFromInst);
                final Instance toInst = multi.<Instance>getSelect(selToInst);
                final Status fromStatus = Status.get(multi.<Long>getSelect(selFromStatus));
                final Status toStatus = Status.get(multi.<Long>getSelect(selToStatus));
                if (swap.isValidStatus(_parameter, fromStatus) && swap.isValidStatus(_parameter, toStatus)) {
                    final SwapInfo fromInfo = new SwapInfo().setFrom(true)
                            .setDocInstance(toInst)
                            .setSwapDate(multi.getAttribute(CISales.Document2Document4Swap.Date))
                            .setDocName(multi.<String>getSelect(selToName))
                            .setAmount(multi.<BigDecimal>getAttribute(CISales.Document2Document4Swap.Amount))
                            .setCurrencyInstance(multi.<Instance>getSelect(selCurrInst));

                    final Set<SwapInfo> fromInfos;
                    if (ret.containsKey(fromInst)) {
                        fromInfos = ret.get(fromInst);
                    } else {
                        fromInfos = new HashSet<>();
                        ret.put(fromInst, fromInfos);
                    }
                    fromInfos.add(fromInfo);

                    final SwapInfo toInfo = new SwapInfo().setFrom(false)
                            .setDocInstance(fromInst)
                            .setSwapDate(multi.getAttribute(CISales.Document2Document4Swap.Date))
                            .setDocName(multi.<String>getSelect(selFromName))
                            .setAmount(multi.<BigDecimal>getAttribute(CISales.Document2Document4Swap.Amount))
                            .setCurrencyInstance(multi.<Instance>getSelect(selCurrInst));

                    final Set<SwapInfo> toInfos;
                    if (ret.containsKey(toInst)) {
                        toInfos = ret.get(toInst);
                    } else {
                        toInfos = new HashSet<>();
                        ret.put(toInst, toInfos);
                    }
                    toInfos.add(toInfo);
                }
            }
        }
        return ret;
    }

    /**
     * Checks if is valid status.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _status the status
     * @return true, if is valid status
     */
    protected boolean isValidStatus(final Parameter _parameter,
                                    final Status _status)
    {
        return !"Canceled".equals(_status.getKey());
    }

    /**
     * Validation class.
     */
    public class SwapValidation
        extends Validation
    {
        @Override
        protected List<IWarning> validate(final Parameter _parameter,
                                          final List<IWarning> _warnings)
            throws EFapsException
        {
            final List<IWarning> ret = super.validate(_parameter, _warnings);

            final BigDecimal sum = getSum4Positions(_parameter, true);
            final Instance inst = getTargetInstance(_parameter);
            final DocPaymentInfo createDocInfo = getNewDocPaymentInfo(_parameter, inst);
            createDocInfo.setTargetDocInst(inst);
            if (createDocInfo.getCrossTotal().compareTo(sum) < 0) {
                ret.add(new SwapAmountBiggerWarning());
            }
            return ret;
        }
    }

    /**
     *Info class.
     */
    public static class SwapInfo
    {
        /**
         * Instance of the document.
         */
        private Instance docInst;

        /**
         * Name of the document.
         */
        private String docName;

        /**
         * Name of the document.
         */
        private DateTime swapDate;

        /**
         * From or to.
         */
        private boolean from = false;

        /**
         * Amount.
         */
        private BigDecimal amount = BigDecimal.ZERO;

        /**
         * Instance of the curreny.
         */
        private Instance currencyInstance;

        /**
         * @return direction string
         */
        public String getDirection()
        {
            return DBProperties.getProperty(Swap.class.getName() + ".Direction." + (from ? "from" : "to"));
        }

        /**
         * @return document string
         */
        public String getDocument()
        {
            return docInst.getType().getLabel() + " " + docName;
        }

        /**
         * Setter method for instance variable {@link #docInst}.
         *
         * @param _docInst value for instance variable {@link #docInst}
         * @return this for chaining
         */
        public SwapInfo setDocInstance(final Instance _docInst)
        {
            docInst = _docInst;
            return this;
        }

        /**
         * Setter method for instance variable {@link #docName}.
         *
         * @param _docName value for instance variable {@link #docName}
         * @return this for chaining
         */
        public SwapInfo setDocName(final String _docName)
        {
            docName = _docName;
            return this;
        }

        /**
         * Setter method for instance variable {@link #from}.
         *
         * @param _from value for instance variable {@link #from}
         * @return this for chaining
         */
        public SwapInfo setFrom(final boolean _from)
        {
            from = _from;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #docInst}.
         *
         * @return value of instance variable {@link #docInst}
         */
        public Instance getDocInst()
        {
            return docInst;
        }

        /**
         * Getter method for the instance variable {@link #from}.
         *
         * @return value of instance variable {@link #from}
         */
        public boolean isFrom()
        {
            return from;
        }

        /**
         * Getter method for the instance variable {@link #amount}.
         *
         * @return value of instance variable {@link #amount}
         */
        public BigDecimal getAmount()
        {
            return amount;
        }

        /**
         * Setter method for instance variable {@link #amount}.
         *
         * @param _amount value for instance variable {@link #amount}
         * @return the swap info
         */
        public SwapInfo setAmount(final BigDecimal _amount)
        {
            amount = _amount;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #currencyInstance}.
         *
         * @return value of instance variable {@link #currencyInstance}
         */
        public Instance getCurrencyInstance()
        {
            return currencyInstance;
        }

        /**
         * Setter method for instance variable {@link #currencyInstance}.
         *
         * @param _currencyInstance value for instance variable {@link #currencyInstance}
         * @return the swap info
         */
        public SwapInfo setCurrencyInstance(final Instance _currencyInstance)
        {
            currencyInstance = _currencyInstance;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #swapDate}.
         *
         * @return value of instance variable {@link #swapDate}
         */
        public DateTime getSwapDate()
        {
            return swapDate;
        }

        /**
         * Setter method for instance variable {@link #swapDate}.
         *
         * @param _swapDate value for instance variable {@link #swapDate}
         * @return the swap info
         */
        public SwapInfo setSwapDate(final DateTime _swapDate)
        {
            swapDate = _swapDate;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #docName}.
         *
         * @return value of instance variable {@link #docName}
         */
        public String getDocName()
        {
            return docName;
        }
    }

    /**
     * Warning for existing name.
     */
    public static class SwapAmountBiggerWarning
        extends AbstractWarning
    {
        /**
         * Constructor.
         */
        public SwapAmountBiggerWarning()
        {
            setError(true);
        }
    }
}
