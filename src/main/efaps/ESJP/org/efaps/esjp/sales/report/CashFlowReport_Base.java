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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.EnumUtils;
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
import org.efaps.esjp.common.properties.PropertiesUtil;
import org.efaps.esjp.erp.AbstractGroupedByDate;
import org.efaps.esjp.erp.AbstractGroupedByDate_Base.DateGroup;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.sales.cashflow.CashFlowCategory;
import org.efaps.esjp.sales.cashflow.CashFlowGroup;
import org.efaps.esjp.sales.cashflow.ICashFlowGroup;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
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
                    final DateTime date = multi2.<DateTime>getAttribute(CISales.TransactionAbstract.Date);
                    BigDecimal amount = multi2.<BigDecimal>getAttribute(CISales.TransactionAbstract.Amount);
                    final Instance payInst = multi2.<Instance>getSelect(selPaymentInst);

                    final boolean out = multi2.getCurrentInstance().getType().isKindOf(CISales.TransactionOutbound
                                    .getType());

                    if (out) {
                        amount = amount.negate();
                    }

                    final String dateStr = "";
                }

                addProjection4Documents(_parameter, beans);

                final ComparatorChain<DataBean> chain = new ComparatorChain<>();
                chain.addComparator(new Comparator<DataBean>() {
                        @Override
                        public int compare(final DataBean _o1,
                                           final DataBean _o2)
                        {
                            return _o1.getGroup().getWeight().compareTo(_o2.getGroup().getWeight());
                        }
                    }
                );
                chain.addComparator(new Comparator<DataBean>()
                {
                    @Override
                    public int compare(final DataBean _o1,
                                       final DataBean _o2)
                    {
                        int w1 = 0;
                        int w2 = 0;
                        try {
                            w1 = _o1.getCategory().getWeight();
                            w2 = _o2.getCategory().getWeight();
                        } catch (final EFapsException e) {
                            LOG.error("Catched", e);
                        }
                        return Integer.compare(w1, w2);
                    }
                });
                if (isShowContact(_parameter)) {
                    chain.addComparator(new Comparator<DataBean>()
                    {

                        @Override
                        public int compare(final DataBean _o1,
                                           final DataBean _o2)
                        {
                            return _o1.getContact().compareTo(_o2.getContact());
                        }
                    });
                }
                if (isShowDocTypes(_parameter)) {
                    chain.addComparator(new Comparator<DataBean>()
                    {

                        @Override
                        public int compare(final DataBean _o1,
                                           final DataBean _o2)
                        {
                            return _o1.getDocType().compareTo(_o2.getDocType());
                        }
                    });
                }
                chain.addComparator(new Comparator<DataBean>()
                {
                    @Override
                    public int compare(final DataBean _o1,
                                       final DataBean _o2)
                    {
                        return _o1.getDateGroup().compareTo(_o2.getDateGroup());
                    }
                });
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
            final boolean contact = isShowContact(_parameter);
            for (final CashFlowGroup group : CashFlowGroup.values()) {
                final Properties inProps = PropertiesUtil.getProperties4Prefix(props, "Projection." + group.name());

                final Map<Integer, String> typesMap = analyseProperty(_parameter, inProps, "Type");
                final Map<String, Set<String>> attr2types = new HashMap<>();
                for (final String type : typesMap.values()) {
                    final String attr = props.getProperty(type + ".FilterDate", CISales.DocumentAbstract.Date.name);
                    final Set<String> types;
                    if (attr2types.containsKey(attr)) {
                        types = attr2types.get(attr);
                    } else {
                        types = new HashSet<>();
                        attr2types.put(attr, types);
                    }
                    types.add(type);
                }
                final DateGroup dateGroup = getDateGroup(_parameter);
                final AbstractGroupedByDate groupedByDate = new AbstractGroupedByDate()
                {
                };
                final DateTimeFormatter dateTimeFormatter = groupedByDate.getDateTimeFormatter(DateGroup.MONTH);
                for (final Entry<String, Set<String>> entry : attr2types.entrySet()) {
                    final Properties propsTmp = new Properties(props);
                    final String formatStr = "%02d";
                    final int i = 1;
                    for (final String type : entry.getValue()) {
                        propsTmp.put("Type" + String.format(formatStr, i), type);
                    }
                    final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter, propsTmp);
                    queryBldr.addWhereAttrGreaterValue(entry.getKey(), start.withTimeAtStartOfDay().minusMinutes(1));
                    queryBldr.addWhereAttrLessValue(entry.getKey(), end.withTimeAtStartOfDay().plusDays(1));
                    final MultiPrintQuery multi = queryBldr.getPrint();
                    final SelectBuilder selContactName = SelectBuilder.get().linkto(CISales.DocumentSumAbstract.Contact)
                                    .attribute(CIContacts.Contact.Name);
                    if (contact) {
                        multi.addSelect(selContactName);
                    }
                    multi.addAttribute(entry.getKey());
                    multi.addAttribute(CISales.DocumentSumAbstract.RateCrossTotal,
                                    CISales.DocumentSumAbstract.RateNetTotal);
                    multi.execute();
                    while (multi.next()) {
                        final BigDecimal amount;
                        if ("NET".equals(props.getProperty(multi.getCurrentInstance().getType().getName() + ".Total",
                                        "NET"))) {
                            amount = multi.getAttribute(CISales.DocumentSumAbstract.RateNetTotal);
                        } else {
                            amount = multi.getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
                        }
                        final DateTime date = multi.getAttribute(entry.getKey());
                        final String partial = groupedByDate.getPartial(date, dateGroup).toString(dateTimeFormatter);
                        _beans.add(new DataBean()
                                        .setGroup(group)
                                        .setInstance(multi.getCurrentInstance())
                                        .setDateGroup(partial)
                                        .setAmount(amount)
                                        .setContact(contact ? multi.getSelect(selContactName) : null));
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
        protected void addProjection(final Parameter _parameter)
            throws EFapsException
        {
            for (int i = 0; i < this.projection; i++) {
                DateTime start = new DateTime();
                DateTime end = new DateTime();
                DateTime date = new DateTime();
                switch (this.interval) {
                    case MONTH:
                        start = new DateTime().withTimeAtStartOfDay().withDayOfMonth(1).plusMonths(i + 1)
                                        .minusSeconds(1);
                        end = new DateTime().withTimeAtStartOfDay().withDayOfMonth(1).plusMonths(i + 2);
                        date = new DateTime().withTimeAtStartOfDay().withDayOfMonth(1).plusMonths(i + 1);
                        break;
                    case WEEK:
                        start = new DateTime().withTimeAtStartOfDay().withDayOfWeek(1).plusWeeks(i + 1)
                                        .minusSeconds(1);
                        end = new DateTime().withTimeAtStartOfDay().withDayOfWeek(1).plusWeeks(i + 2);
                        date = end.minusHours(1);
                    default:
                        break;
                }
                final QueryBuilder queryBldr = new QueryBuilder(CISales.CashFlowProjected);
                queryBldr.addWhereAttrLessValue(CISales.CashFlowProjected.Date, end);
                queryBldr.addWhereAttrGreaterValue(CISales.CashFlowProjected.Date, start);
                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder sel = SelectBuilder.get().linkto(CISales.CashFlowProjected.CashFlowTypeLink)
                                .attribute(CISales.CashFlowType.Category);
                multi.addSelect(sel);
                multi.addAttribute(CISales.CashFlowProjected.Amount);
                multi.execute();
                while (multi.next()) {
                    final BigDecimal amount = multi.<BigDecimal>getAttribute(CISales.CashFlowProjected.Amount);
                    final ICashFlowCategory cat = multi.<ICashFlowCategory>getSelect(sel);
                    final Group group = getGroup(_parameter, amount.signum() < 0, null, multi.getCurrentInstance());
                    final String dateStr = getDateStr(_parameter, date);
                    group.add(_parameter, amount.signum() < 0, cat, dateStr, amount);
                }
            }
        }
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
                                String.class);
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
            return this.filteredReport;
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
            return this.instance;
        }

        /**
         * Sets the instance.
         *
         * @param _instance the instance
         * @return the data bean
         */
        public DataBean setInstance(final Instance _instance)
        {
            this.instance = _instance;
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
            return this.group;
        }

        /**
         * Sets the group.
         *
         * @param _group the group
         * @return the data bean
         */
        public DataBean setGroup(final ICashFlowGroup _group)
        {
            this.group = _group;
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
            if (this.category == null) {
                final Properties props = Sales.CASHFLOWREPORT_CONFIG.get();
                final String catStr = props.getProperty(getInstance().getType().getName() + ".Category",
                                CashFlowCategory.NONE.name());
                this.category = EnumUtils.getEnum(CashFlowCategory.class, catStr);
            }
            return this.category;
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
            return this.dateGroup;
        }

        /**
         * Sets the date group.
         *
         * @param _dateGroup the date group
         * @return the data bean
         */
        public DataBean setDateGroup(final String _dateGroup)
        {
            this.dateGroup = _dateGroup;
            return this;
        }

        /**
         * Gets the amount.
         *
         * @return the amount
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
         * @param _contact the new contact
         * @return the data bean
         */
        public DataBean setContact(final String _contact)
        {
            this.contact = _contact;
            return this;
        }
    }
}
