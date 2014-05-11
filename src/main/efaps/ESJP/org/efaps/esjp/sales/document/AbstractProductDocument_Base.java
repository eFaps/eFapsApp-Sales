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

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.admin.datamodel.StatusValue;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.ci.CITableSales;
import org.efaps.esjp.common.uiform.Field;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.products.Product;
import org.efaps.esjp.products.Storage;
import org.efaps.esjp.products.util.Products;
import org.efaps.esjp.products.util.Products.ProductIndividual;
import org.efaps.esjp.products.util.ProductsSettings;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.Sales.ProdDocActivation;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AbstractProductDocument.class);

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
     * @param _parameter Paramater as passed by the eFaps API
     * @param _createdDoc the created document
     * @throws EFapsException on error
     */
    protected void createIndiviuals(final Parameter _parameter,
                                    final CreatedDoc _createdDoc)
        throws EFapsException
    {
        if (Products.getSysConfig().getAttributeValueAsBoolean(ProductsSettings.ACTIVATEINDIVIDUAL)) {

            final String[] products = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                            CISales.PositionAbstract.Product.name));

            final Object date = _createdDoc.getValues().get(CISales.DocumentStockAbstract.Date.name);

            for (final String product : products) {
                final Instance prodInst = Instance.get(product);
                if (prodInst.isValid()) {
                    final String[] individuals = _parameter.getParameterValues(prodInst.getOid());
                    if (individuals != null && individuals.length > 0) {
                        final PrintQuery print = new PrintQuery(prodInst);
                        print.addAttribute(CIProducts.ProductAbstract.Individual);
                        print.executeWithoutAccessCheck();
                        final ProductIndividual prIn = print
                                        .<ProductIndividual>getAttribute(CIProducts.ProductAbstract.Individual);
                        if (prIn != null && !ProductIndividual.NONE.equals(prIn)) {
                            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.TransactionInOutAbstract);
                            queryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Product, prodInst);
                            queryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Document,
                                            _createdDoc.getInstance());
                            final MultiPrintQuery multi = queryBldr.getPrint();
                            multi.addAttribute(CIProducts.TransactionInOutAbstract.Storage,
                                            CIProducts.TransactionInOutAbstract.UoM,
                                            CIProducts.TransactionInOutAbstract.Quantity,
                                            CIProducts.TransactionInOutAbstract.Description);
                            multi.executeWithoutAccessCheck();
                            multi.next();
                            final Object storage = multi.getAttribute(CIProducts.TransactionInOutAbstract.Storage);
                            final Object uom = multi.getAttribute(CIProducts.TransactionInOutAbstract.UoM);
                            final Object descr = multi.getAttribute(CIProducts.TransactionInOutAbstract.Description);

                            Type prodType;
                            Type relType;
                            boolean clazz;
                            Object quantity;
                            if (ProductIndividual.BATCH.equals(prIn)) {
                                prodType = CIProducts.ProductBatch.getType();
                                relType = CIProducts.StockProductAbstract2Batch.getType();
                                clazz = false;
                                quantity = multi.getAttribute(CIProducts.TransactionInOutAbstract.Quantity);
                            } else {
                                prodType = CIProducts.ProductIndividual.getType();
                                relType = CIProducts.StockProductAbstract2Individual.getType();
                                clazz = true;
                                quantity = 1;
                            }

                            for (final String individual : individuals) {
                                final Map<String, Object> map = new HashMap<String, Object>();
                                map.put("Name", individual);
                                final Instance indInst = new Product().cloneProduct(_parameter, prodInst,
                                                prodType, map, clazz);
                                final Insert relInsert = new Insert(relType);
                                relInsert.add(CIProducts.Product2ProductAbstract.FromAbstract, prodInst);
                                relInsert.add(CIProducts.Product2ProductAbstract.ToAbstract, indInst);
                                relInsert.execute();

                                final Insert transInsert = new Insert(CIProducts.TransactionIndividualInbound);
                                transInsert.add(CIProducts.TransactionAbstract.Quantity, quantity);
                                transInsert.add(CIProducts.TransactionAbstract.Storage, storage);
                                transInsert.add(CIProducts.TransactionAbstract.Product, indInst);
                                transInsert.add(CIProducts.TransactionAbstract.Description, descr);
                                transInsert.add(CIProducts.TransactionAbstract.Date, date == null ? new DateTime()
                                                : date);
                                transInsert.add(CIProducts.TransactionAbstract.Document, _createdDoc.getInstance());
                                transInsert.add(CIProducts.TransactionAbstract.UoM, uom);
                                transInsert.add(CIProducts.TransactionAbstract.UoM, uom);
                                transInsert.executeWithoutAccessCheck();
                            }
                        }
                    }
                }
            }
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
        final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        list.add(map);

        final StringBuilder js = new StringBuilder();
        final String[] rows = _parameter.getParameterValues("product");
        final List<Map<KeyDef, Object>> values = new ArrayList<Map<KeyDef, Object>>();
        final Instance storage = Instance.get(_parameter.getParameterValue("storageSetter"));
        if (storage.isValid()) {
            for (final String row : rows) {
                final Map<KeyDef, Object> map2 = new HashMap<KeyDef, Object>();
                final Instance prod = Instance.get(row);
                if (prod != null && prod.isValid()) {
                    map2.put(new KeyDefStr("quantityInStock"), getStock4ProductInStorage(_parameter, prod, storage));
                    map2.put(new KeyDefStr("storage"), storage.getOid());
                }
                values.add(map2);
            }
            final List<Map<String, Object>> strValues = convertMap4Script(_parameter, values);
            js.append(getSetFieldValuesScript(_parameter, strValues, null))
                .append("require([\"dojo/query\"], function(query){")
                .append("query(\"select[name=storage]\").forEach(function(node){")
                .append("node.value=\"").append(storage.getId()).append("\";")
                .append("});")
                .append("});");
            InterfaceUtils.appendScript4FieldUpdate(map, js.toString());
        }
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @throws EFapsException on error
     * @return listmap for update
     */
    public Return updateFields4Product(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();

        final int selected = getSelectedRow(_parameter);
        final Instance prodInst = Instance.get(_parameter.getParameterValues("product")[selected]);

        Instance storInst = null;
        if (_parameter.getParameterValues("storage") != null) {
            storInst  = Instance.get(_parameter.getParameterValues("storage")[selected]);
        }

        if (prodInst.isValid()) {
            if (storInst != null && storInst.isValid()) {
                map.put(CITableSales.Sales_DeliveryNotePositionTable.quantityInStock.name,
                                getStock4ProductInStorage(_parameter, prodInst, storInst));
            }
            add2UpdateField4Product(_parameter, map, prodInst);
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        } else {
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
            final StringBuilder js = new StringBuilder();
            js.append("document.getElementsByName('productAutoComplete')[").append(selected).append("].focus()");
            map.put(EFapsKey.FIELDUPDATE_JAVASCRIPT.getKey(), js.toString());
        }
        return retVal;
    }

    @Override
    protected void add2JavaScript4Postions(final Parameter _parameter,
                                           final Collection<Map<KeyDef, Object>> _values,
                                           final Set<String> _noEscape)
        throws EFapsException
    {
        super.add2JavaScript4Postions(_parameter, _values, _noEscape);
        final Instance defaultStorageInst = Products.getSysConfig().getLink(ProductsSettings.DEFAULTWAREHOUSE);
        if (defaultStorageInst.isValid()) {
            for (final Map<KeyDef, Object> map : _values) {
                final Instance prodInst = Instance.get(((String[]) map.get(new KeyDefStr("product")))[0]);
                if (prodInst.isValid()) {
                    final String quantityInStock = getStock4ProductInStorage(_parameter, prodInst, defaultStorageInst);
                    map.put(new KeyDefStr("quantityInStock"), quantityInStock);
                }
            }
        } else {
            AbstractProductDocument_Base.LOG.warn("A storage default should be configurated");
        }
    }

    /**
     * method to get string to values with inventory.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @param _productinst Product.
     * @param _storageInst Storage.
     * @return String to values.
     * @throws EFapsException on error.
     */
    protected String getStock4ProductInStorage(final Parameter _parameter,
                                               final Instance _productinst,
                                               final Instance _storageInst)
        throws EFapsException
    {
        final StringBuilder ret = new StringBuilder();
        final DecimalFormat qtyFrmt = NumberFormatter.get().getFrmt4Quantity(getTypeName4SysConf(_parameter));

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.Inventory);
        queryBldr.addWhereAttrEqValue(CIProducts.Inventory.Storage, _storageInst);
        queryBldr.addWhereAttrEqValue(CIProducts.Inventory.Product, _productinst);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIProducts.Inventory.Quantity, CIProducts.Inventory.Reserved);
        multi.execute();
        if (multi.next()) {
            final BigDecimal quantity = multi.getAttribute(CIProducts.Inventory.Quantity);
            final BigDecimal quantityReserved = multi.getAttribute(CIProducts.Inventory.Reserved);

            ret.append(qtyFrmt.format(quantity)).append(" / ").append(qtyFrmt.format(quantityReserved));
        } else {
            ret.append("0 / 0");
        }
        return ret.toString();
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
        final String[] storage = param.get("storage");

        final Map<?, ?> map = (Map<?, ?>) _parameter.get(ParameterValues.NEW_VALUES);
        final Instance instance = _parameter.getInstance();

        // first check for explicitly set warehouses
        if (storage != null) {
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
            if (Instance.get(storage[idx]).isValid()) {
                ret = Instance.get(storage[idx]).getId();
            } else {
                ret = Long.valueOf(storage[idx]);
            }
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

    public Return changeStatusWithInverseTransaction(final Parameter _parameter)
        throws EFapsException
    {
        new StatusValue().setStatus(_parameter);
        inverseTransaction(_parameter);
        return new Return();
    }

    public Return inverseTransaction(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instance = _parameter.getInstance();
        if (instance.isValid()) {
            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.TransactionInOutAbstract);
            queryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Document, instance);
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CIProducts.TransactionInOutAbstract.Quantity,
                            CIProducts.TransactionInOutAbstract.Storage,
                            CIProducts.TransactionInOutAbstract.Product,
                            CIProducts.TransactionInOutAbstract.Description,
                            CIProducts.TransactionInOutAbstract.Date,
                            CIProducts.TransactionInOutAbstract.UoM);
            multi.execute();
            while (multi.next()) {
                Insert insert;
                if (CIProducts.TransactionInbound.getType().equals(multi.getCurrentInstance().getType())) {
                    insert = new Insert(CIProducts.TransactionOutbound);
                } else {
                    insert = new Insert(CIProducts.TransactionInbound);
                }
                insert.add(CIProducts.TransactionInOutAbstract.Quantity,
                                multi.getAttribute(CIProducts.TransactionInOutAbstract.Quantity));
                insert.add(CIProducts.TransactionInOutAbstract.Storage,
                                multi.getAttribute(CIProducts.TransactionInOutAbstract.Storage));
                insert.add(CIProducts.TransactionInOutAbstract.Product,
                                multi.getAttribute(CIProducts.TransactionInOutAbstract.Product));
                insert.add(CIProducts.TransactionInOutAbstract.Description,
                                multi.getAttribute(CIProducts.TransactionInOutAbstract.Description));
                insert.add(CIProducts.TransactionInOutAbstract.Date,
                                multi.getAttribute(CIProducts.TransactionInOutAbstract.Date));
                insert.add(CIProducts.TransactionInOutAbstract.Document, instance);
                insert.add(CIProducts.TransactionInOutAbstract.UoM,
                                multi.getAttribute(CIProducts.TransactionInOutAbstract.UoM));
                insert.execute();
            }
        }

        return new Return();
    }

    public Return dropDown4ProdDocTypeFieldValue(final Parameter _parameter)
        throws EFapsException
    {

        final Field field = new Field()
        {

            @Override
            protected void add2QueryBuilder4List(final Parameter _parameter,
                                                 final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final Map<Integer, String> activations = analyseProperty(_parameter, "Activation");
                final List<ProdDocActivation> pactivt = new ArrayList<ProdDocActivation>();
                for (final String activation : activations.values()) {
                    final ProdDocActivation pDAct = Sales.ProdDocActivation.valueOf(activation);
                    pactivt.add(pDAct);
                }
                _queryBldr.addWhereAttrEqValue(CISales.ProductDocumentType.Activation, pactivt.toArray());
            };
        };
        return field.dropDownFieldValue(_parameter);
    }
}
