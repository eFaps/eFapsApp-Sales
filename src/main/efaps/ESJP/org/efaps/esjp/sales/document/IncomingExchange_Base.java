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


package org.efaps.esjp.sales.document;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Insert;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("f292a189-7896-4e1d-9d89-d07b978a8b8f")
@EFapsApplication("eFapsApp-Sales")
public abstract class IncomingExchange_Base
    extends AbstractSumOnlyDocument
{
    /**
     * Method for create a new Quotation.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        createDoc(_parameter);
        return new Return();
    }

    @Override
    protected void add2DocCreate(final Parameter _parameter,
                                 final Insert _insert,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final String onlynumber = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.IncomingExchange.OnlyNumber.name));
        if (onlynumber != null) {
            _insert.add(CISales.IncomingExchange.OnlyNumber, onlynumber);
            _createdDoc.getValues().put(CISales.IncomingExchange.OnlyNumber.name, onlynumber);
        }

        final String entityFinancial = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.IncomingExchange.EntityFinancial.name));
        if (entityFinancial != null) {
            _insert.add(CISales.IncomingExchange.EntityFinancial, entityFinancial);
            _createdDoc.getValues().put(CISales.IncomingExchange.EntityFinancial.name, entityFinancial);
        }
    }

    /**
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return updateFields4CrossTotal(final Parameter _parameter)
        throws EFapsException
    {
        return new Exchange().updateFields4CrossTotal(_parameter);
    }

    /**
     * Update fields for pre calculate.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return updateFields4PreCalculate(final Parameter _parameter)
        throws EFapsException
    {
        return new Exchange().updateFields4PreCalculate(_parameter);
    }

    /**
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return createCalculated(final Parameter _parameter)
        throws EFapsException
    {
        return new Exchange().createCalculated(_parameter);
    }

    @Override
    public String getTypeName4SysConf(final Parameter _parameter)
        throws EFapsException
    {
        return getType4SysConf(_parameter).getName();
    }

    @Override
    protected Type getType4SysConf(final Parameter _parameter)
        throws EFapsException
    {
        return getCIType().getType();
    }

    @Override
    public CIType getCIType()
        throws EFapsException
    {
        return CISales.IncomingExchange;
    }
}
