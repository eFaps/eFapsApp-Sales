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

import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.common.datetime.JodaTimeUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DurationFieldType;
import org.joda.time.Partial;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("a7f00855-0d8d-45ce-b98d-10852607ad6c")
@EFapsRevision("$Rev$")
public abstract class AbstractGroupedByDate_Base
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
            ret = new Partial(new DateTimeFieldType[] { DateTimeFieldType.year(), JodaTimeUtils.halfYearOfYear() },
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


}
