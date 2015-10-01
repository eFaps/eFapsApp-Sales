/*
 * Copyright 2003 - 2015 The eFaps Team
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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport_Base;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeDate;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeMonth;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeYear;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.sales.payment.DocPaymentInfo;
import org.efaps.ui.wicket.models.EmbeddedLink;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.ComponentColumnBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.component.GenericElementBuilder;
import net.sf.dynamicreports.report.builder.expression.AbstractComplexExpression;
import net.sf.dynamicreports.report.builder.grid.ColumnGridComponentBuilder;
import net.sf.dynamicreports.report.builder.grid.ColumnTitleGroupBuilder;
import net.sf.dynamicreports.report.builder.group.ColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.subtotal.AggregationSubtotalBuilder;
import net.sf.dynamicreports.report.constant.HorizontalAlignment;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("d095f1fb-9286-4f93-a030-715873826dca")
@EFapsApplication("eFapsApp-Sales")
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


        public SalesReport4Account_Base getFilteredReport()
        {
            return this.filteredReport;
        }

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
            final List<DataBean> dataSource = new ArrayList<>();

            final Map<String, Object> filter = this.filteredReport.getFilterMap(_parameter);

            boolean offset = false;
            if (filter.containsKey("switch")) {
                offset = (boolean) filter.get("switch");
            }
            final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter, offset ? 0 : 100);
            add2QueryBuilder(_parameter, queryBldr);

            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CISales.DocumentSumAbstract.Created,
                            CISales.DocumentSumAbstract.Date,
                            CISales.DocumentSumAbstract.Name,
                            CISales.DocumentSumAbstract.DueDate,
                            CISales.DocumentSumAbstract.Rate,
                            CISales.DocumentSumAbstract.CrossTotal,
                            CISales.DocumentSumAbstract.CurrencyId,
                            CISales.DocumentSumAbstract.RateCurrencyId,
                            CISales.DocumentSumAbstract.RateCrossTotal,
                            CISales.DocumentSumAbstract.Revision);

            final SelectBuilder selContactInst = new SelectBuilder()
                            .linkto(CISales.DocumentSumAbstract.Contact).instance();
            final SelectBuilder selStatus = new SelectBuilder().status().label();
            final SelectBuilder selContactName = new SelectBuilder()
                            .linkto(CISales.DocumentSumAbstract.Contact).attribute(CIContacts.Contact.Name);

            multi.addSelect(selContactInst, selContactName, selStatus);
            multi.execute();
            while (multi.next()) {
                final DataBean dataBean = new DataBean()
                        .setDocInst(multi.getCurrentInstance())
                        .setDocCreated(multi.<DateTime>getAttribute(CISales.DocumentSumAbstract.Created))
                        .setDocDate(multi.<DateTime>getAttribute(CISales.DocumentSumAbstract.Date))
                        .setDocDueDate(multi.<DateTime>getAttribute(CISales.DocumentSumAbstract.DueDate))
                        .setDocName(multi.<String>getAttribute(CISales.DocumentSumAbstract.Name))
                        .setDocRevision(multi.<String>getAttribute(CISales.DocumentSumAbstract.Revision))
                        .setDocStatus(multi.<String>getSelect(selStatus))
                        .setRate(multi.<Object[]>getAttribute(CISales.DocumentSumAbstract.Rate))
                        .addCross(multi.<Long>getAttribute(CISales.DocumentSumAbstract.CurrencyId),
                                    multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.CrossTotal))
                        .addCross(multi.<Long>getAttribute(CISales.DocumentSumAbstract.RateCurrencyId),
                                    multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal));
                dataSource.add(dataBean);
            }

            final ComparatorChain<DataBean> chain = new ComparatorChain<DataBean>();
            chain.addComparator(new Comparator<DataBean>()
            {

                @Override
                public int compare(final DataBean _o1,
                                   final DataBean _o2)
                {
                    return Report4Account.this.filteredReport.getReportKey().equals(ReportKey.OUT)
                                    ? _o1.getDocCreated().compareTo(_o2.getDocCreated())
                                    : _o1.getDocDate().compareTo(_o2.getDocDate());
                }
            }
                            );
            chain.addComparator(new Comparator<DataBean>()
            {

                @Override
                public int compare(final DataBean _o1,
                                   final DataBean _o2)
                {
                    return _o1.getDocContactName().compareTo(_o2.getDocContactName());
                }
            }
                            );
            Collections.sort(dataSource, chain);
            final Collection<Map<String, ?>> col = new ArrayList<>();
            for (final DataBean bean : dataSource) {
                col.add(bean.getMap());
            }

            return new JRMapCollectionDataSource(col);

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
                final Instance contact = ((InstanceFilterValue) filter.get("contact")).getObject();
                if (contact.isValid()) {
                    _queryBldr.addWhereAttrEqValue(CISales.DocumentSumAbstract.Contact, contact);
                }
            }
            if (filter.containsKey("currency")) {
                final Instance currency = ((CurrencyFilterValue) filter.get("currency")).getObject();
                if (currency.isValid()) {
                    _queryBldr.addWhereAttrEqValue(CISales.DocumentSumAbstract.RateCurrencyId, currency);
                }
            }

            if (filter.containsKey("type")) {
                final TypeFilterValue filters = (TypeFilterValue) filter.get("type");
                final Set<Long> typeids = filters.getObject();
                if (!typeids.isEmpty()) {
                    _queryBldr.addWhereAttrEqValue(CISales.DocumentSumAbstract.Type, typeids.toArray());
                }
            }

            _queryBldr.addWhereAttrGreaterValue(CISales.DocumentSumAbstract.Date, dateFrom);
            _queryBldr.addWhereAttrLessValue(CISales.DocumentSumAbstract.Date, dateTo.plusDays(1)
                            .withTimeAtStartOfDay());

            final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Document2Document4Swap);
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

            final String filter = Report4Account.this.filteredReport.getReportKey().equals(ReportKey.OUT)
                            ? "docCreated"
                            : "docDate";

            final TextColumnBuilder<DateTime> monthColumn = AbstractDynamicReport_Base.column(
                            this.filteredReport.getLabel(_parameter, "FilterDate1"), filter,
                            DateTimeMonth.get());
            final TextColumnBuilder<DateTime> yearColumn = AbstractDynamicReport_Base.column(
                            this.filteredReport.getLabel(_parameter, "FilterDate2"), filter,
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
            final TextColumnBuilder<String> revisionColumn = DynamicReports.col.column(
                            this.filteredReport.getLabel(_parameter, "Revision"), "docRevision",
                            DynamicReports.type.stringType());

            final TextColumnBuilder<String> contactNameColumn = DynamicReports.col.column(
                            this.filteredReport.getLabel(_parameter, "ContactName"), "docContactName",
                            DynamicReports.type.stringType());

            final TextColumnBuilder<DateTime> dueDateColumn = AbstractDynamicReport_Base.column(
                            this.filteredReport.getLabel(_parameter, "DueDate"), "docDueDate",
                            DateTimeDate.get());

            final TextColumnBuilder<String> docStatusColumn = DynamicReports.col.column(
                            this.filteredReport.getLabel(_parameter, "Status"), "docStatus",
                            DynamicReports.type.stringType());

            final ColumnGroupBuilder yearGroup = DynamicReports.grp.group(yearColumn).groupByDataType();
            final ColumnGroupBuilder monthGroup = DynamicReports.grp.group(monthColumn).groupByDataType();

            final GenericElementBuilder linkElement = DynamicReports.cmp.genericElement(
                            "http://www.efaps.org", "efapslink")
                            .addParameter(EmbeddedLink.JASPER_PARAMETERKEY, new LinkExpression())
                            .setHeight(12).setWidth(25);

            final ComponentColumnBuilder linkColumn = DynamicReports.col.componentColumn(linkElement).setTitle("");

            _builder.addColumn(yearColumn, monthColumn);

            final List<ColumnGridComponentBuilder> gridList = new ArrayList<>();
            if (getExType().equals(ExportType.HTML)) {
                _builder.addColumn(linkColumn);
                gridList.add(linkColumn);
            }

            gridList.add(typeColumn);
            gridList.add(createdColumn);
            gridList.add(dateColumn);
            gridList.add(nameColumn);
            if (Report4Account.this.filteredReport.getReportKey().equals(ReportKey.OUT)) {
                gridList.add(revisionColumn);
            }
            gridList.add(contactNameColumn);
            gridList.add(dueDateColumn);
            gridList.add(docStatusColumn);

            for (final CurrencyInst currency : getCurrencyInst4Report(_parameter)) {
                final TextColumnBuilder<BigDecimal> crossColumn = DynamicReports.col.column(
                                this.filteredReport.getLabel(_parameter, "crossTotal"),
                                "crossTotal_" + currency.getISOCode(), DynamicReports.type.bigDecimalType());
                final TextColumnBuilder<BigDecimal> payColumn = DynamicReports.col.column(
                                this.filteredReport.getLabel(_parameter, "payed"),
                                "payed_" + currency.getISOCode(), DynamicReports.type.bigDecimalType());
                final TextColumnBuilder<BigDecimal> result = crossColumn.subtract(payColumn);
                result.setTitle(this.filteredReport.getLabel(_parameter, "result"));

                final ColumnTitleGroupBuilder titelGroup = DynamicReports.grid.titleGroup(currency.getName(),
                                crossColumn, payColumn, result);
                gridList.add(titelGroup);
                _builder.addColumn(crossColumn, payColumn, result);
                if (!currency.getInstance().equals(Currency.getBaseCurrency())) {
                    final TextColumnBuilder<BigDecimal> rateColumn = DynamicReports.col.column(
                                    this.filteredReport.getLabel(_parameter, "rate"),
                                    "rate_" + currency.getISOCode(), DynamicReports.type.bigDecimalType());
                    _builder.addColumn(rateColumn);
                    titelGroup.add(rateColumn);
                }

                final AggregationSubtotalBuilder<BigDecimal> crossTotal = DynamicReports.sbt.sum(crossColumn);
                final AggregationSubtotalBuilder<BigDecimal> payTotal = DynamicReports.sbt.sum(payColumn);
                final AggregationSubtotalBuilder<BigDecimal> resultTotal = DynamicReports.sbt.sum(result);
                final AggregationSubtotalBuilder<BigDecimal> crossTotal2 = DynamicReports.sbt.sum(crossColumn);
                final AggregationSubtotalBuilder<BigDecimal> payTotal2 = DynamicReports.sbt.sum(payColumn);
                final AggregationSubtotalBuilder<BigDecimal> resultTotal2 = DynamicReports.sbt.sum(result);
                _builder.addSubtotalAtGroupFooter(monthGroup, crossTotal);
                _builder.addSubtotalAtGroupFooter(monthGroup, payTotal);
                _builder.addSubtotalAtGroupFooter(monthGroup, resultTotal);
                _builder.addSubtotalAtGroupFooter(yearGroup, crossTotal2);
                _builder.addSubtotalAtGroupFooter(yearGroup, payTotal2);
                _builder.addSubtotalAtGroupFooter(yearGroup, resultTotal2);
            }

            _builder.columnGrid(gridList.toArray(new ColumnGridComponentBuilder[gridList.size()]));

            _builder.addColumn(typeColumn.setFixedWidth(120),
                            createdColumn,
                            dateColumn,
                            nameColumn.setHorizontalAlignment(HorizontalAlignment.CENTER).setFixedWidth(140),
                            revisionColumn,
                            contactNameColumn.setFixedWidth(200),
                            dueDateColumn,
                            docStatusColumn);
            if (!showDetails) {
                _builder.setShowColumnValues(false);
            }
            _builder.groupBy(yearGroup, monthGroup);
        }

        /**
         * @return
         */
        public Set<CurrencyInst> getCurrencyInst4Report(final Parameter _parameter)
            throws EFapsException
        {
            Set<CurrencyInst> ret = new HashSet<>();
            final Map<String, Object> filter = this.filteredReport.getFilterMap(_parameter);
            if (filter.containsKey("currency")) {
                final Instance currency = ((CurrencyFilterValue) filter.get("currency")).getObject();
                if (currency.isValid()) {
                    ret.add(new CurrencyInst(currency));
                } else {
                    ret = CurrencyInst.getAvailable();
                }
            } else {
                ret = CurrencyInst.getAvailable();
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

    public static class DataBean
    {

        private Instance docInst;

        private DateTime docCreated;

        private DateTime docDate;

        private DateTime docDueDate;

        private String docName;

        private String docRevision;

        private String docStatus;

        private String docContactName;

        private final Map<Long, BigDecimal> cross = new HashMap<>();

        private final Map<Long, BigDecimal> payments = new HashMap<>();

        private Object[] rate;

        /**
         * @return
         */
        public Map<String, ?> getMap()
            throws EFapsException
        {
            if (this.payments.isEmpty()) {
                evalPayments();
            }
            final Map<String, Object> ret = new HashMap<>();
            ret.put("docOID", getDocInst().getOid());
            ret.put("docType", getDocInst().getType().getLabel());
            ret.put("docCreated", getDocCreated());
            ret.put("docDate", getDocDate());
            ret.put("docName", getDocName());
            ret.put("docRevision", getDocRevision());
            ret.put("docStatus", getDocStatus());
            ret.put("docContactName", getDocContactName());
            ret.put("docDueDate", getDocDueDate());

            for (final CurrencyInst currency : CurrencyInst.getAvailable()) {
                if (this.cross.containsKey(currency.getInstance().getId())) {
                    ret.put("crossTotal_" + currency.getISOCode(), this.cross.get(currency.getInstance().getId()));
                    ret.put("payed_" + currency.getISOCode(), this.payments.get(currency.getInstance().getId()));
                    if (!currency.getInstance().equals(Currency.getBaseCurrency())) {
                        ret.put("rate_" + currency.getISOCode(), currency.isInvert() ? getRate()[1] : getRate()[0]);
                    }
                }
            }
            return ret;
        }

        public DataBean addCross(final Long currencyId,
                                 final BigDecimal _cross)
        {
            this.cross.put(currencyId, _cross);
            return this;
        }

        /**
         * Getter method for the instance variable {@link #docInst}.
         *
         * @return value of instance variable {@link #docInst}
         */
        public Instance getDocInst()
        {
            return this.docInst;
        }

        /**
         * Setter method for instance variable {@link #docInst}.
         *
         * @param _docInst value for instance variable {@link #docInst}
         */
        public DataBean setDocInst(final Instance _docInst)
        {
            this.docInst = _docInst;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #docCreated}.
         *
         * @return value of instance variable {@link #docCreated}
         */
        public DateTime getDocCreated()
        {
            return this.docCreated;
        }

        /**
         * Setter method for instance variable {@link #docCreated}.
         *
         * @param _docCreated value for instance variable {@link #docCreated}
         */
        public DataBean setDocCreated(final DateTime _docCreated)
        {
            this.docCreated = _docCreated;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #docDate}.
         *
         * @return value of instance variable {@link #docDate}
         */
        public DateTime getDocDate()
        {
            return this.docDate;
        }

        /**
         * Setter method for instance variable {@link #docDate}.
         *
         * @param _docDate value for instance variable {@link #docDate}
         */
        public DataBean setDocDate(final DateTime _docDate)
        {
            this.docDate = _docDate;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #docDueDate}.
         *
         * @return value of instance variable {@link #docDueDate}
         */
        public DateTime getDocDueDate()
        {
            return this.docDueDate;
        }

        /**
         * Setter method for instance variable {@link #docDueDate}.
         *
         * @param _docDueDate value for instance variable {@link #docDueDate}
         */
        public DataBean setDocDueDate(final DateTime _docDueDate)
        {
            this.docDueDate = _docDueDate;
            return this;
        }

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
         */
        public DataBean setDocName(final String _docName)
        {
            this.docName = _docName;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #docContactName}.
         *
         * @return value of instance variable {@link #docContactName}
         */
        public String getDocContactName()
        {
            return this.docContactName == null ? "" : this.docContactName;
        }

        /**
         * Setter method for instance variable {@link #docContactName}.
         *
         * @param _docContactName value for instance variable
         *            {@link #docContactName}
         */
        public DataBean setDocContactName(final String _docContactName)
        {
            this.docContactName = _docContactName;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #rate}.
         *
         * @return value of instance variable {@link #rate}
         */
        public Object[] getRate()
        {
            return this.rate;
        }

        /**
         * Setter method for instance variable {@link #rate}.
         *
         * @param _rate value for instance variable {@link #rate}
         */
        public DataBean setRate(final Object[] _rate)
        {
            this.rate = _rate;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #docRevision}.
         *
         * @return value of instance variable {@link #docRevision}
         */
        public String getDocRevision()
        {
            return this.docRevision;
        }

        /**
         * Setter method for instance variable {@link #docRevision}.
         *
         * @param _docRevision value for instance variable {@link #docRevision}
         */
        public DataBean setDocRevision(final String _docRevision)
        {
            this.docRevision = _docRevision;
            return this;
        }

        /**
         * Method to obtains totals of the payments relations to document.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _docInst Instance of the payment
         * @return value of the payments
         * @throws EFapsException on error
         */
        protected void evalPayments()
            throws EFapsException
        {
            if (getDocInst().isValid()) {
                final DocPaymentInfo docPayInfo = new DocPaymentInfo(getDocInst());
                setDocContactName(docPayInfo.getContactName());
                setDocName(docPayInfo.getName());
                this.payments.put(Currency.getBaseCurrency().getId(), docPayInfo.getPaid());
                this.payments.put(docPayInfo.getRateCurrencyInstance().getId(), docPayInfo.getRatePaid());
            }
        }

        /**
         * Getter method for the instance variable {@link #docStatus}.
         *
         * @return value of instance variable {@link #docStatus}
         */
        public String getDocStatus()
        {
            return this.docStatus;
        }

        /**
         * Setter method for instance variable {@link #docStatus}.
         *
         * @param _docStatus value for instance variable {@link #docStatus}
         */
        public DataBean setDocStatus(final String _docStatus)
        {
            this.docStatus = _docStatus;
            return this;
        }
    }
}
