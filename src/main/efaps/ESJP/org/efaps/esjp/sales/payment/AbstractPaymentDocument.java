/*
 * Copyright 2003 - 2016 The eFaps Team
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

package org.efaps.esjp.sales.payment;

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
@EFapsUUID("d1921ce1-a706-48f0-969c-07a5ac3564ef")
@EFapsApplication("eFapsApp-Sales")
public abstract class AbstractPaymentDocument
    extends AbstractPaymentDocument_Base
{

    /**
     * Gets the transaction html.
     *
     * @param _parameter the _parameter
     * @param _instance the _instance
     * @return the transaction html
     * @throws EFapsException the e faps exception
     */
    public static StringBuilder getTransactionHtml(final Parameter _parameter,
                                                   final Instance _instance)
        throws EFapsException
    {
        return AbstractPaymentDocument_Base.getTransactionHtml(_parameter, _instance);
    }
}
