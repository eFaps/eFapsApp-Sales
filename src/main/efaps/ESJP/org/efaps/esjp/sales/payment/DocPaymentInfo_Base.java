/*
 * Copyright 2003 - 2012 The eFaps Team
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

package org.efaps.esjp.sales.payment;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsRevision;
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
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: DocWithPayment_Base.java 8972 2013-02-25 18:33:09Z
 *          jan@moxter.net $
 */
@EFapsUUID("1f2a247a-717a-4285-a43a-d41e47200b0c")
@EFapsRevision("$Rev$")
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
    private final List<PayPos> payPos = new ArrayList<PayPos>();

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
    private TargetInfo targetInfo;

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
            final PrintQuery print = new PrintQuery(this.instance);
            final SelectBuilder selContactName = new SelectBuilder()
                            .linkto(CISales.DocumentAbstract.Contact).attribute(CIContacts.Contact.Name);

            final SelectBuilder selCurInst = new SelectBuilder().linkto(CISales.DocumentSumAbstract.CurrencyId)
                            .instance();
            final SelectBuilder selRateCurInst = new SelectBuilder().linkto(CISales.DocumentSumAbstract.RateCurrencyId)
                            .instance();
            print.addSelect(selCurInst, selRateCurInst, selContactName);
            print.addAttribute(CISales.DocumentAbstract.Name, CISales.DocumentAbstract.Date,
                            CISales.DocumentSumAbstract.RateCrossTotal, CISales.DocumentSumAbstract.Rate,
                            CISales.DocumentSumAbstract.CrossTotal);
            print.executeWithoutAccessCheck();
            this.crossTotal = print.getAttribute(CISales.DocumentSumAbstract.CrossTotal);
            this.rateCrossTotal = print.getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
            this.name = print.getAttribute(CISales.DocumentSumAbstract.Name);
            this.date = print.getAttribute(CISales.DocumentAbstract.Date);
            setRateInfo(new Currency().evaluateRateInfo(getParameter(),
                            print.<Object[]>getAttribute(CISales.DocumentSumAbstract.Rate)));
            this.currencyInstance = print.getSelect(selCurInst);
            this.rateCurrencyInstance = print.getSelect(selRateCurInst);
            this.contactName = print.getSelect(selContactName);

            //check normal payments
            final QueryBuilder queryBldr = new QueryBuilder(CIERP.Document2PaymentDocumentAbstract);
            queryBldr.addWhereAttrEqValue(CIERP.Document2PaymentDocumentAbstract.FromAbstractLink,
                            this.instance.getId());
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CIERP.Document2PaymentDocumentAbstract.Date,
                            CIERP.Document2PaymentDocumentAbstract.Amount);
            final SelectBuilder selCur2 = new SelectBuilder().linkto(
                            CIERP.Document2PaymentDocumentAbstract.CurrencyLink).oid();
            multi.addSelect(selCur2);
            multi.executeWithoutAccessCheck();
            while (multi.next()) {
                final Instance curInst = Instance.get(multi.<String>getSelect(selCur2));
                final DateTime dateTmp = multi.<DateTime>getAttribute(CIERP.Document2PaymentDocumentAbstract.Date);
                final BigDecimal amount = multi.<BigDecimal>getAttribute(CIERP.Document2PaymentDocumentAbstract.Amount);
                this.payPos.add(new PayPos(dateTmp, amount, curInst));
            }

            // check swap
            final QueryBuilder swapQueryBldr = new QueryBuilder(CISales.Document2Document4Swap);
            swapQueryBldr.setOr(true);
            swapQueryBldr.addWhereAttrEqValue(CISales.Document2Document4Swap.FromAbstractLink, this.instance);
            swapQueryBldr.addWhereAttrEqValue(CISales.Document2Document4Swap.ToAbstractLink, this.instance);

            final MultiPrintQuery swapMulti = swapQueryBldr.getPrint();
            swapMulti.addAttribute(CISales.Document2Document4Swap.Amount);
            final SelectBuilder selCur3 = new SelectBuilder()
                            .linkto(CISales.Document2Document4Swap.CurrencyLink).instance();
            swapMulti.addSelect(selCur3);
            swapMulti.executeWithoutAccessCheck();
            while (swapMulti.next()) {
                final Instance curInst = swapMulti.getSelect(selCur3);
                final BigDecimal amount = swapMulti.getAttribute(CISales.Document2Document4Swap.Amount);
                this.payPos.add(new PayPos(this.date, amount, curInst));
            }
            this.initialized = true;

            // check related taxdocs
            final QueryBuilder attrTaxQueryBldr = new QueryBuilder(CISales.IncomingDocumentTax2Document);
            attrTaxQueryBldr.addWhereAttrEqValue(CISales.IncomingDocumentTax2Document.ToAbstractLink, this.instance);

            final QueryBuilder taxQueryBldr = new QueryBuilder(CIERP.Document2PaymentDocumentAbstract);
            taxQueryBldr.addWhereAttrInQuery(CIERP.Document2PaymentDocumentAbstract.FromAbstractLink,
                            attrTaxQueryBldr.getAttributeQuery(CISales.IncomingDocumentTax2Document.FromAbstractLink));
            final MultiPrintQuery taxMulti = taxQueryBldr.getPrint();
            taxMulti.addAttribute(CIERP.Document2PaymentDocumentAbstract.Date,
                            CIERP.Document2PaymentDocumentAbstract.Amount);
            final SelectBuilder taxSelCur = new SelectBuilder().linkto(
                            CIERP.Document2PaymentDocumentAbstract.CurrencyLink).oid();
            taxMulti.addSelect(taxSelCur);
            taxMulti.executeWithoutAccessCheck();
            while (taxMulti.next()) {
                final Instance curInst = Instance.get(taxMulti.<String>getSelect(taxSelCur));
                final DateTime dateTmp = taxMulti.<DateTime>getAttribute(CIERP.Document2PaymentDocumentAbstract.Date);
                final BigDecimal amount = taxMulti
                                .<BigDecimal>getAttribute(CIERP.Document2PaymentDocumentAbstract.Amount);
                this.payPos.add(new PayPos(dateTmp, amount, curInst));
            }
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
        return this.crossTotal.subtract(getPaid()).compareTo(BigDecimal.ZERO) == 0;
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
     * @param _accInst instance of the account
     * @throws EFapsException on error
     *
     */
    public void setTargetDocInst(final Instance _accInst)
        throws EFapsException
    {
        this.targetInfo = new TargetDocInfo(_accInst);
    }

    /**
     * @param _accInst instance of the account
     * @throws EFapsException on error
     *
     */
    public void setAccountInst(final Instance _accInst)
        throws EFapsException
    {
        this.targetInfo = new AccountInfo(_accInst);
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
     */
    public void setName(final String _name)
    {
        this.name = _name;
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
    public TargetInfo getTargetInfo()
        throws EFapsException
    {
        initialize();
        return this.targetInfo;
    }

    /**
     * Setter method for instance variable {@link #accountInfo}.
     *
     * @param _accountInfo value for instance variable {@link #accountInfo}
     */
    public void setAccountInfo(final AccountInfo _accountInfo)
    {
        this.targetInfo = _accountInfo;
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

        String key;
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
     */
    public void setContactName(final String _contactName)
    {
        this.contactName = _contactName;
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
     */
    public void setRateInfo(final RateInfo _rateInfo)
    {
        this.rateInfo = _rateInfo;
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
     */
    public void setDate(final DateTime _date)
    {
        this.date = _date;
    }

    /**
     * Setter method for instance variable {@link #crossTotal}.
     *
     * @param _crossTotal value for instance variable {@link #crossTotal}
     */
    public void setCrossTotal(final BigDecimal _crossTotal)
    {
        this.crossTotal = _crossTotal;
    }

    /**
     * Setter method for instance variable {@link #rateCrossTotal}.
     *
     * @param _rateCrossTotal value for instance variable {@link #rateCrossTotal}
     */
    public void setRateCrossTotal(final BigDecimal _rateCrossTotal)
    {
        this.rateCrossTotal = _rateCrossTotal;
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


    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
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

    public abstract static class TargetInfo
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

        public TargetInfo(final Instance _instance)
            throws EFapsException
        {
            this.instance = _instance;
        }

        /**
         * @return Instance of the currency
         */
        protected Instance getCurrencyInstance()
        {
            return this.currencyInstance;
        }

        /**
         * @return CurrencyInst object for the currency of this account
         */
        protected CurrencyInst getCurrencyInst()
        {
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


    public static class TargetDocInfo
        extends TargetInfo
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
        extends TargetInfo
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
