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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.efaps.admin.datamodel.Attribute;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.IUIValue;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.Listener;
import org.efaps.admin.ui.AbstractCommand;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.admin.ui.field.Field.Display;
import org.efaps.admin.ui.field.FieldTable;
import org.efaps.ci.CIType;
import org.efaps.db.CachedPrintQuery;
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
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.erp.listener.IOnCreateDocument;
import org.efaps.esjp.products.Batch;
import org.efaps.esjp.products.Product;
import org.efaps.esjp.products.Product_Base;
import org.efaps.esjp.products.Storage;
import org.efaps.esjp.products.util.Products;
import org.efaps.esjp.products.util.Products.ProductIndividual;
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
@EFapsApplication("eFapsApp-Sales")
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
        final String remark = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentStockAbstract.Remark.name));
        if (remark != null) {
            insert.add(CISales.DocumentStockAbstract.Remark, remark);
            createdDoc.getValues().put(CISales.DocumentStockAbstract.Remark.name, remark);
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
            final Type type = getType4PositionCreate(_parameter);
            final Insert posIns = new Insert(type);

            posIns.add(CISales.PositionAbstract.PositionNumber, i + 1);
            posIns.add(CISales.PositionAbstract.DocumentAbstractLink, _createdDoc.getInstance());

            String individualName = null;
            final String[] product = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                            CISales.PositionAbstract.Product.name));
            if (product != null && product.length > i) {
                Instance inst = Instance.get(product[i]);
                if (Products.ACTIVATEINDIVIDUAL.get()) {
                    if (inst.getType().isKindOf(CIProducts.ProductIndividualAbstract.getType())) {
                        final PrintQuery print = new PrintQuery(inst);
                        final SelectBuilder sel = SelectBuilder.get()
                                        .linkfrom(CIProducts.StoreableProductAbstract2IndividualAbstract,
                                                        CIProducts.StoreableProductAbstract2IndividualAbstract.ToAbstract)
                                        .linkto(CIProducts.StoreableProductAbstract2IndividualAbstract.FromAbstract)
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

            final String[] remarks = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                            CISales.PositionAbstract.Remark.name));
            if (remarks != null && remarks.length > i) {
                posIns.add(CISales.PositionAbstract.Remark, remarks[i]);
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

            if (type.isKindOf(CISales.PositionProdDocAbstract)) {
                final String[] storages = _parameter.getParameterValues("storage");
                if (ArrayUtils.isNotEmpty(storages)) {
                    final Instance storageInst = Instance.get(storages[i]);
                    if (storageInst.isValid()) {
                        posIns.add(CISales.PositionProdDocAbstract.StorageLink, storageInst);
                    }
                }
            }
            add2PositionCreate(_parameter, posIns, _createdDoc, i);

            posIns.execute();
            if (individualName != null && !type.isKindOf(CISales.PositionProdDocAbstract)) {
                final CIType transType;
                if (posIns.getInstance().getType().isKindOf(CISales.ReturnSlipPosition)
                                || posIns.getInstance().getType().isKindOf(CISales.RecievingTicketPosition)) {
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

    @Override
    protected void add2PositionUpdate(final Parameter _parameter,
                                      final Calculator _calc,
                                      final Update _posUpdate,
                                      final int _idx)
        throws EFapsException
    {
        super.add2PositionUpdate(_parameter, _calc, _posUpdate, _idx);
        final String[] product = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                        CISales.PositionAbstract.Product.name));
        if (product != null && product.length > _idx) {
            final Instance prodInst = Instance.get(product[_idx]);
            if (prodInst.isValid()) {
                _posUpdate.add(CISales.PositionAbstract.Product, prodInst);
            }
        }
        if (_posUpdate.getInstance().getType().isKindOf(CISales.PositionProdDocAbstract)) {
            final String[] storages = _parameter.getParameterValues("storage");
            if (ArrayUtils.isNotEmpty(storages)) {
                final Instance storageInst = Instance.get(storages[_idx]);
                if (storageInst.isValid()) {
                    _posUpdate.add(CISales.PositionProdDocAbstract.StorageLink, storageInst);
                }
            }
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
        if (Products.ACTIVATEINDIVIDUAL.get()) {

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

                            final Type prodType;
                            final Type relType;
                            final boolean clazz;
                            final Object quantity;
                            if (ProductIndividual.BATCH.equals(prIn)) {
                                prodType = CIProducts.ProductBatch.getType();
                                relType = CIProducts.StoreableProductAbstract2Batch.getType();
                                clazz = false;
                                quantity = multi.getAttribute(CIProducts.TransactionInOutAbstract.Quantity);
                            } else {
                                prodType = CIProducts.ProductIndividual.getType();
                                relType = CIProducts.StoreableProductAbstract2Individual.getType();
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
        final List<Map<String, Object>> values = new ArrayList<>();
        final Instance storage = Instance.get(_parameter.getParameterValue("storageSetter"));
        if (storage.isValid() && rows != null) {
            for (final String row : rows) {
                final Map<String, Object> map2 = new HashMap<>();
                final Instance prod = Instance.get(row);
                if (prod != null && prod.isValid()) {
                    map2.put("quantityInStock", getStock4ProductInStorage(_parameter, prod, storage));
                    map2.put("storage", storage.getOid());
                }
                values.add(map2);
            }
            js.append(getSetFieldValuesScript(_parameter, values, null))
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
     * Method to render a drop-down field containing all warehouses.
     *
     * @param _parameter Parameter as passed from eFaps.
     * @return Return containing a SNIPPLET.
     * @throws EFapsException on error.
     */
    public Return getStorageFieldValueUI(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret;
        final IUIValue value = (IUIValue) _parameter.get(ParameterValues.UIOBJECT);
        if (Display.EDITABLE.equals(value.getDisplay())) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> props = (Map<String, Object>) _parameter.get(ParameterValues.PROPERTIES);
            if (!containsProperty(_parameter, "Type")) {
                props.put("Type", CIProducts.DynamicStorage.getType().getName());
            }
            if (!containsProperty(_parameter, "Select")) {
                props.put("Select", "attribute[" + CIProducts.StorageAbstract.Name.name + "]");
            }

            final org.efaps.esjp.common.uiform.Field field = new org.efaps.esjp.common.uiform.Field()
            {

                @Override
                protected void updatePositionList(final Parameter _parameter,
                                                  final List<DropDownPosition> _values)
                    throws EFapsException
                {
                    if (!(TargetMode.EDIT.equals(_parameter.get(ParameterValues.ACCESSMODE)) && "true".equalsIgnoreCase(
                                    getProperty(_parameter, "SetSelected")))) {
                        final Instance inst = getDefaultStorage(_parameter);
                        if (inst.isValid()) {
                            for (final DropDownPosition value : _values) {
                                if (value.getValue().equals(inst.getId()) || value.getValue().equals(inst.getOid())) {
                                    value.setSelected(true);
                                    break;
                                }
                            }
                        }
                    }
                }

                @Override
                protected void add2QueryBuilder4List(final Parameter _parameter,
                                                     final QueryBuilder _queryBldr)
                    throws EFapsException
                {
                    super.add2QueryBuilder4List(_parameter, _queryBldr);
                    _queryBldr.addWhereAttrEqValue(CIProducts.StorageAbstract.StatusAbstract, Status.find(
                                    CIProducts.StorageAbstractStatus.Active));
                }
            };
            ret = field.getOptionListFieldValue(_parameter);
        } else {
            ret = new Return();
        }
        return ret;
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
    public Return getJavaScriptUIValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        retVal.put(ReturnValues.SNIPLETT,
                        InterfaceUtils.wrappInScriptTag(_parameter, getJavaScript4SelectDoc(_parameter)
                                        + getJavaScript4Doc(_parameter) + getJavaScript4SelectedItems(_parameter),
                                        true, 1500));
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API
     * @return a Javascript for selected items
     * @throws EFapsException on error
     */
    protected StringBuilder getJavaScript4SelectedItems(final Parameter _parameter)
        throws EFapsException
    {
        final StringBuilder js = new StringBuilder();
        final List<Instance> instances = getSelectedInstances(_parameter);
        if (!instances.isEmpty()) {
            final Type type = instances.get(0).getType();
            if (type.isKindOf(CIProducts.InventoryAbstract)) {
                final Collection<Map<String, Object>> values = new ArrayList<>();
                final MultiPrintQuery multi = new MultiPrintQuery(instances);
                final SelectBuilder selProd = SelectBuilder.get().linkto(CIProducts.InventoryAbstract.Product);
                final SelectBuilder selProdInst = new SelectBuilder(selProd).instance();
                final SelectBuilder selProdName = new SelectBuilder(selProd).attribute(CIProducts.ProductAbstract.Name);
                final SelectBuilder selProdDescr = new SelectBuilder(selProd)
                                .attribute(CIProducts.ProductAbstract.Description);
                final SelectBuilder selProdDim = new SelectBuilder(selProd)
                                .attribute(CIProducts.ProductAbstract.Dimension);
                final SelectBuilder selProdDefUoM = new SelectBuilder(selProd)
                                .attribute(CIProducts.ProductAbstract.DefaultUoM);
                final SelectBuilder selStorageInst = SelectBuilder.get().linkto(CIProducts.InventoryAbstract.Storage)
                                .instance();
                multi.addSelect(selProdInst, selProdName, selProdDescr, selProdDim, selProdDefUoM, selStorageInst);
                multi.addAttribute(CIProducts.InventoryAbstract.Quantity);

                multi.execute();

                NumberFormatter.get().getFrmt4Quantity(getTypeName4SysConf(_parameter));

                while (multi.next()) {
                    final Instance prodInst = multi.<Instance>getSelect(selProdInst);

                    final Map<String, Object> map = new HashMap<>();
                    map.put("product", new String[] { prodInst.getOid(),
                                    multi.<String>getSelect(selProdName) });
                    map.put("productDesc", multi.<String>getSelect(selProdDescr));
                    final Dimension dim = Dimension.get(multi.<Long>getSelect(selProdDim));
                    final Long defUoMId = multi.<Long>getSelect(selProdDefUoM);
                    final Long uomId;
                    if (defUoMId != null) {
                        final UoM uom = Dimension.getUoM(defUoMId);
                        if (uom.getNumerator() == 1 && uom.getDenominator() == 1) {
                            uomId = uom.getId();
                        } else {
                            uomId = dim.getBaseUoM().getId();
                        }
                    } else {
                        uomId = dim.getBaseUoM().getId();
                    }
                    map.put("uoM", getUoMFieldStr(uomId, dim.getId()));
                    map.put("quantity",
                                    multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.Quantity));

                    final Instance storageInst = multi.getSelect(selStorageInst);
                    final String quantityInStock = getStock4ProductInStorage(_parameter, prodInst, storageInst);
                    map.put("quantityInStock", quantityInStock);
                    map.put("storage", storageInst.getOid());
                    values.add(map);
                }

                final Set<String> noEscape = new HashSet<String>();
                noEscape.add("uoM");

                js.append(getTableRemoveScript(_parameter, "positionTable", false, false))
                                .append(getTableAddNewRowsScript(_parameter, "positionTable", values,
                                                getOnCompleteScript(_parameter), false, false, noEscape));
            }
        }
        return js;
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

        if (_productinst.getType().isCIType(CIProducts.ProductInfinite)) {
            ret.append("\u221e");
        } else {
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
        }
        return ret.toString();
    }

    @Override
    protected List<AbstractUIPosition> updateBean4Indiviual(final Parameter _parameter,
                                                            final AbstractUIPosition _bean)
        throws EFapsException
    {
        final List<AbstractUIPosition> ret = new ArrayList<>();
        if (isUpdateBean4Individual(_parameter, _bean)) {

            if (Products.ACTIVATEINDIVIDUAL.get()) {
                final PrintQuery print = new CachedPrintQuery(_bean.getProdInstance(), Product_Base.CACHEKEY4PRODUCT);
                print.addAttribute(CIProducts.ProductAbstract.Individual);
                print.execute();
                final ProductIndividual indivual = print.<ProductIndividual>getAttribute(
                                CIProducts.ProductAbstract.Individual);
                switch (indivual) {
                    case BATCH:
                        final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.StoreableProductAbstract2Batch);
                        attrQueryBldr.addWhereAttrEqValue(CIProducts.StoreableProductAbstract2Batch.FromLink,
                                        _bean.getProdInstance());
                        final QueryBuilder invQueryBldr = new QueryBuilder(CIProducts.InventoryIndividual);
                        final Instance storInst;
                        if (_bean.getStorageInst() != null) {
                            storInst = _bean.getStorageInst();
                        } else {
                            storInst = getDefaultStorage(_parameter);
                        }

                        if (storInst != null && storInst.isValid()) {
                            invQueryBldr.addWhereAttrEqValue(CIProducts.InventoryIndividual.Storage, storInst);
                        }
                        invQueryBldr.addWhereAttrInQuery(CIProducts.InventoryIndividual.Product,
                                        attrQueryBldr.getAttributeQuery(CIProducts.StoreableProductAbstract2Batch.ToLink));
                        final MultiPrintQuery multi = invQueryBldr.getPrint();
                        final SelectBuilder selProd = SelectBuilder.get()
                                        .linkto(CIProducts.InventoryIndividual.Product);
                        final SelectBuilder selProdInst = new SelectBuilder(selProd).instance();
                        final SelectBuilder selProdName = new SelectBuilder(selProd)
                                        .attribute(CIProducts.ProductAbstract.Name);
                        final SelectBuilder selProdDescr = new SelectBuilder(selProd)
                                        .attribute(CIProducts.ProductAbstract.Description);
                        final SelectBuilder selProdCreated = new SelectBuilder(selProd)
                                        .attribute(CIProducts.ProductAbstract.Created);
                        multi.addSelect(selProdInst, selProdName, selProdDescr, selProdCreated);
                        multi.addAttribute(CIProducts.InventoryIndividual.Quantity);
                        multi.execute();

                        // FIFO
                        final Map<DateTime, AbstractUIPosition> fifoMap = new TreeMap<>();
                        while (multi.next()) {
                            final Instance prodInst = multi.getSelect(selProdInst);
                            _bean.setDoc(null);
                            final AbstractUIPosition bean = SerializationUtils.clone(_bean);
                            bean.setProdInstance(prodInst)
                                            .setProdName(multi.<String>getSelect(selProdName))
                                            .setProdDescr(multi.<String>getSelect(selProdDescr))
                                            .setQuantity(multi.<BigDecimal>getAttribute(
                                                            CIProducts.InventoryIndividual.Quantity));
                            bean.setDoc(this);
                            fifoMap.put(multi.<DateTime>getSelect(selProdCreated), bean);
                        }

                        BigDecimal currentQty = _bean.getQuantity();
                        boolean complete = false;
                        for (final AbstractUIPosition tmpPos : fifoMap.values()) {
                            ret.add(tmpPos);
                            if (currentQty.compareTo(tmpPos.getQuantity()) < 1) {
                                tmpPos.setQuantity(currentQty);
                                complete = true;
                                break;
                            } else {
                                currentQty = currentQty.subtract(tmpPos.getQuantity());
                            }
                        }
                        if (!complete && currentQty.compareTo(BigDecimal.ZERO) > 0) {
                            // to be able to serialize, ensure that the document is not set
                            final AbstractDocument_Base docTmp = _bean.getDoc();
                            _bean.setDoc(null);
                            final AbstractUIPosition bean = SerializationUtils.clone(_bean);
                            bean.setDoc(this)
                                .setProdInstance(Instance.get(""))
                                .setProdName("NOSTOCK")
                                .setProdDescr(multi.<String>getSelect(selProdDescr))
                                .setQuantity(currentQty);
                            ret.add(bean);
                            // back to original state
                            _bean.setDoc(docTmp);
                        }
                        break;
                    default:
                        ret.add(_bean);
                        break;
                }
            } else {
                ret.add(_bean);
            }
        } else {
            ret.addAll(super.updateBean4Indiviual(_parameter, _bean));
        }
        return ret;
    }

    /**
     * Checks if is update bean4 individual.
     *
     * @param _parameter the _parameter
     * @param _bean the _bean
     * @return true, if is update bean4 individual
     */
    protected boolean isUpdateBean4Individual(final Parameter _parameter,
                                              final AbstractUIPosition _bean)
    {
        return true;
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

        // validate that the product is a product that can have stock
        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.StoreableProductAbstract);
        queryBldr.addWhereAttrEqValue(CIProducts.StoreableProductAbstract.ID, productID[0]);

        if (!queryBldr.getQuery().executeWithoutAccessCheck().isEmpty()) {
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
    public Return createTransaction4Document(final Parameter _parameter)
        throws EFapsException
    {
        final QueryBuilder posQueryBldr = new QueryBuilder(CISales.PositionProdDocAbstract);
        posQueryBldr.addWhereAttrEqValue(CISales.PositionProdDocAbstract.DocumentAbstractLink, _parameter
                        .getInstance());
        final MultiPrintQuery posMulti = posQueryBldr.getPrint();
        final SelectBuilder selProdInst = SelectBuilder.get().linkto(CISales.PositionProdDocAbstract.Product)
                        .instance();
        final SelectBuilder selStorageInst = SelectBuilder.get().linkto(CISales.PositionProdDocAbstract.StorageLink)
                        .instance();
        posMulti.addSelect(selProdInst, selStorageInst);
        posMulti.addAttribute(CISales.PositionProdDocAbstract.Quantity, CISales.PositionProdDocAbstract.UoM);
        posMulti.execute();
        while (posMulti.next()) {
            Instance prodInst = posMulti.getSelect(selProdInst);
            final Instance storageInst = posMulti.getSelect(selStorageInst);

            if (InstanceUtils.isValid(prodInst) && !prodInst.getType().isCIType(CIProducts.ProductInfinite)
                            && prodInst.getType().isKindOf(CIProducts.StoreableProductAbstract)
                            && InstanceUtils.isValid(storageInst)) {

                final CIType ciType;
                final CIType ciType4individual;
                if (posMulti.getCurrentInstance().getType().isCIType(CISales.GoodsIssueSlipPosition)) {
                    ciType = CIProducts.TransactionOutbound;
                    ciType4individual = CIProducts.TransactionIndividualOutbound;
                } else {
                    ciType = CIProducts.TransactionInbound;
                    ciType4individual = CIProducts.TransactionIndividualInbound;
                }

                if (Products.ACTIVATEINDIVIDUAL.get()
                                && prodInst.getType().isKindOf(CIProducts.ProductIndividualAbstract)) {
                    final Insert insert = new Insert(ciType4individual);
                    insert.add(CIProducts.TransactionAbstract.Quantity,
                                    posMulti.<BigDecimal>getAttribute(CISales.PositionProdDocAbstract.Quantity));
                    insert.add(CIProducts.TransactionAbstract.Storage, storageInst);
                    insert.add(CIProducts.TransactionAbstract.Product, prodInst);
                    insert.add(CIProducts.TransactionAbstract.Description, getDescription4PositionTrigger(_parameter));
                    insert.add(CIProducts.TransactionAbstract.Date, new DateTime());
                    insert.add(CIProducts.TransactionAbstract.Document, _parameter.getInstance());
                    insert.add(CIProducts.TransactionAbstract.UoM,
                                    posMulti.<Object>getAttribute(CISales.PositionProdDocAbstract.UoM));
                    insert.executeWithoutAccessCheck();

                    prodInst = new Product().getProduct4Individual(_parameter, prodInst);
                }

                final Insert insert = new Insert(ciType);
                insert.add(CIProducts.TransactionAbstract.Quantity,
                                posMulti.<BigDecimal>getAttribute(CISales.PositionProdDocAbstract.Quantity));
                insert.add(CIProducts.TransactionAbstract.Storage, storageInst);
                insert.add(CIProducts.TransactionAbstract.Product, prodInst);
                insert.add(CIProducts.TransactionAbstract.Description, getDescription4PositionTrigger(_parameter));
                insert.add(CIProducts.TransactionAbstract.Date, new DateTime());
                insert.add(CIProducts.TransactionAbstract.Document, _parameter.getInstance());
                insert.add(CIProducts.TransactionAbstract.UoM,
                                posMulti.<Object>getAttribute(CISales.PositionProdDocAbstract.UoM));
                insert.executeWithoutAccessCheck();
            }
        }
        return new Return();
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
            // ensure that all transaction are evaluated
            multi.executeWithoutAccessCheck();
            while (multi.next()) {
                final Insert insert;
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
                                multi.<BigDecimal>getAttribute(CIProducts.TransactionAbstract.Quantity));
                insert.add(CIProducts.TransactionAbstract.Storage,
                                multi.<Long>getAttribute(CIProducts.TransactionAbstract.Storage));
                insert.add(CIProducts.TransactionAbstract.Product,
                                multi.<Long>getAttribute(CIProducts.TransactionAbstract.Product));
                insert.add(CIProducts.TransactionAbstract.Description,
                                multi.<String>getAttribute(CIProducts.TransactionAbstract.Description));
                insert.add(CIProducts.TransactionAbstract.Date,
                                multi.<DateTime>getAttribute(CIProducts.TransactionAbstract.Date));
                insert.add(CIProducts.TransactionAbstract.Document, instance);
                insert.add(CIProducts.TransactionAbstract.UoM,
                                multi.<Long>getAttribute(CIProducts.TransactionAbstract.UoM));
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
     * Gets the description4 product transaction.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst the doc inst
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

    /**
     * Creates the product transaction.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _positions the positions
     * @throws EFapsException on error
     */
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

    /**
     * Gets the positions4 document.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst the doc inst
     * @return the positions4 document
     * @throws EFapsException on error
     */
    protected List<Position> getPositions4Document(final Parameter _parameter,
                                                   final Instance _docInst)
        throws EFapsException
    {
        final List<Position> ret = new ArrayList<>();
        final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.StoreableProductAbstract);

        final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionAbstract);
        queryBldr.addWhereAttrEqValue(CISales.PositionAbstract.DocumentAbstractLink, _docInst);
        queryBldr.addWhereAttrInQuery(CISales.PositionAbstract.Product,
                        attrQueryBldr.getAttributeQuery(CIProducts.StoreableProductAbstract.ID));
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

    @Override
    protected AbstractUIPosition getUIPosition(final Parameter _parameter)
    {
        return new UIProductDocumentPosition(this);
    }

    /**
     * The Class UIProductDocumentPosition.
     *
     * @author The eFaps Team
     */
    public static class UIProductDocumentPosition
        extends AbstractUIPosition
    {

        /** */
        private static final long serialVersionUID = 1L;

        /**
         * Instantiates a new UI product document position.
         *
         * @param _doc the _doc
         */
        public UIProductDocumentPosition(final AbstractDocument_Base _doc)
        {
            super(_doc);
        }

        @Override
        public Map<String, Object> getMap4JavaScript(final Parameter _parameter)
            throws EFapsException
        {
            final Map<String, Object> ret = super.getMap4JavaScript(_parameter);
            final AbstractProductDocument_Base tmpDoc;
            if (getDoc() instanceof AbstractProductDocument_Base) {
                tmpDoc = (AbstractProductDocument_Base) getDoc();
            } else {
                tmpDoc = new AbstractProductDocument()
                {
                };
            }

            final Instance storInst;
            if (getStorageInst() != null) {
                storInst = getStorageInst();
            } else {
                storInst = tmpDoc.getDefaultStorage(_parameter);
            }

            if (storInst.isValid()) {
                if (getProdInstance().isValid()) {
                    final String quantityInStock = tmpDoc.getStock4ProductInStorage(_parameter, getProdInstance(),
                                    storInst);
                    ret.put("quantityInStock", quantityInStock);
                    ret.put("storage", storInst.getOid());
                }
            }
            return ret;
        }
    }

    /**
     * The Class Position.
     */
    public static class Position
    {

        /** The instance. */
        private final Instance instance;

        /** The prod instance. */
        private Instance prodInstance;

        /** The prod name. */
        private String prodName;

        /** The quantity. */
        private BigDecimal quantity;

        /** The descr. */
        private String descr;

        /** The uo m. */
        private UoM uoM;

        /** The storage inst. */
        private Instance storageInst;

        /** The doc inst. */
        private Instance docInst;

        /** The date. */
        private DateTime date;

        /** The transaction descr. */
        private String transactionDescr;


        /**
         * Instantiates a new position.
         *
         * @param _instance the instance
         */
        public Position(final Instance _instance)
        {
            this.instance = _instance;
        }

        /**
         * Gets the transaction type.
         *
         * @return the transaction type
         */
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

        /**
         * Checks if is indiviudal.
         *
         * @return true, if is indiviudal
         */
        public boolean isIndiviudal()
        {
            return getProdInstance().getType().isKindOf(CIProducts.ProductIndividualAbstract);
        }

        /**
         * Gets the transaction descr.
         *
         * @return the transaction descr
         */
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
         * @return the position
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
         * @return the position
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
         * @return the position
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
         * @return the position
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
         * @return the position
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
         * @return the position
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
         * @return the position
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
         * @return the position
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
         * @return the position
         */
        public Position setTransactionDescr(final String _transactionDescr)
        {
            this.transactionDescr = _transactionDescr;
            return this;
        }
    }
}
