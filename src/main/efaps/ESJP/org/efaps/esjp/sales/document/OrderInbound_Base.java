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
import org.efaps.db.AttributeQuery;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.esjp.sales.Calculator;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("77aebe5f-aebd-4662-b503-efdc6e46d9d3")
@EFapsRevision("$Rev$")
public abstract class OrderInbound_Base
    extends DocumentSum
{

    /**
     * Method for create a new Incoming Order.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        createDoc(_parameter);
        return new Return();
    }

    /**
     * Internal Method to create the document and the position.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Instance of CreatedDoc.
     * @throws EFapsException on error.
     */
    protected CreatedDoc createDoc(final Parameter _parameter)
        throws EFapsException
    {
        final String date = _parameter.getParameterValue("date");
        final List<Calculator> calcList = analyseTable(_parameter, null);
        final Long contactid = Instance.get(_parameter.getParameterValue("contact")).getId();
        // Sales-Configuration
        final Instance baseCurrInst = SystemConfiguration.get(
                        UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f")).getLink("CurrencyBase");
        final Instance rateCurrInst = Instance.get(CIERP.Currency.getType(),
                        _parameter.getParameterValue("rateCurrencyId"));
        final Object[] rateObj = getRateObject(_parameter);
        final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                        BigDecimal.ROUND_HALF_UP);

        final Insert insert = new Insert(CISales.OrderInbound);
        insert.add(CISales.OrderInbound.Contact, contactid);
        insert.add(CISales.OrderInbound.CrossTotal, getCrossTotal(calcList).divide(rate, BigDecimal.ROUND_HALF_UP));
        insert.add(CISales.OrderInbound.NetTotal, getNetTotal(calcList).divide(rate, BigDecimal.ROUND_HALF_UP));
        insert.add(CISales.OrderInbound.DiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.OrderInbound.RateNetTotal, getNetTotal(calcList));
        insert.add(CISales.OrderInbound.RateCrossTotal, getCrossTotal(calcList));
        insert.add(CISales.OrderInbound.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.OrderInbound.Date, date);
        insert.add(CISales.OrderInbound.Salesperson, _parameter.getParameterValue("salesperson"));
        insert.add(CISales.OrderInbound.Name, _parameter.getParameterValue("name4create"));
        insert.add(CISales.OrderInbound.Status, Status.find(CISales.OrderInboundStatus.uuid, "Open").getId());
        insert.add(CISales.OrderInbound.CurrencyId, baseCurrInst.getId());
        insert.add(CISales.OrderInbound.RateCurrencyId, rateCurrInst.getId());
        insert.add(CISales.OrderInbound.Rate, rateObj);
        insert.add(CISales.OrderInbound.Note, _parameter.getParameterValue("note"));
        insert.execute();

        final CreatedDoc createdDoc = new CreatedDoc(insert.getInstance());
        createPositions(_parameter, calcList, createdDoc);
        return createdDoc;
    }

    /**
     * Internal Method to create the positions for this Document.
     * @param _parameter    Parameter as passed from eFaps API.
     * @param _calcList     List of Calculators
     * @param _createdDoc   cretaed Document
     * @throws EFapsException on error
     */
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
                final Insert posIns = new Insert(CISales.OrderInboundPosition);
                final Long productdId = Instance.get(_parameter.getParameterValues("product")[i]).getId();
                posIns.add(CISales.OrderInboundPosition.Order, _createdDoc.getInstance().getId());
                posIns.add(CISales.OrderInboundPosition.PositionNumber, i);
                posIns.add(CISales.OrderInboundPosition.Product, productdId.toString());
                posIns.add(CISales.OrderInboundPosition.ProductDesc,
                                _parameter.getParameterValues("productDesc")[i]);
                posIns.add(CISales.OrderInboundPosition.Quantity, calc.getQuantityStr());
                posIns.add(CISales.OrderInboundPosition.UoM, _parameter.getParameterValues("uoM")[i]);
                posIns.add(CISales.OrderInboundPosition.CrossUnitPrice, calc.getCrossUnitPrice()
                                                                            .divide(rate, BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.OrderInboundPosition.NetUnitPrice, calc.getNetUnitPrice()
                                                                            .divide(rate, BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.OrderInboundPosition.CrossPrice, calc.getCrossPrice()
                                                                            .divide(rate, BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.OrderInboundPosition.NetPrice, calc.getNetPrice()
                                                                            .divide(rate, BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.OrderInboundPosition.Tax, calc.getTaxId());
                posIns.add(CISales.OrderInboundPosition.Discount, calc.getDiscountStr());
                posIns.add(CISales.OrderInboundPosition.DiscountNetUnitPrice, calc.getDiscountNetUnitPrice()
                                                                            .divide(rate, BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.OrderInboundPosition.CurrencyId, baseCurrInst.getId());
                posIns.add(CISales.OrderInboundPosition.Rate, rateObj);
                posIns.add(CISales.OrderInboundPosition.RateCurrencyId, rateCurrInst.getId());
                posIns.execute();
                _createdDoc.addPosition(posIns.getInstance());
            }
            i++;
        }
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

    /**
     * Method to get the list of instances for a table.
     * @param _parameter parameter as passed from the efaps api
     * @return list of instances
     * @throws EFapsException on error
     */
    public Return getProductTable(final Parameter _parameter)
        throws EFapsException
    {
        final MultiPrint multi = new MultiPrint() {

            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final QueryBuilder atrrQueryBldr = new QueryBuilder(CISales.OrderInbound);
                atrrQueryBldr.addWhereAttrEqValue(CISales.OrderInbound.Status,
                                Status.find(CISales.OrderInboundStatus.uuid, "Open").getId());
                final AttributeQuery attrQuery = atrrQueryBldr.getAttributeQuery(CISales.OrderInbound.ID);
                _queryBldr.addWhereAttrInQuery(CISales.OrderInboundPosition.Order, attrQuery);
                super.add2QueryBldr(_parameter, _queryBldr);
            }
        };
        return multi.execute(_parameter);
    }
}
