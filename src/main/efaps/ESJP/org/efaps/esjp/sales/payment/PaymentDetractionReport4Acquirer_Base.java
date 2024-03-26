/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
 */
package org.efaps.esjp.sales.payment;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.file.FileUtil;
import org.efaps.esjp.common.jasperreport.EFapsTextReport;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("6d915d6b-fdbe-4bb0-8133-588a0ce3613f")
@EFapsApplication("eFapsApp-Sales")
public abstract class PaymentDetractionReport4Acquirer_Base
{
    private static String MASTER_INDICATOR = "*";
    private static String FILE_EXTENSION = "TXT";
    private static String FILE_STARTCHAR = "D";

    public Return execute(final Parameter _parameter)
        throws EFapsException
    {
        return new EFapsTextReport()
        {
            BigDecimal totalAmount = BigDecimal.ZERO;

            @Override
            protected File getEmptyFile4TextReport()
                throws EFapsException
            {
                final String companyTaxNum = ERP.COMPANY_TAX.get();

                final String name = buildName4TextReport(PaymentDetractionReport4Acquirer_Base.FILE_STARTCHAR,
                                companyTaxNum, getSequenceNumber(_parameter));
                final File file = new FileUtil().getFile(name, PaymentDetractionReport4Acquirer_Base.FILE_EXTENSION);
                return file;
            }

            @Override
            protected void addHeaderDefinition(final Parameter _parameter,
                                               final List<ColumnDefinition> _columnsList)
                throws EFapsException
            {
                _columnsList.add(new ColumnDefinition("", 1, 0, null, null, " ", Type.STRINGTYPE, "%1$1s"));
                _columnsList.add(new ColumnDefinition("", 11, 0, null, null, " ", Type.STRINGTYPE, "%1$11s"));
                _columnsList.add(new ColumnDefinition("", 35, 0, null, null, " ", Type.STRINGTYPE, "%1$-35s"));
                _columnsList.add(new ColumnDefinition("", 6, 0, null, null, " ", Type.STRINGTYPE, "%1$-6s"));
                final DecimalFormat format = (DecimalFormat) NumberFormat.getInstance(Context.getThreadContext()
                                .getLocale());
                format.setMaximumIntegerDigits(13);
                format.setMaximumFractionDigits(2);
                format.setMinimumIntegerDigits(13);
                format.setMinimumFractionDigits(2);
                format.setGroupingUsed(false);
                format.setRoundingMode(RoundingMode.HALF_UP);
                _columnsList.add(new ColumnDefinition("", 15, 2, false, format, "0", Type.NUMBERTYPE, null));
            }

            @Override
            protected List<Object> getHeaderData(final Parameter _parameter)
                throws EFapsException
            {
                final List<Object> ret = new ArrayList<>();
                final String companyName = ERP.COMPANY_NAME.get();
                final String companyTaxNum = ERP.COMPANY_TAX.get();
                ret.add(PaymentDetractionReport4Acquirer_Base.MASTER_INDICATOR);
                ret.add(companyTaxNum);
                ret.add(companyName);
                ret.add(getSequenceNumber(_parameter));
                ret.add(this.totalAmount);

                return ret;
            }

            @Override
            protected void addColumnDefinition(final Parameter _parameter,
                                               final List<ColumnDefinition> _columnsList)
                throws EFapsException
            {
                // without '-' is align to right
                _columnsList.add(new ColumnDefinition("", 11, 0, null, null, " ", Type.STRINGTYPE, "%1$11s"));
                // with '-' is align to left
                _columnsList.add(new ColumnDefinition("", 9, 0, null, null, " ", Type.STRINGTYPE, "%1$-9s"));
                _columnsList.add(new ColumnDefinition("", 3, 0, null, null, " ", Type.STRINGTYPE, "%1$-3s"));
                _columnsList.add(new ColumnDefinition("", 11, 0, null, null, " ", Type.STRINGTYPE, "%1$11s"));

                final DecimalFormat format = (DecimalFormat) NumberFormat.getInstance(Context.getThreadContext()
                                .getLocale());
                format.setMaximumIntegerDigits(13);
                format.setMaximumFractionDigits(2);
                format.setMinimumIntegerDigits(13);
                format.setMinimumFractionDigits(2);
                format.setGroupingUsed(false);
                format.setRoundingMode(RoundingMode.HALF_UP);
                _columnsList.add(new ColumnDefinition("", 15, 2, false, format, "0", Type.NUMBERTYPE, null));
                _columnsList.add(new ColumnDefinition("", 2, 0, null, null, " ", Type.STRINGTYPE, "%1$-2s"));
                _columnsList.add(new ColumnDefinition("", 6, 0, null, new SimpleDateFormat("yyyyMM"),
                                " ", Type.DATETYPE, "%1$-6s"));
                /*
                 * final SimpleDateFormat format2 = new SimpleDateFormat("yyyyMMdd"); _columnsList.add(new
                 * ColumnDefinition("", 6, 0, null, format2, "\\s", Type.DATETYPE, "%1$-6s"));
                 */
            }

            @Override
            protected List<List<Object>> createDataSource(final Parameter _parameter)
                throws EFapsException
            {
                final List<List<Object>> lst = new ArrayList<>();
                final Instance instance = _parameter.getInstance();
                final PrintQuery print = new PrintQuery(instance);
                print.addAttribute(CISales.BulkPaymentDetraction.BulkDefinitionId);
                print.execute();

                final QueryBuilder queryBldrDef = new QueryBuilder(CISales.BulkPaymentDefinition2Contact);
                queryBldrDef.addWhereAttrEqValue(CISales.BulkPaymentDefinition2Contact.FromLink,
                                print.<Long>getAttribute(CISales.BulkPaymentDetraction.BulkDefinitionId));
                final MultiPrintQuery multiDef = queryBldrDef.getPrint();
                multiDef.addAttribute(CISales.BulkPaymentDefinition2Contact.ToLink,
                                CISales.BulkPaymentDefinition2Contact.AccountNumber);
                multiDef.execute();
                final Map<Long, String> map = new HashMap<>();
                while (multiDef.next()) {
                    final Long contactId = multiDef.<Long>getAttribute(CISales.BulkPaymentDefinition2Contact.ToLink);
                    final String accNum = multiDef
                                    .<String>getAttribute(CISales.BulkPaymentDefinition2Contact.AccountNumber);
                    map.put(contactId, accNum);
                }

                final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.BulkPaymentDetraction2PaymentDocument);
                attrQueryBldr.addWhereAttrEqValue(CISales.BulkPaymentDetraction2PaymentDocument.FromLink, instance);
                final AttributeQuery attrQuery = attrQueryBldr
                                .getAttributeQuery(CISales.BulkPaymentDetraction2PaymentDocument.ToLink);

                final QueryBuilder attrQueryBldr2 = new QueryBuilder(CISales.PaymentDetractionOut);
                attrQueryBldr2.addWhereAttrInQuery(CISales.PaymentDetractionOut.ID, attrQuery);
                final AttributeQuery attrQuery2 = attrQueryBldr2.getAttributeQuery(CISales.PaymentDetractionOut.ID);

                final QueryBuilder queryBldr = new QueryBuilder(CISales.Payment);
                queryBldr.addWhereAttrInQuery(CISales.Payment.TargetDocument, attrQuery2);
                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder selProvTaxNum = new SelectBuilder().linkto(CISales.Payment.TargetDocument)
                                .linkto(CISales.PaymentDetractionOut.Contact).clazz(CIContacts.ClassOrganisation)
                                .attribute(CIContacts.ClassOrganisation.TaxNumber);
                final SelectBuilder selProvId = new SelectBuilder().linkto(CISales.Payment.TargetDocument)
                                .linkto(CISales.PaymentDetractionOut.Contact).id();
                final SelectBuilder selOperType = new SelectBuilder().linkto(CISales.Payment.TargetDocument)
                                .linkfrom(CISales.BulkPaymentDetraction2PaymentDocument,
                                                CISales.BulkPaymentDetraction2PaymentDocument.ToLink)
                                .linkto(CISales.BulkPaymentDetraction2PaymentDocument.OperationType)
                                .attribute(CISales.AttributeDefinitionOperationType.Value);
                final SelectBuilder selServType = new SelectBuilder().linkto(CISales.Payment.TargetDocument)
                                .linkfrom(CISales.BulkPaymentDetraction2PaymentDocument,
                                                CISales.BulkPaymentDetraction2PaymentDocument.ToLink)
                                .linkto(CISales.BulkPaymentDetraction2PaymentDocument.ServiceType)
                                .attribute(CISales.AttributeDefinitionOperationType.Value);
                final SelectBuilder selDocDate = new SelectBuilder().linkto(CISales.Payment.CreateDocument)
                                .attribute(CISales.DocumentSumAbstract.Date);
                multi.addSelect(selProvTaxNum, selProvId, selOperType, selServType, selDocDate);
                multi.addAttribute(CISales.Payment.Amount);
                multi.execute();
                while (multi.next()) {
                    final List<Object> rowLst = new ArrayList<>();
                    final String provTaxNum = multi.<String>getSelect(selProvTaxNum);
                    final BigDecimal amount = multi.<BigDecimal>getAttribute(CISales.Payment.Amount);
                    this.totalAmount = this.totalAmount.add(amount);
                    final Long provId = multi.<Long>getSelect(selProvId);
                    final String accNum = map.get(provId);
                    final String operType = multi.<String>getSelect(selOperType);
                    final String servType = multi.<String>getSelect(selServType);
                    final DateTime docDate = multi.<DateTime>getSelect(selDocDate);
                    rowLst.add(provTaxNum);
                    rowLst.add("");
                    rowLst.add(servType);
                    rowLst.add(accNum);
                    rowLst.add(amount);
                    rowLst.add(operType);
                    rowLst.add(docDate);
                    lst.add(rowLst);
                }

                return lst;
            }
        }.getTextReport(_parameter);
    }

    protected String getSequenceNumber(final Parameter _parameter)
        throws EFapsException
    {
        String ret = "";
        final Instance instance = _parameter.getInstance();
        final PrintQuery print = new PrintQuery(instance);
        print.addAttribute(CISales.BulkPaymentDetraction.Date,
                        CISales.BulkPaymentDetraction.DueDate);
        print.execute();
        final DateTime dateFrom = print.<DateTime>getAttribute(CISales.BulkPaymentDetraction.Date);

        final QueryBuilder queryBldr = new QueryBuilder(CISales.BulkPaymentDetraction);
        queryBldr.addWhereAttrGreaterValue(CISales.BulkPaymentDetraction.Date,
                        new DateTime(dateFrom.getYear(), 1, 1, 0, 0, 0).minusSeconds(1));
        queryBldr.addWhereAttrLessValue(CISales.BulkPaymentDetraction.Date, dateFrom);
        final String year = new SimpleDateFormat("yy").format(dateFrom.toDate());
        final InstanceQuery query = queryBldr.getQuery();
        query.execute();
        final Integer num = query.getValues().size() + 1;
        final Formatter formatter = new Formatter();
        ret = formatter.format("%1$2s%2$04d", year, num).toString();
        formatter.close();

        return ret;
    }
}
