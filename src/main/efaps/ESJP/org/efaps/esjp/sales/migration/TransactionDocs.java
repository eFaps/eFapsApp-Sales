/*
 * Copyright 2003 - 2019 The eFaps Team
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

package org.efaps.esjp.sales.migration;

import java.util.HashSet;
import java.util.Set;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("4e54726d-0deb-4c1c-b88c-902e2b5194b0")
@EFapsApplication("eFapsApp-Sales")
public class TransactionDocs
{
    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(TransactionDocs.class);

    public void execute(final Parameter _parameter)
        throws EFapsException
    {
        LOG.info("Started migration");
        final QueryBuilder queryBldr = new QueryBuilder(CISales.TransactionDocument);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.TransactionDocument.Name, CISales.TransactionDocument.Date, CISales.TransactionDocument.Status);
        multi.executeWithoutAccessCheck();
        while (multi.next()) {
            final Instance transDocInst = multi.getCurrentInstance();
            final String transDocName = multi.getAttribute(CISales.TransactionDocument.Name);
            final DateTime date = multi.getAttribute(CISales.TransactionDocument.Date);
            final Long statusId = multi.getAttribute(CISales.TransactionDocument.Status);
            LOG.info("Migrating: {}", transDocName);
            final QueryBuilder queryBldr2 = new QueryBuilder(CIProducts.TransactionAbstract);
            queryBldr2.addWhereAttrEqValue(CIProducts.TransactionAbstract.Document, transDocInst);
            final MultiPrintQuery multi2 = queryBldr2.getPrint();
            multi2.executeWithoutAccessCheck();
            final Set<Instance> outgoing = new HashSet<>();
            final Set<Instance> incoming = new HashSet<>();
            while (multi2.next()) {
                final Instance transInst = multi2.getCurrentInstance();
                if (InstanceUtils.isType(transInst, CIProducts.TransactionInbound)
                                || InstanceUtils.isType(transInst, CIProducts.TransactionIndividualInbound)
                                || InstanceUtils.isType(transInst, CIProducts.TransactionInbound4StaticStorage)
                                || InstanceUtils.isType(transInst, CIProducts.TransactionReservationInbound)) {
                    incoming.add(transInst);
                } else {
                    outgoing.add(transInst);
                }
            }
            final Instance outDocInst = createNewDocument(false, outgoing, transDocName, date, statusId);
            final Instance inDocInst = createNewDocument(true, incoming, transDocName, date, statusId);
            if (InstanceUtils.isValid(outDocInst) && InstanceUtils.isValid(inDocInst)) {
                LOG.info("Connecting out and in");
                final Insert insert = new Insert(CISales.TransactionDocOut2In);
                insert.add(CISales.TransactionDocOut2In.FromLink, outDocInst);
                insert.add(CISales.TransactionDocOut2In.ToLink, inDocInst);
                insert.executeWithoutAccessCheck();
            }
        }
    }

    protected Instance createNewDocument(final boolean  _in,
                                     final Set<Instance> _transInstances,
                                     final String _name,
                                     final DateTime _date,
                                     final Long _statusId)
        throws EFapsException
    {
        Instance ret = null;
        if (!_transInstances.isEmpty()) {
            LOG.info("Found transactions: {}", _transInstances);
            final Insert insert = new Insert(_in ? CISales.TransactionDocumentIn : CISales.TransactionDocumentOut);
            insert.add(CISales.TransactionDocumentIn.Name, _in ? "I" + _name : "S" + _name);
            insert.add(CISales.TransactionDocumentIn.Date, _date);
            insert.add(CISales.TransactionDocumentIn.Status, _statusId);
            insert.executeWithoutAccessCheck();
            ret = insert.getInstance();
            LOG.info("Created new TransactionDocument: {}", ret);
            for (final Instance inst : _transInstances) {
                final Update update = new Update(inst);
                update.add(CIProducts.TransactionAbstract.Document, ret);
                update.executeWithoutAccessCheck();
            }
        }
        return ret;
    }

}
