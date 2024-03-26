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
package org.efaps.esjp.sales;

import java.math.BigDecimal;
import java.util.List;
import java.util.Properties;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
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
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.common.properties.PropertiesUtil;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.Naming;
import org.efaps.esjp.erp.RateInfo;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;

@EFapsUUID("bc0668dd-36b1-4762-949a-be0b732008d1")
@EFapsApplication("eFapsApp-Sales")
public class CreditLine_Base
    extends AbstractCommon
{
    /**
     * Creates a new CreditLine.
     *
     * @param _parameter the parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        return new Create()
        {
            @Override
            protected void add2basicInsert(final Parameter _parameter, final Insert _insert)
                throws EFapsException
            {
                super.add2basicInsert(_parameter, _insert);
                _insert.add(CISales.CreditLine.Name, new Naming().fromNumberGenerator(_parameter, null));
            }
        }.execute(_parameter);
    }

    /**
     * Update credit line.
     *
     * @param _parameter the parameter
     * @param _docInstance the doc instance
     * @throws EFapsException the eFaps exception
     */
    public void updateCreditLine(final Parameter _parameter, final Instance _docInstance)
        throws EFapsException
    {
        if (Sales.CREDITLINE_ACTIVATE.get() && InstanceUtils.isValid(_docInstance)
                        && _docInstance.getType().getStatusAttribute() != null) {
            final PrintQuery print = new PrintQuery(_docInstance);
            print.addAttribute(_docInstance.getType().getStatusAttribute());
            print.executeWithoutAccessCheck();
            updateCreditLine(_parameter, _docInstance,
                            Status.get(print.<Long>getAttribute(_docInstance.getType().getStatusAttribute())));
        }
    }

    /**
     * Update credit line.
     *
     * @param _parameter the parameter
     * @param _docInstance the doc instance
     * @param _status the status
     * @throws EFapsException the eFaps exception
     */
    public void updateCreditLine(final Parameter _parameter, final Instance _docInstance, final Status _status)
        throws EFapsException
    {
        if (getAddStatusList().contains(_status) && !isAssigned(_docInstance)) {
            final PrintQuery print = new PrintQuery(_docInstance);
            print.addAttribute(CIERP.DocumentAbstract.Contact);
            print.executeWithoutAccessCheck();
            final QueryBuilder queryBldr = new QueryBuilder(CISales.CreditLine);
            queryBldr.addWhereAttrEqValue(CISales.CreditLine.Status, Status.find(CISales.CreditLineStatus.Approved));
            queryBldr.addWhereAttrEqValue(CISales.CreditLine.ContactLink,
                            print.<Long>getAttribute(CIERP.DocumentAbstract.Contact));
            final InstanceQuery query = queryBldr.getQuery();
            query.executeWithoutAccessCheck();
            if (query.next()) {
                final Insert insert = new Insert(CISales.CreditLine2DocumentSum);
                insert.add(CISales.CreditLine2DocumentSum.FromLink, query.getCurrentValue());
                insert.add(CISales.CreditLine2DocumentSum.ToLink, _docInstance);
                insert.executeWithoutAccessCheck();
            }
        } else if (getRemoveStatusList().contains(_status)) {
            final Instance relInstance = getCreditLine2DocInstance(_docInstance);
            if (relInstance.isValid()) {
                new Delete(relInstance).execute();
            }
        }
    }

    /**
     * Gets the credit line two doc instance.
     *
     * @param _docInstance the doc instance
     * @return the credit line two doc instance
     * @throws EFapsException the e faps exception
     */
    protected Instance getCreditLine2DocInstance(final Instance _docInstance)
        throws EFapsException
    {
        Instance ret = Instance.get("");
        final QueryBuilder queryBldr = new QueryBuilder(CISales.CreditLine2DocumentSum);
        queryBldr.addWhereAttrEqValue(CISales.CreditLine2DocumentSum.ToLink, _docInstance);
        final InstanceQuery query = queryBldr.getQuery();
        query.execute();
        if (query.next()) {
            ret = query.getCurrentValue();
        }
        return ret;
    }

    /**
     * Checks if is assigned.
     *
     * @param _docInstance the doc instance
     * @return true, if is assigned
     * @throws EFapsException the e faps exception
     */
    protected boolean isAssigned(final Instance _docInstance)
        throws EFapsException
    {
        return getCreditLine2DocInstance(_docInstance).isValid();
    }

     /**
     * Gets the adds the status list.
     *
     * @return the adds the status list
     * @throws EFapsException the e faps exception
     */
    public List<Status> getAddStatusList()
        throws EFapsException
    {
        final Properties properties = Sales.CREDITLINE_LISTENER.get();
        return getStatusListFromProperties(ParameterUtil.instance(),
                        PropertiesUtil.getProperties4Prefix(properties, "ADD"));
    }

    /**
     * Gets the removes the status list.
     *
     * @return the removes the status list
     * @throws EFapsException the e faps exception
     */
    public List<Status> getRemoveStatusList()
        throws EFapsException
    {
        final Properties properties = Sales.CREDITLINE_LISTENER.get();
        return getStatusListFromProperties(ParameterUtil.instance(),
                        PropertiesUtil.getProperties4Prefix(properties, "REMOVE"));
    }

    /**
     * Calculate credit line.
     *
     * @param _parameter the parameter
     * @param _lineInst the line instance
     * @throws EFapsException the e faps exception
     */
    public void calculateCreditLine(final Parameter _parameter, final Instance _lineInst)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_lineInst);
        final SelectBuilder selCurInst = SelectBuilder.get().linkto(CISales.CreditLine.CurrencyLink).instance();
        print.addSelect(selCurInst);
        print.executeWithoutAccessCheck();

        final Instance lineCurInst = print.getSelect(selCurInst);

        BigDecimal applied = BigDecimal.ZERO;
        final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.CreditLine2DocumentSum);
        attrQueryBldr.addWhereAttrEqValue(CISales.CreditLine2DocumentSum.FromLink, _lineInst);

        final QueryBuilder queryBldr = new QueryBuilder(CISales.DocumentSumAbstract);
        queryBldr.addWhereAttrInQuery(CISales.DocumentSumAbstract.ID,
                        attrQueryBldr.getAttributeQuery(CISales.CreditLine2DocumentSum.ToLink));
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selRateCurInst = SelectBuilder.get().linkto(CISales.DocumentSumAbstract.RateCurrencyId)
                        .instance();
        multi.addSelect(selRateCurInst);
        multi.addAttribute(CISales.DocumentSumAbstract.RateCrossTotal, CISales.DocumentSumAbstract.CrossTotal);
        multi.executeWithoutAccessCheck();
        while (multi.next()) {
            final BigDecimal crossTotal = multi.getAttribute(CISales.DocumentSumAbstract.CrossTotal);
            final BigDecimal rateCrossTotal = multi.getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);

            final Instance rateCurInst = multi.getSelect(selRateCurInst);
            // the same currency
            if (rateCurInst.equals(lineCurInst)) {
                applied = applied.add(rateCrossTotal);
                // base currency
            } else if (lineCurInst.equals(Currency.getBaseCurrency())) {
                applied = applied.add(crossTotal);
            } else {
                final RateInfo rateInfo = new Currency().evaluateRateInfo(_parameter, "", lineCurInst);
                final BigDecimal amount = crossTotal.multiply(rateInfo.getRate());
                applied = applied.add(amount);
            }
        }
        final Update update = new Update(_lineInst);
        update.add(CISales.CreditLine.Applied, applied);
        update.executeWithoutTrigger();
    }

    /**
     * Post insert trigger.
     *
     * @param _parameter the parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return postInsertTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final Instance lineInst = getCreditLineInstance(_parameter);
        calculateCreditLine(_parameter, lineInst);
        return new Return();
    }

    /**
     * Pre delete trigger.
     *
     * @param _parameter the parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return preDeleteTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final Instance lineInst = getCreditLineInstance(_parameter);
        Context.getThreadContext().setRequestAttribute(this.getClass().getName() + ".CreditLineInstance", lineInst);
        return new Return();
    }

    /**
     * Post delete trigger.
     *
     * @param _parameter the parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return postDeleteTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final Instance lineInst = (Instance) Context.getThreadContext().getRequestAttribute(this.getClass().getName()
                        + ".CreditLineInstance");
        calculateCreditLine(_parameter, lineInst);
        return new Return();
    }

    /**
     * Gets the credit line instance.
     *
     * @param _parameter the parameter
     * @return the credit line instance
     * @throws EFapsException the e faps exception
     */
    protected Instance getCreditLineInstance(final Parameter _parameter)
        throws EFapsException
    {
        final Instance relInstane = _parameter.getInstance();
        final PrintQuery print = new PrintQuery(relInstane);
        final SelectBuilder selLineInst = SelectBuilder.get().linkto(CISales.CreditLine2DocumentSum.FromLink)
                        .instance();
        print.addSelect(selLineInst);
        print.executeWithoutAccessCheck();
        return print.getSelect(selLineInst);
    }

    /**
     * Evaluate.
     *
     * @param _parameter the parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return evaluate(final Parameter _parameter)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CISales.CreditLine2DocumentSum);
        queryBldr.addWhereAttrEqValue(CISales.CreditLine2DocumentSum.FromLink, _parameter.getInstance());
        final InstanceQuery query = queryBldr.getQuery();
        query.execute();
        while (query.next()) {
            new Delete(query.getCurrentValue()).executeWithoutTrigger();
        }
        final PrintQuery print = new PrintQuery(_parameter.getInstance());
        print.addAttribute(CISales.CreditLine.ContactLink);
        print.execute();
        final Long contactId = print.getAttribute(CISales.CreditLine.ContactLink);

        final QueryBuilder docQueryBldr = new QueryBuilder(CISales.DocumentSumAbstract);
        docQueryBldr.addWhereAttrEqValue(CISales.DocumentSumAbstract.Contact, contactId);
        docQueryBldr.addWhereAttrEqValue(CISales.DocumentSumAbstract.StatusAbstract, getAddStatusList().toArray());
        final InstanceQuery docQuery = docQueryBldr.getQuery();
        docQuery.execute();
        while (docQuery.next()) {
            final Insert insert = new Insert(CISales.CreditLine2DocumentSum);
            insert.add(CISales.CreditLine2DocumentSum.FromLink, _parameter.getInstance());
            insert.add(CISales.CreditLine2DocumentSum.ToLink, docQuery.getCurrentValue());
            insert.executeWithoutTrigger();
        }
        calculateCreditLine(_parameter, _parameter.getInstance());
        return new Return();
    }
}
