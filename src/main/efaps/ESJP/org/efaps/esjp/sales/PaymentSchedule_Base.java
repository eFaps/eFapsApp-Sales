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

package org.efaps.esjp.sales;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.SalesSettings;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("806c701d-ca2a-4e71-b6b1-7777a77299e4")
@EFapsRevision("$Rev: 1$")
public abstract class PaymentSchedule_Base
    extends EventSchedule
{

    /**
     * Method for create a new PaymentSchedule.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = createDoc(_parameter);
        createPositions(_parameter, createdDoc);

        return new Return();
    }

    @Override
    protected void add2DocCreate(final Parameter _parameter,
                                 final Insert _insert,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final Instance baseCurrInst = Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);

        _insert.add(CISales.PaymentSchedule.Total, getTotalFmtStr(getTotal(_parameter, null)));
        _insert.add(CISales.PaymentSchedule.CurrencyId, baseCurrInst);
    }

    @Override
    protected void createPositions(final Parameter _parameter,
                                   final CreatedDoc _createSchedule)
        throws EFapsException
    {
        final String[] oids = _parameter.getParameterValues("document");
        final String[] amounts = _parameter.getParameterValues("amount4Schedule");
        Integer i = 0;
        for (final String oid : oids) {
            final Instance instDoc = Instance.get(oid);
            final PrintQuery printDoc = new PrintQuery(instDoc);
            printDoc.addAttribute(CISales.DocumentSumAbstract.CrossTotal);
            printDoc.addAttribute(CISales.DocumentAbstract.Note);
            printDoc.execute();

            final Insert insertPayShePos = new Insert(CISales.PaymentSchedulePosition);
            insertPayShePos.add(CISales.PaymentSchedulePosition.PaymentSchedule, _createSchedule.getInstance());
            insertPayShePos.add(CISales.PaymentSchedulePosition.Document, instDoc);
            insertPayShePos.add(CISales.PaymentSchedulePosition.PositionNumber, i);
            if (_parameter.getParameterValues(CISales.PaymentSchedulePosition.DocumentDesc.name) != null) {
                insertPayShePos.add(CISales.PaymentSchedulePosition.DocumentDesc,
                                _parameter.getParameterValues(CISales.PaymentSchedulePosition.DocumentDesc.name)[i]);
            } else {
                insertPayShePos.add(CISales.PaymentSchedulePosition.DocumentDesc,
                                DBProperties.getProperty("org.efaps.esjp.sales.PaymentSchedule.defaultDescription"));
            }
            insertPayShePos.add(CISales.PaymentSchedulePosition.NetPrice, amounts[i]);
            insertPayShePos.execute();
            i++;
        }
    }

    public Return createSchedule4SelectedDocs(final Parameter _parameter)
        throws EFapsException
    {
        final String[] others = (String[]) Context.getThreadContext().getSessionAttribute("internalOIDs");
        if (others != null) {
            @SuppressWarnings("unchecked")
            final Map<String, String[]> parameters = (Map<String, String[]>) _parameter
                            .get(ParameterValues.PARAMETERS);
            parameters.put("document", others);
            _parameter.put(ParameterValues.PARAMETERS, parameters);
            createDoc(_parameter);
        }
        return new Return();
    }

    public Return deleteTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instPos = _parameter.getInstance();
        final SelectBuilder selectPaySche = new SelectBuilder()
                                .linkto(CISales.PaymentSchedulePosition.PaymentSchedule).oid();

        final PrintQuery printPos = new PrintQuery(instPos);
        printPos.addAttribute(CISales.PaymentSchedulePosition.NetPrice);
        printPos.addSelect(selectPaySche);

        BigDecimal posNetPrice = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        if (printPos.execute()) {
            posNetPrice = printPos.<BigDecimal>getAttribute(CISales.PaymentSchedulePosition.NetPrice);
            final Instance instPaySche = Instance.get(printPos.<String>getSelect(selectPaySche));
            final PrintQuery printPaySche = new PrintQuery(instPaySche);
            printPaySche.addAttribute(CISales.PaymentSchedule.Total);
            if (printPaySche.execute()) {
                total = printPaySche.<BigDecimal>getAttribute(CISales.PaymentSchedule.Total);
                final Update updatePaySche = new Update(printPaySche.getCurrentInstance());
                updatePaySche.add(CISales.PaymentSchedule.Total,
                                total.subtract(posNetPrice).setScale(2, RoundingMode.HALF_UP));
                updatePaySche.execute();
            }
        }
        return new Return();
    }

    public Return getNotScheduledDocuments(final Parameter _parameter)
        throws EFapsException
    {
        return new MultiPrint()
        {
            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.PaymentSchedule);
                attrQueryBldr.addWhereAttrEqValue(CISales.PaymentSchedule.Status,
                                Status.find(CISales.PaymentScheduleStatus.Canceled));
                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.PaymentSchedule.ID);

                final QueryBuilder attrQueryBldr2 = new QueryBuilder(CISales.PaymentSchedulePosition);
                attrQueryBldr2.addWhereAttrNotInQuery(CISales.PaymentSchedulePosition.PaymentSchedule, attrQuery);
                final AttributeQuery attrQuery2 = attrQueryBldr2
                                .getAttributeQuery(CISales.PaymentSchedulePosition.Document);

                _queryBldr.addWhereAttrNotInQuery(CISales.DocumentAbstract.ID, attrQuery2);
            }
        }.execute(_parameter);
    }

    public Return getScheduledDocuments(final Parameter _parameter)
        throws EFapsException
    {
        return new MultiPrint()
        {
            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.PaymentSchedule);
                attrQueryBldr.addWhereAttrEqValue(CISales.PaymentSchedule.Status,
                                Status.find(CISales.PaymentScheduleStatus.Open));
                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.PaymentSchedule.ID);

                final QueryBuilder attrQueryBldr2 = new QueryBuilder(CISales.PaymentSchedulePosition);
                attrQueryBldr2.addWhereAttrInQuery(CISales.PaymentSchedulePosition.PaymentSchedule, attrQuery);
                final AttributeQuery attrQuery2 = attrQueryBldr2
                                .getAttributeQuery(CISales.PaymentSchedulePosition.Document);

                _queryBldr.addWhereAttrInQuery(CISales.DocumentAbstract.ID, attrQuery2);
            }
        }.execute(_parameter);
    }

    @Override
    protected void add2QueryBldr4AutoCompleteScheduledDoc(final Parameter _parameter,
                                                          final QueryBuilder _queryBldr)
        throws EFapsException
    {
        final SelectBuilder selDocType = new SelectBuilder().linkto(CISales.PaymentSchedulePosition.Document).type();
        final SelectBuilder selDoc = new SelectBuilder().linkto(CISales.PaymentSchedulePosition.Document).instance();

        final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.PaymentSchedule);
        attrQueryBldr.addWhereAttrEqValue(CISales.PaymentSchedule.Status,
                        Status.find(CISales.PaymentScheduleStatus.Open));
        final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.PaymentSchedule.ID);

        final QueryBuilder queryBldr = new QueryBuilder(CISales.PaymentSchedulePosition);
        queryBldr.addWhereAttrInQuery(CISales.PaymentSchedulePosition.PaymentSchedule, attrQuery);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addSelect(selDocType, selDoc);
        multi.execute();

        final List<Long> ids = new ArrayList<Long>();
        while (multi.next()) {
            if (multi.<Type>getSelect(selDocType).getUUID().equals(_queryBldr.getTypeUUID())) {
                ids.add(multi.<Instance>getSelect(selDoc).getId());
            }
        }

        if (!ids.isEmpty()) {
            _queryBldr.addWhereAttrEqValue(CISales.DocumentAbstract.ID, ids.toArray());
        }
    }

    @Override
    public Return updateFields4Contact(final Parameter _parameter)
        throws EFapsException
    {
        final String value = _parameter.getParameterValue("contactAutoComplete");
        final Instance contact = Instance.get(_parameter.getParameterValue("contact"));
        if (value != null && value.length() > 0 && contact.isValid()) {
            Context.getThreadContext().setSessionAttribute(PaymentSchedule.CONTACT_SESSIONKEY, contact);
        } else {
            Context.getThreadContext().removeSessionAttribute(PaymentSchedule.CONTACT_SESSIONKEY);
            _parameter.getParameters().put("contact", new String[]{ "" });
        }
        return super.updateFields4Contact(_parameter);
    }

    public Return removeSession4Schedule(final Parameter _parameter)
        throws EFapsException
    {
        Context.getThreadContext().removeSessionAttribute(PaymentSchedule.CONTACT_SESSIONKEY);
        return new Return();
    }

    @Override
    protected BigDecimal getPaymentDocumentOut4Doc(final Instance _docInstance)
        throws EFapsException
    {
        final Instance baseCurLink = Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);
        final SelectBuilder selRateDoc = new SelectBuilder().linkto(CISales.Payment.CreateDocument)
                        .attribute(CISales.DocumentSumAbstract.Rate);

        BigDecimal ret = BigDecimal.ZERO;

        final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.PaymentDocumentOutAbstract);
        attrQueryBldr.addWhereAttrNotEqValue(CISales.PaymentDocumentOutAbstract.StatusAbstract,
                        Status.find(CISales.PaymentCashOutStatus.Canceled).getId(),
                        Status.find(CISales.PaymentCheckOutStatus.Canceled).getId(),
                        Status.find(CISales.PaymentDepositOutStatus.Canceled).getId(),
                        Status.find(CISales.PaymentDetractionOutStatus.Canceled).getId(),
                        Status.find(CISales.PaymentExchangeOutStatus.Canceled).getId(),
                        Status.find(CISales.PaymentRetentionOutStatus.Canceled).getId(),
                        Status.find(CISales.PaymentSupplierOutStatus.Canceled).getId());
        final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.PaymentDocumentOutAbstract.ID);

        final QueryBuilder queryBldr = new QueryBuilder(CISales.Payment);
        queryBldr.addWhereAttrInQuery(CISales.Payment.TargetDocument, attrQuery);
        queryBldr.addWhereAttrEqValue(CISales.Payment.CreateDocument, _docInstance);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.Payment.Amount,
                        CISales.Payment.CurrencyLink,
                        CISales.Payment.RateCurrencyLink,
                        CISales.Payment.Rate);
        multi.addSelect(selRateDoc);
        multi.execute();
        while (multi.next()) {
            final BigDecimal amount = multi.<BigDecimal>getAttribute(CISales.Payment.Amount);
            final Object[] rateObj = multi.<Object[]>getAttribute(CISales.Payment.Rate);
            final Object[] rateObjDoc = multi.<Object[]>getSelect(selRateDoc);
            if (baseCurLink.getId() == multi.<Long>getAttribute(CISales.Payment.CurrencyLink)
                            && baseCurLink.getId() == multi.<Long>getAttribute(CISales.Payment.RateCurrencyLink)) {
                ret = ret.add(amount);
            } else if (baseCurLink.getId() == multi.<Long>getAttribute(CISales.Payment.RateCurrencyLink)
                            && baseCurLink.getId() != multi.<Long>getAttribute(CISales.Payment.CurrencyLink)) {
                final BigDecimal rate = ((BigDecimal) rateObj[0]);
                ret = ret.add(amount.multiply(rate).setScale(2, BigDecimal.ROUND_HALF_UP));
            } else if (baseCurLink.getId() == multi.<Long>getAttribute(CISales.Payment.CurrencyLink)
                            && baseCurLink.getId() != multi.<Long>getAttribute(CISales.Payment.RateCurrencyLink)) {
                final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                                BigDecimal.ROUND_HALF_UP);
                ret = ret.add(amount.multiply(rate).setScale(2, BigDecimal.ROUND_HALF_UP));
            } else {
                final BigDecimal rate = ((BigDecimal) rateObjDoc[0]).divide((BigDecimal) rateObjDoc[1], 12,
                                BigDecimal.ROUND_HALF_UP);
                ret = ret.add(amount.multiply(rate).setScale(2, BigDecimal.ROUND_HALF_UP));
            }
        }

        return ret;
    }

    @Override
    protected BigDecimal getEventSchedule4Doc(final Instance _instance)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;

        final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.PaymentSchedule);
        attrQueryBldr.addWhereAttrEqValue(CISales.PaymentSchedule.Status,
                        Status.find(CISales.PaymentScheduleStatus.Open));
        final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.PaymentSchedule.ID);

        final QueryBuilder queryBldr = new QueryBuilder(CISales.PaymentSchedulePosition);
        queryBldr.addWhereAttrInQuery(CISales.PaymentSchedulePosition.PaymentSchedule, attrQuery);
        queryBldr.addWhereAttrEqValue(CISales.PaymentSchedulePosition.Document, _instance);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.PaymentSchedulePosition.NetPrice);
        multi.execute();

        while (multi.next()) {
            ret = ret.add(multi.<BigDecimal>getAttribute(CISales.PaymentSchedulePosition.NetPrice));
        }

        return ret;
    }
}
