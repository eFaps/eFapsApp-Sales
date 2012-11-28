package org.efaps.esjp.sales;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CISales;
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

        createSchedule(_parameter);
        return new Return();
    }

    protected CreateSchedule createSchedule(final Parameter _parameter)
        throws EFapsException
    {
        // Sales-Configuration
        final Instance baseCurrInst = SystemConfiguration.get(
                        UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f")).getLink("CurrencyBase");

        final Insert insertPaySche = new Insert(CISales.PaymentSchedule);
        insertPaySche.add(CISales.PaymentSchedule.Date, _parameter.getParameterValue("date"));
        insertPaySche.add(CISales.PaymentSchedule.Name, _parameter.getParameterValue("name4create"));
        insertPaySche.add(CISales.PaymentSchedule.Status,
                        Status.find(CISales.PaymentScheduleStatus.uuid, "Open").getId());
        insertPaySche.add(CISales.PaymentSchedule.Total, getTotalFmtStr(getTotal(_parameter)));
        insertPaySche.add(CISales.PaymentSchedule.CurrencyId, baseCurrInst.getId());

        insertPaySche.execute();

        final CreateSchedule createSchedule = new CreateSchedule(insertPaySche.getInstance());
        createPositions(_parameter, createSchedule);

        return createSchedule;

    }

    protected void createPositions(final Parameter _parameter,
                                   final CreateSchedule createSchedule)
        throws EFapsException
    {
        final String oids[] = _parameter.getParameterValues("document");
        Integer i = 0;
        for (final String oid : oids)
        {
            final Instance instDoc = Instance.get(oid);
            final PrintQuery printDoc = new PrintQuery(instDoc);
            printDoc.addAttribute(CISales.DocumentSumAbstract.CrossTotal);
            printDoc.addAttribute(CISales.DocumentAbstract.Note);
            printDoc.execute();

            final Insert insertPayShePos = new Insert(CISales.PaymentSchedulePosition);
            insertPayShePos.add(CISales.PaymentSchedulePosition.PaymentSchedule, createSchedule.getInstance().getId());
            insertPayShePos.add(CISales.PaymentSchedulePosition.Document, instDoc.getId());
            insertPayShePos.add(CISales.PaymentSchedulePosition.PositionNumber, i);
            if (_parameter.getParameterValues("documentDesc") != null) {
                insertPayShePos.add(CISales.PaymentSchedulePosition.DocumentDesc,
                                _parameter.getParameterValues("documentDesc")[i]);
            } else {
                insertPayShePos.add(CISales.PaymentSchedulePosition.DocumentDesc,
                                DBProperties.getProperty("org.efaps.esjp.sales.PaymentSchedule.defaultDescription"));
            }
            insertPayShePos.add(CISales.PaymentSchedulePosition.NetPrice, printDoc.getAttribute(CISales.DocumentSumAbstract.CrossTotal));
            insertPayShePos.execute();
            i++;
        }
    }

    public Return createSchedule4SelectedDocs(final Parameter _parameter)
        throws EFapsException
    {
        final String[] others = (String[]) Context.getThreadContext().getSessionAttribute("internalOIDs");
        if (others != null) {
            final String name = (String) getNameFieldValueUI(_parameter).get(ReturnValues.VALUES);
            @SuppressWarnings("unchecked") final Map<String, String[]> parameters = (Map<String, String[]>) _parameter
                            .get(ParameterValues.PARAMETERS);
            parameters.put("name4create", new String[] { name });
            parameters.put("document", others);
            _parameter.put(ParameterValues.PARAMETERS, parameters);
            createSchedule(_parameter);
        }
        return new Return();
    }

    public Return deleteTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instPos = _parameter.getInstance();
        final SelectBuilder selectPaySche = new SelectBuilder().linkto(CISales.PaymentSchedulePosition.PaymentSchedule).oid();

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
                updatePaySche.add(CISales.PaymentSchedule.Total, total.subtract(posNetPrice).setScale(2, RoundingMode.HALF_UP));
                updatePaySche.execute();
            }
        }
        return new Return();
    }

}
