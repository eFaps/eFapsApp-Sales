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
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.esjp.erp.Naming;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;

@EFapsUUID("690dc0b0-c81a-422b-9e9d-6f2d3822821e")
@EFapsApplication("eFapsApp-Sales")
public abstract class RouteOrder_Base
{

    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        return new Create()
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
    }
}
