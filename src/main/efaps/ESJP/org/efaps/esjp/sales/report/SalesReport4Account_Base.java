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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.lang3.BooleanUtils;
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
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport_Base;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeDate;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeMonth;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeYear;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.humanresource.Employee;
import org.efaps.esjp.sales.Swap;
import org.efaps.esjp.sales.Swap_Base.SwapInfo;
import org.efaps.esjp.sales.payment.DocPaymentInfo;
import org.efaps.esjp.sales.report.SalesReport4Account.DynReport4Account;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.ui.wicket.models.EmbeddedLink;
import org.efaps.util.EFapsException;
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
     * Used to distinguish between incoming and outgoing report.
     */
    public enum ReportKey
    {
        /** Incoming. */
        IN,
        /** Outgoing. */
        OUT,
        /** Contact related. */
        CONTACT;
    }

    /**
     * Used to distinguish between incoming and outgoing report.
     */
    public enum FilterDate
    {
        /** The date. */
        DATE,
        /** The created. */
        CREATED,
        /** The duedate. */
        DUEDATE;
    }

    /**
     * The Enum Grouping.
     *
     */
    public enum GroupBy
    {
        /** Includes a group on yearl level. */
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
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SalesReport4Account.class);

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
        return new DynReport4Account(this);
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
                    if (ReportKey.CONTACT.equals(getReportKey())) {
                        ret.remove(GroupBy.ASSIGNED);
                        ret.remove(GroupBy.CONTACT);
                    }
                } catch (final EFapsException e) {
                    SalesReport4Account_Base.LOG.error("Catched", e);
                }
                return ret;
            }
        }.setClassName(_className);
        ret.setObject(new ArrayList());
        return ret;
    }

    /**
     * Checks if is show assigned.
     *
     * @return true, if is show assigned
     * @throws EFapsException on error
     */
    protected boolean isShowAssigned()
        throws EFapsException
    {
        return Sales.REPORT_SALES4ACCOUNTIN_ASSIGENED.get() && getReportKey().equals(ReportKey.IN)
                        || Sales.REPORT_SALES4ACCOUNTOUT_ASSIGENED.get() && getReportKey().equals(ReportKey.OUT);
    }

    /**
     * Checks if is show condition.
     *
     * @return true, if is show condition
     * @throws EFapsException on error
     */
    protected boolean isShowCondition()
        throws EFapsException
    {
        return Sales.CHACTIVATESALESCOND.get() && getReportKey().equals(ReportKey.IN) || Sales.CHACTIVATEPURCHASECOND
                        .get() && getReportKey().equals(ReportKey.OUT) || (Sales.CHACTIVATEPURCHASECOND.get()
                                        || Sales.CHACTIVATESALESCOND.get()) && getReportKey().equals(ReportKey.CONTACT);
    }

    /**
     * Report class.
     */
    public abstract static class AbstractDynReport4Account
        extends AbstractDynamicReport
    {

        /**
         * variable to report.
         */
        private final SalesReport4Account_Base filteredReport;

        /**
         * Instantiates a new report4 account.
         *
         * @param _report4Account class used
         */
        public AbstractDynReport4Account(final SalesReport4Account_Base _report4Account)
        {
            this.filteredReport = _report4Account;
        }

        @Override
        protected String getTitle(final Parameter _parameter)
        {
            final String ret;
            switch(getFilteredReport().getReportKey()) {
                case IN:
                    ret = DBProperties.getProperty("Sales_PaymentDocument_Report4AccountMyDesk.Title");
                    break;
                case OUT:
                    ret = DBProperties.getProperty("Sales_PaymentDocumentOut_Report4AccountMyDesk.Title");
                    break;
                default:
                    ret = super.getTitle(_parameter);
                    break;
            }
            return ret;
        }

        /**
         * Gets the filtered report.
         *
         * @return the filtered report
         */
        protected SalesReport4Account_Base getFilteredReport()
        {
            return this.filteredReport;
        }

        @Override
        @SuppressWarnings({ "checkstyle:MethodLength", "unchecked" })
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
                final Map<Instance, DataBean> beans = new HashMap<>();
                final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);

                boolean offset = false;
                if (filter.containsKey("switch")) {
                    offset = (boolean) filter.get("switch");
                }
                final DateTime reportDate = getReportDate(_parameter);
                final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter, offset ? 0 : 100);
                add2QueryBuilder(_parameter, queryBldr);
                queryBldr.setCompanyDependent(isCompanyDependent(_parameter));
                final MultiPrintQuery multi = queryBldr.getPrint();
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
                if (!ReportKey.CONTACT.equals(getFilteredReport().getReportKey())) {
                    multi.addSelect(selContactInst, selContactName);
                }
                multi.execute();
                final Set<Instance> docInsts = new HashSet<>();
                while (multi.next()) {
                    docInsts.add(multi.getCurrentInstance());
                    final DataBean dataBean = getDataBean(_parameter, getFilteredReport().getReportKey())
                                .setDocInst(multi.getCurrentInstance())
                                .setDocCreated(multi.<DateTime>getAttribute(CISales.DocumentSumAbstract.Created))
                                .setDocDate(multi.<DateTime>getAttribute(CISales.DocumentSumAbstract.Date))
                                .setDocDueDate(multi.<DateTime>getAttribute(CISales.DocumentSumAbstract.DueDate))
                                .setDocName(multi.<String>getAttribute(CISales.DocumentSumAbstract.Name))
                                .setDocRevision(multi.<String>getAttribute(CISales.DocumentSumAbstract.Revision))
                                .setDocStatus(multi.<String>getSelect(selStatus))
                                .setReportDate(reportDate);

                    if (!ReportKey.CONTACT.equals(getFilteredReport().getReportKey())) {
                        dataBean.setContactInst(multi.<Instance>getSelect(selContactInst))
                                .setDocContactName(multi.<String>getSelect(selContactName));
                    }

                    if (isCurrencyBase(_parameter)) {
                        dataBean.setCurrencyBase(true)
                            .addCross(multi.<Long>getAttribute(CISales.DocumentSumAbstract.CurrencyId),
                                        multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.CrossTotal));
                    } else {
                        dataBean.setRate(multi.<Object[]>getAttribute(CISales.DocumentSumAbstract.Rate))
                            .addCross(multi.<Long>getAttribute(CISales.DocumentSumAbstract.RateCurrencyId),
                                        multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal));
                    }
                    beans.put(dataBean.getDocInst(), dataBean);
                }

                calculate4Date(_parameter, beans);

                final List<DataBean> dataSource = new ArrayList<>();
                dataSource.addAll(beans.values());
                if (isShowSwapInfo() && !ReportType.MINIMAL.equals(reportType)) {
                    final Map<Instance, Set<SwapInfo>> swapMap = Swap.getSwapInfos4Documents(_parameter, docInsts
                                    .toArray(new Instance[docInsts.size()]));
                    for (final DataBean bean : dataSource) {
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
                if (ReportType.MINIMAL.equals(reportType)) {
                    Collections.sort(dataSource, (_o1, _o2) -> _o1.getDocContactName().compareTo(
                                    _o2.getDocContactName()));
                } else {
                    final FilterDate filterDate = getFilterDate(_parameter);
                    final ComparatorChain<DataBean> chain = new ComparatorChain<>();

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
                    Collections.sort(dataSource, chain);
                }
                final Collection<Map<String, ?>> col = new ArrayList<>();

                for (final DataBean bean : dataSource) {
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

        /**
         * Calculate for date.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _beans the beans
         * @throws EFapsException on error
         */
        protected void calculate4Date(final Parameter _parameter,
                                      final Map<Instance, DataBean> _beans)
            throws EFapsException
        {
            final DateTime reportDate = getReportDate(_parameter);
            if (reportDate.isBefore(new DateTime().withTimeAtStartOfDay())) {
                // check for payments
                final QueryBuilder attrQueryBldr = getQueryBldrFromProperties(_parameter, 100);
                add2QueryBuilder(_parameter, attrQueryBldr);

                final QueryBuilder queryBldr = new QueryBuilder(CISales.Payment);
                queryBldr.addWhereAttrInQuery(CISales.Payment.CreateDocument,
                                attrQueryBldr.getAttributeQuery(CISales.DocumentAbstract.ID));
                queryBldr.addWhereAttrGreaterValue(CISales.Payment.Date, reportDate.minusMinutes(1).plusDays(1));
                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder seDocInst = SelectBuilder.get().linkto(CISales.Payment.CreateDocument).instance();
                multi.addSelect(seDocInst);
                multi.execute();
                while (multi.next()) {
                    final Instance docInst = multi.getSelect(seDocInst);
                    addDataBean(_parameter, _beans, docInst);
                }

                // check for swaps
                final QueryBuilder swapQueryBldr = new QueryBuilder(CISales.Document2Document4Swap);
                swapQueryBldr.addWhereAttrInQuery(CISales.Document2Document4Swap.FromLink,
                                attrQueryBldr.getAttributeQuery(CISales.DocumentAbstract.ID));
                swapQueryBldr.addWhereAttrGreaterValue(CISales.Document2Document4Swap.Date,
                                reportDate.minusMinutes(1).plusDays(1));
                final MultiPrintQuery swapMulti = swapQueryBldr.getPrint();
                final SelectBuilder selDocInst = SelectBuilder.get()
                                .linkto(CISales.Document2Document4Swap.FromLink).instance();
                swapMulti.addSelect(selDocInst);
                swapMulti.execute();
                while (swapMulti.next()) {
                    final Instance docInst = swapMulti.getSelect(selDocInst);
                    addDataBean(_parameter, _beans, docInst);
                }
            }
        }

        /**
         * Adds the data bean.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _beans the beans
         * @param _docInst the doc inst
         * @throws EFapsException on error
         */
        protected void addDataBean(final Parameter _parameter,
                                   final Map<Instance, DataBean> _beans,
                                   final Instance _docInst)
            throws EFapsException
        {
            final DataBean bean;
            if (!_beans.containsKey(_docInst)) {
                final PrintQuery print = new PrintQuery(_docInst);
                print.addAttribute(CISales.DocumentSumAbstract.Created, CISales.DocumentSumAbstract.Date,
                                CISales.DocumentSumAbstract.Name, CISales.DocumentSumAbstract.DueDate,
                                CISales.DocumentSumAbstract.Rate, CISales.DocumentSumAbstract.CrossTotal,
                                CISales.DocumentSumAbstract.CurrencyId, CISales.DocumentSumAbstract.RateCurrencyId,
                                CISales.DocumentSumAbstract.RateCrossTotal, CISales.DocumentSumAbstract.Revision);

                final SelectBuilder selContactInst = new SelectBuilder().linkto(CISales.DocumentSumAbstract.Contact)
                                .instance();
                final SelectBuilder selContactName = new SelectBuilder().linkto(CISales.DocumentSumAbstract.Contact)
                                .attribute(CIContacts.Contact.Name);
                final SelectBuilder selStatus = new SelectBuilder().status().label();
                print.addSelect(selStatus);
                if (!ReportKey.CONTACT.equals(getFilteredReport().getReportKey())) {
                    print.addSelect(selContactInst, selContactName);
                }
                print.execute();
                bean = getDataBean(_parameter, getFilteredReport().getReportKey())
                                .setDocInst(print.getCurrentInstance())
                                .setDocCreated(print.<DateTime>getAttribute(CISales.DocumentSumAbstract.Created))
                                .setDocDate(print.<DateTime>getAttribute(CISales.DocumentSumAbstract.Date))
                                .setDocDueDate(print.<DateTime>getAttribute(CISales.DocumentSumAbstract.DueDate))
                                .setDocName(print.<String>getAttribute(CISales.DocumentSumAbstract.Name))
                                .setDocRevision(print.<String>getAttribute(CISales.DocumentSumAbstract.Revision))
                                .setDocStatus(print.<String>getSelect(selStatus))
                                .setReportDate(getReportDate(_parameter));
                if (!ReportKey.CONTACT.equals(getFilteredReport().getReportKey())) {
                    bean.setContactInst(print.<Instance>getSelect(selContactInst))
                        .setDocContactName(print.<String>getSelect(selContactName));
                }
                if (isCurrencyBase(_parameter)) {
                    bean.setCurrencyBase(true)
                        .addCross(print.<Long>getAttribute(CISales.DocumentSumAbstract.CurrencyId),
                                        print.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.CrossTotal));
                } else {
                    bean.setRate(print.<Object[]>getAttribute(CISales.DocumentSumAbstract.Rate))
                        .addCross(print.<Long>getAttribute(CISales.DocumentSumAbstract.RateCurrencyId),
                                        print.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal));
                }
                _beans.put(_docInst, bean);
            }
        }

        /**
         * Checks if is group by assigned.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return true, if is group by assigned
         * @throws EFapsException on error
         */
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

        /**
         * Checks if is show assigned.
         *
         * @return true, if is show assigned
         * @throws EFapsException on error
         */
        protected boolean isShowSwapInfo()
            throws EFapsException
        {
            return Sales.REPORT_SALES4ACCOUNTIN_SWAPINFO.get()
                            && getFilteredReport().getReportKey().equals(ReportKey.IN)
                            || Sales.REPORT_SALES4ACCOUNTOUT_SWAPINFO.get()
                                            && getFilteredReport().getReportKey().equals(ReportKey.OUT)
                            || Sales.REPORT_SALES4ACCOUNTCONTACT_SWAPINFO.get()
                                        && getFilteredReport().getReportKey().equals(ReportKey.CONTACT);
        }

        /**
         * Checks if is show assigned.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return true, if is show assigned
         * @throws EFapsException on error
         */
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
         * @param _parameter Parameter as passed from the eFaps API
         * @param _queryBldr QueryBuilder the criteria will be added to
         * @throws EFapsException on error
         */
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

        @Override
        @SuppressWarnings("checkstyle:MethodLength")
        protected void addColumnDefinition(final Parameter _parameter,
                                           final JasperReportBuilder _builder)
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
                if (ReportType.STANDARD.equals(reportType)
                                && getFilteredReport().getReportKey().equals(ReportKey.OUT)) {
                    gridList.add(revisionColumn);
                }

                if (!selected.contains(GroupBy.CONDITION) && getFilteredReport().isShowCondition()) {
                    gridList.add(conditionColumn);
                }
                if (!selected.contains(GroupBy.CONTACT)
                                && !ReportKey.CONTACT.equals(getFilteredReport().getReportKey())) {
                    gridList.add(contactNameColumn);
                }
                if (ReportType.STANDARD.equals(reportType) && getFilteredReport().isShowAssigned()
                                && !selected.contains(GroupBy.ASSIGNED)
                                && !ReportKey.CONTACT.equals(getFilteredReport().getReportKey())) {
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
                                _builder.addSubtotalAtGroupFooter(contactGroup,  DynamicReports.sbt.sum(result));
                                break;
                            case MONTHLY:
                                _builder.addSubtotalAtGroupFooter(monthGroup,  DynamicReports.sbt.sum(crossColumn));
                                _builder.addSubtotalAtGroupFooter(monthGroup,  DynamicReports.sbt.sum(payColumn));
                                _builder.addSubtotalAtGroupFooter(monthGroup, DynamicReports.sbt.sum(result));
                                break;
                            case YEARLY:
                                _builder.addSubtotalAtGroupFooter(yearGroup,  DynamicReports.sbt.sum(crossColumn));
                                _builder.addSubtotalAtGroupFooter(yearGroup, DynamicReports.sbt.sum(payColumn));
                                _builder.addSubtotalAtGroupFooter(yearGroup, DynamicReports.sbt.sum(result));
                                break;
                            case DAILY:
                                _builder.addSubtotalAtGroupFooter(dayGroup,  DynamicReports.sbt.sum(crossColumn));
                                _builder.addSubtotalAtGroupFooter(dayGroup, DynamicReports.sbt.sum(payColumn));
                                _builder.addSubtotalAtGroupFooter(dayGroup, DynamicReports.sbt.sum(result));
                                break;
                            case DOCTYPE:
                                _builder.addSubtotalAtGroupFooter(docTypeGroup,  DynamicReports.sbt.sum(crossColumn));
                                _builder.addSubtotalAtGroupFooter(docTypeGroup, DynamicReports.sbt.sum(payColumn));
                                _builder.addSubtotalAtGroupFooter(docTypeGroup, DynamicReports.sbt.sum(result));
                                break;
                            case CONDITION:
                                _builder.addSubtotalAtGroupFooter(conditionGroup,  DynamicReports.sbt.sum(crossColumn));
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
                                dateColumn,
                                dueDateColumn,
                                nameColumn.setHorizontalTextAlignment(HorizontalTextAlignment.CENTER),
                                revisionColumn);

                if (getFilteredReport().isShowCondition()) {
                    columnList.add(conditionColumn);
                }

                if (!selected.contains(GroupBy.CONTACT)
                                && !ReportKey.CONTACT.equals(getFilteredReport().getReportKey())) {
                    columnList.add(contactNameColumn.setFixedWidth(200));
                }

                if (getFilteredReport().isShowAssigned() && !selected.contains(GroupBy.ASSIGNED)
                                && !ReportKey.CONTACT.equals(getFilteredReport().getReportKey())) {
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
                                        getFilteredReport().getLabel(_parameter, "conditionGroupTotal"), grpTtlClm));
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
                                                  final JasperReportBuilder _builder) throws EFapsException
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
                    ret.add(new CurrencyInst(currency) {
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

        /**
         * Checks if is currency base.
         *
         * @param _parameter the _parameter
         * @return true, if is currency base
         * @throws EFapsException on error
         */
        protected boolean isCurrencyBase(final Parameter _parameter)
            throws EFapsException
        {
            final Collection<CurrencyInst> currencies = getCurrencyInst4Report(_parameter);
            return currencies.size() == 1 && "BASE".equals(currencies.iterator().next().getInstance().getKey());
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

        /**
         * Gets the data bean.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _reportKey the report key
         * @return the data bean
         */
        protected DataBean getDataBean(final Parameter _parameter,
                                       final ReportKey _reportKey)
        {
            return new DataBean(_reportKey);
        }

        /**
         * Gets the pay doc.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the pay doc
         * @throws EFapsException on error
         */
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
    }

    /**
     * The Class DataBean.
     */
    public static class DataBean
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

        /** The report key. */
        private final ReportKey reportKey;

        /** The report date. */
        private DateTime reportDate;
        /**
         * Instantiates a new data bean.
         *
         * @param _reportKey the report key
         */
        public DataBean(final ReportKey _reportKey)
        {
            this.reportKey = _reportKey;
        }

        /**
         * Checks if is currency base.
         *
         * @return true, if is currency base
         */
        public boolean isCurrencyBase()
        {
            return this.currencyBase;
        }

        /**
         * Sets the currency base.
         *
         * @param _currencyBase the _currency base
         * @return the data bean
         */
        public DataBean setCurrencyBase(final boolean _currencyBase)
        {
            this.currencyBase = _currencyBase;
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
            final Properties props;
            switch (this.reportKey) {
                case CONTACT:
                    props = Sales.REPORT_SALES4ACCOUNTCONTACT.get();
                    break;
                case OUT:
                    props = Sales.REPORT_SALES4ACCOUNTOUT.get();
                    break;
                case IN:
                default:
                    props = Sales.REPORT_SALES4ACCOUNTIN.get();
                    break;
            }

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
            if (_showCondition) {
                ret.put("condition", getCondition());
            }
            if (_showAssigned) {
                ret.put("assigned", getAssigned());
            }

            if (_showSwapInfo) {
                ret.put("swapInfo", getSwapInfo());
            }
            final boolean negate = BooleanUtils.toBoolean(props.getProperty(getDocInst().getType().getName()
                            + ".Negate"));
            if (isCurrencyBase()) {
                final BigDecimal crossTmp = this.cross.get(Currency.getBaseCurrency().getId());
                if (crossTmp != null) {
                    ret.put("crossTotal_BASE", negate ? crossTmp.negate() : crossTmp);
                }
                final BigDecimal payTmp = this.payments.get(Currency.getBaseCurrency().getId());
                if (payTmp != null) {
                    ret.put("payed_BASE", negate ? payTmp.negate() : payTmp);
                }
            } else {
                for (final CurrencyInst currency : CurrencyInst.getAvailable()) {
                    if (this.cross.containsKey(currency.getInstance().getId())) {
                        final BigDecimal crossTmp =  this.cross.get(currency.getInstance().getId());
                        if (crossTmp != null) {
                            ret.put("crossTotal_" + currency.getISOCode(),  negate ? crossTmp.negate() : crossTmp);
                        }
                        final BigDecimal payTmp = this.payments.get(currency.getInstance().getId());
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
        public DataBean addCross(final Long _currencyId,
                                 final BigDecimal _cross)
        {
            this.cross.put(_currencyId, _cross);
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
         * @return the data bean
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
         * @return the data bean
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
         * @return the data bean
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
         * @return the data bean
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
         * @return the data bean
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
         * @return the data bean
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
         * @return the data bean
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
         * @return the data bean
         */
        public DataBean setDocRevision(final String _docRevision)
        {
            this.docRevision = _docRevision;
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
                final Properties props;
                switch (this.reportKey) {
                    case CONTACT:
                        props = Sales.REPORT_SALES4ACCOUNTCONTACT.get();
                        break;
                    case OUT:
                        props = Sales.REPORT_SALES4ACCOUNTOUT.get();
                        break;
                    case IN:
                    default:
                        props = Sales.REPORT_SALES4ACCOUNTIN.get();
                        break;
                }
                final Boolean perpay =  props.containsKey("PaymentPerPayment")
                                ? BooleanUtils.toBoolean(props.getProperty("PaymentPerPayment")) : null;

                if (getReportDate().isBefore(new DateTime().withTimeAtStartOfDay())) {
                    docPayInfo.getPayPos().removeIf(p -> p.getDate().isAfter(getReportDate()));
                }
                this.payments.put(Currency.getBaseCurrency().getId(),
                                docPayInfo.getPaidInCurrency(Currency.getBaseCurrency(), perpay).abs());
                this.payments.put(docPayInfo.getRateCurrencyInstance().getId(),
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
            return this.docStatus;
        }

        /**
         * Setter method for instance variable {@link #docStatus}.
         *
         * @param _docStatus value for instance variable {@link #docStatus}
         * @return the data bean
         */
        public DataBean setDocStatus(final String _docStatus)
        {
            this.docStatus = _docStatus;
            return this;
        }

        /**
         * Gets the condition.
         *
         * @return the condition
         */
        public String getCondition()
        {
            if (this.condition == null) {
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
                        if (this.condition != null && !this.condition.isEmpty()) {
                            this.condition = this.condition + ", ";
                        } else {
                            this.condition = "";
                        }
                        this.condition = this.condition + multi.getSelect(selName);
                    }
                } catch (final EFapsException e) {
                    SalesReport4Account_Base.LOG.error("Catched", e);
                }
                if (this.condition == null) {
                    this.condition = "";
                }
            }
            return this.condition;
        }

        /**
         * Sets the condition.
         *
         * @param _condition the condition
         * @return the data bean
         */
        public DataBean setCondition(final String _condition)
        {
            this.condition = _condition;
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
            return this.contactInst;
        }

        /**
         * Sets the contact inst.
         *
         * @param _contactInst the contact inst
         * @return the data bean
         */
        public DataBean setContactInst(final Instance _contactInst)
        {
            this.contactInst = _contactInst;
            return this;
        }

        /**
         * Gets the swapInfo.
         *
         * @return the swapInfo
         */
        public String getSwapInfo()
        {
            return this.swapInfo;
        }

        /**
         * Sets the swapInfo.
         *
         * @param _swapInfo the new swapInfo
         * @return the data bean
         */
        public DataBean setSwapInfo(final String _swapInfo)
        {
            this.swapInfo = _swapInfo;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #reportDate}.
         *
         * @return value of instance variable {@link #reportDate}
         */
        public DateTime getReportDate()
        {
            return this.reportDate;
        }

        /**
         * Setter method for instance variable {@link #reportDate}.
         *
         * @param _reportDate value for instance variable {@link #reportDate}
         * @return the data bean
         */
        public DataBean setReportDate(final DateTime _reportDate)
        {
            this.reportDate = _reportDate;
            return this;
        }
    }
}
