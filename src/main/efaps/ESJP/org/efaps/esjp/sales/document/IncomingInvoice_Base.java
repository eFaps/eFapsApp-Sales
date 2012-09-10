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
import java.text.DecimalFormat;
import java.text.ParseException;
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
import org.efaps.esjp.sales.Calculator_Base;
import org.efaps.esjp.sales.Costs;
import org.efaps.util.DateTimeUtil;
import org.efaps.util.EFapsException;
import org.joda.time.DateMidnight;

/**
 * Base class for Type Incoming Invoice.
 * 
 * @author The eFaps Team
 * @version $Id: IncomingInvoice_Base.java 7921 2012-08-20 14:51:53Z
 *          m.aranya@moxter.net $
 */
@EFapsUUID("f7d75f38-5ac8-4bf4-9609-6226ac82fea3")
@EFapsRevision("$Rev$")
public abstract class IncomingInvoice_Base
    extends DocumentSum
{
    /**
     * Executed from a Command execute vent to create a new Incoming Invoice.
     * 
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Instance docInst = createDoc(_parameter);
        new Costs().updateCosts(_parameter, docInst);
        return new Return();
    }

    /**
     * Create a new Incoming Invoice.
     * 
     * @param _parameter Parameter as passed from the eFaps API
     * @return Instance of the created Document
     * @throws EFapsException on error
     */
    protected Instance createDoc(final Parameter _parameter)
        throws EFapsException
    {
        final String date = _parameter.getParameterValue("date");
        final List<Calculator> calcList = analyseTable(_parameter, null);
        final Long contactid = Instance.get(_parameter.getParameterValue("contact")).getId();
        final String name = _parameter.getParameterValue("name");

        // Sales-Configuration
        final Instance baseCurrInst = SystemConfiguration.get(
                        UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f")).getLink("CurrencyBase");
        final Instance rateCurrInst = Instance.get(CIERP.Currency.getType(),
                        _parameter.getParameterValue("rateCurrencyId"));

        final Object[] rateObj = getRateObject(_parameter);
        final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                        BigDecimal.ROUND_HALF_UP);

        final DecimalFormat formater = Calculator_Base.getFormatInstance();
        String strNetTotal = _parameter.getParameterValue("netTotal");
        String strCrossTotal = _parameter.getParameterValue("crossTotal");
        BigDecimal bigCrossTotal = BigDecimal.ZERO;
        BigDecimal bigNetTotal = BigDecimal.ZERO;

        try {
            bigCrossTotal = (BigDecimal) formater.parse(strCrossTotal);
            bigNetTotal = (BigDecimal) formater.parse(strNetTotal);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        BigDecimal calCrossTotal = getCrossTotal(calcList);
        BigDecimal calNetTotal = getNetTotal(calcList);

        BigDecimal crossTotal = bigCrossTotal
                        .compareTo(calCrossTotal) == 0 ? getCrossTotal(calcList) : bigCrossTotal;

        BigDecimal netTotal = bigNetTotal
                        .compareTo(calNetTotal) == 0 ? getNetTotal(calcList) : bigNetTotal;

        final Insert insert = new Insert(CISales.IncomingInvoice);
        insert.add(CISales.IncomingInvoice.RateCrossTotal, crossTotal.setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
        insert.add(CISales.IncomingInvoice.RateNetTotal, netTotal.setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
        insert.add(CISales.IncomingInvoice.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.IncomingInvoice.CrossTotal,
                        crossTotal.divide(rate, BigDecimal.ROUND_HALF_UP).setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
        insert.add(CISales.IncomingInvoice.NetTotal,
                        netTotal.divide(rate, BigDecimal.ROUND_HALF_UP).setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
        insert.add(CISales.IncomingInvoice.DiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.IncomingInvoice.Contact, contactid);
        insert.add(CISales.IncomingInvoice.Date, date == null
                        ? DateTimeUtil.normalize(new DateMidnight().toDateTime()) : date);
        insert.add(CISales.IncomingInvoice.Salesperson, _parameter.getParameterValue("salesperson"));
        insert.add(CISales.IncomingInvoice.Name, name);
        insert.add(CISales.IncomingInvoice.Status, Status.find(CISales.IncomingInvoiceStatus.uuid, "Open").getId());
        insert.add(CISales.IncomingInvoice.Note, _parameter.getParameterValue("note"));
        insert.add(CISales.IncomingInvoice.DueDate, _parameter.getParameterValue("dueDate"));
        insert.add(CISales.IncomingInvoice.CurrencyId, baseCurrInst.getId());
        insert.add(CISales.IncomingInvoice.RateCurrencyId, rateCurrInst.getId());
        insert.add(CISales.IncomingInvoice.Rate, rateObj);
        insert.execute();
        Integer i = 1;
        for (final Calculator calc : calcList) {
            final Insert posIns = new Insert(CISales.IncomingInvoicePosition);
            final Long productdId = Instance.get(_parameter.getParameterValues("product")[i - 1]).getId();
            posIns.add(CISales.IncomingInvoicePosition.IncomingInvoice, insert.getId());
            posIns.add(CISales.IncomingInvoicePosition.PositionNumber, i);
            posIns.add(CISales.IncomingInvoicePosition.Product, productdId);
            posIns.add(CISales.IncomingInvoicePosition.ProductDesc,
                            _parameter.getParameterValues("productDesc")[i - 1]);
            posIns.add(CISales.IncomingInvoicePosition.Quantity, calc.getQuantity());
            posIns.add(CISales.IncomingInvoicePosition.UoM, _parameter.getParameterValues("uoM")[i - 1]);
            posIns.add(CISales.IncomingInvoicePosition.Tax, calc.getTaxId());
            posIns.add(CISales.IncomingInvoicePosition.Discount, calc.getDiscount());
            posIns.add(CISales.IncomingInvoicePosition.CurrencyId, baseCurrInst.getId());
            posIns.add(CISales.IncomingInvoicePosition.Rate, rateObj);
            posIns.add(CISales.IncomingInvoicePosition.RateCurrencyId, rateCurrInst.getId());
            posIns.add(CISales.IncomingInvoicePosition.CrossUnitPrice,
                            calc.getCrossUnitPrice().divide(rate, BigDecimal.ROUND_HALF_UP)
                                            .setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
            posIns.add(CISales.IncomingInvoicePosition.NetUnitPrice,
                            calc.getNetUnitPrice().divide(rate, BigDecimal.ROUND_HALF_UP)
                                            .setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
            posIns.add(CISales.IncomingInvoicePosition.CrossPrice,
                            calc.getCrossPrice().divide(rate, BigDecimal.ROUND_HALF_UP)
                                            .setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
            posIns.add(CISales.IncomingInvoicePosition.NetPrice,
                            calc.getNetPrice().divide(rate, BigDecimal.ROUND_HALF_UP)
                                            .setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
            posIns.add(CISales.IncomingInvoicePosition.DiscountNetUnitPrice,
                            calc.getDiscountNetUnitPrice().divide(rate, BigDecimal.ROUND_HALF_UP)
                                            .setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
            posIns.add(CISales.IncomingInvoicePosition.RateNetUnitPrice,
                            calc.getNetUnitPrice().setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
            posIns.add(CISales.IncomingInvoicePosition.RateCrossUnitPrice,
                            calc.getCrossUnitPrice().setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
            posIns.add(CISales.IncomingInvoicePosition.RateDiscountNetUnitPrice,
                            calc.getDiscountNetUnitPrice().setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
            posIns.add(CISales.IncomingInvoicePosition.RateNetPrice,
                            calc.getNetPrice().setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
            posIns.add(CISales.IncomingInvoicePosition.RateCrossPrice,
                            calc.getCrossPrice().setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));

            posIns.execute();
            i++;
        }
        return insert.getInstance();
    }
}
