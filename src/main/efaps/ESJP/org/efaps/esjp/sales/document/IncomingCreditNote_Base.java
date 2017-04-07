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

package org.efaps.esjp.sales.document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.field.Field;
import org.efaps.ci.CIType;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.contacts.Contacts;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("7eda4cc9-77ac-4050-a76a-fbc5ba973c04")
@EFapsApplication("eFapsApp-Sales")
public abstract class IncomingCreditNote_Base
    extends AbstractDocumentSum
{

    /** The Constant REVISIONKEY. */
    public static final String REVISIONKEY = IncomingCreditNote.class.getName() + ".RevisionKey";

    /**
     * Method for create a new Incoming Credit Note.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = createDoc(_parameter);
        createPositions(_parameter, createdDoc);
        connect2DocumentType(_parameter, createdDoc);
        connect2Derived(_parameter, createdDoc);
        connect2Object(_parameter, createdDoc);
        return new Return();
    }

    /**
     * Method to create IncomingCreditNote into tree of the AccountPettyCash.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create4Account(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = createDoc(_parameter);
        createPositions(_parameter, createdDoc);
        connect2DocumentType(_parameter, createdDoc);
        connect2Derived(_parameter, createdDoc);
        connect2Object(_parameter, createdDoc);
        connect2Account(_parameter, createdDoc);
        createTransaction(_parameter, createdDoc);
        return new Return();
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
        final Type relType;
        if (isUUID(getProperty(_parameter, "ConnectAccountType"))) {
            relType = Type.get(UUID.fromString(getProperty(_parameter, "ConnectAccountType")));
        } else {
            relType = Type.get(getProperty(_parameter, "ConnectAccountType"));
        }
        final Insert insert = new Insert(relType);
        insert.add(CISales.Account2DocumentAbstract.FromLinkAbstract, _parameter.getInstance());
        insert.add(CISales.Account2DocumentAbstract.ToLinkAbstract, _createdDoc.getInstance());
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

        final Insert transInsert = new Insert(CISales.TransactionInbound);
        transInsert.add(CISales.TransactionInbound.Amount,
                        _createdDoc.getValue(CISales.DocumentSumAbstract.RateCrossTotal.name));
        transInsert.add(CISales.TransactionInbound.CurrencyId,
                        _createdDoc.getValue(CISales.DocumentSumAbstract.RateCurrencyId.name));
        transInsert.add(CISales.TransactionInbound.Payment, payInsert.getInstance());
        transInsert.add(CISales.TransactionInbound.Account, _parameter.getInstance());
        transInsert.add(CISales.TransactionInbound.Description,
                        _createdDoc.getValue(CISales.DocumentSumAbstract.Note.name));
        transInsert.add(CISales.TransactionInbound.Date, _createdDoc.getValue(CISales.DocumentSumAbstract.Date.name));
        transInsert.execute();
    }

    @Override
    protected void add2DocCreate(final Parameter _parameter,
                                 final Insert _insert,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        if (_parameter.getInstance() == null
                        || !InstanceUtils.isKindOf(_parameter.getInstance(), CIERP.DocumentAbstract)) {
            final String seqKey = Sales.INCOMINGCREDITNOTE_REVSEQ.get();
            final NumberGenerator numgen = isUUID(seqKey)
                            ? NumberGenerator.get(UUID.fromString(seqKey))
                            : NumberGenerator.get(seqKey);
            if (numgen != null) {
                final String revision = numgen.getNextVal();
                Context.getThreadContext().setSessionAttribute(IncomingCreditNote_Base.REVISIONKEY, revision);
                _insert.add(CISales.IncomingCreditNote.Revision, revision);
            }
        }
    }

    /**
     * Auto complete4 petty cash receipt.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return autoComplete4PettyCashReceipt(final Parameter _parameter)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CISales.PettyCashReceipt);
        queryBldr.addType(CISales.FundsToBeSettledReceipt);
        return new IncomingCreditNote()
        {

            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final Instance instance = _parameter.getInstance();

                if (instance != null && instance.isValid()
                                && (instance.getType().equals(CISales.AccountFundsToBeSettled.getType())
                                || instance.getType().equals(CISales.AccountPettyCash.getType()))) {
                    final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.AccountPettyCash2PettyCashBalance);
                    attrQueryBldr.addType(CISales.AccountFundsToBeSettled2FundsToBeSettledBalance);
                    attrQueryBldr.addWhereAttrEqValue(CISales.Account2DocumentAbstract.FromLinkAbstract, instance);
                    final AttributeQuery attrQuery = attrQueryBldr
                                    .getAttributeQuery(CISales.Account2DocumentAbstract.ToLinkAbstract);

                    final QueryBuilder queryBldr = new QueryBuilder(CISales.PettyCashBalance);
                    queryBldr.addType(CISales.FundsToBeSettledBalance);
                    queryBldr.addWhereAttrInQuery(CISales.AccountBalance.ID, attrQuery);
                    queryBldr.addWhereAttrEqValue(CISales.AccountBalance.StatusAbstract,
                                    Status.find(CISales.PettyCashBalanceStatus.Verified),
                                    Status.find(CISales.FundsToBeSettledBalanceStatus.Verified));
                    final AttributeQuery attrQuery2 = queryBldr.getAttributeQuery(CISales.AccountBalance.ID);

                    final QueryBuilder attrQueryBldr2 = new QueryBuilder(CISales.PettyCashBalance2PettyCashReceipt);
                    attrQueryBldr2.addType(CISales.FundsToBeSettledBalance2FundsToBeSettledReceipt);
                    attrQueryBldr2.addWhereAttrInQuery(CISales.Document2DocumentAbstract.FromAbstractLink, attrQuery2);
                    final AttributeQuery attrQuery3 = attrQueryBldr2
                                    .getAttributeQuery(CISales.Document2DocumentAbstract.ToAbstractLink);

                    final QueryBuilder attrQueryBldr3 = new QueryBuilder(CISales.AccountPettyCash2PettyCashReceipt);
                    attrQueryBldr3.addType(CISales.AccountFundsToBeSettled2FundsToBeSettledReceipt);
                    attrQueryBldr3.addWhereAttrEqValue(CISales.Account2DocumentAbstract.FromLinkAbstract, instance);
                    attrQueryBldr3.addWhereAttrNotInQuery(CISales.Account2DocumentAbstract.ToLinkAbstract, attrQuery3);
                    final AttributeQuery attrQuery4 = attrQueryBldr3
                                    .getAttributeQuery(CISales.Account2DocumentAbstract.ToLinkAbstract);

                    _queryBldr.addWhereAttrInQuery(CISales.DocumentSumAbstract.ID, attrQuery4);
                    _queryBldr.addWhereAttrNotIsNull(CISales.DocumentSumAbstract.Contact);
                } else {
                    super.add2QueryBldr(_parameter, _queryBldr);
                }
            };

        }.autoComplete4Doc(_parameter, queryBldr);
    }

    @Override
    protected List<Map<String, Object>> updateFields4Doc(final Parameter _parameter)
        throws EFapsException
    {
        List<Map<String, Object>> ret = new ArrayList<>();
        final Map<String, Object> map = new HashMap<>();

        final Instance instance = _parameter.getInstance();

        if (instance != null && instance.isValid()
                        && (instance.getType().equals(CISales.AccountPettyCash.getType())
                                        || instance.getType().equals(CISales.AccountFundsToBeSettled.getType()))) {
            final StringBuilder js = new StringBuilder();

            final Field field = (Field) _parameter.get(ParameterValues.UIOBJECT);
            final String fieldName = field.getName();
            final String currentOid = _parameter.getParameterValue(fieldName);
            final PrintQuery print = new PrintQuery(currentOid);

            final SelectBuilder selContactInst = new SelectBuilder()
                            .linkto(CISales.DocumentSumAbstract.Contact).instance();
            final SelectBuilder selContactName = new SelectBuilder()
                            .linkto(CISales.DocumentSumAbstract.Contact).attribute(CIContacts.Contact.Name);
            print.addSelect(selContactInst, selContactName);
            print.execute();

            final Instance contactInst = print.<Instance>getSelect(selContactInst);
            final String contactName = print.<String>getSelect(selContactName);
            final String contactData = new Contacts().getFieldValue4Contact(contactInst, false);

            js.append(getSetFieldValue(0, "contact", contactInst.getOid(), contactName)).append("\n")
                .append(getSetFieldValue(0, "contactData", contactData)).append("\n");
            InterfaceUtils.appendScript4FieldUpdate(map, js.toString());
            ret.add(map);
        } else {
            ret = super.updateFields4Doc(_parameter);
        }
        return ret;
    }

    @Override
    public CIType getCIType()
        throws EFapsException
    {
        return CISales.IncomingCreditNote;
    }

    /**
     * Create the TransactionDocument for this invoice.
     *
     * @param _parameter Parameter from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return createTransDocShadow(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = new TransactionDocument().createDocumentShadow(_parameter);
        final Insert insert = new Insert(CISales.IncomingCreditNote2TransactionDocumentShadowIn);
        insert.add(CISales.IncomingCreditNote2TransactionDocumentShadowIn.FromLink, _parameter.getInstance());
        insert.add(CISales.IncomingCreditNote2TransactionDocumentShadowIn.ToLink, createdDoc.getInstance());
        insert.execute();
        return new Return();
    }
}
