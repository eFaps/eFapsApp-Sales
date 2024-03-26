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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.BooleanUtils;
import org.efaps.admin.datamodel.ui.IUIValue;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.products.Cost;
import org.efaps.esjp.sales.Costs;
import org.efaps.esjp.sales.report.DocPositionReport_Base.UnitPriceMessuredExpression;
import org.efaps.esjp.sales.report.filter.CostTypeFilterValue;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.base.expression.AbstractSimpleExpression;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabMeasureBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabRowGroupBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabVariableBuilder;
import net.sf.dynamicreports.report.constant.Calculation;
import net.sf.dynamicreports.report.definition.ReportParameters;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("63faf41c-d6d4-489b-b8ec-ed46f629f171")
@EFapsApplication("eFapsApp-Sales")
public abstract class DocProductTransactionCostReport_Base
    extends DocProductTransactionReport
{

    /**
     * Gets the cost type field value.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the cost type field value
     * @throws EFapsException on error
     */
    public Return getCostTypeFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final IUIValue value = (IUIValue) _parameter.get(ParameterValues.UIOBJECT);
        final String key = value.getField().getName();
        final Map<String, Object> map = getFilterMap(_parameter);
        ret.put(ReturnValues.VALUES, CostTypeFilterValue.getCostTypePositions(_parameter, (CostTypeFilterValue) map
                        .get(key), Currency.getAvailable().toArray(new Instance[Currency.getAvailable().size()])));
        return ret;
    }

    @Override
    protected Properties getProperties4TypeList(final Parameter _parameter,
                                                final String _fieldName)
        throws EFapsException
    {
        return Sales.REPORT_DOCPRODTRANSCOST.get();
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
        return new DynDocProductTransactionCostReport(this);
    }

    /**
     * The Class DynIndividualProductDocReport.
     */
    public static class DynDocProductTransactionCostReport
        extends DynDocProductTransactionReport
    {

        /**
         * Instantiates a new dyn doc product transaction report.
         *
         * @param _filteredReport the filtered report
         */
        public DynDocProductTransactionCostReport(final DocProductTransactionCostReport_Base _filteredReport)
        {
            super(_filteredReport);
        }

        @Override
        protected void addColumnDefinition(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final Map<String, Object> filters = getFilteredReport().getFilterMap(_parameter);

            final CrosstabBuilder crosstab = DynamicReports.ctab.crosstab();

            final CrosstabMeasureBuilder<BigDecimal> quantityMeasure = DynamicReports.ctab.measure(
                            getFilteredReport().getDBProperty("Quantity"), "quantity", BigDecimal.class,
                            Calculation.SUM);
            crosstab.addMeasure(quantityMeasure);

            final CrosstabRowGroupBuilder<String> docNameRowGroup = DynamicReports.ctab
                            .rowGroup("docName", String.class);
            final CrosstabRowGroupBuilder<String> docContactRowGroup = DynamicReports.ctab
                        .rowGroup("docContact", String.class);
            final CrosstabRowGroupBuilder<String> productRowGroup = DynamicReports.ctab
                        .rowGroup("product", String.class);
            final CrosstabRowGroupBuilder<String> productDescrRowGroup = DynamicReports.ctab
                        .rowGroup("productDescr", String.class)
                        .setHeaderWidth(150).setShowTotal(false);
            final CrosstabRowGroupBuilder<String> uomRowGroup = DynamicReports.ctab
                        .rowGroup("uoM", String.class).setShowTotal(false);

            final GroupByFilterValue groupBy = (GroupByFilterValue) filters.get("docGroup");
            if (groupBy != null) {
                final List<Enum<?>> selected = groupBy.getObject();
                for (final Enum<?> sel : selected) {
                    switch ((DocGroup) sel) {
                        case DOC:
                            crosstab.addRowGroup(docNameRowGroup);
                            break;
                        case CONTACT:
                            crosstab.addRowGroup(docContactRowGroup);
                            break;
                        case PROD:
                        default:
                            crosstab.addRowGroup(productRowGroup, productDescrRowGroup, uomRowGroup);
                            break;
                    }
                }
            }

            final CrosstabColumnGroupBuilder<String> partialColumnGroup = DynamicReports.ctab.columnGroup("partial",
                            String.class);
            crosstab.addColumnGroup(partialColumnGroup);

            final CostTypeFilterValue filterValue = (CostTypeFilterValue) filters.get("costType");
            final String symbol;
            if (filterValue != null) {
                final Instance alterInst = Instance.get(filterValue.getObject());
                if (InstanceUtils.isValid(alterInst)) {
                    symbol = CurrencyInst.get(alterInst).getSymbol();
                } else {
                    symbol = CurrencyInst.get(Currency.getBaseCurrency()).getSymbol();
                }
            } else {
                symbol = CurrencyInst.get(Currency.getBaseCurrency()).getSymbol();
            }

            final CrosstabMeasureBuilder<BigDecimal> costMeasure = DynamicReports.ctab.measure(
                            DBProperties.getProperty(DocPositionCostReport.class.getName() + ".cost") + " " + symbol,
                            "cost", BigDecimal.class, Calculation.SUM);
            crosstab.addMeasure(costMeasure);

            if (filters.containsKey("unitCost") &&  BooleanUtils.isTrue((Boolean) filters.get("unitCost"))) {
                final CrosstabVariableBuilder<BigDecimal> unitPriceVariable = DynamicReports.ctab.variable(
                                new UnitCostExpression(), Calculation.NOTHING);
                crosstab.variables(unitPriceVariable);
                crosstab.addMeasure(DynamicReports.ctab.measure(
                                getFilteredReport().getDBProperty("unitCostMeasure")  + " " + symbol,
                                new UnitPriceMessuredExpression(unitPriceVariable))
                                .setDataType(DynamicReports.type.bigDecimalType()));
            }
            _builder.addSummary(crosstab);
        }

        /**
         * Gets the properties.
         *
         * @return the properties
         * @throws EFapsException on error
         */
        @Override
        protected Properties getProperties()
            throws EFapsException
        {
            return Sales.REPORT_DOCPRODTRANSCOST.get();
        }

        /**
         * Getter method for the instance variable {@link #filteredReport}.
         *
         * @return value of instance variable {@link #filteredReport}
         */
        @Override
        public DocProductTransactionCostReport_Base getFilteredReport()
        {
            return (DocProductTransactionCostReport_Base) super.getFilteredReport();
        }

        /**
         * Gets the bean.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the bean
         * @throws EFapsException on error
         */
        @Override
        public DataBean getBean(final Parameter _parameter)
            throws EFapsException
        {
            final Map<String, Object> filters = getFilteredReport().getFilterMap(_parameter);
            final CostTypeFilterValue filterValue = (CostTypeFilterValue) filters.get("costType");
            return new DataCostBean(_parameter, filterValue);
        }
    }

    /**
     * The Class DataBean.
     */
    public static class DataCostBean
        extends DataBean
    {
        /** The parameter. */
        private final Parameter parameter;

        /** The cost instance. */
        private final CostTypeFilterValue filterValue;

        /**
         * Instantiates a new data cost bean.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _filterValue the filter value
         */
        public DataCostBean(final Parameter _parameter,
                            final CostTypeFilterValue _filterValue)
        {
            this.parameter = _parameter;
            this.filterValue = _filterValue;
        }

        /**
         * Gets the cost.
         *
         * @return the cost
         * @throws EFapsException on error
         */
        public BigDecimal getCost()
            throws EFapsException
        {
            BigDecimal cost = null;
            final Instance tmpInst = Instance.get(this.filterValue.getObject());
            if (this.filterValue.isAlternative() && InstanceUtils.isKindOf(tmpInst, CIERP.Currency)) {
                cost = Cost.getAlternativeCost4Currency(this.parameter, getDate(), tmpInst,
                                getProductInst(), tmpInst);
            } else if (this.filterValue.isAcquisition() && InstanceUtils.isKindOf(tmpInst, CIERP.Currency)) {
                cost = Costs.getAcquisitionCost(this.parameter, getProductInst(), getDocInst(), tmpInst);
            } else {
                cost = Cost.getCost4Currency(this.parameter, getDate(), getProductInst(), Currency
                                .getBaseCurrency());
            }
            return getQuantity().multiply(cost == null ? BigDecimal.ZERO : cost);
        }
    }

    /**
     * The Class UnitPriceMeasureExpression.
     *
     * @author The eFaps Team
     */
    public static class UnitCostExpression
        extends AbstractSimpleExpression<BigDecimal>
    {

        /** The Constant serialVersionUID. */
        private static final long serialVersionUID = 1L;

        @Override
        public BigDecimal evaluate(final ReportParameters _reportParameters)
        {
            final BigDecimal quantity = _reportParameters.getValue("quantity");
            final BigDecimal amount = _reportParameters.getValue("cost");
            return amount.divide(quantity, 4, RoundingMode.HALF_UP);
        }
    }

    /**
     * The Class UnitPriceMeasureExpression.
     *
     * @author The eFaps Team
     */
    public static class UnitCostMessuredExpression
        extends AbstractSimpleExpression<BigDecimal>
    {
        /** The Constant serialVersionUID. */
        private static final long serialVersionUID = 1L;

        /** The unit price variable. */
        private final CrosstabVariableBuilder<BigDecimal> unitPriceVariable;

        /**
         * Instantiates a new unit price messured expression.
         *
         * @param _unitPriceVariable the unit price variable
         */
        public UnitCostMessuredExpression(final CrosstabVariableBuilder<BigDecimal> _unitPriceVariable)
        {
            this.unitPriceVariable = _unitPriceVariable;
        }

        @Override
        public BigDecimal evaluate(final ReportParameters _reportParameters)
        {
            return _reportParameters.getValue(this.unitPriceVariable);
        }
    }
}
