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
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("bc9f7db3-8a53-4b57-951e-da1fbc2c7307")
@EFapsRevision("$Rev$")
public abstract class GoodsIssueSlip_Base
    extends AbstractProductDocument
{
    /**
     * Method for create a new GoodsIssueSlip.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc doc = createDoc(_parameter);
        createPositions(_parameter, doc);
        connect2ProductDocumentType(_parameter, doc);
        return new Return();
    }

    /**
     * Method for trigger in case insert a new position the GoodsIssueSlip.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return goodsIssueSlipPositionInsertTrigger(final Parameter _parameter)
        throws EFapsException
    {
        createTransaction4PositionTrigger(_parameter, CIProducts.TransactionInbound.getType(),
                        evaluateStorage4PositionTrigger(_parameter));
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

            final QueryBuilder queryBldr = new QueryBuilder(CISales.Products_VirtualInventoryStock);
            if (nameSearch) {
                queryBldr.addWhereAttrMatchValue(CISales.Products_VirtualInventoryStock.ProductName, input + "*");
            } else {
                queryBldr.addWhereAttrMatchValue(CISales.Products_VirtualInventoryStock.ProductDescription,
                                input + "*").setIgnoreCase(true);
            }
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CISales.Products_VirtualInventoryStock.OID);
            multi.execute();
            final List<Instance> instances = new ArrayList<Instance>();
            while (multi.next()) {
                instances.add(Instance.get(multi.<String>getAttribute(CISales.Products_VirtualInventoryStock.OID)));
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
                    final String name = print.<String>getAttribute("ProductName");
                    final String desc = print.<String>getAttribute("ProductDescription");
                    final String oid = print.<String>getSelect("linkto[Product].oid");
                    final BigDecimal salesUnit = print.<BigDecimal>getSelect("linkto[Product].attribute[SalesUnit]");

                    if (oid2value.containsKey(oid)) {
                        final Map<String, Long> storagemap = oid2Storage.get(oid);
                        storagemap.put(print.<String>getSelect("linkto[Storage].attribute[Name]"),
                                        print.<Long>getSelect("linkto[Storage].id"));
                        final Map<String, String> map = oid2value.get(oid);
                        map.put("storage", getStorageFieldStr(storagemap));
                    } else {
                        final Map<String, String> map = new HashMap<String, String>();
                        oid2value.put(oid, map);
                        final Map<String, Long> storagemap = new TreeMap<String, Long>();
                        storagemap.put(print.<String>getSelect("linkto[Storage].attribute[Name]"),
                                        print.<Long>getSelect("linkto[Storage].id"));
                        oid2Storage.put(oid, storagemap);
                        map.put("eFapsAutoCompleteKEY", oid);
                        map.put("eFapsAutoCompleteVALUE", name);
                        map.put("eFapsAutoCompleteCHOICE", nameSearch ? name + " - " + desc : desc + " - " + name);
                        map.put("salesUnit", salesUnit.toString());
                        map.put("salesUnitRO", salesUnit.toString());
                        map.put("salesUnitPagackes", "1");
                        map.put("uoM", getUoMFieldStr(print.<Long>getSelect("linkto[Product].attribute[Dimension]")));
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
        throws EFapsException
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
