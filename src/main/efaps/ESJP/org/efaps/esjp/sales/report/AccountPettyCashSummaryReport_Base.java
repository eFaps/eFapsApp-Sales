/*
 * Copyright 2003 - 2014 The eFaps Team
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
import java.util.List;
import java.util.Map;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabMeasureBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabRowGroupBuilder;
import net.sf.dynamicreports.report.constant.Calculation;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("8d44506c-09aa-42c7-aa66-b6c2216cb075")
@EFapsRevision("$Rev$")
public abstract class AccountPettyCashSummaryReport_Base
    extends FilteredReport
{

    /**
     * Logging instance used in this class.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(AccountPettyCashSummaryReport.class);

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
        final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String mime = (String) props.get("Mime");
        final AbstractDynamicReport dyRp = getReport(_parameter);
        dyRp.setFileName(DBProperties.getProperty(AccountPettyCashSummaryReport.class.getName() + ".FileName"));
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
        return new DynAccountPettyCashSummaryReport(this);
    }

    public static class DynAccountPettyCashSummaryReport
        extends AbstractDynamicReport
    {

        private final AccountPettyCashSummaryReport_Base filteredReport;

        /**
         * @param _accountPettyCashSummaryReport_Base
         */
        public DynAccountPettyCashSummaryReport(final AccountPettyCashSummaryReport_Base _filteredReport)
        {
            this.filteredReport = _filteredReport;
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final SelectBuilder selAcc = new SelectBuilder()
                            .linkfrom(CISales.Account2DocumentWithTrans.ToLinkAbstract)
                            .linkto(CISales.Account2DocumentWithTrans.FromLinkAbstract);
            final SelectBuilder selAccInst = new SelectBuilder(selAcc).instance();
            final SelectBuilder selAccName = new SelectBuilder(selAcc).attribute(CISales.AccountPettyCash.Name);
            final SelectBuilder selDocTypeName = new SelectBuilder()
                            .linkfrom(CISales.Document2DocumentType.DocumentLink)
                            .linkto(CISales.Document2DocumentType.DocumentTypeLink).attribute(CIERP.DocumentType.Name);

            final List<DataBean> datasource = new ArrayList<DataBean>();
            final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter);
            add2QueryBldr(_parameter, queryBldr);
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addSelect(selAccInst, selDocTypeName, selAccName);
            multi.addAttribute(CISales.DocumentSumAbstract.NetTotal, CISales.DocumentSumAbstract.CrossTotal);
            multi.execute();
            while (multi.next()) {
                BigDecimal cross = multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.CrossTotal);
                BigDecimal net = multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.NetTotal);

                if (multi.getCurrentInstance().getType().isKindOf(CISales.IncomingCreditNote.getType())) {
                    cross = cross.negate();
                    net = net.negate();
                }
                final DataBean bean = getBean(_parameter);
                datasource.add(bean);
                bean.setPettyCashInstance(multi.<Instance>getSelect(selAccInst)).setCross(cross).setNet(net)
                                .setDocTypeName(multi.<String>getSelect(selDocTypeName))
                                .setPettyCash(multi.<String>getSelect(selAccName));
            }
            return new JRBeanCollectionDataSource(datasource);
        }

        /**
         * @param _parameter Parameter as passed by the eFaps API
         * @param _queryBldr QueryBuilder to add to
         * @throws EFapsException on error
         */
        protected void add2QueryBldr(final Parameter _parameter,
                                     final QueryBuilder _queryBldr)
            throws EFapsException
        {
            final Map<String, Object> filter = this.filteredReport.getFilterMap(_parameter);
            if (filter.containsKey("dateFrom")) {
                final DateTime date = (DateTime) filter.get("dateFrom");
                _queryBldr.addWhereAttrGreaterValue(CIERP.DocumentAbstract.Date,
                                date.withTimeAtStartOfDay().minusSeconds(1));
            }
            if (filter.containsKey("dateTo")) {
                final DateTime date = (DateTime) filter.get("dateTo");
                _queryBldr.addWhereAttrLessValue(CIERP.DocumentAbstract.Date,
                                date.withTimeAtStartOfDay().plusDays(1));
            }
            final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Account2DocumentWithTrans);
            attrQueryBldr.addWhereAttrNotEqValue(CISales.Account2DocumentWithTrans.Type,
                            CISales.AccountFundsToBeSettled2FundsToBeSettledReceipt.getType().getId(),
                            CISales.AccountFundsToBeSettled2IncomingCreditNote.getType().getId());
            final AttributeQuery attrQuery = attrQueryBldr
                            .getAttributeQuery(CISales.Account2DocumentWithTrans.ToLinkAbstract);
            _queryBldr.addWhereAttrInQuery(CISales.DocumentSumAbstract.ID, attrQuery);
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final CrosstabBuilder crosstab = DynamicReports.ctab.crosstab();

            final CrosstabMeasureBuilder<BigDecimal> netMeasure = DynamicReports.ctab.measure(
                            DBProperties.getProperty(AccountPettyCashSummaryReport.class.getName() + ".net"),
                            "net", BigDecimal.class, Calculation.SUM);
            crosstab.addMeasure(netMeasure);

            final CrosstabMeasureBuilder<BigDecimal> crossMeasure = DynamicReports.ctab.measure(
                            DBProperties.getProperty(AccountPettyCashSummaryReport.class.getName() + ".cross"),
                            "cross", BigDecimal.class, Calculation.SUM);
            crosstab.addMeasure(crossMeasure);

            final CrosstabRowGroupBuilder<String> rowTypeGroup = DynamicReports.ctab
                            .rowGroup("pettyCash", String.class)
                            .setHeaderWidth(250);
            crosstab.addRowGroup(rowTypeGroup);

            final CrosstabColumnGroupBuilder<String> columnGroup = DynamicReports.ctab.columnGroup("docType",
                            String.class);

            crosstab.addColumnGroup(columnGroup);

            _builder.addSummary(crosstab);
        }

        /**
         * Getter method for the instance variable {@link #filteredReport}.
         *
         * @return value of instance variable {@link #filteredReport}
         */
        public AccountPettyCashSummaryReport_Base getFilteredReport()
        {
            return this.filteredReport;
        }

        /**
         * @param _parameter
         * @return
         */
        public DataBean getBean(final Parameter _parameter)
        {
            return new DataBean();
        }
    }

    public static class DataBean
    {

        private Instance pettyCashInstance;
        private BigDecimal net;
        private BigDecimal cross;

        private String docTypeName;

        private String pettyCash;

        public String getDocType()
        {
            String ret;
            if (this.docTypeName == null) {
                ret = DBProperties.getProperty(AccountPettyCashReport.class.getName() + ".NotWithDocument");
            } else {
                ret = DBProperties.getProperty(AccountPettyCashReport.class.getName() + ".WithDocument");
            }
            return ret;
        }

        /**
         * Getter method for the instance variable {@link #pettyCashInstance}.
         *
         * @return value of instance variable {@link #pettyCashInstance}
         */
        public Instance getPettyCashInstance()
        {
            return this.pettyCashInstance;
        }

        /**
         * Setter method for instance variable {@link #pettyCashInstance}.
         *
         * @param _pettyCashInstance value for instance variable
         *            {@link #pettyCashInstance}
         */
        public DataBean setPettyCashInstance(final Instance _pettyCashInstance)
        {
            this.pettyCashInstance = _pettyCashInstance;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #net}.
         *
         * @return value of instance variable {@link #net}
         */
        public BigDecimal getNet()
        {
            return this.net;
        }

        /**
         * Setter method for instance variable {@link #net}.
         *
         * @param _net value for instance variable {@link #net}
         */
        public DataBean setNet(final BigDecimal _net)
        {
            this.net = _net;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #cross}.
         *
         * @return value of instance variable {@link #cross}
         */
        public BigDecimal getCross()
        {
            return this.cross;
        }

        /**
         * Setter method for instance variable {@link #cross}.
         *
         * @param _cross value for instance variable {@link #cross}
         */
        public DataBean setCross(final BigDecimal _cross)
        {
            this.cross = _cross;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #docTypeName}.
         *
         * @return value of instance variable {@link #docTypeName}
         */
        public String getDocTypeName()
        {
            return this.docTypeName;
        }

        /**
         * Setter method for instance variable {@link #docTypeName}.
         *
         * @param _docTypeName value for instance variable {@link #docTypeName}
         */
        public DataBean setDocTypeName(final String _docTypeName)
        {
            this.docTypeName = _docTypeName;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #pettyCash}.
         *
         * @return value of instance variable {@link #pettyCash}
         */
        public String getPettyCash()
        {
            return this.pettyCash;
        }

        /**
         * Setter method for instance variable {@link #pettyCash}.
         *
         * @param _pettyCash value for instance variable {@link #pettyCash}
         */
        public DataBean setPettyCash(final String _pettyCash)
        {
            this.pettyCash = _pettyCash;
            return this;
        }

    }

}
