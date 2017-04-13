/*
 * Copyright 2003 - 2015 The eFaps Team
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
 */

package org.efaps.esjp.sales.payment;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.collections4.CollectionUtils;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.CachedPrintQuery;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.common.properties.PropertiesUtil;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;

/**
 * Class is responsible to update a Document when a PaymentDocument is related
 * to it. e.g change the Status when a invoice was payed completely.
 *
 * @author The eFaps Team
 */
@EFapsUUID("46709cec-7b85-4630-9e2c-517db66ce2d0")
@EFapsApplication("eFapsApp-Sales")
public abstract class DocumentUpdate_Base
    extends AbstractCommon
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
        return updateDocument(_parameter, docInst);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInstance instance of the document
     * @return new Return
     * @throws EFapsException on error
     */
    public Return updateDocument(final Parameter _parameter,
                                 final Instance _docInstance)
        throws EFapsException
    {
        Instance docInst = _docInstance;
        if (InstanceUtils.isType(docInst, CISales.PayableDocument2Document)) {
            final PrintQuery print = new PrintQuery(docInst);
            final SelectBuilder selDocInst = SelectBuilder.get().linkto(CISales.PayableDocument2Document.FromLink)
                            .instance();
            print.addSelect(selDocInst);
            print.executeWithoutAccessCheck();
            docInst = print.getSelect(selDocInst);
        }

        for (final Instance instance : getInstances(_parameter, docInst)) {
            if (isApplyPaidRule(_parameter, _docInstance) && validateCriteria4Paid(_parameter, _docInstance)) {
                final Status targetStatus = getPaidTargetStatus(_parameter, instance);
                setStatus(_parameter, instance, targetStatus);
            } else if (isApplyUnpaidRule(_parameter, _docInstance) && validateCriteria4Unpaid(_parameter,
                            _docInstance)) {
                final Status targetStatus = getUnpaidTargetStatus(_parameter, instance);
                setStatus(_parameter, instance, targetStatus);
            }
        }
        return new Return();
    }

    /**
     * Provides the possiblity to Update also related Documents by adding them to the list.
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInstance instance of the document
     * @return new Return
     * @throws EFapsException on error
     */
    protected List<Instance> getInstances(final Parameter _parameter,
                                          final Instance _docInstance)
        throws EFapsException
    {
        final List<Instance> ret = new ArrayList<>();
        ret.add(_docInstance);

        if (_docInstance.getType().isKindOf(CISales.DocumentSumTaxAbstract)) {
            final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.IncomingDocumentTax2Document);
            attrQueryBldr.addWhereAttrEqValue(CISales.IncomingDocumentTax2Document.FromAbstractLink, _docInstance);
            final QueryBuilder queryBldr = new QueryBuilder(CISales.DocumentAbstract);
            queryBldr.addWhereAttrInQuery(CISales.DocumentAbstract.ID,
                            attrQueryBldr.getAttributeQuery(CISales.IncomingDocumentTax2Document.ToAbstractLink));
            ret.addAll(queryBldr.getQuery().executeWithoutAccessCheck());
        }
        final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Document2Document4Swap);
        attrQueryBldr.addWhereAttrEqValue(CISales.Document2Document4Swap.ToLink, _docInstance);
        final QueryBuilder queryBldr = new QueryBuilder(CISales.DocumentSumAbstract);
        queryBldr.addWhereAttrInQuery(CISales.DocumentSumAbstract.ID,
                        attrQueryBldr.getAttributeQuery(CISales.Document2Document4Swap.FromLink));
        ret.addAll(queryBldr.getQuery().execute());
        return ret;
    }

    /**
     * Checks if is paid rule by searching for a property.
     *
     * TYPE.Paid.Origin.Status01=Draft
     * TYPE.Paid.Origin.Status02=Open
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInstance the doc instance
     * @return true, if is paid rule
     * @throws EFapsException on error
     */
    protected boolean isApplyPaidRule(final Parameter _parameter,
                                      final Instance _docInstance)
        throws EFapsException
    {
        return isApplyRule(_parameter, _docInstance, ".Paid.Origin");
    }

    /**
     * Checks if is paid rule by searching for a property.
     *
     * TYPE.Unpaid.Origin.Status01=Paid
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInstance the doc instance
     * @return true, if is paid rule
     * @throws EFapsException on error
     */
    protected boolean isApplyUnpaidRule(final Parameter _parameter,
                                      final Instance _docInstance)
        throws EFapsException
    {
        return isApplyRule(_parameter, _docInstance, ".Unpaid.Origin");
    }

    /**
     * Apply rule.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInstance the doc instance
     * @param _key the key
     * @return true, if successful
     * @throws EFapsException on error
     */
    protected boolean isApplyRule(final Parameter _parameter,
                                  final Instance _docInstance,
                                  final String _key)
        throws EFapsException
    {
        boolean ret = false;
        final Properties properties = PropertiesUtil.getProperties4Prefix(Sales.PAYMENT_RULES.get(),
                        _docInstance.getType().getName() + _key);
        final List<Status> statuses = getStatusListFromProperties(ParameterUtil.clone(_parameter,
                        Parameter.ParameterValues.INSTANCE, _docInstance), properties);
        if (CollectionUtils.isNotEmpty(statuses)) {
            final PrintQuery print = CachedPrintQuery.get4Request(_docInstance);
            print.addAttribute(_docInstance.getType().getStatusAttribute());
            print.executeWithoutAccessCheck();
            final Status status = Status.get(print.<Long>getAttribute(_docInstance.getType().getStatusAttribute()));
            ret = statuses.contains(status);
        }
        return ret;
    }

    /**
     * Validate criterias for paid.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInstance the doc instance
     * @return true, if successful
     * @throws EFapsException on error
     */
    protected boolean validateCriteria4Paid(final Parameter _parameter,
                                            final Instance _docInstance)
        throws EFapsException
    {
        final DocPaymentInfo doc = new DocPaymentInfo(_docInstance);
        return doc.isPaid();
    }

    /**
     * Validate criterias for unpaid.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInstance the doc instance
     * @return true, if successful
     * @throws EFapsException on error
     */
    protected boolean validateCriteria4Unpaid(final Parameter _parameter,
                                              final Instance _docInstance)
        throws EFapsException
    {
        final DocPaymentInfo doc = new DocPaymentInfo(_docInstance);
        return !doc.isPaid();
    }

    /**
     * Gets the paid status.
     * Sales_Invoice.Paid.TargetStatus=Open
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst the doc inst
     * @return the paid status
     * @throws EFapsException on error
     */
    protected Status getPaidTargetStatus(final Parameter _parameter,
                                         final Instance _docInst)
        throws EFapsException
    {
        return getTargetStatus(_parameter, _docInst, "Paid");
    }

    /**
     * Gets the paid status.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst the doc inst
     * @return the paid status
     * @throws EFapsException on error
     */
    protected Status getUnpaidTargetStatus(final Parameter _parameter,
                                           final Instance _docInst)
        throws EFapsException
    {
        return getTargetStatus(_parameter, _docInst, "Unpaid");
    }

    /**
     * Get the Status the Document will be set to if the criteria are met.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInstance the doc instance
     * @param _key the key
     * @return Status if found, else null
     * @throws EFapsException on error
     */
    protected Status getTargetStatus(final Parameter _parameter,
                                     final Instance _docInstance,
                                     final String _key)
        throws EFapsException
    {
        final Properties properties = PropertiesUtil.getProperties4Prefix(Sales.PAYMENT_RULES.get(),
                        _docInstance.getType().getName() + "." +_key);
        return Status.find(_docInstance.getType().getStatusAttribute().getLink().getName(),
                        properties.getProperty("TargetStatus"));
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
}
