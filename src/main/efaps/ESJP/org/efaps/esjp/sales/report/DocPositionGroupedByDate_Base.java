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
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.AbstractGroupedByDate;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.Partial;
import org.joda.time.format.DateTimeFormatter;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("e852be19-7290-4d0f-ba41-85ea3287372e")
@EFapsRevision("$Rev$")
public abstract class DocPositionGroupedByDate_Base
    extends AbstractGroupedByDate
{

    public ValueList getValueList(final Parameter _parameter,
                                  final DateTime _start,
                                  final DateTime _end,
                                  final DateGroup _dateGourp,
                                  final Properties _props,
                                  final Type... _types)
        throws EFapsException
    {
        final boolean showAmount = false;

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
            startPartial = startPartial.withFieldAdded(_dateGourp.getFieldType(), 1);
        }

        final QueryBuilder attrQueryBldr;
        if (_types == null || _types.length == 0) {
            attrQueryBldr = new QueryBuilder(CISales.DocumentAbstract);
        } else {
            attrQueryBldr = new QueryBuilder(_types[0]);
            for (int i = 1; i < _types.length; i++) {
                attrQueryBldr.addType(_types[i]);
            }
        }
        final List<Status> statuslist = getStatusListFromProperties(new Parameter(), _props);
        if (!statuslist.isEmpty()) {
            attrQueryBldr.addWhereAttrEqValue(CISales.DocumentAbstract.StatusAbstract, statuslist.toArray());
        }
        attrQueryBldr.addWhereAttrGreaterValue(CISales.DocumentAbstract.Date, _start.withTimeAtStartOfDay()
                        .minusMinutes(1));
        attrQueryBldr.addWhereAttrLessValue(CISales.DocumentAbstract.Date, _end.withTimeAtStartOfDay().plusDays(1));

        final QueryBuilder queryBldr;
        if (showAmount) {
            queryBldr = new QueryBuilder(CISales.PositionSumAbstract);
        } else {
            queryBldr = new QueryBuilder(CISales.PositionAbstract);
        }
        queryBldr.addWhereAttrInQuery(CISales.PositionAbstract.DocumentAbstractLink,
                        attrQueryBldr.getAttributeQuery(CISales.DocumentAbstract.ID));

        add2QueryBuilder(_parameter, queryBldr);

        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selDoc = SelectBuilder.get().linkto(CISales.PositionAbstract.DocumentAbstractLink);
        final SelectBuilder selDocInst = new SelectBuilder(selDoc).instance();
        final SelectBuilder selDocDate = new SelectBuilder(selDoc).attribute(CISales.DocumentAbstract.Date);
        final SelectBuilder selDocContactName = new SelectBuilder(selDoc).linkto(CISales.DocumentAbstract.Contact)
                        .attribute(CIContacts.ContactAbstract.Name);
        final SelectBuilder selProd = SelectBuilder.get().linkto(CISales.PositionAbstract.Product);
        final SelectBuilder selProdInst = new SelectBuilder(selProd).instance();
        final SelectBuilder selProdName = new SelectBuilder(selProd).attribute(CIProducts.ProductAbstract.Name);
        final SelectBuilder selProdDescr = new SelectBuilder(selProd).attribute(CIProducts.ProductAbstract.Description);
        final SelectBuilder selProdUoM = new SelectBuilder(selProd).attribute(CIProducts.ProductAbstract.DefaultUoM);

        multi.addSelect(selDocInst, selDocDate, selProdInst, selDocContactName, selProdName, selProdDescr, selProdUoM);
        multi.addAttribute(CISales.PositionAbstract.UoM, CISales.PositionAbstract.Quantity);
        SelectBuilder selRateCurInst = null;
        if (showAmount) {
            selRateCurInst = SelectBuilder.get().linkto(CISales.PositionSumAbstract.RateCurrencyId).instance();
            multi.addSelect(selRateCurInst);
            multi.addAttribute(CISales.PositionSumAbstract.CrossPrice, CISales.PositionSumAbstract.RateCrossPrice,
                            CISales.PositionSumAbstract.NetPrice, CISales.PositionSumAbstract.RateNetPrice);
        }
        multi.execute();
        while (multi.next()) {
            final Instance docInst = multi.getSelect(selDocInst);
            final Map<String, Object> map = new HashMap<>();
            final DateTime dateTime = multi.getSelect(selDocDate);

            if (showAmount) {
                BigDecimal total;
                BigDecimal rateTotal;
                if ("NET".equals(props.getProperty(docInst.getType().getName() + ".Total"))) {
                    total = multi.getAttribute(CISales.PositionSumAbstract.NetPrice);
                    rateTotal = multi.getAttribute(CISales.PositionSumAbstract.RateNetPrice);
                } else {
                    total = multi.getAttribute(CISales.PositionSumAbstract.CrossPrice);
                    rateTotal = multi.getAttribute(CISales.PositionSumAbstract.RateCrossPrice);
                }
                if ("true".equals(props.getProperty(docInst.getType().getName() + ".Negate"))) {
                    total = total.negate();
                    rateTotal = rateTotal.negate();
                }
                final Instance rateCurInst = multi.getSelect(selRateCurInst);
                map.put(CurrencyInst.get(rateCurInst).getISOCode(), rateTotal);
                map.put("BASE", total);
            }
            BigDecimal quantity = multi.getAttribute(CISales.PositionAbstract.Quantity);
            final UoM uom = Dimension.getUoM(multi.<Long>getAttribute(CISales.PositionAbstract.UoM));

            String uoMStr = "";
            final Long defaultUoMID = multi.<Long>getSelect(selProdUoM);
            if (defaultUoMID != null) {
                final UoM defaultUoM = Dimension.getUoM(defaultUoMID);
                uoMStr = defaultUoM.getName();
                if (defaultUoM.equals(uom)) {
                    map.put("quantity", quantity);
                } else {
                    quantity = quantity.multiply(new BigDecimal(uom.getNumerator()))
                                    .setScale(8, BigDecimal.ROUND_HALF_UP)
                                    .divide(new BigDecimal(uom.getDenominator()), BigDecimal.ROUND_HALF_UP);
                    quantity = quantity.multiply(new BigDecimal(defaultUoM.getDenominator()))
                                    .setScale(8, BigDecimal.ROUND_HALF_UP)
                                    .divide(new BigDecimal(defaultUoM.getNumerator()), BigDecimal.ROUND_HALF_UP);
                }
            } else {
                uoMStr = uom.getDimension().getBaseUoM().getName();
                if (!uom.equals(uom.getDimension().getBaseUoM())) {
                    quantity = quantity.multiply(new BigDecimal(uom.getNumerator()))
                                    .setScale(8, BigDecimal.ROUND_HALF_UP)
                                    .divide(new BigDecimal(uom.getDenominator()), BigDecimal.ROUND_HALF_UP);
                }
                map.put("quantity", quantity);
            }
            map.put("docInstance", docInst);
            map.put("contact", multi.getSelect(selDocContactName));
            map.put("partial", getPartial(dateTime, _dateGourp).toString(dateTimeFormatter));
            map.put("type", docInst.getType().getLabel());
            map.put("product", multi.getSelect(selProdName) + " - " + multi.getSelect(selProdDescr)
                            + " [" + uoMStr + "]");
            ret.add(map);
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
