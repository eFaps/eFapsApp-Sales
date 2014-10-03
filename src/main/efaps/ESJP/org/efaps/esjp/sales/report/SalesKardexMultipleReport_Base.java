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

import java.util.Map;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperReport;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: SalesKardexReport_Base.java 12300 2014-03-25 16:25:14Z
 *          m.aranya@moxter.net $
 */
@EFapsUUID("6678c3f1-8076-4e04-8ef5-02d0924aefc0")
@EFapsRevision("$Rev$")
public abstract class SalesKardexMultipleReport_Base
    extends SalesKardexReport
{
    @Override
    public void init(final JasperReport _jasperReport,
                     final Parameter _parameter,
                     final JRDataSource _parentSource,
                     final Map<String, Object> _jrParameters)
        throws EFapsException
    {
        ParameterUtil.setParmeterValue(_parameter, CIFormSales.Sales_Products_Kardex_OfficialReportForm.product.name, "4159.13779");
        super.init(_jasperReport, _parameter, _parentSource, _jrParameters);
    }
}
