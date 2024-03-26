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
package org.efaps.esjp.ci;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsNoUpdate;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIAttribute;
import org.efaps.ci.CIType;

/**
 * This class is only used in case that the Archive App is not installed
 * to be able to compile the classes.
 * @author The eFaps Team
 */
@EFapsUUID("00a820bd-d8c1-48c6-a97c-9048f27fd061")
@EFapsApplication("eFapsApp-Sales")
@EFapsNoUpdate
public final class CIArchives
{
    public static final _Document2ArchiveAbstract Document2ArchiveAbstract = new _Document2ArchiveAbstract("459e97b9-b803-42c4-b620-d5af855eff4c");
    public static class _Document2ArchiveAbstract extends _Object2ArchiveAbstract
    {
        protected _Document2ArchiveAbstract(final String _uuid)
        {
            super(_uuid);
        }
        public final CIAttribute FromLinkAbstract = new CIAttribute(this, "FromLinkAbstract");
    }

    public static final _Object2ArchiveAbstract Object2ArchiveAbstract = new _Object2ArchiveAbstract("dff29743-ed7d-4cca-83a4-2c140777a752");
    public static class _Object2ArchiveAbstract extends CIType
    {
        protected _Object2ArchiveAbstract(final String _uuid)
        {
            super(_uuid);
        }
        public final CIAttribute Created = new CIAttribute(this, "Created");
        public final CIAttribute Creator = new CIAttribute(this, "Creator");
        public final CIAttribute Modified = new CIAttribute(this, "Modified");
        public final CIAttribute Modifier = new CIAttribute(this, "Modifier");
        public final CIAttribute ToLinkAbstract = new CIAttribute(this, "ToLinkAbstract");
    }
}
