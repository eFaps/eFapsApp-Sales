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

import java.math.BigDecimal;
import java.util.Map;

import org.efaps.admin.datamodel.Status;
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
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: ReturnSlip_Base.java 8536 2013-01-16 19:22:45Z
 *          sara.landa@efaps.org $
 */
@EFapsUUID("791d92ae-bbf6-45c0-979e-bdcce5c48d50")
@EFapsRevision("$Rev$")
public abstract class ReturnSlip_Base
    extends AbstractDocument
{

    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Instance returnSlip = createDoc(_parameter);
        connect2ProductDocumentType(_parameter, returnSlip);
        return new Return();
    }

    /**
     * Method to obtain the instance of a return slip.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return Instance of the document.
     * @throws EFapsException on error.
     */
    public Instance createDoc(final Parameter _parameter)
        throws EFapsException
    {
        final String date = _parameter.getParameterValue("date");
        final Long contactid = Instance.get(_parameter.getParameterValue("contact")).getId();
        final Insert insert = new Insert(CISales.ReturnSlip);
        insert.add(CISales.ReturnSlip.Contact, contactid.toString());
        insert.add(CISales.ReturnSlip.Date, date);
        insert.add(CISales.ReturnSlip.Salesperson, _parameter.getParameterValue("salesperson"));
        insert.add(CISales.ReturnSlip.Name, _parameter.getParameterValue("name4create"));
        insert.add(CISales.ReturnSlip.Status, ((Long) Status.find(CISales.ReturnSlipStatus.uuid, "Closed")
                        .getId()).toString());
        insert.execute();
        Integer i = 0;
        for (final String quantity : _parameter.getParameterValues("quantity")) {
            final Insert posIns = new Insert(CISales.ReturnSlipPosition);
            final Long productdId = Instance.get(_parameter.getParameterValues("product")[i]).getId();
            posIns.add(CISales.ReturnSlipPosition.ReturnSlip, insert.getId());
            posIns.add(CISales.ReturnSlipPosition.PositionNumber, i + 1);
            posIns.add(CISales.ReturnSlipPosition.Product, productdId);
            posIns.add(CISales.ReturnSlipPosition.ProductDesc, _parameter.getParameterValues("productDesc")[i]);
            posIns.add(CISales.ReturnSlipPosition.Quantity, (new BigDecimal(quantity)).toString());
            posIns.add(CISales.ReturnSlipPosition.UoM, _parameter.getParameterValues("uoM")[i]);
            posIns.execute();
            i++;
        }
        return insert.getInstance();
    }

    protected void connect2ProductDocumentType(final Parameter _parameter,
                                               final Instance _instance)
        throws EFapsException
    {
        final Instance instDocType = Instance.get(_parameter.getParameterValue("documentType"));
        if (instDocType.isValid() && _instance.isValid()) {
            final Insert insert = new Insert(CISales.Document2DocumentType);
            insert.add(CISales.Document2DocumentType.DocumentLink, _instance);
            insert.add(CISales.Document2DocumentType.DocumentTypeLink, instDocType);
            insert.execute();
        }
    }

    public Return returnSlipPositionInsertTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final Map<String, String[]> param = Context.getThreadContext().getParameters();
        final String[] storageIds = param.get("storage");
        final String[] date = param.get("date");

        final Instance instance = _parameter.getInstance();
        final Map<?, ?> map = (Map<?, ?>) _parameter.get(ParameterValues.NEW_VALUES);

        final Object[] productID = (Object[]) map.get(instance.getType().getAttribute("Product"));
        final Object[] qauntity = (Object[]) map.get(instance.getType().getAttribute("Quantity"));
        final Object[] deliveryNodeId = (Object[]) map.get(instance.getType().getAttribute("ReturnSlip"));
        final Object[] uom = (Object[]) map.get(instance.getType().getAttribute("UoM"));
        final Object[] pos = (Object[]) map.get(instance.getType().getAttribute(
                        CISales.ReturnSlipPosition.PositionNumber.name));

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
                        DBProperties.getProperty("org.efaps.esjp.sales.document.ReturnSlip.description4Trigger"));
        insert.add(CIProducts.TransactionInbound.Date, date[0] == null ? new DateTime() : date[0]);
        insert.add(CIProducts.TransactionInbound.Document, deliveryNodeId[0]);
        insert.add(CIProducts.TransactionInbound.UoM, uom[0]);
        insert.execute();

        return new Return();
    }
}
