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

package org.efaps.esjp.sales;

import java.util.UUID;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
public enum Sales
{
    /** Sales_CreditNote.*/
    CREDITNOTE("6409ee95-3cd2-4063-8080-d010466fd5c7"),
    /** Sales_CreditNotePosition.*/
    CREDITNOTE_POS("09500148-85d0-4610-9125-f26ec45e6d6d"),
    /** Sales_CreditNoteStatus.*/
    CREDITNOTE_STATUS("2cec3e39-3616-4a01-bfe8-99e6b0ea228b"),
    /** Sales_DocumentSumAbstract. */
    DOCSUM_ABSTRACT("611bbf4c-ea17-4974-b88f-7f48e3d015b0"),
    /** Sales_DeliveryNote. */
    DELIVERYNOTE("24578505-8323-4640-b2cb-cfb451382390"),
    /** Sales_DeliveryNotePosition.*/
    DELIVERYNOTE_POS("81fb89fb-0c5e-43bf-9d35-18667892d2c7"),
    /** Sales_DeliveryNoteStatus.*/
    DELIVERYNOTE_STATUS("8f327d14-597d-48ce-8d68-4d0eb0fe214b"),
    /** Sales_GoodsIssueSlip. */
    GOODSISSUESLIP("086795b4-51c3-4626-85a6-05b6c774dcff"),
    /** Sales_GoodsIssueSlipPosition.*/
    GOODSISSUESLIP_POS("15dd35ff-78b4-42d8-89f3-3bd1cedda5ca"),
    /** Sales_GoodsIssueSlipStatus.*/
    GOODSISSUESLIP_STATUS("3059a0bd-ad99-4ee8-9811-85d91d0b6580"),
    /** Sales_IncomingInvoice.*/
    INCOMINGINVOICE("491aa65f-cbc8-464c-86bc-ef19ed6302ad"),
    /** Sales_IncomingInvoicePosition. */
    INCOMINGINVOICE_POS("c7fa6f33-383d-42b3-b645-6a05986902dc"),
    /** Sales_IncomingInvoiceStatus. */
    INCOMINGINVOICE_STATUS("eb1e4d87-934b-41f0-917a-3a96786e9c02"),
    /** Sales_Invoice.*/
    INVOICE("180bf737-8816-4e36-ad71-5ee6392e185b"),
    /** Sales_InvoicePosition. */
    INVOICE_POS("1d4d8a7e-a7d0-42fb-a4d0-4f59f7d66dd2"),
    /** Sales_InvoiceStatus. */
    INVOICE_STATUS("6b23fe75-4b36-4cbf-a829-c841b89c45a3"),
    /** Sales_OrderInbound.*/
    ORDERIN("c9b714db-9078-49db-9ce2-dcf56e3fae2b"),
    /** Sales_OrderInboundPosition. */
    ORDERIN_POS("6968948d-8c0b-45b6-b91d-1dd0428b5f06"),
    /** Sales_OrderInboundStatus. */
    ORDERIN_STATUS("f53fd8fa-7a3d-4fc6-9938-8f50c670e18f"),
    /** Sales_OrderOutbound.*/
    ORDEROUT("ae29490a-0751-413e-9c6f-f198eff849fa"),
    /** Sales_OrderOutboundPosition. */
    ORDEROUT_POS("569bed44-68cb-470e-bd73-9a5e485f73dd"),
    /** Sales_OrderOutboundStatus. */
    ORDEROUT_STATUS("84f2f612-01c1-4d8d-8cfd-875107d1a80c"),
    /** Sales_PartialInvoice.*/
    PARTIALINVOICE("17e30627-33c7-4dcb-a209-056932d0c9c0"),
    /** Sales_PartialInvoicePosition. */
    PARTIALINVOICE_POS("43b9c0ab-86d0-48de-b96e-82b1699b79c6"),
    /** Sales_PartialInvoiceStatus. */
    PARTIALINVOICE_STATUS("c1f34d3b-0852-4c06-8a7d-aab384ee0cc2"),
    /** Sales_Payment. */
    PAYMENT("367bf4d8-d082-4eb6-9afc-88917b876ddb"),
    /** Sales_Quotation.*/
    QUOTATION("68ee631f-757a-4029-9391-418e38860421"),
    /** Sales_QuotationPosition.*/
    QUOTATION_POS("dea2d547-8074-4fc1-8267-d1a5b7f6e1be"),
    /** Sales_QuotationStatus.*/
    QUOTATION_STATUS("bba08338-d552-40ae-9dd9-719129cf8422"),
    /** Sales_Receipt.*/
    RECEIPT("40ebe7bf-ab1e-4ac5-bfbf-81f7c13e8530"),
    /** Sales_ReceiptPosition.*/
    RECEIPT_POS("5d092856-bf86-4965-b50d-7aa7d82cd195"),
    /** Sales_ReceiptStatus.*/
    RECEIPT_STATUS("0265e30e-eb04-4287-a0a6-588b2ab586d1"),
    /** Sales_RecievingTicket.*/
    RECIEVINGTICKET("9a73bf0d-9d2a-4de4-aa80-9e6398968ec5"),
    /** Sales_RecievingTicketPosition.*/
    RECIEVINGTICKET_POS("22bc398c-700c-4371-ad7c-edec2e1529c5"),
    /** Sales_RecievingTicketStatus.*/
    RECIEVINGTICKET_STATUS("22bc398c-700c-4371-ad7c-edec2e1529c5"),
    /** Sales_Reminder.*/
    REMINDER("77b5c009-0b45-40d4-8417-a79c30568904"),
    /** Sales_ReminderPosition.*/
    REMINDER_POS("b6429526-fad2-433a-920d-2a5a50fa9261"),
    /** Sales_ReminderStatus.*/
    REMINDER_STATUS("0996e3f5-00d0-4b4a-9357-23c69fbd8798"),
    /** Sales_Reservation. */
    RESERVATION("b2922837-3a52-4b6a-8ef9-ba2b1c7b789a"),
    /** Sales_ReservationPosition. */
    RESERVATION_POS("2fbcf945-871b-4d7d-8a3c-ada768918509"),
    /** Sales_ReservationStatus. */
    RESERVATION_STATUS("b6b81a6c-c4cb-49d6-a733-7d73bde47c8f"),
    /** Sales_ReturnSlip. */
    RETURNSLIP("80de801d-1943-421a-981d-df84d81f25cf"),
    /** Sales_ReturnSlipPosition. */
    RETURNSLIP_POS("999155cd-579e-4b1c-b7dc-3c907255b924"),
    /** Sales_ReturnSlipStatus. */
    RETURNSLIP_STATUS("c78e866d-460a-4b57-9bd4-7c773e6befb4");

    /**
     * UUID for the Type.
     */
    private final UUID uuid;


    /**
     * @param _uuid string for the uuid
     */
    private Sales(final String _uuid)
    {
        this.uuid = UUID.fromString(_uuid);
    }

    /**
     * Getter method for the instance variable {@link #uuid}.
     *
     * @return value of instance variable {@link #uuid}
     */
    public UUID getUuid()
    {
        return this.uuid;
    }
}
