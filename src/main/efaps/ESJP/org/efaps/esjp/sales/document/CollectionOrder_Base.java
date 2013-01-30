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
 * Revision:        $Rev: 8180 $
 * Last Changed:    $Date: 2012-11-08 19:00:37 -0500 (jue, 08 nov 2012) $
 * Last Changed By: $Author: m.aranya@moxter.net $
 */

package org.efaps.esjp.sales.document;

import java.text.NumberFormat;
import java.util.Map;
import java.util.UUID;

import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 * 
 * @author The eFaps Team
 * @version $Id: Payment_Base.java 7671 2012-06-14 17:25:53Z
 *          jorge.cueva@moxter.net $
 */
@EFapsUUID("771297ac-8d56-42f6-a1ed-fa573b42698e")
@EFapsRevision("$Rev: 8180 $")
public abstract class CollectionOrder_Base
    extends AbstractDocument
{

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */

    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        createDoc(_parameter);
        return new Return();
    }

    public Instance createDoc(final Parameter _parameter)
        throws EFapsException
    {
        final String date = _parameter.getParameterValue("date");
        final Long contactid = Instance.get(_parameter.getParameterValue("contact")).getId();
        final Insert insert = new Insert(CISales.CollectionOrder);
        insert.add(CISales.CollectionOrder.Contact, contactid.toString());
        insert.add(CISales.CollectionOrder.Date, date);
        insert.add(CISales.CollectionOrder.Salesperson, _parameter.getParameterValue("salesperson"));
        insert.add(CISales.CollectionOrder.Name, getName4Create(_parameter));
        insert.add(CISales.CollectionOrder.Note, _parameter.getParameterValue("note"));
        insert.add(CISales.CollectionOrder.Status,
                        ((Long) Status.find(CISales.CollectionOrderStatus.uuid, "Open")
                                        .getId()).toString());
        insert.execute();

        return insert.getInstance();
    }

    protected String getName4Create(final Parameter _parameter)
        throws EFapsException
    {
        // CollectionOrder secuencie
        return NumberGenerator.get(UUID.fromString("e89316af-42c6-4df1-ae7e-7c9f9c2bb73c")).getNextVal();
    }

    public Return getNameFieldValueUIT(final Parameter _parameter)
        throws EFapsException
    {
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String type = (String) properties.get("Type");
        final String includeChildTypes = (String) properties.get("IncludeChildTypes");

        String number = getMaxNumber(Type.get(type), !"false".equalsIgnoreCase(includeChildTypes));
        System.out.print(number);

        if (number == null) {
            number = "0001";
        } else {
            Integer num = Integer.parseInt(number) + 1;
            System.out.print("  " + num);
            final int lengthn = number.length();
            final NumberFormat nff = NumberFormat.getInstance();
            nff.setMinimumIntegerDigits(lengthn);
            nff.setMaximumIntegerDigits(lengthn);
            nff.setGroupingUsed(false);
            number = nff.format(num);
            System.out.print("  " + number);
        }
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, number);
        return retVal;
    }

}
