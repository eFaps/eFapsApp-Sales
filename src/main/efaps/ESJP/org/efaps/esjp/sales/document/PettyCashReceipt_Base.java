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
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIContacts;
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
        final boolean evaluatePostions = !"false".equalsIgnoreCase(getProperty(_parameter, "EvaluatePostions"));
        // first check the positions
        final List<Calculator> calcList = analyseTable(_parameter, null);
        if (evaluatePostions && calcList.isEmpty() || getNetTotal(_parameter, calcList).compareTo(BigDecimal.ZERO) == 0) {
            html.append(DBProperties.getProperty(PettyCashReceipt.class.getName() + ".validate4Positions"));
        } else {
            if (evalDeducible(_parameter)) {
                final Return tmp = validateName(_parameter);
                final String snipplet = (String) tmp.get(ReturnValues.SNIPLETT);
                if (snipplet != null) {
                    html.append(snipplet);
                }
                final String name = _parameter
                                .getParameterValue(CIFormSales.Sales_PettyCashReceiptForm.name4create.name);
                final Instance contactInst = Instance.get(_parameter
                                .getParameterValue(CIFormSales.Sales_PettyCashReceiptForm.contact.name));
                if (name != null && !name.isEmpty() && contactInst.isValid()) {
                    ret.put(ReturnValues.TRUE, true);
                } else {
                    html.append(DBProperties.getProperty(PettyCashReceipt.class.getName() + ".validate4Deducible"));
                }
            } else {
                final String name = _parameter
                                .getParameterValue(CIFormSales.Sales_PettyCashReceiptForm.name4create.name);
                final String contact = _parameter
                                .getParameterValue(CIFormSales.Sales_PettyCashReceiptForm.contact.name);
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

    public Return getJavaScriptUIValue4EditJustification(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Instance inst = _parameter.getCallInstance();
        final StringBuilder js = new StringBuilder();
        js.append("<script type=\"text/javascript\">")
                        .append("Wicket.Event.add(window, \"domready\", function(event) {");

        if (inst.isValid()) {
            final PrintQuery print = new PrintQuery(inst);
            final SelectBuilder selContOid = new SelectBuilder().linkto(CISales.PettyCashReceipt.Contact).oid();
            final SelectBuilder selContName = new SelectBuilder().linkto(CISales.PettyCashReceipt.Contact)
                            .attribute(CIContacts.Contact.Name);
            final SelectBuilder selDocName = new SelectBuilder().clazz(CISales.PettyCashReceipt_Class)
                            .attribute(CISales.PettyCashReceipt_Class.Name);
            final SelectBuilder selEmplName = new SelectBuilder().clazz(CISales.PettyCashReceipt_Class)
                            .attribute(CISales.PettyCashReceipt_Class.EmployeeName);
            print.addSelect(selContOid, selContName, selDocName, selEmplName);
            if (print.execute()) {
                final Instance contInst = Instance.get(print.<String>getSelect(selContOid));
                final String contName = print.<String>getSelect(selContName);
                final String docName = print.<String>getSelect(selDocName);
                final String empName = print.<String>getSelect(selEmplName);
                if (contInst.isValid()) {
                    js.append(getSetFieldValue(0, "contactAutoComplete", contName, true))
                                    .append(getSetFieldValue(0, "contact", contInst.getOid(), true));
                }
                if (docName != null) {
                    js.append(getSetFieldValue(0, "name", docName, true));
                }
                if (empName != null) {
                    js.append(getSetFieldValue(0, "employeeName", empName, true));
                }
            }
            js.append(" });").append("</script>");
        }
        ret.put(ReturnValues.SNIPLETT, js.toString());
        return ret;
    }


    public Return editJustification4PettyCashReceipt(final Parameter _parameter)
        throws EFapsException
    {
        final String contact = _parameter.getParameterValue("contact");
        final String docName = _parameter.getParameterValue("name");
        final String emp = _parameter.getParameterValue("employeeName");
        _parameter.getParameterValue("receiptTypeLink");
        final Update update = new Update(_parameter.getCallInstance());
        update.add(CISales.PettyCashReceipt.Contact, contact.length() > 0 ? Instance.get(contact).getId() : null);
        update.execute();

        final PrintQuery print = new PrintQuery(_parameter.getCallInstance());
        final SelectBuilder selClazz = new SelectBuilder().clazz(CISales.PettyCashReceipt_Class).oid();
        print.addSelect(selClazz);
        if (print.execute()) {
            final Instance instClazz = Instance.get(print.<String>getSelect(selClazz));
            if (instClazz.isValid()) {
                final Update update2 = new Update(instClazz);
                update2.add(CISales.PettyCashReceipt_Class.Name, docName.length() > 0 ? docName : null);
                update2.add(CISales.PettyCashReceipt_Class.EmployeeName, emp.length() > 0 ? emp : null);
                // update2.add(CISales.PettyCashReceipt_Class.ReceiptTypeLink,
                // typeDoc.length() > 0 ? typeDoc : null);
                update2.execute();
            }
        }
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
