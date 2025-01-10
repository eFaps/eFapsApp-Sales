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
package org.efaps.esjp.sales.report;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeDate;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.grid.ColumnTitleGroupBuilder;
import net.sf.dynamicreports.report.builder.group.ColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.subtotal.AggregationSubtotalBuilder;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: RetentionBookReport_Base.java 14384 2014-11-07 01:41:00Z
 *          jan@moxter.net $
 */
@EFapsUUID("2b4204b9-3211-43fc-9e9e-c754482fe7d8")
@EFapsApplication("eFapsApp-Sales")
public abstract class RetentionBookReport_Base
    extends FilteredReport
{

    /**
     * Logging instance used in this class.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(RetentionBookReport.class);

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
        dyRp.setFileName(DBProperties.getProperty(RetentionBookReport.class.getName() + ".FileName"));
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
        return new DynRetentionBookReport(this);
    }

    /**
     * Report class.
     */
    public static class DynRetentionBookReport
        extends AbstractDynamicReport
    {

        /**
         * variable to report.
         */
        private final RetentionBookReport_Base filteredReport;

        /**
         * @param _report class used
         */
        public DynRetentionBookReport(final RetentionBookReport_Base _report)
        {
            this.filteredReport = _report;
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final List<DataBean> datasource = new ArrayList<>();
            final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter);
            add2QueryBldr(_parameter, queryBldr);

            final QueryBuilder relQueryBldr = new QueryBuilder(CISales.RetentionCertificate2PaymentRetentionOut);
            relQueryBldr.addWhereAttrInQuery(CISales.RetentionCertificate2PaymentRetentionOut.FromLink,
                            queryBldr.getAttributeQuery("ID"));

            final MultiPrintQuery multi = relQueryBldr.getPrint();
            final SelectBuilder selCert = SelectBuilder.get().linkto(
                            CISales.RetentionCertificate2PaymentRetentionOut.FromLink);
            final SelectBuilder selCertName = new SelectBuilder(selCert).attribute(CISales.RetentionCertificate.Name);
            final SelectBuilder selRet = SelectBuilder.get().linkto(
                            CISales.RetentionCertificate2PaymentRetentionOut.ToLink);
            final SelectBuilder selCertAmount = new SelectBuilder(selRet)
                            .attribute(CISales.IncomingRetention.CrossTotal);

            final SelectBuilder selDoc = new SelectBuilder(selRet)
                            .linkfrom(CISales.IncomingRetention2IncomingInvoice.FromLink)
                            .linkto(CISales.IncomingRetention2IncomingInvoice.ToLink);
            final SelectBuilder selDocName = new SelectBuilder(selDoc).attribute(CISales.DocumentAbstract.Name);
            final SelectBuilder selDocDate = new SelectBuilder(selDoc).attribute(CISales.DocumentAbstract.Date);
            final SelectBuilder selDocTotal = new SelectBuilder(selDoc)
                            .attribute(CISales.DocumentSumAbstract.CrossTotal);
            final SelectBuilder selDocInst = new SelectBuilder(selDoc).instance();
            final SelectBuilder selContact = new SelectBuilder(selDoc).linkto(CISales.DocumentAbstract.Contact);
            final SelectBuilder selContactName = new SelectBuilder(selContact)
                            .attribute(CIContacts.ContactAbstract.Name);
            final SelectBuilder selContactTaxNumber = new SelectBuilder(selContact)
                            .clazz(CIContacts.ClassOrganisation).attribute(CIContacts.ClassOrganisation.TaxNumber);

            multi.addSelect(selCertName, selCertAmount, selDocName, selDocDate, selDocTotal, selDocInst,
                            selContactName, selContactTaxNumber);
            multi.execute();

            while (multi.next()) {
                final DataBean bean = new DataBean();

                bean.setDocInst(multi.<Instance>getSelect(selDocInst))
                                .setCertName(multi.<String>getSelect(selCertName))
                                .setCertAmount(multi.<BigDecimal>getSelect(selCertAmount))
                                .setDocName(multi.<String>getSelect(selDocName))
                                .setDate(multi.<DateTime>getSelect(selDocDate))
                                .setDocTotal(multi.<BigDecimal>getSelect(selDocTotal))
                                .setContactName(multi.<String>getSelect(selContactName))
                                .setContactTaxNumber(multi.<String>getSelect(selContactTaxNumber));
                datasource.add(bean);
            }

            return new JRBeanCollectionDataSource(datasource);
        }

        /**
         * @param _parameter Parameter as passed by the eFaps API
         * @param _queryBldr QueryBuilder to add to
         * @throws EFapsException on error
         */
        protected void add2QueryBldr(final Parameter _parameter,
                                     final QueryBuilder _queryBldr)
            throws EFapsException
        {
            final Map<String, Object> filter = this.filteredReport.getFilterMap(_parameter);
            if (filter.containsKey("dateFrom")) {
                final DateTime date = (DateTime) filter.get("dateFrom");
                _queryBldr.addWhereAttrGreaterValue(CIERP.DocumentAbstract.Date,
                                date.withTimeAtStartOfDay().minusSeconds(1));
            }
            if (filter.containsKey("dateTo")) {
                final DateTime date = (DateTime) filter.get("dateTo");
                _queryBldr.addWhereAttrLessValue(CIERP.DocumentAbstract.Date,
                                date.withTimeAtStartOfDay().plusDays(1));
            }

        }

        @Override
        protected void addColumnDefinition(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {

            final TextColumnBuilder<String> contactColumn = DynamicReports.col.column(DBProperties
                            .getProperty(RetentionBookReport.class.getName() + ".Column.contact"),
                            "contact", DynamicReports.type.stringType());

            final TextColumnBuilder<DateTime> dateColumn = DynamicReports.col.column(DBProperties
                            .getProperty(RetentionBookReport.class.getName() + ".Column.date"),
                            "date", DateTimeDate.get());
            final TextColumnBuilder<String> docTypeColumn = DynamicReports.col.column(DBProperties
                            .getProperty(RetentionBookReport.class.getName() + ".Column.docType"),
                            "docType", DynamicReports.type.stringType());
            final TextColumnBuilder<String> docSerialColumn = DynamicReports.col.column(DBProperties
                            .getProperty(RetentionBookReport.class.getName() + ".Column.docSerial"),
                            "docSerial", DynamicReports.type.stringType());
            final TextColumnBuilder<String> docNumColumn = DynamicReports.col.column(DBProperties
                            .getProperty(RetentionBookReport.class.getName() + ".Column.docNum"),
                            "docNum", DynamicReports.type.stringType());
            final ColumnTitleGroupBuilder docTitelGroup = DynamicReports.grid.titleGroup(DBProperties
                            .getProperty(RetentionBookReport.class.getName() + ".TitelGroup.doc"), docTypeColumn,
                            docSerialColumn, docNumColumn);

            final TextColumnBuilder<String> typeColumn = DynamicReports.col.column(DBProperties
                            .getProperty(RetentionBookReport.class.getName() + ".Column.type"),
                            "type", DynamicReports.type.stringType());

            final TextColumnBuilder<BigDecimal> debitColumn = DynamicReports.col.column(DBProperties
                            .getProperty(RetentionBookReport.class.getName() + ".Column.debit"),
                            "debit", DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> creditColumn = DynamicReports.col.column(DBProperties
                            .getProperty(RetentionBookReport.class.getName() + ".Column.credit"),
                            "credit", DynamicReports.type.bigDecimalType());

            final ColumnTitleGroupBuilder amountGroup = DynamicReports.grid.titleGroup(DBProperties
                            .getProperty(RetentionBookReport.class.getName() + ".TitelGroup.amount"), debitColumn,
                            creditColumn);

            final TextColumnBuilder<String> certSerialColumn = DynamicReports.col.column(DBProperties
                            .getProperty(RetentionBookReport.class.getName() + ".Column.certSerial"),
                            "certSerial", DynamicReports.type.stringType());
            final TextColumnBuilder<String> certNumColumn = DynamicReports.col.column(DBProperties
                            .getProperty(RetentionBookReport.class.getName() + ".Column.certNum"),
                            "certNum", DynamicReports.type.stringType());
            final TextColumnBuilder<BigDecimal> certAmountColumn = DynamicReports.col.column(DBProperties
                            .getProperty(RetentionBookReport.class.getName() + ".Column.certAmount"),
                            "certAmount", DynamicReports.type.bigDecimalType());

            final ColumnTitleGroupBuilder certTitelGroup = DynamicReports.grid.titleGroup(DBProperties
                            .getProperty(RetentionBookReport.class.getName() + ".TitelGroup.cert"), certSerialColumn,
                            certNumColumn, certAmountColumn);

            _builder.columnGrid(dateColumn, docTitelGroup, typeColumn, amountGroup, certTitelGroup);

            final ColumnGroupBuilder contactGroup = DynamicReports.grp.group(contactColumn).groupByDataType();
            _builder.addGroup(contactGroup);

            final AggregationSubtotalBuilder<BigDecimal> certAmountTotal = DynamicReports.sbt.sum(certAmountColumn);
            final AggregationSubtotalBuilder<BigDecimal> debitTotal = DynamicReports.sbt.sum(debitColumn);
            final AggregationSubtotalBuilder<BigDecimal> creditTotal = DynamicReports.sbt.sum(creditColumn);

            _builder.addColumn(contactColumn, dateColumn, docTypeColumn, docSerialColumn, docNumColumn, typeColumn,
                            debitColumn,
                            creditColumn, certSerialColumn, certNumColumn, certAmountColumn);
            _builder.addSubtotalAtGroupFooter(contactGroup, certAmountTotal);
            _builder.addSubtotalAtGroupFooter(contactGroup, debitTotal);
            _builder.addSubtotalAtGroupFooter(contactGroup, creditTotal);
        }

        /**
         * Getter method for the instance variable {@link #filteredReport}.
         *
         * @return value of instance variable {@link #filteredReport}
         */
        protected RetentionBookReport_Base getFilteredReport()
        {
            return this.filteredReport;
        }
    }

    public static class DataBean
    {

        private DateTime date;
        private Instance docInst;
        private String docType;
        private String type;
        private BigDecimal certAmount;
        private String contactTaxNumber;
        private String contactName;
        private BigDecimal docTotal;
        private String docName;
        private String certName;

        /**
         * Getter method for the instance variable {@link #contact}.
         *
         * @return value of instance variable {@link #contact}
         */
        public String getContact()
        {
            return this.contactTaxNumber + " " + this.contactName;
        }

        /**
         * @param _select
         */
        public DataBean setContactTaxNumber(final String _contactTaxNumber)
        {
            this.contactTaxNumber = _contactTaxNumber;
            return this;
        }

        /**
         * @param _select
         * @return
         */
        public DataBean setContactName(final String _contactName)
        {
            this.contactName = _contactName;
            return this;
        }

        /**
         * @param _select
         */
        public DataBean setDocTotal(final BigDecimal _docTotal)
        {
            this.docTotal = _docTotal;
            return this;

        }

        /**
         * @param _select
         */
        public DataBean setDocName(final String _docName)
        {
            this.docName = _docName;
            return this;
        }

        /**
         * @param _select
         */
        public DataBean setCertName(final String _certName)
        {
            this.certName = _certName;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #date}.
         *
         * @return value of instance variable {@link #date}
         */
        public DateTime getDate()
        {
            return this.date;
        }

        /**
         * Setter method for instance variable {@link #date}.
         *
         * @param _date value for instance variable {@link #date}
         */
        public DataBean setDate(final DateTime _date)
        {
            this.date = _date;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #docType}.
         *
         * @return value of instance variable {@link #docType}
         */
        public String getDocType()
            throws EFapsException
        {
            if (this.docType == null) {
                final SelectBuilder selDocType = SelectBuilder.get()
                                .linkfrom(CISales.Document2DocumentType.DocumentLink)
                                .linkto(CISales.Document2DocumentType.DocumentTypeLink)
                                .attribute(CIERP.DocumentType.Name);
                final PrintQuery print = new PrintQuery(getDocInst());
                print.addSelect(selDocType);
                print.execute();
                this.docType = print.getSelect(selDocType);
            }
            return this.docType;
        }

        /**
         * Setter method for instance variable {@link #docType}.
         *
         * @param _docType value for instance variable {@link #docType}
         */
        public DataBean setDocType(final String _docType)
        {
            this.docType = _docType;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #docSerial}.
         *
         * @return value of instance variable {@link #docSerial}
         */
        public String getDocSerial()
        {
            return getSerial(this.docName);
        }

        /**
         * Getter method for the instance variable {@link #docNum}.
         *
         * @return value of instance variable {@link #docNum}
         */
        public String getDocNum()
        {
            return getNum(this.docName);
        }

        /**
         * Getter method for the instance variable {@link #type}.
         *
         * @return value of instance variable {@link #type}
         */
        public String getType()
        {
            if (this.type == null) {
                if (getDocInst().getType().isCIType(CISales.IncomingInvoice)) {
                    this.type = "Compra";
                }
            }
            return this.type;
        }

        /**
         * Setter method for instance variable {@link #type}.
         *
         * @param _type value for instance variable {@link #type}
         */
        public void setType(final String _type)
        {
            this.type = _type;
        }

        /**
         * Getter method for the instance variable {@link #debit}.
         *
         * @return value of instance variable {@link #debit}
         */
        public BigDecimal getDebit()
        {
            return this.docTotal;
        }

        /**
         * Getter method for the instance variable {@link #credit}.
         *
         * @return value of instance variable {@link #credit}
         */
        public BigDecimal getCredit()
        {
            return null;
        }

        /**
         * Getter method for the instance variable {@link #certSerial}.
         *
         * @return value of instance variable {@link #certSerial}
         */
        public String getCertSerial()
        {
            return getSerial(this.certName);
        }

        /**
         * Getter method for the instance variable {@link #certNum}.
         *
         * @return value of instance variable {@link #certNum}
         */
        public String getCertNum()
        {
            return getNum(this.certName);
        }

        /**
         * Getter method for the instance variable {@link #certAmount}.
         *
         * @return value of instance variable {@link #certAmount}
         */
        public BigDecimal getCertAmount()
        {
            return this.certAmount;
        }

        /**
         * Setter method for instance variable {@link #certAmount}.
         *
         * @param _certAmount value for instance variable {@link #certAmount}
         */
        public DataBean setCertAmount(final BigDecimal _certAmount)
        {
            this.certAmount = _certAmount;
            return this;
        }

        protected String getSerial(final String _name)
        {
            String ret = null;
            if (_name != null && !_name.isEmpty()) {
                final String[] arr = _name.split("-");
                if (arr.length > 1) {
                    ret = arr[0];
                }
            }
            return ret;
        }

        protected String getNum(final String _name)
        {
            String ret = null;
            if (_name != null && !_name.isEmpty()) {
                final String[] arr = _name.split("-");
                if (arr.length > 1) {
                    ret = arr[1];
                } else {
                    ret = arr[0];
                }
            }
            return ret;
        }

        /**
         * Getter method for the instance variable {@link #docInst}.
         *
         * @return value of instance variable {@link #docInst}
         */
        public Instance getDocInst()
        {
            return this.docInst;
        }

        /**
         * Setter method for instance variable {@link #docInst}.
         *
         * @param _docInst value for instance variable {@link #docInst}
         */
        public DataBean setDocInst(final Instance _docInst)
        {
            this.docInst = _docInst;
            return this;
        }
    }
}
