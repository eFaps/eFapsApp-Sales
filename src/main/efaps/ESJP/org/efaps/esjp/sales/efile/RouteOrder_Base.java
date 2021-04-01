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

package org.efaps.esjp.sales.efile;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.StandartReport;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.esjp.common.uiform.Edit;
import org.efaps.esjp.common.uisearch.Connect;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.esjp.erp.Naming;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;

@EFapsUUID("690dc0b0-c81a-422b-9e9d-6f2d3822821e")
@EFapsApplication("eFapsApp-Sales")
public abstract class RouteOrder_Base
    extends CommonDocument
{

    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final var ret =  new Create()
        {
            @Override
            protected void add2basicInsert(final Parameter _parameter, final Insert _insert)
                throws EFapsException
            {
                super.add2basicInsert(_parameter, _insert);
                if (Sales.ROUTEORDER_NUMGEN.exists()) {
                    _insert.add(CISales.RouteOrder.Name, new Naming().fromNumberGenerator(_parameter, null));
                }
                _insert.add(CISales.RouteOrder.Status,
                                Status.find(CISales.RouteOrderStatus, Sales.ROUTEORDER_STATUS4CREATE.get()));
            }
        }.execute(_parameter);
        if (Sales.ROUTEORDER_JASPERREPORT.exists()) {
            final var inst = (Instance) ret.get(ReturnValues.INSTANCE);
            final var clonedParameter = ParameterUtil.clone(_parameter, ParameterValues.INSTANCE, inst);
            createReport(clonedParameter);
        }
        return ret;
    }

    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final var ret = new Edit().execute(_parameter);
        createReport(_parameter);
        return ret;
    }

    public Return connect(final Parameter _parameter)
        throws EFapsException
    {
        final var ret = new Connect().execute(_parameter);
        createReport(_parameter);
        return ret;
    }

    @Override
    public Return createReport(final Parameter _parameter)
        throws EFapsException
    {
        if (Sales.ROUTEORDER_JASPERREPORT.exists()) {
            ParameterUtil.setProperty(_parameter, "JasperReport", Sales.ROUTEORDER_JASPERREPORT.get());
            ParameterUtil.setProperty(_parameter, "Checkin", "true");
            super.createReport(_parameter);
        }
        return null;
    }

    @Override
    protected void add2Report(final Parameter _parameter,
                              final CreatedDoc _createdDoc,
                              final StandartReport _report)
        throws EFapsException
    {
        super.add2Report(_parameter, _createdDoc, _report);
        final var eval = EQL.builder()
                        .print(_parameter.getInstance())
                        .attribute(CISales.RouteOrder.Name)
                        .evaluate();
        final var name = eval.get(CISales.RouteOrder.Name);
        final String fileName = DBProperties.getProperty(_parameter.getInstance().getType().getLabelKey(), "es")
                        + "_" + name;
        _report.setFileName(fileName);
    }
}
