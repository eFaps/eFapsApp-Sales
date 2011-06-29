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
import org.efaps.admin.datamodel.Type;
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
@EFapsUUID("2cedc1c6-2197-4048-a5bb-911d467f0ff9")
@EFapsRevision("$Rev$")
public abstract class Quotation_Base
    extends DocumentSum
{

    /**
     * Method for create a new Quotation.
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

        final Insert insert = new Insert(CISales.Quotation);
        insert.add(CISales.Quotation.Contact, contactid);
        insert.add(CISales.Quotation.CrossTotal, getCrossTotal(calcList).divide(rate, BigDecimal.ROUND_HALF_UP));
        insert.add(CISales.Quotation.NetTotal, getNetTotal(calcList).divide(rate, BigDecimal.ROUND_HALF_UP));
        insert.add(CISales.Quotation.DiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.Quotation.RateNetTotal, getNetTotal(calcList));
        insert.add(CISales.Quotation.RateCrossTotal, getCrossTotal(calcList));
        insert.add(CISales.Quotation.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.Quotation.Date, date);
        insert.add(CISales.Quotation.DueDate, _parameter.getParameterValue("dueDate") != null
                                                    ? _parameter.getParameterValue("dueDate"): date);
        insert.add(CISales.Quotation.Salesperson, _parameter.getParameterValue("salesperson"));
        insert.add(CISales.Quotation.Name, getDocName4Create(_parameter));
        insert.add(CISales.Quotation.Status, Status.find(CISales.QuotationStatus.uuid, "Open").getId());
        insert.add(CISales.Quotation.CurrencyId, baseCurrInst.getId());
        insert.add(CISales.Quotation.RateCurrencyId, rateCurrInst.getId());
        insert.add(CISales.Quotation.Rate, rateObj);
        insert.add(CISales.Quotation.Note, _parameter.getParameterValue("note"));
        insert.execute();

        final CreatedDoc createdDoc = new CreatedDoc(insert.getInstance());
        createPositions(_parameter, calcList, createdDoc);
        return createdDoc;
    }

    @Override
    protected Type getPositionType(final Parameter _parameter)
    {
        return CISales.QuotationPosition.getType();
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
