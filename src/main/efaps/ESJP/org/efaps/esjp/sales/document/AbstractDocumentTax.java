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

package org.efaps.esjp.sales.document;

import java.util.List;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.util.EFapsException;

/**
 * This class must be replaced for customization, therefore it is left empty.
 * Functional description can be found in the related "<code>_base</code>"
 * class.
 *
 * @author The eFaps Team
 */
@EFapsUUID("ad706d17-00d3-4fbd-b461-8a406c3b90ac")
@EFapsApplication("eFapsApp-Sales")
public abstract class AbstractDocumentTax
    extends AbstractDocumentTax_Base
{
    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst instance of the document
     * @return html field value
     * @throws EFapsException on error
     */
    public static StringBuilder getSmallTaxField4Doc(final Parameter _parameter,
                                                     final Instance _docInst)
        throws EFapsException
    {
        return AbstractDocumentTax_Base.getSmallTaxField4Doc(_parameter, _docInst);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst instance of the document
     * @return html field value
     * @throws EFapsException on error
     */
    public static DocTaxInfo getDocTaxInfo(final Parameter _parameter,
                                           final Instance _docInst)
        throws EFapsException
    {
        return AbstractDocumentTax_Base.getDocTaxInfo(_parameter, _docInst);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInstances instance of the document
     * @throws EFapsException on error
     */
    public static void evaluateDocTaxInfo(final Parameter _parameter,
                                          final List<Instance> _docInstances)
        throws EFapsException
    {
        AbstractDocumentTax_Base.evaluateDocTaxInfo(_parameter, _docInstances);
    }
}
