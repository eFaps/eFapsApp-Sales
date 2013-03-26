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
import java.util.TreeMap;
import java.util.UUID;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.EventDefinition;
import org.efaps.admin.event.EventType;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Parameter.ParameterValues;
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
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.StandartReport;
import org.efaps.esjp.sales.document.AbstractDocument_Base;
import org.efaps.util.EFapsException;

/**
 * This class must be replaced for customization, therefore it is left empty.
 * Functional description can be found in the related "<code>_base</code>"
 * class.
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("00a800f8-66f6-4040-a082-79f2c96f0f3c")
@EFapsRevision("$Rev$")
public class DocumentUpdate
    extends DocumentUpdate_Base
{

	public Return renew(final Parameter _parameter) throws EFapsException
	{
	    Return ret = new Return();
	     
	        final Instance instance = _parameter.getInstance();
	        
	        if (instance.getType().getUUID().equals(CISales.PaymentCash.uuid)) {
	            createFile(_parameter, instance, "Sales_PaymentDocumentMyDesk_Menu_Action_CreatePaymentCash");
	        } else if (instance.getType().getUUID().equals(CISales.PaymentCheck.uuid)) {
	            createFile(_parameter, instance, "Sales_PaymentDocumentMyDesk_Menu_Action_CreatePaymentCheck");
	        } else if (instance.getType().getUUID().equals(CISales.PaymentCreditCardAmericanExpress.uuid)) {
	            createFile(_parameter, instance, "Sales_PaymentDocumentMyDesk_Menu_Action_CreatePaymentCreditCardAmericanExpress");
	        } else if (instance.getType().getUUID().equals(CISales.PaymentCreditCardDiners.uuid)) {
	            createFile(_parameter, instance, "Sales_PaymentDocumentMyDesk_Menu_Action_CreatePaymentCreditCardDiners");
	        } else if (instance.getType().getUUID().equals(CISales.PaymentCreditCardMastercard.uuid)) {
	            createFile(_parameter, instance, "Sales_PaymentDocumentMyDesk_Menu_Action_CreatePaymentCreditCardMastercard");
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
	        } 
	    return ret;
	}
	protected void createFile(final Parameter _parameter,
	        final Instance _instance,
	        final String _cmd)
	throws EFapsException
	{
		final Command cmd = Command.get(_cmd);
		final List<EventDefinition> events = cmd.getEvents(EventType.UI_COMMAND_EXECUTE);
		final EventDefinition def = events.get(0);
		final String reportName = def.getProperty("JasperReport");
		final String mime2 = def.getProperty("Mime");
		final String clazz = def.getProperty("DataSourceClass");
		
		final PrintQuery print = new PrintQuery(_instance);
		final SelectBuilder selFileName = new SelectBuilder().file().label();
		print.addSelect(selFileName);
		print.addAttribute("Name");
		print.execute();
		String name = print.<String>getSelect(selFileName);
		_parameter.get(ParameterValues.PROPERTIES);
		_parameter.put(ParameterValues.INSTANCE, _instance);
		final Map<String, String> props = (Map<String, String>) _parameter.get(ParameterValues.PROPERTIES);
		props.put("JasperReport", reportName);
		String mime = _parameter.getParameterValue("mime");
		if (!(mime != null && mime.length() > 0)) {
		mime = mime2 != null ? mime2 : "txt";
		}
		props.put("Mime", mime);
		if (clazz != null) {
		props.put("DataSourceClass", clazz);
		}
		if (name != null && !name.isEmpty()) {
		name = name.substring(0, name.lastIndexOf("."));
		} else {
		name = _instance.getType().getLabel() + "_" + print.getAttribute("Name");
		}
		final StandartReport report = new StandartReport();
		report.setFileName(name);
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
	
}
