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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.lang.BooleanUtils;
import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.CachedPrintQuery;
import org.efaps.db.Instance;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.erp.AbstractGroupedByDate;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.products.BOM;
import org.efaps.esjp.products.BOM_Base.ProductBOMBean;
import org.efaps.esjp.sales.report.DocPositionGroupedByDate_Base.ValueList;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabMeasureBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabRowGroupBuilder;
import net.sf.dynamicreports.report.constant.Calculation;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

/**
 *
 * @author The eFaps Team
 */
@EFapsUUID("4fad5c69-177a-4d35-85ad-984146bcf546")
@EFapsApplication("eFapsApp-Sales")
public abstract class DocPositionReport_Base
    extends FilteredReport
{
    /**
     * The Enum DateConfig.
     *
     * @author The eFaps Team
     */
    public enum ContactGroup
    {
        /** None. */
        NONE,
        /** Group by Document Contact*/
        DOCCONTACT,
        /** Group by Product Producer. */
        PRODPRODUCER,
        /** Group by Product Supplier. */
        PRODSUPPLIER;
    }

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DocPositionReport.class);

    /**
     * Values for the report.
     */
    private ValueList valueList;

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

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return the report class
     * @throws EFapsException on error
     */
    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new DynDocPositionReport(this);
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return list of DataBeans
     * @throws EFapsException on error
     */
    protected ValueList getData(final Parameter _parameter)
        throws EFapsException
    {
        if (this.valueList == null) {
            final DocPositionGroupedByDate ds = new DocPositionGroupedByDate()
            {
                @Override
                protected void add2QueryBuilder(final Parameter _parameter,
                                                final QueryBuilder _queryBldr)
                    throws EFapsException
                {
                    super.add2QueryBuilder(_parameter, _queryBldr);
                    DocPositionReport_Base.this.add2QueryBuilder(_parameter, _queryBldr);
                }
            };
            final Map<String, Object> filter = getFilterMap(_parameter);
            final DateTime start;
            final DateTime end;
            if (filter.containsKey("dateFrom")) {
                start = (DateTime) filter.get("dateFrom");
            } else {
                start = new DateTime();
            }
            if (filter.containsKey("dateTo")) {
                end = (DateTime) filter.get("dateTo");
            } else {
                end = new DateTime();
            }
            final List<Type> typeList;
            if (filter.containsKey("type")) {
                typeList = new ArrayList<>();
                final TypeFilterValue filters = (TypeFilterValue) filter.get("type");
                for (final Long typeid : filters.getObject()) {
                    typeList.add(Type.get(typeid));
                }
            } else {
                typeList = getTypeList(_parameter);
            }
            final Properties props = getProperties4TypeList(_parameter);
            AbstractGroupedByDate.DateGroup dateGroup;
            if (filter.containsKey("dateGroup") && filter.get("dateGroup") != null) {
                dateGroup = (AbstractGroupedByDate.DateGroup) ((EnumFilterValue) filter.get("dateGroup")).getObject();
            } else {
                dateGroup = DocumentSumGroupedByDate_Base.DateGroup.MONTH;
            }

            this.valueList = ds.getValueList(_parameter, start, end, dateGroup, props,
                            typeList.toArray(new Type[typeList.size()]));

            if (filter.containsKey("bom") && BooleanUtils.isTrue((Boolean) filter.get("bom"))) {
                final List<Map<String, Object>> tmpList = new ArrayList<>();
                for (final Map<String, Object> value  : this.valueList) {
                    final Instance prodInst = (Instance) value.get("productInst");
                    final BigDecimal quantity = (BigDecimal) value.get("quantity");
                    final UoM uom  = (UoM) value.get("uoM");
                    final BOM bom  = new BOM();
                    final List<ProductBOMBean> prodBeans = bom.getBOMProducts(_parameter, prodInst, quantity, uom);
                    if (prodBeans.isEmpty()) {
                        tmpList.add(value);
                    } else {
                        for (final ProductBOMBean bean : prodBeans) {
                            final Map<String, Object> newmap = new HashMap<>(value);
                            newmap.put("quantity", bean.getQuantity());
                            newmap.put("uoM", bean.getUoM());
                            newmap.put("product", bean.getName() + " - " + bean.getDescription()
                                        + " [" + bean.getUoM().getName() + "]");
                            newmap.put("productInst", bean.getInstance());
                            tmpList.add(newmap);
                        }
                    }
                }
                this.valueList.clear();
                this.valueList.addAll(tmpList);
            }
            final ContactGroup contactGroup = evaluateContactGroup(_parameter);
            if (ContactGroup.PRODPRODUCER.equals(contactGroup)
                            || ContactGroup.PRODPRODUCER.equals(contactGroup)) {
                for (final Map<String, Object> value  : this.valueList) {
                    final Instance prodInst = (Instance) value.get("productInst");
                    final CachedPrintQuery print = CachedPrintQuery.get4Request(prodInst);
                    final SelectBuilder selContactName = ContactGroup.PRODPRODUCER.equals(contactGroup)
                                    ? SelectBuilder.get().linkfrom(CIProducts.Product2Producer.From).linkto(
                                            CIProducts.Product2Producer.To).attribute(CIContacts.ContactAbstract.Name)
                                    : SelectBuilder.get().linkfrom(CIProducts.Product2Supplier.From).linkto(
                                            CIProducts.Product2Supplier.To).attribute(CIContacts.ContactAbstract.Name);
                    print.addSelect(selContactName);
                    print.executeWithoutAccessCheck();
                    value.put("contact", print.getSelect(selContactName));
                }
            }

        }
        return this.valueList;
    }

    @Override
    protected Properties getProperties4TypeList(final Parameter _parameter)
        throws EFapsException
    {
        Properties ret = null;
        if (containsProperty(_parameter, "SystemConfig")) {
            final String sysConfstr = getProperty(_parameter, "SystemConfig");
            final SystemConfiguration config;
            if (isUUID(sysConfstr)) {
                config = SystemConfiguration.get(UUID.fromString(sysConfstr));
            } else {
                config = SystemConfiguration.get(sysConfstr);
            }
            if (config != null) {
                ret = config.getAttributeValueAsProperties(getProperty(_parameter, "ConfigAttribute"), true);
            }
        }
        if (ret == null) {
            ret = Sales.DOCPOSREPORT.get();
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _queryBldr queryBuilder to add to
     * @throws EFapsException on error
     */
    protected void add2QueryBuilder(final Parameter _parameter,
                                    final QueryBuilder _queryBldr)
        throws EFapsException
    {
        final Map<String, Object> filter = getFilterMap(_parameter);

        if (filter.containsKey("contact")) {
            final InstanceFilterValue filterValue = (InstanceFilterValue) filter.get("contact");
            if (filterValue != null && filterValue.getObject().isValid()) {
                final ContactGroup contactGroup = evaluateContactGroup(_parameter);
                switch (contactGroup) {
                    case PRODPRODUCER:
                        final QueryBuilder p2pAttrQueryBldr = new QueryBuilder(CIProducts.Product2Producer);
                        p2pAttrQueryBldr.addWhereAttrEqValue(CIProducts.Product2Producer.To, filterValue.getObject());

                        _queryBldr.addWhereAttrInQuery(CISales.PositionAbstract.Product,
                                        p2pAttrQueryBldr.getAttributeQuery(CIProducts.Product2Producer.From));
                        break;
                    case PRODSUPPLIER:
                        final QueryBuilder p2sAttrQueryBldr = new QueryBuilder(CIProducts.Product2Supplier);
                        p2sAttrQueryBldr.addWhereAttrEqValue(CIProducts.Product2Supplier.To, filterValue.getObject());

                        _queryBldr.addWhereAttrInQuery(CISales.PositionAbstract.Product,
                                        p2sAttrQueryBldr.getAttributeQuery(CIProducts.Product2Supplier.From));
                        break;
                    default:
                        final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.DocumentAbstract);
                        attrQueryBldr.addWhereAttrEqValue(CISales.DocumentAbstract.Contact, filterValue.getObject());

                        _queryBldr.addWhereAttrInQuery(CISales.PositionAbstract.DocumentAbstractLink,
                                        attrQueryBldr.getAttributeQuery(CISales.DocumentAbstract.ID));
                        break;
                }
            }
        }
    }

    /**
     * Evaluate contact group.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the date config
     * @throws EFapsException on error
     */
    protected ContactGroup evaluateContactGroup(final Parameter _parameter)
        throws EFapsException
    {
        ContactGroup ret;
        final Map<String, Object> filter = getFilterMap(_parameter);
        final EnumFilterValue value = (EnumFilterValue) filter.get("contactGroup");
        if (value != null) {
            ret = (ContactGroup) value.getObject();
        } else {
            ret = ContactGroup.NONE;
        }
        return ret;
    }


    /**
     * Dynamic Report.
     */
    public static class DynDocPositionReport
        extends AbstractDynamicReport
    {

        /**
         * Filtered Report.
         */
        private final DocPositionReport_Base filteredReport;

        /**
         * @param _filteredReport report
         */
        public DynDocPositionReport(final DocPositionReport_Base _filteredReport)
        {
            this.filteredReport = _filteredReport;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final ValueList values = getDocPositionReport().getData(_parameter);
            final ComparatorChain<Map<String, Object>> chain = new ComparatorChain<>();
            chain.addComparator(new Comparator<Map<String, Object>>()
            {

                @Override
                public int compare(final Map<String, Object> _o1,
                                   final Map<String, Object> _o2)
                {
                    return 0;
                }
            });
            Collections.sort(values, chain);
            return new JRMapCollectionDataSource((Collection) values);
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final boolean showAmount = false;
            final CrosstabBuilder crosstab = DynamicReports.ctab.crosstab();

            final Map<String, Object> filterMap = getDocPositionReport().getFilterMap(_parameter);
            CurrencyInst selected = null;
            if (filterMap.containsKey("currency")) {
                final CurrencyFilterValue filter = (CurrencyFilterValue) filterMap.get("currency");
                if (filter.getObject() instanceof Instance && filter.getObject().isValid()) {
                    selected = CurrencyInst.get(filter.getObject());
                }
            }
            final ContactGroup contactGrp = this.filteredReport.evaluateContactGroup(_parameter);
            if (!ContactGroup.NONE.equals(contactGrp)) {
                final CrosstabRowGroupBuilder<String> contactGroup = DynamicReports.ctab.rowGroup("contact",
                                String.class).setHeaderWidth(150);
                crosstab.addRowGroup(contactGroup);
            }
            if (filterMap.containsKey("typeGroup")) {
                final Boolean contactBool = (Boolean) filterMap.get("typeGroup");
                if (contactBool != null && contactBool) {
                    final CrosstabRowGroupBuilder<String> typeGroup = DynamicReports.ctab.rowGroup("type",
                                    String.class).setHeaderWidth(150);
                    crosstab.addRowGroup(typeGroup);
                }
            }

            if (showAmount) {
                if (selected == null) {
                    for (final CurrencyInst currency : CurrencyInst.getAvailable()) {
                        final CrosstabMeasureBuilder<BigDecimal> amountMeasure = DynamicReports.ctab.measure(
                                        currency.getSymbol(),
                                        currency.getISOCode(), BigDecimal.class, Calculation.SUM);
                        crosstab.addMeasure(amountMeasure);
                    }
                    final CrosstabMeasureBuilder<BigDecimal> amountMeasure = DynamicReports.ctab.measure(
                                    DBProperties.getProperty(DocPositionReport.class.getName() + ".BASE")
                                                    + " " + CurrencyInst.get(Currency.getBaseCurrency()).getSymbol(),
                                    "BASE", BigDecimal.class, Calculation.SUM);
                    crosstab.addMeasure(amountMeasure);
                } else {
                    final CrosstabMeasureBuilder<BigDecimal> amountMeasure = DynamicReports.ctab.measure(
                                    selected.getSymbol(),
                                    selected.getISOCode(), BigDecimal.class, Calculation.SUM);
                    crosstab.addMeasure(amountMeasure);
                }
            } else {
                final CrosstabMeasureBuilder<BigDecimal> quantityMeasure = DynamicReports.ctab.measure(
                                DBProperties.getProperty(DocPositionReport.class.getName() + ".quantity"),
                                "quantity", BigDecimal.class, Calculation.SUM);
                crosstab.addMeasure(quantityMeasure);
            }
            final CrosstabRowGroupBuilder<String> rowTypeGroup = DynamicReports.ctab.rowGroup("product", String.class)
                            .setHeaderWidth(250);
            crosstab.addRowGroup(rowTypeGroup);

            final CrosstabColumnGroupBuilder<String> columnGroup = DynamicReports.ctab.columnGroup("partial",
                            String.class);

            crosstab.addColumnGroup(columnGroup);
            crosstab.setCellWidth(200);
            _builder.addSummary(crosstab);
        }

        /**
         * Getter method for the instance variable {@link #filteredReport}.
         *
         * @return value of instance variable {@link #filteredReport}
         */
        public DocPositionReport_Base getDocPositionReport()
        {
            return this.filteredReport;
        }
    }
}
