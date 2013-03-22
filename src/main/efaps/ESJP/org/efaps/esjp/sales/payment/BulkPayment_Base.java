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


package org.efaps.esjp.sales.payment;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.subtotal.AggregationSubtotalBuilder;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.jasperreports.engine.JRDataSource;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.sales.document.AbstractDocument;
import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("929914ed-1511-4fb8-9cc8-7514811d3f74")
@EFapsRevision("$Rev$")
public abstract class BulkPayment_Base
    extends AbstractDocument
{

    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        // Sales-Configuration
        final Instance baseInst = SystemConfiguration.get(UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f"))
                        .getLink("CurrencyBase");
        final Insert insert = new Insert(CISales.BulkPayment);
        insert.add(CISales.BulkPayment.Date, _parameter.getParameterValue(CIFormSales.Sales_BulkPaymentForm.date.name));
        insert.add(CISales.BulkPayment.Name, _parameter.getParameterValue(CIFormSales.Sales_BulkPaymentForm.name.name));
        insert.add(CISales.BulkPayment.CrossTotal, BigDecimal.ZERO);
        insert.add(CISales.BulkPayment.CrossTotal, BigDecimal.ZERO);
        insert.add(CISales.BulkPayment.NetTotal, BigDecimal.ZERO);
        insert.add(CISales.BulkPayment.DiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.BulkPayment.RateCrossTotal, BigDecimal.ZERO);
        insert.add(CISales.BulkPayment.RateNetTotal, BigDecimal.ZERO);
        insert.add(CISales.BulkPayment.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.BulkPayment.CurrencyId, baseInst.getId());
        insert.add(CISales.BulkPayment.RateCurrencyId, baseInst.getId());
        insert.add(CISales.BulkPayment.Rate, new Object[] { 1, 1 });
        insert.add(CISales.BulkPayment.Status, Status.find(CISales.BulkPaymentStatus.uuid, "Open").getId());
        insert.execute();

        final Insert relinsert = new Insert(CISales.BulkPayment2Account);
        relinsert.add(CISales.BulkPayment2Account.FromLink, insert.getId());
        relinsert.add(CISales.BulkPayment2Account.ToLink,
                        _parameter.getParameterValue(CIFormSales.Sales_BulkPaymentForm.account4create.name));
        relinsert.execute();

        return new Return();
    }


    public Return getReport4Detail(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final AbstractDynamicReport dyRp = getReport(_parameter);
        dyRp.setFileName(CISales.BulkPayment.getType().getLabel());
        final String html = dyRp.getHtml(_parameter);
        ret.put(ReturnValues.SNIPLETT, html);
        return ret;
    }


    public Return exportReport4Detail(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String mime = (String) props.get("Mime");
        final AbstractDynamicReport dyRp = getReport(_parameter);
        dyRp.setFileName(CISales.BulkPayment.getType().getLabel());
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



    protected AbstractDynamicReport getReport(final Parameter _parameter)
    {
        return new Report4Detail();
    }

    public class Report4Detail
        extends AbstractDynamicReport
    {

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final DRDataSource ret = new DRDataSource("contact", "taxnumber", "docName", "amount");
            final List<Map<String, Object>> values = new ArrayList<Map<String, Object>>();

            // TODO add more status
            final QueryBuilder pdAttrQueryBldr = new QueryBuilder(CISales.PaymentDocumentOutAbstract);
            pdAttrQueryBldr.addWhereAttrNotEqValue(CISales.PaymentDocumentOutAbstract.StatusAbstract,
                            Status.find(CISales.PaymentDetractionOutStatus.uuid, "Canceled").getId());
            final AttributeQuery pdAttrQuery = pdAttrQueryBldr.getAttributeQuery(CISales.PaymentDocumentOutAbstract.ID);

            final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.BulkPayment2PaymentDocument);
            attrQueryBldr.addWhereAttrEqValue(CISales.BulkPayment2PaymentDocument.FromLink, _parameter.getInstance()
                            .getId());
            attrQueryBldr.addWhereAttrInQuery(CISales.BulkPayment2PaymentDocument.ToLink, pdAttrQuery);
            final AttributeQuery attrQuery = attrQueryBldr
                            .getAttributeQuery(CISales.BulkPayment2PaymentDocument.ToLink);

            final QueryBuilder queryBldr = new QueryBuilder(CISales.Payment);
            queryBldr.addWhereAttrInQuery(CISales.Payment.TargetDocument, attrQuery);
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selDocName = new SelectBuilder().linkto(CISales.Payment.CreateDocument).attribute(
                            CIERP.DocumentAbstract.Name);
            final SelectBuilder selContact = new SelectBuilder().linkto(CISales.Payment.TargetDocument)
                            .linkto(CISales.PaymentDocumentOutAbstract.Contact).attribute(
                                            CIContacts.Contact.Name);
            final SelectBuilder selTaxnumber = new SelectBuilder().linkto(CISales.Payment.TargetDocument)
                            .linkto(CISales.PaymentDocumentOutAbstract.Contact)
                            .clazz(CIContacts.ClassOrganisation)
                            .attribute(CIContacts.ClassOrganisation.TaxNumber);
            multi.addSelect(selDocName, selContact, selTaxnumber);
            multi.addAttribute(CISales.Payment.Amount);
            multi.execute();
            while (multi.next()) {
                final Map<String, Object> map = new HashMap<String, Object>();
                values.add(map);
                map.put("amount", multi.getAttribute(CISales.Payment.Amount));
                map.put("docName", multi.getSelect(selDocName));
                map.put("contact", multi.getSelect(selContact));
                map.put("taxnumber", multi.getSelect(selTaxnumber));
            }
            Collections.sort(values, new Comparator<Map<String, Object>>()
            {

                @Override
                public int compare(final Map<String, Object> _map,
                                   final Map<String, Object> _map1)
                {
                    return _map.get("contact").toString().compareTo(_map1.get("contact").toString());
                }
            });

            for (final Map<String, Object> map : values) {
                ret.add(map.get("contact"), map.get("taxnumber"), map.get("docName"), map.get("amount"));
            }

            return ret;
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final TextColumnBuilder<String> contactColumn  = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.payment.BulkPayment.Report4Detail.contact"),
                            "contact", DynamicReports.type.stringType()).setColumns(60);
            final TextColumnBuilder<String> taxnumberColumn  = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.payment.BulkPayment.Report4Detail.taxnumber"),
                            "taxnumber", DynamicReports.type.stringType());
            final TextColumnBuilder<String> docNameColumn  = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.payment.BulkPayment.Report4Detail.docName"),
                            "docName", DynamicReports.type.stringType());
            final TextColumnBuilder<BigDecimal> amountColumn = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.payment.BulkPayment.Report4Detail.amount"),
                            "amount", DynamicReports.type.bigDecimalType());

            final AggregationSubtotalBuilder<BigDecimal> subtotal = DynamicReports.sbt.sum(amountColumn);

            _builder.addColumn(contactColumn, taxnumberColumn, docNameColumn, amountColumn);
            _builder.addSubtotalAtColumnFooter(subtotal);
        }
    }
}
