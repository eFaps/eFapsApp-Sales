/*
 * Copyright 2013 - 2019 The eFaps Team
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
package org.efaps.esjp.sales.document;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;

@EFapsUUID("c24dfc2a-f885-4489-b54d-8288afedf0d8")
@EFapsApplication("eFapsApp-Sales")
public class IncomingQuotation
    extends IncomingQuotation_Base
{
    /**
     * Used to store the Revision in the Context.
     */
    public static final String REVISIONKEY =  IncomingQuotation_Base.REVISIONKEY;
}
