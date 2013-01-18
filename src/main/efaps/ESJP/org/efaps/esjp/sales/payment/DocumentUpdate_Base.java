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

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIERP;
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
        return Status.find(statusType.getUUID(), "Paid");
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
        final DocWithPayment doc = new DocWithPayment(_docInst);
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
