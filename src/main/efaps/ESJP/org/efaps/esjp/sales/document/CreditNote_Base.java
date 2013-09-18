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
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("a66a61fd-487a-4764-9e72-f65050c1d39e")
@EFapsRevision("$Rev$")
public abstract class CreditNote_Base
    extends DocumentSum
{

    /**
     * Method for create a new Credit Note.
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
        connectDocument2DerivatedDoc(_parameter, createdDoc);
        return new Return();
    }

    protected void connectDocument2DerivatedDoc(final Parameter _parameter,
                                                final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final Instance derived = Instance.get(_parameter.getParameterValue("derived"));

        if (derived.isValid()) {
            final Insert relInsert = new Insert(CISales.Document2DerivativeDocument);
            relInsert.add(CISales.Document2DerivativeDocument.From, derived.getId());
            relInsert.add(CISales.Document2DerivativeDocument.To, _createdDoc.getInstance().getId());
            relInsert.execute();
        }
    }
}
