/*
 * Copyright 2003 - 2015 The eFaps Team
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
import java.util.List;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.StandartReport_Base.JasperActivation;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("f0c3b423-b48a-4eef-9c75-891330544b40")
@EFapsApplication("eFapsApp-Sales")
public abstract class Reminder_Base
    extends AbstractDocumentSum
{
    /**
     * Method for create a new Credit Note.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final CreatedDoc createdDoc = createDoc(_parameter);
        createPositions(_parameter, createdDoc);
        connect2Derived(_parameter, createdDoc);
        connect2Object(_parameter, createdDoc);

        if (Sales.REMINDER_JASPERACTIVATION.get().contains(JasperActivation.ONCREATE)) {
            final File file = createReport(_parameter, createdDoc);
            if (file != null) {
                ret.put(ReturnValues.VALUES, file);
                ret.put(ReturnValues.TRUE, true);
            }
        }
        ret.put(ReturnValues.INSTANCE, createdDoc.getInstance());
        return ret;
    }

    @Override
    protected List<Instance> connect2Derived(final Parameter _parameter,
                                             final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final List<Instance> ret = super.connect2Derived(_parameter, _createdDoc);
        final String[] deriveds = _parameter.getParameterValues("derived");

        ParameterUtil.setParameterValues(_parameter, "invoices", deriveds);
        return ret;
    }

    @Override
    protected void add2DocCreate(final Parameter _parameter, final Insert _insert, final CreatedDoc _createdDoc)
        throws EFapsException
    {
        super.add2DocCreate(_parameter, _insert, _createdDoc);
        _insert.add(CISales.Reminder.ReminderReason, _parameter.getParameterValue(
                        CIFormSales.Sales_ReminderForm.reminderReason.name));
    }
    /**
     * Edit.
     *
     * @param _parameter Parameter from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final EditedDoc editDoc = editDoc(_parameter);
        updatePositions(_parameter, editDoc);

        if (Sales.REMINDER_JASPERACTIVATION.get().contains(JasperActivation.ONEDIT)) {
            final File file = createReport(_parameter, editDoc);
            if (file != null) {
                ret.put(ReturnValues.VALUES, file);
                ret.put(ReturnValues.TRUE, true);
            }
        }
        return ret;
    }

    @Override
    protected void add2DocEdit(final Parameter _parameter, final Update _update, final EditedDoc _editDoc)
        throws EFapsException
    {
        super.add2DocEdit(_parameter, _update, _editDoc);
        _update.add(CISales.Reminder.ReminderReason, _parameter.getParameterValue(
                        CIFormSales.Sales_ReminderForm.reminderReason.name));
    }

    @Override
    public CIType getCIType()
        throws EFapsException
    {
        return CISales.Reminder;
    }
}
