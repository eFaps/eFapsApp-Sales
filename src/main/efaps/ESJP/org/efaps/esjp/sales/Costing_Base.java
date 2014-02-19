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

package org.efaps.esjp.sales;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("65041308-73a6-47de-bd1d-3dacc37dbc6c")
@EFapsRevision("$Rev$")
public abstract class Costing_Base
{

    public Return execute(final Parameter _parameter)
        throws EFapsException
    {
        final SelectBuilder selDocInst = new SelectBuilder()
                        .linkto(CIProducts.TransactionInOutAbstract.Document).instance();
        final SelectBuilder selProdInst = new SelectBuilder()
                        .linkto(CIProducts.TransactionInOutAbstract.Product).instance();

        final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.Costing);
        final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CIProducts.Costing.TransactionAbstractLink);

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.TransactionInOutAbstract);
        queryBldr.addWhereAttrNotInQuery(CIProducts.TransactionInOutAbstract.ID, attrQuery);
        queryBldr.addOrderByAttributeAsc(CIProducts.TransactionInOutAbstract.Date);
        queryBldr.addOrderByAttributeAsc(CIProducts.TransactionInOutAbstract.Position);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addSelect(selDocInst, selProdInst);
        multi.setEnforceSorted(true);
        multi.executeWithoutAccessCheck();

        final Map<Instance, Instance> prod2Costing = new HashMap<Instance, Instance>();
        while (multi.next()) {
            final BigDecimal cost = getCost(multi.getCurrentInstance(),
                            multi.<Instance>getSelect(selDocInst), multi.<Instance>getSelect(selProdInst));
            final Instance costing = insertCost(multi.getCurrentInstance(), BigDecimal.ZERO, cost, BigDecimal.ZERO);
            final Instance prodInst = multi.<Instance>getSelect(selProdInst);
            if (!prod2Costing.containsKey(prodInst)) {
                prod2Costing.put(prodInst, costing);
            }
        }

        for (final Entry<Instance, Instance> entry : prod2Costing.entrySet()) {
            prod2Costing.put(entry.getKey(), getPenultimate(entry.getValue()));
        }

        return new Return();
    }

    protected BigDecimal getCost(final Instance _transInstance,
                                 final Instance _docInstance,
                                 final Instance _prodInstance)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;

        if (CISales.DeliveryNote.getType().equals(_docInstance.getType())) {
            final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Document2DerivativeDocument);
            attrQueryBldr.addWhereAttrEqValue(CISales.Document2DerivativeDocument.From, _docInstance);
            final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.Document2DerivativeDocument.To);

            final QueryBuilder queryBldr = new QueryBuilder(CISales.IncomingInvoicePosition);
            queryBldr.addWhereAttrInQuery(CISales.IncomingInvoicePosition.DocumentAbstractLink, attrQuery);
            queryBldr.addWhereAttrEqValue(CISales.IncomingInvoicePosition.Product, _prodInstance);
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CISales.IncomingInvoicePosition.NetUnitPrice);
            multi.execute();
            if (multi.next()) {
                ret = multi.<BigDecimal>getAttribute(CISales.IncomingInvoicePosition.NetUnitPrice);
            }
        } else if (CISales.IncomingInvoice.getType().equals(_docInstance.getType())) {
            final QueryBuilder queryBldr = new QueryBuilder(CISales.IncomingInvoicePosition);
            queryBldr.addWhereAttrEqValue(CISales.IncomingInvoicePosition.DocumentAbstractLink, _docInstance);
            queryBldr.addWhereAttrEqValue(CISales.IncomingInvoicePosition.Product, _prodInstance);
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CISales.IncomingInvoicePosition.NetUnitPrice);
            multi.execute();
            if (multi.next()) {
                ret = multi.<BigDecimal>getAttribute(CISales.IncomingInvoicePosition.NetUnitPrice);
            }
        }

        return ret;
    }

    protected Instance getPenultimate(final Instance _instance)
        throws EFapsException
    {
        Instance ret = null;
        Instance transRet = null;

        final SelectBuilder selTransInst = new SelectBuilder()
                        .linkto(CIProducts.Costing.TransactionAbstractLink).instance();

        final SelectBuilder selProdInst = new SelectBuilder()
                        .linkto(CIProducts.Costing.TransactionAbstractLink)
                        .attribute(CIProducts.TransactionInOutAbstract.Product).instance();

        final SelectBuilder selDate = new SelectBuilder()
                        .linkto(CIProducts.Costing.TransactionAbstractLink)
                        .attribute(CIProducts.TransactionInOutAbstract.Date);

        final PrintQuery print = new PrintQuery(_instance);
        print.addSelect(selTransInst, selProdInst, selDate);
        print.executeWithoutAccessCheck();

        final Instance transInst = print.<Instance>getSelect(selTransInst);

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.TransactionInOutAbstract);
        queryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Product,
                        print.<Instance>getSelect(selProdInst));
        queryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Date, print.<DateTime>getSelect(selDate));
        queryBldr.addOrderByAttributeAsc(CIProducts.TransactionInOutAbstract.Position);
        final InstanceQuery query = queryBldr.getQuery();
        query.executeWithoutAccessCheck();
        Instance prev = null;
        while (query.next()) {
            if (query.getCurrentValue().equals(transInst)) {
                if (prev != null) {
                    transRet = prev;
                }
            } else {
                prev = query.getCurrentValue();
            }
        }
        if (prev == null) {
            final QueryBuilder queryBldr2 = new QueryBuilder(CIProducts.TransactionInOutAbstract);
            queryBldr2.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Product,
                            print.<Instance>getSelect(selProdInst));
            queryBldr2.addWhereAttrLessValue(CIProducts.TransactionInOutAbstract.Date, print.<DateTime>getSelect(selDate));
            queryBldr2.addOrderByAttributeAsc(CIProducts.TransactionInOutAbstract.Date);
            queryBldr2.addOrderByAttributeAsc(CIProducts.TransactionInOutAbstract.Position);
            final InstanceQuery query2 = queryBldr2.getQuery();
            query2.setLimit(1);
            query2.executeWithoutAccessCheck();
            while (query2.next()) {
                transRet = query2.getCurrentValue();
            }
        }
        if (transRet == null) {
            transRet = transInst;
            ret = _instance;
        } else {
            final QueryBuilder queryBldrRes = new QueryBuilder(CIProducts.Costing);
            queryBldrRes.addWhereAttrEqValue(CIProducts.Costing.TransactionAbstractLink, transRet);
            final InstanceQuery queryRes = queryBldrRes.getQuery();
            queryRes.executeWithoutAccessCheck();
            if (queryRes.next()) {
                ret = queryRes.getCurrentValue();
            }
        }

        return ret;
    }

    protected Instance insertCost(final Instance _transaction,
                              final BigDecimal _quantity,
                              final BigDecimal _cost,
                              final BigDecimal _result)
        throws EFapsException
    {
        final Insert insert = new Insert(CIProducts.Costing);
        insert.add(CIProducts.Costing.Quantity, _quantity);
        insert.add(CIProducts.Costing.TransactionAbstractLink, _transaction);
        insert.add(CIProducts.Costing.Cost, _cost);
        insert.add(CIProducts.Costing.Result, _result);
        insert.executeWithoutTrigger();

        return insert.getInstance();
    }
}
