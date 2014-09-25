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

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.QueryBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("f270918a-0c1c-4b83-b466-855412563b9d")
@EFapsRevision("$Rev$")
public abstract class CheckBook_Base
    extends CommonDocument
{

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return Return containing the instance
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Create create = new Create();
        return create.execute(_parameter);
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return empty Return
     * @throws EFapsException on error
     */
    public Return createRelations(final Parameter _parameter)
        throws EFapsException
    {
        final String startStr = _parameter.getParameterValue(CIFormSales.Sales_CheckBookCreateRelationForm.start.name);
        final String endStr = _parameter.getParameterValue(CIFormSales.Sales_CheckBookCreateRelationForm.end.name);

        final int start = Integer.parseInt(startStr);
        final int end = Integer.parseInt(endStr);
        for (int i = start; i < end; i++) {
            final Insert insert = new Insert(CISales.CheckBook2PaymentCheckOut);
            insert.add(CISales.CheckBook2PaymentCheckOut.Number, String.format("%06d", i));
            insert.add(CISales.CheckBook2PaymentCheckOut.FromLink, _parameter.getCallInstance());
            insert.add(CISales.CheckBook2PaymentCheckOut.ToLink, _parameter.getCallInstance());
            insert.execute();
        }
        return new Return();
    }

    public Return setDeactivated(final Parameter _parameter)
        throws EFapsException
    {
        final String[] others = (String[]) _parameter.get(ParameterValues.OTHERS);

        if (others != null) {
            for (final String other : others) {
                final Instance otherInst = Instance.get(other);
                if (otherInst.isValid()) {
                    if (containsProperty(_parameter, "Value")) {
                        final Update update = new Update(otherInst);
                        update.add(CISales.CheckBook2PaymentCheckOut.Value, getProperty(_parameter, "Value"));
                        update.executeWithoutTrigger();
                    }
                }
            }
        }
        return new Return();
    }

    public Return getMultiPrint(final Parameter _parameter)
        throws EFapsException
    {
        final MultiPrint multi = new MultiPrint()
        {

            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
                throws EFapsException
            {
                _queryBldr.addWhereAttrNotEqValue(CISales.CheckBook2PaymentCheckOut.ToLink, _parameter.getInstance());
            }
        };

        return multi.execute(_parameter);
    }
}


