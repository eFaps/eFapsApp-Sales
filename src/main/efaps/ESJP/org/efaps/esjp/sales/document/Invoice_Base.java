/*
 * Copyright 2003 - 2021 The eFaps Team
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

package org.efaps.esjp.sales.document;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Instance;
import org.efaps.db.QueryBuilder;
import org.efaps.eql.EQL;
import org.efaps.eql.builder.Selectables;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CIMsgContacts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.StandartReport_Base.JasperActivation;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.contacts.taxid.Request;
import org.efaps.esjp.contacts.taxid.TaxIdInfo;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.sales.Channel;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;

/**
 *
 * @author The eFaps Team
 */
@EFapsUUID("43417000-af54-4cb5-a266-4e6df2ed793e")
@EFapsApplication("eFapsApp-Sales")
public abstract class Invoice_Base
    extends AbstractDocumentSum
{

    /**
     * Method for create a new Quotation.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final CreatedDoc createdDoc = createDoc(_parameter);
        createPositions(_parameter, createdDoc);
        connect2Derived(_parameter, createdDoc);
        connect2Object(_parameter, createdDoc);

        if (Sales.PERCEPTIONCERTIFICATEACTIVATE.get()) {
            new PerceptionCertificate().create4Doc(_parameter, createdDoc);
        }
        if (Sales.INVOICE_JASPERACTIVATION.get().contains(JasperActivation.ONCREATE)) {
            final File file = createReport(_parameter, createdDoc);
            if (file != null) {
                ret.put(ReturnValues.VALUES, file);
                ret.put(ReturnValues.TRUE, true);
            }
        }
        ret.put(ReturnValues.INSTANCE, createdDoc.getInstance());
        return ret;
    }

    /**
     * Method for create a new Quotation.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final EditedDoc editedDoc = editDoc(_parameter);
        updatePositions(_parameter, editedDoc);
        updateConnection2Object(_parameter, editedDoc);

        if (Sales.INVOICE_JASPERACTIVATION.get().contains(JasperActivation.ONEDIT)) {
            final File file = createReport(_parameter, editedDoc);
            if (file != null) {
                ret.put(ReturnValues.VALUES, file);
                ret.put(ReturnValues.TRUE, true);
            }
        }
        return ret;
    }

    @Override
    public CIType getCIType()
        throws EFapsException
    {
        return CISales.Invoice;
    }

    @Override
    protected void add2UpdateMap4Contact(final Parameter _parameter,
                                         final Instance _contactInstance,
                                         final Map<String, Object> _map)
        throws EFapsException
    {
        super.add2UpdateMap4Contact(_parameter, _contactInstance, _map);
        if (Sales.INVOICE_ACTIVATECONDITION.get()) {
            InterfaceUtils.appendScript4FieldUpdate(_map, new Channel().getConditionJs(_parameter, _contactInstance,
                            CISales.ChannelSalesCondition2Contact));
        }
        if (Sales.INVOICE_FROMDELIVERYNOTEAC.exists()) {
            final QueryBuilder queryBldr = new QueryBuilder(CISales.DeliveryNote);
            queryBldr.addWhereAttrEqValue(CISales.DeliveryNote.Status, Status.find(CISales.DeliveryNoteStatus.Open));
            InterfaceUtils.appendScript4FieldUpdate(_map, getJS4Doc4Contact(_parameter, _contactInstance,
                            CIFormSales.Sales_InvoiceForm.deliveryNotes.name, queryBldr));
        }
    }

    @Override
    protected StringBuilder add2JavaScript4DocumentContact(final Parameter _parameter,
                                                           final List<Instance> _instances,
                                                           final Instance _contactInstance)
        throws EFapsException
    {
        final StringBuilder ret = super.add2JavaScript4DocumentContact(_parameter, _instances, _contactInstance);
        if (Sales.INVOICE_ACTIVATECONDITION.get()) {
            ret.append(new Channel().getConditionJs(_parameter, _contactInstance,
                            CISales.ChannelSalesCondition2Contact));
        }
        if (Sales.INVOICE_FROMDELIVERYNOTEAC.exists()) {
            final QueryBuilder queryBldr = new QueryBuilder(CISales.DeliveryNote);
            queryBldr.addWhereAttrEqValue(CISales.DeliveryNote.Status, Status.find(CISales.DeliveryNoteStatus.Open));
            ret.append(getJS4Doc4Contact(_parameter, _contactInstance,
                            CIFormSales.Sales_InvoiceForm.deliveryNotes.name, queryBldr));
        }
        if (Sales.INVOICE_FROMORDERINBOUNDAC.exists()) {
            final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter,
                            Sales.INVOICE_FROMORDERINBOUNDAC.get());
            ret.append(getJS4Doc4Contact(_parameter, _contactInstance,
                            CIFormSales.Sales_InvoiceForm.orderInbound.name, queryBldr));
        }
        return ret;
    }

    @Override
    public Return validate(final Parameter _parameter)
        throws EFapsException
    {
        final Validation validation = new Validation();
        return validation.validate(_parameter, this, Sales.INVOICE_VALIDATION.get());
    }

    public Return autoComplete4TaxId(final Parameter _parameter)
        throws EFapsException
    {
        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        final var query = EQL.builder().nestedQuery(CIContacts.ClassOrganisation)
                        .where()
                        .attribute(CIContacts.ClassOrganisation.TaxNumber).like(input + "%").up()
                        .selectable(Selectables.attribute(CIContacts.ClassOrganisation.ContactLink));

        final var eval = EQL.builder().print().query(CIContacts.Contact)
                        .where().attribute(CIContacts.Contact.ID).in(query)
                        .select()
                        .attribute(CIContacts.Contact.OID, CIContacts.Contact.Name)
                        .clazz(CIContacts.ClassOrganisation).attribute(CIContacts.ClassOrganisation.TaxNumber)
                        .as("taxId")
                        .evaluate();
        final List<Map<String, String>> list = new ArrayList<>();
        while (eval.next()) {
            final String oid = eval.get(CIContacts.Contact.OID);
            final String name = eval.get(CIContacts.Contact.Name);
            final String taxId = eval.get("taxId");

            final Map<String, String> map = new HashMap<>();
            map.put(EFapsKey.AUTOCOMPLETE_KEY.getKey(), oid);
            map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), taxId);
            map.put(EFapsKey.AUTOCOMPLETE_CHOICE.getKey(), taxId + " - " + name);
            list.add(map);
        }
        Collections.sort(list, (_o1,
                                _o2) -> _o1.get(EFapsKey.AUTOCOMPLETE_CHOICE.getKey()).compareTo(
                                                _o2.get(EFapsKey.AUTOCOMPLETE_CHOICE.getKey())));
        if (list.isEmpty() && input.matches("^\\d{11}")) {
            final Map<String, String> map = new HashMap<>();
            map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), input);
            map.put(EFapsKey.AUTOCOMPLETE_CHOICE.getKey(), input + " - " + "Buscar en linea");
            list.add(map);
        }

        final var ret = new Return();
        ret.put(ReturnValues.VALUES, list);
        return ret;
    }

    public Return updateFields4TaxId(final Parameter _parameter)
        throws EFapsException
    {
        // get the value
        final String value = _parameter.getParameterValue("taxId");
        final var contactInst = Instance.get(value);
        final List<Map<String, Object>> list = new ArrayList<>();
        final Map<String, Object> map = new HashMap<>();
        list.add(map);
        String info = "";
        if (InstanceUtils.isKindOf(contactInst, CIContacts.ContactAbstract)) {
            final var eval = EQL.builder().print(contactInst)
                            .attribute(CIContacts.Contact.Name)
                            .msgPhrase(CIMsgContacts.ContactInfoMsgPhrase).as("info")
                            .evaluate();
            if (eval.next()) {
                final var html = eval.get(CIContacts.Contact.Name) + "<br><br>" + eval.get("info");
                info = StringEscapeUtils.escapeEcmaScript(html);
                map.put("fillContact", eval.inst().getOid());
            }
        } else {
            final var pattern = Pattern.compile("^\\d{11}");
            final var matcher = pattern.matcher(value);
            if (matcher.find()) {
                final var taxId = matcher.group();
                final var request = new Request();
                final var dto = request.getTaxpayer(taxId);
                info = new TaxIdInfo().getSnipplet4Taxpayer(_parameter, dto).toString();
                if (dto == null) {
                    map.put("fillContact", "NONE");
                } else {
                    map.put("fillContact", taxId);
                }
            }
        }
        map.put(EFapsKey.FIELDUPDATE_JAVASCRIPT.getKey(),
                        "document.getElementsByName('info')[0].innerHTML=\"" + info + "\";");
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    @Override
    protected Instance getContactInstance4Hint(final Parameter _parameter, final String _contactHint)
        throws EFapsException
    {
        final Instance ret;
        if ("NONE".equals(_contactHint)) {
            ret = null;
        } else {
            final var request = new Request();
            final var dto = request.getTaxpayer(_contactHint);
            ret = new TaxIdInfo().createContactFromTaxpayerDto(dto, true);
        }
        return ret;
    }
}
