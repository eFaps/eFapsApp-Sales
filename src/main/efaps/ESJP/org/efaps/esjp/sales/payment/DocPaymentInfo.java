/*
 * Copyright 2003 - 2015 The eFaps Team
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

package org.efaps.esjp.sales.payment;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.util.EFapsException;

/**
 * This class must be replaced for customization, therefore it is left empty.
 * Functional description can be found in the related "<code>_base</code>"
 * class.
 *
 * @author The eFaps Team
 */
@EFapsUUID("e59d7d3e-21ed-45f3-addc-02672734ad80")
@EFapsApplication("eFapsApp-Sales")
public class DocPaymentInfo
    extends DocPaymentInfo_Base
{

    /**
     * @param _docInst Instance of the document
     */
    public DocPaymentInfo(final Instance _docInst)
    {
        super(_docInst);
    }

    public static void initialize(final Parameter _parameter,
                                  final DocPaymentInfo_Base... _infos)
        throws EFapsException
    {
        DocPaymentInfo_Base.initialize(_parameter, _infos);
    }

    public static void initBaseDoc(final Parameter _parameter,
                                   final DocPaymentInfo_Base... _infos)
        throws EFapsException
    {
        DocPaymentInfo_Base.initBaseDoc(_parameter, _infos);
    }

    public static void initPayments(final Parameter _parameter,
                                    final DocPaymentInfo_Base... _infos)
        throws EFapsException
    {
        DocPaymentInfo_Base.initPayments(_parameter, _infos);
    }

    public static void initDetraction(final Parameter _parameter,
                                      final DocPaymentInfo_Base... _infos)
        throws EFapsException
    {
        DocPaymentInfo_Base.initDetraction(_parameter, _infos);
    }

    public static void initRetention(final Parameter _parameter,
                                        final DocPaymentInfo_Base... _infos)
        throws EFapsException
    {
        DocPaymentInfo_Base.initRetention(_parameter, _infos);
    }

    public static void initSwap(final Parameter _parameter,
                                   final DocPaymentInfo_Base... _infos)
        throws EFapsException
    {
        DocPaymentInfo_Base.initSwap(_parameter, _infos);
    }

}
