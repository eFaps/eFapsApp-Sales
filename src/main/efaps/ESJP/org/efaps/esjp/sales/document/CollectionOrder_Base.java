/*
 * Copyright 2003 - 2012 The eFaps Team
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
 * Revision:        $Rev: 8180 $
 * Last Changed:    $Date: 2012-11-08 19:00:37 -0500 (jue, 08 nov 2012) $
 * Last Changed By: $Author: m.aranya@moxter.net $
 */

package org.efaps.esjp.sales.document;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: Payment_Base.java 7671 2012-06-14 17:25:53Z
 *          jorge.cueva@moxter.net $
 */
@EFapsUUID("771297ac-8d56-42f6-a1ed-fa573b42698e")
@EFapsRevision("$Rev: 8180 $")
public abstract class CollectionOrder_Base
    extends AbstractDocumentSum
{

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = createDoc(_parameter);
        connect2Object(_parameter, createdDoc);
        final Return ret = new Return();
        ret.put(ReturnValues.INSTANCE, createdDoc.getInstance());
        return ret;
    }


}
