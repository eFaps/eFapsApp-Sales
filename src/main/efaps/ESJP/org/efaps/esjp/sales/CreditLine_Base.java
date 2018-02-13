/*
 * Copyright 2003 - 2018 The eFaps Team
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

package org.efaps.esjp.sales;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.esjp.erp.Naming;
import org.efaps.util.EFapsException;

@EFapsUUID("bc0668dd-36b1-4762-949a-be0b732008d1")
@EFapsApplication("eFapsApp-Sales")
public class CreditLine_Base
{

    /**
     * Creates a new CreditLine.
     *
     * @param _parameter the parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        return new Create() {
            @Override
            protected void add2basicInsert(final Parameter _parameter, final Insert _insert)
                throws EFapsException
            {
                super.add2basicInsert(_parameter, _insert);
                _insert.add(CISales.CreditLine.Name, new Naming().fromNumberGenerator(_parameter, null));
            }
        }.execute(_parameter);
    }
}
