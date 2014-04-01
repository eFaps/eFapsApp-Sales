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

package org.efaps.esjp.sales;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.base.expression.AbstractSimpleExpression;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.VariableBuilder;
import net.sf.dynamicreports.report.builder.column.ComponentColumnBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.component.GenericElementBuilder;
import net.sf.dynamicreports.report.builder.expression.AbstractComplexExpression;
import net.sf.dynamicreports.report.builder.group.ColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.style.ReportStyleBuilder;
import net.sf.dynamicreports.report.builder.subtotal.AggregationSubtotalBuilder;
import net.sf.dynamicreports.report.constant.Calculation;
import net.sf.dynamicreports.report.constant.HorizontalAlignment;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.jasperreports.engine.JRDataSource;

import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
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
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.ui.wicket.models.EmbeddedLink;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: Account_Base.java 8120 2012-10-26 18:21:34Z jan@moxter.net $
 */
@EFapsUUID("81ca4010-b4ef-40e5-ad0a-55b99db6b617")
@EFapsRevision("$Rev: 8120 $")
public abstract class SalesProductReport_Base
    extends FilteredReport
{
    /**
     * @param _parameter    Parameter as passed by the eFasp API
     * @return Return containing html snipplet
     * @throws EFapsException on error
     */
    public Return generateReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final AbstractDynamicReport dyRp = getReport(_parameter);
        dyRp.setFileName("PurchaseSaleReport");
        final String html = dyRp.getHtmlSnipplet(_parameter);
        ret.put(ReturnValues.SNIPLETT, html);
        return ret;
    }

    public Return exportReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String mime = (String) props.get("Mime");
        final AbstractDynamicReport dyRp = getReport(_parameter);
        dyRp.setFileName("PurchaseSaleReport");
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
        return new BuySellReport(this);
    }

    public static class BuySellReport
        extends AbstractDynamicReport
    {
        protected VariableBuilder<BigDecimal> quantitySum;
        protected VariableBuilder<BigDecimal> netPriceSum;

        private final SalesProductReport_Base filteredReport;

        /**
         * @param _salesProductReport_Base
         */
        public BuySellReport(final SalesProductReport_Base _salesProductReport_Base)
        {
            this.filteredReport = _salesProductReport_Base;
        }

        protected QueryBuilder getQueryBuilder(final Parameter _parameter)
            throws EFapsException
        {
            QueryBuilder ret = null;
            final Map<Integer, String> types = analyseProperty(_parameter, "Type");
            final Map<Integer, String> status = analyseProperty(_parameter, "Status");
            final List<Long> statusList = new ArrayList<Long>();
            for (final Entry<Integer, String> typeEntry : types.entrySet()) {
                final String typeStr = typeEntry.getValue();
                final Type type = isUUID(typeStr) ? Type.get(UUID.fromString(typeStr)) : Type.get(typeStr);
                if (ret == null) {
                    ret = new QueryBuilder(type);
                } else {
                    ret.addType(type);
                }

                if (!status.isEmpty()) {
                    final String statusStr = status.get(typeEntry.getKey());
                    final String[] statusArr = statusStr.split(";");

                    for (final String statusS : statusArr) {

                        final Status statusTmp = Status.find(type.getStatusAttribute().getLink().getUUID(), statusS);
                        if (statusTmp != null) {
                            statusList.add(statusTmp.getId());
                        }

                    }
                }
            }
            if (!statusList.isEmpty()) {
                ret.addWhereAttrEqValue(CISales.DocumentSumAbstract.StatusAbstract,
                                statusList.toArray(new Object[statusList.size()]));
            }
            return ret;
        }


        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final Instance instance = _parameter.getInstance();


            final DRDataSource dataSource = new DRDataSource("docOID", "document", "currency", "contact", "date",
                            "quantity", "netUnitPrice", "netPrice", "contactInst", "productInst", "productName",
                            "statusDoc", "productUoM", "productDiscount");

            final List<Map<String, Object>> lst = new ArrayList<Map<String, Object>>();

            final QueryBuilder attrQueryBldr = getQueryBuilder(_parameter);
            add2QueryBuilder(_parameter, attrQueryBldr);
            if (instance.getType().isKindOf(CIContacts.Contact.getType())) {
                attrQueryBldr.addWhereAttrEqValue(CISales.DocumentSumAbstract.Contact, instance.getId());
            }
            final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.DocumentSumAbstract.ID);

            final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionSumAbstract);
            queryBldr.addWhereAttrInQuery(CISales.PositionSumAbstract.DocumentAbstractLink, attrQuery);
            if (instance.getType().isKindOf(CIProducts.ProductAbstract.getType())) {
                queryBldr.addWhereAttrEqValue(CISales.PositionSumAbstract.Product, instance.getId());
            }
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CISales.PositionSumAbstract.Quantity,
                            CISales.PositionSumAbstract.NetUnitPrice,
                            CISales.PositionSumAbstract.NetPrice,
                            CISales.PositionSumAbstract.Discount,
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
            final SelectBuilder selDocInst = new SelectBuilder()
                                    .linkto(CISales.PositionSumAbstract.DocumentAbstractLink)
                                    .instance();
            final SelectBuilder selDocStatus = new SelectBuilder()
                                    .linkto(CISales.PositionSumAbstract.DocumentAbstractLink)
                                    .attribute(CISales.DocumentSumAbstract.StatusAbstract);

            multi.addSelect(selContactInst, selContactName, selDocDate, selDocType, selDocName,
                            selCurSymbol, selProductInst, selProductName, selProductDesc, selDocInst, selDocStatus);
            multi.execute();
            while (multi.next()) {
                final Map<String, Object> map = new HashMap<String, Object>();
                final Instance productInst = multi.<Instance>getSelect(selProductInst);
                final String productName = multi.<String>getSelect(selProductName);
                final String productDesc = multi.<String>getSelect(selProductDesc);
                final String curSymbol = multi.<String>getSelect(selCurSymbol);
                final BigDecimal quantity = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.Quantity);
                final BigDecimal netUnitPrice = multi.<BigDecimal>getAttribute(
                                CISales.PositionSumAbstract.NetUnitPrice);
                final BigDecimal netPrice = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.NetPrice);
                final UoM uoM = Dimension.getUoM(multi.<Long>getAttribute(CISales.PositionSumAbstract.UoM));
                final Instance contactInst = multi.<Instance>getSelect(selContactInst);
                final String contactName = multi.<String>getSelect(selContactName);
                final DateTime docDate = multi.<DateTime>getSelect(selDocDate);
                final String docName = multi.<String>getSelect(selDocName);
                final Instance docInst = multi.<Instance>getSelect(selDocInst);
                final String docType = multi.<String>getSelect(selDocType);
                final String statusName = Status.get(multi.<Long>getSelect(selDocStatus)).getLabel();
                final BigDecimal discount = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.Discount);
                map.put("docOID", docInst.getOid());
                map.put("document", docType + ": " + docName);
                map.put("currency", curSymbol);
                map.put("contact", contactName);
                map.put("date", docDate.toDate());
                map.put("quantity", quantity.multiply(new BigDecimal(uoM.getNumerator()))
                                    .divide(new BigDecimal(uoM.getDenominator()), 4, BigDecimal.ROUND_HALF_UP));
                map.put("netUnitPrice", netUnitPrice.multiply(new BigDecimal(uoM.getDenominator()))
                                    .divide(new BigDecimal(uoM.getNumerator()), 4, BigDecimal.ROUND_HALF_UP));
                map.put("netPrice", netPrice);
                map.put("contactInst", contactInst);
                map.put("productInst", productInst);
                map.put("productName", productName + ": " + productDesc);
                map.put("statusDoc", statusName);
                map.put("productUoM", uoM.getDimension().getBaseUoM().getName());
                map.put("productDiscount", discount);
                lst.add(map);
            }

            Collections.sort(lst, new Comparator<Map<String, Object>>()
            {
                @Override
                public int compare(final Map<String, Object> _o1,
                                   final Map<String, Object> _o2)
                {
                    final Date date1 = (Date) _o1.get("date");
                    final Date date2 = (Date) _o2.get("date");
                    final int ret;
                    if (date1.equals(date2)) {
                        if (instance.getType().isKindOf(CIProducts.ProductAbstract.getType())) {
                            final String contact1 = (String) _o1.get("contact");
                            final String contact2 = (String) _o2.get("contact");
                            ret = contact1.compareTo(contact2);
                        } else {
                            final String product1 = (String) _o1.get("productName");
                            final String product2 = (String) _o2.get("productName");
                            ret = product1.compareTo(product2);
                        }
                    } else {
                        ret = date1.compareTo(date2);
                    }
                    return ret;
                }
            });

            for (final Map<String, Object> map : lst) {
                dataSource.add(map.get("docOID"),
                                map.get("document"),
                                map.get("currency"),
                                map.get("contact"),
                                map.get("date"),
                                map.get("quantity"),
                                map.get("netUnitPrice"),
                                map.get("netPrice"),
                                map.get("contactInst"),
                                map.get("productInst"),
                                map.get("productName"),
                                map.get("statusDoc"),
                                map.get("productUoM"),
                                map.get("productDiscount"));
            }

            return dataSource;
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
            final Map<String, Object> filter = this.filteredReport.getFilterMap(_parameter);
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

            _queryBldr.addWhereAttrGreaterValue(CISales.DocumentSumAbstract.Date, dateFrom);
            _queryBldr.addWhereAttrLessValue(CISales.DocumentSumAbstract.Date, dateTo.plusDays(1)
                            .withTimeAtStartOfDay());
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
            final boolean oppositeFilter = Boolean.parseBoolean((String) props.get("OppositeFilter"));

            final boolean showDetails = Boolean.parseBoolean((String) props.get("ShowDetails"));

            final TextColumnBuilder<String> contactColumn  = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.SalesProductReport.Contact"), "contact",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> productColumn  = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.SalesProductReport.Produdct"), "productName",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<Date> monthColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.SalesProductReport.FilterDate1"), "date",
                            DynamicReports.type.dateMonthType());
            final TextColumnBuilder<Date> yearColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.SalesProductReport.FilterDate2"), "date",
                            DynamicReports.type.dateYearType());
            final TextColumnBuilder<Date> dateColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.SalesProductReport.Date"), "date",
                            DynamicReports.type.dateType());

            final TextColumnBuilder<BigDecimal> netPriceColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.SalesProductReport.NetPrice"), "netPrice",
                            DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> quantityColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.SalesProductReport.Quantity"), "quantity",
                            DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> netUnitPriceColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.SalesProductReport.NetUnitPrice"), "netUnitPrice",
                            DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<String> currencyColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.SalesProductReport.Currency"), "currency",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> documentColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.SalesProductReport.Document"), "document",
                            DynamicReports.type.stringType());

            final TextColumnBuilder<String> statusColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.SalesProductReport.Status"), "statusDoc",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> uomColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.SalesProductReport.UoM"), "productUoM",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<BigDecimal> discountColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.SalesProductReport.Discount"), "productDiscount",
                            DynamicReports.type.bigDecimalType());

            final ColumnGroupBuilder yearGroup  = DynamicReports.grp.group(yearColumn).groupByDataType();
            final ColumnGroupBuilder monthGroup = DynamicReports.grp.group(monthColumn).groupByDataType();
            final ColumnGroupBuilder contactGroup = DynamicReports.grp.group(contactColumn).groupByDataType();
            final ColumnGroupBuilder productGroup = DynamicReports.grp.group(productColumn).groupByDataType();

            DynamicReports.sbt
                            .customValue(new UnitPriceSubtotal(), netUnitPriceColumn)
                                      .setDataType(DynamicReports.type.bigDecimalType());

            final AggregationSubtotalBuilder<BigDecimal> quantityProdSum = DynamicReports.sbt.sum(quantityColumn);
            final AggregationSubtotalBuilder<BigDecimal> netUnitPriceProdSum = DynamicReports.sbt.sum(netUnitPriceColumn);
            final AggregationSubtotalBuilder<BigDecimal> netPriceProdSum = DynamicReports.sbt.sum(netPriceColumn);

            final AggregationSubtotalBuilder<BigDecimal> quantityTotSum = DynamicReports.sbt.sum(quantityColumn)
                            .setStyle(DynamicReports.stl.style().setBold(true)
                                            .setTopBorder(DynamicReports.stl.penDouble()));
            final AggregationSubtotalBuilder<BigDecimal> netPriceTotSum = DynamicReports.sbt.sum(netPriceColumn)
                            .setStyle(DynamicReports.stl.style().setBold(true)
                                            .setTopBorder(DynamicReports.stl.penDouble()));

            final GenericElementBuilder linkElement = DynamicReports.cmp.genericElement(
                            "http://www.efaps.org", "efapslink")
                            .addParameter(EmbeddedLink.JASPER_PARAMETERKEY, new LinkExpression())
                            .setHeight(12).setWidth(25);

            final ComponentColumnBuilder linkColumn = DynamicReports.col.componentColumn(linkElement).setTitle("");

            this.quantitySum = DynamicReports.variable("quantity", BigDecimal.class, Calculation.SUM);
            this.netPriceSum = DynamicReports.variable("netPrice", BigDecimal.class, Calculation.SUM);

            if (oppositeFilter) {
                if (_parameter.getInstance().getType().isKindOf(CIProducts.ProductAbstract.getType())) {
                    this.quantitySum.setResetGroup(contactGroup);
                    this.netPriceSum.setResetGroup(contactGroup);
                } else {
                    this.quantitySum.setResetGroup(productGroup);
                    this.netPriceSum.setResetGroup(productGroup);
                }
            } else {
                this.quantitySum.setResetGroup(monthGroup);
                this.netPriceSum.setResetGroup(monthGroup);
            }

            _builder.addVariable(this.quantitySum, this.netPriceSum);

            _builder.addColumn(yearColumn,
                                monthColumn);
            if (oppositeFilter) {
                if (_parameter.getInstance().getType().isKindOf(CIProducts.ProductAbstract.getType())) {
                    _builder.addColumn(contactColumn);
                } else {
                    _builder.addColumn(productColumn);
                }
            }
            if (getExType().equals(ExportType.HTML)) {
                _builder.addColumn(linkColumn);
            }
            _builder.addColumn(documentColumn.setFixedWidth(200),
                                statusColumn.setHorizontalAlignment(HorizontalAlignment.CENTER),
                                dateColumn,
                                uomColumn.setHorizontalAlignment(HorizontalAlignment.CENTER).setFixedWidth(70),
                                quantityColumn,
                                netUnitPriceColumn,
                                discountColumn,
                                netPriceColumn,
                                currencyColumn.setHorizontalAlignment(HorizontalAlignment.CENTER).setFixedWidth(70));
            if (!showDetails) {
                _builder.setShowColumnValues(false);
            }
            if (oppositeFilter) {
                if (_parameter.getInstance().getType().isKindOf(CIProducts.ProductAbstract.getType())) {
                    _builder.groupBy(yearGroup, monthGroup, contactGroup);
                    _builder.addSubtotalAtGroupFooter(contactGroup, quantityProdSum);
                    //_builder.addSubtotalAtGroupFooter(contactGroup, unitPriceSbt);
                    _builder.addSubtotalAtGroupFooter(contactGroup, netUnitPriceProdSum);
                } else {
                    _builder.groupBy(yearGroup, monthGroup, productGroup);
                    _builder.addSubtotalAtGroupFooter(productGroup, quantityProdSum);
                    //_builder.addSubtotalAtGroupFooter(productGroup, unitPriceSbt);
                    _builder.addSubtotalAtGroupFooter(productGroup, netUnitPriceProdSum);
                    _builder.addSubtotalAtGroupFooter(productGroup, netPriceProdSum);
                    _builder.addSubtotalAtSummary(quantityTotSum, netPriceTotSum);
                }
            } else {
                _builder.groupBy(yearGroup, monthGroup);
                _builder.addSubtotalAtGroupFooter(monthGroup, quantityProdSum);
                //_builder.addSubtotalAtGroupFooter(monthGroup, unitPriceSbt);
                _builder.addSubtotalAtGroupFooter(monthGroup, netUnitPriceProdSum);
                _builder.addSubtotalAtGroupFooter(monthGroup, netPriceProdSum);
                _builder.addSubtotalAtSummary(quantityTotSum, netPriceTotSum);
            }
            /*_builder.pageFooter(DynamicReports.cmp.pageXofY()
                                            .setStyle(DynamicReports.stl.style().bold()
                                            .setAlignment(HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE)
                                            .setTopBorder(DynamicReports.stl.pen1Point())));*/
        }

        protected class UnitPriceSubtotal
            extends AbstractSimpleExpression<BigDecimal>
        {
            private static final long serialVersionUID = 1L;

            @Override
            public BigDecimal evaluate(final ReportParameters reportParameters)
            {
                final BigDecimal quantitySumValue = reportParameters.getValue(BuySellReport.this.quantitySum);
                final BigDecimal priceSumValue = reportParameters.getValue(BuySellReport.this.netPriceSum);

                final BigDecimal total = priceSumValue.divide(quantitySumValue, 4, BigDecimal.ROUND_HALF_UP);
                return total;
            }
        }

        @Override
        protected ReportStyleBuilder getSubtotalStyle4Html(final Parameter _parameter)
            throws EFapsException
        {
            return DynamicReports.stl.style().bold()
                            .setTopBorder(DynamicReports.stl.penThin());
        }
    }

    public static class LinkExpression
        extends AbstractComplexExpression<EmbeddedLink>
    {

        private static final long serialVersionUID = 1L;

        public LinkExpression()
        {
            addExpression(DynamicReports.field("docOID", String.class));
        }

        @Override
        public EmbeddedLink evaluate(final List<?> _values,
                                     final ReportParameters _reportParameters)
        {
            final String oid = (String) _values.get(0);
            return EmbeddedLink.getJasperLink(oid);
        }
    }
}
