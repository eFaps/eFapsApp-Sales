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
import org.efaps.admin.program.esjp.EFapsApplication;
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
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.document.AbstractDocument_Base;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("ba14f903-9522-4a10-b847-db50fdb360a3")
@EFapsApplication("eFapsApp-Sales")
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
        updateDetractions(_parameter, createdDoc);
        executeAutomation(_parameter, createdDoc);
        final Return ret = new Return();
        final File file = createReport(_parameter, createdDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }

        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return createMassive(final Parameter _parameter)
        throws EFapsException
    {
        final String[] detractionDocs = _parameter.getParameterValues("detractionDoc");
        final String[] paymentAmounts = _parameter.getParameterValues("paymentAmount");

        if (detractionDocs != null) {
            final BulkPaymentDetraction bulk = new BulkPaymentDetraction();
            final Parameter bulkParameter = ParameterUtil.clone(_parameter);
            ParameterUtil.setParameterValues(bulkParameter, "account4create", _parameter.getParameterValues("account"));
            final CreatedDoc bulkDoc = bulk.createDoc(bulkParameter);
            for (int i = 0; i < paymentAmounts.length; i++) {
                final Parameter parameter = ParameterUtil.clone(_parameter);
                ParameterUtil.setParameterValues(parameter, "amount", paymentAmounts[i]);
                ParameterUtil.setParameterValues(parameter, "paymentAmount", paymentAmounts[i]);
                ParameterUtil.setParameterValues(parameter, "createDocument", detractionDocs[i]);
                ParameterUtil.setParameterValues(parameter, "detractionDoc", detractionDocs[i]);
                addParameter2DocValues(parameter);
                final CreatedDoc createdDoc = createDoc(parameter);
                createPayment(parameter, createdDoc);
                updateDetractions(parameter, createdDoc);
                connectBulkPayment2PaymentDocumentOut(parameter, bulkDoc, createdDoc);
                executeAutomation(parameter, createdDoc);
            }
        }
        return new Return();
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _detractionDoc detraction document
     * @throws EFapsException on error
     */
    private void addParameter2DocValues(final Parameter _parameter)
        throws EFapsException
    {
        final Instance detractionDoc = Instance.get(_parameter.getParameterValue("detractionDoc"));
        if (detractionDoc.isValid()) {
            final SelectBuilder selContact = new SelectBuilder().linkto(CISales.IncomingDetraction.Contact).oid();
            final SelectBuilder selServiceId = new SelectBuilder().linkto(CISales.IncomingDetraction.ServiceType).id();

            final PrintQuery print = new PrintQuery(detractionDoc);
            print.addSelect(selContact, selServiceId);
            print.execute();

            ParameterUtil.setParameterValues(_parameter, "contact", print.<String>getSelect(selContact));
            ParameterUtil.setParameterValues(_parameter, "serviceType", String.valueOf(print.<Long>getSelect(selServiceId)));
        }
    }

    protected void connectBulkPayment2PaymentDocumentOut(final Parameter _parameter,
                                                         final CreatedDoc _bulkDoc,
                                                         final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final String opTypeId = _parameter
                        .getParameterValue(CIFormSales.Sales_PaymentDetractionOut4MassiveForm.operationType.name);
        final String serTypeId = _parameter.getParameterValue("serviceType");

        if (_createdDoc.getInstance().isValid()) {
            final Insert insert = new Insert(CISales.BulkPaymentDetraction2PaymentDocument);
            insert.add(CISales.BulkPaymentDetraction2PaymentDocument.FromLink, _bulkDoc.getInstance());
            insert.add(CISales.BulkPaymentDetraction2PaymentDocument.ToLink, _createdDoc.getInstance());
            insert.add(CISales.BulkPaymentDetraction2PaymentDocument.OperationType, opTypeId);
            insert.add(CISales.BulkPaymentDetraction2PaymentDocument.ServiceType, serTypeId);
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
        final List<Instance> instances = new ArrayList<>();
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

        final Map<Instance, Map<String, Object>> valuesTmp = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        while (multi.next()) {
            final DocPaymentInfo docInfo = getNewDocPaymentInfo(_parameter, multi.getCurrentInstance());
            final BigDecimal crossTotal = multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.CrossTotal);
            final Instance docInstance = multi.<Instance>getSelect(selDocInstance);
            final String docName = multi.<String>getAttribute(CISales.DocumentSumAbstract.Name);
            final String docContactName = multi.<String>getSelect(selDocContactName);
            final Map<String, Object> map;
            if (valuesTmp.containsKey(multi.getCurrentInstance())) {
                map = valuesTmp.get(valuesTmp);
            } else {
                map = new HashMap<>();
                valuesTmp.put(multi.getCurrentInstance(), map);

                map.put("createDocument", new String[] { docInstance.getOid(), docName });
                map.put("createDocumentContact", docContactName);
                map.put("createDocumentDesc", docInfo.getInfoField());
                map.put("payment4Pay",  NumberFormatter.get().getTwoDigitsFormatter()
                                .format(multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal)));
                map.put("paymentRate", NumberFormatter.get().getFormatter(0, 3)
                                .format(multi.<Object[]>getAttribute(CISales.DocumentSumAbstract.Rate)[1]));
             //   map.put("paymentRate" + RateUI.INVERTEDSUFFIX), docInfo.getCurrencyInstance().isInvert());
                map.put("paymentAmount",
                                NumberFormatter.get().getZeroDigitsFormatter().format(crossTotal));
                map.put("paymentDiscount",  NumberFormatter.get().getTwoDigitsFormatter().format(BigDecimal.ZERO));
                map.put("paymentAmountDesc", NumberFormatter.get().getTwoDigitsFormatter().format(BigDecimal.ZERO));
                map.put("detractionDoc", multi.getCurrentInstance().getOid());
                total = total.add(crossTotal.setScale(0, BigDecimal.ROUND_HALF_UP));
            }
        }

        js.append("<script type=\"text/javascript\">\n");
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            Context.getThreadContext().removeSessionAttribute(AbstractPaymentDocument_Base.CHANGE_AMOUNT);
        } else {
            Context.getThreadContext().setSessionAttribute(AbstractPaymentDocument_Base.CHANGE_AMOUNT, true);
        }
        if (!TargetMode.EDIT.equals(Context.getThreadContext()
                        .getSessionAttribute(AbstractDocument_Base.TARGETMODE_DOC_KEY))) {
            js.append(getTableRemoveScript(_parameter, "paymentTable", false, false))
                            .append(getTableAddNewRowsScript(_parameter, "paymentTable",  valuesTmp.values(),
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
    protected void add2QueryBldr4AutoComplete4CreateDocument(final Parameter _parameter,
                                                             final QueryBuilder _queryBldr)
        throws EFapsException
    {
        if (_queryBldr.getType().equals(CISales.IncomingInvoice.getType())) {
            final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.IncomingDetraction2IncomingInvoice);
            final AttributeQuery attrQuery =
                            attrQueryBldr.getAttributeQuery(CISales.IncomingDetraction2IncomingInvoice.ToLink);

            _queryBldr.addWhereAttrInQuery(CISales.DocumentAbstract.ID, attrQuery);
        } else {
            super.add2QueryBldr4AutoComplete4CreateDocument(_parameter, _queryBldr);
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
        final List<Map<String, String>> list = new ArrayList<>();
        final Map<String, String> map = new HashMap<>();
        final int selected = getSelectedRow(_parameter);
        final Instance docInstance = Instance.get(_parameter.getParameterValues("createDocument")[selected]);
        final Instance accInstance = Instance.get(CISales.AccountCashDesk.getType(),
                        _parameter.getParameterValue("account"));
        if (docInstance.isValid() && accInstance.isValid()) {
            final DocPaymentInfo docInfo = getNewDocPaymentInfo(_parameter, docInstance);
            docInfo.setAccountInst(accInstance);

            final BigDecimal total4Doc = docInfo.getCrossTotal();
            final BigDecimal payments4Doc = docInfo.getPaid(false);
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
                map.put("total4DiscountPay",  NumberFormatter.get().getTwoDigitsFormatter()
                                .format(amount.subtract(totalPay4Position)));
            }
            list.add(map);
        }

        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

}
