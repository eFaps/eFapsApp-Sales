/*
 * Copyright 2003 - 2012 The eFaps Team
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.ci.CITableSales;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.document.AbstractDocumentTax;
import org.efaps.esjp.sales.document.AbstractDocumentTax_Base.DocTaxInfo;
import org.efaps.esjp.sales.payment.DocPaymentInfo_Base.AccountInfo;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 *
 */
@EFapsUUID("7f35941b-2517-4918-8dc9-9b13c4f0235a")
@EFapsApplication("eFapsApp-Sales")
public abstract class PaymentRetentionOut_Base
    extends AbstractPaymentOut
{

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = createDoc(_parameter);
        createPayment(_parameter, createdDoc);
        closeRetention(_parameter);
        executeAutomation(_parameter, createdDoc);
        final Return ret = new Return();
        final File file = createReport(_parameter, createdDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }

        return ret;
    }

    protected void closeRetention(final Parameter _parameter)
        throws EFapsException
    {
        final String[] createDocuments = _parameter
                        .getParameterValues(CITableSales.Sales_PaymentRetentionOutPaymentTable.createDocument.name);
        for (final String createDocument : createDocuments) {
            final Instance inst = Instance.get(createDocument);
            if (inst.isValid()) {
                final DocTaxInfo docTaxInfo = AbstractDocumentTax.getDocTaxInfo(_parameter, inst);
                if (docTaxInfo.isValid() && docTaxInfo.isRetention()) {
                    final Update update = new Update(docTaxInfo.getTaxDocInstance(CISales.IncomingRetention.getType()));
                    update.add(CISales.IncomingRetention.StatusAbstract,
                                    Status.find(CISales.IncomingRetentionStatus.Paid));
                    update.executeWithoutAccessCheck();
                }
            }
        }
    }

    protected List<Instance> getDocInstances(final Parameter _parameter,
                                             final String[] _oids)
        throws EFapsException
    {
        final List<Instance> ret = new ArrayList<>();
        final List<Instance> retIns = new ArrayList<>();
        for (final String oid : _oids) {
            retIns.add(Instance.get(oid));
        }
        final QueryBuilder queryBldr = new QueryBuilder(CISales.IncomingDocumentTax2Document);
        queryBldr.addWhereAttrEqValue(CISales.IncomingDocumentTax2Document.FromAbstractLink, retIns.toArray());
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder sel = SelectBuilder.get().linkto(CISales.IncomingDocumentTax2Document.ToAbstractLink)
                        .instance();
        multi.addSelect(sel);
        multi.execute();
        while (multi.next()) {
            ret.add(multi.<Instance>getSelect(sel));
        }
        return ret;
    }

    protected StringBuilder getJavaScript4Positions(final Parameter _parameter,
                                                    final AccountInfo _accountInfo,
                                                    final List<Instance> _docInstances)
        throws EFapsException
    {
        final StringBuilder js = new StringBuilder();
        final List<Map<String, Object>> values = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (final Instance docInst : _docInstances) {
            final Map<String, Object> map = new HashMap<>();
            values.add(map);
            final DocPaymentInfo docInfo = getNewDocPaymentInfo(_parameter, docInst);
            docInfo.setAccountInfo(_accountInfo);
            final DocTaxInfo docTaxInfo = AbstractDocumentTax.getDocTaxInfo(_parameter, docInst);
            final BigDecimal amount4PayDoc = docTaxInfo.getTaxAmount();
            final BigDecimal paymentDiscount = BigDecimal.ZERO;
            final BigDecimal paymentAmountDesc = BigDecimal.ZERO;

            total = total.add(amount4PayDoc);
            map.put(CITableSales.Sales_PaymentRetentionOutPaymentTable.createDocument.name,
                            new String[] { docInst.getOid(), docInfo.getName() });

            map.put(CITableSales.Sales_PaymentRetentionOutPaymentTable.createDocumentDesc.name,
                            docInfo.getInfoField());
            map.put(CITableSales.Sales_PaymentRetentionOutPaymentTable.payment4Pay.name,
                            NumberFormatter.get().getTwoDigitsFormatter().format(amount4PayDoc));
            map.put(CITableSales.Sales_PaymentRetentionOutPaymentTable.paymentAmount.name,
                            NumberFormatter.get().getTwoDigitsFormatter().format(amount4PayDoc));
            map.put(CITableSales.Sales_PaymentRetentionOutPaymentTable.paymentAmountDesc.name,
                            NumberFormatter.get().getTwoDigitsFormatter().format(paymentAmountDesc));
            map.put(CITableSales.Sales_PaymentRetentionOutPaymentTable.paymentDiscount.name,
                            NumberFormatter.get().getTwoDigitsFormatter().format(paymentDiscount));
     //       map.put(CITableSales.Sales_PaymentRetentionOutPaymentTable.paymentRate.name), NumberFormatter
     //                     .get().getFormatter(0, 3).format(docInfo.getObject4Rate()));
     //       map.put(CITableSales.Sales_PaymentRetentionOutPaymentTable.paymentRate.name
     //                       + RateUI.INVERTEDSUFFIX), "" + docInfo.getCurrencyInst().isInvert());
        }

        js.append(getTableRemoveScript(_parameter, "paymentTable", false, false))
                            .append(getTableAddNewRowsScript(_parameter, "paymentTable", values,
                                            null, false, false, new HashSet<String>()));
        js.append(getSetFieldValue(0, "amount",  NumberFormatter.get().getTwoDigitsFormatter().format(total)))
                        .append(getSetFieldValue(0, "total4DiscountPay",
                                        NumberFormatter.get().getTwoDigitsFormatter().format(BigDecimal.ZERO)));
        return js;
    }

    @Override
    public Return getJavaScriptUIValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String accountStr = _parameter
                        .getParameterValue(CIFormSales.Sales_IncomingRetention4PaymentRetentionForm.account.name);
        final StringBuilder js = new StringBuilder();
        js.append(getSetDropDownScript(_parameter, CIFormSales.Sales_PaymentRetentionOutForm.account.name, accountStr));

        final String[] oids = (String[]) Context.getThreadContext().getSessionAttribute(
                        CIFormSales.Sales_IncomingRetention4PaymentRetentionForm.storeOIDs.name);
        if (oids != null) {
            js.append(getJavaScript4Positions(_parameter,
                            new AccountInfo(Instance.get(CISales.AccountCashDesk.getType(), accountStr)),
                            getDocInstances(_parameter, oids)));
        }
        ret.put(ReturnValues.SNIPLETT, InterfaceUtils.wrappInScriptTag(_parameter, js, true, 1500));
        return ret;
    }


}
