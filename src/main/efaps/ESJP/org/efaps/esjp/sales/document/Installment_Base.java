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
package org.efaps.esjp.sales.document;

import java.io.File;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * The Class IncomingInstallment_Base.
 *
 * @author The eFaps Team
 */
@EFapsUUID("f425a374-fa40-42eb-98ca-e77536f5d949")
@EFapsApplication("eFapsApp-Sales")
public abstract class Installment_Base
    extends AbstractSumOnlyDocument
{

    /**
     * Creates the.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final CreatedDoc createdDoc = createDoc(_parameter);
        connect2Object(_parameter, createdDoc);
        updateCreditDueDate(_parameter, createdDoc.getInstance());

        final File file = createReport(_parameter, createdDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        ret.put(ReturnValues.INSTANCE, createdDoc.getInstance());
        return ret;
    }

    /**
     * Creates the.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException on error
     */
    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final EditedDoc editDoc = editDoc(_parameter);
        updateConnection2Object(_parameter, editDoc);
        updateCreditDueDate(_parameter, editDoc.getInstance());

        final File file = createReport(_parameter, editDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    @Override
    public Return getJavaScriptUIValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final PrintQuery print = new PrintQuery(_parameter.getCallInstance());
        final SelectBuilder selContactInst = SelectBuilder.get().linkto(CISales.DocumentAbstract.Contact).instance();
        print.addSelect(selContactInst);
        print.execute();
        final Instance contactInst = print.getSelect(selContactInst);

        retVal.put(ReturnValues.SNIPLETT, InterfaceUtils.wrappInScriptTag(_parameter, add2JavaScript4DocumentContact(
                        _parameter, null, contactInst), true, 1500));
        return retVal;
    }

    /**
     * Update credit due date to highest date.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @throws EFapsException
     */
    protected void updateCreditDueDate(final Parameter _parameter,
                                       final Instance _instance)
        throws EFapsException
    {
        if (InstanceUtils.isValid(_instance)) {
            final PrintQuery print = new PrintQuery(_instance);
            final SelectBuilder selCreditInst = SelectBuilder.get().linkfrom(CISales.Installment2Credit.FromLink).linkto(
                            CISales.Installment2Credit.ToLink).instance();
            print.addSelect(selCreditInst);
            print.execute();
            final Instance creditInst = print.getSelect(selCreditInst);
            if (InstanceUtils.isKindOf(creditInst, CISales.Credit)) {

                final QueryBuilder atrtQueryBldr = new QueryBuilder(CISales.Installment2Credit);
                atrtQueryBldr.addWhereAttrEqValue(CISales.Installment2Credit.ToLink, creditInst);

                final QueryBuilder queryBldr = new QueryBuilder(CISales.Installment);
                queryBldr.addWhereAttrInQuery(CISales.Installment.ID, atrtQueryBldr.getAttributeQuery(
                                CISales.Installment2Credit.FromLink));
                queryBldr.addOrderByAttributeDesc(CISales.Installment.DueDate);
                queryBldr.setLimit(1);

                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CISales.Installment.DueDate);
                multi.execute();
                if (multi.next()) {
                    final Update update = new Update(creditInst);
                    update.add(CISales.Credit.DueDate, multi.<DateTime>getAttribute(CISales.Installment.DueDate));
                    update.executeWithoutAccessCheck();
                }
            }
        }
    }
}
