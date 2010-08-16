/*
 * Copyright 2003 - 2009 The eFaps Team
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

package org.efaps.esjp.sales.document;

import java.util.List;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.esjp.sales.Calculator;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("d2d083ad-d0e2-43de-a35d-f42dbdfcf7e4")
@EFapsRevision("$Rev$")
public abstract class PartialInvoice_Base
    extends DocumentSum
{
    public Return create(final Parameter _parameter) throws EFapsException
    {
        final String date = _parameter.getParameterValue("date");
        final List<Calculator> calcList = analyseTable(_parameter, null);
        final String netTotal = getNetTotalStr(calcList);
        final String crossTotal = getCrossTotalStr(calcList);
        final Long contactid = Instance.get(_parameter.getParameterValue("contact")).getId();
        final Insert insert = new Insert("Sales_PartialInvoice");
        insert.add("Contact", contactid.toString());
        insert.add("CrossTotal", crossTotal);
        insert.add("NetTotal", netTotal);
        insert.add("Date", date);
        insert.add("Salesperson", _parameter.getParameterValue("salesperson"));
        insert.add("Name", _parameter.getParameterValue("name4create"));
        insert.execute();
        Integer i = 0;
        for (final Calculator calc : calcList) {
            final Insert posIns = new Insert("Sales_PartialInvoicePosition");
            final Long productdId = Instance.get(_parameter.getParameterValues("product")[i]).getId();
            posIns.add("PartialInvoice", insert.getId());
            posIns.add("PositionNumber", i.toString());
            posIns.add("Product", productdId.toString());
            posIns.add("ProductDesc", _parameter.getParameterValues("productdesc")[i]);
            posIns.add("Quantity", calc.getQuantityStr());
            posIns.add("UoM",  _parameter.getParameterValues("uom")[i]);
            posIns.add("CrossUnitPrice", calc.getCrossUnitPriceStr());
            posIns.add("NetUnitPrice", calc.getNetUnitPriceStr());
            posIns.add("CrossPrice", calc.getCrossPriceStr());
            posIns.add("NetPrice", calc.getNetPriceStr());
            posIns.add("Tax", (calc.getTaxId()).toString());
            posIns.add("Discount", calc.getDiscountStr());
            posIns.execute();
            i++;
        }
        return new Return();
    }
}
