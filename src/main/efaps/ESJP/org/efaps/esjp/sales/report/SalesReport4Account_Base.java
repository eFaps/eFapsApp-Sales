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
import java.util.Date;
import java.util.HashMap;
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

import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.FilteredReport;
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
     * @param _parameter    Parameter as passed by the eFasp API
     * @return Return containing html snipplet
     * @throws EFapsException on error
     */
    public Return generateReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final AbstractDynamicReport dyRp = getReport(_parameter);
        dyRp.setFileName("SaleReport4Account");
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
        final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String mime = (String) props.get("Mime");
        final AbstractDynamicReport dyRp = getReport(_parameter);
        dyRp.setFileName("SaleReport4Account");
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
        return new Report4Account(this);
    }

    public static class Report4Account
        extends AbstractDynamicReport
    {

        /**
         * variable to report.
         */
        private final SalesReport4Account_Base filteredReport;

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

            final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter);
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

                final Map<String, Object> map = new HashMap<String, Object>();

                map.put("docOID", multi.getCurrentInstance().getOid());
                map.put("docType", multi.getCurrentInstance().getType().getLabel());
                map.put("docCreated", created != null ? created.toDate() : created);
                map.put("docDate", date != null ? date.toDate() : date);
                map.put("docName", name);
                map.put("docContactName", contactName);
                map.put("docDueDate", dueDate != null ? dueDate.toDate() : dueDate);
                map.put("docCrossTotal", crossTotal);
                map.put("docPayment", BigDecimal.ZERO);
                map.put("docDifference", BigDecimal.ZERO);
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
                    final Date date1 = (Date) _o1.get("docDate");
                    final Date date2 = (Date) _o2.get("docDate");
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
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final boolean showDetails = Boolean.parseBoolean(getProperty(_parameter, "ShowDetails"));

            final TextColumnBuilder<Date> monthColumn = DynamicReports.col.column(DBProperties
                            .getProperty(SalesReport4Account.class.getName() + ".FilterDate1"), "docDate",
                            DynamicReports.type.dateMonthType());
            final TextColumnBuilder<Date> yearColumn = DynamicReports.col.column(DBProperties
                            .getProperty(SalesReport4Account.class.getName() + ".FilterDate2"), "docDate",
                            DynamicReports.type.dateYearType());
            final TextColumnBuilder<Date> dateColumn = DynamicReports.col.column(DBProperties
                            .getProperty(SalesReport4Account.class.getName() + ".Created"), "docDate",
                            DynamicReports.type.dateType());

            final TextColumnBuilder<String> typeColumn = DynamicReports.col.column(DBProperties
                            .getProperty(SalesReport4Account.class.getName() + ".Type"), "docType",
                            DynamicReports.type.stringType());

            final TextColumnBuilder<Date> createdColumn = DynamicReports.col.column(DBProperties
                            .getProperty(SalesReport4Account.class.getName() + ".Date"), "docCreated",
                            DynamicReports.type.dateType());

            final TextColumnBuilder<String> nameColumn = DynamicReports.col.column(DBProperties
                            .getProperty(SalesReport4Account.class.getName() + ".Name"), "docName",
                            DynamicReports.type.stringType());

            final TextColumnBuilder<String> contactNameColumn = DynamicReports.col.column(DBProperties
                            .getProperty(SalesReport4Account.class.getName() + ".ContactName"), "docContactName",
                            DynamicReports.type.stringType());

            final TextColumnBuilder<Date> dueDateColumn = DynamicReports.col.column(DBProperties
                            .getProperty(SalesReport4Account.class.getName() + ".DueDate"), "docDueDate",
                            DynamicReports.type.dateType());

            final TextColumnBuilder<BigDecimal> crossTotalColumn = DynamicReports.col.column(DBProperties
                            .getProperty(SalesReport4Account.class.getName() + ".CrossTotal"), "docCrossTotal",
                            DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<BigDecimal> paymentColumn = DynamicReports.col.column(DBProperties
                            .getProperty(SalesReport4Account.class.getName() + ".Payment"), "docPayment",
                            DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<BigDecimal> differenceColumn = DynamicReports.col.column(DBProperties
                            .getProperty(SalesReport4Account.class.getName() + ".Difference"), "docDifference",
                            DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<String> rateCurrencyColumn = DynamicReports.col.column(DBProperties
                            .getProperty(SalesReport4Account.class.getName() + ".RateCurrency"), "docRateCurrency",
                            DynamicReports.type.stringType());

            final TextColumnBuilder<BigDecimal> rateObjectColumn = DynamicReports.col.column(DBProperties
                            .getProperty(SalesReport4Account.class.getName() + ".RateObject"), "docRateObject",
                            DynamicReports.type.bigDecimalType());

            final ColumnGroupBuilder yearGroup  = DynamicReports.grp.group(yearColumn).groupByDataType();
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
    }

    public static class LinkExpression
        extends AbstractComplexExpression<EmbeddedLink>
    {

        private static final long serialVersionUID = 1L;

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
}
