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
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.efaps.admin.common.MsgPhrase;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.Listener;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.common.properties.PropertiesUtil;
import org.efaps.esjp.contacts.Contacts;
import org.efaps.esjp.erp.AbstractGroupedByDate_Base;
import org.efaps.esjp.erp.AbstractGroupedByDate_Base.DateGroup;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.erp.rest.modules.IFilteredReportProvider;
import org.efaps.esjp.sales.listener.IOnDocumentSumReport;
import org.efaps.esjp.sales.report.DocumentSumGroupedByDate_Base.ValueList;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.ui.html.dojo.charting.Axis;
import org.efaps.esjp.ui.html.dojo.charting.ColumnsChart;
import org.efaps.esjp.ui.html.dojo.charting.Data;
import org.efaps.esjp.ui.html.dojo.charting.Orientation;
import org.efaps.esjp.ui.html.dojo.charting.PlotLayout;
import org.efaps.esjp.ui.html.dojo.charting.Serie;
import org.efaps.esjp.ui.html.dojo.charting.Util;
import org.efaps.esjp.ui.rest.AutocompleteController;
import org.efaps.esjp.ui.rest.dto.AutocompleteResponseDto;
import org.efaps.esjp.ui.rest.dto.OptionDto;
import org.efaps.esjp.ui.rest.dto.ValueDto;
import org.efaps.esjp.ui.rest.dto.ValueDto.Builder;
import org.efaps.esjp.ui.rest.dto.ValueType;
import org.efaps.util.EFapsException;
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
 * TODO comment!.
 *
 * @author The eFaps Team
 */
@EFapsUUID("19c0ee49-3942-4872-9e7d-de1f0d73b446")
@EFapsApplication("eFapsApp-Sales")
public abstract class DocumentSumReport_Base
    extends FilteredReport
    implements IFilteredReportProvider
{

    /**
     * The Enum GROUP.
     *
     * @author The eFaps Team
     */
    public enum GROUP
    {

        /** The none. */
        NONE,

        /** The contact. */
        CONTACT;
    }

    /**
     * The Enum User.
     *
     */
    public enum User
    {

        /** The none. */
        NONE,

        /** The creator. */
        CREATOR,

        /** The modifier. */
        MODIFIER,

        /** The person. */
        PERSON;
    }

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DocumentSumReport.class);

    /**
     * DataBean list.
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

        final ValueList values = getData(_parameter).groupBy("partial", "type");

        final ComparatorChain<Map<String, Object>> chain = new ComparatorChain<>();
        chain.addComparator((_o1,
                             _o2) -> 0);
        Collections.sort(values, chain);

        int x = 0;
        final Map<String, Integer> xmap = new LinkedHashMap<>();

        final Map<String, ColumnsChart> carts = new HashMap<>();
        final Map<String, Map<String, Serie<Data>>> seriesMap = new HashMap<>();
        final Axis xAxis = new Axis().setName("x");
        for (final Map<String, Object> map : values) {

            if (!xmap.containsKey(map.get("partial"))) {
                xmap.put((String) map.get("partial"), x++);
            }

            final ColumnsChart columsChart;
            final Map<String, Serie<Data>> series;
            if (carts.containsKey(map.get("type"))) {
                columsChart = carts.get(map.get("type"));
                series = seriesMap.get(map.get("type"));
            } else {
                series = new HashMap<>();
                columsChart = new ColumnsChart().setPlotLayout(PlotLayout.CLUSTERED)
                                .setGap(5).setWidth(650).setHeight(400);
                columsChart.setTitle((String) map.get("type"));
                columsChart.setOrientation(Orientation.VERTICAL_CHART_LEGEND);
                CurrencyInst selected = null;
                final Map<String, Object> filterMap = getFilterMap(_parameter);
                boolean isBase = false;
                if (filterMap.containsKey("currency")) {
                    final CurrencyFilterValue filter = (CurrencyFilterValue) filterMap.get("currency");
                    if (filter.getObject() instanceof Instance && filter.getObject().isValid()) {
                        selected = CurrencyInst.get(filter.getObject());
                    } else if (filter.getObject() instanceof Instance && "BASE".equals(filter.getObject().getKey())) {
                        isBase = true;
                    }
                }
                if (selected == null) {
                    if (!isBase) {
                        for (final CurrencyInst currency : CurrencyInst.getAvailable()) {
                            final Serie<Data> serie = new Serie<>();
                            serie.setName(currency.getName());
                            series.put(currency.getISOCode(), serie);
                            columsChart.addSerie(serie);
                        }
                    }
                    final Serie<Data> baseSerie = new Serie<>();
                    series.put("BASE", baseSerie);
                    baseSerie.setName(DBProperties.getProperty(DocumentSumReport.class.getName() + ".BASE")
                                    + " " + CurrencyInst.get(Currency.getBaseCurrency()).getSymbol());
                    columsChart.addSerie(baseSerie);
                } else {
                    final Serie<Data> serie = new Serie<>();
                    serie.setName(selected.getName());
                    series.put(selected.getISOCode(), serie);
                    columsChart.addSerie(serie);
                }
                columsChart.addAxis(xAxis);
                carts.put((String) map.get("type"), columsChart);
                seriesMap.put((String) map.get("type"), series);
            }

            for (final Entry<String, Object> entry : map.entrySet()) {
                final DecimalFormat fmtr = NumberFormatter.get().getFormatter();
                final Data dataTmp = new Data().setSimple(false);
                final Serie<Data> serie = series.get(entry.getKey());
                if (serie != null) {
                    serie.addData(dataTmp);
                    final BigDecimal y = ((BigDecimal) entry.getValue()).abs();
                    dataTmp.setXValue(xmap.get(map.get("partial")));
                    dataTmp.setYValue(y);
                    dataTmp.setTooltip(fmtr.format(y) + " " + serie.getName() + " - " + map.get("partial"));
                }
            }

        }
        final List<Map<String, Object>> labels = new ArrayList<>();
        for (final Entry<String, Integer> entry : xmap.entrySet()) {
            final Map<String, Object> map = new HashMap<>();
            map.put("value", entry.getValue());
            map.put("text", Util.wrap4String(entry.getKey()));
            labels.add(map);
        }
        xAxis.setLabels(Util.mapCollectionToObjectArray(labels));
        final StringBuilder chartHtml = new StringBuilder()
                        .append("<div style=\"position: relative;\">");
        for (final ColumnsChart chart : carts.values()) {
            chartHtml.append("<div style=\"float:left\">").append(chart.getHtmlSnipplet()).append("</div>");
        }
        chartHtml.append("</div>");
        ret.put(ReturnValues.SNIPLETT, html + chartHtml);
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
     * Gets the data.
     *
     * @param _parameter Parameter as passed by the eFasp API
     * @return list of DataBeans
     * @throws EFapsException on error
     */
    protected ValueList getData(final Parameter _parameter)
        throws EFapsException
    {
        if (this.valueList == null) {
            final DocumentSumGroupedByDate ds = getGroupedBy(_parameter);
            final Map<String, Object> filter = getFilterMap(_parameter);
            final var dateFrom = (LocalDate) filter.get("dateFrom");
            final var dateTo = (LocalDate) filter.get("dateTo");

            final List<Type> typeList;
            if (filter.containsKey("type")) {
                typeList = evaluateTypeFilter("type");
            } else {
                typeList = getTypeList(_parameter);
            }
            final var dateGroup = evaluateEnumFilter("dateGroup", DocumentSumGroupedByDate_Base.DateGroup.class,
                            DateGroup.MONTH);

            this.valueList = ds.getValueList(_parameter, dateFrom, dateTo, dateGroup, Sales.REPORT_DOCSUM.get(),
                            typeList.toArray(new Type[typeList.size()]));
        }
        return this.valueList;
    }

    /**
     * Gets the grouped by.
     *
     * @param _parameter the _parameter
     * @return the grouped by
     * @throws EFapsException on error
     */
    protected DocumentSumGroupedByDate getGroupedBy(final Parameter _parameter)
        throws EFapsException
    {
        return new DocumentSumGroupedByDate()
        {

            @Override
            protected void add2QueryBuilder(final Parameter _parameter,
                                            final QueryBuilder _queryBldr)
                throws EFapsException
            {
                super.add2QueryBuilder(_parameter, _queryBldr);
                DocumentSumReport_Base.this.add2QueryBuilder(_parameter, _queryBldr);
            }

            @Override
            protected void add2Print(final Parameter _parameter,
                                     final MultiPrintQuery _multi)
                throws EFapsException
            {
                super.add2Print(_parameter, _multi);
                final var val = evaluateEnumFilter("userGroup", User.class, User.NONE);
                switch (val) {
                    case CREATOR:
                        // Admin_User_PersonMsgPhrase
                        _multi.addMsgPhrase(SelectBuilder.get().linkto(CIERP.DocumentAbstract.Creator),
                                        MsgPhrase.get(UUID.fromString("eec67428-1228-4b91-88c7-e600901887b2")));
                        break;
                    case MODIFIER:
                        // Admin_User_PersonMsgPhrase
                        _multi.addMsgPhrase(SelectBuilder.get().linkto(CIERP.DocumentAbstract.Modifier),
                                        MsgPhrase.get(UUID.fromString("eec67428-1228-4b91-88c7-e600901887b2")));
                        break;
                    case PERSON:
                        // Admin_User_PersonMsgPhrase
                        _multi.addMsgPhrase(SelectBuilder.get().linkto(CIERP.DocumentAbstract.Salesperson),
                                        MsgPhrase.get(UUID.fromString("eec67428-1228-4b91-88c7-e600901887b2")));
                        break;
                    default:
                        break;
                }
            }

            @Override
            protected void add2Map(final Parameter _parameter,
                                   final MultiPrintQuery _multi,
                                   final Map<String, Object> _map)
                throws EFapsException
            {
                super.add2Map(_parameter, _multi, _map);
                final var val = evaluateEnumFilter("userGroup", User.class, User.NONE);
                switch (val) {
                    case CREATOR:
                        // Admin_User_PersonMsgPhrase
                        _map.put("user", _multi.getMsgPhrase(
                                        SelectBuilder.get().linkto(CIERP.DocumentAbstract.Creator),
                                        MsgPhrase.get(UUID.fromString("eec67428-1228-4b91-88c7-e600901887b2"))));
                        break;
                    case MODIFIER:
                        // Admin_User_PersonMsgPhrase
                        _map.put("user", _multi.getMsgPhrase(
                                        SelectBuilder.get().linkto(CIERP.DocumentAbstract.Modifier),
                                        MsgPhrase.get(UUID.fromString("eec67428-1228-4b91-88c7-e600901887b2"))));
                        break;
                    case PERSON:
                        // Admin_User_PersonMsgPhrase
                        _map.put("user", _multi.getMsgPhrase(
                                        SelectBuilder.get().linkto(CIERP.DocumentAbstract.Salesperson),
                                        MsgPhrase.get(UUID.fromString("eec67428-1228-4b91-88c7-e600901887b2"))));
                        break;
                    default:
                        break;
                }
                LOG.debug("userGrpup", _map);
            }
        };
    }

    /**
     * Add2 query builder.
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
    }

    @Override
    protected Object getDefaultValue(final Parameter _parameter,
                                     final String _field,
                                     final String _type,
                                     final String _default)
        throws EFapsException
    {
        final Object ret;
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
     * Gets the report.
     *
     * @param _parameter Parameter as passed by the eFasp API
     * @return the report class
     * @throws EFapsException on error
     */
    @Override
    public AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new DynDocumentSumReport(this);
    }

    @Override
    public Object evalDefaultValue4Key(final String key)
    {
        return switch (key) {
            case "dateFrom": {
                try {
                    final var zoneId = Context.getThreadContext().getZoneId();
                    final var adjuster = evalTemporalAdjuster(Sales.REPORT_DOCSUM.get(), "DefaultDateFrom");
                    yield LocalDate.now(zoneId).with(adjuster);
                } catch (final EFapsException e) {
                    LOG.error("Catched", e);
                }
            }
            case "dateTo": {
                try {
                    final var zoneId = Context.getThreadContext().getZoneId();
                    yield LocalDate.now(zoneId);
                } catch (final EFapsException e) {
                    LOG.error("Catched", e);
                }
            }
            case "type": {
                try {
                    final var typeKeys = PropertiesUtil.analyseProperty(Sales.REPORT_DOCSUM.get(), "Type", 0)
                                    .values()
                                    .toArray(String[]::new);
                    yield getOptions4Types(typeKeys).stream().map(OptionDto::getValue).toList();
                } catch (final EFapsException e) {
                    LOG.error("Catched", e);
                }
            }
            case "currency": {
                yield "BASE";
            }
            case "dateGroup": {
                yield AbstractGroupedByDate_Base.DateGroup.MONTH.name();
            }
            case "contactGroup": {
                yield false;
            }
            case "userGroup": {
                yield User.NONE;
            }
            default:
                yield null;
        };
    }

    @Override
    public List<ValueDto> getFilters()
    {
        final List<ValueDto> ret = new ArrayList<>();
        final var filterMap = getFilterMap();
        try {
            for (final var filterDef : getFilterDefinitions()) {
                final var value = filterMap.get(filterDef.getName());
                switch (filterDef.getName()) {
                    case "contactGroup" -> {
                        ret.add(filterDef.withValue(toBoolean(value)).build());
                    }
                    case "type" -> {
                        final var typeValue = ((List<?>) value).stream().map(this::toLong).toList();
                        ret.add(filterDef.withValue(typeValue).build());
                    }
                    case "contact" -> {
                        if (value == null) {
                            ret.add(filterDef.build());
                        } else {
                            final List<OptionDto> contactOptions = new ArrayList<>();
                            final var contactEval = EQL.builder()
                                            .print((String) value)
                                            .attribute(CIContacts.Contact.Name)
                                            .evaluate();
                            while (contactEval.next()) {
                                contactOptions.add(OptionDto.builder()
                                                .withLabel(contactEval.get(CIContacts.Contact.Name))
                                                .withValue(contactEval.inst().getOid())
                                                .build());
                            }
                            ret.add(filterDef.withValue(value).withOptions(contactOptions).build());
                        }
                    }
                    default -> ret.add(filterDef.withValue(value).build());
                }
            }
        } catch (final EFapsException e) {
            LOG.error("Catched", e);
        }
        return ret;
    }

    @Override
    public List<Builder> getFilterDefinitions()
        throws EFapsException
    {
        final List<Builder> ret = new ArrayList<>();
        ret.add(ValueDto.builder()
                        .withName("dateFrom")
                        .withLabel(getLabel("dateFrom"))
                        .withType(ValueType.DATE)
                        .withRequired(true));
        ret.add(ValueDto.builder()
                        .withName("dateTo")
                        .withLabel(getLabel("dateTo"))
                        .withType(ValueType.DATE)
                        .withRequired(true));

        final var typeKeys = PropertiesUtil.analyseProperty(Sales.REPORT_DOCSUM.get(), "Type", 0)
                        .values()
                        .toArray(String[]::new);
        final var typeOptions = getOptions4Types(typeKeys);
        ret.add(ValueDto.builder()
                        .withName("type")
                        .withLabel(getLabel("type"))
                        .withType(ValueType.CHECKBOX)
                        .withRequired(true)
                        .withOptions(typeOptions));

        ret.add(ValueDto.builder()
                        .withName("contact")
                        .withLabel(getLabel("contact"))
                        .withType(ValueType.AUTOCOMPLETE));

        ret.add(ValueDto.builder()
                        .withName("currency")
                        .withLabel(getLabel("currency"))
                        .withType(ValueType.DROPDOWN)
                        .withOptions(getOptions4Currency(true)));

        ret.add(ValueDto.builder()
                        .withName("dateGroup")
                        .withLabel(getLabel("dateGroup"))
                        .withType(ValueType.RADIO)
                        .withOptions(getOptions4Enum(AbstractGroupedByDate_Base.DateGroup.class)));

        ret.add(ValueDto.builder()
                        .withName("contactGroup")
                        .withLabel(getLabel("contactGroup"))
                        .withType(ValueType.RADIO)
                        .withOptions(getOptions4Boolean("contactGroup")));

        ret.add(ValueDto.builder()
                        .withName("userGroup")
                        .withLabel(getLabel("userGroup"))
                        .withType(ValueType.RADIO)
                        .withOptions(getOptions4Enum(User.class)));
        return ret;
    }

    @Override
    public AutocompleteResponseDto autocomplete(final String fieldName,
                                                final String term)
    {
        final List<OptionDto> options = new ArrayList<>();
        try {
            final var parameters = ParameterUtil.instance();
            parameters.put(ParameterValues.OTHERS, term);

            if ("contact".equals(fieldName)) {
                final var returns = new Contacts().autoComplete4Contact(parameters);
                final List<?> values = (List<?>) returns.get(ReturnValues.VALUES);
                options.addAll(new AutocompleteController().evalOptions(values));
            }
        } catch (final EFapsException e) {
            LOG.error("Catched", e);
        }
        return AutocompleteResponseDto.builder()
                        .withOptions(options)
                        .build();
    }

    /**
     * Dynamic Report.
     *
     * @author The eFaps Team
     */
    public static class DynDocumentSumReport
        extends AbstractDynamicReport
    {

        /**
         * Report this DynamicReport is betted in.
         */
        private final DocumentSumReport_Base sumReport;

        /**
         * Instantiates a new dyn document sum report.
         *
         * @param _sumReport report
         */
        public DynDocumentSumReport(final DocumentSumReport_Base _sumReport)
        {
            this.sumReport = _sumReport;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final JRRewindableDataSource ret;
            if (getSumReport().isCached(_parameter)) {
                ret = getSumReport().getDataSourceFromCache(_parameter);
                try {
                    ret.moveFirst();
                } catch (final JRException e) {
                    e.printStackTrace();
                }
            } else {
                final ValueList values = getSumReport().getData(_parameter);
                ret = new JRMapCollectionDataSource((Collection) values);
                getSumReport().cache(_parameter, ret);
            }
            return ret;
        }

        @Override
        protected void addColumnDefinition(final Parameter _parameter,
                                           final JasperReportBuilder _builder)
            throws EFapsException
        {
            final CrosstabBuilder crosstab = DynamicReports.ctab.crosstab();

            for (final IOnDocumentSumReport listener : Listener.get().<IOnDocumentSumReport>invoke(
                            IOnDocumentSumReport.class)) {
                listener.prepend2ColumnDefinition(_parameter, _builder, crosstab);
            }

            final Map<String, Object> filterMap = getSumReport().getFilterMap(_parameter);
            CurrencyInst selected = null;
            boolean isBase = false;
            if (filterMap.containsKey("currency")) {
                selected = sumReport.evaluateCurrencyInstFilter("currency");
                isBase = "BASE".equals(filterMap.get("currency"));
            }

            if (sumReport.evaluateBooleanFilter("contactGroup")) {
                final CrosstabRowGroupBuilder<String> contactGroup = DynamicReports.ctab.rowGroup("contact",
                                String.class).setHeaderWidth(150);
                crosstab.addRowGroup(contactGroup);
            }

            final var val = sumReport.evaluateEnumFilter("userGroup", User.class, User.NONE);
            if (!User.NONE.equals(val)) {
                final CrosstabRowGroupBuilder<String> contactGroup = DynamicReports.ctab.rowGroup("user",
                                String.class).setHeaderWidth(150);
                crosstab.addRowGroup(contactGroup);
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

            final CrosstabColumnGroupBuilder<String> columnGroup = DynamicReports.ctab.columnGroup("partial",
                            String.class);

            crosstab.addColumnGroup(columnGroup);
            crosstab.setCellWidth(200);

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
        public DocumentSumReport_Base getSumReport()
        {
            return this.sumReport;
        }
    }
}
