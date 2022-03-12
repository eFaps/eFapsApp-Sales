/*
 * Copyright 2003 - 2020 The eFaps Team
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.lang3.BooleanUtils;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIAttribute;
import org.efaps.db.CachedMultiPrintQuery;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.history.status.StatusHistory;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport_Base;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeDate;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeMonth;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeYear;
import org.efaps.esjp.common.properties.PropertiesUtil;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.humanresource.Employee;
import org.efaps.esjp.sales.Swap;
import org.efaps.esjp.sales.Swap_Base.SwapInfo;
import org.efaps.esjp.sales.payment.DocPaymentInfo;
import org.efaps.ui.wicket.models.EmbeddedLink;
import org.efaps.util.EFapsException;
import org.efaps.util.cache.CacheReloadException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.ColumnBuilder;
import net.sf.dynamicreports.report.builder.column.ComponentColumnBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.component.GenericElementBuilder;
import net.sf.dynamicreports.report.builder.grid.ColumnGridComponentBuilder;
import net.sf.dynamicreports.report.builder.grid.ColumnTitleGroupBuilder;
import net.sf.dynamicreports.report.builder.group.ColumnGroupBuilder;
import net.sf.dynamicreports.report.constant.HorizontalTextAlignment;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

@EFapsUUID("aa4234b2-578e-11ea-82b4-0242ac130003")
@EFapsApplication("eFapsApp-Sales")
public abstract class AccountsAbstractReport_Base
    extends FilteredReport
{

    public enum FilterDate
    {
        /** Filter by date. */
        DATE,
        /** Filter by created. */
        CREATED,
        /** Filter by due-date. */
        DUEDATE;
    }

    public enum GroupBy
    {
        /** Includes a group on yearly level. */
        YEARLY,
        /** Includes a group on monthly level. */
        MONTHLY,
        /** Includes a group on daily level. */
        DAILY,
        /** Includes a group on contact level. */
        CONTACT,
        /** Includes a group on assigned level. */
        ASSIGNED,
        /** Includes a group on type level. */
        DOCTYPE,
        /** Includes a group on condition level. */
        CONDITION;
    }

    /**
     * Used to define which report to be displayed.
     */
    public enum ReportType
    {
        /** The standard. */
        STANDARD,
        /** Outgoing. */
        MANAGEMENT,
        /** Contact related. */
        MINIMAL;
    }

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AccountsAbstractReport.class);

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
        dyRp.setFileName(DBProperties.getProperty(this.getClass().getName() + ".FileName"));
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
     * Gets the group by filter value.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _className the class name
     * @return the group by filter value
     */
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected GroupByFilterValue getGroupByFilterValue(final Parameter _parameter,
                                                       final String _className)
    {
        final GroupByFilterValue ret = new GroupByFilterValue()
        {

            /** The Constant serialVersionUID. */
            private static final long serialVersionUID = 1L;

            @Override
            public List<Enum<?>> getInactive()
            {
                final List<Enum<?>> ret = super.getInactive();
                try {
                    if (!isShowAssigned()) {
                        ret.remove(GroupBy.ASSIGNED);
                    }
                    if (!isShowCondition()) {
                        ret.remove(GroupBy.CONDITION);
                    }
                    if (this.getClass().getName().contains("Contact")) {
                        ret.remove(GroupBy.ASSIGNED);
                        ret.remove(GroupBy.CONTACT);
                    }
                } catch (final EFapsException e) {
                    LOG.error("Catched", e);
                }
                return ret;
            }
        }.setClassName(_className);
        ret.setObject(new ArrayList());
        return ret;
    }

    protected String getLabel(final Parameter _parameter,
                              final String _key)
    {
        return DBProperties.getProperty(this.getClass().getName() + "." + _key);
    }

    protected abstract boolean isShowAssigned()
        throws EFapsException;

    protected abstract boolean isShowCondition()
        throws EFapsException;

    protected abstract AccountsAbstractDynReport getReport(final Parameter _parameter)
        throws EFapsException;

    protected static abstract class AccountsAbstractDynReport
        extends AbstractDynamicReport
    {

        private final AccountsAbstractReport_Base filteredReport;

        public AccountsAbstractDynReport(final AccountsAbstractReport_Base _filteredReport)
        {
            filteredReport = _filteredReport;
        }

        protected AccountsAbstractReport_Base getFilteredReport()
        {
            return filteredReport;
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
                final ReportType reportType = getReportType(_parameter);

                final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);

                final boolean includePaid = filter.containsKey("includePaid")
                                ? (boolean) filter.get("includePaid")
                                : false;

                final DateTime reportDate = getReportDate(_parameter);
                Map<Instance, String> instance2status = null;
                final List<Instance> instances;
                if (reportDate.isBefore(new DateTime().withTimeAtStartOfDay())) {
                    instance2status = calculate4Date(_parameter);
                    instances = new ArrayList<>(instance2status.keySet());
                } else {
                    final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter, getProperties(),
                                    includePaid ? "PAID" : null);
                    add2QueryBuilder(_parameter, queryBldr);
                    queryBldr.setCompanyDependent(isCompanyDependent(_parameter));
                    instances = queryBldr.getQuery().execute();
                }

                final Map<Instance, AbstractDataBean> beans = getBeans(_parameter, instances);

                if (instance2status != null) {
                    for (final Entry<Instance, AbstractDataBean> entry : beans.entrySet()) {
                        final String status = instance2status.get(entry.getKey());
                        if (status != null) {
                            entry.getValue().setDocStatus(status);
                        }
                    }
                }

                final List<AbstractDataBean> dataSource = new ArrayList<>();
                dataSource.addAll(beans.values());
                if (isShowSwapInfo() && !ReportType.MINIMAL.equals(reportType)) {
                    final Map<Instance, Set<SwapInfo>> swapMap = Swap.getSwapInfos4Documents(_parameter, instances
                                    .toArray(new Instance[instances.size()]));
                    for (final AbstractDataBean bean : dataSource) {
                        if (swapMap.containsKey(bean.getDocInst())) {
                            final Set<SwapInfo> swapInfos = swapMap.get(bean.getDocInst());
                            final StringBuilder fromStr = new StringBuilder();
                            final StringBuilder toStr = new StringBuilder();
                            for (final SwapInfo swapInfo : swapInfos) {
                                if (swapInfo.getSwapDate().isBefore(reportDate)) {
                                    if (swapInfo.isFrom()) {
                                        if (fromStr.length() > 0) {
                                            fromStr.append(", ");
                                        } else {
                                            fromStr.append(swapInfo.getDirection()).append(" ");
                                        }
                                        fromStr.append(swapInfo.getDocument());
                                    } else {
                                        if (toStr.length() > 0) {
                                            toStr.append(", ");
                                        } else {
                                            toStr.append(swapInfo.getDirection()).append(" ");
                                        }
                                        toStr.append(swapInfo.getDocument());
                                    }
                                }
                            }
                            final StringBuilder str = new StringBuilder();
                            str.append(fromStr);
                            if (str.length() > 0 && toStr.length() > 0) {
                                str.append("\n");
                            }
                            str.append(toStr);
                            bean.setSwapInfo(str.toString());
                        }
                    }
                }

                sort(_parameter, dataSource);

                final Collection<Map<String, ?>> col = new ArrayList<>();
                for (final AbstractDataBean bean : dataSource) {
                    col.add(bean.getMap(getFilteredReport().isShowCondition(),
                                    getFilteredReport().isShowAssigned(), isShowSwapInfo()));
                }
                if (ReportType.MINIMAL.equals(reportType)) {
                    final Collection<Map<String, ?>> col2 = new ArrayList<>();
                    String currentContact = "jztsw";
                    Map<String, Object> currentMap = new HashMap<>();
                    for (final Map<String, ?> values : col) {
                        if (currentContact.equals(values.get("docContactName"))) {
                            for (final Entry<String, ?> entry : values.entrySet()) {
                                if (entry.getKey().startsWith("crossTotal_")
                                                || entry.getKey().startsWith("payed_")) {
                                    BigDecimal amount = (BigDecimal) entry.getValue();
                                    if (currentMap.containsKey(entry.getKey())) {
                                        amount = amount.add((BigDecimal) currentMap.get(entry.getKey()));
                                    }
                                    currentMap.put(entry.getKey(), amount);
                                }
                            }
                        } else {
                            currentContact = String.valueOf(values.get("docContactName"));
                            currentMap = (Map<String, Object>) values;
                            col2.add(values);
                        }
                    }
                    ret = new JRMapCollectionDataSource(col2);
                } else {
                    ret = new JRMapCollectionDataSource(col);
                }
                getFilteredReport().cache(_parameter, ret);
            }
            return ret;
        }

        protected void add2QueryBuilder(final Parameter _parameter,
                                        final QueryBuilder _queryBldr)
            throws EFapsException
        {
            final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);
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
            if (InstanceUtils.isKindOf(_parameter.getInstance(), CIContacts.ContactAbstract)) {
                _queryBldr.addWhereAttrEqValue(CISales.DocumentSumAbstract.Contact, _parameter.getInstance());
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
            final CIAttribute dateAttr;
            switch (getFilterDate(_parameter)) {
                case CREATED:
                    dateAttr = CISales.DocumentSumAbstract.Created;
                    break;
                case DUEDATE:
                    dateAttr = CISales.DocumentSumAbstract.DueDate;
                    break;
                case DATE:
                default:
                    dateAttr = CISales.DocumentSumAbstract.Date;
                    break;
            }
            _queryBldr.addWhereAttrGreaterValue(dateAttr, dateFrom.withTimeAtStartOfDay().minusMinutes(1));
            _queryBldr.addWhereAttrLessValue(dateAttr, dateTo.plusDays(1).withTimeAtStartOfDay());
        }

        protected Map<Instance, String> calculate4Date(final Parameter _parameter)
            throws EFapsException
        {
            final Map<Instance, String> ret = new HashMap<>();
            final DateTime reportDate = getReportDate(_parameter);

            final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);
            final DateTime dateFrom = filter.containsKey("dateFrom") ? (DateTime) filter.get("dateFrom") : null;

            Collection<Long> typeIds = null;
            if (filter.containsKey("type")) {
                final TypeFilterValue filters = (TypeFilterValue) filter.get("type");
                if (!filters.getObject().isEmpty()) {
                    typeIds = filters.getObject();
                }
            }
            if (typeIds == null) {
                typeIds = getTypeIds();
            }

            final StatusHistory statusHistory = new StatusHistory();
            final Map<Instance, Long> inst2status = statusHistory.getStatusUpdatesByDateAndTypes(_parameter,
                            reportDate, dateFrom, typeIds.stream().toArray(Long[]::new));

            LOG.debug("Found {} for inst2status", inst2status.size());

            // prepare the additional filters to be applied
            final Set<Instance> filterlist = new HashSet<Instance>();
            boolean applyFilterlist = false;
            if (filter.containsKey("contact") || filter.containsKey("currency")) {
                applyFilterlist = true;
                final List<Type> types = getTypes();
                final QueryBuilder queryBldr = new QueryBuilder(getTypes().get(0));
                queryBldr.addType(types.stream().toArray(Type[]::new));
                if (filter.containsKey("currency")) {
                    final Instance contact = ((InstanceFilterValue) filter.get("contact")).getObject();
                    if (contact.isValid()) {
                        queryBldr.addWhereAttrEqValue(CISales.DocumentSumAbstract.Contact, contact);
                    }
                }
                if (filter.containsKey("currency")) {
                    final Instance currency = ((CurrencyFilterValue) filter.get("currency")).getObject();
                    if (currency.isValid()) {
                        queryBldr.addWhereAttrEqValue(CISales.DocumentSumAbstract.RateCurrencyId, currency);
                    }
                }
                filterlist.addAll(queryBldr.getQuery().executeWithoutAccessCheck());
            }

            // get a Set of status that are included in the report
            final Set<Long> statusIds = getFilteredReport().getStatusListFromProperties(_parameter, getProperties())
                            .stream()
                            .map(status -> status.getId())
                            .collect(Collectors.toSet());

            LOG.debug("Checking agains statusSet {}", statusIds);

            // check the list of instances with their status for the given date
            for (final Entry<Instance, Long> entry : inst2status.entrySet()) {
                // if the status is wanted verify that it is in the report
                // and check if the instance is part of the filterlist (if
                // activated)
                if (statusIds.contains(entry.getValue())
                                && (!applyFilterlist || filterlist.contains(entry.getKey()))) {
                    ret.put(entry.getKey(), Status.get(entry.getValue()).getLabel());
                }
            }
            LOG.debug("Returning {} ", ret);
            return ret;
        }

        protected Map<Instance, AbstractDataBean> getBeans(final Parameter _parameter, final List<Instance> _instances)
            throws EFapsException
        {
            final Map<Instance, AbstractDataBean> beans = new HashMap<>();

            final MultiPrintQuery multi = new MultiPrintQuery(_instances);
            multi.addAttribute(CISales.DocumentSumAbstract.Created, CISales.DocumentSumAbstract.Date,
                            CISales.DocumentSumAbstract.Name, CISales.DocumentSumAbstract.DueDate,
                            CISales.DocumentSumAbstract.Rate, CISales.DocumentSumAbstract.CrossTotal,
                            CISales.DocumentSumAbstract.CurrencyId, CISales.DocumentSumAbstract.RateCurrencyId,
                            CISales.DocumentSumAbstract.RateCrossTotal, CISales.DocumentSumAbstract.Revision);

            final SelectBuilder selContactInst = new SelectBuilder().linkto(CISales.DocumentSumAbstract.Contact)
                            .instance();
            final SelectBuilder selContactName = new SelectBuilder().linkto(CISales.DocumentSumAbstract.Contact)
                            .attribute(CIContacts.Contact.Name);
            final SelectBuilder selStatus = new SelectBuilder().status().label();
            multi.addSelect(selStatus);
            if (!isContactReport()) {
                multi.addSelect(selContactInst, selContactName);
            }
            multi.execute();
            final DateTime reportDate = getReportDate(_parameter);
            final Set<Instance> docInsts = new HashSet<>();
            while (multi.next()) {
                docInsts.add(multi.getCurrentInstance());
                final AbstractDataBean dataBean = getDataBean(_parameter)
                                .setDocInst(multi.getCurrentInstance())
                                .setDocCreated(multi.<DateTime>getAttribute(CISales.DocumentSumAbstract.Created))
                                .setDocDate(multi.<DateTime>getAttribute(CISales.DocumentSumAbstract.Date))
                                .setDocDueDate(multi.<DateTime>getAttribute(CISales.DocumentSumAbstract.DueDate))
                                .setDocName(multi.<String>getAttribute(CISales.DocumentSumAbstract.Name))
                                .setDocRevision(multi.<String>getAttribute(CISales.DocumentSumAbstract.Revision))
                                .setDocStatus(multi.<String>getSelect(selStatus))
                                .setReportDate(reportDate);

                if (!isContactReport()) {
                    dataBean.setContactInst(multi.<Instance>getSelect(selContactInst))
                                    .setDocContactName(multi.<String>getSelect(selContactName));
                }

                if (isCurrencyBase(_parameter)) {
                    dataBean.setCurrencyBase(true)
                                    .addCross(multi.<Long>getAttribute(CISales.DocumentSumAbstract.CurrencyId),
                                                    multi.<BigDecimal>getAttribute(
                                                                    CISales.DocumentSumAbstract.CrossTotal));
                } else {
                    dataBean.setRate(multi.<Object[]>getAttribute(CISales.DocumentSumAbstract.Rate))
                                    .addCross(multi.<Long>getAttribute(CISales.DocumentSumAbstract.RateCurrencyId),
                                                    multi.<BigDecimal>getAttribute(
                                                                    CISales.DocumentSumAbstract.RateCrossTotal));
                }
                beans.put(dataBean.getDocInst(), dataBean);
            }
            return beans;
        }

        protected abstract AbstractDataBean getDataBean(final Parameter _parameter);

        protected abstract Properties getProperties()
            throws EFapsException;

        protected abstract boolean isShowSwapInfo()
            throws EFapsException;

        protected List<Long> getTypeIds()
            throws EFapsException
        {
            return getTypes().stream().map(Type::getId).collect(Collectors.toList());
        }

        /**
         * Get a list of the types used in the report
         *
         * @return list of types
         * @throws EFapsException
         */
        protected List<Type> getTypes()
            throws EFapsException
        {
            final Properties props = getProperties();
            return PropertiesUtil.analyseProperty(props, "Type", 0).values()
                            .stream()
                            .map(type -> {
                                try {
                                    return isUUID(type)
                                                    ? Type.get(UUID.fromString(type))
                                                    : Type.get(type);
                                } catch (final CacheReloadException e) {
                                    LOG.error("Invalid Type Definition for {}", type);
                                }
                                return null;
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
        }

        protected void sort(final Parameter _parameter, final List<AbstractDataBean> _dataSource)
            throws EFapsException
        {
            final ReportType reportType = getReportType(_parameter);
            if (ReportType.MINIMAL.equals(reportType)) {
                Collections.sort(_dataSource, (_o1, _o2) -> _o1.getDocContactName().compareTo(
                                _o2.getDocContactName()));
            } else {
                final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);
                final FilterDate filterDate = getFilterDate(_parameter);
                final ComparatorChain<AbstractDataBean> chain = new ComparatorChain<>();
                final GroupByFilterValue groupBy = (GroupByFilterValue) filter.get("groupBy");
                if (groupBy != null) {
                    final List<Enum<?>> selected = groupBy.getObject();
                    for (final Enum<?> sel : selected) {
                        switch ((GroupBy) sel) {
                            case ASSIGNED:
                                chain.addComparator((_o1, _o2) -> {
                                    int ret1 = 0;
                                    try {
                                        ret1 = _o1.getAssigned().compareTo(_o2.getAssigned());
                                    } catch (final EFapsException e) {
                                        AbstractDynamicReport_Base.LOG.error("Catched", e);
                                    }
                                    return ret1;
                                });
                                break;
                            case CONTACT:
                                chain.addComparator((_o1, _o2) -> _o1.getDocContactName().compareTo(
                                                _o2.getDocContactName()));
                                break;
                            case DAILY:
                            case MONTHLY:
                            case YEARLY:
                                chain.addComparator((_o1, _o2) -> {
                                    final int ret1;
                                    switch (filterDate) {
                                        case CREATED:
                                            ret1 = _o1.getDocCreated().compareTo(_o2.getDocCreated());
                                            break;
                                        case DUEDATE:
                                            if (_o1.getDocDueDate() != null && _o2.getDocDueDate() != null) {
                                                ret1 = _o1.getDocDueDate().compareTo(_o2.getDocDueDate());
                                            } else {
                                                ret1 = 0;
                                            }
                                            break;
                                        case DATE:
                                        default:
                                            ret1 = _o1.getDocDate().compareTo(_o2.getDocDate());
                                            break;
                                    }
                                    return ret1;
                                });
                                break;
                            case DOCTYPE:
                                chain.addComparator((_o1, _o2) -> _o1.getDocInst().getType().getLabel().compareTo(
                                                _o2.getDocInst().getType().getLabel()));
                                break;
                            case CONDITION:
                                chain.addComparator((_o1, _o2) -> _o1.getCondition().compareTo(_o2.getCondition()));
                                break;
                            default:
                                chain.addComparator((_o1, _o2) -> _o1.getDocContactName().compareTo(
                                                _o2.getDocContactName()));
                                break;
                        }
                    }
                }
                chain.addComparator((_o1, _o2) -> {
                    final int ret1;
                    switch (filterDate) {
                        case CREATED:
                            ret1 = _o1.getDocCreated().compareTo(_o2.getDocCreated());
                            break;
                        case DUEDATE:
                            if (_o1.getDocDueDate() != null && _o2.getDocDueDate() != null) {
                                ret1 = _o1.getDocDueDate().compareTo(_o2.getDocDueDate());
                            } else {
                                ret1 = 0;
                            }
                            break;
                        case DATE:
                        default:
                            ret1 = _o1.getDocDate().compareTo(_o2.getDocDate());
                            break;
                    }
                    return ret1;
                });
                Collections.sort(_dataSource, chain);
            }
        }

        protected boolean isContactReport()
        {
            return false;
        }

        protected ReportType getReportType(final Parameter _parameter)
            throws EFapsException
        {
            final Map<String, Object> filterMap = getFilteredReport().getFilterMap(_parameter);
            final ReportType ret;
            if (filterMap.containsKey("reportType")) {
                final EnumFilterValue filterValue = (EnumFilterValue) filterMap.get("reportType");
                ret = (ReportType) filterValue.getObject();
            } else {
                ret = ReportType.STANDARD;
            }
            return ret;
        }

        protected DateTime getReportDate(final Parameter _parameter)
            throws EFapsException
        {
            final Map<String, Object> filterMap = getFilteredReport().getFilterMap(_parameter);
            final DateTime ret;
            if (filterMap.containsKey("reportDate")) {
                ret = (DateTime) filterMap.get("reportDate");
            } else {
                ret = new DateTime().withTimeAtStartOfDay();
            }
            return ret;
        }

        protected FilterDate getFilterDate(final Parameter _parameter)
            throws EFapsException
        {
            final Map<String, Object> filterMap = getFilteredReport().getFilterMap(_parameter);

            final FilterDate ret;
            if (filterMap.containsKey("filterDate")) {
                final EnumFilterValue filterValue = (EnumFilterValue) filterMap.get("filterDate");
                ret = (FilterDate) filterValue.getObject();
            } else {
                ret = FilterDate.DATE;
            }
            return ret;
        }

        /**
         * Checks if is company depended.
         *
         * @param _parameter the _parameter
         * @return true, if is company depended
         * @throws EFapsException on error
         */
        protected boolean isCompanyDependent(final Parameter _parameter)
            throws EFapsException
        {
            return "true".equalsIgnoreCase(getProperty(_parameter, "CompanyDependent", "true"));
        }

        protected boolean isCurrencyBase(final Parameter _parameter)
            throws EFapsException
        {
            final Collection<CurrencyInst> currencies = getCurrencyInst4Report(_parameter);
            return currencies.size() == 1 && "BASE".equals(currencies.iterator().next().getInstance().getKey());
        }

        /**
         * Gets the currency instances for the report.
         *
         * @param _parameter the _parameter
         * @return the currency inst4 report
         * @throws EFapsException on error
         */
        protected Collection<CurrencyInst> getCurrencyInst4Report(final Parameter _parameter)
            throws EFapsException
        {
            Collection<CurrencyInst> ret = new HashSet<>();
            final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);
            if (filter.containsKey("currency")) {
                final Instance currency = ((CurrencyFilterValue) filter.get("currency")).getObject();
                if ("BASE".equals(currency.getKey())) {
                    ret.add(new CurrencyInst(currency)
                    {

                        @Override
                        public String getName()
                            throws EFapsException
                        {
                            return DBProperties.getProperty(FilteredReport.class.getName() + ".BaseCurrency");
                        }

                        @Override
                        public String getISOCode()
                            throws EFapsException
                        {
                            return "BASE";
                        }
                    });
                } else if (currency.isValid()) {
                    ret.add(new CurrencyInst(currency));
                } else {
                    ret = CurrencyInst.getAvailable();
                }
            } else {
                ret = CurrencyInst.getAvailable();
            }
            return ret;
        }

        @Override
        protected void addColumnDefinition(final Parameter _parameter, final JasperReportBuilder _builder)
            throws EFapsException
        {
            final ReportType reportType = getReportType(_parameter);
            if (ReportType.MINIMAL.equals(reportType)) {
                addColumnDefinition4Minimal(_parameter, _builder);
            } else {
                final Map<String, Object> filterMap = getFilteredReport().getFilterMap(_parameter);
                final GroupByFilterValue groupBy = (GroupByFilterValue) filterMap.get("groupBy");
                final List<Enum<?>> selected = groupBy == null ? new ArrayList<>() : groupBy.getObject();

                final String filter;
                switch (getFilterDate(_parameter)) {
                    case CREATED:
                        filter = "docCreated";
                        break;
                    case DUEDATE:
                        filter = "docDueDate";
                        break;
                    case DATE:
                    default:
                        filter = "docDate";
                        break;
                }

                final TextColumnBuilder<DateTime> monthColumn = AbstractDynamicReport_Base.column(
                                getFilteredReport().getLabel(_parameter, "FilterDate1"), filter,
                                DateTimeMonth.get());
                final TextColumnBuilder<DateTime> yearColumn = AbstractDynamicReport_Base.column(
                                getFilteredReport().getLabel(_parameter, "FilterDate2"), filter,
                                DateTimeYear.get());
                final TextColumnBuilder<DateTime> dayColumn = AbstractDynamicReport_Base.column(
                                getFilteredReport().getLabel(_parameter, "FilterDate3"), filter,
                                DateTimeYear.get());
                final TextColumnBuilder<DateTime> dateColumn = AbstractDynamicReport_Base.column(
                                getFilteredReport().getLabel(_parameter, "Date"), "docDate",
                                DateTimeDate.get());
                final TextColumnBuilder<String> typeColumn = DynamicReports.col.column(
                                getFilteredReport().getLabel(_parameter, "Type"), "docType",
                                DynamicReports.type.stringType());
                final TextColumnBuilder<DateTime> createdColumn = AbstractDynamicReport_Base.column(
                                getFilteredReport().getLabel(_parameter, "Created"), "docCreated",
                                DateTimeDate.get());
                final TextColumnBuilder<String> nameColumn = DynamicReports.col.column(
                                getFilteredReport().getLabel(_parameter, "Name"), "docName",
                                DynamicReports.type.stringType());
                final TextColumnBuilder<String> revisionColumn = DynamicReports.col.column(
                                getFilteredReport().getLabel(_parameter, "Revision"), "docRevision",
                                DynamicReports.type.stringType());
                final TextColumnBuilder<String> contactNameColumn = DynamicReports.col.column(
                                getFilteredReport().getLabel(_parameter, "ContactName"), "docContactName",
                                DynamicReports.type.stringType());
                final TextColumnBuilder<DateTime> dueDateColumn = AbstractDynamicReport_Base.column(
                                getFilteredReport().getLabel(_parameter, "DueDate"), "docDueDate",
                                DateTimeDate.get());
                final TextColumnBuilder<String> docStatusColumn = DynamicReports.col.column(
                                getFilteredReport().getLabel(_parameter, "Status"), "docStatus",
                                DynamicReports.type.stringType());
                final TextColumnBuilder<String> conditionColumn = DynamicReports.col.column(
                                getFilteredReport().getLabel(_parameter, "Condition"), "condition",
                                DynamicReports.type.stringType());
                final TextColumnBuilder<String> assignedColumn = DynamicReports.col.column(
                                getFilteredReport().getLabel(_parameter, "Assigned"), "assigned",
                                DynamicReports.type.stringType());
                final TextColumnBuilder<String> swapInfoColumn = DynamicReports.col.column(
                                getFilteredReport().getLabel(_parameter, "SwapInfo"), "swapInfo",
                                DynamicReports.type.stringType()).setWidth(120);

                final GenericElementBuilder linkElement = DynamicReports.cmp.genericElement(
                                "http://www.efaps.org", "efapslink")
                                .addParameter(EmbeddedLink.JASPER_PARAMETERKEY, new LinkExpression("docOID"))
                                .setHeight(12).setWidth(25);

                final ComponentColumnBuilder linkColumn = DynamicReports.col.componentColumn(linkElement).setTitle("");

                final ColumnGroupBuilder assignedGroup = DynamicReports.grp.group(assignedColumn).groupByDataType();
                final ColumnGroupBuilder yearGroup = DynamicReports.grp.group(yearColumn).groupByDataType();
                final ColumnGroupBuilder monthGroup = DynamicReports.grp.group(monthColumn).groupByDataType();
                final ColumnGroupBuilder dayGroup = DynamicReports.grp.group(dayColumn).groupByDataType();
                final ColumnGroupBuilder contactGroup = DynamicReports.grp.group(contactNameColumn).groupByDataType();
                final ColumnGroupBuilder docTypeGroup = DynamicReports.grp.group(typeColumn).groupByDataType();
                final ColumnGroupBuilder conditionGroup = DynamicReports.grp.group(conditionColumn).groupByDataType();

                final List<ColumnBuilder<?, ?>> columnList = new ArrayList<>();
                final List<ColumnGridComponentBuilder> gridList = new ArrayList<>();
                Collections.addAll(columnList, yearColumn, monthColumn, dayColumn);

                if (getExType().equals(ExportType.HTML)) {
                    columnList.add(linkColumn);
                    gridList.add(linkColumn);
                }
                if (!selected.contains(GroupBy.DOCTYPE)) {
                    gridList.add(typeColumn);
                }
                gridList.add(dateColumn);
                gridList.add(dueDateColumn);

                gridList.add(nameColumn);
                if (ReportType.STANDARD.equals(reportType) && isShowRevision()) {
                    gridList.add(revisionColumn);
                }

                if (!selected.contains(GroupBy.CONDITION) && getFilteredReport().isShowCondition()) {
                    gridList.add(conditionColumn);
                }
                if (!selected.contains(GroupBy.CONTACT) && !isContactReport()) {
                    gridList.add(contactNameColumn);
                }
                if (ReportType.STANDARD.equals(reportType) && getFilteredReport().isShowAssigned()
                                && !selected.contains(GroupBy.ASSIGNED)
                                && !isContactReport()) {
                    gridList.add(assignedColumn);
                }

                if (isShowSwapInfo() && ReportType.STANDARD.equals(reportType)) {
                    gridList.add(swapInfoColumn);
                }
                if (ReportType.STANDARD.equals(reportType)) {
                    Collections.addAll(gridList, createdColumn, docStatusColumn);
                }

                for (final CurrencyInst currency : getCurrencyInst4Report(_parameter)) {
                    final TextColumnBuilder<BigDecimal> crossColumn = DynamicReports.col.column(
                                    getFilteredReport().getLabel(_parameter, "crossTotal"),
                                    "crossTotal_" + currency.getISOCode(), DynamicReports.type.bigDecimalType());
                    final TextColumnBuilder<BigDecimal> payColumn = DynamicReports.col.column(
                                    getFilteredReport().getLabel(_parameter, "payed"),
                                    "payed_" + currency.getISOCode(), DynamicReports.type.bigDecimalType());
                    final TextColumnBuilder<BigDecimal> result = crossColumn.subtract(payColumn);
                    result.setTitle(getFilteredReport().getLabel(_parameter, "result"));

                    final ColumnTitleGroupBuilder titelGroup = DynamicReports.grid.titleGroup(currency.getName(),
                                    crossColumn, payColumn, result);
                    gridList.add(titelGroup);
                    Collections.addAll(columnList, crossColumn, payColumn, result);

                    if (ReportType.STANDARD.equals(reportType) && !currency.getInstance()
                                    .equals(Currency.getBaseCurrency()) && !isCurrencyBase(_parameter)) {
                        final TextColumnBuilder<BigDecimal> rateColumn = DynamicReports.col.column(
                                        getFilteredReport().getLabel(_parameter, "rate"),
                                        "rate_" + currency.getISOCode(), DynamicReports.type.bigDecimalType());
                        columnList.add(rateColumn);
                        titelGroup.add(rateColumn);
                    }

                    for (final Enum<?> sel : selected) {
                        switch ((GroupBy) sel) {
                            case ASSIGNED:
                                _builder.addSubtotalAtGroupFooter(assignedGroup, DynamicReports.sbt.sum(crossColumn));
                                _builder.addSubtotalAtGroupFooter(assignedGroup, DynamicReports.sbt.sum(payColumn));
                                _builder.addSubtotalAtGroupFooter(assignedGroup, DynamicReports.sbt.sum(result));
                                break;
                            case CONTACT:
                                _builder.addSubtotalAtGroupFooter(contactGroup, DynamicReports.sbt.sum(crossColumn));
                                _builder.addSubtotalAtGroupFooter(contactGroup, DynamicReports.sbt.sum(payColumn));
                                _builder.addSubtotalAtGroupFooter(contactGroup, DynamicReports.sbt.sum(result));
                                break;
                            case MONTHLY:
                                _builder.addSubtotalAtGroupFooter(monthGroup, DynamicReports.sbt.sum(crossColumn));
                                _builder.addSubtotalAtGroupFooter(monthGroup, DynamicReports.sbt.sum(payColumn));
                                _builder.addSubtotalAtGroupFooter(monthGroup, DynamicReports.sbt.sum(result));
                                break;
                            case YEARLY:
                                _builder.addSubtotalAtGroupFooter(yearGroup, DynamicReports.sbt.sum(crossColumn));
                                _builder.addSubtotalAtGroupFooter(yearGroup, DynamicReports.sbt.sum(payColumn));
                                _builder.addSubtotalAtGroupFooter(yearGroup, DynamicReports.sbt.sum(result));
                                break;
                            case DAILY:
                                _builder.addSubtotalAtGroupFooter(dayGroup, DynamicReports.sbt.sum(crossColumn));
                                _builder.addSubtotalAtGroupFooter(dayGroup, DynamicReports.sbt.sum(payColumn));
                                _builder.addSubtotalAtGroupFooter(dayGroup, DynamicReports.sbt.sum(result));
                                break;
                            case DOCTYPE:
                                _builder.addSubtotalAtGroupFooter(docTypeGroup, DynamicReports.sbt.sum(crossColumn));
                                _builder.addSubtotalAtGroupFooter(docTypeGroup, DynamicReports.sbt.sum(payColumn));
                                _builder.addSubtotalAtGroupFooter(docTypeGroup, DynamicReports.sbt.sum(result));
                                break;
                            case CONDITION:
                                _builder.addSubtotalAtGroupFooter(conditionGroup, DynamicReports.sbt.sum(crossColumn));
                                _builder.addSubtotalAtGroupFooter(conditionGroup, DynamicReports.sbt.sum(payColumn));
                                _builder.addSubtotalAtGroupFooter(conditionGroup, DynamicReports.sbt.sum(result));
                                break;
                            default:
                                break;
                        }
                    }
                    _builder.addSubtotalAtSummary(DynamicReports.sbt.sum(crossColumn));
                    _builder.addSubtotalAtSummary(DynamicReports.sbt.sum(payColumn));
                    _builder.addSubtotalAtSummary(DynamicReports.sbt.sum(result));
                }

                Collections.addAll(columnList, typeColumn.setFixedWidth(120),
                                dateColumn, dueDateColumn,
                                nameColumn.setHorizontalTextAlignment(HorizontalTextAlignment.CENTER),
                                revisionColumn);

                if (getFilteredReport().isShowCondition()) {
                    columnList.add(conditionColumn);
                }

                if (!selected.contains(GroupBy.CONTACT) && !isContactReport()) {
                    columnList.add(contactNameColumn.setFixedWidth(200));
                }

                if (getFilteredReport().isShowAssigned() && !selected.contains(GroupBy.ASSIGNED)
                                && !isContactReport()) {
                    columnList.add(assignedColumn);
                }

                if (isShowSwapInfo() && ReportType.STANDARD.equals(reportType)) {
                    columnList.add(swapInfoColumn);
                }
                final TextColumnBuilder<String> grpTtlClm;
                if (ReportType.MANAGEMENT.equals(reportType)) {
                    grpTtlClm = nameColumn;
                } else {
                    grpTtlClm = docStatusColumn;
                    Collections.addAll(columnList, createdColumn, docStatusColumn);
                }

                _builder.addSubtotalAtSummary(DynamicReports.sbt.text(getFilteredReport().getLabel(
                                _parameter, "summaryTotal"), grpTtlClm));

                for (final Enum<?> sel : selected) {
                    switch ((GroupBy) sel) {
                        case ASSIGNED:
                            _builder.groupBy(assignedGroup);
                            _builder.addSubtotalAtGroupFooter(assignedGroup, DynamicReports.sbt.text(getFilteredReport()
                                            .getLabel(_parameter, "assignedGroupTotal"), grpTtlClm));
                            break;
                        case CONTACT:
                            _builder.groupBy(contactGroup);
                            _builder.addSubtotalAtGroupFooter(contactGroup, DynamicReports.sbt.text(getFilteredReport()
                                            .getLabel(_parameter, "contactGroupTotal"), grpTtlClm));
                            break;
                        case DAILY:
                            _builder.groupBy(dayGroup);
                            _builder.addSubtotalAtGroupFooter(dayGroup, DynamicReports.sbt.text(getFilteredReport()
                                            .getLabel(_parameter, "dayGroupTotal"), grpTtlClm));
                            break;
                        case MONTHLY:
                            _builder.groupBy(monthGroup);
                            _builder.addSubtotalAtGroupFooter(monthGroup, DynamicReports.sbt.text(getFilteredReport()
                                            .getLabel(_parameter, "monthGroupTotal"), grpTtlClm));
                            break;
                        case YEARLY:
                            _builder.groupBy(yearGroup);
                            _builder.addSubtotalAtGroupFooter(yearGroup, DynamicReports.sbt.text(getFilteredReport()
                                            .getLabel(_parameter, "yearGroupTotal"), grpTtlClm));
                            break;
                        case DOCTYPE:
                            _builder.groupBy(docTypeGroup);
                            _builder.addSubtotalAtGroupFooter(docTypeGroup, DynamicReports.sbt.text(getFilteredReport()
                                            .getLabel(_parameter, "docTypeGroupTotal"), grpTtlClm));
                            break;
                        case CONDITION:
                            _builder.groupBy(conditionGroup);
                            _builder.addSubtotalAtGroupFooter(conditionGroup, DynamicReports.sbt.text(
                                            getFilteredReport().getLabel(_parameter, "conditionGroupTotal"),
                                            grpTtlClm));
                            break;
                        default:
                            break;
                    }
                }
                _builder.columnGrid(gridList.toArray(new ColumnGridComponentBuilder[gridList.size()]));
                _builder.addColumn(columnList.toArray(new ColumnBuilder[gridList.size()]));
            }
        }

        /**
         * Adds the column definition for minimal.
         *
         * @param _parameter the parameter
         * @param _builder the builder
         * @throws EFapsException on error
         */
        protected void addColumnDefinition4Minimal(final Parameter _parameter,
                                                   final JasperReportBuilder _builder)
            throws EFapsException
        {
            final TextColumnBuilder<String> contactNameColumn = DynamicReports.col.column(
                            getFilteredReport().getLabel(_parameter, "ContactName"), "docContactName",
                            DynamicReports.type.stringType()).setFixedWidth(200);

            final List<ColumnBuilder<?, ?>> columnList = new ArrayList<>();
            final List<ColumnGridComponentBuilder> gridList = new ArrayList<>();
            Collections.addAll(columnList, contactNameColumn);
            gridList.add(contactNameColumn);

            for (final CurrencyInst currency : getCurrencyInst4Report(_parameter)) {
                final TextColumnBuilder<BigDecimal> crossColumn = DynamicReports.col.column(
                                getFilteredReport().getLabel(_parameter, "crossTotal"),
                                "crossTotal_" + currency.getISOCode(), DynamicReports.type.bigDecimalType());
                final TextColumnBuilder<BigDecimal> payColumn = DynamicReports.col.column(
                                getFilteredReport().getLabel(_parameter, "payed"),
                                "payed_" + currency.getISOCode(), DynamicReports.type.bigDecimalType());
                final TextColumnBuilder<BigDecimal> result = crossColumn.subtract(payColumn);
                result.setTitle(getFilteredReport().getLabel(_parameter, "result"));

                final ColumnTitleGroupBuilder titelGroup = DynamicReports.grid.titleGroup(currency.getName(),
                                crossColumn, payColumn, result);
                gridList.add(titelGroup);
                Collections.addAll(columnList, crossColumn, payColumn, result);
                _builder.addSubtotalAtSummary(DynamicReports.sbt.sum(crossColumn), DynamicReports.sbt.sum(payColumn),
                                DynamicReports.sbt.sum(result));
            }
            _builder.columnGrid(gridList.toArray(new ColumnGridComponentBuilder[gridList.size()]));
            _builder.addColumn(columnList.toArray(new ColumnBuilder[gridList.size()]));
        }

        protected boolean isShowRevision()
        {
            return false;
        }
    }

    protected static abstract class AbstractDataBean
    {

        /** The doc inst. */
        private Instance docInst;

        /** The doc created. */
        private DateTime docCreated;

        /** The doc date. */
        private DateTime docDate;

        /** The doc due date. */
        private DateTime docDueDate;

        /** The doc name. */
        private String docName;

        /** The doc revision. */
        private String docRevision;

        /** The doc status. */
        private String docStatus;

        /** The doc contact name. */
        private String docContactName;

        /** The contact inst. */
        private Instance contactInst;

        /** The cross. */
        private final Map<Long, BigDecimal> cross = new HashMap<>();

        /** The payments. */
        private final Map<Long, BigDecimal> payments = new HashMap<>();

        /** The rate. */
        private Object[] rate;

        /** The currency base. */
        private boolean currencyBase;

        /** The condition. */
        private String condition;

        /** The swapInfo. */
        private String swapInfo;

        /** The report date. */
        private DateTime reportDate;

        protected abstract Properties getProperties()
            throws EFapsException;

        /**
         * Checks if is currency base.
         *
         * @return true, if is currency base
         */
        public boolean isCurrencyBase()
        {
            return currencyBase;
        }

        /**
         * Sets the currency base.
         *
         * @param _currencyBase the _currency base
         * @return the data bean
         */
        public AbstractDataBean setCurrencyBase(final boolean _currencyBase)
        {
            currencyBase = _currencyBase;
            return this;
        }

        /**
         * Gets the map.
         *
         * @param _showCondition the show condition
         * @param _showAssigned the show assigned
         * @param _showSwapInfo the show swap info
         * @return the map
         * @throws EFapsException on error
         */
        public Map<String, ?> getMap(final boolean _showCondition,
                                     final boolean _showAssigned,
                                     final boolean _showSwapInfo)
            throws EFapsException
        {
            if (payments.isEmpty()) {
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
            if (_showCondition) {
                ret.put("condition", getCondition());
            }
            if (_showAssigned) {
                ret.put("assigned", getAssigned());
            }

            if (_showSwapInfo) {
                ret.put("swapInfo", getSwapInfo());
            }
            final boolean negate = BooleanUtils.toBoolean(getProperties().getProperty(getDocInst().getType().getName()
                            + ".Negate"));
            if (isCurrencyBase()) {
                final BigDecimal crossTmp = cross.get(Currency.getBaseCurrency().getId());
                if (crossTmp != null) {
                    ret.put("crossTotal_BASE", negate ? crossTmp.negate() : crossTmp);
                }
                final BigDecimal payTmp = payments.get(Currency.getBaseCurrency().getId());
                if (payTmp != null) {
                    ret.put("payed_BASE", negate ? payTmp.negate() : payTmp);
                }
            } else {
                for (final CurrencyInst currency : CurrencyInst.getAvailable()) {
                    if (cross.containsKey(currency.getInstance().getId())) {
                        final BigDecimal crossTmp = cross.get(currency.getInstance().getId());
                        if (crossTmp != null) {
                            ret.put("crossTotal_" + currency.getISOCode(), negate ? crossTmp.negate() : crossTmp);
                        }
                        final BigDecimal payTmp = payments.get(currency.getInstance().getId());
                        if (payTmp != null) {
                            ret.put("payed_" + currency.getISOCode(), negate ? payTmp.negate() : payTmp);
                        }
                        if (!currency.getInstance().equals(Currency.getBaseCurrency())) {
                            ret.put("rate_" + currency.getISOCode(), currency.isInvert() ? getRate()[1] : getRate()[0]);
                        }
                    }
                }
            }
            return ret;
        }

        /**
         * Adds the cross.
         *
         * @param _currencyId the currency id
         * @param _cross the _cross
         * @return the data bean
         */
        public AbstractDataBean addCross(final Long _currencyId,
                                         final BigDecimal _cross)
        {
            cross.put(_currencyId, _cross);
            return this;
        }

        /**
         * Getter method for the instance variable {@link #docInst}.
         *
         * @return value of instance variable {@link #docInst}
         */
        public Instance getDocInst()
        {
            return docInst;
        }

        /**
         * Setter method for instance variable {@link #docInst}.
         *
         * @param _docInst value for instance variable {@link #docInst}
         * @return the data bean
         */
        public AbstractDataBean setDocInst(final Instance _docInst)
        {
            docInst = _docInst;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #docCreated}.
         *
         * @return value of instance variable {@link #docCreated}
         */
        public DateTime getDocCreated()
        {
            return docCreated;
        }

        /**
         * Setter method for instance variable {@link #docCreated}.
         *
         * @param _docCreated value for instance variable {@link #docCreated}
         * @return the data bean
         */
        public AbstractDataBean setDocCreated(final DateTime _docCreated)
        {
            docCreated = _docCreated;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #docDate}.
         *
         * @return value of instance variable {@link #docDate}
         */
        public DateTime getDocDate()
        {
            return docDate;
        }

        /**
         * Setter method for instance variable {@link #docDate}.
         *
         * @param _docDate value for instance variable {@link #docDate}
         * @return the data bean
         */
        public AbstractDataBean setDocDate(final DateTime _docDate)
        {
            docDate = _docDate;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #docDueDate}.
         *
         * @return value of instance variable {@link #docDueDate}
         */
        public DateTime getDocDueDate()
        {
            return docDueDate;
        }

        /**
         * Setter method for instance variable {@link #docDueDate}.
         *
         * @param _docDueDate value for instance variable {@link #docDueDate}
         * @return the data bean
         */
        public AbstractDataBean setDocDueDate(final DateTime _docDueDate)
        {
            docDueDate = _docDueDate;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #docName}.
         *
         * @return value of instance variable {@link #docName}
         */
        public String getDocName()
        {
            return docName;
        }

        /**
         * Setter method for instance variable {@link #docName}.
         *
         * @param _docName value for instance variable {@link #docName}
         * @return the data bean
         */
        public AbstractDataBean setDocName(final String _docName)
        {
            docName = _docName;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #docContactName}.
         *
         * @return value of instance variable {@link #docContactName}
         */
        public String getDocContactName()
        {
            return docContactName == null ? "" : docContactName;
        }

        /**
         * Setter method for instance variable {@link #docContactName}.
         *
         * @param _docContactName value for instance variable
         *            {@link #docContactName}
         * @return the data bean
         */
        public AbstractDataBean setDocContactName(final String _docContactName)
        {
            docContactName = _docContactName;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #rate}.
         *
         * @return value of instance variable {@link #rate}
         */
        public Object[] getRate()
        {
            return rate;
        }

        /**
         * Setter method for instance variable {@link #rate}.
         *
         * @param _rate value for instance variable {@link #rate}
         * @return the data bean
         */
        public AbstractDataBean setRate(final Object[] _rate)
        {
            rate = _rate;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #docRevision}.
         *
         * @return value of instance variable {@link #docRevision}
         */
        public String getDocRevision()
        {
            return docRevision;
        }

        /**
         * Setter method for instance variable {@link #docRevision}.
         *
         * @param _docRevision value for instance variable {@link #docRevision}
         * @return the data bean
         */
        public AbstractDataBean setDocRevision(final String _docRevision)
        {
            docRevision = _docRevision;
            return this;
        }

        /**
         * Method to obtains totals of the payments relations to document.
         *
         * @throws EFapsException on error
         */
        protected void evalPayments()
            throws EFapsException
        {
            if (getDocInst().isValid()) {
                final DocPaymentInfo docPayInfo = new DocPaymentInfo(getDocInst());
                final Boolean perpay = getProperties().containsKey("PaymentPerPayment")
                                ? BooleanUtils.toBoolean(getProperties().getProperty("PaymentPerPayment"))
                                : null;

                if (getReportDate().isBefore(new DateTime().withTimeAtStartOfDay())) {
                    docPayInfo.getPayPos().removeIf(p -> p.getDate().isAfter(getReportDate()));
                }
                payments.put(Currency.getBaseCurrency().getId(),
                                docPayInfo.getPaidInCurrency(Currency.getBaseCurrency(), perpay).abs());
                payments.put(docPayInfo.getRateCurrencyInstance().getId(),
                                docPayInfo.getPaidInCurrency(docPayInfo.getRateCurrencyInstance(), perpay).abs());
            }
        }

        /**
         * Getter method for the instance variable {@link #docStatus}.
         *
         * @return value of instance variable {@link #docStatus}
         */
        public String getDocStatus()
        {
            return docStatus;
        }

        /**
         * Setter method for instance variable {@link #docStatus}.
         *
         * @param _docStatus value for instance variable {@link #docStatus}
         * @return the data bean
         */
        public AbstractDataBean setDocStatus(final String _docStatus)
        {
            docStatus = _docStatus;
            return this;
        }

        /**
         * Gets the condition.
         *
         * @return the condition
         */
        public String getCondition()
        {
            if (condition == null) {
                try {
                    final QueryBuilder queryBldr = new QueryBuilder(CISales.ChannelCondition2DocumentAbstract);
                    queryBldr.addWhereAttrEqValue(CISales.Channel2DocumentAbstract.ToAbstractLink, getDocInst());
                    final CachedMultiPrintQuery multi = queryBldr.getCachedPrint4Request();
                    final SelectBuilder selName = SelectBuilder.get()
                                    .linkto(CISales.Channel2DocumentAbstract.FromAbstractLink)
                                    .attribute(CISales.ChannelAbstract.Name);
                    multi.addSelect(selName);
                    multi.execute();
                    while (multi.next()) {
                        if (condition != null && !condition.isEmpty()) {
                            condition = condition + ", ";
                        } else {
                            condition = "";
                        }
                        condition = condition + multi.getSelect(selName);
                    }
                } catch (final EFapsException e) {
                    LOG.error("Catched", e);
                }
                if (condition == null) {
                    condition = "";
                }
            }
            return condition;
        }

        /**
         * Sets the condition.
         *
         * @param _condition the condition
         * @return the data bean
         */
        public AbstractDataBean setCondition(final String _condition)
        {
            condition = _condition;
            return this;
        }

        /**
         * Gets the assigned.
         *
         * @return the assigned
         * @throws EFapsException on error
         */
        public String getAssigned()
            throws EFapsException
        {
            final String ret;
            if (InstanceUtils.isValid(getContactInst())) {
                ret = Employee.getEmployeeAssigned2Contact(new Parameter(), getContactInst());
            } else {
                ret = "";
            }
            return ret;
        }

        /**
         * Gets the contact inst.
         *
         * @return the contact inst
         */
        public Instance getContactInst()
        {
            return contactInst;
        }

        /**
         * Sets the contact inst.
         *
         * @param _contactInst the contact inst
         * @return the data bean
         */
        public AbstractDataBean setContactInst(final Instance _contactInst)
        {
            contactInst = _contactInst;
            return this;
        }

        /**
         * Gets the swapInfo.
         *
         * @return the swapInfo
         */
        public String getSwapInfo()
        {
            return swapInfo;
        }

        /**
         * Sets the swapInfo.
         *
         * @param _swapInfo the new swapInfo
         * @return the data bean
         */
        public AbstractDataBean setSwapInfo(final String _swapInfo)
        {
            swapInfo = _swapInfo;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #reportDate}.
         *
         * @return value of instance variable {@link #reportDate}
         */
        public DateTime getReportDate()
        {
            return reportDate;
        }

        /**
         * Setter method for instance variable {@link #reportDate}.
         *
         * @param _reportDate value for instance variable {@link #reportDate}
         * @return the data bean
         */
        public AbstractDataBean setReportDate(final DateTime _reportDate)
        {
            reportDate = _reportDate;
            return this;
        }
    }

}
