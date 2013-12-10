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
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.field.Field;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.PriceUtil;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.SalesSettings;
import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("35abd734-d033-460f-bb00-2f40e3a58320")
@EFapsRevision("$Rev$")
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
            final Instance baseInst = Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);

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

    public Return checkbox4InvoiceFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);

        final PrintQuery print = new PrintQuery(_parameter.getInstance());
        final SelectBuilder selContInst = new SelectBuilder().linkto(CISales.PaymentCheck.Contact).instance();
        print.addSelect(selContInst);
        print.execute();
        final Instance contInst = print.<Instance>getSelect(selContInst);

        final boolean checked = "true".equalsIgnoreCase((String) props.get("checked"));
        if (!checked) {
            if (contInst.isValid()) {
                Context.getThreadContext().setSessionAttribute(AbstractPaymentDocument_Base.INVOICE_SESSIONKEY,
                                contInst);
            } else {
                Context.getThreadContext().removeSessionAttribute(AbstractPaymentDocument_Base.INVOICE_SESSIONKEY);
            }
        } else {
            Context.getThreadContext().removeSessionAttribute(AbstractPaymentDocument_Base.INVOICE_SESSIONKEY);
        }
        Context.getThreadContext().setSessionAttribute(AbstractPaymentDocument_Base.CONTACT_SESSIONKEY, contInst);
        return new org.efaps.esjp.common.uiform.Field().checkboxFieldValue(_parameter);
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

    @Override
    public DocumentInfo getNewDocumentInfo(final Instance _instance)
        throws EFapsException
    {
        return new DocumentInfoIn(_instance);
    }

    public class DocumentInfoIn
        extends AbstractPaymentDocument.DocumentInfo
    {

        public DocumentInfoIn(final Instance _instance)
            throws EFapsException
        {
            super(_instance);
        }

        @Override
        protected BigDecimal getRateCrossTotal4Query()
            throws EFapsException
        {
            BigDecimal ret = BigDecimal.ZERO;

            if (getInstance().isValid() && getInstance().getType().equals(CISales.Invoice.getType())) {
                final InstanceQuery query = getPaymentDerivedDocument();
                query.execute();
                while (query.next()) {
                    if (query.getCurrentValue().getType().equals(CISales.CreditNote.getType())) {
                        ret = ret.add(compareDocs(query.getCurrentValue()).negate());
                    } else if (query.getCurrentValue().getType().equals(CISales.Reminder.getType())) {
                        ret = ret.add(compareDocs(query.getCurrentValue()));
                    }
                }
            }

            return ret.setScale(2, BigDecimal.ROUND_HALF_UP);
        }
    }
}
