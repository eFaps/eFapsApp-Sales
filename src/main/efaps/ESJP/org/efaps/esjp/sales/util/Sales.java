//CHECKSTYLE:OFF
//CHECKSTYLE:ON
/*
 * Copyright 2003 - 2020 The eFaps Team
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
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "DefaultTaxCatergory4Product")
                    .description("Default product document type for UsageReport.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute AFUNDSSETTLED_PERMITA = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "AccountFundsToBeSettled.PermitAugment")
                    .description("Permit the augmentation for FundstoBeSettled.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute AFUNDSSETTLED_FILE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "AccountFundsToBeSettled.ActivateFiles")
                    .description("Activate file upload for AccountFundsToBeSettled");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ACTIVATECOMPANYINDREPORT = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.ActivateCompanyIndependent")
                    .description("Activate the Company Independent Reports.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ACQUISITIONCOSTINGCREATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "AcquisitionCosting.Create")
                    .description("Allows create a AcquisitionCosting manually.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute BIN_INCOMINGEXCHANGE_ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "BinIncomingExchange.Activate")
                    .defaultValue(false)
                    .description("Activate BinIncomingExchange.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute BIN_INCOMINGEXCHANGE_NUMGEN = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "BinIncomingExchange.NumberGenerator")
                    .defaultValue(CINumGenSales.BinIncomingExchangeSequence.uuid.toString())
                    .description("NumberGenerator for BinIncomingExchange.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute CALCULATORCONFIG = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Config4Calculator")
                    .concatenate(true)
                    .description("Configuration for Calculators.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PARTIALCONFIG = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Config4Partial")
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
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.CashFlowReport")
                    .description("Properties for the  CashFlowReport.")
                    .addDefaultValue("Projection.IN.Type01", CISales.Installment.getType().getName())
                    .addDefaultValue("Projection.IN.StatusGroup01", CISales.InstallmentStatus.getType().getName())
                    .addDefaultValue("Projection.IN.Status01", "!" + CISales.InstallmentStatus.Canceled.key)
                    .addDefaultValue("Projection.IN.Type02", CISales.Receipt.getType().getName())
                    .addDefaultValue("Projection.IN.StatusGroup02", CISales.ReceiptStatus.getType().getName())
                    .addDefaultValue("Projection.IN.Status02", "!" + CISales.ReceiptStatus.Replaced.key)
                    .addDefaultValue(CISales.Installment.getType().getName() + ".FilterDate",
                                    CISales.Installment.DueDate.name)
                    .addDefaultValue(CISales.Installment.getType().getName() + ".Category",
                                    CashFlowCategory.CREDIT.name())
                    .addDefaultValue(CISales.Receipt.getType().getName() + ".Category",
                                    CashFlowCategory.SELL.name());

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute CLASSTAXINFOACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "class.TaxInfo.Activate")
                    .description("Activate the classification Sales_Contacts_ClassTaxinfo.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute COMPARATORCONFIG = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Config4Comparator")
                    .concatenate(true)
                    .description("Configuration for Comparator. e.g. Default.Deviation4Quantity=0.01\n"
                                    + "Sales_OrderOutbound.Deviation4Net=0.01\n"
                                    + "Sales_OrderOutbound.EvaluateRateCurrency=true");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute CHACTIVATESALESCOND = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Channel.ActivateSalesCondition")
                    .description("Activate the Sales Conditions.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute CHACTIVATEPURCHASECOND = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Channel.ActivatePurchaseCondition")
                    .description("Activate the Purchase Conditions");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink CONCIL4AUTO = new SysConfLink()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Conciliation.Link4Automation")
                    .description("The Conciliation used for Automation Mechanism.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute COSTINGACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Costing.Activate")
                    .description("Allows to activate/deactivate the costing mechanisms.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute COSTINGOO4RT = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Costing.IncludeOrderOutbound4RecievingTicket")
                    .description("Include the OrderOutbound as last chance for costinrg on reciveing ticket.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute COSTING_ONECOST = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Costing.OverallResultOnly")
                    .description("Register only one amount with the overall result in Cost");

    /** See description. */
    @EFapsSysConfAttribute
    public static final IntegerSysConfAttribute COSTINGMAXTRANSACTION = new IntegerSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Costing.MaxTransaction")
                    .description("Max number of transaction that will be analyzed at once before committing.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final ListSysConfAttribute COSTINGALTINSTS = new ListSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Costing.AlternativeCurrencies")
                    .description("List of Alternative Currency OIDs that will be calculated also.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink COSTINGSTORAGEGROUP = new SysConfLink()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Costing.StorageGroup")
                    .description("Storage Group that is used as a filter for using only the transactions "
                            + "that belong to the given StorageGroup, if not present the "
                            + "calculation is over all Storages.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute CONTRACT_ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Contract.Activate")
                    .description("Main switch to activate Contracts.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute CONTRACT_NUMGEN = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Contract.NumberGenerator")
                    .defaultValue(CINumGenSales.ContractSequence.uuid.toString())
                    .description("NumberGenerator for Contract.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute CONTRACT_STATUS4CREATE = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Contract.Status4Create")
                    .defaultValue(CISales.ContractStatus.Draft.key)
                    .description("Key of the Status for create.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute CREDIT_ASSIGNEMPLOYEE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Credit.AssignEmployee")
                    .description("Allows to assign to an Employee.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute CREDIT_USENUMGEN = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Credit.UseNumberGenerator")
                    .description("Use a NumberGenerator for the Name.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute CREDITLINE_ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "CreditLine.Activate")
                    .description("Activate the Credit Line Mechanism.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute CREDITLINE_LISTENER = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "CreditLine.Listener")
                    .description("Listener configuration for CreditLine.")
                    .addDefaultValue("ADD.StatusGroup01", CISales.InvoiceStatus.getType().getName())
                    .addDefaultValue("ADD.Status01", CISales.InvoiceStatus.Draft.key)
                    .addDefaultValue("ADD.StatusGroup02", CISales.InvoiceStatus.getType().getName())
                    .addDefaultValue("ADD.Status02", CISales.InvoiceStatus.Open.key)
                    .addDefaultValue("REMOVE.StatusGroup01", CISales.InvoiceStatus.getType().getName())
                    .addDefaultValue("REMOVE.Status01", CISales.InvoiceStatus.Paid.key)
                    .addDefaultValue("REMOVE.StatusGroup02", CISales.InvoiceStatus.getType().getName())
                    .addDefaultValue("REMOVE.Status02", CISales.InvoiceStatus.Replaced.key);

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute CREDITLINE_NUMGEN = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "CreditLine.NumberGenerator")
                    .defaultValue(CINumGenSales.CreditLineSequence.uuid.toString())
                    .description("NumberGenerator for CreditLine.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute CREDITNOTE_FROMINVOICEAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "CreditNote.CreateFromInvoiceAutoComplete")
                    .description("AutoComplete and Activation for Create from Receipt for DeliveryNote.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute CREDITNOTE_FROMRECEIPTAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "CreditNote.CreateFromReceiptAutoComplete")
                    .description("AutoComplete and Activation for Create from Receipt for DeliveryNote.")
                    .addDefaultValue("Type", "Sales_Receipt")
                    .addDefaultValue("StatusGroup01", "Sales_ReceiptStatus")
                    .addDefaultValue("Status01", "*");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute CREDITNOTE_JASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "CreditNote.JasperReport")
                    .description("Name of the jasperReport for CreditNote");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> CREDITNOTE_MIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "CreditNote.Mime")
                    .clazz(JasperMime.class)
                    .description("Name of the jasperReport for CreditNote");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BitEnumSysConfAttribute<JasperActivation> CREDITNOTE_JASPERACTIVATION
        = new BitEnumSysConfAttribute<JasperActivation>()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "CreditNote.JasperActivation")
                    .clazz(JasperActivation.class)
                    .addDefaultValue(JasperActivation.ONCREATE)
                    .addDefaultValue(JasperActivation.ONEDIT)
                    .description("Activation of Jasperreport Creation");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute CREDITNOTE_SWAP = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "CreditNote.ActivateSwap")
                    .description("Use a NumberGenerator for the Name.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute CREDITNOTE_REASON = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "CreditNote.ActivateReason")
                    .description("Activate/Deactivate the mechanism to assign a Reason to CreditNote.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink DEFAULTCURRENCY4DOC = new SysConfLink()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "DefaultCurrency4Documents")
                    .description("Activate the Remark Attribute for Order Inbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute DELIVERYNOTE_PRODUCTAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "DeliveryNote.ProductAutoComplete")
                    .description("Possiblity to overwrite the standart properties for Product AutoComplete.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute DEPENDENTSLISTENER = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "DependentListener")
                    .addDefaultValue("TriggerStatus.StatusGroup01", CISales.IncomingInvoiceStatus.getType().getName())
                    .addDefaultValue("TriggerStatus.Status01", CISales.IncomingInvoiceStatus.Replaced.key)
                    .addDefaultValue("TriggerStatus.StatusGroup02",
                                    CISales.IncomingProfServReceiptStatus.getType().getName())
                    .addDefaultValue("TriggerStatus.Status02", CISales.IncomingProfServReceiptStatus.Canceled.key)
                    .addDefaultValue(CISales.IncomingInvoice.getType().getName() + ".RelationType01",
                                    CISales.IncomingRetention2IncomingInvoice.getType().getName())
                    .addDefaultValue(CISales.IncomingInvoice.getType().getName() + ".InstanceLink01",
                                    CISales.IncomingRetention2IncomingInvoice.ToLink.name)
                    .addDefaultValue(CISales.IncomingInvoice.getType().getName() + ".DependentLink01",
                                    CISales.IncomingRetention2IncomingInvoice.FromLink.name)
                    .addDefaultValue(CISales.IncomingInvoice.getType().getName() + ".DependentStatusGroup01",
                                    CISales.IncomingRetentionStatus.getType().getName())
                    .addDefaultValue(CISales.IncomingInvoice.getType().getName() + ".DependentStatus01",
                                    CISales.IncomingRetentionStatus.Canceled.key)
                    .addDefaultValue(CISales.IncomingInvoice.getType().getName() + ".RelationType02",
                                    CISales.IncomingDetraction2IncomingInvoice.getType().getName())
                    .addDefaultValue(CISales.IncomingInvoice.getType().getName() + ".InstanceLink02",
                                    CISales.IncomingDetraction2IncomingInvoice.ToLink.name)
                    .addDefaultValue(CISales.IncomingInvoice.getType().getName() + ".DependentLink02",
                                    CISales.IncomingDetraction2IncomingInvoice.FromLink.name)
                    .addDefaultValue(CISales.IncomingInvoice.getType().getName() + ".DependentStatusGroup02",
                                    CISales.IncomingDetractionStatus.getType().getName())
                    .addDefaultValue(CISales.IncomingInvoice.getType().getName() + ".DependentStatus02",
                                    CISales.IncomingDetractionStatus.Canceled.key)
                    .addDefaultValue(CISales.IncomingInvoice.getType().getName() + ".RelationType03",
                                    CISales.IncomingPerceptionCertificate2IncomingInvoice.getType().getName())
                    .addDefaultValue(CISales.IncomingInvoice.getType().getName() + ".InstanceLink03",
                                    CISales.IncomingPerceptionCertificate2IncomingInvoice.ToLink.name)
                    .addDefaultValue(CISales.IncomingInvoice.getType().getName() + ".DependentLink03",
                                    CISales.IncomingPerceptionCertificate2IncomingInvoice.FromLink.name)
                    .addDefaultValue(CISales.IncomingInvoice.getType().getName() + ".DependentStatusGroup03",
                                    CISales.IncomingPerceptionCertificateStatus.getType().getName())
                    .addDefaultValue(CISales.IncomingInvoice.getType().getName() + ".DependentStatus03",
                                    CISales.IncomingPerceptionCertificateStatus.Canceled.key)
                    .addDefaultValue(CISales.IncomingProfServReceipt.getType().getName() + ".RelationType01",
                                    CISales.IncomingProfServRetention2IncomingProfServReceipt.getType().getName())
                    .addDefaultValue(CISales.IncomingProfServReceipt.getType().getName() + ".InstanceLink01",
                                    CISales.IncomingProfServRetention2IncomingProfServReceipt.ToLink.name)
                    .addDefaultValue(CISales.IncomingProfServReceipt.getType().getName() + ".DependentLink01",
                                    CISales.IncomingProfServRetention2IncomingProfServReceipt.FromLink.name)
                    .addDefaultValue(CISales.IncomingProfServReceipt.getType().getName() + ".DependentStatusGroup01",
                                    CISales.IncomingProfServRetentionStatus.getType().getName())
                    .addDefaultValue(CISales.IncomingProfServReceipt.getType().getName() + ".DependentStatus01",
                                    CISales.IncomingProfServRetentionStatus.Canceled.key)
                    .description("Ruled for the DependetListeners");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute DELIVERYNOTE_FROMINVOICEAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "DeliveryNote.CreateFromInvoiceAutoComplete")
                    .description("AutoComplete and Activation for Create from Invoice for DeliveryNote.")
                    .addDefaultValue("Type", "Sales_Invoice")
                    .addDefaultValue("StatusGroup01", "Sales_InvoiceStatus")
                    .addDefaultValue("Status01", "Open");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute DELIVERYNOTE_FROMRECEIPTAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "DeliveryNote.CreateFromReceiptAutoComplete")
                    .description("AutoComplete and Activation for Create from Receipt for DeliveryNote.")
                    .addDefaultValue("Type", "Sales_Receipt")
                    .addDefaultValue("StatusGroup01", "Sales_ReceiptStatus")
                    .addDefaultValue("Status01", "Open");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute DELIVERYNOTE_FROMORINAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "DeliveryNote.CreateFromOrderInboundAutoComplete")
                    .description("AutoComplete and Activation for Create from OrderInbound for DeliveryNote.")
                    .addDefaultValue("Type", CISales.OrderInbound.getType().getName())
                    .addDefaultValue("StatusGroup01", CISales.OrderInboundStatus.getType().getName())
                    .addDefaultValue("Status01", CISales.OrderInboundStatus.Open.key);

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute DELIVERYNOTE_JASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "DeliveryNote.JasperReport")
                    .description("Name of the jasperReport for DeliveryNote.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> DELIVERYNOTE_MIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "DeliveryNote.Mime")
                    .clazz(JasperMime.class)
                    .description("Name of the jasperReport for DeliveryNote.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute DELIVERYNOTE_CONTMSGPH4ARP = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "DeliveryNote.ContactMsgPhrase4ArrivalPoint")
                    .defaultValue(CIMsgContacts.ContactAddressMsgPhrase.uuid.toString())
                    .description("MsgPhrase for a contact applied to ArrivalPoint .");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute DELIVERYNOTE_SUBCONTMSGPH4ARP = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "DeliveryNote.SubContactMsgPhrase4ArrivalPoint")
                    .defaultValue(CIMsgContacts.SubContactAddressMsgPhrase.uuid.toString())
                    .description("MsgPhrase for a sub contact applied to ArrivalPoint .");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute DELIVERYNOTE_ACTIVATEREMARK = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "DeliveryNote.ActivateRemark")
                    .description("Activate the Remark Attribute for Order Inbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final ListSysConfAttribute DELIVERYNOTE_DEFAULTARRIVALPOINTS = new ListSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "DeliveryNote.DefaultArrivalPoints")
                    .description("Default addresses used as the departure point in delivery note.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final ListSysConfAttribute DELIVERYNOTE_DEFAULTDEPARTUREPOINTS = new ListSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "DeliveryNote.DefaultDeparturePoints")
                    .description("Default addresses used as the departure point in delivery note.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink DELIVERYNOTE_DEFAULTWAREHOUSE = new SysConfLink()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "DeliveryNote.DefaultWareHouse")
                    .description("Possibility to set a default Warehouse fo0r DeliveryNote. If not set "
                                    + "the default from Products applies.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute DOCSITUATIONREPORT = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.DocSituationReport")
                    .description("Properties 4 DocSituationReport");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute DOCPOSCOSTREPORT = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.DocPositionCostReport")
                    .addDefaultValue("Type01", "Sale_Invoice")
                    .addDefaultValue("Sales_Invoice.Total", "NET")
                    .addDefaultValue("StatusGroup01", "Sales_InvoiceStatus")
                    .addDefaultValue("Status01", "!Replaced ")
                    .description("Properties to define a paid threshold for types. e.g. Sale_Invoice=0.05");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute DOCPOSCOSTREPORTALTERNATIVE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.DocPositionCostReport.ActivateAlternative")
                    .description("Activate the Cost Alternative Mechanism.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute DOCVSDOCREPORT = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.DocVsDocReport")
                    .description("Properties to configure the DocVsDocReport."
                                    + "VersusNN=KEY\n"
                                    + "KEY.Label= Boleta vs. Guia de Remision\n"
                                    + "KEY.Criteria=DateAfter|Relation\n"
                                    + "KEY.left.TypeNN=Sales_Receipt\n"
                                    + "KEY.left.StatusGroupNN=Sales_ReceiptStatus\n"
                                    + "KEY.left.StatusNN=Draft\n"
                                    + "KEY.right.TypeNN=Sales_DeliveryNote\n"
                                    + "KEY.right.StatusGroupNN=Sales_DeliveryNoteStatus\n"
                                    + "KEY.right.StatusNN=Open\n"
                                    + "KEY.relation.TypeNN=Sales_Receipt2DeliveryNote\n"
                                    + "KEY.relation.LeftAttributeNN=FromLink\n"
                                    + "KEY.relation.RightAttributeNN=ToLink");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute DOCSUMREPORT = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.DocumentSumReport")
                    .concatenate(true)
                    .description("Properties to configure the DocumentSumReport.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute EXCHANGEACTIVATEGUARANTEE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Exchange.ActivateGuarantee")
                    .description("Allows to activate/deactivate the realtion to a Guarantee.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute EXCHANGEUSENUMGEN = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Exchange.UseNumberGenerator")
                    .description("Use a NumberGenerator for the Name.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute EXCHANGE_REVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Exchange.RevisionSequence")
                    .defaultValue(CINumGenSales.ExchangeRevisionSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute FUNDSTOBESETTLED_ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "FundsToBeSettled.Activate")
                    .description("Activate FundsToBeSettled mechanism");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute FUNDSTOBESETTLEDRECEIPT_REVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "FundsToBeSettledReceipt.RevisionSequence")
                    .defaultValue(CINumGenSales.IncomingInvoiceRevisionSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute FUNDSTOBESETTLEDRECEIPT_ASSIGNEMPLOYEE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "FundsToBeSettledReceipt.AssignEmployee")
                    .description("Activate the mechanism to assign an employee to FundsToBeSettledReceipt.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute FUNDSTOBESETTLEDRECEIPT_PRODUCTAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "FundsToBeSettledReceipt.ProductAutoComplete")
                    .description("Possiblity to overwrite the standart properties for Product AutoComplete.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute GOODSISSUESLIPACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "GoodsIssueSlip.Activate")
                    .description("Activate GoodsIssueSlip mechnism");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute GOODSISSUESLIPASSCONTACT = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "GoodsIssueSlip.AssignContact")
                    .description("Activate GoodsIssueSlip mechnism");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute GOODSISSUESLIPASSIGNEMPLOYEE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "GoodsIssueSlip.AssignEmployee")
                    .description("Activate GoodsIssueSlip mechnism");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink GOODSISSUESLIPDEFAULTWAREHOUSE = new SysConfLink()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "GoodsIssueSlip.DefaultWareHouse")
                    .description("Possibility to set a default Warehouse for GoodsIssueSlip. If not set "
                                    + "the default from Products applies.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute  GOODSISSUESLIPJASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "GoodsIssueSlip.JasperReport")
                    .description("Name of the jasperReport for GoodsIssueSlip.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime>  GOODSISSUESLIPMIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "GoodsIssueSlip.Mime")
                    .clazz(JasperMime.class)
                    .description("Name of the jasperReport for GoodsIssueSlip.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute INCOMINGCHECK_ACTIVATE = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingCheck.Activate")
                    .defaultValue(CINumGenSales.IncomingCheckRevisionSequence.uuid.toString())
                    .description("Activate Incoming Check mechanism");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute INCOMINGCHECK_REVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingCheck.RevisionSequence")
                    .defaultValue(CINumGenSales.IncomingCheckRevisionSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute INCOMINGCHECK_STATUS4CREATE = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingCheck.Status4Create")
                    .defaultValue("Open")
                    .description("Key of the Status for create.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute INCOMINGCREDITNOTE_REVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingCreditNote.RevisionSequence")
                    .defaultValue(CINumGenSales.IncomingInvoiceRevisionSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INCOMINGCREDITNOTE_FROMINCINVOICEAC
            = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingCreditNote.CreateFromIncomingInvoiceAutoComplete")
                    .description("Possibiloity to overwrite the configuraiton for the Automcomplete for Invoice.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute  INCOMINGCREDITNOTE_TRANSDOC = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingCreditNote.ActivateTransDocShadow")
                    .description("Activate the possiblity to create a TransactionDocumentShadow");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink INCOMINGCREDITNOTE_DEFAULTWAREHOUSE = new SysConfLink()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingCreditNote.DefaultWareHouse")
                    .description("Possibility to set a default Warehouse for IncomingCreditNote-TransDocShadow. "
                                    + "If not set the default from Products applies.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink INCOMINGCREDITNOTE_DEFAULTPRODDOCTYPE = new SysConfLink()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingCreditNote.DefaultProductDocumentType")
                    .description("Possibility to set a default Product Document Type for "
                                    + "IncomingCreditNote-TransDocShadow. ");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INCOMINGEXCHANGE_JASPERREPORT
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingExchange.JasperReport")
                    .description("Name of the jasperReport for IncomingExchange.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> INCOMINGEXCHANGE_MIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingExchange.Mime")
                    .clazz(JasperMime.class)
                    .description("Name of the jasperReport for IncomingExchange");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGEXCHANGE_ACTIVATEGUARANTEE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingExchange.ActivateGuarantee")
                    .description("Allows to activate/deactivate the realtion to a Guarantee.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGEXCHANGE_USENUMGEN = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingExchange.UseNumberGenerator")
                    .description("Use a NumberGenerator for the Name.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute INCOMINGEXCHANGE_REVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingExchange.RevisionSequence")
                    .defaultValue(CINumGenSales.IncomingExchangeRevisionSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGEXCHANGE_FILE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingExchange.ActivateFiles")
                    .description("Activate the possiblity to upload Files");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGEXCHANGE_FILESTRBROWSER = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingExchange.ActivateFilesStructurBrowser")
                    .description("Activate the possiblity to upload Files incl. FolderStructure");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGINVOICE_FROMORDEROUTBOUND = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingInvoice.CreateFromOrderOutbound")
                    .description("Allows to activate/deactivate the mechanisms to relate Incoming Invoice and "
                                    + "Order Outbound.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INCOMINGINVOICE_FROMORDEROUTBOUNDAC
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingInvoice.CreateFromOrderOutboundAutoComplete")
                    .addDefaultValue("Type", CISales.OrderOutbound.uuid.toString())
                    .addDefaultValue("StatusGroup", CISales.OrderOutbound.getType().getName())
                    .addDefaultValue("Status", CISales.OrderOutboundStatus.Open.key)
                    .description("Config for a QueryBuilder for Autocomplete and Query of OrderOutbound to create "
                                    + "Incoming Invoice from.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGINVOICE_FROMSERVORDEROUTBOUND = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingInvoice.CreateFromServiceOrderOutbound")
                    .description("Allows to activate/deactivate the mechanisms to relate Incoming Invoice and "
                                    + "Order Outbound.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INCOMINGINVOICE_FROMSERVORDEROUTBOUNDAC
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingInvoice.CreateFromServiceOrderOutboundAutoComplete")
                    .description("Config for a QueryBuilder for Autocomplete and Query of OrderOutbound "
                                    + "to create Incoming Invoice from.")
                    .addDefaultValue("Type", CISales.ServiceOrderOutbound.uuid.toString())
                    .addDefaultValue("StatusGroup", CISales.ServiceOrderOutbound.getType().getName())
                    .addDefaultValue("Status", CISales.ServiceOrderOutboundStatus.Open.key);

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGINVOICE_FROMRECIEVINGTICKET = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingInvoice.CreateFromRecievingTicket")
                    .description("Allows to activate/deactivate the mechanisms to relate Incoming Invoice "
                                    + "and Recieving Ticket.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INCOMINGINVOICE_FROMRECIEVINGTICKETAC
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingInvoice.CreateFromRecievingTicketAutoComplete")
                    .description("Config for a QueryBuilder for Autocomplete and Query of RecievingTicket to create "
                                    + "Incoming Invoice from.")
                    .addDefaultValue("Type", CISales.RecievingTicket.uuid.toString())
                    .addDefaultValue("StatusGroup", CISales.RecievingTicketStatus.getType().getName())
                    .addDefaultValue("Status", CISales.RecievingTicketStatus.Open.key);

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGINVOICE_FROMPRODUCTREQUEST = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingInvoice.CreateFromProductRequest")
                    .description("Allows to activate/deactivate the mechanisms to relate Incoming Invoice "
                                    + "and Product Request.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INCOMINGINVOICE_FROMPRODUCTREQUESTAC
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingInvoice.CreateFromProductRequestAutoComplete")
                    .description("Config for a QueryBuilder for Autocomplete and Query of Product Request to create "
                                    + "Incoming Invoice from.")
                    .addDefaultValue("Type", CISales.ProductRequest.uuid.toString())
                    .addDefaultValue("StatusGroup", CISales.ProductRequestStatus.getType().getName())
                    .addDefaultValue("Status", CISales.ProductRequestStatus.Open.key);

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGINVOICE_FROMSERVICEREQUEST = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingInvoice.CreateFromServiceRequest")
                    .description("Allows to activate/deactivate the mechanisms to relate Incoming Invoice "
                                    + "and Service Request.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INCOMINGINVOICE_FROMSERVICEREQUESTAC
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingInvoice.CreateFromServiceRequestAutoComplete")
                    .description("Config for a QueryBuilder for Autocomplete and Query of Product Request to create "
                                    + "Incoming Invoice from.")
                    .addDefaultValue("Type", CISales.ServiceRequest.uuid.toString())
                    .addDefaultValue("StatusGroup", CISales.ServiceRequestStatus.getType().getName())
                    .addDefaultValue("Status", CISales.ServiceRequestStatus.Open.key);

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGINVOICE_ACTIVATECONDITION = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingInvoice.ActivateCondition")
                    .description("Activate the mechanism to assign a condition in Order Outbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGINVOICE_ACTIVATEREGPURPRICE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingInvoice.ActivateRegisterOfPuchasePrice")
                    .description("Allows to activate/deactivate the registering of the prices during a purchase.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGINVOICE_ASSDEP = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingInvoice.AssignDepartment")
                    .description("Activate the mechanism to assign a department to IncomingInvoice.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGINVOICE_ACTPERC = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingInvoice.ActivatePerception")
                    .description("Activate the calculation of Perception.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGINVOICE_ACTRET = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingInvoice.ActivateRetention")
                    .description("Activate the calculation of Retention.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute INCOMINGINVOICE_REVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingInvoice.RevisionSequence")
                    .defaultValue(CINumGenSales.IncomingInvoiceRevisionSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute  INCOMINGINVOICE_TRANSDOC = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingInvoice.ActivateTransDocShadow")
                    .description("Activate the possiblity to create a TransactionDocumentShadow");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute  INCOMINGINVOICE_FILE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingInvoice.ActivateFiles")
                    .description("Activate the possiblity to upload Files");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute  INCOMINGINVOICE_FILESTRBROWSER = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingInvoice.ActivateFilesStructurBrowser")
                    .description("Activate the possiblity to upload Files incl. FolderStructure");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink INCOMINGINVOICE_DEFAULTWAREHOUSE = new SysConfLink()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingInvoice.DefaultWareHouse")
                    .description("Possibility to set a default Warehouse for IncomingCreditNote-TransDocShadow. "
                                    + "If not set the default from Products applies.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INCOMINGINVOICE_PRODUCTAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingInvoice.ProductAutoComplete")
                    .description("Possiblity to overwrite the standart properties for Product AutoComplete.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INCOMINGINVOICE_VALIDATION = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingInvoice.Validation")
                    .addDefaultValue("Validation01", "NAME")
                    .addDefaultValue("Validation02", "QUANTITYGREATERZERO")
                    .addDefaultValue("Validation03", "AMOUNTGREATERZERO")
                    .addDefaultValue("NAME.ValidateContact", "true")
                    .addDefaultValue("NAME.QueryType01", "Sales_IncomingInvoice")
                    .addDefaultValue("NAME.QueryType02", "Sales_IncomingReceipt")
                    .addDefaultValue("NAME.QueryType03", "Accounting_ExternalVoucher")
                    .description("Configuration Properties to activate validations.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGPROFSERVREC_ACTRET = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingProfServReceipt.ActivateRetention")
                    .description("Activate the calculation of Retention.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGPROFSERVREC_ACTIVATECONDITION = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingProfServReceipt.ActivateCondition")
                    .description("Activate the mechanism to assign a condition to IncomingProfServReceipt");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute INCOMINGPROFSERVREC_REVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingProfServReceipt.RevisionSequence")
                    .defaultValue(CINumGenSales.IncomingInvoiceRevisionSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute  INCOMINGPROFSERVREC_FILE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingProfServReceipt.ActivateFiles")
                    .description("Activate the possiblity to upload Files");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGPROFSERVREC_FILESTRBROWSER = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingProfServReceipt.ActivateFilesStructurBrowser")
                    .description("Activate the possiblity to upload Files incl. FolderStructure");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGPERCEPTIONCERTIFICATEACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingPerceptionCertificate.Activate")
                    .description("Activate Perception Certificates");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute INCOMINGQUOTATION_ACTIVATE = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingQuotation.Activate")
                    .description("Activate the possiblity to register Incoming Quotations");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute INCOMINGQUOTATION_REVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingQuotation.RevisionSequence")
                    .defaultValue(CINumGenSales.IncomingQuotationRevisionSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGQUOTATION_FILE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingQuotation.ActivateFiles")
                    .description("Activate the possiblity to upload Files");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGQUOTATION_FILESTRBROWSER = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingQuotation.ActivateFilesStructurBrowser")
                    .description("Activate the possiblity to upload Files incl. FolderStructure");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute INCOMINGRECEIPTREVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingReceipt.RevisionSequence")
                    .defaultValue(CINumGenSales.IncomingInvoiceRevisionSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGRECEIPTFROMORDEROUTBOUND = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingReceipt.CreateFromOrderOutbound")
                    .description("Allows to activate/deactivate the mechanisms to relate Incoming Receipt and "
                                    + "Order Outbound.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INCOMINGRECEIPTFROMORDEROUTBOUNDAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingReceipt.CreateFromOrderOutboundAutoComplete")
                    .description("Config for a QueryBuilder for Autocomplete and Query of OrderOutbound to create "
                                    + "Incoming Receipt from.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGRECEIPTFROMSERVORDEROUTBOUND = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingReceipt.CreateFromServiceOrderOutbound")
                    .description("Allows to activate/deactivate the mechanisms to relate Incoming Receipt and "
                                    + "Order Outbound.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INCOMINGRECEIPTFROMSERVORDEROUTBOUNDAC
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingReceipt.CreateFromServiceOrderOutboundAutoComplete")
                    .description("Config for a QueryBuilder for Autocomplete and Query of OrderOutbound "
                                    + "to create Incoming Receipt from.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute  INCOMINGRECEIPT_FILE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingReceipt.ActivateFiles")
                    .description("Activate the possiblity to upload Files");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGRECEIPT_FILESTRBROWSER = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingReceipt.ActivateFilesStructurBrowser")
                    .description("Activate the possiblity to upload Files incl. FolderStructure");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INCOMINGRECEIPT_VALIDATION = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingReceipt.Validation")
                    .addDefaultValue("Validation01", "NAME")
                    .addDefaultValue("Validation02", "QUANTITYGREATERZERO")
                    .addDefaultValue("Validation03", "AMOUNTGREATERZERO")
                    .addDefaultValue("NAME.ValidateContact", "true")
                    .addDefaultValue("NAME.QueryType01", "Sales_IncomingInvoice")
                    .addDefaultValue("NAME.QueryType02", "Sales_IncomingReceipt")
                    .addDefaultValue("NAME.QueryType03", "Accounting_ExternalVoucher")
                    .description("Configuration Properties to activate validations.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute INCOMINGREMINDER_REVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingReminder.RevisionSequence")
                    .defaultValue(CINumGenSales.IncomingInvoiceRevisionSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INCOMINGREMINDER_FROMINCINVOICEAC
            = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingReminder.CreateFromIncomingInvoiceAutoComplete")
                    .description("Possibiloity to overwrite the configuraiton for the Automcomplete "
                                    + "for Incoming Invoice.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGREMINDER_ACTIVATECONDITION = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingReminder.ActivateCondition")
                    .description("Activate the mechanism to assign a condition in Incoming Reminder");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGRETENTIONCERTIFICATE_ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingRetentionCertificate.Activate")
                    .description("Activate Incoming Retention Certificates");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink INCOMINGRETENTIONCERTIFICATE_DEFAULTCURRENCY = new SysConfLink()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingRetentionCertificate.DefaultCurrency")
                    .description("Possibility to set a default Currenty for IncomingRetentionCertificate. "
                                    + "If not set the default from Products applies.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute INCOMINGRETENTIONCERTIFICATE_REVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingRetentionCertificate.RevisionSequence")
                    .defaultValue(CINumGenSales.IncomingRetentionCertificateRevisionSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGRETENTIONACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingRetention.Activate")
                    .description("Activate Incoming Retention Certificates");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INCOMINGPROFSERVRETENTION_ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "IncomingProfServRetention.Activate")
                    .defaultValue(true)
                    .description("Activate Incoming IncomingProfServRetention");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INVOICE_ACTIVATECONDITION = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Invoice.ActivateCondition")
                    .description("Activate the mechanism to assign a condition in Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INVOICE_ACTIVATEWIZARD = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Invoice.ActivateWizard")
                    .description("Activate the Wizard for Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute INVOICE_JASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Invoice.JasperReport")
                    .description("Name of the jasperReport for Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> INVOICE_MIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Invoice.Mime")
                    .clazz(JasperMime.class)
                    .description("Name of the jasperReport for Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BitEnumSysConfAttribute<JasperActivation> INVOICE_JASPERACTIVATION
        = new BitEnumSysConfAttribute<JasperActivation>()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Invoice.JasperActivation")
                    .clazz(JasperActivation.class)
                    .addDefaultValue(JasperActivation.ONCREATE)
                    .addDefaultValue(JasperActivation.ONEDIT)
                    .description("Activation of Jasperreport Creation");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INVOICE_ASSIGNACTION = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Invoice.AssignAction")
                    .description("Activate the mechanism to assign a action in Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INVOICE_FROMDELIVERYNOTEAC
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Invoice.CreateFromDeliveryNoteAutoComplete")
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
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Invoice.CreateFromQuotationAutoComplete")
                    .addDefaultValue("Type", CISales.Quotation.uuid.toString())
                    .description("Config for a QueryBuilder for Autocomplete and Query of quotation to create "
                                    + " Invoice from.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INVOICE_FROMORDERINBOUNDAC
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Invoice.CreateFromOrderInboundAutoComplete")
                    .addDefaultValue("Type", CISales.OrderInbound.uuid.toString())
                    .addDefaultValue("StatusGroup", CISales.OrderInboundStatus.getType().getName())
                    .addDefaultValue("Status", "Open")
                    .description("Config for a QueryBuilder for Autocomplete and Query of OrderInbound to create "
                                    + " Invoice from.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INVOICE_FROMINVOICEAC
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Invoice.CreateFromInvoiceAutoComplete")
                    .addDefaultValue("Type", CISales.Invoice.uuid.toString())
                    .description("Config for a QueryBuilder for Autocomplete and Query of Invoice to create "
                                    + " Invoice from.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INVOICEASSIGNEMPLOYEE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Invoice.AssignEmployee")
                    .description("Activate the mechanism to assign employee to Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INVOICE_PRODUCTAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Invoice.ProductAutoComplete")
                    .description("Possiblity to overwrite the standart properties for Product AutoComplete.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INVOICE_VALIDATION = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Invoice.Validation")
                    .addDefaultValue("Validation01", "QUANTITYGREATERZERO")
                    .addDefaultValue("Validation02", "AMOUNTGREATERZERO")
                    .addDefaultValue("Validation03", "DUPLICATEDPOSITION")
                    .addDefaultValue("Validation04", "SERIAL")
                    .description("Configuration Properties to activate validations.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INVOICE_FILE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Invoice.ActivateFiles")
                    .description("Activate the possiblity to upload Files");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute  INVOICE_FILESTRBROWSER = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Invoice.ActivateFilesStructurBrowser")
                    .description("Activate the possiblity to upload Files incl. FolderStructure");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INSTALLMENT_USENUMGEN = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Installment.UseNumberGenerator")
                    .description("Use a NumberGenerator for the Name.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute ORDEROUTBOUND_CREATEFROMPRODREQAC
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "OrderOutbound.CreateFromProductRequestAutoComplete")
                    .addDefaultValue("Type", CISales.ProductRequest.uuid.toString())
                    .addDefaultValue("StatusGroup", CISales.ProductRequestStatus.getType().getName())
                    .addDefaultValue("Status", CISales.ProductRequestStatus.Open.key)
                    .description("Activation and QueryBuilder for Autocomplete a ProductRequest to create OrderOutbound from.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute ORDEROUTBOUND_CREATEFROMQUOTEREQAC
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "OrderOutbound.CreateFromQuoteRequestAutoComplete")
                    .addDefaultValue("Type", CISales.QuoteRequest.uuid.toString())
                    .addDefaultValue("StatusGroup", CISales.QuoteRequestStatus.getType().getName())
                    .addDefaultValue("Status", CISales.QuoteRequestStatus.Open.key)
                    .description("Activation and QueryBuilder for Autocomplete a QuoteRequest to create OrderOutbound from.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ORDEROUTBOUND_ACTIVATECONDITION = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "OrderOutbound.ActivateCondition")
                    .description("Activate the mechanism to assign a condition in Order Outbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ORDEROUTBOUND_ACTIVATECOMMENT = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "OrderOutbound.ActivateComment")
                    .description("Activate the mechanism to write Comments for Order Outbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ORDEROUTBOUND_ACTIVATEDELIVERYDATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "OrderOutbound.ActivateDeliveryDate")
                    .description("Activate DeliveryDate for Order Outbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ORDEROUTBOUND_ASSIGNEMPLOYEE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "OrderOutbound.AssignEmployee")
                    .description("Activate the mechanism to assign employee to an Order Outbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute ORDEROUTBOUND_JASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "OrderOutbound.JasperReport")
                    .description("Name of the jasperReport for Order Outbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute ORDEROUTBOUND_PRODUCTAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "OrderOutbound.ProductAutoComplete")
                    .description("Configuration for the AutoComplete for Products in OrderOutbound")
                    .addDefaultValue("Type", "Products_ProductAbstract");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ORDERINBOUND_ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "OrderInbound.Activate")
                    .description("Deactivate Order Inbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ORDERINBOUND_ASSIGNEMPLOYEE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "OrderInbound.AssignEmployee")
                    .description("Activate the mechanism to assign employee to an Order Inbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ORDERINBOUND_ACTIVATECONDITION = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "OrderInbound.ActivateCondition")
                    .description("Activate the mechanism to assign a condition in Order Inbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ORDERINBOUND_ACTIVATEREMARK = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "OrderInbound.ActivateRemark")
                    .description("Activate the Remark Attribute for Order Inbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute ORDERINBOUND_PRODUCTAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "OrderInbound.ProductAutoComplete")
                    .description("Possiblity to overwrite the standart properties for Product AutoComplete.")
                    .addDefaultValue("Type", CIProducts.ProductStandart.getType().getName());

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute ORDERINBOUND_STATUS4CREATE = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "OrderInbound.Status4Create")
                    .description("Key of the Status for create.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute ORDERINBOUND_REVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "OrderInbound.RevisionSequence")
                    .defaultValue(CINumGenSales.OrderInboundRevisionSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute ORDERINBOUND_FROMQUOTATIONAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "OrderInbound.CreateFromQuotationAutoComplete")
                    .description("AutoComplete and Activation for Create from Quotation for OrderInbound.")
                    .addDefaultValue("Type", CISales.Quotation.getType().getName())
                    .addDefaultValue("StatusGroup01", CISales.QuotationStatus.getType().getName())
                    .addDefaultValue("Status01", CISales.QuotationStatus.Open.key);

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ORDERINBOUND_FILE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "OrderInbound.ActivateFiles")
                    .description("Activate the possiblity to upload Files");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ORDERINBOUND_FILESTRBROWSER = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "OrderInbound.ActivateFilesStructurBrowser")
                    .description("Activate the possiblity to upload Files incl. FolderStructure");

    /** See description. */
    @EFapsSysConfAttribute
    public static final ListSysConfAttribute PAYMENT_DOCS4OBLIGATION = new ListSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.Documents4Obligations")
                    .description("List of Types (Documents) that create the obligation to make Outgoing payments")
                    .addDefaultValue(CISales.IncomingInvoice.getType().getName())
                    .addDefaultValue(CISales.IncomingReminder.getType().getName())
                    .addDefaultValue(CISales.IncomingProfServReceipt.getType().getName())
                    .addDefaultValue(CISales.IncomingReceipt.getType().getName())
                    .addDefaultValue(CISales.IncomingInstallment.getType().getName())
                    .addDefaultValue(CISales.Exchange.getType().getName())
                    .addDefaultValue(CISales.PaymentOrder.getType().getName())
                    .addDefaultValue(CISales.IncomingDetraction.getType().getName())
                    .addDefaultValue(CISales.IncomingPerceptionCertificate.getType().getName());

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENT_RULES = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.Rules")
                    .concatenate(true)
                    .description("Config for Paid/Unpaid evaluation.\n"
                                    + "TYPE.Paid.TargetStatus=STATUS\n"
                                    + "TYPE.Paid.Origin.StatusNN=STATUS\n"
                                    + "TYPE.Unpaid.TargetStatus=STATUS\n"
                                    + "TYPE.Unpaid.Origin.StatusNN=STATUS.\n"
                                    + "Evaluation of paid with currency is per payment or with totals"
                                    + "TYPE.PerPayment=true/false\n"
                                    + "Permited Status for Swap Paid evaluation."
                                    + "TYPE.Swap.Status4ToNN=STATUS\n"
                                    + "TYPE.Swap.Status4FromNN=STATUS\n"
                                    + "To activate the listener to evaluate on status change:"
                                    + "OnStatusChange.StatusGroup=Sales_IncomingExchangeStatus\n"
                                    + "OnStatusChange.Status=Open\n"
                                    + "Threshold for paid evaluation."
                                    + "TYPE.Threshold==0.05")
                    .addDefaultValue(CISales.CollectionOrder.getType().getName() + ".Paid.TargetStatus", CISales.CollectionOrderStatus.Closed.key)
                    .addDefaultValue(CISales.CollectionOrder.getType().getName() + ".Paid.Origin.Status01", CISales.CollectionOrderStatus.Approved.key)
                    .addDefaultValue(CISales.CollectionOrder.getType().getName() + ".Paid.Origin.Status02", CISales.CollectionOrderStatus.Open.key)
                    .addDefaultValue(CISales.CollectionOrder.getType().getName() + ".Unpaid.TargetStatus", CISales.CollectionOrderStatus.Approved.key)
                    .addDefaultValue(CISales.CollectionOrder.getType().getName() + ".Unpaid.Origin.Status01", CISales.CollectionOrderStatus.Closed.key)
                    .addDefaultValue(CISales.CreditNote.getType().getName() + ".Paid.TargetStatus", CISales.CreditNoteStatus.Paid.key)
                    .addDefaultValue(CISales.CreditNote.getType().getName() + ".Paid.Origin.Status01", CISales.CreditNoteStatus.Draft.key)
                    .addDefaultValue(CISales.CreditNote.getType().getName() + ".Paid.Origin.Status02", CISales.CreditNoteStatus.Open.key)
                    .addDefaultValue(CISales.CreditNote.getType().getName() + ".Unpaid.TargetStatus", CISales.CreditNoteStatus.Open.key)
                    .addDefaultValue(CISales.CreditNote.getType().getName() + ".Unpaid.Origin.Status01", CISales.CreditNoteStatus.Paid.key)
                    .addDefaultValue(CISales.Exchange.getType().getName() + ".Paid.TargetStatus", CISales.ExchangeStatus.Closed.key)
                    .addDefaultValue(CISales.Exchange.getType().getName() + ".Paid.Origin.Status01", CISales.ExchangeStatus.Draft.key)
                    .addDefaultValue(CISales.Exchange.getType().getName() + ".Paid.Origin.Status02", CISales.ExchangeStatus.Open.key)
                    .addDefaultValue(CISales.Exchange.getType().getName() + ".Unpaid.TargetStatus", CISales.ExchangeStatus.Open.key)
                    .addDefaultValue(CISales.Exchange.getType().getName() + ".Unpaid.Origin.Status01", CISales.ExchangeStatus.Closed.key)
                    .addDefaultValue(CISales.IncomingCreditNote.getType().getName() + ".Paid.TargetStatus", CISales.IncomingCreditNoteStatus.Paid.key)
                    .addDefaultValue(CISales.IncomingCreditNote.getType().getName() + ".Paid.Origin.Status01", CISales.IncomingCreditNoteStatus.Digitized.key)
                    .addDefaultValue(CISales.IncomingCreditNote.getType().getName() + ".Paid.Origin.Status02", CISales.IncomingCreditNoteStatus.Open.key)
                    .addDefaultValue(CISales.IncomingCreditNote.getType().getName() + ".Unpaid.TargetStatus", CISales.IncomingCreditNoteStatus.Open.key)
                    .addDefaultValue(CISales.IncomingCreditNote.getType().getName() + ".Unpaid.Origin.Status01", CISales.IncomingCreditNoteStatus.Paid.key)
                    .addDefaultValue(CISales.IncomingExchange.getType().getName() + ".Paid.TargetStatus", CISales.IncomingExchangeStatus.Closed.key)
                    .addDefaultValue(CISales.IncomingExchange.getType().getName() + ".Paid.Origin.Status01", CISales.IncomingExchangeStatus.Digitized.key)
                    .addDefaultValue(CISales.IncomingExchange.getType().getName() + ".Paid.Origin.Status02", CISales.IncomingExchangeStatus.Open.key)
                    .addDefaultValue(CISales.IncomingExchange.getType().getName() + ".Unpaid.TargetStatus", CISales.IncomingExchangeStatus.Open.key)
                    .addDefaultValue(CISales.IncomingExchange.getType().getName() + ".Unpaid.Origin.Status01", CISales.IncomingExchangeStatus.Closed.key)
                    .addDefaultValue(CISales.IncomingInstallment.getType().getName() + ".Paid.TargetStatus", CISales.IncomingInstallmentStatus.Paid.key)
                    .addDefaultValue(CISales.IncomingInstallment.getType().getName() + ".Paid.Origin.Status01", CISales.IncomingInstallmentStatus.Digitized.key)
                    .addDefaultValue(CISales.IncomingInstallment.getType().getName() + ".Paid.Origin.Status02", CISales.IncomingInstallmentStatus.Open.key)
                    .addDefaultValue(CISales.IncomingInstallment.getType().getName() + ".Unpaid.TargetStatus", CISales.IncomingInstallmentStatus.Open.key)
                    .addDefaultValue(CISales.IncomingInstallment.getType().getName() + ".Unpaid.Origin.Status01", CISales.IncomingInstallmentStatus.Paid.key)
                    .addDefaultValue(CISales.IncomingInvoice.getType().getName() + ".Paid.TargetStatus", CISales.IncomingInvoiceStatus.Paid.key)
                    .addDefaultValue(CISales.IncomingInvoice.getType().getName() + ".Paid.Origin.Status01", CISales.IncomingInvoiceStatus.Digitized.key)
                    .addDefaultValue(CISales.IncomingInvoice.getType().getName() + ".Paid.Origin.Status02", CISales.IncomingInvoiceStatus.Open.key)
                    .addDefaultValue(CISales.IncomingInvoice.getType().getName() + ".Unpaid.TargetStatus", CISales.IncomingInvoiceStatus.Open.key)
                    .addDefaultValue(CISales.IncomingInvoice.getType().getName() + ".Unpaid.Origin.Status01", CISales.IncomingInvoiceStatus.Paid.key)
                    .addDefaultValue(CISales.IncomingPerceptionCertificate.getType().getName() + ".Paid.TargetStatus", CISales.IncomingPerceptionCertificateStatus.Paid.key)
                    .addDefaultValue(CISales.IncomingPerceptionCertificate.getType().getName() + ".Paid.Origin.Status", CISales.IncomingPerceptionCertificateStatus.Open.key)
                    .addDefaultValue(CISales.IncomingPerceptionCertificate.getType().getName() + ".Unpaid.TargetStatus", CISales.IncomingPerceptionCertificateStatus.Open.key)
                    .addDefaultValue(CISales.IncomingPerceptionCertificate.getType().getName() + ".Unpaid.Origin.Status", CISales.IncomingPerceptionCertificateStatus.Paid.key)
                    .addDefaultValue(CISales.IncomingProfServReceipt.getType().getName() + ".Paid.TargetStatus", CISales.IncomingProfServReceiptStatus.Paid.key)
                    .addDefaultValue(CISales.IncomingProfServReceipt.getType().getName() + ".Paid.Origin.Status01", CISales.IncomingProfServReceiptStatus.Digitized.key)
                    .addDefaultValue(CISales.IncomingProfServReceipt.getType().getName() + ".Paid.Origin.Status02", CISales.IncomingProfServReceiptStatus.Open.key)
                    .addDefaultValue(CISales.IncomingProfServReceipt.getType().getName() + ".Unpaid.TargetStatus", CISales.IncomingProfServReceiptStatus.Open.key)
                    .addDefaultValue(CISales.IncomingProfServReceipt.getType().getName() + ".Unpaid.Origin.Status01", CISales.IncomingProfServReceiptStatus.Paid.key)
                    .addDefaultValue(CISales.IncomingReceipt.getType().getName() + ".Paid.TargetStatus", CISales.IncomingReceiptStatus.Paid.key)
                    .addDefaultValue(CISales.IncomingReceipt.getType().getName() + ".Paid.Origin.Status01", CISales.IncomingReceiptStatus.Digitized.key)
                    .addDefaultValue(CISales.IncomingReceipt.getType().getName() + ".Paid.Origin.Status02", CISales.IncomingReceiptStatus.Open.key)
                    .addDefaultValue(CISales.IncomingReceipt.getType().getName() + ".Unpaid.TargetStatus", CISales.IncomingReceiptStatus.Open.key)
                    .addDefaultValue(CISales.IncomingReceipt.getType().getName() + ".Unpaid.Origin.Status01", CISales.IncomingReceiptStatus.Paid.key)
                    .addDefaultValue(CISales.IncomingReminder.getType().getName() + ".Paid.TargetStatus", CISales.IncomingReminderStatus.Paid.key)
                    .addDefaultValue(CISales.IncomingReminder.getType().getName() + ".Paid.Origin.Status01", CISales.IncomingReminderStatus.Digitized.key)
                    .addDefaultValue(CISales.IncomingReminder.getType().getName() + ".Paid.Origin.Status02", CISales.IncomingReminderStatus.Open.key)
                    .addDefaultValue(CISales.IncomingReminder.getType().getName() + ".Unpaid.TargetStatus", CISales.IncomingReminderStatus.Open.key)
                    .addDefaultValue(CISales.IncomingReminder.getType().getName() + ".Unpaid.Origin.Status01", CISales.IncomingReminderStatus.Paid.key)
                    .addDefaultValue(CISales.Installment.getType().getName() + ".Paid.TargetStatus", CISales.InstallmentStatus.Paid.key)
                    .addDefaultValue(CISales.Installment.getType().getName() + ".Paid.Origin.Status01", CISales.InstallmentStatus.Draft.key)
                    .addDefaultValue(CISales.Installment.getType().getName() + ".Paid.Origin.Status02", CISales.InstallmentStatus.Open.key)
                    .addDefaultValue(CISales.Installment.getType().getName() + ".Unpaid.TargetStatus", CISales.InstallmentStatus.Open.key)
                    .addDefaultValue(CISales.Installment.getType().getName() + ".Unpaid.Origin.Status01", CISales.InstallmentStatus.Paid.key)
                    .addDefaultValue(CISales.Invoice.getType().getName() + ".Paid.TargetStatus", CISales.InvoiceStatus.Paid.key)
                    .addDefaultValue(CISales.Invoice.getType().getName() + ".Paid.Origin.Status01", CISales.InvoiceStatus.Draft.key)
                    .addDefaultValue(CISales.Invoice.getType().getName() + ".Paid.Origin.Status02", CISales.InvoiceStatus.Open.key)
                    .addDefaultValue(CISales.Invoice.getType().getName() + ".Unpaid.TargetStatus", CISales.InvoiceStatus.Open.key)
                    .addDefaultValue(CISales.Invoice.getType().getName() + ".Unpaid.Origin.Status01", CISales.InvoiceStatus.Paid.key)
                    .addDefaultValue(CISales.PaymentOrder.getType().getName() + ".Paid.TargetStatus", CISales.PaymentOrderStatus.Closed.key)
                    .addDefaultValue(CISales.PaymentOrder.getType().getName() + ".Paid.Origin.Status01", CISales.PaymentOrderStatus.Open.key)
                    .addDefaultValue(CISales.PaymentOrder.getType().getName() + ".Paid.Origin.Status02", CISales.PaymentOrderStatus.Approved.key)
                    .addDefaultValue(CISales.PaymentOrder.getType().getName() + ".Unpaid.TargetStatus", CISales.PaymentOrderStatus.Approved.key)
                    .addDefaultValue(CISales.PaymentOrder.getType().getName() + ".Unpaid.Origin.Status01", CISales.PaymentOrderStatus.Closed.key)
                    .addDefaultValue(CISales.Receipt.getType().getName() + ".Paid.TargetStatus", CISales.ReceiptStatus.Paid.key)
                    .addDefaultValue(CISales.Receipt.getType().getName() + ".Paid.Origin.Status01", CISales.ReceiptStatus.Draft.key)
                    .addDefaultValue(CISales.Receipt.getType().getName() + ".Paid.Origin.Status02", CISales.ReceiptStatus.Open.key)
                    .addDefaultValue(CISales.Receipt.getType().getName() + ".Unpaid.TargetStatus", CISales.ReceiptStatus.Open.key)
                    .addDefaultValue(CISales.Receipt.getType().getName() + ".Unpaid.Origin.Status01", CISales.ReceiptStatus.Paid.key)
                    .addDefaultValue(CISales.Reminder.getType().getName() + ".Paid.TargetStatus", CISales.ReminderStatus.Paid.key)
                    .addDefaultValue(CISales.Reminder.getType().getName() + ".Paid.Origin.Status01", CISales.ReminderStatus.Draft.key)
                    .addDefaultValue(CISales.Reminder.getType().getName() + ".Paid.Origin.Status02", CISales.ReminderStatus.Open.key)
                    .addDefaultValue(CISales.Reminder.getType().getName() + ".Unpaid.TargetStatus", CISales.ReminderStatus.Open.key)
                    .addDefaultValue(CISales.Reminder.getType().getName() + ".Unpaid.Origin.Status01", CISales.ReminderStatus.Paid.key);

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PAYMENT_THRESHOLD4CREATEDOC = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.in.Threshold4CreateOrderOnSettle")
                    .defaultValue("0")
                    .description("Threshold to generated a payment order or collection order on settlement.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYMENT_CREDITCARD_ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.in.CreditCard.Activate")
                    .description("Deactivate the automatic generation of a code for every Payment Documents.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PAYMENT_CREDITCARD_CREATESTATUS = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.in.CreditCard.Status4Create")
                    .description("Possiblity to set the create Status for PaymentDeposit.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENT_CREDITCARD_PAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.in.CreditCard.PayableDocuments")
                    .addDefaultValue("Type01", CISales.Invoice.getType().getName())
                    .addDefaultValue("Type02", CISales.Receipt.getType().getName())
                    .addDefaultValue("StatusGroup01", CISales.InvoiceStatus.getType().getName())
                    .addDefaultValue("Status01", CISales.InvoiceStatus.Open.key)
                    .addDefaultValue("StatusGroup02", CISales.ReceiptStatus.getType().getName())
                    .addDefaultValue("Status02", CISales.ReceiptStatus.Open.key)
                    .description("Properties to define the Query for Documents that can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYMENT_DEBITCARD_ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.in.DebitCard.Activate")
                    .description("Deactivate the automatic generation of a code for every Payment Documents.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PAYMENT_DEBITCARD_CREATESTATUS = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.in.DebitCard.Status4Create")
                    .description("Possiblity to set the create Status for PaymentDeposit.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENT_DEBITCARD_PAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.in.DebitCard.PayableDocuments")
                    .addDefaultValue("Type01", CISales.Invoice.getType().getName())
                    .addDefaultValue("Type02", CISales.Receipt.getType().getName())
                    .addDefaultValue("StatusGroup01", CISales.InvoiceStatus.getType().getName())
                    .addDefaultValue("Status01", CISales.InvoiceStatus.Open.key)
                    .addDefaultValue("StatusGroup02", CISales.ReceiptStatus.getType().getName())
                    .addDefaultValue("Status02", CISales.ReceiptStatus.Open.key)
                    .description("Properties to define the Query for Documents that can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYMENT_DETRACTION_ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.in.Detraction.Activate")
                    .defaultValue(true)
                    .description("Activate the possiblity to use Detraction as a payment.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PAYMENT_DETRACTION_CREATESTATUS = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.in.Detraction.Status4Create")
                    .description("Possiblity to set the create Status for PaymentDeposit.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENT_DETRACTION_PAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.in.Detraction.PayableDocuments")
                    .addDefaultValue("Type01", CISales.Invoice.getType().getName())
                    .addDefaultValue("Type02", CISales.Receipt.getType().getName())
                    .addDefaultValue("StatusGroup01", CISales.InvoiceStatus.getType().getName())
                    .addDefaultValue("Status01", CISales.InvoiceStatus.Open.key)
                    .addDefaultValue("StatusGroup02", CISales.ReceiptStatus.getType().getName())
                    .addDefaultValue("Status02", CISales.ReceiptStatus.Open.key)
                    .description("Properties to define the Query for Documents that can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYMENT_RETENTION_ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.in.Retention.Activate")
                    .defaultValue(true)
                    .description("Activate the possiblity to use Retention as a payment.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PAYMENT_RETENTION_CREATESTATUS = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.in.Retention.Status4Create")
                    .description("Possiblity to set the create Status for PaymentDeposit.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENT_RETENTION_PAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.in.Retention.PayableDocuments")
                    .description("Properties to define the Query for Documents that can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYMENT_CASH_ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.in.Cash.Activate")
                    .description("Properties to define the Query for Documents that can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENT_CASH_PAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.in.Cash.PayableDocuments")
                    .addDefaultValue("Type01", CISales.Invoice.getType().getName())
                    .addDefaultValue("Type02", CISales.Receipt.getType().getName())
                    .addDefaultValue("StatusGroup01", CISales.InvoiceStatus.getType().getName())
                    .addDefaultValue("Status01", CISales.InvoiceStatus.Open.key)
                    .addDefaultValue("StatusGroup02", CISales.ReceiptStatus.getType().getName())
                    .addDefaultValue("Status02", CISales.ReceiptStatus.Open.key)
                    .description("Properties to define the Query for Documents that can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PAYMENT_CASH_CREATESTATUS = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.in.Cash.Status4Create")
                    .description("Possiblity to set the create Status for PaymentDeposit.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYMENT_CHECK_ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.in.Check.Activate")
                    .description("Properties to define the Query for Documents that can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENT_CHECK_PAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.in.Check.PayableDocuments")
                    .addDefaultValue("Type01", CISales.Invoice.getType().getName())
                    .addDefaultValue("Type02", CISales.Receipt.getType().getName())
                    .addDefaultValue("StatusGroup01", CISales.InvoiceStatus.getType().getName())
                    .addDefaultValue("Status01", CISales.InvoiceStatus.Open.key)
                    .addDefaultValue("StatusGroup02", CISales.ReceiptStatus.getType().getName())
                    .addDefaultValue("Status02", CISales.ReceiptStatus.Open.key)
                    .description("Properties to define the Query for Documents that can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PAYMENT_CHECK_CREATESTATUS = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.in.Check.Status4Create")
                    .description("Possiblity to set the create Status for PaymentDeposit.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYMENT_DEPOSIT_ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.in.Deposit.Activate")
                    .description("Properties to define the Query for Documents that can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENT_DEPOSIT_PAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.in.Deposit.PayableDocuments")
                    .addDefaultValue("Type01", CISales.Invoice.getType().getName())
                    .addDefaultValue("Type02", CISales.Receipt.getType().getName())
                    .addDefaultValue("StatusGroup01", CISales.InvoiceStatus.getType().getName())
                    .addDefaultValue("Status01", CISales.InvoiceStatus.Open.key)
                    .addDefaultValue("StatusGroup02", CISales.ReceiptStatus.getType().getName())
                    .addDefaultValue("Status02", CISales.ReceiptStatus.Open.key)
                    .description("Properties to define the Query for Documents that can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PAYMENT_DEPOSIT_CREATESTATUS = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.in.Deposit.Status4Create")
                    .description("Possiblity to set the create Status for PaymentDeposit.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYMENT_INTERNAL_ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.in.Internal.Activate")

                    .description("Activate the Internal Payment Documents.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PAYMENT_INTERNAL_CREATESTATUS = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.in.Internal.Status4Create")
                    .description("Possiblity to set the create Status for PaymentDeposit.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENT_INTERNAL_PAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.in.Internal.PayableDocuments")
                    .addDefaultValue("Type01", CISales.IncomingInvoice.getType().getName())
                    .addDefaultValue("Type02", CISales.IncomingReceipt.getType().getName())
                    .addDefaultValue("StatusGroup01", CISales.IncomingInvoiceStatus.getType().getName())
                    .addDefaultValue("Status01", CISales.IncomingInvoiceStatus.Open.key)
                    .addDefaultValue("StatusGroup02", CISales.IncomingReceiptStatus.getType().getName())
                    .addDefaultValue("Status02", CISales.IncomingReceiptStatus.Open.key)
                    .description("Properties to define the Query for Documents that can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENTDOCUMENT_TOBESETTLED = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.in.Config4ToBeSetteled")
                    .addDefaultValue("Type", CISales.OrderOutbound.uuid.toString())
                    .addDefaultValue("StatusGroup", CISales.OrderOutboundStatus.getType().getName())
                    .addDefaultValue("Status", "!" + CISales.OrderOutboundStatus.Canceled.key)
                    .addDefaultValue("Tag.Type", CISales.AccountabilityTag4CollectionOrder.getType().getName())
                    .addDefaultValue("Electable.Type01", CISales.OrderOutbound.getType().getName())
                    .addDefaultValue("Electable.Type02", CISales.CollectionOrder.getType().getName())
                    .description("QueryBuilder config for the Documents that must be settled.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYMENTDOCUMENTDEACTIVATECODE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.in.DeactivateCodeGeneration")
                    .description("Deactivate the automatic generation of a code for every Payment Documents.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PAYMENTDOCUMENTNUMGEN = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.in.NumberGenerator")
                    .defaultValue(CINumGenSales.PaymentDocumentSequence.uuid.toString())
                    .description("NumberGenerator Payment Documents.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYMENTDOCUMENT_GENERATEREPORT = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.in.GenerateReport")
                    .description("Activate the generation of a report for every payment.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYMENTDOCUMENTOUTACTIVATECODE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.out.DeactivateCodeGeneration")
                    .description("Deactivate the automatic generation of a code for every Payment Out Documents.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYMENTDOCUMENTOUT_GENERATEREPORT = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.out.GenerateReport")
                    .description("Activate the generation of a report for every payment.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENTDOCUMENTOUT_TOBESETTLED = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.out.Config4ToBeSetteled")
                    .addDefaultValue("Type01", CISales.OrderOutbound.uuid.toString())
                    .addDefaultValue("Type02", CISales.ServiceOrderOutbound.uuid.toString())
                    .addDefaultValue("StatusGroup01", CISales.OrderOutboundStatus.getType().getName())
                    .addDefaultValue("Status01", "!" + CISales.OrderOutboundStatus.Canceled.key)
                    .addDefaultValue("StatusGroup02", CISales.ServiceOrderOutboundStatus.getType().getName())
                    .addDefaultValue("Status02", "!" + CISales.ServiceOrderOutboundStatus.Canceled.key)
                    .addDefaultValue("Tag.Type01", CISales.AccountabilityTag4PaymentOrder.getType().getName())
                    .addDefaultValue("Electable.Type01", CISales.OrderOutbound.getType().getName())
                    .addDefaultValue("Electable.Type02", CISales.PaymentOrder.getType().getName())
                    .addDefaultValue("Electable.Type03", CISales.ServiceOrderOutbound.getType().getName())
                    .description("QueryBuilder config for the Documents that must be settled.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PAYMENTDOCUMENTOUTNUMGEN = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.out.NumberGenerator")
                    .defaultValue(CINumGenSales.PaymentDocumentOutSequence.uuid.toString())
                    .description("NumberGenerator Payment Out Documents.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENTORDER_JASPERREPORT
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "PaymentOrder.JasperReport")
                    .description("Name of the jasperReport for PaymentOrder.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> PAYMENTORDER_MIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "PaymentOrder.Mime")
                    .clazz(JasperMime.class)
                    .description("Name of the jasperReport for PaymentOrder");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BitEnumSysConfAttribute<JasperActivation> PAYMENTORDER_JASPERACTIVATION
        = new BitEnumSysConfAttribute<JasperActivation>()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "PaymentOrder.JasperActivation")
                    .clazz(JasperActivation.class)
                    .addDefaultValue(JasperActivation.ONCREATE)
                    .addDefaultValue(JasperActivation.ONEDIT)
                    .description("Activation of Jasperreport Creation");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute  PAYMENTORDER_FILE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "PaymentOrder.ActivateFiles")
                    .description("Activate the possiblity to upload Files");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute  PAYMENTORDER_FILESTRBROWSER = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "PaymentOrder.ActivateFilesStructurBrowser")
                    .description("Activate the possiblity to upload Files incl. FolderStructure");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute  PAYMENTORDER_ASSIGNACTION = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "PaymentOrder.AssignAction")
                    .description("Activate the Action mechanism for PaymentOrder");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENTOUT_CASH_PAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.out.Cash.PayableDocuments")
                    .description("Properties to define the Query for Documents that can be payed.");
    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PAYMENTOUT_CASH_CREATESTATUS = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.out.Cash.Status4Create")
                    .description("Possiblity to set the create Status for PaymentDeposit.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYMENTOUT_CASH_FILE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.out.Cash.ActivateFiles")
                    .description("Activate the possiblity to upload Files");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYMENTOUT_CASH_FILESTRBROWSER = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.out.Cash.ActivateFilesStructurBrowser")
                    .description("Activate the possiblity to upload Files incl. FolderStructure");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENTOUT_CHECK_PAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.out.Check.PayableDocuments")
                    .description("Properties to define the Query for Documents that can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PAYMENTOUT_CHECK_CREATESTATUS = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.out.Check.Status4Create")
                    .description("Possiblity to set the create Status for PaymentDeposit.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENTOUT_DEPOSIT_PAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.out.Deposit.PayableDocuments")
                    .description("Properties to define the Query for Documents taht can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PAYMENTOUT_DEPOSIT_CREATESTATUS = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.out.Deposit.Status4Create")
                    .description("Possiblity to set the create Status for PaymentDeposit.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYMENTOUT_DEPOSIT_FILE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.out.Deposit.ActivateFiles")
                    .description("Activate the possiblity to upload Files");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYMENTOUT_DEPOSIT_FILESTRBROWSER = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.out.Deposit.ActivateFilesStructurBrowser")
                    .description("Activate the possiblity to upload Files incl. FolderStructure");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENTOUT_DETRACTION_PAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.out.Detraction.PayableDocuments")
                    .description("Properties to define the Query for Documents taht can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PAYMENTOUT_DETRACTION_CREATESTATUS = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.out.Detraction.Status4Create")
                    .description("Possiblity to set the create Status for PaymentDeposit.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENTOUT_EXCHANGE_PAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.out.Exchange.PayableDocuments")
                    .description("Properties to define the Query for Documents taht can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PAYMENTOUT_EXCHANGE_CREATESTATUS = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.out.Exchange.Status4Create")
                    .description("Possiblity to set the create Status for PaymentDeposit.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYMENTOUT_INTERNAL_ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.out.Internal.Activate")
                    .description("Activate the Internal Payment Documents.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENTOUT_INTERNAL_PAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.out.Internal.PayableDocuments")
                    .description("Properties to define the Query for Documents that can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PAYMENTOUT_INTERNAL_CREATESTATUS = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.out.Internal.Status4Create")
                    .description("Possiblity to set the create Status for PaymentDeposit.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PAYMENTOUT_THRESHOLD4CREATEDOC = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.out.Threshold4CreateOrderOnSettle")
                    .defaultValue("0")
                    .description("Threshold to generated a payment order or collection order on settlement.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PAYMENTOUT_RETENTION_ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.out.Retention.Activate")
                    .defaultValue(true)
                    .description("Activate the possiblity to use Retention as a payment.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PAYMENTOUT_RETENTION_CREATESTATUS = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.out.Retention.Status4Create")
                    .description("Possiblity to set the create Status for PaymentDeposit.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PAYMENTOUT_RETENTION_PAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "payment.out.Retention.PayableDocuments")
                    .description("Properties to define the Query for Documents that can be payed.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PETTYCASH_ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "AccountPettyCash.Activate")
                    .description("Activate AccountPettyCash.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PETTYCASH_PERMITPARTIAL = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "AccountPettyCash.PermitPartialBalance")
                    .description("Permit the partial Balance of a AccountPettyCash.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PETTYCASH_LATEBALANCETRANS = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "AccountPettyCash.LateBalanceTransaction")
                    .description("If set to true the transaction for the balance is done with the PaymentDoc.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PETTYCASH_CONNECT_INVOICE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "AccountPettyCash.ConnectIncomingInvoice")
                    .description("Enable the connection of Incoming Invoice.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PETTYCASH_CONNECT_RECEIPT = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "AccountPettyCash.ConnectIncomingReceipt")
                    .description("Enable the connection of Incoming Invoice.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PETTYCASH_CONNECT_PROFSERVRECEIPT = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "AccountPettyCash.ConnectIncomingProfServReceipt")
                    .description("Enable the connection of Incoming ProfServReceipt.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PETTYCASHBAL_REQUIREBOOKED4PAY = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "PettyCashBalance.RequireBooked4Payment")
                    .description("Permit the partial Balance of a AccountPettyCash.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink PETTYCASHBAL_ACTDEF4COLORD = new SysConfLink()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "PettyCashBalance.ActionDefinition4CollectionOrder")
                    .description("Default ActionDefinition for the creation of a Collection Order.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink PETTYCASHBAL_ACTDEF4PAYORD = new SysConfLink()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "PettyCashBalance.ActionDefinition4PaymentOrder")
                    .description("Default ActionDefinition for the creation of a Payment Order.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink PETTYCASHBAL_CONTACT4PAYORD = new SysConfLink()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "PettyCashBalance.Contact4PaymentOrder")
                    .description("Set a Contact for the Payment Order");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink PETTYCASHBAL_CONTACT4COLORD = new SysConfLink()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "PettyCashBalance.Contact4CollectionOrder")
                    .description("Set a Contact for the Collection Order");

    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PETTYCASHBAL_CONTACT4ORDERCR = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "PettyCashBalance.ActivateContact4OrderCreation")
                    .description("Activate the possiblity to select the contact on creation of PaymentOrder/CollectionOrder");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PETTYCASHRECEIPT_REVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "PettyCashReceipt.RevisionSequence")
                    .defaultValue(CINumGenSales.IncomingInvoiceRevisionSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

   /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PETTYCASHRECEIPT_JASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "PettyCashReceipt.JasperReport")
                    .description("Name of the jasperReport for PettyCashReceipt.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> PETTYCASHRECEIPT_MIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "PettyCashReceipt.Mime")
                    .clazz(JasperMime.class)
                    .description("Mime of the jasperReport for PettyCashReceipt.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PETTYCASHRECEIPT_ASSIGNEMPLOYEE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "PettyCashReceipt.AssignEmployee")
                    .description("Activate the mechanism to assign an employee to PettyCashReceipt.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PETTYCASHRECEIPT_PRODUCTAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "PettyCashReceipt.ProductAutoComplete")
                    .description("Possiblity to overwrite the standart properties for Product AutoComplete.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PRODUCTREQUEST_ASSDEP = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "ProductRequest.AssignDepartment")
                    .description("Activate the mechanism to assign a department to ProductRequest.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PRODUCTREQUEST_ASSIGNEMPLOYEE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "ProductRequest.AssignEmployee")
                    .description("Activate the mechanism to assign an employee to ProductRequest.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute PRODUCTREQUEST_AUTOCOMPLETE4PRODUCTS
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "ProductRequest.AutoComplete4Products")
                    .description("Possib ility to overwrite the dafutl Autocomplete for Products in ProductRequest.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute PRODUCTREQUEST_JASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "ProductRequest.JasperReport")
                    .description("Name of the jasperReport for ProductRequest.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> PRODUCTREQUEST_MIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "ProductRequest.Mime")
                    .clazz(JasperMime.class)
                    .description("Mime of the jasperReport for ProductRequest.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PRODUCTREQUEST_ACTIVATECOMMENT = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "ProductRequest.ActivateComment")
                    .description("Activate the mechanism to write Comments for Order Outbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PRODUCTREQUEST_FILE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "ProductRequest.ActivateFiles")
                    .description("Activate the possiblity to upload Files");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PRODUCTREQUEST_FILESTRBROWSER = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "ProductRequest.ActivateFilesStructurBrowser")
                    .description("Activate the possiblity to upload Files incl. FolderStructure");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink PRODSTOCKRPTSTORAGEGRP = new SysConfLink()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.ProductStockReport.StorageGroup")
                    .description("StorageGroup applied for the ProductStockReport.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final IntegerSysConfAttribute PRODUCTIONCOSTINGMAXDEV = new IntegerSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "ProductionCosting.MaxDeviation")
                    .defaultValue(0)
                    .description("Maximum in percent that the new costing can deviat from the "
                                    + "currenct cost before triggering an alert. ");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink PRODUCTIONREPORTDEFAULTWAREHOUSE = new SysConfLink()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "ProductionReport.DefaultWareHouse")
                    .description("Possibility to set a default Warehouse for ProductionReport. If not set "
                                    + "the default from Products applies.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PERCEPTIONCERTIFICATEACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "PerceptionCertificate.Activate")
                    .description("Activate Perception Certificates");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute QUOTEREQUEST_ACTIVATE = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "QuoteRequest.Activate")
                    .defaultValue("true")
                    .description("Activate the possiblity to register QuoteRequest");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute QUOTEREQUEST_JASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "QuoteRequest.JasperReport")
                    .description("Name of the jasperReport for QuoteRequest.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> QUOTEREQUEST_MIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "QuoteRequest.Mime")
                    .clazz(JasperMime.class)
                    .description("Mime of the jasperReport for QuoteRequest.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink RETURNUSAGEREPORTDEFAULTPRODUCTDOCUMENTTYPE = new SysConfLink()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "ReturnUsageReport.DefaultProductDocumentType")
                    .description("Default product document type for ReturnUsageReport.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink PRODUCTIONREPORTDEFAULTPRODUCTDOCUMENTTYPE = new SysConfLink()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "ProductionReport.DefaultProductDocumentType")
                    .description("Default product document type for ProductionReport.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute PROFSERVRETENTIONCERTIFICATEACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "ProfServRetentionCertificate.Activate")
                    .description("Activate Retention Certificates");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute QUOTATION_ASSIGNEMPLOYEE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Quotation.AssignEmployee")
                    .description("Allows to assign to an Employee.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute QUOTATION_ACTIVATECONDITION = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Quotation.ActivateCondition")
                    .description("Activate Condition for Quotation.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute QUOTATION_JASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Quotation.JasperReport")
                    .description("Name of the jasperReport for Quotation");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> QUOTATION_MIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Quotation.Mime")
                    .clazz(JasperMime.class)
                    .description("Name of the jasperReport for Quotation");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute RECEIPT_ASSIGNEMPLOYEE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Receipt.AssignEmployee")
                    .description("Activate the mechanism to assign employee to Receipt");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute RECEIPT_ACTIVATECONDITION = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Receipt.ActivateCondition")
                    .description("Activate the mechanism to assign a condition in Receipt");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink  RECEIPT_DEFAULTCONDITION = new SysConfLink()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Receipt.DefaultCondition")
                    .description("Set the default condition for Receipt");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute RECEIPT_JASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Receipt.JasperReport")
                    .description("Name of the jasperReport for ReturnUsageReport");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> RECEIPT_MIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Receipt.Mime")
                    .clazz(JasperMime.class)
                    .description("Mime for the jasperReport for ReturnUsageReport");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BitEnumSysConfAttribute<JasperActivation> RECEIPT_JASPERACTIVATION
        = new BitEnumSysConfAttribute<JasperActivation>()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Receipt.JasperActivation")
                    .clazz(JasperActivation.class)
                    .addDefaultValue(JasperActivation.ONCREATE)
                    .addDefaultValue(JasperActivation.ONEDIT)
                    .description("Activation of Jasperreport Creation");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute RECEIPT_TRANSDOC = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Receipt.ActivateTransDocShadow")
                    .description("Activate the possiblity to create a TransactionDocumentShadow");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink RECEIPT_DEFAULTWAREHOUSE = new SysConfLink()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Receipt.DefaultWareHouse")
                    .description("Possibility to set a default Warehouse for RecievingTicket-ActivateTransDocShadow. "
                                    + "If not set the default from Products applies.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute RECEIPT_FROMDELIVERYNOTEAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Receipt.CreateFromDeliveryNoteAutoComplete")
                    .description("AutoComplete and Activation for Create from DeliveryNote for Receipt.")
                    .addDefaultValue("Type", "Sales_DeliveryNote")
                    .addDefaultValue("StatusGroup01", "Sales_DeliveryNoteStatus")
                    .addDefaultValue("Status01", "Open");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute RECEIPT_VALIDATION = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Receipt.Validation")
                    .addDefaultValue("Validation01", "QUANTITYGREATERZERO")
                    .addDefaultValue("Validation02", "AMOUNTGREATERZERO")
                    .description("Configuration Properties to activate validations.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute RECIEVINGTICKET_FROMORDEROUTBOUNDAC
                    = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "RecievingTicket.CreateFromOrderOutboundAutoComplete")
                    .description(" QueryBuilder for Autocomplete of OrderOutbound to create Recieving Ticket from.")
                    .addDefaultValue("Type", CISales.OrderOutbound.getType().getName())
                    .addDefaultValue("StatusGroup01", CISales.OrderOutboundStatus.getType().getName())
                    .addDefaultValue("Status01", CISales.OrderOutboundStatus.Open.key)
                    .addDefaultValue("StatusGroup02", CISales.OrderOutboundStatus.getType().getName())
                    .addDefaultValue("Status02", CISales.OrderOutboundStatus.Invoiced.key);

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute RECIEVINGTICKET_POSREMARK = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "RecievingTicket.ActivateRemark4Position")
                    .description("Activate the Company Independent Reports.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute RECIEVINGTICKET_REVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "RecievingTicket.RevisionSequence")
                    .defaultValue(CINumGenSales.RecievingTicketRevisionSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink RECIEVINGTICKET_DEFAULTWAREHOUSE = new SysConfLink()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "RecievingTicket.DefaultWareHouse")
                    .description("Possibility to set a default Warehouse for RecievingTicket. If not set "
                                    + "the default from Products applies.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute RECIEVINGTICKET_FILE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "RecievingTicket.ActivateFiles")
                    .description("Activate the possiblity to upload Files");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute RECIEVINGTICKET_FILESTRBROWSER = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "RecievingTicket.ActivateFilesStructurBrowser")
                    .description("Activate the possiblity to upload Files incl. FolderStructure");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute REMINDER_JASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Reminder.JasperReport")
                    .description("Name of the jasperReport for CreditNote");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> REMINDER_MIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Reminder.Mime")
                    .clazz(JasperMime.class)
                    .description("Name of the jasperReport for CreditNote");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BitEnumSysConfAttribute<JasperActivation> REMINDER_JASPERACTIVATION
        = new BitEnumSysConfAttribute<JasperActivation>()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Reminder.JasperActivation")
                    .clazz(JasperActivation.class)
                    .addDefaultValue(JasperActivation.ONCREATE)
                    .addDefaultValue(JasperActivation.ONEDIT)
                    .description("Activation of Jasperreport Creation");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute RETENTIONCERTIFICATEACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "RetentionCertificate.Activate")
                    .description("Activate Retention Certificates");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute RETUSAGEREPJASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "ReturnUsageReport.JasperReport")
                    .description("Name of the jasperReport for ReturnUsageReport");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> RETUSAGEREPMIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "ReturnUsageReport.Mime")
                    .clazz(JasperMime.class)
                    .description("Mime for the jasperReport for ReturnUsageReport");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_ABCREPORT = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.ABCReport")
                    .description("Configuration for the ABCReport.")
                    .addDefaultValue("Type01", CISales.Invoice.getType().getName())
                    .addDefaultValue("Type02", CISales.Receipt.getType().getName())
                    .addDefaultValue("Type03", CISales.IncomingInvoice.getType().getName())
                    .addDefaultValue("Type04", CISales.IncomingReceipt.getType().getName())
                    .addDefaultValue("StatusGroup01", CISales.InvoiceStatus.getType().getName())
                    .addDefaultValue("Status01", "!" + CISales.InvoiceStatus.Replaced.key)
                    .addDefaultValue("StatusGroup02", CISales.ReceiptStatus.getType().getName())
                    .addDefaultValue("Status02", "!" + CISales.ReceiptStatus.Replaced.key)
                    .addDefaultValue("StatusGroup03", CISales.IncomingInvoiceStatus.getType().getName())
                    .addDefaultValue("Status03", "!" + CISales.IncomingInvoiceStatus.Replaced.key)
                    .addDefaultValue("StatusGroup04", CISales.IncomingReceiptStatus.getType().getName())
                    .addDefaultValue("Status04", "!" + CISales.IncomingReceiptStatus.Replaced.key);

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_ABCREPORT_ACTASSIGNED = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.ABCReport.ActivateContactAssigned2Employee")
                    .description("Activate the Assigned Employee.");

    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_ACCOUNTS4CONTACT = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.AccountsStatement4Contact")
                    .description("Properties for AccountsStatement4Contact Report.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_ACCOUNTS4CONTACT_SWAPINFO = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.AccountsStatement4Contact.ActivateSwapInfo")
                    .description("Activate the column for Swap related Infos.");

    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_ACCOUNTSPAYABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.AccountsPayable")
                    .addDefaultValue("Type01", CISales.IncomingInvoice.getType().getName())
                    .addDefaultValue("Type02", CISales.IncomingReceipt.getType().getName())
                    .addDefaultValue("Type03", CISales.IncomingReminder.getType().getName())
                    .addDefaultValue("StatusGroup01", CISales.IncomingInvoiceStatus.getType().getName())
                    .addDefaultValue("Status01", CISales.IncomingInvoiceStatus.Open.key)
                    .addDefaultValue("StatusGroup02", CISales.IncomingReceiptStatus.getType().getName())
                    .addDefaultValue("Status02", CISales.IncomingReceiptStatus.Open.key)
                    .addDefaultValue("StatusGroup03", CISales.IncomingReminderStatus.getType().getName())
                    .addDefaultValue("Status03", CISales.IncomingReminderStatus.Open.key)
                    .addDefaultValue("PAID.Type01", CISales.IncomingInvoice.getType().getName())
                    .addDefaultValue("PAID.Type02", CISales.IncomingReceipt.getType().getName())
                    .addDefaultValue("PAID.Type03", CISales.IncomingReminder.getType().getName())
                    .addDefaultValue("PAID.StatusGroup01", CISales.IncomingInvoiceStatus.getType().getName())
                    .addDefaultValue("PAID.Status01", "!" + CISales.IncomingInvoiceStatus.Replaced.key)
                    .addDefaultValue("PAID.StatusGroup02", CISales.IncomingReceiptStatus.getType().getName())
                    .addDefaultValue("PAID.Status02", "!" + CISales.IncomingReceiptStatus.Replaced.key)
                    .addDefaultValue("PAID.StatusGroup03", CISales.IncomingReminderStatus.getType().getName())
                    .addDefaultValue("PAID.Status03", "!" + CISales.IncomingReminderStatus.Replaced.key)
                    .description("Properties for AccountsPayable Report.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_ACCOUNTSPAYABLE_ASSIGNED = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.AccountsPayable.ActivateContactAssigned2Employee")
                    .description("Activate the column for Employee Assigned to Contact.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_ACCOUNTSPAYABLE_SWAPINFO = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.AccountsPayable.ActivateSwapInfo")
                    .description("Activate the column for Swap related Infos.");

    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_ACCOUNTSRECIEVABLE = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.AccountsRecievable")
                    .addDefaultValue("Type01", CISales.Invoice.getType().getName())
                    .addDefaultValue("Type02", CISales.Receipt.getType().getName())
                    .addDefaultValue("Type03", CISales.Reminder.getType().getName())
                    .addDefaultValue("Type04", CISales.IncomingExchange.getType().getName())
                    .addDefaultValue("StatusGroup01", CISales.InvoiceStatus.getType().getName())
                    .addDefaultValue("Status01", CISales.InvoiceStatus.Open.key)
                    .addDefaultValue("StatusGroup02", CISales.ReceiptStatus.getType().getName())
                    .addDefaultValue("Status02", CISales.ReceiptStatus.Open.key)
                    .addDefaultValue("StatusGroup03", CISales.ReminderStatus.getType().getName())
                    .addDefaultValue("Status03", CISales.ReminderStatus.Open.key)
                    .addDefaultValue("StatusGroup04", CISales.IncomingExchangeStatus.getType().getName())
                    .addDefaultValue("Status04", CISales.IncomingExchangeStatus.Open.key)
                    .addDefaultValue("PAID.Type01", CISales.Invoice.getType().getName())
                    .addDefaultValue("PAID.Type02", CISales.Receipt.getType().getName())
                    .addDefaultValue("PAID.Type03", CISales.Reminder.getType().getName())
                    .addDefaultValue("PAID.Type04", CISales.IncomingExchange.getType().getName())
                    .addDefaultValue("PAID.StatusGroup01", CISales.InvoiceStatus.getType().getName())
                    .addDefaultValue("PAID.Status01", "!" + CISales.InvoiceStatus.Replaced.key)
                    .addDefaultValue("PAID.StatusGroup02", CISales.ReceiptStatus.getType().getName())
                    .addDefaultValue("PAID.Status02", "!" + CISales.ReceiptStatus.Replaced.key)
                    .addDefaultValue("PAID.StatusGroup03", CISales.ReminderStatus.getType().getName())
                    .addDefaultValue("PAID.Status03", "!" + CISales.ReminderStatus.Replaced.key)
                    .addDefaultValue("PAID.StatusGroup04", CISales.IncomingExchangeStatus.getType().getName())
                    .addDefaultValue("PAID.Status04", "!" + CISales.IncomingExchangeStatus.Canceled.key)
                    .description("Properties for AccountsRecievable Report.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_ACCOUNTSRECIEVABLE_ASSIGNED = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.AccountsRecievable.ActivateContactAssigned2Employee")
                    .description("Activate the column for Employee Assigned to Contact.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_ACCOUNTSRECIEVABLE_SWAPINFO = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.AccountsRecievable.ActivateSwapInfo")
                    .description("Activate the column for Swap related Infos.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_CARRIER_ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.CarrierReport.Activate")
                    .description("Activate the CarrierReport.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_DOCPOS = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.DocPositionReport")
                    .description("Properties to define a paid threshold for types. e.g. Sale_Invoice=0.05")
                    .addDefaultValue("productType.Type01", CIProducts.StoreableProductAbstract.getType().getName())
                    .addDefaultValue("productType.Type02", CIProducts.UnstoreableProductAbstract.getType().getName());

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_DOCPOS_BOM = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.DocPositionReport.ActivateBOM")
                    .description("Activate the Company Independent Reports.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_DOCPOS_POS = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.DocPositionReport.ActivatePOS")
                    .description("Activate POS Group for DocPositionReport.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_DOCPOS_PRODAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.DocPositionReport.ProductAutoComplete")
                    .description("Properties to define the autocomplete for products");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_PARTIAL = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.PartialReport")
                    .description("Properties 4 PartialReport.")
                    .addDefaultValue("Type", CISales.Invoice2DeliveryNote.getType().getName());

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_PAYMENT = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.PaymentReport")
                    .description("Properties 4 PaymentReport.")
                    .addDefaultValue("IN.Type", CISales.PaymentDocumentAbstract.getType().getName())
                    .addDefaultValue("OUT.Type", CISales.PaymentDocumentOutAbstract.getType().getName())
                    .addDefaultValue("BOTH.Type", CISales.PaymentDocumentIOAbstract.getType().getName());

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_PAYMENTSUM = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.PaymentSumReport")
                    .description("Properties for PaymentSumReport.\n"
                                    + "OUT.Sales_PaymentDepositOut.Negate=true")
                    .addDefaultValue("PAYMENT.IN.Type", CISales.PaymentDocumentAbstract.getType().getName())
                    .addDefaultValue("PAYMENT.OUT.Type", CISales.PaymentDocumentOutAbstract.getType().getName())
                    .addDefaultValue("PAYMENT.BOTH.Type", CISales.PaymentDocumentIOAbstract.getType().getName())
                    .addDefaultValue("DOCUMENT.IN.Type", CISales.IncomingCheck.getType().getName())
                    .addDefaultValue("DOCUMENT.BOTH.Type", CISales.IncomingCheck.getType().getName());

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_PAYMENTSUM_ASSIGENED = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.PaymentSumReport.ActivateContactAssigned2Employee")
                    .description("Properties 4 Products in Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_SALESPROD = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.SalesProductReport")
                    .description("Properties 4 Products in Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_SALESPROD_ASSIGENED = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.SalesProductReport.ActivateContactAssigned2Employee")
                    .description("Properties 4 Products in Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_SALESPROD_CONDITION = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.SalesProductReport.ActivateCondition")
                    .description("Properties 4 Products in Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final IntegerSysConfAttribute REPORT_SALESPROD_PRODFAMLEVEL = new IntegerSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.SalesProductReport.ProductFamilyLevel")
                    .description("Properties 4 Products in Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_SWAP = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.SwapReport")
                    .description("Properties for SwapReport.")
                    .addDefaultValue("Type", CISales.DocumentSumAbstract.getType().getName());

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_SALESRECORD = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.SalesRecordReport")
                    .description("Properties for SalesRecordReport. \n"
                                    + "QueryBuilder definition\n"
                                    + "TYPE.Negate=true"
                                    + "tax.UUID=IGV|OTHER|UNAFF")
                    .addDefaultValue("Type01", CISales.Invoice.getType().getName())
                    .addDefaultValue("Type02", CISales.Receipt.getType().getName())
                    .addDefaultValue("Type03", CISales.CreditNote.getType().getName())
                    .addDefaultValue("Type04", CISales.Reminder.getType().getName())
                    .addDefaultValue(CISales.CreditNote.getType().getName() + ".Negate", "true")
                    .addDefaultValue("tax.06e40be6-40d8-44f4-9d8f-585f2f97ce63", "IGV")
                    .addDefaultValue("tax.90346a9b-f637-4dd2-8cdf-802608cf5660", "OTHER");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_DOCPRODTRANS = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.DocProductTransactionReport")
                    .description("Properties for DocProductTransactionReport.")
                    .addDefaultValue("Type", CISales.DeliveryNote.getType().getName());

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_DOCPRODTRANS_ACTPRODTYPE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.DocProductTransactionReport.ActivateProdType")
                    .description("Activate the ProductType field for the DocProductTransactionReport.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_DOCPRODTRANS_FAB = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.DocProductTransactionReport.AnalyzeFabrication")
                    .description("Activate the Anlization for fabrication for the DocProductTransactionReport.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_DOCPRODTRANSCOST = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.DocProductTransactionCostReport")
                    .description("Properties for DocProductTransactionReportCost.")
                    .addDefaultValue("Type", CISales.DeliveryNote.getType().getName());

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_DOCPRODTRANSCOST_ACTPRODTYPE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.DocProductTransactionCostReport.ActivateProdType")
                    .description("Activate the ProductType field for the DocProductTransactionReport.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_PRODTRANSSUM_ALTERNATIVE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.ProductsTransactionSummaryReport.ActivateAlternative")
                    .description("Activate the ProductType field for the DocProductTransactionReport.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_PRODTRANSSUM_ACQUISITION = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.ProductsTransactionSummaryReport.ActivateAcquisition")
                    .description("Activate AquisitionCosting.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_DOCPRODTRANSCOST_FAB = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.DocProductTransactionCostReport.AnalyzeFabrication")
                    .description("Activate the Anlization for fabrication for the DocProductTransactionReport.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_DOCBALANCE = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.DocBalanceReport")
                    .addDefaultValue("Type01", CISales.Invoice.getType().getName())
                    .addDefaultValue("Type02", CISales.CreditNote.getType().getName())
                    .addDefaultValue("Type03", CISales.Reminder.getType().getName())
                    .addDefaultValue("Type04", CISales.IncomingExchange.getType().getName())
                    .description("Configures the ABC Report for Providers.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_EMPLOYEE2DOC = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.Employee2DocReport")
                    .description("Autocomplete Properties 4 Products in Invoice");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_EMPLOYEE2DOC_CONDITION = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.Employee2DocReport.ActivateCondition")
                    .description("Activate the filter for condition.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_RATEVALID = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.RateValidationReport")
                    .addDefaultValue("Type01", CISales.Invoice.getType().getName())
                    .addDefaultValue("Type02", CISales.CreditNote.getType().getName())
                    .addDefaultValue("Type03", CISales.Reminder.getType().getName())
                    .addDefaultValue("Type04", CISales.Receipt.getType().getName())
                    .addDefaultValue("Type05", CISales.IncomingInvoice.getType().getName())
                    .addDefaultValue("Type06", CISales.IncomingCreditNote.getType().getName())
                    .addDefaultValue("Type07", CISales.IncomingReminder.getType().getName())
                    .addDefaultValue("Type08", CISales.IncomingReceipt.getType().getName())
                    .addDefaultValue("StatusGroup01", CISales.InvoiceStatus.getType().getName())
                    .addDefaultValue("Status01", "!" + CISales.InvoiceStatus.Replaced.key)
                    .addDefaultValue("StatusGroup02", CISales.CreditNoteStatus.getType().getName())
                    .addDefaultValue("Status02", "!" + CISales.CreditNoteStatus.Replaced.key)
                    .addDefaultValue("StatusGroup03", CISales.ReminderStatus.getType().getName())
                    .addDefaultValue("Status03", "!" + CISales.ReminderStatus.Replaced.key)
                    .addDefaultValue("StatusGroup04", CISales.ReceiptStatus.getType().getName())
                    .addDefaultValue("Status04", "!" + CISales.ReceiptStatus.Replaced.key)
                    .addDefaultValue("StatusGroup05", CISales.IncomingInvoiceStatus.getType().getName())
                    .addDefaultValue("Status05", "!" + CISales.IncomingInvoiceStatus.Replaced.key)
                    .addDefaultValue("StatusGroup06", CISales.IncomingCreditNoteStatus.getType().getName())
                    .addDefaultValue("Status06", "!" + CISales.IncomingCreditNoteStatus.Replaced.key)
                    .addDefaultValue("StatusGroup07", CISales.IncomingReminderStatus.getType().getName())
                    .addDefaultValue("Status07", "!" + CISales.IncomingReminderStatus.Replaced.key)
                    .addDefaultValue("StatusGroup08", CISales.IncomingReceiptStatus.getType().getName())
                    .addDefaultValue("Status08", "!" + CISales.IncomingReceiptStatus.Replaced.key)
                    .description("Configures the RateValidationReport for Providers.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_STATISTICS = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.Statistics")
                    .description("Configuration for the StatisticsReport.")
                    .addDefaultValue("Type", CIERP.DocumentAbstract.getType().getName());

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_SALES4ACCOUNTCONTACT = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.SalesReport4Account.CONTACT")
                    .description("Properties 4 SalesReport4Account CONTACT.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_SALES4ACCOUNTCONTACT_SWAPINFO = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.SalesReport4Account.CONTACT.ActivateSwapInfo")
                    .description("Activate the column for Swap related Infos.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_SALES4ACCOUNTIN = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.SalesReport4Account.IN")
                    .description("Properties 4 SalesReport4Account IN.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_SALES4ACCOUNTIN_ASSIGENED = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.SalesReport4Account.IN.ActivateContactAssigned2Employee")
                    .description("Activate the column for Employee Assigned to Contact.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_SALES4ACCOUNTIN_SWAPINFO = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.SalesReport4Account.IN.ActivateSwapInfo")
                    .description("Activate the column for Swap related Infos.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_SALES4ACCOUNTOUT = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.SalesReport4Account.OUT")
                    .description("Properties 4 SalesReport4Account OUT.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_SALES4ACCOUNTOUT_ASSIGENED = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.SalesReport4Account.OUT.ActivateContactAssigned2Employee")
                    .description("Activate the column for Employee Assigned to Contact.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPORT_SALES4ACCOUNTOUT_SWAPINFO = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.SalesReport4Account.OUT.ActivateSwapInfo")
                    .description("Activate the column for Swap related Infos.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPORT_SALESKARDEX = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "report.SalesKardexReport")
                    .addDefaultValue("StatusGroup01", CISales.DeliveryNoteStatus.getType().getName())
                    .addDefaultValue("Status01", "!" + CISales.DeliveryNoteStatus.Canceled.key)
                    .addDefaultValue("StatusGroup02", CISales.ConsignmentNoteStatus.getType().getName())
                    .addDefaultValue("Status02", "!" + CISales.ConsignmentNoteStatus.Canceled.key)
                    .addDefaultValue("StatusGroup03", CISales.ProductionOrderStatus.getType().getName())
                    .addDefaultValue("Status03", "!" + CISales.ProductionOrderStatus.Canceled.key)
                    .addDefaultValue("StatusGroup04", CISales.ProductionReportStatus.getType().getName())
                    .addDefaultValue("Status04", "!" + CISales.ProductionReportStatus.Canceled.key)
                    .addDefaultValue("StatusGroup05", CISales.ProductRequestStatus.getType().getName())
                    .addDefaultValue("Status05", "!" + CISales.ProductRequestStatus.Canceled.key)
                    .addDefaultValue("StatusGroup06", CISales.RecievingTicketStatus.getType().getName())
                    .addDefaultValue("Status06", "!" + CISales.RecievingTicketStatus.Canceled.key)
                    .addDefaultValue("StatusGroup07", CISales.ReservationStatus.getType().getName())
                    .addDefaultValue("Status07", "!" + CISales.ReservationStatus.Canceled.key)
                    .addDefaultValue("StatusGroup08", CISales.ReturnUsageReportStatus.getType().getName())
                    .addDefaultValue("Status08", "!" + CISales.ReturnUsageReportStatus.Canceled.key)
                    .addDefaultValue("StatusGroup09", CISales.UsageReportStatus.getType().getName())
                    .addDefaultValue("Status09", "!" + CISales.UsageReportStatus.Canceled.key)
                    .addDefaultValue("StatusGroup10", CISales.TransactionDocumentStatus.getType().getName())
                    .addDefaultValue("Status10", "!" + CISales.TransactionDocumentStatus.Canceled.key)
                    .addDefaultValue("StatusGroup11", CISales.TransactionDocumentShadowInStatus.getType().getName())
                    .addDefaultValue("Status11", "!" + CISales.TransactionDocumentShadowInStatus.Canceled.key)
                    .addDefaultValue("StatusGroup12", CISales.TransactionDocumentShadowOutStatus.getType().getName())
                    .addDefaultValue("Status12", "!" + CISales.TransactionDocumentShadowOutStatus.Canceled.key)
                    .description("Properties 4 SalesKardexReport. prodDoc");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute SERIALNUMBERS = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "SerialNumbers")
                    .concatenate(true)
                    .description("A mapping like: Sales_Invoice=001;002;003.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final IntegerSysConfAttribute SERIALNUMBERSUFFIXLENGTH = new IntegerSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "SerialNumbers.SuffixLength")
                    .defaultValue(6)
                    .description("Length of the number part of a Serialnumber .\"001-NNNNNN\". Default Value: 6");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute SERVICEREQUEST_ASSDEP = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "ServiceRequest.AssignDepartment")
                    .description("Activate the mechanism to assign a department to ServiceRequest.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute SERVICEREQUEST_ASSIGNEMPLOYEE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "ServiceRequest.AssignEmployee")
                    .description("Activate the mechanism to assign an employee to ServiceRequest.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute SERVICEREQUEST_AUTOCOMPLETE4PRODUCTS
        = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "ServiceRequest.AutoComplete4Products")
                    .description("Possib ility to overwrite the dafutl Autocomplete for Products in ServiceRequest.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute SERVICEREQUEST_JASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "ServiceRequest.JasperReport")
                    .description("Name of the jasperReport for ServiceRequest.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> SERVICEREQUEST_MIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "ServiceRequest.Mime")
                    .clazz(JasperMime.class)
                    .description("Mime of the jasperReport for ServiceRequest.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute SERVICEREQUEST_FILE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "ServiceRequest.ActivateFiles")
                    .description("Activate the possiblity to upload Files");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute SERVICEREQUEST_FILESTRBROWSER = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "ServiceRequest.ActivateFilesStructurBrowser")
                    .description("Activate the possiblity to upload Files incl. FolderStructure");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute SERVICEORDEROUTBOUND_ACTIVATECOMMENT = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "ServiceOrderOutbound.ActivateComment")
                    .description("Activate the mechanism to write Comments for Service Order Outbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute SERVICEORDEROUTBOUND_CREATEFROMSERVDREQ = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "ServiceOrderOutbound.CreateFromServiceRequest")
                    .description("Activate the ppossiblity to create an ServiceOrderOutbound from ServiceRequest");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute SERVICEORDEROUTBOUND_ACTIVATECONDITION = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "ServiceOrderOutbound.ActivateCondition")
                    .description("Activate the mechanism to assign a condition in Service Order Outbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute SERVICEORDEROUTBOUND_ACTIVATEDELIVERYDATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "ServiceOrderOutbound.ActivateDeliveryDate")
                    .description("Activate DeliveryDate for Service Order Outbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute SERVICEORDEROUTBOUND_ASSIGNEMPLOYEE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "ServiceOrderOutbound.AssignEmployee")
                    .description("Activate the mechanism to assign employee to a Service Order Outbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute SERVICEORDEROUTBOUND_JASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "ServiceOrderOutbound.JasperReport")
                    .description("Name of the jasperReport for Service Order Outbound");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> SERVICEORDEROUTBOUND_MIME
        = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "ServiceOrderOutbound.Mime")
                    .clazz(JasperMime.class)
                    .description("Mime of the jasperReport for Service Order Outbound.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute SWAPCONFIG = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Config4Swap")
                    .concatenate(true)
                    .description("Configuration for Swap. e.g.\n"
                                    + "TYPE.Type=TYPE\n"
                                    + "TYPE.StatusGroup=STATUSTYPE\n"
                                    + "TYPE.Status=STATUSKEY\n"
                                    + "TYPE.Filter4Contact=true\n");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute TEMPLATE_NUMGEN = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Template.NumberGenerator")
                    .defaultValue(CINumGenSales.TemplateSequence.uuid.toString())
                    .description("NumberGenerator for Templates.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute TRANSDOCJASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "TransactionDocument.JasperReport")
                    .description("Name of the jasperReport for CreditNote");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> TRANSDOCMIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "TransactionDocument.Mime")
                    .clazz(JasperMime.class)
                    .description("Name of the jasperReport for CreditNote");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute TRANSDOCIN_SEQ = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "TransactionDocumentIn.Sequence")
                    .defaultValue(CINumGenSales.TransactionDocumentInSequence.uuid.toString())
                    .description("Sequence for TransactionDocumentIn");

    @EFapsSysConfAttribute
    public static final StringSysConfAttribute TRANSDOCOUT_SEQ = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "TransactionDocumentOut.Sequence")
                    .defaultValue(CINumGenSales.TransactionDocumentOutSequence.uuid.toString())
                    .description("Sequence for TransactionDocumentOut");

    @EFapsSysConfAttribute
    public static final StringSysConfAttribute TRANSDOCOUT2IN_JASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "TransactionDocumentOut2In.JasperReport")
                    .defaultValue("Sales_TransactionDocOut2In")
                    .description("JasperReport for TransactionDocOut2In");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute TRANSDOCSHADOWIN_REVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "TransactionDocumentShadowIn.RevisionSequence")
                    .defaultValue(CINumGenSales.TransactionDocumentShadowInSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute TRANSDOCSHADOWOUT_REVSEQ = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "TransactionDocumentShadowOut.RevisionSequence")
                    .defaultValue(CINumGenSales.TransactionDocumentShadowOutSequence.uuid.toString())
                    .description("UUID of the Sequence used for the Revision.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute USAGEREPORT_PRODUCTAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "UsageReport.ProductAutoComplete")
                    .description("Possiblity to overwrite the standart properties for Product AutoComplete.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink USAGEREPORT_DEFAULTPRODUCTDOCUMENTTYPE = new SysConfLink()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "UsageReport.DefaultProductDocumentType")
                    .description("Default product document type for UsageReport.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink USAGEREPORT_DEFAULTWAREHOUSE = new SysConfLink()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "UsageReport.DefaultWareHouse")
                    .description("Possibility to set a default Warehouse fo0r DeliveryNote. If not set "
                                    + "the default from Products applies.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute USAGEREPORT_JASPERREPORT = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "UsageReport.JasperReport")
                    .description("Name of the jasperReport for UsageReport");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<JasperMime> USAGEREPORT_MIME = new EnumSysConfAttribute<JasperMime>()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "UsageReport.Mime")
                    .clazz(JasperMime.class)
                    .description("Mime for the jasperReport for UsageReport");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute VALIDATION_NAMEREGEX = new StringSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "validation.NameRegex")
                    .defaultValue("(\\d{3}|(\\w{4}))-\\d{1,6}")
                    .description("Regular Expresion used to validate name fields containing SerialNumber.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REMINDER_FROMINVOICEAC = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Reminder.CreateFromInvoiceAutoComplete")
                    .description("Possibiloity to overwrite the configuraiton for the Automcomplete for Invoice.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REMINDER_REASON = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Reminder.ActivateReason")
                    .description("Activate/Deactivate the mechanism to assign a Reason to CreditNote.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute RESERVATIONACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Reservation.Activate")
                    .description("Activate reservations.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute RESERVATIONACTIVATETRANSTRIG = new BooleanSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "Reservation.ActivateTransactionTrigger")
                    .description(" Activate the reservation trigger mechanism on transactions..");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink EXTEND_PRODDOC4MOVEMASSOUT = new SysConfLink()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "extend.ProductDocumentType4MoveMassiveOut")
                    .description("Default Product Document type for move massive - Outgoing.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink EXTEND_PRODDOC4MOVEMASSIN = new SysConfLink()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "extend.ProductDocumentType4MoveMassiveIn")
                    .description("Default Product Document type for move massive - Incoming.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink EXTEND_PRODDOC4MOVEOUT = new SysConfLink()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "extend.ProductDocumentType4MoveOut")
                    .description("Default Product Document type for move - Outgoing.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink EXTEND_PRODDOC4MOVEIN = new SysConfLink()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "extend.ProductDocumentType4MoveIn")
                    .description("Default Product Document type for move - Incoming.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute MIG_TRANSACT = new PropertiesSysConfAttribute()
                    .sysConfUUID(Sales.SYSCONFUUID)
                    .key(Sales.BASE + "migration.TransactionDocument")
                    .description("Mapping of DocumentType OID to new OID. IN -> incoming movements, OUT -> outgoing movements")
                    .addDefaultValue("IN.1526.13", "1526.16")
                    .addDefaultValue("OUT.1526.13", "1526.17");

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
        return SystemConfiguration.get(Sales.SYSCONFUUID);
    }
}
