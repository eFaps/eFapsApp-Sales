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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.RateUI;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
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
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

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
     * {@inheritDoc}
     */
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

            final Set<String> names = new HashSet<String>();
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
    public Return dropDown4CreateDocuments(final Parameter _parameter)
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
                attrQueryBldr.addWhereAttrEqValue(CISales.Payment.TargetDocument, _parameter.getInstance().getId());
                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.Payment.CreateDocument);

                _queryBldr.addWhereAttrInQuery(CISales.DocumentSumAbstract.ID, attrQuery);

                final List<Long> excludes = excludeTypes4DropDownPaymentsRender(_parameter);
                if (!excludes.isEmpty()) {
                    _queryBldr.addWhereAttrNotEqValue(CISales.DocumentSumAbstract.Type, excludes.toArray());
                }
            }
        }.dropDownFieldValue(_parameter);
        return ret;
    }

    /**
     * Update fields4 filtered documents.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return updateFields4FilteredDocuments(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();
        final int selected = getSelectedRow(_parameter);

        final Instance docInst = Instance.get(_parameter.getParameterValues("createDocument")[selected]);
        if (docInst.isValid()) {
            final SelectBuilder selCur = new SelectBuilder()
                            .linkto(CISales.DocumentSumAbstract.RateCurrencyId).instance();

            final PrintQuery print = new PrintQuery(docInst);
            print.addAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
            print.addSelect(selCur);

            if (print.execute()) {
                if (docInst.getType().isKindOf(CISales.DocumentSumAbstract.getType())) {
                    final BigDecimal amount = print.getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);

                    final CurrencyInst curr = new CurrencyInst(print.<Instance>getSelect(selCur));
                    final String valueCrossTotal = curr.getSymbol() + " "
                                    + NumberFormatter.get().getTwoDigitsFormatter().format(amount);
                    map.put("crossTotal4Read", valueCrossTotal);
                    list.add(map);
                }
            }
        }

        final Return ret = new Return();
        ret.put(ReturnValues.VALUES, list);
        return ret;
    }

    /**
     * Update payable documents.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return updatePayableDocuments(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();

        final Instance instDoc = Instance.get(_parameter.getParameterValue("createExistDocument"));
        final BigDecimal amount = getAmounts2Payment(_parameter, instDoc, _parameter.getCallInstance());

        final String[] documents = _parameter.getParameterValues("createDocument");
        boolean first = true;
        if (documents != null && documents.length > 0) {
            final Map<String, Object> infoDoc = new HashMap<String, Object>();
            for (final String doc : documents) {
                final Instance document = Instance.get(doc);
                if (document.isValid()) {
                    final PrintQuery print = new PrintQuery(document);
                    print.addAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
                    print.execute();
                    final BigDecimal amount4Doc = print
                                    .<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
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
                }
            }
            if (amount.compareTo(getAmounts4Render(_parameter)) != 0) {
                final BigDecimal difference = amount.subtract(getAmounts4Render(_parameter));
                if (checkDifference(_parameter, difference)) {
                    createDocument(_parameter, infoDoc, difference);
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

        final String defaultAmount = Sales.PAYMENTAMOUNT4CREATEDDOC.get();
        if (defaultAmount != null && !defaultAmount.isEmpty()) {
            final DecimalFormat fmtr = NumberFormatter.get().getFormatter();
            try {
                final BigDecimal amount = (BigDecimal) fmtr.parse(defaultAmount);

                if (_difference.abs().compareTo(amount) >= 0) {
                    ret = true;
                }
            } catch (final ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
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
    protected void createDocument(final Parameter _parameter,
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
     * Method to autoComplete documents.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return with values.
     * @throws EFapsException on error.
     */
    public Return autoComplete4FilteredDocuments(final Parameter _parameter)
        throws EFapsException
    {
        final boolean filtered = "true".equalsIgnoreCase(_parameter.getParameterValue("filterDocuments"));
        final Instance docInst = Instance.get(_parameter.getParameterValue("createExistDocument"));

        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        if (!input.isEmpty()) {
            final List<Instance> lstDocs = new MultiPrint()
            {

                @Override
                protected void add2QueryBldr(final Parameter _parameter,
                                             final QueryBuilder _queryBldr)
                    throws EFapsException
                {
                    if (filtered) {
                        if (docInst.isValid()) {
                            final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Document2DocumentAbstract);
                            attrQueryBldr.addWhereAttrEqValue(CISales.Document2DocumentAbstract.FromAbstractLink,
                                            docInst);
                            final AttributeQuery attrQuery = attrQueryBldr
                                            .getAttributeQuery(CISales.Document2DocumentAbstract.ToAbstractLink);
                            _queryBldr.addWhereAttrInQuery(CISales.DocumentSumAbstract.ID, attrQuery);
                        }
                    }
                    _queryBldr.addWhereAttrMatchValue(CISales.DocumentSumAbstract.Name,
                                    input + "*").setIgnoreCase(true);
                };
            }.getInstances(_parameter);

            if (!lstDocs.isEmpty()) {
                final Map<String, Map<String, String>> tmpMap = new TreeMap<String, Map<String, String>>();
                final MultiPrintQuery multi = new MultiPrintQuery(lstDocs);
                multi.addAttribute(CISales.DocumentAbstract.Name,
                                CISales.DocumentAbstract.Date,
                                CISales.DocumentSumAbstract.RateCrossTotal);
                final SelectBuilder selCur = new SelectBuilder()
                                .linkto(CISales.DocumentSumAbstract.RateCurrencyId).instance();
                multi.addSelect(selCur);
                multi.execute();
                while (multi.next()) {
                    final String name = multi.<String>getAttribute(CISales.DocumentAbstract.Name);
                    final DateTime date = multi.<DateTime>getAttribute(CISales.DocumentAbstract.Date);

                    final StringBuilder choice = new StringBuilder();
                    choice.append(name).append(" - ").append(multi.getCurrentInstance().getType().getLabel())
                                    .append(" - ").append(date.toString(DateTimeFormat.forStyle("S-").withLocale(
                                                    Context.getThreadContext().getLocale())));

                    if (multi.getCurrentInstance().getType().isKindOf(CISales.DocumentSumAbstract.getType())) {
                        final BigDecimal amount = multi
                                        .<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
                        final CurrencyInst curr = new CurrencyInst(multi.<Instance>getSelect(selCur));
                        choice.append(" - ").append(curr.getSymbol()).append(" ")
                                        .append(NumberFormatter.get().getTwoDigitsFormatter().format(amount));
                    }

                    final Map<String, String> map = new HashMap<String, String>();
                    map.put(EFapsKey.AUTOCOMPLETE_KEY.getKey(), multi.getCurrentInstance().getOid());
                    map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), name);
                    map.put(EFapsKey.AUTOCOMPLETE_CHOICE.getKey(), choice.toString());
                    tmpMap.put(multi.getCurrentInstance().getOid(), map);
                }
                list.addAll(tmpMap.values());
            }
        }
        final Return ret = new Return();
        ret.put(ReturnValues.VALUES, list);
        return ret;
    }

    /**
     * Validate payments.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return validatePayments(final Parameter _parameter)
        throws EFapsException
    {
        final Instance paymentInst = _parameter.getCallInstance();
        final Return ret = new Return();
        final StringBuilder error = new StringBuilder();
        final Instance docInstance = Instance.get(_parameter.getParameterValue("createExistDocument"));
        if (docInstance.isValid()) {
            final BigDecimal amount = getAmounts2Payment(_parameter, docInstance, paymentInst);
            if (amount.compareTo(getAmounts4Render(_parameter)) == 0) {
                error.append(DBProperties
                                .getProperty("org.efaps.esjp.sales.payment.AbstractPaymentOut.AmountsValid"));
                ret.put(ReturnValues.SNIPLETT, error.toString());
                ret.put(ReturnValues.TRUE, true);
            } else {
                final BigDecimal difference = amount.subtract(getAmounts4Render(_parameter));
                if (difference.signum() == 1) {
                    error.append(DBProperties
                                    .getProperty("org.efaps.esjp.sales.payment.AbstractPaymentOut.AmountLess"));
                } else {
                    error.append(DBProperties
                                    .getProperty("org.efaps.esjp.sales.payment.AbstractPaymentOut.AmountGreater"));
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
     * Gets the amounts2 payment.
     *
     * @param _parameter the _parameter
     * @param _docInst the _doc inst
     * @param _payment the _payment
     * @return the amounts2 payment
     * @throws EFapsException the e faps exception
     */
    protected BigDecimal getAmounts2Payment(final Parameter _parameter,
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
     * Gets the amounts4 render.
     *
     * @param _parameter the _parameter
     * @return the amounts4 render
     * @throws EFapsException the e faps exception
     */
    protected BigDecimal getAmounts4Render(final Parameter _parameter)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        final String[] documents = _parameter.getParameterValues("createDocument");
        if (documents != null && documents.length > 0) {
            for (final String doc : documents) {
                final Instance docInst = Instance.get(doc);
                if (docInst.isValid()) {
                    final PrintQuery print = new PrintQuery(docInst);
                    print.addAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
                    print.execute();
                    ret = ret.add(print.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal));
                }
            }
        }
        return ret;
    }

    /**
     * Check4 doc legal payments.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return check4DocLegalPayments(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();

        final Instance paymentDoc = _parameter.getInstance();

        if (paymentDoc.isValid()) {
            final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Payment);
            attrQueryBldr.addWhereAttrEqValue(CISales.Payment.TargetDocument, paymentDoc);
            final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.Payment.CreateDocument);

            final QueryBuilder queryBldr = new QueryBuilder(CISales.DocumentSumAbstract);
            queryBldr.addWhereAttrInQuery(CISales.DocumentSumAbstract.ID, attrQuery);

            final List<Long> excludes = excludeTypes4DropDownPaymentsRender(_parameter);
            if (!excludes.isEmpty()) {
                queryBldr.addWhereAttrNotEqValue(CISales.DocumentSumAbstract.Type, excludes.toArray());
            }

            final InstanceQuery query = queryBldr.getQuery();
            query.execute();
            if (!query.getValues().isEmpty()) {
                ret.put(ReturnValues.TRUE, true);
            }
        }
        return ret;
    }

    /**
     * Exclude types4 drop down payments render.
     *
     * @param _parameter the _parameter
     * @return the list
     * @throws EFapsException the e faps exception
     */
    protected List<Long> excludeTypes4DropDownPaymentsRender(final Parameter _parameter)
        throws EFapsException
    {
        final List<Long> excludes = new ArrayList<Long>();

        final Map<Integer, String> excludeTypes = analyseProperty(_parameter, "ExcludeType");
        for (final Entry<Integer, String> exclude : excludeTypes.entrySet()) {
            final Type type = Type.get(exclude.getValue());
            if (type != null) {
                excludes.add(type.getId());
            }
        }

        return excludes;
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
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();
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
