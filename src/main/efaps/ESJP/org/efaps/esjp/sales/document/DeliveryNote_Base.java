/*
 * Copyright 2003 - 2016 The eFaps Team
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
 */

package org.efaps.esjp.sales.document;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.efaps.admin.common.MsgPhrase;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.ci.CIType;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.contacts.Contacts;
import org.efaps.esjp.erp.Revision;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("363ad7a5-1e7b-4194-89e3-a31d07d783df")
@EFapsApplication("eFapsApp-Sales")
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
        connect2Terms(_parameter, doc);

        final File file = createReport(_parameter, doc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
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

        final String transferReason = _parameter
                        .getParameterValue(CIFormSales.Sales_DeliveryNoteForm.transferReason.name);
        if (transferReason != null && !transferReason.isEmpty()) {
            _insert.add(CISales.DeliveryNote.TransferReason, transferReason);
            _createdDoc.getValues().put(CISales.DeliveryNote.TransferReason.name, transferReason);
        }
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final CreatedDoc doc = editDoc(_parameter);
        connect2Object(_parameter, doc);
        final File file = createReport(_parameter, doc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    @Override
    protected void add2DocEdit(final Parameter _parameter,
                               final Update _update,
                               final EditedDoc _editDoc)
        throws EFapsException
    {
        super.add2DocEdit(_parameter, _update, _editDoc);
        final String arrivalPoint = _parameter.getParameterValue(CIFormSales.Sales_DeliveryNoteForm.arrivalPoint.name);
        if (arrivalPoint != null) {
            _update.add(CISales.DeliveryNote.ArrivalPoint, arrivalPoint);
            _editDoc.getValues().put(CISales.DeliveryNote.ArrivalPoint.name, arrivalPoint);
        }

        final String departurePoint = _parameter
                        .getParameterValue(CIFormSales.Sales_DeliveryNoteForm.departurePoint.name);
        if (departurePoint != null) {
            _update.add(CISales.DeliveryNote.DeparturePoint, departurePoint);
            _editDoc.getValues().put(CISales.DeliveryNote.DeparturePoint.name, departurePoint);
        }

        final String vehicleBrand = _parameter.getParameterValue(CIFormSales.Sales_DeliveryNoteForm.vehicleBrand.name);
        if (vehicleBrand != null) {
            _update.add(CISales.DeliveryNote.VehicleBrand, vehicleBrand);
            _editDoc.getValues().put(CISales.DeliveryNote.VehicleBrand.name, vehicleBrand);
        }

        final String vehicleDriverInfo = _parameter
                        .getParameterValue(CIFormSales.Sales_DeliveryNoteForm.vehicleDriverInfo.name);
        if (vehicleDriverInfo != null) {
            _update.add(CISales.DeliveryNote.VehicleDriverInfo, vehicleDriverInfo);
            _editDoc.getValues().put(CISales.DeliveryNote.VehicleDriverInfo.name, vehicleDriverInfo);
        }

        final String vehicleLicencePlate = _parameter
                        .getParameterValue(CIFormSales.Sales_DeliveryNoteForm.vehicleLicencePlate.name);
        if (vehicleLicencePlate != null) {
            _update.add(CISales.DeliveryNote.VehicleLicencePlate, vehicleLicencePlate);
            _editDoc.getValues().put(CISales.DeliveryNote.VehicleLicencePlate.name, vehicleLicencePlate);
        }

        final Instance carrierInst = Instance.get(_parameter
                        .getParameterValue(CIFormSales.Sales_DeliveryNoteForm.carrierLink.name));
        if (carrierInst.isValid()) {
            _update.add(CISales.DeliveryNote.CarrierLink, carrierInst);
            _editDoc.getValues().put(CISales.DeliveryNote.CarrierLink.name, carrierInst);
        }

        final String transferReason = _parameter
                        .getParameterValue(CIFormSales.Sales_DeliveryNoteForm.transferReason.name);
        if (transferReason != null && !transferReason.isEmpty()) {
            _update.add(CISales.DeliveryNote.TransferReason, transferReason);
            _editDoc.getValues().put(CISales.DeliveryNote.TransferReason.name, transferReason);
        }
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
            final List<String> depPoints = Sales.DELIVERYNOTEDEFAULTDEPARTUREPOINTS.get();
            if (!depPoints.isEmpty()) {
                final FieldValue fieldValue = (FieldValue) _parameter.get(ParameterValues.UIOBJECT);
                fieldValue.setValue(depPoints.get(0).trim());
            }
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @return return containing default value for create mode
     * @throws EFapsException on error
     */
    public Return autoComplete4DeparturePoint(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        boolean all = false;
        if (input.isEmpty() || "*".equals(input)) {
            all = true;
        }
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final List<String> depPoints = Sales.DELIVERYNOTEDEFAULTDEPARTUREPOINTS.get();
        for (final String depPoint : depPoints) {
            if (all || StringUtils.startsWithIgnoreCase(depPoint, input)) {
                final Map<String, String> map = new HashMap<String, String>();
                map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), depPoint);
                list.add(map);
            }
        }
        add2AutoComplete4DeparturePoint(_parameter, list);

        Collections.sort(list, new Comparator<Map<String, String>>()
        {

            @Override
            public int compare(final Map<String, String> _o1,
                               final Map<String, String> _o2)
            {
                return _o1.get(EFapsKey.AUTOCOMPLETE_VALUE.getKey()).compareTo(
                                _o2.get(EFapsKey.AUTOCOMPLETE_VALUE.getKey()));
            }
        });
        ret.put(ReturnValues.VALUES, list);
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _list     list to add to
     * @throws EFapsException on error
     */
    protected void add2AutoComplete4DeparturePoint(final Parameter _parameter,
                                                   final List<Map<String, String>> _list)
        throws EFapsException
    {
        // to be used by implementation
    }

    @Override
    protected void add2UpdateMap4Contact(final Parameter _parameter,
                                         final Instance _contactInstance,
                                         final Map<String, Object> _map)
        throws EFapsException
    {
        super.add2UpdateMap4Contact(_parameter, _contactInstance, _map);
        final PrintQuery print = new PrintQuery(_contactInstance);
        final MsgPhrase msgPhrase = MsgPhrase.get(UUID.fromString(Sales.DELIVERYNOTECONTMSGPH4ARP.get()));
        print.addMsgPhrase(msgPhrase);
        print.execute();
        final String address = print.getMsgPhrase(msgPhrase);
        _map.put(CIFormSales.Sales_DeliveryNoteForm.arrivalPoint.name, address);
    }


    @Override
    protected StringBuilder add2JavaScript4DocumentContact(final Parameter _parameter,
                                                           final List<Instance> _instances,
                                                           final Instance _contactInstance)
        throws EFapsException
    {
        final StringBuilder ret = super.add2JavaScript4DocumentContact(_parameter, _instances, _contactInstance);
        final PrintQuery print = new PrintQuery(_contactInstance);
        final MsgPhrase msgPhrase = MsgPhrase.get(UUID.fromString(Sales.DELIVERYNOTECONTMSGPH4ARP.get()));
        print.addMsgPhrase(msgPhrase);
        print.execute();
        final String adress = print.getMsgPhrase(msgPhrase);
        ret.append(getSetFieldValue(0, CIFormSales.Sales_DeliveryNoteForm.arrivalPoint.name, adress));
        return ret;
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
     * @param _parameter Parameter as passed from the eFaps API.
     * @return return containing default value for create mode
     * @throws EFapsException on error
     */
    public Return updateFields4Carrier(final Parameter _parameter)
        throws EFapsException
    {
        return new Contacts()
        {

            @SuppressWarnings("unchecked")
            @Override
            protected void add2UpdateMap4Contact(final Parameter _parameter,
                                                 final Instance _instance,
                                                 final Map<String, Object> _map)
                throws EFapsException
            {
                final QueryBuilder queryBldr = new QueryBuilder(CIContacts.ClassCarrier);
                queryBldr.addWhereAttrEqValue(CIContacts.ClassCarrier.ContactLink, _instance);
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttributeSet(CIContacts.ClassCarrier.CarrierSet.name);
                multi.addAttributeSet(CIContacts.ClassCarrier.DriverSet.name);
                multi.execute();
                if (multi.next()) {
                    final Map<String, Object> dataCarrier = multi
                                    .getAttributeSet(CIContacts.ClassCarrier.CarrierSet.name);
                    final Map<String, Object> dataDriver = multi
                                    .getAttributeSet(CIContacts.ClassCarrier.DriverSet.name);
                    if (dataCarrier == null) {
                        _map.put(CIFormSales.Sales_DeliveryNoteForm.vehicleBrand.name, "");
                        _map.put(CIFormSales.Sales_DeliveryNoteForm.vehicleLicencePlate.name, "");
                    } else {
                        final List<String> make = (ArrayList<String>) dataCarrier.get("Make");
                        final List<String> registration = (ArrayList<String>) dataCarrier.get("Registration");
                        final List<Boolean> defaults = (ArrayList<Boolean>) dataCarrier.get("DefaultSelected");
                        int i = 0;
                        boolean sel = false;
                        for (final Boolean bol : defaults) {
                            if (bol) {
                                sel = true;
                                break;
                            }
                            i++;
                        }
                        _map.put(CIFormSales.Sales_DeliveryNoteForm.vehicleBrand.name,
                                        make.isEmpty() || !sel ? "" : make.get(i));
                        _map.put(CIFormSales.Sales_DeliveryNoteForm.vehicleLicencePlate.name,
                                        registration.isEmpty() || !sel ? "" : registration.get(i));
                    }
                    if (dataDriver == null) {
                        _map.put(CIFormSales.Sales_DeliveryNoteForm.vehicleDriverInfo.name, "");
                    } else {
                        final ArrayList<String> license = (ArrayList<String>) dataDriver.get("License");
                        final List<Boolean> defaults = (ArrayList<Boolean>) dataDriver.get("DefaultSelected");
                        int i = 0;
                        boolean sel = false;
                        for (final Boolean bol : defaults) {
                            if (bol) {
                                sel = true;
                                break;
                            }
                            i++;
                        }
                        _map.put(CIFormSales.Sales_DeliveryNoteForm.vehicleDriverInfo.name,
                                        license.isEmpty() || !sel ? "" : license.get(i));
                    }
                }
            }
        }.updateFields4Contact(_parameter);
    }

    /**
     * Used by the AutoCompleteField used in the select contact.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4ArrivalPoint(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        boolean all = false;
        if (input.isEmpty() || "*".equals(input)) {
            all = true;
        }

        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();

        final Instance contactInst = Instance.get(_parameter
                        .getParameterValue(CIFormSales.Sales_DeliveryNoteForm.contact.name));
        if (contactInst.isValid()) {
            final PrintQuery print = new PrintQuery(contactInst);
            final MsgPhrase msgPhrase = MsgPhrase.get(UUID.fromString(Sales.DELIVERYNOTECONTMSGPH4ARP.get()));
            print.addMsgPhrase(msgPhrase);
            print.execute();
            final String address = print.getMsgPhrase(msgPhrase);
            if (all || StringUtils.startsWithIgnoreCase(address, input)) {
                final Map<String, String> map = new HashMap<String, String>();
                map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), address);
                map.put(EFapsKey.AUTOCOMPLETE_CHOICE.getKey(), getFormatedDBProperty("ContactArrivalPoint", address));
                list.add(map);
            }

            if (org.efaps.esjp.contacts.util.Contacts.ACTIVATESUBCONTACTS.get()) {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CIContacts.Contact2SubContact);
                attrQueryBldr.addWhereAttrEqValue(CIContacts.Contact2SubContact.From, contactInst);
                final QueryBuilder queryBldr = new QueryBuilder(CIContacts.SubContact);
                queryBldr.addWhereAttrInQuery(CIContacts.SubContact.ID,
                                attrQueryBldr.getAttributeQuery(CIContacts.Contact2SubContact.To));
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CIContacts.SubContact.Name);
                final MsgPhrase subMsgPhrase = MsgPhrase.get(UUID.fromString(Sales.DELIVERYNOTESUBCONTMSGPH4ARP.get()));
                multi.addMsgPhrase(subMsgPhrase);
                multi.execute();
                int idx = 1;
                while (multi.next()) {
                    final String loAddress = multi.getMsgPhrase(subMsgPhrase);
                    if (all || StringUtils.startsWithIgnoreCase(loAddress, input)) {
                        final Map<String, String> map = new HashMap<String, String>();
                        map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), loAddress);
                        map.put(EFapsKey.AUTOCOMPLETE_CHOICE.getKey(),
                                        getFormatedDBProperty("SubContactArrivalPoint", loAddress, idx,
                                                        multi.getAttribute(CIContacts.SubContact.Name)));
                        list.add(map);
                        idx++;
                    }
                }
            }
        }

        final Instance carrierInst = Instance.get(_parameter
                        .getParameterValue(CIFormSales.Sales_DeliveryNoteForm.carrierLink.name));
        if (carrierInst.isValid()) {
            final PrintQuery print = new PrintQuery(carrierInst);
            final MsgPhrase msgPhrase = MsgPhrase.get(UUID.fromString(Sales.DELIVERYNOTECONTMSGPH4ARP.get()));
            print.addMsgPhrase(msgPhrase);
            print.execute();
            final String address = print.getMsgPhrase(msgPhrase);
            if (all || StringUtils.startsWithIgnoreCase(address, input)) {
                final Map<String, String> map = new HashMap<String, String>();
                map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), address);
                map.put(EFapsKey.AUTOCOMPLETE_CHOICE.getKey(), getFormatedDBProperty("CarrierArrivalPoint", address));
                list.add(map);
            }
        }

        final List<String> destinations = Sales.DELIVERYNOTEDEFAULTARRIVALPOINTS.get();
        int idx = 1;
        for (final String destination : destinations) {
            if (all || StringUtils.startsWithIgnoreCase(destination, input)) {
                final Map<String, String> map = new HashMap<String, String>();
                map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), destination);
                map.put(EFapsKey.AUTOCOMPLETE_CHOICE.getKey(),
                                getFormatedDBProperty("ConfiguredArrivalPoint", destination, idx));
                list.add(map);
                idx++;
            }
        }

        add2AutoComplete4ArrivalPoint(_parameter, list);

        Collections.sort(list, new Comparator<Map<String, String>>()
        {
            @Override
            public int compare(final Map<String, String> _o1,
                               final Map<String, String> _o2)
            {
                return _o1.get(EFapsKey.AUTOCOMPLETE_CHOICE.getKey()).compareTo(
                                _o2.get(EFapsKey.AUTOCOMPLETE_CHOICE.getKey()));
            }
        });
        ret.put(ReturnValues.VALUES, list);
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _list     list to add to
     *  @throws EFapsException on error
     */
    protected void add2AutoComplete4ArrivalPoint(final Parameter _parameter,
                                                 final List<Map<String, String>> _list)
        throws EFapsException
    {
        // to be used by implementation
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
        if (Sales.RESERVATIONACTIVATETRANSTRIG.get()) {
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
                        final Map<Instance, BigDecimal> pos2Reserved;
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

    @Override
    public String getTypeName4AutoComplete4Product(final Parameter _parameter)
        throws EFapsException
    {
        return CISales.DeliveryNote.getType().getName();
    }


    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4VehicleBrand(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        boolean all = false;
        if (input.isEmpty() || "*".equals(input)) {
            all = true;
        }

        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();

        final Instance carrierInst = Instance.get(_parameter
                        .getParameterValue(CIFormSales.Sales_DeliveryNoteForm.carrierLink.name));
        if (carrierInst.isValid()) {
            final QueryBuilder queryBldr = new QueryBuilder(CIContacts.ClassCarrier);
            queryBldr.addWhereAttrEqValue(CIContacts.ClassCarrier.ContactLink, carrierInst);
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttributeSet(CIContacts.ClassCarrier.CarrierSet.name);
            multi.addAttributeSet(CIContacts.ClassCarrier.DriverSet.name);
            multi.execute();
            if (multi.next()) {
                final Map<String, Object> dataCarrier = multi
                                .getAttributeSet(CIContacts.ClassCarrier.CarrierSet.name);

                if (dataCarrier != null) {
                    @SuppressWarnings("unchecked")
                    final List<String> values = (ArrayList<String>) dataCarrier.get("Make");
                    for (final String val : values) {
                        if (all || StringUtils.startsWithIgnoreCase(val, input)) {
                            final Map<String, String> map = new HashMap<String, String>();
                            map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), val);
                            list.add(map);
                        }
                    }
                }
            }
        }
        ret.put(ReturnValues.VALUES, list);
        return ret;
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4VehicleLicencePlate(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        boolean all = false;
        if (input.isEmpty() || "*".equals(input)) {
            all = true;
        }

        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();

        final Instance carrierInst = Instance.get(_parameter
                        .getParameterValue(CIFormSales.Sales_DeliveryNoteForm.carrierLink.name));
        if (carrierInst.isValid()) {
            final QueryBuilder queryBldr = new QueryBuilder(CIContacts.ClassCarrier);
            queryBldr.addWhereAttrEqValue(CIContacts.ClassCarrier.ContactLink, carrierInst);
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttributeSet(CIContacts.ClassCarrier.CarrierSet.name);
            multi.execute();
            if (multi.next()) {
                final Map<String, Object> dataCarrier = multi
                                .getAttributeSet(CIContacts.ClassCarrier.CarrierSet.name);

                if (dataCarrier != null) {
                    @SuppressWarnings("unchecked")
                    final List<String> values = (ArrayList<String>) dataCarrier.get("Registration");
                    for (final String val : values) {
                        if (all || StringUtils.startsWithIgnoreCase(val, input)) {
                            final Map<String, String> map = new HashMap<String, String>();
                            map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), val);
                            list.add(map);
                        }
                    }
                }
            }
        }
        ret.put(ReturnValues.VALUES, list);
        return ret;
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4VehicleDriverInfo(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        boolean all = false;
        if (input.isEmpty() || "*".equals(input)) {
            all = true;
        }

        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();

        final Instance carrierInst = Instance.get(_parameter
                        .getParameterValue(CIFormSales.Sales_DeliveryNoteForm.carrierLink.name));
        if (carrierInst.isValid()) {
            final QueryBuilder queryBldr = new QueryBuilder(CIContacts.ClassCarrier);
            queryBldr.addWhereAttrEqValue(CIContacts.ClassCarrier.ContactLink, carrierInst);
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttributeSet(CIContacts.ClassCarrier.DriverSet.name);
            multi.execute();
            if (multi.next()) {
                final Map<String, Object> dataCarrier = multi
                                .getAttributeSet(CIContacts.ClassCarrier.DriverSet.name);

                if (dataCarrier != null) {
                    @SuppressWarnings("unchecked")
                    final List<String> values = (ArrayList<String>) dataCarrier.get("License");
                    for (final String val : values) {
                        if (all || StringUtils.startsWithIgnoreCase(val, input)) {
                            final Map<String, String> map = new HashMap<String, String>();
                            map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), val);
                            list.add(map);
                        }
                    }
                }
            }
        }
        ret.put(ReturnValues.VALUES, list);
        return ret;
    }

    @Override
    public CIType getCIType()
        throws EFapsException
    {
        return CISales.DeliveryNote;
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new return
     * @throws EFapsException on error
     */
    public Return connect2InvoiceTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_parameter.getInstance());
        final SelectBuilder selStatus = SelectBuilder.get().linkto(CISales.Invoice2DeliveryNote.ToLink)
                        .attribute(CISales.DeliveryNote.Status);
        final SelectBuilder selDelNoteInst = SelectBuilder.get().linkto(CISales.Invoice2DeliveryNote.ToLink)
                        .instance();
        final SelectBuilder selInvoiceInst = SelectBuilder.get().linkto(CISales.Invoice2DeliveryNote.FromLink)
                        .instance();
        print.addSelect(selStatus, selDelNoteInst, selInvoiceInst);
        print.addAttribute(CISales.Invoice2DeliveryNote.ToLink,
                        CISales.Invoice2DeliveryNote.FromLink);
        print.executeWithoutAccessCheck();
        final Status status = Status.get(print.<Long>getSelect(selStatus));
        // if the deliverynote ticket was open check if the status must change
        if (status.equals(Status.find(CISales.DeliveryNoteStatus.Open))) {
            final Instance delNoteInst = print.<Instance>getSelect(selDelNoteInst);
            final DocComparator comp = new DocComparator();
            comp.setDocInstance(delNoteInst);
            // check for the case that there are n Invoices for the given DeliveryNote
            final QueryBuilder queryBldr = new QueryBuilder(CISales.Invoice2DeliveryNote);
            queryBldr.addWhereAttrEqValue(CISales.Invoice2DeliveryNote.ToLink, delNoteInst);
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selInvInst = SelectBuilder.get()
                            .linkto(CISales.Invoice2DeliveryNote.FromLink)
                            .instance();
            multi.addSelect(selInvInst);
            multi.executeWithoutAccessCheck();
            while (multi.next()) {
                final Instance invInst = multi.getSelect(selInvInst);
                final DocComparator docComp = new DocComparator();
                docComp.setDocInstance(invInst);
                comp.substractQuantity(docComp);
            }

            if (comp.quantityIsZero()) {
                final Update update = new Update(delNoteInst);
                update.add(CISales.DeliveryNote.Status, Status.find(CISales.DeliveryNoteStatus.Closed));
                update.executeWithoutAccessCheck();
            } else {
                // check for the case that for the n DeliveryNotes for the Invoice
                final Instance invoiceInst = print.<Instance>getSelect(selInvoiceInst);
                final DocComparator invComp = new DocComparator();
                invComp.setDocInstance(invoiceInst);
                // check for the case that there are n Invoices for the given DeliveryNote
                final QueryBuilder queryBldr2 = new QueryBuilder(CISales.Invoice2DeliveryNote);
                queryBldr2.addWhereAttrEqValue(CISales.Invoice2DeliveryNote.FromLink, invoiceInst);
                final MultiPrintQuery multi2 = queryBldr2.getPrint();
                final SelectBuilder selDelInst = SelectBuilder.get()
                                .linkto(CISales.Invoice2DeliveryNote.ToLink)
                                .instance();
                final SelectBuilder selDelStatus = SelectBuilder.get()
                                .linkto(CISales.Invoice2DeliveryNote.ToLink).status();
                multi2.addSelect(selDelInst, selDelStatus);
                multi2.executeWithoutAccessCheck();
                final Set<Instance> delInsts = new HashSet<>();
                while (multi2.next()) {
                    final Instance delInst = multi2.getSelect(selDelInst);
                    final Status delStatus = multi2.getSelect(selDelStatus);
                    if (!delStatus.equals(Status.find(CISales.DeliveryNoteStatus.Canceled))) {
                        final DocComparator docComp = new DocComparator();
                        docComp.setDocInstance(delInst);
                        invComp.substractQuantity(docComp);
                        if (delStatus.equals(Status.find(CISales.DeliveryNoteStatus.Open))) {
                            delInsts.add(delInst);
                        }
                    }
                }
                if (invComp.quantityIsZero()) {
                    for (final Instance inst : delInsts) {
                        final Update update = new Update(inst);
                        update.add(CISales.DeliveryNote.Status, Status.find(CISales.DeliveryNoteStatus.Closed));
                        update.executeWithoutAccessCheck();
                    }
                }
            }
        }
        return new Return();
    }

}
