/*
 * Copyright 2003 - 2021 The eFaps Team
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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.products.reports.PriceListReport;
import org.efaps.esjp.sales.Calculator;
import org.efaps.esjp.sales.document.Receipt;
import org.efaps.util.EFapsException;

import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.ColumnBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;

@EFapsUUID("484d62d5-77c7-4f24-887b-4b28df469423")
@EFapsApplication("eFapsApp-Sales")
public class PriceListReportWithTax_Base
    extends PriceListReport
{

    @Override
    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new DynPriceListWithTaxReport(_parameter, this);
    }

    public static class DynPriceListWithTaxReport
        extends DynPriceListReport
    {

        public DynPriceListWithTaxReport(final Parameter _parameter,
                                         final PriceListReportWithTax_Base _report)
            throws EFapsException
        {
            super(_parameter, _report);
        }

        protected void add2Value(final Instance _productInst, final Map<String, Object> _map)
            throws EFapsException
        {
            final var parameter = ParameterUtil.instance();
            final var calcConfig = new Receipt();
            final var tempMap = new HashMap<String,Object>();
            for (final Entry<String, Object> entry : _map.entrySet()) {
                if (entry.getKey().startsWith("price-")) {
                    final var key = "calcPrice" + entry.getKey().substring(6);
                    final BigDecimal price = (BigDecimal) entry.getValue();
                    final var calc = new Calculator(parameter, (Calculator) null, _productInst, BigDecimal.ONE, price,
                                    BigDecimal.ZERO, false, calcConfig);
                    if (Calculator.priceIsNet(parameter, calcConfig)) {
                        tempMap.put(key, calc.getCrossUnitPrice());
                    } else {
                        tempMap.put(key, calc.getNetUnitPrice());
                    }
                }
            }
            _map.putAll(tempMap);
        }

        protected ColumnBuilder<?, ?>[] add2PriceColumnBldrs(final TextColumnBuilder<BigDecimal> price,
                                                             final TextColumnBuilder<String> currency)
        {
            final var fieldName = "calcPrice" + price.getName().substring(6);
            final TextColumnBuilder<BigDecimal> calcPrice = DynamicReports.col.column("", fieldName,
                            DynamicReports.type.bigDecimalType());
            return new ColumnBuilder[] { price, calcPrice, currency };
        }
    }
}
