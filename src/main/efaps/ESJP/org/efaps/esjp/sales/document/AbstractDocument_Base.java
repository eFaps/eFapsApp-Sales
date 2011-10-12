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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.datamodel.ui.RateUI;
import org.efaps.admin.datamodel.ui.UIInterface;
import org.efaps.admin.dbproperty.DBProperties;
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
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.sales.Calculator;
import org.efaps.esjp.sales.Calculator_Base;
import org.efaps.esjp.sales.ICalculatorConfig;
import org.efaps.esjp.sales.Payment;
import org.efaps.esjp.sales.Payment_Base;
import org.efaps.esjp.sales.Payment_Base.OpenAmount;
import org.efaps.esjp.sales.PriceUtil;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;
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
    public static final String CURRENCY_INSTANCE_KEY = "efaps_currency_instance_key";

    /**
     * Key used to store the list of calculators in the session.
     */
    public static final String CALCULATOR_KEY = "efaps_positions_calculator_key";

    /**
     * Method must be called on opening the form containing positions to initialise
     * a new positions calculator cache!
     *
     * @param _parameter   Parameter as passed by the eFaps API for esjp
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
        final DecimalFormat formater = (DecimalFormat) NumberFormat.getInstance(Context.getThreadContext().getLocale());
        formater.setMaximumFractionDigits(2);
        formater.setMinimumFractionDigits(2);
        formater.setRoundingMode(RoundingMode.HALF_UP);
        formater.setParseBigDecimal(true);
        return formater;
    }

    /**
     * @param _calc calculator the format is wanted for
     * @return  Decimal Format
     * @throws EFapsException on error
     */
    protected DecimalFormat getDigitsformater4UnitPrice(final Calculator _calc)
        throws EFapsException
    {
        final DecimalFormat formater = (DecimalFormat) NumberFormat.getInstance(Context.getThreadContext().getLocale());
        if (_calc.isLongDecimal()) {
            formater.setMaximumFractionDigits(4);
            formater.setMinimumFractionDigits(4);
        } else {
            formater.setMaximumFractionDigits(2);
            formater.setMinimumFractionDigits(2);
        }
        formater.setRoundingMode(RoundingMode.HALF_UP);
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
     * Used by the AutoCompleteField used in the select doc form
     * for DeliveryNote.
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
     * Used by the AutoCompleteField used in the select doc form
     * for IncomingInvoices.
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
     * Used by the AutoCompleteField used in the select doc form
     * for Invoices.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4Invoice(final Parameter _parameter)
        throws EFapsException
    {
        return autoComplete4Doc(_parameter, CISales.Invoice.uuid, null);
    }


    /**
     * Used by the AutoCompleteField used in the select doc form
     * for OrderInbound.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4OrderInbound(final Parameter _parameter)
        throws EFapsException
    {
        return autoComplete4Doc(_parameter, CISales.OrderInbound.uuid,
                                            Status.find(CISales.OrderInboundStatus.uuid, "Open"));
    }


    /**
     * Used by the AutoCompleteField used in the select doc form
     * for OrderOutbound.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4OrderOutbound(final Parameter _parameter)
        throws EFapsException
    {
        final Map<? , ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final Status status;
        if (props.containsKey("Status")) {
            status = Status.find(CISales.OrderOutboundStatus.uuid, (String) props.get("Status"));
        } else {
            status = Status.find(CISales.OrderOutboundStatus.uuid, "Open");
        }
        return autoComplete4Doc(_parameter, CISales.OrderOutbound.uuid, status);
    }

    /**
     * Used by the AutoCompleteField used in the select doc form
     * for Quotations.
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
     * Used by the AutoCompleteField used in the select doc form
     * for Receipts.
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
     * Used by the AutoCompleteField used in the select doc form
     * for Receipts.
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
     * Used by the AutoCompleteField used in the select doc form
     * for Receipts.
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
     * Used by the AutoCompleteField used in the select doc form
     * for CostSheets.
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
     * Generic method to get a list of documents.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @param _typeUUID  UUID of the type to be searched.
     * @param _status   status used as additional filter, <code>null</code> to deactivated
     *  @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    protected Return autoComplete4Doc(final Parameter _parameter,
                                      final UUID _typeUUID,
                                      final Status _status)
        throws EFapsException
    {
        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, Map<String, String>> tmpMap = new TreeMap<String, Map<String, String>>();
        final QueryBuilder queryBldr = new QueryBuilder(_typeUUID);
        queryBldr.addWhereAttrMatchValue(CISales.DocumentAbstract.Name, input + "*");
        if (_status != null) {
            queryBldr.addWhereAttrEqValue(CISales.DocumentAbstract.StatusAbstract, _status.getId());
        }
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.DocumentAbstract.OID, CISales.DocumentAbstract.Name, CISales.DocumentAbstract.Date);
        multi.execute();
        while (multi.next()) {
            final String name = multi.<String>getAttribute(CISales.DocumentAbstract.Name);
            final String oid = multi.<String>getAttribute(CISales.DocumentAbstract.OID);
            final DateTime date = multi.<DateTime>getAttribute(CISales.DocumentAbstract.Date);

            final Map<String, String> map = new HashMap<String, String>();
            map.put(EFapsKey.AUTOCOMPLETE_KEY.getKey(), oid);
            map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), name);
            map.put(EFapsKey.AUTOCOMPLETE_CHOICE.getKey(), name + " - "
                     + date.toString(DateTimeFormat.forStyle("S-").withLocale(Context.getThreadContext().getLocale())));
            map.put("selectedDoc", oid);
            tmpMap.put(name, map);
        }
        list.addAll(tmpMap.values());
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * Used by the update event used in the select doc form
     * for DeliveryNote.
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
     * Used by the update event used in the select doc form
     * for IncomingInvoice.
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
     * Used by the update event used in the select doc form
     * for IncomingInvoice.
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
     * Used by the update event used in the select doc form
     * for Invoice.
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
     * Used by the update event used in the select doc form
     * for OrderInbound.
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
     * Used by the update event used in the select doc form
     * for OrderOutbound.
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
     * Used by the update event used in the select doc form
     * for Quotation.
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
     * Used by the update event used in the select doc form
     * for Receipt.
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
     * Used by the update event used in the select doc form
     * for CreditNote.
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
     * Used by the update event used in the select doc form
     * for CostSheet.
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
     * @param _parameter Parameter as passed from the eFaps API
     * @return map list for update event
     * @throws EFapsException on error
     */
    protected Return updateFields4Doc(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();
        final String oid = _parameter.getParameterValue("selectedDoc");
        if (oid != null && oid.length() > 0) {
            final PrintQuery print = new PrintQuery(oid);
            print.addAttribute("Name", "Date");
            print.addSelect("type.label");
            print.execute();

            final Map<? , ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
            final String field = props.containsKey("field") ?  (String) props.get("field") : "info";
            final StringBuilder bldr = new StringBuilder();
            bldr.append(print.getSelect("type.label")).append(" - ").append(print.getAttribute("Name"))
                .append(" - ").append(print.<DateTime> getAttribute("Date").toString(
                                     DateTimeFormat.forStyle("S-").withLocale(Context.getThreadContext().getLocale())));
            map.put(field, StringEscapeUtils.escapeJavaScript(bldr.toString()));

            map.put(EFapsKey.FIELDUPDATE_JAVASCRIPT.getKey(), getCleanJS(_parameter));
            list.add(map);
        }
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * Method to get a small script that cleans out all the field minus
     * the current one.
     * @param _parameter Parameter as passed from the eFaps API
     * @return script
     */
    protected String getCleanJS(final Parameter _parameter)
    {
        final Field field =  (Field) _parameter.get(ParameterValues.UIOBJECT);

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
     * Used by the AutoCompleteField used in the select contact.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4Contact(final Parameter _parameter)
        throws EFapsException
    {
        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final QueryBuilder queryBldr = new QueryBuilder(CIContacts.Contact);
        queryBldr.addWhereAttrMatchValue(CIContacts.Contact.Name, input + "*").setIgnoreCase(true);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIContacts.Contact.OID, CIContacts.Contact.Name);
        multi.execute();
        while (multi.next()) {
            final String name = multi.<String>getAttribute(CIContacts.Contact.Name);
            final String oid = multi.<String>getAttribute(CIContacts.Contact.OID);
            final Map<String, String> map = new HashMap<String, String>();
            map.put("eFapsAutoCompleteKEY", oid);
            map.put("eFapsAutoCompleteVALUE", name);
            map.put("eFapsAutoCompleteCHOICE", name);
            list.add(map);
        }
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * @param _parameter Parameter as passeb by the eFaps API
     * @return update map
     * @throws EFapsException on error
     */
    public Return updateFields4Contact(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instance = Instance.get(_parameter.getParameterValue("contact"));
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();
        if (instance.getId() > 0) {
            map.put("contactData", getFieldValue4Contact(instance));
        } else {
            map.put("contactData", "????");
        }
        list.add(map);
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
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
        final PrintQuery print = new PrintQuery(_instance);
        print.addSelect("class[Sales_Contacts_ClassClient].attribute[BillingAdressStreet]");
        print.addSelect("class[Contacts_ClassOrganisation].attribute[TaxNumber]");
        print.addSelect("class[Contacts_ClassPerson].attribute[IdentityCard]");
        print.addSelect("class[Contacts_ClassLocation].attribute[LocationAdressStreet]");
        print.execute();
        final String taxnumber = print.<String> getSelect("class[Contacts_ClassOrganisation].attribute[TaxNumber]");
        final String idcard = print.<String> getSelect("class[Contacts_ClassPerson].attribute[IdentityCard]");
        final boolean dni = taxnumber == null || (taxnumber.length() < 1 && idcard != null && idcard.length() > 1);
        final String street = print.getSelect("class[Sales_Contacts_ClassClient].attribute[BillingAdressStreet]");
        final String locStreet = print.getSelect("class[Contacts_ClassLocation].attribute[LocationAdressStreet]");

        final StringBuilder strBldr = new StringBuilder();
        strBldr.append(dni ? DBProperties.getProperty("Contacts_ClassPerson/IdentityCard.Label")
                        : DBProperties.getProperty("Contacts_ClassOrganisation/TaxNumber.Label"))
                        .append(": ").append(dni ? idcard : taxnumber).append("  -  ")
                        .append(DBProperties.getProperty("Sales_Contacts_ClassClient/BillingAdressStreet.Label"))
                        .append(": ")
                        .append(street.length() > 0 ? street : locStreet);
        return strBldr.toString();
    }

    /**
     * Method is called from a hidden field to include javascript in the form.
     * @param _parameter    Parameter as passed from the eFaps API
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
     * @param _parameter    Parameter as passed from the eFaps API
     * @return javascript
     * @throws EFapsException on error
     */
    protected String getJavaScript(final Parameter _parameter)
        throws EFapsException
    {
        return getJavaScript(_parameter, true);
    }

    /**
     * Method to get a javascript used to fill fields in a form.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _setStyle must the style be set
     * @return  javascript
     * @throws EFapsException on error
     */
    protected String getJavaScript(final Parameter _parameter,
                                   final boolean _setStyle)
        throws EFapsException
    {
        final StringBuilder js = new StringBuilder();
        js.append("<script type=\"text/javascript\">");
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

        js.append("Wicket.Event.add(window, \"domready\", function(event) {")
            .append("var cn = document.getElementsByName('rateCurrencyData');")
            .append("if (cn.length > 0) { ")
            .append(" cn[0].appendChild(document.createTextNode(1));")
            .append("}}); ")
            .append("var ele = document.createElement('input');")
            .append("var attr = document.createAttribute('type');")
            .append("attr.nodeValue = 'hidden';")
            .append("ele.setAttributeNode(attr);")
            .append("document.getElementById('eFapsContentDiv').appendChild(ele);");

        final boolean copy = _parameter.getParameterValue("selectedRow") != null;
        if (copy || _parameter.getParameterValue("selectedDoc") != null || _parameter.getCallInstance() != null) {
            final Instance instCall = _parameter.getCallInstance();
            final String oid = copy ? _parameter.getParameterValue("selectedRow")
                                    : _parameter.getParameterValue("selectedDoc");
            final Instance instance = Instance.get(oid);
            if (instance.isValid()) {
                js.append("ele.value='").append(oid).append("';")
                    .append("ele.name='").append(copy ? "copy" : "derived").append("';")
                    .append(getSetValuesString(instance));
            } else if (instCall != null && instCall.isValid()
                                    && instCall.getType().isKindOf(CISales.DocumentAbstract.getType())) {
                js.append(getSetValuesString(instCall));
            }
        }
        js.append("</script>");
        return js.toString();
    }

    /**
     * Method to get the javascript part for setting the values.
     * @param _instance  instance to be copied
     * @return  javascript
     * @throws EFapsException on error
     */
    protected String getSetValuesString(final Instance _instance)
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

        final BigDecimal netTotal = print.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateNetTotal);
        final BigDecimal crossTotal = print.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
        final String contactOid = print.<String>getSelect(selContOID);
        final String contactName = print.<String>getSelect(selContName);
        final String contactData = getFieldValue4Contact(Instance.get(contactOid));
        final String note = print.<String>getAttribute(CIERP.DocumentAbstract.Note);
        final Object[] rates = print.<Object[]>getAttribute(CISales.DocumentSumAbstract.Rate);

        final DecimalFormat formater = getTwoDigitsformater();
        final DecimalFormat formaterSysConf = getDigitsformater4UnitPrice(new Calculator());

        final StringBuilder currency = new StringBuilder();
        BigDecimal rate = null;
        BigDecimal[] ratesCur = null;
        if (rates != null) {
            if (!rates[2].equals(rates[3])) {
                currency.append("document.getElementsByName('rateCurrencyId')[0].selectedIndex=")
                                .append(((Long) rates[2]) - 1).append(";");
                rate = (BigDecimal) rates[1];
                final Instance newInst = Instance.get(CIERP.Currency.getType(), rates[2].toString());
                Context.getThreadContext().setSessionAttribute(AbstractDocument_Base.CURRENCY_INSTANCE_KEY, newInst);
                ratesCur = new PriceUtil().getExchangeRate(new DateTime().toDateMidnight().toDateTime(), newInst);
                currency.append(getSetFieldValue(0, "rateCurrencyData", ratesCur[1].toString()));
            }
        }

        js.append("function setValue() {")
            .append(currency)
            .append(getSetFieldValue(0, "contact", contactOid))
            .append(getSetFieldValue(0, "contactAutoComplete", contactName))
            .append(getSetFieldValue(0, "contactData", contactData))
            .append(getSetFieldValue(0, "netTotal", netTotal == null
                            ? BigDecimal.ZERO.toString() : formater.format(netTotal)))
            .append(getSetFieldValue(0, "crossTotal", netTotal == null
                            ? BigDecimal.ZERO.toString() : formater.format(crossTotal)))
            .append(getSetFieldValue(0, "note", note))
            .append(addAdditionalFields(_instance))
            .append("}")
            .append("function setRows() {");

        final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionAbstract);
        queryBldr.addWhereAttrEqValue(CISales.PositionAbstract.DocumentAbstractLink, _instance.getId());
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.PositionAbstract.PositionNumber,
                           CISales.PositionAbstract.ProductDesc,
                           CISales.PositionAbstract.Quantity,
                           CISales.PositionAbstract.UoM,
                           CISales.PositionAbstract.CrossUnitPrice,
                           CISales.PositionAbstract.NetUnitPrice,
                           CISales.PositionAbstract.DiscountNetUnitPrice,
                           CISales.PositionAbstract.CrossPrice,
                           CISales.PositionAbstract.NetPrice,
                           CISales.PositionAbstract.Tax,
                           CISales.PositionAbstract.Discount);
        final SelectBuilder selProdOID = new SelectBuilder().linkto(CISales.PositionAbstract.Product).oid();
        final SelectBuilder selProdName = new SelectBuilder().linkto(CISales.PositionAbstract.Product)
            .attribute(CIProducts.ProductAbstract.Name);
        final SelectBuilder selProdDim = new SelectBuilder().linkto(CISales.PositionAbstract.Product)
            .attribute(CIProducts.ProductAbstract.Dimension);
        multi.addSelect(selProdOID, selProdName, selProdDim);
        multi.execute();

        final Map<Integer, Object[]> values = new TreeMap<Integer, Object[]>();

        while (multi.next()) {
            final BigDecimal netUnitPrice = multi.<BigDecimal>getAttribute(CISales.PositionAbstract.NetUnitPrice);
            final BigDecimal discount = multi.<BigDecimal>getAttribute(CISales.PositionAbstract.Discount);
            final BigDecimal discountNetUnitPrice = multi.
                    <BigDecimal>getAttribute(CISales.PositionAbstract.DiscountNetUnitPrice);
            final BigDecimal netPrice = multi.<BigDecimal>getAttribute(CISales.PositionAbstract.NetPrice);

            final Object[] value = new Object[] { multi.getAttribute(CISales.PositionAbstract.Quantity),
                            multi.getSelect(selProdName),
                            multi.getSelect(selProdOID),
                            multi.getAttribute(CISales.PositionAbstract.ProductDesc),
                            multi.getAttribute(CISales.PositionAbstract.UoM),
                rate != null ? netUnitPrice.divide(rate, BigDecimal.ROUND_HALF_UP) : netUnitPrice,
                rate != null ? discount.divide(rate, BigDecimal.ROUND_HALF_UP) : discount,
                rate != null ? discountNetUnitPrice.divide(rate, BigDecimal.ROUND_HALF_UP)
                    : discountNetUnitPrice,
                rate != null ? netPrice.divide(rate, BigDecimal.ROUND_HALF_UP) : netPrice,
                            multi.getSelect(selProdDim),
                            multi.getCurrentInstance()};
            values.put(multi.<Integer>getAttribute(CISales.PositionAbstract.PositionNumber), value);
        }
        int i = 0;
        if (!values.isEmpty()) {
            for (final Entry<Integer, Object[]> entry : values.entrySet()) {
                js.append(getSetFieldValue(i, "quantity",
                                ((BigDecimal) entry.getValue()[0]).stripTrailingZeros().toPlainString()))
                    .append(getSetFieldValue(i, "productAutoComplete", (String) entry.getValue()[1]))
                    .append(getSetFieldValue(i, "product", (String) entry.getValue()[2]))
                    .append(getSetFieldValue(i, "productDesc", (String) entry.getValue()[3]))
                    .append(getSetFieldValue(i, "netUnitPrice", formaterSysConf.format(entry.getValue()[5])))
                    .append(getSetFieldValue(i, "discount", formater.format(entry.getValue()[6])))
                    .append(getSetFieldValue(i, "discountNetUnitPrice", formaterSysConf.format(entry.getValue()[7])))
                    .append(getSetFieldValue(i, "netPrice", formater.format(entry.getValue()[8])))
                    .append(addAdditionalPositions((Instance) entry.getValue()[10], i, (String) entry.getValue()[2]))
                    .append(getSetFieldValue(i, "uoM",
                                    getUoMFieldStr((Long) entry.getValue()[4], (Long) entry.getValue()[9]), false));
                i++;
            }
        }
        js.append("}").append("Wicket.Event.add(window, \"domready\", function(event) {")
            .append("addNewRows_positionTable(").append(i - 1).append(", setRows, null);")
            .append("setValue();")
            .append(getDomReadyScript(_instance))
            .append(" });");

        return js.toString();
    }

    /**
     * Method to set additional fields for the document.
     *
     * @param _instance Instance of the document.
     * @return new StringBuilder with the additional fields.
     * @throws EFapsException
     */
    protected StringBuilder addAdditionalFields(final Instance _instance)
        throws EFapsException
    {
        return new StringBuilder();
    }

    /**
     * Method to set additional positions for the document.
     *
     * @param _instPos Instance of the document.
     * @param _oidProd OID of the product in the position.
     * @param _position Integer with the position in the positions.
     * @return new StringBuilder with the additional positions.
     * @throws EFapsException on error.
     */
    protected StringBuilder addAdditionalPositions(final Instance _instPos,
                                                   final Integer _position,
                                                   final String _oidProd)
        throws EFapsException
    {
        return new StringBuilder();
    }

    /**
     * Get a "eFapsSetFieldValue" Javascript line.
     * @param _idx          index of the field
     * @param _fieldName    name of the field
     * @param _value        value
     * @return StringBuilder
     */
    protected StringBuilder getSetFieldValue(final int _idx,
                                             final String _fieldName,
                                             final String _value)
    {
        return getSetFieldValue(_idx, _fieldName, _value, true);
    }

    /**
     * Get a "eFapsSetFieldValue" Javascript line.
     * @param _idx          index of the field
     * @param _fieldName    name of the field
     * @param _value        value
     * @param _escape       must the value be escaped
     * @return StringBuilder
     */
    protected StringBuilder getSetFieldValue(final int _idx,
                                             final String _fieldName,
                                             final String _value,
                                             final boolean _escape)
    {
        final StringBuilder ret = new StringBuilder();
        ret.append("eFapsSetFieldValue(").append(_idx).append(",'").append(_fieldName).append("',");
        if (_escape) {
            ret.append("'").append(StringEscapeUtils.escapeJavaScript(_value)).append("'");
        } else {
            ret.append(_value);
        }
        ret.append(");");
        return ret;
    }

    /**
     * Add additional JavaScript to the Script that will be executed after the
     * DOM of the Html-Document was loaded. Can be used by implementations.
     *
     * @param _instance Instance of the derived Document
     * @return String containing valid Javascript
     * @throws EFapsException on error
     */
    protected String getDomReadyScript(final Instance _instance)
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
        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductAbstract);
        queryBldr.addWhereAttrMatchValue(CIProducts.ProductAbstract.Name, input + "*").setIgnoreCase(true);
        if (properties.containsKey("InStock")) {
            final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Products_VirtualInventoryStock);
            final AttributeQuery attrQuery = attrQueryBldr
                                                .getAttributeQuery(CISales.Products_VirtualInventoryStock.Product);
            queryBldr.addWhereAttrInQuery(CIProducts.ProductAbstract.ID, attrQuery);
        }
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIProducts.ProductAbstract.OID, CIProducts.ProductAbstract.Name,
                        CIProducts.ProductAbstract.Description, CIProducts.ProductAbstract.Dimension);
        multi.execute();
        while (multi.next()) {
            final String name = multi.<String>getAttribute("Name");
            final String desc = multi.<String>getAttribute("Description");
            final String oid = multi.<String>getAttribute("OID");
            final Map<String, String> map = new HashMap<String, String>();
            map.put("eFapsAutoCompleteKEY", oid);
            map.put("eFapsAutoCompleteVALUE", name);
            map.put("eFapsAutoCompleteCHOICE", name + "- " + desc);
            map.put("productDesc", desc);
            map.put("uoM", getUoMFieldStr(multi.<Long>getAttribute("Dimension")));
            map.put("discount", "0");
            list.add(map);
        }

        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * Method to update the fields on leaving the product field.
     * @param _parameter    Parameter as passed from the eFaps API
     * @return  map list with values
     * @throws EFapsException on errro
     */
    public Return updateFields4Product(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();

        final int selected = getSelectedRow(_parameter);
        final String oid = _parameter.getParameterValues("product")[selected];
        String name;
        //validate that a product was selected
        if (oid.length() > 0) {
            final PrintQuery print = new PrintQuery(oid);
            print.addAttribute("Name");
            print.execute();
            name = print.getAttribute("Name");
        } else {
            name = "";
        }

        if (name.length() > 0) {
            final List<Calculator> calcList = analyseTable(_parameter, selected);
            if (calcList.size() > 0) {
                final Calculator cal = calcList.get(selected);
                map.put("quantity", cal.getQuantityStr());
                map.put("netUnitPrice", cal.getNetUnitPriceFmtStr(getDigitsformater4UnitPrice(cal)));
                map.put("netPrice", cal.getNetPriceFmtStr(getTwoDigitsformater()));
                map.put("discountNetUnitPrice", cal.getDiscountNetUnitPriceFmtStr(getDigitsformater4UnitPrice(cal)));
                map.put("netTotal", getNetTotalFmtStr(calcList));
                map.put("crossTotal", getCrossTotalFmtStr(calcList));
                map.put("productAutoComplete", name);
                list.add(map);
                retVal.put(ReturnValues.VALUES, list);
            }
        } else {
            map.put("productAutoComplete", name);
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
            final StringBuilder js = new StringBuilder();
            js.append("document.getElementsByName('productAutoComplete')[").append(selected).append("].focus()");
            map.put(EFapsKey.FIELDUPDATE_JAVASCRIPT.getKey(), js.toString());
        }
        return retVal;
    }

    protected String getUoMFieldStr(final long _selected,
                                    final long _dimId)
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
    {
        final Dimension dim = Dimension.get(_dimId);
        return getUoMFieldStr(dim.getBaseUoM().getId(), _dimId);
    }


    /**
     * @param _uoMId id of the UoM
     * @return Field String
     */
    protected String getUoMFieldStrByUoM(final long _uoMId)
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
     * Method is executed as an update event of the field containing the
     * quantity of products to calculate the new totals.
     *
     * @param _parameter
     * @return
     * @throws EFapsException
     */
    public Return updateFields4Quantity(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();

        final int selected = getSelectedRow(_parameter);

        final List<Calculator> calcList = analyseTable(_parameter, null);

        final Calculator cal = calcList.get(selected);
        if (calcList.size() > 0) {
            map.put("quantity", cal.getQuantityStr());
            map.put("netUnitPrice", cal.getNetUnitPriceFmtStr(getDigitsformater4UnitPrice(cal)));
            map.put("netPrice", cal.getNetPriceFmtStr(getTwoDigitsformater()));
            map.put("netTotal", getNetTotalFmtStr(calcList));
            map.put("crossTotal", getCrossTotalFmtStr(calcList));
            map.put("discountNetUnitPrice", cal.getDiscountNetUnitPriceFmtStr(getDigitsformater4UnitPrice(cal)));
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        }
        return retVal;
    }

    /**
     * Method is executed as an update event of the field containing the net
     * unit price for products to calculate the new totals.
     *
     * @param _parameter
     * @return
     * @throws EFapsException
     */
    public Return updateFields4NetUnitPrice(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();

        final int selected = getSelectedRow(_parameter);

        final List<Calculator> calcList = analyseTable(_parameter, null);

        final Calculator cal = calcList.get(selected);
        if (calcList.size() > 0) {
            map.put("quantity", cal.getQuantityStr());
            map.put("netUnitPrice", cal.getNetUnitPriceFmtStr(getDigitsformater4UnitPrice(cal)));
            map.put("netPrice", cal.getNetPriceFmtStr(getTwoDigitsformater()));
            map.put("netTotal", getNetTotalFmtStr(calcList));
            map.put("crossTotal",  getCrossTotalFmtStr(calcList));
            map.put("discountNetUnitPrice", cal.getDiscountNetUnitPriceFmtStr(getDigitsformater4UnitPrice(cal)));
            list.add(map);

            retVal.put(ReturnValues.VALUES, list);
        }
        return retVal;
    }

    /**
     * Method is executed as an update event of the field containing the
     * discount for products to calculate the new totals.
     *
     * @param _parameter as passed from eFaps API.
     * @return Return
     * @throws EFapsException on error
     */
    public Return updateFields4Discount(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();
        final int selected = getSelectedRow(_parameter);

        final List<Calculator> calcList = analyseTable(_parameter, null);

        final Calculator cal = calcList.get(selected);
        if (calcList.size() > 0) {
            map.put("quantity", cal.getQuantityStr());
            map.put("netUnitPrice", cal.getNetUnitPriceFmtStr(getDigitsformater4UnitPrice(cal)));
            map.put("netPrice", cal.getNetPriceFmtStr(getTwoDigitsformater()));
            map.put("netTotal", getNetTotalFmtStr(calcList));
            map.put("crossTotal", getCrossTotalFmtStr(calcList));
            map.put("discountNetUnitPrice", cal.getDiscountNetUnitPriceFmtStr(getDigitsformater4UnitPrice(cal)));
            if (cal.getDiscount().compareTo(BigDecimal.ZERO) == 0) {
                map.put("discount", cal.getDiscountStr());
            }
            list.add(map);

            retVal.put(ReturnValues.VALUES, list);
        }
        return retVal;
    }

    /**
     * Analyse the table to calculate.
     *
     * @param _parameter        Parameter as passed by the eFaps API for esjp
     * @param _row4priceFromDB  the price for the given row will be retrieved
     *                          from the DB, null means none, -1 means all
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
        String[] unitPrices = _parameter.getParameterValues("netUnitPrice");
        if (unitPrices == null && quantities != null) {
            unitPrices = new String[quantities.length];
        }
        final String[] discounts = _parameter.getParameterValues("discount");
        final String[] oids = _parameter.getParameterValues("product");
        final boolean withoutTax = "true".equals(_parameter.getParameterValue("withoutVAT"));

        final List<Calculator> oldCalcs = (List<Calculator>) Context.getThreadContext().getSessionAttribute(
                        AbstractDocument_Base.CALCULATOR_KEY);

        if (quantities != null) {
            for (int i = 0; i < quantities.length; i++) {
                Calculator oldCalc = null;
                if (oldCalcs.size() > 0 && oldCalcs.size() > i) {
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
     * @param _includeMinRetail must the minimum retail price be included
     * @throws EFapsException on error
     * @return new Calculator
     */
    public  Calculator getCalculator( final Parameter _parameter,
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
     * @param _parameter Parameter as passed by the eFaps API
     * @return return granting access or not
     * @throws EFapsException on error
     */
    public Return accessCheck4NetUnitPrice(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Field field = (Field) _parameter.get(ParameterValues.UIOBJECT);
        if ((field.isEditableDisplay(TargetMode.CREATE) && !isIncludeMinRetail(_parameter))
                        || (field.isReadonlyDisplay(TargetMode.CREATE) && isIncludeMinRetail(_parameter))) {
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
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
        //Sales-Configuration
        final SystemConfiguration config = SystemConfiguration.get(
                        UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f"));
        return config.getAttributeValueAsBoolean("ActivateMinRetailPrice");
    }

    @Override
    public boolean isLongDecimal(final Parameter _parameter)
        throws EFapsException
    {
        boolean ret = false;
        //Sales-Configuration
        final SystemConfiguration config = SystemConfiguration.get(
                        UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f"));
        final Properties props = config.getAttributeValueAsProperties("ActivateLongDecimal");
        final String type = getTypeName4SystemConfiguration();

        if (props.containsKey(type) && Boolean.parseBoolean(props.getProperty(type))) {
            ret = true;
        }
        return ret;
    }

    protected String getTypeName4SystemConfiguration() {
        return CISales.DocumentAbstract.getType().getName();
    }

    protected String getCrossTotalFmtStr(final List<Calculator> _calcList)
        throws EFapsException
    {
        return getTwoDigitsformater().format(getCrossTotal(_calcList));
    }

    protected String getCrossTotalStr(final List<Calculator> _calcList)
        throws EFapsException
    {
        return getCrossTotal(_calcList).toString();
    }

    protected BigDecimal getCrossTotal(final List<Calculator> _calcList)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        for (final Calculator calc : _calcList) {
            ret = ret.add(calc.getCrossPrice());
        }
        return ret;
    }

    protected String getNetTotalFmtStr(final List<Calculator> _calcList)
        throws EFapsException
    {
        return getTwoDigitsformater().format(getNetTotal(_calcList));
    }

    protected String getNetTotalStr(final List<Calculator> _calcList)
        throws EFapsException
    {
        return getNetTotal(_calcList).toString();
    }

    /**
     * Method to get the
     * @param _calcList
     * @return
     * @throws EFapsException
     */
    protected BigDecimal getNetTotal(final List<Calculator> _calcList)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        for (final Calculator calc : _calcList) {
            ret = ret.add(calc.getNetPrice());
        }
        return ret;
    }

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
     *
     * @param _type type to search for
     * @param _expandChild expand childs
     * @return maximum
     * @throws EFapsException on error
     */
    protected String getMaxNumber(final Type _type,
                                  final boolean _expandChild)
        throws EFapsException
    {
        String ret = null;
        final QueryBuilder queryBuilder = new QueryBuilder(_type);
        queryBuilder.addOrderByAttributeDesc(CIERP.DocumentAbstract.Name);
        final InstanceQuery query  = queryBuilder.getQuery();
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

        String number = getMaxNumber(Type.get(type), !"false".equalsIgnoreCase(includeChildTypes));
        if (number == null) {
            number = "001-000001";
        } else {
            //get the numbers after the first "-"
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
            values.put(multi.<String>getAttribute(CIERP.Currency.Name), multi.getCurrentInstance().getId());
        }
        // Sales-Configuration
        final Instance baseInst = SystemConfiguration.get(UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f"))
                        .getLink("CurrencyBase");
        Context.getThreadContext().setSessionAttribute(AbstractDocument_Base.CURRENCY_INSTANCE_KEY, baseInst);
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
     * Update the form after change of rate currency.
     *
     * @param _parameter Parameter as passed by the eFaps API for esjp
     * @return javascript for update
     * @throws EFapsException on error
     */
    public Return updateFields4RateCurrency(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();

        final Instance newInst = Instance.get(CIERP.Currency.getType(),
                        _parameter.getParameterValue("rateCurrencyId"));
        final Map<String, String> map = new HashMap<String, String>();
        Instance currentInst = (Instance) Context.getThreadContext().getSessionAttribute(
                                        AbstractDocument_Base.CURRENCY_INSTANCE_KEY);
        // Sales-Configuration
        final Instance baseInst = SystemConfiguration.get(UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f"))
                        .getLink("CurrencyBase");
        if (currentInst == null) {
            currentInst = baseInst;
        }
        Context.getThreadContext().setSessionAttribute(AbstractDocument_Base.CURRENCY_INSTANCE_KEY, newInst);

        if (!newInst.equals(currentInst)) {

            final BigDecimal[] rates = new PriceUtil().getRates(_parameter, newInst, currentInst);

            final List<Calculator> calculators = analyseTable(_parameter, null);

            final StringBuilder js = new StringBuilder();
            int i = 0;
            for (final Calculator calculator : calculators) {
                if (!calculator.isEmpty()) {
                    calculator.applyRate(newInst, rates[2]);
                    js.append("document.getElementsByName('netUnitPrice')[").append(i).append("].value='")
                        .append(calculator.getNetUnitPriceFmtStr(getDigitsformater4UnitPrice(calculator))).append("';")
                        .append("document.getElementsByName('netPrice')[").append(i).append("].firstChild.data='")
                        .append(calculator.getNetPriceFmtStr(getTwoDigitsformater())).append("';")
                        .append("document.getElementsByName('discountNetUnitPrice')[").append(i)
                        .append("].firstChild.data='")
                        .append(calculator.getDiscountNetUnitPriceFmtStr(getDigitsformater4UnitPrice(calculator)))
                        .append("';");
                }
                i++;
            }
            if (calculators.size() > 0) {
                js.append("eFapsSetFieldValue(document.getElementsByName('netTotal')[0].id,'crossTotal','")
                        .append(getCrossTotalFmtStr(calculators)).append("');")
                    .append("eFapsSetFieldValue(document.getElementsByName('netTotal')[0].id,'netTotal','")
                        .append(getNetTotalFmtStr(calculators)).append("');")
                    .append(addFields4RateCurrency(_parameter, calculators));
                if (_parameter.getParameterValue("openAmount") != null) {
                    js.append("eFapsSetFieldValue(document.getElementsByName('netTotal')[0].id,'openAmount','")
                        .append(getBaseCrossTotal(calculators)).append("');");
                }
            }
            js.append("eFapsSetFieldValue(document.getElementsByName('netTotal')[0].id,'rateCurrencyData','")
                     .append(rates[3]).append("');")
                 .append("document.getElementsByName('rate')[0].value='").append(rates[3]).append("';")
                 .append("document.getElementsByName('rate").append(RateUI.INVERTEDSUFFIX)
                     .append("')[0].value='").append(rates[3].compareTo(rates[0]) != 0).append("';");
            map.put("eFapsFieldUpdateJS", js.toString());
            list.add(map);
        }

        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
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
            .append("document.getElementById('eFapsContentDiv').appendChild(ele);")
            .append("ele.name='selectedDoc';")
            .append(" });")
            .append("</script>");
        final Return retVal = new Return();
        retVal.put(ReturnValues.SNIPLETT, js.toString());
        return retVal;
    }

    /**
     * Method to set the openAmount into the session cache.
     *
     * @param _parameter   Parameter as passed from the eFaps API
     * @param _calcList    List of <code>Calculator</code>
     * @throws EFapsException on error
     */
    protected void setOpenAmount(final Parameter _parameter,
                                 final List<Calculator> _calcList)
        throws EFapsException
    {
        final Instance curInst = (Instance) Context.getThreadContext().getSessionAttribute(
                        AbstractDocument_Base.CURRENCY_INSTANCE_KEY);
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
            values.put(multi.<String>getAttribute("Name"), multi.<Long>getAttribute("ID"));
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
            rate = (BigDecimal) Calculator_Base.getFormatInstance().parse(_parameter.getParameterValue("rate"));
        } catch (final ParseException e) {
            throw new EFapsException(AbstractDocument_Base.class, "analyzeRate.ParseException", e);
        }
        final boolean rInv = "true".equalsIgnoreCase(_parameter.getParameterValue("rate" + RateUI.INVERTEDSUFFIX));
        return new Object[] { rInv ? BigDecimal.ONE : rate, rInv ? rate : BigDecimal.ONE };
    }

    /**
     * Get the name for the document on creation.
     * @param _parameter    Parameter as passed by the eFaps API
     * @return new Name
     * @throws EFapsException on error
     */
    protected String getDocName4Create(final Parameter _parameter)
        throws EFapsException
    {
        return _parameter.getParameterValue("name4create");
    }

}
