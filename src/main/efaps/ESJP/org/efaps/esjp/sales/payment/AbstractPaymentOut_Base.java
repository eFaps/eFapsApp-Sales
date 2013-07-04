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


package org.efaps.esjp.sales.payment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringEscapeUtils;
import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.RateUI;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uiform.Field;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.esjp.sales.PriceUtil;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.SalesSettings;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("9482d7a1-86dd-40f9-b4f6-6a2eac106aeb")
@EFapsRevision("$Rev$")
public abstract class AbstractPaymentOut_Base
    extends AbstractPaymentDocument
{

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
        String ret;
        if (_parameter.getInstance() != null
                        && _parameter.getInstance().getType().isKindOf(CISales.BulkPayment.getType())) {
            final PrintQuery print = new PrintQuery(_parameter.getInstance());
            print.addAttribute(CISales.BulkPayment.Name, CISales.BulkPayment.Date);
            print.execute();
            final String name = print.<String>getAttribute(CISales.BulkPayment.Name);
            ret = name + " - " + String.format("%02d", 1);

            _parameter.getParameters().put(getFieldName4Attribute(_parameter,
                        CISales.PaymentDocumentAbstract.Date.name),
                        new String[] {print.<DateTime>getAttribute(CISales.BulkPayment.Date).toString()});

            final QueryBuilder accQueryBldr = new QueryBuilder(CISales.BulkPayment2Account);
            accQueryBldr.addWhereAttrEqValue(CISales.BulkPayment2Account.FromLink, _parameter.getInstance().getId());
            final MultiPrintQuery accMulti = accQueryBldr.getPrint();
            accMulti.addAttribute(CISales.BulkPayment2Account.ToLink);
            accMulti.execute();
            if (accMulti.next()) {
                _parameter.getParameters().put("account",
                            new String[] {accMulti.<Long>getAttribute(CISales.BulkPayment2Account.ToLink).toString()});
            }

            final Set<String> names = new HashSet<String>();
            final QueryBuilder queryBldr = new QueryBuilder(CISales.BulkPayment2PaymentDocument);
            queryBldr.addWhereAttrEqValue(CISales.BulkPayment2PaymentDocument.FromLink, _parameter.getInstance().getId());
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder sel = new SelectBuilder().linkto(CISales.BulkPayment2PaymentDocument.ToLink)
                            .attribute(CISales.PaymentDocumentOutAbstract.Name);
            multi.addSelect(sel);
            multi.execute();
            while (multi.next()) {
                names.add(multi.<String>getSelect(sel));
            }
            for (int i = 1; i< 100; i++) {
                final String nameTmp = name + " - " + String.format("%02d", i);
                if (!names.contains(nameTmp)) {
                    ret = nameTmp;
                    break;
                }
            }
        } else {
            ret = super.getDocName4Create(_parameter);
        }
        return ret;
    }


    @Override
    protected CreatedDoc createDoc(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc ret = super.createDoc(_parameter);

        // in case of bulkpayment connect the paymentdoc to the bulkpayment
        if (_parameter.getInstance() != null
                        && _parameter.getInstance().getType().isKindOf(CISales.BulkPayment.getType())) {
            final Insert insert = new Insert(CISales.BulkPayment2PaymentDocument);
            insert.add(CISales.BulkPayment2PaymentDocument.FromLink, _parameter.getInstance().getId());
            insert.add(CISales.BulkPayment2PaymentDocument.ToLink, ret.getInstance().getId());
            insert.execute();
        }
        return ret;
    }


    public Return getJavaScriptUIValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final StringBuilder html = new StringBuilder().append("<script type=\"text/javascript\">/*<![CDATA[*/");
        final Instance instance = _parameter.getInstance();
        if (instance != null && instance.getType().isKindOf(CISales.BulkPayment.getType())) {
            final QueryBuilder queryBldr = new QueryBuilder(CISales.BulkPayment2Account);
            queryBldr.addWhereAttrEqValue(CISales.BulkPayment2Account.FromLink, instance.getId());
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CISales.BulkPayment2Account.ToLink);
            multi.execute();
            multi.next();
            html.append("document.getElementsByName('account')[0].value=")
                            .append(multi.getAttribute(CISales.BulkPayment2Account.ToLink)).append(";\n")
                            .append("document.getElementsByName('account')[0].disabled = true;\n");
            html.append(addCurrencyValue(_parameter, multi.<Long>getAttribute(CISales.BulkPayment2Account.ToLink)));
        }
        html.append("/*]]>*/ </script>");
        ret.put(ReturnValues.SNIPLETT, html.toString());
        return ret;
    }

    protected String addCurrencyValue(final Parameter _parameter,
                                      final Long _accountID)
        throws EFapsException
    {
        final StringBuilder ret = new StringBuilder();
        final PrintQuery print = new PrintQuery(CISales.AccountCashDesk.getType(), _accountID);
        print.addAttribute(CISales.AccountCashDesk.CurrencyLink);
        print.execute();

        final Instance newInst = Instance.get(CIERP.Currency.getType(),
                        print.<Long>getAttribute(CISales.AccountCashDesk.CurrencyLink));

        // Sales-Configuration
        final Instance baseInst = SystemConfiguration.get(UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f"))
                        .getLink("CurrencyBase");

        final PrintQuery print2 = new PrintQuery(_parameter.getInstance());
        print2.addAttribute(CISales.BulkPayment.Date);
        print2.execute();
        _parameter.getParameters()
                        .put("date_eFapsDate",
                                        new String[] { print2.<DateTime>getAttribute(CISales.BulkPayment.Date)
                                                        .toString("dd/MM/YYYY") });

        final BigDecimal[] rates = new PriceUtil().getRates(_parameter, newInst, baseInst);
        ret.append("document.getElementsByName('rate')[0].value='").append(rates[3].stripTrailingZeros()).append("';\n")
            .append("document.getElementsByName('rate").append(RateUI.INVERTEDSUFFIX)
            .append("')[0].value='").append(rates[3].compareTo(rates[0]) != 0).append("';\n");
        return ret.toString();
    }

    public Return checkAccess4BulkPayment(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Instance instance = _parameter.getInstance();
        if (instance == null || !instance.getType().isKindOf(CISales.BulkPayment.getType())) {
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    public Return getPaymentDocuments4Pay(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new MultiPrint()
        {
            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.DocumentSumAbstract);
                final Object[] ids = getPayableDocuments(_parameter);
                attrQueryBldr.addWhereAttrNotEqValue(CISales.DocumentSumAbstract.Type, ids);
                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.DocumentSumAbstract.ID);

                final QueryBuilder attrQueryBldr2 = new QueryBuilder(CISales.Payment);
                attrQueryBldr2.addWhereAttrInQuery(CISales.Payment.CreateDocument, attrQuery);
                final AttributeQuery attrQuery2 = attrQueryBldr2.getAttributeQuery(CISales.Payment.TargetDocument);

                _queryBldr.addWhereAttrInQuery(CISales.PaymentDocumentOutAbstract.ID, attrQuery2);

            }
        }.execute(_parameter);
        return ret;
    }

    protected Object[] getPayableDocuments(final Parameter _parameter)
        throws EFapsException
    {
        final String uuidsStr = Sales.getSysConfig().getAttributeValue(SalesSettings.PAYABLEDOCS);
        final String[] uuids = uuidsStr.split(";");
        final List<Long> lstIds = new ArrayList<Long>();
        for (final String uuid : uuids) {
            final Type type = Type.get(UUID.fromString(uuid));
            lstIds.add(type.getId());
        }

        return lstIds.toArray();
    }

    public Return dropDown4CreateDocuments(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Field()
        {
            @Override
            protected void add2QueryBuilder4List(final Parameter _parameter,
                                                 final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Payment);
                attrQueryBldr.addWhereAttrEqValue(CISales.Payment.TargetDocument, _parameter.getInstance().getId());
                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.Payment.CreateDocument);

                _queryBldr.addWhereAttrInQuery(CISales.DocumentSumAbstract.ID, attrQuery);
            }
        }.dropDownFieldValue(_parameter);
        return ret;
    }

    public Return updateField4PayableDocuments(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instDoc = Instance.get(_parameter
                        .getParameterValue(CIFormSales.Sales_PaymentCheckOut4PayPaymentForm.createExistDocument.name));

        final StringBuilder js = new StringBuilder();
        js.append("var select = document.getElementsByName('")
                        .append(CIFormSales.Sales_PaymentCheckOut4PayPaymentForm.createExistRelatedDocument.name)
                        .append("')[0];")
                        .append(" select.options.length = 0; ");

        final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Document2DocumentAbstract);
        attrQueryBldr.addWhereAttrEqValue(CISales.Document2DocumentAbstract.FromAbstractLink, instDoc.getId());
        final AttributeQuery attrQuery = attrQueryBldr
                        .getAttributeQuery(CISales.Document2DocumentAbstract.ToAbstractLink);

        final QueryBuilder queryBldr = new QueryBuilder(CISales.DocumentSumAbstract);
        queryBldr.addWhereAttrInQuery(CISales.DocumentSumAbstract.ID, attrQuery);
        queryBldr.addWhereAttrEqValue(CISales.DocumentSumAbstract.Type, getPayableDocuments(_parameter));

        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.DocumentSumAbstract.Name);
        multi.execute();
        while (multi.next()) {
            final String typeLabel = multi.getCurrentInstance().getType().getLabel();
            final String name = multi.<String>getAttribute(CISales.DocumentSumAbstract.Name);
            final Instance inst = multi.getCurrentInstance();
            js.append(" select.options[select.options.length] = new Option('")
                .append(StringEscapeUtils.escapeEcmaScript(StringEscapeUtils.escapeHtml4(typeLabel + " - " + name)))
                .append("','").append(inst.getOid()).append("'); ");

        }

        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(EFapsKey.FIELDUPDATE_JAVASCRIPT.getKey(), js.toString());
        list.add(map);

        final Return ret = new Return();
        ret.put(ReturnValues.VALUES, list);
        return ret;
    }

    public Return updatePayableDocuments(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Instance instDoc = Instance.get(_parameter
                        .getParameterValue(CIFormSales.Sales_PaymentCheckOut4PayPaymentForm.createExistDocument.name));
        final QueryBuilder queryBldr = new QueryBuilder(CISales.Payment);
        queryBldr.addWhereAttrEqValue(CISales.Payment.TargetDocument, _parameter.getCallInstance().getId());
        queryBldr.addWhereAttrEqValue(CISales.Payment.CreateDocument, instDoc.getId());

        final InstanceQuery query = queryBldr.getQuery();
        query.execute();
        if (query.next()) {
            Instance newInstDoc = null;
            if (_parameter.getParameterValue(CIFormSales.Sales_PaymentCheckOut4PayPaymentForm
                            .createExistRelatedDocument.name) != null
                            && !_parameter.getParameterValue(CIFormSales.Sales_PaymentCheckOut4PayPaymentForm
                                            .createExistRelatedDocument.name).isEmpty()) {
                newInstDoc = Instance.get(_parameter.getParameterValue(CIFormSales.
                                Sales_PaymentCheckOut4PayPaymentForm.createExistRelatedDocument.name));
            } else {
                newInstDoc = Instance.get(_parameter.getParameterValue("createDocument"));
            }
            if (newInstDoc != null && newInstDoc.isValid()) {
                final Update updatePay = new Update(query.getCurrentValue());
                updatePay.add(CISales.Payment.CreateDocument, newInstDoc.getId());
                updatePay.execute();

                final Insert insert = new Insert(CISales.PayableDocument2Document);
                insert.add(CISales.PayableDocument2Document.FromLink, newInstDoc.getId());
                insert.add(CISales.PayableDocument2Document.ToLink, instDoc.getId());
                insert.add(CISales.PayableDocument2Document.PayDocLink, query.getCurrentValue().getId());
                insert.execute();

                final PrintQuery printPay = new PrintQuery(query.getCurrentValue());
                printPay.addAttribute(CISales.Payment.Amount);
                printPay.execute();
                final BigDecimal amountPay = printPay.<BigDecimal>getAttribute(CISales.Payment.Amount);

                final PrintQuery printDoc = new PrintQuery(newInstDoc);
                printDoc.addAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
                printDoc.execute();
                final BigDecimal amountDoc = printDoc
                                .<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);

                if (amountDoc.compareTo(amountPay) == 0) {
                    final Update updateDoc = new Update(newInstDoc);
                    updateDoc.add(CISales.DocumentSumAbstract.StatusAbstract,
                                    Status.find(newInstDoc.getType().getStatusAttribute().getLink().getUUID(), "Paid")
                                                    .getId());
                    updateDoc.execute();
                }

            }
        }

        return ret;
    }

}
