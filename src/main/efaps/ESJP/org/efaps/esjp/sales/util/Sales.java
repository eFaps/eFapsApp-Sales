/*
 * Copyright 2003 - 2016 The eFaps Team
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
import org.efaps.esjp.ci.CINumGenSales;
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
    public static final PropertiesSysConfAttribute AUTOCOMPLETE4PRODUCT = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "AutoComplete4Product")
                    .concatenate(true)
                    .description("General Configuration for Autocomplete for Products.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink DEFAULTTAXCAT4PRODUCT = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "DefaultTaxCatergory4Product")
                    .description("Default product document type for UsageReport.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute AFUNDSSETTLEDPERMITA = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "AccountFundsToBeSettled.PermitAugment")
                    .description("Permit the augmentation for FundstoBeSettled.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ACTIVATECOMPANYINDREPORT = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.ActivateCompanyIndependent")
                    .description("Activate the Company Independent Reports.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ACQUISITIONCOSTINGCREATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "AcquisitionCosting.Create")
                    .description("Allows create a AcquisitionCosting manually.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute CALCULATORCONFIG = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Config4Calculator")
                    .concatenate(true)
                    .description("Configuration for Calculators.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute CLASSTAXINFOACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "class.TaxInfo.Activate")
                    .description("Activate the classification Sales_Contacts_ClassTaxinfo.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute COMPARATORCONFIG = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Config4Comparator")
                    .concatenate(true)
                    .description("Configuration for Comparator. e.g. Default.Deviation4Quantity=0.01\n"
                                    + "Sales_OrderOutbound.Deviation4Net=0.01\n"
                                    + "Sales_OrderOutbound.EvaluateRateCurrency=true");

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
    @EFapsSysConfLink
    public static final SysConfLink CONCIL4AUTO = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Conciliation.Link4Automation")
                    .description("The Conciliation used for Automation Mechanism.");

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
    @EFapsSysConfLink
    public static final SysConfLink DEFAULTCURRENCY4DOC = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "DefaultCurrency4Documents")
                    .description("Activate the Remark Attribute for Order Inbound");

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
    public static final BooleanSysConfAttribute DELIVERYNOTEACTIVATEREMARK = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "DeliveryNote.ActivateRemark")
                    .description("Activate the Remark Attribute for Order Inbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final ListSysConfAttribute DELIVERYNOTEDEFAULTARRIVALPOINTS = new ListSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "DeliveryNote.DefaultArrivalPoints")
                    .description("Default addresses used as the departure point in delivery note.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final ListSysConfAttribute DELIVERYNOTEDEFAULTDEPARTUREPOINTS = new ListSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "DeliveryNote.DefaultDeparturePoints")
                    .description("Default addresses used as the departure point in delivery note.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink DELIVERYNOTEDEFAULTWAREHOUSE = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "DeliveryNote.DefaultWareHouse")
                    .description("Possibility to set a default Warehouse fo0r DeliveryNote. If not set "
                                    + "the default from Products applies.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute DOCSITUATIONREPORT = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.DocSituationReport")
                    .description("Properties 4 DocSituationReport");

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
    public static final PropertiesSysConfAttribute DOCSUMREPORT = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.DocumentSumReport")
                    .concatenate(true)
                    .description("Properties to configure the DocumentSumReport.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute EMPLOYEE2DOCREPORT = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.Employee2DocReport")
                    .description("Autocomplete Properties 4 Products in Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute EXCHANGEACTIVATEGUARANTEE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Exchange.ActivateGuarantee")
                    .description("Allows to activate/deactivate the realtion to a Guarantee.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute EXCHANGEACTIVATESITUATION = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Exchange.ActivateSituationLink")
                    .description("Allows to activate/deactivate the SituationLink.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute EXCHANGEUSENUMGEN = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Exchange.UseNumberGenerator")
                    .description("Use a NumberGenerator for the Name.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute EXCHANGE_REVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Exchange.RevisionSequence")
                    .defaultValue(CINumGenSales.ExchangeRevisionSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute FUNDSTOBESETTLEDRECEIPTREVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "FundsToBeSettledReceipt.RevisionSequence")
                    .defaultValue(CINumGenSales.IncomingInvoiceRevisionSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute GOODSISSUESLIPACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "GoodsIssueSlip.Activate")
                    .description("Activate GoodsIssueSlip mechnism");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute GOODSISSUESLIPASSCONTACT = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "GoodsIssueSlip.AssignContact")
                    .description("Activate GoodsIssueSlip mechnism");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute GOODSISSUESLIPASSEMPLOYEE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "GoodsIssueSlip.AssignEmployee")
                    .description("Activate GoodsIssueSlip mechnism");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink GOODSISSUESLIPDEFAULTWAREHOUSE = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "GoodsIssueSlip.DefaultWareHouse")
                    .description("Possibility to set a default Warehouse for GoodsIssueSlip. If not set "
                                    + "the default from Products applies.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute  GOODSISSUESLIPJASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "GoodsIssueSlip.JasperReport")
                    .description("Name of the jasperReport for GoodsIssueSlip.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime>  GOODSISSUESLIPMIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "GoodsIssueSlip.Mime")
                    .clazz(JasperMime.class)
                    .description("Name of the jasperReport for GoodsIssueSlip.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute INCOMINGCREDITNOTEREVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingCreditNote.RevisionSequence")
                    .defaultValue(CINumGenSales.IncomingInvoiceRevisionSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INCOMINGEXCHANGEJASPERREPORT
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingExchange.JasperReport")
                    .description("Name of the jasperReport for IncomingExchange.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> INCOMINGEXCHANGEMIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingExchange.Mime")
                    .clazz(JasperMime.class)
                    .description("Name of the jasperReport for IncomingExchange");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGEXCHANGEACTIVATEGUARANTEE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingExchange.ActivateGuarantee")
                    .description("Allows to activate/deactivate the realtion to a Guarantee.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGEXCHANGEACTIVATESITUATION = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingExchange.ActivateSituationLink")
                    .description("Allows to activate/deactivate the SituationLink.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGEXCHANGEUSENUMGEN = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingExchange.UseNumberGenerator")
                    .description("Use a NumberGenerator for the Name.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute INCOMINGEXCHANGE_REVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingExchange.RevisionSequence")
                    .defaultValue(CINumGenSales.IncomingExchangeRevisionSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

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
    public static final BooleanSysConfAttribute INCOMINGINVOICEACTIVATEREGPURPRICE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingInvoice.ActivateRegisterOfPuchasePrice")
                    .description("Allows to activate/deactivate the registering of the prices during a purchase.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGINVOICEACTPERC = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingInvoice.ActivatePerception")
                    .description("Activate the calculation of Perception.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGINVOICEACTRET = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingInvoice.ActivateRetention")
                    .description("Activate the calculation of Retention.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute INCOMINGINVOICEREVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingInvoice.RevisionSequence")
                    .defaultValue(CINumGenSales.IncomingInvoiceRevisionSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGPROFSERVRECACTRET = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingProfServReceipt.ActivateRetention")
                    .description("Activate the calculation of Retention.");


    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute INCOMINGPROFSERVRECEIPTREVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingProfServReceipt.RevisionSequence")
                    .defaultValue(CINumGenSales.IncomingInvoiceRevisionSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");


    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGPERCEPTIONCERTIFICATEACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingPerceptionCertificate.Activate")
                    .description("Activate Perception Certificates");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute INCOMINGRECEIPTREVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingReceipt.RevisionSequence")
                    .defaultValue(CINumGenSales.IncomingInvoiceRevisionSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGRECEIPTFROMORDEROUTBOUND = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingReceipt.CreateFromOrderOutbound")
                    .description("Allows to activate/deactivate the mechanisms to relate Incoming Receipt and "
                                    + "Order Outbound.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INCOMINGRECEIPTFROMORDEROUTBOUNDAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingReceipt.CreateFromOrderOutboundAutoComplete")
                    .description("Config for a QueryBuilder for Autocomplete and Query of OrderOutbound to create "
                                    + "Incoming Receipt from.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGRECEIPTFROMSERVORDEROUTBOUND = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingReceipt.CreateFromServiceOrderOutbound")
                    .description("Allows to activate/deactivate the mechanisms to relate Incoming Receipt and "
                                    + "Order Outbound.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INCOMINGRECEIPTFROMSERVORDEROUTBOUNDAC
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingReceipt.CreateFromServiceOrderOutboundAutoComplete")
                    .description("Config for a QueryBuilder for Autocomplete and Query of OrderOutbound "
                                    + "to create Incoming Receipt from.");




    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute INCOMINGREMINDERREVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingReminder.RevisionSequence")
                    .defaultValue(CINumGenSales.IncomingInvoiceRevisionSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGRETENTIONCERTIFICATEACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingRetentionCertificate.Activate")
                    .description("Activate Incoming Retention Certificates");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGRETENTIONACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingRetention.Activate")
                    .description("Activate Incoming Retention Certificates");

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
    public static final PropertiesSysConfAttribute PAYMENTTHRESHOLD4PAID = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.Threshold4Paid")
                    .description("Properties to define a paid threshold for types. e.g. Sale_Invoice=0.05");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PAYMENTAMOUNT4CREATEDDOC = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.Threshold4CreateOrder")
                    .description("Threshold  generated payment order or collection order.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYMENTCREDITCARDACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.in.CreditCard.Activate")
                    .description("Deactivate the automatic generation of a code for every Payment Documents.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENTCHECKPAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.in.Check.PayableDocuments")
                    .description("Properties to define the Query for Documents that can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYMENTDEBITCARDACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.in.DebitCard.Activate")
                    .description("Deactivate the automatic generation of a code for every Payment Documents.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYMENTDOCUMENTDEACTIVATECODE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.in.DeactivateCodeGeneration")
                    .description("Deactivate the automatic generation of a code for every Payment Documents.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PAYMENTDOCUMENTNUMGEN = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.in.NumberGenerator")
                    .defaultValue(CINumGenSales.PaymentDocumentSequence.uuid.toString())
                    .description("NumberGenerator Payment Documents.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYMENTGENERATEREPORT = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.in.GenerateReport")
                    .description("Activate the generation of a report for every payment.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYMENTDOCUMENTOUTACTIVATECODE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.out.DeactivateCodeGeneration")
                    .description("Deactivate the automatic generation of a code for every Payment Out Documents.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PAYMENTDOCUMENTOUTNUMGEN = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.out.NumberGenerator")
                    .defaultValue(CINumGenSales.PaymentDocumentOutSequence.uuid.toString())
                    .description("NumberGenerator Payment Out Documents.");

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

    /** See description. */
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
    public static final BooleanSysConfAttribute PETTYCASHERMITPARTIAL = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "AccountPettyCash.PermitPartialBalance")
                    .description("Permit the partial Balance of a AccountPettyCash.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PETTYCASHBALREQUIREBOOKED4PAY = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "PettyCashBalance.RequireBooked4Payment")
                    .description("Permit the partial Balance of a AccountPettyCash.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink PETTYCASHBALACTDEF4COLORD = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "PettyCashBalance.ActionDefinition4CollectionOrder")
                    .description("Default ActionDefinition for the creation of a Collection Order.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink PETTYCASHBALACTDEF4PAYORD = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "PettyCashBalance.ActionDefinition4PaymentOrder")
                    .description("Default ActionDefinition for the creation of a Payment Order..");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PETTYCASHRECEIPTREVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "PettyCashReceipt.RevisionSequence")
                    .defaultValue(CINumGenSales.IncomingInvoiceRevisionSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

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
    @EFapsSysConfLink
    public static final SysConfLink PRODSTOCKRPTSTORAGEGRP = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.ProductStockReport.StorageGroup")
                    .description("StorageGroup applied for the ProductStockReport.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final IntegerSysConfAttribute PRODUCTIONCOSTINGMAXDEV = new IntegerSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "ProductionCosting.MaxDeviation")
                    .defaultValue(0)
                    .description("Maximum in percent that the new costing can deviat from the "
                                    + "currenct cost before triggering an alert. ");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink PRODUCTIONREPORTDEFAULTWAREHOUSE = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "ProductionReport.DefaultWareHouse")
                    .description("Possibility to set a default Warehouse for ProductionReport. If not set "
                                    + "the default from Products applies.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PERCEPTIONCERTIFICATEACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "PerceptionCertificate.Activate")
                    .description("Activate Perception Certificates");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink RETURNUSAGEREPORTDEFAULTPRODUCTDOCUMENTTYPE = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "ReturnUsageReport.DefaultProductDocumentType")
                    .description("Default product document type for ReturnUsageReport.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink PRODUCTIONREPORTDEFAULTPRODUCTDOCUMENTTYPE = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "ProductionReport.DefaultProductDocumentType")
                    .description("Default product document type for ProductionReport.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PROFSERVRETENTIONCERTIFICATEACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "ProfServRetentionCertificate.Activate")
                    .description("Activate Retention Certificates");

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
    public static final StringSysConfAttribute RECIEVINGTICKETREVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "RecievingTicket.RevisionSequence")
                    .defaultValue(CINumGenSales.RecievingTicketRevisionSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink RECIEVINGTICKETDEFAULTWAREHOUSE = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "RecievingTicket.DefaultWareHouse")
                    .description("Possibility to set a default Warehouse for RecievingTicket. If not set "
                                    + "the default from Products applies.");

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
    public static final BooleanSysConfAttribute RETENTIONCERTIFICATEACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "RetentionCertificate.Activate")
                    .description("Activate Retention Certificates");

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
    public static final BooleanSysConfAttribute REPORT_CARRIER_ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.CarrierReport.Activate")
                    .description("Activate the CarrierReport.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute SALESREPORT4ACCOUNTIN = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.SalesReport4Account.IN")
                    .description("Properties 4 SalesReport4Account IN.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute SALESREPORT4ACCOUNTINASSIGENED = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.SalesReport4Account.IN.ActivateContactAssigned2Employee")
                    .description("Activate the column for Employee Assigned to Contact.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute SALESREPORT4ACCOUNTOUT = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.SalesReport4Account.OUT")
                    .description("Properties 4 SalesReport4Account OUT.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute SALESREPORT4ACCOUNTOUTASSIGENED = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.SalesReport4Account.OUT.ActivateContactAssigned2Employee")
                    .description("Activate the column for Employee Assigned to Contact.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute SERIALNUMBERS = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "SerialNumbers")
                    .concatenate(true)
                    .description("A mapping like: Sales_Invoice=001;002;003.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final IntegerSysConfAttribute SERIALNUMBERSUFFIXLENGTH = new IntegerSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "SerialNumbers.SuffixLength")
                    .defaultValue(6)
                    .description("Length of the number part of a Serialnumber .\"001-NNNNNN\". Default Value: 6");

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
    public static final PropertiesSysConfAttribute SWAPCONFIG = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Config4Swap")
                    .concatenate(true)
                    .description("Configuration for Swap. e.g.\n"
                                    + "TYPE.Type=TYPE\n"
                                    + "TYPE.StatusGroup=STATUSTYPE\n"
                                    + "TYPE.Status=STATUSKEY\n"
                                    + "TYPE.Filter4Contact=true\n");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute TRANSDOCJASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "TransactionDocument.JasperReport")
                    .description("Name of the jasperReport for CreditNote");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> TRANSDOCMIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "TransactionDocument.Mime")
                    .clazz(JasperMime.class)
                    .description("Name of the jasperReport for CreditNote");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink USAGEREPORTDEFAULTPRODUCTDOCUMENTTYPE = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "UsageReport.DefaultProductDocumentType")
                    .description("Default product document type for UsageReport.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink USAGEREPORTDEFAULTWAREHOUSE = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "UsageReport.DefaultWareHouse")
                    .description("Possibility to set a default Warehouse fo0r DeliveryNote. If not set "
                                    + "the default from Products applies.");

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
    public static final BooleanSysConfAttribute RESERVATIONACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Reservation.Activate")
                    .description("Activate reservations.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute RESERVATIONACTIVATETRANSTRIG = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Reservation.ActivateTransactionTrigger")
                    .description(" Activate the reservation trigger mechanism on transactions..");

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
