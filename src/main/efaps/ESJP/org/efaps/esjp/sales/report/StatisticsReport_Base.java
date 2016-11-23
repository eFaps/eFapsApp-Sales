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
package org.efaps.esjp.sales.report;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.user.Person;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.common.datetime.JodaTimeUtils;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.erp.AbstractGroupedByDate;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.DurationFieldType;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabMeasureBuilder;
import net.sf.dynamicreports.report.constant.Calculation;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * The Class StatisticsReport_Base.
 *
 * @author The eFaps Team
 */
@EFapsUUID("c12fe4eb-eb15-4e08-a532-70e903cd29a4")
@EFapsApplication("eFapsApp-Sales")
public abstract class StatisticsReport_Base
    extends FilteredReport
{

    /**
     * The Enum ReportType.
     *
     */
    public enum ReportType
    {
        /** The documentquantity. */
        TYPE,
        /** The documentquantity. */
        CONTACT,
        /** The documentquantity. */
        CONTACT_TYPE,
        /** The documentquantity. */
        CREATOR,
        /** The documentquantity. */
        CREATOR_TYPE;
    }

    /**
     * The Enum DateGroup.
     *
     * @author The eFaps Team
     */
    public enum DateGroup
    {

        /** The none. */
        NONE(DurationFieldType.years()),
        /** The year. */
        YEAR(DurationFieldType.years()),
        /** Half of a year. */
        HALFYEAR(JodaTimeUtils.halfYears()),
        /** Quarter of a year. */
        QUARTER(JodaTimeUtils.quarters()),
        /** Month. */
        MONTH(DurationFieldType.months());

        /**
         * Fieldtype.
         */
        private final DurationFieldType fieldType;

        /**
         * @param _fieldType fieldType
         */
        DateGroup(final DurationFieldType _fieldType)
        {
            this.fieldType = _fieldType;
        }

        /**
         * @return the fieldType
         */
        public DurationFieldType getFieldType()
        {
            return this.fieldType;
        }
    }

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(StatisticsReport.class);

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return Return containing the file
     * @throws EFapsException on error
     */
    public Return exportReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String mime = getProperty(_parameter, "Mime");
        final AbstractDynamicReport dyRp = getReport(_parameter);
        dyRp.setFileName(getDBProperty("FileName"));
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
     * @param _parameter Parameter as passed by the eFasp API
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
     * Gets the report.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the report
     * @throws EFapsException on error
     */
    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new DynStatisticsReport(this);
    }

    /**
     * The Class DynStatisticsReport.
     */
    public static class DynStatisticsReport
        extends AbstractDynamicReport
    {

        /** The filtered report. */
        private final StatisticsReport_Base filteredReport;

        /**
         * Instantiates a new dyn statistics report.
         *
         * @param _filteredReport the filtered report
         */
        public DynStatisticsReport(final StatisticsReport_Base _filteredReport)
        {
            this.filteredReport = _filteredReport;
        }

        /**
         * Gets the filtered report.
         *
         * @return the filtered report
         */
        public StatisticsReport_Base getFilteredReport()
        {
            return this.filteredReport;
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final JRRewindableDataSource ret;
            if (getFilteredReport().isCached(_parameter)) {
                ret = getFilteredReport().getDataSourceFromCache(_parameter);
                try {
                    ret.moveFirst();
                } catch (final JRException e) {
                    StatisticsReport_Base.LOG.error("Catched", e);
                }
            } else {
                final List<DataBean> beans = new ArrayList<>();
                final DateGroup dateGroup = getDateGroup(_parameter);
                final ReportType reportType = getReportType(_parameter);

                final GroupedByDate groupedByDate = new GroupedByDate();
                DateTimeFormatter dateTimeFormatter = null;
                if (dateGroup != null) {
                    dateTimeFormatter = groupedByDate.getDateTimeFormatter(dateGroup.getFieldType());
                }

                final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter, Sales.REPORT_STATISTICS.get());
                add2QueryBuilder(_parameter, queryBldr);
                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder selContactName = SelectBuilder.get()
                                .linkto(CIERP.DocumentAbstract.Contact).attribute(CIContacts.ContactAbstract.Name);
                final SelectBuilder selStatus = SelectBuilder.get().status().label();
                multi.addSelect(selStatus);
                switch (reportType) {
                    case CREATOR_TYPE:
                    case CREATOR:
                        multi.addAttribute(CIERP.DocumentAbstract.Creator);
                        break;
                    case CONTACT:
                        multi.addSelect(selContactName);
                        break;
                    case CONTACT_TYPE:
                        multi.addSelect(selContactName);
                    case TYPE:
                    default:
                        break;
                }

                multi.addAttribute(CIERP.DocumentAbstract.Date);
                multi.executeWithoutAccessCheck();
                while (multi.next()) {
                    final Type type = multi.getCurrentInstance().getType();
                    final DataBean bean = new DataBean();
                    beans.add(bean);
                    if (DateGroup.NONE.equals(dateGroup)) {
                        bean.setPartial(ERP.COMPANY_NAME.get());
                    } else {
                        bean.setPartial(groupedByDate.getPartial(multi.getAttribute(CIERP.DocumentAbstract.Date),
                                        dateGroup.getFieldType()).toString(dateTimeFormatter));
                    }
                    bean.setStatus(multi.getSelect(selStatus));
                    switch (reportType) {
                        case CREATOR_TYPE:
                            bean.setPerson(multi.<Person>getAttribute(CIERP.DocumentAbstract.Creator).getName());
                            bean.setType(type.getLabel());
                            break;
                        case CREATOR:
                            bean.setPerson(multi.<Person>getAttribute(CIERP.DocumentAbstract.Creator).getName());
                            break;
                        case CONTACT:
                            bean.setContactName(multi.getSelect(selContactName));
                            break;
                        case CONTACT_TYPE:
                            bean.setContactName(multi.getSelect(selContactName));
                        case TYPE:
                            bean.setType(type.getLabel());
                        default:
                            break;
                    }
                }
                ret = new JRBeanCollectionDataSource(beans);
                getFilteredReport().cache(_parameter, ret);
            }
            return ret;
        }

        /**
         * Add to query builder.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _queryBldr the query bldr
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
                dateFrom = new DateTime().minusMonths(1);
            }
            final DateTime dateTo;
            if (filter.containsKey("dateTo")) {
                dateTo = (DateTime) filter.get("dateTo");
            } else {
                dateTo = new DateTime();
            }
            _queryBldr.addWhereAttrGreaterValue(CIERP.DocumentAbstract.Date, dateFrom
                            .withTimeAtStartOfDay().minusMinutes(1));
            _queryBldr.addWhereAttrLessValue(CIERP.DocumentAbstract.Date, dateTo.plusDays(1)
                            .withTimeAtStartOfDay());
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {

            final TextColumnBuilder<String> quantityColumn = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty("Quantity"), "partial", DynamicReports.type.stringType());

            final ReportType reportType = getReportType(_parameter);
            final CrosstabBuilder crosstab = DynamicReports.ctab.crosstab();

            final TextColumnBuilder<String> countColumn;
            switch (reportType) {
                case CREATOR:
                    countColumn = DynamicReports.col.column("person", DynamicReports.type.stringType());
                    crosstab.addRowGroup(DynamicReports.ctab.rowGroup(countColumn).setHeaderWidth(250));
                    break;
                case CREATOR_TYPE:
                    crosstab.addRowGroup(DynamicReports.ctab.rowGroup(DynamicReports.col.column("person",
                                    DynamicReports.type.stringType())).setHeaderWidth(250));
                    countColumn = DynamicReports.col.column("type", DynamicReports.type.stringType());
                    crosstab.addRowGroup(DynamicReports.ctab.rowGroup(countColumn).setHeaderWidth(250));
                    if (isShowStatus(_parameter)) {
                        crosstab.addRowGroup(DynamicReports.ctab.rowGroup(DynamicReports.col.column("status",
                                    DynamicReports.type.stringType())));
                    }
                    break;
                case CONTACT:
                    countColumn = DynamicReports.col.column("contactName", DynamicReports.type.stringType());
                    crosstab.addRowGroup(DynamicReports.ctab.rowGroup(countColumn).setHeaderWidth(250));
                    break;
                case CONTACT_TYPE:
                    crosstab.addRowGroup(DynamicReports.ctab.rowGroup(DynamicReports.col.column("contactName",
                                    DynamicReports.type.stringType())).setHeaderWidth(250));

                    countColumn = DynamicReports.col.column("type", DynamicReports.type.stringType());
                    crosstab.addRowGroup(DynamicReports.ctab.rowGroup(countColumn).setHeaderWidth(250));
                    if (isShowStatus(_parameter)) {
                        crosstab.addRowGroup(DynamicReports.ctab.rowGroup(DynamicReports.col.column("status",
                                    DynamicReports.type.stringType())));
                    }
                    break;
                case TYPE:
                default:
                    countColumn = DynamicReports.col.column("type", DynamicReports.type.stringType());
                    crosstab.addRowGroup(DynamicReports.ctab.rowGroup(countColumn).setHeaderWidth(250));
                    if (isShowStatus(_parameter)) {
                        crosstab.addRowGroup(DynamicReports.ctab.rowGroup(DynamicReports.col.column("status",
                                    DynamicReports.type.stringType())));
                    }
                    break;
            }

            final CrosstabColumnGroupBuilder<String> quantityColumnGroup = DynamicReports.ctab.columnGroup(
                            quantityColumn).setShowTotal(false);

            final CrosstabMeasureBuilder<BigDecimal> countMeasure = DynamicReports.ctab.measure(getFilteredReport()
                            .getDBProperty("count"), countColumn, Calculation.COUNT);

            crosstab.addMeasure(countMeasure).addColumnGroup(quantityColumnGroup);

            _builder.addSummary(crosstab);
        }

        /**
         * Gets the date group.
         *
         * @param _parameter the parameter
         * @return the date group
         * @throws EFapsException the e faps exception
         */
        protected ReportType getReportType(final Parameter _parameter)
            throws EFapsException
        {
            final ReportType ret;
            final Map<String, Object> filterMap = getFilteredReport().getFilterMap(_parameter);
            if (filterMap.containsKey("reportType") && filterMap.get("reportType") != null) {
                ret = (ReportType) ((EnumFilterValue) filterMap.get("reportType")).getObject();
            } else {
                ret = ReportType.TYPE;
            }
            return ret;
        }

        /**
         * Gets the date group.
         *
         * @param _parameter the parameter
         * @return the date group
         * @throws EFapsException the e faps exception
         */
        protected DateGroup getDateGroup(final Parameter _parameter)
            throws EFapsException
        {
            final DateGroup ret;
            final Map<String, Object> filterMap = getFilteredReport().getFilterMap(_parameter);
            if (filterMap.containsKey("dateGroup") && filterMap.get("dateGroup") != null) {
                ret = (DateGroup) ((EnumFilterValue) filterMap.get("dateGroup")).getObject();
            } else {
                ret = DateGroup.NONE;
            }
            return ret;
        }

        /**
         * Checks if is show status.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return true, if is show status
         * @throws EFapsException on error
         */
        protected boolean isShowStatus(final Parameter _parameter)
            throws EFapsException
        {
            final Map<String, Object> filterMap = getFilteredReport().getFilterMap(_parameter);
            return (Boolean) filterMap.get("showStatus");
        }
    }

    /**
     * The Class DataBean.
     */
    public static class DataBean
    {

        /** The type. */
        private String type;

        /** The partial. */
        private String partial;

        /** The contactName. */
        private String contactName;

        /** The contactName. */
        private String person;

        /** The status. */
        private String status;

        /**
         * Gets the type.
         *
         * @return the type
         */
        public String getType()
        {
            return this.type;
        }

        /**
         * Sets the type.
         *
         * @param _type the type
         * @return the data bean
         */
        public DataBean setType(final String _type)
        {
            this.type = _type;
            return this;
        }

        /**
         * Gets the partial.
         *
         * @return the partial
         */
        public String getPartial()
        {
            return this.partial;
        }

        /**
         * Sets the partial.
         *
         * @param _partial the partial
         * @return the data bean
         */
        public DataBean setPartial(final String _partial)
        {
            this.partial = _partial;
            return this;
        }

        /**
         * Gets the contactName.
         *
         * @return the contactName
         */
        public String getContactName()
        {
            return this.contactName;
        }

        /**
         * Sets the contact name.
         *
         * @param _contactName the contact name
         * @return the data bean
         */
        public DataBean setContactName(final String _contactName)
        {
            this.contactName = _contactName;
            return this;
        }

        /**
         * Gets the contactName.
         *
         * @return the contactName
         */
        public String getPerson()
        {
            return this.person;
        }

        /**
         * Sets the person.
         *
         * @param _person the person
         * @return the data bean
         */
        public DataBean setPerson(final String _person)
        {
            this.person = _person;
            return this;
        }

        /**
         * Gets the status.
         *
         * @return the status
         */
        public String getStatus()
        {
            return this.status;
        }

        /**
         * Sets the status.
         *
         * @param _status the status
         * @return the data bean
         */
        public DataBean setStatus(final String _status)
        {
            this.status = _status;
            return this;
        }
    }

    /**
     * The Class GroupedByDate.
     */
    public static class GroupedByDate
        extends AbstractGroupedByDate
    {

    }
}
