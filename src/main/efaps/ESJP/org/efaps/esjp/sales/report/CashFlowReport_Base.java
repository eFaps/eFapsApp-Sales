/*
 * Copyright 2003 - 2013 The eFaps Team
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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabMeasureBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabRowGroupBuilder;
import net.sf.dynamicreports.report.constant.Calculation;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.jasperreports.engine.JRDataSource;

import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("67b5c710-4e36-4eb2-95c0-8f04b15500ec")
@EFapsRevision("$Rev$")
public abstract class CashFlowReport_Base
{

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(CashFlowReport.class);

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
        dyRp.setFileName(DBProperties.getProperty(CashFlowReport.class.getName() + ".FileName"));
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
     * @return the report class
     * @throws EFapsException on error
     */
    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new CFReport();
    }

    /**
     * Report class.
     */
    public static class CFReport
        extends AbstractDynamicReport
    {

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final DRDataSource dataSource = new DRDataSource("group", "category", "date", "amount");
            final Map<String, Map<String, BigDecimal>> rowMap = new TreeMap<String, Map<String, BigDecimal>>();
            final Map<Instance, Instance> inst2inst = new HashMap<Instance, Instance>();
            final QueryBuilder queryBldr = new QueryBuilder(CISales.Payment);
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selCreateInst = SelectBuilder.get().linkto(CISales.Payment.CreateDocument).instance();
            multi.addSelect(selCreateInst);
            multi.execute();
            while (multi.next()) {
                final Instance payInst = multi.getCurrentInstance();
                final Instance docInst = multi.getSelect(selCreateInst);
                inst2inst.put(payInst, docInst);
            }

            final QueryBuilder queryBldr2 = new QueryBuilder(CISales.TransactionAbstract);
            final SelectBuilder selPaymentInst = SelectBuilder.get()
                            .linkto(CISales.TransactionAbstract.Payment).instance();
            final MultiPrintQuery multi2 = queryBldr2.getPrint();
            multi2.addSelect(selPaymentInst);
            multi2.addAttribute(CISales.TransactionAbstract.Amount, CISales.TransactionAbstract.Date);
            multi2.execute();
            while (multi2.next()) {
                final DateTime date = multi2.<DateTime>getAttribute(CISales.TransactionAbstract.Date);
                BigDecimal amount = multi2.<BigDecimal>getAttribute(CISales.TransactionAbstract.Amount);
                final Instance payInst = multi2.<Instance>getSelect(selPaymentInst);

                final boolean out = multi2.getCurrentInstance().getType().isKindOf(
                                CISales.TransactionOutbound.getType());

                final String category = getCategory(_parameter, out, payInst, inst2inst.get(payInst));
                final String dateStr = getDateStr(_parameter, date);
                Map<String, BigDecimal> map;
                if (rowMap.containsKey(category)) {
                    map = rowMap.get(category);
                } else {
                    map = new HashMap<String, BigDecimal>();
                    rowMap.put(category, map);
                }
                BigDecimal amountTmp;
                if (map.containsKey(dateStr)) {
                    amountTmp = map.get(dateStr);
                } else {
                    amountTmp = BigDecimal.ZERO;
                }
                if (out) {
                    amount = amount.negate();
                }
                map.put(dateStr, amountTmp.add(amount));
            }

            for (final Entry<String, Map<String, BigDecimal>> entryRow : rowMap.entrySet()) {
                for (final Entry<String, BigDecimal> entryCol : entryRow.getValue().entrySet()) {
                    final String prodName = entryCol.getKey();
                    final BigDecimal quantity = entryCol.getValue();
                    dataSource.add(entryRow.getKey(), prodName, quantity);
                }
            }

            return dataSource;
        }


        /**
         * @param _parameter Parameter as passed by the eFaps API
         * @param _date      DateTime the String is wanted for
         * @return String for Date
         * @throws EFapsException on error
         */
        protected String getDateStr(final Parameter _parameter,
                                    final DateTime _date)
            throws EFapsException
        {
            final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
            final String pattern;
            if (props.containsKey("Pattern4Date")) {
                pattern = (String) props.get("Pattern4Date");
            } else {
                pattern = "MM/yyyy";
            }
            final DateTimeFormatter formatter = DateTimeFormat.forPattern(pattern)
                            .withLocale(Context.getThreadContext().getLocale());

            return _date.toString(formatter);
        }


        /**
         * @param _parameter    Parameter as passed by the eFaps API
         * @param _out          outgoing or not
         * @param _payInst      Instance of the Payment
         * @param _docInst      Instance of the Document
         * @return name of the category
         * @throws EFapsException on error
         */
        protected String getCategory(final Parameter _parameter,
                                     final boolean _out,
                                     final Instance _payInst,
                                     final Instance _docInst)
            throws EFapsException
        {
            String ret;
            if (_docInst == null) {
                ret = "4. Finanzas";
            } else if (_docInst.isValid() && _docInst.getType().isKindOf(CISales.Invoice.getType())){
                ret = "1. Venta";
            } else if (_docInst.isValid() && _docInst.getType().isKindOf(CISales.IncomingInvoice.getType())){
                ret = "2. Compra";
            } else if (_docInst.isValid() && _docInst.getType().isKindOf(CISales.IncomingCredit.getType())){
                ret = "5. Créditos";
                //Payroll_Payslip
            } else if (_docInst.isValid() && _docInst.getType().getUUID().equals(
                            UUID.fromString("a298d361-7530-4a24-b69f-ff3a1186a081"))){
                ret = "3. Sueldos";
            } else {
                ret = "6. Otros";
            }
            return ret;
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final CrosstabBuilder crosstab = DynamicReports.ctab.crosstab();
            final CrosstabMeasureBuilder<BigDecimal> amountMeasure = DynamicReports.ctab.measure("amount",
                            BigDecimal.class, Calculation.SUM);
            final CrosstabRowGroupBuilder<String> rowGroup = DynamicReports.ctab.rowGroup("group", String.class);
            final CrosstabRowGroupBuilder<String> rowCategory = DynamicReports.ctab.rowGroup("category", String.class);
            final CrosstabColumnGroupBuilder<String> columnGroup = DynamicReports.ctab
                            .columnGroup("date", String.class);

            crosstab.headerCell(DynamicReports.cmp.text(DBProperties
                            .getProperty(CashFlowReport.class.getName() + ".HeaderCell"))
                            .setStyle(DynamicReports.stl.style().setBold(true)))
                            .rowGroups(rowGroup, rowCategory)
                            .columnGroups(columnGroup)
                            .measures(amountMeasure);
            _builder.summary(crosstab);
        }

    }
}
