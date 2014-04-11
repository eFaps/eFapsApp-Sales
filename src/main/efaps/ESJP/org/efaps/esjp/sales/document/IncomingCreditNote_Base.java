/*
 * Copyright 2003 - 2013 The eFaps Team
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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;

import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.contacts.Contacts;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.SalesSettings;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("7eda4cc9-77ac-4050-a76a-fbc5ba973c04")
@EFapsRevision("$Rev$")
public abstract class IncomingCreditNote_Base
    extends DocumentSum
{

    public static final String REVISIONKEY = "org.efaps.esjp.sales.document.IncomingCreditNote.RevisionKey";

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
        return new Return();
    }

    public Return create4AccountPettyCash(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = createDoc(_parameter);
        createPositions(_parameter, createdDoc);
        connect2DocumentType(_parameter, createdDoc);
        connect2Derived(_parameter, createdDoc);
        connect2Account(_parameter, createdDoc);
        createTransaction(_parameter, createdDoc);
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
        final Insert insert = new Insert(CISales.AccountPettyCash2IncomingCreditNote);
        insert.add(CISales.AccountPettyCash2IncomingCreditNote.FromLink, _parameter.getInstance());
        insert.add(CISales.AccountPettyCash2IncomingCreditNote.ToLink, _createdDoc.getInstance());
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
        if (_parameter.getInstance() == null) {
            final SystemConfiguration config = Sales.getSysConfig();
            final Properties props = config.getAttributeValueAsProperties(SalesSettings.INCOMINGCREDITNOTESEQUENCE);

            final NumberGenerator numgen = NumberGenerator.get(UUID.fromString(props.getProperty("UUID")));
            if (numgen != null) {
                final String revision = numgen.getNextVal();
                Context.getThreadContext().setSessionAttribute(IncomingCreditNote_Base.REVISIONKEY, revision);
                _insert.add(CISales.IncomingCreditNote.Revision, revision);
            }
        }
    }

    @Override
    public Return autoComplete4IncomingInvoice(final Parameter _parameter)
        throws EFapsException
    {
        return new IncomingCreditNote()
        {

            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final Map<Integer, String> types = analyseProperty(_parameter, "Type");
                if (!types.isEmpty()) {
                    for (final Entry<Integer, String> entry : types.entrySet()) {
                        final Type type = Type.get(entry.getValue());
                        if (type != null) {
                            _queryBldr.addType(type);
                        }
                    }
                }
            };
        }.autoComplete4Doc(_parameter, CISales.IncomingInvoice.uuid, (Status[]) null);
    }

    @Override
    public Return autoComplete4PettyCashReceipt(final Parameter _parameter)
        throws EFapsException
    {
        return new IncomingCreditNote()
        {

            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final Instance instance = _parameter.getInstance();

                if ((instance != null && instance.isValid())
                                && instance.getType().equals(CISales.AccountPettyCash.getType())) {
                    final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.AccountPettyCash2PettyCashReceipt);
                    attrQueryBldr.addWhereAttrEqValue(CISales.AccountPettyCash2PettyCashReceipt.FromLink, instance);
                    final AttributeQuery attrQuery = attrQueryBldr
                                    .getAttributeQuery(CISales.AccountPettyCash2PettyCashReceipt.ToLink);

                    _queryBldr.addWhereAttrInQuery(CISales.PettyCashReceipt.ID, attrQuery);
                    _queryBldr.addWhereAttrNotIsNull(CISales.PettyCashReceipt.Contact);
                } else {
                    super.add2QueryBldr(_parameter, _queryBldr);
                }
            };

            @Override
            protected void addMap4Autocomplete(final Parameter _parameter,
                                               final MultiPrintQuery _multi,
                                               final Map<String, String> _map)
                throws EFapsException
            {
                final Instance instance = _parameter.getInstance();

                if ((instance != null && instance.isValid())
                                && instance.getType().equals(CISales.AccountPettyCash.getType())) {
                    final SelectBuilder selContactInst = new SelectBuilder()
                                    .linkto(CISales.PettyCashReceipt.Contact).instance();
                    final SelectBuilder selContactName = new SelectBuilder()
                                    .linkto(CISales.PettyCashReceipt.Contact).attribute(CIContacts.Contact.Name);

                    final Instance contactInst = _multi.<Instance>getSelect(selContactInst);
                    _map.put("contact", contactInst.getOid());
                    _map.put("contactAutoComplete", _multi.<String>getSelect(selContactName));
                    _map.put("contactData", new Contacts().getFieldValue4Contact(contactInst, false));
                }
            }

            @Override
            protected void addMulti4Autocomplete(final Parameter _parameter,
                                                 final MultiPrintQuery _multi)
                throws EFapsException
            {
                final Instance instance = _parameter.getInstance();

                if ((instance != null && instance.isValid())
                                && instance.getType().equals(CISales.AccountPettyCash.getType())) {
                    final SelectBuilder selContactInst = new SelectBuilder()
                                    .linkto(CISales.PettyCashReceipt.Contact).instance();
                    final SelectBuilder selContactName = new SelectBuilder()
                                    .linkto(CISales.PettyCashReceipt.Contact).attribute(CIContacts.Contact.Name);
                    _multi.addSelect(selContactInst, selContactName);
                }
            }
        }.autoComplete4Doc(_parameter, CISales.PettyCashReceipt.uuid, (Status[]) null);
    }

    @Override
    public String getTypeName4SysConf(final Parameter _parameter)
        throws EFapsException
    {
        return CISales.IncomingCreditNote.getType().getName();
    }
}
