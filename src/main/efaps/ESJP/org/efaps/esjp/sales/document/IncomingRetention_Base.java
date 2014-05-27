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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.Calculator;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("6c067449-1aba-42ea-b259-38a3d1e32658")
@EFapsRevision("$Rev$")
public abstract class IncomingRetention_Base
    extends DocumentSum
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
     * Executed from a Command execute event to create a new Incoming PerceptionCertificate.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final String docOID = _parameter.getParameterValue(CIFormSales.Sales_IncomingRetentionCreateForm.incomingInvoice.name);
        final Instance docInst = Instance.get(docOID);
        if(docInst != null && docInst.isValid()) {
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

            createdDoc.getValues().put(CISales.DocumentSumAbstract.Date.name, print.getAttribute(CISales.DocumentSumAbstract.Date));
            createdDoc.getValues().put(CISales.DocumentSumAbstract.Contact.name, print.getAttribute(CISales.DocumentSumAbstract.Contact));
            createdDoc.getValues().put(CISales.DocumentSumAbstract.Group.name, print.getAttribute(CISales.DocumentSumAbstract.Group));
            createdDoc.getValues().put(CISales.DocumentSumAbstract.Rate.name, print.getAttribute(CISales.DocumentSumAbstract.Rate));
            createdDoc.getValues().put(CISales.DocumentSumAbstract.CurrencyId.name, print.getAttribute(CISales.DocumentSumAbstract.CurrencyId));
            createdDoc.getValues().put(CISales.DocumentSumAbstract.RateCurrencyId.name, print.getAttribute(CISales.DocumentSumAbstract.RateCurrencyId));

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
                createdDoc.addValue(IncomingRetention_Base.AMOUNTVALUE, retention);
                doc.create4Doc(_parameter, createdDoc, -1);
            } catch (final ParseException p) {
                throw new EFapsException(IncomingRetention.class, "Perception.ParseException", p);
            }
        }
        return new Return();
    }

    /**
     * Executed from a Command execute event to create a new Incoming PerceptionCertificate.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @param _createdDoc as CreatedDoc with values.
     * @param _index > 0 if position, -1 if doc itself
     * @throws EFapsException on error
     */
    public void create4Doc(final Parameter _parameter,
                           final CreatedDoc _createdDoc,
                           final int _index)
        throws EFapsException
    {
        final BigDecimal amount = (BigDecimal) _createdDoc.getValue(IncomingRetention_Base.AMOUNTVALUE);
        final Instance documentInst;
        if (_index < 0) {
            documentInst = _createdDoc.getInstance();
        } else {
            documentInst = _createdDoc.getPositions().get(_index);
        }

        final Insert insert = new Insert(CISales.IncomingRetention);
        insert.add(CISales.IncomingRetention.Date, _createdDoc.getValues()
                        .get(CISales.PaymentDocumentAbstract.Date.name));
        insert.add(CISales.IncomingRetention.Contact, _createdDoc.getValues()
                        .get(CISales.PaymentDocumentAbstract.Contact.name));
        insert.add(CISales.IncomingRetention.Salesperson, Context.getThreadContext().getPersonId());
        insert.add(CISales.IncomingRetention.Group, _createdDoc.getValues()
                        .get(CISales.PaymentDocumentAbstract.Group.name));
        insert.add(CISales.IncomingRetention.Rate, _createdDoc.getValues()
                        .get(CISales.PaymentDocumentAbstract.Rate.name));
        if (_createdDoc.getValues().containsKey(CISales.PaymentDocumentAbstract.CurrencyLink.name)) {
            insert.add(CISales.IncomingRetention.CurrencyId, _createdDoc.getValues()
                        .get(CISales.PaymentDocumentAbstract.CurrencyLink.name));
            insert.add(CISales.IncomingRetention.RateCurrencyId, _createdDoc.getValues()
                        .get(CISales.PaymentDocumentAbstract.RateCurrencyLink.name));
        } else {
            insert.add(CISales.IncomingRetention.CurrencyId, _createdDoc.getValues()
                            .get(CISales.DocumentSumAbstract.CurrencyId.name));
            insert.add(CISales.IncomingDetraction.RateCurrencyId, _createdDoc.getValues()
                            .get(CISales.IncomingRetention.RateCurrencyId.name));
        }

        insert.add(CISales.IncomingRetention.RateNetTotal, BigDecimal.ZERO);
        insert.add(CISales.IncomingRetention.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.IncomingRetention.NetTotal, BigDecimal.ZERO);
        insert.add(CISales.IncomingRetention.DiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.IncomingRetention.Status, Status.find(CISales.IncomingRetentionStatus.Open));
        insert.add(CISales.IncomingRetention.Name, getDocName4Document(_parameter, documentInst));

        final Object[] rateObj = (Object[]) _createdDoc.getValue(CISales.DocumentSumAbstract.Rate.name);
        final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                        BigDecimal.ROUND_HALF_UP);
        final DecimalFormat totalFrmt = NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter));
        final int scale = totalFrmt.getMaximumFractionDigits();

        insert.add(CISales.IncomingRetention.RateCrossTotal, amount.setScale(scale, BigDecimal.ROUND_HALF_UP));
        insert.add(CISales.IncomingRetention.CrossTotal,
                        amount.divide(rate, BigDecimal.ROUND_HALF_UP).setScale(scale, BigDecimal.ROUND_HALF_UP));
        insert.execute();

        final Insert relInsert = new Insert(CISales.IncomingRetention2IncomingInvoice);
        relInsert.add(CISales.IncomingRetention2IncomingInvoice.FromLink, insert.getInstance());
        relInsert.add(CISales.IncomingRetention2IncomingInvoice.ToLink, documentInst);
        relInsert.execute();
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
    protected BigDecimal getCrossTotal(final Parameter _parameter,
                                       final List<Calculator> _calcList)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        if (_calcList.isEmpty()) {
            final DecimalFormat formatter = NumberFormatter.get().getFormatter();
            try {
                final BigDecimal rateCrossTotal = (BigDecimal) formatter.parse(_parameter
                                .getParameterValue(CIFormSales.Sales_IncomingRetentionForm.crossTotal.name));
                ret = ret.add(rateCrossTotal);
            } catch (final ParseException p) {
                throw new EFapsException(IncomingRetention.class, "RateCrossTotal.ParseException", p);
            }
        } else {
            ret = super.getCrossTotal(_parameter, _calcList);
        }

        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return containing maplist
     * @throws EFapsException on error
     */
    public Return updateFields4RetentionPercent(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();

        final String docOID = _parameter.getParameterValue(CIFormSales.Sales_IncomingRetentionCreateForm.incomingInvoice.name);
        final Instance docInst = Instance.get(docOID);

        if(docInst != null && docInst.isValid()) {

            final List<Calculator> calcList = getCalulators4Doc(_parameter, docInst);

            final String retentionPercentStr = _parameter
                            .getParameterValue(CIFormSales.Sales_IncomingRetentionCreateForm.retentionPercent.name);
            if (retentionPercentStr != null && !retentionPercentStr.isEmpty()) {
                final DecimalFormat formatter = NumberFormatter.get().getFormatter();
                try {
                    final BigDecimal retentionPercent = (BigDecimal) formatter.parse(retentionPercentStr);
                    final BigDecimal crossTotal = getCrossTotal(_parameter, calcList);
                    final BigDecimal retention = crossTotal.multiply(retentionPercent
                                    .setScale(8, BigDecimal.ROUND_HALF_UP)
                                    .divide(new BigDecimal(100), BigDecimal.ROUND_HALF_UP));
                    final String retentionStr = NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter))
                                    .format(retention);
                    map.put(CIFormSales.Sales_IncomingRetentionCreateForm.retentionValue.name, retentionStr);
                } catch (final ParseException e) {
                    IncomingRetention_Base.LOG.error("Catched parsing error", e);
                }
            }

            if (calcList.size() > 0) {
                list.add(map);
                retVal.put(ReturnValues.VALUES, list);
            }
        }
        return retVal;
    }

}
