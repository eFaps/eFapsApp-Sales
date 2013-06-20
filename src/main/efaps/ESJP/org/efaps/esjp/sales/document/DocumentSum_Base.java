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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
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
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uisearch.Search;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.sales.Calculator;
import org.efaps.esjp.sales.PriceUtil;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.SalesSettings;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;

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
        final Instance rateCurrInst = _parameter.getParameterValue("rateCurrencyId") == null
                        ? baseCurrInst
                        : Instance.get(CIERP.Currency.getType(), _parameter.getParameterValue("rateCurrencyId"));

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

        insert.add(CISales.DocumentSumAbstract.RateCrossTotal,
                        getCrossTotal(_parameter, calcList).setScale(isLongDecimal(_parameter),
                                        BigDecimal.ROUND_HALF_UP));
        insert.add(CISales.DocumentSumAbstract.RateNetTotal,
                       getNetTotal(_parameter, calcList).setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
        insert.add(CISales.DocumentSumAbstract.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.DocumentSumAbstract.CrossTotal,
                        getCrossTotal(_parameter, calcList).divide(rate, BigDecimal.ROUND_HALF_UP).setScale(
                                        isLongDecimal(_parameter),
                                        BigDecimal.ROUND_HALF_UP));
        insert.add(CISales.DocumentSumAbstract.NetTotal,
                        getNetTotal(_parameter, calcList).divide(rate, BigDecimal.ROUND_HALF_UP).setScale(
                                        isLongDecimal(_parameter),
                                        BigDecimal.ROUND_HALF_UP));
        insert.add(CISales.DocumentSumAbstract.DiscountTotal, BigDecimal.ZERO);

        insert.add(CISales.DocumentSumAbstract.CurrencyId, baseCurrInst);
        insert.add(CISales.DocumentSumAbstract.Rate, rateObj);
        insert.add(CISales.DocumentSumAbstract.RateCurrencyId, rateCurrInst);

        createdDoc.getValues().put(CISales.DocumentSumAbstract.CurrencyId.name, baseCurrInst);
        createdDoc.getValues().put(CISales.DocumentSumAbstract.RateCurrencyId.name, rateCurrInst);
        createdDoc.getValues().put(CISales.DocumentSumAbstract.Rate.name, rateObj);

        addStatus2DocCreate(_parameter, insert, createdDoc);
        add2DocCreate(_parameter, insert, createdDoc);
        insert.execute();

        createdDoc.setInstance(insert.getInstance());
        return createdDoc;
    }

    /**
     * Method to connect the document with the selected document type.
     *
     * @param _parameter
     * @param _instance
     * @throws EFapsException
     */
    protected void connect2DocumentType(final Parameter _parameter,
                                        final Instance _instance)
        throws EFapsException
    {
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
        //positions
        _map.put("quantity", _cal.getQuantityStr());
        _map.put("netUnitPrice", _cal.getNetUnitPriceFmtStr(getDigitsformater4UnitPrice(_cal)));
        _map.put("netPrice", _cal.getNetPriceFmtStr(getTwoDigitsformater()));
        _map.put("discountNetUnitPrice", _cal.getDiscountNetUnitPriceFmtStr(getDigitsformater4UnitPrice(_cal)));
        _map.put("discount", _cal.getDiscountFmtStr(getDigitsFormater4Disount(_cal)));

        //totals
        _map.put("netTotal", getNetTotalFmtStr(_parameter, _calcList));
        _map.put("crossTotal", getCrossTotalFmtStr(_parameter, _calcList));

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
            map.put("quantity", cal.getQuantityStr());
            map.put("netUnitPrice", cal.getNetUnitPriceFmtStr(getDigitsformater4UnitPrice(cal)));
            map.put("netPrice", cal.getNetPriceFmtStr(getTwoDigitsformater()));
            map.put("netTotal", getNetTotalFmtStr(_parameter, calcList));
            map.put("crossTotal", getCrossTotalFmtStr(_parameter, calcList));
            map.put("discountNetUnitPrice", cal.getDiscountNetUnitPriceFmtStr(getDigitsformater4UnitPrice(cal)));
            map.put("discount", cal.getDiscountFmtStr(getDigitsFormater4Disount(cal)));
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
        final String oid = _parameter.getParameterValues("product")[selected];
        String name;
        // validate that a product was selected
        if (oid.length() > 0) {
            final PrintQuery print = new PrintQuery(oid);
            print.addAttribute("Name");
            print.execute();
            name = print.getAttribute("Name");
        } else {
            name = "";
        }
        if (name.length() > 0) {
            final List<Calculator> calcList = analyseTable(_parameter, selected);
            if (calcList.size() > 0) {
                final Calculator cal = calcList.get(selected);
                add2Map4UpdateField(_parameter, map, calcList, cal);
                map.put("productAutoComplete", name);
                list.add(map);
                retVal.put(ReturnValues.VALUES, list);
            }
        } else {
            map.put("productAutoComplete", name);
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
            final StringBuilder js = new StringBuilder();
            js.append("document.getElementsByName('productAutoComplete')[").append(selected).append("].focus()");
            map.put(EFapsKey.FIELDUPDATE_JAVASCRIPT.getKey(), js.toString());
        }
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

        final Instance rateCurrInst = Instance.get(CIERP.Currency.getType(),
                        _parameter.getParameterValue("rateCurrencyId"));
        final Object[] rateObj = getRateObject(_parameter);
        final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                        BigDecimal.ROUND_HALF_UP);

        @SuppressWarnings("unchecked")
        final List<Calculator> calcList = (List<Calculator>) _createdDoc.getValue(DocumentSum_Base.CALCULATORS_VALUE);

        Integer i = 0;
        for (final Calculator calc : calcList) {
            if (!calc.isEmpty()) {
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

                posIns.add(CISales.PositionSumAbstract.Quantity, calc.getQuantity());

                posIns.add(CISales.PositionSumAbstract.CrossUnitPrice,
                                calc.getCrossUnitPrice()
                                                .divide(rate, BigDecimal.ROUND_HALF_UP)
                                                .setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.PositionSumAbstract.NetUnitPrice,
                                calc.getNetUnitPrice()
                                                .divide(rate, BigDecimal.ROUND_HALF_UP)
                                                .setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.PositionSumAbstract.CrossPrice,
                                calc.getCrossPrice()
                                                .divide(rate, BigDecimal.ROUND_HALF_UP)
                                                .setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.PositionSumAbstract.NetPrice,
                                calc.getNetPrice()
                                                .divide(rate, BigDecimal.ROUND_HALF_UP)
                                                .setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.PositionSumAbstract.Tax, (calc.getTaxId()).toString());
                posIns.add(CISales.PositionSumAbstract.Discount, calc.getDiscountStr());
                posIns.add(CISales.PositionSumAbstract.DiscountNetUnitPrice,
                                calc.getDiscountNetUnitPrice()
                                                .divide(rate, BigDecimal.ROUND_HALF_UP)
                                                .setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.PositionSumAbstract.CurrencyId, baseCurrInst.getId());
                posIns.add(CISales.PositionSumAbstract.Rate, rateObj);
                posIns.add(CISales.PositionSumAbstract.RateCurrencyId, rateCurrInst.getId());
                posIns.add(CISales.PositionSumAbstract.RateNetUnitPrice,
                                calc.getNetUnitPrice().setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.PositionSumAbstract.RateCrossUnitPrice,
                                calc.getCrossUnitPrice().setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.PositionSumAbstract.RateDiscountNetUnitPrice,
                                calc.getDiscountNetUnitPrice().setScale(isLongDecimal(_parameter),
                                                BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.PositionSumAbstract.RateNetPrice,
                                calc.getNetPrice().setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
                posIns.add(CISales.PositionSumAbstract.RateCrossPrice,
                                calc.getCrossPrice().setScale(isLongDecimal(_parameter), BigDecimal.ROUND_HALF_UP));
                add2PositionInsert(_parameter, calc, posIns);
                posIns.execute();
                _createdDoc.addPosition(posIns.getInstance());
            }
            i++;
        }
    }


    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _calc Calculator
     * @param _posIns insert
     * @throws EFapsException on error
     */
    protected void add2PositionInsert(final Parameter _parameter,
                                      final Calculator _calc,
                                      final Insert _posIns)
        throws EFapsException
    {
        // to be implemented by subclasses
    }

    /**
     * Used by the update event used in the select doc form for CostSheet.
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
            print.addAttribute(CISales.DocumentSumAbstract.RateCrossTotal,
                            CISales.DocumentSumAbstract.RateDiscountTotal,
                            CISales.DocumentSumAbstract.RateNetTotal,
                            CISales.DocumentSumAbstract.RateCurrencyId,
                            CISales.DocumentSumAbstract.CurrencyId,
                            CISales.DocumentSumAbstract.Date);
            print.execute();

            final BigDecimal rateCross = print.<BigDecimal> getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
            final BigDecimal rateDiscount = print.<BigDecimal> getAttribute(
                            CISales.DocumentSumAbstract.RateDiscountTotal);
            final BigDecimal rateNet = print.<BigDecimal> getAttribute(CISales.DocumentSumAbstract.RateNetTotal);
            final Instance targetCurrInst = Instance.get(CIERP.Currency.getType(),
                            print.<Long> getAttribute(CISales.DocumentSumAbstract.RateCurrencyId));
            final Instance currentInst = Instance.get(CIERP.Currency.getType(),
                            print.<Long> getAttribute(CISales.DocumentSumAbstract.CurrencyId));

            final PriceUtil priceUtil = new PriceUtil();
            final BigDecimal[] rates = priceUtil.getRates(_parameter, targetCurrInst, currentInst);
            final BigDecimal rate = rates[2];

            final BigDecimal[] rates2 = priceUtil.getExchangeRate(priceUtil.getDateFromParameter(_parameter),
                            targetCurrInst);
            final CurrencyInst currInst = new CurrencyInst(targetCurrInst);

            final Object[] rateObj = new Object[] { currInst.isInvert() ? BigDecimal.ONE : rates2[0],
                            currInst.isInvert() ? rates2[1] : BigDecimal.ONE };

            final Update update = new Update(docInst);
            update.add(CISales.DocumentSumAbstract.CrossTotal, rateCross.compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO.setScale(isDecimal4Doc(docInst), BigDecimal.ROUND_HALF_UP)
                                            .setScale(2, BigDecimal.ROUND_HALF_UP)
                            : rateCross.divide(rate, BigDecimal.ROUND_HALF_UP)
                                .setScale(isDecimal4Doc(docInst), BigDecimal.ROUND_HALF_UP)
                                .setScale(2, BigDecimal.ROUND_HALF_UP));
            update.add(CISales.DocumentSumAbstract.NetTotal, rateNet.compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO.setScale(isDecimal4Doc(docInst), BigDecimal.ROUND_HALF_UP)
                                            .setScale(2, BigDecimal.ROUND_HALF_UP)
                            : rateNet.divide(rate, BigDecimal.ROUND_HALF_UP)
                                .setScale(isDecimal4Doc(docInst), BigDecimal.ROUND_HALF_UP)
                                .setScale(2, BigDecimal.ROUND_HALF_UP));
            update.add(CISales.DocumentSumAbstract.DiscountTotal, rateDiscount.compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO.setScale(isDecimal4Doc(docInst), BigDecimal.ROUND_HALF_UP)
                                            .setScale(2, BigDecimal.ROUND_HALF_UP)
                            : rateDiscount.divide(rate, BigDecimal.ROUND_HALF_UP)
                                .setScale(isDecimal4Doc(docInst), BigDecimal.ROUND_HALF_UP)
                                .setScale(2, BigDecimal.ROUND_HALF_UP));
            update.add(CISales.DocumentSumAbstract.Rate, rateObj);
            update.execute();

            final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionSumAbstract);
            queryBldr.addWhereAttrEqValue(CISales.PositionSumAbstract.DocumentAbstractLink, docInst.getId());
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CISales.PositionSumAbstract.RateCrossPrice,
                            CISales.PositionSumAbstract.RateCrossUnitPrice,
                            CISales.PositionSumAbstract.RateDiscountNetUnitPrice,
                            CISales.PositionSumAbstract.RateNetPrice,
                            CISales.PositionSumAbstract.RateNetUnitPrice);
            multi.execute();
            while (multi.next()) {
                final BigDecimal rateCrossPrice = multi.<BigDecimal> getAttribute(
                                CISales.PositionSumAbstract.RateCrossPrice);
                final BigDecimal rateDscNetUnitPrice = multi.<BigDecimal> getAttribute(
                                CISales.PositionSumAbstract.RateDiscountNetUnitPrice);
                final BigDecimal rateCrossUnitPrice = multi.<BigDecimal> getAttribute(
                                CISales.PositionSumAbstract.RateCrossUnitPrice);
                final BigDecimal rateNetPrice = multi.<BigDecimal> getAttribute(
                                CISales.PositionSumAbstract.RateNetPrice);
                final BigDecimal rateNetUnitPrice = multi.<BigDecimal> getAttribute(
                                CISales.PositionSumAbstract.RateNetUnitPrice);
                final BigDecimal newCrossPrice = getNewValue(docInst, rateCrossPrice, rates[2]);
                final BigDecimal newDiscountNetUnitPrice = getNewValue(docInst, rateDscNetUnitPrice, rates[2]);
                final BigDecimal newCrossUnitPrice = getNewValue(docInst, rateCrossUnitPrice, rates[2]);
                final BigDecimal newCrossNetPrice = getNewValue(docInst, rateNetPrice, rates[2]);
                final BigDecimal newCrossNetUnitPrice = getNewValue(docInst, rateNetUnitPrice, rates[2]);
                final Update updatePos = new Update(multi.getCurrentInstance());
                updatePos.add(CISales.PositionSumAbstract.CrossPrice, newCrossPrice);
                updatePos.add(CISales.PositionSumAbstract.DiscountNetUnitPrice, newDiscountNetUnitPrice);
                updatePos.add(CISales.PositionSumAbstract.CrossUnitPrice, newCrossUnitPrice);
                updatePos.add(CISales.PositionSumAbstract.NetPrice, newCrossNetPrice);
                updatePos.add(CISales.PositionSumAbstract.NetUnitPrice, newCrossNetUnitPrice);
                updatePos.add(CISales.PositionSumAbstract.Rate, rateObj);
                updatePos.execute();
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
        return getTwoDigitsformater().format(getCrossTotal(_parameter, _calcList));
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
        return getTwoDigitsformater().format(getNetTotal(_parameter, _calcList));
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
     * @param _calc calculator the format is wanted for
     * @return Decimal Format
     * @throws EFapsException on error
     */
    protected DecimalFormat getDigitsFormater4Disount(final Calculator _calc)
        throws EFapsException
    {
        final DecimalFormat formater = (DecimalFormat) NumberFormat.getInstance(Context.getThreadContext().getLocale());
        if (_calc.isLongDecimal() != 2) {
            formater.setMaximumFractionDigits(_calc.isLongDecimal());
            formater.setMinimumFractionDigits(_calc.isLongDecimal());
        } else {
            formater.setMaximumFractionDigits(2);
            formater.setMinimumFractionDigits(2);
        }
        formater.setRoundingMode(RoundingMode.HALF_UP);
        formater.setParseBigDecimal(true);
        return formater;
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
        return getTwoDigitsformater().format(getPerceptionTotal(_parameter, _calcList));
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
     * Method to override if a default value is required for a document,
     *
     * @param _parameter as passed from eFaps API
     * @return
     * @throws EFapsException on error.
     */
    public Return dropDown4DocumentType(final Parameter _parameter)
        throws EFapsException
    {
        return new org.efaps.esjp.common.uiform.Field().dropDownFieldValue(_parameter);
    }

    /**
     * @param _parameter as passed from eFaps API.
     * @return
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
                final Instance instance = _parameter.getInstance();
                final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Document2DocumentType);
                attrQueryBldr.addWhereAttrEqValue(CISales.Document2DocumentType.DocumentLink, instance);
                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.Document2DocumentType.DocumentLink);

                _queryBldr.addWhereAttrNotInQuery(CISales.DocumentAbstract.ID, attrQuery);
            }
        }.execute(_parameter);
    }
}
