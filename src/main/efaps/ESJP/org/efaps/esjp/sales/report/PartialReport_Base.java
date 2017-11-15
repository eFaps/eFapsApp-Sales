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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.efaps.admin.datamodel.Attribute;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.CachedMultiPrintQuery;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport_Base.ExportType;
import org.efaps.esjp.common.properties.PropertiesUtil;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.base.expression.AbstractSimpleExpression;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.ComponentColumnBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.component.SubreportBuilder;
import net.sf.dynamicreports.report.builder.group.ColumnGroupBuilder;
import net.sf.dynamicreports.report.constant.StretchType;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

// TODO: Auto-generated Javadoc
/**
 * TODO comment!.
 *
 * @author The eFaps Team
 */
@EFapsUUID("14caf734-cf91-482e-acc5-837da9df78df")
@EFapsApplication("eFapsApp-Sales")
public abstract class PartialReport_Base
    extends FilteredReport
{

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DocVsDocReport.class);

    /**
     * Generate report.
     *
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
        final String mime = getProperty(_parameter, "Mime", "pdf");
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
        return Sales.REPORT_PARTIAL.get();
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
        return new DynPartialReport(this);
    }

    /**
     * The Class DynSalesProductReport.
     *
     * @author The eFaps Team
     */
    public static class DynPartialReport
        extends AbstractDynamicReport
    {

        /** The filtered report. */
        private final PartialReport_Base filteredReport;

        /**
         * Instantiates a new dyn doc vs doc report.
         *
         * @param _filteredReport the filtered report
         */
        public DynPartialReport(final PartialReport_Base _filteredReport)
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
                    throw new EFapsException("JRException", e);
                }
            } else {
                final Map<Instance, Set<Instance>> origin2partial = new HashMap<>();

                final QueryBuilder queryBldr = new QueryBuilder(CIERP.Document2DocumentAbstract);
                queryBldr.addWhereAttrEqValue(CIERP.Document2DocumentAbstract.Situation,
                                ERP.DocRelationSituation.PARTIAL);
                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder selFromInst = SelectBuilder.get().linkto(
                                CIERP.Document2DocumentAbstract.FromAbstractLink).instance();
                final SelectBuilder selToInst = SelectBuilder.get().linkto(
                                CIERP.Document2DocumentAbstract.ToAbstractLink).instance();
                multi.addSelect(selFromInst, selToInst);
                multi.execute();
                final Attribute fromLinkAttr = CIERP.Document2DocumentAbstract.getType().getAttribute(
                                CIERP.Document2DocumentAbstract.FromAbstractLink.name);
                while (multi.next()) {
                    final Instance relInst = multi.getCurrentInstance();
                    final Instance fromInst = multi.getSelect(selFromInst);
                    final Instance toInst = multi.getSelect(selToInst);
                    final Properties properties = PropertiesUtil.getProperties4Prefix(Sales.PARTIALCONFIG.get(), relInst
                                    .getType().getName());
                    final String origLink = properties.getProperty("RelationOriginLink");

                    final Instance origin;
                    final Instance partial;
                    if (fromLinkAttr.getSqlColNames().toString().equals(relInst.getType().getAttribute(origLink)
                                    .getSqlColNames().toString())) {
                        origin = fromInst;
                        partial = toInst;
                    } else {
                        origin = toInst;
                        partial = fromInst;
                    }
                    final Set<Instance> partials;
                    if (origin2partial.containsKey(origin)) {
                        partials = origin2partial.get(origin);
                    } else {
                        partials = new HashSet<>();
                        origin2partial.put(origin, partials);
                    }
                    partials.add(partial);
                }
                final List<DataBean> dataSource = new ArrayList<>();
                for (final Entry<Instance, Set<Instance>> entry : origin2partial.entrySet()) {
                    final List<DataBean> origins = getOriginBeans(_parameter, entry.getKey());
                    final List<PartialBean> partials = getPartialBeans(_parameter, entry.getValue().toArray());
                    for (final DataBean origin : origins) {
                        for (final PartialBean partial : partials) {
                            if (origin.getProdInst().equals(partial.getProdInst())) {
                                origin.addPartial(partial);
                            }
                        }
                    }
                    dataSource.addAll(origins);
                }

                ret = new JRBeanCollectionDataSource(dataSource);
                getFilteredReport().cache(_parameter, ret);
            }
            return ret;
        }

        /**
         * Gets the origin beans.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _originInst the origin inst
         * @return the origin beans
         * @throws EFapsException on error
         */
        protected List<DataBean> getOriginBeans(final Parameter _parameter,
                                                final Instance _originInst)
            throws EFapsException
        {
            final List<DataBean> ret = new ArrayList<>();
            final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionAbstract);
            queryBldr.addWhereAttrEqValue(CISales.PositionAbstract.DocumentAbstractLink, _originInst);
            final CachedMultiPrintQuery multi = queryBldr.getCachedPrint4Request();

            final SelectBuilder selDoc = SelectBuilder.get().linkto(CISales.PositionAbstract.DocumentAbstractLink);
            final SelectBuilder selDocInst = new SelectBuilder(selDoc).instance();
            final SelectBuilder selDocName = new SelectBuilder(selDoc).attribute(CISales.DocumentAbstract.Name);
            final SelectBuilder selContactName = new SelectBuilder(selDoc).linkto(CIERP.DocumentAbstract.Contact)
                            .attribute(CIContacts.Contact.Name);

            final SelectBuilder selProdInst = SelectBuilder.get().linkto(CISales.PositionAbstract.Product).instance();
            final SelectBuilder selProdName = SelectBuilder.get().linkto(CISales.PositionAbstract.Product).attribute(
                            CIProducts.ProductAbstract.Name);
            multi.addSelect(selDocInst, selDocName, selContactName, selProdInst, selProdName);
            multi.addAttribute(CISales.PositionAbstract.Quantity, CISales.PositionAbstract.ProductDesc);
            multi.execute();
            while (multi.next()) {
                final DataBean bean = new DataBean()
                                .setContactName(multi.getSelect(selContactName))
                                .setDocName(multi.<Instance>getSelect(selDocInst).getType().getLabel()
                                                + " - " + multi.getSelect(selDocName))
                                .setQuantity(multi.getAttribute(CISales.PositionAbstract.Quantity))
                                .setProdInst(multi.getSelect(selProdInst))
                                .setProdName(multi.getSelect(selProdName))
                                .setProdDescr(multi.getAttribute(CISales.PositionAbstract.ProductDesc));
                ret.add(bean);
            }
            return ret;
        }


        /**
         * Gets the partial beans.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _partialInsts the partial insts
         * @return the partial beans
         * @throws EFapsException on error
         */
        protected List<PartialBean> getPartialBeans(final Parameter _parameter,
                                                    final Object... _partialInsts)
            throws EFapsException
        {
            final List<PartialBean> ret = new ArrayList<>();
            final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionAbstract);
            queryBldr.addWhereAttrEqValue(CISales.PositionAbstract.DocumentAbstractLink, _partialInsts);
            final CachedMultiPrintQuery multi = queryBldr.getCachedPrint4Request();

            final SelectBuilder selDoc = SelectBuilder.get().linkto(CISales.PositionAbstract.DocumentAbstractLink);
            final SelectBuilder selDocInst = new SelectBuilder(selDoc).instance();
            final SelectBuilder selDocName = new SelectBuilder(selDoc).attribute(CISales.DocumentAbstract.Name);
            final SelectBuilder selProdInst = SelectBuilder.get().linkto(CISales.PositionAbstract.Product).instance();
            multi.addSelect(selDocName, selDocInst, selProdInst);
            multi.addAttribute(CISales.PositionAbstract.Quantity, CISales.PositionAbstract.ProductDesc);
            multi.execute();
            while (multi.next()) {
                final PartialBean bean = new PartialBean()
                                .setDocType(multi.<Instance>getSelect(selDocInst).getType().getLabel())
                                .setDocName(multi.getSelect(selDocName))
                                .setQuantity(multi.getAttribute(CISales.PositionAbstract.Quantity))
                                .setProdInst(multi.getSelect(selProdInst));
                ret.add(bean);
            }
            return ret;
        }

        @Override
        protected void addColumnDefinition(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {

            final TextColumnBuilder<String> contactNameColumn = DynamicReports.col.column(this.filteredReport
                            .getDBProperty("column.ContactName"), "contactName", DynamicReports.type.stringType());

            final TextColumnBuilder<String> docNameColumn = DynamicReports.col.column(this.filteredReport
                            .getDBProperty("column.originDocName"), "docName", DynamicReports.type.stringType());

            final TextColumnBuilder<BigDecimal> quantityColumn = DynamicReports.col.column(this.filteredReport
                            .getDBProperty("column.quantityColumn"), "quantity", DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<String> prodNameColumn = DynamicReports.col.column(this.filteredReport
                            .getDBProperty("column.prodName"), "prodName", DynamicReports.type.stringType());

            final TextColumnBuilder<String> prodDescrColumn = DynamicReports.col.column(this.filteredReport
                            .getDBProperty("column.prodDescr"), "prodDescr", DynamicReports.type.stringType());

            final ColumnGroupBuilder contactGroup = DynamicReports.grp.group(contactNameColumn).groupByDataType();
            final ColumnGroupBuilder docNameGroup = DynamicReports.grp.group(docNameColumn).groupByDataType();

            final SubreportBuilder subReport = DynamicReports.cmp
                            .subreport(getSubreportDesign(_parameter, getExType()))
                            .setDataSource(getSubreportDataSource(_parameter))
                            .setStretchType(StretchType.CONTAINER_HEIGHT)
                            .setStyle(DynamicReports.stl.style().setBorder(DynamicReports.stl.pen1Point()));

            final ComponentColumnBuilder subReportColumn = DynamicReports.col.componentColumn(this.filteredReport
                            .getDBProperty("column.subReport"), subReport).setMinWidth(250);

            _builder.fields(DynamicReports.field("partials", List.class))
                .addGroup(contactGroup, docNameGroup)
                .addColumn(contactNameColumn, docNameColumn, quantityColumn, prodNameColumn, prodDescrColumn,
                                subReportColumn);
        }

        /**
         * Gets the contact inst.
         *
         * @param _parameter the _parameter
         * @return the contact inst
         * @throws EFapsException on error
         */
        protected Object[] getContactInsts(final Parameter _parameter)
            throws EFapsException
        {
            final Object[] ret;
            final Map<String, Object> filterMap = this.filteredReport.getFilterMap(_parameter);
            final InstanceSetFilterValue filter = (InstanceSetFilterValue) filterMap.get("contact");
            if (filter == null || (filter != null && filter.getObject() == null)) {
                ret = new Object[] {};
            } else {
                ret = filter.getObject().toArray();
            }
            return ret;
        }

        /**
         * Gets the subreport data source.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the subreport data source
         */
        protected SubreportDataSource getSubreportDataSource(final Parameter _parameter)
        {
            return new SubreportDataSource();
        }

        /**
         * Gets the subreport design.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _exType the ex type
         * @return the subreport design
         */
        protected SubreportDesign getSubreportDesign(final Parameter _parameter,
                                                     final ExportType _exType)
        {
            return new SubreportDesign(_exType);
        }


        /**
         * Getter method for the instance variable {@link #filteredReport}.
         *
         * @return value of instance variable {@link #filteredReport}
         */
        protected PartialReport_Base getFilteredReport()
        {
            return this.filteredReport;
        }
    }

    /**
     * The Class PartialBean.
     *
     * @author The eFaps Team
     */
    public static class PartialBean
    {
        /** The origin doc name. */
        private String docName;

        /** The doc type. */
        private String docType;

        /** The prod inst. */
        private Instance prodInst;

        /** The quantity. */
        private BigDecimal quantity;

        /**
         * Gets the origin doc name.
         *
         * @return the origin doc name
         */
        public String getDocName()
        {
            return this.docName;
        }

        /**
         * Sets the doc name.
         *
         * @param _docName the doc name
         * @return the partial bean
         */
        public PartialBean setDocName(final String _docName)
        {
            this.docName = _docName;
            return this;
        }

        /**
         * Gets the prod inst.
         *
         * @return the prod inst
         */
        public Instance getProdInst()
        {
            return this.prodInst;
        }

        /**
         * Sets the prod inst.
         *
         * @param _prodInst the prod inst
         * @return the partial bean
         */
        public PartialBean setProdInst(final Instance _prodInst)
        {
            this.prodInst = _prodInst;
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
         * @return the partial bean
         */
        public PartialBean setQuantity(final BigDecimal _quantity)
        {
            this.quantity = _quantity;
            return this;
        }

        /**
         * Gets the doc type.
         *
         * @return the doc type
         */
        public String getDocType()
        {
            return this.docType;
        }

        /**
         * Sets the doc type.
         *
         * @param _docType the doc type
         * @return the partial bean
         */
        public PartialBean setDocType(final String _docType)
        {
            this.docType = _docType;
            return this;
        }
    }

    /**
     * The Class DataBean.
     *
     * @author The eFaps Team
     */
    public static class DataBean
    {

        /** The quantity. */
        private BigDecimal quantity;

        /** The prod inst. */
        private Instance prodInst;

        /** The prod name. */
        private String prodName;

        /** The prod descr. */
        private String prodDescr;

        /** The contact name. */
        private String contactName;

        /** The origin doc name. */
        private String docName;

        /** The partials. */
        private final List<PartialBean> partials = new ArrayList<>();;


        /**
         * Adds the partial.
         *
         * @param _bean the bean
         * @return the data bean
         */
        public DataBean addPartial(final PartialBean _bean)
        {
            this.partials.add(_bean);
            return this;
        }

        /**
         * Gets the partials.
         *
         * @return the partials
         */
        public List<PartialBean>getPartials()
        {
            return this.partials;
        }

        /**
         * Gets the contact name.
         *
         * @return the contact name
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
         * Gets the origin doc name.
         *
         * @return the origin doc name
         */
        public String getDocName()
        {
            return this.docName;
        }

        /**
         * Sets the doc name.
         *
         * @param _originDocName the origin doc name
         * @return the data bean
         */
        public DataBean setDocName(final String _originDocName)
        {
            this.docName = _originDocName;
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
         * Gets the prod inst.
         *
         * @return the prod inst
         */
        public Instance getProdInst()
        {
            return this.prodInst;
        }

        /**
         * Sets the prod inst.
         *
         * @param _prodInst the prod inst
         * @return the data bean
         */
        public DataBean setProdInst(final Instance _prodInst)
        {
            this.prodInst = _prodInst;
            return this;
        }

        /**
         * Gets the prod name.
         *
         * @return the prod name
         */
        public String getProdName()
        {
            return this.prodName;
        }

        /**
         * Sets the prod name.
         *
         * @param _prodName the prod name
         * @return the data bean
         */
        public DataBean setProdName(final String _prodName)
        {
            this.prodName = _prodName;
            return this;
        }

        /**
         * Gets the prod descr.
         *
         * @return the prod descr
         */
        public String getProdDescr()
        {
            return this.prodDescr;
        }

        /**
         * Sets the prod descr.
         *
         * @param _prodDescr the prod descr
         * @return the data bean
         */
        public DataBean setProdDescr(final String _prodDescr)
        {
            this.prodDescr = _prodDescr;
            return this;
        }
    }

    /**
     * The Class SubreportDesign.
     *
     * @author The eFaps Team
     */
    public static class SubreportDesign
        extends AbstractSimpleExpression<JasperReportBuilder>
    {

        /** The Constant serialVersionUID. */
        private static final long serialVersionUID = 1L;

        /** The ex type. */
        private final ExportType exType;

        /**
         * Instantiates a new subreport design.
         *
         * @param _exType the ex type
         */
        public SubreportDesign(final ExportType _exType)
        {
            this.exType = _exType;
        }

        @Override
        public JasperReportBuilder evaluate(final ReportParameters _reportParameters)
        {
            final TextColumnBuilder<String> docTypeColumn = DynamicReports.col.column("docType",
                            DynamicReports.type.stringType());

            final TextColumnBuilder<String> docNameColumn = DynamicReports.col.column("docName",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<BigDecimal> quantityColumn = DynamicReports.col.column("quantity",
                            DynamicReports.type.bigDecimalType());

            final JasperReportBuilder report = DynamicReports.report();

            if (ExportType.PDF.equals(this.exType)) {
                report.setColumnStyle(DynamicReports.stl.style().setPadding(DynamicReports.stl.padding(2))
                                .setLeftBorder(DynamicReports.stl.pen1Point())
                                .setRightBorder(DynamicReports.stl.pen1Point())
                                .setBottomBorder(DynamicReports.stl.pen1Point())
                                .setTopBorder(DynamicReports.stl.pen1Point()));
                docNameColumn.setWidth(17);
                quantityColumn.setWidth(49);

            } else if (ExportType.EXCEL.equals(this.exType)) {
                docNameColumn.setFixedWidth(60);
                quantityColumn.setFixedWidth(60);
            }
            report.columns(docTypeColumn, docNameColumn, quantityColumn)
                .subtotalsAtColumnFooter(DynamicReports.sbt.sum(quantityColumn));
            return report;
        }
    }

    /**
     * The Class SubreportDataSource.
     *
     * @author The eFaps Team
     */
    public static class SubreportDataSource
        extends AbstractSimpleExpression<JRDataSource>
    {

        /** The Constant serialVersionUID. */
        private static final long serialVersionUID = 1L;

        @Override
        public JRDataSource evaluate(final ReportParameters _reportParameters)
        {
            final Collection<PartialBean> value = _reportParameters.getValue("partials");
            return new JRBeanCollectionDataSource(value);
        }
    }

}
