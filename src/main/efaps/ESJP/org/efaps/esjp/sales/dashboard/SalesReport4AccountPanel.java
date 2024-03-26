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
 * The Class SalesReport4AccountPanel.
 *
 * @author The eFaps Team
 */
@EFapsUUID("2948cf53-1a3f-4016-a31b-3a33b150572c")
@EFapsApplication("eFapsApp-Sales")
public class SalesReport4AccountPanel
    extends SalesReport4AccountPanel_Base
{

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new sales4 contact panel.
     */
    public SalesReport4AccountPanel()
    {
        super();
    }

    /**
     * Instantiates a new sales4 contact panel_ base.
     *
     * @param _config the _config
     */
    public SalesReport4AccountPanel(final String _config)
    {
        super(_config);
    }
}
