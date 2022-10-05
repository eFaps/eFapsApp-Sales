/*
 * Copyright 2003 - 2022 The eFaps Team
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.SetUtils;
import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
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
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeDate;
import org.efaps.esjp.common.properties.PropertiesUtil;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.erp.RateInfo;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.ColumnBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.grid.ColumnGridComponentBuilder;
import net.sf.dynamicreports.report.builder.grid.ColumnTitleGroupBuilder;
import net.sf.dynamicreports.report.builder.group.ColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.subtotal.AggregationSubtotalBuilder;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * The Class PaymentReport_Base.
 *
 * @author The eFaps Team
 */
@EFapsUUID("57b58c92-507b-4293-b5ca-9b7c253fef81")
@EFapsApplication("eFapsApp-Sales")
public abstract class PaymentReport_Base
    extends FilteredReport
{

    /**
     * Used to distinguish between incoming and outgoing report.
     */
    public enum PayDoc
    {
        /** Both. */
        BOTH,
        /** Incoming. */
        IN,
        /** Outgoing. */
        OUT,
    }

    /**
     * The Enum Grouping.
     */
    public enum Grouping
    {

        /** The contact. */
        CONTACT,

        /** The type. */
        TYPE;
    }

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PaymentReport.class);

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
        dyRp.setFileName(getDBProperty("FileName"));
        final String html = dyRp.getHtmlSnipplet(_parameter);
        ret.put(ReturnValues.SNIPLETT, html);
        return ret;
    }

    /**
     * Export report.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return exportReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String mime = getProperty(_parameter, "Mime", "pdf");
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
    protected List<Type> getTypeList(final Parameter _parameter, final String _fieldName)
        throws EFapsException
    {
        List<Type> ret = super.getTypeList(_parameter, _fieldName);
        if (ret.size() < 2) {
            ret = CISales.PaymentDocumentIOAbstract.getType().getChildTypes().stream()
                            .filter(type -> !type.isAbstract())
                            .collect(Collectors.toList());
        }
        return ret;
    }

    @Override
    protected Properties getProperties4TypeList(final Parameter _parameter, final String _fieldName)
        throws EFapsException
    {
        return PropertiesUtil.getProperties4Prefix(Sales.REPORT_PAYMENT.get(), PayDoc.BOTH.name(), true);
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
        return new DynPaymentReport(this);
    }

    /**
     * The Class DynPaymentReport.
     */
    public static class DynPaymentReport
        extends AbstractDynamicReport
    {

        /** The filtered report. */
        private final PaymentReport_Base filteredReport;

        /**
         * Instantiates a new dyn payment report.
         *
         * @param _paymentReport the payment report
         */
        public DynPaymentReport(final PaymentReport_Base _paymentReport)
        {
            filteredReport = _paymentReport;
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
                    PaymentReport_Base.LOG.error("JRException", e);
                    throw new EFapsException("JRException", e);
                }
            } else {
                final PayDoc payDoc = getPayDoc(_parameter);
                final List<DataBean> beans = new ArrayList<>();

                final QueryBuilder queryBldr = new QueryBuilder(CISales.Payment);
                add2QueryBuilder(_parameter, queryBldr);

                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder selCurrInst = SelectBuilder.get().linkto(CISales.Payment.CurrencyLink).instance();
                final SelectBuilder selTargetDoc = SelectBuilder.get().linkto(CISales.Payment.TargetDocument);
                final SelectBuilder selTargetDocInst = new SelectBuilder(selTargetDoc).instance();
                final SelectBuilder selTargetDocName = new SelectBuilder(selTargetDoc).attribute(
                                CISales.PaymentDocumentIOAbstract.Name);
                final SelectBuilder selTargetDocCode = new SelectBuilder(selTargetDoc).attribute(
                                CISales.PaymentDocumentIOAbstract.Code);
                final SelectBuilder selTargetDocDate = new SelectBuilder(selTargetDoc).attribute(
                                CISales.PaymentDocumentIOAbstract.Date);
                final SelectBuilder selCreateDoc = SelectBuilder.get().linkto(CISales.Payment.CreateDocument);
                final SelectBuilder selCreateDocInst = new SelectBuilder(selCreateDoc).instance();
                final SelectBuilder selCreateDocName = new SelectBuilder(selCreateDoc).attribute(
                                CISales.DocumentSumAbstract.Name);
                final SelectBuilder selCreateDocRev = new SelectBuilder(selCreateDoc).attribute(
                                CISales.DocumentSumAbstract.Revision);
                final SelectBuilder selCreateDocContactName = new SelectBuilder(selCreateDoc)
                                .linkto(CISales.DocumentSumAbstract.Contact)
                                .attribute(CIContacts.Contact.Name);

                final SelectBuilder selAccount = SelectBuilder.get()
                                .linkfrom(CISales.TransactionAbstract.Payment)
                                .linkto(CISales.TransactionAbstract.Account)
                                .attribute(CISales.AccountAbstract.Name);
                multi.addSelect(selCurrInst, selTargetDocInst, selTargetDocName, selCreateDocInst, selTargetDocCode,
                                selTargetDocDate, selCreateDocName, selCreateDocRev, selCreateDocContactName,
                                selAccount);
                multi.addAttribute(CISales.Payment.Amount, CISales.Payment.Rate);
                multi.execute();
                while (multi.next()) {
                    final Object[] rateObj = multi.getAttribute(CISales.Payment.Rate);
                    final RateInfo rateInfo = RateInfo.getRateInfo(rateObj);
                    BigDecimal rate = null;
                    if (rateInfo.getRateUI().compareTo(BigDecimal.ONE) != 0) {
                        rate = rateInfo.getRateUI();
                    }
                    BigDecimal amount = multi.getAttribute(CISales.Payment.Amount);
                    final Instance targetDocInst = multi.getSelect(selTargetDocInst);
                    if (payDoc.equals(PayDoc.BOTH) && InstanceUtils.isKindOf(targetDocInst,
                                    CISales.PaymentDocumentOutAbstract)) {
                        amount = amount.negate();
                    }
                    final DataBean dataBean = new DataBean()
                                    .setRelInst(multi.getCurrentInstance())
                                    .setAccount(multi.getSelect(selAccount))
                                    .setAmount(amount)
                                    .setCurrencyInst(multi.getSelect(selCurrInst))
                                    .setTargetDocDate(multi.getSelect(selTargetDocDate))
                                    .setTargetDocInst(targetDocInst)
                                    .setTargetDocName(multi.getSelect(selTargetDocName))
                                    .setTargetDocCode(multi.getSelect(selTargetDocCode))
                                    .setCreateDocInst(multi.getSelect(selCreateDocInst))
                                    .setCreateDocName(multi.getSelect(selCreateDocName))
                                    .setCreateDocRevision(multi.getSelect(selCreateDocRev))
                                    .setCreateDocContactName(multi.getSelect(selCreateDocContactName))
                                    .setRate(rate);
                    beans.add(dataBean);
                    PaymentReport_Base.LOG.debug("Read {}", dataBean);
                }
                final ComparatorChain<DataBean> chain = new ComparatorChain<>();
                final Map<String, Object> filters = getFilteredReport().getFilterMap(_parameter);
                final GroupByFilterValue groupBy = (GroupByFilterValue) filters.get("groupBy");
                if (groupBy != null) {
                    final List<Enum<?>> selected = groupBy.getObject();
                    for (final Enum<?> sel : selected) {
                        switch ((Grouping) sel) {
                            case CONTACT:
                                chain.addComparator((_arg0, _arg1) -> _arg0.getCreateDocContactName()
                                                .compareTo(_arg1.getCreateDocContactName()));
                                break;
                            case TYPE:
                                chain.addComparator((_arg0, _arg1) -> _arg0.getTargetDocType().compareTo(_arg1.getTargetDocType()));
                                break;
                            default:
                                break;
                        }
                    }
                }
                chain.addComparator((_arg0, _arg1) -> _arg0.getTargetDocDate().compareTo(_arg1.getTargetDocDate()));
                chain.addComparator((_arg0, _arg1) -> _arg0.getTargetDocCode().compareTo(_arg1.getTargetDocCode()));
                Collections.sort(beans, chain);
                ret = new JRBeanCollectionDataSource(beans);
                getFilteredReport().cache(_parameter, ret);
            }
            return ret;
        }

        /**
         * Add2 query builder.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _queryBldr the query bldr
         * @throws EFapsException on error
         */
        protected void add2QueryBuilder(final Parameter _parameter,
                                        final QueryBuilder _queryBldr)
            throws EFapsException
        {
            final Map<String, Object> filterMap = filteredReport.getFilterMap(_parameter);
            final DateTime dateFrom;
            if (filterMap.containsKey("dateFrom")) {
                dateFrom = (DateTime) filterMap.get("dateFrom");
            } else {
                dateFrom = new DateTime().minusMonths(1);
            }
            final DateTime dateTo;
            if (filterMap.containsKey("dateTo")) {
                dateTo = (DateTime) filterMap.get("dateTo");
            } else {
                dateTo = new DateTime();
            }
            QueryBuilder attrQueryBldr = null;
            if (filterMap.containsKey("payDocType")) {
                final TypeFilterValue filter = (TypeFilterValue) filterMap.get("payDocType");
                if (!filter.getObject().isEmpty() && filter.getObject().iterator().next() > 0) {
                    Set<Type> types = new HashSet<>();
                    for (final Long id : filter.getObject()) {
                        types.add(Type.get(id));
                    }
                    if (filter.isNegate()) {
                        types = SetUtils.difference(CISales.PaymentDocumentIOAbstract.getType().getChildTypes(), types)
                                        .stream().filter(type -> !type.isAbstract())
                                        .collect(Collectors.toSet());
                    }
                    final Iterator<Type> typeIter = types.iterator();
                    attrQueryBldr = new QueryBuilder(typeIter.next());
                    while (typeIter.hasNext()) {
                        attrQueryBldr.addType(typeIter.next());
                    }
                    final List<Status> statusList = getStatusListFromProperties(_parameter,
                                    PropertiesUtil.getProperties4Prefix(Sales.REPORT_PAYMENT.get(),
                                                    PayDoc.BOTH.name(), true));
                    if (!statusList.isEmpty()) {
                        Type tempType = attrQueryBldr.getType();
                        while (!tempType.isCheckStatus() && tempType.getParentType() != null) {
                            tempType = tempType.getParentType();
                        }
                        attrQueryBldr.addWhereAttrEqValue(tempType.getStatusAttribute(), statusList.toArray());
                    }
                }
            }
            if (attrQueryBldr == null) {
                switch (getPayDoc(_parameter)) {
                    case IN:
                        attrQueryBldr = getQueryBldrFromProperties(_parameter, Sales.REPORT_PAYMENT.get(),
                                        PayDoc.IN.name());
                        break;
                    case OUT:
                        attrQueryBldr = getQueryBldrFromProperties(_parameter, Sales.REPORT_PAYMENT.get(),
                                        PayDoc.OUT.name());
                        break;
                    case BOTH:
                    default:
                        attrQueryBldr = getQueryBldrFromProperties(_parameter, Sales.REPORT_PAYMENT.get(),
                                        PayDoc.BOTH.name());
                        break;
                }
            }
            attrQueryBldr.addWhereAttrGreaterValue(CISales.PaymentDocumentIOAbstract.Date, dateFrom
                            .withTimeAtStartOfDay().minusMinutes(1));
            attrQueryBldr.addWhereAttrLessValue(CISales.PaymentDocumentIOAbstract.Date, dateTo.plusDays(1)
                            .withTimeAtStartOfDay());

            _queryBldr.addWhereAttrInQuery(CISales.Payment.TargetDocument, attrQueryBldr.getAttributeQuery(
                            CISales.PaymentDocumentIOAbstract.ID));

            if (filterMap.containsKey("contact")) {
                final InstanceSetFilterValue filterValue = (InstanceSetFilterValue) filterMap.get("contact");
                if (filterValue != null) {
                    final Iterator<Instance> instanceIter = filterValue.getObject().iterator();
                    while (instanceIter.hasNext()) {
                        final Instance instance = instanceIter.next();
                        if (!instance.isValid()) {
                            instanceIter.remove();
                        }
                    }
                    if (!filterValue.getObject().isEmpty()) {
                        final QueryBuilder attrQueryBldr2 = new QueryBuilder(CISales.DocumentSumAbstract);
                        if (filterValue.isNegate()) {
                            attrQueryBldr2.addWhereAttrNotEqValue(CISales.DocumentSumAbstract.Contact,
                                        filterValue.getObject().toArray());
                        } else {
                            attrQueryBldr2.addWhereAttrEqValue(CISales.DocumentSumAbstract.Contact,
                                            filterValue.getObject().toArray());
                        }
                        _queryBldr.addWhereAttrInQuery(CISales.Payment.CreateDocument,
                                        attrQueryBldr2.getAttributeQuery(CISales.DocumentSumAbstract.ID));
                    }
                }
            }

            final Instance currInst = evaluateCurrency(_parameter);
            if (InstanceUtils.isValid(currInst)) {
                _queryBldr.addWhereAttrEqValue(CISales.Payment.CurrencyLink, currInst);
            }
        }

        /**
         * Evaluate currency inst.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the currency inst
         * @throws EFapsException on error
         */
        protected Instance evaluateCurrency(final Parameter _parameter)
            throws EFapsException
        {
            final Map<String, Object> filterMap = filteredReport.getFilterMap(_parameter);
            Instance ret = null;
            if (filterMap.containsKey("currency")) {
                final CurrencyFilterValue filter = (CurrencyFilterValue) filterMap.get("currency");
                if (filter.getObject() instanceof Instance && filter.getObject().isValid()) {
                    ret = filter.getObject();
                }
            }
            return ret;
        }

        /**
         * Gets the pay doc.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the pay doc
         * @throws EFapsException on error
         */
        protected PayDoc getPayDoc(final Parameter _parameter)
            throws EFapsException
        {
            final Map<String, Object> filterMap = filteredReport.getFilterMap(_parameter);

            final PayDoc ret;
            if (filterMap.containsKey("payDoc")) {
                final EnumFilterValue filterValue = (EnumFilterValue) filterMap.get("payDoc");
                ret = (PayDoc) filterValue.getObject();
            } else {
                ret = PayDoc.BOTH;
            }
            return ret;
        }

        @Override
        protected void addColumnDefinition(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final Instance currInst = evaluateCurrency(_parameter);

            final List<ColumnBuilder<?, ?>> targetColumns = new ArrayList<>();
            if (ExportType.HTML.equals(getExType())) {
                targetColumns.add(FilteredReport.getLinkColumn(_parameter, "targetOID"));
            }
            final TextColumnBuilder<String> targetDocTypeColumn = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty("Column.targetDocType"), "targetDocType", DynamicReports.type.stringType());
            final TextColumnBuilder<String> targetDocNameColumn = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty("Column.targetDocName"), "targetDocName", DynamicReports.type.stringType());
            final TextColumnBuilder<String> targetDocCodeColumn = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty("Column.targetDocCode"), "targetDocCode", DynamicReports.type.stringType());
            final TextColumnBuilder<DateTime> targetDocDateColumn = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty("Column.targetDocCode"), "targetDocDate", DateTimeDate.get());
            final TextColumnBuilder<String> accountColumn = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty("Column.account"), "account",  DynamicReports.type.stringType());

            Collections.addAll(targetColumns, targetDocDateColumn, targetDocTypeColumn, targetDocNameColumn,
                            targetDocCodeColumn, accountColumn);

            final List<ColumnBuilder<?, ?>> createColumns = new ArrayList<>();
            if (ExportType.HTML.equals(getExType())) {
                createColumns.add(FilteredReport.getLinkColumn(_parameter, "createOID"));
            }
            final TextColumnBuilder<String> createDocContactNameColumn = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty("Column.createDocContactName"), "createDocContactName", DynamicReports.type
                                            .stringType()).setWidth(250);
            final TextColumnBuilder<String> createDocTypeColumn = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty("Column.createDocType"), "createDocType", DynamicReports.type.stringType());
            final TextColumnBuilder<String> createDocNameColumn = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty("Column.createDocName"), "createDocName", DynamicReports.type.stringType());
            final TextColumnBuilder<String> createDocRevisionColumn = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty("Column.createDocRevision"), "createDocRevision", DynamicReports.type
                                            .stringType());

            Collections.addAll(createColumns, createDocContactNameColumn, createDocTypeColumn, createDocNameColumn,
                            createDocRevisionColumn);

            final TextColumnBuilder<BigDecimal> amountColumn = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty("Column.amount"), "amount", DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<String> currencyColumn = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty("Column.currency"), "currency", DynamicReports.type.stringType());

            final TextColumnBuilder<BigDecimal> rateColumn = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty("Column.rate"), "rate", DynamicReports.type.bigDecimalType());

            final List<ColumnBuilder<?, ?>> columns = new ArrayList<>();
            columns.addAll(targetColumns);
            columns.addAll(createColumns);
            Collections.addAll(columns, amountColumn, currencyColumn, rateColumn);

            final Map<String, Object> filters = getFilteredReport().getFilterMap(_parameter);
            final GroupByFilterValue groupBy = (GroupByFilterValue) filters.get("groupBy");
            if (groupBy != null) {
                final List<Enum<?>> selected = groupBy.getObject();
                for (final Enum<?> sel : selected) {
                    switch ((Grouping) sel) {
                        case CONTACT:
                            final ColumnGroupBuilder contactGroup = DynamicReports.grp.group(createDocContactNameColumn)
                                            .groupByDataType();
                            final AggregationSubtotalBuilder<BigDecimal> groupSum = DynamicReports.sbt.sum(
                                            amountColumn);
                            _builder.groupBy(contactGroup);
                            if (InstanceUtils.isValid(currInst)) {
                                _builder.addSubtotalAtGroupFooter(contactGroup, groupSum);
                            }
                            columns.remove(createDocContactNameColumn);
                            createColumns.remove(createDocContactNameColumn);
                            break;
                        case TYPE:
                            final ColumnGroupBuilder typeGroup = DynamicReports.grp.group(targetDocTypeColumn)
                                            .groupByDataType();
                            final AggregationSubtotalBuilder<BigDecimal> groupSum2 = DynamicReports.sbt.sum(
                                            amountColumn);
                            _builder.groupBy(typeGroup);
                            if (InstanceUtils.isValid(currInst)) {
                                _builder.addSubtotalAtGroupFooter(typeGroup, groupSum2);
                            }
                            columns.remove(targetDocTypeColumn);
                            targetColumns.remove(targetDocTypeColumn);
                            break;
                        default:
                            break;
                    }
                }
            }

            final ColumnTitleGroupBuilder targetDocTitleGroup = DynamicReports.grid.titleGroup(getFilteredReport()
                            .getDBProperty("TitleGroup.targetDoc"),
                            targetColumns.toArray(new ColumnGridComponentBuilder[targetColumns.size()]));
            final ColumnTitleGroupBuilder createDocTitleGroup = DynamicReports.grid.titleGroup(getFilteredReport()
                            .getDBProperty("TitleGroup.createDoc"),
                            createColumns.toArray(new ColumnGridComponentBuilder[createColumns.size()]));

            _builder.columnGrid(targetDocTitleGroup, createDocTitleGroup, amountColumn, currencyColumn, rateColumn)
                    .addColumn(columns.toArray(new ColumnBuilder[columns.size()]));


            if (InstanceUtils.isValid(currInst)) {
                _builder.subtotalsAtSummary(DynamicReports.sbt.sum(amountColumn));
            }
        }

        /**
         * Gets the filtered report.
         *
         * @return the filtered report
         */
        public PaymentReport_Base getFilteredReport()
        {
            return filteredReport;
        }
    }

    /**
     * The Class DataBean.
     */
    public static class DataBean
    {

        /** The rel inst. */
        private Instance relInst;

        /** The amount. */
        private BigDecimal amount;

        /** The amount. */
        private BigDecimal rate;

        /** The account. */
        private String account;

        /** The currency inst. */
        private Instance currencyInst;

        /** The rel inst. */
        private Instance targetDocInst;

        /** The create doc name. */
        private String targetDocName;

        /** The create doc revision. */
        private String targetDocCode;

        /** The target doc date. */
        private DateTime targetDocDate;

        /** The rel inst. */
        private Instance createDocInst;

        /** The create doc name. */
        private String createDocName;

        /** The create doc revision. */
        private String createDocRevision;

        /** The create doc contact name. */
        private String createDocContactName;

        /**
         * Gets the account.
         *
         * @return the account
         */
        public String getAccount()
        {
            return account;
        }

        /**
         * Sets the account.
         *
         * @param _account the account
         * @return the data bean
         */
        public DataBean setAccount(final Object _account)
        {
            if (_account instanceof Collection) {
                account = String.valueOf(((Collection<?>) _account).iterator().next());
            } else {
                account = String.valueOf(_account);
            }
            return this;
        }

        /**
         * Gets the target doc type.
         *
         * @return the target doc type
         */
        public String getTargetOID()
        {
            return InstanceUtils.isValid(getTargetDocInst()) ? getTargetDocInst().getOid() : null;
        }

        /**
         * Gets the target doc type.
         *
         * @return the target doc type
         */
        public String getTargetDocType()
        {
            return InstanceUtils.isValid(getTargetDocInst()) ? getTargetDocInst().getType().getLabel() : null;
        }

        /**
         * Gets the target doc type.
         *
         * @return the target doc type
         */
        public String getCreateOID()
        {
            return InstanceUtils.isValid(getCreateDocInst()) ? getCreateDocInst().getOid() : null;
        }

        /**
         * Gets the creates the doc type.
         *
         * @return the creates the doc type
         */
        public String getCreateDocType()
        {
            return InstanceUtils.isValid(getCreateDocInst()) ? getCreateDocInst().getType().getLabel() : null;
        }

        /**
         * Gets the currency.
         *
         * @return the currency
         * @throws EFapsException on error
         */
        public String getCurrency()
            throws EFapsException
        {
            return InstanceUtils.isValid(getCurrencyInst()) ? CurrencyInst.get(getCurrencyInst()).getSymbol() : null;
        }

        /**
         * Gets the amount.
         *
         * @return the amount
         */
        public BigDecimal getAmount()
        {
            return amount;
        }

        /**
         * Sets the amount.
         *
         * @param _amount the amount
         * @return the data bean
         */
        public DataBean setAmount(final BigDecimal _amount)
        {
            amount = _amount;
            return this;
        }

        /**
         * Gets the currency inst.
         *
         * @return the currency inst
         */
        public Instance getCurrencyInst()
        {
            return currencyInst;
        }

        /**
         * Sets the currency inst.
         *
         * @param _currencyInst the currency inst
         * @return the data bean
         */
        public DataBean setCurrencyInst(final Instance _currencyInst)
        {
            currencyInst = _currencyInst;
            return this;
        }

        /**
         * Gets the rel inst.
         *
         * @return the rel inst
         */
        public Instance getRelInst()
        {
            return relInst;
        }

        /**
         * Sets the rel inst.
         *
         * @param _relInst the rel inst
         * @return the data bean
         */
        public DataBean setRelInst(final Instance _relInst)
        {
            relInst = _relInst;
            return this;
        }

        /**
         * Gets the rel inst.
         *
         * @return the rel inst
         */
        public Instance getTargetDocInst()
        {
            return targetDocInst;
        }

        /**
         * Sets the target doc inst.
         *
         * @param _targetDocInst the target doc inst
         * @return the data bean
         */
        public DataBean setTargetDocInst(final Instance _targetDocInst)
        {
            targetDocInst = _targetDocInst;
            return this;
        }

        /**
         * Gets the rel inst.
         *
         * @return the rel inst
         */
        public Instance getCreateDocInst()
        {
            return createDocInst;
        }

        /**
         * Sets the create doc inst.
         *
         * @param _createDocInst the create doc inst
         * @return the data bean
         */
        public DataBean setCreateDocInst(final Instance _createDocInst)
        {
            createDocInst = _createDocInst;
            return this;
        }

        /**
         * Gets the create doc name.
         *
         * @return the create doc name
         */
        public String getCreateDocName()
        {
            return createDocName;
        }

        /**
         * Sets the create doc name.
         *
         * @param _createDocName the create doc name
         * @return the data bean
         */
        public DataBean setCreateDocName(final String _createDocName)
        {
            createDocName = _createDocName;
            return this;
        }

        /**
         * Gets the create doc revision.
         *
         * @return the create doc revision
         */
        public String getCreateDocRevision()
        {
            return createDocRevision;
        }

        /**
         * Sets the create doc revision.
         *
         * @param _createDocRevision the create doc revision
         * @return the data bean
         */
        public DataBean setCreateDocRevision(final String _createDocRevision)
        {
            createDocRevision = _createDocRevision;
            return this;
        }

        /**
         * Gets the create doc contact name.
         *
         * @return the create doc contact name
         */
        public String getCreateDocContactName()
        {
            return createDocContactName;
        }

        /**
         * Sets the create doc contact name.
         *
         * @param _createDocContactName the create doc contact name
         * @return the data bean
         */
        public DataBean setCreateDocContactName(final String _createDocContactName)
        {
            createDocContactName = _createDocContactName;
            return this;
        }

        /**
         * Gets the create doc name.
         *
         * @return the create doc name
         */
        public String getTargetDocName()
        {
            return targetDocName;
        }

        /**
         * Sets the target doc name.
         *
         * @param _targetDocName the target doc name
         * @return the data bean
         */
        public DataBean setTargetDocName(final String _targetDocName)
        {
            targetDocName = _targetDocName;
            return this;
        }

        /**
         * Gets the create doc revision.
         *
         * @return the create doc revision
         */
        public String getTargetDocCode()
        {
            return targetDocCode;
        }

        /**
         * Sets the target doc code.
         *
         * @param _targetDocCode the target doc code
         * @return the data bean
         */
        public DataBean setTargetDocCode(final String _targetDocCode)
        {
            targetDocCode = _targetDocCode;
            return this;
        }

        /**
         * Gets the target doc date.
         *
         * @return the target doc date
         */
        public DateTime getTargetDocDate()
        {
            return targetDocDate;
        }

        /**
         * Sets the target doc date.
         *
         * @param _targetDocDate the target doc date
         * @return the data bean
         */
        public DataBean setTargetDocDate(final DateTime _targetDocDate)
        {
            targetDocDate = _targetDocDate;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #rate}.
         *
         * @return value of instance variable {@link #rate}
         */
        public BigDecimal getRate()
        {
            return rate;
        }

        /**
         * Setter method for instance variable {@link #rate}.
         *
         * @param _rate value for instance variable {@link #rate}
         */
        public DataBean setRate(final BigDecimal _rate)
        {
            rate = _rate;
            return this;
        }

        @Override
        public String toString()
        {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }
}
