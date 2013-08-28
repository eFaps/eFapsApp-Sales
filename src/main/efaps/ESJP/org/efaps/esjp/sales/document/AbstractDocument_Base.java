/*
 * Copyright 2003 - 2009 The eFaps Team
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

package org.efaps.esjp.sales.document;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.datamodel.ui.RateUI;
import org.efaps.admin.datamodel.ui.UIInterface;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.admin.ui.field.Field;
import org.efaps.admin.ui.field.Field.Display;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.contacts.Contacts;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.sales.Calculator;
import org.efaps.esjp.sales.Calculator_Base;
import org.efaps.esjp.sales.ICalculatorConfig;
import org.efaps.esjp.sales.Payment;
import org.efaps.esjp.sales.Payment_Base;
import org.efaps.esjp.sales.Payment_Base.OpenAmount;
import org.efaps.esjp.sales.PriceUtil;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.SalesSettings;
import org.efaps.ui.wicket.models.objects.UIForm;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;
import org.efaps.util.cache.CacheReloadException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: AbstractDocument_Base.java 3674 2010-01-28 18:52:35Z jan.moxter
 *          $
 */
@EFapsUUID("b3b70ce7-16d0-4425-8ddd-b667cfd3329a")
@EFapsRevision("$Rev$")
public abstract class AbstractDocument_Base
    extends CommonDocument
    implements ICalculatorConfig
{

    /**
     * Key used to store the instance of the current Currency in the session.
     */
    public static final String CURRENCYINST_KEY = "org.efaps.esjp.sales.document.AbstractDocument.CurrencyInstance";

    /**
     * Key used to store the list of calculators in the session.
     */
    public static final String CALCULATOR_KEY = "org.efaps.esjp.sales.document.AbstractDocument.CalculatorKey";

    /**
     * Key used to store the target mode for the Document in the session.
     */
    public static final String TARGETMODE_DOC_KEY = "org.efaps.esjp.sales.document.AbstractDocument.TargeModeKey";

    /**
     * Method must be called on opening the form containing positions to
     * initialise a new positions calculator cache!
     *
     * @param _parameter Parameter as passed by the eFaps API for esjp
     * @return new Return
     * @throws EFapsException on error
     */
    public Return activatePositionsCalculator(final Parameter _parameter)
        throws EFapsException
    {
        Context.getThreadContext().setSessionAttribute(AbstractDocument_Base.CALCULATOR_KEY,
                        new ArrayList<Calculator>());
        return new Return();
    }

    /**
     * Method to get a formater.
     *
     * @return a formater
     * @throws EFapsException on error
     */
    protected DecimalFormat getTwoDigitsformater()
        throws EFapsException
    {
        return getFormater(2, 2);
    }

    /**
     * Method to get a formater.
     *
     * @return a formater
     * @throws EFapsException on error
     */
    protected DecimalFormat getZeroDigitsformater()
        throws EFapsException
    {
        return getFormater(0, 0);
    }

    /**
     * @return a formater used to format bigdecimal for the user interface
     * @param _maxFrac maximum Faction, null to deactivate
     * @param _minFrac minimum Faction, null to activate
     * @throws EFapsException on error
     */
    public DecimalFormat getFormater(final Integer _minFrac,
                                     final Integer _maxFrac)
        throws EFapsException
    {
        final DecimalFormat formater = (DecimalFormat) NumberFormat.getInstance(Context.getThreadContext().getLocale());
        if (_maxFrac != null) {
            formater.setMaximumFractionDigits(_maxFrac);
        }
        if (_minFrac != null) {
            formater.setMinimumFractionDigits(_minFrac);
        }
        formater.setRoundingMode(RoundingMode.HALF_UP);
        formater.setParseBigDecimal(true);
        return formater;
    }

    /**
     * @param _calc calculator the format is wanted for
     * @return Decimal Format
     * @throws EFapsException on error
     */
    protected DecimalFormat getDigitsformater4UnitPrice(final Calculator _calc)
        throws EFapsException
    {
        final DecimalFormat formater = (DecimalFormat) NumberFormat.getInstance(Context.getThreadContext().getLocale());
        if (_calc.isLongDecimal() != 2) {
            formater.setMaximumFractionDigits(_calc.isLongDecimal());
            formater.setMinimumFractionDigits(_calc.isLongDecimal());
        } else {
            formater.setMaximumFractionDigits(2);
            formater.setMinimumFractionDigits(2);
        }
        formater.setRoundingMode(RoundingMode.HALF_UP);
        formater.setGroupingUsed(true);
        formater.setParseBigDecimal(true);
        return formater;
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return Return containing the value formated
     * @throws EFapsException on error
     */
    public Return formatQuantity(final Parameter _parameter)
        throws EFapsException
    {
        final TargetMode mode = (TargetMode) _parameter.get(ParameterValues.ACCESSMODE);
        final StringBuilder js = new StringBuilder();
        final Return retVal = new Return();
        if (mode.equals(TargetMode.VIEW) || mode.equals(TargetMode.PRINT)) {
            final FieldValue value = (FieldValue) _parameter.get(ParameterValues.UIOBJECT);
            if (value.getDisplay().equals(Display.READONLY)) {
                final BigDecimal dec = (BigDecimal) value.getValue();
                js.append(dec.stripTrailingZeros().toPlainString());
            } else {
                js.append(value.getValue());
            }
            retVal.put(ReturnValues.VALUES, js.toString());
        }
        return retVal;
    }

    /**
     * Used by the AutoCompleteField used in the select doc form for
     * DeliveryNote.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete
     * @throws EFapsException on error.
     */
    public Return autoComplete4DeliveryNote(final Parameter _parameter)
        throws EFapsException
    {
        return autoComplete4Doc(_parameter, CISales.DeliveryNote.uuid, null);
    }

    /**
     * Used by the AutoCompleteField used in the select doc form for
     * IncomingInvoices.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4IncomingInvoice(final Parameter _parameter)
        throws EFapsException
    {

        return autoComplete4Doc(_parameter, CISales.IncomingInvoice.uuid, null);
    }

    /**
     * Used by the AutoCompleteField used in the select doc form for Invoices.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4Invoice(final Parameter _parameter)
        throws EFapsException
    {
        final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final Status status;
        if (props.containsKey("Status")) {
            status = Status.find(CISales.InvoiceStatus, (String) props.get("Status"));
        } else {
            status = null;
        }
        return autoComplete4Doc(_parameter, CISales.Invoice.uuid, status);
    }

    /**
     * Used by the AutoCompleteField used in the select doc form for
     * OrderInbound.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4OrderInbound(final Parameter _parameter)
        throws EFapsException
    {
        final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final Status status;
        if (props.containsKey("Status")) {
            status = Status.find(CISales.OrderInboundStatus.uuid, (String) props.get("Status"));
        } else {
            status = Status.find(CISales.OrderInboundStatus.uuid, "Open");
        }
        return autoComplete4Doc(_parameter, CISales.OrderInbound.uuid, status);
    }

    /**
     * Used by the AutoCompleteField used in the select doc form for
     * OrderOutbound.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4OrderOutbound(final Parameter _parameter)
        throws EFapsException
    {
        final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final Status status;
        if (props.containsKey("Status")) {
            status = Status.find(CISales.OrderOutboundStatus.uuid, (String) props.get("Status"));
        } else {
            status = Status.find(CISales.OrderOutboundStatus.uuid, "Open");
        }
        return autoComplete4Doc(_parameter, CISales.OrderOutbound.uuid, status);
    }

    /**
     * Used by the AutoCompleteField used in the select doc form for Quotations.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4Quotation(final Parameter _parameter)
        throws EFapsException
    {
        return autoComplete4Doc(_parameter, CISales.Quotation.uuid, null);
    }

    /**
     * Used by the AutoCompleteField used in the select doc form for Receipts.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4Receipt(final Parameter _parameter)
        throws EFapsException
    {
        return autoComplete4Doc(_parameter, CISales.Receipt.uuid, null);
    }

    /**
     * Used by the AutoCompleteField used in the select doc form for Receipts.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4RecievingTicket(final Parameter _parameter)
        throws EFapsException
    {
        return autoComplete4Doc(_parameter, CISales.RecievingTicket.uuid, null);
    }

    /**
     * Used by the AutoCompleteField used in the select doc form for Receipts.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4CreditNote(final Parameter _parameter)
        throws EFapsException
    {
        return autoComplete4Doc(_parameter, CISales.CreditNote.uuid, null);
    }

    /**
     * Used by the AutoCompleteField used in the select doc form for CostSheets.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4CostSheet(final Parameter _parameter)
        throws EFapsException
    {
        return autoComplete4Doc(_parameter, CISales.CostSheet.uuid, null);
    }


    /**
     * Used by the AutoCompleteField used in the select doc form for
     * OrderOutbound.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4Reservation(final Parameter _parameter)
        throws EFapsException
    {
        final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final Status status;
        if (props.containsKey("Status")) {
            status = Status.find(CISales.ReservationStatus .uuid, (String) props.get("Status"));
        } else {
            status = Status.find(CISales.ReservationStatus.uuid, "Open");
        }
        return autoComplete4Doc(_parameter, CISales.Reservation.uuid, status);
    }


    /**
     * Generic method to get a list of documents.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @param _typeUUID UUID of the type to be searched.
     * @param _status status used as additional filter, <code>null</code> to
     *            deactivated
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    protected Return autoComplete4Doc(final Parameter _parameter,
                                      final UUID _typeUUID,
                                      final Status _status)
        throws EFapsException
    {
        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);

        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, Map<String, String>> tmpMap = new TreeMap<String, Map<String, String>>();
        final QueryBuilder queryBldr = new QueryBuilder(_typeUUID);
        add2QueryBldr(_parameter, queryBldr);
        queryBldr.addWhereAttrMatchValue(CISales.DocumentAbstract.Name, input + "*").setIgnoreCase(true);
        if (_status != null) {
            queryBldr.addWhereAttrEqValue(CISales.DocumentAbstract.StatusAbstract, _status.getId());
        }
        final String key = properties.containsKey("Key") ? (String) properties.get("Key") : "OID";
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(key);
        multi.addAttribute(CISales.DocumentAbstract.OID, CISales.DocumentAbstract.Name, CISales.DocumentAbstract.Date);
        multi.execute();
        while (multi.next()) {
            final String name = multi.<String> getAttribute(CISales.DocumentAbstract.Name);
            final String oid = multi.<String> getAttribute(CISales.DocumentAbstract.OID);
            final DateTime date = multi.<DateTime> getAttribute(CISales.DocumentAbstract.Date);

            final Map<String, String> map = new HashMap<String, String>();
            map.put(EFapsKey.AUTOCOMPLETE_KEY.getKey(), multi.getAttribute(key).toString());
            map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), name);
            map.put(EFapsKey.AUTOCOMPLETE_CHOICE.getKey(),
                            name + " - " + date.toString(DateTimeFormat.forStyle("S-").withLocale(
                                                            Context.getThreadContext().getLocale())));
            map.put("selectedDoc", oid);
            tmpMap.put(name, map);
        }
        list.addAll(tmpMap.values());
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * Used by the AutoCompleteField used in the select contact.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4Contact(final Parameter _parameter)
        throws EFapsException
    {
        final Contacts contacts = new Contacts();
        return contacts.autoComplete4Contact(_parameter);
    }

    /**
     * @param _parameter Parameter as passeb by the eFaps API
     * @return update map
     * @throws EFapsException on error
     */
    public Return updateFields4Contact(final Parameter _parameter)
        throws EFapsException
    {
        final Contacts contacts = new Contacts() {
            @Override
            public String getFieldValue4Contact(final Instance _instance)
                throws EFapsException
            {
                return AbstractDocument_Base.this.getFieldValue4Contact(_instance);
            }
        };
        return contacts.updateFields4Contact(_parameter);
    }

    protected void add2QueryBldr(final Parameter _parameter,
                                 final QueryBuilder _queryBldr)
        throws EFapsException
    {
        // TODO Auto-generated method stub

    }

    /**
     * Used by the update event used in the select doc form for DeliveryNote.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return map list for update event
     * @throws EFapsException on error
     */
    public Return updateFields4DeliveryNote(final Parameter _parameter)
        throws EFapsException
    {
        return updateFields4Doc(_parameter);
    }

    /**
     * Used by the update event used in the select doc form for IncomingInvoice.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return map list for update event
     * @throws EFapsException on error
     */
    public Return updateFields4IncomingInvoice(final Parameter _parameter)
        throws EFapsException
    {
        return updateFields4Doc(_parameter);
    }

    /**
     * Used by the update event used in the select doc form for IncomingInvoice.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return map list for update event
     * @throws EFapsException on error
     */
    public Return updateFields4RecievingTicket(final Parameter _parameter)
        throws EFapsException
    {
        return updateFields4Doc(_parameter);
    }

    /**
     * Used by the update event used in the select doc form for Invoice.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return map list for update event
     * @throws EFapsException on error
     */
    public Return updateFields4Invoice(final Parameter _parameter)
        throws EFapsException
    {
        return updateFields4Doc(_parameter);
    }

    /**
     * Used by the update event used in the select doc form for OrderInbound.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return map list for update event
     * @throws EFapsException on error
     */
    public Return updateFields4OrderInbound(final Parameter _parameter)
        throws EFapsException
    {
        return updateFields4Doc(_parameter);
    }

    /**
     * Used by the update event used in the select doc form for OrderOutbound.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return map list for update event
     * @throws EFapsException on error
     */
    public Return updateFields4OrderOutbound(final Parameter _parameter)
        throws EFapsException
    {
        return updateFields4Doc(_parameter);
    }


    /**
     * Used by the update event used in the select doc form for OrderOutbound.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return map list for update event
     * @throws EFapsException on error
     */
    public Return updateFields4Reservation(final Parameter _parameter)
        throws EFapsException
    {
        return updateFields4Doc(_parameter);
    }

    /**
     * Used by the update event used in the select doc form for Quotation.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return map list for update event
     * @throws EFapsException on error
     */
    public Return updateFields4Quotation(final Parameter _parameter)
        throws EFapsException
    {
        return updateFields4Doc(_parameter);
    }

    /**
     * Used by the update event used in the select doc form for Receipt.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return map list for update event
     * @throws EFapsException on error
     */
    public Return updateFields4Receipt(final Parameter _parameter)
        throws EFapsException
    {
        return updateFields4Doc(_parameter);
    }

    /**
     * Used by the update event used in the select doc form for CreditNote.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return map list for update event
     * @throws EFapsException on error
     */
    public Return updateFields4CreditNote(final Parameter _parameter)
        throws EFapsException
    {
        return updateFields4Doc(_parameter);
    }

    /**
     * Used by the update event used in the select doc form for CostSheet.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return map list for update event
     * @throws EFapsException on error
     */
    public Return updateFields4CostSheet(final Parameter _parameter)
        throws EFapsException
    {
        return updateFields4Doc(_parameter);
    }

    /**
     * Generic method to get the listmap for update event.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return map list for update event
     * @throws EFapsException on error
     */
    protected Return updateFields4Doc(final Parameter _parameter)
        throws EFapsException
    {
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);

        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();
        final String input = properties.containsKey("input") ? (String) properties.get("input") : "selectedDoc";
        final String oid = _parameter.getParameterValue(input);
        if (oid != null && oid.length() > 0) {
            final PrintQuery print = new PrintQuery(oid);
            print.addAttribute(CIERP.DocumentAbstract.Name, CIERP.DocumentAbstract.Date);
            final SelectBuilder sel = SelectBuilder.get().type().label();
            print.addSelect(sel);
            print.execute();

            final String field = properties.containsKey("field") ? (String) properties.get("field") : "info";
            final StringBuilder bldr = new StringBuilder();
            bldr.append(print.getSelect(sel)).append(" - ")
                .append(print.getAttribute(CIERP.DocumentAbstract.Name)).append(" - ")
                .append(print.<DateTime> getAttribute(CIERP.DocumentAbstract.Date).toString(
                               DateTimeFormat.forStyle("S-").withLocale(Context.getThreadContext().getLocale())));
            map.put(field, StringEscapeUtils.escapeEcmaScript(bldr.toString()));

            map.put(EFapsKey.FIELDUPDATE_JAVASCRIPT.getKey(), getCleanJS(_parameter));
            list.add(map);
        }
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * Method to get a small script that cleans out all the field minus the
     * current one.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return script
     */
    protected String getCleanJS(final Parameter _parameter)
    {
        final Field field = (Field) _parameter.get(ParameterValues.UIOBJECT);

        final StringBuilder js = new StringBuilder();
        js.append("inputs = document.getElementsByTagName('INPUT');")
            .append("for (i=0;i<inputs.length;i++) {")
            .append("if (inputs[i].type == 'text' && inputs[i].name != '").append(field.getName())
            .append("AutoComplete') {")
            .append("inputs[i].value='';")
            .append("}")
            .append("}");
        return js.toString();
    }

    /**
     * @param _parameter Paraemter as passed by the eFasp API
     * @return List map for the update event
     * @throws EFapsException on error
     */
    public Return updateFields4Uom(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();

        final int selected = getSelectedRow(_parameter);
        final List<Calculator> calcList = analyseTable(_parameter, null);
        if (calcList.size() > 0) {
            final Calculator cal = calcList.get(selected);

            final Long uomID = Long.parseLong(_parameter.getParameterValues("uoM")[selected]);
            final UoM uom = Dimension.getUoM(uomID);
            final BigDecimal up = cal.getProductPrice().getCurrentPrice().multiply(new BigDecimal(uom.getNumerator()))
                            .divide(new BigDecimal(uom.getDenominator()));

            cal.setUnitPrice(up);
            map.put("quantity", cal.getQuantityStr());
            map.put("netunitprice", cal.getNetUnitPriceFmtStr(getDigitsformater4UnitPrice(cal)));
            map.put("netprice", cal.getNetPriceFmtStr(getTwoDigitsformater()));
            map.put("nettotal", getNetTotalFmtStr(calcList));
            list.add(map);

            retVal.put(ReturnValues.VALUES, list);
        }
        return retVal;
    }

    /**
     * Method to get the value for the field directly under the Contact.
     *
     * @param _instance Instacne of the contact
     * @return String for the field
     * @throws EFapsException on error
     */
    protected String getFieldValue4Contact(final Instance _instance)
        throws EFapsException
    {
        final Contacts contacts = new Contacts();
        return contacts.getFieldValue4Contact(_instance);
    }

    /**
     * Method is called from a hidden field to include javascript in the form.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return Return containing the javascript
     * @throws EFapsException on error
     */
    public Return getJavaScriptUIValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        retVal.put(ReturnValues.SNIPLETT, getJavaScript(_parameter));
        return retVal;
    }

    /**
     * Method to get the javascript.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return javascript
     * @throws EFapsException on error
     */
    protected String getJavaScript(final Parameter _parameter)
        throws EFapsException
    {
        return getJavaScript(_parameter, false);
    }

    /**
     * Method to get a javascript used to fill fields in a form.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _setStyle must the style be set
     * @return javascript
     * @throws EFapsException on error
     */
    protected String getJavaScript(final Parameter _parameter,
                                   final boolean _setStyle)
        throws EFapsException
    {
        final StringBuilder js = new StringBuilder();
        js.append("<script type=\"text/javascript\">\n");
        if (_setStyle) {
            js.append("Wicket.Event.add(window, \"domready\", function(event) {")
                .append(" var nt = document.getElementsByName('netTotal')[0];")
                .append(" if(typeof nt=='undefined') {")
                .append("   nt = document.getElementsByName('netTotal4Read')[0];")
                .append(" }")
                .append(" if(typeof nt!='undefined') {")
                .append(" while (nt.nodeName != 'TABLE') {")
                .append(" nt = nt.parentNode;")
                .append("}")
                .append("nt.style.marginLeft='auto';")
                .append("nt.style.width='2%';")
                .append(" }});");
        }

        js.append("Wicket.Event.add(window, \"domready\", function(event) {\n")
                .append("var cn = document.getElementsByName('rateCurrencyData');")
                .append("if (cn.length > 0) { ")
                .append(" cn[0].appendChild(document.createTextNode('");
        final Instance currency4Invoice = Sales.getSysConfig().getLink(SalesSettings.CURRENCY4INVOICE);
        final Instance baseCurrency = Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);
        if (currency4Invoice.equals(baseCurrency)) {
            js.append("1").append("'));");
        } else {
            js.append(getRateCurrencyData(_parameter, currency4Invoice, baseCurrency)).append("'));");
        }
        js.append("}}); ");
        js.append("var ele = document.createElement('input');")
                .append("var attr = document.createAttribute('type');")
                .append("attr.nodeValue = 'hidden';")
                .append("ele.setAttributeNode(attr);")
                .append("require([\"dojo/query\"],function(query){")
                .append("dojo.query('.eFapsContentDiv')[0].appendChild(ele);")
                .append("});\n");
        js.append(updateRateFields(_parameter, currency4Invoice, baseCurrency)).append("\n");
        final FieldValue command = (FieldValue) _parameter.get(ParameterValues.UIOBJECT);
        final TargetMode mode = command.getTargetMode();
        Context.getThreadContext().setSessionAttribute(AbstractDocument_Base.TARGETMODE_DOC_KEY, mode);

        boolean copy = _parameter.getParameterValue("selectedRow") != null;
        if (copy || _parameter.getParameterValue("selectedDoc") != null || _parameter.getCallInstance() != null) {
            final Instance instCall = _parameter.getCallInstance();
            final Instance instance = getInstance4Derived(_parameter);
            if (instance.isValid()) {
                // in case of copy check if it is really a copy (meaning the same type will be created)
                final Object object = _parameter.get(ParameterValues.CLASS);
                if (copy && object instanceof UIForm) {
                    final UIForm uiForm = (UIForm) object;
                    final Type type = uiForm.getCommand().getTargetCreateType();
                    if (type != null && !instance.getType().equals(type)) {
                        copy = false;
                    }
                }
                js.append("ele.value='").append(instance.getOid()).append("';")
                                .append("ele.name='").append(copy ? "copy" : "derived").append("';")
                                .append(getSetValuesString(_parameter, instance));
            } else if (instCall != null && instCall.isValid()
                            && instCall.getType().isKindOf(CISales.DocumentAbstract.getType())) {
                js.append(getSetValuesString(_parameter, instCall));
            }
        }
        js.append("</script>\n");
        return js.toString();
    }



    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return the Instance used for the derived on the javascript field. returns
     */
    protected Instance getInstance4Derived(final Parameter _parameter)
    {
        Instance ret = Instance.get(_parameter.getParameterValue("selectedDoc"));
        if (!ret.isValid()) {
            ret = Instance.get( _parameter.getParameterValue("selectedRow"));
        }
        return ret;
    }


    protected String getRateCurrencyData(final Parameter _parameter,
                                         final Instance _instanceCurrency,
                                         final Instance _baseCurrency)
        throws EFapsException
    {

        final BigDecimal[] rates = new PriceUtil().getRates(_parameter, _instanceCurrency, _baseCurrency);
        final BigDecimal rateValue = rates[3].setScale(3, BigDecimal.ROUND_HALF_UP);
        final DecimalFormat formatter = Calculator_Base.getFormatInstance();
        formatter.setMaximumFractionDigits(3);
        formatter.setMinimumFractionDigits(3);
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        return formatter.format(rateValue);
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _newInst      new Instance
     * @param _currentInst  current Instance
     * @return html for field
     * @throws EFapsException on error
     */
    protected String updateRateFields(final Parameter _parameter,
                                      final Instance _newInst,
                                      final Instance _currentInst)
        throws EFapsException
    {
        final StringBuilder js = new StringBuilder();
        if (!_newInst.equals(_currentInst)) {
            final BigDecimal[] rates = new PriceUtil().getRates(_parameter, _newInst, _currentInst);
            js.append(getSetFieldValue(0, "rate", rates[3].toString()))
                .append("\n")
                .append(getSetFieldValue(0, "rate" + RateUI.INVERTEDSUFFIX,"" + (rates[3].compareTo(rates[0]) != 0)))
                .append("\n");
        }
        return js.toString();
    }

    /**
     * Method to get the javascript part for setting the values.
     *
     * @param _parameter Paramter as passed by the eFaps API
     * @param _instance instance to be copied
     * @return javascript
     * @throws EFapsException on error
     */
    protected String getSetValuesString(final Parameter _parameter,
                                        final Instance _instance)
        throws EFapsException
    {

        final StringBuilder js = new StringBuilder();
        final PrintQuery print = new PrintQuery(_instance);
        print.addAttribute(CISales.DocumentSumAbstract.RateNetTotal,
                        CISales.DocumentSumAbstract.RateCrossTotal,
                        CISales.DocumentSumAbstract.Rate,
                        CIERP.DocumentAbstract.Name,
                        CIERP.DocumentAbstract.Note);
        final SelectBuilder selContOID = new SelectBuilder().linkto(CIERP.DocumentAbstract.Contact).oid();
        final SelectBuilder selContName = new SelectBuilder().linkto(CIERP.DocumentAbstract.Contact)
                        .attribute(CIContacts.Contact.Name);
        print.addSelect(selContOID, selContName);
        print.execute();

        final BigDecimal netTotal = print.<BigDecimal> getAttribute(CISales.DocumentSumAbstract.RateNetTotal);
        final BigDecimal crossTotal = print.<BigDecimal> getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
        final String contactOid = print.<String> getSelect(selContOID);
        final String contactName = print.<String> getSelect(selContName);
        final String contactData = getFieldValue4Contact(Instance.get(contactOid));
        final String note = print.<String> getAttribute(CIERP.DocumentAbstract.Note);
        final Object[] rates = print.<Object[]> getAttribute(CISales.DocumentSumAbstract.Rate);

        final DecimalFormat formater = getTwoDigitsformater();
        final DecimalFormat formaterZero = getZeroDigitsformater();
        final DecimalFormat formaterSysConf = getDigitsformater4UnitPrice(new Calculator(_parameter, this));

        final StringBuilder currency = new StringBuilder();
        BigDecimal rate = null;
        BigDecimal[] ratesCur = null;
        if (rates != null) {
            final Instance currency4Invoice = Sales.getSysConfig().getLink(SalesSettings.CURRENCY4INVOICE);
            final Instance baseCurrency = Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);
            final Instance instanceDerived = getInstance4Derived(_parameter);
            boolean derived = false;
            if (instanceDerived.isValid()) {
                derived = true;
            }
            final Instance newInst = Instance.get(CIERP.Currency.getType(), rates[2].toString());
            Context.getThreadContext().setSessionAttribute(AbstractDocument_Base.CURRENCYINST_KEY, newInst);
            ratesCur = new PriceUtil().getExchangeRate(new DateTime().toDateMidnight().toDateTime(), newInst);

            if ((rates[2].equals(rates[3]) && !currency4Invoice.equals(baseCurrency) && !derived)
                            || !rates[2].equals(rates[3])) {
                currency.append(getSetFieldValue(0, "rateCurrencyId", "" + ((Long) rates[2])))
                        .append("\n");
                currency.append(getSetFieldValue(0, "rateCurrencyData", ratesCur[1].toString()))
                        .append(getSetFieldValue(0, "rate", ratesCur[1].toString()))
                        .append("\n")
                        .append(getSetFieldValue(0, "rate" + RateUI.INVERTEDSUFFIX,
                                        "" + new CurrencyInst(newInst).isInvert()))
                        .append("\n");
                if (!rates[2].equals(rates[3])) {
                    rate = (BigDecimal) rates[1];
                }
            }
        }

        js.append("function setValue() {\n")
            .append(currency)
            .append(getSetFieldValue(0, "contact", contactOid)).append("\n")
            .append(getSetFieldValue(0, "contactAutoComplete", contactName)).append("\n")
            .append(getSetFieldValue(0, "contactData", contactData)).append("\n")
            .append(getSetFieldValue(0, "netTotal", netTotal == null
                            ? BigDecimal.ZERO.toString() : formater.format(netTotal))).append("\n")
            .append(getSetFieldValue(0, "crossTotal", netTotal == null
                            ? BigDecimal.ZERO.toString() : formater.format(crossTotal))).append("\n")
            .append(getSetFieldValue(0, "note", note)).append("\n")
            .append(addAdditionalFields(_parameter, _instance)).append("\n")
            .append("}\n");

        final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionAbstract);
        queryBldr.addWhereAttrEqValue(CISales.PositionAbstract.DocumentAbstractLink, _instance.getId());
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.PositionAbstract.PositionNumber,
                        CISales.PositionAbstract.ProductDesc,
                        CISales.PositionAbstract.Quantity,
                        CISales.PositionAbstract.UoM,
                        CISales.PositionSumAbstract.CrossUnitPrice,
                        CISales.PositionSumAbstract.NetUnitPrice,
                        CISales.PositionSumAbstract.DiscountNetUnitPrice,
                        CISales.PositionSumAbstract.CrossPrice,
                        CISales.PositionSumAbstract.NetPrice,
                        CISales.PositionSumAbstract.RateNetUnitPrice,
                        CISales.PositionSumAbstract.RateDiscountNetUnitPrice,
                        CISales.PositionSumAbstract.RateNetPrice,
                        CISales.PositionSumAbstract.Tax,
                        CISales.PositionSumAbstract.Discount);
        final SelectBuilder selProdOID = new SelectBuilder().linkto(CISales.PositionSumAbstract.Product).oid();
        final SelectBuilder selProdName = new SelectBuilder().linkto(CISales.PositionSumAbstract.Product)
                        .attribute(CIProducts.ProductAbstract.Name);
        final SelectBuilder selProdDim = new SelectBuilder().linkto(CISales.PositionSumAbstract.Product)
                        .attribute(CIProducts.ProductAbstract.Dimension);
        multi.addSelect(selProdOID, selProdName, selProdDim);
        multi.execute();

        final Map<Integer, Map<String, String>> values = new TreeMap<Integer, Map<String, String>>();

        while (multi.next()) {
            final Map<String, String> map = new HashMap<String, String>();

            final BigDecimal netUnitPrice = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.NetUnitPrice);
            final BigDecimal discountNetUnitPrice = multi.
                            <BigDecimal>getAttribute(CISales.PositionSumAbstract.DiscountNetUnitPrice);
            final BigDecimal netPrice = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.NetPrice);
            final BigDecimal rateNetUnitPrice = multi.<BigDecimal>getAttribute(
                            CISales.PositionSumAbstract.RateNetUnitPrice);
            final BigDecimal rateDiscountNetUnitPrice = multi.
                            <BigDecimal>getAttribute(CISales.PositionSumAbstract.RateDiscountNetUnitPrice);
            final BigDecimal rateNetPrice = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.RateNetPrice);
            final BigDecimal discount = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.Discount);

            map.put("oid", multi.getCurrentInstance().getOid());
            map.put("quantity", formaterZero
                            .format(multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.Quantity)));
            map.put("productAutoComplete", multi.<String>getSelect(selProdName));
            map.put("product",  multi.<String>getSelect(selProdOID));
            map.put("productDesc",  multi.<String>getAttribute(CISales.PositionSumAbstract.ProductDesc));
            map.put("uoM", getUoMFieldStr(multi.<Long>getAttribute(CISales.PositionSumAbstract.UoM),
                                            multi.<Long>getSelect(selProdDim)));
            if (TargetMode.EDIT.equals(Context.getThreadContext()
                            .getSessionAttribute(AbstractDocument_Base.TARGETMODE_DOC_KEY))) {
                map.put("netUnitPrice", rateNetUnitPrice == null || netUnitPrice == null
                                ? BigDecimal.ZERO.toString()
                                : formaterSysConf.format(rate != null ? rateNetUnitPrice : netUnitPrice));
                map.put("discountNetUnitPrice", rateDiscountNetUnitPrice == null || discountNetUnitPrice == null
                                ? BigDecimal.ZERO.toString()
                                : formaterSysConf.format(rate != null ? rateDiscountNetUnitPrice : discountNetUnitPrice));
                map.put("netPrice", rateNetPrice == null || netPrice == null
                                ? BigDecimal.ZERO.toString()
                                : formater.format(rate != null ? rateNetPrice : netPrice));
            } else {
                map.put("netUnitPrice", netUnitPrice == null
                                ? BigDecimal.ZERO.toString()
                                : formaterSysConf.format(rate != null ? netUnitPrice.divide(rate,
                                                BigDecimal.ROUND_HALF_UP) : netUnitPrice));
                map.put("discountNetUnitPrice", discountNetUnitPrice == null
                                ? BigDecimal.ZERO.toString()
                                : formaterSysConf.format(rate != null ? discountNetUnitPrice.divide(rate,
                                                BigDecimal.ROUND_HALF_UP)
                                                : discountNetUnitPrice));
                map.put("netPrice", netPrice == null
                                ? BigDecimal.ZERO.toString()
                                : formater.format(rate != null ? netPrice.divide(rate, BigDecimal.ROUND_HALF_UP)
                                                : netPrice));
                map.put("discount", discount == null
                                ? BigDecimal.ZERO.toString()
                                : formater.format(discount));
            }
            values.put(multi.<Integer>getAttribute(CISales.PositionSumAbstract.PositionNumber), map);
        }

        final Set<String> noEscape = new HashSet<String>();
        noEscape.add("uoM");

        add2SetValuesString4Postions(_parameter, values, noEscape);
        js.append("require([\"dojo/domReady!\"], function(){\n")
            .append("setValue();\n");
        if (TargetMode.EDIT.equals(Context.getThreadContext()
                        .getSessionAttribute(AbstractDocument_Base.TARGETMODE_DOC_KEY))) {
            js.append(getSetFieldValuesScript(_parameter, values.values(), noEscape));
        } else {
            js.append(getTableRemoveScript(_parameter, "positionTable", false, false))
                .append(getTableAddNewRowsScript(_parameter, "positionTable", values.values(),
                            getOnCompleteScript(_parameter), false, false, noEscape));
        }
        js.append(getDomReadyScript(_parameter, _instance))
            .append(" });\n");
        return js.toString();
    }

    /**
     * Method for acon complete script.
     *
     * @param _parameter Paramter as passed by the eFaps API
     * @return new StringBuilder with the additional fields.
     * @throws EFapsException on error
     */
    protected StringBuilder getOnCompleteScript(final Parameter _parameter)
        throws EFapsException
    {
        return new StringBuilder();
    }


    /**
     * @param _parameter Paramter as passed by the eFaps API
     * @param _values values to be added to
     * @param _noEscape no escape fields
     * @throws EFapsException
     */
    protected void add2SetValuesString4Postions(final Parameter _parameter,
                                                final Map<Integer, Map<String, String>> _values,
                                                final Set<String> _noEscape)
        throws EFapsException
    {
        // to be used by implementations
    }

    /**
     * Method to set additional fields for the document.
     *
     * @param _parameter Paramter as passed by the eFaps API
     * @param _instance Instance of the document.
     * @return new StringBuilder with the additional fields.
     * @throws EFapsException on error
     */
    protected StringBuilder addAdditionalFields(final Parameter _parameter,
                                                final Instance _instance)
        throws EFapsException
    {
        return new StringBuilder();
    }

    /**
     * Add additional JavaScript to the Script that will be executed after the
     * DOM of the Html-Document was loaded. Can be used by implementations.
     *
     * @param _parameter Paramter as passed by the eFaps API
     * @param _instance Instance of the derived Document
     * @return String containing valid Javascript
     * @throws EFapsException on error
     */
    protected String getDomReadyScript(final Parameter _parameter,
                                       final Instance _instance)
        throws EFapsException
    {
        return "";
    }

    /**
     * Auto-complete for the field with products.
     *
     * @param _parameter parameter from eFaps.
     * @return List to be rendered for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4Product(final Parameter _parameter)
        throws EFapsException
    {
        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        if (!input.isEmpty()) {
            final boolean nameSearch = Character.isDigit(input.charAt(0));
            final String typeStr = (String) properties.get("Type");
            Type type;
            if (typeStr != null) {
                type = Type.get(typeStr);
            } else {
                type = CIProducts.ProductAbstract.getType();
            }

            final QueryBuilder queryBldr = new QueryBuilder(type);
            if (nameSearch) {
                queryBldr.addWhereAttrMatchValue(CIProducts.ProductAbstract.Name, input + "*").setIgnoreCase(true);
            } else {
                queryBldr.addWhereAttrMatchValue(CIProducts.ProductAbstract.Description, input + "*")
                                .setIgnoreCase(true);
            }
            queryBldr.addWhereAttrEqValue(CIProducts.ProductAbstract.Active, true);

            if (properties.containsKey("InStock")) {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Products_VirtualInventoryStock);
                final AttributeQuery attrQuery = attrQueryBldr
                                .getAttributeQuery(CISales.Products_VirtualInventoryStock.Product);
                queryBldr.addWhereAttrInQuery(CIProducts.ProductAbstract.ID, attrQuery);
            }

            final String exclude = (String) properties.get("ExcludeTypes");
            if (exclude != null) {
                final String[] typesArray = exclude.split(";");
                for (int x = 0; x < typesArray.length; x++) {
                    final QueryBuilder queryBldr2 = new QueryBuilder(Type.get(typesArray[x]));
                    final AttributeQuery attrQuery = queryBldr2.getAttributeQuery(CIProducts.ProductAbstract.ID);
                    queryBldr.addWhereAttrNotInQuery(CIProducts.ProductAbstract.ID, attrQuery);
                }
            }

            final Map<String, Map<String, String>> sortMap = new TreeMap<String, Map<String, String>>();
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CIProducts.ProductAbstract.Name,
                            CIProducts.ProductAbstract.Description,
                            CIProducts.ProductAbstract.Dimension);
            multi.execute();
            while (multi.next()) {
                final String name = multi.<String> getAttribute(CIProducts.ProductAbstract.Name);
                final String desc = multi.<String> getAttribute(CIProducts.ProductAbstract.Description);
                final String oid = multi.getCurrentInstance().getOid();
                final String choice;
                if (nameSearch) {
                    choice = name + " - " + desc;
                } else {
                    choice = desc + " - " + name;
                }
                final Map<String, String> map = new HashMap<String, String>();
                map.put("eFapsAutoCompleteKEY", oid);
                map.put("eFapsAutoCompleteVALUE", name);
                map.put("eFapsAutoCompleteCHOICE", choice);
                map.put("productDesc", desc);
                map.put("uoM", getUoMFieldStr(multi.<Long> getAttribute(CIProducts.ProductAbstract.Dimension)));
                map.put("discount", "0");
                sortMap.put(choice, map);
            }
            list.addAll(sortMap.values());
        }

        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }


    protected String getUoMFieldStr(final long _selected,
                                    final long _dimId)
        throws CacheReloadException
    {
        final Dimension dim = Dimension.get(_dimId);
        final StringBuilder js = new StringBuilder();

        js.append("new Array('").append(_selected).append("'");

        for (final UoM uom : dim.getUoMs()) {
            js.append(",'").append(uom.getId()).append("','").append(uom.getName()).append("'");
        }
        js.append(")");
        return js.toString();
    }

    protected String getUoMFieldStr(final long _dimId)
        throws CacheReloadException
    {
        final Dimension dim = Dimension.get(_dimId);
        return getUoMFieldStr(dim.getBaseUoM().getId(), _dimId);
    }

    /**
     * @param _uoMId id of the UoM
     * @return Field String
     */
    protected String getUoMFieldStrByUoM(final long _uoMId)
        throws CacheReloadException
    {
        return getUoMFieldStr(_uoMId, Dimension.getUoM(_uoMId).getDimId());
    }

    /**
     * Method to evaluate the selected row.
     *
     * @param _parameter paaremter
     * @return number of selected row.
     */
    protected int getSelectedRow(final Parameter _parameter)
    {
        int ret = 0;
        final String value = _parameter.getParameterValue("eFapsRowSelectedRow");
        if (value != null && value.length() > 0) {
            ret = Integer.parseInt(value);
        }
        return ret;
    }

    /**
     * Analyse the table to calculate.
     *
     * @param _parameter Parameter as passed by the eFaps API for esjp
     * @param _row4priceFromDB the price for the given row will be retrieved
     *            from the DB, null means none, -1 means all
     * @return List of Calculators
     * @throws EFapsException on error
     */
    @SuppressWarnings("unchecked")
    public List<Calculator> analyseTable(final Parameter _parameter,
                                         final Integer _row4priceFromDB)
        throws EFapsException
    {
        final List<Calculator> ret = new ArrayList<Calculator>();
        final String[] quantities = _parameter.getParameterValues("quantity");
        String[] discounts = _parameter.getParameterValues("discount");
        String[] unitPrices = _parameter.getParameterValues("netUnitPrice");
        if (unitPrices == null && quantities != null) {
            unitPrices = new String[quantities.length];
            Arrays.fill(unitPrices, "");
        }
        if (discounts == null && quantities != null) {
            discounts = new String[quantities.length];
            Arrays.fill(discounts, "");
        }
        final String[] oids = _parameter.getParameterValues("product");
        final boolean withoutTax = "true".equals(_parameter.getParameterValue("withoutVAT"));

        final List<Calculator> oldCalcs = (List<Calculator>) Context.getThreadContext().getSessionAttribute(
                        AbstractDocument_Base.CALCULATOR_KEY);

        if (quantities != null) {
            for (int i = 0; i < quantities.length; i++) {
                Calculator oldCalc = null;
                if (oldCalcs != null && oldCalcs.size() > 0 && oldCalcs.size() > i) {
                    oldCalc = oldCalcs.get(i);
                }
                if (quantities[i].length() > 0 || discounts[i].length() > 0 || unitPrices[i].length() > 0
                                || oids[i].length() > 0) {
                    final boolean priceFromDB = _row4priceFromDB != null
                                    && (_row4priceFromDB.equals(i) || _row4priceFromDB.equals(-1));
                    final Calculator calc = getCalculator(_parameter, oldCalc, oids[i], quantities[i], unitPrices[i],
                                    discounts[i], priceFromDB);
                    calc.setWithoutTax(withoutTax);
                    ret.add(calc);
                } else {
                    ret.add(new Calculator());
                }
            }
        }
        Context.getThreadContext().setSessionAttribute(AbstractDocument_Base.CALCULATOR_KEY, ret);
        return ret;
    }

    /**
     * @param _parameter Parameter parameter as passe dfrom the eFaps API
     * @param _oldCalc calculator
     * @param _oid oid of the product
     * @param _quantity quantity
     * @param _unitPrice unit price
     * @param _discount discount
     * @param _priceFromDB must the price set from DB
     * @throws EFapsException on error
     * @return new Calculator
     */
    public Calculator getCalculator(final Parameter _parameter,
                                    final Calculator _oldCalc,
                                    final String _oid,
                                    final String _quantity,
                                    final String _unitPrice,
                                    final String _discount,
                                    final boolean _priceFromDB)
        throws EFapsException
    {

        return new Calculator(_parameter, _oldCalc, _oid, _quantity, _unitPrice, _discount, _priceFromDB, this);
    }

    /**
     * @return the type name used in SystemConfiguration
     */
    protected String getTypeName4SystemConfiguration()
    {
        return CISales.DocumentAbstract.getType().getName();
    }

    /**
     * Method to get formated String representation of the cross total for a
     * list of Calculators.
     *
     * @param _calcList list of Calculator the net total is wanted for
     * @return formated String representation of the cross total
     * @throws EFapsException on error
     */
    @Deprecated
    protected String getCrossTotalFmtStr(final List<Calculator> _calcList)
        throws EFapsException
    {
        return getTwoDigitsformater().format(getCrossTotal(_calcList));
    }

    /**
     * Method to get String representation of the cross total for a list of Calculators.
     *
     * @param _calcList list of Calculator the net total is wanted for
     * @return String representation of the cross total
     * @throws EFapsException on error
     */
    @Deprecated
    protected String getCrossTotalStr(final List<Calculator> _calcList)
        throws EFapsException
    {
        return getCrossTotal(_calcList).toString();
    }

    /**
     * Method to get the cross total for a list of Calculators.
     *
     * @param _calcList list of Calculator the net total is wanted for
     * @return the cross total
     * @throws EFapsException on error
     */
    @Deprecated
    protected BigDecimal getCrossTotal(final List<Calculator> _calcList)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        for (final Calculator calc : _calcList) {
            ret = ret.add(calc.getCrossPrice());
        }
        return ret;
    }

    /**
     * Method to get formated String representation of the net total for a
     * list of Calculators.
     *
     * @param _calcList list of Calculator the net total is wanted for
     * @return formated String representation of the net total
     * @throws EFapsException on error
     */
    @Deprecated
    protected String getNetTotalFmtStr(final List<Calculator> _calcList)
        throws EFapsException
    {
        return getTwoDigitsformater().format(getNetTotal(_calcList));
    }

    /**
     * Method to get String representation of the net total for a list of Calculators.
     *
     * @param _calcList list of Calculator the net total is wanted for
     * @return String representation of the net total
     * @throws EFapsException on error
     */
    @Deprecated
    protected String getNetTotalStr(final List<Calculator> _calcList)
        throws EFapsException
    {
        return getNetTotal(_calcList).toString();
    }

    /**
     * Method to get the net total for a list of Calculators.
     *
     * @param _calcList list of Calculator the net total is wanted for
     * @return the net total
     * @throws EFapsException on error
     */
    @Deprecated
    protected BigDecimal getNetTotal(final List<Calculator> _calcList)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        for (final Calculator calc : _calcList) {
            ret = ret.add(calc.getNetPrice());
        }
        return ret;
    }

    /**
     * Method to get the base cross total for a list of Calculators.
     *
     * @param _calcList list of Calculator the net total is wanted for
     * @return the base cross total
     * @throws EFapsException on error
     */
    @Deprecated
    protected BigDecimal getBaseCrossTotal(final List<Calculator> _calcList)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        for (final Calculator calculator : _calcList) {
            ret = ret.add(calculator.getProductCrossPrice().getBasePrice());
        }
        return ret;
    }

    /**
     * Method to get the maximum for a value from the database.
     * @param _parameter Parameter as passed by the eFaps API for esjp
     * @param _type type to search for
     * @param _expandChild expand childs
     * @return maximum
     * @throws EFapsException on error
     */
    protected String getMaxNumber(final Parameter _parameter,
                                  final Type _type,
                                  final boolean _expandChild)
        throws EFapsException
    {
        String ret = null;
        final QueryBuilder queryBuilder = new QueryBuilder(_type);
        queryBuilder.addOrderByAttributeDesc(CIERP.DocumentAbstract.Name);
        final InstanceQuery query = queryBuilder.getQuery();
        query.setIncludeChildTypes(_expandChild);
        query.setLimit(1);
        final MultiPrintQuery multi = new MultiPrintQuery(query.execute());
        multi.addAttribute(CIERP.DocumentAbstract.Name);
        multi.execute();
        if (multi.next()) {
            ret = multi.getAttribute(CIERP.DocumentAbstract.Name);
        }
        return ret;
    }

    /**
     * Method to get the value for the name.
     *
     * @param _parameter Parameter as passed by the eFaps API for esjp
     * @return Return containing the value
     * @throws EFapsException on error
     */
    public Return getNameFieldValueUI(final Parameter _parameter)
        throws EFapsException
    {
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String type = (String) properties.get("Type");
        final String includeChildTypes = (String) properties.get("IncludeChildTypes");

        String number = getMaxNumber(_parameter, Type.get(type), !"false".equalsIgnoreCase(includeChildTypes));
        if (number == null) {
            number = "001-000001";
        } else {
            // get the numbers after the first "-"
            final Pattern pattern = Pattern.compile("(?<=-)\\d*");
            final Matcher matcher = pattern.matcher(number);
            if (matcher.find()) {
                final String numTmp = matcher.group();
                final int length = numTmp.length();
                final Integer numInt = Integer.parseInt(numTmp) + 1;
                final NumberFormat nf = NumberFormat.getInstance();
                nf.setMinimumIntegerDigits(length);
                nf.setMaximumIntegerDigits(length);
                nf.setGroupingUsed(false);
                number = number.substring(0, number.indexOf("-") + 1) + nf.format(numInt);
            }
        }
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, number);
        return retVal;
    }

    /**
     * Called from the field with the rate currency for a document. Returning a
     * dropdown with all currencies. The default is set inside the
     * SystemConfiguration for sales.
     *
     * @param _parameter Parameter as passed by the eFaps API for esjp
     * @return a dropdown with all currency
     * @throws EFapsException on error
     */
    public Return rateCurrencyFieldValueUI(final Parameter _parameter)
        throws EFapsException
    {
        final FieldValue fieldValue = (FieldValue) _parameter.get(ParameterValues.UIOBJECT);
        final QueryBuilder queryBldr = new QueryBuilder(CIERP.Currency);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIERP.Currency.Name);
        multi.execute();
        final Map<String, Long> values = new TreeMap<String, Long>();
        while (multi.next()) {
            values.put(multi.<String> getAttribute(CIERP.Currency.Name), multi.getCurrentInstance().getId());
        }
        final Instance baseInst = Sales.getSysConfig().getLink(SalesSettings.CURRENCY4INVOICE);
        Context.getThreadContext().setSessionAttribute(AbstractDocument_Base.CURRENCYINST_KEY, baseInst);
        final StringBuilder html = new StringBuilder();
        html.append("<select ").append(UIInterface.EFAPSTMPTAG)
                        .append(" name=\"").append(fieldValue.getField().getName()).append("\" size=\"1\">");
        for (final Entry<String, Long> entry : values.entrySet()) {
            html.append("<option value=\"").append(entry.getValue()).append("\"");
            if (entry.getValue().equals(baseInst.getId())) {
                html.append(" selected=\"selected\" ");
            }
            html.append(">").append(entry.getKey()).append("</option>");
        }
        html.append("</select>");
        final Return retVal = new Return();
        retVal.put(ReturnValues.SNIPLETT, html.toString());
        return retVal;
    }

    /**
     * Method to add extra fields to update when the currency change.
     *
     * @param _parameter as passed from eFaps API.
     * @param _calculators with the values.
     * @return StringBuilder.
     */
    protected StringBuilder addFields4RateCurrency(final Parameter _parameter,
                                                   final List<Calculator> _calculators)
        throws EFapsException
    {
        return new StringBuilder();
    }

    /**
     * Render a checkbox to disable the application of taxes used by Forms
     * during billing.
     *
     * @param _parameter Parameter as passed by the eFaps API for esjp
     * @return Html sniplett
     */
    public Return withoutVATFieldValue(final Parameter _parameter)
    {
        final Return ret = new Return();
        ret.put(ReturnValues.SNIPLETT, "<input " + UIInterface.EFAPSTMPTAG
                        + " type=\"checkbox\" name=\"withoutVAT\" value=\"true\" />");
        ret.put(ReturnValues.TRUE, true);
        return ret;
    }

    /**
     * Render a script to set the focus to another field.
     *
     * @param _parameter Parameter as passed by the eFaps API for esjp
     * @return javascript sniplett
     */
    public Return getJS4SelectDocumentForm(final Parameter _parameter)
    {
        final StringBuilder js = new StringBuilder();
        js.append("<script type=\"text/javascript\">")
            .append("Wicket.Event.add(window, \"domready\", function(event) {")
            .append("inputs = document.getElementsByTagName('INPUT');")
            .append("for (i=0;i<inputs.length;i++) {")
            .append("inputs[i].blur();")
            .append("}")
            .append("var ele = document.createElement('input');")
            .append("var attr = document.createAttribute('type');")
            .append("attr.nodeValue = 'hidden';")
            .append("ele.setAttributeNode(attr);")
            .append("require([\"dojo/query\"],function(query){")
            .append("dojo.query('.eFapsContentDiv')[0].appendChild(ele);")
            .append("ele.name='selectedDoc';")
            .append("});")
            .append(" });")
            .append("</script>");
        final Return retVal = new Return();
        retVal.put(ReturnValues.SNIPLETT, js.toString());
        return retVal;
    }

    /**
     * Method to set the openAmount into the session cache.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _calcList List of <code>Calculator</code>
     * @throws EFapsException on error
     */
    protected void setOpenAmount(final Parameter _parameter,
                                 final List<Calculator> _calcList)
        throws EFapsException
    {
        final Instance curInst = (Instance) Context.getThreadContext().getSessionAttribute(
                        AbstractDocument_Base.CURRENCYINST_KEY);
        final OpenAmount openAmount = new Payment().new OpenAmount(new CurrencyInst(curInst), getCrossTotal(_calcList),
                        new PriceUtil().getDateFromParameter(_parameter));
        Context.getThreadContext().setSessionAttribute(Payment_Base.OPENAMOUNT_SESSIONKEY, openAmount);
    }

    /**
     * Method to render a drop-down field containing all warehouses.
     *
     * @param _parameter Parameter as passed from eFaps.
     * @return Return containing a SNIPPLET.
     * @throws EFapsException on error.
     */
    public Return getStorageFieldValueUI(final Parameter _parameter)
        throws EFapsException
    {
        final FieldValue fieldValue = (FieldValue) _parameter.get(ParameterValues.UIOBJECT);
        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.StorageAbstract);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIProducts.StorageAbstract.ID, CIProducts.StorageAbstract.Name);
        multi.execute();

        final Map<String, Long> values = new TreeMap<String, Long>();
        while (multi.next()) {
            values.put(multi.<String> getAttribute("Name"), multi.<Long> getAttribute("ID"));
        }
        // Sales_Configuration
        final Instance warehouse = SystemConfiguration.get(UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f"))
                        .getLink("DefaultWarehouse");

        final StringBuilder html = new StringBuilder();
        html.append("<select name=\"").append(fieldValue.getField().getName()).append("\" size=\"1\">");
        for (final Entry<String, Long> entry : values.entrySet()) {
            html.append("<option value=\"").append(entry.getValue());
            if (entry.getValue().equals(warehouse.getId())) {
                html.append("\" selected=\"selected");
            }
            html.append("\">").append(entry.getKey()).append("</option>");
        }
        html.append("</select>");
        final Return retVal = new Return();
        retVal.put(ReturnValues.SNIPLETT, html.toString());
        return retVal;
    }

    /**
     * Get a rate Object from the User Interface.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return Object
     * @throws EFapsException on error.
     */
    protected Object[] getRateObject(final Parameter _parameter)
        throws EFapsException
    {
        BigDecimal rate = BigDecimal.ONE;
        try {
            if (_parameter.getParameterValue("rate") != null) {
                rate = (BigDecimal) Calculator_Base.getFormatInstance().parse(_parameter.getParameterValue("rate"));
            }
        } catch (final ParseException e) {
            throw new EFapsException(AbstractDocument_Base.class, "analyzeRate.ParseException", e);
        }
        final boolean rInv = "true".equalsIgnoreCase(_parameter.getParameterValue("rate" + RateUI.INVERTEDSUFFIX));
        return new Object[] { rInv ? BigDecimal.ONE : rate, rInv ? rate : BigDecimal.ONE };
    }

    /**
     * Get the name for the document on creation.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return new Name
     * @throws EFapsException on error
     */
    @Override
    protected String getDocName4Create(final Parameter _parameter)
        throws EFapsException
    {
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final boolean useNumGen = "true".equalsIgnoreCase((String) properties.get("UseNumberGenerator4Name"));
        String ret;
        if (useNumGen) {
            ret = super.getDocName4Create(_parameter);
        } else {
            ret = _parameter.getParameterValue("name4create");
        }
        return ret;
    }

    protected BigDecimal getFormat4BigDecimal(final String _number)
        throws EFapsException
    {
        BigDecimal number = BigDecimal.ZERO;
        try {
            number = (BigDecimal) Calculator_Base.getFormatInstance().parse(_number);
        } catch (final ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return number;
    }

    // new methods for abstraction
    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return type used for creation of positions
     * @throws EFapsException on error
     */
    protected Type getType4PositionCreate(final Parameter _parameter)
        throws EFapsException
    {
        final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        Type ret = null;
        if (props.containsKey("PositionType")) {
            ret = Type.get(String.valueOf(props.get("PositionType")));
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return number of positions
     * @throws EFapsException on error
     */
    protected int getPositionsCount(final Parameter _parameter)
        throws EFapsException
    {
        final String[] countAr = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                        CISales.PositionAbstract.Quantity.name));
        return countAr == null ? 0 : countAr.length;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return true it minimum retail price must be applied else false
     * @throws EFapsException on error
     *
     */
    @Override
    public boolean isIncludeMinRetail(final Parameter _parameter)
        throws EFapsException
    {
        return Sales.getSysConfig().getAttributeValueAsBoolean(SalesSettings.MINRETAILPRICE);
    }

    @Override
    public int isLongDecimal(final Parameter _parameter)
        throws EFapsException
    {
        int ret = 2;
        final Properties props =  Sales.getSysConfig().getAttributeValueAsProperties(SalesSettings.LONGDECIMAL);
        final String type = getTypeName4SystemConfiguration();

        if (props.containsKey(type) && Integer.valueOf(props.getProperty(type)) != ret) {
            ret = Integer.valueOf(props.getProperty(type));
        }
        return ret;
    }
}
