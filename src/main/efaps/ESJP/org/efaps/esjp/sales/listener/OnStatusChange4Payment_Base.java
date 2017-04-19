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
package org.efaps.esjp.sales.listener;

import java.util.Properties;
import java.util.Set;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.esjp.admin.datamodel.AbstractSetStatusListener;
import org.efaps.esjp.common.properties.PropertiesUtil;
import org.efaps.esjp.sales.payment.DocumentUpdate;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;

/**
 * Listener that executes the evaluation if a document is paid.
 * The Status change that executes the listener is configured
 * via SystemConfiguration.
 *
 * @author The eFaps Team
 */
@EFapsUUID("ce230c12-1136-4cec-a0f4-93b111357af6")
@EFapsApplication("eFapsApp-Sales")
public abstract class OnStatusChange4Payment_Base
    extends AbstractSetStatusListener
{

    @Override
    public void after(final Parameter _parameter,
                      final Instance _instance,
                      final Status _status)
        throws EFapsException
    {
        new DocumentUpdate().updateDocument(_parameter, _instance);
    }

    @Override
    public Set<Status> getStatus()
        throws EFapsException
    {
        final Set<Status> ret = super.getStatus();
        final Properties props = PropertiesUtil.getProperties4Prefix(Sales.PAYMENT_RULES.get(), "OnStatusChange",
                        true);
        ret.addAll(getStatusListFromProperties(new Parameter(), props));
        return ret;
    }
}
