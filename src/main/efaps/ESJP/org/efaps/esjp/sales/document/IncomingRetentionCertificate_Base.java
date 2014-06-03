/*
 * Copyright 2003 - 2014 The eFaps Team
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

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.List;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.Calculator;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */

@EFapsUUID("07c31497-62dd-48af-9962-7dc8a20d4fb6")
@EFapsRevision("$Rev$")
public abstract class IncomingRetentionCertificate_Base
    extends DocumentSum
{

    /**
     * Used to store the RetentionDoc in the Context.
     */
    public static final String RETENTIONDOC = IncomingRetentionCertificate.class.getName() + ".RetentiontionDoc";

    /**
     * Executed from a Command execute vent to create a new Incoming PerceptionCertificate.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final BigDecimal crossTotal = parseBigDecimal(_parameter
                        .getParameterValue(CIFormSales.Sales_IncomingRetentionCertificateForm.crossTotal.name));
        if (crossTotal.compareTo(BigDecimal.ZERO) > 0) {
            createDoc(_parameter);
        }
        return new Return();
    }


    /**
     * Executed from a Command execute event to edit.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final BigDecimal crossTotal = parseBigDecimal(_parameter
                        .getParameterValue(CIFormSales.Sales_IncomingRetentionCertificateForm.crossTotal.name));
        if (crossTotal.compareTo(BigDecimal.ZERO) > 0) {
            editDoc(_parameter);
        }
        return new Return();
    }

    /**
     * Executed from a Command execute vent to create a new Incoming PerceptionCertificate.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @param _createdDoc as CreatedDoc with values.
     * @throws EFapsException on error
     */
    public void update4Connect(final Parameter _parameter,
                               final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final Instance retentionDoc = (Instance) _createdDoc.getValue(RETENTIONDOC);

        if (retentionDoc != null && retentionDoc.isValid()) {
            for (int i = 0; i < _createdDoc.getPositions().size(); i++) {
                if (CISales.Invoice.getType().equals(_createdDoc.getPositions().get(i).getType())) {
                    final Insert relInsert = new Insert(CISales.IncomingRetentionCertificate2Invoice);
                    relInsert.add(CISales.IncomingRetentionCertificate2Invoice.FromLink, retentionDoc);
                    relInsert.add(CISales.IncomingRetentionCertificate2Invoice.ToLink,
                                    _createdDoc.getPositions().get(i));
                    relInsert.execute();
                }
            }
        }
    }

    @Override
    protected BigDecimal getCrossTotal(final Parameter _parameter,
                                       final List<Calculator> _calcList)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        if (_calcList.isEmpty()) {
            ret = ret.add(parseBigDecimal(_parameter
                            .getParameterValue(CIFormSales.Sales_IncomingRetentionCertificateForm.crossTotal.name)));
        } else {
            ret = super.getCrossTotal(_parameter, _calcList);
        }

        return ret;
    }

    protected BigDecimal parseBigDecimal(final String _value)
        throws EFapsException
    {
        final DecimalFormat formater = NumberFormatter.get().getFormatter();
        BigDecimal ret = BigDecimal.ZERO;
        try {
            if (_value != null && !_value.isEmpty()) {
                ret = (BigDecimal) formater.parse(_value);
            }
        } catch (final ParseException p) {
            throw new EFapsException(IncomingRetentionCertificate.class, "RateCrossTotal.ParseException", p);
        }
        return ret;
    }
}
