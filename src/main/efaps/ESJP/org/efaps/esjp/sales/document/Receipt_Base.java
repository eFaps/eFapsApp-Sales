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

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.sales.Calculator;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("0bea9972-51c4-4c93-a16b-515df8c4ea77")
@EFapsRevision("$Rev$")
public abstract class Receipt_Base
    extends DocumentSum
{

    /**
     * Method for create a new Receipt.
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final String date = _parameter.getParameterValue("date");
        final List<Calculator> calcList = analyseTable(_parameter, null);
        final Long contactid = Instance.get(_parameter.getParameterValue("contact")).getId();
        final Object[] rateObj = getRateObject(_parameter);
        final Insert insert = new Insert(CISales.Receipt);
        insert.add(CISales.Receipt.Contact, contactid.toString());
        insert.add(CISales.Receipt.CrossTotal, getCrossTotalStr(calcList));
        insert.add(CISales.Receipt.NetTotal, getNetTotalStr(calcList));
        insert.add(CISales.Receipt.RateNetTotal, getNetTotal(calcList));
        insert.add(CISales.Receipt.RateCrossTotal, getCrossTotal(calcList));
        insert.add(CISales.Receipt.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.Receipt.DiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.Receipt.Date, date);
        insert.add(CISales.Receipt.Salesperson, _parameter.getParameterValue("salesperson"));
        insert.add(CISales.Receipt.Status,
                            ((Long) Status.find(CISales.ReceiptStatus.uuid, "Open").getId()).toString());
        insert.add(CISales.Receipt.CurrencyId, _parameter.getParameterValue("rateCurrencyId"));
        insert.add(CISales.Receipt.RateCurrencyId, _parameter.getParameterValue("rateCurrencyId"));
        insert.add(CISales.Receipt.Rate, rateObj);
        insert.add(CISales.Receipt.Name, _parameter.getParameterValue("name4create"));
        insert.add(CISales.Receipt.Note, _parameter.getParameterValue("note"));
        insert.execute();
        Integer i = 0;
        for (final Calculator calc : calcList) {
            final Insert posIns = new Insert(CISales.ReceiptPosition);
            final Long productdId = Instance.get(_parameter.getParameterValues("product")[i]).getId();
            posIns.add(CISales.ReceiptPosition.Receipt, insert.getId());
            posIns.add(CISales.ReceiptPosition.PositionNumber, i.toString());
            posIns.add(CISales.ReceiptPosition.Product, productdId.toString());
            posIns.add(CISales.ReceiptPosition.ProductDesc, _parameter.getParameterValues("productAutoComplete")[i]);
            posIns.add(CISales.ReceiptPosition.Quantity, calc.getQuantityStr());
            posIns.add(CISales.ReceiptPosition.UoM, _parameter.getParameterValues("uoM")[i]);
            posIns.add(CISales.ReceiptPosition.CrossUnitPrice, calc.getCrossUnitPriceStr());
            posIns.add(CISales.ReceiptPosition.NetUnitPrice, calc.getNetUnitPriceStr());
            posIns.add(CISales.ReceiptPosition.CrossPrice, calc.getCrossPriceStr());
            posIns.add(CISales.ReceiptPosition.NetPrice, calc.getNetPriceStr());
            posIns.add(CISales.ReceiptPosition.Tax, (calc.getTaxId()).toString());
            posIns.add(CISales.ReceiptPosition.Discount, calc.getDiscountStr());
            posIns.add(CISales.ReceiptPosition.DiscountNetUnitPrice, calc.getDiscountNetUnitPrice());
            posIns.add(CISales.ReceiptPosition.CurrencyId, _parameter.getParameterValue("rateCurrencyId"));
            posIns.add(CISales.ReceiptPosition.Rate, rateObj);
            posIns.add(CISales.ReceiptPosition.RateCurrencyId, _parameter.getParameterValue("rateCurrencyId"));
            posIns.execute();
            i++;
        }
        return new Return();
    }
}
