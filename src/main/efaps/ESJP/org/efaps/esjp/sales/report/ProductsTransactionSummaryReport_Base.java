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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.comparators.ComparatorChain;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport_Base;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.products.Inventory;
import org.efaps.esjp.products.Inventory_Base.InventoryBean;
import org.efaps.esjp.products.StorageGroup;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.grid.ColumnTitleGroupBuilder;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * TODO comment!.
 *
 * @author The eFaps Team
 */
@EFapsUUID("ed77a618-66b7-4245-b46e-56158a1fd5c0")
@EFapsApplication("eFapsApp-Sales")
public abstract class ProductsTransactionSummaryReport_Base
    extends FilteredReport
{

    /**
     * The Enum Valuation.
     *
     * @author The eFaps Team
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
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ProductsTransactionSummaryReport.class);

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
        final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String mime = (String) props.get("Mime");
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
        return new DynProductsTransactionSummaryReport(this);
    }

    /**
     * The Class DynProductsTransactionSummaryReport.
     *
     */
    public static class DynProductsTransactionSummaryReport
        extends AbstractDynamicReport
    {

        /** The filtered report. */
        private final ProductsTransactionSummaryReport_Base filteredReport;

        /**
         * Instantiates a new dyn products transaction summary report.
         *
         * @param _filteredReport the filtered report
         */
        public DynProductsTransactionSummaryReport(final ProductsTransactionSummaryReport_Base _filteredReport)
        {
            this.filteredReport = _filteredReport;
        }

        /**
         * Getter method for the instance variable {@link #filteredReport}.
         *
         * @return value of instance variable {@link #filteredReport}
         */
        protected ProductsTransactionSummaryReport_Base getFilteredReport()
        {
            return this.filteredReport;
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
                    AbstractDynamicReport_Base.LOG.error("error", e);
                    throw new EFapsException("JRException", e);
                }
            } else {
                final Map<Instance, DataBean> beans = new HashMap<>();
                // Admin_DataModel_StatusAbstract
                final QueryBuilder statusQueryBldr = new QueryBuilder(UUID.fromString(
                                "f3eaf2f3-b24c-43c0-9ea1-0f6354438c81"));
                statusQueryBldr.addWhereAttrNotEqValue("Key", "Canceled");

                final QueryBuilder docQueryBldr = new QueryBuilder(CIERP.DocumentAbstract);
                docQueryBldr.addWhereAttrInQuery(CIERP.DocumentAbstract.StatusAbstract, statusQueryBldr
                                .getAttributeQuery("ID"));

                final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.TransactionInOutAbstract);
                attrQueryBldr.addWhereAttrInQuery(CIProducts.TransactionInOutAbstract.Document, docQueryBldr
                                .getAttributeQuery(CIERP.DocumentAbstract.ID));
                add2QueryBldr4Transaction(_parameter, attrQueryBldr);

                final QueryBuilder queryBldr = new QueryBuilder(CIProducts.Costing);
                queryBldr.addWhereAttrInQuery(CIProducts.Costing.TransactionAbstractLink, attrQueryBldr
                                .getAttributeQuery(CIProducts.TransactionInOutAbstract.ID));

                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder selTrans = SelectBuilder.get().linkto(CIProducts.Costing.TransactionAbstractLink);
                final SelectBuilder selTransInst = new SelectBuilder(selTrans).instance();
                final SelectBuilder selQuantity = new SelectBuilder(selTrans).attribute(
                                CIProducts.TransactionAbstract.Quantity);
                final SelectBuilder selProd = new SelectBuilder(selTrans).linkto(
                                CIProducts.TransactionAbstract.Product);
                final SelectBuilder selProdInst = new SelectBuilder(selProd).instance();
                final SelectBuilder selProdName = new SelectBuilder(selProd).attribute(CIProducts.ProductAbstract.Name);
                final SelectBuilder selProdDescr = new SelectBuilder(selProd).attribute(
                                CIProducts.ProductAbstract.Description);
                multi.addSelect(selProdInst, selProdName, selProdDescr, selTransInst, selQuantity);
                multi.addAttribute(CIProducts.Costing.Cost);
                multi.execute();

                while (multi.next()) {
                    final Instance prodInst = multi.getSelect(selProdInst);
                    final DataBean bean;
                    if (beans.containsKey(prodInst)) {
                        bean = beans.get(prodInst);
                    } else {
                        bean = new DataBean().setProdInst(prodInst).setProdName(multi.getSelect(selProdName))
                                        .setProdDescr(multi.getSelect(selProdDescr));
                        beans.put(prodInst, bean);
                    }
                    final Instance transInst = multi.getSelect(selTransInst);
                    final BigDecimal quantity = multi.getSelect(selQuantity);
                    final BigDecimal cost = multi.getAttribute(CIProducts.Costing.Cost);
                    if (transInst.getType().isCIType(CIProducts.TransactionInbound)) {
                        bean.addIncoming(quantity, cost);
                    } else if (transInst.getType().isCIType(CIProducts.TransactionOutbound)) {
                        bean.addOutgoing(quantity, cost);
                    }
                }
                final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);
                final Inventory inventory = new Inventory()
                {

                    @Override
                    protected org.efaps.admin.datamodel.Type getInventoryType(final Parameter _parameter)
                        throws EFapsException
                    {
                        return CIProducts.Inventory.getType();
                    };

                    @Override
                    protected org.efaps.admin.datamodel.Type getTransactionType(final Parameter _parameter)
                        throws EFapsException
                    {
                        return CIProducts.TransactionInOutAbstract.getType();
                    };
                };
                final DateTime dateTo = (DateTime) filter.get("dateTo");
                inventory.setDate(dateTo.plusDays(1).withTimeAtStartOfDay());
                inventory.setCurrencyInst(Currency.getBaseCurrency());
                inventory.setShowStorage(false);
                inventory.setStorageInsts(getStorageInsts(_parameter));
                for (final InventoryBean invBean : inventory.getInventory(_parameter)) {
                    if (beans.containsKey(invBean.getProdInstance())) {
                        beans.get(invBean.getProdInstance())
                            .setEndQty(invBean.getQuantity())
                            .setEndValue(invBean.getTotal());
                    } else {
                        final DataBean bean = new DataBean()
                                        .setProdInst(invBean.getProdInstance())
                                        .setProdName(invBean.getProdName())
                                        .setProdDescr(invBean.getProdDescr())
                                        .setEndQty(invBean.getQuantity())
                                        .setEndValue(invBean.getTotal());
                        beans.put(invBean.getProdInstance(), bean);
                    }
                }

                @SuppressWarnings("unchecked")
                final List<DataBean> dataSource = new ArrayList(beans.values());
                final ComparatorChain<DataBean> chain = new ComparatorChain<>();

                chain.addComparator(new Comparator<DataBean>()
                {

                    @Override
                    public int compare(final DataBean _o1,
                                       final DataBean _o2)
                    {
                        return _o1.getProdName().compareTo(_o2.getProdName());
                    }
                });

                Collections.sort(dataSource, chain);
                ret = new JRBeanCollectionDataSource(dataSource);
                getFilteredReport().cache(_parameter, ret);
            }
            return ret;
        }

        /**
         * Adds the two query bldr for transaction.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _queryBldr the query bldr
         * @throws EFapsException on error
         */
        protected void add2QueryBldr4Transaction(final Parameter _parameter,
                                                 final QueryBuilder _queryBldr)
            throws EFapsException
        {
            final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);
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
            _queryBldr.addWhereAttrGreaterValue(CIProducts.TransactionInOutAbstract.Date, dateFrom
                            .withTimeAtStartOfDay().minusMinutes(1));
            _queryBldr.addWhereAttrLessValue(CIProducts.TransactionInOutAbstract.Date, dateTo.plusDays(1)
                            .withTimeAtStartOfDay());

            final List<Instance> storageInsts = getStorageInsts(_parameter);
            if (CollectionUtils.isNotEmpty(storageInsts)) {
                _queryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Storage, storageInsts.toArray());
            }
        }

        /**
         * Gets the storage insts.
         *
         * @param _parameter the _parameter
         * @return the storage insts
         * @throws EFapsException the e faps exception
         */
        protected List<Instance> getStorageInsts(final Parameter _parameter)
            throws EFapsException
        {
            final List<Instance> ret = new ArrayList<>();
            final Map<String, Object> map = getFilteredReport().getFilterMap(_parameter);
            if (map.containsKey("storageGroup")) {
                final InstanceFilterValue filter = (InstanceFilterValue) map.get("storageGroup");
                if (filter.getObject() != null && filter.getObject().isValid()) {
                    ret.addAll(new StorageGroup().getStorage4Group(_parameter, filter.getObject()));
                }
            }

            if (ret.isEmpty() && map.containsKey("storage")) {
                final InstanceFilterValue filter = (InstanceFilterValue) map.get("storage");
                if (filter.getObject() != null && filter.getObject().isValid()) {
                    ret.add(filter.getObject());
                }
            }
            return ret;
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final TextColumnBuilder<String> prodNameColumn = DynamicReports.col.column(this.filteredReport
                            .getDBProperty("ProdName"), "prodName", DynamicReports.type.stringType());
            final TextColumnBuilder<String> prodDescrColumn = DynamicReports.col.column(this.filteredReport
                            .getDBProperty("ProdDescr"), "prodDescr", DynamicReports.type.stringType())
                            .setFixedWidth(150);

            final TextColumnBuilder<BigDecimal> startQtyColumn = DynamicReports.col.column(this.filteredReport
                            .getDBProperty("StartQty"), "startQty", DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> startValueColumn = DynamicReports.col.column(this.filteredReport
                            .getDBProperty("StartValue"), "startValue", DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> startUnitPriceColumn = DynamicReports.col.column(this.filteredReport
                            .getDBProperty("StartUnitPrice"), "startUnitPrice", DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<BigDecimal> incomingQtyColumn = DynamicReports.col.column(this.filteredReport
                            .getDBProperty("IncomingQty"), "incomingQty", DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> incomingValueColumn = DynamicReports.col.column(this.filteredReport
                            .getDBProperty("IncomingValue"), "incomingValue", DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> incomingUnitPriceColumn = DynamicReports.col.column(this.filteredReport
                            .getDBProperty("IncomingUnitPrice"), "incomingUnitPrice", DynamicReports.type
                                            .bigDecimalType());

            final TextColumnBuilder<BigDecimal> outgoingQtyColumn = DynamicReports.col.column(this.filteredReport
                            .getDBProperty("OutgoingQty"), "outgoingQty", DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> outgoingValueColumn = DynamicReports.col.column(this.filteredReport
                            .getDBProperty("OutgoingValue"), "outgoingValue", DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> outgoingUnitPriceColumn = DynamicReports.col.column(this.filteredReport
                            .getDBProperty("OutgoingUnitPrice"), "outgoingUnitPrice", DynamicReports.type
                                            .bigDecimalType());

            final TextColumnBuilder<BigDecimal> endQtyColumn = DynamicReports.col.column(this.filteredReport
                            .getDBProperty("EndQty"), "endQty", DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> endValueColumn = DynamicReports.col.column(this.filteredReport
                            .getDBProperty("EndValue"), "endValue", DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> endUnitPriceColumn = DynamicReports.col.column(this.filteredReport
                            .getDBProperty("EndUnitPrice"), "endUnitPrice", DynamicReports.type.bigDecimalType());

            final ColumnTitleGroupBuilder startGrp = DynamicReports.grid.titleGroup(this.filteredReport.getDBProperty(
                            "StartTitleGrp"), startQtyColumn,  startUnitPriceColumn, startValueColumn);
            final ColumnTitleGroupBuilder inGrp = DynamicReports.grid.titleGroup(this.filteredReport.getDBProperty(
                            "IncomingTitleGrp"), incomingQtyColumn, incomingUnitPriceColumn, incomingValueColumn);
            final ColumnTitleGroupBuilder outGrp = DynamicReports.grid.titleGroup(this.filteredReport.getDBProperty(
                            "OutgoingTitleGrp"), outgoingQtyColumn, outgoingUnitPriceColumn, outgoingValueColumn);

            final ColumnTitleGroupBuilder endGrp = DynamicReports.grid.titleGroup(this.filteredReport.getDBProperty(
                            "EndTitleGrp"), endQtyColumn, endUnitPriceColumn, endValueColumn);

            _builder.columnGrid(prodNameColumn, prodDescrColumn, startGrp, inGrp, outGrp, endGrp)
                .addColumn(prodNameColumn, prodDescrColumn,
                            startQtyColumn, startUnitPriceColumn, startValueColumn,
                            incomingQtyColumn, incomingUnitPriceColumn, incomingValueColumn,
                            outgoingQtyColumn, outgoingUnitPriceColumn, outgoingValueColumn,
                            endQtyColumn, endUnitPriceColumn, endValueColumn);
        }
    }

    /**
     * The Class DataBean.
     *
     */
    public static class DataBean
    {

        /** The prod inst. */
        private Instance prodInst;

        /** The prod name. */
        private String prodName;

        /** The prod descr. */
        private String prodDescr;

        /** The incoming qty. */
        private BigDecimal incomingQty;

        /** The incoming value. */
        private BigDecimal incomingValue;

        /** The outgoing qty. */
        private BigDecimal outgoingQty;

        /** The outgoing value. */
        private BigDecimal outgoingValue;

        /** The end qty. */
        private BigDecimal endQty;

        /** The end value. */
        private BigDecimal endValue;

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
         * @return the data bean
         */
        public DataBean setProdInst(final Instance _prodInst)
        {
            this.prodInst = _prodInst;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #incoming}.
         *
         * @return value of instance variable {@link #incoming}
         */
        public BigDecimal getIncomingQty()
        {
            return this.incomingQty;
        }

        /**
         * Setter method for instance variable {@link #incoming}.
         *
         * @param _qty the qty
         * @param _cost the cost
         * @return the data bean
         */
        public DataBean addIncoming(final BigDecimal _qty,
                                    final BigDecimal _cost)
        {
            if (this.incomingQty == null) {
                this.incomingQty = BigDecimal.ZERO;
            }
            if (this.incomingValue == null) {
                this.incomingValue = BigDecimal.ZERO;
            }
            this.incomingQty = this.incomingQty.add(_qty);
            this.incomingValue = this.incomingValue.add(_qty.multiply(_cost));
            return this;
        }

        /**
         * Getter method for the instance variable {@link #outgoing}.
         *
         * @return value of instance variable {@link #outgoing}
         */
        public BigDecimal getOutgoing()
        {
            return this.outgoingQty;
        }

        /**
         * Setter method for instance variable {@link #incoming}.
         *
         * @param _qty the qty
         * @param _cost the cost
         * @return the data bean
         */
        public DataBean addOutgoing(final BigDecimal _qty,
                                    final BigDecimal _cost)
        {
            if (this.outgoingQty == null) {
                this.outgoingQty = BigDecimal.ZERO;
            }
            if (this.outgoingValue == null) {
                this.outgoingValue = BigDecimal.ZERO;
            }
            this.outgoingQty = this.outgoingQty.add(_qty);
            this.outgoingValue = this.outgoingValue.add(_qty.multiply(_cost));
            return this;
        }

        /**
         * Getter method for the instance variable {@link #prodName}.
         *
         * @return value of instance variable {@link #prodName}
         */
        public String getProdName()
        {
            return this.prodName;
        }

        /**
         * Setter method for instance variable {@link #prodName}.
         *
         * @param _prodName value for instance variable {@link #prodName}
         * @return the data bean
         */
        public DataBean setProdName(final String _prodName)
        {
            this.prodName = _prodName;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #endValue}.
         *
         * @return value of instance variable {@link #endValue}
         */
        public BigDecimal getQty()
        {
            return this.endQty;
        }

        /**
         * Setter method for instance variable {@link #endValue}.
         *
         * @param _endQty the end qty
         * @return the data bean
         */
        public DataBean setEndQty(final BigDecimal _endQty)
        {
            this.endQty = _endQty;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #incomingValue}.
         *
         * @return value of instance variable {@link #incomingValue}
         */
        public BigDecimal getIncomingValue()
        {
            return this.incomingValue;
        }

        /**
         * Getter method for the instance variable {@link #outgoingQty}.
         *
         * @return value of instance variable {@link #outgoingQty}
         */
        public BigDecimal getOutgoingQty()
        {
            return this.outgoingQty;
        }

        /**
         * Setter method for instance variable {@link #outgoingQty}.
         *
         * @param _outgoingQty value for instance variable {@link #outgoingQty}
         */
        public void setOutgoingQty(final BigDecimal _outgoingQty)
        {
            this.outgoingQty = _outgoingQty;
        }

        /**
         * Getter method for the instance variable {@link #outgoingValue}.
         *
         * @return value of instance variable {@link #outgoingValue}
         */
        public BigDecimal getOutgoingValue()
        {
            return this.outgoingValue;
        }

        /**
         * Setter method for instance variable {@link #outgoingValue}.
         *
         * @param _outgoingValue value for instance variable
         *            {@link #outgoingValue}
         */
        public void setOutgoingValue(final BigDecimal _outgoingValue)
        {
            this.outgoingValue = _outgoingValue;
        }

        /**
         * Getter method for the instance variable {@link #endValue}.
         *
         * @return value of instance variable {@link #endValue}
         */
        public BigDecimal getEndValue()
        {
            return this.endValue;
        }

        /**
         * Setter method for instance variable {@link #endValue}.
         *
         * @param _endValue value for instance variable {@link #endValue}
         * @return the data bean
         */
        public DataBean setEndValue(final BigDecimal _endValue)
        {
            this.endValue = _endValue;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #endQty}.
         *
         * @return value of instance variable {@link #endQty}
         */
        public BigDecimal getEndQty()
        {
            return this.endQty;
        }

        /**
         * Setter method for instance variable {@link #incomingQty}.
         *
         * @param _incomingQty value for instance variable {@link #incomingQty}
         */
        public void setIncomingQty(final BigDecimal _incomingQty)
        {
            this.incomingQty = _incomingQty;
        }

        /**
         * Gets the incoming unit price.
         *
         * @return the incoming unit price
         */
        public BigDecimal getIncomingUnitPrice()
        {
            BigDecimal ret = null;
            if (this.incomingQty != null && this.incomingValue != null) {
                ret = this.incomingValue.divide(this.incomingQty, 4, RoundingMode.HALF_UP);
            }
            return ret;
        }

        /**
         * Gets the outgoing unit price.
         *
         * @return the outgoing unit price
         */
        public BigDecimal getOutgoingUnitPrice()
        {
            BigDecimal ret = null;
            if (this.outgoingQty != null && this.outgoingValue != null) {
                ret = this.outgoingValue.divide(this.outgoingQty, 4, RoundingMode.HALF_UP);
            }
            return ret;
        }

        /**
         * Gets the end unit price.
         *
         * @return the end unit price
         */
        public BigDecimal getEndUnitPrice()
        {
            BigDecimal ret = null;
            if (this.endQty != null && this.endValue != null) {
                ret = this.endValue.divide(this.endQty, 4, RoundingMode.HALF_UP);
            }
            return ret;
        }

        /**
         * Getter method for the instance variable {@link #endValue}.
         *
         * @return value of instance variable {@link #endValue}
         */
        public BigDecimal getStartValue()
        {
            final BigDecimal end = this.endValue == null ? BigDecimal.ZERO : this.endValue;
            final BigDecimal out = this.outgoingValue == null ? BigDecimal.ZERO : this.outgoingValue;
            final BigDecimal in = this.incomingValue == null ? BigDecimal.ZERO : this.incomingValue;
            return end.subtract(in).add(out);
        }

        /**
         * Getter method for the instance variable {@link #endQty}.
         *
         * @return value of instance variable {@link #endQty}
         */
        public BigDecimal getStartQty()
        {
            final BigDecimal end = this.endQty == null ? BigDecimal.ZERO : this.endQty;
            final BigDecimal out = this.outgoingQty == null ? BigDecimal.ZERO : this.outgoingQty;
            final BigDecimal in = this.incomingQty == null ? BigDecimal.ZERO : this.incomingQty;
            return end.subtract(in).add(out);
        }

        /**
         * Gets the start unit price.
         *
         * @return the start unit price
         */
        public BigDecimal getStartUnitPrice()
        {
            BigDecimal ret = null;
            if (getStartQty() != null && getStartQty().compareTo(BigDecimal.ZERO) != 0) {
                ret = getStartValue().divide(getStartQty(), 4, RoundingMode.HALF_UP);
            }
            return ret;
        }

        /**
         * Getter method for the instance variable {@link #prodDescr}.
         *
         * @return value of instance variable {@link #prodDescr}
         */
        public String getProdDescr()
        {
            return this.prodDescr;
        }

        /**
         * Setter method for instance variable {@link #prodDescr}.
         *
         * @param _prodDescr value for instance variable {@link #prodDescr}
         * @return the data bean
         */
        public DataBean setProdDescr(final String _prodDescr)
        {
            this.prodDescr = _prodDescr;
            return this;
        }
    }
}
