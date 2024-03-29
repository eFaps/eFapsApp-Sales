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

import java.util.List;
import java.util.UUID;

import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * Base class for Type Incoming Invoice.
 *
 * @author The eFaps Team
 */
@EFapsUUID("e740fd7c-4601-4595-8a7e-0175522cbd74")
@EFapsApplication("eFapsApp-Sales")
public abstract class IncomingReceipt_Base
    extends AbstractDocumentSum
{

    /**
     * Used to store the Revision in the Context.
     */
    public static final String REVISIONKEY =  IncomingReceipt.class.getName() + ".RevisionKey";

    /**
     * Executed from a Command execute vent to create a new Incoming Receipt.
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
        incomingReceiptCreateTransaction(_parameter, createdDoc);
        connect2DocumentType(_parameter, createdDoc);
        connect2ProductDocumentType(_parameter, createdDoc);
        afterCreate(_parameter, createdDoc.getInstance());
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
     * Method to do a transaction of all the products of a Incoming Receipt when
     * it is created.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _createdDoc instance of Incoming Receipt document recently created
     * @throws EFapsException on error
     */
    public void incomingReceiptCreateTransaction(final Parameter _parameter,
                                               final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final String storage = _parameter.getParameterValue("storage");
        final String date = _parameter.getParameterValue("date");

        if (storage != null) {
            final List<Instance> positions = _createdDoc.getPositions();
            for (final Instance instance : positions) {
                final PrintQuery print = new PrintQuery(instance);
                print.addAttribute(CISales.IncomingReceiptPosition.Product, CISales.IncomingReceiptPosition.Quantity,
                                CISales.IncomingReceiptPosition.IncomingReceipt, CISales.IncomingReceiptPosition.UoM);
                print.execute();

                final Object productID = print.<Object>getAttribute(CISales.IncomingReceiptPosition.Product);
                final Object quantity = print.<Object>getAttribute(CISales.IncomingReceiptPosition.Quantity);
                final Object incomingInvoiceId = print
                                .<Object>getAttribute(CISales.IncomingReceiptPosition.IncomingReceipt);
                final Object uom = print.<Object>getAttribute(CISales.IncomingReceiptPosition.UoM);

                final Insert insert = new Insert(CIProducts.TransactionInbound);
                insert.add(CIProducts.TransactionInbound.Quantity, quantity);
                insert.add(CIProducts.TransactionInbound.Storage, storage);
                insert.add(CIProducts.TransactionInbound.Product, productID);
                insert.add(CIProducts.TransactionInbound.Description,
                         DBProperties.getProperty("org.efaps.esjp.sales.document.IncomingReceipt.description4Trigger"));
                insert.add(CIProducts.TransactionInbound.Date, date == null ? new DateTime() : date);
                insert.add(CIProducts.TransactionInbound.Document, incomingInvoiceId);
                insert.add(CIProducts.TransactionInbound.UoM, uom);
                insert.execute();
            }
        }
    }

    @Override
    protected void add2DocCreate(final Parameter _parameter,
                                 final Insert _insert,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final String seqKey = Sales.INCOMINGRECEIPTREVSEQ.get();
        final NumberGenerator numgen = isUUID(seqKey)
                        ? NumberGenerator.get(UUID.fromString(seqKey))
                        : NumberGenerator.get(seqKey);
        if (numgen != null) {
            final String revision = numgen.getNextVal();
            Context.getThreadContext().setSessionAttribute(IncomingReceipt_Base.REVISIONKEY, revision);
            _insert.add(CISales.IncomingReceipt.Revision, revision);
        }
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return with Snipplet
     * @throws EFapsException on error
     */
    public Return showRevisionFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        return getRevisionSequenceFieldValue(_parameter, IncomingReceipt.REVISIONKEY);
    }

    @Override
    public Type getType4SysConf(final Parameter _parameter)
        throws EFapsException
    {
        return CISales.IncomingReceipt.getType();
    }

    @Override
    public Return validate(final Parameter _parameter)
        throws EFapsException
    {
        final Validation validation = new Validation();
        return validation.validate(_parameter, this, Sales.INCOMINGRECEIPT_VALIDATION.get());
    }

}
