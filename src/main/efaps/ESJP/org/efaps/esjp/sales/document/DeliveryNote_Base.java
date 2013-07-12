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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.Revision;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: DeliveryNote_Base.java 7915 2012-08-17 15:30:12Z
 *          m.aranya@moxter.net $
 */
@EFapsUUID("363ad7a5-1e7b-4194-89e3-a31d07d783df")
@EFapsRevision("$Rev$")
public abstract class DeliveryNote_Base
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

    public Return getJS4SelectInvoiceForm(final Parameter _parameter)
        throws EFapsException
    {
        final StringBuilder js = new StringBuilder();
        js.append("<script type=\"text/javascript\">")
                        .append("Wicket.Event.add(window, \"domready\", function(event) {")
                        .append("var obj=wicketGet(\"label25\");")
                        .append("obj.setfocus();")
                        .append(" });")
                        .append("</script>");

        final Return retVal = new Return();
        retVal.put(ReturnValues.SNIPLETT, js.toString());
        return retVal;
    }

    /**
     * PositionNumber must start with 1.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return new empty Return
     * @throws EFapsException on error
     */
    public Return deliveryNotePositionInsertTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final Map<String, String[]> param = Context.getThreadContext().getParameters();
        final String[] storageIds = param.get("storage");
        final String[] date = param.get("date");

        final Instance instance = _parameter.getInstance();
        final Map<?, ?> map = (Map<?, ?>) _parameter.get(ParameterValues.NEW_VALUES);

        final Object[] productID = (Object[]) map.get(instance.getType().getAttribute(
                        CISales.DeliveryNotePosition.Product.name));
        final Object[] qauntity = (Object[]) map.get(instance.getType().getAttribute(
                        CISales.DeliveryNotePosition.Quantity.name));
        final Object[] uom = (Object[]) map.get(instance.getType().getAttribute(CISales.DeliveryNotePosition.UoM.name));
        final Object[] pos = (Object[]) map.get(instance.getType().getAttribute(
                        CISales.DeliveryNotePosition.PositionNumber.name));

        Object[] deliveryNoteId = (Object[]) map.get(instance.getType().getAttribute(
                        CISales.DeliveryNotePosition.DeliveryNote.name));
        // if it did not work check the abstract attribute
        if (deliveryNoteId == null) {
            deliveryNoteId = (Object[]) map.get(instance.getType().getAttribute(
                            CISales.RecievingTicketPosition.DocumentAbstractLink.name));
        }

        Long storage = null;
        if (storageIds != null) {
            final Integer posInt = (Integer) pos[0];
            storage = Long.valueOf(storageIds[posInt - 1]);
        } else {
            final QueryBuilder query = new QueryBuilder(CIProducts.Inventory);
            query.addWhereAttrEqValue(CIProducts.Inventory.Product, productID[0]);
            final MultiPrintQuery multi = query.getPrint();
            multi.addAttribute(CIProducts.Inventory.Storage);
            multi.execute();
            if (multi.next()) {
                storage = multi.<Long>getAttribute(CIProducts.Inventory.Storage);
            }
        }
        // Sales-Configuration
        final SystemConfiguration conf = SystemConfiguration.get(UUID
                        .fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f"));
        if (conf.getAttributeValueAsBoolean("DeliveryNote_TransactionTrigger4Reservation")) {
            final Instance contactInst = Instance.get(param.get("contact")[0]);
            String quantitystring = qauntity[0].toString();
            quantitystring = quantitystring.replace(",", "");
            BigDecimal quantity = new BigDecimal(quantitystring);
            final Map<Instance, Map<Instance, BigDecimal>> res2pos = new HashMap<Instance, Map<Instance, BigDecimal>>();
            if (contactInst.isValid()) {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Reservation);
                attrQueryBldr.addWhereAttrEqValue(CISales.Reservation.Status,
                                Status.find(CISales.ReservationStatus.uuid, "Open").getId());
                attrQueryBldr.addWhereAttrEqValue(CISales.Reservation.Contact, contactInst.getId());
                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.Reservation.ID);

                final QueryBuilder queryBldr = new QueryBuilder(CISales.ReservationPosition);
                queryBldr.addWhereAttrEqValue(CISales.ReservationPosition.Product, productID[0]);
                queryBldr.addWhereAttrInQuery(CISales.ReservationPosition.Reservation, attrQuery);
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CISales.ReservationPosition.Quantity);
                final SelectBuilder sel = new SelectBuilder().linkto(CISales.ReservationPosition.Reservation).oid();
                multi.addSelect(sel);
                multi.execute();
                while (multi.next()) {
                    BigDecimal reservedTmp = multi
                                    .<BigDecimal>getAttribute(CISales.ReservationPosition.Quantity) != null
                                    ? multi.<BigDecimal>getAttribute(CISales.ReservationPosition.Quantity)
                                    : BigDecimal.ZERO;
                    // less delivered than reserved
                    if (quantity.subtract(reservedTmp).signum() == -1) {
                        reservedTmp = quantity;
                    }
                    if (quantity.signum() == 1) {
                        final Insert insert = new Insert(CIProducts.TransactionReservationOutbound);
                        insert.add(CIProducts.TransactionAbstract.Quantity, reservedTmp);
                        insert.add(CIProducts.TransactionAbstract.Storage, storage);
                        insert.add(CIProducts.TransactionAbstract.Product, productID[0]);
                        insert.add(CIProducts.TransactionAbstract.Description,
                                        DBProperties.getProperty("org.efaps.esjp.sales.document.DeliveryNote.description4Trigger"));
                        insert.add(CIProducts.TransactionAbstract.Date, date[0] == null ? new DateTime() : date[0]);
                        insert.add(CIProducts.TransactionAbstract.Document, deliveryNoteId[0]);
                        insert.add(CIProducts.TransactionAbstract.UoM, uom[0]);
                        insert.execute();

                        final Instance resInst = Instance.get(multi.<String>getSelect(sel));
                        Map<Instance, BigDecimal> pos2Reserved;
                        if (res2pos.containsKey(resInst)) {
                            pos2Reserved = res2pos.get(resInst);
                        } else {
                            pos2Reserved = new HashMap<Instance, BigDecimal>();
                            res2pos.put(resInst, pos2Reserved);
                        }
                        pos2Reserved.put(multi.getCurrentInstance(), reservedTmp);
                        quantity = quantity.subtract(reservedTmp);
                    }
                }
            }
            for (final Entry<Instance, Map<Instance, BigDecimal>> entry : res2pos.entrySet()) {
                updateReservation(_parameter, entry.getKey(), entry.getValue());
            }
        }

        final Insert insert = new Insert(CIProducts.TransactionOutbound);
        insert.add(CIProducts.TransactionOutbound.Quantity, qauntity[0]);
        insert.add(CIProducts.TransactionOutbound.Storage, storage);
        insert.add(CIProducts.TransactionOutbound.Product, productID[0]);
        insert.add(CIProducts.TransactionOutbound.Description,
                        DBProperties.getProperty("org.efaps.esjp.sales.document.DeliveryNote.description4Trigger"));
        insert.add(CIProducts.TransactionOutbound.Date, new DateTime());
        insert.add(CIProducts.TransactionOutbound.Document, deliveryNoteId[0]);
        insert.add(CIProducts.TransactionOutbound.UoM, uom[0]);
        insert.execute();
        return new Return();
    }

    protected void updateReservation(final Parameter _parameter,
                                     final Instance _reservationInstance,
                                     final Map<Instance, BigDecimal> _pos2Reserved)
        throws EFapsException
    {
        final Parameter parameter = new Parameter();
        parameter.put(ParameterValues.INSTANCE, _reservationInstance);
        parameter.put(ParameterValues.PARAMETERS, _parameter.get(ParameterValues.PARAMETERS));
        parameter.put(ParameterValues.PROPERTIES, _parameter.get(ParameterValues.PROPERTIES));

        @SuppressWarnings("unchecked") final Map<Object, Object> props = (Map<Object, Object>) _parameter
                        .get(ParameterValues.PROPERTIES);
        props.put("Status", "Replaced");

        final Revision revision = new Revision()
        {

            @Override
            protected Instance copyDoc(final Parameter _parameter)
                throws EFapsException
            {
                Instance ret = null;
                final QueryBuilder queryBuilder = new QueryBuilder(CISales.ReservationPosition);
                queryBuilder.addWhereAttrEqValue(CISales.ReservationPosition.Reservation, _reservationInstance.getId());
                final MultiPrintQuery multi = queryBuilder.getPrint();
                while (multi.next()) {
                    final Insert insert = new Insert(CISales.ReservationPosition);
                    final Set<String> added = new HashSet<String>();
                    added.add(CISales.ReservationPosition.getType()
                                    .getAttribute(CISales.ReservationPosition.Reservation.name).getSqlColNames()
                                    .toString());
                    boolean execute = false;
                    if (_pos2Reserved.containsKey(multi.getCurrentInstance())) {
                        final PrintQuery print = new PrintQuery(multi.getCurrentInstance());
                        print.addAttribute(CISales.ReservationPosition.Quantity);
                        print.executeWithoutAccessCheck();
                        final BigDecimal quantity = print
                                        .<BigDecimal>getAttribute(CISales.ReservationPosition.Quantity);
                        final BigDecimal reserved = _pos2Reserved.get(multi.getCurrentInstance());

                        if (quantity.subtract(reserved).signum() == 1) {
                            insert.add(CISales.ReservationPosition.Quantity, quantity.subtract(reserved));
                            added.add(CISales.ReservationPosition.getType()
                                            .getAttribute(CISales.ReservationPosition.Quantity.name).getSqlColNames()
                                            .toString());
                            execute = true;
                        }
                    } else {
                        execute = true;
                    }
                    if (execute) {
                        if (ret == null) {
                            ret = super.copyDoc(_parameter);
                        }
                        insert.add(CISales.ReservationPosition.Reservation, ret.getId());
                        addAttributes(_parameter, multi.getCurrentInstance(), insert, added);
                        insert.executeWithoutTrigger();
                    }
                }
                // if no revision was made correct the status
                if (ret == null) {
                    final Update update = new Update(_reservationInstance);
                    update.add(CISales.Reservation.Status,
                                    Status.find(CISales.ReservationStatus.uuid, "Closed").getId());
                    update.executeWithoutAccessCheck();
                }
                return ret;
            }
        };
        revision.revise(parameter);
    }
}
