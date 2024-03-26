/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
 */
package org.efaps.esjp.sales.report;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
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
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.properties.PropertiesUtil;
import org.efaps.esjp.common.uiform.Field_Base.DropDownPosition;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.group.ColumnGroupBuilder;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("8ef44f59-6aea-4f28-a4e7-eae15f71208c")
@EFapsApplication("eFapsApp-Sales")
public abstract class DocVsDocReport_Base
    extends FilteredReport
{
    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DocVsDocReport.class);

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

    /**
     * Gets the versus field value.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the versus field value
     * @throws EFapsException on error
     */
    public Return getVersusFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final IUIValue value = (IUIValue) _parameter.get(ParameterValues.UIOBJECT);
        final String fieldName = value.getField().getName();
        final Map<String, Object> map = getFilterMap(_parameter);
        String key = "";
        if (map.containsKey(fieldName)) {
            final Object obj = map.get(fieldName);
            if (obj instanceof VersusFilterValue) {
                key = ((VersusFilterValue) obj).getObject();
            }
        } else {
            map.put(fieldName, "");
        }
        final List<DropDownPosition> values = new ArrayList<>();
        final Map<Integer, String> versus = PropertiesUtil.analyseProperty(Sales.DOCVSDOCREPORT.get(), "Versus", 0);
        for (final String vs : versus.values()) {
            final DropDownPosition pos = new org.efaps.esjp.common.uiform.Field().getDropDownPosition(
                                _parameter, vs, Sales.DOCVSDOCREPORT.get().getProperty(vs + ".Label", vs));
            pos.setSelected(key.equals(vs));
            values.add(pos);
        }
        final Return ret = new Return();
        ret.put(ReturnValues.VALUES, values);
        return ret;
    }

    @Override
    protected Object getDefaultValue(final Parameter _parameter,
                                     final String _field,
                                     final String _type,
                                     final String _default)
        throws EFapsException
    {
        final Object ret;
        if ("Versus".equalsIgnoreCase(_type)) {
            final Map<Integer, String> versus = PropertiesUtil.analyseProperty(Sales.DOCVSDOCREPORT.get(), "Versus", 0);
            ret = new VersusFilterValue().setObject(versus.values().iterator().next());
        } else {
            ret = super.getDefaultValue(_parameter, _field, _type, _default);
        }
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
        return new DynDocVsDocReport(this);
    }

    /**
     * Filter that has a Instance as base.
     */
    public static class VersusFilterValue
        extends AbstractFilterValue<String>
    {
        /**
         * Needed for serialization.
         */
        private static final long serialVersionUID = 1L;

        @Override
        public String getLabel(final Parameter _parameter)
            throws EFapsException
        {
            return Sales.DOCVSDOCREPORT.get().getProperty(getObject() + ".Label", getObject());
        }

        @Override
        public AbstractFilterValue<String> parseObject(final String[] _values)
        {
            setObject(_values[0]);
            return this;
        }
    }

    /**
     * The Class DynSalesProductReport.
     */
    public static class DynDocVsDocReport
        extends AbstractDynamicReport
    {

        /** The filtered report. */
        private final DocVsDocReport_Base filteredReport;

        /**
         * Instantiates a new dyn doc vs doc report.
         *
         * @param _filteredReport the filtered report
         */
        public DynDocVsDocReport(final DocVsDocReport_Base _filteredReport)
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
                final List<DataBean> dataSource = new ArrayList<>();
                final List<DocBean> lefts = getDocBeans(_parameter, null);
                final List<DocBean> rights = getDocBeans(_parameter, lefts);

                final Map<Instance, DocComparator> map = new HashMap<>();
                // 1. group by Contacts
                for (final DocBean docBean : lefts) {
                    if (!map.containsKey(docBean.getContactInstance())) {
                        map.put(docBean.getContactInstance(), getComparator(_parameter));
                    }
                    map.get(docBean.getContactInstance()).getLeftDocuments().add(docBean);
                }
                for (final DocBean docBean : rights) {
                    if (!map.containsKey(docBean.getContactInstance())) {
                        map.put(docBean.getContactInstance(), getComparator(_parameter));
                    }
                    map.get(docBean.getContactInstance()).getRightDocuments().add(docBean);
                }

                for (final DocComparator comp : map.values()) {
                    dataSource.addAll(comp.getDataBeans(_parameter));
                }

                final ComparatorChain<DataBean> chain = new ComparatorChain<>();
                chain.addComparator((_o1, _o2) -> _o1.getContactName().compareTo(_o2.getContactName()));
                Collections.sort(dataSource, chain);
                ret = new JRBeanCollectionDataSource(dataSource);
                getFilteredReport().cache(_parameter, ret);
            }
            return ret;
        }

        /**
         * Gets the doc beans.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _previous the previous
         * @return the doc beans
         * @throws EFapsException on error
         */
        protected List<DocBean> getDocBeans(final Parameter _parameter,
                                            final List<DocBean> _previous)
            throws EFapsException
        {
            final List<DocBean> ret = new ArrayList<>();
            // only execute if no previous are given or when a not empty list is given
            if (_previous == null || _previous != null && !_previous.isEmpty()) {
                final QueryBuilder attrQueryBldr = getAttrQueryBuilder(_parameter, _previous != null);
                final Object[] contactInsts = getContactInsts(_parameter);
                if (contactInsts.length > 0) {
                    attrQueryBldr.addWhereAttrEqValue(CISales.DocumentAbstract.Contact, getContactInsts(_parameter));
                } else if (_previous != null && !_previous.isEmpty()) {
                    final Set<Instance> contactInstSet = new HashSet<>();
                    for (final DocBean docBean : _previous) {
                        contactInstSet.add(docBean.getContactInstance());
                    }
                }

                final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionAbstract);
                queryBldr.addWhereAttrInQuery(CISales.PositionAbstract.DocumentAbstractLink, attrQueryBldr
                                .getAttributeQuery(CISales.DocumentAbstract.ID));

                final MultiPrintQuery multi = queryBldr.getPrint();

                final SelectBuilder selDoc = SelectBuilder.get().linkto(CISales.PositionAbstract.DocumentAbstractLink);
                final SelectBuilder selDocInst = new SelectBuilder(selDoc).instance();
                final SelectBuilder selDocDate = new SelectBuilder(selDoc).attribute(CISales.DocumentAbstract.Date);
                final SelectBuilder selDocName = new SelectBuilder(selDoc).attribute(CISales.DocumentAbstract.Name);
                final SelectBuilder selContact = new SelectBuilder(selDoc).linkto(CISales.DocumentAbstract.Contact);
                final SelectBuilder selContactInst = new SelectBuilder(selContact).instance();
                final SelectBuilder selContactName = new SelectBuilder(selContact).attribute(CIContacts.Contact.Name);

                final SelectBuilder selProd = SelectBuilder.get().linkto(CISales.PositionAbstract.Product);
                final SelectBuilder selProdInst = new SelectBuilder(selProd).instance();
                final SelectBuilder selProdName = new SelectBuilder(selProd).attribute(CIProducts.ProductAbstract.Name);
                final SelectBuilder selProdDescr = new SelectBuilder(selProd).attribute(
                                CIProducts.ProductAbstract.Description);

                multi.addSelect(selDocInst, selDocDate, selDocName, selContactInst, selContactName,
                                selProdInst, selProdName, selProdDescr);
                multi.addAttribute(CISales.PositionAbstract.Quantity);
                multi.execute();
                while (multi.next()) {
                    final DocBean bean = new DocBean()
                            .setDocInst(multi.getSelect(selDocInst))
                            .setDocDate(multi.<DateTime>getSelect(selDocDate))
                            .setDocName(multi.<String>getSelect(selDocName))
                            .setContactInstance(multi.<Instance>getSelect(selContactInst))
                            .setContactName(multi.<String>getSelect(selContactName))
                            .setProductInstance(multi.<Instance>getSelect(selProdInst))
                            .setProductName(multi.<String>getSelect(selProdName))
                            .setProductDesc(multi.<String>getSelect(selProdDescr))
                            .setQuantity(multi.<BigDecimal>getAttribute(CISales.PositionAbstract.Quantity));
                    ret.add(bean);
                }
            }
            return ret;
        }

        /**
         * Gets the attr query builder.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _right the right
         * @return the attr query builder
         * @throws EFapsException on error
         */
        protected QueryBuilder getAttrQueryBuilder(final Parameter _parameter,
                                                   final boolean _right)
            throws EFapsException
        {
            final Properties versusProperties = getVsProperties(_parameter);
            final Properties properties = PropertiesUtil.getProperties4Prefix(versusProperties, _right ? "right"
                            : "left", true);
            final Map<Integer, String> types = PropertiesUtil.analyseProperty(properties, "Type", 0);
            QueryBuilder ret = null;
            for (final String typeStr : types.values()) {
                final Type type;
                if (isUUID(typeStr)) {
                    type = Type.get(UUID.fromString(typeStr));
                } else {
                    type = Type.get(typeStr);
                }
                if (ret == null) {
                    ret = new QueryBuilder(type);
                } else {
                    ret.addType(type);
                }
            }
            final List<Status> statusList = getStatusListFromProperties(_parameter, properties);
            if (!statusList.isEmpty()) {
                ret.addWhereAttrEqValue(CISales.DocumentAbstract.StatusAbstract, statusList.toArray());
            }
            return ret;
        }

        @SuppressWarnings("rawtypes")
        @Override
        protected void addColumnDefinition(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {

            final TextColumnBuilder<String> contactNameColumn = DynamicReports.col.column(this.filteredReport
                            .getDBProperty("column.ContactName"), "contactName", DynamicReports.type.stringType());

            final ColumnGroupBuilder contactGroup = DynamicReports.grp.group(contactNameColumn).groupByDataType();

            final TextColumnBuilder<String> productNameColumn = DynamicReports.col.column(this.filteredReport
                            .getDBProperty("column.ProductName"), "productName", DynamicReports.type.stringType());
            final TextColumnBuilder<String> productDescColumn = DynamicReports.col.column(this.filteredReport
                            .getDBProperty("column.ProductDesc"), "productDesc", DynamicReports.type.stringType());

            final TextColumnBuilder<BigDecimal> quantityColumn = DynamicReports.col.column(this.filteredReport
                            .getDBProperty("column.Quantity"), "quantity", DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<List> leftDocColumn = DynamicReports.col.column(this.filteredReport
                            .getDBProperty("column.LeftDocNames"), "leftDocNames", DynamicReports.type.listType());
            final TextColumnBuilder<List> leftQuantitiesColumn = DynamicReports.col.column(this.filteredReport
                            .getDBProperty("column.LeftQuantities"), "leftQuantities", DynamicReports.type.listType());
            final TextColumnBuilder<List> rightDocColumn = DynamicReports.col.column(this.filteredReport
                            .getDBProperty("column.RightDocNames"), "rightDocNames", DynamicReports.type.listType());
            final TextColumnBuilder<List> rightQuantitiesColumn = DynamicReports.col.column(this.filteredReport
                        .getDBProperty("column.RightQuantities"), "rightQuantities", DynamicReports.type.listType());

            _builder.addGroup(contactGroup).addColumn(contactNameColumn,
                            productNameColumn, productDescColumn, quantityColumn, leftDocColumn, leftQuantitiesColumn,
                            rightDocColumn, rightQuantitiesColumn);
        }

        /**
         * Gets the selected.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the selected
         * @throws EFapsException on error
         */
        protected Properties getVsProperties(final Parameter _parameter)
            throws EFapsException
        {
            final Map<String, Object> filterMap = this.filteredReport.getFilterMap(_parameter);
            final VersusFilterValue filter = (VersusFilterValue) filterMap.get("versus");
            return PropertiesUtil.getProperties4Prefix(Sales.DOCVSDOCREPORT.get(), filter.getObject(), true);
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
            if (filter == null || filter != null && filter.getObject() == null) {
                ret = new Object[] {};
            } else {
                ret = filter.getObject().toArray();
            }
            return ret;
        }

        /**
         * Getter method for the instance variable {@link #filteredReport}.
         *
         * @return value of instance variable {@link #filteredReport}
         */
        protected DocVsDocReport_Base getFilteredReport()
        {
            return this.filteredReport;
        }

        /**
         * Gets the comparator.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the comparator
         */
        protected DocComparator getComparator(final Parameter _parameter)
        {
            return new DocComparator(this);
        }
    }

    /**
     * The Class DocComparator.
     *
     */
    public static class DocComparator
        extends AbstractCommon
    {

        /** The left documents. */
        private final List<DocBean> leftDocuments = new ArrayList<>();

        /** The right documents. */
        private final List<DocBean> rightDocuments = new ArrayList<>();

        /** The dynamic report. */
        private final DynDocVsDocReport dynamicReport;

        /**
         * Instantiates a new doc comparator.
         *
         * @param _dynamicReport the dynamic report
         */
        public DocComparator(final DynDocVsDocReport _dynamicReport)
        {
            this.dynamicReport = _dynamicReport;
        }

        /**
         * Gets the left documents.
         *
         * @return the left documents
         */
        public List<DocBean> getLeftDocuments()
        {
            return this.leftDocuments;
        }

        /**
         * Gets the right documents.
         *
         * @return the right documents
         */
        public List<DocBean> getRightDocuments()
        {
            return this.rightDocuments;
        }

        /**
         * Gets the data beans.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the data beans
         * @throws EFapsException on error
         */
        public Collection<DataBean> getDataBeans(final Parameter _parameter)
            throws EFapsException
        {
            final List<DataBean> ret;
            switch (getCompareCriteria(_parameter)) {
                case "DateAfter":
                    ret = getDataBeans4DateAfter(_parameter);
                    break;
                case "DateEqualOrAfter":
                    ret = getDataBeans4DateEqualOrAfter(_parameter);
                    break;
                case "Relation":
                    ret = getDataBeans4Relation(_parameter);
                    break;
                default:
                    LOG.error("No Criteria found for selected");
                    ret = new ArrayList<>();
                    break;
            }
            return ret;
        }

        /**
         * Gets the data beans for date after.
         *
         * @param _parameter the parameter
         * @return the data beans for date after
         */
        protected List<DataBean> getDataBeans4DateAfter(final Parameter _parameter)
        {
            final List<DataBean> ret = new ArrayList<>();
            sort(_parameter, getLeftDocuments());
            sort(_parameter, getRightDocuments());

            for (final DocBean leftBean : getLeftDocuments()) {
                final DataBean dataBean = new DataBean()
                                .addLeft(leftBean)
                                .setContactName(leftBean.getContactName())
                                .setProductName(leftBean.getProductName())
                                .setProductDesc(leftBean.getProductDesc())
                                .setQuantity(leftBean.getQuantity());
                ret.add(dataBean);
                boolean found = false;
                final Iterator<DocBean> iter = getRightDocuments().iterator();
                while (iter.hasNext() && !found) {
                    final DocBean rightBean = iter.next();
                    if (rightBean.getDocDate().isAfter(leftBean.getDocDate())) {
                        if (rightBean.getProductInstance().equals(leftBean.getProductInstance())) {
                            BigDecimal currentQuantity = dataBean.getQuantity();
                            // if currentQuantity > rightQuanity remove the right bean
                            if (currentQuantity.compareTo(rightBean.getQuantity()) > 0) {
                                currentQuantity = currentQuantity.subtract(rightBean.getQuantity());
                                dataBean.getRightBeans().add(rightBean);
                                iter.remove();
                            } else if (currentQuantity.compareTo(rightBean.getQuantity()) == 0) {
                                currentQuantity = BigDecimal.ZERO;
                                dataBean.getRightBeans().add(rightBean);
                                iter.remove();
                                found = true;
                            } else {
                            // currentQuantity < rightQuanity mark as found but keep the rightbean
                                currentQuantity = BigDecimal.ZERO;
                                dataBean.getRightBeans().add(rightBean);
                                found = true;
                            }
                            dataBean.setQuantity(currentQuantity);
                        }
                    } else {
                        iter.remove();
                    }
                }
            }
            return ret;
        }

        /**
         * Gets the data beans for date equal or after.
         *
         * @param _parameter the parameter
         * @return the data beans for date equal or after
         */
        protected List<DataBean> getDataBeans4DateEqualOrAfter(final Parameter _parameter)
        {
            final List<DataBean> ret = new ArrayList<>();
            sort(_parameter, getLeftDocuments());
            sort(_parameter, getRightDocuments());

            for (final DocBean leftBean : getLeftDocuments()) {
                final DataBean dataBean = new DataBean()
                                .addLeft(leftBean)
                                .setContactName(leftBean.getContactName())
                                .setProductName(leftBean.getProductName())
                                .setProductDesc(leftBean.getProductDesc())
                                .setQuantity(leftBean.getQuantity());
                ret.add(dataBean);
                boolean found = false;
                final Iterator<DocBean> iter = getRightDocuments().iterator();
                while (iter.hasNext() && !found) {
                    final DocBean rightBean = iter.next();
                    if (rightBean.getDocDate().isAfter(leftBean.getDocDate())
                                    || rightBean.getDocDate().isEqual(leftBean.getDocDate())) {
                        if (rightBean.getProductInstance().equals(leftBean.getProductInstance())) {
                            BigDecimal currentQuantity = dataBean.getQuantity();
                            // if currentQuantity > rightQuanity remove the right bean
                            if (currentQuantity.compareTo(rightBean.getQuantity()) > 0) {
                                currentQuantity = currentQuantity.subtract(rightBean.getQuantity());
                                dataBean.getRightBeans().add(rightBean);
                                iter.remove();
                            } else if (currentQuantity.compareTo(rightBean.getQuantity()) == 0) {
                                currentQuantity = BigDecimal.ZERO;
                                dataBean.getRightBeans().add(rightBean);
                                iter.remove();
                                found = true;
                            } else {
                            // currentQuantity < rightQuanity mark as found but keep the rightbean
                                currentQuantity = BigDecimal.ZERO;
                                dataBean.getRightBeans().add(rightBean);
                                found = true;
                            }
                            dataBean.setQuantity(currentQuantity);
                        }
                    } else {
                        iter.remove();
                    }
                }
            }
            return ret;
        }

        /**
         * Gets the data beans for relation.
         *
         * @param _parameter the parameter
         * @return the data beans for relation
         * @throws EFapsException the e faps exception
         */
        protected List<DataBean> getDataBeans4Relation(final Parameter _parameter)
            throws EFapsException
        {
            final List<DataBean> ret = new ArrayList<>();
            final Properties vsProperties = getDynamicReport().getVsProperties(_parameter);
            final Properties properties = PropertiesUtil.getProperties4Prefix(vsProperties, "relation", true);
            for (final DocBean leftBean : getLeftDocuments()) {
                final DataBean dataBean = new DataBean()
                                .addLeft(leftBean)
                                .setContactName(leftBean.getContactName())
                                .setProductName(leftBean.getProductName())
                                .setProductDesc(leftBean.getProductDesc())
                                .setQuantity(leftBean.getQuantity());
                ret.add(dataBean);
                final Set<Instance> rightInsts = new HashSet<>();
                final QueryBuilder relQueryBldr = getQueryBldrFromProperties(_parameter, properties);
                relQueryBldr.addWhereAttrEqValue(properties.getProperty("LeftAttribute"), leftBean.getDocInst());
                final MultiPrintQuery multi = relQueryBldr.getCachedPrint4Request();
                final SelectBuilder selRightInst = SelectBuilder.get()
                                .linkto(properties.getProperty("RightAttribute")).instance();
                multi.addSelect(selRightInst);
                multi.execute();
                while (multi.next()) {
                    rightInsts.add(multi.getSelect(selRightInst));
                }
                final List<DocBean> rightDocs = getRightDocuments().stream()
                                .filter(bean -> rightInsts.contains(bean.getDocInst()))
                                .collect(Collectors.toList());
                boolean found = false;
                final Iterator<DocBean> iter = rightDocs.iterator();
                while (iter.hasNext() && !found) {
                    final DocBean rightBean = iter.next();
                    if (rightBean.getProductInstance().equals(leftBean.getProductInstance())) {
                        BigDecimal currentQuantity = dataBean.getQuantity();
                        // if currentQuantity > rightQuanity remove the right bean
                        if (currentQuantity.compareTo(rightBean.getQuantity()) > 0) {
                            currentQuantity = currentQuantity.subtract(rightBean.getQuantity());
                            dataBean.getRightBeans().add(rightBean);
                            getRightDocuments().remove(rightBean);
                        } else if (currentQuantity.compareTo(rightBean.getQuantity()) == 0) {
                            currentQuantity = BigDecimal.ZERO;
                            dataBean.getRightBeans().add(rightBean);
                            getRightDocuments().remove(rightBean);
                            found = true;
                        } else {
                            // currentQuantity < rightQuanity mark as found but keep the rightbean
                            currentQuantity = BigDecimal.ZERO;
                            dataBean.getRightBeans().add(rightBean);
                            found = true;
                        }
                        dataBean.setQuantity(currentQuantity);
                    }
                }
            }
            return ret.stream()
                        .filter(bean -> bean.getQuantity().compareTo(BigDecimal.ZERO) != 0)
                        .collect(Collectors.toList());
        }

        /**
         * Gets the compare criteria.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the compare criteria
         * @throws EFapsException on error
         */
        protected String getCompareCriteria(final Parameter _parameter)
            throws EFapsException
        {
            return getDynamicReport().getVsProperties(_parameter).getProperty("Criteria", "No Criteria");
        }

        /**
         * Sort.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _list the list
         */
        protected void sort(final Parameter _parameter,
                            final List<DocBean> _list)
        {
            final ComparatorChain<DocBean> chain = new ComparatorChain<>();
            chain.addComparator((_o1, _o2) -> _o1.getDocDate().compareTo(_o2.getDocDate()));
            chain.addComparator((_o1, _o2) -> _o1.getDocName().compareTo(_o2.getDocName()));
            chain.addComparator((_o1, _o2) -> _o1.getProductName().compareTo(_o2.getProductName()));
            Collections.sort(_list, chain);
        }

        /**
         * Gets the dynamic report.
         *
         * @return the dynamic report
         */
        public DynDocVsDocReport getDynamicReport()
        {
            return this.dynamicReport;
        }
    }

    /**
     * The Class DocBean.
     */
    public static class DocBean
    {


        /** The doc inst. */
        private Instance docInst;

        /** The doc name. */
        private String docName;

        /** The date. */
        private DateTime docDate;

        /** The contact instance. */
        private Instance contactInstance;

        /** The contact name. */
        private String contactName;

        /** The product instance. */
        private Instance productInstance;

        /** The product name. */
        private String productName;

        /** The product desc. */
        private String productDesc;

        /** The quantity. */
        private BigDecimal quantity;

        /**
         * Gets the doc inst.
         *
         * @return the doc inst
         */
        public Instance getDocInst()
        {
            return this.docInst;
        }

        /**
         * Sets the doc inst.
         *
         * @param _docInst the new doc inst
         * @return the doc bean
         */
        public DocBean setDocInst(final Instance _docInst)
        {
            this.docInst = _docInst;
            return this;
        }

        /**
         * Gets the product instance.
         *
         * @return the product instance
         */
        public Instance getProductInstance()
        {
            return this.productInstance;
        }

        /**
         * Sets the product instance.
         *
         * @param _productInstance the product instance
         * @return the doc bean
         */
        public DocBean setProductInstance(final Instance _productInstance)
        {
            this.productInstance = _productInstance;
            return this;
        }

        /**
         * Gets the product name.
         *
         * @return the product name
         */
        public String getProductName()
        {
            return this.productName;
        }

        /**
         * Sets the product name.
         *
         * @param _productName the product name
         * @return the doc bean
         */
        public DocBean setProductName(final String _productName)
        {
            this.productName = _productName;
            return this;
        }

        /**
         * Gets the product desc.
         *
         * @return the product desc
         */
        public String getProductDesc()
        {
            return this.productDesc;
        }

        /**
         * Sets the product desc.
         *
         * @param _productDesc the product desc
         * @return the doc bean
         */
        public DocBean setProductDesc(final String _productDesc)
        {
            this.productDesc = _productDesc;
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
         * @return the doc bean
         */
        public DocBean setQuantity(final BigDecimal _quantity)
        {
            this.quantity = _quantity;
            return this;
        }

        /**
         * Gets the contact instance.
         *
         * @return the contact instance
         */
        public Instance getContactInstance()
        {
            return this.contactInstance;
        }

        /**
         * Sets the contact instance.
         *
         * @param _contactInstance the contact instance
         * @return the doc bean
         */
        public DocBean setContactInstance(final Instance _contactInstance)
        {
            this.contactInstance = _contactInstance;
            return this;
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
         * @return the doc bean
         */
        public DocBean setContactName(final String _contactName)
        {
            this.contactName = _contactName;
            return this;
        }

        /**
         * Gets the date.
         *
         * @return the date
         */
        public DateTime getDocDate()
        {
            return this.docDate;
        }

        /**
         * Sets the date.
         *
         * @param _date the date
         * @return the doc bean
         */
        public DocBean setDocDate(final DateTime _date)
        {
            this.docDate = _date.withTimeAtStartOfDay();
            return this;
        }

        /**
         * Gets the doc name.
         *
         * @return the doc name
         */
        public String getDocName()
        {
            return this.docName;
        }

        /**
         * Sets the doc name.
         *
         * @param _docName the doc name
         * @return the doc bean
         */
        public DocBean setDocName(final String _docName)
        {
            this.docName = _docName;
            return this;
        }
    }

    /**
     * The Class DataBean.
     */
    public static class DataBean
    {

        /** The contact name. */
        private String contactName;

        /** The product name. */
        private String productName;

        /** The product desc. */
        private String productDesc;

        /** The quantity. */
        private BigDecimal quantity;

        /** The right beans. */
        private final List<DocBean> leftBeans = new ArrayList<>();


        /** The right beans. */
        private final List<DocBean> rightBeans = new ArrayList<>();

        /**
         * Gets the product desc.
         *
         * @return the product desc
         */
        public String getProductDesc()
        {
            return this.productDesc;
        }

        /**
         * Sets the product desc.
         *
         * @param _productDesc the product desc
         * @return the data bean
         */
        public DataBean setProductDesc(final String _productDesc)
        {
            this.productDesc = _productDesc;
            return this;
        }

        /**
         * Gets the product name.
         *
         * @return the product name
         */
        public String getProductName()
        {
            return this.productName;
        }

        /**
         * Sets the product name.
         *
         * @param _productName the product name
         * @return the data bean
         */
        public DataBean setProductName(final String _productName)
        {
            this.productName = _productName;
            return this;
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
         * @return the doc bean
         */
        public DataBean setContactName(final String _contactName)
        {
            this.contactName = _contactName;
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
         * @return the doc bean
         */
        public DataBean setQuantity(final BigDecimal _quantity)
        {
            this.quantity = _quantity;
            return this;
        }

        /**
         * Gets the right beans.
         *
         * @return the right beans
         */
        public List<DocBean> getRightBeans()
        {
            return this.rightBeans;
        }

        /**
         * Gets the right beans.
         *
         * @return the right beans
         */
        public List<DocBean> getLeftBeans()
        {
            return this.leftBeans;
        }

        /**
         * Adds the right.
         *
         * @param _right the right
         * @return the data bean
         */
        public DataBean addRight(final DocBean _right)
        {
            getLeftBeans().add(_right);
            return this;
        }

        /**
         * Adds the left.
         *
         * @param _left the left
         * @return the data bean
         */
        public DataBean addLeft(final DocBean _left)
        {
            getLeftBeans().add(_left);
            return this;
        }

        /**
         * Gets the left doc names.
         *
         * @return the left doc names
         */
        public List<String> getLeftDocNames()
        {
            final List<String> ret = new ArrayList<>();
            for (final DocBean bean : getLeftBeans()) {
                ret.add(bean.getDocName());
            }
            return ret;
        }

        /**
         * Gets the left quantities.
         *
         * @return the left quantities
         */
        public List<BigDecimal> getLeftQuantities()
        {
            final List<BigDecimal> ret = new ArrayList<>();
            for (final DocBean bean : getLeftBeans()) {
                ret.add(bean.getQuantity());
            }
            return ret;
        }

        /**
         * Gets the right doc names.
         *
         * @return the right doc names
         */
        public List<String> getRightDocNames()
        {
            final List<String> ret = new ArrayList<>();
            for (final DocBean bean : getRightBeans()) {
                ret.add(bean.getDocName());
            }
            return ret;
        }

        /**
         * Gets the right quantities.
         *
         * @return the right quantities
         */
        public List<BigDecimal> getRightQuantities()
        {
            final List<BigDecimal> ret = new ArrayList<>();
            for (final DocBean bean : getRightBeans()) {
                ret.add(bean.getQuantity());
            }
            return ret;
        }
    }

}
