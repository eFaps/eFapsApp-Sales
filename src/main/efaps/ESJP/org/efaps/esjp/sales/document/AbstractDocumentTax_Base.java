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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.field.Field;
import org.efaps.admin.ui.field.Field.Display;
import org.efaps.ci.CIType;
import org.efaps.db.Context;
import org.efaps.db.Delete;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: AbstractDocumentTax_Base.java 13280 2014-07-09 17:50:17Z
 *          jan@moxter.net $
 */
@EFapsUUID("83f2f7db-b2d5-4817-bd4c-abf4a0cffa17")
@EFapsRevision("$Rev: 1$")
public abstract class AbstractDocumentTax_Base
    extends AbstractDocumentSum
{

    /**
     * Used to store the PerceptionValue in the Context.
     */
    public static final String TAXAMOUNTVALUE = AbstractDocumentTax.class.getName() + ".TaxAmountValue";

    /**
     * Used to store the PerceptionValue in the Context.
     */
    public static final String REQKEY4DOCTAXINFO = AbstractDocumentTax.class.getName() + ".RequestKey4DocTaxInfo";

    /**
     * Executed from a Command execute vent to create a new Incoming
     * PerceptionCertificate.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @param _createdDoc as CreatedDoc with values.
     * @return the newly created tax document
     * @throws EFapsException on error
     */
    public CreatedDoc createUpdate4Doc(final Parameter _parameter,
                                       final CreatedDoc _createdDoc)
        throws EFapsException
    {
        CreatedDoc ret = null;
        final Type type = getType4create4Doc(_parameter);
        final DocTaxInfo docTaxInfo = AbstractDocumentTax_Base.getDocTaxInfo(_parameter, _createdDoc.getInstance());

        final BigDecimal value = (BigDecimal) _createdDoc.getValue(AbstractDocumentTax_Base.TAXAMOUNTVALUE);
        if (value.compareTo(BigDecimal.ZERO) > 0) {
            ret = new CreatedDoc();
            final Update update;
            if (docTaxInfo.isValid() && docTaxInfo.isKindOf(type)) {
                update = new Update(docTaxInfo.getTaxDocInstance(type));
            } else {
                update = new Insert(type);
                final PrintQuery print = new PrintQuery(_createdDoc.getInstance());
                print.addAttribute(CISales.DocumentSumAbstract.Date, CISales.DocumentSumAbstract.Contact,
                                CISales.DocumentSumAbstract.Salesperson, CISales.DocumentSumAbstract.Group,
                                CISales.DocumentSumAbstract.Rate, CISales.DocumentSumAbstract.CurrencyId,
                                CISales.DocumentSumAbstract.RateCurrencyId, CISales.DocumentSumAbstract.Name);
                print.executeWithoutAccessCheck();
                update.add(CISales.DocumentSumAbstract.Date, print.getAttribute(CISales.DocumentSumAbstract.Date));
                update.add(CISales.DocumentSumAbstract.Contact,
                                print.getAttribute(CISales.DocumentSumAbstract.Contact));
                update.add(CISales.DocumentSumAbstract.Salesperson,
                                print.getAttribute(CISales.DocumentSumAbstract.Salesperson));
                update.add(CISales.DocumentSumAbstract.Group, print.getAttribute(CISales.DocumentSumAbstract.Group));
                update.add(CISales.DocumentSumAbstract.Rate, print.getAttribute(CISales.DocumentSumAbstract.Rate));
                update.add(CISales.DocumentSumAbstract.CurrencyId,
                                print.getAttribute(CISales.DocumentSumAbstract.CurrencyId));
                update.add(CISales.DocumentSumAbstract.RateCurrencyId,
                                print.getAttribute(CISales.DocumentSumAbstract.RateCurrencyId));
                update.add(CISales.DocumentSumAbstract.Name, print.getAttribute(CISales.DocumentSumAbstract.Name));
                update.add(CISales.DocumentSumAbstract.RateNetTotal, BigDecimal.ZERO);
                update.add(CISales.DocumentSumAbstract.RateDiscountTotal, BigDecimal.ZERO);
                update.add(CISales.DocumentSumAbstract.NetTotal, BigDecimal.ZERO);
                update.add(CISales.DocumentSumAbstract.DiscountTotal, BigDecimal.ZERO);
                add2createUpdate4Doc(_parameter, _createdDoc, update);
            }

            final Object[] rateObj = (Object[]) _createdDoc.getValue(CISales.DocumentSumAbstract.Rate.name);
            final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                            BigDecimal.ROUND_HALF_UP);
            final DecimalFormat totalFrmt = NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter));
            final int scale = totalFrmt.getMaximumFractionDigits();

            update.add(CISales.DocumentSumAbstract.RateCrossTotal,
                            value.setScale(scale, BigDecimal.ROUND_HALF_UP));
            update.add(CISales.DocumentSumAbstract.CrossTotal,
                            value.divide(rate, BigDecimal.ROUND_HALF_UP).setScale(scale, BigDecimal.ROUND_HALF_UP));

            addStatus2create4Doc(_parameter, update, _createdDoc);
            update.execute();

            ret.setInstance(update.getInstance());
            if (update instanceof Insert) {
                connectDoc(_parameter, _createdDoc, ret);
            }
            docTaxInfo.clean4TaxDocInst(update.getInstance());
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API
     * @param _createdDoc created doc
     * @param _update       update to be used
     * @throws EFapsException on error
     */
    protected void add2createUpdate4Doc(final Parameter _parameter,
                                        final CreatedDoc _createdDoc,
                                        final Update _update)
        throws EFapsException
    {

    }

    /**
     * @param _parameter Parameter as passed from the eFaps API
     * @return type for create
     * @throws EFapsException on error
     */
    protected abstract Type getType4create4Doc(final Parameter _parameter)
        throws EFapsException;

    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @param _origDoc the original document
     * @param _taxDoc the tax document
     * @throws EFapsException on error
     */
    protected abstract void connectDoc(final Parameter _parameter,
                                       final CreatedDoc _origDoc,
                                       final CreatedDoc _taxDoc)
        throws EFapsException;

    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @param _insert insert to add to
     * @param _origDoc the original document
     * @throws EFapsException on error
     */
    protected void addStatus2create4Doc(final Parameter _parameter,
                                        final Update _insert,
                                        final CreatedDoc _origDoc)
        throws EFapsException
    {
        _insert.add(CISales.DocumentSumAbstract.StatusAbstract,
                        Status.find(_insert.getInstance().getType().getStatusAttribute().getLink().getUUID(), "Open"));
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst instance of the document
     * @return html field value
     * @throws EFapsException on error
     */
    public static StringBuilder getSmallTaxField4Doc(final Parameter _parameter,
                                                     final Instance _docInst)
        throws EFapsException
    {
        final StringBuilder ret = new StringBuilder();
        final Object uiObject = _parameter.get(ParameterValues.UIOBJECT);
        final DocTaxInfo doctaxInfo = AbstractDocumentTax_Base.getDocTaxInfo(_parameter, _docInst);
        final DecimalFormat formater = NumberFormatter.get().getTwoDigitsFormatter();
        if (uiObject instanceof Field || ((FieldValue) uiObject).getDisplay().equals(Display.EDITABLE)
                        || ((FieldValue) uiObject).getDisplay().equals(Display.HIDDEN)) {
            // TODO configurable
            final String fieldName = "taxDocType" + _docInst.getOid();
            final String id1 = RandomStringUtils.randomAlphanumeric(6);
            final String id2 = RandomStringUtils.randomAlphanumeric(6);
            final String id3 = RandomStringUtils.randomAlphanumeric(6);
            final String id4 = RandomStringUtils.randomAlphanumeric(6);

            ret.append("<input type=\"radio\" value=\"").append("NONE").append("\" name=\"")
                            .append(fieldName).append("\" id=\"").append(id1).append("\"");
            if (!doctaxInfo.isValid()) {
                ret.append(" checked=\"checked\" ");
            }
            ret.append("/><label for=\"").append(id1).append("\">")
                            .append(DBProperties.getProperty(AbstractDocumentTax.class.getName() + ".NoneSmallLabel"))
                            .append("</label>");

            ret.append("<input type=\"radio\" value=\"").append(CISales.IncomingDetraction.getType().getId())
                            .append("\" name=\"").append(fieldName).append("\" id=\"").append(id2).append("\"");
            if (doctaxInfo.isDetraction()) {
                ret.append(" checked=\"checked\" ");
            }
            ret.append("/><label for=\"").append(id2).append("\">")
                            .append(DBProperties.getProperty(AbstractDocumentTax.class.getName()
                                            + ".IncomingDetractionSmallLabel"))
                            .append("</label>");

            ret.append("<input type=\"radio\" value=\"").append(CISales.IncomingRetention.getType().getId())
                            .append("\" name=\"").append(fieldName).append("\" id=\"").append(id3).append("\"");
            if (doctaxInfo.isRetention()) {
                ret.append(" checked=\"checked\" ");
            }
            ret.append("/><label for=\"").append(id3).append("\">")
                            .append(DBProperties.getProperty(AbstractDocumentTax.class.getName()
                                            + ".IncomingRetentionSmallLabel"))
                            .append("</label>");

            ret.append("<input type=\"radio\" value=\"").append(CISales.IncomingPerceptionCertificate.getType().getId())
                            .append("\" name=\"").append(fieldName).append("\" id=\"").append(id4).append("\"");
            if (doctaxInfo.isPerception()) {
                ret.append(" checked=\"checked\" ");
            }
            ret.append("/><label for=\"").append(id4).append("\">")
                            .append(DBProperties.getProperty(AbstractDocumentTax.class.getName()
                                            + ".IncomingPerceptionCertificateSmallLabel")).append("</label>");

            ret.append("<input type=\"text\" name=\"taxDocAmount\" size=\"6\" value=\"")
                            .append(formater.format(doctaxInfo.getTaxAmount()))
                            .append("\">");
        } else if (((FieldValue) uiObject).getDisplay().equals(Display.READONLY)
                        || ((FieldValue) uiObject).getDisplay().equals(Display.NONE)) {
            if (!doctaxInfo.isValid()) {
                ret.append(DBProperties.getProperty(AbstractDocumentTax.class.getName() + ".NoneSmallLabel"));
            } else {
                if (doctaxInfo.isDetraction()) {
                    ret.append(DBProperties.getProperty(AbstractDocumentTax.class.getName()
                                + ".IncomingDetractionSmallLabel"))
                        .append(": ").append(formater.format(doctaxInfo.getTaxAmount())).append(" (")
                        .append(formater.format(doctaxInfo.getPercent())).append("%)");
                } else if (doctaxInfo.isRetention()) {
                    ret.append(DBProperties.getProperty(AbstractDocumentTax.class.getName()
                                + ".IncomingRetentionSmallLabel"))
                        .append(": ").append(formater.format(doctaxInfo.getTaxAmount())).append(" (")
                        .append(formater.format(doctaxInfo.getPercent())).append("%)");
                } else if (doctaxInfo.isPerception()) {
                    ret.append(DBProperties.getProperty(AbstractDocumentTax.class.getName()
                                + ".IncomingPerceptionCertificateSmallLabel"))
                        .append(": ").append(formater.format(doctaxInfo.getTaxAmount())).append(" (")
                        .append(formater.format(doctaxInfo.getPercent())).append("%)");
                }

                if (doctaxInfo.isProfServRetention()) {
                    ret.append(DBProperties.getProperty(AbstractDocumentTax.class.getName()
                                    + ".IncomingProfServRetentionSmallLabel"))
                            .append(": ")
                            .append(formater.format(doctaxInfo.getTaxAmount(CISales.IncomingProfServRetention)))
                            .append(" (")
                            .append(formater.format(doctaxInfo.getPercent(CISales.IncomingProfServRetention)))
                            .append("%)");
                }
                if (doctaxInfo.isProfServInsurance()) {
                    ret.append(DBProperties.getProperty(AbstractDocumentTax.class.getName()
                                    + ".IncomingProfServInsuranceSmallLabel"))
                        .append(": ")
                        .append(formater.format(doctaxInfo.getTaxAmount(CISales.IncomingProfServInsurance)))
                        .append(" (")
                        .append(formater.format(doctaxInfo.getPercent(CISales.IncomingProfServInsurance)))
                        .append("%)");
                }
            }
        } else {
            ret.append("-");
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst instance of the document
     * @return DocTaxInfo
     * @throws EFapsException on error
     */
    public static DocTaxInfo getDocTaxInfo(final Parameter _parameter,
                                           final Instance _docInst)
        throws EFapsException
    {
        if (!Context.getThreadContext().containsRequestAttribute(AbstractDocumentTax_Base.REQKEY4DOCTAXINFO)) {
            Context.getThreadContext().setRequestAttribute(AbstractDocumentTax_Base.REQKEY4DOCTAXINFO,
                            new HashMap<Instance, DocTaxInfo>());
        }
        @SuppressWarnings("unchecked")
        final Map<Instance, DocTaxInfo> mapping = (Map<Instance, DocTaxInfo>) Context.getThreadContext()
                        .getRequestAttribute(AbstractDocumentTax_Base.REQKEY4DOCTAXINFO);
        if (!mapping.containsKey(_docInst)) {
            mapping.put(_docInst, new DocTaxInfo(_docInst));
        }
        return mapping.get(_docInst);
    }



    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInstances instance of the document
     *
     * @throws EFapsException on error
     */
    public static void evaluateDocTaxInfo(final Parameter _parameter,
                                          final List<Instance> _docInstances)
        throws EFapsException
    {
        if (!Context.getThreadContext().containsRequestAttribute(AbstractDocumentTax_Base.REQKEY4DOCTAXINFO)) {
            Context.getThreadContext().setRequestAttribute(AbstractDocumentTax_Base.REQKEY4DOCTAXINFO,
                            new HashMap<Instance, DocTaxInfo>());
        }
        @SuppressWarnings("unchecked")
        final Map<Instance, DocTaxInfo> mapping = (Map<Instance, DocTaxInfo>) Context.getThreadContext()
                        .getRequestAttribute(AbstractDocumentTax_Base.REQKEY4DOCTAXINFO);
        final List<Instance> instances = new ArrayList<>();
        final List<Instance> validInstances = new ArrayList<>();
        for (final Instance docInstance : _docInstances) {
            if (!mapping.containsKey(docInstance)) {
                instances.add(docInstance);
            }
        }
        if (!instances.isEmpty()) {
            final QueryBuilder queryBldr = new QueryBuilder(CISales.IncomingDocumentTax2Document);
            queryBldr.addWhereAttrEqValue(CISales.IncomingDocumentTax2Document.ToAbstractLink, instances.toArray());
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selDoc = SelectBuilder.get().linkto(
                            CISales.IncomingDocumentTax2Document.ToAbstractLink);
            final SelectBuilder selDocInst = new SelectBuilder(selDoc).instance();
            final SelectBuilder selDocTax = SelectBuilder.get().linkto(
                            CISales.IncomingDocumentTax2Document.FromAbstractLink);
            final SelectBuilder selDocTaxInst = new SelectBuilder(selDocTax).instance();
            final SelectBuilder selDocTaxCrossTotal = new SelectBuilder(selDocTax)
                            .attribute(CISales.DocumentSumAbstract.CrossTotal);
            final SelectBuilder selDocCrossTotal = new SelectBuilder(selDoc)
                            .attribute(CISales.DocumentSumAbstract.CrossTotal);
            multi.addSelect(selDocInst, selDocTaxInst, selDocTaxCrossTotal, selDocCrossTotal);
            multi.execute();
            while (multi.next()) {
                final Instance docInstance = multi.getSelect(selDocInst);
                validInstances.add(docInstance);
                final Instance relInstance = multi.getCurrentInstance();
                final Instance taxDocInstance = multi.getSelect(selDocTaxInst);
                final BigDecimal taxAmount = multi.getSelect(selDocTaxCrossTotal);
                final BigDecimal crossTotal = multi.getSelect(selDocCrossTotal);
                final BigDecimal percent = new BigDecimal(100).setScale(8).divide(crossTotal, BigDecimal.ROUND_HALF_UP)
                               .multiply(taxAmount);
                final DocTaxInfo docTaxInfo;
                if (mapping.containsKey(docInstance)) {
                    docTaxInfo = mapping.get(docInstance);
                } else {
                    docTaxInfo  = new DocTaxInfo(docInstance);
                    mapping.put(docInstance, docTaxInfo);
                }
                docTaxInfo.addRelInstance(relInstance);
                docTaxInfo.addTaxDocInstance(taxDocInstance);
                docTaxInfo.addTaxAmount(taxDocInstance, taxAmount);
                docTaxInfo.addPercent(taxDocInstance, percent);
                docTaxInfo.setInitialized(true);
            }
            for (final Instance inst : CollectionUtils.subtract(instances, validInstances)) {
                final DocTaxInfo docTaxInfo = new DocTaxInfo(inst);
                mapping.put(inst, docTaxInfo);
                docTaxInfo.setInitialized(true);
            }
        }
    }

    /**
     * Class containing info about DocTax.
     */
    public static class DocTaxInfo
    {
        /**
         * Instance of the document.
         */
        private Instance docInstance;

        /**
         * Instance of the DocTax.
         */
        private final List<Instance> taxDocInstances = new ArrayList<>();

        /**
         * Instance of the relation.
         */
        private final List<Instance> relInstances = new ArrayList<>();

        /**
         * initialized or not.
         */
        private boolean initialized = false;

        /**
         * Amount of tax.
         */
        private final Map<Instance, BigDecimal> tax2Amount = new HashMap<>();

        /**
         * Amount of tax.
         */
        private final Map<Instance, BigDecimal> tax2Percent = new HashMap<>();

        /**
         * Amount of the tax payed.
         */
        private BigDecimal paymentAmount = null;

        /**
         * @param _docInst instance of teh docuemnt the info belong to
         */
        public DocTaxInfo(final Instance _docInst)
        {
            this.docInstance = _docInst;
        }

        /**
         * @throws EFapsException on error
         */
        protected void initialize()
            throws EFapsException
        {
            if (!this.initialized && this.docInstance != null && this.docInstance.isValid()) {
                final QueryBuilder queryBldr = new QueryBuilder(CISales.IncomingDocumentTax2Document);
                queryBldr.addWhereAttrEqValue(CISales.IncomingDocumentTax2Document.ToAbstractLink, this.docInstance);
                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder selDoc = SelectBuilder.get().linkto(
                                CISales.IncomingDocumentTax2Document.ToAbstractLink);
                final SelectBuilder selDocTax = SelectBuilder.get().linkto(
                                CISales.IncomingDocumentTax2Document.FromAbstractLink);
                final SelectBuilder selDocTaxInst = new SelectBuilder(selDocTax).instance();
                final SelectBuilder selDocTaxCrossTotal = new SelectBuilder(selDocTax)
                                .attribute(CISales.DocumentSumAbstract.CrossTotal);
                final SelectBuilder selDocCrossTotal = new SelectBuilder(selDoc)
                                .attribute(CISales.DocumentSumAbstract.CrossTotal);
                multi.addSelect(selDocTaxInst, selDocTaxCrossTotal, selDocCrossTotal);
                multi.execute();
                while (multi.next()) {
                    this.relInstances.add(multi.getCurrentInstance());
                    final Instance taxDocInst = multi.<Instance>getSelect(selDocTaxInst);
                    final BigDecimal taxAmount = multi.<BigDecimal>getSelect(selDocTaxCrossTotal);
                    this.taxDocInstances.add(taxDocInst);
                    this.tax2Amount.put(taxDocInst, taxAmount);
                    final BigDecimal crossTotal = multi.getSelect(selDocCrossTotal);
                    final BigDecimal percent = new BigDecimal(100).setScale(8)
                                    .divide(crossTotal, BigDecimal.ROUND_HALF_UP)
                                    .multiply(taxAmount);
                    this.tax2Percent.put(taxDocInst, percent);
                }
                this.initialized = true;
            }
        }

        /**
         * @param _taxDocInst instance to be retained, if null all will be removed
         * @throws EFapsException on error
         */
        public void clean4TaxDocInst(final Instance _taxDocInst)
            throws EFapsException
        {
            if (_taxDocInst == null
                            || _taxDocInst.getType().isKindOf(CISales.IncomingPerceptionCertificate)
                            || _taxDocInst.getType().isKindOf(CISales.IncomingRetention)
                            || _taxDocInst.getType().isKindOf(CISales.IncomingDetraction)) {
                final QueryBuilder queryBldr = new QueryBuilder(CISales.IncomingDocumentTax2Document);
                queryBldr.addWhereAttrEqValue(CISales.IncomingDocumentTax2Document.ToAbstractLink, this.docInstance);
                if (_taxDocInst != null) {
                    queryBldr.addWhereAttrNotEqValue(CISales.IncomingDocumentTax2Document.FromAbstractLink,
                                    _taxDocInst);
                }
                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder selDocTaxInst = SelectBuilder.get().linkto(
                                CISales.IncomingDocumentTax2Document.FromAbstractLink).instance();
                multi.addSelect(selDocTaxInst);
                multi.execute();
                while (multi.next()) {
                    final Instance docTaxInst = multi.getSelect(selDocTaxInst);

                    final Update update = new Update(docTaxInst);
                    update.add(CISales.DocumentSumAbstract.StatusAbstract, Status.find(
                                    docTaxInst.getType().getStatusAttribute().getLink().getUUID(),
                                    "Canceled"));
                    update.executeWithoutAccessCheck();

                    new Delete(multi.getCurrentInstance()).executeWithoutAccessCheck();
                }
            }
        }

        /**
         * @param _ciType tyep to be removed
         * @throws EFapsException on error
         */
        public void cleanByType(final CIType _ciType)
            throws EFapsException
        {
            final QueryBuilder attrQueryBldr = new QueryBuilder(_ciType);

            final QueryBuilder queryBldr = new QueryBuilder(CISales.IncomingDocumentTax2Document);
            queryBldr.addWhereAttrEqValue(CISales.IncomingDocumentTax2Document.ToAbstractLink, this.docInstance);
            queryBldr.addWhereAttrInQuery(CISales.IncomingDocumentTax2Document.FromAbstractLink,
                            attrQueryBldr.getAttributeQuery("ID"));
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selDocTaxInst = SelectBuilder.get().linkto(
                            CISales.IncomingDocumentTax2Document.FromAbstractLink).instance();
            multi.addSelect(selDocTaxInst);
            multi.execute();
            while (multi.next()) {
                final Instance docTaxInst = multi.getSelect(selDocTaxInst);

                final Update update = new Update(docTaxInst);
                update.add(CISales.DocumentSumAbstract.StatusAbstract, Status.find(
                                docTaxInst.getType().getStatusAttribute().getLink().getUUID(),
                                "Canceled"));
                update.executeWithoutAccessCheck();
                new Delete(multi.getCurrentInstance()).executeWithoutAccessCheck();
            }
        }

        /**
         * @param _taxDocInstance instance the percent belongs to
         * @param _percent percent
         */
        public void addPercent(final Instance _taxDocInstance,
                               final BigDecimal _percent)
        {
            this.tax2Percent.put(_taxDocInstance, _percent);
        }

        /**
         * @param _taxDocInstance instance the amount belongs to
         * @param _taxAmount amount
         */
        public void addTaxAmount(final Instance _taxDocInstance,
                                 final BigDecimal _taxAmount)
        {
            this.tax2Amount.put(_taxDocInstance, _taxAmount);
        }

        /**
         * Getter method for the instance variable {@link #perception}.
         *
         * @return value of instance variable {@link #perception}
         * @throws EFapsException on error
         */
        public boolean isPerception()
            throws EFapsException
        {
            initialize();
            return !this.taxDocInstances.isEmpty() && hasType(CISales.IncomingPerceptionCertificate);
        }

        /**
         * @param _type type to be checked for
         * @return true if has thegiven type
         */
        protected boolean hasType(final CIType _type)
        {
            return hasType(_type.getType());
        }

        /**
         * @param _type type to be checked for
         * @return true if has thegiven type
         */
        protected boolean hasType(final Type _type)
        {
            boolean ret = false;
            for (final Instance taxDocInst : this.taxDocInstances) {
                ret = taxDocInst.isValid() && taxDocInst.getType().isKindOf(_type);
                if (ret) {
                    break;
                }
            }
            return ret;
        }

        /**
         * @return true if valid else false
         * @throws EFapsException on error
         */
        public boolean isValid()
            throws EFapsException
        {
            initialize();
            return this.docInstance != null && this.docInstance.isValid() && !this.taxDocInstances.isEmpty();
        }

        /**
         * Getter method for the instance variable {@link #perception}.
         * @param _type type to be checked for
         * @return value of instance variable {@link #perception}
         * @throws EFapsException on error
         */
        public boolean isKindOf(final Type _type)
            throws EFapsException
        {
            initialize();
            return !this.taxDocInstances.isEmpty() && hasType(_type);
        }

        /**
         * Getter method for the instance variable {@link #retention}.
         *
         * @return value of instance variable {@link #retention}
         * @throws EFapsException on error
         */
        public boolean isRetention()
            throws EFapsException
        {
            initialize();
            return !this.taxDocInstances.isEmpty() && hasType(CISales.IncomingRetention);
        }

        /**
         * Getter method for the instance variable {@link #detraction}.
         *
         * @return value of instance variable {@link #detraction}
         * @throws EFapsException on error
         */
        public boolean isDetraction()
            throws EFapsException
        {
            initialize();
            return !this.taxDocInstances.isEmpty() && hasType(CISales.IncomingDetraction);
        }

        /**
         * Getter method for the instance variable {@link #docInstance}.
         *
         * @return value of instance variable {@link #docInstance}
         * @throws EFapsException on error
         */
        public Instance getDocInstance()
            throws EFapsException
        {
            initialize();
            return this.docInstance;
        }

        /**
         * Setter method for instance variable {@link #docInstance}.
         *
         * @param _docInstance value for instance variable {@link #docInstance}
         */
        public void setDocInstance(final Instance _docInstance)
        {
            this.docInstance = _docInstance;
        }

        /**
         * Setter method for instance variable {@link #taxDocInstance}.
         *
         * @param _taxDocInstance value for instance variable
         *            {@link #taxDocInstance}
         */
        public void addTaxDocInstance(final Instance _taxDocInstance)
        {
            this.taxDocInstances.add(_taxDocInstance);
        }

        /**
         * Getter method for the instance variable {@link #taxAmount}.
         *
         * @return value of instance variable {@link #taxAmount}
         * @throws EFapsException on error
         */
        public BigDecimal getTaxAmount()
            throws EFapsException
        {
            initialize();
            return this.tax2Amount.size() == 1 ? this.tax2Amount.values().iterator().next() : BigDecimal.ZERO;
        }

        /**
         * @param _taxDocInst taxdoc Instance the amount is wanted for
         * @return amount
         * @throws EFapsException on error
         */
        public BigDecimal getTaxAmount(final Instance _taxDocInst)
            throws EFapsException
        {
            initialize();
            return this.tax2Amount.get(_taxDocInst);
        }

        /**
         * @param _citype taxdoc type the amount is wanted for
         * @return amount
         * @throws EFapsException on error
         */
        public BigDecimal getTaxAmount(final CIType _citype)
            throws EFapsException
        {
            return getTaxAmount(getTaxDocInstance(_citype));
        }

        /**
         * Getter method for the instance variable {@link #percent}.
         *
         * @return value of instance variable {@link #percent}
         * @throws EFapsException on error
         */
        public BigDecimal getPercent()
            throws EFapsException
        {
            initialize();
            return this.tax2Percent.size() == 1 ? this.tax2Percent.values().iterator().next() : BigDecimal.ZERO;
        }

        /**
         * @param _taxDocInst taxdoc Instance the percent is wanted for
         * @return percent
         * @throws EFapsException on error
         */
        public BigDecimal getPercent(final Instance _taxDocInst)
            throws EFapsException
        {
            initialize();
            return this.tax2Percent.get(_taxDocInst);
        }

        /**
         * @param _citype taxdoc type the percent is wanted for
         * @return percent
         * @throws EFapsException on error
         */
        public BigDecimal getPercent(final CIType _citype) throws EFapsException
        {
            return getPercent(getTaxDocInstance(_citype));
        }

        /**
         * Setter method for instance variable {@link #relInstance}.
         *
         * @param _relInstance value for instance variable {@link #relInstance}
         */
        public void addRelInstance(final Instance _relInstance)
        {
            this.relInstances.add(_relInstance);
        }

        /**
         * Getter method for the instance variable {@link #initialized}.
         *
         * @return value of instance variable {@link #initialized}
         */
        public boolean isInitialized()
        {
            return this.initialized;
        }

        /**
         * Setter method for instance variable {@link #initialized}.
         *
         * @param _initialized value for instance variable {@link #initialized}
         */
        public void setInitialized(final boolean _initialized)
        {
            this.initialized = _initialized;
        }

        /**
         * Getter method for the instance variable {@link #paymentAmount}.
         *
         * @return value of instance variable {@link #paymentAmount}
         * @throws EFapsException on error
         */
        public BigDecimal getPaymentAmount()
            throws EFapsException
        {
            if (this.paymentAmount == null) {
                this.paymentAmount = BigDecimal.ZERO;
                final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.PaymentDetractionOut);
                attrQueryBldr.addType(CISales.PaymentRetentionOut);

                final QueryBuilder queryBldr = new QueryBuilder(CISales.Payment);
                queryBldr.addWhereAttrEqValue(CISales.Payment.CreateDocument, getDocInstance());
                queryBldr.addWhereAttrInQuery(CISales.Payment.TargetDocument,
                                attrQueryBldr.getAttributeQuery(CISales.PaymentDocumentAbstract.ID));
                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder selAmount = SelectBuilder.get()
                                .linkfrom(CISales.TransactionAbstract, CISales.TransactionAbstract.Payment)
                                .attribute(CISales.TransactionAbstract.Amount);
                multi.addSelect(selAmount);
                multi.execute();
                while (multi.next()) {
                    this.paymentAmount = this.paymentAmount.add(multi.<BigDecimal>getSelect(selAmount));
                }
            }
            return this.paymentAmount;
        }

        /**
         * Setter method for instance variable {@link #paymentAmount}.
         *
         * @param _paymentAmount value for instance variable {@link #paymentAmount}
         */
        public void setPaymentAmount(final BigDecimal _paymentAmount)
        {
            this.paymentAmount = _paymentAmount;
        }

        /**
         * @return true if of this type
         * @throws EFapsException on error
         */
        public boolean isProfServRetention()
            throws EFapsException
        {
            initialize();
            return !this.taxDocInstances.isEmpty() && hasType(CISales.IncomingProfServRetention);
        }

        /**
         * @return true if of this type
         * @throws EFapsException on error
         */
        public boolean isProfServInsurance()
            throws EFapsException
        {
            initialize();
            return !this.taxDocInstances.isEmpty() && hasType(CISales.IncomingProfServInsurance);
        }

        /**
         * @param _type type the taxdoc instance is wanted for
         * @return if found instance, else null
         */
        public Instance getTaxDocInstance(final Type _type)
        {
            Instance ret = null;
            for (final Instance taxDocInst : this.taxDocInstances) {
                if (taxDocInst.isValid() && taxDocInst.getType().isKindOf(_type)) {
                    ret = taxDocInst;
                }
            }
            return ret;
        }

        /**
         * @param _citype type the taxdoc instance is wanted for
         * @return if found instance, else null
         */
        public Instance getTaxDocInstance(final CIType _citype)
        {
            return getTaxDocInstance(_citype.getType());
        }

        /**
         * Get the datraction type instance.
         * @return if found instance, else null
         * @throws EFapsException on error
         */
        public Instance getDetractionTypeInst()
            throws EFapsException
        {
            Instance ret = null;
            if (isDetraction()) {
                final Instance taxDocInst = getTaxDocInstance(CISales.IncomingDetraction);
                final PrintQuery print = new PrintQuery(taxDocInst);
                final SelectBuilder sel = SelectBuilder.get().linkto(CISales.IncomingDetraction.ServiceType).instance();
                print.addSelect(sel);
                print.execute();
                ret = print.getSelect(sel);
            }
            return ret;
        }
    }
}
