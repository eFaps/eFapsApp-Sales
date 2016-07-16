/*
 * Copyright 2003 - 2016 The eFaps Team
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

package org.efaps.esjp.sales.document;

import java.io.File;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("f3252401-2007-4ff3-997d-5e5d1a7ab863")
@EFapsApplication("eFapsApp-Sales")
public abstract class UsageReport_Base
    extends AbstractProductDocument
{
    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final CreatedDoc createdDoc = createDoc(_parameter);
        createPositions(_parameter, createdDoc);
        connect2ProductDocumentType(_parameter, createdDoc);
        connect2Object(_parameter, createdDoc);

        final File file = createReport(_parameter, createdDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    @Override
    protected void connect2ProductDocumentType(final Parameter _parameter,
                                               final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final Instance instDocType = Instance.get(_parameter.getParameterValue("productDocumentType"));
        if (instDocType.isValid() && _createdDoc.getInstance().isValid()) {
            super.connect2ProductDocumentType(_parameter, _createdDoc);
        } else {
            final Instance productDocType = Sales.USAGEREPORT_DEFAULTPRODUCTDOCUMENTTYPE.get();
            if (productDocType != null && productDocType.isValid()) {
                insert2DocumentTypeAbstract(CISales.Document2ProductDocumentType, _createdDoc, productDocType);
            }
        }
    }

    /**
     * Trigger to insert a position of the usage report in the inventory.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return usageReportPositionInsertTrigger(final Parameter _parameter)
        throws EFapsException
    {
        createTransaction4PositionTrigger(_parameter, CIProducts.TransactionOutbound.getType(),
                        evaluateStorage4PositionTrigger(_parameter));
        return new Return();
    }

    @Override
    public CIType getCIType()
        throws EFapsException
    {
        return CISales.UsageReport;
    }
}
