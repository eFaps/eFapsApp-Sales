/*
 * Copyright 2003 - 2009 The eFaps Team
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
import java.util.List;
import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.sales.Calculator;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("f0c3b423-b48a-4eef-9c75-891330544b40")
@EFapsRevision("$Rev$")
public abstract class Reminder_Base
    extends DocumentSum
{

    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final String date = _parameter.getParameterValue("date");
        final List<Calculator> calcList = analyseTable(_parameter, null);
        final Long contactid = Instance.get(_parameter.getParameterValue("contact")).getId();
        final Object[] rateObj = getRateObject(_parameter);
        final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                        BigDecimal.ROUND_HALF_UP);
        final Instance baseCurrInst = SystemConfiguration.get(
                        UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f")).getLink("CurrencyBase");
        final Instance rateCurrInst = Instance.get(CIERP.Currency.getType(),
                        _parameter.getParameterValue("rateCurrencyId"));

        final Insert insert = new Insert(CISales.Reminder);
        insert.add(CISales.Reminder.Contact, contactid.toString());
        insert.add(CISales.Reminder.CrossTotal, getCrossTotalStr(calcList));
        insert.add(CISales.Reminder.NetTotal, getNetTotalStr(calcList));
        insert.add(CISales.Reminder.Date, date);
        insert.add(CISales.Reminder.Salesperson, _parameter.getParameterValue("salesperson"));
        insert.add(CISales.Reminder.DiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.Reminder.CurrencyId, baseCurrInst.getId());
        insert.add(CISales.Reminder.RateCurrencyId, rateCurrInst.getId());
        insert.add(CISales.Reminder.RateCrossTotal, getCrossTotal(calcList));
        insert.add(CISales.Reminder.RateNetTotal, getNetTotal(calcList));
        insert.add(CISales.Reminder.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.Reminder.Rate, rateObj);
        insert.add(CISales.Reminder.Status,
                                ((Long) Status.find(CISales.ReminderStatus.uuid, "Open").getId()).toString());
        insert.add(CISales.Reminder.Note, _parameter.getParameterValue("note"));
        insert.add(CISales.Reminder.DueDate, _parameter.getParameterValue("dueDate"));
        insert.add(CISales.Reminder.Name, _parameter.getParameterValue("name4create"));
        insert.execute();
        Integer i = 0;
        for (final Calculator calc : calcList) {
            final Insert posIns = new Insert(CISales.ReminderPosition);
            final Long productdId = Instance.get(_parameter.getParameterValues("product")[i]).getId();
            posIns.add(CISales.ReminderPosition.Reminder, insert.getId());
            posIns.add(CISales.ReminderPosition.PositionNumber, i.toString());
            posIns.add(CISales.ReminderPosition.Product, productdId.toString());
            posIns.add(CISales.ReminderPosition.ProductDesc, _parameter.getParameterValues("productDesc")[i]);
            posIns.add(CISales.ReminderPosition.Quantity, calc.getQuantityStr());
            posIns.add(CISales.ReminderPosition.UoM, _parameter.getParameterValues("uoM")[i]);
            posIns.add(CISales.ReminderPosition.CrossUnitPrice, calc.getCrossUnitPriceStr());
            posIns.add(CISales.ReminderPosition.NetUnitPrice, calc.getNetUnitPriceStr());
            posIns.add(CISales.ReminderPosition.CrossPrice, calc.getCrossPriceStr());
            posIns.add(CISales.ReminderPosition.NetPrice, calc.getNetPriceStr());
            posIns.add(CISales.ReminderPosition.Tax, (calc.getTaxId()).toString());
            posIns.add(CISales.ReminderPosition.Discount, calc.getDiscountStr());
            posIns.add(CISales.ReminderPosition.CurrencyId, baseCurrInst.getId());
            posIns.add(CISales.ReminderPosition.RateCurrencyId, rateCurrInst.getId());
            posIns.add(CISales.ReminderPosition.Rate, rateObj);
            posIns.add(CISales.ReminderPosition.DiscountNetUnitPrice, calc.getDiscountNetUnitPrice().divide(rate,
                            BigDecimal.ROUND_HALF_UP));

            posIns.execute();
            i++;
        }
        return new Return();
    }
}
