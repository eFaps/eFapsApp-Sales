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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.efaps.admin.datamodel.Attribute;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.document.AbstractDocument_Base;
import org.efaps.esjp.sales.document.AbstractDocument_Base.KeyDef;
import org.efaps.esjp.sales.document.AbstractDocument_Base.KeyDefStr;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: PaymentExchange_Base.java 8156 2012-11-05 15:32:12Z
 *          jan@moxter.net $
 */
@EFapsUUID("ba14f903-9522-4a10-b847-db50fdb360a3")
@EFapsRevision("$Rev$")
public abstract class PaymentDetractionOut_Base
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
        connectBulkPayment2PaymentDocumentOut(_parameter, createdDoc);
        updateDetractions(_parameter, createdDoc);
        executeAutomation(_parameter, createdDoc);
        final Return ret = createReportDoc(_parameter, createdDoc);
        return ret;
    }

    protected void connectBulkPayment2PaymentDocumentOut(final Parameter _parameter,
                                                         final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final String opTypeId = _parameter
                        .getParameterValue(CIFormSales.Sales_PaymentDetractionOutForm.operationType.name);
        final String servTypeId = _parameter
                        .getParameterValue(CIFormSales.Sales_PaymentDetractionOutForm.serviceType.name);
        final String bulkPayId = _parameter
                        .getParameterValue(CIFormSales.Sales_PaymentDetractionOutForm.bulkPaymentDoc.name);

        if (_createdDoc.getInstance().isValid()
                        && bulkPayId != null && !bulkPayId.isEmpty()) {
            final Insert insert = new Insert(CISales.BulkPayment2PaymentDocument);
            insert.add(CISales.BulkPayment2PaymentDocument.FromLink, bulkPayId);
            insert.add(CISales.BulkPayment2PaymentDocument.ToLink, _createdDoc.getInstance());
            insert.add(CISales.BulkPayment2PaymentDocument.OperationType, opTypeId);
            insert.add(CISales.BulkPayment2PaymentDocument.ServiceType, servTypeId);
            insert.execute();
        }
    }

    protected void updateDetractions(final Parameter _parameter,
                                     final CreatedDoc _createdDoc)
        throws EFapsException
    {
        if (!_createdDoc.getPositions().isEmpty()) {
            final String[] detractionDocs = _parameter.getParameterValues("detractionDoc");
            if (detractionDocs != null && detractionDocs.length > 0) {
                for (final String detractionDoc : detractionDocs) {
                    final Instance detractionInst = Instance.get(detractionDoc);
                    if (detractionInst.isValid()
                                    && CISales.IncomingDetraction.getType().equals(detractionInst.getType())) {
                        final Update update = new Update(detractionInst);
                        update.add(CISales.IncomingDetraction.Status,
                                        Status.find(CISales.IncomingDetractionStatus.Paid));
                        update.executeWithoutAccessCheck();
                    }
                }
            }
        }
    }

    public Return getJavaScript4SelectableRowsValues(final Parameter _parameter)
        throws EFapsException
    {
        final List<Instance> instances = new ArrayList<Instance>();
        if (_parameter.get(ParameterValues.INSTANCE) == null) {
            evaluateStatusDocs(_parameter, instances);
        }
        final Return ret = new Return();
        if (!instances.isEmpty()) {
            ret.put(ReturnValues.SNIPLETT, getJavaScript4Positions(_parameter, instances).toString());
        }
        return ret;
    }

    protected void evaluateStatusDocs(final Parameter _parameter,
                                      final List<Instance> _instances)
        throws EFapsException
    {
        final String[] oids = _parameter.getParameterValues("selectedRow");
        if (oids != null && oids.length > 0) {
            final Map<Integer, String> status = analyseProperty(_parameter, "Status");
            for (final String oid : oids) {
                final Instance instance = Instance.get(oid);
                final Attribute attrStatus = instance.getType().getStatusAttribute();
                final String attrStatusName = attrStatus.getLink().getName();
                final PrintQuery print = new PrintQuery(instance);
                print.addAttribute(attrStatus);
                print.execute();

                for (final Entry<Integer, String> statu : status.entrySet()) {
                    final Status stat = Status.find(attrStatusName, statu.getValue());
                    if (print.<Long>getAttribute(attrStatus).equals(stat.getId())) {
                        _instances.add(instance);
                    }
                }
            }
        }
    }

    protected StringBuilder getJavaScript4Positions(final Parameter _parameter,
                                                    final List<Instance> _instances)
        throws EFapsException
    {
        final SelectBuilder selDoc = new SelectBuilder()
                        .linkfrom(CIERP.Document2DocumentAbstract, CIERP.Document2DocumentAbstract.FromAbstractLink)
                        .linkto(CIERP.Document2DocumentAbstract.ToAbstractLink);
        final SelectBuilder selDocInstance = new SelectBuilder(selDoc).instance();
        final SelectBuilder selDocContactName = new SelectBuilder(selDoc)
                        .linkto(CIERP.DocumentAbstract.Contact).attribute(CIContacts.Contact.Name);

        final StringBuilder js = new StringBuilder();

        final MultiPrintQuery multi = new MultiPrintQuery(_instances);
        multi.addAttribute(CISales.DocumentSumAbstract.Name,
                        CISales.DocumentSumAbstract.Rate,
                        CISales.DocumentSumAbstract.CurrencyId,
                        CISales.DocumentSumAbstract.RateCurrencyId,
                        CISales.DocumentSumAbstract.CrossTotal,
                        CISales.DocumentSumAbstract.RateCrossTotal);
        multi.addSelect(selDocInstance, selDocContactName);
        multi.execute();

        final Map<Instance, Map<KeyDef, Object>> valuesTmp = new LinkedHashMap<Instance, Map<KeyDef, Object>>();
        BigDecimal total = BigDecimal.ZERO;
        while (multi.next()) {
            final DocPaymentInfo docInfo = getNewDocPaymentInfo(_parameter, multi.getCurrentInstance());
            final BigDecimal crossTotal = multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.CrossTotal);
            final Instance docInstance = multi.<Instance>getSelect(selDocInstance);
            final String docName = multi.<String>getAttribute(CISales.DocumentSumAbstract.Name);
            final String docContactName = multi.<String>getSelect(selDocContactName);
            final Map<KeyDef, Object> map;
            if (valuesTmp.containsKey(multi.getCurrentInstance())) {
                map = valuesTmp.get(valuesTmp);
            } else {
                map = new HashMap<KeyDef, Object>();
                valuesTmp.put(multi.getCurrentInstance(), map);

                map.put(new KeyDefStr("createDocument"), new String[] { docInstance.getOid(), docName });
                map.put(new KeyDefStr("createDocumentContact"), docContactName);
                map.put(new KeyDefStr("createDocumentDesc"), docInfo.getInfoField());
                map.put(new KeyDefStr("payment4Pay"),  NumberFormatter.get().getTwoDigitsFormatter()
                                .format(multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal)));
                map.put(new KeyDefStr("paymentRate"), NumberFormatter.get().getFormatter(0, 3)
                                .format(multi.<Object[]>getAttribute(CISales.DocumentSumAbstract.Rate)[1]));
             //   map.put(new KeyDefStr("paymentRate" + RateUI.INVERTEDSUFFIX), docInfo.getCurrencyInstance().isInvert());
                map.put(new KeyDefStr("paymentAmount"),
                                NumberFormatter.get().getZeroDigitsFormatter().format(crossTotal));
                map.put(new KeyDefStr("paymentDiscount"),  NumberFormatter.get().getTwoDigitsFormatter().format(BigDecimal.ZERO));
                map.put(new KeyDefStr("paymentAmountDesc"),  NumberFormatter.get().getTwoDigitsFormatter().format(BigDecimal.ZERO));
                map.put(new KeyDefStr("detractionDoc"), multi.getCurrentInstance().getOid());
                total = total.add(crossTotal.setScale(0, BigDecimal.ROUND_HALF_UP));
            }
        }
        final Collection<Map<KeyDef, Object>> values = valuesTmp.values();
        final List<Map<String, Object>> strValues = convertMap4Script(_parameter, values);

        js.append("<script type=\"text/javascript\">\n");
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            Context.getThreadContext().removeSessionAttribute(AbstractPaymentDocument_Base.CHANGE_AMOUNT);
        } else {
            Context.getThreadContext().setSessionAttribute(AbstractPaymentDocument_Base.CHANGE_AMOUNT, true);
        }
        if (!TargetMode.EDIT.equals(Context.getThreadContext()
                        .getSessionAttribute(AbstractDocument_Base.TARGETMODE_DOC_KEY))) {
            js.append(getTableRemoveScript(_parameter, "paymentTable", false, false))
                            .append(getTableAddNewRowsScript(_parameter, "paymentTable", strValues,
                                            getOnCompleteScript(_parameter), false, false, new HashSet<String>()));
        }
        js.append(getSetFieldValue(0, "amount",  NumberFormatter.get().getTwoDigitsFormatter().format(total)))
            .append(getSetFieldValue(0, "total4DiscountPay",  NumberFormatter.get().getTwoDigitsFormatter().format(BigDecimal.ZERO)))
            .append("</script>\n");
        return js;
    }

    protected StringBuilder getOnCompleteScript(final Parameter _parameter)
        throws EFapsException
    {
        return new StringBuilder();
    }



    /**
     * {@inheritDoc}
     */
    @Override
    protected void add2QueryBldr4autoComplete4CreateDocument(final Parameter _parameter,
                                                             final QueryBuilder _queryBldr)
        throws EFapsException
    {
        if (_queryBldr.getType().equals(CISales.IncomingInvoice.getType())) {
            final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.IncomingDetraction2IncomingInvoice);
            final AttributeQuery attrQuery =
                            attrQueryBldr.getAttributeQuery(CISales.IncomingDetraction2IncomingInvoice.ToLink);

            _queryBldr.addWhereAttrInQuery(CISales.DocumentAbstract.ID, attrQuery);
        } else {
            super.add2QueryBldr4autoComplete4CreateDocument(_parameter, _queryBldr);
        }
    }

    public Return recalculateAmount(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final StringBuilder js = new StringBuilder();

        final String[] amount4Payments = _parameter.getParameterValues("paymentAmount");
        BigDecimal ret = BigDecimal.ZERO;
        if (amount4Payments != null && amount4Payments.length > 0) {
            for (final String amount4Pay : amount4Payments) {
                ret = ret.add(parseBigDecimal(amount4Pay));
            }
        }
        js.append(getSetFieldValue(0, "amount",   NumberFormatter.get().getTwoDigitsFormatter().format(ret)));
        retVal.put(ReturnValues.SNIPLETT, js.toString());
        return retVal;
    }

    @Override
    protected BigDecimal getRound4Amount(final BigDecimal _amount4PayDoc,
                                         final BigDecimal _rate)
    {
        return super.getRound4Amount(_amount4PayDoc, _rate).setScale(0, BigDecimal.ROUND_HALF_UP);
    }

    public Return updateFields4CreateDocumentMassive(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();
        final int selected = getSelectedRow(_parameter);
        final Instance docInstance = Instance.get(_parameter.getParameterValues("createDocument")[selected]);
        final Instance accInstance = Instance.get(CISales.AccountCashDesk.getType(),
                        _parameter.getParameterValue("account"));
        if (docInstance.isValid() && accInstance.isValid()) {
            final DocPaymentInfo docInfo = getNewDocPaymentInfo(_parameter, docInstance);
            docInfo.setAccountInst(accInstance);
            docInfo.setRateTarget(getRateObject(_parameter));

            final BigDecimal total4Doc = docInfo.getCrossTotal();
            final BigDecimal payments4Doc = docInfo.getPaid();
            final BigDecimal amount4PayDoc = total4Doc.subtract(payments4Doc);

            map.put("createDocument", docInfo.getInstance().getOid());
            map.put("detractionDoc", docInfo.getInstance().getOid());
            map.put("createDocumentContact", docInfo.getContactName());
            map.put("createDocumentDesc", docInfo.getInfoField());
            map.put("payment4Pay",  NumberFormatter.get().getTwoDigitsFormatter().format(amount4PayDoc));
            map.put("paymentAmount",  NumberFormatter.get().getTwoDigitsFormatter().format(amount4PayDoc));
            map.put("paymentAmountDesc",  NumberFormatter.get().getTwoDigitsFormatter().format(BigDecimal.ZERO));
            map.put("paymentDiscount",  NumberFormatter.get().getTwoDigitsFormatter().format(BigDecimal.ZERO));
         //   map.put("paymentRate", NumberFormatter.get().getFormatter(0, 3).format(docInfo.getObject4Rate()));
         //   map.put("paymentRate" + RateUI.INVERTEDSUFFIX, "" + docInfo.getCurrencyInst().isInvert());
            final BigDecimal update = parseBigDecimal(_parameter.getParameterValues("paymentAmount")[selected]);
            final BigDecimal totalPay4Position = getSum4Positions(_parameter, true).subtract(update).add(amount4PayDoc);
            if (Context.getThreadContext().getSessionAttribute(AbstractPaymentDocument_Base.CHANGE_AMOUNT) == null) {
                map.put("amount",  NumberFormatter.get().getTwoDigitsFormatter().format(totalPay4Position));
                map.put("total4DiscountPay",  NumberFormatter.get().getTwoDigitsFormatter().format(BigDecimal.ZERO));
            } else {
                final BigDecimal amount = parseBigDecimal(_parameter.getParameterValue("amount"));
                map.put("total4DiscountPay",  NumberFormatter.get().getTwoDigitsFormatter().format(amount.subtract(totalPay4Position)));
            }
            list.add(map);
        }

        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

}
