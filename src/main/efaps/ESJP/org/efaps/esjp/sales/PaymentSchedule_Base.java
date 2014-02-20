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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
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
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.SalesSettings;
import org.efaps.ui.wicket.util.EFapsKey;
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

        if (oids != null && oids.length > 0) {
            for (int i = 0; i < oids.length; i++) {
                final Instance instDoc = Instance.get(oids[i]);
                if (instDoc.isValid()) {
                    final Insert posInsert = new Insert(getType4PositionCreate(_parameter));
                    posInsert.add(CISales.PaymentSchedulePosition.PaymentSchedule, _createSchedule.getInstance());
                    posInsert.add(CISales.PaymentSchedulePosition.Document, instDoc);
                    posInsert.add(CISales.PaymentSchedulePosition.PositionNumber, i);
                    if (_parameter.getParameterValues(CISales.PaymentSchedulePosition.DocumentDesc.name) != null) {
                        posInsert.add(CISales.PaymentSchedulePosition.DocumentDesc, _parameter
                                .getParameterValues(CISales.PaymentSchedulePosition.DocumentDesc.name)[i]);
                    } else {
                        posInsert.add(CISales.PaymentSchedulePosition.DocumentDesc,
                                DBProperties.getProperty("org.efaps.esjp.sales.PaymentSchedule.defaultDescription"));
                    }
                    posInsert.add(CISales.PaymentSchedulePosition.NetPrice, amounts[i]);
                    posInsert.execute();
                }
            }
        }
    }

    /**
     * Edit.
     *
     * @param _parameter Parameter from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final EditedDoc editDoc = editDoc(_parameter);
        updatePositions(_parameter, editDoc);
        return new Return();
    }

    @Override
    protected void updatePositions(final Parameter _parameter,
                                   final EditedDoc _editDoc)
        throws EFapsException
    {
        @SuppressWarnings("unchecked")
        final Map<String, String> oidMap = (Map<String, String>) _parameter.get(ParameterValues.OIDMAP4UI);
        final String[] rowKeys = _parameter.getParameterValues(EFapsKey.TABLEROW_NAME.getKey());

        for (int i = 0; i < rowKeys.length; i++) {
            final Instance inst = Instance.get(oidMap.get(rowKeys[i]));
            final Update update;
            if (inst.isValid()) {
                update = new Update(inst);
            } else {
                update = new Insert(getType4PositionUpdate(_parameter));
            }
            update.add(CISales.PaymentSchedulePosition.PositionNumber, i + 1);
            update.add(CISales.PaymentSchedulePosition.PaymentSchedule, _editDoc.getInstance());

            final String[] document = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                            CISales.PaymentSchedulePosition.Document.name));
            if (document != null && document.length > i) {
                final Instance docInst = Instance.get(document[i]);
                if (docInst.isValid()) {
                    update.add(CISales.PaymentSchedulePosition.Document, docInst);
                }
            }
            final String[] documentDesc = _parameter.getParameterValues(getFieldName4Attribute(_parameter,
                            CISales.PaymentSchedulePosition.DocumentDesc.name));
            if (documentDesc != null && documentDesc.length > i) {
                update.add(CISales.PaymentSchedulePosition.DocumentDesc, documentDesc[i]);
            } else {
                update.add(CISales.PaymentSchedulePosition.DocumentDesc,
                                DBProperties.getProperty("org.efaps.esjp.sales.PaymentSchedule.defaultDescription"));
            }
            update.add(CISales.PaymentSchedulePosition.NetPrice, _parameter.getParameterValues("amount4Schedule")[i]);
            update.execute();

            _editDoc.addPosition(update.getInstance());
        }
        deletePosition4Update(_parameter, _editDoc);
    }

    @Override
    protected void deletePosition4Update(final Parameter _parameter,
                                         final EditedDoc _editDoc)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(getType4PositionUpdate(_parameter));
        queryBldr.addWhereAttrEqValue(CISales.PaymentSchedulePosition.PaymentSchedule, _editDoc.getInstance());
        final InstanceQuery query = queryBldr.getQuery();
        query.execute();
        final Set<Instance> delIns = new HashSet<Instance>();
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

    @Override
    protected void add2DocEdit(final Parameter _parameter,
                               final Update _update,
                               final EditedDoc _editDoc)
        throws EFapsException
    {
        final String contact = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.PaymentSchedule.Contact.name));
        if (contact != null) {
            final Instance inst = Instance.get(contact);
            if (inst.isValid()) {
                _update.add(CISales.PaymentSchedule.Contact, inst.getId());
                _editDoc.getValues().put(CISales.PaymentSchedule.Contact.name, inst);
            }
        }
        _update.add(CISales.PaymentSchedule.Total, getTotalFmtStr(getTotal(_parameter, null)));
    }

    @SuppressWarnings("unchecked")
    public Return createSchedule4SelectedDocs(final Parameter _parameter)
        throws EFapsException
    {
        final String[] others = (String[]) Context.getThreadContext().getSessionAttribute("internalOIDs");
        final String[] other = (String[]) Context.getThreadContext().getSessionAttribute("internalAmounts");
        if (others != null) {
            final Map<String, String[]> parameters =
                            (Map<String, String[]>) _parameter.get(ParameterValues.PARAMETERS);
            parameters.put("document", others);
            parameters.put("amount4Schedule", other);
            _parameter.put(ParameterValues.PARAMETERS, parameters);
            final CreatedDoc createdDoc = createDoc(_parameter);
            createPositions(_parameter, createdDoc);
        }
        Context.getThreadContext().removeSessionAttribute("internalAmounts");
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
            BigDecimal rate = BigDecimal.ONE;
            if (multi.<Long>getAttribute(CISales.Payment.CurrencyLink)
                            .equals(multi.<Long>getAttribute(CISales.Payment.RateCurrencyLink))) {
                rate = ((BigDecimal) rateObjDoc[0]).divide((BigDecimal) rateObjDoc[1], 12, BigDecimal.ROUND_HALF_UP);
            } else {
                if (!multi.<Long>getAttribute(CISales.Payment.CurrencyLink).equals(baseCurLink.getId())) {
                    rate = ((BigDecimal) rateObj[1]).divide((BigDecimal) rateObj[0], 12, BigDecimal.ROUND_HALF_UP);
                }
            }
            ret = ret.add(amount.divide(rate, BigDecimal.ROUND_HALF_UP).setScale(2, BigDecimal.ROUND_HALF_UP));
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

    public Return getNetTotal(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        Context.getThreadContext().removeSessionAttribute("internalAmounts");
        final String[] selectedDoc = _parameter.getParameterValues("selectedDoc") == null
                        ? _parameter.getParameterValues("selectedRow") : _parameter.getParameterValues("selectedDoc");
        BigDecimal amount = BigDecimal.ZERO;

        if (selectedDoc != null) {
            final String[] other = new String[selectedDoc.length];
            if (selectedDoc.length > 1) {
                int i = 0;
                for (final String element : selectedDoc) {
                    final Instance instance = Instance.get(element);
                    if (instance.isValid()) {
                        final PrintQuery print = new PrintQuery(instance);
                        print.addAttribute(CISales.DocumentSumAbstract.CrossTotal);
                        print.execute();
                        final BigDecimal amountAux = print
                                        .<BigDecimal>getAttribute(CISales.DocumentSumAbstract.CrossTotal);
                        amount = amount.add(amountAux);
                        other[i] = getTotalFmtStr(amountAux);
                        i++;
                    }
                }
                ret.put(ReturnValues.VALUES, NumberFormatter.get().getTwoDigitsFormatter().format(amount).toString());
            } else {
                final Instance instance = Instance.get(selectedDoc[0]);
                if (instance.isValid()) {
                    final PrintQuery print = new PrintQuery(instance);
                    print.addAttribute(CISales.DocumentSumAbstract.CrossTotal);
                    print.execute();
                    amount = print.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.CrossTotal);
                    other[0] = getTotalFmtStr(amount);
                    ret.put(ReturnValues.VALUES, NumberFormatter.get().getTwoDigitsFormatter().format(amount)
                                    .toString());
                }
            }
            Context.getThreadContext().setSessionAttribute("internalAmounts", other);
        }
        return ret;
    }
}
