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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.document.DocumentSum;
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
@EFapsRevision("$Rev: 1$")
public abstract class EventSchedule_Base
    extends DocumentSum
{
    /**
     * Instance to the Contact.
     */
    public static final String CONTACT_SESSIONKEY = EventSchedule.class.getName() +  ".ContactSessionKey";

    public Return autoComplete4ScheduleDoc(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final String input = (String) _parameter.get(ParameterValues.OTHERS);

        final Instance contact = (Instance) Context.getThreadContext()
                        .getSessionAttribute(EventSchedule.CONTACT_SESSIONKEY);

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

        }.getInstances(_parameter);

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
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();

        final int selected = getSelectedRow(_parameter);
        final Instance docInst = Instance.get(_parameter.getParameterValues("document")[selected]);
        String name;
        BigDecimal netPrice = BigDecimal.ZERO;
        BigDecimal rateNetPrice = BigDecimal.ZERO;
        String symbol;
        String rateSymbol;
        if (docInst.isValid()) {
            final SelectBuilder selSymbol = new SelectBuilder()
                            .linkto(CISales.DocumentSumAbstract.CurrencyId).attribute(CIERP.Currency.Symbol);
            final SelectBuilder selRateSymbol = new SelectBuilder()
                            .linkto(CISales.DocumentSumAbstract.RateCurrencyId).attribute(CIERP.Currency.Symbol);

            final PrintQuery print = new PrintQuery(docInst);
            print.addAttribute(CISales.DocumentAbstract.Name,
                            CISales.DocumentAbstract.Note,
                            CISales.DocumentSumAbstract.CrossTotal,
                            CISales.DocumentSumAbstract.RateCrossTotal);
            print.addSelect(selSymbol, selRateSymbol);
            print.execute();

            name = print.<String>getAttribute(CISales.DocumentAbstract.Name);
            netPrice = print.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.CrossTotal);
            rateNetPrice = print.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
            symbol = print.<String>getSelect(selSymbol);
            rateSymbol = print.<String>getSelect(selRateSymbol);
        } else {
            name = "";
            symbol = "";
            rateSymbol = "";
        }

        if (name.length() > 0) {
            map.put("netPrice", symbol + getNetPriceFmtStr(netPrice) + " / "
                            + getNetPriceFmtStr(getPaymentDocumentOut4Doc(docInst)) + " / "
                            + getNetPriceFmtStr(getEventSchedule4Doc(docInst)));
            map.put("rateNetPrice", rateSymbol + getNetPriceFmtStr(rateNetPrice));
            map.put("amount4Schedule", getNetPriceFmtStr(netPrice.subtract(getPaymentDocumentOut4Doc(docInst))));
            map.put("total", getTotalFmtStr(getTotal(_parameter, docInst)));
            map.put("documentAutoComplete", name);
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        } else {
            map.put("documentAutocomplete", name);
            map.put("amount4Schedule", "");
            map.put("netPrice", "");
            map.put("rateNetPrice", "");
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
            final StringBuilder js = new StringBuilder();
            js.append("document.getElementsByName('documentAutoComplete')[").append(selected).append("].focus()");
            map.put(EFapsKey.FIELDUPDATE_JAVASCRIPT.getKey(), js.toString());
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
}
