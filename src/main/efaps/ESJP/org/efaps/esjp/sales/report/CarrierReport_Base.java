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
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.BooleanUtils;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.erp.AbstractGroupedByDate;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.sales.report.DocPositionGroupedByDate_Base.ValueList;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabMeasureBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabRowGroupBuilder;
import net.sf.dynamicreports.report.constant.Calculation;
import net.sf.dynamicreports.report.constant.HorizontalTextAlignment;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("ab7175eb-d0cc-4c1c-8679-352a177fe363")
@EFapsApplication("eFapsApp-Sales")
public abstract class CarrierReport_Base
    extends FilteredReport
{
    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(CarrierReport.class);

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
        final String mime = getProperty(_parameter, "Mime");
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
     * @param _parameter Parameter as passed by the eFasp API
     * @return the report class
     * @throws EFapsException on error
     */
    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new DynCarrierReport(this);
    }

    /**
     * Dynamic Report.
     */
    public static class DynCarrierReport
        extends AbstractDynamicReport
    {

        /** The filtered report. */
        private final CarrierReport_Base filteredReport;

        /**
         * Instantiates a new dyn carrier report.
         *
         * @param _carrierReport the _carrier report
         */
        public DynCarrierReport(final CarrierReport_Base _carrierReport)
        {
            this.filteredReport = _carrierReport;
        }

        @SuppressWarnings("unchecked")
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
                final ValueList values = getData(_parameter);
                ret = new JRMapCollectionDataSource((Collection) values);
                getFilteredReport().cache(_parameter, ret);
            }
            return ret;
        }

        /**
         * Gets the data.
         *
         * @param _parameter the _parameter
         * @return the data
         * @throws EFapsException the e faps exception
         */
        protected ValueList getData(final Parameter _parameter)
            throws EFapsException
        {
            final DocPositionGroupedByDate ds = new DocPositionGroupedByDate()
            {

                @Override
                protected void add2QueryBuilder(final Parameter _parameter,
                                                final QueryBuilder _queryBldr)
                    throws EFapsException
                {
                    super.add2QueryBuilder(_parameter, _queryBldr);
                    DynCarrierReport.this.add2QueryBuilder(_parameter, _queryBldr);
                }

                @Override
                protected void add2Print(final Parameter _parameter,
                                         final MultiPrintQuery _multi)
                    throws EFapsException
                {
                    super.add2Print(_parameter, _multi);
                    final SelectBuilder selCarrName = SelectBuilder.get()
                                    .linkto(CISales.DeliveryNotePosition.DeliveryNote)
                                    .linkto(CISales.DeliveryNote.CarrierLink)
                                    .attribute(CIContacts.Contact.Name);
                    _multi.addSelect(selCarrName);
                }

                @Override
                protected void add2RowMap(final Parameter _parameter,
                                          final MultiPrintQuery _multi,
                                          final Map<String, Object> _map)
                    throws EFapsException
                {
                    super.add2RowMap(_parameter, _multi, _map);
                    final SelectBuilder selCarrName = SelectBuilder.get()
                                    .linkto(CISales.DeliveryNotePosition.DeliveryNote)
                                    .linkto(CISales.DeliveryNote.CarrierLink)
                                    .attribute(CIContacts.Contact.Name);
                    _map.put("carrier", _multi.getSelect(selCarrName));

                    final DateTime date = (DateTime) _map.get("docDate");
                    _map.put("docName", _map.get("docName") + " "
                    + date.toString(DateTimeFormat.shortDate().withLocale(Context.getThreadContext().getLocale())));
                }
            };
            final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);
            final DateTime start;
            final DateTime end;
            if (filter.containsKey("dateFrom")) {
                start = (DateTime) filter.get("dateFrom");
            } else {
                start = new DateTime();
            }
            if (filter.containsKey("dateTo")) {
                end = (DateTime) filter.get("dateTo");
            } else {
                end = new DateTime();
            }
            final AbstractGroupedByDate.DateGroup dateGroup;
            if (filter.containsKey("dateGroup") && filter.get("dateGroup") != null) {
                dateGroup = (AbstractGroupedByDate.DateGroup) ((EnumFilterValue) filter.get("dateGroup")).getObject();
            } else {
                dateGroup = DocumentSumGroupedByDate_Base.DateGroup.MONTH;
            }
            final Properties props = new Properties();
            props.put("StatusGroup", CISales.DeliveryNoteStatus.getType().getName());
            props.put("Status", "!" + CISales.DeliveryNoteStatus.Canceled.key);
            return ds.getValueList(_parameter, start, end, dateGroup, props, CISales.DeliveryNote.getType());
        }

        /**
         * @param _parameter Parameter as passed by the eFaps API
         * @param _queryBldr queryBuilder to add to
         * @throws EFapsException on error
         */
        protected void add2QueryBuilder(final Parameter _parameter,
                                        final QueryBuilder _queryBldr)
            throws EFapsException
        {
            final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);
            if (filter.containsKey("carrier")) {
                final InstanceSetFilterValue filterValue = (InstanceSetFilterValue) filter.get("carrier");
                if (filterValue != null) {
                    final Iterator<Instance> instanceIter = filterValue.getObject().iterator();
                    while (instanceIter.hasNext()) {
                        final Instance instance = instanceIter.next();
                        if (!instance.isValid()) {
                            instanceIter.remove();
                        }
                    }
                    if (!filterValue.getObject().isEmpty()) {
                        final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.DeliveryNote);
                        if (filterValue.isNegate()) {
                            attrQueryBldr.addWhereAttrNotEqValue(CISales.DeliveryNote.CarrierLink,
                                        filterValue.getObject().toArray());
                        } else {
                            attrQueryBldr.addWhereAttrEqValue(CISales.DeliveryNote.CarrierLink,
                                            filterValue.getObject().toArray());
                        }
                        _queryBldr.addWhereAttrInQuery(CISales.PositionAbstract.DocumentAbstractLink,
                                attrQueryBldr.getAttributeQuery(CISales.DocumentAbstract.ID));
                    }
                }
            }
            if (filter.containsKey("contact")) {
                final InstanceSetFilterValue filterValue = (InstanceSetFilterValue) filter.get("contact");
                if (filterValue != null) {
                    final Iterator<Instance> instanceIter = filterValue.getObject().iterator();
                    while (instanceIter.hasNext()) {
                        final Instance instance = instanceIter.next();
                        if (!instance.isValid()) {
                            instanceIter.remove();
                        }
                    }
                    if (!filterValue.getObject().isEmpty()) {
                        final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.DeliveryNote);
                        if (filterValue.isNegate()) {
                            attrQueryBldr.addWhereAttrNotEqValue(CISales.DeliveryNote.Contact,
                                        filterValue.getObject().toArray());
                        } else {
                            attrQueryBldr.addWhereAttrEqValue(CISales.DeliveryNote.Contact,
                                            filterValue.getObject().toArray());
                        }
                        _queryBldr.addWhereAttrInQuery(CISales.PositionAbstract.DocumentAbstractLink,
                                attrQueryBldr.getAttributeQuery(CISales.DocumentAbstract.ID));
                    }
                }
            }
        }

        @Override
        protected void addColumnDefinition(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final CrosstabBuilder crosstab = DynamicReports.ctab.crosstab();

            final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);

            final CrosstabRowGroupBuilder<String> carrierGroup = DynamicReports.ctab
                            .rowGroup("carrier", String.class)
                            .setShowTotal(true);
            crosstab.addRowGroup(carrierGroup);

            if (BooleanUtils.isTrue((Boolean) filter.get("groupByContact"))) {
                final CrosstabRowGroupBuilder<String> docGroup = DynamicReports.ctab
                            .rowGroup("contact", String.class)
                            .setShowTotal(true);
                crosstab.addRowGroup(docGroup);
            }

            if (BooleanUtils.isTrue((Boolean) filter.get("showDocName"))) {
                final CrosstabRowGroupBuilder<String> docGroup = DynamicReports.ctab
                            .rowGroup("docName", String.class)
                            .setShowTotal(true);
                crosstab.addRowGroup(docGroup);
            }
            final CrosstabRowGroupBuilder<String> rowColGroup = DynamicReports.ctab
                            .rowGroup("productName", String.class)
                            .setShowTotal(true)
                            .setHeaderWidth(150);
            crosstab.addRowGroup(rowColGroup);

            final CrosstabRowGroupBuilder<String> rowTypeGroup = DynamicReports.ctab
                            .rowGroup("productDescr", String.class)
                            .setHeaderWidth(250)
                            .setHeaderHorizontalTextAlignment(HorizontalTextAlignment.LEFT)
                            .setShowTotal(false);
            crosstab.addRowGroup(rowTypeGroup);

            final CrosstabRowGroupBuilder<String> uomGroup = DynamicReports.ctab.rowGroup("uoMStr", String.class)
                            .setHeaderWidth(50)
                            .setShowTotal(false);
            crosstab.addRowGroup(uomGroup);

            final CrosstabColumnGroupBuilder<String> columnGroup = DynamicReports.ctab.columnGroup("partial",
                            String.class).setShowTotal(true);
            crosstab.addColumnGroup(columnGroup);

            final CrosstabMeasureBuilder<BigDecimal> quantityMeasure = DynamicReports.ctab.measure(
                            DBProperties.getProperty(DocPositionReport.class.getName() + ".quantity"),
                            "quantity", BigDecimal.class, Calculation.SUM);

            crosstab.setCellWidth(200);

            crosstab.addMeasure(quantityMeasure);

            _builder.addSummary(crosstab);
        }

        /**
         * Getter method for the instance variable {@link #filteredReport}.
         *
         * @return value of instance variable {@link #filteredReport}
         */
        protected CarrierReport_Base getFilteredReport()
        {
            return this.filteredReport;
        }
    }
}
