/*
 * Copyright 2003 - 2013 The eFaps Team
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

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.Calculator;
import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("15c2a1f5-9b93-4389-a298-5c4116ecf614")
@EFapsRevision("$Rev$")
public abstract class IncomingDetraction_Base
    extends DocumentSum
{

    /**
     * Used to store the PerceptionValue in the Context.
     */
    public static final String AMOUNTVALUE = IncomingDetraction.class.getName() + ".AmountValue";


    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _createdDoc   cretaeddo
     * @param _index >0 if position, -1 if doc itself
     * @throws EFapsException on error
     */
    public void create4Doc(final Parameter _parameter,
                           final CreatedDoc _createdDoc,
                           final int _index)
        throws EFapsException
    {
        final BigDecimal amount = (BigDecimal) _createdDoc.getValue(IncomingDetraction_Base.AMOUNTVALUE);
        final Instance documentInst;
        if (_index < 0) {
            documentInst = _createdDoc.getInstance();
        } else {
            documentInst = _createdDoc.getPositions().get(_index);
        }

        final Insert insert = new Insert(CISales.IncomingDetraction);
        insert.add(CISales.IncomingDetraction.Date, _createdDoc.getValues()
                        .get(CISales.PaymentDocumentAbstract.Date.name));
        insert.add(CISales.IncomingDetraction.Contact, _createdDoc.getValues()
                        .get(CISales.PaymentDocumentAbstract.Contact.name));
        insert.add(CISales.IncomingDetraction.Salesperson, Context.getThreadContext().getPersonId());
        insert.add(CISales.IncomingDetraction.Group, _createdDoc.getValues()
                        .get(CISales.PaymentDocumentAbstract.Group.name));
        insert.add(CISales.IncomingDetraction.Rate, _createdDoc.getValues()
                        .get(CISales.PaymentDocumentAbstract.Rate.name));
        if (_createdDoc.getValues().containsKey(CISales.PaymentDocumentAbstract.CurrencyLink.name)) {
            insert.add(CISales.IncomingDetraction.CurrencyId, _createdDoc.getValues()
                        .get(CISales.PaymentDocumentAbstract.CurrencyLink.name));
            insert.add(CISales.IncomingDetraction.RateCurrencyId, _createdDoc.getValues()
                        .get(CISales.PaymentDocumentAbstract.RateCurrencyLink.name));
        } else {
            insert.add(CISales.IncomingDetraction.CurrencyId, _createdDoc.getValues()
                            .get(CISales.DocumentSumAbstract.CurrencyId.name));
                insert.add(CISales.IncomingDetraction.RateCurrencyId, _createdDoc.getValues()
                            .get(CISales.DocumentSumAbstract.RateCurrencyId.name));
        }
        insert.add(CISales.IncomingDetraction.RateNetTotal, BigDecimal.ZERO);
        insert.add(CISales.IncomingDetraction.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.IncomingDetraction.NetTotal, BigDecimal.ZERO);
        insert.add(CISales.IncomingDetraction.DiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.IncomingDetraction.Status, Status.find(CISales.IncomingDetractionStatus.Open));
        insert.add(CISales.IncomingDetraction.Name, getDocName4Document(_parameter, _createdDoc));

        final Object[] rateObj = (Object[]) _createdDoc.getValue(CISales.DocumentSumAbstract.Rate.name);
        final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                        BigDecimal.ROUND_HALF_UP);
        final DecimalFormat totalFrmt = NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter));
        final int scale = totalFrmt.getMaximumFractionDigits();

        insert.add(CISales.IncomingDetraction.RateCrossTotal, amount.setScale(scale, BigDecimal.ROUND_HALF_UP));
        insert.add(CISales.IncomingDetraction.CrossTotal,
                        amount.divide(rate, BigDecimal.ROUND_HALF_UP).setScale(scale, BigDecimal.ROUND_HALF_UP));
        insert.execute();

        final Insert relInsert = new Insert(CISales.IncomingDetraction2IncomingInvoice);
        relInsert.add(CISales.IncomingDetraction2IncomingInvoice.FromLink, insert.getInstance());
        relInsert.add(CISales.IncomingDetraction2IncomingInvoice.ToLink, documentInst);
        relInsert.execute();
    }


    /**
     * Method to obtains name to CreatedDoc.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @param _createdDoc CreatedDoc with values.
     * @return Object to name.
     * @throws EFapsException on error.
     */
    protected Object getDocName4Document(final Parameter _parameter,
                                         final CreatedDoc _createdDoc)
        throws EFapsException
    {
        return _createdDoc.getValues().get(CISales.DocumentSumAbstract.Name.name);
    }

    /**
     * Executed from a Command execute event to edit.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        editDoc(_parameter);
        return new Return();
    }

    @Override
    protected BigDecimal getCrossTotal(final Parameter _parameter,
                                       final List<Calculator> _calcList)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        if (_calcList.isEmpty()) {
            final DecimalFormat formatter = NumberFormatter.get().getFormatter();
            try {
                final BigDecimal rateCrossTotal = (BigDecimal) formatter.parse(_parameter
                                .getParameterValue(CIFormSales.Sales_IncomingDetractionForm.crossTotal.name));
                ret = ret.add(rateCrossTotal);
            } catch (final ParseException p) {
                throw new EFapsException(IncomingDetraction.class, "RateCrossTotal.ParseException", p);
            }
        } else {
            ret = super.getCrossTotal(_parameter, _calcList);
        }

        return ret;
    }
}
