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
package org.efaps.esjp.projects;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsNoUpdate;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;

/**
 * Class is used as placeholder to be able to link to an esjp for events.
 * It will be replaced on installation for the real one from the
 * Projects App.
 *
 * @author The eFaps Team
 * 
 */
@EFapsUUID("7bc8c88f-64c3-402d-aed9-1ad0d93f5437")
@EFapsApplication("eFapsApp-Sales")
@EFapsNoUpdate
public class Project
{
    public StringBuilder getProjectData(final Parameter _parameter,
                                        final Instance _currentValue)
    {
        // PLACEHOLDER ONLY
        return null;
    }
}
