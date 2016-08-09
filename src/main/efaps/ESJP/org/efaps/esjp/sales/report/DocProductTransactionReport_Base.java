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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections4.CollectionUtils;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.Listener;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.erp.AbstractGroupedByDate;
import org.efaps.esjp.erp.AbstractGroupedByDate_Base.DateGroup;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.sales.listener.IOnDocProductTransactionReport;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabMeasureBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabRowGroupBuilder;
import net.sf.dynamicreports.report.constant.Calculation;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("a5b8aeef-92cf-4cc5-b585-85bc4c8259c0")
@EFapsApplication("eFapsApp-Sales")
public abstract class DocProductTransactionReport_Base
    extends FilteredReport
{

    /**
     * The Enum DocGroup.
     *
     * @author The eFaps Team
     */
    public enum DocGroup
    {

        /** The none. */
        NONE,

        /** The leftfromprod. */
        LEFTFROMPROD;
    }

    /**
     * The Enum DocGroup.
     *
     * @author The eFaps Team
     */
    public enum ProdType
    {

        /** The product. */
        PRODUCT,

        /** The individual. */
        INDIVIDUAL,

        /** The both. */
        BOTH;
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

    @Override
    protected Properties getProperties4TypeList(final Parameter _parameter,
                                                final String _fieldName)
        throws EFapsException
    {
        return Sales.REPORT_DOCPRODTRANS.get();
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return the report class
     * @throws EFapsException on error
     */
    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new DynDocProductTransactionReport(this);
    }

    /**
     * The Class DynIndividualProductDocReport.
     */
    public static class DynDocProductTransactionReport
        extends AbstractDynamicReport
    {

        /** The filtered report. */
        private final DocProductTransactionReport_Base filteredReport;

        /**
         * Instantiates a new dyn doc product transaction report.
         *
         * @param _filteredReport the filtered report
         */
        public DynDocProductTransactionReport(final DocProductTransactionReport_Base _filteredReport)
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
                    e.printStackTrace();
                }
            } else {
                final DateGroup dateGroup = getDateGroup(_parameter);
                final AbstractGroupedByDate groupedByDate = new AbstractGroupedByDate()
                {
                };
                final DateTimeFormatter dateTimeFormatter = groupedByDate.getDateTimeFormatter(dateGroup);

                final Collection<Map<String, ?>> values = new ArrayList<>();

                final MultiPrintQuery multi = getQueryBldr(_parameter).getPrint();
                final SelectBuilder selDoc = SelectBuilder.get().linkto(CIProducts.TransactionAbstract.Document);
                final SelectBuilder selDocDate = new SelectBuilder(selDoc).attribute(CIERP.DocumentAbstract.Date);
                final SelectBuilder selDocName = new SelectBuilder(selDoc).attribute(CIERP.DocumentAbstract.Name);

                final SelectBuilder selProduct = SelectBuilder.get().linkto(CIProducts.TransactionAbstract.Product);
                final SelectBuilder selProductInst = new SelectBuilder(selProduct).instance();
                final SelectBuilder selProductName = new SelectBuilder(selProduct)
                                .attribute(CIProducts.ProductAbstract.Name);
                multi.addSelect(selProductInst, selProductName, selDocDate, selDocName);
                multi.addAttribute(CIProducts.TransactionAbstract.Quantity);
                multi.execute();
                while (multi.next()) {
                    final Map<String, Object> map = new HashMap<>();
                    values.add(map);
                    final BigDecimal quantity = multi.getAttribute(CIProducts.TransactionAbstract.Quantity);
                    final Instance productInst = multi.getSelect(selProductInst);
                    final String productName = multi.getSelect(selProductName);
                    final String docName = multi.getSelect(selDocName);
                    final DateTime date = multi.getSelect(selDocDate);
                    map.put("quantity", quantity);
                    map.put("product", productName);
                    map.put("productInst", productInst);
                    map.put("docName", docName);
                    map.put("partial", groupedByDate.getPartial(date, dateGroup).toString(dateTimeFormatter));
                }

                for (final IOnDocProductTransactionReport listener : Listener.get()
                                .<IOnDocProductTransactionReport>invoke(IOnDocProductTransactionReport.class)) {
                    listener.updateValues(_parameter, this, values);
                }

                ret = new JRMapCollectionDataSource(values);
                getFilteredReport().cache(_parameter, ret);
            }
            return ret;
        }

        /**
         * Gets the query bldr.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the query bldr
         * @throws EFapsException on error
         */
        protected QueryBuilder getQueryBldr(final Parameter _parameter)
            throws EFapsException
        {
            final List<Type> types = getFilteredReport().getTypeList(_parameter);
            final QueryBuilder prodDocQueryBldr;
            if (CollectionUtils.isNotEmpty(types)) {
                prodDocQueryBldr = new QueryBuilder(types.get(0));
                boolean first = true;
                for (final Iterator<Type> iterator = types.iterator(); iterator.hasNext();) {
                    final Type type = iterator.next();
                    if (first) {
                        first = false;
                    } else {
                        prodDocQueryBldr.addType(type);
                    }
                }
            } else {
                prodDocQueryBldr = new QueryBuilder(CISales.DeliveryNote);
            }

            final List<Status> statusList = getStatusListFromProperties(_parameter, Sales.REPORT_DOCPRODTRANS.get());
            if (CollectionUtils.isNotEmpty(statusList)) {
                prodDocQueryBldr.addWhereAttrEqValue(CIERP.DocumentAbstract.StatusAbstract, statusList.toArray());
            }
            add2ProdDocQueryBuilder(_parameter, prodDocQueryBldr);

            final QueryBuilder transQueryBldr = new QueryBuilder(CIProducts.TransactionAbstract);
            transQueryBldr.addWhereAttrInQuery(CIProducts.TransactionAbstract.Document, prodDocQueryBldr
                            .getAttributeQuery(CIERP.DocumentAbstract.ID));
            add2TransactionQueryBuilder(_parameter, transQueryBldr);
            return transQueryBldr;
        }

        /**
         * Adds to the  transaction query builder.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _queryBldr the query bldr
         * @throws EFapsException on error
         */
        protected void add2TransactionQueryBuilder(final Parameter _parameter,
                                                   final QueryBuilder _queryBldr)
            throws EFapsException
        {
            final ProdType prodType = getProdType(_parameter);
            switch (prodType) {
                case PRODUCT:
                    final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.ProductIndividualAbstract);
                    _queryBldr.addWhereAttrNotInQuery(CIProducts.TransactionAbstract.Product, attrQueryBldr
                                    .getAttributeQuery(CIProducts.ProductAbstract.ID));
                    break;
                case INDIVIDUAL:
                    final QueryBuilder attrQueryBldr2 = new QueryBuilder(CIProducts.ProductIndividualAbstract);
                    _queryBldr.addWhereAttrInQuery(CIProducts.TransactionAbstract.Product, attrQueryBldr2
                                    .getAttributeQuery(CIProducts.ProductAbstract.ID));
                    break;
                default:
                    break;
            }
        }

        /**
         * Adds to the Product document Querybuilder.
         *
         * @param _parameter the parameter
         * @param _queryBldr the query bldr
         * @throws EFapsException on error
         */
        protected void add2ProdDocQueryBuilder(final Parameter _parameter,
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
            if (filter.containsKey("prodDocType")) {
                final InstanceSetFilterValue filterValue = (InstanceSetFilterValue) filter.get("prodDocType");
                if (CollectionUtils.isNotEmpty(filterValue.getObject())) {
                    final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Document2ProductDocumentType);
                    attrQueryBldr.addWhereAttrEqValue(CISales.Document2ProductDocumentType.DocumentTypeLink,
                                    filterValue.getObject().toArray());
                    if (filterValue.isNegate()) {
                        _queryBldr.addWhereAttrNotInQuery(CIERP.DocumentAbstract.ID,
                                    attrQueryBldr.getAttributeQuery(CISales.Document2ProductDocumentType.DocumentLink));
                    } else {
                        _queryBldr.addWhereAttrInQuery(CIERP.DocumentAbstract.ID,
                                    attrQueryBldr.getAttributeQuery(CISales.Document2ProductDocumentType.DocumentLink));
                    }
                }
            }

            _queryBldr.addWhereAttrGreaterValue(CIERP.DocumentAbstract.Date, dateFrom
                            .withTimeAtStartOfDay().minusMinutes(1));
            _queryBldr.addWhereAttrLessValue(CIERP.DocumentAbstract.Date, dateTo.plusDays(1)
                            .withTimeAtStartOfDay());
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final CrosstabBuilder crosstab = DynamicReports.ctab.crosstab();

            final CrosstabMeasureBuilder<BigDecimal> quantityMeasure = DynamicReports.ctab.measure(
                            getFilteredReport().getDBProperty("Quantity"), "quantity", BigDecimal.class,
                            Calculation.SUM);
            crosstab.addMeasure(quantityMeasure);

            final DocGroup docGroup = getDocGroup(_parameter);
            if (DocGroup.LEFTFROMPROD.equals(docGroup)) {
                final CrosstabRowGroupBuilder<String> docNameRowGroup = DynamicReports.ctab
                                .rowGroup("docName", String.class);
                crosstab.addRowGroup(docNameRowGroup);
            }

            final CrosstabRowGroupBuilder<String> productRowGroup = DynamicReports.ctab
                            .rowGroup("product", String.class)
                            .setHeaderWidth(150);
            crosstab.addRowGroup(productRowGroup);

            final CrosstabColumnGroupBuilder<String> partialColumnGroup = DynamicReports.ctab.columnGroup("partial",
                            String.class);
            crosstab.addColumnGroup(partialColumnGroup);

            _builder.addSummary(crosstab);
        }

        /**
         * Getter method for the instance variable {@link #filteredReport}.
         *
         * @return value of instance variable {@link #filteredReport}
         */
        public DocProductTransactionReport_Base getFilteredReport()
        {
            return this.filteredReport;
        }

        /**
         * Gets the doc Group.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the doc Group
         * @throws EFapsException on error
         */
        protected DocGroup getDocGroup(final Parameter _parameter)
            throws EFapsException
        {
            DocGroup ret = DocGroup.NONE;
            final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);
            if (filter.containsKey("docGroup") && filter.get("docGroup") != null) {
                final EnumFilterValue filterValue = (EnumFilterValue) filter.get("docGroup");
                ret = (DocGroup) filterValue.getObject();
            }
            return ret;
        }

        /**
         * Gets the date group.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the date group
         * @throws EFapsException on error
         */
        protected DateGroup getDateGroup(final Parameter _parameter)
            throws EFapsException
        {
            final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);
            final AbstractGroupedByDate.DateGroup ret;
            if (filter.containsKey("dateGroup") && filter.get("dateGroup") != null) {
                ret = (AbstractGroupedByDate.DateGroup) ((EnumFilterValue) filter.get("dateGroup")).getObject();
            } else {
                ret = DocumentSumGroupedByDate_Base.DateGroup.MONTH;
            }
            return ret;
        }

        /**
         * Gets the date group.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the date group
         * @throws EFapsException on error
         */
        protected ProdType getProdType(final Parameter _parameter)
            throws EFapsException
        {
            final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);
            final ProdType ret;
            if (filter.containsKey("prodType") && filter.get("prodType") != null) {
                ret = (ProdType) ((EnumFilterValue) filter.get("prodType")).getObject();
            } else {
                ret = ProdType.BOTH;
            }
            return ret;
        }
    }
}
