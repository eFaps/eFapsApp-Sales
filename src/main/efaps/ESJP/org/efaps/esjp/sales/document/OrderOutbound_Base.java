/*
final Return ret = new Return(); * Copyright 2003 - 2013 The eFaps Team
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

import java.io.File;
import java.util.List;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("bd08a90e-91ce-4f03-b1bc-921a53b71948")
@EFapsRevision("$Rev$")
public abstract class OrderOutbound_Base
    extends AbstractDocumentSum
{

    /**
     * Executed from a Command execute event to create a new OrderOutbound.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final CreatedDoc createdDoc = createDoc(_parameter);
        createPositions(_parameter, createdDoc);
        connectChannel2Document(_parameter, createdDoc);
        connect2Derived(_parameter, createdDoc);
        connect2Object(_parameter, createdDoc);
        connect2Terms(_parameter, createdDoc);

        final File file = createReport(_parameter, createdDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        executeProcess(_parameter, createdDoc);
        ret.put(ReturnValues.INSTANCE, createdDoc.getInstance());
        return ret;
    }

    /**
     * Executed from a Command execute event to create a new OrderOutbound.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final EditedDoc createdDoc = editDoc(_parameter);
        updatePositions(_parameter, createdDoc);

        final File file = createReport(_parameter, createdDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }


    /**
     * @param _parameter Parameter as passed from the eFaps API
     * @param _createdDoc the created document
     * @throws EFapsException on error
     */
    protected void connectChannel2Document(final Parameter _parameter,
                                           final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final Instance instCondition = Instance.get(_parameter.getParameterValue("conditionSales"));
        if (instCondition.isValid() && _createdDoc.getInstance().isValid()) {
            final Insert insert = new Insert(CISales.ChannelSalesChannel2Document);
            insert.add(CISales.ChannelSalesChannel2Document.FromLink, instCondition);
            insert.add(CISales.ChannelSalesChannel2Document.ToLink, _createdDoc.getInstance());
            insert.execute();
        }
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return connect2RecievingTicketTrigger(final Parameter _parameter)
        throws EFapsException
    {
        connect2DocTrigger(_parameter);
        return new Return();
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return connect2IncomingInvoiceTrigger(final Parameter _parameter)
        throws EFapsException
    {
        connect2DocTrigger(_parameter);
        return new Return();
    }

    protected void connect2DocTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_parameter.getInstance());
        final SelectBuilder selStatus = SelectBuilder.get().linkto(CISales.Document2DocumentAbstract.FromAbstractLink)
                        .attribute(CISales.OrderOutbound.Status);
        final SelectBuilder selOOInst = SelectBuilder.get().linkto(CISales.Document2DocumentAbstract.FromAbstractLink)
                        .instance();
        print.addSelect(selOOInst, selStatus);
        print.executeWithoutAccessCheck();
        final Instance ooInst = print.getSelect(selOOInst);
        final Status status = Status.get(print.<Long>getSelect(selStatus));
        // if the recieving ticket was open check if the status must change
        if (status.equals(Status.find(CISales.OrderOutboundStatus.Open))) {

            final DocComparator comp = new DocComparator();
            comp.setDocInstance(ooInst);

            final QueryBuilder queryBldr = new QueryBuilder(CISales.OrderOutbound2IncomingInvoice);
            queryBldr.addType(CISales.OrderOutbound2RecievingTicket);
            queryBldr.addWhereAttrEqValue(CISales.Document2DocumentAbstract.FromAbstractLink, ooInst);
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selDocInst = SelectBuilder.get()
                            .linkto(CISales.Document2DocumentAbstract.ToAbstractLink)
                            .instance();
            multi.addSelect(selDocInst);
            multi.executeWithoutAccessCheck();
            while (multi.next()) {
                final Instance docInst = multi.getSelect(selDocInst);
                final DocComparator docComp = new DocComparator();
                docComp.setDocInstance(docInst);
                if (docInst.getType().isKindOf(CISales.IncomingInvoice.getType())) {
                    comp.substractNet(docComp);
                } else {
                    comp.substractQuantity(docComp);
                }
            }
            if (comp.netIsZero() && comp.quantityIsZero()) {
                final Update update = new Update(ooInst);
                update.add(CISales.OrderOutbound.Status, Status.find(CISales.OrderOutboundStatus.Received));
                update.executeWithoutAccessCheck();
            }
        }
    }


    @Override
    protected boolean isContact2JavaScript4Document(final Parameter _parameter,
                                                    final List<Instance> _instances)
        throws EFapsException
    {
        boolean ret = true;
        if (!_instances.isEmpty() && _instances.get(0).isValid()) {
            ret = !_instances.get(0).getType().isKindOf(CISales.ProductRequest.getType());
        }
        return ret;
    }

    @Override
    public String getTypeName4SysConf(final Parameter _parameter)
        throws EFapsException
    {
        return CISales.OrderOutbound.getType().getName();
    }
}
