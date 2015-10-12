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

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.lang3.EnumUtils;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.api.ui.IEsjpSnipplet;
import org.efaps.db.Instance;
import org.efaps.esjp.common.dashboard.AbstractDashboardPanel;
import org.efaps.esjp.common.datetime.JodaTimeUtils;
import org.efaps.esjp.erp.Currency;
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
    protected Integer getQuantity()
    {
        return Integer.valueOf(getConfig().getProperty("Quantity", "14"));
    }

    /**
     * Gets the currency inst.
     *
     * @return the currency inst
     * @throws EFapsException on error
     */
    protected Instance getCurrencyInst()
        throws EFapsException
    {
        return Instance.get(getConfig().getProperty("CurrencyOID", "1.1"));
    }

    /**
     * Gets the group.
     *
     * @return the group
     */
    protected DocumentSumGroupedByDate_Base.DateGroup getDateGroup()
    {
        return EnumUtils.getEnum(DocumentSumGroupedByDate_Base.DateGroup.class,
                        getConfig().getProperty("DateGroup", "DAY"));
    }

    /**
     * Checks if is date group start.
     *
     * @return true, if is date group start
     */
    protected boolean isDateGroupStart()
    {
        return "true".equalsIgnoreCase(getConfig().getProperty("DateGroupStart", "false"));
    }

    /**
     * Show serie.
     *
     * @param _key the _key
     * @return true, if successful
     * @throws EFapsException on error
     */
    protected boolean showSerie(final String _key)
        throws EFapsException
    {
        boolean ret = true;
        if (getCurrencyInst().isValid()) {
            ret = _key.equals(CurrencyInst.get(getCurrencyInst()).getISOCode());
        } else {
            if (getCurrencyInst().getId() == 1) {
                ret = true;
            } else {
                ret = _key.equals("BASE");
            }
        }
        return ret;
    }

    @Override
    public CharSequence getHtmlSnipplet()
        throws EFapsException
    {
        final DocumentSumGroupedByDate ds = new DocumentSumGroupedByDate();
        final Parameter parameter = new Parameter();
        final boolean isDateStart = isDateGroupStart();
        final DateTime start;
        switch (getDateGroup()) {
            case YEAR:
                start = isDateStart ? new DateTime().minusYears(getQuantity()).withDayOfYear(1)
                                : new DateTime().minusYears(getQuantity());
                break;
            case HALFYEAR:
                start = new DateTime().withFieldAdded(JodaTimeUtils.halfYears(), -getQuantity());
                break;
            case QUARTER:
                start = new DateTime().withFieldAdded(JodaTimeUtils.quarters(), -getQuantity());
                break;
            case MONTH:
                start = isDateStart ? new DateTime().minusMonths(getQuantity()).withDayOfMonth(1)
                                : new DateTime().minusMonths(getQuantity());
                break;
            case WEEK:
                start = isDateStart ? new DateTime().minusWeeks(getQuantity()).withDayOfWeek(1)
                                : new DateTime().minusWeeks(getQuantity());
                break;
            case DAY:
            default:
                start = new DateTime().minusDays(getQuantity());
                break;
        }
        final DateTime end = new DateTime().plusDays(1);

        final ValueList values = ds.getValueList(parameter, start, end, getDateGroup(),
                        getConfig()).groupBy("partial");
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
        final ColumnsChart chart = new ColumnsChart().setPlotLayout(PlotLayout.CLUSTERED)
                        .setGap(5).setWidth(getWidth()).setHeight(getHeight());
        final String title = getTitle();
        if (title != null && !title.isEmpty()) {
            chart.setTitle(getTitle());
        }
        chart.setOrientation(Orientation.VERTICAL_CHART_LEGEND);

        final Axis xAxis = new Axis().setName("x");
        chart.addAxis(xAxis);

        final Map<String, Serie<Data>> series = new HashMap<>();
        final Serie<Data> baseSerie = new Serie<Data>();
        baseSerie.setName(DBProperties.getProperty("org.efaps.esjp.sales.report.DocumentSumReport.BASE") + " "
                        + CurrencyInst.get(Currency.getBaseCurrency()).getSymbol());
        series.put("BASE", baseSerie);
        for (final CurrencyInst curr : CurrencyInst.getAvailable()) {
            final Serie<Data> serie = new Serie<Data>();
            serie.setName(curr.getName());
            series.put(curr.getISOCode(), serie);
        }

        for (final Map<String, Object> map : values) {
            if (!xmap.containsKey(map.get("partial"))) {
                xmap.put((String) map.get("partial"), x++);
            }
            for (final Entry<String, Object> entry : map.entrySet()) {
                final DecimalFormat fmtr = NumberFormatter.get().getFormatter();
                final Data dataTmp = new Data().setSimple(false);
                final Serie<Data> serie = series.get(entry.getKey());
                if (serie != null && showSerie(entry.getKey())) {
                    serie.addData(dataTmp);
                    final BigDecimal y = ((BigDecimal) entry.getValue());
                    // for the case that negaitve numbers are given
                    if (y.compareTo(BigDecimal.ZERO) < 0 && chart.getAxis().size() < 2) {
                        chart.addAxis(new Axis().setName("y").setVertical(true));
                    }
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

        int count = 0;
        for (final Serie<Data> serie : series.values()) {
            if (!serie.getData().isEmpty()) {
                count++;
            }
        }

        for (final Entry<String, Serie<Data>> entry : series.entrySet()) {
            // only one currency and base is used, do not show base
            if (count == 2) {
                if (!"BASE".equals(entry.getKey()) && !entry.getValue().getData().isEmpty()) {
                    chart.addSerie(entry.getValue());
                }
            } else {
                if (!entry.getValue().getData().isEmpty()) {
                    chart.addSerie(entry.getValue());
                }
            }
        }

        xAxis.setLabels(Util.mapCollectionToObjectArray(labels));
        return chart.getHtmlSnipplet();
    }

    @Override
    public boolean isVisible()
        throws EFapsException
    {
        return true;
    }
}
