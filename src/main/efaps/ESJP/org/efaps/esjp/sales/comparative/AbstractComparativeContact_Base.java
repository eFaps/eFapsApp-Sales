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
package org.efaps.esjp.sales.comparative;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.esjp.contacts.Contacts;
import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 *
 */
@EFapsUUID("89ddbbe5-0446-4122-abbb-3e9ca68fb85d")
@EFapsApplication("eFapsApp-Sales")
public abstract class AbstractComparativeContact_Base
    extends AbstractComparative
{

    /**
     * Used by the AutoCompleteField used in the select contact.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4Contact(final Parameter _parameter)
        throws EFapsException
    {
        final Contacts contacts = new Contacts();
        return contacts.autoComplete4Contact(_parameter);
    }

    /**
     * @param _parameter Parameter as passeb by the eFaps API
     * @return update map
     * @throws EFapsException on error
     */
    public Return updateFields4Contact(final Parameter _parameter)
        throws EFapsException
    {
        final Contacts contacts = new Contacts()
        {

            @Override
            public String getFieldValue4Contact(final Instance _instance)
                throws EFapsException
            {
                return AbstractComparativeContact_Base.this.getFieldValue4Contact(_instance);
            }
        };
        return contacts.updateFields4Contact(_parameter);
    }

    /**
     * Method to get the value for the field directly under the Contact.
     *
     * @param _instance Instacne of the contact
     * @return String for the field
     * @throws EFapsException on error
     */
    protected String getFieldValue4Contact(final Instance _instance)
        throws EFapsException
    {
        final Contacts contacts = new Contacts();
        return contacts.getFieldValue4Contact(_instance);
    }
}
