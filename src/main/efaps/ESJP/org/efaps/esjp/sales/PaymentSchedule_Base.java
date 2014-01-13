package org.efaps.esjp.sales;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.SalesSettings;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: PaymentSchedule_Base.java $
 */
@EFapsUUID("806c701d-ca2a-4e71-b6b1-7777a77299e4")
@EFapsRevision("$Rev: 1$")
public class PaymentSchedule_Base
    extends EventSchedule
{

    /**
     * Method for create a new PaymentSchedule.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = createDoc(_parameter);
        createPositions(_parameter, createdDoc);

        return new Return();
    }

    @Override
    protected void add2DocCreate(final Parameter _parameter,
                                 final Insert _insert,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final Instance baseCurrInst = Sales.getSysConfig().getLink(SalesSettings.CURRENCYBASE);

        _insert.add(CISales.PaymentSchedule.Total, getTotalFmtStr(getTotal(_parameter)));
        _insert.add(CISales.PaymentSchedule.CurrencyId, baseCurrInst);
    }

    @Override
    protected void createPositions(final Parameter _parameter,
                                   final CreatedDoc createSchedule)
        throws EFapsException
    {
        final String oids[] = _parameter.getParameterValues("document");
        Integer i = 0;
        for (final String oid : oids) {
            final Instance instDoc = Instance.get(oid);
            final PrintQuery printDoc = new PrintQuery(instDoc);
            printDoc.addAttribute(CISales.DocumentSumAbstract.CrossTotal);
            printDoc.addAttribute(CISales.DocumentAbstract.Note);
            printDoc.execute();

            final Insert insertPayShePos = new Insert(CISales.PaymentSchedulePosition);
            insertPayShePos.add(CISales.PaymentSchedulePosition.PaymentSchedule, createSchedule.getInstance().getId());
            insertPayShePos.add(CISales.PaymentSchedulePosition.Document, instDoc.getId());
            insertPayShePos.add(CISales.PaymentSchedulePosition.PositionNumber, i);
            if (_parameter.getParameterValues(CISales.PaymentSchedulePosition.DocumentDesc.name) != null) {
                insertPayShePos.add(CISales.PaymentSchedulePosition.DocumentDesc,
                                _parameter.getParameterValues(CISales.PaymentSchedulePosition.DocumentDesc.name)[i]);
            } else {
                insertPayShePos.add(CISales.PaymentSchedulePosition.DocumentDesc,
                                DBProperties.getProperty("org.efaps.esjp.sales.PaymentSchedule.defaultDescription"));
            }
            insertPayShePos.add(CISales.PaymentSchedulePosition.NetPrice,
                            printDoc.getAttribute(CISales.DocumentSumAbstract.CrossTotal));
            insertPayShePos.execute();
            i++;
        }
    }

    public Return createSchedule4SelectedDocs(final Parameter _parameter)
        throws EFapsException
    {
        final String[] others = (String[]) Context.getThreadContext().getSessionAttribute("internalOIDs");
        if (others != null) {
            @SuppressWarnings("unchecked")
            final Map<String, String[]> parameters = (Map<String, String[]>) _parameter
                            .get(ParameterValues.PARAMETERS);
            parameters.put("document", others);
            _parameter.put(ParameterValues.PARAMETERS, parameters);
            createDoc(_parameter);
        }
        return new Return();
    }

    public Return deleteTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instPos = _parameter.getInstance();
        final SelectBuilder selectPaySche = new SelectBuilder().linkto(CISales.PaymentSchedulePosition.PaymentSchedule)
                        .oid();

        final PrintQuery printPos = new PrintQuery(instPos);
        printPos.addAttribute(CISales.PaymentSchedulePosition.NetPrice);
        printPos.addSelect(selectPaySche);

        BigDecimal posNetPrice = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        if (printPos.execute()) {
            posNetPrice = printPos.<BigDecimal>getAttribute(CISales.PaymentSchedulePosition.NetPrice);
            final Instance instPaySche = Instance.get(printPos.<String>getSelect(selectPaySche));
            final PrintQuery printPaySche = new PrintQuery(instPaySche);
            printPaySche.addAttribute(CISales.PaymentSchedule.Total);
            if (printPaySche.execute()) {
                total = printPaySche.<BigDecimal>getAttribute(CISales.PaymentSchedule.Total);
                final Update updatePaySche = new Update(printPaySche.getCurrentInstance());
                updatePaySche.add(CISales.PaymentSchedule.Total,
                                total.subtract(posNetPrice).setScale(2, RoundingMode.HALF_UP));
                updatePaySche.execute();
            }
        }
        return new Return();
    }

    public Return getNotScheduledDocuments(final Parameter _parameter)
        throws EFapsException
    {
        return new MultiPrint()
        {
            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.PaymentSchedule);
                attrQueryBldr.addWhereAttrEqValue(CISales.PaymentSchedule.Status,
                                Status.find(CISales.PaymentScheduleStatus.Canceled));
                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.PaymentSchedule.ID);

                final QueryBuilder attrQueryBldr2 = new QueryBuilder(CISales.PaymentSchedulePosition);
                attrQueryBldr2.addWhereAttrNotInQuery(CISales.PaymentSchedulePosition.PaymentSchedule, attrQuery);
                final AttributeQuery attrQuery2 = attrQueryBldr2
                                .getAttributeQuery(CISales.PaymentSchedulePosition.Document);

                _queryBldr.addWhereAttrNotInQuery(CISales.DocumentAbstract.ID, attrQuery2);
            }
        }.execute(_parameter);
    }

    public Return getScheduledDocuments(final Parameter _parameter)
        throws EFapsException
    {
        return new MultiPrint()
        {
            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.PaymentSchedule);
                attrQueryBldr.addWhereAttrEqValue(CISales.PaymentSchedule.Status,
                                Status.find(CISales.PaymentScheduleStatus.Open));
                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.PaymentSchedule.ID);

                final QueryBuilder attrQueryBldr2 = new QueryBuilder(CISales.PaymentSchedulePosition);
                attrQueryBldr2.addWhereAttrInQuery(CISales.PaymentSchedulePosition.PaymentSchedule, attrQuery);
                final AttributeQuery attrQuery2 = attrQueryBldr2
                                .getAttributeQuery(CISales.PaymentSchedulePosition.Document);

                _queryBldr.addWhereAttrInQuery(CISales.DocumentAbstract.ID, attrQuery2);
            }
        }.execute(_parameter);
    }

    @Override
    protected void add2QueryBldr4AutoCompleteScheduledDoc(final Parameter _parameter,
                                                          final QueryBuilder _queryBldr)
        throws EFapsException
    {
        final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.PaymentSchedule);
        attrQueryBldr.addWhereAttrEqValue(CISales.PaymentSchedule.Status,
                        Status.find(CISales.PaymentScheduleStatus.Canceled));
        final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.PaymentSchedule.ID);

        final QueryBuilder attrQueryBldr2 = new QueryBuilder(CISales.PaymentSchedulePosition);
        attrQueryBldr2.addWhereAttrNotInQuery(CISales.PaymentSchedulePosition.PaymentSchedule, attrQuery);
        final AttributeQuery attrQuery2 = attrQueryBldr2
                        .getAttributeQuery(CISales.PaymentSchedulePosition.Document);

        _queryBldr.addWhereAttrNotInQuery(CISales.DocumentAbstract.ID, attrQuery2);
    }

    @Override
    public Return updateFields4Contact(final Parameter _parameter)
        throws EFapsException
    {
        final String value = _parameter.getParameterValue("contactAutoComplete");
        final Instance contact = Instance.get(_parameter.getParameterValue("contact"));
        if (value != null && value.length() > 0 && contact.isValid()) {
            Context.getThreadContext().setSessionAttribute(PaymentSchedule.CONTACT_SESSIONKEY, contact);
        } else {
            Context.getThreadContext().removeSessionAttribute(PaymentSchedule.CONTACT_SESSIONKEY);
            _parameter.getParameters().put("contact", new String[]{ "" });
        }
        return super.updateFields4Contact(_parameter);
    }

    public Return removeSession4Schedule(final Parameter _parameter)
        throws EFapsException
    {
        Context.getThreadContext().removeSessionAttribute(PaymentSchedule.CONTACT_SESSIONKEY);
        return new Return();
    }
}
