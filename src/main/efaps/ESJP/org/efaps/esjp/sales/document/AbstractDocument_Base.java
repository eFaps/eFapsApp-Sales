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
package org.efaps.esjp.sales.document;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.IUIValue;
import org.efaps.admin.datamodel.ui.RateUI;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.EventType;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.Listener;
import org.efaps.admin.ui.AbstractCommand;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.admin.ui.field.Field;
import org.efaps.api.ui.IMapFilter;
import org.efaps.ci.CIType;
import org.efaps.db.AttributeQuery;
import org.efaps.db.CachedInstanceQuery;
import org.efaps.db.CachedPrintQuery;
import org.efaps.db.Context;
import org.efaps.db.Delete;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.common.properties.PropertiesUtil;
import org.efaps.esjp.common.uiform.Field_Base.DropDownPosition;
import org.efaps.esjp.common.uiform.Field_Base.ListType;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.common.util.InterfaceUtils_Base.DojoLibs;
import org.efaps.esjp.contacts.Contacts;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.erp.RateFormatter;
import org.efaps.esjp.erp.RateInfo;
import org.efaps.esjp.erp.listener.IOnCreateDocument;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.esjp.products.Batch;
import org.efaps.esjp.products.Product;
import org.efaps.esjp.products.Storage;
import org.efaps.esjp.products.util.Products;
import org.efaps.esjp.products.util.Products.ProductIndividual;
import org.efaps.esjp.sales.Calculator;
import org.efaps.esjp.sales.ICalculatorConfig;
import org.efaps.esjp.sales.listener.IOnCreateFromDocument;
import org.efaps.esjp.sales.listener.IOnQuery;
import org.efaps.esjp.sales.tax.TaxesAttribute;
import org.efaps.esjp.sales.tax.xml.Taxes;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.Sales.TaxPerception;
import org.efaps.esjp.sales.util.Sales.TaxRetention;
import org.efaps.ui.wicket.models.objects.UIForm;
import org.efaps.ui.wicket.models.objects.UITable;
import org.efaps.ui.wicket.models.objects.UITable.TableFilter;
import org.efaps.util.EFapsException;
import org.efaps.util.cache.CacheReloadException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author The eFaps Team
 */
@EFapsUUID("b3b70ce7-16d0-4425-8ddd-b667cfd3329a")
@EFapsApplication("eFapsApp-Sales")
public abstract class AbstractDocument_Base
    extends CommonDocument
    implements ICalculatorConfig
{
    /**
     * Key to the Calculator.
     */
    public static final String CALCULATORS_VALUE = AbstractDocument.class.getName() + ".CalculatorValue";

    /**
     * Key to the Calculator.
     */
    public static final String FIELDTABLES = AbstractDocument.class.getName() + ".FieldTables";

    /**
     * Key used to store the list of calculators in the session.
     */
    public static final String CALCULATOR_KEY = AbstractDocument.class.getName() + ".CalculatorKey";

    /**
     * Key used to store the target mode for the Document in the session.
     */
    public static final String TARGETMODE_DOC_KEY = AbstractDocument.class.getName() + ".TargeModeKey";

    /**
     * Used as a prefix for update script.
     */
    public static final String SELDOCUPDATEPF = "SDUP_";

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDocument.class);


    protected <T> T getValue(final Parameter parameter,
                             final String key)
    {
        return getValue(parameter, key, 0);
    }

    @SuppressWarnings("unchecked")
    protected <T> T getValue(final Parameter parameter,
                             final String key,
                             final int index)
    {
        T ret = null;
        final Map<String, Object> payload = (Map<String, Object>) parameter.get(ParameterValues.PAYLOAD);
        if (payload != null) {
            final var content = payload.get(key);
            if (content instanceof final List list) {
                if (index < list.size()) {
                    ret = (T) list.get(index);
                }
            } else {
                ret = (T) content;
            }
        }
        return ret;
    }

    protected BigDecimal evalBigDecimal(final Parameter parameter,
                                        final String key,
                                        final int index)
    {
        final var value = getValue(parameter, key, index);
        final BigDecimal ret;
        if (value != null && value instanceof final String str && StringUtils.isNotEmpty(str)) {
            ret = new BigDecimal(str);
        } else if (value != null && value instanceof final Integer integer) {
            ret = new BigDecimal(integer);
        } else {
            ret = BigDecimal.ZERO;//
        }
        return ret;
    }


    /**
     * Method must be called on opening the form containing positions to
     * initialise a new positions calculator cache!
     *
     * @param _parameter Parameter as passed by the eFaps API for esjp
     * @return new Return
     * @throws EFapsException on error
     */
    public Return activatePositionsCalculator(final Parameter _parameter)
        throws EFapsException
    {
        Context.getThreadContext().setSessionAttribute(AbstractDocument_Base.CALCULATOR_KEY,
                        new ArrayList<>());
        return new Return();
    }

    /**
     * Used by the AutoCompleteField used in the select doc form.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4Doc(final Parameter _parameter)
        throws EFapsException
    {
        return autoComplete4Doc(_parameter, getQueryBldrFromProperties(_parameter));
    }

    /**
     * Generic method to get a list of documents.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @param _typeUUID UUID of the type to be searched.
     * @param _status status used as additional filter, <code>null</code> to
     *            deactivated
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    protected Return autoComplete4Doc(final Parameter _parameter,
                                      final UUID _typeUUID,
                                      final Status... _status)
        throws EFapsException
    {
        final Type type = Type.get(_typeUUID);
        // if the status is not set explicitly we analyze the properties
        final Status[] status;
        if (_status == null && type.isCheckStatus()) {
            final Type statusType = type.getStatusAttribute().getLink();
            final List<Status> statusList = new ArrayList<>();
            final Map<Integer, String> statusMap = analyseProperty(_parameter, "Status");
            for (final String statusStr : statusMap.values()) {
                final Status statusTmp = Status.find(statusType.getUUID(), statusStr);
                if (statusTmp != null) {
                    statusList.add(statusTmp);
                }
            }
            status = statusList.isEmpty() ? (Status[]) null : statusList.toArray(new Status[statusList.size()]);
        } else {
            status = _status;
        }
        final QueryBuilder queryBldr = new QueryBuilder(type);
        if (status != null) {
            queryBldr.addWhereAttrEqValue(CISales.DocumentAbstract.StatusAbstract, (Object[]) status);
        }
        return autoComplete4Doc(_parameter, queryBldr);
    }

    /**
     * Generic method to get a list of documents.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @param _queryBldr QueryBuilder used as base
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    protected Return autoComplete4Doc(final Parameter _parameter,
                                      final QueryBuilder _queryBldr)
        throws EFapsException
    {
        final String req = (String) _parameter.get(ParameterValues.OTHERS);

        final List<Map<String, String>> list = new ArrayList<>();
        final Map<String, Map<String, String>> tmpMap = new TreeMap<>();

        InterfaceUtils.addMaxResult2QueryBuilder4AutoComplete(_parameter, _queryBldr);
        add2QueryBldr(_parameter, _queryBldr);
        _queryBldr.addWhereAttrMatchValue(CISales.DocumentAbstract.Name, req + "*").setIgnoreCase(true);

        final String key = containsProperty(_parameter, "Key") ? getProperty(_parameter, "Key") : "OID";
        final boolean showContact = !"false".equalsIgnoreCase(getProperty(_parameter, "ShowContact"));

        final MultiPrintQuery multi = _queryBldr.getPrint();
        final SelectBuilder selContactName = SelectBuilder.get().linkto(CISales.DocumentAbstract.Contact)
                        .attribute(CIContacts.Contact.Name);
        if (showContact) {
            multi.addSelect(selContactName);
        }
        multi.addAttribute(key);
        multi.addAttribute(CISales.DocumentAbstract.Name, CISales.DocumentAbstract.Date);
        multi.execute();
        while (multi.next()) {
            final String name = multi.<String>getAttribute(CISales.DocumentAbstract.Name);
            final DateTime date = multi.<DateTime>getAttribute(CISales.DocumentAbstract.Date);
            String choice = name + " - " + date.toString(DateTimeFormat.forStyle("S-").withLocale(
                            Context.getThreadContext().getLocale()));
            if (showContact) {
                choice = choice + " - " + multi.getSelect(selContactName);
            }
            choice = choice + add2ChoiceAutoComplete4Doc(_parameter, multi.getCurrentInstance());
            final Map<String, String> map = new HashMap<>();
            map.put("eFapsAutoCompleteKEY", multi.getAttribute(key).toString());
            map.put("eFapsAutoCompleteVALUE", name);
            map.put("eFapsAutoCompleteCHOICE", choice);
            tmpMap.put(name, map);
        }
        list.addAll(tmpMap.values());
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @param _instance     instance the choice belongs to
     * @return string to add
     * @throws EFapsException on error
     */
    protected String add2ChoiceAutoComplete4Doc(final Parameter _parameter,
                                                final Instance _instance)
        throws EFapsException
    {
        // to be used by implementations
        return "";
    }

    /**
     * Used by the AutoCompleteField used in the select contact.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return map list for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4Contact(final Parameter _parameter)
        throws EFapsException
    {
        final Contacts contacts = new Contacts()
        {

            @Override
            protected QueryBuilder getQueryBldr4AutoComplete(final Parameter _parameter)
                throws EFapsException
            {
                final QueryBuilder ret = super.getQueryBldr4AutoComplete(_parameter);
                AbstractDocument_Base.this.add2QueryBldr4AutoComplete4Contact(_parameter, ret);
                return ret;
            }
        };
        return contacts.autoComplete4Contact(_parameter);
    }

    /**
     * Add2 query bldr4 auto complete4 contact.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @param _queryBldr the _query bldr
     * @throws EFapsException on error
     */
    protected void add2QueryBldr4AutoComplete4Contact(final Parameter _parameter,
                                                      final QueryBuilder _queryBldr)
        throws EFapsException
    {
        // to be used by implementations
    }

    /**
     * @param _parameter Parameter as passeb by the eFaps API
     * @return update map
     * @throws EFapsException on error
     */
    public Return updateFields4Contact(final Parameter _parameter)
        throws EFapsException
    {
        final Contacts contacts = new Contacts() {
            @Override
            public String getFieldValue4Contact(final Instance _instance)
                throws EFapsException
            {
                return AbstractDocument_Base.this.getFieldValue4Contact(_instance, true);
            }

            @Override
            protected void add2UpdateMap4Contact(final Parameter _parameter,
                                                 final Instance _contactInstance,
                                                 final Map<String, Object> _map)
                throws EFapsException
            {
                super.add2UpdateMap4Contact(_parameter, _contactInstance, _map);
                AbstractDocument_Base.this.add2UpdateMap4Contact(_parameter, _contactInstance, _map);
            }
        };
        return contacts.updateFields4Contact(_parameter);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _contactInstance instance of the contact
     * @param _map  map to be added to
     * @throws EFapsException on error
     */
    protected void add2UpdateMap4Contact(final Parameter _parameter,
                                         final Instance _contactInstance,
                                         final Map<String, Object> _map)
        throws EFapsException
    {
        if (Sales.CLASSTAXINFOACTIVATE.get()) {
            final PrintQuery print = new PrintQuery(_contactInstance);
            final SelectBuilder selRetention = SelectBuilder.get()
                            .clazz(CISales.Contacts_ClassTaxinfo)
                            .attribute(CISales.Contacts_ClassTaxinfo.Retention);
            final SelectBuilder selPerception = SelectBuilder.get()
                            .clazz(CISales.Contacts_ClassTaxinfo)
                            .attribute(CISales.Contacts_ClassTaxinfo.Perception);

            print.addSelect(selRetention, selPerception);
            print.execute();
            final TaxRetention retention = print.getSelect(selRetention);
            final TaxPerception perception = print.getSelect(selPerception);

            StringBuilder js = new StringBuilder();
            final StringBuilder label = new StringBuilder();
            if (TaxRetention.AGENT.equals(retention)) {
                label.append(DBProperties.getProperty(TaxRetention.class.getName()
                                + "." + TaxRetention.AGENT.toString()));
            }
            if (TaxPerception.AGENT.equals(perception)) {
                if (label.length() > 0) {
                    label.append(", ");
                }
                label.append(DBProperties.getProperty(TaxPerception.class.getName()
                                + "." + TaxPerception.AGENT.toString()));
            }

            if (StringUtils.isNotEmpty(label)) {
                final String targetFieldName = getProperty(_parameter, "FieldName", "contactData");
                js.append("domConstruct.destroy(\"eFapsRD4C\");")
                    .append("query('[name=\\'").append(targetFieldName)
                    .append("\\']').forEach(function (_node) {\n")
                    .append("domConstruct.create(\"span\",")
                    .append("{ id: \"eFapsRD4C\",  title: \"")
                        .append(StringEscapeUtils.escapeEcmaScript(label.toString()))
                    .append("\", innerHTML: \"").append(StringEscapeUtils.escapeEcmaScript(label.toString()))
                        .append("\",")
                    .append(" style: \"color: red; font-size: 12pt; font-weight: bold; margin-left: 10px;\" }")
                    .append(" , _node)")
                    .append("});");
                js = InterfaceUtils.wrapInDojoRequire(_parameter, js, DojoLibs.DOMCONSTRUCT,  DojoLibs.QUERY);
                js = InterfaceUtils.wrappInScriptTag(_parameter, js, false, 100);
            } else {
                js.append("domConstruct.destroy(\"eFapsRD4C\");");
                js = InterfaceUtils.wrapInDojoRequire(_parameter, js, DojoLibs.DOMCONSTRUCT,  DojoLibs.QUERY);
            }
            InterfaceUtils.appendScript4FieldUpdate(_map, js);
        }
    }

    /**
     * Add to QueryBuilder.
     *
     * @param _parameter the _parameter
     * @param _queryBldr the _query bldr
     * @throws EFapsException on error
     */
    protected void add2QueryBldr(final Parameter _parameter,
                                 final QueryBuilder _queryBldr)
        throws EFapsException
    {
        // for a TOKEN AutoComplete add filter to only have one Contact
        if ("TOKEN".equals(getProperty(_parameter, "AutoType"))) {
            final Object uiObject = _parameter.get(ParameterValues.UIOBJECT);
            if (uiObject instanceof Field) {
                final Instance docInst = Instance.get(_parameter.getParameterValue(((Field) uiObject).getName()));
                if (docInst.isValid()) {
                    final PrintQuery print = new PrintQuery(docInst);
                    print.addAttribute(CIERP.DocumentAbstract.Contact);
                    print.execute();
                    final Object contObj = print.getAttribute(CIERP.DocumentAbstract.Contact);
                    if (contObj != null) {
                        _queryBldr.addWhereAttrEqValue(CIERP.DocumentAbstract.Contact, contObj);
                    }
                }
            }
        }
    }

    /**
     * Used by the update event used in the select doc form for ProductRequest.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return map list for update event
     * @throws EFapsException on error
     */

    public Return updateFields4Document(final Parameter _parameter)
        throws EFapsException
    {
        return new Return().put(ReturnValues.VALUES, updateFields4Doc(_parameter));
    }

    /**
     * Update event executed on change of the serial dropdown.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return map list for update event
     * @throws EFapsException on error
     */
    public Return updateFields4Name(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final List<DropDownPosition> options = getSerialNumbers(_parameter);
        if (options.size() > 1) {
            final List<Map<String, Object>> values = new ArrayList<>();
            final Map<String, Object> map = new HashMap<>();
            final Collection<String> types = analyseProperty(_parameter, "Type").values();
            final Field field = (Field) _parameter.get(ParameterValues.UIOBJECT);
            final String fieldName = field.getName() + "_SN";
            String number = getMaxNumber(_parameter, _parameter.getParameterValue(fieldName),
                            types.toArray(new String[types.size()]));
            if (number == null) {
                number = "000001";
            } else {
                // get the numbers after the first "-"
                final Pattern pattern = Pattern.compile("(?<=-)\\d*");
                final Matcher matcher = pattern.matcher(number);
                if (matcher.find()) {
                    final String numTmp = matcher.group();
                    final int length = numTmp.length();
                    final Integer numInt = Integer.parseInt(numTmp) + 1;
                    final NumberFormat nf = NumberFormat.getInstance();
                    nf.setMinimumIntegerDigits(length);
                    nf.setMaximumIntegerDigits(length);
                    nf.setGroupingUsed(false);
                    number = nf.format(numInt);
                }
            }
            map.put(field.getName(), number);
            values.add(map);
            ret.put(ReturnValues.VALUES, values);
        }
        return ret;
    }

    /**
     * Generic method to get the listmap for update event.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return map list for update event
     * @throws EFapsException on error
     */
    protected List<Map<String, Object>> updateFields4Doc(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, Object>> ret = new ArrayList<>();
        final Map<String, Object> map = new HashMap<>();
        final Field field = (Field) _parameter.get(ParameterValues.UIOBJECT);
        final String fieldName = field.getName();

        final String input = containsProperty(_parameter, "input") ? getProperty(_parameter, "input") : "selectedDoc";
        final String infofield = containsProperty(_parameter, "field") ? getProperty(_parameter, "field") : "info";

        // the oid of the document that executed the update event
        final String[] currentOids = _parameter.getParameterValues(fieldName);
        if (currentOids != null && currentOids.length > 0) {
            final List<Instance> instances = new ArrayList<>();
            for (final String oid : currentOids) {
                final Instance inst = Instance.get(oid);
                if (inst.isValid()) {
                    instances.add(inst);
                }
            }
            if (!instances.isEmpty()) {
                final StringBuilder label = new StringBuilder();
                if (instances.size() == 1) {
                    final PrintQuery print = new PrintQuery(instances.get(0));
                    print.addAttribute(CIERP.DocumentAbstract.Name, CIERP.DocumentAbstract.Date);
                    final SelectBuilder sel = SelectBuilder.get().type().label();
                    print.addSelect(sel);
                    print.execute();
                    label.append(print.<Object>getSelect(sel)).append(" - ")
                        .append(print.<Object>getAttribute(CIERP.DocumentAbstract.Name)).append(" - ")
                        .append(print.<DateTime> getAttribute(CIERP.DocumentAbstract.Date).toString(
                                   DateTimeFormat.forStyle("S-").withLocale(Context.getThreadContext().getLocale())));
                    label.append(add2LabelUpdateFields4Doc(_parameter, print.getInstance()));
                }
                final StringBuilder js = new StringBuilder()
                    .append("require([\"dojo/query\", \"dojo/dom-construct\"], function(query, domConstruct) { ")
                    .append("query(\"div  + input[name='").append(input).append("']\").forEach(domConstruct.destroy); ")
                    .append(" query(\"span[name='").append(infofield).append("']\").forEach(function(node){")
                    .append(" domConstruct.empty(node);")
                    .append(" domConstruct.place(\"<span>")
                    .append(StringEscapeUtils.escapeEcmaScript(label.toString()))
                    .append("<span>")
                    .append("<input type=\\\"hidden\\\" ")
                    .append("name=\\\"").append(input).append("\\\" ")
                    .append("value=\\\"").append(fieldName).append("\\\">")
                    .append("\", node, \"last\"); \n")
                    .append("});")
                    .append("});");
                InterfaceUtils.appendScript4FieldUpdate(map, js.toString());
            }
            InterfaceUtils.appendScript4FieldUpdate(map, getCleanJS(_parameter));
            ret.add(map);
        }
        return ret;
    }

    /**
     * Add2 label update fields4 doc.
     *
     * @param _parameter the _parameter
     * @param _instance the _instance
     * @return the string
     * @throws EFapsException the e faps exception
     */
    protected String add2LabelUpdateFields4Doc(final Parameter _parameter,
                                             final Instance _instance)
        throws EFapsException
    {
        // TODO Auto-generated method stub
        return "";
    }

    /**
     * Method to get a small script that cleans out all the field minus the
     * current one.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return script
     * @throws EFapsException on error
     */
    protected String getCleanJS(final Parameter _parameter)
        throws EFapsException
    {
        final StringBuilder js = new StringBuilder();
        final Field currentField = (Field) _parameter.get(ParameterValues.UIOBJECT);
        for (final Field field : currentField.getCollection().getFields()) {
            if (!currentField.equals(field) && field.getEvents(EventType.UI_FIELD_AUTOCOMPLETE) != null) {
                js.append(getSetFieldValue(0, field.getName(), "", ""));
            }
        }
        return js.toString();
    }

    /**
     * Method to get the value for the field directly under the Contact.
     *
     * @param _instance Instacne of the contact
     * @param _escape
     * @return String for the field
     * @throws EFapsException on error
     */
    protected String getFieldValue4Contact(final Instance _instance,  final boolean _escape)
        throws EFapsException
    {
        final Contacts contacts = new Contacts();
        return contacts.getFieldValue4Contact(_instance, _escape);
    }

    /**
     * Method is called from a hidden field to include javascript in the form.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return Return containing the javascript
     * @throws EFapsException on error
     */
    public Return getJavaScriptUIValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final var js1 = getJavaScript4SelectDoc(_parameter);
        final var js2 = getJavaScript4Doc(_parameter);
        final var js3 = getJavaScript4Contact(_parameter);
        retVal.put(ReturnValues.SNIPLETT,
                        InterfaceUtils.wrappInScriptTag(_parameter, js1 + js2 + js3, true, 1500));
        return retVal;
    }

    protected StringBuilder getJavaScript4Contact(final Parameter _parameter)
        throws EFapsException
    {
        final var js = new StringBuilder();
        if (_parameter.getParameterValue("fillContact") != null) {
            final var contactHint = _parameter.getParameterValue("fillContact");
            var contactInst = Instance.get(contactHint);
            if (!InstanceUtils.isKindOf(contactInst, CIContacts.ContactAbstract)) {
                contactInst = getContactInstance4Hint(_parameter, contactHint);
            }
            if (InstanceUtils.isValid(contactInst)) {
                final var eval = EQL.builder().print(contactInst).attribute(CIContacts.ContactAbstract.Name).evaluate();
                if (eval.next()) {
                    final String contactName = eval.get(CIContacts.ContactAbstract.Name);
                    final String contactData = getFieldValue4Contact(contactInst, false);
                    js.append(getSetFieldValue(0, "contact", contactInst.getOid(), contactName)).append("\n")
                                    .append(getSetFieldValue(0, "contactAutoComplete", contactName)).append("\n")
                                    .append(getSetFieldValue(0, "contactData", contactData)).append("\n");
                }
            }
        }
        return js;
    }

    protected Instance getContactInstance4Hint(final Parameter _parameter, final String _contactHint)
        throws EFapsException
    {
        return null;
    }

    /**
     * Get the JavaScript for setting the values on a "Create From" command.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return Return containing the JavaScript
     * @throws EFapsException on error
     */
    protected String getJavaScript4SelectDoc(final Parameter _parameter)
        throws EFapsException
    {
        final StringBuilder js = new StringBuilder()
            .append("var pN = query('.eFapsContentDiv')[0];\n");

        Context.getThreadContext().setSessionAttribute(AbstractDocument_Base.TARGETMODE_DOC_KEY,
                        _parameter.get(ParameterValues.ACCESSMODE));

        boolean copy = _parameter.getParameterValue("selectedRow") != null;
        if (copy || _parameter.getParameterValue("selectedDoc") != null || _parameter.getCallInstance() != null) {
            final Instance instCall = _parameter.getCallInstance();
            final List<Instance> instances = getInstances4Derived(_parameter);
            if (!instances.isEmpty()) {
                // if only one instance is given it might be a copy and not a derived
                if (instances.size() == 1) {
                    final Instance instance = instances.get(0);
                    // in case of copy check if it is really a copy (meaning the same type will be created)
                    final Object object = _parameter.get(ParameterValues.CLASS);
                    if (copy && object instanceof final UIForm uiForm) {
                        final Type type = uiForm.getCommand().getTargetCreateType();
                        if (type != null && !instance.getType().equals(type)) {
                            copy = false;
                        }
                    }
                    js.append("domConstruct.create(\"input\", {\n")
                        .append(" value: \"").append(instance.getOid()).append("\", ")
                        .append(" name: \"").append(copy ? "copy" : "derived").append("\", ")
                        .append(" type: \"hidden\" ")
                        .append("}, pN);\n");
                } else  {
                    for (final Instance instance : instances) {
                        js.append("domConstruct.create(\"input\", {")
                            .append(" value: \"").append(instance.getOid()).append("\", ")
                            .append(" name: \"derived\", ")
                            .append(" type: \"hidden\" ")
                            .append("}, pN);\n");
                    }
                }
                js.append(getJavaScript4Document(_parameter, instances))
                    .append(instances.size() == 1 ?  getJavaScript4Positions(_parameter, instances.get(0))
                                    : getJavaScript4Positions(_parameter, instances))
                    .append(addDomReadyScript(_parameter, instances));
            } else if (instCall != null && instCall.isValid()
                            && instCall.getType().isKindOf(CISales.DocumentAbstract.getType())) {
                final List<Instance> instCallLst = new ArrayList<>();
                instCallLst.add(instCall);
                js.append("domConstruct.create(\"input\", {")
                    .append(" value: \"").append(instCall.getOid()).append("\", ")
                    .append(" name: \"derived\", ")
                    .append(" type: \"hidden\" ")
                    .append("}, pN);\n");
                // if no instance was detected to copy from set the default Currency
                js.append(updateRateFields(_parameter, null)).append("\n")
                    .append(getJavaScript4Document(_parameter, instCallLst))
                    .append(add2JavaScript4Document(_parameter, instCallLst))
                    .append(getJavaScript4Positions(_parameter, instCall));
            }
        }
        return InterfaceUtils.wrapInDojoRequire(_parameter, js, DojoLibs.QUERY, DojoLibs.DOM,
                        DojoLibs.DOMCONSTRUCT, DojoLibs.NUMBER).toString();
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return script
     * @throws EFapsException on error
     */
    public Return getJavaScript4Search(final Parameter _parameter)
        throws EFapsException
    {
        final Object uiObject = _parameter.get(ParameterValues.CLASS);
        Status status = null;
        String selContact = null;
        if (uiObject instanceof UIForm) {
            final AbstractCommand cmd = ((UIForm) uiObject).getCallingCommand();
            if (cmd != null) {
                final String statusGrp = getProperty(_parameter, "StatusGroup");
                if (containsProperty(_parameter, "Status4SearchKey")) {
                    final String statusStr = cmd.getProperty(getProperty(_parameter, "Status4SearchKey"));
                    if (statusGrp != null && statusStr != null) {
                        if (isUUID(statusGrp)) {
                            status = Status.find(UUID.fromString(statusGrp), statusStr);
                        } else {
                            status = Status.find(statusGrp, statusStr);
                        }
                    }
                }
                selContact = cmd.getProperty("Select4SearchContact");
            }
        }

        final Instance instance = _parameter.getInstance() != null
                        ? _parameter.getInstance() : _parameter.getCallInstance();
        final StringBuilder js = new StringBuilder();
        if (InstanceUtils.isValid(instance)
                        && (instance.getType().isKindOf(CIERP.DocumentAbstract) || selContact != null)) {
            final SelectBuilder selContactId = selContact == null
                            ? new SelectBuilder().linkto(CISales.DocumentSumAbstract.Contact).id()
                            : new SelectBuilder(selContact).id();
            final SelectBuilder selContactName = selContact == null
                    ? new SelectBuilder().linkto(CISales.DocumentSumAbstract.Contact).attribute(CIContacts.Contact.Name)
                    : new SelectBuilder(selContact).attribute(CIContacts.Contact.Name) ;
            final PrintQuery print = new PrintQuery(instance);
            print.addSelect(selContactId, selContactName);
            print.execute();

            final Long contactId = print.<Long>getSelect(selContactId);
            final String contactName = print.<String>getSelect(selContactName);

            js.append(getSetFieldValue(0, "contact", String.valueOf(contactId), contactName)).append("\n");
        }

        if (status != null) {
            js.append(getSetFieldValue(0, "status", Long.valueOf(status.getId()).toString()));
        }

        final Return retVal = new Return();
        retVal.put(ReturnValues.SNIPLETT, InterfaceUtils.wrappInScriptTag(_parameter, js, true, 1500));
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return instance of the currency
     * @throws EFapsException on error
     */
    protected Instance evaluateCurrency4JavaScript(final Parameter _parameter)
        throws EFapsException
    {
        return evaluateCurrencyInstance(_parameter);
    }

    /**
     * Add additional on Dom Ready JavaScript for
     * setting the values on a "Create From" command.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _instances list of instance the values are copied from
     * @return Return containing the JavaScript
     * @throws EFapsException on error
     */
    protected StringBuilder addDomReadyScript(final Parameter _parameter,
                                              final List<Instance> _instances)
        throws EFapsException
    {
        return new StringBuilder();
    }

    /**
     * JavaScript part for setting the Document Head.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _instances list of instance the values are copied from
     * @return Return containing the JavaScript
     * @throws EFapsException on error
     */
    protected StringBuilder getJavaScript4Document(final Parameter _parameter,
                                                   final List<Instance> _instances)
        throws EFapsException
    {
        final StringBuilder js = new StringBuilder();
        // as a default
        final Instance docInst = _instances.get(0);
        final PrintQuery print = new PrintQuery(docInst);
        print.addAttribute(CISales.DocumentSumAbstract.RateNetTotal,
                        CISales.DocumentSumAbstract.RateCrossTotal,
                        CISales.DocumentSumAbstract.Rate,
                        CISales.DocumentSumAbstract.RateTaxes,
                        CIERP.DocumentAbstract.Name,
                        CIERP.DocumentAbstract.Note,
                        CIERP.DocumentAbstract.Remark,
                        CIERP.DocumentAbstract.Date);
        final SelectBuilder selContInst = new SelectBuilder().linkto(CIERP.DocumentAbstract.Contact).instance();
        print.addSelect(selContInst);
        print.execute();

        final BigDecimal netTotal = print.getAttribute(CISales.DocumentSumAbstract.RateNetTotal);
        final BigDecimal crossTotal = print.getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
        final Taxes rateTaxes = print.getAttribute(CISales.DocumentSumAbstract.RateTaxes);

        if (netTotal != null && rateTaxes == null || rateTaxes != null && rateTaxes.getEntries().isEmpty()) {
            js.append(getSetFieldValue(0, "withoutVAT", "true")).append("\n");
        }

        final Instance contactInstance = print.getSelect(selContInst);
        final String note = print.getAttribute(CIERP.DocumentAbstract.Note);
        final String remark = print.getAttribute(CIERP.DocumentAbstract.Remark);
        final String name = print.getAttribute(CIERP.DocumentAbstract.Name);

        final DecimalFormat formater = NumberFormatter.get().getTwoDigitsFormatter();

        js.append(updateRateFields(_parameter, docInst))
            .append(add2JavaScript4DocumentContact(_parameter, _instances, contactInstance));

        if ("true".equalsIgnoreCase(_parameter.getParameterValue(AbstractDocument_Base.SELDOCUPDATEPF + "CopyName"))) {
            js.append(getSetFieldValue(0, "name4create", name)).append("\n");
        }

        final Map<Integer, String> copyDates = analyseProperty(_parameter, "CopyDate4Type");
        if (copyDates.containsValue(docInst.getType().getName())) {
            js.append(getSetFieldValue(0, "date_eFapsDate",
                            print.<DateTime>getAttribute(CIERP.DocumentAbstract.Date).toString()).append("\n"));
        }
        js.append(getSetFieldValue(0, "netTotal", netTotal == null
                            ? BigDecimal.ZERO.toString() : formater.format(netTotal))).append("\n")
            .append(getSetFieldValue(0, "crossTotal", netTotal == null
                            ? BigDecimal.ZERO.toString() : formater.format(crossTotal))).append("\n");
        if (rateTaxes != null) {
            js.append("if (document.getElementsByName('taxes')[0]) {\n")
                .append("document.getElementsByName('taxes')[0].innerHTML='")
                .append(new TaxesAttribute().getUI4ReadOnly(rateTaxes)).append("';\n")
                .append("}\n");
        }
        js.append(getSetFieldValue(0, "note", note)).append("\n")
            .append(getSetFieldValue(0, "remark", remark)).append("\n")
            .append(add2JavaScript4Document(_parameter, _instances)).append("\n")
            .append("\n");
        return js;
    }

    /**
     * Add JavaScript part to the Document Head.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _instances list of instance the values are copied from
     * @param _contactInstance instance of the contact
     * @return Return containing the JavaScript
     * @throws EFapsException on error
     */
    protected StringBuilder add2JavaScript4DocumentContact(final Parameter _parameter,
                                                           final List<Instance> _instances,
                                                           final Instance _contactInstance)
        throws EFapsException
    {
        final StringBuilder ret = new StringBuilder();
        if (isContact2JavaScript4Document(_parameter, _instances) && _contactInstance != null
                        && _contactInstance.isValid()) {
            final PrintQuery print = new PrintQuery(_contactInstance);
            print.addAttribute(CIContacts.ContactAbstract.Name);
            print.execute();
            final String contactName = print.getAttribute(CIContacts.ContactAbstract.Name);
            final String contactData = getFieldValue4Contact(_contactInstance, false);
            ret.append(getSetFieldValue(0, "contact", _contactInstance.getOid(), contactName)).append("\n")
                            .append(getSetFieldValue(0, "contactAutoComplete", contactName)).append("\n")
                            .append(getSetFieldValue(0, "contactData", contactData)).append("\n");

            final Map<String, Object> map = new HashMap<>();
            new Contacts()
            {

                @Override
                public void add2UpdateMap4Contact(final Parameter _parameter,
                                                  final Instance _contactInstance,
                                                  final Map<String, Object> _map)
                    throws EFapsException
                {
                    super.add2UpdateMap4Contact(_parameter, _contactInstance, _map);
                }
            }.add2UpdateMap4Contact(_parameter, _contactInstance, map);
            if (!map.isEmpty()) {
                final List<Map<String, Object>> values = new ArrayList<>();
                values.add(map);
                ret.append(getSetFieldValuesScript(_parameter, values, null));
            }
        }
        return ret;
    }

    /**
     * Must the contact information be added to the JavaScript.
     * @param _parameter Parameter as passed from the eFaps API
     * @param _instances list of instance the values are copied from
     * @return true if it should be added
     * @throws EFapsException on error
     */
    protected boolean isContact2JavaScript4Document(final Parameter _parameter,
                                                    final List<Instance> _instances)
        throws EFapsException
    {
        return true;
    }

    /**
     * Add JavaScript part to  the Document Head.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _instances list of instance the values are copied from
     * @return Return containing the JavaScript
     * @throws EFapsException on error
     */
    protected StringBuilder add2JavaScript4Document(final Parameter _parameter,
                                                    final List<Instance> _instances)
        throws EFapsException
    {
        final StringBuilder ret = new StringBuilder();
        for (final IOnCreateFromDocument listener : Listener.get().<IOnCreateFromDocument>invoke(
                        IOnCreateFromDocument.class)) {
            ret.append(listener.add2JavaScript4Document(_parameter, _instances));
        }
        return ret;
    }

    /**
     * JavaScript part for setting the Positions.<br/>
     * <ol>
     * <li>Get the data and fill a <code>Map&lt;String,Object&gt;</code> with the values</li>
     * <li>Substract/add data by evaluating related document</li>
     * <li>Give the chance to correct/manipulate the original data</li>
     * <li>Convert the <code>Map&lt;String,Object&gt;</code> into a <code>Map&lt;String,String&gt;</code></li>
     * <li>Generate the JavaScript for the data</li>
     * </ol>
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _instance instance the values are copied from
     * @return Return containing the JavaScript
     * @throws EFapsException on error
     */
    protected StringBuilder getJavaScript4Positions(final Parameter _parameter,
                                                    final Instance _instance)
        throws EFapsException
    {
        final StringBuilder js = new StringBuilder();

        final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionAbstract);
        queryBldr.addWhereAttrEqValue(CISales.PositionAbstract.DocumentAbstractLink, _instance);
        queryBldr.addOrderByAttributeAsc(CISales.PositionAbstract.PositionNumber);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.PositionAbstract.PositionNumber,
                        CISales.PositionAbstract.ProductDesc,
                        CISales.PositionAbstract.Quantity,
                        CISales.PositionAbstract.UoM,
                        CISales.PositionSumAbstract.RateNetUnitPrice,
                        CISales.PositionSumAbstract.RateDiscountNetUnitPrice,
                        CISales.PositionSumAbstract.RateNetPrice,
                        CISales.PositionSumAbstract.RateCrossPrice,
                        CISales.PositionSumAbstract.Tax,
                        CISales.PositionSumAbstract.Discount,
                        CISales.PositionProdDocAbstract.StorageLink);
        final SelectBuilder selProdInst = new SelectBuilder().linkto(CISales.PositionSumAbstract.Product).instance();
        final SelectBuilder selProdName = new SelectBuilder().linkto(CISales.PositionSumAbstract.Product)
                        .attribute(CIProducts.ProductAbstract.Name);
        final SelectBuilder selProdDim = new SelectBuilder().linkto(CISales.PositionSumAbstract.Product)
                        .attribute(CIProducts.ProductAbstract.Dimension);
        multi.addSelect(selProdInst, selProdName, selProdDim);
        multi.setEnforceSorted(true);
        multi.execute();

        final List<AbstractUIPosition> values = new ArrayList<>();
        while (multi.next()) {
            final Instance prodInstance = multi.getSelect(selProdInst);
            final BigDecimal quantity = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.Quantity);
            final AbstractUIPosition origBean = getUIPosition(_parameter)
                            .setInstance(multi.getCurrentInstance())
                            .setProdInstance(prodInstance)
                            .setQuantity(quantity)
                            .setUoM(multi.<Long>getAttribute(CISales.PositionAbstract.UoM))
                            .setProdName(multi.<String>getSelect(selProdName))
                            .setProdDescr(multi.<String>getAttribute(CISales.PositionAbstract.ProductDesc));

            // set the storage Instance
            if (multi.getCurrentInstance().getType().isKindOf(CISales.PositionProdDocAbstract)) {
                final QueryBuilder storageQueryBldr = new QueryBuilder(CIProducts.DynamicStorage);
                storageQueryBldr.addWhereAttrEqValue(CIProducts.DynamicStorage.ID,
                                multi.<Long>getAttribute(CISales.PositionProdDocAbstract.StorageLink));
                final CachedInstanceQuery query = storageQueryBldr.getCachedQuery4Request();
                query.execute();
                if (query.next()) {
                    origBean.setStorageInst(query.getCurrentValue());
                }
            }

            final List<AbstractUIPosition> beans = updateBean4Indiviual(_parameter, origBean);
            AbstractDocument_Base.LOG.debug("Updated bean {} with {}", origBean, beans);
            for (final AbstractUIPosition bean : beans) {
                if (multi.getCurrentInstance().getType().isKindOf(CISales.PositionSumAbstract)) {
                    bean.setNetUnitPrice(multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.RateNetUnitPrice))
                        .setDiscountNetUnitPrice(multi.<BigDecimal>getAttribute(
                                        CISales.PositionSumAbstract.RateDiscountNetUnitPrice))
                        .setNetPrice(multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.RateNetPrice))
                        .setCrossPrice(multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.RateCrossPrice))
                        .setDiscount(multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.Discount));
                } else if (getCIType().getType().isKindOf(CISales.DocumentSumAbstract)) {
                    final Calculator calc = getCalculator(_parameter, null, bean.getProdInstance(),
                                    bean.getQuantity(), BigDecimal.ZERO, BigDecimal.ZERO, true, 0);
                    bean.setNetUnitPrice(calc.getNetUnitPrice())
                        .setDiscountNetUnitPrice(calc.getNetUnitPrice())
                        .setNetPrice(calc.getNetPrice())
                        .setCrossPrice(calc.getCrossPrice());
                }
                values.add(bean);
            }
        }

        final Set<String> noEscape = new HashSet<>();
        noEscape.add("uoM");

        updateBeans4Partial(_parameter, values, _instance);

        add2JavaScript4Postions(_parameter, values, _instance);

        final List<Map<String, Object>> strValues = convertMap4Script(_parameter, values);

        if (TargetMode.EDIT.equals(Context.getThreadContext()
                        .getSessionAttribute(AbstractDocument_Base.TARGETMODE_DOC_KEY))) {
            js.append(getSetFieldValuesScript(_parameter, strValues, noEscape));
        } else {
            js.append(getTableRemoveScript(_parameter, "positionTable", false, false))
                            .append(getTableAddNewRowsScript(_parameter, "positionTable", strValues,
                                        getOnCompleteScript(_parameter, values, _instance), false, false, noEscape));
        }
        js.append("\n");
        return js;
    }

    /**
     * Give the chance to replace the product with others. e.g Original Product with Batch.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _currentBean current bean to be updated
     * @return List of beans
     * @throws EFapsException on error
     */
    protected List<AbstractUIPosition> updateBean4Indiviual(final Parameter _parameter,
                                                            final AbstractUIPosition _currentBean)
        throws EFapsException
    {
        final List<AbstractUIPosition> ret = new ArrayList<>();
        ret.add(_currentBean);
        return ret;
    }

    /**
     * Gets the UI position.
     *
     * @param _parameter the _parameter
     * @return the UI position
     */
    protected AbstractUIPosition getUIPosition(final Parameter _parameter)
    {
        return new AbstractUIPosition(_parameter, this)
        {
            private static final long serialVersionUID = 1L;
        };
    }

    /**
     * @param _parameter Paramert as passed by the eFaps API
     * @param _values   values
     * @return converted map
     * @throws EFapsException on error
     */
    protected List<Map<String, Object>> convertMap4Script(final Parameter _parameter,
                                                          final Collection<AbstractUIPosition> _values)
        throws EFapsException
    {
        final List<Map<String, Object>> ret = new ArrayList<>();
        for (final AbstractUIPosition uiPosition :_values) {
            ret.add(uiPosition.getMap4JavaScript(_parameter));
        }
        return ret;
    }

    /**
     * Delete the positions of a Document that were removed in the UserInterface.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _editDoc  EditDoc the postions that will be updated belong to
     * @throws EFapsException on error
     */
    protected void deletePosition4Update(final Parameter _parameter,
                                         final EditedDoc _editDoc)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(getType4PositionUpdate(_parameter));
        queryBldr.addWhereAttrEqValue(CISales.PositionAbstract.DocumentAbstractLink, _editDoc.getInstance());
        final InstanceQuery query = queryBldr.getQuery();
        query.execute();
        final Set<Instance> delIns = new HashSet<>();
        while (query.next()) {
            final Instance inst = query.getCurrentValue();
            if (!_editDoc.getPositions().contains(inst)) {
                delIns.add(inst);
            }
        }
        for (final Instance inst : delIns) {
            final Delete delete = new Delete(inst);
            delete.execute();
        }
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _calc Calculator
     * @param _posUpdate Update
     * @param _idx index
     * @throws EFapsException on error
     */
    protected void add2PositionUpdate(final Parameter _parameter,
                                      final Calculator _calc,
                                      final Update _posUpdate,
                                      final int _idx)
        throws EFapsException
    {
        _posUpdate.add(CISales.PositionAbstract.PositionNumber, _idx + 1);
    }

    /**
     * Adds the two java script for postions.
     *
     * @param _parameter Paramter as passed by the eFaps API
     * @param _values values to be added to
     * @param _instances the instances
     * @throws EFapsException on error
     */
    protected void add2JavaScript4Postions(final Parameter _parameter,
                                           final Collection<AbstractUIPosition> _values,
                                           final Instance... _instances)
        throws EFapsException
    {
        // to be used by implementations
    }


    /**
     * JavaScript part for setting the Positions.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _instances list of instances the values are copied from
     * @return Return containing the JavaScript
     * @throws EFapsException on error
     */
    protected StringBuilder getJavaScript4Positions(final Parameter _parameter,
                                                    final List<Instance> _instances)
        throws EFapsException
    {
        final StringBuilder js = new StringBuilder();
        final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionAbstract);
        queryBldr.addWhereAttrEqValue(CISales.PositionAbstract.DocumentAbstractLink, _instances.toArray());
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.PositionAbstract.PositionNumber,
                        CISales.PositionAbstract.ProductDesc,
                        CISales.PositionAbstract.Quantity,
                        CISales.PositionAbstract.UoM);
        final SelectBuilder selProdInst = new SelectBuilder().linkto(CISales.PositionSumAbstract.Product).instance();
        final SelectBuilder selProdName = new SelectBuilder().linkto(CISales.PositionSumAbstract.Product)
                        .attribute(CIProducts.ProductAbstract.Name);
        final SelectBuilder selProdDim = new SelectBuilder().linkto(CISales.PositionSumAbstract.Product)
                        .attribute(CIProducts.ProductAbstract.Dimension);
        multi.addSelect(selProdInst, selProdName, selProdDim);
        multi.execute();

        final Map<Instance, AbstractUIPosition> valuesTmp = new LinkedHashMap<>();
        while (multi.next()) {
            final Instance prodInst = multi.<Instance>getSelect(selProdInst);
            if (valuesTmp.containsKey(prodInst)) {
                final AbstractUIPosition origBean  = valuesTmp.get(prodInst);
                origBean.setQuantity(origBean.getQuantity().add(
                                multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.Quantity)));
            } else {
                final Instance prodInstance = multi.getSelect(selProdInst);
                final BigDecimal quantity = multi.<BigDecimal>getAttribute(CISales.PositionSumAbstract.Quantity);
                final AbstractUIPosition origBean = getUIPosition(_parameter)
                                .setInstance(multi.getCurrentInstance())
                                .setProdInstance(prodInstance)
                                .setQuantity(quantity)
                                .setUoM(multi.<Long>getAttribute(CISales.PositionAbstract.UoM))
                                .setProdName(multi.<String>getSelect(selProdName))
                                .setProdDescr(multi.<String>getAttribute(CISales.PositionAbstract.ProductDesc));
                for (final AbstractUIPosition bean : updateBean4Indiviual(_parameter, origBean)) {
                    if (valuesTmp.containsKey(bean.getProdInstance())) {
                        final AbstractUIPosition origBean4Prod  = valuesTmp.get(prodInst);
                        origBean4Prod.setQuantity(origBean4Prod.getQuantity().add(bean.getQuantity()));
                    } else {
                        valuesTmp.put(bean.getProdInstance(), bean);
                    }
                }
            }
        }
        final Collection<AbstractUIPosition> values = valuesTmp.values();

        updateBeans4Partial(_parameter, values, _instances.toArray(new Instance[_instances.size()]));

        add2JavaScript4Postions(_parameter, values, _instances.toArray(new Instance[_instances.size()]));

        final List<Map<String, Object>> strValues = convertMap4Script(_parameter, values);

        Collections.sort(strValues, (_o1, _o2) -> String.valueOf(_o1.get("productAutoComplete"))
                        .compareTo(String.valueOf(_o2.get("productAutoComplete"))));
        final Set<String> noEscape = new HashSet<>();
        noEscape.add("uoM");

        if (TargetMode.EDIT.equals(Context.getThreadContext()
                        .getSessionAttribute(AbstractDocument_Base.TARGETMODE_DOC_KEY))) {
            js.append(getSetFieldValuesScript(_parameter, strValues, noEscape));
        } else {
            js.append(getTableRemoveScript(_parameter, "positionTable", false, false))
                .append(getTableAddNewRowsScript(_parameter, "positionTable", strValues,
                            getOnCompleteScript(_parameter, values,
                                        _instances.toArray(new Instance[_instances.size()])), false, false, noEscape));
        }
        js.append("\n");
        return js;
    }

    /**
     * Update beans for partial.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _values the values
     * @param _instances the instances
     * @throws EFapsException on error
     */
    protected void updateBeans4Partial(final Parameter _parameter,
                                       final Collection<AbstractUIPosition> _values,
                                       final Instance... _instances)
        throws EFapsException
    {
        final Properties confProps = PropertiesUtil.getProperties4Prefix(Sales.PARTIALCONFIG.get(), getTypeName4SysConf(
                        _parameter), true);
        if (!confProps.isEmpty()) {
            for (final Instance instance : _instances) {
                final Properties props = PropertiesUtil.getProperties4Prefix(confProps, instance.getType().getName(),
                                true);
                if (!props.isEmpty()) {
                    final String relType = props.getProperty("RelationType");
                    final Properties relProps = PropertiesUtil.getProperties4Prefix(Sales.PARTIALCONFIG.get(),
                                    relType, true);
                    final String relOriginLink = relProps.getProperty("RelationOriginLink");
                    final String relPartialLink = relProps.getProperty("RelationPartialLink");

                    final QueryBuilder relQueryBldr = new QueryBuilder(Type.get(relType));
                    relQueryBldr.addWhereAttrEqValue(relOriginLink, instance);

                    final QueryBuilder posQueryBldr = new QueryBuilder(CISales.PositionAbstract);
                    posQueryBldr.addWhereAttrInQuery(CISales.PositionAbstract.DocumentAbstractLink, relQueryBldr
                                    .getAttributeQuery(relPartialLink));
                    final MultiPrintQuery posMulti = posQueryBldr.getPrint();
                    final SelectBuilder selProdInst = SelectBuilder.get().linkto(CISales.PositionAbstract.Product)
                                    .instance();
                    posMulti.addSelect(selProdInst);
                    posMulti.addAttribute(CISales.PositionAbstract.Quantity);
                    posMulti.execute();
                    while (posMulti.next()) {
                        final Instance prodInst = posMulti.getSelect(selProdInst);
                        BigDecimal quantity = posMulti.getAttribute(CISales.PositionAbstract.Quantity);
                        final Set<AbstractUIPosition> toBeRemoved = new HashSet<>();
                        for (final AbstractUIPosition origBean : _values) {
                            if (origBean.getProdInstance().equals(prodInst)) {
                                origBean.setQuantity(origBean.getQuantity().subtract(quantity));
                                if (origBean.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
                                    break;
                                } else if (origBean.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                                    toBeRemoved.add(origBean);
                                    break;
                                } else {
                                    quantity = origBean.getQuantity().negate();
                                    toBeRemoved.add(origBean);
                                }
                            }
                        }
                        _values.removeAll(toBeRemoved);
                    }
                }
            }
        }
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return the Instance used for the derived on the javascript field. returns
     */
    protected List<Instance> getInstances4Derived(final Parameter _parameter)
    {
        final List<Instance> ret = new ArrayList<>();
        final String[] oids;
        if (_parameter.getParameterValues("selectedRow") != null) {
            oids = _parameter.getParameterValues("selectedRow");
        } else {
            final String selectedDoc = _parameter.getParameterValue("selectedDoc");
            oids = _parameter.getParameterValues(selectedDoc);
        }
        if (oids != null) {
            for (final String oid : oids) {
                final Instance instance = Instance.get(oid);
                if (instance.isValid() && instance.getType().isKindOf(CISales.DocumentAbstract)) {
                    ret.add(instance);
                }
            }
        }
        return ret;
    }

    /**
     * Update rate fields.
     *
     * @param _parameter the _parameter
     * @param _docInst the _doc inst
     * @return the string
     * @throws EFapsException the efaps exception
     */
    protected String updateRateFields(final Parameter _parameter,
                                      final Instance _docInst)
        throws EFapsException
    {
        // if no document is given set the currencies
        Instance docInst = _docInst;
        if (docInst == null && TargetMode.EDIT.equals(_parameter.get(ParameterValues.ACCESSMODE))) {
            docInst = _parameter.getInstance();
        }
        return updateRateFields(_parameter, docInst,
                        evaluateCurrency4JavaScript(_parameter), Currency.getBaseCurrency());
    }

    /**
     * Update rate fields.
     *
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _docInst the _doc inst
     * @param _targetCurrencyInst the _target currency inst
     * @param _currentCurrencyInst the _current currency inst
     * @return html for field
     * @throws EFapsException on error
     */
    protected String updateRateFields(final Parameter _parameter,
                                      final Instance _docInst,
                                      final Instance _targetCurrencyInst,
                                      final Instance _currentCurrencyInst)
        throws EFapsException
    {
        final StringBuilder js = new StringBuilder();

        RateInfo rateInfo = null;
        CurrencyInst currencyInst = null;
        // in edit mode try to use the rate of the document
        if (_docInst != null && _docInst.isValid()
                        && _docInst.getType().isKindOf(CISales.DocumentSumAbstract.getType())) {
            final PrintQuery print = new PrintQuery(_docInst);
            print.addAttribute(CISales.DocumentSumAbstract.Rate);
            print.executeWithoutAccessCheck();
            final Object rate = print.getAttribute(CISales.DocumentSumAbstract.Rate);
            if (rate != null && rate instanceof Object[]) {
                final Currency currency = new Currency();
                rateInfo  = currency.evaluateRateInfo(_parameter, (Object[]) rate);
                currencyInst = rateInfo.getCurrencyInstObj();
            }
        }

        // if no rate yet try to set it
        if (rateInfo == null) {
            final Currency currency = new Currency();
            final String date = _parameter.getParameterValue("date_eFapsDate");
            final RateInfo[] rates = currency.evaluateRateInfos(_parameter, date, _currentCurrencyInst,
                            _targetCurrencyInst);
            rateInfo = rates[2];
            currencyInst = rates[1].getCurrencyInstObj();
        }
        if (rateInfo != null) {
            js.append(getSetFieldValue(0, "rateCurrencyData", getRateUIFrmt(_parameter, rateInfo))).append("\n")
                .append(getSetFieldValue(0, "rate", NumberFormatter.get().getFormatter().format(
                                getRateUI(_parameter, rateInfo)))).append("\n")
                .append(getSetFieldValue(0, "rate" + RateUI.INVERTEDSUFFIX, "" + currencyInst.isInvert())).append("\n")
                .append(getSetFieldValue(0, "rateCurrencyId", String.valueOf(currencyInst.getInstance().getId())))
                    .append("\n");
        }
        return js.toString();
    }

    /**
     * Method for acon complete script.
     *
     * @param _parameter Paramter as passed by the eFaps API
     * @param _values the values
     * @param _docInsts the doc insts
     * @return new StringBuilder with the additional fields.
     * @throws EFapsException on error
     */
    protected StringBuilder getOnCompleteScript(final Parameter _parameter,
                                                final Collection<AbstractUIPosition> _values,
                                                final Instance... _docInsts)
        throws EFapsException
    {
        return new StringBuilder();
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _map          Map to add to
     * @param _prodInst     Instance of the Product
     * @throws EFapsException on error
     */
    protected void add2UpdateField4Product(final Parameter _parameter,
                                           final Map<String, Object> _map,
                                           final Instance _prodInst)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_prodInst);
        print.addAttribute(CIProducts.ProductAbstract.Name, CIProducts.ProductAbstract.Description,
                        CIProducts.ProductAbstract.Dimension,
                        CIProducts.ProductAbstract.DefaultUoM,
                        CIProducts.ProductAbstract.Individual);
        if (print.execute()) {
            final String name = print.getAttribute(CIProducts.ProductAbstract.Name);
            final String desc = print.<String>getAttribute(CIProducts.ProductAbstract.Description);
            final Long dimId = print.<Long>getAttribute(CIProducts.ProductAbstract.Dimension);
            final Long dUoMId = print.<Long>getAttribute(CIProducts.ProductAbstract.DefaultUoM);
            final long selectedUoM;
            if (dUoMId == null) {
                selectedUoM = Dimension.get(dimId).getBaseUoM().getId();
            } else if (Dimension.getUoM(dUoMId).getDimension().equals(Dimension.get(dimId))) {
                selectedUoM = dUoMId;
            } else {
                selectedUoM = Dimension.get(dimId).getBaseUoM().getId();
            }
            _map.put("uoM", getUoMFieldStr(selectedUoM, dimId));
            _map.put("productDesc", StringEscapeUtils.escapeEcmaScript(desc));

            if (Products.ACTIVATEINDIVIDUAL.get()) {
                add4Individual(_parameter, _prodInst,
                                print.<ProductIndividual>getAttribute(CIProducts.ProductAbstract.Individual),
                                _map, _prodInst.getOid(), name +  "-" + desc);
            }
        }
    }

    /**
     * Add for individual.
     *
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _prodInst     instance of the product the new individuals will belong to
     * @param _individual   value of the individual attribute
     * @param _map          map the script will be added to
     * @param _key          key to be used as fieldname etc
     * @param _legend       legend to be presented
     * @return the string builder
     * @throws EFapsException on error
     */
    public StringBuilder add4Individual(final Parameter _parameter,
                                        final Instance _prodInst,
                                        final ProductIndividual _individual,
                                        final Map<String, Object> _map,
                                        final String _key,
                                        final String _legend)
        throws EFapsException
    {
        final StringBuilder js = new StringBuilder();
        if (_individual != null && !ProductIndividual.NONE.equals(_individual)) {
            // TODO make configurable from properties
            final String fieldName = "individual";
            final String qfieldName = "quantity";
            final int quantity;
            switch (_individual) {
                // only for individual it is necessary to now the quantity
                case INDIVIDUAL:
                    final String quantityStr = _parameter.getParameterValues(qfieldName)[getSelectedRow(_parameter)];
                    if (quantityStr != null && !quantityStr.isEmpty()) {
                        quantity = Integer.parseInt(quantityStr);
                    } else {
                        quantity = 1;
                    }
                    break;
                default:
                    quantity = 1;
                    break;
            }
            js.append("var ind = query(\"[name='").append(fieldName).append("']\")[0];")
                .append("if (typeof(ind)!=='undefined') {\n");

            if (_individual.equals(ProductIndividual.INDIVIDUAL)) {
                js.append("var ins = dom.byId(\"in").append(_key).append("\");\n")
                    .append("var fs;\n")
                    .append("if (ins === null) {\n")
                    .append("ins = domConstruct.create(\"span\", { id: \"in").append(_key)
                        .append("\", class: \"eFapsIndividual\"}, ind);\n")
                    .append("var fs =  domConstruct.create(\"fieldset\", {}, ins);\n")
                    .append("domConstruct.create(\"legend\", { innerHTML: \"")
                        .append(StringEscapeUtils.escapeEcmaScript(_legend)).append("\"}, fs);\n")
                    .append("} else {\n")
                    .append("fs = query('fieldset', ins) [0];\n")
                    .append("}\n")
                    .append("var j = 1;\n")
                    .append("for (var i=1;i < ").append(quantity + 1).append("; i++) {\n")
                    .append("var sps = query('span', fs);\n")
                    .append("if (sps.length < i) {\n")
                    .append("var sp = domConstruct.create(\"span\", {}, fs);\n")
                    .append("domConstruct.create(\"label\",{ innerHTML: ")
                        .append("number.format(i, {pattern:'00'}) +\".\"}, sp);\n")
                    .append(" domConstruct.create(\"input\", { name: \"").append(_key).append("\"}, sp);\n")
                    .append("if (j==5) {\n")
                    .append("j=0;\n")
                    .append("domConstruct.create(\"br\", {}, sp);\n")
                    .append("}\n")
                    .append("}\n")
                    .append("j++;\n")
                    .append("}\n")
                    .append("var sps = query('span', fs).forEach(function (node, i) {\n")
                    .append("if (i + 1 > ").append(quantity).append(") {\n")
                    .append("domConstruct.destroy(node);\n")
                    .append("}\n")
                    .append("});\n");

                // remove the ones not any more necessary
                js.append("query('.eFapsIndividual input:first-of-type').forEach(function (node) {\n")
                    .append("var ind = query(\"input[value='\" + node.name + \"']\");\n")
                    .append("if (ind.length === 0) {\n")
                    .append("domConstruct.destroy(\"in\" + node.name);\n")
                    .append("}\n")
                    .append("});\n");

            } else if (_individual.equals(ProductIndividual.BATCH)) {
                final String id = RandomStringUtils.randomAlphabetic(8);
                js.append("domConstruct.destroy(\"in").append(_key).append("\");")
                    .append(" var x = domConstruct.create(\"span\", { id: \"in").append(_key)
                    .append("\", class: \"eFapsIndividual\"}, ind);")
                    .append(" var fs=  domConstruct.create(\"fieldset\", { }, x);")
                    .append("domConstruct.create(\"legend\", { innerHTML: \"")
                    .append(StringEscapeUtils.escapeEcmaScript(_legend)).append("\"}, fs);")
                    .append(" domConstruct.create(\"input\", { name: \"").append(_key)
                    .append("\" , checked: \"checked\", type:\"radio\", value: \"").append(ProductIndividual.BATCH)
                    .append("\", id:\"").append(id).append("\"}, fs);")
                    .append(" domConstruct.create(\"label\", { innerHTML: \"")
                    .append(DBProperties.getProperty(AbstractDocument.class.getName() + ".CreateNewBatch"))
                    .append("\", for:\"").append(id).append("\"}, fs);");
                final List<Instance> batchInsts = new Batch().getExistingBatch4ProductInst(_parameter, _prodInst);
                if (!batchInsts.isEmpty()) {
                    final MultiPrintQuery multi = new MultiPrintQuery(batchInsts);
                    multi.addAttribute(CIProducts.ProductAbstract.Name);
                    multi.executeWithoutAccessCheck();
                    while (multi.next()) {
                        final String name = multi.getAttribute(CIProducts.ProductAbstract.Name);
                        final String id2 = RandomStringUtils.randomAlphabetic(8);
                        js.append(" domConstruct.create(\"input\", { name: \"").append(_key)
                            .append("\" , type:\"radio\", value: \"").append(multi.getCurrentInstance().getOid())
                            .append("\", id:\"").append(id2).append("\"}, fs);")
                            .append(" domConstruct.create(\"label\", { innerHTML: \"")
                            .append(StringEscapeUtils.escapeEcmaScript(name)).append("\", for:\"")
                            .append(id2).append("\"}, fs);");
                    }
                }
            }
            js.append("}");
            if (_map != null) {
                InterfaceUtils.appendScript4FieldUpdate(_map, InterfaceUtils.wrapInDojoRequire(_parameter, js,
                            DojoLibs.QUERY, DojoLibs.DOM, DojoLibs.DOMCONSTRUCT, DojoLibs.NUMBER));
            }
        }
        return js;
    }

    /**
     * Auto-complete for the field with products.
     *
     * @param _parameter parameter from eFaps.
     * @return List to be rendered for auto-complete.
     * @throws EFapsException on error.
     */
    public Return autoComplete4Product(final Parameter _parameter)
        throws EFapsException
    {
        final Product product = new Product()
        {
            @Override
            protected boolean add2QueryBldr4AutoComplete4Product(final Parameter _parameter,
                                                                 final QueryBuilder _queryBldr)
                throws EFapsException
            {
                return AbstractDocument_Base.this.add2QueryBldr4AutoComplete4Product(_parameter, _queryBldr);
            }
        };
        return product.autoComplete4Product(_parameter);
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _queryBldr    QueryBuilder to add to
     * @return true if allow cache, else false
     * @throws EFapsException on error
     */
    protected boolean add2QueryBldr4AutoComplete4Product(final Parameter _parameter,
                                                         final QueryBuilder _queryBldr)
        throws EFapsException
    {
        boolean ret = true;
        for (final IOnQuery listener : Listener.get().<IOnQuery>invoke(
                        IOnQuery.class)) {
            ret = ret && listener.add2QueryBldr4AutoComplete4Product(this, _parameter, _queryBldr);
        }

        catalogFilter4productAutoComplete(_parameter, _queryBldr);
        if (Products.ACTIVATEINDIVIDUAL.get()) {
            // show products of type Individual and Batch
            final boolean showIndividual = "true".equalsIgnoreCase(getProperty(_parameter, "ShowIndividual", "false"));
            if (!showIndividual) {
                _queryBldr.addWhereAttrNotEqValue(CIProducts.ProductAbstract.Type,
                                CIProducts.ProductIndividual.getType().getId(),
                                CIProducts.ProductBatch.getType().getId());
            }
            // show products that are marked to use individual
            final boolean hideIndividualizable = "true".equalsIgnoreCase(getProperty(_parameter,
                            "HideMarkedIndividual", "false"));
            if (hideIndividualizable) {
                _queryBldr.addWhereAttrNotEqValue(CIProducts.StoreableProductAbstract.Individual,
                                ProductIndividual.INDIVIDUAL, ProductIndividual.BATCH);
            }
        }
        return ret;
    }

    /**
     * Gets the type name4 auto complete4 product.
     *
     * @param _parameter the _parameter
     * @return the type name4 auto complete4 product
     * @throws EFapsException the e faps exception
     */
    protected String getTypeName4AutoComplete4Product(final Parameter _parameter) throws EFapsException
    {
        return getTypeName4SysConf(_parameter);
    }

    /**
     * Catalog filter4product auto complete.
     *
     * @param _parameter the _parameter
     * @param _queryBldr the _query bldr
     * @throws EFapsException the e faps exception
     */
    protected void catalogFilter4productAutoComplete(final Parameter _parameter,
                                                     final QueryBuilder _queryBldr)
        throws EFapsException
    {
        /**
        final UITableCell tableCell = (UITableCell) _parameter.get(ParameterValues.CLASS);
        final AbstractCommand command = tableCell.getParent().getCommand();
        // evaluate the type
        Type typeDoc = command.getTargetCreateType();
        if (typeDoc == null) {
            if (_parameter.getInstance() == null) {
                final UITableCell cell = (UITableCell) _parameter.get(ParameterValues.CLASS);
                if (cell.getParent().getInstance() != null) {
                    typeDoc = cell.getParent().getInstance().getType();
                }
            } else {
                typeDoc = _parameter.getInstance().getType();
            }
        }

        if (typeDoc == null) {
            AbstractDocument_Base.LOG.error("No type found for: {}", _parameter);
        } else {
            final QueryBuilder queryBldr = new QueryBuilder(CISales.Products_Catalog2DocumentType);
            queryBldr.addWhereAttrEqValue(CISales.Products_Catalog2DocumentType.DocumentTypeLink, typeDoc.getId());
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selCat = new SelectBuilder()
                            .linkto(CISales.Products_Catalog2DocumentType.CatalogLinkAbstract).instance();
            multi.addSelect(selCat);
            multi.execute();
            final List<Instance> instances = new ArrayList<Instance>();
            while (multi.next()) {
                final Instance catInst = multi.<Instance>getSelect(selCat);
                instances.add(catInst);
            }
            if (!instances.isEmpty()) {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.Catalog2Products);
                attrQueryBldr.addWhereAttrEqValue(CIProducts.Catalog2Products.CatalogLinkAbstract,
                                instances.toArray());
                final AttributeQuery attrQuery = attrQueryBldr
                                .getAttributeQuery(CIProducts.Catalog2Products.ProductLink);
                _queryBldr.addWhereAttrInQuery(CIProducts.ProductAbstract.ID, attrQuery);
            }
        }

        final QueryBuilder queryBldr = new QueryBuilder(CISales.Products_Catalog2DocumentType);
        queryBldr.addWhereAttrEqValue(CISales.Products_Catalog2DocumentType.DocumentTypeLink, typeDoc.getId());
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selCat = new SelectBuilder()
                        .linkto(CISales.Products_Catalog2DocumentType.CatalogLinkAbstract).instance();
        multi.addSelect(selCat);
        multi.execute();
        final List<Instance> instances = new ArrayList<Instance>();
        while (multi.next()) {
            final Instance catInst = multi.<Instance>getSelect(selCat);
            instances.add(catInst);
        }
        if (!instances.isEmpty()) {
            final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.Catalog2Products);
            attrQueryBldr.addWhereAttrEqValue(CIProducts.Catalog2Products.CatalogLinkAbstract,
                            instances.toArray());
            final AttributeQuery attrQuery = attrQueryBldr
                            .getAttributeQuery(CIProducts.Catalog2Products.ProductLink);
            _queryBldr.addWhereAttrInQuery(CIProducts.ProductAbstract.ID, attrQuery);
        }
        */
    }

    /**
     * Analyse the table to calculate.
     *
     * @param _parameter Parameter as passed by the eFaps API for esjp
     * @param _row4priceFromDB the price for the given row will be retrieved
     *            from the DB, null means none, -1 means all
     * @return List of Calculators
     * @throws EFapsException on error
     */
    @SuppressWarnings("unchecked")
    public List<Calculator> analyseTable(final Parameter _parameter,
                                         final Integer _row4priceFromDB)
        throws EFapsException
    {
        final List<Calculator> ret = new ArrayList<>();

        final List<String[]> allParameters = new ArrayList<>();
        String[] quantities = _parameter.getParameterValues("quantity");
        allParameters.add(quantities);
        String[] discounts = _parameter.getParameterValues("discount");
        allParameters.add(discounts);
        String[] unitPrices = getUnitPricesFromUI(_parameter);
        allParameters.add(unitPrices);
        String[] oids = _parameter.getParameterValues("product");
        allParameters.add(oids);

        allParameters.sort((a1,
                            a2) -> {
            final var s1 = a1 == null ? 0 : a1.length;
            final var s2 = a2 == null ? 0 : a2.length;
            return s2 - s1;
        });
        if (allParameters.get(0) == null) {
            return ret;
        }
        final var  maxLength = allParameters.get(0).length;
        if (quantities == null) {
            quantities = new String[maxLength];
            Arrays.fill(quantities, "");
        }
        if (discounts == null) {
            discounts = new String[maxLength];
            Arrays.fill(discounts, "");
        }
        if (unitPrices == null) {
            unitPrices = new String[maxLength];
            Arrays.fill(unitPrices, "");
        }
        if (oids == null) {
            oids = new String[maxLength];
            Arrays.fill(oids, "");
        }

        final boolean withoutTax = "true".equals(_parameter.getParameterValue("withoutVAT"));

        final List<Calculator> oldCalcs = (List<Calculator>) Context.getThreadContext().getSessionAttribute(
                        AbstractDocument_Base.CALCULATOR_KEY);

        if (quantities != null) {
            for (int i = 0; i < quantities.length; i++) {
                Calculator oldCalc = null;
                if (oldCalcs != null && oldCalcs.size() > 0 && oldCalcs.size() > i) {
                    oldCalc = oldCalcs.get(i);
                }
                if (quantities[i].length() > 0 || discounts[i].length() > 0 || unitPrices[i].length() > 0
                                || oids[i].length() > 0) {
                    final boolean priceFromDB = _row4priceFromDB != null
                                    && (_row4priceFromDB.equals(i) || _row4priceFromDB.equals(-1));
                    final Calculator calc = getCalculator(_parameter, oldCalc, oids[i], quantities[i], unitPrices[i],
                                    discounts[i], priceFromDB, i);
                    calc.setWithoutTax(withoutTax);
                    ret.add(calc);
                } else {
                    ret.add(new Calculator());
                }
            }
        }
        Context.getThreadContext().setSessionAttribute(AbstractDocument_Base.CALCULATOR_KEY, ret);
        return ret;
    }

    /**
     * @param _parameter Parameter parameter as passe dfrom the eFaps API
     * @param _oldCalc calculator
     * @param _oid oid of the product
     * @param _quantity quantity
     * @param _unitPrice unit price
     * @param _discount discount
     * @param _priceFromDB must the price set from DB
     * @param _idx index
     * @throws EFapsException on error
     * @return new Calculator
     */

    public Calculator getCalculator(final Parameter _parameter,
                                    final Calculator _oldCalc,
                                    final String _oid,
                                    final String _quantity,
                                    final String _unitPrice,
                                    final String _discount,
                                    final boolean _priceFromDB,
                                    final int _idx)
        throws EFapsException
    {
        return new Calculator(_parameter, _oldCalc, _oid, _quantity, _unitPrice, _discount, _priceFromDB, this);
    }

    /**
     * @param _parameter Parameter parameter as passe dfrom the eFaps API
     * @param _oldCalc calculator
     * @param _prodInstance Instance of the product
     * @param _quantity quantity
     * @param _unitPrice unit price
     * @param _discount discount
     * @param _priceFromDB must the price set from DB
     * @param _idx index
     * @throws EFapsException on error
     * @return new Calculator
     */
    public Calculator getCalculator(final Parameter _parameter,
                                    final Calculator _oldCalc,
                                    final Instance _prodInstance,
                                    final BigDecimal _quantity,
                                    final BigDecimal _unitPrice,
                                    final BigDecimal _discount,
                                    final boolean _priceFromDB,
                                    final int _idx)
        throws EFapsException
    {
        return new Calculator(_parameter, _oldCalc, _prodInstance, _quantity, _unitPrice, _discount,
                        _priceFromDB, this);
    }

    /**
     * @param _parameter Parameter parameter as passed from the eFaps API
     * @return the type name used in SystemConfiguration
     * @throws EFapsException on error
     */
    public String getTypeName4SysConf(final Parameter _parameter)
        throws EFapsException
    {
        return getType4SysConf(_parameter).getName();
    }

    @Override
    protected Type getType4SysConf(final Parameter _parameter)
        throws EFapsException
    {
        return getCIType().getType();
    }

    @Override
    public CIType getCIType()
        throws EFapsException
    {
        return CISales.DocumentAbstract;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return type used for creation of positions
     * @throws EFapsException on error
     */
    protected Type getType4PositionCreate(final Parameter _parameter)
        throws EFapsException
    {
        Type ret = null;
        if (containsProperty(_parameter, "PositionType")) {
            ret = Type.get(getProperty(_parameter, "PositionType"));
        } else if (getTypeName4SysConf(_parameter) != null) {
            ret = Type.get(getTypeName4SysConf(_parameter) + "Position");
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return type used for creation of positions
     * @throws EFapsException on error
     */
    protected Type getType4PositionUpdate(final Parameter _parameter)
        throws EFapsException
    {
        return getType4PositionCreate(_parameter);
    }

    /**
     * Method to get the maximum for a value from the database.
     *
     * @param _parameter Parameter as passed by the eFaps API for esjp
     * @param _serial optional serial number used as filter
     * @param _types the _types
     * @return maximum
     * @throws EFapsException on error
     */
    protected String getMaxNumber(final Parameter _parameter,
                                  final String _serial,
                                  final String...   _types)
        throws EFapsException
    {
        String ret = null;
        QueryBuilder queryBuilder = null;
        for (final String typeStr : _types) {
            final Type type;
            if (isUUID(typeStr)) {
                type = Type.get(UUID.fromString(typeStr));
            } else {
                type = Type.get(typeStr);
            }
            if (queryBuilder == null) {
                queryBuilder = new QueryBuilder(type);
            } else {
                queryBuilder.addType(type);
            }
        }
        if (_serial != null) {
            queryBuilder.addWhereAttrMatchValue(CIERP.DocumentAbstract.Name, _serial + "*");
        }
        queryBuilder.addOrderByAttributeDesc(CIERP.DocumentAbstract.Name);
        final InstanceQuery query = queryBuilder.getQuery();
        query.setLimit(1);
        final MultiPrintQuery multi = new MultiPrintQuery(query.execute());
        multi.addAttribute(CIERP.DocumentAbstract.Name);
        multi.execute();
        if (multi.next()) {
            ret = multi.getAttribute(CIERP.DocumentAbstract.Name);
        }
        return ret;
    }

    /**
     * Method to get the value for the name.
     *
     * @param _parameter Parameter as passed by the eFaps API for esjp
     * @return Return containing the value
     * @throws EFapsException on error
     */
    public Return getNameFieldValueUI(final Parameter _parameter)
        throws EFapsException
    {
        final Collection<String> types = analyseProperty(_parameter, "Type").values();
        String number = getMaxNumber(_parameter, null, types.toArray(new String[types.size()]));
        if (number == null) {
            number = "001-000001";
        } else {
            // get the numbers after the first "-"
            final Pattern pattern = Pattern.compile("(?<=-)\\d*");
            final Matcher matcher = pattern.matcher(number);
            if (matcher.find()) {
                final String numTmp = matcher.group();
                final int length = numTmp.length();
                final Integer numInt = Integer.parseInt(numTmp) + 1;
                final NumberFormat nf = NumberFormat.getInstance();
                nf.setMinimumIntegerDigits(length);
                nf.setMaximumIntegerDigits(length);
                nf.setGroupingUsed(false);
                number = number.substring(0, number.indexOf("-") + 1) + nf.format(numInt);
            }
        }
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, number);
        return retVal;
    }

    /**
     * Method to get the value for the name.
     *
     * @param _parameter Parameter as passed by the eFaps API for esjp
     * @return Return containing the value
     * @throws EFapsException on error
     */
    public Return getNameWithSerialFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final StringBuilder html = new StringBuilder();
        final IUIValue fieldValue = (IUIValue) _parameter.get(ParameterValues.UIOBJECT);
        final String fieldName = fieldValue.getField().getName() + "_SN";
        final List<DropDownPosition> options = getSerialNumbers(_parameter);
        String serial = "001";
        final StringBuilder snHtml;
        if (options.size() == 1) {
            serial = options.get(0).getValue().toString();
            snHtml = new StringBuilder().append("<input type=\"hidden\" value=\"").append(serial).append("\" name=\"")
                            .append(fieldName).append("\"><span>").append(serial).append("-</span>");
        } else {
            for (final DropDownPosition option : options) {
                if (option.isSelected()) {
                    serial = option.getValue().toString();
                    break;
                }
            }
            final Parameter parameter = ParameterUtil.clone(_parameter);
            ParameterUtil.setProperty(parameter, "FieldName", fieldName);
            final org.efaps.esjp.common.uiform.Field field = new org.efaps.esjp.common.uiform.Field();
            snHtml = field.getDropDownField(parameter, options).append("-");
        }
        html.append(snHtml);

        final Collection<String> types = analyseProperty(_parameter, "Type").values();
        String number = getMaxNumber(_parameter, serial, types.toArray(new String[types.size()]));

        Integer numberInt = 1;
        if (number != null) {
            // get the numbers after the first "-"
            final Pattern pattern = Pattern.compile("(?<=-)\\d*");
            final Matcher matcher = pattern.matcher(number);
            if (matcher.find()) {
                final String numTmp = matcher.group();
                numberInt = Integer.parseInt(numTmp) + 1;
            }
        }
        final String type = getProperty(_parameter, "Type");
        final var serialProps = ERP.SERIALNUMBERS.get();
        final int length = Integer.valueOf(serialProps.getProperty(type + ".SuffixLength", "6"));
        final NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumIntegerDigits(length);
        nf.setMaximumIntegerDigits(length);
        nf.setGroupingUsed(false);
        number = nf.format(numberInt);
        html.append("<input type=\"text\" size=\"8\" name=\"").append(fieldValue.getField().getName())
                        .append("\" value=\"").append(number).append("\">");
        final Return retVal = new Return();
        retVal.put(ReturnValues.SNIPLETT, html);
        return retVal;
    }

    /**
     * Gets the name with serial filter field value.
     *
     * @param _parameter the _parameter
     * @return the name with serial filter field value
     * @throws EFapsException the e faps exception
     */
    public Return getNameWithSerialFilterFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final List<DropDownPosition> values = getSerialNumbers(_parameter);
        for (final DropDownPosition position: values) {
            position.setValue(position.getValue() + "-*");
        }
        final UIForm uiform = (UIForm) _parameter.get(ParameterValues.CLASS);
        final String sessKey =  uiform.getCallingCommand().getUUID() + "-" + UITable.UserCacheKey.FILTER.getValue();
        if (Context.getThreadContext().containsSessionAttribute(sessKey)) {
            @SuppressWarnings("unchecked")
            final Map<String, TableFilter> sessfilter = (Map<String, TableFilter>) Context
                            .getThreadContext().getSessionAttribute(sessKey);
            final IUIValue fieldValue = (IUIValue) _parameter.get(ParameterValues.UIOBJECT);
            if (sessfilter.containsKey(fieldValue.getField().getName())) {
                final TableFilter filter = sessfilter.get(fieldValue.getField().getName());
                final IMapFilter map = (IMapFilter) filter.getFilter();
                if (map != null && map.containsKey(fieldValue.getField().getName())) {
                    final String[] selected = (String[]) map.get(fieldValue.getField().getName());
                    if (selected != null && selected.length > 0) {
                        for (final DropDownPosition position: values) {
                            for (final String sel : selected) {
                                if (position.getValue().equals(sel)) {
                                    position.setSelected(true);
                                    break;
                                } else {
                                    position.setSelected(false);
                                }
                            }
                        }
                    }
                }
            }
        }
        final Return ret = new Return();
        ret.put(ReturnValues.VALUES, values);
        return ret;
    }

    /**
     *
     * @param _parameter Parameter as passed from eFaps to an esjp
     * @return Return with True if VIEW, else false
     * @throws EFapsException on error
     */
    public Return serialAccessCheck(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final boolean access = !getSerialNumbers(_parameter).isEmpty();
        final boolean inverse = "true".equalsIgnoreCase(getProperty(_parameter, "Inverse"));
        if (!inverse && access || inverse && !access) {
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    /**
     * Method to get the value for the name.
     *
     * @param _parameter Parameter as passed by the eFaps API for esjp
     * @return list of dropdowns
     * @throws EFapsException on error
     */
    protected List<DropDownPosition> getSerialNumbers(final Parameter _parameter)
        throws EFapsException
    {
        final String type = getProperty(_parameter, "Type");
        final Properties properties = ERP.SERIALNUMBERS.get();
        final String serialStr = properties.getProperty(type, "001");
        final List<DropDownPosition> ret = new ArrayList<>();
        final org.efaps.esjp.common.uiform.Field field = new org.efaps.esjp.common.uiform.Field();
        boolean first = true;
        for (final String serial : serialStr.split(";")) {
            final DropDownPosition option = field.getDropDownPosition(_parameter, serial, serial);
            ret.add(option);
            if (first) {
                option.setSelected(true);
                first = false;
            }
        }
        Collections.sort(ret, new Comparator<DropDownPosition>() {
            @SuppressWarnings("unchecked")
            @Override
            public int compare(final DropDownPosition _o1,
                               final DropDownPosition _o2)
            {
                return _o1.getOrderValue().compareTo(_o2.getOrderValue());
            }
        });
        return ret;
    }

    /**
     * Called from the field with the rate currency for a document. Returning a
     * dropdown with all currencies. The default is set inside the
     * SystemConfiguration for sales.
     *
     * @param _parameter Parameter as passed by the eFaps API for esjp
     * @return a dropdown with all currency
     * @throws EFapsException on error
     */
    public Return rateCurrencyFieldValueUI(final Parameter _parameter)
        throws EFapsException
    {
        @SuppressWarnings("unchecked")
        final Map<Object, Object> props = (Map<Object, Object>) _parameter.get(ParameterValues.PROPERTIES);
        if (!props.containsKey("Type")) {
            props.put("Type", CIERP.Currency.getType().getName());
        }
        if (!props.containsKey("Select")) {
            props.put("Select", "attribute[Name]");
        }
        if (!props.containsKey("StatusGroup") && !props.containsKey("StatusGroup01")) {
            props.put("StatusGroup", CIERP.CurrencyStatus.getType().getName());
            props.put("Status", CIERP.CurrencyStatus.Active.key);
        }

        final Instance currInst = evaluateCurrency4JavaScript(_parameter);

        final org.efaps.esjp.common.uiform.Field field = new org.efaps.esjp.common.uiform.Field()
        {

            @Override
            protected void updatePositionList(final Parameter _parameter,
                                              final List<DropDownPosition> _values)
                throws EFapsException
            {
                for (final DropDownPosition pos : _values) {
                    if (currInst.getId() == (Long) pos.getValue()) {
                        pos.setSelected(true);
                        break;
                    }
                }
            }
        };
        return field.getOptionListFieldValue(_parameter);
    }

    /**
     * Rate field value ui.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return rateFieldValueUI(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        if (TargetMode.CREATE.equals(_parameter.get(ParameterValues.ACCESSMODE))) {
            // for create mode simulate a value from the Database with the given configuration
            final Instance currInst = evaluateCurrency4JavaScript(_parameter);
            final RateInfo rateInfo = new Currency().evaluateRateInfo(_parameter, (String) null, currInst);
            if ("true".equalsIgnoreCase(getProperty(_parameter, "UI"))) {
                ret.put(ReturnValues.VALUES, getRateFrmt(_parameter, rateInfo));
            } else {
                final Object[] rateObject = getRateObject(_parameter, rateInfo);
                ret.put(ReturnValues.VALUES, new Object[] { rateObject[0], rateObject[1], currInst.getId() });
            }
        }
        return ret;
    }

    /**
     * Evaluate currency instance.
     *
     * @param _parameter the _parameter
     * @return the instance
     * @throws CacheReloadException the cache reload exception
     * @throws EFapsException the e faps exception
     */
    protected Instance evaluateCurrencyInstance(final Parameter _parameter)
        throws CacheReloadException, EFapsException
    {
        Instance ret = null;
        //first check if in a currenct document, and if use the current Currency
        if (TargetMode.EDIT.equals(_parameter.get(ParameterValues.ACCESSMODE))) {
            final Instance inst = _parameter.getInstance();
            if (inst != null && inst.isValid() && inst.getType().isKindOf(CISales.DocumentSumAbstract.getType())) {
                final PrintQuery print = CachedPrintQuery.get4Request(inst);
                final SelectBuilder sel = SelectBuilder.get().linkto(CISales.DocumentSumAbstract.RateCurrencyId)
                                .instance();
                print.addSelect(sel);
                print.executeWithoutAccessCheck();
                final Instance instTmp = print.<Instance>getSelect(sel);
                if (instTmp != null && instTmp.isValid()) {
                    ret = instTmp;
                }
            }
        }
        if (!InstanceUtils.isValid(ret)) {
            // first priority specifically set for a type
            String typeName = getTypeName4SysConf(_parameter);
            if (typeName.startsWith("Sales_")) {
                typeName = typeName.replace("Sales_", "");
            }
            final String key = Sales.BASE + typeName + ".DefaultCurrency";
            ret = Sales.getSysConfig().getLink(key);
        }
        if (!InstanceUtils.isValid(ret)) {
            ret = Sales.DEFAULTCURRENCY4DOC.get();
        }
        if (!InstanceUtils.isValid(ret)) {
            ret =  Currency.getBaseCurrency();
        }
        return ret;
    }

    /**
     * Method to add extra fields to update when the currency change.
     *
     * @param _parameter as passed from eFaps API.
     * @param _calculators with the values.
     * @return StringBuilder.
     * @throws EFapsException on error
     */
    protected StringBuilder addFields4RateCurrency(final Parameter _parameter,
                                                   final List<Calculator> _calculators)
        throws EFapsException
    {
        return new StringBuilder();
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API for esjp
     * @return boolean value
     * @throws EFapsException on error
     */
    public Return withoutVATFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final TargetMode mode = (TargetMode) _parameter.get(ParameterValues.ACCESSMODE);
        if (TargetMode.EDIT.equals(mode)) {
            final PrintQuery print = new PrintQuery(_parameter.getInstance());
            print.addAttribute(CISales.DocumentSumAbstract.Taxes);
            print.execute();
            final Taxes taxes = print.getAttribute(CISales.DocumentSumAbstract.Taxes);
            ret.put(ReturnValues.VALUES, taxes == null || taxes.getEntries().isEmpty());
        } else if (TargetMode.CREATE.equals(mode)) {
            ret.put(ReturnValues.VALUES, BooleanUtils.toBoolean(getProperty(_parameter, "DefaultValue", "false")));
        }
        return ret;
    }

    /**
     * Render a script to set the focus to another field.
     *
     * @param _parameter Parameter as passed by the eFaps API for esjp
     * @return javascript sniplett
     */
    public Return getJS4SelectDocumentForm(final Parameter _parameter)
    {
        final StringBuilder js = new StringBuilder();
        js.append("<script type=\"text/javascript\">")
            .append("Wicket.Event.add(window, \"domready\", function(event) {")
            .append("inputs = document.getElementsByTagName('INPUT');")
            .append("for (i=0;i<inputs.length;i++) {")
            .append("inputs[i].blur();")
            .append("}")
            .append("var ele = document.createElement('input');")
            .append("var attr = document.createAttribute('type');")
            .append("attr.nodeValue = 'hidden';")
            .append("ele.setAttributeNode(attr);")
            .append("require([\"dojo/query\"],function(query){")
            .append("dojo.query('.eFapsContentDiv')[0].appendChild(ele);")
            .append("ele.name='selectedDoc';")
            .append("});")
            .append(" });")
            .append("</script>");
        final Return retVal = new Return();
        retVal.put(ReturnValues.SNIPLETT, js.toString());
        return retVal;
    }

    /**
     * Get the default storage.
     * @param _parameter Parameter as passed by the eFaps API
     * @return instance of a storage
     * @throws EFapsException on error
     */
    protected Instance getDefaultStorage(final Parameter _parameter)
        throws EFapsException
    {
        return Storage.getDefaultStorage(_parameter);
    }

    /**
     * Get a rate Object from the User Interface.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return Object
     * @throws EFapsException on error.
     */
    protected Object[] getRateObject(final Parameter parameter)
        throws EFapsException
    {
        if (isRest()) {
            final var dateStr = this.<String>getValue(parameter, "date");
            final int selectedCurrency = getValue(parameter, "rateCurrencyId");
            final var curInst = CurrencyInst.get(Long.valueOf(selectedCurrency)).getInstance();
            final var rateInfo = new Currency().evaluateRateInfo(parameter, dateStr, curInst);
            return RateInfo.getRateObject(parameter, rateInfo, getCIType().getType().getName());
        }

        BigDecimal rate = BigDecimal.ONE;
        try {
            if (parameter.getParameterValue("rate") != null) {
                rate = (BigDecimal) RateFormatter.get().getFrmt4Rate(null).parse(parameter.getParameterValue("rate"));
            }
        } catch (final ParseException e) {
            throw new EFapsException(AbstractDocument_Base.class, "analyzeRate.ParseException", e);
        }
        final boolean rInv = "true".equalsIgnoreCase(parameter.getParameterValue("rate" + RateUI.INVERTEDSUFFIX));
        return new Object[] { rInv ? BigDecimal.ONE : rate, rInv ? rate : BigDecimal.ONE };
    }

    /**
     * Get the name for the document on creation.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return new Name
     * @throws EFapsException on error
     */
    @Override
    protected String getDocName4Create(final Parameter _parameter)
        throws EFapsException
    {
        String ret = _parameter.getParameterValue("serial");
        if (StringUtils.isNotEmpty(ret)) {
            ret = ret + "-" + getNumberSuffix(0);
        } else {
            // next priority are values from the UserInterface
            ret = _parameter.getParameterValue("name4create");
            if (StringUtils.isEmpty(ret)) {
                ret = super.getDocName4Create(_parameter);
            } else {
                final String sn = _parameter.getParameterValue("name4create_SN");
                if (sn != null && !sn.isEmpty()) {
                    ret = sn + "-" + ret;
                }
            }
        }
        return ret;
    }

    @Override
    protected void addStatus2DocCreate(final Parameter _parameter,
                                       final Insert _insert,
                                       final CreatedDoc _createdDoc)
        throws EFapsException
    {
        Status status = null;
        //  first check if set via special SystemConfiguration
        final Type type = getType4DocCreate(_parameter);
        if (type != null) {
            // construct the key
            final String configKey = Sales.BASE + type.getName().replaceFirst("Sales_", "") + ".Status4Create";
            if (Sales.getSysConfig().containsAttributeValue(configKey)) {
                final String key = Sales.getSysConfig().getAttributeValue(configKey);
                status = Status.find(type.getStatusAttribute().getLink().getUUID(), key);
            }
        }
        if (status == null) {
            super.addStatus2DocCreate(_parameter, _insert, _createdDoc);
        }  else {
            _insert.add(type.getStatusAttribute(), status);
        }
    }


    /**
     * Get the name for the document on edit.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return new Name
     * @throws EFapsException on error
     */
    protected String getDocName4Edit(final Parameter parameter)
        throws EFapsException
    {
        String ret = parameter.getParameterValue("serial");
        if (StringUtils.isNotEmpty(ret)) {
            final String name = EQL.builder().print(parameter.getInstance())
                            .attribute(CISales.DocumentAbstract.Name)
                            .evaluate().get(1);
            if (name.startsWith(ret)) {
                ret = null;
            } else {
                ret = ret + "-" + getNumberSuffix(0);
            }
        } else {
            ret = parameter.getParameterValue("name4edit");
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return number of positions
     * @throws EFapsException on error
     */
    protected int getPositionsCount(final Parameter _parameter)
        throws EFapsException
    {
        final String[] countAr = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                        CISales.PositionAbstract.Quantity.name));
        return countAr == null ? 0 : countAr.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean priceFromUIisNet(final Parameter _parameter)
        throws EFapsException
    {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSysConfKey4Doc(final Parameter _parameter)
        throws EFapsException
    {
        return getTypeName4SysConf(_parameter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSysConfKey4Pos(final Parameter _parameter)
        throws EFapsException
    {
        final Type typeTmp = getType4PositionCreate(_parameter);
        final String ret;
        if (typeTmp != null) {
            ret = typeTmp.getName();
        } else {
            ret = "DefaultPosition";
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return validate(final Parameter _parameter)
        throws EFapsException
    {
        final Validation validation = new Validation();
        return validation.validate(_parameter, this);
    }

    /**
     * Gets the unit prices from ui.
     *
     * @param _parameter the _parameter
     * @return the unit prices from ui
     */
    protected String[] getUnitPricesFromUI(final Parameter _parameter)
    {
        return _parameter.getParameterValues("netUnitPrice");
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _createdDoc   created doc
     * @throws EFapsException on error
     */
    protected void connect2DocumentType(final Parameter _parameter,
                                        final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final Instance instDocType = Instance.get(_parameter.getParameterValue("documentType"));
        if (instDocType.isValid() && _createdDoc.getInstance().isValid()) {
            insert2DocumentTypeAbstract(CISales.Document2DocumentType, _createdDoc, instDocType);
        }
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _createdDoc   created doc
     * @throws EFapsException on error
     */
    protected void connect2ProductDocumentType(final Parameter _parameter,
                                               final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final Instance instDocType = Instance.get(_parameter.getParameterValue("productDocumentType"));
        if (instDocType.isValid() && _createdDoc.getInstance().isValid()) {
            insert2DocumentTypeAbstract(CISales.Document2ProductDocumentType, _createdDoc, instDocType);
        }
    }

    /**
     * Method to add relation document and document type.
     *
     * @param _type CIType to create relation document and document type.
     * @param _createdDoc created doc.
     * @param _docTypeAbs instance the document type.
     * @throws EFapsException on error.
     */
    protected void insert2DocumentTypeAbstract(final CIType _type,
                                               final CreatedDoc _createdDoc,
                                               final Instance _docTypeAbs)
        throws EFapsException
    {
        final Insert insert = new Insert(_type);
        insert.add(CIERP.Document2DocumentTypeAbstract.DocumentLinkAbstract, _createdDoc.getInstance());
        insert.add(CIERP.Document2DocumentTypeAbstract.DocumentTypeLinkAbstract, _docTypeAbs);
        insert.execute();
    }

    /**
     * Connect document type2 catalog.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return connectDocumentType2Catalog(final Parameter _parameter)
        throws EFapsException
    {
        final Instance catInst = _parameter.getInstance();
        final Long typeId = Long.parseLong(_parameter
                        .getParameterValue(CIFormSales.Sales_Products_Catalog2DocumentTypeForm.type.name));

        final Insert insert = new Insert(CISales.Products_Catalog2DocumentType);
        insert.add(CISales.Products_Catalog2DocumentType.CatalogLinkAbstract, catInst);
        insert.add(CISales.Products_Catalog2DocumentType.DocumentTypeLink, typeId);
        insert.execute();
        return new Return();
    }

    /**
     * Method to connect the document with the selected document type.
     *
     * @param _parameter    Parameter as passed from the eFaps API
     * @param _createdDoc   CreatedDoc  to be connected
     * @return the list
     * @throws EFapsException on error
     */
    protected List<Instance> connect2Derived(final Parameter _parameter,
                                             final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final List<Instance> ret = new ArrayList<>();
        final String[] deriveds = _parameter.getParameterValues("derived");
        if (deriveds != null) {
            for (final String derived : deriveds) {
                final Instance derivedInst = Instance.get(derived);
                if (derivedInst.isValid() && _createdDoc.getInstance().isValid()) {
                    final Insert insert = new Insert(CISales.Document2DerivativeDocument);
                    insert.add(CISales.Document2DerivativeDocument.From, derivedInst);
                    insert.add(CISales.Document2DerivativeDocument.To, _createdDoc.getInstance());
                    insert.execute();
                    ret.add(derivedInst);
                }
                final Map<Integer, String> rules = analyseProperty(_parameter, "DerivedRulesType");
                for (final Entry<Integer, String> entry : rules.entrySet()) {
                    final Type type = isUUID(entry.getValue()) ? Type.get(UUID.fromString(entry.getValue()))
                                    : Type.get(entry.getValue());
                    if (derivedInst.getType().equals(type)) {
                        final Map<Integer, String> relations = analyseProperty(_parameter, "DerivedRulesRelation");
                        final Map<Integer, String> currentLinks = analyseProperty(_parameter, "DerivedRulesCurrenLink");
                        final Map<Integer, String> derivdedLinks = analyseProperty(_parameter,
                                        "DerivedRulesDerivedLink");

                        final Type relType = isUUID(relations.get(entry.getKey())) ? Type.get(UUID.fromString(relations
                                        .get(entry.getKey()))) : Type.get(relations.get(entry.getKey()));
                        final Insert insert = new Insert(relType);
                        insert.add(derivdedLinks.get(entry.getKey()), derivedInst);
                        insert.add(currentLinks.get(entry.getKey()), _createdDoc.getInstance());
                        insert.execute();
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Method to connect the document with terms.
     *
     * @param _parameter    Parameter as passed from the eFaps API
     * @param _createdDoc   CreatedDoc  to be connected
     * @throws EFapsException on error
     */
    protected void connect2Terms(final Parameter _parameter,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final String[] terms = _parameter.getParameterValues("terms");
        if (terms != null) {
            for (final String term : terms) {
                if (!term.isEmpty() && _createdDoc.getInstance().isValid()) {
                    final Insert insert = new Insert(CISales.Document2TextElement);
                    insert.add(CISales.Document2TextElement.Document, _createdDoc.getInstance());
                    insert.add(CISales.Document2TextElement.TextElement, term);
                    insert.execute();
                }
            }
        }
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new empty Return
     * @throws EFapsException on error
     */
    public Return generateCode4Doc(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instance = _parameter.getInstance();

        if (instance != null && instance.isValid()) {
            final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionGroupRoot);
            queryBldr.addWhereAttrEqValue(CISales.PositionGroupRoot.DocumentAbstractLink, instance);
            queryBldr.addOrderByAttributeAsc(CISales.PositionGroupRoot.Order);
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.setEnforceSorted(true);
            multi.execute();
            int i = 1;
            while (multi.next()) {
                calculateCode(_parameter, multi.getCurrentInstance(), new Integer[] { i });
                i++;
            }
        }
        return new Return();
    }

    /**
     * Recursive method to set the code for groups.
     * @param _parameter Parameter as passed by the eFaps API
     * @param _parent instance the children will be searched for
     * @param _current Array used for calculation
     * @throws EFapsException on error
     */
    protected void calculateCode(final Parameter _parameter,
                                 final Instance _parent,
                                 final Integer[] _current)
        throws EFapsException
    {
        final Update update = new Update(_parent);
        update.add(CISales.PositionGroupAbstract.Code, getCode(_parameter, _current));
        update.execute();

        final QueryBuilder queryBldr = new QueryBuilder(CISales.PositionGroupNode);
        queryBldr.addWhereAttrEqValue(CISales.PositionGroupNode.ParentGroupLink, _parent);
        queryBldr.addOrderByAttributeAsc(CISales.PositionGroupNode.Order);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.setEnforceSorted(true);
        multi.execute();
        int i = 1;
        while (multi.next()) {
            calculateCode(_parameter, multi.getCurrentInstance(), ArrayUtils.add(_current, i));
            i++;
        }
    }

    /**
     * Checks is a document is selected
     * by {@link #getJS4Doc4Contact(Parameter, Instance, String, QueryBuilder)}.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst the instance of document that must be checked for selected
     * @return the selected instances4 j s4 doc4 contact
     * @throws EFapsException the e faps exception
     */
    protected boolean docIsSelected4JS4Doc4Contact(final Parameter _parameter,
                                                   final Instance _docInst)
         throws EFapsException
    {
        var isSelected =  getInstances4Derived(_parameter).contains(_docInst);
        if (!isSelected) {
            isSelected = _docInst.equals(_parameter.getCallInstance());
        }
        return isSelected;
    }

    /**
     * Gets the j s4 doc4 contact.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _instance the _instance
     * @param _fieldName the _field name
     * @param _queryBldr the QueryBuilder
     * @return the javascript part
     * @throws EFapsException on error
     */
    protected StringBuilder getJS4Doc4Contact(final Parameter _parameter,
                                              final Instance _instance,
                                              final String _fieldName,
                                              final QueryBuilder _queryBldr)
        throws EFapsException
    {
        final Parameter paraClone = ParameterUtil.clone(_parameter);
        ParameterUtil.setProperty(paraClone, "FieldName", _fieldName);

        final StringBuilder ret = new StringBuilder();
        final org.efaps.esjp.common.uiform.Field field = new org.efaps.esjp.common.uiform.Field();
        final List<DropDownPosition> values = new ArrayList<>();

        if (_instance.getType().isKindOf(CIContacts.Contact.getType())) {
            _queryBldr.addWhereAttrEqValue(CIERP.DocumentAbstract.Contact, _instance);
        } else {
            final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Document2DocumentAbstract);
            attrQueryBldr.addWhereAttrEqValue(CISales.Document2DocumentAbstract.FromAbstractLink, _instance);
            final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(
                            CISales.Document2DocumentAbstract.ToAbstractLink);
            _queryBldr.addWhereAttrInQuery(CIERP.DocumentAbstract.ID, attrQuery);
        }

        final MultiPrintQuery multi = _queryBldr.getPrint();
        multi.addAttribute(CIERP.DocumentAbstract.Name, CIERP.DocumentAbstract.Date);
        multi.execute();
        while (multi.next()) {
            final String name = multi.<String>getAttribute(CIERP.DocumentAbstract.Name);
            final DateTime date = multi.<DateTime>getAttribute(CIERP.DocumentAbstract.Date);
            final String option = name + " "
                            + (date == null ? "" : date.toString("dd/MM/yyyy", Context.getThreadContext().getLocale()));
            final DropDownPosition dropDown = field
                            .getDropDownPosition(paraClone, multi.getCurrentInstance().getOid(), option);
            if (docIsSelected4JS4Doc4Contact(_parameter, multi.getCurrentInstance())) {
                dropDown.setSelected(true);
            }
            values.add(dropDown);
        }
        final StringBuilder html = field.getInputField(paraClone, values, ListType.CHECKBOX);
        ret.append("if (document.getElementsByName('")
            .append(_fieldName).append("')[0]) {\n")
            .append("document.getElementsByName('").append(_fieldName)
            .append("')[0].innerHTML='").append(html).append("';")
            .append("}\n");
        return ret;
    }

    /**
     * Recursive method to set the code for groups.
     * @param _parameter Parameter as passed by the eFaps API
     * @param _current Array used for calculation
     * @return new code as String
     * @throws EFapsException on error
     */
    protected String getCode(final Parameter _parameter,
                             final Integer[] _current)
        throws EFapsException
    {
        final StringBuilder code = new StringBuilder();
        for (int i = 0; i < _current.length; i++) {
            if (i > 0) {
                code.append(".");
            }
            code.append(_current[i]);
        }
        return code.toString();
    }

    /**
     * Gets the revision sequence field value.
     *
     * @param _parameter the _parameter
     * @param _sessionKey the _session key
     * @return the revision sequence field value
     * @throws EFapsException the e faps exception
     */
    protected Return getRevisionSequenceFieldValue(final Parameter _parameter,
                                                   final String _sessionKey)
        throws EFapsException
    {
        final Return ret = new Return();
        final String revision = (String) Context.getThreadContext().getSessionAttribute(_sessionKey);
        Context.getThreadContext().setSessionAttribute(_sessionKey, null);
        final StringBuilder html = new StringBuilder();
        html.append("<span style=\"text-align: center; width: 98%; font-size:40pt; height: 55px; position:absolute\">")
                        .append(revision).append("</span>");
        ret.put(ReturnValues.SNIPLETT, html.toString());
        return ret;
    }

    public Return dropDown4Serial(final Parameter parameter)
        throws EFapsException
    {
        final var ret = new Return();
        final var values = getSerialNumbers(parameter);
        if (TargetMode.EDIT.equals(parameter.get(ParameterValues.ACCESSMODE))) {
            final String name = EQL.builder().print(parameter.getInstance())
                            .attribute(CISales.DocumentAbstract.Name)
                            .evaluate().get(1);
            values.forEach(option -> option.setSelected(name.startsWith(option.getLabel())));
        }
        ret.put(ReturnValues.VALUES, values);
        return ret;
    }

    public String getNumberSuffix(final int idx)
        throws EFapsException
    {
        final var type = getCIType().getType().getName();
        final var serialProps = ERP.SERIALNUMBERS.get();
        final int length = Integer.valueOf(serialProps.getProperty(type + ".SuffixLength", "6"));
        final NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumIntegerDigits(length);
        nf.setMaximumIntegerDigits(length);
        nf.setGroupingUsed(false);
        return nf.format(idx);
    }

    protected void afterCreate(final Parameter parameter,
                               final Instance instanec)
        throws EFapsException
    {
        for (final IOnCreateDocument listener : Listener.get().<IOnCreateDocument>invoke(
                        IOnCreateDocument.class)) {
            listener.afterCreate(parameter, instanec);
        }
    }

    /**
     * The Class AbstractUIPosition.
     *
     * @author The eFaps Team
     */
    public abstract static class AbstractUIPosition
        implements Serializable
    {
        /** */
        private static final long serialVersionUID = 1L;

        /** The doc. */
        private AbstractDocument_Base doc;

        /** The parameter. */
        private Parameter parameter;

        /** The prod instance. */
        private Instance prodInstance;

        /** The instance. */
        private Instance instance;

        /** The quantity. */
        private BigDecimal quantity;

        /** The net unit price. */
        private BigDecimal netUnitPrice;

        /** The discount. */
        private BigDecimal discount;

        /** The discount net unit price. */
        private BigDecimal discountNetUnitPrice;

        /** The net price. */
        private BigDecimal netPrice;

        /** The cross price. */
        private BigDecimal crossPrice;

        /** The prod name. */
        private String prodName;

        /** The prod descr. */
        private String prodDescr;

        /** The uo mid. */
        private Long uoMID;

        /** The storage inst. */
        private Instance storageInst;


        /**
         * Instantiates a new UI abstract position.
         */
        public AbstractUIPosition()
        {
        }

        /**
         * Instantiates a new UI abstract position.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _doc the _doc
         */
        public AbstractUIPosition(final Parameter _parameter,
                                  final AbstractDocument_Base _doc)
        {
            parameter = _parameter;
            doc = _doc;
        }

        /**
         * Gets the storage inst.
         *
         * @return the storage inst
         */
        public Instance getStorageInst()
        {
            return storageInst;
        }

        /**
         * Sets the storage inst.
         *
         * @param _storageInst the new storage inst
         * @return the abstract ui position
         */
        public AbstractUIPosition setStorageInst(final Instance _storageInst)
        {
            storageInst = _storageInst;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #prodInstance}.
         *
         * @return value of instance variable {@link #prodInstance}
         */
        public Instance getProdInstance()
        {
            return prodInstance;
        }

        /**
         * Setter method for instance variable {@link #prodInstance}.
         *
         * @param _prodInstance value for instance variable
         *            {@link #prodInstance}
         * @return the abstract ui position
         */
        public AbstractUIPosition setProdInstance(final Instance _prodInstance)
        {
            prodInstance = _prodInstance;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #quantity}.
         *
         * @return value of instance variable {@link #quantity}
         */
        public BigDecimal getQuantity()
        {
            return quantity;
        }

        /**
         * Setter method for instance variable {@link #quantity}.
         *
         * @param _quantity value for instance variable {@link #quantity}
         * @return the abstract ui position
         */
        public AbstractUIPosition setQuantity(final BigDecimal _quantity)
        {
            quantity = _quantity;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #netUnitPrice}.
         *
         * @return value of instance variable {@link #netUnitPrice}
         */
        public BigDecimal getNetUnitPrice()
        {
            return netUnitPrice;
        }

        /**
         * Setter method for instance variable {@link #netUnitPrice}.
         *
         * @param _netUnitPrice the _net unit price
         * @return the abstract ui position
         */
        public AbstractUIPosition setNetUnitPrice(final BigDecimal _netUnitPrice)
        {
            netUnitPrice = _netUnitPrice;
            return this;
        }

        /**
         * Getter method for the instance variable
         * {@link #rateDiscountNetUnitPrice}.
         *
         * @return value of instance variable {@link #rateDiscountNetUnitPrice}
         */
        public BigDecimal getDiscountNetUnitPrice()
        {
            return discountNetUnitPrice;
        }

        /**
         * Setter method for instance variable {@link #rateDiscountNetUnitPrice}
         * .
         *
         * @param _discountNetUnitPrice the _discount net unit price
         * @return the abstract ui position
         */
        public AbstractUIPosition setDiscountNetUnitPrice(final BigDecimal _discountNetUnitPrice)
        {
            discountNetUnitPrice = _discountNetUnitPrice;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #rateNetPrice}.
         *
         * @return value of instance variable {@link #rateNetPrice}
         */
        public BigDecimal getNetPrice()
        {
            return netPrice;
        }

        /**
         * Setter method for instance variable {@link #rateNetPrice}.
         *
         * @param _netPrice the _net price
         * @return the abstract ui position
         */
        public AbstractUIPosition setNetPrice(final BigDecimal _netPrice)
        {
            netPrice = _netPrice;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #crossPrice}.
         *
         * @return value of instance variable {@link #crossPrice}
         */
        public BigDecimal getCrossPrice()
        {
            return crossPrice;
        }

        /**
         * Setter method for instance variable {@link #rateCrossPrice}.
         *
         * @param _crossPrice the _cross price
         * @return the abstract ui position
         */
        public AbstractUIPosition setCrossPrice(final BigDecimal _crossPrice)
        {
            crossPrice = _crossPrice;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #discount}.
         *
         * @return value of instance variable {@link #discount}
         */
        public BigDecimal getDiscount()
        {
            return discount;
        }

        /**
         * Setter method for instance variable {@link #discount}.
         *
         * @param _discount value for instance variable {@link #discount}
         * @return the abstract ui position
         */
        public AbstractUIPosition setDiscount(final BigDecimal _discount)
        {
            discount = _discount;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #instance}.
         *
         * @return value of instance variable {@link #instance}
         */
        public Instance getInstance()
        {
            return instance;
        }

        /**
         * Setter method for instance variable {@link #instance}.
         *
         * @param _instance value for instance variable {@link #instance}
         * @return the abstract ui position
         */
        public AbstractUIPosition setInstance(final Instance _instance)
        {
            instance = _instance;
            return this;
        }

        /**
         * Gets the map4 java script.
         *
         * @param _parameter the _parameter
         * @return the map4 java script
         * @throws EFapsException on error
         */
        public Map<String, Object> getMap4JavaScript(final Parameter _parameter)
            throws EFapsException
        {
            final Map<String, Object> ret = new HashMap<>();
            final Type type = getDoc().getType4SysConf(_parameter);
            final DecimalFormat qtyFrmt = NumberFormatter.get().getFrmt4Quantity(type);
            final DecimalFormat upFrmt = NumberFormatter.get().getFrmt4UnitPrice(type);
            final DecimalFormat totFrmt = NumberFormatter.get().getFrmt4Total(type);
            final DecimalFormat disFrmt = NumberFormatter.get().getFrmt4Discount(type);

            ret.put("oid", InstanceUtils.isKindOf(instance, CISales.PositionAbstract)
                            ? instance.getOid() : "");
            ret.put("quantity", qtyFrmt.format(getQuantity()));
            ret.put("product", new String[] { getProdInstance().getOid(), getProdName() });
            ret.put("productDesc", getProdDescr());
            ret.put("uoM", getDoc().getUoMFieldStrByUoM(getUoM()));

            if (getNetUnitPrice() != null) {
                ret.put("netUnitPrice", upFrmt.format(getNetUnitPrice()));
                ret.put("netUnitPrice4Read", upFrmt.format(getNetUnitPrice()));
            }
            if (getDiscountNetUnitPrice() != null) {
                ret.put("discountNetUnitPrice", upFrmt.format(getDiscountNetUnitPrice()));
            }
            if (getNetPrice() != null) {
                ret.put("netPrice", totFrmt.format(getNetPrice()));
            }
            if (getDiscount() != null) {
                ret.put("discount", disFrmt.format(getDiscount()));
            }
            if (getCrossPrice() != null) {
                ret.put("crossPrice", totFrmt.format(getCrossPrice()));
            }
            return ret;
        }

        /**
         * Getter method for the instance variable {@link #prodName}.
         *
         * @return value of instance variable {@link #prodName}
         */
        public String getProdName()
        {
            return prodName;
        }

        /**
         * Setter method for instance variable {@link #prodName}.
         *
         * @param _prodName value for instance variable {@link #prodName}
         * @return the abstract ui position
         */
        public AbstractUIPosition setProdName(final String _prodName)
        {
            prodName = _prodName;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #prodDescr}.
         *
         * @return value of instance variable {@link #prodDescr}
         */
        public String getProdDescr()
        {
            return prodDescr;
        }

        /**
         * Setter method for instance variable {@link #prodDescr}.
         *
         * @param _prodDescr value for instance variable {@link #prodDescr}
         * @return the abstract ui position
         */
        public AbstractUIPosition setProdDescr(final String _prodDescr)
        {
            prodDescr = _prodDescr;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #uoM}.
         *
         * @return value of instance variable {@link #uoM}
         */
        public Long getUoM()
        {
            return uoMID;
        }

        /**
         * Setter method for instance variable {@link #uoM}.
         *
         * @param _long the _long
         * @return the abstract ui position
         */
        public AbstractUIPosition setUoM(final Long _long)
        {
            uoMID = _long;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #doc}.
         *
         * @return value of instance variable {@link #doc}
         */
        public AbstractDocument_Base getDoc()
        {
            return doc;
        }

        /**
         * Setter method for instance variable {@link #doc}.
         *
         * @param _doc value for instance variable {@link #doc}
         * @return the abstract ui position
         */
        public AbstractUIPosition setDoc(final AbstractDocument_Base _doc)
        {
            doc = _doc;
            return this;
        }

        /**
         * Gets the parameter.
         *
         * @return the parameter
         */
        public Parameter getParameter()
        {
            return parameter;
        }

        /**
         * Sets the parameter.
         *
         * @param _parameter the new parameter
         */
        public void setParameter(final Parameter _parameter)
        {
            parameter = _parameter;
        }
    }
}
