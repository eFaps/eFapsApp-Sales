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

package org.efaps.esjp.sales.payment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: Payment_Base.java 7671 2012-06-14 17:25:53Z
 *          jorge.cueva@moxter.net $
 */
@EFapsUUID("39feb877-6310-4170-816d-173f89347e3d")
@EFapsRevision("$Rev$")
public abstract class PaymentCheck_Base
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
        final Return ret = createReportDoc(_parameter, createdDoc);
        return ret;
    }

    public Return returnDiffered(final Parameter _parameter)
        throws EFapsException
    {

        final Return ret = new Return();
        final Map<?, ?> properties = (HashMap<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String typeStr = (String) properties.get("Types");
        Type type;
        final List<Instance> instances = new ArrayList<Instance>();
        if (typeStr != null) {
            type = Type.get(typeStr);
            final QueryBuilder queryBldr = new QueryBuilder(type);
            MultiPrintQuery multi = queryBldr.getPrint();

            multi.addAttribute(CISales.PaymentDocumentAbstract.Date, CISales.PaymentDocumentAbstract.DueDate);
            multi.execute();
            while (multi.next()) {
                Instance inst = multi.getCurrentInstance();
                final DateTime date = multi.<DateTime>getAttribute(CISales.PaymentDocumentAbstract.Date);
                final DateTime dueDate = multi.<DateTime>getAttribute(CISales.PaymentDocumentAbstract.DueDate);
                if (!(date.compareTo(dueDate) == 0)) {
                    instances.add(inst);
                }
            }

        }
        ret.put(ReturnValues.VALUES, instances);
        return ret;

    }

}
