/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
 */
package org.efaps.esjp.sales.document;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.Listener;
import org.efaps.ci.CIType;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Delete;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.StandartReport_Base.JasperActivation;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.Naming;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.erp.listener.IOnAction;
import org.efaps.esjp.sales.Account;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author The eFaps Team
 */
@EFapsUUID("d93f298b-f0bf-4278-a18e-b065cc330e50")
@EFapsApplication("eFapsApp-Sales")
public abstract class PettyCashBalance_Base
    extends AbstractDocumentSum
{

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PettyCashBalance.class);

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
        final Instance balanceInst = createPettyCashBalanceDoc(_parameter);

        final CreatedDoc createdDoc = new CreatedDoc();
        createdDoc.setInstance(balanceInst);

        ret.put(ReturnValues.VALUES, createReport(_parameter, createdDoc));
        ret.put(ReturnValues.TRUE, true);
        return ret;
    }

    /**
     * Create the Report again.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    @Override
    public Return createReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final CreatedDoc createdDoc = new CreatedDoc();
        createdDoc.setInstance(_parameter.getInstance());

        ret.put(ReturnValues.VALUES, createReport(_parameter, createdDoc));
        ret.put(ReturnValues.TRUE, true);
        return ret;
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

        final String startAmountStr = _parameter.getParameterValue(
                        CIFormSales.Sales_AccountPettyCashBalancingWithDateForm.startAmount.name);
        final BigDecimal amount = acc.getAmountPayments(_parameter);

        final DecimalFormat formater = NumberFormatter.get().getTwoDigitsFormatter();
        BigDecimal startAmount = BigDecimal.ZERO;
        try {
            startAmount = (BigDecimal) formater.parse(startAmountStr);
        } catch (final ParseException e) {
            PettyCashBalance_Base.LOG.error("Catched parsing error", e);
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
            if (_parameter.getParameterValue("date") != null) {
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

            final List<Instance> lstInst = new ArrayList<>();

            if (Context.getThreadContext().containsSessionAttribute(
                            CIFormSales.Sales_AccountPettyCashBalancingWithDateForm.paymentsOIDs.name)) {
                final String[] oids = (String[]) Context.getThreadContext().getSessionAttribute(
                                            CIFormSales.Sales_AccountPettyCashBalancingWithDateForm.paymentsOIDs.name);
                if (oids != null) {
                    for (final String oid : oids) {
                        final Instance instPay = Instance.get(oid);
                        lstInst.add(instPay);
                    }
                } else {
                    lstInst.addAll(acc.getPayments(_parameter));
                }
                Context.getThreadContext().removeSessionAttribute(
                                CIFormSales.Sales_AccountPettyCashBalancingWithDateForm.paymentsOIDs.name);
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
                    if (InstanceUtils.isType(docInst, CISales.PettyCashReceipt)) {
                        final Insert rel2Insert = new Insert(CISales.PettyCashBalance2PettyCashReceipt);
                        rel2Insert.add(CISales.PettyCashBalance2PettyCashReceipt.FromLink, balanceInst);
                        rel2Insert.add(CISales.PettyCashBalance2PettyCashReceipt.ToLink, docInst);
                        rel2Insert.execute();

                        final Update update = new Update(docInst);
                        update.add(CISales.PettyCashReceipt.Status, Status.find(CISales.PettyCashReceiptStatus.Closed));
                        update.execute();
                    } else if (InstanceUtils.isType(docInst, CISales.IncomingCreditNote)) {
                        final Insert rel2Insert = new Insert(CISales.PettyCashBalance2IncomingCreditNote);
                        rel2Insert.add(CISales.PettyCashBalance2IncomingCreditNote.FromLink, balanceInst);
                        rel2Insert.add(CISales.PettyCashBalance2IncomingCreditNote.ToLink, docInst);
                        rel2Insert.execute();
                        final Update update = new Update(docInst);
                        update.add(CISales.IncomingCreditNote.Status,
                                        Status.find(CISales.IncomingCreditNoteStatus.Paid));
                        update.execute();
                    }  else if (InstanceUtils.isType(docInst, CISales.IncomingInvoice)) {
                        final Insert rel2Insert = new Insert(CISales.PettyCashBalance2IncomingInvoice);
                        rel2Insert.add(CISales.PettyCashBalance2IncomingInvoice.FromLink, balanceInst);
                        rel2Insert.add(CISales.PettyCashBalance2IncomingInvoice.ToLink, docInst);
                        rel2Insert.execute();
                        final Update update = new Update(docInst);
                        update.add(CISales.IncomingInvoice.Status, Status.find(CISales.IncomingInvoiceStatus.Paid));
                        update.execute();
                    } else if (InstanceUtils.isType(docInst, CISales.IncomingReceipt)) {
                        final Insert rel2Insert = new Insert(CISales.PettyCashBalance2IncomingReceipt);
                        rel2Insert.add(CISales.PettyCashBalance2IncomingReceipt.FromLink, balanceInst);
                        rel2Insert.add(CISales.PettyCashBalance2IncomingReceipt.ToLink, docInst);
                        rel2Insert.execute();
                        final Update update = new Update(docInst);
                        update.add(CISales.IncomingReceipt.Status, Status.find(CISales.IncomingReceiptStatus.Paid));
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

            if ( Sales.PETTYCASH_LATEBALANCETRANS.get()) {
                payInsert.add(CISales.Payment.AccountLink, inst.getId());
                payInsert.add(CISales.Payment.CurrencyLink, curId);
                payInsert.add(CISales.Payment.Amount, difference.abs());
                payInsert.add(CISales.Payment.Status, Status.find(CISales.PaymentStatus.Pending));
            } else {
                payInsert.add(CISales.Payment.Status, Status.find(CISales.PaymentStatus.Executed));
            }
            payInsert.execute();

            if (!Sales.PETTYCASH_LATEBALANCETRANS.get()) {
                final CIType type;
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
                transInsert.add(CISales.TransactionAbstract.Description, getDBProperty("transaction.Create"));
                transInsert.add(CISales.TransactionAbstract.Date, new DateTime());
                transInsert.execute();
            }
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
        Instance actDefInst = null;
        CIType actDef2doc = null;
        final BigDecimal crossTotal = print.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.CrossTotal);
        final Instance accountInst = print.<Instance>getSelect(selAccInst);
        CurrencyInst.get(print.<Long>getAttribute(
                        CISales.DocumentSumAbstract.RateCurrencyId));
        Instance contactInst = null;
        if (crossTotal.compareTo(BigDecimal.ZERO) < 0) {
            type = CISales.CollectionOrder.getType();
            name = new Naming().fromNumberGenerator(_parameter, type.getName());
            status = Status.find(CISales.CollectionOrderStatus.Open);
            relation = CISales.AccountPettyCash2CollectionOrder.getType();
            bal2orderType = CISales.PettyCashBalance2CollectionOrder.getType();
            actDefInst = Sales.PETTYCASHBAL_ACTDEF4COLORD.get();
            actDef2doc = CISales.ActionDefinitionCollectionOrder2Document;
            contactInst = Sales.PETTYCASHBAL_CONTACT4COLORD.get();
        } else if (crossTotal.compareTo(BigDecimal.ZERO) > 0) {
            type = CISales.PaymentOrder.getType();
            name = new Naming().fromNumberGenerator(_parameter, type.getName());
            status = Status.find(CISales.PaymentOrderStatus.Open);
            relation = CISales.AccountPettyCash2PaymentOrder.getType();
            bal2orderType = CISales.PettyCashBalance2PaymentOrder.getType();
            actDefInst = Sales.PETTYCASHBAL_ACTDEF4PAYORD.get();
            actDef2doc = CISales.ActionDefinitionPaymentOrder2Document;
            contactInst = Sales.PETTYCASHBAL_CONTACT4PAYORD.get();
        }
        //if set via UI -> overwrite configurations
        if (_parameter.getParameterValue("contact") != null) {
            final Instance contactInstTmp = Instance.get(_parameter.getParameterValue("contact"));
            if (InstanceUtils.isValid(contactInstTmp)) {
                contactInst = contactInstTmp;
            }
        }
        if (type != null && accountInst != null && accountInst.isValid()) {
            final Insert insert = new Insert(type);
            if (InstanceUtils.isValid(contactInst)) {
                insert.add(CISales.DocumentSumAbstract.Contact, contactInst);
            }
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
            relInsert.add(CISales.AccountPettyCash2Document.FromLinkAbstract, accountInst);
            relInsert.add(CISales.AccountPettyCash2Document.ToLinkAbstract, insert.getInstance());
            relInsert.execute();

            final Insert relInsert2 = new Insert(bal2orderType);
            relInsert2.add(CISales.PettyCashBalance2OrderAbstract.FromAbstractLink, _parameter.getInstance());
            relInsert2.add(CISales.PettyCashBalance2OrderAbstract.ToAbstractLink, insert.getInstance());
            relInsert2.execute();

            if (actDefInst.isValid()) {
                final Insert actDef2DocInsert = new Insert(actDef2doc);
                actDef2DocInsert.add(CIERP.ActionDefinition2DocumentAbstract.FromLinkAbstract, actDefInst);
                actDef2DocInsert.add(CIERP.ActionDefinition2DocumentAbstract.ToLinkAbstract, insert.getInstance());
                actDef2DocInsert.execute();
            } else {
                PettyCashBalance_Base.LOG.error("Missing or wrong Configuration Links for ActionDefinition: '{}', '{}'",
                                Sales.PETTYCASHBAL_ACTDEF4COLORD.getKey(), Sales.PETTYCASHBAL_ACTDEF4PAYORD.getKey());
            }

            if (CISales.PaymentOrder.getType().equals(type)
                            && Sales.PAYMENTORDER_JASPERACTIVATION.get().contains(JasperActivation.ONCREATE)) {
                final CreatedDoc createdDoc = new CreatedDoc();
                createdDoc.setInstance(insert.getInstance());
                final Parameter parameter = ParameterUtil.clone(_parameter);
                ParameterUtil.setProperty(parameter, "JasperConfig", Sales.SYSCONFUUID.toString());
                ParameterUtil.setProperty(parameter, "JasperConfigReport", Sales.PAYMENTORDER_JASPERREPORT.getKey());
                ParameterUtil.setProperty(parameter, "JasperConfigMime", Sales.PAYMENTORDER_MIME.getKey());
                createReport(parameter, createdDoc);
            }

            if (CISales.CollectionOrder.getType().equals(type)
                            && Sales.COLLECTIONORDER_JASPERACTIVATION.get().contains(JasperActivation.ONCREATE)) {
                final CreatedDoc createdDoc = new CreatedDoc();
                createdDoc.setInstance(insert.getInstance());
                final Parameter parameter = ParameterUtil.clone(_parameter);
                ParameterUtil.setProperty(parameter, "JasperConfig", Sales.SYSCONFUUID.toString());
                ParameterUtil.setProperty(parameter, "JasperConfigReport", Sales.COLLECTIONORDER_JASPERREPORT.getKey());
                ParameterUtil.setProperty(parameter, "JasperConfigMime", Sales.COLLECTIONORDER_MIME.getKey());
                createReport(parameter, createdDoc);
            }
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
            query.executeWithoutAccessCheck();
            if (!query.next()) {
                if (Sales.PETTYCASHBAL_REQUIREBOOKED4PAY.get()) {
                    final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.PettyCashReceipt);
                    attrQueryBldr.addWhereAttrNotEqValue(CISales.PettyCashReceipt.Status,
                                    Status.find(CISales.PettyCashReceiptStatus.Canceled));

                    final QueryBuilder chQueryBldr = new QueryBuilder(CISales.PettyCashBalance2PettyCashReceipt);
                    chQueryBldr.addWhereAttrEqValue(CISales.Document2DocumentAbstract.FromAbstractLink,
                                    _parameter.getInstance());
                    chQueryBldr.addWhereAttrInQuery(CISales.Document2DocumentAbstract.ToAbstractLink,
                                    attrQueryBldr.getAttributeQuery(CISales.PettyCashReceipt.ID));
                    final InstanceQuery chQuery = chQueryBldr.getQuery();
                    chQuery.executeWithoutAccessCheck();
                    if (!chQuery.next()) {
                        ret.put(ReturnValues.TRUE, true);
                    }
                } else {
                    ret.put(ReturnValues.TRUE, true);
                }
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
                    // legal documents
                    final String seqKey = Sales.PETTYCASHRECEIPT_REVSEQ.get();
                    final NumberGenerator numgen = isUUID(seqKey)
                                    ? NumberGenerator.get(UUID.fromString(seqKey))
                                    : NumberGenerator.get(seqKey);
                    if (numgen != null) {
                        final String revision = numgen.getNextVal();
                        recUpdate.add(CISales.PettyCashReceipt.Revision, revision);
                    }
                }
            } else {
                recUpdate.add(CISales.IncomingCreditNote.Status, Status.find(CISales.IncomingCreditNoteStatus.Paid));
                final String seqKey = Sales.INCOMINGCREDITNOTE_REVSEQ.get();
                final NumberGenerator numgen = isUUID(seqKey)
                                ? NumberGenerator.get(UUID.fromString(seqKey))
                                : NumberGenerator.get(seqKey);
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
        update.add(CISales.PettyCashBalance.Status, Status.find(CISales.PettyCashBalanceStatus.Verified));
        update.execute();
        return new Return();
    }

    /**
     * Cancel.
     *
     * @param _parameter the parameter
     * @return the return
     * @throws EFapsException the eFaps exception
     */
    public Return cancel(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instance = _parameter.getInstance();

        final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Payment);
        attrQueryBldr.addWhereAttrEqValue(CISales.Payment.CreateDocument, instance);
        attrQueryBldr.addWhereAttrEqValue(CISales.Payment.TargetDocument, instance);

        final QueryBuilder transQueryBldr = new QueryBuilder(CISales.TransactionAbstract);
        transQueryBldr.addWhereAttrInQuery(CISales.TransactionAbstract.Payment, attrQueryBldr.getAttributeQuery(
                        CISales.Payment.ID));
        final MultiPrintQuery multi = transQueryBldr.getPrint();
        multi.addAttribute(CISales.TransactionAbstract.Amount,
                        CISales.TransactionAbstract.CurrencyId,
                        CISales.TransactionAbstract.Payment,
                        CISales.TransactionAbstract.Account,
                        CISales.TransactionAbstract.Date);
        multi.execute();
        while (multi.next()) {
            final CIType type;
            if (InstanceUtils.isKindOf(multi.getCurrentInstance(), CISales.TransactionOutbound)) {
                type = CISales.TransactionInbound;
            } else {
                type = CISales.TransactionOutbound;
            }
            final Insert transInsert = new Insert(type);
            transInsert.add(CISales.TransactionAbstract.Amount, multi.<BigDecimal>getAttribute(
                            CISales.TransactionAbstract.Amount));
            transInsert.add(CISales.TransactionAbstract.CurrencyId, multi.<Long>getAttribute(
                            CISales.TransactionAbstract.CurrencyId));
            transInsert.add(CISales.TransactionAbstract.Payment, multi.<Long>getAttribute(
                            CISales.TransactionAbstract.Payment));
            transInsert.add(CISales.TransactionAbstract.Account, multi.<Long>getAttribute(
                            CISales.TransactionAbstract.Account));
            transInsert.add(CISales.TransactionAbstract.Description, getDBProperty("transaction.Cancel"));
            transInsert.add(CISales.TransactionAbstract.Date, multi.<DateTime>getAttribute(
                            CISales.TransactionAbstract.Date));
            transInsert.execute();
        }

        final QueryBuilder paymentQueryBldr = new QueryBuilder(CISales.Payment);
        paymentQueryBldr.addWhereAttrEqValue(CISales.Payment.TargetDocument, instance);
        final MultiPrintQuery paymentMulti = paymentQueryBldr.getPrint();
        final SelectBuilder sel = SelectBuilder.get().linkto(CISales.Payment.CreateDocument).instance();
        paymentMulti.addSelect(sel);
        paymentMulti.execute();
        while (paymentMulti.next()) {
            final Instance docInst = paymentMulti.getSelect(sel);
            if (InstanceUtils.isKindOf(docInst, CISales.PettyCashReceipt)
                            || InstanceUtils.isKindOf(docInst, CISales.IncomingCreditNote)) {
                final Update update = new Update(paymentMulti.getCurrentInstance());
                update.add(CISales.Payment.TargetDocument, (Object) null);
                update.execute();
            }
        }

        final QueryBuilder queryBldr = new QueryBuilder(CISales.PettyCashBalance2PettyCashReceipt);
        queryBldr.addType(CISales.PettyCashBalance2IncomingCreditNote);
        queryBldr.addWhereAttrEqValue(CISales.Document2DocumentAbstract.FromAbstractLink, instance);
        final InstanceQuery query = queryBldr.getQuery();
        query.execute();
        while (query.next()) {
            new Delete(query.getCurrentValue()).execute();
        }

        final Update update = new Update(instance);
        update.add(CISales.PettyCashBalance.Status, Status.find(CISales.PettyCashBalanceStatus.Canceled));
        update.execute();
        return new Return();
    }
}
