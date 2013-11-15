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
 * Revision:        $Rev: 10815 $
 * Last Changed:    $Date: 2013-11-07 22:05:35 -0500 (jue, 07 nov 2013) $
 * Last Changed By: $Author: jan@moxter.net $
 */


package org.efaps.esjp.sales.document;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.CommonDocument_Base.CreatedDoc;
import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: Exchange_Base.java 10815 2013-11-08 03:05:35Z jan@moxter.net $
 */
@EFapsUUID("fefeba99-3218-4d05-b03b-db0b02445d38")
@EFapsRevision("$Rev: 10815 $")
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
    
    protected void add2DocCreate(final Parameter _parameter,
                                 final Insert _insert,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final String employee = _parameter.getParameterValue(CIFormSales.Sales_CreditForm.number.name);
        if (employee != null) {
            final Instance inst = Instance.get(employee); 
            _insert.add(CISales.Credit.EmployeeLink, inst.getId());
            _createdDoc.getValues().put(CISales.Credit.EmployeeLink.name, inst);
        }
    }
}