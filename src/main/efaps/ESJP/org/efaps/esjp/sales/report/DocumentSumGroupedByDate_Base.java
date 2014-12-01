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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DurationFieldType;
import org.joda.time.Partial;

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

    public enum DateGroup
    {
        WEEK(DurationFieldType.weeks()),

        MONTH(DurationFieldType.months());

        private final DurationFieldType fieldType;

        private DateGroup(final DurationFieldType _fieldType)
        {
            this.fieldType = _fieldType;
        }

        public DurationFieldType getFieldType()
        {
            return this.fieldType;
        }

    }

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
        }
        return ret;
    }

    public List<? extends DataBean> getDataBeans(final DateTime _start,
                                                 final DateTime _end,
                                                 final DateGroup _dateGourp,
                                                 final Properties _props,
                                                 final Type... _types)
        throws EFapsException
    {
        final Properties props = _props == null ? new Properties() : _props;
        final Map<Partial, DataBean> dateGroupmap = new HashMap<>();
        Partial startPartial = getPartial(_start, _dateGourp);
        final Partial endPartial = getPartial(_end, _dateGourp);

        while (!startPartial.isAfter(endPartial)) {
            final DataBean bean = new DataBean().setPartial(startPartial);
            dateGroupmap.put(startPartial, bean);
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
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selRateCurInst = SelectBuilder.get().linkto(CISales.DocumentSumAbstract.RateCurrencyId)
                        .instance();
        multi.addSelect(selRateCurInst);
        multi.addAttribute(CISales.DocumentSumAbstract.CrossTotal, CISales.DocumentSumAbstract.NetTotal,
                        CISales.DocumentSumAbstract.RateCrossTotal, CISales.DocumentSumAbstract.RateNetTotal,
                        CISales.DocumentSumAbstract.Date);
        multi.execute();
        while (multi.next()) {
            final DateTime dateTime = multi.getAttribute(CISales.DocumentSumAbstract.Date);
            BigDecimal total;
            BigDecimal rateTotal;
            if ("NET".equals(props.getProperty(multi.getCurrentInstance().getType().getName() + ".Total"))) {
                total = multi.getAttribute(CISales.DocumentSumAbstract.NetTotal);
                rateTotal = multi.getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
            } else {
                total = multi.getAttribute(CISales.DocumentSumAbstract.CrossTotal);
                rateTotal = multi.getAttribute(CISales.DocumentSumAbstract.RateNetTotal);
            }
            if ("true".equals(props.getProperty(multi.getCurrentInstance().getType().getName() + ".Negate"))) {
                total = total.negate();
                rateTotal = rateTotal.negate();
            }
            final Instance rateCurInst = multi.getSelect(selRateCurInst);
            final DataBean bean = dateGroupmap.get(getPartial(dateTime, _dateGourp));
            bean.addAmount(multi.getCurrentInstance().getType(), "BASE", total);
            bean.addAmount(multi.getCurrentInstance().getType(), CurrencyInst.get(rateCurInst).getISOCode(), rateTotal);
        }
        return new ArrayList<>(dateGroupmap.values());
    }

    public static class ValueBean
    {

        private final Map<String, BigDecimal> amounts = new HashMap<>();

        private Type type;

        public ValueBean addAmount(final String _currISO,
                                   final BigDecimal _amount)
        {
            BigDecimal amount;
            if (this.amounts.containsKey(_currISO)) {
                amount = this.amounts.get(_currISO);
            } else {
                amount = BigDecimal.ZERO;
            }
            this.amounts.put(_currISO, amount.add(_amount));
            return this;
        }

        /**
         * Getter method for the instance variable {@link #type}.
         *
         * @return value of instance variable {@link #type}
         */
        public Type getType()
        {
            return this.type;
        }

        /**
         * Setter method for instance variable {@link #type}.
         *
         * @param _type value for instance variable {@link #type}
         */
        public ValueBean setType(final Type _type)
        {
            this.type = _type;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #amounts}.
         *
         * @return value of instance variable {@link #amounts}
         */
        public Map<String, BigDecimal> getAmounts()
        {
            return this.amounts;
        }
    }

    public static class DataBean
    {

        private Partial partial = new Partial();

        private final Map<Type, ValueBean> values = new HashMap<>();

        public DataBean addAmount(final Type _type,
                                  final String _currISO,
                                  final BigDecimal _amount)
        {
            ValueBean value;
            if (this.values.containsKey(_type)) {
                value = this.values.get(_type);
            } else {
                value = new ValueBean().setType(_type);
                this.values.put(_type, value);
            }
            value.addAmount(_currISO, _amount);
            return this;
        }

        /**
         * Getter method for the instance variable {@link #partial}.
         *
         * @return value of instance variable {@link #partial}
         */
        public Partial getPartial()
        {
            return this.partial;
        }

        /**
         * Setter method for instance variable {@link #partial}.
         *
         * @param _partial value for instance variable {@link #partial}
         */
        public DataBean setPartial(final Partial _partial)
        {
            this.partial = _partial;
            return this;
        }

        @Override
        public String toString()
        {
            return ToStringBuilder.reflectionToString(this);
        }

        /**
         * @param _datasource
         */
        public void add2MapCollection(final Collection<Map<String, ?>> _datasource)
            throws EFapsException
        {
            for (final ValueBean value : getValues().values()) {
                final Map<String, Object> map = new HashMap<>();
                _datasource.add(map);
                map.put("group", getPartial().toString());
                map.put("type", value.getType().getLabel());
                for (final Entry<String, BigDecimal> entry : value.getAmounts().entrySet()) {
                    map.put(entry.getKey(), entry.getValue());
                }
            }
        }

        /**
         * Getter method for the instance variable {@link #values}.
         *
         * @return value of instance variable {@link #values}
         */
        public Map<Type, ValueBean> getValues()
        {
            return this.values;
        }
    }
}
