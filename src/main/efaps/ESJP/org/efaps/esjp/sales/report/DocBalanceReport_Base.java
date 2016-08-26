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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabMeasureBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabRowGroupBuilder;
import net.sf.dynamicreports.report.builder.expression.AbstractComplexExpression;
import net.sf.dynamicreports.report.constant.Calculation;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * The Class DocBalanceReport_Base.
 *
 * @author The eFaps Team
 */
@EFapsUUID("d555b376-c8dd-45a2-9cf1-86745119fbe4")
@EFapsApplication("eFapsApp-Sales")
public abstract class DocBalanceReport_Base
    extends FilteredReport
{

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
     * @return the report class
     * @throws EFapsException on error
     */
    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new DynDocBalanceReport(this);
    }

    /**
     * The Class DynDocBalanceReport.
     */
    public static class DynDocBalanceReport
        extends AbstractDynamicReport
    {

        /** The filtered report. */
        private final DocBalanceReport_Base filteredReport;

        /**
         * Instantiates a new dyn doc balance report.
         *
         * @param _filteredReport the filtered report
         */
        public DynDocBalanceReport(final DocBalanceReport_Base _filteredReport)
        {
            this.filteredReport = _filteredReport;
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
                    throw new EFapsException("JRException", e);
                }
            } else {
                final Collection<DataBean> values = new ArrayList<>();
                final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter, Sales.REPORT_DOCBALANCE.get());
                add2QueryBldr(_parameter, queryBldr);

                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder selContact = SelectBuilder.get().linkto(CIERP.DocumentAbstract.Contact)
                                .attribute(CIContacts.ContactAbstract.Name);
                multi.addSelect(selContact);
                multi.addAttribute(CISales.DocumentSumAbstract.CrossTotal, CISales.DocumentSumAbstract.NetTotal,
                                CISales.DocumentSumAbstract.StatusAbstract);
                multi.execute();
                while (multi.next()) {
                    final BigDecimal amount = multi.getAttribute(CISales.DocumentSumAbstract.NetTotal);
                    final Status status = Status.get(multi.<Long>getAttribute(
                                    CISales.DocumentSumAbstract.StatusAbstract));
                    final DataBean bean = new DataBean()
                            .setType(multi.getCurrentInstance().getType().getLabel())
                            .setAmount(amount)
                            .setStatus(status)
                            .setContact(multi.<String>getSelect(selContact))
                            .setPartial("");
                    values.add(bean);
                }
                ret = new JRBeanCollectionDataSource(values);
                getFilteredReport().cache(_parameter, ret);
            }
            return ret;
        }

        /**
         * @param _parameter Parameter as passed from the eFaps API.
         * @param _queryBldr the query bldr
         * @throws EFapsException on error.
         */
        protected void add2QueryBldr(final Parameter _parameter,
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
            _queryBldr.addWhereAttrGreaterValue(CIERP.DocumentAbstract.Date, dateFrom.minusMinutes(1));
            _queryBldr.addWhereAttrLessValue(CIERP.DocumentAbstract.Date, dateTo.plusDays(1));

            if (filter.containsKey("contact")) {
                final InstanceSetFilterValue filterValue = (InstanceSetFilterValue) filter.get("contact");
                if (filterValue != null) {
                    final Iterator<Instance> instanceIter = filterValue.getObject().iterator();
                    while (instanceIter.hasNext()) {
                        final Instance instance = instanceIter.next();
                        if (!instance.isValid()) {
                            instanceIter.remove();
                        }
                    }
                    if (!filterValue.getObject().isEmpty()) {
                        if (filterValue.isNegate()) {
                            _queryBldr.addWhereAttrNotEqValue(CIERP.DocumentAbstract.Contact,
                                        filterValue.getObject().toArray());
                        } else {
                            _queryBldr.addWhereAttrEqValue(CIERP.DocumentAbstract.Contact,
                                            filterValue.getObject().toArray());
                        }
                    }
                }
            }
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final CrosstabBuilder crosstab = DynamicReports.ctab.crosstab();

            final CrosstabColumnGroupBuilder<String> columnGroup = DynamicReports.ctab.columnGroup("partial",
                            String.class).setShowTotal(false);
            crosstab.addColumnGroup(columnGroup);


            final CrosstabMeasureBuilder<BigDecimal> amountMeasure = DynamicReports.ctab.measure(getFilteredReport()
                            .getDBProperty("totalAmount"), "amount", BigDecimal.class, Calculation.SUM);

            final CrosstabMeasureBuilder<BigDecimal> countMeasure = DynamicReports.ctab.measure(getFilteredReport()
                            .getDBProperty("totalCount"), "amount", BigDecimal.class, Calculation.COUNT);

            final CrosstabMeasureBuilder<BigDecimal> openAmountMeasure = DynamicReports.ctab.measure(getFilteredReport()
                            .getDBProperty("openAmount"), "openAmount", BigDecimal.class, Calculation.SUM);

            final CrosstabMeasureBuilder<BigDecimal> openAmountCountMeasure = DynamicReports.ctab.measure(
                            getFilteredReport().getDBProperty("openCount"), "openAmount",
                            BigDecimal.class, Calculation.COUNT);

            final CrosstabMeasureBuilder<BigDecimal> amPerMeasure = DynamicReports.ctab.measure(
                            getFilteredReport().getDBProperty("amountPercentage"),
                            new PercentageExpression(amountMeasure, columnGroup));

            final CrosstabMeasureBuilder<BigDecimal> countPerMeasure = DynamicReports.ctab.measure(
                            getFilteredReport().getDBProperty("countPercentage"),
                            new CountPercentageExpression(amountMeasure, columnGroup));

            crosstab.addMeasure(amountMeasure, openAmountMeasure, amPerMeasure,
                            countMeasure, openAmountCountMeasure, countPerMeasure);

            final CrosstabRowGroupBuilder<String> rowContactGroup = DynamicReports.ctab.rowGroup("contact",
                            String.class).setShowTotal(false);

            final CrosstabRowGroupBuilder<String> rowTypeGroup = DynamicReports.ctab.rowGroup("type", String.class)
                            .setHeaderWidth(150).setShowTotal(false);
            crosstab.addRowGroup(rowContactGroup, rowTypeGroup);

            _builder.addSummary(crosstab);
        }

        /**
         * Gets the filtered report.
         *
         * @return the filtered report
         */
        public DocBalanceReport_Base getFilteredReport()
        {
            return this.filteredReport;
        }

    }

    /**
     * The Class DataBean.
     *
     * @author The eFaps Team
     */
    public static class DataBean
    {

        /** The contact. */
        private String contact;

        /** The type. */
        private String type;

        /** The partial. */
        private String partial;

        /** The type. */
        private BigDecimal amount;

        /** The status. */
        private Status status;

        /**
         * Gets the open amount.
         *
         * @return the open amount
         */
        public BigDecimal getOpenAmount()
        {
            BigDecimal ret = null;
            if ("Open".equals(this.status.getKey()) || "Draft".equals(this.status.getKey())) {
                ret = getAmount();
            }
            return ret;
        }

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
         * Gets the type.
         *
         * @return the type
         */
        public BigDecimal getAmount()
        {
            return this.amount;
        }

        /**
         * Sets the amount.
         *
         * @param _amount the amount
         * @return the data bean
         */
        public DataBean setAmount(final BigDecimal _amount)
        {
            this.amount = _amount;
            return this;
        }

        /**
         * Gets the status.
         *
         * @return the status
         */
        public Status getStatus()
        {
            return this.status;
        }

        /**
         * Sets the status.
         *
         * @param _status the status
         * @return the data bean
         */
        public DataBean setStatus(final Status _status)
        {
            this.status = _status;
            return this;
        }

        /**
         * Gets the contact.
         *
         * @return the contact
         */
        public String getContact()
        {
            return this.contact;
        }

        /**
         * Sets the contact.
         *
         * @param _contact the contact
         * @return the data bean
         */
        public DataBean setContact(final String _contact)
        {
            this.contact = _contact;
            return this;
        }
    }

    /**
     * The Class PercentageExpression.
     */
    public static class PercentageExpression
        extends AbstractComplexExpression<BigDecimal>
    {

        /** The Constant serialVersionUID. */
        private static final long serialVersionUID = 1L;

        /**
         * Instantiates a new percentage expression.
         *
         * @param _measure the measure
         * @param _columnGroup the column group
         */
        private PercentageExpression(final CrosstabMeasureBuilder<BigDecimal> _measure,
                                     final CrosstabColumnGroupBuilder<String> _columnGroup)
        {
            addExpression(_measure);
            addExpression(DynamicReports.exp.crosstabValue(_measure, _columnGroup));
        }

        @Override
        public BigDecimal evaluate(final List<?> _values,
                                   final ReportParameters _reportParameters)
        {
            final BigDecimal val1 = (BigDecimal) _values.get(2);
            final BigDecimal val2 = (BigDecimal) _values.get(3);
            return new BigDecimal(100).divide(val1, 8, RoundingMode.HALF_UP).multiply(val2).setScale(2,
                            RoundingMode.HALF_UP);
        }
    }

    /**
     * The Class PercentageExpression.
     */
    public static class CountPercentageExpression
        extends PercentageExpression
    {

        /** The Constant serialVersionUID. */
        private static final long serialVersionUID = 1L;

        /**
         * Instantiates a new percentage expression.
         *
         * @param _measure the measure
         * @param _columnGroup the column group
         */
        private CountPercentageExpression(final CrosstabMeasureBuilder<BigDecimal> _measure,
                                          final CrosstabColumnGroupBuilder<String> _columnGroup)
        {
            super(_measure, _columnGroup);
        }

        @Override
        public BigDecimal evaluate(final List<?> _values,
                                   final ReportParameters _reportParameters)
        {
            final BigDecimal val1 = BigDecimal.valueOf((Long) _values.get(4));
            final BigDecimal val2 = BigDecimal.valueOf((Long) _values.get(5));
            return new BigDecimal(100).divide(val1, 8, RoundingMode.HALF_UP).multiply(val2).setScale(2,
                            RoundingMode.HALF_UP);
        }
    }
}
