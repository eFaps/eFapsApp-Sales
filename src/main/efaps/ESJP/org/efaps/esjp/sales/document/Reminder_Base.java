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
import org.efaps.util.DateTimeUtil;
import org.efaps.util.EFapsException;
import org.joda.time.DateMidnight;

/**
 * TODO comment!
 * 
 * @author The eFaps Team
 * @version $Id: Reminder_Base.java 7568 2012-05-31 23:57:47Z
 *          m.aranya@moxter.net $
 */
@EFapsUUID("f0c3b423-b48a-4eef-9c75-891330544b40")
@EFapsRevision("$Rev$")
public abstract class Reminder_Base
    extends DocumentSum
{
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        createDoc(_parameter);
        return new Return();
    }

    protected CreatedDoc createDoc(final Parameter _parameter)
        throws EFapsException
    {
        final String date = _parameter.getParameterValue("date");
        final List<Calculator> calcList = analyseTable(_parameter, null);
        final Instance baseCurrInst = SystemConfiguration.get(
                        UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f")).getLink("CurrencyBase");
        final Instance rateCurrInst = Instance.get(CIERP.Currency.getType(), _parameter
                        .getParameterValue("rateCurrencyId"));
        final Object[] rateObj = getRateObject(_parameter);
        final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                        BigDecimal.ROUND_HALF_UP);
        final Long contactid = Instance.get(_parameter.getParameterValue("contact")).getId();
        final Insert insert = new Insert(CISales.Reminder);
        insert.add(CISales.Reminder.Contact, contactid);
        insert.add(CISales.Reminder.CrossTotal,
                        getCrossTotal(calcList).divide(rate, BigDecimal.ROUND_HALF_UP).setScale(isLongDecimal(_parameter),
                                        BigDecimal.ROUND_HALF_UP));
        insert.add(CISales.Reminder.NetTotal,
                        getNetTotal(calcList).divide(rate, BigDecimal.ROUND_HALF_UP).setScale(isLongDecimal(_parameter),
                                        BigDecimal.ROUND_HALF_UP));
        insert.add(CISales.Reminder.RateNetTotal, getNetTotal(calcList).setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
        insert.add(CISales.Reminder.RateCrossTotal, getCrossTotal(calcList).setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
        insert.add(CISales.Reminder.DiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.Reminder.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.Reminder.CurrencyId, baseCurrInst.getId());
        insert.add(CISales.Reminder.RateCurrencyId, rateCurrInst.getId());
        insert.add(CISales.Reminder.Rate, rateObj);
        insert.add(CISales.Reminder.Date, date == null ? DateTimeUtil.normalize(new DateMidnight().toDateTime()) : date);
        insert.add(CISales.Reminder.Note, _parameter.getParameterValue("note"));
        insert.add(CISales.Reminder.DueDate, _parameter.getParameterValue("dueDate"));
        insert.add(CISales.Reminder.Salesperson, _parameter.getParameterValue("salesperson"));
        insert.add(CISales.Reminder.Status,
                        ((Long) Status.find(CISales.ReminderStatus.uuid, "Open").getId()).toString());
        insert.add(CISales.Reminder.Name, getDocName4Create(_parameter));
        insert.execute();

        final CreatedDoc createdDoc = new CreatedDoc(insert.getInstance());
        createPositions(_parameter, calcList, createdDoc);
        return createdDoc;
    }

    @Override
    protected void createPositions(final Parameter _parameter,
                                   final List<Calculator> _calcList,
                                   final CreatedDoc _createdDoc)
        throws EFapsException
    {
        // Sales-Configuration
        final Instance baseCurrInst = SystemConfiguration.get(
                        UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f")).getLink("CurrencyBase");
        final Instance rateCurrInst = Instance.get(CIERP.Currency.getType(),
                        _parameter.getParameterValue("rateCurrencyId"));

        final Object[] rateObj = getRateObject(_parameter);
        final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                        BigDecimal.ROUND_HALF_UP);
        Integer i = 0;
        for (final Calculator calc : _calcList) {
            if (!calc.isEmpty()) {
                final Insert posIns = new Insert(CISales.ReminderPosition);
                final Long productdId = Instance.get(_parameter.getParameterValues("product")[i]).getId();
                posIns.add(CISales.ReminderPosition.Reminder, _createdDoc.getInstance().getId());
                posIns.add(CISales.ReminderPosition.PositionNumber, i);
                posIns.add(CISales.ReminderPosition.Product, productdId);
                posIns.add(CISales.ReminderPosition.ProductDesc, _parameter.getParameterValues("productDesc")[i]);
                posIns.add(CISales.ReminderPosition.Quantity, calc.getQuantity());
                posIns.add(CISales.ReminderPosition.UoM, _parameter.getParameterValues("uoM")[i]);
                posIns.add(CISales.ReminderPosition.Tax, calc.getTaxId());
                posIns.add(CISales.ReminderPosition.Discount, calc.getDiscount());
                posIns.add(CISales.ReminderPosition.CurrencyId, baseCurrInst.getId());
                posIns.add(CISales.ReminderPosition.Rate, rateObj);
                posIns.add(CISales.ReminderPosition.RateCurrencyId, rateCurrInst.getId());
                posIns.add(CISales.ReminderPosition.CrossUnitPrice,
                                calc.getCrossUnitPrice().divide(rate, BigDecimal.ROUND_HALF_UP)
                                                .setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.ReminderPosition.NetUnitPrice,
                                calc.getNetUnitPrice().divide(rate, BigDecimal.ROUND_HALF_UP)
                                                .setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.ReminderPosition.CrossPrice,
                                calc.getCrossPrice().divide(rate, BigDecimal.ROUND_HALF_UP)
                                                .setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.ReminderPosition.NetPrice,
                                calc.getNetPrice().divide(rate, BigDecimal.ROUND_HALF_UP)
                                                .setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.ReminderPosition.DiscountNetUnitPrice,
                                calc.getDiscountNetUnitPrice().divide(rate, BigDecimal.ROUND_HALF_UP)
                                                .setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.ReminderPosition.RateNetUnitPrice,
                                calc.getNetUnitPrice().setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.ReminderPosition.RateCrossUnitPrice,
                                calc.getCrossUnitPrice().setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.ReminderPosition.RateDiscountNetUnitPrice,
                                calc.getDiscountNetUnitPrice().setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.ReminderPosition.RateNetPrice,
                                calc.getNetPrice().setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.ReminderPosition.RateCrossPrice,
                                calc.getCrossPrice().setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));

                add2PositionInsert(_parameter, calc, posIns);
                posIns.execute();
                _createdDoc.addPosition(posIns.getInstance());
            }
            i++;
        }
    }
}
