/*
 * Copyright 2003 - 2014 The eFaps Team
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

package org.efaps.esjp.sales.report;

import java.util.List;
import java.util.Map;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.util.EFapsException;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperReport;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: SalesKardexReport_Base.java 12300 2014-03-25 16:25:14Z
 *          m.aranya@moxter.net $
 */
@EFapsUUID("6678c3f1-8076-4e04-8ef5-02d0924aefc0")
@EFapsApplication("eFapsApp-Sales")
public abstract class SalesKardexMultipleReport_Base
    extends SalesKardexReport
{

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final JasperReport _jasperReport,
                     final Parameter _parameter,
                     final JRDataSource _parentSource,
                     final Map<String, Object> _jrParameters)
        throws EFapsException
    {
        final List<Instance> listStorage = getStorageInstList(_parameter);
        final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.TransactionInOutAbstract);
        attrQueryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Storage, listStorage.toArray());

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductAbstract);
        queryBldr.addWhereAttrInQuery(CIProducts.ProductAbstract.ID,
                        attrQueryBldr.getAttributeQuery(CIProducts.TransactionInOutAbstract.Product));
        final List<Instance> prodInsts = queryBldr.getQuery().execute();
        super.init(_jasperReport, _parameter, _parentSource, _jrParameters,
                        prodInsts.toArray(new Instance[prodInsts.size()]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void add2Map4ProductInfo(final Parameter _parameter,
                                       final Map<String, Object> _map,
                                       final Instance _transInstance,
                                       final Instance _prodInst)
        throws EFapsException
    {
        final ProductKardex product = new ProductKardex(_prodInst);
        _map.put("prodName", product.getProductName());
        _map.put("prodType", product.getProductExistType());
        _map.put("prodDescr", product.getProductDesc());
        _map.put("prodUoM", product.getProductUoM());
    }

}
