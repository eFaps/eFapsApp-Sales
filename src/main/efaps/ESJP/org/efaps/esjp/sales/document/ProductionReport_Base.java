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

package org.efaps.esjp.sales.document;

import java.util.Map;

import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.ci.CITableSales;
import org.efaps.esjp.products.Product;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("522348f5-1270-4bc6-a4c0-f935d010399a")
@EFapsRevision("$Rev$")
public abstract class ProductionReport_Base
    extends AbstractProductDocument
{

    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = createDoc(_parameter);
        createPositions(_parameter, createdDoc);
        return new Return();
    }

    @Override
    protected void add2PositionCreate(final Parameter _parameter,
                                      final Insert _posInsert,
                                      final CreatedDoc _createdDoc,
                                      final int _idx)
        throws EFapsException
    {
        final String[] product = _parameter
                        .getParameterValues(CITableSales.Sales_ProductionReportPositionTable.batchProduct.name);
        if (product != null && product.length > _idx) {
            final Instance prodInst = Instance.get(product[_idx]);
            if (prodInst.isValid()) {
                final Product prod = new Product();
                final Instance batchInst = prod.createBatch(_parameter, prodInst);
                _posInsert.add(CISales.PositionAbstract.Product, batchInst);
            }
        }
    }

    public Return productionReportPositionInsertTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final Map<String, String[]> param = Context.getThreadContext().getParameters();
        final String[] storageIds = param.get(CITableSales.Sales_ProductionReportPositionTable.storage.name);
        final String[] date = param.get(CIFormSales.Sales_ProductionReportForm.date.name);

        final Instance instance = _parameter.getInstance();
        final Map<?, ?> map = (Map<?, ?>) _parameter.get(ParameterValues.NEW_VALUES);

        final Object[] productID = (Object[]) map.get(instance.getType().getAttribute(
                        CISales.PositionAbstract.Product.name));
        final Object[] qauntity = (Object[]) map.get(instance.getType().getAttribute(
                        CISales.PositionAbstract.Quantity.name));
        final Object[] docId = (Object[]) map.get(instance.getType().getAttribute(
                        CISales.PositionAbstract.DocumentAbstractLink.name));
        final Object[] uom = (Object[]) map.get(instance.getType().getAttribute(CISales.PositionAbstract.UoM.name));
        final Object[] pos = (Object[]) map.get(instance.getType().getAttribute(
                        CISales.PositionAbstract.PositionNumber.name));

        Long storage = null;
        if (storageIds != null) {
            final Integer posInt = (Integer) pos[0];
            storage = Long.valueOf(storageIds[posInt - 1]);
        } else {
            final QueryBuilder query = new QueryBuilder(CIProducts.Inventory);
            query.addWhereAttrEqValue(CIProducts.Inventory.Product, productID[0]);
            final MultiPrintQuery multi = query.getPrint();
            multi.addAttribute(CIProducts.Inventory.Storage);
            multi.execute();
            if (multi.next()) {
                storage = multi.<Long>getAttribute(CIProducts.Inventory.Storage);
            }
        }

        final Insert insert = new Insert(CIProducts.TransactionInbound);
        insert.add(CIProducts.TransactionInbound.Quantity, qauntity[0]);
        insert.add(CIProducts.TransactionInbound.Storage, storage);
        insert.add(CIProducts.TransactionInbound.Product, productID[0]);
        insert.add(CIProducts.TransactionInbound.Description,
                        DBProperties.getProperty("org.efaps.esjp.sales.document.ProductionReport.description4Trigger"));
        insert.add(CIProducts.TransactionInbound.Date, date[0] == null ? new DateTime() : date[0]);
        insert.add(CIProducts.TransactionInbound.Document, docId[0]);
        insert.add(CIProducts.TransactionInbound.UoM, uom[0]);
        insert.execute();

        return new Return();
    }
}
