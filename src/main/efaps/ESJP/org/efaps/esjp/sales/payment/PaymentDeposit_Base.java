/*
 * Copyright 2003 - 2016 The eFaps Team
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
 */

package org.efaps.esjp.sales.payment;

import java.io.File;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 *
 */
@EFapsUUID("c4383bf6-9cca-492b-906f-37fb2e0c9ea2")
@EFapsApplication("eFapsApp-Sales")
public abstract class PaymentDeposit_Base
    extends AbstractPaymentIn
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
        executeAutomation(_parameter, createdDoc);
        final Return ret = new Return();
        final File file = createReport(_parameter, createdDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    /**
     * Creates the 4 doc.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return create4Doc(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = createDoc(_parameter);
        createPayment4Doc(_parameter, createdDoc);
        executeAutomation(_parameter, createdDoc);
        final Return ret = new Return();
        final File file = createReport(_parameter, createdDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    /**
     * Adds the doc 2 payment doc.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return addDoc2PaymentDoc(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = getCreateDoc2addPayment(_parameter);
        createPayment(_parameter, createdDoc);
        final Return ret = new Return();
        final File file = createReport(_parameter, createdDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }

        return ret;
    }

    /**
     * Gets the creates the doc 2 add payment.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the creates the doc 2 add payment
     * @throws EFapsException on error
     */
    protected CreatedDoc getCreateDoc2addPayment(final Parameter _parameter)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_parameter.getInstance());
        print.addAttribute(CISales.PaymentCheck.Name,
                        CISales.PaymentCheck.RateCurrencyLink,
                        CISales.PaymentCheck.Date);
        print.execute();
        final String name = print.<String>getAttribute(CISales.PaymentCheck.Name);
        final Long curId = print.<Long>getAttribute(CISales.PaymentCheck.RateCurrencyLink);
        final DateTime date = print.<DateTime>getAttribute(CISales.PaymentCheck.Date);

        final CreatedDoc createDoc = new CreatedDoc(_parameter.getInstance());
        createDoc.getValues().put(getFieldName4Attribute(_parameter, CISales.PaymentCheck.Name.name), name);
        createDoc.getValues().put(getFieldName4Attribute(_parameter,
                        CISales.PaymentCheck.RateCurrencyLink.name), curId);
        createDoc.getValues().put(getFieldName4Attribute(_parameter, CISales.PaymentCheck.Date.name), date);

        return createDoc;
    }

    /**
     * Drop down field value 4 account.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return dropDownFieldValue4Account(final Parameter _parameter)
        throws EFapsException
    {
        return new org.efaps.esjp.common.uiform.Field()
        {
            @Override
            protected void add2QueryBuilder4List(final Parameter _parameter,
                                                 final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final PrintQuery print = new PrintQuery(_parameter.getInstance());
                print.addAttribute(CISales.PaymentCheck.RateCurrencyLink);
                print.execute();
                final Long curId = print.<Long>getAttribute(CISales.PaymentCheck.RateCurrencyLink);

                _queryBldr.addWhereAttrEqValue(CISales.AccountCashDesk.CurrencyLink, curId);
            }
        }.dropDownFieldValue(_parameter);
    }

}
