/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
 */
package org.efaps.esjp.sales.report.filter;

import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.common.uiform.Field;
import org.efaps.esjp.common.uiform.Field_Base.DropDownPosition;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.FilteredReport_Base.AbstractFilterValue;
import org.efaps.util.EFapsException;

/**
 * The Class CostTypeFilterValue_Base.
 *
 * @author The eFaps Team
 */
@EFapsUUID("d700b18f-9f1e-48ab-b06e-c4201b2f3d15")
@EFapsApplication("eFapsApp-Sales")
public abstract class CostTypeFilterValue_Base
    extends org.efaps.esjp.products.reports.filter.CostTypeFilterValue
{
    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /** The acquisition. */
    private boolean acquisition;

    @Override
    public String getLabel(final Parameter _parameter)
        throws EFapsException
    {
        final String ret;
        final Instance tmpInst = Instance.get(getObject());
        if (isAcquisition() && InstanceUtils.isKindOf(tmpInst, CIERP.Currency)) {
            ret = DBProperties.getFormatedDBProperty(CostTypeFilterValue.class.getName()
                            + ".CostType.Acquisition",
                            (Object) CurrencyInst.get(Instance.get(getObject())).getName());
        } else {
            ret = super.getLabel(_parameter);
        }
        return ret;
    }

    /**
     * Getter method for the instance variable {@link #acquisition}.
     *
     * @return value of instance variable {@link #acquisition}
     */
    public boolean isAcquisition()
    {
        return this.acquisition;
    }

    /**
     * Setter method for instance variable {@link #acquisition}.
     *
     * @param _acquisition value for instance variable {@link #acquisition}
     * @return the cost type filter value
     */
    public CostTypeFilterValue setAcquisition(final boolean _acquisition)
    {
        this.acquisition = _acquisition;
        return (CostTypeFilterValue) this;
    }

    @Override
    public AbstractFilterValue<String> parseObject(final String[] _values)
    {
        if (!ArrayUtils.isEmpty(_values) && _values[0].startsWith("ACQUISITION_")) {
            setAcquisition(true);
            setObject(_values[0].replace("ACQUISITION_", ""));
        } else {
            setAcquisition(false);
        }
        return super.parseObject(_values);
    }

    /**
     * Gets the cost type field value.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _selected the selected
     * @param _currencyInsts the currency insts
     * @return the cost type field value
     * @throws EFapsException on error
     */
    protected static List<DropDownPosition> getCostTypePositions(final Parameter _parameter,
                                                                 final CostTypeFilterValue _selected,
                                                                 final Instance... _currencyInsts)
        throws EFapsException
    {
        final List<DropDownPosition> ret = org.efaps.esjp.products.reports.filter.CostTypeFilterValue
                        .getCostTypePositions(_parameter, _selected);
        if (ArrayUtils.isNotEmpty(_currencyInsts)) {
            for (final Instance currencyInst : _currencyInsts) {
                final boolean selected = _selected != null && _selected.isAcquisition()
                                && currencyInst.getOid().equals(_selected.getObject());
                ret.add(new Field.DropDownPosition("ACQUISITION_" + currencyInst.getOid(),
                                new CostTypeFilterValue()
                                    .setAcquisition(true)
                                    .setObject(currencyInst.getOid())
                                    .getLabel(_parameter))
                                .setSelected(selected));
            }
        }
        return ret;
    }
}
