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

package org.efaps.esjp.sales;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.admin.ui.field.Field.Display;
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
import org.efaps.esjp.sales.payment.DocumentUpdate;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("417d2eff-b3ab-4f1a-91f4-c75da34570f6")
@EFapsRevision("$Rev$")
public abstract class Transaction_Base
{
    /**
     * Method is called from within the form Sales_TransactionAbstractForm to
     * retrieve the value for the Account on Edit or Create.
     *
     * @param _parameter Parameters as passed from eFaps
     * @return Return
     * @throws EFapsException on error
     */
    public Return value4Account(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final FieldValue fieldValue = (FieldValue) _parameter.get(ParameterValues.UIOBJECT);
        final Map<Long, String> cashDeskMap = new LinkedHashMap<Long, String>();
        final List<String> cashDeskDesc = new ArrayList<String>();
        final List<String> cashDeskCurr = new ArrayList<String>();
        final long actual = 0;
        final StringBuilder ret = new StringBuilder();

        final QueryBuilder cashDeskQuery = new QueryBuilder(CISales.AccountCashDesk);
        cashDeskQuery.addOrderByAttributeAsc(CISales.AccountCashDesk.Name);
        final MultiPrintQuery cashDeskMulti = cashDeskQuery.getPrint();
        cashDeskMulti.addAttribute(CISales.AccountCashDesk.ID,
                        CISales.AccountCashDesk.Name,
                        CISales.AccountCashDesk.Description);
        final SelectBuilder selCur = new SelectBuilder().linkto(CISales.AccountCashDesk.CurrencyLink).attribute(
                        CIERP.Currency.Name);
        cashDeskMulti.addSelect(selCur);
        cashDeskMulti.setEnforceSorted(true);
        cashDeskMulti.execute();

        while (cashDeskMulti.next()) {
            cashDeskMap.put(cashDeskMulti.<Long>getAttribute(CISales.AccountCashDesk.ID),
                            cashDeskMulti.<String>getAttribute(CISales.AccountCashDesk.Name));
            cashDeskDesc.add(cashDeskMulti.<String>getAttribute(CISales.AccountCashDesk.Description));
            cashDeskCurr.add(cashDeskMulti.<String>getSelect(selCur));
        }
        int i = 0;

        ret.append("<select size=\"1\" name=\"").append(fieldValue.getField().getName()).append("\">");
        for (final Map.Entry<Long, String> entry : cashDeskMap.entrySet()) {

            ret.append("<option");

            if (entry.getValue().equals(actual)) {
                ret.append(" selected=\"selected\" ");
            }
            ret.append(" value=\"").append(entry.getKey()).append("\">")
                .append(entry.getValue()).append(" - ").append(cashDeskDesc.get(i)).append(" - ").append(cashDeskCurr.get(i))
                .append("</option>");
            i++;
        }

        ret.append("</select>");
        retVal.put(ReturnValues.SNIPLETT, ret.toString());

        return retVal;

    }

    /**
     * Method is executed as trigger after the insert of an
     * Products_TransactionInbound.
     *
     * @param _parameter Parameters as passed from eFaps
     * @return Return
     * @throws EFapsException on error
     */
    public Return inboundTrigger(final Parameter _parameter)
        throws EFapsException
    {
        addRemoveFromAccount(_parameter, true);
        updateDocument(_parameter);
        return new Return();
    }

    /**
     * Method is executed as trigger after the insert of an
     * Products_TransactionOutbound.
     *
     * @param _parameter Parameters as passed from eFaps
     * @return Return
     * @throws EFapsException on error
     */
    public Return outboundTrigger(final Parameter _parameter)
        throws EFapsException
    {
        addRemoveFromAccount(_parameter, false);
        updateDocument(_parameter);
        return new Return();
    }

    /**
     * Add or subtract from the Inventory.
     *
     * @param _parameter Parameters as passed from eFaps
     * @param _add if true the quantity will be added else subtracted
     * @throws EFapsException on error
     */
    protected void addRemoveFromAccount(final Parameter _parameter,
                                        final boolean _add)
        throws EFapsException
    {
        final Instance instance = _parameter.getInstance();
        // get the transaction
        final PrintQuery query = new PrintQuery(instance);
        query.addAttribute(CISales.TransactionAbstract.Account, CISales.TransactionAbstract.Amount,
                        CISales.TransactionAbstract.CurrencyId);
        BigDecimal amount = null;
        Long account = null;
        Long currency = null;
        if (query.execute()) {
            amount = (BigDecimal) query.getAttribute(CISales.TransactionAbstract.Amount);
            account = (Long) query.getAttribute(CISales.TransactionAbstract.Account);
            currency = (Long) query.getAttribute(CISales.TransactionAbstract.CurrencyId);
        }

        final QueryBuilder queryBldr = new QueryBuilder(CISales.Balance);
        queryBldr.addWhereAttrEqValue(CISales.Balance.Account, account);
        queryBldr.addWhereAttrEqValue(CISales.Balance.Currency, currency);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.Balance.Amount);
        multi.execute();

        Update update;
        if (multi.next()) {
            update = new Update(multi.getCurrentInstance());
            final BigDecimal current = multi.<BigDecimal>getAttribute(CISales.Balance.Amount);
            if (_add) {
                amount = current.add(amount);
            } else {
                amount = current.subtract(amount);
            }
        } else {
            update = new Insert(CISales.Balance);
            update.add(CISales.Balance.Currency, currency);
            update.add(CISales.Balance.Account, account);
            if (!_add) {
                amount = amount.negate();
            }
        }
        update.add(CISales.Balance.Amount, amount);
        update.execute();
    }

    protected void updateDocument(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instance = _parameter.getInstance();
        // get the transaction
        final PrintQuery print = new PrintQuery(instance);
        final SelectBuilder sel = new SelectBuilder().linkto(CISales.TransactionAbstract.Payment)
                        .linkto(CIERP.Document2PaymentDocumentAbstract.FromAbstractLink).oid();
        print.addSelect(sel);
        print.executeWithoutAccessCheck();

        final Instance docInst = Instance.get(print.<String>getSelect(sel));
        if (docInst.isValid()) {
            _parameter.put(ParameterValues.INSTANCE, docInst);
            final DocumentUpdate docUpdate = new DocumentUpdate() {
                @Override
                protected boolean executeWithoutTrigger(final Parameter _parameter)
                {
                    return true;
                }
            };
            docUpdate.updateDocument(_parameter);
        }
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return return granting access or not
     * @throws EFapsException on error
     */
    public Return accessCheck4Payment(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();

        if (_parameter.get(ParameterValues.ACCESSMODE) == TargetMode.VIEW) {
            final Instance instance = _parameter.getInstance();
            // get the transaction
            final PrintQuery print = new PrintQuery(instance);
            final SelectBuilder sel = new SelectBuilder().linkto(CISales.TransactionAbstract.Payment)
                            .linkto(CIERP.Document2PaymentDocumentAbstract.FromAbstractLink).oid();
            print.addSelect(sel);
            print.executeWithoutAccessCheck();

            final Instance docInst = Instance.get(print.<String>getSelect(sel));
            if (docInst.isValid()) {
                ret.put(ReturnValues.TRUE, true);
            }
        }

        return ret;
    }

    /**
     * Method is called from a transaction to recalculate all values for the
     * accounts.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return empty Return
     * @throws EFapsException on error
     */
    public Return reCalculateAccounts(final Parameter _parameter)
        throws EFapsException
    {
        BigDecimal sumTotal = BigDecimal.ZERO;
        final QueryBuilder queryBldr = new QueryBuilder(CISales.TransactionAbstract);
        queryBldr.addWhereAttrEqValue(CISales.TransactionAbstract.Account, _parameter.getInstance().getId());
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.TransactionAbstract.Amount);
        multi.execute();

        while (multi.next()) {
            final BigDecimal amount = multi.<BigDecimal>getAttribute(CISales.TransactionAbstract.Amount);

            if (multi.getCurrentInstance().getType().equals(CISales.TransactionInbound.getType())) {
                sumTotal = sumTotal.add(amount);
            } else {
                sumTotal = sumTotal.subtract(amount);
            }
        }

        final QueryBuilder queryBldr2 = new QueryBuilder(CISales.Balance);
        queryBldr2.addWhereAttrEqValue(CISales.Balance.Account, _parameter.getInstance().getId());
        final InstanceQuery accQuery = queryBldr2.getQuery();
        accQuery.execute();
        while (accQuery.next()) {
            final Update update = new Update(accQuery.getCurrentValue());
            update.add(CISales.Balance.Amount, sumTotal);
            update.executeWithoutTrigger();
        }

        return new Return();
    }

    public Return formatDate(final Parameter _parameter)
        throws EFapsException
    {
        final StringBuilder js = new StringBuilder();
        final Return retVal = new Return();
        final FieldValue value = (FieldValue) _parameter.get(ParameterValues.UIOBJECT);
        if (value.getDisplay().equals(Display.READONLY)) {
            String dec = (String) value.getValue();
            if (dec == null || (dec != null && dec.contains("null"))) {
                dec = "";
            }
            js.append(dec);
        } else {
            js.append(value.getValue());
        }
        retVal.put(ReturnValues.VALUES, js.toString());
    return retVal;
    }
}
