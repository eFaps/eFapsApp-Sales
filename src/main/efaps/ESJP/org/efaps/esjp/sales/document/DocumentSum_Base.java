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
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.RateUI;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.admin.ui.field.Field;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Delete;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uisearch.Search;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.CurrencyInst_Base;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.erp.RateInfo;
import org.efaps.esjp.sales.Calculator;
import org.efaps.esjp.sales.Payment;
import org.efaps.esjp.sales.Payment_Base;
import org.efaps.esjp.sales.Payment_Base.OpenAmount;
import org.efaps.esjp.sales.PriceUtil;
import org.efaps.esjp.sales.tax.Tax;
import org.efaps.esjp.sales.tax.TaxesAttribute;
import org.efaps.esjp.sales.tax.xml.TaxEntry;
import org.efaps.esjp.sales.tax.xml.Taxes;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.SalesSettings;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;
import org.efaps.util.cache.CacheReloadException;

/**
 * Class is the generic instance for all documents of type DocumentSum.
 *
 * @author The eFaps Team
 * @version $Id: DocumentSum_Base.java 7915 2012-08-17 15:30:12Z
 *          m.aranya@moxter.net $
 */
/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("e177ab08-67f0-4ce2-8eff-d3f167352bee")
@EFapsRevision("$Rev$")
public abstract class DocumentSum_Base
    extends AbstractDocument
{
    /**
     * Key to the Calculator.
     */
    public static final String CALCULATORS_VALUE = "org.efaps.esjp.sales.document.DocumentSum.CalculatorValue";

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return return granting access or not
     * @throws EFapsException on error
     */
    public Return accessCheck4NetUnitPrice(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Field field = (Field) _parameter.get(ParameterValues.UIOBJECT);
        if ((field.isEditableDisplay(TargetMode.CREATE) && !isIncludeMinRetail(_parameter))
                        || (field.isReadonlyDisplay(TargetMode.CREATE) && isIncludeMinRetail(_parameter))) {
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
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
        _editDoc.addValue(DocumentSum_Base.CALCULATORS_VALUE, calcList);
        final Instance baseCurrInst = Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);
        final Instance rateCurrInst = getRateCurrencyInstance(_parameter, _editDoc);

        final Object[] rateObj = getRateObject(_parameter);
        final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                        BigDecimal.ROUND_HALF_UP);

        final Update update = new Update(_editDoc.getInstance());
        final String name = getDocName4Edit(_parameter);
        if (name != null) {
            update.add(CISales.DocumentSumAbstract.Name, name);
            _editDoc.getValues().put(CISales.DocumentSumAbstract.Name.name, name);
        }

        final String date = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentSumAbstract.Date.name));
        if (date != null) {
            update.add(CISales.DocumentSumAbstract.Date, date);
            _editDoc.getValues().put(CISales.DocumentSumAbstract.Date.name, date);
        }
        final String duedate = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentSumAbstract.DueDate.name));
        if (duedate != null) {
            update.add(CISales.DocumentSumAbstract.DueDate, duedate);
            _editDoc.getValues().put(CISales.DocumentSumAbstract.DueDate.name, duedate);
        }

        final String note = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentSumAbstract.Note.name));
        if (note != null) {
            update.add(CISales.DocumentSumAbstract.Note, note);
            _editDoc.getValues().put(CISales.DocumentSumAbstract.Note.name, note);
        }

        final String salesperson = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentSumAbstract.Salesperson.name));
        if (salesperson != null) {
            update.add(CISales.DocumentSumAbstract.Salesperson, salesperson);
            _editDoc.getValues().put(CISales.DocumentSumAbstract.Salesperson.name, salesperson);
        }

        final String groupId = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentSumAbstract.Group.name));
        if (groupId != null) {
            update.add(CISales.DocumentSumAbstract.Group, groupId);
            _editDoc.getValues().put(CISales.DocumentSumAbstract.Group.name, groupId);
        }

        if (_editDoc.getInstance().getType().isKindOf(CISales.DocumentSumAbstract.getType())) {
            final DecimalFormat frmt = NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter));
            final int scale = frmt.getMaximumFractionDigits();
            final BigDecimal rateCrossTotal = getCrossTotal(_parameter, calcList)
                            .setScale(scale, BigDecimal.ROUND_HALF_UP);
            update.add(CISales.DocumentSumAbstract.RateCrossTotal, rateCrossTotal);
            _editDoc.getValues().put(CISales.DocumentSumAbstract.RateCrossTotal.name, rateCrossTotal);

            final BigDecimal rateNetTotal = getNetTotal(_parameter, calcList).setScale(scale, BigDecimal.ROUND_HALF_UP);
            update.add(CISales.DocumentSumAbstract.RateNetTotal, rateNetTotal);
            _editDoc.getValues().put(CISales.DocumentSumAbstract.RateNetTotal.name, rateNetTotal);

            update.add(CISales.DocumentSumAbstract.RateDiscountTotal, BigDecimal.ZERO);
            update.add(CISales.DocumentSumAbstract.RateTaxes, getRateTaxes(_parameter, calcList, rateCurrInst));
            update.add(CISales.DocumentSumAbstract.Taxes, getTaxes(_parameter, calcList, rate, baseCurrInst));

            final BigDecimal crossTotal = getCrossTotal(_parameter, calcList).divide(rate, BigDecimal.ROUND_HALF_UP)
                            .setScale(scale, BigDecimal.ROUND_HALF_UP);
            update.add(CISales.DocumentSumAbstract.CrossTotal, crossTotal);
            _editDoc.getValues().put(CISales.DocumentSumAbstract.CrossTotal.name, crossTotal);

            final BigDecimal netTotal = getNetTotal(_parameter, calcList).divide(rate, BigDecimal.ROUND_HALF_UP)
                            .setScale(scale, BigDecimal.ROUND_HALF_UP);
            update.add(CISales.DocumentSumAbstract.NetTotal, netTotal);
            _editDoc.getValues().put(CISales.DocumentSumAbstract.CrossTotal.name, netTotal);

            update.add(CISales.DocumentSumAbstract.DiscountTotal, BigDecimal.ZERO);

            update.add(CISales.DocumentSumAbstract.CurrencyId, baseCurrInst);
            update.add(CISales.DocumentSumAbstract.Rate, rateObj);
            update.add(CISales.DocumentSumAbstract.RateCurrencyId, rateCurrInst);

            _editDoc.getValues().put(CISales.DocumentSumAbstract.CurrencyId.name, baseCurrInst);
            _editDoc.getValues().put(CISales.DocumentSumAbstract.RateCurrencyId.name, rateCurrInst);
            _editDoc.getValues().put(CISales.DocumentSumAbstract.Rate.name, rateObj);
        }
        addStatus2DocEdit(_parameter, update, _editDoc);
        add2DocEdit(_parameter, update, _editDoc);
        update.execute();

        Context.getThreadContext().removeSessionAttribute(AbstractDocument_Base.CURRENCYINST_KEY);

        return _editDoc;
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @param _createdDoc createdDoc
     * @return Instance of the document.
     * @throws EFapsException on error.
     */
    protected Instance getRateCurrencyInstance(final Parameter _parameter,
                                               final CreatedDoc _createdDoc)
        throws CacheReloadException, EFapsException
    {
        return _parameter.getParameterValue("rateCurrencyId") == null
                        ? Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE)
                        : Instance.get(CIERP.Currency.getType(), _parameter.getParameterValue("rateCurrencyId"));
    }

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

        final List<Calculator> calcList = analyseTable(_parameter, null);
        createdDoc.addValue(DocumentSum_Base.CALCULATORS_VALUE, calcList);
        final Instance baseCurrInst = Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);
        final Instance rateCurrInst = getRateCurrencyInstance(_parameter, createdDoc);

        final Object[] rateObj = getRateObject(_parameter);
        final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                        BigDecimal.ROUND_HALF_UP);

        final Insert insert = new Insert(getType4DocCreate(_parameter));
        final String name = getDocName4Create(_parameter);
        insert.add(CISales.DocumentSumAbstract.Name, name);
        createdDoc.getValues().put(CISales.DocumentSumAbstract.Name.name, name);
        final String date = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentSumAbstract.Date.name));
        if (date != null) {
            insert.add(CISales.DocumentSumAbstract.Date, date);
            createdDoc.getValues().put(CISales.DocumentSumAbstract.Date.name, date);
        }
        final String duedate = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentSumAbstract.DueDate.name));
        if (duedate != null) {
            insert.add(CISales.DocumentSumAbstract.DueDate, duedate);
            createdDoc.getValues().put(CISales.DocumentSumAbstract.DueDate.name, duedate);
        }
        final String contact = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentSumAbstract.Contact.name));
        if (contact != null) {
            final Instance inst = Instance.get(contact);
            if (inst.isValid()) {
                insert.add(CISales.DocumentSumAbstract.Contact, inst.getId());
                createdDoc.getValues().put(CISales.DocumentSumAbstract.Contact.name, inst);
            }
        }
        final String note = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentSumAbstract.Note.name));
        if (note != null) {
            insert.add(CISales.DocumentSumAbstract.Note, note);
            createdDoc.getValues().put(CISales.DocumentSumAbstract.Note.name, note);
        }
        final String revision = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentSumAbstract.Revision.name));
        if (revision != null) {
            insert.add(CISales.DocumentSumAbstract.Revision, revision);
            createdDoc.getValues().put(CISales.DocumentSumAbstract.Revision.name, revision);
        }
        final String salesperson = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentSumAbstract.Salesperson.name));
        if (salesperson != null) {
            insert.add(CISales.DocumentSumAbstract.Salesperson, salesperson);
            createdDoc.getValues().put(CISales.DocumentSumAbstract.Salesperson.name, salesperson);
        }

        final String groupId = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentSumAbstract.Group.name));
        if (groupId != null) {
            insert.add(CISales.DocumentSumAbstract.Group, groupId);
            createdDoc.getValues().put(CISales.DocumentSumAbstract.Group.name, groupId);
        }

        if (getType4DocCreate(_parameter).isKindOf(CISales.DocumentSumAbstract.getType())) {
            final DecimalFormat frmt = NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter));
            final int scale = frmt.getMaximumFractionDigits();
            final BigDecimal rateCrossTotal = getCrossTotal(_parameter, calcList)
                            .setScale(scale, BigDecimal.ROUND_HALF_UP);
            insert.add(CISales.DocumentSumAbstract.RateCrossTotal, rateCrossTotal);
            createdDoc.getValues().put(CISales.DocumentSumAbstract.RateCrossTotal.name, rateCrossTotal);

            final BigDecimal rateNetTotal = getNetTotal(_parameter, calcList).setScale(scale, BigDecimal.ROUND_HALF_UP);
            insert.add(CISales.DocumentSumAbstract.RateNetTotal, rateNetTotal);
            createdDoc.getValues().put(CISales.DocumentSumAbstract.RateNetTotal.name, rateNetTotal);

            insert.add(CISales.DocumentSumAbstract.RateDiscountTotal, BigDecimal.ZERO);
            insert.add(CISales.DocumentSumAbstract.RateTaxes, getRateTaxes(_parameter, calcList, rateCurrInst));
            insert.add(CISales.DocumentSumAbstract.Taxes, getTaxes(_parameter, calcList, rate, baseCurrInst));

            final BigDecimal crossTotal = getCrossTotal(_parameter, calcList).divide(rate, BigDecimal.ROUND_HALF_UP)
                            .setScale(scale, BigDecimal.ROUND_HALF_UP);
            insert.add(CISales.DocumentSumAbstract.CrossTotal, crossTotal);
            createdDoc.getValues().put(CISales.DocumentSumAbstract.CrossTotal.name, crossTotal);

            final BigDecimal netTotal = getNetTotal(_parameter, calcList).divide(rate, BigDecimal.ROUND_HALF_UP)
                            .setScale(scale, BigDecimal.ROUND_HALF_UP);
            insert.add(CISales.DocumentSumAbstract.NetTotal, netTotal);
            createdDoc.getValues().put(CISales.DocumentSumAbstract.CrossTotal.name, netTotal);

            insert.add(CISales.DocumentSumAbstract.DiscountTotal, BigDecimal.ZERO);

            insert.add(CISales.DocumentSumAbstract.CurrencyId, baseCurrInst);
            insert.add(CISales.DocumentSumAbstract.Rate, rateObj);
            insert.add(CISales.DocumentSumAbstract.RateCurrencyId, rateCurrInst);

            createdDoc.getValues().put(CISales.DocumentSumAbstract.CurrencyId.name, baseCurrInst);
            createdDoc.getValues().put(CISales.DocumentSumAbstract.RateCurrencyId.name, rateCurrInst);
            createdDoc.getValues().put(CISales.DocumentSumAbstract.Rate.name, rateObj);
        }

        addStatus2DocCreate(_parameter, insert, createdDoc);
        add2DocCreate(_parameter, insert, createdDoc);
        insert.execute();

        createdDoc.setInstance(insert.getInstance());

        Context.getThreadContext().removeSessionAttribute(AbstractDocument_Base.CURRENCYINST_KEY);
        return createdDoc;
    }

    /**
     * Method to connect the document with the selected document type.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _createdDoc CreatedDoc
     * @throws EFapsException on error
     */
    protected void connect2DocumentType(final Parameter _parameter,
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
     * Method is executed as an update event of the field containing the
     * quantity of products to calculate the new totals.
     *
     * @param _parameter Parameter as passed by the eFasp API
     * @return Return containing the list
     * @throws EFapsException on error
     */
    public Return updateFields4Quantity(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();

        final int selected = getSelectedRow(_parameter);

        final List<Calculator> calcList = analyseTable(_parameter, null);

        final Calculator cal = calcList.get(selected);
        if (calcList.size() > 0) {
            add2Map4UpdateField(_parameter, map, calcList, cal);
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        }
        return retVal;
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _map          Map the values will be added to
     * @param _calcList      list of all calculators
     * @param _cal           current calculator
     * @throws EFapsException on error
     */
    protected void add2Map4UpdateField(final Parameter _parameter,
                                       final Map<String, String> _map,
                                       final List<Calculator> _calcList,
                                       final Calculator _cal)
        throws EFapsException
    {
        // positions
        if (_cal != null) {
            _map.put("quantity", _cal.getQuantityStr());
            _map.put("netUnitPrice", _cal.getNetUnitPriceFmtStr(NumberFormatter.get().getFrmt4UnitPrice(
                                            getTypeName4SysConf(_parameter))));
            _map.put("netPrice", _cal.getNetPriceFmtStr(NumberFormatter.get().getFrmt4Total(
                            getTypeName4SysConf(_parameter))));
            _map.put("discountNetUnitPrice", _cal.getDiscountNetUnitPriceFmtStr(NumberFormatter.get().getFrmt4UnitPrice(
                                            getTypeName4SysConf(_parameter))));
            _map.put("discount", _cal.getDiscountFmtStr(NumberFormatter.get().getFrmt4Discount(
                            getTypeName4SysConf(_parameter))));
        }
        // totals
        _map.put("netTotal", getNetTotalFmtStr(_parameter, _calcList));
        _map.put("crossTotal", getCrossTotalFmtStr(_parameter, _calcList));

        final StringBuilder js = new StringBuilder();
        if (_map.containsKey(EFapsKey.FIELDUPDATE_JAVASCRIPT.getKey())) {
            js.append(_map.get(EFapsKey.FIELDUPDATE_JAVASCRIPT.getKey())).append("\n");
        }
        js.append("require([\"dojo/query\", \"dojo/NodeList-manipulate\"], function(query){")
            .append("query(\"[name='taxes']\").innerHTML(\"")
            .append(new TaxesAttribute().getUI4ReadOnly(getRateTaxes(_parameter, _calcList, null)))
            .append("\");")
            .append("});");

        _map.put(EFapsKey.FIELDUPDATE_JAVASCRIPT.getKey(), js.toString());

        if (Sales.getSysConfig().getAttributeValueAsBoolean(SalesSettings.PERCEPTION)) {
            _map.put("perceptionTotal", getPerceptionTotalFmtStr(_parameter, _calcList));
        }
    }

    /**
     * Method is executed as an update event of the field containing the net
     * unit price for products to calculate the new totals.
     *
     * @param _parameter Parameter as passed by the eFasp API
     * @return Return containing the list
     * @throws EFapsException on error
     */
    public Return updateFields4NetUnitPrice(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();

        final int selected = getSelectedRow(_parameter);
        final List<Calculator> calcList = analyseTable(_parameter, null);
        final Calculator cal = calcList.get(selected);
        if (calcList.size() > 0) {
            add2Map4UpdateField(_parameter, map, calcList, cal);
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        }
        return retVal;
    }

    /**
     * Method is executed as an update event of the field containing the
     * discount for products to calculate the new totals.
     *
     * @param _parameter Parameter as passed by the eFasp API
     * @return Return containing the list
     * @throws EFapsException on error
     */
    public Return updateFields4Discount(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();
        final int selected = getSelectedRow(_parameter);

        final List<Calculator> calcList = analyseTable(_parameter, null);
        final Calculator cal = calcList.get(selected);
        if (calcList.size() > 0) {
            add2Map4UpdateField(_parameter, map, calcList, cal);
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        }
        return retVal;
    }

    /**
     * Method to update the fields on leaving the product field.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return map list with values
     * @throws EFapsException on errro
     */
    public Return updateFields4Product(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();

        final int selected = getSelectedRow(_parameter);
        final Field field = (Field) _parameter.get(ParameterValues.UIOBJECT);
        final String fieldName = field.getName();
        final String oid = _parameter.getParameterValues(fieldName)[selected];
        String name;
        // validate that a product was selected
        if (oid.length() > 0) {
            final PrintQuery print = new PrintQuery(oid);
            print.addAttribute(CIProducts.ProductAbstract.Name);
            print.execute();
            name = print.getAttribute(CIProducts.ProductAbstract.Name);
        } else {
            name = "";
        }
        if (name.length() > 0) {
            final List<Calculator> calcList = analyseTable(_parameter, selected);
            if (calcList.size() > 0) {
                final Calculator cal = calcList.get(selected);
                add2Map4UpdateField(_parameter, map, calcList, cal);
                map.put(fieldName + "AutoComplete", name);
                list.add(map);
                retVal.put(ReturnValues.VALUES, list);
            }
        } else {
            map.put(fieldName + "AutoComplete", name);
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
            final StringBuilder js = new StringBuilder();
            js.append("document.getElementsByName('").append(fieldName).append("AutoComplete')[").append(selected)
                .append("].focus()");
            map.put(EFapsKey.FIELDUPDATE_JAVASCRIPT.getKey(), js.toString());
        }
        return retVal;
    }

    /**
     * @param _parameter Paraemter as passed by the eFasp API
     * @return List map for the update event
     * @throws EFapsException on error
     */

    public Return updateFields4Uom(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();

        final int selected = getSelectedRow(_parameter);
        final List<Calculator> calcList = analyseTable(_parameter, null);
        if (calcList.size() > 0) {
            final Calculator cal = calcList.get(selected);

            final Long uomID = Long.parseLong(_parameter.getParameterValues("uoM")[selected]);
            final UoM uom = Dimension.getUoM(uomID);
            final BigDecimal up = cal.getProductPrice().getCurrentPrice().multiply(new BigDecimal(uom.getNumerator()))
                            .divide(new BigDecimal(uom.getDenominator()));
            cal.setUnitPrice(up);
            add2Map4UpdateField(_parameter, map, calcList, cal);
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        }
        return retVal;
    }

    @Override
    protected StringBuilder getJavaScript4Positions(final Parameter _parameter,
                                                    final List<Instance> _instances)
        throws EFapsException
    {
        return super.getJavaScript4Positions(_parameter, _instances).append("executeCalculator();");
    }


    /**
     * Update the form after change of rate currency.
     *
     * @param _parameter Parameter as passed by the eFaps API for esjp
     * @return javascript for update
     * @throws EFapsException on error
     */
    public Return updateFields4RateCurrency(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();

        final Instance newInst = getRateCurrencyInstance(_parameter, null);
        final Map<String, String> map = new HashMap<String, String>();
        Instance currentInst = (Instance) Context.getThreadContext().getSessionAttribute(
                        AbstractDocument_Base.CURRENCYINST_KEY);
        final Instance baseInst = Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);
        if (currentInst == null) {
            currentInst = baseInst;
        }
        Context.getThreadContext().setSessionAttribute(AbstractDocument_Base.CURRENCYINST_KEY, newInst);

        if (!newInst.equals(currentInst)) {
            final Currency currency = new Currency();
            final RateInfo[] rateInfos = currency.evaluateRateInfos(_parameter,
                            _parameter.getParameterValue("date_eFapsDate"), currentInst, newInst);

            final List<Calculator> calculators = analyseTable(_parameter, null);

            final StringBuilder js = new StringBuilder();
            int i = 0;
            final Map<Integer, Map<String, String>> values = new TreeMap<Integer, Map<String, String>>();
            for (final Calculator calculator : calculators) {
                final Map<String, String> map2 = new HashMap<String, String>();
                if (!calculator.isEmpty()) {
                    calculator.applyRate(newInst, rateInfos[2].getRate());
                    map2.put("netUnitPrice", calculator.getNetUnitPriceFmtStr(NumberFormatter.get().getFrmt4UnitPrice(
                                                    getTypeName4SysConf(_parameter))));
                    map2.put("netPrice", calculator.getNetPriceFmtStr(NumberFormatter.get().getFrmt4Total(
                                                    getTypeName4SysConf(_parameter))));
                    map2.put("discountNetUnitPrice", calculator.getDiscountNetUnitPriceFmtStr(
                                    NumberFormatter.get().getFrmt4UnitPrice(getTypeName4SysConf(_parameter))));
                    values.put(i, map2);
                }
                i++;
            }

            final Set<String> noEscape = new HashSet<String>();
            add2SetValuesString4Postions4CurrencyUpdate(_parameter, calculators, values, noEscape);
            js.append(getSetFieldValuesScript(_parameter, values.values(), noEscape));

            if (calculators.size() > 0) {
                js.append(getSetFieldValue(0, "crossTotal", getCrossTotalFmtStr(_parameter, calculators)))
                    .append(getSetFieldValue(0, "netTotal", getNetTotalFmtStr(_parameter, calculators)))
                    .append(addFields4RateCurrency(_parameter, calculators));
                if (_parameter.getParameterValue("openAmount") != null) {
                    js.append(getSetFieldValue(0, "openAmount", getBaseCrossTotalFmtStr(_parameter, calculators)));
                }
                if (Sales.getSysConfig().getAttributeValueAsBoolean(SalesSettings.PERCEPTION)) {
                    js.append(getSetFieldValue(0, "perceptionTotal",
                                    getPerceptionTotalFmtStr(_parameter, calculators)));
                }
            }
            js.append(getSetFieldValue(0, "rateCurrencyData", rateInfos[1].getRateUIFrmt()))
                .append(getSetFieldValue(0, "rate", rateInfos[1].getRateUIFrmt()))
                .append(getSetFieldValue(0, "rate" + RateUI.INVERTEDSUFFIX,
                                Boolean.toString(rateInfos[1].getCurrencyInst().isInvert())))
                .append(addAdditionalFields4CurrencyUpdate(_parameter, calculators));

            map.put(EFapsKey.FIELDUPDATE_JAVASCRIPT.getKey(), js.toString());
            list.add(map);
        }

        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * Method to set the openAmount into the session cache.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _calcList List of <code>Calculator</code>
     * @throws EFapsException on error
     */

    protected void setOpenAmount(final Parameter _parameter,
                                 final List<Calculator> _calcList)
        throws EFapsException
    {
        final Instance curInst = (Instance) Context.getThreadContext().getSessionAttribute(
                        AbstractDocument_Base.CURRENCYINST_KEY);
        final OpenAmount openAmount = new Payment().new OpenAmount(new CurrencyInst(curInst),
                        getCrossTotal(_parameter, _calcList),
                        new PriceUtil().getDateFromParameter(_parameter));
        Context.getThreadContext().setSessionAttribute(Payment_Base.OPENAMOUNT_SESSIONKEY, openAmount);
    }


    /**
     * Method to set additional fields for the currency update method.
     *
     * @param _parameter Paramter as passed by the eFaps API
     * @param _calculators list of calculators
     * @return new StringBuilder with the additional fields.
     * @throws EFapsException on error
     */
    protected StringBuilder addAdditionalFields4CurrencyUpdate(final Parameter _parameter,
                                                               final List<Calculator> _calculators)
        throws EFapsException
    {
        // to be used by implementations
        return new StringBuilder();
    }

    /**
     * @param _parameter Paramter as passed by the eFaps API
     * @param _calculators list of calculators
     * @param _values values to be added to
     * @param _noEscape no escape fields
     * @throws EFapsException on error
     */
    protected void add2SetValuesString4Postions4CurrencyUpdate(final Parameter _parameter,
                                                               final List<Calculator> _calculators,
                                                               final Map<Integer, Map<String, String>> _values,
                                                               final Set<String> _noEscape)
        throws EFapsException
    {
        // to be used by implementations
    }

    /**
     * Update the form after change of date.
     *
     * @param _parameter Parameter as passed by the eFaps API for esjp
     * @return javascript for update
     * @throws EFapsException on error
     */
    public Return updateFields4RateCurrencyFromDate(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();
        final Instance newInst = getRateCurrencyInstance(_parameter, null);
        Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);

        final Currency currency = new Currency();
        final RateInfo rateInfo = currency.evaluateRateInfo(_parameter, _parameter.getParameterValue("date_eFapsDate"),
                        newInst);

        final List<Calculator> calculators = analyseTable(_parameter, null);

        final StringBuilder js = new StringBuilder();
        int i = 0;
        final Map<Integer, Map<String, String>> values = new TreeMap<Integer, Map<String, String>>();
        for (final Calculator calculator : calculators) {
            final Map<String, String> map2 = new HashMap<String, String>();
            if (!calculator.isEmpty()) {
                final QueryBuilder qlb = new QueryBuilder(CISales.PositionAbstract);
                qlb.addWhereAttrEqValue(CISales.PositionAbstract.Product, Instance.get(calculator.getOid()));
                qlb.addWhereAttrEqValue(CISales.PositionAbstract.DocumentAbstractLink, _parameter.getInstance());
                final InstanceQuery query = qlb.getQuery();
                query.execute();
                if (!query.next()) {
                    calculator.applyRate(newInst, rateInfo.getRate());
                }
                map2.put("netUnitPrice", calculator.getNetUnitPriceFmtStr(NumberFormatter.get().getFrmt4UnitPrice(
                                getTypeName4SysConf(_parameter))));
                map2.put("netPrice", calculator.getNetPriceFmtStr(NumberFormatter.get().getFrmt4Total(
                                getTypeName4SysConf(_parameter))));
                map2.put("discountNetUnitPrice", calculator.getDiscountNetUnitPriceFmtStr(
                                NumberFormatter.get().getFrmt4UnitPrice(getTypeName4SysConf(_parameter))));
                values.put(i, map2);

            }
            i++;
        }

        final Set<String> noEscape = new HashSet<String>();
        add2SetValuesString4Postions4CurrencyUpdate(_parameter, calculators, values, noEscape);
        js.append(getSetFieldValuesScript(_parameter, values.values(), noEscape));

        if (calculators.size() > 0) {
            js.append(getSetFieldValue(0, "crossTotal", getCrossTotalFmtStr(_parameter, calculators)))
                .append(getSetFieldValue(0, "netTotal", getNetTotalFmtStr(_parameter, calculators)))
                .append(addFields4RateCurrency(_parameter, calculators));
            if (_parameter.getParameterValue("openAmount") != null) {
                js.append(getSetFieldValue(0, "openAmount", getBaseCrossTotalFmtStr(_parameter, calculators)));
            }
        }
        js.append(getSetFieldValue(0, "rateCurrencyData", rateInfo.getRateUIFrmt()))
                        .append(getSetFieldValue(0, "rate", rateInfo.getRateUIFrmt()))
            .append(getSetFieldValue(0, "rate" + RateUI.INVERTEDSUFFIX,
                        Boolean.toString(rateInfo.getCurrencyInst().isInvert())))
                        .append(addAdditionalFields4CurrencyUpdate(_parameter, calculators));

        map.put(EFapsKey.FIELDUPDATE_JAVASCRIPT.getKey(), js.toString());
        list.add(map);

        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * Internal Method to create the positions for this Document.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @param _createdDoc cretaed Document
     * @throws EFapsException on error
     */
    protected void createPositions(final Parameter _parameter,
                                   final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final Instance baseCurrInst = Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);

        final Instance rateCurrInst = getRateCurrencyInstance(_parameter, _createdDoc);
        final Object[] rateObj = getRateObject(_parameter);
        final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                        BigDecimal.ROUND_HALF_UP);

        @SuppressWarnings("unchecked")
        final List<Calculator> calcList = (List<Calculator>) _createdDoc.getValue(DocumentSum_Base.CALCULATORS_VALUE);

        final DecimalFormat totalFrmt = NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter));
        final int scale = totalFrmt.getMaximumFractionDigits();

        final DecimalFormat unitFrmt = NumberFormatter.get().getFrmt4UnitPrice(getTypeName4SysConf(_parameter));
        final int uScale = unitFrmt.getMaximumFractionDigits();

        Integer idx = 0;
        for (final Calculator calc : calcList) {
            if (!calc.isEmpty()) {
                final Insert posIns = new Insert(getType4PositionCreate(_parameter));
                posIns.add(CISales.PositionAbstract.PositionNumber, idx + 1);
                posIns.add(CISales.PositionAbstract.DocumentAbstractLink, _createdDoc.getInstance().getId());

                final String[] product = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                                CISales.PositionAbstract.Product.name));
                if (product != null && product.length > idx) {
                    final Instance inst = Instance.get(product[idx]);
                    if (inst.isValid()) {
                        posIns.add(CISales.PositionAbstract.Product, inst.getId());
                    }
                }

                final String[] productDesc = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                                CISales.PositionAbstract.ProductDesc.name));
                if (productDesc != null && productDesc.length > idx) {
                    posIns.add(CISales.PositionAbstract.ProductDesc, productDesc[idx]);
                }

                final String[] uoM = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                                CISales.PositionAbstract.UoM.name));
                if (uoM != null && uoM.length > idx) {
                    posIns.add(CISales.PositionAbstract.UoM, uoM[idx]);
                }

                posIns.add(CISales.PositionSumAbstract.Quantity, calc.getQuantity());
                posIns.add(CISales.PositionSumAbstract.CrossUnitPrice, calc.getCrossUnitPrice()
                                .divide(rate, BigDecimal.ROUND_HALF_UP).setScale(uScale, BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.PositionSumAbstract.NetUnitPrice, calc.getNetUnitPrice()
                                .divide(rate, BigDecimal.ROUND_HALF_UP).setScale(uScale, BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.PositionSumAbstract.CrossPrice, calc.getCrossPrice()
                                .divide(rate, BigDecimal.ROUND_HALF_UP).setScale(scale, BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.PositionSumAbstract.NetPrice, calc.getNetPrice()
                                .divide(rate, BigDecimal.ROUND_HALF_UP).setScale(scale, BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.PositionSumAbstract.Tax, calc.getTaxCatId());
                posIns.add(CISales.PositionSumAbstract.Discount, calc.getDiscount());
                posIns.add(CISales.PositionSumAbstract.DiscountNetUnitPrice, calc.getDiscountNetUnitPrice()
                                .divide(rate, BigDecimal.ROUND_HALF_UP).setScale(uScale, BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.PositionSumAbstract.CurrencyId, baseCurrInst);
                posIns.add(CISales.PositionSumAbstract.Rate, rateObj);
                posIns.add(CISales.PositionSumAbstract.RateCurrencyId, rateCurrInst);
                posIns.add(CISales.PositionSumAbstract.RateNetUnitPrice, calc.getNetUnitPrice()
                                .setScale(uScale, BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.PositionSumAbstract.RateCrossUnitPrice, calc.getCrossUnitPrice()
                                .setScale(uScale, BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.PositionSumAbstract.RateDiscountNetUnitPrice, calc.getDiscountNetUnitPrice()
                                .setScale(uScale, BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.PositionSumAbstract.RateNetPrice,
                                calc.getNetPrice().setScale(scale, BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.PositionSumAbstract.RateCrossPrice,
                                calc.getCrossPrice().setScale(scale, BigDecimal.ROUND_HALF_UP));
                add2PositionInsert(_parameter, calc, posIns, idx);
                posIns.execute();
                _createdDoc.addPosition(posIns.getInstance());
            }
            idx++;
        }
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _calc Calculator
     * @param _posIns insert
     * @param _idx index
     * @throws EFapsException on error
     */
    protected void add2PositionInsert(final Parameter _parameter,
                                      final Calculator _calc,
                                      final Insert _posIns,
                                      final int _idx)
        throws EFapsException
    {
        // to be implemented by subclasses
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
        final Instance baseCurrInst = Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);
        final Instance rateCurrInst = getRateCurrencyInstance(_parameter, _editDoc);

        final Object[] rateObj = getRateObject(_parameter);
        final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                        BigDecimal.ROUND_HALF_UP);

        @SuppressWarnings("unchecked")
        final List<Calculator> calcList = (List<Calculator>) _editDoc.getValue(DocumentSum_Base.CALCULATORS_VALUE);
        @SuppressWarnings("unchecked")
        final Map<String, String> oidMap = (Map<String, String>) _parameter.get(ParameterValues.OIDMAP4UI);
        final String[] rowKeys = _parameter.getParameterValues(EFapsKey.TABLEROW_NAME.getKey());

        final DecimalFormat totalFrmt = NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter));
        final int scale = totalFrmt.getMaximumFractionDigits();

        final DecimalFormat unitFrmt = NumberFormatter.get().getFrmt4UnitPrice(getTypeName4SysConf(_parameter));
        final int uScale = unitFrmt.getMaximumFractionDigits();

        final Iterator<Calculator> iter = calcList.iterator();
        for (int i = 0; i < rowKeys.length; i++) {
            final Calculator calc = iter.next();
            final Instance inst = Instance.get(oidMap.get(rowKeys[i]));
            if (!calc.isEmpty()) {
                final Update update;
                if (inst.isValid()) {
                    update = new Update(inst);
                } else {
                    update = new Insert(getType4PositionUpdate(_parameter));
                }
                update.add(CISales.PositionAbstract.PositionNumber, i + 1);
                update.add(CISales.PositionAbstract.DocumentAbstractLink, _editDoc.getInstance());

                final String[] product = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                                CISales.PositionAbstract.Product.name));
                if (product != null && product.length > i) {
                    final Instance prodInst = Instance.get(product[i]);
                    if (prodInst.isValid()) {
                        update.add(CISales.PositionAbstract.Product, prodInst);
                    }
                }

                final String[] productDesc = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                                CISales.PositionAbstract.ProductDesc.name));
                if (productDesc != null && productDesc.length > i) {
                    update.add(CISales.PositionAbstract.ProductDesc, productDesc[i]);
                }

                final String[] uoM = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                                CISales.PositionAbstract.UoM.name));
                if (uoM != null && uoM.length > i) {
                    update.add(CISales.PositionAbstract.UoM, uoM[i]);
                }

                update.add(CISales.PositionSumAbstract.Quantity, calc.getQuantity());
                update.add(CISales.PositionSumAbstract.CrossUnitPrice, calc.getCrossUnitPrice()
                                .divide(rate, BigDecimal.ROUND_HALF_UP).setScale(uScale, BigDecimal.ROUND_HALF_UP));
                update.add(CISales.PositionSumAbstract.NetUnitPrice, calc.getNetUnitPrice()
                                .divide(rate, BigDecimal.ROUND_HALF_UP).setScale(uScale, BigDecimal.ROUND_HALF_UP));
                update.add(CISales.PositionSumAbstract.CrossPrice, calc.getCrossPrice()
                                .divide(rate, BigDecimal.ROUND_HALF_UP).setScale(scale, BigDecimal.ROUND_HALF_UP));
                update.add(CISales.PositionSumAbstract.NetPrice, calc.getNetPrice()
                                .divide(rate, BigDecimal.ROUND_HALF_UP).setScale(scale, BigDecimal.ROUND_HALF_UP));
                update.add(CISales.PositionSumAbstract.Tax, calc.getTaxCatId());
                update.add(CISales.PositionSumAbstract.Discount, calc.getDiscountStr());
                update.add(CISales.PositionSumAbstract.DiscountNetUnitPrice, calc.getDiscountNetUnitPrice()
                                .divide(rate, BigDecimal.ROUND_HALF_UP).setScale(uScale, BigDecimal.ROUND_HALF_UP));
                update.add(CISales.PositionSumAbstract.CurrencyId, baseCurrInst);
                update.add(CISales.PositionSumAbstract.Rate, rateObj);
                update.add(CISales.PositionSumAbstract.RateCurrencyId, rateCurrInst);
                update.add(CISales.PositionSumAbstract.RateNetUnitPrice, calc.getNetUnitPrice()
                                .setScale(uScale, BigDecimal.ROUND_HALF_UP));
                update.add(CISales.PositionSumAbstract.RateCrossUnitPrice, calc.getCrossUnitPrice()
                                .setScale(uScale, BigDecimal.ROUND_HALF_UP));
                update.add(CISales.PositionSumAbstract.RateDiscountNetUnitPrice, calc.getDiscountNetUnitPrice()
                                .setScale(uScale, BigDecimal.ROUND_HALF_UP));
                update.add(CISales.PositionSumAbstract.RateNetPrice,
                                calc.getNetPrice().setScale(scale, BigDecimal.ROUND_HALF_UP));
                update.add(CISales.PositionSumAbstract.RateCrossPrice,
                                calc.getCrossPrice().setScale(scale, BigDecimal.ROUND_HALF_UP));
                add2PositionUpdate(_parameter, calc, update, i);
                update.execute();
                _editDoc.addPosition(update.getInstance());
            }
        }
        deletePosition4Update(_parameter, _editDoc);
    }

    /**
     * Delete the positions of a Document that were removed in the UserInterface.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _editDoc  EditDoc the postions that will be updated belong to
     * @throws EFapsException on error
     */
    protected void deletePosition4Update(final Parameter _parameter,
                                         final EditedDoc _editDoc)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(getType4PositionUpdate(_parameter));
        queryBldr.addWhereAttrEqValue(CISales.PositionAbstract.DocumentAbstractLink, _editDoc.getInstance());
        final InstanceQuery query = queryBldr.getQuery();
        query.execute();
        final Set<Instance> delIns = new HashSet<Instance>();
        while (query.next()) {
            final Instance inst = query.getCurrentValue();
            if (!_editDoc.getPositions().contains(inst)) {
                delIns.add(inst);
            }
        }
        for (final Instance inst : delIns) {
            final Delete delete = new Delete(inst);
            delete.execute();
        }
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _calc Calculator
     * @param _posUpdate Update
     * @param _idx index
     * @throws EFapsException on error
     */
    protected void add2PositionUpdate(final Parameter _parameter,
                                      final Calculator _calc,
                                      final Update _posUpdate,
                                      final int _idx)
        throws EFapsException
    {
        // to be used by implenentation
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API
     * @return Return containing list
     * @throws EFapsException on error
     */
    public Return executeCalculatorOnScript(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final List<Calculator> calcList = analyseTable(_parameter, null);
        int i = 0;
        for (final Calculator cal : calcList) {
            final Map<String, String> map = new HashMap<String, String>();
            _parameter.getParameters().put("eFapsRowSelectedRow", new String[] { "" + i });
            add2Map4UpdateField(_parameter, map, calcList, cal);
            list.add(map);
            i++;
        }
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * Recalculate the rate values by instantiating calculators
     * and simulating the interaction with the form.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return map list for update event
     * @throws EFapsException on error
     */
    public Return recalculateRate(final Parameter _parameter)
        throws EFapsException
    {
        final Instance docInst = _parameter.getInstance();
        if (docInst.getType().isKindOf(CISales.DocumentSumAbstract.getType())) {
            final PrintQuery print = new PrintQuery(docInst);
            final SelectBuilder selRateCurInst = SelectBuilder.get()
                            .linkto(CISales.DocumentSumAbstract.RateCurrencyId).instance();
            print.addSelect(selRateCurInst);
            print.execute();

            final Instance baseCurrInst = Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);
            final Instance rateCurrInst = print.<Instance>getSelect(selRateCurInst);
            if (!baseCurrInst.equals(rateCurrInst)) {

                final String dateStr = _parameter.getParameterValue(
                                CIFormSales.Sales_DocumentSum_RecalculateForm.date.name + "_eFapsDate");

                final Currency currency = new Currency();
                final RateInfo rateInfo = currency.evaluateRateInfo(_parameter, dateStr, rateCurrInst);
                final BigDecimal rate = rateInfo.getRate();

                final DecimalFormat frmt = NumberFormatter.get().getFormatter();

                final DecimalFormat totalFrmt = NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter));
                final int scale = totalFrmt.getMaximumFractionDigits();

                final DecimalFormat unitFrmt = NumberFormatter.get().getFrmt4UnitPrice(getTypeName4SysConf(_parameter));
                final int uScale = unitFrmt.getMaximumFractionDigits();

                final List<Calculator> calcList = new ArrayList<Calculator>();
                final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionSumAbstract);
                queryBldr.addWhereAttrEqValue(CISales.PositionSumAbstract.DocumentAbstractLink, docInst);
                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder selProdOid = SelectBuilder.get().linkto(CISales.PositionSumAbstract.Product).oid();
                multi.addSelect(selProdOid);
                multi.addAttribute(CISales.PositionSumAbstract.Quantity,
                                CISales.PositionSumAbstract.RateNetUnitPrice,
                                CISales.PositionSumAbstract.Discount);
                multi.execute();
                while (multi.next()) {
                    //read the ratevalues
                    final BigDecimal quantity = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.Quantity);
                    final BigDecimal unitPrice = multi
                                    .<BigDecimal>getAttribute(CISales.PositionSumAbstract.RateNetUnitPrice);
                    final BigDecimal discount = multi
                                    .<BigDecimal>getAttribute(CISales.PositionSumAbstract.Discount);
                    final String prodOid = multi.<String>getSelect(selProdOid);
                    final Calculator calc = getCalculator(_parameter, null, prodOid, frmt.format(quantity),
                                    unitFrmt.format(unitPrice), frmt.format(discount), false, 0);
                    calcList.add(calc);

                    // update the base values for the position
                    final Update update = new Update(multi.getCurrentInstance());
                    update.add(CISales.PositionSumAbstract.CrossUnitPrice, calc.getCrossUnitPrice()
                                    .divide(rate, BigDecimal.ROUND_HALF_UP).setScale(uScale, BigDecimal.ROUND_HALF_UP));
                    update.add(CISales.PositionSumAbstract.NetUnitPrice, calc.getNetUnitPrice()
                                    .divide(rate, BigDecimal.ROUND_HALF_UP).setScale(uScale, BigDecimal.ROUND_HALF_UP));
                    update.add(CISales.PositionSumAbstract.CrossPrice, calc.getCrossPrice()
                                    .divide(rate, BigDecimal.ROUND_HALF_UP).setScale(scale, BigDecimal.ROUND_HALF_UP));
                    update.add(CISales.PositionSumAbstract.NetPrice, calc.getNetPrice()
                                    .divide(rate, BigDecimal.ROUND_HALF_UP).setScale(scale, BigDecimal.ROUND_HALF_UP));
                    update.add(CISales.PositionSumAbstract.Tax, calc.getTaxCatId());
                    update.add(CISales.PositionSumAbstract.Discount, calc.getDiscount());
                    update.add(CISales.PositionSumAbstract.DiscountNetUnitPrice, calc.getDiscountNetUnitPrice()
                                    .divide(rate, BigDecimal.ROUND_HALF_UP).setScale(uScale, BigDecimal.ROUND_HALF_UP));
                    update.add(CISales.PositionSumAbstract.CurrencyId, baseCurrInst.getId());
                    update.add(CISales.PositionSumAbstract.Rate, rateInfo.getRateObject());
                    update.execute();
                }
                // update the base values for the document
                final Update update = new Update(docInst);
                final BigDecimal netTotal = getNetTotal(_parameter, calcList).divide(rate, BigDecimal.ROUND_HALF_UP)
                                .setScale(scale, BigDecimal.ROUND_HALF_UP);
                update.add(CISales.DocumentSumAbstract.NetTotal, netTotal);

                update.add(CISales.DocumentSumAbstract.Taxes, getTaxes(_parameter, calcList, rate, baseCurrInst));

                final BigDecimal crossTotal = getCrossTotal(_parameter, calcList).divide(rate, BigDecimal.ROUND_HALF_UP)
                            .setScale(scale, BigDecimal.ROUND_HALF_UP);
                update.add(CISales.DocumentSumAbstract.CrossTotal, crossTotal);
                update.add(CISales.DocumentSumAbstract.Rate, rateInfo.getRateObject());
                update.execute();
            }
        }
        return new Return();
    }

    /**
     * Used by an FieldUpdate event used in the form for Recalculating
     * DocumentSum with a rate.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return map list for update event
     * @throws EFapsException on error
     */
    public Return update4DateOnRecalculate(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final Instance docInst = _parameter.getInstance();
        if (docInst.getType().isKindOf(CISales.DocumentSumAbstract.getType())) {
            final PrintQuery print = new PrintQuery(docInst);
            print.addAttribute(CISales.DocumentSumAbstract.RateCurrencyId,
                            CISales.DocumentSumAbstract.CurrencyId);
            print.execute();

            final Instance targetCurrInst = Instance.get(CIERP.Currency.getType(),
                            print.<Long> getAttribute(CISales.DocumentSumAbstract.RateCurrencyId));
            final PriceUtil priceUtil = new PriceUtil();
            final BigDecimal[] rates = priceUtil.getExchangeRate(priceUtil.getDateFromParameter(_parameter),
                            targetCurrInst);
            final BigDecimal rate = rates[1];

            final DecimalFormat formater = (DecimalFormat) NumberFormat.getInstance(
                            Context.getThreadContext().getLocale());
            formater.applyPattern("#,##0.############");
            formater.setRoundingMode(RoundingMode.HALF_UP);
            final String rateStr = formater.format(rate);
            final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
            final Map<String, String> map = new HashMap<String, String>();
            map.put("rate", rateStr);
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        }
        return retVal;
    }


    /**
     * @param _docInst Instance of the document
     * @param _rateValue old Rate
     * @param _newRate new Rate
     * @return new Value
     * @throws EFapsException on error
     */
    protected BigDecimal getNewValue(final Instance _docInst,
                                     final BigDecimal _rateValue,
                                     final BigDecimal _newRate)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        if (_rateValue.compareTo(BigDecimal.ZERO) != 0) {
            ret = _rateValue.divide(_newRate, BigDecimal.ROUND_HALF_UP)
                            .setScale(isDecimal4Doc(_docInst), BigDecimal.ROUND_HALF_UP);
        }
        return ret;
    }

    /**
     * Get decimal for document instance of the system configuration.
     *
     * @param _docInst instance of doc.
     * @return integer.
     * @throws EFapsException on error.
     */
    protected int isDecimal4Doc(final Instance _docInst)
        throws EFapsException
    {
        int ret = 2;
        final SystemConfiguration config = SystemConfiguration.get(
                        UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f"));
        final Properties props = config.getAttributeValueAsProperties("ActivateLongDecimal");
        final String type = _docInst.getType().getName();

        if (props.containsKey(type) && Integer.valueOf(props.getProperty(type)) != ret) {
            ret = Integer.valueOf(props.getProperty(type));
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _calcList List of calculators
     * @param _currencyInst instancof the current currency
     * @return Taxes object
     * @throws EFapsException on error
     */
    public Taxes getRateTaxes(final Parameter _parameter,
                              final List<Calculator> _calcList,
                              final Instance _currencyInst)
        throws EFapsException
    {
        final Map<Tax, BigDecimal> values = new HashMap<Tax, BigDecimal>();
        for (final Calculator calc : _calcList) {
            if (!calc.isWithoutTax()) {
                final Map<Tax, BigDecimal> valMap = calc.getTaxesAmounts();
                for (final Entry<Tax, BigDecimal> entry : valMap.entrySet()) {
                    if (!values.containsKey(entry.getKey())) {
                        values.put(entry.getKey(), BigDecimal.ZERO);
                    }
                    values.put(entry.getKey(), values.get(entry.getKey()).add(entry.getValue()));
                }
            }
        }
        final Taxes ret = new Taxes();

        if (!_calcList.isEmpty()) {
            final Calculator calc = _calcList.iterator().next();
            UUID currencyUUID = null;
            if (_currencyInst != null) {
                final CurrencyInst curInst = CurrencyInst_Base.get(_currencyInst);
                currencyUUID = curInst.getUUID();
            }
            for (final Entry<Tax, BigDecimal> entry : values.entrySet()) {
                final TaxEntry taxentry = new TaxEntry();
                taxentry.setAmount(entry.getValue());
                taxentry.setUUID(entry.getKey().getUUID());
                taxentry.setCatUUID(entry.getKey().getTaxCat().getUuid());
                taxentry.setCurrencyUUID(currencyUUID);
                taxentry.setDate(calc.getDate());
                ret.getEntries().add(taxentry);
            }
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _calcList List of calculators
     * @param _rate rate amount
     * @param _baseCurrInst instancof the base currency
     * @return Taxes object
     * @throws EFapsException on error
     */
    public Taxes getTaxes(final Parameter _parameter,
                          final List<Calculator> _calcList,
                          final BigDecimal _rate,
                          final Instance _baseCurrInst)
        throws EFapsException
    {
        final Taxes ret = getRateTaxes(_parameter, _calcList, _baseCurrInst);
        for (final TaxEntry entry  : ret.getEntries()) {
            entry.setAmount(entry.getAmount().divide(_rate, BigDecimal.ROUND_HALF_UP));
        }
        return ret;
    }

    /**
     * Method to get formated String representation of the cross total for a
     * list of Calculators.
     *
     * @param _parameter Parameter as passed by the eFasp API
     * @param _calcList list of Calculator the net total is wanted for
     * @return formated String representation of the cross total
     * @throws EFapsException on error
     */
    protected String getCrossTotalFmtStr(final Parameter _parameter,
                                         final List<Calculator> _calcList)
        throws EFapsException
    {
        return NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter))
                        .format(getCrossTotal(_parameter, _calcList));
    }

    /**
     * Method to get String representation of the cross total for a list of Calculators.
     * @param _parameter Parameter as passed by the eFasp API
     * @param _calcList list of Calculator the net total is wanted for
     * @return String representation of the cross total
     * @throws EFapsException on error
     */
    protected String getCrossTotalStr(final Parameter _parameter,
                                      final List<Calculator> _calcList)
        throws EFapsException
    {
        return getCrossTotal(_parameter, _calcList).toString();
    }

    /**
     * Method to get the cross total for a list of Calculators.
     *
     * @param _parameter Parameter as passed by the eFasp API
     * @param _calcList list of Calculator the net total is wanted for
     * @return the cross total
     * @throws EFapsException on error
     */
    protected BigDecimal getCrossTotal(final Parameter _parameter,
                                       final List<Calculator> _calcList)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        for (final Calculator calc : _calcList) {
            ret = ret.add(calc.getCrossPrice());
        }
        return ret;
    }

    /**
     * Method to get formated String representation of the net total for a
     * list of Calculators.
     * @param _parameter Parameter as passed by the eFasp API
     * @param _calcList list of Calculator the net total is wanted for
     * @return formated String representation of the net total
     * @throws EFapsException on error
     */
    protected String getNetTotalFmtStr(final Parameter _parameter,
                                       final List<Calculator> _calcList)
        throws EFapsException
    {
        return NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter))
                        .format(getNetTotal(_parameter, _calcList));
    }

    /**
     * Method to get String representation of the net total for a list of Calculators.
     *
     * @param _parameter Parameter as passed by the eFasp API
     * @param _calcList list of Calculator the net total is wanted for
     * @return String representation of the net total
     * @throws EFapsException on error
     */
    protected String getNetTotalStr(final Parameter _parameter,
                                    final List<Calculator> _calcList)
        throws EFapsException
    {
        return getNetTotal(_parameter, _calcList).toString();
    }

    /**
     * Method to get the net total for a list of Calculators.
     *
     * @param _parameter Parameter as passed by the eFasp API
     * @param _calcList list of Calculator the net total is wanted for
     * @return the net total
     * @throws EFapsException on error
     */
    protected BigDecimal getNetTotal(final Parameter _parameter,
                                     final List<Calculator> _calcList)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        for (final Calculator calc : _calcList) {
            ret = ret.add(calc.getNetPrice());
        }
        return ret;
    }

    /**
     * Method to get the base cross total for a list of Calculators.
     *
     * @param _parameter Parameter as passed by the eFasp API
     * @param _calcList list of Calculator the net total is wanted for
     * @return the base cross total
     * @throws EFapsException on error
     */
    protected BigDecimal getBaseCrossTotal(final Parameter _parameter,
                                           final List<Calculator> _calcList)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        for (final Calculator calculator : _calcList) {
            ret = ret.add(calculator.getProductCrossPrice().getBasePrice());
        }
        return ret;
    }

    /**
     * Method to get formated string representation of the base cross total for a list of Calculators.
     *
     * @param _parameter Parameter as passed by the eFasp API
     * @param _calcList list of Calculator the base cross total is wanted for
     * @return the base cross total
     * @throws EFapsException on error
     */
    protected String getBaseCrossTotalFmtStr(final Parameter _parameter,
                                             final List<Calculator> _calcList)
        throws EFapsException
    {
        return NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter))
                        .format(getBaseCrossTotal(_parameter, _calcList));
    }

    /**
     * Method to get String representation of the base cross total for a list of Calculators.
     *
     * @param _parameter Parameter as passed by the eFasp API
     * @param _calcList list of Calculator the base cross total is wanted for
     * @return the base cross total
     * @throws EFapsException on error
     */
    protected String getBaseCrossTotalStr(final Parameter _parameter,
                                          final List<Calculator> _calcList)
        throws EFapsException
    {
        return getBaseCrossTotal(_parameter, _calcList).toString();
    }


    /**
     * Method to get formated String representation of the cross total for a
     * list of Calculators.
     *
     * @param _parameter Parameter as passed by the eFasp API
     * @param _calcList list of Calculator the net total is wanted for
     * @return formated String representation of the cross total
     * @throws EFapsException on error
     */
    protected String getPerceptionTotalFmtStr(final Parameter _parameter,
                                              final List<Calculator> _calcList)
        throws EFapsException
    {
        return NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter))
                        .format(getPerceptionTotal(_parameter, _calcList));
    }

    /**
     * Method to get String representation of the cross total for a list of Calculators.
     * @param _parameter Parameter as passed by the eFasp API
     * @param _calcList list of Calculator the net total is wanted for
     * @return String representation of the cross total
     * @throws EFapsException on error
     */
    protected String getPerceptionTotalStr(final Parameter _parameter,
                                      final List<Calculator> _calcList)
        throws EFapsException
    {
        return getPerceptionTotal(_parameter, _calcList).toString();
    }

    /**
     * Method to get the perception total for a list of Calculators.
     *
     * @param _parameter Parameter as passed by the eFasp API
     * @param _calcList list of Calculator the net total is wanted for
     * @return the cross total
     * @throws EFapsException on error
     */
    protected BigDecimal getPerceptionTotal(final Parameter _parameter,
                                           final List<Calculator> _calcList)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        for (final Calculator calc : _calcList) {
            ret = ret.add(calc.getPerception());
        }
        return ret;
    }

    /**
     * Method to override if a default value is required for a document.
     *
     * @param _parameter as passed from eFaps API
     * @return DropDownfield
     * @throws EFapsException on error.
     */
    public Return dropDown4DocumentType(final Parameter _parameter)
        throws EFapsException
    {
        return new org.efaps.esjp.common.uiform.Field().dropDownFieldValue(_parameter);
    }

    /**
     * @param _parameter as passed from eFaps API.
     * @return Return for a search
     * @throws EFapsException on error.
     */
    public Return search4DocumentType(final Parameter _parameter)
        throws EFapsException
    {
        return new Search()
        {

            @Override
            protected void add2QueryBuilder(final Parameter _parameter,
                                            final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
                final String typeStr = (String) properties.get("SelectType");
                final Type type = Type.get(typeStr);
                final QueryBuilder query = new QueryBuilder(type);
                final AttributeQuery attQueryType = query.getAttributeQuery(CIERP.DocumentTypeAbstract.ID);

                final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Document2DocumentType);
                attrQueryBldr.addWhereAttrInQuery(CISales.Document2DocumentType.DocumentTypeLink, attQueryType);
                final AttributeQuery attrQuery = attrQueryBldr
                                .getAttributeQuery(CISales.Document2DocumentType.DocumentLink);

                _queryBldr.addWhereAttrNotInQuery(CISales.DocumentAbstract.ID, attrQuery);
            }
        } .execute(_parameter);
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return new empty Return
     * @throws EFapsException on error
     */
    public Return changeDocumentType(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instDocType = Instance.get(_parameter.getParameterValue("documentType"));
        if (instDocType.isValid() && _parameter.getInstance().isValid()) {
            final QueryBuilder queryBldr = new QueryBuilder(getType4DocCreate(_parameter));
            queryBldr.addWhereAttrEqValue(CIERP.Document2DocumentTypeAbstract.DocumentLinkAbstract, _parameter.getInstance());

            final InstanceQuery query = queryBldr.getQuery();
            query.execute();

            Update update;
            if (query.next()) {
                update = new Update(query.getCurrentValue());
            } else {
                update = new Insert(getType4DocCreate(_parameter));
                update.add(CIERP.Document2DocumentTypeAbstract.DocumentLinkAbstract, _parameter.getInstance());
            }
            update.add(CIERP.Document2DocumentTypeAbstract.DocumentTypeLinkAbstract, instDocType);
            update.execute();
        }
        return new Return();
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return Return containing TRUE and SNIPLETT if warning
     * @throws EFapsException on error
     */
    public Return validateName(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final StringBuilder html = new StringBuilder();
        final String fieldName = getProperty(_parameter, "FieldName");
        String name = null;
        if (fieldName == null) {
            name  = _parameter.getParameterValue("name4create");
            if (name == null) {
                name = _parameter.getParameterValue("name");
            }
        } else {
            name = _parameter.getParameterValue(fieldName);
        }

        if (name != null) {
            final String[] numbers = name.split("-");
            if (numbers.length == 2 && numbers[0].length() >= numbers[1].length()) {
                html.insert(0, DBProperties.getProperty("org.efaps.esjp.sales.document.InvalidateName"));
                ret.put(ReturnValues.SNIPLETT, html.toString());
            } else if (numbers.length != 2) {
                html.insert(0, DBProperties.getProperty("org.efaps.esjp.sales.document.InvalidateName"));
                ret.put(ReturnValues.SNIPLETT, html.toString());
            }
        }
        ret.put(ReturnValues.TRUE, true);
        return ret;
    }
}
