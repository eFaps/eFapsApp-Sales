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

import java.util.UUID;

import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.sales.Costs;
import org.efaps.util.EFapsException;

/**
 * Base class for Type Incoming Invoice.
 * 
 * @author The eFaps Team
 * @version $Id: IncomingInvoice_Base.java 8362 2012-12-12 18:41:35Z
 *          jan@moxter.net $
 */
@EFapsUUID("e740fd7c-4601-4595-8a7e-0175522cbd74")
@EFapsRevision("$Rev: 10 $")
public class IncomingReceipt_Base
    extends DocumentSum
{

    /**
     * Used to store the Revision in the Context.
     */
    public final static String REVISIONKEY = "org.efaps.esjp.sales.document.IncomingReceipt.RevisionKey";

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
        new Costs().updateCosts(_parameter, createdDoc.getInstance());
        return new Return();
    }

    @Override
    protected void add2DocCreate(final Parameter _parameter,
                                 final Insert _insert,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        // Sales_IncomingReceiptSequence
        final NumberGenerator numgen = NumberGenerator.get(UUID.fromString("58d89fbf-0c0a-4a27-b1df-90f34759011e"));
        if (numgen != null) {
            final String revision = numgen.getNextVal();
            Context.getThreadContext().setSessionAttribute(IncomingReceipt_Base.REVISIONKEY, revision);
            _insert.add(CISales.IncomingReceipt.Revision, revision);
        }
    }

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
