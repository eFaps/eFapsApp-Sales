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
package org.efaps.esjp.sales.document;

import java.io.File;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.esjp.common.jasperreport.StandartReport_Base.JasperActivation;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;

/**
 *
 * @author The eFaps Team
 */
@EFapsUUID("09c62a59-c334-4e84-8ae5-96dfbc6f7463")
@EFapsApplication("eFapsApp-Sales")
public abstract class PaymentOrder_Base
    extends AbstractSumOnlyDocument
{
    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = createDoc(_parameter);
        connect2Object(_parameter, createdDoc);
        final Return ret = new Return();
        if (Sales.PAYMENTORDER_JASPERACTIVATION.get().contains(JasperActivation.ONCREATE)) {
            final File file = createReport(_parameter, createdDoc);
            if (file != null) {
                ret.put(ReturnValues.VALUES, file);
                ret.put(ReturnValues.TRUE, true);
            }
        }
        ret.put(ReturnValues.INSTANCE, createdDoc.getInstance());
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new return
     * @throws EFapsException on error
     */
    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final EditedDoc editedDoc = editDoc(_parameter);
        updateConnection2Object(_parameter, editedDoc);
        if (Sales.PAYMENTORDER_JASPERACTIVATION.get().contains(JasperActivation.ONEDIT)) {
            final File file = createReport(_parameter, editedDoc);
            if (file != null) {
                ret.put(ReturnValues.VALUES, file);
                ret.put(ReturnValues.TRUE, true);
            }
        }
        return ret;
    }

}
