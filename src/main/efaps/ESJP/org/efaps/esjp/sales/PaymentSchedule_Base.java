package org.efaps.esjp.sales;

import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
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
public class PaymentSchedule_Base extends EventSchedule
{

    /**
     * Method for create a new PaymentSchedule.
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

    protected CreateSchedule createSchedule(Parameter _parameter)
        throws EFapsException
    {
        // Sales-Configuration
        final Instance baseCurrInst = SystemConfiguration.get(
                        UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f")).getLink("CurrencyBase");

        final Insert insertPaySche = new Insert(CISales.PaymentSchedule);
        insertPaySche.add(CISales.PaymentSchedule.Date, _parameter.getParameterValue("date"));
        insertPaySche.add(CISales.PaymentSchedule.Name, _parameter.getParameterValue("name4create"));
        insertPaySche.add(CISales.PaymentSchedule.Total, getTotalFmtStr(getTotal(_parameter)));
        insertPaySche.add(CISales.PaymentSchedule.CurrencyId, baseCurrInst.getId());

        insertPaySche.execute();

        CreateSchedule createSchedule = new CreateSchedule(insertPaySche.getInstance());
        createPositions(_parameter, createSchedule);

        return createSchedule;

    }

    protected void createPositions(Parameter _parameter,
                                 CreateSchedule createSchedule)
        throws EFapsException
    {
        String oids[] = _parameter.getParameterValues("document");
        Integer i = 0;
        for(String oid : oids)
        {
            Instance instDoc = Instance.get(oid);
            PrintQuery printDoc = new PrintQuery(instDoc);
            printDoc.addAttribute(CISales.DocumentSumAbstract.CrossTotal);
            printDoc.addAttribute(CISales.DocumentAbstract.Note);
            printDoc.execute();

            Insert insertPayShePos = new Insert(CISales.PaymentSchedulePosition);
            insertPayShePos.add(CISales.PaymentSchedulePosition.PaymentSchedule, createSchedule.getInstance().getId());
            insertPayShePos.add(CISales.PaymentSchedulePosition.Document, instDoc.getId());
            insertPayShePos.add(CISales.PaymentSchedulePosition.PositionNumber, i);
            insertPayShePos.add(CISales.PaymentSchedulePosition.DocumentDesc, _parameter.getParameterValues("documentDesc")[i]);
            insertPayShePos.add(CISales.PaymentSchedulePosition.NetPrice, printDoc.getAttribute(CISales.DocumentSumAbstract.CrossTotal));
            insertPayShePos.execute();
            i++;
        }
    }

}
