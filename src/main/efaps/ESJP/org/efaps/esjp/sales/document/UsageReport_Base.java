/*
 * Copyright 2003 - 2010 The eFaps Team
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

import java.math.BigDecimal;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
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
 * @version $Id$
 */
@EFapsUUID("f3252401-2007-4ff3-997d-5e5d1a7ab863")
@EFapsRevision("$Rev$")
public class UsageReport_Base
    extends AbstractDocument
{
    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter) throws EFapsException
    {
        createDoc(_parameter);
        return new Return();
    }

    /**Method to obtain the instance of a return slip.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return Instance of the document.
     * @throws EFapsException on error.
     */
    public Instance createDoc(final Parameter _parameter)
        throws EFapsException
    {
        final String date = _parameter.getParameterValue("date");
        final Long contactid = Instance.get(_parameter.getParameterValue("contact")).getId();
        final Insert insert = new Insert(CISales.UsageReport);
        insert.add(CISales.UsageReport.Contact, contactid.toString());
        insert.add(CISales.UsageReport.Date, date);
        insert.add(CISales.UsageReport.Salesperson, _parameter.getParameterValue("salesperson"));
        insert.add(CISales.UsageReport.Name, _parameter.getParameterValue("name4create"));
        insert.add(CISales.UsageReport.Status, ((Long) Status.find(CISales.UsageReportStatus.uuid, "Closed")
                                                                                                .getId()).toString());
        insert.execute();
        Integer i = 0;
        for (final String quantity : _parameter.getParameterValues("quantity")) {
            final Insert posIns = new Insert(CISales.UsageReportPosition);
            final Long productdId = Instance.get(_parameter.getParameterValues("product")[i]).getId();
            posIns.add(CISales.UsageReportPosition.UsageReportLink, insert.getId());
            posIns.add(CISales.UsageReportPosition.PositionNumber, i.toString());
            posIns.add(CISales.UsageReportPosition.Product, productdId.toString());
            posIns.add(CISales.UsageReportPosition.ProductDesc, _parameter.getParameterValues("productDesc")[i]);
            posIns.add(CISales.UsageReportPosition.Quantity, (new BigDecimal(quantity)).toString());
            posIns.add(CISales.UsageReportPosition.UoM, _parameter.getParameterValues("uoM")[i]);
            posIns.add(CISales.UsageReportPosition.CrossUnitPrice, 0);
            posIns.add(CISales.UsageReportPosition.NetUnitPrice, 0);
            posIns.add(CISales.UsageReportPosition.CrossPrice, 0);
            posIns.add(CISales.UsageReportPosition.NetPrice, 0);
            posIns.add(CISales.UsageReportPosition.Discount, 0);
            posIns.add(CISales.UsageReportPosition.DiscountNetUnitPrice, 0);
            posIns.add(CISales.UsageReportPosition.Tax, 1);
            posIns.add(CISales.UsageReportPosition.CurrencyId, 1);
            posIns.add(CISales.UsageReportPosition.RateCurrencyId, 1);
            posIns.add(CISales.UsageReportPosition.Rate, new Object[] { BigDecimal.ONE, BigDecimal.ONE });
            posIns.execute();
            i++;
        }
        return insert.getInstance();
    }

    /**
     * Trigger to insert a position of the usage report in the inventory.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return usageReportPositionInsertTrigger(final Parameter _parameter)
        throws EFapsException
    {
       //TODO
        return new Return();
    }
}
