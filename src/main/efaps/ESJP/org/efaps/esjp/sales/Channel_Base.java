/*
 * Copyright 2003 - 2015 The eFaps Team
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

package org.efaps.esjp.sales;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.ui.wicket.util.DateUtil;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("4541b746-f653-46d8-b066-9aea7e111766")
@EFapsApplication("efapsApp-Sales")
public abstract class Channel_Base
    extends CommonDocument
{

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return update field values
     * @throws EFapsException on error
     */
    public Return updateFields4Condition(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final String fieldName = getProperty(_parameter, "ConditionFieldName", "condition");
        final Instance condInst = Instance.get(_parameter.getParameterValue(fieldName));
        if (condInst.isValid() && condInst.getType().isCIType(CISales.ChannelConditionAbstract)) {
            final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
            final Map<String, String> map = new HashMap<String, String>();
            final PrintQuery print = new PrintQuery(condInst);
            print.addAttribute(CISales.ChannelConditionAbstract.QuantityDays);
            print.execute();
            Integer addDays = print.<Integer>getAttribute(CISales.ChannelConditionAbstract.QuantityDays);
            if (addDays == null) {
                addDays = 0;
            }
            DateTime date;
            if (_parameter.getParameterValue("date_eFapsDate") != null) {
                date = DateUtil.getDateFromParameter(_parameter.getParameterValue("date_eFapsDate"));
            } else {
                date = new DateTime();
            }
            map.put("dueDate_eFapsDate", DateUtil.getDate4Parameter(date.plusDays(addDays)));
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        }
        return retVal;
    }

    /**
     * Gets the condition js.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _contactInstance instance of a contact
     * @param _channelRelType the _channel rel type
     * @return update field values
     * @throws EFapsException on error
     */
    public StringBuilder getConditionJs(final Parameter _parameter,
                                        final Instance _contactInstance,
                                        final CIType _channelRelType)
        throws EFapsException
    {
        final StringBuilder js = new StringBuilder();
        if (_contactInstance != null && _contactInstance.isValid()) {
            final QueryBuilder queryBldr = new QueryBuilder(_channelRelType);
            queryBldr.addWhereAttrEqValue(CISales.ChannelConditionAbstract2ContactAbstract.ToAbstractLink,
                            _contactInstance);

            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder sel = SelectBuilder.get()
                            .linkto(CISales.ChannelConditionAbstract2ContactAbstract.FromAbstractLink);
            final SelectBuilder selInst = new SelectBuilder(sel).instance();
            final SelectBuilder selName = new SelectBuilder(sel).attribute(CISales.ChannelConditionAbstract.Name);
            final SelectBuilder selDays = new SelectBuilder(sel).attribute(CISales.ChannelConditionAbstract.QuantityDays);
            multi.addSelect(selInst, selName, selDays);
            multi.addAttribute(CISales.ChannelConditionAbstract2ContactAbstract.IsDefault);
            multi.execute();
            Object[] defaultObj = null;
            final List<Object[]> values = new ArrayList<>();
            while (multi.next()) {
                final Instance inst = multi.getSelect(selInst);
                final String name = multi.getSelect(selName);
                final Integer days = multi.getSelect(selDays);
                final Object[] obj = new Object[] { inst.getOid(), name, days };
                if (BooleanUtils.isTrue(multi.<Boolean>getAttribute(
                                CISales.ChannelConditionAbstract2ContactAbstract.IsDefault))) {
                    defaultObj = obj;
                }
                values.add(obj);
            }
            Collections.sort(values, new Comparator<Object[]>()
            {

                @Override
                public int compare(final Object[] arg0,
                                   final Object[] arg1)
                {
                    return String.valueOf(arg0[1]).compareTo(String.valueOf(arg1[1]));
                }
            });

            if (!values.isEmpty()) {
                final Integer days = defaultObj == null ? (Integer) values.get(0)[2] : (Integer) defaultObj[2];
                js.append("eFapsSetFieldValue(0,'condition', new Array('")
                                .append(defaultObj == null ? values.get(0)[0] : defaultObj[0]).append("'");
                for (final Object[] obj : values) {
                    js.append(",'").append(obj[0]).append("','")
                                    .append(StringEscapeUtils.escapeEcmaScript(String.valueOf(obj[1]))).append("'");
                }

                js.append(")); ");
                DateTime date;
                if (_parameter.getParameterValue("date_eFapsDate") != null) {
                    date = DateUtil.getDateFromParameter(_parameter.getParameterValue("date_eFapsDate"));
                } else {
                    date = new DateTime();
                }
                js.append(getSetFieldValue(0, "dueDate_eFapsDate", DateUtil.getDate4Parameter(date.plusDays(days))));
            }
        }
        return js;
    }
}
