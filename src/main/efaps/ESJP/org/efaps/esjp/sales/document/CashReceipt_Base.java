/*
 * Copyright 2003 - 2011 The eFaps Team
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
 * Revision:        $Rev: 6430 $
 * Last Changed:    $Date: 2011-04-19 13:44:28 -0500 (mar, 19 abr 2011) $
 * Last Changed By: $Author: jorge.cueva@moxter.net $
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
 * @version $Id: CashReceipt_Base.java 6430 2011-04-19 18:44:28Z jorge.cueva@moxter.net $
 */
@EFapsUUID("f084c0b4-dcb0-4ae4-b84e-1841f0ea13ca")
@EFapsRevision("$Rev: 1$")
public abstract class CashReceipt_Base
    extends DocumentSum
{
    /**
     * Method for create a new CashReceipt.
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
        final String name = _parameter.getParameterValue("name4create");
        // Sales-Configuration
        final Instance baseCurrInst = SystemConfiguration.get(
                        UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f")).getLink("CurrencyBase");
        final Instance rateCurrInst = Instance.get(CIERP.Currency.getType(),
                        _parameter.getParameterValue("rateCurrencyId"));

        final Object[] rateObj = getRateObject(_parameter);
        final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                        BigDecimal.ROUND_HALF_UP);

        final Insert insert = new Insert(CISales.CashReceipt);
        insert.add(CISales.CashReceipt.RateCrossTotal, getCrossTotal(calcList));
        insert.add(CISales.CashReceipt.RateNetTotal, getNetTotal(calcList));
        insert.add(CISales.CashReceipt.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.CashReceipt.CrossTotal,
                        getCrossTotal(calcList).divide(rate, BigDecimal.ROUND_HALF_UP));
        insert.add(CISales.CashReceipt.NetTotal, getNetTotal(calcList).divide(rate, BigDecimal.ROUND_HALF_UP));
        insert.add(CISales.CashReceipt.DiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.CashReceipt.Contact, contactid);
        insert.add(CISales.CashReceipt.Date, date == null
                        ? DateTimeUtil.normalize(new DateMidnight().toDateTime()) : date);
        insert.add(CISales.CashReceipt.Salesperson, _parameter.getParameterValue("salesperson"));
        insert.add(CISales.CashReceipt.Name, name);
        insert.add(CISales.CashReceipt.Status, ((Long) Status.find("Sales_CashReceiptStatus", "Open").getId()).toString());
        insert.add(CISales.CashReceipt.Note, _parameter.getParameterValue("note"));
        insert.add(CISales.CashReceipt.DueDate, _parameter.getParameterValue("dueDate"));
        insert.add(CISales.CashReceipt.CurrencyId, baseCurrInst.getId());
        insert.add(CISales.CashReceipt.RateCurrencyId, rateCurrInst.getId());
        insert.add(CISales.CashReceipt.Rate, rateObj);
        insert.execute();

        final CreatedDoc createdDoc = new CreatedDoc(insert.getInstance());
        createPositions(_parameter, calcList, createdDoc);
        return createdDoc;
    }

    /**
     * Internal Method to create the positions for this Document.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @param _calcList List of Calculators
     * @param _createdDoc cretaed Document
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
            final Insert posIns = new Insert(CISales.CashReceiptPosition);
            final Long productdId = Instance.get(_parameter.getParameterValues("product")[i]).getId();
            posIns.add(CISales.CashReceiptPosition.CashReceiptLink, _createdDoc.getInstance().getId());
            posIns.add(CISales.CashReceiptPosition.PositionNumber, i);
            posIns.add(CISales.CashReceiptPosition.Product, productdId);
            posIns.add(CISales.CashReceiptPosition.ProductDesc, _parameter.getParameterValues("productDesc")[i]);
            posIns.add(CISales.CashReceiptPosition.Quantity, calc.getQuantity());
            posIns.add(CISales.CashReceiptPosition.UoM, _parameter.getParameterValues("uoM")[i]);
            posIns.add(CISales.CashReceiptPosition.Tax, calc.getTaxId());
            posIns.add(CISales.CashReceiptPosition.Discount, calc.getDiscount());
            posIns.add(CISales.CashReceiptPosition.CurrencyId, baseCurrInst.getId());
            posIns.add(CISales.CashReceiptPosition.Rate, rateObj);
            posIns.add(CISales.CashReceiptPosition.RateCurrencyId, rateCurrInst.getId());
            posIns.add(CISales.CashReceiptPosition.CrossUnitPrice,
                                                calc.getCrossUnitPrice().divide(rate, BigDecimal.ROUND_HALF_UP));
            posIns.add(CISales.CashReceiptPosition.NetUnitPrice,
                                                calc.getNetUnitPrice().divide(rate, BigDecimal.ROUND_HALF_UP));
            posIns.add(CISales.CashReceiptPosition.CrossPrice, calc.getCrossPrice().divide(rate, BigDecimal.ROUND_HALF_UP));
            posIns.add(CISales.CashReceiptPosition.NetPrice, calc.getNetPrice().divide(rate, BigDecimal.ROUND_HALF_UP));
            posIns.add(CISales.CashReceiptPosition.DiscountNetUnitPrice, calc.getDiscountNetUnitPrice().divide(rate,
                                                                                    BigDecimal.ROUND_HALF_UP));
            posIns.execute();
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
}
