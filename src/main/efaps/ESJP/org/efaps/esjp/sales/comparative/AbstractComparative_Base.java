/*
 * Copyright 2003 - 2016 The eFaps Team
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

package org.efaps.esjp.sales.comparative;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.Listener;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.esjp.common.uiform.Field;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.erp.listener.IOnCreateDocument;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("9274142f-4882-43d4-a87f-840eb51d1720")
@EFapsApplication("eFapsApp-Sales")
public abstract class AbstractComparative_Base
    extends CommonDocument
{
    /**
     * Key during request.
     */
    private static final String REQKEY = AbstractComparative.class.getName() +  ".RequestKey";

    /**
     * Method to create the basic Document. The method checks for the Type to be
     * created for every attribute if a related field is in the parameters.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return Instance of the document.
     * @throws EFapsException on error.
     */
    protected CreatedDoc createDoc(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc ret = new CreatedDoc();
        final Insert insert = new Insert(getType4DocCreate(_parameter));
        final String name = getDocName4Create(_parameter);
        insert.add(CISales.ComparativeDocAbstract.Name, name);
        ret.getValues().put(CISales.ComparativeDocAbstract.Name.name, name);

        final String date = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.ComparativeDocAbstract.Date.name));
        if (date != null) {
            insert.add(CISales.ComparativeDocAbstract.Date, date);
            ret.getValues().put(CISales.ComparativeDocAbstract.Date.name, date);
        }
        final String description = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.ComparativeDocAbstract.Description.name));
        if (description != null) {
            insert.add(CISales.ComparativeDocAbstract.Description, description);
            ret.getValues().put(CISales.ComparativeDocAbstract.Description.name, description);
        }
        final String note = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.ComparativeDocAbstract.Note.name));
        if (note != null) {
            insert.add(CISales.ComparativeDocAbstract.Note, note);
            ret.getValues().put(CISales.ComparativeDocAbstract.Note.name, note);
        }
        addStatus2DocCreate(_parameter, insert, ret);
        add2DocCreate(_parameter, insert, ret);
        insert.execute();
        ret.setInstance(insert.getInstance());

        // call possible listeners
        for (final IOnCreateDocument listener : Listener.get().<IOnCreateDocument>invoke(
                        IOnCreateDocument.class)) {
            listener.afterCreate(_parameter, ret);
        }
        return ret;
    }

    /**
     * @param _parameter Paramter as passed by the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return createDetail(final Parameter _parameter)
        throws EFapsException
    {
        final Create create = new Create()
        {

            @Override
            protected void add2basicInsert(final Parameter _parameter,
                                           final Insert _insert)
                throws EFapsException
            {
                add2DetailCreate(_parameter, _insert);
            }
        };
        return create.execute(_parameter);
    }

    /**
     * Add2 detail create.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _insert the insert
     * @throws EFapsException on error
     */
    protected void add2DetailCreate(final Parameter _parameter,
                                    final Insert _insert)
        throws EFapsException
    {

    }

    /**
     * Gets the value4 link.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _linkValue the link value
     * @return the value4 link
     * @throws EFapsException on error
     */
    public abstract String getValue4Link(final Parameter _parameter,
                                         final Long _linkValue)
        throws EFapsException;

    /**
     * Gets the value.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _detailInst the detail inst
     * @param _dateValue the date value
     * @param _decimalValue the decimal value
     * @param _integerValue the integer value
     * @param _stringValue the string value
     * @return the value
     * @throws EFapsException on error
     */
    public String getValue(final Parameter _parameter,
                           final Instance _detailInst,
                           final DateTime _dateValue,
                           final BigDecimal _decimalValue,
                           final Integer _integerValue,
                           final String _stringValue)
        throws EFapsException
    {
        String ret = "";
        if (_decimalValue != null) {
            ret = getValue4Decimal(_parameter, _decimalValue);
        } else if (_integerValue != null) {
            ret = getValue4Integer(_parameter, _integerValue);
        }

        return ret;
    }

    /**
     * Gets the value4 decimal.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _decimalValue the decimal value
     * @return the value4 decimal
     * @throws EFapsException on error
     */
    protected String getValue4Decimal(final Parameter _parameter,
                                      final BigDecimal _decimalValue)
        throws EFapsException
    {
        final DecimalFormat formatter = NumberFormatter.get().getFormatter();
        return formatter.format(_decimalValue);
    }

    /**
     * Gets the value4 integer.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _integerValue the integer value
     * @return the value4 integer
     * @throws EFapsException on error
     */
    protected String getValue4Integer(final Parameter _parameter,
                                      final Integer _integerValue)
        throws EFapsException
    {
        return _integerValue == null ? "" : _integerValue.toString();
    }

    /**
     * Gets the value.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the value
     * @throws EFapsException on error
     */
    @SuppressWarnings("unchecked")
    public Return getValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        Map<Instance, String> values;
        if (Context.getThreadContext().containsRequestAttribute(AbstractComparative_Base.REQKEY)) {
            values = (Map<Instance, String>) Context.getThreadContext().getRequestAttribute(
                            AbstractComparative_Base.REQKEY);
        } else {
            values = new HashMap<Instance, String>();
            Context.getThreadContext().setRequestAttribute(AbstractComparative_Base.REQKEY, values);

            final List<Instance> detailInst = (List<Instance>) _parameter.get(ParameterValues.REQUEST_INSTANCES);
            final MultiPrintQuery multi = new MultiPrintQuery(detailInst);
            multi.addAttribute(CISales.ComparativeDetailAbstract.Comment,
                            CISales.ComparativeDetailAbstract.AbstractLink,
                            CISales.ComparativeDetailAbstract.AbstractDateValue,
                            CISales.ComparativeDetailAbstract.AbstractDecimalValue,
                            CISales.ComparativeDetailAbstract.AbstractIntegerValue,
                            CISales.ComparativeDetailAbstract.AbstractStringValue);
            multi.executeWithoutAccessCheck();
            while (multi.next()) {
                final String value = getValue(_parameter,
                                multi.getCurrentInstance(),
                                multi.<DateTime>getAttribute(CISales.ComparativeDetailAbstract.AbstractDateValue),
                                multi.<BigDecimal>getAttribute(CISales.ComparativeDetailAbstract.AbstractDecimalValue),
                                multi.<Integer>getAttribute(CISales.ComparativeDetailAbstract.AbstractIntegerValue),
                                multi.<String>getAttribute(CISales.ComparativeDetailAbstract.AbstractStringValue));
                values.put(multi.getCurrentInstance(), value);
            }
        }
        ret.put(ReturnValues.VALUES, values.get(_parameter.getInstance()));
        return ret;
    }


    /**
     * Dimension drop down field value.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return dimensionDropDownFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Field field = new Field()
        {

            @Override
            protected void add2QueryBuilder4List(final Parameter _parameter,
                                                 final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final Instance instance = _parameter.getInstance();
                if (_parameter.getInstance().getType().isKindOf(CISales.ComparativeDetailAbstract.getType())) {
                    final PrintQuery print = new PrintQuery(instance);
                    print.addAttribute(CISales.ComparativeDetailAbstract.ComparativeAbstractLink);
                    print.execute();
                    _queryBldr.addWhereAttrEqValue(CISales.ComparativeDimensionAbstract.ComparativeAbstractLink,
                                print.<Long>getAttribute(CISales.ComparativeDetailAbstract.ComparativeAbstractLink));
                } else {
                    _queryBldr.addWhereAttrEqValue(CISales.ComparativeDimensionAbstract.ComparativeAbstractLink,
                                    instance);
                }
            }
        };
        return field.dropDownFieldValue(_parameter);
    }
}
