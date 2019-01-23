/*
 * Copyright 2003 - 2019 The eFaps Team
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
 */

package org.efaps.esjp.sales.document;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;

/**
 */
@EFapsUUID("77aebe5f-aebd-4662-b503-efdc6e46d9d3")
@EFapsApplication("eFapsApp-Sales")
public abstract class OrderInbound_Base
    extends AbstractDocumentSum
{
    /**
     * Used to store the Revision in the Context.
     */
    public static final String REVISIONKEY =  OrderInbound.class.getName() + ".RevisionKey";

    /**
     * Method for create a new Incoming Order.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = createDoc(_parameter);
        createPositions(_parameter, createdDoc);
        connect2Derived(_parameter, createdDoc);
        connect2Object(_parameter, createdDoc);
        return new Return();
    }

    @Override
    protected void add2DocCreate(final Parameter _parameter,
                                 final Insert _insert,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final String seqKey = Sales.ORDERINBOUND_REVSEQ.get();
        final NumberGenerator numgen = isUUID(seqKey)
                        ? NumberGenerator.get(UUID.fromString(seqKey))
                        : NumberGenerator.get(seqKey);
        if (numgen != null) {
            final String revision = numgen.getNextVal();
            Context.getThreadContext().setSessionAttribute(OrderInbound.REVISIONKEY, revision);
            _insert.add(CISales.OrderInbound.Revision, revision);
        }
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return with Snipplet
     * @throws EFapsException on error
     */
    public Return showRevisionFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        return getRevisionSequenceFieldValue(_parameter, OrderInbound.REVISIONKEY);
    }

    /**
     * Edit.
     *
     * @param _parameter Parameter from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final EditedDoc editDoc = editDoc(_parameter);
        updatePositions(_parameter, editDoc);
        return new Return();
    }

    @Override
    protected Type getType4SysConf(final Parameter _parameter)
        throws EFapsException
    {
        return getCIType().getType();
    }

    @Override
    public String getTypeName4SysConf(final Parameter _parameter)
        throws EFapsException
    {
        return getType4SysConf(_parameter).getName();
    }

    @Override
    public CIType getCIType()
        throws EFapsException
    {
        return CISales.OrderInbound;
    }

    @Override
    protected void add2UpdateMap4Contact(final Parameter _parameter,
                                         final Instance _contactInstance,
                                         final Map<String, Object> _map)
        throws EFapsException
    {
        super.add2UpdateMap4Contact(_parameter, _contactInstance, _map);

        if (Sales.ORDERINBOUND_FROMQUOTATIONAC.exists()) {
            final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter,
                            Sales.ORDERINBOUND_FROMQUOTATIONAC.get());
            InterfaceUtils.appendScript4FieldUpdate(_map, getJS4Doc4Contact(_parameter, _contactInstance,
                            CIFormSales.Sales_OrderInboundForm.quotation.name, queryBldr));
        }
    }

    @Override
    protected StringBuilder add2JavaScript4DocumentContact(final Parameter _parameter,
                                                           final List<Instance> _instances,
                                                           final Instance _contactInstance)
        throws EFapsException
    {
        final StringBuilder ret = super.add2JavaScript4DocumentContact(_parameter, _instances, _contactInstance);

        if (Sales.ORDERINBOUND_FROMQUOTATIONAC.exists()) {
            final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter,
                            Sales.ORDERINBOUND_FROMQUOTATIONAC.get());
            ret.append(getJS4Doc4Contact(_parameter, _contactInstance,
                            CIFormSales.Sales_OrderInboundForm.quotation.name, queryBldr));
        }
        return ret;
    }

    /**
     * Connect two invoice trigger.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return connect2InvoiceTrigger(final Parameter _parameter)
        throws EFapsException
    {
        DocComparator.markPartial(_parameter, _parameter.getInstance());
        return new Return();
    }
}
