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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.dbproperty.DBProperties;
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
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport_Base.ExportType;
import org.efaps.util.EFapsException;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.base.expression.AbstractSimpleExpression;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.ComponentColumnBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.component.HorizontalListBuilder;
import net.sf.dynamicreports.report.builder.component.SubreportBuilder;
import net.sf.dynamicreports.report.builder.component.TextFieldBuilder;
import net.sf.dynamicreports.report.builder.style.StyleBuilder;
import net.sf.dynamicreports.report.constant.PageOrientation;
import net.sf.dynamicreports.report.constant.StretchType;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 *
 */
@EFapsUUID("3dd895fe-c283-487b-9a27-e22aa81b7c67")
@EFapsApplication("eFapsApp-Sales")
public abstract class ConciliationReport_Base
{

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
     * @return the report class
     * @throws EFapsException on error
     */
    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new ConcilReport();
    }

    public static class ConcilReport
        extends AbstractDynamicReport
    {

        @Override
        protected StyleBuilder getColumnStyle4Pdf(final Parameter _parameter)
            throws EFapsException
        {
            return super.getColumnStyle4Pdf(_parameter).setTopBorder(DynamicReports.stl.pen1Point())
                            .setBottomBorder(DynamicReports.stl.pen1Point());
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final List<DataBean> datasource = new ArrayList<>();

            final Map<Instance, DataBean> values = new HashMap<>();
            _parameter.getCallInstance();

            final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.ConciliationPosition);
            attrQueryBldr.addWhereAttrEqValue(CISales.ConciliationPosition.ConciliationLink, _parameter.getInstance());
            final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.ConciliationPosition.ID);

            final QueryBuilder queryBldr = new QueryBuilder(CISales.PaymentDocumentIOAbstract);
            queryBldr.addWhereAttrInQuery(CISales.PaymentDocumentIOAbstract.ConciliationPositionLink, attrQuery);

            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selPos = SelectBuilder.get()
                            .linkto(CISales.PaymentDocumentIOAbstract.ConciliationPositionLink);
            final SelectBuilder selPosInst = new SelectBuilder(selPos).instance();
            final SelectBuilder selPosNumber = new SelectBuilder(selPos)
                            .attribute(CISales.ConciliationPosition.PositionNumber);
            final SelectBuilder selPosAmount = new SelectBuilder(selPos).attribute(CISales.ConciliationPosition.Amount);
            multi.addSelect(selPosInst, selPosAmount, selPosNumber);
            multi.addAttribute(CISales.PaymentDocumentIOAbstract.Amount, CISales.PaymentDocumentIOAbstract.Name);
            multi.execute();
            while (multi.next()) {
                final BigDecimal amount = multi.<BigDecimal>getAttribute(CISales.PaymentDocumentIOAbstract.Amount);
                final Instance posInst = multi.<Instance>getSelect(selPosInst);
                final String name = multi.<String>getAttribute(CISales.PaymentDocumentIOAbstract.Name);
                final Integer posNumber = multi.<Integer>getSelect(selPosNumber);
                final BigDecimal posAmount = multi.<BigDecimal>getSelect(selPosAmount);

                DataBean data;
                if (values.containsKey(posInst)) {
                    data = values.get(posInst);
                } else {
                    data = getDataBean(_parameter);
                    values.put(posInst, data);
                }
                data.setPosition(posNumber);
                data.setAmount(posAmount);
                final Map<String, Object> map = new HashMap<>();
                map.put("payAmount", amount);
                map.put("payType", multi.getCurrentInstance().getType().getLabel());
                map.put("payName", name);
                data.getPositions().add(map);
            }
            datasource.addAll(values.values());
            Collections.sort(datasource, new Comparator<DataBean>() {

                @Override
                public int compare(final DataBean _o1,
                                   final DataBean _o2)
                {
                    return _o1.getPosition().compareTo(_o2.getPosition());
                }
            });
            return new JRBeanCollectionDataSource(datasource);
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {

            final TextColumnBuilder<Integer> positionColumn = DynamicReports.col.column("position",
                            DynamicReports.type.integerType());

            final TextColumnBuilder<BigDecimal> amountColumn = DynamicReports.col.column("amount",
                            DynamicReports.type.bigDecimalType());

            final SubreportBuilder subreport = DynamicReports.cmp
                            .subreport(getSubreportDesign(_parameter, getExType()))
                            .setDataSource(getSubreportDataSource(_parameter))
                            .setStretchType(StretchType.RELATIVE_TO_BAND_HEIGHT)
                            .setStyle(DynamicReports.stl.style().setBorder(DynamicReports.stl.pen1Point()));

            final ComponentColumnBuilder posColumn = DynamicReports.col.componentColumn(subreport);

            final TextFieldBuilder<String> typeTitle = getTitle(_parameter, "positionNumber");
            final TextFieldBuilder<String> nameTitle = getTitle(_parameter, "position");

            final HorizontalListBuilder header = DynamicReports.cmp.horizontalList();
            header.add(typeTitle).add(nameTitle);

            if (ExportType.PDF.equals(getExType())) {
                typeTitle.setWidth(3);
                nameTitle.setWidth(8);
                posColumn.setWidth(54);
            } else if (ExportType.EXCEL.equals(getExType())) {
                _builder.setPageFormat(1000, 400, PageOrientation.LANDSCAPE);
                typeTitle.setFixedWidth(30);
                nameTitle.setFixedWidth(60);
            } else {
                typeTitle.setWidth(3);
                nameTitle.setWidth(8);
                posColumn.setWidth(300);
            }

            _builder.fields(DynamicReports.field("positions", List.class))
                            .addColumn(positionColumn, amountColumn, posColumn)
                            .addColumnHeader(header);
        }

        protected SubreportDataSource getSubreportDataSource(final Parameter _parameter)
        {
            return new SubreportDataSource();
        }

        protected SubreportDesign getSubreportDesign(final Parameter _parameter,
                                                     final ExportType _exType)
        {
            return new SubreportDesign(_exType);
        }

        protected TextFieldBuilder<String> getTitle(final Parameter _parameter,
                                                    final String _key)
        {
            return DynamicReports.cmp.text(DBProperties.getProperty(ConciliationReport.class.getName() + "." + _key))
                            .setStyle(DynamicReports.stl.style().setBold(true));
        }

        protected DataBean getDataBean(final Parameter _parameter)
        {
            return new DataBean();
        }

    }

    public static class SubreportDesign
        extends AbstractSimpleExpression<JasperReportBuilder>
    {

        private static final long serialVersionUID = 1L;
        private final ExportType exType;

        public SubreportDesign(final ExportType _exType)
        {
            this.exType = _exType;
        }

        @Override
        public JasperReportBuilder evaluate(final ReportParameters _reportParameters)
        {
            final TextColumnBuilder<String> payType = DynamicReports.col.column("payType",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> payName = DynamicReports.col.column("payName",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<BigDecimal> payAmount = DynamicReports.col.column("payAmount",
                            DynamicReports.type.bigDecimalType());

            final JasperReportBuilder report = DynamicReports.report();

            if (ExportType.PDF.equals(this.exType)) {
                report.setColumnStyle(DynamicReports.stl.style().setPadding(DynamicReports.stl.padding(2))
                                .setLeftBorder(DynamicReports.stl.pen1Point())
                                .setRightBorder(DynamicReports.stl.pen1Point())
                                .setBottomBorder(DynamicReports.stl.pen1Point())
                                .setTopBorder(DynamicReports.stl.pen1Point()));
                payType.setWidth(17);
                payName.setWidth(49);

            } else if (ExportType.EXCEL.equals(this.exType)) {
                payType.setFixedWidth(60);
                payName.setFixedWidth(180);
                payAmount.setFixedWidth(60);
            }
            report.columns(payType, payName, payAmount);
            return report;
        }
    }

    public static class SubreportDataSource
        extends AbstractSimpleExpression<JRDataSource>
    {

        private static final long serialVersionUID = 1L;

        @Override
        public JRDataSource evaluate(final ReportParameters reportParameters)
        {
            final Collection<Map<String, ?>> value = reportParameters.getValue("positions");
            return new JRMapCollectionDataSource(value);
        }
    }

    public static class DataBean
    {
        private Integer position;
        private BigDecimal amount;
        private final Collection<Map<String, Object>> positions = new ArrayList<>();

        /**
         * Getter method for the instance variable {@link #position}.
         *
         * @return value of instance variable {@link #position}
         */
        public Integer getPosition()
        {
            return this.position;
        }

        /**
         * Setter method for instance variable {@link #position}.
         *
         * @param _position value for instance variable {@link #position}
         */
        public void setPosition(final Integer _position)
        {
            this.position = _position;
        }

        /**
         * Getter method for the instance variable {@link #amount}.
         *
         * @return value of instance variable {@link #amount}
         */
        public BigDecimal getAmount()
        {
            return this.amount;
        }

        /**
         * Setter method for instance variable {@link #amount}.
         *
         * @param _amount value for instance variable {@link #amount}
         */
        public void setAmount(final BigDecimal _amount)
        {
            this.amount = _amount;
        }


        /**
         * Getter method for the instance variable {@link #positions}.
         *
         * @return value of instance variable {@link #positions}
         */
        public Collection<Map<String, Object>> getPositions()
        {
            return this.positions;
        }

    }
}
