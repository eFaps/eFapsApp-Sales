/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
 */
package org.efaps.esjp.sales.document;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.lang3.EnumUtils;
import org.efaps.abacus.api.ITax;
import org.efaps.abacus.api.TaxType;
import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.IUIValue;
import org.efaps.admin.datamodel.ui.RateUI;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.admin.ui.field.Field;
import org.efaps.ci.CIAttribute;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.datetime.JodaTimeUtils;
import org.efaps.esjp.common.uisearch.Search;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.erp.RateInfo;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.esjp.erp.util.ERP.DocTypeActivation;
import org.efaps.esjp.erp.util.ERP.DocTypeConfiguration;
import org.efaps.esjp.sales.Calculator;
import org.efaps.esjp.sales.CalculatorService;
import org.efaps.esjp.sales.Channel;
import org.efaps.esjp.sales.Payment;
import org.efaps.esjp.sales.Payment_Base;
import org.efaps.esjp.sales.Payment_Base.OpenAmount;
import org.efaps.esjp.sales.PriceUtil;
import org.efaps.esjp.sales.payment.DocPaymentInfo_Base;
import org.efaps.esjp.sales.tax.Tax;
import org.efaps.esjp.sales.tax.TaxAmount;
import org.efaps.esjp.sales.tax.TaxCat_Base;
import org.efaps.esjp.sales.tax.TaxesAttribute;
import org.efaps.esjp.sales.tax.xml.TaxEntry;
import org.efaps.esjp.sales.tax.xml.Taxes;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.promotionengine.pojo.Document;
import org.efaps.promotionengine.pojo.Position;
import org.efaps.util.DateTimeUtil;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class is the abstract instance for all documents of type DocumentSum.
 *
 * @author The eFaps Team
 */
@EFapsUUID("e177ab08-67f0-4ce2-8eff-d3f167352bee")
@EFapsApplication("eFapsApp-Sales")
public abstract class AbstractDocumentSum_Base
    extends AbstractDocument
{

    /**
     * Key to sore access check during a request.
     */
    public static final String ACCESSREQKEY = AbstractDocumentSum.class.getName() + ".accessCheck4Rate";
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDocument.class);

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
        if (field.isEditableDisplay(TargetMode.CREATE) && !Calculator.isIncludeMinRetail(_parameter, this) || field
                        .isReadonlyDisplay(TargetMode.CREATE) && Calculator.isIncludeMinRetail(_parameter, this)) {
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    /**
     * AccessCheck that grants access if currency and ratecurrency are
     * different.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return return granting access or not
     * @throws EFapsException on error
     */
    public Return accessCheck4Rate(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        Object obj = Context.getThreadContext().getRequestAttribute(AbstractDocumentSum_Base.ACCESSREQKEY);
        if (obj == null) {
            final PrintQuery print = new PrintQuery(_parameter.getInstance());
            print.addAttribute(CISales.DocumentSumAbstract.CurrencyId, CISales.DocumentSumAbstract.RateCurrencyId);
            print.executeWithoutAccessCheck();
            final Long currencyId = print.<Long>getAttribute(CISales.DocumentSumAbstract.CurrencyId);
            final Long rateCurrencyId = print.<Long>getAttribute(CISales.DocumentSumAbstract.RateCurrencyId);
            obj = currencyId != null && !currencyId.equals(rateCurrencyId);
            Context.getThreadContext().setRequestAttribute(AbstractDocumentSum_Base.ACCESSREQKEY, obj);
        }
        if ((Boolean) obj) {
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
        _editDoc.addValue(AbstractDocument_Base.CALCULATORS_VALUE, calcList);
        final Instance baseCurrInst = Currency.getBaseCurrency();
        final Instance rateCurrInst = getRateCurrencyInstance(_parameter, _editDoc);

        final Object[] rateObj = getRateObject(_parameter);
        final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12, RoundingMode.HALF_UP);

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

        final String contact = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentSumAbstract.Contact.name));
        final Instance contactIns = Instance.get(contact);
        if (contactIns != null && contactIns.isValid()) {
            update.add(CISales.DocumentSumAbstract.Contact, contactIns.getId());
            _editDoc.getValues().put(CISales.DocumentSumAbstract.Contact.name, contactIns);
        }

        final String remark = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentSumAbstract.Remark.name));
        if (remark != null) {
            update.add(CISales.DocumentSumAbstract.Remark, remark);
            _editDoc.getValues().put(CISales.DocumentSumAbstract.Remark.name, remark);
        }

        final String revision = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentSumAbstract.Revision.name));
        if (revision != null) {
            update.add(CISales.DocumentSumAbstract.Revision, revision);
            _editDoc.getValues().put(CISales.DocumentSumAbstract.Revision.name, revision);
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
            final DecimalFormat frmt = NumberFormatter.get().getFrmt4Total(getType4SysConf(_parameter));
            final int scale = frmt.getMaximumFractionDigits();
            final BigDecimal rateCrossTotal = getCrossTotal(_parameter, calcList).setScale(scale, RoundingMode.HALF_UP);
            update.add(CISales.DocumentSumAbstract.RateCrossTotal, rateCrossTotal);
            _editDoc.getValues().put(CISales.DocumentSumAbstract.RateCrossTotal.name, rateCrossTotal);

            final BigDecimal rateNetTotal = getNetTotal(_parameter, calcList).setScale(scale, RoundingMode.HALF_UP);
            update.add(CISales.DocumentSumAbstract.RateNetTotal, rateNetTotal);
            _editDoc.getValues().put(CISales.DocumentSumAbstract.RateNetTotal.name, rateNetTotal);

            update.add(CISales.DocumentSumAbstract.RateDiscountTotal, BigDecimal.ZERO);
            update.add(CISales.DocumentSumAbstract.RateTaxes, getRateTaxes(_parameter, calcList, rateCurrInst));
            update.add(CISales.DocumentSumAbstract.Taxes, getTaxes(_parameter, calcList, rate, baseCurrInst));

            final BigDecimal crossTotal = getCrossTotal(_parameter, calcList).divide(rate, RoundingMode.HALF_UP)
                            .setScale(scale, RoundingMode.HALF_UP);
            update.add(CISales.DocumentSumAbstract.CrossTotal, crossTotal);
            _editDoc.getValues().put(CISales.DocumentSumAbstract.CrossTotal.name, crossTotal);

            final BigDecimal netTotal = getNetTotal(_parameter, calcList).divide(rate, RoundingMode.HALF_UP)
                            .setScale(scale, RoundingMode.HALF_UP);
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
        throws EFapsException
    {
        return new Currency().getCurrencyFromUI(_parameter, "rateCurrencyId");
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
        createdDoc.addValue(AbstractDocument_Base.CALCULATORS_VALUE, calcList);
        final Instance baseCurrInst = Currency.getBaseCurrency();
        final Instance rateCurrInst = getRateCurrencyInstance(_parameter, createdDoc);

        final Object[] rateObj = getRateObject(_parameter);
        final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                        RoundingMode.HALF_UP);

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

        final String remark = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentSumAbstract.Remark.name));
        if (remark != null) {
            insert.add(CISales.DocumentSumAbstract.Remark, remark);
            createdDoc.getValues().put(CISales.DocumentSumAbstract.Remark.name, remark);
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
            final DecimalFormat frmt = NumberFormatter.get().getFrmt4Total(getType4SysConf(_parameter));
            final int scale = frmt.getMaximumFractionDigits();
            final BigDecimal rateCrossTotal = getCrossTotal(_parameter, calcList)
                            .setScale(scale, RoundingMode.HALF_UP);
            insert.add(CISales.DocumentSumAbstract.RateCrossTotal, rateCrossTotal);
            createdDoc.getValues().put(CISales.DocumentSumAbstract.RateCrossTotal.name, rateCrossTotal);

            final BigDecimal rateNetTotal = getNetTotal(_parameter, calcList).setScale(scale, RoundingMode.HALF_UP);
            insert.add(CISales.DocumentSumAbstract.RateNetTotal, rateNetTotal);
            createdDoc.getValues().put(CISales.DocumentSumAbstract.RateNetTotal.name, rateNetTotal);

            insert.add(CISales.DocumentSumAbstract.RateDiscountTotal, BigDecimal.ZERO);
            insert.add(CISales.DocumentSumAbstract.RateTaxes, getRateTaxes(_parameter, calcList, rateCurrInst));
            insert.add(CISales.DocumentSumAbstract.Taxes, getTaxes(_parameter, calcList, rate, baseCurrInst));

            final BigDecimal crossTotal = getCrossTotal(_parameter, calcList).divide(rate, RoundingMode.HALF_UP)
                            .setScale(scale, RoundingMode.HALF_UP);
            insert.add(CISales.DocumentSumAbstract.CrossTotal, crossTotal);
            createdDoc.getValues().put(CISales.DocumentSumAbstract.CrossTotal.name, crossTotal);

            final BigDecimal netTotal = getNetTotal(_parameter, calcList).divide(rate, RoundingMode.HALF_UP)
                            .setScale(scale, RoundingMode.HALF_UP);
            insert.add(CISales.DocumentSumAbstract.NetTotal, netTotal);
            createdDoc.getValues().put(CISales.DocumentSumAbstract.NetTotal.name, netTotal);

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
        return createdDoc;
    }

    /**
     * Method is executed as an update event of the field containing the
     * quantity of products to calculate the new totals.
     *
     * @param _parameter Parameter as passed by the eFasp API
     * @return Return containing the list
     * @throws EFapsException on error
     */
    public Return updateFields4Quantity(final Parameter parameter)
        throws EFapsException
    {
        if (isRest()) {
            return new Return().put(ReturnValues.VALUES, calculate(parameter));
        }

        final Return retVal = new Return();
        final List<Map<String, Object>> list = new ArrayList<>();
        final Map<String, Object> map = new HashMap<>();

        final int selected = getSelectedRow(parameter);

        final List<Calculator> calcList = analyseTable(parameter, null);

        final Calculator cal = calcList.get(selected);
        if (calcList.size() > 0) {
            add2Map4UpdateField(parameter, map, calcList, cal, true);
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        }
        return retVal;
    }

    protected BigDecimal evalQuantity(final Parameter parameter,
                                      final int index)
    {
        return evalBigDecimal(parameter, "quantity", index);
    }

    protected BigDecimal evalNetUnitPrice(final Parameter parameter,
                                          final int index,
                                          final CalculatorService service,
                                          final Instance prodInst)
        throws EFapsException
    {
        BigDecimal ret = evalBigDecimal(parameter, "rateNetUnitPrice", index);
        if (ret == null || ret.compareTo(BigDecimal.ZERO) == 0) {
            final var dateStr = getValue(parameter, "date");
            final var localDate = DateTimeUtil.toContextDate(dateStr);
            final var dateTime = JodaTimeUtils.toDateTime(DateTimeUtil.asContextDateTime(localDate));
            final var prodPrice = service.evalPriceFromDB(dateTime, prodInst);
            ret = prodPrice.getCurrentPrice();
        }
        return ret;
    }

    public List<Map<String, Object>> calculate(final Parameter parameter)
        throws EFapsException
    {
        final List<Map<String, Object>> list = new ArrayList<>();

        final var calcDoc = new Document();

        final var service = new CalculatorService(getCIType().getType().getName());

        int idx = 0;
        var prodInst = Instance.get(getValue(parameter, "product", idx));
        while (InstanceUtils.isKindOf(prodInst, CIProducts.ProductAbstract)) {

            final var eval = EQL.builder().print(prodInst)
                            .attribute(CIProducts.ProductAbstract.TaxCategory)
                            .evaluate();
            eval.next();
            final var taxCatId = eval.<Long>get(CIProducts.ProductAbstract.TaxCategory);

            final List<ITax> taxes = TaxCat_Base.get(taxCatId).getTaxes().stream()
                            .map(tax -> {
                                try {

                                    return (ITax) new org.efaps.abacus.pojo.Tax()
                                                    .setKey(CalculatorService.getTaxKey(tax))
                                                    .setPercentage(tax.getFactor().multiply(new BigDecimal("100")))
                                                    .setAmount(tax.getAmount())
                                                    .setType(EnumUtils.getEnum(TaxType.class,
                                                                    tax.getTaxType().name()));
                                } catch (final EFapsException e) {
                                    LOG.error("Catched", e);
                                }
                                return null;
                            })
                            .toList();

            final var position = new Position()
                            .setNetUnitPrice(evalNetUnitPrice(parameter, idx, service, prodInst))
                            .setTaxes(taxes)
                            .setIndex(idx + 1)
                            .setQuantity(evalQuantity(parameter, idx))
                            .setProductOid(prodInst.getOid());
            calcDoc.addPosition(position);

            idx++;
            prodInst = Instance.get(getValue(parameter, "product", idx));
        }

        if (calcDoc.getPositions() != null) {
            service.calculate(calcDoc);
            for (final var position : calcDoc.getPositions()) {
                final Map<String, Object> map = new HashMap<>();
                map.put("quantity", position.getQuantity());
                map.put("rateNetUnitPrice", position.getNetUnitPrice());
                map.put("rateNetUnitPrice4Read", position.getNetUnitPrice());
                map.put("rateNetPrice", position.getNetPrice());
                map.put("rateDiscountNetUnitPrice", position.getNetUnitPrice());
                map.put("discount", 0);
                map.put("rateCrossPrice", position.getCrossPrice());
                map.put("rateNetTotal", calcDoc.getNetTotal());
                map.put("rateCrossTotal", calcDoc.getCrossTotal());
                list.add(map);
            }
        }
        return list;
    }

    /**
     * Update fields4 cross price.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return updateFields4CrossPrice(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final List<Map<String, Object>> list = new ArrayList<>();
        final Map<String, Object> map = new HashMap<>();

        final int selected = getSelectedRow(_parameter);

        final List<Calculator> calcList = analyseTable(_parameter, null);

        final Calculator cal = calcList.get(selected);
        cal.setCrossPrice(_parameter.getParameterValues("crossPrice")[selected]);
        if (calcList.size() > 0) {
            add2Map4UpdateField(_parameter, map, calcList, cal, true);
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        }
        return retVal;
    }

    /**
     * Add to the map for update field.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _map Map the values will be added to
     * @param _calcList list of all calculators
     * @param _cal current calculator
     * @param _includeTotal the include total
     * @throws EFapsException on error
     */
    protected void add2Map4UpdateField(final Parameter _parameter,
                                       final Map<String, Object> _map,
                                       final List<Calculator> _calcList,
                                       final Calculator _cal,
                                       final boolean _includeTotal)
        throws EFapsException
    {
        // positions
        if (_cal != null) {
            _map.put("quantity", _cal.getQuantityStr());
            _map.put("netUnitPrice", _cal.getNetUnitPriceFmtStr());
            _map.put("netUnitPrice4Read", _cal.getNetUnitPriceFmtStr());
            _map.put("netPrice", _cal.getNetPriceFmtStr());
            _map.put("discountNetUnitPrice", _cal.getDiscountNetUnitPriceFmtStr());
            _map.put("discount", _cal.getDiscountFmtStr());
            _map.put("crossPrice", _cal.getCrossPriceFmtStr());
        }
        if (_includeTotal) {
            // totals
            _map.put("netTotal", getNetTotalFmtStr(_parameter, _calcList));
            _map.put("crossTotal", getCrossTotalFmtStr(_parameter, _calcList));

            final StringBuilder js = new StringBuilder();
            js.append(getTaxesScript(_parameter,
                            new TaxesAttribute().getUI4ReadOnly(getRateTaxes(_parameter, _calcList, null))));
            InterfaceUtils.appendScript4FieldUpdate(_map, js);
            if (Sales.PERCEPTIONCERTIFICATEACTIVATE.get()) {
                _map.put("perceptionTotal", getPerceptionTotalFmtStr(_parameter, _calcList));
            }
        }
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _innerHtml innerHtml part of the taxfield
     * @return StringBuilder with Javascript
     */
    protected StringBuilder getTaxesScript(final Parameter _parameter,
                                           final String _innerHtml)
    {
        return getTaxesScript(_parameter, "taxes", _innerHtml);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _fieldName fieldName
     * @param _innerHtml innerHtml part of the taxfield
     * @return StringBuilder with Javascript
     */
    protected StringBuilder getTaxesScript(final Parameter _parameter,
                                           final String _fieldName,
                                           final String _innerHtml)
    {
        return new StringBuilder()
                        .append("require([\"dojo/query\", \"dojo/NodeList-manipulate\"], function(query){")
                        .append("query(\"[name='").append(_fieldName).append("']\").innerHTML(\"")
                        .append(_innerHtml)
                        .append("\");")
                        .append("});");
    }

    /**
     * Method is executed as an update event of the field containing the net
     * unit price for products to calculate the new totals.
     *
     * @param _parameter Parameter as passed by the eFasp API
     * @return Return containing the list
     * @throws EFapsException on error
     */
    public Return updateFields4NetUnitPrice(final Parameter parameter)
        throws EFapsException
    {
        if (isRest()) {
            return new Return().put(ReturnValues.VALUES, calculate(parameter));
        }

        final Return retVal = new Return();
        final List<Map<String, Object>> list = new ArrayList<>();
        final Map<String, Object> map = new HashMap<>();

        final int selected = getSelectedRow(parameter);
        final List<Calculator> calcList = analyseTable(parameter, null);
        final Calculator cal = calcList.get(selected);
        if (calcList.size() > 0) {
            add2Map4UpdateField(parameter, map, calcList, cal, true);
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
    public Return updateFields4Discount(final Parameter parameter)
        throws EFapsException
    {
        if (isRest()) {
            return new Return().put(ReturnValues.VALUES, calculate(parameter));
        }
        final Return retVal = new Return();
        final List<Map<String, Object>> list = new ArrayList<>();
        final Map<String, Object> map = new HashMap<>();
        final int selected = getSelectedRow(parameter);

        final List<Calculator> calcList = analyseTable(parameter, null);
        final Calculator cal = calcList.get(selected);
        if (calcList.size() > 0) {
            add2Map4UpdateField(parameter, map, calcList, cal, true);
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
    public Return updateFields4Product(final Parameter parameter)
        throws EFapsException
    {
        if (isRest()) {
            final var list = calculate(parameter);
            int idx = 0;
            var prodInst = Instance.get(getValue(parameter, "product", idx));
            while (InstanceUtils.isKindOf(prodInst, CIProducts.ProductAbstract)) {
                final var map = list.get(idx);
                add2UpdateField4Product(parameter, map, prodInst);
                idx++;
                prodInst = Instance.get(getValue(parameter, "product", idx));
            }
            return new Return().put(ReturnValues.VALUES, list);
        }
        final Return retVal = new Return();
        final List<Map<String, Object>> list = new ArrayList<>();
        final Map<String, Object> map = new HashMap<>();

        final int selected = getSelectedRow(parameter);
        final Field field = (Field) parameter.get(ParameterValues.UIOBJECT);
        final String fieldName = field.getName();
        final Instance prodInst = Instance.get(parameter.getParameterValues(fieldName)[selected]);

        // validate that a product was selected
        if (prodInst.isValid()) {
            add2UpdateField4Product(parameter, map, prodInst);
            final List<Calculator> calcList = analyseTable(parameter, selected);
            if (calcList.size() > 0) {
                final Calculator cal = calcList.get(selected);
                add2Map4UpdateField(parameter, map, calcList, cal, true);
            }
        }
        list.add(map);
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * @param _parameter Paraemter as passed by the eFasp API
     * @return List map for the update event
     * @throws EFapsException on error
     */
    public Return updateFields4Uom(final Parameter parameter)
        throws EFapsException
    {
        if (isRest()) {
            LOG.warn("UoM changes are not evaluated", parameter);
            return new Return().put(ReturnValues.VALUES, calculate(parameter));
        }
        final Return retVal = new Return();
        final List<Map<String, Object>> list = new ArrayList<>();
        final Map<String, Object> map = new HashMap<>();

        final int selected = getSelectedRow(parameter);
        final List<Calculator> calcList = analyseTable(parameter, null);
        if (calcList.size() > 0) {
            final Calculator cal = calcList.get(selected);

            final Long uomID = Long.parseLong(parameter.getParameterValues("uoM")[selected]);
            final UoM uom = Dimension.getUoM(uomID);
            final BigDecimal up = cal.getProductPrice().getCurrentPrice().multiply(new BigDecimal(uom.getNumerator()))
                            .divide(new BigDecimal(uom.getDenominator()));
            cal.setUnitPrice(up);
            add2Map4UpdateField(parameter, map, calcList, cal, true);
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
        return super.getJavaScript4Positions(_parameter, _instances).append(
                        InterfaceUtils.wrappInScriptTag(_parameter, "executeCalculator();\n", false, 2000));
    }

    @Override
    protected StringBuilder getJavaScript4Positions(final Parameter _parameter,
                                                    final Instance _instance)
        throws EFapsException
    {
        return super.getJavaScript4Positions(_parameter, _instance).append(
                        InterfaceUtils.wrappInScriptTag(_parameter, "executeCalculator();\n", false, 2000));
    }

    /**
     * Update the form after change of rate currency.
     *
     * @param _parameter Parameter as passed by the eFaps API for esjp
     * @return javascript for update
     * @throws EFapsException on error
     */
    public Return updateFields4RateCurrency(final Parameter parameter)
        throws EFapsException
    {
        if (isRest()) {
            final Instance newInst = getRateCurrencyInstance(parameter, null);
            final var rateInfo = new Currency().evaluateRateInfo(parameter, parameter.getParameterValue("date"),
                            newInst);
            final var list = calculate(parameter);
            if (list.isEmpty()) {
                final var map = new HashMap<String, Object>();
                map.put("rateCurrencyData", rateInfo.getRateUI());
                list.add(map);
            } else {
                list.get(0).put("rateCurrencyData", rateInfo.getRateUI());
            }
            return new Return().put(ReturnValues.VALUES, list);
        }

        final List<Map<String, String>> list = new ArrayList<>();

        final Instance newInst = getRateCurrencyInstance(parameter, null);
        final Map<String, String> map = new HashMap<>();
        Instance currentInst = new Currency().getCurrencyFromUI(parameter, "rateCurrencyId_eFapsPrevious");
        final Instance baseInst = Currency.getBaseCurrency();
        if (currentInst == null || currentInst != null && !currentInst.isValid()) {
            currentInst = baseInst;
        }

        if (!newInst.equals(currentInst)) {
            final Currency currency = new Currency();
            final RateInfo[] rateInfos = currency.evaluateRateInfos(parameter,
                            parameter.getParameterValue("date_eFapsDate"), currentInst, newInst);

            final List<Calculator> calculators = analyseTable(parameter, null);

            final StringBuilder js = new StringBuilder();
            int i = 0;
            final Map<Integer, Map<String, Object>> values = new TreeMap<>();
            for (final Calculator calculator : calculators) {
                final Map<String, Object> map2 = new HashMap<>();
                if (!calculator.isEmpty()) {
                    calculator.applyRate(newInst, RateInfo.getRate(parameter, rateInfos[2],
                                    getTypeName4SysConf(parameter)));
                    map2.put("netUnitPrice", calculator.getNetUnitPriceFmtStr());
                    map2.put("netUnitPrice4Read", calculator.getNetUnitPriceFmtStr());
                    map2.put("netPrice", calculator.getNetPriceFmtStr());
                    map2.put("discountNetUnitPrice", calculator.getDiscountNetUnitPriceFmtStr());
                    map2.put("crossPrice", calculator.getCrossPriceFmtStr());
                    values.put(i, map2);
                }
                i++;
            }

            final Set<String> noEscape = new HashSet<>();
            add2SetValuesString4Postions4CurrencyUpdate(parameter, calculators, values, noEscape);
            js.append(getSetFieldValuesScript(parameter, values.values(), noEscape));

            if (calculators.size() > 0) {
                js.append(getSetFieldValue(0, "crossTotal", getCrossTotalFmtStr(parameter, calculators)))
                                .append(getSetFieldValue(0, "netTotal", getNetTotalFmtStr(parameter, calculators)))
                                .append(addFields4RateCurrency(parameter, calculators));
                if (parameter.getParameterValue("openAmount") != null) {
                    js.append(getSetFieldValue(0, "openAmount", getBaseCrossTotalFmtStr(parameter, calculators)));
                }
                if (Sales.PERCEPTIONCERTIFICATEACTIVATE.get()) {
                    js.append(getSetFieldValue(0, "perceptionTotal",
                                    getPerceptionTotalFmtStr(parameter, calculators)));
                }
            }
            js.append(getSetFieldValue(0, "rateCurrencyData", getRateUIFrmt(parameter, rateInfos[1])))
                            .append(getSetFieldValue(0, "rate", getRateUIFrmt(parameter, rateInfos[1])))
                            .append(getSetFieldValue(0, "rate" + RateUI.INVERTEDSUFFIX,
                                            Boolean.toString(rateInfos[1].isInvert())))
                            .append(getSetFieldValue(0, "rateCurrencyId_eFapsPrevious",
                                            parameter.getParameterValue("rateCurrencyId")))
                            .append(addAdditionalFields4CurrencyUpdate(parameter, calculators));

            map.put("eFapsFieldUpdateJS", js.toString());
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
        final Instance curInst = getCurrencyFromUI(_parameter);
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
                                                               final Map<Integer, Map<String, Object>> _values,
                                                               final Set<String> _noEscape)
        throws EFapsException
    {
        // to be used by implementations
    }

    /**
     * Update the form after change of date.
     *
     * @param _parameter Parameter as passed by the eFaps API.
     * @return JavaScript for update.
     * @throws EFapsException on error.
     */
    public Return updateFields4Date(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, String>> list = new ArrayList<>();
        final Map<String, String> map = new HashMap<>();
        final Instance newCurrInst = getRateCurrencyInstance(_parameter, null);
        final String date = _parameter.getParameterValue("date_eFapsDate");

        final StringBuilder js = new StringBuilder();
        final RateInfo rateInfo = new Currency().evaluateRateInfo(_parameter, date, newCurrInst);

        js.append(getSetFieldValue(0, "rateCurrencyData",
                        RateInfo.getRateUIFrmt(_parameter, rateInfo, getTypeName4SysConf(_parameter))))
                        .append(getSetFieldValue(0, "rate",
                                        RateInfo.getRateUIFrmt(_parameter, rateInfo, getTypeName4SysConf(_parameter))))
                        .append(getSetFieldValue(0, "rate" + RateUI.INVERTEDSUFFIX,
                                        Boolean.toString(rateInfo.isInvert())));

        map.put("eFapsFieldUpdateJS", js.toString());
        new Channel().add2FieldUpdateMap4Condition(_parameter, map);
        list.add(map);
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * Update the form after change of date. (Seems to be unused)
     *
     * @param _parameter Parameter as passed by the eFaps API for esjp
     * @return javascript for update
     * @throws EFapsException on error
     */
    public Return updateFields4RateCurrencyFromDate(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, String>> list = new ArrayList<>();
        final Map<String, String> map = new HashMap<>();
        final Instance newInst = getRateCurrencyInstance(_parameter, null);

        final Currency currency = new Currency();
        final RateInfo rateInfo = currency.evaluateRateInfo(_parameter, _parameter.getParameterValue("date_eFapsDate"),
                        newInst);

        final List<Calculator> calculators = analyseTable(_parameter, null);

        final StringBuilder js = new StringBuilder();
        int i = 0;
        final Map<Integer, Map<String, Object>> values = new TreeMap<>();
        for (final Calculator calculator : calculators) {
            final Map<String, Object> map2 = new HashMap<>();
            if (!calculator.isEmpty()) {
                final QueryBuilder qlb = new QueryBuilder(CISales.PositionAbstract);
                qlb.addWhereAttrEqValue(CISales.PositionAbstract.Product, Instance.get(calculator.getOid()));
                qlb.addWhereAttrEqValue(CISales.PositionAbstract.DocumentAbstractLink, _parameter.getInstance());
                final InstanceQuery query = qlb.getQuery();
                query.execute();
                if (!query.next()) {
                    calculator.applyRate(newInst, RateInfo.getRate(_parameter, rateInfo,
                                    getTypeName4SysConf(_parameter)));
                }
                map2.put("netUnitPrice", calculator.getNetUnitPriceFmtStr());
                map2.put("netUnitPrice4Read", calculator.getNetUnitPriceFmtStr());
                map2.put("netPrice", calculator.getNetPriceFmtStr());
                map2.put("discountNetUnitPrice", calculator.getDiscountNetUnitPriceFmtStr());
                map2.put("crossPrice", calculator.getCrossPriceFmtStr());
                values.put(i, map2);
            }
            i++;
        }

        final Set<String> noEscape = new HashSet<>();
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
        js.append(getSetFieldValue(0, "rateCurrencyData", rateInfo.getRateUIFrmt(null)))
                        .append(getSetFieldValue(0, "rate", rateInfo.getRateUIFrmt(null)))
                        .append(getSetFieldValue(0, "rate" + RateUI.INVERTEDSUFFIX,
                                        Boolean.toString(rateInfo.isInvert())))
                        .append(addAdditionalFields4CurrencyUpdate(_parameter, calculators));

        map.put("eFapsFieldUpdateJS", js.toString());
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
        final Instance baseCurrInst = Currency.getBaseCurrency();

        final Instance rateCurrInst = getRateCurrencyInstance(_parameter, _createdDoc);
        final Object[] rateObj = getRateObject(_parameter);
        final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                        RoundingMode.HALF_UP);

        @SuppressWarnings("unchecked") final List<Calculator> calcList = (List<Calculator>) _createdDoc.getValue(
                        AbstractDocument_Base.CALCULATORS_VALUE);

        final DecimalFormat totalFrmt = NumberFormatter.get().getFrmt4Total(getType4SysConf(_parameter));
        final int scale = totalFrmt.getMaximumFractionDigits();

        final DecimalFormat unitFrmt = NumberFormatter.get().getFrmt4UnitPrice(getType4SysConf(_parameter));
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

                final String[] remarks = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                                CISales.PositionAbstract.Remark.name));
                if (remarks != null && remarks.length > idx) {
                    posIns.add(CISales.PositionAbstract.Remark, remarks[idx]);
                }

                final String[] uoM = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                                CISales.PositionAbstract.UoM.name));
                if (uoM != null && uoM.length > idx) {
                    posIns.add(CISales.PositionAbstract.UoM, uoM[idx]);
                }

                posIns.add(CISales.PositionSumAbstract.Quantity, calc.getQuantity());
                posIns.add(CISales.PositionSumAbstract.CrossUnitPrice, calc.getCrossUnitPrice()
                                .divide(rate, RoundingMode.HALF_UP).setScale(uScale, RoundingMode.HALF_UP));
                posIns.add(CISales.PositionSumAbstract.NetUnitPrice, calc.getNetUnitPrice()
                                .divide(rate, RoundingMode.HALF_UP).setScale(uScale, RoundingMode.HALF_UP));
                posIns.add(CISales.PositionSumAbstract.CrossPrice, calc.getCrossPrice()
                                .divide(rate, RoundingMode.HALF_UP).setScale(scale, RoundingMode.HALF_UP));
                posIns.add(CISales.PositionSumAbstract.NetPrice, calc.getNetPrice()
                                .divide(rate, RoundingMode.HALF_UP).setScale(scale, RoundingMode.HALF_UP));
                posIns.add(CISales.PositionSumAbstract.Tax, calc.getTaxCatId());
                final Taxes taxes = calc.getTaxes(baseCurrInst);
                taxes.getEntries().forEach(entry -> {
                    entry.setAmount(entry.getAmount().divide(rate, RoundingMode.HALF_UP));
                    entry.setBase(entry.getBase().divide(rate, RoundingMode.HALF_UP));
                });
                posIns.add(CISales.PositionSumAbstract.Taxes, taxes);
                posIns.add(CISales.PositionSumAbstract.Discount, calc.getDiscount());
                posIns.add(CISales.PositionSumAbstract.DiscountNetUnitPrice, calc.getDiscountNetUnitPrice()
                                .divide(rate, RoundingMode.HALF_UP).setScale(uScale, RoundingMode.HALF_UP));
                posIns.add(CISales.PositionSumAbstract.CurrencyId, baseCurrInst);
                posIns.add(CISales.PositionSumAbstract.Rate, rateObj);
                posIns.add(CISales.PositionSumAbstract.RateCurrencyId, rateCurrInst);
                posIns.add(CISales.PositionSumAbstract.RateNetUnitPrice, calc.getNetUnitPrice()
                                .setScale(uScale, RoundingMode.HALF_UP));
                posIns.add(CISales.PositionSumAbstract.RateCrossUnitPrice, calc.getCrossUnitPrice()
                                .setScale(uScale, RoundingMode.HALF_UP));
                posIns.add(CISales.PositionSumAbstract.RateDiscountNetUnitPrice, calc.getDiscountNetUnitPrice()
                                .setScale(uScale, RoundingMode.HALF_UP));
                posIns.add(CISales.PositionSumAbstract.RateNetPrice,
                                calc.getNetPrice().setScale(scale, RoundingMode.HALF_UP));
                posIns.add(CISales.PositionSumAbstract.RateCrossPrice,
                                calc.getCrossPrice().setScale(scale, RoundingMode.HALF_UP));
                posIns.add(CISales.PositionSumAbstract.RateTaxes, calc.getTaxes(rateCurrInst));
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

    protected void updatePositions(final Parameter parameter,
                                   final Instance docInstance)
        throws EFapsException
    {
        final var eval = EQL.builder().print()
                        .query(CISales.PositionSumAbstract)
                        .where()
                        .attribute(CISales.PositionSumAbstract.DocumentAbstractLink).eq(docInstance)
                        .select().attribute(CISales.PositionSumAbstract.PositionNumber)
                        .orderBy(CISales.PositionSumAbstract.PositionNumber)
                        .evaluate();
        final var posInstances = new ArrayList<Instance>();
        while (eval.next()) {
            posInstances.add(eval.inst());
        }
        final var existingIter = posInstances.iterator();

        final var service = new CalculatorService(getCIType().getType().getName())
        {

            @Override
            protected BigDecimal evalNetUnitPrice(final Object dateObject,
                                                  final Instance positionInst,
                                                  final Instance productInst)
                throws EFapsException
            {
                final var eval = EQL.builder().print(positionInst)
                                .attribute(CISales.PositionSumAbstract.RateNetUnitPrice)
                                .evaluate();
                eval.next();
                return eval.get(CISales.PositionSumAbstract.RateNetUnitPrice);
            }
        };

        int idx = 0;
        var prodInst = Instance.get(getValue(parameter, "product", idx));
        while (InstanceUtils.isKindOf(prodInst, CIProducts.ProductAbstract)) {

            final var prodEval = EQL.builder().print(prodInst)
                            .attribute(CIProducts.ProductAbstract.TaxCategory)
                            .evaluate();
            prodEval.next();
            final var taxCatId = prodEval.<Long>get(CIProducts.ProductAbstract.TaxCategory);

            final var netUnitPrice =  evalNetUnitPrice(parameter, idx, service, prodInst);
            final Map<CIAttribute, Object> values = new HashMap<>();
            values.put(CISales.PositionSumAbstract.DocumentAbstractLink, docInstance);
            values.put(CISales.PositionSumAbstract.PositionNumber, idx + 1);
            values.put(CISales.PositionSumAbstract.Quantity, evalQuantity(parameter, idx));
            values.put(CISales.PositionSumAbstract.Product, prodInst);
            values.put(CISales.PositionSumAbstract.ProductDesc, getValue(parameter, "productDesc", idx));
            values.put(CISales.PositionSumAbstract.UoM, getValue(parameter, "uoM", idx));
            values.put(CISales.PositionSumAbstract.RateNetUnitPrice, netUnitPrice);
            values.put(CISales.PositionSumAbstract.NetUnitPrice, netUnitPrice);
            values.put(CISales.PositionSumAbstract.Tax, taxCatId);
            values.put(CISales.PositionSumAbstract.Discount, evalBigDecimal(parameter, "discount", idx));
            values.put(CISales.PositionSumAbstract.RateCurrencyId, getValue(parameter, "rateCurrencyId") );
            values.put(CISales.PositionSumAbstract.CurrencyId, Currency.getBaseCurrency());

            // set defaults
            values.put(CISales.PositionSumAbstract.Rate, new Object[] { BigDecimal.ONE , BigDecimal.ONE });
            values.put(CISales.PositionSumAbstract.CrossUnitPrice, BigDecimal.ZERO);
            values.put(CISales.PositionSumAbstract.CrossPrice, BigDecimal.ZERO);
            values.put(CISales.PositionSumAbstract.NetPrice, BigDecimal.ZERO);
            values.put(CISales.PositionSumAbstract.DiscountNetUnitPrice, BigDecimal.ZERO);
            values.put(CISales.PositionSumAbstract.RateCrossUnitPrice, BigDecimal.ZERO);
            values.put(CISales.PositionSumAbstract.RateDiscountNetUnitPrice, BigDecimal.ZERO);
            values.put(CISales.PositionSumAbstract.RateNetPrice, BigDecimal.ZERO);
            values.put(CISales.PositionSumAbstract.RateCrossPrice, BigDecimal.ZERO);

            if (existingIter.hasNext()) {
                upsert(existingIter.next(), values);
            } else {
                upsert(getType4PositionUpdate(parameter), values);
            }
            idx++;
            prodInst = Instance.get(getValue(parameter, "product", idx));
        }
        if (existingIter.hasNext()) {
            EQL.builder().delete(existingIter.next()).stmt().execute();
        }

        service.recalculate(docInstance);
    }

    protected Instance upsert(final Type type,
                              final Map<CIAttribute, Object> values)
        throws EFapsException
    {
        return upsert(Instance.get(type, 0), values);
    }

    protected Instance upsert(final Instance instance,
                              final Map<CIAttribute, Object> values)
        throws EFapsException
    {
        final Instance ret;
        if (instance.isValid()) {
            ret = instance;
            final var update = EQL.builder().update(instance);
            for (final var entry : values.entrySet()) {
                update.set(entry.getKey(), entry.getValue());
            }
            update.execute();
        } else {
            final var insert = EQL.builder().insert(instance.getType());
            for (final var entry : values.entrySet()) {
                insert.set(entry.getKey(), entry.getValue());
            }
            ret = insert.execute();
        }
        return ret;
    }

    /**
     * Update the positions of a Document.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _editDoc EditDoc the postions that will be updated belong to
     * @throws EFapsException on error
     */
    protected void updatePositions(final Parameter _parameter,
                                   final EditedDoc _editDoc)
        throws EFapsException
    {
        if (isRest()) {
            updatePositions(_parameter, _editDoc.getInstance());
            return;
        }

        final Instance baseCurrInst = Currency.getBaseCurrency();
        final Instance rateCurrInst = getRateCurrencyInstance(_parameter, _editDoc);

        final Object[] rateObj = getRateObject(_parameter);
        final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                        RoundingMode.HALF_UP);

        @SuppressWarnings("unchecked") final List<Calculator> calcList = (List<Calculator>) _editDoc
                        .getValue(AbstractDocument_Base.CALCULATORS_VALUE);
        @SuppressWarnings("unchecked") final Map<String, String> oidMap = (Map<String, String>) _parameter
                        .get(ParameterValues.OIDMAP4UI);
        final String[] rowKeys = _parameter.getParameterValues("eFapsTRID");

        final DecimalFormat totalFrmt = NumberFormatter.get().getFrmt4Total(getType4SysConf(_parameter));
        final int scale = totalFrmt.getMaximumFractionDigits();

        final DecimalFormat unitFrmt = NumberFormatter.get().getFrmt4UnitPrice(getType4SysConf(_parameter));
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

                final String[] remarks = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                                CISales.PositionAbstract.Remark.name));
                if (remarks != null && remarks.length > i) {
                    update.add(CISales.PositionAbstract.Remark, remarks[i]);
                }

                final String[] uoM = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                                CISales.PositionAbstract.UoM.name));
                if (uoM != null && uoM.length > i) {
                    update.add(CISales.PositionAbstract.UoM, uoM[i]);
                }

                update.add(CISales.PositionSumAbstract.Quantity, calc.getQuantity());
                update.add(CISales.PositionSumAbstract.CrossUnitPrice, calc.getCrossUnitPrice()
                                .divide(rate, RoundingMode.HALF_UP).setScale(uScale, RoundingMode.HALF_UP));
                update.add(CISales.PositionSumAbstract.NetUnitPrice, calc.getNetUnitPrice()
                                .divide(rate, RoundingMode.HALF_UP).setScale(uScale, RoundingMode.HALF_UP));
                update.add(CISales.PositionSumAbstract.CrossPrice, calc.getCrossPrice()
                                .divide(rate, RoundingMode.HALF_UP).setScale(scale, RoundingMode.HALF_UP));
                update.add(CISales.PositionSumAbstract.NetPrice, calc.getNetPrice()
                                .divide(rate, RoundingMode.HALF_UP).setScale(scale, RoundingMode.HALF_UP));
                update.add(CISales.PositionSumAbstract.Tax, calc.getTaxCatId());
                update.add(CISales.PositionSumAbstract.Discount, calc.getDiscountStr());
                update.add(CISales.PositionSumAbstract.DiscountNetUnitPrice, calc.getDiscountNetUnitPrice()
                                .divide(rate, RoundingMode.HALF_UP).setScale(uScale, RoundingMode.HALF_UP));
                update.add(CISales.PositionSumAbstract.CurrencyId, baseCurrInst);
                final Taxes taxes = calc.getTaxes(baseCurrInst);
                taxes.getEntries().forEach(entry -> {
                    entry.setAmount(entry.getAmount().divide(rate, RoundingMode.HALF_UP));
                    entry.setBase(entry.getBase().divide(rate, RoundingMode.HALF_UP));
                });
                update.add(CISales.PositionSumAbstract.Taxes, taxes);
                update.add(CISales.PositionSumAbstract.Rate, rateObj);
                update.add(CISales.PositionSumAbstract.RateCurrencyId, rateCurrInst);
                update.add(CISales.PositionSumAbstract.RateNetUnitPrice, calc.getNetUnitPrice()
                                .setScale(uScale, RoundingMode.HALF_UP));
                update.add(CISales.PositionSumAbstract.RateCrossUnitPrice, calc.getCrossUnitPrice()
                                .setScale(uScale, RoundingMode.HALF_UP));
                update.add(CISales.PositionSumAbstract.RateDiscountNetUnitPrice, calc.getDiscountNetUnitPrice()
                                .setScale(uScale, RoundingMode.HALF_UP));
                update.add(CISales.PositionSumAbstract.RateNetPrice,
                                calc.getNetPrice().setScale(scale, RoundingMode.HALF_UP));
                update.add(CISales.PositionSumAbstract.RateCrossPrice,
                                calc.getCrossPrice().setScale(scale, RoundingMode.HALF_UP));
                update.add(CISales.PositionSumAbstract.RateTaxes, calc.getTaxes(rateCurrInst));
                add2PositionUpdate(_parameter, calc, update, i);
                update.execute();
                _editDoc.addPosition(update.getInstance());
            }
        }
        deletePosition4Update(_parameter, _editDoc);
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API
     * @return Return containing list
     * @throws EFapsException on error
     */
    public Return executeCalculatorOnScript(final Parameter _parameter)
        throws EFapsException
    {
        final Instance derivedInst = Instance.get(_parameter.getParameterValue("derived"));
        Integer row4priceFromDB = null;
        if (derivedInst != null && derivedInst.isValid()
                        && derivedInst.getType().isKindOf(CISales.DocumentStockAbstract)) {
            row4priceFromDB = -1;
        }

        final Return retVal = new Return();
        final List<Map<String, Object>> list = new ArrayList<>();
        final List<Calculator> calcList = analyseTable(_parameter, row4priceFromDB);
        int i = 0;
        for (final Calculator cal : calcList) {
            // always add the first and than only the ones visible in the
            // userinterface
            if (i == 0 || !cal.isBackground()) {
                final Map<String, Object> map = new HashMap<>();
                _parameter.getParameters().put("eFapsRowSelectedRow", new String[] { "" + i });
                add2Map4UpdateField(_parameter, map, calcList, cal, i == 0);
                list.add(map);
            }
            i++;
        }
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * Recalculate the rate values by instantiating calculators and simulating
     * the interaction with the form.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return map list for update event
     * @throws EFapsException on error
     */
    public Return recalculateRate(final Parameter _parameter)
        throws EFapsException
    {
        final List<Instance> instances;
        final Instance instance = _parameter.getInstance();
        if (InstanceUtils.isKindOf(instance, CISales.DocumentSumAbstract)) {
            instances = new ArrayList<>();
            instances.add(instance);
        } else {
            final List<Instance> selInsts = getInstances(_parameter, "", true);
            if (containsProperty(_parameter, "Select4Instance")) {
                instances = new ArrayList<>();
                final String select = getProperty(_parameter, "Select4Instance");
                final MultiPrintQuery multi = new MultiPrintQuery(selInsts);
                multi.addSelect(select);
                multi.execute();
                while (multi.next()) {
                    instances.add(multi.getSelect(select));
                }
            } else {
                instances = selInsts;
            }
        }
        for (final Instance docInst : instances) {
            if (InstanceUtils.isKindOf(docInst, CISales.DocumentSumAbstract)) {
                final PrintQuery print = new PrintQuery(docInst);
                final SelectBuilder selRateCurInst = SelectBuilder.get().linkto(
                                CISales.DocumentSumAbstract.RateCurrencyId).instance();
                print.addSelect(selRateCurInst);
                print.addAttribute(CISales.DocumentSumAbstract.Date, CISales.DocumentSumAbstract.Rate);
                print.execute();

                final Instance baseCurrInst = Currency.getBaseCurrency();
                final Instance rateCurrInst = print.<Instance>getSelect(selRateCurInst);
                if (!baseCurrInst.equals(rateCurrInst)) {

                    final String dateStr = _parameter.getParameterValue(
                                    CIFormSales.Sales_DocumentSum_RecalculateForm.date.name + "_eFapsDate");
                    final DateTime date;
                    if (dateStr == null) {
                        date = print.getAttribute(CISales.DocumentSumAbstract.Date);
                    } else {
                        date = JodaTimeUtils.getDateFromParameter(dateStr);
                    }
                    final Currency currency = new Currency();
                    final RateInfo rateInfo = currency.evaluateRateInfo(_parameter, date, rateCurrInst);
                    final BigDecimal rate = RateInfo.getRate(_parameter, rateInfo, docInst.getType().getName());
                    final Object[] rateObj = RateInfo.getRateObject(_parameter, rateInfo, docInst.getType().getName());

                    final Object[] currentRateObj = print.getAttribute(CISales.DocumentSumAbstract.Rate);

                    if (((BigDecimal) currentRateObj[0]).compareTo((BigDecimal) rateObj[0]) != 0
                                    || ((BigDecimal) currentRateObj[1]).compareTo((BigDecimal) rateObj[1]) != 0) {

                        final DecimalFormat frmt = NumberFormatter.get().getFormatter();

                        final DecimalFormat totalFrmt = NumberFormatter.get().getFrmt4Total(getType4SysConf(
                                        _parameter));
                        final int scale = totalFrmt.getMaximumFractionDigits();

                        final DecimalFormat unitFrmt = NumberFormatter.get().getFrmt4UnitPrice(getType4SysConf(
                                        _parameter));
                        final int uScale = unitFrmt.getMaximumFractionDigits();

                        final List<Calculator> calcList = new ArrayList<>();
                        final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionSumAbstract);
                        queryBldr.addWhereAttrEqValue(CISales.PositionSumAbstract.DocumentAbstractLink, docInst);
                        final MultiPrintQuery multi = queryBldr.getPrint();
                        final SelectBuilder selProdOid = SelectBuilder.get().linkto(CISales.PositionSumAbstract.Product)
                                        .oid();
                        multi.addSelect(selProdOid);
                        multi.addAttribute(CISales.PositionSumAbstract.Quantity,
                                        CISales.PositionSumAbstract.RateNetUnitPrice,
                                        CISales.PositionSumAbstract.Discount);
                        multi.execute();
                        while (multi.next()) {
                            // read the ratevalues
                            final BigDecimal quantity = multi.<BigDecimal>getAttribute(
                                            CISales.PositionSumAbstract.Quantity);
                            final BigDecimal unitPrice = multi.<BigDecimal>getAttribute(
                                            CISales.PositionSumAbstract.RateNetUnitPrice);
                            final BigDecimal discount = multi.<BigDecimal>getAttribute(
                                            CISales.PositionSumAbstract.Discount);
                            final String prodOid = multi.<String>getSelect(selProdOid);
                            final Calculator calc = getCalculator(_parameter, null, prodOid, frmt.format(quantity),
                                            unitFrmt.format(unitPrice), frmt.format(discount), false, 0);
                            calcList.add(calc);

                            // update the base values for the position
                            final Update update = new Update(multi.getCurrentInstance());
                            update.add(CISales.PositionSumAbstract.CrossUnitPrice, calc.getCrossUnitPrice().divide(rate,
                                            RoundingMode.HALF_UP).setScale(uScale, RoundingMode.HALF_UP));
                            update.add(CISales.PositionSumAbstract.NetUnitPrice, calc.getNetUnitPrice().divide(rate,
                                            RoundingMode.HALF_UP).setScale(uScale, RoundingMode.HALF_UP));
                            update.add(CISales.PositionSumAbstract.CrossPrice, calc.getCrossPrice().divide(rate,
                                            RoundingMode.HALF_UP).setScale(scale, RoundingMode.HALF_UP));
                            update.add(CISales.PositionSumAbstract.NetPrice, calc.getNetPrice().divide(rate,
                                            RoundingMode.HALF_UP).setScale(scale, RoundingMode.HALF_UP));
                            update.add(CISales.PositionSumAbstract.Tax, calc.getTaxCatId());
                            update.add(CISales.PositionSumAbstract.Discount, calc.getDiscount());
                            update.add(CISales.PositionSumAbstract.DiscountNetUnitPrice, calc.getDiscountNetUnitPrice()
                                            .divide(rate, RoundingMode.HALF_UP).setScale(uScale,
                                                            RoundingMode.HALF_UP));
                            update.add(CISales.PositionSumAbstract.CurrencyId, baseCurrInst.getId());
                            update.add(CISales.PositionSumAbstract.Rate, rateObj);
                            update.execute();
                        }
                        // update the base values for the document
                        final Update update = new Update(docInst);
                        final BigDecimal netTotal = getNetTotal(_parameter, calcList).divide(rate,
                                        RoundingMode.HALF_UP).setScale(scale, RoundingMode.HALF_UP);
                        update.add(CISales.DocumentSumAbstract.NetTotal, netTotal);

                        update.add(CISales.DocumentSumAbstract.Taxes, getTaxes(_parameter, calcList, rate,
                                        baseCurrInst));

                        final BigDecimal crossTotal = getCrossTotal(_parameter, calcList).divide(rate,
                                        RoundingMode.HALF_UP).setScale(scale, RoundingMode.HALF_UP);
                        update.add(CISales.DocumentSumAbstract.CrossTotal, crossTotal);
                        update.add(CISales.DocumentSumAbstract.Rate, rateObj);
                        update.execute();
                    }
                }
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
            print.addAttribute(CISales.DocumentSumAbstract.RateCurrencyId);
            print.execute();

            final CurrencyInst curInst = CurrencyInst
                            .get(print.<Long>getAttribute(CISales.DocumentSumAbstract.RateCurrencyId));
            final RateInfo rateInfo = new Currency().evaluateRateInfo(_parameter,
                            _parameter.getParameterValue("date_eFapsDate"), curInst.getInstance());
            final BigDecimal rate = RateInfo.getRateUI(_parameter, rateInfo, docInst.getType().getName());

            final DecimalFormat formater = (DecimalFormat) NumberFormat.getInstance(
                            Context.getThreadContext().getLocale());
            formater.applyPattern("#,##0.############");
            formater.setRoundingMode(RoundingMode.HALF_UP);
            final String rateStr = formater.format(rate);
            final List<Map<String, String>> list = new ArrayList<>();
            final Map<String, String> map = new HashMap<>();
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
            ret = _rateValue.divide(_newRate, RoundingMode.HALF_UP)
                            .setScale(isDecimal4Doc(_docInst), RoundingMode.HALF_UP);
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
        final Map<Tax, TaxAmount> values = new HashMap<>();
        for (final Calculator calc : _calcList) {
            if (!calc.isWithoutTax()) {
                for (final TaxAmount taxAmount : calc.getTaxesAmounts()) {
                    if (!values.containsKey(taxAmount.getTax())) {
                        values.put(taxAmount.getTax(), new TaxAmount().setTax(taxAmount.getTax()));
                    }
                    values.get(taxAmount.getTax())
                                    .addAmount(taxAmount.getAmount())
                                    .addBase(taxAmount.getBase());
                }
            }
        }
        final Taxes ret = new Taxes();
        if (!_calcList.isEmpty()) {
            final Calculator calc = _calcList.iterator().next();
            UUID currencyUUID = null;
            if (_currencyInst != null) {
                final CurrencyInst curInst = CurrencyInst.get(_currencyInst);
                currencyUUID = curInst.getUUID();
            }
            for (final TaxAmount taxAmount : values.values()) {
                final TaxEntry taxentry = new TaxEntry();
                taxentry.setAmount(taxAmount.getAmount());
                taxentry.setBase(taxAmount.getBase());
                taxentry.setUUID(taxAmount.getTax().getUUID());
                taxentry.setCatUUID(taxAmount.getTax().getTaxCat().getUuid());
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
        for (final TaxEntry entry : ret.getEntries()) {
            entry.setAmount(entry.getAmount().divide(_rate, RoundingMode.HALF_UP));
            entry.setBase(entry.getBase().divide(_rate, RoundingMode.HALF_UP));
        }
        return ret;
    }

    /**
     * Gets the calculators for a document.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst Instance of a Document the List of Calculator is wanted
     *            for
     * @param _excludes Collection of Instances no Calculator is wanted for
     * @return List of Calculator
     * @throws EFapsException on error
     */
    protected List<Calculator> getCalculators4Doc(final Parameter _parameter,
                                                  final Instance _docInst,
                                                  final Collection<Instance> _excludes)
        throws EFapsException
    {
        final List<Calculator> ret = new ArrayList<>();
        final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionSumAbstract);
        queryBldr.addWhereAttrEqValue(CISales.PositionSumAbstract.DocumentAbstractLink, _docInst);
        queryBldr.addOrderByAttributeAsc(CISales.PositionSumAbstract.PositionNumber);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.setEnforceSorted(true);
        final SelectBuilder selProdInst = SelectBuilder.get().linkto(CISales.PositionSumAbstract.Product).instance();
        multi.addSelect(selProdInst);
        multi.addAttribute(CISales.PositionSumAbstract.Quantity, CISales.PositionSumAbstract.Discount,
                        CISales.PositionSumAbstract.RateNetUnitPrice, CISales.PositionSumAbstract.RateCrossUnitPrice,
                        CISales.PositionSumAbstract.PositionNumber);
        multi.execute();
        while (multi.next()) {
            if (_excludes == null || _excludes != null && !_excludes.contains(multi.getCurrentInstance())) {
                final BigDecimal quantity = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.Quantity);
                final BigDecimal discount = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.Discount);
                final BigDecimal unitPrice;
                if (Calculator.priceIsNet(_parameter, this)) {
                    unitPrice = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.RateNetUnitPrice);
                } else {
                    unitPrice = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.RateCrossUnitPrice);
                }
                final Integer idx = multi.<Integer>getAttribute(CISales.PositionSumAbstract.PositionNumber);
                final Instance prodInst = multi.<Instance>getSelect(selProdInst);
                ret.add(getCalculator(_parameter, null, prodInst, quantity, unitPrice, discount, false, idx));
            }
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
        return NumberFormatter.get().getFrmt4Total(getType4SysConf(_parameter))
                        .format(getCrossTotal(_parameter, _calcList));
    }

    /**
     * Method to get String representation of the cross total for a list of
     * Calculators.
     *
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
        return Calculator.getCrossTotal(_parameter, _calcList);
    }

    /**
     * Method to get formated String representation of the net total for a list
     * of Calculators.
     *
     * @param _parameter Parameter as passed by the eFasp API
     * @param _calcList list of Calculator the net total is wanted for
     * @return formated String representation of the net total
     * @throws EFapsException on error
     */
    protected String getNetTotalFmtStr(final Parameter _parameter,
                                       final List<Calculator> _calcList)
        throws EFapsException
    {
        return NumberFormatter.get().getFrmt4Total(getType4SysConf(_parameter))
                        .format(getNetTotal(_parameter, _calcList));
    }

    /**
     * Method to get String representation of the net total for a list of
     * Calculators.
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
        return Calculator.getNetTotal(_parameter, _calcList);
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
        return Calculator.getBaseCrossTotal(_parameter, _calcList);
    }

    /**
     * Method to get formated string representation of the base cross total for
     * a list of Calculators.
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
        return NumberFormatter.get().getFrmt4Total(getType4SysConf(_parameter))
                        .format(getBaseCrossTotal(_parameter, _calcList));
    }

    /**
     * Method to get String representation of the base cross total for a list of
     * Calculators.
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
        return NumberFormatter.get().getFrmt4Total(getType4SysConf(_parameter))
                        .format(getPerceptionTotal(_parameter, _calcList));
    }

    /**
     * Method to get String representation of the cross total for a list of
     * Calculators.
     *
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
        return Calculator.getPerceptionTotal(_parameter, _calcList);
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
        Return ret = new Return();
        final IUIValue uiValue = (IUIValue) _parameter.get(ParameterValues.UIOBJECT);

        if (uiValue.getField().isEditableDisplay((TargetMode) _parameter.get(ParameterValues.ACCESSMODE))) {
            final org.efaps.esjp.common.uiform.Field field = new org.efaps.esjp.common.uiform.Field()
            {

                @Override
                protected void add2QueryBuilder4List(final Parameter _parameter,
                                                     final QueryBuilder _queryBldr)
                    throws EFapsException
                {
                    final Map<Integer, String> activations = analyseProperty(_parameter, "Activation");
                    final List<DocTypeActivation> pactivt = new ArrayList<>();
                    for (final String activation : activations.values()) {
                        final DocTypeActivation pDAct = ERP.DocTypeActivation.valueOf(activation);
                        pactivt.add(pDAct);
                    }
                    if (!pactivt.isEmpty()) {
                        _queryBldr.addWhereAttrEqValue(CIERP.DocumentType.Activation, pactivt.toArray());
                    }
                    final Map<Integer, String> configurations = analyseProperty(_parameter, "Configuration");
                    final List<DocTypeConfiguration> configs = new ArrayList<>();
                    for (final String configuration : configurations.values()) {
                        final DocTypeConfiguration config = ERP.DocTypeConfiguration.valueOf(configuration);
                        configs.add(config);
                    }
                    if (!configs.isEmpty()) {
                        _queryBldr.addWhereAttrEqValue(CIERP.DocumentType.Configuration, configs.toArray());
                    }
                }
            };
            ret = field.getOptionListFieldValue(_parameter);
        }
        return ret;
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
        }.execute(_parameter);
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return new empty Return
     * @throws EFapsException on error
     */
    public Return changeDocumentType(final Parameter _parameter)
        throws EFapsException
    {
        final String value;
        if (_parameter.getParameterValue("documentType") != null) {
            value = _parameter.getParameterValue("documentType");
        } else {
            value = _parameter.getParameterValue("productDocumentType");
        }

        final Instance instDocType = Instance.get(value);
        if (InstanceUtils.isValid(instDocType)) {
            final List<Instance> documentInstances;
            if (InstanceUtils.isValid(_parameter.getInstance())) {
                documentInstances = Collections.singletonList(_parameter.getInstance());
            } else {
                documentInstances = getSelectedInstances(_parameter);
            }

            for (final Instance documentInst : documentInstances) {
                final QueryBuilder queryBldr = new QueryBuilder(getType4DocCreate(_parameter));
                queryBldr.addWhereAttrEqValue(CIERP.Document2DocumentTypeAbstract.DocumentLinkAbstract, documentInst);
                final InstanceQuery query = queryBldr.getQuery();
                query.execute();

                final Update update;
                if (query.next()) {
                    update = new Update(query.getCurrentValue());
                } else {
                    update = new Insert(getType4DocCreate(_parameter));
                    update.add(CIERP.Document2DocumentTypeAbstract.DocumentLinkAbstract, documentInst);
                }
                update.add(CIERP.Document2DocumentTypeAbstract.DocumentTypeLinkAbstract, instDocType);
                update.execute();
            }
        }
        return new Return();
    }

    /**
     * Validate connect document.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return validateConnectDocument(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Map<?, ?> others = (HashMap<?, ?>) _parameter.get(ParameterValues.OTHERS);
        final StringBuilder html = new StringBuilder();
        final String[] childOids = (String[]) others.get("selectedRow");
        boolean validate = true;
        if (childOids != null) {
            final Instance callInstance = _parameter.getCallInstance();
            for (final String childOid : childOids) {
                final Instance child = Instance.get(childOid);
                if (callInstance.getType().isKindOf(CISales.DocumentSumAbstract.getType())) {
                    if (child.getType().equals(CISales.IncomingInvoice.getType())
                                    && check4Relation(CISales.Document2Document4Swap.uuid, child).next()) {
                        validate = false;
                        html.append(getString4ReturnInvalidate(child));
                        break;
                    } else if (child.getType().equals(CISales.IncomingInvoice.getType())
                                    && check4Relation(CISales.IncomingPerceptionCertificate2IncomingInvoice.uuid,
                                                    child).next()) {
                        validate = false;
                        html.append(getString4ReturnInvalidate(child));
                        break;
                    } else if (child.getType().equals(CISales.PaymentOrder.getType())
                                    && check4Relation(CISales.Document2Document4Swap.uuid, child).next()) {
                        validate = false;
                        html.append(getString4ReturnInvalidate(child));
                        break;
                    } else if (child.getType().equals(CISales.Invoice.getType())
                                    && check4Relation(CISales.Document2Document4Swap.uuid, child).next()) {
                        validate = false;
                        html.append(getString4ReturnInvalidate(child));
                        break;
                    } else if (child.getType().equals(CISales.Invoice.getType())
                                    && check4Relation(CISales.IncomingRetentionCertificate2Invoice.uuid,
                                                    child).next()) {
                        validate = false;
                        html.append(getString4ReturnInvalidate(child));
                        break;
                    } else if (child.getType().equals(CISales.CollectionOrder.getType())
                                    && check4Relation(CISales.Document2Document4Swap.uuid, child).next()) {
                        validate = false;
                        html.append(getString4ReturnInvalidate(child));
                        break;
                    }
                }
            }
            if (validate) {
                ret.put(ReturnValues.TRUE, true);
                html.append(DBProperties.getProperty(this.getClass().getName() + ".validateConnectDoc"));
                ret.put(ReturnValues.SNIPLETT, html.toString());
            } else {
                html.insert(0, DBProperties.getProperty(this.getClass().getName() + ".invalidateConnectDoc")
                                + "<p>");
                ret.put(ReturnValues.SNIPLETT, html.toString());
            }
        }
        return ret;
    }

    /**
     * Check4 relation.
     *
     * @param _typeUUID the type uuid
     * @param _instance the instance
     * @return the multi print query
     * @throws EFapsException on error
     */
    protected MultiPrintQuery check4Relation(final UUID _typeUUID,
                                             final Instance _instance)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(_typeUUID);
        queryBldr.addWhereAttrMatchValue(CISales.Document2DocumentAbstract.ToAbstractLink, _instance.getId());
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.Document2DocumentAbstract.OID);
        multi.execute();
        return multi;
    }

    /**
     * Gets the string4 return invalidate.
     *
     * @param _child the child
     * @return the string4 return invalidate
     * @throws EFapsException on error
     */
    protected StringBuilder getString4ReturnInvalidate(final Instance _child)
        throws EFapsException
    {
        final StringBuilder html = new StringBuilder();
        final PrintQuery print = new PrintQuery(_child);
        print.addAttribute(CISales.DocumentAbstract.Name);
        print.execute();
        return html.append(_child.getType().getLabel()
                        + " - " + print.<String>getAttribute(CISales.DocumentAbstract.Name));
    }

    /**
     * Gets the currency from ui.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the currency from ui
     * @throws EFapsException on error
     */
    protected Instance getCurrencyFromUI(final Parameter _parameter)
        throws EFapsException
    {
        return new Currency().getCurrencyFromUI(_parameter);
    }

    /**
     * Gets the payment analysis.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the payment analysis
     * @throws EFapsException on error
     */
    public Return getPaymentAnalysisFieldValueUI(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        ret.put(ReturnValues.SNIPLETT, DocPaymentInfo_Base.getInfoHtml(_parameter, _parameter.getInstance()));
        return ret;
    }

    /**
     * Gets the payment info field value UI.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the payment info field value UI
     * @throws EFapsException
     */
    public Return getPaymentInfoFieldValueUI(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        ret.put(ReturnValues.VALUES, DocPaymentInfo_Base.getInfoValue(_parameter, _parameter.getInstance()));
        return ret;
    }
}
