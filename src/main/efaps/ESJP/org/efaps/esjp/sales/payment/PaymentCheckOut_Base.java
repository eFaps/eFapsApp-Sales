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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.Command;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.esjp.erp.CommonDocument_Base.CreatedDoc;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: Payment_Base.java 7671 2012-06-14 17:25:53Z
 *          jorge.cueva@moxter.net $
 */
@EFapsUUID("541288ca-b89a-4b1c-97ad-f01eba31ccaa")
@EFapsRevision("$Rev$")
public abstract class PaymentCheckOut_Base
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
        final Return ret = createReportDoc(_parameter, createdDoc);
        return ret;
    }

    @Override
    protected void add2DocCreate(final Parameter _parameter,
                                 final Insert _insert,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final String dueDate = _parameter.getParameterValue("dueDate");
        if (dueDate != null) {
            _insert.add("DueDate", dueDate);
            _createdDoc.getValues().put("DueDate", dueDate);
        }
    }

    public Return getPayment2CheckOutPositionInstances(final Parameter _parameter)
        throws EFapsException
    {

        final Return ret = new Return();
        final Map<?, ?> properties = (HashMap<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String typeStr = (String) properties.get("Types");
        Type type;
        boolean band = false;
        final String cmd = (String) properties.get("AllCheckDifered");

        final List<Instance> instances = new ArrayList<Instance>();
        if (typeStr != null) {
            type = Type.get(typeStr);
            QueryBuilder _queryBldr = new QueryBuilder(CISales.Payment);
            QueryBuilder qlb = new QueryBuilder(type);
            final AttributeQuery attrQuery = qlb.getAttributeQuery("ID");
            _queryBldr.addWhereAttrInQuery(CISales.Payment.TargetDocument, attrQuery);

            MultiPrintQuery multi = _queryBldr.getPrint();
            final SelectBuilder seldate = new SelectBuilder().linkto(CISales.Payment.TargetDocument).attribute(
                            CISales.PaymentDocumentAbstract.Date);
            final SelectBuilder selduedate = new SelectBuilder().linkto(CISales.Payment.TargetDocument).attribute(
                            CISales.PaymentDocumentAbstract.DueDate);
            multi.addSelect(seldate, selduedate);
            multi.execute();
            while (multi.next()) {
                Instance inst = multi.getCurrentInstance();
                final DateTime date = multi.<DateTime>getSelect(seldate);
                final DateTime dueDate = multi.<DateTime>getSelect(selduedate);
                if (cmd != null
                                && "AllDifered".equalsIgnoreCase(cmd)) {
                    band = true;
                } else {
                    QueryBuilder qlb2 = new QueryBuilder(CISales.PayableDocument2Document);
                    qlb2.addWhereAttrEqValue("PayDocLink", multi.getCurrentInstance().getId());
                    MultiPrintQuery multi2 = qlb2.getPrint();
                    final SelectBuilder selDerivaded = new SelectBuilder().linkto(
                                    CISales.PayableDocument2Document.ToLink).attribute(CISales.DocumentAbstract.Name);
                    multi2.addSelect(selDerivaded);
                    multi2.execute();
                    if (multi2.next()) {
                        band = true;
                    } else {
                        band = false;
                    }
                }
                if (!(date.compareTo(dueDate) == 0) && band) {
                    instances.add(inst);
                }
            }

        }
        ret.put(ReturnValues.VALUES, instances);
        return ret;

    }




}
