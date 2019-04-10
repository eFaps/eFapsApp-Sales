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
import java.util.Map;
import java.util.Map.Entry;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.contacts.Contacts;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("903c028c-1b3a-47fc-adee-792e7f9ddcfc")
@EFapsApplication("eFapsApp-Sales")
public abstract class PaymentRetention_Base
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

    @Override
    protected Status getStatus4Create()
        throws EFapsException
    {
        Status ret = super.getStatus4Create();
        if (Sales.PAYMENT_RETENTION_CREATESTATUS.exists()) {
            ret = Status.find(CISales.PaymentRetentionStatus, Sales.PAYMENT_RETENTION_CREATESTATUS.get());
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return edit4Draft(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final EditedDoc editedDoc = editDoc(_parameter);
        updatePayment(_parameter, editedDoc);
        final File file = createReport(_parameter, editedDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    @Override
    protected String getDocName4Create(final Parameter _parameter)
        throws EFapsException
    {
        final String name;
        final Instance instance = Instance.get(_parameter.getParameterValue("name"));

        if (instance.isValid()) {
            final PrintQuery print = new PrintQuery(instance);
            print.addAttribute(CISales.DocumentAbstract.Name);
            print.execute();

            name = print.<String>getAttribute(CISales.DocumentAbstract.Name);
        } else {
            name = super.getDocName4Create(_parameter);
        }
        return name;
    }

    @Override
    protected void add2QueryBldr4PickerMultiPrint(final Parameter _parameter,
                                                  final QueryBuilder _queryBldr)
        throws EFapsException
    {
        super.add2QueryBldr4PickerMultiPrint(_parameter, _queryBldr);
        add2QueryBldr4AutoComplete4CreateDocument(_parameter, _queryBldr);
    }

    @Override
    protected void add2QueryBldr4AutoComplete4CreateDocument(final Parameter _parameter,
                                                             final QueryBuilder _queryBldr)
        throws EFapsException
    {
        final Instance incRetCertif = Instance.get(_parameter
                        .getParameterValue(CIFormSales.Sales_PaymentRetentionForm.name.name));
        if (incRetCertif.isValid()) {
            final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.IncomingRetentionCertificate2Document);
            attrQueryBldr.addWhereAttrEqValue(CISales.IncomingRetentionCertificate2Document.FromLink, incRetCertif);
            final AttributeQuery attrQuery = attrQueryBldr
                            .getAttributeQuery(CISales.IncomingRetentionCertificate2Document.ToAbstractLink);
            _queryBldr.addWhereAttrInQuery(CIERP.DocumentAbstract.ID, attrQuery);
        }
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
        if (_parameter.getInstance() != null && _parameter.getInstance().getType().isCIType(
                        CISales.IncomingRetentionCertificate)) {
            final StringBuilder js = new StringBuilder();
            final PrintQuery print = new PrintQuery(_parameter.getInstance());
            final SelectBuilder selContactInst = SelectBuilder.get().linkto(
                            CISales.IncomingRetentionCertificate.Contact).instance();
            final SelectBuilder selContactName = SelectBuilder.get().linkto(
                            CISales.IncomingRetentionCertificate.Contact).attribute(CIContacts.Contact.Name);
            print.addSelect(selContactInst, selContactName);
            print.addAttribute(CISales.IncomingRetentionCertificate.Name, CISales.IncomingRetentionCertificate.Date,
                            CISales.IncomingRetentionCertificate.RateNetTotal);
            print.execute();

            final Instance contactInst = print.getSelect(selContactInst);
            final String contactName = print.getSelect(selContactName);

            final String amount = NumberFormatter.get().getTwoDigitsFormatter().format(print.<BigDecimal>getAttribute(
                            CISales.IncomingRetentionCertificate.RateNetTotal));

            js.append(getSetFieldValue(0, CIFormSales.Sales_PaymentRetentionForm.name.name,
                            print.getCurrentInstance().getOid(),
                            print.<String>getAttribute(CISales.IncomingRetentionCertificate.Name)))
                .append(getSetFieldValue(0, CIFormSales.Sales_PaymentRetentionForm.contact.name, contactInst.getOid(),
                                            contactName))
                .append(getSetFieldValue(0, CIFormSales.Sales_PaymentRetentionForm.contactData.name,
                                                            new Contacts().getFieldValue4Contact(contactInst, false)))
                .append(getSetFieldValue(0, CIFormSales.Sales_PaymentRetentionForm.amount.name, amount))
                .append(getSetFieldValue(0, CIFormSales.Sales_PaymentRetentionForm.account.name, _parameter
                                    .getParameterValue(CIFormSales.Sales_IncomingCheckSelectAccountForm.account.name)));
            // store that the amount was set from the User
            ParameterUtil.setParameterValues(_parameter, "amount", amount);
            Context.getThreadContext().setSessionAttribute(AbstractPaymentDocument_Base.CHANGE_AMOUNT, true);

            final QueryBuilder queryBldr = new QueryBuilder(CISales.IncomingRetentionCertificate2Document);
            queryBldr.addWhereAttrEqValue(CISales.IncomingRetentionCertificate2Document.FromLink,
                            _parameter.getInstance());
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selDocInst = SelectBuilder.get()
                            .linkto(CISales.IncomingRetentionCertificate2Document.ToAbstractLink).instance();
            final SelectBuilder selDocName = SelectBuilder.get()
                            .linkto(CISales.IncomingRetentionCertificate2Document.ToAbstractLink)
                            .attribute(CISales.DocumentSumAbstract.Name);
            multi.addSelect(selDocInst, selDocName);
            multi.execute();
            final Collection<Map<String, Object>> values = new ArrayList<>();
            while (multi.next()) {
                final Instance docInst = multi.getSelect(selDocInst);
                final Map<String, Object> map = getPositionUpdateMap(_parameter, docInst, true);
                values.add(map);
            }

            for (final Entry<String, Object> entry : getSumUpdateMap(_parameter, values, true).entrySet()) {
                js.append(getSetFieldValue(0, entry.getKey(), String.valueOf(entry.getValue())));
            }

            js.append(getTableRemoveScript(_parameter, CIFormSales.Sales_PaymentRetentionForm.paymentTable.name))
                            .append(getTableAddNewRowsScript(_parameter,
                                            CIFormSales.Sales_PaymentRetentionForm.paymentTable.name, values, null));
            retVal.put(ReturnValues.SNIPLETT, InterfaceUtils.wrappInScriptTag(_parameter, js, true, 1500));
        }
        return retVal;
    }

    @Override
    public CIType getCIType()
        throws EFapsException
    {
        return CISales.PaymentRetention;
    }

}
