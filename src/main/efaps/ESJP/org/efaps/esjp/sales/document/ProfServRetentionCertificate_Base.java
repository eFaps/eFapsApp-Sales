/*
 * Copyright 2003 - 2013 The eFaps Team
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

package org.efaps.esjp.sales.document;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: IncomingProfServRetentionCertificate_Base.java 13286 2014-07-10 00:56:02Z
 *          jan@moxter.net $
 */
@EFapsUUID("22d97ba9-4b62-4d98-b2a4-3061308e5985")
@EFapsApplication("eFapsApp-Sales")
public abstract class ProfServRetentionCertificate_Base
    extends AbstractDocumentSum
{
    /**
     * @param _parameter Parameter as passed from eFaps API.
     * @return List of instances
     * @throws EFapsException on error
     */
    public Return documentMultiPrint(final Parameter _parameter)
        throws EFapsException
    {
        final MultiPrint multi = new MultiPrint()
        {

            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.ProfServRetentionCertificate2IncomingProfServReceipt);
                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(
                                CISales.ProfServRetentionCertificate2IncomingProfServReceipt.ToLink);
                _queryBldr.addWhereAttrNotInQuery(CIERP.DocumentAbstract.ID, attrQuery);

                final QueryBuilder attrQueryBldr2 = new QueryBuilder(CISales.ProfServRetentionCertificate);
                attrQueryBldr2.addWhereAttrEqValue(CISales.ProfServRetentionCertificate.ID, _parameter.getInstance());
                final AttributeQuery attrQuery2 = attrQueryBldr2
                                .getAttributeQuery(CISales.ProfServRetentionCertificate.Contact);

                final QueryBuilder attrQueryBldr3 = new QueryBuilder(CISales.IncomingProfServRetention);
                attrQueryBldr3.addWhereAttrInQuery(CISales.IncomingProfServRetention.Contact, attrQuery2);
                final AttributeQuery attrQuery3 = attrQueryBldr3.getAttributeQuery(CISales.IncomingProfServRetention.ID);

                _queryBldr.addWhereAttrInQuery(CIERP.DocumentAbstract.ID, attrQuery3);
            }
        };
        return multi.execute(_parameter);
    }
}
