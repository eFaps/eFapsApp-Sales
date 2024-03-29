/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
 */
package org.efaps.esjp.sales.report;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.EnumUtils;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
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
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.common.properties.PropertiesUtil;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.AbstractGroupedByDate;
import org.efaps.esjp.erp.AbstractGroupedByDate_Base.DateGroup;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.sales.Calculator;
import org.efaps.esjp.sales.ICalculatorConfig;
import org.efaps.esjp.sales.cashflow.CashFlowCategory;
import org.efaps.esjp.sales.cashflow.CashFlowGroup;
import org.efaps.esjp.sales.cashflow.ICashFlowGroup;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.efaps.util.UUIDUtil;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabMeasureBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabRowGroupBuilder;
import net.sf.dynamicreports.report.constant.Calculation;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * Report for CashFlow.
 *
 * @author The eFaps Team
 */
@EFapsUUID("67b5c710-4e36-4eb2-95c0-8f04b15500ec")
@EFapsApplication("eFapsApp-Sales")
public abstract class CashFlowReport_Base
    extends FilteredReport
{

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(CashFlowReport.class);

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

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return the report class
     * @throws EFapsException on error
     */
    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new DynamicCashFlowReport(this);
    }

    /**
     * Report class.
     */
    public static class DynamicCashFlowReport
        extends AbstractDynamicReport
    {

        /** The filtered report. */
        private final CashFlowReport_Base filteredReport;

        /**
         * Instantiates a new dynamic cash flow report.
         *
         * @param _filteredReport the filtered report
         */
        public DynamicCashFlowReport(final CashFlowReport_Base _filteredReport)
        {
            filteredReport = _filteredReport;
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
                    LOG.error("Catched JRException", e);
                }
            } else {
                final List<DataBean> beans = new ArrayList<>();
                final Map<Instance, Instance> inst2inst = new HashMap<>();
                final QueryBuilder queryBldr = new QueryBuilder(CISales.Payment);
                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder selCreateInst = SelectBuilder.get().linkto(CISales.Payment.CreateDocument)
                                .instance();
                multi.addSelect(selCreateInst);
                multi.execute();
                while (multi.next()) {
                    final Instance payInst = multi.getCurrentInstance();
                    final Instance docInst = multi.getSelect(selCreateInst);
                    inst2inst.put(payInst, docInst);
                }

                final QueryBuilder transQueryBldr = new QueryBuilder(CISales.TransactionAbstract);
                add2TransactionQueryBldr(_parameter, transQueryBldr);

                final MultiPrintQuery multi2 = transQueryBldr.getPrint();
                final SelectBuilder selPaymentInst = SelectBuilder.get().linkto(CISales.TransactionAbstract.Payment)
                                .instance();
                multi2.addSelect(selPaymentInst);
                multi2.addAttribute(CISales.TransactionAbstract.Amount, CISales.TransactionAbstract.Date);
                multi2.execute();
                while (multi2.next()) {
                    multi2.<DateTime>getAttribute(CISales.TransactionAbstract.Date);
                    BigDecimal amount = multi2.<BigDecimal>getAttribute(CISales.TransactionAbstract.Amount);
                    multi2.<Instance>getSelect(selPaymentInst);

                    final boolean out = multi2.getCurrentInstance().getType().isKindOf(CISales.TransactionOutbound
                                    .getType());

                    if (out) {
                        amount = amount.negate();
                    }
                }

                addProjection4Documents(_parameter, beans);

                final ComparatorChain<DataBean> chain = new ComparatorChain<>();
                chain.addComparator((_o1,
                                     _o2) -> _o1.getGroup().getWeight().compareTo(_o2.getGroup().getWeight()));
                chain.addComparator((_o1,
                                     _o2) -> {
                    int w1 = 0;
                    int w2 = 0;
                    try {
                        w1 = _o1.getCategory().getWeight();
                        w2 = _o2.getCategory().getWeight();
                    } catch (final EFapsException e) {
                        LOG.error("Catched", e);
                    }
                    return Integer.compare(w1, w2);
                });
                if (isShowContact(_parameter)) {
                    chain.addComparator((_o1,
                                         _o2) -> _o1.getContact().compareTo(_o2.getContact()));
                }
                if (isShowDocTypes(_parameter)) {
                    chain.addComparator((_o1,
                                         _o2) -> _o1.getDocType().compareTo(_o2.getDocType()));
                }
                chain.addComparator((_o1,
                                     _o2) -> _o1.getDateGroup().compareTo(_o2.getDateGroup()));
                Collections.sort(beans, chain);
                ret = new JRBeanCollectionDataSource(beans);
                getFilteredReport().cache(_parameter, ret);
            }
            return ret;
        }

        /**
         * Adds the projection based on analysing documents.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _beans the beans
         * @throws EFapsException on error
         */
        protected void addProjection4Documents(final Parameter _parameter,
                                               final List<DataBean> _beans)
            throws EFapsException
        {
            final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);
            final DateTime start;
            final DateTime end;
            if (filter.containsKey("dateFrom")) {
                start = (DateTime) filter.get("dateFrom");
            } else {
                start = new DateTime();
            }
            if (filter.containsKey("dateTo")) {
                end = (DateTime) filter.get("dateTo");
            } else {
                end = new DateTime();
            }

            final Properties props = Sales.CASHFLOWREPORT_CONFIG.get();
            final String currencyKey = props.getProperty("Currency", "BASE");
            final CurrencyInst currencyInst;
            if ("BASE".equals(currencyKey)) {
                currencyInst = CurrencyInst.get(Currency.getBaseCurrency());
            } else if (UUIDUtil.isUUID(currencyKey)) {
                currencyInst = CurrencyInst.get(UUID.fromString(currencyKey));
            } else {
                currencyInst = CurrencyInst.find(currencyKey).orElse(CurrencyInst.get(Currency.getBaseCurrency()));
            }

            final boolean contact = isShowContact(_parameter);
            for (final CashFlowGroup group : CashFlowGroup.values()) {
                final Properties inProps = PropertiesUtil.getProperties4Prefix(props, "Projection." + group.name());

                final Map<Integer, String> typesMap = analyseProperty(_parameter, inProps, "Type");

                final Map<String, Set<String>> attr2sumTypes = new HashMap<>();
                final Map<String, Set<String>> attr2stockTypes = new HashMap<>();
                for (final String typeStr : typesMap.values()) {
                    final Type type = UUIDUtil.isUUID(typeStr) ? Type.get(UUID.fromString(typeStr)) : Type.get(typeStr);
                    final boolean isSum = type.isKindOf(CISales.DocumentSumAbstract);
                    final String attr = props.getProperty(typeStr + ".FilterDate", CISales.DocumentAbstract.Date.name);
                    final Set<String> types;
                    if (isSum) {
                        if (attr2sumTypes.containsKey(attr)) {
                            types = attr2sumTypes.get(attr);
                        } else {
                            types = new HashSet<>();
                            attr2sumTypes.put(attr, types);
                        }
                    } else {
                        if (attr2stockTypes.containsKey(attr)) {
                            types = attr2stockTypes.get(attr);
                        } else {
                            types = new HashSet<>();
                            attr2stockTypes.put(attr, types);
                        }
                    }
                    types.add(typeStr);
                }
                final DateGroup dateGroup = getDateGroup(_parameter);
                final AbstractGroupedByDate groupedByDate = new AbstractGroupedByDate()
                {
                };
                final DateTimeFormatter dateTimeFormatter = groupedByDate.getDateTimeFormatter(DateGroup.MONTH);

                for (final Entry<String, Set<String>> entry : attr2sumTypes.entrySet()) {
                    final Properties propsTmp = new Properties(props);
                    inProps.stringPropertyNames().forEach(key -> {
                        if (key.startsWith("Status")) {
                            propsTmp.put(key, inProps.get(key));
                        }
                    });

                    final String formatStr = "%02d";
                    int i = 1;
                    for (final String type : entry.getValue()) {
                        propsTmp.put("Type" + String.format(formatStr, i), type);
                        i++;
                    }
                    LOG.debug("Props: {}", propsTmp);
                    final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter, propsTmp);

                    queryBldr.addWhereAttrGreaterValue(entry.getKey(), start.withTimeAtStartOfDay().minusMinutes(1));
                    queryBldr.addWhereAttrLessValue(entry.getKey(), end.withTimeAtStartOfDay().plusDays(1));
                    final MultiPrintQuery multi = queryBldr.getPrint();
                    final SelectBuilder selContactInst = SelectBuilder.get().linkto(CISales.DocumentSumAbstract.Contact)
                                    .instance();
                    multi.addSelect(selContactInst);
                    final SelectBuilder selContactName = SelectBuilder.get().linkto(CISales.DocumentSumAbstract.Contact)
                                    .attribute(CIContacts.Contact.Name);
                    if (contact) {
                        multi.addSelect(selContactName);
                    }
                    multi.addAttribute(entry.getKey());
                    multi.addAttribute(CISales.DocumentSumAbstract.Date,
                                    CISales.DocumentSumAbstract.CrossTotal,
                                    CISales.DocumentSumAbstract.RateCrossTotal,
                                    CISales.DocumentSumAbstract.NetTotal,
                                    CISales.DocumentSumAbstract.RateNetTotal,
                                    CISales.DocumentSumAbstract.RateCurrencyId);
                    multi.execute();
                    while (multi.next()) {
                        final Long rateCurrencyId = multi.getAttribute(CISales.DocumentSumAbstract.RateCurrencyId);
                        final boolean isNet = "NET".equals(props
                                        .getProperty(multi.getCurrentInstance().getType().getName() + ".Total", "NET"));
                        final BigDecimal amount;
                        // no currency conversion needed if base
                        if (Currency.getBaseCurrency().equals(currencyInst.getInstance())) {
                            amount = isNet ? multi.getAttribute(CISales.DocumentSumAbstract.NetTotal)
                                            : multi.getAttribute(CISales.DocumentSumAbstract.CrossTotal);

                        } else if (rateCurrencyId.equals(currencyInst.getInstance().getId())) {
                            amount = isNet ? multi.getAttribute(CISales.DocumentSumAbstract.RateNetTotal)
                                            : multi.getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
                        } else {
                            final BigDecimal tmpAmount = isNet
                                            ? multi.getAttribute(CISales.DocumentSumAbstract.NetTotal)
                                            : multi.getAttribute(CISales.DocumentSumAbstract.CrossTotal);
                            final Instance toCurrencyInstance = CurrencyInst.get(rateCurrencyId).getInstance();
                            final DateTime date = multi.getAttribute(CISales.DocumentSumAbstract.Date);
                            final java.time.LocalDate localDate = java.time.LocalDate.of(date.getYear(),
                                            date.getMonthOfYear(), date.getDayOfMonth());
                            amount = Currency.convert(_parameter, tmpAmount, Currency.getBaseCurrency(),
                                            toCurrencyInstance, multi.getCurrentInstance().getType().getName(),
                                            localDate);
                        }
                        final DateTime date = multi.getAttribute(entry.getKey());
                        final String partial = groupedByDate.getPartial(date, dateGroup).toString(dateTimeFormatter);
                        _beans.add(getDataBean()
                                        .setGroup(group)
                                        .setInstance(multi.getCurrentInstance())
                                        .setDateGroup(partial)
                                        .setAmount(amount)
                                        .setContact(contact ? multi.getSelect(selContactName) : null)
                                        .setContactInstance(multi.getSelect(selContactInst)));
                    }
                }

                for (final Entry<String, Set<String>> entry : attr2stockTypes.entrySet()) {
                    final Properties propsTmp = new Properties(props);
                    inProps.stringPropertyNames().forEach(key -> {
                        if (key.startsWith("Status")) {
                            propsTmp.put(key, inProps.get(key));
                        }
                    });

                    final String formatStr = "%02d";
                    int i = 1;
                    for (final String type : entry.getValue()) {
                        propsTmp.put("Type" + String.format(formatStr, i), type);
                        i++;
                    }
                    LOG.debug("Props: {}", propsTmp);
                    final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter, propsTmp);

                    queryBldr.addWhereAttrGreaterValue(entry.getKey(), start.withTimeAtStartOfDay().minusMinutes(1));
                    queryBldr.addWhereAttrLessValue(entry.getKey(), end.withTimeAtStartOfDay().plusDays(1));
                    final MultiPrintQuery multi = queryBldr.getPrint();
                    final SelectBuilder selContactInst = SelectBuilder.get()
                                    .linkto(CISales.DocumentStockAbstract.Contact).instance();
                    multi.addSelect(selContactInst);
                    final SelectBuilder selContactName = SelectBuilder.get()
                                    .linkto(CISales.DocumentStockAbstract.Contact)
                                    .attribute(CIContacts.Contact.Name);
                    if (contact) {
                        multi.addSelect(selContactName);
                    }
                    multi.addAttribute(entry.getKey());
                    multi.execute();
                    while (multi.next()) {
                        final DateTime date = multi.getAttribute(entry.getKey());
                        final String partial = groupedByDate.getPartial(date, dateGroup).toString(dateTimeFormatter);
                        _beans.add(getDataBean()
                                        .setGroup(group)
                                        .setInstance(multi.getCurrentInstance())
                                        .setDateGroup(partial)
                                        .setContact(contact ? multi.getSelect(selContactName) : null)
                                        .setContactInstance(multi.getSelect(selContactInst)));
                    }
                }
            }
        }

        /**
         * Add2 transaction query bldr.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _queryBldr the query bldr
         * @throws EFapsException on error
         */
        protected void add2TransactionQueryBldr(final Parameter _parameter,
                                                final QueryBuilder _queryBldr)
            throws EFapsException
        {
            final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);
            final DateTime start;
            final DateTime end;
            if (filter.containsKey("dateFrom")) {
                start = (DateTime) filter.get("dateFrom");
            } else {
                start = new DateTime();
            }
            if (filter.containsKey("dateTo")) {
                end = (DateTime) filter.get("dateTo");
            } else {
                end = new DateTime();
            }
            _queryBldr.addWhereAttrGreaterValue(CISales.TransactionAbstract.Date, start.withTimeAtStartOfDay()
                            .minusMinutes(1));
            _queryBldr.addWhereAttrLessValue(CISales.TransactionAbstract.Date, end.withTimeAtStartOfDay().plusDays(1));
        }

        /**
         * protected void addProjection(final Parameter _parameter) throws
         * EFapsException { for (int i = 0; i < this.projection; i++) { DateTime
         * start = new DateTime(); DateTime end = new DateTime(); DateTime date
         * = new DateTime(); switch (this.interval) { case MONTH: start = new
         * DateTime().withTimeAtStartOfDay().withDayOfMonth(1).plusMonths(i + 1)
         * .minusSeconds(1); end = new
         * DateTime().withTimeAtStartOfDay().withDayOfMonth(1).plusMonths(i +
         * 2); date = new
         * DateTime().withTimeAtStartOfDay().withDayOfMonth(1).plusMonths(i +
         * 1); break; case WEEK: start = new
         * DateTime().withTimeAtStartOfDay().withDayOfWeek(1).plusWeeks(i + 1)
         * .minusSeconds(1); end = new
         * DateTime().withTimeAtStartOfDay().withDayOfWeek(1).plusWeeks(i + 2);
         * date = end.minusHours(1); default: break; } final QueryBuilder
         * queryBldr = new QueryBuilder(CISales.CashFlowProjected);
         * queryBldr.addWhereAttrLessValue(CISales.CashFlowProjected.Date, end);
         * queryBldr.addWhereAttrGreaterValue(CISales.CashFlowProjected.Date,
         * start); final MultiPrintQuery multi = queryBldr.getPrint(); final
         * SelectBuilder sel =
         * SelectBuilder.get().linkto(CISales.CashFlowProjected.CashFlowTypeLink)
         * .attribute(CISales.CashFlowType.Category); multi.addSelect(sel);
         * multi.addAttribute(CISales.CashFlowProjected.Amount);
         * multi.execute(); while (multi.next()) { final BigDecimal amount =
         * multi.<BigDecimal>getAttribute(CISales.CashFlowProjected.Amount);
         * final ICashFlowCategory cat =
         * multi.<ICashFlowCategory>getSelect(sel); final Group group =
         * getGroup(_parameter, amount.signum() < 0, null,
         * multi.getCurrentInstance()); final String dateStr =
         * getDateStr(_parameter, date); group.add(_parameter, amount.signum() <
         * 0, cat, dateStr, amount); } } }
         */

        @Override
        protected void addColumnDefinition(final Parameter _parameter,
                                           final JasperReportBuilder _builder)
            throws EFapsException
        {
            final CrosstabBuilder crosstab = DynamicReports.ctab.crosstab();
            final List<CrosstabRowGroupBuilder<?>> rowGroups = new ArrayList<>();
            final CrosstabRowGroupBuilder<String> groupGroup = DynamicReports.ctab.rowGroup("groupLabel", String.class);
            final CrosstabRowGroupBuilder<String> categoryCategory = DynamicReports.ctab.rowGroup("categoryLabel",
                            String.class);
            Collections.addAll(rowGroups, groupGroup, categoryCategory);

            if (isShowContact(_parameter)) {
                final CrosstabRowGroupBuilder<String> contactGroup = DynamicReports.ctab.rowGroup("contact",
                                String.class).setHeaderWidth(200);
                rowGroups.add(contactGroup);
            }

            if (isShowDocTypes(_parameter)) {
                final CrosstabRowGroupBuilder<String> docTypeGroup = DynamicReports.ctab.rowGroup("docType",
                                String.class);
                rowGroups.add(docTypeGroup);
            }

            final CrosstabColumnGroupBuilder<String> dateGroup = DynamicReports.ctab.columnGroup("dateGroup",
                            String.class).setShowTotal(false);
            final CrosstabMeasureBuilder<BigDecimal> amountMeasure = DynamicReports.ctab.measure("amount",
                            BigDecimal.class, Calculation.SUM);

            crosstab.headerCell(DynamicReports.cmp.text(getFilteredReport().getDBProperty("HeaderCell"))
                            .setStyle(DynamicReports.stl.style().setBold(true)))
                            .rowGroups(rowGroups.toArray(new CrosstabRowGroupBuilder[rowGroups.size()]))
                            .columnGroups(dateGroup)
                            .measures(amountMeasure)
                            .setDataPreSorted(true);
            _builder.summary(crosstab);
        }

        /**
         * Checks if is show doc types.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return true, if is show doc types
         * @throws EFapsException on error
         */
        protected boolean isShowContact(final Parameter _parameter)
            throws EFapsException
        {
            final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);
            return BooleanUtils.isTrue((Boolean) filter.get("contactGroup"));
        }

        /**
         * Checks if is show doc types.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return true, if is show doc types
         * @throws EFapsException on error
         */
        protected boolean isShowDocTypes(final Parameter _parameter)
            throws EFapsException
        {
            final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);
            return BooleanUtils.isTrue((Boolean) filter.get("typeGroup"));
        }

        /**
         * Gets the date group.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the date group
         * @throws EFapsException on error
         */
        protected DateGroup getDateGroup(final Parameter _parameter)
            throws EFapsException
        {
            final DateGroup dateGroup;
            final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);
            if (filter.containsKey("dateGroup") && filter.get("dateGroup") != null) {
                dateGroup = (AbstractGroupedByDate.DateGroup) ((EnumFilterValue) filter.get("dateGroup")).getObject();
            } else {
                dateGroup = DocumentSumGroupedByDate_Base.DateGroup.MONTH;
            }
            return dateGroup;
        }

        /**
         * Gets the filtered report.
         *
         * @return the filtered report
         */
        public CashFlowReport_Base getFilteredReport()
        {
            return filteredReport;
        }

        protected DataBean getDataBean()
        {
            return new DataBean();
        }
    }

    /**
     * The Class DataBean.
     *
     */
    public static class DataBean
    {

        /** The instance. */
        private Instance instance;

        private Instance contactInstance;

        /** The group. */
        private ICashFlowGroup group;

        /** The category. */
        private CashFlowCategory category;

        /** The date group. */
        private String dateGroup;

        /** The amount. */
        private BigDecimal amount;

        /** The contact. */
        private String contact;

        /**
         * Gets the instance.
         *
         * @return the instance
         */
        public Instance getInstance()
        {
            return instance;
        }

        /**
         * Sets the instance.
         *
         * @param _instance the instance
         * @return the data bean
         */
        public DataBean setInstance(final Instance _instance)
        {
            instance = _instance;
            return this;
        }

        /**
         * Gets the group label.
         *
         * @return the group label
         */
        public String getGroupLabel()
        {
            return DBProperties.getProperty(CashFlowReport.class.getName() + ".Group." + getGroup().getLabelKey());
        }

        /**
         * Gets the group.
         *
         * @return the group
         */
        public ICashFlowGroup getGroup()
        {
            return group;
        }

        /**
         * Sets the group.
         *
         * @param _group the group
         * @return the data bean
         */
        public DataBean setGroup(final ICashFlowGroup _group)
        {
            group = _group;
            return this;
        }

        /**
         * Gets the category.
         *
         * @return the category
         * @throws EFapsException on error
         */
        public CashFlowCategory getCategory()
            throws EFapsException
        {
            if (category == null) {
                final Properties props = Sales.CASHFLOWREPORT_CONFIG.get();
                final String catStr = props.getProperty(getInstance().getType().getName() + ".Category",
                                CashFlowCategory.NONE.name());
                category = EnumUtils.getEnum(CashFlowCategory.class, catStr);
            }
            return category;
        }

        /**
         * Gets the category label.
         *
         * @return the category label
         * @throws EFapsException on error
         */
        public String getCategoryLabel()
            throws EFapsException
        {
            return DBProperties.getProperty(CashFlowReport.class.getName() + ".Category." + getGroup().getLabelKey()
                            + "." + getCategory().getLabelKey());
        }

        /**
         * Gets the date group.
         *
         * @return the date group
         */
        public String getDocType()
        {
            return getInstance().getType().getLabel();
        }

        /**
         * Gets the date group.
         *
         * @return the date group
         */
        public String getDateGroup()
        {
            return dateGroup;
        }

        /**
         * Sets the date group.
         *
         * @param _dateGroup the date group
         * @return the data bean
         */
        public DataBean setDateGroup(final String _dateGroup)
        {
            dateGroup = _dateGroup;
            return this;
        }

        /**
         * Gets the amount.
         *
         * @return the amount
         */
        public BigDecimal getAmount()
        {
            if (amount == null && InstanceUtils.isKindOf(instance, CISales.DocumentStockAbstract)) {
                amount = evalAmount4StockDocuments();
            }
            return amount;
        }

        protected BigDecimal evalAmount4StockDocuments()
        {
            BigDecimal ret = BigDecimal.ZERO;
            try {
                final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionAbstract);
                queryBldr.addWhereAttrEqValue(CISales.PositionAbstract.DocumentAbstractLink, instance);
                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder prodSel = SelectBuilder.get().linkto(CISales.PositionAbstract.Product)
                                .instance();
                multi.addSelect(prodSel);
                multi.addAttribute(CISales.PositionAbstract.Quantity);
                multi.execute();
                final List<Calculator> calculators = new ArrayList<>();
                final ICalculatorConfig calcConfig = new ICalculatorConfig()
                {

                    @Override
                    public String getSysConfKey4Doc(final Parameter _parameter)
                        throws EFapsException
                    {
                        return "Sales_Invoice";
                    }

                    @Override
                    public String getSysConfKey4Pos(final Parameter _parameter)
                        throws EFapsException
                    {
                        return "Sales_InvoicePosition";
                    }

                    @Override
                    public boolean priceFromUIisNet(final Parameter _parameter)
                        throws EFapsException
                    {
                        return false;
                    }
                };
                final Parameter parameter = ParameterUtil.instance();
                while (multi.next()) {
                    final Instance prodInst = multi.getSelect(prodSel);
                    final BigDecimal quantity = multi.getAttribute(CISales.PositionAbstract.Quantity);
                    final Calculator calc = new Calculator(parameter, null, prodInst, quantity, null,
                                    BigDecimal.ZERO, true,
                                    calcConfig);
                    calculators.add(calc);
                }
                ret = Calculator.getNetTotal(parameter, calculators);
            } catch (final EFapsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return ret;
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
         * Gets the contact.
         *
         * @return the contact
         */
        public String getContact()
        {
            return contact;
        }

        /**
         * Sets the contact.
         *
         * @param _contact the new contact
         * @return the data bean
         */
        public DataBean setContact(final String _contact)
        {
            contact = _contact;
            return this;
        }

        public Instance getContactInstance()
        {
            return contactInstance;
        }

        public DataBean setContactInstance(final Instance contactInstance)
        {
            this.contactInstance = contactInstance;
            return this;
        }
    }
}
