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

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.util.EFapsException;


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


    public void create4Doc(final Parameter _parameter,
                           final CreatedDoc _createdDoc,
                           final int _index)
        throws EFapsException
    {
        final BigDecimal amount = (BigDecimal) _createdDoc.getValue(AMOUNTVALUE);

        final Insert insert = new Insert(CISales.IncomingDetraction);
        insert.add(CISales.IncomingDetraction.Date, _createdDoc.getValues()
                        .get(getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.Date.name)));
        insert.add(CISales.IncomingDetraction.Contact, _createdDoc.getValues()
                        .get(getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.Contact.name)));
        insert.add(CISales.IncomingDetraction.Group, _createdDoc.getValues()
                        .get(getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.Group.name)));
        insert.add(CISales.IncomingDetraction.Rate, _createdDoc.getValues()
                        .get(getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.Rate.name)));
        insert.add(CISales.IncomingDetraction.CurrencyId, _createdDoc.getValues()
                        .get(getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.CurrencyLink.name)));
        insert.add(CISales.IncomingDetraction.RateCurrencyId, _createdDoc.getValues()
                        .get(getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.RateCurrencyLink.name)));
        insert.add(CISales.IncomingDetraction.RateNetTotal, BigDecimal.ZERO);
        insert.add(CISales.IncomingDetraction.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.IncomingDetraction.NetTotal, BigDecimal.ZERO);
        insert.add(CISales.IncomingDetraction.DiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.IncomingDetraction.Status, Status.find(CISales.IncomingDetractionStatus.Open));
        insert.add(CISales.IncomingDetraction.Name, _createdDoc.getValues()
                        .get(getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.Code.name)));

        final DecimalFormat totalFrmt = NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter));
        final int scale = totalFrmt.getMaximumFractionDigits();

        insert.add(CISales.IncomingDetraction.RateCrossTotal, amount.setScale(scale, BigDecimal.ROUND_HALF_UP));
        insert.add(CISales.IncomingDetraction.CrossTotal, amount.setScale(scale, BigDecimal.ROUND_HALF_UP));
        insert.execute();

        final Insert relInsert = new Insert(CISales.IncomingDetraction2IncomingInvoice);
        relInsert.add(CISales.IncomingDetraction2IncomingInvoice.FromLink, insert.getInstance());
        relInsert.add(CISales.IncomingDetraction2IncomingInvoice.ToLink, _createdDoc.getPositions().get(_index));
        relInsert.execute();
    }
}
