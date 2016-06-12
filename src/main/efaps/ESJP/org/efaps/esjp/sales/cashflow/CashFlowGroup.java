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

package org.efaps.esjp.sales.cashflow;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
public enum CashFlowGroup
    implements ICashFlowGroup
{
    /** The in. */
    IN(1),
    /** The out. */
    OUT(2);

    /** The weight. */
    private int weight;

    /**
     * Instantiates a new cash flow group.
     *
     * @param _weight the weight
     */
    CashFlowGroup(final int _weight)
    {
        this.weight = _weight;
    }

    @Override
    public Integer getWeight()
    {
        return this.weight;
    }

    @Override
    public String getLabelKey()
    {
        return name();
    }
}
