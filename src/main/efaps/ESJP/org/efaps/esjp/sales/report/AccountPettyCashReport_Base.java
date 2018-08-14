/*
 * Copyright 2003 - 2017 The eFaps Team
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

import java.awt.Color;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.collections4.map.LazyMap;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.CachedPrintQuery;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIMsgHumanResource;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport_Base;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeDate;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.component.TextFieldBuilder;
import net.sf.dynamicreports.report.builder.group.ColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.subtotal.AggregationSubtotalBuilder;
import net.sf.dynamicreports.report.constant.HorizontalTextAlignment;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

/**
 * Report used to analyze Petty Cash.
 *
 * @author The eFaps Team
 */
@EFapsUUID("862bf037-e651-49a9-9b37-e0288c1a6e68")
@EFapsApplication("eFapsApp-Sales")
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

        /**
         * Show details.
         *
         * @return true, if successful
         */
        protected boolean showDetails()
        {
            return true;
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
                    LOG.error("Catched JRException", e);
                }
            } else {
                final SelectBuilder selCurInst = new SelectBuilder()
                                .linkto(CISales.DocumentSumAbstract.RateCurrencyId).instance();
                final SelectBuilder selAccInst = new SelectBuilder()
                                .linkfrom(CISales.Account2DocumentWithTrans.ToLinkAbstract)
                                .linkto(CISales.Account2DocumentWithTrans.FromLinkAbstract)
                                .instance();
                final SelectBuilder selAccName = new SelectBuilder()
                                .linkfrom(CISales.Account2DocumentWithTrans.ToLinkAbstract)
                                .linkto(CISales.Account2DocumentWithTrans.FromLinkAbstract)
                                .attribute(CISales.AccountPettyCash.Name);
                final SelectBuilder selDocTypeName = new SelectBuilder()
                                .linkfrom(CISales.Document2DocumentType.DocumentLink)
                                .linkto(CISales.Document2DocumentType.DocumentTypeLink)
                                .attribute(CIERP.DocumentType.Name);
                final SelectBuilder selActionName = new SelectBuilder()
                                .linkfrom(CISales.ActionDefinitionPettyCashReceipt2Document.ToLink)
                                .linkto(CISales.ActionDefinitionPettyCashReceipt2Document.FromLink)
                                .attribute(CISales.ActionDefinitionPettyCashReceipt.Name);
                final SelectBuilder selContactName = SelectBuilder.get().linkto(CISales.DocumentSumAbstract.Contact)
                                .attribute(CIContacts.ContactAbstract.Name);
                final SelectBuilder selEmployee = SelectBuilder.get()
                                .linkfrom(CISales.Employee2PettyCashReceipt.ToLink)
                                .linkto(CISales.Employee2PettyCashReceipt.FromLink);

                final List<DocumentBean> datasource = new ArrayList<>();
                final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter);
                add2QueryBldr(_parameter, queryBldr);
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addSelect(selAccInst, selAccName, selDocTypeName, selActionName, selCurInst, selContactName);
                if (Sales.PETTYCASHRECEIPT_ASSEMPLOYEE.get()) {
                    multi.addMsgPhrase(selEmployee, CIMsgHumanResource.EmployeeWithNumberMsgPhrase);
                }
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
                    final DocumentBean bean = getBean(_parameter)
                                    .setInstance(multi.getCurrentInstance())
                                    .setPettyCashInstance(multi.<Instance>getSelect(selAccInst))
                                    .setPettyCash(multi.getSelect(selAccName))
                                    .setAction(multi.<String>getSelect(selActionName))
                                    .setCross(cross)
                                    .setNet(net)
                                    .setCurrencyInstance(multi.<Instance>getSelect(selCurInst))
                                    .setDocTypeName(multi.<String>getSelect(selDocTypeName))
                                    .setDocName(multi.<String>getAttribute(CISales.DocumentSumAbstract.Name))
                                    .setContactName(multi.<String>getSelect(selContactName))
                                    .setDate(multi.<DateTime>getAttribute(CISales.DocumentSumAbstract.Date))
                                    .setSwitch(isGroup(_parameter))
                                    .setBaseNet(basenet)
                                    .setBaseCross(basecross);
                    if (Sales.PETTYCASHRECEIPT_ASSEMPLOYEE.get()) {
                        bean.setEmployee(multi.getMsgPhrase(selEmployee,
                                        CIMsgHumanResource.EmployeeWithNumberMsgPhrase));
                    }
                    datasource.add(bean);
                }
                final ComparatorChain<DocumentBean> chain = new ComparatorChain<>();
                chain.addComparator((_o1, _o2) -> _o1.getPettyCash().compareTo(_o2.getPettyCash()));
                if (isGroup(_parameter)) {
                    chain.addComparator((_o1, _o2) -> {
                        int ret1 = 0;
                        try {
                            ret1 =  _o1.getLiquidation().compareTo(_o2.getLiquidation());
                        } catch (final EFapsException e) {
                            LOG.error("Catched", e);
                        }
                        return ret1;
                    });
                }
                chain.addComparator((_o1, _o2) -> _o1.getOfficial().compareTo(_o2.getOfficial()));
                chain.addComparator((_o1, _o2) -> _o1.getAction().compareTo(_o2.getAction()));
                Collections.sort(datasource, chain);

                final Collection<Map<String, ?>> col = new ArrayList<>();
                for (final DocumentBean bean : datasource) {
                    col.add(bean.getMap());
                }
                ret = new JRMapCollectionDataSource(col);
                getFilteredReport().cache(_parameter, ret);
            }
            return ret;
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

            final Instance instance = _parameter.getInstance();
            final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Account2DocumentWithTrans);
            attrQueryBldr.addWhereAttrNotEqValue(CISales.Account2DocumentWithTrans.Type,
                            CISales.AccountFundsToBeSettled2FundsToBeSettledReceipt.getType().getId(),
                            CISales.AccountFundsToBeSettled2IncomingCreditNote.getType().getId());
            if (InstanceUtils.isType(instance, CISales.AccountPettyCash)) {
                attrQueryBldr.addWhereAttrEqValue(CISales.Account2DocumentWithTrans.FromLinkAbstract, instance);
            }
            final AttributeQuery attrQuery = attrQueryBldr
                            .getAttributeQuery(CISales.Account2DocumentWithTrans.ToLinkAbstract);
            _queryBldr.addWhereAttrInQuery(CISales.DocumentSumAbstract.ID, attrQuery);
        }

        @Override
        protected void addColumnDefinition(final Parameter _parameter,
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
                            "contactName", DynamicReports.type.stringType()).setWidth(150);
            final TextColumnBuilder<String> employeeColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AccountPettyCashReport.class.getName() + ".Column.Employee"), "employee",
                            DynamicReports.type.stringType()).setWidth(150);

            final TextColumnBuilder<DateTime> dateColumn = AbstractDynamicReport_Base.column(
                            DBProperties.getProperty(AccountPettyCashReport.class.getName() + ".Column.Date"), "date",
                            DateTimeDate.get());

            final ColumnGroupBuilder pettyCashGroup = DynamicReports.grp.group(pettyCashColumn).groupByDataType()
                            .setStyle(DynamicReports.stl.style().setBackgroundColor(Color.YELLOW));
            final ColumnGroupBuilder liquidationGroup = DynamicReports.grp.group(liquidationColumn).groupByDataType();
            final ColumnGroupBuilder officialGroup = DynamicReports.grp.group(officialColumn).groupByDataType();
            final ColumnGroupBuilder actionGroup = DynamicReports.grp.group(actionColumn).groupByDataType();

            final TextFieldBuilder<String> pettyCashTotalBldr = DynamicReports.cmp.text(
                            DynamicReports.<String>field("pettyCashResume", String.class))
                            .setStyle(DynamicReports.stl.style().boldItalic()
                                            .setHorizontalTextAlignment(HorizontalTextAlignment.CENTER));
            pettyCashGroup.footer(pettyCashTotalBldr);

            _builder.addGroup(pettyCashGroup).addColumn(pettyCashColumn);
            if (isGroup(_parameter)) {
                _builder.addGroup(liquidationGroup).addColumn(liquidationColumn);
            }
            _builder.addGroup(officialGroup, actionGroup).addColumn(officialColumn, actionColumn);

            if (showDetails()) {
                if (getExType().equals(ExportType.HTML)) {
                    _builder.addColumn(FilteredReport.getLinkColumn(_parameter, "oid"));
                }
                _builder.addColumn(docNameColumn, dateColumn, contactNameColumn);
                if (Sales.PETTYCASHRECEIPT_ASSEMPLOYEE.get()) {
                    _builder.addColumn(employeeColumn);
                }
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

            final TextColumnBuilder<String> subTotalTtextColumn = Sales.PETTYCASHRECEIPT_ASSEMPLOYEE.get()
                            ? employeeColumn : contactNameColumn;
            _builder.addSubtotalAtGroupFooter(pettyCashGroup, FilteredReport.getCustomTextSubtotalBuilder(
                            _parameter, "pettyCash", subTotalTtextColumn), amountSum);

            if (isGroup(_parameter)) {
                _builder.addSubtotalAtGroupFooter(liquidationGroup, FilteredReport.getCustomTextSubtotalBuilder(
                                _parameter, "liquidation", subTotalTtextColumn), amountSum1);
            }

            _builder.addSubtotalAtGroupFooter(officialGroup, FilteredReport.getCustomTextSubtotalBuilder(_parameter,
                            "official", subTotalTtextColumn), amountSum2);
            _builder.addSubtotalAtGroupFooter(actionGroup, FilteredReport.getCustomTextSubtotalBuilder(_parameter,
                            "action", subTotalTtextColumn), amountSum3);
        }

        /**
         * Checks if is group.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return true, if is group
         * @throws EFapsException on error
         */
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

        /** The contact name. */
        private String contactName;

        /** The contact name. */
        private String employee;

        /**
         * Amount.
         */
        private BigDecimal cross;

        /**
         * Amount.
         */
        private BigDecimal net;

        /** The petty cash instance. */
        private Instance pettyCashInstance;

        /** The petty cash. */
        private String pettyCash;

        /**
         * Currency.
         */
        private Instance currencyInstance;

        /** The doc name. */
        private String docName;

        /** The doc type name. */
        private String docTypeName;

        /** The date. */
        private DateTime date;

        /** The is group. */
        private boolean isGroup;

        /** The liquidation. */
        private String liquidation;

        /** The base cross. */
        private BigDecimal baseCross;

        /** The base net. */
        private BigDecimal baseNet;

        /**
         * Getter method for the instance variable {@link #pettyCash}.
         *
         * @return value of instance variable {@link #pettyCash}
         */
        public String getPettyCash()
        {
            return this.pettyCash;
        }

        /**
         * Gets the petty cash resume.
         *
         * @return the petty cash resume
         */
        public String getPettyCashResume()
        {
            String ret = null;
            try {
                BigDecimal startAmount = BigDecimal.ZERO;
                String currency = "";
                final PrintQuery print = CachedPrintQuery.get4Request(getPettyCashInstance());
                final SelectBuilder sel = SelectBuilder.get().linkto(CISales.AccountPettyCash.CurrencyLink).instance();
                print.addSelect(sel);
                print.addAttribute(CISales.AccountPettyCash.AmountAbstract);
                if (print.execute()) {
                    startAmount = print.getAttribute(CISales.AccountPettyCash.AmountAbstract);
                    currency = CurrencyInst.get(print.<Instance>getSelect(sel)).getSymbol();
                }

                final QueryBuilder queryBldr = new QueryBuilder(CISales.Balance);
                queryBldr.addWhereAttrEqValue(CISales.Balance.Account, getPettyCashInstance());
                final MultiPrintQuery multi = queryBldr.getCachedPrint4Request();
                final SelectBuilder curSel = new SelectBuilder().linkto(CISales.Balance.Currency).attribute(
                                CIERP.Currency.Symbol);
                multi.addSelect(curSel);
                multi.addAttribute(CISales.Balance.Amount);
                multi.execute();

                BigDecimal currentAmount = null;
                while (multi.next()) {
                    currentAmount = multi.<BigDecimal>getAttribute(CISales.Balance.Amount);
                }
                ret = DBProperties.getFormatedDBProperty(AccountPettyCashReport.class.getName() + ".PettyCashResume",
                                (Object) getPettyCash(), currency, startAmount, currentAmount);
            } catch (final EFapsException e) {
                AccountPettyCashReport_Base.LOG.error("Catched error on evalutaion of PettyCashResume", e);
            }
            return ret;
        }

        /**
         * Sets the switch.
         *
         * @param _isGroup the new switch
         * @return the document bean
         */
        public DocumentBean setSwitch(final boolean _isGroup)
        {
            this.isGroup = _isGroup;
            return this;
        }

        /**
         * Gets the switch.
         *
         * @return the switch
         */
        public Boolean getSwitch()
        {
            return this.isGroup;
        }

        /**
         * Gets the map.
         *
         * @return the map
         * @throws EFapsException on error
         */
        public Map<String, ?> getMap()
            throws EFapsException
        {
            final Transformer<String, Object> transformer = _input -> {
                Object ret = null;
                switch (_input) {
                    case "pettyCash":
                        ret = getPettyCash();
                        break;
                    case "liquidation":
                        try {
                            ret = getLiquidation();
                        } catch (final EFapsException e1) {
                            AccountPettyCashReport_Base.LOG.error("Catched", e1);
                        }
                        break;
                    case "pettyCashResume":
                        ret = getPettyCashResume();
                        break;
                    case "official":
                        ret = getOfficial();
                        break;
                    case "action":
                        ret = getAction();
                        break;
                    case "docName":
                        ret = getDocName();
                        break;
                    case "oid":
                        ret = getOid();
                        break;
                    case "date":
                        ret = getDate();
                        break;
                    case "baseAmount":
                        ret = getBaseAmount();
                        break;
                    case "contactName":
                        ret =  getContactName();
                        break;
                    case "employee":
                        ret = getEmployee();
                        break;
                    default:
                        try {
                            if (_input.equals(CurrencyInst.get(getCurrencyInstance()).getISOCode())) {
                                ret = getCross();
                            }
                        } catch (final EFapsException e2) {
                            AccountPettyCashReport_Base.LOG.error("Catched", e2);
                        }
                        break;
                }
                return ret;
            };
            final Map<String, Object> ret = LazyMap.lazyMap(new HashMap<String, Object>(), transformer);
            return ret;
        }

        /**
         * Gets the base amount.
         *
         * @return the base amount
         */
        private BigDecimal getBaseAmount()
        {
            return this.baseCross;
        }

        /**
         * Sets the petty cash instance.
         *
         * @param _pettyCashInstance the new petty cash instance
         * @return the document bean
         */
        public DocumentBean setPettyCashInstance(final Instance _pettyCashInstance)
        {
            this.pettyCashInstance = _pettyCashInstance;
            return this;
        }

        /**
         * Sets the doc type name.
         *
         * @param _docTypeName the new doc type name
         * @return the document bean
         */
        public DocumentBean setDocTypeName(final String _docTypeName)
        {
            this.docTypeName = _docTypeName;
            return this;
        }

        /**
         * Setter method for instance variable {@link #pettyCash}.
         *
         * @param _name value for instance variable {@link #pettyCash}
         * @return the document bean
         */
        public DocumentBean setPettyCash(final String _name)
        {
            this.pettyCash = _name;
            return this;
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
         * @return the document bean
         */
        public DocumentBean setTypeLabel(final String _typeLabel)
        {
            this.typeLabel = _typeLabel;
            return this;
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

        /**
         * Gets the official.
         *
         * @return the official
         */
        public String getOfficial()
        {
            final String ret;
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
         * @param _action the new actionDef of the document
         * @return the document bean
         */
        public DocumentBean setAction(final String _action)
        {
            this.action = _action;
            return this;
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
         * @param _instance the new oID of the document
         * @return the document bean
         */
        public DocumentBean setInstance(final Instance _instance)
        {
            this.instance = _instance;
            return this;
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
         * @return the document bean
         */
        public DocumentBean setDocName(final String _docName)
        {
            this.docName = _docName;
            return this;
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
         * @return the document bean
         */
        public DocumentBean setContactName(final String _contactName)
        {
            this.contactName = _contactName;
            return this;
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
         * @return the document bean
         */
        public DocumentBean setCross(final BigDecimal _cross)
        {
            this.cross = _cross;
            return this;
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
         * @return the document bean
         */
        public DocumentBean setNet(final BigDecimal _net)
        {
            this.net = _net;
            return this;
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
         * @return the document bean
         */
        public DocumentBean setCurrencyInstance(final Instance _currencyInstance)
        {
            this.currencyInstance = _currencyInstance;
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
         * @return the document bean
         */
        public DocumentBean setDate(final DateTime _date)
        {
            this.date = _date;
            return this;
        }

        /**
         * Gets the liquidation.
         *
         * @return the liquidation
         * @throws EFapsException on error
         */
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
                final MultiPrintQuery multi = queryBldr.getCachedPrint4Request();
                multi.addAttribute(CISales.PettyCashBalance.Name);
                multi.execute();
                if (multi.next()) {
                    this.liquidation = multi.<String>getAttribute(CISales.PettyCashBalance.Name);
                }
            }
            return this.liquidation == null
                            ? DBProperties.getProperty(AccountPettyCashReport.class.getName() + ".WithoutLiquidation")
                                            : this.liquidation;
        }

        /**
         * Sets the base net.
         *
         * @param _baseNet the new base net
         * @return the document bean
         */
        public DocumentBean setBaseNet(final BigDecimal _baseNet)
        {
            this.baseNet = _baseNet;
            return this;
        }

        /**
         * Sets the base cross.
         *
         * @param _baseCross the new base cross
         * @return the document bean
         */
        public DocumentBean setBaseCross(final BigDecimal _baseCross)
        {
            this.baseCross = _baseCross;
            return this;
        }

        /**
         * Gets the base net.
         *
         * @return the base net
         */
        public BigDecimal getBaseNet()
        {
            return this.baseNet;
        }

        /**
         * Gets the base cross.
         *
         * @return the base cross
         */
        public BigDecimal getBaseCross()
        {
            return this.baseCross;
        }

        /**
         * Getter method for the instance variable {@link #employee}.
         *
         * @return value of instance variable {@link #employee}
         */
        public String getEmployee()
        {
            return this.employee;
        }

        /**
         * Setter method for instance variable {@link #employee}.
         *
         * @param _employee value for instance variable {@link #employee}
         * @return the document bean
         */
        public DocumentBean setEmployee(final String _employee)
        {
            this.employee = _employee;
            return this;
        }

        @Override
        public String toString()
        {
            return ToStringBuilder.reflectionToString(this);
        }
    }
}
