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
 * Revision:        $Rev: 8342 $
 * Last Changed:    $Date: 2012-12-11 09:42:17 -0500 (Tue, 11 Dec 2012) $
 * Last Changed By: $Author: jan@moxter.net $
 */


package org.efaps.esjp.sales.document;

import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uiform.Field;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.SalesSettings;
import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: CodeTemplates.xml 8342 2012-12-11 14:42:17Z jan@moxter.net $
 */
public abstract class IncomingReminder_Base
    extends DocumentSum
{

    public static final String REVISIONKEY = "org.efaps.esjp.sales.document.IncomingReminder.RevisionKey";

    /**
     * Method for create a new Incoming Credit Note.
     * 
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = createDoc(_parameter);
        createPositions(_parameter, createdDoc);
        return new Return();
    }

    @Override
    protected void add2DocCreate(Parameter _parameter,
                                 Insert _insert,
                                 CreatedDoc _createdDoc)
        throws EFapsException
    {
        final SystemConfiguration config = Sales.getSysConfig();
        final Properties props = config.getAttributeValueAsProperties(SalesSettings.INCOMINGREMINDERSEQUENCE);

        final NumberGenerator numgen = NumberGenerator.get(UUID.fromString(props.getProperty("UUID")));
        if (numgen != null) {
            final String revision = numgen.getNextVal();
            Context.getThreadContext().setSessionAttribute(IncomingReminder_Base.REVISIONKEY, revision);
            _insert.add(CISales.IncomingReminder.Revision, revision);
        }

    }

    @Override
    protected void connect2DocumentType(Parameter _parameter,
                                        Instance _instance)
        throws EFapsException
    {
        final Instance instDocType = Instance.get(_parameter
                        .getParameterValue(CIFormSales.Sales_IncomingReminderForm.documentType.name));
        if (instDocType.isValid() && _instance.isValid()) {
            final Insert insert = new Insert(CISales.Document2DocumentType);
            insert.add(CISales.Document2DocumentType.DocumentLink, _instance);
            insert.add(CISales.Document2DocumentType.DocumentTypeLink, instDocType);
            insert.execute();
        }
    }

    @Override
    public Return dropDown4DocumentType(Parameter _parameter)
        throws EFapsException
    {
        return new Field()
        {

            @Override
            protected void updatePositionList(final Parameter _parameter,
                                              final List<DropDownPosition> _values)
                throws EFapsException
            {
                Boolean hasSelect = false;
                for (final DropDownPosition val : _values) {
                    if (val.isSelected()) {
                        hasSelect = true;
                    }
                }
                if (!hasSelect) {
                    final Properties props = Sales.getSysConfig()
                                    .getAttributeValueAsProperties(SalesSettings.DEFAULTDOCTYPE4DOC);
                    if (props != null) {
                        final Instance defInst = Instance.get(props.getProperty(CISales.IncomingReminder.getType()
                                        .getUUID().toString()));
                        if (defInst.isValid()) {
                            for (final DropDownPosition val : _values) {
                                if (val.getValue().toString().equals(defInst.getOid())) {
                                    val.setSelected(true);
                                }
                            }
                        }
                    }
                }
            }

        }.dropDownFieldValue(_parameter);
    }
}
