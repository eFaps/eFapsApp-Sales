package org.efaps.esjp.sales;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.document.DocumentSum;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: EventSchedule_Base.java $
 */
@EFapsUUID("89eb3b05-47a9-4327-96f9-108986f171b7")
@EFapsRevision("$Rev: 1$")
public class EventSchedule_Base
    extends DocumentSum
{

    public Return autoComplete4ScheduleDoc(final Parameter _parameter)
        throws EFapsException
    {
        final Map<Integer, String> types = analyseProperty(_parameter, "Type");
        final Map<Integer, String> statusGrps = analyseProperty(_parameter, "StatusGrp");
        final Map<Integer, String> status = analyseProperty(_parameter, "Status");
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        final Instance instContact = Instance.get(Context.getThreadContext().getSessionAttribute("oidContact").toString());
        if(instContact!=null && instContact.isValid()) {
            for (final Entry<Integer, String> typeEntry : types.entrySet()) {
                final Map<String, Map<String, String>> tmpMap = new TreeMap<String, Map<String, String>>();
                final Type type = Type.get(typeEntry.getValue());
                if (type != null) {
                    final QueryBuilder queryBldr = new QueryBuilder(type);
                    add2QueryBldr4AutoCompleteScheduledDoc(_parameter, queryBldr);
                    queryBldr.addWhereAttrEqValue(CISales.DocumentAbstract.Contact, instContact);
                    queryBldr.addWhereAttrMatchValue(CISales.DocumentAbstract.Name, input + "*");

                    if (statusGrps.containsKey(typeEntry.getKey())) {
                        final String statiStr = status.get(typeEntry.getKey());
                        final String[] statiAr = statiStr.split(";");
                        final List<Object> statusList = new ArrayList<Object>();
                        for (final String stati : statiAr) {
                            final Status stat = Status.find(statusGrps.get(typeEntry.getKey()), stati);
                            if (status != null) {
                                statusList.add(stat.getId());
                            }
                        }
                        queryBldr.addWhereAttrEqValue(CISales.DocumentAbstract.StatusAbstract, statusList.toArray());
                    }

                    final MultiPrintQuery multi = queryBldr.getPrint();
                    final SelectBuilder selrateCurrSymbol = new SelectBuilder().linkto(CISales.DocumentSumAbstract.RateCurrencyId).attribute(CIERP.Currency.Symbol);
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

                        final StringBuilder choice = new StringBuilder()
                                        .append(name).append(" - ")
                                        .append(Instance.get(oid).getType().getLabel()).append(" - ")
                                        .append(date.toString(DateTimeFormat.forStyle("S-").withLocale(
                                                        Context.getThreadContext().getLocale()))).append(" - ")
                                        .append(dueDate.toString(DateTimeFormat.forStyle("S-").withLocale(
                                                        Context.getThreadContext().getLocale()))).append(" - ")
                                        .append(rateCurrSymbol + total.toString());
                        final Map<String, String> map = new HashMap<String, String>();
                        map.put(EFapsKey.AUTOCOMPLETE_KEY.getKey(), oid);
                        map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), name);
                        map.put(EFapsKey.AUTOCOMPLETE_CHOICE.getKey(), choice.toString());
                        tmpMap.put(name, map);
                    }
                }
                list.addAll(tmpMap.values());

            }
        }
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    protected void add2QueryBldr4AutoCompleteScheduledDoc(final Parameter _parameter,
                                                          final QueryBuilder queryBldr)
        throws EFapsException
    {
        // TODO Auto-generated method stub
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
        final String oid = _parameter.getParameterValues("document")[selected];
        String name;
        BigDecimal netPrice = BigDecimal.ZERO;
        BigDecimal rateNetPrice = BigDecimal.ZERO;
        String symbol;
        String rateSymbol;
        if (oid != null && oid.length() > 0) {
            final SelectBuilder selSymbol = new SelectBuilder().linkto(CISales.DocumentSumAbstract.CurrencyId)
                            .attribute(CIERP.Currency.Symbol);
            final SelectBuilder selRateSymbol = new SelectBuilder().linkto(CISales.DocumentSumAbstract.RateCurrencyId)
                            .attribute(CIERP.Currency.Symbol);
            final PrintQuery print = new PrintQuery(oid);
            print.addAttribute(CISales.DocumentAbstract.Name, CISales.DocumentAbstract.Note);
            print.addAttribute(CISales.DocumentSumAbstract.CrossTotal,
                            CISales.DocumentSumAbstract.RateCrossTotal);
            print.addSelect(selSymbol, selRateSymbol);
            print.execute();
            name = print.getAttribute(CISales.DocumentAbstract.Name);
            netPrice = print.getAttribute(CISales.DocumentSumAbstract.CrossTotal);
            rateNetPrice = print.getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
            print.getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
            symbol = print.getSelect(selSymbol);
            rateSymbol = print.getSelect(selRateSymbol);
        } else {
            name = "";
            symbol = "";
            rateSymbol = "";
        }

        if (name.length() > 0) {
            map.put("netPrice", symbol + getNetPriceFmtStr(netPrice));
            map.put("rateNetPrice", rateSymbol + getNetPriceFmtStr(rateNetPrice));
            map.put("total", getTotalFmtStr(getTotal(_parameter)));
            map.put("documentAutoComplete", name);

            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        } else {
            map.put("documentAutocomplete", name);
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
            final StringBuilder js = new StringBuilder();
            js.append("document.getElementsByName('productAutoComplete')[").append(selected).append("].focus()");
            map.put(EFapsKey.FIELDUPDATE_JAVASCRIPT.getKey(), js.toString());
        }
        return retVal;
    }

    public BigDecimal getTotal(final Parameter _parameter)
        throws EFapsException
    {
        BigDecimal total = BigDecimal.ZERO;
        final String[] oids = _parameter.getParameterValues("document");
        for (int i = 0; i < oids.length; i++) {
            if (oids[i] != null && oids[i].length() > 0) {
                final PrintQuery print = new PrintQuery(oids[i]);
                print.addAttribute(CISales.DocumentSumAbstract.CrossTotal);
                print.execute();
                total = total.add(print.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.CrossTotal));
            }
        }
        return total;
    }

    @Override
    protected String getMaxNumber(final Parameter _parameter,
                                  final Type _type,
                                  final boolean _expandChild)
        throws EFapsException
    {
        String ret = null;
        final QueryBuilder queryBuilEventSche = new QueryBuilder(_type);
        queryBuilEventSche.addOrderByAttributeDesc(CIERP.EventScheduleAbstract.Name);
        final InstanceQuery queryEventSche = queryBuilEventSche.getQuery();
        queryEventSche.setIncludeChildTypes(_expandChild);
        queryEventSche.setLimit(1);
        final MultiPrintQuery multiEventShe = new MultiPrintQuery(queryEventSche.execute());
        multiEventShe.addAttribute(CIERP.EventScheduleAbstract.Name);
        multiEventShe.execute();
        if (multiEventShe.next()) {
            ret = multiEventShe.getAttribute(CIERP.EventScheduleAbstract.Name);
        }
        return ret;
    }

    protected String getNetPriceFmtStr(final BigDecimal netTotal)
        throws EFapsException
    {
        return NumberFormatter.get().getTwoDigitsFormatter().format(netTotal);
    }

    protected String getTotalFmtStr(final BigDecimal netTotal)
        throws EFapsException
    {
        return NumberFormatter.get().getTwoDigitsFormatter().format(netTotal);
    }

}
