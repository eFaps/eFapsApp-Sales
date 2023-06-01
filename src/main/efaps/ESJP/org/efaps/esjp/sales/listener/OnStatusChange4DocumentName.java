/*
 * Copyright 2003 - 2023 The eFaps Team
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
package org.efaps.esjp.sales.listener;

import java.text.NumberFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.attributetype.IStatusChangeListener;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsListener;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.eql2.StmtFlag;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.sales.util.Sales;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("0a4162de-e74a-47d3-9906-27247fc51101")
@EFapsApplication("eFapsApp-Sales")
@EFapsListener
public class OnStatusChange4DocumentName
    implements IStatusChangeListener
{

    final static Pattern PREFIXPATTERN = Pattern.compile("^(\\w*)");

    final static Pattern SUFFIXPATTERN = Pattern.compile("(?<=-)\\d*");

    private static final Logger LOG = LoggerFactory.getLogger(OnStatusChange4DocumentName.class);

    @Override
    public int getWeight()
    {
        return 0;
    }

    @Override
    public void onInsert(final Instance instance,
                         final Long statusId)
        throws EFapsException
    {
        updateName(instance, statusId);
    }

    @Override
    public void onUpdate(final Instance instance,
                         final Long statusId)
        throws EFapsException
    {
        updateName(instance, statusId);
    }

    protected void updateName(final Instance instance,
                              final Long statusId)
        throws EFapsException
    {
        final var properties = Sales.SERIALNUMBERS.get();
        final var type = instance.getType().getName();
        final var statusKey = properties.getProperty(type + ".AssignOnStatus");
        if (statusKey != null) {
            final var status = Status.find(instance.getType().getStatusAttribute().getLink().getUUID(), statusKey);
            if (status != null && status.getId() == statusId) {
                final String name = EQL.builder().print(instance)
                                .attribute(CISales.DocumentAbstract.Name)
                                .evaluate().get(1);
                final Matcher prefixMatcher = PREFIXPATTERN.matcher(name);
                prefixMatcher.find();
                final var serial = prefixMatcher.group();

                final Matcher suffixMatcher = SUFFIXPATTERN.matcher(name);
                suffixMatcher.find();
                if (Integer.parseInt(suffixMatcher.group()) == 0) {
                    Integer numberInt = 1;
                    final var eval = EQL.builder().print().query(type)
                                    .where()
                                    .attribute(CISales.DocumentAbstract.Name).like(serial + "%")
                                    .select()
                                    .attribute(CISales.DocumentAbstract.Name)
                                    .orderBy("1", true)
                                    .limit(1)
                                    .evaluate();
                    if (eval.next()) {
                        final Matcher matcher = SUFFIXPATTERN.matcher(eval.get(CISales.DocumentAbstract.Name));
                        if (matcher.find()) {
                            final String numTmp = matcher.group();
                            numberInt = Integer.parseInt(numTmp) + 1;
                        }
                    }

                    final var serialProps = Sales.SERIALNUMBERS.get();
                    final int length = Integer.valueOf(serialProps.getProperty(type + ".SuffixLength", "6"));
                    final NumberFormat nf = NumberFormat.getInstance();
                    nf.setMinimumIntegerDigits(length);
                    nf.setMaximumIntegerDigits(length);
                    nf.setGroupingUsed(false);
                    final var number = nf.format(numberInt);

                    EQL.builder().with(StmtFlag.TRIGGEROFF)
                                    .update(instance)
                                    .set(CISales.DocumentAbstract.Name, serial + "-" + number)
                                    .execute();
                }
            }
        }
    }
}
