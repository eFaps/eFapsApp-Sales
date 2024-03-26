/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
 */
package org.efaps.esjp.sales.report;

import java.util.Properties;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;

@EFapsUUID("dbba8496-7c26-480b-b5f8-081c16f87222")
@EFapsApplication("eFapsApp-Sales")
public abstract class AccountsRecievableReport_Base
    extends AccountsAbstractReport
{

    @Override
    protected AccountsAbstractDynReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new AccountsRecievableDynReport(this);
    }

    @Override
    protected boolean isShowAssigned()
        throws EFapsException
    {
        return Sales.REPORT_ACCOUNTSRECIEVABLE_ASSIGNED.get();
    }

    @Override
    protected boolean isShowCondition()
        throws EFapsException
    {
        return Sales.CHACTIVATESALESCOND.get();
    }

    @Override
    protected Properties getProperties4TypeList(final Parameter _parameter, final String _fieldName)
        throws EFapsException
    {
        return Sales.REPORT_ACCOUNTSRECIEVABLE.get();
    }

    public static class AccountsRecievableDynReport
        extends AccountsAbstractDynReport
    {

        public AccountsRecievableDynReport(final AccountsRecievableReport_Base _filteredReport)
        {
            super(_filteredReport);
        }

        @Override
        protected AbstractDataBean getDataBean(final Parameter _parameter)
        {
            return new DataBean();
        }

        @Override
        protected Properties getProperties()
            throws EFapsException
        {
            return Sales.REPORT_ACCOUNTSRECIEVABLE.get();
        }

        @Override
        protected boolean isShowSwapInfo()
            throws EFapsException
        {
            return Sales.REPORT_ACCOUNTSRECIEVABLE_SWAPINFO.get();
        }
    }

    public static class DataBean
        extends AbstractDataBean
    {

        @Override
        protected Properties getProperties()
            throws EFapsException
        {
            return Sales.REPORT_ACCOUNTSRECIEVABLE.get();
        }
    }
}
