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
package org.efaps.esjp.sales.listener;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsListener;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.util.cache.CacheReloadException;

/**
 * This class must be replaced for customization, therefore it is left empty.
 * Functional description can be found in the related "<code>_base</code>"
 * class.
 *
 * @author The eFaps Team
 */
@EFapsUUID("8685517e-6698-4ccc-900f-9def4bfdffd3")
@EFapsApplication("eFapsApp-Sales")
@EFapsListener
public class OnStatusChange4Payment
    extends OnStatusChange4Payment_Base
{
    /**
     * Instantiates a new on swap participant status change.
     *
     * @throws CacheReloadException the cache reload exception
     */
    public OnStatusChange4Payment()
        throws CacheReloadException
    {
        super();
    }
}
