/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
 */
package org.efaps.esjp.sales.document;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.AbstractPositionWarning;
import org.efaps.esjp.erp.IWarning;
import org.efaps.esjp.products.Cost;
import org.efaps.esjp.sales.Calculator;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("a6067925-214b-44b9-98ec-833d64fc3f7f")
@EFapsApplication("eFapsApp-Sales")
public abstract class ProductionCosting_Base
    extends AbstractDocumentSum
{

    /**
     * Executed from a Command execute vent to create a new Incoming Invoice.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = createDoc(_parameter);
        createPositions(_parameter, createdDoc);
        connect2DocumentType(_parameter, createdDoc);
        final List<Instance> derived = connect2Derived(_parameter, createdDoc);
        connect2Object(_parameter, createdDoc);
        connect2ProductionReport(_parameter, createdDoc, derived);
        afterCreate(_parameter, createdDoc.getInstance());
        return new Return();
    }

    /**
     * Connect2 production report.
     *
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _createdDoc   created doc
     * @param _deriveds the deriveds
     * @throws EFapsException on error
     */
    protected void connect2ProductionReport(final Parameter _parameter,
                                           final CreatedDoc _createdDoc,
                                           final List<Instance> _deriveds)
        throws EFapsException
    {
        final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.Document2DerivativeDocument);
        attrQueryBldr.addWhereAttrEqValue(CISales.Document2DerivativeDocument.To, _createdDoc.getInstance());

        final QueryBuilder queryBldr;
        Instance connectInst = null;
        if (_deriveds == null | _deriveds.isEmpty() || _deriveds.get(0).getType().isKindOf(CISales.ProductionCosting)) {
            queryBldr = new QueryBuilder(CISales.ProductionCosting2ProductionReport);
            queryBldr.addWhereAttrInQuery(CISales.ProductionCosting2ProductionReport.FromLink,
                            attrQueryBldr.getAttributeQuery(CISales.Document2DerivativeDocument.From));
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selInst = SelectBuilder.get().linkto(CISales.IncomingInvoice2RecievingTicket.ToLink)
                            .instance();
            multi.addSelect(selInst);
            multi.execute();
            while (multi.next()) {
                connectInst =  multi.getSelect(selInst);
            }
        } else {
            connectInst = _deriveds.get(0);
        }
        if (connectInst != null) {
            final Insert insert = new Insert(CISales.ProductionCosting2ProductionReport);
            insert.add(CISales.ProductionCosting2ProductionReport.FromLink, _createdDoc.getInstance());
            insert.add(CISales.ProductionCosting2ProductionReport.ToLink, connectInst);
            insert.execute();
        }

        for (final Instance inst : _deriveds) {
            if (inst.getType().isKindOf(CISales.ProductionCosting)) {
                final Update update = new Update(inst);
                update.add(CISales.ProductionCosting.Status, Status.find(CISales.ProductionCostingStatus.Canceled));
                update.execute();
            }
        }
    }

    /**
     * Edit.
     *
     * @param _parameter Parameter from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final EditedDoc editDoc = editDoc(_parameter);
        updatePositions(_parameter, editDoc);
        return new Return();
    }

    @Override
    public Return executeCalculatorOnScript(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final List<Map<String, Object>> list = new ArrayList<>();
        final List<Calculator> calcList = analyseTable(_parameter, null);
        int i = 0;
        for (final Calculator cal : calcList) {
            final Map<String, Object> map = new HashMap<>();
            _parameter.getParameters().put("eFapsRowSelectedRow", new String[] { "" + i });
            add2Map4UpdateField(_parameter, map, calcList, cal, i == 0);
            list.add(map);
            i++;
        }
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    @Override
    public Return validate(final Parameter _parameter)
        throws EFapsException
    {
        return new Validation()
        {

            @Override
            protected List<IWarning> validate(final Parameter _parameter,
                                              final List<IWarning> _warnings)
                throws EFapsException
            {
                final List<IWarning> ret = super.validate(_parameter, _warnings);
                if (Sales.PRODUCTIONCOSTINGMAXDEV.get() > 0) {
                    final List<Calculator> calcs = analyseTable(_parameter, null);
                    int i = 1;
                    for (final Calculator calc : calcs) {
                        final BigDecimal currentCost = Cost.getCost4Currency(_parameter, calc.getProductInstance(),
                                        getRateCurrencyInstance(_parameter, null));
                        if (currentCost.compareTo(BigDecimal.ZERO) > 0) {
                            final BigDecimal deviation = calc.getNetUnitPrice().subtract(currentCost).setScale(4,
                                            RoundingMode.HALF_UP).divide(currentCost, RoundingMode.HALF_UP).multiply(
                                                            new BigDecimal(100)).abs();
                            if (deviation.compareTo(new BigDecimal(Sales.PRODUCTIONCOSTINGMAXDEV.get())) > 0) {
                                ret.add(new ProductionCostingMaximumDeviationWarning().setPosition(i));
                            }
                        }
                        i++;
                    }
                }
                return ret;
            }
        }.validate(_parameter, this);
    }

    @Override
    public CIType getCIType()
        throws EFapsException
    {
        return CISales.ProductionCosting;
    }

    /**
     * Warning for maximum deviation.
     */
    public static class ProductionCostingMaximumDeviationWarning
        extends AbstractPositionWarning
    {

    }
}
