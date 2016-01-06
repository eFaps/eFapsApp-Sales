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

package org.efaps.esjp.sales.payment;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringEscapeUtils;
import org.efaps.admin.datamodel.ui.RateUI;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.PriceUtil;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("541288ca-b89a-4b1c-97ad-f01eba31ccaa")
@EFapsApplication("eFapsApp-Sales")
public abstract class PaymentCheckOut_Base
    extends AbstractPaymentOut
{

    /** The Constant PAYMENT. */
    private static final String PAYMENT = "org.efaps.esjp.sales.payment.PaymentCheckOut.getpayableDocument";

    /** The Constant ACCOUNT. */
    private static final String ACCOUNT = "org.efaps.esjp.sales.payment.PaymentCheckOut.getAccount";

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
        connect2CheckBook(_parameter, createdDoc);
        executeAutomation(_parameter, createdDoc);
        final Return ret = new Return();
        final File file = createReport(_parameter, createdDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    @Override
    protected void add2DocCreate(final Parameter _parameter,
                                 final Insert _insert,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        super.add2DocCreate(_parameter, _insert, _createdDoc);

        final String issued = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.PaymentCheckOut.Issued.name));
        if (issued != null) {
            _insert.add(CISales.PaymentCheckOut.Issued, issued);
            _createdDoc.getValues().put(getFieldName4Attribute(_parameter, CISales.PaymentCheckOut.Issued.name),
                            issued);
        }
    }
    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _createdDoc created doc
     * @throws EFapsException on error
     */
    public void connect2CheckBook(final Parameter _parameter,
                                  final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final Instance checkBook2checkInst = Instance.get(_parameter
                        .getParameterValue(CIFormSales.Sales_PaymentCheckOutForm.name.name));
        if (checkBook2checkInst.isValid()) {
            final PrintQuery print = new PrintQuery(checkBook2checkInst);
            print.addAttribute(CISales.CheckBook2PaymentCheckOut.Number);
            print.executeWithoutAccessCheck();

            final Update updatePayDoc = new Update(_createdDoc.getInstance());
            updatePayDoc.add(CIERP.PaymentDocumentAbstract.Name,
                            print.<String>getAttribute(CISales.CheckBook2PaymentCheckOut.Number));
            updatePayDoc.executeWithoutTrigger();

            final Update relUpdate = new Update(checkBook2checkInst);
            relUpdate.add(CISales.CheckBook2PaymentCheckOut.ToLink, _createdDoc.getInstance());
            relUpdate.executeWithoutAccessCheck();
        }
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new Return containing maplist
     * @throws EFapsException on error
     */
    public Return updateFields4CheckBook(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Instance checkBookInst = Instance.get(_parameter
                        .getParameterValue(CIFormSales.Sales_PaymentCheckOutForm.checkBook.name));
        if (checkBookInst.isValid()) {
            final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
            final PrintQuery print = new PrintQuery(checkBookInst);
            final SelectBuilder selAcc = SelectBuilder.get().linkto(CISales.CheckBook.AccountLink);
            final SelectBuilder selAccInst = new SelectBuilder(selAcc).instance();
            final SelectBuilder selCurrInst = new SelectBuilder(selAcc).linkto(CISales.AccountCashDesk.CurrencyLink)
                            .instance();
            print.addSelect(selAccInst, selCurrInst);
            print.execute();

            final String msgPhrase = getProperty(_parameter, "MsgPhrase", "Sales_AccountMsgPhrase4Payment");
            final Instance accInst = print.<Instance>getSelect(selAccInst);
            final Instance newInst = print.<Instance>getSelect(selCurrInst);

            final PrintQuery accPrint = new PrintQuery(accInst);
            accPrint.addMsgPhrase(msgPhrase);
            accPrint.execute();

            final String label = accPrint.getMsgPhrase(msgPhrase);

            final StringBuilder nameArr = new StringBuilder().append("new Array('0'");

            final QueryBuilder queryBldr = new QueryBuilder(CISales.CheckBook2PaymentCheckOut);
            queryBldr.addWhereAttrEqValue(CISales.CheckBook2PaymentCheckOut.FromLink, checkBookInst);
            queryBldr.addWhereAttrEqValue(CISales.CheckBook2PaymentCheckOut.ToLink, checkBookInst);
            queryBldr.addWhereAttrIsNull(CISales.CheckBook2PaymentCheckOut.Value);
            queryBldr.addOrderByAttributeAsc(CISales.CheckBook2PaymentCheckOut.Number);
            queryBldr.setLimit(20);
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.setEnforceSorted(true);
            multi.addAttribute(CISales.CheckBook2PaymentCheckOut.Number);
            multi.execute();
            while (multi.next()) {
                final String number = multi.<String>getAttribute(CISales.CheckBook2PaymentCheckOut.Number);
                nameArr.append(",'").append(multi.getCurrentInstance().getOid())
                    .append("','").append(number).append("'");
            }
            nameArr.append(")");
            final Map<String, String> map = new HashMap<String, String>();
            map.put(CIFormSales.Sales_PaymentCheckOutForm.name.name, nameArr.toString());

            final Instance baseInst = Currency.getBaseCurrency();

            final BigDecimal[] rates = new PriceUtil().getRates(_parameter, newInst, baseInst);
            map.put("rate", NumberFormatter.get().getFormatter(0, 3).format(rates[3]));
            map.put("rate" + RateUI.INVERTEDSUFFIX, "" + (rates[3].compareTo(rates[0]) != 0));

            final StringBuilder accArr = new StringBuilder()
                    .append("new Array('").append(accInst.getId()).append("'")
                    .append(",'").append(accInst.getId()).append("','")
                    .append(StringEscapeUtils.unescapeEcmaScript(label))
                    .append("'").append(")");
            map.put(CIFormSales.Sales_PaymentCheckOutForm.account.name, accArr.toString());
            list.add(map);

            ret.put(ReturnValues.VALUES, list);
        }
        return ret;
    }

    /**
     * Gets the payment2 check out position instances.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the payment2 check out position instances
     * @throws EFapsException on error
     */
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

    /**
     * Gets the payable document.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the payable document
     * @throws EFapsException on error
     */
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

    /**
     * Gets the account.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the account
     * @throws EFapsException on error
     */
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
