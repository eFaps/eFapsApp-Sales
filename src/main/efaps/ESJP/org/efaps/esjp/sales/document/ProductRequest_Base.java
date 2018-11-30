/*
 * Copyright 2003 - 2016 The eFaps Team
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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;
import org.efaps.util.cache.CacheReloadException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("97c5f066-ac96-465c-89da-f890896b7ab4")
@EFapsApplication("eFapsApp-Sales")
public abstract class ProductRequest_Base
    extends AbstractProductDocument
{
    /**
     * {@inheritDoc}
     */
    @Override
    public CIType getCIType()
        throws EFapsException
    {
        return CISales.ProductRequest;
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final CreatedDoc createdDoc = createDoc(_parameter);
        createPositions(_parameter, createdDoc);
        connect2Derived(_parameter, createdDoc);
        connect2Object(_parameter, createdDoc);
        connect2Terms(_parameter, createdDoc);

        final File file = createReport(_parameter, createdDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        ret.put(ReturnValues.INSTANCE, createdDoc.getInstance());
        return ret;
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final EditedDoc editedDoc = editDoc(_parameter);
        updatePositions(_parameter, editedDoc);
        updateConnection2Object(_parameter, editedDoc);
        final File file = createReport(_parameter, editedDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return connect2OrderOutboundTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_parameter.getInstance());
        final SelectBuilder selStatus = SelectBuilder.get().linkto(CISales.Document2DocumentAbstract.FromAbstractLink)
                        .attribute(CISales.ProductRequest.Status);
        final SelectBuilder selProdReqInst = SelectBuilder.get().linkto(
                        CISales.Document2DocumentAbstract.FromAbstractLink).instance();
        print.addSelect(selProdReqInst, selStatus);
        print.executeWithoutAccessCheck();

        final Instance prodReqInst = print.getSelect(selProdReqInst);
        final Status status = Status.get(print.<Long>getSelect(selStatus));
        final DocComparator comp = getComparator(_parameter, prodReqInst);
        final Map<Status, Status> maping = getStatusMapping4connect2OrderOutbound();
        if (comp.quantityIsZero() && maping.containsKey(status)) {
            final Update update = new Update(prodReqInst);
            update.add(CISales.ProductRequest.Status, maping.get(status));
            update.executeWithoutAccessCheck();
        }
        return new Return();
    }

    /**
     * Gets the comparator.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _prodReqInst the prod req inst
     * @return the comparator
     * @throws EFapsException on error
     */
    protected DocComparator getComparator(final Parameter _parameter,
                                          final Instance _prodReqInst)
        throws EFapsException
    {
        final DocComparator ret = new DocComparator();
        ret.setDocInstance(_prodReqInst);

        final QueryBuilder queryBldr = new QueryBuilder(CISales.ProductRequest2OrderOutbound);
        queryBldr.addType(CISales.ProductRequest2OrderOutbound);
        queryBldr.addWhereAttrEqValue(CISales.ProductRequest2OrderOutbound.FromAbstractLink, _prodReqInst);
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selDocInst = SelectBuilder.get()
                        .linkto(CISales.Document2DocumentAbstract.ToAbstractLink)
                        .instance();
        multi.addSelect(selDocInst);
        multi.executeWithoutAccessCheck();
        while (multi.next()) {
            final Instance docInst = multi.getSelect(selDocInst);
            final DocComparator docComp = new DocComparator();
            docComp.setDocInstance(docInst);
            ret.substractQuantity(docComp);
        }
        return ret;
    }

    /**
     * Gets the status mapping4connect2 incoming invoice.
     *
     * @return the status mapping4connect2 incoming invoice
     * @throws CacheReloadException the cache reload exception
     */
    protected Map<Status, Status> getStatusMapping4connect2OrderOutbound()
        throws CacheReloadException
    {
        final Map<Status, Status> ret = new HashMap<>();
        ret.put(Status.find(CISales.ProductRequestStatus.Open), Status.find(CISales.ProductRequestStatus.Closed));
        return ret;
    }

}
