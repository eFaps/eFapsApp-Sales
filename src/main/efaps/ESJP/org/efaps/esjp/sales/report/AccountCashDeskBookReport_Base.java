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

package org.efaps.esjp.sales.report;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
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
import org.efaps.esjp.erp.util.ERP;
import org.efaps.ui.wicket.models.EmbeddedLink;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.ComponentColumnBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.component.GenericElementBuilder;
import net.sf.dynamicreports.report.builder.expression.AbstractComplexExpression;
import net.sf.dynamicreports.report.builder.subtotal.AggregationSubtotalBuilder;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * Report used to analyze receipt for professional fees (Recibos Honorarios) by
 * grouping them by month and contact.
 *
 * @author The eFaps Team
 */
@EFapsUUID("3a74eb77-e576-4fdb-9169-82c7b03f25bb")
@EFapsApplication("eFapsApp-Sales")
public abstract class AccountCashDeskBookReport_Base
    extends FilteredReport
{

    /**
     * Logging instance used in this class.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(AccountCashDeskBookReport.class);

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
        final AbstractDynamicReport dyRp = getReport(_parameter);
        dyRp.setFileName(getDBProperty(".FileName"));

        final Map<String, Object> params = new HashMap<String, Object>();
        final SystemConfiguration config = ERP.getSysConfig();
        if (config != null) {
            final String companyName = ERP.COMPANYNAME.get();
            if (companyName != null && !companyName.isEmpty()) {
                params.put("CompanyName", companyName);
            }
        }
        dyRp.setParameters(params);
        File file = null;
        if ("xls".equalsIgnoreCase(getProperty(_parameter, "Mime"))) {
            file = dyRp.getExcel(_parameter);
        } else if ("pdf".equalsIgnoreCase(getProperty(_parameter, "Mime"))) {
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
        return new DynAccountCashDeskBookReport(this);
    }

    /**
     * Report class.
     */
    public static class DynAccountCashDeskBookReport
        extends AbstractDynamicReport
    {
        /**
         * variable to report.
         */
        private final AccountCashDeskBookReport_Base filteredReport;

        /**
         * @param _report class used
         */
        public DynAccountCashDeskBookReport(final AccountCashDeskBookReport_Base _report)
        {
            this.filteredReport = _report;
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final SelectBuilder selPaymentDocInst = new SelectBuilder().linkto(CISales.TransactionAbstract.Payment)
                            .linkto(CISales.Payment.TargetDocument).instance();
            final SelectBuilder selDocInst = new SelectBuilder().linkto(CISales.TransactionAbstract.Payment)
                            .linkto(CISales.Payment.CreateDocument).instance();

            final List<DocumentBean> datasource = new ArrayList<>();
            final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter);
            add2QueryBldr(_parameter, queryBldr);
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CISales.TransactionAbstract.Date,
                            CISales.TransactionAbstract.Amount);
            multi.addSelect(selPaymentDocInst, selDocInst);
            multi.execute();
            BigDecimal total = BigDecimal.ZERO;
            while (multi.next()) {
                final DocumentBean bean = getBean(_parameter);
                datasource.add(bean);
                bean.setOid(multi.getCurrentInstance().getOid());
                bean.setDate(multi.<DateTime>getAttribute(CISales.TransactionAbstract.Date));
                bean.setTypeTrans(getDBProperty(multi.getCurrentInstance().getType().getName()));
                final Instance payDocInst = multi.<Instance>getSelect(selPaymentDocInst);
                final Instance docInst = multi.<Instance>getSelect(selDocInst);
                getValues4Instances(_parameter, bean, payDocInst, docInst);
                if (CISales.TransactionInbound.getType().equals(multi.getCurrentInstance().getType())) {
                    bean.setIn(multi.<BigDecimal>getAttribute(CISales.TransactionAbstract.Amount));
                } else {
                    bean.setOut(multi.<BigDecimal>getAttribute(CISales.TransactionAbstract.Amount));
                }
                total = total.add(bean.getIn().add(bean.getOut().negate()));
                bean.setBalance(total);
            }
            final ComparatorChain<DocumentBean> chain = new ComparatorChain<DocumentBean>();
            chain.addComparator(new Comparator<DocumentBean>() {
                    @Override
                    public int compare(final DocumentBean _o1,
                                       final DocumentBean _o2)
                    {
                        return _o1.getDate().compareTo(_o2.getDate());
                    }
                }
            );
            Collections.sort(datasource, chain);
            return new JRBeanCollectionDataSource(datasource);
        }

        /**
         * @param _bean
         * @param _payDocInst
         * @param _docInst
         */
        protected void getValues4Instances(final Parameter _parameter,
                                           final DocumentBean _bean,
                                           final Instance _payDocInst,
                                           final Instance _docInst)
            throws EFapsException
        {
            final SelectBuilder selContactName = new SelectBuilder().linkto(CIERP.DocumentAbstract.Contact)
                            .attribute(CIContacts.Contact.Name);
            if (_payDocInst != null && _payDocInst.isValid()) {
                final PrintQuery print = new PrintQuery(_payDocInst);
                print.addAttribute(CISales.PaymentDocumentIOAbstract.Code,
                                CISales.PaymentDocumentIOAbstract.Note);
                print.addSelect(selContactName);
                print.execute();

                _bean.setVoucher(print.<String>getAttribute(CISales.PaymentDocumentIOAbstract.Code));
                _bean.setContact(print.<String>getSelect(selContactName));
                _bean.setDescription(print.<String>getAttribute(CISales.PaymentDocumentIOAbstract.Note));
            }
            if (_docInst != null && _docInst.isValid()) {
                final PrintQuery print = new PrintQuery(_docInst);
                print.addAttribute(CISales.DocumentAbstract.Name);
                print.addSelect(selContactName);
                print.execute();

                _bean.setNumDoc(print.<String>getAttribute(CISales.DocumentAbstract.Name));
                if (_bean.getContact() == null || _bean.getContact().isEmpty()) {
                    _bean.setContact(print.<String>getSelect(selContactName));
                }
            }
        }

        /**
         * @param _parameter  Parameter as passed by the eFaps API
         * @param _queryBldr    QueryBuilder to add to
         * @throws EFapsException on error
         */
        protected void add2QueryBldr(final Parameter _parameter,
                                     final QueryBuilder _queryBldr)
            throws EFapsException
        {
            final Instance instance = _parameter.getInstance();
            if (instance != null && instance.isValid()
                            && CISales.AccountCashDesk.getType().equals(instance.getType())) {
                _queryBldr.addWhereAttrEqValue(CISales.TransactionAbstract.Account, instance);
            } else {
                _queryBldr.addWhereAttrEqValue(CISales.TransactionAbstract.Account, 0);
            }

            final Map<String, Object> filter = this.filteredReport.getFilterMap(_parameter);
            if (filter.containsKey("dateFrom")) {
                final DateTime date = (DateTime) filter.get("dateFrom");
                _queryBldr.addWhereAttrGreaterValue(CISales.TransactionAbstract.Date,
                                date.withTimeAtStartOfDay().minusSeconds(1));
                if (getParameters() != null) {
                    final String dateStr = date.toString("dd/MM/YY");
                    getParameters().put("DateFrom", dateStr);
                }
            }
            if (filter.containsKey("dateTo")) {
                final DateTime date = (DateTime) filter.get("dateTo");
                _queryBldr.addWhereAttrLessValue(CISales.TransactionAbstract.Date,
                                date.withTimeAtStartOfDay().plusDays(1));
                if (getParameters() != null) {
                    final String dateStr = date.toString("dd/MM/YY");
                    getParameters().put("DateTo", dateStr);
                }
            }
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final TextColumnBuilder<DateTime> dateColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AccountCashDeskBookReport.class.getName() + ".Column.Date"),
                            "date", DateTimeDate.get());
            final TextColumnBuilder<String> voucherColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AccountCashDeskBookReport.class.getName() + ".Column.Voucher"),
                            "voucher", DynamicReports.type.stringType());
            final TextColumnBuilder<String> typeColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AccountCashDeskBookReport.class.getName() + ".Column.Type"),
                            "typeTrans", DynamicReports.type.stringType());
            final TextColumnBuilder<String> numDocColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AccountCashDeskBookReport.class.getName() + ".Column.NumDoc"),
                            "numDoc", DynamicReports.type.stringType());
            final TextColumnBuilder<String> contactColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AccountCashDeskBookReport.class.getName() + ".Column.Contact"),
                            "contact", DynamicReports.type.stringType());
            final TextColumnBuilder<String> descriptionColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AccountCashDeskBookReport.class.getName() + ".Column.Description"),
                            "description", DynamicReports.type.stringType());
            final TextColumnBuilder<BigDecimal> inColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AccountCashDeskBookReport.class.getName() + ".Column.In"), "in",
                            DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> outColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AccountCashDeskBookReport.class.getName() + ".Column.Out"), "out",
                            DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> balanceColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AccountCashDeskBookReport.class.getName() + ".Column.Balance"), "balance",
                            DynamicReports.type.bigDecimalType());

            final GenericElementBuilder linkElement = DynamicReports.cmp.genericElement(
                            "http://www.efaps.org", "efapslink")
                            .addParameter(EmbeddedLink.JASPER_PARAMETERKEY, new LinkExpression())
                            .setHeight(12).setWidth(25);
            final ComponentColumnBuilder linkColumn = DynamicReports.col.componentColumn(linkElement).setTitle("");
            if (getExType().equals(ExportType.HTML)) {
                _builder.addColumn(linkColumn);
            }

            _builder.addColumn(dateColumn, voucherColumn, typeColumn, numDocColumn, contactColumn, descriptionColumn,
                            inColumn, outColumn, balanceColumn);

            final AggregationSubtotalBuilder<BigDecimal> totalInSum = DynamicReports.sbt.sum(inColumn);
            final AggregationSubtotalBuilder<BigDecimal> totalOutSum = DynamicReports.sbt.sum(outColumn);

            _builder.addSubtotalAtColumnFooter(totalInSum, totalOutSum);
        }

        /**
         * @param _parameter Parameter as passed by the eFaps API
         * @return new DocumentBean
         * @throws EFapsException on error
         */
        protected DocumentBean getBean(final Parameter _parameter)
            throws EFapsException
        {
            return new DocumentBean();
        }

        /**
         * Getter method for the instance variable {@link #filteredReport}.
         *
         * @return value of instance variable {@link #filteredReport}
         */
        protected AccountCashDeskBookReport_Base getFilteredReport()
        {
            return this.filteredReport;
        }
    }

    /**
     * Expression used to render a link for the UserInterface.
     */
    public static class LinkExpression
        extends AbstractComplexExpression<EmbeddedLink>
    {

        /**
         * Needed for serialization.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Costructor.
         */
        public LinkExpression()
        {
            addExpression(DynamicReports.field("oid", String.class));
        }

        @Override
        public EmbeddedLink evaluate(final List<?> _values,
                                     final ReportParameters _reportParameters)
        {
            final String oid = (String) _values.get(0);
            return EmbeddedLink.getJasperLink(oid);
        }
    }

    /**
     * DataBean.
     */
    public static class DocumentBean
    {
        /**
         * OID of the document.
         */
        private String oid;

        /**
         * Date.
         */
        private DateTime date;

        /**
         * Code of the payment document.
         */
        private String voucher;

        /**
         * Type of the transaction.
         */
        private String typeTrans;

        /**
         * NumberDoc of the transaction.
         */
        private String numDoc;

        /**
         * Contact of the transaction.
         */
        private String contact;

        /**
         * Description of the transaction.
         */
        private String description;

        /**
         * In of the transaction.
         */
        private BigDecimal in = BigDecimal.ZERO;

        /**
         * Out of the transaction.
         */
        private BigDecimal out = BigDecimal.ZERO;

        /**
         * Balance of the transaction.
         */
        private BigDecimal balance;

        /**
         * Getter method for the instance variable {@link #oid}.
         *
         * @return value of instance variable {@link #oid}
         */
        public String getOid()
        {
            return this.oid;
        }

        /**
         * Setter method for instance variable {@link #oid}.
         *
         * @param _oid value for instance variable {@link #oid}
         */
        public void setOid(final String _oid)
        {
            this.oid = _oid;
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
        public void setDate(final DateTime _date)
        {
            this.date = _date;
        }

        /**
         * Getter method for the instance variable {@link #voucher}.
         *
         * @return value of instance variable {@link #voucher}
         */
        public String getVoucher()
        {
            return this.voucher;
        }

        /**
         * Setter method for instance variable {@link #voucher}.
         *
         * @param _date value for instance variable {@link #voucher}
         */
        public void setVoucher(final String _voucher)
        {
            this.voucher = _voucher;
        }

        /**
         * Getter method for the instance variable {@link #typeTrans}.
         *
         * @return value of instance variable {@link #typeTrans}
         */
        public String getTypeTrans()
        {
            return this.typeTrans;
        }

        /**
         * Setter method for instance variable {@link #typeTrans}.
         *
         * @param _date value for instance variable {@link #typeTrans}
         */
        public void setTypeTrans(final String _typeTrans)
        {
            this.typeTrans = _typeTrans;
        }

        /**
         * Getter method for the instance variable {@link #typeTrans}.
         *
         * @return value of instance variable {@link #numDoc}
         */
        public String getNumDoc()
        {
            return this.numDoc;
        }

        /**
         * Setter method for instance variable {@link #numDoc}.
         *
         * @param _date value for instance variable {@link #numDoc}
         */
        public void setNumDoc(final String _numDoc)
        {
            this.numDoc = _numDoc;
        }

        /**
         * Getter method for the instance variable {@link #contact}.
         *
         * @return value of instance variable {@link #contact}
         */
        public String getContact()
        {
            return this.contact;
        }

        /**
         * Setter method for instance variable {@link #contact}.
         *
         * @param _date value for instance variable {@link #contact}
         */
        public void setContact(final String _contact)
        {
            this.contact = _contact;
        }

        /**
         * Getter method for the instance variable {@link #description}.
         *
         * @return value of instance variable {@link #description}
         */
        public String getDescription()
        {
            return this.description;
        }

        /**
         * Setter method for instance variable {@link #description}.
         *
         * @param _date value for instance variable {@link #description}
         */
        public void setDescription(final String _description)
        {
            this.description = _description;
        }

        /**
         * Getter method for the instance variable {@link #in}.
         *
         * @return value of instance variable {@link #in}
         */
        public BigDecimal getIn()
        {
            return this.in;
        }

        /**
         * Setter method for instance variable {@link #in}.
         *
         * @param _date value for instance variable {@link #in}
         */
        public void setIn(final BigDecimal _in)
        {
            this.in = _in;
        }

        /**
         * Getter method for the instance variable {@link #out}.
         *
         * @return value of instance variable {@link #out}
         */
        public BigDecimal getOut()
        {
            return this.out;
        }

        /**
         * Setter method for instance variable {@link #out}.
         *
         * @param _date value for instance variable {@link #out}
         */
        public void setOut(final BigDecimal _out)
        {
            this.out = _out;
        }

        /**
         * Getter method for the instance variable {@link #balance}.
         *
         * @return value of instance variable {@link #balance}
         */
        public BigDecimal getBalance()
        {
            return this.balance;
        }

        /**
         * Setter method for instance variable {@link #balance}.
         *
         * @param _date value for instance variable {@link #balance}
         */
        public void setBalance(final BigDecimal _balance)
        {
            this.balance = _balance;
        }
    }
}
