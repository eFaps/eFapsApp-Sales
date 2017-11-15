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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
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
import net.sf.dynamicreports.report.definition.ReportParameters;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("f874c441-e026-4904-9de4-28173a67c008")
@EFapsApplication("eFapsApp-Sales")
public abstract class InventoryReport_Base
    extends org.efaps.esjp.products.reports.InventoryReport
{

    /**
     * The Enum Valuation.
     */
    public enum Valuation
    {

        /** The cost. */
        COST,

        /** The replacement. */
        REPLACEMENT;
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
         * Instantiates a new sales dyn inventory report.
         *
         * @param _inventoryReport the _inventory report
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
        protected void addColumnDefinition(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            super.addColumnDefinition(_parameter, _builder);

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
                if (getExType().equals(ExportType.HTML)) {
                    final GenericElementBuilder linkElement = DynamicReports.cmp.genericElement(
                                    "http://www.efaps.org", "efapslink")
                                    .addParameter(EmbeddedLink.JASPER_PARAMETERKEY, new DocLinkExpression())
                                    .setHeight(12).setWidth(25);
                    final ComponentColumnBuilder linkColumn = DynamicReports.col.componentColumn(linkElement)
                                    .setTitle("");
                    _builder.addColumn(linkColumn);
                }

                _builder.addColumn(docNameColumn, docDateColumn, docCreatedColumn,
                                costColumn, totalColumn, currencyColumn);
            }
        }

        /**
         * Checks if is evaluate cost.
         *
         * @param _parameter the _parameter
         * @return true, if is evaluate cost
         * @throws EFapsException the e faps exception
         */
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

    /**
     * The Class SalesInventory.
     */
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
                bean.setCostBean(_parameter, costs.get(bean.getProdInstance()), null);
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

    /**
     * The Class SalesInventoryBean.
     */
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

        /**
         * Gets the doc created.
         *
         * @return the doc created
         */
        public DateTime getDocCreated()
        {
            DateTime ret = null;
            if (getCostBean() != null && getCostBean() instanceof SalesCostBean) {
                ret = ((SalesCostBean) getCostBean()).getDocCreated();
            }
            return ret;
        }

        /**
         * Gets the doc date.
         *
         * @return the doc date
         */
        public DateTime getDocDate()
        {
            DateTime ret = null;
            if (getCostBean() != null && getCostBean() instanceof SalesCostBean) {
                ret = ((SalesCostBean) getCostBean()).getDate();
            }
            return ret;
        }

        /**
         * Gets the doc oid.
         *
         * @return the doc oid
         */
        public String getDocOID()
        {
            String ret = null;
            if (getCostBean() != null && getCostBean() instanceof SalesCostBean) {
                final Instance inst = ((SalesCostBean) getCostBean()).getDocInst();
                ret = inst != null && inst.isValid() ? inst.getOid() : null;
            }
            return ret;
        }

    }

    /**
     * The Class SalesCost.
     */
    public static class SalesCost
        extends Cost
    {

        /** The evaluate cost. */
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

                final QueryBuilder queryBldr = new QueryBuilder(CISales.IncomingInvoicePosition);
                queryBldr.addType(CISales.IncomingReceiptPosition);
                queryBldr.addWhereAttrEqValue(CISales.IncomingInvoicePosition.Product, (Object[]) _prodInsts);

                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder selCurInst = SelectBuilder.get()
                                .linkto(CISales.IncomingInvoicePosition.CurrencyId).instance();
                final SelectBuilder selDoc = SelectBuilder.get()
                                .linkto(CISales.IncomingInvoicePosition.DocumentAbstractLink);
                final SelectBuilder selDocInst = new SelectBuilder(selDoc).instance();
                final SelectBuilder selDocName = new SelectBuilder(selDoc).attribute(CISales.DocumentAbstract.Name);
                final SelectBuilder selDocDate = new SelectBuilder(selDoc).attribute(CISales.DocumentAbstract.Date);
                final SelectBuilder selDocCreate = new SelectBuilder(selDoc)
                                .attribute(CISales.DocumentAbstract.Created);
                final SelectBuilder selProdInst = SelectBuilder.get().linkto(CISales.IncomingInvoicePosition.Product)
                                .instance();
                multi.addSelect(selCurInst, selDocInst, selDocName, selDocDate, selDocCreate, selProdInst);
                multi.addAttribute(CISales.IncomingInvoicePosition.DiscountNetUnitPrice);
                multi.execute();
                final List<SalesCostBean> beans = new ArrayList<>();
                while (multi.next()) {
                    final Instance curInst = multi.getSelect(selCurInst);
                    final BigDecimal cost = multi
                                    .getAttribute(CISales.IncomingInvoicePosition.DiscountNetUnitPrice);
                    final SalesCostBean bean = new SalesCostBean();
                    bean.setProductInstance(multi.<Instance>getSelect(selProdInst));
                    bean.setCurrencyInstance(curInst);
                    bean.setCost(cost);
                    bean.setDocInst(multi.<Instance>getSelect(selDocInst));
                    bean.setDocName(multi.<String>getSelect(selDocName));
                    bean.setDate(multi.<DateTime>getSelect(selDocDate));
                    bean.setDocCreated(multi.<DateTime>getSelect(selDocCreate));
                    beans.add(bean);
                }
                Collections.sort(beans, new Comparator<SalesCostBean>()
                {

                    @Override
                    public int compare(final SalesCostBean _arg0,
                                       final SalesCostBean _arg1)
                    {
                        return _arg1.getDate().compareTo(_arg0.getDate());
                    }
                });
                for (final SalesCostBean bean : beans) {
                    if (!ret.containsKey(bean.getProductInstance())) {
                        ret.put(bean.getProductInstance(), bean);
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

    /**
     * The Class SalesCostBean.
     */
    public static class SalesCostBean
        extends CostBean
    {
        /** The doc inst. */
        private Instance docInst;

        /** The doc name. */
        private String docName;

        /** The doc created. */
        private DateTime docCreated;

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
         * Getter method for the instance variable {@link #created}.
         *
         * @return value of instance variable {@link #created}
         */
        public DateTime getDocCreated()
        {
            return this.docCreated;
        }

        /**
         * Setter method for instance variable {@link #created}.
         *
         * @param _created value for instance variable {@link #created}
         */
        public void setDocCreated(final DateTime _created)
        {
            this.docCreated = _created;
        }

        /**
         * Getter method for the instance variable {@link #docInst}.
         *
         * @return value of instance variable {@link #docInst}
         */
        public Instance getDocInst()
        {
            return this.docInst;
        }

        /**
         * Setter method for instance variable {@link #docInst}.
         *
         * @param _docInst value for instance variable {@link #docInst}
         */
        public void setDocInst(final Instance _docInst)
        {
            this.docInst = _docInst;
        }
    }

    /**
     * Expression used to render a link for the UserInterface.
     */
    public static class DocLinkExpression
        extends AbstractComplexExpression<EmbeddedLink>
    {

        /**
         * Needed for serialization.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Costructor.
         */
        public DocLinkExpression()
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
}
