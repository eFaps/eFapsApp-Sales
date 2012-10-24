/*
 * Copyright 2003 - 2012 The eFaps Team
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

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;


/**
 * basic class for documents that do not have prices.
 *
 * @author The eFaps Team
 * @version $Id: AbstractDocument_Base.java 3674 2010-01-28 18:52:35Z jan.moxter$
 */
@EFapsUUID("92e52f22-f3f2-43b3-87c3-ad224d9832fc")
@EFapsRevision("$Rev$")
public abstract class AbstractProductDocument_Base
    extends AbstractDocument
{

    /**
     * Method to create the basic Document. The method checks for the Type to be
     * created for every attribute if a related field is in the parameters.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return Instance of the document.
     * @throws EFapsException on error.
     */
    protected CreatedDoc createDoc(final Parameter _parameter)
        throws EFapsException
    {

        final CreatedDoc createdDoc = new CreatedDoc();

        final Insert insert = new Insert(getType4DocCreate(_parameter));
        insert.add(CISales.DocumentStockAbstract.Name, getDocName4Create(_parameter));

        final String date = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentStockAbstract.Date.name));
        if (date != null) {
            insert.add(CISales.DocumentStockAbstract.Date, date);
            createdDoc.getValues().put(getFieldName4Attribute(_parameter, CISales.DocumentStockAbstract.Date.name),
                            date);
        }
        final String duedate = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentStockAbstract.DueDate.name));
        if (duedate != null) {
            insert.add(CISales.DocumentStockAbstract.DueDate, date);
            createdDoc.getValues().put(getFieldName4Attribute(_parameter, CISales.DocumentStockAbstract.DueDate.name),
                            duedate);
        }
        final String contact = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentStockAbstract.Contact.name));
        if (contact != null) {
            final Instance inst = Instance.get(contact);
            if (inst.isValid()) {
                insert.add(CISales.DocumentStockAbstract.Contact, inst.getId());
                createdDoc.getValues().put(
                                getFieldName4Attribute(_parameter, CISales.DocumentStockAbstract.Contact.name), inst);
            }
        }
        final String note = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentStockAbstract.Note.name));
        if (note != null) {
            insert.add(CISales.DocumentStockAbstract.Note, note);
            createdDoc.getValues().put(getFieldName4Attribute(_parameter, CISales.DocumentStockAbstract.Note.name),
                            note);
        }
        final String revision = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentStockAbstract.Revision.name));
        if (revision != null) {
            insert.add(CISales.DocumentStockAbstract.Revision, revision);
            createdDoc.getValues().put(getFieldName4Attribute(_parameter, CISales.DocumentStockAbstract.Revision.name),
                            revision);
        }
        final String salesperson = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentStockAbstract.Salesperson.name));
        if (salesperson != null) {
            insert.add(CISales.DocumentStockAbstract.Salesperson, salesperson);
            createdDoc.getValues().put(
                            getFieldName4Attribute(_parameter, CISales.DocumentStockAbstract.Salesperson.name),
                            salesperson);
        }

        addStatus2DocCreate(_parameter, insert, createdDoc);
        add2DocCreate(_parameter, insert, createdDoc);
        insert.execute();

        createdDoc.setInstance(insert.getInstance());
        return createdDoc;
    }


    protected void createPositions(final Parameter _parameter,
                                   final CreatedDoc _createdDoc)
        throws EFapsException
    {

        for (int i = 0; i < getPositionsCount(_parameter); i++) {
            final Insert posIns = new Insert(getType4PositionCreate(_parameter));
            posIns.add(CISales.PositionAbstract.PositionNumber, i + 1);
            posIns.add(CISales.PositionAbstract.DocumentAbstractLink, _createdDoc.getInstance().getId());

            final String product = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                            CISales.PositionAbstract.Product.name))[i];
            if (product != null) {
                final Instance inst = Instance.get(product);
                if (inst.isValid()) {
                    posIns.add(CISales.PositionAbstract.Product, inst.getId());
                }
            }

            final String productDesc = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                            CISales.PositionAbstract.ProductDesc.name))[i];
            if (productDesc != null) {
                posIns.add(CISales.PositionAbstract.ProductDesc, productDesc);
            }

            final String UoM = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                            CISales.PositionAbstract.UoM.name))[i];
            if (UoM != null) {
                posIns.add(CISales.PositionAbstract.UoM, UoM);
            }

            final String quantity = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                            CISales.PositionAbstract.Quantity.name))[i];
            if (quantity != null) {
                posIns.add(CISales.PositionAbstract.Quantity, quantity);
            }

            add2PositionCreate(_parameter, posIns, _createdDoc);

            posIns.execute();
        }
    }

    /**
     * Method is calles in the preocess of creation
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _posInsert    insert to add to
     * @param _createdDoc   document created
     */
    protected void add2PositionCreate(final Parameter _parameter,
                                      final Insert _posInsert,
                                      final CreatedDoc _createdDoc)
    {
        // used by implementation
    }
}
