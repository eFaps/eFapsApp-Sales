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
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.ui.wicket.util.EFapsKey;
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

    @Override
    protected void add2JavaScript4ProductAutoComplete(final Parameter _parameter,
                                                      final Map<String, String> _map)
        throws EFapsException
    {
        final Instance prodInst = Instance.get(_map.get(EFapsKey.AUTOCOMPLETE_KEY.getKey()));
        final Map<String, Long> storagemap = new TreeMap<String, Long>();
        if (prodInst.isValid()) {
            final PrintQuery print = new PrintQuery(prodInst);
            print.addAttribute(CIProducts.ProductAbstract.SalesUnit);
            print.execute();
            final BigDecimal salesUnit = print.<BigDecimal>getAttribute(CIProducts.ProductAbstract.SalesUnit);

            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.Inventory);
            queryBldr.addWhereAttrEqValue(CIProducts.Inventory.Product, prodInst);
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selStorageInst = new SelectBuilder()
                        .linkto(CIProducts.Inventory.Storage).instance();
            final SelectBuilder selStorageName = new SelectBuilder()
                        .linkto(CIProducts.Inventory.Storage).attribute(CIProducts.StorageAbstract.Name);
            multi.addSelect(selStorageInst, selStorageName);
            multi.execute();
            while (multi.next()) {
                storagemap.put(print.<String>getSelect(selStorageName),
                                print.<Instance>getSelect(selStorageInst).getId());
            }
            _map.put("salesUnit", salesUnit.toString());
            _map.put("salesUnitRO", salesUnit.toString());
            _map.put("salesUnitPagackes", "1");
            _map.put("storage", getStorageFieldStr(storagemap));
        }
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
