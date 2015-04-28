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

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.esjp.common.util.InterfaceUtils;
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
    /**
     * Revision Key.
     */
    public static final String REVISIONKEY =  RecievingTicket.class.getName() +  ".RevisionKey";

    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = createDoc(_parameter);
        createPositions(_parameter, createdDoc);
        createIndiviuals(_parameter, createdDoc);
        connect2ProductDocumentType(_parameter, createdDoc);
        connect2Derived(_parameter, createdDoc);
        connect2Object(_parameter, createdDoc);
        return new Return();
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
            Context.getThreadContext().setSessionAttribute(RecievingTicket_Base.REVISIONKEY, revision);
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

    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new return
     * @throws EFapsException on error
     */
    public Return incomingInvoiceMultiPrint(final Parameter _parameter)
        throws EFapsException
    {
        final MultiPrint multi = new MultiPrint()
        {
            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
                throws EFapsException
            {
                super.add2QueryBldr(_parameter, _queryBldr);
                final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.DocumentAbstract);
                attrQueryBldr.addWhereAttrEqValue(CISales.DocumentAbstract.ID, _parameter.getInstance());
                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.DocumentAbstract.Contact);
                _queryBldr.addWhereAttrInQuery(CISales.DocumentAbstract.Contact, attrQuery);
            }
        };
        return multi.execute(_parameter);
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new return
     * @throws EFapsException on error
     */
    public Return connect2IncomingInvoiceTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_parameter.getInstance());
        final SelectBuilder selStatus = SelectBuilder.get().linkto(CISales.IncomingInvoice2RecievingTicket.ToLink)
                        .attribute(CISales.RecievingTicket.Status);
        final SelectBuilder selRecInst = SelectBuilder.get().linkto(CISales.IncomingInvoice2RecievingTicket.ToLink)
                        .instance();
        print.addSelect(selStatus, selRecInst);
        print.addAttribute(CISales.IncomingInvoice2RecievingTicket.ToLink,
                        CISales.IncomingInvoice2RecievingTicket.FromLink);
        print.executeWithoutAccessCheck();
        final Status status = Status.get(print.<Long>getSelect(selStatus));
        // if the recieving ticket was open check if the status must change
        if (status.equals(Status.find(CISales.RecievingTicketStatus.Open))) {
            final Instance recInst = print.<Instance>getSelect(selRecInst);
            final DocComparator comp = new DocComparator();
            comp.setDocInstance(recInst);

            final QueryBuilder queryBldr = new QueryBuilder(CISales.IncomingInvoice2RecievingTicket);
            queryBldr.addWhereAttrEqValue(CISales.IncomingInvoice2RecievingTicket.ToLink, recInst);
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selInvInst = SelectBuilder.get()
                            .linkto(CISales.IncomingInvoice2RecievingTicket.FromLink)
                            .instance();
            multi.addSelect(selInvInst);
            multi.executeWithoutAccessCheck();
            while (multi.next()) {
                final Instance invInst = multi.getSelect(selInvInst);
                final DocComparator docComp = new DocComparator();
                docComp.setDocInstance(invInst);
                comp.substractQuantity(docComp);
            }

            if (comp.quantityIsZero()) {
                final Update update = new Update(print.<Instance>getSelect(selRecInst));
                update.add(CISales.RecievingTicket.Status, Status.find(CISales.RecievingTicketStatus.Closed));
                update.executeWithoutAccessCheck();
            }
        }
        return new Return();
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return with Snipplet
     * @throws EFapsException on error
     */
    public Return showRevisionFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String revision = (String) Context.getThreadContext().getSessionAttribute(
                        RecievingTicket_Base.REVISIONKEY);
        Context.getThreadContext().setSessionAttribute(RecievingTicket_Base.REVISIONKEY, null);
        final StringBuilder html = new StringBuilder();
        html.append("<span style=\"text-align: center; width: 98%; font-size:40pt; height: 55px; position:absolute\">")
                        .append(revision).append("</span>");
        ret.put(ReturnValues.SNIPLETT, html.toString());
        return ret;
    }

    @Override
    protected StringBuilder add2JavaScript4DocumentContact(final Parameter _parameter,
                                                           final List<Instance> _instances,
                                                           final Instance _contactInstance)
        throws EFapsException
    {
        final StringBuilder ret = super.add2JavaScript4DocumentContact(_parameter, _instances, _contactInstance);
        if (Sales.getSysConfig().getAttributeValueAsBoolean(SalesSettings.RECIEVINGTICKETFROMORDEROUTBOUND)) {
            ret.append(getJS4Doc4Contact(_parameter, _contactInstance, CISales.OrderOutbound.getType(),
                            CIFormSales.Sales_RecievingTicketForm.orderOutbound.name,
                            Status.find(CISales.OrderOutboundStatus.Open)));
        }
        return ret;
    }

    @Override
    protected void add2UpdateMap4Contact(final Parameter _parameter,
                                         final Instance _contactInstance,
                                         final Map<String, Object> _map)
        throws EFapsException
    {
        super.add2UpdateMap4Contact(_parameter, _contactInstance, _map);
        if (Sales.getSysConfig().getAttributeValueAsBoolean(SalesSettings.RECIEVINGTICKETFROMORDEROUTBOUND)) {
            InterfaceUtils.appendScript4FieldUpdate(_map, getJS4Doc4Contact(_parameter, _contactInstance,
                            CISales.OrderOutbound.getType(), CIFormSales.Sales_RecievingTicketForm.orderOutbound.name,
                            Status.find(CISales.OrderOutboundStatus.Open)));
        }
    }
}
