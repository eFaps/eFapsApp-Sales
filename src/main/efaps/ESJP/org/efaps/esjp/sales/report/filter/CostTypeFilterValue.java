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
package org.efaps.esjp.sales.report.filter;

import java.util.List;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.esjp.common.uiform.Field_Base.DropDownPosition;
import org.efaps.util.EFapsException;

/**
 * This class must be replaced for customization, therefore it is left empty.
 * Functional description can be found in the related "<code>_Base</code>"
 * class.
 *
 * @author The eFaps Team
 */
@EFapsUUID("58cb39f6-ffb5-4ffb-ba38-7e1f21b611e8")
@EFapsApplication("eFapsApp-Sales")
public class CostTypeFilterValue
    extends CostTypeFilterValue_Base
{
    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Gets the cost type positions.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _selected the selected
     * @param _currencyInsts the currency insts
     * @return the cost type positions
     * @throws EFapsException on error
     */
    public static List<DropDownPosition> getCostTypePositions(final Parameter _parameter,
                                                              final CostTypeFilterValue _selected,
                                                              final Instance... _currencyInsts)
        throws EFapsException
    {
        return CostTypeFilterValue_Base.getCostTypePositions(_parameter, _selected, _currencyInsts);
    }
}
