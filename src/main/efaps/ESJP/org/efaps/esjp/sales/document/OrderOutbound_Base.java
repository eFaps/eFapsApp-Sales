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

import java.util.List;

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
@EFapsUUID("bd08a90e-91ce-4f03-b1bc-921a53b71948")
@EFapsRevision("$Rev$")
public abstract class OrderOutbound_Base
    extends DocumentSum
{

    /**
     * Executed from a Command execute event to create a new OrderOutbound.
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
        connectChannel2Document(_parameter, createdDoc);
        connect2Derived(_parameter, createdDoc);
        executeProcess(_parameter, createdDoc);
        return new Return();
    }

    protected void connectChannel2Document(final Parameter _parameter,
                                           final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final Instance instCondition = Instance.get(_parameter.getParameterValue("conditionSales"));
        if (instCondition.isValid() && _createdDoc.getInstance().isValid()) {
            final Insert insert = new Insert(CISales.ChannelSalesChannel2Document);
            insert.add(CISales.ChannelSalesChannel2Document.FromLink, instCondition);
            insert.add(CISales.ChannelSalesChannel2Document.ToLink, _createdDoc.getInstance());
            insert.execute();
        }
    }

    @Override
    protected boolean isContact2JavaScript4Document(final Parameter _parameter,
                                                    final List<Instance> _instances)
        throws EFapsException
    {
        boolean ret = true;
        if (!_instances.isEmpty() && _instances.get(0).isValid()) {
            ret = !_instances.get(0).getType().isKindOf(CISales.ProductRequest.getType());
        }
        return ret;
    }

    @Override
    public String getTypeName4SysConf(final Parameter _parameter)
        throws EFapsException
    {
        return CISales.OrderOutbound.getType().getName();
    }
}
