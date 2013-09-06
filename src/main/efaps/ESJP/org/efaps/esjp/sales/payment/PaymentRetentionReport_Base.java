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
 * Revision:        $Rev: 8120 $
 * Last Changed:    $Date: 2012-10-26 13:21:34 -0500 (vie, 26 oct 2012) $
 * Last Changed By: $Author: jan@moxter.net $
 */

package org.efaps.esjp.sales.payment;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperReport;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.EFapsMapDataSource;
import org.efaps.esjp.common.jasperreport.StandartReport;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.esjp.erp.util.ERPSettings;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: Account_Base.java 8120 2012-10-26 18:21:34Z jan@moxter.net $
 */
@EFapsUUID("e04973c1-1095-4be3-adb9-6ac2d125ad0a")
@EFapsRevision("$Rev: 8120 $")
public abstract class PaymentRetentionReport_Base
    extends EFapsMapDataSource
{
    /**
     * Enum used to define the keys for the map.
     */
    public enum Field
    {
        /** */
        PAYDATE("paymentDate"),
        /** */
        DOCTYPE("documentType"),
        /** */
        DOCNUM("documentNumber"),
        /** */
        NAME("name"),
        /** */
        CROSSAMOUNT("crossAmount"),
        /** */
        RETENTION("retentionEffect"),
        /** */
        NETAMOUNT("netAmount");

        /**
         * key.
         */
        private final String key;

        /**
         * @param _key key
         */
        private Field(final String _key)
        {
            this.key = _key;
        }

        /**
         * Getter method for the instance variable {@link #key}.
         *
         * @return value of instance variable {@link #key}
         */
        public String getKey()
        {
            return this.key;
        }
    }

    public enum DocType
    {
        /** */
        OTHER("0"),
        /** */
        RUC("06"),
        /** */
        DNI("01"),
        /** */
        CE("04"),
        /** */
        PAS("07");

        /**
         * key.
         */
        private final String key;

        /**
         * @param _key key
         */
        private DocType(final String _key)
        {
            this.key = _key;
        }

        /**
         * Getter method for the instance variable {@link #key}.
         *
         * @return value of instance variable {@link #key}
         */
        public String getKey()
        {
            return this.key;
        }
    }

    @Override
    public void init(final JasperReport _jasperReport,
                     final Parameter _parameter,
                     final JRDataSource _parentSource,
                     final Map<String, Object> _jrParameters)
        throws EFapsException
    {
        final List<Map<String, Object>> values = new ArrayList<Map<String, Object>>();
        final DateTime dateFrom = new DateTime(_parameter.getParameterValue("dateFrom"));
        final DateTime dateTo = new DateTime(_parameter.getParameterValue("dateTo"));

        final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.PaymentRetentionOut);
        attrQueryBldr.addWhereAttrGreaterValue(CISales.PaymentRetentionOut.Date, dateFrom);
        attrQueryBldr.addWhereAttrLessValue(CISales.PaymentRetentionOut.Date, dateTo);
        final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.PaymentRetentionOut.ID);

        final QueryBuilder queryBldr = new QueryBuilder(CISales.Payment);
        queryBldr.addWhereAttrInQuery(CISales.Payment.TargetDocument, attrQuery);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.Payment.Amount,
                            CISales.Payment.CurrencyLink);
        final SelectBuilder selContact = new SelectBuilder().linkto(CISales.Payment.CreateDocument)
                        .linkto(CISales.DocumentSumAbstract.Contact).attribute(CIContacts.Contact.Name);
        final SelectBuilder selDocNumRuc = new SelectBuilder().linkto(CISales.Payment.CreateDocument)
                        .linkto(CISales.DocumentSumAbstract.Contact).clazz(CIContacts.ClassOrganisation)
                        .attribute(CIContacts.ClassOrganisation.TaxNumber);
        final SelectBuilder selDocNumDni = new SelectBuilder().linkto(CISales.Payment.CreateDocument)
                        .linkto(CISales.DocumentSumAbstract.Contact).clazz(CIContacts.ClassPerson)
                        .attribute(CIContacts.ClassPerson.IdentityCard);
        final SelectBuilder selCrossTotal = new SelectBuilder().linkto(CISales.Payment.CreateDocument)
                        .attribute(CISales.DocumentSumAbstract.RateCrossTotal);
        final SelectBuilder selDate = new SelectBuilder().linkto(CISales.Payment.TargetDocument)
                        .attribute(CISales.PaymentRetentionOut.Date);
        multi.addSelect(selContact, selDocNumDni, selDocNumRuc, selCrossTotal, selDate);
        multi.execute();
        while (multi.next()) {
            final Map<String, Object> map = new HashMap<String, Object>();
            final String contactName = multi.<String>getSelect(selContact);
            String docNum = multi.<String>getSelect(selDocNumRuc);
            String docType = PaymentRetentionReport_Base.DocType.RUC.getKey();
            if (docNum == null) {
                docNum = multi.<String>getSelect(selDocNumDni);
                if (docNum.length() == 8) {
                    docType = PaymentRetentionReport_Base.DocType.DNI.getKey();
                } else if (docNum.length() == 9) {
                    docType = PaymentRetentionReport_Base.DocType.CE.getKey();
                } else {
                    docType = PaymentRetentionReport_Base.DocType.OTHER.getKey();
                }
            }
            final DateTime date = multi.<DateTime>getSelect(selDate);
            final BigDecimal crossTot = multi.<BigDecimal>getSelect(selCrossTotal);
            final BigDecimal retention = multi.<BigDecimal>getAttribute(CISales.Payment.Amount);

            map.put(PaymentRetentionReport_Base.Field.PAYDATE.getKey(), date);
            map.put(PaymentRetentionReport_Base.Field.DOCTYPE.getKey(), docType);
            map.put(PaymentRetentionReport_Base.Field.DOCNUM.getKey(), docNum);
            map.put(PaymentRetentionReport_Base.Field.NAME.getKey(), contactName);
            map.put(PaymentRetentionReport_Base.Field.CROSSAMOUNT.getKey(), crossTot);
            map.put(PaymentRetentionReport_Base.Field.RETENTION.getKey(), retention);
            map.put(PaymentRetentionReport_Base.Field.NETAMOUNT.getKey(), crossTot.subtract(retention));

            values.add(map);
        }

        Collections.sort(values, new Comparator<Map<String, Object>>() {

            @Override
            public int compare(final Map<String, Object> _o1,
                               final Map<String, Object> _o2)
            {
                final DateTime date1 = (DateTime) _o1.get(PaymentRetentionReport_Base.Field.PAYDATE.getKey());
                final DateTime date2 = (DateTime) _o2.get(PaymentRetentionReport_Base.Field.PAYDATE.getKey());
                final int ret;
                if (date1.isEqual(date2)) {
                    final String sort1 = (String) _o1.get(PaymentRetentionReport_Base.Field.NAME.getKey());
                    final String sort2 = (String) _o2.get(PaymentRetentionReport_Base.Field.NAME.getKey());
                    ret = sort1.compareTo(sort2);
                } else {
                    ret = date1.compareTo(date2);
                }
                return ret;
            }
        });
        getValues().addAll(values);
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
        buildHeader(_parameter, report);
        report.setFileName(getReportName(_parameter, from, to));
        report.getJrParameters().put("Mime", mime);
        return report.execute(_parameter);
    }

    protected void buildHeader(final Parameter _parameter,
                               final StandartReport report)
        throws EFapsException
    {
        final DateTime dateFrom = new DateTime(_parameter.getParameterValue("dateFrom"));
        final SystemConfiguration erpConfig = ERP.getSysConfig();
        final String companyName = erpConfig.getAttributeValue(ERPSettings.COMPANYNAME);
        final String companyNumber = erpConfig.getAttributeValue(ERPSettings.COMPANYTAX);

        final StringBuilder dateFormat = new StringBuilder()
                        .append(new SimpleDateFormat("MMMMM").format(dateFrom.toDate()).toUpperCase())
                        .append(" ")
                        .append(new SimpleDateFormat("yyyy").format(dateFrom.toDate()));
        report.getJrParameters().put("date", dateFormat.toString());
        report.getJrParameters().put("companyName", companyName);
        report.getJrParameters().put("companyNumber", companyNumber);
    }

    protected String getReportName(final Parameter _parameter,
                                   final DateTime _from,
                                   final DateTime _to)
    {
        return DBProperties.getProperty("Sales_PaymentRetentionReport.Label", "es")
                        + "-" + _from.toString(DateTimeFormat.shortDate())
                        + "-" + _to.toString(DateTimeFormat.shortDate());
    }
}
