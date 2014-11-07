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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.jasperreports.engine.JRDataSource;

import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Checkin;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.sales.document.AbstractDocumentTax;
import org.efaps.esjp.sales.document.AbstractDocumentTax_Base.DocTaxInfo;
import org.efaps.esjp.sales.document.RetentionCertificate;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Report used to analyze retention for documents by grouping them by month and
 * contact.
 *
 * @author The eFaps Team
 * @version $Id: RetentionCertificateReport_Base.java 14072 2014-09-19 17:33:57Z
 *          m.aranya@moxter.net $
 */
@EFapsUUID("b02e955e-4cb7-4c01-bec0-4bc69a8f8789")
@EFapsRevision("$Rev$")
public abstract class RetentionCertificateReport_Base
    extends FilteredReport
{

    /**
     * Logging instance used in this class.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(RetentionReport.class);

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
     * @param _parameter Parameter as passed from eFaps API.
     * @throws EFapsException on error
     * @return Return containing the report
     */
    public Return createReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final AbstractDynamicReport dyRp = getReport(_parameter);
        final File file = dyRp.getPDF(_parameter);
        ret.put(ReturnValues.VALUES, file);

        try {
            final FileInputStream in = new FileInputStream(file);
            final Checkin checkin = new Checkin(_parameter.getInstance());
            checkin.execute(file.getName(), in, Long.valueOf(file.length()).intValue());
        } catch (final FileNotFoundException e) {
            throw new EFapsException("FileNotFoundException", e);
        }
        return ret;
    }

    public Return createReportMultiple(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final AbstractDynamicReport dyRp = getReport(_parameter);
        final File file = dyRp.getExcel(_parameter);
        ret.put(ReturnValues.VALUES, file);
        ret.put(ReturnValues.TRUE, true);
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
        dyRp.setFileName(DBProperties.getProperty(RetentionReport.class.getName() + ".FileName"));
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
        return new DynRetentionCertificateReport(this);
    }

    /**
     * Report class.
     */
    public static class DynRetentionCertificateReport
        extends AbstractDynamicReport
    {

        /**
         * variable to report.
         */
        private final RetentionCertificateReport_Base filteredReport;

        /**
         * @param _report class used
         */
        public DynRetentionCertificateReport(final RetentionCertificateReport_Base _report)
        {
            this.filteredReport = _report;
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final DRDataSource dataSource = new DRDataSource("type", "name", "date", "rateNetTotal", "rateCrossTotal",
                            "rateCurrency", "taxAmount");

            final QueryBuilder attQueryBldr = new QueryBuilder(CISales.RetentionCertificate2IncomingRetention);
            attQueryBldr.addWhereAttrEqValue(CISales.RetentionCertificate2IncomingRetention.FromLink,
                            getInstances(_parameter));
            final AttributeQuery attQuery = attQueryBldr
                            .getAttributeQuery(CISales.RetentionCertificate2IncomingRetention.ToLink);

            final QueryBuilder relAttrQueryBldr = new QueryBuilder(CISales.IncomingRetention2IncomingInvoice);
            relAttrQueryBldr.addWhereAttrInQuery(CISales.IncomingRetention2IncomingInvoice.FromLink, attQuery);
            final AttributeQuery payAttrQuery = relAttrQueryBldr
                            .getAttributeQuery(CISales.IncomingRetention2IncomingInvoice.ToLink);

            final QueryBuilder queryBldr = new QueryBuilder(CISales.DocumentSumAbstract);
            queryBldr.addWhereAttrInQuery(CISales.DocumentSumAbstract.ID, payAttrQuery);
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selTypelabel = new SelectBuilder().type().label();
            final SelectBuilder selCurrencylabel = new SelectBuilder().linkto(
                            CISales.DocumentSumAbstract.RateCurrencyId).attribute(CIERP.Currency.Symbol);
            multi.addSelect(selTypelabel, selCurrencylabel);
            multi.addAttribute(CISales.DocumentSumAbstract.Name, CISales.DocumentSumAbstract.Date,
                            CISales.DocumentSumAbstract.RateCrossTotal, CISales.DocumentSumAbstract.RateNetTotal);
            multi.execute();

            while (multi.next()) {
                final DocTaxInfo taxdoc = AbstractDocumentTax.getDocTaxInfo(_parameter, multi.getCurrentInstance());
                dataSource.add(multi.getSelect(selTypelabel),
                                multi.getAttribute(CISales.DocumentSumAbstract.Name),
                                multi.<DateTime>getAttribute(CISales.DocumentSumAbstract.Date).toDate(),
                                multi.getAttribute(CISales.DocumentSumAbstract.RateNetTotal),
                                multi.getAttribute(CISales.DocumentSumAbstract.RateCrossTotal),
                                multi.getSelect(selCurrencylabel),
                                taxdoc.getTaxAmount());
            }
            return dataSource;
        }

        /**
         * @param _parameter
         * @return
         */
        protected Object[] getInstances(final Parameter _parameter)
            throws EFapsException
        {
            final List<Instance> listInstances = new ArrayList<Instance>();
            if (_parameter.getInstance() != null && _parameter.getInstance().isValid()) {
                listInstances.add(_parameter.getInstance());
            } else {
                final String[] others = (String[]) _parameter.get(ParameterValues.OTHERS);
                if (others != null) {
                    for (final String other : others) {
                        final Instance instance = Instance.get(other);
                        if (instance.isValid()) {
                            listInstances.add(instance);
                        }
                    }
                }
            }

            return listInstances.isEmpty() ? new Object[] { 0 } : listInstances.toArray();
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            _builder.addColumn(
                            DynamicReports.col.reportRowNumberColumn().setFixedColumns(3),
                            DynamicReports.col.column(DBProperties
                                            .getProperty("org.efaps.esjp.sales.document.RetentionCertificate.Type"),
                                            "type", DynamicReports.type.stringType()),
                            DynamicReports.col.column(DBProperties
                                            .getProperty("org.efaps.esjp.sales.document.RetentionCertificate.Name"),
                                            "name", DynamicReports.type.stringType()),
                            DynamicReports.col.column(DBProperties
                                            .getProperty("org.efaps.esjp.sales.document.RetentionCertificate.Date"),
                                            "date", DynamicReports.type.dateType()),
                            DynamicReports.col.column(
                                            DBProperties.getProperty(RetentionCertificate.class.getName()
                                                            + ".RateNetTotal"),
                                            "rateNetTotal", DynamicReports.type.bigDecimalType()),
                            DynamicReports.col.column(DBProperties
                                            .getProperty(RetentionCertificate.class.getName()
                                                            + ".RateCrossTotal"),
                                            "rateCrossTotal", DynamicReports.type.bigDecimalType()),
                            DynamicReports.col.column(DBProperties.getProperty(RetentionCertificate.class.getName()
                                            + ".RateCurrency"),
                                            "rateCurrency", DynamicReports.type.stringType())
                                            .setFixedColumns(3),
                            DynamicReports.col.column(DBProperties.getProperty(RetentionCertificate.class.getName()
                                            + ".TaxAmount"),
                                            "taxAmount", DynamicReports.type.bigDecimalType())
                                            .setFixedColumns(3));

        }

        /**
         * Getter method for the instance variable {@link #filteredReport}.
         *
         * @return value of instance variable {@link #filteredReport}
         */
        protected RetentionCertificateReport_Base getFilteredReport()
        {
            return this.filteredReport;
        }
    }
}
