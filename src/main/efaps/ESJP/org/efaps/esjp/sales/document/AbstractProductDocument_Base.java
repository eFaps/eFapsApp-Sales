/*
 * Copyright 2003 - 2012 The eFaps Team
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.datamodel.Attribute;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.products.Storage;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;


/**
 * basic class for documents that do not have prices.
 *
 * @author The eFaps Team
 * @version $Id: AbstractDocument_Base.java 3674 2010-01-28 18:52:35Z jan.moxter$
 */
@EFapsUUID("92e52f22-f3f2-43b3-87c3-ad224d9832fc")
@EFapsRevision("$Rev$")
public abstract class AbstractProductDocument_Base
    extends AbstractDocument
{

    /**
     * Method to create the basic Document. The method checks for the Type to be
     * created for every attribute if a related field is in the parameters.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return Instance of the document.
     * @throws EFapsException on error.
     */
    protected CreatedDoc createDoc(final Parameter _parameter)
        throws EFapsException
    {

        final CreatedDoc createdDoc = new CreatedDoc();

        final Insert insert = new Insert(getType4DocCreate(_parameter));
        final String name = getDocName4Create(_parameter);
        insert.add(CISales.DocumentStockAbstract.Name, name);
        createdDoc.getValues().put(CISales.DocumentStockAbstract.Name.name, name);
        final String date = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentStockAbstract.Date.name));
        if (date != null) {
            insert.add(CISales.DocumentStockAbstract.Date, date);
            createdDoc.getValues().put(CISales.DocumentStockAbstract.Date.name, date);
        }
        final String duedate = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentStockAbstract.DueDate.name));
        if (duedate != null) {
            insert.add(CISales.DocumentStockAbstract.DueDate, duedate);
            createdDoc.getValues().put(CISales.DocumentStockAbstract.DueDate.name, duedate);
        }
        final String contact = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentStockAbstract.Contact.name));
        if (contact != null) {
            final Instance inst = Instance.get(contact);
            if (inst.isValid()) {
                insert.add(CISales.DocumentStockAbstract.Contact, inst.getId());
                createdDoc.getValues().put(CISales.DocumentStockAbstract.Contact.name, inst);
            }
        }
        final String note = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentStockAbstract.Note.name));
        if (note != null) {
            insert.add(CISales.DocumentStockAbstract.Note, note);
            createdDoc.getValues().put(CISales.DocumentStockAbstract.Note.name, note);
        }
        final String revision = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentStockAbstract.Revision.name));
        if (revision != null) {
            insert.add(CISales.DocumentStockAbstract.Revision, revision);
            createdDoc.getValues().put(CISales.DocumentStockAbstract.Revision.name, revision);
        }
        final String salesperson = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentStockAbstract.Salesperson.name));
        if (salesperson != null) {
            insert.add(CISales.DocumentStockAbstract.Salesperson, salesperson);
            createdDoc.getValues().put(CISales.DocumentStockAbstract.Salesperson.name, salesperson);
        }

        addStatus2DocCreate(_parameter, insert, createdDoc);
        add2DocCreate(_parameter, insert, createdDoc);
        insert.execute();

        createdDoc.setInstance(insert.getInstance());
        return createdDoc;
    }


    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _createdDoc created doc
     * @throws EFapsException on error
     */
    protected void createPositions(final Parameter _parameter,
                                   final CreatedDoc _createdDoc)
        throws EFapsException
    {

        for (int i = 0; i < getPositionsCount(_parameter); i++) {
            final Insert posIns = new Insert(getType4PositionCreate(_parameter));
            posIns.add(CISales.PositionAbstract.PositionNumber, i + 1);
            posIns.add(CISales.PositionAbstract.DocumentAbstractLink, _createdDoc.getInstance().getId());

            final String[] product = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                            CISales.PositionAbstract.Product.name));
            if (product != null && product.length > i) {
                final Instance inst = Instance.get(product[i]);
                if (inst.isValid()) {
                    posIns.add(CISales.PositionAbstract.Product, inst.getId());
                }
            }

            final String[] productDesc = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                            CISales.PositionAbstract.ProductDesc.name));
            if (productDesc != null && productDesc.length > i) {
                posIns.add(CISales.PositionAbstract.ProductDesc, productDesc[i]);
            }

            final String[] uoM = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                            CISales.PositionAbstract.UoM.name));
            if (uoM != null && uoM.length > i) {
                posIns.add(CISales.PositionAbstract.UoM, uoM[i]);
            }

            final String[] quantity = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                            CISales.PositionAbstract.Quantity.name));
            if (quantity != null && quantity.length > i) {
                posIns.add(CISales.PositionAbstract.Quantity, quantity[i]);
            }

            add2PositionCreate(_parameter, posIns, _createdDoc, i);

            posIns.execute();
        }
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _createdDoc   created doc
     * @throws EFapsException on error
     */
    protected void connect2ProductDocumentType(final Parameter _parameter,
                                               final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final Instance instDocType = Instance.get(_parameter.getParameterValue("documentType"));
        if (instDocType.isValid() && _createdDoc.getInstance().isValid()) {
            final Insert insert = new Insert(CISales.Document2DocumentType);
            insert.add(CISales.Document2DocumentType.DocumentLink, _createdDoc.getInstance());
            insert.add(CISales.Document2DocumentType.DocumentTypeLink, instDocType);
            insert.execute();
        }
    }

    /**
     * Method is called in the process of creation.
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _posInsert    insert to add to
     * @param _createdDoc   document created
     * @param _idx          index
     * @throws EFapsException on Error
     */
    protected void add2PositionCreate(final Parameter _parameter,
                                      final Insert _posInsert,
                                      final CreatedDoc _createdDoc,
                                      final int _idx)
        throws EFapsException
    {
        // used by implementation
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return mapping for autocomplete
     * @throws EFapsException on error
     */
    public Return autoComplete4Storage(final Parameter _parameter)
        throws EFapsException
    {
        @SuppressWarnings("unchecked")
        final Map<Object, Object> properties = (Map<Object, Object>) _parameter.get(ParameterValues.PROPERTIES);
        if (!properties.containsKey("key")) {
            properties.put("Key", "ID");
        }
        final Storage storage = new Storage();
        return storage.autoComplete4Storage(_parameter);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return update mapping
     * @throws EFapsException on error
     */
    public Return updateFields4Storage(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();
        list.add(map);
        final StringBuilder js = new StringBuilder()
            .append("require([\"dojo/query\"], function(query){")
            .append("query(\"select[name=storage]\").forEach(function(node){")
            .append("node.value=\"").append(_parameter.getParameterValue("storageSetter")).append("\";")
            .append("});")
            .append("});");

        map.put(EFapsKey.FIELDUPDATE_JAVASCRIPT.getKey(), js.toString());
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }


    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return storage id
     * @throws EFapsException on error
     */
    protected long evaluateStorage4PositionTrigger(final Parameter _parameter)
        throws EFapsException
    {
        long ret = 0;
        final Map<String, String[]> param = Context.getThreadContext().getParameters();
        final String[] storageIds = param.get("storage");

        final Map<?, ?> map = (Map<?, ?>) _parameter.get(ParameterValues.NEW_VALUES);
        final Instance instance = _parameter.getInstance();

        // first check for explicitly set warehouses
        if (storageIds != null) {
            final Object[] posObj = (Object[]) map.get(instance.getType().getAttribute(
                            CISales.PositionAbstract.PositionNumber.name));
            final int pos = (Integer) posObj[0];
            final String[] productOids = param.get("product");
            int i = 0;
            int idx = 0;
            while (i < pos) {
                if (i < productOids.length && Instance.get(productOids[i]).isValid()) {
                    idx = i;
                }
                i++;
            }
            ret = Long.valueOf(storageIds[idx]);
        } else {
            // check for a warehouse by the inventory
            final Object[] productID = (Object[]) map.get(instance.getType().getAttribute(
                            CISales.PositionAbstract.Product.name));
            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.Inventory);
            queryBldr.addWhereAttrEqValue(CIProducts.Inventory.Product, productID[0]);
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CIProducts.Inventory.Storage);
            multi.executeWithoutAccessCheck();
            if (multi.next()) {
                ret = multi.<Long>getAttribute(CIProducts.Inventory.Storage);
            }
        }
        if (ret == 0) {
            // if we did not find on yet, get a warehouse
            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.Warehouse);
            final InstanceQuery query = queryBldr.getQuery();
            query.executeWithoutAccessCheck();
            if (query.next()) {
                ret = query.getCurrentValue().getId();
            }
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _type Type to create
     * @param _storageId sorage id
     * @throws EFapsException on error
     */
    protected void createTransaction4PositionTrigger(final Parameter _parameter,
                                                     final Type _type,
                                                     final long _storageId)
        throws EFapsException
    {
        final Map<String, String[]> param = Context.getThreadContext().getParameters();
        final String[] date = param.get("date");
        final Instance instance = _parameter.getInstance();

        final Map<?, ?> map = (Map<?, ?>) _parameter.get(ParameterValues.NEW_VALUES);

        final Object[] productID = (Object[]) map.get(instance.getType().getAttribute(
                        CISales.PositionAbstract.Product.name));
        final Object[] qauntity = (Object[]) map.get(instance.getType().getAttribute(
                        CISales.PositionAbstract.Quantity.name));
        final Object[] uom = (Object[]) map.get(instance.getType().getAttribute(CISales.PositionAbstract.UoM.name));

        final Insert insert = new Insert(_type);
        insert.add(CIProducts.TransactionAbstract.Quantity, qauntity[0]);
        insert.add(CIProducts.TransactionAbstract.Storage, _storageId);
        insert.add(CIProducts.TransactionAbstract.Product, productID[0]);
        insert.add(CIProducts.TransactionAbstract.Description, getDescription4PositionTrigger(_parameter));
        insert.add(CIProducts.TransactionAbstract.Date, date == null ? new DateTime() : date[0]);
        insert.add(CIProducts.TransactionAbstract.Document, evaluateParentDocId4PositionTrigger(_parameter));
        insert.add(CIProducts.TransactionAbstract.UoM, uom[0]);
        insert.execute();
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return description
     * @throws EFapsException on error
     */
    protected String getDescription4PositionTrigger(final Parameter _parameter)
        throws EFapsException
    {
        return DBProperties.getProperty(_parameter.getInstance().getType().getName() + ".description4Trigger");
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return id of parent doc
     * @throws EFapsException on error
     */
    protected Long evaluateParentDocId4PositionTrigger(final Parameter _parameter)
        throws EFapsException
    {
        long ret = 0;
        final Instance instance = _parameter.getInstance();
        final Map<?, ?> map = (Map<?, ?>) _parameter.get(ParameterValues.NEW_VALUES);
        for (final Attribute attr : instance.getType().getAttributes().values()) {
            if (attr.hasLink() && attr.getLink().isKindOf(CISales.DocumentAbstract.getType())) {
                final Object[] docObj = (Object[]) map.get(attr);
                if (docObj != null) {
                    ret = (long) docObj[0];
                    break;
                }
            }
        }
        return ret;
    }
}
