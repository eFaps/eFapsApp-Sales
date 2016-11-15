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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.lang3.ArrayUtils;
import org.efaps.admin.datamodel.Classification;
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
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.eql.ClassSelect;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeDate;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeMonth;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeYear;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.erp.RateInfo;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.ui.wicket.models.EmbeddedLink;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        /** Use net prices. */
        NET,
        /** Use cross prices. */
        CROSS;
    }

    /**
     * The Enum DateConfig.
     *
     * @author The eFaps Team
     */
    public enum GroupConfig
    {
        /** Group by Product. */
        PRODUCT,
        /** Group by Contact. */
        CONTACT,
        /** Group by product family. */
        PRODFAMILY;
    }

    /**
     * Logging instance used in this class.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(SalesProductReport.class);

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
        return new DynSalesProductReport(this);
    }

    /**
     * The Class DynSalesProductReport.
     */
    public static class DynSalesProductReport
        extends AbstractDynamicReport
    {

        /** The filtered report. */
        private final SalesProductReport_Base filteredReport;

        /**
         * Instantiates a new dyn sales product report.
         *
         * @param _filteredReport the filtered report
         */
        public DynSalesProductReport(final SalesProductReport_Base _filteredReport)
        {
            this.filteredReport = _filteredReport;
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
                final DateConfig dateConfig = evaluateDateConfig(_parameter);
                final GroupConfig groupConfig = evaluateGroupConfig(_parameter);
                final PriceConfig priceConfig = evaluatePriceConfig(_parameter);
                final CurrencyInst currencyInst = evaluateCurrencyInst(_parameter);

                final List<DataBean> data = new ArrayList<>();

                final QueryBuilder attrQueryBldr = getQueryBldrFromProperties(_parameter);
                add2QueryBuilder(_parameter, attrQueryBldr);
                if (getContactInst(_parameter).length > 0) {
                    if (isContactNegate(_parameter)) {
                        attrQueryBldr.addWhereAttrNotEqValue(CISales.DocumentSumAbstract.Contact,
                                        getContactInst(_parameter));
                    } else {
                        attrQueryBldr.addWhereAttrEqValue(CISales.DocumentSumAbstract.Contact,
                                        getContactInst(_parameter));
                    }
                }
                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.DocumentSumAbstract.ID);

                final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionSumAbstract);
                queryBldr.addWhereAttrInQuery(CISales.PositionSumAbstract.DocumentAbstractLink, attrQuery);
                if (getProductInst(_parameter).length > 0) {
                    if (isProductNegate(_parameter)) {
                        queryBldr.addWhereAttrNotEqValue(CISales.PositionSumAbstract.Product,
                                        getProductInst(_parameter));
                    } else {
                        queryBldr.addWhereAttrEqValue(CISales.PositionSumAbstract.Product, getProductInst(_parameter));
                    }
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
                final SelectBuilder selProductInst = new SelectBuilder()
                                .linkto(CISales.PositionSumAbstract.Product).instance();
                final SelectBuilder selProductName = new SelectBuilder()
                                .linkto(CISales.PositionSumAbstract.Product)
                                .attribute(CIProducts.ProductAbstract.Name);
                final SelectBuilder selProductDesc = new SelectBuilder()
                                .linkto(CISales.PositionSumAbstract.Product)
                                .attribute(CIProducts.ProductAbstract.Description);
                final SelectBuilder selDoc = new SelectBuilder()
                                .linkto(CISales.PositionSumAbstract.DocumentAbstractLink);
                final SelectBuilder selContactInst = new SelectBuilder(selDoc)
                                .linkto(CISales.DocumentSumAbstract.Contact).instance();
                final SelectBuilder selContactName = new SelectBuilder(selDoc)
                                .linkto(CISales.DocumentSumAbstract.Contact)
                                .attribute(CIContacts.Contact.Name);
                final SelectBuilder selDocDate = new SelectBuilder(selDoc)
                                .attribute(CISales.DocumentSumAbstract.Date);
                final SelectBuilder selDocType = new SelectBuilder(selDoc).type().label();
                final SelectBuilder selDocName = new SelectBuilder(selDoc)
                                .attribute(CISales.DocumentSumAbstract.Name);
                final SelectBuilder selDocOID = new SelectBuilder(selDoc).oid();
                final SelectBuilder selDocStatus = new SelectBuilder(selDoc).status().label();
                final SelectBuilder selCondtion = new SelectBuilder(selDoc)
                                .linkfrom(CISales.ChannelCondition2DocumentAbstract.ToAbstractLink)
                                .linkto(CISales.ChannelCondition2DocumentAbstract.FromAbstractLink)
                                .attribute(CISales.ChannelAbstract.Name);

                if (Sales.REPORT_SALESPROD_CONDITION.get()) {
                    multi.addSelect(selCondtion);
                }

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
                                    .setProductDiscount(multi.<BigDecimal>getAttribute(
                                                                    CISales.PositionSumAbstract.Discount));

                    if (Sales.REPORT_SALESPROD_CONDITION.get()) {
                        dataBean.setCondition(multi.getSelect(selCondtion));
                    }

                    data.add(dataBean);

                    final UoM uoM = Dimension.getUoM(multi.<Long>getAttribute(CISales.PositionSumAbstract.UoM));
                    BigDecimal quantityTmp = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.Quantity);
                    quantityTmp = quantityTmp.multiply(new BigDecimal(uoM.getNumerator()))
                                    .divide(new BigDecimal(uoM.getDenominator()), 4, BigDecimal.ROUND_HALF_UP);
                    dataBean.setQuantity(quantityTmp).setProductUoM(uoM.getName());

                    final Instance curInst = multi.getSelect(selCurInst);
                    final BigDecimal unitPrice;
                    final BigDecimal price;
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

                sort(_parameter, data);

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
                        String criteria = (GroupConfig.PRODFAMILY.equals(groupConfig)
                                                ? bean.getProductFamily()
                                                : bean.getProductInst().getOid())
                                        + "-" + bean.getContactInst().getOid();
                        switch (dateConfig) {
                            case DAILY:
                                criteria = criteria  + bean.getDocDate().toString("-YYYY-MM-dd");
                                break;
                            case MONTHLY:
                                criteria = criteria  + bean.getDocDate().toString("-YYYY-MM");
                                break;
                            case YEARLY:
                                criteria = criteria  + bean.getDocDate().toString("YYYY");
                                break;
                            default:
                                break;
                        }

                        if (!added.containsKey(criteria)) {
                            added.put(criteria, bean);
                            dataSource.add(bean);
                        } else {
                            final DataBean current = added.get(criteria);
                            current.setPrice(current.getPrice().add(bean.getPrice()))
                                            .setQuantity(current.getQuantity().add(bean.getQuantity()));
                            if (current.getPrice().compareTo(BigDecimal.ZERO) == 0) {
                                current.setUnitPrice(BigDecimal.ZERO);
                            } else {
                                current .setUnitPrice(current.getPrice().divide(current.getQuantity(),
                                                BigDecimal.ROUND_HALF_UP));
                            }
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

        /**
         * Sort.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _data the data
         * @throws EFapsException on error
         */
        protected void sort(final Parameter _parameter,
                            final List<DataBean> _data)
            throws EFapsException
        {
            final ComparatorChain<DataBean> comparator = new ComparatorChain<>();
            switch (evaluateDateConfig(_parameter)) {
                case DAILY:
                case LATEST:
                    comparator.addComparator(new Comparator<DataBean>()
                    {

                        @Override
                        public int compare(final DataBean _arg0,
                                           final DataBean _arg1)
                        {
                            return _arg0.getDocDate().compareTo(_arg1.getDocDate());
                        }
                    });
                    break;
                case MONTHLY:
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
                    break;
                case YEARLY:
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
                    break;
                default:
                    break;
            }

            switch (evaluateGroupConfig(_parameter)) {
                case CONTACT:
                    comparator.addComparator(new Comparator<DataBean>()
                    {

                        @Override
                        public int compare(final DataBean _arg0,
                                           final DataBean _arg1)
                        {
                            return _arg0.getContact().compareTo(_arg1.getContact());
                        }
                    });
                    break;
                case PRODUCT:
                    comparator.addComparator(new Comparator<DataBean>()
                    {

                        @Override
                        public int compare(final DataBean _arg0,
                                           final DataBean _arg1)
                        {
                            return _arg0.getProductName().compareTo(_arg1.getProductName());
                        }
                    });
                    break;
                case PRODFAMILY:
                    comparator.addComparator(new Comparator<DataBean>()
                    {

                        @Override
                        public int compare(final DataBean _arg0,
                                           final DataBean _arg1)
                        {
                            return _arg0.getProductFamily().compareTo(_arg1.getProductFamily());
                        }
                    });
                    break;
                default:
                    break;
            }

            Collections.sort(_data, comparator);
        }

        /**
         * Checks if is hide details.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return true, if is hide details
         * @throws EFapsException on error
         */
        protected boolean isHideDetails(final Parameter _parameter)
            throws EFapsException
        {
            final Map<String, Object> filter = this.filteredReport.getFilterMap(_parameter);
            return (Boolean) filter.get("hideDetails");
        }

        /**
         * Evaluate currency inst.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the currency inst
         * @throws EFapsException on error
         */
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

        /**
         * Evaluate date config.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the date config
         * @throws EFapsException on error
         */
        protected DateConfig evaluateDateConfig(final Parameter _parameter)
            throws EFapsException
        {
            final DateConfig ret;
            final Map<String, Object> filter = this.filteredReport.getFilterMap(_parameter);
            final EnumFilterValue value = (EnumFilterValue) filter.get("dateConfig");
            if (value != null) {
                ret = (DateConfig) value.getObject();
            } else {
                ret = DateConfig.DAILY;
            }
            return ret;
        }

        /**
         * Evaluate price config.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the price config
         * @throws EFapsException on error
         */
        protected PriceConfig evaluatePriceConfig(final Parameter _parameter)
            throws EFapsException
        {
            final PriceConfig ret;
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
         * Evaluate group config.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the group config
         * @throws EFapsException on error
         */
        protected GroupConfig evaluateGroupConfig(final Parameter _parameter)
            throws EFapsException
        {
            final GroupConfig ret;
            final Map<String, Object> filter = this.filteredReport.getFilterMap(_parameter);
            final EnumFilterValue value = (EnumFilterValue) filter.get("groupConfig");
            if (value != null) {
                ret = (GroupConfig) value.getObject();
            } else {
                ret = GroupConfig.PRODUCT;
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
            if (Sales.REPORT_SALESPROD_CONDITION.get() && ArrayUtils.isNotEmpty(getConditionInsts(_parameter))) {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.ChannelCondition2DocumentAbstract);
                attrQueryBldr.addWhereAttrEqValue(CISales.ChannelCondition2DocumentAbstract.FromAbstractLink,
                                getConditionInsts(_parameter));

                if (isConditionNegate(_parameter)) {
                    _queryBldr.addWhereAttrNotInQuery(CISales.DocumentAbstract.ID,
                            attrQueryBldr.getAttributeQuery(CISales.ChannelCondition2DocumentAbstract.ToAbstractLink));
                } else {
                    _queryBldr.addWhereAttrInQuery(CISales.DocumentAbstract.ID,
                            attrQueryBldr.getAttributeQuery(CISales.ChannelCondition2DocumentAbstract.ToAbstractLink));
                }
            }

            _queryBldr.addWhereAttrGreaterValue(CISales.DocumentSumAbstract.Date, dateFrom.minusDays(1));
            _queryBldr.addWhereAttrLessValue(CISales.DocumentSumAbstract.Date, dateTo.plusDays(1)
                            .withTimeAtStartOfDay());
        }

        /**
         * Checks if the report is in a contact instance.
         *
         * @param _parameter the _parameter
         * @return true, if is contact
         * @throws EFapsException on error
         */
        protected boolean isContact(final Parameter _parameter)
            throws EFapsException
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
         * Checks if is contact negate.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return true, if is contact negate
         * @throws EFapsException on error
         */
        protected boolean isContactNegate(final Parameter _parameter)
            throws EFapsException
        {
            boolean ret = false;
            if (!isContact(_parameter)) {
                final Map<String, Object> filterMap = this.filteredReport.getFilterMap(_parameter);
                final InstanceSetFilterValue filter = (InstanceSetFilterValue) filterMap.get("contact");
                ret = filter != null && filter.isNegate();
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

        /**
         * Gets the product inst.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the product inst
         * @throws EFapsException on error
         */
        protected Object[] getProductInst(final Parameter _parameter)
            throws EFapsException
        {
            final Object[] ret;
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
         * Checks if is contact negate.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return true, if is contact negate
         * @throws EFapsException on error
         */
        protected boolean isProductNegate(final Parameter _parameter)
            throws EFapsException
        {
            boolean ret = false;
            if (!isProduct(_parameter)) {
                final Map<String, Object> filterMap = this.filteredReport.getFilterMap(_parameter);
                final InstanceSetFilterValue filter = (InstanceSetFilterValue) filterMap.get("product");
                ret = filter != null && filter.isNegate();
            }
            return ret;
        }

        /**
         * Gets the product inst.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the product inst
         * @throws EFapsException on error
         */
        protected Object[] getConditionInsts(final Parameter _parameter)
            throws EFapsException
        {
            final Object[] ret;
            final Map<String, Object> filterMap = this.filteredReport.getFilterMap(_parameter);
            final InstanceSetFilterValue filter = (InstanceSetFilterValue) filterMap.get("condition");
            if (filter == null || (filter != null && filter.getObject() == null)) {
                ret = new Object[] {};
            } else {
                ret = filter.getObject().toArray();
            }
            return ret;
        }

        /**
         * Checks if is contact negate.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return true, if is contact negate
         * @throws EFapsException on error
         */
        protected boolean isConditionNegate(final Parameter _parameter)
            throws EFapsException
        {
            final Map<String, Object> filterMap = this.filteredReport.getFilterMap(_parameter);
            final InstanceSetFilterValue filter = (InstanceSetFilterValue) filterMap.get("condition");
            final boolean ret = filter != null && filter.isNegate();
            return ret;
        }

        /**
         * Checks if is instance.
         *
         * @param _parameter the _parameter
         * @return true, if is instance
         */
        protected boolean isInstance(final Parameter _parameter)
        {
            return _parameter.getInstance() != null && _parameter.getInstance().isValid();
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final DateConfig dateConfig = evaluateDateConfig(_parameter);
            final GroupConfig groupConfig = evaluateGroupConfig(_parameter);

            final boolean hideDetails = isHideDetails(_parameter);

            final TextColumnBuilder<String> contactColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("Contact"), "contact",
                            DynamicReports.type.stringType()).setWidth(250);
            final TextColumnBuilder<String> productColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("Product"), "product",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> productFamilyColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("ProductFamily"), "productFamily",
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
            final TextColumnBuilder<String> conditionColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("Condition"), "condition",
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
            final ColumnGroupBuilder productFamilyGroup = DynamicReports.grp.group(productFamilyColumn)
                            .groupByDataType();

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
                    switch (groupConfig) {
                        case CONTACT:
                            quantity4ProdCon.setResetGroup(contactGroup);
                            price4ProdCon.setResetGroup(contactGroup);
                            break;
                        case PRODFAMILY:
                            quantity4ProdCon.setResetGroup(productFamilyGroup);
                            price4ProdCon.setResetGroup(productFamilyGroup);
                            break;
                        case PRODUCT:
                            quantity4ProdCon.setResetGroup(productGroup);
                            price4ProdCon.setResetGroup(productGroup);
                            break;
                        default:
                            break;
                    }
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
                switch (groupConfig) {
                    case CONTACT:
                        _builder.addColumn(productNameColumn, productDescColumn);
                        break;
                    case PRODFAMILY:
                        break;
                    case PRODUCT:
                        break;
                    default:
                        break;
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

            if (hideDetails) {
                switch (groupConfig) {
                    case CONTACT:
                        if (isProduct(_parameter)) {
                            _builder.addColumn(contactColumn);
                        } else {
                            _builder.addColumn(productNameColumn, productDescColumn);
                        }
                        break;
                    case PRODFAMILY:
                        if (isContact(_parameter)) {
                            _builder.addColumn(productFamilyColumn);
                        } else {
                            _builder.addColumn(contactColumn);
                        }
                        break;
                    case PRODUCT:
                        if (isContact(_parameter)) {
                            _builder.addColumn(productNameColumn, productDescColumn);
                        } else {
                            _builder.addColumn(contactColumn);
                        }
                        break;
                    default:
                        break;

                }
            } else {
                if (!isInstance(_parameter)) {
                    switch (groupConfig) {
                        case CONTACT:
                            _builder.addColumn(productNameColumn, productDescColumn);
                            break;
                        case PRODUCT:
                            _builder.addColumn(contactColumn);
                            break;
                        case PRODFAMILY:
                            break;
                        default:
                            break;
                    }
                }

                if (getExType().equals(ExportType.HTML)) {
                    _builder.addColumn(linkColumn);
                }
                _builder.addColumn(docTypeColumn);
                if (Sales.REPORT_SALESPROD_CONDITION.get()) {
                    _builder.addColumn(conditionColumn);
                }
                _builder.addColumn(docNameColumn.setFixedWidth(150),
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
                    switch (groupConfig) {
                        case CONTACT:
                            _builder.groupBy(contactGroup);
                            _builder.addSubtotalAtGroupFooter(contactGroup, DynamicReports.sbt.first(currencyColumn)
                                            .setStyle(txtStyle), netPriceProdSum);
                            if (getProductInst(_parameter).length == 1) {
                                _builder.addSubtotalAtGroupFooter(contactGroup,
                                            DynamicReports.sbt.first(uomColumn).setStyle(txtStyle),
                                            quantityProdSum, unitPriceProdSum);
                            }
                            break;
                        case PRODFAMILY:
                            _builder.groupBy(productFamilyGroup);
                            _builder.addSubtotalAtGroupFooter(productFamilyGroup, DynamicReports.sbt.first(
                                            currencyColumn).setStyle(txtStyle), netPriceProdSum);
                            break;
                        case PRODUCT:
                            _builder.groupBy(productGroup);
                            _builder.addSubtotalAtGroupFooter(productGroup, DynamicReports.sbt.first(currencyColumn)
                                            .setStyle(txtStyle), netPriceProdSum);
                            _builder.addSubtotalAtGroupFooter(productGroup,
                                            DynamicReports.sbt.first(uomColumn).setStyle(txtStyle),
                                            quantityProdSum, unitPriceProdSum);
                            break;
                        default:
                            break;
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

                // only in case of one product present the price per unit
                if (getProductInst(_parameter).length == 1) {
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

    /**
     * The Class UnitPriceTotal.
     */
    public static class UnitPriceTotal
        extends AbstractSimpleExpression<BigDecimal>
    {

        /** The Constant serialVersionUID. */
        private static final long serialVersionUID = 1L;

        /** The price. */
        private final VariableBuilder<BigDecimal> price;

        /** The quantity. */
        private final VariableBuilder<BigDecimal> quantity;

        /**
         * Instantiates a new unit price total.
         *
         * @param _name the name
         * @param _quantity the quantity
         * @param _price the price
         */
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
            final BigDecimal ret;
            if (priceSumValue.compareTo(BigDecimal.ZERO) == 0) {
                ret = BigDecimal.ZERO;
            } else {
                ret = priceSumValue.divide(quantitySumValue, 4, BigDecimal.ROUND_HALF_UP);
            }
            return ret;
        }
    }

    /**
     * The Class LinkExpression.
     */
    public static class LinkExpression
        extends AbstractComplexExpression<EmbeddedLink>
    {

        /** The Constant serialVersionUID. */
        private static final long serialVersionUID = 1L;

        /**
         * Instantiates a new link expression.
         */
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

    /**
     * The Class DataBean.
     */
    public static class DataBean
    {

        /** The doc oid. */
        private String docOID;

        /** The doc name. */
        private String docName;

        /** The doc type. */
        private String docType;

        /** The doc date. */
        private DateTime docDate;

        /** The doc status. */
        private String docStatus;

        /** The currency. */
        private String currency;

        /** The contact. */
        private String contact;

        /** The quantity. */
        private BigDecimal quantity;

        /** The unit price. */
        private BigDecimal unitPrice;

        /** The price. */
        private BigDecimal price;

        /** The contact inst. */
        private Instance contactInst;

        /** The product inst. */
        private Instance productInst;

        /** The product name. */
        private String productName;

        /** The product desc. */
        private String productDesc;

        /** The product uo m. */
        private String productUoM;

        /** The product discount. */
        private BigDecimal productDiscount;

        /** The product family. */
        private String productFamily;

        /** The condition. */
        private String condition;

        /**
         * Gets the product desc.
         *
         * @return the product desc
         */
        public String getProductDesc()
        {
            return this.productDesc;
        }

        /**
         * Sets the product desc.
         *
         * @param _productDesc the product desc
         * @return the data bean
         */
        public DataBean setProductDesc(final String _productDesc)
        {
            this.productDesc = _productDesc;
            return this;
        }

        /**
         * Gets the doc oid.
         *
         * @return the doc oid
         */
        public String getDocOID()
        {
            return this.docOID;
        }

        /**
         * Sets the doc oid.
         *
         * @param _docOID the doc oid
         * @return the data bean
         */
        public DataBean setDocOID(final String _docOID)
        {
            this.docOID = _docOID;
            return this;
        }

        /**
         * Gets the doc name.
         *
         * @return the doc name
         */
        public String getDocName()
        {
            return this.docName;
        }

        /**
         * Sets the doc name.
         *
         * @param _docName the doc name
         * @return the data bean
         */
        public DataBean setDocName(final String _docName)
        {
            this.docName = _docName;
            return this;
        }

        /**
         * Gets the currency.
         *
         * @return the currency
         */
        public String getCurrency()
        {
            return this.currency;
        }

        /**
         * Sets the currency.
         *
         * @param _currency the currency
         * @return the data bean
         */
        public DataBean setCurrency(final String _currency)
        {
            this.currency = _currency;
            return this;
        }

        /**
         * Gets the contact.
         *
         * @return the contact
         */
        public String getContact()
        {
            return this.contact;
        }

        /**
         * Sets the contact.
         *
         * @param _contact the contact
         * @return the data bean
         */
        public DataBean setContact(final String _contact)
        {
            this.contact = _contact;
            return this;
        }

        /**
         * Gets the doc date.
         *
         * @return the doc date
         */
        public DateTime getDocDate()
        {
            return this.docDate;
        }

        /**
         * Sets the date.
         *
         * @param _docDate the doc date
         * @return the data bean
         */
        public DataBean setDate(final DateTime _docDate)
        {
            this.docDate = _docDate;
            return this;
        }

        /**
         * Gets the quantity.
         *
         * @return the quantity
         */
        public BigDecimal getQuantity()
        {
            return this.quantity;
        }

        /**
         * Sets the quantity.
         *
         * @param _quantity the quantity
         * @return the data bean
         */
        public DataBean setQuantity(final BigDecimal _quantity)
        {
            this.quantity = _quantity;
            return this;
        }

        /**
         * Gets the contact inst.
         *
         * @return the contact inst
         */
        public Instance getContactInst()
        {
            return this.contactInst;
        }

        /**
         * Sets the contact inst.
         *
         * @param _contactInst the contact inst
         * @return the data bean
         */
        public DataBean setContactInst(final Instance _contactInst)
        {
            this.contactInst = _contactInst;
            return this;
        }

        /**
         * Gets the product inst.
         *
         * @return the product inst
         */
        public Instance getProductInst()
        {
            return this.productInst;
        }

        /**
         * Sets the product inst.
         *
         * @param _productInst the product inst
         * @return the data bean
         */
        public DataBean setProductInst(final Instance _productInst)
        {
            this.productInst = _productInst;

            return this;
        }

        /**
         * Gets the product name.
         *
         * @return the product name
         */
        public String getProductName()
        {
            return this.productName;
        }

        /**
         * Sets the product name.
         *
         * @param _productName the product name
         * @return the data bean
         */
        public DataBean setProductName(final String _productName)
        {
            this.productName = _productName;
            return this;
        }

        /**
         * Gets the product uo m.
         *
         * @return the product uo m
         */
        public String getProductUoM()
        {
            return this.productUoM;
        }

        /**
         * Sets the product uo m.
         *
         * @param _productUoM the product uo m
         * @return the data bean
         */
        public DataBean setProductUoM(final String _productUoM)
        {
            this.productUoM = _productUoM;
            return this;
        }

        /**
         * Gets the product discount.
         *
         * @return the product discount
         */
        public BigDecimal getProductDiscount()
        {
            return this.productDiscount;
        }

        /**
         * Sets the product discount.
         *
         * @param _productDiscount the product discount
         * @return the data bean
         */
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
         * Sets the unit price.
         *
         * @param _unitPrice the unitPrice to set
         * @return the data bean
         */
        public DataBean setUnitPrice(final BigDecimal _unitPrice)
        {
            this.unitPrice = _unitPrice;
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
         * @param _price the price to set
         * @return the data bean
         */
        public DataBean setPrice(final BigDecimal _price)
        {
            this.price = _price;
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
         * Sets the doc type.
         *
         * @param _docType the doc type
         * @return the data bean
         */
        public DataBean setDocType(final String _docType)
        {
            this.docType = _docType;
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
         * Sets the doc status.
         *
         * @param _docStatus the doc status
         * @return the data bean
         */
        public DataBean setDocStatus(final String _docStatus)
        {
            this.docStatus = _docStatus;
            return this;
        }

        /**
         * Gets the product.
         *
         * @return the product
         */
        public String getProduct()
        {
            return getProductName() + " " + getProductDesc();
        }

        /**
         * Gets the product.
         *
         * @return the product
         * @throws EFapsException on error
         */
        public String getProductFamily()
        {
            if (this.productFamily == null) {
                try {
                    final PrintQuery print = new PrintQuery(getProductInst());
                    final SelectBuilder selClazz = SelectBuilder.get().clazz().type();
                    print.addSelect(selClazz);
                    print.execute();
                    final List<Classification> clazzList = print.<List<Classification>>getSelect(selClazz);
                    if (clazzList == null) {
                        this.productFamily = "-";
                    } else {
                        final ClassSelect classSel = new ClassSelect()
                        {
                            @Override
                            protected int getLevel()
                            {
                                int ret = -1;
                                try {
                                    ret = Sales.REPORT_SALESPROD_PRODFAMLEVEL.get();
                                } catch (final EFapsException e) {
                                    LOG.error("Catched error on Configuration evaluation.");
                                }
                                return ret;
                            }
                        };

                        this.productFamily = (String) classSel.evalValue(clazzList);
                    }
                } catch (final EFapsException e) {
                    LOG.error("Catched error on Family evaluation.");
                }
            }
            return this.productFamily;
        }

        /**
         * Gets the condition.
         *
         * @return the condition
         */
        public String getCondition()
        {
            return this.condition;
        }

        /**
         * Sets the condition.
         *
         * @param _condition the condition
         * @return the data bean
         */
        public DataBean setCondition(final String _condition)
        {
            this.condition = _condition;
            return this;
        }
    }
}
