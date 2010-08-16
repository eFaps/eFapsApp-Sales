/*
 * Copyright 2003 - 2009 The eFaps Team
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

package org.efaps.esjp.sales;

import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;

/**
 * This class must be replaced for customization, therefore it is left empty.
 * Functional description can be found in the related "<code>_base</code>"
 * class.
 *
 * @author The eFasp Team
 * @version $Id$
 */
@EFapsUUID("9d760033-3a06-4ef9-ac90-c67fa03c04a6")
@EFapsRevision("$Rev$")
public class Tax extends Tax_Base
{

    /**
     * @param oidTmp
     * @param idTmp
     * @param nameTmp
     */
    protected Tax(final String _oidTmp, final long _idTmp, final String _nameTmp)
    {
        super(_oidTmp, _idTmp, _nameTmp);
    }

}
