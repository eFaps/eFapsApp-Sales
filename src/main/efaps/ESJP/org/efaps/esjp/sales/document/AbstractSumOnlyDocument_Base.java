/*
 * Copyright 2003 - 2013 The eFaps Team
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


package org.efaps.esjp.sales.document;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.SalesSettings;
import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("31c806ae-c891-4797-a85b-71113a79c02e")
@EFapsRevision("$Rev$")
public abstract class AbstractSumOnlyDocument_Base
    extends DocumentSum
{
    /**
     * Method to create the basic Document. The method checks for the Type to be
     * created for every attribute if a related field is in the parameters.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return Instance of the document.
     * @throws EFapsException on error.
     */
    @Override
    protected CreatedDoc createDoc(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = new CreatedDoc();

        final Instance baseCurrInst = Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);
        final Instance rateCurrInst = _parameter.getParameterValue("rateCurrencyId") == null
                        ? baseCurrInst
                        : Instance.get(CIERP.Currency.getType(), _parameter.getParameterValue("rateCurrencyId"));

        final Object[] rateObj = getRateObject(_parameter);
        final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12, BigDecimal.ROUND_HALF_UP);

        final Insert insert = new Insert(getType4DocCreate(_parameter));
        final String name = getDocName4Create(_parameter);
        insert.add(CISales.DocumentSumAbstract.Name, name);
        createdDoc.getValues().put(CISales.DocumentSumAbstract.Name.name, name);
        final String date = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentSumAbstract.Date.name));
        if (date != null) {
            insert.add(CISales.DocumentSumAbstract.Date, date);
            createdDoc.getValues().put(CISales.DocumentSumAbstract.Date.name, date);
        }
        final String duedate = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentSumAbstract.DueDate.name));
        if (duedate != null) {
            insert.add(CISales.DocumentSumAbstract.DueDate, duedate);
            createdDoc.getValues().put(CISales.DocumentSumAbstract.DueDate.name, duedate);
        }
        final String contact = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentSumAbstract.Contact.name));
        if (contact != null) {
            final Instance inst = Instance.get(contact);
            if (inst.isValid()) {
                insert.add(CISales.DocumentSumAbstract.Contact, inst.getId());
                createdDoc.getValues().put(CISales.DocumentSumAbstract.Contact.name, inst);
            }
        }
        final String note = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentSumAbstract.Note.name));
        if (note != null) {
            insert.add(CISales.DocumentSumAbstract.Note, note);
            createdDoc.getValues().put(CISales.DocumentSumAbstract.Note.name, note);
        }
        final String revision = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentSumAbstract.Revision.name));
        if (revision != null) {
            insert.add(CISales.DocumentSumAbstract.Revision, revision);
            createdDoc.getValues().put(CISales.DocumentSumAbstract.Revision.name, revision);
        }
        final String salesperson = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentSumAbstract.Salesperson.name));
        if (salesperson != null) {
            insert.add(CISales.DocumentSumAbstract.Salesperson, salesperson);
            createdDoc.getValues().put(CISales.DocumentSumAbstract.Salesperson.name, salesperson);
        }

        final String groupId = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.DocumentSumAbstract.Group.name));
        if (groupId != null) {
            insert.add(CISales.DocumentSumAbstract.Group, groupId);
            createdDoc.getValues().put(CISales.DocumentSumAbstract.Group.name, groupId);
        }

        final DecimalFormat frmt = NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter));
        final int scale = frmt.getMaximumFractionDigits();

        try {
            final String rateNetTotalStr = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                            CISales.DocumentSumAbstract.RateNetTotal.name));

            if (rateNetTotalStr != null && !rateNetTotalStr.isEmpty()) {
                final BigDecimal rateNetTotal = (BigDecimal) frmt.parse(rateNetTotalStr);
                insert.add(CISales.DocumentSumAbstract.RateNetTotal, rateNetTotal);
                createdDoc.getValues().put(CISales.DocumentSumAbstract.RateNetTotal.name, rateNetTotal);

                final BigDecimal netTotal = rateNetTotal.divide(rate, BigDecimal.ROUND_HALF_UP).setScale(scale,
                                BigDecimal.ROUND_HALF_UP);
                insert.add(CISales.DocumentSumAbstract.NetTotal, netTotal);
                createdDoc.getValues().put(CISales.DocumentSumAbstract.NetTotal.name, netTotal);
            }

            final String rateCrossTotalStr = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                            CISales.DocumentSumAbstract.RateCrossTotal.name));
            if (rateCrossTotalStr != null && !rateCrossTotalStr.isEmpty()) {
                final BigDecimal rateCrossTotal = (BigDecimal) frmt.parse(rateCrossTotalStr);
                insert.add(CISales.DocumentSumAbstract.RateCrossTotal, rateCrossTotal);
                createdDoc.getValues().put(CISales.DocumentSumAbstract.RateCrossTotal.name, rateCrossTotal);

                final BigDecimal crossTotal = rateCrossTotal.divide(rate, BigDecimal.ROUND_HALF_UP).setScale(scale,
                                BigDecimal.ROUND_HALF_UP);
                insert.add(CISales.DocumentSumAbstract.CrossTotal, crossTotal);
                createdDoc.getValues().put(CISales.DocumentSumAbstract.CrossTotal.name, crossTotal);
                // if not added yet do it now to prevent error
                if (!createdDoc.getValues().containsKey(CISales.DocumentSumAbstract.RateNetTotal.name)) {
                    insert.add(CISales.DocumentSumAbstract.RateNetTotal, rateCrossTotal);
                    createdDoc.getValues().put(CISales.DocumentSumAbstract.NetTotal.name, rateCrossTotal);
                    insert.add(CISales.DocumentSumAbstract.NetTotal, crossTotal);
                    createdDoc.getValues().put(CISales.DocumentSumAbstract.NetTotal.name, crossTotal);
                }
            }
        } catch (final ParseException e) {
           throw new EFapsException("Parsing Error", e);
        }

        insert.add(CISales.DocumentSumAbstract.DiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.DocumentSumAbstract.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.DocumentSumAbstract.CurrencyId, baseCurrInst);
        createdDoc.getValues().put(CISales.DocumentSumAbstract.CurrencyId.name, baseCurrInst);
        insert.add(CISales.DocumentSumAbstract.Rate, rateObj);
        createdDoc.getValues().put(CISales.DocumentSumAbstract.Rate.name, rateObj);
        insert.add(CISales.DocumentSumAbstract.RateCurrencyId, rateCurrInst);
        createdDoc.getValues().put(CISales.DocumentSumAbstract.RateCurrencyId.name, rateCurrInst);


        addStatus2DocCreate(_parameter, insert, createdDoc);
        add2DocCreate(_parameter, insert, createdDoc);
        insert.execute();

        createdDoc.setInstance(insert.getInstance());

        Context.getThreadContext().removeSessionAttribute(AbstractDocument_Base.CURRENCYINST_KEY);
        return createdDoc;
    }

    public Return validateConnectDocument(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Map<?, ?> others = (HashMap<?, ?>) _parameter.get(ParameterValues.OTHERS);
        final StringBuilder html = new StringBuilder();
        final String[] childOids = (String[]) others.get("selectedRow");
        boolean validate = true;
        if (childOids != null) {
            final Instance callInstance = _parameter.getCallInstance();
            for (final String childOid : childOids) {
                final Instance child = Instance.get(childOid);
                if (callInstance.getType().isKindOf(CISales.DocumentSumAbstract.getType())) {
                    if (child.getType().equals(CISales.IncomingInvoice.getType())
                                    && check4Relation(CISales.IncomingExchange2IncomingInvoice.uuid, child).next()) {
                        validate = false;
                        html.append(getString4ReturnInvalidate(child));
                        break;
                    } else if (child.getType().equals(CISales.Invoice.getType())
                                    && check4Relation(CISales.Exchange2Invoice.uuid, child).next()) {
                        validate = false;
                        html.append(getString4ReturnInvalidate(child));
                        break;
                    }
                }
            }
            if (validate) {
                ret.put(ReturnValues.TRUE, true);
                html.append(DBProperties.getProperty(this.getClass().getName() + ".validateConnectDoc"));
                ret.put(ReturnValues.SNIPLETT, html.toString());
            } else {
                html.insert(0, DBProperties.getProperty(this.getClass().getName() + ".invalidateConnectDoc")
                                + "<p>");
                ret.put(ReturnValues.SNIPLETT, html.toString());
            }
        }
        return ret;
    }

    protected MultiPrintQuery check4Relation(final UUID _typeUUID,
                                             final Instance _instance)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(_typeUUID);
        queryBldr.addWhereAttrMatchValue(CISales.Document2DocumentAbstract.ToAbstractLink, _instance.getId());
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.Document2DocumentAbstract.OID);
        multi.execute();
        return multi;
    }

    protected StringBuilder getString4ReturnInvalidate(final Instance _child)
        throws EFapsException
    {
        final StringBuilder html = new StringBuilder();
        final PrintQuery print = new PrintQuery(_child);
        print.addAttribute(CISales.DocumentAbstract.Name);
        print.execute();
        return html.append(_child.getType().getLabel()
                        + " - " + print.<String>getAttribute(CISales.DocumentAbstract.Name));
    }

    public Return getJavaScript4Search(final Parameter _parameter)
        throws EFapsException
    {
        final StringBuilder js = new StringBuilder();
        final Instance instance = _parameter.getInstance() != null
                        ? _parameter.getInstance() : _parameter.getCallInstance();
        js.append("<script type=\"text/javascript\">\n");
        if (instance.isValid()
                        && (instance.getType().equals(CISales.IncomingExchange.getType())
                        || instance.getType().equals(CISales.Exchange.getType()))) {
            final SelectBuilder selContactId = new SelectBuilder()
                            .linkto(CISales.DocumentSumAbstract.Contact).id();
            final SelectBuilder selContactName = new SelectBuilder()
                            .linkto(CISales.DocumentSumAbstract.Contact).attribute(CIContacts.Contact.Name);
            final PrintQuery print = new PrintQuery(instance);
            print.addSelect(selContactId, selContactName);
            print.execute();

            js.append(getSetFieldValue(0, "contact", String.valueOf(print.<Long>getSelect(selContactId)))).append("\n")
                .append(getSetFieldValue(0, "contactAutoComplete", print.<String>getSelect(selContactName)));
        }
        js.append("\n</script>\n");
        final Return retVal = new Return();
        retVal.put(ReturnValues.SNIPLETT, js.toString());
        return retVal;
    }
}
