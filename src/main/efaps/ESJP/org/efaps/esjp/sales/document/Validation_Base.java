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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.Command;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.AbstractPositionWarning;
import org.efaps.esjp.erp.AbstractWarning;
import org.efaps.esjp.erp.IWarning;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.erp.WarningUtil;
import org.efaps.esjp.products.util.Products;
import org.efaps.esjp.sales.Calculator;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("6ccd0664-cf7a-478f-b356-d8ec9c140b20")
@EFapsApplication("eFapsApp-Sales")
public abstract class Validation_Base
    extends AbstractDocument
{
    /**
     * Enum for enabling standard validations.
     */
    public enum Validations
    {
        /** Basic validation for Positions.*/
        POSITION,
        /** Basic validation for Positions.*/
        DUPLICATEDPOSITION,
        /** Validate that only one product is selected.*/
        ONLYONEPRODUCT,
        /** Validate Quantity in Stock. */
        QUANTITYINSTOCK,
        /** Validate Quantity in Stock for a Document Instance in Background. */
        QUANTITYINSTOCK4DOC,
        /** Validate that Quantity is greater than zero. */
        QUANTITYGREATERZERO,
        /** Validate that Amount is greater than zero. */
        AMOUNTGREATERZERO,
        /** Validate that Amount is greater than zero. */
        TOTALGREATERZERO,
        /** Validate the Name. */
        NAME,
        /** Validate the Serial Name. */
        SERIAL,
        /** Present always a warning "Are you sure". */
        AREYOUSURE,
        /** Validate the selected products for Individual.*/
        INDIVIDUAL;
    }

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Validation.class);

    /**
     * @param _parameter parameter as passed by the eFasp API
     * @param _doc the document calling the evaluation
     * @return Return with the result
     * @throws EFapsException on error
     */
    public Return validate(final Parameter _parameter,
                           final AbstractDocument_Base _doc)
        throws EFapsException
    {
        final Return ret = new Return();
        final Map<Integer, String> validations = analyseProperty(_parameter, "Validation");
        List<IWarning> warnings = new ArrayList<IWarning>();
        boolean areyousure = false;
        for (final String validation : validations.values()) {
            final Validations val = Validations.valueOf(validation);
            switch (val) {
                case POSITION:
                    warnings.addAll(validatePositions(_parameter, _doc));
                    break;
                case DUPLICATEDPOSITION:
                    warnings.addAll(validateDuplicatedPositions(_parameter, _doc));
                    break;
                case ONLYONEPRODUCT:
                    warnings.addAll(validateOnlyOneProduct(_parameter, _doc));
                    break;
                case QUANTITYINSTOCK:
                    warnings.addAll(validateQuantityInStorage(_parameter, _doc));
                    break;
                case QUANTITYINSTOCK4DOC:
                    warnings.addAll(validateQuantityInStorage4Doc(_parameter, _doc));
                    break;
                case QUANTITYGREATERZERO:
                    warnings.addAll(validateQuantityGreaterZero(_parameter, _doc));
                    break;
                case NAME:
                    warnings.addAll(validateName(_parameter, _doc));
                    break;
                case SERIAL:
                    warnings.addAll(validateSerial(_parameter, _doc));
                    break;
                case AMOUNTGREATERZERO:
                    warnings.addAll(validateAmountGreaterZero(_parameter, _doc));
                    break;
                case TOTALGREATERZERO:
                    warnings.addAll(validateTotalGreaterZero(_parameter, _doc));
                    break;
                case INDIVIDUAL:
                    warnings.addAll(validateIndividual(_parameter, _doc));
                case AREYOUSURE:
                    areyousure = true;
                    break;
                default:
                    break;
            }
        }

        warnings = validate(_parameter, warnings);
        if (warnings.isEmpty() && areyousure) {
            warnings.add(new AreYouSureWarning());
        }

        if (warnings.isEmpty()) {
            ret.put(ReturnValues.TRUE, true);
        } else {
            ret.put(ReturnValues.SNIPLETT, WarningUtil.getHtml4Warning(warnings).toString());
            if (!WarningUtil.hasError(warnings)) {
                ret.put(ReturnValues.TRUE, true);
            }
        }
        return ret;
    }

    /**
     * Validate that the positions are valid.
     * @param _parameter Parameter as passed by the eFasp API
     * @param _doc the document calling the evaluation
     * @return List of warnings, empty list if no warning
     * @throws EFapsException on error
     */
    public List<IWarning> validatePositions(final Parameter _parameter,
                                            final AbstractDocument_Base _doc)
        throws EFapsException
    {
        final List<IWarning> ret = new ArrayList<IWarning>();
        final String[] product = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                        CISales.PositionAbstract.Product.name));
        for (int i = 0; i < getPositionsCount(_parameter); i++) {
            final Instance prodInst = Instance.get(product[i]);
            if (!prodInst.isValid()) {
                ret.add(new PositionWarning().setPosition(i + 1));
            }
        }
        return ret;
    }

    /**
     * Validate that the positions are valid.
     * @param _parameter Parameter as passed by the eFasp API
     * @param _doc the document calling the evaluation
     * @return List of warnings, empty list if no warning
     * @throws EFapsException on error
     */
    public List<IWarning> validateDuplicatedPositions(final Parameter _parameter,
                                                      final AbstractDocument_Base _doc)
        throws EFapsException
    {
        final List<IWarning> ret = new ArrayList<IWarning>();
        final String[] product = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                        CISales.PositionAbstract.Product.name));
        final Set<Instance> prods = new HashSet<>();
        for (int i = 0; i < getPositionsCount(_parameter); i++) {
            final Instance prodInst = Instance.get(product[i]);
            if (prods.contains(prodInst)) {
                ret.add(new DuplicatedPositionWarning().setPosition(i + 1));
            } else {
                prods.add(prodInst);
            }
        }
        return ret;
    }

    /**
     * Validate that the positions are valid.
     * @param _parameter Parameter as passed by the eFasp API
     * @param _doc the document calling the evaluation
     * @return List of warnings, empty list if no warning
     * @throws EFapsException on error
     */
    public List<IWarning> validateOnlyOneProduct(final Parameter _parameter,
                                                 final AbstractDocument_Base _doc)
        throws EFapsException
    {
        final List<IWarning> ret = new ArrayList<IWarning>();
        final String[] product = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                        CISales.PositionAbstract.Product.name));
        Instance currentProdInst = null;
        for (int i = 0; i < getPositionsCount(_parameter); i++) {
            final Instance prodInst = Instance.get(product[i]);
            if (currentProdInst == null) {
                currentProdInst = prodInst;
            } else if (!currentProdInst.equals(prodInst)) {
                ret.add(new OnlyOneProductWarning());
            }
        }
        return ret;
    }

    /**
     * Validate that the given quantities have numbers bigger than Zero.
     * @param _parameter Parameter as passed by the eFasp API
     * @param _doc the document calling the evaluation
     * @return List of warnings, empty list if no warning
     * @throws EFapsException on error
     */
    public List<IWarning> validateAmountGreaterZero(final Parameter _parameter,
                                                    final AbstractDocument_Base _doc)
        throws EFapsException
    {
        final List<IWarning> ret = new ArrayList<IWarning>();
        final List<Calculator> calcs = _doc.analyseTable(_parameter, null);
        int i = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (final Calculator calc : calcs) {
            if (!calc.isEmpty()) {
                total = total.add(calc.getCrossPrice());
                if (calc.getCrossPrice().compareTo(BigDecimal.ZERO) < 1) {
                    ret.add(new AmountGreaterZeroWarning().setPosition(i + 1));
                }
            }
            i++;
        }
        if (total.compareTo(BigDecimal.ZERO) < 1) {
            ret.add(new TotalGreaterZeroWarning());
        }
        return ret;
    }

    /**
     * Validate that the given quantities have numbers bigger than Zero.
     * @param _parameter Parameter as passed by the eFasp API
     * @param _doc the document calling the evaluation
     * @return List of warnings, empty list if no warning
     * @throws EFapsException on error
     */
    public List<IWarning> validateTotalGreaterZero(final Parameter _parameter,
                                                   final AbstractDocument_Base _doc)
        throws EFapsException
    {
        final List<IWarning> ret = new ArrayList<IWarning>();
        final List<Calculator> calcs = _doc.analyseTable(_parameter, null);
        final BigDecimal total = Calculator.getNetTotal(_parameter, calcs);
        if (total.compareTo(BigDecimal.ZERO) < 1) {
            ret.add(new TotalGreaterZeroWarning());
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _warnings warings to add/alternate etc.
     * @return List of warnings
     * @throws EFapsException on error
     */
    protected List<IWarning> validate(final Parameter _parameter,
                                      final List<IWarning> _warnings)
        throws EFapsException
    {
        // to be used by implementations
        return _warnings;
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @param _doc the document calling the evaluation
     * @return List of warnings, if none an empty list
     * @throws EFapsException on error
     */
    public List<IWarning> validateName(final Parameter _parameter,
                                       final AbstractDocument_Base _doc)
        throws EFapsException
    {
        final List<IWarning> ret = new ArrayList<IWarning>();

        final Instance docInst = _parameter.getInstance();

        final String fieldName = getProperty(_parameter, "NAME_FieldName");
        String name = null;
        if (fieldName == null) {
            name = _parameter.getParameterValue("name4create");
            if (name == null) {
                name = _parameter.getParameterValue("name");
            }
        } else {
            name = _parameter.getParameterValue(fieldName);
        }

        if (name != null) {
            final String[] numbers = name.split("-");
            if (numbers.length == 2 && numbers[0].length() >= numbers[1].length()) {
                ret.add(new InvalidNameWarning());
            } else if (numbers.length != 2) {
                ret.add(new InvalidNameWarning());
            }

            if ("true".equalsIgnoreCase(getProperty(_parameter, "NAME_ValidateContact"))) {
                final Instance contactInst = getContactInstance(_parameter);
                if (contactInst != null && contactInst.isValid()) {
                    QueryBuilder queryBldr = null;
                    final Map<Integer, String> types = analyseProperty(_parameter, "NAME_QueryType");
                    for (final Entry<Integer, String> entryType : types.entrySet()) {
                        final Type type = Type.get(entryType.getValue());
                        if (type != null) {
                            if (queryBldr == null) {
                                queryBldr = new QueryBuilder(type);
                            } else {
                                queryBldr.addType(type);
                            }
                        }
                    }
                    if (queryBldr != null) {
                        queryBldr.addWhereAttrEqValue(CIERP.DocumentAbstract.Contact, contactInst);
                        queryBldr.addWhereAttrEqValue(CIERP.DocumentAbstract.Name, name).setIgnoreCase(true);
                        if (docInst != null && docInst.isValid()) {
                            queryBldr.addWhereAttrNotEqValue(CIERP.DocumentAbstract.ID, docInst);
                        }
                        final MultiPrintQuery multi = queryBldr.getPrint();
                        multi.addAttribute(CIERP.DocumentAbstract.Name, CIERP.DocumentAbstract.Revision,
                                        CIERP.DocumentAbstract.Date);
                        multi.execute();

                        while (multi.next()) {
                            final String label = multi.getCurrentInstance().getType().getLabel();
                            final String nameStr = multi.<String>getAttribute(CIERP.DocumentAbstract.Name);
                            final String dateStr = multi.<DateTime>getAttribute(CIERP.DocumentAbstract.Date)
                                            .toString("dd/MM/YYYY");
                            final String revStr = multi.<String>getAttribute(CIERP.DocumentAbstract.Revision);
                            ret.add(new ExistingNameWarning().addObject(label, nameStr, dateStr,
                                            revStr == null ? "" : revStr));
                        }
                    }
                }
            }
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @param _doc the document calling the evaluation
     * @return List of warnings, if none an empty list
     * @throws EFapsException on error
     */
    public List<IWarning> validateSerial(final Parameter _parameter,
                                         final AbstractDocument_Base _doc)
        throws EFapsException
    {
        final List<IWarning> ret = new ArrayList<IWarning>();
        final Instance docInst = _parameter.getInstance();
        Type type = null;
        if (docInst != null && docInst.isValid()) {
            type = docInst.getType();
        } else {
            final Object cmd = _parameter.get(ParameterValues.UIOBJECT);
            if (cmd != null && cmd instanceof Command) {
                type = ((Command) cmd).getTargetCreateType();
            }
        }

        final String fieldName = getProperty(_parameter, "NAME_FieldName");
        String name = null;
        String snName = null;
        if (fieldName == null) {
            name = _parameter.getParameterValue("name4create");
            snName = _parameter.getParameterValue("name4create_SN");
            if (name == null) {
                name = _parameter.getParameterValue("name");
                snName = _parameter.getParameterValue("name_SN");
            }
        } else {
            name = _parameter.getParameterValue(fieldName);
            snName = _parameter.getParameterValue(fieldName + "_SN");
        }

        if (type != null && name != null) {
            name = snName + "-" + name;
            final QueryBuilder queryBldr = new QueryBuilder(type);
            queryBldr.addWhereAttrEqValue(CIERP.DocumentAbstract.Name, name).setIgnoreCase(true);
            if (!queryBldr.getQuery().execute().isEmpty()) {
                ret.add(new ExistingSerialWarning());
            }
        }
        return ret;
    }


    /**
     * @param _parameter Paramter as passed by the eFaps API
     * @return instance of the contact
     * @throws EFapsException on error
     */
    protected Instance getContactInstance(final Parameter _parameter)
        throws EFapsException
    {
        final Instance ret;

        final String contact = getProperty(_parameter, "ContactFieldName") != null
                        ? _parameter.getParameterValue(getProperty(_parameter, "ContactFieldName"))
                        : _parameter.getParameterValue("contact");

        final Instance contactInst = Instance.get(contact);
        final Instance instance = _parameter.getInstance();

        if (instance != null && !contactInst.isValid()) {
            final SelectBuilder selContactInst = new SelectBuilder()
                            .linkto(CISales.DocumentAbstract.Contact).instance();

            final PrintQuery print = new PrintQuery(instance);
            print.addSelect(selContactInst);
            print.execute();
            ret = print.<Instance>getSelect(selContactInst);
        } else {
            ret = contactInst;
        }
        return ret;
    }


    /**
     * Validate that the given quantities have numbers bigger than Zero.
     * @param _parameter Parameter as passed by the eFasp API
     * @param _doc the document calling the evaluation
     * @return List of warnings, empty list if no warning
     * @throws EFapsException on error
     */
    public List<IWarning> validateQuantityGreaterZero(final Parameter _parameter,
                                                      final AbstractDocument_Base _doc)
        throws EFapsException
    {
        final List<IWarning> ret = new ArrayList<IWarning>();
        final String[] quantities = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                        CISales.PositionAbstract.Quantity.name));
        for (int i = 0; i < getPositionsCount(_parameter); i++) {
            BigDecimal quantity = BigDecimal.ZERO;
            try {
                if (StringUtils.isNotEmpty(quantities[i])) {
                    quantity = (BigDecimal) NumberFormatter.get().getFormatter().parse(quantities[i]);
                }
            } catch (final ParseException e) {
                Validation_Base.LOG.debug("Catched ParseException on validation", e);
            }
            if (quantity.compareTo(BigDecimal.ZERO) < 1) {
                ret.add(new QuantityGreateZeroWarning().setPosition(i + 1));
            }
        }
        return ret;
    }

    /**
     * Validate that the given quantities exist in the stock.
     * @param _parameter Parameter as passed by the eFasp API
     * @param _doc the document calling the evaluation
     * @return List of warnings, empty list if no warning
     * @throws EFapsException on error
     */
    public List<IWarning> validateQuantityInStorage4Doc(final Parameter _parameter,
                                                        final AbstractDocument_Base _doc)
        throws EFapsException
    {
        final List<IWarning> ret = new ArrayList<IWarning>();
        final QueryBuilder posQueryBldr = new QueryBuilder(CISales.PositionProdDocAbstract);
        posQueryBldr.addWhereAttrEqValue(CISales.PositionProdDocAbstract.DocumentAbstractLink, _parameter
                        .getInstance());
        final MultiPrintQuery posMulti = posQueryBldr.getPrint();
        final SelectBuilder selProdInst = SelectBuilder.get().linkto(CISales.PositionProdDocAbstract.Product)
                        .instance();
        final SelectBuilder selStorageInst = SelectBuilder.get().linkto(CISales.PositionProdDocAbstract.StorageLink)
                        .instance();
        posMulti.addSelect(selProdInst, selStorageInst);
        posMulti.addAttribute(CISales.PositionProdDocAbstract.Quantity, CISales.PositionProdDocAbstract.UoM,
                        CISales.PositionProdDocAbstract.PositionNumber);
        posMulti.execute();
        while (posMulti.next()) {
            final Instance prodInst = posMulti.getSelect(selProdInst);
            if (prodInst.isValid() && !prodInst.getType().isCIType(CIProducts.ProductInfinite)) {
                BigDecimal currQuantity = BigDecimal.ZERO;
                final Instance storageInst = posMulti.getSelect(selStorageInst);
                final QueryBuilder queryBldr = new QueryBuilder(CIProducts.InventoryAbstract);
                queryBldr.addWhereAttrEqValue(CIProducts.InventoryAbstract.Product, prodInst);
                queryBldr.addWhereAttrEqValue(CIProducts.InventoryAbstract.Storage, storageInst);
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CIProducts.InventoryAbstract.Quantity, CIProducts.InventoryAbstract.Reserved);
                multi.execute();
                while (multi.next()) {
                    currQuantity = currQuantity.add(multi.<BigDecimal>getAttribute(
                                    CIProducts.InventoryAbstract.Quantity));
                    currQuantity = currQuantity.add(multi.<BigDecimal>getAttribute(
                                    CIProducts.InventoryAbstract.Reserved));
                }
                BigDecimal quantity = posMulti.getAttribute(CISales.PositionProdDocAbstract.Quantity);
                final UoM uoM = Dimension.getUoM(posMulti.<Long>getAttribute(CISales.PositionProdDocAbstract.UoM));
                quantity = quantity.multiply(new BigDecimal(uoM.getNumerator())).divide(new BigDecimal(uoM
                                .getDenominator()), BigDecimal.ROUND_HALF_UP);
                if (quantity.compareTo(currQuantity) > 0) {
                    ret.add(new NotEnoughStockWarning().setPosition(posMulti.<Integer>getAttribute(
                                    CISales.PositionProdDocAbstract.PositionNumber)));
                }
            }
        }
        return ret;
    }

    /**
     * Validate that the given quantities exist in the stock.
     * @param _parameter Parameter as passed by the eFasp API
     * @param _doc the document calling the evaluation
     * @return List of warnings, empty list if no warning
     * @throws EFapsException on error
     */
    public List<IWarning> validateQuantityInStorage(final Parameter _parameter,
                                                    final AbstractDocument_Base _doc)
        throws EFapsException
    {
        final List<IWarning> ret = new ArrayList<IWarning>();
        final String[] product = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                        CISales.PositionAbstract.Product.name));
        final String[] uoMs = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                        CISales.PositionAbstract.UoM.name));
        final String[] quantities = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                        CISales.PositionAbstract.Quantity.name));
        final String[] storage = _parameter.getParameterValues("storage");

        for (int i = 0; i < getPositionsCount(_parameter); i++) {

            final Instance prodInst = Instance.get(product[i]);
            if (prodInst.isValid() && !prodInst.getType().isCIType(CIProducts.ProductInfinite)) {
                BigDecimal currQuantity = BigDecimal.ZERO;
                final QueryBuilder queryBldr = new QueryBuilder(CIProducts.InventoryAbstract);
                queryBldr.addWhereAttrEqValue(CIProducts.InventoryAbstract.Product, prodInst);
                if (ArrayUtils.isNotEmpty(storage)) {
                    queryBldr.addWhereAttrEqValue(CIProducts.InventoryAbstract.Storage, Instance.get(storage[i]));
                } else if ("true".equalsIgnoreCase(getProperty(_parameter, "QUANTITYINSTOCK_UseDefaultWareHouse"))) {
                    final Instance wareHInst = _doc.getDefaultStorage(_parameter);
                    if (wareHInst != null && wareHInst.isValid()) {
                        queryBldr.addWhereAttrEqValue(CIProducts.InventoryAbstract.Storage, wareHInst);
                    }
                }
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CIProducts.InventoryAbstract.Quantity,
                                CIProducts.InventoryAbstract.Reserved);
                multi.execute();
                while (multi.next()) {
                    currQuantity = currQuantity.add(multi
                                    .<BigDecimal>getAttribute(CIProducts.InventoryAbstract.Quantity));
                    currQuantity = currQuantity.add(multi
                                    .<BigDecimal>getAttribute(CIProducts.InventoryAbstract.Reserved));
                }
                if (StringUtils.isNotEmpty(quantities[i])) {
                    BigDecimal quantity = BigDecimal.ZERO;
                    try {
                        quantity = (BigDecimal) NumberFormatter.get().getFormatter().parse(quantities[i]);
                    } catch (final ParseException e) {
                        Validation_Base.LOG.debug("Catched ParseException on validation", e);
                    }
                    final UoM uoM = Dimension.getUoM(Long.valueOf(uoMs[i]));
                    quantity = quantity.multiply(new BigDecimal(uoM.getNumerator())).divide(
                                    new BigDecimal(uoM.getDenominator()), BigDecimal.ROUND_HALF_UP);
                    if (quantity.compareTo(currQuantity) > 0) {
                        ret.add(new NotEnoughStockWarning().setPosition(i + 1));
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Validate individual.
     *
     * @param _parameter the _parameter
     * @param _doc the _doc
     * @return the list
     * @throws EFapsException the e faps exception
     */
    public List<IWarning> validateIndividual(final Parameter _parameter,
                                             final AbstractDocument_Base _doc)
        throws EFapsException
    {
        final List<IWarning> ret = new ArrayList<IWarning>();
        if (Products.ACTIVATEINDIVIDUAL.get()) {
            final String[] product = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                            CISales.PositionAbstract.Product.name));
            final List<Instance> instances = new ArrayList<>();
            for (int i = 0; i < getPositionsCount(_parameter); i++) {
                final Instance prodInst = Instance.get(product[i]);
                instances.add(prodInst);
            }
            final MultiPrintQuery multi = new MultiPrintQuery(instances);
            multi.addAttribute(CIProducts.ProductAbstract.Individual);
            multi.setEnforceSorted(true);
            multi.executeWithoutAccessCheck();
            int idx = 1;
            while (multi.next()) {
                final Instance prodInst = multi.getCurrentInstance();
                // if it is not an individual product, verify that it is not marked a individual
                if (!prodInst.getType().isKindOf(CIProducts.ProductIndividualAbstract)) {
                    Products.ProductIndividual individual = multi.getAttribute(CIProducts.ProductAbstract.Individual);
                    if (individual == null) {
                        individual  = Products.ProductIndividual.NONE;
                    }
                    switch (individual) {
                        case BATCH:
                        case INDIVIDUAL:
                            ret.add(new ProductMustBeIndividualWarning().setPosition(idx));
                            break;
                        case NONE:
                        default:
                            break;
                    }
                }
                idx++;
            }
        }
        return ret;
    }

    /**
     * Warning for not enough Stock.
     */
    public static class PositionWarning
        extends AbstractPositionWarning
    {
        /**
         * Constructor.
         */
        public PositionWarning()
        {
            setError(true);
        }
    }

    /**
     * Warning for duplicated position.
     */
    public static class DuplicatedPositionWarning
        extends AbstractPositionWarning
    {
    }

    /**
     * Warning for not enough Stock.
     */
    public static class NotEnoughStockWarning
        extends AbstractPositionWarning
    {
        /**
         * Constructor.
         */
        public NotEnoughStockWarning()
        {
            setError(true);
        }
    }

    /**
     * Warning for quantity greater zero.
     */
    public static class QuantityGreateZeroWarning
        extends AbstractPositionWarning
    {
        /**
         * Constructor.
         */
        public QuantityGreateZeroWarning()
        {
            setError(true);
        }
    }

    /**
     * Warning for amount greater zero.
     */
    public static class AmountGreaterZeroWarning
        extends AbstractPositionWarning
    {
        /**
         * Constructor.
         */
        public AmountGreaterZeroWarning()
        {
            setError(true);
        }
    }

    /**
     * Warning for amount greater zero.
     */
    public static class TotalGreaterZeroWarning
        extends AbstractWarning
    {
        /**
         * Constructor.
         */
        public TotalGreaterZeroWarning()
        {
            setError(true);
        }
    }

    /**
     * Warning for invalid name.
     */
    public static class InvalidNameWarning
        extends AbstractWarning
    {
    }

    /**
     * Warning for existing name.
     */
    public static class ExistingNameWarning
        extends AbstractWarning
    {
    }

    /**
     * Warning for existing name.
     */
    public static class ExistingSerialWarning
        extends AbstractWarning
    {
    }


    /**
     * Warning for existing name.
     */
    public static class AreYouSureWarning
        extends AbstractWarning
    {
    }

    /**
     * Warning for amount greater zero.
     */
    public static class OnlyOneProductWarning
        extends AbstractWarning
    {
        /**
         * Constructor.
         */
        public OnlyOneProductWarning()
        {
            setError(true);
        }
    }

    /**
     * Warning that a product must be individual.
     */
    public static class ProductMustBeIndividualWarning
        extends AbstractPositionWarning
    {
        /**
         * Constructor.
         */
        public ProductMustBeIndividualWarning()
        {
            setError(true);
        }
    }

}
