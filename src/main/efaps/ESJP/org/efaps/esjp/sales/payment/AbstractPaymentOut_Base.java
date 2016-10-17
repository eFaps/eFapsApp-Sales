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
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.ui.RateUI;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIStatus;
import org.efaps.ci.CIType;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uiform.Field;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
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
        if (_parameter.getInstance() != null
                        && _parameter.getInstance().getType().isKindOf(CISales.BulkPayment.getType())) {
            final PrintQuery print = new PrintQuery(_parameter.getInstance());
            print.addAttribute(CISales.BulkPayment.Name, CISales.BulkPayment.Date);
            print.execute();
            final String name = print.<String>getAttribute(CISales.BulkPayment.Name);
            ret = name + " - " + String.format("%02d", 1);

            _parameter.getParameters().put(getFieldName4Attribute(_parameter,
                            CISales.PaymentDocumentAbstract.Date.name),
                            new String[] { print.<DateTime>getAttribute(CISales.BulkPayment.Date).toString() });

            final QueryBuilder accQueryBldr = new QueryBuilder(CISales.BulkPayment2Account);
            accQueryBldr.addWhereAttrEqValue(CISales.BulkPayment2Account.FromLink, _parameter.getInstance());
            final MultiPrintQuery accMulti = accQueryBldr.getPrint();
            accMulti.addAttribute(CISales.BulkPayment2Account.ToLink);
            accMulti.execute();
            if (accMulti.next()) {
                _parameter.getParameters().put("account",
                                new String[] { accMulti.<Long>getAttribute(CISales.BulkPayment2Account.ToLink)
                                                .toString() });
            }

            final Set<String> names = new HashSet<>();
            final QueryBuilder queryBldr = new QueryBuilder(CISales.BulkPayment2PaymentDocument);
            queryBldr.addWhereAttrEqValue(CISales.BulkPayment2PaymentDocument.FromLink, _parameter.getInstance());
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder sel = new SelectBuilder().linkto(CISales.BulkPayment2PaymentDocument.ToLink)
                            .attribute(CISales.PaymentDocumentOutAbstract.Name);
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

        final Instance newInst = Instance.get(CIERP.Currency.getType(),
                        print.<Long>getAttribute(CISales.AccountCashDesk.CurrencyLink));

        final Instance baseInst = Currency.getBaseCurrency();

        final PrintQuery print2 = new PrintQuery(_parameter.getInstance());
        print2.addAttribute(CISales.BulkPayment.Date);
        print2.execute();
        _parameter.getParameters()
                        .put("date_eFapsDate",
                                        new String[] { print2.<DateTime>getAttribute(CISales.BulkPayment.Date)
                                                        .toString("dd/MM/YYYY") });

        final BigDecimal[] rates = new PriceUtil().getRates(_parameter, newInst, baseInst);
        ret.append("document.getElementsByName('rate')[0].value='").append(rates[3].stripTrailingZeros()).append("';\n")
                        .append("document.getElementsByName('rate").append(RateUI.INVERTEDSUFFIX)
                        .append("')[0].value='").append(rates[3].compareTo(rates[0]) != 0).append("';\n");
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
                final QueryBuilder targetAttrQueryBldr = getQueryBldrFromProperties(_parameter,
                                Sales.PAYMENTDOCUMENTOUT_TOBESETTLED.get());
                final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Payment);
                attrQueryBldr.addWhereAttrInQuery(CISales.Payment.CreateDocument,
                                targetAttrQueryBldr.getAttributeQuery(CISales.DocumentAbstract.ID));

                final QueryBuilder queryBldr = new QueryBuilder(CISales.PaymentDocumentOutAbstract);
                queryBldr.addWhereAttrInQuery(CISales.PaymentDocumentOutAbstract.ID,
                                attrQueryBldr.getAttributeQuery(CISales.Payment.TargetDocument));
                return queryBldr.getQuery().execute();
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
                final Properties tmpProps = new Properties();
                for (final Enumeration<?> propertyNames = props.propertyNames(); propertyNames.hasMoreElements();) {
                    final String key = (String) propertyNames.nextElement();
                    if (key.startsWith("Type")) {
                        tmpProps.put(key, props.get(key));
                    }
                }
                final QueryBuilder queryBldr = getQueryBldrFromProperties(tmpProps);
                _queryBldr.addWhereAttrInQuery(CISales.DocumentSumAbstract.ID,
                                queryBldr.getAttributeQuery(CISales.DocumentSumAbstract.ID));
                _queryBldr.addWhereAttrInQuery(CISales.DocumentSumAbstract.ID,
                                attrQueryBldr.getAttributeQuery(CISales.Payment.CreateDocument));

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
            final Properties tmpProps = new Properties();
            for (final Enumeration<?> propertyNames = props.propertyNames(); propertyNames.hasMoreElements();) {
                final String key = (String) propertyNames.nextElement();
                if (key.startsWith("Type")) {
                    tmpProps.put(key, props.get(key));
                }
            }
            final QueryBuilder queryBldr = getQueryBldrFromProperties(tmpProps);
            queryBldr.addWhereAttrInQuery(CISales.DocumentSumAbstract.ID, attrQuery);

            final InstanceQuery query = queryBldr.getQuery();
            query.execute();
            if (!query.getValues().isEmpty()) {
                ret.put(ReturnValues.TRUE, true);
            }
        }
        return ret;
    }

    /**
     * Update payable documents.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return settlePayment(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();

        final Instance instDoc = Instance.get(_parameter.getParameterValue("currentDocument"));
        final BigDecimal amount = getAmount4CurrentDocument(_parameter, instDoc, _parameter.getCallInstance());

        final String[] documents = _parameter.getParameterValues("settleDocument");
        final String[] settleTotals = _parameter.getParameterValues("settleTotal");
        boolean first = true;
        if (documents != null && documents.length > 0) {
            final Map<String, Object> infoDoc = new HashMap<>();
            int i = 0;
            for (final String doc : documents) {
                final Instance document = Instance.get(doc);
                if (document.isValid()) {
                    try {
                        final BigDecimal amount4Doc = (BigDecimal) NumberFormatter.get().getTwoDigitsFormatter()
                                        .parse(settleTotals[i]);
                        if (first) {
                            replacePaymentInfo(_parameter, instDoc, document, amount4Doc, infoDoc);
                            first = false;
                        } else {
                            if (!infoDoc.isEmpty()) {
                                final Insert payInsert = new Insert(CISales.Payment);
                                payInsert.add(CISales.Payment.CreateDocument, document);
                                payInsert.add(CISales.Payment.TargetDocument, _parameter.getCallInstance());
                                payInsert.add(CISales.Payment.Amount, amount4Doc);
                                payInsert.add(CISales.Payment.CurrencyLink, infoDoc.get("currency"));
                                payInsert.add(CISales.Payment.Date, infoDoc.get("date"));
                                payInsert.execute();

                                final Insert transIns = new Insert(CISales.TransactionOutbound);
                                transIns.add(CISales.TransactionOutbound.Amount, amount4Doc);
                                transIns.add(CISales.TransactionOutbound.CurrencyId, infoDoc.get("currency"));
                                transIns.add(CISales.TransactionOutbound.Payment, payInsert.getInstance());
                                transIns.add(CISales.TransactionOutbound.Date, infoDoc.get("date"));
                                transIns.add(CISales.TransactionOutbound.Account, infoDoc.get("account"));
                                transIns.execute();

                                final Insert insert = new Insert(CISales.PayableDocument2Document);
                                insert.add(CISales.PayableDocument2Document.FromLink, document);
                                insert.add(CISales.PayableDocument2Document.ToLink, instDoc);
                                insert.add(CISales.PayableDocument2Document.PayDocLink, payInsert);
                                insert.execute();
                            }
                        }
                    } catch (final ParseException e) {
                        LOG.error("Catched", e);
                    }
                }
                i++;
            }
            final BigDecimal settleAmount = getAmount4SettleDocument(_parameter);
            if (amount.compareTo(settleAmount) != 0) {
                final BigDecimal difference = amount.subtract(settleAmount);
                if (checkDifference(_parameter, difference)) {
                    createDocument4Settle(_parameter, infoDoc, difference);
                }
            }
        }
        return ret;
    }

    /**
     * Check difference.
     *
     * @param _parameter the _parameter
     * @param _difference the _difference
     * @return true, if successful
     * @throws EFapsException the e faps exception
     */
    protected boolean checkDifference(final Parameter _parameter,
                                      final BigDecimal _difference)
        throws EFapsException
    {
        boolean ret = false;

        final String defaultAmount = Sales.PAYMENT_AMOUNT4CREATEDOC.get();
        if (defaultAmount != null && !defaultAmount.isEmpty()) {
            final DecimalFormat fmtr = NumberFormatter.get().getFormatter();
            try {
                final BigDecimal amount = (BigDecimal) fmtr.parse(defaultAmount);
                if (_difference.abs().compareTo(amount) >= 0) {
                    ret = true;
                }
            } catch (final ParseException e) {
                LOG.error("Catched", e);
            }
        }
        return ret;
    }

    /**
     * Creates the document.
     *
     * @param _parameter the _parameter
     * @param _infoDoc the _info doc
     * @param _difference the _difference
     * @throws EFapsException the e faps exception
     */
    protected void createDocument4Settle(final Parameter _parameter,
                                         final Map<String, Object> _infoDoc,
                                         final BigDecimal _difference)
        throws EFapsException
    {
        final Instance currInst = Currency.getBaseCurrency();

        CIType type = null;
        CIStatus status = null;
        String name = null;
        Instance.get(null);
        if (_difference.signum() == 1) {
            type = CISales.CollectionOrder;
            status = CISales.CollectionOrderStatus.Approved;
            // Sales_CollectionOrderSequence
            name = NumberGenerator.get(UUID.fromString("e89316af-42c6-4df1-ae7e-7c9f9c2bb73c")).getNextVal();
        } else {
            type = CISales.PaymentOrder;
            status = CISales.PaymentOrderStatus.Approved;
            // Sales_PaymentOrderSequence
            name = NumberGenerator.get(UUID.fromString("f15f6031-c5d3-4bf8-89f4-a7a1b244d22e")).getNextVal();
        }

        final Instance rateCurrInst = Instance.get(CIERP.Currency.getType(), (Long) _infoDoc.get("currency"));
        final PriceUtil util = new PriceUtil();
        final BigDecimal[] rates = util.getExchangeRate((DateTime) _infoDoc.get("date"), rateCurrInst);
        final CurrencyInst cur = new CurrencyInst(rateCurrInst);
        final Object[] rate = new Object[] { cur.isInvert() ? BigDecimal.ONE : rates[1],
                        cur.isInvert() ? rates[1] : BigDecimal.ONE };

        final Insert insert = new Insert(type);
        insert.add(CISales.DocumentSumAbstract.Name, name);
        insert.add(CISales.DocumentSumAbstract.Date, _infoDoc.get("date"));
        insert.add(CISales.DocumentSumAbstract.Contact, getContact4PaymentOut(_parameter));
        insert.add(CISales.DocumentSumAbstract.CurrencyId, currInst);
        insert.add(CISales.DocumentSumAbstract.RateCurrencyId, rateCurrInst);
        insert.add(CISales.DocumentSumAbstract.Salesperson, Context.getThreadContext().getPerson().getId());
        insert.add(CISales.DocumentSumAbstract.RateCrossTotal, _difference.abs());
        insert.add(CISales.DocumentSumAbstract.RateNetTotal, _difference.abs());
        insert.add(CISales.DocumentSumAbstract.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.DocumentSumAbstract.CrossTotal, _difference.abs());
        insert.add(CISales.DocumentSumAbstract.NetTotal, _difference.abs());
        insert.add(CISales.DocumentSumAbstract.DiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.DocumentSumAbstract.StatusAbstract, Status.find(status));
        insert.add(CISales.DocumentSumAbstract.Rate, rate);
        insert.add(CISales.DocumentSumAbstract.Note, generateNote4PaymentOut(_parameter));
        insert.execute();

    }

    /**
     * Generate note4 payment out.
     *
     * @param _parameter the _parameter
     * @return the string
     * @throws EFapsException the e faps exception
     */
    protected String generateNote4PaymentOut(final Parameter _parameter)
        throws EFapsException
    {
        final StringBuilder ret = new StringBuilder();

        if (_parameter.getCallInstance() != null && _parameter.getCallInstance().isValid()) {
            final PrintQuery print = new PrintQuery(_parameter.getCallInstance());
            print.addAttribute(CISales.PaymentDocumentIOAbstract.Name);
            print.execute();

            ret.append(DBProperties.getProperty(AbstractPaymentOut.class.getName() + ".generateDoc4Note.Label"))
                            .append(": ").append(_parameter.getCallInstance().getType().getLabel()).append(" - ")
                            .append(print.<String>getAttribute(CISales.PaymentDocumentIOAbstract.Name));
        }

        return ret.toString();
    }

    /**
     * Gets the contact4 payment out.
     *
     * @param _parameter the _parameter
     * @return the contact4 payment out
     * @throws EFapsException the e faps exception
     */
    protected Long getContact4PaymentOut(final Parameter _parameter)
        throws EFapsException
    {
        Long contactId = null;

        if (_parameter.getCallInstance() != null && _parameter.getCallInstance().isValid()) {
            final PrintQuery print = new PrintQuery(_parameter.getCallInstance());
            print.addAttribute(CISales.PaymentDocumentIOAbstract.Contact);
            print.execute();

            contactId = print.<Long>getAttribute(CISales.PaymentDocumentIOAbstract.Contact);
        }

        return contactId;
    }

    /**
     * Replace payment info.
     *
     * @param _parameter the _parameter
     * @param _doc4Render the _doc4 render
     * @param _doc4Replaced the _doc4 replaced
     * @param _amountDoc4Replaced the _amount doc4 replaced
     * @param _infoDoc the _info doc
     * @throws EFapsException the e faps exception
     */
    protected void replacePaymentInfo(final Parameter _parameter,
                                      final Instance _doc4Render,
                                      final Instance _doc4Replaced,
                                      final BigDecimal _amountDoc4Replaced,
                                      final Map<String, Object> _infoDoc)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CISales.Payment);
        queryBldr.addWhereAttrEqValue(CISales.Payment.TargetDocument, _parameter.getCallInstance());
        queryBldr.addWhereAttrEqValue(CISales.Payment.CreateDocument, _doc4Render);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.Payment.Amount);
        multi.execute();

        if (multi.next()) {
            final BigDecimal amountDoc4Render = multi.<BigDecimal>getAttribute(CISales.Payment.Amount);

            final QueryBuilder queryBldr2 = new QueryBuilder(CISales.TransactionAbstract);
            queryBldr2.addWhereAttrEqValue(CISales.TransactionAbstract.Payment, multi.getCurrentInstance());
            final MultiPrintQuery multi2 = queryBldr2.getPrint();
            multi2.addAttribute(CISales.TransactionAbstract.Account,
                            CISales.TransactionAbstract.CurrencyId,
                            CISales.TransactionAbstract.Date);
            multi2.execute();

            if (multi2.next()) {
                final QueryBuilder queryBldr3 = new QueryBuilder(CISales.Balance);
                queryBldr3.addWhereAttrEqValue(CISales.Balance.Account,
                                multi2.<Long>getAttribute(CISales.TransactionAbstract.Account));
                queryBldr3.addWhereAttrEqValue(CISales.Balance.Currency,
                                multi2.<Long>getAttribute(CISales.TransactionAbstract.CurrencyId));
                final MultiPrintQuery multi3 = queryBldr3.getPrint();
                multi3.addAttribute(CISales.Balance.Amount);
                multi3.execute();

                if (multi3.next()) {
                    final BigDecimal balanceAmount = multi3.<BigDecimal>getAttribute(CISales.Balance.Amount);
                    if (multi2.getCurrentInstance().getType().equals(CISales.TransactionOutbound.getType())) {
                        final BigDecimal different = amountDoc4Render.subtract(_amountDoc4Replaced);
                        final Update update = new Update(multi3.getCurrentInstance());
                        update.add(CISales.Balance.Amount, balanceAmount.add(different));
                        update.execute();

                        final Update update2 = new Update(multi2.getCurrentInstance());
                        update2.add(CISales.TransactionAbstract.Amount, _amountDoc4Replaced);
                        update2.execute();

                        final Update update3 = new Update(multi.getCurrentInstance());
                        update3.add(CISales.Payment.CreateDocument, _doc4Replaced);
                        update3.add(CISales.Payment.Amount, _amountDoc4Replaced);
                        update3.execute();

                        final Insert insert = new Insert(CISales.PayableDocument2Document);
                        insert.add(CISales.PayableDocument2Document.FromLink, _doc4Replaced);
                        insert.add(CISales.PayableDocument2Document.ToLink, _doc4Render);
                        insert.add(CISales.PayableDocument2Document.PayDocLink, multi.getCurrentInstance());
                        insert.execute();

                        _infoDoc.put("account", multi2.<Long>getAttribute(CISales.TransactionAbstract.Account));
                        _infoDoc.put("currency", multi2.<Long>getAttribute(CISales.TransactionAbstract.CurrencyId));
                        _infoDoc.put("date", multi2.<DateTime>getAttribute(CISales.TransactionAbstract.Date));
                    }
                }
            }
        }
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
        final BigDecimal recalculatePos = getSum4Positions(_parameter, true)
                        .subtract(payAmount).add(amount.subtract(payAmountDesc));
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
