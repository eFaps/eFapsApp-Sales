/*
 * Copyright 2003 - 2019 The eFaps Team
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
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.properties.PropertiesUtil;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.erp.RateInfo;
import org.efaps.esjp.sales.document.AbstractDocumentTax;
import org.efaps.esjp.sales.document.AbstractDocumentTax_Base.DocTaxInfo;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.esjp.ui.html.Table;
import org.efaps.util.EFapsException;
import org.jfree.util.Log;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

/**
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
        instance = _docInst;
    }

    /**
     * Initialize the instance of this class.
     *
     * @throws EFapsException on erro
     */
    protected void initialize()
        throws EFapsException
    {
        if (!initialized) {
            DocPaymentInfo.initialize(getParameter(), this);
        }
    }

    /**
     * Checks if is obligation doc.
     *
     * @return true, if is obligation doc
     * @throws EFapsException on error
     */
    public boolean isObligationDoc()
        throws EFapsException
    {
        final List<String> docs = Sales.PAYMENT_DOCS4OBLIGATION.get();
        return docs.contains(getInstance().getType().getName())
                        || docs.contains(getInstance().getType().getUUID().toString());
    }

    /**
     * @param _parameter parameter to be used
     * @return this
     */
    public DocPaymentInfo setParameter(final Parameter _parameter)
    {
        parameter = _parameter;
        return (DocPaymentInfo) this;
    }

    /**
     * Getter method for the instance variable {@link #instance}.
     *
     * @return value of instance variable {@link #instance}
     */
    public Instance getInstance()
    {
        return instance;
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
        return crossTotal;
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
        return rateCrossTotal;
    }

    /**
     * Get the crossTotal converted into the the given target currency.
     * @param _parameter Parameter
     * @param _targetCurrency currency to convert into
     * @return converted crossTotal
     * @throws EFapsException on error
     */
    public BigDecimal getCrossTotalInCurrency(final Parameter _parameter,
                                              final Instance _targetCurrency)
        throws EFapsException
    {
        BigDecimal ret;
        if (getCurrencyInstance().equals(_targetCurrency)) {
            // if currency is wanted
            ret = getCrossTotal();
        } else if (getRateCurrencyInstance().equals(_targetCurrency)) {
            // if rate currency is wanted
            ret = getRateCrossTotal();
        } else {
            // convert the amount using the date of the document
            ret = Currency.convert(_parameter, getRateCrossTotal(), getRateCurrencyInstance(), _targetCurrency,
                            getInstance().getType().getName(),
                            LocalDate.of(getDate().getYear(), getDate().getMonthOfYear(), getDate().getDayOfMonth()));
        }
        return ret;
    }

    public BigDecimal getPaidInCurrency(final Instance _targetCurrency,
                                        final Boolean _perPayment)
        throws EFapsException
    {
        initialize();
        final boolean perPayment = _perPayment == null ? isPerPayment() : _perPayment;
        BigDecimal ret = BigDecimal.ZERO;
        for (final PayPos pos : getPayPos()) {
            ret = ret.add(pos.getAmountInCurrency(getParameter(), _targetCurrency, perPayment));
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
        if (rateInfo4Target == null) {
            rateInfo4Target = new Currency().evaluateRateInfos(getParameter(),
                        getDate(), getRateCurrencyInstance(), getTargetInfo().getCurrencyInstance())[2];
        }
        return rateInfo4Target;
    }

    /**
     * Setter method for instance variable {@link #rateInfo4Account}.
     *
     * @param _rateInfo4Account value for instance variable {@link #rateInfo4Account}
     */
    public void setRateInfo4Target(final RateInfo _rateInfo4Account)
    {
        rateInfo4Target = _rateInfo4Account;
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
     * Getter method for the instance variable {@link #payPos}.
     *
     * @return value of instance variable {@link #payPos}
     * @throws EFapsException on error
     */
    public List<PayPos> getPayPos()
        throws EFapsException
    {
        initialize();
        return payPos;
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
        return currencyInstance;
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
        return rateCurrencyInstance;
    }

    /**
     * Checks if is per payment.
     *
     * @return true, if is per payment
     * @throws EFapsException on error
     */
    protected boolean isPerPayment()
        throws EFapsException
    {
        final Properties props = Sales.PAYMENT_RULES.get();
        return BooleanUtils.toBoolean(props.getProperty(instance.getType().getName() + ".PerPayment"));
    }

    /**
     * @return true if paid completely
     * @throws EFapsException on error
     */
    public boolean isPaid()
        throws EFapsException
    {
        initialize();
        final Properties props = Sales.PAYMENT_RULES.get();
        boolean ret = false;
        for (final Instance curInst : getCurrencies()) {
            BigDecimal threshold = BigDecimal.ZERO;
            try {
                final CurrencyInst currencyInst = CurrencyInst.get(curInst);
                String val = null;
                if (props.containsKey(instance.getType().getName() + "." + currencyInst.getISOCode() + ".Threshold")) {
                    val = props.getProperty(instance.getType().getName()
                                    + "." + currencyInst.getISOCode() + ".Threshold");
                } else if (props.containsKey(instance.getType().getName() + ".Threshold")) {
                    val = props.getProperty(instance.getType().getName() + ".Threshold");
                }
                if (val != null) {
                    final DecimalFormat format =  (DecimalFormat) NumberFormat.getInstance();
                    format.setParseBigDecimal(true);
                    threshold = (BigDecimal) format.parse(val);
                }
            } catch (final ParseException e) {
                throw new EFapsException("catched ParseException", e);
            }
            ret = getBalanceInCurrency(curInst, isPerPayment()).abs().setScale(1, RoundingMode.HALF_UP)
                            .compareTo(threshold) <= 0;
            if (ret) {
                break;
            }
        }
        return ret;
    }

    public BigDecimal getBalanceInCurrency(final Instance _targetCurrency,
                                           final Boolean _perPayment)
        throws EFapsException
    {
        BigDecimal total = getCrossTotalInCurrency(getParameter(), _targetCurrency);
        if (isObligationDoc()) {
            total = total.negate();
        }
        return total.subtract(getPaidInCurrency(_targetCurrency, _perPayment));
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
        targetInfo = new TargetDocInfo(_accInst);
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
        targetInfo = new AccountInfo(_accInst);
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
        return name;
    }

    /**
     * Setter method for instance variable {@link #name}.
     *
     * @param _name value for instance variable {@link #name}
     * @return the doc payment info
     */
    public DocPaymentInfo setName(final String _name)
    {
        name = _name;
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
        return contactName;
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
        return targetInfo;
    }

    /**
     * Setter method for instance variable {@link #accountInfo}.
     *
     * @param _accountInfo value for instance variable {@link #accountInfo}
     * @return the doc payment info
     */
    public DocPaymentInfo setAccountInfo(final AccountInfo _accountInfo)
    {
        targetInfo = _accountInfo;
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
        objects.add(getPaidInCurrency(getRateCurrencyInstance(), null).abs());
        objects.add(getBalanceInCurrency(getRateCurrencyInstance(), null).abs());
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
            objects.add(getCrossTotalInCurrency(getParameter(), getTargetInfo().getCurrencyInstance()));
            objects.add(getPaidInCurrency(getTargetInfo().getCurrencyInstance(), null).abs());
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
        return NumberFormatter.get().getFrmt4Total(getInstance().getType());
    }

    /**
     * Setter method for instance variable {@link #contactName}.
     *
     * @param _contactName value for instance variable {@link #contactName}
     * @return the doc payment info
     */
    public DocPaymentInfo setContactName(final String _contactName)
    {
        contactName = _contactName;
        return (DocPaymentInfo) this;
    }

    /**
     * Getter method for the instance variable {@link #parameter}.
     *
     * @return value of instance variable {@link #parameter}
     */
    public Parameter getParameter()
    {
        return parameter;
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
        return rateInfo;
    }

    /**
     * Setter method for instance variable {@link #rateInfo}.
     *
     * @param _rateInfo value for instance variable {@link #rateInfo}
     * @return the doc payment info
     */
    public DocPaymentInfo setRateInfo(final RateInfo _rateInfo)
    {
        rateInfo = _rateInfo;
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
        return date;
    }

    /**
     * Setter method for instance variable {@link #date}.
     *
     * @param _date value for instance variable {@link #date}
     * @return the doc payment info
     */
    public DocPaymentInfo setDate(final DateTime _date)
    {
        date = _date;
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
        crossTotal = _crossTotal;
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
        rateCrossTotal = _rateCrossTotal;
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
        currencyInstance = _currencyInstance;
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
        rateCurrencyInstance = _rateCurrencyInstance;
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
        initialized = _initialized;
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
        final Map<Instance, DocPaymentInfo_Base> instance2info = DocPaymentInfo_Base.getInfoMap(_parameter, _infos);

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
            info.setCrossTotal(multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.CrossTotal))
                .setRateCrossTotal(multi.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal))
                .setName(multi.<String>getAttribute(CISales.DocumentSumAbstract.Name))
                .setDate(multi.<DateTime>getAttribute(CISales.DocumentSumAbstract.Date))
                .setRateInfo(new Currency().evaluateRateInfo(info.getParameter(),
                            multi.<Object[]>getAttribute(CISales.DocumentSumAbstract.Rate)))
                .setCurrencyInstance(multi.<Instance>getSelect(selCurInst))
                .setRateCurrencyInstance(multi.<Instance>getSelect(selRateCurInst))
                .setContactName(multi.<String>getSelect(selContactName));
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
        final Map<Instance, DocPaymentInfo_Base> instance2info = DocPaymentInfo_Base.getInfoMap(_parameter, _infos);

        // check normal payments
        final QueryBuilder queryBldr = new QueryBuilder(CIERP.Document2PaymentDocumentAbstract);
        queryBldr.addWhereAttrEqValue(CIERP.Document2PaymentDocumentAbstract.FromAbstractLink,
                        instance2info.keySet().toArray());
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIERP.Document2PaymentDocumentAbstract.Date,
                        CIERP.Document2PaymentDocumentAbstract.Amount,
                        CIERP.Document2PaymentDocumentAbstract.Rate,
                        CIERP.Document2PaymentDocumentAbstract.RateCurrencyLink);
        final SelectBuilder selRateCurInst = new SelectBuilder().linkto(
                        CIERP.Document2PaymentDocumentAbstract.RateCurrencyLink).instance();
        final SelectBuilder selCurInst = new SelectBuilder().linkto(
                        CIERP.Document2PaymentDocumentAbstract.CurrencyLink).instance();
        final SelectBuilder selDocInst = new SelectBuilder().linkto(
                        CIERP.Document2PaymentDocumentAbstract.FromAbstractLink).instance();
        final SelectBuilder selPaymentDocInst = new SelectBuilder().linkto(
                        CIERP.Document2PaymentDocumentAbstract.ToAbstractLink).instance();
        final SelectBuilder selPaymentDocName = new SelectBuilder().linkto(
                        CIERP.Document2PaymentDocumentAbstract.ToAbstractLink)
                        .attribute(CIERP.PaymentDocumentAbstract.Name);
        final SelectBuilder selPaymentDocRate = new SelectBuilder().linkto(
                        CIERP.Document2PaymentDocumentAbstract.ToAbstractLink)
                        .attribute(CIERP.PaymentDocumentAbstract.Rate);
        multi.addSelect(selCurInst, selRateCurInst, selDocInst, selPaymentDocInst, selPaymentDocName,
                        selPaymentDocRate);
        multi.executeWithoutAccessCheck();
        while (multi.next()) {
            final Instance docInst = multi.getSelect(selDocInst);
            final DocPaymentInfo_Base info = instance2info.get(docInst);
            final Instance curInst = multi.getSelect(selCurInst);
            final Instance rateCurInst = multi.getSelect(selRateCurInst);

            final DateTime dateTmp = multi.getAttribute(CIERP.Document2PaymentDocumentAbstract.Date);
            BigDecimal amount = multi.getAttribute(CIERP.Document2PaymentDocumentAbstract.Amount);
            final Instance payDocInst = multi.getSelect(selPaymentDocInst);
            final String name = multi.getSelect(selPaymentDocName);
            final RateInfo rateInfo;

            // payment was in the same currency
            if (curInst.equals(rateCurInst)) {
                // the applied currency was the base currency
                if (Currency.getBaseCurrency().equals(rateCurInst)) {
                    rateInfo = RateInfo.getRateInfo(multi.<Object[]>getAttribute(
                                    CIERP.Document2PaymentDocumentAbstract.Rate));
                } else {
                    rateInfo = RateInfo.getRateInfo(multi.<Object[]>getSelect(selPaymentDocRate));
                }
            } else {
                // different currencies use the one registered in the relation
                rateInfo = RateInfo.getRateInfo(multi.<Object[]>getAttribute(
                                CIERP.Document2PaymentDocumentAbstract.Rate));
            }
            if (InstanceUtils.isKindOf(payDocInst, CISales.PaymentDocumentOutAbstract)) {
                amount = amount.negate();
            }
            info.payPos.add(new PayPos(dateTmp, amount, curInst)
                            .setTypeLabel(payDocInst.getType().getLabel())
                            .setName(name)
                            .setRateInfo(rateInfo));
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
        final Map<Instance, DocPaymentInfo_Base> instance2info = DocPaymentInfo_Base.getInfoMap(_parameter, _infos);

        // check related taxdocs for detraction, for detraction the payment for detraction will be included
        final QueryBuilder attrTaxQueryBldr = new QueryBuilder(CISales.IncomingDetraction2IncomingInvoice);
        attrTaxQueryBldr.addWhereAttrEqValue(CISales.IncomingDetraction2IncomingInvoice.ToAbstractLink,
                        instance2info.keySet().toArray());

        final QueryBuilder taxQueryBldr = new QueryBuilder(CIERP.Document2PaymentDocumentAbstract);
        taxQueryBldr.addWhereAttrInQuery(CIERP.Document2PaymentDocumentAbstract.FromAbstractLink,
                        attrTaxQueryBldr.getAttributeQuery(CISales.IncomingDocumentTax2Document.FromAbstractLink));
        final MultiPrintQuery taxMulti = taxQueryBldr.getPrint();
        taxMulti.addAttribute(CIERP.Document2PaymentDocumentAbstract.Date,
                        CIERP.Document2PaymentDocumentAbstract.Amount,
                        CIERP.Document2PaymentDocumentAbstract.Rate);
        final SelectBuilder selTaxCurInst = SelectBuilder.get().linkto(
                        CIERP.Document2PaymentDocumentAbstract.CurrencyLink).instance();
        final SelectBuilder selRateCurInst = new SelectBuilder().linkto(
                        CIERP.Document2PaymentDocumentAbstract.RateCurrencyLink).instance();
        final SelectBuilder selDocInst = SelectBuilder.get()
                        .linkto(CIERP.Document2PaymentDocumentAbstract.FromAbstractLink)
                        .linkfrom(CISales.IncomingDetraction2IncomingInvoice.FromLink)
                        .linkto(CISales.IncomingDetraction2IncomingInvoice.ToLink).instance();
        final SelectBuilder selPaymentDocRate = new SelectBuilder().linkto(
                        CIERP.Document2PaymentDocumentAbstract.ToAbstractLink)
                        .attribute(CIERP.PaymentDocumentAbstract.Rate);
        final SelectBuilder selPaymentDocInst = new SelectBuilder().linkto(
                        CIERP.Document2PaymentDocumentAbstract.ToAbstractLink).instance();
        final SelectBuilder selPaymentDocName = new SelectBuilder().linkto(
                        CIERP.Document2PaymentDocumentAbstract.ToAbstractLink)
                        .attribute(CIERP.PaymentDocumentAbstract.Name);
        taxMulti.addSelect(selTaxCurInst, selDocInst, selRateCurInst, selPaymentDocRate, selPaymentDocInst,
                        selPaymentDocName);
        taxMulti.executeWithoutAccessCheck();
        while (taxMulti.next()) {
            final Instance docInst = taxMulti.getSelect(selDocInst);
            final Instance payDocInst = taxMulti.getSelect(selPaymentDocInst);
            final DocPaymentInfo_Base info = instance2info.get(docInst);
            final Instance rateCurInst = taxMulti.getSelect(selRateCurInst);
            final Instance curInst = taxMulti.getSelect(selTaxCurInst);
            final DateTime dateTmp = taxMulti.getAttribute(CIERP.Document2PaymentDocumentAbstract.Date);
            BigDecimal amount = taxMulti.getAttribute(CIERP.Document2PaymentDocumentAbstract.Amount);
            final String name = taxMulti.getSelect(selPaymentDocName);

            final RateInfo rateInfo;
            // payment was in the same currency
            if (curInst.equals(rateCurInst)) {
                // the applied currency was the base currency
                if (Currency.getBaseCurrency().equals(rateCurInst)) {
                    rateInfo = RateInfo.getRateInfo(taxMulti.<Object[]>getAttribute(
                                    CIERP.Document2PaymentDocumentAbstract.Rate));
                } else {
                    rateInfo = RateInfo.getRateInfo(taxMulti.<Object[]>getSelect(selPaymentDocRate));
                }
            } else {
                // different currencies use the one registered in the relation
                rateInfo = RateInfo.getRateInfo(taxMulti.<Object[]>getAttribute(
                                CIERP.Document2PaymentDocumentAbstract.Rate));
            }
            if (InstanceUtils.isKindOf(payDocInst, CISales.PaymentDocumentOutAbstract)) {
                amount = amount.negate();
            }
            info.payPos.add(new PayPos(dateTmp, amount, curInst)
                            .setTypeLabel(CISales.IncomingDetraction.getType().getLabel())
                            .setName(name)
                            .setRateInfo(rateInfo));
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
        final Map<Instance, DocPaymentInfo_Base> instance2info = DocPaymentInfo_Base.getInfoMap(_parameter, _infos);

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
        final SelectBuilder selRateCurInst = new SelectBuilder().linkto(CISales.IncomingRetention.RateCurrencyId)
                        .instance();
        final SelectBuilder selRateObj = new SelectBuilder().linkto(CISales.IncomingRetention.Rate);
        final SelectBuilder selDocInst = SelectBuilder.get()
                        .linkfrom(CISales.IncomingRetention2IncomingInvoice.FromLink)
                        .linkto(CISales.IncomingRetention2IncomingInvoice.ToLink).instance();
        final SelectBuilder selPaymentDocInst = new SelectBuilder().linkto(
                        CIERP.Document2PaymentDocumentAbstract.ToAbstractLink).instance();
        final SelectBuilder selPaymentDocName = new SelectBuilder().linkto(
                        CIERP.Document2PaymentDocumentAbstract.ToAbstractLink)
                        .attribute(CIERP.PaymentDocumentAbstract.Name);
        retMulti.addSelect(retSelCur, selDocInst, selRateCurInst, selRateObj, selPaymentDocInst, selPaymentDocName);
        retMulti.executeWithoutAccessCheck();
        while (retMulti.next()) {
            final Instance docInst = retMulti.getSelect(selDocInst);
            retMulti.getSelect(selPaymentDocInst);
            final DocPaymentInfo_Base info = instance2info.get(docInst);
            final Instance curInst = retMulti.getSelect(retSelCur);
            final DateTime dateTmp = retMulti.getAttribute(CISales.IncomingRetention.Date);
            final BigDecimal amount = retMulti.getAttribute(CISales.IncomingRetention.CrossTotal);
            final String name = retMulti.getSelect(selPaymentDocName);

            final RateInfo rateInfo = RateInfo.getRateInfo(retMulti.<Object[]>getSelect(selRateObj));
            info.payPos.add(new PayPos(dateTmp, amount, curInst)
                            .setTypeLabel(CISales.IncomingRetention.getType().getLabel())
                            .setName(name)
                            .setRateInfo(rateInfo));
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
        final Map<Instance, DocPaymentInfo_Base> instance2info = DocPaymentInfo_Base.getInfoMap(_parameter, _infos);

        final QueryBuilder swapQueryBldr = new QueryBuilder(CISales.Document2Document4Swap);
        swapQueryBldr.setOr(true);
        swapQueryBldr.addWhereAttrEqValue(CISales.Document2Document4Swap.FromAbstractLink,
                        instance2info.keySet().toArray());
        swapQueryBldr.addWhereAttrEqValue(CISales.Document2Document4Swap.ToAbstractLink,
                        instance2info.keySet().toArray());
        final Set<Instance> verifySet = new HashSet<>();
        final MultiPrintQuery swapMulti = swapQueryBldr.getPrint();
        swapMulti.addAttribute(CISales.Document2Document4Swap.Amount, CISales.Document2Document4Swap.Date);
        final SelectBuilder selCurInst = new SelectBuilder()
                        .linkto(CISales.Document2Document4Swap.CurrencyLink).instance();
        final SelectBuilder selDocFrom = SelectBuilder.get().linkto(CISales.Document2Document4Swap.FromLink);
        final SelectBuilder selDocFromInst = new SelectBuilder(selDocFrom).instance();
        final SelectBuilder selDocFromStatus = new SelectBuilder(selDocFrom).status().key();
        final SelectBuilder selDocFromRate = new SelectBuilder(selDocFrom).attribute(CISales.DocumentSumAbstract.Rate);

        final SelectBuilder selDocTo = SelectBuilder.get().linkto(CISales.Document2Document4Swap.ToLink);
        final SelectBuilder selDocToInst = new SelectBuilder(selDocTo).instance();
        final SelectBuilder selDocToName = new SelectBuilder(selDocTo).attribute(CIERP.DocumentAbstract.Name);
        final SelectBuilder selDocToStatus = new SelectBuilder(selDocTo).status().key();
        final SelectBuilder selDocToRate = new SelectBuilder(selDocTo).attribute(CISales.DocumentSumAbstract.Rate);

        swapMulti.addSelect(selCurInst, selDocFromInst, selDocFromRate, selDocToInst, selDocToName, selDocFromStatus,
                        selDocToStatus, selDocToRate);
        swapMulti.executeWithoutAccessCheck();
        while (swapMulti.next()) {
            if (!verifySet.contains(swapMulti.getCurrentInstance())) {
                verifySet.add(swapMulti.getCurrentInstance());
                final Instance docFromInst = swapMulti.getSelect(selDocFromInst);
                final Instance docToInst = swapMulti.getSelect(selDocToInst);
                final String docToName = swapMulti.getSelect(selDocToName);
                final String keyFrom = swapMulti.getSelect(selDocFromStatus);
                final String keyTo = swapMulti.getSelect(selDocToStatus);
                if (instance2info.containsKey(docFromInst)
                                && DocPaymentInfo_Base.isValidStatus4Swap(_parameter, docFromInst, keyFrom, true)
                                && DocPaymentInfo_Base.isValidStatus4Swap(_parameter, docToInst, keyTo, false)) {
                    final DocPaymentInfo_Base info = instance2info.get(docFromInst);
                    final Instance curInst = swapMulti.getSelect(selCurInst);
                    final RateInfo docRateInfo = new Currency().evaluateRateInfo(info.getParameter(),
                                    swapMulti.<Object[]>getSelect(selDocFromRate));
                    RateInfo rateInfo = null;
                    if (docRateInfo.getCurrencyInstance().equals(curInst)) {
                        rateInfo = docRateInfo;
                    } else {
                        rateInfo = new Currency().evaluateRateInfo(info.getParameter(),
                                        swapMulti.<Object[]>getSelect(selDocToRate));
                        if (!rateInfo.getTargetCurrencyInstance().equals(docRateInfo.getCurrencyInstance())) {
                            rateInfo.setTargetCurrencyInstance(docRateInfo.getCurrencyInstance());

                            rateInfo.setRate(rateInfo.getRate().divide(docRateInfo.getRate(), RoundingMode.HALF_UP));
                            rateInfo.setSaleRate(rateInfo.getSaleRate().divide(docRateInfo.getSaleRate(),
                                            RoundingMode.HALF_UP));

                            if (rateInfo.isInvert()) {
                                rateInfo.setRateUI(rateInfo.getRateUI().multiply(docRateInfo.getRateUI()));
                                rateInfo.setSaleRateUI(rateInfo.getSaleRateUI().multiply(docRateInfo.getSaleRateUI()));
                            } else {
                                rateInfo.setRateUI(rateInfo.getRate());
                                rateInfo.setSaleRateUI(rateInfo.getSaleRate());
                            }
                        }
                    }
                    BigDecimal amount = swapMulti.getAttribute(CISales.Document2Document4Swap.Amount);
                    // for the case of obligations the amount is negated
                    if (info.isObligationDoc()) {
                        amount = amount.negate();
                    }
                    final DateTime swapDate = swapMulti.getAttribute(CISales.Document2Document4Swap.Date);
                    final DateTime posDate = swapDate == null ? info.date : swapDate;
                    info.payPos.add(new PayPos(posDate, amount, curInst)
                                    .setTypeLabel(docToInst.getType().getLabel())
                                    .setName(docToName)
                                    .setRateInfo(rateInfo));
                }
            }
        }
    }

    /**
     * Checks if is valid status for swap.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst the doc inst
     * @param _statusKey the status key
     * @param _from the from
     * @return true, if is valid status for swap
     * @throws EFapsException on error
     */
    protected static boolean isValidStatus4Swap(final Parameter _parameter,
                                                final Instance _docInst,
                                                final String _statusKey,
                                                final boolean _from)
        throws EFapsException
    {
        boolean ret = false;
        final Properties props = PropertiesUtil.getProperties4Prefix(Sales.PAYMENT_RULES.get(),
                        _docInst.getType().getName());

        final Collection<String> stats = PropertiesUtil.analyseProperty(props, "Swap.Status4" + (_from ? "From" : "To"),
                        0).values();
        if (stats.isEmpty()) {
            ret = !"Canceled".equals(_statusKey) && !"Replaced".equals(_statusKey);
        } else {
            ret = stats.contains(_statusKey);
        }
        return ret;
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
     * Gets the detailed info.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst the doc inst
     * @return the detailed info
     * @throws EFapsException on error
     */
    public static CharSequence getInfoHtml(final Parameter _parameter,
                                           final Instance _docInst)
        throws EFapsException
    {
        final StringBuilder ret = new StringBuilder();
        final DocPaymentInfo payInfo = new DocPaymentInfo(_docInst).setTargetDocInst(_docInst);

        final List<Instance> currencyInstances = getCurrencies();

        ret.append("<div style=\"display:flex;flex-wrap:wrap;\">");

        ret.append("<div style=\"margin:10px;\">")
            .append("<h3>").append(getLabel("PerPayment")).append("</h3>");
        for (final Instance currencyInstance : currencyInstances) {
            ret.append(getTable(_parameter, payInfo, CurrencyInst.get(currencyInstance), true));
        }
        ret.append("</div>");

        ret.append("<div style=\"margin:10px;\">")
            .append("<h3>").append(getLabel("PerDoc")).append("</h3>");
        for (final Instance currencyInstance : currencyInstances) {
            ret.append(getTable(_parameter, payInfo, CurrencyInst.get(currencyInstance), false));
        }
        ret.append("</div>");
        ret.append("</div>");
        return ret;
    }

    protected static List<Instance> getCurrencies() throws EFapsException {
        final Instance baseCurrencyInstance = Currency.getBaseCurrency();
        final List<Instance> currencyInstances = (List<Instance>) Currency.getAvailable();
        currencyInstances.remove(baseCurrencyInstance);

        Collections.sort(currencyInstances, (i1,i2) -> {
            try {
                return CurrencyInst.get(i1).getName().compareTo(CurrencyInst.get(i2).getName());
            } catch (final EFapsException e) {
                Log.error("Catched", e);
            }
            return 0;
        });
        currencyInstances.add(0, baseCurrencyInstance);
        return currencyInstances;
    }

    protected static CharSequence getTable(final Parameter _parameter,
                                           final DocPaymentInfo _payInfo,
                                           final CurrencyInst _currencyInst,
                                           final boolean _perPayment)
        throws EFapsException
    {
        final DecimalFormat formatter = NumberFormatter.get().getFrmt4Total(_parameter.getInstance().getType());

        final Table table = new Table()
            .setStyle("width:100%;")
            .addRow()
                .addHeaderColumn(getLabel("LabelColumn"), 4)
                .addHeaderColumn(getLabel("AmountColumn"))
            .addRow()
                .addColumn(_currencyInst.getName(), "font-weight:bold;font-size: 120%;")
            .addRow()
                .addColumn(getLabel("CrossTotal"), "font-weight:bold", 4)
                .addColumn(frmtAmount(formatter, _payInfo.getCrossTotalInCurrency(_parameter,
                                _currencyInst.getInstance()), _currencyInst), "text-align: right;");

        for (final PayPos payPos  : _payInfo.getPayPos()) {
            table.addRow()
                .addColumn(payPos.getTypeLabel())
                .addColumn(payPos.getName())
                .addColumn(payPos.getDate().toString(DateTimeFormat.mediumDate()
                                .withLocale(Context.getThreadContext().getLocale())));

            if (!payPos.getCurrencyInstance().equals(_currencyInst.getInstance())) {
                // add the payment in its currency of origin
                table.addColumn(frmtAmount(formatter, payPos.getAmount(), payPos.getCurrencyInstance()),
                                    "text-align: right;");
                // add the payment in the target currency
                table.addColumn(frmtAmount(formatter, payPos.getAmountInCurrency(_parameter,
                                _currencyInst.getInstance(), _perPayment), _currencyInst), "text-align: right;");
            } else {
                table.getCurrentColumn().setColSpan(2)
                    .getCurrentTable()
                    .addColumn(frmtAmount(formatter, payPos.getAmount(),_currencyInst), "text-align: right;");
            }
        }
        table
            .addRow()
                .addColumn(getLabel("Paid"), "font-weight:bold", 4)
                .addColumn(frmtAmount(formatter, _payInfo.getPaidInCurrency(_currencyInst.getInstance(), _perPayment),
                                _currencyInst), "text-align: right;")
            .addRow()
                .addColumn(getLabel("Balance"), "font-weight:bold;text-align:right", 4)
                .addColumn(frmtAmount(formatter, _payInfo.getBalanceInCurrency(_currencyInst.getInstance(),
                                _perPayment), _currencyInst), "text-align: right;");
        return table.toHtml();
    }

    private static String getLabel(final String _key)
    {
        return DBProperties.getProperty(DocPaymentInfo.class.getName() +  "." + _key);
    }

    private static String frmtAmount(final DecimalFormat _formatter,
                                     final BigDecimal _amount,
                                     final Instance _currencyInstance)
        throws EFapsException
    {
        return frmtAmount(_formatter, _amount, CurrencyInst.get(_currencyInstance));
    }

    private static String frmtAmount(final DecimalFormat _formatter,
                                     final BigDecimal _amount,
                                     final CurrencyInst _currencyInst)
        throws EFapsException
    {
        return _formatter.format(_amount) + " " + _currencyInst.getSymbol();
    }

    /**
     * Gets the info value.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst the doc inst
     * @return the info value
     * @throws EFapsException on error
     */
    public static CharSequence getInfoValue(final Parameter _parameter,
                                            final Instance _docInst)
        throws EFapsException
    {
        final DocPaymentInfo payInfo = new DocPaymentInfo(_docInst)
                        .setTargetDocInst(_docInst);
        return payInfo.getInfoField();
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
         * Instance of the Currency the amount is in
         */
        private final Instance currencyInstance;

        /** The label. */
        private String typeLabel;

        /** The label. */
        private String name;

        /** The rate info. */
        private RateInfo rateInfo;

        /**
         * Instantiates a new pay pos.
         *
         * @param _date Date of he Payment
         * @param _amount Amount of the Payment
         * @param _curInst instance of the Currency
         */
        public PayPos(final DateTime _date,
                      final BigDecimal _amount,
                      final Instance _curInst)
        {
            date = _date;
            amount = _amount;
            currencyInstance = _curInst;
        }

        /**
         * Getter method for the instance variable {@link #date}.
         *
         * @return value of instance variable {@link #date}
         */
        public DateTime getDate()
        {
            return date;
        }

        /**
         * Getter method for the instance variable {@link #amount}.
         *
         * @return value of instance variable {@link #amount}
         */
        public BigDecimal getAmount()
        {
            return amount;
        }

        /**
         * Get the amount converted into the the given target currency.
         * PerPayment true means use the rate registered in the payment
         * else uses the registered exchange rate.
         * @param _parameter Parameter
         * @param _targetCurrency currency to convert into
         * @return converted crossTotal
         * @throws EFapsException on error
         */
        public BigDecimal getAmountInCurrency(final Parameter _parameter,
                                              final Instance _targetCurrency,
                                              final boolean _perPayment)
              throws EFapsException
        {
            BigDecimal ret;
            if (getCurrencyInstance().equals(_targetCurrency)) {
                ret = getAmount();
            } else if (_perPayment && getRateInfo().getCurrencyInstance().equals(_targetCurrency)) {
                ret = getAmount().multiply(getRateInfo().getRate());
            } else if (_perPayment && getRateInfo().getTargetCurrencyInstance().equals(_targetCurrency)) {
                ret = getRateInfo().getCurrencyInstObj().isInvert()
                                ? getAmount().multiply(getRateInfo().getRateUI())
                                : getAmount().divide(getRateInfo().getRate(), RoundingMode.HALF_UP);
            } else {
                ret = Currency.convert(_parameter, getAmount(), getCurrencyInstance(),
                                    _targetCurrency, "WHAT_SHOULD_I_USED",
                            LocalDate.of(getDate().getYear(), getDate().getMonthOfYear(), getDate().getDayOfMonth()));
            }
            return ret;
        }

        /**
         * Getter method for the instance variable {@link #currencyid}.
         *
         * @return value of instance variable {@link #currencyid}
         */
        public Instance getCurrencyInstance()
        {
            return currencyInstance;
        }

        /**
         * Gets the label.
         *
         * @return the label
         */
        public String getTypeLabel()
        {
            return typeLabel;
        }

        /**
         * Sets the label.
         *
         * @param _label the label
         * @return the pay pos
         */
        public PayPos setTypeLabel(final String _label)
        {
            typeLabel = _label;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #name}.
         *
         * @return value of instance variable {@link #name}
         */
        public String getName()
        {
            return name;
        }

        /**
         * Setter method for instance variable {@link #name}.
         *
         * @param _name value for instance variable {@link #name}
         * @return the pay pos
         */
        public PayPos setName(final String _name)
        {
            name = _name;
            return this;
        }

        /**
         * Gets the rate info.
         *
         * @return the rate info
         */
        public RateInfo getRateInfo()
        {
            return rateInfo;
        }

        /**
         * Sets the rate info.
         *
         * @param _rateInfo the rate info
         * @return the pay pos
         */
        public PayPos setRateInfo(final RateInfo _rateInfo)
        {
            rateInfo = _rateInfo;
            return this;
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
            instance = _instance;
        }

        /**
         * Gets the currency instance.
         *
         * @return Instance of the currency
         * @throws EFapsException the e faps exception
         */
        public Instance getCurrencyInstance()
            throws EFapsException
        {
            if (currencyInstance == null) {
                currencyInstance = Currency.getBaseCurrency();
            }
            return currencyInstance;
        }

        /**
         * Gets the currency inst.
         *
         * @return CurrencyInst object for the currency of this account
         * @throws EFapsException the e faps exception
         */
        public CurrencyInst getCurrencyInst()
            throws EFapsException
        {
            if (currencyInst == null) {
                currencyInst = CurrencyInst.get(getCurrencyInstance());
            }
            return currencyInst;
        }

        /**
         * Getter method for the instance variable {@link #instance}.
         *
         * @return value of instance variable {@link #instance}
         */
        public Instance getInstance()
        {
            return instance;
        }

        /**
         * Setter method for instance variable {@link #currencyInstance}.
         *
         * @param _currencyInstance value for instance variable {@link #currencyInstance}
         */
        public void setCurrencyInstance(final Instance _currencyInstance)
        {
            currencyInstance = _currencyInstance;
        }

        /**
         * Setter method for instance variable {@link #currencyInst}.
         *
         * @param _currencyInst value for instance variable {@link #currencyInst}
         */
        public void setCurrencyInst(final CurrencyInst _currencyInst)
        {
            currencyInst = _currencyInst;
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
                final PrintQuery print = new PrintQuery(getInstance());
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
                final PrintQuery print = new PrintQuery(getInstance());
                print.addSelect(selAccCurInst);
                print.execute();
                setCurrencyInstance(print.<Instance>getSelect(selAccCurInst));
                setCurrencyInst(CurrencyInst.get(getCurrencyInstance()));
            }
        }
    }
}
