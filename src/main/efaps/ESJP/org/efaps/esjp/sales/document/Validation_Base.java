/*
 * Copyright 2003 - 2014 The eFaps Team
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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.AbstractPositionWarning;
import org.efaps.esjp.erp.IWarning;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.erp.WarningUtil;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("6ccd0664-cf7a-478f-b356-d8ec9c140b20")
@EFapsRevision("$Rev$")
public abstract class Validation_Base
    extends AbstractDocument
{

    public enum Validations
    {
        QUANTITYINSTOCK, QUANTITYGREATERZERO;
    }


    @Override
    public Return validate(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Map<Integer, String> validations = analyseProperty(_parameter, "Validation");
        final List<IWarning> warnings = new ArrayList<IWarning>();
        for (final String validation : validations.values()) {
            final Validations val = Validations.valueOf(validation);
            switch (val) {
                case QUANTITYINSTOCK:
                    warnings.addAll(validateQuantityInStorage(_parameter));
                    break;
                case QUANTITYGREATERZERO:
                    warnings.addAll(validateQuantityGreaterZero(_parameter));
                    break;
                default:
                    break;
            }
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
     * Validate that the given quantities have numbers bigger than Zero.
     * @param _parameter Parameter as passed by the eFasp API
     * @return List of warnings, empty list if no warning
     * @throws EFapsException on error
     */
    public List<IWarning> validateQuantityGreaterZero(final Parameter _parameter)
        throws EFapsException
    {
        final List<IWarning> ret = new ArrayList<IWarning>();
        final String[] quantities = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                        CISales.PositionAbstract.Quantity.name));
        for (int i = 0; i < getPositionsCount(_parameter); i++) {
            BigDecimal quantity = BigDecimal.ZERO;
            try {
                quantity = (BigDecimal) NumberFormatter.get().getFormatter().parse(quantities[i]);
            } catch (final ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
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
     * @return List of warnings, empty list if no warning
     * @throws EFapsException on error
     */
    public List<IWarning> validateQuantityInStorage(final Parameter _parameter)
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
            if (prodInst.isValid()) {
                BigDecimal currQuantity = BigDecimal.ZERO;
                final QueryBuilder queryBldr = new QueryBuilder(CIProducts.InventoryAbstract);
                queryBldr.addWhereAttrEqValue(CIProducts.InventoryAbstract.Product, prodInst);
                if (ArrayUtils.isNotEmpty(storage)) {
                    queryBldr.addWhereAttrEqValue(CIProducts.InventoryAbstract.Storage, Instance.get(storage[i]));
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
                        // TODO Auto-generated catch block
                        e.printStackTrace();
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

    public static class NotEnoughStockWarning
        extends AbstractPositionWarning
    {

        public NotEnoughStockWarning()
        {
            setError(true);
        }
    }

    public static class QuantityGreateZeroWarning
        extends AbstractPositionWarning
    {

        public QuantityGreateZeroWarning()
        {
            setError(true);
        }
}

}
