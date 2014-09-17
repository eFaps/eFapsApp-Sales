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
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.ui.RateUI;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.ci.CITableSales;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.erp.RateFormatter;
import org.efaps.esjp.sales.document.AbstractDocument_Base;
import org.efaps.esjp.sales.payment.DocPaymentInfo;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("310e13ba-6fdb-49a7-8612-5c0f3802e550")
@EFapsRevision("$Rev$")
public abstract class Swap_Base
    extends CommonDocument
{

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
        final Map<String, Map<String, String>> tmpMap = new TreeMap<String, Map<String, String>>();

        final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter);

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
            String choice = name + " - " + date.toString(DateTimeFormat.forStyle("S-").withLocale(
                            Context.getThreadContext().getLocale()));
            if (showContact) {
                choice = choice + " - " + multi.getSelect(selContactName);
            }
            final Map<String, String> map = new HashMap<String, String>();
            map.put(EFapsKey.AUTOCOMPLETE_KEY.getKey(), multi.getAttribute(key).toString());
            map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), name);
            map.put(EFapsKey.AUTOCOMPLETE_CHOICE.getKey(), choice);
            tmpMap.put(name, map);
        }
        list.addAll(tmpMap.values());
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
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
        final List<Map<String, Object>> values = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        final int idx = getSelectedRow(_parameter);
        final Instance docInst = Instance.get(_parameter.getParameterValues("document")[idx]);
        if (docInst.isValid()) {
            final PrintQuery print = new PrintQuery(docInst);
            final SelectBuilder selContactName = SelectBuilder.get().linkto(CISales.DocumentSumAbstract.Contact)
                            .attribute(CIContacts.Contact.Name);
            final SelectBuilder selCurrInst = SelectBuilder.get().linkto(CISales.DocumentSumAbstract.CurrencyId)
                            .instance();
            final SelectBuilder selRateCurrInst = SelectBuilder.get()
                            .linkto(CISales.DocumentSumAbstract.RateCurrencyId)
                            .instance();
            print.addSelect(selContactName, selCurrInst, selRateCurrInst);
            print.addAttribute(CISales.DocumentSumAbstract.CrossTotal, CISales.DocumentSumAbstract.NetTotal,
                            CISales.DocumentSumAbstract.RateNetTotal, CISales.DocumentSumAbstract.RateCrossTotal,
                            CISales.DocumentSumAbstract.StatusAbstract);
            print.execute();
            final CurrencyInst currInst = CurrencyInst.get(print.getSelect(selCurrInst));
            final CurrencyInst rateCurrInst = CurrencyInst.get(print.getSelect(selRateCurrInst));

            map.put("contact", print.getSelect(selContactName));
            map.put("crossTotal", print.getAttribute(CISales.DocumentSumAbstract.CrossTotal) + currInst.getSymbol());
            map.put("netTotal", print.getAttribute(CISales.DocumentSumAbstract.NetTotal) + currInst.getSymbol());
            map.put("rateCrossTotal",
                            print.getAttribute(CISales.DocumentSumAbstract.RateCrossTotal) + rateCurrInst.getSymbol());
            map.put("rateNetTotal",
                            print.getAttribute(CISales.DocumentSumAbstract.RateNetTotal) + rateCurrInst.getSymbol());
            map.put("status", Status.get(
                            print.<Long>getAttribute(CISales.DocumentSumAbstract.StatusAbstract)).getLabel());
            map.putAll(getPositionUpdateMap(_parameter, docInst));
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
        final Instance createInst = Instance.get(_parameter
                        .getParameterValue(CIFormSales.Sales_SwapCreateCollectForm.createDocument.name));
        final String[] docOids = _parameter.getParameterValues(CITableSales.Sales_SwapPayTable.document.name);
        if (createInst.isValid() && docOids != null) {
            final DocPaymentInfo createDocInfo = getNewDocPaymentInfo(_parameter, createInst);
            createDocInfo.setTargetDocInst(createInst);
            int i = 0;
            for (final String docOid : docOids) {
                final Instance docInst = Instance.get(docOid);
                if (docInst.isValid()) {
                    try {
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
                        insert.execute();
                    } catch (final ParseException e) {
                        LOG.error("Catched ParseException", e);
                    }
                }
                i++;
            }
        }
        return new Return();
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
        final Instance targetInstance = Instance.get(_parameter.getParameterValue("createDocument"));
        final Instance docInstance = Instance.get(_parameter.getParameterValues("document")[selected]);

        if (docInstance.isValid() && targetInstance.isValid()) {
            try {
                final Object[] rateObj = getRateObject(_parameter, "rate", selected);
                final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                                BigDecimal.ROUND_HALF_UP);
                final BigDecimal rateUI = (BigDecimal) NumberFormatter.get().getFormatter()
                                .parse(_parameter.getParameterValues("rate")[selected]);
                final BigDecimal partial = (BigDecimal) NumberFormatter.get().getFormatter()
                                .parse(_parameter.getParameterValues("partial")[selected]);

                final DocPaymentInfo docInfo = getNewDocPaymentInfo(_parameter, docInstance);
                docInfo.setTargetDocInst(targetInstance);
                docInfo.getRateInfo4Target().setRate(rate);
                docInfo.getRateInfo4Target().setRateUI(rateUI);
                docInfo.setRateCrossTotal(partial);
                final Map<String, Object> map = getPositionUpdateMap(_parameter, docInfo);
                map.put(EFapsKey.FIELDUPDATE_USEIDX.getKey(), selected);
                list.add(map);
                final Map<String, Object> sum = getSumUpdateMap(_parameter, list, true);
                sum.put(EFapsKey.FIELDUPDATE_USEIDX.getKey(), 0);
                list.add(sum);
            } catch (final ParseException e) {
                LOG.error("Catched ParseException", e);
            }
        }
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
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
        final Instance targetInstance = Instance.get(_parameter.getParameterValue("createDocument"));
        final Instance docInstance = Instance.get(_parameter.getParameterValues("document")[selected]);

        if (docInstance.isValid() && targetInstance.isValid()) {
            try {
                final Object[] rateObj = getRateObject(_parameter, "rate", selected);
                final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                                BigDecimal.ROUND_HALF_UP);
                final BigDecimal rateUI = (BigDecimal) NumberFormatter.get().getFormatter()
                                .parse(_parameter.getParameterValues("rate")[selected]);
                final DocPaymentInfo docInfo = getNewDocPaymentInfo(_parameter, docInstance);
                docInfo.setTargetDocInst(targetInstance);
                docInfo.getRateInfo4Target().setRate(rate);
                docInfo.getRateInfo4Target().setRateUI(rateUI);

                final Map<String, Object> map = getPositionUpdateMap(_parameter, docInfo);
                map.put(EFapsKey.FIELDUPDATE_USEIDX.getKey(), selected);
                list.add(map);

                final Map<String, Object> sum = getSumUpdateMap(_parameter, list, true);
                sum.put(EFapsKey.FIELDUPDATE_USEIDX.getKey(), 0);
                list.add(sum);

            } catch (final ParseException e) {
                LOG.error("Catched ParseException", e);
            }
        }
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * @param _parameter parameter as passed by the eFasp API
     * @param _docInst instance of the document the map is wanted for
     * @param _addDocInfo add the autocomplete field also
     * @return map containing values for update
     * @throws EFapsException on error
     */
    protected Map<String, Object> getPositionUpdateMap(final Parameter _parameter,
                                                       final Instance _docInst)
        throws EFapsException
    {
        final Instance targetInstance = Instance.get(_parameter.getParameterValue("createDocument"));
        final DocPaymentInfo docInfo = getNewDocPaymentInfo(_parameter, _docInst);
        docInfo.setTargetDocInst(targetInstance);
        return getPositionUpdateMap(_parameter, docInfo);
    }

    /**
     * @param _parameter parameter as passed by the eFasp API
     * @param _docInfo DocPaymentInfo the map is wanted for
     * @param _addDocInfo add the autocomplete field also
     * @return map containing values for update
     * @throws EFapsException on error
     */
    protected Map<String, Object> getPositionUpdateMap(final Parameter _parameter,
                                                       final DocPaymentInfo _docInfo)
        throws EFapsException
    {
        final Map<String, Object> ret = new HashMap<>();
        final DecimalFormat frmt = _docInfo.getFormatter();
        final BigDecimal total4Doc = _docInfo.getCrossTotal4Target();
        final BigDecimal payments4Doc = _docInfo.getPaid4Target();
        ret.put("partial", frmt.format(total4Doc.subtract(payments4Doc)));
        ret.put("rate", _docInfo.getRateInfo4Target().getRateUIFrmt());
        ret.put("rate" + RateUI.INVERTEDSUFFIX, "" + _docInfo.getRateInfo4Target().isInvert());
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
            LOG.error("Catched ParseException", e);
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
            rate = (BigDecimal) RateFormatter.get().getFrmt4Rate().parse(_parameter.getParameterValues(_field)[_index]);
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
    protected BigDecimal getSum4Positions(final Parameter _parameter,
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
                LOG.debug("Catched ParseException", e);
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
        final List<Map<String, Object>> values = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();

        final Instance docInst = Instance.get(_parameter.getParameterValue("createDocument"));
        if (docInst.isValid()) {
            final DocPaymentInfo docInfo = getNewDocPaymentInfo(_parameter, docInst);
            docInfo.setTargetDocInst(docInst);
            map.put("createInfo", docInfo.getInfoField());
        }
        values.add(map);
        return new Return().put(ReturnValues.VALUES, values);
    }

    public DocPaymentInfo getNewDocPaymentInfo(final Parameter _parameter,
                                               final Instance _instance)
        throws EFapsException
    {
        return new DocPaymentInfo(_instance).setParameter(_parameter);
    }

}
