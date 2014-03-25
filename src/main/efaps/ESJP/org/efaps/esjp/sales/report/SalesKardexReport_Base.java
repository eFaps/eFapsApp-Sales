/*
 * Copyright 2003 - 2013 The eFaps Team
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

package org.efaps.esjp.sales.report;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperReport;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.EFapsMapDataSource;
import org.efaps.esjp.common.jasperreport.StandartReport;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.esjp.erp.util.ERPSettings;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("172d6272-0b49-4c2e-a004-24d90c719a98")
@EFapsRevision("$Rev$")
public abstract class SalesKardexReport_Base
    extends EFapsMapDataSource
{

    /**
     * Logger for this class.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(SalesKardexReport_Base.class);

    /**
     * Mapping for No es defined by SUNAT.
     */
    protected static final Map<Long, String> DOCTYPE_MAP = new HashMap<Long, String>();
    static {
        SalesKardexReport_Base.DOCTYPE_MAP.put(CISales.RecievingTicket.getType().getId(), "09");
        SalesKardexReport_Base.DOCTYPE_MAP.put(CISales.DeliveryNote.getType().getId(), "09");
        SalesKardexReport_Base.DOCTYPE_MAP.put(CISales.ReturnSlip.getType().getId(), "09");
        SalesKardexReport_Base.DOCTYPE_MAP.put(CISales.UsageReport.getType().getId(), "00");
        SalesKardexReport_Base.DOCTYPE_MAP.put(CISales.ReturnUsageReport.getType().getId(), "00");
        SalesKardexReport_Base.DOCTYPE_MAP.put(CISales.TransactionDocument.getType().getId(), "00");
    }

    /**
     * Mapping for No es defined by SUNAT.
     */
    protected static final Map<Long, String> EXISTTYPE_MAP = new HashMap<Long, String>();
    static {
        SalesKardexReport_Base.EXISTTYPE_MAP.put(CIProducts.ProductMaterial.getType().getId(),
                        "03 MATERIAS PRIMAS Y AUXILIARES - MATERIALES");
    }

    /**
     * Enum used to define the keys for the map.
     */
    public enum Field
    {
        /** */
        TRANS_DATE("transDate"),
        /** */
        TRANS_DOC_TYPE("transDocType"),
        /** */
        TRANS_DOC_SERIE("transDocSerie"),
        /** */
        TRANS_DOC_NUMBER("transDocNumber"),
        /** */
        TRANS_DOC_OPERATION("transDocOperation"),
        /** */
        TRANS_INBOUND_QUANTITY("transInboundQuantity"),
        /** */
        TRANS_INBOUND_COST("transInboundCost"),
        /** */
        TRANS_OUTBOUND_QUANTITY("transOutboundQuantity");

        /**
         * key.
         */
        private final String key;

        /**
         * @param _key key
         */
        private Field(final String _key)
        {
            this.key = _key;
        }

        /**
         * Getter method for the instance variable {@link #key}.
         *
         * @return value of instance variable {@link #key}
         */
        public String getKey()
        {
            return this.key;
        }
    }

    @Override
    public void init(final JasperReport _jasperReport,
                     final Parameter _parameter,
                     final JRDataSource _parentSource,
                     final Map<String, Object> _jrParameters)
        throws EFapsException
    {
        final List<Map<String, Object>> values = new ArrayList<Map<String, Object>>();

        final DateTime dateFrom = new DateTime(_parameter
                        .getParameterValue(CIFormSales.Sales_Products_Kardex_OfficialReportForm.dateFrom.name));

        final DateTime dateTo = new DateTime(_parameter
                        .getParameterValue(CIFormSales.Sales_Products_Kardex_OfficialReportForm.dateTo.name));

        final Instance product = Instance.get(_parameter
                        .getParameterValue(CIFormSales.Sales_Products_Kardex_OfficialReportForm.product.name));

        final Instance storage = Instance.get(_parameter
                        .getParameterValue(CIFormSales.Sales_Products_Kardex_OfficialReportForm.storage.name));

        if (product.isValid()) {
            final List<Long> listStorage = getStorage4List(storage);
            values.add(getInventoryValue(_parameter, product, listStorage, dateFrom));

            final SelectBuilder selTrans = new SelectBuilder().linkto(CIProducts.TransactionInOutAbstract.Document);
            final SelectBuilder selTransDocInst = new SelectBuilder(selTrans).instance();
            final SelectBuilder selTransDocType = new SelectBuilder(selTrans).type();
            final SelectBuilder selTransDocName = new SelectBuilder(selTrans).attribute(CIERP.DocumentAbstract.Name);

            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.TransactionInOutAbstract);
            queryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Product, product);
            queryBldr.addWhereAttrLessValue(CIProducts.TransactionInOutAbstract.Date, dateTo.plusSeconds(1));
            queryBldr.addWhereAttrGreaterValue(CIProducts.TransactionInOutAbstract.Date, dateFrom.minusSeconds(1));
            if (!listStorage.isEmpty()) {
                queryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Storage, listStorage.toArray());
            } else {
                queryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Storage, 0);
                SalesKardexReport_Base.LOG.debug("Report not contains storage");
            }
            addAttrQuery2InitKardex(_parameter, queryBldr);
            queryBldr.addOrderByAttributeAsc(CIProducts.TransactionInOutAbstract.Date);
            queryBldr.addOrderByAttributeAsc(CIProducts.TransactionInOutAbstract.Position);
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CIProducts.TransactionInOutAbstract.Date,
                            CIProducts.TransactionInOutAbstract.Quantity,
                            CIProducts.TransactionInOutAbstract.UoM);
            multi.addSelect(selTransDocInst, selTransDocType, selTransDocName);
            multi.setEnforceSorted(true);
            multi.execute();
            while (multi.next()) {
                final Map<String, Object> map = new HashMap<String, Object>();
                final DateTime dateTrans = multi.<DateTime>getAttribute(CIProducts.TransactionInOutAbstract.Date);
                final Long uoMId = multi.<Long>getAttribute(CIProducts.TransactionInOutAbstract.UoM);
                final UoM uoM = Dimension.getUoM(uoMId);
                BigDecimal quantity = multi.<BigDecimal>getAttribute(CIProducts.TransactionInOutAbstract.Quantity);

                quantity = quantity.multiply(new BigDecimal(uoM.getNumerator()).divide(new BigDecimal(uoM.getDenominator())))
                                .setScale(2,BigDecimal.ROUND_HALF_UP);

                final Type transDocType = multi.<Type>getSelect(selTransDocType);
                final Instance transDocInst = multi.<Instance>getSelect(selTransDocInst);
                final String transDocName = multi.<String>getSelect(selTransDocName);

                map.put(Field.TRANS_DATE.getKey(), dateTrans);
                if (multi.getCurrentInstance().getType().equals(CIProducts.TransactionInbound.getType())
                                || multi.getCurrentInstance().getType()
                                .equals(CIProducts.TransactionInbound4StaticStorage.getType())) {
                    map.put(Field.TRANS_INBOUND_QUANTITY.getKey(), quantity);
                    addMap2DocumentTypeIn(_parameter, map, transDocInst, dateFrom, dateTo, product);
                } else {
                    map.put(Field.TRANS_OUTBOUND_QUANTITY.getKey(), quantity);
                    map.put(Field.TRANS_DOC_SERIE.getKey(), getSeriesOrNumberDocOut(_parameter, transDocName, true));
                    map.put(Field.TRANS_DOC_NUMBER.getKey(), getSeriesOrNumberDocOut(_parameter, transDocName, false));
                    addMap2DocumentTypeOut(_parameter, map, transDocInst, transDocType);
                }
                addMap2TransactionInfo(_parameter, map, multi.getCurrentInstance());
                values.add(map);
            }
        }
        getValues().addAll(values);
    }

    /**
     * @param _parameter
     * @param _currentInstance
     */
    protected void addMap2TransactionInfo(final Parameter _parameter,
                                          final Map<String, Object> _map,
                                          final Instance _transaction)
    {
        // to be implemented
    }

    protected Map<String, Object> getInventoryValue(final Parameter _parameter,
                                                    final Instance _product,
                                                    final List<Long> _listStorage,
                                                    final DateTime _dateFrom)
        throws EFapsException
    {
        BigDecimal actualInventory = BigDecimal.ZERO;
        SalesKardexReport_Base.LOG.debug("Start evaluate inventory");
        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.Inventory);
        if (!_listStorage.isEmpty()) {
            queryBldr.addWhereAttrEqValue(CIProducts.Inventory.Storage, _listStorage.toArray());
        } else {
            queryBldr.addWhereAttrEqValue(CIProducts.Inventory.Storage, 0);
            SalesKardexReport_Base.LOG.debug("Report not contains storage");
        }
        queryBldr.addWhereAttrEqValue(CIProducts.Inventory.Product, _product);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIProducts.Inventory.Quantity);
        multi.execute();
        while (multi.next()) {
            final BigDecimal quantity = multi.<BigDecimal>getAttribute(CIProducts.Inventory.Quantity);
            actualInventory = actualInventory.add(quantity);
            SalesKardexReport_Base.LOG.debug("Inventory: {}", actualInventory);
        }

        final QueryBuilder transQueryBldr = new QueryBuilder(CIProducts.TransactionInOutAbstract);
        if (!_listStorage.isEmpty()) {
            transQueryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Storage, _listStorage.toArray());
        } else {
            transQueryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Storage, 0);
            SalesKardexReport_Base.LOG.debug("Report not contains storage");
        }
        transQueryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Product, _product);
        transQueryBldr.addWhereAttrGreaterValue(CIProducts.TransactionInOutAbstract.Date, _dateFrom.minusMinutes(1));
        addAttrQuery2InventoryValue(_parameter, transQueryBldr);
        final MultiPrintQuery transMulti = transQueryBldr.getPrint();
        transMulti.addAttribute(CIProducts.TransactionInOutAbstract.Quantity,
                                CIProducts.TransactionInOutAbstract.UoM);
        transMulti.execute();
        while (transMulti.next()) {
            BigDecimal quantity = transMulti.<BigDecimal>getAttribute(CIProducts.TransactionInbound.Quantity);
            final Long uoMId = transMulti.<Long>getAttribute(CIProducts.TransactionInbound.UoM);
            final UoM uoM = Dimension.getUoM(uoMId);
            quantity = quantity.multiply(new BigDecimal(uoM.getNumerator())
                                            .divide(new BigDecimal(uoM.getDenominator())));
            final Instance inst = transMulti.getCurrentInstance();
            if (inst.getType().equals(CIProducts.TransactionInbound.getType())
                            || inst.getType().equals(CIProducts.TransactionInbound4StaticStorage.getType())) {
                quantity = quantity.negate();
            }
            actualInventory = actualInventory.add(quantity);
            SalesKardexReport_Base.LOG.debug("Transaction: {} ,Inventory: {}", quantity, actualInventory);
        }

        final Map<String, Object> map = new HashMap<String, Object>();
        map.put(Field.TRANS_DOC_OPERATION.getKey(), "16");
        map.put(Field.TRANS_INBOUND_QUANTITY.getKey(), actualInventory);
        map.put(Field.TRANS_INBOUND_COST.getKey(), getCost(_parameter, _product, _dateFrom.minusDays(1)));

        return map;
    }

    protected void addAttrQuery2InventoryValue(final Parameter _parameter,
                                               final QueryBuilder _queryBldr)
        throws EFapsException
    {
        // to be implemented
    }

    protected void addAttrQuery2InitKardex(final Parameter _parameter,
                                           final QueryBuilder _queryBldr)
        throws EFapsException
    {
        // to be implemented
    }

    protected List<Long> getStorage4List(final Instance _storage)
        throws EFapsException
    {
        final List<Long> list = new ArrayList<Long>();

        if (_storage.getType().isKindOf(CIProducts.StorageGroupAbstract.getType())) {
            final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.StorageGroupAbstract2StorageAbstract);
            attrQueryBldr.addWhereAttrEqValue(CIProducts.StorageGroupAbstract2StorageAbstract.FromAbstractLink,
                            _storage.getId());
            final AttributeQuery attrQuery =
                            attrQueryBldr.getAttributeQuery(CIProducts.StorageGroupAbstract2StorageAbstract.ToAbstractLink);

            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.StorageAbstract);
            queryBldr.addWhereAttrInQuery(CIProducts.StorageAbstract.ID, attrQuery);
            final InstanceQuery query = queryBldr.getQuery();
            query.execute();
            while (query.next()) {
                list.add(query.getCurrentValue().getId());
            }
        } else {
            list.add(_storage.getId());
        }
        return list;
    }

    protected BigDecimal getCost(final Parameter _parameter,
                                 final Instance _productInstance,
                                 final DateTime _dateFrom)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ONE.negate();

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductCost);
        queryBldr.addWhereAttrEqValue(CIProducts.ProductCost.ProductLink, _productInstance.getId());
        queryBldr.addWhereAttrGreaterValue(CIProducts.ProductCost.ValidUntil, _dateFrom.minusMinutes(1));
        queryBldr.addWhereAttrLessValue(CIProducts.ProductCost.ValidFrom, _dateFrom.plusMinutes(1));
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIProducts.ProductCost.Price);
        multi.execute();
        if (multi.next()) {
            ret = multi.<BigDecimal>getAttribute(CIProducts.ProductCost.Price);
        }

        return ret;
    }

    protected void addMap2DocumentTypeIn(final Parameter _parameter,
                                         final Map<String, Object> _map,
                                         final Instance _docTransaction,
                                         final DateTime _dateTransStart,
                                         final DateTime _dateTransFinish,
                                         final Instance _productInst)
        throws EFapsException
    {
        final DocTransactionIn docTransIn =
                        new DocTransactionIn(_parameter, _docTransaction, _dateTransStart, _dateTransFinish, _productInst);
        _map.put(Field.TRANS_DOC_TYPE.getKey(), docTransIn.getDocumentTypeName(docTransIn.getInstance()));
        _map.put(Field.TRANS_INBOUND_COST.getKey(), docTransIn.getCost4Doc());
        _map.put(Field.TRANS_DOC_SERIE.getKey(), getSeriesOrNumberDocOut(_parameter, docTransIn.getDoc2DocName(), true));
        _map.put(Field.TRANS_DOC_NUMBER.getKey(), getSeriesOrNumberDocOut(_parameter, docTransIn.getDoc2DocName(), false));
        _map.put(Field.TRANS_DOC_OPERATION.getKey(), docTransIn.getProductDocumentTypeName(docTransIn.getInstance()));
    }

    protected String getSeriesOrNumberDocOut(final Parameter _parameter,
                                             final String _name,
                                             final boolean _suffix)
    {
        String ret = "-";

        final String[] arrays = _name.split("-");

        if (arrays != null && arrays.length > 1) {
            if (arrays.length == 2) {
                if (_suffix) {
                    if (arrays[0].matches("^\\d+")) {
                        ret = arrays[0];
                    }
                } else {
                    if (!arrays[0].matches("^\\d+")) {
                        ret = _name;
                    } else {
                        ret = arrays[1];
                    }
                }
            } else if (arrays.length == 3) {
                if (_suffix) {
                    if (arrays[1].matches("^\\d+")) {
                        ret = arrays[1];
                    }
                } else {
                    if (!arrays[1].matches("^\\d+")) {
                        ret = _name;
                    } else {
                        ret = arrays[2];
                    }
                }
            } else {
                if (!_suffix) {
                    ret = _name;
                }
            }
        }

        return ret.trim();
    }

    protected void addMap2DocumentTypeOut(final Parameter _parameter,
                                          final Map<String, Object> _map,
                                          final Instance _transDocInst,
                                          final Type _transDocType)
        throws EFapsException
    {
        String ret = "-";

        final SelectBuilder selDocTypeLink = new SelectBuilder()
                        .linkto(CISales.Document2DocumentType.DocumentTypeLink);
        final SelectBuilder selDocTypeLinkName = new SelectBuilder(selDocTypeLink).attribute(CIERP.DocumentType.Name);
        final SelectBuilder selDocTypeLinkType = new SelectBuilder(selDocTypeLink).type();

        final QueryBuilder queryBldr = new QueryBuilder(CISales.Document2DocumentType);
        queryBldr.addWhereAttrEqValue(CISales.Document2DocumentType.DocumentLink, _transDocInst.getId());
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addSelect(selDocTypeLinkType, selDocTypeLinkName);
        multi.execute();
        while (multi.next()) {
            if (multi.<Type>getSelect(selDocTypeLinkType).equals(CISales.ProductDocumentType.getType())) {
                ret = multi.<String>getSelect(selDocTypeLinkName);
            }
        }
        if (SalesKardexReport_Base.DOCTYPE_MAP.containsKey(_transDocType.getId())) {
            _map.put(Field.TRANS_DOC_TYPE.getKey(), SalesKardexReport_Base.DOCTYPE_MAP.get(_transDocType.getId()));
        } else {
            _map.put(Field.TRANS_DOC_TYPE.getKey(), "-");
        }
        _map.put(Field.TRANS_DOC_OPERATION.getKey(), ret);
    }

    protected void addReport2Parameter(final Parameter _parameter,
                                        final StandartReport _report)
        throws EFapsException
    {
        // TODO Auto-generated method stub
    }

    public Return createReport(final Parameter _parameter)
        throws EFapsException
    {
        final String dateFrom = _parameter.getParameterValue(CIFormSales.Sales_Products_Kardex_OfficialReportForm.dateFrom.name);
        final String dateTo = _parameter.getParameterValue(CIFormSales.Sales_Products_Kardex_OfficialReportForm.dateTo.name);
        final Instance productInst = Instance.get(_parameter
                                .getParameterValue(CIFormSales.Sales_Products_Kardex_OfficialReportForm.product.name));
        final Instance storageGroupInst = Instance.get(_parameter
                        .getParameterValue(CIFormSales.Sales_Products_Kardex_OfficialReportForm.storage.name));
        final DateTime from = new DateTime(dateFrom);
        final DateTime to = new DateTime(dateTo);

        final StandartReport report = new StandartReport();
        report.setFileName(getReportName(_parameter, from, to));
        report.getJrParameters().put("Periode", shortTimes(from, to));
        final SystemConfiguration config = ERP.getSysConfig();
        if (config != null) {
            final String companyName = config.getAttributeValue(ERPSettings.COMPANYNAME);
            final String companyTaxNumb = config.getAttributeValue(ERPSettings.COMPANYTAX);

            if (companyName != null && companyTaxNumb != null && !companyName.isEmpty() && !companyTaxNumb.isEmpty()) {
                report.getJrParameters().put("CompanyName", companyName);
                report.getJrParameters().put("CompanyTaxNum", companyTaxNumb);
            }
        }
        if (productInst.isValid()) {
            final ProductKardex product = new ProductKardex(productInst);
            report.getJrParameters().put("ProductName", product.getProductName());
            report.getJrParameters().put("ProductDesc", product.getProductDesc());
            report.getJrParameters().put("ProductUoM", product.getProductUoM());
            report.getJrParameters().put("ProductExistType", product.getProductExistType());
        }
        if (storageGroupInst.isValid()) {
            final PrintQuery print = new PrintQuery(storageGroupInst);
            print.addAttribute(CIProducts.StorageGroupAbstract.Name);
            print.execute();

            report.getJrParameters().put("StorageName", print.<String>getAttribute(CIProducts.StorageGroupAbstract.Name));
        }
        addReport2Parameter(_parameter, report);

        return report.execute(_parameter);
    }

    /**
     * Get the name for the report.
     *
     * @param _parameter Parameter as passed form the eFaps API
     * @param _from fromdate
     * @param _to   to date
     * @return name of the report
     */
    protected String getReportName(final Parameter _parameter,
                                   final DateTime _from,
                                   final DateTime _to)
    {
        return DBProperties.getProperty("Sales_Products_KardexOfficial.Label", "es") + "-" + shortTimes(_from, _to);
    }

    protected String shortTimes(final DateTime _from,
                                final DateTime _to)
    {
        return _from.toString(DateTimeFormat.shortDate()) + " - " + _to.toString(DateTimeFormat.shortDate());
    }

    public class ProductKardex
    {
        private final Instance product;

        private String name;

        private String description;

        private String uoM;

        public ProductKardex(final Instance _product)
            throws EFapsException
        {
            this.product = _product;
            if (getInstance().isValid()) {
                final PrintQuery print = new PrintQuery(getInstance());
                print.addAttribute(CIProducts.ProductAbstract.Name,
                                CIProducts.ProductAbstract.Description,
                                CIProducts.ProductAbstract.Dimension);
                print.execute();

                this.name = print.<String>getAttribute(CIProducts.ProductAbstract.Name);
                this.description = print.<String>getAttribute(CIProducts.ProductAbstract.Description);
                this.uoM = Dimension.get(print.<Long>getAttribute(CIProducts.ProductAbstract.Dimension))
                                .getBaseUoM().getName();
            }
        }

        protected Instance getInstance() {
            return this.product;
        }

        protected String getProductName()
        {
            return this.name;
        }

        protected String getProductDesc()
        {
            return this.description;
        }

        protected String getProductUoM()
        {
            return this.uoM;
        }

        protected String getProductExistType()
            throws EFapsException
        {
            final StringBuilder ret = new StringBuilder();
            if (SalesKardexReport_Base.EXISTTYPE_MAP.containsKey(this.product.getType().getId())) {
                ret.append(SalesKardexReport_Base.EXISTTYPE_MAP.get(this.product.getType().getId()));
            }

            return ret.toString();
        }
    }

    public class DocTransactionIn
    {

        final Instance instance;

        final Parameter param;

        final DateTime dateTransactionInStart;

        final DateTime dateTransactionInFinish;

        final ProductKardex product;

        Instance doc2docInstance;

        public DocTransactionIn(final Parameter _parameter,
                                final Instance _docTransaction,
                                final DateTime _dateStartTransaction,
                                final DateTime _dateFinishTransaction,
                                final Instance _product)
            throws EFapsException
        {
            this.param = _parameter;
            this.instance = _docTransaction;
            this.dateTransactionInStart = _dateStartTransaction;
            this.dateTransactionInFinish = _dateFinishTransaction;
            this.doc2docInstance = null;
            this.product = new ProductKardex(_product);
        }

        protected Instance getInstance()
        {
            return this.instance;
        }

        protected Instance getDoc2DocInstance()
        {
            return this.doc2docInstance;
        }

        protected String getDocumentName(final Type _type,
                                         final Instance _instance)
            throws EFapsException
        {
            final StringBuilder ret = new StringBuilder();
            final QueryBuilder attrQueryBldr = new QueryBuilder(CIERP.Document2DocumentTypeAbstract);
            attrQueryBldr.addWhereAttrEqValue(CIERP.Document2DocumentTypeAbstract.DocumentLinkAbstract, _instance.getId());
            final AttributeQuery attrQuery = attrQueryBldr
                            .getAttributeQuery(CIERP.Document2DocumentTypeAbstract.DocumentTypeLinkAbstract);

            final QueryBuilder queryBldr = new QueryBuilder(_type);
            queryBldr.addWhereAttrInQuery(CIERP.DocumentTypeAbstract.ID, attrQuery);
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CIERP.DocumentTypeAbstract.Name);
            multi.execute();
            while (multi.next()) {
                if (multi.getCurrentInstance().getType().equals(_type)) {
                    ret.append(multi.<String>getAttribute(CIERP.DocumentTypeAbstract.Name));
                    if (_type.equals(CIERP.DocumentType.getType())) {
                        this.doc2docInstance = _instance;
                    }
                    break;
                }
            }
            return ret.toString();
        }

        protected String getDocumentTypeName(final Instance _docInst)
            throws EFapsException
        {
            final StringBuilder ret = new StringBuilder();

            if (CISales.IncomingInvoice.getType().equals(_docInst.getType())
                            || CISales.IncomingReceipt.getType().equals(_docInst.getType())) {
                ret.append(getDocumentName(CIERP.DocumentType.getType(), _docInst));
            } else {
                if (CISales.RecievingTicket.getType().equals(_docInst.getType())) {
                    ret.append(getDoc2Doc4DocName(_docInst));
                    if (SalesKardexReport_Base.DOCTYPE_MAP.containsKey(_docInst.getType().getId())
                                    && ret.toString().isEmpty()) {
                        ret.append(SalesKardexReport_Base.DOCTYPE_MAP.get(_docInst.getType().getId()));
                        this.doc2docInstance = _docInst;
                    }
                } else if (CISales.ReturnSlip.getType().equals(_docInst.getType())
                                || CISales.ReturnUsageReport.getType().equals(_docInst.getType())
                                || CISales.TransactionDocument.getType().equals(_docInst.getType())) {
                    if (SalesKardexReport_Base.DOCTYPE_MAP.containsKey(_docInst.getType().getId())) {
                        ret.append(SalesKardexReport_Base.DOCTYPE_MAP.get(_docInst.getType().getId()));
                        this.doc2docInstance = _docInst;
                    }
                }
            }

            if (ret.toString().isEmpty()) {
                ret.append("-");
            }

            return ret.toString();
        }

        protected String getDoc2Doc4DocName(final Instance _instance)
            throws EFapsException
        {
            final StringBuilder ret = new StringBuilder();

            final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Document2DerivativeDocument);
            attrQueryBldr.addWhereAttrEqValue(CISales.Document2DerivativeDocument.From, _instance.getId());
            final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.Document2DerivativeDocument.To);

            final QueryBuilder queryBldr = new QueryBuilder(CIERP.DocumentAbstract);
            queryBldr.addWhereAttrInQuery(CIERP.DocumentAbstract.ID, attrQuery);
            queryBldr.addOrderByAttributeAsc(CIERP.DocumentAbstract.ID);
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CIERP.DocumentAbstract.Name,
                            CIERP.DocumentAbstract.Date);
            multi.setEnforceSorted(true);
            multi.execute();
            while (multi.next()) {
                if (multi.getCurrentInstance().getType().equals(CISales.IncomingInvoice.getType())
                                || multi.getCurrentInstance().getType().equals(CISales.IncomingReceipt.getType())) {
                    if (multi.<DateTime>getAttribute(CIERP.DocumentAbstract.Date).isAfter(this.dateTransactionInStart.minusSeconds(1)) &&
                                    multi.<DateTime>getAttribute(CIERP.DocumentAbstract.Date).isBefore(this.dateTransactionInFinish.plusSeconds(1))) {
                        if (existsProduct4Document(multi.getCurrentInstance())) {
                            ret.append(getDocumentName(CIERP.DocumentType.getType(), multi.getCurrentInstance()));
                            break;
                        }
                    }
                } else {
                    ret.append(getExtendDoc2Doc4DocName(multi.getCurrentInstance()));
                }
            }
            return ret.toString();
        }

        protected boolean existsProduct4Document(final Instance _curInst)
            throws EFapsException
        {
            boolean ret = false;
            final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionSumAbstract);
            queryBldr.addWhereAttrEqValue(CISales.PositionSumAbstract.DocumentAbstractLink, _curInst.getId());
            queryBldr.addWhereAttrEqValue(CISales.PositionSumAbstract.Product, this.product.getInstance());
            final InstanceQuery query = queryBldr.getQuery();
            query.execute();
            if (query.next()) {
                ret = true;
            }
            return ret;
        }

        public String getExtendDoc2Doc4DocName(final Instance _doc2doc)
            throws EFapsException
        {
            return new StringBuilder().toString();
        }

        protected String getProductDocumentTypeName(final Instance _docInst)
            throws EFapsException
        {
            final StringBuilder ret = new StringBuilder();

            if (CISales.IncomingInvoice.getType().equals(_docInst.getType())
                            || CISales.IncomingReceipt.getType().equals(_docInst.getType())
                            || CISales.RecievingTicket.getType().equals(_docInst.getType())
                            || CISales.ReturnSlip.getType().equals(_docInst.getType())
                            || CISales.ReturnUsageReport.getType().equals(_docInst.getType())
                            || CISales.TransactionDocument.getType().equals(_docInst.getType())) {
                ret.append(getDocumentName(CISales.ProductDocumentType.getType(), _docInst));
            }

            if (ret.toString().isEmpty()) {
                ret.append("-");
            }

            return ret.toString();
        }

        protected BigDecimal getCost4Doc()
            throws EFapsException
        {
            BigDecimal ret = BigDecimal.ONE.negate();
            if (this.doc2docInstance != null && this.doc2docInstance.isValid()) {
                if (this.doc2docInstance.getType().equals(CISales.IncomingInvoice.getType())
                                || this.doc2docInstance.getType().equals(CISales.IncomingReceipt.getType())) {
                    if (this.product.getInstance().isValid()) {
                        final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionSumAbstract);
                        queryBldr.addWhereAttrEqValue(CISales.PositionSumAbstract.DocumentAbstractLink,
                                        this.doc2docInstance.getId());
                        queryBldr.addWhereAttrEqValue(CISales.PositionSumAbstract.Product, this.product.getInstance()
                                        .getId());
                        queryBldr.addOrderByAttributeAsc(CISales.PositionSumAbstract.PositionNumber);
                        final MultiPrintQuery multi = queryBldr.getPrint();
                        multi.addAttribute(CISales.PositionSumAbstract.NetUnitPrice,
                                        CISales.PositionSumAbstract.UoM);
                        multi.setEnforceSorted(true);
                        multi.execute();
                        while (multi.next()) {
                            final Long uoMId = multi.<Long>getAttribute(CISales.IncomingInvoicePosition.UoM);
                            final UoM uoM = Dimension.getUoM(uoMId);
                            ret = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.NetUnitPrice)
                                        .multiply(new BigDecimal(uoM.getNumerator())
                                        .divide(new BigDecimal(uoM.getDenominator()))).setScale(2, BigDecimal.ROUND_HALF_UP);

                            final BigDecimal other = getCost4IncomingCreditNote();
                            if (other.compareTo(ret) < 1) {
                                ret = ret.subtract(other);
                            }
                            break;
                        }
                    }
                }
            }
            return ret;
        }

        protected BigDecimal getCost4IncomingCreditNote()
            throws EFapsException
        {
            BigDecimal ret = BigDecimal.ZERO;

            if (this.doc2docInstance.getType().equals(CISales.IncomingInvoice.getType())) {
                if (this.product.getInstance().isValid()) {
                    final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Document2DerivativeDocument);
                    attrQueryBldr.addWhereAttrEqValue(CISales.Document2DocumentAbstract.FromAbstractLink,
                                    this.doc2docInstance);
                    final AttributeQuery attrQuery = attrQueryBldr
                                    .getAttributeQuery(CISales.Document2DocumentAbstract.ToAbstractLink);

                    final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionSumAbstract);
                    queryBldr.addWhereAttrEqValue(CISales.PositionSumAbstract.Type,
                                    CISales.IncomingCreditNotePosition.getType().getId());
                    queryBldr.addWhereAttrInQuery(CISales.PositionSumAbstract.DocumentAbstractLink, attrQuery);
                    queryBldr.addWhereAttrEqValue(CISales.PositionSumAbstract.Product, this.product.getInstance());
                    queryBldr.addOrderByAttributeAsc(CISales.PositionSumAbstract.PositionNumber);
                    final MultiPrintQuery multi = queryBldr.getPrint();
                    multi.addAttribute(CISales.PositionSumAbstract.NetUnitPrice,
                                    CISales.PositionSumAbstract.UoM);
                    multi.setEnforceSorted(true);
                    multi.execute();
                    while (multi.next()) {
                        final Long uoMId = multi.<Long>getAttribute(CISales.IncomingInvoicePosition.UoM);
                        final UoM uoM = Dimension.getUoM(uoMId);
                        ret = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.NetUnitPrice)
                                    .multiply(new BigDecimal(uoM.getNumerator())
                                    .divide(new BigDecimal(uoM.getDenominator()))).setScale(2, BigDecimal.ROUND_HALF_UP);
                        break;
                    }
                }
            }

            return ret;
        }

        protected String getDoc2DocName()
            throws EFapsException
        {
            final StringBuilder ret = new StringBuilder();

            if (this.doc2docInstance != null && this.doc2docInstance.isValid()) {
                final PrintQuery print = new PrintQuery(this.doc2docInstance);
                print.addAttribute(CIERP.DocumentAbstract.Name);
                print.execute();

                ret.append(print.<String>getAttribute(CIERP.DocumentAbstract.Name));
            }

            return ret.toString();
        }
    }
}
