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
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("a66a61fd-487a-4764-9e72-f65050c1d39e")
@EFapsRevision("$Rev$")
public abstract class CreditNote_Base
    extends DocumentSum
{

    /**
     * Method for create a new Credit Note.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        try {
            final String date = _parameter.getParameterValue("date");
            final List<Calculator> calcList = analyseTable(_parameter, null);
            final Instance baseCurrInst = SystemConfiguration.get(
                            UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f")).getLink("CurrencyBase");
            final Instance rateCurrInst = Instance.get(CIERP.Currency.getType(), _parameter
                            .getParameterValue("rateCurrencyId"));
            final Object[] rateObj = getRateObject(_parameter);
            final BigDecimal rate = (BigDecimal) Calculator_Base.getFormatInstance().parse(
                            _parameter.getParameterValue("rate"));
            final Long contactid = Instance.get(_parameter.getParameterValue("contact")).getId();
            final Insert insert = new Insert(CISales.CreditNote);
            insert.add(CISales.CreditNote.Contact, contactid.toString());
            insert.add(CISales.CreditNote.CrossTotal, getCrossTotalStr(calcList));
            insert.add(CISales.CreditNote.RateCrossTotal, getCrossTotalStr(calcList));
            insert.add(CISales.CreditNote.NetTotal, getNetTotalStr(calcList));
            insert.add(CISales.CreditNote.RateNetTotal, getNetTotalStr(calcList));
            insert.add(CISales.CreditNote.DiscountTotal, BigDecimal.ZERO);
            insert.add(CISales.CreditNote.RateDiscountTotal, BigDecimal.ZERO);
            insert.add(CISales.CreditNote.CurrencyId, baseCurrInst.getId());
            insert.add(CISales.CreditNote.RateCurrencyId, rateCurrInst.getId());
            insert.add(CISales.CreditNote.Rate, rateObj);
            insert.add(CISales.CreditNote.Date, date);
            insert.add(CISales.CreditNote.Note, _parameter.getParameterValue("note"));
            insert.add(CISales.CreditNote.DueDate, _parameter.getParameterValue("dueDate"));
            insert.add(CISales.CreditNote.Salesperson, _parameter.getParameterValue("salesperson"));
            insert.add(CISales.CreditNote.Status,
                                ((Long) Status.find(CISales.CreditNoteStatus.uuid, "Open").getId()).toString());
            insert.add(CISales.CreditNote.Name, _parameter.getParameterValue("name4create"));

            insert.execute();
            Integer i = 0;
            for (final Calculator calc : calcList) {
                if (!calc.isEmpty()) {
                    final Insert posIns = new Insert(CISales.CreditNotePosition);
                    final Long productdId = Instance.get(_parameter.getParameterValues("product")[i]).getId();
                    posIns.add(CISales.CreditNotePosition.CreditNote, insert.getId());
                    posIns.add(CISales.CreditNotePosition.PositionNumber, i);
                    posIns.add(CISales.CreditNotePosition.Product, productdId);
                    posIns.add(CISales.CreditNotePosition.ProductDesc, _parameter.getParameterValues("productDesc")[i]);
                    posIns.add(CISales.CreditNotePosition.Quantity, calc.getQuantity());
                    posIns.add(CISales.CreditNotePosition.UoM, _parameter.getParameterValues("uoM")[i]);
                    posIns.add(CISales.CreditNotePosition.Tax, calc.getTaxId());
                    posIns.add(CISales.CreditNotePosition.Discount, calc.getDiscount());
                    posIns.add(CISales.CreditNotePosition.CurrencyId, rateCurrInst.getId());
                    posIns.add(CISales.CreditNotePosition.Rate, rateObj);
                    posIns.add(CISales.CreditNotePosition.RateCurrencyId, rateCurrInst.getId());
                    if (baseCurrInst.equals(rateCurrInst)) {
                        posIns.add(CISales.CreditNotePosition.CrossUnitPrice, calc.getCrossUnitPrice());
                        posIns.add(CISales.CreditNotePosition.NetUnitPrice, calc.getNetUnitPrice());
                        posIns.add(CISales.CreditNotePosition.CrossPrice, calc.getCrossPrice());
                        posIns.add(CISales.CreditNotePosition.NetPrice, calc.getNetPrice());
                        posIns.add(CISales.CreditNotePosition.DiscountNetUnitPrice, calc.getDiscountNetUnitPrice());
                    } else {
                        posIns.add(CISales.CreditNotePosition.CrossUnitPrice,
                                                calc.getCrossUnitPrice().divide(rate, BigDecimal.ROUND_HALF_UP));
                        posIns.add(CISales.CreditNotePosition.NetUnitPrice,
                                                calc.getNetUnitPrice().divide(rate, BigDecimal.ROUND_HALF_UP));
                        posIns.add(CISales.CreditNotePosition.CrossPrice,
                                                calc.getCrossPrice().divide(rate, BigDecimal.ROUND_HALF_UP));
                        posIns.add(CISales.CreditNotePosition.NetPrice,
                                                calc.getNetPrice().divide(rate, BigDecimal.ROUND_HALF_UP));
                        posIns.add(CISales.CreditNotePosition.DiscountNetUnitPrice,
                                                calc.getDiscountNetUnitPrice().divide(rate, BigDecimal.ROUND_HALF_UP));
                    }
                    posIns.execute();
                }
                i++;
            }
        } catch (final ParseException e) {
            throw new EFapsException(Invoice.class, "create.ParseException", e);
        }
        return new Return();
    }
}
