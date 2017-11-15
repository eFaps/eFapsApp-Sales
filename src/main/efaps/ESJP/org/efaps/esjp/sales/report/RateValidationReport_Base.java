/*
 * Copyright 2003 - 2017 The eFaps Team
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections.comparators.ComparatorChain;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeDate;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.erp.RateInfo;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.ComponentColumnBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("e1f4dd70-40e0-41b5-b2da-52a31fbd3e28")
@EFapsApplication("eFapsApp-Sales")
public abstract class RateValidationReport_Base
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

    @Override
    protected Properties getProperties4TypeList(final Parameter _parameter,
                                                final String _fieldName)
        throws EFapsException
    {
        return Sales.REPORT_RATEVALID.get();
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return the report class
     * @throws EFapsException on error
     */
    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new DynRateValidationReport(this);
    }

    /**
     * The Class DynDocBalanceReport.
     */
    public static class DynRateValidationReport
        extends AbstractDynamicReport
    {

        /** The filtered report. */
        private RateValidationReport_Base filteredReport;

        /**
         * Instantiates a new dyn rate validation report.
         *
         * @param _rateValidationReport the rate validation report
         */
        public DynRateValidationReport(final RateValidationReport_Base _rateValidationReport)
        {
            this.filteredReport = _rateValidationReport;
        }

        @SuppressWarnings("unchecked")
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
                final List<DataBean> values = new ArrayList<>();

                final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter, Sales.REPORT_RATEVALID.get());
                add2QueryBldr(_parameter, queryBldr);
                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder selRateCurInst = SelectBuilder.get().linkto(
                                CISales.DocumentSumAbstract.RateCurrencyId).instance();
                multi.addSelect(selRateCurInst);
                multi.addAttribute(CISales.DocumentSumAbstract.Date, CISales.DocumentSumAbstract.Rate,
                                CISales.DocumentSumAbstract.Name);
                multi.execute();

                while (multi.next()) {
                    final Instance baseCurrInst = Currency.getBaseCurrency();
                    final Instance rateCurrInst = multi.<Instance>getSelect(selRateCurInst);
                    if (!baseCurrInst.equals(rateCurrInst)) {
                        final Instance docInst = multi.getCurrentInstance();
                        final DateTime date = multi.getAttribute(CISales.DocumentSumAbstract.Date);
                        final Object[] currentRateObj = multi.getAttribute(CISales.DocumentSumAbstract.Rate);
                        final Currency currency = new Currency();
                        final RateInfo rateInfo = currency.evaluateRateInfo(_parameter, date, rateCurrInst);
                        final Object[] rateObj = RateInfo.getRateObject(_parameter, rateInfo,
                                        docInst.getType().getName());
                        if (((BigDecimal) currentRateObj[0]).compareTo((BigDecimal) rateObj[0]) != 0
                                        || ((BigDecimal) currentRateObj[1]).compareTo((BigDecimal) rateObj[1]) != 0) {
                            final RateInfo currentRateInfo = RateInfo.getRateInfo(currentRateObj);

                            final DataBean dataBean = getDataBean(_parameter)
                                            .setDocOID(docInst.getOid())
                                            .setDocType(docInst.getType().getLabel())
                                            .setDocName(multi.getAttribute(CISales.DocumentSumAbstract.Name))
                                            .setDocDate(date)
                                            .setCurrentRate(RateInfo.getRateUI(_parameter, currentRateInfo,
                                                            docInst.getType().getName()))
                                            .setRegisteredRate(RateInfo.getRateUI(_parameter, rateInfo,
                                                            docInst.getType().getName()));
                            values.add(dataBean);
                        }
                    }
                }
                final ComparatorChain chain = new ComparatorChain();
                chain.addComparator(new Comparator<DataBean>()
                {

                    @Override
                    public int compare(final DataBean _o1,
                                       final DataBean _o2)
                    {

                        return _o1.getDocDate().compareTo(_o2.getDocDate());
                    }
                });
                chain.addComparator(new Comparator<DataBean>()
                {

                    @Override
                    public int compare(final DataBean _o1,
                                       final DataBean _o2)
                    {

                        return _o1.getDocName().compareTo(_o2.getDocName());
                    }
                });
                Collections.sort(values, chain);
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

            if (filter.containsKey("type")) {
                _queryBldr.addWhereAttrEqValue(CIERP.DocumentAbstract.Type, ((TypeFilterValue) filter.get("type"))
                                .getObject().toArray());
            }
        }

        @Override
        protected void addColumnDefinition(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final TextColumnBuilder<String> docTypeColumn = DynamicReports.col.column(getFilteredReport()
                           .getDBProperty("Column.docType"), "docType",
                           DynamicReports.type.stringType());
            final TextColumnBuilder<String> docNameColumn = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty("Column.docName"), "docName",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<DateTime> docDateColumn = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty("Column.docDate"), "docDate",
                            DateTimeDate.get());
            final TextColumnBuilder<BigDecimal> currentRateColumn = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty("Column.currentRate"), "currentRate",
                            DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> registeredRateColumn = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty("Column.registeredRate"), "registeredRate",
                            DynamicReports.type.bigDecimalType());

            if (ExportType.HTML.equals(getExType())) {
                final ComponentColumnBuilder linkColumn = FilteredReport.getLinkColumn(_parameter, "docOID");
                _builder.addColumn(linkColumn);
            }

            _builder.addColumn(docTypeColumn, docNameColumn, docDateColumn, currentRateColumn, registeredRateColumn);
        }

        /**
         * Getter method for the instance variable {@link #filteredReport}.
         *
         * @return value of instance variable {@link #filteredReport}
         */
        protected RateValidationReport_Base getFilteredReport()
        {
            return this.filteredReport;
        }

        /**
         * Setter method for instance variable {@link #filteredReport}.
         *
         * @param _filteredReport value for instance variable
         *            {@link #filteredReport}
         */
        protected void setFilteredReport(final RateValidationReport_Base _filteredReport)
        {
            this.filteredReport = _filteredReport;
        }

        /**
         * Gets the data bean.
         *
         * @param _parameter the parameter
         * @return the data bean
         */
        protected DataBean getDataBean(final Parameter _parameter)
        {
            return new DataBean();
        }
    }

    /**
     * The Class DataBean.
     */
    public static class DataBean
    {

        /** The doc OID. */
        private String docOID;

        /** The doc name. */
        private String docName;

        /** The doc type. */
        private String docType;

        /** The docdate. */
        private DateTime docdate;

        /** The current rate. */
        private BigDecimal currentRate;

        /** The registered rate. */
        private BigDecimal registeredRate;

        /**
         * Getter method for the instance variable {@link #docName}.
         *
         * @return value of instance variable {@link #docName}
         */
        public String getDocName()
        {
            return this.docName;
        }

        /**
         * Setter method for instance variable {@link #docName}.
         *
         * @param _docName value for instance variable {@link #docName}
         * @return the data bean
         */
        public DataBean setDocName(final String _docName)
        {
            this.docName = _docName;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #currentRate}.
         *
         * @return value of instance variable {@link #currentRate}
         */
        public BigDecimal getCurrentRate()
        {
            return this.currentRate;
        }

        /**
         * Setter method for instance variable {@link #currentRate}.
         *
         * @param _currentRate value for instance variable {@link #currentRate}
         * @return the data bean
         */
        public DataBean setCurrentRate(final BigDecimal _currentRate)
        {
            this.currentRate = _currentRate;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #registeredRate}.
         *
         * @return value of instance variable {@link #registeredRate}
         */
        public BigDecimal getRegisteredRate()
        {
            return this.registeredRate;
        }

        /**
         * Setter method for instance variable {@link #registeredRate}.
         *
         * @param _registeredRate value for instance variable
         *            {@link #registeredRate}
         * @return the data bean
         */
        public DataBean setRegisteredRate(final BigDecimal _registeredRate)
        {
            this.registeredRate = _registeredRate;
            return this;
        }


        /**
         * Getter method for the instance variable {@link #docType}.
         *
         * @return value of instance variable {@link #docType}
         */
        public String getDocType()
        {
            return this.docType;
        }


        /**
         * Setter method for instance variable {@link #docType}.
         *
         * @param _docType value for instance variable {@link #docType}
         * @return the data bean
         */
        public DataBean setDocType(final String _docType)
        {
            this.docType = _docType;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #docdate}.
         *
         * @return value of instance variable {@link #docdate}
         */
        public DateTime getDocDate()
        {
            return this.docdate;
        }

        /**
         * Setter method for instance variable {@link #docdate}.
         *
         * @param _docdate value for instance variable {@link #docdate}
         * @return the data bean
         */
        public DataBean setDocDate(final DateTime _docdate)
        {
            this.docdate = _docdate;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #docOID}.
         *
         * @return value of instance variable {@link #docOID}
         */
        public String getDocOID()
        {
            return this.docOID;
        }

        /**
         * Setter method for instance variable {@link #docOID}.
         *
         * @param _docOID value for instance variable {@link #docOID}
         * @return the data bean
         */
        public DataBean setDocOID(final String _docOID)
        {
            this.docOID = _docOID;
            return this;
        }
    }
}
