/*
 * Copyright 2003 - 2014 The eFaps Team
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

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
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.lang3.StringEscapeUtils;
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
import org.efaps.admin.ui.field.Field;
import org.efaps.db.AttributeQuery;
import org.efaps.db.CachedPrintQuery;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.erp.FilteredReport;
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
    extends FilteredReport
{

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ProductStockReport.class);

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return value for the form
     * @throws EFapsException on error
     */
    public Return getJavaScriptUIValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Map<String, Object> map = getFilterMap(_parameter);
        if (!map.containsKey("project")) {
            map.put("project", "");
        }

        if (map.get("project") instanceof ProjectFilterValue) {
            final ProjectFilterValue value = (ProjectFilterValue) map.get("project");
            final StringBuilder js = new StringBuilder();
            js.append("<script type=\"text/javascript\">\n")
                            .append("require([\"dojo/ready\", \"dojo/query\",\"dojo/dom-construct\"],")
                            .append(" function(ready, query, domConstruct){\n")
                            .append(" ready(1500, function(){")
                            .append("eFapsSetFieldValue(").append(0).append(",'").append("project").append("',")
                            .append("'").append(value.getObject().getOid()).append("'")
                            .append(",'").append(StringEscapeUtils.escapeEcmaScript(value.getLabel())).append("'")
                            .append(");")
                            .append("});").append("});\n</script>\n");
            ret.put(ReturnValues.SNIPLETT, js.toString());
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _field field teh valu eis wanted for
     * @return object
     */
    @Override
    protected Object getFilterValue(final Parameter _parameter,
                                    final Field _field,
                                    final Map<String, Object> _oldFilter)
    {
        final Object obj;
        final String val = _parameter.getParameterValue(_field.getName());
        if ("project".equals(_field.getName())) {
            obj = new ProjectFilterValue().setObject(Instance.get(val));
        } else {
            obj = super.getFilterValue(_parameter, _field, _oldFilter);
        }
        return obj;
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
        dyRp.setFileName(DBProperties.getProperty("org.efaps.esjp.sales.report.ProductStockReport.FileName"));
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
     * @param _parameter Parameter as passed by the eFasp API
     * @return the report class
     * @throws EFapsException on error
     */
    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new ProdStockReport(this);
    }

    /**
     * FilterClass.
     */
    public static class ProjectFilterValue
        extends FilterValue<Instance>
    {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        @Override
        public String getLabel()
            throws EFapsException
        {
            String ret;
            if (getObject().isValid()) {
                final PrintQuery print = new PrintQuery(getObject());
                print.addAttribute("Name", "Description");
                print.execute();
                ret = print.<String>getAttribute("Name") + " - " + print.<String>getAttribute("Description");
            } else {
                ret = "";
            }
            return ret;
        }
    }

    /**
     * Report class.
     */
    public static class ProdStockReport
        extends AbstractDynamicReport
    {

        /**
         * variable to report.
         */
        private final ProductStockReport_Base filteredReport;

        /**
         * @param _report4Account class used
         */
        public ProdStockReport(final ProductStockReport_Base _report4Account)
        {
            this.filteredReport = _report4Account;
        }

        /**
         * @param _parameter Parameter as passed by the eFaps API
         * @return AttributeQuery used for the DataSource
         * @throws EFapsException on error
         */
        protected AttributeQuery getDocAttrQuery(final Parameter _parameter)
            throws EFapsException
        {
            final QueryBuilder queryBldr;
            if (containsProperty(_parameter, "Type")) {
                queryBldr = getQueryBldrFromProperties(_parameter);
            } else {
                final PrintQuery print = new PrintQuery(_parameter.getInstance());
                print.addAttribute(CIERP.DocumentAbstract.StatusAbstract);
                print.execute();

                final QueryBuilder prodQueryBldr = new QueryBuilder(CISales.PositionAbstract);
                prodQueryBldr.addWhereAttrEqValue(CISales.PositionAbstract.DocumentAbstractLink,
                                _parameter.getInstance());

                final QueryBuilder posQueryBldr = new QueryBuilder(CISales.PositionAbstract);
                posQueryBldr.addWhereAttrInQuery(CISales.PositionAbstract.Product,
                                prodQueryBldr.getAttributeQuery(CISales.PositionAbstract.Product));

                queryBldr = new QueryBuilder(_parameter.getInstance().getType());
                queryBldr.addWhereAttrEqValue(CIERP.DocumentAbstract.StatusAbstract,
                                print.getAttribute(CIERP.DocumentAbstract.StatusAbstract));
                queryBldr.addWhereAttrInQuery(CIERP.DocumentAbstract.ID,
                                posQueryBldr.getAttributeQuery(CISales.PositionAbstract.DocumentAbstractLink));
            }
            return queryBldr.getAttributeQuery("ID");
        }

        /**
         * @param _parameter Parameter as passed by the eFaps API
         * @param _queryBldr QueryBuilder to add to
         * @throws EFapsException on error
         */
        protected void add2QueryBldr(final Parameter _parameter,
                                     final QueryBuilder _queryBldr)
            throws EFapsException
        {
            final Map<String, Object> filter = this.filteredReport.getFilterMap(_parameter);
            if (filter.containsKey("project") && filter.get("project") instanceof ProjectFilterValue) {
                final Instance projectInst = ((ProjectFilterValue) filter.get("project")).getObject();
                if (projectInst.isValid()) {
                    // Projects_Project2DocumentAbstract
                    final QueryBuilder attrQueryBldr = new QueryBuilder(
                                    UUID.fromString("a6accf51-06d0-4882-a4c7-617cd5bf789b"));
                    attrQueryBldr.addWhereAttrEqValue("FromAbstract", projectInst);
                    _queryBldr.addWhereAttrInQuery(CISales.PositionAbstract.DocumentAbstractLink,
                                    attrQueryBldr.getAttributeQuery("ToAbstract"));
                }
            }
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final Map<String, DataBean> beans = new HashMap<>();
            final Set<Instance> prodInsts = new HashSet<>();
            final Set<Instance> docInsts = new HashSet<>();

            final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionAbstract);
            queryBldr.addWhereAttrInQuery(CISales.PositionAbstract.DocumentAbstractLink, getDocAttrQuery(_parameter));
            add2QueryBldr(_parameter, queryBldr);
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CISales.PositionAbstract.Quantity);
            final SelectBuilder selProdInstance = new SelectBuilder().linkto(CISales.PositionAbstract.Product)
                            .instance();
            final SelectBuilder selDocInst = new SelectBuilder()
                            .linkto(CISales.PositionAbstract.DocumentAbstractLink).instance();
            multi.addSelect(selDocInst, selProdInstance);
            multi.execute();
            while (multi.next()) {
                final Instance prodInst = multi.<Instance>getSelect(selProdInstance);
                prodInsts.add(prodInst);
                final Instance docInst = multi.<Instance>getSelect(selDocInst);
                docInsts.add(docInst);
                final BigDecimal quantity = multi.<BigDecimal>getAttribute(CISales.PositionAbstract.Quantity);
                final BigDecimal quantity4Derived = getQuantity4Derived(_parameter, docInst, prodInst);
                final DataBean dataBeanTmp = getDataBean().setDocInst(docInst).setProdInst(prodInst);
                final DataBean dataBean;
                if (beans.containsKey(dataBeanTmp.getKey())) {
                    dataBean = beans.get(dataBeanTmp.getKey());
                } else {
                    dataBean = dataBeanTmp;
                    beans.put(dataBeanTmp.getKey(), dataBean);
                }
                dataBean.addQuantity(quantity.negate().add(quantity4Derived));
            }
            final List<DataBean> dataSource = new ArrayList<>(beans.values());
            final int rowQ = String.valueOf(docInsts.size()).length();

            Collections.sort(dataSource, new Comparator<DataBean>()
            {

                @Override
                public int compare(final DataBean _o1,
                                   final DataBean _o2)
                {
                    return _o1.getDocName().compareTo(_o2.getDocName());
                }
            });
            Instance current = null;
            int cont = 2;
            for (final DataBean bean : dataSource) {
                if (!bean.getDocInst().equals(current)) {
                    cont++;
                    current = bean.getDocInst();
                }
                bean.setDocName(String.format("%0" + rowQ + "d", cont) + ". " + bean.getDocName());
            }

            for (final Instance prodInst : prodInsts) {
                final BigDecimal[] quantities = getStock4Product(_parameter, prodInst);
                final String docName = String.format("%0" + rowQ + "d", 1) + ". " + DBProperties
                                .getProperty("org.efaps.esjp.sales.report.ProductStockReport.Stock");
                dataSource.add(getDataBean()
                                .setProdInst(prodInst)
                                .setDocName(docName)
                                .setQuantity(quantities[0]));

                final String docName2 = String.format("%0" + rowQ + "d", 2) + ". " + DBProperties
                                .getProperty("org.efaps.esjp.sales.report.ProductStockReport.Reserved");
                dataSource.add(getDataBean()
                                .setProdInst(prodInst)
                                .setDocName(docName2)
                                .setQuantity(quantities[1]));
            }

            final ComparatorChain<DataBean> chain = new ComparatorChain<DataBean>();
            chain.addComparator(new Comparator<ProductStockReport_Base.DataBean>()
            {

                @Override
                public int compare(final DataBean _o1,
                                   final DataBean _o2)
                {
                    return _o1.getProdName().compareTo(_o2.getProdName());
                }
            });
            chain.addComparator(new Comparator<ProductStockReport_Base.DataBean>()
            {

                @Override
                public int compare(final DataBean _o1,
                                   final DataBean _o2)
                {
                    return _o1.getDocName().compareTo(_o2.getDocName());
                }
            });
            Collections.sort(dataSource, chain);

            return new JRBeanCollectionDataSource(dataSource);
        }

        /**
         * @return new DataBean
         */
        protected DataBean getDataBean()
        {
            return new DataBean();
        }

        /**
         * @param _parameter Parameter as passed by the eFaps API
         * @param _docInst instance of the document
         * @param _prodInst instance of the product
         * @return value
         * @throws EFapsException on error
         */
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
         * @return array with quantity, reserved
         * @throws EFapsException on error
         */
        protected BigDecimal[] getStock4Product(final Parameter _parameter,
                                                final Instance _prodInstance)
            throws EFapsException
        {
            final BigDecimal[] quantities = new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO };
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
                multi.addAttribute(CIProducts.Inventory.Quantity, CIProducts.Inventory.Reserved);
                multi.execute();
                while (multi.next()) {
                    final BigDecimal quantityTmp = multi.<BigDecimal>getAttribute(CIProducts.Inventory.Quantity);
                    final BigDecimal reservedTmp = multi.<BigDecimal>getAttribute(CIProducts.Inventory.Reserved);
                    quantities[0] = quantities[0].add(quantityTmp);
                    quantities[1] = quantities[1].add(reservedTmp);
                }
            } else {
                ProductStockReport_Base.LOG.warn("It's required a system configuration for Storage Group");
            }
            return quantities;
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
                final CrosstabRowGroupBuilder<String> rowGroup = DynamicReports.ctab.rowGroup("prodName", String.class)
                                .setShowTotal(false);
                if (ExportType.HTML.equals(getExType())) {
                    rowGroup.setHeaderWidth(250);
                }

                final CrosstabColumnGroupBuilder<String> columnGroup = DynamicReports.ctab
                                .columnGroup("docName", String.class);

                crosstab.headerCell(DynamicReports.cmp.text(DBProperties
                                .getProperty("org.efaps.esjp.sales.report.ProductStockReport.HeaderCell1"))
                                .setStyle(DynamicReports.stl.style().setBold(true))).rowGroups(rowGroup)
                                .columnGroups(columnGroup).measures(quantityMeasure);
            } else {
                final CrosstabRowGroupBuilder<String> rowGroup = DynamicReports.ctab.rowGroup("docName", String.class);
                final CrosstabColumnGroupBuilder<String> columnGroup = DynamicReports.ctab
                                .columnGroup("prodName", String.class).setShowTotal(false);

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

    /**
     * Bean containing the data.
     */
    public static class DataBean
    {
        /**
         * Instance of the product.
         */
        private Instance prodInst;

        /**
         * Instance of the document.
         */
        private Instance docInst;

        /**
         * Name of the product.
         */
        private String prodName;

        /**
         * Name of the document.
         */
        private String docName;

        /**
         * Document was initialized.
         */
        private boolean initDoc;

        /**
         * Product was initialized.
         */
        private boolean initProd;

        /**
         * Quantity.
         */
        private BigDecimal quantity = BigDecimal.ZERO;

        /**
         * Getter method for the instance variable {@link #prodName}.
         *
         * @return value of instance variable {@link #prodName}
         */
        public String getProdName()
        {
            if (!isInitProd()) {
                try {
                    final PrintQuery print = CachedPrintQuery.get4Request(getProdInst());
                    print.addAttribute(CIProducts.ProductAbstract.Name, CIProducts.ProductAbstract.Description);
                    print.execute();
                    setProdName(print.getAttribute(CIProducts.ProductAbstract.Name) + " - "
                                    + print.getAttribute(CIProducts.ProductAbstract.Description));
                } catch (final EFapsException e) {
                    LOG.error("Error on retrieving name", e);
                }
            }
            return this.prodName;
        }

        /**
         * @return key used for temporal caching
         */
        public String getKey()
        {
            return getProdInst().getOid() + "-" + getDocInst().getOid();
        }

        /**
         * Setter method for instance variable {@link #prodName}.
         *
         * @param _prodName value for instance variable {@link #prodName}
         */
        public void setProdName(final String _prodName)
        {
            this.prodName = _prodName;
            setInitProd(true);
        }

        /**
         * Getter method for the instance variable {@link #docName}.
         *
         * @return value of instance variable {@link #docName}
         */
        public String getDocName()
        {
            if (!isInitDoc()) {
                try {
                    final PrintQuery print = CachedPrintQuery.get4Request(getDocInst());
                    print.addAttribute(CIERP.DocumentAbstract.Name);
                    print.execute();
                    setDocName(print.<String>getAttribute(CIERP.DocumentAbstract.Name));
                } catch (final EFapsException e) {
                    LOG.error("Error on retrieving name", e);
                }
            }
            return this.docName == null ? "" : this.docName;
        }

        /**
         * Setter method for instance variable {@link #docName}.
         *
         * @param _docName value for instance variable {@link #docName}
         * @return this for chaining
         */
        public DataBean setDocName(final String _docName)
        {
            this.docName = _docName;
            setInitDoc(true);
            return this;
        }

        /**
         * Getter method for the instance variable {@link #quantity}.
         *
         * @return value of instance variable {@link #quantity}
         */
        public BigDecimal getQuantity()
        {
            return this.quantity;
        }

        /**
         * Setter method for instance variable {@link #quantity}.
         *
         * @param _quantity value for instance variable {@link #quantity}
         * @return this for chaining
         */
        public DataBean setQuantity(final BigDecimal _quantity)
        {
            this.quantity = _quantity;
            return this;
        }

        /**
         * Setter method for instance variable {@link #quantity}.
         *
         * @param _quantity value for instance variable {@link #quantity}
         */
        public void addQuantity(final BigDecimal _quantity)
        {
            setQuantity(getQuantity().add(_quantity));
        }

        /**
         * Getter method for the instance variable {@link #docInst}.
         *
         * @return value of instance variable {@link #docInst}
         */
        public Instance getDocInst()
        {
            return this.docInst;
        }

        /**
         * Setter method for instance variable {@link #docInst}.
         *
         * @param _docInst value for instance variable {@link #docInst}
         * @return this for chaining
         */
        public DataBean setDocInst(final Instance _docInst)
        {
            this.docInst = _docInst;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #prodInst}.
         *
         * @return value of instance variable {@link #prodInst}
         */
        public Instance getProdInst()
        {
            return this.prodInst;
        }

        /**
         * Setter method for instance variable {@link #prodInst}.
         *
         * @param _prodInst value for instance variable {@link #prodInst}
         * @return this for chaining
         */
        public DataBean setProdInst(final Instance _prodInst)
        {
            this.prodInst = _prodInst;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #initDoc}.
         *
         * @return value of instance variable {@link #initDoc}
         */
        public boolean isInitDoc()
        {
            return this.initDoc;
        }

        /**
         * Setter method for instance variable {@link #initDoc}.
         *
         * @param _initDoc value for instance variable {@link #initDoc}
         */
        public void setInitDoc(final boolean _initDoc)
        {
            this.initDoc = _initDoc;
        }

        /**
         * Getter method for the instance variable {@link #initProd}.
         *
         * @return value of instance variable {@link #initProd}
         */
        public boolean isInitProd()
        {
            return this.initProd;
        }

        /**
         * Setter method for instance variable {@link #initProd}.
         *
         * @param _initProd value for instance variable {@link #initProd}
         */
        public void setInitProd(final boolean _initProd)
        {
            this.initProd = _initProd;
        }
    }
}
