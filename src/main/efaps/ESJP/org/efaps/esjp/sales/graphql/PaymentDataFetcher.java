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
package org.efaps.esjp.sales.graphql;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.eql.builder.Print;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.graphql.BaseDataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNamedType;

@EFapsUUID("90d0c7c2-99a1-4f01-843e-f1aad86fa01c")
@EFapsApplication("eFapsApp-Sales")
public class PaymentDataFetcher
    extends BaseDataFetcher
{

    private static final Logger LOG = LoggerFactory.getLogger(PaymentDataFetcher.class);

    @Override
    public Object get(final DataFetchingEnvironment environment)
        throws Exception
    {
        final Map<String, Object> source = environment.getSource();
        LOG.info("Executing PaymentDataFetcher for {}", source);
        Instance paymentInst = null;
        List<Instance> paymentInsts = null;
        if (source != null) {
            final var paymentObject = source.get(environment.getField().getName());
            if (paymentObject instanceof Instance) {
                paymentInst = (Instance) paymentObject;
            } else if (paymentObject instanceof String) {
                paymentInst = Instance.get((String) paymentObject);
            } else if (paymentObject instanceof final List objectList) {
                paymentInsts = new ArrayList<>();
                for (final var nestedObject : objectList) {
                    if (nestedObject instanceof Instance) {
                        paymentInsts.add((Instance) nestedObject);
                    } else if (nestedObject instanceof String) {
                        paymentInsts.add(Instance.get((String) nestedObject));
                    }
                }
            }
        }
        final GraphQLNamedType graphType;
        if (environment.getFieldType() instanceof GraphQLList) {
            graphType = (GraphQLNamedType) ((GraphQLList) environment.getFieldType()).getWrappedType();
        } else {
            graphType = (GraphQLNamedType) environment.getFieldType();
        }
        final var keys = new String[] { "type", "name", "amount", "authorization", "cardLabel", "cardNumber",
                        "equipmentIdent", "info", "operationDateTime", "operationId", "serviceProvider",
                        "ePaymentTypeValue", "ePaymentTypeDesc", "ePaymentTypeKey" };

        LOG.info("graphTypeName {}", graphType);
        final var keyMapping = getKeyMapping(environment, graphType, keys);
        LOG.info("keyMapping {}", keyMapping);
        Object data = null;
        if (InstanceUtils.isKindOf(paymentInst, CISales.PaymentDocumentIOAbstract)) {
            final var print = EQL.builder()
                            .print(paymentInst);
            addSelects(print, paymentInst.getType());
            final var evaluator = print.evaluate();
            data = getValueMap(keyMapping, evaluator, keys);
        } else if (paymentInsts != null) {
            final var typeGroups = paymentInsts.stream().collect(Collectors.groupingBy(Instance::getType));
            for (final var entry : typeGroups.entrySet()) {
                final var print = EQL.builder()
                                .print(entry.getValue().stream().toArray(Instance[]::new));
                addSelects(print, entry.getKey());
                final var evaluator = print.evaluate();
                data = getValueMaps(keyMapping, evaluator, keys);
            }
        } else {
            // todo
        }
        return DataFetcherResult.newResult()
                        .data(data)
                        .build();
    }

    public void addSelects(final Print print,
                           final Type type)
    {
        print.type().label().as("type")
                        .attribute(CISales.PaymentDocumentIOAbstract.Name).as("name")
                        .attribute(CISales.PaymentDocumentIOAbstract.Amount).as("amount");

        if (CISales.PaymentElectronic.getType().equals(type)) {
            print.attribute(CISales.PaymentElectronic.Authorization).as("authorization")
                            .attribute(CISales.PaymentElectronic.CardLabel).as("cardLabel")
                            .attribute(CISales.PaymentElectronic.CardNumber).as("cardNumber")
                            .attribute(CISales.PaymentElectronic.EquipmentIdent).as("equipmentIdent")
                            .attribute(CISales.PaymentElectronic.Info).as("info")
                            .attribute(CISales.PaymentElectronic.OperationDateTime).as("operationDateTime")
                            .attribute(CISales.PaymentElectronic.OperationId).as("operationId")
                            .attribute(CISales.PaymentElectronic.ServiceProvider).as("serviceProvider")
                            .linkto(CISales.PaymentElectronic.ElectronicPaymentType)
                            .attribute(CISales.AttributeDefinitionPaymentElectronicType.Value)
                            .as("ePaymentTypeValue")
                            .linkto(CISales.PaymentElectronic.ElectronicPaymentType)
                            .attribute(CISales.AttributeDefinitionPaymentElectronicType.Description)
                            .as("ePaymentTypeDesc")
                            .linkto(CISales.PaymentElectronic.ElectronicPaymentType)
                            .attribute(CISales.AttributeDefinitionPaymentElectronicType.MappingKey)
                            .as("ePaymentTypeKey");

        } else if (CISales.PaymentCard.getType().equals(type)) {
            print.attribute(CISales.PaymentCard.Authorization).as("authorization")
                            .attribute(CISales.PaymentCard.CardNumber).as("cardNumber")
                            .attribute(CISales.PaymentCard.Info).as("info")
                            .attribute(CISales.PaymentCard.OperationDateTime).as("operationDateTime")
                            .attribute(CISales.PaymentCard.OperationId).as("operationId")
                            .attribute(CISales.PaymentCard.ServiceProvider).as("serviceProvider")
                            .linkto(CISales.PaymentCard.CardType)
                            .attribute(CISales.AttributeDefinitionPaymentCardType.Value)
                            .as("cardLabel")
                            .linkto(CISales.PaymentCard.CardType)
                            .attribute(CISales.AttributeDefinitionPaymentCardType.Description)
                            .as("cardDesc")
                            .linkto(CISales.PaymentCard.CardType)
                            .attribute(CISales.AttributeDefinitionPaymentCardType.MappingKey)
                            .as("cardKey");
        } else if (CISales.PaymentCash.getType().equals(type)) {
            print.attribute(CISales.PaymentCash.Note).as("info");
        }
    }

}
