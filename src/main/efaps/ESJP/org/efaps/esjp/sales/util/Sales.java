/*
 * Copyright 2003 - 2015 The eFaps Team
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
 */

package org.efaps.esjp.sales.util;

import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.IBitEnum;
import org.efaps.admin.datamodel.IEnum;
import org.efaps.admin.datamodel.attributetype.BitEnumType;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.api.annotation.EFapsSysConfAttribute;
import org.efaps.api.annotation.EFapsSysConfLink;
import org.efaps.api.annotation.EFapsSystemConfiguration;
import org.efaps.esjp.admin.common.systemconfiguration.BooleanSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.EnumSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.IntegerSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.ListSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.PropertiesSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.StringSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.SysConfLink;
import org.efaps.esjp.ci.CIMsgContacts;
import org.efaps.esjp.common.jasperreport.StandartReport_Base.JasperMime;
import org.efaps.util.cache.CacheReloadException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("70a6a397-b8ef-40c5-853e-cff331bc79bb")
@EFapsApplication("eFapsApp-Sales")
@EFapsSystemConfiguration("c9a1cbc3-fd35-4463-80d2-412422a3802f")
public final class Sales
{

    /** The base. */
    public static final String BASE = "org.efaps.sales.";

    /** Sales-Configuration. */
    public static final UUID SYSCONFUUID = UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute COSTINGACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Costing.Activate")
                    .description("Allows to activate/deactivate the costing mechanisms.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute COSTINGOO4RT = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Costing.IncludeOrderOutbound4RecievingTicket")
                    .description("Include the OrderOutbound as last chance for costinrg on reciveing ticket.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final IntegerSysConfAttribute COSTINGMAXTRANSACTION = new IntegerSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Costing.MaxTransaction")
                    .description("Max number of transaction that will be analyzed at once before committing.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final ListSysConfAttribute COSTINGALTINSTS = new ListSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Costing.AlternativeCurrencies")
                    .description("List of Alternative Currency OIDs that will be calculated also.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink COSTINGSTORAGEGROUP = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Costing.StorageGroup")
                    .description("Storage Group that is used as a filter for using only the transactions "
                            + "that belong to the given StorageGroup, if not present the "
                            + "calculation is over all Storages.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PRODUCTABCREPORT4PROV = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Config4ProductABCReport4Provider")
                    .description("Configures the ABC Report for Providers.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PRODUCTABCREPORT4PROD = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Config4ProductABCReport4Product")
                    .description("Configures the ABC Report for Products..");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ACQUISITIONCOSTINGCREATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "AcquisitionCosting.Create")
                    .description("Allows create a AcquisitionCosting manually.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute DELIVERYNOTEJASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "DeliveryNote.JasperReport")
                    .description("Name of the jasperReport for DeliveryNote.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> DELIVERYNOTEMIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "DeliveryNote.Mime")
                    .clazz(JasperMime.class)
                    .description("Name of the jasperReport for DeliveryNote.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute DELIVERYNOTECONTMSGPH4ARP = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "DeliveryNote.ContactMsgPhrase4ArrivalPoint")
                    .defaultValue(CIMsgContacts.ContactAddressMsgPhrase.uuid.toString())
                    .description("MsgPhrase for a contact applied to ArrivalPoint .");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute DELIVERYNOTESUBCONTMSGPH4ARP = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "DeliveryNote.SubContactMsgPhrase4ArrivalPoint")
                    .defaultValue(CIMsgContacts.SubContactAddressMsgPhrase.uuid.toString())
                    .description("MsgPhrase for a sub contact applied to ArrivalPoint .");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INCOMINGEXCHANGEJASPERREPORT
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingExchange.JasperReport")
                    .description("Name of the jasperReport for IncomingExchange.");

    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INCOMINGEXCHANGEMIME
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingExchange.Mime")
                    .description("Mime of the JasperReport for IncomingExchange.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGINVOICEFROMORDEROUTBOUND = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingInvoice.CreateFromOrderOutbound")
                    .description("Allows to activate/deactivate the mechanisms to relate Incoming Invoice and "
                                    + "Order Outbound.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INCOMINGINVOICEFROMORDEROUTBOUNDAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingInvoice.CreateFromOrderOutboundAutoComplete")
                    .description("Config for a QueryBuilder for Autocomplete and Query of OrderOutbound to create "
                                    + "Incoming Invoice from.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGINVOICEFROMSERVORDEROUTBOUND = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingInvoice.CreateFromServiceOrderOutbound")
                    .description("Allows to activate/deactivate the mechanisms to relate Incoming Invoice and "
                                    + "Order Outbound.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INCOMINGINVOICEFROMSERVORDEROUTBOUNDAC
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingInvoice.CreateFromServiceOrderOutboundAutoComplete")
                    .description("Config for a QueryBuilder for Autocomplete and Query of OrderOutbound "
                                    + "to create Incoming Invoice from.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGINVOICEFROMRECIEVINGTICKET = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingInvoice.CreateFromRecievingTicket")
                    .description("Allows to activate/deactivate the mechanisms to relate Incoming Invoice "
                                    + "and Recieving Ticket.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INCOMINGINVOICEFROMRECIEVINGTICKETAC
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingInvoice.CreateFromRecievingTicketAutoComplete")
                    .description("Config for a QueryBuilder for Autocomplete and Query of RecievingTicket to create "
                                    + "Incoming Invoice from.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGINVOICEACTIVATECONDITION = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingInvoice.ActivateCondition")
                    .description("Activate the mechanism to assign a condition in Order Outbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute CHACTIVATESALESCOND = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Channel.ActivateSalesCondition")
                    .description("Activate the Sales Conditions.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute CHACTIVATEPURCHASECOND = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Channel.ActivatePurchaseCondition")
                    .description("Activate the Purchase Conditions");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute RECEIPTASSIGNEMPLOYEE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Receipt.AssignEmployee")
                    .description("Activate the mechanism to assign employee to Receipt");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute RECEIPTACTIVATECONDITION = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Receipt.ActivateCondition")
                    .description("Activate the mechanism to assign a condition in Receipt");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ORDEROUTBOUNDACTIVATECONDITION = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "OrderOutbound.ActivateCondition")
                    .description("Activate the mechanism to assign a condition in Order Outbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute ORDEROUTBOUNDJASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "OrderOutbound.JasperReport")
                    .description("Name of the jasperReport for Order Outbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute ORDEROUTBOUNDPRODUCTAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "OrderOutbound.ProductAutoComplete")
                    .description("Configuration for the AutoComplete for Products in OrderOutbound")
                    .addDefaultValue("Type", "Products_ProductAbstract");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute SERVICEORDEROUTBOUNDACTIVATECONDITION = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "ServiceOrderOutbound.ActivateCondition")
                    .description("Activate the mechanism to assign a condition in Order Outbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute SERVICEORDEROUTBOUNDJASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "ServiceOrderOutbound.JasperReport")
                    .description("Name of the jasperReport for Order Outbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INVOICEACTIVATECONDITION = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Invoice.ActivateCondition")
                    .description("Activate the mechanism to assign a condition in Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute INVOICEJASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Invoice.JasperReport")
                    .description("Name of the jasperReport for Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> INVOICEMIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Invoice.Mime")
                    .clazz(JasperMime.class)
                    .description("Name of the jasperReport for Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INVOICEASSIGNACTION = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Invoice.AssignAction")
                    .description("Activate the mechanism to assign a action in Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INVOICEFROMDELIVERYNOTE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Invoice.CreateFromDeliveryNote")
                    .description("Allows to activate/deactivate the mechanisms to relate Invoice and Delivery Note.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INVOICEASSIGNEMPLOYEE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Invoice.AssignEmployee")
                    .description("Activate the mechanism to assign employee to Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INVOICEAUTOCOMPLETE4PRODUCTS = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Invoice.AutoCompleteProperties4Products")
                    .description("Autocomplete Properties 4 Products in Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute CREDITNOTEJASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "CreditNote.JasperReport")
                    .description("Name of the jasperReport for CreditNote");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> CREDITNOTEMIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "CreditNote.Mime")
                    .clazz(JasperMime.class)
                    .description("Name of the jasperReport for CreditNote");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute REMINDERJASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Reminder.JasperReport")
                    .description("Name of the jasperReport for CreditNote");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> REMINDERMIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Reminder.Mime")
                    .clazz(JasperMime.class)
                    .description("Name of the jasperReport for CreditNote");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute EMPLOYEE2DOCREPORT = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.Employee2DocReport")
                    .description("Autocomplete Properties 4 Products in Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute USAGEREPORTJASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "UsageReport.JasperReport")
                    .description("Name of the jasperReport for UsageReport");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> USAGEREPORTMIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "UsageReport.Mime")
                    .clazz(JasperMime.class)
                    .description("Mime for the jasperReport for UsageReport");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute RETUSAGEREPJASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "ReturnUsageReport.JasperReport")
                    .description("Name of the jasperReport for ReturnUsageReport");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> RETUSAGEREPMIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "ReturnUsageReport.Mime")
                    .clazz(JasperMime.class)
                    .description("Mime for the jasperReport for ReturnUsageReport");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ORDERINBOUNDACTIVATEREMARK = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "OrderInbound.ActivateRemark")
                    .description("Activate the Remark Attribute for Order Inbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute ORDERINBOUNDSTATUS4CREATE = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "OrderInbound.Status4Create")
                    .description("Key of the Status for create.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute DELIVERYNOTEACTIVATEREMARK = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "DeliveryNote.ActivateRemark")
                    .description("Activate the Remark Attribute for Order Inbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PRODUCTREPORT = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.SalesProductReport")
                    .description("Properties 4 Products in Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PRODUCTREPORTASSIGENED = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.SalesProductReport.ActivateContactAssigned2Employee")
                    .description("Properties 4 Products in Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final IntegerSysConfAttribute PRODUCTREPORTPRODFAMLEVEL = new IntegerSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.SalesProductReport.ProductFamilyLevel")
                    .description("Properties 4 Products in Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute DOCSITUATIONREPORT = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.DocSituationReport")
                    .description("Properties 4 DocSituationReport");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute SALESREPORT4ACCOUNTIN = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.SalesReport4Account.IN")
                    .description("Properties 4 SalesReport4Account IN.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute SALESREPORT4ACCOUNTOUT = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.SalesReport4Account.OUT")
                    .description("Properties 4 SalesReport4Account OUT.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ACTIVATECOMPANYINDREPORT = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.ActivateCompanyIndependent")
                    .description("Activate the Company Independent Reports.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENTTHRESHOLD4PAID = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.Threshold4Paid")
                    .description("Properties to define a paid threshold for types. e.g. Sale_Invoice=0.05");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENTOUTCASHPAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.out.Cash.PayableDocuments")
                    .description("Properties to define the Query for Documents that can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENTOUTCHECKPAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.out.Check.PayableDocuments")
                    .description("Properties to define the Query for Documents that can be payed.");

    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENTOUTDEPOSITPAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.out.Deposit.PayableDocuments")
                    .description("Properties to define the Query for Documents taht can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENTOUTDETRACTIONPAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.out.Detraction.PayableDocuments")
                    .description("Properties to define the Query for Documents taht can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENTOUTEXCHANGEPAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.out.Exchange.PayableDocuments")
                    .description("Properties to define the Query for Documents taht can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute DOCPOSREPORT = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.DocPositionReport")
                    .description("Properties to define a paid threshold for types. e.g. Sale_Invoice=0.05");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute DOCPOSREPORTBOM = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.DocPositionReport.ActivateBOM")
                    .description("Activate the Company Independent Reports.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute DOCVSDOCREPORT = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.DocVsDocReport")
                    .description("Properties to configure the DocVsDocReport.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute RECIEVINGTICKETFROMORDEROUTBOUND = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "RecievingTicket.CreateFromOrderOutbound")
                    .description("Activate the Company Independent Reports.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute RECIEVINGTICKETFROMORDEROUTBOUNDAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "RecievingTicket.CreateFromOrderOutboundAutoComplete")
                    .description(" QueryBuilder for Autocomplete of OrderOutbound to create Recieving Ticket from.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute RECIEVINGTICKETPOSREMARK = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "RecievingTicket.ActivateRemark4Position")
                    .description("Activate the Company Independent Reports.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute QUOTATIONACTIVATECONDITION = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Quotation.ActivateCondition")
                    .description("Activate Condition for Quotation.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute QUOTATIONJASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Quotation.JasperReport")
                    .description("Name of the jasperReport for Quotation");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> QUOTATIONMIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Quotation.Mime")
                    .clazz(JasperMime.class)
                    .description("Name of the jasperReport for Quotation");

    /** See description. */
    @EFapsSysConfAttribute
    public static final IntegerSysConfAttribute PRODUCTIONCOSTINGMAXDEV = new IntegerSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "ProductionCosting.MaxDeviation")
                    .defaultValue(0)
                    .description("Maximum in percent that the new costing can deviat from the "
                                    + "currenct cost before triggering an alert. ");

    /**
     * Singelton.
     */
    private Sales()
    {
    }

    /**
    * Enum used for a multistate for Perception in Sales_Contacts_ClassTaxinfo.
    */
    public enum TaxPerception
        implements IEnum
    {
        /** Cliente Normal. DEFAULT VALUE if no information available. */
        CLIENT,
        /** Cliente final. */
        ENDCOSTUMER,
        /** Agente de Percepcion. */
        AGENT;

        @Override
        public int getInt()
        {
            return ordinal();
        }
    }

    /**
     * Enum used for a multistate for Perception in Sales_Contacts_ClassTaxinfo.
     */
    public enum TaxRetention
        implements IEnum
    {
        /** Cliente Normal. DEFAULT VALUE if no information available. */
        CLIENT,
        /** Agente de Retencion. */
        AGENT;

        @Override
        public int getInt()
        {
            return ordinal();
        }
    }

    /**
     * Enum used for a multistate for Activation in Sales_ProductDocumentType.
     */
    public enum ProdDocActivation
        implements IBitEnum
    {
        /** NONE. */
        NONE,
        /** Incoming. */
        INCOMING,
        /** Outgoing. */
        OUTGOING;

        /**
         * {@inheritDoc}
         */
        @Override
        public int getInt()
        {
            return BitEnumType.getInt4Index(ordinal());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getBitIndex()
        {
            return ordinal();
        }
    }

    /**
     * Enum used for a multistate for Activation in Sales_AccountCashDesk.
     */
    public enum AccountCDActivation
        implements IBitEnum
    {
        /** NONE. */
        CASH,
        /** CHECK. */
        CHECK,
        /** CARD. */
        CARD,
        /** DEPOSIT. */
        DEPOSIT,
        /** TAX. */
        TAX,
        /** EXCHANGE. */
        EXCHANGE;

        /**
         * {@inheritDoc}
         */
        @Override
        public int getInt()
        {
            return BitEnumType.getInt4Index(ordinal());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getBitIndex()
        {
            return ordinal();
        }
    }

    /**
     * Enum used for a multistate for Automation in Sales_AccountCashDesk.
     * <ol>
     * <li>Create Payment</li>
     * <li>Create Consolation</li>
     * <li>Create Transaction in Accounting</li>
     * </ol>
     */
    public enum AccountAutomation
        implements IEnum
    {

        /** NONE. */
        NONE,
        /** CONCILIATION. */
        CONCILIATION,
        /** TRANSACTION. */
        TRANSACTION,
        /** FULL. */
        FULL;

        /**
         * {@inheritDoc}
         */
        @Override
        public int getInt()
        {
            return BitEnumType.getInt4Index(ordinal());
        }
    }

    /**
     * @return the SystemConfigruation for Sales
     * @throws CacheReloadException on error
     */
    public static SystemConfiguration getSysConfig()
        throws CacheReloadException
    {
        return SystemConfiguration.get(SYSCONFUUID);
    }
}
