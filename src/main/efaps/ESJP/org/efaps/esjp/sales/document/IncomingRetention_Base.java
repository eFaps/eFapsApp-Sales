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
import org.efaps.db.PrintQuery;
import org.efaps.esjp.ci.CIERP;
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
@EFapsUUID("6c067449-1aba-42ea-b259-38a3d1e32658")
@EFapsRevision("$Rev$")
public abstract class IncomingRetention_Base
    extends DocumentSum
{
    /**
     * Used to store the PerceptionValue in the Context.
     */
    public static final String AMOUNTVALUE = IncomingRetention.class.getName() + ".AmountValue";

    /**
     * Executed from a Command execute event to create a new Incoming PerceptionCertificate.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @param _createdDoc as CreatedDoc with values.
     * @param _index > 0 if position, -1 if doc itself
     * @throws EFapsException on error
     */
    public void create4Doc(final Parameter _parameter,
                           final CreatedDoc _createdDoc,
                           final int _index)
        throws EFapsException
    {
        final BigDecimal amount = (BigDecimal) _createdDoc.getValue(IncomingRetention_Base.AMOUNTVALUE);
        final Instance documentInst;
        if (_index < 0) {
            documentInst = _createdDoc.getInstance();
        } else {
            documentInst = _createdDoc.getPositions().get(_index);
        }

        final Insert insert = new Insert(CISales.IncomingRetention);
        insert.add(CISales.IncomingRetention.Date, _createdDoc.getValues()
                        .get(CISales.PaymentDocumentAbstract.Date.name));
        insert.add(CISales.IncomingRetention.Contact, _createdDoc.getValues()
                        .get(CISales.PaymentDocumentAbstract.Contact.name));
        insert.add(CISales.IncomingRetention.Salesperson, Context.getThreadContext().getPersonId());
        insert.add(CISales.IncomingRetention.Group, _createdDoc.getValues()
                        .get(CISales.PaymentDocumentAbstract.Group.name));
        insert.add(CISales.IncomingRetention.Rate, _createdDoc.getValues()
                        .get(CISales.PaymentDocumentAbstract.Rate.name));
        if (_createdDoc.getValues().containsKey(CISales.PaymentDocumentAbstract.CurrencyLink.name)) {
            insert.add(CISales.IncomingRetention.CurrencyId, _createdDoc.getValues()
                        .get(CISales.PaymentDocumentAbstract.CurrencyLink.name));
            insert.add(CISales.IncomingRetention.RateCurrencyId, _createdDoc.getValues()
                        .get(CISales.PaymentDocumentAbstract.RateCurrencyLink.name));
        } else {
            insert.add(CISales.IncomingRetention.CurrencyId, _createdDoc.getValues()
                            .get(CISales.DocumentSumAbstract.CurrencyId.name));
            insert.add(CISales.IncomingDetraction.RateCurrencyId, _createdDoc.getValues()
                            .get(CISales.IncomingRetention.RateCurrencyId.name));
        }

        insert.add(CISales.IncomingRetention.RateNetTotal, BigDecimal.ZERO);
        insert.add(CISales.IncomingRetention.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.IncomingRetention.NetTotal, BigDecimal.ZERO);
        insert.add(CISales.IncomingRetention.DiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.IncomingRetention.Status, Status.find(CISales.IncomingRetentionStatus.Open));
        insert.add(CISales.IncomingRetention.Name, getDocName4Document(_parameter, documentInst));

        final DecimalFormat totalFrmt = NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter));
        final int scale = totalFrmt.getMaximumFractionDigits();

        insert.add(CISales.IncomingRetention.RateCrossTotal, amount.setScale(scale, BigDecimal.ROUND_HALF_UP));
        insert.add(CISales.IncomingRetention.CrossTotal, amount.setScale(scale, BigDecimal.ROUND_HALF_UP));
        insert.execute();

        final Insert relInsert = new Insert(CISales.IncomingRetention2IncomingInvoice);
        relInsert.add(CISales.IncomingRetention2IncomingInvoice.FromLink, insert.getInstance());
        relInsert.add(CISales.IncomingRetention2IncomingInvoice.ToLink, documentInst);
        relInsert.execute();
    }

    protected String getDocName4Document(final Parameter _parameter,
                                         final Instance _instance)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_instance);
        print.addAttribute(CIERP.DocumentAbstract.Name);
        print.execute();

        return print.<String>getAttribute(CIERP.DocumentAbstract.Name);
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
                                .getParameterValue(CIFormSales.Sales_IncomingRetentionForm.crossTotal.name));
                ret = ret.add(rateCrossTotal);
            } catch (final ParseException p) {
                throw new EFapsException(IncomingRetention.class, "RateCrossTotal.ParseException", p);
            }
        } else {
            ret = super.getCrossTotal(_parameter, _calcList);
        }

        return ret;
    }
}
