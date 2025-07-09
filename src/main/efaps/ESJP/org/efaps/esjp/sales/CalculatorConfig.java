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
package org.efaps.esjp.sales;

import java.util.UUID;

import org.apache.commons.lang3.EnumUtils;
import org.efaps.abacus.api.CrossTotalFlow;
import org.efaps.abacus.api.IConfig;
import org.efaps.abacus.api.TaxCalcFlow;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.efaps.util.UUIDUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("67ddb8ca-ebbf-4e96-b0f6-45797801a0f6")
@EFapsApplication("eFapsApp-Sales")
public class CalculatorConfig
    implements IConfig
{

    private static final Logger LOG = LoggerFactory.getLogger(CalculatorConfig.class);

    private final String typeKey;

    public CalculatorConfig(final String typeKey)
    {
        this.typeKey = typeKey;
    }

    @Override
    public int getCrossPriceScale()
    {
        return getInt("CrossPriceScale", 4);
    }

    @Override
    public CrossTotalFlow getCrossTotalFlow()
    {
        return getEnum("CrossTotalFlow", CrossTotalFlow.class, CrossTotalFlow.SumCrossPrice);
    }

    @Override
    public int getNetPriceScale()
    {
        return getInt("NetPriceScale", 4);
    }

    @Override
    public TaxCalcFlow getTaxCalcFlow()
    {
        return getEnum("TaxCalcFlow", TaxCalcFlow.class, TaxCalcFlow.RoundSum);
    }

    @Override
    public int getTaxScale()
    {
        return getInt("TaxScale", 2);
    }

    public String getPriceEvaluation()
    {
        return "PriceList";
    }

    public String getTypeKey()
    {
        return typeKey;
    }

    public UUID getPriceList()
        throws EFapsException
    {
        final String value = getString("PriceList", CIProducts.ProductPricelistRetail.uuid.toString());
        final UUID ret;
        if (UUIDUtil.isUUID(value)) {
            ret = UUID.fromString(value);
        } else {
            ret = Type.get(value).getUUID();
        }
        return ret;
    }

    private String getString(final String key,
                             final String defaultValue)
    {
        try {
            final var properties = Sales.CALCULATOR_CONFIG.get();
            return properties.getProperty(key, String.valueOf(defaultValue));

        } catch (final EFapsException e) {
            LOG.error("Catched", e);
        }
        return defaultValue;
    }

    private Integer getInt(final String key,
                           final int defaultValue)
    {
        try {
            final var properties = Sales.CALCULATOR_CONFIG.get();
            final var strVal = properties.getProperty(key, String.valueOf(defaultValue));
            return Integer.valueOf(strVal);
        } catch (final EFapsException e) {
            LOG.error("Catched", e);
        }
        return defaultValue;
    }

    private <E extends Enum<E>> E getEnum(final String key,
                                          final Class<E> enumClass,
                                          final E defaultValue)
    {
        E value = null;
        try {
            final var properties = Sales.CALCULATOR_CONFIG.get();
            value = EnumUtils.getEnum(enumClass, properties.getProperty(key));
        } catch (final EFapsException e) {
            LOG.error("Catched", e);
        }
        return value == null ? defaultValue : value;
    }

}
