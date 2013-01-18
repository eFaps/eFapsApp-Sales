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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.sales.PriceUtil;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("1f2a247a-717a-4285-a43a-d41e47200b0c")
@EFapsRevision("$Rev$")
public abstract class DocWithPayment_Base
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
    private Object[] rate;

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
     * @param _docInst instance of the document
     */
    public DocWithPayment_Base(final Instance _docInst)
    {
        this.instance = _docInst;
    }

    /**
     * Initialize the instance of this class.
     * @throws EFapsException on erro
     */
    public void initialize()
        throws EFapsException
    {
        if (!this.initialized) {
            final PrintQuery print = new PrintQuery(this.instance);
            final SelectBuilder selCur = new SelectBuilder().linkto(CISales.DocumentSumAbstract.CurrencyId).oid();
            final SelectBuilder selRateCur = new SelectBuilder().linkto(CISales.DocumentSumAbstract.RateCurrencyId)
                            .oid();
            print.addSelect(selCur, selRateCur);
            print.addAttribute(CISales.DocumentSumAbstract.RateCrossTotal, CISales.DocumentSumAbstract.Rate,
                            CISales.DocumentSumAbstract.CrossTotal);
            print.executeWithoutAccessCheck();
            this.crossTotal = print.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.CrossTotal);
            this.rateCrossTotal = print.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
            this.rate = print.<Object[]>getAttribute(CISales.DocumentSumAbstract.Rate);
            this.currencyInstance = Instance.get(print.<String>getSelect(selCur));
            this.rateCurrencyInstance = Instance.get(print.<String>getSelect(selRateCur));

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
                final DateTime date = multi.<DateTime>getAttribute(CIERP.Document2PaymentDocumentAbstract.Date);
                final BigDecimal amount = multi.<BigDecimal>getAttribute(CIERP.Document2PaymentDocumentAbstract.Amount);
                this.payPos.add(new PayPos(date, amount, curInst));
            }
            this.initialized = true;
        }
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
     */
    public BigDecimal getCrossTotal()
    {
        return this.crossTotal;
    }

    /**
     * Getter method for the instance variable {@link #rateCrossTotal}.
     *
     * @return value of instance variable {@link #rateCrossTotal}
     */
    public BigDecimal getRateCrossTotal()
    {
        return this.rateCrossTotal;
    }


    /**
     * Getter method for the instance variable {@link #rate}.
     *
     * @return value of instance variable {@link #rate}
     */
    public Object[] getRate()
    {
        return this.rate;
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
     */
    public List<PayPos> getPayPos()
    {
        return this.payPos;
    }

    /**
     * Getter method for the instance variable {@link #currencyInstance}.
     *
     * @return value of instance variable {@link #currencyInstance}
     */
    public Instance getCurrencyInstance()
    {
        return this.currencyInstance;
    }

    /**
     * Getter method for the instance variable {@link #rateCurrencyInstance}.
     *
     * @return value of instance variable {@link #rateCurrencyInstance}
     */
    public Instance getRateCurrencyInstance()
    {
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
        final PriceUtil priceUtil = new PriceUtil();
        for (final PayPos pos : this.payPos) {
            // the currency of the PaymentDocument is the same as the base
            // currency
            if (pos.getCurrencyInstance().equals(this.currencyInstance)) {
                ret = ret.add(pos.getAmount());
            } else {
                final BigDecimal[] rates = priceUtil.getRates(pos.getDate(), this.currencyInstance,
                                pos.getCurrencyInstance());
                ret = ret.add(pos.getAmount().multiply(rates[2]));
            }
        }
        return ret;
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
         * @param _date  Date of he Payment
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
}
