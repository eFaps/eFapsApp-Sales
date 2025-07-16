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

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.EnumUtils;
import org.efaps.abacus.api.ITax;
import org.efaps.abacus.api.TaxType;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.EsjpScanner;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.datetime.JodaTimeUtils;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.RateInfo;
import org.efaps.esjp.sales.PriceUtil_Base.ProductPrice;
import org.efaps.esjp.sales.tax.Tax;
import org.efaps.esjp.sales.tax.TaxCat_Base;
import org.efaps.esjp.sales.tax.xml.TaxEntry;
import org.efaps.esjp.sales.tax.xml.Taxes;
import org.efaps.promotionengine.api.IDocument;
import org.efaps.promotionengine.api.IPromotionsProvider;
import org.efaps.promotionengine.pojo.Document;
import org.efaps.promotionengine.pojo.Position;
import org.efaps.promotionengine.promotion.Promotion;
import org.efaps.util.DateTimeUtil;
import org.efaps.util.EFapsBaseException;
import org.efaps.util.EFapsException;
import org.efaps.util.UUIDUtil;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("78086a4f-7726-4814-9ad1-ce45b507cecc")
@EFapsApplication("eFapsApp-Sales")
public class CalculatorService
{

    private static final Logger LOG = LoggerFactory.getLogger(CalculatorService.class);

    private static Class<?> PROMPROVCLZ;

    private CalculatorConfig config;

    public CalculatorService()
    {
        this(new CalculatorConfig(null));
    }

    public CalculatorService(final String typeKey)
    {
        this(new CalculatorConfig(typeKey));
    }

    public CalculatorService(final CalculatorConfig config)
    {
        this.config = config;
    }

    public void recalculate(final String oid)
        throws EFapsException
    {
        recalculate(Instance.get(oid));
    }

    public void recalculate(final Instance docInst)
        throws EFapsException
    {
        if (InstanceUtils.isKindOf(docInst, CISales.DocumentSumAbstract)) {
            ParameterUtil.instance();

            final var taxMap = new HashMap<String, Tax>();
            final var calcDoc = new Document();

            final var docEval = EQL.builder().print(docInst)
                            .attribute(CISales.DocumentSumAbstract.Date, CISales.DocumentSumAbstract.Rate,
                                            CISales.DocumentSumAbstract.CurrencyId,
                                            CISales.DocumentSumAbstract.RateCurrencyId)
                            .evaluate();
            docEval.next();
            final var date = docEval.get(CISales.DocumentSumAbstract.Date);

            final Object[] rateObj = docEval.get(CISales.DocumentSumAbstract.Rate);
            final var rateCurrency = CurrencyInst.get(docEval.<Long>get(CISales.DocumentSumAbstract.RateCurrencyId));
            final var currency = CurrencyInst.get(docEval.<Long>get(CISales.DocumentSumAbstract.CurrencyId));

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
                final var netUnitPrice = evalNetUnitPrice(date, posEval.inst(), prodInst);
                final var taxCatId = posEval.<Long>get("taxCat");

                final List<ITax> taxes = TaxCat_Base.get(taxCatId).getTaxes().stream()
                                .map(tax -> {
                                    try {
                                        taxMap.put(getTaxKey(tax), tax);
                                        return (ITax) new org.efaps.abacus.pojo.Tax()
                                                        .setKey(getTaxKey(tax))
                                                        .setPercentage(tax.getFactor().multiply(new BigDecimal("100")))
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
                                .setNetUnitPrice(netUnitPrice)
                                .setTaxes(taxes)
                                .setIndex(posEval.get(CISales.PositionSumAbstract.PositionNumber))
                                .setQuantity(posEval.get(CISales.PositionSumAbstract.Quantity))
                                .setProductOid(prodInst.getOid()));
            }
            final var result = calculate(calcDoc);
            LOG.info("Result: {}", result);
            EQL.builder().update(docInst)
                            .set(CISales.DocumentSumAbstract.RateNetTotal, result.getNetTotal())
                            .set(CISales.DocumentSumAbstract.NetTotal,
                                            convert(result.getNetTotal(), rateObj, currency.getInstance()))
                            .set(CISales.DocumentSumAbstract.RateCrossTotal, result.getCrossTotal())
                            .set(CISales.DocumentSumAbstract.CrossTotal,
                                            convert(result.getCrossTotal(), rateObj, currency.getInstance()))
                            .set(CISales.DocumentSumAbstract.RateDiscountTotal, BigDecimal.ZERO)
                            .set(CISales.DocumentSumAbstract.DiscountTotal, BigDecimal.ZERO)
                            .set(CISales.DocumentSumAbstract.Rate, rateObj)
                            .set(CISales.DocumentSumAbstract.Taxes, toTaxes(result.getTaxes(), currency.getUUID()))
                            .set(CISales.DocumentSumAbstract.RateTaxes,
                                            toTaxes(result.getTaxes(), rateCurrency.getUUID()))
                            .execute();

            for (final var pos : result.getPositions()) {
                final var eval = EQL.builder().print().query(CISales.PositionSumAbstract)
                                .where()
                                .attribute(CISales.PositionSumAbstract.DocumentAbstractLink).eq(docInst)
                                .and()
                                .attribute(CISales.PositionSumAbstract.PositionNumber).eq(pos.getIndex())
                                .select()
                                .instance()
                                .evaluate();
                if (eval.next()) {
                    EQL.builder().update(eval.inst())
                                    .set(CISales.PositionSumAbstract.CrossPrice,
                                                    convert(pos.getCrossPrice(), rateObj, currency.getInstance()))
                                    .set(CISales.PositionSumAbstract.CrossUnitPrice,
                                                    convert(pos.getCrossUnitPrice(), rateObj, currency.getInstance()))
                                    .set(CISales.PositionSumAbstract.Discount, BigDecimal.ZERO)
                                    .set(CISales.PositionSumAbstract.DiscountNetUnitPrice,
                                                    convert(pos.getNetPrice(), rateObj, currency.getInstance()))
                                    .set(CISales.PositionSumAbstract.NetPrice,
                                                    convert(pos.getNetPrice(), rateObj, currency.getInstance()))
                                    .set(CISales.PositionSumAbstract.NetUnitPrice,
                                                    convert(pos.getNetUnitPrice(), rateObj, currency.getInstance()))
                                    .set(CISales.PositionSumAbstract.RateNetUnitPrice, pos.getNetUnitPrice())
                                    .set(CISales.PositionSumAbstract.RateCrossPrice, pos.getCrossPrice())
                                    .set(CISales.PositionSumAbstract.RateCrossUnitPrice, pos.getCrossUnitPrice())
                                    .set(CISales.PositionSumAbstract.RateDiscountNetUnitPrice, pos.getNetPrice())
                                    .set(CISales.PositionSumAbstract.RateNetPrice, pos.getNetPrice())
                                    .set(CISales.PositionSumAbstract.RateTaxes,
                                                    toTaxes(pos.getTaxes(), rateCurrency.getUUID()))
                                    .set(CISales.PositionSumAbstract.Taxes,
                                                    toTaxes(result.getTaxes(), currency.getUUID()))
                                    .execute();
                }
            }
        }
    }

    protected BigDecimal evalNetUnitPrice(final Object dateObject,
                                          final Instance positionInst,
                                          final Instance productInst)
        throws EFapsException
    {
        final var dateTime = JodaTimeUtils.toDateTime(DateTimeUtil.toContextDateTime(dateObject));
        return evalPriceFromDB(dateTime, productInst).getCurrentPrice();
    }

    public IDocument calculate(final IDocument document)
        throws EFapsException
    {
        final var calculator = new org.efaps.promotionengine.Calculator(getConfig());
        calculator.calc(document, getPromotions());
        return document;
    }

    public CalculatorConfig getConfig()
    {
        return config;
    }

    public void setConfig(final CalculatorConfig config)
    {
        this.config = config;
    }

    protected BigDecimal convert(final BigDecimal rateAmount,
                                 final Object[] rateObj,
                                 final Instance targetCurrenycInstance)
        throws EFapsException
    {
        final var rateInfo = RateInfo.getRateInfo(rateObj);
        return Currency.convertToCurrency(ParameterUtil.instance(), rateAmount, rateInfo, getConfig().getTypeKey(),
                        targetCurrenycInstance);
    }

    protected Taxes toTaxes(final List<ITax> taxes,
                            final UUID currencyUUID)
    {
        final var result = new Taxes();
        for (final var tax : taxes) {
            final var uuids = tax.getKey().split(":");
            final var entry = new TaxEntry();
            entry.setUUID(UUID.fromString(uuids[1]));
            entry.setCatUUID(UUID.fromString(uuids[0]));
            entry.setAmount(tax.getAmount());
            entry.setBase(tax.getBase());
            entry.setDate(DateTime.now());
            entry.setCurrencyUUID(currencyUUID);
            result.getEntries().add(entry);
        }
        return result;
    }

    public ProductPrice evalPriceFromDB(final DateTime dateTime,
                                        final Instance productInstance)
        throws EFapsException
    {
        final var parameter = ParameterUtil.instance();
        final ProductPrice ret;
        final String setting = getConfig().getPriceEvaluation();
        ret = switch (setting) {
            case "Latest" -> new PriceUtil().getLatestPrice(parameter, productInstance, getPriceEvalType(),
                            true, false);
            case "Latest4Contact" -> new PriceUtil().getLatestPrice(parameter, productInstance, getPriceEvalType(),
                            true, true);
            case "PriceList" ->
                new PriceUtil().getPrice(parameter, productInstance.getOid(), getConfig().getPriceList(),
                                getConfig().getTypeKey());
            default -> new PriceUtil().getPrice(parameter, dateTime, productInstance, getConfig().getPriceList(),
                            getConfig().getTypeKey(), false);
        };
        return ret;
    }

    protected Type getPriceEvalType()
        throws EFapsException
    {
        final String typeStr = CISales.DocumentSumAbstract.getType().getUUID().toString();
        final Type ret;
        if (UUIDUtil.isUUID(typeStr)) {
            ret = Type.get(UUID.fromString(typeStr));
        } else {
            ret = Type.get(typeStr);
        }
        return ret;
    }

    public List<Promotion> getPromotions()
        throws EFapsException
    {
        List<Promotion> ret = new ArrayList<>();
        if (PROMPROVCLZ == null) {
            final var promotionsProviderClasses = new EsjpScanner().scan4SubTypes(IPromotionsProvider.class);
            if (promotionsProviderClasses != null && !promotionsProviderClasses.isEmpty()) {
                PROMPROVCLZ = promotionsProviderClasses.iterator().next();
            }
            if (PROMPROVCLZ == null) {
                PROMPROVCLZ = Object.class;
            }
        }
        if (PROMPROVCLZ != null && PROMPROVCLZ.isInterface()) {
            try {
                final var promotionsProviderImpl = (IPromotionsProvider) PROMPROVCLZ.getConstructor()
                                .newInstance();
                ret = promotionsProviderImpl.getPromotions();
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                            | InvocationTargetException | NoSuchMethodException | SecurityException
                            | EFapsBaseException e) {
                LOG.error("Catched", e);
            }
        }
        return ret;
    }

    public static String getTaxKey(final Tax tax)
        throws EFapsException
    {
        return tax.getTaxCat().getUuid() + ":" + tax.getUUID();
    }
}
