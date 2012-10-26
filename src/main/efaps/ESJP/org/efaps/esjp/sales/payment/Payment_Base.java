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

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractCommand;
import org.efaps.db.Insert;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 * 
 * @author The eFaps Team
 * @version $Id: Payment_Base.java 7671 2012-06-14 17:25:53Z
 *          jorge.cueva@moxter.net $
 */
@EFapsUUID("c7281e33-540f-4db1-bcc6-38e89528883f")
@EFapsRevision("$Rev$")
public abstract class Payment_Base
    extends CommonDocument
{
    public Return createPaymentPlanned(final Parameter _parameter)
        throws EFapsException
    {
        createDoc(_parameter);
        return new Return();
    }

    protected CreatedDoc createDoc(final Parameter _parameter)
        throws EFapsException
    {
        final AbstractCommand command = (AbstractCommand) _parameter.get(ParameterValues.UIOBJECT);
        final Insert insert = new Insert(command.getTargetCreateType());
        insert.add(CISales.PaymentDocumentAbstract.Name, _parameter.getParameterValue("name"));
        insert.add(CISales.PaymentDocumentAbstract.Note, _parameter.getParameterValue("description"));
        insert.add(CISales.PaymentDocumentAbstract.Amount, _parameter.getParameterValue("amount"));
        insert.add(CISales.PaymentDocumentAbstract.Date, _parameter.getParameterValue("date"));
        insert.add(CISales.PaymentDocumentAbstract.CurrencyLink, _parameter.getParameterValue("currencyLink"));
        insert.add(CISales.PaymentDocumentAbstract.StatusAbstract, Status.find(CISales.PaymentDocumentStatus.uuid, "Open").getId());
        insert.execute();
        final CreatedDoc createdDoc = new CreatedDoc(insert.getInstance());
        return createdDoc;
    }
}
