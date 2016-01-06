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

import java.util.List;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.ui.html.Table;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("8f2539fd-4e71-4f5a-bab2-6329b3dcebbf")
@EFapsApplication("eFapsApp-Sales")
public abstract class TransactionDocument_Base
    extends AbstractProductDocument
{

    /**
     * Access check4 document shadow.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return accessCheck4DocumentShadow(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.TransactionDocumentShadowAbstract);
        attrQueryBldr.addWhereAttrNotEqValue(CISales.TransactionDocumentShadowAbstract.StatusAbstract,
                        Status.find(CISales.TransactionDocumentShadowInStatus.Canceled),
                        Status.find(CISales.TransactionDocumentShadowOutStatus.Canceled));

        final QueryBuilder queryBldr = new QueryBuilder(CIERP.Document2DocumentAbstract);
        queryBldr.addWhereAttrEqValue(CIERP.Document2DocumentAbstract.FromAbstractLink, _parameter.getInstance());
        queryBldr.addWhereAttrInQuery(CIERP.Document2DocumentAbstract.ToAbstractLink,
                        attrQueryBldr.getAttributeQuery(CISales.TransactionDocumentShadowAbstract.ID));
        final InstanceQuery query = queryBldr.getQuery();
        if (query.executeWithoutAccessCheck().isEmpty()) {
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    /**
     * Creates the document shadow.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the created doc
     * @throws EFapsException on error
     */
    public CreatedDoc createDocumentShadow(final Parameter _parameter)
        throws EFapsException
    {
        return createDocumentShadow(_parameter, _parameter.getInstance());
    }

    /**
     * Creates the document shadow.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst the doc inst
     * @return the created doc
     * @throws EFapsException on error
     */
    public CreatedDoc createDocumentShadow(final Parameter _parameter,
                                           final Instance _docInst)
        throws EFapsException
    {
        final CreatedDoc ret = new CreatedDoc();
        final PrintQuery print = new PrintQuery(_docInst);
        print.addAttribute(CIERP.DocumentAbstract.Name, CIERP.DocumentAbstract.Date,
                        CIERP.DocumentAbstract.Contact);
        print.execute();

        final Insert insert = new Insert(getType4DocCreate(_parameter));
        insert.add(CIERP.DocumentAbstract.Name, print.<String>getAttribute(CIERP.DocumentAbstract.Name));
        insert.add(CIERP.DocumentAbstract.Date, print.<DateTime>getAttribute(CIERP.DocumentAbstract.Date));
        insert.add(CIERP.DocumentAbstract.Contact, print.<Long>getAttribute(CIERP.DocumentAbstract.Contact));
        addStatus2DocCreate(_parameter, insert, ret);
        add2DocCreate(_parameter, insert, ret);
        insert.execute();

        ret.setInstance(insert.getInstance());

        connect2ProductDocumentType(_parameter, ret);

        final List<Position> positions = getPositions4Document(_parameter, _docInst);
        final int i = 0;
        for (final Position pos : positions) {
            final Insert posIns = new Insert(getType4PositionCreate(_parameter));
            posIns.add(CISales.PositionAbstract.PositionNumber, i + 1);
            posIns.add(CISales.PositionAbstract.DocumentAbstractLink, ret.getInstance());
            posIns.add(CISales.PositionAbstract.UoM, pos.getUoM());
            posIns.add(CISales.PositionAbstract.Quantity, pos.getQuantity());
            final String descr = pos.getDescr();
            posIns.add(CISales.PositionAbstract.ProductDesc, descr);
            posIns.add(CISales.PositionAbstract.Product, pos.getProdInstance());
            posIns.execute();
        }
        createProductTransaction4Document(_parameter, ret.getInstance(),
                        Instance.get(_parameter.getParameterValue("storage")));

        return ret;
    }

    /**
     * Gets the products4 doc shadow field value.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the products4 doc shadow field value
     * @throws EFapsException on error
     */
    public Return getProducts4DocShadowFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Instance docInst = _parameter.getInstance();
        final Return ret = new Return();

        final Table table = new Table();
        final List<Position> positions = getPositions4Document(_parameter, docInst);
        for (final Position pos : positions) {
            table.addRow().addColumn(pos.getQuantity().toString()).addColumn(pos.getUoM().getName())
                            .addColumn(pos.getProdName()).addColumn(pos.getDescr());
        }
        ret.put(ReturnValues.SNIPLETT, table.toHtml());
        return ret;
    }

}
