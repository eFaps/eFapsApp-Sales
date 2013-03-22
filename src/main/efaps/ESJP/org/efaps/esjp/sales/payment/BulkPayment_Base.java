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


package org.efaps.esjp.sales.payment;

import java.math.BigDecimal;
import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.sales.document.AbstractDocument;
import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("929914ed-1511-4fb8-9cc8-7514811d3f74")
@EFapsRevision("$Rev$")
public abstract class BulkPayment_Base
    extends AbstractDocument
{

    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        // Sales-Configuration
        final Instance baseInst = SystemConfiguration.get(UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f"))
                        .getLink("CurrencyBase");
        final Insert insert = new Insert(CISales.BulkPayment);
        insert.add(CISales.BulkPayment.Date, _parameter.getParameterValue(CIFormSales.Sales_BulkPaymentForm.date.name));
        insert.add(CISales.BulkPayment.Name, _parameter.getParameterValue(CIFormSales.Sales_BulkPaymentForm.name.name));
        insert.add(CISales.BulkPayment.CrossTotal, BigDecimal.ZERO);
        insert.add(CISales.BulkPayment.CrossTotal, BigDecimal.ZERO);
        insert.add(CISales.BulkPayment.NetTotal, BigDecimal.ZERO);
        insert.add(CISales.BulkPayment.DiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.BulkPayment.RateCrossTotal, BigDecimal.ZERO);
        insert.add(CISales.BulkPayment.RateNetTotal, BigDecimal.ZERO);
        insert.add(CISales.BulkPayment.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.BulkPayment.CurrencyId, baseInst.getId());
        insert.add(CISales.BulkPayment.RateCurrencyId, baseInst.getId());
        insert.add(CISales.BulkPayment.Rate, new Object[] { 1, 1 });
        insert.add(CISales.BulkPayment.Status, Status.find(CISales.BulkPaymentStatus.uuid, "Open").getId());
        insert.execute();

        final Insert relinsert = new Insert(CISales.BulkPayment2Account);
        relinsert.add(CISales.BulkPayment2Account.FromLink, insert.getId());
        relinsert.add(CISales.BulkPayment2Account.ToLink,
                        _parameter.getParameterValue(CIFormSales.Sales_BulkPaymentForm.account4create.name));
        relinsert.execute();

        return new Return();
    }

}
