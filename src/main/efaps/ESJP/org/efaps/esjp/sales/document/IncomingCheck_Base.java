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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.esjp.sales.payment.DocPaymentInfo;
import org.efaps.util.EFapsException;

/**
 * The Class IncomingCheck_Base.
 *
 * @author The eFaps Team
 */
@EFapsUUID("8ac285dd-5fbe-417d-8097-7026204b0e7e")
@EFapsApplication("eFapsApp-Sales")
public abstract class IncomingCheck_Base
    extends AbstractSumOnlyDocument
{

    /**
     * Method for create a new IncomingCredit.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final CreatedDoc createdDoc = createDoc(_parameter);
        connect2Object(_parameter, createdDoc);
        final File file = createReport(_parameter, createdDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        ret.put(ReturnValues.INSTANCE, createdDoc.getInstance());
        return new Return();
    }

    /**
     * Method for create a new IncomingCredit.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final EditedDoc editedDoc = editDoc(_parameter);
        updateConnection2Object(_parameter, editedDoc);
        final File file = createReport(_parameter, editedDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    /**
     * Update fields4 create document.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return updateFields4CreateDocument(final Parameter _parameter)
        throws EFapsException
    {
        final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        final int selected = getSelectedRow(_parameter);
        final Instance docInstance = Instance.get(_parameter.getParameterValues("createDocument")[selected]);
        final DocPaymentInfo info = new DocPaymentInfo(docInstance).setParameter(_parameter);
        final Map<String, Object> map = new HashMap<>();
        map.put("createDocumentDesc", info.getInfoField());
        list.add(map);
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }
}
