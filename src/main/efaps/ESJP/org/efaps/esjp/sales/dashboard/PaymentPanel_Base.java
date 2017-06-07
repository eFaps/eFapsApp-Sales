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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.EnumUtils;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.api.ui.IEsjpSnipplet;
import org.efaps.db.Instance;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.common.dashboard.AbstractDashboardPanel;
import org.efaps.esjp.common.datetime.JodaTimeUtils;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.erp.AbstractGroupedByDate_Base.DateGroup;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.erp.RateInfo;
import org.efaps.esjp.sales.report.PaymentSumReport;
import org.efaps.esjp.sales.report.PaymentSumReport_Base.DataBean;
import org.efaps.esjp.sales.report.PaymentSumReport_Base.DynPaymentSumReport;
import org.efaps.esjp.sales.report.PaymentSumReport_Base.PayDoc;
import org.efaps.esjp.ui.html.dojo.charting.Axis;
import org.efaps.esjp.ui.html.dojo.charting.ColumnsChart;
import org.efaps.esjp.ui.html.dojo.charting.Data;
import org.efaps.esjp.ui.html.dojo.charting.Orientation;
import org.efaps.esjp.ui.html.dojo.charting.PlotLayout;
import org.efaps.esjp.ui.html.dojo.charting.Serie;
import org.efaps.esjp.ui.html.dojo.charting.Util;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * The Class PaymentPanel_Base.
 *
 * @author The eFaps Team
 */
@EFapsUUID("9a493043-0ce3-4c36-959b-f4712c62429c")
@EFapsApplication("eFapsApp-Sales")
public abstract class PaymentPanel_Base
    extends AbstractDashboardPanel
    implements IEsjpSnipplet
{

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new sales panel_ base.
     */
    public PaymentPanel_Base()
    {
        super();
    }

    /**
     * Instantiates a new sales panel_ base.
     *
     * @param _config the _config
     */
    public PaymentPanel_Base(final String _config)
    {
        super(_config);
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
        return Instance.get(getConfig().getProperty("CurrencyOID", Currency.getBaseCurrency().getOid()));
    }

    /**
     * Checks if is filter.
     *
     * @return true, if is filter
     */
    protected boolean isFilter()
    {
        return BooleanUtils.toBoolean(getConfig().getProperty("Filter4Currency", "true"));
    }

    /**
     * Gets the height.
     *
     * @return the height
     */
    protected Integer getQuantity()
    {
        return Integer.valueOf(getConfig().getProperty("Quantity", "6"));
    }

    /**
     * Gets the pay doc.
     *
     * @return the pay doc
     */
    protected PayDoc getPayDoc()
    {
        return EnumUtils.getEnum(PayDoc.class, getConfig().getProperty("PayDoc", "BOTH"));
    }

    /**
     * Gets the date group.
     *
     * @return the date group
     */
    protected DateGroup getDateGroup()
    {
        return EnumUtils.getEnum(DateGroup.class, getConfig().getProperty("DateGroup", "MONTH"));
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

    @Override
    public CharSequence getHtmlSnipplet()
        throws EFapsException
    {
        final CharSequence ret;
        if (isCached()) {
            ret = getFromCache();
        } else {
            final Instance currencyInst = getCurrencyInst();
            final boolean currencyFilter = isFilter();

            final Map<String, Serie<Data>> series = new TreeMap<>();
            final Map<String, Map<String, BigDecimal>> values = new TreeMap<>();
            final Map<String, BigDecimal> totals = new HashMap<>();
            final JRBeanCollectionDataSource source = new DynDataBean(this).getData();
            for (final Object data: source.getData()) {
                final DataBean bean = (DataBean) data;
                final BigDecimal amount;
                if (bean.getRateCurrencyInst().equals(currencyInst)) {
                    amount = bean.getAmount();
                } else {
                    if (currencyFilter) {
                        amount = BigDecimal.ZERO;
                    } else {
                        RateInfo rateinfo = RateInfo.getRateInfo(bean.getRate());
                        if (rateinfo.getCurrencyInstance().equals(Currency.getBaseCurrency())
                                      && rateinfo.getCurrencyInstance().equals(rateinfo.getTargetCurrencyInstance()) ) {
                            rateinfo = new Currency().evaluateRateInfos(ParameterUtil.instance(), bean.getDate(),
                                            rateinfo.getCurrencyInstance(), currencyInst)[2];
                        }
                        amount = Currency.convertToCurrency(ParameterUtil.instance(), bean.getAmount(),
                                        rateinfo, null, currencyInst);
                    }
                }
                if (amount.compareTo(BigDecimal.ZERO) != 0) {

                    final Map<String, BigDecimal> map;
                    if (values.containsKey(bean.getPartial())) {
                        map = values.get(bean.getPartial());
                    } else {
                        map = new TreeMap<>();
                        values.put(bean.getPartial(), map);
                    }
                    final BigDecimal val;
                    if (map.containsKey(bean.getType())) {
                        val = map.get(bean.getType());
                    } else {
                        val = BigDecimal.ZERO;
                    }
                    map.put(bean.getType(), val.add(amount));

                    final BigDecimal total = totals.containsKey(bean.getPartial())
                                    ? totals.get(bean.getPartial()) : BigDecimal.ZERO;
                    totals.put(bean.getPartial(), total.add(amount));

                    if (!series.containsKey(bean.getType())) {
                        final Serie<Data> serie = new Serie<>();
                        serie.setName(bean.getType());
                        series.put(bean.getType(), serie);
                    }
                }
            }

            final ColumnsChart chart = new ColumnsChart().setPlotLayout(PlotLayout.STACKED)
                            .setGap(5).setWidth(getWidth()).setHeight(getHeight());
            chart.getPlots().get("default").addConfig("stroke", "{color: \"grey\", width: 1}");
            final String title = getTitle();
            if (title != null && !title.isEmpty()) {
                chart.setTitle(getTitle());
            }
            chart.setOrientation(Orientation.VERTICAL_CHART_LEGEND);
            final Axis xAxis = new Axis().setName("x").setMinorTicks(false);
            chart.addAxis(xAxis);

            final List<Map<String, Object>> labels = new ArrayList<>();

            int x = 1;
            final DecimalFormat fmtr = NumberFormatter.get().getTwoDigitsFormatter();
            for (final Entry<String, Map<String, BigDecimal>> entry : values.entrySet()) {
                final Map<String, Object> map = new HashMap<>();
                map.put("value", x);
                map.put("text", Util.wrap4String(entry.getKey()));
                labels.add(map);

                for (final Entry<String, Serie<Data>> serieEntry : series.entrySet()) {
                    if (x == 1) {
                        chart.addSerie(serieEntry.getValue());
                    }
                    final BigDecimal value;
                    if (entry.getValue().containsKey(serieEntry.getKey())) {
                        value = entry.getValue().get(serieEntry.getKey());
                    } else {
                        value = BigDecimal.ZERO;
                    }
                    final Data dataTmp = new Data().setSimple(false);
                    dataTmp.setXValue(null);
                    dataTmp.setYValue(value.intValue());
                    final StringBuilder toolTip = new StringBuilder().append(fmtr.format(value))
                                    .append("/").append(fmtr.format(totals.get(entry.getKey())))
                                    .append(" ").append(serieEntry.getValue().getName());

                    dataTmp.setTooltip(toolTip.toString());
                    serieEntry.getValue().addData(dataTmp);
                }
                x++;
            }
            if (!labels.isEmpty()) {
                xAxis.setLabels(Util.mapCollectionToObjectArray(labels));
            }

            ret = chart.getHtmlSnipplet();
            cache(ret);
        }
        return ret;
    }

    @Override
    public boolean isVisible()
        throws EFapsException
    {
        return true;
    }

    /**
     * The Class DynDataBean.
     *
     */
    public static class DynDataBean
        extends DynPaymentSumReport
    {

        /** The panel. */
        private final PaymentPanel_Base panel;

        /**
         * Instantiates a new dyn data bean.
         *
         * @param _panel the panel
         */
        public DynDataBean(final PaymentPanel_Base _panel)
        {
            super(new PaymentSumReport() {
                @Override
                public boolean isCached(final Parameter _parameter)
                    throws EFapsException
                {
                    return false;
                }
            });
            this.panel = _panel;
        }

        /**
         * Gets the data.
         *
         * @return the data
         * @throws EFapsException on error
         */
        protected JRBeanCollectionDataSource getData()
            throws EFapsException
        {
            return (JRBeanCollectionDataSource) super.createDataSource(ParameterUtil.instance());
        }

        @Override
        protected Properties getProperties(final Parameter _parameter)
            throws EFapsException
        {
            return this.panel.getConfig();
        }

        @Override
        protected PayDoc getPayDoc(final Parameter _parameter)
            throws EFapsException
        {
            return this.panel.getPayDoc();
        }

        @Override
        protected DateGroup getDateGroup(final Parameter _parameter)
            throws EFapsException
        {
            return this.panel.getDateGroup();
        }

        @Override
        protected void add2QueryBuilder(final Parameter _parameter,
                                        final QueryBuilder _queryBldr)
            throws EFapsException
        {
            final DateTime dateFrom;
            switch (this.panel.getDateGroup()) {
                case YEAR:
                    dateFrom = this.panel.isDateGroupStart()
                        ? new DateTime().minusYears(this.panel.getQuantity()).withDayOfYear(1)
                                        : new DateTime().minusYears(this.panel.getQuantity());
                    break;
                case HALFYEAR:
                    final DateTime hyDate = new DateTime().withFieldAdded(JodaTimeUtils.halfYears(),
                                    -this.panel.getQuantity());
                    if (this.panel.isDateGroupStart()) {
                        dateFrom = hyDate.getMonthOfYear() < 7
                                        ? hyDate.withDayOfMonth(1).withMonthOfYear(1)
                                        : hyDate.withDayOfMonth(1).withMonthOfYear(7);
                    } else {
                        dateFrom = hyDate;
                    }
                    break;
                case QUARTER:
                    final DateTime qDate =  new DateTime().withFieldAdded(JodaTimeUtils.quarters(),
                                    -this.panel.getQuantity());
                    if (this.panel.isDateGroupStart()) {
                        if (qDate.getMonthOfYear() < 4) {
                            dateFrom = qDate.withDayOfMonth(1).withMonthOfYear(1);
                        } else if (qDate.getMonthOfYear() < 7) {
                            dateFrom = qDate.withDayOfMonth(1).withMonthOfYear(4);
                        } else if (qDate.getMonthOfYear() < 10) {
                            dateFrom = qDate.withDayOfMonth(1).withMonthOfYear(7);
                        } else {
                            dateFrom = qDate.withDayOfMonth(1).withMonthOfYear(10);
                        }
                    } else {
                        dateFrom = qDate;
                    }
                    break;
                case WEEK:
                    dateFrom = this.panel.isDateGroupStart()
                            ? new DateTime().minusWeeks(this.panel.getQuantity()).withDayOfWeek(1)
                                            : new DateTime().minusWeeks(this.panel.getQuantity());
                    break;
                case DAY:
                    dateFrom = new DateTime().minusDays(this.panel.getQuantity());
                    break;
                case MONTH:
                default:
                    dateFrom = this.panel.isDateGroupStart() ? new DateTime().minusMonths(this.panel.getQuantity())
                                    .withDayOfMonth(1) : new DateTime().minusMonths(this.panel.getQuantity());
                    break;
            }
            _queryBldr.addWhereAttrGreaterValue(CIERP.DocumentAbstract.Date, dateFrom.withTimeAtStartOfDay()
                            .minusMinutes(1));
        }
    }
}
