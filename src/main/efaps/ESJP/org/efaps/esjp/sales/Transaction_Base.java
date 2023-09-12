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

package org.efaps.esjp.sales;

import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.IUIValue;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.admin.ui.field.Field;
import org.efaps.admin.ui.field.Field.Display;
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
import org.efaps.esjp.common.file.FileUtil;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.RateInfo;
import org.efaps.esjp.sales.payment.AbstractPaymentDocument;
import org.efaps.esjp.sales.payment.DocumentUpdate;
import org.efaps.esjp.sales.payment.TransferDocument;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.ui.html.Table;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("417d2eff-b3ab-4f1a-91f4-c75da34570f6")
@EFapsApplication("eFapsApp-Sales")
public abstract class Transaction_Base
    extends CommonDocument
{
    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Transaction.class);

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new empty Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final List<Instance> transInstances = new ArrayList<>();
        transInstances.add((Instance) new Create().execute(_parameter).get(ReturnValues.INSTANCE));
        final File file = createTransferDocument(_parameter, transInstances, false);
        final Return ret = new Return();
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    /** Creates the transaction for payment.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the instance
     * @throws EFapsException on error
     */
    public Return createTransaction4PaymentDocument(final Parameter _parameter)
        throws EFapsException
    {
        List<Instance> instances = new ArrayList<>();
        if (InstanceUtils.isKindOf(_parameter.getInstance(), CISales.PaymentDocumentIOAbstract)) {
            instances.add(_parameter.getInstance());
        } else {
            instances = getSelectedInstances(_parameter);
        }
        for (final Instance inst : instances) {
            final EditedDoc editedDoc = new EditedDoc(inst);
            final PrintQuery print = new PrintQuery(inst);
            print.addAttribute(CISales.PaymentDocumentIOAbstract.RateCurrencyLink,
                            CISales.PaymentDocumentAbstract.Date);
            print.executeWithoutAccessCheck();
            editedDoc.getValues().put(CISales.PaymentDocumentAbstract.RateCurrencyLink.name,
                            print.getAttribute(CISales.PaymentDocumentIOAbstract.RateCurrencyLink));
            editedDoc.getValues().put(CISales.PaymentDocumentAbstract.Date.name,
                            print.getAttribute(CISales.PaymentDocumentIOAbstract.Date));

            final QueryBuilder queryBldr = new QueryBuilder(CISales.Payment);
            queryBldr.addWhereAttrEqValue(CISales.Payment.TargetDocument, inst);
            queryBldr.addWhereAttrEqValue(CISales.Payment.Status, Status.find(CISales.PaymentStatus.Pending));
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CISales.Payment.Amount, CISales.Payment.AccountLink);
            multi.executeWithoutAccessCheck();
            while (multi.next()) {
                editedDoc.getValues().put(CISales.TransactionAbstract.Account.name,
                                multi.getAttribute(CISales.Payment.AccountLink));
                final Update update = new Update(multi.getCurrentInstance());
                update.add(CISales.Payment.Status, Status.find(CISales.PaymentStatus.Executed));
                update.add(CISales.Payment.AccountLink, (Object) null);
                update.execute();
                createTransaction4Payment(_parameter, editedDoc, multi.getCurrentInstance(),
                                multi.getAttribute(CISales.Payment.Amount));
            }
            new AbstractPaymentDocument() { }.executeAutomation(_parameter, editedDoc);
        }
        return new Return();
    }

    /**
     * Creates the transaction for payment.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _createdDoc the created doc
     * @param _paymentInstance the payment instance
     * @param _amount the amount
     * @return the instance
     * @throws EFapsException on error
     */
    public Instance createTransaction4Payment(final Parameter _parameter,
                                              final CreatedDoc _createdDoc,
                                              final Instance _paymentInstance,
                                              final BigDecimal _amount)
        throws EFapsException
    {
        Instance ret = null;
        if (InstanceUtils.isKindOf(_createdDoc.getInstance(), CISales.PaymentDocumentIOAbstract)) {
            final PrintQuery print = new PrintQuery(_paymentInstance);
            print.addAttribute(CISales.Payment.Status);
            print.executeWithoutAccessCheck();
            final Status status = Status.get(print.<Long>getAttribute(CISales.Payment.Status));

            if (Status.find(CISales.PaymentStatus.Executed).equals(status)) {
                final Insert transIns;
                if (InstanceUtils.isKindOf(_createdDoc.getInstance(), CISales.PaymentDocumentAbstract)) {
                    transIns = new Insert(CISales.TransactionInbound);
                } else {
                    transIns = new Insert(CISales.TransactionOutbound);
                }
                transIns.add(CISales.TransactionAbstract.CurrencyId, _createdDoc.getValues().get(
                                CISales.PaymentDocumentAbstract.RateCurrencyLink.name));
                transIns.add(CISales.TransactionAbstract.Payment, _paymentInstance);
                transIns.add(CISales.TransactionAbstract.Amount, _amount);
                transIns.add(CISales.TransactionAbstract.Date, _createdDoc.getValues().get(
                                CISales.PaymentDocumentAbstract.Date.name));
                transIns.add(CISales.TransactionAbstract.Account,
                                _createdDoc.getValues().get(CISales.TransactionAbstract.Account.name));
                transIns.execute();
                ret = transIns.getInstance();
                // if late transaction is activated -> check if a transaction for Petty Cash must be done
                if (Sales.PETTYCASH_LATEBALANCETRANS.get()) {
                    new Account().lateTransaction4Balance(_parameter, CISales.PettyCashBalance2CollectionOrder,
                                    CISales.PettyCashBalance2PaymentOrder, _paymentInstance);
                }
                if (Sales.FUNDSTOBESETTLED_LATEBALANCETRANS.get()) {
                    new Account().lateTransaction4Balance(_parameter, CISales.FundsToBeSettledBalance2CollectionOrder,
                                    CISales.FundsToBeSettledBalance2PaymentOrder,_paymentInstance);
                }
            } else {
                final Update update = new Update(_paymentInstance);
                update.add(CISales.Payment.AccountLink,
                                _createdDoc.getValues().get(CISales.TransactionAbstract.Account.name));
                update.executeWithoutTrigger();
            }
        }
        return ret;
    }

    /**
     * Creates the transfer document.
     *
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _transInstances   list of transaction instances
     * @param _combine the combine
     * @return file created or null
     * @throws EFapsException on error
     */
    protected File createTransferDocument(final Parameter _parameter,
                                          final List<Instance> _transInstances,
                                          final boolean _combine)
        throws EFapsException
    {
        final Map<String, TransInfo> key2info = new LinkedHashMap<>();
        final Set<String> notes = new HashSet<>();

        final MultiPrintQuery multi = new MultiPrintQuery(_transInstances);
        multi.addAttribute(CISales.TransactionAbstract.Amount, CISales.TransactionAbstract.Date,
                        CISales.TransactionAbstract.Description, CISales.TransactionAbstract.Account);
        final SelectBuilder selCurInst = SelectBuilder.get().linkto(CISales.TransactionAbstract.CurrencyId).instance();

        final SelectBuilder selAccName = SelectBuilder.get().linkto(CISales.TransactionAbstract.Account)
                        .attribute(CISales.AccountAbstract.Name);
        final SelectBuilder selAccInst = SelectBuilder.get().linkto(CISales.TransactionAbstract.Account).instance();
        multi.addSelect(selCurInst, selAccName, selAccInst);
        multi.executeWithoutAccessCheck();
        DateTime date = null;
        while (multi.next()) {
            BigDecimal amount = multi.<BigDecimal>getAttribute(CISales.TransactionAbstract.Amount);

            if (multi.getCurrentInstance().getType().isKindOf(CISales.TransactionOutbound.getType())) {
                amount = amount.negate();
            }
            final Instance curInst = multi.getSelect(selCurInst);
            final Instance accInst = multi.getSelect(selAccInst);
            final String key = curInst.getOid() + "-"  + accInst.getOid();
            final TransInfo info;
            if (key2info.containsKey(key)) {
                info = key2info.get(key);
            } else {
                info = new TransInfo(curInst);
            }
            info.add(amount);
            key2info.put(key, info);
            date = multi.getAttribute(CISales.TransactionAbstract.Date);
            notes.add(multi.<String>getAttribute(CISales.TransactionAbstract.Description));
            info.addAccount(multi.<String>getSelect(selAccName));
            info.addTransInstance(multi.getCurrentInstance());
        }
        String noteStr = "";
        for (final String note : notes) {
            if (note != null) {
                noteStr = noteStr + note;
            }
        }
        Instance currentDocInst = null;
        String currentName = null;
        final List<File> files = new ArrayList<>();
        for (final TransInfo info : key2info.values()) {
            final TransferDocument transDoc = new TransferDocument();
            final CurrencyInst curInst = info.getCurrencyInst();
            final RateInfo rateInfo = new Currency().evaluateRateInfo(_parameter, date, curInst.getInstance());
            if (!_combine || currentDocInst == null) {
                currentName = transDoc.getDocName4Create(_parameter);
                final Insert docInsert = new Insert(CISales.TransferDocument);
                docInsert.add(CISales.TransferDocument.Name, currentName);
                docInsert.add(CISales.TransferDocument.CurrencyLink, Currency.getBaseCurrency());
                docInsert.add(CISales.TransferDocument.Amount, info.getAmount());
                docInsert.add(CISales.TransferDocument.RateCurrencyLink, curInst.getInstance());
                docInsert.add(CISales.TransferDocument.Rate, rateInfo.getRateObject());
                docInsert.add(CISales.TransferDocument.Date, date);
                docInsert.add(CISales.TransferDocument.Note, noteStr);
                docInsert.add(CISales.TransferDocument.Status, Status.find(CISales.TransferDocumentStatus.Open));
                docInsert.execute();
                currentDocInst = docInsert.getInstance();
            }

            final Insert payInsert = new Insert(CISales.Payment);
            payInsert.add(CISales.Payment.Date, date);
            payInsert.add(CISales.Payment.TargetDocument, currentDocInst);
            payInsert.add(CISales.Payment.Amount, info.getAmount());
            payInsert.add(CISales.Payment.Rate, rateInfo.getRateObject());
            payInsert.add(CISales.Payment.RateCurrencyLink, curInst.getInstance());
            payInsert.add(CISales.Payment.CurrencyLink, Currency.getBaseCurrency());
            payInsert.execute();

            for (final Instance inst : info.getTransInst()) {
                final Update update = new Update(inst);
                update.add(CISales.TransactionAbstract.Payment, payInsert.getInstance());
                update.execute();
            }

            final CreatedDoc createdDoc = new CreatedDoc(currentDocInst);
            createdDoc.getValues().put(CISales.PaymentDocumentAbstract.Name.name, currentName);
            createdDoc.getValues().put("accountName", info.getAccountName());
            createdDoc.getValues().put("accountCurrencyName", curInst.getName());
            final File file = transDoc.createReport(_parameter, createdDoc);
            if (file != null) {
                if (_combine && !files.isEmpty()) {
                    files.clear();
                }
                files.add(file);
            }
        }
        File ret = null;
        if (!files.isEmpty()) {
            ret = new FileUtil().combinePdfs(files, CISales.TransferDocument.getType().getLabel(), false);
        }
        return ret;
    }

    /**
     * Method is executed as trigger after the insert of an
     * Products_TransactionInbound.
     *
     * @param _parameter Parameters as passed from eFaps
     * @return Return
     * @throws EFapsException on error
     */
    public Return inboundTrigger(final Parameter _parameter)
        throws EFapsException
    {
        addRemoveFromAccount(_parameter, true);
        updateDocument(_parameter);
        return new Return();
    }

    /**
     * Method is executed as trigger after the insert of an
     * Products_TransactionOutbound.
     *
     * @param _parameter Parameters as passed from eFaps
     * @return Return
     * @throws EFapsException on error
     */
    public Return outboundTrigger(final Parameter _parameter)
        throws EFapsException
    {
        addRemoveFromAccount(_parameter, false);
        updateDocument(_parameter);
        return new Return();
    }

    /**
     * Method is executed as trigger after the insert of an
     * Products_TransactionOutbound.
     *
     * @param _parameter Parameters as passed from eFaps
     * @return Return
     * @throws EFapsException on error
     */
    public Return deleteTrigger(final Parameter _parameter)
        throws EFapsException
    {
        addRemoveFromAccount(_parameter, true);
        return new Return();
    }

    /**
     *
     * @param _parameter Parameters as passed from eFaps
     * @return Return
     * @throws EFapsException on error
     */
    public Return updateTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_parameter.getInstance());
        final SelectBuilder accSel = SelectBuilder.get().linkto(CISales.TransactionAbstract.Account).instance();
        print.addSelect(accSel);
        if (print.execute()) {
            final Instance accInst = print.<Instance>getSelect(accSel);
            if (accInst != null && accInst.isValid()) {
                reCalculateAccounts(_parameter, accInst);
            }
        }
        return new Return();
    }


    /**
     * Add or subtract from the Inventory.
     *
     * @param _parameter Parameters as passed from eFaps
     * @param _add if true the quantity will be added else subtracted
     * @throws EFapsException on error
     */
    protected void addRemoveFromAccount(final Parameter _parameter,
                                        final boolean _add)
        throws EFapsException
    {
        final Instance instance = _parameter.getInstance();
        // get the transaction
        final PrintQuery print = new PrintQuery(instance);
        final SelectBuilder accSel = SelectBuilder.get().linkto(CISales.TransactionAbstract.Account).instance();
        final SelectBuilder curSel = SelectBuilder.get().linkto(CISales.TransactionAbstract.CurrencyId).instance();
        print.addSelect(accSel, curSel);
        print.addAttribute(CISales.TransactionAbstract.Amount);
        BigDecimal amount = BigDecimal.ZERO;
        Instance accountInst = null;
        Instance currencyInst = null;
        if (print.execute()) {
            amount = print.<BigDecimal>getAttribute(CISales.TransactionAbstract.Amount);
            accountInst =  print.<Instance>getSelect(accSel);
            currencyInst = print.<Instance>getSelect(curSel);
        }
        if (!_add) {
            amount = amount.negate();
        }
        updateBalance(_parameter, accountInst, currencyInst, amount);
    }

    /**
     * Update balance.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _accInst the acc inst
     * @param _currInst the curr inst
     * @param _amount the amount
     * @throws EFapsException on error
     */
    public void updateBalance(final Parameter _parameter,
                              final Instance _accInst,
                              final Instance _currInst,
                              final BigDecimal _amount)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CISales.Balance);
        queryBldr.addWhereAttrEqValue(CISales.Balance.Account, _accInst);
        queryBldr.addWhereAttrEqValue(CISales.Balance.Currency, _currInst);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.Balance.Amount);
        multi.execute();
        BigDecimal amount = _amount;
        final Update update;
        if (multi.next()) {
            update = new Update(multi.getCurrentInstance());
            final BigDecimal current = multi.<BigDecimal>getAttribute(CISales.Balance.Amount);
            amount = current.add(_amount);
        } else {
            update = new Insert(CISales.Balance);
            update.add(CISales.Balance.Currency, _currInst);
            update.add(CISales.Balance.Account, _accInst);
        }
        update.add(CISales.Balance.Amount, amount);
        update.execute();
    }


    /**
     * Update document.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @throws EFapsException on error
     */
    protected void updateDocument(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instance = _parameter.getInstance();
        // get the transaction
        final PrintQuery print = new PrintQuery(instance);
        final SelectBuilder sel = new SelectBuilder().linkto(CISales.TransactionAbstract.Payment)
                        .linkto(CIERP.Document2PaymentDocumentAbstract.FromAbstractLink).oid();
        final SelectBuilder selAcc = new SelectBuilder().linkto(CISales.TransactionAbstract.Account).instance();
        print.addSelect(sel, selAcc);
        print.executeWithoutAccessCheck();

        final Instance docInst = Instance.get(print.<String>getSelect(sel));
        final Instance accInst = print.<Instance>getSelect(selAcc);
        if (docInst.isValid() && accInst != null && !accInst.getType().equals(CISales.AccountPettyCash.getType())
                        && accInst != null && !accInst.getType().equals(CISales.AccountFundsToBeSettled.getType())) {
            _parameter.put(ParameterValues.INSTANCE, docInst);
            final DocumentUpdate docUpdate = new DocumentUpdate() {
                @Override
                protected boolean executeWithoutTrigger(final Parameter _parameter)
                {
                    return true;
                }
            };
            docUpdate.updateDocument(_parameter);
        }
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return return granting access or not
     * @throws EFapsException on error
     */
    public Return accessCheck4Payment(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();

        if (_parameter.get(ParameterValues.ACCESSMODE) == TargetMode.VIEW) {
            final Instance instance = _parameter.getCallInstance();
            // get the transaction
            final PrintQuery print = new PrintQuery(instance);
            final SelectBuilder sel = new SelectBuilder().linkto(CISales.TransactionAbstract.Payment)
                            .linkto(CIERP.Document2PaymentDocumentAbstract.FromAbstractLink).oid();
            print.addSelect(sel);
            print.executeWithoutAccessCheck();

            final Instance docInst = Instance.get(print.<String>getSelect(sel));
            if (docInst.isValid()) {
                ret.put(ReturnValues.TRUE, true);
            }
        }
        return ret;
    }

    public Return accessCheck4EditAccount(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Instance callInstance = _parameter.getCallInstance();
        if (InstanceUtils.isKindOf(callInstance, CISales.TransactionAbstract)) {
            final PrintQuery print = new PrintQuery(callInstance);
            final SelectBuilder selAcount = new SelectBuilder().linkto(CISales.TransactionAbstract.Account)
                            .instance();
            print.addSelect(selAcount);
            print.executeWithoutAccessCheck();
            final Instance accountInst = print.getSelect(selAcount);
            if (!InstanceUtils.isType(accountInst, CISales.AccountFundsToBeSettled)
                            && !InstanceUtils.isType(accountInst, CISales.AccountPettyCash)) {
                ret.put(ReturnValues.TRUE, true);
            }
        } else {
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    /**
     * Method is called from a transaction to recalculate all values for the
     * accounts.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return empty Return
     * @throws EFapsException on error
     */
    public Return reCalculateAccounts(final Parameter _parameter)
        throws EFapsException
    {
        return reCalculateAccounts(_parameter, _parameter.getInstance());
    }

    /**
     * Method is called from a transaction to recalculate all values for the
     * accounts.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param  _accInst instance of the account
     * @return empty Return
     * @throws EFapsException on error
     */
    public Return reCalculateAccounts(final Parameter _parameter,
                                      final Instance _accInst)
        throws EFapsException
    {
        BigDecimal sumTotal = BigDecimal.ZERO;
        final QueryBuilder queryBldr = new QueryBuilder(CISales.TransactionAbstract);
        queryBldr.addWhereAttrEqValue(CISales.TransactionAbstract.Account, _accInst);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.TransactionAbstract.Amount);
        multi.execute();

        while (multi.next()) {
            final BigDecimal amount = multi.<BigDecimal>getAttribute(CISales.TransactionAbstract.Amount);

            if (multi.getCurrentInstance().getType().equals(CISales.TransactionInbound.getType())) {
                sumTotal = sumTotal.add(amount);
            } else {
                sumTotal = sumTotal.subtract(amount);
            }
        }

        final QueryBuilder queryBldr2 = new QueryBuilder(CISales.Balance);
        queryBldr2.addWhereAttrEqValue(CISales.Balance.Account, _accInst);
        final InstanceQuery accQuery = queryBldr2.getQuery();
        accQuery.execute();
        while (accQuery.next()) {
            final Update update = new Update(accQuery.getCurrentValue());
            update.add(CISales.Balance.Amount, sumTotal);
            update.executeWithoutTrigger();
        }

        return new Return();
    }

    /**
     * Format date.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return formatDate(final Parameter _parameter)
        throws EFapsException
    {
        final StringBuilder js = new StringBuilder();
        final Return retVal = new Return();
        final IUIValue value = (IUIValue) _parameter.get(ParameterValues.UIOBJECT);
        if (value.getDisplay().equals(Display.READONLY)) {
            String dec = (String) value.getObject();
            if (dec == null || dec != null && dec.contains("null")) {
                dec = "";
            }
            js.append(dec);
        } else {
            js.append(value.getObject());
        }
        retVal.put(ReturnValues.VALUES, js.toString());
        return retVal;
    }

    /**
     * Gets the table4 transaction field value ui.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the table4 transaction field value ui
     * @throws EFapsException on error
     */
    public Return getTable4TransactionFieldValueUI(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String amountName =  getProperty(_parameter, "AmountName");
        final String currencyName =  getProperty(_parameter, "CurrencyName");
        final String costName =  getProperty(_parameter, "CostName");
        final String currencyId =  getProperty(_parameter, "CurrencyId");

        final Table table = new Table()
                .addRow()
                .addHeaderColumn(DBProperties.getProperty("org.efaps.esjp.sales.Transaction.Amount"))
                .addHeaderColumn(DBProperties.getProperty("org.efaps.esjp.sales.Transaction.Currency"))
                .addHeaderColumn(DBProperties.getProperty("org.efaps.esjp.sales.Transaction.Cost"))
                .addRow()
                .addColumn(new StringBuilder().append("<input type=\"text\" size=\"6\" name=\"")
                                .append(amountName).append("\"/>"))
                .addColumn(new StringBuilder().append("<input type=\"text\" size=\"6\" name=\"")
                                .append(currencyName)
                                .append("\" value=\"\"").append(" readonly=\"readonly\" ").append("\"/>")
                                .append("<input type=\"hidden\" name=\"").append(currencyId)
                                .append("\" value=\"\"").append("\"/>"))
                .addColumn(new StringBuilder().append("<input type=\"text\" size=\"6\" name=\"")
                                .append(costName).append("\"/>"));
        ret.put(ReturnValues.SNIPLETT, table.toHtml());
        return ret;
    }

    /**
     * Update drop down transaction.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return updateDropDownTransaction(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final StringBuilder html = new StringBuilder();
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        Long currencyId = (long) 0;
        final String currencyType = (String) properties.get("CurrencyType");
        final String currId = (String) properties.get("CurrencyId");
        final Field field = (Field) _parameter.get(ParameterValues.UIOBJECT);
        html.append("document.getElementsByName('").append(currencyType).append("')[0].value='");

        if (!_parameter.getParameterValue(field.getName()).isEmpty()) {
            final Long id = Long.parseLong(_parameter.getParameterValue(field.getName()));

            final QueryBuilder queryBldr = new QueryBuilder(CISales.AccountAbstract);
            queryBldr.addWhereAttrEqValue(CISales.AccountAbstract.ID, id);
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selCurrencylabel = new SelectBuilder().linkto(CISales.AccountAbstract.CurrencyLink)
                            .attribute(CIERP.Currency.Name);
            multi.addSelect(selCurrencylabel);
            multi.addAttribute(CISales.AccountAbstract.CurrencyLink);
            multi.execute();
            if (multi.next()) {
                final String currencyName = multi.<String>getSelect(selCurrencylabel);
                currencyId = multi.<Long>getAttribute(CISales.AccountAbstract.CurrencyLink);
                if (currencyName != null) {
                    html.append(currencyName);
                }
            }
        }
        html.append("';");

        html.append("document.getElementsByName('").append(currId).append("')[0].value='").append(currencyId)
                        .append("';");
        final List<Map<String, String>> list = new ArrayList<>();
        final Map<String, String> map = new HashMap<>();
        map.put(EFapsKey.FIELDUPDATE_JAVASCRIPT.getKey(), html.toString());
        list.add(map);
        ret.put(ReturnValues.VALUES, list);
        return ret;
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @return new empty Return
     * @throws EFapsException on errro
     */
    public Return createInternalTransfer(final Parameter _parameter)
        throws EFapsException
    {
        final String costOutStr = _parameter.getParameterValue("cost_outbound");
        final String currIdOutStr = _parameter.getParameterValue("currencyId_outbound");
        final String amountOutStr = _parameter.getParameterValue("amount_outbound");
        final String costInStr = _parameter.getParameterValue("cost_inbound");
        final String currIdInStr = _parameter.getParameterValue("currencyId_inbound");
        final String amountInStr = _parameter.getParameterValue("amount_inbound");
        final String charger = _parameter.getParameterValue("charger");
        final String payment = _parameter.getParameterValue("payment");

        final List<Instance> transInst = new ArrayList<>();
        // transaction outbound
        transInst.addAll(createInternalTransaction(_parameter, CISales.TransactionOutbound.getType(), charger,
                        costOutStr, currIdOutStr, amountOutStr));
        // transaction inbound
        transInst.addAll(createInternalTransaction(_parameter, CISales.TransactionInbound.getType(), payment,
                        costInStr, currIdInStr, amountInStr));
        final File file = createTransferDocument(_parameter, transInst, true);
        final Return ret = new Return();
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;

    }

    /**
     * Method for create internal transfers.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @param _transactionType  type of transactio to be inserted
     * @param _account  account the transaction belongs to
     * @param _cost     cost
     * @param _currencyId currency
     * @param _amount   amount
     * @return List of created transaction instance
     * @throws EFapsException on error
     */
    public List<Instance> createInternalTransaction(final Parameter _parameter,
                                            final Type _transactionType,
                                            final String _account,
                                            final String _cost,
                                            final String _currencyId,
                                            final String _amount)
        throws EFapsException
    {
        final List<Instance> ret = new ArrayList<>();
        final String note = _parameter.getParameterValue("note");

        Instance instCharger = null;
        if (!_account.isEmpty() && _account != null) {
            final QueryBuilder queryBldr = new QueryBuilder(CISales.AccountAbstract);
            queryBldr.addWhereAttrEqValue(CISales.AccountAbstract.ID, _account);
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CISales.AccountAbstract.OID);
            multi.execute();
            if (multi.next()) {
                final String accountOid = multi.<String>getAttribute(CISales.AccountAbstract.OID);
                instCharger = Instance.get(accountOid);
            }
        }

        Long currId = (long) 0;
        if (!_currencyId.isEmpty() && _currencyId != null) {
            currId = Long.parseLong(_currencyId);
        }
        final DecimalFormat formater = (DecimalFormat) NumberFormat.getInstance(Context.getThreadContext().getLocale());
        formater.setParseBigDecimal(true);
        if (_cost != null && !_cost.isEmpty() && instCharger.isValid() && instCharger != null) {
            BigDecimal cost = BigDecimal.ZERO;

            try {
                cost = (BigDecimal) formater.parse(_cost);
            } catch (final ParseException e) {
                Transaction_Base.LOG.error("Catched", e);
            }

            if (cost.compareTo(BigDecimal.ZERO) != 0) {
                final Insert transInsertCost = new Insert(_transactionType);
                transInsertCost.add(CISales.TransactionAbstract.Amount, cost);
                transInsertCost.add(CISales.TransactionAbstract.CurrencyId, currId);
                transInsertCost.add(CISales.TransactionAbstract.Account, instCharger.getId());
                transInsertCost.add(CISales.TransactionAbstract.Description, note);
                transInsertCost.add(CISales.TransactionAbstract.Date, new DateTime());
                transInsertCost.execute();
                ret.add(transInsertCost.getInstance());
            }
        }

        if (_amount != null && !_amount.isEmpty() && instCharger.isValid() && instCharger != null) {
            BigDecimal amount = BigDecimal.ZERO;
            try {
                amount = (BigDecimal) formater.parse(_amount);
            } catch (final ParseException e) {
                Transaction_Base.LOG.error("Catched", e);
            }
            final Insert transInsertAmount = new Insert(_transactionType);
            transInsertAmount.add(CISales.TransactionAbstract.Amount, amount);
            transInsertAmount.add(CISales.TransactionAbstract.CurrencyId, currId);
            transInsertAmount.add(CISales.TransactionAbstract.Account, instCharger.getId());
            transInsertAmount.add(CISales.TransactionAbstract.Description, note);
            transInsertAmount.add(CISales.TransactionAbstract.Date, new DateTime());
            transInsertAmount.execute();
            ret.add(transInsertAmount.getInstance());
        }
        return ret;
    }

    /**
     * The Class TransInfo.
     */
    public static class TransInfo
    {

        /** The currency inst. */
        private final Instance currencyInst;

        /** The amount. */
        private BigDecimal amount = BigDecimal.ZERO;

        /** The accounts. */
        private final Set<String> accounts = new LinkedHashSet<>();

        /** The trans inst. */
        private final Set<Instance> transInst = new LinkedHashSet<>();

        /**
         * Instantiates a new trans info.
         *
         * @param _inst the inst
         */
        public TransInfo(final Instance _inst)
        {
            currencyInst = _inst;
        }

        /**
         * Gets the account name.
         *
         * @return the account name
         */
        public Object getAccountName()
        {
            String ret = "";
            for (final String account : accounts) {
                if (account != null) {
                    ret = ret.isEmpty() ? account : ret +  " -> " + account;
                }
            }
            return ret;
        }

        /**
         * Adds the account.
         *
         * @param _select the select
         */
        public void addAccount(final String _select)
        {
            accounts.add(_select);
        }

        /**
         * Adds the trans instance.
         *
         * @param _inst the inst
         */
        public void addTransInstance(final Instance _inst)
        {
            transInst.add(_inst);
        }

        /**
         * Adds the.
         *
         * @param _amount the amount
         */
        public void add(final BigDecimal _amount)
        {
            amount = amount.add(_amount);
        }

        /**
         * Gets the currency inst.
         *
         * @return the currency inst
         */
        public CurrencyInst getCurrencyInst()
        {
            return new CurrencyInst(currencyInst);
        }

        /**
         * Getter method for the instance variable {@link #amount}.
         *
         * @return value of instance variable {@link #amount}
         */
        public BigDecimal getAmount()
        {
            return amount;
        }

        /**
         * Getter method for the instance variable {@link #transInst}.
         *
         * @return value of instance variable {@link #transInst}
         */
        public Set<Instance> getTransInst()
        {
            return transInst;
        }
    }
}
