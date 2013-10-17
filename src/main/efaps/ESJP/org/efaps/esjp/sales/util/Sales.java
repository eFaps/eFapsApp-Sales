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

import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.util.cache.CacheReloadException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("70a6a397-b8ef-40c5-853e-cff331bc79bb")
@EFapsRevision("$Rev$")
public final class Sales
{
	/**
	 * Singelton.
	 */
	private Sales()
	{
	}

	/**
	 * Enum used for a multistate for Perception in Sales_Contacts_ClassTaxinfo.
	 */
	public enum TaxPerception {
		/** Cliente Normal. DEFAULT VALUE if no information available. */
		CLIENT,
		/** Cliente final. */
		ENDCOSTUMER,
		/** Agente de Percepcion. */
		AGENT;
	}

	/**
	 * @return the SystemConfigruation for Sales
	 * @throws CacheReloadException on error
	 */
	public static SystemConfiguration getSysConfig()
			throws CacheReloadException
			{
		// Sales-Configuration
		return SystemConfiguration.get(UUID
				.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f"));
	}
}
