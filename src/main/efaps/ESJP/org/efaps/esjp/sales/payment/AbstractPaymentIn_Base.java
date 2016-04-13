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

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.datamodel.ui.RateUI;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.field.Field;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.PriceUtil;
import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("35abd734-d033-460f-bb00-2f40e3a58320")
@EFapsApplication("eFapsApp-Sales")
public abstract class AbstractPaymentIn_Base
    extends AbstractPaymentDocument
{
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
            final Instance baseInst = Currency.getBaseCurrency();

            final BigDecimal[] rates = new PriceUtil().getRates(_parameter, newInst, baseInst);

            map.put("rate", NumberFormatter.get().getFormatter(0, 2).format(rates[3]));
            map.put("rate" + RateUI.INVERTEDSUFFIX, "" + "" + (rates[3].compareTo(rates[0]) != 0));
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        } else {
            retVal = super.updateFields4RateCurrency(_parameter);
        }

        return retVal;
    }


    public Return getJavaScript4AmountAddDoc(final Parameter _parameter)
        throws EFapsException
    {
        final DecimalFormat fmtr = NumberFormatter.get().getFormatter(2, 2);

        final Instance instance = _parameter.getInstance();
        final Return ret = new Return();
        if (instance != null && instance.isValid()) {
            final PrintQuery print = new PrintQuery(instance);
            print.addAttribute(CISales.PaymentCheck.Amount);
            print.execute();
            BigDecimal amountTotal = print.<BigDecimal>getAttribute(CISales.PaymentCheck.Amount);

            final QueryBuilder queryBldr = new QueryBuilder(CISales.Payment);
            queryBldr.addWhereAttrEqValue(CISales.Payment.TargetDocument, instance);
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CISales.Payment.Amount);
            multi.execute();
            while (multi.next()) {
                final BigDecimal amountPay = multi.<BigDecimal>getAttribute(CISales.Payment.Amount);
                amountTotal = amountTotal.subtract(amountPay);
            }
            final StringBuilder js = new StringBuilder();
            js.append("<script type=\"text/javascript\">\n")
                .append(getSetFieldValue(0, "amount", fmtr.format(amountTotal)))
                .append(getSetFieldValue(0, "restAmount", fmtr.format(amountTotal)))
                .append("</script>\n");

            ret.put(ReturnValues.SNIPLETT, js.toString());
        }

        return ret;
    }

    public Return getEmptyTable4PaymentDocWithOutDoc(final Parameter _parameter)
    {
        final List<Instance> lst = new ArrayList<Instance>();
        final Return ret = new Return();
        ret.put(ReturnValues.VALUES, lst);
        return ret;
    }

}
