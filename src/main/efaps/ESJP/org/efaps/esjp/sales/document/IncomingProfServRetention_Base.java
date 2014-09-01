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

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.SalesSettings;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: IncomingRetention_Base.java 13286 2014-07-10 00:56:02Z
 *          jan@moxter.net $
 */
@EFapsUUID("7b8de4b0-eba3-4eb5-b98b-9546393d9334")
@EFapsRevision("$Rev$")
public abstract class IncomingProfServRetention_Base
    extends AbstractDocumentTax
{

    /**
     * Used to store the PerceptionValue in the Context.
     */
    public static final String AMOUNTVALUE = IncomingRetention.class.getName() + ".AmountValue";

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(IncomingRetention.class);

    /**
     * Executed from a Command execute event to create a new Incoming
     * PerceptionCertificate.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Empty Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final String docOID = _parameter
                        .getParameterValue(CIFormSales.Sales_IncomingRetentionCreateForm.incomingInvoice.name);
        final Instance docInst = Instance.get(docOID);
        if (docInst != null && docInst.isValid()) {
            final CreatedDoc createdDoc = new CreatedDoc();
            createdDoc.setInstance(docInst);
            final DecimalFormat formatter = NumberFormatter.get().getFormatter();

            final PrintQuery print = new PrintQuery(docInst);
            print.addAttribute(CISales.DocumentSumAbstract.Date);
            print.addAttribute(CISales.DocumentSumAbstract.Contact);
            print.addAttribute(CISales.DocumentSumAbstract.Group);
            print.addAttribute(CISales.DocumentSumAbstract.Rate);
            print.addAttribute(CISales.DocumentSumAbstract.CurrencyId);
            print.addAttribute(CISales.DocumentSumAbstract.RateCurrencyId);
            print.execute();

            createdDoc.getValues().put(CISales.DocumentSumAbstract.Date.name,
                            print.getAttribute(CISales.DocumentSumAbstract.Date));
            createdDoc.getValues().put(CISales.DocumentSumAbstract.Contact.name,
                            print.getAttribute(CISales.DocumentSumAbstract.Contact));
            createdDoc.getValues().put(CISales.DocumentSumAbstract.Group.name,
                            print.getAttribute(CISales.DocumentSumAbstract.Group));
            createdDoc.getValues().put(CISales.DocumentSumAbstract.Rate.name,
                            print.getAttribute(CISales.DocumentSumAbstract.Rate));
            createdDoc.getValues().put(CISales.DocumentSumAbstract.CurrencyId.name,
                            print.getAttribute(CISales.DocumentSumAbstract.CurrencyId));
            createdDoc.getValues().put(CISales.DocumentSumAbstract.RateCurrencyId.name,
                            print.getAttribute(CISales.DocumentSumAbstract.RateCurrencyId));

            try {
                final String retentionValueStr = _parameter
                                .getParameterValue(CIFormSales.Sales_IncomingRetentionCreateForm.retentionValue.name);
                final BigDecimal retention;
                if (retentionValueStr != null && !retentionValueStr.isEmpty()) {
                    retention = (BigDecimal) formatter.parse(retentionValueStr);
                } else {
                    retention = BigDecimal.ZERO;
                }
                final IncomingRetention doc = new IncomingRetention();
                createdDoc.addValue(IncomingProfServRetention_Base.AMOUNTVALUE, retention);
                doc.createUpdate4Doc(_parameter, createdDoc);
            } catch (final ParseException p) {
                throw new EFapsException(IncomingRetention.class, "Perception.ParseException", p);
            }
        }
        return new Return();
    }

    @Override
    protected void connectDoc(final Parameter _parameter,
                              final CreatedDoc _origDoc,
                              final CreatedDoc _taxDoc)
        throws EFapsException
    {
        final Insert relInsert = new Insert(CISales.IncomingProfServRetention2IncomingProfServReceipt);
        relInsert.add(CISales.IncomingProfServRetention2IncomingProfServReceipt.FromLink, _taxDoc.getInstance());
        relInsert.add(CISales.IncomingProfServRetention2IncomingProfServReceipt.ToLink, _origDoc.getInstance());
        relInsert.execute();
    }

    /**
     * @param _parameter
     * @return type
     * @throws EFapsException
     *
     */
    @Override
    protected Type getType4create4Doc(final Parameter _parameter)
        throws EFapsException
    {
        return CISales.IncomingProfServRetention.getType();
    }

    protected String getDocName4Document(final Parameter _parameter,
                                         final Instance _instance)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_instance);
        print.addAttribute(CIERP.DocumentAbstract.Name);
        print.execute();

        return print.<String>getAttribute(CIERP.DocumentAbstract.Name);
    }

    /**
     * Executed from a Command execute event to edit.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        editDoc(_parameter);
        return new Return();
    }

    @Override
    protected Instance getRateCurrencyInstance(final Parameter _parameter,
                                               final CreatedDoc _createdDoc)
        throws EFapsException
    {
        Instance ret;
        if (_parameter.getInstance() != null && _parameter.getInstance().isValid()
                        && _parameter.getInstance().getType().isKindOf(CISales.IncomingProfServRetention.getType())) {
            final PrintQuery print = new PrintQuery(_parameter.getInstance());
            final SelectBuilder sel = SelectBuilder.get()
                            .linkfrom(CISales.IncomingProfServRetention2IncomingProfServReceipt.FromLink)
                            .linkto(CISales.IncomingProfServRetention2IncomingProfServReceipt.ToLink)
                            .linkto(CISales.IncomingProfServReceipt.RateCurrencyId).instance();
            print.addSelect(sel);
            print.execute();
            ret = print.getSelect(sel);
        } else {
            ret = _parameter.getParameterValue("rateCurrencyId") == null
                            ? Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE)
                            : Instance.get(CIERP.Currency.getType(), _parameter.getParameterValue("rateCurrencyId"));
        }
        return ret;
    }

    @Override
    protected Object[] getRateObject(final Parameter _parameter)
        throws EFapsException
    {
        Object[] ret;
        if (_parameter.getInstance() != null && _parameter.getInstance().isValid()
                        && _parameter.getInstance().getType().isKindOf(CISales.IncomingProfServRetention.getType())) {
            final PrintQuery print = new PrintQuery(_parameter.getInstance());
            final SelectBuilder sel = SelectBuilder.get()
                            .linkfrom(CISales.IncomingProfServRetention2IncomingProfServReceipt.FromLink)
                            .linkto(CISales.IncomingProfServRetention2IncomingProfServReceipt.ToLink)
                            .attribute(CISales.IncomingInvoice.Rate);
            print.addSelect(sel);
            print.execute();
            ret = print.getSelect(sel);
        } else {
            ret = super.getRateObject(_parameter);
        }
        return ret;
    }

    /**
     * @param _parameter
     * @param _instance
     */
    @Override
    protected String add2ChoiceAutoComplete4Doc(final Parameter _parameter,
                                                final Instance _instance)
        throws EFapsException
    {
        String ret = "";
        final PrintQuery print = new PrintQuery(_instance);
        final SelectBuilder selSymbol = new SelectBuilder()
                        .linkto(CISales.DocumentSumAbstract.RateCurrencyId).attribute(CIERP.Currency.Symbol);

        print.addAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
        print.addSelect(selSymbol);
        print.execute();
        final BigDecimal crossTotal = print.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
        final String symbol = print.<String>getSelect(selSymbol);
        if (crossTotal != null) {
            ret = " - " + symbol + " " + crossTotal;
        }
        return ret;
    }

    /**
     * @param _parameter
     * @param _instance
     */
    @Override
    protected String add2LabelUpdateFields4Doc(final Parameter _parameter,
                                               final Instance _instance)
        throws EFapsException
    {
        String ret = "";
        final PrintQuery print = new PrintQuery(_instance);
        final SelectBuilder selSymbol = new SelectBuilder()
                        .linkto(CISales.DocumentSumAbstract.RateCurrencyId).attribute(CIERP.Currency.Symbol);

        print.addAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
        print.addSelect(selSymbol);
        print.execute();
        final BigDecimal crossTotal = print.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
        final String symbol = print.<String>getSelect(selSymbol);
        if (crossTotal != null) {
            ret = " - " + symbol + " " + crossTotal;
        }
        return ret;
    }

    @Override
    protected void add2QueryBldr(final Parameter _parameter,
                                 final QueryBuilder _queryBldr)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CISales.IncomingProfServRetention);
        queryBldr.addType(CISales.IncomingProfServRetention, CISales.IncomingPerceptionCertificate);
        queryBldr.addWhereAttrNotEqValue(CISales.DocumentAbstract.StatusAbstract,
                        Status.find(CISales.IncomingDetractionStatus.Canceled),
                        Status.find(CISales.IncomingProfServRetentionStatus.Canceled),
                        Status.find(CISales.IncomingPerceptionCertificateStatus.Canceled));
        final AttributeQuery attrQuery = queryBldr.getAttributeQuery(CISales.DocumentAbstract.ID);

        final QueryBuilder queryBldr2 = new QueryBuilder(CISales.IncomingDocumentTax2Document);
        queryBldr2.addWhereAttrInQuery(CISales.IncomingDocumentTax2Document.FromAbstractLink, attrQuery);
        final AttributeQuery attrQuery2 = queryBldr2
                        .getAttributeQuery(CISales.IncomingDocumentTax2Document.ToAbstractLink);

        final QueryBuilder queryBldr3 = new QueryBuilder(CISales.IncomingInvoice);
        queryBldr3.setOr(true);
        queryBldr3.addWhereAttrEqValue(CISales.IncomingInvoice.Status, Status.find(CISales.IncomingInvoiceStatus.Open));
        queryBldr3.addWhereAttrEqValue(CISales.IncomingInvoice.Status, Status.find(CISales.IncomingInvoiceStatus.Paid));
        queryBldr3.addWhereAttrEqValue(CISales.IncomingInvoice.Status,
                        Status.find(CISales.IncomingInvoiceStatus.Digitized));
        final AttributeQuery attrQuery3 = queryBldr3.getAttributeQuery(CISales.IncomingInvoice.ID);

        final QueryBuilder queryBldr4 = new QueryBuilder(CISales.IncomingInvoice);
        queryBldr4.addWhereAttrInQuery(CISales.IncomingInvoice.ID, attrQuery3);
        queryBldr4.addWhereAttrNotInQuery(CISales.IncomingInvoice.ID, attrQuery2);
        final AttributeQuery attrQuery4 = queryBldr4.getAttributeQuery(CISales.IncomingInvoice.ID);

        _queryBldr.addWhereAttrInQuery(CISales.DocumentAbstract.ID, attrQuery4);
    }

    @Override
    public Return getJavaScriptUIValue(final Parameter _parameter)
        throws EFapsException
    {
        final StringBuilder js = new StringBuilder();
        js.append("<script type=\"text/javascript\">\n")
                        .append("require([\"dojo/ready\"],")
                        .append(" function(ready){\n")
                        .append(" ready(1500, function(){")
                        .append(getSetFieldReadOnlyScript(_parameter, "retentionValue"))
                        .append(getSetFieldReadOnlyScript(_parameter, "totalAmount"))
                        .append("});").append("});\n</script>\n");
        final Return retVal = new Return();
        retVal.put(ReturnValues.SNIPLETT, js.toString());
        return retVal;
    }
}
