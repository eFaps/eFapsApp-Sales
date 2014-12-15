/*
 * Copyright 2003 - 2014 The eFaps Team
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

package org.efaps.esjp.sales.listener;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabBuilder;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.IEsjpListener;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.sales.report.DocumentSumGroupedByDate_Base.ValueList;
import org.efaps.util.EFapsException;

/**
 * Contains methods that are executed during the process of executing queries
 * against the eFaps Database like Autocompletes or MultiPrints.
 *
 * @author The eFaps Team
 * @version $Id: IOnDocumentSumReport.java 14626 2014-12-15 21:15:17Z
 *          jan@moxter.net $
 */
@EFapsUUID("b7b529ac-e62d-4efd-8db5-01c69ead37ad")
@EFapsRevision("$Rev$")
public interface IOnDocumentSumReport
    extends IEsjpListener
{

    /**
     * @param _parameter
     * @param _builder
     * @param _crosstab
     */
    void add2ColumnDefinition(final Parameter _parameter,
                              final JasperReportBuilder _builder,
                              final CrosstabBuilder _crosstab)
        throws EFapsException;

    /**
     * @param _parameter
     * @param _builder
     * @param _crosstab
     */
    void prepend2ColumnDefinition(final Parameter _parameter,
                                  final JasperReportBuilder _builder,
                                  final CrosstabBuilder _crosstab)
        throws EFapsException;

    /**
     * @param _parameter
     * @param _ret
     */
    void add2ValueList(final Parameter _parameter,
                       final ValueList _ret)
        throws EFapsException;

    /**
     * @param _parameter
     * @param _queryBldr
     */
    void add2QueryBuilder(Parameter _parameter,
                          QueryBuilder _queryBldr)
        throws EFapsException;

}
