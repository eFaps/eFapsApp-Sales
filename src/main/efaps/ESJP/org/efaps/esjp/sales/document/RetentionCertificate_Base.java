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

package org.efaps.esjp.sales.document;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.jasperreports.engine.JRDataSource;

import org.efaps.admin.datamodel.Attribute;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Checkin;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.esjp.sales.document.AbstractDocumentTax_Base.DocTaxInfo;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("02d5a390-516e-43d4-8d46-ca9c6599146a")
@EFapsRevision("$Rev$")
public abstract class RetentionCertificate_Base
    extends AbstractSumOnlyDocument
{

    /**
     * Logging instance used to give logging information of this class.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(RetentionCertificate_Base.class);

    protected static final String REQKEY =  RetentionCertificate.class.getName();

    /**
     * Method for create a new Quotation.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        createDoc(_parameter);
        return new Return();
    }

    /**
     * @param _parameter Parameter as passed from eFaps API.
     * @return List of instances
     * @throws EFapsException on error
     */
    public Return documentMultiPrint(final Parameter _parameter)
        throws EFapsException
    {
        final MultiPrint multi = new MultiPrint()
        {
            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.RetentionCertificate2PaymentRetentionOut);
                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(
                                CISales.RetentionCertificate2PaymentRetentionOut.ToLink);
                _queryBldr.addWhereAttrNotInQuery(CIERP.DocumentAbstract.ID, attrQuery);

                final QueryBuilder attrQueryBldr2 = new QueryBuilder(CISales.RetentionCertificate);
                attrQueryBldr2.addWhereAttrEqValue(CISales.RetentionCertificate.ID, _parameter.getInstance().getId());
                final AttributeQuery attrQuery2 = attrQueryBldr2
                                .getAttributeQuery(CISales.RetentionCertificate.Contact);

                final QueryBuilder attrQueryBldr3 = new QueryBuilder(CISales.PaymentRetentionOut);
                attrQueryBldr3.addWhereAttrInQuery(CISales.PaymentRetentionOut.Contact, attrQuery2);
                final AttributeQuery attrQuery3 = attrQueryBldr3.getAttributeQuery(CISales.PaymentRetentionOut.ID);

                _queryBldr.addWhereAttrInQuery(CIERP.DocumentAbstract.ID, attrQuery3);
            }
        };
        return multi.execute(_parameter);
    }


    public Return insertPostTrigger4Rel(final Parameter _parameter)
        throws EFapsException
    {
        final Map<?, ?> values = (HashMap<?, ?>) _parameter.get(ParameterValues.NEW_VALUES);
        final Attribute attr = _parameter.getInstance().getType().getAttribute("FromLink");
        updateSum(_parameter, Instance.get(CISales.RetentionCertificate.getType(),
                        (Long)((Object[]) values.get(attr))[0]));
        return new Return();
    }

    public Return updatePostTrigger4Rel(final Parameter _parameter)
        throws EFapsException
    {
        final Map<?, ?> values = (HashMap<?, ?>) _parameter.get(ParameterValues.NEW_VALUES);
        final Attribute attr = _parameter.getInstance().getType().getAttribute("FromLink");
        updateSum(_parameter, Instance.get(CISales.RetentionCertificate.getType(),
                        (Long)((Object[]) values.get(attr))[0]));
        return new Return();
    }

    public Return deletePreTrigger4Rel(final Parameter _parameter)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_parameter.getInstance());
        final SelectBuilder selCInst = SelectBuilder.get()
                        .linkto(CISales.RetentionCertificate2PaymentRetentionOut.FromLink)
                        .instance();
        print.addSelect(selCInst);
        print.execute();
        Context.getThreadContext().setRequestAttribute(REQKEY, print.getSelect(selCInst));
        return new Return();
    }

    public Return deletePostTrigger4Rel(final Parameter _parameter)
        throws EFapsException
    {
        updateSum(_parameter,  (Instance) Context.getThreadContext().getRequestAttribute(REQKEY));
        return new Return();
    }

    protected void updateSum(final Parameter _parameter,
                             final Instance _certInst)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CISales.RetentionCertificate2PaymentRetentionOut);
        queryBldr.addWhereAttrEqValue(CISales.RetentionCertificate2PaymentRetentionOut.FromLink, _certInst);
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder sel = SelectBuilder.get().linkto(CISales.RetentionCertificate2PaymentRetentionOut.ToLink)
                        .attribute(CISales.PaymentRetentionOut.Amount);
        multi.addSelect(sel);
        multi.execute();
        BigDecimal total = BigDecimal.ZERO;

        while (multi.next()) {
            total = total.add(multi.<BigDecimal>getSelect(sel));
        }
        if (_certInst != null && _certInst.isValid()) {
            final Update update = new Update(_certInst);
            update.add(CISales.RetentionCertificate.CrossTotal, total);
            update.execute();
        }
    }

    /**
     * Fieldvalue for the Detail of the RetentionCertificate.
     * @param _parameter Parameter as passed from eFaps API.
     * @throws EFapsException on error
     * @return Return containing html snipplet
     */
    public Return getDetailFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final AbstractDynamicReport dyRp = getReport(_parameter);
        ret.put(ReturnValues.SNIPLETT, dyRp.getHtmlSnipplet(_parameter));
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

    /**
     * @param _parameter Parameter as passed from eFaps API.
     * @throws EFapsException on error
     * @return new Report instance
     */
    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new RetentionCertificateReport();
    }

    /**
     * Report class.
     */
    public static class RetentionCertificateReport
        extends AbstractDynamicReport
    {

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final DRDataSource dataSource = new DRDataSource("type", "name", "date", "rateNetTotal", "rateCrossTotal",
                            "rateCurrency", "taxAmount");

            final QueryBuilder attQueryBldr = new QueryBuilder(CISales.RetentionCertificate2PaymentRetentionOut);
            attQueryBldr.addWhereAttrEqValue(CISales.RetentionCertificate2PaymentRetentionOut.FromLink, _parameter
                            .getInstance().getId());
            final AttributeQuery attQuery = attQueryBldr
                            .getAttributeQuery(CISales.RetentionCertificate2PaymentRetentionOut.ToLink);

            final QueryBuilder payAttrQueryBldr = new QueryBuilder(CISales.Payment);
            payAttrQueryBldr.addWhereAttrInQuery(CISales.Payment.TargetDocument, attQuery);
            final AttributeQuery payAttrQuery = payAttrQueryBldr.getAttributeQuery(CISales.Payment.CreateDocument);

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
                ;
                dataSource.add(multi.getSelect(selTypelabel),
                                multi.getAttribute(CISales.DocumentSumAbstract.Name),
                                multi.<DateTime>getAttribute(CISales.DocumentSumAbstract.Date).toDate(),
                                multi.getAttribute(CISales.DocumentSumAbstract.RateNetTotal),
                                multi.getAttribute(CISales.DocumentSumAbstract.RateCrossTotal),
                                multi.getSelect(selCurrencylabel),
                                taxdoc.getPaymentAmount());
            }
            return dataSource;
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
    }
}
