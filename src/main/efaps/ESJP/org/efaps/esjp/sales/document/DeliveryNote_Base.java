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

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SearchQuery;
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
@EFapsUUID("363ad7a5-1e7b-4194-89e3-a31d07d783df")
@EFapsRevision("$Rev$")
public abstract class DeliveryNote_Base
    extends AbstractDocument
{
    /**
     * Create a new DeliveryNote.
     *
     * @param _parameter parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error,
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        createDoc(_parameter);
        return new Return();
    }

    /**
     * Method for create a new DeliveryNote and return the instance of the deliveryNote.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return instance of the deliveryNote.
     * @throws EFapsException on error.
     */
    protected Instance createDoc(final Parameter _parameter)
        throws EFapsException
    {
        final String date = _parameter.getParameterValue("date");
        final Long contactid = Instance.get(_parameter.getParameterValue("contact")).getId();
        final Insert insert = new Insert(CISales.DeliveryNote);
        insert.add(CISales.DeliveryNote.Contact, contactid.toString());
        insert.add(CISales.DeliveryNote.Date, date);
        insert.add(CISales.DeliveryNote.Salesperson, _parameter.getParameterValue("salesperson"));
        insert.add(CISales.DeliveryNote.Name, _parameter.getParameterValue("name4create"));
        insert.add(CISales.DeliveryNote.Status,
                        ((Long) Status.find(CISales.DeliveryNoteStatus.uuid, "Closed").getId()).toString());
        insert.execute();
        Integer i = 0;
        for (final String quantity : _parameter.getParameterValues("quantity")) {
            final Insert posIns = new Insert(CISales.DeliveryNotePosition);
            final Long productdId = Instance.get(_parameter.getParameterValues("product")[i]).getId();
            posIns.add(CISales.DeliveryNotePosition.DeliveryNote, insert.getId());
            posIns.add(CISales.DeliveryNotePosition.PositionNumber, i.toString());
            posIns.add(CISales.DeliveryNotePosition.Product, productdId.toString());
            posIns.add(CISales.DeliveryNotePosition.ProductDesc, _parameter.getParameterValues("productDesc")[i]);
            posIns.add(CISales.DeliveryNotePosition.Quantity, (new BigDecimal(quantity)).toString());
            posIns.add(CISales.DeliveryNotePosition.UoM, _parameter.getParameterValues("uoM")[i]);
            posIns.execute();
            i++;
        }
        return insert.getInstance();
    }

    public Return getJS4SelectInvoiceForm(final Parameter _parameter)
        throws EFapsException
    {
        final StringBuilder js = new StringBuilder();
        js.append("<script type=\"text/javascript\">")
                        .append("Wicket.Event.add(window, \"domready\", function(event) {")
                        .append("var obj=wicketGet(\"label25\");")
                        .append("obj.setfocus();")
                        .append(" });")
                        .append("</script>");

        final Return retVal = new Return();
        retVal.put(ReturnValues.SNIPLETT, js.toString());
        return retVal;
    }

    public Return deliveryNotePositionInsertTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final Map<String, String[]> param = Context.getThreadContext().getParameters();
        final String[] productOids = param.get("product");
        final String[] storageIds = param.get("storage");

        final Instance instance = _parameter.getInstance();
        final Map<?, ?> map = (Map<?, ?>) _parameter.get(ParameterValues.NEW_VALUES);

        final Object[] productID = (Object[]) map.get(instance.getType().getAttribute("Product"));
        final Object[] qauntity = (Object[]) map.get(instance.getType().getAttribute("Quantity"));
        final Object[] deliveryNodeId = (Object[]) map.get(instance.getType().getAttribute("DeliveryNote"));
        final Object[] uom = (Object[]) map.get(instance.getType().getAttribute("UoM"));
        String storage = null;
        if (storageIds != null) {
            for (int i = 0; i < productOids.length; i++) {
                if (productID[0].equals(((Long) Instance.get(productOids[i]).getId()).toString())) {
                    storage = storageIds[i];
                }
            }
        } else {

//            final SearchQuery query = new SearchQuery();
//            query.setQueryTypes("Products_Inventory");
//            query.addWhereExprEqValue("Product", productID[0].toString());
//            query.addSelect("Storage");
//            query.execute();
//            if (query.next()) {
//                storage = ((Long) query.get("Storage")).toString();
//            }


        	// Change SearchQuery to QueryBuilder.

            final QueryBuilder query = new QueryBuilder(CIProducts.Inventory);
            query.addWhereAttrEqValue(CIProducts.Inventory.Product, productID[0]);
            MultiPrintQuery multi = query.getPrint();
            multi.addAttribute(CIProducts.Inventory.Storage);
            multi.execute();
            while(multi.next()){
            	storage = multi.<String>getAttribute(CIProducts.Inventory.Storage);
            }
        }

        final Insert insert = new Insert("Products_TransactionOutbound");
        insert.add("Quantity", qauntity[0]);
        insert.add("Storage", storage.toString());
        insert.add("Product", productID[0]);
        insert.add("Description",
                        DBProperties.getProperty("org.efaps.esjp.sales.document.DeliveryNote.description4Trigger"));
        insert.add("Date", new DateTime());
        insert.add("Document", deliveryNodeId[0]);
        insert.add("UoM", uom[0]);
        insert.execute();

        return new Return();
    }
}
