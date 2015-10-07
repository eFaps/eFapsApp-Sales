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
import java.util.UUID;

import org.apache.commons.lang3.StringEscapeUtils;
import org.efaps.admin.common.MsgPhrase;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.api.ui.IEsjpSnipplet;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.common.dashboard.AbstractDashboardPanel;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.erp.RateInfo;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.ui.html.dojo.charting.BarsChart;
import org.efaps.esjp.ui.html.dojo.charting.Data;
import org.efaps.esjp.ui.html.dojo.charting.Orientation;
import org.efaps.esjp.ui.html.dojo.charting.Serie;
import org.efaps.util.EFapsException;
import org.efaps.util.UUIDUtil;
import org.joda.time.DateTime;

/**
 * The Class Employee2DocPanel_Base.
 *
 * @author The eFaps Team
 */
@EFapsUUID("13c3297d-02cf-4cf0-a971-b8c89bef4fab")
@EFapsApplication("eFapsApp-Sales")
public abstract class Employee2DocPanel_Base
    extends AbstractDashboardPanel
    implements IEsjpSnipplet
{

    /** */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new sales4 contact panel.
     */
    public Employee2DocPanel_Base()
    {
        super();
    }

    /**
     * Instantiates a new sales4 contact panel_ base.
     *
     * @param _config the _config
     */
    public Employee2DocPanel_Base(final String _config)
    {
        super(_config);
    }

    /**
     * Gets the month.
     *
     * @return the month
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
     * Gets the total.
     *
     * @return the total
     * @throws EFapsException on error
     */
    protected String getTotal()
        throws EFapsException
    {
        return getConfig().getProperty("Total", "NET");
    }

    /**
     * Gets the count.
     *
     * @return the count
     * @throws EFapsException on error
     */
    protected Integer getCount()
        throws EFapsException
    {
        return Integer.parseInt(getConfig().getProperty("Count", "10"));
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
        final Map<Instance, String> employees = new HashMap<>();

        final QueryBuilder queryBldr = AbstractCommon.getQueryBldrFromProperties(getConfig());

        queryBldr.addWhereAttrGreaterValue(CISales.DocumentSumAbstract.Date, start);
        queryBldr.addWhereAttrLessValue(CISales.DocumentSumAbstract.Date, end);
        final MultiPrintQuery multi = queryBldr.getPrint();
        // HumanResource_EmployeeMsgPhrase as default
        final String msgPhraseStr = Sales.EMPLOYEE2DOCREPORT.get().getProperty("EmployeeMsgPhrase",
                        "f543ca6d-29fb-4f1a-8747-0057b9a08404");
        final SelectBuilder selEmpl = SelectBuilder.get()
                        .linkfrom(CIHumanResource.Employee2DocumentAbstract.ToAbstractLink)
                        .linkto(CIHumanResource.Employee2DocumentAbstract.FromAbstractLink);
        final SelectBuilder selEmplInst = new SelectBuilder(selEmpl).instance();
        final MsgPhrase msgPhrase = UUIDUtil.isUUID(msgPhraseStr) ? MsgPhrase.get(UUID.fromString(msgPhraseStr))
                        : MsgPhrase.get(msgPhraseStr);
        multi.addMsgPhrase(selEmpl, msgPhrase);
        multi.addSelect(selEmplInst);
        multi.addAttribute(CISales.DocumentSumAbstract.NetTotal, CISales.DocumentSumAbstract.CrossTotal,
                        CISales.DocumentSumAbstract.Date);
        multi.execute();
        while (multi.next()) {
            final DateTime date = multi.getAttribute(CISales.DocumentSumAbstract.Date);
            final Instance emplInst = multi.getSelect(selEmplInst);

            final RateInfo rateInfo = new Currency().evaluateRateInfo(parameter, date, getCurrencyInst());

            BigDecimal currentValue = multi.<BigDecimal>getAttribute(getTotal().equals("NET")
                            ? CISales.DocumentSumAbstract.NetTotal : CISales.DocumentSumAbstract.CrossTotal);

            currentValue = currentValue.multiply(rateInfo.getRate()).setScale(2, BigDecimal.ROUND_HALF_UP);

            BigDecimal val;
            if (values.containsKey(emplInst)) {
                val = values.get(emplInst);
            } else {
                val = BigDecimal.ZERO;
            }
            values.put(emplInst, val.add(currentValue));
            if (!employees.containsKey(emplInst)) {
                employees.put(emplInst, multi.getMsgPhrase(selEmpl, msgPhrase));
            }
        }
        final Comparator<Map.Entry<Instance, BigDecimal>> byMapValues = new Comparator<
                        Map.Entry<Instance, BigDecimal>>()
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
                                    + "    return value.label;\n" + "}");
                    _plot.addConfig("labels", true);
                };
            }.setWidth(getWidth()).setHeight(getHeight()).setGap(2);

        chart.setTitle(getTitle());
        chart.setOrientation(Orientation.CHART_ONLY);

        final Serie<Data> serie = new Serie<Data>();
        chart.addSerie(serie);
        for (final Map.Entry<Instance, BigDecimal> entry : sorted) {
            final Data dataTmp = new Data().setSimple(false);
            serie.addData(dataTmp);
            dataTmp.setXValue(y);
            dataTmp.setYValue(entry.getValue());
            dataTmp.addConfig("label", "\"" + StringEscapeUtils.escapeEcmaScript(employees.get(entry.getKey())) + "\"");
            dataTmp.setTooltip(employees.get(entry.getKey()) + " - "
                            + NumberFormatter.get().getTwoDigitsFormatter().format(entry.getValue()));
            y--;
            if (y < 1) {
                final Data dataTmp2 = new Data().setSimple(false);
                serie.addData(dataTmp2);
                dataTmp2.setXValue(0);
                dataTmp2.setYValue(0);
                break;
            }
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
