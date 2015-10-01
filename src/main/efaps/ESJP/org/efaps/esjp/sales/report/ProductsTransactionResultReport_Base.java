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

import java.math.BigDecimal;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.products.Cost;
import org.efaps.esjp.products.reports.TransactionResultReport;
import org.efaps.esjp.products.reports.TransactionResultReport_Base;
import org.efaps.esjp.sales.Costing;
import org.efaps.esjp.sales.Costing_Base.CostingInfo;
import org.efaps.util.EFapsException;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.subtotal.AggregationSubtotalBuilder;
import net.sf.dynamicreports.report.constant.Calculation;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("5eaccc41-8c6f-4cb0-99da-4265b80a5646")
@EFapsApplication("eFapsApp-Sales")
public abstract class ProductsTransactionResultReport_Base
    extends TransactionResultReport
{

    /**
     * The Enum Valuation.
     */
    public enum Valuation
    {
        /** The cost. */
        COST,

        /** The costing. */
        COSTING,

        /** The none. */
        NONE;
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return the report class
     * @throws EFapsException on error
     */
    @Override
    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new DynProductsTransactionResultReport(this);
    }

    /**
     * The Class DynProductsTransactionResultReport.
     */
    public static class DynProductsTransactionResultReport
        extends DynTransactionResultReport
    {

        /**
         * Instantiates a new dyn products transaction result report.
         *
         * @param _filteredReport the _filtered report
         */
        public DynProductsTransactionResultReport(final TransactionResultReport_Base _filteredReport)
        {
            super(_filteredReport);
        }

        @Override
        protected void add2Bean(final Parameter _parameter,
                                final DataBean _bean)
            throws EFapsException
        {
            final CurrencyFilterValue filter = (CurrencyFilterValue) getFilteredReport().getFilterMap(
                            _parameter).get("currency");
            switch (getValuation(_parameter)) {
                case COST:
                    final Instance prodInst = _parameter.getInstance();
                    final BigDecimal cost = Cost.getCost4Currency(_parameter, _bean.getDate(), prodInst,
                                    filter.getObject());
                    ((CostDataBean) _bean).setCost(cost).setCurrency(CurrencyInst.get(filter.getObject()).getSymbol());
                    break;
                case COSTING:
                    final CostingInfo costing = Costing.getCosting4Currency(_parameter, _bean.getDate(),
                                    filter.getObject(), _bean.getTransInst());
                    if (costing != null) {
                        ((CostDataBean) _bean).setAverage(costing.getResult()).setCost(costing.getCost()).setCurrency(
                                    CurrencyInst.get(filter.getObject()).getSymbol());
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        protected DataBean getDataBean()
        {
            return new CostDataBean();
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            super.addColumnDefintion(_parameter, _builder);

            if (!Valuation.NONE.equals(getValuation(_parameter))) {
                final TextColumnBuilder<String> currencyColumn = DynamicReports.col.column(getFilteredReport()
                                .getDBProperty("Column.Currency"),
                                "currency", DynamicReports.type.stringType()).setWidth(15);
                _builder.getReport().getColumns().add(4, currencyColumn.build());

                final TextColumnBuilder<BigDecimal> costColumn = DynamicReports.col.column(getFilteredReport()
                                .getDBProperty("Column.Cost"),
                                "cost", DynamicReports.type.bigDecimalType());

                final TextColumnBuilder<BigDecimal> totalCostColumn = DynamicReports.col.column(getFilteredReport()
                                .getDBProperty("Column.TotalCost"),
                                "totalCost", DynamicReports.type.bigDecimalType());
                final AggregationSubtotalBuilder<BigDecimal> totalCostSum = DynamicReports.sbt.aggregate(
                                totalCostColumn,
                                Calculation.NOTHING);

                _builder.getReport().getColumns().add(4, totalCostColumn.build());
                _builder.getReport().getColumns().add(4, costColumn.build());
                _builder.addSubtotalAtSummary(totalCostSum);

                if (getStorageGroup() != null) {
                    final AggregationSubtotalBuilder<Object> totalCostSum4Grp = DynamicReports.sbt.aggregate(
                                    totalCostColumn, Calculation.NOTHING);
                    _builder.subtotalsAtGroupFooter(getStorageGroup(), totalCostSum4Grp);
                }

                if (Valuation.COSTING.equals(getValuation(_parameter))) {
                    final TextColumnBuilder<BigDecimal> averageColumn = DynamicReports.col.column(getFilteredReport()
                                    .getDBProperty("Column.Average"),
                                    "average", DynamicReports.type.bigDecimalType());

                    final TextColumnBuilder<BigDecimal> averageTotalColumn = DynamicReports.col.column(
                                    getFilteredReport().getDBProperty("Column.TotalAverage"),
                                    "totalAverage", DynamicReports.type.bigDecimalType());
                    final AggregationSubtotalBuilder<BigDecimal> averageTotalSum = DynamicReports.sbt.aggregate(
                                    averageTotalColumn,
                                    Calculation.NOTHING);

                    _builder.getReport().getColumns().add(6, averageTotalColumn.build());
                    _builder.getReport().getColumns().add(6, averageColumn.build());

                    _builder.addSubtotalAtSummary(averageTotalSum);

                    if (getStorageGroup() != null) {
                        final AggregationSubtotalBuilder<Object> averageTotalSum4Grp = DynamicReports.sbt.aggregate(
                                        averageTotalColumn, Calculation.NOTHING);
                        _builder.subtotalsAtGroupFooter(getStorageGroup(), averageTotalSum4Grp);
                    }
                }
            }
        }

        /**
         * Gets the valuation.
         *
         * @param _parameter the _parameter
         * @return the valuation
         * @throws EFapsException on error
         */
        protected Valuation getValuation(final Parameter _parameter)
            throws EFapsException
        {
            final EnumFilterValue filter = (EnumFilterValue) getFilteredReport().getFilterMap(_parameter).get(
                            "valuation");
            Valuation ret;
            if (filter != null) {
                ret = (Valuation) filter.getObject();
            } else {
                ret = Valuation.NONE;
            }
            return ret;
        }
    }

    /**
     * The Class CostDataBean.
     */
    public static class CostDataBean
        extends DataBean
    {

        /** The cost. */
        private BigDecimal cost;

        /** The average. */
        private BigDecimal average;

        /** The currency. */
        private String currency;

        /**
         * Getter method for the instance variable {@link #cost}.
         *
         * @return value of instance variable {@link #cost}
         */
        public BigDecimal getCost()
        {
            return this.cost;
        }

        /**
         * Setter method for instance variable {@link #cost}.
         *
         * @param _cost value for instance variable {@link #cost}
         * @return the cost data bean
         */
        public CostDataBean setCost(final BigDecimal _cost)
        {
            this.cost = _cost;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #total}.
         *
         * @return value of instance variable {@link #total}
         */
        public BigDecimal getTotalCost()
        {
            BigDecimal ret;
            if (getTotal() == null || getCost() == null) {
                ret = BigDecimal.ZERO;
            } else {
                ret = getTotal().multiply(getCost());
            }
            return ret;
        }

        /**
         * Getter method for the instance variable {@link #total}.
         *
         * @return value of instance variable {@link #total}
         */
        public BigDecimal getTotalAverage()
        {
            BigDecimal ret;
            if (getTotal() == null || getCost() == null) {
                ret = BigDecimal.ZERO;
            } else {
                ret = getTotal().multiply(getAverage());
            }
            return ret;
        }

        /**
         * Getter method for the instance variable {@link #curreny}.
         *
         * @return value of instance variable {@link #curreny}
         */
        public String getCurrency()
        {
            return this.currency;
        }

        /**
         * Setter method for instance variable {@link #currency}.
         *
         * @param _currency the _currency
         * @return the cost data bean
         */
        public CostDataBean setCurrency(final String _currency)
        {
            this.currency = _currency;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #average}.
         *
         * @return value of instance variable {@link #average}
         */
        public BigDecimal getAverage()
        {
            return this.average;
        }

        /**
         * Setter method for instance variable {@link #average}.
         *
         * @param _average value for instance variable {@link #average}
         * @return the cost data bean
         */
        public CostDataBean setAverage(final BigDecimal _average)
        {
            this.average = _average;
            return this;
        }
    }
}
