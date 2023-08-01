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
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;

import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.Calculator;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */

@EFapsUUID("07c31497-62dd-48af-9962-7dc8a20d4fb6")
@EFapsApplication("eFapsApp-Sales")
public abstract class IncomingRetentionCertificate_Base
    extends AbstractDocumentSum
{

    /**
     * Used to store the RetentionDoc in the Context.
     */
    public static final String RETENTIONDOC = IncomingRetentionCertificate.class.getName() + ".RetentiontionDoc";

    /**
     * Used to store the Revision in the Context.
     */
    protected static final String REVISIONKEY = IncomingRetentionCertificate.class.getName() + "RevisionKey";

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
        final Return ret = new Return();
        final BigDecimal crossTotal = parseBigDecimal(_parameter.getParameterValue(
                        CIFormSales.Sales_IncomingRetentionCertificateForm.crossTotal.name));
        if (crossTotal.compareTo(BigDecimal.ZERO) > 0) {
            final CreatedDoc createdDoc = createDoc(_parameter);
            connect2Object(_parameter, createdDoc);
            final File file = createReport(_parameter, createdDoc);
            if (file != null) {
                ret.put(ReturnValues.VALUES, file);
                ret.put(ReturnValues.TRUE, true);
            }
            ret.put(ReturnValues.INSTANCE, createdDoc.getInstance());
            afterCreate(_parameter, createdDoc.getInstance());
        }
        return ret;
    }

    @Override
    protected void add2DocCreate(final Parameter _parameter,
                                 final Insert _insert,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final String seqKey = Sales.INCOMINGRETENTIONCERTIFICATE_REVSEQ.get();
        final NumberGenerator numgen = isUUID(seqKey)
                        ? NumberGenerator.get(UUID.fromString(seqKey))
                        : NumberGenerator.get(seqKey);
        if (numgen != null) {
            final String revision = numgen.getNextVal();
            Context.getThreadContext().setSessionAttribute(IncomingRetentionCertificate.REVISIONKEY, revision);
            _insert.add(CISales.IncomingRetentionCertificate.Revision, revision);
        }
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

    /**
     * Parses the big decimal.
     *
     * @param _value the _value
     * @return the big decimal
     * @throws EFapsException the e faps exception
     */
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

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return with Snipplet
     * @throws EFapsException on error
     */
    public Return showRevisionFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        return getRevisionSequenceFieldValue(_parameter, IncomingRetentionCertificate.REVISIONKEY);
    }

    @Override
    protected void add2QueryBldr(final Parameter _parameter,
                                 final QueryBuilder _queryBldr)
        throws EFapsException
    {
        final Instance contactInst = Instance.get(_parameter.getParameterValue("contact"));
        if (InstanceUtils.isValid(contactInst)) {
            _queryBldr.addWhereAttrEqValue(CIERP.DocumentAbstract.Contact, contactInst);
        } else {
            super.add2QueryBldr(_parameter, _queryBldr);
        }
    }

    @Override
    public CIType getCIType()
        throws EFapsException
    {
        return CISales.IncomingRetentionCertificate;
    }
}
