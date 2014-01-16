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

package org.efaps.esjp.sales.document;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Checkin;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.StandartReport;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("a66a61fd-487a-4764-9e72-f65050c1d39e")
@EFapsRevision("$Rev$")
public abstract class CreditNote_Base
    extends DocumentSum
{

    /**
     * Method for create a new Credit Note.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        Return ret = new Return();
        final CreatedDoc createdDoc = createDoc(_parameter);
        createPositions(_parameter, createdDoc);
        connect2Derived(_parameter, createdDoc);

        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        if (properties.containsKey("JasperReport")) {
            final StandartReport report = new StandartReport();
            _parameter.put(ParameterValues.INSTANCE, createdDoc.getInstance());

            final String fileName = DBProperties.getProperty("Sales_CreditNote.Label", "es") + "_"
                            + createdDoc.getValues().get(CISales.DocumentStockAbstract.Name.name);
            report.setFileName(fileName);
            ret = report.execute(_parameter);
            ret.put(ReturnValues.TRUE, true);

            try {
                final File file = (File) ret.get(ReturnValues.VALUES);
                final InputStream input = new FileInputStream(file);
                final Checkin checkin = new Checkin(createdDoc.getInstance());
                checkin.execute(fileName + "." + properties.get("Mime"), input, ((Long) file.length()).intValue());
            } catch (final FileNotFoundException e) {
                throw new EFapsException(Invoice.class, "create.FileNotFoundException", e);
            }
        }
        return ret;
    }
}
