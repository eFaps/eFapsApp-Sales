/*
 * Copyright 2003 - 2015 The eFaps Team
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

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Insert;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("d1c3dff3-cfcd-4eea-a6d7-7b1529aefada")
@EFapsApplication("eFapsApp-Sales")
public abstract class AcquisitionCosting_Base
    extends AbstractDocumentSum
{

    /**
     * Executed from a Command execute vent to create a new Incoming Invoice.
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
        connect2Derived(_parameter, createdDoc);
        connect2Object(_parameter, createdDoc);
        connect2RecievingTicket(_parameter, createdDoc);
        return new Return();
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _createdDoc   created doc
     * @throws EFapsException on error
     */
    protected void connect2RecievingTicket(final Parameter _parameter,
                                           final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Document2DerivativeDocument);
        attrQueryBldr.addWhereAttrEqValue(CISales.Document2DerivativeDocument.To, _createdDoc.getInstance());

        final QueryBuilder queryBldr = new QueryBuilder(CISales.IncomingInvoice2RecievingTicket);
        queryBldr.addWhereAttrInQuery(CISales.IncomingInvoice2RecievingTicket.FromLink,
                        attrQueryBldr.getAttributeQuery(CISales.Document2DerivativeDocument.From));

        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selInst = SelectBuilder.get().linkto(CISales.IncomingInvoice2RecievingTicket.ToLink)
                        .instance();
        multi.addSelect(selInst);
        multi.execute();
        while (multi.next()) {
            final Insert insert = new Insert(CISales.AcquisitionCosting2RecievingTicket);
            insert.add(CISales.AcquisitionCosting2RecievingTicket.FromLink, _createdDoc.getInstance());
            insert.add(CISales.AcquisitionCosting2RecievingTicket.ToLink, multi.getSelect(selInst));
            insert.execute();
        }
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

    @Override
    public Return validate(final Parameter _parameter)
        throws EFapsException
    {
        return new Validation().validate(_parameter, this);
    }

    @Override
    public CIType getCIType()
        throws EFapsException
    {
        return CISales.AcquisitionCosting;
    }
}
