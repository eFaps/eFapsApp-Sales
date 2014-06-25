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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
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
import org.efaps.esjp.contacts.Contacts;
import org.efaps.esjp.erp.AbstractWarning;
import org.efaps.esjp.erp.IWarning;
import org.efaps.esjp.sales.Account;
import org.efaps.esjp.sales.Calculator;
import org.efaps.esjp.sales.Transaction;
import org.efaps.esjp.sales.document.Validation_Base.InvalidNameWarning;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("403479a4-9335-4fce-91ee-2c0f22cf525e")
@EFapsRevision("$Rev$")
public abstract class FundsToBeSettledReceipt_Base
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
        final CreatedDoc createdDoc = createDoc(_parameter);
        createPositions(_parameter, createdDoc);
        connect2DocumentType(_parameter, createdDoc);
        connect2Account(_parameter, createdDoc);
        createTransaction(_parameter, createdDoc);
        connect2Object(_parameter, createdDoc);
        return new Return();
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
        final Insert insert = new Insert(CISales.AccountFundsToBeSettled2FundsToBeSettledReceipt);
        insert.add(CISales.AccountFundsToBeSettled2FundsToBeSettledReceipt.FromLink, _parameter.getInstance());
        insert.add(CISales.AccountFundsToBeSettled2FundsToBeSettledReceipt.ToLink, _createdDoc.getInstance());
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
        final Insert payInsert = new Insert(CISales.Payment);
        payInsert.add(CISales.Payment.Date, _createdDoc.getValue(CISales.DocumentSumAbstract.Date.name));
        payInsert.add(CISales.Payment.CreateDocument, _createdDoc.getInstance());
        payInsert.execute();

        final Insert transInsert = new Insert(CISales.TransactionOutbound);
        transInsert.add(CISales.TransactionOutbound.Amount,
                        _createdDoc.getValue(CISales.DocumentSumAbstract.RateCrossTotal.name));
        transInsert.add(CISales.TransactionOutbound.CurrencyId,
                        _createdDoc.getValue(CISales.DocumentSumAbstract.RateCurrencyId.name));
        transInsert.add(CISales.TransactionOutbound.Payment, payInsert.getInstance());
        transInsert.add(CISales.TransactionOutbound.Account, _parameter.getInstance());
        transInsert.add(CISales.TransactionOutbound.Description,
                        _createdDoc.getValue(CISales.DocumentSumAbstract.Note.name));
        transInsert.add(CISales.TransactionOutbound.Date, _createdDoc.getValue(CISales.DocumentSumAbstract.Date.name));
        transInsert.execute();
    }

    /**
     * Update the transaction for the PettyCash.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _editedDoc doc the transaction is connected to
     * @param _prevAmount previous amount
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
            final SelectBuilder accSel = SelectBuilder.get().linkto(CISales.TransactionAbstract.Account).instance();
            final SelectBuilder curSel = SelectBuilder.get().linkto(CISales.TransactionAbstract.CurrencyId).instance();
            multi.addSelect(accSel, curSel);
            multi.addAttribute(CISales.TransactionOutbound.Amount);
            multi.executeWithoutAccessCheck();
            if (multi.next()) {
                final BigDecimal amount = multi.<BigDecimal>getAttribute(CISales.TransactionAbstract.Amount);
                final Instance accountInst = multi.<Instance>getSelect(accSel);
                final Instance currencyInst = multi.<Instance>getSelect(curSel);
                final BigDecimal newAmount = (BigDecimal) _editedDoc
                                .getValue(CISales.DocumentSumAbstract.RateCrossTotal.name);
                if (newAmount.compareTo(amount) != 0) {
                    final Update update = new Update(multi.getCurrentInstance());
                    update.add(CISales.TransactionOutbound.Amount,
                                    _editedDoc.getValue(CISales.DocumentSumAbstract.RateCrossTotal.name));
                    update.add(CISales.TransactionOutbound.CurrencyId,
                                    _editedDoc.getValue(CISales.DocumentSumAbstract.RateCurrencyId.name));
                    update.add(CISales.TransactionOutbound.Description,
                                    _editedDoc.getValue(CISales.DocumentSumAbstract.Note.name));
                    update.execute();
                    new Transaction().updateBalance(_parameter, accountInst, currencyInst, newAmount.subtract(amount));
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
        if (ret == null || (ret != null && ret.isEmpty())) {
            final PrintQuery print = new PrintQuery(_parameter.getInstance());
            print.addAttribute(CISales.AccountFundsToBeSettled.Name);
            print.execute();
            final String accName = print.<String>getAttribute(CISales.AccountFundsToBeSettled.Name);
            ret =  accName + " " + (new Account().getMaxPosition(_parameter, _parameter.getInstance()) + 1);
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return html for display and true or false
     * @throws EFapsException on errro
     */
    @Override
    public Return validate(final Parameter _parameter)
        throws EFapsException
    {
        final Validation validation = new Validation()
        {

            @Override
            protected List<IWarning> validate(final Parameter _parameter,
                                              final List<IWarning> _warnings)
                throws EFapsException
            {
                final List<IWarning> ret = super.validate(_parameter, _warnings);
                ret.addAll(validateFundsToBeSettled(_parameter, ret));
                return ret;
            }
        };
        return validation.validate(_parameter, this);
    }

    public List<IWarning> validateFundsToBeSettled(final Parameter _parameter,
                                                   final List<IWarning> _ret)
        throws EFapsException
    {
        final List<IWarning> ret = new ArrayList<IWarning>();
        final boolean evaluatePositions = !"false".equalsIgnoreCase(getProperty(_parameter, "EvaluatePositions"));
        // first check the positions
        final List<Calculator> calcList = analyseTable(_parameter, null);
        if (evaluatePositions
                        && (calcList.isEmpty() || getNetTotal(_parameter, calcList).compareTo(BigDecimal.ZERO) == 0)) {
            ret.add(new EvaluatePositionWarning());
        } else {
            if (evalDeducible(_parameter)) {
                final String name = _parameter
                                .getParameterValue(CIFormSales.Sales_FundsToBeSettledReceiptForm.name4create.name);
                final Instance contactInst = Instance.get(_parameter
                                .getParameterValue(CIFormSales.Sales_FundsToBeSettledReceiptForm.contact.name));
                if ((name == null || !name.isEmpty()) && !contactInst.isValid()) {
                    ret.add(new EvaluateDeducibleDocWarning());
                }
            } else {
                final String name = _parameter
                                .getParameterValue(CIFormSales.Sales_FundsToBeSettledReceiptForm.name4create.name);
                final String contact = _parameter
                                .getParameterValue(CIFormSales.Sales_FundsToBeSettledReceiptForm.contact.name);
                if ((name != null && !name.isEmpty()) || (contact != null && !contact.isEmpty())) {
                    ret.add(new EvaluateNotDeducibleDocWarning());
                } else {
                    final Iterator<IWarning> iterator = _ret.iterator();
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
                multi.addAttribute(CISales.Balance.Amount);
                multi.execute();
                BigDecimal amountBal = BigDecimal.ZERO;
                while (multi.next()) {
                    amountBal = amountBal.add(multi.<BigDecimal>getAttribute(CISales.Balance.Amount));
                }
                if (getNetTotal(_parameter, calcList).compareTo(amountBal) == 1) {
                    ret.add(new EvaluateBalanceAccountDocWarning());
                }
            }
        }
        return ret;
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
        if (!evalDeducible(_parameter)) {
            final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
            final Map<String, Object> map = new HashMap<String, Object>();
            list.add(map);
            map.put(CIFormSales.Sales_FundsToBeSettledReceiptForm.name4create.name, "");
            map.put(CIFormSales.Sales_FundsToBeSettledReceiptForm.contactData.name, "");
            map.put(CIFormSales.Sales_FundsToBeSettledReceiptForm.contact.name, new String[] { "", "" });
            retVal.put(ReturnValues.VALUES, list);
        }
        return retVal;
    }

    /**
     * @param _parameter Paramater as passed by the eFaps APi
     * @return true if deducible else false
     */
    protected boolean evalDeducible(final Parameter _parameter)
    {
        return !"NONE".equals(_parameter.getParameterValue(CIFormSales.Sales_FundsToBeSettledReceiptForm.documentType.name));
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
        final EditedDoc editDoc = editDoc(_parameter);
        updatePositions(_parameter, editDoc);
        updateTransaction(_parameter, editDoc);
        return new Return();
    }
    /**
     * @param _parameter parameter as passed by the eFaps API
     * @return Return contiaining javascript
     * @throws EFapsException on error
     */
    public Return getJavaScriptUIValue4EditJustification(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Instance inst = _parameter.getCallInstance();
        final StringBuilder js = new StringBuilder();
        js.append("<script type=\"text/javascript\">")
                        .append("Wicket.Event.add(window, \"domready\", function(event) {");

        if (inst.isValid()) {
            final PrintQuery print = new PrintQuery(inst);
            final SelectBuilder selContInst = new SelectBuilder().linkto(CISales.FundsToBeSettledReceipt.Contact).instance();
            final SelectBuilder selContName = new SelectBuilder().linkto(CISales.FundsToBeSettledReceipt.Contact)
                            .attribute(CIContacts.Contact.Name);
            print.addSelect(selContInst, selContName);
            print.addAttribute(CISales.FundsToBeSettledReceipt.Name);

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
                                        CIFormSales.Sales_FundsToBeSettledReceiptJustificationEditForm.documentType.name,
                                        multi.<Instance>getSelect(selDocTypeInst).getOid()));
                    }
                    final String info = new Contacts().getFieldValue4Contact(contInst, false);
                    final String contName = print.<String>getSelect(selContName);
                    final String name = print.<String>getAttribute(CISales.FundsToBeSettledReceipt.Name);

                    js.append(getSetFieldValue(0,
                                    CIFormSales.Sales_FundsToBeSettledReceiptJustificationEditForm.contactData.name, info))
                        .append(getSetFieldValue(0,
                                    CIFormSales.Sales_FundsToBeSettledReceiptJustificationEditForm.contact.name
                                                                    + "AutoComplete", contName))
                        .append(getSetFieldValue(0,
                                    CIFormSales.Sales_FundsToBeSettledReceiptJustificationEditForm.contact.name,
                                                    contInst.getOid()))
                        .append(getSetFieldValue(0,
                                    CIFormSales.Sales_FundsToBeSettledReceiptJustificationEditForm.name4create.name, name));
                }
            }
            js.append(" });").append("</script>");
        }
        ret.put(ReturnValues.SNIPLETT, js.toString());
        return ret;
    }

    /**
     * @param _parameter parameter as passed by the eFaps API
     * @return new empty Return
     * @throws EFapsException on error
     */
    public Return editJustification(final Parameter _parameter)
        throws EFapsException
    {
        final boolean deducible = evalDeducible(_parameter);

        final String contact = _parameter
                        .getParameterValue(CIFormSales.Sales_FundsToBeSettledReceiptJustificationEditForm.contact.name);
        final String docName = _parameter
                        .getParameterValue(CIFormSales.Sales_FundsToBeSettledReceiptJustificationEditForm.name4create.name);
        final String docType = _parameter
                        .getParameterValue(CIFormSales.Sales_FundsToBeSettledReceiptJustificationEditForm.documentType.name);

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
            update.add(CISales.FundsToBeSettledReceipt.Contact, Instance.get(contact));
            update.add(CISales.FundsToBeSettledReceipt.Name, docName);

            Update relUpdate;
            if (docTypeRelInst != null && docTypeRelInst.isValid()) {
                relUpdate = new Update(docTypeRelInst);
            } else {
                relUpdate = new Insert(CISales.Document2DocumentType);
                relUpdate.add(CISales.Document2DocumentType.DocumentLink, _parameter.getCallInstance());
            }
            relUpdate.add(CISales.Document2DocumentType.DocumentTypeLink, Instance.get(docType));
            relUpdate.execute();
        } else {
            update.add(CISales.FundsToBeSettledReceipt.Contact, (Object) null);
            if (docTypeRelInst != null && docTypeRelInst.isValid()) {
                new Delete(docTypeRelInst).execute();
            }
            final PrintQuery print = new PrintQuery(_parameter.getCallInstance());
            final SelectBuilder posSel = SelectBuilder.get().linkfrom(CISales.AccountFundsToBeSettled2FundsToBeSettledReceipt,
                            CISales.AccountFundsToBeSettled2FundsToBeSettledReceipt.ToLink)
                            .attribute(CISales.AccountFundsToBeSettled2FundsToBeSettledReceipt.Position);
            final SelectBuilder nameSel = SelectBuilder.get().linkfrom(CISales.AccountFundsToBeSettled2FundsToBeSettledReceipt,
                            CISales.AccountFundsToBeSettled2FundsToBeSettledReceipt.ToLink)
                            .linkto(CISales.AccountFundsToBeSettled2FundsToBeSettledReceipt.FromLink)
                            .attribute(CISales.AccountFundsToBeSettled.Name);
            print.addSelect(posSel, nameSel);
            print.execute();
            update.add(CISales.FundsToBeSettledReceipt.Name, print.getSelect(nameSel) + " " + print.getSelect(posSel));
        }
        update.execute();
        return new Return();
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
                                DBProperties.getProperty(FundsToBeSettledReceipt.class.getName()
                                                + ".NONEPosition.Label"));
                _values.add(0, ddPos);
            };
        }.dropDownFieldValue(_parameter);
    }

    public static class EvaluatePositionWarning
        extends AbstractWarning
    {

        public EvaluatePositionWarning()
        {
            setError(true);
        }
    }

    public static class EvaluateDeducibleDocWarning
        extends AbstractWarning
    {

        public EvaluateDeducibleDocWarning()
        {
            setError(true);
        }
    }

    public static class EvaluateNotDeducibleDocWarning
        extends AbstractWarning
    {

        public EvaluateNotDeducibleDocWarning()
        {
            setError(true);
        }
    }

    public static class EvaluateBalanceAccountDocWarning
        extends AbstractWarning
    {

        public EvaluateBalanceAccountDocWarning()
        {

        }
    }
}
