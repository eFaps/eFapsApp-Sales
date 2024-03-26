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
package org.efaps.esjp.sales.dashboard;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;

/**
 * This class must be replaced for customization, therefore it is left empty.
 * Functional description can be found in the related "<code>_Base</code>"
 * class.
 *
 * @author The eFaps Team
 */
@EFapsUUID("463f08a9-e2b4-4dad-8533-42aa480f5a50")
@EFapsApplication("eFapsApp-Sales")
public class SalesVariation4ContactPanel
    extends SalesVariation4ContactPanel_Base
{
    /** */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new sales4 contact panel.
     */
    public SalesVariation4ContactPanel()
    {
        super();
    }

    /**
     * Instantiates a new sales variation4 contact panel.
     *
     * @param _config the _config
     */
    public SalesVariation4ContactPanel(final String _config)
    {
        super(_config);
    }
}
