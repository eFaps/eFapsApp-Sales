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

package org.efaps.esjp.sales.document;


import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.PrintQuery;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("fefeba99-3218-4d05-b03b-db0b02445d38")
@EFapsApplication("eFapsApp-Sales")
public abstract class Credit_Base
    extends AbstractSumOnlyDocument
{

    /**
     * Method for create a new Credit.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final CreatedDoc createdDoc = createDoc(_parameter);
        connect2Derived(_parameter, createdDoc);
        connect2Object(_parameter, createdDoc);

        final File file = createReport(_parameter, createdDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        ret.put(ReturnValues.INSTANCE, createdDoc.getInstance());
        return ret;
    }

    /**
     * Edit.
     *
     * @param _parameter Parameter from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final EditedDoc editDoc = editDoc(_parameter);
        updateConnection2Object(_parameter, editDoc);

        final File file = createReport(_parameter, editDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    @Override
    public CIType getCIType()
        throws EFapsException
    {
        return CISales.Credit;
    }

    /**
     * Method for update field contact or employee.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return updateFields4Credit(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final StringBuilder js = new StringBuilder();
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> values = new TreeMap<String, String>();
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String val = (String) properties.get("ValidationType");
        if (val != null && val.equals("Employee")) {
            js.append(getSetFieldValue(0, "contactAutoComplete", "")).append(getSetFieldValue(0, "contact", "")).append(
                            getSetFieldValue(0, "contactData", ""));
        } else if (val != null && val.equals("Contact")) {
            js.append(getSetFieldValue(0, "employee", "")).append(getSetFieldValue(0, "employeeAutoComplete", ""));
        }
        values.put(EFapsKey.FIELDUPDATE_JAVASCRIPT.getKey(), js.toString());
        list.add(values);
        ret.put(ReturnValues.VALUES, list);
        return ret;
    }

    /**
     * Creates a number of installments automatically.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return createInstallments(final Parameter _parameter)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_parameter.getCallInstance());
        final SelectBuilder selContactOID = SelectBuilder.get().linkto(CISales.Credit.Contact).oid();
        print.addSelect(selContactOID);
        print.addAttribute(CISales.Credit.RateCrossTotal, CISales.Credit.Rate, CISales.Credit.RateCurrencyId);
        print.execute();
        final Object[] rateObject = print.getAttribute(CISales.Credit.Rate);
        final BigDecimal rateCrossTotal = print.getAttribute(CISales.Credit.RateCrossTotal);

        final int quantity = Integer.parseInt(_parameter.getParameterValue(
                        CIFormSales.Sales_Credit_CreateInstallmentsForm.quantity.name));
        final DateTime dueDate = ISODateTimeFormat.dateTime().parseDateTime(_parameter.getParameterValue(
                        CIFormSales.Sales_Credit_CreateInstallmentsForm.dueDate.name));

        final BigDecimal installAmmount = rateCrossTotal.setScale(8, RoundingMode.HALF_UP)
                        .divide(new BigDecimal(quantity), RoundingMode.HALF_UP);
        final DecimalFormat fomatter = NumberFormatter.get().getFrmt4Total(CISales.Installment.getType().getName());
        final String installAmountStr = fomatter.format(installAmmount);

        final Parameter parameter = ParameterUtil.clone(_parameter);
        ParameterUtil.setParmeterValue(parameter, "contact", print.<String>getSelect(selContactOID));
        ParameterUtil.setParmeterValue(parameter, "rateCurrencyId",
                        String.valueOf(print.<Long>getAttribute(CISales.Credit.RateCurrencyId)));

        for (int i = 0; i < quantity; i++) {
            ParameterUtil.setParmeterValue(parameter, "dueDate", dueDate.plusMonths(i).toString());
            ParameterUtil.setParmeterValue(parameter, "revision", String.valueOf(i + 1));
            ParameterUtil.setParmeterValue(parameter, "rateCrossTotal", installAmountStr);
            new Installment()
            {
                @Override
                protected Object[] getRateObject(final Parameter _parameter)
                    throws EFapsException
                {
                    return rateObject;
                };
            }.create(parameter);
        }
        return new Return();
    }
}
