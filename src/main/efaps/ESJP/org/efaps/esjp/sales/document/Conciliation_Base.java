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


package org.efaps.esjp.sales.document;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.efaps.admin.datamodel.Attribute;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.util.EFapsException;



/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
public abstract class Conciliation_Base
    extends AbstractSumOnlyDocument
{
    /**
     * Key used to pass value during the request.
     */
    public static final String REQUESTKEY = Conciliation.class.getName() + "RequestKey";

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = createDoc(_parameter);
        final Return ret = new Return();
        ret.put(ReturnValues.INSTANCE, createdDoc.getInstance());
        return ret;
    }


    @Override
    protected void add2DocCreate(final Parameter _parameter,
                                 final Insert _insert,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        super.add2DocCreate(_parameter, _insert, _createdDoc);

        _insert.add(CISales.DocumentSumAbstract.RateNetTotal, BigDecimal.ZERO);
        _createdDoc.getValues().put(CISales.DocumentSumAbstract.RateNetTotal.name, BigDecimal.ZERO);
        _insert.add(CISales.DocumentSumAbstract.NetTotal, BigDecimal.ZERO);
        _createdDoc.getValues().put(CISales.DocumentSumAbstract.NetTotal.name, BigDecimal.ZERO);
        _insert.add(CISales.DocumentSumAbstract.RateCrossTotal, BigDecimal.ZERO);
        _createdDoc.getValues().put(CISales.DocumentSumAbstract.RateCrossTotal.name, BigDecimal.ZERO);
        _insert.add(CISales.DocumentSumAbstract.CrossTotal, BigDecimal.ZERO);
        _createdDoc.getValues().put(CISales.DocumentSumAbstract.CrossTotal.name, BigDecimal.ZERO);


        final String accountStr = _parameter.getParameterValue(CIFormSales.Sales_ConciliationForm.account.name);
        _insert.add(CISales.Conciliation.AccountLink, accountStr);
        _createdDoc.getValues().put(CISales.DocumentSumAbstract.CrossTotal.name, accountStr);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new return
     * @throws EFapsException on error
     */
    public Return paymentMultiPrint(final Parameter _parameter)
        throws EFapsException
    {

        return new MultiPrint()
        {
            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final QueryBuilder accAttrQueryBldr = new QueryBuilder(CISales.Conciliation);
                accAttrQueryBldr.addWhereAttrEqValue(CISales.Conciliation.ID, _parameter.getInstance());
                final AttributeQuery accAttrQuery = accAttrQueryBldr.getAttributeQuery(
                                CISales.Conciliation.AccountLink);

                final QueryBuilder transAttrQueryBldr = new QueryBuilder(CISales.TransactionAbstract);
                transAttrQueryBldr.addWhereAttrInQuery(CISales.TransactionAbstract.Account, accAttrQuery);
                final AttributeQuery transAttrQuery = transAttrQueryBldr
                                .getAttributeQuery(CISales.TransactionAbstract.Payment);

                final QueryBuilder payAttrQueryBldr = new QueryBuilder(CISales.Payment);
                payAttrQueryBldr.addWhereAttrInQuery(CISales.Payment.ID, transAttrQuery);
                final AttributeQuery payAttrQuery = payAttrQueryBldr.getAttributeQuery(CISales.Payment.TargetDocument);

                _queryBldr.addWhereAttrInQuery(CISales.PaymentDocumentIOAbstract.ID, payAttrQuery);
                _queryBldr.addWhereAttrIsNull(CISales.PaymentDocumentIOAbstract.ConciliationPositionLink);
            };
        } .execute(_parameter);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new return
     * @throws EFapsException on error
     */
    public Return positionMultiPrint(final Parameter _parameter)
        throws EFapsException
    {

        return new MultiPrint()
        {

            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final QueryBuilder posAttrQueryBldr = new QueryBuilder(CISales.ConciliationPosition);
                posAttrQueryBldr.addWhereAttrEqValue(CISales.ConciliationPosition.ConciliationLink,
                                _parameter.getInstance());
                final AttributeQuery posAttrQuery = posAttrQueryBldr.getAttributeQuery(
                                CISales.ConciliationPosition.ID);

                _queryBldr.addWhereAttrInQuery(CISales.PaymentDocumentIOAbstract.ConciliationPositionLink,
                                posAttrQuery);
            };
        } .execute(_parameter);
    }


    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new return
     * @throws EFapsException on error
     */
    public Return create4Position(final Parameter _parameter)
        throws EFapsException
    {
        final Map<Instance, BigDecimal> inst2amount = new HashMap<Instance, BigDecimal>();
        final String[] oids = _parameter.getParameterValues("selectedRow");
        final boolean single = "true".equalsIgnoreCase(getProperty(_parameter, "Single"));
        if (oids != null && oids.length > 0) {
            Instance posInst = null;
            for (final String oid : oids) {
                if (single || posInst == null) {
                    final Insert insert = new Insert(CISales.ConciliationPosition);
                    insert.add(CISales.ConciliationPosition.ConciliationLink, _parameter.getCallInstance());
                    insert.add(CISales.ConciliationPosition.PositionNumber, 1);
                    insert.execute();
                    posInst = insert.getInstance();
                    inst2amount.put(posInst, BigDecimal.ZERO);
                }
                final Instance paymentInst = Instance.get(oid);
                final PrintQuery print = new PrintQuery(paymentInst);
                print.addAttribute(CISales.PaymentDocumentIOAbstract.Amount);
                print.executeWithoutAccessCheck();
                BigDecimal amount = print.<BigDecimal>getAttribute(CISales.PaymentDocumentIOAbstract.Amount);
                if (paymentInst.getType().isKindOf(CISales.PaymentDocumentOutAbstract.getType())) {
                    amount = amount.negate();
                }
                inst2amount.put(posInst, inst2amount.get(posInst).add(amount));

                final Update update = new Update(paymentInst);
                update.add(CISales.PaymentDocumentIOAbstract.ConciliationPositionLink, posInst);
                update.execute();
            }
        }
        for (final Entry<Instance, BigDecimal> entry : inst2amount.entrySet()) {
            final Update update = new Update(entry.getKey());
            update.add(CISales.ConciliationPosition.Amount, entry.getValue());
            update.execute();
        }
        recalculate(_parameter, _parameter.getCallInstance());
        return new Return();
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new return
     * @throws EFapsException on error
     */
    public Return positionDeletePreTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CISales.PaymentDocumentIOAbstract);
        queryBldr.addWhereAttrEqValue(CISales.PaymentDocumentIOAbstract.ConciliationPositionLink,
                        _parameter.getInstance());
        final InstanceQuery query = queryBldr.getQuery();
        query.executeWithoutAccessCheck();
        while (query.next()) {
            final Update update = new Update(query.getCurrentValue());
            update.add(CISales.PaymentDocumentIOAbstract.ConciliationPositionLink, (Object) null);
            update.execute();
        }

        final PrintQuery print = new PrintQuery(_parameter.getInstance());
        final SelectBuilder selInst = SelectBuilder.get().linkto(CISales.ConciliationPosition.ConciliationLink)
                        .instance();
        print.addSelect(selInst);
        print.addAttribute(CISales.ConciliationPosition.ConciliationLink);
        print.executeWithoutAccessCheck();

        Context.getThreadContext().setRequestAttribute(Conciliation_Base.REQUESTKEY, print.getSelect(selInst));

        final QueryBuilder queryBldr2 = new QueryBuilder(CISales.ConciliationPosition);
        queryBldr2.addWhereAttrEqValue(CISales.ConciliationPosition.ConciliationLink,
                        print.getAttribute(CISales.ConciliationPosition.ConciliationLink));
        queryBldr2.addWhereAttrNotEqValue(CISales.ConciliationPosition.ID, _parameter.getInstance());
        queryBldr2.addOrderByAttributeAsc(CISales.ConciliationPosition.PositionNumber);
        final InstanceQuery query2 = queryBldr2.getQuery();
        query2.execute();
        int i = 1;
        while (query2.next()) {
            final Update update = new Update(query2.getCurrentValue());
            update.add(CISales.ConciliationPosition.PositionNumber, i);
            update.executeWithoutTrigger();
            i++;
        }
        return new Return();
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new return
     * @throws EFapsException on error
     */
    public Return positionDeletePostTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final Instance inst = (Instance) Context.getThreadContext().getRequestAttribute(Conciliation_Base.REQUESTKEY);
        recalculate(_parameter, inst);
        return new Return();
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _concInst insatnce to be recaluclated
     * @throws EFapsException on error
     */
    protected void recalculate(final Parameter _parameter,
                               final Instance _concInst)
        throws EFapsException
    {
        BigDecimal amount = BigDecimal.ZERO;
        final QueryBuilder queryBldr = new QueryBuilder(CISales.ConciliationPosition);
        queryBldr.addWhereAttrEqValue(CISales.ConciliationPosition.ConciliationLink, _concInst);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.ConciliationPosition.Amount);
        multi.executeWithoutAccessCheck();
        while (multi.next()) {
            amount = amount.add(multi.<BigDecimal>getAttribute(CISales.ConciliationPosition.Amount));
        }

        final Update update = new Update(_concInst);
        update.add(CISales.Conciliation.RateNetTotal, amount);
        update.execute();
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new return
     * @throws EFapsException on error
     */
    public Return positionInsertPostTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_parameter.getInstance());
        print.addAttribute(CISales.ConciliationPosition.ConciliationLink);
        print.executeWithoutAccessCheck();

        final QueryBuilder queryBldr = new QueryBuilder(CISales.ConciliationPosition);
        queryBldr.addWhereAttrEqValue(CISales.ConciliationPosition.ConciliationLink,
                        print.getAttribute(CISales.ConciliationPosition.ConciliationLink));
        queryBldr.addWhereAttrNotEqValue(CISales.ConciliationPosition.ID, _parameter.getInstance());
        queryBldr.addOrderByAttributeDesc(CISales.ConciliationPosition.PositionNumber);
        queryBldr.setLimit(1);

        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.ConciliationPosition.PositionNumber);
        multi.executeWithoutAccessCheck();
        while (multi.next()) {
            final Integer pos = multi.getAttribute(CISales.ConciliationPosition.PositionNumber);
            final Update update = new Update(_parameter.getInstance());
            update.add(CISales.ConciliationPosition.PositionNumber, pos + 1);
            update.executeWithoutTrigger();
        }
        return new Return();
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new empty Return
     * @throws EFapsException on error
     */
    public Return updatePostTrigger(final Parameter _parameter)
        throws EFapsException
    {
        @SuppressWarnings("unchecked")
        final Map<Attribute, Object> values = (Map<Attribute, Object>) _parameter.get(ParameterValues.NEW_VALUES);
        for (final Entry<Attribute, Object> entry : values.entrySet()) {
            if (CISales.Conciliation.Status.name.equals(entry.getKey().getName())
                            || CISales.Conciliation.StatusAbstract.name.equals(entry.getKey().getName())) {
                final Object objID = ((Object[]) entry.getValue())[0];
                final Long statusid = objID instanceof String ? Long.valueOf((String) objID) : (Long) objID;
                final Status status = Status.get(statusid);
                if (CISales.ConciliationStatus.Closed.key.equals(status.getKey())) {
                    final QueryBuilder posAttrQueryBldr = new QueryBuilder(CISales.ConciliationPosition);
                    posAttrQueryBldr.addWhereAttrEqValue(CISales.ConciliationPosition.ConciliationLink,
                                    _parameter.getInstance());
                    final AttributeQuery posAttrQuery = posAttrQueryBldr
                                    .getAttributeQuery(CISales.ConciliationPosition.ID);

                    final QueryBuilder queryBldr = new QueryBuilder(CISales.PaymentDocumentIOAbstract);
                    queryBldr.addWhereAttrInQuery(CISales.PaymentDocumentIOAbstract.ConciliationPositionLink,
                                    posAttrQuery);
                    final InstanceQuery query = queryBldr.getQuery();
                    query.executeWithoutAccessCheck();
                    while (query.next()) {
                        final Instance payInst = query.getCurrentValue();
                        final Status payStatus = Status.find(payInst.getType().getStatusAttribute().getLink().getUUID(),
                                        "Closed");
                        if (payStatus != null) {
                            final Update update = new Update(payInst);
                            update.add(CISales.PaymentDocumentIOAbstract.StatusAbstract, payStatus);
                            update.execute();
                        }
                    }
                }
            }
        }
        return new Return();
    }
}
