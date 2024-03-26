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
package org.efaps.esjp.sales.document;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.util.EFapsException;

/**
 * This class must be replaced for customization, therefore it is left empty.
 * Functional description can be found in the related "<code>_base</code>"
 * class.
 *
 * @author The eFaps Team
 */
@EFapsUUID("f306b281-cb88-4de2-9352-c2b57e8f010e")
@EFapsApplication("eFapsApp-Sales")
public class DocComparator
    extends DocComparator_Base
{

    /**
     * Mark partial.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _instance the instance
     * @throws EFapsException on error
     */
    public static void markPartial(final Parameter _parameter,
                                   final Instance _instance)
        throws EFapsException
    {
        DocComparator_Base.markPartial(_parameter, _instance);
    }
}
