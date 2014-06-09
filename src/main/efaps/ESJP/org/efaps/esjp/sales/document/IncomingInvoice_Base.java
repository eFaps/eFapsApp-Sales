/*
 * Copyright 2003 - 2013 The eFaps Team
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
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.lang3.BooleanUtils;
import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.common.uiform.Field;
import org.efaps.esjp.common.uiform.Field_Base.DropDownPosition;
import org.efaps.esjp.common.uiform.Field_Base.ListType;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.esjp.erp.AbstractWarning;
import org.efaps.esjp.erp.IWarning;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.Calculator;
import org.efaps.esjp.sales.PriceUtil;
import org.efaps.esjp.sales.PriceUtil_Base.ProductPrice;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.SalesSettings;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for Type Incoming Invoice.
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("f7d75f38-5ac8-4bf4-9609-6226ac82fea3")
@EFapsRevision("$Rev$")
public abstract class IncomingInvoice_Base
    extends DocumentSum
{
    /**
     * Used to store the Revision in the Context.
     */
    public static final String REVISIONKEY = IncomingInvoice.class.getName() + "RevisionKey";

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(IncomingInvoice.class);

    /**
     * Executed from a Command execute vent to create a new Incoming Invoice.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = createDoc(_parameter);
        createPositions(_parameter, createdDoc);
        incomingInvoiceCreateTransaction(_parameter, createdDoc);
        connect2DocumentType(_parameter, createdDoc);
        connect2ProductDocumentType(_parameter, createdDoc);
        connect2Derived(_parameter, createdDoc);
        connect2RecievingTicket(_parameter, createdDoc);
        registerPurchasePrices(_parameter, createdDoc);

        connect2Object(_parameter, createdDoc);
        createTaxDoc(_parameter, createdDoc);

        return new Return();
    }

    /**
     * @param _parameter Parameter as passed by the efasp API
     * @param _createdDoc created Document
     * @throws EFapsException on error
     */
    protected void createTaxDoc(final Parameter _parameter,
                                final CreatedDoc _createdDoc)
        throws EFapsException
    {
        if ("false".equalsIgnoreCase(_parameter
                        .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.headingTaxDoc.name))) {

            final String perceptionValueStr = _parameter
                            .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.perceptionValue.name);
            if (perceptionValueStr != null && !perceptionValueStr.isEmpty()) {
                final DecimalFormat formatter = NumberFormatter.get().getFormatter();
                try {
                    final BigDecimal perception = (BigDecimal) formatter.parse(perceptionValueStr);
                    final IncomingPerceptionCertificate doc = new IncomingPerceptionCertificate();
                    _createdDoc.addValue(IncomingPerceptionCertificate_Base.PERCEPTIONVALUE, perception);
                    doc.create4Doc(_parameter, _createdDoc);
                } catch (final ParseException p) {
                    throw new EFapsException(IncomingInvoice.class, "Perception.ParseException", p);
                }
            }

            final boolean isDetraction = "true".equalsIgnoreCase(_parameter
                            .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.detractionCheckbox.name));
            if (isDetraction) {
                final DecimalFormat formatter = NumberFormatter.get().getFormatter();
                try {
                    final String detractionValueStr = _parameter
                                    .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.detractionValue.name);
                    final BigDecimal detraction;
                    if (detractionValueStr != null && !detractionValueStr.isEmpty()) {
                        detraction = (BigDecimal) formatter.parse(detractionValueStr);
                    } else {
                        detraction = BigDecimal.ZERO;
                    }
                    final IncomingDetraction doc = new IncomingDetraction();
                    _createdDoc.addValue(IncomingDetraction_Base.AMOUNTVALUE, detraction);
                    doc.create4Doc(_parameter, _createdDoc, -1);
                } catch (final ParseException p) {
                    throw new EFapsException(IncomingInvoice.class, "Perception.ParseException", p);
                }
            }
            final boolean isRetention = "true".equalsIgnoreCase(_parameter
                            .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.retentionCheckbox.name));

            if (isRetention) {
                final DecimalFormat formatter = NumberFormatter.get().getFormatter();
                try {
                    final String retentionValueStr = _parameter
                                    .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.retentionValue.name);
                    final BigDecimal retention;
                    if (retentionValueStr != null && !retentionValueStr.isEmpty()) {
                        retention = (BigDecimal) formatter.parse(retentionValueStr);
                    } else {
                        retention = BigDecimal.ZERO;
                    }
                    final IncomingRetention doc = new IncomingRetention();
                    _createdDoc.addValue(IncomingRetention_Base.AMOUNTVALUE, retention);
                    doc.create4Doc(_parameter, _createdDoc, -1);
                } catch (final ParseException p) {
                    throw new EFapsException(IncomingInvoice.class, "Perception.ParseException", p);
                }
            }
        }
    }

    /**
     * Edit.
     *
     * @param _parameter Parameter from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final EditedDoc editDoc = editDoc(_parameter);
        updatePositions(_parameter, editDoc);
        return new Return();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Return updateFields4Contact(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Return tmps = super.updateFields4Contact(_parameter);

        final List<Map<String, String>> list = (List<Map<String, String>>) tmps.get(ReturnValues.VALUES);

        final String value = _parameter.getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.contact.name);
        final Instance instance = value.contains(".") ? Instance.get(value)
                        : Instance.get(CIContacts.Contact.getType(), value);

        final Map<String, String> map = new HashMap<String, String>();
        map.put(EFapsKey.FIELDUPDATE_JAVASCRIPT.getKey(), getJS4RecievingTicket(_parameter, instance).toString());
        list.add(map);
        ret.put(ReturnValues.VALUES, list);

        final Map<Object, Object> props = (Map<Object, Object>) _parameter.get(ParameterValues.PROPERTIES);
        props.remove("FieldName");
        return ret;
    }

    /**
     * @param _parameter Parameter from the eFaps API.
     *  @param _instance instance
     * @return new Return.
     * @throws EFapsException on error.
     *
     */
    protected StringBuilder getJS4RecievingTicket(final Parameter _parameter,
                                                  final Instance _instance)
        throws EFapsException
    {
        final Parameter paraClone = ParameterUtil.clone(_parameter);
        ParameterUtil.setProperty(paraClone, "FieldName", CIFormSales.Sales_IncomingInvoiceForm.recievingTickets.name);

        final StringBuilder ret = new StringBuilder();
        final Field field = new Field();
        final List<DropDownPosition> values = new ArrayList<DropDownPosition>();
        final QueryBuilder queryBldr = new QueryBuilder(CISales.RecievingTicket);
        queryBldr.addWhereAttrEqValue(CISales.RecievingTicket.Status,
                        Status.find(CISales.RecievingTicketStatus.Closed));

        if (_instance.getType().isKindOf(CIContacts.Contact.getType())) {
            queryBldr.addWhereAttrEqValue(CISales.RecievingTicket.Contact, _instance);
        } else if (_instance.getType().isKindOf(CISales.OrderOutbound.getType())
                        || _instance.getType().isKindOf(CISales.ServiceOrderOutbound.getType())) {
            final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Document2DerivativeDocument);
            attrQueryBldr.addWhereAttrEqValue(CISales.Document2DerivativeDocument.From, _instance);
            final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.Document2DerivativeDocument.To);
            queryBldr.addWhereAttrInQuery(CISales.RecievingTicket.ID, attrQuery);
        }

        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.RecievingTicket.Name, CISales.RecievingTicket.Date);
        multi.execute();
        while (multi.next()) {
            final String name = multi.<String>getAttribute(CISales.RecievingTicket.Name);
            final DateTime date = multi.<DateTime>getAttribute(CISales.RecievingTicket.Date);
            final String option = name + " "
                            + (date == null ? "" : date.toString("dd/MM/yyyy", Context.getThreadContext().getLocale()));
            final DropDownPosition dropDown = field
                            .getDropDownPosition(paraClone, multi.getCurrentInstance().getOid(), option);
            values.add(dropDown);
        }
        final StringBuilder html = field.getInputField(paraClone, values, ListType.CHECKBOX);
        ret.append("if (document.getElementsByName('")
            .append(CIFormSales.Sales_IncomingInvoiceForm.recievingTickets.name).append("')[0]) {\n")
            .append("document.getElementsByName('").append(CIFormSales.Sales_IncomingInvoiceForm.recievingTickets.name)
            .append("')[0].innerHTML='").append(html).append("';")
            .append("}\n");
        return ret;
    }

    @Override
    protected StringBuilder add2JavaScript4Document(final Parameter _parameter,
                                                    final List<Instance> _instances)
        throws EFapsException
    {
        final StringBuilder ret = super.add2JavaScript4Document(_parameter, _instances);
        ret.append(getJS4RecievingTicket(_parameter, _instances.get(0)));
        return ret;
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _createdDoc   created doc
     * @throws EFapsException on error
     */
    protected void connect2RecievingTicket(final Parameter _parameter,
                                           final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final String[] tickets = _parameter.getParameterValues(
                        CIFormSales.Sales_IncomingInvoiceForm.recievingTickets.name);
        if (tickets != null) {
            for (final String oid : tickets) {
                final Instance ticketInst = Instance.get(oid);
                if (ticketInst.isValid()) {
                    final Insert insert = new Insert(CISales.IncomingInvoice2RecievingTicket);
                    insert.add(CISales.IncomingInvoice2RecievingTicket.FromLink, _createdDoc.getInstance());
                    insert.add(CISales.IncomingInvoice2RecievingTicket.ToLink, ticketInst);
                    insert.execute();
                }
            }
        }
    }


    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _createdDoc   created doc
     * @throws EFapsException on error
     */
    protected void registerPurchasePrices(final Parameter _parameter,
                                          final CreatedDoc _createdDoc)
        throws EFapsException
    {
        if (Sales.getSysConfig().getAttributeValueAsBoolean(SalesSettings.ACTIVATEREGPURPRICE)) {
            @SuppressWarnings("unchecked")
            final List<Calculator> calcList = (List<Calculator>) _createdDoc.getValue(
                            DocumentSum_Base.CALCULATORS_VALUE);
            if (calcList != null) {
                final String dateStr = (String) _createdDoc.getValue(CISales.DocumentSumAbstract.Date.name);
                DateTime date;
                if (dateStr == null) {
                    date = new DateTime().withTimeAtStartOfDay();
                } else {
                    date = new DateTime(dateStr).withTimeAtStartOfDay();
                }
                final Object[] rateObj = getRateObject(_parameter);
                final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                                BigDecimal.ROUND_HALF_UP);
                final DecimalFormat unitFrmt = NumberFormatter.get().getFrmt4UnitPrice(getTypeName4SysConf(_parameter));
                final int uScale = unitFrmt.getMaximumFractionDigits();
                for (final Calculator calc : calcList) {

                    final ProductPrice prodPrice = new PriceUtil().getPrice(_parameter, date, calc.getProductInstance(),
                                    CIProducts.ProductPricelistPurchase);
                    final BigDecimal basePrice = prodPrice.getBasePrice();
                    BigDecimal price;
                    if (Sales.getSysConfig().getAttributeValueAsBoolean(SalesSettings.PRODPRICENET)) {
                        price = calc.getNetUnitPrice().divide(rate, BigDecimal.ROUND_HALF_UP)
                                        .setScale(uScale, BigDecimal.ROUND_HALF_UP);
                    } else {
                        price = calc.getCrossUnitPrice().divide(rate, BigDecimal.ROUND_HALF_UP)
                                        .setScale(uScale, BigDecimal.ROUND_HALF_UP);
                    }
                    if (price.compareTo(basePrice) != 0) {
                        final Insert insert = new Insert(CIProducts.ProductPricelistPurchase);
                        insert.add(CIProducts.ProductPricelistPurchase.ProductLink, calc.getProductInstance());
                        insert.add(CIProducts.ProductPricelistPurchase.ValidFrom, date);
                        insert.add(CIProducts.ProductPricelistPurchase.ValidUntil, date.plusYears(10));
                        insert.execute();

                        final Insert posInsert = new Insert(CIProducts.ProductPricelistPosition);
                        posInsert.add(CIProducts.ProductPricelistPosition.ProductPricelist, insert.getInstance());
                        posInsert.add(CIProducts.ProductPricelistPosition.CurrencyId,
                                        Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE));
                        posInsert.add(CIProducts.ProductPricelistPosition.Price, price);
                        posInsert.execute();
                    }
                }
            }
        }
    }

    /**
     * Method to do a transaction of all the products of a Incoming Invoice when
     * it is created.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _createdDoc instance of Incoming Invoice document recently created
     * @throws EFapsException on error
     */
    protected void incomingInvoiceCreateTransaction(final Parameter _parameter,
                                                    final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final String storage = _parameter.getParameterValue("storage");
        final String date = _parameter.getParameterValue("date");

        if (storage != null) {
            final List<Instance> positions = _createdDoc.getPositions();
            for (final Instance instance : positions) {
                final PrintQuery print = new PrintQuery(instance);
                print.addAttribute(CISales.IncomingInvoicePosition.Product, CISales.IncomingInvoicePosition.Quantity,
                                CISales.IncomingInvoicePosition.IncomingInvoice, CISales.IncomingInvoicePosition.UoM);
                print.execute();

                final Object productID = print.<Object>getAttribute(CISales.IncomingInvoicePosition.Product);
                final Object quantity = print.<Object>getAttribute(CISales.IncomingInvoicePosition.Quantity);
                final Object incomingInvoiceId = print
                                .<Object>getAttribute(CISales.IncomingInvoicePosition.IncomingInvoice);
                final Object uom = print.<Object>getAttribute(CISales.IncomingInvoicePosition.UoM);

                final Insert insert = new Insert(CIProducts.TransactionInbound);
                insert.add(CIProducts.TransactionInbound.Quantity, quantity);
                insert.add(CIProducts.TransactionInbound.Storage, storage);
                insert.add(CIProducts.TransactionInbound.Product, productID);
                insert.add(CIProducts.TransactionInbound.Description,
                         DBProperties.getProperty("org.efaps.esjp.sales.document.IncomingInvoice.description4Trigger"));
                insert.add(CIProducts.TransactionInbound.Date, date == null ? new DateTime() : date);
                insert.add(CIProducts.TransactionInbound.Document, incomingInvoiceId);
                insert.add(CIProducts.TransactionInbound.UoM, uom);
                insert.execute();
            }
        }
    }

    @Override
    protected void add2DocCreate(final Parameter _parameter,
                                 final Insert _insert,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final SystemConfiguration config = Sales.getSysConfig();
        final Properties props = config.getAttributeValueAsProperties(SalesSettings.INCOMINGINVOICESEQUENCE);

        final NumberGenerator numgen = NumberGenerator.get(UUID.fromString(props.getProperty("UUID")));
        if (numgen != null) {
            final String revision = numgen.getNextVal();
            Context.getThreadContext().setSessionAttribute(IncomingInvoice_Base.REVISIONKEY, revision);
            _insert.add(CISales.IncomingInvoice.Revision, revision);
        }
    }

    @Override
    protected void add2Map4UpdateField(final Parameter _parameter,
                                       final Map<String, Object> _map,
                                       final List<Calculator> _calcList,
                                       final Calculator _cal)
        throws EFapsException
    {
        super.add2Map4UpdateField(_parameter, _map, _calcList, _cal);
        final String perceptionPercentStr = _parameter
                        .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.perceptionPercent.name);
        if (perceptionPercentStr != null && !perceptionPercentStr.isEmpty()) {
            final DecimalFormat formatter = NumberFormatter.get().getFormatter();
            try {
                final BigDecimal perceptionPercent = (BigDecimal) formatter.parse(perceptionPercentStr);
                final BigDecimal crossTotal = getCrossTotal(_parameter, _calcList);
                final BigDecimal perception = crossTotal.multiply(perceptionPercent
                                .setScale(8, BigDecimal.ROUND_HALF_UP)
                                .divide(new BigDecimal(100), BigDecimal.ROUND_HALF_UP));
                final String perceptionStr = NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter))
                                .format(perception);
                _map.put(CIFormSales.Sales_IncomingInvoiceForm.perceptionValue.name, perceptionStr);
            } catch (final ParseException e) {
                IncomingInvoice_Base.LOG.error("Catched parsing error", e);
            }
        }
        final String detractionPercentStr = _parameter
                        .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.detractionPercent.name);
        if (detractionPercentStr != null && !detractionPercentStr.isEmpty()) {
            final DecimalFormat formatter = NumberFormatter.get().getFormatter();
            try {
                final BigDecimal detractionPercent = (BigDecimal) formatter.parse(detractionPercentStr);
                final BigDecimal crossTotal = getCrossTotal(_parameter, _calcList);
                final BigDecimal detraction = crossTotal.multiply(detractionPercent
                                .setScale(8, BigDecimal.ROUND_HALF_UP)
                                .divide(new BigDecimal(100), BigDecimal.ROUND_HALF_UP));
                final String detractionStr = NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter))
                                .format(detraction);
                _map.put(CIFormSales.Sales_IncomingInvoiceForm.detractionValue.name, detractionStr);
            } catch (final ParseException e) {
                IncomingInvoice_Base.LOG.error("Catched parsing error", e);
            }
        }
        final String retentionPercentStr = _parameter
                        .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.retentionPercent.name);
        if (retentionPercentStr != null && !retentionPercentStr.isEmpty()) {
            final DecimalFormat formatter = NumberFormatter.get().getFormatter();
            try {
                final BigDecimal retentionPercent = (BigDecimal) formatter.parse(retentionPercentStr);
                final BigDecimal crossTotal = getCrossTotal(_parameter, _calcList);
                final BigDecimal retention = crossTotal.multiply(retentionPercent
                                .setScale(8, BigDecimal.ROUND_HALF_UP)
                                .divide(new BigDecimal(100), BigDecimal.ROUND_HALF_UP));
                final String retentionStr = NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter))
                                .format(retention);
                _map.put(CIFormSales.Sales_IncomingInvoiceForm.retentionValue.name, retentionStr);
            } catch (final ParseException e) {
                IncomingInvoice_Base.LOG.error("Catched parsing error", e);
            }
        }
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return containing maplist
     * @throws EFapsException on error
     */
    public Return updateFields4PerceptionPercent(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();

        final List<Calculator> calcList = analyseTable(_parameter, null);

        if (calcList.size() > 0) {
            add2Map4UpdateField(_parameter, map, calcList, null);
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        }
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return containing maplist
     * @throws EFapsException on error
     */
    public Return updateFields4RetentionPercent(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();

        final List<Calculator> calcList = analyseTable(_parameter, null);

        if (calcList.size() > 0) {
            add2Map4UpdateField(_parameter, map, calcList, null);
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        }
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return containing maplist
     * @throws EFapsException on error
     */
    public Return updateFields4DetractionPercent(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();

        final List<Calculator> calcList = analyseTable(_parameter, null);

        if (calcList.size() > 0) {
            add2Map4UpdateField(_parameter, map, calcList, null);
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        }
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return with Snipplet
     * @throws EFapsException on error
     */
    public Return showRevisionFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String revision = (String) Context.getThreadContext().getSessionAttribute(
                        IncomingInvoice_Base.REVISIONKEY);
        Context.getThreadContext().setSessionAttribute(IncomingInvoice_Base.REVISIONKEY, null);
        final StringBuilder html = new StringBuilder();
        html.append("<span style=\"text-align: center; display: block; width: 100%; font-size: 40px; height: 55px;\">")
                        .append(revision).append("</span>");
        ret.put(ReturnValues.SNIPLETT, html.toString());
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return with Snipplet
     * @throws EFapsException on error
     */
    public Return getJavaScript4TaxDocUIValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final StringBuilder js = new StringBuilder()
            .append("<script type=\"text/javascript\">\n")
            .append("require([\"dojo/ready\"], function(ready) {")
            .append("ready(1600, function(){")
            .append("function deac(_key, _dis) {")
            .append("require([\"dojo/query\"], function(query){")
            .append("query(\"input[name^=\\\"\" + _key + \"\\\"]\").forEach(function(node) {")
            .append("if (node.type==='text') {")
            .append("node.disabled = _dis ? '' : 'disabled';")
            .append("}")
            .append("});")
            .append("});")
            .append("}")
            .append("deac(\"perception\");")
            .append("deac(\"retention\");")
            .append("deac(\"detraction\");")
            .append("require([\"dojo/query\"], function(query){")
            .append("query(\"input[name$=\\\"Checkbox\\\"] \").on(\"click\", function(evt) {")
            .append("var key = evt.currentTarget.name.substring(0,evt.currentTarget.name.length-8);")
            .append("deac(key, evt.currentTarget.checked );")
            .append("});")
            .append("});")
            .append("});")
            .append("});")
            .append("\n</script>\n");
        retVal.put(ReturnValues.SNIPLETT, js.toString());
        return retVal;
    }


    @Override
    public String getTypeName4SysConf(final Parameter _parameter)
        throws EFapsException
    {
        return CISales.IncomingInvoice.getType().getName();
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return containing maplist
     * @throws EFapsException on error
     */
    public Return recievingTicketMultiPrint(final Parameter _parameter)
        throws EFapsException
    {
        final MultiPrint multi = new MultiPrint()
        {

            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
                throws EFapsException
            {
                super.add2QueryBldr(_parameter, _queryBldr);
                final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.DocumentAbstract);
                attrQueryBldr.addWhereAttrEqValue(CISales.DocumentAbstract.ID, _parameter.getInstance());
                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.DocumentAbstract.Contact);
                _queryBldr.addWhereAttrInQuery(CISales.DocumentAbstract.Contact, attrQuery);
            }
        };
        return multi.execute(_parameter);
    }

    @Override
    public Return validate(final Parameter _parameter)
        throws EFapsException
    {
        final Validation validation = new Validation()
        {
            @Override
            protected List<IWarning> validate(final Parameter _parameter,
                                              final List<IWarning> _warnings)
                throws EFapsException
            {
                final List<IWarning> ret = super.validate(_parameter, _warnings);
                ret.addAll(validatTaxDoc(_parameter));
                return ret;
            }
        };
        return validation.validate(_parameter, this);
    }

    /**
     * Validate that the given quantities have numbers bigger than Zero.
     *
     * @param _parameter Parameter as passed by the eFasp API
     * @return List of warnings, empty list if no warning
     * @throws EFapsException on error
     */
    public List<IWarning> validatTaxDoc(final Parameter _parameter)
        throws EFapsException
    {
        final List<IWarning> ret = new ArrayList<IWarning>();
        if ("false".equalsIgnoreCase(_parameter
                        .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.headingTaxDoc.name))) {
            final boolean isPerception = "true".equalsIgnoreCase(_parameter
                            .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.perceptionCheckbox.name));
            final boolean isRetention = "true".equalsIgnoreCase(_parameter
                            .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.retentionCheckbox.name));
            final boolean isDetraction = "true".equalsIgnoreCase(_parameter
                            .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.detractionCheckbox.name));
            if ((BooleanUtils.toInteger(isPerception) + BooleanUtils.toInteger(isRetention) + BooleanUtils
                            .toInteger(isDetraction)) > 1) {
                ret.add(new OnlyOneTaxDocWarning());
            }

            if (isPerception && Sales.getSysConfig().getAttributeValueAsBoolean(SalesSettings.ISPERCEPTIONAGENT)) {
                final Instance inst = Instance.get(_parameter
                                .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.contact.name));
                final PrintQuery print = new PrintQuery(inst);
                final SelectBuilder sel = SelectBuilder.get().clazz(CISales.Contacts_ClassTaxinfo)
                                .attribute(CISales.Contacts_ClassTaxinfo.Perception);
                print.addSelect(sel);
                print.executeWithoutAccessCheck();
                final Sales.TaxPerception perc = print.getSelect(sel);
                if (perc != null && perc.equals(Sales.TaxPerception.AGENT)) {
                    ret.add(new ContactIsPerceptionAgentWarning());
                }
            }

            if (isRetention && Sales.getSysConfig().getAttributeValueAsBoolean(SalesSettings.ISRETENTIONAGENT)) {
                final Instance inst = Instance.get(_parameter
                                .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.contact.name));
                final PrintQuery print = new PrintQuery(inst);
                final SelectBuilder sel = SelectBuilder.get().clazz(CISales.Contacts_ClassTaxinfo)
                                .attribute(CISales.Contacts_ClassTaxinfo.Retention);
                print.addSelect(sel);
                print.executeWithoutAccessCheck();
                final Sales.TaxRetention perc = print.getSelect(sel);
                if (perc != null && perc.equals(Sales.TaxRetention.AGENT)) {
                    ret.add(new ContactIsRetentionAgentWarning());
                }
            }
        }
        return ret;
    }

    public static class OnlyOneTaxDocWarning
        extends AbstractWarning
    {
        public OnlyOneTaxDocWarning()
        {
            setError(true);
        }
    }

    public static class ContactIsPerceptionAgentWarning
        extends AbstractWarning
    {

        public ContactIsPerceptionAgentWarning()
        {
            setError(true);
        }
    }

    public static class ContactIsRetentionAgentWarning
        extends AbstractWarning
    {

        public ContactIsRetentionAgentWarning()
        {
            setError(true);
        }
    }
}
