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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.admin.ui.field.Field;
import org.efaps.admin.ui.field.Field.Display;
import org.efaps.db.Context;
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
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

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
        final PrintQuery print = new PrintQuery(instance);
        final SelectBuilder accSel = SelectBuilder.get().linkto(CISales.TransactionAbstract.Account).instance();
        final SelectBuilder curSel = SelectBuilder.get().linkto(CISales.TransactionAbstract.CurrencyId).instance();
        print.addSelect(accSel, curSel);
        print.addAttribute(CISales.TransactionAbstract.Amount);
        BigDecimal amount = BigDecimal.ZERO;
        Instance accountInst = null;
        Instance currencyInst = null;
        if (print.execute()) {
            amount = print.<BigDecimal>getAttribute(CISales.TransactionAbstract.Amount);
            accountInst =  print.<Instance>getSelect(accSel);
            currencyInst = print.<Instance>getSelect(curSel);
        }
        if (!_add) {
            amount = amount.negate();
        }
        updateBalance(_parameter, accountInst, currencyInst, amount);
    }

    public void updateBalance(final Parameter _parameter,
                              final Instance _accInst,
                              final Instance _currInst,
                              final BigDecimal _amount)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CISales.Balance);
        queryBldr.addWhereAttrEqValue(CISales.Balance.Account, _accInst);
        queryBldr.addWhereAttrEqValue(CISales.Balance.Currency, _currInst);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.Balance.Amount);
        multi.execute();
        BigDecimal amount = _amount;
        Update update;
        if (multi.next()) {
            update = new Update(multi.getCurrentInstance());
            final BigDecimal current = multi.<BigDecimal>getAttribute(CISales.Balance.Amount);
            amount = current.add(_amount);
        } else {
            update = new Insert(CISales.Balance);
            update.add(CISales.Balance.Currency, _currInst);
            update.add(CISales.Balance.Account, _accInst);
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
        final SelectBuilder selAcc = new SelectBuilder().linkto(CISales.TransactionAbstract.Account).instance();
        print.addSelect(sel, selAcc);
        print.executeWithoutAccessCheck();

        final Instance docInst = Instance.get(print.<String>getSelect(sel));
        final Instance accInst = print.<Instance>getSelect(selAcc);
        if (docInst.isValid() && (accInst != null && !accInst.getType().equals(CISales.AccountPettyCash.getType()))) {
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

    public Return getTable4TransactionFieldValueUI(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final StringBuilder html = new StringBuilder();
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String amountName = (String) properties.get("AmountName");
        final String currencyName = (String) properties.get("CurrencyName");
        final String costName = (String) properties.get("CostName");
        final String currencyId = (String) properties.get("CurrencyId");

        html.append("<table>");
        html.append("<tr>");
        html.append("<td>").append(DBProperties.getProperty("org.efaps.esjp.sales.Transaction.Amount")).append("</td>");
        html.append("<td>").append(DBProperties.getProperty("org.efaps.esjp.sales.Transaction.Currency"))
                        .append("</td>");
        html.append("<td>").append(DBProperties.getProperty("org.efaps.esjp.sales.Transaction.Cost")).append("</td>");
        html.append("</tr>");

        html.append("<tr>");
        html.append("<td>").append("<input type=\"text\" size=\"6\" name=\"").append(amountName).append("\"/>")
                        .append("</td>");
        html.append("<td>").append("<input type=\"text\" size=\"6\" name=\"").append(currencyName)
                        .append("\" value=\"\"").append(" readonly=\"readonly\" ").append("\"/>")
                        .append("<input type=\"hidden\" name=\"").append(currencyId).append("\" value=\"\"")
                        .append("\"/>").append("</td>");
        html.append("<td>").append("<input type=\"text\" size=\"6\" name=\"").append(costName).append("\"/>")
                        .append("</td>");
        html.append("</table>");

        ret.put(ReturnValues.SNIPLETT, html.toString());
        return ret;
    }

    public Return updateDropDownTransaction(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final StringBuilder html = new StringBuilder();
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        Long currencyId = Long.valueOf(0);
        final String currencyType = (String) properties.get("CurrencyType");
        final String currId = (String) properties.get("CurrencyId");
        final Field field = (Field) _parameter.get(ParameterValues.UIOBJECT);
        html.append("document.getElementsByName('").append(currencyType).append("')[0].value='");

        if (!_parameter.getParameterValue(field.getName()).isEmpty()) {
            final Long id = Long.parseLong(_parameter.getParameterValue(field.getName()));

            final QueryBuilder queryBldr = new QueryBuilder(CISales.AccountAbstract);
            queryBldr.addWhereAttrEqValue(CISales.AccountAbstract.ID, id);
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selCurrencylabel = new SelectBuilder().linkto(CISales.AccountAbstract.CurrencyLink)
                            .attribute(CIERP.Currency.Name);
            multi.addSelect(selCurrencylabel);
            multi.addAttribute(CISales.AccountAbstract.CurrencyLink);
            multi.execute();
            if (multi.next()) {
                final String currencyName = multi.<String>getSelect(selCurrencylabel);
                currencyId = multi.<Long>getAttribute(CISales.AccountAbstract.CurrencyLink);
                if (currencyName != null) {
                    html.append(currencyName);
                }
            }
        }
        html.append("';");

        html.append("document.getElementsByName('").append(currId).append("')[0].value='").append(currencyId)
                        .append("';");
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(EFapsKey.FIELDUPDATE_JAVASCRIPT.getKey(), html.toString());
        list.add(map);
        ret.put(ReturnValues.VALUES, list);
        return ret;
    }

    public Return createInternalTransfer(final Parameter _parameter)
        throws EFapsException
    {
        final String costOutStr = _parameter.getParameterValue("cost_outbound");
        final String currIdOutStr = _parameter.getParameterValue("currencyId_outbound");
        final String amountOutStr = _parameter.getParameterValue("amount_outbound");
        final String costInStr = _parameter.getParameterValue("cost_inbound");
        final String currIdInStr = _parameter.getParameterValue("currencyId_inbound");
        final String amountInStr = _parameter.getParameterValue("amount_inbound");
        final String charger = _parameter.getParameterValue("charger");
        final String payment = _parameter.getParameterValue("payment");

        // transaction outbound
        createInternalTransaction(_parameter, CISales.TransactionOutbound.getType(), charger, costOutStr, currIdOutStr,
                        amountOutStr);
        // transaction inbound
        createInternalTransaction(_parameter, CISales.TransactionInbound.getType(), payment, costInStr, currIdInStr,
                        amountInStr);
        return new Return();
    }

    /**
     * Method for create internal transfers.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return createInternalTransaction(final Parameter _parameter,
                                            final Type _transactionType,
                                            final String _account,
                                            final String _cost,
                                            final String _currencyId,
                                            final String _amount)
        throws EFapsException
    {
        final String note = _parameter.getParameterValue("note");

        Instance instCharger = null;
        if (!_account.isEmpty() && _account != null) {
            final QueryBuilder queryBldr = new QueryBuilder(CISales.AccountAbstract);
            queryBldr.addWhereAttrEqValue(CISales.AccountAbstract.ID, _account);
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CISales.AccountAbstract.OID);
            multi.execute();
            if (multi.next()) {
                final String accountOid = multi.<String>getAttribute(CISales.AccountAbstract.OID);
                instCharger = Instance.get(accountOid);
            }
        }

        Long currId = Long.valueOf(0);
        if (!_currencyId.isEmpty() && _currencyId != null) {
            currId = Long.parseLong(_currencyId);
        }
        final DecimalFormat formater = (DecimalFormat) NumberFormat.getInstance(Context.getThreadContext().getLocale());
        formater.setParseBigDecimal(true);
        if (_cost != null && !_cost.isEmpty() && instCharger.isValid() && instCharger != null) {
            BigDecimal cost = BigDecimal.ZERO;

            try {
                cost = (BigDecimal) formater.parse(_cost);
            } catch (final ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (cost.compareTo(BigDecimal.ZERO) != 0) {
                final Insert transInsertCost = new Insert(_transactionType);
                transInsertCost.add(CISales.TransactionAbstract.Amount, cost);
                transInsertCost.add(CISales.TransactionAbstract.CurrencyId, currId);
                transInsertCost.add(CISales.TransactionAbstract.Account, instCharger.getId());
                transInsertCost.add(CISales.TransactionAbstract.Description, note);
                transInsertCost.add(CISales.TransactionAbstract.Date, new DateTime());
                transInsertCost.execute();
            }
        }

        if (_amount != null && !_amount.isEmpty() && instCharger.isValid() && instCharger != null) {
            BigDecimal amount = BigDecimal.ZERO;
            try {
                amount = (BigDecimal) formater.parse(_amount);
            } catch (final ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            final Insert transInsertAmount = new Insert(_transactionType);
            transInsertAmount.add(CISales.TransactionAbstract.Amount, amount);
            transInsertAmount.add(CISales.TransactionAbstract.CurrencyId, currId);
            transInsertAmount.add(CISales.TransactionAbstract.Account, instCharger.getId());
            transInsertAmount.add(CISales.TransactionAbstract.Description, note);
            transInsertAmount.add(CISales.TransactionAbstract.Date, new DateTime());
            transInsertAmount.execute();
        }

        return new Return();
    }

}
