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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Type;
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
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.erp.Currency;
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
import net.sf.dynamicreports.report.builder.FieldBuilder;
import net.sf.dynamicreports.report.builder.column.PercentageColumnBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.grid.ColumnGridComponentBuilder;
import net.sf.dynamicreports.report.builder.grid.ColumnTitleGroupBuilder;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("e04923c1-1095-4be3-acb9-6ac2d3556d0a")
@EFapsApplication("eFapsApp-Sales")
public abstract class ABCReport_Base
    extends FilteredReport
{

    /**
     * The Enum ReportType.
     */
    public enum ReportType
    {
        /** The provider. */
        CONTACT,
        /** The product. */
        PRODUCT;
    }

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ABCReport.class);

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return Return containing the file
     * @throws EFapsException on error
     */
    public Return exportReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String mime = getProperty(_parameter, "Mime");
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
     * Gets the report.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the report
     * @throws EFapsException on error
     */
    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new DynABCReport(this);
    }

    @Override
    protected Properties getProperties4TypeList(final Parameter _parameter,
                                                final String _fieldName)
        throws EFapsException
    {
        return Sales.REPORT_ABCREPORT.get();
    }

    /**
     * The Class DynABCReport.
     */
    public static class DynABCReport
        extends AbstractDynamicReport
    {

        /** The filtered report. */
        private final ABCReport_Base filteredReport;

        /**
         * Instantiates a new dyn ABC report.
         *
         * @param _filteredReport the filtered report
         */
        public DynABCReport(final ABCReport_Base _filteredReport)
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
                    ABCReport_Base.LOG.error("Catched", e);
                }
            } else {
                final List<DataBean> beans = new ArrayList<>();
                final Map<Instance, DataBean> inst2bean = new HashMap<>();
                final ReportType reportType = getReportType(_parameter);
                final Instance currencyInst = getCurrencyInst(_parameter);

                final QueryBuilder attrQueryBldr = getQueryBldrFromProperties(_parameter, Sales.REPORT_ABCREPORT.get());
                add2QueryBuilder(_parameter, attrQueryBldr);

                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.DocumentSumAbstract.ID);
                final boolean showAmount = isShowAmount(_parameter);
                final QueryBuilder queryBldr;
                if (showAmount) {
                    queryBldr = new QueryBuilder(CISales.PositionSumAbstract);
                } else {
                    queryBldr = new QueryBuilder(CISales.PositionAbstract);
                }
                queryBldr.addWhereAttrInQuery(CISales.PositionAbstract.DocumentAbstractLink, attrQuery);

                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CISales.PositionAbstract.Quantity, CISales.PositionAbstract.UoM);
                if (showAmount) {
                    multi.addAttribute(CISales.PositionSumAbstract.NetPrice, CISales.PositionSumAbstract.CrossPrice,
                                CISales.PositionSumAbstract.RateNetPrice, CISales.PositionSumAbstract.RateCrossPrice,
                                CISales.PositionSumAbstract.RateCurrencyId);
                }
                final SelectBuilder selDocInst = new SelectBuilder().linkto(
                                CISales.PositionAbstract.DocumentAbstractLink).instance();
                final SelectBuilder selDocDate = new SelectBuilder().linkto(
                                CISales.PositionAbstract.DocumentAbstractLink).attribute(CIERP.DocumentAbstract.Date);
                final SelectBuilder selProductInst = new SelectBuilder().linkto(CISales.PositionAbstract.Product)
                                .instance();
                final SelectBuilder selProductName = new SelectBuilder().linkto(CISales.PositionAbstract.Product)
                                .attribute(CIProducts.ProductAbstract.Name);
                final SelectBuilder selProductDesc = new SelectBuilder().linkto(CISales.PositionAbstract.Product)
                                .attribute(CIProducts.ProductAbstract.Description);

                final SelectBuilder selContactInst = new SelectBuilder().linkto(
                                CISales.PositionSumAbstract.DocumentAbstractLink).linkto(
                                                CISales.DocumentSumAbstract.Contact).instance();
                final SelectBuilder selContactName = new SelectBuilder().linkto(
                                CISales.PositionSumAbstract.DocumentAbstractLink).linkto(
                                                CISales.DocumentSumAbstract.Contact).attribute(CIContacts.Contact.Name);

                if (ReportType.PRODUCT.equals(reportType)) {
                    multi.addSelect(selProductInst, selProductName, selProductDesc);
                } else {
                    multi.addSelect(selContactInst, selContactName);
                }
                multi.addSelect(selDocInst, selDocDate);
                multi.execute();
                while (multi.next()) {
                    Instance keyInstance = null;
                    final Instance docInst =  multi.getSelect(selDocInst);
                    if (ReportType.PRODUCT.equals(reportType)) {
                        keyInstance = multi.getSelect(selProductInst);
                    } else {
                        keyInstance = multi.getSelect(selContactInst);
                    }
                    final DataBean bean;
                    if (inst2bean.containsKey(keyInstance)) {
                        bean = inst2bean.get(keyInstance);
                    } else {
                        bean = new DataBean();
                        inst2bean.put(keyInstance, bean);
                        beans.add(bean);
                        if (ReportType.PRODUCT.equals(reportType)) {
                            bean.setProductName(multi.getSelect(selProductName))
                                .setProductDescr(multi.getSelect(selProductDesc))
                                .setUoM(Dimension.getUoM(multi.getAttribute(CISales.PositionAbstract.UoM)));
                        } else {
                            bean.setContactName(multi.getSelect(selContactName));
                        }
                    }
                    if (showAmount) {
                        BigDecimal amount;
                        final Long rateCurrencyId = multi.getAttribute(CISales.PositionSumAbstract.RateCurrencyId);
                        if ("NET".equals(Sales.REPORT_ABCREPORT.get().getProperty(docInst.getType().getName()
                                        + ".Total", "NET"))) {
                            amount = currencyInst.equals(Currency.getBaseCurrency())
                                            ? multi.getAttribute(CISales.PositionSumAbstract.NetPrice)
                                            : multi.getAttribute(CISales.PositionSumAbstract.RateNetPrice);
                        } else {
                            amount = currencyInst.equals(Currency.getBaseCurrency())
                                            ? multi.getAttribute(CISales.PositionSumAbstract.CrossPrice)
                                            : multi.getAttribute(CISales.PositionSumAbstract.RateCrossPrice);
                        }
                        // the amount retrieved is not the wanted
                        if (!currencyInst.equals(Currency.getBaseCurrency())
                                        && rateCurrencyId != currencyInst.getExchangeId()) {
                            final DateTime data = multi.getSelect(selDocDate);
                            final RateInfo[] rates = new Currency().evaluateRateInfos(_parameter, data,
                                            CurrencyInst.get(rateCurrencyId).getInstance(), currencyInst);
                            final BigDecimal rate = RateInfo.getRate(_parameter, rates[2], docInst.getType().getName());
                            amount = amount.divide(rate, 8, RoundingMode.HALF_UP);
                        }
                        bean.addAmount(amount);
                    }
                    if (ReportType.PRODUCT.equals(reportType)) {
                        bean.addQuantity(multi.getAttribute(CISales.PositionAbstract.Quantity),
                                        multi.getAttribute(CISales.PositionAbstract.UoM));
                    }
                    bean.addLine().addDoc(docInst);
                }

                setDocsAbc(_parameter, beans);
                setLinesAbc(_parameter, beans);
                if (ReportType.PRODUCT.equals(reportType)) {
                    setQuantityAbc(_parameter, beans);
                }
                if (showAmount) {
                    setAmountAbc(_parameter, beans);
                }

                ret = new JRBeanCollectionDataSource(beans);
                getFilteredReport().cache(_parameter, ret);
            }
            return ret;
        }

        /**
         * Checks if is show amount.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return true, if is show amount
         * @throws EFapsException on error
         */
        protected boolean isShowAmount(final Parameter _parameter)
            throws EFapsException
        {
            boolean ret = true;
            final Map<String, Object> filterMap = getFilteredReport().getFilterMap(_parameter);
            final List<Type> typeList;
            if (filterMap.containsKey("type")) {
                typeList = new ArrayList<>();
                final TypeFilterValue filters = (TypeFilterValue) filterMap.get("type");
                for (final Long typeid : filters.getObject()) {
                    typeList.add(Type.get(typeid));
                }
            } else {
                typeList = getFilteredReport().getTypeList(_parameter);
            }

            for (final Type type : typeList) {
                if (!type.isKindOf(CISales.DocumentSumAbstract)) {
                    ret = false;
                }
            }
            return ret;
        }

        /**
         * Adds the two query builder.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _queryBldr the query bldr
         * @throws EFapsException on error
         */
        protected void add2QueryBuilder(final Parameter _parameter,
                                        final QueryBuilder _queryBldr)
            throws EFapsException
        {
            final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);
            if (filter.containsKey("dateFrom")) {
                final DateTime date = (DateTime) filter.get("dateFrom");
                _queryBldr.addWhereAttrGreaterValue(CISales.DocumentSumAbstract.Date, date.withTimeAtStartOfDay()
                                .minusSeconds(1));
            }
            if (filter.containsKey("dateTo")) {
                final DateTime date = (DateTime) filter.get("dateTo");
                _queryBldr.addWhereAttrLessValue(CISales.DocumentSumAbstract.Date, date.withTimeAtStartOfDay()
                                .plusDays(1));
            }
            if (filter.containsKey("type")) {
                _queryBldr.addWhereAttrEqValue(CIERP.DocumentAbstract.Type,
                                ((TypeFilterValue) filter.get("type")).getObject().toArray());
            }

            final InstanceFilterValue employeefilter = (InstanceFilterValue) filter.get("employee");
            if (employeefilter != null && employeefilter.getObject() != null && employeefilter.getObject()
                            .isValid()) {
                // HumanResource_Employee2Contact
                final QueryBuilder attrQueryBldr = new QueryBuilder(UUID.fromString(
                                "2f3768b5-ffad-4ed4-a960-2f774e316e21"));
                attrQueryBldr.addWhereAttrEqValue("FromLink", employeefilter.getObject());

                final QueryBuilder queryBldr = new QueryBuilder(CIContacts.Contact);
                queryBldr.addWhereAttrInQuery(CIContacts.Contact.ID, attrQueryBldr.getAttributeQuery("ToLink"));

                _queryBldr.addWhereAttrInQuery(CIERP.DocumentAbstract.Contact,
                                queryBldr.getAttributeQuery(CIContacts.Contact.ID));
            }
        }

        @Override
        protected void addColumnDefinition(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final List<ColumnGridComponentBuilder> grids = new ArrayList<>();
            final ReportType reportType = getReportType(_parameter);
            if (ReportType.PRODUCT.equals(reportType)) {
                final TextColumnBuilder<String> productColumn = DynamicReports.col.column(
                                getFilteredReport().getDBProperty("ProductName"), "productName",
                                DynamicReports.type.stringType());
                final TextColumnBuilder<String> productDescr = DynamicReports.col.column(
                                getFilteredReport().getDBProperty("ProductDescr"), "productDescr",
                                DynamicReports.type.stringType()).setWidth(150);
                final TextColumnBuilder<String> uoMNameColumn = DynamicReports.col.column(
                                getFilteredReport().getDBProperty("UoMName"), "uoMName",
                                DynamicReports.type.stringType());
                _builder.addColumn(productColumn, productDescr, uoMNameColumn);
                Collections.addAll(grids, productColumn, productDescr, uoMNameColumn);
            } else {
                final TextColumnBuilder<String> contactNameColumn = DynamicReports.col.column(
                                getFilteredReport().getDBProperty("ContactName"), "contactName",
                                DynamicReports.type.stringType()).setWidth(250);
                _builder.addColumn(contactNameColumn);
                Collections.addAll(grids, contactNameColumn);
            }

            final TextColumnBuilder<BigDecimal> docsColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty("Docs"), "docs",
                            DynamicReports.type.bigDecimalType());
            final FieldBuilder<BigDecimal> docsField = DynamicReports.field("docs", BigDecimal.class);

            final PercentageColumnBuilder docsPercentColumn = DynamicReports.col.percentageColumn(
                            getFilteredReport().getDBProperty("DocsPercent"), docsField);

            final TextColumnBuilder<String> docsABCColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty("DocsABC"), "docsABC",
                            DynamicReports.type.stringType());

            final ColumnTitleGroupBuilder docsTitleGroup = DynamicReports.grid.titleGroup(getFilteredReport()
                            .getDBProperty("DocsTitleGroup"), docsColumn, docsPercentColumn, docsABCColumn);

            final TextColumnBuilder<BigDecimal> lineColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty("Lines"), "lines",
                            DynamicReports.type.bigDecimalType());
            final FieldBuilder<BigDecimal> linesField = DynamicReports.field("lines", BigDecimal.class);

            final PercentageColumnBuilder linesPercentColumn = DynamicReports.col.percentageColumn(
                            getFilteredReport().getDBProperty("LinesPercent"), linesField);

            final TextColumnBuilder<String> linesABCColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty("LinesABC"), "linesABC",
                            DynamicReports.type.stringType());

            final ColumnTitleGroupBuilder linesTitleGroup = DynamicReports.grid.titleGroup(getFilteredReport()
                            .getDBProperty("LinesTitleGroup"), lineColumn, linesPercentColumn, linesABCColumn);

            Collections.addAll(grids, docsTitleGroup, linesTitleGroup);
            _builder.addColumn(docsColumn, docsPercentColumn, docsABCColumn,
                            lineColumn, linesPercentColumn, linesABCColumn);

            if (ReportType.PRODUCT.equals(reportType)) {
                final TextColumnBuilder<BigDecimal> quantityColumn = DynamicReports.col.column(
                                getFilteredReport().getDBProperty("Quantity"), "quantity",
                                DynamicReports.type.bigDecimalType());
                final FieldBuilder<BigDecimal> quantityField = DynamicReports.field("quantity", BigDecimal.class);

                final PercentageColumnBuilder quantityPercentColumn = DynamicReports.col.percentageColumn(
                                getFilteredReport().getDBProperty("QuantityPercent"), quantityField);

                final TextColumnBuilder<String> quantityABCColumn = DynamicReports.col.column(
                                getFilteredReport().getDBProperty("QuantityABC"), "quantityABC",
                                DynamicReports.type.stringType());

                final ColumnTitleGroupBuilder quantityTitleGroup = DynamicReports.grid.titleGroup(getFilteredReport()
                                .getDBProperty("QuantityTitleGroup"), quantityColumn, quantityPercentColumn,
                                quantityABCColumn);
                Collections.addAll(grids, quantityTitleGroup);
                _builder.addColumn(quantityColumn, quantityPercentColumn, quantityABCColumn);
            }

            if (isShowAmount(_parameter)) {
                final TextColumnBuilder<BigDecimal> amountColumn = DynamicReports.col.column(
                                getFilteredReport().getDBProperty("Amount"), "amount",
                                DynamicReports.type.bigDecimalType());
                final FieldBuilder<BigDecimal> amountField = DynamicReports.field("amount", BigDecimal.class);

                final PercentageColumnBuilder amountPercentColumn = DynamicReports.col.percentageColumn(
                                getFilteredReport().getDBProperty("AmountPercent"), amountField);

                final TextColumnBuilder<String> amountABCColumn = DynamicReports.col.column(
                                getFilteredReport().getDBProperty("AmountABC"), "amountABC",
                                DynamicReports.type.stringType());

                final ColumnTitleGroupBuilder amountTitleGroup = DynamicReports.grid.titleGroup(getFilteredReport()
                                .getDBProperty("AmountTitleGroup"), amountColumn, amountPercentColumn,
                                amountABCColumn);

                Collections.addAll(grids, amountTitleGroup);

                _builder.addColumn(amountColumn, amountPercentColumn, amountABCColumn);
            }
            _builder.columnGrid(grids.toArray(new ColumnGridComponentBuilder[grids.size()]));
        }

        /**
         * Gets the doc Group.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the doc Group
         * @throws EFapsException on error
         */
        protected Instance getCurrencyInst(final Parameter _parameter)
            throws EFapsException
        {
            Instance ret = Currency.getBaseCurrency();
            final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);
            if (filter.containsKey("currency") && filter.get("currency") != null) {
                final CurrencyFilterValue filterValue = (CurrencyFilterValue) filter.get("currency");
                ret = filterValue.getObject();
            }
            return ret;
        }

        /**
         * Gets the doc Group.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the doc Group
         * @throws EFapsException on error
         */
        protected ReportType getReportType(final Parameter _parameter)
            throws EFapsException
        {
            ReportType ret = ReportType.PRODUCT;
            final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);
            if (filter.containsKey("reportType") && filter.get("reportType") != null) {
                final EnumFilterValue filterValue = (EnumFilterValue) filter.get("reportType");
                ret = (ReportType) filterValue.getObject();
            }
            return ret;
        }

        /**
         * Gets the upper limit.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _key the key
         * @param _total the total
         * @return the upper limit
         * @throws EFapsException on error
         */
        protected BigDecimal getUpperLimit(final Parameter _parameter,
                                           final String _key,
                                           final BigDecimal _total)
            throws EFapsException
        {
            final String limit = Sales.REPORT_ABCREPORT.get().getProperty(_key + "UpperLimit", "80");
            return _total.multiply(new BigDecimal(limit)).divide(new BigDecimal(100));
        }

        /**
         * Gets the lower limit.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _key the key
         * @param _total the total
         * @return the lower limit
         * @throws EFapsException on error
         */
        protected BigDecimal getLowerLimit(final Parameter _parameter,
                                           final String _key,
                                           final BigDecimal _total)
            throws EFapsException
        {
            final String limit = Sales.REPORT_ABCREPORT.get().getProperty(_key + "LowerLimit", "5");
            return _total.subtract(_total.multiply(new BigDecimal(limit)).divide(new BigDecimal(100)));
        }

        /**
         * Sets the lines abc.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _beans the beans
         * @throws EFapsException on error
         */
        protected void setDocsAbc(final Parameter _parameter,
                                  final List<DataBean> _beans)
            throws EFapsException
        {
            BigDecimal total = BigDecimal.ZERO;
            for (final DataBean bean : _beans) {
                total = total.add(bean.getDocs());
            }
            final BigDecimal upper = getUpperLimit(_parameter, "Docs", total);
            final BigDecimal lower = getLowerLimit(_parameter, "Docs", total);

            Collections.sort(_beans, new Comparator<DataBean>()
            {

                @Override
                public int compare(final DataBean _o1,
                                   final DataBean _o2)
                {
                    final int ret;
                    final BigDecimal line1 = _o1.getDocs();
                    final BigDecimal line2 = _o2.getDocs();
                    ret = line2.compareTo(line1);
                    return ret;
                }
            });

            BigDecimal contLines = BigDecimal.ZERO;
            boolean first = true;
            for (final DataBean bean : _beans) {
                contLines = contLines.add(bean.getDocs());
                if (first || contLines.compareTo(upper) < 0) {
                    bean.setDocsABC("A");
                    first = false;
                } else if (contLines.compareTo(lower) < 0) {
                    bean.setDocsABC("B");
                } else {
                    bean.setDocsABC("C");
                }
            }
        }

        /**
         * Sets the lines abc.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _beans the beans
         * @throws EFapsException on error
         */
        protected void setLinesAbc(final Parameter _parameter,
                                   final List<DataBean> _beans)
            throws EFapsException
        {
            BigDecimal total = BigDecimal.ZERO;
            for (final DataBean bean : _beans) {
                total = total.add(bean.getLines());
            }
            final BigDecimal upper = getUpperLimit(_parameter, "Lines", total);
            final BigDecimal lower = getLowerLimit(_parameter, "Lines", total);

            Collections.sort(_beans, new Comparator<DataBean>()
            {

                @Override
                public int compare(final DataBean _o1,
                                   final DataBean _o2)
                {
                    final int ret;
                    final BigDecimal line1 = _o1.getLines();
                    final BigDecimal line2 = _o2.getLines();
                    ret = line2.compareTo(line1);
                    return ret;
                }
            });

            BigDecimal contLines = BigDecimal.ZERO;
            boolean first = true;
            for (final DataBean bean : _beans) {
                contLines = contLines.add(bean.getLines());
                if (first || contLines.compareTo(upper) < 0) {
                    bean.setLinesABC("A");
                    first = false;
                } else if (contLines.compareTo(lower) < 0) {
                    bean.setLinesABC("B");
                } else {
                    bean.setLinesABC("C");
                }
            }
        }

        /**
         * Sets the lines abc.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _beans the beans
         * @throws EFapsException on error
         */
        protected void setAmountAbc(final Parameter _parameter,
                                      final List<DataBean> _beans)
            throws EFapsException
        {
            BigDecimal total = BigDecimal.ZERO;
            for (final DataBean bean : _beans) {
                total = total.add(bean.getAmount());
            }
            final BigDecimal upper = getUpperLimit(_parameter, "Amount", total);
            final BigDecimal lower = getLowerLimit(_parameter, "Amount", total);

            Collections.sort(_beans, new Comparator<DataBean>()
            {

                @Override
                public int compare(final DataBean _o1,
                                   final DataBean _o2)
                {
                    final int ret;
                    final BigDecimal line1 = _o1.getAmount();
                    final BigDecimal line2 = _o2.getAmount();
                    ret = line2.compareTo(line1);
                    return ret;
                }
            });

            BigDecimal contAmount = BigDecimal.ZERO;
            boolean first = true;
            for (final DataBean bean : _beans) {
                contAmount = contAmount.add(bean.getAmount());
                if (first || contAmount.compareTo(upper) < 0) {
                    bean.setAmountABC("A");
                    first = false;
                } else if (contAmount.compareTo(lower) < 0) {
                    bean.setAmountABC("B");
                } else {
                    bean.setAmountABC("C");
                }
            }
        }

        /**
         * Sets the lines abc.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _beans the beans
         * @throws EFapsException on error
         */
        protected void setQuantityAbc(final Parameter _parameter,
                                      final List<DataBean> _beans)
            throws EFapsException
        {
            BigDecimal total = BigDecimal.ZERO;
            for (final DataBean bean : _beans) {
                total = total.add(bean.getQuantity());
            }
            final BigDecimal upper = getUpperLimit(_parameter, "Quantity", total);
            final BigDecimal lower = getLowerLimit(_parameter, "Quantity", total);

            Collections.sort(_beans, new Comparator<DataBean>()
            {

                @Override
                public int compare(final DataBean _o1,
                                   final DataBean _o2)
                {
                    final int ret;
                    final BigDecimal line1 = _o1.getQuantity();
                    final BigDecimal line2 = _o2.getQuantity();
                    ret = line2.compareTo(line1);
                    return ret;
                }
            });

            BigDecimal contQuantity = BigDecimal.ZERO;
            boolean first = true;
            for (final DataBean bean : _beans) {
                contQuantity = contQuantity.add(bean.getQuantity());
                if (first || contQuantity.compareTo(upper) < 0) {
                    bean.setQuantityABC("A");
                    first = false;
                } else if (contQuantity.compareTo(lower) < 0) {
                    bean.setQuantityABC("B");
                } else {
                    bean.setQuantityABC("C");
                }
            }
        }

        /**
         * Gets the filtered report.
         *
         * @return the filtered report
         */
        public ABCReport_Base getFilteredReport()
        {
            return this.filteredReport;
        }
    }

    /**
     * The Class DataBean.
     *
     * @author The eFaps Team
     */
    public static class DataBean
    {
        /** The product name. */
        private String contactName;

        /** The product name. */
        private String productName;

        /** The product name. */
        private String productDescr;

        /** The uom. */
        private UoM uom;

        /** The lines. */
        private BigDecimal lines = BigDecimal.ZERO;

        /** The quantity. */
        private BigDecimal quantity = BigDecimal.ZERO;

        /** The amount. */
        private BigDecimal amount = BigDecimal.ZERO;

        /** The lines ABC. */
        private String linesABC;

        /** The lines ABC. */
        private String docsABC;

        /** The lines ABC. */
        private String quantityABC;

        /** The lines ABC. */
        private String amountABC;

        /** The doc inst. */
        private final Set<Instance> docInst = new HashSet<>();

        /**
         * Gets the product name.
         *
         * @return the product name
         */
        public String getProductName()
        {
            return this.productName;
        }

        /**
         * Sets the product name.
         *
         * @param _productName the product name
         * @return the data bean
         */
        public DataBean setProductName(final String _productName)
        {
            this.productName = _productName;
            return this;
        }

        /**
         * Gets the product name.
         *
         * @return the product name
         */
        public String getProductDescr()
        {
            return this.productDescr;
        }

        /**
         * Sets the product descr.
         *
         * @param _productDescr the product descr
         * @return the data bean
         */
        public DataBean setProductDescr(final String _productDescr)
        {
            this.productDescr = _productDescr;
            return this;
        }

        /**
         * Gets the lines.
         *
         * @return the lines
         */
        public BigDecimal getLines()
        {
            return this.lines;
        }

        /**
         * Sets the lines.
         *
         * @param _lines the lines
         * @return the data bean
         */
        public DataBean setLines(final BigDecimal _lines)
        {
            this.lines = _lines;
            return this;
        }

        /**
         * Adds the line.
         *
         * @return the data bean
         */
        public DataBean addLine()
        {
            this.lines = this.lines.add(BigDecimal.ONE);
            return this;
        }

        /**
         * Gets the quantity.
         *
         * @return the quantity
         */
        public BigDecimal getQuantity()
        {
            return this.quantity;
        }

        /**
         * Sets the quantity.
         *
         * @param _quantity the quantity
         * @return the data bean
         */
        public DataBean setQuantity(final BigDecimal _quantity)
        {
            this.quantity = _quantity;
            return this;
        }

        /**
         * Adds the quantity.
         *
         * @param _quantity the quantity
         * @param _uomId the uom id
         * @return the data bean
         */
        public DataBean addQuantity(final BigDecimal _quantity,
                                    final Long _uomId)
        {
            final BigDecimal toAdd;
            if (getUoM().getId() == _uomId) {
                toAdd = _quantity;
            } else {
                if (getUoM().getId() != getUoM().getDimension().getBaseUoM().getId()) {
                    this.quantity = this.quantity.multiply(new BigDecimal(getUoM().getNumerator())).setScale(8,
                                    BigDecimal.ROUND_HALF_UP).divide(new BigDecimal(getUoM().getDenominator()),
                                                    BigDecimal.ROUND_HALF_UP);
                }
                final UoM uoM = Dimension.getUoM(_uomId);
                toAdd = _quantity.multiply(new BigDecimal(uoM.getNumerator())).setScale(8, BigDecimal.ROUND_HALF_UP)
                                .divide(new BigDecimal(uoM.getDenominator()), BigDecimal.ROUND_HALF_UP);
            }
            this.quantity = this.quantity.add(toAdd);
            return this;
        }

        /**
         * Gets the uo M.
         *
         * @return the uo M
         */
        public UoM getUoM()
        {
            return this.uom;
        }

        /**
         * Sets the uo M.
         *
         * @param _uom the uom
         * @return the data bean
         */
        public DataBean setUoM(final UoM _uom)
        {
            this.uom = _uom;
            return this;
        }

        /**
         * Gets the uo M name.
         *
         * @return the uo M name
         */
        public String getUoMName()
        {
            return getUoM().getName();
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
         * Adds the amount.
         *
         * @param _amount the amount
         * @return the data bean
         */
        public DataBean addAmount(final BigDecimal _amount)
        {
            this.amount = this.amount.add(_amount);
            return this;
        }

        /**
         * Gets the lines ABC.
         *
         * @return the lines ABC
         */
        public String getLinesABC()
        {
            return this.linesABC;
        }

        /**
         * Sets the lines ABC.
         *
         * @param _linesABC the lines ABC
         * @return the data bean
         */
        public DataBean setLinesABC(final String _linesABC)
        {
            this.linesABC = _linesABC;
            return this;
        }

        /**
         * Gets the lines ABC.
         *
         * @return the lines ABC
         */
        public String getQuantityABC()
        {
            return this.quantityABC;
        }

        /**
         * Sets the quantity ABC.
         *
         * @param _quantityABC the quantity ABC
         * @return the data bean
         */
        public DataBean setQuantityABC(final String _quantityABC)
        {
            this.quantityABC = _quantityABC;
            return this;
        }

        /**
         * Gets the lines ABC.
         *
         * @return the lines ABC
         */
        public String getAmountABC()
        {
            return this.amountABC;
        }

        /**
         * Sets the amount ABC.
         *
         * @param _amountABC the amount ABC
         * @return the data bean
         */
        public DataBean setAmountABC(final String _amountABC)
        {
            this.amountABC = _amountABC;
            return this;
        }

        /**
         * Gets the product name.
         *
         * @return the product name
         */
        public String getContactName()
        {
            return this.contactName;
        }

        /**
         * Sets the contact name.
         *
         * @param _contactName the contact name
         * @return the data bean
         */
        public DataBean setContactName(final String _contactName)
        {
            this.contactName = _contactName;
            return this;
        }

        /**
         * Gets the docs.
         *
         * @return the docs
         */
        public BigDecimal getDocs()
        {
            return new BigDecimal(this.docInst.size());
        }

        /**
         * Adds the doc.
         *
         * @param _docInstance the doc instance
         * @return the data bean
         */
        public DataBean addDoc(final Instance _docInstance)
        {
            this.docInst.add(_docInstance);
            return this;
        }

        /**
         * Gets the lines ABC.
         *
         * @return the lines ABC
         */
        public String getDocsABC()
        {
            return this.docsABC;
        }

        /**
         * Sets the docs ABC.
         *
         * @param _docsABC the docs ABC
         * @return the data bean
         */
        public DataBean setDocsABC(final String _docsABC)
        {
            this.docsABC = _docsABC;
            return this;
        }
    }
}
