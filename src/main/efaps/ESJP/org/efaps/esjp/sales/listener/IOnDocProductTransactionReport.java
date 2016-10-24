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

import java.util.Collection;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.IEsjpListener;
import org.efaps.esjp.sales.report.DocProductTransactionReport_Base.DataBean;
import org.efaps.esjp.sales.report.DocProductTransactionReport_Base.DynDocProductTransactionReport;
import org.efaps.util.EFapsException;

/**
 * The Interface IOnDocProductTransactionReport.
 *
 * @author The eFaps Team
 */
@EFapsUUID("08703d23-04a2-414d-b12d-8409cc2dea27")
@EFapsApplication("eFapsApp-Sales")
public interface IOnDocProductTransactionReport
    extends IEsjpListener
{

    /**
     * Update values.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _dynReport the dyn report
     * @param _values the values
     * @throws EFapsException on error
     */
    void updateValues(final Parameter _parameter,
                      final DynDocProductTransactionReport _dynReport,
                      final Collection<DataBean> _values)
        throws EFapsException;

}
