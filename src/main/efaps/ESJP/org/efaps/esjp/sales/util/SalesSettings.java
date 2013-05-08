/*
 * Copyright 2003 - 2013 The eFaps Team
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


package org.efaps.esjp.sales.util;

import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("3cad9ef8-22e6-4b3f-9a97-3aa984d8d6c6")
@EFapsRevision("$Rev$")
public interface SalesSettings
{
    /**
     * OID for a Link. BSe Currency for the System.
     */
    String CURRENCYBASE = "org.efaps.sales.CurrencyBase";

    /**
     * OID for a Link. BSe Currency for the System.
     */
    String CURRENCY4INVOICE = "org.efaps.sales.Currency4Invoice";


}
