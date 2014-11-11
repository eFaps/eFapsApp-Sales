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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.ci.CITableSales;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.document.AbstractDocumentTax;
import org.efaps.esjp.sales.document.AbstractDocumentTax_Base.DocTaxInfo;
import org.efaps.esjp.sales.document.AbstractDocument_Base.KeyDef;
import org.efaps.esjp.sales.document.AbstractDocument_Base.KeyDefStr;
import org.efaps.esjp.sales.payment.DocPaymentInfo_Base.AccountInfo;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("7f35941b-2517-4918-8dc9-9b13c4f0235a")
@EFapsRevision("$Rev$")
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
        final Return ret = createReportDoc(_parameter, createdDoc);
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
        final List<Map<KeyDef, Object>> values = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (final Instance docInst : _docInstances) {
            final Map<KeyDef, Object> map = new HashMap<>();
            values.add(map);
            final DocPaymentInfo docInfo = getNewDocPaymentInfo(_parameter, docInst);
            docInfo.setAccountInfo(_accountInfo);
            final DocTaxInfo docTaxInfo = AbstractDocumentTax.getDocTaxInfo(_parameter, docInst);
            final BigDecimal amount4PayDoc = docTaxInfo.getTaxAmount();
            final BigDecimal paymentDiscount = BigDecimal.ZERO;
            final BigDecimal paymentAmountDesc = BigDecimal.ZERO;

            total = total.add(amount4PayDoc);
            map.put(new KeyDefStr(CITableSales.Sales_PaymentRetentionOutPaymentTable.createDocument.name),
                            new String[] { docInst.getOid(), docInfo.getName() });

            map.put(new KeyDefStr(CITableSales.Sales_PaymentRetentionOutPaymentTable.createDocumentDesc.name),
                            docInfo.getInfoField());
            map.put(new KeyDefStr(CITableSales.Sales_PaymentRetentionOutPaymentTable.payment4Pay.name),
                            NumberFormatter.get().getTwoDigitsFormatter().format(amount4PayDoc));
            map.put(new KeyDefStr(CITableSales.Sales_PaymentRetentionOutPaymentTable.paymentAmount.name),
                            NumberFormatter.get().getTwoDigitsFormatter().format(amount4PayDoc));
            map.put(new KeyDefStr(CITableSales.Sales_PaymentRetentionOutPaymentTable.paymentAmountDesc.name),
                            NumberFormatter.get().getTwoDigitsFormatter().format(paymentAmountDesc));
            map.put(new KeyDefStr(CITableSales.Sales_PaymentRetentionOutPaymentTable.paymentDiscount.name),
                            NumberFormatter.get().getTwoDigitsFormatter().format(paymentDiscount));
     //       map.put(new KeyDefStr(CITableSales.Sales_PaymentRetentionOutPaymentTable.paymentRate.name), NumberFormatter
     //                     .get().getFormatter(0, 3).format(docInfo.getObject4Rate()));
     //       map.put(new KeyDefStr(CITableSales.Sales_PaymentRetentionOutPaymentTable.paymentRate.name
     //                       + RateUI.INVERTEDSUFFIX), "" + docInfo.getCurrencyInst().isInvert());
        }

        final List<Map<String, Object>> strValues = convertMap4Script(_parameter, values);
        js.append(getTableRemoveScript(_parameter, "paymentTable", false, false))
                            .append(getTableAddNewRowsScript(_parameter, "paymentTable", strValues,
                                            null, false, false, new HashSet<String>()));
        js.append(getSetFieldValue(0, "amount",  NumberFormatter.get().getTwoDigitsFormatter().format(total)))
                        .append(getSetFieldValue(0, "total4DiscountPay",
                                        NumberFormatter.get().getTwoDigitsFormatter().format(BigDecimal.ZERO)));
        return js;
    }

}
