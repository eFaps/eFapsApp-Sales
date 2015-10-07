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

package org.efaps.esjp.sales.dashboard;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.api.ui.IEsjpSnipplet;
import org.efaps.esjp.common.dashboard.AbstractDashboardPanel;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.report.DocumentSumGroupedByDate;
import org.efaps.esjp.sales.report.DocumentSumGroupedByDate_Base;
import org.efaps.esjp.sales.report.DocumentSumGroupedByDate_Base.ValueList;
import org.efaps.esjp.ui.html.dojo.charting.Axis;
import org.efaps.esjp.ui.html.dojo.charting.ColumnsChart;
import org.efaps.esjp.ui.html.dojo.charting.Data;
import org.efaps.esjp.ui.html.dojo.charting.Orientation;
import org.efaps.esjp.ui.html.dojo.charting.PlotLayout;
import org.efaps.esjp.ui.html.dojo.charting.Serie;
import org.efaps.esjp.ui.html.dojo.charting.Util;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("c4c43876-30f1-431d-9ba4-38758a9f80d5")
@EFapsApplication("eFapsApp-Sales")
public abstract class SalesPanel_Base
    extends AbstractDashboardPanel
    implements IEsjpSnipplet
{

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new sales panel_ base.
     */
    public SalesPanel_Base()
    {
        super();
    }

    /**
     * Instantiates a new sales panel_ base.
     *
     * @param _config the _config
     */
    public SalesPanel_Base(final String _config)
    {
        super(_config);
    }


    /**
     * Gets the height.
     *
     * @return the height
     */
    protected Integer getDays()
    {
        return Integer.valueOf(getConfig().getProperty("Days", "14"));
    }

    @Override
    public CharSequence getHtmlSnipplet()
        throws EFapsException
    {
        final StringBuilder html = new StringBuilder();

        final DocumentSumGroupedByDate ds = new DocumentSumGroupedByDate();
        final Parameter parameter = new Parameter();

        final DateTime start = new DateTime().minusDays(getDays());
        final DateTime end = new DateTime().plusDays(1);

        final ValueList values = ds.getValueList(parameter, start, end, DocumentSumGroupedByDate_Base.DateGroup.DAY,
                        getConfig()).groupBy("partial", "type");
        final ComparatorChain<Map<String, Object>> chain = new ComparatorChain<>();
        chain.addComparator(new Comparator<Map<String, Object>>()
        {

            @Override
            public int compare(final Map<String, Object> _o1,
                               final Map<String, Object> _o2)
            {
                return String.valueOf(_o1.get("partial")).compareTo(String.valueOf(_o2.get("partial")));
            }

        });
        Collections.sort(values, chain);

        int x = 0;
        final Map<String, Integer> xmap = new LinkedHashMap<>();

        final Map<String, ColumnsChart> charts = new HashMap<>();
        final Map<String, Map<String, Serie<Data>>> seriesMap = new HashMap<>();
        final Axis xAxis = new Axis().setName("x");
        for (final Map<String, Object> map : values) {

            if (!xmap.containsKey(map.get("partial"))) {
                xmap.put((String) map.get("partial"), x++);
            }

            final ColumnsChart columsChart;
            final Map<String, Serie<Data>> series;
            if (charts.containsKey(map.get("type"))) {
                columsChart = charts.get(map.get("type"));
                series = seriesMap.get(map.get("type"));
            } else {
                series = new HashMap<>();
                columsChart = new ColumnsChart().setPlotLayout(PlotLayout.CLUSTERED)
                                .setGap(5).setWidth(getWidth()).setHeight(getHeight());
                columsChart.setTitle(getTitle());
                columsChart.setOrientation(Orientation.VERTICAL_CHART_LEGEND);
                final CurrencyInst selected = CurrencyInst.get(UUID.fromString("691758fc-a060-4bd5-b1fa-b33296638126"));

                final Serie<Data> serie = new Serie<Data>();
                serie.setName(selected.getName());
                series.put(selected.getISOCode(), serie);
                columsChart.addSerie(serie);

                columsChart.addAxis(xAxis);
                charts.put((String) map.get("type"), columsChart);
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
        for (final ColumnsChart chart : charts.values()) {
            html.append(chart.getHtmlSnipplet());
        }
        return html;
    }

    @Override
    public boolean isVisible()
        throws EFapsException
    {
        return true;
    }
}
