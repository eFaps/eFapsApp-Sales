/*
 * Copyright 2003 - 2010 The eFaps Team
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

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SearchQuery;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("8b35c62b-debd-46f8-9a07-7a9befb6edc4")
@EFapsRevision("$Rev$")
public class Reservation_Base
    extends AbstractDocument
{
    /**
     * Method for create a new reservation.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final String date = _parameter.getParameterValue("date");
        final Long contactid = Instance.get(_parameter.getParameterValue("contact")).getId();
        final Insert insert = new Insert(CISales.Reservation);
        insert.add(CISales.Reservation.Contact, contactid.toString());
        insert.add(CISales.Reservation.Date, date);
        insert.add(CISales.Reservation.Salesperson, _parameter.getParameterValue("salesperson"));
        insert.add(CISales.Reservation.Name, _parameter.getParameterValue("name"));
        insert.add(CISales.Reservation.Note, _parameter.getParameterValue("note"));
        insert.add(CISales.Reservation.Status,
                        ((Long) Status.find(CISales.ReservationStatus.uuid, "Open").getId()).toString());
        insert.execute();
        Integer i = 1;
        for (final String quantity : _parameter.getParameterValues("quantity")) {
            final Insert posIns = new Insert(CISales.ReservationPosition);
            final Long productdId = Instance.get(_parameter.getParameterValues("product")[i - 1]).getId();
            posIns.add(CISales.ReservationPosition.Reservation, insert.getId());
            posIns.add(CISales.ReservationPosition.PositionNumber, i.toString());
            posIns.add(CISales.ReservationPosition.Product, productdId.toString());
            posIns.add(CISales.ReservationPosition.ProductDesc, _parameter.getParameterValues("productDesc")[i - 1]);
            posIns.add(CISales.ReservationPosition.Quantity, (new BigDecimal(quantity)).toString());
            posIns.add(CISales.ReservationPosition.UoM, _parameter.getParameterValues("uoM")[i - 1]);
            posIns.add(CISales.ReservationPosition.CrossUnitPrice, "0");
            posIns.add(CISales.ReservationPosition.NetUnitPrice, "0");
            posIns.add(CISales.ReservationPosition.CrossPrice, "0");
            posIns.add(CISales.ReservationPosition.NetPrice, "0");
            posIns.add(CISales.ReservationPosition.Discount, "0");
            posIns.add(CISales.ReservationPosition.Tax, "1");
            posIns.add(CISales.ReservationPosition.DiscountNetUnitPrice, BigDecimal.ZERO);
            posIns.add(CISales.ReservationPosition.CurrencyId, 1);
            posIns.add(CISales.ReservationPosition.RateCurrencyId, 1);
            posIns.add(CISales.ReservationPosition.Rate, new Object[] { BigDecimal.ONE, BigDecimal.ONE });
            posIns.execute();
            i++;
        }
        return new Return();
    }

    /**
     * Trigger for insert a new reservation in warehouse.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return reservationPositionInsertTrigger(final Parameter _parameter)
        throws EFapsException
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
            for (int i = 0; i < productOids.length; i++) {
                if (productID[0].toString().equals(((Long) Instance.get(productOids[i]).getId()).toString())) {
                    storage = storageIds[i];
                    break;
                }
            }
        } else {
            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.Warehouse);
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CIProducts.Warehouse.ID);
            multi.execute();
            while (multi.next()) {
                storage = multi.<Long>getAttribute(CIProducts.Warehouse.ID).toString();
            }
        }

        final Insert insert = new Insert(CIProducts.TransactionReservationInbound);
        insert.add(CIProducts.TransactionReservationInbound.Quantity, qauntity[0]);
        insert.add(CIProducts.TransactionReservationInbound.Storage, storage);
        insert.add(CIProducts.TransactionReservationInbound.Product, productID);
        insert.add(CIProducts.TransactionReservationInbound.Description, "automatic by Trigger");
        insert.add(CIProducts.TransactionReservationInbound.Date, new DateTime());
        insert.add("Document", deliveryNodeId[0]);
        insert.add(CIProducts.TransactionReservationInbound.UoM, uom[0]);
        insert.execute();

        return new Return();
    }

    /**
     * Method for established closed a reservation.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return setStatusClosed(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instance = _parameter.getInstance();
        final Update update = new Update(instance);
        update.add("Status", Status.find(CISales.ReservationStatus.uuid, "Closed").getId());
        update.execute();

        final SearchQuery query = new SearchQuery();
        query.setExpand(instance, Type.get(CISales.ReservationPosition.uuid).getName() + "\\Reservation");
        query.addSelect("Product");
        query.addSelect("Quantity");
        query.addSelect("UoM");
        query.execute();
        while (query.next()) {
            final Long productId = (Long) query.get("Product");
            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.Inventory);
            queryBldr.addWhereAttrEqValue(CIProducts.Inventory.Product, productId);
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CIProducts.Inventory.Storage);
            multi.execute();
            while (multi.next()) {
                final Long storage = multi.<Long>getAttribute(CIProducts.Inventory.Storage);

                final Insert insert = new Insert(CIProducts.TransactionReservationOutbound);
                insert.add(CIProducts.TransactionReservationOutbound.Quantity, query.get("Quantity"));
                insert.add(CIProducts.TransactionReservationOutbound.Storage, storage);
                insert.add(CIProducts.TransactionReservationOutbound.Product, productId);
                insert.add(CIProducts.TransactionReservationOutbound.Description,
                                DBProperties.getProperty("org.efaps.esjp.sales.document.Reservation.close"));
                insert.add(CIProducts.TransactionReservationOutbound.Date, new DateTime());
                insert.add("Document", instance.getId());
                insert.add(CIProducts.TransactionReservationOutbound.UoM, query.get("UoM"));
                insert.execute();
            }
        }
        return new Return();
    }
}
