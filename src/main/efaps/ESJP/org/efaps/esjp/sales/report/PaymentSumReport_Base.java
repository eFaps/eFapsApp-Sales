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

import java.awt.Color;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
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
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.properties.PropertiesUtil;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.AbstractGroupedByDate;
import org.efaps.esjp.erp.AbstractGroupedByDate_Base.DateGroup;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.FilteredReport;
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
import net.sf.dynamicreports.report.builder.style.ConditionalStyleBuilder;
import net.sf.dynamicreports.report.builder.style.StyleBuilder;
import net.sf.dynamicreports.report.constant.Calculation;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * The Class PaymentReport_Base.
 *
 * @author The eFaps Team
 */
@EFapsUUID("e50b174c-d308-4226-9b6e-d1ee0997af7f")
@EFapsApplication("eFapsApp-Sales")
public abstract class PaymentSumReport_Base
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
     * Used to distinguish between incoming and outgoing report.
     */
    public enum TypeGroup
    {
        /** Both. */
        NONE,
        /** Incoming. */
        BEFOREACC,
        /** Outgoing. */
        AFTERACC,
    }


    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PaymentSumReport.class);

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
        return new DynPaymentSumReport(this);
    }

    /**
     * The Class DynPaymentReport.
     */
    public static class DynPaymentSumReport
        extends AbstractDynamicReport
    {

        /** The filtered report. */
        private final PaymentSumReport_Base filteredReport;

        /** The neg type. */
        private final Set<Type> negTypes = new HashSet<>();

        /**
         * Instantiates a new dyn payment report.
         *
         * @param _paymentReport the payment report
         */
        public DynPaymentSumReport(final PaymentSumReport_Base _paymentReport)
        {
            this.filteredReport = _paymentReport;
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

                final List<DataBean> beans = new ArrayList<>();
                final GroupedByDate groupedBy = new GroupedByDate();
                final DateGroup dateGroup = getDateGroup(_parameter);
                final DateTimeFormatter dateTimeFormatter = groupedBy.getDateTimeFormatter(dateGroup);

                final QueryBuilder queryBldr = getQueryBuilder(_parameter, "PAYMENT");
                add2QueryBuilder(_parameter, queryBldr);

                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder selAccount = SelectBuilder.get()
                                .linkfrom(CISales.Payment.TargetDocument)
                                .linkfrom(CISales.TransactionAbstract.Payment)
                                .linkto(CISales.TransactionAbstract.Account)
                                .attribute(CISales.AccountAbstract.Name);
                final SelectBuilder selCurrencyInst = SelectBuilder.get()
                                .linkto(CISales.PaymentDocumentAbstract.CurrencyLink)
                                .instance();
                final SelectBuilder selRateCurrencyInst = SelectBuilder.get()
                                .linkto(CISales.PaymentDocumentAbstract.RateCurrencyLink)
                                .instance();
                multi.addSelect(selAccount, selCurrencyInst, selRateCurrencyInst);
                multi.addAttribute(CISales.PaymentDocumentAbstract.Amount, CISales.PaymentDocumentAbstract.Date);
                multi.execute();
                while (multi.next()) {
                    final String partial = groupedBy.getPartial(multi.getAttribute(
                                    CISales.PaymentDocumentAbstract.Date), dateGroup).toString(dateTimeFormatter);
                    BigDecimal amount = multi.getAttribute(CISales.Payment.Amount);
                    if (isNegativ(_parameter, multi.getCurrentInstance())) {
                        amount = amount.negate();
                    }
                    final DataBean dataBean = new DataBean()
                                    .setInstance(multi.getCurrentInstance())
                                    .setAccount(multi.getSelect(selAccount))
                                    .setAmount(amount)
                                    .setCurrencyInst(multi.getSelect(selCurrencyInst))
                                    .setRateCurrencyInst(multi.getSelect(selRateCurrencyInst))
                                    .setPartial(partial);

                    beans.add(dataBean);
                    PaymentSumReport_Base.LOG.debug("Read {}", dataBean);
                }

                final QueryBuilder docQueryBldr = getQueryBuilder(_parameter, "DOCUMENT");
                if (docQueryBldr != null) {
                    add2QueryBuilder(_parameter, docQueryBldr);

                    final MultiPrintQuery docMulti = docQueryBldr.getPrint();
                    final SelectBuilder selDocCurrencyInst = SelectBuilder.get()
                                    .linkto(CISales.DocumentSumAbstract.CurrencyId)
                                    .instance();
                    final SelectBuilder selDocRateCurrencyInst = SelectBuilder.get()
                                    .linkto(CISales.DocumentSumAbstract.RateCurrencyId)
                                    .instance();
                    docMulti.addSelect(selDocCurrencyInst, selDocRateCurrencyInst);
                    docMulti.addAttribute(CISales.DocumentSumAbstract.CrossTotal, CISales.DocumentSumAbstract.Date);
                    docMulti.execute();
                    while (docMulti.next()) {
                        final String partial = groupedBy.getPartial(docMulti.getAttribute(
                                        CISales.DocumentSumAbstract.Date), dateGroup).toString(dateTimeFormatter);
                        BigDecimal amount = docMulti.getAttribute(CISales.DocumentSumAbstract.CrossTotal);
                        if (isNegativ(_parameter, docMulti.getCurrentInstance())) {
                            amount = amount.negate();
                        }
                        final DataBean dataBean = new DataBean()
                                        .setInstance(docMulti.getCurrentInstance())
                                        .setAmount(amount)
                                        .setCurrencyInst(docMulti.getSelect(selDocCurrencyInst))
                                        .setRateCurrencyInst(docMulti.getSelect(selDocRateCurrencyInst))
                                        .setPartial(partial);
                        beans.add(dataBean);
                        PaymentSumReport_Base.LOG.debug("Read {}", dataBean);
                    }
                }
                ret = new JRBeanCollectionDataSource(beans);
                getFilteredReport().cache(_parameter, ret);
            }
            return ret;
        }

        /**
         * Gets the query builder.
         *
         * @param _parameter the parameter
         * @param _suffix the suffix
         * @return the query builder
         * @throws EFapsException the e faps exception
         */
        protected QueryBuilder getQueryBuilder(final Parameter _parameter,
                                               final String _suffix)
            throws EFapsException
        {
            final QueryBuilder ret;
            switch (getPayDoc(_parameter)) {
                case IN:
                    ret = getQueryBldrFromProperties(_parameter, Sales.REPORT_PAYMENTSUM.get(), _suffix + "."
                                    + PayDoc.IN.name());
                    break;
                case OUT:
                    ret = getQueryBldrFromProperties(_parameter, Sales.REPORT_PAYMENTSUM.get(), _suffix + "."
                                    + PayDoc.OUT.name());
                    break;
                case BOTH:
                default:
                    ret = getQueryBldrFromProperties(_parameter, Sales.REPORT_PAYMENTSUM.get(), _suffix + "."
                                    + PayDoc.BOTH.name());
                    break;
            }
            return ret;
        }

        /**
         * Checks if is negativ.
         *
         * @param _parameter the parameter
         * @param _inst the inst
         * @return true, if is negativ
         * @throws EFapsException the e faps exception
         */
        protected boolean isNegativ(final Parameter _parameter,
                                    final Instance _inst)
            throws EFapsException
        {
            if (this.negTypes.isEmpty()) {
                final Collection<String> types = analyseProperty(_parameter, PropertiesUtil.getProperties4Prefix(
                                Sales.REPORT_PAYMENTSUM.get(), "PAYMENT." + PayDoc.OUT.name()), "Type").values();
                types.addAll(analyseProperty(_parameter, PropertiesUtil.getProperties4Prefix(Sales.REPORT_PAYMENTSUM
                                .get(), "DOCUMENT." + PayDoc.OUT.name()), "Type").values());
                for (final String typeStr : types) {
                    final Type type;
                    if (isUUID(typeStr)) {
                        type = Type.get(UUID.fromString(typeStr));
                    } else {
                        type = Type.get(typeStr);
                    }
                    this.negTypes.addAll(getTypeList(_parameter, type));
                }
            }
            return this.negTypes.contains(_inst.getType());
        }

        /**
         * Add to query builder.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _queryBldr the query bldr
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
                dateFrom = new DateTime().minusMonths(1);
            }
            final DateTime dateTo;
            if (filter.containsKey("dateTo")) {
                dateTo = (DateTime) filter.get("dateTo");
            } else {
                dateTo = new DateTime();
            }
            _queryBldr.addWhereAttrGreaterValue(CIERP.DocumentAbstract.Date, dateFrom
                            .withTimeAtStartOfDay().minusMinutes(1));
            _queryBldr.addWhereAttrLessValue(CIERP.DocumentAbstract.Date, dateTo.plusDays(1)
                            .withTimeAtStartOfDay());
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
            final Map<String, Object> filterMap = this.filteredReport.getFilterMap(_parameter);

            final PayDoc ret;
            if (filterMap.containsKey("payDoc")) {
                final EnumFilterValue filterValue = (EnumFilterValue) filterMap.get("payDoc");
                ret = (PayDoc) filterValue.getObject();
            } else {
                ret = PayDoc.BOTH;
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
        protected TypeGroup getTypeGroup(final Parameter _parameter)
            throws EFapsException
        {
            final Map<String, Object> filterMap = this.filteredReport.getFilterMap(_parameter);

            final TypeGroup ret;
            if (filterMap.containsKey("typeGroup")) {
                final EnumFilterValue filterValue = (EnumFilterValue) filterMap.get("typeGroup");
                ret = (TypeGroup) filterValue.getObject();
            } else {
                ret = TypeGroup.NONE;
            }
            return ret;
        }

        /**
         * Gets the date group.
         *
         * @param _parameter the parameter
         * @return the date group
         * @throws EFapsException the e faps exception
         */
        protected DateGroup getDateGroup(final Parameter _parameter)
            throws EFapsException
        {
            final DocumentSumGroupedByDate_Base.DateGroup ret;
            final Map<String, Object> filterMap = this.filteredReport.getFilterMap(_parameter);
            if (filterMap.containsKey("dateGroup") && filterMap.get("dateGroup") != null) {
                ret = (DateGroup) ((EnumFilterValue) filterMap.get("dateGroup")).getObject();
            } else {
                ret = DocumentSumGroupedByDate_Base.DateGroup.MONTH;
            }
            return ret;
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {

            final TypeGroup typeGroup = getTypeGroup(_parameter);

            final CrosstabBuilder crosstab = DynamicReports.ctab.crosstab();

            final CrosstabMeasureBuilder<BigDecimal> amountMeasure = DynamicReports.ctab.measure(
                            getFilteredReport().getDBProperty("Amount"), "amount", BigDecimal.class, Calculation.SUM);
            crosstab.addMeasure(amountMeasure);

            final ConditionalStyleBuilder condition1 = DynamicReports.stl.conditionalStyle(
                            DynamicReports.cnd.smaller(amountMeasure, 0))
                           .setForegroundColor(Color.red);

            final StyleBuilder unitPriceStyle = DynamicReports.stl.style()
                                      .conditionalStyles(condition1);
            amountMeasure.setStyle(unitPriceStyle);

            if (TypeGroup.BEFOREACC.equals(typeGroup)) {
                final CrosstabRowGroupBuilder<String> rowTypeGroup = DynamicReports.ctab
                                .rowGroup("type", String.class);
                crosstab.addRowGroup(rowTypeGroup);
            }

            final CrosstabRowGroupBuilder<String> rowAccountGroup = DynamicReports.ctab
                            .rowGroup("account", String.class)
                            .setHeaderWidth(150);
            crosstab.addRowGroup(rowAccountGroup);

            if (TypeGroup.AFTERACC.equals(typeGroup)) {
                final CrosstabRowGroupBuilder<String> rowTypeGroup = DynamicReports.ctab
                            .rowGroup("type", String.class);
                crosstab.addRowGroup(rowTypeGroup);
            }

            final CrosstabColumnGroupBuilder<String> columnPartialGroup = DynamicReports.ctab.columnGroup("partial",
                            String.class);
            crosstab.addColumnGroup(columnPartialGroup);

            final CrosstabColumnGroupBuilder<String> rowCurrencyGroup = DynamicReports.ctab.columnGroup("currency",
                            String.class).setShowTotal(false);
            crosstab.addColumnGroup(rowCurrencyGroup);

            _builder.addSummary(crosstab);
        }

        /**
         * Gets the filtered report.
         *
         * @return the filtered report
         */
        public PaymentSumReport_Base getFilteredReport()
        {
            return this.filteredReport;
        }
    }

    /**
     * The Class DataBean.
     */
    public static class DataBean
    {

        /** The amount. */
        private BigDecimal amount;

        /** The account. */
        private String account;

        /** The currency inst. */
        private Instance currencyInst;

        /** The currency inst. */
        private Instance rateCurrencyInst;

        /** The instance. */
        private Instance instance;

        /** The partial. */
        private String partial;
        /**
         * Gets the account.
         *
         * @return the account
         */
        public String getAccount()
        {
            return this.account;
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
                this.account = String.valueOf(((Collection<?>) _account).iterator().next());
            } else {
                this.account = String.valueOf(_account);
            }
            return this;
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
            return InstanceUtils.isValid(getRateCurrencyInst())
                            ? CurrencyInst.get(getRateCurrencyInst()).getSymbol() : null;
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
         * Gets the currency inst.
         *
         * @return the currency inst
         */
        public Instance getCurrencyInst()
        {
            return this.currencyInst;
        }

        /**
         * Sets the currency inst.
         *
         * @param _currencyInst the currency inst
         * @return the data bean
         */
        public DataBean setCurrencyInst(final Instance _currencyInst)
        {
            this.currencyInst = _currencyInst;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #type}.
         *
         * @return value of instance variable {@link #type}
         */
        public String getType()
        {
            return getInstance().getType().getLabel();
        }

        /**
         * Getter method for the instance variable {@link #rateCurrencyInst}.
         *
         * @return value of instance variable {@link #rateCurrencyInst}
         */
        public Instance getRateCurrencyInst()
        {
            return this.rateCurrencyInst;
        }

        /**
         * Setter method for instance variable {@link #rateCurrencyInst}.
         *
         * @param _rateCurrencyInst value for instance variable {@link #rateCurrencyInst}
         * @return the data bean
         */
        public DataBean setRateCurrencyInst(final Instance _rateCurrencyInst)
        {
            this.rateCurrencyInst = _rateCurrencyInst;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #partial}.
         *
         * @return value of instance variable {@link #partial}
         */
        public String getPartial()
        {
            return this.partial;
        }


        /**
         * Setter method for instance variable {@link #partial}.
         *
         * @param _partial value for instance variable {@link #partial}
         * @return the data bean
         */
        public DataBean setPartial(final String _partial)
        {
            this.partial = _partial;
            return this;
        }


        /**
         * Getter method for the instance variable {@link #instance}.
         *
         * @return value of instance variable {@link #instance}
         */
        public Instance getInstance()
        {
            return this.instance;
        }


        /**
         * Setter method for instance variable {@link #instance}.
         *
         * @param _instance value for instance variable {@link #instance}
         * @return the data bean
         */
        public DataBean setInstance(final Instance _instance)
        {
            this.instance = _instance;
            return this;
        }

        @Override
        public String toString()
        {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    /**
     * The Class GroupedByDate.
     */
    public static class GroupedByDate
        extends AbstractGroupedByDate
    {

    }
}
