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

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: OrderInbound_Base.java 6431 2011-04-24 05:16:14Z jan@moxter.net
 *          $
 */
@EFapsUUID("77aebe5f-aebd-4662-b503-efdc6e46d9d3")
@EFapsRevision("$Rev$")
public abstract class OrderInbound_Base
    extends AbstractDocumentSum
{

    /**
     * Method for create a new Incoming Order.
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
        executeProcess(_parameter, createdDoc);
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

    @Override
    protected Type getType4SysConf(final Parameter _parameter)
        throws EFapsException
    {
        return getCIType().getType();
    }

    @Override
    public String getTypeName4SysConf(final Parameter _parameter)
        throws EFapsException
    {
        return getType4SysConf(_parameter).getName();
    }

    @Override
    public CIType getCIType()
        throws EFapsException
    {
        return CISales.OrderInbound;
    }
}
