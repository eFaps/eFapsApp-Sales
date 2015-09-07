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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeDate;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeMonth;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeYear;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.erp.RateInfo;
import org.efaps.ui.wicket.models.EmbeddedLink;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.base.expression.AbstractSimpleExpression;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.VariableBuilder;
import net.sf.dynamicreports.report.builder.column.ComponentColumnBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.component.GenericElementBuilder;
import net.sf.dynamicreports.report.builder.expression.AbstractComplexExpression;
import net.sf.dynamicreports.report.builder.group.ColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.style.ReportStyleBuilder;
import net.sf.dynamicreports.report.builder.style.StyleBuilder;
import net.sf.dynamicreports.report.builder.subtotal.AggregationSubtotalBuilder;
import net.sf.dynamicreports.report.constant.Calculation;
import net.sf.dynamicreports.report.constant.HorizontalTextAlignment;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("81ca4010-b4ef-40e5-ad0a-55b99db6b617")
@EFapsApplication("eFapsApp-Sales")
public abstract class SalesProductReport_Base
    extends FilteredReport
{

    /**
     * The Enum DateConfig.
     *
     * @author The eFaps Team
     */
    public enum DateConfig
    {
        /** Includes a group on date level. */
        DAILY,
        /** Includes a group on monthly level. */
        MONTHLY,
        /** Includes a group on monthly level. */
        YEARLY,
        /** Shows onnly the latest. */
        LATEST;
    }

    /**
     * The Enum DateConfig.
     *
     * @author The eFaps Team
     */
    public enum PriceConfig
    {
        /** Includes a group on date level. */
        NET, /** Includes a group on monthly level. */
        CROSS;
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

    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new DynSalesProductReport(this);
    }

    public static class DynSalesProductReport
        extends AbstractDynamicReport
    {

        private final SalesProductReport_Base filteredReport;

        /**
         * @param _salesProductReport_Base
         */
        public DynSalesProductReport(final SalesProductReport_Base _salesProductReport_Base)
        {
            this.filteredReport = _salesProductReport_Base;
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            JRRewindableDataSource ret;
            if (getFilteredReport().isCached(_parameter)) {
                ret = getFilteredReport().getDataSourceFromCache(_parameter);
                try {
                    ret.moveFirst();
                } catch (final JRException e) {
                    throw new EFapsException("JRException", e);
                }
            } else {

                final DateConfig dateConfig = evaluateDateConfig(_parameter);
                final PriceConfig priceConfig = evaluatePriceConfig(_parameter);
                final CurrencyInst currencyInst = evaluateCurrencyInst(_parameter);

                final List<DataBean> data = new ArrayList<>();

                final QueryBuilder attrQueryBldr = getQueryBldrFromProperties(_parameter);
                add2QueryBuilder(_parameter, attrQueryBldr);
                if (getContactInst(_parameter).length > 0) {
                    attrQueryBldr.addWhereAttrEqValue(CISales.DocumentSumAbstract.Contact, getContactInst(_parameter));
                }
                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.DocumentSumAbstract.ID);

                final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionSumAbstract);
                queryBldr.addWhereAttrInQuery(CISales.PositionSumAbstract.DocumentAbstractLink, attrQuery);
                if (getProductInst(_parameter).length > 0) {
                    queryBldr.addWhereAttrEqValue(CISales.PositionSumAbstract.Product, getProductInst(_parameter));
                }
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CISales.PositionSumAbstract.Quantity,
                                CISales.PositionSumAbstract.NetUnitPrice,
                                CISales.PositionSumAbstract.NetPrice,
                                CISales.PositionSumAbstract.CrossUnitPrice,
                                CISales.PositionSumAbstract.CrossPrice,
                                CISales.PositionSumAbstract.RateNetUnitPrice,
                                CISales.PositionSumAbstract.RateNetPrice,
                                CISales.PositionSumAbstract.RateCrossUnitPrice,
                                CISales.PositionSumAbstract.RateCrossPrice,
                                CISales.PositionSumAbstract.Discount,
                                CISales.PositionSumAbstract.UoM);
                final SelectBuilder selCurInst = new SelectBuilder()
                                .linkto(CISales.PositionSumAbstract.RateCurrencyId)
                                .instance();
                final SelectBuilder selContactInst = new SelectBuilder()
                                .linkto(CISales.PositionSumAbstract.DocumentAbstractLink)
                                .linkto(CISales.DocumentSumAbstract.Contact).instance();
                final SelectBuilder selProductInst = new SelectBuilder()
                                .linkto(CISales.PositionSumAbstract.Product).instance();
                final SelectBuilder selProductName = new SelectBuilder()
                                .linkto(CISales.PositionSumAbstract.Product)
                                .attribute(CIProducts.ProductAbstract.Name);
                final SelectBuilder selProductDesc = new SelectBuilder()
                                .linkto(CISales.PositionSumAbstract.Product)
                                .attribute(CIProducts.ProductAbstract.Description);
                final SelectBuilder selContactName = new SelectBuilder()
                                .linkto(CISales.PositionSumAbstract.DocumentAbstractLink)
                                .linkto(CISales.DocumentSumAbstract.Contact)
                                .attribute(CIContacts.Contact.Name);
                final SelectBuilder selDocDate = new SelectBuilder()
                                .linkto(CISales.PositionSumAbstract.DocumentAbstractLink)
                                .attribute(CISales.DocumentSumAbstract.Date);
                final SelectBuilder selDocType = new SelectBuilder()
                                .linkto(CISales.PositionSumAbstract.DocumentAbstractLink).type().label();
                final SelectBuilder selDocName = new SelectBuilder()
                                .linkto(CISales.PositionSumAbstract.DocumentAbstractLink)
                                .attribute(CISales.DocumentSumAbstract.Name);
                final SelectBuilder selDocOID = new SelectBuilder()
                                .linkto(CISales.PositionSumAbstract.DocumentAbstractLink)
                                .oid();
                final SelectBuilder selDocStatus = new SelectBuilder()
                                .linkto(CISales.PositionSumAbstract.DocumentAbstractLink)
                                .status().label();

                multi.addSelect(selContactInst, selContactName, selDocDate, selDocType, selDocName,
                                selCurInst, selProductInst, selProductName, selProductDesc, selDocOID, selDocStatus);
                multi.execute();
                while (multi.next()) {
                    final DateTime date = multi.<DateTime>getSelect(selDocDate);
                    final DataBean dataBean = new DataBean()
                                    .setProductInst(multi.<Instance>getSelect(selProductInst))
                                    .setProductName(multi.<String>getSelect(selProductName))
                                    .setProductDesc(multi.<String>getSelect(selProductDesc))
                                    .setCurrency(currencyInst.getSymbol())
                                    .setContactInst(multi.<Instance>getSelect(selContactInst))
                                    .setContact(multi.<String>getSelect(selContactName))
                                    .setDate(date)
                                    .setDocName(multi.<String>getSelect(selDocName))
                                    .setDocType(multi.<String>getSelect(selDocType))
                                    .setDocOID(multi.<String>getSelect(selDocOID))
                                    .setDocStatus(multi.<String>getSelect(selDocStatus))
                                    .setProductDiscount(
                                                    multi.<BigDecimal>getAttribute(
                                                                    CISales.PositionSumAbstract.Discount));
                    data.add(dataBean);

                    final UoM uoM = Dimension.getUoM(multi.<Long>getAttribute(CISales.PositionSumAbstract.UoM));
                    BigDecimal quantityTmp = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.Quantity);
                    quantityTmp = quantityTmp.multiply(new BigDecimal(uoM.getNumerator()))
                                    .divide(new BigDecimal(uoM.getDenominator()), 4, BigDecimal.ROUND_HALF_UP);
                    dataBean.setQuantity(quantityTmp).setProductUoM(uoM.getName());

                    final Instance curInst = multi.getSelect(selCurInst);
                    BigDecimal unitPrice;
                    BigDecimal price;
                    if (currencyInst.getInstance().equals(curInst)) {
                        unitPrice = PriceConfig.NET.equals(priceConfig) ? multi.<BigDecimal>getAttribute(
                                        CISales.PositionSumAbstract.RateNetUnitPrice) : multi.<BigDecimal>getAttribute(
                                                        CISales.PositionSumAbstract.RateCrossUnitPrice);
                        price = PriceConfig.NET.equals(priceConfig) ? multi.<BigDecimal>getAttribute(
                                        CISales.PositionSumAbstract.RateNetPrice) : multi.<BigDecimal>getAttribute(
                                                        CISales.PositionSumAbstract.RateCrossPrice);
                    } else if (currencyInst.getInstance().equals(Currency.getBaseCurrency())) {
                        unitPrice = PriceConfig.NET.equals(priceConfig) ? multi.<BigDecimal>getAttribute(
                                        CISales.PositionSumAbstract.NetUnitPrice) : multi.<BigDecimal>getAttribute(
                                                        CISales.PositionSumAbstract.CrossUnitPrice);
                        price = PriceConfig.NET.equals(priceConfig) ? multi.<BigDecimal>getAttribute(
                                        CISales.PositionSumAbstract.NetPrice) : multi.<BigDecimal>getAttribute(
                                                        CISales.PositionSumAbstract.CrossPrice);
                    } else {
                        final BigDecimal unitPriceTmp = PriceConfig.NET.equals(priceConfig)
                                        ? multi.<BigDecimal>getAttribute(
                                                        CISales.PositionSumAbstract.RateNetUnitPrice)
                                        : multi.<BigDecimal>getAttribute(
                                                        CISales.PositionSumAbstract.RateCrossUnitPrice);
                        final BigDecimal priceTmp = PriceConfig.NET.equals(priceConfig)
                                        ? multi.<BigDecimal>getAttribute(
                                                        CISales.PositionSumAbstract.RateNetPrice)
                                        : multi.<BigDecimal>getAttribute(
                                                        CISales.PositionSumAbstract.RateCrossPrice);
                        final RateInfo rateInfo = new Currency().evaluateRateInfos(_parameter, date, curInst,
                                        currencyInst.getInstance())[2];

                        final BigDecimal rate = RateInfo.getRate(_parameter, rateInfo,
                                        Instance.get(dataBean.getDocOID()).getType().getName());

                        unitPrice = unitPriceTmp.setScale(8).divide(rate, BigDecimal.ROUND_HALF_UP);
                        price = priceTmp.setScale(8).divide(rate, BigDecimal.ROUND_HALF_UP);
                    }
                    dataBean.setUnitPrice(unitPrice).setPrice(price);
                }

                final ComparatorChain<DataBean> comparator = new ComparatorChain<>();
                if (DateConfig.DAILY.equals(dateConfig) || DateConfig.LATEST.equals(dateConfig)) {
                    comparator.addComparator(new Comparator<DataBean>()
                    {

                        @Override
                        public int compare(final DataBean _arg0,
                                           final DataBean _arg1)
                        {
                            return _arg0.getDocDate().compareTo(_arg1.getDocDate());
                        }
                    });
                } else if (DateConfig.MONTHLY.equals(dateConfig)) {
                    comparator.addComparator(new Comparator<DataBean>()
                    {

                        @Override
                        public int compare(final DataBean _arg0,
                                           final DataBean _arg1)
                        {
                            return Integer.valueOf(_arg0.getDocDate().getYear())
                                            .compareTo(Integer.valueOf(_arg1.getDocDate().getYear()));
                        }
                    });
                    comparator.addComparator(new Comparator<DataBean>()
                    {

                        @Override
                        public int compare(final DataBean _arg0,
                                           final DataBean _arg1)
                        {
                            return Integer.valueOf(_arg0.getDocDate().getMonthOfYear())
                                            .compareTo(Integer.valueOf(_arg1.getDocDate().getMonthOfYear()));
                        }
                    });
                } else if (DateConfig.YEARLY.equals(dateConfig)) {
                    comparator.addComparator(new Comparator<DataBean>()
                    {

                        @Override
                        public int compare(final DataBean _arg0,
                                           final DataBean _arg1)
                        {
                            return Integer.valueOf(_arg0.getDocDate().getYear())
                                            .compareTo(Integer.valueOf(_arg1.getDocDate().getYear()));
                        }
                    });
                }

                if (groupByContact(_parameter)) {
                    comparator.addComparator(new Comparator<DataBean>()
                    {

                        @Override
                        public int compare(final DataBean _arg0,
                                           final DataBean _arg1)
                        {
                            return _arg0.getContact().compareTo(_arg1.getContact());
                        }
                    });
                } else {
                    comparator.addComparator(new Comparator<DataBean>()
                    {

                        @Override
                        public int compare(final DataBean _arg0,
                                           final DataBean _arg1)
                        {
                            return _arg0.getProductName().compareTo(_arg1.getProductName());
                        }
                    });
                }

                Collections.sort(data, comparator);

                final List<DataBean> dataSource;
                if (DateConfig.LATEST.equals(dateConfig)) {
                    Collections.reverse(data);
                    dataSource = new ArrayList<>();
                    final Set<String> added = new HashSet<>();
                    for (final DataBean bean : data) {
                        final String criteria = isContact(_parameter)
                                        ? bean.getProductInst().getOid() : bean.getContactInst().getOid();
                        if (!added.contains(criteria)) {
                            added.add(criteria);
                            dataSource.add(bean);
                        }
                    }
                } else if (isHideDetails(_parameter)) {
                    dataSource = new ArrayList<>();
                    final Map<String, DataBean> added = new LinkedHashMap<>();
                    for (final DataBean bean : data) {
                        final String criteria = groupByContact(_parameter)
                                        ? bean.getProductInst().getOid() : bean.getContactInst().getOid();
                        if (!added.containsKey(criteria)) {
                            added.put(criteria, bean);
                            bean.setDocName(null).setDocOID(null).setDocType(null).setDocStatus(null);
                            dataSource.add(bean);
                        } else {
                            final DataBean current = added.get(criteria);
                            current.setPrice(current.getPrice().add(bean.getPrice()))
                                            .setQuantity(current.getQuantity().add(bean.getQuantity()))
                                            .setUnitPrice(current.getPrice().divide(current.getQuantity(),
                                                            BigDecimal.ROUND_HALF_UP));
                        }
                    }
                } else {
                    dataSource = data;
                }
                ret = new JRBeanCollectionDataSource(dataSource);
                getFilteredReport().cache(_parameter, ret);
            }
            return ret;
        }

        protected boolean isHideDetails(final Parameter _parameter)
            throws EFapsException
        {
            final Map<String, Object> filter = this.filteredReport.getFilterMap(_parameter);
            return (Boolean) filter.get("hideDetails");
        }

        protected CurrencyInst evaluateCurrencyInst(final Parameter _parameter)
            throws EFapsException
        {
            final Map<String, Object> filterMap = this.filteredReport.getFilterMap(_parameter);
            final CurrencyInst ret;

            if (filterMap.containsKey("currency")) {
                final CurrencyFilterValue filter = (CurrencyFilterValue) filterMap.get("currency");
                if (filter.getObject() instanceof Instance && filter.getObject().isValid()) {
                    ret = CurrencyInst.get(filter.getObject());
                } else if (filter.getObject() instanceof Instance && "BASE".equals(filter.getObject().getKey())) {
                    ret = CurrencyInst.get(Currency.getBaseCurrency());
                } else {
                    ret = CurrencyInst.get(Currency.getBaseCurrency());
                }
            } else {
                ret = CurrencyInst.get(Currency.getBaseCurrency());
            }
            return ret;
        }

        protected DateConfig evaluateDateConfig(final Parameter _parameter)
            throws EFapsException
        {
            DateConfig ret;
            final Map<String, Object> filter = this.filteredReport.getFilterMap(_parameter);
            final EnumFilterValue value = (EnumFilterValue) filter.get("dateConfig");
            if (value != null) {
                ret = (DateConfig) value.getObject();
            } else {
                ret = DateConfig.DAILY;
            }
            return ret;
        }

        protected PriceConfig evaluatePriceConfig(final Parameter _parameter)
            throws EFapsException
        {
            PriceConfig ret;
            final Map<String, Object> filter = this.filteredReport.getFilterMap(_parameter);
            final EnumFilterValue value = (EnumFilterValue) filter.get("priceConfig");
            if (value != null) {
                ret = (PriceConfig) value.getObject();
            } else {
                ret = PriceConfig.NET;
            }
            return ret;
        }

        /**
         * @param _parameter Parameter as passed from the eFaps API
         * @param _queryBldr QueryBuilder the criteria will be added to
         * @throws EFapsException on error
         */
        protected void add2QueryBuilder(final Parameter _parameter,
                                        final QueryBuilder _queryBldr)
            throws EFapsException
        {
            final Map<String, Object> filter = this.filteredReport.getFilterMap(_parameter);
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
            if (filter.containsKey("type")) {
                _queryBldr.addWhereAttrEqValue(CISales.DocumentSumAbstract.Type,
                                ((TypeFilterValue) filter.get("type")).getObject().toArray());
            }
            _queryBldr.addWhereAttrGreaterValue(CISales.DocumentSumAbstract.Date, dateFrom);
            _queryBldr.addWhereAttrLessValue(CISales.DocumentSumAbstract.Date, dateTo.plusDays(1)
                            .withTimeAtStartOfDay());
        }

        /**
         * Checks if the report is in a contact instance.
         *
         * @param _parameter the _parameter
         * @return true, if is contact
         * @throws EFapsException
         */
        protected boolean isContact(final Parameter _parameter) throws EFapsException
        {
            return isInstance(_parameter) && _parameter.getInstance().getType().isKindOf(CIContacts.Contact);
        }

        /**
         * Gets the contact inst.
         *
         * @param _parameter the _parameter
         * @return the contact inst
         * @throws EFapsException on error
         */
        protected Object[] getContactInst(final Parameter _parameter)
            throws EFapsException
        {
            Object[] ret;
            if (isContact(_parameter)) {
                ret = new Object[] { _parameter.getInstance() };
            } else {
                final Map<String, Object> filterMap = this.filteredReport.getFilterMap(_parameter);
                final InstanceSetFilterValue filter = (InstanceSetFilterValue) filterMap.get("contact");
                if (filter == null || (filter != null && filter.getObject() == null)) {
                    ret = new Object[] {};
                } else {
                    ret = filter.getObject().toArray();
                }
                if (ret.length == 0) {
                    final InstanceFilterValue employeefilter = (InstanceFilterValue) filterMap.get("employee");
                    if (employeefilter != null &&  employeefilter.getObject() != null
                                    && employeefilter.getObject().isValid()) {
                        //HumanResource_Employee2Contact
                        final QueryBuilder attrQueryBldr = new QueryBuilder(
                                        UUID.fromString("2f3768b5-ffad-4ed4-a960-2f774e316e21"));
                        attrQueryBldr.addWhereAttrEqValue("FromLink", employeefilter.getObject());

                        final QueryBuilder queryBldr = new QueryBuilder(CIContacts.Contact);
                        queryBldr.addWhereAttrInQuery(CIContacts.Contact.ID, attrQueryBldr.getAttributeQuery("ToLink"));
                        ret = queryBldr.getQuery().execute().toArray();
                    }
                }
            }
            return ret;
        }

        /**
         * Checks if the report is in a product instance.
         *
         * @param _parameter the _parameter
         * @return true, if is product
         */
        protected boolean isProduct(final Parameter _parameter)
        {
            return isInstance(_parameter) && _parameter.getInstance().getType().isKindOf(CIProducts.ProductAbstract);
        }

        protected Object[] getProductInst(final Parameter _parameter)
            throws EFapsException
        {
            Object[] ret;
            if (isProduct(_parameter)) {
                ret = new Object[] { _parameter.getInstance() };
            } else {
                final Map<String, Object> filterMap = this.filteredReport.getFilterMap(_parameter);
                final InstanceSetFilterValue filter = (InstanceSetFilterValue) filterMap.get("product");
                if (filter == null || (filter != null && filter.getObject() == null)) {
                    ret = new Object[] {};
                } else {
                    ret = filter.getObject().toArray();
                }
            }
            return ret;
        }

        /**
         * Checks if is instance.
         *
         * @param _parameter the _parameter
         * @return true, if is instance
         */
        protected boolean isInstance(final Parameter _parameter) {
            return _parameter.getInstance() != null && _parameter.getInstance().isValid();
        }

        /**
         * Evaluate it it must be grouped by contact.
         *
         * @param _parameter the _parameter
         * @return true, if successful
         * @throws EFapsException
         */
        protected boolean groupByContact(final Parameter _parameter)
            throws EFapsException
        {
            final Map<String, Object> filter = this.filteredReport.getFilterMap(_parameter);
            return isProduct(_parameter) || (Boolean) filter.get("groupByContact");
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final DateConfig dateConfig = evaluateDateConfig(_parameter);
            final boolean hideDetails = isHideDetails(_parameter);
            final boolean groupByContact = groupByContact(_parameter);

            final TextColumnBuilder<String> contactColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("Contact"), "contact",
                            DynamicReports.type.stringType()).setWidth(250);
            final TextColumnBuilder<String> productColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("Product"), "product",
                            DynamicReports.type.stringType());

            final TextColumnBuilder<String> productNameColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("ProductName"), "productName",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> productDescColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("ProductDesc"), "productDesc",
                            DynamicReports.type.stringType());

            final TextColumnBuilder<DateTime> monthColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("FilterDate1"),
                            "docDate", DateTimeMonth.get("MMMMM"));
            final TextColumnBuilder<DateTime> yearColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("FilterDate2"),
                            "docDate", DateTimeYear.get());
            final TextColumnBuilder<DateTime> dateColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("Date"),
                            "docDate", DateTimeDate.get());

            final TextColumnBuilder<BigDecimal> priceColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("Price"), "price",
                            DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> quantityColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("Quantity"), "quantity",
                            DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> unitPriceColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("UnitPrice"), "unitPrice",
                            DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<String> currencyColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("Currency"), "currency",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> docNameColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("DocName"), "docName",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> docTypeColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("DocType"), "docType",
                            DynamicReports.type.stringType());

            final TextColumnBuilder<String> statusColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("DocStatus"), "docStatus",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> uomColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("UoM"), "productUoM",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<BigDecimal> discountColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("Discount"), "productDiscount",
                            DynamicReports.type.bigDecimalType());

            final ColumnGroupBuilder yearGroup = DynamicReports.grp.group(yearColumn).groupByDataType();
            final ColumnGroupBuilder monthGroup = DynamicReports.grp.group(monthColumn).groupByDataType();
            final ColumnGroupBuilder dateGroup = DynamicReports.grp.group(dateColumn).groupByDataType();
            final ColumnGroupBuilder contactGroup = DynamicReports.grp.group(contactColumn).groupByDataType();
            final ColumnGroupBuilder productGroup = DynamicReports.grp.group(productColumn).groupByDataType();

            final VariableBuilder<BigDecimal> quantity4ProdCon = DynamicReports.variable("quantity",
                            BigDecimal.class, Calculation.SUM);
            final VariableBuilder<BigDecimal> price4ProdCon = DynamicReports.variable("price",
                            BigDecimal.class, Calculation.SUM);
            final VariableBuilder<BigDecimal> quantity4Month = DynamicReports.variable("quantity",
                            BigDecimal.class, Calculation.SUM);
            final VariableBuilder<BigDecimal> price4Month = DynamicReports.variable("price",
                            BigDecimal.class, Calculation.SUM);
            final VariableBuilder<BigDecimal> quantity4Year = DynamicReports.variable("quantity",
                            BigDecimal.class, Calculation.SUM);
            final VariableBuilder<BigDecimal> price4Year = DynamicReports.variable("price",
                            BigDecimal.class, Calculation.SUM);
            final VariableBuilder<BigDecimal> quantity4Total = DynamicReports.variable("quantity",
                            BigDecimal.class, Calculation.SUM);
            final VariableBuilder<BigDecimal> price4Total = DynamicReports.variable("price",
                            BigDecimal.class, Calculation.SUM);
            if (!DateConfig.LATEST.equals(dateConfig)) {
                if (!hideDetails || !isInstance(_parameter)) {
                    quantity4ProdCon.setResetGroup(groupByContact ? contactGroup : productGroup);
                    price4ProdCon.setResetGroup(groupByContact ? contactGroup : productGroup);
                }
                if (!DateConfig.YEARLY.equals(dateConfig)) {
                    quantity4Month.setResetGroup(monthGroup);
                    price4Month.setResetGroup(monthGroup);
                }
                quantity4Year.setResetGroup(yearGroup);
                price4Year.setResetGroup(yearGroup);
            }
            _builder.addVariable(quantity4ProdCon, price4ProdCon, quantity4Month, price4Month, quantity4Total,
                            price4Total, quantity4Year, price4Year);

            final StyleBuilder totalStyle = DynamicReports.stl.style()
                            .setHorizontalTextAlignment(HorizontalTextAlignment.RIGHT).setBold(true)
                            .setPattern("#,##0.00");
            final StyleBuilder txtStyle = DynamicReports.stl.style(totalStyle)
                            .setHorizontalTextAlignment(HorizontalTextAlignment.CENTER);

            final AggregationSubtotalBuilder<BigDecimal> quantityProdSum = DynamicReports.sbt.sum(quantityColumn);
            final AggregationSubtotalBuilder<BigDecimal> netPriceProdSum = DynamicReports.sbt.sum(priceColumn);
            final AggregationSubtotalBuilder<BigDecimal> unitPriceProdSum = DynamicReports.sbt.<BigDecimal>aggregate(
                            new UnitPriceTotal("unitPriceProdSum", quantity4ProdCon, price4ProdCon), unitPriceColumn,
                                Calculation.NOTHING).setStyle(totalStyle);

            final AggregationSubtotalBuilder<BigDecimal> quantityProdSum4Month = DynamicReports.sbt.sum(quantityColumn);
            final AggregationSubtotalBuilder<BigDecimal> netPriceProdSum4Month = DynamicReports.sbt.sum(priceColumn);
            final AggregationSubtotalBuilder<BigDecimal> unitPriceTotal4Month = DynamicReports.sbt
                            .<BigDecimal>aggregate(new UnitPriceTotal("unitPriceTotal4Month", quantity4Month,
                                            price4Month), unitPriceColumn, Calculation.NOTHING)
                            .setStyle(totalStyle);

            final AggregationSubtotalBuilder<BigDecimal> quantityProdSum4Year = DynamicReports.sbt.sum(quantityColumn);
            final AggregationSubtotalBuilder<BigDecimal> netPriceProdSum4Year = DynamicReports.sbt.sum(priceColumn);
            final AggregationSubtotalBuilder<BigDecimal> unitPriceTotal4Year = DynamicReports.sbt
                            .<BigDecimal>aggregate(new UnitPriceTotal("unitPriceTotal4Year", quantity4Year, price4Year),
                                            unitPriceColumn, Calculation.NOTHING)
                            .setStyle(totalStyle);

            final AggregationSubtotalBuilder<BigDecimal> quantityTotSum = DynamicReports.sbt.sum(quantityColumn)
                            .setStyle(totalStyle);
            final AggregationSubtotalBuilder<BigDecimal> netPriceTotSum = DynamicReports.sbt.sum(priceColumn)
                            .setStyle(totalStyle);
            final AggregationSubtotalBuilder<BigDecimal> unitPriceTotSum = DynamicReports.sbt
                            .<BigDecimal>aggregate(new UnitPriceTotal("unitPriceTotSum", quantity4Total, price4Total),
                                            unitPriceColumn, Calculation.NOTHING)
                            .setStyle(totalStyle);

            final GenericElementBuilder linkElement = DynamicReports.cmp.genericElement(
                            "http://www.efaps.org", "efapslink")
                            .addParameter(EmbeddedLink.JASPER_PARAMETERKEY, new LinkExpression())
                            .setHeight(12).setWidth(25);

            final ComponentColumnBuilder linkColumn = DynamicReports.col.componentColumn(linkElement).setTitle("");

            if (DateConfig.LATEST.equals(dateConfig)) {
                if (groupByContact) {
                    _builder.addColumn(productNameColumn, productDescColumn);
                }
            } else {
                _builder.addColumn(yearColumn);
                if (!DateConfig.YEARLY.equals(dateConfig)) {
                    _builder.addColumn(monthColumn);
                }
            }
            if (DateConfig.DAILY.equals(dateConfig)) {
                _builder.addColumn(dateColumn);
            }
            if (!DateConfig.LATEST.equals(dateConfig) && groupByContact
                            && (hideDetails || !isInstance(_parameter))) {
                _builder.addColumn(productNameColumn, productDescColumn);
            }

            if (!groupByContact) {
                _builder.addColumn(contactColumn);
            }

            if (!hideDetails) {
                if (getExType().equals(ExportType.HTML)) {
                    _builder.addColumn(linkColumn);
                }
                _builder.addColumn(docTypeColumn,
                            docNameColumn.setFixedWidth(150),
                            statusColumn.setHorizontalTextAlignment(HorizontalTextAlignment.CENTER),
                            dateColumn);
            }
            _builder.addColumn(uomColumn.setHorizontalTextAlignment(HorizontalTextAlignment.CENTER).setFixedWidth(70),
                            quantityColumn,
                            unitPriceColumn,
                            discountColumn,
                            priceColumn,
                            currencyColumn.setHorizontalTextAlignment(HorizontalTextAlignment.CENTER)
                                            .setFixedWidth(70));
            if (!DateConfig.LATEST.equals(dateConfig)) {
                _builder.groupBy(yearGroup);
                if (!DateConfig.YEARLY.equals(dateConfig)) {
                    _builder.groupBy(monthGroup);
                }
                if (DateConfig.DAILY.equals(dateConfig)) {
                    _builder.groupBy(dateGroup);
                }
                if (!hideDetails || !isInstance(_parameter)) {
                    _builder.groupBy(groupByContact ? contactGroup : productGroup);
                    _builder.addSubtotalAtGroupFooter(groupByContact ? contactGroup : productGroup,
                                    DynamicReports.sbt.first(currencyColumn).setStyle(txtStyle), netPriceProdSum);
                    // grouped by product or grouped by contact with only one product
                    if (!groupByContact || (groupByContact && getProductInst(_parameter).length == 1)) {
                        _builder.addSubtotalAtGroupFooter(groupByContact ? contactGroup : productGroup,
                                    DynamicReports.sbt.first(uomColumn).setStyle(txtStyle),
                                    quantityProdSum, unitPriceProdSum);
                    }
                }


                if (!DateConfig.YEARLY.equals(dateConfig)) {
                    _builder.addSubtotalAtGroupFooter(monthGroup, netPriceProdSum4Month,
                                DynamicReports.sbt.first(currencyColumn).setStyle(txtStyle));
                }
                    _builder.addSubtotalAtGroupFooter(yearGroup, netPriceProdSum4Year,
                                DynamicReports.sbt.first(currencyColumn).setStyle(txtStyle));
                    _builder.addSubtotalAtSummary(netPriceTotSum,
                                DynamicReports.sbt.first(currencyColumn).setStyle(txtStyle));

                    // grouped by product or grouped by contact with only one product
                if (!groupByContact || (groupByContact && getProductInst(_parameter).length == 1)) {
                    if (!DateConfig.YEARLY.equals(dateConfig)) {
                        _builder.addSubtotalAtGroupFooter(monthGroup, quantityProdSum4Month, unitPriceTotal4Month,
                                    DynamicReports.sbt.first(uomColumn).setStyle(txtStyle));
                    }
                    _builder.addSubtotalAtGroupFooter(yearGroup, quantityProdSum4Year, unitPriceTotal4Year,
                                     DynamicReports.sbt.first(uomColumn).setStyle(txtStyle));
                    _builder.addSubtotalAtSummary(quantityTotSum, unitPriceTotSum,
                                    DynamicReports.sbt.first(uomColumn).setStyle(txtStyle));
                }
            }
        }

        @Override
        protected ReportStyleBuilder getSubtotalStyle4Html(final Parameter _parameter)
            throws EFapsException
        {
            return DynamicReports.stl.style().bold()
                            .setTopBorder(DynamicReports.stl.penThin());
        }

        /**
         * Getter method for the instance variable {@link #filteredReport}.
         *
         * @return value of instance variable {@link #filteredReport}
         */
        protected SalesProductReport_Base getFilteredReport()
        {
            return this.filteredReport;
        }
    }

    public static class UnitPriceTotal
        extends AbstractSimpleExpression<BigDecimal>
    {

        private static final long serialVersionUID = 1L;
        private final VariableBuilder<BigDecimal> price;
        private final VariableBuilder<BigDecimal> quantity;

        public UnitPriceTotal(final String _name,
                              final VariableBuilder<BigDecimal> _quantity,
                              final VariableBuilder<BigDecimal> _price)
        {
            super(_name);
            this.price = _price;
            this.quantity = _quantity;
        }

        @Override
        public BigDecimal evaluate(final ReportParameters _reportParameters)
        {
            final BigDecimal quantitySumValue = _reportParameters.getValue(this.quantity);
            final BigDecimal priceSumValue = _reportParameters.getValue(this.price);
            BigDecimal ret;
            if (priceSumValue.compareTo(BigDecimal.ZERO) == 0) {
                ret = BigDecimal.ZERO;
            } else {
                ret = priceSumValue.divide(quantitySumValue, 4, BigDecimal.ROUND_HALF_UP);
            }
            return ret;
        }
    }

    public static class LinkExpression
        extends AbstractComplexExpression<EmbeddedLink>
    {

        private static final long serialVersionUID = 1L;

        public LinkExpression()
        {
            addExpression(DynamicReports.field("docOID", String.class));
        }

        @Override
        public EmbeddedLink evaluate(final List<?> _values,
                                     final ReportParameters _reportParameters)
        {
            final String oid = (String) _values.get(0);
            return EmbeddedLink.getJasperLink(oid);
        }
    }

    public static class DataBean
    {

        private String docOID;
        private String docName;
        private String docType;
        private DateTime docDate;
        private String docStatus;

        private String currency;
        private String contact;

        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal price;
        private Instance contactInst;
        private Instance productInst;
        private String productName;
        private String productDesc;
        private String productUoM;
        private BigDecimal productDiscount;

        public String getProductDesc()
        {
            return this.productDesc;
        }

        public DataBean setProductDesc(final String _productDesc)
        {
            this.productDesc = _productDesc;
            return this;
        }

        public String getDocOID()
        {
            return this.docOID;
        }

        public DataBean setDocOID(final String docOID)
        {
            this.docOID = docOID;
            return this;
        }

        public String getDocName()
        {
            return this.docName;
        }

        public DataBean setDocName(final String _docName)
        {
            this.docName = _docName;
            return this;
        }

        public String getCurrency()
        {
            return this.currency;
        }

        public DataBean setCurrency(final String currency)
        {
            this.currency = currency;
            return this;
        }

        public String getContact()
        {
            return this.contact;
        }

        public DataBean setContact(final String contact)
        {
            this.contact = contact;
            return this;
        }

        public DateTime getDocDate()
        {
            return this.docDate;
        }

        public DataBean setDate(final DateTime _docDate)
        {
            this.docDate = _docDate;
            return this;
        }

        public BigDecimal getQuantity()
        {
            return this.quantity;
        }

        public DataBean setQuantity(final BigDecimal _quantity)
        {
            this.quantity = _quantity;
            return this;
        }

        public Instance getContactInst()
        {
            return this.contactInst;
        }

        public DataBean setContactInst(final Instance _contactInst)
        {
            this.contactInst = _contactInst;
            return this;
        }

        public Instance getProductInst()
        {
            return this.productInst;
        }

        public DataBean setProductInst(final Instance _productInst)
        {
            this.productInst = _productInst;

            return this;
        }

        public String getProductName()
        {
            return this.productName;
        }

        public DataBean setProductName(final String productName)
        {
            this.productName = productName;
            return this;
        }

        public String getProductUoM()
        {
            return this.productUoM;
        }

        public DataBean setProductUoM(final String productUoM)
        {
            this.productUoM = productUoM;
            return this;
        }

        public BigDecimal getProductDiscount()
        {
            return this.productDiscount;
        }

        public DataBean setProductDiscount(final BigDecimal _productDiscount)
        {
            this.productDiscount = _productDiscount;
            return this;
        }

        /**
         * @return the unitPrice
         */
        public BigDecimal getUnitPrice()
        {
            return this.unitPrice;
        }

        /**
         * @param unitPrice the unitPrice to set
         */
        public DataBean setUnitPrice(final BigDecimal unitPrice)
        {
            this.unitPrice = unitPrice;
            return this;
        }

        /**
         * @return the price
         */
        public BigDecimal getPrice()
        {
            return this.price;
        }

        /**
         * Sets the price.
         *
         * @param price the price to set
         */
        public DataBean setPrice(final BigDecimal price)
        {
            this.price = price;
            return this;
        }

        /**
         * @return the docType
         */
        public String getDocType()
        {
            return this.docType;
        }

        /**
         * @param docType the docType to set
         */
        public DataBean setDocType(final String docType)
        {
            this.docType = docType;
            return this;
        }

        /**
         * @return the docStatus
         */
        public String getDocStatus()
        {
            return this.docStatus;
        }

        /**
         * @param docStatus the docStatus to set
         */
        public DataBean setDocStatus(final String _docStatus)
        {
            this.docStatus = _docStatus;
            return this;
        }

        public String getProduct()
        {
            return getProductName() + " " + getProductDesc();
        }
    }
}
