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

package org.efaps.esjp.sales;

import java.math.BigDecimal;
import java.util.Map;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * This class must be replaced for customization, therefore it is left empty.
 * Functional description can be found in the related "<code>_base</code>"
 * class.
 *
 * @author The eFaps Team
 */
@EFapsUUID("90a0c173-06dc-4d91-90b5-bb72845ed827")
@EFapsApplication("eFapsApp-Sales")
public class Costing
    extends Costing_Base
{

    public static BigDecimal getResult4Currency(final Parameter _parameter,
                                               final Instance _currencyInstance,
                                               final Instance _transactionInstance)
        throws EFapsException
    {
        return Costing_Base.getResult4Currency(_parameter, _currencyInstance, _transactionInstance);
    }

    public static BigDecimal getResult4Currency(final Parameter _parameter,
                                                final DateTime _date,
                                                final Instance _currencyInstance,
                                                final Instance _transactionInstance)
         throws EFapsException
     {
         return Costing_Base.getResult4Currency(_parameter, _date, _currencyInstance, _transactionInstance);
     }

    public static  Map<Instance, BigDecimal>  getResults4Currency(final Parameter _parameter,
                                               final Instance _currencyInstance,
                                               final Instance... _transactionInstances)
        throws EFapsException
    {
        return Costing_Base.getResults4Currency(_parameter, _currencyInstance, _transactionInstances);
    }

    public static  Map<Instance, BigDecimal>  getResults4Currency(final Parameter _parameter,
                                                final DateTime _date,
                                                final Instance _currencyInstance,
                                                final Instance... _transactionInstances)
         throws EFapsException
     {
         return Costing_Base.getResults4Currency(_parameter, _date, _currencyInstance, _transactionInstances);
     }
}
