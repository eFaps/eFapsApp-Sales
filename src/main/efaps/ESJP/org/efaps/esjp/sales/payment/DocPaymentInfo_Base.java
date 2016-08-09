/*
 * Copyright 2003 - 2015 The eFaps Team
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

package org.efaps.esjp.sales.payment;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.erp.RateInfo;
import org.efaps.esjp.sales.document.AbstractDocumentTax;
import org.efaps.esjp.sales.document.AbstractDocumentTax_Base.DocTaxInfo;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("1f2a247a-717a-4285-a43a-d41e47200b0c")
@EFapsApplication("eFapsApp-Sales")
public abstract class DocPaymentInfo_Base
{

    /**
     * Instance of the Document.
     */
    private final Instance instance;

    /**
     * CrossTotal of the document.
     */
    private BigDecimal crossTotal;

    /**
     * Rate cross total of the document.
     */
    private BigDecimal rateCrossTotal;

    /**
     * rate applied at the document.
     */
    private RateInfo rateInfo;

    /**
     * rate applied at the document.
     */
    private RateInfo rateInfo4Target;

    /**
     * Is this instance initialized (the data retrieved).
     */
    private boolean initialized = false;

    /**
     * List of PaymendDoc .
     */
    private final List<PayPos> payPos = new ArrayList<>();

    /**
     * Instance of the currency.
     */
    private Instance currencyInstance;

    /**
     * Instance of the rate Currency.
     */
    private Instance rateCurrencyInstance;

    /**
     * Account Info.
     */
    private AbstractTargetInfo targetInfo;

    /**
     * Name of the document.
     */
    private String name;

    /**
     * Name of the contact related.
     */
    private String contactName;

    /**
     * Parameter as passed by the eFaps API.
     */
    private Parameter parameter;

    /**
     * date of the document.
     */
    private DateTime date;

    /**
     * @param _docInst instance of the document
     */
    public DocPaymentInfo_Base(final Instance _docInst)
    {
        this.instance = _docInst;
    }

    /**
     * Initialize the instance of this class.
     *
     * @throws EFapsException on erro
     */
    protected void initialize()
        throws EFapsException
    {
        if (!this.initialized) {
            DocPaymentInfo.initialize(getParameter(), this);
        }
    }

    /**
     * @param _parameter parameter to be used
     * @return this
     */
    public DocPaymentInfo setParameter(final Parameter _parameter)
    {
        this.parameter = _parameter;
        return (DocPaymentInfo) this;
    }

    /**
     * Getter method for the instance variable {@link #instance}.
     *
     * @return value of instance variable {@link #instance}
     */
    public Instance getInstance()
    {
        return this.instance;
    }

    /**
     * Getter method for the instance variable {@link #crossTotal}.
     *
     * @return value of instance variable {@link #crossTotal}
     * @throws EFapsException on error
     */
    public BigDecimal getCrossTotal()
        throws EFapsException
    {
        initialize();
        return this.crossTotal;
    }

    /**
     * Getter method for the instance variable {@link #rateCrossTotal}.
     *
     * @return value of instance variable {@link #rateCrossTotal}
     * @throws EFapsException on error
     */
    public BigDecimal getRateCrossTotal()
        throws EFapsException
    {
        initialize();
        return this.rateCrossTotal;
    }

    /**
     * @return the cross total in the currency of the account
     * @throws EFapsException on error
     */
    public BigDecimal getCrossTotal4Target()
        throws EFapsException
    {
        initialize();
        BigDecimal ret = BigDecimal.ZERO;
        if (getTargetInfo().getCurrencyInstance().equals(getRateCurrencyInstance())) {
            ret = ret.add(getRateCrossTotal());
        } else {
            ret = ret.add(getRateCrossTotal().divide(getRateInfo4Target().getRate(), BigDecimal.ROUND_HALF_UP));
        }
        return ret;
    }

    /**
     * @return the paid amount in the account currency
     * @throws EFapsException on error
     */
    public BigDecimal getPaid4Target()
        throws EFapsException
    {
        initialize();
        BigDecimal ret = BigDecimal.ZERO;
        for (final PayPos pos : this.payPos) {
            // the currency of the PaymentDocument is the same as the ratcurrency
            if (getTargetInfo().getCurrencyInstance().equals(pos.getCurrencyInstance())) {
                ret = ret.add(pos.getAmount());
            } else {
                ret = ret.add(pos.getAmount().divide(getRateInfo4Target().getRate(), BigDecimal.ROUND_HALF_UP));
            }
        }
        return ret;
    }

    /**
     * Getter method for the instance variable {@link #rateInfo}.
     *
     * @return value of instance variable {@link #rateInfo}
     * @throws EFapsException on error
     */
    public RateInfo getRateInfo4Target()
        throws EFapsException
    {
        if (this.rateInfo4Target == null) {
            this.rateInfo4Target = new Currency().evaluateRateInfos(getParameter(),
                        getDate(), getRateCurrencyInstance(), getTargetInfo().getCurrencyInstance())[2];
        }
        return this.rateInfo4Target;
    }

    /**
     * Setter method for instance variable {@link #rateInfo4Account}.
     *
     * @param _rateInfo4Account value for instance variable {@link #rateInfo4Account}
     */
    public void setRateInfo4Target(final RateInfo _rateInfo4Account)
    {
        this.rateInfo4Target = _rateInfo4Account;
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
     * Getter method for the instance variable {@link #payPos}.
     *
     * @return value of instance variable {@link #payPos}
     * @throws EFapsException on error
     */
    public List<PayPos> getPayPos()
        throws EFapsException
    {
        initialize();
        return this.payPos;
    }

    /**
     * Getter method for the instance variable {@link #currencyInstance}.
     *
     * @return value of instance variable {@link #currencyInstance}
     * @throws EFapsException on error
     */
    public Instance getCurrencyInstance()
        throws EFapsException
    {
        initialize();
        return this.currencyInstance;
    }

    /**
     * Getter method for the instance variable {@link #rateCurrencyInstance}.
     *
     * @return value of instance variable {@link #rateCurrencyInstance}
     * @throws EFapsException on error
     */
    public Instance getRateCurrencyInstance()
        throws EFapsException
    {
        initialize();
        return this.rateCurrencyInstance;
    }

    /**
     * @return true if paid completely
     * @throws EFapsException on error
     */
    public boolean isPaid()
        throws EFapsException
    {
        initialize();
        final Properties props = Sales.PAYMENTTHRESHOLD4PAID.get();
        BigDecimal threshold = BigDecimal.ZERO;
        try {
            final DecimalFormat format =  (DecimalFormat) NumberFormat.getInstance();
            format.setParseBigDecimal(true);
            threshold = (BigDecimal) format.parse(props.getProperty(this.instance.getType().getName(), "0"));
        } catch (final ParseException e) {
            throw new EFapsException("catched ParseException", e);
        }
        return this.crossTotal.subtract(getPaid()).abs().compareTo(threshold) <= 0
                        || this.rateCrossTotal.subtract(getRatePaid()).abs().compareTo(threshold) <= 0;
    }

    /**
     * @return the paid amount in the base currency of the document.
     * @throws EFapsException on error
     */
    public BigDecimal getPaid()
        throws EFapsException
    {
        initialize();
        BigDecimal ret = BigDecimal.ZERO;
        for (final PayPos pos : this.payPos) {
            // the currency of the PaymentDocument is the same as the base
            // currency
            if (getCurrencyInstance().equals(pos.getCurrencyInstance())) {
                ret = ret.add(pos.getAmount());
            } else {
                final RateInfo[] rateInfos = new Currency().evaluateRateInfos(getParameter(),
                                getDate(), pos.getCurrencyInstance(), getCurrencyInstance());
                ret = ret.add(pos.getAmount().divide(rateInfos[2].getRate(), BigDecimal.ROUND_HALF_UP));
            }
        }
        return ret;
    }

    /**
     * @return the paid amount in the rate currency of the document.
     * @throws EFapsException on error
     */
    public BigDecimal getRatePaid()
        throws EFapsException
    {
        initialize();
        BigDecimal ret = BigDecimal.ZERO;
        for (final PayPos pos : this.payPos) {
            // the currency of the PaymentDocument is the same as the ratcurrency
            if (getRateCurrencyInstance().equals(pos.getCurrencyInstance())) {
                ret = ret.add(pos.getAmount());
            } else {
                final RateInfo[] rateInfos = new Currency().evaluateRateInfos(getParameter(),
                                getDate(), pos.getCurrencyInstance(), getRateCurrencyInstance());
                ret = ret.add(pos.getAmount().divide(rateInfos[2].getRate(), BigDecimal.ROUND_HALF_UP));
            }
        }
        return ret;
    }

    /**
     * Sets the target doc inst.
     *
     * @param _accInst instance of the account
     * @return the doc payment info
     * @throws EFapsException on error
     */
    public DocPaymentInfo setTargetDocInst(final Instance _accInst)
        throws EFapsException
    {
        this.targetInfo = new TargetDocInfo(_accInst);
        return (DocPaymentInfo) this;
    }

    /**
     * Sets the account inst.
     *
     * @param _accInst instance of the account
     * @return the doc payment info
     * @throws EFapsException on error
     */
    public DocPaymentInfo setAccountInst(final Instance _accInst)
        throws EFapsException
    {
        this.targetInfo = new AccountInfo(_accInst);
        return (DocPaymentInfo) this;
    }

    /**
     * Getter method for the instance variable {@link #name}.
     *
     * @return value of instance variable {@link #name}
     * @throws EFapsException on error
     */
    public String getName()
        throws EFapsException
    {
        initialize();
        return this.name;
    }

    /**
     * Setter method for instance variable {@link #name}.
     *
     * @param _name value for instance variable {@link #name}
     * @return the doc payment info
     */
    public DocPaymentInfo setName(final String _name)
    {
        this.name = _name;
        return (DocPaymentInfo) this;
    }

    /**
     * Getter method for the instance variable {@link #contactName}.
     *
     * @return value of instance variable {@link #contactName}
     * @throws EFapsException on error
     */
    public String getContactName()
        throws EFapsException
    {
        initialize();
        return this.contactName;
    }

    /**
     * Getter method for the instance variable {@link #accountInfo}.
     *
     * @return value of instance variable {@link #accountInfo}
     * @throws EFapsException on error
     */
    public AbstractTargetInfo getTargetInfo()
        throws EFapsException
    {
        initialize();
        return this.targetInfo;
    }

    /**
     * Setter method for instance variable {@link #accountInfo}.
     *
     * @param _accountInfo value for instance variable {@link #accountInfo}
     * @return the doc payment info
     */
    public DocPaymentInfo setAccountInfo(final AccountInfo _accountInfo)
    {
        this.targetInfo = _accountInfo;
        return (DocPaymentInfo) this;
    }

    /**
     * @return String for info
     * @throws EFapsException on error
     */
    public String getInfoField()
        throws EFapsException
    {
        final List<Object> objects = new ArrayList<>();
        objects.add(getRateCrossTotal());
        objects.add(getRatePaid());
        objects.add(BigDecimal.ZERO);
        objects.add(BigDecimal.ZERO);
        final CurrencyInst currInst = CurrencyInst.get(getRateCurrencyInstance());
        objects.add(currInst.getSymbol());
        objects.add(currInst.getISOCode());

        final String key;
        if (getTargetInfo() == null) {
            setTargetDocInst(getInstance());
        }
        if (currInst.getInstance().equals(getTargetInfo().getCurrencyInstance())) {
            key = ".InfoField";
        } else {
            key = ".InfoField4Account";
            objects.add(getCrossTotal4Target());
            objects.add(getPaid4Target());
            objects.add(BigDecimal.ZERO);
            objects.add(BigDecimal.ZERO);
            objects.add(getTargetInfo().getCurrencyInst().getSymbol());
            objects.add(getTargetInfo().getCurrencyInst().getISOCode());
        }
        return DBProperties.getFormatedDBProperty(DocPaymentInfo.class.getName() + key, objects.toArray());
    }

    /**
     * @return formatter usd for this info
     * @throws EFapsException on error
     */
    public DecimalFormat getFormatter()
        throws EFapsException
    {
        return NumberFormatter.get().getFrmt4Total(getInstance().getType().getName());
    }

    /**
     * Setter method for instance variable {@link #contactName}.
     *
     * @param _contactName value for instance variable {@link #contactName}
     * @return the doc payment info
     */
    public DocPaymentInfo setContactName(final String _contactName)
    {
        this.contactName = _contactName;
        return (DocPaymentInfo) this;
    }

    /**
     * Getter method for the instance variable {@link #parameter}.
     *
     * @return value of instance variable {@link #parameter}
     */
    public Parameter getParameter()
    {
        return this.parameter;
    }

    /**
     * Getter method for the instance variable {@link #rateInfo}.
     *
     * @return value of instance variable {@link #rateInfo}
     * @throws EFapsException on error
     */
    public RateInfo getRateInfo()
        throws EFapsException
    {
        initialize();
        return this.rateInfo;
    }

    /**
     * Setter method for instance variable {@link #rateInfo}.
     *
     * @param _rateInfo value for instance variable {@link #rateInfo}
     * @return the doc payment info
     */
    public DocPaymentInfo setRateInfo(final RateInfo _rateInfo)
    {
        this.rateInfo = _rateInfo;
        return (DocPaymentInfo) this;
    }

    /**
     * Getter method for the instance variable {@link #date}.
     *
     * @return value of instance variable {@link #date}
     * @throws EFapsException on error
     */
    public DateTime getDate()
        throws EFapsException
    {
        initialize();
        return this.date;
    }

    /**
     * Setter method for instance variable {@link #date}.
     *
     * @param _date value for instance variable {@link #date}
     * @return the doc payment info
     */
    public DocPaymentInfo setDate(final DateTime _date)
    {
        this.date = _date;
        return (DocPaymentInfo) this;
    }

    /**
     * Setter method for instance variable {@link #crossTotal}.
     *
     * @param _crossTotal value for instance variable {@link #crossTotal}
     * @return the doc payment info
     */
    public DocPaymentInfo setCrossTotal(final BigDecimal _crossTotal)
    {
        this.crossTotal = _crossTotal;
        return (DocPaymentInfo) this;
    }

    /**
     * Setter method for instance variable {@link #rateCrossTotal}.
     *
     * @param _rateCrossTotal value for instance variable {@link #rateCrossTotal}
     * @return the doc payment info
     */
    public DocPaymentInfo setRateCrossTotal(final BigDecimal _rateCrossTotal)
    {
        this.rateCrossTotal = _rateCrossTotal;
        return (DocPaymentInfo) this;
    }

    /**
     * @return DoctaxInfo for this document
     * @throws EFapsException on error
     */
    public DocTaxInfo getDocTaxInfo()
        throws EFapsException
    {
        initialize();
        return AbstractDocumentTax.getDocTaxInfo(getParameter(), getInstance());
    }

    /**
     * Setter method for instance variable {@link #currencyInstance}.
     *
     * @param _currencyInstance value for instance variable {@link #currencyInstance}
     * @return the doc payment info
     */
    public DocPaymentInfo setCurrencyInstance(final Instance _currencyInstance)
    {
        this.currencyInstance = _currencyInstance;
        return (DocPaymentInfo) this;
    }

    /**
     * Setter method for instance variable {@link #rateCurrencyInstance}.
     *
     * @param _rateCurrencyInstance value for instance variable {@link #rateCurrencyInstance}
     * @return the doc payment info
     */
    public DocPaymentInfo setRateCurrencyInstance(final Instance _rateCurrencyInstance)
    {
        this.rateCurrencyInstance = _rateCurrencyInstance;
        return (DocPaymentInfo) this;
    }

    /**
     * Setter method for instance variable {@link #initialized}.
     *
     * @param _initialized value for instance variable {@link #initialized}
     * @return the doc payment info
     */
    public DocPaymentInfo setInitialized(final boolean _initialized)
    {
        this.initialized = _initialized;
        return (DocPaymentInfo) this;
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _infos infos to be initialized with the base information
     * @return mapping for infos to related instance
     */
    protected static Map<Instance, DocPaymentInfo_Base> getInfoMap(final Parameter _parameter,
                                                                   final DocPaymentInfo_Base... _infos)
    {
        final Map<Instance, DocPaymentInfo_Base> ret = new HashMap<>();
        for (final DocPaymentInfo_Base info : _infos) {
            ret.put(info.getInstance(), info);
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _infos infos to be initialized with the base information
     * @throws EFapsException on error
     */
    protected static void initBaseDoc(final Parameter _parameter,
                                      final DocPaymentInfo_Base... _infos)
        throws EFapsException
    {
        final Map<Instance, DocPaymentInfo_Base> instance2info = getInfoMap(_parameter, _infos);

        final MultiPrintQuery multi = new MultiPrintQuery(new ArrayList<>(instance2info.keySet()));
        final SelectBuilder selContactName = new SelectBuilder()
                        .linkto(CISales.DocumentAbstract.Contact).attribute(CIContacts.Contact.Name);
        final SelectBuilder selCurInst = new SelectBuilder().linkto(CISales.DocumentSumAbstract.CurrencyId)
                        .instance();
        final SelectBuilder selRateCurInst = new SelectBuilder().linkto(CISales.DocumentSumAbstract.RateCurrencyId)
                        .instance();
        multi.addSelect(selCurInst, selRateCurInst, selContactName);
        multi.addAttribute(CISales.DocumentAbstract.Name, CISales.DocumentAbstract.Date,
                        CISales.DocumentSumAbstract.RateCrossTotal, CISales.DocumentSumAbstract.Rate,
                        CISales.DocumentSumAbstract.CrossTotal);
        multi.executeWithoutAccessCheck();

        while (multi.next()) {
            final DocPaymentInfo_Base info = instance2info.get(multi.getCurrentInstance());
            info.setCrossTotal(multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.CrossTotal));
            info.setRateCrossTotal(multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal));
            info.setName(multi.<String>getAttribute(CISales.DocumentSumAbstract.Name));
            info.setDate(multi.<DateTime>getAttribute(CISales.DocumentSumAbstract.Date));
            info.setRateInfo(new Currency().evaluateRateInfo(info.getParameter(),
                            multi.<Object[]>getAttribute(CISales.DocumentSumAbstract.Rate)));
            info.setCurrencyInstance(multi.<Instance>getSelect(selCurInst));
            info.setRateCurrencyInstance(multi.<Instance>getSelect(selRateCurInst));
            info.setContactName(multi.<String>getSelect(selContactName));
        }
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _infos infos to be initialized with the base information
     * @throws EFapsException on error
     */
    protected static void initPayments(final Parameter _parameter,
                                       final DocPaymentInfo_Base... _infos)
        throws EFapsException
    {
        final Map<Instance, DocPaymentInfo_Base> instance2info = getInfoMap(_parameter, _infos);

        // check normal payments
        final QueryBuilder queryBldr = new QueryBuilder(CIERP.Document2PaymentDocumentAbstract);
        queryBldr.addWhereAttrEqValue(CIERP.Document2PaymentDocumentAbstract.FromAbstractLink,
                        instance2info.keySet().toArray());
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIERP.Document2PaymentDocumentAbstract.Date,
                        CIERP.Document2PaymentDocumentAbstract.Amount);
        final SelectBuilder selCurInst = new SelectBuilder().linkto(
                        CIERP.Document2PaymentDocumentAbstract.CurrencyLink).instance();
        final SelectBuilder selDocInst = new SelectBuilder().linkto(
                        CIERP.Document2PaymentDocumentAbstract.FromAbstractLink).instance();
        multi.addSelect(selCurInst, selDocInst);
        multi.executeWithoutAccessCheck();
        while (multi.next()) {
            final Instance docInst = multi.getSelect(selDocInst);
            final DocPaymentInfo_Base info = instance2info.get(docInst);
            final Instance curInst = multi.getSelect(selCurInst);
            final DateTime dateTmp = multi.getAttribute(CIERP.Document2PaymentDocumentAbstract.Date);
            final BigDecimal amount = multi.getAttribute(CIERP.Document2PaymentDocumentAbstract.Amount);
            info.payPos.add(new PayPos(dateTmp, amount, curInst));
        }
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _infos infos to be initialized with the base information
     * @throws EFapsException on error
     */
    protected static void initDetraction(final Parameter _parameter,
                                         final DocPaymentInfo_Base... _infos)
        throws EFapsException
    {
        final Map<Instance, DocPaymentInfo_Base> instance2info = getInfoMap(_parameter, _infos);

        // check related taxdocs for detraction, for detraction the payment for detraction will be included
        final QueryBuilder attrTaxQueryBldr = new QueryBuilder(CISales.IncomingDetraction2IncomingInvoice);
        attrTaxQueryBldr.addWhereAttrEqValue(CISales.IncomingDetraction2IncomingInvoice.ToAbstractLink,
                        instance2info.keySet().toArray());

        final QueryBuilder taxQueryBldr = new QueryBuilder(CIERP.Document2PaymentDocumentAbstract);
        taxQueryBldr.addWhereAttrInQuery(CIERP.Document2PaymentDocumentAbstract.FromAbstractLink,
                        attrTaxQueryBldr.getAttributeQuery(CISales.IncomingDocumentTax2Document.FromAbstractLink));
        final MultiPrintQuery taxMulti = taxQueryBldr.getPrint();
        taxMulti.addAttribute(CIERP.Document2PaymentDocumentAbstract.Date,
                        CIERP.Document2PaymentDocumentAbstract.Amount);
        final SelectBuilder selTaxCurInst = SelectBuilder.get().linkto(
                        CIERP.Document2PaymentDocumentAbstract.CurrencyLink).instance();
        final SelectBuilder selDocInst = SelectBuilder.get()
                        .linkto(CIERP.Document2PaymentDocumentAbstract.FromAbstractLink)
                        .linkfrom(CISales.IncomingDetraction2IncomingInvoice.FromLink)
                        .linkto(CISales.IncomingDetraction2IncomingInvoice.ToLink).instance();
        taxMulti.addSelect(selTaxCurInst, selDocInst);
        taxMulti.executeWithoutAccessCheck();
        while (taxMulti.next()) {
            final Instance docInst = taxMulti.getSelect(selDocInst);
            final DocPaymentInfo_Base info = instance2info.get(docInst);
            final Instance curInst = taxMulti.getSelect(selTaxCurInst);
            final DateTime dateTmp = taxMulti.getAttribute(CIERP.Document2PaymentDocumentAbstract.Date);
            final BigDecimal amount = taxMulti.getAttribute(CIERP.Document2PaymentDocumentAbstract.Amount);
            info.payPos.add(new PayPos(dateTmp, amount, curInst));
        }
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _infos infos to be initialized with the base information
     * @throws EFapsException on error
     */
    protected static void initRetention(final Parameter _parameter,
                                        final DocPaymentInfo_Base... _infos)
        throws EFapsException
    {
        final Map<Instance, DocPaymentInfo_Base> instance2info = getInfoMap(_parameter, _infos);

        // check related taxdocs for retention. For retention the emission
        // of a certificate counts as payment
        final QueryBuilder attrTaxQueryBldr2 = new QueryBuilder(CISales.IncomingRetention2IncomingInvoice);
        attrTaxQueryBldr2.addWhereAttrEqValue(CISales.IncomingRetention2IncomingInvoice.ToAbstractLink,
                        instance2info.keySet().toArray());

        final QueryBuilder certQueryBldr = new QueryBuilder(CISales.RetentionCertificate);
        certQueryBldr.addWhereAttrEqValue(CISales.RetentionCertificate.Status,
                        Status.find(CISales.RetentionCertificateStatus.Closed));

        final QueryBuilder certQueryBldr2 = new QueryBuilder(CISales.RetentionCertificate2IncomingRetention);
        certQueryBldr2.addWhereAttrInQuery(CISales.RetentionCertificate2IncomingRetention.FromLink,
                        certQueryBldr.getAttributeQuery(CISales.RetentionCertificate.ID));

        final QueryBuilder retQueryBldr = new QueryBuilder(CISales.IncomingRetention);
        retQueryBldr.addWhereAttrInQuery(CISales.IncomingRetention.ID,
                        certQueryBldr2.getAttributeQuery(CISales.RetentionCertificate2IncomingRetention.ToLink));
        retQueryBldr.addWhereAttrInQuery(CISales.IncomingRetention.ID,
                        attrTaxQueryBldr2.getAttributeQuery(CISales.IncomingRetention2IncomingInvoice.FromLink));
        final MultiPrintQuery retMulti = retQueryBldr.getPrint();
        retMulti.addAttribute(CISales.IncomingRetention.CrossTotal, CISales.IncomingRetention.Date);
        final SelectBuilder retSelCur = new SelectBuilder().linkto(CISales.IncomingRetention.CurrencyId).instance();
        final SelectBuilder selDocInst = SelectBuilder.get()
                        .linkfrom(CISales.IncomingRetention2IncomingInvoice.FromLink)
                        .linkto(CISales.IncomingRetention2IncomingInvoice.ToLink).instance();
        retMulti.addSelect(retSelCur, selDocInst);
        retMulti.executeWithoutAccessCheck();
        while (retMulti.next()) {
            final Instance docInst = retMulti.getSelect(selDocInst);
            final DocPaymentInfo_Base info = instance2info.get(docInst);
            final Instance curInst = retMulti.getSelect(retSelCur);
            final DateTime dateTmp = retMulti.getAttribute(CISales.IncomingRetention.Date);
            final BigDecimal amount = retMulti.getAttribute(CISales.IncomingRetention.CrossTotal);
            info.payPos.add(new PayPos(dateTmp, amount, curInst));
        }
    }

    /**
     * Register Swap information a payments. Only the "from" one is payed,
     * the "to" one is the one paying. Only will be added as a payment
     * position if none of the documents has the status canceled.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _infos infos to be initialized with the base information
     * @throws EFapsException on error
     */
    protected static void initSwap(final Parameter _parameter,
                                   final DocPaymentInfo_Base... _infos)
        throws EFapsException
    {
        final Map<Instance, DocPaymentInfo_Base> instance2info = getInfoMap(_parameter, _infos);

        final QueryBuilder swapQueryBldr = new QueryBuilder(CISales.Document2Document4Swap);
        swapQueryBldr.setOr(true);
        swapQueryBldr.addWhereAttrEqValue(CISales.Document2Document4Swap.FromAbstractLink,
                        instance2info.keySet().toArray());
        swapQueryBldr.addWhereAttrEqValue(CISales.Document2Document4Swap.ToAbstractLink,
                        instance2info.keySet().toArray());
        final Set<Instance> verifySet = new HashSet<>();
        final MultiPrintQuery swapMulti = swapQueryBldr.getPrint();
        swapMulti.addAttribute(CISales.Document2Document4Swap.Amount);
        final SelectBuilder selCur3 = new SelectBuilder()
                        .linkto(CISales.Document2Document4Swap.CurrencyLink).instance();
        final SelectBuilder selDocFromInst = SelectBuilder.get()
                        .linkto(CISales.Document2Document4Swap.FromAbstractLink).instance();

        final SelectBuilder selDocFromStatus = SelectBuilder.get()
                        .linkto(CISales.Document2Document4Swap.FromAbstractLink).status().key();
        final SelectBuilder selDocToStatus = SelectBuilder.get()
                        .linkto(CISales.Document2Document4Swap.ToAbstractLink).status().key();
        swapMulti.addSelect(selCur3, selDocFromInst, selDocFromStatus, selDocToStatus);
        swapMulti.executeWithoutAccessCheck();
        while (swapMulti.next()) {
            if (!verifySet.contains(swapMulti.getCurrentInstance())) {
                verifySet.add(swapMulti.getCurrentInstance());
                final Instance docFromInst = swapMulti.getSelect(selDocFromInst);
                final String key1 = swapMulti.getSelect(selDocFromStatus);
                final String key2 = swapMulti.getSelect(selDocToStatus);
                if (instance2info.containsKey(docFromInst) && !"Canceled".equals(key1) && !"Canceled".equals(key2)
                                && !"Replaced".equals(key1) && !"Replaced".equals(key2)) {
                    final DocPaymentInfo_Base info = instance2info.get(docFromInst);
                    final Instance curInst = swapMulti.getSelect(selCur3);
                    final BigDecimal amount = swapMulti.getAttribute(CISales.Document2Document4Swap.Amount);
                    info.payPos.add(new PayPos(info.date, amount, curInst));
                }
            }
        }
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _infos infos to be initialized with the base information
     * @throws EFapsException on error
     */
    protected static void initialize(final Parameter _parameter,
                                     final DocPaymentInfo_Base... _infos)
        throws EFapsException
    {
        DocPaymentInfo.initBaseDoc(_parameter, _infos);
        DocPaymentInfo.initPayments(_parameter, _infos);
        DocPaymentInfo.initDetraction(_parameter, _infos);
        DocPaymentInfo.initRetention(_parameter, _infos);
        DocPaymentInfo.initSwap(_parameter, _infos);
        for (final DocPaymentInfo_Base info : _infos) {
            info.setInitialized(true);
        }
    }

    /**
     * Internal class that converts the related PaymentDocuments in positions
     * for easier calculation.
     */
    public static class PayPos
    {

        /**
         * Date of he Payment.
         */
        private final DateTime date;

        /**
         * Amount of the Payment.
         */
        private final BigDecimal amount;

        /**
         * instance of the Currency.
         */
        private final Instance currencyInstance;

        /**
         * @param _date Date of he Payment
         * @param _amount Amount of the Payment
         * @param _curInst instance of the Currency
         */
        public PayPos(final DateTime _date,
                      final BigDecimal _amount,
                      final Instance _curInst)
        {
            this.date = _date;
            this.amount = _amount;
            this.currencyInstance = _curInst;
        }

        /**
         * Getter method for the instance variable {@link #date}.
         *
         * @return value of instance variable {@link #date}
         */
        public DateTime getDate()
        {
            return this.date;
        }

        /**
         * Getter method for the instance variable {@link #amount}.
         *
         * @return value of instance variable {@link #amount}
         */
        public BigDecimal getAmount()
        {
            return this.amount;
        }

        /**
         * Getter method for the instance variable {@link #currencyid}.
         *
         * @return value of instance variable {@link #currencyid}
         */
        public Instance getCurrencyInstance()
        {
            return this.currencyInstance;
        }

        @Override
        public String toString()
        {
            return ToStringBuilder.reflectionToString(this);
        }
    }

    /**
     * The Class TargetInfo.
     */
    public abstract static class AbstractTargetInfo
    {

        /**
         * Instance of this acocunt.
         */
        private final Instance instance;

        /**
         * Instance of the currency of the account.
         */
        private Instance currencyInstance;

        /**
         * CurrencyInst object of the currency of the account.
         */
        private CurrencyInst currencyInst;

        /**
         * Instantiates a new target info.
         *
         * @param _instance the instance
         * @throws EFapsException on error
         */
        public AbstractTargetInfo(final Instance _instance)
            throws EFapsException
        {
            this.instance = _instance;
        }

        /**
         * Gets the currency instance.
         *
         * @return Instance of the currency
         * @throws EFapsException the e faps exception
         */
        protected Instance getCurrencyInstance()
            throws EFapsException
        {
            if (this.currencyInstance == null) {
                this.currencyInstance = Currency.getBaseCurrency();
            }
            return this.currencyInstance;
        }

        /**
         * Gets the currency inst.
         *
         * @return CurrencyInst object for the currency of this account
         * @throws EFapsException the e faps exception
         */
        protected CurrencyInst getCurrencyInst()
            throws EFapsException
        {
            if (this.currencyInst == null) {
                this.currencyInst = CurrencyInst.get(getCurrencyInstance());
            }
            return this.currencyInst;
        }

        /**
         * Getter method for the instance variable {@link #instance}.
         *
         * @return value of instance variable {@link #instance}
         */
        public Instance getInstance()
        {
            return this.instance;
        }

        /**
         * Setter method for instance variable {@link #currencyInstance}.
         *
         * @param _currencyInstance value for instance variable {@link #currencyInstance}
         */
        public void setCurrencyInstance(final Instance _currencyInstance)
        {
            this.currencyInstance = _currencyInstance;
        }

        /**
         * Setter method for instance variable {@link #currencyInst}.
         *
         * @param _currencyInst value for instance variable {@link #currencyInst}
         */
        public void setCurrencyInst(final CurrencyInst _currencyInst)
        {
            this.currencyInst = _currencyInst;
        }
    }

    /**
     * The Class TargetDocInfo.
     */
    public static class TargetDocInfo
        extends AbstractTargetInfo
    {
        /**
         * /**
         *
         * @param _instance insatcne of the account
         * @throws EFapsException on error
         */
        public TargetDocInfo(final Instance _instance)
            throws EFapsException
        {
            super(_instance);
            if (getInstance().isValid()) {
                final SelectBuilder selAccCurInst = new SelectBuilder()
                                .linkto(CISales.DocumentSumAbstract.RateCurrencyId).instance();
                final PrintQuery print = new PrintQuery(this.getInstance());
                print.addSelect(selAccCurInst);
                print.execute();
                setCurrencyInstance(print.<Instance>getSelect(selAccCurInst));
                setCurrencyInst(CurrencyInst.get(getCurrencyInstance()));
            }
        }
    }

    /**
     * Account info.
     */
    public static class AccountInfo
        extends AbstractTargetInfo
    {
        /**
        /**
         * @param _instance insatcne of the account
         * @throws EFapsException on error
         */
        public AccountInfo(final Instance _instance)
            throws EFapsException
        {
            super(_instance);
            if (getInstance().isValid()) {
                final SelectBuilder selAccCurInst = new SelectBuilder()
                                .linkto(CISales.AccountAbstract.CurrencyLink).instance();
                final PrintQuery print = new PrintQuery(this.getInstance());
                print.addSelect(selAccCurInst);
                print.execute();
                setCurrencyInstance(print.<Instance>getSelect(selAccCurInst));
                setCurrencyInst(CurrencyInst.get(getCurrencyInstance()));
            }
        }
    }
}
