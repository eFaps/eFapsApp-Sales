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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
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
    public CreatedDoc create4Doc(final Parameter _parameter,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        CreatedDoc ret = null;
        final BigDecimal value = (BigDecimal) _createdDoc.getValue(AbstractDocumentTax_Base.TAXAMOUNTVALUE);
        if (value.compareTo(BigDecimal.ZERO) > 0) {
            ret = new CreatedDoc();
            final Type type = getType4create4Doc(_parameter);
            final Insert insert = new Insert(type);
            insert.add(CISales.DocumentSumAbstract.Date,
                            _createdDoc.getValue(CISales.DocumentSumAbstract.Date.name));
            insert.add(CISales.DocumentSumAbstract.Contact,
                            _createdDoc.getValue(CISales.DocumentSumAbstract.Contact.name));
            insert.add(CISales.DocumentSumAbstract.Salesperson,
                            _createdDoc.getValue(CISales.DocumentSumAbstract.Salesperson.name));
            insert.add(CISales.DocumentSumAbstract.Group,
                            _createdDoc.getValue(CISales.DocumentSumAbstract.Group.name));
            insert.add(CISales.DocumentSumAbstract.Rate,
                            _createdDoc.getValue(CISales.DocumentSumAbstract.Rate.name));
            insert.add(CISales.DocumentSumAbstract.CurrencyId,
                            _createdDoc.getValue(CISales.DocumentSumAbstract.CurrencyId.name));
            insert.add(CISales.DocumentSumAbstract.RateCurrencyId,
                            _createdDoc.getValue(CISales.DocumentSumAbstract.RateCurrencyId.name));
            insert.add(CISales.DocumentSumAbstract.Creator,
                            _createdDoc.getValue(CISales.DocumentSumAbstract.Creator.name));
            insert.add(CISales.DocumentSumAbstract.Created,
                            _createdDoc.getValue(CISales.DocumentSumAbstract.Created.name));
            insert.add(CISales.DocumentSumAbstract.Modifier,
                            _createdDoc.getValue(CISales.DocumentSumAbstract.Modifier.name));
            insert.add(CISales.DocumentSumAbstract.Modified,
                            _createdDoc.getValue(CISales.DocumentSumAbstract.Modified.name));
            insert.add(CISales.DocumentSumAbstract.RateNetTotal, BigDecimal.ZERO);
            insert.add(CISales.DocumentSumAbstract.RateDiscountTotal, BigDecimal.ZERO);
            insert.add(CISales.DocumentSumAbstract.NetTotal, BigDecimal.ZERO);
            insert.add(CISales.DocumentSumAbstract.DiscountTotal, BigDecimal.ZERO);

            insert.add(CISales.DocumentSumAbstract.Name,
                            _createdDoc.getValue(CISales.DocumentSumAbstract.Name.name));

            final Object[] rateObj = (Object[]) _createdDoc.getValue(CISales.DocumentSumAbstract.Rate.name);
            final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                            BigDecimal.ROUND_HALF_UP);
            final DecimalFormat totalFrmt = NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter));
            final int scale = totalFrmt.getMaximumFractionDigits();

            insert.add(CISales.DocumentSumAbstract.RateCrossTotal,
                            value.setScale(scale, BigDecimal.ROUND_HALF_UP));
            insert.add(CISales.DocumentSumAbstract.CrossTotal,
                            value.divide(rate, BigDecimal.ROUND_HALF_UP).setScale(scale, BigDecimal.ROUND_HALF_UP));

            addStatus2create4Doc(_parameter, insert, _createdDoc);
            insert.execute();

            ret.setInstance(insert.getInstance());
            connectDoc(_parameter, _createdDoc, ret);
        }
        return ret;
    }

    /**
     * @param _parameter
     * @return
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
                                        final Insert _insert,
                                        final CreatedDoc _origDoc)
        throws EFapsException
    {
        _insert.add(CISales.DocumentSumAbstract.StatusAbstract,
                        Status.find(_insert.getInstance().getType().getStatusAttribute().getLink().getUUID(), "Open"));
    }

    public static StringBuilder getSmallTaxField4Doc(final Parameter _parameter,
                                                     final Instance _docInst)
        throws EFapsException
    {
        final StringBuilder ret = new StringBuilder();
        final QueryBuilder queryBldr = new QueryBuilder(CISales.IncomingDocumentTax2Document);
        queryBldr.addWhereAttrEqValue(CISales.IncomingDocumentTax2Document.ToAbstractLink, _docInst);
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selDocTax = SelectBuilder.get().linkto(
                        CISales.IncomingDocumentTax2Document.FromAbstractLink);
        final SelectBuilder selDocTaxInst = new SelectBuilder(selDocTax).instance();
        final SelectBuilder selDocTaxCrossTotal = new SelectBuilder(selDocTax)
                        .attribute(CISales.DocumentSumAbstract.CrossTotal);
        multi.addSelect(selDocTaxInst, selDocTaxCrossTotal);
        multi.execute();
        Instance taxDocInst = null;
        BigDecimal crossTotal = BigDecimal.ZERO;
        if (multi.next()) {
            taxDocInst = multi.<Instance>getSelect(selDocTaxInst);
            crossTotal = multi.<BigDecimal>getSelect(selDocTaxCrossTotal);
        }
        // TODO configurable
        final String fieldName = "taxDocType" + _docInst.getOid();
        final String id1 = RandomStringUtils.randomAlphanumeric(6);
        final String id2 = RandomStringUtils.randomAlphanumeric(6);
        final String id3 = RandomStringUtils.randomAlphanumeric(6);
        final String id4 = RandomStringUtils.randomAlphanumeric(6);

        ret.append("<input type=\"radio\" value=\"").append("NONE").append("\" name=\"")
                        .append(fieldName).append("\" id=\"").append(id1).append("\"");
        if (taxDocInst == null) {
            ret.append(" checked=\"checked\" ");
        }
        ret.append("/><label for=\"").append(id1).append("\">")
                        .append(DBProperties.getProperty(AbstractDocumentTax.class.getName() + ".NoneSmallLabel"))
                        .append("</label>");

        ret.append("<input type=\"radio\" value=\"").append(CISales.IncomingDetraction.getType().getId())
                        .append("\" name=\"").append(fieldName).append("\" id=\"").append(id2).append("\"");
        if (taxDocInst != null && taxDocInst.getType().equals(CISales.IncomingDetraction.getType())) {
            ret.append(" checked=\"checked\" ");
        }
        ret.append("/><label for=\"").append(id2).append("\">")
                        .append(DBProperties.getProperty(AbstractDocumentTax.class.getName()
                                        + ".IncomingDetractionSmallLabel"))
                        .append("</label>");

        ret.append("<input type=\"radio\" value=\"").append(CISales.IncomingRetention.getType().getId())
                        .append("\" name=\"").append(fieldName).append("\" id=\"").append(id3).append("\"");
        if (taxDocInst != null && taxDocInst.getType().equals(CISales.IncomingRetention.getType())) {
            ret.append(" checked=\"checked\" ");
        }
        ret.append("/><label for=\"").append(id3).append("\">")
                        .append(DBProperties.getProperty(AbstractDocumentTax.class.getName()
                                        + ".IncomingRetentionSmallLabel"))
                        .append("</label>");

        ret.append("<input type=\"radio\" value=\"").append(CISales.IncomingPerceptionCertificate.getType().getId())
                        .append("\" name=\"").append(fieldName).append("\" id=\"").append(id4).append("\"");
        if (taxDocInst != null && taxDocInst.getType().equals(CISales.IncomingPerceptionCertificate.getType())) {
            ret.append(" checked=\"checked\" ");
        }
        ret.append("/><label for=\"").append(id4).append("\">")
                        .append(DBProperties.getProperty(AbstractDocumentTax.class.getName()
                                        + ".IncomingPerceptionCertificateSmallLabel")).append("</label>");

        final DecimalFormat formater = NumberFormatter.get().getTwoDigitsFormatter();
        ret.append("<input type=\"text\" name=\"taxDocAmount\" size=\"6\" value=\"")
                        .append(formater.format(crossTotal))
                        .append("\">");
        return ret;
    }

    /**
     * @param _parameter
     * @param _docInst
     * @return
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

    public static class DocTaxInfo
    {

        private Instance docInstance;
        private Instance taxDocInstance;

        private boolean initialized = false;
        private BigDecimal taxAmount = BigDecimal.ZERO;
        private BigDecimal percent = BigDecimal.ZERO;

        /**
         * @param _docInst
         */
        public DocTaxInfo(final Instance _docInst)
        {
            this.docInstance = _docInst;
        }

        protected void initialize()
            throws EFapsException
        {
            if (!this.initialized) {
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
                if (multi.next()) {
                    this.taxDocInstance = multi.getSelect(selDocTaxInst);
                    this.taxAmount = multi.getSelect(selDocTaxCrossTotal);
                    final BigDecimal crossTotal = multi.getSelect(selDocCrossTotal);
                    this.percent = new BigDecimal(100).setScale(8).divide(crossTotal, BigDecimal.ROUND_HALF_UP)
                                    .multiply(this.taxAmount);
                }
                this.initialized = true;
            }
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
            return this.taxDocInstance != null
                            && this.taxDocInstance.getType().isKindOf(CISales.IncomingPerceptionCertificate.getType());
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
            return this.taxDocInstance != null
                            && this.taxDocInstance.getType().isKindOf(CISales.IncomingRetention.getType());
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
            return this.taxDocInstance != null
                            && this.taxDocInstance.getType().isKindOf(CISales.IncomingDetraction.getType());
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
         * Getter method for the instance variable {@link #taxDocInstance}.
         *
         * @return value of instance variable {@link #taxDocInstance}
         * @throws EFapsException on error
         */
        public Instance getTaxDocInstance()
            throws EFapsException
        {
            initialize();
            return this.taxDocInstance;
        }

        /**
         * Setter method for instance variable {@link #taxDocInstance}.
         *
         * @param _taxDocInstance value for instance variable
         *            {@link #taxDocInstance}
         */
        public void setTaxDocInstance(final Instance _taxDocInstance)
        {
            this.taxDocInstance = _taxDocInstance;
        }


        /**
         * Getter method for the instance variable {@link #taxAmount}.
         *
         * @return value of instance variable {@link #taxAmount}
         */
        public BigDecimal getTaxAmount()
        {
            return this.taxAmount;
        }


        /**
         * Setter method for instance variable {@link #taxAmount}.
         *
         * @param _taxAmount value for instance variable {@link #taxAmount}
         */
        public void setTaxAmount(final BigDecimal _taxAmount)
        {
            this.taxAmount = _taxAmount;
        }


        /**
         * Getter method for the instance variable {@link #percent}.
         *
         * @return value of instance variable {@link #percent}
         */
        public BigDecimal getPercent()
        {
            return this.percent;
        }


        /**
         * Setter method for instance variable {@link #percent}.
         *
         * @param _percent value for instance variable {@link #percent}
         */
        public void setPercent(final BigDecimal _percent)
        {
            this.percent = _percent;
        }
    }
}
