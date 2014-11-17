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

import java.io.File;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("ea0b0c6e-2be8-4c78-b9a4-91e3691bf2b9")
@EFapsRevision("$Rev$")
public abstract class PaymentExchange_Base
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
        connectPaymentDocument2Document(_parameter, createdDoc);
        final Return ret = new Return();
        final File file = createReport(_parameter, createdDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }

        return ret;
    }

    @Override
    protected String getDocName4Create(final Parameter _parameter)
        throws EFapsException
    {
        final String name;
        final Instance instance = Instance.get(_parameter.getParameterValue("name"));

        if (instance.isValid()) {
            final PrintQuery print = new PrintQuery(instance);
            print.addAttribute(CISales.DocumentAbstract.Name);
            print.execute();

            name = print.<String>getAttribute(CISales.DocumentAbstract.Name);
        } else {
            name = super.getDocName4Create(_parameter);
        }

        return name;
    }

    @Override
    protected void connectPaymentDocument2Document(final Parameter _parameter,
                                                   final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final Instance instDoc = Instance.get(_parameter
                        .getParameterValue(CIFormSales.Sales_PaymentExchangeForm.name.name));
        if (instDoc.isValid() && _createdDoc.getInstance().isValid()) {
            final Insert insert = new Insert(CISales.PaymentExchange2Exchange);
            insert.add(CISales.PaymentExchange2Exchange.FromLink, _createdDoc.getInstance());
            insert.add(CISales.PaymentExchange2Exchange.ToLink, instDoc);
            insert.execute();

            _createdDoc.getValues().put("connectDocument", insert.getInstance());
        }
    }
}
