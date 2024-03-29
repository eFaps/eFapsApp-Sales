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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.IUIValue;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.program.esjp.EFapsApplication;
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
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.erp.RateInfo;
import org.efaps.util.EFapsException;
import org.efaps.util.RandomUtil;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("83f2f7db-b2d5-4817-bd4c-abf4a0cffa17")
@EFapsApplication("eFapsApp-Sales")
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
     * Executed from a Command execute event to create a new Incoming
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
            final Object[] rateObj;
            if (docTaxInfo.isValid() && docTaxInfo.isKindOf(type)) {
                update = new Update(docTaxInfo.getTaxDocInstance(type));

                final PrintQuery print = new PrintQuery(_createdDoc.getInstance());
                print.addAttribute(CISales.DocumentSumAbstract.Rate);
                print.executeWithoutAccessCheck();
                rateObj = print.<Object[]>getAttribute(CISales.DocumentSumAbstract.Rate);
            } else {
                update = new Insert(type);
                final PrintQuery print = new PrintQuery(_createdDoc.getInstance());
                print.addAttribute(CISales.DocumentSumAbstract.Date, CISales.DocumentSumAbstract.DueDate,
                                CISales.DocumentSumAbstract.Contact, CISales.DocumentSumAbstract.Salesperson,
                                CISales.DocumentSumAbstract.Group, CISales.DocumentSumAbstract.Rate,
                                CISales.DocumentSumAbstract.CurrencyId, CISales.DocumentSumAbstract.RateCurrencyId,
                                CISales.DocumentSumAbstract.Name);
                print.executeWithoutAccessCheck();
                rateObj = print.<Object[]>getAttribute(CISales.DocumentSumAbstract.Rate);
                update.add(CISales.DocumentSumAbstract.Date,
                                print.<DateTime>getAttribute(CISales.DocumentSumAbstract.Date));
                update.add(CISales.DocumentSumAbstract.DueDate,
                                print.<DateTime>getAttribute(CISales.DocumentSumAbstract.DueDate));
                update.add(CISales.DocumentSumAbstract.Contact,
                                print.<Long>getAttribute(CISales.DocumentSumAbstract.Contact));
                update.add(CISales.DocumentSumAbstract.Salesperson,
                                print.<Long>getAttribute(CISales.DocumentSumAbstract.Salesperson));
                update.add(CISales.DocumentSumAbstract.Group,
                                print.<Long>getAttribute(CISales.DocumentSumAbstract.Group));
                update.add(CISales.DocumentSumAbstract.Rate, rateObj);
                update.add(CISales.DocumentSumAbstract.CurrencyId,
                                print.<Long>getAttribute(CISales.DocumentSumAbstract.CurrencyId));
                update.add(CISales.DocumentSumAbstract.RateCurrencyId,
                                print.<Long>getAttribute(CISales.DocumentSumAbstract.RateCurrencyId));
                update.add(CISales.DocumentSumAbstract.Name,
                                print.<String>getAttribute(CISales.DocumentSumAbstract.Name));
                update.add(CISales.DocumentSumAbstract.RateNetTotal, BigDecimal.ZERO);
                update.add(CISales.DocumentSumAbstract.RateDiscountTotal, BigDecimal.ZERO);
                update.add(CISales.DocumentSumAbstract.NetTotal, BigDecimal.ZERO);
                update.add(CISales.DocumentSumAbstract.DiscountTotal, BigDecimal.ZERO);
                add2createUpdate4Doc(_parameter, _createdDoc, update);
            }

            final RateInfo rateInfo = RateInfo.getRateInfo(rateObj);
            final BigDecimal crossTotal = Currency.convertToBase(_parameter, value, rateInfo,
                            getTypeName4SysConf(_parameter));

            update.add(CISales.DocumentSumAbstract.RateCrossTotal, value);
            update.add(CISales.DocumentSumAbstract.CrossTotal, crossTotal);

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
    protected abstract Type getType4create4Doc(Parameter _parameter)
        throws EFapsException;

    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @param _origDoc the original document
     * @param _taxDoc the tax document
     * @throws EFapsException on error
     */
    protected abstract void connectDoc(Parameter _parameter,
                                       CreatedDoc _origDoc,
                                       CreatedDoc _taxDoc)
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
        final IUIValue uiObject = (IUIValue) _parameter.get(ParameterValues.UIOBJECT);
        final DocTaxInfo doctaxInfo = AbstractDocumentTax_Base.getDocTaxInfo(_parameter, _docInst);
        final DecimalFormat formater = NumberFormatter.get().getTwoDigitsFormatter();
        if (uiObject instanceof Field ||  uiObject.getDisplay().equals(Display.EDITABLE)
                        || uiObject.getDisplay().equals(Display.HIDDEN)) {
            // TODO configurable
            final String fieldName = "taxDocType" + _docInst.getOid();
            final String id1 = RandomUtil.randomAlphanumeric(6);
            final String id2 = RandomUtil.randomAlphanumeric(6);
            final String id3 = RandomUtil.randomAlphanumeric(6);
            final String id4 = RandomUtil.randomAlphanumeric(6);

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
        } else if (uiObject.getDisplay().equals(Display.READONLY)
                        || uiObject.getDisplay().equals(Display.NONE)) {
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
                final BigDecimal percent = BigDecimal.ZERO.compareTo(crossTotal) == 0 ? BigDecimal.ZERO
                                : new BigDecimal(100).setScale(8).divide(crossTotal, RoundingMode.HALF_UP)
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
         * Amount of tax.
         */
        private final Map<Instance, BigDecimal> tax2RateAmount = new HashMap<>();

        /**
         * Amount of the tax payed.
         */
        private BigDecimal paymentAmount = null;

        /**
         * Amount of the tax payed.
         */
        private String paymentName = null;

        /**
         * Amount of the tax payed.
         */
        private DateTime paymentDate = null;

        /**
         * @param _docInst instance of teh docuemnt the info belong to
         */
        public DocTaxInfo(final Instance _docInst)
        {
            docInstance = _docInst;
        }

        /**
         * @throws EFapsException on error
         */
        protected void initialize()
            throws EFapsException
        {
            if (!initialized && docInstance != null && docInstance.isValid()) {
                final QueryBuilder queryBldr = new QueryBuilder(CISales.IncomingDocumentTax2Document);
                queryBldr.addWhereAttrEqValue(CISales.IncomingDocumentTax2Document.ToAbstractLink, docInstance);
                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder selDoc = SelectBuilder.get().linkto(
                                CISales.IncomingDocumentTax2Document.ToAbstractLink);
                final SelectBuilder selDocTax = SelectBuilder.get().linkto(
                                CISales.IncomingDocumentTax2Document.FromAbstractLink);
                final SelectBuilder selDocTaxInst = new SelectBuilder(selDocTax).instance();
                final SelectBuilder selDocTaxCrossTotal = new SelectBuilder(selDocTax)
                                .attribute(CISales.DocumentSumAbstract.CrossTotal);
                final SelectBuilder selDocTaxRateCrossTotal = new SelectBuilder(selDocTax)
                                .attribute(CISales.DocumentSumAbstract.RateCrossTotal);
                final SelectBuilder selDocCrossTotal = new SelectBuilder(selDoc)
                                .attribute(CISales.DocumentSumAbstract.CrossTotal);
                multi.addSelect(selDocTaxInst, selDocTaxCrossTotal, selDocCrossTotal, selDocTaxRateCrossTotal);
                multi.execute();
                while (multi.next()) {
                    relInstances.add(multi.getCurrentInstance());
                    final Instance taxDocInst = multi.<Instance>getSelect(selDocTaxInst);
                    final BigDecimal taxAmount = multi.<BigDecimal>getSelect(selDocTaxCrossTotal);
                    final BigDecimal taxRateAmount = multi.<BigDecimal>getSelect(selDocTaxRateCrossTotal);
                    taxDocInstances.add(taxDocInst);
                    tax2Amount.put(taxDocInst, taxAmount);
                    tax2RateAmount.put(taxDocInst, taxRateAmount);
                    final BigDecimal crossTotal = multi.getSelect(selDocCrossTotal);
                    final BigDecimal percent = BigDecimal.ZERO.compareTo(crossTotal) == 0 ? BigDecimal.ZERO
                                    : new BigDecimal(100).setScale(8)
                                                    .divide(crossTotal, RoundingMode.HALF_UP)
                                                    .multiply(taxAmount);
                    tax2Percent.put(taxDocInst, percent);
                }
                initialized = true;
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
                queryBldr.addWhereAttrEqValue(CISales.IncomingDocumentTax2Document.ToAbstractLink, docInstance);
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
            queryBldr.addWhereAttrEqValue(CISales.IncomingDocumentTax2Document.ToAbstractLink, docInstance);
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
            tax2Percent.put(_taxDocInstance, _percent);
        }

        /**
         * @param _taxDocInstance instance the amount belongs to
         * @param _taxAmount amount
         */
        public void addTaxAmount(final Instance _taxDocInstance,
                                 final BigDecimal _taxAmount)
        {
            tax2Amount.put(_taxDocInstance, _taxAmount);
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
            return !taxDocInstances.isEmpty() && hasType(CISales.IncomingPerceptionCertificate);
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
            for (final Instance taxDocInst : taxDocInstances) {
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
            return docInstance != null && docInstance.isValid() && !taxDocInstances.isEmpty();
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
            return !taxDocInstances.isEmpty() && hasType(_type);
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
            return !taxDocInstances.isEmpty() && hasType(CISales.IncomingRetention);
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
            return !taxDocInstances.isEmpty() && hasType(CISales.IncomingDetraction);
        }

        /**
         * Getter method for the instance variable {@link #detraction}.
         *
         * @return value of instance variable {@link #detraction}
         * @throws EFapsException on error
         */
        public boolean isDetractionPaid()
            throws EFapsException
        {
            initialize();
            boolean ret = false;
            if (isDetraction()) {
                final Instance taxDocInst = getTaxDocInstance(CISales.IncomingDetraction);
                final PrintQuery print = new PrintQuery(taxDocInst);
                print.addAttribute(CISales.IncomingDetraction.Status);
                print.execute();
                ret = Status.find(CISales.IncomingDetractionStatus.Paid).equals(Status.get(print.<Long>getAttribute(
                                CISales.IncomingDetraction.Status)));
            }
            return ret;
        }

        /**
         * Getter method for the instance variable {@link #retention}.
         *
         * @return value of instance variable {@link #retention}
         * @throws EFapsException on error
         */
        public boolean isPercetion()
            throws EFapsException
        {
            initialize();
            return !taxDocInstances.isEmpty() && hasType(CISales.IncomingPerceptionCertificate);
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
            return docInstance;
        }

        /**
         * Setter method for instance variable {@link #docInstance}.
         *
         * @param _docInstance value for instance variable {@link #docInstance}
         */
        public void setDocInstance(final Instance _docInstance)
        {
            docInstance = _docInstance;
        }

        /**
         * Setter method for instance variable {@link #taxDocInstance}.
         *
         * @param _taxDocInstance value for instance variable
         *            {@link #taxDocInstance}
         */
        public void addTaxDocInstance(final Instance _taxDocInstance)
        {
            taxDocInstances.add(_taxDocInstance);
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
            return tax2Amount.size() == 1 ? tax2Amount.values().iterator().next() : BigDecimal.ZERO;
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
            return tax2Amount.get(_taxDocInst);
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
         * Getter method for the instance variable {@link #taxAmount}.
         *
         * @return value of instance variable {@link #taxAmount}
         * @throws EFapsException on error
         */
        public BigDecimal getTaxRateAmount()
            throws EFapsException
        {
            initialize();
            return tax2RateAmount.size() == 1 ? tax2RateAmount.values().iterator().next() : BigDecimal.ZERO;
        }

        /**
         * @param _taxDocInst taxdoc Instance the amount is wanted for
         * @return amount
         * @throws EFapsException on error
         */
        public BigDecimal getTaxRateAmount(final Instance _taxDocInst)
            throws EFapsException
        {
            initialize();
            return tax2RateAmount.get(_taxDocInst);
        }

        /**
         * @param _citype taxdoc type the amount is wanted for
         * @return amount
         * @throws EFapsException on error
         */
        public BigDecimal getTaxRateAmount(final CIType _citype)
            throws EFapsException
        {
            return getTaxRateAmount(getTaxDocInstance(_citype));
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
            return tax2Percent.size() == 1 ? tax2Percent.values().iterator().next() : BigDecimal.ZERO;
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
            return tax2Percent.get(_taxDocInst);
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
            relInstances.add(_relInstance);
        }

        /**
         * Getter method for the instance variable {@link #initialized}.
         *
         * @return value of instance variable {@link #initialized}
         */
        public boolean isInitialized()
        {
            return initialized;
        }

        /**
         * Setter method for instance variable {@link #initialized}.
         *
         * @param _initialized value for instance variable {@link #initialized}
         */
        public void setInitialized(final boolean _initialized)
        {
            initialized = _initialized;
        }

        /**
         * Analyze payment.
         * @throws EFapsException on error
         */
        protected void analyzePayment()
            throws EFapsException
        {
            final QueryBuilder attrQueryBldr = new QueryBuilder(CISales.PaymentDetractionOut);
            attrQueryBldr.addType(CISales.PaymentRetentionOut);

            final QueryBuilder queryBldr = new QueryBuilder(CISales.Payment);
            queryBldr.addWhereAttrEqValue(CISales.Payment.CreateDocument, taxDocInstances.toArray());
            queryBldr.addWhereAttrInQuery(CISales.Payment.TargetDocument,
                            attrQueryBldr.getAttributeQuery(CISales.PaymentDocumentAbstract.ID));
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selAmount = SelectBuilder.get()
                            .linkfrom(CISales.TransactionAbstract, CISales.TransactionAbstract.Payment)
                            .attribute(CISales.TransactionAbstract.Amount);
            final SelectBuilder selDate = SelectBuilder.get()
                            .linkto(CISales.Payment.TargetDocument)
                            .attribute(CISales.PaymentDocumentOutAbstract.Date);
            final SelectBuilder selName = SelectBuilder.get()
                            .linkto(CISales.Payment.TargetDocument)
                            .attribute(CISales.PaymentDocumentOutAbstract.Name);
            multi.addSelect(selAmount, selDate, selName);
            multi.execute();
            BigDecimal paymentAmountTmp = BigDecimal.ZERO;
            String paymentNameTmp = "";
            while (multi.next()) {
                paymentAmountTmp = paymentAmountTmp.add(multi.<BigDecimal>getSelect(selAmount));
                if (paymentDate == null) {
                    paymentDate = multi.getSelect(selDate);
                }
                if (StringUtils.isNotEmpty(paymentNameTmp)) {
                    paymentNameTmp = paymentNameTmp + ", ";
                }
                paymentNameTmp = paymentNameTmp + multi.getSelect(selName);
            }
            if (paymentAmount == null) {
                paymentAmount = paymentAmountTmp;
            }
            if (paymentName == null) {
                paymentName = paymentNameTmp;
            }
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
            if (paymentAmount == null) {
                analyzePayment();
            }
            return paymentAmount;
        }

        /**
         * Setter method for instance variable {@link #paymentAmount}.
         *
         * @param _paymentAmount value for instance variable {@link #paymentAmount}
         */
        public void setPaymentAmount(final BigDecimal _paymentAmount)
        {
            paymentAmount = _paymentAmount;
        }

        /**
         * Getter method for the instance variable {@link #paymentName}.
         *
         * @return value of instance variable {@link #paymentName}
         * @throws EFapsException on error
         */
        public String getPaymentName()
            throws EFapsException
        {
            if (paymentName == null) {
                analyzePayment();
            }
            return paymentName;
        }

        /**
         * Setter method for instance variable {@link #paymentName}.
         *
         * @param _paymentName value for instance variable {@link #paymentName}
         */
        public void setPaymentName(final String _paymentName)
        {
            paymentName = _paymentName;
        }

        /**
         * Getter method for the instance variable {@link #paymentDate}.
         *
         * @return value of instance variable {@link #paymentDate}
         * @throws EFapsException on error
         */
        public DateTime getPaymentDate()
            throws EFapsException
        {
            if (paymentDate == null) {
                analyzePayment();
            }
            return paymentDate;
        }

        /**
         * Setter method for instance variable {@link #paymentDate}.
         *
         * @param _paymentDate value for instance variable {@link #paymentDate}
         */
        public void setPaymentDate(final DateTime _paymentDate)
        {
            paymentDate = _paymentDate;
        }

        /**
         * @return true if of this type
         * @throws EFapsException on error
         */
        public boolean isProfServRetention()
            throws EFapsException
        {
            initialize();
            return !taxDocInstances.isEmpty() && hasType(CISales.IncomingProfServRetention);
        }

        /**
         * @return true if of this type
         * @throws EFapsException on error
         */
        public boolean isProfServInsurance()
            throws EFapsException
        {
            initialize();
            return !taxDocInstances.isEmpty() && hasType(CISales.IncomingProfServInsurance);
        }

        /**
         * @param _type type the taxdoc instance is wanted for
         * @return if found instance, else null
         */
        public Instance getTaxDocInstance(final Type _type)
        {
            Instance ret = null;
            for (final Instance taxDocInst : taxDocInstances) {
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
