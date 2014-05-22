/*
 * Copyright 2003 - 2012 The eFaps Team
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

package org.efaps.esjp.sales.payment;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("10218fe0-b4bf-48b5-bd67-001a921ba877")
@EFapsRevision("$Rev$")
public abstract class PaymentExchangeOut_Base
    extends AbstractPaymentOut
{

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = createDoc(_parameter);
        createPayment(_parameter, createdDoc);
        connectPaymentDocument2Document(_parameter, createdDoc);
        final Return ret = createReportDoc(_parameter, createdDoc);
        return ret;
    }

    @Override
    protected String getDocName4Create(final Parameter _parameter)
        throws EFapsException
    {
        final String name;
        final Instance instance = Instance.get(_parameter.getParameterValue("name"));

        if (instance.isValid()) {
            final PrintQuery print = new PrintQuery(instance);
            print.addAttribute(CISales.DocumentAbstract.Name);
            print.execute();

            name = print.<String>getAttribute(CISales.DocumentAbstract.Name);
        } else {
            name = super.getDocName4Create(_parameter);
        }

        return name;
    }

    @Override
    protected void connectPaymentDocument2Document(final Parameter _parameter,
                                                   final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final Instance instDoc = Instance.get(_parameter
                        .getParameterValue(CIFormSales.Sales_PaymentExchangeOutForm.name.name));
        if (instDoc.isValid() && _createdDoc.getInstance().isValid()) {
            final Insert insert = new Insert(CISales.PaymentExchangeOut2IncomingExchange);
            insert.add(CISales.PaymentExchangeOut2IncomingExchange.FromLink, _createdDoc.getInstance());
            insert.add(CISales.PaymentExchangeOut2IncomingExchange.ToLink, instDoc);
            insert.execute();

            _createdDoc.getValues().put("connectDocument", insert.getInstance());
        }
    }

    @Override
    protected void add2QueryBldr4autoComplete4CreateDocument(final Parameter _parameter,
                                                             final QueryBuilder _queryBldr)
        throws EFapsException
    {
        final Instance exchange = Instance.get(_parameter.getParameterValue("name"));
        if (exchange.isValid()) {
            final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.IncomingExchange2Invoice);
            attrQueryBldr.addWhereAttrEqValue(CISales.IncomingExchange2Invoice.FromLink, exchange);
            final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.IncomingExchange2Invoice.ToLink);

            _queryBldr.addWhereAttrInQuery(CIERP.DocumentAbstract.ID, attrQuery);
        } else {
            _queryBldr.addWhereAttrEqValue(CIERP.DocumentAbstract.ID, 0);
        }
    }
}
