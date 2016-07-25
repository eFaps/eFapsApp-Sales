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

package org.efaps.esjp.sales.comparative;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 *
 */
@EFapsUUID("666340b6-34d3-45f6-b1f4-8f3079d54240")
@EFapsApplication("eFapsApp-Sales")
public abstract class ComparativeProvider_Base
    extends AbstractComparativeContact
{

    /**
     * @param _parameter Paramter as passed by the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        createDoc(_parameter);
        return new Return();
    }

    @Override
    protected void add2DetailCreate(final Parameter _parameter,
                                    final Insert _insert)
        throws EFapsException
    {
        super.add2DetailCreate(_parameter, _insert);
        final Instance contactInst = Instance.get(_parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.ComparativeDetailProvider.ProviderLink.name)));
        if (contactInst.isValid()) {
            _insert.add(CISales.ComparativeDetailProvider.ProviderLink, contactInst);
        }
    }

    /**
     * @param _parameter
     * @param _attribute
     */
    @Override
    public String getValue4Link(final Parameter _parameter,
                                final Long _linkValue)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(Instance.get(CIContacts.Contact.getType(), _linkValue));
        print.addAttribute(CIContacts.Contact.Name);
        print.execute();
        return print.getAttribute(CIContacts.Contact.Name);
    }
}
