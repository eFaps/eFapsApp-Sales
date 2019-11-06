/*
 * Copyright 2003 - 2017 The eFaps Team
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

package org.efaps.esjp.sales.document;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.ci.CIType;
import org.efaps.db.CachedPrintQuery;
import org.efaps.db.Delete;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.common.util.InterfaceUtils_Base.DojoLibs;
import org.efaps.esjp.contacts.Contacts;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.AbstractWarning;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.IWarning;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.Account;
import org.efaps.esjp.sales.Calculator;
import org.efaps.esjp.sales.document.Validation_Base.InvalidNameWarning;
import org.efaps.ui.wicket.util.DateUtil;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("9b1b92aa-550a-48d3-a58e-2c47e54802f9")
@EFapsApplication("eFapsApp-Sales")
public abstract class PettyCashReceipt_Base
    extends AbstractDocumentSum
{

    /**
     * Executed from a Command execute vent to create a new PettyCashReceipt.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        setValue4ContactPicker(_parameter);
        final CreatedDoc createdDoc = createDoc(_parameter);
        createPositions(_parameter, createdDoc);
        connect2DocumentType(_parameter, createdDoc);
        connect2Account(_parameter, createdDoc);
        createTransaction(_parameter, createdDoc);
        connect2Object(_parameter, createdDoc);
        final File file = createReport(_parameter, createdDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        ret.put(ReturnValues.INSTANCE, createdDoc.getInstance());
        return ret;
    }

    /**
     * Create the TransactionDocument for this receipt.
     *
     * @param _parameter Parameter from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return createTransDocShadow(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = new TransactionDocument().createDocumentShadow(_parameter);
        final Insert insert = new Insert(CISales.PettyCashReceipt2TransactionDocumentShadowIn);
        insert.add(CISales.PettyCashReceipt2TransactionDocumentShadowIn.FromLink, _parameter.getInstance());
        insert.add(CISales.PettyCashReceipt2TransactionDocumentShadowIn.ToLink, createdDoc.getInstance());
        insert.execute();
        return new Return();
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     */
    protected void setValue4ContactPicker(final Parameter _parameter)
    {
        final String contactpicker = _parameter
                        .getParameterValue(CIFormSales.Sales_PettyCashReceiptForm.contactPicker.name);
        if (contactpicker != null) {
            ParameterUtil.setParameterValues(_parameter, CIFormSales.Sales_PettyCashReceiptForm.contact.name,
                            contactpicker);
        }
    }

    /**
     * Connect Account and PettyCash.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _createdDoc doc the account is connected to
     * @throws EFapsException on error
     */
    protected void connect2Account(final Parameter _parameter,
                                   final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final Insert insert = new Insert(CISales.AccountPettyCash2PettyCashReceipt);
        insert.add(CISales.AccountPettyCash2PettyCashReceipt.FromLink, _parameter.getInstance());
        insert.add(CISales.AccountPettyCash2PettyCashReceipt.ToLink, _createdDoc.getInstance());
        insert.execute();
    }

    /**
     * Create the transaction for the PettyCash.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _createdDoc doc the transaction is connected to
     * @throws EFapsException on error
     */
    protected void createTransaction(final Parameter _parameter,
                                     final CreatedDoc _createdDoc)
        throws EFapsException
    {
        createTransaction(_parameter, _createdDoc, _parameter.getInstance());
    }

    /**
     * Create the transaction for the related account.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _createdDoc doc the transaction is connected to
     * @param _accInst instcane of the account
     * @throws EFapsException on error
     */
    protected void createTransaction(final Parameter _parameter,
                                     final CreatedDoc _createdDoc,
                                     final Instance _accInst)
        throws EFapsException
    {
        final CurrencyInst accCurrencyInst = new Account().getCurrencyInst(_parameter, _accInst);

        final DateTime date = new DateTime(_createdDoc.getValue(CISales.DocumentSumAbstract.Date.name));

        final Insert payInsert = new Insert(CISales.Payment);
        payInsert.add(CISales.Payment.Date, date);
        payInsert.add(CISales.Payment.CreateDocument, _createdDoc.getInstance());
        payInsert.execute();

        final Object amount = evaluateAmount4Transaction(_parameter, _createdDoc, accCurrencyInst, date);

        final Insert transInsert = new Insert(CISales.TransactionOutbound);
        transInsert.add(CISales.TransactionOutbound.Amount, amount);
        transInsert.add(CISales.TransactionOutbound.CurrencyId, accCurrencyInst.getInstance());
        transInsert.add(CISales.TransactionOutbound.Payment, payInsert.getInstance());
        transInsert.add(CISales.TransactionOutbound.Account, _accInst);
        transInsert.add(CISales.TransactionOutbound.Description,
                        _createdDoc.getValue(CISales.DocumentSumAbstract.Note.name));
        transInsert.add(CISales.TransactionOutbound.Date, _createdDoc.getValue(CISales.DocumentSumAbstract.Date.name));
        transInsert.execute();
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API
     * @param _createdDoc doc the transaction is connected to
     * @param _accCurrencyInst instcane of the account
     * @param _date date
     * @return amount
     * @throws EFapsException on error
     */
    protected BigDecimal evaluateAmount4Transaction(final Parameter _parameter,
                                                final CreatedDoc _createdDoc,
                                                final CurrencyInst _accCurrencyInst,
                                                final DateTime _date)
        throws EFapsException
    {
        final CurrencyInst rateCurrencyInst = CurrencyInst.get(_createdDoc
                        .getValue(CISales.DocumentSumAbstract.RateCurrencyId.name));
        // evaluate if the amount must be changed to the currency of the account
        final BigDecimal rateAmount = (BigDecimal) _createdDoc.getValue(CISales.DocumentSumAbstract.RateCrossTotal.name);
        return Currency.convert(_parameter, rateAmount, rateCurrencyInst.getInstance(), _accCurrencyInst.getInstance(),
                        getCIType().getType().getName(), LocalDate.of(_date.getYear(), _date.getMonthOfYear(), _date.getDayOfMonth()));
    }

    /**
     * Update the transaction for the PettyCash.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _editedDoc doc the transaction is connected to
     * @throws EFapsException on error
     */
    protected void updateTransaction(final Parameter _parameter,
                                     final EditedDoc _editedDoc)
        throws EFapsException
    {
        // get the payment
        final QueryBuilder payQueryBldr = new QueryBuilder(CISales.Payment);
        payQueryBldr.addWhereAttrEqValue(CISales.Payment.CreateDocument, _editedDoc.getInstance());
        final InstanceQuery payQuery = payQueryBldr.getQuery();
        payQuery.executeWithoutAccessCheck();
        if (payQuery.next()) {
            final QueryBuilder transQueryBldr = new QueryBuilder(CISales.TransactionOutbound);
            transQueryBldr.addWhereAttrEqValue(CISales.TransactionOutbound.Payment, payQuery.getCurrentValue());
            final MultiPrintQuery multi = transQueryBldr.getPrint();
            final SelectBuilder selAccInst = SelectBuilder.get().linkto(CISales.TransactionOutbound.Account).instance();
            multi.addSelect(selAccInst);
            multi.addAttribute(CISales.TransactionOutbound.Amount, CISales.TransactionOutbound.Description);
            multi.executeWithoutAccessCheck();
            if (multi.next()) {
                final Instance accInst = multi.getSelect(selAccInst);
                final CurrencyInst accCurrencyInst = new Account().getCurrencyInst(_parameter, accInst);
                final DateTime date = new DateTime(_editedDoc.getValue(CISales.DocumentSumAbstract.Date.name));
                final String description = multi.<String>getAttribute(CISales.TransactionAbstract.Description);
                final BigDecimal amount = multi.<BigDecimal>getAttribute(CISales.TransactionAbstract.Amount);
                final BigDecimal newAmount = evaluateAmount4Transaction(_parameter, _editedDoc,
                                accCurrencyInst, date);
                if (newAmount.compareTo(amount) != 0
                                || !description.equals(_editedDoc.getValue(CISales.DocumentSumAbstract.Note.name))) {
                    final Update update = new Update(multi.getCurrentInstance());
                    update.add(CISales.TransactionOutbound.Amount, newAmount);
                    update.add(CISales.TransactionOutbound.CurrencyId, accCurrencyInst.getInstance());
                    update.add(CISales.TransactionOutbound.Description,
                                    _editedDoc.getValue(CISales.DocumentSumAbstract.Note.name));
                    update.execute();
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDocName4Create(final Parameter _parameter)
        throws EFapsException
    {
        String ret = _parameter.getParameterValue("name4create");
        if (ret == null || ret != null && ret.isEmpty()) {
            final PrintQuery print = new PrintQuery(_parameter.getInstance());
            print.addAttribute(CISales.AccountPettyCash.Name);
            print.execute();
            final String accName = print.<String>getAttribute(CISales.AccountPettyCash.Name);
            ret =  accName + " " + (new Account().getMaxPosition(_parameter, _parameter.getInstance()) + 1);
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return instance of the currency
     * @throws EFapsException on error
     */
    @Override
    protected Instance evaluateCurrency4JavaScript(final Parameter _parameter)
        throws EFapsException
    {
        Instance ret = null;
        if (TargetMode.CREATE.equals(_parameter.get(ParameterValues.ACCESSMODE)) && _parameter.getCallInstance() != null
                        && _parameter.getCallInstance().isValid() && _parameter.getCallInstance().getType().isCIType(
                                        CISales.AccountPettyCash)) {
            final Instance inst = _parameter.getCallInstance();

            final PrintQuery print = CachedPrintQuery.get4Request(inst);
            final SelectBuilder sel = SelectBuilder.get().linkto(CISales.AccountPettyCash.CurrencyLink).instance();
            print.addSelect(sel);
            print.executeWithoutAccessCheck();
            final Instance instTmp = print.<Instance>getSelect(sel);
            if (instTmp != null && instTmp.isValid()) {
                ret = instTmp;
            }
        } else {
            ret = super.evaluateCurrency4JavaScript(_parameter);
        }
        return ret;
    }

    /**
     * Create the TransactionDocument for this receipt.
     *
     * @param _parameter Parameter from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return deleteTransaction(final Parameter _parameter)
        throws EFapsException
    {
        if (InstanceUtils.isType(_parameter.getInstance(), CISales.PettyCashReceipt)) {
            final QueryBuilder queryBldr = new QueryBuilder(CISales.Payment);
            queryBldr.addWhereAttrEqValue(CISales.Payment.CreateDocument, _parameter.getInstance());
            final InstanceQuery query = queryBldr.getQuery();
            query.execute();
            while (query.next()) {
                final QueryBuilder transQueryBldr = new QueryBuilder(CISales.TransactionAbstract);
                transQueryBldr.addWhereAttrEqValue(CISales.TransactionAbstract.Payment, query.getCurrentValue());
                final InstanceQuery transQuery = transQueryBldr.getQuery();
                transQuery.execute();
                while (transQuery.next()) {
                    new Delete(transQuery.getCurrentValue()).execute();
                }
                new Delete(query.getCurrentValue()).execute();
            }
        }
        return new Return();
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _warnings list of warnings
     * @return list of warnings
     * @throws EFapsException on error
     */
    public List<IWarning> validatePettyCash(final Parameter _parameter,
                                            final List<IWarning> _warnings)
        throws EFapsException
    {
        final List<IWarning> ret = new ArrayList<>();
        final boolean evaluatePositions = !"false".equalsIgnoreCase(getProperty(_parameter, "EvaluatePositions"));
        // first check the positions
        final List<Calculator> calcList = analyseTable(_parameter, null);
        if (evaluatePositions
                        && (calcList.isEmpty() || getNetTotal(_parameter, calcList).compareTo(BigDecimal.ZERO) == 0)) {
            ret.add(new EvaluatePositionWarning());
        } else {
            if (evalDeducible(_parameter)) {
                final String name = _parameter
                                .getParameterValue(CIFormSales.Sales_PettyCashReceiptForm.name4create.name);
                final Instance contactInst = Instance.get(_parameter
                                .getParameterValue(CIFormSales.Sales_PettyCashReceiptForm.contact.name));
                if (name.isEmpty() || !contactInst.isValid()) {
                    ret.add(new EvaluateDeducibleDocWarning());
                }
            } else {
                final String name = _parameter
                                .getParameterValue(CIFormSales.Sales_PettyCashReceiptForm.name4create.name);
                final String contact = _parameter
                                .getParameterValue(CIFormSales.Sales_PettyCashReceiptForm.contact.name);
                if (name != null && !name.isEmpty() || contact != null && !contact.isEmpty()) {
                    ret.add(new EvaluateNotDeducibleDocWarning());
                } else {
                    final Iterator<IWarning> iterator = _warnings.iterator();
                    while (iterator.hasNext()) {
                        final IWarning warning = iterator.next();
                        if (warning instanceof InvalidNameWarning) {
                            iterator.remove();
                            break;
                        }
                    }
                }
            }
            final Instance instance = _parameter.getInstance();
            if (instance != null && instance.isValid()
                            && instance.getType().isKindOf(CISales.AccountAbstract.getType())) {
                final QueryBuilder queryBldr = new QueryBuilder(CISales.Balance);
                queryBldr.addWhereAttrEqValue(CISales.Balance.Account, instance);
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CISales.Balance.Amount, CISales.Balance.Currency);
                multi.execute();
                BigDecimal amountBal = BigDecimal.ZERO;
                while (multi.next()) {
                    amountBal = amountBal.add(multi.<BigDecimal>getAttribute(CISales.Balance.Amount));
                }
                final DecimalFormat frmt = NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter));
                final int scale = frmt.getMaximumFractionDigits();
                final Object[] rateObj = getRateObject(_parameter);
                final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                                RoundingMode.HALF_UP);
                final BigDecimal netTotal = getNetTotal(_parameter, calcList).divide(rate, RoundingMode.HALF_UP)
                                .setScale(scale, RoundingMode.HALF_UP);
                if (netTotal.compareTo(amountBal) == 1) {
                    ret.add(new EvaluateBalanceAccountDocWarning());
                }
            }
        }
        return ret;
    }

    @Override
    public Return validate(final Parameter _parameter)
        throws EFapsException
    {
        setValue4ContactPicker(_parameter);
        final Validation validation = new Validation()
        {
            @Override
            protected List<IWarning> validate(final Parameter _parameter,
                                              final List<IWarning> _warnings)
                throws EFapsException
            {
                final List<IWarning> ret = super.validate(_parameter, _warnings);
                ret.addAll(validatePettyCash(_parameter, ret));
                return ret;
            }
        };
        return validation.validate(_parameter, this);
    }

    /**
     *  @param _parameter Parameter from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return update4DocumentType(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final List<Map<String, Object>> list = new ArrayList<>();
        final Map<String, Object> map = new HashMap<>();
        list.add(map);
        if (!evalDeducible(_parameter)) {
            map.put(CIFormSales.Sales_PettyCashReceiptForm.name4create.name, "");
            map.put(CIFormSales.Sales_PettyCashReceiptForm.contactData.name, "");
            map.put(CIFormSales.Sales_PettyCashReceiptForm.contact.name, new String[] { "", "" });
            InterfaceUtils.appendScript4FieldUpdate(map, getSetFieldReadOnlyScript(_parameter,
                        CIFormSales.Sales_PettyCashReceiptForm.contact.name,
                        CIFormSales.Sales_PettyCashReceiptForm.name4create.name));
            final StringBuilder js = new StringBuilder().append("query(\".eFapsPickerLink\").forEach(function(node) {")
                            .append("node.style.display='none';")
                            .append("});");
            InterfaceUtils.appendScript4FieldUpdate(map,
                            InterfaceUtils.wrapInDojoRequire(_parameter, js, DojoLibs.QUERY));
        } else {
            InterfaceUtils.appendScript4FieldUpdate(map, getSetFieldReadOnlyScript(_parameter,
                            null, false, CIFormSales.Sales_PettyCashReceiptForm.contact.name,
                            CIFormSales.Sales_PettyCashReceiptForm.name4create.name));
            final StringBuilder js = new StringBuilder().append("query(\".eFapsPickerLink\").forEach(function(node) {")
                            .append("node.style.display='';")
                            .append("});");
            InterfaceUtils.appendScript4FieldUpdate(map,
                            InterfaceUtils.wrapInDojoRequire(_parameter, js, DojoLibs.QUERY));
        }
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * @param _parameter Paramater as passed by the eFaps APi
     * @return true if deducible else false
     */
    protected boolean evalDeducible(final Parameter _parameter)
    {
        return !"NONE".equals(_parameter.getParameterValue(CIFormSales.Sales_PettyCashReceiptForm.documentType.name));
    }

    /**
     * Edit.
     *
     * @param _parameter Parameter from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final EditedDoc editDoc = editDoc(_parameter);
        updatePositions(_parameter, editDoc);
        updateTransaction(_parameter, editDoc);
        final File file = createReport(_parameter, editDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    /**
     * @param _parameter parameter as passed by the eFaps API
     * @return Return containing javascript
     * @throws EFapsException on error
     */
    public Return getJavaScriptUIValue4Receipt(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final StringBuilder js = new StringBuilder().append("query(\".eFapsPickerLink\").forEach(function(node) {")
                        .append("node.style.display='none';")
                        .append("});");
        final StringBuilder bldr = InterfaceUtils.wrapInDojoRequire(_parameter, js, DojoLibs.QUERY)
                        .append(getSetFieldReadOnlyScript(_parameter,
                                        CIFormSales.Sales_PettyCashReceiptForm.contact.name,
                                        CIFormSales.Sales_PettyCashReceiptForm.name4create.name));
        ret.put(ReturnValues.SNIPLETT, InterfaceUtils.wrappInScriptTag(_parameter, bldr, true, 1500));
        return ret;
    }

    /**
     * @param _parameter parameter as passed by the eFaps API
     * @return Return containing javascript
     * @throws EFapsException on error
     */
    public Return getJavaScriptUIValue4EditJustification(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Instance inst = _parameter.getCallInstance();
        final StringBuilder js = new StringBuilder();

        if (inst.isValid()) {
            final PrintQuery print = new PrintQuery(inst);
            final SelectBuilder selContInst = new SelectBuilder().linkto(CISales.PettyCashReceipt.Contact).instance();
            final SelectBuilder selContName = new SelectBuilder().linkto(CISales.PettyCashReceipt.Contact)
                            .attribute(CIContacts.Contact.Name);
            final SelectBuilder selActionInst = SelectBuilder.get()
                            .linkfrom(CISales.ActionDefinitionPettyCashReceipt2Document.ToLink)
                            .linkto(CISales.ActionDefinitionPettyCashReceipt2Document.FromLink).instance();
            print.addSelect(selContInst, selContName, selActionInst);
            print.addAttribute(CISales.PettyCashReceipt.Name, CISales.PettyCashReceipt.Date);

            if (print.execute()) {
                final Instance contInst = print.<Instance>getSelect(selContInst);
                if (contInst.isValid()) {
                    final QueryBuilder queryBldr = new QueryBuilder(CISales.Document2DocumentType);
                    queryBldr.addWhereAttrEqValue(CISales.Document2DocumentType.DocumentLink, inst);
                    final MultiPrintQuery multi = queryBldr.getPrint();
                    final SelectBuilder selDocTypeInst = new SelectBuilder().linkto(
                                    CISales.Document2DocumentType.DocumentTypeLink).instance();
                    multi.addSelect(selDocTypeInst);
                    multi.execute();
                    if (multi.next()) {
                        js.append(getSetFieldValue(0,
                                        CIFormSales.Sales_PettyCashReceiptJustificationEditForm.documentType.name,
                                        multi.<Instance>getSelect(selDocTypeInst).getOid()));
                    }
                    final String info = new Contacts().getFieldValue4Contact(contInst, false);
                    final String contName = print.<String>getSelect(selContName);
                    final String name = print.<String>getAttribute(CISales.PettyCashReceipt.Name);

                    js.append(getSetFieldValue(0,
                                    CIFormSales.Sales_PettyCashReceiptJustificationEditForm.contactData.name, info))
                        .append(getSetFieldValue(0,
                                    CIFormSales.Sales_PettyCashReceiptJustificationEditForm.contact.name,
                                                    contInst.getOid(), contName))
                        .append(getSetFieldValue(0,
                                    CIFormSales.Sales_PettyCashReceiptJustificationEditForm.name4create.name, name));
                }
                final DateTime date = print.getAttribute(CISales.PettyCashReceipt.Date);
                js.append(getSetFieldValue(0,
                                CIFormSales.Sales_PettyCashReceiptJustificationEditForm.date.name + "_eFapsDate",
                                DateUtil.getDate4Parameter(date)));

                final Instance actionInst = print.<Instance>getSelect(selActionInst);
                if (actionInst != null && actionInst.isValid()) {
                    js.append(getSetFieldValue(0, CIFormSales.Sales_PettyCashReceiptJustificationEditForm.action.name,
                                    actionInst.getOid()));
                }
            }
        }
        ret.put(ReturnValues.SNIPLETT, InterfaceUtils.wrappInScriptTag(_parameter, js, true, 1500).toString());
        return ret;
    }

    @Override
    public CIType getCIType()
        throws EFapsException
    {
        return CISales.PettyCashReceipt;
    }

    /**
     * @param _parameter parameter as passed by the eFaps API
     * @return new empty Return
     * @throws EFapsException on error
     */
    public Return editJustification(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final boolean deducible = evalDeducible(_parameter);

        final String date = _parameter
                        .getParameterValue(CIFormSales.Sales_PettyCashReceiptJustificationEditForm.date.name);
        final String contact = _parameter
                        .getParameterValue(CIFormSales.Sales_PettyCashReceiptJustificationEditForm.contact.name);
        final String docName = _parameter
                        .getParameterValue(CIFormSales.Sales_PettyCashReceiptJustificationEditForm.name4create.name);
        final String docType = _parameter
                        .getParameterValue(CIFormSales.Sales_PettyCashReceiptJustificationEditForm.documentType.name);

        final Instance actionInst = Instance.get(_parameter
                        .getParameterValue(CIFormSales.Sales_PettyCashReceiptJustificationEditForm.action.name));

        final QueryBuilder queryBldr = new QueryBuilder(CISales.Document2DocumentType);
        queryBldr.addWhereAttrEqValue(CISales.Document2DocumentType.DocumentLink, _parameter.getCallInstance());
        final InstanceQuery query = queryBldr.getQuery();
        query.execute();
        Instance docTypeRelInst = null;
        if (query.next()) {
            docTypeRelInst = query.getCurrentValue();
        }

        final Update update = new Update(_parameter.getCallInstance());
        if (deducible) {
            update.add(CISales.PettyCashReceipt.Contact, Instance.get(contact));
            update.add(CISales.PettyCashReceipt.Name, docName);

            final Update relUpdate;
            if (docTypeRelInst != null && docTypeRelInst.isValid()) {
                relUpdate = new Update(docTypeRelInst);
            } else {
                relUpdate = new Insert(CISales.Document2DocumentType);
                relUpdate.add(CISales.Document2DocumentType.DocumentLink, _parameter.getCallInstance());
            }
            relUpdate.add(CISales.Document2DocumentType.DocumentTypeLink, Instance.get(docType));
            relUpdate.execute();
        } else {
            update.add(CISales.PettyCashReceipt.Contact, (Object) null);
            if (docTypeRelInst != null && docTypeRelInst.isValid()) {
                new Delete(docTypeRelInst).execute();
            }
            final PrintQuery print = new PrintQuery(_parameter.getCallInstance());
            final SelectBuilder posSel = SelectBuilder.get().linkfrom(CISales.AccountPettyCash2PettyCashReceipt,
                            CISales.AccountPettyCash2PettyCashReceipt.ToLink)
                            .attribute(CISales.AccountPettyCash2PettyCashReceipt.Position);
            final SelectBuilder nameSel = SelectBuilder.get().linkfrom(CISales.AccountPettyCash2PettyCashReceipt,
                            CISales.AccountPettyCash2PettyCashReceipt.ToLink)
                            .linkto(CISales.AccountPettyCash2PettyCashReceipt.FromLink)
                            .attribute(CISales.AccountPettyCash.Name);
            print.addSelect(posSel, nameSel);
            print.execute();
            update.add(CISales.PettyCashReceipt.Name, print.getSelect(nameSel) + " " + print.getSelect(posSel));
        }
        update.add(CISales.PettyCashReceipt.Date, date);
        update.execute();

        final PrintQuery print = new PrintQuery(_parameter.getCallInstance());
        final SelectBuilder selActionInst = SelectBuilder.get()
                        .linkfrom(CISales.ActionDefinitionPettyCashReceipt2Document.ToLink)
                        .linkto(CISales.ActionDefinitionPettyCashReceipt2Document.FromLink).instance();
        final SelectBuilder selRelInst = SelectBuilder.get()
                        .linkfrom(CISales.ActionDefinitionPettyCashReceipt2Document.ToLink).instance();
        print.addSelect(selActionInst, selRelInst);
        print.execute();
        final Instance currActionInst = print.getSelect(selActionInst);
        if (!actionInst.equals(currActionInst)) {
            final Instance relInst = print.getSelect(selRelInst);
            final Update actionUpdate;
            if (currActionInst != null && currActionInst.isValid()) {
                actionUpdate = new Update(relInst);
            } else {
                actionUpdate = new Insert(CISales.ActionDefinitionPettyCashReceipt2Document);
                actionUpdate.add(CISales.ActionDefinitionPettyCashReceipt2Document.ToLink,
                                _parameter.getCallInstance());
            }
            actionUpdate.add(CISales.ActionDefinitionPettyCashReceipt2Document.FromLink, actionInst);
            actionUpdate.execute();
        }
        final File file = createReport(_parameter, new EditedDoc(update.getInstance()));
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    @Override
    public Return dropDown4DocumentType(final Parameter _parameter)
        throws EFapsException
    {
        return new org.efaps.esjp.common.uiform.Field()
        {

            @Override
            protected void updatePositionList(final Parameter _parameter,
                                              final List<DropDownPosition> _values)
                throws EFapsException
            {
                final DropDownPosition ddPos = new DropDownPosition("NONE",
                                DBProperties.getProperty(PettyCashReceipt.class.getName() + ".NONEPosition.Label"));
                _values.add(0, ddPos);
            }
        } .getOptionListFieldValue(_parameter);
    }

    /**
     * The Class EvaluatePositionWarning.
     */
    public static class EvaluatePositionWarning
        extends AbstractWarning
    {

        /**
         * Instantiates a new evaluate position warning.
         */
        public EvaluatePositionWarning()
        {
            setError(true);
        }
    }

    /**
     * The Class EvaluateDeducibleDocWarning.
     */
    public static class EvaluateDeducibleDocWarning
        extends AbstractWarning
    {

        /**
         * Instantiates a new evaluate deducible doc warning.
         */
        public EvaluateDeducibleDocWarning()
        {
            setError(true);
        }
    }

    /**
     * The Class EvaluateNotDeducibleDocWarning.
     */
    public static class EvaluateNotDeducibleDocWarning
        extends AbstractWarning
    {

        /**
         * Instantiates a new evaluate not deducible doc warning.
         */
        public EvaluateNotDeducibleDocWarning()
        {
            setError(true);
        }
    }

    /**
     * The Class EvaluateBalanceAccountDocWarning.
     */
    public static class EvaluateBalanceAccountDocWarning
        extends AbstractWarning
    {

        /**
         * Instantiates a new evaluate balance account doc warning.
         */
        public EvaluateBalanceAccountDocWarning()
        {
        }
    }

}
