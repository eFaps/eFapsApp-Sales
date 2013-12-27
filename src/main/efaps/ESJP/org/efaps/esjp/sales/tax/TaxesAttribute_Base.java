/*
 * Copyright 2003 - 2013 The eFaps Team
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

package org.efaps.esjp.sales.tax;

import org.efaps.admin.datamodel.IJaxb;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.tax.xml.TaxEntry;
import org.efaps.esjp.sales.tax.xml.Taxes;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("da82900f-88f7-43c9-a331-b894f3d337e3")
@EFapsRevision("$Rev$")
public abstract class TaxesAttribute_Base
    implements IJaxb
{

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?>[] getClasses()
    {
        return new Class[] { Taxes.class, TaxEntry.class };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUIValue(final Object _object)
        throws EFapsException
    {
        String ret;
        if (_object == null) {
            ret = "";
        } else if (_object instanceof Taxes) {
            final Taxes taxes = (Taxes) _object;
            final StringBuilder html = new StringBuilder();
            for (final TaxEntry entry : taxes.getEntries()) {
                final Tax tax = Tax_Base.get(entry.getCatUUID(), entry.getUUID());
                html.append(tax.getName()).append(" ")
                                .append(NumberFormatter.get().getTwoDigitsFormatter().format(entry.getAmount()));
            }
            ret = html.toString();
        } else {
            ret = _object.toString();
        }
        return ret;
    }
}
