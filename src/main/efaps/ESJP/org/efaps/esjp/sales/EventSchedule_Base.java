package org.efaps.esjp.sales;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIAdminCommon;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.sales.document.AbstractDocument_Base;
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
public class EventSchedule_Base extends AbstractDocument_Base
{


    public Return autoComplete4ScheduleDoc(final Parameter _parameter)
    throws EFapsException
    {

        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        autoCompleteDocs(_parameter, CISales.IncomingInvoice.uuid,
                        Status.find( CISales.IncomingInvoiceStatus.uuid, "Open").getId(), list);

        if (Type.get("Accounting_ExternalVoucher") != null) {
            //Accounting_ExternalVoucher
            //Accounting_ExternalVoucherStatus
            autoCompleteDocs(_parameter, UUID.fromString("612efbd7-8843-447f-bb44-7983a3e87a43"),
                            Status.find(UUID.fromString("1e95c195-f701-43e7-af90-7012779e18c9"), "Open").getId(), list);
        }

        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }


    protected void autoCompleteDocs(Parameter _parameter,
                                 UUID _typeUUID,
                                 long _status,
                                 List<Map<String, String>> _list)
    throws EFapsException
    {

        final Map<String, Map<String, String>> tmpMap = new TreeMap<String, Map<String, String>>();
        SelectBuilder selectContactName = new SelectBuilder().linkto(CISales.DocumentAbstract.Contact)
                        .attribute(CIContacts.Contact.Name);
        final String input = (String) _parameter.get(ParameterValues.OTHERS);

        final QueryBuilder queryBldr = new QueryBuilder(_typeUUID);
        queryBldr.addWhereAttrMatchValue(CISales.DocumentAbstract.Name, input + "*");
        queryBldr.addWhereAttrEqValue(CISales.DocumentAbstract.StatusAbstract, _status);

        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.DocumentAbstract.OID, CISales.DocumentAbstract.Name, CISales.DocumentAbstract.Date);
        multi.addSelect(selectContactName);
        multi.execute();
        while (multi.next()) {
            final String name = multi.<String>getAttribute(CISales.DocumentAbstract.Name);
            final String oid = multi.<String>getAttribute(CISales.DocumentAbstract.OID);
            final String contactName = multi.<String>getSelect(selectContactName);
            final DateTime date = multi.<DateTime>getAttribute(CISales.DocumentAbstract.Date);

            final Map<String, String> map = new HashMap<String, String>();
            map.put(EFapsKey.AUTOCOMPLETE_KEY.getKey(), oid);
            map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), name);
            map.put(EFapsKey.AUTOCOMPLETE_CHOICE.getKey(), name + "-" + Instance.get(oid).getType().getLabel() +
                            " - " + contactName + "-"
                     + date.toString(DateTimeFormat.forStyle("S-").withLocale(Context.getThreadContext().getLocale())));
            map.put("selectedDoc", oid);
            tmpMap.put(name, map);
        }
        _list.addAll(tmpMap.values());
    }

    /**
     * Generic method to get the listmap for update event.
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
        BigDecimal netPrice  = BigDecimal.ZERO;
        if (oid != null && oid.length() > 0) {

            final PrintQuery print = new PrintQuery(oid);
            print.addAttribute(CISales.DocumentAbstract.Name, CISales.DocumentAbstract.Note);
            print.addAttribute(CISales.DocumentSumAbstract.CrossTotal);

            print.execute();
            name = print.getAttribute(CISales.DocumentAbstract.Name);
            netPrice = print.getAttribute(CISales.DocumentSumAbstract.CrossTotal);

        }else {
            name = "";
        }

        if (name.length() > 0) {
            map.put("netPrice", getNetPriceFmtStr(netPrice));
            map.put("total", getTotalFmtStr(getTotal(_parameter)));
            map.put("documentAutoComplete", name);

            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        }else {
            map.put("documentAutocomplete", name);
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
            final StringBuilder js = new StringBuilder();
            js.append("document.getElementsByName('productAutoComplete')[").append(selected).append("].focus()");
            map.put(EFapsKey.FIELDUPDATE_JAVASCRIPT.getKey(), js.toString());
        }
        return retVal;
    }

    public BigDecimal getTotal(Parameter _parameter)
    throws EFapsException
    {
        BigDecimal total = BigDecimal.ZERO;
        String[] oids = _parameter.getParameterValues("document");
        for(int i = 0; i <  oids.length; i++){
            PrintQuery print = new PrintQuery(oids[i]);
            print.addAttribute(CISales.DocumentSumAbstract.CrossTotal);
            print.execute();
            total = total.add(print.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.CrossTotal));
        }
        return total;
    }

    @Override
    protected String getMaxNumber(final Type _type,
                                final boolean _expandChild)
        throws EFapsException
    {
        String ret = null;
        QueryBuilder queryBuilEventSche = new QueryBuilder(_type);
        queryBuilEventSche.addOrderByAttributeDesc(CIERP.EventScheduleAbstract.Name);
        InstanceQuery queryEventSche = queryBuilEventSche.getQuery();
        queryEventSche.setIncludeChildTypes(_expandChild);
        queryEventSche.setLimit(1);
        MultiPrintQuery multiEventShe = new MultiPrintQuery(queryEventSche.execute());
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
        return getTwoDigitsformater().format(netTotal);
    }

    protected String getTotalFmtStr(final BigDecimal netTotal)
        throws EFapsException
    {
        return getTwoDigitsformater().format(netTotal);
    }

    public class CreateSchedule

    {
        /**
         * Instance of the newly created schedule.
         */
        private final Instance instance;

        /**
         * Positions of the created Schedule.
         */
        private final List<Instance> positions = new ArrayList<Instance>();

        /**
         * Map can be used to pass values from one method to another.
         */
        private final Map<String, Object> values = new HashMap<String, Object>();

        /**
         * @param _instance Instance of the Schedule
         */
        public CreateSchedule(final Instance _instance)
        {
            this.instance = _instance;
        }
        /**
         * Getter method for the instance variable {@link #values}.
         *
         * @return value of instance variable {@link #values}
         */
        public Map<String, Object> getValues()
        {
            return this.values;
        }
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
         * Getter method for the instance variable {@link #positions}.
         *
         * @return value of instance variable {@link #positions}
         */
        public List<Instance> getPositions()
        {
            return this.positions;
        }
        /**
         * @param _instance Instance to add
         */
        public void addPosition(final Instance _instance)
        {
            this.positions.add(_instance);
        }
    }


}

