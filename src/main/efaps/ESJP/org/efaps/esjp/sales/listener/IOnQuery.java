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
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */

package org.efaps.esjp.sales.listener;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.IEsjpListener;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.common.listener.ITypedClass;
import org.efaps.util.EFapsException;

/**
 * Contains methods that are executed during the process of executing queries
 * against the eFaps Database like Autocompletes or MultiPrints.
 *
 * @author The eFaps Team
 *
 */
@EFapsUUID("e7c522ee-861b-461b-afe7-1e9f1694da70")
@EFapsApplication("eFapsApp-Sales")
public interface IOnQuery
    extends IEsjpListener
{

    /**
     * @param _typedClass typed class calling this method
     * @param _parameter Parameter as passed by the eFaps API
     * @param _queryBldr QueryBuilder to be added to
     * @return true if caching allowed, false if not
     * @throws EFapsException on error
     */
    boolean add2QueryBldr4AutoComplete4Product(final ITypedClass _typedClass,
                                            final Parameter _parameter,
                                            final QueryBuilder _queryBldr)
        throws EFapsException;

}
