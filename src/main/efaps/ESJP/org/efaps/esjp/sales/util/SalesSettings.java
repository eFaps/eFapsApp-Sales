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
     * Boolean (true/false).<br/>
     * Activate the calculation of Costs for a product in
     * stock. Executed by calling en esjp after the insertion of a Incoming
     * Invoice..
     */
    String CALCULATECOSTS = "org.efaps.sales.ActivateCalculateCosts";

    /**
     * OID for a Link.<br/>
     * Default Currency for the Form like Invoice etc..
     */
    String CURRENCY4INVOICE = "org.efaps.sales.Currency4Invoice";

    /**
     * OID for a Link.<br/>
     * Base Currency for the System.
     */
    String CURRENCYBASE = "org.efaps.sales.CurrencyBase";

    /**
     * Properties.<br/>
     * Activate the possibility the unit price of the specified type
     * will have until n decimal digits.
     */
    String LONGDECIMAL = "org.efaps.sales.ActivateLongDecimal";

    /**
     * Boolean (true/false).<br/>
     * Activate the possibility to define a minimum retail
     * price.
     */
    String MINRETAILPRICE = "org.efaps.sales.ActivateMinRetailPrice";

    /**
     * Boolean (true/false).<br/>
     * Do the product prices include already the tax, or
     * must it be added? (Crossprice or netprice)
     */
    String PRODPRICENET = "org.efaps.sales.ProductPriceIsNetPrice";

    /**
     * Boolean (true/false).<br/>
     * Activate the UserInterface for Sales Channel.
     */
    String CHANNELSALES = "org.efaps.sales.ActivateSalesChannel";

    /**
     * Boolean (true/false).<br/>
     * Activate the UserInterface for Buying Route.
     */
    String CHANNELBUY = "org.efaps.sales.ActivateBuyingRoute";

}
