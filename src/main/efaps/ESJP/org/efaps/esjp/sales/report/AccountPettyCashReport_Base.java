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

import java.awt.Color;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.lang3.BooleanUtils;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport_Base;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeDate;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.ui.wicket.models.EmbeddedLink;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Report used to analyze receipt for professional fees (Recibos Honorarios) by
 * grouping them by month and contact.
 *
 * @author The eFaps Team
 * @version $Id: AccountPettyCashReport_Base.java 13616 2014-08-13 22:10:22Z
 *          m.aranya@moxter.net $
 */
@EFapsUUID("862bf037-e651-49a9-9b37-e0288c1a6e68")
@EFapsRevision("$Rev$")
public abstract class AccountPettyCashReport_Base
    extends FilteredReport
{

    /**
     * Logging instance used in this class.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(AccountPettyCashReport.class);

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
        dyRp.setFileName(DBProperties.getProperty(AccountPettyCashReport.class.getName() + ".FileName"));
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
        return new DynAccountPettyCashReport(this);
    }

    /**
     * Report class.
     */
    public static class DynAccountPettyCashReport
        extends AbstractDynamicReport
    {

        /**
         * variable to report.
         */
        private final AccountPettyCashReport_Base filteredReport;

        /**
         * @param _report class used
         */
        public DynAccountPettyCashReport(final AccountPettyCashReport_Base _report)
        {
            this.filteredReport = _report;
        }

        protected boolean showDetails()
        {
            return true;
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final SelectBuilder selCurInst = new SelectBuilder()
                            .linkto(CISales.DocumentSumAbstract.RateCurrencyId).instance();
            final SelectBuilder selAccInst = new SelectBuilder()
                            .linkfrom(CISales.Account2DocumentWithTrans.ToLinkAbstract)
                            .linkto(CISales.Account2DocumentWithTrans.FromLinkAbstract)
                            .instance();
            final SelectBuilder selDocTypeName = new SelectBuilder()
                            .linkfrom(CISales.Document2DocumentType.DocumentLink)
                            .linkto(CISales.Document2DocumentType.DocumentTypeLink).attribute(CIERP.DocumentType.Name);
            final SelectBuilder selActionName = new SelectBuilder()
                            .linkfrom(CISales.ActionDefinitionPettyCashReceipt2Document.ToLink)
                            .linkto(CISales.ActionDefinitionPettyCashReceipt2Document.FromLink)
                            .attribute(CISales.ActionDefinitionPettyCashReceipt.Name);
            final SelectBuilder selContactName = SelectBuilder.get().linkto(CISales.DocumentSumAbstract.Contact)
                            .attribute(CIContacts.ContactAbstract.Name);
            final List<DocumentBean> datasource = new ArrayList<DocumentBean>();
            final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter);
            add2QueryBldr(_parameter, queryBldr);
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addSelect(selAccInst, selDocTypeName, selActionName, selCurInst, selContactName);
            multi.addAttribute(CISales.DocumentSumAbstract.RateNetTotal, CISales.DocumentSumAbstract.Date,
                            CISales.DocumentSumAbstract.RateCrossTotal, CISales.DocumentSumAbstract.Name,
                            CISales.DocumentSumAbstract.NetTotal, CISales.DocumentSumAbstract.CrossTotal);
            multi.execute();
            while (multi.next()) {
                BigDecimal cross = multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
                BigDecimal net = multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateNetTotal);

                BigDecimal basecross = multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.CrossTotal);
                BigDecimal basenet = multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.NetTotal);
                if (multi.getCurrentInstance().getType().isKindOf(CISales.IncomingCreditNote.getType())) {
                    cross = cross.negate();
                    net = net.negate();

                    basecross = cross.negate();
                    basenet = net.negate();
                }
                final DocumentBean bean = getBean(_parameter);
                datasource.add(bean);
                bean.setInstance(multi.getCurrentInstance());
                bean.setPettyCashInstance(multi.<Instance>getSelect(selAccInst));
                bean.setAction(multi.<String>getSelect(selActionName));
                bean.setCross(cross);
                bean.setNet(net);
                bean.setCurrencyInstance(multi.<Instance>getSelect(selCurInst));
                bean.setDocTypeName(multi.<String>getSelect(selDocTypeName));
                bean.setDocName(multi.<String>getAttribute(CISales.DocumentSumAbstract.Name));
                bean.setContactName(multi.<String>getSelect(selContactName));
                bean.setDate(multi.<DateTime>getAttribute(CISales.DocumentSumAbstract.Date));
                bean.setSwitch(isGroup(_parameter));
                bean.setBaseNet(basenet);
                bean.setBaseCross(basecross);
            }
            final ComparatorChain<DocumentBean> chain = new ComparatorChain<DocumentBean>();
            chain.addComparator(new Comparator<DocumentBean>()
            {

                @Override
                public int compare(final DocumentBean _o1,
                                   final DocumentBean _o2)
                {
                    return _o1.getPettyCash().compareTo(_o2.getPettyCash());
                }
            });
            if (isGroup(_parameter)) {
                chain.addComparator(new Comparator<DocumentBean>()
                {

                    @Override
                    public int compare(final DocumentBean _o1,
                                       final DocumentBean _o2)
                    {
                        int ret = 0;
                        try {
                            ret =  _o1.getLiquidation().compareTo(_o2.getLiquidation());
                        } catch (final EFapsException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        return ret;
                    }
                });
            }
            chain.addComparator(new Comparator<DocumentBean>()
            {

                @Override
                public int compare(final DocumentBean _o1,
                                   final DocumentBean _o2)
                {
                    return _o1.getOfficial().compareTo(_o2.getOfficial());
                }
            });
            chain.addComparator(new Comparator<DocumentBean>()
            {

                @Override
                public int compare(final DocumentBean _o1,
                                   final DocumentBean _o2)
                {
                    return _o1.getAction().compareTo(_o2.getAction());
                }
            });
            Collections.sort(datasource, chain);

            final Collection<Map<String, ?>> col = new ArrayList<>();
            for (final DocumentBean bean : datasource) {
                col.add(bean.getMap());
            }

            return new JRMapCollectionDataSource(col);
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
                _queryBldr.addWhereAttrGreaterValue(CIERP.DocumentAbstract.Created,
                                date.withTimeAtStartOfDay().minusSeconds(1));
            }
            if (filter.containsKey("dateTo")) {
                final DateTime date = (DateTime) filter.get("dateTo");
                _queryBldr.addWhereAttrLessValue(CIERP.DocumentAbstract.Created,
                                date.withTimeAtStartOfDay().plusDays(1));
            }

            final Instance instance = _parameter.getInstance();
            final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Account2DocumentWithTrans);
            attrQueryBldr.addWhereAttrNotEqValue(CISales.Account2DocumentWithTrans.Type,
                            CISales.AccountFundsToBeSettled2FundsToBeSettledReceipt.getType().getId(),
                            CISales.AccountFundsToBeSettled2IncomingCreditNote.getType().getId());
            if (instance != null && instance.isValid()
                            && CISales.AccountPettyCash.getType().equals(instance.getType())) {
                attrQueryBldr.addWhereAttrEqValue(CISales.Account2DocumentWithTrans.FromLinkAbstract, instance);
            }
            final AttributeQuery attrQuery = attrQueryBldr
                            .getAttributeQuery(CISales.Account2DocumentWithTrans.ToLinkAbstract);
            _queryBldr.addWhereAttrInQuery(CISales.DocumentSumAbstract.ID, attrQuery);
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final TextColumnBuilder<String> pettyCashColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AccountPettyCashReport.class.getName() + ".Column.PettyCash"), "pettyCash",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> liquidationColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AccountPettyCashReport.class.getName() + ".Column.Liquidation"), "liquidation",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> officialColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AccountPettyCashReport.class.getName() + ".Column.Official"), "official",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> actionColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AccountPettyCashReport.class.getName() + ".Column.Action"), "action",
                            DynamicReports.type.stringType());

            final TextColumnBuilder<String> docNameColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AccountPettyCashReport.class.getName() + ".Column.DocName"), "docName",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> contactNameColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AccountPettyCashReport.class.getName() + ".Column.ContactName"),
                            "contactName",
                            DynamicReports.type.stringType()).setWidth(150);
            final TextColumnBuilder<DateTime> dateColumn = AbstractDynamicReport_Base.column(
                            DBProperties.getProperty(AccountPettyCashReport.class.getName() + ".Column.Date"), "date",
                            DateTimeDate.get());
            final GenericElementBuilder linkElement = DynamicReports.cmp.genericElement(
                            "http://www.efaps.org", "efapslink")
                            .addParameter(EmbeddedLink.JASPER_PARAMETERKEY, new LinkExpression())
                            .setHeight(12).setWidth(25);
            final ComponentColumnBuilder linkColumn = DynamicReports.col.componentColumn(linkElement).setTitle("");

            final ColumnGroupBuilder pettyCashGroup = DynamicReports.grp.group(pettyCashColumn).groupByDataType()
                            .setStyle(DynamicReports.stl.style().setBackgroundColor(Color.YELLOW));
            final ColumnGroupBuilder liquidationGroup = DynamicReports.grp.group(liquidationColumn).groupByDataType();
            final ColumnGroupBuilder officialGroup = DynamicReports.grp.group(officialColumn).groupByDataType();
            final ColumnGroupBuilder actionGroup = DynamicReports.grp.group(actionColumn).groupByDataType();

            _builder.addGroup(pettyCashGroup).addColumn(pettyCashColumn);
            if (isGroup(_parameter)) {
                _builder.addGroup(liquidationGroup).addColumn(liquidationColumn);
            }
            _builder.addGroup(officialGroup, actionGroup).addColumn(officialColumn, actionColumn);

            if (showDetails()) {
                if (getExType().equals(ExportType.HTML)) {
                    _builder.addColumn(linkColumn);
                }
                _builder.addColumn(docNameColumn, dateColumn, contactNameColumn);
            }
            for (final CurrencyInst currency : CurrencyInst.getAvailable()) {
                final TextColumnBuilder<BigDecimal> amountColumn = DynamicReports.col.column(currency.getName(),
                                currency.getISOCode(), DynamicReports.type.bigDecimalType());

                _builder.addColumn(amountColumn);

                final AggregationSubtotalBuilder<BigDecimal> amountSum = DynamicReports.sbt.sum(amountColumn);
                final AggregationSubtotalBuilder<BigDecimal> amountSum1 = DynamicReports.sbt.sum(amountColumn);
                final AggregationSubtotalBuilder<BigDecimal> amountSum2 = DynamicReports.sbt.sum(amountColumn);
                final AggregationSubtotalBuilder<BigDecimal> amountSum3 = DynamicReports.sbt.sum(amountColumn);

                _builder.addSubtotalAtGroupFooter(pettyCashGroup, amountSum);

                if (isGroup(_parameter)) {
                    _builder.addSubtotalAtGroupFooter(liquidationGroup, amountSum1);
                }

                _builder.addSubtotalAtGroupFooter(officialGroup, amountSum2);
                _builder.addSubtotalAtGroupFooter(actionGroup, amountSum3);
            }

            final TextColumnBuilder<BigDecimal> baseAmountColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AccountPettyCashReport.class.getName() + ".Column.BaseAmount"), "baseAmount",
                            DynamicReports.type.bigDecimalType());
            _builder.addColumn(baseAmountColumn);

            final AggregationSubtotalBuilder<BigDecimal> amountSum = DynamicReports.sbt.sum(baseAmountColumn);
            final AggregationSubtotalBuilder<BigDecimal> amountSum1 = DynamicReports.sbt.sum(baseAmountColumn);
            final AggregationSubtotalBuilder<BigDecimal> amountSum2 = DynamicReports.sbt.sum(baseAmountColumn);
            final AggregationSubtotalBuilder<BigDecimal> amountSum3 = DynamicReports.sbt.sum(baseAmountColumn);

            _builder.addSubtotalAtGroupFooter(pettyCashGroup, amountSum);

            if (isGroup(_parameter)) {
                _builder.addSubtotalAtGroupFooter(liquidationGroup, amountSum1);
            }

            _builder.addSubtotalAtGroupFooter(officialGroup, amountSum2);
            _builder.addSubtotalAtGroupFooter(actionGroup, amountSum3);
        }

        protected boolean isGroup(final Parameter _parameter)
            throws EFapsException
        {
            final Map<String, Object> filter = this.filteredReport.getFilterMap(_parameter);
            return BooleanUtils.isTrue((Boolean) filter.get("switch"));
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
        protected AccountPettyCashReport_Base getFilteredReport()
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
        private Instance instance;

        /**
         * Label of the type.
         */
        private String typeLabel;

        /**
         * ActionDef of the document.
         */
        private String action;

        private String contactName;

        /**
         * Amount.
         */
        private BigDecimal cross;

        /**
         * Amount.
         */
        private BigDecimal net;

        private Instance pettyCashInstance;

        /**
         * Currency.
         */
        private Instance currencyInstance;

        private String docName;

        private String docTypeName;

        private DateTime date;

        private boolean isGroup;

        private String liquidation;

        private BigDecimal baseCross;

        private BigDecimal baseNet;

        /**
         * Getter method for the instance variable {@link #pettyCash}.
         *
         * @return value of instance variable {@link #pettyCash}
         */
        public String getPettyCash()

        {
            final StringBuilder ret = new StringBuilder();

            try {
                final PrintQuery print = new PrintQuery(getPettyCashInstance());
                final SelectBuilder sel = SelectBuilder.get().linkto(CISales.AccountPettyCash.CurrencyLink).instance();
                print.addSelect(sel);
                print.addAttribute(CISales.AccountPettyCash.Name, CISales.AccountPettyCash.AmountAbstract);
                print.execute();
                ret.append(print.getAttribute(CISales.AccountPettyCash.Name))
                                .append(" - ").append(print.getAttribute(CISales.AccountPettyCash.AmountAbstract))
                                .append(CurrencyInst.get(print.getSelect(sel)).getSymbol());
            } catch (final EFapsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return ret.toString();
        }

        /**
         * @param _group
         */
        public void setSwitch(final boolean _isGroup)
        {
            this.isGroup = _isGroup;
        }

        /**
         * @param _group
         */
        public Boolean getSwitch()
        {
            return this.isGroup;
        }

        /**
         * @return
         */
        public Map<String, ?> getMap()
            throws EFapsException
        {
            final Map<String, Object> ret = new HashMap<>();
            ret.put("pettyCash", getPettyCash());
            if (getSwitch()) {
                ret.put("liquidation", getLiquidation());
            }
            ret.put("official", getOfficial());
            ret.put("action", getAction());
            ret.put("docName", getDocName());
            ret.put("contactName", getContactName());
            ret.put("oid", getOid());
            ret.put("date", getDate());
            ret.put(CurrencyInst.get(getCurrencyInstance()).getISOCode(), getCross());
            ret.put("baseAmount", getBaseAmount());
            return ret;
        }

        /**
         * @return
         */
        private BigDecimal getBaseAmount()
        {
            return this.baseCross;
        }

        /**
         * @param _select
         */
        public void setPettyCashInstance(final Instance _pettyCashInstance)
        {
            this.pettyCashInstance = _pettyCashInstance;
        }

        /**
         * @param _select
         */
        public void setDocTypeName(final String _docTypeName)
        {
            this.docTypeName = _docTypeName;
        }

        /**
         * Setter method for instance variable {@link #pettyCash}.
         *
         * @param _name value for instance variable {@link #pettyCash}
         */
        public void setPettyCash(final String _name)
        {
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
         * Getter method for the instance variable {@link #action}.
         *
         * @return value of instance variable {@link #action}
         */
        public String getAction()
        {
            return this.action == null ? "-" : this.action;
        }

        public String getOfficial()
        {
            String ret;
            if (this.docTypeName == null) {
                ret = DBProperties.getProperty(AccountPettyCashReport.class.getName() + ".NotWithDocument");
            } else {
                ret = DBProperties.getProperty(AccountPettyCashReport.class.getName() + ".WithDocument");
            }
            return ret;
        }

        /**
         * Setter method for instance variable {@link #action}.
         *
         * @param _date value for instance variable {@link #action}
         */
        public void setAction(final String _action)
        {
            this.action = _action;
        }

        /**
         * Getter method for the instance variable {@link #oid}.
         *
         * @return value of instance variable {@link #oid}
         */
        public String getOid()
        {
            return this.instance.getOid();
        }

        /**
         * Setter method for instance variable {@link #oid}.
         *
         * @param _oid value for instance variable {@link #oid}
         */
        public void setInstance(final Instance _instance)
        {
            this.instance = _instance;
        }

        /**
         * Getter method for the instance variable {@link #docName}.
         *
         * @return value of instance variable {@link #docName}
         */
        public String getDocName()
        {
            return this.docName;
        }

        /**
         * Setter method for instance variable {@link #docName}.
         *
         * @param _docName value for instance variable {@link #docName}
         */
        public void setDocName(final String _docName)
        {
            this.docName = _docName;
        }

        /**
         * Getter method for the instance variable {@link #pettyCashInstance}.
         *
         * @return value of instance variable {@link #pettyCashInstance}
         */
        public Instance getPettyCashInstance()
        {
            return this.pettyCashInstance;
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
         * Getter method for the instance variable {@link #cross}.
         *
         * @return value of instance variable {@link #cross}
         */
        public BigDecimal getCross()
        {
            return this.cross;
        }

        /**
         * Setter method for instance variable {@link #cross}.
         *
         * @param _cross value for instance variable {@link #cross}
         */
        public void setCross(final BigDecimal _cross)
        {
            this.cross = _cross;
        }

        /**
         * Getter method for the instance variable {@link #net}.
         *
         * @return value of instance variable {@link #net}
         */
        public BigDecimal getNet()
        {
            return this.net;
        }

        /**
         * Setter method for instance variable {@link #net}.
         *
         * @param _net value for instance variable {@link #net}
         */
        public void setNet(final BigDecimal _net)
        {
            this.net = _net;
        }

        /**
         * Getter method for the instance variable {@link #currencyInstance}.
         *
         * @return value of instance variable {@link #currencyInstance}
         */
        public Instance getCurrencyInstance()
        {
            return this.currencyInstance;
        }

        /**
         * Setter method for instance variable {@link #currencyInstance}.
         *
         * @param _currencyInstance value for instance variable
         *            {@link #currencyInstance}
         */
        public void setCurrencyInstance(final Instance _currencyInstance)
        {
            this.currencyInstance = _currencyInstance;
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

        public String getLiquidation()
            throws EFapsException
        {
            if (this.liquidation == null && this.instance != null && this.instance.isValid()) {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.PettyCashBalance2PettyCashReceipt);
                attrQueryBldr.addType(CISales.PettyCashBalance2IncomingCreditNote);
                attrQueryBldr.addWhereAttrEqValue(CISales.Document2DocumentAbstract.ToAbstractLink, this.instance);
                final AttributeQuery attrQuery =
                                attrQueryBldr.getAttributeQuery(CISales.Document2DocumentAbstract.FromAbstractLink);

                final QueryBuilder queryBldr = new QueryBuilder(CISales.PettyCashBalance);
                queryBldr.addWhereAttrInQuery(CISales.PettyCashBalance.ID, attrQuery);
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CISales.PettyCashBalance.Name);
                multi.execute();

                if (multi.next()) {
                    this.liquidation = multi.<String>getAttribute(CISales.PettyCashBalance.Name);
                }
            }
            return this.liquidation == null ? "" : this.liquidation;
        }

        public void setBaseNet(final BigDecimal _baseNet)
        {
            this.baseNet = _baseNet;
        }

        public void setBaseCross(final BigDecimal _baseCross)
        {
            this.baseCross = _baseCross;
        }

        public BigDecimal getBaseNet()
        {
            return this.baseNet;
        }

        public BigDecimal getBaseCross()
        {
            return this.baseCross;
        }
    }
}
