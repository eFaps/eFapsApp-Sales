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
package org.efaps.esjp.sales.listener;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.IEsjpListener;
import org.efaps.esjp.common.listener.ITypedClass;
import org.efaps.esjp.erp.CommonDocument_Base.CreatedDoc;
import org.efaps.util.EFapsException;

/**
 * Basic listener methods belonging to PaymentDocuments.
 *
 * @author The eFaps Team
 *
 */
@EFapsUUID("276db4b2-dc1c-4b25-8ddb-8311e0f066f4")
@EFapsApplication("eFapsApp-Sales")
public interface IOnPayment
    extends IEsjpListener
{

    /**
     * @param _typedClass typed class calling this method
     * @param _parameter Parameter as passed by the eFaps API
     * @param _createdDoc CreatedDoc
     * @throws EFapsException on error
     */
    void executeAutomation(ITypedClass _typedClass,
                           Parameter _parameter,
                           CreatedDoc _createdDoc)
        throws EFapsException;

}
