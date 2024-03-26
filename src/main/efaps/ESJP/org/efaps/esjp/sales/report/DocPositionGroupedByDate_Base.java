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
package org.efaps.esjp.sales.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIMsgProducts;
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
 */
@EFapsUUID("e852be19-7290-4d0f-ba41-85ea3287372e")
@EFapsApplication("eFapsApp-Sales")
public abstract class DocPositionGroupedByDate_Base
    extends AbstractGroupedByDate
{

    /**
     * @param _parameter Paramter as passed by the eFaps API
     * @param _start start date
     * @param _end end date
     * @param _dateGourp date group
     * @param _props properties map
     * @param _types list of type
     * @return ValueList
     * @throws EFapsException on error
     */
    public ValueList getValueList(final Parameter _parameter,
                                  final DateTime _start,
                                  final DateTime _end,
                                  final DateGroup _dateGourp,
                                  final Properties _props,
                                  final Type... _types)
        throws EFapsException
    {
        boolean showAmount = true;
        for (final Type type : _types) {
            if (!type.isKindOf(CISales.DocumentSumAbstract)) {
                showAmount = false;
            }
        }

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
        final List<Status> statuslist = getStatusListFromProperties(new Parameter(), props);
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
        final SelectBuilder selDocName = new SelectBuilder(selDoc).attribute(CISales.DocumentAbstract.Name);
        final SelectBuilder selDocDate = new SelectBuilder(selDoc).attribute(CISales.DocumentAbstract.Date);
        final SelectBuilder selDocContactName = new SelectBuilder(selDoc).linkto(CISales.DocumentAbstract.Contact)
                        .attribute(CIContacts.ContactAbstract.Name);
        final SelectBuilder selProd = SelectBuilder.get().linkto(CISales.PositionAbstract.Product);
        final SelectBuilder selProdInst = new SelectBuilder(selProd).instance();

        final SelectBuilder selProdDescr = new SelectBuilder(selProd).attribute(CIProducts.ProductAbstract.Description);
        final SelectBuilder selProdUoM = new SelectBuilder(selProd).attribute(CIProducts.ProductAbstract.DefaultUoM);

        multi.addSelect(selDocInst, selDocName, selDocDate, selDocContactName,
                        selProdInst, selProdDescr, selProdUoM);
        multi.addAttribute(CISales.PositionAbstract.UoM, CISales.PositionAbstract.Quantity);
        multi.addMsgPhrase(selProd, CIMsgProducts.SlugMsgPhrase);
        SelectBuilder selRateCurInst = null;
        if (showAmount) {
            selRateCurInst = SelectBuilder.get().linkto(CISales.PositionSumAbstract.RateCurrencyId).instance();
            multi.addSelect(selRateCurInst);
            multi.addAttribute(CISales.PositionSumAbstract.CrossPrice, CISales.PositionSumAbstract.RateCrossPrice,
                            CISales.PositionSumAbstract.NetPrice, CISales.PositionSumAbstract.RateNetPrice);
        }
        add2Print(_parameter, multi);
        multi.execute();

        while (multi.next()) {
            final Instance docInst = multi.getSelect(selDocInst);
            final Map<String, Object> map = new HashMap<>();
            final DateTime date = multi.getSelect(selDocDate);

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
                if (!defaultUoM.equals(uom)) {
                    quantity = quantity.multiply(new BigDecimal(uom.getNumerator()))
                                    .setScale(8, RoundingMode.HALF_UP)
                                    .divide(new BigDecimal(uom.getDenominator()), RoundingMode.HALF_UP);
                    quantity = quantity.multiply(new BigDecimal(defaultUoM.getDenominator()))
                                    .setScale(8, RoundingMode.HALF_UP)
                                    .divide(new BigDecimal(defaultUoM.getNumerator()), RoundingMode.HALF_UP);
                }
            } else {
                uoMStr = uom.getDimension().getBaseUoM().getName();
                if (!uom.equals(uom.getDimension().getBaseUoM())) {
                    quantity = quantity.multiply(new BigDecimal(uom.getNumerator()))
                                    .setScale(8, RoundingMode.HALF_UP)
                                    .divide(new BigDecimal(uom.getDenominator()), RoundingMode.HALF_UP);
                }
            }

            final String productName = multi.getMsgPhrase(selProd, CIMsgProducts.SlugMsgPhrase);

            map.put("quantity", quantity);
            map.put("uoM", uom);
            map.put("uoMStr", uoMStr);
            map.put("docInstance", docInst);
            map.put("docName", multi.getSelect(selDocName));
            map.put("docDate", date);
            map.put("contact", multi.getSelect(selDocContactName));
            map.put("partial", getPartial(date, _dateGourp).toString(dateTimeFormatter));
            map.put("type", docInst.getType().getLabel());
            map.put("productName", productName);
            map.put("productDescr", multi.getSelect(selProdDescr));
            map.put("product", productName + " - " + multi.getSelect(selProdDescr)
                            + " [" + uoMStr + "]");
            map.put("productInst", multi.getSelect(selProdInst));

            add2RowMap(_parameter, multi, map);
            ret.add(map);
        }
        return ret;
    }

    /**
     * Add2 row map.
     *
     * @param _parameter the _parameter
     * @param _multi the _multi
     * @param _map the _map
     * @throws EFapsException the e faps exception
     */
    protected void add2RowMap(final Parameter _parameter,
                              final MultiPrintQuery _multi,
                              final Map<String, Object> _map)
        throws EFapsException
    {
        // to be used by implementations
    }

    /**
     * Add to print.
     *
     * @param _parameter the _parameter
     * @param _multi the _multi
     * @throws EFapsException the e faps exception
     */
    protected void add2Print(final Parameter _parameter,
                             final MultiPrintQuery _multi)
        throws EFapsException
    {
        // to be used by implementations
    }

    /**
     * Add2 query builder.
     *
     * @param _parameter the _parameter
     * @param _queryBldr the _query bldr
     * @throws EFapsException the e faps exception
     */
    protected void add2QueryBuilder(final Parameter _parameter,
                                    final QueryBuilder _queryBldr)
        throws EFapsException
    {
        // to be used by implementations
    }

    /**
     * The Class ValueList.
     */
    public static class ValueList
        extends ArrayList<Map<String, Object>>
    {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        /** The start. */
        private DateTime start;

        /** The end. */
        private DateTime end;

        /** The date gourp. */
        private DateGroup dateGourp;

        /** The props. */
        private Properties props;

        /** The types. */
        private Set<Type> types = new HashSet<>();

        /**
         * Group by.
         *
         * @param _keys the _keys
         * @return the value list
         */
        public ValueList groupBy(final String... _keys)
        {
            final Map<String, Map<String, Object>> tmpMap = new HashMap<>();
            final ValueList ret = new ValueList();
            for (final Map<String, Object> map : this) {
                String key = "";
                for (final String keyTmp : _keys) {
                    key = key + map.get(keyTmp);
                }
                final Map<String, Object> newMap;
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

        /**
         * Gets the doc instances.
         *
         * @return the doc instances
         */
        public Set<Instance> getDocInstances()
        {
            final Set<Instance> ret = new HashSet<>();
            for (final Map<String, Object> map : this) {
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
            return start;
        }

        /**
         * Setter method for instance variable {@link #start}.
         *
         * @param _start value for instance variable {@link #start}
         */
        public void setStart(final DateTime _start)
        {
            start = _start;
        }

        /**
         * Getter method for the instance variable {@link #end}.
         *
         * @return value of instance variable {@link #end}
         */
        public DateTime getEnd()
        {
            return end;
        }

        /**
         * Setter method for instance variable {@link #end}.
         *
         * @param _end value for instance variable {@link #end}
         */
        public void setEnd(final DateTime _end)
        {
            end = _end;
        }

        /**
         * Getter method for the instance variable {@link #dateGourp}.
         *
         * @return value of instance variable {@link #dateGourp}
         */
        public DateGroup getDateGourp()
        {
            return dateGourp;
        }

        /**
         * Setter method for instance variable {@link #dateGourp}.
         *
         * @param _dateGourp value for instance variable {@link #dateGourp}
         */
        public void setDateGourp(final DateGroup _dateGourp)
        {
            dateGourp = _dateGourp;
        }

        /**
         * Getter method for the instance variable {@link #props}.
         *
         * @return value of instance variable {@link #props}
         */
        public Properties getProps()
        {
            return props;
        }

        /**
         * Setter method for instance variable {@link #props}.
         *
         * @param _props value for instance variable {@link #props}
         */
        public void setProps(final Properties _props)
        {
            props = _props;
        }

        /**
         * Getter method for the instance variable {@link #types}.
         *
         * @return value of instance variable {@link #types}
         */
        public Set<Type> getTypes()
        {
            return types;
        }

        /**
         * Setter method for instance variable {@link #types}.
         *
         * @param _types value for instance variable {@link #types}
         */
        public void setTypes(final Set<Type> _types)
        {
            types = _types;
        }
    }
}
