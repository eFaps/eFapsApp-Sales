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


package org.efaps.esjp.sales.document;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.efaps.admin.common.MsgPhrase;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.sales.Calculator;
import org.efaps.esjp.sales.Channel;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.efaps.util.cache.CacheReloadException;

/**
 *
 * @author The eFaps Team
 */
@EFapsUUID("65d49d25-c1b9-4883-8bb1-0c53292ee789")
@EFapsApplication("eFapsApp-Sales")
public abstract class ServiceOrderOutbound_Base
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
        connect2Derived(_parameter, createdDoc);
        connect2Terms(_parameter, createdDoc);
        connect2Object(_parameter, createdDoc);
        final File file = createReport(_parameter, createdDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        ret.put(ReturnValues.INSTANCE, createdDoc.getInstance());
        return ret;
    }

    @Override
    protected void add2DocCreate(final Parameter _parameter,
                                 final Insert _insert,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        super.add2DocCreate(_parameter, _insert, _createdDoc);
        final String deliveryDate = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.ServiceOrderOutbound.DeliveryDate.name));
        _insert.add(CISales.ServiceOrderOutbound.DeliveryDate, deliveryDate);
        _createdDoc.getValues().put(CISales.ServiceOrderOutbound.DeliveryDate.name, deliveryDate);
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
        final EditedDoc editedDoc = editDoc(_parameter);
        updatePositions(_parameter, editedDoc);
        updateConnection2Object(_parameter, editedDoc);

        final File file = createReport(_parameter, editedDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    @Override
    protected void add2DocEdit(final Parameter _parameter,
                               final Update _update,
                               final EditedDoc _editDoc)
        throws EFapsException
    {
        super.add2DocEdit(_parameter, _update, _editDoc);
        final String deliveryDate = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.ServiceOrderOutbound.DeliveryDate.name));
        _update.add(CISales.ServiceOrderOutbound.DeliveryDate, deliveryDate);
        _editDoc.getValues().put(CISales.ServiceOrderOutbound.DeliveryDate.name, deliveryDate);
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return connect2IncomingInvoiceTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_parameter.getInstance());
        final SelectBuilder selStatus = SelectBuilder.get().linkto(CISales.Document2DocumentAbstract.FromAbstractLink)
                        .attribute(CISales.ServiceOrderOutbound.Status);
        final SelectBuilder selOOInst = SelectBuilder.get().linkto(CISales.Document2DocumentAbstract.FromAbstractLink)
                        .instance();
        print.addSelect(selOOInst, selStatus);
        print.executeWithoutAccessCheck();

        final Instance ooInst = print.getSelect(selOOInst);
        final Status status = Status.get(print.<Long>getSelect(selStatus));
        final DocComparator comp = getComparator(_parameter, ooInst);
        final Map<Status, Status> maping = getStatusMapping4connect2IncomingInvoice();
        if (comp.netIsZero() && maping.containsKey(status)) {
            final Update update = new Update(ooInst);
            update.add(CISales.ServiceOrderOutbound.Status, maping.get(status));
            update.executeWithoutAccessCheck();
        }
        return new Return();
    }

    /**
     * Gets the comparator.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _orderInst the order inst
     * @return the comparator
     * @throws EFapsException on error
     */
    protected DocComparator getComparator(final Parameter _parameter,
                                          final Instance _orderInst)
                                              throws EFapsException
    {
        final DocComparator ret = new DocComparator();
        ret.setDocInstance(_orderInst);

        final QueryBuilder queryBldr = new QueryBuilder(CISales.ServiceOrderOutbound2IncomingInvoice);
        queryBldr.addWhereAttrEqValue(CISales.Document2DocumentAbstract.FromAbstractLink, _orderInst);
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
            ret.substractNet(docComp);
        }
        return ret;
    }

    /**
     * Gets the status mapping4connect2 incoming invoice.
     *
     * @return the status mapping4connect2 incoming invoice
     * @throws CacheReloadException the cache reload exception
     */
    protected Map<Status, Status> getStatusMapping4connect2IncomingInvoice()
        throws CacheReloadException
    {
        final Map<Status, Status> ret = new HashMap<>();
        ret.put(Status.find(CISales.ServiceOrderOutboundStatus.Open),
                        Status.find(CISales.ServiceOrderOutboundStatus.Closed));
        return ret;
    }



    @Override
    protected void add2UpdateMap4Contact(final Parameter _parameter,
                                         final Instance _contactInstance,
                                         final Map<String, Object> _map)
        throws EFapsException
    {
        super.add2UpdateMap4Contact(_parameter, _contactInstance, _map);
        if (Sales.SERVICEORDEROUTBOUND_ACTIVATECONDITION.get()) {
            InterfaceUtils.appendScript4FieldUpdate(_map,
                            new Channel().getConditionJs(_parameter, _contactInstance,
                                            CISales.ChannelPurchaseCondition2Contact));
        }
    }

    @Override
    protected StringBuilder add2JavaScript4DocumentContact(final Parameter _parameter,
                                                           final List<Instance> _instances,
                                                           final Instance _contactInstance)
        throws EFapsException
    {
        final StringBuilder ret = super.add2JavaScript4DocumentContact(_parameter, _instances, _contactInstance);
        if (Sales.SERVICEORDEROUTBOUND_ACTIVATECONDITION.get()) {
            ret.append(new Channel().getConditionJs(_parameter, _contactInstance,
                            CISales.ChannelPurchaseCondition2Contact));
        }
        return ret;
    }

    @Override
    protected StringBuilder add2JavaScript4Document(final Parameter _parameter,
                                                    final List<Instance> _instances)
        throws EFapsException
    {
        final StringBuilder ret = super.add2JavaScript4Document(_parameter, _instances);
        // if we create from a ServiceRequest and for ServiceRequest and OrderOutbOund AssignEmployee is activated
        if (!_instances.isEmpty() && InstanceUtils.isKindOf(_instances.get(0), CISales.ServiceRequest)
                        && Sales.SERVICEREQUEST_ASSIGNEMPLOYEE.get() && Sales.SERVICEORDEROUTBOUND_ASSIGNEMPLOYEE.get()) {
           final QueryBuilder queryBldr = new QueryBuilder(CISales.Employee2ServiceRequest);
           queryBldr.addWhereAttrEqValue(CISales.Employee2ServiceRequest.ToLink, _instances.get(0));
           final MultiPrintQuery multi = queryBldr.getPrint();
           final SelectBuilder selEmployee = SelectBuilder.get().linkto(CISales.Employee2ServiceRequest.FromLink);
           final SelectBuilder selEmployeeOid = new SelectBuilder(selEmployee).oid();
           multi.addSelect(selEmployeeOid);
           //HumanResource_EmployeeWithNumberMsgPhrase
           final MsgPhrase msgPhrase = MsgPhrase.get(UUID.fromString("c6686d34-f9d7-4bf4-b9f1-80dad440eac4"));
           multi.addMsgPhrase(selEmployee, msgPhrase);
           multi.executeWithoutAccessCheck();
           if (multi.next()) {
               final String label = multi.getMsgPhrase(selEmployee, msgPhrase);
               final String employeeOid = multi.getSelect(selEmployeeOid);
               ret.append(getSetFieldValue(0, "employee", employeeOid, label)).append("\n");
           }
        }
        return ret;
    }

    @Override
    protected boolean isContact2JavaScript4Document(final Parameter _parameter,
                                                    final List<Instance> _instances)
        throws EFapsException
    {
        boolean ret = true;
        if (!_instances.isEmpty() && _instances.get(0).isValid()) {
            ret = !_instances.get(0).getType().isKindOf(CISales.ServiceRequest.getType());
        }
        return ret;
    }

    @Override
    public CIType getCIType()
        throws EFapsException
    {
        return CISales.ServiceOrderOutbound;
    }

    @Override
    public Calculator getCalculator(final Parameter _parameter,
                                    final Calculator _oldCalc,
                                    final String _oid,
                                    final String _quantity,
                                    final String _unitPrice,
                                    final String _discount,
                                    final boolean _priceFromDB,
                                    final int _idx)
        throws EFapsException
    {

        return new Calculator(_parameter, _oldCalc, _oid, _quantity, _unitPrice, _discount, _priceFromDB, this)
        {

            private static final long serialVersionUID = 1L;

            @Override
            protected UUID getPriceListUUID()
            {
                return CIProducts.ProductPricelistPurchase.uuid;
            }
        };
    }
}
