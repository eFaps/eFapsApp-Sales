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

import java.math.BigDecimal;
import java.util.Map;

import org.joda.time.DateTime;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.SearchQuery;
import org.efaps.db.Update;
import org.efaps.esjp.sales.Sales;
import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("8b35c62b-debd-46f8-9a07-7a9befb6edc4")
@EFapsRevision("$Rev$")
public class Reservation_Base extends AbstractDocument
{
    public Return create(final Parameter _parameter) throws EFapsException
    {
        final String date = _parameter.getParameterValue("date");
        final Long contactid = Instance.get(_parameter.getParameterValue("contact")).getId();
        final Insert insert = new Insert(Sales.RESERVATION.getUuid());
        insert.add("Contact", contactid.toString());
        insert.add("Date", date);
        insert.add("Salesperson", _parameter.getParameterValue("salesperson"));
        insert.add("Name", _parameter.getParameterValue("name"));
        insert.add("Note", _parameter.getParameterValue("note"));
        insert.add("Status", ((Long) Status.find(Sales.RESERVATION_STATUS.getUuid(), "Open").getId()).toString());
        insert.execute();
        Integer i = 1;
        for (final String quantity :  _parameter.getParameterValues("quantity")) {
            final Insert posIns = new Insert(Sales.RESERVATION_POS.getUuid());
            final Long productdId = Instance.get(_parameter.getParameterValues("product")[i - 1]).getId();
            posIns.add("Reservation", insert.getId());
            posIns.add("PositionNumber", i.toString());
            posIns.add("Product", productdId.toString());
            posIns.add("ProductDesc", _parameter.getParameterValues("productDesc")[i - 1]);
            posIns.add("Quantity", (new BigDecimal(quantity)).toString());
            posIns.add("UoM",  _parameter.getParameterValues("uoM")[i - 1]);
            posIns.add("CrossUnitPrice", "0");
            posIns.add("NetUnitPrice", "0");
            posIns.add("CrossPrice", "0");
            posIns.add("NetPrice", "0");
            posIns.add("Discount", "0");
            posIns.add("Tax", "1");
            posIns.add("DiscountPrice", BigDecimal.ZERO);
            posIns.add("CurrencyId", 1);
            posIns.add("RateCurrencyId", 1);
            posIns.add("Rate", BigDecimal.ZERO);
            posIns.execute();
            i++;
        }
        return new Return();
    }


    public Return reservationPositionInsertTrigger(final Parameter _parameter) throws EFapsException
    {
        final Map<String, String[]> param = Context.getThreadContext().getParameters();
        final String[] productOids = param.get("product");
        final String[] storageIds = param.get("storage");

        final Instance instance = _parameter.getInstance();
        final Map<?, ?> map = (Map<?, ?>) _parameter.get(ParameterValues.NEW_VALUES);

        final Object[] productID = (Object[]) map.get(instance.getType().getAttribute("Product"));
        final Object[] qauntity = (Object[]) map.get(instance.getType().getAttribute("Quantity"));
        final Object[] deliveryNodeId = (Object[]) map.get(instance.getType().getAttribute("Reservation"));
        final Object[] uom = (Object[]) map.get(instance.getType().getAttribute("UoM"));

        String storage = null;
        if (storageIds != null) {
            for (int i = 0; i <  productOids.length; i++) {
                if (productID[0].toString().equals(((Long) Instance.get(productOids[i]).getId()).toString())) {
                    storage = storageIds[i];
                    break;
                }
            }
        } else {
            final SearchQuery query = new SearchQuery();
            query.setQueryTypes("Products_Warehouse");
            query.addSelect("ID");
            query.execute();
            if (query.next()) {
                storage = ((Long) query.get("ID")).toString();
            }
        }

        final Insert insert = new Insert("Products_TransactionReservationInbound");
        insert.add("Quantity", qauntity[0]);
        insert.add("Storage", storage);
        insert.add("Product", productID);
        insert.add("Description", "automatic by Trigger");
        insert.add("Date", new DateTime());
        insert.add("Document", deliveryNodeId[0]);
        insert.add("UoM", uom[0]);
        insert.execute();

        return new Return();
    }


    public Return setStatusClosed(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instance = _parameter.getInstance();
        final Update update = new Update(instance);
        update.add("Status", Status.find(Sales.RESERVATION_STATUS.getUuid(), "Closed").getId());
        update.execute();

        final SearchQuery query = new SearchQuery();
        query.setExpand(instance, Type.get(Sales.RESERVATION_POS.getUuid()).getName() + "\\Reservation");
        query.addSelect("Product");
        query.addSelect("Quantity");
        query.addSelect("UoM");
        query.execute();
        while (query.next()) {
            final Long productId = (Long) query.get("Product");
            final SearchQuery queryInv = new SearchQuery();
            queryInv.setQueryTypes("Products_Inventory");
            queryInv.addWhereExprEqValue("Product", productId);
            queryInv.addSelect("Storage");
            queryInv.execute();
            if (queryInv.next()) {
                final Long storage = (Long) queryInv.get("Storage");

                final Insert insert = new Insert("Products_TransactionReservationOutbound");
                insert.add("Quantity", query.get("Quantity"));
                insert.add("Storage", storage);
                insert.add("Product", productId);
                insert.add("Description", DBProperties.getProperty("org.efaps.esjp.sales.document.Reservation.close"));
                insert.add("Date", new DateTime());
                insert.add("Document", instance.getId());
                insert.add("UoM", query.get("UoM"));
                insert.execute();
            }
        }
        return new Return();
    }
}
