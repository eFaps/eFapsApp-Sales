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
import java.text.DecimalFormat;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.esjp.sales.Calculator;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 *
 */
@EFapsUUID("87e507dd-cd65-428c-a8f3-c914b2cd431f")
@EFapsApplication("eFapsApp-Sales")
public abstract class PerceptionCertificate_Base
    extends AbstractDocumentSum
{

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _createdDoc document the Certificate belongs to
     * @throws EFapsException on error
     */
    public void create4Doc(final Parameter _parameter,
                           final CreatedDoc _createdDoc)
        throws EFapsException
    {
        @SuppressWarnings("unchecked")
        final List<Calculator> calcList = (List<Calculator>) _createdDoc.getValue(AbstractDocument_Base.CALCULATORS_VALUE);
        final BigDecimal perception = getPerceptionTotal(_parameter, calcList);

        if (perception.compareTo(BigDecimal.ZERO) > 0) {
            final Insert insert = new Insert(CISales.PerceptionCertificate);
            insert.add(CISales.PerceptionCertificate.Date, _createdDoc.getValue(CISales.DocumentSumAbstract.Date.name));
            insert.add(CISales.PerceptionCertificate.Contact,
                            _createdDoc.getValue(CISales.DocumentSumAbstract.Contact.name));
            insert.add(CISales.PerceptionCertificate.Group,
                            _createdDoc.getValue(CISales.DocumentSumAbstract.Group.name));
            insert.add(CISales.PerceptionCertificate.Rate, _createdDoc.getValue(CISales.DocumentSumAbstract.Rate.name));
            insert.add(CISales.PerceptionCertificate.CurrencyId,
                            _createdDoc.getValue(CISales.DocumentSumAbstract.CurrencyId.name));
            insert.add(CISales.PerceptionCertificate.RateCurrencyId,
                            _createdDoc.getValue(CISales.DocumentSumAbstract.RateCurrencyId.name));
            insert.add(CISales.PerceptionCertificate.RateNetTotal, BigDecimal.ZERO);
            insert.add(CISales.PerceptionCertificate.RateDiscountTotal, BigDecimal.ZERO);
            insert.add(CISales.PerceptionCertificate.NetTotal, BigDecimal.ZERO);
            insert.add(CISales.PerceptionCertificate.DiscountTotal, BigDecimal.ZERO);
            insert.add(CISales.PerceptionCertificate.Status, Status.find(CISales.PerceptionCertificateStatus, "Open"));
            insert.add(CISales.PerceptionCertificate.Name, getDocName4Create(_parameter));

            final Object[] rateObj = (Object[]) _createdDoc.getValue(CISales.DocumentSumAbstract.Rate.name);
            final BigDecimal rate = ((BigDecimal) rateObj[0]).divide((BigDecimal) rateObj[1], 12,
                            BigDecimal.ROUND_HALF_UP);
            final DecimalFormat totalFrmt = NumberFormatter.get().getFrmt4Total(getTypeName4SysConf(_parameter));
            final int scale = totalFrmt.getMaximumFractionDigits();

            insert.add(CISales.PerceptionCertificate.RateCrossTotal,
                            perception.setScale(scale, BigDecimal.ROUND_HALF_UP));
            insert.add(CISales.DocumentSumAbstract.CrossTotal, perception.divide(rate, BigDecimal.ROUND_HALF_UP)
                            .setScale(scale, BigDecimal.ROUND_HALF_UP));
            insert.execute();

            final Insert relInsert = new Insert(CISales.PerceptionCertificate2Document);
            relInsert.add(CISales.PerceptionCertificate2Document.FromLink, insert.getInstance());
            relInsert.add(CISales.PerceptionCertificate2Document.ToLink, _createdDoc.getInstance());
            relInsert.execute();
        }
    }

    @Override
    protected String getDocName4Create(final Parameter _parameter)
        throws EFapsException
    {
        final Type type = CISales.PerceptionCertificate.getType();
        final Properties props = ERP.NUMBERGENERATOR.get();
        final String uuid = props.getProperty(type.getName());
        final NumberGenerator numGen = NumberGenerator.get(UUID.fromString(uuid));
        return numGen.getNextVal();
    }
}
