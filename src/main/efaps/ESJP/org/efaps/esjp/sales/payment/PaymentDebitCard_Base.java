/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
 */
package org.efaps.esjp.sales.payment;

import java.io.File;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 *
 */
@EFapsUUID("16e87ae6-0b51-49ec-a40b-13ee55838a55")
@EFapsApplication("eFapsApp-Sales")
public abstract class PaymentDebitCard_Base
    extends AbstractPaymentIn
{

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = createDoc(_parameter);
        createPayment(_parameter, createdDoc);
        executeAutomation(_parameter, createdDoc);
        final Return ret = new Return();
        final File file = createReport(_parameter, createdDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    @Override
    protected Status getStatus4Create()
        throws EFapsException
    {
        Status ret = super.getStatus4Create();
        if (Sales.PAYMENT_DEBITCARD_CREATESTATUS.exists()) {
            ret = Status.find(CISales.PaymentDebitCardAbstractStatus, Sales.PAYMENT_DEBITCARD_CREATESTATUS.get());
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return edit4Draft(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final EditedDoc editedDoc = editDoc(_parameter);
        updatePayment(_parameter, editedDoc);
        final File file = createReport(_parameter, editedDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    @Override
    public CIType getCIType()
        throws EFapsException
    {
        return CISales.PaymentDebitCardAbstract;
    }
}
