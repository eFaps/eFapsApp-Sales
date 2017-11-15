/*
 * Copyright 2003 - 2015 The eFaps Team
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
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.lang3.BooleanUtils;
import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.Listener;
import org.efaps.db.Instance;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.sales.listener.IOnDocumentSumReport;
import org.efaps.esjp.sales.payment.DocPaymentInfo;
import org.efaps.esjp.sales.report.DocumentSumGroupedByDate_Base.ValueList;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

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
@EFapsUUID("086c53e8-bb2a-4235-b234-8b9c38cadc7c")
@EFapsApplication("eFapsApp-Sales")
public abstract class DocSituationReport_Base
    extends FilteredReport
{

    /**
     * DataBean list.
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
        final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String mime = (String) props.get("Mime");
        final AbstractDynamicReport dyRp = getReport(_parameter);
        dyRp.setFileName(DBProperties.getProperty(DocSituationReport.class.getName() + ".FileName"));
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
     * @return list of DataBeans
     * @throws EFapsException on error
     */
    protected ValueList getData(final Parameter _parameter)
        throws EFapsException
    {
        if (this.valueList == null) {
            final DocumentSumGroupedByDate ds = new DocumentSumGroupedByDate()
            {

                @Override
                protected void add2QueryBuilder(final Parameter _parameter,
                                                final QueryBuilder _queryBldr)
                    throws EFapsException
                {
                    super.add2QueryBuilder(_parameter, _queryBldr);
                    DocSituationReport_Base.this.add2QueryBuilder(_parameter, _queryBldr);
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
            final Properties props = getProperties4TypeList(_parameter, null);
            final DocumentSumGroupedByDate_Base.DateGroup dateGroup;
            if (filter.containsKey("dateGroup") && filter.get("dateGroup") != null) {
                dateGroup = (DocumentSumGroupedByDate_Base.DateGroup)
                                ((EnumFilterValue) filter.get("dateGroup")).getObject();
            } else {
                dateGroup = DocumentSumGroupedByDate_Base.DateGroup.MONTH;
            }

            this.valueList = ds.getValueList(_parameter, start, end, dateGroup, props,
                            typeList.toArray(new Type[typeList.size()]));

            final Set<Instance> instances = this.valueList.getDocInstances();
            final Map<Instance, DocPaymentInfo> infos = new HashMap<>();
            for (final Instance inst  :instances) {
                final DocPaymentInfo info = new DocPaymentInfo(inst);
                infos.put(inst, info);
            }
            if (!infos.isEmpty()) {
                DocPaymentInfo.initialize(_parameter, infos.values().toArray(
                                new DocPaymentInfo[infos.values().size()]));
                for (final Map<String, Object> map : this.valueList) {
                    final Instance docInstance = (Instance) map.get("docInstance");
                    if (docInstance != null && docInstance.isValid()) {
                        final DocPaymentInfo info = infos.get(docInstance);
                        final boolean perpay = BooleanUtils.toBoolean(props.getProperty(
                                        docInstance.getType().getName() + ".PaymentPerPayment"));
                        BigDecimal paid = info.getPaid(perpay);
                        if ("true".equals(props.getProperty(docInstance.getType().getName() + ".Negate"))) {
                            paid = paid.negate();
                        }
                        map.put("BASEPaid", paid);
                        map.put("BASEDifference", ((BigDecimal) map.get("BASE")).subtract(paid));
                    }
                }
            }
        }
        return this.valueList;
    }

    /**
     * Add to query builder.
     *
     * @param _parameter the _parameter
     * @param _queryBldr the _query bldr
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
                _queryBldr.addWhereAttrEqValue(CISales.DocumentSumAbstract.Contact, filterValue.getObject());
            }
        }
        for (final IOnDocumentSumReport listener : Listener.get().<IOnDocumentSumReport>invoke(
                        IOnDocumentSumReport.class)) {
            listener.add2QueryBuilder(_parameter, _queryBldr);
        }
        _queryBldr.setCompanyDependent(isCompanyDependent(_parameter));
    }

    /**
     * Checks if is company depended.
     *
     * @param _parameter the _parameter
     * @return true, if is company depended
     * @throws EFapsException on error
     */
    protected boolean isCompanyDependent(final Parameter _parameter)
        throws EFapsException
    {
        return "true".equalsIgnoreCase(getProperty(_parameter, "CompanyDependent", "true"));
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
            ret = Sales.DOCSITUATIONREPORT.get();
        }
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
        return new DynDocSituationReport(this);
    }

    /**
     * Dynamic Report.
     */
    public static class DynDocSituationReport
        extends AbstractDynamicReport
    {

        /**
         * Report this DynamicReport is betted in.
         */
        private final DocSituationReport_Base sumReport;

        /**
         * @param _sumReport report
         */
        public DynDocSituationReport(final DocSituationReport_Base _sumReport)
        {
            this.sumReport = _sumReport;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            JRRewindableDataSource ret;
            if (getFilteredReport().isCached(_parameter)) {
                ret = getFilteredReport().getDataSourceFromCache(_parameter);
                try {
                    ret.moveFirst();
                } catch (final JRException e) {
                    throw new EFapsException("JRException", e);
                }
            } else {
                final ValueList values = getFilteredReport().getData(_parameter);
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
                ret = new JRMapCollectionDataSource((Collection) values);
                getFilteredReport().cache(_parameter, ret);
            }
            return ret;
        }

        @Override
        protected void addColumnDefinition(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final CrosstabBuilder crosstab = DynamicReports.ctab.crosstab();

            final Map<String, Object> filterMap = getFilteredReport().getFilterMap(_parameter);

            for (final IOnDocumentSumReport listener : Listener.get().<IOnDocumentSumReport>invoke(
                            IOnDocumentSumReport.class)) {
                listener.prepend2ColumnDefinition(_parameter, _builder, crosstab);
            }

            if (filterMap.containsKey("contactGroup")) {
                final Boolean contactBool = (Boolean) filterMap.get("contactGroup");
                if (contactBool != null && contactBool) {
                    final CrosstabRowGroupBuilder<String> contactGroup = DynamicReports.ctab.rowGroup("contact",
                                    String.class).setHeaderWidth(150);
                    crosstab.addRowGroup(contactGroup);
                }
            }

            for (final CurrencyInst currency : CurrencyInst.getAvailable()) {
                final CrosstabMeasureBuilder<BigDecimal> amountMeasure = DynamicReports.ctab.measure(
                                currency.getSymbol(),
                                currency.getISOCode(), BigDecimal.class, Calculation.SUM);
                crosstab.addMeasure(amountMeasure);
            }
            final CrosstabMeasureBuilder<BigDecimal> amountMeasure = DynamicReports.ctab.measure(
                            DBProperties.getProperty(DocSituationReport.class.getName() + ".BASE")
                                            + " " + CurrencyInst.get(Currency.getBaseCurrency()).getSymbol(),
                            "BASE", BigDecimal.class, Calculation.SUM);
            crosstab.addMeasure(amountMeasure);

            final CrosstabMeasureBuilder<BigDecimal> paidMeasure = DynamicReports.ctab.measure(
                            DBProperties.getProperty(DocSituationReport.class.getName() + ".BASEPaid"),
                            "BASEPaid", BigDecimal.class, Calculation.SUM);
            crosstab.addMeasure(paidMeasure);

            final CrosstabMeasureBuilder<BigDecimal> diffMeasure = DynamicReports.ctab.measure(
                            DBProperties.getProperty(DocSituationReport.class.getName() + ".BASEDifference"),
                            "BASEDifference", BigDecimal.class, Calculation.SUM);
            crosstab.addMeasure(diffMeasure);

            final CrosstabRowGroupBuilder<String> rowTypeGroup = DynamicReports.ctab.rowGroup("type", String.class)
                            .setHeaderWidth(150);
            crosstab.addRowGroup(rowTypeGroup);

            final CrosstabColumnGroupBuilder<String> columnGroup = DynamicReports.ctab.columnGroup("partial",
                            String.class);

            crosstab.addColumnGroup(columnGroup);
            crosstab.setCellWidth(300);

            for (final IOnDocumentSumReport listener : Listener.get().<IOnDocumentSumReport>invoke(
                            IOnDocumentSumReport.class)) {
                listener.add2ColumnDefinition(_parameter, _builder, crosstab);
            }
            _builder.addSummary(crosstab);
        }

        /**
         * Getter method for the instance variable {@link #sumReport}.
         *
         * @return value of instance variable {@link #sumReport}
         */
        public DocSituationReport_Base getFilteredReport()
        {
            return this.sumReport;
        }
    }
}
