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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
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
import org.efaps.esjp.products.util.Products;
import org.efaps.esjp.products.util.Products.ProductIndividual;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.ui.html.Table;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
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
        final Map<Instance, Set<Instance>> prod2ind = getProduct2IndividualMap(_parameter);
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
                final Set<Instance> indInsts = prod2ind.get(pos.getProdInstance());
                final Iterator<Instance> individualIter = indInsts.iterator();
                int j = 0;
                while (individualIter.hasNext() && j < pos.getQuantity().intValue()) {
                    final Instance indInst = individualIter.next();
                    final PrintQuery indPrint = new PrintQuery(indInst);
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
                        final QueryBuilder attrQueryBldr = new QueryBuilder(
                                        CIProducts.StoreableProductAbstract2IndividualAbstract);
                        attrQueryBldr.addWhereAttrEqValue(
                                        CIProducts.StoreableProductAbstract2IndividualAbstract.FromAbstract,
                                        pos.getProdInstance());
                        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductAbstract);
                        queryBldr.addWhereAttrInQuery(CIProducts.ProductAbstract.ID, attrQueryBldr.getAttributeQuery(
                                        CIProducts.StoreableProductAbstract2IndividualAbstract.ToAbstract));
                        final List<Instance> prodInsts = queryBldr.getQuery().execute();
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
            final Map<Instance, Set<Instance>> map = getProduct2IndividualMap(_parameter);
            final Iterator<Position> iter = ret.iterator();
            while (iter.hasNext()) {
                final Position pos = iter.next();
                final PrintQuery print = new PrintQuery(pos.getProdInstance());
                print.addAttribute(CIProducts.ProductAbstract.Individual);
                print.execute();
                final ProductIndividual individual = print.getAttribute(CIProducts.ProductAbstract.Individual);
                switch (individual) {
                    case INDIVIDUAL:
                        replacement.add(pos);
                        final Set<Instance> individualInsts = map.get(pos.getProdInstance());
                        if (CollectionUtils.isNotEmpty(individualInsts)) {
                            final Iterator<Instance> individualIter = individualInsts.iterator();
                            int i = 0;
                            while (individualIter.hasNext() && i < pos.getQuantity().intValue()) {
                                final Instance indInst = individualIter.next();
                                final PrintQuery indPrint = new PrintQuery(indInst);
                                indPrint.addAttribute(CIProducts.ProductAbstract.Name);
                                indPrint.executeWithoutAccessCheck();
                                final String individualName = indPrint.getAttribute(CIProducts.ProductAbstract.Name);
                                replacement.add(new Position(pos.getInstance())
                                                    .setProdInstance(indInst)
                                                    .setProdName(individualName)
                                                    .setDescr(pos.getDescr())
                                                    .setQuantity(BigDecimal.ONE)
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
    protected Map<Instance, Set<Instance>> getProduct2IndividualMap(final Parameter _parameter)
        throws EFapsException
    {
        final Map<Instance, Set<Instance>> ret = new HashMap<>();
        final List<Instance> individuals = getInstances(_parameter, "individual");
        for (final Instance individual : individuals) {
            final Instance prodInst = new Product().getProduct4Individual(_parameter, individual);
            final Set<Instance> set;
            if (ret.containsKey(prodInst)) {
                set = ret.get(prodInst);
            } else {
                set = new HashSet<>();
                ret.put(prodInst, set);
            }
            set.add(individual);
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

}
