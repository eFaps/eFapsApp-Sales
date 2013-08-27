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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.datamodel.ui.RateUI;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.field.Field;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.sales.PriceUtil;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.SalesSettings;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("c4383bf6-9cca-492b-906f-37fb2e0c9ea2")
@EFapsRevision("$Rev$")
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
        final Return ret = createReportDoc(_parameter, createdDoc);
        return ret;
    }

    public Return createWithOutDoc(final Parameter _parameter)
        throws EFapsException
    {
        createDoc(_parameter);
        return new Return();
    }

    public Return addDoc2PaymentDoc(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = getCreateDoc2addPayment(_parameter);
        createPayment(_parameter, createdDoc);
        final Return ret = createReportDoc(_parameter, createdDoc);
        return ret;
    }

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
        createDoc.getValues().put(getFieldName4Attribute(_parameter, CISales.PaymentCheck.RateCurrencyLink.name), curId);
        createDoc.getValues().put(getFieldName4Attribute(_parameter, CISales.PaymentCheck.Date.name), date);

        return createDoc;
    }

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


    @Override
    public Return updateFields4RateCurrency(final Parameter _parameter)
        throws EFapsException
    {
        Return retVal = new Return();
        final Field field = (Field) _parameter.get(ParameterValues.UIOBJECT);
        if (CIFormSales.Sales_PaymentDepositWithOutDocForm.currencyLink.name.equals(field.getName())) {
            final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
            final Map<String, String> map = new HashMap<String, String>();

            final Instance newInst = Instance.get(CIERP.Currency.getType(),
                        _parameter.getParameterValue(CIFormSales.Sales_PaymentDepositWithOutDocForm.currencyLink.name));
            final Instance baseInst = Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);

            final BigDecimal[] rates = new PriceUtil().getRates(_parameter, newInst, baseInst);

            map.put(CIFormSales.Sales_PaymentDepositWithOutDocForm.rate.name, getFormater(0, 2).format(rates[3]));
            map.put(CIFormSales.Sales_PaymentDepositWithOutDocForm.rate.name + RateUI.INVERTEDSUFFIX,
                            "" + "" + (rates[3].compareTo(rates[0]) != 0));
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        } else {
            retVal = super.updateFields4RateCurrency(_parameter);
        }

        return retVal;
    }


}
