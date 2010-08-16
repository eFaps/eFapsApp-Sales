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
 * @version $Id$
 */
@EFapsUUID("43417000-af54-4cb5-a266-4e6df2ed793e")
@EFapsRevision("$Rev$")
public abstract class Invoice_Base
    extends DocumentSum
{

    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String date = _parameter.getParameterValue("date");
        final List<Calculator> calcList = analyseTable(_parameter, null);
        final Long contactid = Instance.get(_parameter.getParameterValue("contact")).getId();
        final String name = _parameter.getParameterValue("name4create");
        // Sales-Configuration
        final Instance baseCurrInst = SystemConfiguration.get(
                        UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f")).getLink("CurrencyBase");
        final Instance rateCurrInst = Instance.get(CIERP.Currency.getType(),
                        _parameter.getParameterValue("rateCurrencyId"));

        final Object[] rateObj = getRateObject(_parameter);
        final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                        BigDecimal.ROUND_HALF_UP);

        final Insert insert = new Insert(CISales.Invoice);
        insert.add(CISales.Invoice.RateCrossTotal, getCrossTotal(calcList));
        insert.add(CISales.Invoice.RateNetTotal, getNetTotal(calcList));
        insert.add(CISales.Invoice.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.Invoice.CrossTotal, getCrossTotal(calcList).divide(rate, BigDecimal.ROUND_HALF_UP));
        insert.add(CISales.Invoice.NetTotal, getNetTotal(calcList).divide(rate, BigDecimal.ROUND_HALF_UP));
        insert.add(CISales.Invoice.DiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.Invoice.Contact, contactid);
        insert.add(CISales.Invoice.Date, date == null ? DateTimeUtil.normalize(new DateMidnight().toDateTime()) : date);
        insert.add(CISales.Invoice.Salesperson, _parameter.getParameterValue("salesperson"));
        insert.add(CISales.Invoice.Name, name);
        insert.add(CISales.Invoice.Status, ((Long) Status.find("Sales_InvoiceStatus", "Open").getId()).toString());
        insert.add(CISales.Invoice.Note, _parameter.getParameterValue("note"));
        insert.add(CISales.Invoice.DueDate, _parameter.getParameterValue("dueDate"));
        insert.add(CISales.Invoice.CurrencyId, baseCurrInst.getId());
        insert.add(CISales.Invoice.RateCurrencyId, rateCurrInst.getId());
        insert.add(CISales.Invoice.Rate, rateObj);
        insert.execute();
        insert.getInstance();
        Integer i = 1;
        for (final Calculator calc : calcList) {
            final Insert posIns = new Insert(CISales.InvoicePosition);
            final Long productdId = Instance.get(_parameter.getParameterValues("product")[i - 1]).getId();
            posIns.add(CISales.InvoicePosition.Invoice, insert.getId());
            posIns.add(CISales.InvoicePosition.PositionNumber, i);
            posIns.add(CISales.InvoicePosition.Product, productdId);
            posIns.add(CISales.InvoicePosition.ProductDesc, _parameter.getParameterValues("productDesc")[i - 1]);
            posIns.add(CISales.InvoicePosition.Quantity, calc.getQuantity());
            posIns.add(CISales.InvoicePosition.UoM, _parameter.getParameterValues("uoM")[i - 1]);
            posIns.add(CISales.InvoicePosition.Tax, calc.getTaxId());
            posIns.add(CISales.InvoicePosition.Discount, calc.getDiscount());
            posIns.add(CISales.InvoicePosition.CurrencyId, rateCurrInst.getId());
            posIns.add(CISales.InvoicePosition.Rate, rateObj);
            posIns.add(CISales.InvoicePosition.RateCurrencyId, rateCurrInst.getId());
            posIns.add(CISales.InvoicePosition.CrossUnitPrice,
                                                calc.getCrossUnitPrice().divide(rate, BigDecimal.ROUND_HALF_UP));
            posIns.add(CISales.InvoicePosition.NetUnitPrice,
                                                calc.getNetUnitPrice().divide(rate, BigDecimal.ROUND_HALF_UP));
            posIns.add(CISales.InvoicePosition.CrossPrice, calc.getCrossPrice().divide(rate, BigDecimal.ROUND_HALF_UP));
            posIns.add(CISales.InvoicePosition.NetPrice, calc.getNetPrice().divide(rate, BigDecimal.ROUND_HALF_UP));
            posIns.add(CISales.InvoicePosition.DiscountNetUnitPrice, calc.getDiscountNetUnitPrice().divide(rate,
                                                                                    BigDecimal.ROUND_HALF_UP));
            posIns.execute();
            i++;
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getJavaScript(final Parameter _parameter)
        throws EFapsException
    {
        return getJavaScript(_parameter, false);
    }
}
