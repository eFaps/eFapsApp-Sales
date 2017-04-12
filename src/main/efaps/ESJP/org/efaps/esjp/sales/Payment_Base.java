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

package org.efaps.esjp.sales;

import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.IUIValue;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.payment.AbstractPaymentDocument;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("32d6c7f5-a70c-4eea-a940-ab136952dfdc")
@EFapsApplication("eFapsApp-Sales")
public abstract class Payment_Base
    extends AbstractPaymentDocument
{

    /**
     * String used as key to store the Date int the Session.
     */
    public static final String OPENAMOUNT_SESSIONKEY = "eFaps_SalesPayment_OpenAmount_SessionKey";

    /**
     * Metho used to create a Payment.
     *
     * @param _parameter Parameter as passed from the eFpas API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        Instance docInst = null;
        if (_parameter.getInstance() != null) {
            docInst = _parameter.getInstance();
        } else if (_parameter.getParameterValue("createdocument") != null) {
            docInst = Instance.get(_parameter.getParameterValue("createdocument"));
        }

        if (docInst != null && docInst.isValid()) {
            final String dateStr = _parameter.getParameterValue("date");

            final String[] amounts = _parameter.getParameterValues("amount");
            final String[] currencies = _parameter.getParameterValues("currency");
            final String[] paymentType = _parameter.getParameterValues("paymentType");
            final String[] descriptions = _parameter.getParameterValues("description");
            final String[] accounts = _parameter.getParameterValues("account");

            for (int i = 0; i < amounts.length; i++) {
                final CreatedDoc createdDoc = new CreatedDoc();
                final String targetTypeId = paymentType[i];
                Instance targetDocInst = null;
                boolean out = false;
                if (targetTypeId != null && !targetTypeId.isEmpty()) {
                    final Instance baseInst = Currency.getBaseCurrency();
                    final Instance newInst = Instance.get(CIERP.Currency.getType(), currencies[i]);
                    final CurrencyInst currency = new CurrencyInst(newInst);
                    final PriceUtil util = new PriceUtil();
                    final BigDecimal[] rate = util.getExchangeRate(util.getDateFromParameter(_parameter), newInst);
                    final Object[] rateObj = new Object[] { currency.isInvert() ? BigDecimal.ONE : rate[1],
                                    currency.isInvert() ? rate[1] : BigDecimal.ONE };
                    final Type targetType = Type.get(Long.valueOf(targetTypeId));

                    out  = targetType.isKindOf(CISales.PaymentDocumentOutAbstract.getType());

                    final Status status = Status.find(targetType.getStatusAttribute().getLink().getUUID(), "Open");
                    final Insert targetDocIns = new Insert(targetType);
                    final PrintQuery docPrint = new PrintQuery(docInst);
                    docPrint.addAttribute(CIERP.DocumentAbstract.Contact, CIERP.DocumentAbstract.Name);
                    docPrint.execute();
                    targetDocIns.add(CISales.PaymentDocumentAbstract.StatusAbstract, status.getId());
                    targetDocIns.add(CISales.PaymentDocumentAbstract.Contact,
                                    docPrint.<Long>getAttribute(CIERP.DocumentAbstract.Contact));
                    targetDocIns.add(CISales.PaymentDocumentAbstract.Name,
                                    docPrint.<String>getAttribute(CIERP.DocumentAbstract.Name));
                    final String code = getCode4CreateDoc(_parameter);
                    if (code != null) {
                        targetDocIns.add(CISales.PaymentDocumentOutAbstract.Code, code);
                    }
                    targetDocIns.add(CISales.PaymentDocumentAbstract.Date, dateStr);
                    targetDocIns.add(CISales.PaymentDocumentAbstract.CurrencyLink, baseInst.getId());
                    targetDocIns.add(CISales.PaymentDocumentAbstract.Amount, amounts[i]);
                    targetDocIns.add(CISales.PaymentDocumentAbstract.RateCurrencyLink, currencies[i]);
                    targetDocIns.add(CISales.PaymentDocumentAbstract.Rate, rateObj);

                    createdDoc.getValues().put(getFieldName4Attribute(_parameter,
                                    CISales.PaymentDocumentOutAbstract.Name.name),
                                    docPrint.<String>getAttribute(CIERP.DocumentAbstract.Name));
                    createdDoc.getValues().put(getFieldName4Attribute(_parameter,
                                    CISales.PaymentDocumentOutAbstract.Contact.name),
                                    docPrint.<Long>getAttribute(CIERP.DocumentAbstract.Contact));
                    createdDoc.getValues().put(getFieldName4Attribute(_parameter,
                                    CISales.PaymentDocumentOutAbstract.Code.name), code);
                    createdDoc.getValues().put(getFieldName4Attribute(_parameter,
                                    CISales.PaymentDocumentOutAbstract.Rate.name), rateObj);
                    createdDoc.getValues().put(getFieldName4Attribute(_parameter,
                                    CISales.PaymentDocumentOutAbstract.CurrencyLink.name), baseInst.getId());
                    createdDoc.getValues().put(getFieldName4Attribute(_parameter,
                                    CISales.PaymentDocumentOutAbstract.Date.name), dateStr);
                    createdDoc.getValues().put(getFieldName4Attribute(_parameter,
                                    CISales.PaymentDocumentOutAbstract.RateCurrencyLink.name), currencies[i]);
                    createdDoc.getValues().put(getFieldName4Attribute(_parameter,
                                    CISales.PaymentDocumentOutAbstract.Amount.name), amounts[i]);
                    targetDocIns.execute();
                    targetDocInst = targetDocIns.getInstance();
                    createdDoc.setInstance(targetDocInst);
                }

                final Insert insert = new Insert(CISales.Payment);
                insert.add(CISales.Payment.Date, dateStr);
                insert.add(CISales.Payment.CreateDocument, docInst.getId());
                if (targetDocInst != null) {
                    insert.add(CISales.Payment.TargetDocument, targetDocInst.getId());
                }
                insert.add(CISales.Payment.CurrencyLink, currencies[i]);
                insert.add(CISales.Payment.Amount, amounts[i]);
                //TODO
                insert.add(CISales.Payment.RateCurrencyLink, currencies[i]);
                insert.add(CISales.Payment.Rate, new Object[]{1, 1});
                insert.execute();
                final Instance paymentInst = insert.getInstance();

                final CIType transType;
                if (out) {
                    transType = CISales.TransactionOutbound;
                } else {
                    transType = CISales.TransactionInbound;
                }
                final Insert transIns = new Insert(transType);
                transIns.add(CISales.TransactionAbstract.Amount, amounts[i]);
                transIns.add(CISales.TransactionAbstract.CurrencyId, currencies[i]);
                transIns.add(CISales.TransactionAbstract.Payment,  paymentInst);
                transIns.add(CISales.TransactionAbstract.Description, descriptions[i].length() < 1
                                ? DBProperties.getProperty("Sales_Payment.Label")
                                : descriptions[i]);
                transIns.add(CISales.TransactionAbstract.Date, dateStr);
                transIns.add(CISales.TransactionAbstract.Account, accounts[i]);
                createdDoc.getValues().put(getFieldName4Attribute(_parameter, CISales.TransactionAbstract.Account.name),
                                accounts[i]);
                transIns.execute();

                final File file = createReport(_parameter, createdDoc);
                if (file != null) {
                    ret.put(ReturnValues.VALUES, file);
                    ret.put(ReturnValues.TRUE, true);
                }
            }
        }
        return ret;
    }

    /**
     * \ Method is used to set the Value for the open amount field on opening
     * the form.
     *
     * @param _parameter Parameter as passed from the efaps API
     * @return Snipplets with value for open amount field
     * @throws EFapsException on error
     */
    public Return getOpenAmountUI(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        Instance instance = null;
        if (_parameter.getCallInstance() != null) {
            instance = _parameter.getInstance();
        } else if (_parameter.getParameterValue("createdocument") != null) {
            instance = Instance.get(_parameter.getParameterValue("createdocument"));
        }
        final IUIValue fieldValue = (IUIValue) _parameter.get(ParameterValues.UIOBJECT);

        if (instance != null && instance.isValid()) {
            final OpenAmount openAmount = getOpenAmount(_parameter, instance);

            Context.getThreadContext().setSessionAttribute(Payment_Base.OPENAMOUNT_SESSIONKEY, openAmount);
            final StringBuilder html = new StringBuilder();
            final DecimalFormat formater = NumberFormatter.get().getFormatter();
            html.append(formater.format(openAmount.getCrossTotal())).append(" ")
                            .append(openAmount.getSymbol()).append("<hr/>")
                            .append("<span name=\"").append(fieldValue.getField().getName()).append("_table\">")
                            .append(getOpenAmountInnerHtml(_parameter, openAmount))
                            .append("</span>");

            ret.put(ReturnValues.SNIPLETT, html.toString());
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API
     * @param _openAmount open Amount
     * @return StringBuilder for html snipplet
     * @throws EFapsException on error
     */
    protected StringBuilder getOpenAmountInnerHtml(final Parameter _parameter,
                                                   final OpenAmount _openAmount)
        throws EFapsException
    {
        final StringBuilder html = new StringBuilder();

        // Sales-Configuration
        final Instance baseCurrInst = Currency.getBaseCurrency();
        final DecimalFormat formater = NumberFormatter.get().getFormatter();
        final PriceUtil priceUtil = new PriceUtil();

        final QueryBuilder queryBldr = new QueryBuilder(CIERP.Currency);
        final InstanceQuery query = queryBldr.getQuery();
        while (query.next()) {
            // only add other currencies to the list
            if (!_openAmount.getCurrencyInstance().equals(query.getCurrentValue())) {
                // if the base is part of the list put it on first place
                final BigDecimal[] rates = priceUtil.getRates(_parameter, query.getCurrentValue(),
                                _openAmount.getCurrencyInstance());

                final CurrencyInst currInst = new CurrencyInst(query.getCurrentValue());
                final StringBuilder row = new StringBuilder();
                row.append("<tr><td>").append(formater.format(_openAmount.getCrossTotal().multiply(rates[2])))
                                .append(" ").append(currInst.getSymbol())
                                .append("</td><td>").append(rates[2]).append("</td></tr>");
                if (query.getCurrentValue().equals(baseCurrInst)) {
                    html.insert(0, row);
                } else {
                    html.append(row);
                }
            }
        }
        html.insert(0, "<table>");
        html.append("</table>");
        return html;
    }

    /**
     * Gets the open amount.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _instance the instance
     * @return the open amount
     * @throws EFapsException on error
     */
    public OpenAmount getOpenAmount(final Parameter _parameter,
                                    final Instance _instance)
        throws EFapsException
    {
        return getOpenAmount(_parameter, _instance, true);
    }

    /**
     * Method to calculate the open amount for an instance.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _instance instance the open amount is wanted for
     * @param _accessCheck the access check
     * @return open amount
     * @throws EFapsException on error
     */
    public OpenAmount getOpenAmount(final Parameter _parameter,
                                    final Instance _instance,
                                    final boolean _accessCheck)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_instance);
        print.addAttribute(CISales.DocumentSumAbstract.RateCrossTotal,
                        CISales.DocumentSumAbstract.RateCurrencyId,
                        CISales.DocumentSumAbstract.Date);
        final SelectBuilder sel = new SelectBuilder().linkto(CISales.DocumentSumAbstract.RateCurrencyId)
                        .attribute(CIERP.Currency.Symbol);
        print.addSelect(sel);
        if (_accessCheck) {
            print.execute();
        } else {
            print.executeWithoutAccessCheck();
        }
        final BigDecimal ratecrossTotal = print.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
        print.<String>getSelect(sel);

        final Instance rateCurrInst = Instance.get(CIERP.Currency.getType(),
                        print.<Long>getAttribute(CISales.DocumentSumAbstract.RateCurrencyId));
        final DateTime rateDate = print.<DateTime>getAttribute(CISales.DocumentSumAbstract.Date);

        final QueryBuilder queryBldr = new QueryBuilder(CISales.Payment);
        queryBldr.addWhereAttrEqValue(CISales.Payment.CreateDocument, _instance.getId());
        final InstanceQuery query = queryBldr.getQuery();
        if (_accessCheck) {
            query.execute();
        } else {
            query.executeWithoutAccessCheck();
        }
        final List<Instance> instances = query.getValues();
        BigDecimal paid = BigDecimal.ZERO;

        if (instances.size() > 0) {
            final SelectBuilder amountSel = new SelectBuilder()
                            .linkfrom(CISales.TransactionAbstract, CISales.TransactionAbstract.Payment)
                            .attribute(CISales.TransactionAbstract.Amount);
            final SelectBuilder currIdSel = new SelectBuilder()
                            .linkfrom(CISales.TransactionAbstract, CISales.TransactionAbstract.Payment)
                            .attribute(CISales.TransactionAbstract.CurrencyId);
            final SelectBuilder oidSel = new SelectBuilder()
                            .linkfrom(CISales.TransactionAbstract, CISales.TransactionAbstract.Payment)
                            .oid();

            final PriceUtil priceUtil = new PriceUtil();
            final MultiPrintQuery multi = new MultiPrintQuery(instances);
            multi.addSelect(amountSel, currIdSel, oidSel);
            if (_accessCheck) {
                multi.execute();
            } else {
                multi.executeWithoutAccessCheck();
            }
            final Set<String> oids = new HashSet<>();
            while (multi.next()) {
                if (multi.isList4Select(amountSel.toString())) {
                    final List<BigDecimal> list = multi.<List<BigDecimal>>getSelect(amountSel);
                    final List<Long> currList = multi.<List<Long>>getSelect(currIdSel);
                    final List<String> oidList = multi.<List<String>>getSelect(oidSel);

                    final Iterator<Long> currIter = currList.iterator();
                    final Iterator<String> oidIter = oidList.iterator();
                    for (final BigDecimal pos : list) {
                        final BigDecimal temp;
                        final Long currId = currIter.next();
                        final String oid = oidIter.next();
                        if (!oids.contains(oid)) {
                            oids.add(oid);
                            if (rateCurrInst.getId() == currId) {
                                temp = pos;
                            } else {
                                final BigDecimal[] rates = priceUtil.getRates(_parameter,
                                                rateCurrInst,
                                                Instance.get(CIERP.Currency.getType(), currId.toString()));
                                temp = pos.multiply(rates[2]);
                            }
                            paid = paid.add(temp);
                        }
                    }
                } else {
                    final BigDecimal temp = multi.<BigDecimal>getSelect(amountSel);
                    final Long currId = multi.<Long>getSelect(currIdSel);
                    final String oid = multi.<String>getSelect(oidSel);

                    if (temp != null && !oids.contains(oid)) {
                        oids.add(oid);
                        if (rateCurrInst.getId() == currId) {
                            paid = paid.add(temp);
                        } else {
                            final BigDecimal[] rates = priceUtil.getRates(_parameter,
                                            rateCurrInst,
                                            Instance.get(CIERP.Currency.getType(), currId.toString()));
                            paid = paid.add(temp.multiply(rates[2]));
                        }
                    }
                }
            }
        }
        return new OpenAmount(new CurrencyInst(rateCurrInst), ratecrossTotal.subtract(paid), rateDate);
    }

    /**
     * Method is called as an update event on changes on one of the amount
     * fields.
     *
     * @param _parameter Parameter as passed from the efaps API
     * @return List of maps of the fields to be updated
     * @throws EFapsException on error
     */
    public Return updateFields4Amount(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, getListMap(_parameter, false));
        return retVal;
    }

    /**
     * Get the list of maps for the field to be updated.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _updateRate mus the field with the rate also be updated
     * @return List of maps of the fields to be updated
     * @throws EFapsException on error
     */
    protected List<Map<String, String>> getListMap(final Parameter _parameter,
                                                   final boolean _updateRate)
        throws EFapsException
    {
        final List<Map<String, String>> list = new ArrayList<>();
        final Map<String, String> map = new HashMap<>();
        final String[] amounts = _parameter.getParameterValues("amount");
        final String[] currencies = _parameter.getParameterValues("currency");

        final DecimalFormat formatter = NumberFormatter.get().getTwoDigitsFormatter();

        final Map<String, BigDecimal> curr2Rate = new HashMap<>();
        final Map<String, String> curr2Symb = new HashMap<>();

        final OpenAmount openAmount = (OpenAmount) Context.getThreadContext().getSessionAttribute(
                        Payment_Base.OPENAMOUNT_SESSIONKEY);
        BigDecimal openTotalAmount = openAmount.getCrossTotal();
        final PriceUtil priceUtil = new PriceUtil();
        for (int i = 0; i < amounts.length; i++) {
            final BigDecimal amount;
            try {
                amount = amounts[i].length() < 1
                                ? BigDecimal.ZERO.setScale(2) : (BigDecimal) formatter.parse(amounts[i]);
            } catch (final ParseException e) {
                throw new EFapsException(Payment_Base.class, "getListMap.ParseException", e);
            }

            final BigDecimal rate;
            if (curr2Rate.containsKey(currencies[i])) {
                rate = curr2Rate.get(currencies[i]);
            } else {
                final Instance currInst = Instance.get(CIERP.Currency.getType(), currencies[i]);
                final BigDecimal[] rates = priceUtil.getRates(_parameter, openAmount.getCurrencyInstance(), currInst);
                rate = rates[2];
                curr2Rate.put(currencies[i], rate);
                curr2Symb.put(currencies[i], new CurrencyInst(currInst).getSymbol());
            }
            openTotalAmount = openTotalAmount.subtract(amount.multiply(rate));
        }
        final StringBuilder value = new StringBuilder();

        for (final Entry<String, BigDecimal> entry : curr2Rate.entrySet()) {
            if (value.length() > 0) {
                value.append(" - ");
            }
            value.append(formatter.format(openTotalAmount.divide(entry.getValue(), BigDecimal.ROUND_HALF_UP)))
                            .append(" ").append(curr2Symb.get(entry.getKey()));
        }
        map.put("openAmountTotal", value.toString());
        // if the date was changed the rate and the original value must be
        // changed
        if (_updateRate) {
            final StringBuilder js = new StringBuilder();
            js.append("document.getElementsByName(\"openAmount_table\")[0].innerHTML=\"")
                            .append(getOpenAmountInnerHtml(_parameter, openAmount)).append("\"");
            map.put("eFapsFieldUpdateJS", js.toString());
        }

        list.add(map);
        return list;
    }

    /**
     * Method is called as an update event on changes in the date fields.
     *
     * @param _parameter Parameter as passed from the efaps API
     * @return List of maps of the fields to be updated
     * @throws EFapsException on error
     */
    public Return updateFields4Date(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, getListMap(_parameter, true));
        return retVal;
    }

    @Override
    public Return picker4Document(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final Map<String, String> map = new HashMap<>();
        retVal.put(ReturnValues.VALUES, map);

        final Instance viewOid = Instance.get(_parameter.getParameterValue("selectedRow"));
        if (viewOid.isValid()) {
            final PrintQuery print = new PrintQuery(viewOid);
            print.addAttribute(CISales.DocumentAbstract.Name);
            if (print.execute()) {
                final String name = print.<String>getAttribute(CISales.DocumentAbstract.Name);
                map.put(EFapsKey.PICKER_VALUE.getKey(), name);
                map.put(EFapsKey.PICKER_JAVASCRIPT.getKey(), getOpenAmount4Picker(_parameter));
                map.put("createdocument", viewOid.getOid());
            }
        }
        return retVal;
    }

    /**
     * Gets the open amount4 picker.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the open amount4 picker
     * @throws EFapsException on error
     */
    protected String getOpenAmount4Picker(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instance = Instance.get(_parameter.getParameterValue("selectedRow"));
        final StringBuilder html = new StringBuilder();

        if (instance != null && instance.isValid()) {
            final OpenAmount openAmount = getOpenAmount(_parameter, instance);

            Context.getThreadContext().setSessionAttribute(Payment_Base.OPENAMOUNT_SESSIONKEY, openAmount);
            final DecimalFormat formater = NumberFormatter.get().getFormatter();
            html.append("document.getElementsByName('openAmount')[0].innerHTML='")
                    .append(formater.format(openAmount.getCrossTotal())).append(" ")
                    .append(openAmount.getSymbol()).append("<hr/>")
                    .append("<span name=\"").append("openAmount").append("_table\">")
                    .append(getOpenAmountInnerHtml(_parameter, openAmount))
                    .append("</span>").append("';");
        }
        return html.toString();
    }

    /**
     * Auto complete4 document.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     */
    public Return autoComplete4Document(final Parameter _parameter)
    {
        final Return ret = new Return();
        return ret;
    }

    /**
     * Multi print4 payment picker.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return multiPrint4PaymentPicker(final Parameter _parameter)
        throws EFapsException
    {
        return new MultiPrint() {
            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _bldr)
                throws EFapsException
            {
                final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
                final String statusStr = (String) properties.get("Status");
                final String typesStr = (String) properties.get("Types");
                final List<Long> idStatus = new ArrayList<>();
                final String[] status = statusStr.split(";");
                final String[] types = typesStr.split(";");
                for (final String st : status) {
                    for (final String type : types) {
                        idStatus.add(Status.find(Type.get(type).getStatusAttribute().getLink().getUUID(), st).getId());
                    }
                }
                _bldr.addWhereAttrEqValue(CISales.DocumentAbstract.StatusAbstract, idStatus.toArray());
            }
        }.execute(_parameter);
    }

    /**
     * Represents on open amount.
     */
    public class OpenAmount
        implements Serializable
    {
        /**
         * Needed for serialization.
         */
        private static final long serialVersionUID = 1L;

        /**
         * CurrencyInst of this OpenAmount.
         */
        private final CurrencyInst currencyInstance;

        /**
         * Cross Total.
         */
        private final BigDecimal crossTotal;

        /**
         * Date of this OpenAmount.
         */
        private final DateTime date;

        /**
         * @param _currencyInstance Currency Instance
         * @param _crossTotal Cross Total
         * @param _rateDate date of the rate
         */
        public OpenAmount(final CurrencyInst _currencyInstance,
                          final BigDecimal _crossTotal,
                          final DateTime _rateDate)
        {
            this.currencyInstance = _currencyInstance;
            this.crossTotal = _crossTotal;
            this.date = _rateDate;
        }

        /**
         * Getter method for the instance variable {@link #symbol}.
         *
         * @return value of instance variable {@link #symbol}
         * @throws EFapsException on error
         */
        public String getSymbol()
            throws EFapsException
        {
            return this.currencyInstance.getSymbol();
        }

        /**
         * Getter method for the instance variable {@link #currencyInstance}.
         *
         * @return value of instance variable {@link #currencyInstance}
         */
        public Instance getCurrencyInstance()
        {
            return this.currencyInstance.getInstance();
        }

        /**
         * Getter method for the instance variable {@link #crossTotal}.
         *
         * @return value of instance variable {@link #crossTotal}
         */
        public BigDecimal getCrossTotal()
        {
            return this.crossTotal;
        }

        /**
         * Getter method for the instance variable {@link #date}.
         *
         * @return value of instance variable {@link #date}
         */
        public DateTime getDate()
        {
            return this.date;
        }

    }
}
