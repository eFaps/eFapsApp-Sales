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
package org.efaps.esjp.sales;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.util.EFapsException;

/**
 * This class must be replaced for customization, therefore it is left empty.
 * Functional description can be found in the related "<code>_base</code>"
 * class.
 *
 * @author The eFaps Team
 * @version $Id: Calculator.java 5989 2010-12-27 22:02:29Z jan@moxter.net $
 */
@EFapsUUID("95155127-2301-4c8a-b288-13779c64c71a")
@EFapsApplication("eFapsApp-Sales")
public interface ICalculatorConfig
{
    /**
     * Gets the Key for SystemComfiguration used for Document like totals.
     *
     * @param _parameter the _parameter
     * @return the key
     * @throws EFapsException on error
     */
    String getSysConfKey4Doc(final Parameter _parameter)
        throws EFapsException;

    /**
     * Gets the Key for SystemComfiguration used for Positions like prices.
     *
     * @param _parameter the _parameter
     * @return the key
     * @throws EFapsException on error
     */
    String getSysConfKey4Pos(final Parameter _parameter)
        throws EFapsException;

    /**
     * is the Price past from the  UserInterface the netprice.
     *
     * @param _parameter the _parameter
     * @return true, if successful
     * @throws EFapsException on error
     */
    boolean priceFromUIisNet(Parameter _parameter)
        throws EFapsException;
}
