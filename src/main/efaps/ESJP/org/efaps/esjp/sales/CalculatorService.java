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
package org.efaps.esjp.sales;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.EnumUtils;
import org.efaps.abacus.api.IConfig;
import org.efaps.abacus.api.ITax;
import org.efaps.abacus.api.TaxType;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.datetime.JodaTimeUtils;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.sales.tax.Tax;
import org.efaps.esjp.sales.tax.TaxCat_Base;
import org.efaps.promotionengine.api.IDocument;
import org.efaps.promotionengine.pojo.Document;
import org.efaps.promotionengine.pojo.Position;
import org.efaps.util.DateTimeUtil;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("78086a4f-7726-4814-9ad1-ce45b507cecc")
@EFapsApplication("eFapsApp-Sales")
public class CalculatorService
{

    private static final Logger LOG = LoggerFactory.getLogger(CalculatorService.class);

    public void recalculate(final String oid)
        throws EFapsException
    {
        recalculate(Instance.get(oid));
    }

    public void recalculate(final Instance docInst)
        throws EFapsException
    {
        if (InstanceUtils.isKindOf(docInst, CISales.DocumentSumAbstract)) {
            final var parameter = ParameterUtil.instance();

            final var taxMap = new HashMap<String, Tax>();
            final var calcDoc = new Document();

            final var docEval = EQL.builder().print(docInst)
                            .attribute(CISales.DocumentSumAbstract.Date)
                            .evaluate();
            docEval.next();
            final var date = docEval.get(CISales.DocumentSumAbstract.Date);
            final var dateTime = JodaTimeUtils.toDateTime(DateTimeUtil.toDateTime(date));

            final var posEval = EQL.builder().print().query(CISales.PositionSumAbstract)
                            .where()
                            .attribute(CISales.PositionSumAbstract.DocumentAbstractLink).eq(docInst)
                            .select()
                            .attribute(CISales.PositionSumAbstract.PositionNumber, CISales.PositionSumAbstract.Quantity)
                            .linkto(CISales.PositionSumAbstract.Product).instance().as("productInst")
                            .linkto(CISales.PositionSumAbstract.Product)
                            .attribute(CIProducts.ProductAbstract.TaxCategory).as("taxCat")
                            .evaluate();

            while (posEval.next()) {
                final Instance prodInst = posEval.get("productInst");
                final var posKey = posEval.inst().getType().getName();
                final var prodPrice = new PriceUtil().getPrice(parameter, dateTime, prodInst,
                                CIProducts.ProductPricelistRetail.uuid, posKey, false);
                final var taxCatId = posEval.<Long>get("taxCat");

                final List<ITax> taxes = TaxCat_Base.get(taxCatId).getTaxes().stream()
                                .map(tax -> {
                                    try {
                                        taxMap.put(tax.getName(), tax);

                                        return (ITax) new org.efaps.abacus.pojo.Tax()
                                                        .setKey(tax.getName())
                                                        .setPercentage(tax.getFactor())
                                                        .setAmount(tax.getAmount())
                                                        .setType(EnumUtils.getEnum(TaxType.class,
                                                                        tax.getTaxType().name()));
                                    } catch (final EFapsException e) {
                                        LOG.error("Catched", e);
                                    }
                                    return null;
                                })
                                .toList();

                calcDoc.addPosition(new Position()
                                .setNetUnitPrice(prodPrice.getCurrentPrice())
                                .setTaxes(taxes)
                                .setIndex(posEval.get(CISales.PositionSumAbstract.PositionNumber))
                                .setQuantity(posEval.get(CISales.PositionSumAbstract.Quantity))
                                .setProductOid(prodInst.getOid()));
            }
            final var result = calculate(calcDoc);
            LOG.info("Result: {}", result);
        }

    }

    public IDocument calculate(final IDocument document)
    {
        final var calculator = new org.efaps.promotionengine.Calculator(getConfig());
        calculator.calc(document, new ArrayList<>());
        return document;
    }

    protected IConfig getConfig()
    {
        return new CalculatorConfig();
    }

}
