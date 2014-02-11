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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
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
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: Payment_Base.java 7671 2012-06-14 17:25:53Z
 *          jorge.cueva@moxter.net $
 */
@EFapsUUID("541288ca-b89a-4b1c-97ad-f01eba31ccaa")
@EFapsRevision("$Rev$")
public abstract class PaymentCheckOut_Base
    extends AbstractPaymentOut
{

    private final static String PAYMENT = "org.efaps.esjp.sales.payment.PaymentCheckOut.getpayableDocument";
    private final static String ACCOUNT = "org.efaps.esjp.sales.payment.PaymentCheckOut.getAccount";

    /**
     * Create a new PaymentCheckOut.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = createDoc(_parameter);
        createPayment(_parameter, createdDoc);
        createDocumentTax(_parameter, createdDoc);
        final Return ret = createReportDoc(_parameter, createdDoc);
        return ret;
    }

    public Return getPayment2CheckOutPositionInstances(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Map<?, ?> properties = (HashMap<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String cmd = (String) properties.get("AllCheckDifered");

        boolean band = false;

        final List<Instance> instances = new ArrayList<Instance>();
        final MultiPrint multiPrint = new MultiPrint() {
            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.PaymentCheckOut);
                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.PaymentCheckOut.ID);

                _queryBldr.addWhereAttrInQuery(CISales.Payment.TargetDocument, attrQuery);
            }
        };

        if (!multiPrint.getInstances(_parameter).isEmpty()) {
            final SelectBuilder seldate = new SelectBuilder().linkto(CISales.Payment.TargetDocument).attribute(
                            CISales.PaymentDocumentAbstract.Date);
            final SelectBuilder selduedate = new SelectBuilder().linkto(CISales.Payment.TargetDocument).attribute(
                            CISales.PaymentDocumentAbstract.DueDate);

            final MultiPrintQuery multi = new MultiPrintQuery(multiPrint.getInstances(_parameter));
            multi.addSelect(seldate, selduedate);
            multi.execute();
            while (multi.next()) {
                final DateTime date = multi.<DateTime>getSelect(seldate);
                final DateTime dueDate = multi.<DateTime>getSelect(selduedate);
                if (cmd != null && "AllDifered".equalsIgnoreCase(cmd)) {
                    band = true;
                } else {
                    final QueryBuilder queryBldr = new QueryBuilder(CISales.PayableDocument2Document);
                    queryBldr.addWhereAttrEqValue(CISales.PayableDocument2Document.PayDocLink,
                                    multi.getCurrentInstance().getId());
                    final MultiPrintQuery multi2 = queryBldr.getPrint();
                    final SelectBuilder selDerivaded = new SelectBuilder().linkto(
                                    CISales.PayableDocument2Document.ToLink).attribute(CISales.DocumentAbstract.Name);
                    multi2.addSelect(selDerivaded);
                    multi2.execute();
                    if (multi2.next()) {
                        band = true;
                    } else {
                        band = false;
                    }
                }
                if (!(date.compareTo(dueDate) == 0) && band) {
                    instances.add(multi.getCurrentInstance());
                }
            }
        }

        ret.put(ReturnValues.VALUES, instances);
        return ret;
    }

    @SuppressWarnings("unchecked")
    public Return getpayableDocument(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();

        Map<Instance, String> values;
        if (Context.getThreadContext().containsRequestAttribute(PaymentCheckOut_Base.PAYMENT)) {
            values = (Map<Instance, String>) Context.getThreadContext().getRequestAttribute(
                            PaymentCheckOut_Base.PAYMENT);
        } else {
            values = new HashMap<Instance, String>();
            Context.getThreadContext().setRequestAttribute(PaymentCheckOut_Base.PAYMENT, values);
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
            final SelectBuilder selPaymentOid = new SelectBuilder().linkto(
                            CISales.PayableDocument2Document.PayDocLink)
                            .oid();

            multi.addSelect(selOrderOutboundName, selPaymentOid, selCreated);
            multi.execute();
            final Map<Instance, String> derivadedDocumentPayment = new HashMap<Instance, String>();
            while (multi.next()) {
                if (multi.<String>getSelect(selPaymentOid) != null) {
                    final Instance prodInst = Instance.get(multi.<String>getSelect(selPaymentOid));
                    final String name = multi.<String>getSelect(selOrderOutboundName);
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
    public Return getAccount(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();

        Map<Instance, String> values;
        if (Context.getThreadContext().containsRequestAttribute(PaymentCheckOut_Base.ACCOUNT)) {
            values = (Map<Instance, String>) Context.getThreadContext().getRequestAttribute(
                            PaymentCheckOut_Base.ACCOUNT);
        } else {
            values = new HashMap<Instance, String>();
            Context.getThreadContext().setRequestAttribute(PaymentCheckOut_Base.ACCOUNT, values);
            final Map<Instance, Instance> paymentList = new HashMap<Instance, Instance>();

            final List<Instance> paymentceListPos = (List<Instance>) _parameter.get(ParameterValues.REQUEST_INSTANCES);
            final MultiPrintQuery priceListMulti = new MultiPrintQuery(paymentceListPos);
            priceListMulti.execute();
            while (priceListMulti.next()) {
                paymentList.put(priceListMulti.getCurrentInstance(),
                                priceListMulti.getCurrentInstance());
            }

            final QueryBuilder qlb = new QueryBuilder(CISales.Payment);
            final AttributeQuery attrQuery = qlb.getAttributeQuery(CISales.Payment.ID);

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
                    final String name = multi.<String>getSelect(selAccountName);
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
