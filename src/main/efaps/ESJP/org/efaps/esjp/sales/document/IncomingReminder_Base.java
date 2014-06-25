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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;

import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.field.Field;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.SalesSettings;
import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("a126d27b-105b-4751-bc88-22d320f5b177")
@EFapsRevision("$Rev$")
public abstract class IncomingReminder_Base
    extends AbstractDocumentSum
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
        connect2DocumentType(_parameter, createdDoc);
        connect2Derived(_parameter, createdDoc);
        connect2Object(_parameter, createdDoc);
        return new Return();
    }

    /**
     * Edit.
     *
     * @param _parameter Parameter from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final EditedDoc editDoc = editDoc(_parameter);
        updatePositions(_parameter, editDoc);
        return new Return();
    }

    @Override
    protected void add2DocCreate(final Parameter _parameter,
                                 final Insert _insert,
                                 final CreatedDoc _createdDoc)
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
    public Return autoComplete4IncomingInvoice(final Parameter _parameter)
        throws EFapsException
    {
        return new IncomingReminder()
        {

            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final Map<Integer, String> types = analyseProperty(_parameter, "Type");
                if (!types.isEmpty()) {
                    for (final Entry<Integer, String> entry : types.entrySet()) {
                        final Type type = Type.get(entry.getValue());
                        if (type != null) {
                            _queryBldr.addType(type);
                        }
                    }
                }
            };
        }.autoComplete4Doc(_parameter, CISales.IncomingInvoice.uuid, (Status[]) null);
    }

    @Override
    public Type getType4SysConf(final Parameter _parameter)
        throws EFapsException
    {
        return CISales.IncomingReminder.getType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Return updateFields4IncomingInvoice(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret;
        if (containsProperty(_parameter, "UpdateContactField")
                        && "true".equalsIgnoreCase(getProperty(_parameter, "UpdateContactField"))) {
            final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
            final Map<String, Object> map = new HashMap<String, Object>();

            final Field field = (Field) _parameter.get(ParameterValues.UIOBJECT);
            final Instance docInst = Instance.get(_parameter.getParameterValue(field.getName()));

            final SelectBuilder selContactInst = new SelectBuilder()
                                    .linkto(CISales.DocumentAbstract.Contact).instance();
            final SelectBuilder selContactName = new SelectBuilder()
                                    .linkto(CISales.DocumentAbstract.Contact).attribute(CISales.DocumentAbstract.Name);

            final PrintQuery print = new PrintQuery(docInst);
            print.addSelect(selContactInst, selContactName);
            print.execute();

            final Instance contactInst = print.<Instance>getSelect(selContactInst);
            final String contactName = print.<String>getSelect(selContactName);

            if (contactInst != null && contactInst.isValid()) {
                map.put("contact", new String[]{ contactInst.getOid(), contactName });
                map.put("contactData", getFieldValue4Contact(contactInst));
            }

            list.add(map);
            ret = new Return().put(ReturnValues.VALUES, list);
        } else {
            ret = super.updateFields4IncomingInvoice(_parameter);
        }

        return ret;
    }
}
