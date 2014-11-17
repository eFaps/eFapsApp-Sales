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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: Payment_Base.java 7671 2012-06-14 17:25:53Z
 *          jorge.cueva@moxter.net $
 */
@EFapsUUID("39feb877-6310-4170-816d-173f89347e3d")
@EFapsRevision("$Rev$")
public abstract class PaymentCheck_Base
    extends AbstractPaymentIn
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
        executeAutomation(_parameter, createdDoc);
        final Return ret = new Return();
        final File file = createReport(_parameter, createdDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    @Override
    protected void add2DocCreate(final Parameter _parameter,
                                 final Insert _insert,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        super.add2DocCreate(_parameter, _insert, _createdDoc);

        final String issued = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.PaymentCheck.Issued.name));
        if (issued != null) {
            _insert.add(CISales.PaymentCheck.Issued, issued);
            _createdDoc.getValues().put(getFieldName4Attribute(_parameter, CISales.PaymentCheck.Issued.name),
                            issued);
        }
    }

    public Return createWithOutDoc(final Parameter _parameter)
        throws EFapsException
    {
        createDoc(_parameter);
        return new Return();
    }

    public Return addDoc2PaymentDoc(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = getCreateDoc2addPayment(_parameter);
        createPayment(_parameter, createdDoc);
        final Return ret = new Return();
        final File file = createReport(_parameter, createdDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    protected CreatedDoc getCreateDoc2addPayment(final Parameter _parameter)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_parameter.getInstance());
        print.addAttribute(CISales.PaymentCheck.Name,
                        CISales.PaymentCheck.RateCurrencyLink,
                        CISales.PaymentCheck.Date);
        print.execute();
        final String name = print.<String>getAttribute(CISales.PaymentCheck.Name);
        final Long curId = print.<Long>getAttribute(CISales.PaymentCheck.RateCurrencyLink);
        final DateTime date = print.<DateTime>getAttribute(CISales.PaymentCheck.Date);

        final CreatedDoc createDoc = new CreatedDoc(_parameter.getInstance());
        createDoc.getValues().put(getFieldName4Attribute(_parameter, CISales.PaymentCheck.Name.name), name);
        createDoc.getValues()
                        .put(getFieldName4Attribute(_parameter, CISales.PaymentCheck.RateCurrencyLink.name), curId);
        createDoc.getValues().put(getFieldName4Attribute(_parameter, CISales.PaymentCheck.Date.name), date);

        return createDoc;
    }

    public Return dropDownFieldValue4Account(final Parameter _parameter)
        throws EFapsException
    {
        return new org.efaps.esjp.common.uiform.Field()
        {

            @Override
            protected void add2QueryBuilder4List(final Parameter _parameter,
                                                 final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final PrintQuery print = new PrintQuery(_parameter.getInstance());
                print.addAttribute(CISales.PaymentCheck.RateCurrencyLink);
                print.execute();
                final Long curId = print.<Long>getAttribute(CISales.PaymentCheck.RateCurrencyLink);

                _queryBldr.addWhereAttrEqValue(CISales.AccountCashDesk.CurrencyLink, curId);
            }
        }.dropDownFieldValue(_parameter);
    }

    public Return returnDiffered(final Parameter _parameter)
        throws EFapsException
    {

        final Return ret = new Return();
        final Map<?, ?> properties = (HashMap<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String typeStr = (String) properties.get("Types");
        Type type;
        final List<Instance> instances = new ArrayList<Instance>();
        if (typeStr != null) {
            type = Type.get(typeStr);
            final QueryBuilder queryBldr = new QueryBuilder(type);
            final MultiPrintQuery multi = queryBldr.getPrint();

            multi.addAttribute(CISales.PaymentDocumentAbstract.Date, CISales.PaymentDocumentAbstract.DueDate);
            multi.execute();
            while (multi.next()) {
                final Instance inst = multi.getCurrentInstance();
                final DateTime date = multi.<DateTime>getAttribute(CISales.PaymentDocumentAbstract.Date);
                final DateTime dueDate = multi.<DateTime>getAttribute(CISales.PaymentDocumentAbstract.DueDate);
                if (!(date.compareTo(dueDate) == 0)) {
                    instances.add(inst);
                }
            }

        }
        ret.put(ReturnValues.VALUES, instances);
        return ret;
    }
}
