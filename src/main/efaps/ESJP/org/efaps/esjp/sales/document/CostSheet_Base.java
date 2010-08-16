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
@EFapsUUID("8c4962d1-f010-43a7-a2a3-eefbbd9952a5")
@EFapsRevision("$Rev$")
public abstract class CostSheet_Base
    extends AbstractDocument
{

    /**
     * Executed from a Command execute vent to create a new CostSheet.
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
     * Create a new CostSheet.
     *
     * @param _parameter    Parameter as passed from the eFaps API.
     * @return Instance of the created Document.
     * @throws EFapsException on error.
     */
    protected Instance createDoc(final Parameter _parameter)
        throws EFapsException
    {
        final String date = _parameter.getParameterValue("date");
        final List<Calculator> calcList = analyseTable(_parameter, null);
        final Long contactid = Instance.get(_parameter.getParameterValue("contact")).getId();
        final Object[] rateObj = getRateObject(_parameter);
        final Insert insert = new Insert(CISales.CostSheet);
        insert.add(CISales.CostSheet.Contact, contactid);
        insert.add(CISales.CostSheet.CrossTotal, getCrossTotal(calcList));
        insert.add(CISales.CostSheet.NetTotal, getNetTotal(calcList));
        insert.add(CISales.CostSheet.RateNetTotal, getNetTotal(calcList));
        insert.add(CISales.CostSheet.RateCrossTotal, getCrossTotal(calcList));
        insert.add(CISales.CostSheet.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.CostSheet.Date, date);
        insert.add(CISales.CostSheet.Salesperson, _parameter.getParameterValue("salesperson"));
        insert.add(CISales.CostSheet.Name, _parameter.getParameterValue("name4create"));
        insert.add(CISales.CostSheet.Status, Status.find(CISales.CostSheetStatus.uuid, "Open").getId());
        insert.add(CISales.CostSheet.DiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.CostSheet.CurrencyId, _parameter.getParameterValue("rateCurrencyId"));
        insert.add(CISales.CostSheet.RateCurrencyId, _parameter.getParameterValue("rateCurrencyId"));
        insert.add(CISales.CostSheet.Rate, rateObj);
        insert.add(CISales.CostSheet.Note, _parameter.getParameterValue("note"));
        insert.execute();
        Integer i = 1;
        for (final Calculator calc : calcList) {
            final Insert posIns = new Insert(CISales.CostSheetPosition);
            final Long productdId = Instance.get(_parameter.getParameterValues("product")[i - 1]).getId();
            posIns.add(CISales.CostSheetPosition.CostSheet, insert.getId());
            posIns.add(CISales.CostSheetPosition.PositionNumber, i.toString());
            posIns.add(CISales.CostSheetPosition.Product, productdId.toString());
            posIns.add(CISales.CostSheetPosition.ProductDesc, _parameter.getParameterValues("productDesc")[i - 1]);
            posIns.add(CISales.CostSheetPosition.Quantity, calc.getQuantityStr());
            posIns.add(CISales.CostSheetPosition.UoM, _parameter.getParameterValues("uoM")[i - 1]);
            posIns.add(CISales.CostSheetPosition.CrossUnitPrice, calc.getCrossUnitPriceStr());
            posIns.add(CISales.CostSheetPosition.NetUnitPrice, calc.getNetUnitPriceStr());
            posIns.add(CISales.CostSheetPosition.CrossPrice, calc.getCrossPriceStr());
            posIns.add(CISales.CostSheetPosition.NetPrice, calc.getNetPriceStr());
            posIns.add(CISales.CostSheetPosition.Tax, (calc.getTaxId()).toString());
            posIns.add(CISales.CostSheetPosition.Discount, calc.getDiscountStr());
            posIns.add(CISales.CostSheetPosition.DiscountNetUnitPrice, calc.getDiscountNetUnitPrice());
            posIns.add(CISales.CostSheetPosition.CurrencyId, _parameter.getParameterValue("rateCurrencyId"));
            posIns.add(CISales.CostSheetPosition.Rate, rateObj);
            posIns.add(CISales.CostSheetPosition.RateCurrencyId, _parameter.getParameterValue("rateCurrencyId"));
            posIns.execute();
            i++;
        }
        return insert.getInstance();
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
