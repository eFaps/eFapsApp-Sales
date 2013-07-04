/*
 * Copyright 2003 - 2012 The eFaps Team
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

package org.efaps.esjp.sales.payment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * This class must be replaced for customization, therefore it is left empty.
 * Functional description can be found in the related "<code>_base</code>"
 * class.
 *
 * @author The eFaps Team
 * @version $Id: PaymentCheck.java 8100 2012-10-26 15:23:50Z m.aranya@moxter.net
 *          $
 */
@EFapsUUID("a5d92efb-36e4-4d74-9ab2-c3749510ae99")
@EFapsRevision("$Rev$")
public class PaymentCheckOut
    extends PaymentCheckOut_Base
{

    private final static String PAYMENT = "org.efaps.esjp.sales.payment.PaymentCheckOut.getpayableDocument";
    private final static String ACCOUNT = "org.efaps.esjp.sales.payment.PaymentCheckOut.getAccount";

    @SuppressWarnings("unchecked")
    public Return getpayableDocument(Parameter _parameter)
        throws EFapsException
    {
        Return ret = new Return();

        Map<Instance, String> values;
        if (Context.getThreadContext().containsRequestAttribute(PaymentCheckOut.PAYMENT)) {
            values = (Map<Instance, String>) Context.getThreadContext().getRequestAttribute(
                            PaymentCheckOut.PAYMENT);
        } else {
            values = new HashMap<Instance, String>();
            Context.getThreadContext().setRequestAttribute(PaymentCheckOut.PAYMENT, values);
            final Map<Instance, Instance> paymentList = new HashMap<Instance, Instance>();

            final List<Instance> paymentceListPos = (List<Instance>) _parameter.get(ParameterValues.REQUEST_INSTANCES);
            final MultiPrintQuery priceListMulti = new MultiPrintQuery(paymentceListPos);
            priceListMulti.execute();
            while (priceListMulti.next()) {
                paymentList.put(priceListMulti.getCurrentInstance(),
                                priceListMulti.getCurrentInstance());
            }

            final QueryBuilder queryBldr = new QueryBuilder(CISales.PayableDocument2Document);

            final MultiPrintQuery multi = queryBldr.getPrint();

            final SelectBuilder selOrderOutboundName = new SelectBuilder().linkto(
                            CISales.PayableDocument2Document.ToLink)
                            .attribute(CISales.DocumentAbstract.Name);
            final SelectBuilder selCreated = new SelectBuilder().linkto(
                            CISales.PayableDocument2Document.FromLink)
                            .oid();
            final SelectBuilder selPaymentOid = new SelectBuilder().linkto("PayDocLink")
                            .oid();

            multi.addSelect(selOrderOutboundName, selPaymentOid, selCreated);
            multi.execute();
            final Map<Instance, String> derivadedDocumentPayment = new HashMap<Instance, String>();
            while (multi.next()) {
                if (multi.<String>getSelect(selPaymentOid) != null) {
                    final Instance prodInst = Instance.get(multi.<String>getSelect(selPaymentOid));
                    String name = multi.<String>getSelect(selOrderOutboundName);
                    derivadedDocumentPayment.put(prodInst, name);
                }

            }

            for (final Entry<Instance, String> entry : derivadedDocumentPayment.entrySet()) {
                if (entry.getKey() != null && paymentList.get(entry.getKey()) != null) {
                    values.put(paymentList.get(entry.getKey()), entry.getValue().toString());
                }
            }

        }
        ret.put(ReturnValues.VALUES, values.get(_parameter.getInstance()));

        return ret;
    }

    @SuppressWarnings("unchecked")
    public Return getAccount(Parameter _parameter)
        throws EFapsException
    {
        Return ret = new Return();

        Map<Instance, String> values;
        if (Context.getThreadContext().containsRequestAttribute(PaymentCheckOut.ACCOUNT)) {
            values = (Map<Instance, String>) Context.getThreadContext().getRequestAttribute(
                            PaymentCheckOut.ACCOUNT);
        } else {
            values = new HashMap<Instance, String>();
            Context.getThreadContext().setRequestAttribute(PaymentCheckOut.ACCOUNT, values);
            final Map<Instance, Instance> paymentList = new HashMap<Instance, Instance>();

            final List<Instance> paymentceListPos = (List<Instance>) _parameter.get(ParameterValues.REQUEST_INSTANCES);
            final MultiPrintQuery priceListMulti = new MultiPrintQuery(paymentceListPos);
            priceListMulti.execute();
            while (priceListMulti.next()) {
                paymentList.put(priceListMulti.getCurrentInstance(),
                                priceListMulti.getCurrentInstance());
            }

            QueryBuilder qlb = new QueryBuilder(CISales.Payment);
            final AttributeQuery attrQuery = qlb.getAttributeQuery("ID");

            final QueryBuilder queryBldr = new QueryBuilder(CISales.TransactionAbstract);
            queryBldr.addWhereAttrInQuery(CISales.TransactionAbstract.Payment, attrQuery);

            final MultiPrintQuery multi = queryBldr.getPrint();

            final SelectBuilder selAccountName = new SelectBuilder().linkto(
                            CISales.TransactionAbstract.Account)
                            .attribute(CISales.AccountAbstract.Name);
            final SelectBuilder selPaymentOid = new SelectBuilder().linkto(CISales.TransactionAbstract.Payment).oid();

            multi.addSelect(selAccountName, selPaymentOid);
            multi.execute();
            final Map<Instance, String> derivadedAccountPayment = new HashMap<Instance, String>();
            while (multi.next()) {
                final Instance paymentInst = Instance.get(multi.<String>getSelect(selPaymentOid));
                if (multi.<String>getSelect(selPaymentOid) != null) {
                    String name = multi.<String>getSelect(selAccountName);
                    derivadedAccountPayment.put(paymentInst, name);
                }

            }

            for (final Entry<Instance, String> entry : derivadedAccountPayment.entrySet()) {
                if (entry.getKey() != null && paymentList.get(entry.getKey()) != null) {
                    values.put(paymentList.get(entry.getKey()), entry.getValue().toString());
                }
            }

        }
        ret.put(ReturnValues.VALUES, values.get(_parameter.getInstance()));

        return ret;
    }

}
