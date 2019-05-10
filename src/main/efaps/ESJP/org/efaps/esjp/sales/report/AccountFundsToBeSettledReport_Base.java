/*
 * Copyright 2003 - 2019 The eFaps Team
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

import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.collections4.map.LazyMap;
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

/**
 * Report used to analyze FundsToBeSettled.
 *
 * @author The eFaps Team
 */
@EFapsUUID("e16eb9b7-d574-47aa-a3ad-b3e4ca2307b7")
@EFapsApplication("eFapsApp-Sales")
public abstract class AccountFundsToBeSettledReport_Base
    extends FilteredReport
{

    /**
     * Logging instance used in this class.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(AccountFundsToBeSettledReport.class);

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
        dyRp.setFileName(DBProperties.getProperty(AccountFundsToBeSettledReport.class.getName() + ".FileName"));
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
        return new DynAccountFundsToBeSettledReport(this);
    }

    /**
     * Report class.
     */
    public static class DynAccountFundsToBeSettledReport
        extends AbstractDynamicReport
    {

        /**
         * variable to report.
         */
        private final AccountFundsToBeSettledReport_Base filteredReport;

        /**
         * @param _report class used
         */
        public DynAccountFundsToBeSettledReport(final AccountFundsToBeSettledReport_Base _report)
        {
            filteredReport = _report;
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
                                .attribute(CISales.AccountFundsToBeSettled.Name);
                final SelectBuilder selDocTypeName = new SelectBuilder()
                                .linkfrom(CISales.Document2DocumentType.DocumentLink)
                                .linkto(CISales.Document2DocumentType.DocumentTypeLink)
                                .attribute(CIERP.DocumentType.Name);
                final SelectBuilder selActionName = new SelectBuilder()
                                .linkfrom(CISales.ActionDefinitionFundsToBeSettledReceipt2Document.ToLink)
                                .linkto(CISales.ActionDefinitionFundsToBeSettledReceipt2Document.FromLink)
                                .attribute(CISales.ActionDefinitionFundsToBeSettledReceipt.Name);
                final SelectBuilder selContactName = SelectBuilder.get().linkto(CISales.DocumentSumAbstract.Contact)
                                .attribute(CIContacts.ContactAbstract.Name);
                final SelectBuilder selEmployee = SelectBuilder.get()
                                .linkfrom(CISales.Employee2FundsToBeSettledReceipt.ToLink)
                                .linkto(CISales.Employee2FundsToBeSettledReceipt.FromLink);

                final List<DocumentBean> datasource = new ArrayList<>();
                final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter);
                add2QueryBldr(_parameter, queryBldr);
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addSelect(selAccInst, selAccName, selDocTypeName, selActionName, selCurInst, selContactName);
                if (Sales.FUNDSTOBESETTLEDRECEIPT_ASSEMPLOYEE.get()) {
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
                                    .setFundsToBeSettledInstance(multi.<Instance>getSelect(selAccInst))
                                    .setFundsToBeSettled(multi.getSelect(selAccName))
                                    .setAction(multi.<String>getSelect(selActionName))
                                    .setCross(cross)
                                    .setNet(net)
                                    .setCurrencyInstance(multi.<Instance>getSelect(selCurInst))
                                    .setDocTypeName(multi.<String>getSelect(selDocTypeName))
                                    .setDocName(multi.<String>getAttribute(CISales.DocumentSumAbstract.Name))
                                    .setContactName(multi.<String>getSelect(selContactName))
                                    .setDate(multi.<DateTime>getAttribute(CISales.DocumentSumAbstract.Date))
                                    .setBaseNet(basenet)
                                    .setBaseCross(basecross);
                    if (Sales.FUNDSTOBESETTLEDRECEIPT_ASSEMPLOYEE.get()) {
                        bean.setEmployee(multi.getMsgPhrase(selEmployee,
                                        CIMsgHumanResource.EmployeeWithNumberMsgPhrase));
                    }
                    datasource.add(bean);
                }
                final ComparatorChain<DocumentBean> chain = new ComparatorChain<>();
                chain.addComparator((_o1, _o2) -> _o1.getFundsToBeSettled().compareTo(_o2.getFundsToBeSettled()));
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
            final Map<String, Object> filter = filteredReport.getFilterMap(_parameter);
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
                            CISales.AccountPettyCash2PettyCashReceipt.getType().getId(),
                            CISales.AccountPettyCash2IncomingCreditNote.getType().getId(),
                            CISales.AccountPettyCash2Document.getType().getId());
            if (InstanceUtils.isType(instance, CISales.AccountFundsToBeSettled)) {
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
            final TextColumnBuilder<String> FundsToBeSettledColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AccountFundsToBeSettledReport.class.getName() + ".Column.FundsToBeSettled"), "FundsToBeSettled",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> officialColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AccountFundsToBeSettledReport.class.getName() + ".Column.Official"), "official",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> actionColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AccountFundsToBeSettledReport.class.getName() + ".Column.Action"), "action",
                            DynamicReports.type.stringType());

            final TextColumnBuilder<String> docNameColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AccountFundsToBeSettledReport.class.getName() + ".Column.DocName"), "docName",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> contactNameColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AccountFundsToBeSettledReport.class.getName() + ".Column.ContactName"),
                            "contactName", DynamicReports.type.stringType()).setWidth(150);
            final TextColumnBuilder<String> employeeColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AccountFundsToBeSettledReport.class.getName() + ".Column.Employee"), "employee",
                            DynamicReports.type.stringType()).setWidth(150);

            final TextColumnBuilder<DateTime> dateColumn = AbstractDynamicReport_Base.column(
                            DBProperties.getProperty(AccountFundsToBeSettledReport.class.getName() + ".Column.Date"), "date",
                            DateTimeDate.get());

            final ColumnGroupBuilder FundsToBeSettledGroup = DynamicReports.grp.group(FundsToBeSettledColumn).groupByDataType()
                            .setStyle(DynamicReports.stl.style().setBackgroundColor(Color.YELLOW));
            final ColumnGroupBuilder officialGroup = DynamicReports.grp.group(officialColumn).groupByDataType();
            final ColumnGroupBuilder actionGroup = DynamicReports.grp.group(actionColumn).groupByDataType();

            final TextFieldBuilder<String> FundsToBeSettledTotalBldr = DynamicReports.cmp.text(
                            DynamicReports.<String>field("FundsToBeSettledResume", String.class))
                            .setStyle(DynamicReports.stl.style().boldItalic()
                                            .setHorizontalTextAlignment(HorizontalTextAlignment.CENTER));
            FundsToBeSettledGroup.footer(FundsToBeSettledTotalBldr);

            _builder.addGroup(FundsToBeSettledGroup).addColumn(FundsToBeSettledColumn);
            _builder.addGroup(officialGroup, actionGroup).addColumn(officialColumn, actionColumn);

            if (showDetails()) {
                if (getExType().equals(ExportType.HTML)) {
                    _builder.addColumn(FilteredReport.getLinkColumn(_parameter, "oid"));
                }
                _builder.addColumn(docNameColumn, dateColumn, contactNameColumn);
                if (Sales.FUNDSTOBESETTLEDRECEIPT_ASSEMPLOYEE.get()) {
                    _builder.addColumn(employeeColumn);
                }
            }
            for (final CurrencyInst currency : CurrencyInst.getAvailable()) {
                final TextColumnBuilder<BigDecimal> amountColumn = DynamicReports.col.column(currency.getName(),
                                currency.getISOCode(), DynamicReports.type.bigDecimalType());

                _builder.addColumn(amountColumn);

                final AggregationSubtotalBuilder<BigDecimal> amountSum = DynamicReports.sbt.sum(amountColumn);
                final AggregationSubtotalBuilder<BigDecimal> amountSum2 = DynamicReports.sbt.sum(amountColumn);
                final AggregationSubtotalBuilder<BigDecimal> amountSum3 = DynamicReports.sbt.sum(amountColumn);

                _builder.addSubtotalAtGroupFooter(FundsToBeSettledGroup, amountSum);
                _builder.addSubtotalAtGroupFooter(officialGroup, amountSum2);
                _builder.addSubtotalAtGroupFooter(actionGroup, amountSum3);
            }

            final TextColumnBuilder<BigDecimal> baseAmountColumn = DynamicReports.col.column(DBProperties
                            .getProperty(AccountFundsToBeSettledReport.class.getName() + ".Column.BaseAmount"), "baseAmount",
                            DynamicReports.type.bigDecimalType());
            _builder.addColumn(baseAmountColumn);

            final AggregationSubtotalBuilder<BigDecimal> amountSum = DynamicReports.sbt.sum(baseAmountColumn);
            final AggregationSubtotalBuilder<BigDecimal> amountSum2 = DynamicReports.sbt.sum(baseAmountColumn);
            final AggregationSubtotalBuilder<BigDecimal> amountSum3 = DynamicReports.sbt.sum(baseAmountColumn);

            final TextColumnBuilder<String> subTotalTtextColumn = Sales.FUNDSTOBESETTLEDRECEIPT_ASSEMPLOYEE.get()
                            ? employeeColumn : contactNameColumn;
            _builder.addSubtotalAtGroupFooter(FundsToBeSettledGroup, FilteredReport.getCustomTextSubtotalBuilder(
                            _parameter, "FundsToBeSettled", subTotalTtextColumn), amountSum);

            _builder.addSubtotalAtGroupFooter(officialGroup, FilteredReport.getCustomTextSubtotalBuilder(_parameter,
                            "official", subTotalTtextColumn), amountSum2);
            _builder.addSubtotalAtGroupFooter(actionGroup, FilteredReport.getCustomTextSubtotalBuilder(_parameter,
                            "action", subTotalTtextColumn), amountSum3);
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
        protected AccountFundsToBeSettledReport_Base getFilteredReport()
        {
            return filteredReport;
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
        private Instance fundsToBeSettledInstance;

        /** The petty cash. */
        private String fundsToBeSettled;

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

        /** The liquidation. */
        private String liquidation;

        /** The base cross. */
        private BigDecimal baseCross;

        /** The base net. */
        private BigDecimal baseNet;

        public String getFundsToBeSettled()
        {
            return fundsToBeSettled;
        }

        /**
         * Gets the petty cash resume.
         *
         * @return the petty cash resume
         */
        public String getFundsToBeSettledResume()
        {
            String ret = null;
            try {
                BigDecimal startAmount = BigDecimal.ZERO;
                String currency = "";
                final PrintQuery print = CachedPrintQuery.get4Request(getFundsToBeSettledInstance());
                final SelectBuilder sel = SelectBuilder.get().linkto(CISales.AccountFundsToBeSettled.CurrencyLink).instance();
                print.addSelect(sel);
                print.addAttribute(CISales.AccountFundsToBeSettled.AmountAbstract);
                if (print.execute()) {
                    startAmount = print.getAttribute(CISales.AccountFundsToBeSettled.AmountAbstract);
                    currency = CurrencyInst.get(print.<Instance>getSelect(sel)).getSymbol();
                }

                final QueryBuilder queryBldr = new QueryBuilder(CISales.Balance);
                queryBldr.addWhereAttrEqValue(CISales.Balance.Account, getFundsToBeSettledInstance());
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
                ret = DBProperties.getFormatedDBProperty(AccountFundsToBeSettledReport.class.getName() + ".FundsToBeSettledResume",
                                (Object) getFundsToBeSettled(), currency, startAmount, currentAmount);
            } catch (final EFapsException e) {
                AccountFundsToBeSettledReport_Base.LOG.error("Catched error on evalutaion of FundsToBeSettledResume", e);
            }
            return ret;
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
                    case "FundsToBeSettled":
                        ret = getFundsToBeSettled();
                        break;
                    case "liquidation":
                        try {
                            ret = getLiquidation();
                        } catch (final EFapsException e1) {
                            AccountFundsToBeSettledReport_Base.LOG.error("Catched", e1);
                        }
                        break;
                    case "FundsToBeSettledResume":
                        ret = getFundsToBeSettledResume();
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
                            AccountFundsToBeSettledReport_Base.LOG.error("Catched", e2);
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
            return baseCross;
        }

        /**
         * Sets the petty cash instance.
         *
         * @param _FundsToBeSettledInstance the new petty cash instance
         * @return the document bean
         */
        public DocumentBean setFundsToBeSettledInstance(final Instance _fundsToBeSettledInstance)
        {
            fundsToBeSettledInstance = _fundsToBeSettledInstance;
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
            docTypeName = _docTypeName;
            return this;
        }

        /**
         * Setter method for instance variable {@link #FundsToBeSettled}.
         *
         * @param _name value for instance variable {@link #FundsToBeSettled}
         * @return the document bean
         */
        public DocumentBean setFundsToBeSettled(final String _name)
        {
            fundsToBeSettled = _name;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #typeLabel}.
         *
         * @return value of instance variable {@link #typeLabel}
         */
        public String getTypeLabel()
        {
            return typeLabel;
        }

        /**
         * Setter method for instance variable {@link #typeLabel}.
         *
         * @param _typeLabel value for instance variable {@link #typeLabel}
         * @return the document bean
         */
        public DocumentBean setTypeLabel(final String _typeLabel)
        {
            typeLabel = _typeLabel;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #action}.
         *
         * @return value of instance variable {@link #action}
         */
        public String getAction()
        {
            return action == null ? "-" : action;
        }

        /**
         * Gets the official.
         *
         * @return the official
         */
        public String getOfficial()
        {
            final String ret;
            if (docTypeName == null) {
                ret = DBProperties.getProperty(AccountFundsToBeSettledReport.class.getName() + ".NotWithDocument");
            } else {
                ret = DBProperties.getProperty(AccountFundsToBeSettledReport.class.getName() + ".WithDocument");
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
            action = _action;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #oid}.
         *
         * @return value of instance variable {@link #oid}
         */
        public String getOid()
        {
            return instance.getOid();
        }

        /**
         * Setter method for instance variable {@link #oid}.
         *
         * @param _instance the new oID of the document
         * @return the document bean
         */
        public DocumentBean setInstance(final Instance _instance)
        {
            instance = _instance;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #docName}.
         *
         * @return value of instance variable {@link #docName}
         */
        public String getDocName()
        {
            return docName;
        }

        /**
         * Setter method for instance variable {@link #docName}.
         *
         * @param _docName value for instance variable {@link #docName}
         * @return the document bean
         */
        public DocumentBean setDocName(final String _docName)
        {
            docName = _docName;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #FundsToBeSettledInstance}.
         *
         * @return value of instance variable {@link #FundsToBeSettledInstance}
         */
        public Instance getFundsToBeSettledInstance()
        {
            return fundsToBeSettledInstance;
        }

        /**
         * Getter method for the instance variable {@link #contactName}.
         *
         * @return value of instance variable {@link #contactName}
         */
        public String getContactName()
        {
            return contactName;
        }

        /**
         * Setter method for instance variable {@link #contactName}.
         *
         * @param _contactName value for instance variable {@link #contactName}
         * @return the document bean
         */
        public DocumentBean setContactName(final String _contactName)
        {
            contactName = _contactName;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #cross}.
         *
         * @return value of instance variable {@link #cross}
         */
        public BigDecimal getCross()
        {
            return cross;
        }

        /**
         * Setter method for instance variable {@link #cross}.
         *
         * @param _cross value for instance variable {@link #cross}
         * @return the document bean
         */
        public DocumentBean setCross(final BigDecimal _cross)
        {
            cross = _cross;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #net}.
         *
         * @return value of instance variable {@link #net}
         */
        public BigDecimal getNet()
        {
            return net;
        }

        /**
         * Setter method for instance variable {@link #net}.
         *
         * @param _net value for instance variable {@link #net}
         * @return the document bean
         */
        public DocumentBean setNet(final BigDecimal _net)
        {
            net = _net;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #currencyInstance}.
         *
         * @return value of instance variable {@link #currencyInstance}
         */
        public Instance getCurrencyInstance()
        {
            return currencyInstance;
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
            currencyInstance = _currencyInstance;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #date}.
         *
         * @return value of instance variable {@link #date}
         */
        public DateTime getDate()
        {
            return date;
        }

        /**
         * Setter method for instance variable {@link #date}.
         *
         * @param _date value for instance variable {@link #date}
         * @return the document bean
         */
        public DocumentBean setDate(final DateTime _date)
        {
            date = _date;
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
            if (liquidation == null && instance != null && instance.isValid()) {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.FundsToBeSettledBalance2FundsToBeSettledReceipt);
                attrQueryBldr.addType(CISales.FundsToBeSettledBalance2IncomingCreditNote);
                attrQueryBldr.addWhereAttrEqValue(CISales.Document2DocumentAbstract.ToAbstractLink, instance);
                final AttributeQuery attrQuery =
                                attrQueryBldr.getAttributeQuery(CISales.Document2DocumentAbstract.FromAbstractLink);
                final QueryBuilder queryBldr = new QueryBuilder(CISales.FundsToBeSettledBalance);
                queryBldr.addWhereAttrInQuery(CISales.FundsToBeSettledBalance.ID, attrQuery);
                final MultiPrintQuery multi = queryBldr.getCachedPrint4Request();
                multi.addAttribute(CISales.FundsToBeSettledBalance.Name);
                multi.execute();
                if (multi.next()) {
                    liquidation = multi.<String>getAttribute(CISales.FundsToBeSettledBalance.Name);
                }
            }
            return liquidation == null
                            ? DBProperties.getProperty(AccountFundsToBeSettledReport.class.getName() + ".WithoutLiquidation")
                                            : liquidation;
        }

        /**
         * Sets the base net.
         *
         * @param _baseNet the new base net
         * @return the document bean
         */
        public DocumentBean setBaseNet(final BigDecimal _baseNet)
        {
            baseNet = _baseNet;
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
            baseCross = _baseCross;
            return this;
        }

        /**
         * Gets the base net.
         *
         * @return the base net
         */
        public BigDecimal getBaseNet()
        {
            return baseNet;
        }

        /**
         * Gets the base cross.
         *
         * @return the base cross
         */
        public BigDecimal getBaseCross()
        {
            return baseCross;
        }

        /**
         * Getter method for the instance variable {@link #employee}.
         *
         * @return value of instance variable {@link #employee}
         */
        public String getEmployee()
        {
            return employee;
        }

        /**
         * Setter method for instance variable {@link #employee}.
         *
         * @param _employee value for instance variable {@link #employee}
         * @return the document bean
         */
        public DocumentBean setEmployee(final String _employee)
        {
            employee = _employee;
            return this;
        }

        @Override
        public String toString()
        {
            return ToStringBuilder.reflectionToString(this);
        }
    }
}
