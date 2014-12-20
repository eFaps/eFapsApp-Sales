/*
 * Copyright 2003 - 2014 The eFaps Team
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
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */

package org.efaps.esjp.sales.report;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.Listener;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.common.datetime.JodaTimeUtils;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.sales.listener.IOnDocumentSumReport;
import org.efaps.util.EFapsException;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DurationField;
import org.joda.time.DurationFieldType;
import org.joda.time.Partial;
import org.joda.time.field.ScaledDurationField;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: DocumentSumGroupedByDate_Base.java 14547 2014-11-30 22:44:37Z
 *          jan@moxter.net $
 */
@EFapsUUID("e7448d39-68b9-45b3-b63b-f9a911040358")
@EFapsRevision("$Rev$")
public abstract class DocumentSumGroupedByDate_Base
    extends AbstractCommon
{


    /**
     * Grouping.
     */
    public enum DateGroup
    {
        /** Year. */
        YEAR(DurationFieldType.years()),
        /** Half of a year. */
        HALFYEAR(JodaTimeUtils.halfYears()),
        /** Quarter of a year. */
        QUARTER(JodaTimeUtils.quarters()),
        /** Month. */
        MONTH(DurationFieldType.months()),
        /** Week. */
        WEEK(DurationFieldType.weeks()),
        /** Day. */
        DAY(DurationFieldType.days());

        /**
         * Fieldtype.
         */
        private final DurationFieldType fieldType;

        /**
         * @param _fieldType fieldType
         */
        private DateGroup(final DurationFieldType _fieldType)
        {
            this.fieldType = _fieldType;
        }

        /**
         * @return the fieldType
         */
        public DurationFieldType getFieldType()
        {
            return this.fieldType;
        }
    }


    public static final DurationFieldType Quarters = new DurationFieldType("quarters")
    {

        private static final long serialVersionUID = -8167713675442491871L;

        @Override
        public DurationField getField(final Chronology chronology)
        {
            return new ScaledDurationField(chronology.months(), Quarters, 3);
        }
    };

    public static final DurationFieldType HalfYears = new DurationFieldType("halfyear")
    {

        private static final long serialVersionUID = -8167713675442491872L;

        @Override
        public DurationField getField(final Chronology chronology)
        {
            return new ScaledDurationField(chronology.months(), HalfYears, 6);
        }
    };

    public Partial getPartial(final DateTime _date,
                              final DateGroup _dateGourp)
    {
        return getPartial(_date, _dateGourp.getFieldType());
    }

    public Partial getPartial(final DateTime _date,
                              final DurationFieldType _fieldType)
    {
        Partial ret = null;
        if (DurationFieldType.weeks().equals(_fieldType)) {
            ret = new Partial(new DateTimeFieldType[] { DateTimeFieldType.weekyear(),
                            DateTimeFieldType.weekOfWeekyear() },
                            new int[] { _date.getWeekyear(), _date.getWeekOfWeekyear() });
        } else if (DurationFieldType.months().equals(_fieldType)) {
            ret = new Partial(new DateTimeFieldType[] { DateTimeFieldType.year(),
                            DateTimeFieldType.monthOfYear() },
                            new int[] { _date.getYear(), _date.getMonthOfYear() });
        } else if (DurationFieldType.days().equals(_fieldType)) {
            ret = new Partial(new DateTimeFieldType[] { DateTimeFieldType.year(),
                            DateTimeFieldType.monthOfYear(), DateTimeFieldType.dayOfMonth() },
                            new int[] { _date.getYear(), _date.getMonthOfYear(), _date.getDayOfMonth() });
        } else if (DurationFieldType.years().equals(_fieldType)) {
            ret = new Partial(new DateTimeFieldType[] { DateTimeFieldType.year() },
                            new int[] { _date.getYear() });
        } else if (JodaTimeUtils.halfYears().equals(_fieldType)) {
            ret = new Partial(new DateTimeFieldType[] { DateTimeFieldType.year(), JodaTimeUtils.halfYearOfYear()},
                            new int[] { _date.getYear(), _date.get(JodaTimeUtils.halfYearOfYear()) });
        } else if (JodaTimeUtils.quarters().equals(_fieldType)) {
            ret = new Partial(new DateTimeFieldType[] { DateTimeFieldType.year(), JodaTimeUtils.quarterOfYear() },
                            new int[] { _date.getYear(), _date.get(JodaTimeUtils.quarterOfYear()) });
        }
        return ret;
    }

    public DateTimeFormatter getDateTimeFormatter(final DateGroup _dateGourp)
    {
        return getDateTimeFormatter(_dateGourp.getFieldType());
    }

    public DateTimeFormatter getDateTimeFormatter(final DurationFieldType _fieldType)
    {
        DateTimeFormatter ret = null;
        if (JodaTimeUtils.halfYears().equals(_fieldType)) {
            ret = new DateTimeFormatterBuilder().appendYear(4, 4).appendLiteral("-S")
                            .appendDecimal(JodaTimeUtils.halfYearOfYear(), 1, 1).toFormatter();
        } else if (JodaTimeUtils.quarters().equals(_fieldType)) {
            ret = new DateTimeFormatterBuilder().appendYear(4, 4).appendLiteral("-Q")
                            .appendDecimal(JodaTimeUtils.quarterOfYear(), 1, 1).toFormatter();
        }
        return ret;
    }

    public ValueList getValueList(final Parameter _parameter,
                                  final DateTime _start,
                                  final DateTime _end,
                                  final DateGroup _dateGourp,
                                  final Properties _props,
                                  final Type... _types)
        throws EFapsException
    {
        final ValueList ret = new ValueList();
        ret.setStart(_start);
        ret.setEnd(_end);
        ret.setDateGourp(_dateGourp);
        CollectionUtils.addAll(ret.getTypes(), _types);

        final Properties props = _props == null ? new Properties() : _props;

        Partial startPartial = getPartial(_start, _dateGourp);
        final Partial endPartial = getPartial(_end, _dateGourp);
        final DateTimeFormatter dateTimeFormatter = getDateTimeFormatter(_dateGourp);


        while (!startPartial.isAfter(endPartial)) {
            // final DataBean bean = new DataBean().setPartial(startPartial);
            // dateGroupmap.put(startPartial, bean);
            startPartial = startPartial.withFieldAdded(_dateGourp.getFieldType(), 1);
        }

        final QueryBuilder queryBldr;
        if (_types == null || _types.length == 0) {
            queryBldr = new QueryBuilder(CISales.DocumentSumAbstract);
        } else {
            queryBldr = new QueryBuilder(_types[0]);
            for (int i = 1; i < _types.length; i++) {
                queryBldr.addType(_types[i]);
            }
        }
        final List<Status> statuslist = getStatusListFromProperties(new Parameter(), _props);
        if (!statuslist.isEmpty()) {
            queryBldr.addWhereAttrEqValue(CISales.DocumentSumAbstract.StatusAbstract, statuslist.toArray());
        }
        queryBldr.addWhereAttrGreaterValue(CISales.DocumentSumAbstract.Date, _start.withTimeAtStartOfDay()
                        .minusMinutes(1));
        queryBldr.addWhereAttrLessValue(CISales.DocumentSumAbstract.Date, _end.withTimeAtStartOfDay().plusDays(1));

        add2QueryBuilder(_parameter, queryBldr);

        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selRateCurInst = SelectBuilder.get().linkto(CISales.DocumentSumAbstract.RateCurrencyId)
                        .instance();
        final SelectBuilder selContactName = SelectBuilder.get().linkto(CISales.DocumentSumAbstract.Contact)
                        .attribute(CIContacts.ContactAbstract.Name);
        multi.addSelect(selContactName, selRateCurInst);
        multi.addAttribute(CISales.DocumentSumAbstract.CrossTotal, CISales.DocumentSumAbstract.NetTotal,
                        CISales.DocumentSumAbstract.RateCrossTotal, CISales.DocumentSumAbstract.RateNetTotal,
                        CISales.DocumentSumAbstract.Date);
        multi.execute();
        while (multi.next()) {
            final Map<String, Object> map = new HashMap<>();
            final DateTime dateTime = multi.getAttribute(CISales.DocumentSumAbstract.Date);
            BigDecimal total;
            BigDecimal rateTotal;
            if ("NET".equals(props.getProperty(multi.getCurrentInstance().getType().getName() + ".Total"))) {
                total = multi.getAttribute(CISales.DocumentSumAbstract.NetTotal);
                rateTotal = multi.getAttribute(CISales.DocumentSumAbstract.RateNetTotal);
            } else {
                total = multi.getAttribute(CISales.DocumentSumAbstract.CrossTotal);
                rateTotal = multi.getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
            }
            if ("true".equals(props.getProperty(multi.getCurrentInstance().getType().getName() + ".Negate"))) {
                total = total.negate();
                rateTotal = rateTotal.negate();
            }
            final Instance rateCurInst = multi.getSelect(selRateCurInst);
            map.put("docInstance", multi.getCurrentInstance());
            map.put("contact", multi.getSelect(selContactName));
            map.put("partial", getPartial(dateTime, _dateGourp).toString(dateTimeFormatter));
            map.put("type", multi.getCurrentInstance().getType().getLabel());
            map.put("BASE", total);
            map.put(CurrencyInst.get(rateCurInst).getISOCode(), rateTotal);
            ret.add(map);
        }

        for (final IOnDocumentSumReport listener : Listener.get().<IOnDocumentSumReport>invoke(
                        IOnDocumentSumReport.class)) {
            listener.add2ValueList(_parameter, ret);
        }
        return ret;
    }

    protected void add2QueryBuilder(final Parameter _parameter,
                                    final QueryBuilder _queryBldr)
        throws EFapsException
    {
        // tobe used by
    }

    public static class ValueList
        extends ArrayList<Map<String, Object>>
    {

        /**
         *
         */
        private static final long serialVersionUID = 1L;
        private DateTime start;
        private DateTime end;
        private DateGroup dateGourp;
        private Properties props;
        private Set<Type> types = new HashSet<>();

        public ValueList groupBy(final String... _keys)
        {
            final Map<String, Map<String, Object>> tmpMap = new HashMap<>();
            final ValueList ret = new ValueList();
            final Iterator<Map<String, Object>> iter = iterator();
            while (iter.hasNext()) {
                final Map<String, Object> map = iter.next();
                String key = "";
                for (final String keyTmp : _keys) {
                    key = key + map.get(keyTmp);
                }
                Map<String, Object> newMap;
                if (tmpMap.containsKey(key)) {
                    newMap = tmpMap.get(key);
                    for (final Entry<String, Object> entry : map.entrySet()) {
                        if (entry.getValue() instanceof BigDecimal) {
                            if (newMap.containsKey(entry.getKey())) {
                                newMap.put(entry.getKey(), ((BigDecimal) newMap.get(entry.getKey()))
                                                .add((BigDecimal) entry.getValue()));
                            } else {
                                newMap.put(entry.getKey(), entry.getValue());
                            }
                        }
                    }
                } else {
                    newMap = map;
                    tmpMap.put(key, newMap);
                    ret.add(newMap);
                }
            }
            return ret;
        }

        public Set<Instance> getDocInstances()
        {
            final Set<Instance> ret = new HashSet<>();
            final Iterator<Map<String, Object>> iter = iterator();
            while (iter.hasNext()) {
                final Map<String, Object> map = iter.next();
                final Instance docInstance = (Instance) map.get("docInstance");
                if (docInstance != null && docInstance.isValid()) {
                    ret.add(docInstance);
                }
            }
            return ret;
        }

        /**
         * Getter method for the instance variable {@link #start}.
         *
         * @return value of instance variable {@link #start}
         */
        public DateTime getStart()
        {
            return this.start;
        }

        /**
         * Setter method for instance variable {@link #start}.
         *
         * @param _start value for instance variable {@link #start}
         */
        public void setStart(final DateTime _start)
        {
            this.start = _start;
        }

        /**
         * Getter method for the instance variable {@link #end}.
         *
         * @return value of instance variable {@link #end}
         */
        public DateTime getEnd()
        {
            return this.end;
        }

        /**
         * Setter method for instance variable {@link #end}.
         *
         * @param _end value for instance variable {@link #end}
         */
        public void setEnd(final DateTime _end)
        {
            this.end = _end;
        }

        /**
         * Getter method for the instance variable {@link #dateGourp}.
         *
         * @return value of instance variable {@link #dateGourp}
         */
        public DateGroup getDateGourp()
        {
            return this.dateGourp;
        }

        /**
         * Setter method for instance variable {@link #dateGourp}.
         *
         * @param _dateGourp value for instance variable {@link #dateGourp}
         */
        public void setDateGourp(final DateGroup _dateGourp)
        {
            this.dateGourp = _dateGourp;
        }

        /**
         * Getter method for the instance variable {@link #props}.
         *
         * @return value of instance variable {@link #props}
         */
        public Properties getProps()
        {
            return this.props;
        }

        /**
         * Setter method for instance variable {@link #props}.
         *
         * @param _props value for instance variable {@link #props}
         */
        public void setProps(final Properties _props)
        {
            this.props = _props;
        }

        /**
         * Getter method for the instance variable {@link #types}.
         *
         * @return value of instance variable {@link #types}
         */
        public Set<Type> getTypes()
        {
            return this.types;
        }

        /**
         * Setter method for instance variable {@link #types}.
         *
         * @param _types value for instance variable {@link #types}
         */
        public void setTypes(final Set<Type> _types)
        {
            this.types = _types;
        }
    }
}
