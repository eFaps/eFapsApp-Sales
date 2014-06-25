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
 * Revision:        $Rev: 8342 $
 * Last Changed:    $Date: 2012-12-11 09:42:17 -0500 (Tue, 11 Dec 2012) $
 * Last Changed By: $Author: jan@moxter.net $
 */


package org.efaps.esjp.sales.document;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.sales.Calculator;
import org.efaps.util.EFapsException;


/**
 *
 * @author The eFaps Team
 * @version $Id: ReturnUsageReport.java 10186 2013-09-12 11:41:31Z m.aranya@moxter.net $
 */
@EFapsUUID("65d49d25-c1b9-4883-8bb1-0c53292ee789")
@EFapsRevision("$Rev: 10186 $")
public abstract class ServiceOrderOutbound_Base
    extends AbstractDocumentSum
{

    /**
     * Executed from a Command execute event to create a new OrderOutbound.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final CreatedDoc createdDoc = createDoc(_parameter);
        createPositions(_parameter, createdDoc);
        connectChannel2Document(_parameter, createdDoc);
        connect2Derived(_parameter, createdDoc);
        connect2Terms(_parameter, createdDoc);
        final File file = createReport(_parameter, createdDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        executeProcess(_parameter, createdDoc);
        ret.put(ReturnValues.INSTANCE, createdDoc.getInstance());
        return ret;
    }

    /**
     * Executed from a Command execute event to create a new OrderOutbound.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final EditedDoc createdDoc = editDoc(_parameter);
        updatePositions(_parameter, createdDoc);

        final File file = createReport(_parameter, createdDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    protected void connectChannel2Document(final Parameter _parameter,
                                           final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final Instance instCondition = Instance.get(_parameter.getParameterValue("conditionSales"));
        if (instCondition.isValid() && _createdDoc.getInstance().isValid()) {
            final Insert insert = new Insert(CISales.ChannelSalesChannel2Document);
            insert.add(CISales.ChannelSalesChannel2Document.FromLink, instCondition);
            insert.add(CISales.ChannelSalesChannel2Document.ToLink, _createdDoc.getInstance());
            insert.execute();
        }
    }

    @Override
    protected boolean isContact2JavaScript4Document(final Parameter _parameter,
                                                    final List<Instance> _instances)
        throws EFapsException
    {
        boolean ret = true;
        if (!_instances.isEmpty() && _instances.get(0).isValid()) {
            ret = !_instances.get(0).getType().isKindOf(CISales.ServiceRequest.getType());
        }
        return ret;
    }

    @Override
    public Calculator getCalculator(final Parameter _parameter,
                                    final Calculator _oldCalc,
                                    final String _oid,
                                    final String _quantity,
                                    final String _unitPrice,
                                    final String _discount,
                                    final boolean _priceFromDB,
                                    final int _idx)
        throws EFapsException
    {

        return new Calculator(_parameter, _oldCalc, _oid, _quantity, _unitPrice, _discount, _priceFromDB, this)
        {

            private static final long serialVersionUID = 1L;

            @Override
            protected UUID getPriceListUUID()
            {
                return CIProducts.ProductPricelistPurchase.uuid;
            }
        };
    }
}
