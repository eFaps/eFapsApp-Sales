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

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
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
import org.efaps.esjp.contacts.Contacts;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.ui.wicket.util.DateUtil;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("39feb877-6310-4170-816d-173f89347e3d")
@EFapsApplication("eFapsApp-Sales")
public abstract class PaymentCheck_Base
    extends AbstractPaymentIn
{

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = createDoc(_parameter);
        connectIncomingCheck(_parameter, createdDoc);
        createPayment(_parameter, createdDoc);
        executeAutomation(_parameter, createdDoc);
        final Return ret = new Return();
        final File file = createReport(_parameter, createdDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    /**
     * Connect incoming check.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _createdDoc the created doc
     * @throws EFapsException on error
     */
    protected void connectIncomingCheck(final Parameter _parameter,
                                        final CreatedDoc _createdDoc)
        throws EFapsException
    {
        if (_parameter.getInstance() != null && _parameter.getInstance().getType().isCIType(CISales.IncomingCheck)) {
            final Insert insert = new Insert(CISales.IncomingCheck2PaymentCheck);
            insert.add(CISales.IncomingCheck2PaymentCheck.FromLink, _parameter.getInstance());
            insert.add(CISales.IncomingCheck2PaymentCheck.ToLink, _createdDoc.getInstance());
            insert.execute();

            final Update update = new Update(_parameter.getInstance());
            update.add(CISales.IncomingCheck.Status, Status.find(CISales.IncomingCheckStatus.Closed));
            update.execute();
        }
    }

    @Override
    protected void add2DocCreate(final Parameter _parameter,
                                 final Insert _insert,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        super.add2DocCreate(_parameter, _insert, _createdDoc);

        final String issued = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.PaymentCheck.Issued.name));
        if (issued != null) {
            _insert.add(CISales.PaymentCheck.Issued, issued);
            _createdDoc.getValues().put(getFieldName4Attribute(_parameter, CISales.PaymentCheck.Issued.name), issued);
        }
    }

    /**
     * Creates the with out doc.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return createWithOutDoc(final Parameter _parameter)
        throws EFapsException
    {
        createDoc(_parameter);
        return new Return();
    }

    /**
     * Adds the doc2 payment doc.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return addDoc2PaymentDoc(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = getCreateDoc2addPayment(_parameter);
        createPayment(_parameter, createdDoc);
        final Return ret = new Return();
        final File file = createReport(_parameter, createdDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    /**
     * Gets the creates the doc2add payment.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the creates the doc2add payment
     * @throws EFapsException on error
     */
    protected CreatedDoc getCreateDoc2addPayment(final Parameter _parameter)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_parameter.getInstance());
        print.addAttribute(CISales.PaymentCheck.Name, CISales.PaymentCheck.RateCurrencyLink, CISales.PaymentCheck.Date);
        print.execute();
        final String name = print.<String>getAttribute(CISales.PaymentCheck.Name);
        final Long curId = print.<Long>getAttribute(CISales.PaymentCheck.RateCurrencyLink);
        final DateTime date = print.<DateTime>getAttribute(CISales.PaymentCheck.Date);

        final CreatedDoc createDoc = new CreatedDoc(_parameter.getInstance());
        createDoc.getValues().put(getFieldName4Attribute(_parameter, CISales.PaymentCheck.Name.name), name);
        createDoc.getValues().put(getFieldName4Attribute(_parameter, CISales.PaymentCheck.RateCurrencyLink.name),
                        curId);
        createDoc.getValues().put(getFieldName4Attribute(_parameter, CISales.PaymentCheck.Date.name), date);

        return createDoc;
    }

    /**
     * Drop down field value4 account.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return dropDownFieldValue4Account(final Parameter _parameter)
        throws EFapsException
    {
        return new org.efaps.esjp.common.uiform.Field()
        {

            @Override
            protected void add2QueryBuilder4List(final Parameter _parameter,
                                                 final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final PrintQuery print = new PrintQuery(_parameter.getInstance());
                print.addAttribute(CISales.PaymentCheck.RateCurrencyLink);
                print.execute();
                final Long curId = print.<Long>getAttribute(CISales.PaymentCheck.RateCurrencyLink);

                _queryBldr.addWhereAttrEqValue(CISales.AccountCashDesk.CurrencyLink, curId);
            }
        }.dropDownFieldValue(_parameter);
    }

    /**
     * Deferred multi print.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return deferredMultiPrint(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final List<Instance> instances = new ArrayList<>();
        final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.PaymentDocumentAbstract.Date, CISales.PaymentDocumentAbstract.DueDate);
        multi.execute();
        while (multi.next()) {
            final Instance inst = multi.getCurrentInstance();
            final DateTime date = multi.<DateTime>getAttribute(CISales.PaymentDocumentAbstract.Date);
            final DateTime dueDate = multi.<DateTime>getAttribute(CISales.PaymentDocumentAbstract.DueDate);
            if (!(date.compareTo(dueDate) == 0)) {
                instances.add(inst);
            }
        }
        ret.put(ReturnValues.VALUES, instances);
        return ret;
    }

    /**
     * Gets the java script ui value.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the java script ui value
     * @throws EFapsException on error
     */
    public Return getJavaScriptUIValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        if (_parameter.getInstance() != null && _parameter.getInstance().getType().isCIType(CISales.IncomingCheck)) {
            final StringBuilder js = new StringBuilder();
            final PrintQuery print = new PrintQuery(_parameter.getInstance());
            final SelectBuilder selContactInst = SelectBuilder.get().linkto(CISales.IncomingCheck.Contact).instance();
            final SelectBuilder selContactName = SelectBuilder.get().linkto(CISales.IncomingCheck.Contact).attribute(
                            CIContacts.Contact.Name);
            print.addSelect(selContactInst, selContactName);
            print.addAttribute(CISales.IncomingCheck.Name, CISales.IncomingCheck.Date, CISales.IncomingCheck.DueDate,
                            CISales.IncomingCheck.RateNetTotal);
            print.execute();

            final Instance contactInst = print.getSelect(selContactInst);
            final String contactName = print.getSelect(selContactName);

            final String amount = NumberFormatter.get().getTwoDigitsFormatter().format(
                            print.<BigDecimal>getAttribute(CISales.IncomingCheck.RateNetTotal));

            js.append(getSetFieldValue(0, CIFormSales.Sales_PaymentCheckForm.name.name, print.<String>getAttribute(
                            CISales.IncomingCheck.Name)))
                .append(getSetFieldValue(0, CIFormSales.Sales_PaymentCheckForm.contact.name, contactInst.getOid(),
                                            contactName))
                .append(getSetFieldValue(0, CIFormSales.Sales_PaymentCheckForm.contactData.name,
                                new Contacts().getFieldValue4Contact(contactInst, false)))
                .append(getSetFieldValue(0, CIFormSales.Sales_PaymentCheckForm.amount.name, amount))
                .append(getSetFieldValue(0, CIFormSales.Sales_PaymentCheckForm.account.name,
                        _parameter.getParameterValue(CIFormSales.Sales_IncomingCheckSelectAccountForm.account.name)))
        //      .append(getSetFieldValue(0, CIFormSales.Sales_PaymentCheckForm.date.name + "_eFapsDate",
       //                    DateUtil.getDate4Parameter(print.<DateTime>getAttribute(CISales.IncomingCheck.DueDate))))
                .append(getSetFieldValue(0, CIFormSales.Sales_PaymentCheckForm.dueDate.name + "_eFapsDate",
                            DateUtil.getDate4Parameter(print.<DateTime>getAttribute(CISales.IncomingCheck.Date))));

            // store that the amount was set from the User
            ParameterUtil.setParameterValues(_parameter, "amount", amount);
            Context.getThreadContext().setSessionAttribute(AbstractPaymentDocument_Base.CHANGE_AMOUNT, true);

            final QueryBuilder queryBldr = new QueryBuilder(CISales.IncomingCheck2ApplyDocument);
            queryBldr.addWhereAttrEqValue(CISales.IncomingCheck2ApplyDocument.FromLink, _parameter.getInstance());
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selDocInst = SelectBuilder.get().linkto(CISales.IncomingCheck2ApplyDocument.ToLink)
                            .instance();
            final SelectBuilder selDocName = SelectBuilder.get().linkto(CISales.IncomingCheck2ApplyDocument.ToLink)
                            .attribute(CISales.DocumentSumAbstract.Name);
            multi.addSelect(selDocInst, selDocName);
            multi.execute();
            final Collection<Map<String, Object>> values = new ArrayList<>();
            while (multi.next()) {
                final Instance docInst = multi.getSelect(selDocInst);
                final Map<String, Object> map = getPositionUpdateMap(_parameter, docInst, true);
                values.add(map);
            }

            for (final Entry<String, Object> entry: getSumUpdateMap(_parameter, values, true).entrySet()) {
                js.append(getSetFieldValue(0, entry.getKey(), String.valueOf(entry.getValue())));
            }

            js.append(getTableRemoveScript(_parameter, CIFormSales.Sales_PaymentCheckForm.paymentTable.name))
                .append(getTableAddNewRowsScript(_parameter, CIFormSales.Sales_PaymentCheckForm.paymentTable.name,
                                values, null));
            retVal.put(ReturnValues.SNIPLETT, InterfaceUtils.wrappInScriptTag(_parameter, js, true, 1500));
        }
        return retVal;
    }
}
