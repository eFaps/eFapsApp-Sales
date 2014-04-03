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


package org.efaps.esjp.sales.comparative;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("9274142f-4882-43d4-a87f-840eb51d1720")
@EFapsRevision("$Rev$")
public abstract class AbstractComparative_Base
    extends CommonDocument
{
    /**
     * Method to create the basic Document. The method checks for the Type to be
     * created for every attribute if a related field is in the parameters.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return Instance of the document.
     * @throws EFapsException on error.
     */
    protected CreatedDoc createDoc(final Parameter _parameter) throws EFapsException
    {
        final CreatedDoc ret = new CreatedDoc();
        final Insert insert = new Insert(getType4DocCreate(_parameter));
        final String name = getDocName4Create(_parameter);
        insert.add(CISales.ComparativeAbstract.Name, name);
        ret.getValues().put(CISales.ComparativeAbstract.Name.name, name);

        final String date = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.ComparativeAbstract.Date.name));
        if (date != null) {
            insert.add(CISales.ComparativeAbstract.Date, date);
            ret.getValues().put(CISales.ComparativeAbstract.Date.name, date);
        }
        final String description = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.ComparativeAbstract.Description.name));
        if (description != null) {
            insert.add(CISales.ComparativeAbstract.Description, description);
            ret.getValues().put(CISales.ComparativeAbstract.Description.name, description);
        }
        final String note = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.ComparativeAbstract.Note.name));
        if (note != null) {
            insert.add(CISales.ComparativeAbstract.Note, note);
            ret.getValues().put(CISales.ComparativeAbstract.Note.name, note);
        }
        addStatus2DocCreate(_parameter, insert, ret);
        add2DocCreate(_parameter, insert, ret);
        insert.execute();

        ret.setInstance(insert.getInstance());
        return ret;
    }

    /**
     * @param _parameter Paramter as passed by the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return createDetail(final Parameter _parameter)
        throws EFapsException
    {
        final Create create = new Create() {
            @Override
            protected void add2basicInsert(final Parameter _parameter,
                                           final Insert _insert)
                throws EFapsException
            {
                add2DetailCreate(_parameter, _insert);
            }
        };
        return create.execute(_parameter);
    }

    protected void add2DetailCreate(final Parameter _parameter,
                                    final Insert _insert)
        throws EFapsException
    {

    }
}
