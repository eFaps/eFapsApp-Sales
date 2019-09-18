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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.admin.datamodel.StatusValue;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.common.util.InterfaceUtils_Base.DojoLibs;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.products.Inventory;
import org.efaps.esjp.products.Inventory_Base.InventoryBean;
import org.efaps.esjp.products.Product;
import org.efaps.esjp.products.Transaction_Base;
import org.efaps.esjp.products.util.Products;
import org.efaps.esjp.products.util.Products.ProductIndividual;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.ui.html.Table;
import org.efaps.util.EFapsException;
import org.efaps.util.RandomUtil;
import org.joda.time.DateTime;

/**
 * @author The eFaps Team
 */
@EFapsUUID("8f2539fd-4e71-4f5a-bab2-6329b3dcebbf")
@EFapsApplication("eFapsApp-Sales")
public abstract class TransactionDocument_Base
    extends AbstractProductDocument
{

    /**
     * Used to store the Revision in the Context.
     */
    protected static final String REVISIONKEY = TransactionDocument.class.getName() + ".RevisionKey";

    /**
     * Access check4 document shadow.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return accessCheck4DocumentShadow(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.TransactionDocumentShadowAbstract);
        attrQueryBldr.addWhereAttrNotEqValue(CISales.TransactionDocumentShadowAbstract.StatusAbstract,
                        Status.find(CISales.TransactionDocumentShadowInStatus.Canceled),
                        Status.find(CISales.TransactionDocumentShadowOutStatus.Canceled));

        final QueryBuilder queryBldr = new QueryBuilder(CIERP.Document2DocumentAbstract);
        queryBldr.addWhereAttrEqValue(CIERP.Document2DocumentAbstract.FromAbstractLink, _parameter.getInstance());
        queryBldr.addWhereAttrInQuery(CIERP.Document2DocumentAbstract.ToAbstractLink,
                        attrQueryBldr.getAttributeQuery(CISales.TransactionDocumentShadowAbstract.ID));
        final InstanceQuery query = queryBldr.getQuery();
        if (query.executeWithoutAccessCheck().isEmpty()) {
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    /**
     * Creates the document shadow.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the created doc
     * @throws EFapsException on error
     */
    public CreatedDoc createDocumentShadow(final Parameter _parameter)
        throws EFapsException
    {
        return createDocumentShadow(_parameter, _parameter.getInstance());
    }

    /**
     * Creates the document shadow.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst the doc inst
     * @return the created doc
     * @throws EFapsException on error
     */
    public CreatedDoc createDocumentShadow(final Parameter _parameter,
                                           final Instance _docInst)
        throws EFapsException
    {
        final CreatedDoc ret = new CreatedDoc();
        final PrintQuery print = new PrintQuery(_docInst);
        print.addAttribute(CIERP.DocumentAbstract.Name, CIERP.DocumentAbstract.Date,
                        CIERP.DocumentAbstract.Contact);
        print.execute();
        final String docName = print.<String>getAttribute(CIERP.DocumentAbstract.Name);
        final DateTime date = print.<DateTime>getAttribute(CIERP.DocumentAbstract.Date);
        final Insert insert = new Insert(getType4DocCreate(_parameter));
        insert.add(CIERP.DocumentAbstract.Name, docName);
        insert.add(CIERP.DocumentAbstract.Date, date);
        insert.add(CIERP.DocumentAbstract.Contact, print.<Long>getAttribute(CIERP.DocumentAbstract.Contact));
        ret.getValues().put(CISales.DocumentStockAbstract.Date.name, date);

        addStatus2DocCreate(_parameter, insert, ret);
        add2DocCreate(_parameter, insert, ret);
        insert.execute();

        ret.setInstance(insert.getInstance());

        connect2ProductDocumentType(_parameter, ret);

        final List<Position> positions = getPositions4Document(_parameter, _docInst);
        final Map<Instance, Set<IndividualWithQuantity>> prod2ind = getProduct2IndividualMap(_parameter);
        final List<String> prodOids = new ArrayList<>();
        int i = 1;
        for (final Position pos : positions) {
            final Insert posIns = new Insert(getType4PositionCreate(_parameter));
            posIns.add(CISales.PositionAbstract.PositionNumber, i);
            posIns.add(CISales.PositionAbstract.DocumentAbstractLink, ret.getInstance());
            posIns.add(CISales.PositionAbstract.UoM, pos.getUoM());
            posIns.add(CISales.PositionAbstract.Quantity, pos.getQuantity());
            String descr =  pos.getDescr();
            if (prod2ind.containsKey(pos.getProdInstance())) {
                final Set<IndividualWithQuantity> indInsts = prod2ind.get(pos.getProdInstance());
                final Iterator<IndividualWithQuantity> individualIter = indInsts.iterator();
                int j = 0;
                while (individualIter.hasNext() && j < pos.getQuantity().intValue()) {
                    final IndividualWithQuantity indInst = individualIter.next();
                    final PrintQuery indPrint = new PrintQuery(indInst.getIndividual());
                    indPrint.addAttribute(CIProducts.ProductAbstract.Name);
                    indPrint.executeWithoutAccessCheck();
                    final String individualName = indPrint.getAttribute(CIProducts.ProductAbstract.Name);
                    descr = descr + " - Nº: " + individualName;
                    individualIter.remove();
                    j++;
                }
            }
            posIns.add(CISales.PositionAbstract.ProductDesc, descr);
            posIns.add(CISales.PositionAbstract.Product, pos.getProdInstance());
            posIns.execute();
            i++;
            prodOids.add(pos.getProdInstance().getOid());
        }
        createProductTransaction4Document(_parameter, ret.getInstance(),
                        Instance.get(_parameter.getParameterValue("storage")),
                        _docInst.getType().getLabel(), docName);

        if ("true".equalsIgnoreCase(getProperty(_parameter, "CreateIndividuals", "false"))) {
            final Parameter parameter = ParameterUtil.clone(_parameter);
            ParameterUtil.setParameterValues(parameter, getFieldName4Attribute(parameter,
                            CISales.PositionAbstract.Product.name), prodOids.toArray(new String[prodOids.size()]));
            createIndividuals(parameter, ret);
        }
        return ret;
    }

    @Override
    protected void add2DocCreate(final Parameter _parameter,
                                 final Insert _insert,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        super.add2DocCreate(_parameter, _insert, _createdDoc);

        final Type type = getType4DocCreate(_parameter);
        final String seqKey;
        if (type.isCIType(CISales.TransactionDocumentShadowOut)) {
            seqKey = Sales.TRANSDOCSHADOWOUT_REVSEQ.get();
        } else {
            seqKey = Sales.TRANSDOCSHADOWIN_REVSEQ.get();
        }
        final NumberGenerator numgen = isUUID(seqKey)
                        ? NumberGenerator.get(UUID.fromString(seqKey))
                        : NumberGenerator.get(seqKey);
        if (numgen != null) {
            final String revision = numgen.getNextVal();
            _insert.add(CISales.DocumentAbstract.Revision, revision);
            Context.getThreadContext().setSessionAttribute(TransactionDocument.REVISIONKEY, revision);
        }
    }

    /**
     * Update fields 4 storage 4 doc shadow.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return updateFields4Storage4DocShadow(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Instance docInst = _parameter.getCallInstance();
        final Instance storageInst = Instance.get(_parameter.getParameterValue(
                        CIFormSales.Sales_Receipt_CreateTransDocShadowForm.storage.name));

        final List<Map<String, Object>> list = new ArrayList<>();
        final Map<String, Object> map = new HashMap<>();
        list.add(map);

        final StringBuilder js = new StringBuilder()
                        .append(" html.set(dom.byId(\"positionInfoTable\"),\"")
                        .append(StringEscapeUtils.escapeEcmaScript(getPositionInfoSnipplet(_parameter,
                                        docInst, storageInst).toString()))
                        .append("\");");

        InterfaceUtils.appendScript4FieldUpdate(map,
                        InterfaceUtils.wrapInDojoRequire(_parameter, js, DojoLibs.DOM, DojoLibs.HTML));
        ret.put(ReturnValues.VALUES, list);
        return ret;
    }

    /**
     * Gets the products4 doc shadow field value.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the products4 doc shadow field value
     * @throws EFapsException on error
     */
    public Return getProducts4DocShadowOutFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Instance docInst = _parameter.getCallInstance();
        final Return ret = new Return();
        final StringBuilder html = new StringBuilder()
                .append("<span id=\"positionInfoTable\">")
                .append(getPositionInfoSnipplet(_parameter, docInst, getDefaultStorage(_parameter)))
                .append("</span>");
        ret.put(ReturnValues.SNIPLETT, html.toString());
        return ret;
    }

    /**
     * Gets the products 4 doc shadow in field value.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the products 4 doc shadow in field value
     * @throws EFapsException on error
     */
    public Return getProducts4DocShadowInFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Instance docInst = _parameter.getCallInstance();
        final Return ret = new Return();
        final Table table = new Table();

        final List<Position> positions = getPositions4Document(_parameter, docInst);
        table.addRow()
                .addHeaderColumn(getDBProperty("Quantity"))
                .addHeaderColumn(getDBProperty("UoM"))
                .addHeaderColumn(getDBProperty("ProdName"))
                .addHeaderColumn(getDBProperty("ProdDescr"));

        if (Products.ACTIVATEINDIVIDUAL.get()) {
            table.addHeaderColumn(getDBProperty("Individual"));
        }

        for (final Position pos : positions) {
            final StringBuilder stockHtml = new StringBuilder();
            ProductIndividual individual = ProductIndividual.NONE;
            if (Products.ACTIVATEINDIVIDUAL.get()) {
                final PrintQuery print = new PrintQuery(pos.getProdInstance());
                print.addAttribute(CIProducts.ProductAbstract.Individual);
                print.execute();
                individual = print.getAttribute(CIProducts.ProductAbstract.Individual);
            }
            switch (individual) {
                case INDIVIDUAL:
                    for (int i = 0; i < pos.getQuantity().intValue(); i++) {
                        if (i > 0) {
                            stockHtml.append("<br/>");
                        }
                        stockHtml.append("<label>").append(i + 1).append(".</label>")
                            .append("<input name=\"").append(pos.getProdInstance().getOid()).append("\">");
                    }
                    break;
                default:
                    stockHtml.append("-");
                    break;
            }
            table.addRow()
                .addColumn(pos.getQuantity().toString())
                .addColumn(pos.getUoM().getName())
                .addColumn(pos.getProdName())
                .addColumn(pos.getDescr());

            if (Products.ACTIVATEINDIVIDUAL.get()) {
                table.addColumn(stockHtml);
            }
        }

        ret.put(ReturnValues.SNIPLETT, table.toHtml());
        return ret;
    }

    @Override
    public void createProductTransaction4Document(final Parameter _parameter,
                                                  final Instance _docInst,
                                                  final Instance _storageInst,
                                                  final Object... _args)
        throws EFapsException
    {
        final List<Position> positions = getPositions4Transaction(_parameter, _docInst);
        final String transDescr = getDescription4ProductTransaction(_parameter, _docInst, _args);
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
     * Gets the position info snipplet.
     *
     * TODO: make it work for Batch also
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst the doc inst
     * @param _storageInst the storage inst
     * @return the position info snipplet
     * @throws EFapsException on error
     */
    public CharSequence getPositionInfoSnipplet(final Parameter _parameter,
                                                final Instance _docInst,
                                                final Instance _storageInst)
        throws EFapsException
    {
        final Table table = new Table();
        final PrintQuery docPrint = new PrintQuery(_docInst);
        docPrint.addAttribute(CIERP.DocumentAbstract.Date);
        docPrint.execute();
        final DateTime date = docPrint.getAttribute(CIERP.DocumentAbstract.Date);

        final List<Position> positions = getPositions4Document(_parameter, _docInst);
        table.addRow()
                .addHeaderColumn(getDBProperty("Quantity"))
                .addHeaderColumn(getDBProperty("UoM"))
                .addHeaderColumn(getDBProperty("ProdName"))
                .addHeaderColumn(getDBProperty("ProdDescr"))
                .addHeaderColumn(getDBProperty("Stock"));
        for (final Position pos : positions) {
            if (InstanceUtils.isKindOf(pos.getProdInstance(), CIProducts.StoreableProductAbstract)) {
                final StringBuilder stockHtml = new StringBuilder();
                ProductIndividual individual = ProductIndividual.NONE;
                if (Products.ACTIVATEINDIVIDUAL.get()) {
                    final PrintQuery print = new PrintQuery(pos.getProdInstance());
                    print.addAttribute(CIProducts.ProductAbstract.Individual);
                    print.execute();
                    individual = print.getAttribute(CIProducts.ProductAbstract.Individual);
                }
                switch (individual) {
                    case INDIVIDUAL:
                        final List<Instance> prodInsts = getIndividuals(pos.getProdInstance());
                        if (prodInsts.isEmpty()) {
                            stockHtml.append(getDBProperty("NOINDIVIDUAL"));
                        } else {
                            final Map<Instance, InventoryBean> inventoryBeans = Inventory.getInventory4Products(
                                            _parameter, _storageInst, date,
                                            prodInsts.toArray(new Instance[prodInsts.size()]));
                            for (final InventoryBean inventoryBean : inventoryBeans.values()) {
                                if (stockHtml.length() > 0) {
                                    stockHtml.append("<br/>");
                                }
                                stockHtml.append("<label><input type=\"checkbox\" name=\"individual\" value=\"")
                                    .append(inventoryBean.getProdOID())
                                    .append("\">")
                                    .append(inventoryBean.getProdName()).append("</label>");
                            }
                        }
                        break;
                    case BATCH:
                        final List<Instance> batches = getIndividuals(pos.getProdInstance());
                        if (batches.isEmpty()) {
                            stockHtml.append(getDBProperty("NOINDIVIDUAL"));
                        } else {
                            final Map<Instance, InventoryBean> inventoryBeans = Inventory.getInventory4Products(
                                            _parameter, _storageInst, date,
                                            batches.toArray(new Instance[batches.size()]));
                            if (inventoryBeans.isEmpty()) {
                                stockHtml.append(getDBProperty("NOSTOCK"));
                            }
                            BigDecimal quantity = pos.getQuantity();
                            for (final InventoryBean inventoryBean : inventoryBeans.values()) {
                                if (stockHtml.length() > 0) {
                                    stockHtml.append("<br/>");
                                }
                                BigDecimal batchSize = BigDecimal.ZERO;
                                if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                                    if (inventoryBean.getQuantity().compareTo(quantity) > 0) {
                                        batchSize = quantity;
                                        quantity = BigDecimal.ZERO;
                                    } else {
                                        batchSize = inventoryBean.getQuantity();
                                        quantity = quantity.subtract(inventoryBean.getQuantity());
                                    }
                                }
                                final String inputId = RandomUtil.randomAlphabetic(5);
                                stockHtml.append("<label><input type=\"checkbox\" name=\"individual\" value=\"")
                                    .append(inventoryBean.getProdOID())
                                    .append("\" onclick=\"document.getElementById('")
                                    .append(inputId)
                                    .append("').disabled = !this.checked;\" >")
                                    .append(inventoryBean.getProdName()).append("</label>")
                                    .append("<input id=").append(inputId)
                                    .append(" type=\"number\" name=\"batchSize\" value=\"")
                                    .append(batchSize).append("\" disabled>");
                            }
                        }
                        break;
                    default:
                        final InventoryBean inventoryBean = Inventory.getInventory4Product(_parameter, _storageInst,
                                        date, pos.getProdInstance());
                        if (inventoryBean != null) {
                            stockHtml.append(NumberFormatter.get().getFormatter().format(inventoryBean.getQuantity()));
                        }
                        break;
                }
                table.addRow()
                    .addColumn(pos.getQuantity().toString())
                    .addColumn(pos.getUoM().getName())
                    .addColumn(pos.getProdName())
                    .addColumn(pos.getDescr())
                    .addColumn(stockHtml);
            }
        }
        return table.toHtml();
    }

    protected List<Instance> getIndividuals(final Instance _prodInst) throws EFapsException {
        final QueryBuilder attrQueryBldr = new QueryBuilder(
                        CIProducts.StoreableProductAbstract2IndividualAbstract);
        attrQueryBldr.addWhereAttrEqValue(CIProducts.StoreableProductAbstract2IndividualAbstract.FromAbstract,
                        _prodInst);
        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductAbstract);
        queryBldr.addWhereAttrInQuery(CIProducts.ProductAbstract.ID, attrQueryBldr.getAttributeQuery(
                        CIProducts.StoreableProductAbstract2IndividualAbstract.ToAbstract));
        return queryBldr.getQuery().execute();
    }

    /**
     * Gets the positions for the  transaction document.
     *
     * TODO: make it work for Batch also
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst the doc inst
     * @return the positions 4 trans doc
     * @throws EFapsException on error
     */
    protected List<Position> getPositions4Transaction(final Parameter _parameter,
                                                      final Instance _docInst)
        throws EFapsException
    {
        List<Position> ret = getPositions4Document(_parameter, _docInst);
        if (Products.ACTIVATEINDIVIDUAL.get()) {
            final List<Position> replacement = new ArrayList<>();
            final Map<Instance, Set<IndividualWithQuantity>> map = getProduct2IndividualMap(_parameter);
            final Iterator<Position> iter = ret.iterator();
            while (iter.hasNext()) {
                final Position pos = iter.next();
                final PrintQuery print = new PrintQuery(pos.getProdInstance());
                print.addAttribute(CIProducts.ProductAbstract.Individual);
                print.execute();
                final ProductIndividual individual = print.getAttribute(CIProducts.ProductAbstract.Individual);
                switch (individual) {
                    case INDIVIDUAL:
                    case BATCH:
                        replacement.add(pos);
                        final Set<IndividualWithQuantity> individualInsts = map.get(pos.getProdInstance());
                        if (CollectionUtils.isNotEmpty(individualInsts)) {
                            final Iterator<IndividualWithQuantity> individualIter = individualInsts.iterator();
                            int i = 0;
                            while (individualIter.hasNext() && i < pos.getQuantity().intValue()) {
                                final IndividualWithQuantity indInst = individualIter.next();
                                final PrintQuery indPrint = new PrintQuery(indInst.getIndividual());
                                indPrint.addAttribute(CIProducts.ProductAbstract.Name);
                                indPrint.executeWithoutAccessCheck();
                                final String individualName = indPrint.getAttribute(CIProducts.ProductAbstract.Name);
                                replacement.add(new Position(pos.getInstance())
                                                    .setProdInstance(indInst.getIndividual())
                                                    .setProdName(individualName)
                                                    .setDescr(pos.getDescr())
                                                    .setQuantity(indInst.getQuantity())
                                                    .setUoM(pos.getUoM()));
                                individualIter.remove();
                                i++;
                            }
                        }
                        break;
                    default:
                        replacement.add(pos);
                        break;
                }
            }
            ret = replacement;
        }
        return ret;
    }

    /**
     * Gets the product 2 individual map.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the product 2 individual map
     * @throws EFapsException on error
     */
    protected Map<Instance, Set<IndividualWithQuantity>> getProduct2IndividualMap(final Parameter _parameter)
        throws EFapsException
    {
        final Map<Instance, Set<IndividualWithQuantity>> ret = new HashMap<>();
        final List<Instance> individuals = getInstances(_parameter, "individual");
        final String[] quantities = _parameter.getParameterValues("batchSize");
        int idx = 0;
        for (final Instance individual : individuals) {
            final Instance prodInst = new Product().getProduct4Individual(_parameter, individual);
            final Set<IndividualWithQuantity> set;
            if (ret.containsKey(prodInst)) {
                set = ret.get(prodInst);
            } else {
                set = new HashSet<>();
                ret.put(prodInst, set);
            }
            final IndividualWithQuantity inq = new IndividualWithQuantity()
                            .setIndividual(individual)
                            .setQuantity(ArrayUtils.isNotEmpty(quantities)
                                            ? NumberFormatter.parse(quantities[idx]) : BigDecimal.ONE);
            set.add(inq);
            idx++;
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return with Snipplet
     * @throws EFapsException on error
     */
    public Return showRevisionFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        return getRevisionSequenceFieldValue(_parameter, TransactionDocument.REVISIONKEY);
    }

    public Instance createDocument(final Parameter _parameter,
                                   final CIType _type, final DateTime _date)
        throws EFapsException
    {
        final Insert insert = new Insert(_type);
        String seqKey;
        if (_type.equals(CISales.TransactionDocumentOut)) {
            seqKey = Sales.TRANSDOCOUT_SEQ.get();
        } else {
            seqKey = Sales.TRANSDOCIN_SEQ.get();
        }
        final NumberGenerator numgen = isUUID(seqKey)
                        ? NumberGenerator.get(UUID.fromString(seqKey))
                        : NumberGenerator.get(seqKey);
        if (numgen != null) {
            final String name = numgen.getNextVal();
            insert.add(CISales.DocumentAbstract.Name, name);

            String newName;
            final String currentName = (String) Context.getThreadContext().getSessionAttribute(Transaction_Base.NAMEKEY);
            if (StringUtils.isNotEmpty(currentName)) {
                newName = currentName + " -> " + name;
            } else {
                newName = name;
            }
            Context.getThreadContext().setSessionAttribute(Transaction_Base.NAMEKEY, newName);

        }
        insert.add(CISales.TransactionDocumentAbstract.Date, _date);
        insert.add(CISales.TransactionDocumentAbstract.StatusAbstract,
                        Status.find(CISales.TransactionDocumentStatus.Open));
        insert.execute();
        return insert.getInstance();
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new empty Return
     * @throws EFapsException on error
     */
    @Override
    public Return changeStatusWithInverseTransaction(final Parameter _parameter)
        throws EFapsException
    {
        final Instance docInstance = _parameter.getInstance();
        final boolean in = InstanceUtils.isKindOf(docInstance, CISales.TransactionDocumentIn);
        final boolean out = InstanceUtils.isKindOf(docInstance, CISales.TransactionDocumentOut);
        if (in || out) {
            final QueryBuilder queryBldr = new QueryBuilder(CISales.TransactionDocOut2In);
            queryBldr.addWhereAttrEqValue(in ? CISales.TransactionDocOut2In.ToLink : CISales.TransactionDocOut2In.FromLink, docInstance);
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selCounterpartInst = SelectBuilder.get()
                            .linkto(in ? CISales.TransactionDocOut2In.FromLink :  CISales.TransactionDocOut2In.ToLink).instance();
            final SelectBuilder selCounterpartStatus = SelectBuilder.get()
                            .linkto(in ? CISales.TransactionDocOut2In.FromLink :  CISales.TransactionDocOut2In.ToLink).status();
            multi.addSelect(selCounterpartInst, selCounterpartStatus);
            multi.execute();
            if (multi.next()) {
                final Status status = multi.getSelect(selCounterpartStatus);
                if (status.getKey() != CISales.TransactionDocumentStatus.Canceled.key) {
                    final Instance counterpartInst = multi.getSelect(selCounterpartInst);
                    final Parameter parameter = ParameterUtil.clone(_parameter, Parameter.ParameterValues.INSTANCE, counterpartInst);
                    new StatusValue().setStatus(parameter);
                    inverseTransaction(counterpartInst);
                }
            }
            new StatusValue().setStatus(_parameter);
            inverseTransaction(docInstance);
        } else {
            new StatusValue().setStatus(_parameter);
            inverseTransaction(docInstance);
        }
        return new Return();
    }

    public static class IndividualWithQuantity {

        /** The individual. */
        private Instance individual;

        /** The quantity. */
        private BigDecimal quantity;

        /**
         * Gets the individual.
         *
         * @return the individual
         */
        public Instance getIndividual()
        {
            return individual;
        }

        /**
         * Sets the individual.
         *
         * @param _individual the individual
         * @return the individual with quantity
         */
        public IndividualWithQuantity setIndividual(final Instance _individual)
        {
            individual = _individual;
            return this;
        }

        /**
         * Gets the quantity.
         *
         * @return the quantity
         */
        public BigDecimal getQuantity()
        {
            return quantity;
        }

        /**
         * Sets the quantity.
         *
         * @param _quantity the quantity
         * @return the individual with quantity
         */
        public IndividualWithQuantity setQuantity(final BigDecimal _quantity)
        {
            quantity = _quantity;
            return this;
        }
    }
}
