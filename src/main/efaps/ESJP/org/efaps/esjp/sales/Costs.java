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

import java.math.BigDecimal;

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
 *
 */
@EFapsUUID("873ddf05-8342-40ac-b1ac-17afe66c6f0c")
@EFapsApplication("eFapsApp-Sales")
public class Costs
    extends Costs_Base
{

    /**
     * Gets the acquisition cost.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _productInstance the product instance
     * @param _docInst the doc inst
     * @param _currenyInst the curreny inst
     * @return the acquisition cost
     * @throws EFapsException on error
     */
    public static BigDecimal getAcquisitionCost(final Parameter _parameter,
                                                final Instance _productInstance,
                                                final Instance _docInst,
                                                final Instance _currenyInst)
        throws EFapsException
    {
        return Costs_Base.getAcquisitionCost(_parameter, _productInstance, _docInst, _currenyInst);
    }

    /**
     * Gets the acquisition cost.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _productInstance the product instance
     * @param _currenyInst the curreny inst
     * @param _date the date
     * @return the acquisition cost
     * @throws EFapsException on error
     */
    public static BigDecimal getAcquisitionCost4Date(final Parameter _parameter,
                                                     final Instance _productInstance,
                                                     final Instance _currenyInst,
                                                     final DateTime _date)
        throws EFapsException
    {
        return Costs_Base.getAcquisitionCost4Date(_parameter, _productInstance, _currenyInst, _date);
    }
}
