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
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.Listener;
import org.efaps.admin.ui.AbstractCommand;
import org.efaps.admin.ui.field.FieldTable;
import org.efaps.ci.CIType;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.admin.datamodel.StatusValue;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.ci.CITableSales;
import org.efaps.esjp.common.uiform.Edit;
import org.efaps.esjp.common.uiform.Field;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.erp.listener.IOnCreateDocument;
import org.efaps.esjp.products.Batch;
import org.efaps.esjp.products.Product;
import org.efaps.esjp.products.Storage;
import org.efaps.esjp.products.util.Products;
import org.efaps.esjp.products.util.Products.ProductIndividual;
import org.efaps.esjp.products.util.ProductsSettings;
import org.efaps.esjp.sales.Calculator;
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

        // call possible listeners
        for (final IOnCreateDocument listener : Listener.get().<IOnCreateDocument>invoke(
                        IOnCreateDocument.class)) {
            listener.afterCreate(_parameter, createdDoc);
        }

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
            posIns.add(CISales.PositionAbstract.DocumentAbstractLink, _createdDoc.getInstance());

            String individualName = null;
            final String[] product = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                            CISales.PositionAbstract.Product.name));
            if (product != null && product.length > i) {
                Instance inst = Instance.get(product[i]);
                if (Products.getSysConfig().getAttributeValueAsBoolean(ProductsSettings.ACTIVATEINDIVIDUAL)) {
                    if (inst.getType().isKindOf(CIProducts.ProductIndividualAbstract.getType())) {
                        final PrintQuery print = new PrintQuery(inst);
                        final SelectBuilder sel = SelectBuilder.get()
                                        .linkfrom(CIProducts.StockProductAbstract2IndividualAbstract,
                                                        CIProducts.StockProductAbstract2IndividualAbstract.ToAbstract)
                                        .linkto(CIProducts.StockProductAbstract2IndividualAbstract.FromAbstract)
                                        .instance();
                        print.addSelect(sel);
                        print.addAttribute(CIProducts.ProductAbstract.Name);
                        print.executeWithoutAccessCheck();
                        inst = print.getSelect(sel);
                        individualName = print.getAttribute(CIProducts.ProductAbstract.Name);
                    }
                }
                if (inst.isValid()) {
                    posIns.add(CISales.PositionAbstract.Product, inst.getId());
                }
            }

            final String[] productDesc = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                            CISales.PositionAbstract.ProductDesc.name));
            if (productDesc != null && productDesc.length > i) {
                String descr = productDesc[i];
                if (individualName != null) {
                    descr = descr + " - Nº: " + individualName;
                }
                posIns.add(CISales.PositionAbstract.ProductDesc, descr);
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
            if (individualName != null) {
                CIType transType;
                if (posIns.getInstance().getType().isKindOf(CISales.ReturnSlipPosition)) {
                    transType = CIProducts.TransactionIndividualInbound;
                } else {
                    transType = CIProducts.TransactionIndividualOutbound;
                }

                final Insert transInsert = new Insert(transType);
                transInsert.add(CIProducts.TransactionAbstract.Quantity, quantity[i]);
                transInsert.add(CIProducts.TransactionAbstract.Storage,
                                Instance.get(_parameter.getParameterValues("storage")[i]));
                transInsert.add(CIProducts.TransactionAbstract.Product, Instance.get(product[i]));
                transInsert.add(CIProducts.TransactionAbstract.Description, "TODO");
                transInsert.add(CIProducts.TransactionAbstract.Date,
                                _createdDoc.getValue(CISales.DocumentStockAbstract.Date.name));
                transInsert.add(CIProducts.TransactionAbstract.Document, _createdDoc.getInstance());
                transInsert.add(CIProducts.TransactionAbstract.UoM, uoM[i]);
                transInsert.executeWithoutAccessCheck();
            }
        }
    }

    /**
     * Method to edit the basic Document. The method checks for the Type to be
     * created for every attribute if a related field is in the parameters.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return the edited document
     * @throws EFapsException on error.
     */
    protected EditedDoc editDoc(final Parameter _parameter)
        throws EFapsException
    {
        return editDoc(_parameter, new EditedDoc(_parameter.getInstance()));
    }

    /**
     * Method to edit the basic Document. The method checks for the Type to be
     * created for every attribute if a related field is in the parameters.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @param _editDoc edited document
     * @return the edited document
     * @throws EFapsException on error.
     */
    protected EditedDoc editDoc(final Parameter _parameter,
                                final EditedDoc _editDoc)
        throws EFapsException
    {
        final List<Calculator> calcList = analyseTable(_parameter, null);
        _editDoc.addValue(AbstractDocument_Base.CALCULATORS_VALUE, calcList);

        final AbstractCommand command = (AbstractCommand) _parameter.get(ParameterValues.UIOBJECT);

        final Edit edit = new Edit()
        {
            @Override
            protected void add2MainUpdate(final Parameter _parameter,
                                          final Update _update)
                throws EFapsException
            {
                super.add2MainUpdate(_parameter, _update);
                addStatus2DocEdit(_parameter, _update, _editDoc);
                add2DocEdit(_parameter, _update, _editDoc);
            }
        };
        final List<FieldTable> fieldTables = new ArrayList<FieldTable>();

        edit.updateMainElements(_parameter, command.getTargetForm(), _editDoc.getInstance(),
                        _editDoc.getValues(), fieldTables);

        _editDoc.addValue(AbstractDocument_Base.FIELDTABLES, fieldTables);
        return _editDoc;
    }

    /**
     * Update the positions of a Document.
     * @param _parameter Parameter as passed by the eFaps API
     * @param _editDoc  EditDoc the postions that will be updated belong to
     * @throws EFapsException on error
     */
    protected void updatePositions(final Parameter _parameter,
                                   final EditedDoc _editDoc)
        throws EFapsException
    {
        @SuppressWarnings("unchecked")
        final List<Calculator> calcList = (List<Calculator>) _editDoc.getValue(AbstractDocument_Base.CALCULATORS_VALUE);

        final Edit edit = new Edit()
        {
            @Override
            protected void add2Update4FieldTable(final Parameter _parameter,
                                                 final Update _update,
                                                 final RowUpdate _row)
                throws EFapsException
            {
                super.add2Update4FieldTable(_parameter, _update, _row);
                add2PositionUpdate(_parameter, calcList.get(_row.getIndex()), _update, _row.getIndex());
            }
        };
        @SuppressWarnings("unchecked")
        final List<FieldTable> fieldTables = (List<FieldTable>) _editDoc.getValue(AbstractDocument_Base.FIELDTABLES);
        edit.updateFieldTable(_parameter, _editDoc.getInstance(), fieldTables);
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
                                map.put(CIProducts.ProductAbstract.Individual.name,
                                                Products.ProductIndividual.NONE.getInt());
                                final Instance indInst;
                                if (ProductIndividual.BATCH.equals(prIn)) {
                                    if (individual.equals(Products.ProductIndividual.BATCH.toString())) {
                                        final String name = new Batch().getNewName(_parameter, prodInst);
                                        map.put(CIProducts.ProductAbstract.Name.name, name);
                                        indInst = new Product().cloneProduct(_parameter, prodInst,
                                                    prodType, map, clazz);
                                        final Insert relInsert = new Insert(relType);
                                        relInsert.add(CIProducts.Product2ProductAbstract.FromAbstract, prodInst);
                                        relInsert.add(CIProducts.Product2ProductAbstract.ToAbstract, indInst);
                                        relInsert.execute();
                                    } else {
                                        indInst = Instance.get(individual);
                                    }
                                } else {
                                    map.put(CIProducts.ProductAbstract.Name.name, individual);
                                    indInst = new Product().cloneProduct(_parameter, prodInst, prodType, map, clazz);
                                    final Insert relInsert = new Insert(relType);
                                    relInsert.add(CIProducts.Product2ProductAbstract.FromAbstract, prodInst);
                                    relInsert.add(CIProducts.Product2ProductAbstract.ToAbstract, indInst);
                                    relInsert.execute();
                                }

                                if (indInst != null && indInst.isValid()) {
                                    final Insert transInsert = new Insert(CIProducts.TransactionIndividualInbound);
                                    transInsert.add(CIProducts.TransactionAbstract.Quantity, quantity);
                                    transInsert.add(CIProducts.TransactionAbstract.Storage, storage);
                                    transInsert.add(CIProducts.TransactionAbstract.Product, indInst);
                                    transInsert.add(CIProducts.TransactionAbstract.Description, descr);
                                    transInsert.add(CIProducts.TransactionAbstract.Date, date == null ? new DateTime()
                                                    : date);
                                    transInsert.add(CIProducts.TransactionAbstract.Document, _createdDoc.getInstance());
                                    transInsert.add(CIProducts.TransactionAbstract.UoM, uom);
                                    transInsert.executeWithoutAccessCheck();
                                }
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
        if (storage.isValid() && rows != null) {
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
                .append("node.value=\"").append(storage.getOid()).append("\";")
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
        final Instance defaultStorageInst = getDefaultStorage(_parameter);
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

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.InventoryAbstract);
        queryBldr.addWhereAttrEqValue(CIProducts.InventoryAbstract.Storage, _storageInst);
        queryBldr.addWhereAttrEqValue(CIProducts.InventoryAbstract.Product, _productinst);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIProducts.InventoryAbstract.Quantity, CIProducts.InventoryAbstract.Reserved);
        multi.execute();
        if (multi.next()) {
            final BigDecimal quantity = multi.getAttribute(CIProducts.InventoryAbstract.Quantity);
            final BigDecimal quantityReserved = multi.getAttribute(CIProducts.InventoryAbstract.Reserved);

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
        insert.executeWithoutAccessCheck();
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

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new empty Return
     * @throws EFapsException on error
     */
    public Return changeStatusWithInverseTransaction(final Parameter _parameter)
        throws EFapsException
    {
        new StatusValue().setStatus(_parameter);
        inverseTransaction(_parameter);
        return new Return();
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new empty Return
     * @throws EFapsException on error
     */
    public Return inverseTransaction(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instance = _parameter.getInstance();
        if (instance.isValid()) {
            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.TransactionInbound);
            queryBldr.addType(CIProducts.TransactionOutbound, CIProducts.TransactionIndividualInbound,
                            CIProducts.TransactionIndividualOutbound, CIProducts.TransactionReservationInbound,
                            CIProducts.TransactionReservationOutbound);
            queryBldr.addWhereAttrEqValue(CIProducts.TransactionAbstract.Document, instance);
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CIProducts.TransactionAbstract.Quantity,
                            CIProducts.TransactionAbstract.Storage,
                            CIProducts.TransactionAbstract.Product,
                            CIProducts.TransactionAbstract.Description,
                            CIProducts.TransactionAbstract.Date,
                            CIProducts.TransactionAbstract.UoM);
            multi.execute();
            while (multi.next()) {
                Insert insert;
                if (CIProducts.TransactionInbound.getType().equals(multi.getCurrentInstance().getType())) {
                    insert = new Insert(CIProducts.TransactionOutbound);
                } else if (CIProducts.TransactionOutbound.getType().equals(multi.getCurrentInstance().getType())) {
                    insert = new Insert(CIProducts.TransactionInbound);
                } else if (CIProducts.TransactionIndividualInbound.getType().equals(
                                multi.getCurrentInstance().getType())) {
                    insert = new Insert(CIProducts.TransactionIndividualOutbound);
                } else if (CIProducts.TransactionIndividualOutbound.getType().equals(
                                multi.getCurrentInstance().getType())) {
                    insert = new Insert(CIProducts.TransactionIndividualInbound);
                } else if (CIProducts.TransactionReservationInbound.getType().equals(
                                multi.getCurrentInstance().getType())) {
                    insert = new Insert(CIProducts.TransactionReservationOutbound);
                } else {
                    insert = new Insert(CIProducts.TransactionReservationInbound);
                }
                insert.add(CIProducts.TransactionAbstract.Quantity,
                                multi.getAttribute(CIProducts.TransactionAbstract.Quantity));
                insert.add(CIProducts.TransactionAbstract.Storage,
                                multi.getAttribute(CIProducts.TransactionAbstract.Storage));
                insert.add(CIProducts.TransactionAbstract.Product,
                                multi.getAttribute(CIProducts.TransactionAbstract.Product));
                insert.add(CIProducts.TransactionAbstract.Description,
                                multi.getAttribute(CIProducts.TransactionAbstract.Description));
                insert.add(CIProducts.TransactionAbstract.Date,
                                multi.getAttribute(CIProducts.TransactionAbstract.Date));
                insert.add(CIProducts.TransactionAbstract.Document, instance);
                insert.add(CIProducts.TransactionAbstract.UoM,
                                multi.getAttribute(CIProducts.TransactionAbstract.UoM));
                insert.execute();
            }
        }
        return new Return();
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new empty Return
     * @throws EFapsException on error
     */
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

    /**
     * Method is called from a hidden field to include javascript in the form.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return Return containing the javascript
     * @throws EFapsException on error
     */
    public Return getStorageJavaScriptUIValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final StringBuilder js = new StringBuilder();
        js.append("<script type=\"text/javascript\">\n")
            .append("require([\"dojo/topic\",\"dojo/query\"], function(topic,query){\n")
                .append("topic.subscribe(\"eFaps/addRow/positionTable\", function(){\n")
                    .append("query(\"input[name=storageSetter]\").forEach(function(node){\n")
                        .append("if (node.value!==\"\") {")
                            .append("query(\"select[name=storage]\").forEach(function(node2){\n")
                                .append("node2.value=node.value;\n")
                             .append("});\n")
                         .append("}")
                     .append("});\n")
                 .append("});\n")
            .append("});\n")
            .append("</script>");
        retVal.put(ReturnValues.SNIPLETT, js.toString());
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return description
     * @throws EFapsException on error
     */
    protected String getDescription4ProductTransaction(final Parameter _parameter,
                                                       final Instance _docInst)
        throws EFapsException
    {
        return DBProperties.getProperty(_docInst.getType().getName() + ".description4ProductTransaction");
    }

    /**
     * @param _parameter    Parameter as passed from the eFaps API
     * @param _docInst      Instance of the document the transaction will be created for
     * @param _storageInst  Instance of the storage the transaction will be created for
     * @throws EFapsException on error
     */
    public void createProductTransaction4Document(final Parameter _parameter,
                                                  final Instance _docInst,
                                                  final Instance _storageInst)
        throws EFapsException
    {
        final List<Position> positions = getPositions4Document(_parameter, _docInst);
        final String transDescr = getDescription4ProductTransaction(_parameter, _docInst);
        final PrintQuery print = new PrintQuery(_docInst);
        print.addAttribute(CIERP.DocumentAbstract.Date);
        print.execute();
        final DateTime date = print.getAttribute(CIERP.DocumentAbstract.Date);
        for (final Position pos : positions) {
            pos.setStorageInst(_storageInst).setTransactionDescr(transDescr).setDocInst(_docInst).setDate(date);
        }
        createProductTransaction(_parameter, positions);
    }

    public void createProductTransaction(final Parameter _parameter,
                                         final List<Position> _positions)
        throws EFapsException
    {

        for (final Position pos : _positions) {
            final Insert transInsert = new Insert(pos.getTransactionType());
            transInsert.add(CIProducts.TransactionAbstract.Quantity, pos.getQuantity());
            transInsert.add(CIProducts.TransactionAbstract.Storage, pos.getStorageInst());
            transInsert.add(CIProducts.TransactionAbstract.Product, pos.getProdInstance());
            transInsert.add(CIProducts.TransactionAbstract.UoM, pos.getUoM());
            transInsert.add(CIProducts.TransactionAbstract.Description, pos.getTransactionDescr());
            transInsert.add(CIProducts.TransactionAbstract.Date, pos.getDate());
            transInsert.add(CIProducts.TransactionAbstract.Document, pos.getDocInst());
            transInsert.executeWithoutAccessCheck();
        }
    }

    protected List<Position> getPositions4Document(final Parameter _parameter,
                                                   final Instance _docInst)
        throws EFapsException
    {
        final List<Position> ret = new ArrayList<>();
        final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.StockProductAbstract);

        final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionAbstract);
        queryBldr.addWhereAttrEqValue(CISales.PositionAbstract.DocumentAbstractLink, _docInst);
        queryBldr.addWhereAttrInQuery(CISales.PositionAbstract.Product,
                        attrQueryBldr.getAttributeQuery(CIProducts.StockProductAbstract.ID));
        queryBldr.addOrderByAttributeAsc(CISales.PositionAbstract.PositionNumber);
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selProdInst = SelectBuilder.get().linkto(CISales.PositionAbstract.Product)
                        .instance();
        final SelectBuilder selProdName = SelectBuilder.get().linkto(CISales.PositionAbstract.Product)
                        .attribute(CIProducts.ProductAbstract.Name);
        multi.addSelect(selProdInst, selProdName);
        multi.addAttribute(CISales.PositionAbstract.Quantity, CISales.PositionAbstract.ProductDesc,
                        CISales.PositionAbstract.UoM);
        multi.setEnforceSorted(true);
        multi.execute();
        while (multi.next()) {
            ret.add(new Position(multi.getCurrentInstance())
                            .setProdInstance(multi.<Instance>getSelect(selProdInst))
                            .setProdName(multi.<String>getSelect(selProdName))
                            .setDescr(multi.<String>getAttribute(CISales.PositionAbstract.ProductDesc))
                            .setQuantity(multi.<BigDecimal>getAttribute(CISales.PositionAbstract.Quantity))
                            .setUoM(Dimension.getUoM(multi.<Long>getAttribute(CISales.PositionAbstract.UoM))));
        }
        return ret;
    }


    public static class Position
    {
        private final Instance instance;
        private Instance prodInstance;
        private String prodName;
        private BigDecimal quantity;
        private String descr;
        private UoM uoM;
        private Instance storageInst;
        private Instance docInst;

        private DateTime date;
        private String transactionDescr;


        /**
         * @param _currentInstance
         */
        public Position(final Instance _instance)
        {
            this.instance = _instance;
        }

        public Type getTransactionType()
        {
            Type ret = null;
            if (getInstance().getType().isCIType(CISales.TransactionDocumentShadowInPosition)) {
                if (isIndiviudal()) {
                    ret = CIProducts.TransactionIndividualInbound.getType();
                } else {
                    ret = CIProducts.TransactionInbound.getType();
                }
            }
            return ret;
        }

        public boolean isIndiviudal()
        {
             return getProdInstance().getType().isKindOf(CIProducts.ProductIndividualAbstract);
        }

        public String getTransactionDescr()
        {
            return this.transactionDescr;
        }

        /**
         * Getter method for the instance variable {@link #prodName}.
         *
         * @return value of instance variable {@link #prodName}
         */
        public String getProdName()
        {
            return this.prodName;
        }

        /**
         * Setter method for instance variable {@link #prodName}.
         *
         * @param _prodName value for instance variable {@link #prodName}
         */
        public Position setProdName(final String _prodName)
        {
            this.prodName = _prodName;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #quantity}.
         *
         * @return value of instance variable {@link #quantity}
         */
        public BigDecimal getQuantity()
        {
            return this.quantity;
        }

        /**
         * Setter method for instance variable {@link #quantity}.
         *
         * @param _quantity value for instance variable {@link #quantity}
         */
        public Position setQuantity(final BigDecimal _quantity)
        {
            this.quantity = _quantity;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #descr}.
         *
         * @return value of instance variable {@link #descr}
         */
        public String getDescr()
        {
            return this.descr;
        }

        /**
         * Setter method for instance variable {@link #descr}.
         *
         * @param _descr value for instance variable {@link #descr}
         */
        public Position setDescr(final String _descr)
        {
            this.descr = _descr;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #uoM}.
         *
         * @return value of instance variable {@link #uoM}
         */
        public UoM getUoM()
        {
            return this.uoM;
        }

        /**
         * Setter method for instance variable {@link #uoM}.
         *
         * @param _uoM value for instance variable {@link #uoM}
         */
        public Position setUoM(final UoM _uoM)
        {
            this.uoM = _uoM;
            return this;
        }


        /**
         * Getter method for the instance variable {@link #instance}.
         *
         * @return value of instance variable {@link #instance}
         */
        public Instance getInstance()
        {
            return this.instance;
        }

        /**
         * Getter method for the instance variable {@link #prodInstance}.
         *
         * @return value of instance variable {@link #prodInstance}
         */
        public Instance getProdInstance()
        {
            return this.prodInstance;
        }

        /**
         * Setter method for instance variable {@link #prodInstance}.
         *
         * @param _prodInstance value for instance variable {@link #prodInstance}
         */
        public Position setProdInstance(final Instance _prodInstance)
        {
            this.prodInstance = _prodInstance;
            return this;
        }


        /**
         * Getter method for the instance variable {@link #storageInst}.
         *
         * @return value of instance variable {@link #storageInst}
         */
        public Instance getStorageInst()
        {
            return this.storageInst;
        }


        /**
         * Setter method for instance variable {@link #storageInst}.
         *
         * @param _storageInst value for instance variable {@link #storageInst}
         */
        public Position setStorageInst(final Instance _storageInst)
        {
            this.storageInst = _storageInst;
            return this;
        }


        /**
         * Getter method for the instance variable {@link #date}.
         *
         * @return value of instance variable {@link #date}
         */
        public DateTime getDate()
        {
            return this.date;
        }


        /**
         * Setter method for instance variable {@link #date}.
         *
         * @param _date value for instance variable {@link #date}
         */
        public Position setDate(final DateTime _date)
        {
            this.date = _date;
            return this;
        }


        /**
         * Getter method for the instance variable {@link #docInst}.
         *
         * @return value of instance variable {@link #docInst}
         */
        public Instance getDocInst()
        {
            return this.docInst;
        }


        /**
         * Setter method for instance variable {@link #docInst}.
         *
         * @param _docInst value for instance variable {@link #docInst}
         */
        public Position setDocInst(final Instance _docInst)
        {
            this.docInst = _docInst;
            return this;
        }


        /**
         * Setter method for instance variable {@link #transactionDescr}.
         *
         * @param _transactionDescr value for instance variable {@link #transactionDescr}
         */
        public Position setTransactionDescr(final String _transactionDescr)
        {
            this.transactionDescr = _transactionDescr;
            return this;
        }
    }

}
