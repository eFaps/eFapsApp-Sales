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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.efaps.admin.datamodel.ui.RateUI;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uiform.Field;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.PriceUtil;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("9482d7a1-86dd-40f9-b4f6-6a2eac106aeb")
@EFapsApplication("eFapsApp-Sales")
public abstract class AbstractPaymentOut_Base
    extends AbstractPaymentDocument
{

    /**
     * Logger for this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPaymentOut.class);

    @Override
    protected String getDocName4Create(final Parameter _parameter)
        throws EFapsException
    {
        String ret;
        if (_parameter.getInstance() != null && _parameter.getInstance().getType().isKindOf(CISales.BulkPayment
                        .getType())) {
            final PrintQuery print = new PrintQuery(_parameter.getInstance());
            print.addAttribute(CISales.BulkPayment.Name, CISales.BulkPayment.Date);
            print.execute();
            final String name = print.<String>getAttribute(CISales.BulkPayment.Name);
            ret = name + " - " + String.format("%02d", 1);

            _parameter.getParameters().put(getFieldName4Attribute(_parameter,
                            CISales.PaymentDocumentAbstract.Date.name), new String[] { print.<DateTime>getAttribute(
                                            CISales.BulkPayment.Date).toString() });

            final QueryBuilder accQueryBldr = new QueryBuilder(CISales.BulkPayment2Account);
            accQueryBldr.addWhereAttrEqValue(CISales.BulkPayment2Account.FromLink, _parameter.getInstance());
            final MultiPrintQuery accMulti = accQueryBldr.getPrint();
            accMulti.addAttribute(CISales.BulkPayment2Account.ToLink);
            accMulti.execute();
            if (accMulti.next()) {
                _parameter.getParameters().put("account", new String[] { accMulti.<Long>getAttribute(
                                CISales.BulkPayment2Account.ToLink).toString() });
            }

            final Set<String> names = new HashSet<>();
            final QueryBuilder queryBldr = new QueryBuilder(CISales.BulkPayment2PaymentDocument);
            queryBldr.addWhereAttrEqValue(CISales.BulkPayment2PaymentDocument.FromLink, _parameter.getInstance());
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder sel = new SelectBuilder().linkto(CISales.BulkPayment2PaymentDocument.ToLink).attribute(
                            CISales.PaymentDocumentOutAbstract.Name);
            multi.addSelect(sel);
            multi.execute();
            while (multi.next()) {
                names.add(multi.<String>getSelect(sel));
            }
            for (int i = 1; i < 100; i++) {
                final String nameTmp = name + " - " + String.format("%02d", i);
                if (!names.contains(nameTmp)) {
                    ret = nameTmp;
                    break;
                }
            }
        } else {
            ret = super.getDocName4Create(_parameter);
        }
        return ret;
    }

    /**
     * Gets the java script ui value.
     *
     * @param _parameter the _parameter
     * @return the java script ui value
     * @throws EFapsException the e faps exception
     */
    public Return getJavaScriptUIValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final StringBuilder html = new StringBuilder().append("<script type=\"text/javascript\">/*<![CDATA[*/");
        final Instance instance = _parameter.getInstance();
        if (instance != null && instance.getType().isKindOf(CISales.BulkPayment.getType())) {
            final QueryBuilder queryBldr = new QueryBuilder(CISales.BulkPayment2Account);
            queryBldr.addWhereAttrEqValue(CISales.BulkPayment2Account.FromLink, instance.getId());
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CISales.BulkPayment2Account.ToLink);
            multi.execute();
            multi.next();
            final String accountStr = multi.getAttribute(CISales.BulkPayment2Account.ToLink).toString();
            if (accountStr != null && !accountStr.isEmpty()) {
                html.append(getSetDropDownScript(_parameter, accountStr, "account"));
            }
        }
        html.append("/*]]>*/ </script>");
        ret.put(ReturnValues.SNIPLETT, html.toString());
        return ret;
    }

    /**
     * Adds the currency value.
     *
     * @param _parameter the _parameter
     * @param _accountID the _account id
     * @return the string
     * @throws EFapsException the e faps exception
     */
    protected String addCurrencyValue(final Parameter _parameter,
                                      final Long _accountID)
        throws EFapsException
    {
        final StringBuilder ret = new StringBuilder();
        final PrintQuery print = new PrintQuery(CISales.AccountCashDesk.getType(), _accountID);
        print.addAttribute(CISales.AccountCashDesk.CurrencyLink);
        print.execute();

        final Instance newInst = Instance.get(CIERP.Currency.getType(), print.<Long>getAttribute(
                        CISales.AccountCashDesk.CurrencyLink));

        final Instance baseInst = Currency.getBaseCurrency();

        final PrintQuery print2 = new PrintQuery(_parameter.getInstance());
        print2.addAttribute(CISales.BulkPayment.Date);
        print2.execute();
        _parameter.getParameters().put("date_eFapsDate", new String[] { print2.<DateTime>getAttribute(
                        CISales.BulkPayment.Date).toString("dd/MM/YYYY") });

        final BigDecimal[] rates = new PriceUtil().getRates(_parameter, newInst, baseInst);
        ret.append("document.getElementsByName('rate')[0].value='").append(rates[3].stripTrailingZeros()).append("';\n")
                        .append("document.getElementsByName('rate").append(RateUI.INVERTEDSUFFIX).append(
                                        "')[0].value='").append(rates[3].compareTo(rates[0]) != 0).append("';\n");
        return ret.toString();
    }

    /**
     * Check access4 bulk payment.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return checkAccess4BulkPayment(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Instance instance = _parameter.getInstance();
        if (instance == null || !instance.getType().isKindOf(CISales.BulkPayment.getType())) {
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    /**
     * Gets the payment out documents that need to be settled
     * (the user is still accountable for them).
     *
     * @param _parameter the _parameter
     * @return the payment documents4 pay
     * @throws EFapsException the e faps exception
     */
    public Return getPaymentDocuments4ToBeSettled(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new MultiPrint()
        {

            @Override
            public List<Instance> getInstances(final Parameter _parameter)
                throws EFapsException
            {
                final List<Instance> ret = new ArrayList<>();
                final QueryBuilder targetAttrQueryBldr1 = getQueryBldrFromProperties(_parameter,
                                Sales.PAYMENTDOCUMENTOUT_TOBESETTLED.get());
                final QueryBuilder attrQueryBldr1 = new QueryBuilder(CISales.Payment);
                attrQueryBldr1.addWhereAttrInQuery(CISales.Payment.CreateDocument, targetAttrQueryBldr1
                                .getAttributeQuery(CISales.DocumentAbstract.ID));

                final QueryBuilder queryBldr1 = new QueryBuilder(CISales.PaymentDocumentOutAbstract);
                queryBldr1.addWhereAttrInQuery(CISales.PaymentDocumentOutAbstract.ID, attrQueryBldr1.getAttributeQuery(
                                CISales.Payment.TargetDocument));
                ret.addAll(queryBldr1.getQuery().execute());

                final QueryBuilder targetAttrQueryBldr = getQueryBldrFromProperties(_parameter,
                                Sales.PAYMENTDOCUMENTOUT_TOBESETTLED.get(), "Tag");
                if (targetAttrQueryBldr != null) {
                    final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Payment);
                    attrQueryBldr.addWhereAttrInQuery(CISales.Payment.CreateDocument, targetAttrQueryBldr
                                    .getAttributeQuery(CIERP.Tag4DocumentAbstract.DocumentAbstractLink));

                    final QueryBuilder queryBldr = new QueryBuilder(CISales.PaymentDocumentOutAbstract);
                    queryBldr.addWhereAttrInQuery(CISales.PaymentDocumentOutAbstract.ID, attrQueryBldr
                                    .getAttributeQuery(CISales.Payment.TargetDocument));

                    ret.addAll(queryBldr.getQuery().execute());
                }
                return ret;
            }
        }.execute(_parameter);
        return ret;
    }

    /**
     * Drop down4 create documents.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return currentDocument4SettleFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Field()
        {

            @Override
            protected void add2QueryBuilder4List(final Parameter _parameter,
                                                 final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Payment);
                attrQueryBldr.addWhereAttrEqValue(CISales.Payment.TargetDocument, _parameter.getCallInstance());

                final Properties props = Sales.PAYMENTDOCUMENTOUT_TOBESETTLED.get();
                final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter, props, "Electable");
                _queryBldr.addWhereAttrInQuery(CISales.DocumentSumAbstract.ID, queryBldr.getAttributeQuery(
                                CISales.DocumentSumAbstract.ID));
                _queryBldr.addWhereAttrInQuery(CISales.DocumentSumAbstract.ID, attrQueryBldr.getAttributeQuery(
                                CISales.Payment.CreateDocument));

            }
        }.getOptionListFieldValue(_parameter);
        return ret;
    }

    /**
     * Accesscheck that checks if documents a related that must be settled.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return check4ToBeSettled(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Instance paymentDoc = _parameter.getInstance();
        if (paymentDoc.isValid()) {
            final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Payment);
            attrQueryBldr.addWhereAttrEqValue(CISales.Payment.TargetDocument, paymentDoc);
            final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.Payment.CreateDocument);

            final Properties props = Sales.PAYMENTDOCUMENTOUT_TOBESETTLED.get();
            final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter, props, "Electable");
            if (queryBldr != null) {
                queryBldr.addWhereAttrInQuery(CISales.DocumentSumAbstract.ID, attrQuery);

                final InstanceQuery query = queryBldr.getQuery();
                query.execute();
                if (!query.getValues().isEmpty()) {
                    ret.put(ReturnValues.TRUE, true);
                }
            }
        }
        return ret;
    }

    /**
     * Settle payment.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return settlePayment(final Parameter _parameter)
        throws EFapsException
    {
        new Settlement(true).settlePayment(_parameter);
        final Return ret = new Return();
        return ret;
    }

    /**
     * Update fields for settle document.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return updateFields4SettleDocument(final Parameter _parameter)
        throws EFapsException
    {
        return new Settlement(true).updateFields4SettleDocument(_parameter);
    }

    /**
     * Validate payments.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return validateSettlePayment(final Parameter _parameter)
        throws EFapsException
    {
        return new Settlement(true).validateSettlePayment(_parameter);
    }

    /**
     * Update fields4 payment amount desc.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return updateFields4PaymentAmountDesc(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, String>> list = new ArrayList<>();
        final Map<String, String> map = new HashMap<>();
        final int selected = getSelectedRow(_parameter);

        final String amountStr = _parameter.getParameterValues("payment4Pay")[selected];
        final String payAmountStr = _parameter.getParameterValues("paymentAmount")[selected];
        final String payAmountDescStr = _parameter.getParameterValues("paymentAmountDesc")[selected];

        final BigDecimal amount = parseBigDecimal(amountStr);
        final BigDecimal payAmount = parseBigDecimal(payAmountStr);
        final BigDecimal payAmountDesc = parseBigDecimal(payAmountDescStr);

        map.put("paymentAmount", NumberFormatter.get().getTwoDigitsFormatter().format(amount.subtract(payAmountDesc)));
        map.put("paymentAmountDesc", NumberFormatter.get().getTwoDigitsFormatter().format(payAmountDesc));
        final BigDecimal recalculatePos = getSum4Positions(_parameter, true).subtract(payAmount).add(amount.subtract(
                        payAmountDesc));
        BigDecimal total4DiscountPay = BigDecimal.ZERO;
        if (Context.getThreadContext().getSessionAttribute(AbstractPaymentDocument_Base.CHANGE_AMOUNT) == null) {
            map.put("amount", NumberFormatter.get().getTwoDigitsFormatter().format(recalculatePos));
        } else {
            total4DiscountPay = getAmount4Pay(_parameter).abs().subtract(recalculatePos);
        }
        map.put("total4DiscountPay", NumberFormatter.get().getTwoDigitsFormatter().format(total4DiscountPay));
        list.add(map);

        final Return ret = new Return();
        ret.put(ReturnValues.VALUES, list);
        return ret;
    }
}
