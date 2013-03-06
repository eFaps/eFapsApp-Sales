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

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uitable.MultiPrint;
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
    extends DocumentSum
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

        return new Return();
    }

    /**
     * Method to get the list of instances for a table.
     *
     * @param _parameter parameter as passed from the efaps api
     * @return list of instances
     * @throws EFapsException on error
     */
    public Return getProductTable(final Parameter _parameter)
        throws EFapsException
    {
        final MultiPrint multi = new MultiPrint() {

            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final QueryBuilder atrrQueryBldr = new QueryBuilder(CISales.OrderInbound);
                atrrQueryBldr.addWhereAttrEqValue(CISales.OrderInbound.Status,
                                Status.find(CISales.OrderInboundStatus.uuid, "Open").getId());
                final AttributeQuery attrQuery = atrrQueryBldr.getAttributeQuery(CISales.OrderInbound.ID);
                _queryBldr.addWhereAttrInQuery(CISales.OrderInboundPosition.Order, attrQuery);
                super.add2QueryBldr(_parameter, _queryBldr);
            }
        };
        return multi.execute(_parameter);
    }
}
