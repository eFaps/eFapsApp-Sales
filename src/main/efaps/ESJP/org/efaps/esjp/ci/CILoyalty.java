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
//CHECKSTYLE:OFF
package org.efaps.esjp.ci;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsNoUpdate;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIAttribute;

@EFapsUUID("3b9f8bfc-f61e-4abd-bab3-a81ed5272c65")
@EFapsApplication("eFapsApp-Sales")
@EFapsNoUpdate
public final class CILoyalty
{

    public static final _PaymentAbstract PaymentAbstract = new _PaymentAbstract("2d6b095e-2f8b-4c4a-959f-98f33b5b6ae7");
    public static class _PaymentAbstract extends org.efaps.esjp.ci.CISales._PaymentDocumentAbstract
    {
        protected _PaymentAbstract(final String _uuid)
        {
            super(_uuid);
        }
        public final CIAttribute ProgrammLinkAbstract = new CIAttribute(this, "ProgrammLinkAbstract");
        public final CIAttribute StatusAbstract = new CIAttribute(this, "StatusAbstract");
    }

    public static final _PaymentPoints PaymentPoints = new _PaymentPoints("302b3c5e-f32d-4f87-a1f8-d61e1b408f06");
    public static class _PaymentPoints extends _PaymentAbstract
    {
        protected _PaymentPoints(final String _uuid)
        {
            super(_uuid);
        }
        public final CIAttribute Authorization = new CIAttribute(this, "Authorization");
        public final CIAttribute Info = new CIAttribute(this, "Info");
        public final CIAttribute OperationId = new CIAttribute(this, "OperationId");
        public final CIAttribute PointsAmount = new CIAttribute(this, "PointsAmount");
        public final CIAttribute PointsLink = new CIAttribute(this, "PointsLink");
        public final CIAttribute PointsTypeLink = new CIAttribute(this, "PointsTypeLink");
        public final CIAttribute Status = new CIAttribute(this, "Status");
    }

}
