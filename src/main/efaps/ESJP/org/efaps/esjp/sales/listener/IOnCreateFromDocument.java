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

import java.util.List;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.IEsjpListener;
import org.efaps.db.Instance;
import org.efaps.util.EFapsException;


/**
 * Contains methods that are executed during the process of creating
 * a document from a document.
 *
 * @author The eFaps Team
 *
 */
@EFapsUUID("ad2f45c6-a34f-4cbd-8e57-de16069ae8bf")
@EFapsApplication("eFapsApp-Sales")
public interface IOnCreateFromDocument
    extends IEsjpListener
{
    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _instances list of instance
     * @return StringBuilder to be added
     * @throws EFapsException on error
     */
    StringBuilder add2JavaScript4Document(final Parameter _parameter,
                                          final List<Instance> _instances) throws EFapsException;
}
