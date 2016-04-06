/*
 * Copyright 2003 - 2016 The eFaps Team
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

package org.efaps.esjp.sales.tax;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.CachedMultiPrintQuery;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("baf00224-ebe4-4c33-96d7-20f4ca772c16")
@EFapsApplication("eFapsApp-Sales")
public abstract class TaxCat_Base
    implements Serializable
{

    /**
     * Key used for Caching the Query related to the TaxCategories.
     */
    protected static final String CACHEKEY = TaxCat.class.getName() + ".CacheKey";

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instance of the TaxCategory this <code>TaxCat</code> belongs to.
     */
    private final Instance instance;

    /**
     * UUID of the TaxCategory this <code>TaxCat</code> belongs to.
     */
    private final UUID uuid;

    /**
     * Name of the TaxCategory this <code>TaxCat</code> belongs to.
     */
    private final String name;

    /**
     * @param _instance Instance of the TaxCategory
     * @param _uuid     UUID of the TaxCategory
     * @param _name     Name of the TaxCategory
     */
    public TaxCat_Base(final Instance _instance,
                       final String _uuid,
                       final String _name)
    {
        this.instance = _instance;
        this.uuid = UUID.fromString(_uuid);
        this.name = _name;
    }

    /**
     * Get the list of taxes for now.
     * @return List of taxes belonging to this TaxCategory
     * @throws EFapsException on error
     */
    public Collection<? extends Tax> getTaxes()
        throws EFapsException
    {
        return getTaxes(new DateTime());
    }

    /**
     * Get the list of taxes for a given date.
     * @param _date date to be used as filter
     * @return List of taxes belonging to this TaxCategory
     * @throws EFapsException on error
     */
    public Collection<? extends Tax> getTaxes(final DateTime _date)
        throws EFapsException
    {
        final List<Tax> ret = new ArrayList<Tax>();
        final QueryBuilder queryBldr = new QueryBuilder(CISales.Tax);
        queryBldr.addWhereAttrEqValue(CISales.Tax.TaxCategory, this.instance);
        queryBldr.addOrderByAttributeDesc(CISales.Tax.ValidFrom);
        final MultiPrintQuery multi = queryBldr.getCachedPrint(TaxCat.CACHEKEY);
        multi.setEnforceSorted(true);
        multi.addAttribute(CISales.Tax.Name, CISales.Tax.Numerator, CISales.Tax.Denominator, CISales.Tax.ValidFrom,
                        CISales.Tax.UUID);
        multi.execute();
        final Set<String> uuids = new HashSet<String>();
        while (multi.next()) {
            final String nameTmp = multi.<String>getAttribute(CISales.Tax.Name);
            final String uuidTmp = multi.<String>getAttribute(CISales.Tax.UUID);
            final Integer numerator = multi.<Integer>getAttribute(CISales.Tax.Numerator);
            final Integer denominator = multi.<Integer>getAttribute(CISales.Tax.Denominator);
            final DateTime validFrom = multi.<DateTime>getAttribute(CISales.Tax.ValidFrom);
            if (!uuids.contains(uuidTmp) && validFrom.isBefore(_date)) {
                ret.add(new Tax(this, multi.getCurrentInstance(), nameTmp, uuidTmp, numerator, denominator));
                uuids.add(uuidTmp);
            }
        }
        return ret;
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
     * Getter method for the instance variable {@link #uuid}.
     *
     * @return value of instance variable {@link #uuid}
     */
    public UUID getUuid()
    {
        return this.uuid;
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
     * Get a TaxCat for an given id.
     * @param _taxCatId id of the taxcat wanted
     * @return taxcat for the id
     * @throws EFapsException on error
     */
    public static TaxCat get(final long _taxCatId)
        throws EFapsException
    {
        TaxCat ret = null;
        final QueryBuilder queryBldr = new QueryBuilder(CISales.TaxCategory);
        queryBldr.addWhereAttrEqValue(CISales.TaxCategory.ID, _taxCatId);
        final CachedMultiPrintQuery multi = queryBldr.getCachedPrint(TaxCat.CACHEKEY);
        multi.addAttribute(CISales.TaxCategory.UUID, CISales.TaxCategory.Name);
        multi.execute();
        if (multi.next()) {
            ret = new TaxCat(multi.getCurrentInstance(), multi.<String>getAttribute(CISales.TaxCategory.UUID),
                            multi.<String>getAttribute(CISales.TaxCategory.Name));
        }
        return ret;
    }

    /**
     * Get a TaxCat for an given UUID.
     * @param _uuid uuid of the taxcat wanted
     * @return taxcat for the id
     * @throws EFapsException on error
     */
    public static TaxCat get(final UUID _uuid)
        throws EFapsException
    {
        TaxCat ret = null;
        final QueryBuilder queryBldr = new QueryBuilder(CISales.TaxCategory);
        queryBldr.addWhereAttrEqValue(CISales.TaxCategory.UUID, _uuid.toString());
        final CachedMultiPrintQuery multi = queryBldr.getCachedPrint(TaxCat.CACHEKEY);
        multi.addAttribute(CISales.TaxCategory.UUID, CISales.TaxCategory.Name);
        multi.execute();
        if (multi.next()) {
            ret = new TaxCat(multi.getCurrentInstance(), multi.<String>getAttribute(CISales.TaxCategory.UUID),
                            multi.<String>getAttribute(CISales.TaxCategory.Name));
        }
        return ret;
    }
}
