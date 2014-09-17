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

import org.efaps.admin.datamodel.Attribute;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.NumberFormatter;
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
    extends AbstractDocumentSum
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

        final Instance baseCurrInst = Currency.getBaseCurrency();
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

    /**
     * Method to update amount.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return insertPostTrigger(final Parameter _parameter)
        throws EFapsException
    {
        updateAmount(_parameter);
        recalculate(_parameter, false);
        return new Return();
    }

    /**
     * Method to update amount.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return updatePostTrigger(final Parameter _parameter)
        throws EFapsException
    {
        recalculate(_parameter, false);
        return new Return();
    }

    /**
     * Method to recalculate amount to be removed.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return deletePreTrigger(final Parameter _parameter)
        throws EFapsException
    {
        recalculate(_parameter, true);
        return new Return();
    }

    /**
     * Method to update amount with new values.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @throws EFapsException on error.
     */
    protected void updateAmount(final Parameter _parameter)
        throws EFapsException
    {
        final SelectBuilder selRateCurInst = new SelectBuilder()
                        .linkto(CISales.DocumentSumAbstract.RateCurrencyId).instance();

        final Map<?, ?> values = (HashMap<?, ?>) _parameter.get(ParameterValues.NEW_VALUES);
        final Attribute attr = _parameter.getInstance().getType().getAttribute("ToLink");

        final QueryBuilder queryBldr = new QueryBuilder(CISales.DocumentSumAbstract);
        queryBldr.addWhereAttrEqValue(CISales.DocumentSumAbstract.ID, (Object[]) values.get(attr));
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.DocumentSumAbstract.RateCrossTotal,
                        CISales.DocumentSumAbstract.CrossTotal);
        multi.addSelect(selRateCurInst);
        if (multi.execute()) {
            final Attribute attr2 = _parameter.getInstance().getType().getAttribute("FromLink");
            final QueryBuilder queryBldr2 = new QueryBuilder(CISales.DocumentSumAbstract);
            queryBldr2.addWhereAttrEqValue(CISales.DocumentSumAbstract.ID, (Object[]) values.get(attr2));
            final MultiPrintQuery multi2 = queryBldr2.getPrint();
            multi2.addAttribute(CISales.DocumentSumAbstract.Rate);
            multi2.addSelect(selRateCurInst);
            if (multi2.execute()) {
                final Instance curInst = multi.<Instance>getSelect(selRateCurInst);
                final Instance curInst2 = multi2.<Instance>getSelect(selRateCurInst);

                BigDecimal amount = BigDecimal.ZERO;
                if (curInst.equals(curInst2)) {
                    amount = multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
                } else {
                    final Instance baseCurrInst = Currency.getBaseCurrency();
                    if (!curInst.equals(baseCurrInst) && curInst2.equals(baseCurrInst)) {
                        amount = multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.CrossTotal);
                    } else {
                        final DecimalFormat frmt = NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter));
                        final int scale = frmt.getMaximumFractionDigits();
                        final Object[] rateObj = multi2.<Object[]>getAttribute(CISales.DocumentSumAbstract.Rate);
                        final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                                        BigDecimal.ROUND_HALF_UP);
                        amount = multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal)
                                        .multiply(rate).setScale(scale, BigDecimal.ROUND_HALF_UP);
                    }
                }
                final Update update = new Update(_parameter.getInstance());
                update.add(CISales.Document2DocumentWithAmount.Amount, amount);
                update.executeWithoutTrigger();
            }
        }
    }

    /**
     * Method to recalculate amounts and update document.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @param _exclude boolean.
     * @throws EFapsException on error.
     */
    protected void recalculate(final Parameter _parameter,
                               final boolean _exclude)
        throws EFapsException
    {
        final Instance instance = _parameter.getInstance();

        if (instance != null && instance.isValid()) {
            final SelectBuilder selDocInst = new SelectBuilder()
                            .linkto(CISales.Document2DocumentWithAmount.FromAbstractLink).instance();
            final SelectBuilder selDocRate = new SelectBuilder()
                            .linkto(CISales.Document2DocumentWithAmount.FromAbstractLink)
                            .attribute(CISales.DocumentSumAbstract.Rate);
            final SelectBuilder selDocRateCur = new SelectBuilder()
                            .linkto(CISales.Document2DocumentWithAmount.FromAbstractLink)
                            .attribute(CISales.DocumentSumAbstract.RateCurrencyId);
            final SelectBuilder selDoc2DocRate = new SelectBuilder()
                            .linkto(CISales.Document2DocumentWithAmount.ToAbstractLink)
                            .attribute(CISales.DocumentSumAbstract.Rate);

            final PrintQuery print = new PrintQuery(instance);
            print.addSelect(selDocInst, selDocRate, selDocRateCur, selDoc2DocRate);
            print.execute();

            final Instance document = print.<Instance>getSelect(selDocInst);
            Object[] rateObjDoc = print.<Object[]>getSelect(selDocRate);
            final Instance rateCur = Instance.get(CIERP.Currency.getType(), print.<Long>getSelect(selDocRateCur));

            if (document != null && document.isValid()) {
                if (Currency.getBaseCurrency().equals(rateCur)) {
                    rateObjDoc = print.<Object[]>getSelect(selDoc2DocRate);
                }

                final QueryBuilder queryBldr = new QueryBuilder(CISales.Document2DocumentWithAmount);
                queryBldr.addWhereAttrEqValue(CISales.Document2DocumentWithAmount.FromAbstractLink, document);
                if (_exclude) {
                    queryBldr.addWhereAttrNotEqValue(CISales.Document2DocumentWithAmount.ID, instance);
                }
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CISales.Document2DocumentWithAmount.Amount);
                multi.execute();

                BigDecimal rateAmount = BigDecimal.ZERO;
                while (multi.next()) {
                    rateAmount = rateAmount
                                    .add(multi.<BigDecimal>getAttribute(CISales.Document2DocumentWithAmount.Amount));
                }
                final BigDecimal rate = ((BigDecimal) rateObjDoc[0])
                                .divide((BigDecimal) rateObjDoc[1], 12, BigDecimal.ROUND_HALF_UP);
                final DecimalFormat frmt = NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter));
                final int scale = frmt.getMaximumFractionDigits();

                final Update update = new Update(document);
                update.add(CISales.DocumentSumAbstract.RateCrossTotal, rateAmount);
                update.add(CISales.DocumentSumAbstract.RateNetTotal, rateAmount);
                final BigDecimal amount = rateAmount.divide(rate, BigDecimal.ROUND_HALF_UP).setScale(scale,
                                BigDecimal.ROUND_HALF_UP);
                update.add(CISales.DocumentSumAbstract.CrossTotal, amount);
                update.add(CISales.DocumentSumAbstract.NetTotal, amount);
                update.executeWithoutTrigger();
            }
        }
    }
}
