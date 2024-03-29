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
package org.efaps.esjp.sales.tax.select;

import java.math.BigDecimal;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIAttribute;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.sales.tax.xml.TaxEntry;

@EFapsUUID("a2791900-2283-4b54-bb30-b8533dc72e37")
@EFapsApplication("eFapsApp-Sales")
public abstract class TaxSelect_Base
    extends AbstractTaxSelect
{
    @Override
    protected CIAttribute getAttribute()
    {
        return CISales.DocumentSumAbstract.Taxes;
    }

    @Override
    protected BigDecimal getValue(final TaxEntry _taxEntry)
    {
        return _taxEntry.getAmount();
    }
}
