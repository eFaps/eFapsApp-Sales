/*
 * Copyright 2003 - 2009 The eFaps Team
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

package org.efaps.esjp.sales;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;
import org.efaps.util.cache.AbstractAutomaticCache;
import org.efaps.util.cache.CacheObjectInterface;
import org.efaps.util.cache.CacheReloadException;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 *
 */
@EFapsUUID("0c3a91e2-1497-423f-ae60-a0ae94e95451")
@EFapsApplication("eFapsApp-Sales")
@Deprecated
public abstract class Tax_Base
    implements CacheObjectInterface
{
    /**
     * CACHE for the Taxes.
     */
    private static TaxCache CACHE = new TaxCache();

    /**
     * Id of this Tax.
     */
    private final long id;
    /**
     * Name of htis tax.
     */
    private final String name;
    /**
     * Oid of this Tax.
     */
    private final String oid;

    /**
     * map of dates to rate.
     */
    private final Map<DateTime, TaxRate> rateMap = new TreeMap<>();

    /**
     * @param _oid      oid of this tax
     * @param _id       id of this tax
     * @param _name     name of this tax
     */
    protected Tax_Base(final String _oid,
                       final long _id,
                       final String _name)
    {
        oid = _oid;
        id = _id;
        name = _name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getId()
    {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName()
    {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UUID getUUID()
    {
        return null;
    }

    public TaxRate getTaxRate(final LocalDate _localDate)
    {
        TaxRate ret = null;
        for (final Entry<DateTime, TaxRate> entry : rateMap.entrySet()) {
            final LocalDate validityDate = entry.getKey().toLocalDate();
            if (_localDate.isAfter(validityDate) || _localDate.isEqual(validityDate)) {
                ret = entry.getValue();
            }
        }
        return ret;
    }

    /**
     * @throws EFapsException on error
     *
     */
    private void evaluateRates()
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CISales.Tax);
        queryBldr.addWhereAttrEqValue(CISales.Tax.TaxCategory, Instance.get(oid).getId());
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.Tax.Name, CISales.Tax.Numerator, CISales.Tax.Denominator, CISales.Tax.ValidFrom,
                        CISales.Tax.UUID);

        multi.execute();
        while (multi.next()) {
            final Long idTmp = multi.getCurrentInstance().getId();
            final String oidTmp = multi.getCurrentInstance().getOid();
            final String nameTmp = multi.<String>getAttribute(CISales.Tax.Name);
            final String uuidTmp = multi.<String>getAttribute(CISales.Tax.UUID);
            final Integer numerator = multi.<Integer>getAttribute(CISales.Tax.Numerator);
            final Integer denominator = multi.<Integer>getAttribute(CISales.Tax.Denominator);
            final DateTime validfrom = multi.<DateTime>getAttribute(CISales.Tax.ValidFrom);
            rateMap.put(validfrom, new TaxRate(oidTmp, idTmp, nameTmp, uuidTmp, numerator, denominator));
        }
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this).append("id", id).append("Name", name).toString();
    }

    public static Tax get(final long _id)
    {
        return Tax_Base.CACHE.get(_id);
    }

    public static Tax get(final String _name)
    {
        return Tax_Base.CACHE.get(_name);
    }

    protected static class TaxCache
        extends AbstractAutomaticCache<Tax>
    {

        @Override
        protected void readCache(final Map<Long, Tax> _cache4Id,
                                 final Map<String, Tax> _cache4Name,
                                 final Map<UUID, Tax> _cache4UUID)
            throws CacheReloadException
        {
            try {
                final QueryBuilder queryBldr = new QueryBuilder(CISales.TaxCategory);
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CISales.TaxCategory.Name);
                multi.execute();
                while (multi.next()) {
                    final String oidTmp = multi.getCurrentInstance().getOid();
                    final long idTmp = multi.getCurrentInstance().getId();
                    final String nameTmp = multi.getAttribute(CISales.TaxCategory.Name);

                    final Tax tax = new Tax(oidTmp, idTmp, nameTmp);
                    _cache4Id.put(idTmp, tax);
                    _cache4Name.put(nameTmp, tax);
                }

                for (final Tax_Base tax : _cache4Id.values()) {
                    tax.evaluateRates();
                }
            } catch (final EFapsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static class TaxRate
    {

        private final String oid;
        private final Long id;
        private final String name;
        private final Integer numerator;
        private final Integer denominator;
        private final String uuid;


        public TaxRate(final String _oid,
                       final Long _id,
                       final String _name,
                       final String _uuidTmp,
                       final Integer _numerator,
                        final Integer _denominator)
        {
            oid = _oid;
            id = _id;
            name = _name;
            uuid = _uuidTmp;
            numerator = _numerator;
            denominator = _denominator;
        }

        /**
         * Getter method for instance variable {@link #oid}.
         *
         * @return value of instance variable {@link #oid}
         */
        public String getOid()
        {
            return oid;
        }

        /**
         * Getter method for instance variable {@link #id}.
         *
         * @return value of instance variable {@link #id}
         */
        public Long getId()
        {
            return id;
        }

        /**
         * Getter method for instance variable {@link #name}.
         *
         * @return value of instance variable {@link #name}
         */
        public String getName()
        {
            return name;
        }

        /**
         * Getter method for instance variable {@link #numerator}.
         *
         * @return value of instance variable {@link #numerator}
         */
        public Integer getNumerator()
        {
            return numerator;
        }

        /**
         * Getter method for instance variable {@link #denominator}.
         *
         * @return value of instance variable {@link #denominator}
         */
        public Integer getDenominator()
        {
            return denominator;
        }

        /**
         * Getter method for the instance variable {@link #uuid}.
         *
         * @return value of instance variable {@link #uuid}
         */
        public String getUuid()
        {
            return uuid;
        }

        public static TaxRate getZeroTax()
        {
            return new TaxRate("", Long.valueOf(1), "", "ZERO", 1, 1);
        }
    }
}
