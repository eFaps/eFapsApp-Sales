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

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.contacts.Contacts;
import org.efaps.esjp.erp.Revision;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.SalesSettings;
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
        final Return ret = new Return();
        final CreatedDoc doc = createDoc(_parameter);
        createPositions(_parameter, doc);
        connect2ProductDocumentType(_parameter, doc);
        connect2Derived(_parameter, doc);
        connect2Object(_parameter, doc);

        final File file = createReport(_parameter, doc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @return return containing default value for create mode
     * @throws EFapsException on error
     */
    public Return departurePointFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        if (TargetMode.CREATE.equals(_parameter.get(ParameterValues.ACCESSMODE))) {
            final String depPoint = Sales.getSysConfig().getAttributeValue(SalesSettings.DEFAULTDEPARTUREPOINT);
            final FieldValue fieldValue = (FieldValue) _parameter.get(ParameterValues.UIOBJECT);
            fieldValue.setValue(depPoint);
        }
        return ret;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void add2DocCreate(final Parameter _parameter,
                                 final Insert _insert,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        super.add2DocCreate(_parameter, _insert, _createdDoc);

        final String arrivalPoint = _parameter.getParameterValue(CIFormSales.Sales_DeliveryNoteForm.arrivalPoint.name);
        if (arrivalPoint != null) {
            _insert.add(CISales.DeliveryNote.ArrivalPoint, arrivalPoint);
            _createdDoc.getValues().put(CISales.DeliveryNote.ArrivalPoint.name, arrivalPoint);
        }

        final String departurePoint = _parameter
                        .getParameterValue(CIFormSales.Sales_DeliveryNoteForm.departurePoint.name);
        if (departurePoint != null) {
            _insert.add(CISales.DeliveryNote.DeparturePoint, departurePoint);
            _createdDoc.getValues().put(CISales.DeliveryNote.DeparturePoint.name, departurePoint);
        }

        final String vehicleBrand = _parameter.getParameterValue(CIFormSales.Sales_DeliveryNoteForm.vehicleBrand.name);
        if (vehicleBrand != null) {
            _insert.add(CISales.DeliveryNote.VehicleBrand, vehicleBrand);
            _createdDoc.getValues().put(CISales.DeliveryNote.VehicleBrand.name, vehicleBrand);
        }

        final String vehicleDriverInfo = _parameter
                        .getParameterValue(CIFormSales.Sales_DeliveryNoteForm.vehicleDriverInfo.name);
        if (vehicleDriverInfo != null) {
            _insert.add(CISales.DeliveryNote.VehicleDriverInfo, vehicleDriverInfo);
            _createdDoc.getValues().put(CISales.DeliveryNote.VehicleDriverInfo.name, vehicleDriverInfo);
        }

        final String vehicleLicencePlate = _parameter
                        .getParameterValue(CIFormSales.Sales_DeliveryNoteForm.vehicleLicencePlate.name);
        if (vehicleLicencePlate != null) {
            _insert.add(CISales.DeliveryNote.VehicleLicencePlate, vehicleLicencePlate);
            _createdDoc.getValues().put(CISales.DeliveryNote.VehicleLicencePlate.name, vehicleLicencePlate);
        }

        final Instance carrierInst = Instance.get(_parameter
                        .getParameterValue(CIFormSales.Sales_DeliveryNoteForm.carrierLink.name));
        if (carrierInst.isValid()) {
            _insert.add(CISales.DeliveryNote.CarrierLink, carrierInst);
            _createdDoc.getValues().put(CISales.DeliveryNote.CarrierLink.name, carrierInst);
        }
    }

    /**
     * Used by the AutoCompleteField used in the select contact.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4Carrier(final Parameter _parameter)
        throws EFapsException
    {
        final Contacts contacts = new Contacts();
        return contacts.autoComplete4Contact(_parameter);
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

        final String[] date = param.get("date");

        final Instance instance = _parameter.getInstance();
        final Map<?, ?> map = (Map<?, ?>) _parameter.get(ParameterValues.NEW_VALUES);

        final Object[] productID = (Object[]) map.get(instance.getType().getAttribute(
                        CISales.DeliveryNotePosition.Product.name));
        final Object[] qauntity = (Object[]) map.get(instance.getType().getAttribute(
                        CISales.DeliveryNotePosition.Quantity.name));
        final Object[] uom = (Object[]) map.get(instance.getType().getAttribute(CISales.DeliveryNotePosition.UoM.name));


        Object[] deliveryNoteId = (Object[]) map.get(instance.getType().getAttribute(
                        CISales.DeliveryNotePosition.DeliveryNote.name));
        // if it did not work check the abstract attribute
        if (deliveryNoteId == null) {
            deliveryNoteId = (Object[]) map.get(instance.getType().getAttribute(
                            CISales.RecievingTicketPosition.DocumentAbstractLink.name));
        }

        final Long storageID = evaluateStorage4PositionTrigger(_parameter);
        if (Sales.getSysConfig().getAttributeValueAsBoolean("DeliveryNote_TransactionTrigger4Reservation")) {
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
                        insert.add(CIProducts.TransactionAbstract.Storage, storageID);
                        insert.add(CIProducts.TransactionAbstract.Product, productID[0]);
                        insert.add(CIProducts.TransactionAbstract.Description, DBProperties.getProperty(
                                        "org.efaps.esjp.sales.document.DeliveryNote.description4Trigger"));
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
        createTransaction4PositionTrigger(_parameter, CIProducts.TransactionOutbound.getType(), storageID);

        return new Return();
    }

    /**
     * @param _parameter    Paramter as passed by the eFaps API
     * @param _reservationInstance istance of the reservation
     * @param _pos2Reserved position mapping
     * @throws EFapsException on error
     */
    protected void updateReservation(final Parameter _parameter,
                                     final Instance _reservationInstance,
                                     final Map<Instance, BigDecimal> _pos2Reserved)
        throws EFapsException
    {
        final Parameter parameter = new Parameter();
        parameter.put(ParameterValues.INSTANCE, _reservationInstance);
        parameter.put(ParameterValues.PARAMETERS, _parameter.get(ParameterValues.PARAMETERS));
        parameter.put(ParameterValues.PROPERTIES, _parameter.get(ParameterValues.PROPERTIES));

        @SuppressWarnings("unchecked")
        final Map<Object, Object> props = (Map<Object, Object>) _parameter.get(ParameterValues.PROPERTIES);
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
