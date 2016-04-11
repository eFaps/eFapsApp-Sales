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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringEscapeUtils;
import org.efaps.admin.datamodel.ui.IUIValue;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.field.Field.Display;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.document.AbstractDocumentSum;
import org.efaps.esjp.sales.document.AbstractDocumentTax;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("89eb3b05-47a9-4327-96f9-108986f171b7")
@EFapsApplication("eFapsApp-Sales")
public abstract class EventSchedule_Base
    extends AbstractDocumentSum
{

    /**
     * Method to autoComplete Schedules Doc.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return autoComplete4ScheduleDoc(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final String input = (String) _parameter.get(ParameterValues.OTHERS);

        final Instance contact;
        if (containsProperty(_parameter, "ExtraParameter")) {
            contact = Instance.get(_parameter.getParameterValue(getProperty(_parameter, "ExtraParameter")));
        } else {
            contact = Instance.get("");
        }

        final List<Instance> query = new MultiPrint()
        {

            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
                throws EFapsException
            {
                if (contact != null && contact.isValid()) {
                    _queryBldr.addWhereAttrEqValue(CISales.DocumentAbstract.Contact, contact);
                }
                _queryBldr.addWhereAttrMatchValue(CISales.DocumentAbstract.Name, input + "*").setIgnoreCase(true);
            }
        } .getInstances(_parameter);

        final Map<String, Map<String, String>> tmpMap = new TreeMap<String, Map<String, String>>();
        final SelectBuilder selrateCurrSymbol = new SelectBuilder()
                        .linkto(CISales.DocumentSumAbstract.RateCurrencyId).attribute(CIERP.Currency.Symbol);
        if (!query.isEmpty()) {
            final MultiPrintQuery multi = new MultiPrintQuery(query);
            multi.addAttribute(CISales.DocumentAbstract.OID,
                            CISales.DocumentAbstract.Name,
                            CISales.DocumentAbstract.Date,
                            CISales.DocumentAbstract.DueDate,
                            CISales.DocumentSumAbstract.RateCrossTotal);
            multi.addSelect(selrateCurrSymbol);
            multi.execute();
            while (multi.next()) {
                final String name = multi.<String>getAttribute(CISales.DocumentAbstract.Name);
                final String oid = multi.<String>getAttribute(CISales.DocumentAbstract.OID);
                final DateTime date = multi.<DateTime>getAttribute(CISales.DocumentAbstract.Date);
                final DateTime dueDate = multi.<DateTime>getAttribute(CISales.DocumentAbstract.DueDate);
                final BigDecimal total = multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
                final String rateCurrSymbol = multi.<String>getSelect(selrateCurrSymbol);

                final DateTimeFormatter locale = DateTimeFormat.forStyle("S-").withLocale(
                                Context.getThreadContext().getLocale());
                final StringBuilder choice = new StringBuilder().append(name).append(" - ")
                                .append(Instance.get(oid).getType().getLabel()).append(" - ");
                if (date != null) {
                    choice.append(date.toString(locale)).append(" - ");
                }
                if (dueDate != null) {
                    choice.append(dueDate.toString(locale)).append(" - ");
                }
                choice.append(rateCurrSymbol + getTotalFmtStr(total));

                final Map<String, String> map = new HashMap<String, String>();
                map.put(EFapsKey.AUTOCOMPLETE_KEY.getKey(), oid);
                map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), name);
                map.put(EFapsKey.AUTOCOMPLETE_CHOICE.getKey(), choice.toString());
                tmpMap.put(name, map);
            }
        }

        list.addAll(tmpMap.values());
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * method to obtains total payments document out at the document.
     *
     * @param _instance of the Document.
     * @return new BigDecimal.
     * @throws EFapsException on error.
     */
    protected BigDecimal getPaymentDocumentOut4Doc(final Instance _instance)
        throws EFapsException
    {
        return BigDecimal.ZERO;
    }

    /**
     * Generic method to get the listmap for update event.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return map list for update event
     * @throws EFapsException on error
     */
    public Return updateFields4ScheduleDoc(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();

        final int selected = getSelectedRow(_parameter);
        final Instance docInst = Instance.get(_parameter.getParameterValues("document")[selected]);
        String name;
        BigDecimal netPrice = BigDecimal.ZERO;
        BigDecimal rateNetPrice = BigDecimal.ZERO;
        String symbol;
        String rateSymbol;
        String contactName;
        if (docInst.isValid()) {
            final SelectBuilder selSymbol = new SelectBuilder()
                            .linkto(CISales.DocumentSumAbstract.CurrencyId).attribute(CIERP.Currency.Symbol);
            final SelectBuilder selRateSymbol = new SelectBuilder()
                            .linkto(CISales.DocumentSumAbstract.RateCurrencyId).attribute(CIERP.Currency.Symbol);
            final SelectBuilder selContactNameSel = new SelectBuilder()
                .linkto(CISales.DocumentSumAbstract.Contact).attribute(CIContacts.ContactAbstract.Name);

            final PrintQuery print = new PrintQuery(docInst);
            print.addAttribute(CISales.DocumentAbstract.Name,
                            CISales.DocumentAbstract.Note,
                            CISales.DocumentSumAbstract.CrossTotal,
                            CISales.DocumentSumAbstract.RateCrossTotal);
            print.addSelect(selSymbol, selRateSymbol, selContactNameSel);
            print.execute();

            name = print.<String>getAttribute(CISales.DocumentAbstract.Name);
            netPrice = print.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.CrossTotal);
            rateNetPrice = print.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
            symbol = print.<String>getSelect(selSymbol);
            rateSymbol = print.<String>getSelect(selRateSymbol);
            contactName = print.<String>getSelect(selContactNameSel);
        } else {
            name = "";
            symbol = "";
            rateSymbol = "";
            contactName = "";
        }
        map.put("contactPos", contactName);
        if (name.length() > 0) {
            map.put("netPrice", symbol + getNetPriceFmtStr(netPrice) + " / "
                            + getNetPriceFmtStr(getPaymentDocumentOut4Doc(docInst)) + " / "
                            + getNetPriceFmtStr(getEventSchedule4Doc(docInst)));
            map.put("rateNetPrice", rateSymbol + getNetPriceFmtStr(rateNetPrice));
            map.put("amount4Schedule", getNetPriceFmtStr(netPrice.subtract(getPaymentDocumentOut4Doc(docInst))));
            map.put("total", getTotalFmtStr(getTotal(_parameter, docInst)));
            final StringBuilder script = new StringBuilder()
                .append("document.getElementsByName(\"retPerDet\")[").append(selected).append("].innerHTML=\"")
                .append(StringEscapeUtils.escapeEcmaScript(
                                AbstractDocumentTax.getSmallTaxField4Doc(_parameter, docInst).toString()))
                .append("\"");
            InterfaceUtils.appendScript4FieldUpdate(map, script);
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        } else {
            map.put("amount4Schedule", "");
            map.put("netPrice", "");
            map.put("rateNetPrice", "");
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        }
        return retVal;
    }

    /**
     * Method to obtains total of the event schedule to document.
     *
     * @param _instance of the document.
     * @return new BigDecimal.
     * @throws EFapsException on error.
     */
    protected BigDecimal getEventSchedule4Doc(final Instance _instance)
        throws EFapsException
    {
        return BigDecimal.ZERO;
    }

    /**
     * Obtains Total of the positions documents.
     *
     * @param _parameter Parameter as passed from the EFaps API.
     * @param _ignoreDoc Instance to document.
     * @return BigDecimal value.
     * @throws EFapsException on error.
     */
    public BigDecimal getTotal(final Parameter _parameter,
                               final Instance _ignoreDoc)
        throws EFapsException
    {
        final DecimalFormat formatter = NumberFormatter.get().getFormatter();
        BigDecimal total = BigDecimal.ZERO;
        final String[] oids = _parameter.getParameterValues("document");
        final String[] amounts = _parameter.getParameterValues("amount4Schedule");
        if (oids != null && oids.length > 0) {
            for (int i = 0; i < oids.length; i++) {
                final Instance docInst = Instance.get(oids[i]);
                if (docInst.isValid()) {
                    final PrintQuery print = new PrintQuery(docInst);
                    print.addAttribute(CISales.DocumentSumAbstract.CrossTotal);
                    print.execute();

                    BigDecimal amount = BigDecimal.ZERO;
                    if (_ignoreDoc != null && _ignoreDoc.isValid() && docInst.equals(_ignoreDoc)) {
                        amount = print.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.CrossTotal);
                    } else {
                        try {
                            if (!amounts[i].isEmpty()) {
                                amount = (BigDecimal) formatter.parse(amounts[i]);
                            }
                        } catch (final ParseException e) {
                            throw new EFapsException(EventSchedule.class, "Amount4Schedule.ParseException", e);
                        }
                    }
                    total = total.add(amount);
                }
            }
        }
        return total;
    }

    /**
     * Format BigDecimal.
     *
     * @param _netTotal to format.
     * @return NumberFormatter.
     * @throws EFapsException on error.
     */
    protected String getNetPriceFmtStr(final BigDecimal _netTotal)
        throws EFapsException
    {
        return NumberFormatter.get().getTwoDigitsFormatter().format(_netTotal);
    }

    /**
     * Format BigDecimal.
     *
     * @param _netTotal to format.
     * @return NumberFormatter.
     * @throws EFapsException on error.
     */
    protected String getTotalFmtStr(final BigDecimal _netTotal)
        throws EFapsException
    {
        return NumberFormatter.get().getTwoDigitsFormatter().format(_netTotal);
    }

    /**
     * Method to show command edit if case date is after day system.
     *
     * @param _parameter Parameter as passed from the EFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return validateSchedule4Date(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();

        if (_parameter.get(ParameterValues.INSTANCE) != null) {
            final PrintQuery print = new PrintQuery(_parameter.getInstance());
            print.addAttribute(CIERP.DocumentAbstract.Date);
            print.execute();

            final DateTime date = print.<DateTime>getAttribute(CIERP.DocumentAbstract.Date);
            if (date.isAfter(new DateTime().withTime(0, 0, 0, 0))
                            || date.equals(new DateTime().withTime(0, 0, 0, 0))) {
                ret.put(ReturnValues.TRUE, true);
            }
        }

        return ret;
    }

    /**
     * Get JavaScript.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return java script.
     * @throws EFapsException on error.
     */
    public Return getJavaScript4UIValue(final Parameter _parameter)
        throws EFapsException
    {
        final StringBuilder js = new StringBuilder();
        js.append("<script type=\"text/javascript\">\n");
        final IUIValue fieldValue = (IUIValue) _parameter.get(ParameterValues.UIOBJECT);
        final String[] oids = _parameter.getParameterValues("selectedRow");
        final DecimalFormat formatter = NumberFormatter.get().getTwoDigitsFormatter();
        if (oids != null && oids.length > 0) {
            final List<Map<String, Object>> values = new ArrayList<>();
            final List<Instance> instances = new ArrayList<>();
            for (final String oid : oids) {
                instances.add(Instance.get(oid));
            }
            final SelectBuilder selSymbol = new SelectBuilder()
                .linkto(CISales.DocumentSumAbstract.CurrencyId).attribute(CIERP.Currency.Symbol);
            final SelectBuilder selRateSymbol = new SelectBuilder()
                .linkto(CISales.DocumentSumAbstract.RateCurrencyId).attribute(CIERP.Currency.Symbol);
            final SelectBuilder selContactNameSel = new SelectBuilder()
                .linkto(CISales.DocumentSumAbstract.Contact).attribute(CIContacts.ContactAbstract.Name);

            final MultiPrintQuery multi = new MultiPrintQuery(instances);
            multi.addAttribute(CISales.DocumentAbstract.Name,
                            CISales.DocumentAbstract.Note,
                            CISales.DocumentSumAbstract.CrossTotal,
                            CISales.DocumentSumAbstract.RateCrossTotal);
            multi.addSelect(selSymbol, selRateSymbol, selContactNameSel);
            multi.setEnforceSorted(true);
            multi.execute();
            while (multi.next()) {
                final Instance docInst = multi.getCurrentInstance();
                final String name = multi.<String>getAttribute(CISales.DocumentAbstract.Name);
                final BigDecimal crossTotal = multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.CrossTotal);
                final BigDecimal rateCrossTotal = multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
                final String symbol = multi.<String>getSelect(selSymbol);
                final String rateSymbol = multi.<String>getSelect(selRateSymbol);
                final String contactName = multi.<String>getSelect(selContactNameSel);

                final Map<String, Object> map = new HashMap<>();
                values.add(map);
                map.put("contactPos", contactName);
                map.put("document", new String[] { docInst.getOid(), name });
                map.put("amount4Schedule", formatter.format(crossTotal));
                map.put("rateNetPrice", rateSymbol + formatter.format(rateCrossTotal));
                map.put("netPrice", symbol + formatter.format(crossTotal) + " / "
                                    + formatter.format(getPaymentDocumentOut4Doc(docInst)) + " / "
                                    + formatter.format(getEventSchedule4Doc(docInst)));
            }

            final StringBuilder script = new StringBuilder();
            int i = 0;
            for (final Instance inst : instances) {
                script.append("document.getElementsByName(\"retPerDet\")[").append(i).append("].innerHTML=\"")
                    .append(StringEscapeUtils.escapeEcmaScript(
                            AbstractDocumentTax.getSmallTaxField4Doc(_parameter, inst).toString()))
                     .append("\";");
                i++;
            }

            js.append(getTableRemoveScript(_parameter, "positionTable", false, false))
                            .append(getTableAddNewRowsScript(_parameter, "positionTable", values,
                                            script, false, false, new HashSet<String>()));

        } else if (Display.HIDDEN.equals(fieldValue.getDisplay())) {

            final SelectBuilder selDoc = new SelectBuilder()
                            .linkto(CISales.PaymentSchedulePosition.Document).instance();
            final SelectBuilder selDocName = new SelectBuilder()
                            .linkto(CISales.PaymentSchedulePosition.Document).attribute(CIERP.DocumentAbstract.Name);
            final SelectBuilder selDocRateCrossTotal = new SelectBuilder()
                            .linkto(CISales.PaymentSchedulePosition.Document)
                            .attribute(CISales.DocumentSumAbstract.RateCrossTotal);
            final SelectBuilder selDocCrossTotal = new SelectBuilder()
                            .linkto(CISales.PaymentSchedulePosition.Document)
                            .attribute(CISales.DocumentSumAbstract.CrossTotal);
            final SelectBuilder selDocSymbol = new SelectBuilder()
                            .linkto(CISales.PaymentSchedulePosition.Document)
                            .linkto(CISales.DocumentSumAbstract.CurrencyId).attribute(CIERP.Currency.Symbol);
            final SelectBuilder selDocRateSymbol = new SelectBuilder()
                            .linkto(CISales.PaymentSchedulePosition.Document)
                            .linkto(CISales.DocumentSumAbstract.RateCurrencyId).attribute(CIERP.Currency.Symbol);

            final Instance instance = _parameter.getInstance();
            final Map<Instance, Map<String, Object>> valuesTmp = new LinkedHashMap<>();
            if (instance != null && instance.isValid()) {
                final SelectBuilder selContact = new SelectBuilder().linkto(CISales.PaymentSchedule.Contact).instance();
                final PrintQuery print = new PrintQuery(instance);
                print.addSelect(selContact);
                print.executeWithoutAccessCheck();

                final QueryBuilder queryBldr = new QueryBuilder(CISales.PaymentSchedulePosition);
                queryBldr.addWhereAttrEqValue(CISales.PaymentSchedulePosition.EventScheduleAbstractLink, instance);
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addSelect(selDoc, selDocName, selDocCrossTotal,
                                selDocRateCrossTotal, selDocSymbol, selDocRateSymbol);
                multi.addAttribute(CISales.PaymentSchedulePosition.DocumentDesc,
                                CISales.PaymentSchedulePosition.NetPrice);
                multi.executeWithoutAccessCheck();
                while (multi.next()) {
                    final Instance docInst = multi.<Instance>getSelect(selDoc);
                    final BigDecimal rateNetPrice = multi.<BigDecimal>getSelect(selDocRateCrossTotal);
                    final BigDecimal netPrice = multi.<BigDecimal>getSelect(selDocCrossTotal);
                    final String symbol = multi.<String>getSelect(selDocSymbol);
                    final String rateSymbol = multi.<String>getSelect(selDocRateSymbol);

                    final Map<String, Object> map;
                    if (!valuesTmp.containsKey(docInst)) {
                        map = new HashMap<>();
                        valuesTmp.put(docInst, map);
                        map.put("document",
                                        new String[] { docInst.getOid(), multi.<String>getSelect(selDocName) });
                        map.put("documentDesc",
                                        multi.<String>getAttribute(CISales.PaymentSchedulePosition.DocumentDesc));
                        map.put("amount4Schedule",
                                        getNetPriceFmtStr(multi
                                               .<BigDecimal>getAttribute(CISales.PaymentSchedulePosition.NetPrice)));
                        map.put("rateNetPrice", rateSymbol + getNetPriceFmtStr(rateNetPrice));
                        map.put("netPrice", symbol + getNetPriceFmtStr(netPrice) + " / "
                                        + getNetPriceFmtStr(getPaymentDocumentOut4Doc(docInst)) + " / "
                                        + getNetPriceFmtStr(getEventSchedule4Doc(docInst)));
                    }
                }
                js.append(getTableRemoveScript(_parameter, "positionTable", false, false))
                                .append(getTableAddNewRowsScript(_parameter, "positionTable", valuesTmp.values(),
                                                getOnCompleteScript(_parameter), false, false, new HashSet<String>()));
            }

        }
        js.append("</script>\n");
        final Return ret = new Return();
        ret.put(ReturnValues.SNIPLETT, js.toString());
        return ret;
    }
}
