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
     * rate that will be applied at the document.
     */
    private Object[] rateTarget;

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

    private AccountInfo accountInfo;

    /**
     * Name of the document.
     */
    private String name;

    private String contactName;

    private Parameter parameter;

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
            this.initialized = true;
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
    public BigDecimal getCrossTotal4Account()
        throws EFapsException
    {
        initialize();
        BigDecimal ret = BigDecimal.ZERO;
        if (getAccountInfo().getCurrencyInstance().equals(getCurrencyInstance())) {
            ret = ret.add(getCrossTotal());
        } else if (getAccountInfo().getCurrencyInstance().equals(getRateCurrencyInstance())) {
            ret = ret.add(getRateCrossTotal());
        } else {
            final RateInfo[] rateInfos = new Currency().evaluateRateInfos(getParameter(),
                            getDate(), getRateCurrencyInstance(), getAccountInfo().getCurrencyInstance());
            ret = ret.add(getRateCrossTotal().divide(rateInfos[2].getRate(), BigDecimal.ROUND_HALF_UP));
        }
        return ret;
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
     * @return the paid amount in the account currency
     * @throws EFapsException on error
     */
    public BigDecimal getPaid4Account()
        throws EFapsException
    {
        initialize();
        BigDecimal ret = BigDecimal.ZERO;
        for (final PayPos pos : this.payPos) {
            // the currency of the PaymentDocument is the same as the ratcurrency
            if (getAccountInfo().getCurrencyInstance().equals(pos.getCurrencyInstance())) {
                ret = ret.add(pos.getAmount());
            } else {
                final RateInfo[] rateInfos = new Currency().evaluateRateInfos(getParameter(),
                                getDate(), pos.getCurrencyInstance(), getAccountInfo().getCurrencyInstance());
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
    public void setAccountInst(final Instance _accInst)
        throws EFapsException
    {
        this.accountInfo = new AccountInfo(_accInst);
    }

    /**
     * Getter method for the instance variable {@link #rateTarget}.
     *
     * @return value of instance variable {@link #rateTarget}
     */
    public Object[] getRateTarget()
    {
        return this.rateTarget;
    }

    /**
     * Setter method for instance variable {@link #rateTarget}.
     *
     * @param _rateTarget value for instance variable {@link #rateTarget}
     */
    public void setRateTarget(final Object[] _rateTarget)
    {
        this.rateTarget = _rateTarget;
    }

    /**
     * Getter method for the instance variable {@link #name}.
     *
     * @return value of instance variable {@link #name}
     */
    public String getName()
    {
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
     */
    public AccountInfo getAccountInfo()
    {
        return this.accountInfo;
    }

    /**
     * Setter method for instance variable {@link #accountInfo}.
     *
     * @param _accountInfo value for instance variable {@link #accountInfo}
     */
    public void setAccountInfo(final AccountInfo _accountInfo)
    {
        this.accountInfo = _accountInfo;
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
        if (currInst.getInstance().equals(getAccountInfo().getCurrencyInstance())) {
            key = ".InfoField";
        } else {
            key = ".InfoField4Account";
            objects.add(getCrossTotal4Account());
            objects.add(getPaid4Account());
            objects.add(BigDecimal.ZERO);
            objects.add(BigDecimal.ZERO);
            objects.add(getAccountInfo().getCurrencyInst().getSymbol());
            objects.add(getAccountInfo().getCurrencyInst().getISOCode());
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
     */
    public RateInfo getRateInfo()
    {
        return this.rateInfo;
    }

    /**
     * Getter method for the instance variable {@link #rateInfo}.
     *
     * @return value of instance variable {@link #rateInfo}
     * @throws EFapsException on error
     */
    public RateInfo getRateInfo4Account()
        throws EFapsException
    {
        final RateInfo[] rateInfos = new Currency().evaluateRateInfos(getParameter(),
                        getDate(), getRateCurrencyInstance(), getAccountInfo().getCurrencyInstance());
        return rateInfos[2];
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
     */
    public DateTime getDate()
    {
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

    /**
     * Account info.
     */
    public static class AccountInfo
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
         * @param _instance insatcne of the account
         * @throws EFapsException on error
         */
        public AccountInfo(final Instance _instance)
            throws EFapsException
        {
            this.instance = _instance;
            if (this.instance.isValid()) {
                final SelectBuilder selAccCurInst = new SelectBuilder()
                                .linkto(CISales.AccountAbstract.CurrencyLink).instance();
                final PrintQuery print = new PrintQuery(this.instance);
                print.addSelect(selAccCurInst);
                print.execute();

                this.currencyInstance = print.<Instance>getSelect(selAccCurInst);
                this.currencyInst = CurrencyInst.get(this.currencyInstance);
            }
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
    }
}
