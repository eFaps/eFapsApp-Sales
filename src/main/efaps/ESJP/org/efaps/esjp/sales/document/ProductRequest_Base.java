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

import java.io.File;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("97c5f066-ac96-465c-89da-f890896b7ab4")
@EFapsApplication("eFapsApp-Sales")
public abstract class ProductRequest_Base
    extends AbstractProductDocument
{
    /**
     * {@inheritDoc}
     */
    @Override
    public CIType getCIType()
        throws EFapsException
    {
        return CISales.ProductRequest;
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final CreatedDoc createdDoc = createDoc(_parameter);
        createPositions(_parameter, createdDoc);
        connect2Derived(_parameter, createdDoc);
        connect2Object(_parameter, createdDoc);
        connect2Terms(_parameter, createdDoc);

        final File file = createReport(_parameter, createdDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        executeProcess(_parameter, createdDoc);
        ret.put(ReturnValues.INSTANCE, createdDoc.getInstance());
        return ret;
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final EditedDoc editedDoc = editDoc(_parameter);
        updatePositions(_parameter, editedDoc);
        updateConnection2Object(_parameter, editedDoc);
        final File file = createReport(_parameter, editedDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }
}
