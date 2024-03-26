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
package org.efaps.esjp.sales.listener;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.esjp.erp.listener.IOnCreateDocument;
import org.efaps.esjp.sales.CreditLine;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;

/**
 * The Class OnCreateDocument4CreditLine_Base.
 */
@EFapsUUID("0d6cda75-c916-46d3-a24d-0c5a4aa27217")
@EFapsApplication("eFapsApp-Sales")
public abstract class OnCreateDocument4CreditLine_Base
    implements IOnCreateDocument
{

    @Override
    public void afterCreate(final Parameter parameter,
                            final Instance docInstance)
        throws EFapsException
    {
        if (Sales.CREDITLINE_ACTIVATE.get()) {
            new CreditLine().updateCreditLine(parameter, docInstance);
        }
    }

    @Override
    public int getWeight()
    {
        return 0;
    }
}
