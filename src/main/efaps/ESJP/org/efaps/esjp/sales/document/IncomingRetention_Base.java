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
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.NumberFormatter;
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
     * Executed from a Command execute vent to create a new Incoming PerceptionCertificate.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @param _createdDoc as CreatedDoc with values.
     * @throws EFapsException on error
     */
    public void create4Doc(final Parameter _parameter,
                           final CreatedDoc _createdDoc,
                           final int _index)
        throws EFapsException
    {
        final BigDecimal amount = (BigDecimal) _createdDoc.getValue(AMOUNTVALUE);
        final Instance documentInst = _createdDoc.getPositions().get(_index);

        final Insert insert = new Insert(CISales.IncomingRetention);
        insert.add(CISales.IncomingRetention.Date, _createdDoc.getValues()
                        .get(getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.Date.name)));
        insert.add(CISales.IncomingRetention.Contact, _createdDoc.getValues()
                        .get(getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.Contact.name)));
        insert.add(CISales.IncomingRetention.Salesperson, Context.getThreadContext().getPersonId());
        insert.add(CISales.IncomingRetention.Group, _createdDoc.getValues()
                        .get(getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.Group.name)));
        insert.add(CISales.IncomingRetention.Rate, _createdDoc.getValues()
                        .get(getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.Rate.name)));
        insert.add(CISales.IncomingRetention.CurrencyId, _createdDoc.getValues()
                        .get(getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.CurrencyLink.name)));
        insert.add(CISales.IncomingRetention.RateCurrencyId, _createdDoc.getValues()
                        .get(getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.RateCurrencyLink.name)));
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
}
