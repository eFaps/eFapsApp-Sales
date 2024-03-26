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
package org.efaps.esjp.sales.graphql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.esjp.sales.tax.Tax_Base;
import org.efaps.esjp.sales.tax.xml.Taxes;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

@EFapsUUID("94321076-c369-42f6-9caf-d0936806283d")
@EFapsApplication("eFapsApp-Sales")
public abstract class TaxDataFetcher_Base
    implements DataFetcher<Object>
{

    @Override
    public Object get(final DataFetchingEnvironment _environment)
        throws Exception
    {
        final Map<String, Object> source = _environment.getSource();
        final var taxes = (Taxes) source.get(_environment.getField().getName());
        final List<Map<String, Object>> data = new ArrayList<>();
        for (final var taxEntry : taxes.getEntries()) {
            final var tax = Tax_Base.get(taxEntry.getCatUUID(), taxEntry.getUUID());
            final Map<String, Object> map = new HashMap<>();
            map.put("amount", taxEntry.getAmount());
            map.put("base", taxEntry.getBase());
            map.put("name", tax.getName());
            map.put("factor", tax.getFactor());
            data.add(map);
        }
        return DataFetcherResult.newResult()
                        .data(data)
                        .build();
    }
}
