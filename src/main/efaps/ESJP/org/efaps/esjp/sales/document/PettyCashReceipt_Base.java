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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.sales.Account;
import org.efaps.esjp.sales.Calculator;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("9b1b92aa-550a-48d3-a58e-2c47e54802f9")
@EFapsRevision("$Rev$")
public abstract class PettyCashReceipt_Base
    extends DocumentSum
{

    /**
     * Executed from a Command execute vent to create a new PettyCashReceipt.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = createDoc(_parameter);
        createPositions(_parameter, createdDoc);
        connect2DocumentType(_parameter, createdDoc);
        connect2Account(_parameter, createdDoc);
        createTransaction(_parameter, createdDoc);
        return new Return();
    }

    protected void connect2Account(final Parameter _parameter,
                                   final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final Insert insert = new Insert(CISales.AccountPettyCash2PettyCashReceipt);
        insert.add(CISales.AccountPettyCash2PettyCashReceipt.FromLink, _parameter.getInstance());
        insert.add(CISales.AccountPettyCash2PettyCashReceipt.ToLink, _createdDoc.getInstance());
        insert.execute();
    }

    protected void createTransaction(final Parameter _parameter,
                                     final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final Insert payInsert = new Insert(CISales.Payment);
        payInsert.add(CISales.Payment.Date, _createdDoc.getValue(CISales.DocumentSumAbstract.Date.name));
        payInsert.add(CISales.Payment.CreateDocument, _createdDoc.getInstance());
        payInsert.execute();

        final Insert transInsert = new Insert(CISales.TransactionOutbound);
        transInsert.add(CISales.TransactionOutbound.Amount,
                        _createdDoc.getValue(CISales.DocumentSumAbstract.RateCrossTotal.name));
        transInsert.add(CISales.TransactionOutbound.CurrencyId,
                        _createdDoc.getValue(CISales.DocumentSumAbstract.RateCurrencyId.name));
        transInsert.add(CISales.TransactionOutbound.Payment, payInsert.getInstance());
        transInsert.add(CISales.TransactionOutbound.Account, _parameter.getInstance());
        transInsert.add(CISales.TransactionOutbound.Description,
                        _createdDoc.getValue(CISales.DocumentSumAbstract.Note.name));
        transInsert.add(CISales.TransactionOutbound.Date, _createdDoc.getValue(CISales.DocumentSumAbstract.Date.name));
        transInsert.execute();
    }



    @Override
    protected String getDocName4Create(final Parameter _parameter)
        throws EFapsException
    {
        String ret = _parameter.getParameterValue("name4create");
        if (ret == null || (ret != null && ret.isEmpty())) {
            final PrintQuery print = new PrintQuery(_parameter.getInstance());
            print.addAttribute(CISales.AccountPettyCash.Name);
            print.execute();
            final String accName = print.<String>getAttribute(CISales.AccountPettyCash.Name);
            ret =  accName + " " + (new Account().getMaxPosition(_parameter, _parameter.getInstance()) + 1);
        }
        return ret;
    }

    public Return validate(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final StringBuilder html = new StringBuilder();

        // first check the positions
        final List<Calculator> calcList = analyseTable(_parameter, null);
        if (calcList.isEmpty() || getNetTotal(_parameter, calcList).compareTo(BigDecimal.ZERO) ==0) {
            html.append(DBProperties.getProperty(PettyCashReceipt.class.getName() + ".validate4Positions"));
        } else {
            if (evalDeducible(_parameter)) {
                final Return tmp = validateName(_parameter);
                final String snipplet = (String) tmp.get(ReturnValues.SNIPLETT);
                if (snipplet != null) {
                    html.append(snipplet);
                }
                final String name = _parameter.getParameterValue(CIFormSales.Sales_PettyCashReceiptForm.name4create.name);
                final Instance contactInst = Instance.get(_parameter
                                .getParameterValue(CIFormSales.Sales_PettyCashReceiptForm.contact.name));
                if (name != null && !name.isEmpty() && contactInst.isValid()) {
                    ret.put(ReturnValues.TRUE, true);
                } else {
                    html.append(DBProperties.getProperty(PettyCashReceipt.class.getName() + ".validate4Deducible"));
                }
            } else {
                final String name = _parameter.getParameterValue(CIFormSales.Sales_PettyCashReceiptForm.name4create.name);
                final String contact = _parameter.getParameterValue(CIFormSales.Sales_PettyCashReceiptForm.contact.name);
                if ((name == null || name.isEmpty()) && (contact == null || contact.isEmpty())) {
                    ret.put(ReturnValues.TRUE, true);
                } else {
                    html.append(DBProperties.getProperty(PettyCashReceipt.class.getName() + ".validate4NotDeducible"));
                }
            }
        }
        if (html.length() > 0) {
            ret.put(ReturnValues.SNIPLETT, html.toString());
        }
        return ret;
    }

    public Return update4DocumentType(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        if (!evalDeducible(_parameter)) {
            final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
            final Map<String, String> map = new HashMap<String, String>();
            list.add(map);
            map.put(CIFormSales.Sales_PettyCashReceiptForm.name4create.name, "");
            map.put(CIFormSales.Sales_PettyCashReceiptForm.contactData.name, "");
            map.put(CIFormSales.Sales_PettyCashReceiptForm.contact.name, "");
            map.put(CIFormSales.Sales_PettyCashReceiptForm.contact.name + "AutoComplete", "");
            retVal.put(ReturnValues.VALUES, list);
        }
        return retVal;
    }

    protected boolean evalDeducible(final Parameter _parameter)
    {
        return !"NONE".equals(_parameter.getParameterValue(CIFormSales.Sales_PettyCashReceiptForm.documentType.name));
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
    public Return dropDown4DocumentType(final Parameter _parameter)
        throws EFapsException
    {
        return new org.efaps.esjp.common.uiform.Field()
        {

            @Override
            protected void updatePositionList(final Parameter _parameter,
                                              final List<DropDownPosition> _values)
                throws EFapsException
            {
                final DropDownPosition ddPos = new DropDownPosition("NONE",
                                DBProperties.getProperty(PettyCashReceipt.class.getName() + ".NONEPosition.Label"));
                _values.add(0, ddPos);
            };
        } .dropDownFieldValue(_parameter);
    }
}
