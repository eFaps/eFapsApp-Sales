package org.efaps.esjp.sales.document;

import java.util.UUID;

import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.Recur.Frequency;

import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Insert;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

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


    public void run() {
        final Recur recur = new Recur.Builder()
                        .frequency(Frequency.MONTHLY)
                        .build();
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
