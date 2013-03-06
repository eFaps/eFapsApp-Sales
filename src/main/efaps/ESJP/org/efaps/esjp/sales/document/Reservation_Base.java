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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.efaps.admin.datamodel.Dimension;
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
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.CommonDocument_Base.CreatedDoc;
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
    extends AbstractProductDocument
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
        final CreatedDoc doc = createDoc(_parameter);
        createPositions(_parameter, doc);
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
        Object[] deliveryNodeId = (Object[]) map.get(instance.getType().getAttribute(
                        CISales.ReservationPosition.Reservation.name));
        // if it did not work check the abstract attribute
        if (deliveryNodeId == null) {
            deliveryNodeId = (Object[]) map.get(instance.getType().getAttribute(
                            CISales.ReservationPosition.DocumentAbstractLink.name));
        }
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
    public Return setStatus(final Parameter _parameter)
        throws EFapsException
    {
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final Instance instance = _parameter.getInstance();
        final Update update = new Update(instance);
        if (properties.containsKey("ReservationStatus")) {
            update.add(CISales.Reservation.Status, Status.find(CISales.ReservationStatus.uuid,
                            (String) properties.get("ReservationStatus")).getId());
        } else {
            update.add(CISales.Reservation.Status, Status.find(CISales.ReservationStatus.uuid, "Canceled").getId());
        }
        update.execute();

        final QueryBuilder queryBldr = new QueryBuilder(CISales.ReservationPosition);
        queryBldr.addWhereAttrEqValue(CISales.ReservationPosition.Reservation, instance.getId());
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selProdInst = new SelectBuilder().linkto(CISales.ReservationPosition.Product).instance();
        multi.addSelect(selProdInst);
        multi.addAttribute(CISales.ReservationPosition.Quantity,
                        CISales.ReservationPosition.UoM);
        multi.execute();
        while (multi.next()) {
            final Instance prodInst = multi.<Instance>getSelect(selProdInst);
            final List<Instance> storInsts = getStorageInstances4StatusClosed(_parameter, prodInst);
            for (final Instance storInst : storInsts) {
                final Insert insert = new Insert(CIProducts.TransactionReservationOutbound);
                insert.add(CIProducts.TransactionReservationOutbound.Quantity,
                                multi.getAttribute(CISales.ReservationPosition.Quantity));
                insert.add(CIProducts.TransactionReservationOutbound.Storage, storInst.getId());
                insert.add(CIProducts.TransactionReservationOutbound.Product, prodInst.getId());
                insert.add(CIProducts.TransactionReservationOutbound.Description,
                                DBProperties.getProperty("org.efaps.esjp.sales.document.Reservation.cancel"));
                insert.add(CIProducts.TransactionReservationOutbound.Date, new DateTime());
                insert.add("Document", instance.getId());
                insert.add(CIProducts.TransactionReservationOutbound.UoM,
                                multi.getAttribute(CISales.ReservationPosition.UoM));
                insert.execute();
            }
        }
        return new Return();
    }

    protected List<Instance> getStorageInstances4StatusClosed(final Parameter _parameter,
                                                              final Instance _prodInst)
        throws EFapsException
    {
        final List<Instance> ret = new ArrayList<Instance>();

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.Inventory);
        queryBldr.addWhereAttrEqValue(CIProducts.Inventory.Product, _prodInst.getId());
        queryBldr.addWhereAttrGreaterValue(CIProducts.Inventory.Reserved, BigDecimal.ZERO);
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selStorInst = new SelectBuilder().linkto(CIProducts.Inventory.Storage).instance();
        multi.addSelect(selStorInst);
        multi.execute();
        while (multi.next()) {
            final Instance storInst = multi.<Instance>getSelect(selStorInst);
            if (storInst.isValid()) {
                ret.add(storInst);
            }
        }
        return ret;
    }
}
