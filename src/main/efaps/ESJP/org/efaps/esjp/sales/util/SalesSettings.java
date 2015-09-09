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


package org.efaps.esjp.sales.util;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("3cad9ef8-22e6-4b3f-9a97-3aa984d8d6c6")
@EFapsApplication("eFapsApp-Sales")
public interface SalesSettings
{
    /**
     * OID for a Link.<br/>
     * Default Currency for the Form like Invoice etc..
     */
    String BASE = "org.efaps.sales.";

    /**
     * OID for a Link.<br/>
     * Default Currency for the Form like Invoice etc..
     */
    String CURRENCY4INVOICE = SalesSettings.BASE + "Currency4Invoice";

    /**
     * Integer.
     * max number of transaction that will be analyzed at once before committing.
     */
    String COSTINGMAXTRANSACTION = SalesSettings.BASE + "CostingMaxTransaction";

    /**
     * Boolean.
     * Allows to activate/deactivate the registering of the prices during a purchase.
     */
    String ACTIVATEREGPURPRICE = SalesSettings.BASE + "ActivateRegisterOfPuchasePrice";

    /**
     * Link.
     * Storage Group that is used as a filter for using only the transactions
     * that belong to the given StorageGroup, if not present the calculation is over all Storages.
     */
    String COSTINGSTORAGEGROUP = SalesSettings.BASE + "CostingStorageGroup";

    /**
     * Boolean (true/false).<br/>
     * Activate the possibility to define a minimum retail
     * price.
     */
    String MINRETAILPRICE = SalesSettings.BASE + "ActivateMinRetailPrice";

    /**
     * Boolean (true/false).<br/>
     * Do the product prices include already the tax, or
     * must it be added? (Crossprice or netprice)
     */
    String PRODPRICENET = SalesSettings.BASE + "ProductPriceIsNetPrice";

    /**
     * Link to a default warehouse instance.
     */
    String DEFAULTTAXCAT4PRODUCT = SalesSettings.BASE + "DefaultTaxCategory4Product";

    /**
     * Boolean (true/false).<br/>
     * Activate the UserInterface for Buying Route.
     */
    String CHANNELBUY = SalesSettings.BASE + "ActivateBuyingRoute";

    /**
     * UUID's. <br/>
     * UUID of the documents to pay. Incoming Invoice like default.
     */
    String PAYABLEDOCS = SalesSettings.BASE + "PayableDocuments";

    /**
     * Boolean (true/false).<br/>
     * The current company is an Agent of Perception.
     */
    String ISPERCEPTIONAGENT = SalesSettings.BASE + "IsPerceptionAgent";

    /**
     * Boolean (true/false).<br/>
     * Activate the calculation of Perception.
     */
    String ACTIVATEPERCEPTION = SalesSettings.BASE + "ActivatePerception";

    /**
     * Boolean (true/false).<br/>
     * The current company is an Agent of Retention.
     */
    String ISRETENTIONAGENT = SalesSettings.BASE + "IsRetentionAgent";

    /**
     * Boolean (true/false).<br/>
     * Activate the calculation of retention.
     */
    String ACTIVATERETENTION = SalesSettings.BASE + "ActivateRetention";

    /**
     * Boolean (true/false).<br/>
     * Activate the show variable.
     */
    String ACTIVATECODE4PAYMENTDOCUMENT = SalesSettings.BASE + "ActivateCode4PaymentDocument";


    /**
     * Boolean (true/false).<br/>
     * Activate the show variable.
     */
    String ACTIVATECODE4PAYMENTDOCUMENTOUT = SalesSettings.BASE + "ActivateCode4PaymentDocumentOut";

    /**
     * UUID of the Sequence to payments document.
     */
    String SEQUENCE4PAYMENTDOCUMENT = SalesSettings.BASE + "NumberGenerator4PaymentDocument";

    /**
     * UUID of the Sequence to payments document out.
     */
    String SEQUENCE4PAYMENTDOCUMENTOUT = SalesSettings.BASE + "NumberGenerator4PaymentDocumentOut";

    /**
     * UUID of the Sequence to payments document report.
     */
    String ACTIVATEPRINTREPORT4PAYMENTDOCUMENT = SalesSettings.BASE + "ActivatePrintReport4PaymentDocument";

    /**
     * UUID of the Sequence to Incoming Credit Note.
     */
    String INCOMINGCREDITNOTESEQUENCE = SalesSettings.BASE + "IncomingCreditNoteSequence";

    /**
     * UUID of the Sequence to Incoming Reminder.
     */
    String INCOMINGREMINDERSEQUENCE = SalesSettings.BASE + "IncomingReminderSequence";

    /**
     * UUID of the Sequence to Incoming Credit Note.
     */
    String RECIEVINGTICKETSEQUENCE = SalesSettings.BASE + "RecievingTicketSequence";

    /**
     * UUID of the Sequence to Incoming Receipt.
     */
    String INCOMINGRECEIPTSEQUENCE = SalesSettings.BASE + "IncomingReceiptSequence";

    /**
     * UUID of the Sequence to Incoming Receipt.
     */
    String INCOMINGINVOICESEQUENCE = SalesSettings.BASE + "IncomingInvoiceSequence";

    /**
     *  OID default productDocumentType to return usage report.
     */
    String STORAGEGROUP4PRODUCTREQUESTREPORT = SalesSettings.BASE + "StorageGroup4ProductRequestReport";

    /**
     * OID default to insert spending into PaymentOrder.
     */
    String DEFAULTSPENDING = SalesSettings.BASE + "DefaultSpending";

    /**
     * Default amount to validate generated payment order or collection order.
     */
    String DEFAULTSAMOUNT4CREATEDDOC = SalesSettings.BASE + "DefaultAmount4CreatedDoc";

    /**
     * Listing seperated by LineBreak<br/>
     * Default addresses used as the departure point in delivery note.
     */
    String DEFAULTDEPARTUREPOINTS = SalesSettings.BASE + "DefaultDeparturePoints";

    /**
     * Listing seperated by LineBreak<br/>
     * Default addresses used as the departure point in delivery note.
     */
    String DEFAULTARRIVALPOINTS = SalesSettings.BASE + "DefaultArrivalPoints";

    /**
     * Properties. Can be concatenated;
     */
    String AUTOCOMPLETE4PRODUCT = SalesSettings.BASE + "AutoComplete4Product";

    /**
     * Properties. Can be concatenated.
     * A mapping like: Sales_Invoice=001;002;003
     */
    String SERIALNUMBERS = SalesSettings.BASE + "SerialNumbers";

    /**
     * Integer. Length of the number part of a Serialnumber ."001-NNNNNN"
     */
    String SERIALNUMBERSUFFIXLENGTH = SalesSettings.BASE + "SerialNumbersSuffixLength";

    /**
     * Boolean.
     * Activate the reservation trigger mechanism on transactions.
     */
    String ACTIVATETRANSTRIG4RES = SalesSettings.BASE + "ActivateTransactionTrigger4Reservation";

    /**
     * Boolean.
     * Activate reservations .
     */
    String ACTIVATERESERVATIONS = SalesSettings.BASE + "ActivateReservation";


    /**
     * Link.
     * The Conciliation used for Automation Mechanism.
     */
    String CONCIL4AUTO = SalesSettings.BASE + "Conciliation4Automation";

    /**
     * Link.
     */
    String ACTDEF4COLORDPC = SalesSettings.BASE + "ActDef4CollectionOrder4PettyCashBalance";

    /**
     * Link.
     */
    String ACTDEF4PAYORDPC = SalesSettings.BASE + "ActDef4PaymentOrder4PettyCashBalance";

    /**
     * Boolean.
     * Permit the augmentation for FundstoBeSettled..
     */
    String PERMITAUGMENT4FUNDSTBS = SalesSettings.BASE + "AccountFundsToBeSettledPermitAugment";

    /**
     * Boolean.
     * Permit the augmentation for FundstoBeSettled..
     */
    String PERMITPARTIAL4PETTYCASH = SalesSettings.BASE + "AccountPettyCashPermitPartial";

    /**
     * Boolean.
     * Permit the augmentation for FundstoBeSettled..
     */
    String REQUIREBOOKED4PETTYCASHPAYMENT = SalesSettings.BASE + "AccountPettyCashBalanceRequireBooked4Payment";

    /**
     * Boolean.
     * Permit the augmentation for FundstoBeSettled..
     */
    String ACTIVATEPICKER4PETTYCASHRECIPT = SalesSettings.BASE + "PettyCashReceiptActivateContactPicker";

    /**
     * Properties. Can be concatenated.
     * A mapping for AutoComplete etc.
     */
    String SWAPCONFIG = SalesSettings.BASE + "Config4Swap";

    /**
     * Properties. Can be concatenated.
     * A mapping for AutoComplete etc.
     */
    String WITHOUTTAXCONFIG = SalesSettings.BASE + "Config4WithoutTax";

    /**
     * Properties. Can be concatenated.
     * A mapping for AutoComplete etc.
     */
    String CREATEFROMCONFIG = SalesSettings.BASE + "Config4CreateFrom";

    /**
     * Properties. Can be concatenated.
     * A mapping for AutoComplete etc.
     */
    String CALCULATORCONFIG = SalesSettings.BASE + "Config4Calculator";

    /**
     * Properties. Can be concatenated.
     */
    String DOCSUMREPORT = SalesSettings.BASE + "DocumentSumReport";

    /**
     * Properties. Can be concatenated.
     */
    String DOCPOSREPORT = SalesSettings.BASE + "DocPositionReport";

    /**
     * Boolean.
     */
    String DOCPOSREPORTBOM = SalesSettings.BASE + "DocPositionReportBOM";

    /**
     * Properties. Can be concatenated.
     */
    String DOCSITUATIONREPORT = SalesSettings.BASE + "DocSituationReport";

    /**
     * Boolean.
     */
    String RECIEVINGTICKETFROMORDEROUTBOUND = SalesSettings.BASE + "RecievingTicket.CreateFromOrderOutbound";

    /**
     * Properties. QueryBuilder for Autocomplete of OrderOutbound to create Recieving Ticket from.
     */
    String RECIEVINGTICKETCREATEFROMORDEROUTBOUNDAC = SalesSettings.BASE + "RecievingTicket.CreateFromOrderOutboundAutoComplete";

    /**
     * Boolean.
     */
    String RECIEVINGTICKETPOSREMARK = SalesSettings.BASE + "RecievingTicket.ActivateRemark4Position";

    /**
     * Link.
     */
    String USAGEREPORTDEFAULTPRODUCTDOCUMENTTYPE = SalesSettings.BASE + "UsageReport.DefaultProductDocumentType";

    /**
     * Link.
     */
    String RETURNUSAGEREPORTDEFAULTPRODUCTDOCUMENTTYPE = SalesSettings.BASE + "ReturnUsageReport.DefaultProductDocumentType";

    /**
     * Link.
     */
    String PRODUCTIONREPORTDEFAULTPRODUCTDOCUMENTTYPE = SalesSettings.BASE + "ProductionReport.DefaultProductDocumentType";

}
