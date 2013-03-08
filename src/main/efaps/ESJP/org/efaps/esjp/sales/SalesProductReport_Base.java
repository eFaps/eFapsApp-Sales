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

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.base.expression.AbstractSimpleExpression;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.VariableBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.group.ColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.subtotal.AggregationSubtotalBuilder;
import net.sf.dynamicreports.report.builder.subtotal.CustomSubtotalBuilder;
import net.sf.dynamicreports.report.constant.Calculation;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.jasperreports.engine.JRDataSource;

import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
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
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
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
{
    public Return generateReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final AbstractDynamicReport dyRp = getReport(_parameter);
        dyRp.setFileName("PurchaseSaleReport");
        final String html = dyRp.getHtml((_parameter));
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
        return new BuySellReport();
    }

    public class BuySellReport
        extends AbstractDynamicReport
    {
        private VariableBuilder<BigDecimal> quantitySum;
        private VariableBuilder<BigDecimal> netPriceSum;

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final Instance instProd = _parameter.getInstance();
            final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
            final String typesStr = (String) props.get("Types");
            final String[] types = typesStr.split(";");
            final Integer yearsAgo = Integer.parseInt((String) props.get("YearsAgo"));

            final DateTime date2Filter = new DateTime().minusYears(yearsAgo);

            final DRDataSource dataSource = new DRDataSource("contact", "date", "quantity", "netUnitPrice", "netPrice", "contactInst");

            final List<Map<String, Object>> lst = new ArrayList<Map<String, Object>>();
            for (final String type : types) {
                final QueryBuilder attrQueryBldr = new QueryBuilder(Type.get(type));
                attrQueryBldr.addWhereAttrGreaterValue(CISales.DocumentSumAbstract.Date, date2Filter);
                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.DocumentSumAbstract.ID);

                final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionSumAbstract);
                queryBldr.addWhereAttrInQuery(CISales.PositionSumAbstract.DocumentAbstractLink, attrQuery);
                queryBldr.addWhereAttrEqValue(CISales.PositionSumAbstract.Product, instProd.getId());
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CISales.PositionSumAbstract.Quantity,
                                CISales.PositionSumAbstract.NetUnitPrice,
                                CISales.PositionSumAbstract.NetPrice,
                                CISales.PositionSumAbstract.UoM);
                final SelectBuilder selContactInst = new SelectBuilder()
                                        .linkto(CISales.PositionSumAbstract.DocumentAbstractLink)
                                        .linkto(CISales.DocumentSumAbstract.Contact).instance();
                final SelectBuilder selContactName = new SelectBuilder()
                                        .linkto(CISales.PositionSumAbstract.DocumentAbstractLink)
                                        .linkto(CISales.DocumentSumAbstract.Contact)
                                        .attribute(CIContacts.Contact.Name);
                final SelectBuilder selDocDate = new SelectBuilder()
                                        .linkto(CISales.PositionSumAbstract.DocumentAbstractLink)
                                        .attribute(CISales.DocumentSumAbstract.Date);
                multi.addSelect(selContactInst, selContactName, selDocDate);
                multi.execute();
                while (multi.next()) {
                    final Map<String, Object> map = new HashMap<String, Object>();
                    final BigDecimal quantity = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.Quantity);
                    final BigDecimal netUnitPrice = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.NetUnitPrice);
                    final BigDecimal netPrice = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.NetPrice);
                    final UoM uoM = Dimension.getUoM(multi.<Long>getAttribute(CISales.PositionSumAbstract.UoM));
                    final Instance contactInst = multi.<Instance>getSelect(selContactInst);
                    final String contactName = multi.<String>getSelect(selContactName);
                    final DateTime docDate = multi.<DateTime>getSelect(selDocDate);
                    map.put("contact", contactName);
                    map.put("date", docDate.toDate());
                    map.put("quantity", quantity.multiply(new BigDecimal(uoM.getNumerator()))
                                        .divide(new BigDecimal(uoM.getDenominator()), 4, BigDecimal.ROUND_HALF_UP));
                    map.put("netUnitPrice", netUnitPrice.multiply(new BigDecimal(uoM.getDenominator()))
                                        .divide(new BigDecimal(uoM.getNumerator()), 4, BigDecimal.ROUND_HALF_UP));
                    map.put("netPrice", netPrice);
                    map.put("contactInst", contactInst);
                    lst.add(map);
                }
            }

            Collections.sort(lst, new Comparator<Map<String, Object>>(){
                @Override
                public int compare(final Map<String, Object> _o1,
                                   final Map<String, Object> _o2)
                {
                    final Date date1 = (Date) _o1.get("date");
                    final Date date2 = (Date) _o2.get("date");
                    final int ret;
                    if (date1.equals(date2)) {
                        final String contact1 = (String) _o1.get("contact");
                        final String contact2 = (String) _o2.get("contact");
                        ret = contact1.compareTo(contact2);
                    } else {
                        ret = date1.compareTo(date2);
                    }
                    return ret;
                }});

            for (final Map<String, Object> map : lst) {
                dataSource.add(map.get("contact"),
                                map.get("date"),
                                map.get("quantity"),
                                map.get("netUnitPrice"),
                                map.get("netPrice"),
                                map.get("contactInst"));
            }

            return dataSource;
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
            final boolean contactFilter = Boolean.parseBoolean((String) props.get("ContactFilter"));
            final String dateFilter = (String) props.get("DateFilter");
            final boolean showDetails = Boolean.parseBoolean((String) props.get("ShowDetails"));

            final TextColumnBuilder<String> contactColumn  = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.SalesProductReport.Contact"),
                            "contact", DynamicReports.type.stringType());
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

            final ColumnGroupBuilder yearGroup  = DynamicReports.grp.group(yearColumn).groupByDataType();
            final ColumnGroupBuilder monthGroup = DynamicReports.grp.group(monthColumn).groupByDataType();
            final ColumnGroupBuilder contactGroup = DynamicReports.grp.group(contactColumn).groupByDataType();

            final CustomSubtotalBuilder<BigDecimal> unitPriceSbt = DynamicReports.sbt
                            .customValue(new UnitPriceSubtotal(), netUnitPriceColumn)
                                      .setDataType(DynamicReports.type.bigDecimalType());

            final AggregationSubtotalBuilder<BigDecimal> quantitySum2 = DynamicReports.sbt.sum(quantityColumn);

            quantitySum = DynamicReports.variable("quantity", BigDecimal.class, Calculation.SUM);
            netPriceSum = DynamicReports.variable("netPrice", BigDecimal.class, Calculation.SUM);

            if (contactFilter) {
                quantitySum.setResetGroup(contactGroup);
                netPriceSum.setResetGroup(contactGroup);
            } else {
                quantitySum.setResetGroup(monthGroup);
                netPriceSum.setResetGroup(monthGroup);
            }

            _builder.addVariable(quantitySum, netPriceSum);

            _builder.addColumn(yearColumn,
                                monthColumn);
            if (contactFilter) {
                _builder.addColumn(contactColumn);
            }
            _builder.addColumn(dateColumn,
                                quantityColumn,
                                netUnitPriceColumn,
                                netPriceColumn);
            if (!showDetails) {
                _builder.setShowColumnValues(false);
            }
            if (contactFilter) {
                _builder.groupBy(yearGroup, monthGroup, contactGroup);
                _builder.addSubtotalAtGroupFooter(contactGroup, quantitySum2);
                //_builder.addSubtotalAtLastGroupFooter(netPriceSum2);
                _builder.addSubtotalAtGroupFooter(contactGroup, unitPriceSbt);
            } else {
                _builder.groupBy(yearGroup, monthGroup);
                _builder.addSubtotalAtGroupFooter(monthGroup, quantitySum2);
                //_builder.addSubtotalAtLastGroupFooter(netPriceSum2);
                _builder.addSubtotalAtGroupFooter(monthGroup, unitPriceSbt);
            }
            /*_builder.pageFooter(DynamicReports.cmp.pageXofY()
                                            .setStyle(DynamicReports.stl.style().bold()
                                            .setAlignment(HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE)
                                            .setTopBorder(DynamicReports.stl.pen1Point())));*/
        }

        private class UnitPriceSubtotal extends AbstractSimpleExpression<BigDecimal> {
            private static final long serialVersionUID = 1L;
            @Override
            public BigDecimal evaluate(final ReportParameters reportParameters) {
                final BigDecimal quantitySumValue = reportParameters.getValue(quantitySum);
                final BigDecimal priceSumValue = reportParameters.getValue(netPriceSum);

                final BigDecimal total = priceSumValue.divide(quantitySumValue, 4, BigDecimal.ROUND_HALF_UP);
                return total;
            }
       }

    }
}
