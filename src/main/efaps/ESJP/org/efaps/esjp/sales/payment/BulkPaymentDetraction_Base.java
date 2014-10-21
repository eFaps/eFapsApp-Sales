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

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.subtotal.AggregationSubtotalBuilder;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.jasperreports.engine.JRDataSource;

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
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.sales.document.AbstractDocument;
import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("f58acffd-cd5b-4b24-b25f-92edfcbc651f")
@EFapsRevision("$Rev$")
public abstract class BulkPaymentDetraction_Base
    extends AbstractDocument
{

    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        createDoc(_parameter);
        return new Return();
    }

    /**
     * @param _parameter
     * @return
     */
    public CreatedDoc createDoc(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc ret = new CreatedDoc();
        final Instance baseInst = Currency.getBaseCurrency();
        final Insert insert = new Insert(CISales.BulkPaymentDetraction);
        insert.add(CISales.BulkPaymentDetraction.Date,
                        _parameter.getParameterValue(CIFormSales.Sales_BulkPaymentForm.date.name));
        insert.add(CISales.BulkPaymentDetraction.Name,
                        _parameter.getParameterValue(CIFormSales.Sales_BulkPaymentForm.name.name));
        insert.add(CISales.BulkPaymentDetraction.BulkDefinitionId,
                        _parameter.getParameterValue(CIFormSales.Sales_BulkPaymentForm.bulkDefinition.name));
        insert.add(CISales.BulkPaymentDetraction.CrossTotal, BigDecimal.ZERO);
        insert.add(CISales.BulkPaymentDetraction.CrossTotal, BigDecimal.ZERO);
        insert.add(CISales.BulkPaymentDetraction.NetTotal, BigDecimal.ZERO);
        insert.add(CISales.BulkPaymentDetraction.DiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.BulkPaymentDetraction.RateCrossTotal, BigDecimal.ZERO);
        insert.add(CISales.BulkPaymentDetraction.RateNetTotal, BigDecimal.ZERO);
        insert.add(CISales.BulkPaymentDetraction.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.BulkPaymentDetraction.CurrencyId, baseInst);
        insert.add(CISales.BulkPaymentDetraction.RateCurrencyId, baseInst);
        insert.add(CISales.BulkPaymentDetraction.Rate, new Object[] { 1, 1 });
        insert.add(CISales.BulkPaymentDetraction.Status, Status.find(CISales.BulkPaymentStatus.Open));
        insert.execute();
        ret.setInstance(insert.getInstance());

        final Insert relinsert = new Insert(CISales.BulkPayment2Account);
        relinsert.add(CISales.BulkPayment2Account.FromLink, insert.getId());
        relinsert.add(CISales.BulkPayment2Account.ToLink,
                        _parameter.getParameterValue(CIFormSales.Sales_BulkPaymentForm.account4create.name));
        relinsert.execute();

        return ret;
    }

    public Return connectInsertPostTrigger(final Parameter _parameter)
        throws EFapsException
    {
        updateTotal(_parameter, _parameter.getInstance());
        return new Return();
    }

    public Return connectUpdatePostTrigger(final Parameter _parameter)
        throws EFapsException
    {
        updateTotal(_parameter, _parameter.getInstance());
        return new Return();
    }

    protected void updateTotal(final Parameter _parameter,
                               final Instance _instance) throws EFapsException
    {
        Instance instance;
        BigDecimal total = BigDecimal.ZERO;
        if (_instance.getType().isKindOf(CISales.BulkPaymentDetraction2PaymentDocument.getType())) {
            final PrintQuery print = new PrintQuery(_instance);
            final SelectBuilder selInst = SelectBuilder.get().linkto(CISales.BulkPaymentDetraction2PaymentDocument.FromLink).instance();
            print.addSelect(selInst);
            print.executeWithoutAccessCheck();
            instance = print.getSelect(selInst);
        } else {
            instance = _instance;
        }

        final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.BulkPaymentDetraction2PaymentDocument);
        attrQueryBldr.addWhereAttrEqValue(CISales.BulkPaymentDetraction2PaymentDocument.FromLink, instance);

        final AttributeQuery attrQuery = attrQueryBldr
                        .getAttributeQuery(CISales.BulkPaymentDetraction2PaymentDocument.ToLink);

        final QueryBuilder queryBldr = new QueryBuilder(CISales.Payment);
        queryBldr.addWhereAttrInQuery(CISales.Payment.TargetDocument, attrQuery);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.Payment.Amount);
        multi.executeWithoutAccessCheck();
        while (multi.next()) {
            total = total.add(multi.<BigDecimal>getAttribute(CISales.Payment.Amount));
        }

        final Update update = new Update(instance);
        update.add(CISales.BulkPaymentDetraction.CrossTotal, total);
        update.executeWithoutAccessCheck();
    }


    public Return getReport4Detail(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final AbstractDynamicReport dyRp = getReport(_parameter);
        dyRp.setFileName(CISales.BulkPaymentDetraction.getType().getLabel());
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
        dyRp.setFileName(CISales.BulkPaymentDetraction.getType().getLabel());
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
            final DRDataSource ret = new DRDataSource("contact", "taxnumber", "accountNumber", "docName", "amount");
            final List<Map<String, Object>> values = new ArrayList<Map<String, Object>>();

            final Map<String, String> cont2Acc = new HashMap<String, String>();
            final PrintQuery print = new PrintQuery(_parameter.getInstance());
            print.addAttribute(CISales.BulkPaymentDetraction.BulkDefinitionId);
            print.execute();

            final QueryBuilder queryBldrCont = new QueryBuilder(CISales.BulkPaymentDefinition2Contact);
            queryBldrCont.addWhereAttrEqValue(CISales.BulkPaymentDefinition2Contact.FromLink,
                            print.getAttribute(CISales.BulkPaymentDetraction.BulkDefinitionId));
            final MultiPrintQuery multiCont = queryBldrCont.getPrint();
            multiCont.addAttribute(CISales.BulkPaymentDefinition2Contact.AccountNumber);
            final SelectBuilder selContOid = new SelectBuilder()
                            .linkto(CISales.BulkPaymentDefinition2Contact.ToLink).oid();
            multiCont.addSelect(selContOid);
            multiCont.execute();
            while (multiCont.next()) {
                final String contOid = multiCont.<String>getSelect(selContOid);
                final String account = multiCont
                                .<String>getAttribute(CISales.BulkPaymentDefinition2Contact.AccountNumber);
                cont2Acc.put(contOid, account);
            }

            // TODO add more status
            final QueryBuilder pdAttrQueryBldr = new QueryBuilder(CISales.PaymentDocumentOutAbstract);
            pdAttrQueryBldr.addWhereAttrNotEqValue(CISales.PaymentDocumentOutAbstract.StatusAbstract,
                            Status.find(CISales.PaymentDetractionOutStatus.uuid, "Canceled").getId(),
                            Status.find(CISales.PaymentSupplierOutStatus.uuid, "Canceled").getId());
            final AttributeQuery pdAttrQuery = pdAttrQueryBldr.getAttributeQuery(CISales.PaymentDocumentOutAbstract.ID);

            final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.BulkPaymentDetraction2PaymentDocument);
            attrQueryBldr.addWhereAttrEqValue(CISales.BulkPaymentDetraction2PaymentDocument.FromLink, _parameter.getInstance()
                            .getId());
            attrQueryBldr.addWhereAttrInQuery(CISales.BulkPaymentDetraction2PaymentDocument.ToLink, pdAttrQuery);
            final AttributeQuery attrQuery = attrQueryBldr
                            .getAttributeQuery(CISales.BulkPaymentDetraction2PaymentDocument.ToLink);

            final QueryBuilder queryBldr = new QueryBuilder(CISales.Payment);
            queryBldr.addWhereAttrInQuery(CISales.Payment.TargetDocument, attrQuery);
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selDocName = new SelectBuilder().linkto(CISales.Payment.CreateDocument).attribute(
                            CIERP.DocumentAbstract.Name);
            final SelectBuilder selContactOid = new SelectBuilder().linkto(CISales.Payment.CreateDocument)
                            .linkto(CISales.PaymentDocumentOutAbstract.Contact).oid();
            final SelectBuilder selContact = new SelectBuilder().linkto(CISales.Payment.CreateDocument)
                            .linkto(CISales.PaymentDocumentOutAbstract.Contact)
                            .attribute(CIContacts.Contact.Name);
            final SelectBuilder selTaxnumber = new SelectBuilder().linkto(CISales.Payment.CreateDocument)
                            .linkto(CISales.PaymentDocumentOutAbstract.Contact)
                            .clazz(CIContacts.ClassOrganisation)
                            .attribute(CIContacts.ClassOrganisation.TaxNumber);
            multi.addSelect(selContactOid, selDocName, selContact, selTaxnumber);
            multi.addAttribute(CISales.Payment.Amount);
            multi.execute();
            while (multi.next()) {
                final String contactOid = multi.<String>getSelect(selContactOid);
                final Map<String, Object> map = new HashMap<String, Object>();
                values.add(map);
                map.put("amount", multi.getAttribute(CISales.Payment.Amount));
                map.put("docName", multi.getSelect(selDocName));
                map.put("contact", multi.getSelect(selContact));
                map.put("accountNumber", cont2Acc.containsKey(contactOid) ? cont2Acc.get(contactOid) : "");
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
                ret.add(map.get("contact"),
                        map.get("taxnumber"),
                        map.get("accountNumber"),
                        map.get("docName"),
                        map.get("amount"));
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
            final TextColumnBuilder<String> accountNumberColumn  = DynamicReports.col.column(DBProperties
                            .getProperty("org.efaps.esjp.sales.payment.BulkPayment.Report4Detail.accountNumber"),
                            "accountNumber", DynamicReports.type.stringType());
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

            _builder.addColumn(contactColumn, taxnumberColumn, accountNumberColumn, docNameColumn, amountColumn);
            _builder.addSubtotalAtColumnFooter(subtotal);
        }
    }

    @Override
    public Return validate(final Parameter _parameter)
        throws EFapsException
    {
        final StringBuilder html = new StringBuilder();
        final Return ret = new Return();
        final String accNumBank = _parameter
                        .getParameterValue(CIFormSales.Sales_BulkPaymentDefinition2ContactForm.accountNumber.name);
        if (accNumBank != null && !accNumBank.isEmpty()) {
            final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.BulkPaymentDefinition);
            final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.BulkPaymentDefinition.ID);

            final QueryBuilder queryBldr = new QueryBuilder(CISales.BulkPaymentDefinition2Contact);
            queryBldr.addWhereAttrInQuery(CISales.BulkPaymentDefinition2Contact.FromLink, attrQuery);
            queryBldr.addWhereAttrEqValue(CISales.BulkPaymentDefinition2Contact.AccountNumber, accNumBank);
            final InstanceQuery query = queryBldr.getQuery();
            query.execute();
            if (query.next()) {
                html.append(DBProperties.getProperty("org.efaps.esjp.sales.payment.BulkPayment.existingAccount"));
            }
        }

        if (!html.toString().isEmpty()) {
            ret.put(ReturnValues.SNIPLETT, html.toString());
        } else {
            html.append(DBProperties.getProperty("org.efaps.esjp.sales.payment.BulkPayment.nonExistingAccount"));
            ret.put(ReturnValues.SNIPLETT, html.toString());
            ret.put(ReturnValues.TRUE, true);
        }

        return ret;
    }
}
