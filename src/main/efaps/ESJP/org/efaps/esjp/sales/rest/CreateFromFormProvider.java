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
package org.efaps.esjp.sales.rest;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.efaps.admin.event.EventType;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractUserInterfaceObject;
import org.efaps.admin.ui.field.FieldTable;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.properties.PropertiesUtil;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.ui.rest.provider.StandardFormProvider;
import org.efaps.util.EFapsException;

@EFapsUUID("1cd70df2-37f8-4960-8278-fd9e527ad4eb")
@EFapsApplication("eFapsApp-Sales")
public class CreateFromFormProvider
    extends StandardFormProvider
{

    private Instance derivedInst;

    @Override
    public Instance evalSectionInstance(final Instance instance)
        throws EFapsException
    {
        final var properties = new Properties();
        properties.putAll(getProperties());

        final var instfields = PropertiesUtil.analyseProperty(properties, "instField", 0);

        for (final var instField : instfields.entrySet()) {
            final var inst = Instance.get((String) getPayloadValues().get(instField.getValue()));
            if (InstanceUtils.isValid(inst)) {
                derivedInst = inst;
                return inst;
            }
        }
        return super.evalSectionInstance(instance);
    }

    @Override
    public Map<String, ?> getValues()
        throws EFapsException
    {
        Map<String, Object> map = null;
        if (derivedInst != null) {
            map = new HashMap<>();
            map.put("derived", derivedInst.getOid());
        }
        final var dateStr = LocalDate.now(Context.getThreadContext().getZoneId()).toString();
        map.put("date", dateStr);
        map.put("dueDate", dateStr);

        final var properties = new Properties();
        properties.putAll(getProperties());

        final var cleanFields = PropertiesUtil.analyseProperty(properties, "cleanField", 0);
        for (final var cleanField : cleanFields.entrySet()) {
            map.put(cleanField.getValue(), "");
        }
        return map;
    }

    @Override
    public Map<String, String> evalEventProperties(final AbstractUserInterfaceObject cmd,
                                                   final EventType eventType)
        throws EFapsException
    {
        if (EventType.UI_CONTENT_EVALUATE.equals(eventType) && cmd instanceof final FieldTable fieldTable) {
            final Map<String, String> map = new HashMap<>();
            map.putAll(fieldTable.getEvents(eventType).get(0).getPropertyMap());
            map.put("Type", CISales.PositionAbstract.getType().getName());
            map.put("LinkFrom", CISales.PositionAbstract.DocumentAbstractLink.name);
            return map;
        }
        return super.evalEventProperties(cmd, eventType);
    }

}
