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

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.Calculator;
import org.efaps.esjp.sales.Channel;
import org.efaps.esjp.sales.document.AbstractDocumentTax_Base.DocTaxInfo;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("f6743e59-52f3-491e-8183-588982c787dc")
@EFapsApplication("eFapsApp-Sales")
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
        final EditedDoc editedDoc = editDoc(_parameter);
        updatePositions(_parameter, editedDoc);
        updateConnection2Object(_parameter, editedDoc);
        createUpdateTaxDoc(_parameter, editedDoc, true);
        return new Return();
    }

    @Override
    protected void add2DocCreate(final Parameter _parameter,
                                 final Insert _insert,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final String seqKey = Sales.INCOMINGPROFSERVREC_REVSEQ.get();
        final NumberGenerator numgen = isUUID(seqKey)
                        ? NumberGenerator.get(UUID.fromString(seqKey))
                        : NumberGenerator.get(seqKey);
        if (numgen != null) {
            final String revision = numgen.getNextVal();
            Context.getThreadContext().setSessionAttribute(IncomingProfServReceipt.REVISIONKEY, revision);
            _insert.add(CISales.IncomingProfServReceipt.Revision, revision);
        }
    }

    @Override
    protected void add2UpdateMap4Contact(final Parameter _parameter,
                                         final Instance _contactInstance,
                                         final Map<String, Object> _map)
        throws EFapsException
    {
        super.add2UpdateMap4Contact(_parameter, _contactInstance, _map);
        if (Sales.INCOMINGPROFSERVREC_ACTIVATECONDITION.get()) {
            InterfaceUtils.appendScript4FieldUpdate(_map,
                            new Channel().getConditionJs(_parameter, _contactInstance,
                                            CISales.ChannelPurchaseCondition2Contact));
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
        if ("false".equalsIgnoreCase(ParameterUtil.getParameterValue(_parameter,
                        CIFormSales.Sales_IncomingProfServReceiptForm.headingTaxDoc.name,
                        CIFormSales.Sales_IncomingProfServReceiptForm.headingTaxDoc4Edit.name,
                        CIFormSales.Sales_IncomingProfServReceiptForm.headingTaxDoc4EditCollapsed.name))) {
            final boolean isRetention = "true".equalsIgnoreCase(_parameter
                            .getParameterValue(CIFormSales.Sales_IncomingProfServReceiptForm.retentionCheckbox.name));
            final String retentionValueStr = _parameter
                            .getParameterValue(CIFormSales.Sales_IncomingProfServReceiptForm.retentionValue.name);
            if (retentionValueStr != null && !retentionValueStr.isEmpty()) {
                final DecimalFormat formatter = NumberFormatter.get().getFormatter();
                try {
                    final BigDecimal retention = (BigDecimal) formatter.parse(retentionValueStr);
                    final IncomingProfServRetention doc = new IncomingProfServRetention();
                    _createdDoc.addValue(AbstractDocumentTax_Base.TAXAMOUNTVALUE, retention);
                    doc.createUpdate4Doc(_parameter, _createdDoc);
                } catch (final ParseException p) {
                    throw new EFapsException(IncomingInvoice.class, "Perception.ParseException", p);
                }
            }

            final boolean isInsurance = "true".equalsIgnoreCase(_parameter
                            .getParameterValue(CIFormSales.Sales_IncomingProfServReceiptForm.insuranceCheckbox.name));
            if (isInsurance) {
                final DecimalFormat formatter = NumberFormatter.get().getFormatter();
                try {
                    final String insuranceValueStr = _parameter.getParameterValue(
                                    CIFormSales.Sales_IncomingProfServReceiptForm.insuranceValue.name);
                    final BigDecimal detraction;
                    if (insuranceValueStr != null && !insuranceValueStr.isEmpty()) {
                        detraction = (BigDecimal) formatter.parse(insuranceValueStr);
                    } else {
                        detraction = BigDecimal.ZERO;
                    }
                    final IncomingProfServInsurance doc = new IncomingProfServInsurance();
                    _createdDoc.addValue(AbstractDocumentTax_Base.TAXAMOUNTVALUE, detraction);
                    doc.createUpdate4Doc(_parameter, _createdDoc);
                } catch (final ParseException p) {
                    throw new EFapsException(IncomingInvoice.class, "Perception.ParseException", p);
                }
            }
            final DocTaxInfo docTaxInfo = AbstractDocumentTax_Base.getDocTaxInfo(_parameter, _createdDoc.getInstance());
            if (!isInsurance) {
                docTaxInfo.cleanByType(CISales.IncomingProfServInsurance);
            }
            if (!isRetention) {
                docTaxInfo.cleanByType(CISales.IncomingProfServRetention);
            }
        } else {
            final DocTaxInfo docTaxInfo = AbstractDocumentTax_Base.getDocTaxInfo(_parameter, _createdDoc.getInstance());
            docTaxInfo.clean4TaxDocInst(null);
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
        return getRevisionSequenceFieldValue(_parameter, IncomingProfServReceipt.REVISIONKEY);
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
            retVal.put(ReturnValues.VALUES, doctaxInfo.getPercent(CISales.IncomingProfServRetention));
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
            retVal.put(ReturnValues.VALUES, doctaxInfo.getPercent(CISales.IncomingProfServInsurance));
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
            retVal.put(ReturnValues.VALUES, doctaxInfo.getTaxAmount(CISales.IncomingProfServRetention));
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
            retVal.put(ReturnValues.VALUES, doctaxInfo.getTaxAmount(CISales.IncomingProfServInsurance));
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
        final List<Map<String, Object>> list = new ArrayList<>();
        final Map<String, Object> map = new HashMap<>();

        final List<Calculator> calcList = analyseTable(_parameter, null);

        if (calcList.size() > 0) {
            add2Map4UpdateField(_parameter, map, calcList, null, true);
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
        final List<Map<String, Object>> list = new ArrayList<>();
        final Map<String, Object> map = new HashMap<>();

        final List<Calculator> calcList = analyseTable(_parameter, null);

        if (calcList.size() > 0) {
            add2Map4UpdateField(_parameter, map, calcList, null, true);
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        }
        return retVal;
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
                                       final Calculator _cal,
                                       final boolean _includeTotal)
        throws EFapsException
    {
        super.add2Map4UpdateField(_parameter, _map, _calcList, _cal, _includeTotal);

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
                _map.put(CIFormSales.Sales_IncomingProfServReceiptForm.insuranceValue.name, insuranceStr);
            } catch (final ParseException e) {
                IncomingProfServReceipt_Base.LOG.error("Catched parsing error", e);
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
                IncomingProfServReceipt_Base.LOG.error("Catched parsing error", e);
            }
        }
    }

}
