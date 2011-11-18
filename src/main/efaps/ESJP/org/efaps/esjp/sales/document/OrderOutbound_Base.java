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
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.SearchQuery;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.sales.Calculator;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("bd08a90e-91ce-4f03-b1bc-921a53b71948")
@EFapsRevision("$Rev$")
public abstract class OrderOutbound_Base
    extends DocumentSum
{

    public Return create(final Parameter _parameter) throws EFapsException
    {
        final String date = _parameter.getParameterValue("date");
        final List<Calculator> calcList = analyseTable(_parameter, null);
        final Long contactid = Instance.get(_parameter.getParameterValue("contact")).getId();

        final Instance baseCurrInst = SystemConfiguration.get(
                        UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f")).getLink("CurrencyBase");
        final Instance rateCurrInst = Instance.get(CIERP.Currency.getType(),
                        _parameter.getParameterValue("rateCurrencyId"));
        final Object[] rateObj = getRateObject(_parameter);
        final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                        BigDecimal.ROUND_HALF_UP);

        final Insert insert = new Insert(CISales.OrderOutbound);
        insert.add(CISales.OrderOutbound.Contact, contactid.toString());
        insert.add(CISales.OrderOutbound.CrossTotal, getCrossTotal(calcList).divide(rate, BigDecimal.ROUND_HALF_UP));
        insert.add(CISales.OrderOutbound.NetTotal, getNetTotal(calcList).divide(rate, BigDecimal.ROUND_HALF_UP));
        insert.add(CISales.OrderOutbound.DiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.OrderOutbound.RateCrossTotal, getCrossTotal(calcList));
        insert.add(CISales.OrderOutbound.RateNetTotal, getNetTotal(calcList));
        insert.add(CISales.OrderOutbound.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.OrderOutbound.Date, date);
        insert.add(CISales.OrderOutbound.Salesperson, _parameter.getParameterValue("salesperson"));
        insert.add(CISales.OrderOutbound.Name, _parameter.getParameterValue("name4create"));
        insert.add(CISales.OrderOutbound.Status, Status.find(CISales.OrderOutboundStatus.uuid, "Open").getId());
        insert.add(CISales.OrderOutbound.CurrencyId, baseCurrInst.getId());
        insert.add(CISales.OrderOutbound.RateCurrencyId, rateCurrInst.getId());
        insert.add(CISales.OrderOutbound.Rate, rateObj);
        insert.execute();
        Integer i = 1;
        for (final Calculator calc : calcList) {
            if (!calc.isEmpty()) {
                final Insert posIns = new Insert(CISales.OrderOutboundPosition);
                final Instance prodInst = Instance.get(_parameter.getParameterValues("product")[i - 1]);
                final BigDecimal quantity = calc.getQuantity();
                final UoM uom = Dimension.getUoM(Long.parseLong(_parameter.getParameterValues("uoM")[i - 1]));
                posIns.add(CISales.OrderOutboundPosition.Order, insert.getId());
                posIns.add(CISales.OrderOutboundPosition.PositionNumber, i.toString());
                posIns.add(CISales.OrderOutboundPosition.Product, prodInst.getId());
                posIns.add(CISales.OrderOutboundPosition.ProductDesc,
                                                _parameter.getParameterValues("productDesc")[i - 1]);
                posIns.add(CISales.OrderOutboundPosition.Quantity, quantity);
                posIns.add(CISales.OrderOutboundPosition.UoM, uom.getId());
                posIns.add(CISales.OrderOutboundPosition.CrossUnitPrice,
                                                calc.getCrossUnitPrice().divide(rate, BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.OrderOutboundPosition.NetUnitPrice,
                                                calc.getNetUnitPrice().divide(rate, BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.OrderOutboundPosition.CrossPrice,
                                                calc.getCrossPrice().divide(rate, BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.OrderOutboundPosition.DiscountNetUnitPrice,
                                                calc.getDiscountNetUnitPrice().divide(rate, BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.OrderOutboundPosition.NetPrice,
                                                calc.getNetPrice().divide(rate, BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.OrderOutboundPosition.Tax, (calc.getTaxId()).toString());
                posIns.add(CISales.OrderOutboundPosition.Discount, calc.getDiscountStr());
                posIns.add(CISales.OrderOutboundPosition.CurrencyId, baseCurrInst.getId());
                posIns.add(CISales.OrderOutboundPosition.RateCurrencyId, rateCurrInst.getId());
                posIns.add(CISales.OrderOutboundPosition.Rate, rateObj);
                posIns.execute();
            }
            i++;
        }
        return new Return();
    }

    /**
     * Method to get the javascript.
     * @param _parameter    Parameter as passed from the eFaps API
     * @return javascript
     * @throws EFapsException on error
     */
    @Override
    protected String getJavaScript(final Parameter _parameter)
        throws EFapsException
    {
        return getJavaScript(_parameter, false);
    }



    protected BigDecimal getPrice(final String _oid) throws EFapsException
    {
        final DateTime now = new DateTime();
        final SearchQuery query = new SearchQuery();
        BigDecimal ret = null;
        query.setExpand(_oid, "Products_ProductPricelistPurchase\\Product");
        query.addWhereExprLessValue("ValidFrom", now);
        query.addWhereExprGreaterValue("ValidUntil", now);
        query.addSelect("OID");
        query.execute();
        if (query.next()) {
            final String oid = (String) query.get("OID");
            final SearchQuery query2 = new SearchQuery();
            query2.setExpand(oid, "Products_ProductPricelistPosition\\ProductPricelist");
            query2.addSelect("Price");
            query2.execute();
            if (query2.next()) {
                ret = (BigDecimal) query2.get("Price");
            }
        }
        return ret;
    }
}
