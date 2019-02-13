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

package org.efaps.esjp.sales.listener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.esjp.products.listener.IOnTransaction;
import org.efaps.esjp.sales.document.TransactionDocument;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

@EFapsUUID("c1ef26a2-b815-43f9-993d-76c8a65b39b0")
@EFapsApplication("eFapsApp-Sales")
public abstract class OnProductTransaction_Base
    extends AbstractCommon
    implements IOnTransaction
{

    @Override
    public File createDocuments4Transactions(final Parameter _parameter,
                                             final Instance... _transactionInstances)
        throws EFapsException
    {
        File file = null;
        // Separate them into IN and OUT
        final List<Instance> transactionOutInsts = new ArrayList<>();
        final List<Instance> transactionInInsts = new ArrayList<>();
        for (final Instance transactionInstance : _transactionInstances) {
            if (InstanceUtils.isKindOf(transactionInstance, CIProducts.TransactionInbound)
                            || InstanceUtils.isKindOf(transactionInstance, CIProducts.TransactionIndividualInbound)
                            || InstanceUtils.isKindOf(transactionInstance, CIProducts.TransactionReservationInbound)
                            || InstanceUtils.isKindOf(transactionInstance,
                                            CIProducts.TransactionInbound4StaticStorage)) {
                transactionInInsts.add(transactionInstance);
            } else if (InstanceUtils.isKindOf(transactionInstance, CIProducts.TransactionOutbound)
                            || InstanceUtils.isKindOf(transactionInstance, CIProducts.TransactionIndividualOutbound)
                            || InstanceUtils.isKindOf(transactionInstance, CIProducts.TransactionReservationOutbound)) {
                transactionOutInsts.add(transactionInstance);
            }
        }
        final Instance outDoc = createDoc(_parameter, transactionOutInsts, true);
        connectDocument2ProductDocumentType(_parameter, outDoc);
        final Instance inDoc = createDoc(_parameter, transactionInInsts, false);
        connectDocument2ProductDocumentType(_parameter, inDoc);

        // if we have out and in -> movement
        if (InstanceUtils.isValid(outDoc) && InstanceUtils.isValid(inDoc)) {
            final Insert insert = new Insert(CISales.TransactionDocOut2In);
            insert.add(CISales.TransactionDocOut2In.FromLink, outDoc);
            insert.add(CISales.TransactionDocOut2In.ToLink, inDoc);
            insert.execute();

            final CommonDocument document = new CommonDocument();
            final Parameter tmpParameter = ParameterUtil.clone(_parameter, ParameterValues.INSTANCE, insert.getInstance());

            ParameterUtil.setProperty(tmpParameter, "JasperReport", Sales.TRANSDOCOUT2IN_JASPERREPORT.get());
            final Return retTmp = document.createReport(tmpParameter);
            file = (File) retTmp.get(ReturnValues.VALUES);
        }
        return file;
    }

    protected void connectDocument2ProductDocumentType(final Parameter _parameter,
                                                       final Instance _docInst)
        throws EFapsException
    {
        Instance prodDocTypeInst = null;
        if (InstanceUtils.isKindOf(_docInst, CISales.TransactionDocumentOut)) {
            prodDocTypeInst = Instance.get(ParameterUtil.getParameterValue(_parameter, "productDocumentTypeOut",
                            "productDocumentType"));
        } else if (InstanceUtils.isKindOf(_docInst, CISales.TransactionDocumentIn)) {
            prodDocTypeInst = Instance.get(ParameterUtil.getParameterValue(_parameter, "productDocumentTypeIn",
                            "productDocumentType"));
        }
        if (InstanceUtils.isValid(prodDocTypeInst)) {
            final Insert insert = new Insert(CISales.Document2ProductDocumentType);
            insert.add(CISales.Document2ProductDocumentType.DocumentLink, _docInst);
            insert.add(CISales.Document2ProductDocumentType.DocumentTypeLink, prodDocTypeInst);
            insert.execute();
        }
    }

    protected Instance createDoc(final Parameter _parameter,
                                 final List<Instance> _transactionInsts,
                                 final boolean _outgoing)
        throws EFapsException
    {
        Instance ret = null;
        if (!_transactionInsts.isEmpty()) {
            final TransactionDocument transDoc = new TransactionDocument();
            ret = transDoc.createDocument(_parameter,
                            _outgoing ? CISales.TransactionDocumentOut : CISales.TransactionDocumentIn,
                            evalDate(_parameter, _transactionInsts.get(0)));
            // connect the transactions to the document
            for (final Instance transactionOutInst : _transactionInsts) {
                final Update update = new Update(transactionOutInst);
                update.add(CIProducts.TransactionAbstract.Document, ret);
                update.executeWithoutTrigger();
            }
        }
        return ret;
    }

    protected DateTime evalDate(final Parameter _parameter,
                                final Instance transactionInst)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(transactionInst);
        print.addAttribute(CIProducts.TransactionAbstract.Date);
        print.executeWithoutAccessCheck();
        return print.getAttribute(CIProducts.TransactionAbstract.Date);
    }

    @Override
    public int getWeight()
    {
        return 0;
    }
}
