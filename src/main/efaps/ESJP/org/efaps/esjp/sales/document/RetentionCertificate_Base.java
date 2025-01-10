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
import java.util.HashMap;
import java.util.Map;

import org.efaps.admin.datamodel.Attribute;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.esjp.sales.payment.DocumentUpdate;
import org.efaps.esjp.sales.report.RetentionCertificateReport;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: RetentionCertificate_Base.java 14072 2014-09-19 17:33:57Z
 *          m.aranya@moxter.net $
 */
@EFapsUUID("02d5a390-516e-43d4-8d46-ca9c6599146a")
@EFapsApplication("eFapsApp-Sales")
public abstract class RetentionCertificate_Base
    extends AbstractSumOnlyDocument
{

    /**
     * Logging instance used to give logging information of this class.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(RetentionCertificate_Base.class);

    /**
     * Key used for temp caching.
     */
    protected static final String REQKEY = RetentionCertificate.class.getName();

    /**
     * Method for create a new Quotation.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        createDoc(_parameter);
        return new Return();
    }

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
                final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.RetentionCertificate2PaymentRetentionOut);
                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(
                                CISales.RetentionCertificate2PaymentRetentionOut.ToLink);
                _queryBldr.addWhereAttrNotInQuery(CIERP.DocumentAbstract.ID, attrQuery);

                final QueryBuilder attrQueryBldr2 = new QueryBuilder(CISales.RetentionCertificate);
                attrQueryBldr2.addWhereAttrEqValue(CISales.RetentionCertificate.ID, _parameter.getInstance());
                final AttributeQuery attrQuery2 = attrQueryBldr2
                                .getAttributeQuery(CISales.RetentionCertificate.Contact);

                final QueryBuilder attrQueryBldr3 = new QueryBuilder(CISales.PaymentRetentionOut);
                attrQueryBldr3.addWhereAttrInQuery(CISales.PaymentRetentionOut.Contact, attrQuery2);
                final AttributeQuery attrQuery3 = attrQueryBldr3.getAttributeQuery(CISales.PaymentRetentionOut.ID);

                _queryBldr.addWhereAttrInQuery(CIERP.DocumentAbstract.ID, attrQuery3);
            }
        };
        return multi.execute(_parameter);
    }

    /**
     * @param _parameter Parameter as passed from eFaps API.
     * @return new empty Return
     * @throws EFapsException on error
     */
    public Return insertPostTrigger4Rel(final Parameter _parameter)
        throws EFapsException
    {
        final Map<?, ?> values = (HashMap<?, ?>) _parameter.get(ParameterValues.NEW_VALUES);
        final Attribute attr = _parameter.getInstance().getType().getAttribute("FromLink");
        updateSum(_parameter, Instance.get(CISales.RetentionCertificate.getType(),
                        (Long) ((Object[]) values.get(attr))[0]));
        return new Return();
    }

    /**
     * @param _parameter Parameter as passed from eFaps API.
     * @return new empty Return
     * @throws EFapsException on error
     */
    public Return updatePostTrigger4Rel(final Parameter _parameter)
        throws EFapsException
    {
        final Map<?, ?> values = (HashMap<?, ?>) _parameter.get(ParameterValues.NEW_VALUES);
        final Attribute attr = _parameter.getInstance().getType().getAttribute("FromLink");
        updateSum(_parameter, Instance.get(CISales.RetentionCertificate.getType(),
                        (Long) ((Object[]) values.get(attr))[0]));
        return new Return();
    }

    /**
     * @param _parameter Parameter as passed from eFaps API.
     * @return new empty Return
     * @throws EFapsException on error
     */
    public Return deletePreTrigger4Rel(final Parameter _parameter)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_parameter.getInstance());
        final SelectBuilder selCInst = SelectBuilder.get()
                        .linkto(CISales.RetentionCertificate2PaymentRetentionOut.FromLink)
                        .instance();
        print.addSelect(selCInst);
        print.execute();
        Context.getThreadContext().setRequestAttribute(REQKEY, print.getSelect(selCInst));
        return new Return();
    }

    /**
     * @param _parameter Parameter as passed from eFaps API.
     * @return new empty Return
     * @throws EFapsException on error
     */
    public Return deletePostTrigger4Rel(final Parameter _parameter)
        throws EFapsException
    {
        updateSum(_parameter, (Instance) Context.getThreadContext().getRequestAttribute(REQKEY));
        return new Return();
    }

    /**
     * @param _parameter Parameter as passed from eFaps API.
     * @param _certInst instance of teh certificate to be updated
     * @throws EFapsException on error
     */
    protected void updateSum(final Parameter _parameter,
                             final Instance _certInst)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CISales.RetentionCertificate2PaymentRetentionOut);
        queryBldr.addWhereAttrEqValue(CISales.RetentionCertificate2PaymentRetentionOut.FromLink, _certInst);
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder sel = SelectBuilder.get().linkto(CISales.RetentionCertificate2PaymentRetentionOut.ToLink)
                        .attribute(CISales.PaymentRetentionOut.Amount);
        multi.addSelect(sel);
        multi.execute();
        BigDecimal total = BigDecimal.ZERO;

        while (multi.next()) {
            total = total.add(multi.<BigDecimal>getSelect(sel));
        }
        if (_certInst != null && _certInst.isValid()) {
            final Update update = new Update(_certInst);
            update.add(CISales.RetentionCertificate.CrossTotal, total);
            update.add(CISales.RetentionCertificate.RateCrossTotal, total);
            update.execute();
        }
    }

    /**
     * Fieldvalue for the Detail of the RetentionCertificate.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @throws EFapsException on error
     * @return Return containing html snipplet
     */
    public Return getDetailFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        return new RetentionCertificateReport().generateReport(_parameter);
    }

    /**
     * @param _parameter Parameter as passed from eFaps API.
     * @return new empty Return
     * @throws EFapsException on error
     */
    public Return updateRelatedDocuments(final Parameter _parameter)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CISales.RetentionCertificate2PaymentRetentionOut);
        queryBldr.addWhereAttrEqValue(CISales.RetentionCertificate2PaymentRetentionOut.FromLink, _parameter.getInstance());
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder sel = SelectBuilder.get().linkto(CISales.RetentionCertificate2PaymentRetentionOut.ToLink)
                        .instance();
        multi.addSelect(sel);
        multi.execute();

        while (multi.next()) {
            final Instance inst = multi.getSelect(sel);
            new DocumentUpdate().updateDocument(_parameter, inst);
        }
        return new Return();
    }



    @Override
    public CIType getCIType()
        throws EFapsException
    {
        return CISales.RetentionCertificate;
    }
}
