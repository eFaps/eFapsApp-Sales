/*
 * Copyright 2003 - 2013 The eFaps Team
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("fefeba99-3218-4d05-b03b-db0b02445d38")
@EFapsRevision("$Rev$")
public abstract class Credit_Base
    extends AbstractSumOnlyDocument
{
    /**
     * Method for create a new Credit.
     *
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

    @Override
    protected void add2DocCreate(final Parameter _parameter,
                                 final Insert _insert,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final Instance empInst = Instance.get(_parameter.getParameterValue(CIFormSales.Sales_CreditForm.employee.name));
        if (empInst.isValid()) {
            _insert.add(CISales.Credit.EmployeeLink, empInst);
            _createdDoc.getValues().put(CISales.Credit.EmployeeLink.name, empInst);
        }
    }

    /**
     * Method for update field contact or employee.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return updateFields4Credit(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final StringBuilder js = new StringBuilder();
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> values = new TreeMap<String, String>();
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String val = (String) properties.get("ValidationType");
        if (val != null && val.equals("Employee")) {
            js.append(getSetFieldValue(0, "contactAutoComplete", ""))
                            .append(getSetFieldValue(0, "contact", ""))
                            .append(getSetFieldValue(0, "contactData", ""));
        } else if (val != null && val.equals("Contact")) {
            js.append(getSetFieldValue(0, "employee", ""))
                            .append(getSetFieldValue(0, "employeeAutoComplete", ""));
        }
        values.put(EFapsKey.FIELDUPDATE_JAVASCRIPT.getKey(), js.toString());
        list.add(values);
        ret.put(ReturnValues.VALUES, list);
        return ret;
    }

}
