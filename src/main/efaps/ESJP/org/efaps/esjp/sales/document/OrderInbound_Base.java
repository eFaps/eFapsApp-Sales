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

import java.util.ArrayList;
import java.util.List;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.SearchQuery;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.sales.Calculator;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("77aebe5f-aebd-4662-b503-efdc6e46d9d3")
@EFapsRevision("$Rev$")
public abstract class OrderInbound_Base
    extends DocumentSum
{
    public Return create(final Parameter _parameter) throws EFapsException
    {
        final String date = _parameter.getParameterValue("date");
        final List<Calculator> calcList = analyseTable(_parameter, null);
        final Long contactid = Instance.get(_parameter.getParameterValue("contact")).getId();
        final Insert insert = new Insert(CISales.OrderInbound);
        insert.add(CISales.OrderInbound.Contact, contactid.toString());
        insert.add(CISales.OrderInbound.CrossTotal, getCrossTotalStr(calcList));
        insert.add(CISales.OrderInbound.NetTotal, getNetTotalStr(calcList));
        insert.add(CISales.OrderInbound.Date, date);
        insert.add(CISales.OrderInbound.Salesperson, _parameter.getParameterValue("salesperson"));
        insert.add(CISales.OrderInbound.Status, ((Long) Status.find(CISales.OrderInboundStatus.uuid, "Open").getId()).toString());
        insert.add(CISales.OrderInbound.Name, _parameter.getParameterValue("name4create"));
        insert.execute();
        Integer i = 0;
        for (final Calculator calc : calcList) {
            final Insert posIns = new Insert("Sales_QuotationPosition");
            final Long productdId = Instance.get(_parameter.getParameterValues("product")[i]).getId();
            posIns.add(CISales.OrderInboundPosition.Order, insert.getId());
            posIns.add(CISales.OrderInboundPosition.PositionNumber, i.toString());
            posIns.add(CISales.OrderInboundPosition.Product, productdId.toString());
            posIns.add(CISales.OrderInboundPosition.ProductDesc, _parameter.getParameterValues("productAutoComplete")[i]);
            posIns.add(CISales.OrderInboundPosition.Quantity, calc.getQuantityStr());
            posIns.add(CISales.OrderInboundPosition.UoM, _parameter.getParameterValues("uom")[i]);
            posIns.add(CISales.OrderInboundPosition.CrossUnitPrice, calc.getCrossUnitPriceStr());
            posIns.add(CISales.OrderInboundPosition.NetUnitPrice, calc.getNetUnitPriceStr());
            posIns.add(CISales.OrderInboundPosition.CrossPrice, calc.getCrossPriceStr());
            posIns.add(CISales.OrderInboundPosition.NetPrice, calc.getNetPriceStr());
            posIns.add(CISales.OrderInboundPosition.Tax, (calc.getTaxId()).toString());
            posIns.add(CISales.OrderInboundPosition.Discount, calc.getDiscountStr());
            posIns.execute();
            i++;
        }
        return new Return();
    }

    /**
     * Method to get the list of instances for a table.
     * @param _parameter parameter as passed from the efaps api
     * @return list of instances
     * @throws EFapsException on error
     */
    public Return getProductTable(final Parameter _parameter) throws EFapsException
    {
        final Return ret = new Return();
        final List<Instance> instances = new ArrayList<Instance>();

        final SearchQuery query = new SearchQuery();
        query.setQueryTypes("Sales_OrderInbound");
        query.addWhereExprEqValue("Status", Status.find("Sales_OrderInboundStatus", "Open").getId());
        query.addSelect("OID");
        query.execute();
        while (query.next()) {
            final SearchQuery expand = new SearchQuery();
            expand.setExpand((String) query.get("OID"), "Sales_OrderInboundPosition\\Order");
            expand.addSelect("OID");
            expand.execute();
            while (expand.next()) {
                  instances.add(Instance.get((String) expand.get("OID")));
            }
        }
        ret.put(ReturnValues.VALUES, instances);
        return ret;
    }
}
