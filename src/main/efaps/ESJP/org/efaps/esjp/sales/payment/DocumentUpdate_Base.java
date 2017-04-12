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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
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
import org.efaps.admin.program.esjp.EFapsApplication;
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
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.esjp.sales.document.AbstractDocument_Base;
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
            final Status targetStatus = getTargetStatus(_parameter, instance);
            if (targetStatus != null && checkStatus(_parameter, instance)
                            && validateDocumentCriterias(_parameter, instance)) {
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
}
