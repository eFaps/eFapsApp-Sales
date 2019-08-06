/*
 * Copyright 2003 - 2019 The eFaps Team
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

package org.efaps.esjp.sales.tax;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.UUID;

import org.efaps.admin.datamodel.Attribute;
import org.efaps.admin.datamodel.IJaxb;
import org.efaps.admin.datamodel.ui.UIValue;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.db.Context;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.tax.xml.TaxEntry;
import org.efaps.esjp.sales.tax.xml.Taxes;
import org.efaps.util.EFapsException;

/**
 * @author The eFaps Team
 */
@EFapsUUID("da82900f-88f7-43c9-a331-b894f3d337e3")
@EFapsApplication("eFapsApp-Sales")
public abstract class TaxesAttribute_Base
    implements IJaxb
{

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?>[] getClasses()
    {
        return new Class[] { Taxes.class, TaxEntry.class };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUISnipplet(final TargetMode _mode,
                                final UIValue _value)
        throws EFapsException
    {
        final String ret;
        if (_value.getDbValue() == null) {
            ret = "";
        } else if (_value.getDbValue() instanceof Taxes) {
            final Taxes taxes = (Taxes) _value.getDbValue();
            final StringBuilder html = new StringBuilder();
            boolean first = true;
            for (final TaxEntry entry : taxes.getEntries()) {
                if (first) {
                    first = false;
                } else {
                    html.append("<br/>");
                }
                final Tax tax = Tax_Base.get(entry.getCatUUID(), entry.getUUID());
                html.append(getLabel(_value.getAttribute(), tax.getName(), entry.getAmount(), entry.getCurrencyUUID()));
            }
            ret = html.toString();
        } else {
            ret = _value.getDbValue().toString();
        }
        return ret;
    }

    /**
     * @param _taxes taxes the html is wanted for
     * @return html snipplet
     * @throws EFapsException on error
     */
    public String getUI4ReadOnly(final Taxes _taxes)
        throws EFapsException
    {
        final StringBuilder html = new StringBuilder();
        boolean first = true;
        for (final TaxEntry entry : _taxes.getEntries()) {
            if (first) {
                first = false;
            } else {
                html.append("<br/>");
            }
            final Tax tax = Tax_Base.get(entry.getCatUUID(), entry.getUUID());
            html.append(getLabel(null, tax.getName(), entry.getAmount(), entry.getCurrencyUUID()));
        }
        return html.toString();
    }

    /**
     * @param _attribute    Atribute
     * @param _name         Name of the Tax
     * @param _amount       Amnount
     * @param _currencyUUID UUID of the Currency
     * @return label for UserInterface
     * @throws EFapsException on error
     */
    protected String getLabel(final Attribute _attribute,
                              final String _name,
                              final BigDecimal _amount,
                              final UUID _currencyUUID)
        throws EFapsException
    {
        String ret = "";
        String currName = "";
        String currSymbol = "";
        String currISOCode = "";
        if (_currencyUUID != null) {
            final CurrencyInst currencyInst = CurrencyInst.get(_currencyUUID);
            currName = currencyInst.getName();
            currSymbol = currencyInst.getSymbol();
            currISOCode = currencyInst.getISOCode();
        }
        final DecimalFormat formatter;
        if (_attribute != null) {
            formatter = NumberFormatter.get().getFrmt4Total(_attribute.getParent().getName());
        } else {
            formatter = NumberFormatter.get().getTwoDigitsFormatter();
        }
        final String amount = formatter.format(_amount);
        ret = DBProperties.getFormatedDBProperty(TaxesAttribute.class.getName() + ".Label",
                        Context.getThreadContext().getLanguage(), _name, amount, currName, currSymbol, currISOCode);
        return ret;
    }
}
