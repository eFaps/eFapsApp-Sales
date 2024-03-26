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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.stmt.selection.Evaluator;
import org.efaps.eql.EQL;
import org.efaps.eql2.StmtFlag;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.RateInfo;
import org.efaps.esjp.sales.Calculator;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.Recur.Frequency;

@EFapsUUID("b5c1b7e2-d567-4fa7-a1af-5c4f614481e2")
@EFapsApplication("eFapsApp-Sales")
public abstract class Template_Base
{

    public Return createInvoice(final Parameter _parameter)
        throws EFapsException
    {
        final InvoiceTemplate template = new InvoiceTemplate();
        return template.create(_parameter);
    }

    public Return editInvoice(final Parameter _parameter)
        throws EFapsException
    {
        final InvoiceTemplate template = new InvoiceTemplate();
        return template.edit(_parameter);
    }

    public void run()
    {
        new Recur.Builder()
                        .frequency(Frequency.MONTHLY)
                        .build();
    }

    public Return createDocument(final Parameter _parameter)
        throws EFapsException
    {
        final Instance templateInst = _parameter.getInstance();

        final Parameter parameter = ParameterUtil.clone(_parameter);

        final Evaluator eval = EQL.builder()
                .print(templateInst)
                .linkto(CISales.InvoiceTemplate.Contact).oid()
                .attribute(CISales.InvoiceTemplate.Note)
                .stmt().evaluate();
        final String contactOid = eval.get(1);
        final String note = eval.get(2);

        ParameterUtil.setParameterValues(parameter, "date", DateTime.now().toString());
        ParameterUtil.setParameterValues(parameter, "contact", contactOid);
        ParameterUtil.setParameterValues(parameter, "note", note);

        final Evaluator posEval = EQL.builder()
            .print()
                .query(CISales.InvoiceTemplatePosition)
                .where()
                    .attribute(CISales.InvoiceTemplatePosition.InvoiceTemplateLink).eq(templateInst)
            .select()
            .linkto(CISales.InvoiceTemplatePosition.Product).oid()
            .attribute(CISales.InvoiceTemplatePosition.ProductDesc)
            .attribute(CISales.InvoiceTemplatePosition.UoM)
            .orderBy(CISales.InvoiceTemplatePosition.PositionNumber)
            .stmt()
            .evaluate();

        while (posEval.next()) {
            final String productoOid = posEval.get(1);
            final String productDesc = posEval.get(2);
            final Long uoMId = posEval.get(3);
            ParameterUtil.addParameterValues(parameter, "product", productoOid);
            ParameterUtil.addParameterValues(parameter, "productDesc", productDesc);
            ParameterUtil.addParameterValues(parameter, "uoM", uoMId.toString());
        }

        final Invoice invoice = new Invoice() {
            @Override
            protected Instance getRateCurrencyInstance(final Parameter _parameter,
                                                       final CreatedDoc _createdDoc)
                throws EFapsException
            {
                final Evaluator eval = EQL.builder()
                    .with(StmtFlag.REQCACHED)
                    .print(templateInst)
                    .linkto(CISales.InvoiceTemplate.RateCurrencyId).instance()
                    .stmt().evaluate();
                return eval.get(1);
            }

            @Override
            protected Object[] getRateObject(final Parameter _parameter)
                throws EFapsException
            {
                final Evaluator eval = EQL.builder()
                                .with(StmtFlag.REQCACHED)
                                .print(templateInst)
                                .linkto(CISales.InvoiceTemplate.RateCurrencyId).instance()
                                .stmt().evaluate();
                final Instance rateCurrencyInst = eval.get(1);

                final Currency currency = new Currency();
                final RateInfo rateInfo = currency.evaluateRateInfo(_parameter, DateTime.now(), rateCurrencyInst);
                return RateInfo.getRateObject(_parameter, rateInfo, CISales.Invoice.getType().getName());
            }

            @Override
            protected Type getType4DocCreate(final Parameter _parameter)
                throws EFapsException
            {
                return CISales.Invoice.getType();
            }

            @Override
            protected String getDocName4Create(final Parameter _parameter)
                throws EFapsException
            {
                return "How to do that";
            }

            @Override
            public List<Calculator> analyseTable(final Parameter _parameter,
                                                 final Integer _row4priceFromDB)
                throws EFapsException
            {
                return getCalculators4Doc(_parameter, templateInst, Collections.emptyList());
            }
        };
        return invoice.create(parameter);
    }

    public static class InvoiceTemplate
        extends AbstractDocumentSum
    {

        public Return create(final Parameter _parameter)
            throws EFapsException
        {
            final CreatedDoc createdDoc = createDoc(_parameter);
            createPositions(_parameter, createdDoc);
            connect2Derived(_parameter, createdDoc);
            connect2Object(_parameter, createdDoc);
            afterCreate(_parameter, createdDoc.getInstance());
            return new Return();
        }

        public Return edit(final Parameter _parameter)
            throws EFapsException
        {
            final EditedDoc editedDoc = editDoc(_parameter);
            updatePositions(_parameter, editedDoc);
            updateConnection2Object(_parameter, editedDoc);
            return new Return();
        }

        @Override
        protected void addStatus2DocCreate(final Parameter _parameter,
                                           final Insert _insert,
                                           final CreatedDoc _createdDoc)
            throws EFapsException
        {
            _insert.add(CISales.InvoiceTemplate.Status, Status.find(CISales.TemplateStatus.Active));
        }

        @Override
        protected void add2DocCreate(final Parameter _parameter,
                                     final Insert _insert,
                                     final CreatedDoc _createdDoc)
            throws EFapsException
        {
            super.add2DocCreate(_parameter, _insert, _createdDoc);
            _insert.add(CISales.InvoiceTemplate.Date, DateTime.now());
        }

        @Override
        protected String getDocName4Create(final Parameter _parameter)
            throws EFapsException
        {
            return NumberGenerator.get(UUID.fromString(Sales.TEMPLATE_NUMGEN.get())).getNextVal();
        }

        @Override
        public CIType getCIType()
            throws EFapsException
        {
            return CISales.InvoiceTemplate;
        }

        @Override
        protected Type getType4PositionCreate(final Parameter _parameter)
            throws EFapsException
        {
            return CISales.InvoiceTemplatePosition.getType();
        }

        @Override
        protected Type getType4PositionUpdate(final Parameter _parameter)
            throws EFapsException
        {
            return CISales.InvoiceTemplatePosition.getType();
        }
    }
}
