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

package org.efaps.esjp.sales;

import java.math.BigDecimal;
import java.util.List;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.util.EFapsException;

/**
 * This class must be replaced for customization, therefore it is left empty.
 * Functional description can be found in the related "<code>_base</code>"
 * class.
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("a4d2eb01-14c3-4116-9176-2d3bd42c3114")
@EFapsRevision("$Rev$")
public class Calculator
    extends Calculator_Base
{

    /**
     * Needed for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * @param _parameter Parameter parameter as passe dfrom the eFaps API
     * @param _calc calculator
     * @param _oid oid of the product
     * @param _quantity quantity
     * @param _unitPrice unit price
     * @param _discount discount
     * @param _priceFromDB must the price set from DB
     * @param _config config to be used
     * @throws EFapsException on error
     */
    // CHECKSTYLE:OFF
    public Calculator(final Parameter _parameter,
                      final Calculator _calc,
                      final String _oid,
                      final String _quantity,
                      final String _unitPrice,
                      final String _discount,
                      final boolean _priceFromDB,
                      final ICalculatorConfig _config)
        throws EFapsException
    { // CHECKSTYLE:ON
        super(_parameter, _calc, _oid, _quantity, _unitPrice, _discount, _priceFromDB, _config);
    }

    /**
     * @param _parameter Parameter parameter as passed from the eFaps API
     * @param _calc calculator
     * @param _prodInstance Instance of the product
     * @param _quantity quantity
     * @param _unitPrice unit price
     * @param _discount discount
     * @param _priceFromDB must the price set from DB
     * @param _config config to be used
     * @throws EFapsException on error
     */
    // CHECKSTYLE:OFF
    public Calculator(final Parameter _parameter,
                      final Calculator _calc,
                      final Instance _prodInstance,
                      final BigDecimal _quantity,
                      final BigDecimal _unitPrice,
                      final BigDecimal _discount,
                      final boolean _priceFromDB,
                      final ICalculatorConfig _config)
        throws EFapsException
    // CHECKSTYLE:ON
    {
        super(_parameter, _calc, _prodInstance, _quantity, _unitPrice, _discount, _priceFromDB, _config);
    }

    /**
     * @throws EFapsException on error
     */
    public Calculator()
        throws EFapsException
    {
        super();
    }

    /**
     * @param _parameter Parameter parameter as passed from the eFaps API
     * @param _config config to be used
     * @throws EFapsException on error
     */
    public Calculator(final Parameter _parameter,
                      final ICalculatorConfig _config)
        throws EFapsException
    {
        super(_parameter, _config);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _calcList List of calculator
     * @return crossTotal
     * @throws EFapsException on error
     */
    public static BigDecimal getCrossTotal(final Parameter _parameter,
                                           final List<Calculator> _calcList)
        throws EFapsException
    {
        return Calculator_Base.getCrossTotal(_parameter, _calcList);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _calcList List of calculator
     * @return crossTotal
     * @throws EFapsException on error
     */
    public static BigDecimal getNetTotal(final Parameter _parameter,
                                         final List<Calculator> _calcList)
        throws EFapsException
    {
        return Calculator_Base.getNetTotal(_parameter, _calcList);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _calcList List of calculator
     * @return crossTotal
     * @throws EFapsException on error
     */
    public static BigDecimal getBaseCrossTotal(final Parameter _parameter,
                                               final List<Calculator> _calcList)
        throws EFapsException
    {
        return Calculator_Base.getBaseCrossTotal(_parameter, _calcList);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _calcList List of calculator
     * @return crossTotal
     * @throws EFapsException on error
     */
    public static BigDecimal getPerceptionTotal(final Parameter _parameter,
                                                final List<Calculator> _calcList)
        throws EFapsException
    {
        return Calculator_Base.getPerceptionTotal(_parameter, _calcList);
    }
}
