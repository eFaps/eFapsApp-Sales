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

package org.efaps.esjp.sales.report;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.efaps.admin.datamodel.Status;
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
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeDate;
import org.efaps.esjp.common.properties.PropertiesUtil;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.erp.RateInfo;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.esjp.sales.tax.xml.TaxEntry;
import org.efaps.esjp.sales.tax.xml.Taxes;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.efaps.util.cache.CacheReloadException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.grid.ColumnTitleGroupBuilder;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

/**
 *
 * @author The eFaps Team
 */
@EFapsUUID("8e6fa637-760a-48ab-8bb9-bf643f4d65d1")
@EFapsApplication("eFapsApp-Sales")
public abstract class SalesRecordReport_Base
    extends FilteredReport
{

    /**
     * Logger for this class.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(SalesRecordReport_Base.class);

    /**
     * Enum used to define the keys for the map.
     */
    public enum Column
    {

        /** Date of the report (used only in accounting for ple). */
        NUMBER("reportNumber"),

        /** The docdate. */
        DOCDATE("docDate"),

        /** The docduedate. */
        DOCDUEDATE("docDueDate"),

        /** The docnumber. */
        DOCNUMBER("docNumber"),

        /** The docsn. */
        DOCSN("docSerialNo"),

        /** The doc doctype. */
        DOCDOCTYPE("docDocumentType"),

        /** The contactname. */
        CONTACTNAME("contactName"),

        /** The contactname. */
        CONTACTDOITYPE("contactDOIType"),

        /** The contactname. */
        CONTACTDOINUMBER("contactDOINumber"),

        /** The exportval. */
        EXPORTVAL("exportValue"),

        /** The taxableval. */
        TAXABLEVAL("taxableValue"),

        /** The exoneratedval. */
        EXONERATEDVAL("exoneratedValue"),

        /** The unaffval. */
        UNAFFVAL("unaffectedValue"),

        /** The isc. */
        ISC("isc"),

        /** The igv. */
        IGV("igv"),

        /** The othertax. */
        OTHERTAX("otherTax"),

        /** The total. */
        TOTAL("total"),

        /** The rate. */
        RATE("rate"),

        /** The reldate. */
        RELDATE("relatedDate"),

        /** The reldoctype. */
        RELDOCTYPE("relatedDocumentType"),

        /** The relsn. */
        RELSN("relatedSerialNo"),

        /** The relnumber. */
        RELNUMBER("relatedNumber");

        /**
         * key.
         */
        private final String key;

        /**
         * @param _key key
         */
        Column(final String _key)
        {
            key = _key;
        }

        /**
         * Getter method for the instance variable {@link #key}.
         *
         * @return value of instance variable {@link #key}
         */
        public String getKey()
        {
            return key;
        }
    }

    /** The Constant DOCTYPE_MAP. */
    protected static final Map<UUID, String> DOCTYPE_MAP = new HashMap<>();
    static {
        SalesRecordReport_Base.DOCTYPE_MAP.put(CISales.Invoice.getType().getUUID(), "01");
        SalesRecordReport_Base.DOCTYPE_MAP.put(CISales.Receipt.getType().getUUID(), "03");
        SalesRecordReport_Base.DOCTYPE_MAP.put(CISales.CreditNote.getType().getUUID(), "07");
        SalesRecordReport_Base.DOCTYPE_MAP.put(CISales.Reminder.getType().getUUID(), "08");
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
        dyRp.setFileName(getDBProperty("FileName"));
        final String html = dyRp.getHtmlSnipplet(_parameter);
        ret.put(ReturnValues.SNIPLETT, html);
        return ret;
    }

    /**
     * Export report.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return exportReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String mime = (String) props.get("Mime");
        final AbstractDynamicReport dyRp = getReport(_parameter);
        dyRp.setFileName(getDBProperty("FileName"));
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
     * Gets the report.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the report
     * @throws EFapsException on error
     */
    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new DynSalesRecordReport(this);
    }

    /**
     * The Class DynSalesProductReport.
     */
    public static class DynSalesRecordReport
        extends AbstractDynamicReport
    {

        /** The filtered report. */
        private final SalesRecordReport_Base filteredReport;

        /**
         * Instantiates a new dyn sales record report.
         *
         * @param _filteredReport the filtered report
         */
        public DynSalesRecordReport(final SalesRecordReport_Base _filteredReport)
        {
            filteredReport = _filteredReport;
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final JRRewindableDataSource ret;
            if (getFilteredReport().isCached(_parameter)) {
                ret = getFilteredReport().getDataSourceFromCache(_parameter);
                try {
                    ret.moveFirst();
                } catch (final JRException e) {
                    throw new EFapsException("JRException", e);
                }
            } else {
                final List<Map<String, ?>> values = new ArrayList<>();
                final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter, Sales.REPORT_SALESRECORD.get());
                add2QueryBldr(_parameter, queryBldr);
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CISales.DocumentSumAbstract.CrossTotal, CISales.DocumentSumAbstract.NetTotal,
                                CISales.DocumentSumAbstract.Rate, CISales.DocumentSumAbstract.Name,
                                CISales.DocumentSumAbstract.Date, CISales.DocumentSumAbstract.DueDate,
                                CISales.DocumentSumAbstract.StatusAbstract, CISales.DocumentSumAbstract.Taxes);
                final SelectBuilder selContactName = new SelectBuilder()
                                .linkto(CISales.DocumentSumAbstract.Contact)
                                .attribute(CIContacts.Contact.Name);
                final SelectBuilder selContactTaxNum = new SelectBuilder()
                                .linkto(CISales.DocumentSumAbstract.Contact)
                                .clazz(CIContacts.ClassOrganisation)
                                .attribute(CIContacts.ClassOrganisation.TaxNumber);
                final SelectBuilder selContactIdCard = new SelectBuilder()
                                .linkto(CISales.DocumentSumAbstract.Contact)
                                .clazz(CIContacts.ClassPerson)
                                .attribute(CIContacts.ClassPerson.IdentityCard);
                final SelectBuilder selContactDOIType = new SelectBuilder()
                                .linkto(CISales.DocumentSumAbstract.Contact)
                                .clazz(CIContacts.ClassPerson)
                                .linkto(CIContacts.ClassPerson.DOITypeLink)
                                .attribute(CIContacts.AttributeDefinitionDOIType.MappingKey);
                final SelectBuilder selCountry = new SelectBuilder()
                                .linkto(CISales.DocumentSumAbstract.Contact)
                                .clazz(CIContacts.ClassLocation)
                                .linkto(CIContacts.ClassLocation.LocationCountryLink)
                                .attribute(CIContacts.AttributeDefinitionCountry.Value);
                multi.addSelect(selContactName, selContactTaxNum, selContactIdCard, selContactDOIType,
                                selCountry);
                multi.execute();
                while (multi.next()) {
                    final Map<String, Object> map = new HashMap<>();
                    values.add(map);
                    final Status status = Status
                                    .get(multi.<Long>getAttribute(CISales.DocumentSumAbstract.StatusAbstract));
                    final boolean valid = isValid(_parameter, status);

                    final String docName = multi.getAttribute(CISales.DocumentSumAbstract.Name);
                    map.put(Column.DOCDATE.getKey(), multi.getAttribute(CISales.DocumentSumAbstract.Date));
                    map.put(Column.DOCSN.getKey(), getSerialNo(_parameter, docName));
                    map.put(Column.DOCNUMBER.getKey(), getNumber(_parameter, docName));
                    map.put(Column.DOCDOCTYPE.getKey(), getDocTypeMap(_parameter).get(
                                    multi.getCurrentInstance().getType().getUUID()));

                    if (valid) {
                        final String taxNumber = multi.getSelect(selContactTaxNum);
                        final String contactIdCard = multi.getSelect(selContactIdCard);
                        final String contactDOIType = multi.getSelect(selContactDOIType);

                        final Object[] rateObj = multi.getAttribute(CISales.DocumentSumAbstract.Rate);
                        final RateInfo rateInfo = RateInfo.getRateInfo(rateObj);
                        final String country = multi.getSelect(selCountry);
                        final Taxes taxes = multi.getAttribute(CISales.DocumentSumAbstract.Taxes);

                        final BigDecimal netTotal = multi.getAttribute(CISales.DocumentSumAbstract.NetTotal);
                        final BigDecimal crossTotal = multi.getAttribute(CISales.DocumentSumAbstract.CrossTotal);

                        map.put(Column.DOCDUEDATE.getKey(), multi.getAttribute(CISales.DocumentSumAbstract.DueDate));
                        map.put(Column.CONTACTNAME.getKey(), multi.getSelect(selContactName));
                        map.put(Column.CONTACTDOITYPE.getKey(), StringUtils.isNotEmpty(taxNumber)
                                        ? "6" : contactDOIType);
                        map.put(Column.CONTACTDOINUMBER.getKey(), StringUtils.isNotEmpty(taxNumber)
                                        ? taxNumber : contactIdCard);
                        map.put(Column.RATE.getKey(), Currency.getBaseCurrency().equals(rateInfo.getCurrencyInstance())
                                        ? null : rateInfo.getRateUI());

                        map.put(Column.TOTAL.getKey(), crossTotal);
                        evalTaxedValues(_parameter, map, netTotal, crossTotal, taxes, country);
                        evalRelated(_parameter, values, map, multi.getCurrentInstance());
                    } else {
                        map.put(Column.CONTACTNAME.getKey(), Status.find(CISales.InvoiceStatus.Replaced).getLabel());
                    }
                }

                final ComparatorChain<Map<String, ?>> chain = new ComparatorChain<>();
                chain.addComparator((_o1,
                 _o2) -> {
                    final String val1 = (String) _o1.get(SalesRecordReport_Base.Column.DOCDOCTYPE.getKey());
                    final String val2 = (String) _o2.get(SalesRecordReport_Base.Column.DOCDOCTYPE.getKey());
                    return ObjectUtils.compare(val1, val2);
                });
                chain.addComparator((_o1,
                 _o2) -> {
                    final DateTime date1 = (DateTime) _o1.get(SalesRecordReport_Base.Column.DOCDATE.getKey());
                    final DateTime date2 = (DateTime) _o2.get(SalesRecordReport_Base.Column.DOCDATE.getKey());
                    return ObjectUtils.compare(date1, date2);
                });
                chain.addComparator((_o1,
                 _o2) -> {
                    final String val1 = (String) _o1.get(SalesRecordReport_Base.Column.DOCNUMBER.getKey());
                    final String val2 = (String) _o2.get(SalesRecordReport_Base.Column.DOCNUMBER.getKey());
                    return ObjectUtils.compare(val1, val2);
                });

                Collections.sort(values, chain);
                ret = new JRMapCollectionDataSource(values);
                getFilteredReport().cache(_parameter, ret);
            }
            return ret;
        }

        /**
         * Checks if is valid.
         *
         * @param _parameter the parameter
         * @param _status the status
         * @return true, if is valid
         * @throws CacheReloadException the cache reload exception
         */
        protected boolean isValid(final Parameter _parameter,
                                  final Status _status)
            throws CacheReloadException
        {
            return !_status.equals(Status.find(CISales.InvoiceStatus.Replaced))
                            && !_status.equals(Status.find(CISales.ReceiptStatus.Replaced))
                            && !_status.equals(Status.find(CISales.ReminderStatus.Replaced))
                            && !_status.equals(Status.find(CISales.CreditNoteStatus.Replaced));
        }

        /**
         * Eval related.
         *
         * @param _parameter the parameter
         * @param _values the values
         * @param _map the map
         * @param _docInst the doc inst
         * @throws EFapsException the e faps exception
         */
        protected void evalRelated(final Parameter _parameter,
                                   final Collection<Map<String, ?>> _values,
                                   final Map<String, Object> _map,
                                   final Instance _docInst)
            throws EFapsException
        {
            QueryBuilder attrQueryBldr = null;
            if (InstanceUtils.isKindOf(_docInst, CISales.CreditNote)) {
                attrQueryBldr = new QueryBuilder(CISales.CreditNote2Invoice);
                attrQueryBldr.addWhereAttrEqValue(CISales.CreditNote2Invoice.FromLink, _docInst);
            } else if (InstanceUtils.isKindOf(_docInst, CISales.Reminder)) {
                attrQueryBldr = new QueryBuilder(CISales.Reminder2Invoice);
                attrQueryBldr.addWhereAttrEqValue(CISales.Reminder2Invoice.FromLink, _docInst);
            }
            if (attrQueryBldr != null) {
                final QueryBuilder queryBldr = new QueryBuilder(CISales.Invoice);
                queryBldr.addWhereAttrInQuery(CISales.Invoice.ID,
                                attrQueryBldr.getAttributeQuery(CISales.CreditNote2Invoice.ToLink));
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CISales.Invoice.Date, CISales.Invoice.Name,
                                CISales.DocumentSumAbstract.StatusAbstract);
                multi.execute();
                boolean first = true;
                while (multi.next()) {
                    final Status status = Status.get(multi.<Long>getAttribute(
                                    CISales.DocumentSumAbstract.StatusAbstract));
                    final boolean valid = isValid(_parameter, status);
                    if (valid) {
                        final Map<String, Object> map;
                        if (first) {
                            first = false;
                            map = _map;
                        } else {
                            map = new HashMap<>();
                            for (final Entry<String, Object> entry : _map.entrySet()) {
                                map.put(entry.getKey(), entry.getValue());
                            }
                            _values.add(map);
                        }
                        map.put(Column.RELDATE.getKey(), multi.getAttribute(CISales.Invoice.Date));
                        map.put(Column.RELSN.getKey(), getSerialNo(_parameter, multi.getAttribute(CISales.Invoice.Name)));
                        map.put(Column.RELNUMBER.getKey(), getNumber(_parameter, multi.getAttribute(CISales.Invoice.Name)));
                        map.put(Column.RELDOCTYPE.getKey(), getDocTypeMap(_parameter).get(
                                        multi.getCurrentInstance().getType().getUUID()));
                    }
                }
            }
        }

        /**
         * Eval taxed values.
         *
         * @param _parameter the parameter
         * @param _map the map
         * @param _netTotal the net total
         * @param _crossTotal the cross total
         * @param _taxes the taxes
         * @param _country the country
         * @throws EFapsException the e faps exception
         */
        protected void evalTaxedValues(final Parameter _parameter,
                                       final Map<String, Object> _map,
                                       final BigDecimal _netTotal,
                                       final BigDecimal _crossTotal,
                                       final Taxes _taxes,
                                       final String _country)
            throws EFapsException
        {
            // no taxes found
            if (_taxes == null) {
                // but it is local, therefore it must be unaffected, in other case it is export
                if (ERP.COMPANY_COUNTRY.get().equalsIgnoreCase(_country)) {
                    _map.put(Column.TAXABLEVAL.getKey(), _netTotal);
                    _map.put(Column.UNAFFVAL.getKey(), _netTotal);
                } else {
                    _map.put(Column.EXPORTVAL.getKey(), _netTotal);
                }
            } else {
                BigDecimal igv = BigDecimal.ZERO;
                final BigDecimal unaff = BigDecimal.ZERO;
                BigDecimal other = BigDecimal.ZERO;
                final Properties taxDef = PropertiesUtil.getProperties4Prefix(Sales.REPORT_SALESRECORD.get(), "tax");
                for (final TaxEntry entry : _taxes.getEntries()) {
                    final String def = taxDef.getProperty(entry.getUUID().toString(), "OTHER");
                    switch (def) {
                        case "IGV":
                            igv = igv.add(entry.getAmount());
                            break;
                        case "UNAFF":
                            break;
                        case "OTHER":
                        default:
                            other = other.add(entry.getAmount());
                            break;
                    }
                }
                if (BigDecimal.ZERO.compareTo(igv) < 0) {
                    _map.put(Column.IGV.getKey(), igv);
                }
                if (BigDecimal.ZERO.compareTo(unaff) < 0) {
                    _map.put(Column.UNAFFVAL.getKey(), unaff);
                }
                if (BigDecimal.ZERO.compareTo(other) < 0) {
                    _map.put(Column.OTHERTAX.getKey(), other);
                }
                _map.put(Column.TAXABLEVAL.getKey(), _netTotal);
            }
        }

        /**
         * @param _parameter Parameter as passed from the eFaps API.
         * @param _queryBldr the query bldr
         * @throws EFapsException on error.
         */
        protected void add2QueryBldr(final Parameter _parameter,
                                     final QueryBuilder _queryBldr)
            throws EFapsException
        {
            final Map<String, Object> filter = filteredReport.getFilterMap(_parameter);
            final DateTime dateFrom;
            if (filter.containsKey("dateFrom")) {
                dateFrom = (DateTime) filter.get("dateFrom");
            } else {
                dateFrom = new DateTime().minusYears(1);
            }
            final DateTime dateTo;
            if (filter.containsKey("dateTo")) {
                dateTo = (DateTime) filter.get("dateTo");
            } else {
                dateTo = new DateTime();
            }
            _queryBldr.addWhereAttrGreaterValue(CIERP.DocumentAbstract.Date, dateFrom.minusMinutes(1));
            _queryBldr.addWhereAttrLessValue(CIERP.DocumentAbstract.Date, dateTo.plusDays(1));

            final String contactOid = _parameter.getParameterValue("contact");
            final String contactName = _parameter.getParameterValue("contactAutoComplete");

            if (contactOid != null && !contactOid.isEmpty() && contactName != null && !contactName.isEmpty()) {
                _queryBldr.addWhereAttrEqValue(CIERP.DocumentAbstract.Contact, Instance.get(contactOid).getId());
            }
        }

        @Override
        protected void addColumnDefinition(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final TextColumnBuilder<Integer> numberColumn = DynamicReports.col
                            .reportRowNumberColumn(getFilteredReport().getDBProperty(Column.NUMBER.getKey()));

            final TextColumnBuilder<DateTime> docDateCol = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty(Column.DOCDATE.getKey()), Column.DOCDATE.getKey(), DateTimeDate.get());
            final TextColumnBuilder<DateTime> docDueDateCol = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty(Column.DOCDUEDATE.getKey()), Column.DOCDUEDATE.getKey(), DateTimeDate.get());

            final TextColumnBuilder<String> docDocTypeColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty(Column.DOCDOCTYPE.getKey()), Column.DOCDOCTYPE.getKey(),
                            DynamicReports.type.stringType());

            final TextColumnBuilder<String> docNumberColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty(Column.DOCNUMBER.getKey()), Column.DOCNUMBER.getKey(),
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> docSNColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty(Column.DOCSN.getKey()), Column.DOCSN.getKey(),
                            DynamicReports.type.stringType());

            final ColumnTitleGroupBuilder docTitleGroup = DynamicReports.grid.titleGroup(
                            getFilteredReport().getDBProperty("docTitleGroup"), docDocTypeColumn, docSNColumn,
                            docNumberColumn);

            final TextColumnBuilder<String> contactDOITypeColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty(Column.CONTACTDOITYPE.getKey()),
                            Column.CONTACTDOITYPE.getKey(),
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> contactDOINumberColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty(Column.CONTACTDOINUMBER.getKey()),
                            Column.CONTACTDOINUMBER.getKey(),
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> contactNameColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty(Column.CONTACTNAME.getKey()), Column.CONTACTNAME.getKey(),
                            DynamicReports.type.stringType());

            final ColumnTitleGroupBuilder contactDOITitleGroup = DynamicReports.grid.titleGroup(
                            getFilteredReport().getDBProperty("contactDOITitleGroup"), contactDOITypeColumn,
                            contactDOINumberColumn);

            final ColumnTitleGroupBuilder contactTitleGroup = DynamicReports.grid.titleGroup(
                            getFilteredReport().getDBProperty("contactTitleGroup"), contactDOITitleGroup,
                            contactNameColumn);


            final TextColumnBuilder<BigDecimal> exportValColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty(Column.EXPORTVAL.getKey()), Column.EXPORTVAL.getKey(),
                            DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<BigDecimal> taxableValColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty(Column.TAXABLEVAL.getKey()), Column.TAXABLEVAL.getKey(),
                            DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<BigDecimal> exoneratedValueColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty(Column.EXONERATEDVAL.getKey()),
                            Column.EXONERATEDVAL.getKey(), DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<BigDecimal> unaffectedValueColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty(Column.UNAFFVAL.getKey()), Column.UNAFFVAL.getKey(),
                            DynamicReports.type.bigDecimalType());

            final ColumnTitleGroupBuilder taxfreeGroup = DynamicReports.grid.titleGroup(
                            getFilteredReport().getDBProperty("taxfreeGroup"), exoneratedValueColumn,
                            unaffectedValueColumn);

            final TextColumnBuilder<BigDecimal> iscColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty(Column.ISC.getKey()), Column.ISC.getKey(),
                            DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<BigDecimal> igvColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty(Column.IGV.getKey()), Column.IGV.getKey(),
                            DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<BigDecimal> otherTaxColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty(Column.OTHERTAX.getKey()), Column.OTHERTAX.getKey(),
                            DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<BigDecimal> totalColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty(Column.TOTAL.getKey()), Column.TOTAL.getKey(),
                            DynamicReports.type.bigDecimalType());


            final TextColumnBuilder<BigDecimal> rateColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty(Column.RATE.getKey()), Column.RATE.getKey(),
                            DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<DateTime> relDateCol = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty(Column.RELDATE.getKey()), Column.RELDATE.getKey(), DateTimeDate.get());

            final TextColumnBuilder<String> relDocTypeColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty(Column.RELDOCTYPE.getKey()), Column.RELDOCTYPE.getKey(),
                            DynamicReports.type.stringType());

            final TextColumnBuilder<String> relSerialNoColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty(Column.RELSN.getKey()), Column.RELSN.getKey(),
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> relNumberColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty(Column.RELNUMBER.getKey()), Column.RELNUMBER.getKey(),
                            DynamicReports.type.stringType());

            final ColumnTitleGroupBuilder relatedGroup = DynamicReports.grid.titleGroup(
                            getFilteredReport().getDBProperty("relatedGroup"), relDateCol,
                            relDocTypeColumn, relSerialNoColumn, relNumberColumn);

            _builder.columnGrid(numberColumn, docDateCol, docDueDateCol, docTitleGroup, contactTitleGroup,
                                    exportValColumn, taxableValColumn, taxfreeGroup, iscColumn, igvColumn,
                                    otherTaxColumn, totalColumn, rateColumn, relatedGroup)
                    .addColumn(numberColumn, docDateCol, docDueDateCol, docDocTypeColumn, docSNColumn,
                                        docNumberColumn, contactDOITypeColumn, contactDOINumberColumn,
                                        contactNameColumn, exportValColumn, taxableValColumn, exoneratedValueColumn,
                                        unaffectedValueColumn, iscColumn, igvColumn, otherTaxColumn, totalColumn,
                                        rateColumn, relDateCol, relDocTypeColumn, relSerialNoColumn, relNumberColumn);
        }

        /**
         * Gets the serial no.
         *
         * @param _parameter the parameter
         * @param _name the name
         * @return the serial no
         */
        protected String getSerialNo(final Parameter _parameter,
                                     final String _name)
        {
            final String ret;
            final Pattern pattern = Pattern.compile("^[A-Za-z0-9]{1,4}");
            final Matcher matcher = pattern.matcher(_name);
            if (matcher.find()) {
                ret = matcher.group();
            } else {
                ret = "";
            }
            return ret;
        }

        /**
         * Gets the number.
         *
         * @param _parameter the parameter
         * @param _name the name
         * @return the number
         */
        protected String getNumber(final Parameter _parameter,
                                   final String _name)
        {
            final String ret;
            final Pattern pattern = Pattern.compile("(?<=[^A-Za-z0-9])\\d+");
            final Matcher matcher = pattern.matcher(_name);
            if (matcher.find()) {
                ret = matcher.group();
            } else {
                ret = _name;
            }
            return ret;
        }

        /**
         * Gets the doc type map.
         *
         * @param _parameter the parameter
         * @return the doc type map
         */
        protected Map<UUID, String> getDocTypeMap(final Parameter _parameter)
        {
            return SalesRecordReport_Base.DOCTYPE_MAP;
        }

        /**
         * Getter method for the instance variable {@link #filteredReport}.
         *
         * @return value of instance variable {@link #filteredReport}
         */
        public SalesRecordReport_Base getFilteredReport()
        {
            return filteredReport;
        }
    }
}
