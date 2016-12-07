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
import org.efaps.esjp.admin.common.systemconfiguration.BitEnumSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.BooleanSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.EnumSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.IntegerSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.ListSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.PropertiesSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.StringSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.SysConfLink;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIMsgContacts;
import org.efaps.esjp.ci.CINumGenSales;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.StandartReport_Base.JasperActivation;
import org.efaps.esjp.common.jasperreport.StandartReport_Base.JasperMime;
import org.efaps.esjp.sales.cashflow.CashFlowCategory;
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
    public static final PropertiesSysConfAttribute PARTIALCONFIG = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Config4Partial")
                    .concatenate(true)
                    .description("Configuration for Partial. e.g:\n"
                                    + "CREATETYPE.CREATEFROMTYPE.RelationType=RELTYPE\n"
                                    + "RELTYPE.RelationOriginLink=Link to Origin Type\n"
                                    + "RELTYPE.RelationPartialLink=Link to Partial Type\n"
                                    + "Sales_DeliveryNote.Sales_Invoice.RelationType=Sales_Invoice2DeliveryNote\n"
                                    + "Sales_Invoice2DeliveryNote.RelationOriginLink=FromLink\n"
                                    + "Sales_Invoice2DeliveryNote.RelationPartialLink=ToLink\n");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute CASHFLOWREPORT_CONFIG = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.CashFlowReport")
                    .description("Properties for the  CashFlowReport.")
                    .addDefaultValue("Projection.IN.Type01", CISales.Installment.getType().getName())
                    .addDefaultValue("Projection.IN.Type02", CISales.Receipt.getType().getName())
                    .addDefaultValue(CISales.Installment.getType().getName() + ".FilterDate",
                                    CISales.Installment.DueDate.name)
                    .addDefaultValue(CISales.Installment.getType().getName() + ".Category",
                                    CashFlowCategory.CREDIT.name())
                    .addDefaultValue(CISales.Receipt.getType().getName() + ".Category",
                                    CashFlowCategory.SELL.name());

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
    public static final BooleanSysConfAttribute COSTING_ONECOST = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Costing.OverallResultOnly")
                    .description("Register only one amount with the overall result in Cost");

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
    public static final BooleanSysConfAttribute CREDIT_ASSEMPLOYEE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Credit.AssignEmployee")
                    .description("Allows to assign to an Employee.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute CREDIT_USENUMGEN = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Credit.UseNumberGenerator")
                    .description("Use a NumberGenerator for the Name.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute CREDITNOTE_FROMINVOICEAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "CreditNote.CreateFromInvoiceAutoComplete")
                    .description("Possibiloity to overwrite the configuraiton for the Autocomplete for Invoice.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute CREDITNOTE_JASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "CreditNote.JasperReport")
                    .description("Name of the jasperReport for CreditNote");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> CREDITNOTE_MIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "CreditNote.Mime")
                    .clazz(JasperMime.class)
                    .description("Name of the jasperReport for CreditNote");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BitEnumSysConfAttribute<JasperActivation> CREDITNOTE_JASPERACTIVATION
        = new BitEnumSysConfAttribute<JasperActivation>()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "CreditNote.JasperActivation")
                    .clazz(JasperActivation.class)
                    .addDefaultValue(JasperActivation.ONCREATE)
                    .addDefaultValue(JasperActivation.ONEDIT)
                    .description("Activation of Jasperreport Creation");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink DEFAULTCURRENCY4DOC = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "DefaultCurrency4Documents")
                    .description("Activate the Remark Attribute for Order Inbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute DELIVERYNOTE_PRODUCTAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "DeliveryNote.ProductAutoComplete")
                    .description("Possiblity to overwrite the standart properties for Product AutoComplete.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute DELIVERYNOTE_FROMINVOICEAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "DeliveryNote.CreateFromInvoiceAutoComplete")
                    .description("AutoComplete and Activation for Create from Invoice for DeliveryNote.")
                    .addDefaultValue("Type", "Sales_Invoice")
                    .addDefaultValue("StatusGroup01", "Sales_InvoiceStatus")
                    .addDefaultValue("Status01", "Open");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute DELIVERYNOTE_FROMRECEIPTAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "DeliveryNote.CreateFromReceiptAutoComplete")
                    .description("AutoComplete and Activation for Create from Receipt for DeliveryNote.")
                    .addDefaultValue("Type", "Sales_Receipt")
                    .addDefaultValue("StatusGroup01", "Sales_ReceiptStatus")
                    .addDefaultValue("Status01", "Open");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute DELIVERYNOTE_JASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "DeliveryNote.JasperReport")
                    .description("Name of the jasperReport for DeliveryNote.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> DELIVERYNOTE_MIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "DeliveryNote.Mime")
                    .clazz(JasperMime.class)
                    .description("Name of the jasperReport for DeliveryNote.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute DELIVERYNOTE_CONTMSGPH4ARP = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "DeliveryNote.ContactMsgPhrase4ArrivalPoint")
                    .defaultValue(CIMsgContacts.ContactAddressMsgPhrase.uuid.toString())
                    .description("MsgPhrase for a contact applied to ArrivalPoint .");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute DELIVERYNOTE_SUBCONTMSGPH4ARP = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "DeliveryNote.SubContactMsgPhrase4ArrivalPoint")
                    .defaultValue(CIMsgContacts.SubContactAddressMsgPhrase.uuid.toString())
                    .description("MsgPhrase for a sub contact applied to ArrivalPoint .");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute DELIVERYNOTE_ACTIVATEREMARK = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "DeliveryNote.ActivateRemark")
                    .description("Activate the Remark Attribute for Order Inbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final ListSysConfAttribute DELIVERYNOTE_DEFAULTARRIVALPOINTS = new ListSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "DeliveryNote.DefaultArrivalPoints")
                    .description("Default addresses used as the departure point in delivery note.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final ListSysConfAttribute DELIVERYNOTE_DEFAULTDEPARTUREPOINTS = new ListSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "DeliveryNote.DefaultDeparturePoints")
                    .description("Default addresses used as the departure point in delivery note.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink DELIVERYNOTE_DEFAULTWAREHOUSE = new SysConfLink()
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
    public static final PropertiesSysConfAttribute DOCPOSCOSTREPORT = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.DocPositionCostReport")
                    .addDefaultValue("Type01", "Sale_Invoice")
                    .addDefaultValue("Sales_Invoice.Total", "NET")
                    .addDefaultValue("StatusGroup01", "Sales_InvoiceStatus")
                    .addDefaultValue("Status01", "!Replaced ")
                    .description("Properties to define a paid threshold for types. e.g. Sale_Invoice=0.05");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute DOCPOSCOSTREPORTALTERNATIVE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.DocPositionCostReport.ActivateAlternative")
                    .description("Activate the Cost Alternative Mechanism.");

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
    public static final StringSysConfAttribute INCOMINGCHECK_REVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingCheck.RevisionSequence")
                    .defaultValue(CINumGenSales.IncomingCheckRevisionSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute INCOMINGCREDITNOTE_REVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingCreditNote.RevisionSequence")
                    .defaultValue(CINumGenSales.IncomingInvoiceRevisionSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INCOMINGCREDITNOTE_FROMINCINVOICEAC
            = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingCreditNote.CreateFromIncomingInvoiceAutoComplete")
                    .description("Possibiloity to overwrite the configuraiton for the Automcomplete for Invoice.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute  INCOMINGCREDITNOTE_TRANSDOC = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingCreditNote.ActivateTransDocShadow")
                    .description("Activate the possiblity to create a TransactionDocumentShadow");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink INCOMINGCREDITNOTE_DEFAULTWAREHOUSE = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingCreditNote.DefaultWareHouse")
                    .description("Possibility to set a default Warehouse for IncomingCreditNote-TransDocShadow. "
                                    + "If not set the default from Products applies.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink INCOMINGCREDITNOTE_DEFAULTPRODDOCTYPE = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingCreditNote.DefaultProductDocumentType")
                    .description("Possibility to set a default Product Document Type for "
                                    + "IncomingCreditNote-TransDocShadow. ");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INCOMINGEXCHANGE_JASPERREPORT
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingExchange.JasperReport")
                    .description("Name of the jasperReport for IncomingExchange.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> INCOMINGEXCHANGE_MIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingExchange.Mime")
                    .clazz(JasperMime.class)
                    .description("Name of the jasperReport for IncomingExchange");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGEXCHANGE_ACTIVATEGUARANTEE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingExchange.ActivateGuarantee")
                    .description("Allows to activate/deactivate the realtion to a Guarantee.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGEXCHANGE_USENUMGEN = new BooleanSysConfAttribute()
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
    public static final BooleanSysConfAttribute INCOMINGINVOICE_FROMORDEROUTBOUND = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingInvoice.CreateFromOrderOutbound")
                    .description("Allows to activate/deactivate the mechanisms to relate Incoming Invoice and "
                                    + "Order Outbound.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INCOMINGINVOICE_FROMORDEROUTBOUNDAC
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingInvoice.CreateFromOrderOutboundAutoComplete")
                    .addDefaultValue("Type", CISales.OrderOutbound.uuid.toString())
                    .description("Config for a QueryBuilder for Autocomplete and Query of OrderOutbound to create "
                                    + "Incoming Invoice from.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGINVOICE_FROMSERVORDEROUTBOUND = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingInvoice.CreateFromServiceOrderOutbound")
                    .description("Allows to activate/deactivate the mechanisms to relate Incoming Invoice and "
                                    + "Order Outbound.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INCOMINGINVOICE_FROMSERVORDEROUTBOUNDAC
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingInvoice.CreateFromServiceOrderOutboundAutoComplete")
                    .description("Config for a QueryBuilder for Autocomplete and Query of OrderOutbound "
                                    + "to create Incoming Invoice from.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGINVOICE_FROMRECIEVINGTICKET = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingInvoice.CreateFromRecievingTicket")
                    .description("Allows to activate/deactivate the mechanisms to relate Incoming Invoice "
                                    + "and Recieving Ticket.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INCOMINGINVOICE_FROMRECIEVINGTICKETAC
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingInvoice.CreateFromRecievingTicketAutoComplete")
                    .description("Config for a QueryBuilder for Autocomplete and Query of RecievingTicket to create "
                                    + "Incoming Invoice from.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGINVOICE_ACTIVATECONDITION = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingInvoice.ActivateCondition")
                    .description("Activate the mechanism to assign a condition in Order Outbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGINVOICE_ACTIVATEREGPURPRICE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingInvoice.ActivateRegisterOfPuchasePrice")
                    .description("Allows to activate/deactivate the registering of the prices during a purchase.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGINVOICE_ACTPERC = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingInvoice.ActivatePerception")
                    .description("Activate the calculation of Perception.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGINVOICE_ACTRET = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingInvoice.ActivateRetention")
                    .description("Activate the calculation of Retention.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute INCOMINGINVOICE_REVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingInvoice.RevisionSequence")
                    .defaultValue(CINumGenSales.IncomingInvoiceRevisionSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute  INCOMINGINVOICE_TRANSDOC = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingInvoice.ActivateTransDocShadow")
                    .description("Activate the possiblity to create a TransactionDocumentShadow");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink INCOMINGINVOICE_DEFAULTWAREHOUSE = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingInvoice.DefaultWareHouse")
                    .description("Possibility to set a default Warehouse for IncomingCreditNote-TransDocShadow. "
                                    + "If not set the default from Products applies.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INCOMINGINVOICE_PRODUCTAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingInvoice.ProductAutoComplete")
                    .description("Possiblity to overwrite the standart properties for Product AutoComplete.");

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
    public static final StringSysConfAttribute INCOMINGREMINDER_REVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingReminder.RevisionSequence")
                    .defaultValue(CINumGenSales.IncomingInvoiceRevisionSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INCOMINGREMINDER_FROMINCINVOICEAC
            = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingReminder.CreateFromIncomingInvoiceAutoComplete")
                    .description("Possibiloity to overwrite the configuraiton for the Automcomplete "
                                    + "for Incoming Invoice.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGRETENTIONCERTIFICATE_ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingRetentionCertificate.Activate")
                    .description("Activate Incoming Retention Certificates");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink INCOMINGRETENTIONCERTIFICATE_DEFAULTCURRENCY = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingRetentionCertificate.DefaultCurrency")
                    .description("Possibility to set a default Currenty for IncomingRetentionCertificate. "
                                    + "If not set the default from Products applies.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute INCOMINGRETENTIONCERTIFICATE_REVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingRetentionCertificate.RevisionSequence")
                    .defaultValue(CINumGenSales.IncomingRetentionCertificateRevisionSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGRETENTIONACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "IncomingRetention.Activate")
                    .description("Activate Incoming Retention Certificates");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INVOICE_ACTIVATECONDITION = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Invoice.ActivateCondition")
                    .description("Activate the mechanism to assign a condition in Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute INVOICE_JASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Invoice.JasperReport")
                    .description("Name of the jasperReport for Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> INVOICE_MIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Invoice.Mime")
                    .clazz(JasperMime.class)
                    .description("Name of the jasperReport for Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BitEnumSysConfAttribute<JasperActivation> INVOICE_JASPERACTIVATION
        = new BitEnumSysConfAttribute<JasperActivation>()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Invoice.JasperActivation")
                    .clazz(JasperActivation.class)
                    .addDefaultValue(JasperActivation.ONCREATE)
                    .addDefaultValue(JasperActivation.ONEDIT)
                    .description("Activation of Jasperreport Creation");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INVOICE_ASSIGNACTION = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Invoice.AssignAction")
                    .description("Activate the mechanism to assign a action in Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INVOICE_FROMDELIVERYNOTEAC
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Invoice.CreateFromDeliveryNoteAutoComplete")
                    .addDefaultValue("Type", CISales.DeliveryNote.uuid.toString())
                    .addDefaultValue("StatusGroup", CISales.DeliveryNoteStatus.getType().getName())
                    .addDefaultValue("Status", "Open")
                    .description("Config for a QueryBuilder for Autocomplete and Query of DeliveryNote to create "
                                    + "Invoice from.\n"
                                    + "AutoType=TOKEN\n"
                                    + "ExtraParameter=deliveryNote");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INVOICE_FROMQUOTATIONAC
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Invoice.CreateFromQuotationAutoComplete")
                    .addDefaultValue("Type", CISales.Quotation.uuid.toString())
                    .description("Config for a QueryBuilder for Autocomplete and Query of quotation to create "
                                    + " Invoice from.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INVOICE_FROMORDERINBOUNDAC
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Invoice.CreateFromOrderInboundAutoComplete")
                    .addDefaultValue("Type", CISales.OrderInbound.uuid.toString())
                    .addDefaultValue("StatusGroup", CISales.OrderInboundStatus.getType().getName())
                    .addDefaultValue("Status", "Open")
                    .description("Config for a QueryBuilder for Autocomplete and Query of OrderInbound to create "
                                    + " Invoice from.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INVOICE_FROMINVOICEAC
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Invoice.CreateFromInvoiceAutoComplete")
                    .addDefaultValue("Type", CISales.Invoice.uuid.toString())
                    .description("Config for a QueryBuilder for Autocomplete and Query of Invoice to create "
                                    + " Invoice from.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INVOICEASSIGNEMPLOYEE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Invoice.AssignEmployee")
                    .description("Activate the mechanism to assign employee to Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INVOICE_PRODUCTAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Invoice.ProductAutoComplete")
                    .description("Possiblity to overwrite the standart properties for Product AutoComplete.");;

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INSTALLMENT_USENUMGEN = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Installment.UseNumberGenerator")
                    .description("Use a NumberGenerator for the Name.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ORDEROUTBOUND_CREATEFROMPRODREQ = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "OrderOutbound.CreateFromProductRequest")
                    .description("Activate the ppossiblity to create an OrderOutbound from ProductRequest");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ORDEROUTBOUND_ACTIVATECONDITION = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "OrderOutbound.ActivateCondition")
                    .description("Activate the mechanism to assign a condition in Order Outbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute ORDEROUTBOUND_JASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "OrderOutbound.JasperReport")
                    .description("Name of the jasperReport for Order Outbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute ORDEROUTBOUND_PRODUCTAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "OrderOutbound.ProductAutoComplete")
                    .description("Configuration for the AutoComplete for Products in OrderOutbound")
                    .addDefaultValue("Type", "Products_ProductAbstract");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ORDERINBOUND_ACTIVATEREMARK = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "OrderInbound.ActivateRemark")
                    .description("Activate the Remark Attribute for Order Inbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute ORDERINBOUND_STATUS4CREATE = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "OrderInbound.Status4Create")
                    .description("Key of the Status for create.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENT_PAIDRULES = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.PaidRules")
                    .description("Properties to define the paid rules for types. e.g. \n"
                                    + "Sales_Invoice.Threshold=0.05\n"
                                    + "Sales_Invoice.PerPayment=false\n"
                                    + "Sales_Invoice.Swap.Status4From01=Open\n"
                                    + "OnStatusChange.StatusGroup=Sales_IncomingExchangeStatus\n"
                                    + "OnStatusChange.Status=Open\n");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PAYMENT_AMOUNT4CREATEDOC = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.Threshold4CreateOrder")
                    .description("Threshold  generated payment order or collection order.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYMENT_CREDITCARDACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.in.CreditCard.Activate")
                    .description("Deactivate the automatic generation of a code for every Payment Documents.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYMENT_DETRACTIONACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.in.Detraction.Activate")
                    .defaultValue(true)
                    .description("Activate the possiblity to use Detraction as a payment.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYMENT_RETENTIONACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.in.Retention.Activate")
                    .defaultValue(true)
                    .description("Activate the possiblity to use Retention as a payment.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENT_CASH_PAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.in.Cash.PayableDocuments")
                    .description("Properties to define the Query for Documents that can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENT_CHECK_PAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.in.Check.PayableDocuments")
                    .description("Properties to define the Query for Documents that can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENT_CREDITCARD_PAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.in.CreditCard.PayableDocuments")
                    .description("Properties to define the Query for Documents that can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENT_DEBITCARD_PAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.in.DebitCard.PayableDocuments")
                    .description("Properties to define the Query for Documents that can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENT_DEPOSIT_PAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.in.Deposit.PayableDocuments")
                    .description("Properties to define the Query for Documents that can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENT_DETRACTION_PAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.in.Detraction.PayableDocuments")
                    .description("Properties to define the Query for Documents that can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYMENT_INTERNAL_ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.in.Internal.Activate")
                    .description("Activate the Internal Payment Documents.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENT_INTERNAL_PAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.in.Internal.PayableDocuments")
                    .description("Properties to define the Query for Documents that can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENT_RETENTION_PAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.in.Retention.PayableDocuments")
                    .description("Properties to define the Query for Documents that can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENTDOCUMENT_TOBESETTLED = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.in.Config4ToBeSetteled")
                    .addDefaultValue("Type", CISales.OrderOutbound.uuid.toString())
                    .addDefaultValue("StatusGroup", CISales.OrderOutboundStatus.getType().getName())
                    .addDefaultValue("Status", "!" + CISales.OrderOutboundStatus.Canceled.key)
                    .description("QueryBuilder config for the Documents that must be settled.");

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
    public static final BooleanSysConfAttribute PAYMENTDOCUMENT_GENERATEREPORT = new BooleanSysConfAttribute()
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
    public static final BooleanSysConfAttribute PAYMENTDOCUMENTOUT_GENERATEREPORT = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.out.GenerateReport")
                    .description("Activate the generation of a report for every payment.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENTDOCUMENTOUT_TOBESETTLED = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.out.Config4ToBeSetteled")
                    .addDefaultValue("Type", CISales.OrderOutbound.uuid.toString())
                    .addDefaultValue("StatusGroup", CISales.OrderOutboundStatus.getType().getName())
                    .addDefaultValue("Status", "!" + CISales.OrderOutboundStatus.Canceled.key)
                    .description("QueryBuilder config for the Documents that must be settled.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PAYMENTDOCUMENTOUTNUMGEN = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.out.NumberGenerator")
                    .defaultValue(CINumGenSales.PaymentDocumentOutSequence.uuid.toString())
                    .description("NumberGenerator Payment Out Documents.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENTORDER_JASPERREPORT
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "PaymentOrder.JasperReport")
                    .description("Name of the jasperReport for PaymentOrder.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> PAYMENTORDER_MIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "PaymentOrder.Mime")
                    .clazz(JasperMime.class)
                    .description("Name of the jasperReport for PaymentOrder");

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
    public static final BooleanSysConfAttribute PAYMENTOUT_INTERNAL_ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.out.Internal.Activate")
                    .description("Activate the Internal Payment Documents.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENTOUT_INTERNAL_PAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "payment.out.Internal.PayableDocuments")
                    .description("Properties to define the Query for Documents that can be payed.");

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
    public static final BooleanSysConfAttribute PRODUCTREQUEST_ASSDEP = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "ProductRequest.AssignDepartment")
                    .description("Activate the mechanism to assign a department to ProductRequest.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PRODUCTREQUEST_AUTOCOMPLETE4PRODUCTS
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "ProductRequest.AutoComplete4Products")
                    .description("Possib ility to overwrite the dafutl Autocomplete for Products in ProductRequest.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PRODUCTREQUEST_JASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "ProductRequest.JasperReport")
                    .description("Name of the jasperReport for ProductRequest.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> PRODUCTREQUEST_MIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "ProductRequest.Mime")
                    .clazz(JasperMime.class)
                    .description("Mime of the jasperReport for ProductRequest.");

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
    public static final BooleanSysConfAttribute RECEIPT_ASSIGNEMPLOYEE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Receipt.AssignEmployee")
                    .description("Activate the mechanism to assign employee to Receipt");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute RECEIPT_ACTIVATECONDITION = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Receipt.ActivateCondition")
                    .description("Activate the mechanism to assign a condition in Receipt");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink  RECEIPT_DEFAULTCONDITION = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Receipt.DefaultCondition")
                    .description("Set the default condition for Receipt");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute RECEIPT_JASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Receipt.JasperReport")
                    .description("Name of the jasperReport for ReturnUsageReport");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> RECEIPT_MIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Receipt.Mime")
                    .clazz(JasperMime.class)
                    .description("Mime for the jasperReport for ReturnUsageReport");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BitEnumSysConfAttribute<JasperActivation> RECEIPT_JASPERACTIVATION
        = new BitEnumSysConfAttribute<JasperActivation>()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Receipt.JasperActivation")
                    .clazz(JasperActivation.class)
                    .addDefaultValue(JasperActivation.ONCREATE)
                    .addDefaultValue(JasperActivation.ONEDIT)
                    .description("Activation of Jasperreport Creation");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute RECEIPT_TRANSDOC = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Receipt.ActivateTransDocShadow")
                    .description("Activate the possiblity to create a TransactionDocumentShadow");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink RECEIPT_DEFAULTWAREHOUSE = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Receipt.DefaultWareHouse")
                    .description("Possibility to set a default Warehouse for RecievingTicket-ActivateTransDocShadow. "
                                    + "If not set the default from Products applies.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute RECEIPT_FROMDELIVERYNOTEAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Receipt.CreateDeliveryNoteAutoComplete")
                    .description("AutoComplete and Activation for Create from DeliveryNote for Receipt.")
                    .addDefaultValue("Type", "Sales_DeliveryNote")
                    .addDefaultValue("StatusGroup01", "Sales_DeliveryNoteStatus")
                    .addDefaultValue("Status01", "Open");

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
    public static final StringSysConfAttribute REMINDER_JASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Reminder.JasperReport")
                    .description("Name of the jasperReport for CreditNote");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> REMINDER_MIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Reminder.Mime")
                    .clazz(JasperMime.class)
                    .description("Name of the jasperReport for CreditNote");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BitEnumSysConfAttribute<JasperActivation> REMINDER_JASPERACTIVATION
        = new BitEnumSysConfAttribute<JasperActivation>()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Reminder.JasperActivation")
                    .clazz(JasperActivation.class)
                    .addDefaultValue(JasperActivation.ONCREATE)
                    .addDefaultValue(JasperActivation.ONEDIT)
                    .description("Activation of Jasperreport Creation");

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
    public static final PropertiesSysConfAttribute REPORT_ABCREPORT = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.ABCReport")
                    .description("Configuration for the ABCReport.")
                    .addDefaultValue("Type01", CISales.Invoice.getType().getName())
                    .addDefaultValue("Type02", CISales.Receipt.getType().getName());

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_CARRIER_ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.CarrierReport.Activate")
                    .description("Activate the CarrierReport.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_DOCPOS = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.DocPositionReport")
                    .description("Properties to define a paid threshold for types. e.g. Sale_Invoice=0.05")
                    .addDefaultValue("productType.Type01", CIProducts.StoreableProductAbstract.getType().getName())
                    .addDefaultValue("productType.Type02", CIProducts.UnstoreableProductAbstract.getType().getName());

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_DOCPOS_BOM = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.DocPositionReport.ActivateBOM")
                    .description("Activate the Company Independent Reports.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_DOCPOS_PRODAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.DocPositionReport.ProductAutoComplete")
                    .description("Properties to define the autocomplete for products");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_PARTIAL = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.PartialReport")
                    .description("Properties 4 PartialReport.")
                    .addDefaultValue("Type", CISales.Invoice2DeliveryNote.getType().getName());

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_PAYMENT = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.PaymentReport")
                    .description("Properties 4 PaymentReport.")
                    .addDefaultValue("IN.Type", CISales.PaymentDocumentAbstract.getType().getName())
                    .addDefaultValue("OUT.Type", CISales.PaymentDocumentOutAbstract.getType().getName())
                    .addDefaultValue("BOTH.Type", CISales.PaymentDocumentIOAbstract.getType().getName());

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_PAYMENTSUM = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.PaymentSumReport")
                    .description("Properties for PaymentSumReport.\n"
                                    + "OUT.Sales_PaymentDepositOut.Negate=true")
                    .addDefaultValue("PAYMENT.IN.Type", CISales.PaymentDocumentAbstract.getType().getName())
                    .addDefaultValue("PAYMENT.OUT.Type", CISales.PaymentDocumentOutAbstract.getType().getName())
                    .addDefaultValue("PAYMENT.BOTH.Type", CISales.PaymentDocumentIOAbstract.getType().getName())
                    .addDefaultValue("DOCUMENT.IN.Type", CISales.IncomingCheck.getType().getName())
                    .addDefaultValue("DOCUMENT.BOTH.Type", CISales.IncomingCheck.getType().getName());

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_SALESPROD = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.SalesProductReport")
                    .description("Properties 4 Products in Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_SALESPROD_ASSIGENED = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.SalesProductReport.ActivateContactAssigned2Employee")
                    .description("Properties 4 Products in Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_SALESPROD_CONDITION = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.SalesProductReport.ActivateCondition")
                    .description("Properties 4 Products in Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final IntegerSysConfAttribute REPORT_SALESPROD_PRODFAMLEVEL = new IntegerSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.SalesProductReport.ProductFamilyLevel")
                    .description("Properties 4 Products in Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_SWAP = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.SwapReport")
                    .description("Properties for SwapReport.")
                    .addDefaultValue("Type", CISales.DocumentSumAbstract.getType().getName());

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_SALESRECORD = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.SalesRecordReport")
                    .description("Properties for SalesRecordReport.")
                    .addDefaultValue("Type01", CISales.Invoice.getType().getName())
                    .addDefaultValue("Type02", CISales.Receipt.getType().getName())
                    .addDefaultValue("Type03", CISales.CreditNote.getType().getName())
                    .addDefaultValue("Type04", CISales.Reminder.getType().getName());

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_DOCPRODTRANS = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.DocProductTransactionReport")
                    .description("Properties for DocProductTransactionReport.")
                    .addDefaultValue("Type", CISales.DeliveryNote.getType().getName());

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_DOCPRODTRANS_ACTPRODTYPE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.DocProductTransactionReport.ActivateProdType")
                    .description("Activate the ProductType field for the DocProductTransactionReport.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_DOCPRODTRANS_FAB = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.DocProductTransactionReport.AnalyzeFabrication")
                    .description("Activate the Anlization for fabrication for the DocProductTransactionReport.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_DOCPRODTRANSCOST = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.DocProductTransactionCostReport")
                    .description("Properties for DocProductTransactionReportCost.")
                    .addDefaultValue("Type", CISales.DeliveryNote.getType().getName());

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_DOCPRODTRANSCOST_ACTPRODTYPE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.DocProductTransactionCostReport.ActivateProdType")
                    .description("Activate the ProductType field for the DocProductTransactionReport.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_DOCPRODTRANSCOST_FAB = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.DocProductTransactionCostReport.AnalyzeFabrication")
                    .description("Activate the Anlization for fabrication for the DocProductTransactionReport.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_DOCBALANCE = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.DocBalanceReport")
                    .addDefaultValue("Type01", CISales.Invoice.getType().getName())
                    .addDefaultValue("Type02", CISales.CreditNote.getType().getName())
                    .addDefaultValue("Type03", CISales.Reminder.getType().getName())
                    .addDefaultValue("Type04", CISales.IncomingExchange.getType().getName())
                    .description("Configures the ABC Report for Providers.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_STATISTICS = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.Statistics")
                    .description("Configuration for the StatisticsReport.")
                    .addDefaultValue("Type", CIERP.DocumentAbstract.getType().getName());

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute SALESREPORT4ACCOUNTIN = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.SalesReport4Account.IN")
                    .description("Properties 4 SalesReport4Account IN.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute SALESREPORT4ACCOUNTIN_ASSIGENED = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.SalesReport4Account.IN.ActivateContactAssigned2Employee")
                    .description("Activate the column for Employee Assigned to Contact.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute SALESREPORT4ACCOUNTIN_SWAPINFO = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.SalesReport4Account.IN.ActivateSwapInfo")
                    .description("Activate the column for Swap related Infos.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute SALESREPORT4ACCOUNTOUT = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.SalesReport4Account.OUT")
                    .description("Properties 4 SalesReport4Account OUT.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute SALESREPORT4ACCOUNTOUT_ASSIGENED = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.SalesReport4Account.OUT.ActivateContactAssigned2Employee")
                    .description("Activate the column for Employee Assigned to Contact.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute SALESREPORT4ACCOUNTOUT_SWAPINFO = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.SalesReport4Account.OUT.ActivateSwapInfo")
                    .description("Activate the column for Swap related Infos.");



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
    public static final BooleanSysConfAttribute SERVICEREQUEST_ASSDEP = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "ServiceRequest.AssignDepartment")
                    .description("Activate the mechanism to assign a department to ServiceRequest.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute SERVICEREQUEST_AUTOCOMPLETE4PRODUCTS
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "ServiceRequest.AutoComplete4Products")
                    .description("Possib ility to overwrite the dafutl Autocomplete for Products in ServiceRequest.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute SERVICEREQUEST_JASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "ServiceRequest.JasperReport")
                    .description("Name of the jasperReport for ServiceRequest.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> SERVICEREQUEST_MIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "ServiceRequest.Mime")
                    .clazz(JasperMime.class)
                    .description("Mime of the jasperReport for ServiceRequest.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute SERVICEORDEROUTBOUND_CREATEFROMSERVDREQ = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "ServiceOrderOutbound.CreateFromServiceRequest")
                    .description("Activate the ppossiblity to create an ServiceOrderOutbound from ServiceRequest");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute SERVICEORDEROUTBOUND_ACTIVATECONDITION = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "ServiceOrderOutbound.ActivateCondition")
                    .description("Activate the mechanism to assign a condition in Order Outbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute SERVICEORDEROUTBOUND_JASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "ServiceOrderOutbound.JasperReport")
                    .description("Name of the jasperReport for Service Order Outbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> SERVICEORDEROUTBOUND_MIME
        = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "ServiceOrderOutbound.Mime")
                    .clazz(JasperMime.class)
                    .description("Mime of the jasperReport for Service Order Outbound.");

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
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute TRANSDOCSHADOWIN_REVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "TransactionDocumentShadowIn.RevisionSequence")
                    .defaultValue(CINumGenSales.TransactionDocumentShadowInSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute TRANSDOCSHADOWOUT_REVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "TransactionDocumentShadowOut.RevisionSequence")
                    .defaultValue(CINumGenSales.TransactionDocumentShadowOutSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute USAGEREPORT_PRODUCTAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "UsageReport.ProductAutoComplete")
                    .description("Possiblity to overwrite the standart properties for Product AutoComplete.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink USAGEREPORT_DEFAULTPRODUCTDOCUMENTTYPE = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "UsageReport.DefaultProductDocumentType")
                    .description("Default product document type for UsageReport.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink USAGEREPORT_DEFAULTWAREHOUSE = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "UsageReport.DefaultWareHouse")
                    .description("Possibility to set a default Warehouse fo0r DeliveryNote. If not set "
                                    + "the default from Products applies.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute USAGEREPORT_JASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "UsageReport.JasperReport")
                    .description("Name of the jasperReport for UsageReport");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> USAGEREPORT_MIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "UsageReport.Mime")
                    .clazz(JasperMime.class)
                    .description("Mime for the jasperReport for UsageReport");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REMINDER_FROMINVOICEAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Reminder.CreateFromInvoiceAutoComplete")
                    .description("Possibiloity to overwrite the configuraiton for the Automcomplete for Invoice.");

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

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink EXTEND_PRODDOC4MOVEMASS = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "extend.ProductDocumentType4MoveMassive")
                    .description("Default Product Document type for move massive.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink EXTEND_PRODDOC4MOVE = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "extend.ProductDocumentType4Move")
                    .description("Default Product Document type for move.");

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
        EXCHANGE,
        /** The internal. */
        INTERNAL;

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
