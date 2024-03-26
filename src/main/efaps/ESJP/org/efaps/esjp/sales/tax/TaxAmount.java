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

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;

@EFapsUUID("4be300dd-bea9-44f8-8c0c-4f787dd65dd6")
@EFapsApplication("eFapsApp-Sales")
public class TaxAmount
{

    private Tax tax;
    private BigDecimal amount;
    private BigDecimal base;

    public Tax getTax()
    {
        return this.tax;
    }

    public TaxAmount setTax(final Tax _tax)
    {
        this.tax = _tax;
        return this;
    }

    public BigDecimal getAmount()
    {
        return this.amount;
    }

    public TaxAmount setAmount(final BigDecimal _amount)
    {
        this.amount = _amount;
        return this;
    }

    public BigDecimal getBase()
    {
        return this.base;
    }

    public TaxAmount setBase(final BigDecimal _base)
    {
        this.base = _base;
        return this;
    }

    /**
     * Adds to the amount. Initiates amount white 0 if necessary;
     *
     * @param _amount the amount
     * @return the tax amount
     */
    public TaxAmount addAmount(final BigDecimal _amount)
    {
        if (this.amount == null) {
            this.amount = BigDecimal.ZERO;
        }
        this.amount = this.amount.add(_amount);
        return this;
    }

    /**
     * Adds to the base. Initiates base white 0 if necessary;
     *
     * @param _base the base
     * @return the tax amount
     */
    public TaxAmount addBase(final BigDecimal _base)
    {
        if (this.base == null) {
            this.base = BigDecimal.ZERO;
        }
        this.base = this.base.add(_base);
        return this;
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    }
}
