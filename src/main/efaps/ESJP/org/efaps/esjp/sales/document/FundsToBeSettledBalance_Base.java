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


package org.efaps.esjp.sales.document;

import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.Listener;
import org.efaps.ci.CIType;
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
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.StandartReport;
import org.efaps.esjp.erp.Naming;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.erp.listener.IOnAction;
import org.efaps.esjp.sales.Account;
import org.efaps.esjp.sales.Account_Base;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.SalesSettings;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("4957283f-22c6-4b31-90be-948547309d13")
@EFapsRevision("$Rev$")
public abstract class FundsToBeSettledBalance_Base
    extends AbstractDocumentSum
{
    /**
     * Method for create a new petty Cash Balance.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Instance balanceInst = createBalanceDoc(_parameter);

        final CreatedDoc createdDoc = new CreatedDoc(balanceInst);

        final File file = createReport(_parameter, createdDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    @Override
    protected void add2Report(final Parameter _parameter,
                              final CreatedDoc _createdDoc,
                              final StandartReport _report)
        throws EFapsException
    {
        super.add2Report(_parameter, _createdDoc, _report);
        final PrintQuery printAmount = new PrintQuery(_parameter.getCallInstance());
        printAmount.addAttribute(CISales.AccountFundsToBeSettled.Name, CISales.AccountFundsToBeSettled.AmountAbstract);
        printAmount.execute();
        BigDecimal amount = printAmount.<BigDecimal>getAttribute(CISales.AccountAbstract.AmountAbstract);
        final String accName = printAmount.<String>getAttribute(CISales.AccountAbstract.Name);
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        _report.getJrParameters().put("AccName", accName);
        _report.getJrParameters().put("AmountPettyCash", amount);
    }

    @Override
    public File createReport(final Parameter _parameter,
                             final CreatedDoc _createdDoc)
        throws EFapsException
    {
        return super.createReport(_parameter, _createdDoc);
    }

    /**
     * Internal method to create a PettyCashBalance.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return instance of new PettyCashBalance, null if not created
     * @throws EFapsException on error
     */
    public Instance createBalanceDoc(final Parameter _parameter)
        throws EFapsException
    {
        Instance ret = null;
        final Instance inst = _parameter.getCallInstance();
        final boolean withDateConf = Sales.getSysConfig()
                        .getAttributeValueAsBoolean("FundsToBeSettledBalance_CommandWithDate");

        final PrintQuery print = new PrintQuery(inst);
        print.addAttribute(CISales.AccountAbstract.CurrencyLink,
                        CISales.AccountFundsToBeSettled.AmountAbstract);
        print.execute();
        final Long curId = print.<Long>getAttribute(CISales.AccountAbstract.CurrencyLink);
        BigDecimal startAmountOrig = print.<BigDecimal>getAttribute(CISales.AccountFundsToBeSettled.AmountAbstract);
        if (startAmountOrig == null) {
            startAmountOrig = BigDecimal.ZERO;
        }
        final Account acc = new Account();

        final String startAmountStr = _parameter.getParameterValue("startAmount");
        final BigDecimal amount = acc.getAmountPayments(_parameter);

        final DecimalFormat formater = NumberFormatter.get().getTwoDigitsFormatter();
        BigDecimal startAmount = BigDecimal.ZERO;
        try {
            if (startAmountStr == null) {
                startAmount = startAmountOrig;
            } else {
                startAmount = (BigDecimal) formater.parse(startAmountStr);
            }
        } catch (final ParseException e) {
            throw new EFapsException(Account_Base.class, "ParseException", e);
        }
        final BigDecimal difference;
        // the transactions sum to Zero
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            // check if it is the first balance
            if (acc.hasTransaction(_parameter)) {
                difference = startAmount.subtract(startAmountOrig);
            } else {
                difference = startAmount;
            }
        } else {
            difference = amount.negate().add(startAmount.subtract(startAmountOrig));
        }
        if (startAmount.compareTo(startAmountOrig) != 0) {
            final Update update = new Update(inst);
            update.add(CISales.AccountFundsToBeSettled.AmountAbstract, startAmount);
            update.execute();
        }

        // only if there is a difference it will be executed
        if (difference.compareTo(BigDecimal.ZERO) != 0) {
            final Insert insert = new Insert(CISales.FundsToBeSettledBalance);
            insert.add(CISales.FundsToBeSettledBalance.Name, getDocName4Create(_parameter));
            insert.add(CISales.FundsToBeSettledBalance.Salesperson, Context.getThreadContext().getPersonId());
            if (withDateConf) {
                insert.add(CISales.FundsToBeSettledBalance.Date, _parameter.getParameterValue("date"));
            } else {
                insert.add(CISales.FundsToBeSettledBalance.Date, new DateTime());
            }
            insert.add(CISales.FundsToBeSettledBalance.Status, Status.find(CISales.FundsToBeSettledBalanceStatus.Open));
            insert.add(CISales.FundsToBeSettledBalance.CrossTotal, difference);
            insert.add(CISales.FundsToBeSettledBalance.NetTotal, BigDecimal.ZERO);
            insert.add(CISales.FundsToBeSettledBalance.DiscountTotal, BigDecimal.ZERO);
            insert.add(CISales.FundsToBeSettledBalance.RateCrossTotal, BigDecimal.ZERO);
            insert.add(CISales.FundsToBeSettledBalance.RateNetTotal, BigDecimal.ZERO);
            insert.add(CISales.FundsToBeSettledBalance.RateDiscountTotal, BigDecimal.ZERO);
            insert.add(CISales.FundsToBeSettledBalance.Rate, new Object[] { 1, 1 });
            insert.add(CISales.FundsToBeSettledBalance.RateCurrencyId, curId);
            insert.add(CISales.FundsToBeSettledBalance.CurrencyId, curId);
            insert.execute();

            ret = insert.getInstance();

            final List<Instance> lstInst = new ArrayList<Instance>();
            if (withDateConf) {
                final String[] oids = (String[]) Context.getThreadContext().getSessionAttribute("paymentsOid");
                if (oids != null) {
                    for (int i = 0; i < oids.length; i++) {
                        final Instance instPay = Instance.get(oids[i]);
                        lstInst.add(instPay);
                    }
                } else {
                    lstInst.addAll(acc.getPayments(_parameter));
                }
            } else {
                lstInst.addAll(acc.getPayments(_parameter));
            }

            final Instance balanceInst = insert.getInstance();

            // connect account and balance
            final Insert relInsert = new Insert(CISales.AccountFundsToBeSettled2FundsToBeSettledBalance);
            relInsert.add(CISales.AccountFundsToBeSettled2FundsToBeSettledBalance.FromLink, _parameter.getInstance());
            relInsert.add(CISales.AccountFundsToBeSettled2FundsToBeSettledBalance.ToLink, balanceInst);
            relInsert.execute();

            // connect balance and receipts
            final MultiPrintQuery multi = new MultiPrintQuery(lstInst);
            final SelectBuilder sel = SelectBuilder.get().linkto(CISales.Payment.CreateDocument).instance();
            multi.addSelect(sel);
            multi.executeWithoutAccessCheck();
            while (multi.next()) {
                final Instance docInst = multi.<Instance>getSelect(sel);
                if (docInst != null && docInst.isValid()) {
                    Insert rel2Insert = null;
                    Status status = null;
                    if (docInst.getType().equals(CISales.FundsToBeSettledReceipt.getType())) {
                        rel2Insert = new Insert(CISales.FundsToBeSettledBalance2FundsToBeSettledReceipt);
                        status = Status.find(CISales.FundsToBeSettledReceiptStatus.Closed);
                    } else if (docInst.getType().equals(CISales.IncomingCreditNote.getType())) {
                        rel2Insert = new Insert(CISales.FundsToBeSettledBalance2IncomingCreditNote);
                        status = Status.find(CISales.IncomingCreditNoteStatus.Paid);
                    } else if (docInst.getType().equals(CISales.IncomingInvoice.getType())) {
                        rel2Insert = new Insert(CISales.FundsToBeSettledBalance2IncomingInvoice);
                        status = Status.find(CISales.IncomingInvoiceStatus.Paid);
                    }
                    if (rel2Insert != null && status != null) {
                        rel2Insert.add(CISales.Document2DocumentAbstract.FromAbstractLink, balanceInst);
                        rel2Insert.add(CISales.Document2DocumentAbstract.ToAbstractLink, docInst);
                        rel2Insert.execute();

                        final Update update = new Update(docInst);
                        update.add(CISales.DocumentSumAbstract.StatusAbstract, status);
                        update.execute();
                    }
                }
            }

            for (final Instance instance : lstInst) {
                final Update update = new Update(instance);
                update.add(CISales.Payment.TargetDocument, balanceInst);
                update.execute();
            }

            final Insert payInsert = new Insert(CISales.Payment);
            payInsert.add(CISales.Payment.Date, new DateTime());
            payInsert.add(CISales.Payment.CreateDocument, balanceInst);
            payInsert.add(CISales.Payment.TargetDocument, balanceInst);
            payInsert.execute();

            CIType type;
            if (difference.compareTo(BigDecimal.ZERO) < 0) {
                type = CISales.TransactionOutbound;
            } else {
                type = CISales.TransactionInbound;
            }

            final Insert transInsert = new Insert(type);
            transInsert.add(CISales.TransactionAbstract.Amount, difference.abs());
            transInsert.add(CISales.TransactionAbstract.CurrencyId, curId);
            transInsert.add(CISales.TransactionAbstract.Payment,  payInsert.getInstance().getId());
            transInsert.add(CISales.TransactionAbstract.Account, inst.getId());
            transInsert.add(CISales.TransactionAbstract.Description,
                        DBProperties.getProperty("org.efaps.esjp.sales.Account_Base.Transaction.FundsToBeSettledBalance"));
            transInsert.add(CISales.TransactionAbstract.Date, new DateTime());
            transInsert.execute();
        }
        return ret;
    }

    @Override
    protected String getDocName4Create(final Parameter _parameter)
        throws EFapsException
    {
        return new DateTime().toLocalTime().toString();
    }

    public Return createDoc4Account(final Parameter _parameter)
        throws EFapsException
    {
        createDoc4Account(_parameter, _parameter.getInstance());
        return new Return();
    }

    /**
     * Created document CollectionOrder/PaymentOrder.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @param _docInst  Instance of the Document
     * @throws EFapsException on error.
     */
    public void createDoc4Account(final Parameter _parameter,
                                  final Instance _docInst)
        throws EFapsException
    {
        final SelectBuilder selAccInst = new SelectBuilder()
                        .linkfrom(CISales.AccountFundsToBeSettled2FundsToBeSettledBalance.ToLink)
                        .linkto(CISales.AccountFundsToBeSettled2FundsToBeSettledBalance.FromLink).instance();

        final PrintQuery print = new PrintQuery(_docInst);
        print.addAttribute(CISales.DocumentSumAbstract.Rate,
                        CISales.DocumentSumAbstract.CurrencyId,
                        CISales.DocumentSumAbstract.RateCurrencyId,
                        CISales.DocumentSumAbstract.CrossTotal);
        print.addSelect(selAccInst);
        print.execute();

        Type type = null;
        Type relation = null;
        Type bal2orderType = null;
        String name = null;
        Status status = null;
        CIType actionRelType = null;
        final BigDecimal crossTotal = print.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.CrossTotal);
        final Instance accountInst = print.<Instance>getSelect(selAccInst);
        if (crossTotal.compareTo(BigDecimal.ZERO) < 0) {
            type = CISales.CollectionOrder.getType();
            name = new Naming().fromNumberGenerator(_parameter, type.getName());
            status = Status.find(CISales.CollectionOrderStatus.Open);
            relation = CISales.AccountFundsToBeSettled2CollectionOrder.getType();
            bal2orderType = CISales.FundsToBeSettledBalance2CollectionOrder.getType();
            actionRelType = CISales.ActionDefinitionCollectionOrder2Document;
        } else if (crossTotal.compareTo(BigDecimal.ZERO) > 0) {
            type = CISales.PaymentOrder.getType();
            name = new Naming().fromNumberGenerator(_parameter, type.getName());
            status = Status.find(CISales.PaymentOrderStatus.Open);
            relation = CISales.AccountFundsToBeSettled2PaymentOrder.getType();
            bal2orderType = CISales.FundsToBeSettledBalance2PaymentOrder.getType();
            actionRelType = CISales.ActionDefinitionPaymentOrder2Document;
        }
        if (type != null && accountInst != null && accountInst.isValid()) {
            final Insert insert = new Insert(type);
            insert.add(CISales.DocumentSumAbstract.Name, name);
            insert.add(CISales.DocumentSumAbstract.Date, new DateTime());
            insert.add(CISales.DocumentSumAbstract.DueDate, new DateTime());
            insert.add(CISales.DocumentSumAbstract.Salesperson, Context.getThreadContext().getPerson().getId());
            insert.add(CISales.DocumentSumAbstract.RateCrossTotal, crossTotal.abs());
            insert.add(CISales.DocumentSumAbstract.RateNetTotal, crossTotal.abs());
            insert.add(CISales.DocumentSumAbstract.RateDiscountTotal, BigDecimal.ZERO);
            insert.add(CISales.DocumentSumAbstract.CrossTotal, crossTotal.abs());
            insert.add(CISales.DocumentSumAbstract.NetTotal, crossTotal.abs());
            insert.add(CISales.DocumentSumAbstract.DiscountTotal, BigDecimal.ZERO);
            insert.add(CISales.DocumentSumAbstract.CurrencyId,
                            print.<Long>getAttribute(CISales.DocumentSumAbstract.CurrencyId));
            insert.add(CISales.DocumentSumAbstract.RateCurrencyId,
                            print.<Long>getAttribute(CISales.DocumentSumAbstract.RateCurrencyId));
            insert.add(CISales.DocumentSumAbstract.Rate,
                            print.<Object[]>getAttribute(CISales.DocumentSumAbstract.Rate));
            insert.add(CISales.DocumentSumAbstract.StatusAbstract, status);
            insert.add(CISales.DocumentSumAbstract.Note,
                            _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                                            CISales.DocumentSumAbstract.Note.name)));
            insert.execute();

            final Insert relInsert = new Insert(relation);
            relInsert.add(CISales.AccountFundsToBeSettled2Document.FromLinkAbstract, accountInst);
            relInsert.add(CISales.AccountFundsToBeSettled2Document.ToLinkAbstract, insert.getInstance());
            relInsert.execute();

            final Insert relInsert2 = new Insert(bal2orderType);
            relInsert2.add(CISales.FundsToBeSettledBalance2OrderAbstract.FromAbstractLink, _docInst);
            relInsert2.add(CISales.FundsToBeSettledBalance2OrderAbstract.ToAbstractLink, insert.getInstance());
            relInsert2.execute();

            final Instance actionInst = Instance.get(_parameter.getParameterValue("action"));
            if (actionInst.isValid()) {
                final Insert actionRelInsert = new Insert(actionRelType);
                actionRelInsert.add(CIERP.ActionDefinition2DocumentAbstract.FromLinkAbstract, actionInst);
                actionRelInsert.add(CIERP.ActionDefinition2DocumentAbstract.ToLinkAbstract, insert.getInstance());
                actionRelInsert.execute();
            }
        }
    }


    /**
     * Method for verify a FundsToBeSettledBalance.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return verify(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instance = _parameter.getInstance();
        final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.FundsToBeSettledBalance2FundsToBeSettledReceipt);
        attrQueryBldr.addType(CISales.FundsToBeSettledBalance2IncomingCreditNote);
        attrQueryBldr.addWhereAttrEqValue(CISales.Document2DocumentAbstract.FromAbstractLink, instance);
        final AttributeQuery attrQuery = attrQueryBldr
                        .getAttributeQuery(CISales.Document2DocumentAbstract.ToAbstractLink);

        final QueryBuilder queryBldr = new QueryBuilder(CISales.DocumentSumAbstract);
        queryBldr.addWhereAttrInQuery(CISales.DocumentSumAbstract.ID, attrQuery);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.DocumentSumAbstract.Contact);
        multi.execute();
        while (multi.next()) {
            final Object contactObj = multi.getAttribute(CISales.FundsToBeSettledReceipt.Contact);
            final Instance docInst = multi.getCurrentInstance();

            final Update recUpdate = new Update(docInst);
            if (docInst.getType().isKindOf(CISales.FundsToBeSettledReceipt.getType())) {
                recUpdate.add(CISales.FundsToBeSettledReceipt.Status,
                                Status.find(CISales.FundsToBeSettledReceiptStatus.Closed));
                if (contactObj != null) {
                    final Properties props = Sales.getSysConfig().getAttributeValueAsProperties(
                                    SalesSettings.INCOMINGINVOICESEQUENCE);
                    final NumberGenerator numgen = NumberGenerator.get(UUID.fromString(props.getProperty("UUID")));
                    if (numgen != null) {
                        final String revision = numgen.getNextVal();
                        recUpdate.add(CISales.FundsToBeSettledReceipt.Revision, revision);
                    }
                }
            } else {
                recUpdate.add(CISales.IncomingCreditNote.Status, Status.find(CISales.IncomingCreditNoteStatus.Paid));
                final Properties props = Sales.getSysConfig().getAttributeValueAsProperties(
                                SalesSettings.INCOMINGCREDITNOTESEQUENCE);
                final NumberGenerator numgen = NumberGenerator.get(UUID.fromString(props.getProperty("UUID")));
                if (numgen != null) {
                    final String revision = numgen.getNextVal();
                    recUpdate.add(CISales.IncomingCreditNote.Revision, revision);
                }
            }
            recUpdate.execute();
            for (final IOnAction listener : Listener.get().<IOnAction>invoke(IOnAction.class)) {
                listener.onDocumentUpdate(_parameter, docInst);
            }
        }

        final Update update = new Update(instance);
        update.add(CISales.FundsToBeSettledBalance.Status, Status.find(CISales.FundsToBeSettledBalanceStatus.Verified));
        update.execute();
        return new Return();
    }

}
