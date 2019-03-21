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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.ColumnBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.grid.ColumnGridComponentBuilder;
import net.sf.dynamicreports.report.builder.grid.ColumnTitleGroupBuilder;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.efaps.admin.datamodel.ui.IUIValue;
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
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.products.Inventory;
import org.efaps.esjp.products.Inventory_Base.InventoryBean;
import org.efaps.esjp.products.StorageGroup;
import org.efaps.esjp.sales.Costs;
import org.efaps.esjp.sales.report.filter.CostTypeFilterValue;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ProductsTransactionSummaryReport.class);

    public enum ProdDocDisplay {
        NONE,
        INCOMING,
        OUTGOOING,
        BOTH
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
        final Instance[] currencies;
        if (Sales.REPORT_PRODTRANSSUM_ACQUISITION.get()) {
            currencies = Currency.getAvailable().toArray(new Instance[Currency.getAvailable().size()]);
        } else {
            currencies = new Instance[0];
        }
        ret.put(ReturnValues.VALUES, CostTypeFilterValue.getCostTypePositions(_parameter, (CostTypeFilterValue) map
                        .get(key), currencies));
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

        /** The data source. */
        private DataSource dataSource;

        /**
         * Instantiates a new dyn products transaction summary report.
         *
         * @param _filteredReport the filtered report
         */
        public DynProductsTransactionSummaryReport(final ProductsTransactionSummaryReport_Base _filteredReport)
        {
            filteredReport = _filteredReport;
        }

        /**
         * Getter method for the instance variable {@link #filteredReport}.
         *
         * @return value of instance variable {@link #filteredReport}
         */
        protected ProductsTransactionSummaryReport_Base getFilteredReport()
        {
            return filteredReport;
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            return dataSource;
        }

        /**
         * Eval data source.
         *
         * @param _parameter the parameter
         * @throws EFapsException the e faps exception
         */
        protected void evalDataSource(final Parameter _parameter)
            throws EFapsException
        {
            if (getFilteredReport().isCached(_parameter)) {
                dataSource = (DataSource) getFilteredReport().getDataSourceFromCache(_parameter);
                ProductsTransactionSummaryReport_Base.LOG.debug("Got datasource for report from cache: {}",
                                dataSource);
                dataSource.moveFirst();
            } else {
                final ProdDocDisplay prodDocDisplay = evaluateProdDocDisplay(_parameter);
                final Map<Instance, DataBean> beans = new HashMap<>();
                final Map<String, Object> filterMap = getFilteredReport().getFilterMap(_parameter);
                final CostTypeFilterValue filterValue = (CostTypeFilterValue) filterMap.get("costType");
                Instance alterInst = null;
                if (filterValue != null) {
                    alterInst = Instance.get(filterValue.getObject());
                }

                // Admin_DataModel_StatusAbstract
                final QueryBuilder statusQueryBldr = new QueryBuilder(UUID.fromString(
                                "f3eaf2f3-b24c-43c0-9ea1-0f6354438c81"));
                statusQueryBldr.addWhereAttrNotEqValue("Key", "Canceled");

                final QueryBuilder docQueryBldr = new QueryBuilder(CIERP.DocumentAbstract);
                docQueryBldr.addWhereAttrInQuery(CIERP.DocumentAbstract.StatusAbstract, statusQueryBldr
                                .getAttributeQuery("ID"));
                add2QueryBldr4Doc(_parameter, docQueryBldr);

                final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.TransactionInOutAbstract);
                attrQueryBldr.addWhereAttrInQuery(CIProducts.TransactionInOutAbstract.Document, docQueryBldr
                                .getAttributeQuery(CIERP.DocumentAbstract.ID));
                add2QueryBldr4Transaction(_parameter, attrQueryBldr);

                final QueryBuilder queryBldr;
                if (filterValue != null && filterValue.isAlternative() && InstanceUtils.isValid(alterInst)) {
                    queryBldr = new QueryBuilder(CIProducts.CostingAlternative);
                    queryBldr.addWhereAttrEqValue(CIProducts.CostingAlternative.CurrencyLink, alterInst);
                } else {
                    queryBldr = new QueryBuilder(CIProducts.Costing);
                }
                queryBldr.addWhereAttrInQuery(CIProducts.Costing.TransactionAbstractLink, attrQueryBldr
                                .getAttributeQuery(CIProducts.TransactionInOutAbstract.ID));

                final MultiPrintQuery multi = queryBldr.getPrint();

                final SelectBuilder selTrans = SelectBuilder.get()
                                .linkto(CIProducts.CostingAbstract.TransactionAbstractLink);
                final SelectBuilder prodDocTypeSel = new SelectBuilder(selTrans)
                                .linkto(CIProducts.TransactionInOutAbstract.Document)
                                .linkfrom(CIERP.Document2DocumentTypeAbstract.DocumentLinkAbstract)
                                .linkto(CIERP.Document2DocumentTypeAbstract.DocumentTypeLinkAbstract)
                                .attribute(CIERP.DocumentTypeAbstract.Description);
                final SelectBuilder prodDocTypeOidSel = new SelectBuilder(selTrans)
                                .linkto(CIProducts.TransactionInOutAbstract.Document)
                                .linkfrom(CIERP.Document2DocumentTypeAbstract.DocumentLinkAbstract)
                                .linkto(CIERP.Document2DocumentTypeAbstract.DocumentTypeLinkAbstract)
                                .oid();

                final SelectBuilder selTransInst = new SelectBuilder(selTrans).instance();
                final SelectBuilder selTransDate = new SelectBuilder(selTrans).attribute(
                                CIProducts.TransactionAbstract.Date);
                final SelectBuilder selQuantity = new SelectBuilder(selTrans).attribute(
                                CIProducts.TransactionAbstract.Quantity);
                final SelectBuilder selProd = new SelectBuilder(selTrans).linkto(
                                CIProducts.TransactionAbstract.Product);
                final SelectBuilder selProdInst = new SelectBuilder(selProd).instance();
                final SelectBuilder selProdName = new SelectBuilder(selProd).attribute(CIProducts.ProductAbstract.Name);
                final SelectBuilder selProdDescr = new SelectBuilder(selProd).attribute(
                                CIProducts.ProductAbstract.Description);
                if (!ProdDocDisplay.NONE.equals(prodDocDisplay)) {
                    multi.addSelect(prodDocTypeSel, prodDocTypeOidSel);
                }
                multi.addSelect(selProdInst, selProdName, selProdDescr, selTransInst, selTransDate, selQuantity);
                multi.addAttribute(CIProducts.CostingAbstract.Cost);
                multi.execute();
                final Map<String, String> incomingProdDocTypes = new HashMap<>();
                final Map<String, String> outgoingProdDocTypes = new HashMap<>();
                while (multi.next()) {
                    final Instance prodInst = multi.getSelect(selProdInst);
                    final DataBean bean;
                    if (beans.containsKey(prodInst)) {
                        bean = beans.get(prodInst);
                    } else {
                        bean = new DataBean().setProdInst(prodInst)
                              .setProdName(multi.getSelect(selProdName))
                              .setProdDescr(multi.getSelect(selProdDescr));
                        beans.put(prodInst, bean);
                    }
                    if (!ProdDocDisplay.NONE.equals(prodDocDisplay)) {
                        bean.setProdDocType(multi.getSelect(prodDocTypeSel))
                            .setProdDocTypeOid(multi.getSelect(prodDocTypeOidSel));
                    }
                    final Instance transInst = multi.getSelect(selTransInst);
                    final BigDecimal quantity = multi.getSelect(selQuantity);
                    final BigDecimal cost;
                    if (filterValue != null && filterValue.isAcquisition()) {
                        cost = Costs.getAcquisitionCost4Date(_parameter, prodInst,
                            alterInst, multi.getSelect(selTransDate));
                    } else {
                        cost = multi.getAttribute(CIProducts.CostingAbstract.Cost);
                    }
                    if (transInst.getType().isCIType(CIProducts.TransactionInbound)) {
                        incomingProdDocTypes.put(bean.getProdDocTypeOid(), bean.getProdDocType());
                        bean.addIncoming(prodDocDisplay, quantity, cost);
                    } else if (transInst.getType().isCIType(CIProducts.TransactionOutbound)) {
                        bean.addOutgoing(prodDocDisplay, quantity, cost);
                        outgoingProdDocTypes.put(bean.getProdDocTypeOid(), bean.getProdDocType());
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
                    }

                    @Override
                    protected org.efaps.admin.datamodel.Type getTransactionType(final Parameter _parameter)
                        throws EFapsException
                    {
                        return CIProducts.TransactionInOutAbstract.getType();
                    }

                    @Override
                    protected void addCost(final Parameter _parameter,
                                           final List<InventoryBean> _beans,
                                           final Set<Instance> _prodInsts)
                        throws EFapsException
                    {
                        if (filterValue != null && filterValue.isAcquisition()) {
                            for (final InventoryBean bean : _beans) {
                                final BigDecimal costValue = Costs.getAcquisitionCost4Date(_parameter,
                                                bean.getProdInstance(), Instance.get(filterValue.getObject()),
                                                getDate());
                                bean.setCost(costValue);
                            }
                        } else {
                            super.addCost(_parameter, _beans, _prodInsts);
                        }
                    }
                };
                final DateTime dateTo = (DateTime) filter.get("dateTo");
                // to calculate backwards the start must be the exact date
                inventory.setDate(dateTo.withTimeAtStartOfDay());
                if (InstanceUtils.isValid(alterInst)) {
                    inventory.setCurrencyInst(alterInst);
                    inventory.setAlternativeCurrencyInst(alterInst);
                } else {
                    inventory.setCurrencyInst(Currency.getBaseCurrency());
                }
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

                final List<DataBean> data = new ArrayList<>(beans.values());
                final ComparatorChain<DataBean> chain = new ComparatorChain<>();
                chain.addComparator((_o1, _o2) -> _o1.getProdName().compareTo(_o2.getProdName()));
                Collections.sort(data, chain);
                final Collection<Map<String, ?>> mapCol = new ArrayList<>();
                for (final DataBean bean : data) {
                    mapCol.add(bean.getMap());
                }
                dataSource = new DataSource(mapCol, incomingProdDocTypes, outgoingProdDocTypes);
                getFilteredReport().cache(_parameter, dataSource);
            }
        }

        /**
         * Adds the two query bldr for transaction.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _docQueryBldr the query bldr
         * @throws EFapsException on error
         */
        protected void add2QueryBldr4Doc(final Parameter _parameter,
                                         final QueryBuilder _docQueryBldr)
            throws EFapsException
        {
            final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);
            if (filter.containsKey("productDocumentType")) {
                final InstanceSetFilterValue filterValue = (InstanceSetFilterValue) filter.get("productDocumentType");
                final Set<Instance> instances = filterValue.getObject();
                if (instances != null && !instances.isEmpty()) {
                    final QueryBuilder queryBldr = new QueryBuilder(CIERP.Document2DocumentTypeAbstract);
                    queryBldr.addWhereAttrEqValue(CIERP.Document2DocumentTypeAbstract.DocumentTypeLinkAbstract,
                                    instances.toArray());
                    if (filterValue.isNegate()) {
                        _docQueryBldr.addWhereAttrNotInQuery(CIERP.DocumentAbstract.ID, queryBldr.getAttributeQuery(
                                    CIERP.Document2DocumentTypeAbstract.DocumentLinkAbstract));
                    } else {
                        _docQueryBldr.addWhereAttrInQuery(CIERP.DocumentAbstract.ID, queryBldr.getAttributeQuery(
                                        CIERP.Document2DocumentTypeAbstract.DocumentLinkAbstract));
                    }
                }
            }
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

        /**
         * Evaluate prod doc display.
         *
         * @param _parameter the parameter
         * @return the prod doc display
         * @throws EFapsException the e faps exception
         */
        protected ProdDocDisplay evaluateProdDocDisplay(final Parameter _parameter)
            throws EFapsException
        {
            final ProdDocDisplay ret;
            final Map<String, Object> filter = filteredReport.getFilterMap(_parameter);
            final EnumFilterValue value = (EnumFilterValue) filter.get("prodDocDisplay");
            if (value != null) {
                ret = (ProdDocDisplay) value.getObject();
            } else {
                ret = ProdDocDisplay.NONE;
            }
            return ret;
        }

        @Override
        protected void addColumnDefinition(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            evalDataSource(_parameter);
            final ProdDocDisplay prodDocDisplay = evaluateProdDocDisplay(_parameter);

            final TextColumnBuilder<String> prodNameColumn = DynamicReports.col.column(filteredReport
                            .getDBProperty("ProdName"), "prodName", DynamicReports.type.stringType());
            final TextColumnBuilder<String> prodDescrColumn = DynamicReports.col.column(filteredReport
                            .getDBProperty("ProdDescr"), "prodDescr", DynamicReports.type.stringType())
                            .setFixedWidth(150);

            final TextColumnBuilder<BigDecimal> startQtyColumn = DynamicReports.col.column(filteredReport
                            .getDBProperty("StartQty"), "startQty", DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> startValueColumn = DynamicReports.col.column(filteredReport
                            .getDBProperty("StartValue"), "startValue", DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> startUnitPriceColumn = DynamicReports.col.column(filteredReport
                            .getDBProperty("StartUnitPrice"), "startUnitPrice", DynamicReports.type.bigDecimalType());

            final List<ColumnBuilder<?, ?>> incomingColumns = new ArrayList<>();
            if (ProdDocDisplay.BOTH.equals(prodDocDisplay) || ProdDocDisplay.INCOMING.equals(prodDocDisplay)) {
                final Map<String, String> sorted = new TreeMap<>((_key0,_key1)
                            -> DynProductsTransactionSummaryReport.this.dataSource.getIncomingProdDocTypes().get(_key0)
                            .compareTo(
                          DynProductsTransactionSummaryReport.this.dataSource.getIncomingProdDocTypes().get(_key1)));
                sorted.putAll(dataSource.getIncomingProdDocTypes());
                for (final Entry<String, String> entry : sorted.entrySet()) {
                    final String qtyKey = "INCOMING-Qty" + entry.getKey();
                    final String valueKey = "INCOMING-Value" + entry.getKey();
                    incomingColumns.add(DynamicReports.col.column(entry.getValue() + " - "
                                    + filteredReport.getDBProperty("IncomingQty"), qtyKey, DynamicReports.type
                                    .bigDecimalType()));
                    incomingColumns.add(DynamicReports.col.column(entry.getValue() + " - "
                                    + filteredReport.getDBProperty("IncomingValue"), valueKey, DynamicReports.type
                                    .bigDecimalType()));
                }
            }
            incomingColumns.add(DynamicReports.col.column(filteredReport.getDBProperty("IncomingQty"),
                            "incomingQty", DynamicReports.type.bigDecimalType()));
            incomingColumns.add(DynamicReports.col.column(filteredReport.getDBProperty("IncomingUnitPrice"),
                            "incomingUnitPrice", DynamicReports.type.bigDecimalType()));
            incomingColumns.add(DynamicReports.col.column(filteredReport.getDBProperty("IncomingValue"),
                            "incomingValue", DynamicReports.type.bigDecimalType()));

            final List<ColumnBuilder<?, ?>> outgoingColumns = new ArrayList<>();
            if (ProdDocDisplay.BOTH.equals(prodDocDisplay) || ProdDocDisplay.OUTGOOING.equals(prodDocDisplay)) {
                final Map<String, String> sorted = new TreeMap<>((_key0,_key1)
                                -> DynProductsTransactionSummaryReport.this.dataSource.getOutgoingProdDocTypes()
                                .get(_key0).compareTo(
                          DynProductsTransactionSummaryReport.this.dataSource.getOutgoingProdDocTypes().get(_key1)));
                sorted.putAll(dataSource.getOutgoingProdDocTypes());
                for (final Entry<String, String> entry : sorted.entrySet()) {
                    final String qtyKey = "OUTGOOING-Qty" + entry.getKey();
                    final String valueKey = "OUTGOOING-Value" + entry.getKey();
                    outgoingColumns.add(DynamicReports.col.column(entry.getValue() + " - "
                                    + filteredReport.getDBProperty("OutgoingQty"), qtyKey, DynamicReports.type
                                    .bigDecimalType()));
                    outgoingColumns.add(DynamicReports.col.column(entry.getValue() + " - "
                                    + filteredReport.getDBProperty("OutgoingValue"), valueKey, DynamicReports.type
                                    .bigDecimalType()));
                }
            }

            outgoingColumns.add(DynamicReports.col.column(filteredReport.getDBProperty("OutgoingQty"),
                            "outgoingQty", DynamicReports.type.bigDecimalType()));
            outgoingColumns.add(DynamicReports.col.column(filteredReport.getDBProperty("OutgoingUnitPrice"),
                            "outgoingUnitPrice", DynamicReports.type.bigDecimalType()));
            outgoingColumns.add(DynamicReports.col.column(filteredReport.getDBProperty("OutgoingValue"),
                            "outgoingValue", DynamicReports.type.bigDecimalType()));

            final TextColumnBuilder<BigDecimal> endQtyColumn = DynamicReports.col.column(filteredReport
                            .getDBProperty("EndQty"), "endQty", DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> endValueColumn = DynamicReports.col.column(filteredReport
                            .getDBProperty("EndValue"), "endValue", DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> endUnitPriceColumn = DynamicReports.col.column(filteredReport
                            .getDBProperty("EndUnitPrice"), "endUnitPrice", DynamicReports.type.bigDecimalType());

            final ColumnTitleGroupBuilder startGrp = DynamicReports.grid.titleGroup(filteredReport.getDBProperty(
                            "StartTitleGrp"), startQtyColumn,  startUnitPriceColumn, startValueColumn);
            final ColumnTitleGroupBuilder inGrp = DynamicReports.grid.titleGroup(filteredReport.getDBProperty(
                            "IncomingTitleGrp"),
                            incomingColumns.toArray(new ColumnGridComponentBuilder[incomingColumns.size()]));
            final ColumnTitleGroupBuilder outGrp = DynamicReports.grid.titleGroup(filteredReport.getDBProperty(
                            "OutgoingTitleGrp"),
                            outgoingColumns.toArray(new ColumnGridComponentBuilder[outgoingColumns.size()]));

            final ColumnTitleGroupBuilder endGrp = DynamicReports.grid.titleGroup(filteredReport.getDBProperty(
                            "EndTitleGrp"), endQtyColumn, endUnitPriceColumn, endValueColumn);

            _builder.columnGrid(prodNameColumn, prodDescrColumn, startGrp, inGrp, outGrp, endGrp)
                .addColumn(prodNameColumn, prodDescrColumn, startQtyColumn, startUnitPriceColumn, startValueColumn)
                .addColumn(incomingColumns.toArray(new ColumnBuilder[incomingColumns.size()]))
                .addColumn(outgoingColumns.toArray(new ColumnBuilder[outgoingColumns.size()]))
                .addColumn(endQtyColumn, endUnitPriceColumn, endValueColumn);
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

        /** The prod doc type. */
        private String prodDocType;

        /** The prod doc type. */
        private String prodDocTypeOid;

        /** The prod doc type map. */
        private final Map<String, BigDecimal> prodDocTypeMap = new HashMap<>();
        /**
         * Getter method for the instance variable {@link #prodInst}.
         *
         * @return value of instance variable {@link #prodInst}
         */
        public Instance getProdInst()
        {
            return prodInst;
        }

        /**
         * Setter method for instance variable {@link #prodInst}.
         *
         * @param _prodInst value for instance variable {@link #prodInst}
         * @return the data bean
         */
        public DataBean setProdInst(final Instance _prodInst)
        {
            prodInst = _prodInst;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #incoming}.
         *
         * @return value of instance variable {@link #incoming}
         */
        public BigDecimal getIncomingQty()
        {
            return incomingQty;
        }

        /**
         * Setter method for instance variable {@link #incoming}.
         *
         * @param _prodDocDisplay the prod doc display
         * @param _qty the qty
         * @param _cost the cost
         * @return the data bean
         */
        public DataBean addIncoming(final ProdDocDisplay _prodDocDisplay, final BigDecimal _qty,
                                    final BigDecimal _cost)
        {
            if (incomingQty == null) {
                incomingQty = BigDecimal.ZERO;
            }
            if (incomingValue == null) {
                incomingValue = BigDecimal.ZERO;
            }
            incomingQty = incomingQty.add(_qty);
            incomingValue = incomingValue.add(_qty.multiply(_cost));
            if (ProdDocDisplay.INCOMING.equals(_prodDocDisplay) || ProdDocDisplay.BOTH.equals(_prodDocDisplay)) {
                final String qtyKey = "INCOMING-Qty" + getProdDocTypeOid();
                final String valueKey = "INCOMING-Value" + getProdDocTypeOid();
                if (!prodDocTypeMap.containsKey(qtyKey)) {
                    prodDocTypeMap.put(qtyKey, BigDecimal.ZERO);
                }
                if (!prodDocTypeMap.containsKey(valueKey)) {
                    prodDocTypeMap.put(valueKey, BigDecimal.ZERO);
                }
                prodDocTypeMap.put(qtyKey, prodDocTypeMap.get(qtyKey).add(_qty));
                prodDocTypeMap.put(valueKey, prodDocTypeMap.get(valueKey).add(_qty.multiply(_cost)));
            }
            return this;
        }

        /**
         * Getter method for the instance variable {@link #outgoing}.
         *
         * @return value of instance variable {@link #outgoing}
         */
        public BigDecimal getOutgoing()
        {
            return outgoingQty;
        }

        /**
         * Setter method for instance variable {@link #incoming}.
         *
         * @param _prodDocDisplay the prod doc display
         * @param _qty the qty
         * @param _cost the cost
         * @return the data bean
         */
        public DataBean addOutgoing(final ProdDocDisplay _prodDocDisplay,
                                    final BigDecimal _qty,
                                    final BigDecimal _cost)
        {
            if (outgoingQty == null) {
                outgoingQty = BigDecimal.ZERO;
            }
            if (outgoingValue == null) {
                outgoingValue = BigDecimal.ZERO;
            }
            outgoingQty = outgoingQty.add(_qty);
            outgoingValue = outgoingValue.add(_qty.multiply(_cost));
            if (ProdDocDisplay.OUTGOOING.equals(_prodDocDisplay) || ProdDocDisplay.BOTH.equals(_prodDocDisplay)) {
                final String qtyKey = "OUTGOOING-Qty" + getProdDocTypeOid();
                final String valueKey = "OUTGOOING-Value" + getProdDocTypeOid();
                if (!prodDocTypeMap.containsKey(qtyKey)) {
                    prodDocTypeMap.put(qtyKey, BigDecimal.ZERO);
                }
                if (!prodDocTypeMap.containsKey(valueKey)) {
                    prodDocTypeMap.put(valueKey, BigDecimal.ZERO);
                }
                prodDocTypeMap.put(qtyKey, prodDocTypeMap.get(qtyKey).add(_qty));
                prodDocTypeMap.put(valueKey, prodDocTypeMap.get(valueKey).add(_qty.multiply(_cost)));
            }
            return this;
        }

        /**
         * Getter method for the instance variable {@link #prodName}.
         *
         * @return value of instance variable {@link #prodName}
         */
        public String getProdName()
        {
            return prodName;
        }

        /**
         * Setter method for instance variable {@link #prodName}.
         *
         * @param _prodName value for instance variable {@link #prodName}
         * @return the data bean
         */
        public DataBean setProdName(final String _prodName)
        {
            prodName = _prodName;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #endValue}.
         *
         * @return value of instance variable {@link #endValue}
         */
        public BigDecimal getQty()
        {
            return endQty;
        }

        /**
         * Setter method for instance variable {@link #endValue}.
         *
         * @param _endQty the end qty
         * @return the data bean
         */
        public DataBean setEndQty(final BigDecimal _endQty)
        {
            endQty = _endQty;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #incomingValue}.
         *
         * @return value of instance variable {@link #incomingValue}
         */
        public BigDecimal getIncomingValue()
        {
            return incomingValue;
        }

        /**
         * Getter method for the instance variable {@link #outgoingQty}.
         *
         * @return value of instance variable {@link #outgoingQty}
         */
        public BigDecimal getOutgoingQty()
        {
            return outgoingQty;
        }

        /**
         * Setter method for instance variable {@link #outgoingQty}.
         *
         * @param _outgoingQty value for instance variable {@link #outgoingQty}
         */
        public void setOutgoingQty(final BigDecimal _outgoingQty)
        {
            outgoingQty = _outgoingQty;
        }

        /**
         * Getter method for the instance variable {@link #outgoingValue}.
         *
         * @return value of instance variable {@link #outgoingValue}
         */
        public BigDecimal getOutgoingValue()
        {
            return outgoingValue;
        }

        /**
         * Setter method for instance variable {@link #outgoingValue}.
         *
         * @param _outgoingValue value for instance variable
         *            {@link #outgoingValue}
         */
        public void setOutgoingValue(final BigDecimal _outgoingValue)
        {
            outgoingValue = _outgoingValue;
        }

        /**
         * Getter method for the instance variable {@link #endValue}.
         *
         * @return value of instance variable {@link #endValue}
         */
        public BigDecimal getEndValue()
        {
            return endValue;
        }

        /**
         * Setter method for instance variable {@link #endValue}.
         *
         * @param _endValue value for instance variable {@link #endValue}
         * @return the data bean
         */
        public DataBean setEndValue(final BigDecimal _endValue)
        {
            endValue = _endValue;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #endQty}.
         *
         * @return value of instance variable {@link #endQty}
         */
        public BigDecimal getEndQty()
        {
            return endQty;
        }

        /**
         * Setter method for instance variable {@link #incomingQty}.
         *
         * @param _incomingQty value for instance variable {@link #incomingQty}
         */
        public void setIncomingQty(final BigDecimal _incomingQty)
        {
            incomingQty = _incomingQty;
        }

        /**
         * Gets the incoming unit price.
         *
         * @return the incoming unit price
         */
        public BigDecimal getIncomingUnitPrice()
        {
            BigDecimal ret = null;
            if (incomingQty != null && incomingValue != null) {
                ret = incomingValue.divide(incomingQty, 4, RoundingMode.HALF_UP);
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
            if (outgoingQty != null && outgoingValue != null) {
                ret = outgoingValue.divide(outgoingQty, 4, RoundingMode.HALF_UP);
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
            if (endQty != null && endValue != null) {
                ret = endValue.divide(endQty, 4, RoundingMode.HALF_UP);
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
            final BigDecimal end = endValue == null ? BigDecimal.ZERO : endValue;
            final BigDecimal out = outgoingValue == null ? BigDecimal.ZERO : outgoingValue;
            final BigDecimal in = incomingValue == null ? BigDecimal.ZERO : incomingValue;
            return end.subtract(in).add(out);
        }

        /**
         * Getter method for the instance variable {@link #endQty}.
         *
         * @return value of instance variable {@link #endQty}
         */
        public BigDecimal getStartQty()
        {
            final BigDecimal end = endQty == null ? BigDecimal.ZERO : endQty;
            final BigDecimal out = outgoingQty == null ? BigDecimal.ZERO : outgoingQty;
            final BigDecimal in = incomingQty == null ? BigDecimal.ZERO : incomingQty;
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
            return prodDescr;
        }

        /**
         * Setter method for instance variable {@link #prodDescr}.
         *
         * @param _prodDescr value for instance variable {@link #prodDescr}
         * @return the data bean
         */
        public DataBean setProdDescr(final String _prodDescr)
        {
            prodDescr = _prodDescr;
            return this;
        }

        /**
         * Gets the prod doc type.
         *
         * @return the prod doc type
         */
        public String getProdDocType()
        {
            return prodDocType;
        }

        /**
         * Sets the prod doc type.
         *
         * @param _prodDocType the new prod doc type
         * @return the data bean
         */
        public DataBean setProdDocType(final String _prodDocType)
        {
            prodDocType = _prodDocType;
            return this;
        }

        /**
         * Gets the prod doc type.
         *
         * @return the prod doc type
         */
        public String getProdDocTypeOid()
        {
            return prodDocTypeOid;
        }

        /**
         * Sets the prod doc type oid.
         *
         * @param _prodDocTypeOid the prod doc type oid
         * @return the data bean
         */
        public DataBean setProdDocTypeOid(final String _prodDocTypeOid)
        {
            prodDocTypeOid = _prodDocTypeOid;
            return this;
        }

        /**
         * Gets the map.
         *
         * @return the map
         */
        public Map<String, Object> getMap() {
            final Map<String, Object> ret = new HashMap<>();
            ret.put("prodName", getProdName());
            ret.put("prodDescr", getProdDescr());
            ret.put("startQty", getStartQty());
            ret.put("startValue", getStartValue());
            ret.put("startUnitPrice", getStartUnitPrice());
            ret.put("incomingQty", getIncomingQty());
            ret.put("incomingValue", getIncomingValue());
            ret.put("incomingUnitPrice", getIncomingUnitPrice());
            ret.put("outgoingQty", getOutgoingQty());
            ret.put("outgoingValue", getOutgoingValue());
            ret.put("outgoingUnitPrice", getOutgoingUnitPrice());
            ret.put("endQty", getEndQty());
            ret.put("endValue", getEndValue());
            ret.put("endUnitPrice", getEndUnitPrice());
            ret.putAll(prodDocTypeMap);
            return ret;
        }

        @Override
        public String toString()
        {
            return ToStringBuilder.reflectionToString(this);
        }
    }

    /**
     * The Class DataSource.
     */
    public static class DataSource
        extends JRMapCollectionDataSource
    {

        /** The incoming prod doc types. */
        private final Map<String, String> incomingProdDocTypes;

        /** The outgoing prod doc types. */
        private final Map<String, String> outgoingProdDocTypes;

        /**
         * Instantiates a new data source.
         *
         * @param _col the col
         */
        public DataSource(final Collection<Map<String, ?>> _col, final Map<String, String> _incomingProdDocTypes,
                          final Map<String, String> _outgoingProdDocTypes)
        {
            super(_col);
            incomingProdDocTypes = _incomingProdDocTypes;
            outgoingProdDocTypes = _outgoingProdDocTypes;
        }

        /**
         * Gets the incoming prod doc types.
         *
         * @return the incoming prod doc types
         */
        public Map<String, String> getIncomingProdDocTypes()
        {
            return incomingProdDocTypes;
        }

        /**
         * Gets the outgoing prod doc types.
         *
         * @return the outgoing prod doc types
         */
        public Map<String, String> getOutgoingProdDocTypes()
        {
            return outgoingProdDocTypes;
        }
    }
}
