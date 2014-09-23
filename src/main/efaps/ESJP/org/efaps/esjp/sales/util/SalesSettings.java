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

import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("3cad9ef8-22e6-4b3f-9a97-3aa984d8d6c6")
@EFapsRevision("$Rev$")
public interface SalesSettings
{
    /**
     * OID for a Link.<br/>
     * Default Currency for the Form like Invoice etc..
     */
    String CURRENCY4INVOICE = "org.efaps.sales.Currency4Invoice";

    /**
     * Integer.
     * max number of transaction that will be analyzed at once before committing.
     */
    String COSTINGMAXTRANSACTION = "org.efaps.sales.CostingMaxTransaction";

    /**
     * Boolean.
     * Allows to activate/deactivate the costing mechanisms.
     */
    String ACTIVATECOSTING = "org.efaps.sales.ActivateCosting";

    /**
     * Boolean.
     * Allows to activate/deactivate the registering of the prices during a purchase.
     */
    String ACTIVATEREGPURPRICE = "org.efaps.sales.ActivateRegisterOfPuchasePrice";

    /**
     * Link.
     * Storage Group that is used as a filter for using only the transactions
     * that belong to the given StorageGroup, if not present the calculation is over all Storages.
     */
    String COSTINGSTORAGEGROUP = "org.efaps.sales.CostingStorageGroup";

    /**
     * Boolean (true/false).<br/>
     * Activate the possibility to define a minimum retail
     * price.
     */
    String MINRETAILPRICE = "org.efaps.sales.ActivateMinRetailPrice";

    /**
     * Boolean (true/false).<br/>
     * Do the product prices include already the tax, or
     * must it be added? (Crossprice or netprice)
     */
    String PRODPRICENET = "org.efaps.sales.ProductPriceIsNetPrice";

    /**
     * Link to a default warehouse instance.
     */
    String DEFAULTTAXCAT4PRODUCT = "org.efaps.sales.DefaultTaxCategory4Product";

    /**
     * Boolean (true/false).<br/>
     * Activate the UserInterface for Sales Channel.
     */
    String CHANNELSALES = "org.efaps.sales.ActivateSalesChannel";

    /**
     * Boolean (true/false).<br/>
     * Activate the UserInterface for Buying Route.
     */
    String CHANNELBUY = "org.efaps.sales.ActivateBuyingRoute";

    /**
     * UUID's. <br/>
     * UUID of the documents to pay. Incoming Invoice like default.
     */
    String PAYABLEDOCS = "org.efaps.sales.PayableDocuments";

    /**
     * Boolean (true/false).<br/>
     * The current company is an Agent of Perception.
     */
    String ISPERCEPTIONAGENT = "org.efaps.sales.IsPerceptionAgent";

    /**
     * Boolean (true/false).<br/>
     * Activate the calculation of Perception.
     */
    String ACTIVATEPERCEPTION = "org.efaps.sales.ActivatePerception";

    /**
     * Boolean (true/false).<br/>
     * The current company is an Agent of Retention.
     */
    String ISRETENTIONAGENT = "org.efaps.sales.IsRetentionAgent";

    /**
     * Boolean (true/false).<br/>
     * Activate the calculation of retention.
     */
    String ACTIVATERETENTION = "org.efaps.sales.ActivateRetention";

    /**
     * Boolean (true/false).<br/>
     * Activate the show variable.
     */
    String ACTIVATECODE4PAYMENTDOCUMENT = "org.efaps.sales.ActivateCode4PaymentDocument";


    /**
     * Boolean (true/false).<br/>
     * Activate the show variable.
     */
    String ACTIVATECODE4PAYMENTDOCUMENTOUT = "org.efaps.sales.ActivateCode4PaymentDocumentOut";

    /**
     * UUID of the Sequence to payments document.
     */
    String SEQUENCE4PAYMENTDOCUMENT = "org.efaps.sales.NumberGenerator4PaymentDocument";

    /**
     * UUID of the Sequence to payments document out.
     */
    String SEQUENCE4PAYMENTDOCUMENTOUT = "org.efaps.sales.NumberGenerator4PaymentDocumentOut";

    /**
     * UUID of the Sequence to payments document report.
     */
    String ACTIVATEPRINTREPORT4PAYMENTDOCUMENT = "org.efaps.sales.ActivatePrintReport4PaymentDocument";

    /**
     * UUID of the Sequence to Incoming Credit Note.
     */
    String INCOMINGCREDITNOTESEQUENCE = "org.efaps.sales.IncomingCreditNoteSequence";

    /**
     * UUID of the Sequence to Incoming Reminder.
     */
    String INCOMINGREMINDERSEQUENCE = "org.efaps.sales.IncomingReminderSequence";

    /**
     * UUID of the Sequence to Incoming Credit Note.
     */
    String RECIEVINGTICKETSEQUENCE = "org.efaps.sales.RecievingTicketSequence";

    /**
     * UUID of the Sequence to Incoming Receipt.
     */
    String INCOMINGRECEIPTSEQUENCE = "org.efaps.sales.IncomingReceiptSequence";

    /**
     * UUID of the Sequence to Incoming Receipt.
     */
    String INCOMINGINVOICESEQUENCE = "org.efaps.sales.IncomingInvoiceSequence";

    /**
     *  OID default productDocumentType to usage report.
     */
    String PRODUCTDOCUMENTTYPE4USAGEREPORT = "org.efaps.sales.ProductDocumentType4UsageReport";

    /**
     *  OID default productDocumentType to return usage report.
     */
    String PRODUCTDOCUMENTTYPE4RETURNUSAGEREPORT = "org.efaps.sales.ProductDocumentType4ReturnUsageReport";

    /**
     *  OID default productDocumentType to return usage report.
     */
    String STORAGEGROUP4PRODUCTREQUESTREPORT = "org.efaps.sales.StorageGroup4ProductRequestReport";

    /**
     * OID default to insert spending into PaymentOrder.
     */
    String DEFAULTSPENDING = "org.efaps.sales.DefaultSpending";

    /**
     * Default amount to validate generated payment order or collection order.
     */
    String DEFAULTSAMOUNT4CREATEDDOC = "org.efaps.sales.DefaultAmount4CreatedDoc";

    /**
     * Properties.<br/>
     * Can be concatenated.<br/>
     * Set a Price List for a Type used to Calculator. Used for Sales Documents.
     */
    String PRICELIST4CALCULATOR = "org.efaps.sales.PriceList4Calculator";

    /**
     * Listing seperated by LineBreak<br/>
     * Default addresses used as the departure point in delivery note.
     */
    String DEFAULTDEPARTUREPOINTS = "org.efaps.sales.DefaultDeparturePoints";

    /**
     * Listing seperated by LineBreak<br/>
     * Default addresses used as the departure point in delivery note.
     */
    String DEFAULTARRIVALPOINTS = "org.efaps.sales.DefaultArrivalPoints";

    /**
     * Properties. Can be concatenated;
     */
    String AUTOCOMPLETE4PRODUCT = "org.efaps.sales.AutoComplete4Product";

    /**
     * Properties. Can be concatenated.
     * A mapping like: Sales_Invoice=001;002;003
     */
    String SERIALNUMBERS = "org.efaps.sales.SerialNumbers";

    /**
     * Boolean.
     * Activate the reservation trigger mechanism on transactions.
     */
    String ACTIVATETRANSTRIG4RES = "org.efaps.sales.ActivateTransactionTrigger4Reservation";

    /**
     * Link.
     * The Conciliation used for Automation Mechanism.
     */
    String CONCIL4AUTO = "org.efaps.sales.Conciliation4Automation";

    /**
     * Link.
     * Key + UUID of the Currency.
     * ("org.efaps.sales.ActDef4CollectionOrder4PettyCashBalance.123123-15132-13132-13132-321321)
     */
    String ACTDEF4COLORDPC = "org.efaps.sales.ActDef4CollectionOrder4PettyCashBalance";

    /**
     * Link.
     * Key + UUID of the Currency.
     * ("org.efaps.sales.ActDef4PaymentOrder4PettyCashBalance.123123-15132-13132-13132-321321)
     */
    String ACTDEF4PAYORDPC = "org.efaps.sales.ActDef4PaymentOrder4PettyCashBalance";

    /**
     * Boolean.
     * Permit the augmentation for FundstoBeSettled..
     */
    String PERMITAUGMENT4FUNDSTBS = "org.efaps.sales.AccountFundsToBeSettledPermitAugment";

    /**
     * Boolean.
     * Permit the augmentation for FundstoBeSettled..
     */
    String PERMITPARTIAL4PETTYCASH = "org.efaps.sales.AccountPettyCashPermitPartial";

    /**
     * Boolean.
     * Permit the augmentation for FundstoBeSettled..
     */
    String REQUIREBOOKED4PETTYCASHPAYMENT = "org.efaps.sales.AccountPettyCashBalanceRequireBooked4Payment";

    /**
     * Properties. Can be concatenated.
     * A mapping for AutoComplete etc.
     */
    String SWAPCONFIG = "org.efaps.sales.Config4Swap";

}
