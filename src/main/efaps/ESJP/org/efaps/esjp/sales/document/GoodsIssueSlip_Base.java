/*
 * Copyright 2003 - 2010 The eFaps Team
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Map.Entry;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.SearchQuery;
import org.efaps.esjp.sales.Sales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("bc9f7db3-8a53-4b57-951e-da1fbc2c7307")
@EFapsRevision("$Rev$")
public abstract class GoodsIssueSlip_Base
    extends AbstractDocument
{
    public Return create(final Parameter _parameter) throws EFapsException
    {
        final String date = _parameter.getParameterValue("date");
        final Long contactid = Instance.get(_parameter.getParameterValue("contact")).getId();
        final Insert insert = new Insert(Sales.GOODSISSUESLIP.getUuid());
        insert.add("Contact", contactid.toString());
        insert.add("Date", date);
        insert.add("Salesperson", _parameter.getParameterValue("salesperson"));
        insert.add("Name", _parameter.getParameterValue("name4create"));
        insert.add("Status", ((Long) Status.find(Type.get(Sales.GOODSISSUESLIP_STATUS.getUuid()).getName(),
                        "Closed").getId()).toString());
        insert.execute();
        Integer i = 0;
        for (final String quantity :  _parameter.getParameterValues("quantity")) {
            final Insert posIns = new Insert(Sales.GOODSISSUESLIP_POS.getUuid());
            final Long productdId = Instance.get(_parameter.getParameterValues("product")[i]).getId();
            posIns.add("GoodsIssueSlip", insert.getId());
            posIns.add("PositionNumber", i.toString());
            posIns.add("Product", productdId.toString());
            posIns.add("ProductDesc", _parameter.getParameterValues("productDesc")[i]);
            posIns.add("Quantity", (new BigDecimal(quantity)).toString());
            posIns.add("UoM",  _parameter.getParameterValues("uoM")[i]);
            posIns.add("CrossUnitPrice", "0");
            posIns.add("NetUnitPrice", "0");
            posIns.add("CrossPrice", "0");
            posIns.add("NetPrice", "0");
            posIns.add("Discount", "0");
            posIns.add("Tax", "1");
            posIns.add("DiscountNetUnitPrice", BigDecimal.ZERO);
            posIns.add("CurrencyId", 1);
            posIns.add("RateCurrencyId", 1);
            posIns.add("Rate", BigDecimal.ZERO);
            posIns.execute();
            i++;
        }
        return new Return();
    }



    public Return goodsIssueSlipPositionInsertTrigger(final Parameter _parameter) throws EFapsException
    {
        final Map<String, String[]> param = Context.getThreadContext().getParameters();
        final String[] productOids = param.get("product");
        final String[] storageIds = param.get("storage");

        final Instance instance = _parameter.getInstance();
        final Map<?, ?> map = (Map<?, ?>) _parameter.get(ParameterValues.NEW_VALUES);

        final Object[] productID = (Object[]) map.get(instance.getType().getAttribute("Product"));
        final Object[] qauntity = (Object[]) map.get(instance.getType().getAttribute("Quantity"));
        final Object[] deliveryNodeId = (Object[]) map.get(instance.getType().getAttribute("GoodsIssueSlip"));
        final Object[] uom = (Object[]) map.get(instance.getType().getAttribute("UoM"));
        String storage = null;
        if (storageIds != null) {
            for (int i = 0; i <  productOids.length; i++) {
                if (productID[0].equals(((Long) Instance.get(productOids[i]).getId()).toString())) {
                    storage = storageIds[i];
                }
            }
        } else {
            final SearchQuery query = new SearchQuery();
            query.setQueryTypes("Products_Inventory");
            query.addWhereExprEqValue("Product", productID[0].toString());
            query.addSelect("Storage");
            query.execute();
            if (query.next()) {
                storage = ((Long) query.get("Storage")).toString();
            }
        }

        final Insert insert = new Insert("Products_TransactionOutbound");
        insert.add("Quantity", qauntity[0]);
        insert.add("Storage", storage.toString());
        insert.add("Product", productID[0]);
        insert.add("Description",
                   DBProperties.getProperty("org.efaps.esjp.sales.document.GoodsIssueSlip.description4Trigger"));
        insert.add("Date", new DateTime());
        insert.add("Document", deliveryNodeId[0]);
        insert.add("UoM", uom[0]);
        insert.execute();

        return new Return();
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public Return autoComplete4Product(final Parameter _parameter)
        throws EFapsException
    {
        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        if (input.length() > 0) {
            final boolean nameSearch = Character.isDigit(input.charAt(0));

            final SearchQuery query = new SearchQuery();
            query.setQueryTypes("Sales_Products_VirtualInventoryStock");
            if (nameSearch) {
                query.addWhereExprMatchValue("ProductName", input + "*");
            } else {
                query.addWhereExprMatchValue("ProductDescription", input + "*").setIgnoreCase(true);
            }
            query.addSelect("OID");
            query.execute();
            final List<Instance> instances = new ArrayList<Instance>();
            while (query.next()) {
                instances.add(Instance.get((String) query.get("OID")));
            }
            if (instances.size() > 0) {
                final MultiPrintQuery print = new MultiPrintQuery(instances);
                print.addAttribute("ProductName", "ProductDescription");
                print.addSelect("linkto[Product].oid");
                print.addSelect("linkto[Product].attribute[SalesUnit]");
                print.addSelect("linkto[Product].attribute[Dimension]");
                print.addSelect("linkto[Storage].id");
                print.addSelect("linkto[Storage].attribute[Name]");
                print.execute();

                final Map<String, Map<String, String>> oid2value = new HashMap<String, Map<String, String>>();
                final Map<String, Map<String, Long>> oid2Storage = new HashMap<String, Map<String, Long>>();
                while (print.next()) {
                    final String name = print.<String> getAttribute("ProductName");
                    final String desc = print.<String> getAttribute("ProductDescription");
                    final String oid = print.<String> getSelect("linkto[Product].oid");
                    final BigDecimal salesUnit = print.<BigDecimal> getSelect("linkto[Product].attribute[SalesUnit]");

                    if (oid2value.containsKey(oid)) {
                        final Map<String, Long> storagemap = oid2Storage.get(oid);
                        storagemap.put(print.<String> getSelect("linkto[Storage].attribute[Name]"),
                                        print.<Long> getSelect("linkto[Storage].id"));
                        final Map<String, String> map = oid2value.get(oid);
                        map.put("storage", getStorageFieldStr(storagemap));
                    } else {
                        final Map<String, String> map = new HashMap<String, String>();
                        oid2value.put(oid, map);
                        final Map<String, Long> storagemap = new TreeMap<String, Long>();
                        storagemap.put(print.<String> getSelect("linkto[Storage].attribute[Name]"),
                                        print.<Long> getSelect("linkto[Storage].id"));
                        oid2Storage.put(oid, storagemap);
                        map.put("eFapsAutoCompleteKEY", oid);
                        map.put("eFapsAutoCompleteVALUE", name);
                        map.put("eFapsAutoCompleteCHOICE", nameSearch ? name + " - " + desc : desc + " - " + name);
                        map.put("salesUnit", salesUnit.toString());
                        map.put("salesUnitRO", salesUnit.toString());
                        map.put("salesUnitPagackes", "1");
                        map.put("uoM", getUoMFieldStr(print.<Long> getSelect("linkto[Product].attribute[Dimension]")));
                        map.put("productDesc", desc);
                        map.put("storage", getStorageFieldStr(storagemap));
                        list.add(map);
                    }
                }
            }
        }
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * Make an javascript array for the userinterface.
     *
     * @param _storagemap map os storages to be used
     * @return String
     */
    protected String getStorageFieldStr(final Map<String, Long> _storagemap)
    {
        final StringBuilder js = new StringBuilder();
        // Sales-Configuration
        final Instance warehouse = SystemConfiguration.get(UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f"))
                        .getLink("DefaultWarehouse");

        js.append("new Array('").append(warehouse.getId()).append("'");

        for (final Entry<String, Long> entry : _storagemap.entrySet()) {
            js.append(",'").append(entry.getValue()).append("','").append(entry.getKey()).append("'");
        }
        js.append(")");
        return js.toString();
    }

}
