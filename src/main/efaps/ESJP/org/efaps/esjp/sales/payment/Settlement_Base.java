/*
 * Copyright 2003 - 2021 The eFaps Team
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIStatus;
import org.efaps.ci.CIType;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.tag.Tag;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.Naming;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.PriceUtil;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class Settle_Base.
 *
 * @author The eFaps Team
 */
@EFapsUUID("8e4b870d-4d52-4272-9deb-4cade067c146")
@EFapsApplication("eFapsApp-Sales")
public abstract class Settlement_Base
    extends AbstractPaymentDocument
{

    /**
     * Logger for this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Settlement.class);

    /** The out. */
    private final boolean out;

    /**
     * Instantiates a new settlement base.
     *
     * @param _out the out
     */
    public Settlement_Base(final boolean _out)
    {
        out = _out;
    }

    /**
     * Settle payment.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @throws EFapsException on error
     */
    public void settlePayment(final Parameter _parameter)
        throws EFapsException
    {
        final Instance settleDocInst = Instance.get(_parameter.getParameterValue("currentDocument"));
        final BigDecimal amount = getAmount4CurrentDocument(_parameter, settleDocInst, _parameter.getCallInstance());

        final String[] documents = _parameter.getParameterValues("settleDocument");
        final String[] settleTotals = _parameter.getParameterValues("settleTotal");

        final var payDocEval = EQL.builder().print(_parameter.getCallInstance())
            .attribute(CISales.PaymentDocumentAbstract.RateCurrencyLink)
            .evaluate();
        payDocEval.next();
        final Long payDocCurrencyId = payDocEval.get(CISales.PaymentDocumentAbstract.RateCurrencyLink);
        boolean first = true;
        if (documents != null && documents.length > 0) {
            final Map<String, Object> infoDoc = new HashMap<>();
            int i = 0;
            for (final String doc : documents) {
                final Instance document = Instance.get(doc);
                if (document.isValid()) {
                    try {
                        final BigDecimal amount4Doc = (BigDecimal) NumberFormatter.get().getTwoDigitsFormatter().parse(
                                        settleTotals[i]);
                        if (first) {
                            replacePaymentInfo(_parameter, settleDocInst, document, amount4Doc, infoDoc);
                            first = false;
                        } else {
                            if (!infoDoc.isEmpty()) {
                                final PrintQuery print = new PrintQuery(document);
                                print.addAttribute(CISales.DocumentSumAbstract.Rate,
                                                CISales.DocumentSumAbstract.RateCurrencyId,
                                                CISales.DocumentSumAbstract.CurrencyId);
                                print.execute();
                                final var rateCurrencyId = print.getAttribute(CISales.DocumentSumAbstract.RateCurrencyId);
                                Object rate ;
                                Long paymentRateCurrencyId;
                                Long paymentCurrencyId ;
                                if (payDocCurrencyId.equals(rateCurrencyId)) {
                                    rate = new Object[]{1,1};
                                    paymentRateCurrencyId = payDocCurrencyId;
                                    paymentCurrencyId = payDocCurrencyId;
                                } else {
                                    rate = print.getAttribute(CISales.DocumentSumAbstract.Rate);
                                    paymentRateCurrencyId  = print.getAttribute(CISales.DocumentSumAbstract.RateCurrencyId);
                                    paymentCurrencyId = print.getAttribute(CISales.DocumentSumAbstract.CurrencyId);
                                }

                                final Insert payInsert = new Insert(CISales.Payment);
                                payInsert.add(CISales.Payment.CreateDocument, document);
                                payInsert.add(CISales.Payment.TargetDocument, _parameter.getCallInstance());
                                payInsert.add(CISales.Payment.Amount, amount4Doc);
                                payInsert.add(CISales.Payment.Date, infoDoc.get("date"));
                                payInsert.add(CISales.Payment.RateCurrencyLink, paymentRateCurrencyId);
                                payInsert.add(CISales.Payment.CurrencyLink, paymentCurrencyId);
                                payInsert.add(CISales.Payment.Rate, rate);
                                payInsert.execute();

                                final Insert transIns = new Insert(out ? CISales.TransactionOutbound
                                                : CISales.TransactionInbound);
                                transIns.add(CISales.TransactionOutbound.Amount, amount4Doc);
                                transIns.add(CISales.TransactionOutbound.CurrencyId, infoDoc.get("currency"));
                                transIns.add(CISales.TransactionOutbound.Payment, payInsert.getInstance());
                                transIns.add(CISales.TransactionOutbound.Date, infoDoc.get("date"));
                                transIns.add(CISales.TransactionOutbound.Account, infoDoc.get("account"));
                                transIns.execute();

                                final Insert insert = new Insert(CISales.PayableDocument2Document);
                                insert.add(CISales.PayableDocument2Document.FromLink, document);
                                insert.add(CISales.PayableDocument2Document.ToLink, settleDocInst);
                                insert.add(CISales.PayableDocument2Document.PayDocLink, payInsert);
                                insert.execute();
                            }
                        }
                    } catch (final ParseException e) {
                        Settlement_Base.LOG.error("Catched", e);
                    }
                }
                i++;
            }
            final BigDecimal settleAmount = getAmount4SettleDocument(_parameter);
            if (amount.compareTo(settleAmount) != 0) {
                final BigDecimal difference = amount.subtract(settleAmount);
                if (checkThreshold(_parameter, difference)) {
                    createDocument4Settle(_parameter, infoDoc, difference);
                }
            }
            if (InstanceUtils.isType(settleDocInst, CISales.CollectionOrder)) {
                Tag.untagObject(_parameter, settleDocInst, CISales.AccountabilityTag4CollectionOrder.getType());
            } else if (InstanceUtils.isType(settleDocInst, CISales.PaymentOrder)) {
                Tag.untagObject(_parameter, settleDocInst, CISales.AccountabilityTag4PaymentOrder.getType());
            }
        }
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
            name = new Naming().fromNumberGenerator(_parameter, CISales.CollectionOrder.getType().getName());
        } else {
            type = CISales.PaymentOrder;
            status = CISales.PaymentOrderStatus.Approved;
            name = new Naming().fromNumberGenerator(_parameter, CISales.PaymentOrder.getType().getName());
        }

        final Instance rateCurrInst = Instance.get(CIERP.Currency.getType(), (Long) _infoDoc.get("currency"));
        final PriceUtil util = new PriceUtil();
        final BigDecimal[] rates = util.getExchangeRate((DateTime) _infoDoc.get("date"), rateCurrInst);
        final CurrencyInst cur = new CurrencyInst(rateCurrInst);
        final Object[] rate = new Object[] { cur.isInvert() ? BigDecimal.ONE : rates[1], cur.isInvert() ? rates[1]
                        : BigDecimal.ONE };

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

            ret.append(DBProperties.getProperty(AbstractPaymentOut.class.getName() + ".generateDoc4Note.Label")).append(
                            ": ").append(_parameter.getCallInstance().getType().getLabel()).append(" - ").append(print
                                            .<String>getAttribute(CISales.PaymentDocumentIOAbstract.Name));
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
     * Gets the amounts4 render.
     *
     * @param _parameter the _parameter
     * @return the amounts4 render
     * @throws EFapsException the e faps exception
     */
    protected BigDecimal getAmount4SettleDocument(final Parameter _parameter)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        final String[] settleTotals = _parameter.getParameterValues("settleTotal");
        if (ArrayUtils.isNotEmpty(settleTotals)) {
            for (final String total : settleTotals) {
                try {
                    ret = ret.add((BigDecimal) NumberFormatter.get().getTwoDigitsFormatter().parse(total));
                } catch (final ParseException e) {
                    Settlement_Base.LOG.error("Catched", e);
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
    protected boolean checkThreshold(final Parameter _parameter,
                                      final BigDecimal _difference)
        throws EFapsException
    {
        boolean ret = false;

        final String defaultAmount = out ? Sales.PAYMENTOUT_THRESHOLD4CREATEDOC.get()
                        : Sales.PAYMENT_THRESHOLD4CREATEDOC.get();
        if (defaultAmount != null && !defaultAmount.isEmpty()) {
            final DecimalFormat fmtr = NumberFormatter.get().getFormatter();
            try {
                final BigDecimal amount = (BigDecimal) fmtr.parse(defaultAmount);
                if (_difference.abs().compareTo(amount) >= 0) {
                    ret = true;
                }
            } catch (final ParseException e) {
                Settlement_Base.LOG.error("Catched", e);
            }
        }
        return ret;
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
            multi2.addAttribute(CISales.TransactionAbstract.Account, CISales.TransactionAbstract.CurrencyId,
                            CISales.TransactionAbstract.Date);
            multi2.execute();

            if (multi2.next()) {
                final QueryBuilder queryBldr3 = new QueryBuilder(CISales.Balance);
                queryBldr3.addWhereAttrEqValue(CISales.Balance.Account, multi2.<Long>getAttribute(
                                CISales.TransactionAbstract.Account));
                queryBldr3.addWhereAttrEqValue(CISales.Balance.Currency, multi2.<Long>getAttribute(
                                CISales.TransactionAbstract.CurrencyId));
                final MultiPrintQuery multi3 = queryBldr3.getPrint();
                multi3.addAttribute(CISales.Balance.Amount);
                multi3.execute();

                if (multi3.next()) {
                    final BigDecimal balanceAmount = multi3.<BigDecimal>getAttribute(CISales.Balance.Amount);
                    final BigDecimal different;
                    if (InstanceUtils.isType(multi2.getCurrentInstance(), CISales.TransactionOutbound)) {
                        different = amountDoc4Render.subtract(_amountDoc4Replaced);
                    } else {
                        different = _amountDoc4Replaced.subtract(amountDoc4Render);
                    }

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

    /**
     * Gets the amounts2 payment.
     *
     * @param _parameter the _parameter
     * @param _docInst the _doc inst
     * @param _payment the _payment
     * @return the amounts2 payment
     * @throws EFapsException the e faps exception
     */
    protected BigDecimal getAmount4CurrentDocument(final Parameter _parameter,
                                                   final Instance _docInst,
                                                   final Instance _payment)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;

        final QueryBuilder queryBldr = new QueryBuilder(CISales.Payment);
        queryBldr.addWhereAttrEqValue(CISales.Payment.CreateDocument, _docInst);
        queryBldr.addWhereAttrEqValue(CISales.Payment.TargetDocument, _payment);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.Payment.Amount);
        multi.execute();
        while (multi.next()) {
            ret = ret.add(multi.<BigDecimal>getAttribute(CISales.Payment.Amount));
        }
        return ret;
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
        final Instance paymentInst = _parameter.getCallInstance();
        final Return ret = new Return();
        final StringBuilder error = new StringBuilder();
        final Instance docInstance = Instance.get(_parameter.getParameterValue("currentDocument"));
        if (docInstance.isValid()) {
            final BigDecimal amount = getAmount4CurrentDocument(_parameter, docInstance, paymentInst);
            final BigDecimal settleAmount = getAmount4SettleDocument(_parameter);
            if (amount.compareTo(settleAmount) == 0) {
                error.append(DBProperties.getProperty("org.efaps.esjp.sales.payment.AbstractPaymentOut.AmountsValid"));
                ret.put(ReturnValues.SNIPLETT, error.toString());
                ret.put(ReturnValues.TRUE, true);
            } else {
                final BigDecimal difference = amount.subtract(settleAmount);
                if (difference.signum() == 1) {
                    error.append(DBProperties.getProperty(
                                    "org.efaps.esjp.sales.payment.AbstractPaymentOut.AmountLess"));
                } else {
                    error.append(DBProperties.getProperty(
                                    "org.efaps.esjp.sales.payment.AbstractPaymentOut.AmountGreater"));
                }
                error.append(" ").append(difference.abs());
                ret.put(ReturnValues.TRUE, true);
                ret.put(ReturnValues.SNIPLETT, error.toString());
            }
        } else {
            error.append(DBProperties.getProperty("org.efaps.esjp.sales.payment.AbstractPaymentOut.SelectedDocument"));
            ret.put(ReturnValues.SNIPLETT, error.toString());
        }
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
        final List<Map<String, String>> list = new ArrayList<>();
        final Map<String, String> map = new HashMap<>();
        final int selected = getSelectedRow(_parameter);

        final Instance instDoc = Instance.get(_parameter.getParameterValue("currentDocument"));
        final Instance docInst = Instance.get(_parameter.getParameterValues("settleDocument")[selected]);
        if (docInst.isValid()) {
            final SelectBuilder selCur = new SelectBuilder().linkto(CISales.DocumentSumAbstract.RateCurrencyId)
                            .instance();

            final PrintQuery print = new PrintQuery(docInst);
            print.addAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
            print.addSelect(selCur);

            if (print.execute()) {
                if (docInst.getType().isKindOf(CISales.DocumentSumAbstract.getType())) {
                    final BigDecimal amount = print.getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
                    final BigDecimal cAmount = getAmount4CurrentDocument(_parameter, instDoc, _parameter
                                    .getCallInstance());

                    final CurrencyInst curr = new CurrencyInst(print.<Instance>getSelect(selCur));
                    final String valueCrossTotal = curr.getSymbol() + " " + NumberFormatter.get()
                                    .getTwoDigitsFormatter().format(amount);
                    map.put("crossTotal4Read", valueCrossTotal);
                    map.put("settleTotal", cAmount.compareTo(amount) > 0 ? NumberFormatter.get().getTwoDigitsFormatter()
                                    .format(amount) : NumberFormatter.get().getTwoDigitsFormatter().format(cAmount));
                    list.add(map);
                }
            }
        }
        final Return ret = new Return();
        ret.put(ReturnValues.VALUES, list);
        return ret;
    }
}
