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

import java.util.UUID;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.util.EFapsException;

/**
 * This class must be replaced for customization, therefore it is left empty.
 * Functional description can be found in the related "<code>_base</code>"
 * class.
 *
 * @author The eFasp Team
 */
@EFapsUUID("11947a89-f648-46c4-b5f0-db572ff23e17")
@EFapsApplication("eFapsApp-Sales")
public class TaxCat
    extends TaxCat_Base
{

    /** The Constant CACHEKEY. */
    public static final String CACHEKEY = TaxCat_Base.CACHEKEY;

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * @param _instance Instance of the TaxCategory
     * @param _uuid     uuid of the TaxCategory
     * @param _name     name of the TaxCategory
     */
    public TaxCat(final Instance _instance,
                  final String _uuid,
                  final String _name)
    {
        super(_instance, _uuid, _name);
    }

    /**
     * Get a TaxCat for an given id.
     * @param _taxCatId id of the taxcat wanted
     * @return taxcat for the id
     * @throws EFapsException on error
     */
    public static TaxCat get(final long _taxCatId)
        throws EFapsException
    {
        return TaxCat_Base.get(_taxCatId);
    }

    /**
     * Get a TaxCat for an given UUID.
     * @param _uuid uuid of the taxcat wanted
     * @return taxcat for the id
     * @throws EFapsException on error
     */
    public static TaxCat get(final UUID _uuid)
        throws EFapsException
    {
        return TaxCat_Base.get(_uuid);
    }
}
