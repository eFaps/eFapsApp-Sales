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
package org.efaps.esjp.sales.document;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;


/**
 * Class is a generic class instance for all documents of type DocumentSum.
 * and therefore is empty.
 * This is necessary to be able to use it from any triggers.
 *
 * @author The eFaps Team
 *
 */
@EFapsUUID("bdb7b256-d5c9-4099-9c96-2f4563820ac4")
@EFapsApplication("eFapsApp-Sales")
public abstract class GenericDocumentSum_Base
    extends AbstractDocumentSum
{

}
