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
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.sales.PriceUtil;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * Class is the generic instance for all documents of type DocumentSum.
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("e177ab08-67f0-4ce2-8eff-d3f167352bee")
@EFapsRevision("$Rev$")
public abstract class DocumentSum_Base
    extends AbstractDocument
{

    /**
     * Used by the update event used in the select doc form
     * for CostSheet.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return map list for update event
     * @throws EFapsException on error
     */
    public Return recalculateRate(final Parameter _parameter)
        throws EFapsException
    {
        final Instance docInst = _parameter.getInstance();
        if (docInst.getType().isKindOf(CISales.DocumentSumAbstract.getType())) {
            final PrintQuery print = new PrintQuery(docInst);
            print.addAttribute(CISales.DocumentSumAbstract.RateCrossTotal,
                            CISales.DocumentSumAbstract.RateDiscountTotal,
                            CISales.DocumentSumAbstract.RateNetTotal,
                            CISales.DocumentSumAbstract.RateCurrencyId,
                            CISales.DocumentSumAbstract.CurrencyId,
                            CISales.DocumentSumAbstract.Date);
            print.execute();

            final BigDecimal rateCross = print.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
            final BigDecimal rateDiscount = print.<BigDecimal>getAttribute(
                            CISales.DocumentSumAbstract.RateDiscountTotal);
            final BigDecimal rateNet = print.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateNetTotal);
            final Instance targetCurrInst = Instance.get(CIERP.Currency.getType(),
                            print.<Long>getAttribute(CISales.DocumentSumAbstract.RateCurrencyId));
            final Instance currentInst = Instance.get(CIERP.Currency.getType(),
                            print.<Long>getAttribute(CISales.DocumentSumAbstract.CurrencyId));
            final DateTime date = print.<DateTime>getAttribute(CISales.DocumentSumAbstract.Date);

            final PriceUtil priceUtil = new PriceUtil();
            final BigDecimal[] rates = priceUtil.getRates(_parameter, targetCurrInst, currentInst);
            final BigDecimal rate = rates[2];

            final BigDecimal[] rates2 = priceUtil.getExchangeRate(priceUtil.getDateFromParameter(_parameter),
                            targetCurrInst);
            final CurrencyInst currInst = new CurrencyInst(targetCurrInst);

            final Object[] rateObj = new Object[] { currInst.isInvert() ? BigDecimal.ONE : rates2[0],
                            currInst.isInvert() ? rates2[1] : BigDecimal.ONE };

            final Update update = new Update(docInst);
            update.add(CISales.DocumentSumAbstract.CrossTotal, rateCross.compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO : rateCross.divide(rate, BigDecimal.ROUND_HALF_UP));
            update.add(CISales.DocumentSumAbstract.NetTotal, rateNet.compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO : rateNet.divide(rate, BigDecimal.ROUND_HALF_UP));
            update.add(CISales.DocumentSumAbstract.DiscountTotal, rateDiscount.compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO : rateDiscount.divide(rate, BigDecimal.ROUND_HALF_UP));
            update.add(CISales.DocumentSumAbstract.Rate, rateObj);
            update.execute();

            final BigDecimal[] rates4Calc = priceUtil.getRates(date, targetCurrInst, currentInst);

            final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionAbstract);
            queryBldr.addWhereAttrEqValue(CISales.PositionAbstract.DocumentAbstractLink, docInst.getId());
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CISales.PositionAbstract.CrossPrice, CISales.PositionAbstract.CrossUnitPrice,
                            CISales.PositionAbstract.DiscountNetUnitPrice,  CISales.PositionAbstract.NetPrice,
                            CISales.PositionAbstract.NetUnitPrice);
            multi.execute();
            while (multi.next()) {
                final BigDecimal crossPrice = multi.<BigDecimal>getAttribute(CISales.PositionAbstract.CrossPrice);
                final BigDecimal dscNetUnitPrice = multi.<BigDecimal>getAttribute(
                                CISales.PositionAbstract.DiscountNetUnitPrice);
                final BigDecimal crossUnitPrice = multi.<BigDecimal>getAttribute(
                                CISales.PositionAbstract.CrossUnitPrice);
                final BigDecimal netPrice = multi.<BigDecimal>getAttribute(
                                CISales.PositionAbstract.NetPrice);
                final BigDecimal netUnitPrice = multi.<BigDecimal>getAttribute(CISales.PositionAbstract.NetUnitPrice);

                final Update updatePos =  new Update(multi.getCurrentInstance());
                updatePos.add(CISales.PositionAbstract.CrossPrice, getNewValue(crossPrice, rates4Calc[2], rates[2]));
                updatePos.add(CISales.PositionAbstract.DiscountNetUnitPrice,
                                getNewValue(dscNetUnitPrice, rates4Calc[2], rates[2]));
                updatePos.add(CISales.PositionAbstract.CrossUnitPrice,
                                getNewValue(crossUnitPrice, rates4Calc[2], rates[2]));
                updatePos.add(CISales.PositionAbstract.NetPrice, getNewValue(netPrice, rates4Calc[2], rates[2]));
                updatePos.add(CISales.PositionAbstract.NetUnitPrice,
                                getNewValue(netUnitPrice, rates4Calc[2], rates[2]));
                updatePos.add(CISales.PositionAbstract.Rate, rateObj);
                updatePos.execute();
            }
        }
        return new Return();
    }

    /**
     * @param _oldValue old Value
     * @param _oldRate old Rate
     * @param _newRate new Rate
     * @return new Value
     */
    protected BigDecimal getNewValue(final BigDecimal _oldValue,
                                     final BigDecimal _oldRate,
                                     final BigDecimal _newRate)
    {
        BigDecimal ret = BigDecimal.ZERO;
        if (_oldValue.compareTo(BigDecimal.ZERO) != 0) {
            ret = _oldValue.multiply(_oldRate).divide(_newRate, BigDecimal.ROUND_HALF_UP)
                .setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        return ret;
    }

    /**
     * Used by an FieldUpdate event used in the form
     * for Recalculating DocumentSum with a rate.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return map list for update event
     * @throws EFapsException on error
     */
    public Return update4DateOnRecalculate(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final Instance docInst = _parameter.getInstance();
        if (docInst.getType().isKindOf(CISales.DocumentSumAbstract.getType())) {
            final PrintQuery print = new PrintQuery(docInst);
            print.addAttribute(CISales.DocumentSumAbstract.RateCurrencyId,
                            CISales.DocumentSumAbstract.CurrencyId);
            print.execute();

            final Instance targetCurrInst = Instance.get(CIERP.Currency.getType(),
                            print.<Long> getAttribute(CISales.DocumentSumAbstract.RateCurrencyId));
            final PriceUtil priceUtil = new PriceUtil();
            final BigDecimal[] rates = priceUtil.getExchangeRate(priceUtil.getDateFromParameter(_parameter),
                            targetCurrInst);
            final BigDecimal rate = rates[1];

            final DecimalFormat formater = (DecimalFormat) NumberFormat.getInstance(
                            Context.getThreadContext().getLocale());
            formater.applyPattern("#,##0.############");
            formater.setRoundingMode(RoundingMode.HALF_UP);
            final String rateStr = formater.format(rate);
            final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
            final Map<String, String> map = new HashMap<String, String>();
            map.put("rate", rateStr);
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        }
        return retVal;
    }

}
