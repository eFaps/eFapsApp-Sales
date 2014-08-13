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
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.StandartReport;
import org.efaps.esjp.erp.Naming;
import org.efaps.esjp.erp.NumberFormatter;
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
@EFapsUUID("d93f298b-f0bf-4278-a18e-b065cc330e50")
@EFapsRevision("$Rev$")
public abstract class PettyCashBalance_Base
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
        final Instance accInstance = _parameter.getCallInstance();

        final PrintQuery printAmount = new PrintQuery(accInstance);
        printAmount.addAttribute(CISales.AccountPettyCash.Name, CISales.AccountPettyCash.AmountAbstract);
        printAmount.execute();
        BigDecimal amount = printAmount.<BigDecimal>getAttribute(CISales.AccountAbstract.AmountAbstract);
        final String accName = printAmount.<String>getAttribute(CISales.AccountAbstract.Name);
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }

        final Instance balanceInst = createPettyCashBalanceDoc(_parameter);

        final CreatedDoc createdDoc = new CreatedDoc();
        createdDoc.addValue("AmountPettyCash", amount);
        createdDoc.addValue("AccName", accName);
        createdDoc.setInstance(balanceInst);

        ret.put(ReturnValues.VALUES, createReport(_parameter, createdDoc));
        ret.put(ReturnValues.TRUE, true);
        return ret;
    }

    @Override
    protected void add2Report(final Parameter _parameter,
                              final CreatedDoc _createdDoc,
                              final StandartReport _report)
        throws EFapsException
    {
        super.add2Report(_parameter, _createdDoc, _report);
        _report.getJrParameters().put("AccName", _createdDoc.getValue("AccName"));
        _report.getJrParameters().put("AmountPettyCash", _createdDoc.getValue("AmountPettyCash"));
    }

    /**
     * Internal method to create a PettyCashBalance.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return instance of new PettyCashBalance, null if not created
     * @throws EFapsException on error
     */
    protected Instance createPettyCashBalanceDoc(final Parameter _parameter)
        throws EFapsException
    {
        Instance ret = null;
        final Instance inst = _parameter.getCallInstance();
        final boolean withDateConf = Sales.getSysConfig()
                        .getAttributeValueAsBoolean("PettyCashBalance_CommandWithDate");

        final PrintQuery print = new PrintQuery(inst);
        print.addAttribute(CISales.AccountAbstract.CurrencyLink,
                        CISales.AccountPettyCash.AmountAbstract);
        print.execute();
        final Long curId = print.<Long>getAttribute(CISales.AccountAbstract.CurrencyLink);
        BigDecimal startAmountOrig = print.<BigDecimal>getAttribute(CISales.AccountPettyCash.AmountAbstract);
        if (startAmountOrig == null) {
            startAmountOrig = BigDecimal.ZERO;
        }
        final Account acc = new Account();

        final String startAmountStr = _parameter.getParameterValue("startAmount");
        final BigDecimal amount = acc.getAmountPayments(_parameter);

        final DecimalFormat formater = NumberFormatter.get().getTwoDigitsFormatter();
        BigDecimal startAmount = BigDecimal.ZERO;
        try {
            startAmount = (BigDecimal) formater.parse(startAmountStr);
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
            update.add(CISales.AccountPettyCash.AmountAbstract, startAmount);
            update.execute();
        }

        // only if there is a difference it will be executed
        if (difference.compareTo(BigDecimal.ZERO) != 0) {
            final Insert insert = new Insert(CISales.PettyCashBalance);
            insert.add(CISales.PettyCashBalance.Name, getDocName4Create(_parameter));
            insert.add(CISales.PettyCashBalance.Salesperson, Context.getThreadContext().getPersonId());
            if (withDateConf) {
                insert.add(CISales.PettyCashBalance.Date, _parameter.getParameterValue("date"));
            } else {
                insert.add(CISales.PettyCashBalance.Date, new DateTime());
            }
            insert.add(CISales.PettyCashBalance.Status, Status.find(CISales.PettyCashBalanceStatus.Open));
            insert.add(CISales.PettyCashBalance.CrossTotal, difference);
            insert.add(CISales.PettyCashBalance.NetTotal, BigDecimal.ZERO);
            insert.add(CISales.PettyCashBalance.DiscountTotal, BigDecimal.ZERO);
            insert.add(CISales.PettyCashBalance.RateCrossTotal, BigDecimal.ZERO);
            insert.add(CISales.PettyCashBalance.RateNetTotal, BigDecimal.ZERO);
            insert.add(CISales.PettyCashBalance.RateDiscountTotal, BigDecimal.ZERO);
            insert.add(CISales.PettyCashBalance.Rate, new Object[] { 1, 1 });
            insert.add(CISales.PettyCashBalance.RateCurrencyId, curId);
            insert.add(CISales.PettyCashBalance.CurrencyId, curId);
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
            final Insert relInsert = new Insert(CISales.AccountPettyCash2PettyCashBalance);
            relInsert.add(CISales.AccountPettyCash2PettyCashBalance.FromLink, _parameter.getInstance());
            relInsert.add(CISales.AccountPettyCash2PettyCashBalance.ToLink, balanceInst);
            relInsert.execute();

            // connect balance and receipts
            final MultiPrintQuery multi = new MultiPrintQuery(lstInst);
            final SelectBuilder sel = SelectBuilder.get().linkto(CISales.Payment.CreateDocument).instance();
            multi.addSelect(sel);
            multi.executeWithoutAccessCheck();
            while (multi.next()) {
                final Instance docInst = multi.<Instance>getSelect(sel);
                if (docInst != null && docInst.isValid()) {
                    if (docInst.getType().equals(CISales.PettyCashReceipt.getType())) {
                        final Insert rel2Insert = new Insert(CISales.PettyCashBalance2PettyCashReceipt);
                        rel2Insert.add(CISales.PettyCashBalance2PettyCashReceipt.FromLink, balanceInst);
                        rel2Insert.add(CISales.PettyCashBalance2PettyCashReceipt.ToLink, docInst);
                        rel2Insert.execute();

                        final Update update = new Update(docInst);
                        update.add(CISales.PettyCashReceipt.Status, Status.find(CISales.PettyCashReceiptStatus.Closed));
                        update.execute();
                    } else if (docInst.getType().equals(CISales.IncomingCreditNote.getType())) {
                        final Insert rel2Insert = new Insert(CISales.PettyCashBalance2IncomingCreditNote);
                        rel2Insert.add(CISales.PettyCashBalance2IncomingCreditNote.FromLink, balanceInst);
                        rel2Insert.add(CISales.PettyCashBalance2IncomingCreditNote.ToLink, docInst);
                        rel2Insert.execute();
                        final Update update = new Update(docInst);
                        update.add(CISales.IncomingCreditNote.Status,
                                        Status.find(CISales.IncomingCreditNoteStatus.Paid));
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
                        DBProperties.getProperty("org.efaps.esjp.sales.Accountg_Base.Transaction.CashDeskBalance"));
            transInsert.add(CISales.TransactionAbstract.Date, new DateTime());
            transInsert.execute();
        }
        return ret;
    }

    /**
     * Created document CollectionOrder/PaymentOrder.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return createDoc4Account(final Parameter _parameter)
        throws EFapsException
    {
        final SelectBuilder selAccInst = new SelectBuilder()
                        .linkfrom(CISales.AccountPettyCash2PettyCashBalance,
                                        CISales.AccountPettyCash2PettyCashBalance.ToLink)
                        .linkto(CISales.AccountPettyCash2PettyCashBalance.FromLink).instance();

        final PrintQuery print = new PrintQuery(_parameter.getInstance());
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

        final BigDecimal crossTotal = print.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.CrossTotal);
        final Instance accountInst = print.<Instance>getSelect(selAccInst);
        if (crossTotal.compareTo(BigDecimal.ZERO) < 0) {
            type = CISales.CollectionOrder.getType();
            name = new Naming().fromNumberGenerator(_parameter, type.getName());
            status = Status.find(CISales.CollectionOrderStatus.Open);
            relation = CISales.AccountPettyCash2CollectionOrder.getType();
            bal2orderType = CISales.PettyCashBalance2CollectionOrder.getType();
        } else if (crossTotal.compareTo(BigDecimal.ZERO) > 0) {
            type = CISales.PaymentOrder.getType();
            name = new Naming().fromNumberGenerator(_parameter, type.getName());
            status = Status.find(CISales.PaymentOrderStatus.Open);
            relation = CISales.AccountPettyCash2PaymentOrder.getType();
            bal2orderType = CISales.PettyCashBalance2PaymentOrder.getType();
        }
        if (type != null && accountInst != null && accountInst.isValid()) {
            final Insert insert = new Insert(type);
            insert.add(CISales.DocumentSumAbstract.Name, name);
            insert.add(CISales.DocumentSumAbstract.Date, new DateTime());
            insert.add(CISales.DocumentSumAbstract.DueDate, new DateTime());
            insert.add(CISales.DocumentSumAbstract.Salesperson, Context.getThreadContext().getPerson().getId());
            insert.add(CISales.DocumentSumAbstract.RateCrossTotal, crossTotal);
            insert.add(CISales.DocumentSumAbstract.RateNetTotal, crossTotal);
            insert.add(CISales.DocumentSumAbstract.RateDiscountTotal, BigDecimal.ZERO);
            insert.add(CISales.DocumentSumAbstract.CrossTotal, crossTotal);
            insert.add(CISales.DocumentSumAbstract.NetTotal, crossTotal);
            insert.add(CISales.DocumentSumAbstract.DiscountTotal, BigDecimal.ZERO);
            insert.add(CISales.DocumentSumAbstract.CurrencyId,
                            print.<Long>getAttribute(CISales.DocumentSumAbstract.CurrencyId));
            insert.add(CISales.DocumentSumAbstract.RateCurrencyId,
                            print.<Long>getAttribute(CISales.DocumentSumAbstract.RateCurrencyId));
            insert.add(CISales.DocumentSumAbstract.Rate,
                            print.<Object[]>getAttribute(CISales.DocumentSumAbstract.Rate));
            insert.add(CISales.DocumentSumAbstract.StatusAbstract, status);
            insert.execute();

            final Insert relInsert = new Insert(relation);
            relInsert.add(CISales.AccountPettyCash2Document.FromLinkAbstract, accountInst);
            relInsert.add(CISales.AccountPettyCash2Document.ToLinkAbstract, insert.getInstance());
            relInsert.execute();

            final Insert relInsert2 = new Insert(bal2orderType);
            relInsert2.add(CISales.PettyCashBalance2OrderAbstract.FromAbstractLink, _parameter.getInstance());
            relInsert2.add(CISales.PettyCashBalance2OrderAbstract.ToAbstractLink, insert.getInstance());
            relInsert2.execute();
        }
        return new Return();
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return with Access
     * @throws EFapsException on error
     */
    public Return accessCheck4AccountDoc(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        if (_parameter.getInstance() != null && _parameter.getInstance().isValid()) {
            final QueryBuilder queryBldr = new QueryBuilder(CISales.PettyCashBalance2OrderAbstract);
            queryBldr.addWhereAttrEqValue(CISales.PettyCashBalance2OrderAbstract.FromAbstractLink,
                            _parameter.getInstance());
            final InstanceQuery query = queryBldr.getQuery();
            query.execute();
            if (!query.next()) {
                ret.put(ReturnValues.TRUE, true);
            }
        }
        return ret;
    }

    /**
     * Method for verify a Petty Cash Balance.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return verify(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instance = _parameter.getInstance();
        final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.PettyCashBalance2PettyCashReceipt);
        attrQueryBldr.addType(CISales.PettyCashBalance2IncomingCreditNote);
        attrQueryBldr.addWhereAttrEqValue(CISales.Document2DocumentAbstract.FromAbstractLink, instance);
        final AttributeQuery attrQuery = attrQueryBldr
                        .getAttributeQuery(CISales.Document2DocumentAbstract.ToAbstractLink);

        final QueryBuilder queryBldr = new QueryBuilder(CISales.DocumentSumAbstract);
        queryBldr.addWhereAttrInQuery(CISales.DocumentSumAbstract.ID, attrQuery);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.DocumentSumAbstract.Contact);
        multi.execute();
        while (multi.next()) {
            final Object contactObj = multi.getAttribute(CISales.DocumentSumAbstract.Contact);
            final Instance docInst = multi.getCurrentInstance();

            final Update recUpdate = new Update(docInst);
            if (docInst.getType().isKindOf(CISales.PettyCashReceipt.getType())) {
                recUpdate.add(CISales.PettyCashReceipt.Status, Status.find(CISales.PettyCashReceiptStatus.Closed));
                if (contactObj != null) {
                    final Properties props = Sales.getSysConfig().getAttributeValueAsProperties(
                                    SalesSettings.INCOMINGINVOICESEQUENCE);
                    final NumberGenerator numgen = NumberGenerator.get(UUID.fromString(props.getProperty("UUID")));
                    if (numgen != null) {
                        final String revision = numgen.getNextVal();
                        recUpdate.add(CISales.PettyCashReceipt.Revision, revision);
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
        }

        final Update update = new Update(instance);
        update.add(CISales.PettyCashBalance.Status, Status.find(CISales.PettyCashBalanceStatus.Verified));
        update.execute();
        return new Return();
    }

}
