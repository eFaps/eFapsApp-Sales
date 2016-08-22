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

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.ui.IUIValue;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractCommand;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.subtotal.AggregationSubtotalBuilder;
import net.sf.dynamicreports.report.constant.HorizontalAlignment;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("e04923c1-1095-4be3-acb9-6ac2d3556d0a")
@EFapsApplication("eFapsApp-Sales")
public abstract class ProductABCReport_Base
    extends FilteredReport
{

    /**
     * The Enum ReportType.
     */
    public enum ReportType
    {

        /** The provider. */
        PROVIDER,

        /** The product. */
        PRODUCT;
    }

    private ReportType reportType;

    protected void setReportType(final ReportType _reportType)
    {
        this.reportType = _reportType;
    }


    @Override
    protected String getCacheKey(final Parameter _parameter)
        throws EFapsException
    {
        if (this.reportType == null) {
            final Object ui = _parameter.get(ParameterValues.UIOBJECT);
            String namestr;
            if (ui instanceof AbstractCommand) {
                namestr = ((AbstractCommand) _parameter.get(ParameterValues.UIOBJECT)).getName();
            } else {
                namestr = ((IUIValue) _parameter.get(ParameterValues.UIOBJECT)).getField().getCollection().getName();
            }
            if (namestr.contains("Provider")) {
                this.reportType = ReportType.PROVIDER;
            } else {
                this.reportType = ReportType.PRODUCT;
            }
        }
        return super.getCacheKey(_parameter) + this.reportType;
    }


    public Return exportReport4Product(final Parameter _parameter)
        throws EFapsException
    {
        setReportType(ReportType.PRODUCT);
        final Return ret = new Return();
        final String mime = getProperty(_parameter, "Mime");
        final AbstractDynamicReport dyRp = getABC4ProductReport(_parameter);
        dyRp.setFileName("ProductABCReport");
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

    public Return exportReport4Provider(final Parameter _parameter)
        throws EFapsException
    {
        setReportType(ReportType.PROVIDER);
        final Return ret = new Return();
        final String mime = getProperty(_parameter, "Mime");
        final AbstractDynamicReport dyRp = getABC4ProviderReport(_parameter);
        dyRp.setFileName("ProductABC4ProviderReport");
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
    public Return generateReport4Provider(final Parameter _parameter)
        throws EFapsException
    {
        setReportType(ReportType.PROVIDER);
        final Return ret = new Return();
        final AbstractDynamicReport dyRp = getABC4ProviderReport(_parameter);
        final String html = dyRp.getHtmlSnipplet(_parameter);
        ret.put(ReturnValues.SNIPLETT, html);
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return Return containing html snipplet
     * @throws EFapsException on error
     */
    public Return generateReport4Product(final Parameter _parameter)
        throws EFapsException
    {
        setReportType(ReportType.PRODUCT);
        final Return ret = new Return();
        final AbstractDynamicReport dyRp = getABC4ProductReport(_parameter);
        final String html = dyRp.getHtmlSnipplet(_parameter);
        ret.put(ReturnValues.SNIPLETT, html);
        return ret;
    }

    protected AbstractDynamicReport getABC4ProductReport(final Parameter _parameter)
        throws EFapsException
    {
        return new ABC4ProductReport();
    }

    protected AbstractDynamicReport getABC4ProviderReport(final Parameter _parameter)
        throws EFapsException
    {
        return new ABC4ProviderReport();
    }

    public class ABC4ProductReport
        extends AbstractDynamicReport
    {

        protected void add2QueryBuilder(final Parameter _parameter,
                                        final QueryBuilder _queryBldr)
                                            throws EFapsException
        {
            final Map<String, Object> filter = getFilterMap(_parameter);
            if (filter.containsKey("dateFrom")) {
                final DateTime date = (DateTime) filter.get("dateFrom");
                _queryBldr.addWhereAttrGreaterValue(CISales.DocumentSumAbstract.Date,
                                date.withTimeAtStartOfDay().minusSeconds(1));
            }
            if (filter.containsKey("dateTo")) {
                final DateTime date = (DateTime) filter.get("dateTo");
                _queryBldr.addWhereAttrLessValue(CISales.DocumentSumAbstract.Date,
                                date.withTimeAtStartOfDay().plusDays(1));
            }
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            JRRewindableDataSource ret;
            if (isCached(_parameter)) {
                ret = getDataSourceFromCache(_parameter);
                try {
                    ret.moveFirst();
                } catch (final JRException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                ret = new DRDataSource("productInst", "productName", "lines", "linesAbc",
                                "items", "itemsAbc", "amount", "amountAbc");

                final Map<String, ProductABC> map = new HashMap<>();
                BigDecimal netPriceSum = BigDecimal.ZERO;
                BigDecimal quantitySum = BigDecimal.ZERO;
                BigDecimal linesSum = BigDecimal.ZERO;
                final QueryBuilder attrQueryBldr = getQueryBldrFromProperties(_parameter,
                                Sales.REPORT_PRODUCTABC4PROD.get());
                add2QueryBuilder(_parameter, attrQueryBldr);

                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.DocumentSumAbstract.ID);
                final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionSumAbstract);
                queryBldr.addWhereAttrInQuery(CISales.PositionSumAbstract.DocumentAbstractLink, attrQuery);

                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CISales.PositionSumAbstract.Quantity,
                                CISales.PositionSumAbstract.NetPrice,
                                CISales.PositionSumAbstract.UoM);

                final SelectBuilder selProductInst = new SelectBuilder()
                                .linkto(CISales.PositionSumAbstract.Product).instance();
                final SelectBuilder selProductName = new SelectBuilder()
                                .linkto(CISales.PositionSumAbstract.Product)
                                .attribute(CIProducts.ProductAbstract.Name);
                final SelectBuilder selProductDesc = new SelectBuilder()
                                .linkto(CISales.PositionSumAbstract.Product)
                                .attribute(CIProducts.ProductAbstract.Description);

                multi.addSelect(selProductInst, selProductName, selProductDesc);
                multi.execute();
                linesSum = linesSum.add(new BigDecimal(multi.getInstanceList().size()));
                while (multi.next()) {
                    final Instance productInst = multi.<Instance>getSelect(selProductInst);
                    final String productName = multi.<String>getSelect(selProductName);
                    final String productDesc = multi.<String>getSelect(selProductDesc);
                    final UoM uoM = Dimension.getUoM(multi.<Long>getAttribute(CISales.PositionSumAbstract.UoM));
                    final BigDecimal quantity = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.Quantity)
                                    .multiply(new BigDecimal(uoM.getNumerator()))
                                    .divide(new BigDecimal(uoM.getDenominator()), 4, BigDecimal.ROUND_HALF_UP);
                    final BigDecimal netPrice = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.NetPrice);
                    if (map.containsKey(productInst.getOid())) {
                        final ProductABC prodAbc = map.get(productInst.getOid());
                        prodAbc.addElements(quantity, netPrice);
                    } else {
                        final ProductABC prodAbc = new ProductABC(productInst, productName,
                                        productDesc, quantity, netPrice);
                        map.put(productInst.getOid(), prodAbc);
                    }
                    netPriceSum = netPriceSum.add(netPrice);
                    quantitySum = quantitySum.add(quantity);
                }

                final List<ProductABC> lst = new ArrayList<>();
                lst.addAll(map.values());

                setLinesAbc(lst, linesSum);
                setItemsAbc(lst, quantitySum);
                setAmountAbc(lst, netPriceSum);

                Collections.sort(lst, new Comparator<ProductABC>()
                {

                    @Override
                    public int compare(final ProductABC _o1,
                                       final ProductABC _o2)
                    {
                        final int ret;
                        final String name1 = _o1.getName();
                        final String name2 = _o2.getName();
                        ret = name1.compareTo(name2);
                        return ret;
                    }
                });

                for (final ProductABC prod : lst) {
                   ((DRDataSource) ret).add(prod.getInstance(),
                                    prod.getName() + ": " + prod.getDesc(),
                                    prod.getLines(),
                                    prod.getLinesABC(),
                                    prod.getItems(),
                                    prod.getItemsABC(),
                                    prod.getAmount(),
                                    prod.getAmountABC());
                }
                cache(_parameter, ret);
            }
            return ret;
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final Instance baseInst = Currency.getBaseCurrency();
            final CurrencyInst curInst = new CurrencyInst(baseInst);
            final TextColumnBuilder<String> productColumn  = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.report.ABC4ProductReport.ProductName"), "productName",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<BigDecimal> amountColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.report.ABC4ProductReport.Amount")
                                        + " (" + curInst.getSymbol() + ")", "amount",
                            DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<String> amountAbcColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.report.ABC4ProductReport.AmountAbc"), "amountAbc",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<BigDecimal> itemsColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.report.ABC4ProductReport.Items"), "items",
                            DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<String> itemsAbcColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.report.ABC4ProductReport.ItemsAbc"), "itemsAbc",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<BigDecimal> linesColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.report.ABC4ProductReport.Lines"), "lines",
                            DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<String> linesAbcColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.report.ABC4ProductReport.LinesAbc"), "linesAbc",
                            DynamicReports.type.stringType());

            final AggregationSubtotalBuilder<BigDecimal> itemsSum = DynamicReports.sbt.sum(itemsColumn);
            final AggregationSubtotalBuilder<BigDecimal> amountSum = DynamicReports.sbt.sum(amountColumn);
            final AggregationSubtotalBuilder<BigDecimal> linesSum = DynamicReports.sbt.sum(linesColumn);

            _builder.addColumn(productColumn.setFixedWidth(300),
                                linesColumn,
                                linesAbcColumn.setHorizontalAlignment(HorizontalAlignment.CENTER),
                                itemsColumn,
                                itemsAbcColumn.setHorizontalAlignment(HorizontalAlignment.CENTER),
                                amountColumn,
                                amountAbcColumn.setHorizontalAlignment(HorizontalAlignment.CENTER));
            _builder.addSubtotalAtSummary(linesSum, itemsSum, amountSum);
        }
    }

    public class ABC4ProviderReport
        extends AbstractDynamicReport
    {
        protected void add2QueryBuilder(final Parameter _parameter,
                                        final QueryBuilder _queryBldr)
                                            throws EFapsException
        {
            final Map<String, Object> filter = getFilterMap(_parameter);
            if (filter.containsKey("dateFrom")) {
                final DateTime date = (DateTime) filter.get("dateFrom");
                _queryBldr.addWhereAttrGreaterValue(CISales.DocumentSumAbstract.Date,
                                date.withTimeAtStartOfDay().minusSeconds(1));
            }
            if (filter.containsKey("dateTo")) {
                final DateTime date = (DateTime) filter.get("dateTo");
                _queryBldr.addWhereAttrLessValue(CISales.DocumentSumAbstract.Date,
                                date.withTimeAtStartOfDay().plusDays(1));
            }
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            JRRewindableDataSource ret;
            if (isCached(_parameter)) {
                ret = getDataSourceFromCache(_parameter);
                try {
                    ret.moveFirst();
                } catch (final JRException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                ret = new DRDataSource("contactInst", "contactName", "lines", "linesAbc",
                                                                "amount", "amountAbc");

                final Map<String, ProductABC> map = new HashMap<>();
                BigDecimal netPriceSum = BigDecimal.ZERO;
                BigDecimal linesSum = BigDecimal.ZERO;

                final QueryBuilder attrQueryBldr = getQueryBldrFromProperties(_parameter,
                                Sales.REPORT_PRODUCTABC4PROV.get());
                add2QueryBuilder(_parameter, attrQueryBldr);
                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.DocumentSumAbstract.ID);

                final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionSumAbstract);
                queryBldr.addWhereAttrInQuery(CISales.PositionSumAbstract.DocumentAbstractLink, attrQuery);

                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CISales.PositionSumAbstract.NetPrice);
                final SelectBuilder selContactInst = new SelectBuilder()
                                        .linkto(CISales.PositionSumAbstract.DocumentAbstractLink)
                                        .linkto(CISales.DocumentSumAbstract.Contact).instance();
                final SelectBuilder selContactName = new SelectBuilder()
                                        .linkto(CISales.PositionSumAbstract.DocumentAbstractLink)
                                        .linkto(CISales.DocumentSumAbstract.Contact)
                                        .attribute(CIContacts.Contact.Name);

                multi.addSelect(selContactInst, selContactName);
                multi.execute();

                while (multi.next()) {
                    final BigDecimal netPrice = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.NetPrice);
                    final Instance contactInst = multi.<Instance>getSelect(selContactInst);
                    final String contactName = multi.<String>getSelect(selContactName);

                    if (map.containsKey(contactInst.getOid())) {
                        final ProductABC prod = map.get(contactInst.getOid());
                        prod.addElements(BigDecimal.ZERO, netPrice);
                    } else {
                        final ProductABC prod = new ProductABC(contactInst, contactName,
                                                                null, BigDecimal.ZERO, netPrice);
                        map.put(contactInst.getOid(), prod);
                    }
                    netPriceSum = netPriceSum.add(netPrice);
                    linesSum = linesSum.add(BigDecimal.ONE);
                }


                final List<ProductABC> lst = new ArrayList<>();
                lst.addAll(map.values());

                setLinesAbc(lst, linesSum);
                setAmountAbc(lst, netPriceSum);

                Collections.sort(lst, new Comparator<ProductABC>()
                {
                    @Override
                    public int compare(final ProductABC _o1,
                                       final ProductABC _o2)
                    {
                        final int ret;
                        final String name1 = _o1.getName();
                        final String name2 = _o2.getName();
                        ret = name1.compareTo(name2);
                        return ret;
                    }
                });

                for (final ProductABC prod : lst) {
                    ((DRDataSource) ret).add(prod.getInstance(),
                                    prod.getName(),
                                    prod.getLines(),
                                    prod.getLinesABC(),
                                    prod.getAmount(),
                                    prod.getAmountABC());
                }
                cache(_parameter, ret);
            }
            return ret;
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final Instance baseInst = Currency.getBaseCurrency();
            final CurrencyInst curInst = new CurrencyInst(baseInst);
            final TextColumnBuilder<String> contactColumn  = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.report.ABC4ProviderReport.Contact"), "contactName",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<BigDecimal> amountColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.report.ABC4ProviderReport.Amount")
                                        + " (" + curInst.getSymbol() + ")", "amount",
                            DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<String> amountAbcColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.report.ABC4ProviderReport.AmountAbc"), "amountAbc",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<BigDecimal> linesColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.report.ABC4ProviderReport.Lines"), "lines",
                            DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<String> linesAbcColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.report.ABC4ProviderReport.LinesAbc"), "linesAbc",
                            DynamicReports.type.stringType());

            final AggregationSubtotalBuilder<BigDecimal> linesSum = DynamicReports.sbt.sum(linesColumn);
            final AggregationSubtotalBuilder<BigDecimal> netPriceContSum = DynamicReports.sbt.sum(amountColumn);

            _builder.addColumn(contactColumn.setFixedWidth(300),
                            linesColumn,
                            linesAbcColumn.setHorizontalAlignment(HorizontalAlignment.CENTER),
                            amountColumn,
                            amountAbcColumn.setHorizontalAlignment(HorizontalAlignment.CENTER));
            _builder.addSubtotalAtSummary(linesSum, netPriceContSum);
        }

    }

    protected void setLinesAbc(final List<ProductABC> _lst,
                               final BigDecimal _lines)
        throws EFapsException
    {
        Properties props;
        if (this.reportType.equals(ReportType.PRODUCT)) {
            props = Sales.REPORT_PRODUCTABC4PROD.get();
        } else {
            props = Sales.REPORT_PRODUCTABC4PROV.get();
        }
        final String constantA = props.getProperty("PercentA", "80");
        final String constantB = props.getProperty("PercentB", "5");

        final BigDecimal valueA = _lines.multiply(new BigDecimal(constantA)).divide(new BigDecimal(100));
        final BigDecimal valueB = _lines.subtract(_lines.multiply(
                        new BigDecimal(constantB)).divide(new BigDecimal(100)));

        Collections.sort(_lst, new Comparator<ProductABC>()
        {

            @Override
            public int compare(final ProductABC _o1,
                               final ProductABC _o2)
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
        for (final ProductABC prod : _lst) {
            contLines = contLines.add(prod.getLines());
            if (first || contLines.compareTo(valueA) < 0) {
                prod.setLinesABC("A");
                first = false;
            } else if (contLines.compareTo(valueB) < 0) {
                prod.setLinesABC("B");
            } else {
                prod.setLinesABC("C");
            }
        }
    }

    protected void setItemsAbc(final List<ProductABC> _lst,
                               final BigDecimal _items)
        throws EFapsException
    {
        Properties props;
        if (this.reportType.equals(ReportType.PRODUCT)) {
            props = Sales.REPORT_PRODUCTABC4PROD.get();
        } else {
            props = Sales.REPORT_PRODUCTABC4PROV.get();
        }
        final String constantA =  props.getProperty("PercentA", "80");
        final String constantB = props.getProperty("PercentB", "5");

        final BigDecimal valueA = _items.multiply(new BigDecimal(constantA)).divide(new BigDecimal(100));
        final BigDecimal valueB = _items.subtract(_items.multiply(
                        new BigDecimal(constantB)).divide(new BigDecimal(100)));

        Collections.sort(_lst, new Comparator<ProductABC>()
        {
            @Override
            public int compare(final ProductABC _o1,
                               final ProductABC _o2)
            {
                final int ret;
                final BigDecimal item1 = _o1.getItems();
                final BigDecimal item2 = _o2.getItems();
                ret = item2.compareTo(item1);
                return ret;
            }
        });

        BigDecimal contItems = BigDecimal.ZERO;
        boolean first = true;
        for (final ProductABC prod : _lst) {
            contItems = contItems.add(prod.getItems());
            if (first || contItems.compareTo(valueA) < 0) {
                prod.setItemsABC("A");
                first = false;
            } else if (contItems.compareTo(valueB) < 0) {
                prod.setItemsABC("B");
            } else {
                prod.setItemsABC("C");
            }
        }
    }

    protected void setAmountAbc(final List<ProductABC> _lst,
                               final BigDecimal _amount)
        throws EFapsException
    {
        Properties props;
        if (this.reportType.equals(ReportType.PRODUCT)) {
            props = Sales.REPORT_PRODUCTABC4PROD.get();
        } else {
            props = Sales.REPORT_PRODUCTABC4PROV.get();
        }
        final String constantA =  props.getProperty("PercentA", "80");
        final String constantB =  props.getProperty("PercentB", "5");

        final BigDecimal valueA = _amount.multiply(new BigDecimal(constantA))
                                            .divide(new BigDecimal(100));
        final BigDecimal valueB = _amount.subtract(_amount.multiply(new BigDecimal(constantB))
                                            .divide(new BigDecimal(100)));


        Collections.sort(_lst, new Comparator<ProductABC>()
        {
            @Override
            public int compare(final ProductABC _o1,
                               final ProductABC _o2)
            {
                final int ret;
                final BigDecimal amount1 = _o1.getAmount();
                final BigDecimal amount2 = _o2.getAmount();
                ret = amount2.compareTo(amount1);
                return ret;
            }
        });

        BigDecimal contAmount = BigDecimal.ZERO;
        boolean first = true;
        for (final ProductABC prod : _lst) {
            contAmount = contAmount.add(prod.getAmount());
            if (first || contAmount.compareTo(valueA) < 0) {
                prod.setAmountABC("A");
                first = false;
            } else if (contAmount.compareTo(valueB) < 0) {
                prod.setAmountABC("B");
            } else {
                prod.setAmountABC("C");
            }
        }
    }

    public class ProductABC
    {
        private final Instance instance;
        private final String name;
        private final String desc;
        private BigDecimal lines;
        private String linesABC;
        private BigDecimal items;
        private String itemsABC;

        private BigDecimal amount;
        private String amountABC;

        public ProductABC(final Instance _instance,
                          final String _name,
                          final String _desc,
                          final BigDecimal _items,
                          final BigDecimal _amount)
        {
            this.instance = _instance;
            this.name = _name;
            this.desc = _desc;
            this.lines = BigDecimal.ONE;
            this.items = _items;
            this.amount = _amount;
        }

        /**
         * @return the lines
         */
        private BigDecimal getLines()
        {
            return this.lines;
        }

        /**
         * @param lines the lines to set
         */
        private void addElements(final BigDecimal _items,
                                 final BigDecimal _amount)
        {
            this.lines = this.lines.add(BigDecimal.ONE);
            this.items = this.items.add(_items);
            this.amount = this.amount.add(_amount);
        }

        /**
         * @return the linesABC
         */
        private String getLinesABC()
        {
            return this.linesABC;
        }

        /**
         * @param linesABC the linesABC to set
         */
        private void setLinesABC(final String linesABC)
        {
            this.linesABC = linesABC;
        }

        /**
         * @return the items
         */
        private BigDecimal getItems()
        {
            return this.items;
        }

        /**
         * @return the itemsABC
         */
        private String getItemsABC()
        {
            return this.itemsABC;
        }

        public void setItemsABC(final String itemsABC)
        {
            this.itemsABC = itemsABC;
        }

        /**
         * @return the amount
         */
        private BigDecimal getAmount()
        {
            return this.amount;
        }

        /**
         * @return the amountABC
         */
        private String getAmountABC()
        {
            return this.amountABC;
        }

        /**
         * @param amountABC the amountABC to set
         */
        private void setAmountABC(final String amountABC)
        {
            this.amountABC = amountABC;
        }

        /**
         * @return the instance
         */
        private Instance getInstance()
        {
            return this.instance;
        }

        /**
         * @return the name
         */
        private String getName()
        {
            return this.name;
        }

        /**
         * @return the name
         */
        private String getDesc()
        {
            return this.desc;
        }
    }
}
