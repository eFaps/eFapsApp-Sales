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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;

import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeDate;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.products.Cost;
import org.efaps.esjp.products.Cost_Base.CostBean;
import org.efaps.esjp.products.Inventory;
import org.efaps.esjp.products.Inventory_Base.InventoryBean;
import org.efaps.esjp.products.reports.InventoryReport;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("f874c441-e026-4904-9de4-28173a67c008")
@EFapsRevision("$Rev$")
public abstract class InventoryReport_Base
    extends org.efaps.esjp.products.reports.InventoryReport
{

    public enum Valuation
    {
        COST, REPLACEMENT;
    }

    /**
     * Logging instance used in this class.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(InventoryReport.class);

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return the report class
     * @throws EFapsException on error
     */
    @Override
    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new SalesDynInventoryReport(this);
    }

    /**
     * Report class.
     */
    public static class SalesDynInventoryReport
        extends DynInventoryReport
    {

        /**
         * @param _inventoryReport_Base
         */
        public SalesDynInventoryReport(final FilteredReport _inventoryReport)
        {
            super(_inventoryReport);
        }

        @Override
        protected Inventory getInventoryObject(final Parameter _parameter)
        {
            return new SalesInventory();
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            super.addColumnDefintion(_parameter, _builder);

            final TextColumnBuilder<String> docNameColumn = DynamicReports.col.column(DBProperties
                            .getProperty(InventoryReport.class.getName() + ".Column.docName"),
                            "docName", DynamicReports.type.stringType());

            final TextColumnBuilder<DateTime> docDateColumn = DynamicReports.col.column(DBProperties
                            .getProperty(InventoryReport.class.getName() + ".Column.docDate"),
                            "docDate", DateTimeDate.get());

            final TextColumnBuilder<DateTime> docCreatedColumn = DynamicReports.col.column(DBProperties
                            .getProperty(InventoryReport.class.getName() + ".Column.docCreated"),
                            "docCreated", DateTimeDate.get());

            final TextColumnBuilder<BigDecimal> costColumn = DynamicReports.col.column(DBProperties
                            .getProperty(InventoryReport.class.getName() + ".Column.cost"),
                            "cost", DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<BigDecimal> totalColumn = DynamicReports.col.column(DBProperties
                            .getProperty(InventoryReport.class.getName() + ".Column.total"),
                            "total", DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<String> currencyColumn = DynamicReports.col.column(DBProperties
                            .getProperty(InventoryReport.class.getName() + ".Column.currency"),
                            "currency", DynamicReports.type.stringType());

            if (!isEvaluateCost(_parameter)) {
                _builder.addColumn(docNameColumn, docDateColumn, docCreatedColumn,
                                costColumn, totalColumn, currencyColumn);
            }
        }

        @Override
        protected boolean isEvaluateCost(final Parameter _parameter)
            throws EFapsException
        {
            final Map<String, Object> filter = getFilterMap(_parameter);
            Valuation valuation;
            if (filter.containsKey("valuation")) {
                valuation = (Valuation) ((EnumFilterValue) filter.get("valuation")).getObject();
            } else {
                valuation = Valuation.COST;
            }
            return valuation.equals(Valuation.COST);
        }
    }

    public static class SalesInventory
        extends Inventory
    {

        @Override
        protected void addCost(final Parameter _parameter,
                               final List<InventoryBean> _beans,
                               final Set<Instance> _prodInsts)
            throws EFapsException
        {
            final Map<Instance, CostBean> costs = getCostObject(_parameter)
                            .getCosts(_parameter, _prodInsts.toArray(new Instance[_prodInsts.size()]));
            for (final InventoryBean bean : _beans) {
                bean.setCostBean(costs.get(bean.getProdInstance()));
            }
        }

        @Override
        protected Cost getCostObject(final Parameter _parameter)
        {
            final SalesCost ret = new SalesCost();
            ret.setEvaluateCost(isEvaluateCost());
            return ret;
        }

        @Override
        protected InventoryBean getBean(final Parameter _parameter)
        {
            return new SalesInventoryBean();
        }
    }

    public static class SalesInventoryBean
        extends InventoryBean
    {

        /**
         * Getter method for the instance variable {@link #docName}.
         *
         * @return value of instance variable {@link #docName}
         */
        public String getDocName()
        {
            String ret = "";
            if (getCostBean() != null && getCostBean() instanceof SalesCostBean) {
                ret = ((SalesCostBean) getCostBean()).getDocName();
            }
            return ret;
        }

        public DateTime getDocCreated()
        {
            DateTime ret = null;
            if (getCostBean() != null && getCostBean() instanceof SalesCostBean) {
                ret = ((SalesCostBean) getCostBean()).getCreated();
            }
            return ret;
        }

        public DateTime getDocDate()
        {
            DateTime ret = null;
            if (getCostBean() != null && getCostBean() instanceof SalesCostBean) {
                ret = ((SalesCostBean) getCostBean()).getDate();
            }
            return ret;
        }
    }

    public static class SalesCost
        extends Cost
    {

        private boolean evaluateCost;

        @Override
        public Map<Instance, CostBean> getCosts(final Parameter _parameter,
                                                final DateTime _date,
                                                final Instance... _prodInsts)
            throws EFapsException
        {
            final Map<Instance, CostBean> ret;
            if (isEvaluateCost()) {
                ret = super.getCosts(_parameter, _date, _prodInsts);
            } else {
                ret = new HashMap<>();
                for (final Instance prodInst : _prodInsts) {
                    final QueryBuilder posAttrQueryBldr = new QueryBuilder(CISales.IncomingInvoicePosition);
                    posAttrQueryBldr.addType(CISales.IncomingReceiptPosition);
                    posAttrQueryBldr.addWhereAttrEqValue(CISales.IncomingInvoicePosition.Product, prodInst);

                    final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.IncomingInvoice);
                    attrQueryBldr.addType(CISales.IncomingReceipt);
                    attrQueryBldr.addWhereAttrInQuery(CISales.DocumentAbstract.ID, posAttrQueryBldr.getAttributeQuery(
                                                    CISales.IncomingInvoicePosition.DocumentAbstractLink));
                    attrQueryBldr.addOrderByAttributeDesc(CISales.IncomingInvoice.Created);
                    attrQueryBldr.setLimit(1);

                    final QueryBuilder queryBldr = new QueryBuilder(CISales.IncomingInvoicePosition);
                    queryBldr.addType(CISales.IncomingReceiptPosition);
                    queryBldr.addWhereAttrEqValue(CISales.IncomingInvoicePosition.Product, prodInst);
                    queryBldr.addWhereAttrInQuery(CISales.IncomingInvoicePosition.DocumentAbstractLink,
                                    attrQueryBldr.getAttributeQuery(CISales.DocumentAbstract.ID));

                    final MultiPrintQuery multi = queryBldr.getPrint();
                    final SelectBuilder selCurInst = SelectBuilder.get()
                                    .linkto(CISales.IncomingInvoicePosition.CurrencyId).instance();
                    final SelectBuilder selDoc = SelectBuilder.get()
                                    .linkto(CISales.IncomingInvoicePosition.DocumentAbstractLink);
                    final SelectBuilder selDocName = new SelectBuilder(selDoc).attribute(CISales.DocumentAbstract.Name);
                    final SelectBuilder selDocDate = new SelectBuilder(selDoc).attribute(CISales.DocumentAbstract.Date);
                    final SelectBuilder selDocCreate = new SelectBuilder(selDoc)
                                    .attribute(CISales.DocumentAbstract.Created);
                    multi.addSelect(selCurInst, selDocName, selDocDate, selDocCreate);
                    multi.addAttribute(CISales.IncomingInvoicePosition.DiscountNetUnitPrice);
                    multi.execute();
                    if (multi.next()) {
                        final Instance curInst = multi.getSelect(selCurInst);
                        final BigDecimal cost = multi
                                        .getAttribute(CISales.IncomingInvoicePosition.DiscountNetUnitPrice);
                        final SalesCostBean bean = new SalesCostBean();
                        bean.setProductInstance(prodInst);
                        bean.setCurrencyInstance(curInst);
                        bean.setCost(cost);
                        bean.setDocName(multi.<String>getSelect(selDocName));
                        bean.setDate(multi.<DateTime>getSelect(selDocDate));
                        bean.setCreated(multi.<DateTime>getSelect(selDocCreate));
                        ret.put(prodInst, bean);
                    }
                }
            }
            return ret;
        }

        /**
         * Getter method for the instance variable {@link #evaluateCost}.
         *
         * @return value of instance variable {@link #evaluateCost}
         */
        public boolean isEvaluateCost()
        {
            return this.evaluateCost;
        }

        /**
         * Setter method for instance variable {@link #evaluateCost}.
         *
         * @param _evaluateCost value for instance variable
         *            {@link #evaluateCost}
         */
        public void setEvaluateCost(final boolean _evaluateCost)
        {
            this.evaluateCost = _evaluateCost;
        }
    }

    public static class SalesCostBean
        extends CostBean
    {

        private String docName;

        private DateTime date;

        private DateTime created;

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
         * Getter method for the instance variable {@link #created}.
         *
         * @return value of instance variable {@link #created}
         */
        public DateTime getCreated()
        {
            return this.created;
        }

        /**
         * Setter method for instance variable {@link #created}.
         *
         * @param _created value for instance variable {@link #created}
         */
        public void setCreated(final DateTime _created)
        {
            this.created = _created;
        }
    }

}