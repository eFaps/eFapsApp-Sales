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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.admin.datamodel.AbstractSetStatusListener;
import org.efaps.esjp.common.properties.PropertiesUtil;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;

/**
 * The Class OnStatusChange4DependentDocument_Base.
 *
 * @author The eFaps Team
 */
@EFapsUUID("03f9b920-d33a-40a8-957d-212867ef9b5f")
@EFapsApplication("eFapsApp-Sales")
public abstract class OnStatusChange4DependentDocument_Base
    extends AbstractSetStatusListener
{

    @Override
    public void after(final Parameter _parameter,
                      final Instance _instance,
                      final Status _status)
        throws EFapsException
    {
        final String typeName = _instance.getType().getName();
        final Properties props = PropertiesUtil.getProperties4Prefix(Sales.DEPENDENTSLISTENER.get(), typeName, true);
        final Map<Integer, String> relationTypes = PropertiesUtil.analyseProperty(props, "RelationType", 0);
        final Map<Integer, String> instanceLinks = PropertiesUtil.analyseProperty(props, "InstanceLink", 0);
        final Map<Integer, String> dependentLinks = PropertiesUtil.analyseProperty(props, "DependentLink", 0);
        final Map<Integer, String> dependentStatus = PropertiesUtil.analyseProperty(props, "DependentStatus", 0);
        final Map<Integer, String> dependentStatusGrp = PropertiesUtil.analyseProperty(props, "DependentStatusGroup",
                        0);

        for (final Entry<Integer, String> entry : relationTypes.entrySet()) {
            final QueryBuilder queryBldr = new QueryBuilder(Type.get(entry.getValue()));
            queryBldr.addWhereAttrEqValue(instanceLinks.get(entry.getKey()), _instance);
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder depInstSel = SelectBuilder.get().linkto(dependentLinks.get(entry.getKey())).instance();
            multi.addSelect(depInstSel);
            multi.execute();
            while (multi.next()) {
                final Instance depInst = multi.<Instance>getSelect(depInstSel);
                final Update update = new Update(depInst);
                final Status depstatus = Status.find(dependentStatusGrp.get(entry.getKey()), dependentStatus.get(entry
                                .getKey()));
                update.add(depInst.getType().getStatusAttribute(), depstatus);
                update.execute();
            }
        }
    }

    @Override
    public Set<Status> getStatus()
        throws EFapsException
    {
        final Set<Status> ret = super.getStatus();
        final Properties props = PropertiesUtil.getProperties4Prefix(Sales.DEPENDENTSLISTENER.get(), "TriggerStatus",
                        true);
        ret.addAll(getStatusListFromProperties(new Parameter(), props));
        return ret;
    }
}
