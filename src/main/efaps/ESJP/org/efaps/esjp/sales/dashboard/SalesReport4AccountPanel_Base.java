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
package org.efaps.esjp.sales.dashboard;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.api.ui.IEsjpSnipplet;
import org.efaps.esjp.common.dashboard.AbstractDashboardPanel;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.report.SalesReport4Account;
import org.efaps.esjp.sales.report.SalesReport4Account.DynReport4Account;
import org.efaps.esjp.sales.report.SalesReport4Account_Base;
import org.efaps.esjp.sales.report.SalesReport4Account_Base.ReportKey;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.ui.html.dojo.charting.Axis;
import org.efaps.esjp.ui.html.dojo.charting.BarsChart;
import org.efaps.esjp.ui.html.dojo.charting.Data;
import org.efaps.esjp.ui.html.dojo.charting.Orientation;
import org.efaps.esjp.ui.html.dojo.charting.Serie;
import org.efaps.esjp.ui.html.dojo.charting.Util;
import org.efaps.util.EFapsBaseException;
import org.efaps.util.EFapsException;
import org.efaps.util.cache.CacheReloadException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

/**
 * The Class SalesReport4AccountPanel_Base.
 *
 * @author The eFaps Team
 */
@EFapsUUID("1b756456-73dc-4fcf-b7d1-ad26219eb70b")
@EFapsApplication("eFapsApp-Sales")
public abstract class SalesReport4AccountPanel_Base
    extends AbstractDashboardPanel
    implements IEsjpSnipplet
{

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SalesReport4AccountPanel.class);

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new sales4 contact panel.
     */
    public SalesReport4AccountPanel_Base()
    {
        super();
    }

    /**
     * Instantiates a new sales4 contact panel_ base.
     *
     * @param _config the _config
     */
    public SalesReport4AccountPanel_Base(final String _config)
    {
        super(_config);
    }

    /**
     * Gets the pay doc.
     *
     * @return the pay doc
     */
    protected ReportKey getReportKey()
    {
        return EnumUtils.getEnum(ReportKey.class, getConfig().getProperty("ReportKey", "IN"));
    }

    /**
     * Gets the duration type.
     *
     * @return the duration type
     */
    protected String getDurationType()
    {
        return getConfig().getProperty("DurationType", "YEARS");
    }

    /**
     * Gets the duration.
     *
     * @return the duration
     * @throws EFapsException on error
     */
    protected Integer getDuration()
        throws EFapsException
    {
        return Integer.parseInt(getConfig().getProperty("Duration", "10"));
    }

    /**
     * Gets the quantiy.
     *
     * @return the quantiy
     * @throws EFapsException on error
     */
    protected Integer getCount()
        throws EFapsException
    {
        return Integer.parseInt(getConfig().getProperty("Count", "10"));
    }

    @Override
    public Properties getConfig()
    {
        final Properties ret = super.getConfig();
        if (!ret.containsKey("QueryBldrConfig")) {
            try {
                ret.put("QueryBldrConfig", Sales.getSysConfig().getUUID().toString());
            } catch (final CacheReloadException e) {
                SalesReport4AccountPanel_Base.LOG.error("Cached error", e);
            }
        }
        if (!ret.containsKey("QueryBldrConfigAttribute")) {
            final String attr = ret.getProperty("ReportKey", "IN").equals("IN") ? Sales.REPORT_SALES4ACCOUNTIN.getKey()
                            : Sales.REPORT_SALES4ACCOUNTOUT.getKey();
            ret.put("QueryBldrConfigAttribute", attr);
        }
        return ret;
    }

    @Override
    public CharSequence getHtmlSnipplet()
        throws EFapsBaseException
    {
        final CharSequence ret;
        if (isCached()) {
            ret = getFromCache();
        } else {
            final SalesReport4Account report = new SalesReport4Account()
            {

                @Override
                protected ReportKey getReportKey()
                {
                    return SalesReport4AccountPanel_Base.this.getReportKey();
                }

                @Override
                public boolean isCached(final Parameter _parameter)
                    throws EFapsException
                {
                    return false;
                }

                @Override
                public Map<String, Object> getFilterMap(final Parameter _parameter)
                    throws EFapsException
                {
                    final Map<String, Object> ret = new HashMap<>();
                    switch (getDurationType()) {
                        case "YEARS":
                            ret.put("dateFrom", new DateTime().minusYears(getDuration()));
                            break;
                        case "MONTHS":
                            ret.put("dateFrom", new DateTime().minusMonths(getDuration()));
                            break;
                        case "WEEKS":
                            ret.put("dateFrom", new DateTime().minusWeeks(getDuration()));
                            break;
                        case "DAYS":
                            ret.put("dateFrom", new DateTime().minusDays(getDuration()));
                            break;
                        default:
                            ret.put("dateFrom", new DateTime().minusYears(1));
                            break;
                    }
                    ret.put("switch", true);
                    return ret;
                }
            };

            final Parameter parameter = ParameterUtil.instance();
            for (final Entry<Object, Object> entry : getConfig().entrySet()) {
                ParameterUtil.setProperty(parameter, (String) entry.getKey(), (String) entry.getValue());
            }
            final DynReport dynReport = new DynReport(report);
            final JRMapCollectionDataSource source = dynReport.getDataSource(parameter);

            final Map<String, BigDecimal> values = new HashMap<>();
            for (final Map<String, ?> data : source.getData()) {
                final String docContactName = (String) data.get("docContactName");
                BigDecimal amount;
                if (values.containsKey(docContactName)) {
                    amount = values.get(docContactName);
                } else {
                    amount = BigDecimal.ZERO;
                }

                final BigDecimal total = (BigDecimal) data.get("crossTotal_BASE");
                if (total != null) {
                    amount = amount.add(total);
                }
                final BigDecimal payed = (BigDecimal) data.get("payed_BASE");
                if (payed != null) {
                    amount = amount.subtract(payed);
                }
                values.put(docContactName, amount);
            }

            final Comparator<Map.Entry<String, BigDecimal>> byMapValues = (_left,
                                                                           _right) -> _right.getValue().compareTo(_left
                                                                                           .getValue());

            // create a list of map entries
            final List<Map.Entry<String, BigDecimal>> sorted = new ArrayList<>();

            // add all candy bars
            sorted.addAll(values.entrySet());
            Collections.sort(sorted, byMapValues);

            final DecimalFormat format = NumberFormatter.get().getTwoDigitsFormatter();
            int y = getCount();
            final BarsChart chart = new BarsChart()
            {

                @Override
                protected void configurePlot(final org.efaps.esjp.ui.html.dojo.charting.Plot _plot)
                {
                    super.configurePlot(_plot);
                    _plot.addConfig("labelFunc", "function labelFunc(value, fixed, precision) {\n"
                                    + "    return value.label;\n" + "}");
                    _plot.addConfig("labels", true);
                };
            }.setWidth(getWidth()).setHeight(getHeight()).setGap(2);

            final String title = getTitle();
            if (title != null && !title.isEmpty()) {
                chart.setTitle(getTitle());
            }
            chart.setOrientation(Orientation.CHART_ONLY);

            // add y-Axis manual to change the label for the axis
            final List<Map<String, Object>> labels = new ArrayList<>();
            for (int i = 1; i < y + 1; i++) {
                final Map<String, Object> map = new HashMap<>();
                map.put("value", i);
                map.put("text", StringEscapeUtils.escapeEcmaScript((y + 1 - i) + "."));
                labels.add(map);
            }
            chart.addAxis(new Axis().setName("y").setVertical(true).setMin(0).setLabels(Util.mapCollectionToObjectArray(
                            labels)));

            final Serie<Data> serie = new Serie<>();
            chart.addSerie(serie);
            boolean added = false;
            for (final Map.Entry<String, BigDecimal> entry : sorted) {
                final Data dataTmp = new Data().setSimple(false);
                serie.addData(dataTmp);
                dataTmp.setXValue(y);
                dataTmp.setYValue(entry.getValue().abs());
                dataTmp.addConfig("label", "\"" + StringEscapeUtils.escapeEcmaScript(entry.getKey()) + "\"");
                dataTmp.setTooltip(entry.getKey() + " - " + format.format(entry.getValue().abs()));
                y--;
                if (y < 1) {
                    final Data dataTmp2 = new Data().setSimple(false);
                    serie.addData(dataTmp2);
                    dataTmp2.setXValue(0);
                    dataTmp2.setYValue(0);
                    added = true;
                    break;
                }
            }
            if (!added) {
                final Data dataTmp2 = new Data().setSimple(false);
                serie.addData(dataTmp2);
                dataTmp2.setXValue(0);
                dataTmp2.setYValue(0);
            }
            ret = chart.getHtmlSnipplet();
            cache(ret);
        }
        return ret;
    }

    @Override
    public boolean isVisible()
        throws EFapsBaseException
    {
        return true;
    }

    /**
     * The Class DynReport.
     */
    public static class DynReport
        extends DynReport4Account
    {

        /**
         * Instantiates a new dyn report.
         *
         * @param _report4Account the report 4 account
         */
        public DynReport(final SalesReport4Account_Base _report4Account)
        {
            super(_report4Account);
        }

        /**
         * Gets the data source.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the data source
         * @throws EFapsException on error
         */
        public JRMapCollectionDataSource getDataSource(final Parameter _parameter)
            throws EFapsException
        {
            return (JRMapCollectionDataSource) super.createDataSource(_parameter);
        }

        @Override
        protected boolean isShowSwapInfo()
        {
            return false;
        }

        @Override
        protected boolean isCurrencyBase(final Parameter _parameter)
            throws EFapsException
        {
            return true;
        }
    }
}
