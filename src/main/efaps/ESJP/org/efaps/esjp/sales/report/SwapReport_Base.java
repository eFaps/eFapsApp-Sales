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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.BooleanUtils;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeDate;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.ColumnBuilder;
import net.sf.dynamicreports.report.builder.column.ComponentColumnBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.grid.ColumnGridComponentBuilder;
import net.sf.dynamicreports.report.builder.grid.ColumnTitleGroupBuilder;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

/**
 * The Class SwapReport_Base.
 *
 * @author The eFaps Team
 */
@EFapsUUID("3dc21670-5ae1-4646-ae86-4ed61ddab0a6")
@EFapsApplication("eFapsApp-Sales")
public abstract class SwapReport_Base
    extends FilteredReport
{

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SwapReport.class);

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
        final String mime = getProperty(_parameter, "Mime", "pdf");
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
        return new DynSwapReport(this);
    }

    /**
     * The Class DynPaymentReport.
     */
    public static class DynSwapReport
        extends AbstractDynamicReport
    {

        /** The filtered report. */
        private final SwapReport_Base filteredReport;

        /**
         * Instantiates a new dyn swap report.
         *
         * @param _filteredReport the filtered report
         */
        public DynSwapReport(final SwapReport_Base _filteredReport)
        {
            this.filteredReport = _filteredReport;
        }

        /**
         * Gets the filtered report.
         *
         * @return the filtered report
         */
        public SwapReport_Base getFilteredReport()
        {
            return this.filteredReport;
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

                final QueryBuilder attrQueryBldr = getQueryBldr(_parameter);
                add2QueryBuilder(_parameter, attrQueryBldr);

                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CIERP.DocumentAbstract.ID);

                final List<Map<Instance, Set<Instance>>> cols = new ArrayList<>();

                final Map<Instance, Set<Instance>> instances = new HashMap<>();

                final QueryBuilder queryBldr = new QueryBuilder(CISales.Document2Document4Swap);
                queryBldr.addWhereAttrInQuery(CISales.Document2Document4Swap.ToLink, attrQuery);

                final AttributeQuery attrQuery2 = new QueryBuilder(CISales.Document2Document4Swap)
                                .getAttributeQuery(CISales.Document2Document4Swap.FromLink);
                queryBldr.addWhereAttrNotInQuery(CISales.Document2Document4Swap.ToLink, attrQuery2);

                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder selToInst = SelectBuilder.get().linkto(CISales.Document2Document4Swap.ToLink)
                                .instance();
                final SelectBuilder selFromInst = SelectBuilder.get().linkto(CISales.Document2Document4Swap.FromLink)
                                .instance();
                multi.addSelect(selToInst, selFromInst);
                multi.execute();
                while (multi.next()) {
                    final Instance toInst = multi.getSelect(selToInst);
                    final Instance fromInst = multi.getSelect(selFromInst);
                    SwapReport_Base.LOG.debug("FromInst: {}", fromInst);
                    SwapReport_Base.LOG.debug("ToInst: {}", toInst);
                    final Set<Instance> toInsts;
                    if (instances.containsKey(fromInst)) {
                        toInsts = instances.get(fromInst);
                    } else {
                        toInsts = new HashSet<>();
                        instances.put(fromInst, toInsts);
                    }
                    toInsts.add(toInst);
                }
                cols.add(instances);
                Map<Instance, Set<Instance>> map = instances;
                while (!map.isEmpty()) {
                    map = getFromMap(_parameter, map.keySet());
                    if (!map.isEmpty()) {
                        cols.add(map);
                    }
                }

                final Set<Instance> docInsts = new HashSet<>();
                for (final Map<Instance, Set<Instance>> col : cols) {
                    for (final Entry<Instance, Set<Instance>> entry : col.entrySet()) {
                        docInsts.add(entry.getKey());
                        for (final Instance inst : entry.getValue()) {
                            docInsts.add(inst);
                        }
                    }
                }
                final Map<Instance, Object[]> docValues = new HashMap<>();
                final MultiPrintQuery docMulti = new MultiPrintQuery(new ArrayList<>(docInsts));
                docMulti.addAttribute(CISales.DocumentAbstract.Name, CISales.DocumentAbstract.Date);
                docMulti.execute();
                while (docMulti.next()) {
                    final String name = docMulti.getAttribute(CISales.DocumentAbstract.Name);
                    final DateTime date = docMulti.getAttribute(CISales.DocumentAbstract.Date);
                    docValues.put(docMulti.getCurrentInstance(), new Object[] { name, date });
                }

                final List<Map<String, ?>> tmpValues = new ArrayList<>();
                final Iterator<Map<Instance, Set<Instance>>> iter = cols.iterator();

                // add the first one
                if (iter.hasNext()) {
                    final Map<Instance, Set<Instance>> first = iter.next();
                    for (final Entry<Instance, Set<Instance>> entry : first.entrySet()) {
                        for (final Instance inst : entry.getValue()) {
                            final Map<String, Object> valueMap = new HashMap<>();
                            tmpValues.add(valueMap);
                            valueMap.put("fromInst1", entry.getKey());
                            valueMap.put("toInst1", inst);
                        }
                    }
                }

                int i = 1;
                while (iter.hasNext()) {
                    final Map<Instance, Set<Instance>> current = iter.next();
                    for (final Entry<Instance, Set<Instance>> entry : current.entrySet()) {
                        for (final Instance inst : entry.getValue()) {
                            for (final Map<String, ?> tmpMap : tmpValues) {
                                if (inst.equals(tmpMap.get("fromInst" + i))) {
                                    @SuppressWarnings("unchecked")
                                    final Map<String, Object> tmpMap2 = (Map<String, Object>) tmpMap;
                                    tmpMap2.put("fromInst" + (i + 1), entry.getKey());
                                    tmpMap2.put("toInst" + (i + 1), inst);
                                }
                            }
                        }
                    }
                    i++;
                }
                final List<Map<String, ?>> newValues = new ArrayList<>();
                final Iterator<Map<String, ?>> valIter = tmpValues.iterator();
                while (valIter.hasNext()) {
                    final Map<String, ?> tmpMap = valIter.next();
                    final Map<String, Object> newMap = new HashMap<>();
                    newValues.add(newMap);
                    newMap.put("docName1", ((Instance) tmpMap.get("toInst1")).getType().getLabel() + " - "
                                    + docValues.get((tmpMap.get("toInst1")))[0]);
                    newMap.put("docOID1", ((Instance) tmpMap.get("toInst1")).getOid());
                    newMap.put("docDate1", docValues.get((tmpMap.get("toInst1")))[1]);
                    newMap.put("docName2", ((Instance) tmpMap.get("fromInst1")).getType().getLabel() + " - "
                                    + docValues.get(tmpMap.get("fromInst1"))[0]);
                    newMap.put("docOID2", ((Instance) tmpMap.get("fromInst1")).getOid());
                    newMap.put("docDate2", docValues.get(tmpMap.get("fromInst1"))[1]);
                    int j = 3;
                    while (tmpMap.containsKey("fromInst" + (j - 1))) {
                        newMap.put("docName" + j, ((Instance) tmpMap.get("fromInst" + (j - 1))).getType().getLabel()
                                        + " - " + docValues.get((tmpMap.get("fromInst" + (j - 1))))[0]);
                        newMap.put("docOID" + j, ((Instance) tmpMap.get("fromInst" + (j - 1))).getOid());
                        newMap.put("docDate" + j, docValues.get((tmpMap.get("fromInst" + (j - 1))))[1]);
                        j++;
                    }
                }

                if (isTranspose(_parameter)) {
                    final List<Map<String, ?>> newValues2 = new ArrayList<>();
                    final Iterator<Map<String, ?>> valIter2 = newValues.iterator();
                    while (valIter2.hasNext()) {
                        final Map<String, ?> tmpMap = valIter2.next();
                        final Map<String, Object> newMap = new HashMap<>();
                        newValues2.add(newMap);
                        int max = 0;
                        while (tmpMap.containsKey("docOID" + (max + 1))) {
                            max++;
                        }

                        int k = max;
                        for (int j = 1; j < max + 1; j++) {
                            newMap.put("docOID" + k, tmpMap.get("docOID" + j));
                            newMap.put("docName" + k, tmpMap.get("docName" + j));
                            newMap.put("docDate" + k, tmpMap.get("docDate" + j));
                            k--;
                        }
                        valIter2.remove();
                    }
                    newValues.addAll(newValues2);
                }
                ret = new JRMapCollectionDataSource(newValues);
                getFilteredReport().cache(_parameter, ret);
            }
            return ret;
        }

        /**
         * Adds the 2 query builder.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _queryBldr the query bldr
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
                dateFrom = new DateTime().minusMonths(1);
            }
            final DateTime dateTo;
            if (filter.containsKey("dateTo")) {
                dateTo = (DateTime) filter.get("dateTo");
            } else {
                dateTo = new DateTime();
            }
            _queryBldr.addWhereAttrGreaterValue(CIERP.DocumentAbstract.Date, dateFrom
                            .withTimeAtStartOfDay().minusMinutes(1));
            _queryBldr.addWhereAttrLessValue(CIERP.DocumentAbstract.Date, dateTo.plusDays(1)
                            .withTimeAtStartOfDay());
        }

        /**
         * Gets the from map.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _toInsts the to insts
         * @return the from map
         * @throws EFapsException on error
         */
        protected Map<Instance, Set<Instance>> getFromMap(final Parameter _parameter,
                                                          final Collection<Instance> _toInsts)
            throws EFapsException
        {
            final Map<Instance, Set<Instance>> ret = new HashMap<>();
            final QueryBuilder queryBldr = new QueryBuilder(CISales.Document2Document4Swap);
            queryBldr.addWhereAttrEqValue(CISales.Document2Document4Swap.ToLink, _toInsts.toArray());
            final MultiPrintQuery multi = queryBldr.getPrint();

            final SelectBuilder selToInst = SelectBuilder.get().linkto(CISales.Document2Document4Swap.ToLink)
                            .instance();
            final SelectBuilder selFromInst = SelectBuilder.get().linkto(CISales.Document2Document4Swap.FromLink)
                            .instance();
            multi.addSelect(selToInst, selFromInst);
            multi.execute();
            while (multi.next()) {
                final Instance toInst = multi.getSelect(selToInst);
                final Instance fromInst = multi.getSelect(selFromInst);
                SwapReport_Base.LOG.debug("FromInst: {}", fromInst);
                SwapReport_Base.LOG.debug("ToInst: {}", toInst);
                final Set<Instance> toInsts;
                if (ret.containsKey(fromInst)) {
                    toInsts = ret.get(fromInst);
                } else {
                    toInsts = new HashSet<>();
                    ret.put(fromInst, toInsts);
                }
                toInsts.add(toInst);
            }
            return ret;
        }

        /**
         * Checks if is transpose.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return true, if is transpose
         * @throws EFapsException on error
         */
        protected boolean isTranspose(final Parameter _parameter)
            throws EFapsException
        {
            final Map<String, Object> filter = this.filteredReport.getFilterMap(_parameter);
            return BooleanUtils.isTrue((Boolean) filter.get("transpose"));
        }

        /**
         * Gets the query bldr.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the query bldr
         * @throws EFapsException on error
         */
        protected QueryBuilder getQueryBldr(final Parameter _parameter)
            throws EFapsException
        {
            final QueryBuilder ret = getQueryBldrFromProperties(_parameter, Sales.REPORT_SWAP.get());
            return ret;
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final JRMapCollectionDataSource ds = (JRMapCollectionDataSource) createDataSource(_parameter);
            int max = 1;
            for (final Map<String, ?> map : ds.getData()) {
                while (map.containsKey("docOID" + (max + 1))) {
                    max++;
                }
            }
            final List<ColumnGridComponentBuilder> titleGroups = new ArrayList<>();
            final List<ColumnBuilder<?, ?>> columns = new ArrayList<>();
            for (int i = 1; i < max + 1; i++) {

                final ColumnTitleGroupBuilder docTitleGroup = DynamicReports.grid.titleGroup(getFilteredReport()
                                .getDBProperty("TitleGroup.doc") + " " + i);
                titleGroups.add(docTitleGroup);

                if (ExportType.HTML.equals(getExType())) {
                    final ComponentColumnBuilder linkColumn = FilteredReport.getLinkColumn(_parameter, "docOID" + i);
                    columns.add(linkColumn);
                    docTitleGroup.add(linkColumn);
                }
                final TextColumnBuilder<String> docNameCol = DynamicReports.col.column(getFilteredReport()
                                .getDBProperty("Column.docName"), "docName" + i, DynamicReports.type
                                                .stringType()).setWidth(150);
                final TextColumnBuilder<DateTime> docDateCol = DynamicReports.col.column(getFilteredReport()
                                .getDBProperty("Column.docDate"), "docDate" + i, DateTimeDate.get());
                Collections.addAll(columns, docNameCol, docDateCol);
                docTitleGroup.add(docNameCol, docDateCol);
            }
            _builder.columnGrid(titleGroups.toArray(new ColumnGridComponentBuilder[titleGroups.size()]))
                .addColumn(columns.toArray(new ColumnBuilder[columns.size()]));
        }
    }
}
