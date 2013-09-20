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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.RateUI;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIAttribute;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Checkin;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.admin.datamodel.StatusValue;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.StandartReport;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.esjp.erp.util.ERPSettings;
import org.efaps.esjp.sales.Calculator_Base;
import org.efaps.esjp.sales.PriceUtil;
import org.efaps.esjp.sales.document.AbstractDocument_Base;
import org.efaps.esjp.sales.document.Invoice;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.SalesSettings;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: Payment_Base.java 7671 2012-06-14 17:25:53Z
 *          jorge.cueva@moxter.net $
 */
@EFapsUUID("c7281e33-540f-4db1-bcc6-38e89528883f")
@EFapsRevision("$Rev$")
public abstract class AbstractPaymentDocument_Base
    extends CommonDocument
{

    public static final String INVOICE_SESSIONKEY = "eFaps_Selected_Sales_Invoice";

    public static final String CONTACT_SESSIONKEY = "eFaps_Selected_Contact";

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return CreatedDoc instance
     * @throws EFapsException on error
     */
    protected CreatedDoc createDoc(final Parameter _parameter)
        throws EFapsException
    {
        final Insert insert = new Insert(getType4DocCreate(_parameter));
        final CreatedDoc createdDoc = new CreatedDoc();

        final String name = getDocName4Create(_parameter);
        if (name != null) {
            insert.add(CISales.PaymentDocumentAbstract.Name, name);
            createdDoc.getValues().put(getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.Name.name),
                            name);
        }

        final String note = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.PaymentDocumentAbstract.Note.name));
        if (note != null) {
            insert.add(CISales.PaymentDocumentAbstract.Note, note);
            createdDoc.getValues().put(getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.Note.name),
                            note);
        }

        final String amount = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.PaymentDocumentAbstract.Amount.name));
        if (amount != null) {
            insert.add(CISales.PaymentDocumentAbstract.Amount, amount);
            createdDoc.getValues().put(getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.Amount.name),
                            amount);
        }

        final String date = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.PaymentDocumentAbstract.Date.name));
        if (date != null) {
            insert.add(CISales.PaymentDocumentAbstract.Date, date);
            createdDoc.getValues().put(getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.Date.name),
                            date);
        }

        final String dueDate = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.PaymentDocumentAbstract.DueDate.name));
        if (dueDate != null) {
            insert.add(CISales.PaymentDocumentAbstract.DueDate, dueDate);
            createdDoc.getValues().put(getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.DueDate.name),
                            dueDate);
        }

        final String revision = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.PaymentDocumentAbstract.Revision.name));
        if (revision != null) {
            insert.add(CISales.PaymentDocumentAbstract.Revision, revision);
            createdDoc.getValues().put(getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.Revision.name),
                            revision);
        }

        final String currencyLink = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.PaymentDocumentAbstract.CurrencyLink.name));
        if (currencyLink != null) {
            insert.add(CISales.PaymentDocumentAbstract.RateCurrencyLink, currencyLink);
            createdDoc.getValues().put(
                            getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.RateCurrencyLink.name),
                            currencyLink);
            final Instance baseCurrInst = Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);
            insert.add(CISales.PaymentDocumentAbstract.CurrencyLink, baseCurrInst.getId());
            createdDoc.getValues().put(
                            getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.CurrencyLink.name),
                            baseCurrInst.getId());
        }

        final String currencyLink4Account = getRateCurrencyLink4Account(_parameter);
        if (currencyLink4Account != null) {
            insert.add(CISales.PaymentDocumentAbstract.RateCurrencyLink, currencyLink4Account);
            createdDoc.getValues().put(
                            getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.RateCurrencyLink.name),
                            currencyLink4Account);
            final Instance baseCurrInst = Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);
            insert.add(CISales.PaymentDocumentAbstract.CurrencyLink, baseCurrInst.getId());
            createdDoc.getValues().put(
                            getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.CurrencyLink.name),
                            baseCurrInst.getId());
        }

        final String contact = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.PaymentDocumentAbstract.Contact.name));
        if (contact != null && Instance.get(contact).isValid()) {
            insert.add(CISales.PaymentDocumentAbstract.Contact, Instance.get(contact).getId());
            createdDoc.getValues().put(
                            getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.Contact.name),
                            Instance.get(contact).getId());
        }

        final Object rateObj = _parameter.getParameterValue("rate");
        if (rateObj != null) {
            insert.add(CISales.PaymentDocumentAbstract.Rate, getRateObject(_parameter));
            createdDoc.getValues().put(
                            getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.Rate.name), getRateObject(_parameter));
        }

        final String code = getCode4GeneratedDocWithSysConfig(_parameter);
        if (code != null) {
            insert.add(CISales.PaymentDocumentAbstract.Code, code);
            createdDoc.getValues().put(
                            getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.Code.name), code);
        }

        addStatus2DocCreate(_parameter, insert, createdDoc);
        add2DocCreate(_parameter, insert, createdDoc);
        insert.execute();

        createdDoc.setInstance(insert.getInstance());

        return createdDoc;
    }

    protected String getRateCurrencyLink4Account(final Parameter _parameter)
        throws EFapsException
    {
        long currencyId = 0;
        final String account = _parameter.getParameterValue("account");
        if (account != null) {
            final PrintQuery print = new PrintQuery(CISales.AccountCashDesk.getType(), account);
            print.addAttribute(CISales.AccountCashDesk.CurrencyLink);
            print.execute();
            currencyId = print.<Long>getAttribute(CISales.AccountCashDesk.CurrencyLink);
        }
        return currencyId == 0 ? null : String.valueOf(currencyId);
    }

    /**
     * @param _parameter
     * @param _createdDoc
     */
    protected void createPayment(final Parameter _parameter,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final String[] createDocument = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                        CISales.Payment.CreateDocument.name));
        final String[] paymentAmount = _parameter.getParameterValues("paymentAmount");

        for (int i = 0; i < getPaymentCount(_parameter); i++) {

            final Insert payInsert = new Insert(getPaymentType(_parameter, _createdDoc));
            Insert transIns;
            if ((getType4DocCreate(_parameter) != null
                        && getType4DocCreate(_parameter).isKindOf(CISales.PaymentDocumentAbstract.getType()))
                    || (_parameter.getInstance() != null
                        && _parameter.getInstance().isValid()
                        && _parameter.getInstance().getType().isKindOf(CISales.PaymentDocumentAbstract.getType()))) {
                transIns = new Insert(CISales.TransactionInbound);
            } else {
                transIns = new Insert(CISales.TransactionOutbound);
            }

            if (createDocument.length > i && createDocument[i] != null) {
                final Instance inst = Instance.get(createDocument[i]);
                if (inst.isValid()) {
                    payInsert.add(CISales.Payment.CreateDocument, inst.getId());
                }
            }
            if (paymentAmount.length > i && paymentAmount[i] != null) {
                payInsert.add(CISales.Payment.Amount, paymentAmount[i]);
                transIns.add(CISales.TransactionAbstract.Amount, paymentAmount[i]);
            }
            payInsert.add(CISales.Payment.TargetDocument, _createdDoc.getInstance().getId());
            payInsert.add(CISales.Payment.CurrencyLink,
                            _createdDoc.getValues().get(getFieldName4Attribute(_parameter,
                                            CISales.PaymentDocumentAbstract.RateCurrencyLink.name)));
            payInsert.add(CISales.Payment.Date,
                            _createdDoc.getValues().get(getFieldName4Attribute(_parameter,
                                            CISales.PaymentDocumentAbstract.Date.name)));
            add2PaymentCreate(_parameter, payInsert, _createdDoc, i);
            payInsert.execute();

            transIns.add(CISales.TransactionAbstract.CurrencyId,
                            _createdDoc.getValues().get(getFieldName4Attribute(_parameter,
                                            CISales.PaymentDocumentAbstract.RateCurrencyLink.name)));
            transIns.add(CISales.TransactionAbstract.Payment, payInsert.getId());
            transIns.add(CISales.TransactionAbstract.Date,
                            _createdDoc.getValues().get(getFieldName4Attribute(_parameter,
                                            CISales.PaymentDocumentAbstract.Date.name)));
            transIns.add(CISales.TransactionAbstract.Account, _parameter.getParameterValue("account"));
            _createdDoc.getValues().put(getFieldName4Attribute(_parameter,
                            CISales.TransactionAbstract.Account.name), _parameter.getParameterValue("account"));

            transIns.execute();
        }
    }

    protected Type getPaymentType(final Parameter _parameter,
                                  final CreatedDoc _createdDoc)
    {
        return CISales.Payment.getType();
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return number of positions
     * @throws EFapsException on error
     */
    protected int getPaymentCount(final Parameter _parameter)
        throws EFapsException
    {
        final String[] countAr = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                        CISales.Payment.CreateDocument.name));
        return countAr == null ? 0 : countAr.length;
    }

    /**
     * Method is calles in the preocess of creation
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _posInsert insert to add to
     * @param _createdDoc document created
     * @throws EFapsException on error
     */
    protected void add2PaymentCreate(final Parameter _parameter,
                                     final Insert _payInsert,
                                     final CreatedDoc _createdDoc,
                                     final int _idx)
          throws EFapsException
    {
        // used by implementation
    }

    public Return autoComplete4CreateDocument(final Parameter _parameter)
        throws EFapsException
    {
        final DecimalFormat formater = (DecimalFormat) NumberFormat.getInstance(Context.getThreadContext().getLocale());
        formater.setMaximumFractionDigits(2);
        formater.setMinimumFractionDigits(2);
        formater.setRoundingMode(RoundingMode.HALF_UP);
        formater.setParseBigDecimal(true);

        final Instance contactInst = (Instance) Context.getThreadContext().getSessionAttribute(
                        AbstractPaymentDocument_Base.INVOICE_SESSIONKEY);

        final Instance contactSessionInst = (Instance) Context.getThreadContext().getSessionAttribute(
                        AbstractPaymentDocument_Base.CONTACT_SESSIONKEY);

        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);

        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();

        for (int i = 0; i < 100; i++) {
            if (props.containsKey("Type" + i)) {
                final Map<Integer, Map<String, String>> tmpMap = new TreeMap<Integer, Map<String, String>>();
                final Type type = Type.get(String.valueOf(props.get("Type" + i)));
                if (type != null) {
                    final QueryBuilder queryBldr = new QueryBuilder(type);
                    queryBldr.addWhereAttrMatchValue(CISales.DocumentAbstract.Name, input + "*").setIgnoreCase(true);
                    if (contactInst != null && contactInst.isValid() && contactSessionInst != null
                                    && contactSessionInst.isValid()) {
                        queryBldr.addWhereAttrEqValue(CISales.DocumentAbstract.Contact, contactInst.getId());
                        queryBldr.addOrderByAttributeAsc(CISales.DocumentAbstract.Date);
                        queryBldr.addOrderByAttributeAsc(CISales.DocumentAbstract.Name);
                    }
                    add2QueryBldr4autoComplete4CreateDocument(_parameter, queryBldr);

                    if (props.containsKey("StatusGroup" + i)) {
                        final String statiStr = String.valueOf(props.get("Stati" + i));
                        final String[] statiAr = statiStr.split(";");
                        final List<Object> statusList = new ArrayList<Object>();
                        for (final String stati : statiAr) {
                            final Status status = Status.find((String) props.get("StatusGroup" + i), stati);
                            if (status != null) {
                                statusList.add(status.getId());
                            }
                        }
                        queryBldr.addWhereAttrEqValue(CISales.DocumentAbstract.StatusAbstract, statusList.toArray());
                    }

                    final MultiPrintQuery multi = queryBldr.getPrint();
                    multi.addAttribute(CISales.DocumentAbstract.Date,
                                    CISales.DocumentAbstract.Name,
                                    CISales.DocumentAbstract.OID,
                                    CISales.DocumentSumAbstract.RateCrossTotal);
                    final SelectBuilder selCur = new SelectBuilder()
                                                    .linkto(CISales.DocumentSumAbstract.RateCurrencyId).instance();
                    multi.addSelect(selCur);
                    multi.setEnforceSorted(true);
                    multi.execute();
                    int number=0;
                    while (multi.next()) {
                        final String name = multi.<String>getAttribute(CISales.DocumentAbstract.Name);
                        final String oid = multi.<String>getAttribute(CISales.DocumentAbstract.OID);
                        final DateTime date = multi.<DateTime>getAttribute(CISales.DocumentAbstract.Date);

                        final StringBuilder choice = new StringBuilder()
                                        .append(name).append(" - ").append(Instance.get(oid).getType().getLabel())
                                        .append(" - ").append(date.toString(DateTimeFormat.forStyle("S-").withLocale(
                                                        Context.getThreadContext().getLocale())));
                        if (multi.getCurrentInstance().getType().isKindOf(CISales.DocumentSumAbstract.getType())) {
                            final BigDecimal amount = multi
                                            .<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
                            final CurrencyInst curr = new CurrencyInst(multi.<Instance>getSelect(selCur));
                            choice.append(" - ").append(curr.getSymbol()).append(" ").append(formater.format(amount));
                        }
                        final Map<String, String> map = new HashMap<String, String>();
                        map.put(EFapsKey.AUTOCOMPLETE_KEY.getKey(), oid);
                        map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), name);
                        map.put(EFapsKey.AUTOCOMPLETE_CHOICE.getKey(), choice.toString());
                        number++;
                        tmpMap.put(number, map);
                    }
                    list.addAll(tmpMap.values());
                }
            } else {
                break;
            }
        }

        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    public Return updateFields4AbsoluteAmount(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();
        final Return retVal = new Return();
        final BigDecimal amount2Pay = getAmount4Pay(_parameter).abs();
        map.put("amount", getTwoDigitsformater().format(amount2Pay));
        map.put("total4DiscountPay", getTwoDigitsformater().format(amount2Pay.subtract(getSumsPositions(_parameter))));
        list.add(map);
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    protected BigDecimal getAmount4Pay(final Parameter _parameter)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        final DecimalFormat formater = Calculator_Base.getFormatInstance();
        final String amountStr = _parameter.getParameterValue("amount");
        try {
            if (amountStr != null && !amountStr.isEmpty()) {
                ret = (BigDecimal) formater.parse(amountStr);
            }
        } catch (final ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ret;
    }

    public Return updateFields4CreateDocument(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();
        final int selected = getSelectedRow(_parameter);
        final String doc = _parameter.getParameterValues("createDocument")[selected];
        if (doc != null) {
            final BigDecimal total4Doc = getAttribute4Document(doc, CISales.DocumentSumAbstract.RateCrossTotal.name);
            final BigDecimal payments4Doc = getPayments4Document(doc);
            final BigDecimal amount2Pay = total4Doc.subtract(payments4Doc);
            final String symbol = getSymbol4Document(doc, CISales.DocumentSumAbstract.RateCurrencyId.name, CIERP.Currency.Symbol.name);
            final StringBuilder bldr = new StringBuilder();
            bldr.append(getTwoDigitsformater().format(total4Doc))
                            .append(" / ").append(getTwoDigitsformater().format(payments4Doc)).append(" - ").append(symbol);
            map.put("createDocumentDesc", bldr.toString());
            map.put("payment4Pay", getTwoDigitsformater().format(amount2Pay));
            map.put("paymentAmount", getTwoDigitsformater().format(BigDecimal.ZERO));
            map.put("paymentAmountDesc", getTwoDigitsformater().format(BigDecimal.ZERO));
            list.add(map);
        }
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    public Return updateFields4PaymentAmount(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();
        final int selected = getSelectedRow(_parameter);
        final String docStr = _parameter.getParameterValues("createDocument")[selected];
        final String payStr = _parameter.getParameterValues("paymentAmount")[selected];
        final String amount4PayStr = _parameter.getParameterValues("payment4Pay")[selected];
        final String account = _parameter.getParameterValue("account");
        final PrintQuery printAccount = new PrintQuery(Instance.get(CISales.AccountCashDesk.getType(), account));
        final SelectBuilder selCurInst = new SelectBuilder().linkto(CISales.AccountAbstract.CurrencyLink).instance();
        printAccount.addSelect(selCurInst);
        printAccount.execute();
        final Instance currencyAccount = printAccount.<Instance>getSelect(selCurInst);

        final DecimalFormat formater = Calculator_Base.getFormatInstance();
        BigDecimal pay = BigDecimal.ZERO;
        BigDecimal amount4Pay = BigDecimal.ZERO;
        try {
            if (payStr != null && !payStr.isEmpty()) {
                pay = (BigDecimal) formater.parse(payStr);
            }
            if (amount4PayStr != null && !amount4PayStr.isEmpty()) {
                amount4Pay = (BigDecimal) formater.parse(amount4PayStr);
            }
        } catch (final ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        BigDecimal payConverted = pay;
        final Instance instanceDoc = Instance.get(docStr);
        final QueryBuilder queryBldr = new QueryBuilder(CISales.DocumentAbstract);
        queryBldr.addWhereAttrEqValue(CISales.DocumentAbstract.ID, instanceDoc.getId());
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selCurrencyInst = new SelectBuilder()
                        .linkto(CISales.DocumentSumAbstract.RateCurrencyId).instance();
        multi.addSelect(selCurrencyInst);
        multi.execute();
        if (multi.next()) {
            final Instance currencyDocSelected = multi.<Instance>getSelect(selCurrencyInst);
            if (!currencyDocSelected.equals(currencyAccount)) {
                final Instance baseInstDoc = Instance.get(CIERP.Currency.getType(), currencyAccount.getId());
                final BigDecimal[] rates = new PriceUtil().getRates(_parameter, currencyDocSelected, baseInstDoc);
                payConverted = payConverted.multiply(rates[2]);
            }
        }

        final Return retVal = new Return();
        map.put("paymentAmount", getTwoDigitsformater().format(pay));
        map.put("paymentAmountDesc", getTwoDigitsformater().format(amount4Pay.subtract(payConverted)));
        map.put("total4DiscountPay", getTwoDigitsformater()
                        .format(getAmount4Pay(_parameter).abs().subtract(getSumsPositions(_parameter))));
        list.add(map);
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    protected String getSymbol4Document(final String _doc,
                                        final String _linkTo,
                                        final String _attribute)
        throws EFapsException
    {
        String ret = "";
        final Instance docInst = Instance.get(_doc);
        if (docInst.isValid()) {
            final SelectBuilder selSymbol = new SelectBuilder().linkto(_linkTo).attribute(_attribute);
            final PrintQuery print = new PrintQuery(_doc);
            print.addSelect(selSymbol);
            print.execute();
            ret = print.<String>getSelect(selSymbol);
        }
        return ret;
    }

    protected BigDecimal getPayments4Document(final String _doc)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        final Instance docInst = Instance.get(_doc);
        if (docInst.isValid()) {
            final QueryBuilder queryBldr = new QueryBuilder(CISales.Payment);
            queryBldr.addWhereAttrEqValue(CISales.Payment.CreateDocument, docInst.getId());
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CISales.Payment.Amount);
            multi.execute();
            while (multi.next()) {
                ret = ret.add(multi.<BigDecimal>getAttribute(CISales.Payment.Amount));
            }
        }
        return ret;
    }

    protected BigDecimal getAttribute4Document(final String _doc,
                                               final String _attribute)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        final Instance docInst = Instance.get(_doc);
        if (docInst.isValid()) {
            final PrintQuery print = new PrintQuery(_doc);
            print.addAttribute(_attribute);
            print.execute();
            ret = print.<BigDecimal>getAttribute(_attribute);
        }
        return ret == null ? BigDecimal.ZERO : ret;
    }

    protected BigDecimal getSumsPositions(final Parameter _parameter)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        final DecimalFormat formater = Calculator_Base.getFormatInstance();
        final String[] paymentPosAmount = _parameter.getParameterValues("paymentAmount");
        for (int i = 0; i < getPaymentCount(_parameter); i++) {
            BigDecimal paymentPos = BigDecimal.ZERO;
            if (paymentPosAmount.length > i && paymentPosAmount[i] != null) {
                try {
                    if (paymentPosAmount[i] != null && !paymentPosAmount[i].isEmpty()) {
                        paymentPos = (BigDecimal) formater.parse(paymentPosAmount[i]);
                    }
                } catch (final ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                ret = ret.add(paymentPos);
            }
        }
        return ret;
    }

    protected int getSelectedRow(final Parameter _parameter)
    {
        int ret = 0;
        final String value = _parameter.getParameterValue("eFapsRowSelectedRow");
        if (value != null && value.length() > 0) {
            ret = Integer.parseInt(value);
        }
        return ret;
    }

    public Return update4checkbox4Invoive(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String contactOid = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.PaymentDocumentAbstract.Contact.name));
        final String check = _parameter.getParameterValue("checkbox4Invoice");
        final Instance contact = Instance.get(contactOid);
        if (check == null && !"true".equalsIgnoreCase(check)) {
            if (contact.isValid()) {
                Context.getThreadContext().setSessionAttribute(AbstractPaymentDocument_Base.INVOICE_SESSIONKEY, contact);
            } else {
                Context.getThreadContext().setSessionAttribute(AbstractPaymentDocument_Base.INVOICE_SESSIONKEY, null);
            }
        } else {
            Context.getThreadContext().setSessionAttribute(AbstractPaymentDocument_Base.INVOICE_SESSIONKEY, null);
        }
        Context.getThreadContext().setSessionAttribute(AbstractPaymentDocument_Base.CONTACT_SESSIONKEY, contact);
        return ret;
    }

    public Return updateFields4RateCurrency(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final PrintQuery print = new PrintQuery(CISales.AccountCashDesk.getType(),
                        _parameter.getParameterValue("account"));
        print.addAttribute(CISales.AccountCashDesk.CurrencyLink);
        print.execute();

        final Instance newInst = Instance.get(CIERP.Currency.getType(),
                        print.<Long>getAttribute(CISales.AccountCashDesk.CurrencyLink));

        final Instance baseInst = Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);

        final Map<String, String> map = new HashMap<String, String>();
        final BigDecimal[] rates = new PriceUtil().getRates(_parameter, newInst, baseInst);
        map.put(CIFormSales.Sales_PaymentCheckWithOutDocForm.rate.name, getFormater(0, 2).format(rates[3]));
        map.put(CIFormSales.Sales_PaymentCheckWithOutDocForm.rate.name + RateUI.INVERTEDSUFFIX,
                        "" + (rates[3].compareTo(rates[0]) != 0));
        list.add(map);

        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    public Return updateFields4Contact(final Parameter _parameter)
        throws EFapsException
    {
        final Instance contact = Instance.get(_parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.PaymentDocumentAbstract.Contact.name)));
        final String check = _parameter.getParameterValue("checkbox4Invoice");
        if (contact.isValid() && check == null && !"true".equalsIgnoreCase(check)) {
            Context.getThreadContext().setSessionAttribute(AbstractPaymentDocument_Base.INVOICE_SESSIONKEY, contact);
        } else {
            Context.getThreadContext().setSessionAttribute(AbstractPaymentDocument_Base.INVOICE_SESSIONKEY, null);
        }
        Context.getThreadContext().setSessionAttribute(AbstractPaymentDocument_Base.CONTACT_SESSIONKEY, contact);
        return new Return();
    }

    public Return deactivateFiltered4Invoice(final Parameter _parameter)
        throws EFapsException
    {
        Context.getThreadContext().setSessionAttribute(AbstractPaymentDocument_Base.INVOICE_SESSIONKEY, null);
        Context.getThreadContext().setSessionAttribute(AbstractPaymentDocument_Base.CONTACT_SESSIONKEY, null);
        return new Return();
    }

    /**
     * Method to get a formater.
     *
     * @return a formater
     * @throws EFapsException on error
     */
    protected DecimalFormat getTwoDigitsformater()
        throws EFapsException
    {
        return getFormater(2, 2);
    }

    /**
     * Method to get a formater.
     *
     * @return a formater
     * @throws EFapsException on error
     */
    protected DecimalFormat getZeroDigitsformater()
        throws EFapsException
    {
        return getFormater(0, 0);
    }

    /**
     * @return a formater used to format bigdecimal for the user interface
     * @param _maxFrac maximum Faction, null to deactivate
     * @param _minFrac minimum Faction, null to activate
     * @throws EFapsException on error
     */
    public DecimalFormat getFormater(final Integer _minFrac,
                                     final Integer _maxFrac)
        throws EFapsException
    {
        final DecimalFormat formater = (DecimalFormat) NumberFormat.getInstance(Context.getThreadContext().getLocale());
        if (_maxFrac != null) {
            formater.setMaximumFractionDigits(_maxFrac);
        }
        if (_minFrac != null) {
            formater.setMinimumFractionDigits(_minFrac);
        }
        formater.setRoundingMode(RoundingMode.HALF_UP);
        formater.setParseBigDecimal(true);
        return formater;
    }

    protected Object[] getRateObject(final Parameter _parameter)
        throws EFapsException
    {
        BigDecimal rate = BigDecimal.ONE;
        try {
            rate = (BigDecimal) Calculator_Base.getFormatInstance().parse(_parameter.getParameterValue("rate"));
        } catch (final ParseException e) {
            throw new EFapsException(AbstractDocument_Base.class, "analyzeRate.ParseException", e);
        }
        final boolean rInv = "true".equalsIgnoreCase(_parameter.getParameterValue("rate" + RateUI.INVERTEDSUFFIX));
        return new Object[] { rInv ? BigDecimal.ONE : rate, rInv ? rate : BigDecimal.ONE };
    }

    protected String getCode4GeneratedDocWithSysConfig(final Parameter _parameter)
        throws EFapsException
    {
        String ret = "";
        // Sales-Configuration
        final SystemConfiguration config = SystemConfiguration.get(
                        UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f"));
        if (config != null) {
            if (getType4DocCreate(_parameter).isKindOf(CISales.PaymentDocumentAbstract.getType())) {
                final boolean active = config.getAttributeValueAsBoolean(SalesSettings.ACTIVATECODE4PAYMENTDOCUMENT);
                if (active) {
                    final String uuid = config.getAttributeValue(SalesSettings.SEQUENCE4PAYMENTDOCUMENT);
                    ret = NumberGenerator.get(UUID.fromString(uuid)).getNextVal();
                }
            } else if (getType4DocCreate(_parameter).isKindOf(CISales.PaymentDocumentOutAbstract.getType())) {
                final boolean active = config.getAttributeValueAsBoolean(SalesSettings.ACTIVATECODE4PAYMENTDOCUMENTOUT);
                if (active) {
                    final String uuid = config.getAttributeValue(SalesSettings.SEQUENCE4PAYMENTDOCUMENTOUT);
                    ret = NumberGenerator.get(UUID.fromString(uuid)).getNextVal();
                }
            }
        }
        return !ret.isEmpty() ? ret : null;
    }

    protected boolean getActive4GenerateReport(final Parameter _parameter)
        throws EFapsException
    {
        boolean ret = false;
        final SystemConfiguration config = Sales.getSysConfig();
        if (config != null) {
            final boolean active = config.getAttributeValueAsBoolean(SalesSettings.ACTIVATEPRINTREPORT4PAYMENTDOCUMENT);
            if (active) {
                ret = true;
            }
        }
        return ret;
    }

    public Return validatePaymentDocument(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final StringBuilder html = new StringBuilder();
        final BigDecimal amount4Doc = getAmount4Pay(_parameter);
        final BigDecimal pos4Doc = getSumsPositions(_parameter);
        if (amount4Doc.compareTo(pos4Doc) == 0) {
            html.append(DBProperties.getProperty("org.efaps.esjp.sales.payment.PaymentCorrect"));
        } else {
            if (amount4Doc.compareTo(pos4Doc) == 1) {
                html.append(DBProperties.getProperty("org.efaps.esjp.sales.payment.PaymentPositive"));
            } else {
                html.append(DBProperties.getProperty("org.efaps.esjp.sales.payment.PaymentNegative"));
            }
        }
        ret.put(ReturnValues.SNIPLETT, html.toString());
        ret.put(ReturnValues.TRUE, true);
        return ret;
    }

    protected Return createReportDoc(final Parameter _parameter,
                                     final CreatedDoc _createdDoc)
        throws EFapsException
    {
        Return ret = new Return();

        if (getActive4GenerateReport(_parameter)) {
            final StandartReport report = new StandartReport();
            final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
            _parameter.put(ParameterValues.INSTANCE, _createdDoc.getInstance());
            Object name = _createdDoc.getValues().get(
                            getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.Code.name));
            if (name == null) {
                name = _createdDoc.getValues()
                                .get(getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.Name.name));
            }

            final String fileName = DBProperties.getProperty(_createdDoc.getInstance().getType().getName() + ".Label")
                            + "_" + name;
            report.setFileName(fileName);
            final SelectBuilder selCurName = new SelectBuilder().linkto(CISales.AccountCashDesk.CurrencyLink)
                            .attribute(CIERP.Currency.Name);
            report.getJrParameters().put("accountName",
                    getSelectString4AttributeAccount((String) _createdDoc.getValues()
                                .get(getFieldName4Attribute(_parameter,
                                            CISales.TransactionAbstract.Account.name)),
                                            null, CISales.AccountCashDesk.Name));
            report.getJrParameters().put("accountCurrencyName",
                    getSelectString4AttributeAccount((String) _createdDoc.getValues()
                                .get(getFieldName4Attribute(_parameter,
                                            CISales.TransactionAbstract.Account.name)), selCurName, null));

            final SystemConfiguration config = ERP.getSysConfig();
            if (config != null) {
                final String companyName = config.getAttributeValue(ERPSettings.COMPANYNAME);
                final String companyTaxNumb = config.getAttributeValue(ERPSettings.COMPANYTAX);

                if (companyName != null && companyTaxNumb != null && !companyName.isEmpty()
                                && !companyTaxNumb.isEmpty()) {
                    report.getJrParameters().put("CompanyName", companyName);
                    report.getJrParameters().put("CompanyTaxNum", companyTaxNumb);
                }
            }
            addParameter4Report(_parameter, _createdDoc, report);
            ret = report.execute(_parameter);
            ret.put(ReturnValues.TRUE, true);

            try {
                final File file = (File) ret.get(ReturnValues.VALUES);
                final InputStream input = new FileInputStream(file);
                final Checkin checkin = new Checkin(_createdDoc.getInstance());
                checkin.execute(fileName + "." + properties.get("Mime"), input, ((Long) file.length()).intValue());
            } catch (final FileNotFoundException e) {
                throw new EFapsException(Invoice.class, "create.FileNotFoundException", e);
            }
        }

        return ret;
    }

    protected String getSelectString4AttributeAccount(final String _accountId,
                                                      final SelectBuilder _select,
                                                      final CIAttribute _attribute)
        throws EFapsException
    {
        String ret = "";
        if (_accountId != null) {
            final PrintQuery print = new PrintQuery(CISales.AccountCashDesk.getType(), _accountId);
            if (_select != null) {
                print.addSelect(_select);
            } else if (_attribute != null) {
                print.addAttribute(_attribute);
            }
            print.execute();
            if (_select != null) {
                ret = print.getSelect(_select);
            } else if (_attribute != null) {
                ret = print.getAttribute(_attribute);
            }
        }
        return ret.isEmpty() ? null : ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _queryBldr queryBuilder to add to
     */
    protected void add2QueryBldr4autoComplete4CreateDocument(final Parameter _parameter,
                                                             final QueryBuilder _queryBldr)
    {
        // used bt implementation
    }

    protected void addParameter4Report(final Parameter _parameter,
                                       final CreatedDoc _createdDoc,
                                       final StandartReport _report)
        throws EFapsException
    {
        // used bt implementation
    }

    public Return update4StatusCanceled (final Parameter _parameter)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CISales.Payment);
        queryBldr.addWhereAttrEqValue(CISales.Payment.TargetDocument, _parameter.getInstance().getId());
        final InstanceQuery queryInst = queryBldr.getQuery();
        queryInst.execute();
        while (queryInst.next()) {
            final QueryBuilder queryBldr2 = new QueryBuilder(CISales.TransactionAbstract);
            queryBldr2.addWhereAttrEqValue(CISales.TransactionAbstract.Payment, queryInst.getCurrentValue().getId());
            final MultiPrintQuery multi = queryBldr2.getPrint();
            multi.addAttribute(CISales.TransactionAbstract.Amount,
                            CISales.TransactionAbstract.CurrencyId,
                            CISales.TransactionAbstract.Account);
            multi.execute();
            boolean updatePayment = false;
            while (multi.next()) {
                Insert insert;
                if (multi.getCurrentInstance().getType().isKindOf(CISales.TransactionOutbound.getType())) {
                    insert = new Insert(CISales.TransactionInbound);
                } else {
                    insert = new Insert(CISales.TransactionOutbound);
                }
                insert.add(CISales.TransactionAbstract.Amount, multi.<BigDecimal>getAttribute(CISales.TransactionAbstract.Amount));
                insert.add(CISales.TransactionAbstract.CurrencyId, multi.<Long>getAttribute(CISales.TransactionAbstract.CurrencyId));
                insert.add(CISales.TransactionAbstract.Account, multi.<Long>getAttribute(CISales.TransactionAbstract.Account));
                insert.add(CISales.TransactionAbstract.Payment, queryInst.getCurrentValue().getId());
                insert.add(CISales.TransactionAbstract.Description,
                                DBProperties.getProperty("org.efaps.esjp.sales.payment.AbstractPaymentDocument.correctionPayment"));
                insert.add(CISales.TransactionAbstract.Date, new DateTime());
                insert.execute();

                if (insert.getInstance().isValid()) {
                    updatePayment = true;
                }
            }

            if (updatePayment) {
                final Update update = new Update(queryInst.getCurrentValue());
                update.add(CISales.Payment.Amount, BigDecimal.ZERO);
                update.executeWithoutAccessCheck();
            }
        }

        final Update updatePayment = new Update(_parameter.getInstance());
        updatePayment.add(CISales.Payment.Amount, BigDecimal.ZERO);
        updatePayment.executeWithoutAccessCheck();

        return new StatusValue().setStatus(_parameter);
    }

    public Return getPayments4Document(final Parameter _parameter)
        throws EFapsException
    {
        return new MultiPrint()
        {
            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.PayableDocument2Document);
                attrQueryBldr.addWhereAttrEqValue(CISales.PayableDocument2Document.ToLink,
                                _parameter.getInstance().getId());
                final AttributeQuery attrQuery = attrQueryBldr
                                .getAttributeQuery(CISales.PayableDocument2Document.FromLink);

                _queryBldr.addWhereAttrInQuery(CISales.Payment.CreateDocument, attrQuery);
                _queryBldr.setOr(true);
            }
        }.execute(_parameter);
    }

    /**
     * Executed the command on the button.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return executeButton(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        StringBuilder js = new StringBuilder();
        final String account = _parameter.getParameterValue("account");
        final PrintQuery printAccount = new PrintQuery(Instance.get(CISales.AccountCashDesk.getType(), account));
        final SelectBuilder selCurInst = new SelectBuilder().linkto(
                        CISales.AccountAbstract.CurrencyLink).instance();
        printAccount.addSelect(selCurInst);
        printAccount.execute();
        final Instance currencyId = printAccount.<Instance>getSelect(selCurInst);

        BigDecimal restAmount = getAmount4Pay(_parameter);
        BigDecimal sumPayments = BigDecimal.ZERO;
        final List<Instance> instances = getDocInstances(_parameter);
        final List<Instance> instances2Print = new ArrayList<Instance>();

        for (final Instance inst : instances) {
            final QueryBuilder queryBldr = new QueryBuilder(CISales.DocumentAbstract);
            queryBldr.addWhereAttrEqValue(CISales.DocumentAbstract.ID, inst.getId());
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CISales.DocumentAbstract.Name, CISales.DocumentSumAbstract.RateCrossTotal);
            final SelectBuilder selCurrencyInst = new SelectBuilder()
                            .linkto(CISales.DocumentSumAbstract.RateCurrencyId).instance();
            multi.addSelect(selCurrencyInst);
            multi.execute();
            if (multi.next()) {
                final Instance instCurrency = multi.<Instance>getSelect(selCurrencyInst);

                final BigDecimal pay = BigDecimal.ZERO;
                final String doc = inst.getOid();
                final BigDecimal total4Doc = getAttribute4Document(doc, CISales.DocumentSumAbstract.RateCrossTotal.name);
                final BigDecimal payments4Doc = getPayments4Document(doc);
                final BigDecimal amount2Pay = total4Doc.subtract(payments4Doc);
                BigDecimal amountDue = amount2Pay.subtract(pay);

                if (!currencyId.equals(instCurrency)) {
                    final Instance baseInstDoc = Instance.get(CIERP.Currency.getType(), currencyId.getId());
                    final BigDecimal[] rates = new PriceUtil().getRates(_parameter, baseInstDoc, instCurrency);
                    amountDue = amountDue.multiply(rates[2]);
                }

                instances2Print.add(multi.getCurrentInstance());
                if (amountDue.compareTo(restAmount) == 1 || amountDue.compareTo(restAmount) == 0) {
                    break;
                } else {
                    restAmount = restAmount.subtract(amountDue);
                    sumPayments = sumPayments.add(amountDue);
                }

            }
        }
        sumPayments = sumPayments.add(restAmount);
        js = buildHtml4ExecuteButton(_parameter, instances2Print, restAmount, sumPayments, currencyId);
        ret.put(ReturnValues.SNIPLETT, js.toString());
        return ret;
    }

    protected StringBuilder buildHtml4ExecuteButton(final Parameter _parameter,
                                                    final List<Instance> _instancesList,
                                                    final BigDecimal _restAmount,
                                                    final BigDecimal _sumPayments,
                                                    final Instance _currencyActual)
        throws EFapsException
    {
        final StringBuilder js = new StringBuilder();
        js.append(getTableRemoveScript(_parameter, "paymentTable"));

        js.append("function setPayment(){");
        int index = 0;
        boolean lastPos = false;

        for (final Instance payment : _instancesList) {
            if (_instancesList.size() == index + 1) {
                lastPos = true;
            }
            js.append(getScriptLine(_parameter, payment, index, lastPos, _restAmount, _currencyActual));
            index++;
        }

        js.append("}\n");
        js.append(getScriptValues(_parameter, _instancesList));

        final BigDecimal total4DiscountPay = getAmount4Pay(_parameter).abs().subtract(_sumPayments);
        js.append(getSetFieldValue(0, "total4DiscountPay", total4DiscountPay == null
                        ? BigDecimal.ZERO.toString() : getTwoDigitsformater().format(total4DiscountPay)));

        return js;
    }

    /**
     * @param _instances instances to print
     * @return StringBuilder
     */
    protected StringBuilder getScriptValues(final Parameter _parameter,
                                            final List<Instance> _instances)
    {
        final StringBuilder ret = new StringBuilder();
        ret.append(" addNewRows_paymentTable(").append(_instances.size() - 1)
                        .append(", setPayment, null);");

        return ret;
    }

    /**
     * @param _instance Instance of each document
     * @param _index index of each document
     * @param _lastPosition boolean to indicate the last position to print
     * @param _restAmount last quantity to pay
     * @param _currencyActual current currency of document
     * @return StringBuilder
     * @throws EFapsException on error
     */
    protected StringBuilder getScriptLine(final Parameter _parameter,
                                          final Instance _instance,
                                          final Integer _index,
                                          final boolean _lastPosition,
                                          final BigDecimal _restAmount,
                                          final Instance _currencyActual)
        throws EFapsException
    {
        final StringBuilder ret = new StringBuilder();
        final QueryBuilder queryBldr = new QueryBuilder(CISales.DocumentAbstract);
        queryBldr.addWhereAttrEqValue(CISales.DocumentAbstract.ID, _instance.getId());
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.DocumentAbstract.Name, CISales.DocumentSumAbstract.RateCrossTotal, CISales.DocumentSumAbstract.CurrencyId);
        final SelectBuilder selCurrencyInst = new SelectBuilder()
                        .linkto(CISales.DocumentSumAbstract.RateCurrencyId).instance();
        multi.addSelect(selCurrencyInst);
        multi.execute();
        if (multi.next()){
            final String name = multi.<String>getAttribute(CISales.DocumentAbstract.Name);
            final BigDecimal rateCrossTotal = multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);

            BigDecimal pay = BigDecimal.ZERO;
            final String doc = _instance.getOid();
            final BigDecimal total4Doc = getAttribute4Document(doc, CISales.DocumentSumAbstract.RateCrossTotal.name);
            final BigDecimal payments4Doc = getPayments4Document(doc);
            final BigDecimal amount2Pay = total4Doc.subtract(payments4Doc);
            final BigDecimal amountDue = amount2Pay.subtract(pay);
            final String symbol = getSymbol4Document(_instance.getOid(), CISales.DocumentSumAbstract.RateCurrencyId.name, CIERP.Currency.Symbol.name);
            final StringBuilder bldr = new StringBuilder();
            bldr.append(getTwoDigitsformater().format(rateCrossTotal))
                            .append(" / ").append(getTwoDigitsformater().format(payments4Doc)).append(" - ").append(symbol);


            ret.append(getSetFieldValue(_index, "createDocument", _instance.getOid())).append("\n")
            .append(getSetFieldValue(_index, "createDocumentAutoComplete", name)).append("\n")
            .append(getSetFieldValue(_index, "createDocumentDesc", bldr.toString())).append("\n");


            final Instance currencyDocInst = multi.<Instance>getSelect(selCurrencyInst);

            BigDecimal amountDueConverted = amountDue;
            BigDecimal restAmountConverted = _restAmount;
            if (!_currencyActual.equals(currencyDocInst)) {
                final Instance baseInstDoc = Instance.get(CIERP.Currency.getType(), _currencyActual.getId());
                final BigDecimal[] rates = new PriceUtil().getRates(_parameter, baseInstDoc, currencyDocInst);
                amountDueConverted = amountDueConverted.multiply(rates[2]);
                restAmountConverted = restAmountConverted.multiply(rates[3]);
            }

            if (!_lastPosition) {
                ret.append(getSetFieldValue(_index, "paymentAmount", amountDue == null
                                ? BigDecimal.ZERO.toString() : getTwoDigitsformater().format(amountDueConverted)))
                                .append("\n");
                pay = amountDue;
            } else {
                ret.append(getSetFieldValue(_index, "paymentAmount", _restAmount == null
                                ? BigDecimal.ZERO.toString() : getTwoDigitsformater().format(_restAmount)))
                                .append("\n");
                pay = restAmountConverted;
            }

            ret.append(getSetFieldValue(_index, "paymentAmountDesc",
                            getTwoDigitsformater().format(amount2Pay.subtract(pay)))).append("\n");
        }

        return ret;
    }

    protected List<Instance> getDocInstances(final Parameter _parameter)
        throws EFapsException
    {
        final List<Instance> instances = new ArrayList<Instance>();
        final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final Instance contactInst = (Instance) Context.getThreadContext().getSessionAttribute(
                        AbstractPaymentDocument_Base.INVOICE_SESSIONKEY);

        final Instance contactSessionInst = (Instance) Context.getThreadContext().getSessionAttribute(
                        AbstractPaymentDocument_Base.CONTACT_SESSIONKEY);
        for (int i = 0; i < 100; i++) {
            if (props.containsKey("Type" + i)) {
                final Type type = Type.get(String.valueOf(props.get("Type" + i)));
                if (type != null) {
                    final QueryBuilder queryBldr = new QueryBuilder(type);
                    if (contactInst != null && contactInst.isValid() && contactSessionInst != null
                                    && contactSessionInst.isValid()) {
                        queryBldr.addWhereAttrEqValue(CISales.DocumentAbstract.Contact, contactInst.getId());
                        queryBldr.addOrderByAttributeAsc(CISales.DocumentAbstract.Date);
                        queryBldr.addOrderByAttributeAsc(CISales.DocumentAbstract.Name);
                    }
                    add2QueryBldr4autoComplete4CreateDocument(_parameter, queryBldr);

                    if (props.containsKey("StatusGroup" + i)) {
                        final String statiStr = String.valueOf(props.get("Stati" + i));
                        final String[] statiAr = statiStr.split(";");
                        final List<Object> statusList = new ArrayList<Object>();
                        for (final String stati : statiAr) {
                            final Status status = Status.find((String) props.get("StatusGroup" + i), stati);
                            if (status != null) {
                                statusList.add(status.getId());
                            }
                        }
                        queryBldr.addWhereAttrEqValue(CISales.DocumentAbstract.StatusAbstract, statusList.toArray());
                    }
                    final InstanceQuery query = queryBldr.getQuery();
                    query.execute();
                    instances.addAll(query.getValues());
                }
            }
        }

        final List<Long> listIds = new ArrayList<Long>();
        for (final Instance instanceAux : instances) {
            listIds.add(instanceAux.getId());
        }
        if (!listIds.isEmpty()) {
            final QueryBuilder queryBldrDocs = new QueryBuilder(CISales.DocumentAbstract);
            queryBldrDocs.addWhereAttrEqValue(CISales.DocumentAbstract.ID, listIds.toArray());
            queryBldrDocs.addOrderByAttributeAsc(CISales.DocumentAbstract.Date);
            queryBldrDocs.addOrderByAttributeAsc(CISales.DocumentAbstract.Name);
            final MultiPrintQuery multi = queryBldrDocs.getPrint();
            multi.setEnforceSorted(true);
            multi.execute();
            instances.clear();
            instances.addAll(multi.getInstanceList());
        }

        return instances;
    }

}
