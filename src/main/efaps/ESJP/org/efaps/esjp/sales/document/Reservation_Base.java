/*
 * Copyright 2003 - 2011 The eFaps Team
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
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
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
public abstract class Reservation_Base
    extends AbstractDocument
{

    /**
     * Method for create a new reservation.
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        createDoc(_parameter);
        return new Return();
    }

    /**
     * Internal Method to create the document and the position.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Instance of CreatedDoc.
     * @throws EFapsException on error.
     */
    protected CreatedDoc createDoc(final Parameter _parameter)
        throws EFapsException
    {
        final String date = _parameter.getParameterValue("date");

        final Long contactid = Instance.get(_parameter.getParameterValue("contact")).getId();
        final Insert insert = new Insert(CISales.Reservation);
        insert.add(CISales.Reservation.Contact, contactid.toString());
        insert.add(CISales.Reservation.Date, date);
        insert.add(CISales.Reservation.Salesperson, _parameter.getParameterValue("salesperson"));
        insert.add(CISales.Reservation.Name, getName4Create(_parameter));
        insert.add(CISales.Reservation.Note, _parameter.getParameterValue("note"));
        insert.add(CISales.Reservation.Status, getStatus4Create(_parameter));
        insert.execute();
        final CreatedDoc createdDoc = new CreatedDoc(insert.getInstance());
        createPositions(_parameter, createdDoc);
        return createdDoc;
    }

    /**
     * Get the status id for the document on create.
     * @param _parameter    Parameter as passed from eFaps API.
     * @return id of a status
     * @throws EFapsException on error
     */
    protected long getStatus4Create(final Parameter _parameter)
        throws EFapsException
    {
        return Status.find(CISales.ReservationStatus.uuid, "Open").getId();
    }

    /**
     * Get the name for the document on create.
     * @param _parameter    Parameter as passed from eFaps API.
     * @return id of a status
     * @throws EFapsException on error
     */
    protected String getName4Create(final Parameter _parameter)
        throws EFapsException
    {
        return _parameter.getParameterValue("name");
    }

    /**
     * Internal Method to create the positions for this Document.
     * @param _parameter    Parameter as passed from eFaps API.
     * @param _createdDoc   cretaed Document
     * @throws EFapsException on error
     */
    protected void createPositions(final Parameter _parameter,
                                   final CreatedDoc _createdDoc)
        throws EFapsException
    {
        Integer i = 1;
        for (final String quantity : _parameter.getParameterValues("quantity")) {
            final Insert posIns = new Insert(CISales.ReservationPosition);
            final Long productdId = Instance.get(_parameter.getParameterValues("product")[i - 1]).getId();
            posIns.add(CISales.ReservationPosition.Reservation, _createdDoc.getInstance().getId());
            posIns.add(CISales.ReservationPosition.PositionNumber, i.toString());
            posIns.add(CISales.ReservationPosition.Product, productdId.toString());
            posIns.add(CISales.ReservationPosition.ProductDesc, _parameter.getParameterValues("productDesc")[i - 1]);
            posIns.add(CISales.ReservationPosition.Quantity, (new BigDecimal(quantity)).toString());
            posIns.add(CISales.ReservationPosition.UoM, _parameter.getParameterValues("uoM")[i - 1]);
            posIns.execute();
            i++;
            _createdDoc.addPosition(posIns.getInstance());
        }
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

        Object storage = null;
        if (storageIds != null) {
            for (int i = 0; i < productOids.length; i++) {
                if (productID[0].toString().equals(((Long) Instance.get(productOids[i]).getId()).toString())) {
                    storage = storageIds[i];
                    break;
                }
            }
        } else {
            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.Warehouse);
            add2QueryBldr4Trigger(_parameter, queryBldr);
            final InstanceQuery query = queryBldr.getQuery();
            query.execute();
            if (query.next()) {
                storage = query.getCurrentValue().getId();
            }
        }

        final Insert insert = new Insert(CIProducts.TransactionReservationInbound);
        insert.add(CIProducts.TransactionReservationInbound.Quantity, qauntity[0]);
        insert.add(CIProducts.TransactionReservationInbound.Storage, storage);
        insert.add(CIProducts.TransactionReservationInbound.Product, productID);
        insert.add(CIProducts.TransactionReservationInbound.Description,
                DBProperties.getProperty("org.efaps.esjp.sales.document.Reservation.create"));
        insert.add(CIProducts.TransactionReservationInbound.Date, new DateTime());
        insert.add(CIProducts.TransactionReservationInbound.Document, deliveryNodeId[0]);
        insert.add(CIProducts.TransactionReservationInbound.UoM, uom[0]);
        insert.execute();

        return new Return();
    }

    /**
     * Allows to add additional criteria to the QueryBuilder.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @param _queryBldr QueryBuilder to add to
     * @throws EFapsException on error.
     */
    protected void add2QueryBldr4Trigger(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
        throws EFapsException
    {
        // to be used by implementation
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

        final QueryBuilder queryBldr = new QueryBuilder(CISales.ReservationPosition);
        queryBldr.addWhereAttrEqValue(CISales.ReservationPosition.Reservation, instance.getId());
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.ReservationPosition.Product, CISales.ReservationPosition.Quantity,
                        CISales.ReservationPosition.UoM);
        multi.execute();
        while (multi.next()) {
            final Long productId = multi.<Long>getAttribute(CISales.ReservationPosition.Product);
            final QueryBuilder queryBldr2 = new QueryBuilder(CIProducts.Inventory);
            queryBldr2.addWhereAttrEqValue(CIProducts.Inventory.Product, productId);
            final MultiPrintQuery multi2 = queryBldr2.getPrint();
            multi2.addAttribute(CIProducts.Inventory.Storage);
            multi2.execute();
            while (multi2.next()) {
                final Long storage = multi2.<Long>getAttribute(CIProducts.Inventory.Storage);
                final Insert insert = new Insert(CIProducts.TransactionReservationOutbound);
                insert.add(CIProducts.TransactionReservationOutbound.Quantity,
                                multi.getAttribute(CISales.ReservationPosition.Quantity));
                insert.add(CIProducts.TransactionReservationOutbound.Storage, storage);
                insert.add(CIProducts.TransactionReservationOutbound.Product, productId);
                insert.add(CIProducts.TransactionReservationOutbound.Description,
                                DBProperties.getProperty("org.efaps.esjp.sales.document.Reservation.close"));
                insert.add(CIProducts.TransactionReservationOutbound.Date, new DateTime());
                insert.add("Document", instance.getId());
                insert.add(CIProducts.TransactionReservationOutbound.UoM,
                                multi.getAttribute(CISales.ReservationPosition.UoM));
                insert.execute();
            }
        }
        return new Return();
    }
}
