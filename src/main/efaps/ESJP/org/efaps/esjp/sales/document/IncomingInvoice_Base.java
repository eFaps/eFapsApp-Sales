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

import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.common.SystemConfiguration;
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
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uiform.Field;
import org.efaps.esjp.common.uiform.Field_Base.DropDownPosition;
import org.efaps.esjp.common.uiform.Field_Base.ListType;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.Calculator;
import org.efaps.esjp.sales.Costs;
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
    public static final String REVISIONKEY = "org.efaps.esjp.sales.document.IncomingInvoice.RevisionKey";

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
        new Costs().updateCosts(_parameter, createdDoc.getInstance());
        incomingInvoiceCreateTransaction(_parameter, createdDoc);
        connect2DocumentType(_parameter, createdDoc);
        connect2ProductDocumentType(_parameter, createdDoc);
        connect2Derived(_parameter, createdDoc);
        connect2RecievingTicket(_parameter, createdDoc);
        final String perceptionValueStr = _parameter
                        .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.perceptionValue.name);
        if (perceptionValueStr != null && !perceptionValueStr.isEmpty()) {
            final DecimalFormat formatter = NumberFormatter.get().getFormatter();
            try {
                final BigDecimal perception = (BigDecimal) formatter.parse(perceptionValueStr);
                final IncomingPerceptionCertificate_Base doc = new IncomingPerceptionCertificate();
                createdDoc.addValue(IncomingPerceptionCertificate_Base.PERCEPTIONVALUE, perception);
                doc.create4Doc(_parameter, createdDoc);
            } catch (final ParseException p) {
                throw new EFapsException(IncomingInvoice.class, "Perception.ParseException", p);
            }
        }
        return new Return();
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

        final Map<Object,Object> props = (Map<Object, Object>) _parameter.get(ParameterValues.PROPERTIES);
        props.remove("FieldName");
        return ret;
    }

    protected StringBuilder getJS4RecievingTicket(final Parameter _parameter,
                                                  final Instance _instance)
        throws EFapsException
    {
        @SuppressWarnings("unchecked")
        final Map<Object,Object> props = (Map<Object, Object>) _parameter.get(ParameterValues.PROPERTIES);
        props.put("FieldName", CIFormSales.Sales_IncomingInvoiceForm.recievingTickets.name);

        final StringBuilder ret = new StringBuilder();
        final Field field = new Field();
        final List<DropDownPosition> values = new ArrayList<DropDownPosition>();
        final QueryBuilder queryBldr = new QueryBuilder(CISales.RecievingTicket);

        if (_instance.getType().isKindOf(CIContacts.Contact.getType())) {
            queryBldr.addWhereAttrEqValue(CISales.RecievingTicket.Contact, _instance);
        } else if (_instance.getType().isKindOf(CISales.OrderOutbound.getType())) {
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
                            .getDropDownPosition(_parameter, multi.getCurrentInstance().getOid(), option);
            values.add(dropDown);
        }
        final StringBuilder html = field.getInputField(_parameter, values, ListType.CHECKBOX);

        ret.append("document.getElementsByName('").append(CIFormSales.Sales_IncomingInvoiceForm.recievingTickets.name)
                        .append("')[0].innerHTML='").append(html).append("';");
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
                                       final Map<String, String> _map,
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
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();

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

    @Override
    public String getTypeName4SysConf(final Parameter _parameter)
        throws EFapsException
    {
        return CISales.IncomingInvoice.getType().getName();
    }


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
}
