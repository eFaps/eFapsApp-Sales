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


package org.efaps.esjp.sales.tax;

import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;



/**
 * This class must be replaced for customization, therefore it is left empty.
 * Functional description can be found in the related "<code>_base</code>"
 * class.
 *
 * @author The eFasp Team
 * @version $Id$
 */
@EFapsUUID("b74fc4d7-aee7-4300-bd1a-7bf59486deca")
@EFapsRevision("$Rev$")
public class Tax
    extends Tax_Base
{

    /**
     * @param _currentInstance
     * @param _nameTmp
     * @param _uuidTmp
     * @param _numerator
     * @param _denominator
     */
    public Tax(final TaxCat_Base _taxcat,
               final Instance _instance,
               final String _nameTmp,
               final String _uuidTmp,
               final Integer _numerator,
               final Integer _denominator)
    {
        super(_taxcat, _instance, _nameTmp, _uuidTmp, _numerator, _denominator);
    }

    public Tax(){
        super();
    }
}
