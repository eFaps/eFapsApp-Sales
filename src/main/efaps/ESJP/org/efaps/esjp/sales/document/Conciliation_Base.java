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

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.db.Insert;
import org.efaps.esjp.ci.CIFormSales;
import org.efaps.esjp.ci.CISales;
import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
public abstract class Conciliation_Base
    extends AbstractSumOnlyDocument
{
    public Return create(final Parameter _parameter) throws EFapsException
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
        super.add2DocCreate(_parameter, _insert, _createdDoc);

        _insert.add(CISales.DocumentSumAbstract.RateNetTotal, BigDecimal.ZERO);
        _createdDoc.getValues().put(CISales.DocumentSumAbstract.RateNetTotal.name, BigDecimal.ZERO);
        _insert.add(CISales.DocumentSumAbstract.NetTotal, BigDecimal.ZERO);
        _createdDoc.getValues().put(CISales.DocumentSumAbstract.NetTotal.name, BigDecimal.ZERO);
        _insert.add(CISales.DocumentSumAbstract.RateCrossTotal, BigDecimal.ZERO);
        _createdDoc.getValues().put(CISales.DocumentSumAbstract.RateCrossTotal.name, BigDecimal.ZERO);
        _insert.add(CISales.DocumentSumAbstract.CrossTotal, BigDecimal.ZERO);
        _createdDoc.getValues().put(CISales.DocumentSumAbstract.CrossTotal.name, BigDecimal.ZERO);


        final String accountStr = _parameter.getParameterValue(CIFormSales.Sales_ConciliationForm.account.name);
        _insert.add(CISales.Conciliation.AccountLink, accountStr);
        _createdDoc.getValues().put(CISales.DocumentSumAbstract.CrossTotal.name, accountStr);
    }
}
