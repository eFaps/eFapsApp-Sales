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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.util.EFapsException;

/**
 * This class must be replaced for customization, therefore it is left empty.
 * Functional description can be found in the related "<code>_Base</code>"
 * class.
 *
 * @author The eFaps Team
 */
@EFapsUUID("8c053f91-9893-4e45-be2d-a3098493a258")
@EFapsApplication("eFapsApp-Sales")
public class Swap
    extends Swap_Base
{
    /**
     * Key used for storing information during request.
     */
    public static final String REQUESTKEY = Swap_Base.REQUESTKEY;

    /**
     * Gets the swap infos.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _callInstance the call instance
     * @param _relInsts the rel insts
     * @return the swap infos
     * @throws EFapsException on error
     */
    public static Map<Instance, SwapInfo> getSwapInfos(final Parameter _parameter,
                                                       final Instance _callInstance,
                                                       final List<Instance> _relInsts)
        throws EFapsException
    {
        return Swap_Base.getSwapInfos(_parameter, _callInstance, _relInsts);
    }

    /**
     * Gets the swap infos.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInstances the doc instances
     * @return the swap infos
     * @throws EFapsException on error
     */
    public static Map<Instance, Set<SwapInfo>> getSwapInfos4Documents(final Parameter _parameter,
                                                                      final Instance... _docInstances)
        throws EFapsException
    {
        return Swap_Base.getSwapInfos4Documents(_parameter, _docInstances);
    }

}
