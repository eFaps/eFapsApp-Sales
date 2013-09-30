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

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
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
import org.efaps.db.Context;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.file.FileUtil;
import org.efaps.esjp.common.jasperreport.EFapsMapDataSource;
import org.efaps.esjp.common.jasperreport.EFapsTextReport;
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
    public static final String format = "0626";

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
        LASTNAME("lastName"),
        /** */
        LASTNAME2("lastName2"),
        /** */
        RETVOUCHERSERIE("retentionVoucherSerie"),
        /** */
        RETVOUCHERNUMB("retentionVoucherNumb"),
        /** */
        COMPANYNAME("companyName"),
        /** */
        VOUCHERTYPE("voucherType"),
        /** */
        VOUCHERSERIE("voucherSerie"),
        /** */
        VOUCHERNUMB("voucherNumb"),
        /** */
        VOUCHERDATE("voucherDate"),
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
        attrQueryBldr.addWhereAttrGreaterValue(CISales.PaymentRetentionOut.Date, dateFrom.minusMinutes(1));
        attrQueryBldr.addWhereAttrLessValue(CISales.PaymentRetentionOut.Date, dateTo.plusMinutes(1));
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
        Return ret = new Return();
        final String dateFrom = _parameter.getParameterValue("dateFrom");
        final String dateTo = _parameter.getParameterValue("dateTo");
        final String mime = _parameter.getParameterValue("mime");
        final Map<String, Object> props = (Map<String, Object>) _parameter.get(ParameterValues.PROPERTIES);
        props.put("Mime", mime);
        final DateTime from = new DateTime(dateFrom);
        final DateTime to = new DateTime(dateTo);
        if ("txt".equalsIgnoreCase(mime)) {
            ret = createTextReport(_parameter);
        } else {
            final StandartReport report = new StandartReport();
            buildHeader(_parameter, report);
            report.setFileName(getReportName(_parameter, from, to));
            report.getJrParameters().put("Mime", mime);

            ret = report.execute(_parameter);
        }

        return ret;
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
                        .append(new SimpleDateFormat("MMMMM", Context.getThreadContext().getLocale())
                                        .format(dateFrom.toDate()).toUpperCase())
                        .append(" ")
                        .append(new SimpleDateFormat("yyyy", Context.getThreadContext().getLocale())
                                        .format(dateFrom.toDate()));
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

    public Return createTextReport(final Parameter _parameter)
        throws EFapsException
    {
        return new EFapsTextReport()
        {
            @Override
            protected File getEmptyFile4TextReport()
                throws EFapsException
            {
                final SystemConfiguration erpConfig = ERP.getSysConfig();
                final String companyNumber = erpConfig.getAttributeValue(ERPSettings.COMPANYTAX);
                final String dateFrom = _parameter.getParameterValue("dateFrom");
                final String dateTo = _parameter.getParameterValue("dateTo");
                final DateTime from = new DateTime(dateFrom);
                final DateTime to = new DateTime(dateTo);
                final String name = buildName4TextReport(format, "" + from.getYear(),
                                "" + from.getMonthOfYear(), companyNumber);
                final File file = new FileUtil().getFile(name, "TXT");
                return file;
            }

            @Override
            protected void addHeaderDefinition(final Parameter _parameter,
                                               final List<ColumnDefinition> _columnsList)
                throws EFapsException
            {
            }

            @Override
            protected List<Object> getHeaderData(final Parameter _parameter)
                throws EFapsException
            {
                return null;
            }

            @Override
            protected void addColumnDefinition(final Parameter _parameter,
                                               final List<ColumnDefinition> _columnsList)
                throws EFapsException
            {
                final DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getInstance(Context.getThreadContext()
                                .getLocale());
                decimalFormat.setMaximumIntegerDigits(12);
                decimalFormat.setMaximumFractionDigits(2);
                decimalFormat.setMinimumFractionDigits(2);
                decimalFormat.setGroupingUsed(false);
                decimalFormat.setRoundingMode(RoundingMode.HALF_UP);

                _columnsList.add(new ColumnDefinition("|", 11, 0, null, null, "", Type.STRINGTYPE, null));
                _columnsList.add(new ColumnDefinition("|", 40, 0, null, null, "", Type.STRINGTYPE, null));
                _columnsList.add(new ColumnDefinition("|", 20, 0, null, null, "", Type.STRINGTYPE, null));
                _columnsList.add(new ColumnDefinition("|", 20, 0, null, null, "", Type.STRINGTYPE, null));
                _columnsList.add(new ColumnDefinition("|", 20, 0, null, null, "", Type.STRINGTYPE, null));
                _columnsList.add(new ColumnDefinition("|", 4, 0, null, null, "", Type.STRINGTYPE, null));
                _columnsList.add(new ColumnDefinition("|", 8, 0, null, null, "", Type.STRINGTYPE, null));
                _columnsList.add(new ColumnDefinition("|", 10, 0, null, new SimpleDateFormat("dd/MM/yyyy"),
                                "", Type.DATETYPE, "%1$-10s"));
                _columnsList.add(new ColumnDefinition("|", 12, 2, true, decimalFormat, "0", Type.NUMBERTYPE, null));

                _columnsList.add(new ColumnDefinition("|", 2, 0, null, null, "", Type.STRINGTYPE, null));
                _columnsList.add(new ColumnDefinition("|", 4, 0, null, null, "", Type.STRINGTYPE, null));
                _columnsList.add(new ColumnDefinition("|", 15, 0, null, null, "", Type.STRINGTYPE, null));
                _columnsList.add(new ColumnDefinition("|", 10, 0, null, new SimpleDateFormat("dd/MM/yyyy"),
                                "", Type.DATETYPE, "%1$-10s"));
                _columnsList.add(new ColumnDefinition("|", 12, 2, true, decimalFormat, "0", Type.NUMBERTYPE, null));
            }

            @Override
            protected List<List<Object>> createDataSource(final Parameter _parameter)
                throws EFapsException
            {
                final List<List<Object>> lst = new ArrayList<List<Object>>();


                final DateTime dateFrom = new DateTime(_parameter.getParameterValue("dateFrom"));
                final DateTime dateTo = new DateTime(_parameter.getParameterValue("dateTo"));

                final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.PaymentRetentionOut);
                attrQueryBldr.addWhereAttrGreaterValue(CISales.PaymentRetentionOut.Date, dateFrom.minusMinutes(1));
                attrQueryBldr.addWhereAttrLessValue(CISales.PaymentRetentionOut.Date, dateTo.plusMinutes(1));
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
                final SelectBuilder selDocPersFLName = new SelectBuilder().linkto(CISales.Payment.CreateDocument)
                                .linkto(CISales.DocumentSumAbstract.Contact).clazz(CIContacts.ClassPerson)
                                .attribute(CIContacts.ClassPerson.FirstLastName);
                final SelectBuilder selDocPersSLName = new SelectBuilder().linkto(CISales.Payment.CreateDocument)
                                .linkto(CISales.DocumentSumAbstract.Contact).clazz(CIContacts.ClassPerson)
                                .attribute(CIContacts.ClassPerson.SecondLastName);
                final SelectBuilder selDocPersFName = new SelectBuilder().linkto(CISales.Payment.CreateDocument)
                                .linkto(CISales.DocumentSumAbstract.Contact).clazz(CIContacts.ClassPerson)
                                .attribute(CIContacts.ClassPerson.Forename);
                final SelectBuilder selCrossTotal = new SelectBuilder().linkto(CISales.Payment.CreateDocument)
                                .attribute(CISales.DocumentSumAbstract.RateCrossTotal);
                final SelectBuilder selCreateDocName = new SelectBuilder().linkto(CISales.Payment.CreateDocument)
                                .attribute(CISales.DocumentSumAbstract.Name);
                final SelectBuilder selCreateDocDate = new SelectBuilder().linkto(CISales.Payment.CreateDocument)
                                .attribute(CISales.DocumentSumAbstract.Date);
                final SelectBuilder selTargetDocDate = new SelectBuilder().linkto(CISales.Payment.TargetDocument)
                                .attribute(CISales.PaymentRetentionOut.Date);
                final SelectBuilder selTargetDocName = new SelectBuilder().linkto(CISales.Payment.TargetDocument)
                                .attribute(CISales.PaymentRetentionOut.Name);
                multi.addSelect(selContact, selDocNumDni, selDocNumRuc, selCreateDocDate,
                                selCrossTotal, selTargetDocDate, selCreateDocName, selTargetDocName,
                                selDocPersFLName, selDocPersSLName, selDocPersFName);
                multi.execute();
                final List<Map<String, Object>> lstMap = new ArrayList<Map<String, Object>>();
                while (multi.next()) {
                    final Map<String, Object> map = new HashMap<String, Object>();

                    String contactName = multi.<String>getSelect(selContact);
                    final String firstLastName = multi.<String>getSelect(selDocPersFLName);
                    final String secondLastName = multi.<String>getSelect(selDocPersSLName);
                    final String docNum = multi.<String>getSelect(selDocNumRuc);

                    if (firstLastName != null && !firstLastName.isEmpty()
                                    && secondLastName != null && !secondLastName.isEmpty()) {
                        contactName = multi.<String>getSelect(selDocPersFName);
                    }
                    final DateTime voucherDate = multi.<DateTime>getSelect(selCreateDocDate);
                    final DateTime retVoucherDate = multi.<DateTime>getSelect(selTargetDocDate);
                    final BigDecimal crossTot = multi.<BigDecimal>getSelect(selCrossTotal);
                    final BigDecimal retention = multi.<BigDecimal>getAttribute(CISales.Payment.Amount);

                    map.put(PaymentRetentionReport_Base.Field.DOCNUM.getKey(), docNum);
                    if (firstLastName != null && !firstLastName.isEmpty()
                                    && secondLastName != null && !secondLastName.isEmpty()) {
                        map.put(PaymentRetentionReport_Base.Field.COMPANYNAME.getKey(), null);
                        map.put(PaymentRetentionReport_Base.Field.NAME.getKey(), contactName);
                        map.put(PaymentRetentionReport_Base.Field.LASTNAME.getKey(), firstLastName);
                        map.put(PaymentRetentionReport_Base.Field.LASTNAME2.getKey(), secondLastName);
                    } else {
                        map.put(PaymentRetentionReport_Base.Field.COMPANYNAME.getKey(), contactName);
                        map.put(PaymentRetentionReport_Base.Field.NAME.getKey(), null);
                        map.put(PaymentRetentionReport_Base.Field.LASTNAME.getKey(), null);
                        map.put(PaymentRetentionReport_Base.Field.LASTNAME2.getKey(), null);
                    }

                    final String retentionVoucherName = multi.<String>getSelect(selTargetDocName);
                    final String[] retentionVoucherNameArr = retentionVoucherName.split("-");
                    if (retentionVoucherNameArr.length == 2) {
                        final String retVoucherSerie = retentionVoucherNameArr[0];
                        final String retVoucherNumb = retentionVoucherNameArr[1];
                        map.put(PaymentRetentionReport_Base.Field.RETVOUCHERSERIE.getKey(), retVoucherSerie);
                        map.put(PaymentRetentionReport_Base.Field.RETVOUCHERNUMB.getKey(), retVoucherNumb);
                    } else {
                        final String retVoucherNumb = retentionVoucherNameArr[0];
                        map.put(PaymentRetentionReport_Base.Field.RETVOUCHERSERIE.getKey(), null);
                        map.put(PaymentRetentionReport_Base.Field.RETVOUCHERNUMB.getKey(), retVoucherNumb);
                    }
                    map.put(PaymentRetentionReport_Base.Field.RETENTION.getKey(), retention);
                    map.put(PaymentRetentionReport_Base.Field.PAYDATE.getKey(), retVoucherDate);

                    map.put(PaymentRetentionReport_Base.Field.VOUCHERTYPE.getKey(), "01");

                    final String voucherName = multi.<String>getSelect(selCreateDocName);
                    final String[] voucherNameArr = voucherName.split("-");
                    if (voucherNameArr.length == 2) {
                        final String voucherSerie = voucherNameArr[0];
                        final String voucherNumb = voucherNameArr[1];
                        map.put(PaymentRetentionReport_Base.Field.VOUCHERSERIE.getKey(), voucherSerie);
                        map.put(PaymentRetentionReport_Base.Field.VOUCHERNUMB.getKey(), voucherNumb);
                    } else {
                        final String voucherNumb = voucherNameArr[0];
                        map.put(PaymentRetentionReport_Base.Field.VOUCHERSERIE.getKey(), "");
                        map.put(PaymentRetentionReport_Base.Field.VOUCHERNUMB.getKey(), voucherNumb);
                    }
                    map.put(PaymentRetentionReport_Base.Field.VOUCHERDATE.getKey(), voucherDate);
                    map.put(PaymentRetentionReport_Base.Field.CROSSAMOUNT.getKey(), crossTot);

                    lstMap.add(map);
                }

                Collections.sort(lstMap, new Comparator<Map<String, Object>>() {

                    @Override
                    public int compare(final Map<String, Object> _o1,
                                       final Map<String, Object> _o2)
                    {
                        final DateTime date1 = (DateTime) _o1.get(PaymentRetentionReport_Base.Field.PAYDATE.getKey());
                        final DateTime date2 = (DateTime) _o2.get(PaymentRetentionReport_Base.Field.PAYDATE.getKey());
                        final int ret;
                        ret = date1.compareTo(date2);
                        return ret;
                    }
                });

                for (final Map<String, Object> map : lstMap) {
                    final List<Object> rowLst = new ArrayList<Object>();
                    rowLst.add(map.get(PaymentRetentionReport_Base.Field.DOCNUM.getKey()));
                    rowLst.add(map.get(PaymentRetentionReport_Base.Field.COMPANYNAME.getKey()));
                    rowLst.add(map.get(PaymentRetentionReport_Base.Field.LASTNAME.getKey()));
                    rowLst.add(map.get(PaymentRetentionReport_Base.Field.LASTNAME2.getKey()));
                    rowLst.add(map.get(PaymentRetentionReport_Base.Field.NAME.getKey()));
                    rowLst.add(map.get(PaymentRetentionReport_Base.Field.RETVOUCHERSERIE.getKey()));
                    rowLst.add(map.get(PaymentRetentionReport_Base.Field.RETVOUCHERNUMB.getKey()));
                    rowLst.add(map.get(PaymentRetentionReport_Base.Field.PAYDATE.getKey()));
                    rowLst.add(map.get(PaymentRetentionReport_Base.Field.RETENTION.getKey()));
                    rowLst.add(map.get(PaymentRetentionReport_Base.Field.VOUCHERTYPE.getKey()));
                    rowLst.add(map.get(PaymentRetentionReport_Base.Field.VOUCHERSERIE.getKey()));
                    rowLst.add(map.get(PaymentRetentionReport_Base.Field.VOUCHERNUMB.getKey()));
                    rowLst.add(map.get(PaymentRetentionReport_Base.Field.VOUCHERDATE.getKey()));
                    rowLst.add(map.get(PaymentRetentionReport_Base.Field.CROSSAMOUNT.getKey()));
                    lst.add(rowLst);
                }

                return lst;
            }
        }.getTextReport(_parameter);
    }
}
