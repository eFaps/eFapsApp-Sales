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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperReport;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.EFapsMapDataSource;
import org.efaps.esjp.common.jasperreport.StandartReport;
import org.efaps.util.DateTimeUtil;
import org.efaps.util.EFapsException;
import org.efaps.util.cache.CacheReloadException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: DocReport_Base.java 7679 2012-06-14 22:39:59Z
 *          jorge.cueva@moxter.net $
 */
@EFapsUUID("471945cd-8b0c-430d-a2ef-2618f10cbf86")
@EFapsRevision("$Rev$")
public abstract class PaymentDocReport_Base
    extends EFapsMapDataSource
{

    @Override
    public void init(final JasperReport _jasperReport,
                     final Parameter _parameter,
                     final JRDataSource _parentSource,
                     final Map<String, Object> _jrParameters)
        throws EFapsException
    {
        final String contactOid = _parameter.getParameterValue("contact");
        final String contactName = _parameter.getParameterValue("contactAutoComplete");
        final String dateFromStr = _parameter.getParameterValue("dateFrom");
        final String dateToStr = _parameter.getParameterValue("dateTo");
        final DateTime from = DateTimeUtil.normalize(new DateTime(dateFromStr));
        final DateTime to = DateTimeUtil.normalize(new DateTime(dateToStr));

        final QueryBuilder queryBldr = new QueryBuilder(CISales.Invoice.getType());
        queryBldr.addWhereAttrGreaterValue(CISales.Invoice.Date, from.minusMinutes(1));
        queryBldr.addWhereAttrLessValue(CISales.Invoice.Date, to.plusDays(1));
        queryBldr.addWhereAttrEqValue(CISales.Invoice.Status,
                Status.find(CISales.InvoiceStatus.uuid, "Open").getId());
        if (contactOid != null && !contactOid.isEmpty() && contactName != null && !contactName.isEmpty()) {
            queryBldr.addWhereAttrEqValue(CISales.Invoice.Contact, Instance.get(contactOid).getId());
        }
        queryBldr.addOrderByAttributeAsc(CISales.Invoice.Date);

        final SelectBuilder selContactName = new SelectBuilder()
                        .linkto(CISales.Invoice.Contact).attribute(CIContacts.Contact.Name);
        final SelectBuilder statusName = new SelectBuilder()
                        .linkto(CISales.Invoice.Status).attribute(CISales.InvoiceStatus.Key);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.Invoice.Date, CISales.Invoice.Name, CISales.Invoice.RateCurrencyId,
                        CISales.Invoice.CrossTotal, CISales.Invoice.RateCrossTotal);
        multi.addSelect(selContactName, statusName);
        addAttribute4MultiPrintQuery(_parameter, multi);
        multi.setEnforceSorted(true);
        multi.execute();
        while (multi.next()) {
            final Long rateCurInv = multi.<Long>getAttribute(CISales.Invoice.RateCurrencyId);
            final SelectBuilder selRatePay = new SelectBuilder().linkto(CISales.Payment.TargetDocument)
                            .attribute(CISales.PaymentDocumentAbstract.Rate);
            final SelectBuilder selRateCurPay = new SelectBuilder().linkto(CISales.Payment.TargetDocument)
                            .attribute(CISales.PaymentDocumentAbstract.RateCurrencyLink);
            final Map<String, Object> map = new HashMap<String, Object>();
            final QueryBuilder queryBldr2 = new QueryBuilder(CISales.Payment);
            queryBldr2.addWhereAttrEqValue(CISales.Payment.CreateDocument, multi.getCurrentInstance().getId());
            final MultiPrintQuery multi2 = queryBldr2.getPrint();
            multi2.addAttribute(CISales.Payment.Amount);
            multi2.addSelect(selRatePay, selRateCurPay);
            multi2.execute();
            final Map<Long, ArrayList<Object[]>> map4Pay = new HashMap<Long, ArrayList<Object[]>>();
            while (multi2.next()) {
                final Long rateCurPay = multi2.<Long>getSelect(selRateCurPay);
                final Object[] rate4Pay = multi2.<Object[]>getSelect(selRatePay);
                final BigDecimal amount = multi2.<BigDecimal>getAttribute(CISales.Payment.Amount);
                ArrayList<Object[]> list4Pay = new ArrayList<Object[]>();
                if (map4Pay.containsKey(rateCurPay)) {
                    list4Pay = map4Pay.get(rateCurPay);
                }
                list4Pay.add(new Object[] { rate4Pay, amount });
                map4Pay.put(rateCurPay, list4Pay);
            }

            final QueryBuilder queryBldr3 = new QueryBuilder(CISales.InvoicePosition);
            queryBldr3.addWhereAttrEqValue(CISales.InvoicePosition.Invoice, multi.getCurrentInstance().getId());
            queryBldr3.addOrderByAttributeAsc(CISales.InvoicePosition.PositionNumber);
            final InstanceQuery queryInst = queryBldr3.getQuery();
            queryInst.setLimit(1);
            queryInst.execute();
            String getPositionStr = null;
            while (queryInst.next()) {
                final PrintQuery printPos = new PrintQuery(queryInst.getCurrentValue());
                printPos.addAttribute(CISales.InvoicePosition.ProductDesc);
                printPos.execute();
                final String descPos = printPos.<String>getAttribute(CISales.InvoicePosition.ProductDesc);
                final String[] arrays = descPos.split("\n");
                if (arrays.length > 1) {
                    getPositionStr = arrays[0] + " " + arrays[1];
                } else {
                    getPositionStr = descPos.substring(0, descPos.length() > 100 ? 100 : descPos.length());
                }
            }

            BigDecimal acumulatedPay = BigDecimal.ZERO;
            final BigDecimal rateCrossInv = multi.<BigDecimal>getAttribute(CISales.Invoice.RateCrossTotal);
            final BigDecimal crossInv = multi.<BigDecimal>getAttribute(CISales.Invoice.CrossTotal);
            // Sales-Configuration
            final Instance baseCurrInst = SystemConfiguration.get(
                            UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f")).getLink("CurrencyBase");
            if (map4Pay.size() == 1) {
                for (final Entry<Long, ArrayList<Object[]>> getPay : map4Pay.entrySet()) {
                    if (!getPay.getKey().equals(baseCurrInst.getId())) {
                        for (final Object[] iter : getPay.getValue()) {
                            final BigDecimal amountPay = (BigDecimal) iter[1];
                            acumulatedPay = acumulatedPay.add(amountPay);
                        }
                        map.put("RateCrossTotal", rateCrossInv);
                        map.put("RatePayment", acumulatedPay);
                        map.put("RateDiscountPayment", rateCrossInv.subtract(acumulatedPay));
                    } else {
                        for (final Object[] iter : getPay.getValue()) {
                            final Object[] rates = (Object[]) iter[0];
                            final BigDecimal amountPay = (BigDecimal) iter[1];
                            final BigDecimal rate = ((BigDecimal) rates[0]).divide((BigDecimal) rates[1], 12,
                                            BigDecimal.ROUND_HALF_UP);
                            acumulatedPay = acumulatedPay.add(amountPay.divide(rate, RoundingMode.HALF_UP).setScale(2,
                                            BigDecimal.ROUND_HALF_UP));
                        }
                        map.put("CrossTotal", crossInv);
                        map.put("Payment", acumulatedPay);
                        map.put("DiscountPayment", crossInv.subtract(acumulatedPay));
                    }
                }
            } else if (map4Pay.size() > 1) {
                for (final Entry<Long, ArrayList<Object[]>> getPay : map4Pay.entrySet()) {
                    for (final Object[] iter : getPay.getValue()) {
                        final Object[] rates = (Object[]) iter[0];
                        final BigDecimal amountPay = (BigDecimal) iter[1];
                        final BigDecimal rate = ((BigDecimal) rates[0]).divide((BigDecimal) rates[1], 12,
                                        BigDecimal.ROUND_HALF_UP);
                        acumulatedPay = acumulatedPay.add(amountPay.divide(rate, RoundingMode.HALF_UP)
                                        .setScale(2, BigDecimal.ROUND_HALF_UP));
                    }
                }
                map.put("CrossTotal", crossInv);
                map.put("Payment", acumulatedPay);
                map.put("DiscountPayment", crossInv.subtract(acumulatedPay));
            } else {
                if (rateCurInv.equals(baseCurrInst.getId())) {
                    map.put("CrossTotal", crossInv);
                    map.put("Payment", acumulatedPay);
                    map.put("DiscountPayment", crossInv.subtract(acumulatedPay));
                } else {
                    map.put("RateCrossTotal", rateCrossInv);
                    map.put("RatePayment", acumulatedPay);
                    map.put("RateDiscountPayment", rateCrossInv.subtract(acumulatedPay));
                }
            }
            map.put("Id", multi.getCurrentInstance().getId());
            map.put("Date", multi.<DateTime>getAttribute(CISales.Invoice.Date));
            map.put("DueDate", multi.<DateTime>getAttribute(CISales.Invoice.Date));
            map.put("Name4Doc", multi.<String>getAttribute(CISales.Invoice.Name));
            map.put("Description", getPositionStr);
            map.put("ContactName", multi.<String>getSelect(selContactName));
            map.put("Condition", Status.find(CISales.InvoiceStatus.uuid, multi.<String>getSelect(statusName)).getLabel());
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
        return report.execute(_parameter);
    }


    protected String getReportName(final Parameter _parameter,
                                   final DateTime _from,
                                   final DateTime _to)
    {
        return DBProperties.getProperty("Sales_PaymentDocReport.Label", "es")
                        + "-" + _from.toString(DateTimeFormat.shortDate())
                        + "-" + _to.toString(DateTimeFormat.shortDate());
    }
}
