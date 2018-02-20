/*
 * Copyright 2003 - 2018 The eFaps Team
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

package org.efaps.esjp.sales;

import java.util.List;
import java.util.Properties;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Delete;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.common.properties.PropertiesUtil;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.esjp.erp.Naming;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;

@EFapsUUID("bc0668dd-36b1-4762-949a-be0b732008d1")
@EFapsApplication("eFapsApp-Sales")
public class CreditLine_Base
    extends AbstractCommon
{
    /**
     * Creates a new CreditLine.
     *
     * @param _parameter the parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        return new Create()
        {
            @Override
            protected void add2basicInsert(final Parameter _parameter, final Insert _insert)
                throws EFapsException
            {
                super.add2basicInsert(_parameter, _insert);
                _insert.add(CISales.CreditLine.Name, new Naming().fromNumberGenerator(_parameter, null));
            }
        }.execute(_parameter);
    }

    /**
     * Update credit line.
     *
     * @param _parameter the parameter
     * @param _docInstance the doc instance
     * @throws EFapsException the eFaps exception
     */
    public void updateCreditLine(final Parameter _parameter, final Instance _docInstance)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_docInstance);
        print.addAttribute(_docInstance.getType().getStatusAttribute());
        print.executeWithoutAccessCheck();
        updateCreditLine(_parameter, _docInstance,
                        Status.get(print.<Long>getAttribute(_docInstance.getType().getStatusAttribute())));
    }

    /**
     * Update credit line.
     *
     * @param _parameter the parameter
     * @param _docInstance the doc instance
     * @param _status the status
     * @throws EFapsException the eFaps exception
     */
    public void updateCreditLine(final Parameter _parameter, final Instance _docInstance, final Status _status)
        throws EFapsException
    {
        if (getAddStatusList().contains(_status) && !isAssigned(_docInstance)) {
            final PrintQuery print = new PrintQuery(_docInstance);
            print.addAttribute(CIERP.DocumentAbstract.Contact);
            print.executeWithoutAccessCheck();
            final QueryBuilder queryBldr = new QueryBuilder(CISales.CreditLine);
            queryBldr.addWhereAttrEqValue(CISales.CreditLine.Status, Status.find(CISales.CreditLineStatus.Approved));
            queryBldr.addWhereAttrEqValue(CISales.CreditLine.ContactLink,
                            print.<Long>getAttribute(CIERP.DocumentAbstract.Contact));
            final InstanceQuery query = queryBldr.getQuery();
            query.executeWithoutAccessCheck();
            if (query.next()) {
                final Insert insert = new Insert(CISales.CreditLine2DocumentSum);
                insert.add(CISales.CreditLine2DocumentSum.FromLink, query.getCurrentValue());
                insert.add(CISales.CreditLine2DocumentSum.ToLink, _docInstance);
                insert.executeWithoutAccessCheck();
            }
        } else if (getRemoveStatusList().contains(_status)) {
            final Instance relInstance = getCreditLine2DocInstance(_docInstance);
            if (relInstance.isValid()) {
                new Delete(relInstance).execute();
            }
        }
    }

    /**
     * Gets the credit line two doc instance.
     *
     * @param _docInstance the doc instance
     * @return the credit line two doc instance
     * @throws EFapsException the e faps exception
     */
    protected Instance getCreditLine2DocInstance(final Instance _docInstance)
        throws EFapsException
    {
        Instance ret = Instance.get("");
        final QueryBuilder queryBldr = new QueryBuilder(CISales.CreditLine2DocumentSum);
        queryBldr.addWhereAttrEqValue(CISales.CreditLine2DocumentSum.ToLink, _docInstance);
        final InstanceQuery query = queryBldr.getQuery();
        query.execute();
        if (query.next()) {
            ret = query.getCurrentValue();
        }
        return ret;
    }

    /**
     * Checks if is assigned.
     *
     * @param _docInstance the doc instance
     * @return true, if is assigned
     * @throws EFapsException the e faps exception
     */
    protected boolean isAssigned(final Instance _docInstance)
        throws EFapsException
    {
        return getCreditLine2DocInstance(_docInstance).isValid();
    }

     /**
     * Gets the adds the status list.
     *
     * @return the adds the status list
     * @throws EFapsException the e faps exception
     */
    public List<Status> getAddStatusList()
        throws EFapsException
    {
        final Properties properties = Sales.CREDITLINE_LISTENER.get();
        return getStatusListFromProperties(ParameterUtil.instance(),
                        PropertiesUtil.getProperties4Prefix(properties, "ADD"));
    }

    /**
     * Gets the removes the status list.
     *
     * @return the removes the status list
     * @throws EFapsException the e faps exception
     */
    public List<Status> getRemoveStatusList()
        throws EFapsException
    {
        final Properties properties = Sales.CREDITLINE_LISTENER.get();
        return getStatusListFromProperties(ParameterUtil.instance(),
                        PropertiesUtil.getProperties4Prefix(properties, "REMOVE"));
    }
}
