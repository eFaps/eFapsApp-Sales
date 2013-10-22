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
 * Revision:        $Rev: 8342 $
 * Last Changed:    $Date: 2012-12-11 09:42:17 -0500 (Tue, 11 Dec 2012) $
 * Last Changed By: $Author: jan@moxter.net $
 */


package org.efaps.esjp.sales;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uisearch.Connect;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 * 
 * @author The eFaps Team
 * @version $Id: Channel.java 8120 2012-10-26 18:21:34Z jorge.cueva@moxter.net $
 */
@EFapsUUID("63b2a93a-dc17-4d28-9efc-57d19c5bc46a")
@EFapsRevision("$Rev: 1 $")
public abstract class Condition_Base
{

    public Return connectContact(final Parameter _parameter)
        throws EFapsException
    {
        final Connect connect = new Connect()
        {

            @Override
            protected void addInsertConnect(final Parameter _parameter,
                                            final Insert _insert)
                throws EFapsException
            {
                _insert.add(CISales.ChannelCondition2Contact.Status,
                                Status.find(CISales.Channel2ContactStatus.Requested));
            }
        };
        return connect.execute(_parameter);
    }

}
