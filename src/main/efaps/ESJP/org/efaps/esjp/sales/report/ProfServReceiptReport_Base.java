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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.efaps.admin.datamodel.Status;
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
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport_Base;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeDate;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeMonth;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.erp.util.ERP.DocTypeConfiguration;
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
import net.sf.dynamicreports.report.builder.group.ColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.subtotal.AggregationSubtotalBuilder;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;


/**
 * Report used to analyze receipt for professional fees
 * (Recibos Honorarios) by grouping them by month and contact.
 *
 * @author The eFaps Team
 *
 */
@EFapsUUID("20346db3-7715-4fd7-b73f-59b3e03d0ea2")
@EFapsApplication("eFapsApp-Sales")
public abstract class ProfServReceiptReport_Base
    extends FilteredReport
{

    /**
     * Logging instance used in this class.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(ProfServReceiptReport.class);

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
        dyRp.setFileName(DBProperties.getProperty(ProfServReceiptReport.class.getName() + ".FileName"));
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
        return new DynProfServReceiptReport(this);
    }

    /**
     * Report class.
     */
    public static class DynProfServReceiptReport
        extends AbstractDynamicReport
    {

        /**
         * variable to report.
         */
        private final ProfServReceiptReport_Base filteredReport;

        /**
         * @param _report class used
         */
        public DynProfServReceiptReport(final ProfServReceiptReport_Base _report)
        {
            this.filteredReport = _report;
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final List<DocumentBean> datasource = new ArrayList<>();
            final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter);
            add2QueryBldr(_parameter, queryBldr);
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selContactName = SelectBuilder.get().linkto(CISales.DocumentAbstract.Contact)
                            .attribute(CIContacts.ContactAbstract.Name);
            multi.addSelect(selContactName);
            multi.addAttribute(CISales.DocumentAbstract.Name, CISales.DocumentAbstract.Revision,
                            CISales.DocumentAbstract.Date, CISales.DocumentSumAbstract.CrossTotal,
                            CISales.DocumentAbstract.StatusAbstract);
            multi.execute();
            while (multi.next()) {
                final DocumentBean bean = getBean(_parameter);
                datasource.add(bean);
                bean.setOid(multi.getCurrentInstance().getOid());
                bean.setName(multi.<String>getAttribute(CISales.DocumentAbstract.Name));
                bean.setTypeLabel(multi.getCurrentInstance().getType().getLabel());
                bean.setContactName(multi.<String>getSelect(selContactName));
                bean.setDate(multi.<DateTime>getAttribute(CISales.DocumentAbstract.Date));
                bean.setCrossTotal(multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.CrossTotal));
                bean.setRevision(multi.<String>getAttribute(CISales.DocumentAbstract.Revision));
                bean.setStatusLabel(Status.get(multi.<Long>getAttribute(CISales.DocumentAbstract.StatusAbstract))
                                .getLabel());
            }
            final ComparatorChain<DocumentBean> chain = new ComparatorChain<>();
            chain.addComparator(new Comparator<DocumentBean>() {
                    @Override
                    public int compare(final DocumentBean _o1,
                                       final DocumentBean _o2)
                    {
                        return Integer.valueOf(_o1.getDate().getYear())
                                        .compareTo(Integer.valueOf(_o2.getDate().getYear()));
                    }
                }
            );
            chain.addComparator(new Comparator<DocumentBean>() {
                    @Override
                    public int compare(final DocumentBean _o1,
                                       final DocumentBean _o2)
                    {
                        return Integer.valueOf(_o1.getDate().getMonthOfYear())
                                        .compareTo(Integer.valueOf(_o2.getDate().getMonthOfYear()));
                    }
                }
            );
            chain.addComparator(new Comparator<DocumentBean>() {
                    @Override
                    public int compare(final DocumentBean _o1,
                                       final DocumentBean _o2)
                    {
                        return _o1.getContactName().compareTo(_o2.getContactName());
                    }
                }
            );
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
         * @param _parameter  Parameter as passed by the eFaps API
         * @param _queryBldr    QueryBuilder to add to
         * @throws EFapsException on error
         */
        protected void add2QueryBldr(final Parameter _parameter,
                                     final QueryBuilder _queryBldr)
            throws EFapsException
        {
            final Map<String, Object> filter = this.filteredReport.getFilterMap(_parameter);
            if (filter.containsKey("contact") && filter.get("contact") instanceof InstanceFilterValue) {
                final Instance contactInst = ((InstanceFilterValue) filter.get("contact")).getObject();
                if (contactInst.isValid()) {
                    _queryBldr.addWhereAttrEqValue(CIERP.DocumentAbstract.Contact, contactInst);
                }
            }
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

            final QueryBuilder docTypeAttrQueryBldr = new QueryBuilder(CIERP.DocumentType);
            docTypeAttrQueryBldr.addWhereAttrEqValue(CIERP.DocumentType.Configuration,
                            DocTypeConfiguration.PROFESSIONALSERVICE);
            final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Document2DocumentType);
            attrQueryBldr.addWhereAttrInQuery(CISales.Document2DocumentType.DocumentTypeLink,
                            docTypeAttrQueryBldr.getAttributeQuery(CIERP.DocumentType.ID));
            _queryBldr.addWhereAttrInQuery(CIERP.DocumentAbstract.ID,
                            attrQueryBldr.getAttributeQuery(CISales.Document2DocumentType.DocumentLink));

        }

        @Override
        protected void addColumnDefinition(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final TextColumnBuilder<String> nameColumn = DynamicReports.col.column(DBProperties
                            .getProperty(ProfServReceiptReport.class.getName() + ".Column.Name"), "name",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> revisionColumn = DynamicReports.col.column(DBProperties
                            .getProperty(ProfServReceiptReport.class.getName() + ".Column.Revision"), "revision",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> typeColumn = DynamicReports.col.column(DBProperties
                            .getProperty(ProfServReceiptReport.class.getName() + ".Column.Type"), "typeLabel",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> contactNameColumn = DynamicReports.col.column(DBProperties
                            .getProperty(ProfServReceiptReport.class.getName() + ".Column.ContactName"), "contactName",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<DateTime> dateColumn = AbstractDynamicReport_Base.column(
                            DBProperties.getProperty(ProfServReceiptReport.class.getName() + ".Column.Date"), "date",
                            DateTimeDate.get());
            final TextColumnBuilder<DateTime> monthColumn = AbstractDynamicReport_Base.column(
                            DBProperties.getProperty(ProfServReceiptReport.class.getName() + ".Column.Month"), "date",
                            DateTimeMonth.get());
            final TextColumnBuilder<BigDecimal> crossTotalColumn = DynamicReports.col.column(DBProperties
                            .getProperty(ProfServReceiptReport.class.getName() + ".Column.CrossTotal"), "crossTotal",
                            DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<String> statusColumn = DynamicReports.col.column(DBProperties
                            .getProperty(ProfServReceiptReport.class.getName() + ".Column.Status"), "statusLabel",
                            DynamicReports.type.stringType());

            final GenericElementBuilder linkElement = DynamicReports.cmp.genericElement(
                            "http://www.efaps.org", "efapslink")
                            .addParameter(EmbeddedLink.JASPER_PARAMETERKEY, new LinkExpression())
                            .setHeight(12).setWidth(25);
            final ComponentColumnBuilder linkColumn = DynamicReports.col.componentColumn(linkElement).setTitle("");
            if (getExType().equals(ExportType.HTML)) {
                _builder.addColumn(linkColumn);
            }

            final ColumnGroupBuilder contactGroup = DynamicReports.grp.group(contactNameColumn).groupByDataType();
            final ColumnGroupBuilder monthGroup = DynamicReports.grp.group(monthColumn).groupByDataType();

            _builder.addGroup(monthGroup, contactGroup);
            final AggregationSubtotalBuilder<BigDecimal> crossTotalSum = DynamicReports.sbt.sum(crossTotalColumn);

            _builder.addColumn(monthColumn, contactNameColumn, typeColumn, revisionColumn,
                            nameColumn, dateColumn, crossTotalColumn, statusColumn);
            _builder.addSubtotalAtGroupFooter(contactGroup, crossTotalSum);
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
        protected ProfServReceiptReport_Base getFilteredReport()
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
         * Name.
         */
        private String name;

        /**
         * Label of the type.
         */
        private String typeLabel;

        /**
         * Name of the contact.
         */
        private String contactName;

        /**
         * Date.
         */
        private DateTime date;

        /**
         * CrossTotal.
         */
        private BigDecimal crossTotal;

        /**
         * Revision.
         */
        private String revision;

        /**
         * Status label.
         */
        private String statusLabel;

        /**
         * Getter method for the instance variable {@link #name}.
         *
         * @return value of instance variable {@link #name}
         */
        public String getName()
        {
            return this.name;
        }

        /**
         * Setter method for instance variable {@link #name}.
         *
         * @param _name value for instance variable {@link #name}
         */
        public void setName(final String _name)
        {
            this.name = _name;
        }

        /**
         * Getter method for the instance variable {@link #typeLabel}.
         *
         * @return value of instance variable {@link #typeLabel}
         */
        public String getTypeLabel()
        {
            return this.typeLabel;
        }

        /**
         * Setter method for instance variable {@link #typeLabel}.
         *
         * @param _typeLabel value for instance variable {@link #typeLabel}
         */
        public void setTypeLabel(final String _typeLabel)
        {
            this.typeLabel = _typeLabel;
        }

        /**
         * Getter method for the instance variable {@link #contactName}.
         *
         * @return value of instance variable {@link #contactName}
         */
        public String getContactName()
        {
            return this.contactName;
        }

        /**
         * Setter method for instance variable {@link #contactName}.
         *
         * @param _contactName value for instance variable {@link #contactName}
         */
        public void setContactName(final String _contactName)
        {
            this.contactName = _contactName;
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
         * Getter method for the instance variable {@link #crossTotal}.
         *
         * @return value of instance variable {@link #crossTotal}
         */
        public BigDecimal getCrossTotal()
        {
            return this.crossTotal;
        }


        /**
         * Setter method for instance variable {@link #crossTotal}.
         *
         * @param _crossTotal value for instance variable {@link #crossTotal}
         */
        public void setCrossTotal(final BigDecimal _crossTotal)
        {
            this.crossTotal = _crossTotal;
        }


        /**
         * Getter method for the instance variable {@link #revision}.
         *
         * @return value of instance variable {@link #revision}
         */
        public String getRevision()
        {
            return this.revision;
        }


        /**
         * Setter method for instance variable {@link #revision}.
         *
         * @param _revision value for instance variable {@link #revision}
         */
        public void setRevision(final String _revision)
        {
            this.revision = _revision;
        }


        /**
         * Getter method for the instance variable {@link #statusLabel}.
         *
         * @return value of instance variable {@link #statusLabel}
         */
        public String getStatusLabel()
        {
            return this.statusLabel;
        }


        /**
         * Setter method for instance variable {@link #statusLabel}.
         *
         * @param _statusLabel value for instance variable {@link #statusLabel}
         */
        public void setStatusLabel(final String _statusLabel)
        {
            this.statusLabel = _statusLabel;
        }


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
    }
}
