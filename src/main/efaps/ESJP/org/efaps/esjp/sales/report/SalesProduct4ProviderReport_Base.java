/*
 * Copyright 2003 - 2010 The eFaps Team
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
 * Revision:        $Rev: 8120 $
 * Last Changed:    $Date: 2012-10-26 13:21:34 -0500 (vie, 26 oct 2012) $
 * Last Changed By: $Author: jan@moxter.net $
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

import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
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
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.base.expression.AbstractSimpleExpression;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.group.ColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.subtotal.AggregationSubtotalBuilder;
import net.sf.dynamicreports.report.constant.HorizontalAlignment;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.jasperreports.engine.JRDataSource;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: Account_Base.java 8120 2012-10-26 18:21:34Z jan@moxter.net $
 */
@EFapsUUID("5b06c70e-9409-4015-871c-7b2d45acad4c")
@EFapsApplication("eFapsApp-Sales")
public abstract class SalesProduct4ProviderReport_Base
{

    public Return exportReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        _parameter.get(ParameterValues.PROPERTIES);
        final String mime = _parameter.getParameterValue("mime");
        final AbstractDynamicReport dyRp = getReport(_parameter);
        dyRp.setFileName("Product4ProviderReport");
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

    public Return exportReport2(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        _parameter.get(ParameterValues.PROPERTIES);
        final String mime = _parameter.getParameterValue("mime");
        final AbstractDynamicReport dyRp = getReport2(_parameter);
        dyRp.setFileName("Product4ProviderReport2");
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

    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new Product4ProviderReport();
    }

    protected AbstractDynamicReport getReport2(final Parameter _parameter)
        throws EFapsException
    {
        return new Product4ProviderReport2();
    }

    public class Product4ProviderReport
        extends AbstractDynamicReport
    {
        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
            final String typesStr = (String) props.get("Types");
            final String[] types = typesStr.split(";");
            final DateTime dateFrom = new DateTime(_parameter.getParameterValue("dateFrom"));
            final DateTime dateTo = new DateTime(_parameter.getParameterValue("dateTo"));

            final DRDataSource dataSource = new DRDataSource("document", "currency", "contact", "date", "quantity",
                            "netPrice", "contactInst", "productInst", "productName", "productDesc", "productNameDesc");

            final List<Map<String, Object>> lst = new ArrayList<>();
            for (final String type : types) {
                final QueryBuilder attrQueryBldr = new QueryBuilder(Type.get(type));
                attrQueryBldr.addWhereAttrGreaterValue(CISales.DocumentSumAbstract.Date, dateFrom.minusDays(1));
                attrQueryBldr.addWhereAttrLessValue(CISales.DocumentSumAbstract.Date, dateTo.plusDays(1));
                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.DocumentSumAbstract.ID);

                final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionSumAbstract);
                queryBldr.addWhereAttrInQuery(CISales.PositionSumAbstract.DocumentAbstractLink, attrQuery);

                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CISales.PositionSumAbstract.Quantity,
                                CISales.PositionSumAbstract.NetUnitPrice,
                                CISales.PositionSumAbstract.NetPrice,
                                CISales.PositionSumAbstract.UoM);
                final SelectBuilder selCurSymbol = new SelectBuilder()
                                        .linkto(CISales.PositionSumAbstract.RateCurrencyId)
                                        .attribute(CIERP.Currency.Symbol);
                final SelectBuilder selContactInst = new SelectBuilder()
                                        .linkto(CISales.PositionSumAbstract.DocumentAbstractLink)
                                        .linkto(CISales.DocumentSumAbstract.Contact).instance();
                final SelectBuilder selProductInst = new SelectBuilder()
                                        .linkto(CISales.PositionSumAbstract.Product).instance();
                final SelectBuilder selProductName = new SelectBuilder()
                                        .linkto(CISales.PositionSumAbstract.Product)
                                        .attribute(CIProducts.ProductAbstract.Name);
                final SelectBuilder selProductDesc = new SelectBuilder()
                                        .linkto(CISales.PositionSumAbstract.Product)
                                        .attribute(CIProducts.ProductAbstract.Description);
                final SelectBuilder selContactName = new SelectBuilder()
                                        .linkto(CISales.PositionSumAbstract.DocumentAbstractLink)
                                        .linkto(CISales.DocumentSumAbstract.Contact)
                                        .attribute(CIContacts.Contact.Name);
                final SelectBuilder selDocDate = new SelectBuilder()
                                        .linkto(CISales.PositionSumAbstract.DocumentAbstractLink)
                                        .attribute(CISales.DocumentSumAbstract.Date);
                final SelectBuilder selDocType = new SelectBuilder()
                                        .linkto(CISales.PositionSumAbstract.DocumentAbstractLink).type().label();
                final SelectBuilder selDocName = new SelectBuilder()
                                        .linkto(CISales.PositionSumAbstract.DocumentAbstractLink)
                                        .attribute(CISales.DocumentSumAbstract.Name);
                multi.addSelect(selContactInst, selContactName, selDocDate, selDocType, selDocName,
                                selCurSymbol, selProductInst, selProductName, selProductDesc);
                multi.execute();
                while (multi.next()) {
                    final Map<String, Object> map = new HashMap<>();
                    final Instance productInst = multi.<Instance>getSelect(selProductInst);
                    final String productName = multi.<String>getSelect(selProductName);
                    final String productDesc = multi.<String>getSelect(selProductDesc);
                    final String curSymbol = multi.<String>getSelect(selCurSymbol);
                    final BigDecimal quantity = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.Quantity);
                    multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.NetUnitPrice);
                    final BigDecimal netPrice = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.NetPrice);
                    final UoM uoM = Dimension.getUoM(multi.<Long>getAttribute(CISales.PositionSumAbstract.UoM));
                    final Instance contactInst = multi.<Instance>getSelect(selContactInst);
                    final String contactName = multi.<String>getSelect(selContactName);
                    final DateTime docDate = multi.<DateTime>getSelect(selDocDate);
                    final String docName = multi.<String>getSelect(selDocName);
                    final String docType = multi.<String>getSelect(selDocType);
                    map.put("document", docType + ": " + docName);
                    map.put("currency", curSymbol);
                    map.put("contact", contactName);
                    map.put("date", docDate.toDate());
                    map.put("quantity", quantity.multiply(new BigDecimal(uoM.getNumerator()))
                                        .divide(new BigDecimal(uoM.getDenominator()), 4, BigDecimal.ROUND_HALF_UP));
                    map.put("netPrice", netPrice);
                    map.put("contactInst", contactInst);
                    map.put("productInst", productInst);
                    map.put("productName", productName);
                    map.put("productDesc", productDesc);
                    map.put("productNameDesc", productName + ": " + productDesc);
                    lst.add(map);
                }
            }

            Collections.sort(lst, new Comparator<Map<String, Object>>(){
                @Override
                public int compare(final Map<String, Object> _o1,
                                   final Map<String, Object> _o2)
                {
                    final int ret;
                    final String contact1 = (String) _o1.get("contact");
                    final String contact2 = (String) _o2.get("contact");
                    if (contact1.equals(contact2)) {
                        final String product1 = (String) _o1.get("productName");
                        final String product2 = (String) _o2.get("productName");
                        ret = product1.compareTo(product2);
                    } else {
                        ret = contact1.compareTo(contact2);
                    }

                    return ret;
                }});

            for (final Map<String, Object> map : lst) {
                dataSource.add(map.get("document"),
                                map.get("currency"),
                                map.get("contact"),
                                map.get("date"),
                                map.get("quantity"),
                                map.get("netPrice"),
                                map.get("contactInst"),
                                map.get("productInst"),
                                map.get("productName"),
                                map.get("productDesc"),
                                map.get("productNameDesc"));
            }

            return dataSource;
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);

            final Instance baseInst = Currency.getBaseCurrency();
            final CurrencyInst curInst = new CurrencyInst(baseInst);
            final boolean showDetails = Boolean.parseBoolean((String) props.get("ShowDetails"));
            final TextColumnBuilder<String> contactColumn  = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.report.SalesProduct4ProviderReport.Contact"), "contact",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> productColumn  = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.report.SalesProduct4ProviderReport.ProductName"), "productName",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> productDescColumn  = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.report.SalesProduct4ProviderReport.ProductDesc"), "productDesc",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> productNameDescColumn  = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.report.SalesProduct4ProviderReport.ProductNameDesc"), "productNameDesc",
                            DynamicReports.type.stringType());
            DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.report.SalesProduct4ProviderReport.Date"), "date",
                            DynamicReports.type.dateType());

            final TextColumnBuilder<BigDecimal> netPriceColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.report.SalesProduct4ProviderReport.NetPrice")
                                        + " (" + curInst.getSymbol() + ")", "netPrice",
                            DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> quantityColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.report.SalesProduct4ProviderReport.Quantity"), "quantity",
                            DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<String> currencyColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.report.SalesProduct4ProviderReport.Currency"), "currency",
                            DynamicReports.type.stringType());

            final ColumnGroupBuilder contactGroup = DynamicReports.grp.group(contactColumn).groupByDataType();
            final ColumnGroupBuilder productGroup = DynamicReports.grp.group(showDetails
                            ? productColumn : productNameDescColumn).groupByDataType()
                            .setStyle(DynamicReports.stl.style().setBold(false)
                                    .setHorizontalAlignment(HorizontalAlignment.LEFT)
                                    .setPadding(DynamicReports.stl.padding(2)));

            final AggregationSubtotalBuilder<BigDecimal> quantitySum = DynamicReports.sbt.sum(quantityColumn)
                            .setStyle(DynamicReports.stl.style().setBold(false));
            final AggregationSubtotalBuilder<BigDecimal> netPriceSum = DynamicReports.sbt.sum(netPriceColumn)
                            .setStyle(DynamicReports.stl.style().setBold(false));
            final AggregationSubtotalBuilder<BigDecimal> netPriceProviderSum = DynamicReports.sbt.sum(netPriceColumn);

            _builder.addColumn(contactColumn);
            if (showDetails) {
                _builder.addColumn(productColumn);
            } else {
                _builder.addColumn(productNameDescColumn);
            }
            _builder.addColumn(productDescColumn.setFixedWidth(400),
                                quantityColumn,
                                netPriceColumn);

            if (!showDetails) {
                _builder.setShowColumnValues(false);
            } else {
                _builder.addColumn(currencyColumn.setHorizontalAlignment(HorizontalAlignment.CENTER).setFixedWidth(70));
            }

            _builder.groupBy(contactGroup, productGroup);
            _builder.addSubtotalAtGroupFooter(productGroup, quantitySum);
            _builder.addSubtotalAtGroupFooter(productGroup, netPriceSum);
            _builder.addSubtotalAtGroupFooter(contactGroup, netPriceProviderSum);
        }
    }

    public class Product4ProviderReport2
        extends AbstractDynamicReport
    {
        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
            final String typesStr = (String) props.get("Types");
            final String[] types = typesStr.split(";");
            final DateTime dateFrom = new DateTime(_parameter.getParameterValue("dateFrom"));
            final DateTime dateTo = new DateTime(_parameter.getParameterValue("dateTo"));

            final DRDataSource dataSource = new DRDataSource("document", "currency", "contact", "date", "quantity",
                            "netPrice", "contactInst", "productInst", "productName", "productDesc", "productNameDesc");

            final List<Map<String, Object>> lst = new ArrayList<>();
            for (final String type : types) {
                final QueryBuilder attrQueryBldr = new QueryBuilder(Type.get(type));
                attrQueryBldr.addWhereAttrGreaterValue(CISales.DocumentSumAbstract.Date, dateFrom.minusDays(1));
                attrQueryBldr.addWhereAttrLessValue(CISales.DocumentSumAbstract.Date, dateTo.plusDays(1));
                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.DocumentSumAbstract.ID);

                final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionSumAbstract);
                queryBldr.addWhereAttrInQuery(CISales.PositionSumAbstract.DocumentAbstractLink, attrQuery);

                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CISales.PositionSumAbstract.Quantity,
                                CISales.PositionSumAbstract.NetUnitPrice,
                                CISales.PositionSumAbstract.NetPrice,
                                CISales.PositionSumAbstract.UoM);
                final SelectBuilder selCurSymbol = new SelectBuilder()
                                        .linkto(CISales.PositionSumAbstract.CurrencyId)
                                        .attribute(CIERP.Currency.Symbol);
                final SelectBuilder selContactInst = new SelectBuilder()
                                        .linkto(CISales.PositionSumAbstract.DocumentAbstractLink)
                                        .linkto(CISales.DocumentSumAbstract.Contact).instance();
                final SelectBuilder selProductInst = new SelectBuilder()
                                        .linkto(CISales.PositionSumAbstract.Product).instance();
                final SelectBuilder selProductName = new SelectBuilder()
                                        .linkto(CISales.PositionSumAbstract.Product)
                                        .attribute(CIProducts.ProductAbstract.Name);
                final SelectBuilder selProductDesc = new SelectBuilder()
                                        .linkto(CISales.PositionSumAbstract.Product)
                                        .attribute(CIProducts.ProductAbstract.Description);
                final SelectBuilder selContactName = new SelectBuilder()
                                        .linkto(CISales.PositionSumAbstract.DocumentAbstractLink)
                                        .linkto(CISales.DocumentSumAbstract.Contact)
                                        .attribute(CIContacts.Contact.Name);
                final SelectBuilder selDocDate = new SelectBuilder()
                                        .linkto(CISales.PositionSumAbstract.DocumentAbstractLink)
                                        .attribute(CISales.DocumentSumAbstract.Date);
                final SelectBuilder selDocType = new SelectBuilder()
                                        .linkto(CISales.PositionSumAbstract.DocumentAbstractLink).type().label();
                final SelectBuilder selDocName = new SelectBuilder()
                                        .linkto(CISales.PositionSumAbstract.DocumentAbstractLink)
                                        .attribute(CISales.DocumentSumAbstract.Name);
                multi.addSelect(selContactInst, selContactName, selDocDate, selDocType, selDocName,
                                selCurSymbol, selProductInst, selProductName, selProductDesc);
                multi.execute();
                while (multi.next()) {
                    final Map<String, Object> map = new HashMap<>();
                    final Instance productInst = multi.<Instance>getSelect(selProductInst);
                    final String productName = multi.<String>getSelect(selProductName);
                    final String productDesc = multi.<String>getSelect(selProductDesc);
                    final String curSymbol = multi.<String>getSelect(selCurSymbol);
                    final BigDecimal quantity = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.Quantity);
                    multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.NetUnitPrice);
                    final BigDecimal netPrice = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.NetPrice);
                    final UoM uoM = Dimension.getUoM(multi.<Long>getAttribute(CISales.PositionSumAbstract.UoM));
                    final Instance contactInst = multi.<Instance>getSelect(selContactInst);
                    final String contactName = multi.<String>getSelect(selContactName);
                    final DateTime docDate = multi.<DateTime>getSelect(selDocDate);
                    final String docName = multi.<String>getSelect(selDocName);
                    final String docType = multi.<String>getSelect(selDocType);
                    map.put("document", docType + ": " + docName);
                    map.put("currency", curSymbol);
                    map.put("contact", contactName);
                    map.put("date", docDate.toDate());
                    map.put("quantity", quantity.multiply(new BigDecimal(uoM.getNumerator()))
                                        .divide(new BigDecimal(uoM.getDenominator()), 4, BigDecimal.ROUND_HALF_UP));
                    map.put("netPrice", netPrice);
                    map.put("contactInst", contactInst);
                    map.put("productInst", productInst);
                    map.put("productName", productName);
                    map.put("productDesc", productDesc);
                    map.put("productNameDesc", productName + ": " + productDesc);
                    lst.add(map);
                }
            }

            Collections.sort(lst, new Comparator<Map<String, Object>>(){
                @Override
                public int compare(final Map<String, Object> _o1,
                                   final Map<String, Object> _o2)
                {
                    final int ret;
                    final String product1 = (String) _o1.get("productName");
                    final String product2 = (String) _o2.get("productName");
                    if (product1.equals(product2)) {
                        final String contact1 = (String) _o1.get("contact");
                        final String contact2 = (String) _o2.get("contact");
                        ret = contact1.compareTo(contact2);
                    } else {
                        ret = product1.compareTo(product2);
                    }

                    return ret;
                }});

            for (final Map<String, Object> map : lst) {
                dataSource.add(map.get("document"),
                                map.get("currency"),
                                map.get("contact"),
                                map.get("date"),
                                map.get("quantity"),
                                map.get("netPrice"),
                                map.get("contactInst"),
                                map.get("productInst"),
                                map.get("productName"),
                                map.get("productDesc"),
                                map.get("productNameDesc"));
            }

            return dataSource;
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
            final Instance baseInst = Currency.getBaseCurrency();
            final CurrencyInst curInst = new CurrencyInst(baseInst);
            final boolean showDetails = Boolean.parseBoolean((String) props.get("ShowDetails"));
            final TextColumnBuilder<String> contactColumn  = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.Product4ProviderReport2.Contact"), "contact",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> productNameDescColumn  = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.Product4ProviderReport2.ProductName"), "productNameDesc",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> emptyColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.Product4ProviderReport2.Contact"), "contact",
                            DynamicReports.type.stringType()).setPrintWhenExpression(new ExpressionColumn());

            final TextColumnBuilder<BigDecimal> netPriceColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.Product4ProviderReport2.NetPrice")
                                    + " (" + curInst.getSymbol() + ")", "netPrice",
                            DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> quantityColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.Product4ProviderReport2.Quantity"), "quantity",
                            DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<String> currencyColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.Product4ProviderReport2.Currency"), "currency",
                            DynamicReports.type.stringType());

            final ColumnGroupBuilder productGroup = DynamicReports.grp.group(productNameDescColumn).groupByDataType();
            final ColumnGroupBuilder contactGroup = DynamicReports.grp.group(contactColumn).groupByDataType()
                            .setStyle(DynamicReports.stl.style().setBold(false)
                                    .setHorizontalAlignment(HorizontalAlignment.LEFT)
                                    .setPadding(DynamicReports.stl.padding(2)));

            final AggregationSubtotalBuilder<BigDecimal> quantityProdSum = DynamicReports.sbt.sum(quantityColumn)
                            .setStyle(DynamicReports.stl.style().setBold(false)
                                            .setTopBorder(DynamicReports.stl.penThin()));
            final AggregationSubtotalBuilder<BigDecimal> netPriceProdSum = DynamicReports.sbt.sum(netPriceColumn)
                            .setStyle(DynamicReports.stl.style().setBold(false)
                                            .setTopBorder(DynamicReports.stl.penThin()));
            final AggregationSubtotalBuilder<BigDecimal> quantityContSum = DynamicReports.sbt.sum(quantityColumn)
                            .setStyle(DynamicReports.stl.style().setBold(false));
            final AggregationSubtotalBuilder<BigDecimal> netPriceContSum = DynamicReports.sbt.sum(netPriceColumn)
                            .setStyle(DynamicReports.stl.style().setBold(false));
            final AggregationSubtotalBuilder<Long> providerDisCount = DynamicReports.sbt.distinctCount(contactColumn)
                            .setShowInColumn(emptyColumn)
                            .setStyle(DynamicReports.stl.style().setBold(false));

            _builder.addColumn(productNameDescColumn,
                            contactColumn,
                            emptyColumn.setFixedWidth(400),
                            quantityColumn,
                            netPriceColumn);

            if (!showDetails) {
                _builder.setShowColumnValues(false);
            } else {
                _builder.addColumn(currencyColumn.setHorizontalAlignment(HorizontalAlignment.CENTER).setFixedWidth(70));
            }

            _builder.groupBy(productGroup, contactGroup);
            _builder.addSubtotalAtGroupFooter(contactGroup, quantityContSum);
            _builder.addSubtotalAtGroupFooter(contactGroup, netPriceContSum);
            _builder.addSubtotalAtGroupFooter(productGroup, quantityProdSum);
            _builder.addSubtotalAtGroupFooter(productGroup, netPriceProdSum);
            _builder.addSubtotalAtGroupFooter(productGroup, providerDisCount);
        }

        private class ExpressionColumn
            extends AbstractSimpleExpression<Boolean>
        {
            private static final long serialVersionUID = 1L;
            @Override
            public Boolean evaluate(final ReportParameters reportParameters)
            {
                return false;
            }
        }
    }
}
