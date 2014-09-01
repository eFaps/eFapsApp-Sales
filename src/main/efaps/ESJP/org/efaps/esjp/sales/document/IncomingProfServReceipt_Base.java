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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.Calculator;
import org.efaps.esjp.sales.document.AbstractDocumentTax_Base.DocTaxInfo;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.sales.util.SalesSettings;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("f6743e59-52f3-491e-8183-588982c787dc")
@EFapsRevision("$Rev$")
public abstract class IncomingProfServReceipt_Base
    extends AbstractDocumentSum
{

    /**
     * Used to store the Revision in the Context.
     */
    protected static final String REVISIONKEY = IncomingProfServReceipt.class.getName() + "RevisionKey";

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(IncomingProfServReceipt.class);

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
        connect2Derived(_parameter, createdDoc);
        connect2Object(_parameter, createdDoc);
        createUpdateTaxDoc(_parameter, createdDoc, false);
        return new Return();
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
        createUpdateTaxDoc(_parameter, editDoc, true);
        return new Return();
    }


    @Override
    protected void add2DocCreate(final Parameter _parameter,
                                 final Insert _insert,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final SystemConfiguration config = Sales.getSysConfig();
        final Properties props = config.getAttributeValueAsProperties(SalesSettings.INCOMINGINVOICESEQUENCE);

        final NumberGenerator numgen = NumberGenerator.get(UUID.fromString(props.getProperty("UUID")));
        if (numgen != null) {
            final String revision = numgen.getNextVal();
            Context.getThreadContext().setSessionAttribute(REVISIONKEY, revision);
            _insert.add(CISales.IncomingProfServReceipt.Revision, revision);
        }
    }


    /**
     * @param _parameter Parameter as passed by the efasp API
     * @param _createdDoc created Document
     * @param _isUpdate is it an update or not
     * @throws EFapsException on error
     */
    public void createUpdateTaxDoc(final Parameter _parameter,
                                   final CreatedDoc _createdDoc,
                                   final boolean _isUpdate)
        throws EFapsException
    {
        boolean executed = false;
        if ("false".equalsIgnoreCase(ParameterUtil.getParameterValue(_parameter,
                        CIFormSales.Sales_IncomingProfServReceiptForm.headingTaxDoc.name,
                        CIFormSales.Sales_IncomingProfServReceiptForm.headingTaxDoc4Edit.name,
                        CIFormSales.Sales_IncomingProfServReceiptForm.headingTaxDoc4EditCollapsed.name))) {

            final String perceptionValueStr = _parameter
                            .getParameterValue(CIFormSales.Sales_IncomingProfServReceiptForm.perceptionValue.name);
            if (perceptionValueStr != null && !perceptionValueStr.isEmpty()) {
                final DecimalFormat formatter = NumberFormatter.get().getFormatter();
                try {
                    final BigDecimal perception = (BigDecimal) formatter.parse(perceptionValueStr);
                    final IncomingPerceptionCertificate doc = new IncomingPerceptionCertificate();
                    _createdDoc.addValue(AbstractDocumentTax_Base.TAXAMOUNTVALUE, perception);
                    doc.createUpdate4Doc(_parameter, _createdDoc);
                    executed = true;
                } catch (final ParseException p) {
                    throw new EFapsException(IncomingInvoice.class, "Perception.ParseException", p);
                }
            }

            final boolean isInsurance = "true".equalsIgnoreCase(_parameter
                            .getParameterValue(CIFormSales.Sales_IncomingProfServReceiptForm.insuranceCheckbox.name));
            if (isInsurance) {
                final DecimalFormat formatter = NumberFormatter.get().getFormatter();
                try {
                    final String insuranceValueStr = _parameter
                                    .getParameterValue(CIFormSales.Sales_IncomingProfServReceiptForm.insuranceValue.name);
                    final BigDecimal detraction;
                    if (insuranceValueStr != null && !insuranceValueStr.isEmpty()) {
                        detraction = (BigDecimal) formatter.parse(insuranceValueStr);
                    } else {
                        detraction = BigDecimal.ZERO;
                    }
                    final IncomingDetraction doc = new IncomingDetraction();
                    _createdDoc.addValue(AbstractDocumentTax_Base.TAXAMOUNTVALUE, detraction);

                    doc.createUpdate4Doc(_parameter, _createdDoc);
                    executed = true;
                } catch (final ParseException p) {
                    throw new EFapsException(IncomingInvoice.class, "Perception.ParseException", p);
                }
            }

        }
        if (_createdDoc.getValue(AbstractDocumentTax_Base.TAXAMOUNTVALUE) == null && _isUpdate && !executed) {
            _createdDoc.addValue(AbstractDocumentTax_Base.TAXAMOUNTVALUE, BigDecimal.ZERO);
            new AbstractDocumentTax()
            {

                @Override
                protected Type getType4create4Doc(final Parameter _parameter)
                    throws EFapsException
                {
                    // just any type so that the evaluation will fail
                    return CISales.DeliveryNote.getType();
                }

                @Override
                protected void connectDoc(final Parameter _parameter,
                                          final CreatedDoc _origDoc,
                                          final CreatedDoc _taxDoc)
                    throws EFapsException
                {
                    // not needed
                }
            }.createUpdate4Doc(_parameter, _createdDoc);
        }
    }


    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return with Snipplet
     * @throws EFapsException on error
     */
    public Return showRevisionFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String revision = (String) Context.getThreadContext().getSessionAttribute(
                        IncomingProfServReceipt.REVISIONKEY);
        Context.getThreadContext().setSessionAttribute(IncomingProfServReceipt.REVISIONKEY, null);
        final StringBuilder html = new StringBuilder();
        html.append("<span style=\"text-align: center; width: 98%; font-size:40pt; height: 55px; position:absolute\">")
            .append(revision).append("</span>");
        ret.put(ReturnValues.SNIPLETT, html.toString());
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return value for the checkbox
     * @throws EFapsException on error
     */
    public Return accessCheck4TaxDoc(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final boolean inverse = "true".equalsIgnoreCase(getProperty(_parameter, "Inverse"));

        final DocTaxInfo docTaxInfo = AbstractDocumentTax.getDocTaxInfo(_parameter, _parameter.getInstance());
        final Boolean access = docTaxInfo.isProfServRetention() || docTaxInfo.isProfServInsurance();
        if (!inverse && access || inverse && !access) {
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return value for the checkbox
     * @throws EFapsException on error
     */
    public Return getRetentionCheckBoxValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, AbstractDocumentTax.getDocTaxInfo(_parameter, _parameter.getInstance())
                        .isProfServRetention());
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return value for the checkbox
     * @throws EFapsException on error
     */
    public Return getInsuranceCheckBoxValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, AbstractDocumentTax.getDocTaxInfo(_parameter, _parameter.getInstance())
                        .isProfServInsurance());
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return value for the checkbox
     * @throws EFapsException on error
     */
    public Return getRetentionPercentValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final DocTaxInfo doctaxInfo = AbstractDocumentTax.getDocTaxInfo(_parameter, _parameter.getInstance());
        if (doctaxInfo.isProfServRetention()) {
            retVal.put(ReturnValues.VALUES, doctaxInfo.getPercent());
        }
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return value for the checkbox
     * @throws EFapsException on error
     */
    public Return getInsurancePercentValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final DocTaxInfo doctaxInfo = AbstractDocumentTax.getDocTaxInfo(_parameter, _parameter.getInstance());
        if (doctaxInfo.isProfServInsurance()) {
            retVal.put(ReturnValues.VALUES, doctaxInfo.getPercent());
        }
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return value for the checkbox
     * @throws EFapsException on error
     */
    public Return getRetentionValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final DocTaxInfo doctaxInfo = AbstractDocumentTax.getDocTaxInfo(_parameter, _parameter.getInstance());
        if (doctaxInfo.isProfServRetention()) {
            retVal.put(ReturnValues.VALUES, doctaxInfo.getTaxAmount());
        }
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return value for the checkbox
     * @throws EFapsException on error
     */
    public Return getInsuranceValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final DocTaxInfo doctaxInfo = AbstractDocumentTax.getDocTaxInfo(_parameter, _parameter.getInstance());
        if (doctaxInfo.isProfServInsurance()) {
            retVal.put(ReturnValues.VALUES, doctaxInfo.getTaxAmount());
        }
        return retVal;
    }
    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return containing maplist
     * @throws EFapsException on error
     */
    public Return updateFields4RetentionPercent(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();

        final List<Calculator> calcList = analyseTable(_parameter, null);

        if (calcList.size() > 0) {
            add2Map4UpdateField(_parameter, map, calcList, null);
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        }
        return retVal;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return containing maplist
     * @throws EFapsException on error
     */
    public Return updateFields4InsurancePercent(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();

        final List<Calculator> calcList = analyseTable(_parameter, null);

        if (calcList.size() > 0) {
            add2Map4UpdateField(_parameter, map, calcList, null);
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        }
        return retVal;
    }

    @Override
    public String getTypeName4SysConf(final Parameter _parameter)
        throws EFapsException
    {
        return getType4SysConf(_parameter).getName();
    }

    @Override
    protected Type getType4SysConf(final Parameter _parameter)
        throws EFapsException
    {
        return  getCIType().getType();
    }

    @Override
    public CIType getCIType()
        throws EFapsException
    {
        return CISales.IncomingProfServReceipt;
    }

    @Override
    protected void add2Map4UpdateField(final Parameter _parameter,
                                       final Map<String, Object> _map,
                                       final List<Calculator> _calcList,
                                       final Calculator _cal)
        throws EFapsException
    {
        super.add2Map4UpdateField(_parameter, _map, _calcList, _cal);

        final String insurancePercentStr = _parameter
                        .getParameterValue(CIFormSales.Sales_IncomingProfServReceiptForm.insurancePercent.name);
        if (insurancePercentStr != null && !insurancePercentStr.isEmpty()) {
            final DecimalFormat formatter = NumberFormatter.get().getFormatter();
            try {
                final BigDecimal insurancePercent = (BigDecimal) formatter.parse(insurancePercentStr);
                final BigDecimal crossTotal = getCrossTotal(_parameter, _calcList);
                final BigDecimal detraction = crossTotal.multiply(insurancePercent
                                .setScale(8, BigDecimal.ROUND_HALF_UP)
                                .divide(new BigDecimal(100), BigDecimal.ROUND_HALF_UP));
                final String insuranceStr = NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter))
                                .format(detraction);
                _map.put(CIFormSales.Sales_IncomingProfServReceiptForm.detractionValue.name, insuranceStr);
            } catch (final ParseException e) {
                LOG.error("Catched parsing error", e);
            }
        }
        final String retentionPercentStr = _parameter
                        .getParameterValue(CIFormSales.Sales_IncomingProfServReceiptForm.retentionPercent.name);
        if (retentionPercentStr != null && !retentionPercentStr.isEmpty()) {
            final DecimalFormat formatter = NumberFormatter.get().getFormatter();
            try {
                final BigDecimal retentionPercent = (BigDecimal) formatter.parse(retentionPercentStr);
                final BigDecimal crossTotal = getCrossTotal(_parameter, _calcList);
                final BigDecimal retention = crossTotal.multiply(retentionPercent
                                .setScale(8, BigDecimal.ROUND_HALF_UP)
                                .divide(new BigDecimal(100), BigDecimal.ROUND_HALF_UP));
                final String retentionStr = NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter))
                                .format(retention);
                _map.put(CIFormSales.Sales_IncomingProfServReceiptForm.retentionValue.name, retentionStr);
            } catch (final ParseException e) {
                LOG.error("Catched parsing error", e);
            }
        }
    }

}
