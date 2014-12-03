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
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */

package org.efaps.esjp.sales.report;

import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabMeasureBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabRowGroupBuilder;
import net.sf.dynamicreports.report.constant.Calculation;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.uiform.Field;
import org.efaps.esjp.common.uiform.Field_Base.DropDownPosition;
import org.efaps.esjp.common.uiform.Field_Base.ListType;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.report.DocumentSumGroupedByDate_Base.DataBean;
import org.efaps.esjp.sales.report.DocumentSumGroupedByDate_Base.ValueBean;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.SalesSettings;
import org.efaps.esjp.ui.html.dojo.charting.Axis;
import org.efaps.esjp.ui.html.dojo.charting.ColumnsChart;
import org.efaps.esjp.ui.html.dojo.charting.Data;
import org.efaps.esjp.ui.html.dojo.charting.Orientation;
import org.efaps.esjp.ui.html.dojo.charting.PlotLayout;
import org.efaps.esjp.ui.html.dojo.charting.Serie;
import org.efaps.esjp.ui.html.dojo.charting.Util;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.Partial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: DocumentSumReport_Base.java 14547 2014-11-30 22:44:37Z
 *          jan@moxter.net $
 */
@EFapsUUID("19c0ee49-3942-4872-9e7d-de1f0d73b446")
@EFapsRevision("$Rev$")
public abstract class DocumentSumReport_Base
    extends FilteredReport
{

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DocumentSumReport.class);

    /**
     * DataBean list.
     */
    private List<? extends DataBean> data;

    @Override
    public Return getTypeFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final List<DropDownPosition> values = new ArrayList<>();
        final Map<String, Object> filter = getFilterMap(_parameter);
        final Set<Long> selected = new HashSet<>();
        if (filter.containsKey("type")) {
            final TypeFilterValue filters = (TypeFilterValue) filter.get("type");
            selected.addAll(filters.getObject());
        }
        final List<Type> types = getTypeList(_parameter);
        for (final Type type : types) {
            final DropDownPosition dropdown = new DropDownPosition(type.getId(), type.getLabel());
            dropdown.setSelected(selected.contains(type.getId()));
            values.add(dropdown);
        }
        final Return ret = new Return();
        ret.put(ReturnValues.SNIPLETT, new Field().getInputField(_parameter, values, ListType.CHECKBOX));
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return Return containing the file
     * @throws EFapsException on error
     */
    public Return getCurrencyFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final List<DropDownPosition> values = new ArrayList<>();
        final Map<String, Object> filterMap = getFilterMap(_parameter);
        Object current = null;
        if (filterMap.containsKey("currency")) {
            final CurrencyFilterValue filter = (CurrencyFilterValue) filterMap.get("currency");
            current = filter.getObject();
        }
        for (final CurrencyInst currency : CurrencyInst.getAvailable()) {
            final DropDownPosition dropdown = new DropDownPosition(currency.getInstance().getOid(), currency.getName());
            dropdown.setSelected(currency.getInstance().equals(current));
            values.add(dropdown);
        }
        values.add(new DropDownPosition("-", "-"));
        Collections.sort(values, new Comparator<DropDownPosition>()
        {
            @Override
            public int compare(final DropDownPosition _o1,
                               final DropDownPosition _o2)
            {
                return String.valueOf(_o1.getOrderValue()).compareTo(String.valueOf(_o2.getOrderValue()));
            }
        });
        final Return ret = new Return();
        ret.put(ReturnValues.SNIPLETT, new Field().getDropDownField(_parameter, values));
        return ret;
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
        String html = dyRp.getHtmlSnipplet(_parameter);

        final List<? extends DataBean> dataBeans = getData(_parameter);
        Collections.sort(this.data, new Comparator<DataBean>()
        {

            @Override
            public int compare(final DataBean _arg0,
                               final DataBean _arg1)
            {
                return _arg0.getPartial().compareTo(_arg1.getPartial());
            }
        });
        int x = 0;
        final Map<Partial, Integer> xmap = new LinkedHashMap<>();

        final Map<Type, ColumnsChart> carts = new HashMap<>();
        final Map<Type, Map<String, Serie<Data>>> seriesMap = new HashMap<>();
        final Axis xAxis = new Axis().setName("x");
        for (final DataBean bean : dataBeans) {

            if (!xmap.containsKey(bean.getPartial())) {
                xmap.put(bean.getPartial(), x++);
            }
            for (final ValueBean value : bean.getValues().values()) {
                final ColumnsChart columsChart;
                final Map<String, Serie<Data>> series;
                if (carts.containsKey(value.getType())) {
                    columsChart = carts.get(value.getType());
                    series = seriesMap.get(value.getType());
                } else {
                    series = new HashMap<>();
                    columsChart = new ColumnsChart().setPlotLayout(PlotLayout.CLUSTERED)
                                    .setGap(5).setWidth(650).setHeight(400);
                    columsChart.setTitle(value.getType().getLabel());
                    columsChart.setOrientation(Orientation.VERTICAL_CHART_LEGEND);
                    CurrencyInst selected = null;
                    final Map<String, Object> filterMap = getFilterMap(_parameter);
                    if (filterMap.containsKey("currency")) {
                        final CurrencyFilterValue filter = (CurrencyFilterValue) filterMap.get("currency");
                        if (filter.getObject() instanceof Instance && filter.getObject().isValid()) {
                            selected = CurrencyInst.get(filter.getObject());
                        }
                    }
                    if (selected == null) {
                        for (final CurrencyInst currency : CurrencyInst.getAvailable()) {
                            final Serie<Data> serie = new Serie<Data>();
                            serie.setName(currency.getName());
                            series.put(currency.getISOCode(), serie);
                            columsChart.addSerie(serie);
                        }
                        final Serie<Data> baseSerie = new Serie<Data>();
                        series.put("BASE", baseSerie);
                        baseSerie.setName(DBProperties.getProperty(DocumentSumReport.class.getName() + ".BASE")
                                        + " " + CurrencyInst.get(Currency.getBaseCurrency()).getSymbol());
                        columsChart.addSerie(baseSerie);
                    } else {
                        final Serie<Data> serie = new Serie<Data>();
                        serie.setName(selected.getName());
                        series.put(selected.getISOCode(), serie);
                        columsChart.addSerie(serie);
                    }
                    columsChart.addAxis(xAxis);
                    carts.put(value.getType(), columsChart);
                    seriesMap.put(value.getType(), series);
                }

                for (final Entry<String, BigDecimal> entry : value.getAmounts().entrySet()) {
                    final DecimalFormat fmtr = NumberFormatter.get().getFormatter();
                    final Data dataTmp = new Data().setSimple(false);
                    final Serie<Data> serie = series.get(entry.getKey());
                    if (serie != null) {
                        serie.addData(dataTmp);
                        final BigDecimal y = value.getAmounts().get(entry.getKey()).abs();
                        dataTmp.setXValue(xmap.get(bean.getPartial()));
                        dataTmp.setYValue(y);
                        dataTmp.setTooltip(fmtr.format(y) + " " + serie.getName() + " - " + bean.getPartial());
                    }
                }
            }
        }
        final List<Map<String, Object>> labels = new ArrayList<>();
        for (final Entry<Partial, Integer> entry : xmap.entrySet()) {
            final Map<String, Object> map = new HashMap<>();
            map.put("value", entry.getValue());
            map.put("text", Util.wrap4String(entry.getKey()));
            labels.add(map);
        }
        xAxis.setLabels(Util.mapCollectionToObjectArray(labels));
        for (final ColumnsChart chart : carts.values()) {
            html = html + "<div style=\"float:left\">" + chart.getHtmlSnipplet() + "</div>";
        }
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
     * @return list of DataBeans
     * @throws EFapsException on error
     */
    protected List<? extends DataBean> getData(final Parameter _parameter)
        throws EFapsException
    {
        if (this.data == null) {
            final DocumentSumGroupedByDate ds = new DocumentSumGroupedByDate();
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
            final Properties props = Sales.getSysConfig().getAttributeValueAsProperties(SalesSettings.DOCSUMREPORT,
                            true);
            this.data = ds.getDataBeans(start, end, DocumentSumGroupedByDate_Base.DateGroup.MONTH, props,
                            typeList.toArray(new Type[typeList.size()]));
        }
        return this.data;
    }

    @Override
    protected Object getDefaultValue(final Parameter _parameter,
                                     final String _field,
                                     final String _type,
                                     final String _default)
        throws EFapsException
    {
        Object ret;
        if ("Type".equalsIgnoreCase(_type)) {
            final Set<Long> set = new HashSet<>();
            final List<Type> types = getTypeList(_parameter);
            for (final Type type : types) {
                set.add(type.getId());
            }
            ret = new TypeFilterValue().setObject(set);
        } else {
            ret = super.getDefaultValue(_parameter, _field, _type, _default);
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return list of types for the report
     * @throws EFapsException on error
     */
    protected List<Type> getTypeList(final Parameter _parameter)
        throws EFapsException
    {
        final List<Type> ret = new ArrayList<>();
        final Properties props = Sales.getSysConfig().getAttributeValueAsProperties(SalesSettings.DOCSUMREPORT, true);
        final String key = "TYPE";
        for (int i = 1; i < 100; i++) {
            final String keyTmp = key + String.format("%02d", i);
            if (props.containsKey(keyTmp)) {
                final String typeStr = props.getProperty(keyTmp);
                if (isUUID(typeStr)) {
                    ret.add(Type.get(UUID.fromString(typeStr)));
                } else {
                    ret.add(Type.get(typeStr));
                }
            } else {
                break;
            }
        }
        LOG.debug("Found types: {}", ret);
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
        return new DynDocumentSumReport(this);
    }

    /**
     * Dynamic Report.
     */
    public static class DynDocumentSumReport
        extends AbstractDynamicReport
    {

        /**
         * Report this DynamicReport is betted in.
         */
        private final DocumentSumReport_Base sumReport;

        /**
         * @param _sumReport report
         */
        public DynDocumentSumReport(final DocumentSumReport_Base _sumReport)
        {
            this.sumReport = _sumReport;
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final List<? extends DataBean> data = this.sumReport.getData(_parameter);
            Collections.sort(data, new Comparator<DataBean>()
            {

                @Override
                public int compare(final DataBean _arg0,
                                   final DataBean _arg1)
                {
                    return _arg0.getPartial().compareTo(_arg1.getPartial());
                }
            });
            final Collection<Map<String, ?>> datasource = new ArrayList<>();
            for (final DataBean bean : data) {
                bean.add2MapCollection(datasource);
            }
            return new JRMapCollectionDataSource(datasource);
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final CrosstabBuilder crosstab = DynamicReports.ctab.crosstab();
            final Map<String, Object> filterMap = getSumReport().getFilterMap(_parameter);
            CurrencyInst selected = null;
            if (filterMap.containsKey("currency")) {
                final CurrencyFilterValue filter = (CurrencyFilterValue) filterMap.get("currency");
                if (filter.getObject() instanceof Instance && filter.getObject().isValid()) {
                    selected = CurrencyInst.get(filter.getObject());
                }
            }
            if (selected == null) {
                for (final CurrencyInst currency : CurrencyInst.getAvailable()) {
                    final CrosstabMeasureBuilder<BigDecimal> amountMeasure = DynamicReports.ctab.measure(
                                    currency.getSymbol(),
                                    currency.getISOCode(), BigDecimal.class, Calculation.SUM);
                    crosstab.addMeasure(amountMeasure);
                }
                final CrosstabMeasureBuilder<BigDecimal> amountMeasure = DynamicReports.ctab.measure(
                                DBProperties.getProperty(DocumentSumReport.class.getName() + ".BASE")
                                                + " " + CurrencyInst.get(Currency.getBaseCurrency()).getSymbol(), "BASE",
                                BigDecimal.class, Calculation.SUM);
                crosstab.addMeasure(amountMeasure);
            } else {
                final CrosstabMeasureBuilder<BigDecimal> amountMeasure = DynamicReports.ctab.measure(
                                selected.getSymbol(),
                                selected.getISOCode(), BigDecimal.class, Calculation.SUM);
                crosstab.addMeasure(amountMeasure);
            }
            final CrosstabRowGroupBuilder<String> rowTypeGroup = DynamicReports.ctab.rowGroup("type", String.class)
                            .setHeaderWidth(150);
            crosstab.addRowGroup(rowTypeGroup);

            final CrosstabColumnGroupBuilder<String> columnGroup = DynamicReports.ctab.columnGroup("group",
                            String.class);

            crosstab.addColumnGroup(columnGroup);

            _builder.addSummary(crosstab);

            /*
             * final BarChartBuilder chart = DynamicReports.cht.barChart()
             * .setCategory(groupColumn)
             * .series(DynamicReports.cht.serie(amountColumn
             * ).setSeries(typeColumn)) .setSeriesOrderType(OrderType.ASCENDING)
             * .
             * setValueAxisFormat(DynamicReports.cht.axisFormat().setLabel("Stock"
             * )); _builder.addSummary(chart);
             */
        }


        /**
         * Getter method for the instance variable {@link #sumReport}.
         *
         * @return value of instance variable {@link #sumReport}
         */
        public DocumentSumReport_Base getSumReport()
        {
            return this.sumReport;
        }
    }

}
