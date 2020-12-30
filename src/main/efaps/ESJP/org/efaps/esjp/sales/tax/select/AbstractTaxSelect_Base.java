/*
 * Copyright 2003 - 2020 The eFaps Team
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

package org.efaps.esjp.sales.tax.select;

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIAttribute;
import org.efaps.db.CachedMultiPrintQuery;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.esjp.common.eql.AbstractSelect;
import org.efaps.esjp.sales.tax.Tax;
import org.efaps.esjp.sales.tax.Tax_Base;
import org.efaps.esjp.sales.tax.xml.TaxEntry;
import org.efaps.esjp.sales.tax.xml.Taxes;
import org.efaps.util.EFapsException;
import org.efaps.util.UUIDUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("8dc47c22-4eb0-4a92-b08e-3615ae14a4c6")
@EFapsApplication("eFapsApp-Sales")
public abstract class AbstractTaxSelect_Base
    extends AbstractSelect
{
    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AbstractTaxSelect.class);

    @Override
    public void initialize(final List<Instance> _instances,
                           final String... _eqlParameters)
        throws EFapsException
    {
        String taxHint = null;
        if (ArrayUtils.isNotEmpty(_eqlParameters)) {
            taxHint = _eqlParameters[0];
        }

        final MultiPrintQuery multi = CachedMultiPrintQuery.get4Request(_instances);
        multi.addAttribute(getAttribute());
        multi.execute();
        while (multi.next()) {
           final Taxes taxes =  multi.getAttribute(getAttribute());
           if (taxes != null) {
               if (taxes.getEntries().size() == 1 && taxHint == null) {
                   final var value = taxes.getEntries().get(0).getAmount();
                   getValues().put(multi.getCurrentInstance(), value);
               } else {
                   if (taxHint == null) {
                       LOG.error("cannot evaluate TaxSelect for {} due to having more than one entry but no TaxHint",
                                       multi.getCurrentInstance());
                   } else {
                       BigDecimal value = null;
                       for (final TaxEntry taxEntry: taxes.getEntries()) {
                           if (UUIDUtil.isUUID(taxHint) && taxHint.equals(taxEntry.getUUID().toString())) {
                               value = taxEntry.getAmount();
                               getValues().put(multi.getCurrentInstance(), value);
                               break;
                           } else {
                              final Tax tax = Tax_Base.get(taxEntry.getCatUUID(), taxEntry.getUUID());
                              if (taxHint.equals(tax.getName())) {
                                  value = taxEntry.getAmount();
                                  getValues().put(multi.getCurrentInstance(), value);
                                  break;
                              }
                           }
                       }
                   }
               }
           }
        }
    }

    protected abstract CIAttribute getAttribute();

    protected abstract BigDecimal getValue(TaxEntry _taxEntry);
}
