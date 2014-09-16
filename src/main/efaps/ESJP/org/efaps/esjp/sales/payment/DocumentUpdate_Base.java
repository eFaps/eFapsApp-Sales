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

package org.efaps.esjp.sales.payment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.EventDefinition;
import org.efaps.admin.event.EventType;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.Command;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Checkin;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.StandartReport;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.esjp.erp.util.ERPSettings;
import org.efaps.esjp.sales.document.AbstractDocument_Base;
import org.efaps.util.EFapsException;

/**
 * Class is responsible to update a Document when a PaymentDocument is related
 * to it. e.g change the Status when a invoice was payed completely.
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("46709cec-7b85-4630-9e2c-517db66ce2d0")
@EFapsRevision("$Rev$")
public abstract class DocumentUpdate_Base
{

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return updateDocument(final Parameter _parameter)
        throws EFapsException
    {
        final Instance docInst = _parameter.getInstance();

        final Status targetStatus = getTargetStatus(_parameter, docInst);
        if (targetStatus != null && checkStatus(_parameter, docInst)
                        && validateDocumentCriterias(_parameter, docInst)) {
            setStatus(_parameter, docInst, targetStatus);
        }
        return new Return();
    }

    /**
     * Get the Status the Document will be set to if the criteria are met.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst The Instance of the Document that will be updated
     * @return Status if found, else null
     * @throws EFapsException on error
     */
    protected Status getTargetStatus(final Parameter _parameter,
                                     final Instance _docInst)
        throws EFapsException
    {
        final Type statusType = _docInst.getType().getStatusAttribute().getLink();
        Status ret = Status.find(statusType.getUUID(), "Paid");
        if (ret == null) {
            ret = Status.find(statusType.getUUID(), "Closed");
        }
        return ret;
    }

    /**
     * Check if the current Status of the document allows an automatic update of
     * the Status.
     *
     * To be used by implementation.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst The Instance of the Document that will be updated
     * @return true
     * @throws EFapsException on error
     */
    protected boolean checkStatus(final Parameter _parameter,
                                  final Instance _docInst)
        throws EFapsException
    {
        return true;
    }

    /**
     * Check if the document must be updated.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst The Instance of the Document that will be updated
     * @return true
     * @throws EFapsException on error
     */
    protected boolean validateDocumentCriterias(final Parameter _parameter,
                                                final Instance _docInst)
        throws EFapsException
    {
        final DocPaymentInfo doc = new DocPaymentInfo(_docInst);
        return doc.isPaid();
    }

    /**
     * Check if the document must be updated.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst The Instance of the Document that will be updated
     * @param _status Status the Document will be set to
     * @throws EFapsException on error
     */
    protected void setStatus(final Parameter _parameter,
                             final Instance _docInst,
                             final Status _status)
        throws EFapsException
    {
        final Update update = new Update(_docInst);
        update.add(CIERP.DocumentAbstract.StatusAbstract, _status.getId());
        if (executeWithoutTrigger(_parameter)) {
            update.executeWithoutTrigger();
        } else {
            update.execute();
        }
    }

    /**
     * Must the update be done without trigger.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return false
     * @throws EFapsException on error
     */
    protected boolean executeWithoutTrigger(final Parameter _parameter)
        throws EFapsException
    {
        return false;
    }

    public Return renew(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();

        final Instance instance = _parameter.getInstance();

        if (instance.getType().getUUID().equals(CISales.PaymentCash.uuid)) {
            createFile(_parameter, instance, "Sales_PaymentDocumentMyDesk_Menu_Action_CreatePaymentCash");
        } else if (instance.getType().getUUID().equals(CISales.PaymentCheck.uuid)) {
            createFile(_parameter, instance, "Sales_PaymentDocumentMyDesk_Menu_Action_CreatePaymentCheck");
        } else if (instance.getType().getUUID().equals(CISales.PaymentCreditCardAmericanExpress.uuid)) {
            createFile(_parameter, instance,
                            "Sales_PaymentDocumentMyDesk_Menu_Action_CreatePaymentCreditCardAmericanExpress");
        } else if (instance.getType().getUUID().equals(CISales.PaymentCreditCardDiners.uuid)) {
            createFile(_parameter, instance, "Sales_PaymentDocumentMyDesk_Menu_Action_CreatePaymentCreditCardDiners");
        } else if (instance.getType().getUUID().equals(CISales.PaymentCreditCardMastercard.uuid)) {
            createFile(_parameter, instance,
                            "Sales_PaymentDocumentMyDesk_Menu_Action_CreatePaymentCreditCardMastercard");
        } else if (instance.getType().getUUID().equals(CISales.PaymentCreditCardVisa.uuid)) {
            createFile(_parameter, instance, "Sales_PaymentDocumentMyDesk_Menu_Action_CreatePaymentCreditCardVisa");
        } else if (instance.getType().getUUID().equals(CISales.PaymentDebitCardVisa.uuid)) {
            createFile(_parameter, instance, "Sales_PaymentDocumentMyDesk_Menu_Action_CreatePaymentDebitCardVisa");
        } else if (instance.getType().getUUID().equals(CISales.PaymentDeposit.uuid)) {
            createFile(_parameter, instance, "Sales_PaymentDocumentMyDesk_Menu_Action_CreatePaymentDeposit");
        } else if (instance.getType().getUUID().equals(CISales.PaymentDetraction.uuid)) {
            createFile(_parameter, instance, "Sales_PaymentDocumentMyDesk_Menu_Action_CreatePaymentDetraction");
        } else if (instance.getType().getUUID().equals(CISales.PaymentExchange.uuid)) {
            createFile(_parameter, instance, "Sales_PaymentDocumentMyDesk_Menu_Action_CreatePaymentExchange");
        } else if (instance.getType().getUUID().equals(CISales.PaymentRetention.uuid)) {
            createFile(_parameter, instance, "Sales_PaymentDocumentMyDesk_Menu_Action_CreatePaymentRetention");
        } else if (instance.getType().getUUID().equals(CISales.PaymentCashOut.uuid)) {
            createFile(_parameter, instance, "Sales_PaymentDocumentOutMyDesk_Menu_Action_CreatePaymentCashOut");
        } else if (instance.getType().getUUID().equals(CISales.PaymentCheckOut.uuid)) {
            createFile(_parameter, instance, "Sales_PaymentDocumentOutMyDesk_Menu_Action_CreatePaymentCheckOut");
        } else if (instance.getType().getUUID().equals(CISales.PaymentDepositOut.uuid)) {
            createFile(_parameter, instance, "Sales_PaymentDocumentOutMyDesk_Menu_Action_CreatePaymentDepositOut");
        } else if (instance.getType().getUUID().equals(CISales.PaymentDetractionOut.uuid)) {
            createFile(_parameter, instance, "Sales_PaymentDocumentOutMyDesk_Menu_Action_CreatePaymentDetractionOut");
        } else if (instance.getType().getUUID().equals(CISales.PaymentExchangeOut.uuid)) {
            createFile(_parameter, instance, "Sales_PaymentDocumentOutMyDesk_Menu_Action_CreatePaymentExchangeOut");
        } else if (instance.getType().getUUID().equals(CISales.PaymentRetentionOut.uuid)) {
            createFile(_parameter, instance, "Sales_PaymentDocumentOutMyDesk_Menu_Action_CreatePaymentRetentionOut");
        }  else if (instance.getType().getUUID().equals(CISales.PaymentSupplierOut.uuid)) {
            createFile(_parameter, instance, "Sales_PaymentDocumentOutMyDesk_Menu_Action_CreatePaymentSupplierOut");
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    protected void createFile(final Parameter _parameter,
                              final Instance _instance,
                              final String _cmd)
        throws EFapsException
    {
        final Command cmd = Command.get(_cmd);
        final List<EventDefinition> events = cmd.getEvents(EventType.UI_COMMAND_EXECUTE);
        final EventDefinition definition = events.get(0);
        final String reportName = definition.getProperty("JasperReport");
        final String mimeDef = definition.getProperty("Mime");
        final String clazzDef = definition.getProperty("DataSourceClass");

        final SelectBuilder selFileName = new SelectBuilder().file().label();
        final PrintQuery print = new PrintQuery(_instance);

        print.addSelect(selFileName);
        print.addAttribute("Name");
        print.execute();
        String name = print.<String>getSelect(selFileName);

        _parameter.put(ParameterValues.INSTANCE, _instance);
        final Map<String, String> props = (Map<String, String>) _parameter.get(ParameterValues.PROPERTIES);
        props.put("JasperReport", reportName);
        String mime = _parameter.getParameterValue("mime");
        if (!(mime != null && !mime.isEmpty())) {
            mime = mimeDef != null ? mimeDef : "txt";
        }
        props.put("Mime", mime);
        if (clazzDef != null) {
            props.put("DataSourceClass", clazzDef);
        }

        if (name != null && !name.isEmpty()) {
            name = name.substring(0, name.lastIndexOf("."));
        } else {
            name = _instance.getType().getLabel() + "_" + print.<String>getAttribute("Name");
        }

        String accName = null;
        String accCurName = null;

        final StandartReport report = new StandartReport();
        report.setFileName(name);

        final QueryBuilder attrQueryBldr =  new QueryBuilder(CISales.Payment);
        attrQueryBldr.addWhereAttrEqValue(CISales.Payment.TargetDocument, _instance.getId());
        final AttributeQuery attrQuery =  attrQueryBldr.getAttributeQuery(CISales.Payment.ID);

        final SelectBuilder selAccName = new SelectBuilder().linkto(CISales.TransactionAbstract.Account)
                        .attribute(CISales.AccountAbstract.Name);
        final SelectBuilder selAccCurName = new SelectBuilder().linkto(CISales.TransactionAbstract.Account)
                        .linkto(CISales.AccountAbstract.CurrencyLink).attribute(CIERP.Currency.Name);

        final QueryBuilder queryBldr2 = new QueryBuilder(CISales.TransactionAbstract);
        queryBldr2.addWhereAttrInQuery(CISales.TransactionAbstract.Payment, attrQuery);
        final MultiPrintQuery multi = queryBldr2.getPrint();
        multi.addSelect(selAccName, selAccCurName);
        multi.execute();
        if (multi.next()){
            accName = multi.<String>getSelect(selAccName);
            accCurName = multi.<String>getSelect(selAccCurName);
        }
        report.getJrParameters().put("accountName", accName);
        report.getJrParameters().put("accountCurrencyName", accCurName);

        final SystemConfiguration config = ERP.getSysConfig();
        if (config != null) {
            final String companyName = config.getAttributeValue(ERPSettings.COMPANYNAME);
            final String companyTaxNumb = config.getAttributeValue(ERPSettings.COMPANYTAX);

            if (companyName != null && companyTaxNumb != null && !companyName.isEmpty()
                            && !companyTaxNumb.isEmpty()) {
                report.getJrParameters().put("CompanyName", companyName);
                report.getJrParameters().put("CompanyTaxNum", companyTaxNumb);
            }
        }
        addParameterReport4Renew(_instance, report);
        final Return ret = report.execute(_parameter);

        final File file = (File) ret.get(ReturnValues.VALUES);
        InputStream input = null;
        try {
            input = new FileInputStream(file);
        } catch (final FileNotFoundException e) {
            throw new EFapsException(AbstractDocument_Base.class, "createFile", e);
        }

        final Checkin checkin = new Checkin(_instance);
        checkin.execute(name + "." + mime, input, ((Long) file.length()).intValue());
    }

    protected void addParameterReport4Renew(final Instance _instance,
                                            final StandartReport _report)
        throws EFapsException
    {
        // TODO Auto-generated method stub

    }

}
