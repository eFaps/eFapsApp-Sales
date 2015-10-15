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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.api.ui.IEsjpSnipplet;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.common.dashboard.AbstractDashboardPanel;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.RateInfo;
import org.efaps.esjp.ui.html.dojo.charting.BarsChart;
import org.efaps.esjp.ui.html.dojo.charting.Data;
import org.efaps.esjp.ui.html.dojo.charting.Orientation;
import org.efaps.esjp.ui.html.dojo.charting.Serie;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * The Class Sales4ContactPanel_Base.
 *
 * @author The eFaps Team
 */
@EFapsUUID("8e4aae87-ac9e-441c-9995-9698ddc4c2f2")
@EFapsApplication("eFapsApp-Sales")
public abstract class Sales4ContactPanel_Base
    extends AbstractDashboardPanel
    implements IEsjpSnipplet
{

    /**
    *
    */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new sales4 contact panel.
     */
    public Sales4ContactPanel_Base()
    {
        super();
    }

    /**
     * Instantiates a new sales4 contact panel_ base.
     *
     * @param _config the _config
     */
    public Sales4ContactPanel_Base(final String _config)
    {
        super(_config);
    }

    /**
     * Gets the height.
     *
     * @return the height
     */
    protected Integer getMonth()
    {
        return Integer.valueOf(getConfig().getProperty("Month", "6"));
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

    /**
     * Gets the currency inst.
     *
     * @return the currency inst
     * @throws EFapsException on error
     */
    protected String getTotal()
        throws EFapsException
    {
        return getConfig().getProperty("Total", "NET");
    }

    @Override
    public CharSequence getHtmlSnipplet()
        throws EFapsException
    {
        final StringBuilder ret = new StringBuilder();
        final Parameter parameter = new Parameter();
        final DateTime start = new DateTime().withTimeAtStartOfDay().minusMonths(getMonth());
        final DateTime end = new DateTime().withTimeAtStartOfDay().plusDays(1);

        final Map<Instance, BigDecimal> values = new HashMap<>();
        final Map<Instance, String> contacts = new HashMap<>();

        final QueryBuilder queryBldr = AbstractCommon.getQueryBldrFromProperties(getConfig());

        queryBldr.addWhereAttrGreaterValue(CISales.DocumentSumAbstract.Date, start);
        queryBldr.addWhereAttrLessValue(CISales.DocumentSumAbstract.Date, end);
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selContactInst = SelectBuilder.get().linkto(CISales.DocumentSumAbstract.Contact).instance();
        final SelectBuilder selContactName = SelectBuilder.get().linkto(CISales.DocumentSumAbstract.Contact)
                        .attribute(CIContacts.Contact.Name);
        multi.addSelect(selContactInst, selContactName);
        multi.addAttribute(CISales.DocumentSumAbstract.NetTotal, CISales.DocumentSumAbstract.CrossTotal,
                        CISales.DocumentSumAbstract.Date);
        multi.execute();
        while (multi.next()) {
            final DateTime date = multi.getAttribute(CISales.DocumentSumAbstract.Date);
            final Instance contactInst = multi.getSelect(selContactInst);

            final RateInfo rateInfo = new Currency().evaluateRateInfo(parameter, date, getCurrencyInst());

            BigDecimal currentValue = multi.<BigDecimal>getAttribute(getTotal().equals("NET")
                            ? CISales.DocumentSumAbstract.NetTotal
                                            :  CISales.DocumentSumAbstract.CrossTotal);

            currentValue = currentValue.multiply(rateInfo.getRate()).setScale(2, BigDecimal.ROUND_HALF_UP);

            BigDecimal val;
            if (values.containsKey(contactInst)) {
                val = values.get(contactInst);
            } else {
                val = BigDecimal.ZERO;
            }
            values.put(contactInst, val.add(currentValue));
            if (!contacts.containsKey(contactInst)) {
                contacts.put(contactInst,  multi.<String>getSelect(selContactName));
            }
        }
        final Comparator<Map.Entry<Instance, BigDecimal>> byMapValues
            = new Comparator<Map.Entry<Instance, BigDecimal>>()
            {
                @Override
                public int compare(final Map.Entry<Instance, BigDecimal> _left,
                                   final Map.Entry<Instance, BigDecimal> _right)
                {
                    return _right.getValue().compareTo(_left.getValue());
                }
            };
        // create a list of map entries
        final List<Map.Entry<Instance, BigDecimal>> sorted = new ArrayList<>();

        // add all candy bars
        sorted.addAll(values.entrySet());
        Collections.sort(sorted, byMapValues);

        int y = getCount();
        final BarsChart chart = new BarsChart()
            {
                @Override
                protected void configurePlot(final org.efaps.esjp.ui.html.dojo.charting.Plot _plot)
                {
                    super.configurePlot(_plot);
                    _plot.addConfig("labelFunc", "function labelFunc(value, fixed, precision) {\n"
                                    + "    return value.label;\n"
                                    + "}");
                    _plot.addConfig("labels", true);
                };
            }.setWidth(getWidth()).setHeight(getHeight()).setGap(2);

        chart.setTitle(getTitle());
        chart.setOrientation(Orientation.CHART_ONLY);

        final Serie<Data> serie = new Serie<Data>();
        chart.addSerie(serie);
        boolean added = false;
        for (final Map.Entry<Instance, BigDecimal> entry : sorted) {
            final Data dataTmp = new Data().setSimple(false);
            serie.addData(dataTmp);
            dataTmp.setXValue(y);
            dataTmp.setYValue(entry.getValue());
            dataTmp.addConfig("label", "\"" + StringEscapeUtils.escapeEcmaScript(contacts.get(entry.getKey())) + "\"");
            dataTmp.setTooltip(contacts.get(entry.getKey()) + " - " + entry.getValue());
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
        ret.append(chart.getHtmlSnipplet());
        return ret;
    }

    @Override
    public boolean isVisible()
        throws EFapsException
    {
        return true;
    }
}
