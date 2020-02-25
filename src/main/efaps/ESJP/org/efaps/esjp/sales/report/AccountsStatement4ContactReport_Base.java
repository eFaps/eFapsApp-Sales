/*
 * Copyright 2003 - 2020 The eFaps Team
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
package org.efaps.esjp.sales.report;

import java.util.Properties;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;

@EFapsUUID("0c34f5fc-731c-46f9-9f64-d0d838a14105")
@EFapsApplication("eFapsApp-Sales")
public abstract class AccountsStatement4ContactReport_Base
    extends AccountsAbstractReport
{

    @Override
    protected AccountsAbstractDynReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new AccountsStatement4ContactDynReport(this);
    }

    @Override
    protected boolean isShowAssigned()
        throws EFapsException
    {
        return false;
    }

    @Override
    protected boolean isShowCondition()
        throws EFapsException
    {
        return Sales.CHACTIVATESALESCOND.get();
    }

    public static class AccountsStatement4ContactDynReport
        extends AccountsAbstractDynReport
    {

        public AccountsStatement4ContactDynReport(final AccountsStatement4ContactReport_Base _filteredReport)
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
            return Sales.REPORT_ACCOUNTS4CONTACT.get();
        }

        @Override
        protected boolean isShowSwapInfo()
            throws EFapsException
        {
            return Sales.REPORT_ACCOUNTS4CONTACT_SWAPINFO.get();
        }

        @Override
        protected boolean isContactReport()
        {
            return true;
        }
    }

    public static class DataBean
        extends AbstractDataBean
    {

        @Override
        protected Properties getProperties()
            throws EFapsException
        {
            return Sales.REPORT_ACCOUNTS4CONTACT.get();
        }
    }
}
