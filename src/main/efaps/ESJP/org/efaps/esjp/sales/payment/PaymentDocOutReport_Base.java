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

package org.efaps.esjp.sales.payment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.EFapsMapDataSource;
import org.efaps.esjp.common.jasperreport.StandartReport;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.util.DateTimeUtil;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperReport;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: DocReport_Base.java 7679 2012-06-14 22:39:59Z
 *          jorge.cueva@moxter.net $
 */
@EFapsUUID("a79e177d-ec60-4718-a4e1-c1e829bf20e9")
@EFapsApplication("eFapsApp-Sales")
public abstract class PaymentDocOutReport_Base
    extends EFapsMapDataSource
{

    @Override
    public void init(final JasperReport _jasperReport,
                     final Parameter _parameter,
                     final JRDataSource _parentSource,
                     final Map<String, Object> _jrParameters)
        throws EFapsException
    {
        final Instance contact = Instance.get(_parameter.getParameterValue("contact"));
        final String dateFromStr = _parameter.getParameterValue("dateFrom");
        final String dateToStr = _parameter.getParameterValue("dateTo");
        final DateTime from = DateTimeUtil.normalize(new DateTime(dateFromStr));
        final DateTime to = DateTimeUtil.normalize(new DateTime(dateToStr));

        final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter);
        queryBldr.addWhereAttrGreaterValue(CISales.DocumentSumAbstract.Date, from.minusMinutes(1));
        queryBldr.addWhereAttrLessValue(CISales.DocumentSumAbstract.Date, to.plusDays(1));

        if (contact.isValid()) {
            queryBldr.addWhereAttrEqValue(CISales.DocumentSumAbstract.Contact, contact);
        }
        queryBldr.addOrderByAttributeAsc(CISales.DocumentSumAbstract.Date);

        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.DocumentSumAbstract.Date,
                        CISales.DocumentSumAbstract.DueDate,
                        CISales.DocumentSumAbstract.Created,
                        CISales.DocumentSumAbstract.Name,
                        CISales.DocumentSumAbstract.RateCurrencyId,
                        CISales.DocumentSumAbstract.CrossTotal,
                        CISales.DocumentSumAbstract.RateCrossTotal,
                        CISales.DocumentSumAbstract.Rate);
        final SelectBuilder selContactName = new SelectBuilder()
                .linkto(CISales.DocumentSumAbstract.Contact).attribute(CIContacts.Contact.Name);
        final SelectBuilder statusName = new SelectBuilder()
                .linkto(CISales.DocumentSumAbstract.StatusAbstract).attribute(CISales.InvoiceStatus.Key);
        final SelectBuilder selRateCurInst = new SelectBuilder()
                        .linkto(CISales.DocumentSumAbstract.RateCurrencyId).instance();
        multi.addSelect(selContactName, statusName, selRateCurInst);
        addAttribute4MultiPrintQuery(_parameter, multi);
        multi.setEnforceSorted(true);
        multi.execute();
        while (multi.next()) {
            final Instance rateCurDocInst = multi.<Instance>getSelect(selRateCurInst);
            final Object[] ratesDoc = multi.<Object[]>getAttribute(CISales.DocumentSumAbstract.Rate);
            final Map<String, Object> map = new HashMap<>();

            final QueryBuilder queryBldr2 = new QueryBuilder(CISales.Payment);
            queryBldr2.addWhereAttrEqValue(CISales.Payment.CreateDocument, multi.getCurrentInstance());
            final MultiPrintQuery multi2 = queryBldr2.getPrint();
            multi2.addAttribute(CISales.Payment.Amount);
            final SelectBuilder selRatePay = new SelectBuilder().linkto(CISales.Payment.TargetDocument)
                            .attribute(CISales.PaymentDocumentAbstract.Rate);
            final SelectBuilder selRateCurPayInst = new SelectBuilder().linkto(CISales.Payment.TargetDocument)
                            .linkto(CISales.PaymentDocumentAbstract.RateCurrencyLink).instance();
            multi2.addSelect(selRatePay, selRateCurPayInst);
            multi2.execute();

            final List<PaymentOut> lstPayDocs = new ArrayList<>();
            while (multi2.next()) {
                final Instance rateCurPayInst = multi2.<Instance>getSelect(selRateCurPayInst);
                final Object[] rate4Pay = multi2.<Object[]>getSelect(selRatePay);
                final BigDecimal amount = multi2.<BigDecimal>getAttribute(CISales.Payment.Amount);

                lstPayDocs.add(new PaymentOut(amount, rateCurPayInst, rate4Pay));
            }

            BigDecimal acumulatedPay = BigDecimal.ZERO;
            BigDecimal acumulatedRatePay = BigDecimal.ZERO;
            final BigDecimal rateCrossInv = multi
                            .<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
            final BigDecimal crossInv = multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.CrossTotal);
            final Instance baseCurrInst = Currency.getBaseCurrency();
            for (final PaymentOut payOut : lstPayDocs) {
                if (!rateCurDocInst.equals(payOut.getRateCurrency())) {
                    if (!rateCurDocInst.equals(baseCurrInst)) {
                        final Object[] rates = payOut.getRate();
                        final BigDecimal amountPay = payOut.getAmount();
                        final BigDecimal rate = ((BigDecimal) rates[1]).divide((BigDecimal) rates[0], 12,
                                        BigDecimal.ROUND_HALF_UP);
                        final BigDecimal anountPayUpd = amountPay.multiply(rate);
                        acumulatedPay = acumulatedPay.add(anountPayUpd);

                        final BigDecimal rateDoc = ((BigDecimal) ratesDoc[0]).divide((BigDecimal) ratesDoc[1], 12,
                                        BigDecimal.ROUND_HALF_UP);
                        acumulatedRatePay.add(anountPayUpd.multiply(rateDoc));

                    } else {
                        final Object[] rates = payOut.getRate();
                        final BigDecimal amountPay = payOut.getAmount();
                        final BigDecimal rate = ((BigDecimal) rates[1]).divide((BigDecimal) rates[0], 12,
                                        BigDecimal.ROUND_HALF_UP);
                        acumulatedRatePay = acumulatedRatePay.add(amountPay.multiply(rate));
                        acumulatedPay = acumulatedPay.add(amountPay.multiply(rate));
                    }
                } else {
                    if (!rateCurDocInst.equals(baseCurrInst)) {
                        acumulatedRatePay = acumulatedRatePay.add(payOut.getAmount());

                        final Object[] rates = payOut.getRate();
                        final BigDecimal amountPay = payOut.getAmount();
                        final BigDecimal rate = ((BigDecimal) rates[1]).divide((BigDecimal) rates[0], 12,
                                        BigDecimal.ROUND_HALF_UP);
                        acumulatedPay = acumulatedPay.add(amountPay.multiply(rate));
                    } else {
                        acumulatedPay = acumulatedPay.add(payOut.getAmount());
                        acumulatedRatePay = acumulatedRatePay.add(payOut.getAmount());
                    }
                }
            }
            final CurrencyInst curInst = new CurrencyInst(rateCurDocInst);
            map.put("Currency", curInst.getISOCode());
            map.put("Rate", curInst.isInvert() ? ratesDoc[1] : ratesDoc[0]);
            map.put("RateCrossTotal", rateCrossInv);
            map.put("RatePayment", acumulatedRatePay);
            map.put("RateDiscountPayment", rateCrossInv.subtract(acumulatedRatePay));
            map.put("CrossTotal", crossInv);
            map.put("Payment", acumulatedPay);
            map.put("DiscountPayment", crossInv.subtract(acumulatedPay));
            map.put("Id", multi.getCurrentInstance().getId());
            map.put("DocumentType", multi.getCurrentInstance().getType().getLabel());
            map.put("Date", multi.<DateTime>getAttribute(CISales.DocumentSumAbstract.Date));
            map.put("CreateDate", multi.<DateTime>getAttribute(CISales.DocumentSumAbstract.Created));
            map.put("DueDate", multi.<DateTime>getAttribute(CISales.DocumentSumAbstract.DueDate));
            map.put("Name4Doc", multi.<String>getAttribute(CISales.DocumentSumAbstract.Name));
            map.put("ContactName", multi.<String>getSelect(selContactName));
            addMapDataSource4Report(_parameter, multi, map);
            getValues().add(map);
        }
    }


    protected void addMapDataSource4Report(final Parameter _parameter,
                                           final MultiPrintQuery _multi,
                                           final Map<String, Object> _map)
        throws EFapsException
    {
        // to be implement
    }

    protected void addAttribute4MultiPrintQuery(final Parameter _parameter,
                                                final MultiPrintQuery _multi)
        throws EFapsException
    {
        // to be implement
    }

    @SuppressWarnings("unchecked")
    public Return createDocReport(final Parameter _parameter)
        throws EFapsException
    {
        final String dateFrom = _parameter.getParameterValue("dateFrom");
        final String dateTo = _parameter.getParameterValue("dateTo");
        final String mime = _parameter.getParameterValue("mime");
        final Map<String, Object> props = (Map<String, Object>) _parameter.get(ParameterValues.PROPERTIES);
        props.put("Mime", mime);
        final DateTime from = new DateTime(dateFrom);
        final DateTime to = new DateTime(dateTo);
        final StandartReport report = new StandartReport();
        report.setFileName(getReportName(_parameter, from, to));
        report.getJrParameters().put("FromDate", from.toDate());
        report.getJrParameters().put("ToDate", to.toDate());
        report.getJrParameters().put("Mime", mime);
        final Instance baseCurrInst = Currency.getBaseCurrency();

        report.getJrParameters().put("BaseCurrency", CurrencyInst.get(baseCurrInst).getISOCode());
        final SystemConfiguration config = ERP.getSysConfig();
        if (config != null) {
            final String companyName =  ERP.COMPANYNAME.get();
            final String companyTaxNumb = ERP.COMPANYTAX.get();

            if (companyName != null && companyTaxNumb != null && !companyName.isEmpty() && !companyTaxNumb.isEmpty()) {
                report.getJrParameters().put("CompanyName", companyName);
                report.getJrParameters().put("CompanyTaxNum", companyTaxNumb);
            }
        }

        return report.execute(_parameter);
    }

    protected String getReportName(final Parameter _parameter,
                                   final DateTime _from,
                                   final DateTime _to)
    {
        return DBProperties.getProperty("Sales_PaymentDocOutReport.Label", "es")
                        + "-" + _from.toString(DateTimeFormat.shortDate())
                        + "-" + _to.toString(DateTimeFormat.shortDate());
    }

    public class PaymentOut
    {

        private final BigDecimal amount;
        private final Instance rateCurrency;
        private final Object[] rate;

        public PaymentOut(final BigDecimal _amount,
                          final Instance _rateCurrency,
                          final Object[] _rate)
        {
            this.amount = _amount;
            this.rateCurrency = _rateCurrency;
            this.rate = _rate;
        }

        /**
         * @return the amount
         */
        private BigDecimal getAmount()
        {
            return this.amount;
        }

        /**
         * @return the rateCurrency
         */
        private Instance getRateCurrency()
        {
            return this.rateCurrency;
        }

        /**
         * @return the rate
         */
        private Object[] getRate()
        {
            return this.rate;
        }
    }
}
