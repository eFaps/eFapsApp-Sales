/*
 * Copyright 2003 - 2010 The eFaps Team
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

package org.efaps.esjp.sales;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringEscapeUtils;
import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractCommand;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.ci.CIType;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.StandartReport;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.erp.Revision;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.esjp.erp.util.ERPSettings;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.SalesSettings;
import org.efaps.ui.wicket.util.DateUtil;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("70868417-752b-45e8-8ada-67d0b15ad35b")
@EFapsRevision("$Rev$")
public abstract class Account_Base
{

    /**
     * Method for create a new Cash Desk Balance.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return cashDeskBalance(final Parameter _parameter)
        throws EFapsException
    {
        final Instance cashDeskInstance = _parameter.getCallInstance();

        final Instance baseCurrInst = Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);

        final DateTime date = new DateTime(_parameter.getParameterValue("date"));
        final Insert insert = new Insert(CISales.CashDeskBalance);
        insert.add(CISales.CashDeskBalance.Name, new DateTime().toLocalTime());
        insert.add(CISales.CashDeskBalance.Date, date);
        insert.add(CISales.CashDeskBalance.Status, Status.find(CISales.CashDeskBalanceStatus.Closed).getId());
        insert.add(CISales.CashDeskBalance.CrossTotal, BigDecimal.ZERO);
        insert.add(CISales.CashDeskBalance.NetTotal, BigDecimal.ZERO);
        insert.add(CISales.CashDeskBalance.DiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.CashDeskBalance.RateCrossTotal, BigDecimal.ZERO);
        insert.add(CISales.CashDeskBalance.RateNetTotal, BigDecimal.ZERO);
        insert.add(CISales.CashDeskBalance.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.CashDeskBalance.Rate, new Object[] { 1, 1 });
        insert.add(CISales.CashDeskBalance.RateCurrencyId, baseCurrInst.getId());
        insert.add(CISales.CashDeskBalance.CurrencyId, baseCurrInst.getId());
        insert.execute();

        final Instance balanceInst = insert.getInstance();
        final String[] payments = _parameter.getParameterValues("payments");
        if (payments != null) {
            for (final String payment : payments) {
                final Update update = new Update(payment);
                update.add("TargetDocument", balanceInst.getId());
                update.execute();
            }
        }

        final Insert payInsert = new Insert(CISales.Payment);
        payInsert.add(CISales.Payment.Date, new DateTime());
        payInsert.add(CISales.Payment.CreateDocument, balanceInst.getId());
        payInsert.execute();

        final Instance payInst = payInsert.getInstance();

        final String[] payIds = _parameter.getParameterValues("payId");
        final String[] currIds = _parameter.getParameterValues("currId");
        final String[] amounts = _parameter.getParameterValues("amount");
        BigDecimal crossTotal = BigDecimal.ZERO;

        for (int i = 0;  i < payIds.length; i++) {
            BigDecimal amount = BigDecimal.ZERO;
            if (amounts == null) {
                final DecimalFormat formater = (DecimalFormat) NumberFormat.getInstance(Context.getThreadContext()
                                .getLocale());
                formater.setParseBigDecimal(true);
                try {
                    amount = (BigDecimal) formater.parse(_parameter.getParameterValue("startAmount"));
                } catch (final ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                amount = new BigDecimal(amounts[i]);
            }

            final Long currId = Long.parseLong(currIds[i]);

            final BigDecimal rate = BigDecimal.ONE;

            crossTotal = crossTotal.add(amount.divide(rate, BigDecimal.ROUND_HALF_UP));
            final Insert transInsert = new Insert(CISales.TransactionOutbound);
            transInsert.add(CISales.TransactionOutbound.Amount, amount);
            transInsert.add(CISales.TransactionOutbound.CurrencyId, currId);
            transInsert.add(CISales.TransactionOutbound.Payment, payInst.getId());
            transInsert.add(CISales.TransactionOutbound.Account, cashDeskInstance.getId());
            transInsert.add(CISales.TransactionOutbound.Description,
                            DBProperties.getProperty("org.efaps.esjp.sales.TransactionOutBoundDescription.CashDeskBalance"));
            transInsert.add(CISales.TransactionOutbound.Date, new DateTime());
            transInsert.execute();
        }

        final Update update = new Update(balanceInst);
        update.add("CrossTotal", crossTotal);
        update.execute();

        return new Return();
    }

    /**
     * method for obtains a Rate of the CurrencyRateClient.
     *
     * @param _date validFrom.
     * @param _currId currency.
     * @return BigDecimal with ret.
     * @throws EFapsException on error.
     */
    protected Object[] getRate(final DateTime _date,
                                 final Long _currId)
        throws EFapsException
    {
        Object[] rates = null;
        final QueryBuilder queryBldr = new QueryBuilder(CIERP.CurrencyRateClient);
        queryBldr.addWhereAttrEqValue(CIERP.CurrencyRateClient.CurrencyLink, _currId);
        queryBldr.addWhereAttrLessValue(CIERP.CurrencyRateClient.ValidFrom, _date.plusMinutes(1));
        queryBldr.addWhereAttrGreaterValue(CIERP.CurrencyRateClient.ValidUntil, _date);

        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIERP.CurrencyRateClient.Rate);
        multi.execute();
        if (multi.next()) {
            rates = multi.<Object[]>getAttribute(CIERP.CurrencyRateClient.Rate);
        }
        return rates;
    }

    public Return revenuesFieldValueUI(final Parameter _parameter)
        throws EFapsException
    {
        final StringBuilder bldr = new StringBuilder();
        final Instance cashDeskInstance = _parameter.getCallInstance();
        final String dateStr = Context.getThreadContext().getParameter("date");
        final DateTime date = new DateTime(dateStr);
        final QueryBuilder queryBldr = new QueryBuilder(CISales.TransactionInbound);
        queryBldr.addWhereAttrEqValue(CISales.TransactionInbound.Account, cashDeskInstance.getId());
        queryBldr.addWhereAttrGreaterValue(CISales.TransactionInbound.Date, date.minusMinutes(1));
        queryBldr.addWhereAttrLessValue(CISales.TransactionInbound.Date, date.plusDays(1));
        final InstanceQuery query = queryBldr.getQuery();
        final List<Instance> instances = query.execute();

        bldr.append("<input type=\"hidden\" name=\"date\" value=\"").append(dateStr).append("\"/>");
        if (instances.size() > 0) {
            final MultiPrintQuery multi = new MultiPrintQuery(instances);
            multi.addAttribute("Amount");
            multi.addSelect("linkto[Payment].oid");
            multi.addSelect("linkto[Payment].attribute[TargetDocument]");
            multi.addSelect("linkto[PaymentType].attribute[Value]");
            multi.addSelect("linkto[CurrencyId].attribute[Name]");
            multi.addAttribute("PaymentType");
            multi.addAttribute("CurrencyId");
            multi.execute();
            final Map<String, BigDecimal> map = new TreeMap<String, BigDecimal>();
            final Map<String, Long[]> key2Ids = new TreeMap<String, Long[]>();
            final Set<String> oids = new HashSet<String>();
            while (multi.next()) {
                if (multi.getSelect("linkto[Payment].attribute[TargetDocument]") == null) {
                    final String oid = multi.<String>getSelect("linkto[Payment].oid");
                    if (!oids.contains(oid)) {
                        bldr.append("<input type=\"hidden\" name=\"payments\" value=\"")
                            .append(oid).append("\"/>");
                        oids.add(oid);
                    }
                    final Long payId = multi.<Long>getAttribute("PaymentType");
                    final Long currId = multi.<Long>getAttribute("CurrencyId");
                    final String payType = multi.<String>getSelect("linkto[PaymentType].attribute[Value]");
                    final String currName = multi.<String>getSelect("linkto[CurrencyId].attribute[Name]");
                    final String key = payType + " - " + currName + ":";
                    final BigDecimal temp = (BigDecimal) multi.getAttribute("Amount");
                    BigDecimal total = map.get(key);
                    if (total == null) {
                        total = BigDecimal.ZERO;
                    }
                    total = total.add(temp);
                    map.put(key, total);
                    key2Ids.put(key, new Long[]{payId, currId});
                }
            }
            for (final Entry<String, Long[]> entry : key2Ids.entrySet()) {
                bldr.append("<input type=\"hidden\" name=\"payId\" value=\"")
                    .append(entry.getValue()[0]).append("\"/>");
                bldr.append("<input type=\"hidden\" name=\"currId\" value=\"")
                    .append(entry.getValue()[1]).append("\"/>");
                bldr.append("<input type=\"hidden\" name=\"amount\" value=\"")
                    .append(map.get(entry.getKey())).append("\"/>");
            }
            bldr.append("<table>");
            final DecimalFormat formatter = NumberFormatter.get().getFormatter();
            for (final Entry<String, BigDecimal> entry : map.entrySet()) {
                bldr.append("<tr><td>").append(entry.getKey()).append("</td><td>")
                    .append(formatter.format(entry.getValue())).append("</td></tr>");
            }
            bldr.append("</table>");
        } else {
            final PrintQuery printcashDesk = new PrintQuery(cashDeskInstance);
            printcashDesk.addAttribute(CISales.AccountCashDesk.CurrencyLink);
            printcashDesk.execute();
            final Long currencyId = printcashDesk.<Long>getAttribute(CISales.AccountCashDesk.CurrencyLink);

            bldr.append("<input type=\"hidden\" name=\"payId\" value=\"")
                            .append(BigDecimal.ZERO).append("\"/>");
            bldr.append("<input type=\"hidden\" name=\"currId\" value=\"")
                            .append(currencyId).append("\"/>");
        }
        final Return ret = new Return();
        ret.put(ReturnValues.SNIPLETT, bldr.toString());
        return ret;
    }

    /**
     * method for obtains start amount to cash desk balance.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return startAmountFieldValueUI(final Parameter _parameter)
        throws EFapsException
    {
        return new Return();
    }

    /**
     * Obtain the start amount from the instance.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return BigDecimal with amount of the account. BigDecimal.ZERO if null.
     * @throws EFapsException on error.
     */
    public BigDecimal getStartAmount(final Parameter _parameter)
        throws EFapsException
    {
        final Instance inst = _parameter.getInstance();
        final PrintQuery print = new PrintQuery(inst);
        print.addAttribute(CISales.AccountAbstract.AmountAbstract);
        print.execute();
        final BigDecimal ret = print.<BigDecimal>getAttribute(CISales.AccountAbstract.AmountAbstract);
        return ret == null ? BigDecimal.ZERO : ret;
    }

    /**
     * Method for obtains accounts transaction of a payments and returned amount.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return ret BigDecimal.
     * @throws EFapsException on error.
     */
    public BigDecimal getAmountPayments(final Parameter _parameter)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;

        final boolean withDateConf = Sales.getSysConfig()
                        .getAttributeValueAsBoolean("PettyCashBalance_CommandWithDate");
        final AbstractCommand command = _parameter.get(ParameterValues.UIOBJECT) instanceof AbstractCommand
                        ? (AbstractCommand) _parameter.get(ParameterValues.UIOBJECT) : null;
        final List<Instance> lstInst = new ArrayList<Instance>();

        String[] oids = _parameter.getParameterValues("selectedRow");
        if (withDateConf && (command != null
                        && command.getTargetCreateType().isKindOf(CISales.PettyCashBalance.getType()))
                        || oids != null) {
            if (oids == null) {
                oids = (String[]) Context.getThreadContext().getSessionAttribute("paymentsOid");
            }
            if (oids != null) {
                for (int i = 0; i < oids.length; i++) {
                    final Instance instPay = Instance.get(oids[i]);
                    lstInst.add(instPay);
                }
            }
            if (oids == null) {
                lstInst.addAll(getPayments(_parameter));
            }
        } else {
            lstInst.addAll(getPayments(_parameter));
        }

        final SelectBuilder selOut = new SelectBuilder()
                        .linkfrom(CISales.TransactionOutbound, CISales.TransactionOutbound.Payment)
                        .attribute(CISales.TransactionOutbound.Amount);
        final SelectBuilder selIn = new SelectBuilder()
                        .linkfrom(CISales.TransactionInbound, CISales.TransactionInbound.Payment)
                        .attribute(CISales.TransactionInbound.Amount);

        for (final Instance instance : lstInst) {
            final PrintQuery print = new PrintQuery(instance);
            print.addSelect(selOut, selIn);
            print.execute();
            if (print.isList4Select(selIn.toString())) {
                final List<BigDecimal> adds = print.<List<BigDecimal>>getSelect(selIn);
                for (final BigDecimal add : adds) {
                    ret = ret.add(add == null ? BigDecimal.ZERO : add);
                }
            } else {
                final BigDecimal add = print.<BigDecimal>getSelect(selIn);
                ret = ret.add(add == null ? BigDecimal.ZERO : add);
            }
            if (print.isList4Select(selOut.toString())) {
                final List<BigDecimal> subs = print.<List<BigDecimal>>getSelect(selOut);
                for (final BigDecimal sub : subs) {
                    ret = ret.subtract(sub == null ? BigDecimal.ZERO : sub);
                }
            } else {
                final BigDecimal sub = print.<BigDecimal>getSelect(selOut);
                ret = ret.subtract(sub == null ? BigDecimal.ZERO : sub);
            }
        }
        return ret;
    }

    /**
     * Method to get the value for the name.
     *
     * @param _parameter Parameter as passed by the eFaps API for esjp
     * @return Return containing the value
     * @throws EFapsException on error
     */
    public Return getNameFieldValueUI(final Parameter _parameter)
        throws EFapsException
    {
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final FieldValue fValue = (FieldValue) _parameter.get(ParameterValues.UIOBJECT);
        DateTime firstDate = null, lastDate = null;
        if (fValue.getTargetMode().equals(TargetMode.CREATE)) {
            if (fValue.getField().getName().equals("name4create")) {
                firstDate = new DateTime().withDayOfMonth(1).withTimeAtStartOfDay();
                lastDate = new DateTime().plusMonths(1).withDayOfMonth(1).withTimeAtStartOfDay();
            }
        }
        final Return retVal = new Return();
        if (firstDate != null && lastDate != null) {
            final String type = (String) properties.get("Type");
            String number = getMaxNumber(Type.get(type), firstDate, lastDate);
            if (number == null) {
                final Integer month = firstDate.getMonthOfYear();
                number = "00001-" + (month.intValue() < 10 ? "0" + month : month);
            } else {
                final String numTmp = number.substring(0, number.indexOf("-"));
                final int length = numTmp.trim().length();
                final Integer numInt = Integer.parseInt(numTmp.trim()) + 1;
                final NumberFormat nf = NumberFormat.getInstance();
                nf.setMinimumIntegerDigits(length);
                nf.setMaximumIntegerDigits(length);
                nf.setGroupingUsed(false);
                number = nf.format(numInt) + number.substring(number.indexOf("-")).trim();
            }
            retVal.put(ReturnValues.VALUES, number);
        }

        return retVal;
    }

    /**
     * Method to get the maximum for a value from the database.
     *
     * @param _type Long for search filter to petty cash.
     * @param _firstDate for search minimum date.
     * @param _lastDate for search maximum date.
     * @return ret Return for maximum value.
     * @throws EFapsException on error
     */
    protected String getMaxNumber(final Type _type,
                                  final DateTime _firstDate,
                                  final DateTime _lastDate)
        throws EFapsException
    {
        String ret = null;

        final QueryBuilder queryBldr = new QueryBuilder(_type);
        queryBldr.addWhereAttrGreaterValue(CIERP.DocumentAbstract.Date, _firstDate.minusSeconds(1));
        queryBldr.addWhereAttrLessValue(CIERP.DocumentAbstract.Date, _lastDate);
        queryBldr.addOrderByAttributeDesc(CIERP.DocumentAbstract.Name);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIERP.DocumentAbstract.Name);
        multi.setEnforceSorted(true);
        multi.execute();

        if (multi.next()) {
            ret = multi.<String>getAttribute(CIERP.DocumentAbstract.Name);
        }

        return ret;
    }

    /**
     * Method to get the value for the number in case of selected a new Date.
     *
     * @param _parameter Parameter as passed by the eFaps API.
     * @return Return containing the value
     * @throws EFapsException on error
     */
    public Return updateNameFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final DateTime date = DateUtil.getDateFromParameter(_parameter.getParameterValue("date_eFapsDate"));

        final DateTime firstDate = new DateTime(date).withDayOfMonth(1).withTimeAtStartOfDay();
        final DateTime lastDate = new DateTime(date).plusMonths(1).withDayOfMonth(1).withTimeAtStartOfDay();

        if (firstDate != null && lastDate != null) {
            final String type = (String) properties.get("Type");
            String number = getMaxNumber(Type.get(type), firstDate, lastDate);
            if (number == null) {
                final Integer month = firstDate.getMonthOfYear();
                number = "00001-" + (month.intValue() < 10 ? "0" + month : month);
            } else {
                final String numTmp = number.substring(0, number.indexOf("-"));
                final int length = numTmp.trim().length();
                final Integer numInt = Integer.parseInt(numTmp.trim()) + 1;
                final NumberFormat nf = NumberFormat.getInstance();
                nf.setMinimumIntegerDigits(length);
                nf.setMaximumIntegerDigits(length);
                nf.setGroupingUsed(false);
                number = nf.format(numInt) + number.substring(number.indexOf("-")).trim();
            }
            final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
            final Map<String, String> map = new HashMap<String, String>();
            map.put("name4create", number);
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        }
        return retVal;
    }

    /**
     * Method for obtains a get amount current of the petty cash receipt.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return setAmountCurrentFieldValueUI(final Parameter _parameter)
        throws EFapsException
    {
        final Instance inst = _parameter.getInstance();
        final QueryBuilder queryBldr = new QueryBuilder(CISales.Balance);
        queryBldr.addWhereAttrEqValue(CISales.Balance.Account, inst.getId());
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder curSel = new SelectBuilder().linkto(CISales.Balance.Currency)
                                                                                    .attribute(CIERP.Currency.Symbol);
        multi.addSelect(curSel);
        multi.addAttribute(CISales.Balance.Amount);
        multi.execute();

        String symbol = null;
        BigDecimal amount = null;
        while (multi.next()) {
            symbol = multi.<String>getSelect(curSel);
            amount = multi.<BigDecimal>getAttribute(CISales.Balance.Amount);
        }
        final Return ret = new Return();
        if (amount == null){
            ret.put(ReturnValues.VALUES, "");
        } else{
            ret.put(ReturnValues.VALUES,
                            symbol + " " + NumberFormatter.get().getTwoDigitsFormatter().format(amount).toString());
        }
        return ret;
    }
    /**
     * Method for create a new petty Cash Balance.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return pettyCashBalance(final Parameter _parameter)
        throws EFapsException
    {
        Return ret = new Return();

        final PrintQuery printAmount = new PrintQuery(_parameter.getCallInstance());
        printAmount.addAttribute(CISales.AccountPettyCash.Name, CISales.AccountPettyCash.AmountAbstract);
        printAmount.execute();
        BigDecimal amount = printAmount.<BigDecimal>getAttribute(CISales.AccountAbstract.AmountAbstract);
        final String accName = printAmount.<String>getAttribute(CISales.AccountAbstract.Name);
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }

        final Instance balanceInst = createPettyCashBalanceDoc(_parameter);

        final PrintQuery print2 = new PrintQuery(balanceInst);
        print2.addAttribute(CISales.PettyCashBalance.Name);
        print2.execute();
        final String nameBalance = print2.<String>getAttribute(CISales.PettyCashBalance.Name);

        _parameter.put(ParameterValues.INSTANCE, balanceInst);

        final StandartReport report = new StandartReport();
        report.getJrParameters().put("AccName", accName);
        report.getJrParameters().put("AmountPettyCash", amount);

        final SystemConfiguration config = ERP.getSysConfig();
        if (config != null) {
            final String companyName = config.getAttributeValue(ERPSettings.COMPANYNAME);
            if (companyName != null && !companyName.isEmpty()) {
                report.getJrParameters().put("CompanyName", companyName);
            }
        }

        final String fileName = DBProperties.getProperty("Sales_PettyCashBalance.Label") + "_" + nameBalance;
        report.setFileName(fileName);
        ret = report.execute(_parameter);

        return ret;
    }

    /**
     * Internal method to create a PettyCashBalance.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return instance of new PettyCashBalance, null if not created
     * @throws EFapsException on error
     */
    protected Instance createPettyCashBalanceDoc(final Parameter _parameter)
        throws EFapsException
    {
        Instance ret = null;
        final Instance inst = _parameter.getCallInstance();
        final boolean withDateConf = Sales.getSysConfig().getAttributeValueAsBoolean("PettyCashBalance_CommandWithDate");

        final PrintQuery print = new PrintQuery(inst);
        print.addAttribute(CISales.AccountAbstract.CurrencyLink,
                            CISales.AccountPettyCash.AmountAbstract);
        print.execute();
        final Long curId = print.<Long>getAttribute(CISales.AccountAbstract.CurrencyLink);
        BigDecimal startAmountOrig = print.<BigDecimal>getAttribute(CISales.AccountPettyCash.AmountAbstract);
        if (startAmountOrig == null) {
            startAmountOrig = BigDecimal.ZERO;
        }
        final String startAmountStr = _parameter.getParameterValue("startAmount");
        final BigDecimal amount = getAmountPayments(_parameter);

        final DecimalFormat formater = NumberFormatter.get().getTwoDigitsFormatter();
        BigDecimal startAmount = BigDecimal.ZERO;
        try {
            startAmount = (BigDecimal) formater.parse(startAmountStr);
        } catch (final ParseException e) {
            throw new EFapsException(Account_Base.class, "ParseException", e);
        }
        final BigDecimal difference;
        // the transactions sum to Zero
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            // check if it is the first balance
            if (hasTransaction(_parameter)) {
                difference = startAmount.subtract(startAmountOrig);
            } else {
                difference = startAmount;
            }
        } else {
            difference = amount.negate().add(startAmount.subtract(startAmountOrig));
        }
        if (startAmount.compareTo(startAmountOrig) != 0) {
            final Update update = new Update(inst);
            update.add(CISales.AccountPettyCash.AmountAbstract, startAmount);
            update.execute();
        }

        // only if there is a difference it will be executed
        if (difference.compareTo(BigDecimal.ZERO) != 0) {
            final Insert insert = new Insert(CISales.PettyCashBalance);
            insert.add(CISales.PettyCashBalance.Name, getName4PettyCashBalance(_parameter));
            insert.add(CISales.PettyCashBalance.Salesperson, Context.getThreadContext().getPersonId());
            if (withDateConf) {
                insert.add(CISales.PettyCashBalance.Date, _parameter.getParameterValue("date"));
            } else {
                insert.add(CISales.PettyCashBalance.Date, new DateTime());
            }
            insert.add(CISales.PettyCashBalance.Status,
                            Status.find(CISales.PettyCashBalanceStatus.uuid, "Open").getId());
            insert.add(CISales.PettyCashBalance.CrossTotal, difference);
            insert.add(CISales.PettyCashBalance.NetTotal, BigDecimal.ZERO);
            insert.add(CISales.PettyCashBalance.DiscountTotal, BigDecimal.ZERO);
            insert.add(CISales.PettyCashBalance.RateCrossTotal, BigDecimal.ZERO);
            insert.add(CISales.PettyCashBalance.RateNetTotal, BigDecimal.ZERO);
            insert.add(CISales.PettyCashBalance.RateDiscountTotal, BigDecimal.ZERO);
            insert.add(CISales.PettyCashBalance.Rate, new Object[] { 1, 1 });
            insert.add(CISales.PettyCashBalance.RateCurrencyId, curId);
            insert.add(CISales.PettyCashBalance.CurrencyId, curId);
            insert.execute();

            ret = insert.getInstance();

            final List<Instance> lstInst = new ArrayList<Instance>();
            if (withDateConf) {
                final String[] oids = (String[]) Context.getThreadContext().getSessionAttribute("paymentsOid");
                if (oids != null) {
                    for (int i = 0; i < oids.length; i++) {
                        final Instance instPay = Instance.get(oids[i]);
                        lstInst.add(instPay);
                    }
                } else {
                    getPayments4PettyCashBalance(_parameter);
                    lstInst.addAll(getPayments(_parameter));
                }
            } else {
                lstInst.addAll(getPayments(_parameter));
            }

            final Instance balanceInst = insert.getInstance();
            for (final Instance instance : lstInst) {
                final Update update = new Update(instance);
                update.add(CISales.Payment.TargetDocument, balanceInst.getId());
                update.execute();
            }

            final Insert payInsert = new Insert(CISales.Payment);
            payInsert.add(CISales.Payment.Date, new DateTime());
            payInsert.add(CISales.Payment.CreateDocument, balanceInst.getId());
            payInsert.add(CISales.Payment.TargetDocument, balanceInst.getId());
            payInsert.execute();

            CIType type;
            if (difference.compareTo(BigDecimal.ZERO) < 0) {
                type = CISales.TransactionOutbound;
            } else {
                type = CISales.TransactionInbound;
            }

            final Insert transInsert = new Insert(type);
            transInsert.add(CISales.TransactionAbstract.Amount, difference.abs());
            transInsert.add(CISales.TransactionAbstract.CurrencyId, curId);
            transInsert.add(CISales.TransactionAbstract.Payment,  payInsert.getInstance().getId());
            transInsert.add(CISales.TransactionAbstract.Account, inst.getId());
            transInsert.add(CISales.TransactionAbstract.Description,
                        DBProperties.getProperty("org.efaps.esjp.sales.Accountg_Base.Transaction.CashDeskBalance"));
            transInsert.add(CISales.TransactionAbstract.Date, new DateTime());
            transInsert.execute();
        }
        return ret;
    }

    /**
     * Method to print the payments without a target document.
     *
     * @param _parameter as passed from eFaps API.
     * @return Return with the instances.
     * @throws EFapsException on error.
     */
    public Return getPayments4PettyCashBalance(final Parameter _parameter)
        throws EFapsException
    {
        final List<Instance> lst = getPayments(_parameter);

        final Return ret = new Return();
        ret.put(ReturnValues.VALUES, lst);
        return ret;
    }

    /**
     * Method to get payments without a target document.
     *
     * @param _parameter as passed from eFaps API.
     * @return List with the Instances.
     * @throws EFapsException on error.
     */
    protected List<Instance> getPayments(final Parameter _parameter)
        throws EFapsException
    {
        final Instance inst = (_parameter.getCallInstance() == null
                                        ? _parameter.getInstance() : _parameter.getCallInstance());
        final QueryBuilder subQueryBldr = new QueryBuilder(CISales.TransactionAbstract);
        subQueryBldr.addWhereAttrEqValue(CISales.TransactionAbstract.Account, inst.getId());
        final AttributeQuery attrQuery = subQueryBldr.getAttributeQuery(CISales.TransactionAbstract.Payment);
        final QueryBuilder queryBldr = new QueryBuilder(CISales.Payment);
        queryBldr.addWhereAttrIsNull(CISales.Payment.TargetDocument);
        queryBldr.addWhereAttrInQuery(CISales.Payment.ID, attrQuery);

        final InstanceQuery query = queryBldr.getQuery();
        query.execute();

        return query.getValues();
    }

    /**Method to activate the command to create a balance with date and choosing the receipts or not.
     *
     * @param _parameter as passed from eFaps API.
     * @return Return with true if the command to appear is with date.
     * @throws EFapsException on error.
     */
    public Return check4SystemConfiguration(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final boolean withDateComm = Boolean.parseBoolean((String) props.get("WithDate"));
        final boolean withDateConf = Sales.getSysConfig().getAttributeValueAsBoolean("PettyCashBalance_CommandWithDate");
        if (withDateConf && withDateComm) {
            ret.put(ReturnValues.TRUE, true);
        } else if (!withDateConf && !withDateComm) {
            ret.put(ReturnValues.TRUE, true);
        }

        return ret;
    }

    protected String getName4PettyCashBalance(final Parameter _parameter)
        throws EFapsException
    {
        return new DateTime().toLocalTime().toString();
    }

    /**
     * method for create a new petty cash receipt.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return createPettyCashReceipt(final Parameter _parameter)
        throws EFapsException
    {
        createPettyCashReceiptDoc( _parameter);
        return new Return();
    }

    /**
     * Internal method to create a PettyCashReceipt.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return instance of new PettyCashBalance, null if not created
     * @throws EFapsException on error
     */
    protected Instance createPettyCashReceiptDoc(final Parameter _parameter)
        throws EFapsException
    {
        final Instance accInst = _parameter.getCallInstance();

        final PrintQuery print = new PrintQuery(accInst);
        print.addAttribute(CISales.AccountAbstract.CurrencyLink);
        print.execute();
        final Long curId = print.<Long>getAttribute(CISales.AccountAbstract.CurrencyLink);

        final String crossTotalStr = _parameter.getParameterValue("crossTotal");
        final String note = _parameter.getParameterValue("note");
        final String name = _parameter.getParameterValue("name4create");
        final String contact = _parameter.getParameterValue("contact");

        final DateTime date = new DateTime(_parameter.getParameterValue("date"));
        final Insert insert = new Insert(CISales.PettyCashReceipt);
        insert.add(CISales.PettyCashReceipt.Name, name);
        insert.add(CISales.PettyCashReceipt.Date, date);
        insert.add(CISales.PettyCashReceipt.Status, Status.find(CISales.PettyCashReceiptStatus.uuid, "Closed").getId());
        insert.add(CISales.PettyCashReceipt.CrossTotal, crossTotalStr);
        insert.add(CISales.PettyCashReceipt.NetTotal, BigDecimal.ZERO);
        insert.add(CISales.PettyCashReceipt.DiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.PettyCashReceipt.RateCrossTotal, BigDecimal.ZERO);
        insert.add(CISales.PettyCashReceipt.RateNetTotal, BigDecimal.ZERO);
        insert.add(CISales.PettyCashReceipt.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.PettyCashReceipt.Rate, new Object[]{ 1, 1 });
        insert.add(CISales.PettyCashReceipt.RateCurrencyId, curId);
        insert.add(CISales.PettyCashReceipt.CurrencyId, curId);
        insert.add(CISales.PettyCashReceipt.Contact, contact.length() > 0 ? Instance.get(contact).getId() : null);
        insert.execute();

        final Instance receiptInst = insert.getInstance();

        new Create().insertClassification(_parameter, receiptInst);

        final Insert payInsert = new Insert(CISales.Payment);
        payInsert.add(CISales.Payment.Date, date);
        payInsert.add(CISales.Payment.CreateDocument, receiptInst.getId());
        payInsert.execute();

        final Insert transInsert = new Insert(CISales.TransactionOutbound);
        transInsert.add(CISales.TransactionOutbound.Amount, crossTotalStr);
        transInsert.add(CISales.TransactionOutbound.CurrencyId, curId);
        transInsert.add(CISales.TransactionOutbound.Payment, payInsert.getInstance().getId());
        transInsert.add(CISales.TransactionOutbound.Account, accInst.getId());
        transInsert.add(CISales.TransactionOutbound.Description, note);
        transInsert.add(CISales.TransactionOutbound.Date, date);
        transInsert.execute();

        return receiptInst;
    }

    /**
     * Method for return a field and put start amount to petty cash.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return ret SNIPLETT with values to field.
     * @throws EFapsException on error.
     */
    public Return startAmountFieldValue4PettyCashUI(final Parameter _parameter)
        throws EFapsException
    {
        final BigDecimal amount = getStartAmount(_parameter);

        final Return ret = new Return();
        ret.put(ReturnValues.VALUES, amount.setScale(2, BigDecimal.ROUND_HALF_UP));
        return ret;
    }
    /**
     * method for update field and obtains a difference amount.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return retVal with Value.
     * @throws EFapsException on error.
     */
    public Return updateFieldValue4PettyCash(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        BigDecimal difference = BigDecimal.ZERO;
        final DecimalFormat formater = NumberFormatter.get().getTwoDigitsFormatter();
        if (hasTransaction(_parameter)) {
            final BigDecimal amount = getAmountPayments(_parameter);
            final String startAmountStr = _parameter.getParameterValue("startAmount");

            BigDecimal startAmount = BigDecimal.ZERO;
            try {
                startAmount = (BigDecimal) formater.parse(startAmountStr);
            } catch (final ParseException e) {
               throw new EFapsException(Account_Base.class, "ParseException", e);
            }
            final BigDecimal startAmountOrig = getStartAmount(_parameter);

            difference = amount.negate();
            if(startAmount.compareTo(startAmountOrig) != 0){
                difference = difference.add(startAmount.subtract(startAmountOrig));
            }
        }
        final Map<String, String> map = new HashMap<String, String>();
        map.put("messageBalancing", formater.format(difference).toString());
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        list.add(map);
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }
    /**
     * method for set field value for PettyCash with value is the difference amount.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return retVal with value is the difference amount.
     * @throws EFapsException on error.
     */
    public Return setFieldValue4PettyCash(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final BigDecimal amount = getAmountPayments(_parameter);

        final DecimalFormat formater = NumberFormatter.get().getTwoDigitsFormatter();

        retVal.put(ReturnValues.VALUES, formater.format(amount.negate()));
        return retVal;
    }

    /**
     * method for set the revenue field value for PettyCash with value is the
     * difference amount.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return retVal with value is the difference amount.
     * @throws EFapsException on error.
     */
    public Return revenuesFieldValue4PettyCashUI(final Parameter _parameter)
        throws EFapsException
    {
        final DecimalFormat formater = NumberFormatter.get().getTwoDigitsFormatter();
        final StringBuilder bldr = new StringBuilder();

        final BigDecimal startAmount = getStartAmount(_parameter);
        final BigDecimal amount = getAmountPayments(_parameter);
        final BigDecimal difference = (amount != BigDecimal.ZERO ? startAmount.add(amount)
                                                           : BigDecimal.ZERO);

        bldr.append(formater.format(difference));
        final Return ret = new Return();
        ret.put(ReturnValues.SNIPLETT, bldr.toString());
        return ret;
    }

    /**
     * Executed from the validate event for creation of a PettyCashReceipt.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return Return containing true if validation is passed
     * @throws EFapsException on error
     */
    public Return validatePettyCashReceipt(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String addVal = additionalValidate4PettyCashReceipt(_parameter);
        if (hasTransaction(_parameter)
                        || _parameter.getInstance().getType().isKindOf(CISales.PettyCashReceipt.getType())) {
            if (addVal == null || addVal != null && addVal.isEmpty()) {
                if (!_parameter.getInstance().getType().isKindOf(CISales.PettyCashReceipt.getType())) {
                    final BigDecimal amount = getAmountPayments(_parameter);
                    final BigDecimal startAmount = getStartAmount(_parameter);
                    final String crossTotalStr = _parameter.getParameterValue("crossTotal");
                    final DecimalFormat formater = NumberFormatter.get().getTwoDigitsFormatter();
                    BigDecimal crossTotal = BigDecimal.ZERO;
                    try {
                        crossTotal = (BigDecimal) formater.parse(crossTotalStr);
                    } catch (final ParseException e) {
                        throw new EFapsException(Account_Base.class, "ParseException", e);
                    }
                    final BigDecimal difference = startAmount.add(amount).subtract(crossTotal);
                    if (difference.signum() == -1) {
                        ret.put(ReturnValues.VALUES,
                                        "org.efaps.esjp.sales.Account.validatePettyCashReceipt.NegativeAmount");
                    }
                }
                ret.put(ReturnValues.TRUE, true);
            } else {
                ret.put(ReturnValues.VALUES, addVal);
            }
        } else {
            ret.put(ReturnValues.VALUES, "org.efaps.esjp.sales.Account.validatePettyCashReceipt.NoTransaction");
        }
        return ret;
    }

    /**
     * Has the Account already transactions.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return true if account has transactions, else false
     * @throws EFapsException on error
     */
    protected boolean hasTransaction(final Parameter _parameter)
        throws EFapsException
    {
        final Instance inst = _parameter.getInstance();
        final QueryBuilder queryBuilder = new QueryBuilder(CISales.TransactionAbstract);
        queryBuilder.addWhereAttrEqValue(CISales.TransactionAbstract.Account, inst.getId());
        final InstanceQuery query = queryBuilder.getQuery();
        return !query.execute().isEmpty();
    }

    protected String additionalValidate4PettyCashReceipt(final Parameter _parameter)
        throws EFapsException
    {
        return null;
    }

    public Return accessCheck4Active(final Parameter _parameter)
        throws EFapsException
    {
        final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);

        Return ret = new Return();
        final Instance accountInst = _parameter.getInstance();

        if (accountInst.isValid()) {
            final PrintQuery print = new PrintQuery(accountInst);
            print.addAttribute(CISales.AccountAbstract.Active);
            print.execute();

            final boolean active = print.<Boolean>getAttribute(CISales.AccountAbstract.Active) == null
                                                ? false : print.<Boolean>getAttribute(CISales.AccountAbstract.Active);

            if (active) {
                if (props.containsKey("AccessCheck4SystemConfiguration")) {
                    if ("true".equalsIgnoreCase((String) props.get("AccessCheck4SystemConfiguration"))) {
                        ret = check4SystemConfiguration(_parameter);
                    }
                } else {
                    ret.put(ReturnValues.TRUE, true);
                }
            }
        }

        return ret;
    }

    public Return downloadXlsLiquidation(final Parameter _parameter)
        throws EFapsException
    {
        Return ret = new Return();

        final SelectBuilder selPayId = new SelectBuilder().linkfrom(CISales.Payment, CISales.Payment.CreateDocument)
                        .id();

        final PrintQuery print = new PrintQuery(_parameter.getInstance());
        print.addAttribute(CISales.PettyCashBalance.Name, CISales.PettyCashBalance.CrossTotal);
        print.addSelect(selPayId);
        print.execute();

        String nameAccount = null;
        final QueryBuilder queryAcc = new QueryBuilder(CISales.TransactionAbstract);
        queryAcc.addWhereAttrEqValue(CISales.TransactionAbstract.Payment, print.<Long>getSelect(selPayId));
        final InstanceQuery queryInst = queryAcc.getQuery();
        queryInst.execute();
        if (!queryInst.getValues().isEmpty()) {
            final SelectBuilder selNameAcc = new SelectBuilder().linkto(CISales.TransactionAbstract.Account)
                            .attribute(CISales.AccountAbstract.Name);
            for (final Instance inst : queryInst.getValues()) {
                if (nameAccount == null) {
                    final PrintQuery print4Acc = new PrintQuery(inst);
                    print4Acc.addSelect(selNameAcc);
                    print4Acc.execute();
                    nameAccount = print4Acc.<String>getSelect(selNameAcc);
                }
            }
        }

        final String nameBalance = print.<String>getAttribute(CISales.PettyCashBalance.Name);
        final BigDecimal amount = print.<BigDecimal>getAttribute(CISales.PettyCashBalance.CrossTotal);

        _parameter.put(ParameterValues.INSTANCE, _parameter.getInstance());

        final StandartReport report = new StandartReport();
        report.getJrParameters().put("AccName", nameAccount);
        report.getJrParameters().put("AmountPettyCash", amount);

        final SystemConfiguration config = ERP.getSysConfig();
        if (config != null) {
            final String companyName = config.getAttributeValue(ERPSettings.COMPANYNAME);
            if (companyName != null && !companyName.isEmpty()) {
                report.getJrParameters().put("CompanyName", companyName);
            }
        }

        final String fileName = DBProperties.getProperty("Sales_PettyCashBalance.Label", "es") + "_" + nameBalance;
        report.setFileName(fileName);
        ret = report.execute(_parameter);

        return ret;
    }

    protected StringBuilder getSetFieldValue(final int _idx,
                                             final String _fieldName,
                                             final String _value,
                                             final boolean _escape)
    {
        final StringBuilder ret = new StringBuilder();
        ret.append("eFapsSetFieldValue(").append(_idx).append(",'").append(_fieldName).append("',");
        if (_escape) {
            ret.append("'").append(StringEscapeUtils.escapeEcmaScript(_value)).append("'");
        } else {
            ret.append(_value);
        }
        ret.append(");");
        return ret;
    }

    public Return getJavaScriptUIValue4EditJustification(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Instance inst = _parameter.getCallInstance();
        final StringBuilder js = new StringBuilder();
        js.append("<script type=\"text/javascript\">")
                        .append("Wicket.Event.add(window, \"domready\", function(event) {");

        if (inst.isValid()) {
            final PrintQuery print = new PrintQuery(inst);
            final SelectBuilder selContOid = new SelectBuilder().linkto(CISales.PettyCashReceipt.Contact).oid();
            final SelectBuilder selContName = new SelectBuilder().linkto(CISales.PettyCashReceipt.Contact)
                            .attribute(CIContacts.Contact.Name);
            final SelectBuilder selDocName = new SelectBuilder().clazz(CISales.PettyCashReceipt_Class)
                            .attribute(CISales.PettyCashReceipt_Class.Name);
            final SelectBuilder selEmplName = new SelectBuilder().clazz(CISales.PettyCashReceipt_Class)
                            .attribute(CISales.PettyCashReceipt_Class.EmployeeName);
            print.addSelect(selContOid, selContName, selDocName, selEmplName);
            if (print.execute()) {
                final Instance contInst = Instance.get(print.<String>getSelect(selContOid));
                final String contName = print.<String>getSelect(selContName);
                final String docName = print.<String>getSelect(selDocName);
                final String empName = print.<String>getSelect(selEmplName);
                if (contInst.isValid()) {
                    js.append(getSetFieldValue(0, "contactAutoComplete", contName, true))
                                    .append(getSetFieldValue(0, "contact", contInst.getOid(), true));
                }
                if (docName != null) {
                    js.append(getSetFieldValue(0, "name", docName, true));
                }
                if (empName != null) {
                    js.append(getSetFieldValue(0, "employeeName", empName, true));
                }
            }
            js.append(" });").append("</script>");
        }
        ret.put(ReturnValues.SNIPLETT, js.toString());
        return ret;
    }

    public Return dropDownFieldValueReceipt(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final StringBuilder html = new StringBuilder();
        final FieldValue fieldValue = (FieldValue) _parameter.get(ParameterValues.UIOBJECT);

        final PrintQuery print = new PrintQuery(_parameter.getInstance() != null
                        ? _parameter.getInstance() : _parameter.getCallInstance());
        final SelectBuilder selDocType = new SelectBuilder().clazz(CISales.PettyCashReceipt_Class)
                        .attribute(CISales.PettyCashReceipt_Class.ReceiptTypeLink);
        print.addSelect(selDocType);
        print.execute();
        final Long receiptId = print.<Long>getSelect(selDocType);
        final QueryBuilder queryBldr = new QueryBuilder(CISales.PettyCashReceiptTypeAttr.getType());
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.PettyCashReceiptTypeAttr.Value, CISales.PettyCashReceiptTypeAttr.Description);
        multi.execute();
        html.append("<select name=\"").append(fieldValue.getField().getName()).append("\">");
        while (multi.next()) {
            final String value = multi.<String>getAttribute(CISales.PettyCashReceiptTypeAttr.Value);
            final String description = multi.<String>getAttribute(CISales.PettyCashReceiptTypeAttr.Description);
            final Long id = multi.getCurrentInstance().getId();
            html.append("<option value=\"").append(id).append("\"");
            if (receiptId != null && receiptId.equals(id)) {
                html.append(" selected ");
            }
            html.append(">").append(value + " - " + description).append("</option>");
        }
        html.append("</select>");
        retVal.put(ReturnValues.SNIPLETT, html.toString());

        return retVal;
    }

    public Return editJustification4PettyCashReceipt(final Parameter _parameter)
        throws EFapsException
    {
        final String contact = _parameter.getParameterValue("contact");
        final String docName = _parameter.getParameterValue("name");
        final String emp = _parameter.getParameterValue("employeeName");
        final String typeDoc = _parameter.getParameterValue("receiptTypeLink");
        final Update update = new Update(_parameter.getCallInstance());
        update.add(CISales.PettyCashReceipt.Contact, contact.length() > 0 ? Instance.get(contact).getId() : null);
        update.execute();

        final PrintQuery print = new PrintQuery(_parameter.getCallInstance());
        final SelectBuilder selClazz = new SelectBuilder().clazz(CISales.PettyCashReceipt_Class).oid();
        print.addSelect(selClazz);
        if (print.execute()) {
            final Instance instClazz = Instance.get(print.<String>getSelect(selClazz));
            if (instClazz.isValid()) {
                final Update update2 = new Update(instClazz);
                update2.add(CISales.PettyCashReceipt_Class.Name, docName.length() > 0 ? docName : null);
                update2.add(CISales.PettyCashReceipt_Class.EmployeeName, emp.length() > 0 ? emp : null);
                update2.add(CISales.PettyCashReceipt_Class.ReceiptTypeLink, typeDoc.length() > 0 ? typeDoc : null);
                update2.execute();
            }
        }
        return new Return();
    }

    public Return editAmount4PettyCashReceipt(final Parameter _parameter)
        throws EFapsException
    {
        final Revision rev = new Revision()
        {

            @Override
            public Return revise(final Parameter _parameter)
                throws EFapsException
            {
                final Instance newInst = copyDoc(_parameter);
                final Map<Instance, Instance> map = copyRelations(_parameter, newInst);
                setStati(_parameter, newInst);
                createTransaction(_parameter, newInst, map);

                return new Return();
            }

            @SuppressWarnings("unused")
            protected Return createTransaction(final Parameter _parameter,
                                               final Instance _newInst,
                                               final Map<Instance, Instance> _mapRel)
                throws EFapsException
            {
                final Return ret = new Return();
                final Insert relInsert = new Insert(CIERP.Document2Class);
                relInsert.add(CIERP.Document2Class.DocumentLink, _newInst.getId());
                relInsert.add(CIERP.Document2Class.ClassTypeId, CISales.PettyCashReceipt_Class.getType().getId());
                relInsert.execute();

                Instance instPayment = null;
                Instance instPaymentOld = null;
                Instance instClass = null;
                for (final Entry<Instance, Instance> entry : _mapRel.entrySet()) {
                    if (entry.getValue().getType().isKindOf(CISales.Payment.getType())) {
                        instPayment = entry.getValue();
                        instPaymentOld = entry.getKey();
                    }
                    if (entry.getValue().getType().isKindOf(CISales.PettyCashReceipt_Class.getType())) {
                        instClass = entry.getValue();
                    }
                }

                if (instPayment != null) {
                    final Update updatePay = new Update(instPayment);
                    updatePay.add(CISales.Payment.TargetDocument, new Object[] { null });
                    updatePay.execute();

                    final QueryBuilder queryBldr = new QueryBuilder(CISales.TransactionOutbound);
                    queryBldr.addWhereAttrEqValue(CISales.TransactionOutbound.Payment, instPaymentOld.getId());
                    final MultiPrintQuery multi = queryBldr.getPrint();
                    multi.addAttribute(CISales.TransactionOutbound.Description);
                    final SelectBuilder selAcc = new SelectBuilder().linkto(CISales.TransactionOutbound.Account).oid();
                    multi.addSelect(selAcc);
                    multi.execute();
                    Instance accInst = null;
                    if (multi.next()) {
                        accInst = Instance.get(multi.<String>getSelect(selAcc));
                        final String descTxn = multi.<String>getAttribute(CISales.TransactionOutbound.Description);
                        if (accInst.isValid()) {
                            final PrintQuery print = new PrintQuery(accInst);
                            print.addAttribute(CISales.AccountAbstract.CurrencyLink);
                            print.execute();
                            final Long curId = print.<Long>getAttribute(CISales.AccountAbstract.CurrencyLink);

                            final PrintQuery print2 = new PrintQuery(_parameter.getInstance());
                            print2.addAttribute(CISales.PettyCashReceipt.CrossTotal,
                                            CISales.PettyCashReceipt.Note);
                            print2.execute();

                            final String noteDoc = print2.<String>getAttribute(CISales.PettyCashReceipt.Note);
                            final Update updateDoc = new Update(_parameter.getInstance());
                            updateDoc.add(CISales.PettyCashReceipt.Note,
                                            DBProperties.getProperty("org.efaps.esjp.sales.Account.CorrectionDocument")
                                                            + " - " + noteDoc);
                            updateDoc.execute();

                            final BigDecimal oldAmount = print2
                                            .<BigDecimal>getAttribute(CISales.PettyCashReceipt.CrossTotal);
                            final BigDecimal newAmount = new BigDecimal(_parameter.getParameterValue("crossTotal"));

                            final Insert transInsertIn = new Insert(CISales.TransactionInbound);
                            transInsertIn.add(CISales.TransactionInbound.Amount, oldAmount);
                            transInsertIn.add(CISales.TransactionInbound.CurrencyId, curId);
                            transInsertIn.add(CISales.TransactionInbound.Payment, instPayment.getId());
                            transInsertIn.add(CISales.TransactionInbound.Account, accInst.getId());
                            transInsertIn.add(CISales.TransactionInbound.Description,
                                            DBProperties.getProperty("org.efaps.esjp.sales.Account.CorrectionInbound")
                                                            + ": " + descTxn);
                            transInsertIn.add(CISales.TransactionInbound.Date, new Date());
                            transInsertIn.execute();

                            final Insert transInsertOut = new Insert(CISales.TransactionOutbound);
                            transInsertOut.add(
                                            CISales.TransactionOutbound.Amount,
                                            (newAmount.compareTo(BigDecimal.ZERO) == -1) ? newAmount
                                                            .multiply(BigDecimal.valueOf(-1)) : newAmount);
                            transInsertOut.add(CISales.TransactionOutbound.CurrencyId, curId);
                            transInsertOut.add(CISales.TransactionOutbound.Payment, instPayment.getId());
                            transInsertOut.add(CISales.TransactionOutbound.Account, accInst.getId());
                            transInsertOut.add(CISales.TransactionOutbound.Description,
                                            DBProperties.getProperty("org.efaps.esjp.sales.Account.CorrectionOutbound")
                                                            + ": " + descTxn);
                            transInsertOut.add(CISales.TransactionOutbound.Date, new Date());
                            transInsertOut.execute();

                            final Update update = new Update(_newInst);
                            update.add(CISales.PettyCashReceipt.CrossTotal,
                                            (newAmount.compareTo(BigDecimal.ZERO) == -1) ? newAmount
                                                            .multiply(BigDecimal.valueOf(-1)) : newAmount);
                            update.add(CISales.PettyCashReceipt.RateCrossTotal,
                                            (newAmount.compareTo(BigDecimal.ZERO) == -1) ? newAmount
                                                            .multiply(BigDecimal.valueOf(-1)) : newAmount);
                            update.execute();
                        }
                    }

                }
                return ret;
            }
        };
        final Return ret = rev.revise(_parameter);
        return ret;
    }
}
