/*
 * Copyright 2003 - 2009 The eFaps Team
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

import java.util.Properties;
import java.util.UUID;

import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.SalesSettings;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: RecievingTicket_Base.java 8018 2012-10-09 17:47:24Z
 *          jan@moxter.net $
 */
@EFapsUUID("f6f4e147-fc24-487e-ae81-69e4c44ac964")
@EFapsRevision("$Rev$")
public abstract class RecievingTicket_Base
    extends AbstractProductDocument
{
    public static final String REVISIONKEY = "org.efaps.esjp.sales.document.RecievingTicket.RevisionKey";

    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc doc = createDoc(_parameter);
        createPositions(_parameter, doc);
        connect2ProductDocumentType(_parameter, doc);
        connect2Derived(_parameter, doc);
        return new Return();
    }

    @Override
    protected String getDocName4Create(final Parameter _parameter)
        throws EFapsException
    {
        return _parameter.getParameterValue("name");
    }

    @Override
    protected void add2DocCreate(final Parameter _parameter,
                                 final Insert _insert,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final SystemConfiguration config = Sales.getSysConfig();
        final Properties props = config.getAttributeValueAsProperties(SalesSettings.RECIEVINGTICKETSEQUENCE);

        final NumberGenerator numgen = NumberGenerator.get(UUID.fromString(props.getProperty("UUID")));
        if (numgen != null) {
            final String revision = numgen.getNextVal();
            Context.getThreadContext().setSessionAttribute(IncomingCreditNote_Base.REVISIONKEY, revision);
            _insert.add(CISales.RecievingTicket.Revision, revision);
        }

    }

    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new return
     * @throws EFapsException on error
     */
    public Return recievingTicketPositionInsertTrigger(final Parameter _parameter)
        throws EFapsException
    {
        createTransaction4PositionTrigger(_parameter, CIProducts.TransactionInbound.getType(),
                        evaluateStorage4PositionTrigger(_parameter));
        return new Return();
    }

    public Return recievingTicketUpdatePostTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instance = _parameter.getInstance();
        if (instance.isValid()) {
            final PrintQuery print = new PrintQuery(instance);
            print.addAttribute(CISales.RecievingTicket.Status);
            print.executeWithoutAccessCheck();

            if (Status.find(CISales.RecievingTicketStatus.Canceled).getId()
                            == print.<Long>getAttribute(CISales.RecievingTicket.Status)) {
                final QueryBuilder queryBldr = new QueryBuilder(CIProducts.TransactionInOutAbstract);
                queryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Document, instance);
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CIProducts.TransactionInOutAbstract.Quantity,
                                CIProducts.TransactionInOutAbstract.Storage,
                                CIProducts.TransactionInOutAbstract.Product,
                                CIProducts.TransactionInOutAbstract.Description,
                                CIProducts.TransactionInOutAbstract.Date,
                                CIProducts.TransactionInOutAbstract.UoM);
                multi.execute();
                while (multi.next()) {
                    Insert insert;
                    if (CIProducts.TransactionInbound.getType().equals(multi.getCurrentInstance().getType())) {
                        insert = new Insert(CIProducts.TransactionOutbound);
                    } else {
                        insert = new Insert(CIProducts.TransactionInbound);
                    }
                    insert.add(CIProducts.TransactionInOutAbstract.Quantity,
                                    multi.getAttribute(CIProducts.TransactionInOutAbstract.Quantity));
                    insert.add(CIProducts.TransactionInOutAbstract.Storage,
                                    multi.getAttribute(CIProducts.TransactionInOutAbstract.Storage));
                    insert.add(CIProducts.TransactionInOutAbstract.Product,
                                    multi.getAttribute(CIProducts.TransactionInOutAbstract.Product));
                    insert.add(CIProducts.TransactionInOutAbstract.Description,
                                    multi.getAttribute(CIProducts.TransactionInOutAbstract.Description));
                    insert.add(CIProducts.TransactionInOutAbstract.Date,
                                    multi.getAttribute(CIProducts.TransactionInOutAbstract.Date));
                    insert.add(CIProducts.TransactionInOutAbstract.Document, instance);
                    insert.add(CIProducts.TransactionInOutAbstract.UoM,
                                    multi.getAttribute(CIProducts.TransactionInOutAbstract.UoM));
                    insert.execute();
                }
            }
        }
        return new Return();
    }
}
