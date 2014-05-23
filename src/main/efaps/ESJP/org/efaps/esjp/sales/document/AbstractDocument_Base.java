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

package org.efaps.esjp.sales.document;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.datamodel.ui.RateUI;
import org.efaps.admin.datamodel.ui.UIInterface;
import org.efaps.admin.event.EventType;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.Listener;
import org.efaps.admin.ui.AbstractCommand;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.admin.ui.field.Field;
import org.efaps.ci.CIType;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
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
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.contacts.Contacts;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.erp.RateFormatter;
import org.efaps.esjp.erp.RateInfo;
import org.efaps.esjp.products.Product;
import org.efaps.esjp.products.util.Products;
import org.efaps.esjp.products.util.Products.ProductIndividual;
import org.efaps.esjp.products.util.ProductsSettings;
import org.efaps.esjp.sales.Calculator;
import org.efaps.esjp.sales.ICalculatorConfig;
import org.efaps.esjp.sales.PriceUtil;
import org.efaps.esjp.sales.listener.IOnCreateFromDocument;
import org.efaps.esjp.sales.tax.TaxesAttribute;
import org.efaps.esjp.sales.tax.xml.Taxes;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.SalesSettings;
import org.efaps.jaas.AppAccessHandler;
import org.efaps.ui.wicket.models.cell.UITableCell;
import org.efaps.ui.wicket.models.objects.UIForm;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;
import org.efaps.util.cache.CacheReloadException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * Used as a prefix for update script;
     */
    public static final String SELDOCUPDATEPF = "SDUP_";

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDocument.class);

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
        return autoComplete4Doc(_parameter, CISales.DeliveryNote.uuid, (Status[]) null);
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
        return autoComplete4Doc(_parameter, CISales.IncomingInvoice.uuid, (Status[]) null);
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
        return autoComplete4Doc(_parameter, CISales.Invoice.uuid, (Status[]) null);
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
        return autoComplete4Doc(_parameter, CISales.OrderInbound.uuid, (Status[]) null);
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
        return autoComplete4Doc(_parameter, CISales.OrderOutbound.uuid, (Status[]) null);
    }

    /**
     * Used by the AutoCompleteField used in the select doc form for
     * OrderOutbound.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4ServiceOrderOutbound(final Parameter _parameter)
        throws EFapsException
    {
        return autoComplete4Doc(_parameter, CISales.ServiceOrderOutbound.uuid, (Status[]) null);
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
        return autoComplete4Doc(_parameter, CISales.Quotation.uuid, (Status[]) null);
    }

    /**
     * Used by the AutoCompleteField used in the select doc form for ProductRequest.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */

    public Return autoComplete4ProductRequest(final Parameter _parameter)
        throws EFapsException
    {
        return autoComplete4Doc(_parameter, CISales.ProductRequest.uuid, (Status[]) null);
    }

    /**
     * Used by the AutoCompleteField used in the select doc form for Exchange.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4Exchange(final Parameter _parameter)
        throws EFapsException
    {
        return autoComplete4Doc(_parameter, CISales.Exchange.uuid, (Status[]) null);
    }

    /**
     * Used by the AutoCompleteField used in the select doc form for IncomingCredit.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4IncomingCredit(final Parameter _parameter)
        throws EFapsException
    {
        return autoComplete4Doc(_parameter, CISales.IncomingCredit.uuid, (Status[]) null);
    }

    /**
     * Used by the AutoCompleteField used in the select doc form for0 Incoming Exchange.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4IncomingExchange(final Parameter _parameter)
        throws EFapsException
    {
        return autoComplete4Doc(_parameter, CISales.IncomingExchange.uuid, (Status[]) null);
    }

    /**
     * Used by the AutoCompleteField used in the select doc form for0 Incoming Exchange.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4QuoteRequest(final Parameter _parameter)
        throws EFapsException
    {
        return autoComplete4Doc(_parameter, CISales.QuoteRequest.uuid, (Status[]) null);
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
        return autoComplete4Doc(_parameter, CISales.Receipt.uuid, (Status[]) null);
    }

    /**
     * Used by the AutoCompleteField used in the select doc form for RecievingTicket.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4RecievingTicket(final Parameter _parameter)
        throws EFapsException
    {
        return autoComplete4Doc(_parameter, CISales.RecievingTicket.uuid, (Status[]) null);
    }

    /**
     * Used by the AutoCompleteField used in the select doc form for Credit.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4Credit(final Parameter _parameter)
        throws EFapsException
    {
        return autoComplete4Doc(_parameter, CISales.Credit.uuid, (Status[]) null);
    }

    /**
     * Used by the AutoCompleteField used in the select doc form for CreditNote.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4CreditNote(final Parameter _parameter)
        throws EFapsException
    {
        return autoComplete4Doc(_parameter, CISales.CreditNote.uuid, (Status[]) null);
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
        return autoComplete4Doc(_parameter, CISales.CostSheet.uuid, (Status[]) null);
    }


    /**
     * Used by the AutoCompleteField used in the select doc form for
     * Reservation.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4Reservation(final Parameter _parameter)
        throws EFapsException
    {
        return autoComplete4Doc(_parameter, CISales.Reservation.uuid, (Status[]) null);
    }

    /**
     * Used by the AutoCompleteField used in the select doc form for IncomingRetentionCertificate.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4IncomingRetentionCertificate(final Parameter _parameter)
        throws EFapsException
    {
        return autoComplete4Doc(_parameter, CISales.IncomingRetentionCertificate.uuid, (Status[]) null);
    }

    /**
     * Used by the AutoCompleteField used in the select doc form for PettyCashReceipt.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4PettyCashReceipt(final Parameter _parameter)
        throws EFapsException
    {
        return autoComplete4Doc(_parameter, CISales.PettyCashReceipt.uuid, (Status[]) null);
    }

    /**
     * Used by the AutoCompleteField used in the select doc form.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4Doc(final Parameter _parameter)
        throws EFapsException
    {
        return autoComplete4Doc(_parameter, getQueryBldrFromProperties(_parameter));
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
                                      final Status... _status)
        throws EFapsException
    {
        final Type type = Type.get(_typeUUID);
        // if the status is not set explicitly we analyze the properties
        Status[] status;
        if (_status == null && type.isCheckStatus()) {
            final Type statusType = type.getStatusAttribute().getLink();
            final List<Status> statusList = new ArrayList<Status>();
            final Map<Integer, String> statusMap = analyseProperty(_parameter, "Status");
            for (final String statusStr : statusMap.values()) {
                final Status statusTmp = Status.find(statusType.getUUID(), statusStr);
                if (statusTmp != null) {
                    statusList.add(statusTmp);
                }
            }
            status = statusList.isEmpty() ? (Status[]) null : statusList.toArray(new Status[statusList.size()]);
        } else {
            status = _status;
        }
        final QueryBuilder queryBldr = new QueryBuilder(type);
        if (status != null) {
            queryBldr.addWhereAttrEqValue(CISales.DocumentAbstract.StatusAbstract, (Object[]) status);
        }
        return autoComplete4Doc(_parameter, queryBldr);
    }

    /**
     * Generic method to get a list of documents.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @param _queryBldr QueryBuilder used as base
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    protected Return autoComplete4Doc(final Parameter _parameter,
                                      final QueryBuilder _queryBldr)
        throws EFapsException
    {
        final String req = (String) _parameter.get(ParameterValues.OTHERS);

        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, Map<String, String>> tmpMap = new TreeMap<String, Map<String, String>>();

        InterfaceUtils.addMaxResult2QueryBuilder4AutoComplete(_parameter, _queryBldr);
        add2QueryBldr(_parameter, _queryBldr);
        _queryBldr.addWhereAttrMatchValue(CISales.DocumentAbstract.Name, req + "*").setIgnoreCase(true);

        final String key = containsProperty(_parameter, "Key") ? getProperty(_parameter, "Key") : "OID";
        final boolean showContact = !"false".equalsIgnoreCase(getProperty(_parameter, "ShowContact"));

        final MultiPrintQuery multi = _queryBldr.getPrint();
        final SelectBuilder selContactName = SelectBuilder.get().linkto(CISales.DocumentAbstract.Contact)
                        .attribute(CIContacts.Contact.Name);
        if (showContact) {
            multi.addSelect(selContactName);
        }
        multi.addAttribute(key);
        multi.addAttribute(CISales.DocumentAbstract.Name, CISales.DocumentAbstract.Date);
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
        return new Return().put(ReturnValues.VALUES, updateFields4Doc(_parameter));
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
        return new Return().put(ReturnValues.VALUES, updateFields4Doc(_parameter));
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
        return new Return().put(ReturnValues.VALUES, updateFields4Doc(_parameter));
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
        return new Return().put(ReturnValues.VALUES, updateFields4Doc(_parameter));
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
        return new Return().put(ReturnValues.VALUES, updateFields4Doc(_parameter));
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
        return new Return().put(ReturnValues.VALUES, updateFields4Doc(_parameter));
    }

    /**
     * Used by the update event used in the select doc form for OrderOutbound.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return map list for update event
     * @throws EFapsException on error
     */
    public Return updateFields4ServiceOrderOutbound(final Parameter _parameter)
        throws EFapsException
    {
        return new Return().put(ReturnValues.VALUES, updateFields4Doc(_parameter));
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
        return new Return().put(ReturnValues.VALUES, updateFields4Doc(_parameter));
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
        return new Return().put(ReturnValues.VALUES, updateFields4Doc(_parameter));
    }

    /**
     * Used by the update event used in the select doc form for ProductRequest.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return map list for update event
     * @throws EFapsException on error
     */

    public Return updateFields4ProductRequest(final Parameter _parameter)
        throws EFapsException
    {
        return new Return().put(ReturnValues.VALUES, updateFields4Doc(_parameter));
    }

    /**
     * Used by the update event used in the select doc form for ProductRequest.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return map list for update event
     * @throws EFapsException on error
     */

    public Return updateFields4Document(final Parameter _parameter)
        throws EFapsException
    {
        return new Return().put(ReturnValues.VALUES, updateFields4Doc(_parameter));
    }

    /**
     * Used by the update event used in the select doc form for PettyCashReceipt.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return map list for update event
     * @throws EFapsException on error
     */

    public Return updateFields4PettyCashReceipt(final Parameter _parameter)
        throws EFapsException
    {
        return new Return().put(ReturnValues.VALUES, updateFields4Doc(_parameter));
    }

    /**
     * Used by the update event used in the select doc form for ProductRequest.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return map list for update event
     * @throws EFapsException on error
     */

    public Return updateFields4QuoteRequest(final Parameter _parameter)
        throws EFapsException
    {
        return new Return().put(ReturnValues.VALUES, updateFields4Doc(_parameter));
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
        return new Return().put(ReturnValues.VALUES, updateFields4Doc(_parameter));
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
        return new Return().put(ReturnValues.VALUES, updateFields4Doc(_parameter));
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
        return new Return().put(ReturnValues.VALUES, updateFields4Doc(_parameter));
    }

    /**
     * Generic method to get the listmap for update event.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return map list for update event
     * @throws EFapsException on error
     */
    protected List<Map<String, Object>> updateFields4Doc(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        final Field field = (Field) _parameter.get(ParameterValues.UIOBJECT);
        final String fieldName = field.getName();

        final String input = containsProperty(_parameter, "input") ? getProperty(_parameter, "input") : "selectedDoc";
        final String infofield = containsProperty(_parameter, "field") ? getProperty(_parameter, "field") : "info";
        final boolean multiple = "true".equalsIgnoreCase(getProperty(_parameter, "multiple"));
        // the oid of the document that executed the update event
        final String currentOid = _parameter.getParameterValue(fieldName);
        // the previous selected oids
        final String[] selectedOids = _parameter.getParameterValues(input);
        if (currentOid != null && !currentOid.isEmpty()) {
            final PrintQuery print = new PrintQuery(currentOid);
            print.addAttribute(CIERP.DocumentAbstract.Name, CIERP.DocumentAbstract.Date);
            final SelectBuilder sel = SelectBuilder.get().type().label();
            print.addSelect(sel);
            print.execute();
            final StringBuilder label = new StringBuilder();
            label.append(print.getSelect(sel)).append(" - ")
                .append(print.getAttribute(CIERP.DocumentAbstract.Name)).append(" - ")
                .append(print.<DateTime> getAttribute(CIERP.DocumentAbstract.Date).toString(
                               DateTimeFormat.forStyle("S-").withLocale(Context.getThreadContext().getLocale())));
            final Map<?,?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
            if (multiple) {
                if (!Arrays.asList(Arrays.copyOfRange(selectedOids, 1, selectedOids.length)).contains(
                                print.getCurrentInstance().getOid())) {
                    final StringBuilder js = new StringBuilder()
                        .append("require([\"dojo/query\", \"dojo/dom-construct\",\"dojo/on\"], ")
                        .append("function(query, domConstruct, on) {")
                        .append("query(\"div  + input[name='").append(input)
                        .append("']\").forEach(domConstruct.destroy); ")
                        .append(" query(\"span[name='").append(infofield).append("']\").forEach(function(node){")
                        .append("var ul;")
                        .append("if (query(\"ul\", node).length==0) {")
                        .append(" domConstruct.empty(node);")
                        .append("ul = domConstruct.place(\"<ul>");

                    for (final Entry<?,?> entry : properties.entrySet()) {
                        js.append("<input type=\\\"hidden\\\" ")
                            .append("name=\\\"").append(AbstractDocument_Base.SELDOCUPDATEPF).append(entry.getKey())
                            .append("\\\" ").append("value=\\\"").append(entry.getValue()).append("\\\">");
                    }

                    js.append("</ul>\", node);\n")
                        .append("} else {")
                        .append("ul = query(\"ul\", node)[0];")
                        .append("}")
                        .append("var x = domConstruct.place(\"<li>")
                        .append(StringEscapeUtils.escapeEcmaScript(label.toString()))
                        .append("<img style=\\\"cursor:pointer\\\"")
                        .append(" src=\\\"/").append(AppAccessHandler.getApplicationKey())
                        .append("/servlet/image/org.efaps.ui.wicket.components.table.delete.png?\\\">")
                        .append("<input type=\\\"hidden\\\" ")
                        .append("name=\\\"").append(input).append("\\\" ")
                        .append("value=\\\"").append(currentOid).append("\\\"></li>\"")
                        .append(", ul, \"last\"); \n")
                        .append("var img = query(\"img\", x)[0];")
                        .append("on(img, \"click\", function(e){")
                        .append("domConstruct.destroy(img.parentNode);")
                        .append("});")
                        .append("});")
                        .append("});");

                    InterfaceUtils.appendScript4FieldUpdate(map, js.toString());
                }
            } else {
                final StringBuilder js = new StringBuilder()
                    .append("require([\"dojo/query\", \"dojo/dom-construct\"], function(query, domConstruct) { ")
                    .append("query(\"div  + input[name='").append(input).append("']\").forEach(domConstruct.destroy); ")
                    .append(" query(\"span[name='").append(infofield).append("']\").forEach(function(node){")
                    .append(" domConstruct.empty(node);")
                    .append(" domConstruct.place(\"<span>")
                    .append(StringEscapeUtils.escapeEcmaScript(label.toString()))
                    .append("<span>")
                    .append("<input type=\\\"hidden\\\" ")
                        .append("name=\\\"").append(input).append("\\\" ")
                        .append("value=\\\"").append(currentOid).append("\\\">");

                for (final Entry<?,?> entry : properties.entrySet()) {
                    js.append("<input type=\\\"hidden\\\" ")
                        .append("name=\\\"").append(AbstractDocument_Base.SELDOCUPDATEPF).append(entry.getKey())
                        .append("\\\" ").append("value=\\\"").append(entry.getValue()).append("\\\">");
                 }

                js.append("\", node, \"last\"); \n")
                    .append("});")
                    .append("});");

                InterfaceUtils.appendScript4FieldUpdate(map, js.toString());
            }
            InterfaceUtils.appendScript4FieldUpdate(map, getCleanJS(_parameter));
            ret.add(map);
        }
        return ret;
    }

    /**
     * Method to get a small script that cleans out all the field minus the
     * current one.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return script
     * @throws EFapsException on error
     */
    protected String getCleanJS(final Parameter _parameter)
        throws EFapsException
    {
        final StringBuilder js = new StringBuilder();
        final Field currentField = (Field) _parameter.get(ParameterValues.UIOBJECT);
        for (final Field field : currentField.getCollection().getFields()) {
            if (!currentField.equals(field) && field.getEvents(EventType.UI_FIELD_AUTOCOMPLETE) != null) {
                js.append(getSetFieldValue(0, field.getName(), "", ""));
            }
        }
        return js.toString();
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
        retVal.put(ReturnValues.SNIPLETT, getJavaScript4SelectDoc(_parameter));
        return retVal;
    }

    /**
     * Get the JavaScript for setting the values on a "Create From" command.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return Return containing the JavaScript
     * @throws EFapsException on error
     */
    protected String getJavaScript4SelectDoc(final Parameter _parameter)
        throws EFapsException
    {
        final Instance currency4Invoice = evaluateCurrency4JavaScript(_parameter);
        final Instance baseCurrency = Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);

        final StringBuilder js = new StringBuilder();
        js.append("<script type=\"text/javascript\">\n")
            .append("require([\"dojo/ready\", \"dojo/query\",\"dojo/dom-construct\"],")
            .append(" function(ready, query, domConstruct){\n")
            .append(" ready(1500, function(){")
            .append(updateRateFields(_parameter, currency4Invoice, baseCurrency)).append("\n")
            .append("var pN = dojo.query('.eFapsContentDiv')[0];\n");

        final FieldValue command = (FieldValue) _parameter.get(ParameterValues.UIOBJECT);
        final TargetMode mode = command.getTargetMode();
        Context.getThreadContext().setSessionAttribute(AbstractDocument_Base.TARGETMODE_DOC_KEY, mode);

        boolean copy = _parameter.getParameterValue("selectedRow") != null;
        if (copy || _parameter.getParameterValue("selectedDoc") != null || _parameter.getCallInstance() != null) {
            final Instance instCall = _parameter.getCallInstance();
            final List<Instance> instances = getInstances4Derived(_parameter);
            if (!instances.isEmpty()) {
                // if obnly one instance is given it might be a copy and not a derived
                if (instances.size() == 1) {
                    final Instance instance = instances.get(0);
                    // in case of copy check if it is really a copy (meaning the same type will be created)
                    final Object object = _parameter.get(ParameterValues.CLASS);
                    if (copy && object instanceof UIForm) {
                        final UIForm uiForm = (UIForm) object;
                        final Type type = uiForm.getCommand().getTargetCreateType();
                        if (type != null && !instance.getType().equals(type)) {
                            copy = false;
                        }
                    }
                    js.append("domConstruct.create(\"input\", {\n")
                        .append(" value: \"").append(instance.getOid()).append("\", ")
                        .append(" name: \"").append(copy ? "copy" : "derived").append("\", ")
                        .append(" type: \"hidden\" ")
                        .append("}, pN);\n");
                } else  {
                    for (final Instance instance : instances) {
                        js.append("domConstruct.create(\"input\", {")
                            .append(" value: \"").append(instance.getOid()).append("\", ")
                            .append(" name: \"derived\", ")
                            .append(" type: \"hidden\" ")
                            .append("}, pN);\n");
                    }
                }
                js.append(getJavaScript4Document(_parameter, instances))
                    .append(instances.size() == 1 ?  getJavaScript4Positions(_parameter, instances.get(0))
                                    : getJavaScript4Positions(_parameter, instances))
                    .append(addDomReadyScript(_parameter, instances));
            } else if (instCall != null && instCall.isValid()
                            && instCall.getType().isKindOf(CISales.DocumentAbstract.getType())) {
                final List<Instance> instCallLst = new ArrayList<Instance>();
                instCallLst.add(instCall);
                js.append(add2JavaScript4Document(_parameter, instCallLst))
                    .append(getJavaScript4Positions(_parameter, instCall));
            }
        }
        js.append("});").append("});\n</script>\n");
        return js.toString();
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return script
     * @throws EFapsException on error
     */
    public Return getJavaScript4Search(final Parameter _parameter)
        throws EFapsException
    {
        final StringBuilder js = new StringBuilder();
        final Instance instance = _parameter.getInstance() != null
                        ? _parameter.getInstance() : _parameter.getCallInstance();
        js.append("<script type=\"text/javascript\">\n")
                        .append("require([\"dojo/ready\"], function(ready){ready(1500, function(){\n");
        if (instance != null && instance.isValid()) {
            final SelectBuilder selContactId = new SelectBuilder()
                            .linkto(CISales.DocumentSumAbstract.Contact).id();
            final SelectBuilder selContactName = new SelectBuilder()
                            .linkto(CISales.DocumentSumAbstract.Contact).attribute(CIContacts.Contact.Name);
            final PrintQuery print = new PrintQuery(instance);
            print.addSelect(selContactId, selContactName);
            print.execute();

            final Long contactId = print.<Long>getSelect(selContactId);
            final String contactName = print.<String>getSelect(selContactName);

            js.append(getSetFieldValue(0, "contact", String.valueOf(contactId), contactName)).append("\n");
        }
        js.append(" })});").append("</script>");
        final Return retVal = new Return();
        retVal.put(ReturnValues.SNIPLETT, js.toString());
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return instance of the currency
     * @throws EFapsException on error
     */
    protected Instance evaluateCurrency4JavaScript(final Parameter _parameter)
        throws EFapsException
    {
        Instance ret = Sales.getSysConfig().getLink(SalesSettings.CURRENCY4INVOICE);
        if (TargetMode.EDIT.equals(_parameter.get(ParameterValues.ACCESSMODE))) {
            final Instance inst = _parameter.getInstance();
            if (inst != null && inst.isValid() && inst.getType().isKindOf(CISales.DocumentSumAbstract.getType())) {
                final PrintQuery print = new PrintQuery(inst);
                final SelectBuilder sel = SelectBuilder.get().linkto(CISales.DocumentSumAbstract.RateCurrencyId)
                                .instance();
                print.addSelect(sel);
                print.executeWithoutAccessCheck();
                final Instance instTmp = print.<Instance>getSelect(sel);
                if (instTmp != null && instTmp.isValid()) {
                    ret = instTmp;
                }
            }
        }
        return ret;
    }


    /**
     * Add additional on Dom Ready JavaScript for
     * setting the values on a "Create From" command.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _instances list of instance the values are copied from
     * @return Return containing the JavaScript
     * @throws EFapsException on error
     */
    protected StringBuilder addDomReadyScript(final Parameter _parameter,
                                              final List<Instance> _instances)
        throws EFapsException
    {
        return new StringBuilder();
    }

    /**
     * JavaScript part for setting the Document Head.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _instances list of instance the values are copied from
     * @return Return containing the JavaScript
     * @throws EFapsException on error
     */
    protected StringBuilder getJavaScript4Document(final Parameter _parameter,
                                                   final List<Instance> _instances)
        throws EFapsException
    {
        final StringBuilder js = new StringBuilder();
        // as a default
        final Instance instance = _instances.get(0);
        final PrintQuery print = new PrintQuery(instance);
        print.addAttribute(CISales.DocumentSumAbstract.RateNetTotal,
                        CISales.DocumentSumAbstract.RateCrossTotal,
                        CISales.DocumentSumAbstract.Rate,
                        CISales.DocumentSumAbstract.RateTaxes,
                        CIERP.DocumentAbstract.Name,
                        CIERP.DocumentAbstract.Note);
        final SelectBuilder selContOID = new SelectBuilder().linkto(CIERP.DocumentAbstract.Contact).oid();
        final SelectBuilder selContName = new SelectBuilder().linkto(CIERP.DocumentAbstract.Contact)
                        .attribute(CIContacts.Contact.Name);
        print.addSelect(selContOID, selContName);
        print.execute();

        final BigDecimal netTotal = print.<BigDecimal> getAttribute(CISales.DocumentSumAbstract.RateNetTotal);
        final BigDecimal crossTotal = print.<BigDecimal> getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
        final Taxes rateTaxes = print.<Taxes> getAttribute(CISales.DocumentSumAbstract.RateTaxes);

        final String contactOid = print.<String> getSelect(selContOID);
        final String contactName = print.<String> getSelect(selContName);
        final String contactData = new Contacts().getFieldValue4Contact(Instance.get(contactOid), false);
        final String note = print.<String> getAttribute(CIERP.DocumentAbstract.Note);
        final String name = print.<String> getAttribute(CIERP.DocumentAbstract.Name);
        final Object[] rates = print.<Object[]> getAttribute(CISales.DocumentSumAbstract.Rate);

        final DecimalFormat formater = NumberFormatter.get().getTwoDigitsFormatter();

        final StringBuilder currStrBldr = new StringBuilder();
        BigDecimal[] ratesCur = null;
        if (rates != null) {
            final Instance currency4Invoice = Sales.getSysConfig().getLink(SalesSettings.CURRENCY4INVOICE);
            final Instance baseCurrency = Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);
            final Instance instanceDerived = getInstances4Derived(_parameter).get(0);
            boolean derived = false;
            if (instanceDerived.isValid()) {
                derived = true;
            }
            final Instance newInst = Instance.get(CIERP.Currency.getType(), rates[2].toString());
            Context.getThreadContext().setSessionAttribute(AbstractDocument_Base.CURRENCYINST_KEY, newInst);
            ratesCur = new PriceUtil().getExchangeRate(new DateTime().withTimeAtStartOfDay(), newInst);

            if ((rates[2].equals(rates[3]) && !currency4Invoice.equals(baseCurrency) && !derived)
                            || !rates[2].equals(rates[3])) {
                currStrBldr.append(getSetFieldValue(0, "rateCurrencyId", "" + (rates[2]))).append("\n")
                    .append(getSetFieldValue(0, "rateCurrencyData", ratesCur[1].toString()))
                    .append(getSetFieldValue(0, "rate", ratesCur[1].toString())).append("\n")
                    .append(getSetFieldValue(0, "rate" + RateUI.INVERTEDSUFFIX,
                                    "" + new CurrencyInst(newInst).isInvert())).append("\n");
            }
        }

        js.append(currStrBldr);
        if (isContact2JavaScript4Document(_parameter, _instances)) {
            js.append(getSetFieldValue(0, "contact", contactOid, contactName)).append("\n")
                .append(getSetFieldValue(0, "contactAutoComplete", contactName)).append("\n")
                .append(getSetFieldValue(0, "contactData", contactData)).append("\n");
        }

        if ("true".equalsIgnoreCase(_parameter.getParameterValue(AbstractDocument_Base.SELDOCUPDATEPF + "CopyName"))) {
            js.append(getSetFieldValue(0, "name4create", name)).append("\n");
        }

        js.append(getSetFieldValue(0, "netTotal", netTotal == null
                            ? BigDecimal.ZERO.toString() : formater.format(netTotal))).append("\n")
            .append(getSetFieldValue(0, "crossTotal", netTotal == null
                            ? BigDecimal.ZERO.toString() : formater.format(crossTotal))).append("\n");
        if (rateTaxes != null) {
            js.append("if (document.getElementsByName('taxes')[0]) {\n")
                .append("document.getElementsByName('taxes')[0].innerHTML='")
                .append(new TaxesAttribute().getUI4ReadOnly(rateTaxes)).append("';\n")
                .append("}\n");
        }
        js.append(getSetFieldValue(0, "note", note)).append("\n")
            .append(add2JavaScript4Document(_parameter, _instances)).append("\n")
            .append("\n");
        return js;
    }

    /**
     * Must the contact information be added to the JavaScript.
     * @param _parameter Parameter as passed from the eFaps API
     * @param _instances list of instance the values are copied from
     * @return true if it should be added
     * @throws EFapsException on error
     */
    protected boolean isContact2JavaScript4Document(final Parameter _parameter,
                                                    final List<Instance> _instances)
        throws EFapsException
    {
        return true;
    }

    /**
     * Add JavaScript part to  the Document Head.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _instances list of instance the values are copied from
     * @return Return containing the JavaScript
     * @throws EFapsException on error
     */
    protected StringBuilder add2JavaScript4Document(final Parameter _parameter,
                                                    final List<Instance> _instances)
        throws EFapsException
    {
        final StringBuilder ret = new StringBuilder();
        for (final IOnCreateFromDocument listener : Listener.get().<IOnCreateFromDocument>invoke(
                        IOnCreateFromDocument.class)) {
            ret.append(listener.add2JavaScript4Document(_parameter, _instances));
        }
        return ret;
    }

    /**
     * JavaScript part for setting the Positions.<br/>
     * <ol>
     * <li>Get the data and fill a <code>Map&lt;String,Object&gt;</code> with the values</li>
     * <li>Substract/add data by evaluating related document</li>
     * <li>Give the chance to correct/manipulate the original data</li>
     * <li>Convert the <code>Map&lt;String,Object&gt;</code> into a <code>Map&lt;String,String&gt;</code></li>
     * <li>Generate the JavaScript for the data</li>
     * </ol>
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _instance instance the values are copied from
     * @return Return containing the JavaScript
     * @throws EFapsException on error
     */
    protected StringBuilder getJavaScript4Positions(final Parameter _parameter,
                                                    final Instance _instance)
        throws EFapsException
    {
        final StringBuilder js = new StringBuilder();

        final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionAbstract);
        queryBldr.addWhereAttrEqValue(CISales.PositionAbstract.DocumentAbstractLink, _instance);
        queryBldr.addOrderByAttributeAsc(CISales.PositionAbstract.PositionNumber);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.PositionAbstract.PositionNumber,
                        CISales.PositionAbstract.ProductDesc,
                        CISales.PositionAbstract.Quantity,
                        CISales.PositionAbstract.UoM,
                        CISales.PositionSumAbstract.RateNetUnitPrice,
                        CISales.PositionSumAbstract.RateDiscountNetUnitPrice,
                        CISales.PositionSumAbstract.RateNetPrice,
                        CISales.PositionSumAbstract.RateCrossPrice,
                        CISales.PositionSumAbstract.Tax,
                        CISales.PositionSumAbstract.Discount);
        final SelectBuilder selProdOID = new SelectBuilder().linkto(CISales.PositionSumAbstract.Product).oid();
        final SelectBuilder selProdName = new SelectBuilder().linkto(CISales.PositionSumAbstract.Product)
                        .attribute(CIProducts.ProductAbstract.Name);
        final SelectBuilder selProdDim = new SelectBuilder().linkto(CISales.PositionSumAbstract.Product)
                        .attribute(CIProducts.ProductAbstract.Dimension);
        multi.addSelect(selProdOID, selProdName, selProdDim);
        multi.setEnforceSorted(true);
        multi.execute();

        final List<Map<KeyDef, Object>> values = new ArrayList<Map<KeyDef, Object>>();
        final DecimalFormat qtyFrmt = NumberFormatter.get().getFrmt4Quantity(getTypeName4SysConf(_parameter));
        final DecimalFormat upFrmt = NumberFormatter.get().getFrmt4UnitPrice(getTypeName4SysConf(_parameter));
        final DecimalFormat totFrmt = NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter));
        final DecimalFormat disFrmt = NumberFormatter.get().getFrmt4Discount(getTypeName4SysConf(_parameter));

        while (multi.next()) {
            final Map<KeyDef, Object> map = new HashMap<KeyDef, Object>();

            final BigDecimal rateNetUnitPrice = multi.<BigDecimal>getAttribute(
                            CISales.PositionSumAbstract.RateNetUnitPrice);
            final BigDecimal rateDiscountNetUnitPrice = multi.
                            <BigDecimal>getAttribute(CISales.PositionSumAbstract.RateDiscountNetUnitPrice);
            final BigDecimal rateNetPrice = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.RateNetPrice);
            final BigDecimal rateCrossPrice = multi
                            .<BigDecimal>getAttribute(CISales.PositionSumAbstract.RateCrossPrice);
            final BigDecimal discount = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.Discount);

            map.put(new KeyDefStr("oid"), multi.getCurrentInstance().getOid());
            map.put(new KeyDefFrmt("quantity", qtyFrmt),
                            multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.Quantity));
            map.put(new KeyDefStr("productAutoComplete"), multi.<String>getSelect(selProdName));
            map.put(new KeyDefStr("product"), new String[] { multi.<String>getSelect(selProdOID),
                            multi.<String>getSelect(selProdName) });
            map.put(new KeyDefStr("productDesc"), multi.<String>getAttribute(CISales.PositionSumAbstract.ProductDesc));
            map.put(new KeyDefStr("uoM"), getUoMFieldStr(multi.<Long>getAttribute(CISales.PositionSumAbstract.UoM),
                            multi.<Long>getSelect(selProdDim)));

            map.put(new KeyDefFrmt("netUnitPrice", upFrmt), rateNetUnitPrice == null ? BigDecimal.ZERO
                            : rateNetUnitPrice);
            map.put(new KeyDefFrmt("discountNetUnitPrice", upFrmt), rateDiscountNetUnitPrice == null ? BigDecimal.ZERO
                            : rateDiscountNetUnitPrice);
            map.put(new KeyDefFrmt("netPrice", totFrmt), rateNetPrice == null ? BigDecimal.ZERO : rateNetPrice);
            map.put(new KeyDefFrmt("discount", disFrmt), discount == null ? BigDecimal.ZERO : discount);
            map.put(new KeyDefFrmt("crossPrice", totFrmt), rateCrossPrice == null ? BigDecimal.ZERO : rateCrossPrice);
            values.add(map);
        }

        final Set<String> noEscape = new HashSet<String>();
        noEscape.add("uoM");

        evaluatePositions4RelatedInstances(_parameter, values, _instance);

        add2JavaScript4Postions(_parameter, values, noEscape);

        final List<Map<String, Object>> strValues = convertMap4Script(_parameter, values);

        if (TargetMode.EDIT.equals(Context.getThreadContext()
                        .getSessionAttribute(AbstractDocument_Base.TARGETMODE_DOC_KEY))) {
            js.append(getSetFieldValuesScript(_parameter, strValues, noEscape));
        } else {
            js.append(getTableRemoveScript(_parameter, "positionTable", false, false))
                            .append(getTableAddNewRowsScript(_parameter, "positionTable", strValues,
                                            getOnCompleteScript(_parameter), false, false, noEscape));
        }
        js.append("\n");
        return js;
    }

    /**
     * @param _parameter Paramert as passed by the eFaps API
     * @param _values   values
     * @return converted map
     * @throws EFapsException on error
     */
    protected List<Map<String, Object>> convertMap4Script(final Parameter _parameter,
                                                          final Collection<Map<KeyDef, Object>> _values)
        throws EFapsException
    {
        final List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
        for (final Map<KeyDef, Object> valueMap :_values) {
            final Map<String, Object> map  = new HashMap<String, Object>();
            for (final Entry<KeyDef, Object> entry : valueMap.entrySet()) {
                map.put(entry.getKey().getName(), entry.getKey().convert4Map(entry.getValue()));
            }
            ret.add(map);
        }
        return ret;
    }

    /**
     * JavaScript part for update positions according to derived documents.<br/>
     * <ol>
     * <li>Evaluate the relations of the selected instances with the <code>&lt;RelationType&gt;</code> property</li>
     * <li>Evaluate the derived type with the <code>&lt;DerivedType&gt;</code> property</li>
     * <li>Give the positions according to the document instances analyzed</li>
     * <li>Update the quantities or delete positions from the _values Map</li>
     * </ol>
     *
     * @param _parameter as passed from eFaps API.
     * @param _values   values for positions
     * @param _instances instances to be evaluated
     * @throws EFapsException on error.
     */
    protected void evaluatePositions4RelatedInstances(final Parameter _parameter,
                                                      final Collection<Map<KeyDef, Object>> _values,
                                                      final Instance... _instances)
        throws EFapsException
    {
        final Map<Integer, String> relTypes = analyseProperty(_parameter, "RelationType");
        final Map<Integer, String> linkFroms = analyseProperty(_parameter, "RelationLinkFrom");
        final Map<Integer, String> linkTos = analyseProperty(_parameter, "RelationLinkTo");
        final Map<Integer, String> types = analyseProperty(_parameter, "DerivedType");
        final Map<Integer, String> statusGrps = analyseProperty(_parameter, "DerivedStatusGrp");
        final Map<Integer, String> status = analyseProperty(_parameter, "DerivedStatus");
        final Map<Integer, String> substracts = analyseProperty(_parameter, "RelationSubstracts");
        final DecimalFormat qtyFrmt = NumberFormatter.get().getFrmt4Quantity(getTypeName4SysConf(_parameter));
        final List<Map<KeyDef, Object>> lstRemove = new ArrayList<Map<KeyDef, Object>>();
        for (final Entry<Integer, String> relTypeEntry : relTypes.entrySet()) {
            final Integer key = relTypeEntry.getKey();
            final boolean substract = "true".equalsIgnoreCase(substracts.get(key));
            final Type relType = Type.get(relTypeEntry.getValue());
            final Map<String, BigDecimal> prodQuantMap = new HashMap<String, BigDecimal>();

            final QueryBuilder attrQueryBldr = new QueryBuilder(relType);
            attrQueryBldr.addWhereAttrEqValue(linkFroms.get(key), (Object[]) _instances);
            final AttributeQuery attrQuery = attrQueryBldr
                            .getAttributeQuery(linkTos.get(key));

            final Type type = Type.get(types.get(key));
            final QueryBuilder attrQueryBldr2 = new QueryBuilder(type);
            final String[] statusArr = status.get(key).split(";");
            final List<Status> statusLst = new ArrayList<Status>();
            for (final String stat : statusArr) {
                final Status st = Status.find(statusGrps.get(key), stat);
                statusLst.add(st);
            }
            attrQueryBldr2.addWhereAttrEqValue(CISales.DocumentAbstract.StatusAbstract, statusLst.toArray());
            attrQueryBldr2.addWhereAttrInQuery(CISales.DocumentAbstract.ID, attrQuery);
            final AttributeQuery attrQuery2 = attrQueryBldr2.getAttributeQuery(CISales.DocumentAbstract.ID);

            final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionAbstract);
            queryBldr.addWhereAttrInQuery(CISales.PositionAbstract.DocumentAbstractLink, attrQuery2);

            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selProdOID = new SelectBuilder().linkto(CISales.PositionSumAbstract.Product).oid();
            multi.addAttribute(CISales.PositionAbstract.Quantity);
            multi.addSelect(selProdOID);
            multi.execute();
            while (multi.next()) {
                final String prodOid = multi.<String>getSelect(selProdOID);
                final BigDecimal quantity = multi.<BigDecimal>getAttribute(CISales.PositionAbstract.Quantity);

                if (prodQuantMap.containsKey(prodOid)) {
                    prodQuantMap.put(prodOid, prodQuantMap.get(prodOid).add(quantity));
                } else {
                    prodQuantMap.put(prodOid, quantity);
                }
            }

            for (final Map<KeyDef, Object> map : _values) {
                final String[] product = (String[]) map.get(new KeyDefStr("product"));
                if (prodQuantMap.containsKey(product[0])) {
                    final BigDecimal quantityTotal = (BigDecimal) map.get(new KeyDefFrmt("quantity", qtyFrmt));
                    final BigDecimal quantityPartial = prodQuantMap.get(product[0]);

                    if (substract) {
                        final BigDecimal quantityCurr = quantityTotal.subtract(quantityPartial);
                        if (quantityCurr.compareTo(BigDecimal.ZERO) > 0) {
                            map.put(new KeyDefFrmt("quantity", qtyFrmt), quantityCurr);
                        } else {
                            lstRemove.add(map);
                        }
                    } else {
                        final BigDecimal quantityCurr = quantityTotal.add(quantityPartial);
                        map.put(new KeyDefFrmt("quantity", qtyFrmt), quantityCurr);
                    }
                }
            }
        }

        for (final Map<KeyDef, Object> remove : lstRemove) {
            _values.remove(remove);
        }
    }

    /**
     * @param _parameter Paramter as passed by the eFaps API
     * @param _values values to be added to
     * @param _noEscape no escape fields
     * @throws EFapsException on error
     */
    protected void add2JavaScript4Postions(final Parameter _parameter,
                                           final Collection<Map<KeyDef, Object>> _values,
                                           final Set<String> _noEscape)
        throws EFapsException
    {
        // to be used by implementations
    }


    /**
     * JavaScript part for setting the Positions.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _instances list of instances the values are copied from
     * @return Return containing the JavaScript
     * @throws EFapsException on error
     */
    protected StringBuilder getJavaScript4Positions(final Parameter _parameter,
                                                    final List<Instance> _instances)
        throws EFapsException
    {
        final StringBuilder js = new StringBuilder();
        final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionAbstract);
        queryBldr.addWhereAttrEqValue(CISales.PositionAbstract.DocumentAbstractLink, _instances.toArray());
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.PositionAbstract.PositionNumber,
                        CISales.PositionAbstract.ProductDesc,
                        CISales.PositionAbstract.Quantity,
                        CISales.PositionAbstract.UoM);
        final SelectBuilder selProdInst = new SelectBuilder().linkto(CISales.PositionSumAbstract.Product).instance();
        final SelectBuilder selProdName = new SelectBuilder().linkto(CISales.PositionSumAbstract.Product)
                        .attribute(CIProducts.ProductAbstract.Name);
        final SelectBuilder selProdDim = new SelectBuilder().linkto(CISales.PositionSumAbstract.Product)
                        .attribute(CIProducts.ProductAbstract.Dimension);
        multi.addSelect(selProdInst, selProdName, selProdDim);
        multi.execute();

        final Map<Instance, Map<KeyDef, Object>> valuesTmp = new LinkedHashMap<Instance, Map<KeyDef, Object>>();
        final DecimalFormat qtyFrmt = NumberFormatter.get().getFrmt4Quantity(getTypeName4SysConf(_parameter));
        while (multi.next()) {
            final Instance prodInst = multi.<Instance>getSelect(selProdInst);
            final Map<KeyDef, Object> map;
            if (valuesTmp.containsKey(prodInst)) {
                map = valuesTmp.get(prodInst);
                final BigDecimal quantity = (BigDecimal) map.get(new KeyDefFrmt("quantity", qtyFrmt));
                map.put(new KeyDefFrmt("quantity", qtyFrmt),
                                quantity.add(multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.Quantity)));
            } else {
                map = new HashMap<KeyDef, Object>();
                valuesTmp.put(prodInst, map);
                map.put(new KeyDefStr("product"), new String[] { prodInst.getOid(),
                    multi.<String>getSelect(selProdName)});
                map.put(new KeyDefStr("productDesc"), multi.<String>getAttribute(
                                CISales.PositionSumAbstract.ProductDesc));
                map.put(new KeyDefStr("uoM"), getUoMFieldStr(multi.<Long>getAttribute(CISales.PositionSumAbstract.UoM),
                                                multi.<Long>getSelect(selProdDim)));
                map.put(new KeyDefFrmt("quantity", qtyFrmt),
                                multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.Quantity));
            }
        }
        final Collection<Map<KeyDef, Object>> values = valuesTmp.values();

        final Set<String> noEscape = new HashSet<String>();
        noEscape.add("uoM");

        evaluatePositions4RelatedInstances(_parameter, values, _instances.toArray(new Instance[_instances.size()]));

        add2JavaScript4Postions(_parameter, values, noEscape);

        final List<Map<String, Object>> strValues = convertMap4Script(_parameter, values);

        Collections.sort(strValues, new Comparator<Map<String, Object>>()
        {
            @Override
            public int compare(final Map<String, Object> _o1,
                               final Map<String, Object> _o2)
            {
                return String.valueOf(_o1.get("productAutoComplete"))
                                .compareTo(String.valueOf(_o2.get("productAutoComplete")));
            }
        });

        if (TargetMode.EDIT.equals(Context.getThreadContext()
                        .getSessionAttribute(AbstractDocument_Base.TARGETMODE_DOC_KEY))) {
            js.append(getSetFieldValuesScript(_parameter, strValues, noEscape));
        } else {
            js.append(getTableRemoveScript(_parameter, "positionTable", false, false))
                .append(getTableAddNewRowsScript(_parameter, "positionTable", strValues,
                            getOnCompleteScript(_parameter), false, false, noEscape));
        }
        js.append("\n");
        return js;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return the Instance used for the derived on the javascript field. returns
     */
    protected List<Instance> getInstances4Derived(final Parameter _parameter)
    {
        final List<Instance> ret = new ArrayList<Instance>();
        final String[] selectedDoc = _parameter.getParameterValues("selectedDoc") == null
                        ? _parameter.getParameterValues("selectedRow") : _parameter.getParameterValues("selectedDoc");
        if (selectedDoc != null) {
            if (selectedDoc.length > 1) {
                for (int i = 0; i < selectedDoc.length; i++) {
                    final Instance instance = Instance.get(selectedDoc[i]);
                    if (instance.isValid()) {
                        ret.add(instance);
                    }
                }
            } else {
                final Instance instance = Instance.get(selectedDoc[0]);
                if (instance.isValid()) {
                    ret.add(instance);
                }
            }
        }
        return ret;
    }

    @Deprecated
    protected String getRate4UI(final Parameter _parameter,
                                final Instance _instanceCurrency)
        throws EFapsException
    {
        final Currency currency = new Currency();
        final RateInfo rateInfo = currency.evaluateRateInfo(_parameter, _parameter.getParameterValue("date_eFapsDate"),
                        _instanceCurrency);
        return rateInfo.getRateUIFrmt();
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

        RateInfo rateInfo = null;
        CurrencyInst currencyInst = null;
        // in edit mode try to use the rate of the document
        if (TargetMode.EDIT.equals(_parameter.get(ParameterValues.ACCESSMODE))) {
            final Instance inst = _parameter.getInstance();
            if (inst != null && inst.isValid() && inst.getType().isKindOf(CISales.DocumentSumAbstract.getType())) {
                final PrintQuery print = new PrintQuery(inst);
                print.addAttribute(CISales.DocumentSumAbstract.Rate);
                print.executeWithoutAccessCheck();
                final Object rate = print.getAttribute(CISales.DocumentSumAbstract.Rate);
                if (rate != null && rate instanceof Object[]) {
                    final Currency currency = new Currency();
                    rateInfo  = currency.evaluateRateInfo(_parameter, (Object[]) rate);
                    currencyInst = rateInfo.getCurrencyInst();
                }
            }
        }
        // if no rate yet try to set it
        if (rateInfo == null) {
            final Currency currency = new Currency();
            final String date = _parameter.getParameterValue("date_eFapsDate");
            final RateInfo[] rates = currency.evaluateRateInfos(_parameter, date, _currentInst, _newInst);
            rateInfo = rates[2];
            currencyInst = rates[1].getCurrencyInst();
        }
        if (rateInfo != null) {
            js.append(getSetFieldValue(0, "rateCurrencyData", rateInfo.getRateUIFrmt()))
                .append(getSetFieldValue(0, "rate", NumberFormatter.get().getFormatter().format(rateInfo.getRateUI())))
                .append("\n")
                .append(getSetFieldValue(0, "rate" + RateUI.INVERTEDSUFFIX, "" + currencyInst.isInvert()))
                .append("\n");
        }
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
     * Add additional JavaScript to the Script that will be executed after the
     * DOM of the Html-Document was loaded. Can be used by implementations.
     *
     * @param _parameter Paramter as passed by the eFaps API
     * @param _instance Instance of the derived Document
     * @return String containing valid Javascript
     * @throws EFapsException on error
     */
    @Deprecated
    protected String getDomReadyScript(final Parameter _parameter,
                                       final Instance _instance)
        throws EFapsException
    {
        return "";
    }


    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _map          Map to add to
     * @param _instance     Instance of the Product
     */
    protected void add2UpdateField4Product(final Parameter _parameter,
                                           final Map<String, Object> _map,
                                           final Instance _prodInst)
        throws EFapsException
    {
        final Field field = (Field) _parameter.get(ParameterValues.UIOBJECT);
        final String fieldName = field.getName();

        final PrintQuery print = new PrintQuery(_prodInst);
        print.addAttribute(CIProducts.ProductAbstract.Name, CIProducts.ProductAbstract.Description,
                        CIProducts.ProductAbstract.Dimension,
                        CIProducts.ProductAbstract.DefaultUoM,
                        CIProducts.ProductAbstract.Individual);
        if (print.execute()) {
            final String name = print.getAttribute(CIProducts.ProductAbstract.Name);
            final String desc = print.<String>getAttribute(CIProducts.ProductAbstract.Description);
            final Long dimId = print.<Long>getAttribute(CIProducts.ProductAbstract.Dimension);
            final Long dUoMId = print.<Long>getAttribute(CIProducts.ProductAbstract.DefaultUoM);
            long selectedUoM;
            if (dUoMId == null) {
                selectedUoM = Dimension.get(dimId).getBaseUoM().getId();
            } else {
                if (Dimension.getUoM(dUoMId).getDimension().equals(Dimension.get(dimId))) {
                    selectedUoM = dUoMId;
                } else {
                    selectedUoM = Dimension.get(dimId).getBaseUoM().getId();
                }
            }
            _map.put("uoM", getUoMFieldStr(selectedUoM, dimId));
            _map.put("productDesc", StringEscapeUtils.escapeEcmaScript(desc));
            // TODO: remove when autocomplete ready
            _map.put(fieldName + "AutoComplete", name);

            if (Products.getSysConfig().getAttributeValueAsBoolean(ProductsSettings.ACTIVATEINDIVIDUAL)) {
                add4Individual(_parameter, print.<ProductIndividual>getAttribute(CIProducts.ProductAbstract.Individual),
                            _map, _prodInst.getOid(), name +  "-" + desc);
            }
        }
    }


    protected void add4Individual(final Parameter _parameter,
                                  final ProductIndividual _individual,
                                  final Map<String, Object> _map,
                                  final String _key,
                                  final String _legend)
    {
        if (_individual != null && !ProductIndividual.NONE.equals(_individual)) {
            // TODO make configurable from properties
            final String fieldName = "individual";
            final String qfieldName = "quantity";
            int quantity;
            switch (_individual) {
                case INDIVIDUAL:
                    final String quantityStr = _parameter.getParameterValues(qfieldName)[getSelectedRow(_parameter)];
                    if (quantityStr != null && !quantityStr.isEmpty()) {
                        quantity = Integer.parseInt(quantityStr);
                    } else {
                        quantity = 1;
                    }
                    break;
                default:
                    quantity = 1;
                    break;
            }
            final StringBuilder js = new StringBuilder();
            js.append("require([\"dojo/query\", \"dojo/dom\",\"dojo/dom-construct\",\"dojo/number\"],")
                .append(" function(query, dom, domConstruct,number){")
                .append("var ind = query(\"[name='").append(fieldName).append("']\")[0];")
                .append("if (typeof(ind)!=='undefined') {")
                .append("domConstruct.destroy(\"in").append(_key).append("\");")
                .append(" var x = domConstruct.create(\"span\", { id: \"in").append(_key)
                .append("\", class: \"eFapsIndividual\"}, ind);")
                .append(" var fs=  domConstruct.create(\"fieldset\", { }, x);")
                .append("domConstruct.create(\"legend\", { innerHTML: \"").append(_legend).append("\"}, fs);");

            if (quantity > 5) {
                js.append("var j = 0;");
            }
            if (quantity > 1) {
                js.append("for (var i=1;i < ").append(quantity + 1).append("; i++) {");
            }

            js.append(" domConstruct.create(\"label\", { innerHTML: number.format(i, {pattern:'00'}) +\".\"}, fs); ")
                .append(" domConstruct.create(\"input\", { name: \"").append(_key).append("\"}, fs);");

            if (quantity > 5) {
                js.append("j++;")
                    .append("if (j==5) {")
                    .append("j=0;")
                    .append("domConstruct.create(\"br\", {}, fs);")
                    .append("}");
            }
            if (quantity > 1) {
                js.append("}");
            }
            js.append("}")
                .append("});");

            InterfaceUtils.appendScript4FieldUpdate(_map, js);
        }
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
        final Product product = new Product()
        {
            @Override
            protected boolean add2QueryBldr4AutoComplete4Product(final Parameter _parameter,
                                                                 final QueryBuilder _queryBldr)
                throws EFapsException
            {
                return AbstractDocument_Base.this.add2QueryBldr4AutoComplete4Product(_parameter, _queryBldr);
            }
        };
        return product.autoComplete4Product(_parameter);
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _queryBldr    QueryBuilder to add to
     * @return true if allow cache, else false
     * @throws EFapsException on error
     */
    protected boolean add2QueryBldr4AutoComplete4Product(final Parameter _parameter,
                                                         final QueryBuilder _queryBldr)
        throws EFapsException
    {
        catalogFilter4productAutoComplete(_parameter, _queryBldr);
        if (Products.getSysConfig().getAttributeValueAsBoolean(ProductsSettings.ACTIVATEINDIVIDUAL)) {
            final Properties properties = Sales.getSysConfig().getAttributeValueAsProperties(
                            SalesSettings.AUTOCOMPLETE4PRODUCT, true);
            final String typeName = getTypeName4AutoComplete4Product(_parameter);
            // show products of type Individual and Batch
            final boolean showIndividual = "true".equalsIgnoreCase(properties.getProperty(typeName + ".ShowIndividual",
                            "false"));
            if (!showIndividual) {
                _queryBldr.addWhereAttrNotEqValue(CIProducts.ProductAbstract.Type,
                                CIProducts.ProductIndividual.getType().getId(),
                                CIProducts.ProductBatch.getType().getId());
            }
            // show products that are marked to use individual
            final boolean hideIndividualizable = "true".equalsIgnoreCase(properties.getProperty(typeName
                            + ".HideMarkedIndividual", "false"));
            if (hideIndividualizable) {
                _queryBldr.addWhereAttrNotEqValue(CIProducts.StockProductAbstract.Individual,
                                ProductIndividual.INDIVIDUAL,ProductIndividual.BATCH);
            }
        }
        return true;
    }

    /**
     * @param _parameter
     * @return
     */
    protected String getTypeName4AutoComplete4Product(final Parameter _parameter) throws EFapsException
    {
        return getTypeName4SysConf(_parameter);
    }

    protected void catalogFilter4productAutoComplete(final Parameter _parameter,
                                                     final QueryBuilder _queryBldr)
        throws EFapsException
    {
        final UITableCell tableCell = (UITableCell) _parameter.get(ParameterValues.CLASS);
        final AbstractCommand command = tableCell.getParent().getCommand();
        // evaluate the type
        Type typeDoc = command.getTargetCreateType();
        if (typeDoc == null) {
            if (_parameter.getInstance() == null) {
                final UITableCell cell = (UITableCell) _parameter.get(ParameterValues.CLASS);
                if (cell.getParent().getInstance() != null) {
                    typeDoc = cell.getParent().getInstance().getType();
                }
            } else {
                typeDoc = _parameter.getInstance().getType();
            }
        }

        if (typeDoc == null) {
            AbstractDocument_Base.LOG.error("No type found for: {}", _parameter);
        } else {
            final QueryBuilder queryBldr = new QueryBuilder(CISales.Products_Catalog2DocumentType);
            queryBldr.addWhereAttrEqValue(CISales.Products_Catalog2DocumentType.DocumentTypeLink, typeDoc.getId());
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selCat = new SelectBuilder()
                            .linkto(CISales.Products_Catalog2DocumentType.CatalogLinkAbstract).instance();
            multi.addSelect(selCat);
            multi.execute();
            final List<Instance> instances = new ArrayList<Instance>();
            while (multi.next()) {
                final Instance catInst = multi.<Instance>getSelect(selCat);
                instances.add(catInst);
            }
            if (!instances.isEmpty()) {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.Catalog2Products);
                attrQueryBldr.addWhereAttrEqValue(CIProducts.Catalog2Products.CatalogLinkAbstract,
                                instances.toArray());
                final AttributeQuery attrQuery = attrQueryBldr
                                .getAttributeQuery(CIProducts.Catalog2Products.ProductLink);
                _queryBldr.addWhereAttrInQuery(CIProducts.ProductAbstract.ID, attrQuery);
            }
        }

        final QueryBuilder queryBldr = new QueryBuilder(CISales.Products_Catalog2DocumentType);
        queryBldr.addWhereAttrEqValue(CISales.Products_Catalog2DocumentType.DocumentTypeLink, typeDoc.getId());
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selCat = new SelectBuilder()
                        .linkto(CISales.Products_Catalog2DocumentType.CatalogLinkAbstract).instance();
        multi.addSelect(selCat);
        multi.execute();
        final List<Instance> instances = new ArrayList<Instance>();
        while (multi.next()) {
            final Instance catInst = multi.<Instance>getSelect(selCat);
            instances.add(catInst);
        }
        if (!instances.isEmpty()) {
            final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.Catalog2Products);
            attrQueryBldr.addWhereAttrEqValue(CIProducts.Catalog2Products.CatalogLinkAbstract,
                            instances.toArray());
            final AttributeQuery attrQuery = attrQueryBldr
                            .getAttributeQuery(CIProducts.Catalog2Products.ProductLink);
            _queryBldr.addWhereAttrInQuery(CIProducts.ProductAbstract.ID, attrQuery);
        }
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
        String[] unitPrices = getUnitPricesFromUI(_parameter);
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
                                    discounts[i], priceFromDB, i);
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
     * @param _idx index
     * @throws EFapsException on error
     * @return new Calculator
     */
    //CHECKSTYLE:OFF
    public Calculator getCalculator(final Parameter _parameter,
                                    final Calculator _oldCalc,
                                    final String _oid,
                                    final String _quantity,
                                    final String _unitPrice,
                                    final String _discount,
                                    final boolean _priceFromDB,
                                    final int _idx)
        throws EFapsException
    {
        return new Calculator(_parameter, _oldCalc, _oid, _quantity, _unitPrice, _discount, _priceFromDB, this);
    }
    //CHECKSTYLE:ON


    /**
     * @param _parameter Parameter parameter as passe dfrom the eFaps API
     * @param _oldCalc calculator
     * @param _prodInstance Instance of the product
     * @param _quantity quantity
     * @param _unitPrice unit price
     * @param _discount discount
     * @param _priceFromDB must the price set from DB
     * @param _idx index
     * @throws EFapsException on error
     * @return new Calculator
     */
    //CHECKSTYLE:OFF
    public Calculator getCalculator(final Parameter _parameter,
                                    final Calculator _oldCalc,
                                    final Instance _prodInstance,
                                    final BigDecimal _quantity,
                                    final BigDecimal _unitPrice,
                                    final BigDecimal _discount,
                                    final boolean _priceFromDB,
                                    final int _idx)
        throws EFapsException
    {
        return new Calculator(_parameter, _oldCalc, _prodInstance, _quantity, _unitPrice, _discount, _priceFromDB, this);
    }
    //CHECKSTYLE:ON

    /**
     * @param _parameter Parameter parameter as passed from the eFaps API
     * @return the type name used in SystemConfiguration
     * @throws EFapsException on error
     */
    @Override
    public String getTypeName4SysConf(final Parameter _parameter)
        throws EFapsException
    {
        return CISales.DocumentAbstract.getType().getName();
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
     * @return type used for creation of positions
     * @throws EFapsException on error
     */
    protected Type getType4PositionUpdate(final Parameter _parameter)
        throws EFapsException
    {
        return getType4PositionCreate(_parameter);
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
        @SuppressWarnings("unchecked")
        final Map<Object, Object> props = (Map<Object, Object>) _parameter.get(ParameterValues.PROPERTIES);
        if (!props.containsKey("Type")) {
            props.put("Type", CIERP.Currency.getType().getName());
        }
        if (!props.containsKey("Select")) {
            props.put("Select", "attribute[Name]");
        }

        final Instance currInst = evaluateCurrency4JavaScript(_parameter);
        Context.getThreadContext().setSessionAttribute(AbstractDocument_Base.CURRENCYINST_KEY, currInst);

        final org.efaps.esjp.common.uiform.Field field = new org.efaps.esjp.common.uiform.Field()
        {

            @Override
            protected void updatePositionList(final Parameter _parameter,
                                              final List<DropDownPosition> _values)
                throws EFapsException
            {
                for (final DropDownPosition pos : _values) {
                    if (currInst.getId() == (Long) pos.getValue()) {
                        pos.setSelected(true);
                        break;
                    }
                }
            }
        };
        return field.dropDownFieldValue(_parameter);
    }

    protected Instance evaluateCurrencyInstance(final Parameter _parameter)
        throws CacheReloadException, EFapsException
    {
        return evaluateCurrency4JavaScript(_parameter);
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
     * Method to render a drop-down field containing all warehouses.
     *
     * @param _parameter Parameter as passed from eFaps.
     * @return Return containing a SNIPPLET.
     * @throws EFapsException on error.
     */
    public Return getStorageFieldValueUI(final Parameter _parameter)
        throws EFapsException
    {
        @SuppressWarnings("unchecked")
        final Map<String, Object> props = (Map<String, Object>) _parameter.get(ParameterValues.PROPERTIES);
        final String typeStr = (String) props.get("Type");
        if (typeStr == null) {
            props.put("Type", CIProducts.StorageAbstract.getType().getName());
        }
        final String select = (String) props.get("Select");
        if (select == null) {
            props.put("Select", "attribute[" + CIProducts.StorageAbstract.Name.name + "]");
        }

        final org.efaps.esjp.common.uiform.Field field = new org.efaps.esjp.common.uiform.Field() {
            @Override
            protected void updatePositionList(final Parameter _parameter,
                                              final List<DropDownPosition> _values)
                throws EFapsException
            {
                final Instance inst = Products.getSysConfig().getLink(ProductsSettings.DEFAULTWAREHOUSE);
                if (inst.isValid()) {
                    for (final DropDownPosition value  :_values) {
                        if (value.getValue().equals(inst.getId())) {
                            value.setSelected(true);
                        }
                    }
                }
            }
        };
        return field.dropDownFieldValue(_parameter);
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
                rate = (BigDecimal) RateFormatter.get().getFrmt4Rate().parse(_parameter.getParameterValue("rate"));
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
        final boolean useNumGen = "true".equalsIgnoreCase(getProperty(_parameter, "UseNumberGenerator4Name"));
        final String uuidStr = getProperty(_parameter, "NumberGenerator4NameUUID");
        String ret;
        if (useNumGen) {
            ret = super.getDocName4Create(_parameter);
        } else if (uuidStr != null && !uuidStr.isEmpty()) {
            final NumberGenerator numGen = NumberGenerator.get(UUID.fromString(uuidStr));
            ret = numGen.getNextVal();
        } else {
            ret = _parameter.getParameterValue("name4create");
        }
        return ret;
    }

    /**
     * Get the name for the document on edit.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return new Name
     * @throws EFapsException on error
     */
    protected String getDocName4Edit(final Parameter _parameter)
        throws EFapsException
    {
        return _parameter.getParameterValue("name4edit");
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
    public boolean priceFromUIisNet(final Parameter _parameter)
        throws EFapsException
    {
        return true;
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return validate(final Parameter _parameter)
        throws EFapsException
    {
        final Validation validation = new Validation();
        return validation.validate(_parameter, this);
    }

    /**
     * @param _parameter
     * @return
     */
    protected String[] getUnitPricesFromUI(final Parameter _parameter)
    {
        return _parameter.getParameterValues("netUnitPrice");
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _createdDoc   created doc
     * @throws EFapsException on error
     */
    protected void connect2DocumentType(final Parameter _parameter,
                                        final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final Instance instDocType = Instance.get(_parameter.getParameterValue("documentType"));
        if (instDocType.isValid() && _createdDoc.getInstance().isValid()) {
            insert2DocumentTypeAbstract(CISales.Document2DocumentType, _createdDoc, instDocType);
        }
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _createdDoc   created doc
     * @throws EFapsException on error
     */
    protected void connect2ProductDocumentType(final Parameter _parameter,
                                               final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final Instance instDocType = Instance.get(_parameter.getParameterValue("productDocumentType"));
        if (instDocType.isValid() && _createdDoc.getInstance().isValid()) {
            insert2DocumentTypeAbstract(CISales.Document2ProductDocumentType, _createdDoc, instDocType);
        }
    }

    /**
     * Method to add relation document and document type.
     *
     * @param _type CIType to create relation document and document type.
     * @param _createdDoc created doc.
     * @param _docTypeAbs instance the document type.
     * @throws EFapsException on error.
     */
    protected void insert2DocumentTypeAbstract(final CIType _type,
                                               final CreatedDoc _createdDoc,
                                               final Instance _docTypeAbs)
        throws EFapsException
    {
        final Insert insert = new Insert(_type);
        insert.add(CIERP.Document2DocumentTypeAbstract.DocumentLinkAbstract, _createdDoc.getInstance());
        insert.add(CIERP.Document2DocumentTypeAbstract.DocumentTypeLinkAbstract, _docTypeAbs);
        insert.execute();
    }

    public Return connectDocumentType2Catalog(final Parameter _parameter)
        throws EFapsException
    {
        final Instance catInst = _parameter.getInstance();
        final Long typeId = Long.parseLong(_parameter
                        .getParameterValue(CIFormSales.Sales_Products_Catalog2DocumentTypeForm.type.name));

        final Insert insert = new Insert(CISales.Products_Catalog2DocumentType);
        insert.add(CISales.Products_Catalog2DocumentType.CatalogLinkAbstract, catInst);
        insert.add(CISales.Products_Catalog2DocumentType.DocumentTypeLink, typeId);
        insert.execute();
        return new Return();
    }


    /**
     * Method to connect the document with the selected document type.
     *
     * @param _parameter    Parameter as passed from the eFaps API
     * @param _createdDoc   CreatedDoc  to be connected
     * @throws EFapsException on error
     */
    protected void connect2Derived(final Parameter _parameter,
                                   final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final String[] deriveds = _parameter.getParameterValues("derived");
        if (deriveds != null) {
            for (final String derived : deriveds) {
                final Instance derivedInst = Instance.get(derived);
                if (derivedInst.isValid() && _createdDoc.getInstance().isValid()) {
                    final Insert insert = new Insert(CISales.Document2DerivativeDocument);
                    insert.add(CISales.Document2DerivativeDocument.From, derivedInst);
                    insert.add(CISales.Document2DerivativeDocument.To, _createdDoc.getInstance());
                    insert.execute();
                }
            }
        }
    }

    /**
     * Method to connect the document with terms.
     *
     * @param _parameter    Parameter as passed from the eFaps API
     * @param _createdDoc   CreatedDoc  to be connected
     * @throws EFapsException on error
     */
    protected void connect2Terms(final Parameter _parameter,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final String[] terms = _parameter.getParameterValues("terms");
        if (terms != null) {
            for (final String term : terms) {
                if (!term.isEmpty() && _createdDoc.getInstance().isValid()) {
                    final Insert insert = new Insert(CISales.Document2TextElement);
                    insert.add(CISales.Document2TextElement.Document, _createdDoc.getInstance());
                    insert.add(CISales.Document2TextElement.TextElement, term);
                    insert.execute();
                }
            }
        }
    }


    /**
     * Method to get the javascript.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return javascript
     * @throws EFapsException on error
     */
    @Deprecated
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
    @Deprecated
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
            js.append(getRate4UI(_parameter, currency4Invoice)).append("'));");
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
            final Instance instance = getInstances4Derived(_parameter).get(0);
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
     * Method to set additional fields for the document.
     *
     * @param _parameter Paramter as passed by the eFaps API
     * @param _instance Instance of the document.
     * @return new StringBuilder with the additional fields.
     * @throws EFapsException on error
     */
    @Deprecated
    protected StringBuilder addAdditionalFields(final Parameter _parameter,
                                                final Instance _instance)
        throws EFapsException
    {
        return new StringBuilder();
    }


    /**
     * Method to get the JavaScript part for setting the values of the
     * document header and positions.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _instance instance to be copied
     * @return JavaScript
     * @throws EFapsException on error
     */
    @Deprecated
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

        final String typeName = getTypeName4SysConf(_parameter);
        final DecimalFormat totalFrmt = NumberFormatter.get().getFrmt4Total(typeName);
        final DecimalFormat quantityFrmt = NumberFormatter.get().getFrmt4Quantity(typeName);
        final DecimalFormat unitFrmt = NumberFormatter.get().getFrmt4UnitPrice(typeName);

        final StringBuilder currency = new StringBuilder();
        BigDecimal rate = null;
        BigDecimal[] ratesCur = null;
        if (rates != null) {
            final Instance currency4Invoice = Sales.getSysConfig().getLink(SalesSettings.CURRENCY4INVOICE);
            final Instance baseCurrency = Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);
            final Instance instanceDerived = getInstances4Derived(_parameter).get(0);
            boolean derived = false;
            if (instanceDerived.isValid()) {
                derived = true;
            }
            final Instance newInst = Instance.get(CIERP.Currency.getType(), rates[2].toString());
            Context.getThreadContext().setSessionAttribute(AbstractDocument_Base.CURRENCYINST_KEY, newInst);
            ratesCur = new PriceUtil().getExchangeRate(new DateTime().toDateMidnight().toDateTime(), newInst);

            if ((rates[2].equals(rates[3]) && !currency4Invoice.equals(baseCurrency) && !derived)
                            || !rates[2].equals(rates[3])) {
                currency.append(getSetFieldValue(0, "rateCurrencyId", "" + (rates[2])))
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
                            ? BigDecimal.ZERO.toString() : totalFrmt.format(netTotal))).append("\n")
            .append(getSetFieldValue(0, "crossTotal", netTotal == null
                            ? BigDecimal.ZERO.toString() : totalFrmt.format(crossTotal))).append("\n")
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

        final Map<Integer, Map<String, Object>> values = new TreeMap<Integer, Map<String, Object>>();

        while (multi.next()) {
            final Map<String, Object> map = new HashMap<String, Object>();

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
            map.put("quantity", quantityFrmt
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
                                : unitFrmt.format(rate != null ? rateNetUnitPrice : netUnitPrice));
                map.put("discountNetUnitPrice", rateDiscountNetUnitPrice == null || discountNetUnitPrice == null
                                ? BigDecimal.ZERO.toString()
                                : unitFrmt.format(rate != null ? rateDiscountNetUnitPrice : discountNetUnitPrice));
                map.put("netPrice", rateNetPrice == null || netPrice == null
                                ? BigDecimal.ZERO.toString()
                                : totalFrmt.format(rate != null ? rateNetPrice : netPrice));
            } else {
                map.put("netUnitPrice", netUnitPrice == null
                                ? BigDecimal.ZERO.toString()
                                : unitFrmt.format(rate != null ? netUnitPrice.divide(rate,
                                                BigDecimal.ROUND_HALF_UP) : netUnitPrice));
                map.put("discountNetUnitPrice", discountNetUnitPrice == null
                                ? BigDecimal.ZERO.toString()
                                : unitFrmt.format(rate != null ? discountNetUnitPrice.divide(rate,
                                                BigDecimal.ROUND_HALF_UP)
                                                : discountNetUnitPrice));
                map.put("netPrice", netPrice == null
                                ? BigDecimal.ZERO.toString()
                                : totalFrmt.format(rate != null ? netPrice.divide(rate, BigDecimal.ROUND_HALF_UP)
                                                : netPrice));
                map.put("discount", discount == null
                                ? BigDecimal.ZERO.toString()
                                : totalFrmt.format(discount));
            }
            values.put(multi.<Integer>getAttribute(CISales.PositionSumAbstract.PositionNumber), map);
        }

        final Set<String> noEscape = new HashSet<String>();
        noEscape.add("uoM");

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
     * @param _parameter Parameter as passed by the eFaps API
     * @return new empty Return
     * @throws EFapsException on error
     */
    public Return generateCode4Doc(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instance = _parameter.getInstance();

        if (instance != null && instance.isValid()) {
            final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionGroupRoot);
            queryBldr.addWhereAttrEqValue(CISales.PositionGroupRoot.DocumentAbstractLink, instance);
            queryBldr.addOrderByAttributeAsc(CISales.PositionGroupRoot.Order);
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.setEnforceSorted(true);
            multi.execute();
            int i = 1;
            while (multi.next()) {
                calculateCode(_parameter, multi.getCurrentInstance(), new Integer[] { i });
                i++;
            }
        }
        return new Return();
    }

    /**
     * Recursive method to set the code for groups.
     * @param _parameter Parameter as passed by the eFaps API
     * @param _parent instance the children will be searched for
     * @param _current Array used for calculation
     * @throws EFapsException on error
     */
    protected void calculateCode(final Parameter _parameter,
                                 final Instance _parent,
                                 final Integer[] _current)
        throws EFapsException
    {
        final Update update = new Update(_parent);
        update.add(CISales.PositionGroupAbstract.Code, getCode(_parameter, _current));
        update.execute();

        final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionGroupNode);
        queryBldr.addWhereAttrEqValue(CISales.PositionGroupNode.ParentGroupLink, _parent);
        queryBldr.addOrderByAttributeAsc(CISales.PositionGroupNode.Order);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.setEnforceSorted(true);
        multi.execute();
        int i = 1;
        while (multi.next()) {
            calculateCode(_parameter, multi.getCurrentInstance(), ArrayUtils.add(_current, i));
            i++;
        }
    }

    /**
     * Recursive method to set the code for groups.
     * @param _parameter Parameter as passed by the eFaps API
     * @param _current Array used for calculation
     * @return new code as String
     * @throws EFapsException on error
     */
    private String getCode(final Parameter _parameter,
                           final Integer[] _current)
        throws EFapsException
    {
        final StringBuilder code = new StringBuilder();
        for (int i = 0; i < _current.length; i++) {
            if (i > 0) {
                code.append(".");
            }
            code.append(_current[i]);
        }
        return code.toString();
    }

    public abstract static class KeyDef
    {

        private final String name;

        public KeyDef(final String _name)
        {
            this.name = _name;
        }

        public String getName()
        {
            return this.name;
        }

        public abstract Object convert4Map(final Object _value)
            throws EFapsException;

        @Override
        public boolean equals(final Object _obj)
        {
            boolean ret = false;
            if (_obj instanceof KeyDef) {
                ret = ((KeyDef) _obj).getName().equals(getName());
            } else {
                ret = super.equals(_obj);
            }
            return ret;
        }

        @Override
        public int hashCode()
        {
            return getName().hashCode();
        }
    }

    public static class KeyDefStr
        extends KeyDef
    {
        /**
         * @param _name
         */
        public KeyDefStr(final String _name)
        {
            super(_name);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object convert4Map(final Object _value)
        {
            return  _value;
        }
    }

    public static class KeyDefFrmt
        extends KeyDef
    {

        private final DecimalFormat format;

        /**
         * @param _name
         */
        public KeyDefFrmt(final String _name,
                          final DecimalFormat _format)
        {
            super(_name);
            this.format = _format;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object convert4Map(final Object _value)
            throws EFapsException
        {
            return this.format.format(_value);
        }
    }

}
