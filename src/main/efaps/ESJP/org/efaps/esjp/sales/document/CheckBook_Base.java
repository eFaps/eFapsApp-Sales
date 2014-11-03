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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.esjp.sales.Swap;
import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("f270918a-0c1c-4b83-b466-855412563b9d")
@EFapsRevision("$Rev$")
public abstract class CheckBook_Base
    extends CommonDocument
{

    /**
     * Key used for storing information during request.
     */
    protected static final String REQUESTKEY = CheckBook.class.getName() + ".Key4Request";

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return Return containing the instance
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Create create = new Create();
        return create.execute(_parameter);
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return empty Return
     * @throws EFapsException on error
     */
    public Return createRelations(final Parameter _parameter)
        throws EFapsException
    {
        final String startStr = _parameter.getParameterValue(CIFormSales.Sales_CheckBookCreateRelationForm.start.name);
        final String endStr = _parameter.getParameterValue(CIFormSales.Sales_CheckBookCreateRelationForm.end.name);

        final int start = Integer.parseInt(startStr);
        final int end = Integer.parseInt(endStr);
        for (int i = start; i < end; i++) {
            final Insert insert = new Insert(CISales.CheckBook2PaymentCheckOut);
            insert.add(CISales.CheckBook2PaymentCheckOut.Number, String.format("%06d", i));
            insert.add(CISales.CheckBook2PaymentCheckOut.FromLink, _parameter.getCallInstance());
            insert.add(CISales.CheckBook2PaymentCheckOut.ToLink, _parameter.getCallInstance());
            insert.execute();
        }
        return new Return();
    }

    /**
     * Method to set value deactivated.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return setDeactivated(final Parameter _parameter)
        throws EFapsException
    {
        final String[] others = (String[]) _parameter.get(ParameterValues.OTHERS);

        if (others != null) {
            for (final String other : others) {
                final Instance otherInst = Instance.get(other);
                if (otherInst.isValid()) {
                    if (containsProperty(_parameter, "Value")) {
                        final Update update = new Update(otherInst);
                        update.add(CISales.CheckBook2PaymentCheckOut.Value, getProperty(_parameter, "Value"));
                        update.executeWithoutTrigger();
                    }
                }
            }
        }
        return new Return();
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return getInfoDocumentFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final DocInfo info = getInfos(_parameter).get(_parameter.getInstance());
        return new Return().put(ReturnValues.VALUES, info == null ? "" : info.getDocument());
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @return mapping of infos
     * @throws EFapsException on error.
     */
    @SuppressWarnings("unchecked")
    protected Map<Instance, DocInfo> getInfos(final Parameter _parameter)
        throws EFapsException
    {
        Map<Instance, DocInfo> ret;
        if (Context.getThreadContext().containsRequestAttribute(CheckBook.REQUESTKEY)) {
            ret = (Map<Instance, DocInfo>) Context.getThreadContext().getRequestAttribute(CheckBook.REQUESTKEY);
        } else {
            final Instance callInst = _parameter.getCallInstance();
            final List<Instance> relInsts = (List<Instance>) _parameter.get(ParameterValues.REQUEST_INSTANCES);
            ret = getInfos(_parameter, callInst, relInsts);
            Context.getThreadContext().setRequestAttribute(CheckBook.REQUESTKEY, ret);
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @param _callInstance instance calling
     * @param _relInsts relation instances
     * @return mapping of infos
     * @throws EFapsException on error.
     */
    public static Map<Instance, DocInfo> getInfos(final Parameter _parameter,
                                                  final Instance _callInstance,
                                                  final List<Instance> _relInsts)
        throws EFapsException
    {
        final Map<Instance, DocInfo> ret = new HashMap<Instance, DocInfo>();
        final MultiPrintQuery multi = new MultiPrintQuery(_relInsts);
        final SelectBuilder toSel = SelectBuilder.get().linkto(CISales.CheckBook2PaymentCheckOut.ToLink);
        final SelectBuilder toInstSel = new SelectBuilder(toSel).instance();
        final SelectBuilder toNameSel = new SelectBuilder(toSel).attribute(CISales.DocumentSumAbstract.Name);
        final SelectBuilder toStatusSel = new SelectBuilder(toSel)
                        .attribute(CISales.DocumentSumAbstract.StatusAbstract);
        final SelectBuilder toAmountSel = new SelectBuilder(toSel).attribute(CISales.PaymentDocumentIOAbstract.Amount);
        final SelectBuilder toSymbolSel = new SelectBuilder(toSel).linkto(
                        CISales.PaymentDocumentIOAbstract.RateCurrencyLink)
                        .attribute(CIERP.Currency.Symbol);
        multi.addSelect(toInstSel, toNameSel, toStatusSel, toAmountSel, toSymbolSel);
        multi.execute();
        while (multi.next()) {
            DocInfo info = new DocInfo();
            final Instance toInst = multi.getSelect(toInstSel);
            if (!_callInstance.equals(toInst)) {
                info.setFrom(true)
                                .setDocInstance(multi.<Instance>getSelect(toInstSel))
                                .setDocName(multi.<String>getSelect(toNameSel))
                                .setStatusName(Status.get(multi.<Long>getSelect(toStatusSel)))
                                .setAmount(multi.<BigDecimal>getSelect(toAmountSel))
                                .setSymbol(multi.<String>getSelect(toSymbolSel));
            } else {
                info = null;
            }
            ret.put(multi.getCurrentInstance(), info);
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @return mapping of infos
     * @throws EFapsException on error.
     */
    public Return getInfoStatusDocumentFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final DocInfo info = getInfos(_parameter).get(_parameter.getInstance());
        return new Return().put(ReturnValues.VALUES, info == null ? "" : info.getStatusName());
    }
    /**
     * @param _parameter Parameter as passed from the eFaps API.
     * @return mapping of infos
     * @throws EFapsException on error.
     */
    public Return getInfoValueDocumentFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final DocInfo info = getInfos(_parameter).get(_parameter.getInstance());
        return new Return().put(ReturnValues.VALUES,
                        info == null ? getValue(_parameter.getInstance()) : info.getAmount());
    }

    /**
     * @param _instance Instance
     * @return value
     * @throws EFapsException on error
     */
    protected String getValue(final Instance _instance)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_instance);
        print.addAttribute(CISales.CheckBook2PaymentCheckOut.Value);
        print.execute();
        return print.<String>getAttribute(CISales.CheckBook2PaymentCheckOut.Value);
    }

    /**
     *Info class.
     */
    public static class DocInfo
    {
        /**
         * Instance of the document.
         */
        private Instance docInst;

        /**
         * Name of the document.
         */
        private String docName;

        /**
         * From or to.
         */
        private boolean from = false;

        /**
         * Status of the Document.
         */
        private Status status;

        /**
         * Amount of the Document.
         */
        private BigDecimal amount;

        /**
         * Symbol of the Document.
         */
        private String symbol;

        /**
         * @return direction string
         */
        public String getDirection()
        {
            return DBProperties.getProperty(Swap.class.getName() + ".Direction." + (this.from ? "from" : "to"));
        }

        /**
         * @return document string
         */
        public String getDocument()
        {
            return this.docInst.getType().getLabel() + " " + this.docName;
        }

        /**
         * @return document string
         */
        public String getStatusName()
        {
            return DBProperties.getProperty(this.status.getLabelKey());
        }

        /**
         * @return document string
         */
        public String getAmount()
        {
            return this.symbol + " " + this.amount;
        }

        /**
         * Setter method for instance variable {@link #docInst}.
         *
         * @param _docInst value for instance variable {@link #docInst}
         * @return this for chaining
         */
        public DocInfo setDocInstance(final Instance _docInst)
        {
            this.docInst = _docInst;
            return this;
        }

        /**
         * Setter method for instance variable {@link #docName}.
         *
         * @param _docName value for instance variable {@link #docName}
         * @return this for chaining
         */
        public DocInfo setDocName(final String _docName)
        {
            this.docName = _docName;
            return this;
        }

        /**
         * Setter method for instance variable {@link #from}.
         *
         * @param _from value for instance variable {@link #from}
         * @return this for chaining
         */
        public DocInfo setFrom(final boolean _from)
        {
            this.from = _from;
            return this;
        }

        /**
         * Setter method for instance variable {@link #status}.
         *
         * @param _status value for instance variable {@link #status}
         * @return this for chaining
         */
        public DocInfo setStatusName(final Status _status)
        {
            this.status = _status;
            return this;
        }

        /**
         * Setter method for instance variable {@link #amount}.
         *
         * @param _amount value for instance variable {@link #amount}
         * @return this for chaining
         */
        public DocInfo setAmount(final BigDecimal _amount)
        {
            this.amount = _amount;
            return this;
        }

        /**
         * Setter method for instance variable {@link #symbol}.
         *
         * @param _symbol value for instance variable {@link #symbol}
         * @return this for chaining
         */
        public DocInfo setSymbol(final String _symbol)
        {
            this.symbol = _symbol;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #docInst}.
         *
         * @return value of instance variable {@link #docInst}
         */
        public Instance getDocInst()
        {
            return this.docInst;
        }
    }
}


