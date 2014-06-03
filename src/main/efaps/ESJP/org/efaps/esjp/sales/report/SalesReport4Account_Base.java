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

package org.efaps.esjp.sales.report;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.ComponentColumnBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.component.GenericElementBuilder;
import net.sf.dynamicreports.report.builder.expression.AbstractComplexExpression;
import net.sf.dynamicreports.report.builder.group.ColumnGroupBuilder;
import net.sf.dynamicreports.report.constant.HorizontalAlignment;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.jasperreports.engine.JRDataSource;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport_Base;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeDate;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeMonth;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeYear;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.SalesSettings;
import org.efaps.ui.wicket.models.EmbeddedLink;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("d095f1fb-9286-4f93-a030-715873826dca")
@EFapsRevision("$Rev$")
public abstract class SalesReport4Account_Base
    extends FilteredReport
{

    /**
     * Used to distinguish between incoming and outgoing report.
     */
    public enum ReportKey
    {
        /** Incoming. */
        IN,
        /** Outgoing. */
        OUT
    }

    /**
     * ReportKey for this report.
     */
    private ReportKey reportKey = ReportKey.IN;

    /**
     * @param _parameter    Parameter as passed by the eFasp API
     * @return Return containing html snipplet
     * @throws EFapsException on error
     */
    public Return generateReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final AbstractDynamicReport dyRp = getReport(_parameter);
        final String html = dyRp.getHtmlSnipplet(_parameter);
        ret.put(ReturnValues.SNIPLETT, html);
        return ret;
    }

    /**
     * Method to export (PDF/XLS).
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return file.
     * @throws EFapsException on error.
     */
    public Return exportReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String mime = getProperty(_parameter, "Mime");
        final AbstractDynamicReport dyRp = getReport(_parameter);
        dyRp.setFileName(getLabel(_parameter, "FileName"));
        File file = null;
        if ("xls".equalsIgnoreCase(mime)) {
            file = dyRp.getExcel(_parameter);
        } else if ("pdf".equalsIgnoreCase(mime)) {
            file = dyRp.getPDF(_parameter);
        }
        ret.put(ReturnValues.VALUES, file);
        ret.put(ReturnValues.TRUE, true);

        return ret;
    }

    /**
     * Method to get a new Report instance-of class Report4Account.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return DynamicReport to class.
     * @throws EFapsException on error.
     */
    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        this.reportKey = ReportKey.valueOf(getProperty(_parameter, "ReportKey"));
        return new Report4Account(this);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _key key to be searched
     * @return new Label
     */
    protected String getLabel(final Parameter _parameter,
                              final String _key)
    {
        return DBProperties.getProperty(SalesReport4Account.class.getName() + "." + getReportKey() + "." + _key);
    }

    /**
     * Getter method for the instance variable {@link #reportKey}.
     *
     * @return value of instance variable {@link #reportKey}
     */
    protected ReportKey getReportKey()
    {
        return this.reportKey;
    }

    /**
     * Setter method for instance variable {@link #reportKey}.
     *
     * @param _reportKey value for instance variable {@link #reportKey}
     */
    protected void setReportKey(final ReportKey _reportKey)
    {
        this.reportKey = _reportKey;
    }

    /**
     * Report class.
     */
    public static class Report4Account
        extends AbstractDynamicReport
    {

        /**
         * variable to report.
         */
        private final SalesReport4Account_Base filteredReport;

        /**
         * @param _report4Account class used
         */
        public Report4Account(final SalesReport4Account_Base _report4Account)
        {
            this.filteredReport = _report4Account;
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final DRDataSource dataSource = new DRDataSource("docOID", "docType", "docCreated", "docDate", "docName",
                            "docContactName", "docDueDate", "docCrossTotal", "docPayment", "docDifference",
                            "docRateCurrency", "docRateObject");

            final List<Map<String, Object>> lst = new ArrayList<Map<String, Object>>();

            final Map<String, Object> filter = this.filteredReport.getFilterMap(_parameter);

            boolean offset = false;
            if (filter.containsKey("switch")) {
                offset = (boolean) filter.get("switch");
            }
            final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter, offset ? 100 : 0);
            add2QueryBuilder(_parameter, queryBldr);

            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CISales.DocumentSumAbstract.Created,
                            CISales.DocumentSumAbstract.Date,
                            CISales.DocumentSumAbstract.Name,
                            CISales.DocumentSumAbstract.DueDate,
                            CISales.DocumentSumAbstract.Rate,
                            CISales.DocumentSumAbstract.CrossTotal);

            final SelectBuilder selContactInst = new SelectBuilder()
                                    .linkto(CISales.DocumentSumAbstract.Contact).instance();
            final SelectBuilder selContactName = new SelectBuilder()
                                    .linkto(CISales.DocumentSumAbstract.Contact).attribute(CIContacts.Contact.Name);

            final SelectBuilder selRateCurInst = new SelectBuilder()
                                    .linkto(CISales.DocumentSumAbstract.RateCurrencyId).instance();

            multi.addSelect(selContactInst, selContactName, selRateCurInst);
            multi.execute();
            while (multi.next()) {
                final Object[] rate = multi.<Object[]>getAttribute(CISales.DocumentSumAbstract.Rate);
                final Instance rateCurDocInst = multi.<Instance>getSelect(selRateCurInst);
                final CurrencyInst rateCurInst = new CurrencyInst(rateCurDocInst);
                final DateTime created = multi.<DateTime>getAttribute(CISales.DocumentSumAbstract.Created);
                final DateTime date = multi.<DateTime>getAttribute(CISales.DocumentSumAbstract.Date);
                final DateTime dueDate = multi.<DateTime>getAttribute(CISales.DocumentSumAbstract.DueDate);
                final String name = multi.<String>getAttribute(CISales.DocumentSumAbstract.Name);
                final String contactName = multi.<String>getSelect(selContactName);
                final BigDecimal crossTotal = multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.CrossTotal);
                final BigDecimal payment = getPayments4Document(_parameter, multi.getCurrentInstance());

                final Map<String, Object> map = new HashMap<String, Object>();

                map.put("docOID", multi.getCurrentInstance().getOid());
                map.put("docType", multi.getCurrentInstance().getType().getLabel());
                map.put("docCreated", created);
                map.put("docDate", date);
                map.put("docName", name);
                map.put("docContactName", contactName);
                map.put("docDueDate", dueDate);
                map.put("docCrossTotal", crossTotal);
                map.put("docPayment", payment);
                map.put("docDifference", crossTotal.subtract(payment));
                map.put("docRateCurrency", rateCurInst.getISOCode());
                map.put("docRateObject", rateCurInst.isInvert() ? rate[1] : rate[0]);
                lst.add(map);
            }

            Collections.sort(lst, new Comparator<Map<String, Object>>()
            {
                @Override
                public int compare(final Map<String, Object> _o1,
                                   final Map<String, Object> _o2)
                {
                    final DateTime date1 = (DateTime) _o1.get("docDate");
                    final DateTime date2 = (DateTime) _o2.get("docDate");
                    final int ret;
                    if (date1.equals(date2)) {
                        final String contact1 = (String) _o1.get("docContactName");
                        final String contact2 = (String) _o2.get("docContactName");
                        ret = contact1.compareTo(contact2);
                    } else {
                        ret = date1.compareTo(date2);
                    }
                    return ret;
                }
            });

            for (final Map<String, Object> map : lst) {
                dataSource.add(map.get("docOID"),
                                map.get("docType"),
                                map.get("docCreated"),
                                map.get("docDate"),
                                map.get("docName"),
                                map.get("docContactName"),
                                map.get("docDueDate"),
                                map.get("docCrossTotal"),
                                map.get("docPayment"),
                                map.get("docDifference"),
                                map.get("docRateCurrency"),
                                map.get("docRateObject"));
            }

            return dataSource;
        }

        /**
         * @param _parameter Parameter as passed from the eFaps API
         * @param _queryBldr QueryBuilder the criteria will be added to
         * @throws EFapsException on error
         */
        protected void add2QueryBuilder(final Parameter _parameter,
                                        final QueryBuilder _queryBldr)
            throws EFapsException
        {
            final Map<String, Object> filter = this.filteredReport.getFilterMap(_parameter);
            final DateTime dateFrom;
            if (filter.containsKey("dateFrom")) {
                dateFrom = (DateTime) filter.get("dateFrom");
            } else {
                dateFrom = new DateTime().minusYears(1);
            }
            final DateTime dateTo;
            if (filter.containsKey("dateTo")) {
                dateTo = (DateTime) filter.get("dateTo");
            } else {
                dateTo = new DateTime();
            }
            if (filter.containsKey("contact")) {
                final Instance contact = ((ContactFilterValue) filter.get("contact")).getObject();
                if (contact.isValid()) {
                    _queryBldr.addWhereAttrEqValue(CISales.DocumentSumAbstract.Contact, contact);
                }
            }
            _queryBldr.addWhereAttrGreaterValue(CISales.DocumentSumAbstract.Date, dateFrom);
            _queryBldr.addWhereAttrLessValue(CISales.DocumentSumAbstract.Date, dateTo.plusDays(1)
                            .withTimeAtStartOfDay());

            final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Exchange2IncomingInvoice);
            attrQueryBldr.addType(CISales.Exchange2PaymentOrder,
                            CISales.IncomingExchange2CollectionOrder,
                            CISales.IncomingExchange2Invoice);
            final AttributeQuery attrQuery = attrQueryBldr
                            .getAttributeQuery(CISales.Document2DocumentAbstract.ToAbstractLink);

            _queryBldr.addWhereAttrNotInQuery(CISales.DocumentSumAbstract.ID, attrQuery);
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final boolean showDetails = Boolean.parseBoolean(getProperty(_parameter, "ShowDetails"));

            final TextColumnBuilder<DateTime> monthColumn = AbstractDynamicReport_Base.column(
                            this.filteredReport.getLabel(_parameter, "FilterDate1"), "docDate",
                            DateTimeMonth.get());
            final TextColumnBuilder<DateTime> yearColumn = AbstractDynamicReport_Base.column(
                            this.filteredReport.getLabel(_parameter, "FilterDate2"), "docDate",
                            DateTimeYear.get());
            final TextColumnBuilder<DateTime> dateColumn = AbstractDynamicReport_Base.column(
                            this.filteredReport.getLabel(_parameter, "Created"), "docDate",
                            DateTimeDate.get());

            final TextColumnBuilder<String> typeColumn = DynamicReports.col.column(
                            this.filteredReport.getLabel(_parameter, "Type"), "docType",
                            DynamicReports.type.stringType());

            final TextColumnBuilder<DateTime> createdColumn = AbstractDynamicReport_Base.column(
                            this.filteredReport.getLabel(_parameter, "Date"), "docCreated",
                            DateTimeDate.get());

            final TextColumnBuilder<String> nameColumn = DynamicReports.col.column(
                            this.filteredReport.getLabel(_parameter, "Name"), "docName",
                            DynamicReports.type.stringType());

            final TextColumnBuilder<String> contactNameColumn = DynamicReports.col.column(
                            this.filteredReport.getLabel(_parameter, "ContactName"), "docContactName",
                            DynamicReports.type.stringType());

            final TextColumnBuilder<DateTime> dueDateColumn = AbstractDynamicReport_Base.column(
                            this.filteredReport.getLabel(_parameter, "DueDate"), "docDueDate",
                            DateTimeDate.get());

            final TextColumnBuilder<BigDecimal> crossTotalColumn = DynamicReports.col.column(
                            this.filteredReport.getLabel(_parameter, "CrossTotal"), "docCrossTotal",
                            DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<BigDecimal> paymentColumn = DynamicReports.col.column(
                            this.filteredReport.getLabel(_parameter, "Payment"), "docPayment",
                            DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<BigDecimal> differenceColumn = DynamicReports.col.column(
                            this.filteredReport.getLabel(_parameter, "Difference"), "docDifference",
                            DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<String> rateCurrencyColumn = DynamicReports.col.column(
                            this.filteredReport.getLabel(_parameter, "RateCurrency"), "docRateCurrency",
                            DynamicReports.type.stringType());

            final TextColumnBuilder<BigDecimal> rateObjectColumn = DynamicReports.col.column(
                            this.filteredReport.getLabel(_parameter, "RateObject"), "docRateObject",
                            DynamicReports.type.bigDecimalType());

            final ColumnGroupBuilder yearGroup = DynamicReports.grp.group(yearColumn).groupByDataType();
            final ColumnGroupBuilder monthGroup = DynamicReports.grp.group(monthColumn).groupByDataType();

            final GenericElementBuilder linkElement = DynamicReports.cmp.genericElement(
                            "http://www.efaps.org", "efapslink")
                            .addParameter(EmbeddedLink.JASPER_PARAMETERKEY, new LinkExpression())
                            .setHeight(12).setWidth(25);

            final ComponentColumnBuilder linkColumn = DynamicReports.col.componentColumn(linkElement).setTitle("");

            _builder.addColumn(yearColumn, monthColumn);

            if (getExType().equals(ExportType.HTML)) {
                _builder.addColumn(linkColumn);
            }
            _builder.addColumn(typeColumn.setFixedWidth(120),
                                createdColumn,
                                dateColumn,
                                nameColumn.setHorizontalAlignment(HorizontalAlignment.CENTER).setFixedWidth(140),
                                contactNameColumn.setFixedWidth(200),
                                dueDateColumn,
                                crossTotalColumn,
                                paymentColumn,
                                differenceColumn,
                                rateCurrencyColumn.setHorizontalAlignment(HorizontalAlignment.CENTER).setFixedWidth(70),
                                rateObjectColumn);
            if (!showDetails) {
                _builder.setShowColumnValues(false);
            }
            _builder.groupBy(yearGroup, monthGroup);
        }

        /**
         * Method to obtains totals of the payments relations to document.
         *
         * @param _parameter    Parameter as passed by the eFaps API
         * @param _docInst      Instance of the payment
         * @return value of the payments
         * @throws EFapsException on error
         */
        protected BigDecimal getPayments4Document(final Parameter _parameter,
                                                  final Instance _docInst)
            throws EFapsException
        {
            BigDecimal ret = BigDecimal.ZERO;

            if (_docInst.isValid()) {
                final SelectBuilder sel = new SelectBuilder().linkto(CISales.Payment.TargetDocument);
                final SelectBuilder selTypePay = new SelectBuilder(sel).type();
                final SelectBuilder selRatePay = new SelectBuilder(sel)
                                .attribute(CISales.PaymentDocumentAbstract.Rate);
                final SelectBuilder selRateCurPay = new SelectBuilder(sel)
                                .linkto(CISales.PaymentDocumentAbstract.RateCurrencyLink).instance();

                final QueryBuilder queryBldr = new QueryBuilder(CISales.Payment);
                queryBldr.addWhereAttrEqValue(CISales.Payment.CreateDocument, _docInst);
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addSelect(selTypePay, selRatePay, selRateCurPay);
                multi.addAttribute(CISales.Payment.Amount);
                multi.execute();

                final List<PaymentInfoDocument4Report> lstPayDocs = new ArrayList<PaymentInfoDocument4Report>();
                while (multi.next()) {
                    final Type type = multi.<Type>getSelect(selTypePay);
                    if (type.isKindOf(CISales.PaymentDocumentIOAbstract.getType())) {
                        final Instance rateCurInst = multi.<Instance>getSelect(selRateCurPay);
                        final Object[] ratePay = multi.<Object[]>getSelect(selRatePay);
                        final BigDecimal amount = multi.<BigDecimal>getAttribute(CISales.Payment.Amount);

                        final PaymentInfoDocument4Report payInfo = new PaymentInfoDocument4Report();
                        payInfo.setAmount(amount);
                        payInfo.setCurrency(rateCurInst);
                        payInfo.setRate(ratePay);
                        payInfo.setDocument(_docInst);
                        lstPayDocs.add(payInfo);
                    }
                }
                if (!lstPayDocs.isEmpty()) {
                    final Instance curBase = Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);
                    final Iterator<PaymentInfoDocument4Report> iter = lstPayDocs.iterator();
                    while (iter.hasNext()) {
                        final PaymentInfoDocument4Report current = iter.next();
                        if (current.getCurrDocument() != null
                                        && current.getCurrency() != null) {
                            if (current.getCurrDocument().equals(curBase)
                                            && current.getCurrency().equals(curBase)) {
                                ret = ret.add(current.getAmount());
                            } else if (current.getCurrDocument().equals(curBase)
                                            && !current.getCurrency().equals(curBase)) {
                                final Object[] rates = current.getRate();
                                final BigDecimal amountPay = current.getAmount();
                                final BigDecimal rate = ((BigDecimal) rates[1]).divide((BigDecimal) rates[0], 12,
                                                BigDecimal.ROUND_HALF_UP);
                                ret = ret.add(amountPay.multiply(rate));
                            } else {
                                final Object[] rates = current.getRate();
                                final BigDecimal amountPay = current.getAmount();
                                final BigDecimal rate = ((BigDecimal) rates[0]).divide((BigDecimal) rates[1], 12,
                                                BigDecimal.ROUND_HALF_UP);
                                ret = ret.add(amountPay.multiply(rate));
                            }
                        }
                    }
                }

            }

            return ret;
        }
    }

    /**
     * Link class.
     */
    public static class LinkExpression
        extends AbstractComplexExpression<EmbeddedLink>
    {

        /**
         * Needed for serialization.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Constructor.
         */
        public LinkExpression()
        {
            addExpression(DynamicReports.field("docOID", String.class));
        }

        @Override
        public EmbeddedLink evaluate(final List<?> _values,
                                     final ReportParameters _reportParameters)
        {
            final String oid = (String) _values.get(0);
            return EmbeddedLink.getJasperLink(oid);
        }
    }

    /**
     * TODO comment!
     *
     * @author The eFaps Team
     * @version $Id$
     */
    public static class PaymentInfoDocument4Report
    {

        /**
         * amount of the pay.
         */
        private BigDecimal amount;

        /**
         * currency of the pay.
         */
        private Instance currency;

        /**
         * rate of the pay.
         */
        private Object[] rate;

        /**
         * Instance of the document.
         */
        private Instance document;


        /**
         * class.
         */
        public PaymentInfoDocument4Report()
        {

        }

        /**
         * @param _document to the document.
         */
        protected void setDocument(final Instance _document)
        {
            this.document = _document;
        }

        /**
         * @param _amount to the amount pay.
         */
        protected void setAmount(final BigDecimal _amount)
        {
            this.amount = _amount;
        }

        /**
         * @param _currency to the currency pay.
         */
        protected void setCurrency(final Instance _currency)
        {
            this.currency = _currency;
        }

        /**
         * @param _rate to the rate pay.
         */
        protected void setRate(final Object[] _rate)
        {
            this.rate = _rate;
        }

        /**
         * @return the amount
         */
        protected BigDecimal getAmount()
        {
            return this.amount;
        }

        /**
         * @return the rateCurrency
         */
        protected Instance getCurrency()
        {
            return this.currency;
        }

        /**
         * @return the rate
         */
        protected Object[] getRate()
        {
            return this.rate;
        }

        /**
         * @return Instance to the currency of the document.
         * @throws EFapsException on error.
         */
        protected Instance getCurrDocument()
            throws EFapsException
        {
            Instance ret = null;
            if (this.document != null && this.document.isValid()) {
                final SelectBuilder selRateCur = new SelectBuilder()
                                        .linkto(CISales.DocumentSumAbstract.RateCurrencyId).instance();
                final PrintQuery print = new PrintQuery(this.document);
                print.addSelect(selRateCur);
                print.execute();

                ret = print.<Instance>getSelect(selRateCur);
            }
            return ret;
        }
    }
}
