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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.sales.comparative.AbstractComparative;
import org.efaps.esjp.sales.comparative.ComparativeProvider;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabMeasureBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabRowGroupBuilder;
import net.sf.dynamicreports.report.constant.Calculation;
import net.sf.dynamicreports.report.constant.HorizontalAlignment;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.jasperreports.engine.JRDataSource;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 *
 */
@EFapsUUID("67ec3b10-e551-4bb3-b2a4-6df235e7583f")
@EFapsApplication("eFapsApp-Sales")
public abstract class ComparativeReport_Base
{

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
        dyRp.setFileName(DBProperties.getProperty(ComparativeReport.class.getName() + ".FileName"));
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
     * @return the report class
     * @throws EFapsException on error
     */
    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new ComparativeTableReport();
    }


    public static class ComparativeTableReport
        extends AbstractDynamicReport
    {

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final DRDataSource dataSource = new DRDataSource("type", "dimension", "link", "value", "comment");
            final List<Map<String, String>> values = new ArrayList<>();
            final QueryBuilder queryBldr = new QueryBuilder(CISales.ComparativeDetailAbstract);
            queryBldr.addWhereAttrEqValue(CISales.ComparativeDetailAbstract.ComparativeAbstractLink,
                            _parameter.getInstance());
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selDim = SelectBuilder.get().linkto(CISales.ComparativeDetailAbstract.DimensionLink)
                            .attribute(CISales.ComparativeDimensionAbstract.Name);
            multi.addSelect(selDim);
            multi.addAttribute(CISales.ComparativeDetailAbstract.Comment,
                            CISales.ComparativeDetailAbstract.AbstractLink,
                            CISales.ComparativeDetailAbstract.AbstractDateValue,
                            CISales.ComparativeDetailAbstract.AbstractDecimalValue,
                            CISales.ComparativeDetailAbstract.AbstractIntegerValue,
                            CISales.ComparativeDetailAbstract.AbstractStringValue);
            multi.execute();
            while (multi.next()) {
                final Map<String, String> map = new HashMap<>();
                values.add(map);
                final Type type = multi.getCurrentInstance().getType();
                map.put("type", type.getLabel());
                map.put("comment", multi.<String>getAttribute(CISales.ComparativeDetailAbstract.Comment));
                final AbstractComparative comp = getComparative(_parameter, multi.getCurrentInstance());
                final String link = comp.getValue4Link(_parameter,
                                multi.<Long>getAttribute(CISales.ComparativeDetailAbstract.AbstractLink));
                final String value = comp.getValue(_parameter,
                                multi.getCurrentInstance(),
                                multi.<DateTime>getAttribute(CISales.ComparativeDetailAbstract.AbstractDateValue),
                                multi.<BigDecimal>getAttribute(CISales.ComparativeDetailAbstract.AbstractDecimalValue),
                                multi.<Integer>getAttribute(CISales.ComparativeDetailAbstract.AbstractIntegerValue),
                                multi.<String>getAttribute(CISales.ComparativeDetailAbstract.AbstractStringValue));
                map.put("link", link);
                map.put("value", value);
                map.put("dimension", multi.<String>getSelect(selDim));
            }
            for (final Map<String, String> map : values) {
                dataSource.add(map.get("type"), map.get("dimension"), map.get("link"),
                                map.get("value"), map.get("comment"));
            }
            return dataSource;
        }

        protected AbstractComparative getComparative(final Parameter _parameter,
                                                     final Instance _instance)
        {
            return new ComparativeProvider();
        }


        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final CrosstabBuilder crosstab = DynamicReports.ctab.crosstab();
            final CrosstabRowGroupBuilder<String> rowGroup = DynamicReports.ctab.rowGroup("type", String.class)
                            .setShowTotal(false);
            final CrosstabRowGroupBuilder<String> dimGroup = DynamicReports.ctab.rowGroup("dimension", String.class)
                            .setShowTotal(false);

            final CrosstabMeasureBuilder<String> valueMeasure = DynamicReports.ctab.measure("value",
                            String.class, Calculation.NOTHING);
            valueMeasure.setHorizontalAlignment(HorizontalAlignment.CENTER);

            final CrosstabMeasureBuilder<String> commentMeasure = DynamicReports.ctab.measure("comment",
                            String.class, Calculation.NOTHING);

            final CrosstabColumnGroupBuilder<String> columnGroup = DynamicReports.ctab
                            .columnGroup("link", String.class).setShowTotal(false);

            crosstab.headerCell(DynamicReports.cmp.text(DBProperties
                            .getProperty(ComparativeReport.class.getName() + ".HeaderCell"))
                            .setStyle(DynamicReports.stl.style().setBold(true)))
                            .rowGroups(rowGroup, dimGroup)
                            .columnGroups(columnGroup)
                            .measures(valueMeasure, commentMeasure);
            _builder.summary(crosstab);
        }
    }
}
