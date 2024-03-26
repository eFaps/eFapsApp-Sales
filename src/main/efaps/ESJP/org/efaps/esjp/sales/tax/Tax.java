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
package org.efaps.esjp.sales.tax;

import java.math.BigDecimal;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;

/**
 * This class must be replaced for customization, therefore it is left empty.
 * Functional description can be found in the related "<code>_base</code>"
 * class.
 *
 * @author The eFasp Team
 */
@EFapsUUID("b74fc4d7-aee7-4300-bd1a-7bf59486deca")
@EFapsApplication("eFapsApp-Sales")
public class Tax
    extends Tax_Base
{

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new tax.
     *
     * @param _taxcat the taxcat
     * @param _instance the instance
     * @param _nameTmp the name tmp
     * @param _uuidTmp the uuid tmp
     * @param _numerator the numerator
     * @param _denominator the denominator
     */
    public Tax(final TaxCat_Base _taxcat,
               final Instance _instance,
               final String _name,
               final String _uuid,
               final TaxType _taxType,
               final Integer _numerator,
               final Integer _denominator,
               final BigDecimal _amount,
               final Long _currencyId)
    {
        super(_taxcat, _instance, _name, _uuid, _taxType, _numerator, _denominator, _amount, _currencyId);
    }

    /**
     * Instantiates a new tax.
     */
    public Tax()
    {
        super();
    }
}
