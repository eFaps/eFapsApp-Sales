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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.efaps.admin.common.MsgPhrase;
import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport_Base;
import org.efaps.esjp.erp.AbstractGroupedByDate_Base.DateGroup;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.sales.report.DocumentSumGroupedByDate_Base.ValueList;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
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
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

/**
 * The Class Employee2DocReport_Base.
 *
 * @author The eFaps Team
 */
@EFapsUUID("7df43364-82f3-48f5-b53f-a0f74cb905d3")
@EFapsApplication("eFapsApp-Sales")
public abstract class Employee2DocReport_Base
    extends FilteredReport
{

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Employee2DocReport.class);

    /**
     * Generate report.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException the e faps exception
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
        final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String mime = (String) props.get("Mime");
        final AbstractDynamicReport dyRp = getReport(_parameter);
        dyRp.setFileName(DBProperties.getProperty(DocumentSumReport.class.getName() + ".FileName"));
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
        return new DynEmployee2DocReport(this);
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
        if (ret == null) {
            ret = Sales.REPORT_EMPLOYEE2DOC.get();
        }
        return ret;
    }

    /**
     * The Class DynEmployee2DocReport.
     */
    public static class DynEmployee2DocReport
        extends AbstractDynamicReport
    {

        /** The filtered report. */
        private final Employee2DocReport_Base filteredReport;

        /**
         * Instantiates a new dyn employee2 doc report.
         *
         * @param _employee2DocReport the _employee2 doc report
         */
        public DynEmployee2DocReport(final Employee2DocReport_Base _employee2DocReport)
        {
            this.filteredReport = _employee2DocReport;
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
                    AbstractDynamicReport_Base.LOG.error("Catched JRException", e);
                }
            } else {
                final ValueList values = getDocumentData(_parameter);
                if (!values.isEmpty()) {
                    final QueryBuilder queryBldr = new QueryBuilder(CIHumanResource.Employee2DocumentAbstract);
                    queryBldr.addWhereAttrEqValue(CIHumanResource.Employee2DocumentAbstract.ToAbstractLink,
                                    values.getDocInstances().toArray());
                    add2QueryBuilder(_parameter, queryBldr);
                    final MultiPrintQuery multi = queryBldr.getPrint();
                    final SelectBuilder selDocInst = SelectBuilder.get()
                                    .linkto(CIHumanResource.Employee2DocumentAbstract.ToAbstractLink).instance();
                    multi.addSelect(selDocInst);
                    // HumanResource_EmployeeMsgPhrase as default
                    final String msgPhraseStr = Sales.REPORT_EMPLOYEE2DOC.get().getProperty("EmployeeMsgPhrase",
                                    "f543ca6d-29fb-4f1a-8747-0057b9a08404");
                    final SelectBuilder selEmpl = SelectBuilder.get()
                                    .linkto(CIHumanResource.Employee2DocumentAbstract.FromAbstractLink);
                    final MsgPhrase msgPhrase = isUUID(msgPhraseStr) ? MsgPhrase.get(UUID.fromString(msgPhraseStr))
                                    : MsgPhrase.get(msgPhraseStr);
                    multi.addMsgPhrase(selEmpl, msgPhrase);
                    multi.execute();
                    final Map<Instance, String> doc2employee = new HashMap<>();
                    while (multi.next()) {
                        final String employee = multi.getMsgPhrase(selEmpl, msgPhrase);
                        final Instance docInst = multi.getSelect(selDocInst);
                        doc2employee.put(docInst, employee);
                    }
                    for (final Map<String, Object> map : values) {
                        map.put("employee", doc2employee.get(map.get("docInstance")));
                    }
                }
                ret = new JRMapCollectionDataSource((Collection) values);
                getFilteredReport().cache(_parameter, ret);
            }
            return ret;
        }

        /**
         * Add2 query builder.
         *
         * @param _parameter the _parameter
         * @param _queryBldr the _query bldr
         * @throws EFapsException the e faps exception
         */
        protected void add2QueryBuilder(final Parameter _parameter,
                                        final QueryBuilder _queryBldr)
            throws EFapsException
        {
        }

        /**
         * Add2 document query builder.
         *
         * @param _parameter the _parameter
         * @param _queryBldr the _query bldr
         * @throws EFapsException the e faps exception
         */
        protected void add2DocumentQueryBuilder(final Parameter _parameter,
                                                final QueryBuilder _queryBldr)
            throws EFapsException
        {
            final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);
            final InstanceFilterValue filterValue = (InstanceFilterValue) filter.get("contact");
            if (filterValue != null && filterValue.getObject() != null && filterValue.getObject().isValid()) {
                _queryBldr.addWhereAttrEqValue(CISales.DocumentAbstract.Contact, filterValue.getObject());
            }

            if (Sales.REPORT_EMPLOYEE2DOC_CONDITION.get() && ArrayUtils.isNotEmpty(getConditionInsts(_parameter))) {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.ChannelCondition2DocumentAbstract);
                attrQueryBldr.addWhereAttrEqValue(CISales.ChannelCondition2DocumentAbstract.FromAbstractLink,
                                getConditionInsts(_parameter));

                if (isConditionNegate(_parameter)) {
                    _queryBldr.addWhereAttrNotInQuery(CISales.DocumentAbstract.ID,
                            attrQueryBldr.getAttributeQuery(CISales.ChannelCondition2DocumentAbstract.ToAbstractLink));
                } else {
                    _queryBldr.addWhereAttrInQuery(CISales.DocumentAbstract.ID,
                            attrQueryBldr.getAttributeQuery(CISales.ChannelCondition2DocumentAbstract.ToAbstractLink));
                }
            }
        }

        /**
         * Gets the product inst.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the product inst
         * @throws EFapsException on error
         */
        protected Object[] getConditionInsts(final Parameter _parameter)
            throws EFapsException
        {
            final Object[] ret;
            final Map<String, Object> filterMap = this.filteredReport.getFilterMap(_parameter);
            final InstanceSetFilterValue filter = (InstanceSetFilterValue) filterMap.get("condition");
            if (filter == null || (filter != null && filter.getObject() == null)) {
                ret = new Object[] {};
            } else {
                ret = filter.getObject().toArray();
            }
            return ret;
        }

        /**
         * Checks if is contact negate.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return true, if is contact negate
         * @throws EFapsException on error
         */
        protected boolean isConditionNegate(final Parameter _parameter)
            throws EFapsException
        {
            final Map<String, Object> filterMap = this.filteredReport.getFilterMap(_parameter);
            final InstanceSetFilterValue filter = (InstanceSetFilterValue) filterMap.get("condition");
            final boolean ret = filter != null && filter.isNegate();
            return ret;
        }

        /**
         * Gets the document data.
         *
         * @param _parameter the _parameter
         * @return the document data
         * @throws EFapsException the e faps exception
         */
        protected ValueList getDocumentData(final Parameter _parameter)
            throws EFapsException
        {
            final DocumentSumGroupedByDate ds = new DocumentSumGroupedByDate()
            {
                @Override
                protected void add2QueryBuilder(final Parameter _parameter,
                                                final QueryBuilder _queryBldr)
                    throws EFapsException
                {
                    super.add2QueryBuilder(_parameter, _queryBldr);
                    DynEmployee2DocReport.this.add2DocumentQueryBuilder(_parameter, _queryBldr);
                }

                @Override
                protected void add2Print(final Parameter _parameter,
                                         final MultiPrintQuery _multi)
                    throws EFapsException
                {
                    super.add2Print(_parameter, _multi);
                    _multi.addAttribute(CISales.DocumentAbstract.Name);
                }

                @Override
                protected void add2Map(final Parameter _parameter,
                                       final MultiPrintQuery _multi,
                                       final Map<String, Object> _map)
                    throws EFapsException
                {
                    super.add2Map(_parameter, _multi, _map);
                    final DateTime date = (DateTime) _map.get("date");
                    if (date == null) {
                        _map.put("docName", _multi.getAttribute(CISales.DocumentAbstract.Name));
                    } else {
                        _map.put("docName", _multi.getAttribute(CISales.DocumentAbstract.Name)
                                        + " " + date.toString(DateTimeFormat.forStyle("S-")
                                                        .withLocale(Context.getThreadContext().getLocale())));
                    }
                }
            };
            final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);
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
                typeList = getFilteredReport().getTypeList(_parameter);
            }
            final Properties props = getFilteredReport().getProperties4TypeList(_parameter, null);
            final DocumentSumGroupedByDate_Base.DateGroup dateGroup;
            if (filter.containsKey("dateGroup") && filter.get("dateGroup") != null) {
                dateGroup = (DateGroup) ((EnumFilterValue) filter.get("dateGroup")).getObject();
            } else {
                dateGroup = DocumentSumGroupedByDate_Base.DateGroup.MONTH;
            }

            return ds.getValueList(_parameter, start, end, dateGroup, props,
                            typeList.toArray(new Type[typeList.size()]));
        }

        @Override
        protected void addColumnDefinition(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
                                              throws EFapsException
        {
            final CrosstabBuilder crosstab = DynamicReports.ctab.crosstab();

            final Map<String, Object> filterMap = getFilteredReport().getFilterMap(_parameter);
            CurrencyInst selected = null;
            boolean isBase = false;
            if (filterMap.containsKey("currency")) {
                final CurrencyFilterValue filter = (CurrencyFilterValue) filterMap.get("currency");
                if (filter.getObject() instanceof Instance && filter.getObject().isValid()) {
                    selected = CurrencyInst.get(filter.getObject());
                } else if (filter.getObject() instanceof Instance && "BASE".equals(filter.getObject().getKey())) {
                    isBase = true;
                }
            }

            final CrosstabRowGroupBuilder<String> employeeGroup = DynamicReports.ctab.rowGroup("employee",
                            String.class).setHeaderWidth(150);
            crosstab.addRowGroup(employeeGroup);

            if (filterMap.containsKey("contactGroup")) {
                if (BooleanUtils.isTrue((Boolean) filterMap.get("contactGroup"))) {
                    final CrosstabRowGroupBuilder<String> contactGroup = DynamicReports.ctab.rowGroup("contact",
                                    String.class).setHeaderWidth(150);
                    crosstab.addRowGroup(contactGroup);
                }
            }

            final CrosstabRowGroupBuilder<String> rowTypeGroup = DynamicReports.ctab.rowGroup("type", String.class)
                            .setHeaderWidth(150);
            crosstab.addRowGroup(rowTypeGroup);

            if (filterMap.containsKey("docDetails")) {
                if (BooleanUtils.isTrue((Boolean) filterMap.get("docDetails"))) {
                    final CrosstabRowGroupBuilder<String> docDetailsGroup = DynamicReports.ctab.rowGroup("docName",
                                    String.class);
                    crosstab.addRowGroup(docDetailsGroup);
                }
            }

            if (selected == null) {
                if (!isBase) {
                    for (final CurrencyInst currency : CurrencyInst.getAvailable()) {
                        final CrosstabMeasureBuilder<BigDecimal> amountMeasure = DynamicReports.ctab.measure(
                                        currency.getSymbol(), currency.getISOCode(), BigDecimal.class, Calculation.SUM);
                        crosstab.addMeasure(amountMeasure);
                    }
                }
                final CrosstabMeasureBuilder<BigDecimal> amountMeasure = DynamicReports.ctab.measure(
                                DBProperties.getProperty(DocumentSumReport.class.getName() + ".BASE")
                                                + " " + CurrencyInst.get(Currency.getBaseCurrency()).getSymbol(),
                                "BASE", BigDecimal.class, Calculation.SUM);

                final CrosstabMeasureBuilder<BigDecimal> countMeasure = DynamicReports.ctab.measure(
                                getFilteredReport().getDBProperty("Count"),
                                "BASE", BigDecimal.class, Calculation.COUNT);

                crosstab.addMeasure(amountMeasure, countMeasure);
            } else {
                final CrosstabMeasureBuilder<BigDecimal> amountMeasure = DynamicReports.ctab.measure(
                                selected.getSymbol(),
                                selected.getISOCode(), BigDecimal.class, Calculation.SUM);

                final CrosstabMeasureBuilder<BigDecimal> countMeasure = DynamicReports.ctab.measure(
                                getFilteredReport().getDBProperty("Count"),
                                selected.getISOCode(), BigDecimal.class, Calculation.COUNT);

                crosstab.addMeasure(amountMeasure, countMeasure);
            }

            final CrosstabColumnGroupBuilder<String> columnGroup = DynamicReports.ctab.columnGroup("partial",
                            String.class);

            crosstab.addColumnGroup(columnGroup);
            crosstab.setCellWidth(200);
            _builder.addSummary(crosstab);
        }

        /**
         * Gets the filtered report.
         *
         * @return the filtered report
         */
        public Employee2DocReport_Base getFilteredReport()
        {
            return this.filteredReport;
        }
    }
}
