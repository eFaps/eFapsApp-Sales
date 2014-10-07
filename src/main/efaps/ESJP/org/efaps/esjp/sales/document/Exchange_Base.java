/*
 * Copyright 2003 - 2013 The eFaps Team
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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.ci.CITableSales;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.erp.AbstractWarning;
import org.efaps.esjp.erp.IWarning;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.Swap;
import org.efaps.esjp.sales.payment.DocPaymentInfo;
import org.efaps.esjp.sales.payment.DocumentUpdate;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: Exchange_Base.java 11836 2014-01-27 14:29:16Z
 *          m.aranya@moxter.net $
 */
@EFapsUUID("237aaa91-4c18-413c-9718-268deb312702")
@EFapsRevision("$Rev$")
public abstract class Exchange_Base
    extends AbstractSumOnlyDocument
{

    /**
     * Logger for this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Exchange.class);

    /**
     * Method for create a new Quotation.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        createDoc(_parameter);
        return new Return();
    }

    @Override
    protected void add2DocCreate(final Parameter _parameter,
                                 final Insert _insert,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final String onlynumber = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.Exchange.OnlyNumber.name));
        if (onlynumber != null) {
            _insert.add(CISales.Exchange.OnlyNumber, onlynumber);
            _createdDoc.getValues().put(CISales.Exchange.OnlyNumber.name, onlynumber);
        }

        final String entityFinancial = _parameter.getParameterValue(getFieldName4Attribute(_parameter,
                        CISales.Exchange.EntityFinancial.name));
        if (entityFinancial != null) {
            _insert.add(CISales.Exchange.EntityFinancial, entityFinancial);
            _createdDoc.getValues().put(CISales.Exchange.EntityFinancial.name, entityFinancial);
        }
    }

    /**
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return createCalculated(final Parameter _parameter)
        throws EFapsException
    {
        final String[] nameAr = _parameter
                        .getParameterValues(CITableSales.Sales_ExchangeCreateCalculatedTable.name4Exchange.name);
        final String[] crossAr = _parameter
                        .getParameterValues(CITableSales.Sales_ExchangeCreateCalculatedTable.crossTotal4Exchange.name);
        final String[] currAr = _parameter
                        .getParameterValues(CITableSales.Sales_ExchangeCreateCalculatedTable.currency4Exchange.name);
        final String[] docOids = _parameter.getParameterValues(CITableSales.Sales_SwapPayTable.document.name);
        final List<Instance> docInsts = new ArrayList<>();
        if (nameAr != null && crossAr != null && currAr != null && docOids != null) {
            try {
                final Map<Instance, BigDecimal> exchangeMap = new LinkedHashMap<>();
                final Map<Instance, Map<Instance, BigDecimal>> exchangeValuesMap = new LinkedHashMap<>();

                for (int i = 0; i < nameAr.length; i++) {
                    final Parameter parameter = ParameterUtil.clone(_parameter);
                    ParameterUtil.setParmeterValue(parameter, "name4create", nameAr[i]);
                    ParameterUtil.setParmeterValue(parameter, "rateCrossTotal", crossAr[i]);
                    ParameterUtil.setParmeterValue(parameter, "rateCurrencyId", currAr[i]);
                    ParameterUtil.setParmeterValue(parameter, getFieldName4Attribute(_parameter,
                                    CISales.DocumentSumAbstract.Date.name), new DateTime().toString());
                    final CreatedDoc createdDoc = createDoc(parameter);
                    exchangeMap.put(createdDoc.getInstance(), (BigDecimal) NumberFormatter.get().getFormatter()
                                    .parse(crossAr[i]));
                }
                final Iterator<Entry<Instance, BigDecimal>> exchangeIter = exchangeMap.entrySet().iterator();
                int start = 0;
                BigDecimal missing = null;
                while (exchangeIter.hasNext()) {
                    final Entry<Instance, BigDecimal> entry = exchangeIter.next();
                    BigDecimal currentTotal = BigDecimal.ZERO;
                    for (int i = start; i < docOids.length; i++) {
                        final Instance docInst = Instance.get(docOids[i]);
                        BigDecimal partial = (BigDecimal) NumberFormatter.get().getFormatter()
                                        .parse(_parameter.getParameterValues("partial")[i]);
                        if (missing != null) {
                            partial = partial.subtract(partial.subtract(missing));
                            missing = null;
                        }
                        boolean stop = false;
                        BigDecimal partialTemp = BigDecimal.ZERO;
                        if (entry.getValue().subtract(currentTotal).compareTo(partial) > 0) {
                            partialTemp = partial;
                        } else {
                            partialTemp = entry.getValue().subtract(currentTotal);
                            missing = partial.subtract(partialTemp);
                            start = i;
                            stop = true;
                        }

                        Map<Instance, BigDecimal> innermap;
                        if (exchangeValuesMap.containsKey(entry.getKey())) {
                            innermap = exchangeValuesMap.get(entry.getKey());
                        } else {
                            innermap = new LinkedHashMap<>();
                            exchangeValuesMap.put(entry.getKey(), innermap);
                        }
                        innermap.put(docInst, partialTemp);
                        currentTotal = currentTotal.add(partialTemp);
                        if (stop) {
                            break;
                        }
                    }
                }

                for (final Entry<Instance, Map<Instance, BigDecimal>> entry : exchangeValuesMap.entrySet()) {
                    final DocPaymentInfo createDocInfo = new Swap().getNewDocPaymentInfo(_parameter, entry.getKey());
                    createDocInfo.setTargetDocInst(entry.getKey());
                    docInsts.add(entry.getKey());
                    for (final Entry<Instance, BigDecimal> innerEntry : entry.getValue().entrySet()) {
                        final Instance docInst = innerEntry.getKey();
                        if (docInst.isValid()) {
                            final Insert insert = new Insert(CISales.Document2Document4Swap);
                            if ("Collect".equalsIgnoreCase(getProperty(_parameter, "SwapType"))) {
                                insert.add(CISales.Document2Document4Swap.FromLink, entry.getKey());
                                insert.add(CISales.Document2Document4Swap.ToLink, docInst);
                            } else {
                                insert.add(CISales.Document2Document4Swap.FromLink, docInst);
                                insert.add(CISales.Document2Document4Swap.ToLink, entry.getKey());
                            }
                            insert.add(CISales.Document2Document4Swap.CurrencyLink,
                                            createDocInfo.getRateCurrencyInstance());
                            insert.add(CISales.Document2Document4Swap.Amount, innerEntry.getValue());
                            insert.execute();
                            docInsts.add(docInst);
                        }
                    }
                }
            } catch (final ParseException e) {
                LOG.error("Catched ParseException", e);
            }
        }
        for (final Instance inst : docInsts) {
            new DocumentUpdate().updateDocument(_parameter, inst);
        }
        return new Return();
    }

    /**
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return validate4CreateCalculated(final Parameter _parameter)
        throws EFapsException
    {
        final Validation val = new Validation() {
            @Override
            protected List<IWarning> validate(final Parameter _parameter,
                                              final List<IWarning> _warnings)
                throws EFapsException
            {
                final List<IWarning> ret = super.validate(_parameter, _warnings);
                final BigDecimal exchangeTotal =  getTotal4CreateCalculated(_parameter);
                final BigDecimal docTotal = new Swap().getSum4Positions(_parameter, true);
                if (exchangeTotal.compareTo(docTotal) != 0) {
                    ret.add(new ExhangeAndSwapMustHaveSameTotalWarning());
                }
                return ret;
            }
        };
        return val.validate(_parameter, this);
    }


    /**
     * @param _parameter Parameter as passed from eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return updateFields4CrossTotal(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final List<Map<String, Object>> list = new ArrayList<>();
        final Map<String, Object> map = new HashMap<>();
        final BigDecimal total = getTotal4CreateCalculated(_parameter);
        map.put(CIFormSales.Sales_ExchangeCreateCalculatedForm.exchangeTotal.name, total.toString());
        list.add(map);
        ret.put(ReturnValues.VALUES, list);
        return ret;
    }

    /**
     * Method for create a new Quotation.
     *
     * @param _parameter Parameter as passed from eFaps API.
     * @return total
     * @throws EFapsException on error.
     */
    protected BigDecimal getTotal4CreateCalculated(final Parameter _parameter)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        final String[] crossAr = _parameter
                        .getParameterValues(CITableSales.Sales_ExchangeCreateCalculatedTable.crossTotal4Exchange.name);
        if (crossAr != null) {
            try {
                for (final String cross : crossAr) {
                    ret = ret.add((BigDecimal) NumberFormatter.get().getFormatter()
                                    .parse(cross));
                }
            } catch (final ParseException e) {
                LOG.error("Catched ParseException", e);
            }
        }
        return ret;
    }

    /**
     * Warning for amount greater zero.
     */
    public static class ExhangeAndSwapMustHaveSameTotalWarning
        extends AbstractWarning
    {
        /**
         * Constructor.
         */
        public ExhangeAndSwapMustHaveSameTotalWarning()
        {
            setError(true);
        }
    }
}
