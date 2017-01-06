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

import java.util.List;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.IEsjpListener;
import org.efaps.esjp.common.listener.ITypedClass;
import org.efaps.esjp.erp.IWarning;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("7f5f5b7a-144f-4358-ab6f-3d8e321fed9b")
@EFapsApplication("eFapsApp-Sales")
public interface IOnValidation
    extends IEsjpListener
{

    /**
     * Validate.
     *
     * @param _parameter the parameter
     * @param _doc the doc
     * @param _warnings the warnings
     * @throws EFapsException the e faps exception
     */
    void validate(Parameter _parameter,
                  ITypedClass _doc,
                  List<IWarning> _warnings)
        throws EFapsException;
}
