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
 * Revision:        $Rev: 8120 $
 * Last Changed:    $Date: 2012-10-26 13:21:34 -0500 (vie, 26 oct 2012) $
 * Last Changed By: $Author: jan@moxter.net $
 */

package org.efaps.esjp.sales.report;

import java.awt.Color;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabMeasureBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabRowGroupBuilder;
import net.sf.dynamicreports.report.builder.style.ConditionalStyleBuilder;
import net.sf.dynamicreports.report.builder.style.StyleBuilder;
import net.sf.dynamicreports.report.constant.Calculation;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.jasperreports.engine.JRDataSource;

import org.efaps.admin.common.SystemConfiguration;
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
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.SalesSettings;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: Account_Base.java 8120 2012-10-26 18:21:34Z jan@moxter.net $
 */
@EFapsUUID("361ad1b2-e734-4a50-b202-c20c19fb03e4")
@EFapsRevision("$Rev: 8120 $")
public abstract class ProductStockReport_Base
    extends AbstractCommon
{
    /**
     * Logging instance used in this class.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(ProductStockReport.class);

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
        dyRp.setFileName(DBProperties.getProperty("org.efaps.esjp.sales.report.ProductStockReport.FileName"));
        final String html = dyRp.getHtmlSnipplet(_parameter);
        ret.put(ReturnValues.SNIPLETT, html);
        return ret;
    }

    /**
     * @param _parameter    Parameter as passed by the eFasp API
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
        dyRp.setFileName(DBProperties.getProperty("org.efaps.esjp.sales.report.ProductStockReport.FileName"));
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
     * @param _parameter    Parameter as passed by the eFasp API
     * @return the report class
     * @throws EFapsException on error
     */
    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new ProdStockReport();
    }

    /**
     * @param _parameter  Parameter as passed by the eFaps API
     * @return AttributeQuery used for the DataSource
     * @throws EFapsException on error
     */
    protected AttributeQuery getAttrQuery(final Parameter _parameter)
        throws EFapsException
    {
        final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter);
        return queryBldr.getAttributeQuery("ID");
    }

    public void addQuery2PositionReport(final Parameter _parameter,
                                        final QueryBuilder _qlb)
        throws EFapsException
    {
        // To Implement
    }

    /**
     * Report class.
     */
    public class ProdStockReport
        extends AbstractDynamicReport
    {

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final String colDefault = DBProperties.getProperty("org.efaps.esjp.sales.report.ProductStockReport.Stock");
            final DRDataSource dataSource = new DRDataSource("product", "document", "quantity");
            final Map<String, Map<String, BigDecimal>> rowMap = new LinkedHashMap<String, Map<String, BigDecimal>>();

            final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionAbstract);
            queryBldr.addWhereAttrInQuery(CISales.PositionAbstract.DocumentAbstractLink, getAttrQuery(_parameter));
            addQuery2PositionReport(_parameter, queryBldr);
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CISales.PositionAbstract.Quantity);
            final SelectBuilder selProdName = new SelectBuilder().linkto(CISales.PositionAbstract.Product)
                            .attribute(CIProducts.ProductAbstract.Name);
            final SelectBuilder selProdDesc = new SelectBuilder().linkto(CISales.PositionAbstract.Product)
                            .attribute(CIProducts.ProductAbstract.Description);
            final SelectBuilder selProdInstance = new SelectBuilder().linkto(CISales.PositionAbstract.Product)
                            .instance();
            final SelectBuilder selDocName = new SelectBuilder()
                            .linkto(CISales.PositionAbstract.DocumentAbstractLink)
                            .attribute(CISales.DocumentAbstract.Name);
            final SelectBuilder selDocInst = new SelectBuilder()
                            .linkto(CISales.PositionAbstract.DocumentAbstractLink).instance();
            multi.addSelect(selDocName, selDocInst, selProdName, selProdInstance, selProdDesc);
            multi.execute();
            while (multi.next()) {
                final Instance prodInst = multi.<Instance>getSelect(selProdInstance);
                final Instance docInst = multi.<Instance>getSelect(selDocInst);
                final BigDecimal quantity = multi.<BigDecimal>getAttribute(CISales.PositionAbstract.Quantity);
                final BigDecimal quantity4Derived = getQuantity4Derived(_parameter, docInst, prodInst);
                String prodName = multi.<String>getSelect(selProdName);
                final String prodDesc = multi.<String>getSelect(selProdDesc);
                prodName = prodName + " " + prodDesc;

                final String docName = multi.<String>getSelect(selDocName);
                if (rowMap.containsKey(docName)) {
                    if (rowMap.containsKey(colDefault)) {
                        final BigDecimal stock = getStock4Product(_parameter, prodInst);
                        final Map<String, BigDecimal> colMap = rowMap.get(colDefault);
                        if (!colMap.containsKey(prodName)) {
                            colMap.put(prodName, stock);
                            rowMap.put(colDefault, colMap);
                        }
                    }
                    final Map<String, BigDecimal> colMap = rowMap.get(docName);
                    if (colMap.containsKey(prodName)) {
                        colMap.put(prodName, colMap.get(prodName).add(quantity.negate()).add(quantity4Derived));
                    } else {
                        colMap.put(prodName, quantity.negate().add(quantity4Derived));
                    }
                } else {
                    final BigDecimal stock = getStock4Product(_parameter, prodInst);
                    if (!rowMap.containsKey(colDefault)) {
                        final Map<String, BigDecimal> colMap = new TreeMap<String, BigDecimal>();
                        colMap.put(prodName, stock);
                        rowMap.put(colDefault, colMap);
                    } else {
                        final Map<String, BigDecimal> colMap = rowMap.get(colDefault);
                        if (!colMap.containsKey(prodName)) {
                            colMap.put(prodName, stock);
                            rowMap.put(colDefault, colMap);
                        }
                    }
                    final Map<String, BigDecimal> colMap = new TreeMap<String, BigDecimal>();
                    colMap.put(prodName, quantity.negate().add(quantity4Derived));
                    rowMap.put(docName, colMap);
                }
            }

            final int rowQ = String.valueOf(rowMap.size()).length();
            int cont = 1;
            for (final Entry<String, Map<String, BigDecimal>> entryRow : rowMap.entrySet()) {
                final Formatter formatter = new Formatter();
                final String docName = formatter.format("%0" + rowQ + "d", cont).toString() + ". " + entryRow.getKey();
                for (final Entry<String, BigDecimal> entryCol : entryRow.getValue().entrySet()) {
                    final String prodName = entryCol.getKey();
                    final BigDecimal quantity = entryCol.getValue();
                    dataSource.add(prodName, docName, quantity);
                }
                formatter.close();
                cont++;
            }

            return dataSource;
        }

        protected BigDecimal getQuantity4Derived(final Parameter _parameter,
                                               final Instance _docInst,
                                               final Instance _prodInst)
            throws EFapsException
        {
            BigDecimal ret = BigDecimal.ZERO;
            final Map<Integer, String> types = analyseProperty(_parameter, "DerivedType");
            final Map<Integer, String> statusGrps = analyseProperty(_parameter, "DerivedStatusGrp");
            final Map<Integer, String> status = analyseProperty(_parameter, "DerivedStatus");

            final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Document2DerivativeDocument);
            attrQueryBldr.addWhereAttrEqValue(CISales.Document2DerivativeDocument.From, _docInst);
            final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.Document2DerivativeDocument.To);

            for (final Entry<Integer, String> entry : types.entrySet()) {
                final Integer key = entry.getKey();
                final Type type = Type.get(entry.getValue());
                final QueryBuilder attrQueryBldr2 = new QueryBuilder(type);
                attrQueryBldr2.addWhereAttrInQuery(CISales.DocumentAbstract.ID, attrQuery);
                if (statusGrps.containsKey(key)) {
                    final String[] statusArr = status.get(key).split(";");
                    final List<Status> statusLst = new ArrayList<Status>();
                    for (final String stat : statusArr) {
                        final Status st = Status.find(statusGrps.get(key), stat);
                        statusLst.add(st);
                    }
                    attrQueryBldr2.addWhereAttrEqValue(CISales.DocumentAbstract.StatusAbstract, statusLst.toArray());
                }
                final AttributeQuery attrQuery2 = attrQueryBldr2.getAttributeQuery(CISales.DocumentAbstract.ID);

                final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionAbstract);
                queryBldr.addWhereAttrInQuery(CISales.PositionAbstract.DocumentAbstractLink, attrQuery2);
                queryBldr.addWhereAttrEqValue(CISales.PositionAbstract.Product, _prodInst);
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CISales.PositionAbstract.Quantity);
                multi.executeWithoutAccessCheck();
                while (multi.next()) {
                    final BigDecimal quantity = multi.<BigDecimal>getAttribute(CISales.PositionAbstract.Quantity);
                    ret = ret.add(quantity);
                }
            }
            return ret;
        }

        /**
         * @param _parameter Parameter as passed by the eFaps API
         * @param _prodInstance instance of the product
         * @return value
         * @throws EFapsException on error
         */
        protected BigDecimal getStock4Product(final Parameter _parameter,
                                              final Instance _prodInstance)
            throws EFapsException
        {
            BigDecimal quantity = BigDecimal.ZERO;
            final SystemConfiguration config = Sales.getSysConfig();
            final Instance storGrpInstance = config.getLink(SalesSettings.STORAGEGROUP4PRODUCTREQUESTREPORT);

            if (storGrpInstance != null && storGrpInstance.isValid()) {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.StorageGroupAbstract2StorageAbstract);
                attrQueryBldr.addWhereAttrEqValue(CIProducts.StorageGroupAbstract2StorageAbstract.FromAbstractLink,
                                storGrpInstance);
                final AttributeQuery attrQuery = attrQueryBldr
                                .getAttributeQuery(CIProducts.StorageGroupAbstract2StorageAbstract.ToAbstractLink);

                final QueryBuilder queryBldr = new QueryBuilder(CIProducts.Inventory);
                queryBldr.addWhereAttrEqValue(CIProducts.Inventory.Product, _prodInstance);
                queryBldr.addWhereAttrInQuery(CIProducts.Inventory.Storage, attrQuery);
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CIProducts.Inventory.Quantity);
                multi.execute();
                while (multi.next()) {
                    final BigDecimal quantityTmp = multi.<BigDecimal>getAttribute(CIProducts.Inventory.Quantity);
                    quantity = quantity.add(quantityTmp);
                }
            } else {
                ProductStockReport_Base.LOG.warn("It's required a system configuration for Storage Group");
            }
            return quantity;
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
            // to be implemented by subclasses
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
            final boolean productRow = "true".equalsIgnoreCase((String) props.get("ProductRow"));

            final CrosstabBuilder crosstab = DynamicReports.ctab.crosstab();
            final CrosstabMeasureBuilder<BigDecimal> quantityMeasure = DynamicReports.ctab.measure("quantity",
                            BigDecimal.class, Calculation.SUM);
            if (productRow) {
                final CrosstabRowGroupBuilder<String> rowGroup = DynamicReports.ctab.rowGroup("product", String.class)
                                .setShowTotal(false);
                if (ExportType.HTML.equals(getExType())) {
                    rowGroup.setHeaderWidth(250);
                }

                final CrosstabColumnGroupBuilder<String> columnGroup = DynamicReports.ctab
                                .columnGroup("document", String.class);

                crosstab.headerCell(DynamicReports.cmp.text(DBProperties
                        .getProperty("org.efaps.esjp.sales.report.ProductStockReport.HeaderCell1"))
                        .setStyle(DynamicReports.stl.style().setBold(true))).rowGroups(rowGroup)
                        .columnGroups(columnGroup).measures(quantityMeasure);
            } else {
                final CrosstabRowGroupBuilder<String> rowGroup = DynamicReports.ctab.rowGroup("document", String.class);
                final CrosstabColumnGroupBuilder<String> columnGroup = DynamicReports.ctab
                                .columnGroup("product", String.class).setShowTotal(false);

                crosstab.headerCell(DynamicReports.cmp.text(DBProperties
                        .getProperty("org.efaps.esjp.sales.report.ProductStockReport.HeaderCell2"))
                        .setStyle(DynamicReports.stl.style().setBold(true))).rowGroups(rowGroup)
                        .columnGroups(columnGroup).measures(quantityMeasure);
            }

            final ConditionalStyleBuilder condition = DynamicReports.stl.conditionalStyle(
                            DynamicReports.cnd.greater(quantityMeasure, 0)).setBold(true);

            final ConditionalStyleBuilder condition2 = DynamicReports.stl.conditionalStyle(
                            DynamicReports.cnd.smaller(quantityMeasure, 0))
                            .setForegroundColor(Color.RED).setBold(true);

            final StyleBuilder margin1Style = DynamicReports.stl.style().conditionalStyles(condition, condition2)
                            .setBorder(DynamicReports.stl.pen1Point().setLineColor(Color.BLACK));

            quantityMeasure.setStyle(margin1Style);

            _builder.summary(crosstab);
        }
    }
}
