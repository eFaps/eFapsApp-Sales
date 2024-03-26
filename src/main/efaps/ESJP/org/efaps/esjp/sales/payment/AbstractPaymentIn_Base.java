/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
 */
package org.efaps.esjp.sales.payment;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.efaps.admin.datamodel.ui.RateUI;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.field.Field;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.PriceUtil;
import org.efaps.esjp.sales.util.Sales;
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
            final List<Map<String, String>> list = new ArrayList<>();
            final Map<String, String> map = new HashMap<>();

            final Instance newInst = Instance.get(CIERP.Currency.getType(), _parameter.getParameterValue(
                            CIFormSales.Sales_PaymentDepositWithOutDocForm.currencyLink.name));
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

    /**
     * Gets the payment out documents that need to be settled
     * (the user is still accountable for them).
     *
     * @param _parameter the _parameter
     * @return the payment documents4 pay
     * @throws EFapsException the e faps exception
     */
    public Return getPaymentDocuments4ToBeSettled(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new MultiPrint()
        {

            @Override
            public List<Instance> getInstances(final Parameter _parameter)
                throws EFapsException
            {
                final List<Instance> ret = new ArrayList<>();
                final QueryBuilder targetAttrQueryBldr1 = getQueryBldrFromProperties(_parameter,
                                Sales.PAYMENTDOCUMENT_TOBESETTLED.get());
                final QueryBuilder attrQueryBldr1 = new QueryBuilder(CISales.Payment);
                attrQueryBldr1.addWhereAttrInQuery(CISales.Payment.CreateDocument, targetAttrQueryBldr1
                                .getAttributeQuery(CISales.DocumentAbstract.ID));

                final QueryBuilder queryBldr1 = new QueryBuilder(CISales.PaymentDocumentAbstract);
                queryBldr1.addWhereAttrInQuery(CISales.PaymentDocumentAbstract.ID, attrQueryBldr1.getAttributeQuery(
                                CISales.Payment.TargetDocument));
                ret.addAll(queryBldr1.getQuery().execute());

                final QueryBuilder targetAttrQueryBldr = getQueryBldrFromProperties(_parameter,
                                Sales.PAYMENTDOCUMENT_TOBESETTLED.get(), "Tag");
                if (targetAttrQueryBldr != null) {
                    final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Payment);
                    attrQueryBldr.addWhereAttrInQuery(CISales.Payment.CreateDocument, targetAttrQueryBldr
                                    .getAttributeQuery(CIERP.Tag4DocumentAbstract.DocumentAbstractLink));

                    final QueryBuilder queryBldr = new QueryBuilder(CISales.PaymentDocumentAbstract);
                    queryBldr.addWhereAttrInQuery(CISales.PaymentDocumentAbstract.ID, attrQueryBldr.getAttributeQuery(
                                    CISales.Payment.TargetDocument));

                    ret.addAll(queryBldr.getQuery().execute());
                }
                return ret;
            }
        }.execute(_parameter);
        return ret;
    }

    /**
     * Accesscheck that checks if documents a related that must be settled.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return check4ToBeSettled(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Instance paymentDoc = _parameter.getInstance();
        if (paymentDoc.isValid()) {
            final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Payment);
            attrQueryBldr.addWhereAttrEqValue(CISales.Payment.TargetDocument, paymentDoc);
            final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.Payment.CreateDocument);

            final Properties props = Sales.PAYMENTDOCUMENT_TOBESETTLED.get();

            final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter, props, "Electable");
            if (queryBldr != null) {
                queryBldr.addWhereAttrInQuery(CISales.DocumentSumAbstract.ID, attrQuery);

                final InstanceQuery query = queryBldr.getQuery();
                query.execute();
                if (!query.getValues().isEmpty()) {
                    ret.put(ReturnValues.TRUE, true);
                }
            }
        }
        return ret;
    }

    /**
     * Drop down4 create documents.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return currentDocument4SettleFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new org.efaps.esjp.common.uiform.Field()
        {

            @Override
            protected void add2QueryBuilder4List(final Parameter _parameter,
                                                 final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Payment);
                attrQueryBldr.addWhereAttrEqValue(CISales.Payment.TargetDocument, _parameter.getCallInstance());

                final Properties props = Sales.PAYMENTDOCUMENT_TOBESETTLED.get();
                final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter, props, "Electable");
                _queryBldr.addWhereAttrInQuery(CISales.DocumentSumAbstract.ID, queryBldr.getAttributeQuery(
                                CISales.DocumentSumAbstract.ID));
                _queryBldr.addWhereAttrInQuery(CISales.DocumentSumAbstract.ID, attrQueryBldr.getAttributeQuery(
                                CISales.Payment.CreateDocument));

            }
        }.getOptionListFieldValue(_parameter);
        return ret;
    }

    /**
     * Gets the java script for amount add doc.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the java script for amount add doc
     * @throws EFapsException on error
     */
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

    /**
     * Gets the empty table for payment doc with out doc.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the empty table for payment doc with out doc
     */
    public Return getEmptyTable4PaymentDocWithOutDoc(final Parameter _parameter)
    {
        final List<Instance> lst = new ArrayList<>();
        final Return ret = new Return();
        ret.put(ReturnValues.VALUES, lst);
        return ret;
    }

    /**
     * Settle payment.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return settlePayment(final Parameter _parameter)
        throws EFapsException
    {
        new Settlement(false).settlePayment(_parameter);
        final Return ret = new Return();
        return ret;
    }

    /**
     * Update fields for settle document.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return updateFields4SettleDocument(final Parameter _parameter)
        throws EFapsException
    {
        return new Settlement(false).updateFields4SettleDocument(_parameter);
    }

    /**
     * Validate payments.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return validateSettlePayment(final Parameter _parameter)
        throws EFapsException
    {
        return new Settlement(false).validateSettlePayment(_parameter);
    }
}
