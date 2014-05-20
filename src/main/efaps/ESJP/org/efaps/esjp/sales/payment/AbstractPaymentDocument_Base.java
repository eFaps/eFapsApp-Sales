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
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.lang3.StringEscapeUtils;
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
import org.efaps.ci.CIType;
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
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.ci.CITableSales;
import org.efaps.esjp.common.jasperreport.StandartReport;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.erp.RateFormatter;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.esjp.erp.util.ERPSettings;
import org.efaps.esjp.sales.PriceUtil;
import org.efaps.esjp.sales.document.AbstractDocument_Base;
import org.efaps.esjp.sales.document.IncomingDetraction;
import org.efaps.esjp.sales.document.IncomingDetraction_Base;
import org.efaps.esjp.sales.document.IncomingRetention;
import org.efaps.esjp.sales.document.IncomingRetention_Base;
import org.efaps.esjp.sales.document.Invoice;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.Sales.AccountCDActivation;
import org.efaps.esjp.sales.util.SalesSettings;
import org.efaps.esjp.ui.html.HtmlTable;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    /**
     * Logger for this class.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(MultiPrint.class);

    public static final String INVOICE_SESSIONKEY = "eFaps_Selected_Sales_Invoice";

    public static final String CONTACT_SESSIONKEY = "eFaps_Selected_Contact";

    public static final String CHANGE_AMOUNT = "eFaps_Selected_ChangeAmount";

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
            createdDoc.getValues().put(CISales.PaymentDocumentAbstract.Note.name, note);
        }

        final String amount = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.PaymentDocumentAbstract.Amount.name));
        if (amount != null) {
            insert.add(CISales.PaymentDocumentAbstract.Amount, amount);
            createdDoc.getValues().put(CISales.PaymentDocumentAbstract.Amount.name, amount);
        }

        final String date = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.PaymentDocumentAbstract.Date.name));
        if (date != null) {
            insert.add(CISales.PaymentDocumentAbstract.Date, date);
            createdDoc.getValues().put(CISales.PaymentDocumentAbstract.Date.name, date);
        }

        final String dueDate = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.PaymentDocumentAbstract.DueDate.name));
        if (dueDate != null) {
            insert.add(CISales.PaymentDocumentAbstract.DueDate, dueDate);
            createdDoc.getValues().put(CISales.PaymentDocumentAbstract.DueDate.name, dueDate);
        }

        final String revision = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.PaymentDocumentAbstract.Revision.name));
        if (revision != null) {
            insert.add(CISales.PaymentDocumentAbstract.Revision, revision);
            createdDoc.getValues().put(CISales.PaymentDocumentAbstract.Revision.name, revision);
        }

        final String currencyLink = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.PaymentDocumentAbstract.CurrencyLink.name));
        if (currencyLink != null) {
            insert.add(CISales.PaymentDocumentAbstract.RateCurrencyLink, currencyLink);
            createdDoc.getValues().put(CISales.PaymentDocumentAbstract.RateCurrencyLink.name,
                            Long.parseLong(currencyLink));
            final Instance baseCurrInst = Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);
            insert.add(CISales.PaymentDocumentAbstract.CurrencyLink, baseCurrInst.getId());
            createdDoc.getValues().put(CISales.PaymentDocumentAbstract.CurrencyLink.name,
                            baseCurrInst.getId());
        }

        final String currencyLink4Account = getRateCurrencyLink4Account(_parameter);
        if (currencyLink4Account != null) {
            insert.add(CISales.PaymentDocumentAbstract.RateCurrencyLink, currencyLink4Account);
            createdDoc.getValues().put(CISales.PaymentDocumentAbstract.RateCurrencyLink.name,
                            Long.parseLong(currencyLink4Account));
            final Instance baseCurrInst = Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);
            insert.add(CISales.PaymentDocumentAbstract.CurrencyLink, baseCurrInst.getId());
            createdDoc.getValues().put(CISales.PaymentDocumentAbstract.CurrencyLink.name,
                            baseCurrInst.getId());
        }

        final String contact = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.PaymentDocumentAbstract.Contact.name));
        if (contact != null && Instance.get(contact).isValid()) {
            insert.add(CISales.PaymentDocumentAbstract.Contact, Instance.get(contact).getId());
            createdDoc.getValues().put(CISales.PaymentDocumentAbstract.Contact.name,
                            Instance.get(contact).getId());
        }

        final Object rateObj = _parameter.getParameterValue("rate");
        if (rateObj != null) {
            insert.add(CISales.PaymentDocumentAbstract.Rate, getRateObject(_parameter));
            createdDoc.getValues().put(CISales.PaymentDocumentAbstract.Rate.name, getRateObject(_parameter));
        }

        final String code = getCode4GeneratedDocWithSysConfig(_parameter);
        if (code != null) {
            insert.add(CISales.PaymentDocumentAbstract.Code, code);
            createdDoc.getValues().put(CISales.PaymentDocumentAbstract.Code.name, code);
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
                    payInsert.add(CISales.Payment.RateCurrencyLink, getNewDocumentInfo(inst).getRateCurrency());

                    _createdDoc.addPosition(inst);
                }
            }
            if (paymentAmount.length > i && paymentAmount[i] != null) {
                payInsert.add(CISales.Payment.Amount, paymentAmount[i]);
                transIns.add(CISales.TransactionAbstract.Amount, paymentAmount[i]);
            }
            payInsert.add(CISales.Payment.TargetDocument, _createdDoc.getInstance().getId());
            payInsert.add(CISales.Payment.CurrencyLink,
                            _createdDoc.getValues().get(CISales.PaymentDocumentAbstract.RateCurrencyLink.name));
            payInsert.add(CISales.Payment.Date,
                            _createdDoc.getValues().get(CISales.PaymentDocumentAbstract.Date.name));
            payInsert.add(CISales.Payment.Rate, getRateObject(_parameter, "paymentRate", i));
            add2PaymentCreate(_parameter, payInsert, _createdDoc, i);
            payInsert.execute();

            transIns.add(CISales.TransactionAbstract.CurrencyId,
                            _createdDoc.getValues().get(CISales.PaymentDocumentAbstract.RateCurrencyLink.name));
            transIns.add(CISales.TransactionAbstract.Payment, payInsert.getId());
            transIns.add(CISales.TransactionAbstract.Date,
                            _createdDoc.getValues().get(CISales.PaymentDocumentAbstract.Date.name));
            transIns.add(CISales.TransactionAbstract.Account, _parameter.getParameterValue("account"));
            _createdDoc.getValues().put(CISales.TransactionAbstract.Account.name,
                            _parameter.getParameterValue("account"));

            transIns.execute();
        }
    }

    protected void createDocumentTax(final Parameter _parameter,
                                     final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final String[] amount4Create = _parameter.getParameterValues("amount4DocCreate");
        final String[] option4Create = _parameter.getParameterValues("option4DocCreate");

        final Object rateCurId = _createdDoc.getValues().get(
                        getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.RateCurrencyLink.name));
        final Object curId = _createdDoc.getValues().get(
                        getFieldName4Attribute(_parameter, CISales.PaymentDocumentAbstract.CurrencyLink.name));

        for (int i = 0; i < getPaymentCount(_parameter); i++) {
            if (option4Create != null && amount4Create != null) {
                if (((Long) curId).equals(rateCurId) && isIncomingInvoiceValid(_parameter, _createdDoc, i)
                            && parseBigDecimal(amount4Create[i]).compareTo(BigDecimal.ZERO) > 0) {
                    final String valueDoc = option4Create[i];
                    if ("IncomingRetention".equalsIgnoreCase(valueDoc)) {
                        _createdDoc.addValue(IncomingRetention_Base.AMOUNTVALUE, parseBigDecimal(amount4Create[i]));
                        new IncomingRetention().create4Doc(_parameter, _createdDoc, i);
                    } else if ("IncomingDetraction".equalsIgnoreCase(valueDoc)) {
                        _createdDoc.addValue(IncomingDetraction_Base.AMOUNTVALUE, parseBigDecimal(amount4Create[i]));
                        new IncomingDetraction().create4Doc(_parameter, _createdDoc, i);
                    }
                }
            }
        }
    }

    protected boolean isIncomingInvoiceValid(final Parameter _parameter,
                                             final CreatedDoc _createdDoc,
                                             final int _index)
    {
        boolean ret = false;
        if (!_createdDoc.getPositions().isEmpty() &&
                        CISales.IncomingInvoice.getType().equals(_createdDoc.getPositions().get(_index).getType())) {
            ret = true;
        }
        return ret;
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

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return listmap for autocomplete
     * @throws EFapsException on error
     */
    public Return autoComplete4CreateDocument(final Parameter _parameter)
        throws EFapsException
    {

        final Instance contactInst = Instance.get(_parameter.getParameterValue("contact"));
        final boolean check = !"true".equalsIgnoreCase(_parameter.getParameterValue("checkbox4Invoice"));

        final String input = (String) _parameter.get(ParameterValues.OTHERS);

        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();

        final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter);
        queryBldr.addWhereAttrMatchValue(CISales.DocumentAbstract.Name, input + "*").setIgnoreCase(true);
        queryBldr.addOrderByAttributeAsc(CISales.DocumentAbstract.Date);
        queryBldr.addOrderByAttributeAsc(CISales.DocumentAbstract.Name);

        if (contactInst.isValid() && check) {
            queryBldr.addWhereAttrEqValue(CISales.DocumentAbstract.Contact, contactInst.getId());
        }
        InterfaceUtils.addMaxResult2QueryBuilder4AutoComplete(_parameter, queryBldr);

        add2QueryBldr4autoComplete4CreateDocument(_parameter, queryBldr);

        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.DocumentAbstract.Date,
                        CISales.DocumentAbstract.Name,
                        CISales.DocumentSumAbstract.RateCrossTotal);
        final SelectBuilder selCur = new SelectBuilder()
                        .linkto(CISales.DocumentSumAbstract.RateCurrencyId).instance();
        multi.addSelect(selCur);
        multi.setEnforceSorted(true);
        multi.execute();

        while (multi.next()) {
            final String name = multi.<String>getAttribute(CISales.DocumentAbstract.Name);
            final String oid = multi.getCurrentInstance().getOid();
            final DateTime date = multi.<DateTime>getAttribute(CISales.DocumentAbstract.Date);

            final StringBuilder choice = new StringBuilder()
                            .append(name).append(" - ").append(Instance.get(oid).getType().getLabel())
                            .append(" - ").append(date.toString(DateTimeFormat.forStyle("S-")
                                            .withLocale(Context.getThreadContext().getLocale())));
            if (multi.getCurrentInstance().getType().isKindOf(CISales.DocumentSumAbstract.getType())) {
                final BigDecimal amount = multi
                                .<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
                final CurrencyInst curr = new CurrencyInst(multi.<Instance>getSelect(selCur));
                choice.append(" - ").append(curr.getSymbol()).append(" ")
                                .append(getTwoDigitsformater().format(amount));
            }
            final Map<String, String> map = new HashMap<String, String>();
            map.put(EFapsKey.AUTOCOMPLETE_KEY.getKey(), oid);
            map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), name);
            map.put(EFapsKey.AUTOCOMPLETE_CHOICE.getKey(), choice.toString());
            list.add(map);
        }

        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }


    /**
     * Method to override if a default value is required for a document.
     *
     * @param _parameter as passed from eFaps API
     * @return DropDownfield
     * @throws EFapsException on error.
     */
    public Return dropDown4AccountFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final org.efaps.esjp.common.uiform.Field field = new org.efaps.esjp.common.uiform.Field()
        {
            @Override
            protected void add2QueryBuilder4List(final Parameter _parameter,
                                                 final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final Map<Integer, String> activations = analyseProperty(_parameter, "Activation");
                final List<AccountCDActivation> pactivt = new ArrayList<AccountCDActivation>();
                for (final String activation : activations.values()) {
                    final AccountCDActivation pDAct = AccountCDActivation.valueOf(activation);
                    pactivt.add(pDAct);
                }
                if (!pactivt.isEmpty()) {
                    _queryBldr.addWhereAttrEqValue(CISales.AccountCashDesk.Activation, pactivt.toArray());
                }
            };
        };
        return field.dropDownFieldValue(_parameter);
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

        if (amount2Pay.compareTo(BigDecimal.ZERO) == 0) {
            Context.getThreadContext().removeSessionAttribute(AbstractPaymentDocument_Base.CHANGE_AMOUNT);
        } else {
            Context.getThreadContext().setSessionAttribute(AbstractPaymentDocument_Base.CHANGE_AMOUNT, true);
        }

        return retVal;
    }

    protected BigDecimal getAmount4Pay(final Parameter _parameter)
        throws EFapsException
    {
        return parseBigDecimal(_parameter.getParameterValue("amount"));
    }

    public Return updateFields4CreateDocument(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();
        final int selected = getSelectedRow(_parameter);
        final Instance docInstance = Instance.get(_parameter.getParameterValues("createDocument")[selected]);
        final Instance accInstance = Instance.get(CISales.AccountCashDesk.getType(),
                        _parameter.getParameterValue("account"));
        if (docInstance.isValid() && accInstance.isValid()) {
            final AccountInfo accInfo = new AccountInfo(accInstance);
            final DocumentInfo docInfo = getNewDocumentInfo(docInstance);
            docInfo.setAccountInfo(accInfo);
            docInfo.setRateOptional(getRateObject(_parameter));

            final BigDecimal total4Doc = docInfo.getCrossTotal();
            final BigDecimal payments4Doc = docInfo.getTotalPayments();
            final BigDecimal amount4PayDoc = total4Doc.subtract(payments4Doc);

            map.put("createDocumentContact", docInfo.getContactName());
            map.put("createDocumentDesc", docInfo.getInfoOriginal());
            map.put("payment4Pay", getTwoDigitsformater().format(amount4PayDoc));
            map.put("paymentAmount", getTwoDigitsformater().format(amount4PayDoc));
            map.put("paymentAmountDesc", getTwoDigitsformater().format(BigDecimal.ZERO));
            map.put("paymentDiscount", getTwoDigitsformater().format(BigDecimal.ZERO));
            map.put("paymentRate", NumberFormatter.get().getFormatter(0, 3).format(docInfo.getObject4Rate()));
            map.put("paymentRate" + RateUI.INVERTEDSUFFIX, "" + (docInfo.getCurrencyInst().isInvert()));
            final BigDecimal update = parseBigDecimal(_parameter.getParameterValues("paymentAmount")[selected]);
            final BigDecimal totalPay4Position = getSumsPositions(_parameter).subtract(update).add(amount4PayDoc);
            if (Context.getThreadContext().getSessionAttribute(AbstractPaymentDocument_Base.CHANGE_AMOUNT) == null) {
                map.put("amount", getTwoDigitsformater().format(totalPay4Position));
                map.put("total4DiscountPay", getTwoDigitsformater().format(BigDecimal.ZERO));
            } else {
                final BigDecimal amount = parseBigDecimal(_parameter.getParameterValue("amount"));
                map.put("total4DiscountPay", getTwoDigitsformater().format(amount.subtract(totalPay4Position)));
            }
            list.add(map);
        }

        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    public Return updateFields4PaymentRate(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();
        final int selected = getSelectedRow(_parameter);
        final Instance docInstance = Instance.get(_parameter.getParameterValues("createDocument")[selected]);
        final Instance accInstance = Instance.get(CISales.AccountCashDesk.getType(),
                        _parameter.getParameterValue("account"));
        if (docInstance.isValid() && accInstance.isValid()) {
            final String amount4PayStr = _parameter.getParameterValues("payment4Pay")[selected];
            final Object[] rateObj = getRateObject(_parameter, "paymentRate", selected);
            final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                            BigDecimal.ROUND_HALF_UP);

            final AccountInfo accInfo = new AccountInfo(accInstance);
            final DocumentInfo docInfo = getNewDocumentInfo(docInstance);
            docInfo.setAccountInfo(accInfo);
            final BigDecimal total4Doc = docInfo.getRateCrossTotal();
            final BigDecimal payments4Doc = docInfo.getRateTotalPayments();
            final BigDecimal amount4PayDoc = total4Doc.subtract(payments4Doc);

            BigDecimal newAmount4PayDoc = amount4PayDoc;
            if (!accInfo.getCurrency().equals(docInfo.getRateCurrency())
                            && (docInfo.getCurrencyBase().equals(docInfo.getRateCurrency())
                                            || !docInfo.getCurrencyBase().equals(docInfo.getRateCurrency()))) {
                newAmount4PayDoc = amount4PayDoc.divide(rate, 2, BigDecimal.ROUND_HALF_UP);
            }

            map.put("payment4Pay", getTwoDigitsformater().format(newAmount4PayDoc));
            map.put("paymentAmount", getTwoDigitsformater().format(newAmount4PayDoc));
            map.put("paymentAmountDesc", getTwoDigitsformater().format(BigDecimal.ZERO));
            map.put("paymentDiscount", getTwoDigitsformater().format(BigDecimal.ZERO));
            BigDecimal total4DiscountPay = BigDecimal.ZERO;
            final BigDecimal newAmount = getSumsPositions(_parameter).subtract(parseBigDecimal(amount4PayStr));
            if (Context.getThreadContext().getSessionAttribute(AbstractPaymentDocument_Base.CHANGE_AMOUNT) == null) {
                map.put("amount", getTwoDigitsformater().format(newAmount.add(newAmount4PayDoc)));
            } else {
                total4DiscountPay = getAmount4Pay(_parameter).abs()
                                                .subtract(getSumsPositions(_parameter)).add(newAmount4PayDoc);
            }
            map.put("total4DiscountPay", getTwoDigitsformater().format(total4DiscountPay));
            list.add(map);
        }
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    public Return updateFields4PaymentDiscount(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();
        final int selected = getSelectedRow(_parameter);
        final String amountStr = _parameter.getParameterValues("payment4Pay")[selected];
        final String discountStr = _parameter.getParameterValues("paymentDiscount")[selected];
        final String payAmountStr = _parameter.getParameterValues("paymentAmount")[selected];

        final BigDecimal amount = parseBigDecimal(amountStr);
        final BigDecimal payAmount = parseBigDecimal(payAmountStr);
        final BigDecimal discount = parseBigDecimal(discountStr);

        final BigDecimal discAmount = amount.multiply(discount.divide(new BigDecimal(100)))
                                                .setScale(2, BigDecimal.ROUND_HALF_UP);
        map.put("paymentAmount", getTwoDigitsformater().format(amount.subtract(discAmount)));
        map.put("paymentAmountDesc", getTwoDigitsformater().format(amount.subtract(amount.subtract(discAmount))));
        final BigDecimal recalculatePos = getSumsPositions(_parameter)
                                                    .subtract(payAmount).add(amount.subtract(discAmount));
        BigDecimal total4DiscountPay = BigDecimal.ZERO;
        if (Context.getThreadContext().getSessionAttribute(AbstractPaymentDocument_Base.CHANGE_AMOUNT) == null) {
            map.put("amount", getTwoDigitsformater().format(recalculatePos));
        } else {
            total4DiscountPay = getAmount4Pay(_parameter).abs().subtract(recalculatePos);
        }
        map.put("total4DiscountPay", getTwoDigitsformater().format(total4DiscountPay));
        list.add(map);

        final Return ret = new Return();
        ret.put(ReturnValues.VALUES, list);
        return ret;
    }

    protected BigDecimal parseBigDecimal(final String _value)
        throws EFapsException
    {
        final DecimalFormat formater = NumberFormatter.get().getFormatter();
        BigDecimal ret = BigDecimal.ZERO;
        try {
            if (_value != null && !_value.isEmpty()) {
                ret = (BigDecimal) formater.parse(_value);
            }
        } catch (final ParseException e) {
            ret = BigDecimal.ZERO;
        }
        return ret;
    }

    public Return updateFields4PaymentAmount(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();
        final int selected = getSelectedRow(_parameter);
        final Instance docInstance = Instance.get(_parameter.getParameterValues("createDocument")[selected]);
        final Instance accInstance = Instance.get(CISales.AccountCashDesk.getType(),
                                                        _parameter.getParameterValue("account"));
        if (docInstance.isValid() && accInstance.isValid()) {
            final String payStr = _parameter.getParameterValues("paymentAmount")[selected];
            final String amount4PayStr = _parameter.getParameterValues("payment4Pay")[selected];

            final BigDecimal pay = parseBigDecimal(payStr);
            final BigDecimal amount4PayTotal = parseBigDecimal(amount4PayStr);

            map.put("paymentAmount", getTwoDigitsformater().format(pay));
            map.put("paymentAmountDesc", getTwoDigitsformater().format(amount4PayTotal.subtract(pay)));
            map.put("paymentDiscount", getTwoDigitsformater().format(BigDecimal.ZERO));
            BigDecimal total4DiscountPay = BigDecimal.ZERO;
            if (Context.getThreadContext().getSessionAttribute(AbstractPaymentDocument_Base.CHANGE_AMOUNT) == null) {
                map.put("amount", getTwoDigitsformater().format(getSumsPositions(_parameter)));
            } else {
                total4DiscountPay = getAmount4Pay(_parameter).abs().subtract(getSumsPositions(_parameter));
            }
            map.put("total4DiscountPay", getTwoDigitsformater().format(total4DiscountPay));
            list.add(map);
        }
        final Return retVal = new Return();
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
        final String[] paymentPosAmount = _parameter.getParameterValues("paymentAmount");
        for (int i = 0; i < getPaymentCount(_parameter); i++) {
            ret = ret.add(parseBigDecimal(paymentPosAmount[i]));
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
                Context.getThreadContext().removeSessionAttribute(AbstractPaymentDocument_Base.INVOICE_SESSIONKEY);
            }
        } else {
            Context.getThreadContext().removeSessionAttribute(AbstractPaymentDocument_Base.INVOICE_SESSIONKEY);
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
        map.put("rate", NumberFormatter.get().getFormatter(0, 3).format(rates[3]));
        map.put("rate" + RateUI.INVERTEDSUFFIX, "" + (rates[3].compareTo(rates[0]) != 0));
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
            Context.getThreadContext().removeSessionAttribute(AbstractPaymentDocument_Base.INVOICE_SESSIONKEY);
        }
        Context.getThreadContext().setSessionAttribute(AbstractPaymentDocument_Base.CONTACT_SESSIONKEY, contact);
        return new Return();
    }

    public Return deactivateFiltered4Invoice(final Parameter _parameter)
        throws EFapsException
    {
        Context.getThreadContext().removeSessionAttribute(AbstractPaymentDocument_Base.INVOICE_SESSIONKEY);
        Context.getThreadContext().removeSessionAttribute(AbstractPaymentDocument_Base.CONTACT_SESSIONKEY);
        Context.getThreadContext().removeSessionAttribute(AbstractPaymentDocument_Base.CHANGE_AMOUNT);
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
        return NumberFormatter.get().getTwoDigitsFormatter();
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
        return NumberFormatter.get().getZeroDigitsFormatter();
    }

    protected Object[] getRateObject(final Parameter _parameter)
        throws EFapsException
    {
        BigDecimal rate = BigDecimal.ONE;
        try {
            rate = (BigDecimal) RateFormatter.get().getFrmt4Rate().parse(_parameter.getParameterValue("rate"));
        } catch (final ParseException e) {
            throw new EFapsException(AbstractDocument_Base.class, "analyzeRate.ParseException", e);
        }
        final boolean rInv = "true".equalsIgnoreCase(_parameter.getParameterValue("rate" + RateUI.INVERTEDSUFFIX));
        return new Object[] { rInv ? BigDecimal.ONE : rate, rInv ? rate : BigDecimal.ONE };
    }

    protected Object[] getRateObject(final Parameter _parameter,
                                     final String _rate,
                                     final int _index)
        throws EFapsException
    {
        BigDecimal rate = BigDecimal.ONE;
        try {
            rate = (BigDecimal) RateFormatter.get().getFrmt4Rate().parse(_parameter.getParameterValues(_rate)[_index]);
        } catch (final ParseException e) {
            throw new EFapsException(AbstractDocument_Base.class, "analyzeRate.ParseException", e);
        }
        final boolean rInv = "true".equalsIgnoreCase(_parameter.getParameterValues(_rate + RateUI.INVERTEDSUFFIX)[_index]);
        return new Object[] { rInv ? BigDecimal.ONE : rate, rInv ? rate : BigDecimal.ONE };
    }

    protected String getCode4GeneratedDocWithSysConfig(final Parameter _parameter)
        throws EFapsException
    {
        String ret = "";
        // Sales-Configuration
        final SystemConfiguration config = Sales.getSysConfig();
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

    /**
     * @param _parameter Parameter as passed from the EFaps API.
     * @return new Return with SNIPPLET.
     * @throws EFapsException
     */
    public Return validatePaymentDocument4Positions(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();

        if (!evaluateDocument4PositionDoc(_parameter).toString().isEmpty()) {
            ret.put(ReturnValues.SNIPLETT, evaluateDocument4PositionDoc(_parameter).toString());
        } else {
            ret.put(ReturnValues.TRUE, true);
        }

        return ret;
    }

    /**
     * Method to analyze positions and check is property validate Documents.
     *
     * @param _parameter Parameter as passed from the EFaps API.
     * @return new HtmlTable.
     * @throws EFapsException on error.
     */
    protected HtmlTable evaluateDocument4PositionDoc(final Parameter _parameter)
        throws EFapsException
    {
        final HtmlTable html = new HtmlTable();
        final Map<Integer, String> map = analyseProperty(_parameter, "Document");

        final String[] paymentDocs = _parameter.getParameterValues("createDocument");
        final String[] paymentAutoDocs = _parameter.getParameterValues("createDocumentAutoComplete");
        for (int i = 0; i < getPaymentCount(_parameter); i++) {
            boolean exists = false;
            final Instance document = Instance.get(paymentDocs[i]);
            for (final Entry<Integer, String> entryMap : map.entrySet()) {
                final Type type = Type.get(entryMap.getValue());
                if (type != null && document.isValid()) {
                    if (type.equals(document.getType())) {
                        exists = true;
                        break;
                    }
                }
            }
            if (!exists) {
                html.tr()
                    .td(document.getType().getLabel())
                    .td(paymentAutoDocs[i])
                    .trC();
            }
        }

        final HtmlTable html2 = new HtmlTable();
        if (!html.toString().isEmpty()) {
            html2.table()
                .th(DBProperties.getProperty("Sales_DocumentAbstract/Type.Label"))
                .th(DBProperties.getProperty("Sales_DocumentAbstract/Name.Label"))
                .append(html.toString())
                .tableC();
        }

        return html2;
    }

    protected void connectPaymentDocument2Document(final Parameter _parameter,
                                                   final CreatedDoc _createdDoc)
        throws EFapsException
    {
        // to be implemented
    }

    public Return createReportDoc(final Parameter _parameter,
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
                name = _createdDoc.getValues().get(CISales.PaymentDocumentAbstract.Name.name);
            }

            final String fileName = DBProperties.getProperty(_createdDoc.getInstance().getType().getName() + ".Label")
                            + "_" + name;
            report.setFileName(fileName);
            final SelectBuilder selCurName = new SelectBuilder().linkto(CISales.AccountCashDesk.CurrencyLink)
                            .attribute(CIERP.Currency.Name);

            if (_createdDoc.getValues().containsKey("accountName")) {
                report.getJrParameters().put("accountName", _createdDoc.getValues().get("accountName"));
                report.getJrParameters().put("accountCurrencyName", _createdDoc.getValues().get("accountCurrencyName"));
            } else {
                report.getJrParameters().put("accountName",
                                getSelectString4AttributeAccount((String) _createdDoc.getValues()
                                                .get(CISales.TransactionAbstract.Account.name),
                                                null, CISales.AccountCashDesk.Name));
                report.getJrParameters().put("accountCurrencyName",
                                getSelectString4AttributeAccount((String) _createdDoc.getValues()
                                                .get(CISales.TransactionAbstract.Account.name), selCurName, null));
            }
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

            if (_parameter.getInstance().getType().isKindOf(CISales.PaymentDocumentOutAbstract.getType())) {
                report.getJrParameters().put("ClientOrSupplier", DBProperties.getProperty(
                                "org.efaps.esjp.sales.payment.AbstractDocumentOutPaymentSupplier.Label"));
            } else if (_parameter.getInstance().getType().isKindOf(CISales.PaymentDocumentAbstract.getType())) {
                report.getJrParameters().put("ClientOrSupplier", DBProperties.getProperty(
                                "org.efaps.esjp.sales.payment.AbstractDocumentInPaymentClient.Label"));
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
        throws EFapsException
    {
        // used by implementation
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
        inverseTransactions(_parameter, _parameter.getInstance(), true);
        final Update updatePayment = new Update(_parameter.getInstance());
        updatePayment.add(CISales.Payment.Amount, BigDecimal.ZERO);
        updatePayment.executeWithoutAccessCheck();

        return new StatusValue().setStatus(_parameter);
    }

    /**
     * Inverse the transactions of a PaymentDocument.
     * @param _parameter Parameter as passed by the eFaps API
     * @param _instance instance of the Payment Document
     * @param _isTargetDocument is it a targetDocument (PaymentDocument)
     * @throws EFapsException on error
     */
    protected void inverseTransactions(final Parameter _parameter,
                                       final Instance _instance,
                                       final boolean _isTargetDocument)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CISales.Payment);
        if (_isTargetDocument) {
            queryBldr.addWhereAttrEqValue(CISales.Payment.TargetDocument, _instance);
        } else {
            queryBldr.addWhereAttrEqValue(CISales.Payment.CreateDocument, _instance);
        }
        final InstanceQuery queryInst = queryBldr.getQuery();
        queryInst.execute();
        while (queryInst.next()) {
            final QueryBuilder queryBldr2 = new QueryBuilder(CISales.TransactionAbstract);
            queryBldr2.addWhereAttrEqValue(CISales.TransactionAbstract.Payment, queryInst.getCurrentValue());
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
                insert.add(CISales.TransactionAbstract.Amount,
                                multi.<BigDecimal>getAttribute(CISales.TransactionAbstract.Amount));
                insert.add(CISales.TransactionAbstract.CurrencyId,
                                multi.<Long>getAttribute(CISales.TransactionAbstract.CurrencyId));
                insert.add(CISales.TransactionAbstract.Account,
                                multi.<Long>getAttribute(CISales.TransactionAbstract.Account));
                insert.add(CISales.TransactionAbstract.Payment, queryInst.getCurrentValue().getId());
                insert.add(CISales.TransactionAbstract.Description, DBProperties.getProperty(
                                AbstractPaymentDocument.class.getName() +  ".correctionPayment"));
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
                    sumPayments = sumPayments.add(restAmount);
                    break;
                } else {
                    restAmount = restAmount.subtract(amountDue);
                    sumPayments = sumPayments.add(amountDue);
                }

            }
        }

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

        final List<Map<String, Object>> values = new ArrayList<Map<String, Object>>();

        int index = 0;
        boolean lastPos = false;
        for (final Instance payment : _instancesList) {
            final Map<String, Object> map = new HashMap<String, Object>();
            values.add(map);
            if (_instancesList.size() == index + 1) {
                lastPos = true;
            }
            add2MapPositions(_parameter, payment, index, lastPos, _restAmount, _currencyActual, map);
            index++;
        }

        js.append("\n");
        js.append(getTableAddNewRowsScript(_parameter, "paymentTable", values, null));

        final BigDecimal total4DiscountPay = getAmount4Pay(_parameter).abs().subtract(_sumPayments);
        js.append(getSetFieldValue(0, CIFormSales.Sales_PaymentCheckWithOutDocForm.total4DiscountPay.name,
                        total4DiscountPay == null ? BigDecimal.ZERO.toString() : getTwoDigitsformater().format(total4DiscountPay)));

        return js;
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
    protected void add2MapPositions(final Parameter _parameter,
                                          final Instance _instance,
                                          final Integer _index,
                                          final boolean _lastPosition,
                                          final BigDecimal _restAmount,
                                    final Instance _currencyActual,
                                    final Map<String, Object> _map)
        throws EFapsException
    {
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
            bldr.append(getTwoDigitsformater().format(rateCrossTotal)).append(" / ")
                .append(getTwoDigitsformater().format(payments4Doc)).append(" - ").append(symbol);

            _map.put(CITableSales.Sales_PaymentCheckWithOutDocPaymentTable.createDocument.name, _instance.getOid());
            _map.put(CITableSales.Sales_PaymentCheckWithOutDocPaymentTable.createDocument.name + "AutoComplete", name);
            _map.put(CITableSales.Sales_PaymentCheckWithOutDocPaymentTable.createDocumentDesc.name, bldr.toString());

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
                _map.put(CITableSales.Sales_PaymentCheckWithOutDocPaymentTable.paymentAmount.name, amountDue == null
                                ? BigDecimal.ZERO.toString() : getTwoDigitsformater().format(amountDueConverted));
                pay = amountDue;
            } else {
                if (amount2Pay.compareTo(pay) != 0) {
                    _map.put(CITableSales.Sales_PaymentCheckWithOutDocPaymentTable.paymentAmount.name,
                                    _restAmount == null
                                                    ? BigDecimal.ZERO.toString() : getTwoDigitsformater().format(
                                                                    amountDueConverted));
                    pay = amountDue;
                } else {
                    _map.put(CITableSales.Sales_PaymentCheckWithOutDocPaymentTable.paymentAmount.name,
                                    _restAmount == null
                                                    ? BigDecimal.ZERO.toString() : getTwoDigitsformater().format(
                                                                    _restAmount));
                    pay = restAmountConverted;
                }
            }

            _map.put(CITableSales.Sales_PaymentCheckWithOutDocPaymentTable.paymentAmountDesc.name,
                            getTwoDigitsformater().format(amount2Pay.subtract(pay)));
        }

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

    /**
     * Method to update fields with document selected.
     *
     * @param _parameter Parameter from eFaps API.
     * @return return with values.
     * @throws EFapsException on error.
     */
    public Return updateFields4DocumentSelected(final Parameter _parameter)
        throws EFapsException
    {
        Context.getThreadContext().removeSessionAttribute(AbstractPaymentDocument_Base.INVOICE_SESSIONKEY);
        Context.getThreadContext().removeSessionAttribute(AbstractPaymentDocument_Base.CONTACT_SESSIONKEY);

        final Return ret = new Return();

        final Instance selectDoc = Instance.get(_parameter.getParameterValue("name"));

        final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        if (selectDoc.isValid()) {
            final SelectBuilder selContact = new SelectBuilder().linkto(CISales.DocumentAbstract.Contact);
            final SelectBuilder selContactOid = new SelectBuilder(selContact).oid();
            final SelectBuilder selContactName = new SelectBuilder(selContact).attribute(CIContacts.Contact.Name);

            final Map<Integer, String> fields = analyseProperty(_parameter, "Fields");
            if (!fields.isEmpty()) {
                final PrintQuery print = new PrintQuery(selectDoc);
                print.addAttribute(CISales.DocumentSumAbstract.Date,
                                CISales.DocumentSumAbstract.DueDate,
                                CISales.DocumentSumAbstract.RateCrossTotal);
                print.addSelect(selContactOid, selContactName);
                print.execute();

                for (final Entry<Integer, String> field : fields.entrySet()) {
                    String value;
                    String value2 = null;
                    if (field.getValue().equalsIgnoreCase("amount")) {
                        final BigDecimal amount =
                                        print.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
                        if (amount.compareTo(BigDecimal.ZERO) == 0) {
                            Context.getThreadContext()
                                            .removeSessionAttribute(AbstractPaymentDocument_Base.CHANGE_AMOUNT);
                        } else {
                            Context.getThreadContext()
                                            .setSessionAttribute(AbstractPaymentDocument_Base.CHANGE_AMOUNT, true);
                        }
                        value = getTwoDigitsformater().format(amount);
                    } else if (field.getValue().equalsIgnoreCase("contact")) {
                        value = print.<String>getSelect(selContactOid);
                        value2 = print.<String>getSelect(selContactName);
                    } else if (field.getValue().equalsIgnoreCase("date_eFapsDate")) {
                        final DateTime date = print.<DateTime>getAttribute(CISales.DocumentSumAbstract.Date);
                        value = date.toString("dd/MM/YY");
                    } else if (field.getValue().equalsIgnoreCase("dueDate_eFapsDate")) {
                        final DateTime dueDate = print.<DateTime>getAttribute(CISales.DocumentSumAbstract.DueDate);
                        value = dueDate.toString("dd/MM/YY");
                    } else {
                        value = "";
                    }
                    if (value != null && !value.isEmpty()) {
                        if (value2 == null) {
                            map.put(field.getValue(), StringEscapeUtils.escapeEcmaScript(value));
                        } else {
                            map.put(field.getValue(), new String[] { StringEscapeUtils.escapeEcmaScript(value),
                                            StringEscapeUtils.escapeEcmaScript(value2) });
                        }
                    }
                }
                if (map.containsKey("contact")) {
                    final Instance contactInst = Instance.get(((String[]) map.get("contact"))[0]);
                    if (contactInst.isValid()) {
                        Context.getThreadContext()
                                        .setSessionAttribute(AbstractPaymentDocument_Base.INVOICE_SESSIONKEY,
                                                        contactInst);
                        Context.getThreadContext()
                                        .setSessionAttribute(AbstractPaymentDocument_Base.CONTACT_SESSIONKEY,
                                                        contactInst);
                    }
                }
                list.add(map);
            }
        }
        ret.put(ReturnValues.VALUES, list);
        return ret;
    }

    public class AccountInfo
    {
        private final Instance instance;

        private Instance currency;

        public AccountInfo(final Instance _instance)
            throws EFapsException
        {
            this.instance = _instance;
            if (this.instance.isValid()) {
                final SelectBuilder selAccCurInst = new SelectBuilder()
                                .linkto(CISales.AccountAbstract.CurrencyLink).instance();
                final PrintQuery print = new PrintQuery(this.instance);
                print.addSelect(selAccCurInst);
                print.execute();

                this.currency = print.<Instance>getSelect(selAccCurInst);
            }
        }

        protected Instance getCurrency()
        {
            return this.currency;
        }

        protected CurrencyInst getCurrencyInst()
        {
            return new CurrencyInst(this.currency);
        }
    }

    public DocumentInfo getNewDocumentInfo(final Instance _instance)
        throws EFapsException
    {
        return new DocumentInfo(_instance);
    }

    public class DocumentInfo
    {
        private final Instance instance;

        private Instance rateCurrency;

        private BigDecimal rateCrossTotal;

        private String symbol;

        private Object[] rate;

        private Object[] rateOptional;

        private Instance curBase;

        private List<Instance> lstPayments = new ArrayList<Instance>();

        private AccountInfo accountInfo;

        public DocumentInfo(final Instance _instance)
            throws EFapsException
        {
            this.instance = _instance;
            if (this.instance.isValid()) {
                final SelectBuilder selDocRate = new SelectBuilder()
                                .linkto(CISales.DocumentSumAbstract.RateCurrencyId);
                final SelectBuilder selDocRCurInst = new SelectBuilder(selDocRate).instance();
                final SelectBuilder selDocRCurSymbol = new SelectBuilder(selDocRate).attribute(CIERP.Currency.Symbol);

                final PrintQuery print = new PrintQuery(this.instance);
                print.addAttribute(CISales.DocumentSumAbstract.Rate, CISales.DocumentSumAbstract.RateCrossTotal);
                print.addSelect(selDocRCurInst, selDocRCurSymbol);
                print.execute();

                this.rate = print.<Object[]>getAttribute(CISales.DocumentSumAbstract.Rate);
                this.rateCrossTotal = print.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
                this.rateCurrency = print.<Instance>getSelect(selDocRCurInst);
                this.symbol = print.<String>getSelect(selDocRCurSymbol);
                this.curBase = Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);
                this.rateOptional = null;
                getPayments4Document();
            }
        }

        protected void setAccountInfo(final AccountInfo _accountInfo)
        {
            this.accountInfo = _accountInfo;
        }

        protected void setRateOptional(final Object[] _rate)
        {
            this.rateOptional = _rate;
        }

        protected Instance getInstance()
        {
            return this.instance;
        }

        protected Instance getRateCurrency()
        {
            return this.rateCurrency;
        }

        protected Instance getCurrencyBase()
        {
            return this.curBase;
        }

        protected String getRateSymbol()
        {
            return this.symbol;
        }

        protected CurrencyInst getCurrencyInst()
        {
            return new CurrencyInst(this.rateCurrency);
        }

        protected BigDecimal getCrossTotal()
            throws EFapsException
        {
            BigDecimal ret = this.rateCrossTotal.add(getRateCrossTotal4Query());
            if (!this.rateCurrency.equals(this.accountInfo.getCurrency())) {
                ret = ret.multiply((BigDecimal) this.rate[1]).setScale(2, BigDecimal.ROUND_HALF_UP);
                if (!this.accountInfo.getCurrency().equals(this.curBase)
                                && this.rateCurrency.equals(this.curBase)) {
                    if (this.rateOptional != null) {
                        ret = ret.divide((BigDecimal) this.rateOptional[1], BigDecimal.ROUND_HALF_UP)
                                        .setScale(2, BigDecimal.ROUND_HALF_UP);
                    }
                }
            }
            return ret;
        }

        protected BigDecimal getRateCrossTotal()
        {
            return this.rateCrossTotal;
        }

        protected BigDecimal getTotalPayments()
            throws EFapsException
        {
            BigDecimal ret = getRateTotalPayments();
            if (!this.rateCurrency.equals(this.accountInfo.getCurrency())) {
                ret = ret.multiply((BigDecimal) this.rate[1]).setScale(2, BigDecimal.ROUND_HALF_UP);
                if (this.accountInfo.getCurrency().equals(this.curBase)
                                && !this.rateCurrency.equals(this.curBase)) {
                    if (this.rateOptional != null) {
                        ret = ret.divide((BigDecimal) this.rateOptional[1], BigDecimal.ROUND_HALF_UP)
                                        .setScale(2, BigDecimal.ROUND_HALF_UP);
                    }
                } else if (!this.accountInfo.getCurrency().equals(this.curBase)
                                && this.rateCurrency.equals(this.curBase)) {
                    if (this.rateOptional != null) {
                        ret = ret.divide((BigDecimal) this.rateOptional[1], BigDecimal.ROUND_HALF_UP)
                                        .setScale(2, BigDecimal.ROUND_HALF_UP);
                    }
                }
            }
            return ret;
        }

        protected void getPayments4Document()
            throws EFapsException
        {
            final QueryBuilder queryBldr = new QueryBuilder(CISales.Payment);
            queryBldr.addWhereAttrEqValue(CISales.Payment.CreateDocument, this.instance);
            this.lstPayments = queryBldr.getQuery().executeWithoutAccessCheck();
        }

        protected BigDecimal getRateTotal4TaxDocumentType(final CIType _type)
            throws EFapsException
        {
            BigDecimal ret = BigDecimal.ZERO;
            if (this.instance.isValid() && _type != null) {
                final SelectBuilder selDoc = new SelectBuilder()
                                .linkto(CISales.IncomingDocumentTax2Document.FromAbstractLink);
                final SelectBuilder selDate = new SelectBuilder(selDoc).attribute(CIERP.DocumentAbstract.Date);
                final SelectBuilder selRate = new SelectBuilder(selDoc).attribute(CISales.DocumentSumAbstract.Rate);
                final SelectBuilder selRateCross = new SelectBuilder(selDoc)
                                .attribute(CISales.DocumentSumAbstract.RateCrossTotal);

                final QueryBuilder queryBldr = new QueryBuilder(_type);
                queryBldr.addWhereAttrEqValue(CISales.IncomingDocumentTax2Document.ToAbstractLink, this.instance);
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addSelect(selDate, selRate, selRateCross);
                multi.execute();
                while (multi.next()) {
                    final Object[] rateObj = multi.<Object[]>getSelect(selRate);
                    final BigDecimal amount = multi.<BigDecimal>getSelect(selRateCross);
                    final BigDecimal rate = ((BigDecimal) rateObj[0])
                                    .divide((BigDecimal) rateObj[1], 12, BigDecimal.ROUND_HALF_UP);
                    ret = ret.add(amount.multiply(rate).setScale(2, BigDecimal.ROUND_HALF_UP));
                }
            }
            return ret;
        }

        protected BigDecimal getRateTotalPayments()
            throws EFapsException
        {
            BigDecimal ret = BigDecimal.ZERO;
            if (this.instance.isValid() && !this.lstPayments.isEmpty()) {
                final MultiPrintQuery multi = new MultiPrintQuery(this.lstPayments);
                multi.addAttribute(CISales.Payment.Amount,
                                CISales.Payment.Date,
                                CISales.Payment.Rate);
                multi.execute();
                while (multi.next()) {
                    final Object[] rateObj = multi.<Object[]>getAttribute(CISales.Payment.Rate);
                    final BigDecimal amount = multi.<BigDecimal>getAttribute(CISales.Payment.Amount);
                    final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                                    BigDecimal.ROUND_HALF_UP);
                    ret = ret.add(amount.multiply(rate).setScale(2, BigDecimal.ROUND_HALF_UP));
                }
            }
            return ret;
        }

        protected Object[] getDocRate()
        {
            return this.rate;
        }

        protected Object[] getOptionalRate()
        {
            return this.rateOptional;
        }

        protected String getInfoOriginal()
            throws EFapsException
        {
            return new StringBuilder().toString();
        }

        protected BigDecimal getObject4Rate()
        {
            BigDecimal ret = BigDecimal.ONE;
            if (this.rateCurrency.equals(this.accountInfo.getCurrency())
                            && this.rateCurrency.equals(this.curBase)) {
                ret = (BigDecimal) this.rateOptional[1];
            } else {
                if (!(this.rateCurrency.equals(this.accountInfo.getCurrency()))) {
                    ret = (BigDecimal) this.rate[1];
                }
            }
            return ret;
        }

        protected InstanceQuery getPaymentDerivedDocument()
            throws EFapsException
        {
            final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.PaymentDerivedDocument2Document);
            attrQueryBldr.addWhereAttrEqValue(CISales.PaymentDerivedDocument2Document.FromLink, this.instance);
            final AttributeQuery attrQuery =
                            attrQueryBldr.getAttributeQuery(CISales.PaymentDerivedDocument2Document.ToLink);

            final QueryBuilder queryBldr = new QueryBuilder(CIERP.DocumentAbstract);
            queryBldr.addWhereAttrInQuery(CIERP.DocumentAbstract.ID, attrQuery);
            return queryBldr.getQuery();
        }

        protected BigDecimal getTotals4PaymentDerivedDocument()
            throws EFapsException
        {
            return this.instance.isValid() ? getRateCrossTotal4Query() : BigDecimal.ZERO;
        }

        protected BigDecimal getRateCrossTotal4Query()
            throws EFapsException
        {
            return BigDecimal.ZERO;
        }

        protected BigDecimal compareDocs(final Instance _payDerivedDoc)
            throws EFapsException
        {
            BigDecimal ret = BigDecimal.ZERO;
            if (_payDerivedDoc.getType().isKindOf(CISales.DocumentSumAbstract.getType())) {
                final PrintQuery print = new PrintQuery(_payDerivedDoc);
                print.addAttribute(CISales.DocumentSumAbstract.RateCurrencyId,
                                CISales.DocumentSumAbstract.RateCrossTotal);
                print.executeWithoutAccessCheck();

                if (print.<Long>getAttribute(CISales.DocumentSumAbstract.RateCurrencyId) == getRateCurrency().getId()) {
                    ret = print.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
                }
            }
            return ret.setScale(2, BigDecimal.ROUND_HALF_UP);
        }

        protected String getContactName()
            throws EFapsException
        {
            final StringBuilder ret = new StringBuilder();
            if (this.instance != null && this.instance.isValid()) {
                final SelectBuilder selContactName = new SelectBuilder()
                            .linkto(CISales.DocumentAbstract.Contact).attribute(CIContacts.Contact.Name);
                final PrintQuery print = new PrintQuery(this.instance);
                print.addSelect(selContactName);
                print.execute();

                ret.append(print.<String>getSelect(selContactName));
            }
            return ret.toString();
        }
    }

    /**
     * @param _parameter    Parameter as passed by the eFasp API
     * @param _payDocInst instance of a PaymentDocument
     * @return StringBuilder
     * @throws EFapsException on error
     */
    public static StringBuilder getTransactionHtml(final Parameter _parameter,
                                                   final Instance _payDocInst)
        throws EFapsException
    {
        final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Payment);
        attrQueryBldr.addWhereAttrEqValue(CISales.Payment.TargetDocument, _payDocInst);
        final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.Payment.ID);

        final QueryBuilder queryBldr = new QueryBuilder(CISales.TransactionAbstract);
        queryBldr.addWhereAttrInQuery(CISales.TransactionAbstract.Payment, attrQuery);
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selCurInst = SelectBuilder.get().linkto(CISales.TransactionAbstract.CurrencyId).instance();
        final SelectBuilder selAccName = SelectBuilder.get().linkto(CISales.TransactionAbstract.Account)
                        .attribute(CISales.AccountAbstract.Name);
        final SelectBuilder selAccDescr = SelectBuilder.get().linkto(CISales.TransactionAbstract.Account)
                        .attribute(CISales.AccountAbstract.Description);
        multi.addSelect(selCurInst, selAccName, selAccDescr);
        multi.addAttribute(CISales.TransactionAbstract.Amount, CISales.TransactionAbstract.Description);
        multi.execute();
        final HtmlTable html = new HtmlTable();
        html.table();
        while (multi.next()) {
            final CurrencyInst currencyInst = new CurrencyInst(multi.<Instance>getSelect(selCurInst));
            html.tr()
                .td(multi.getCurrentInstance().getType().getLabel())
                .td(NumberFormatter.get().getTwoDigitsFormatter().format(
                            multi.getAttribute(CISales.TransactionAbstract.Amount)))
                .td(currencyInst.getISOCode())
                .td(multi.<String>getSelect(selAccName))
                .td(multi.<String>getSelect(selAccDescr))
                .trC();
        }
        html.tableC();
        return new StringBuilder(html.toString());
    }
}
