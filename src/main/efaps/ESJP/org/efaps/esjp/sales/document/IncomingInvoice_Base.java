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
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.lang3.BooleanUtils;
import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.common.uiform.Field;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.erp.AbstractWarning;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.IWarning;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.Calculator;
import org.efaps.esjp.sales.Channel;
import org.efaps.esjp.sales.PriceUtil;
import org.efaps.esjp.sales.PriceUtil_Base.ProductPrice;
import org.efaps.esjp.sales.document.AbstractDocumentTax_Base.DocTaxInfo;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for Type Incoming Invoice.
 *
 * @author The eFaps Team
 */
@EFapsUUID("f7d75f38-5ac8-4bf4-9609-6226ac82fea3")
@EFapsApplication("eFapsApp-Sales")
public abstract class IncomingInvoice_Base
    extends AbstractDocumentSum
{
    /**
     * Used to store the Revision in the Context.
     */
    protected static final String REVISIONKEY = IncomingInvoice.class.getName() + "RevisionKey";

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(IncomingInvoice.class);

    /**
     * Executed from a Command execute vent to create a new Incoming Invoice.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final CreatedDoc createdDoc = createDoc(_parameter);
        createPositions(_parameter, createdDoc);
        connect2DocumentType(_parameter, createdDoc);
        connect2Derived(_parameter, createdDoc);
        registerPurchasePrices(_parameter, createdDoc);
        connect2Object(_parameter, createdDoc);
        createUpdateTaxDoc(_parameter, createdDoc, false);
        ret.put(ReturnValues.INSTANCE, createdDoc.getInstance());
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the efasp API
     * @param _createdDoc created Document
     * @param _isUpdate is it an update or not
     * @throws EFapsException on error
     */
    public void createUpdateTaxDoc(final Parameter _parameter,
                                   final CreatedDoc _createdDoc,
                                   final boolean _isUpdate)
        throws EFapsException
    {
        boolean executed = false;
        if ("false".equalsIgnoreCase(ParameterUtil.getParameterValue(_parameter,
                        CIFormSales.Sales_IncomingInvoiceForm.headingTaxDoc.name,
                        CIFormSales.Sales_IncomingInvoiceForm.headingTaxDoc4Edit.name,
                        CIFormSales.Sales_IncomingInvoiceForm.headingTaxDoc4EditCollapsed.name))) {

            final String perceptionValueStr = _parameter
                            .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.perceptionValue.name);
            if (perceptionValueStr != null && !perceptionValueStr.isEmpty()) {
                final DecimalFormat formatter = NumberFormatter.get().getFormatter();
                try {
                    final BigDecimal perception = (BigDecimal) formatter.parse(perceptionValueStr);
                    final IncomingPerceptionCertificate doc = new IncomingPerceptionCertificate();
                    _createdDoc.addValue(AbstractDocumentTax_Base.TAXAMOUNTVALUE, perception);
                    doc.createUpdate4Doc(_parameter, _createdDoc);
                    executed = true;
                } catch (final ParseException p) {
                    throw new EFapsException(IncomingInvoice.class, "Perception.ParseException", p);
                }
            }

            final boolean isDetraction = "true".equalsIgnoreCase(_parameter
                            .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.detractionCheckbox.name));
            if (isDetraction) {
                final DecimalFormat formatter = NumberFormatter.get().getFormatter();
                try {
                    final String detractionValueStr = _parameter
                                    .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.detractionValue.name);
                    final BigDecimal detraction;
                    if (detractionValueStr != null && !detractionValueStr.isEmpty()) {
                        detraction = (BigDecimal) formatter.parse(detractionValueStr);
                    } else {
                        detraction = BigDecimal.ZERO;
                    }
                    final IncomingDetraction doc = new IncomingDetraction();
                    _createdDoc.addValue(AbstractDocumentTax_Base.TAXAMOUNTVALUE, detraction);

                    doc.createUpdate4Doc(_parameter, _createdDoc);
                    executed = true;
                } catch (final ParseException p) {
                    throw new EFapsException(IncomingInvoice.class, "Perception.ParseException", p);
                }
            }
            final boolean isRetention = "true".equalsIgnoreCase(_parameter
                            .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.retentionCheckbox.name));

            if (isRetention) {
                final DecimalFormat formatter = NumberFormatter.get().getFormatter();
                try {
                    final String retentionValueStr = _parameter
                                    .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.retentionValue.name);
                    final BigDecimal retention;
                    if (retentionValueStr != null && !retentionValueStr.isEmpty()) {
                        retention = (BigDecimal) formatter.parse(retentionValueStr);
                    } else {
                        retention = BigDecimal.ZERO;
                    }
                    final IncomingRetention doc = new IncomingRetention();
                    _createdDoc.addValue(AbstractDocumentTax_Base.TAXAMOUNTVALUE, retention);
                    doc.createUpdate4Doc(_parameter, _createdDoc);
                    executed = true;
                } catch (final ParseException p) {
                    throw new EFapsException(IncomingInvoice.class, "Perception.ParseException", p);
                }
            }
        }
        if (_createdDoc.getValue(AbstractDocumentTax_Base.TAXAMOUNTVALUE) == null && _isUpdate && !executed) {
            AbstractDocumentTax.getDocTaxInfo(_parameter, _createdDoc.getInstance()).clean4TaxDocInst(null);
        }
    }

    /**
     * Edit.
     *
     * @param _parameter Parameter from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final EditedDoc editedDoc = editDoc(_parameter);
        updatePositions(_parameter, editedDoc);
        updateConnection2Object(_parameter, editedDoc);
        createUpdateTaxDoc(_parameter, editedDoc, true);
        return new Return();
    }

    /**
     * Create the TransactionDocument for this invoice.
     *
     * @param _parameter Parameter from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return createTransDocShadow(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = new TransactionDocument().createDocumentShadow(_parameter);
        final Insert insert = new Insert(CISales.IncomingInvoice2TransactionDocumentShadowIn);
        insert.add(CISales.IncomingInvoice2TransactionDocumentShadowIn.FromLink, _parameter.getInstance());
        insert.add(CISales.IncomingInvoice2TransactionDocumentShadowIn.ToLink, createdDoc.getInstance());
        insert.execute();

        return new Return();
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _createdDoc   created doc
     * @throws EFapsException on error
     */
    protected void registerPurchasePrices(final Parameter _parameter,
                                          final CreatedDoc _createdDoc)
        throws EFapsException
    {
        if (Sales.INCOMINGINVOICEACTIVATEREGPURPRICE.get()) {
            @SuppressWarnings("unchecked")
            final List<Calculator> calcList = (List<Calculator>) _createdDoc.getValue(
                            AbstractDocument_Base.CALCULATORS_VALUE);
            if (calcList != null) {
                final String dateStr = (String) _createdDoc.getValue(CISales.DocumentSumAbstract.Date.name);
                final DateTime date;
                if (dateStr == null) {
                    date = new DateTime().withTimeAtStartOfDay();
                } else {
                    date = new DateTime(dateStr).withTimeAtStartOfDay();
                }
                final Object[] rateObj = getRateObject(_parameter);
                final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                                BigDecimal.ROUND_HALF_UP);
                final DecimalFormat unitFrmt = NumberFormatter.get().getFrmt4UnitPrice(getTypeName4SysConf(_parameter));
                final int uScale = unitFrmt.getMaximumFractionDigits();
                for (final Calculator calc : calcList) {

                    final ProductPrice prodPrice = new PriceUtil().getPrice(_parameter, date, calc.getProductInstance(),
                                    CIProducts.ProductPricelistPurchase);
                    final BigDecimal basePrice = prodPrice.getBasePrice();
                    final BigDecimal price;
                    if (Calculator.priceIsNet(_parameter, this)) {
                        price = calc.getNetUnitPrice().divide(rate, BigDecimal.ROUND_HALF_UP)
                                        .setScale(uScale, BigDecimal.ROUND_HALF_UP);
                    } else {
                        price = calc.getCrossUnitPrice().divide(rate, BigDecimal.ROUND_HALF_UP)
                                        .setScale(uScale, BigDecimal.ROUND_HALF_UP);
                    }
                    if (price.compareTo(basePrice) != 0) {
                        final Insert insert = new Insert(CIProducts.ProductPricelistPurchase);
                        insert.add(CIProducts.ProductPricelistPurchase.ProductLink, calc.getProductInstance());
                        insert.add(CIProducts.ProductPricelistPurchase.ValidFrom, date);
                        insert.add(CIProducts.ProductPricelistPurchase.ValidUntil, date.plusYears(10));
                        insert.execute();

                        final Insert posInsert = new Insert(CIProducts.ProductPricelistPosition);
                        posInsert.add(CIProducts.ProductPricelistPosition.ProductPricelist, insert.getInstance());
                        posInsert.add(CIProducts.ProductPricelistPosition.CurrencyId, Currency.getBaseCurrency());
                        posInsert.add(CIProducts.ProductPricelistPosition.Price, price);
                        posInsert.execute();
                    }
                }
            }
        }
    }

    /**
     * Method to do a transaction of all the products of a Incoming Invoice when
     * it is created.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _createdDoc instance of Incoming Invoice document recently created
     * @throws EFapsException on error
     */
    protected void incomingInvoiceCreateTransaction(final Parameter _parameter,
                                                    final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final String storage = _parameter.getParameterValue("storage");
        final String date = _parameter.getParameterValue("date");

        if (storage != null) {
            final List<Instance> positions = _createdDoc.getPositions();
            for (final Instance instance : positions) {
                final PrintQuery print = new PrintQuery(instance);
                print.addAttribute(CISales.IncomingInvoicePosition.Product, CISales.IncomingInvoicePosition.Quantity,
                                CISales.IncomingInvoicePosition.IncomingInvoice, CISales.IncomingInvoicePosition.UoM);
                print.execute();

                final Object productID = print.<Object>getAttribute(CISales.IncomingInvoicePosition.Product);
                final Object quantity = print.<Object>getAttribute(CISales.IncomingInvoicePosition.Quantity);
                final Object incomingInvoiceId = print
                                .<Object>getAttribute(CISales.IncomingInvoicePosition.IncomingInvoice);
                final Object uom = print.<Object>getAttribute(CISales.IncomingInvoicePosition.UoM);

                final Insert insert = new Insert(CIProducts.TransactionInbound);
                insert.add(CIProducts.TransactionInbound.Quantity, quantity);
                insert.add(CIProducts.TransactionInbound.Storage, storage);
                insert.add(CIProducts.TransactionInbound.Product, productID);
                insert.add(CIProducts.TransactionInbound.Description,
                         DBProperties.getProperty("org.efaps.esjp.sales.document.IncomingInvoice.description4Trigger"));
                insert.add(CIProducts.TransactionInbound.Date, date == null ? new DateTime() : date);
                insert.add(CIProducts.TransactionInbound.Document, incomingInvoiceId);
                insert.add(CIProducts.TransactionInbound.UoM, uom);
                insert.execute();
            }
        }
    }

    @Override
    protected void add2DocCreate(final Parameter _parameter,
                                 final Insert _insert,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final String seqKey = Sales.INCOMINGINVOICEREVSEQ.get();
        final NumberGenerator numgen = isUUID(seqKey)
                        ? NumberGenerator.get(UUID.fromString(seqKey))
                        : NumberGenerator.get(seqKey);
        if (numgen != null) {
            final String revision = numgen.getNextVal();
            Context.getThreadContext().setSessionAttribute(IncomingInvoice_Base.REVISIONKEY, revision);
            _insert.add(CISales.IncomingInvoice.Revision, revision);
        }
    }

    @Override
    protected void add2Map4UpdateField(final Parameter _parameter,
                                       final Map<String, Object> _map,
                                       final List<Calculator> _calcList,
                                       final Calculator _cal)
        throws EFapsException
    {
        super.add2Map4UpdateField(_parameter, _map, _calcList, _cal);
        final String perceptionPercentStr = _parameter
                        .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.perceptionPercent.name);
        if (perceptionPercentStr != null && !perceptionPercentStr.isEmpty()) {
            final DecimalFormat formatter = NumberFormatter.get().getFormatter();
            try {
                final BigDecimal perceptionPercent = (BigDecimal) formatter.parse(perceptionPercentStr);
                final BigDecimal crossTotal = getCrossTotal(_parameter, _calcList);
                final BigDecimal perception = crossTotal.multiply(perceptionPercent
                                .setScale(8, BigDecimal.ROUND_HALF_UP)
                                .divide(new BigDecimal(100), BigDecimal.ROUND_HALF_UP));
                final String perceptionStr = NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter))
                                .format(perception);
                _map.put(CIFormSales.Sales_IncomingInvoiceForm.perceptionValue.name, perceptionStr);
            } catch (final ParseException e) {
                IncomingInvoice_Base.LOG.error("Catched parsing error", e);
            }
        }
        final String detractionPercentStr = _parameter
                        .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.detractionPercent.name);
        if (detractionPercentStr != null && !detractionPercentStr.isEmpty()) {
            final DecimalFormat formatter = NumberFormatter.get().getFormatter();
            try {
                final BigDecimal detractionPercent = (BigDecimal) formatter.parse(detractionPercentStr);
                final BigDecimal crossTotal = getCrossTotal(_parameter, _calcList);
                final BigDecimal detraction = crossTotal.multiply(detractionPercent
                                .setScale(8, BigDecimal.ROUND_HALF_UP)
                                .divide(new BigDecimal(100), BigDecimal.ROUND_HALF_UP));
                final String detractionStr = NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter))
                                .format(detraction);
                _map.put(CIFormSales.Sales_IncomingInvoiceForm.detractionValue.name, detractionStr);
            } catch (final ParseException e) {
                IncomingInvoice_Base.LOG.error("Catched parsing error", e);
            }
        }
        final String retentionPercentStr = _parameter
                        .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.retentionPercent.name);
        if (retentionPercentStr != null && !retentionPercentStr.isEmpty()) {
            final DecimalFormat formatter = NumberFormatter.get().getFormatter();
            try {
                final BigDecimal retentionPercent = (BigDecimal) formatter.parse(retentionPercentStr);
                final BigDecimal crossTotal = getCrossTotal(_parameter, _calcList);
                final BigDecimal retention = crossTotal.multiply(retentionPercent
                                .setScale(8, BigDecimal.ROUND_HALF_UP)
                                .divide(new BigDecimal(100), BigDecimal.ROUND_HALF_UP));
                final String retentionStr = NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter))
                                .format(retention);
                _map.put(CIFormSales.Sales_IncomingInvoiceForm.retentionValue.name, retentionStr);
            } catch (final ParseException e) {
                IncomingInvoice_Base.LOG.error("Catched parsing error", e);
            }
        }
    }

    @Override
    protected StringBuilder add2JavaScript4DocumentContact(final Parameter _parameter,
                                                           final List<Instance> _instances,
                                                           final Instance _contactInstance)
        throws EFapsException
    {
        final StringBuilder ret = super.add2JavaScript4DocumentContact(_parameter, _instances, _contactInstance);
        if (Sales.INCOMINGINVOICEFROMORDEROUTBOUND.get()) {
            final Properties props = Sales.INCOMINGINVOICEFROMORDEROUTBOUNDAC.get();
            final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter, props);
            ret.append(getJS4Doc4Contact(_parameter, _contactInstance,
                            CIFormSales.Sales_IncomingInvoiceForm.orderOutbounds.name, queryBldr));
        }
        if (Sales.INCOMINGINVOICEFROMSERVORDEROUTBOUND.get()) {
            final Properties props = Sales.INCOMINGINVOICEFROMSERVORDEROUTBOUNDAC.get();
            final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter, props);
            ret.append(getJS4Doc4Contact(_parameter, _contactInstance,
                            CIFormSales.Sales_IncomingInvoiceForm.serviceOrderOutbounds.name, queryBldr));
        }
        if (Sales.INCOMINGINVOICEFROMRECIEVINGTICKET.get()) {
            final Properties props = Sales.INCOMINGINVOICEFROMRECIEVINGTICKETAC.get();
            final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter, props);
            ret.append(getJS4Doc4Contact(_parameter, _contactInstance,
                            CIFormSales.Sales_IncomingInvoiceForm.recievingTickets.name, queryBldr));
        }
        return ret;
    }

    @Override
    protected StringBuilder add2JavaScript4Document(final Parameter _parameter,
                                                    final List<Instance> _instances)
        throws EFapsException
    {
        final StringBuilder ret =  super.add2JavaScript4Document(_parameter, _instances);
        ret.append(new Channel().add2JavaScript4Document(_parameter, _instances));
        return ret;
    }

    @Override
    protected void add2UpdateMap4Contact(final Parameter _parameter,
                                         final Instance _contactInstance,
                                         final Map<String, Object> _map)
        throws EFapsException
    {
        super.add2UpdateMap4Contact(_parameter, _contactInstance, _map);
        if (Sales.INCOMINGINVOICEFROMORDEROUTBOUND.get()) {
            final Properties props = Sales.INCOMINGINVOICEFROMORDEROUTBOUNDAC.get();
            final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter, props);
            InterfaceUtils.appendScript4FieldUpdate(_map, getJS4Doc4Contact(_parameter, _contactInstance,
                            CIFormSales.Sales_IncomingInvoiceForm.orderOutbounds.name, queryBldr));
        }
        if (Sales.INCOMINGINVOICEFROMSERVORDEROUTBOUND.get()) {
            final Properties props = Sales.INCOMINGINVOICEFROMSERVORDEROUTBOUNDAC.get();
            final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter, props);
            InterfaceUtils.appendScript4FieldUpdate(_map, getJS4Doc4Contact(_parameter, _contactInstance,
                            CIFormSales.Sales_IncomingInvoiceForm.serviceOrderOutbounds.name, queryBldr));
        }
        if (Sales.INCOMINGINVOICEFROMRECIEVINGTICKET.get()) {
            final Properties props = Sales.INCOMINGINVOICEFROMRECIEVINGTICKETAC.get();
            final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter, props);
            InterfaceUtils.appendScript4FieldUpdate(_map, getJS4Doc4Contact(_parameter, _contactInstance,
                            CIFormSales.Sales_IncomingInvoiceForm.recievingTickets.name, queryBldr));
        }
    }

    @Override
    protected boolean  docIsSelected4JS4Doc4Contact(final Parameter _parameter,
                                                    final Instance _docInst)
        throws EFapsException
    {
        boolean ret = super.docIsSelected4JS4Doc4Contact(_parameter, _docInst);
        // not marked selected already but it is an order Outbound or a recieving ticket makr related
        if (!ret && !getInstances4Derived(_parameter).isEmpty()
                    && Sales.INCOMINGINVOICEFROMRECIEVINGTICKET.get() && Sales.INCOMINGINVOICEFROMORDEROUTBOUND.get()
                        && (_docInst.getType().isCIType(CISales.OrderOutbound)
                        ||  _docInst.getType().isCIType(CISales.RecievingTicket))) {
            final boolean isOrder = _docInst.getType().isCIType(CISales.OrderOutbound);
            final QueryBuilder queryBldr = new QueryBuilder(CISales.OrderOutbound2RecievingTicket);
            final SelectBuilder selInst;
            if (isOrder) {
                queryBldr.addWhereAttrEqValue(CISales.OrderOutbound2RecievingTicket.ToLink,
                                getInstances4Derived(_parameter).toArray());
                selInst = SelectBuilder.get().linkto(CISales.OrderOutbound2RecievingTicket.FromLink).instance();
            } else {
                queryBldr.addWhereAttrEqValue(CISales.OrderOutbound2RecievingTicket.FromLink,
                                getInstances4Derived(_parameter).toArray());
                selInst = SelectBuilder.get().linkto(CISales.OrderOutbound2RecievingTicket.ToLink).instance();
            }
            final MultiPrintQuery multi = queryBldr.getCachedPrint4Request();
            multi.addSelect(selInst);
            multi.execute();
            while (multi.next()) {
                ret = _docInst.equals(multi.<Instance>getSelect(selInst));
                if (ret) {
                    break;
                }
            }
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return value for the checkbox
     * @throws EFapsException on error
     */
    public Return accessCheck4TaxDoc(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final boolean inverse = "true".equalsIgnoreCase(getProperty(_parameter, "Inverse"));

        final DocTaxInfo docTaxInfo = AbstractDocumentTax.getDocTaxInfo(_parameter, _parameter.getInstance());
        final Boolean access = docTaxInfo.isDetraction() || docTaxInfo.isPerception() || docTaxInfo.isRetention();
        if (!inverse && access || inverse && !access) {
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return value for the checkbox
     * @throws EFapsException on error
     */
    public Return getPeceptionCheckBoxValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, AbstractDocumentTax.getDocTaxInfo(_parameter, _parameter.getInstance())
                        .isPerception());
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return value for the checkbox
     * @throws EFapsException on error
     */
    public Return getRetentionCheckBoxValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, AbstractDocumentTax.getDocTaxInfo(_parameter, _parameter.getInstance())
                        .isRetention());
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return value for the checkbox
     * @throws EFapsException on error
     */
    public Return getDetractionCheckBoxValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, AbstractDocumentTax.getDocTaxInfo(_parameter, _parameter.getInstance())
                        .isDetraction());
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return value for the checkbox
     * @throws EFapsException on error
     */
    public Return getPeceptionPercentValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final DocTaxInfo doctaxInfo = AbstractDocumentTax.getDocTaxInfo(_parameter, _parameter.getInstance());
        if (doctaxInfo.isPerception()) {
            retVal.put(ReturnValues.VALUES, doctaxInfo.getPercent());
        }
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return value for the checkbox
     * @throws EFapsException on error
     */
    public Return getRetentionPercentValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final DocTaxInfo doctaxInfo = AbstractDocumentTax.getDocTaxInfo(_parameter, _parameter.getInstance());
        if (doctaxInfo.isRetention()) {
            retVal.put(ReturnValues.VALUES, doctaxInfo.getPercent());
        }
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return value for the checkbox
     * @throws EFapsException on error
     */
    public Return getDetractionPercentValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final DocTaxInfo doctaxInfo = AbstractDocumentTax.getDocTaxInfo(_parameter, _parameter.getInstance());
        if (doctaxInfo.isDetraction()) {
            retVal.put(ReturnValues.VALUES, doctaxInfo.getPercent());
        }
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return value for the checkbox
     * @throws EFapsException on error
     */
    public Return getPeceptionValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final DocTaxInfo doctaxInfo = AbstractDocumentTax.getDocTaxInfo(_parameter, _parameter.getInstance());
        if (doctaxInfo.isPerception()) {
            retVal.put(ReturnValues.VALUES, doctaxInfo.getTaxAmount());
        }
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return value for the checkbox
     * @throws EFapsException on error
     */
    public Return getRetentionValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final DocTaxInfo doctaxInfo = AbstractDocumentTax.getDocTaxInfo(_parameter, _parameter.getInstance());
        if (doctaxInfo.isRetention()) {
            retVal.put(ReturnValues.VALUES, doctaxInfo.getTaxAmount());
        }
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return value for the checkbox
     * @throws EFapsException on error
     */
    public Return getDetractionValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final DocTaxInfo doctaxInfo = AbstractDocumentTax.getDocTaxInfo(_parameter, _parameter.getInstance());
        if (doctaxInfo.isDetraction()) {
            retVal.put(ReturnValues.VALUES, doctaxInfo.getTaxAmount());
        }
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return value for the checkbox
     * @throws EFapsException on error
     */
    public Return getDetractionServiceType(final Parameter _parameter)
        throws EFapsException
    {
        final Field field = new Field()
        {

            @Override
            protected void updatePositionList(final Parameter _parameter,
                                              final List<DropDownPosition> _values)
                throws EFapsException
            {
                super.updatePositionList(_parameter, _values);
                final DocTaxInfo doctaxInfo = AbstractDocumentTax.getDocTaxInfo(_parameter, _parameter.getInstance());
                if (doctaxInfo.isDetraction()) {
                    final Instance inst = doctaxInfo.getDetractionTypeInst();
                    for (final DropDownPosition pos : _values) {
                        pos.setSelected(pos.getValue().equals(inst.getOid()));
                    }
                }
            }
        };
        return field.getOptionListFieldValue(_parameter);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return containing maplist
     * @throws EFapsException on error
     */
    public Return updateFields4PerceptionPercent(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();

        final List<Calculator> calcList = analyseTable(_parameter, null);

        if (calcList.size() > 0) {
            add2Map4UpdateField(_parameter, map, calcList, null);
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        }
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return containing maplist
     * @throws EFapsException on error
     */
    public Return updateFields4RetentionPercent(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();

        final List<Calculator> calcList = analyseTable(_parameter, null);

        if (calcList.size() > 0) {
            add2Map4UpdateField(_parameter, map, calcList, null);
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        }
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return containing maplist
     * @throws EFapsException on error
     */
    public Return updateFields4DetractionPercent(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();

        final List<Calculator> calcList = analyseTable(_parameter, null);

        if (calcList.size() > 0) {
            add2Map4UpdateField(_parameter, map, calcList, null);
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        }
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return containing maplist
     * @throws EFapsException on error
     */
    public Return updateFields4DetractionServiceType(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();
        final Instance inst = Instance.get(_parameter.getParameterValue("detractionServiceType"));
        final PrintQuery print = new PrintQuery(inst);
        print.addAttribute(CIERP.AttributeDefinitionMappingAbstract.MappingKey,
                        CIERP.AttributeDefinitionMappingAbstract.Description);
        print.execute();
        final String descr = print.getAttribute(CIERP.AttributeDefinitionMappingAbstract.Description);
        if (descr.matches("[0-9]+")) {
            map.put("detractionPercent", descr);
            ParameterUtil.setParmeterValue(_parameter, CIFormSales.Sales_IncomingInvoiceForm.detractionPercent.name,
                            descr);
        }

        final List<Calculator> calcList = analyseTable(_parameter, null);
        if (calcList.size() > 0) {
            add2Map4UpdateField(_parameter, map, calcList, null);
        }

        list.add(map);
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return with Snipplet
     * @throws EFapsException on error
     */
    public Return showRevisionFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String revision = (String) Context.getThreadContext().getSessionAttribute(
                        IncomingInvoice_Base.REVISIONKEY);
        Context.getThreadContext().setSessionAttribute(IncomingInvoice_Base.REVISIONKEY, null);
        final StringBuilder html = new StringBuilder();
        html.append("<span style=\"text-align: center; width: 98%; font-size:40pt; height: 55px; position:absolute\">")
            .append(revision).append("</span>");
        ret.put(ReturnValues.SNIPLETT, html.toString());
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return with Snipplet
     * @throws EFapsException on error
     */
    public Return getJavaScript4TaxDocUIValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final StringBuilder js = new StringBuilder()
            .append("<script type=\"text/javascript\">\n")
            .append("require([\"dojo/ready\"], function(ready) {")
            .append("ready(1600, function(){\n")
            .append("function deac(_key, _dis) {\n")
            .append("require([\"dojo/query\"], function(query){\n")
            .append("query(\"input[name^=\\\"\" + _key + \"\\\"]\").forEach(function(node) {")
            .append("if (node.type==='text' || node.type === 'number') {")
            .append("node.disabled = _dis ? '' : 'disabled';")
            .append("}")
            .append("});")
            .append("query(\"select[name^=\\\"\" + _key + \"\\\"]\").forEach(function(node) {")
            .append("node.disabled = _dis ? '' : 'disabled';")
            .append("});")
            .append("});")
            .append("}")
            .append("require([\"dojo/query\"], function(query){\n")
            .append("query(\"input[name$=\\\"Checkbox\\\"] \").forEach(function(_node) {")
            .append("if (!_node.checked) {")
            .append("deac(_node.name.substring(0,_node.name.length-8));")
            .append("}")
            .append("});")
            .append("});")
            .append("require([\"dojo/query\"], function(query){\n")
            .append("query(\"input[name$=\\\"Checkbox\\\"] \").on(\"click\", function(evt) {")
            .append("var key = evt.currentTarget.name.substring(0,evt.currentTarget.name.length-8);")
            .append("deac(key, evt.currentTarget.checked );")
            .append("});")
            .append("});")
            .append("});")
            .append("});")
            .append("\n</script>\n");
        retVal.put(ReturnValues.SNIPLETT, js.toString());
        return retVal;
    }

    @Override
    public CIType getCIType()
        throws EFapsException
    {
        return CISales.IncomingInvoice;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return containing maplist
     * @throws EFapsException on error
     */
    public Return recievingTicketMultiPrint(final Parameter _parameter)
        throws EFapsException
    {
        final MultiPrint multi = new MultiPrint()
        {

            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
                throws EFapsException
            {
                super.add2QueryBldr(_parameter, _queryBldr);
                final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.DocumentAbstract);
                attrQueryBldr.addWhereAttrEqValue(CISales.DocumentAbstract.ID, _parameter.getInstance());
                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.DocumentAbstract.Contact);
                _queryBldr.addWhereAttrInQuery(CISales.DocumentAbstract.Contact, attrQuery);
            }
        };
        return multi.execute(_parameter);
    }

    @Override
    public Return validate(final Parameter _parameter)
        throws EFapsException
    {
        final Validation validation = new Validation()
        {
            @Override
            protected List<IWarning> validate(final Parameter _parameter,
                                              final List<IWarning> _warnings)
                throws EFapsException
            {
                final List<IWarning> ret = super.validate(_parameter, _warnings);
                ret.addAll(validatTaxDoc(_parameter));
                return ret;
            }
        };
        return validation.validate(_parameter, this);
    }

    /**
     * Validate that the given quantities have numbers bigger than Zero.
     *
     * @param _parameter Parameter as passed by the eFasp API
     * @return List of warnings, empty list if no warning
     * @throws EFapsException on error
     */
    public List<IWarning> validatTaxDoc(final Parameter _parameter)
        throws EFapsException
    {
        final List<IWarning> ret = new ArrayList<IWarning>();
        if ("false".equalsIgnoreCase(_parameter
                        .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.headingTaxDoc.name))) {
            final boolean isPerception = "true".equalsIgnoreCase(_parameter
                            .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.perceptionCheckbox.name));
            final boolean isRetention = "true".equalsIgnoreCase(_parameter
                            .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.retentionCheckbox.name));
            final boolean isDetraction = "true".equalsIgnoreCase(_parameter
                            .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.detractionCheckbox.name));
            if (BooleanUtils.toInteger(isPerception) + BooleanUtils.toInteger(isRetention) + BooleanUtils
                            .toInteger(isDetraction) > 1) {
                ret.add(new OnlyOneTaxDocWarning());
            }

            if (isPerception && Sales.PERCEPTIONCERTIFICATEACTIVATE.get()) {
                final Instance inst = Instance.get(_parameter
                                .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.contact.name));
                final PrintQuery print = new PrintQuery(inst);
                final SelectBuilder sel = SelectBuilder.get().clazz(CISales.Contacts_ClassTaxinfo)
                                .attribute(CISales.Contacts_ClassTaxinfo.Perception);
                print.addSelect(sel);
                print.executeWithoutAccessCheck();
                final Sales.TaxPerception perc = print.getSelect(sel);
                if (perc != null && perc.equals(Sales.TaxPerception.AGENT)) {
                    ret.add(new ContactIsPerceptionAgentWarning());
                }
            }

            if (isRetention && Sales.RETENTIONCERTIFICATEACTIVATE.get()) {
                final Instance inst = Instance.get(_parameter
                                .getParameterValue(CIFormSales.Sales_IncomingInvoiceForm.contact.name));
                final PrintQuery print = new PrintQuery(inst);
                final SelectBuilder sel = SelectBuilder.get().clazz(CISales.Contacts_ClassTaxinfo)
                                .attribute(CISales.Contacts_ClassTaxinfo.Retention);
                print.addSelect(sel);
                print.executeWithoutAccessCheck();
                final Sales.TaxRetention perc = print.getSelect(sel);
                if (perc != null && perc.equals(Sales.TaxRetention.AGENT)) {
                    ret.add(new ContactIsRetentionAgentWarning());
                }
            }
        }
        return ret;
    }

    /**
     * Warning class.
     */
    public static class OnlyOneTaxDocWarning
        extends AbstractWarning
    {
        /**
         * Constructor.
         */
        public OnlyOneTaxDocWarning()
        {
            setError(true);
        }
    }

    /**
     * Warning class.
     */
    public static class ContactIsPerceptionAgentWarning
        extends AbstractWarning
    {

        /**
         * Constructor.
         */
        public ContactIsPerceptionAgentWarning()
        {
            setError(true);
        }
    }

    /**
     * Warning class.
     */
    public static class ContactIsRetentionAgentWarning
        extends AbstractWarning
    {
        /**
         * Constructor.
         */
        public ContactIsRetentionAgentWarning()
        {
            setError(true);
        }
    }
}
