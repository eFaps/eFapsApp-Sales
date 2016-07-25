/*
 * Copyright 2003 - 2014 The eFaps Team
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

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 *
 */
@EFapsUUID("98066068-5423-4f7e-a8e8-d90574dc5b82")
@EFapsApplication("eFapsApp-Sales")
public abstract class ConsignmentNote_Base
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
        return new DeliveryNote().departurePointFieldValue(_parameter);
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
            _insert.add(CISales.ConsignmentNote.ArrivalPoint, arrivalPoint);
            _createdDoc.getValues().put(CISales.ConsignmentNote.ArrivalPoint.name, arrivalPoint);
        }

        final String departurePoint = _parameter
                        .getParameterValue(CIFormSales.Sales_ConsignmentNoteForm.departurePoint.name);
        if (departurePoint != null) {
            _insert.add(CISales.ConsignmentNote.DeparturePoint, departurePoint);
            _createdDoc.getValues().put(CISales.ConsignmentNote.DeparturePoint.name, departurePoint);
        }

        final String vehicleBrand = _parameter
                        .getParameterValue(CIFormSales.Sales_ConsignmentNoteForm.vehicleBrand.name);
        if (vehicleBrand != null) {
            _insert.add(CISales.ConsignmentNote.VehicleBrand, vehicleBrand);
            _createdDoc.getValues().put(CISales.ConsignmentNote.VehicleBrand.name, vehicleBrand);
        }

        final String vehicleDriverInfo = _parameter
                        .getParameterValue(CIFormSales.Sales_ConsignmentNoteForm.vehicleDriverInfo.name);
        if (vehicleDriverInfo != null) {
            _insert.add(CISales.ConsignmentNote.VehicleDriverInfo, vehicleDriverInfo);
            _createdDoc.getValues().put(CISales.ConsignmentNote.VehicleDriverInfo.name, vehicleDriverInfo);
        }

        final String vehicleLicencePlate = _parameter
                        .getParameterValue(CIFormSales.Sales_ConsignmentNoteForm.vehicleLicencePlate.name);
        if (vehicleLicencePlate != null) {
            _insert.add(CISales.ConsignmentNote.VehicleLicencePlate, vehicleLicencePlate);
            _createdDoc.getValues().put(CISales.ConsignmentNote.VehicleLicencePlate.name, vehicleLicencePlate);
        }

        final Instance carrierInst = Instance.get(_parameter
                        .getParameterValue(CIFormSales.Sales_ConsignmentNoteForm.carrierLink.name));
        if (carrierInst.isValid()) {
            _insert.add(CISales.ConsignmentNote.CarrierLink, carrierInst);
            _createdDoc.getValues().put(CISales.ConsignmentNote.CarrierLink.name, carrierInst);
        }

        final String transferReason = _parameter
                        .getParameterValue(CIFormSales.Sales_ConsignmentNoteForm.transferReason.name);
        if (transferReason != null && !transferReason.isEmpty()) {
            _insert.add(CISales.ConsignmentNote.TransferReason, transferReason);
            _createdDoc.getValues().put(CISales.ConsignmentNote.TransferReason.name, transferReason);
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
        return new DeliveryNote().autoComplete4Carrier(_parameter);
    }

}
