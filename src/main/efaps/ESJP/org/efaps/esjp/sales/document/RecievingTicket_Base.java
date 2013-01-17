/*
 * Copyright 2003 - 2009 The eFaps Team
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

import java.util.Map;

import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: RecievingTicket_Base.java 8018 2012-10-09 17:47:24Z
 *          jan@moxter.net $
 */
@EFapsUUID("f6f4e147-fc24-487e-ae81-69e4c44ac964")
@EFapsRevision("$Rev$")
public abstract class RecievingTicket_Base
    extends AbstractProductDocument
{

    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc doc = createDoc(_parameter);
        createPositions(_parameter, doc);
        return new Return();
    }

    @Override
    protected String getDocName4Create(final Parameter _parameter)
        throws EFapsException
    {
        return _parameter.getParameterValue("name");
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new return
     * @throws EFapsException on error
     */
    public Return recievingTicketPositionInsertTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final Map<String, String[]> param = Context.getThreadContext().getParameters();
        final String[] storageIds = param.get("storage");

        final Instance instance = _parameter.getInstance();
        final Map<?, ?> map = (Map<?, ?>) _parameter.get(ParameterValues.NEW_VALUES);

        final Object[] productID = (Object[]) map.get(instance.getType().getAttribute(
                        CISales.RecievingTicketPosition.Product.name));
        final Object[] qauntity = (Object[]) map.get(instance.getType().getAttribute(
                        CISales.RecievingTicketPosition.Quantity.name));
        final Object[] uom = (Object[]) map.get(instance.getType().getAttribute(CISales.DeliveryNotePosition.UoM.name));
        final Object[] pos = (Object[]) map.get(instance.getType().getAttribute(
                        CISales.RecievingTicketPosition.PositionNumber.name));
        Object[] reTickId = (Object[]) map.get(instance.getType().getAttribute(
                        CISales.RecievingTicketPosition.RecievingTicket.name));
        // if it did not work check the abstract attribute
        if (reTickId == null) {
            reTickId = (Object[]) map.get(instance.getType().getAttribute(
                            CISales.RecievingTicketPosition.DocumentAbstractLink.name));
        }

        String storage = null;
        if (storageIds != null) {
            final Integer posInt = (Integer) pos[0];
            storage = storageIds[posInt - 1];
        } else {
            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.Warehouse);
            final InstanceQuery query = queryBldr.getQuery();
            if (query.next()) {
                storage = Long.toString(query.getCurrentValue().getId());
            }
        }

        final Insert insert = new Insert(CIProducts.TransactionInbound);
        insert.add(CIProducts.TransactionInbound.Quantity, qauntity[0]);
        insert.add(CIProducts.TransactionInbound.Storage, storage);
        insert.add(CIProducts.TransactionInbound.Product, productID[0]);
        insert.add(CIProducts.TransactionInbound.Description,
                        DBProperties.getProperty("org.efaps.esjp.sales.document.RecievingTicket.description4Trigger"));
        insert.add(CIProducts.TransactionInbound.Date, new DateTime());
        insert.add(CIProducts.TransactionInbound.Document, reTickId[0]);
        insert.add(CIProducts.TransactionInbound.UoM, uom[0]);
        insert.execute();

        return new Return();
    }
}
