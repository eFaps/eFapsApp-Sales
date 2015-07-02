/*
 * Copyright 2003 - 2010 The eFaps Team
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.comparators.ComparatorChain;
import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.EFapsMapDataSource;
import org.efaps.esjp.common.jasperreport.StandartReport;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.Rate;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.esjp.sales.PriceUtil;
import org.efaps.ui.wicket.util.DateUtil;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.DateTimeUtil;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperReport;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("8e6fa637-760a-48ab-8bb9-bf643f4d65d1")
@EFapsRevision("$Rev$")
public abstract class DocReport_Base
    extends EFapsMapDataSource
{

    /**
     * Logger for this class.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(DocReport_Base.class);

    /**
     * Enum used to define the keys for the map.
     */
    public enum Field
    {
        /** Date of the report (used only in accounting for ple). */
        REPORT_DATE("report_date"),
        /** */
        DOC_DATE("date"),
        /** */
        DOC_DUEDATE("dueDate"),
        /** */
        DOC_DOCTYPE("documentType"),
        /** */
        DOC_NAME("name"),
        /** */
        DOC_SN("docSerialNo"),
        /** */
        DOC_NUMBER("docNumber"),
        /** */
        DOC_TAXNUM("taxNumber"),
        /** */
        DOC_CONTACT("contact"),
        /** */
        DOC_CROSSTOTAL("crossTotal"),
        /** */
        DOC_NETTOTAL("netTotal"),
        /** */
        DOC_IGV("igv"),
        /** */
        DOC_EXPORT("export"),
        /** */
        DOC_RATE("rate"),
        /** */
        DOCREL_DATE("derivedDocumentDate"),
        /** */
        DOCREL_TYPE("derivedDocumentType"),
        /** */
        DOCREL_PREFNAME("derivedDocumentPreffixName"),
        /** */
        DOCREL_SUFNAME("derivedDocumentSuffixName");

        /**
         * key.
         */
        private final String key;

        /**
         * @param _key key
         */
        private Field(final String _key)
        {
            this.key = _key;
        }

        /**
         * Getter method for the instance variable {@link #key}.
         *
         * @return value of instance variable {@link #key}
         */
        public String getKey()
        {
            return this.key;
        }
    }

    /**
     * Mapping for No es defined by SUNAT.
     */
    protected static final Map<Long, String> DOCTYPE_MAP = new HashMap<Long, String>();
    static {
        DocReport_Base.DOCTYPE_MAP.put(CISales.Invoice.getType().getId(), "01");
        DocReport_Base.DOCTYPE_MAP.put(CISales.Receipt.getType().getId(), "02");
        DocReport_Base.DOCTYPE_MAP.put(CISales.CreditNote.getType().getId(), "07");
        DocReport_Base.DOCTYPE_MAP.put(CISales.Reminder.getType().getId(), "08");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void init(final JasperReport _jasperReport,
                     final Parameter _parameter,
                     final JRDataSource _parentSource,
                     final Map<String, Object> _jrParameters)
        throws EFapsException
    {
        final String dateFromStr = Context.getThreadContext().getParameter("dateFrom");
        final String dateToStr = Context.getThreadContext().getParameter("dateTo");
        final String currency = _parameter.getParameterValue("currency");
        final Long rateCurType = Long.parseLong(_parameter.getParameterValue("rateCurrencyType"));
        final boolean active = Boolean.parseBoolean(_parameter.getParameterValue("filterActive"));
        final CurrencyInst curInst = new CurrencyInst(Instance.get(CIERP.Currency.getType(), currency));

        final Instance curBase = Currency.getBaseCurrency();

        final DateTime from = DateTimeUtil.normalize(new DateTime(dateFromStr));
        final DateTime to = DateTimeUtil.normalize(new DateTime(dateToStr));

        final List<Map<String, Object>> values = new ArrayList<Map<String, Object>>();
        final List<Instance> instances = getInstances(_parameter, from, to);

        if (instances.size() > 0) {
            final MultiPrintQuery multiPrint = new MultiPrintQuery(instances);
            multiPrint.addAttribute(CISales.DocumentSumAbstract.CrossTotal,
                            CISales.DocumentSumAbstract.NetTotal,
                            CISales.DocumentSumAbstract.RateCrossTotal,
                            CISales.DocumentSumAbstract.RateNetTotal,
                            CISales.DocumentSumAbstract.Rate,
                            CISales.DocumentSumAbstract.Name,
                            CISales.DocumentSumAbstract.Date,
                            CISales.DocumentSumAbstract.DueDate,
                            CISales.DocumentSumAbstract.StatusAbstract);
            final SelectBuilder selCurInst = new SelectBuilder()
                            .linkto(CISales.DocumentSumAbstract.RateCurrencyId).instance();
            final SelectBuilder selContactName = new SelectBuilder()
                            .linkto(CISales.DocumentSumAbstract.Contact)
                            .attribute(CIContacts.Contact.Name);
            final SelectBuilder selContactTaxNum = new SelectBuilder()
                            .linkto(CISales.DocumentSumAbstract.Contact)
                            .clazz(CIContacts.ClassOrganisation)
                            .attribute(CIContacts.ClassOrganisation.TaxNumber);
            final SelectBuilder selDocRelInst = new SelectBuilder()
                            .linkfrom(CISales.Document2DerivativeDocument, CISales.Document2DerivativeDocument.To)
                            .linkto(CISales.Document2DerivativeDocument.From).instance();
            final SelectBuilder selDocRelName = new SelectBuilder()
                            .linkfrom(CISales.Document2DerivativeDocument, CISales.Document2DerivativeDocument.To)
                            .linkto(CISales.Document2DerivativeDocument.From)
                            .attribute(CISales.DocumentSumAbstract.Name);
            final SelectBuilder selDocRelDate = new SelectBuilder()
                            .linkfrom(CISales.Document2DerivativeDocument, CISales.Document2DerivativeDocument.To)
                            .linkto(CISales.Document2DerivativeDocument.From)
                            .attribute(CISales.DocumentSumAbstract.Date);
            multiPrint.addSelect(selCurInst, selContactName, selContactTaxNum,
                            selDocRelDate, selDocRelInst, selDocRelName);
            multiPrint.execute();

            while (multiPrint.next()) {
                final Map<String, Object> map = new HashMap<String, Object>();
                final Instance instDoc = multiPrint.getCurrentInstance();
                final String docName = multiPrint.<String>getAttribute(CISales.DocumentSumAbstract.Name);
                final DateTime docDate = multiPrint.<DateTime>getAttribute(CISales.DocumentSumAbstract.Date);
                final DateTime docDueDate = multiPrint.<DateTime>getAttribute(CISales.DocumentSumAbstract.DueDate);
                final String contactName = multiPrint.<String>getSelect(selContactName);
                final String contactTaxNum = multiPrint.<String>getSelect(selContactTaxNum);
                final Instance docRelInst = multiPrint.<Instance>getSelect(selDocRelInst);
                final String docRelName = multiPrint.<String>getSelect(selDocRelName);
                final DateTime docRelDate = multiPrint.<DateTime>getSelect(selDocRelDate);
                final CurrencyInst curInstDoc = new CurrencyInst(multiPrint.<Instance>getSelect(selCurInst));
                final Long status = multiPrint.<Long>getAttribute(CISales.DocumentSumAbstract.StatusAbstract);
                final Boolean canceled = new Boolean(status.equals(new Long(Status.find(CISales.InvoiceStatus.uuid,
                                                "Replaced").getId()))
                                || status.equals(new Long(Status.find(CISales.ReminderStatus.uuid,
                                                "Replaced").getId()))
                                || status.equals(new Long(Status.find(CISales.CreditNoteStatus.uuid,
                                                "Replaced").getId())));

                DateTime date = docDate;

                final boolean negate = instDoc.getType().isKindOf(CISales.CreditNote.getType());

                if (instDoc.getType().isKindOf(CISales.CreditNote.getType())
                                || instDoc.getType().isKindOf(CISales.Reminder.getType())) {
                    DocReport_Base.LOG.debug("Document '{}' related with document '{}'", docName, docRelName);
                    date = docRelDate;
                }

                BigDecimal netTotal = BigDecimal.ZERO;
                BigDecimal crossTotal = BigDecimal.ZERO;
                BigDecimal igv = BigDecimal.ZERO;
                if (active) {
                    if (curInstDoc.getInstance().getId() != curInst.getInstance().getId()) {
                        if (curInstDoc.getInstance().getId() != curBase.getId()) {
                            final Rate rateTmp = getRates4DateRange(curInstDoc.getInstance(), date, rateCurType);
                            final BigDecimal netTotalTmp = multiPrint
                                            .<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateNetTotal)
                                                        .multiply(rateTmp.getLabel());
                            final BigDecimal crossTotalTmp = multiPrint
                                            .<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal)
                                                        .multiply(rateTmp.getLabel());

                            final Rate rate = getRates4DateRange(curInst.getInstance(), date, rateCurType);
                            map.put(DocReport_Base.Field.DOC_RATE.getKey(), rateTmp.getLabel());
                            netTotal = negate ? netTotalTmp.multiply(rate.getLabel()).negate()
                                              : netTotalTmp.multiply(rate.getLabel());
                            crossTotal = negate ? crossTotalTmp.multiply(rate.getLabel()).negate()
                                                : crossTotalTmp.multiply(rate.getLabel());
                        } else {
                            final Rate rate = getRates4DateRange(curInst.getInstance(), date, rateCurType);
                            map.put(DocReport_Base.Field.DOC_RATE.getKey(), rate.getLabel());
                            netTotal = negate
                                        ? multiPrint.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateNetTotal)
                                                        .multiply(rate.getValue()).negate()
                                        : multiPrint.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateNetTotal)
                                                        .multiply(rate.getValue());
                            crossTotal = negate
                                       ? multiPrint.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal)
                                                        .multiply(rate.getValue()).negate()
                                       : multiPrint.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal)
                                                        .multiply(rate.getValue());
                        }

                    } else {
                        map.put(DocReport_Base.Field.DOC_RATE.getKey(), BigDecimal.ONE);
                        netTotal = negate
                                    ? multiPrint.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateNetTotal)
                                                    .negate()
                                    : multiPrint.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateNetTotal);
                        crossTotal = negate
                                    ? multiPrint.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal)
                                                    .negate()
                                    : multiPrint.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.RateCrossTotal);
                    }
                } else {
                    map.put(DocReport_Base.Field.DOC_RATE.getKey(), BigDecimal.ONE);
                    netTotal = negate
                                ? multiPrint.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.NetTotal).negate()
                                : multiPrint.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.NetTotal);
                    crossTotal = negate
                                ? multiPrint.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.CrossTotal).negate()
                                : multiPrint.<BigDecimal>getAttribute(CISales.DocumentSumAbstract.CrossTotal);
                }

                igv = crossTotal.subtract(netTotal);

                final Boolean export = BigDecimal.ZERO.compareTo(igv) == 0;

                map.put(DocReport_Base.Field.DOC_DATE.getKey(), docDate);
                map.put(DocReport_Base.Field.DOC_DUEDATE.getKey(), docDueDate);
                DocReport_Base.LOG.debug("Document OID '{}'", instDoc.getOid());
                DocReport_Base.LOG.debug("Document name '{}'", docName);
                map.put(DocReport_Base.Field.DOC_NAME.getKey(), docName);

                final Pattern patternSN = Pattern.compile("^\\d+");
                final Matcher matcherSN = patternSN.matcher(docName);
                if (matcherSN.find()) {
                    map.put(DocReport_Base.Field.DOC_SN.getKey(), matcherSN.group());
                } else {
                    map.put(DocReport_Base.Field.DOC_SN.getKey(), docName);
                }

                final Pattern patternNo = Pattern.compile("(?<=\\D)\\d+");
                final Matcher matcherNo = patternNo.matcher(docName);
                if (matcherNo.find()) {
                    map.put(DocReport_Base.Field.DOC_NUMBER.getKey(), matcherNo.group());
                } else {
                    map.put(DocReport_Base.Field.DOC_NUMBER.getKey(), docName);
                }

                map.put(DocReport_Base.Field.DOC_DOCTYPE.getKey(),
                                DocReport_Base.DOCTYPE_MAP.get(instDoc.getType().getId()));
                if (!canceled) {
                    map.put(DocReport_Base.Field.DOC_CONTACT.getKey(), contactName);
                    map.put(DocReport_Base.Field.DOC_TAXNUM.getKey(), contactTaxNum);
                    map.put(DocReport_Base.Field.DOC_NETTOTAL.getKey(), export ? null : netTotal);
                    map.put(DocReport_Base.Field.DOC_CROSSTOTAL.getKey(), crossTotal);
                    map.put(DocReport_Base.Field.DOC_IGV.getKey(), export ? null : igv);
                    map.put(DocReport_Base.Field.DOC_EXPORT.getKey(), export ? crossTotal : null);
                    if (instDoc.getType().isKindOf(CISales.CreditNote.getType())
                                    || instDoc.getType().isKindOf(CISales.Reminder.getType())) {
                        if (docRelInst != null && docRelInst.isValid()) {
                            map.put(DocReport_Base.Field.DOCREL_DATE.getKey(), docRelDate);
                            map.put(DocReport_Base.Field.DOCREL_PREFNAME.getKey(),
                                            docRelName.split("-").length == 2 ? docRelName.split("-")[0] : "001");
                            map.put(DocReport_Base.Field.DOCREL_SUFNAME.getKey(),
                                            docRelName.split("-").length == 2 ? docRelName.split("-")[1] : docRelName);
                            map.put(DocReport_Base.Field.DOCREL_TYPE.getKey(),
                                            DocReport_Base.DOCTYPE_MAP.get(docRelInst.getType().getId()));
                        }
                    }
                } else {
                    map.put(DocReport_Base.Field.DOC_CONTACT.getKey(),
                                    Status.find(CISales.InvoiceStatus.uuid, "Replaced").getLabel());
                }

                values.add(map);
            }
        }
        final ComparatorChain chain = new ComparatorChain();
        chain.addComparator(new Comparator<Map<String, Object>>() {

            @Override
            public int compare(final Map<String, Object> _o1,
                               final Map<String, Object> _o2)
            {
                final String val1 = (String) _o1.get(DocReport_Base.Field.DOC_DOCTYPE.getKey());
                final String val2 = (String) _o2.get(DocReport_Base.Field.DOC_DOCTYPE.getKey());
                return val1.compareTo(val2);
            }
        });
        chain.addComparator(new Comparator<Map<String, Object>>() {

            @Override
            public int compare(final Map<String, Object> _o1,
                               final Map<String, Object> _o2)
            {
                final DateTime date1 = (DateTime) _o1.get(DocReport_Base.Field.DOC_DATE.getKey());
                final DateTime date2 = (DateTime) _o2.get(DocReport_Base.Field.DOC_DATE.getKey());
                return date1.compareTo(date2);
            }
        });
        chain.addComparator(new Comparator<Map<String, Object>>() {

            @Override
            public int compare(final Map<String, Object> _o1,
                               final Map<String, Object> _o2)
            {
                final String val1 = (String) _o1.get(DocReport_Base.Field.DOC_NAME.getKey());
                final String val2 = (String) _o2.get(DocReport_Base.Field.DOC_NAME.getKey());
                return val1.compareTo(val2);
            }
        });

        Collections.sort(values, chain);
        getValues().addAll(values);
    }


    /**
     * Method for obtains a new List with instance of the documents.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @param _from Datetime from.
     * @param _to Datetime to.
     * @return ret with list instance.
     * @throws EFapsException on error.
     */
    protected List<Instance> getInstances(final Parameter _parameter,
                                          final DateTime _from,
                                          final DateTime _to)
        throws EFapsException
    {
        final List<Instance> ret = new ArrayList<Instance>();
        final Map<String, List<Instance>> values = new TreeMap<String, List<Instance>>();

        values.put("A", getInstances(_parameter, CISales.Invoice.uuid, _from, _to));
        values.put("B", getInstances(_parameter, CISales.Receipt.uuid, _from, _to));
        values.put("C", getInstances(_parameter, CISales.CreditNote.uuid, _from, _to));
        values.put("D", getInstances(_parameter, CISales.Reminder.uuid, _from, _to));

        for (final List<Instance> instances : values.values()) {
            for (final Instance inst : instances) {
                DocReport_Base.LOG.debug("Document OID '{}'", inst.getOid());
                ret.add(inst);
            }
        }
        return ret;
    }

    /**
     * Method for obtains instances of the documents.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @param _typeUUID UUID of type.
     * @param _from Datetime from.
     * @param _to Datetime to.
     * @return ret with values.
     * @throws EFapsException on error.
     */
    protected List<Instance> getInstances(final Parameter _parameter,
                                                  final UUID _typeUUID,
                                                  final DateTime _from,
                                                  final DateTime _to)
        throws EFapsException
    {
        final String contactOid = _parameter.getParameterValue("contact");
        final String contactName = _parameter.getParameterValue("contactAutoComplete");

        final List<Instance> ret = new ArrayList<Instance>();
        final QueryBuilder queryBldr = new QueryBuilder(_typeUUID);
        queryBldr.addWhereAttrGreaterValue(CIERP.DocumentAbstract.Date, _from.minusMinutes(1));
        queryBldr.addWhereAttrLessValue(CIERP.DocumentAbstract.Date, _to.plusDays(1));

        if (contactOid != null && !contactOid.isEmpty() && contactName != null && !contactName.isEmpty()) {
            queryBldr.addWhereAttrEqValue(CIERP.DocumentAbstract.Contact, Instance.get(contactOid).getId());
        }

        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIERP.DocumentAbstract.Name);
        multi.execute();
        while (multi.next()) {
            ret.add(multi.getCurrentInstance());
        }
        return ret;
    }

    /**
     * CReate the Document Report.
     * @param _parameter    Parameter as passed from the eFaps API
     * @return report
     * @throws EFapsException on error
     */
    @SuppressWarnings("unchecked")
    public Return createDocReport(final Parameter _parameter)
        throws EFapsException
    {
        final String dateFrom = _parameter.getParameterValue("dateFrom");
        final String dateTo = _parameter.getParameterValue("dateTo");
        final String mime = _parameter.getParameterValue("mime");
        final String currency = _parameter.getParameterValue("currency");

        final CurrencyInst curInst = new CurrencyInst(Instance.get(CIERP.Currency.getType(), currency));
        final Map<String, Object> props = (Map<String, Object>) _parameter.get(ParameterValues.PROPERTIES);
        props.put("Mime", mime);
        final DateTime from = new DateTime(dateFrom);
        final DateTime to = new DateTime(dateTo);
        final StandartReport report = new StandartReport();
        report.setFileName(getReportName(_parameter, from, to));
        report.getJrParameters().put("FromDate", from.toDate());
        report.getJrParameters().put("ToDate", to.toDate());
        report.getJrParameters().put("Mime", mime);
        report.getJrParameters().put("Currency", curInst.getName());
        report.getJrParameters().put("CurrencyId", curInst.getInstance().getId());

        final SystemConfiguration config = ERP.getSysConfig();
        if (config != null) {
            final String companyName = ERP.COMPANYNAME.get();
            final String companyTaxNumb = ERP.COMPANYTAX.get();

            if (companyName != null && companyTaxNumb != null && !companyName.isEmpty() && !companyTaxNumb.isEmpty()) {
                report.getJrParameters().put("CompanyName", companyName);
                report.getJrParameters().put("CompanyTaxNum", companyTaxNumb);
            }
        }

        addAdditionalParameters(_parameter, report);

        return report.execute(_parameter);
    }

    protected void addAdditionalParameters(final Parameter _parameter,
                                           final StandartReport report)
        throws EFapsException
    {
        // TODO Auto-generated method stub
    }


    protected Rate getRates4DateRange(final Instance _curInst,
                                      final DateTime _date,
                                      final Long _rateCurType)
        throws EFapsException
    {
        Rate rate;
        final QueryBuilder queryBldr = new QueryBuilder(Type.get(_rateCurType));
        queryBldr.addWhereAttrEqValue(CIERP.CurrencyRateAbstract.CurrencyLink, _curInst.getId());
        queryBldr.addWhereAttrGreaterValue(CIERP.CurrencyRateAbstract.ValidUntil, _date.minusMinutes(1));
        queryBldr.addWhereAttrLessValue(CIERP.CurrencyRateAbstract.ValidFrom, _date.plusMinutes(1));
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder valSel = new SelectBuilder()
                        .attribute(CIERP.CurrencyRateAbstract.Rate).value();
        final SelectBuilder labSel = new SelectBuilder()
                        .attribute(CIERP.CurrencyRateAbstract.Rate).label();
        final SelectBuilder curSel = new SelectBuilder()
                        .linkto(CIERP.CurrencyRateAbstract.CurrencyLink).oid();
        multi.addSelect(valSel, labSel, curSel);
        multi.execute();
        if (multi.next()) {
            rate = new Rate(new CurrencyInst(Instance.get(multi.<String>getSelect(curSel))),
                            multi.<BigDecimal>getSelect(valSel),
                            multi.<BigDecimal>getSelect(labSel));
        } else {
            rate = new Rate(new CurrencyInst(Instance.get(CIERP.Currency.getType(),
                            _curInst.getId())), BigDecimal.ONE);
        }

        return rate;
    }

    /**
     * Get the name for the report.
     *
     * @param _parameter Parameter as passed form the eFaps API
     * @param _from fromdate
     * @param _to   to date
     * @return name of the report
     */
    protected String getReportName(final Parameter _parameter,
                                   final DateTime _from,
                                   final DateTime _to)
    {
        return DBProperties.getProperty("Sales_DocReport.Label", "es")
            + "-" + _from.toString(DateTimeFormat.shortDate())
            + "-" + _to.toString(DateTimeFormat.shortDate());
    }

    /**
     * Called from the field with the rate for a document. Returning a
     * input with one rate selected.
     *
     * @param _parameter Parameter as passed by the eFaps API for ESJP
     * @return a input with one rate for currency
     * @throws EFapsException on error
     */
    public Return getRateFieldValueUI(final Parameter _parameter)
        throws EFapsException
    {
        final FieldValue fieldValue = (FieldValue) _parameter.get(ParameterValues.UIOBJECT);
        final StringBuilder html = new StringBuilder();
        // Sales-Configuration
        final Instance baseInst = SystemConfiguration.get(UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f"))
                        .getLink("CurrencyBase");
        final String symbol = new CurrencyInst(baseInst).getSymbol();
        if (fieldValue.getTargetMode().equals(TargetMode.CREATE)) {
            if (fieldValue.getField().getName().equals("rate")) {
                html.append("<input type='text' value='1' name=\"").append(fieldValue.getField().getName())
                    .append("\" /> ").append("<span id='convert'>").append(symbol).append(" -> ").append(symbol)
                    .append("</span>");
            }
        }
        final Return retVal = new Return();
        retVal.put(ReturnValues.SNIPLETT, html.toString());
        return retVal;
    }

    /**
     * Called from the field with the rate for a document. Returning a
     * input with one rate selected.
     *
     * @param _parameter Parameter as passed by the eFaps API for ESJP
     * @return a input with one rate for currency
     * @throws EFapsException on error
     */
    public Return updateRateFieldValueUI(final Parameter _parameter)
        throws EFapsException
    {
        final String datecurr = _parameter.getParameterValue("dateCurrency_eFapsDate");
        final String curr = _parameter.getParameterValue("currency");

        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();

        final Instance newInst = Instance.get(Type.get(CIERP.Currency.uuid), curr);
        final Map<String, String> map = new HashMap<String, String>();
        Instance currentInst = (Instance) Context.getThreadContext().getSessionAttribute(
                                                            AbstractDocument_Base.CURRENCYINST_KEY);
        // Sales-Configuration
        final Instance baseInst = Currency.getBaseCurrency();
        if (currentInst == null) {
            currentInst = baseInst;
        }

        final StringBuilder js = new StringBuilder();
        final CurrencyInst baseCurrInst = new CurrencyInst(baseInst);
        final CurrencyInst currInst = new CurrencyInst(Instance.get(CIERP.Currency.getType(), curr));

        js.append("document.getElementById('convert').innerHTML='").append(baseCurrInst.getSymbol()).append(" -> ")
            .append(currInst.getSymbol()).append("'");
        map.put(EFapsKey.FIELDUPDATE_JAVASCRIPT.getKey(), js.toString());
        if (!newInst.equals(currentInst)) {
            final BigDecimal[] rates = new PriceUtil()
                        .getRates(DateUtil.getDateFromParameter(datecurr), newInst, currentInst);
            map.put("rate", rates[3].toString());
            list.add(map);
        } else {
            final BigDecimal[] rates = new PriceUtil()
                        .getRates(DateUtil.getDateFromParameter(datecurr), currentInst, newInst);
            map.put("rate", rates[3].toString());
            list.add(map);
        }

        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    public Return updateFilterActiveUIValue(final Parameter _parameter) {
        return new Return();
    }
}
