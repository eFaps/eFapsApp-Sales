/*
 * Copyright 2013 - 2013 The eFaps Team
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
 * Revision:        $Rev: 8342 $
 * Last Changed:    $Date: 2012-12-11 09:42:17 -0500 (Tue, 11 Dec 2012) $
 * Last Changed By: $Author: jan@moxter.net $
 */


package org.efaps.esjp.sales.document;

import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.SalesSettings;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * Base class for Type Incoming Invoice.
 *
 * @author The eFaps Team
 * @version $Id: IncomingInvoice_Base.java 8362 2012-12-12 18:41:35Z
 *          jan@moxter.net $
 */
@EFapsUUID("e740fd7c-4601-4595-8a7e-0175522cbd74")
@EFapsRevision("$Rev: 10 $")
public abstract class IncomingReceipt_Base
    extends DocumentSum
{

    /**
     * Used to store the Revision in the Context.
     */
    public static final String REVISIONKEY = "org.efaps.esjp.sales.document.IncomingReceipt.RevisionKey";

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
        connect2DocumentType(_parameter, createdDoc.getInstance());
        return new Return();
    }

    @Override
    protected void connect2DocumentType(final Parameter _parameter,
                                        final Instance _instance)
        throws EFapsException
    {
        final Instance instDocType = Instance.get(_parameter
                        .getParameterValue(CIFormSales.Sales_IncomingReceiptForm.documentType.name));
        if (instDocType.isValid() && _instance.isValid()) {
            final Insert insert = new Insert(CISales.Document2DocumentType);
            insert.add(CISales.Document2DocumentType.DocumentLink, _instance);
            insert.add(CISales.Document2DocumentType.DocumentTypeLink, instDocType);
            insert.execute();
        }
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
        final SystemConfiguration config = Sales.getSysConfig();
        final Properties props = config.getAttributeValueAsProperties(SalesSettings.INCOMINGRECEIPTSEQUENCE);

        final NumberGenerator numgen = NumberGenerator.get(UUID.fromString(props.getProperty("UUID")));
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
        final Return ret = new Return();
        final String revision = (String) Context.getThreadContext().getSessionAttribute(
                        IncomingReceipt_Base.REVISIONKEY);
        Context.getThreadContext().setSessionAttribute(IncomingReceipt_Base.REVISIONKEY, null);
        final StringBuilder html = new StringBuilder();
        html.append("<span style=\"text-align: center; display: block; width: 100%; font-size: 40px; height: 55px;\">")
                        .append(revision).append("</span>");
        ret.put(ReturnValues.SNIPLETT, html.toString());
        return ret;
    }
}
