/*
 * Copyright 2003 - 2013 The eFaps Team
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabMeasureBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabRowGroupBuilder;
import net.sf.dynamicreports.report.constant.Calculation;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.jasperreports.engine.JRDataSource;

import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.sales.cashflow.CashFlowCategory;
import org.efaps.esjp.sales.cashflow.CashFlowGroup;
import org.efaps.esjp.sales.cashflow.ICashFlowCategory;
import org.efaps.esjp.sales.cashflow.ICashFlowGroup;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("67b5c710-4e36-4eb2-95c0-8f04b15500ec")
@EFapsRevision("$Rev$")
public abstract class CashFlowReport_Base
{

    public enum Interval
    {
        MONTH, WEEK;
    }

    public static ICashFlowCategory OPENCAT = new ICashFlowCategory()
    {
        @Override
        public int getInt()
        {
            return -1;
        }

        @Override
        public Integer getWeight()
        {
            return -1;
        }

        @Override
        public String toString()
        {
            return "OPEN";
        }
    };

    public static ICashFlowGroup OPENGRP = new ICashFlowGroup()
    {

        @Override
        public Integer getWeight()
        {
            return -1;
        }

        @Override
        public String toString()
        {
            return "OPEN";
        }
    };

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
        final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String mime = (String) props.get("Mime");
        final AbstractDynamicReport dyRp = getReport(_parameter);
        dyRp.setFileName(DBProperties.getProperty(CashFlowReport.class.getName() + ".FileName"));
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
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final Interval interval;
        if (properties.containsKey("Interval")) {
            interval = Interval.valueOf((String) properties.get("Interval"));
        } else {
            interval = Interval.MONTH;
        }
        int projection;
        if (properties.containsKey("Projection")) {
            projection = Integer.valueOf((String) properties.get("Projection"));
        } else {
            if (Interval.MONTH.equals(interval)) {
                projection = 6;
            } else {
                projection = 8;
            }
        }
        Integer start = 0;
        if (properties.containsKey("Start")) {
            start = Integer.valueOf((String) properties.get("Start"));
        }
        DateTime startDate;
        if (start < 1) {
            startDate = new DateTime().withTimeAtStartOfDay().withDayOfYear(1).plusYears(start).minusSeconds(1);
        } else {
            if (Interval.MONTH.equals(interval)) {
                startDate = new DateTime().withTimeAtStartOfDay().withDayOfMonth(1).minusMonths(start).minusSeconds(1);
            } else {
                startDate = new DateTime().withTimeAtStartOfDay().withDayOfWeek(1).minusWeeks(start).minusSeconds(1);
            }
        }
        return new CFReport(interval, startDate, projection);
    }

    /**
     * Report class.
     */
    public static class CFReport
        extends AbstractDynamicReport
    {

        /**
         *
         */
        private final Map<ICashFlowGroup, Group> groups = new HashMap<ICashFlowGroup, Group>();

        private final Interval interval;

        private final int projection;

        private final DateTime startDate;

        public CFReport(final Interval _interval,
                        final DateTime _startDate,
                        final int _projection)
        {
            this.interval = _interval;
            this.projection = _projection;
            this.startDate = _startDate;
        }


        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final DRDataSource dataSource = new DRDataSource("group", "category", "date", "amount");

            final Map<Instance, Instance> inst2inst = new HashMap<Instance, Instance>();
            final QueryBuilder queryBldr = new QueryBuilder(CISales.Payment);
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selCreateInst = SelectBuilder.get().linkto(CISales.Payment.CreateDocument).instance();
            multi.addSelect(selCreateInst);
            multi.execute();
            while (multi.next()) {
                final Instance payInst = multi.getCurrentInstance();
                final Instance docInst = multi.getSelect(selCreateInst);
                inst2inst.put(payInst, docInst);
            }

            final QueryBuilder queryBldr2 = new QueryBuilder(CISales.TransactionAbstract);
            queryBldr.addWhereAttrGreaterValue(CISales.TransactionAbstract.Date, this.startDate);
            final MultiPrintQuery multi2 = queryBldr2.getPrint();
            final SelectBuilder selPaymentInst = SelectBuilder.get()
                            .linkto(CISales.TransactionAbstract.Payment).instance();
            multi2.addSelect(selPaymentInst);
            multi2.addAttribute(CISales.TransactionAbstract.Amount, CISales.TransactionAbstract.Date);
            multi2.execute();
            while (multi2.next()) {
                final DateTime date = multi2.<DateTime>getAttribute(CISales.TransactionAbstract.Date);
                BigDecimal amount = multi2.<BigDecimal>getAttribute(CISales.TransactionAbstract.Amount);
                final Instance payInst = multi2.<Instance>getSelect(selPaymentInst);

                final boolean out = multi2.getCurrentInstance().getType().isKindOf(
                                CISales.TransactionOutbound.getType());

                if (out) {
                    amount = amount.negate();
                }
                final Group group = getGroup(_parameter, out, payInst, inst2inst.get(payInst));

                final String dateStr = getDateStr(_parameter, date);

                group.add(_parameter,  out, payInst, inst2inst.get(payInst) , dateStr, amount);
            }

            addProjection(_parameter);

            final Map<String, BigDecimal> saldos = new TreeMap<String, BigDecimal>();
            for (final Group group : this.groups.values()) {
                final Map<String, BigDecimal> saldo = group.getSaldo(_parameter);
                for (final Entry<String, BigDecimal> entry : saldo.entrySet()) {
                    BigDecimal amount;
                    if (saldos.containsKey(entry.getKey())) {
                        amount = saldos.get(entry.getKey());
                    } else {
                        amount = BigDecimal.ZERO;
                    }
                    saldos.put(entry.getKey(), amount.add(entry.getValue()));
                }
            }
            final Group open = new Group(CashFlowReport_Base.OPENGRP);
            this.groups.put(CashFlowReport_Base.OPENGRP, open);
            final Category cat = new Category(open, CashFlowReport_Base.OPENCAT);
            open.getCategories().put(CashFlowReport_Base.OPENCAT, cat);
            BigDecimal currentAmount = BigDecimal.ZERO;
            for (final Entry<String, BigDecimal> saldo : saldos.entrySet()) {
                cat.add(_parameter, saldo.getKey(), currentAmount);
                currentAmount = currentAmount.add(saldo.getValue());
            }
            final List<Group> grpList = new ArrayList<Group>(this.groups.values());
            Collections.sort(grpList, new Comparator<Group>()
            {
                @Override
                public int compare(final Group _o1,
                                   final Group _o2)
                {
                    return _o1.getWeight().compareTo(_o2.getWeight());
                }
            });
            int i = 1;
            for (final Group group : grpList) {
                group.setNumber(i);
                group.add2DataSource(_parameter, dataSource);
                i++;
            }
            return dataSource;
        }

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

        /**
         * @param _parameter
         * @param _data
         * @param _out
         * @param _payInst
         * @param _instance
         * @return
         */
        protected Group getGroup(final Parameter _parameter,
                                 final boolean _out,
                                 final Instance _payInst,
                                 final Instance _instance)
        {
            ICashFlowGroup key;
            if (_out) {
                key = CashFlowGroup.OUT;
                if (!this.groups.containsKey(key)) {
                    this.groups.put(key, new Group(key));
                }
            } else {
                key = CashFlowGroup.IN;
                if (!this.groups.containsKey(key)) {
                    this.groups.put(key, new Group(key));
                }
            }
            return this.groups.get(key);
        }

        /**
         * @param _parameter Parameter as passed by the eFaps API
         * @param _date DateTime the String is wanted for
         * @return String for Date
         * @throws EFapsException on error
         */
        protected String getDateStr(final Parameter _parameter,
                                    final DateTime _date)
            throws EFapsException
        {
            String ret;
            switch (this.interval) {
                case MONTH:
                    final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy/MM - MMM")
                                    .withLocale(Context.getThreadContext().getLocale());
                    ret = _date.toString(formatter);
                    break;
                case WEEK:
                    final DateTimeFormatter wformatter = DateTimeFormat.forPattern("yyyy - ww")
                        .withLocale(Context.getThreadContext().getLocale());
                    ret = _date.toString(wformatter);
                    break;
                default:
                    ret = _date.toString();
                    break;
            }
            return ret;
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final CrosstabBuilder crosstab = DynamicReports.ctab.crosstab();
            final CrosstabMeasureBuilder<BigDecimal> amountMeasure = DynamicReports.ctab.measure("amount",
                            BigDecimal.class, Calculation.SUM);
            final CrosstabRowGroupBuilder<String> rowGroup = DynamicReports.ctab.rowGroup("group", String.class);
            final CrosstabRowGroupBuilder<String> rowCategory = DynamicReports.ctab.rowGroup("category", String.class);
            final CrosstabColumnGroupBuilder<String> columnGroup = DynamicReports.ctab
                            .columnGroup("date", String.class).setShowTotal(false);

            crosstab.headerCell(DynamicReports.cmp.text(DBProperties
                            .getProperty(CashFlowReport.class.getName() + ".HeaderCell"))
                            .setStyle(DynamicReports.stl.style().setBold(true)))
                            .rowGroups(rowGroup, rowCategory)
                            .columnGroups(columnGroup)
                            .measures(amountMeasure);
            _builder.summary(crosstab);
        }

        /**
         * Getter method for the instance variable {@link #groups}.
         *
         * @return value of instance variable {@link #groups}
         */
        protected Map<ICashFlowGroup, Group> getGroups()
        {
            return this.groups;
        }

        /**
         * Getter method for the instance variable {@link #projection}.
         *
         * @return value of instance variable {@link #projection}
         */
        protected int getProjection()
        {
            return this.projection;
        }
    }

    public static class Group
    {

        private final Map<ICashFlowCategory, Category> categories = new HashMap<ICashFlowCategory, Category>();

        private Integer number = 0;

        private final ICashFlowGroup cashFlowGroup;

        /**
         * @param _groupName
         */
        public Group(final ICashFlowGroup _group)
        {
            this.cashFlowGroup = _group;
        }

        /**
         * @param _parameter
         * @param _out
         * @param _payInst
         * @param _instance
         * @param _dateStr
         * @param _amount
         */
        public void add(final Parameter _parameter,
                        final boolean _out,
                        final Instance _payInst,
                        final Instance _docInst,
                        final String _dateStr,
                        final BigDecimal _amount)
        {
            CashFlowCategory cat;
            if (_docInst == null) {
                cat = CashFlowCategory.NONE;
            } else if (_docInst.isValid()) {
                if (_docInst.getType().isKindOf(CISales.Invoice.getType())
                                || _docInst.getType().isKindOf(CISales.Receipt.getType())
                                || _docInst.getType().isKindOf(CISales.Reminder.getType())
                                || _docInst.getType().isKindOf(CISales.CreditNote.getType())) {
                    cat = CashFlowCategory.SELL;
                } else if (_docInst.getType().isKindOf(CISales.IncomingInvoice.getType())
                               || _docInst.getType().isKindOf(CISales.IncomingReceipt.getType())
                               || _docInst.getType().isKindOf(CISales.IncomingCreditNote.getType())) {
                    cat = CashFlowCategory.BUY;
                } else if (_docInst.getType().isKindOf(CISales.IncomingCredit.getType())
                                || _docInst.getType().isKindOf(CISales.Credit.getType())) {
                    cat = CashFlowCategory.CREDIT;
                    // Payroll_Payslip
                } else if (_docInst.isValid() && _docInst.getType().getUUID().equals(
                                UUID.fromString("a298d361-7530-4a24-b69f-ff3a1186a081"))) {
                    cat = CashFlowCategory.PAYROLL;
                } else {
                    cat = CashFlowCategory.OTHER;
                }
            } else {
                cat = CashFlowCategory.OTHER;
            }
            add(_parameter, _out, cat, _dateStr, _amount);
        }

        public void add(final Parameter _parameter,
                        final boolean _out,
                        final ICashFlowCategory _cat,
                        final String _dateStr,
                        final BigDecimal _amount)
        {
            Category category;
            if (this.categories.containsKey(_cat)) {
                category = this.categories.get(_cat);
            } else {
                category = new Category(this, _cat);
                this.categories.put(_cat, category);
            }
            category.add(_parameter, _dateStr, _amount);
        }


        /**
         * @param _parameter
         * @return
         */
        public Map<String, BigDecimal> getSaldo(final Parameter _parameter)
        {
            final Map<String, BigDecimal> ret = new TreeMap<String, BigDecimal>();
            for (final Category category : this.categories.values()) {
                final Map<String, BigDecimal> amounts = category.getAmounts();
                for (final Entry<String, BigDecimal> entry : amounts.entrySet()) {
                    BigDecimal amount;
                    if (ret.containsKey(entry.getKey())) {
                        amount = ret.get(entry.getKey());
                    } else {
                        amount = BigDecimal.ZERO;
                    }
                    ret.put(entry.getKey(), amount.add(entry.getValue()));
                }
            }
            return ret;
        }

        /**
         * @param _dataSource
         */
        public void add2DataSource(final Parameter _parameter,
                                   final DRDataSource _dataSource)
        {
            final List<Category> catList = new ArrayList<Category>(this.categories.values());
            Collections.sort(catList, new Comparator<Category>()
            {
                @Override
                public int compare(final Category _o1,
                                   final Category _o2)
                {
                    return _o1.getWeight().compareTo(_o1.getWeight());
                }
            });
            int i = 1;
            for (final Category category : catList) {
                category.setNumber(i);
                category.add2DataSource(_parameter, _dataSource);
                i++;
            }
        }

        /**
         * Getter method for the instance variable {@link #name}.
         *
         * @return value of instance variable {@link #name}
         */
        public String getName()
        {
            return this.number + ". "
                            + DBProperties.getProperty(CashFlowReport.class.getName() + ".Group." + getCashFlowGroup());
        }

        /**
         * Getter method for the instance variable {@link #weight}.
         *
         * @return value of instance variable {@link #weight}
         */
        public Integer getWeight()
        {
            return this.cashFlowGroup.getWeight();
        }

        /**
         * Getter method for the instance variable {@link #categories}.
         *
         * @return value of instance variable {@link #categories}
         */
        public Map<ICashFlowCategory, Category> getCategories()
        {
            return this.categories;
        }

        /**
         * Getter method for the instance variable {@link #number}.
         *
         * @return value of instance variable {@link #number}
         */
        public Integer getNumber()
        {
            return this.number;
        }

        /**
         * Setter method for instance variable {@link #number}.
         *
         * @param _number value for instance variable {@link #number}
         */
        public void setNumber(final Integer _number)
        {
            this.number = _number;
        }


        /**
         * Getter method for the instance variable {@link #cashFlowGroup}.
         *
         * @return value of instance variable {@link #cashFlowGroup}
         */
        public ICashFlowGroup getCashFlowGroup()
        {
            return this.cashFlowGroup;
        }
    }

    /**
     * Category.
     */
    public static class Category
    {

        /**
         * Date to amount mapping.
         */
        private final Map<String, BigDecimal> amounts = new HashMap<String, BigDecimal>();

        private final Group group;

        private Integer number;

        private final ICashFlowCategory cashFlowCategory;


        /**
         * @param _group
         * @param _cat
         */
        public Category(final Group _group,
                        final ICashFlowCategory _cat)
        {
            this.group = _group;
            this.cashFlowCategory = _cat;
        }

        /**
         * @param _parameter
         * @param _dataSource
         */
        public void add2DataSource(final Parameter _parameter,
                                   final DRDataSource _dataSource)
        {
            BigDecimal total = BigDecimal.ZERO;
            for (final Entry<String, BigDecimal> entry : this.amounts.entrySet()) {
                _dataSource.add(this.group.getName(), getName(), entry.getKey(), entry.getValue());
                total = total.add(entry.getValue());
            }
            if (!CashFlowReport_Base.OPENCAT.equals(this.cashFlowCategory)) {
                _dataSource.add(this.group.getName(), getName(), "Total", total);
            }
        }

        /**
         * @param _parameter
         * @param _dateStr
         * @param _amount
         */
        public void add(final Parameter _parameter,
                        final String _dateStr,
                        final BigDecimal _amount)
        {
            BigDecimal amount;
            if (this.amounts.containsKey(_dateStr)) {
                amount = this.amounts.get(_dateStr);
            } else {
                amount = BigDecimal.ZERO;
            }
            this.amounts.put(_dateStr, amount.add(_amount));
        }

        /**
         * Getter method for the instance variable {@link #name}.
         *
         * @return value of instance variable {@link #name}
         */
        public String getName()
        {
            return this.number + ". "
                            + DBProperties.getProperty(CashFlowReport.class.getName() + ".Category."
                                            + this.group.getCashFlowGroup() + "." + getCashFlowCategory());
        }

        /**
         * Getter method for the instance variable {@link #amounts}.
         *
         * @return value of instance variable {@link #amounts}
         */
        public Map<String, BigDecimal> getAmounts()
        {
            return this.amounts;
        }

        /**
         * Getter method for the instance variable {@link #weight}.
         *
         * @return value of instance variable {@link #weight}
         */
        public Integer getWeight()
        {
            return this.cashFlowCategory.getWeight();
        }

        /**
         * Getter method for the instance variable {@link #number}.
         *
         * @return value of instance variable {@link #number}
         */
        public Integer getNumber()
        {
            return this.number;
        }

        /**
         * Setter method for instance variable {@link #number}.
         *
         * @param _number value for instance variable {@link #number}
         */
        public void setNumber(final Integer _number)
        {
            this.number = _number;
        }


        /**
         * Getter method for the instance variable {@link #cashFlowCategory}.
         *
         * @return value of instance variable {@link #cashFlowCategory}
         */
        public ICashFlowCategory getCashFlowCategory()
        {
            return this.cashFlowCategory;
        }
    }
}
