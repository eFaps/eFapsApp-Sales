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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.lang3.BooleanUtils;
import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.CachedPrintQuery;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.properties.PropertiesUtil;
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
import net.sf.dynamicreports.report.base.expression.AbstractSimpleExpression;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabMeasureBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabRowGroupBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabVariableBuilder;
import net.sf.dynamicreports.report.constant.Calculation;
import net.sf.dynamicreports.report.constant.HorizontalTextAlignment;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

/**
 * The Class DocPositionReport_Base.
 *
 * @author The eFaps Team
 */
@EFapsUUID("4fad5c69-177a-4d35-85ad-984146bcf546")
@EFapsApplication("eFapsApp-Sales")
public abstract class DocPositionReport_Base
    extends FilteredReport
{

    private static final Logger LOG = LoggerFactory.getLogger(DocPositionReport.class);

    /**
     * The Enum DateConfig.
     *
     * @author The eFaps Team
     */
    public enum ContactGroup
    {
        /** None. */
        NONE,
        /** Group by Document Contact. */
        DOCCONTACT,
        /** Group by Product Producer. */
        PRODPRODUCER,
        /** Group by Product Supplier. */
        PRODSUPPLIER;
    }

    /**
     * Values for the report.
     */
    private ValueList valueList;

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
        final String html = dyRp.getHtmlSnipplet(_parameter);
        ret.put(ReturnValues.SNIPLETT, html);
        return ret;
    }

    /**
     * Export report.
     *
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
     * Gets the report.
     *
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
     * Gets the data.
     *
     * @param _parameter Parameter as passed by the eFasp API
     * @return list of DataBeans
     * @throws EFapsException on error
     */
    protected ValueList getData(final Parameter _parameter)
        throws EFapsException
    {
        if (valueList == null) {
            final Map<String, Object> filter = getFilterMap(_parameter);

            final DocPositionGroupedByDate ds = new DocPositionGroupedByDate()
            {

                final SelectBuilder selPOSDetails = SelectBuilder.get()
                                .linkto(CISales.PositionAbstract.DocumentAbstractLink)
                                .linkfrom("POS_Balance2Document", "ToLink")
                                .linkto("FromLink")
                                .linkto("UserLink")
                                .attribute("Name");

                @Override
                protected void add2QueryBuilder(final Parameter _parameter,
                                                final QueryBuilder _queryBldr)
                    throws EFapsException
                {
                    super.add2QueryBuilder(_parameter, _queryBldr);
                    DocPositionReport_Base.this.add2QueryBuilder(_parameter, _queryBldr);
                }

                @Override
                protected void add2Print(final Parameter _parameter,
                                         final MultiPrintQuery _multi)
                    throws EFapsException
                {
                    super.add2Print(_parameter, _multi);
                    if (BooleanUtils.isTrue((Boolean) filter.get("posDetails"))) {
                        _multi.addSelect(selPOSDetails);
                    }
                }

                @Override
                protected void add2RowMap(final Parameter _parameter,
                                          final MultiPrintQuery _multi,
                                          final Map<String, Object> _map)
                    throws EFapsException
                {
                    super.add2RowMap(_parameter, _multi, _map);
                    _map.put("posDetails", _multi.getSelect(selPOSDetails));
                }
            };

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
            final Properties props = getProperties4TypeList(_parameter, null);
            final AbstractGroupedByDate.DateGroup dateGroup;
            if (filter.containsKey("dateGroup") && filter.get("dateGroup") != null) {
                dateGroup = (AbstractGroupedByDate.DateGroup) ((EnumFilterValue) filter.get("dateGroup")).getObject();
            } else {
                dateGroup = DocumentSumGroupedByDate_Base.DateGroup.MONTH;
            }

            valueList = ds.getValueList(_parameter, start, end, dateGroup, props,
                            typeList.toArray(new Type[typeList.size()]));

            if (BooleanUtils.isTrue((Boolean) filter.get("bom"))) {
                int counter = 0; // just a variable to prevent eternal loops
                boolean finisched = false;
                while (!finisched && counter < 5) {
                    finisched = true;
                    final List<Map<String, Object>> tmpList = new ArrayList<>();
                    for (final Map<String, Object> value : valueList) {
                        final Instance prodInst = (Instance) value.get("productInst");
                        final BigDecimal quantity = (BigDecimal) value.get("quantity");
                        final UoM uom = (UoM) value.get("uoM");
                        final BOM bom = new BOM();
                        final List<ProductBOMBean> prodBeans = bom.getBOMProducts(_parameter, prodInst, quantity, uom);
                        if (prodBeans.isEmpty()) {
                            tmpList.add(value);
                        } else {
                            finisched = false;
                            for (final ProductBOMBean bean : prodBeans) {
                                final Map<String, Object> newmap = new HashMap<>(value);
                                newmap.put("uoMStr", bean.getUoM().getName());
                                newmap.put("productName", bean.getName());
                                newmap.put("productDescr", bean.getDescription());
                                newmap.put("quantity", bean.getQuantity());
                                newmap.put("uoM", bean.getUoM());
                                newmap.put("product", bean.getName() + " - " + bean.getDescription()
                                                + " [" + bean.getUoM().getName() + "]");
                                newmap.put("productInst", bean.getInstance());
                                tmpList.add(newmap);
                            }
                        }
                    }
                    counter++;
                    valueList.clear();
                    valueList.addAll(tmpList);
                }
            }
            final ContactGroup contactGroup = evaluateContactGroup(_parameter);
            if (ContactGroup.PRODPRODUCER.equals(contactGroup)
                            || ContactGroup.PRODSUPPLIER.equals(contactGroup)) {
                for (final Map<String, Object> value : valueList) {
                    final Instance prodInst = (Instance) value.get("productInst");
                    final CachedPrintQuery print = CachedPrintQuery.get4Request(prodInst);
                    final SelectBuilder selContactName = ContactGroup.PRODPRODUCER.equals(contactGroup)
                                    ? SelectBuilder.get().linkfrom(CIProducts.Product2Producer.From).linkto(
                                                    CIProducts.Product2Producer.To)
                                                    .attribute(CIContacts.ContactAbstract.Name)
                                    : SelectBuilder.get().linkfrom(CIProducts.Product2Supplier.From).linkto(
                                                    CIProducts.Product2Supplier.To)
                                                    .attribute(CIContacts.ContactAbstract.Name);
                    print.addSelect(selContactName);
                    print.executeWithoutAccessCheck();
                    value.put("contact", print.getSelect(selContactName));
                }
            }
        }
        return valueList;
    }

    @Override
    protected Properties getProperties4TypeList(final Parameter _parameter,
                                                final String _fieldName)
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
        if (MapUtils.isEmpty(ret)) {
            ret = Sales.REPORT_DOCPOS.get();
        }
        if ("productType".equals(_fieldName)) {
            ret = PropertiesUtil.getProperties4Prefix(ret, "productType", true);
        }
        return ret;
    }

    /**
     * Adds the 2 query builder.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _queryBldr queryBuilder to add to
     * @throws EFapsException on error
     */
    protected void add2QueryBuilder(final Parameter _parameter,
                                    final QueryBuilder _queryBldr)
        throws EFapsException
    {
        final Map<String, Object> filterMap = getFilterMap(_parameter);
        // if sums are shown and only one currency should be shown filter the
        // positions by it
        if (_queryBldr.getType().isCIType(CISales.PositionSumAbstract) && filterMap.containsKey("currency")) {
            final CurrencyFilterValue filter = (CurrencyFilterValue) filterMap.get("currency");
            if (filter.getObject() instanceof Instance && filter.getObject().isValid()) {
                _queryBldr.addWhereAttrEqValue(CISales.PositionSumAbstract.RateCurrencyId, filter.getObject());
            }
        }

        if (filterMap.containsKey("contact")) {
            final InstanceFilterValue filterValue = (InstanceFilterValue) filterMap.get("contact");
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

        if (filterMap.containsKey("productType")) {
            final TypeFilterValue filterValue = (TypeFilterValue) filterMap.get("productType");
            if (filterValue != null && CollectionUtils.isNotEmpty(filterValue.getObject())) {
                final Iterator<Long> iter = filterValue.getObject().iterator();
                final QueryBuilder queryBldr = new QueryBuilder(Type.get(iter.next()));
                while (iter.hasNext()) {
                    queryBldr.addType(Type.get(iter.next()));
                }
                _queryBldr.addWhereAttrInQuery(CISales.PositionAbstract.Product,
                                queryBldr.getAttributeQuery(CIProducts.ProductAbstract.ID));
            }
        }

        if (filterMap.containsKey("product")) {
            final InstanceSetFilterValue filterValue = (InstanceSetFilterValue) filterMap.get("product");
            if (filterValue != null && CollectionUtils.isNotEmpty(filterValue.getObject())) {
                _queryBldr.addWhereAttrEqValue(CISales.PositionAbstract.Product, filterValue.getObject().toArray());
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
        final ContactGroup ret;
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
     *
     * @author The eFaps Team
     */
    public static class DynDocPositionReport
        extends AbstractDynamicReport
    {

        /**
         * Filtered Report.
         */
        private final DocPositionReport_Base filteredReport;

        /**
         * Instantiates a new dyn doc position report.
         *
         * @param _filteredReport report
         */
        public DynDocPositionReport(final DocPositionReport_Base _filteredReport)
        {
            filteredReport = _filteredReport;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
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
                    LOG.error("Catched JRException", e);
                }
            } else {
                final ValueList values = getFilteredReport().getData(_parameter);
                final ComparatorChain<Map<String, Object>> chain = new ComparatorChain<>();

                final ContactGroup contactGrp = filteredReport.evaluateContactGroup(_parameter);
                if (!ContactGroup.NONE.equals(contactGrp)) {
                    chain.addComparator((_o1,
                                         _o2) -> String.valueOf(_o1.get("contact"))
                                                         .compareTo(String.valueOf(_o2.get("contact"))));
                }
                final Map<String, Object> filterMap = getFilteredReport().getFilterMap(_parameter);
                if (BooleanUtils.isTrue((Boolean) filterMap.get("typeGroup"))) {
                    chain.addComparator((_o1,
                                         _o2) -> String.valueOf(_o1.get("type"))
                                                         .compareTo(String.valueOf(_o2.get("type"))));
                }
                if (BooleanUtils.isTrue((Boolean) filterMap.get("docDetails"))) {
                    chain.addComparator((_o1,
                                         _o2) -> String.valueOf(_o1.get("docName"))
                                                         .compareTo(String.valueOf(_o2.get("docName"))));
                }
                if (BooleanUtils.isTrue((Boolean) filterMap.get("posDetails"))) {
                    chain.addComparator((_o1,
                                         _o2) -> String.valueOf(_o1.get("posDetails"))
                                                         .compareTo(String.valueOf(_o2.get("posDetails"))));
                }
                chain.addComparator((_o1,
                                     _o2) -> String.valueOf(_o1.get("productName"))
                                                     .compareTo(String.valueOf(_o2.get("productName"))));
                chain.addComparator((_o1,
                                     _o2) -> String.valueOf(_o1.get("partial"))
                                                     .compareTo(String.valueOf(_o2.get("partial"))));

                Collections.sort(values, chain);

                // it must be ensured that the first product has information for
                // all used partial so it is garantized that the columns are in
                // order
                final Set<String> partials = new TreeSet<>();
                for (final Map<String, Object> value : values) {
                    partials.add((String) value.get("partial"));
                }
                if (partials.size() > 1) {
                    String productName = (String) values.get(0).get("productName");
                    final Iterator<Map<String, Object>> iter = values.iterator();
                    while (iter.hasNext() && productName.equals(values.get(0).get("productName"))) {
                        final Map<String, Object> currentMap = iter.next();
                        productName = (String) currentMap.get("productName");
                        partials.remove(currentMap.get("partial"));
                    }
                    for (final String partial : partials) {
                        final Map<String, Object> newMap = new HashMap<>();
                        for (final Entry<String, Object> entry : values.get(0).entrySet()) {
                            if (entry.getValue() instanceof Number) {
                                newMap.put(entry.getKey(), null);
                            } else {
                                newMap.put(entry.getKey(), entry.getValue());
                            }
                        }
                        newMap.put("partial", partial);
                        values.add(newMap);
                    }
                    Collections.sort(values, chain);
                }

                final Collection finalValues = new ArrayList<>();
                finalValues.addAll(values);
                ret = new JRMapCollectionDataSource(finalValues);
                getFilteredReport().cache(_parameter, ret);
            }
            return ret;
        }

        @Override
        protected void addColumnDefinition(final Parameter _parameter,
                                           final JasperReportBuilder _builder)
            throws EFapsException
        {
            final Map<String, Object> filterMap = getFilteredReport().getFilterMap(_parameter);
            final List<Type> typeList;
            if (filterMap.containsKey("type")) {
                typeList = new ArrayList<>();
                final TypeFilterValue filters = (TypeFilterValue) filterMap.get("type");
                for (final Long typeid : filters.getObject()) {
                    typeList.add(Type.get(typeid));
                }
            } else {
                typeList = getFilteredReport().getTypeList(_parameter);
            }
            boolean showAmount = true;
            for (final Type type : typeList) {
                if (!type.isKindOf(CISales.DocumentSumAbstract)) {
                    showAmount = false;
                }
            }

            final List<CrosstabRowGroupBuilder<?>> rowGrpBldrs = new ArrayList<>();
            final List<CrosstabColumnGroupBuilder<?>> colGrpBldrs = new ArrayList<>();
            final List<CrosstabMeasureBuilder<?>> measureGrpBldrs = new ArrayList<>();
            final List<CrosstabVariableBuilder<?>> variableGrpBldrs = new ArrayList<>();

            boolean base = false;
            CurrencyInst selected = null;
            if (filterMap.containsKey("currency")) {
                final CurrencyFilterValue filter = (CurrencyFilterValue) filterMap.get("currency");
                if (filter.getObject() instanceof Instance) {
                    if (filter.getObject().isValid()) {
                        selected = CurrencyInst.get(filter.getObject());
                    } else {
                        base = "BASE".equals(filter.getObject().getKey());
                    }
                }
            }
            final ContactGroup contactGrp = filteredReport.evaluateContactGroup(_parameter);
            if (!ContactGroup.NONE.equals(contactGrp)) {
                final CrosstabRowGroupBuilder<String> contactGroup = DynamicReports.ctab.rowGroup("contact",
                                String.class).setHeaderWidth(150);
                rowGrpBldrs.add(contactGroup);
            }
            if (filterMap.containsKey("typeGroup")) {
                final Boolean contactBool = (Boolean) filterMap.get("typeGroup");
                if (contactBool != null && contactBool) {
                    final CrosstabRowGroupBuilder<String> typeGroup = DynamicReports.ctab.rowGroup("type",
                                    String.class).setHeaderWidth(150);
                    rowGrpBldrs.add(typeGroup);
                }
            }
            if (BooleanUtils.isTrue((Boolean) filterMap.get("docDetails"))) {
                final CrosstabRowGroupBuilder<String> docGroup = DynamicReports.ctab.rowGroup("docName",
                                String.class);
                rowGrpBldrs.add(docGroup);
            }

            if (BooleanUtils.isTrue((Boolean) filterMap.get("posDetails"))) {
                final CrosstabRowGroupBuilder<String> posGroup = DynamicReports.ctab.rowGroup("posDetails",
                                String.class);
                rowGrpBldrs.add(posGroup);
            }

            final CrosstabMeasureBuilder<BigDecimal> quantityMeasure = DynamicReports.ctab.measure(
                            filteredReport.getDBProperty("quantity"),
                            "quantity", BigDecimal.class, Calculation.SUM);
            measureGrpBldrs.add(quantityMeasure);

            if (showAmount) {
                if (selected == null) {
                    if (!base) {
                        for (final CurrencyInst currency : CurrencyInst.getAvailable()) {
                            final CrosstabMeasureBuilder<BigDecimal> amountMeasure = DynamicReports.ctab.measure(
                                            currency.getSymbol(),
                                            currency.getISOCode(), BigDecimal.class, Calculation.SUM);
                            measureGrpBldrs.add(amountMeasure);
                            addUnitPrice(_parameter, currency.getSymbol()
                                            + " " + filteredReport.getDBProperty("unitPriceLabel"),
                                            currency.getISOCode(),
                                            variableGrpBldrs, measureGrpBldrs);
                        }
                    }
                    final String title = filteredReport.getDBProperty("BASE") + " "
                                    + CurrencyInst.get(Currency.getBaseCurrency()).getSymbol();
                    final CrosstabMeasureBuilder<BigDecimal> amountMeasure = DynamicReports.ctab.measure(title,
                                    "BASE", BigDecimal.class, Calculation.SUM);
                    measureGrpBldrs.add(amountMeasure);
                    addUnitPrice(_parameter, title + " " + filteredReport.getDBProperty("unitPriceLabel"),
                                    "BASE", variableGrpBldrs, measureGrpBldrs);
                } else {
                    final CrosstabMeasureBuilder<BigDecimal> amountMeasure = DynamicReports.ctab.measure(
                                    selected.getSymbol(),
                                    selected.getISOCode(), BigDecimal.class, Calculation.SUM);
                    measureGrpBldrs.add(amountMeasure);
                    addUnitPrice(_parameter, selected.getSymbol() + " "
                                    + filteredReport.getDBProperty("unitPriceLabel"),
                                    selected.getISOCode(), variableGrpBldrs, measureGrpBldrs);
                }
            }
            final CrosstabRowGroupBuilder<String> rowColGroup = DynamicReports.ctab
                            .rowGroup("productName", String.class)
                            .setShowTotal(true)
                            .setHeaderWidth(150);
            rowGrpBldrs.add(rowColGroup);

            final CrosstabRowGroupBuilder<String> rowTypeGroup = DynamicReports.ctab
                            .rowGroup("productDescr", String.class)
                            .setHeaderWidth(250)
                            .setHeaderHorizontalTextAlignment(HorizontalTextAlignment.LEFT)
                            .setShowTotal(false);
            rowGrpBldrs.add(rowTypeGroup);

            final CrosstabRowGroupBuilder<String> rowTypeGroup2 = DynamicReports.ctab.rowGroup("uoMStr", String.class)
                            .setHeaderWidth(50)
                            .setShowTotal(false);
            rowGrpBldrs.add(rowTypeGroup2);

            final CrosstabColumnGroupBuilder<String> columnGroup = DynamicReports.ctab.columnGroup("partial",
                            String.class).setShowTotal(true);
            colGrpBldrs.add(columnGroup);

            final CrosstabBuilder crosstab = DynamicReports.ctab.crosstab();
            crosstab.setCellWidth(200).setDataPreSorted(true);

            updateBldrs(rowGrpBldrs, colGrpBldrs, measureGrpBldrs);

            for (final CrosstabRowGroupBuilder<?> rowGrpBldr : rowGrpBldrs) {
                crosstab.addRowGroup(rowGrpBldr);
            }
            for (final CrosstabColumnGroupBuilder<?> colGrpBldr : colGrpBldrs) {
                crosstab.addColumnGroup(colGrpBldr);
            }
            for (final CrosstabMeasureBuilder<?> measureGrpBldr : measureGrpBldrs) {
                crosstab.addMeasure(measureGrpBldr);
            }
            for (final CrosstabVariableBuilder<?> variableGrpBldr : variableGrpBldrs) {
                crosstab.variables(variableGrpBldr);
            }
            _builder.addSummary(crosstab);
        }

        /**
         * Adds the unit price.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _title the title
         * @param _key the key
         * @param _variableGrpBldrs the variable grp bldrs
         * @param _measureGrpBldrs the measure grp bldrs
         * @throws EFapsException on error
         */
        protected void addUnitPrice(final Parameter _parameter,
                                    final String _title,
                                    final String _key,
                                    final List<CrosstabVariableBuilder<?>> _variableGrpBldrs,
                                    final List<CrosstabMeasureBuilder<?>> _measureGrpBldrs)
            throws EFapsException
        {
            final Map<String, Object> filterMap = getFilteredReport().getFilterMap(_parameter);
            if (filterMap.containsKey("unitPrice") && BooleanUtils.isTrue((Boolean) filterMap.get("unitPrice"))) {
                final CrosstabVariableBuilder<BigDecimal> unitPriceVariable = DynamicReports.ctab.variable(
                                new UnitPriceExpression(_key), Calculation.NOTHING);
                _variableGrpBldrs.add(unitPriceVariable);

                _measureGrpBldrs.add(DynamicReports.ctab.measure(_title, new UnitPriceMessuredExpression(
                                unitPriceVariable)).setDataType(DynamicReports.type.bigDecimalType()));
            }
        }

        /**
         * Update builders.
         *
         * @param _rowGrpBldrs the row group builders
         * @param _colGrpBldrs the column group builders
         * @param _measureGrpBldrs the measure group builders
         */
        protected void updateBldrs(final List<CrosstabRowGroupBuilder<?>> _rowGrpBldrs,
                                   final List<CrosstabColumnGroupBuilder<?>> _colGrpBldrs,
                                   final List<CrosstabMeasureBuilder<?>> _measureGrpBldrs)
        {
            // to be used by implementation
        }

        /**
         * Getter method for the instance variable {@link #filteredReport}.
         *
         * @return value of instance variable {@link #filteredReport}
         */
        public DocPositionReport_Base getFilteredReport()
        {
            return filteredReport;
        }
    }

    /**
     * The Class UnitPriceMeasureExpression.
     *
     * @author The eFaps Team
     */
    public static class UnitPriceExpression
        extends AbstractSimpleExpression<BigDecimal>
    {

        /** The Constant serialVersionUID. */
        private static final long serialVersionUID = 1L;

        /** The amount field. */
        private final String amountField;

        /**
         * Instantiates a new unit price measure expression.
         *
         * @param _amountField the amount field
         */
        public UnitPriceExpression(final String _amountField)
        {
            amountField = _amountField;
        }

        @Override
        public BigDecimal evaluate(final ReportParameters _reportParameters)
        {
            final BigDecimal quantity = _reportParameters.getValue("quantity");
            final BigDecimal amount = _reportParameters.getValue(amountField);
            LOG.info("Evaluating unitprice for field: {} with quantity: {} and amount {}", amountField, quantity,
                            amount);
            return amount.divide(quantity, 2, RoundingMode.HALF_UP);
        }
    }

    /**
     * The Class UnitPriceMeasureExpression.
     *
     * @author The eFaps Team
     */
    public static class UnitPriceMessuredExpression
        extends AbstractSimpleExpression<BigDecimal>
    {

        /** The Constant serialVersionUID. */
        private static final long serialVersionUID = 1L;

        /** The unit price variable. */
        private final CrosstabVariableBuilder<BigDecimal> unitPriceVariable;

        /**
         * Instantiates a new unit price messured expression.
         *
         * @param _unitPriceVariable the unit price variable
         */
        public UnitPriceMessuredExpression(final CrosstabVariableBuilder<BigDecimal> _unitPriceVariable)
        {
            unitPriceVariable = _unitPriceVariable;
        }

        @Override
        public BigDecimal evaluate(final ReportParameters _reportParameters)
        {
            return _reportParameters.getValue(unitPriceVariable);
        }

    }
}
