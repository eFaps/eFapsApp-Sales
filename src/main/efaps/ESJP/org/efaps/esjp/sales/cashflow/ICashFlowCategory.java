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
 */

package org.efaps.esjp.sales.cashflow;

import org.efaps.admin.datamodel.IEnum;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("248bf10e-7a2c-4f5f-b111-6022736b9227")
@EFapsApplication("eFapsApp-Sales")
public interface ICashFlowCategory
    extends IEnum
{

    /**
     * Gets the weight.
     *
     * @return the weight
     */
    Integer getWeight();

    /**
     * Gets the label key.
     *
     * @return the label key
     */
    String getLabelKey();
}
