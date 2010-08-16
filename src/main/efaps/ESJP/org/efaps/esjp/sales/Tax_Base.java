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
import java.util.TreeMap;
import java.util.UUID;
import java.util.Map.Entry;

import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.SearchQuery;
import org.efaps.util.EFapsException;
import org.efaps.util.cache.AutomaticCache;
import org.efaps.util.cache.CacheObjectInterface;
import org.efaps.util.cache.CacheReloadException;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("0c3a91e2-1497-423f-ae60-a0ae94e95451")
@EFapsRevision("$Rev$")
public abstract class Tax_Base implements CacheObjectInterface
{

    private static TaxCache CACHE = new TaxCache();
    private final long id;
    private final String name;
    private final String oid;
    private final Map<DateTime, TaxRate> rateMap = new TreeMap<DateTime, TaxRate>();

    /**
     * @param oidTmp
     * @param idTmp
     * @param nameTmp
     * @param taxCatId
     * @param numerator
     * @param denominator
     */
    protected Tax_Base(final String _oid, final long _id, final String _name)
    {
        this.oid = _oid;
        this.id = _id;
        this.name = _name;
    }

    public long getId()
    {
        return this.id;
    }

    public String getName()
    {
        return this.name;
    }

    public TaxRate getTaxRate(final LocalDate _localDate)
    {
        TaxRate ret = null;
        for (final Entry<DateTime, TaxRate> entry : this.rateMap.entrySet()) {
            final LocalDate validityDate = entry.getKey().toLocalDate();
            if (_localDate.isAfter(validityDate) || _localDate.isEqual(validityDate)) {
                ret = entry.getValue();
            }
        }
        return ret;
    }

    /**
     * @throws EFapsException
     *
     */
    private void evaluateRates() throws EFapsException
    {
        final SearchQuery query = new SearchQuery();
        query.setExpand(this.oid, "Sales_Tax\\TaxCategory");
        query.addSelect("OID");
        query.addSelect("ID");
        query.addSelect("Name");
        query.addSelect("Numerator");
        query.addSelect("Denominator");
        query.addSelect("ValidFrom");
        query.execute();
        while (query.next()) {
            final Long id = (Long) query.get("ID");
            final String oid = (String) query.get("OID");
            final String name = (String) query.get("Name");
            final Long numerator = (Long) query.get("Numerator");
            final Long denominator = (Long) query.get("Denominator");

            final DateTime validfrom = (DateTime) query.get("ValidFrom");
            this.rateMap.put(validfrom, new TaxRate(oid, id, name, numerator, denominator));
        }
    }

    @Override
    public String toString()
    {
        return "[id: " + this.id + "; Name: " + this.name + "]";
    }

    public UUID getUUID()
    {
        return null;
    }

    public static Tax get(final long _id)
    {
        return Tax_Base.CACHE.get(_id);
    }

    public static Tax get(final String _name)
    {
        return Tax_Base.CACHE.get(_name);
    }

    protected static class TaxCache extends AutomaticCache<Tax>
    {

        @Override
        protected void readCache(final Map<Long, Tax> _cache4Id, final Map<String, Tax> _cache4Name,
                        final Map<UUID, Tax> _cache4UUID) throws CacheReloadException
        {
            try {
                final SearchQuery query = new SearchQuery();
                query.setQueryTypes("Sales_TaxCategory");
                query.addSelect("OID");
                query.addSelect("ID");
                query.addSelect("Name");
                query.execute();
                while (query.next()) {
                    final String oidTmp = (String) query.get("OID");
                    final long idTmp = (Long) query.get("ID");
                    final String nameTmp = (String) query.get("Name");

                    final Tax tax = new Tax(oidTmp, idTmp, nameTmp);
                    _cache4Id.put(idTmp, tax);
                    _cache4Name.put(nameTmp, tax);
                }
                query.close();

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
        private final Long numerator;
        private final Long denominator;
        public TaxRate(final String _oid, final Long _id, final String _name, final Long _numerator,
                        final Long _denominator)
        {
            this.oid = _oid;
            this.id = _id;
            this.name = _name;
            this.numerator = _numerator;
            this.denominator = _denominator;
        }

        /**
         * Getter method for instance variable {@link #oid}.
         *
         * @return value of instance variable {@link #oid}
         */
        public String getOid()
        {
            return this.oid;
        }

        /**
         * Getter method for instance variable {@link #id}.
         *
         * @return value of instance variable {@link #id}
         */
        public Long getId()
        {
            return this.id;
        }

        /**
         * Getter method for instance variable {@link #name}.
         *
         * @return value of instance variable {@link #name}
         */
        public String getName()
        {
            return this.name;
        }

        /**
         * Getter method for instance variable {@link #numerator}.
         *
         * @return value of instance variable {@link #numerator}
         */
        public Long getNumerator()
        {
            return this.numerator;
        }

        /**
         * Getter method for instance variable {@link #denominator}.
         *
         * @return value of instance variable {@link #denominator}
         */
        public Long getDenominator()
        {
            return this.denominator;
        }

        public static TaxRate getZeroTax()
        {
            return new TaxRate("", new Long(1) , "ZERO", new Long(1), new Long(1));
        }
    }
}
