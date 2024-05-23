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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.eql.builder.Print;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.graphql.AbstractDataFetcher;
import org.efaps.esjp.products.util.Products.ProductIndividual;
import org.efaps.graphql.definition.FieldDef;
import org.efaps.graphql.definition.ObjectDef;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLNamedType;

@EFapsUUID("7c5b11cc-6b9b-4c3c-88cc-0b1c7f5bafd8")
@EFapsApplication("eFapsApp-Sales")
public class IndividualProductDataFetcher
    extends AbstractDataFetcher
{

    private static final Logger LOG = LoggerFactory.getLogger(IndividualProductDataFetcher.class);

    @Override
    public Object get(final DataFetchingEnvironment environment)
        throws Exception
    {
        Map<String, Object> values = null;

        LOG.info("Running IndividualDataFetcher with: {}", environment);
        final Map<String, Object> source = environment.getSource();
        LOG.info("    source: {}", source);

        final Instance positionInstance = (Instance) source.get("currentInstance");
        if (InstanceUtils.isKindOf(positionInstance, CISales.PositionSumAbstract)) {
            final var eval = EQL.builder().print(positionInstance)
                            .attribute(CISales.PositionSumAbstract.PositionNumber)
                            .linkto(CISales.PositionSumAbstract.Product).instance().as("prodInstance")
                            .linkto(CISales.PositionSumAbstract.Product)
                            .attribute(CIProducts.ProductAbstract.Individual).as("prodIndividual")

                            .linkto(CISales.PositionSumAbstract.DocumentAbstractLink)
                            .linkfrom(CISales.Document2TransactionDocumentShadowAbstract.FromAbstractLink)
                            .linkto(CISales.Document2TransactionDocumentShadowAbstract.ToAbstractLink)
                            .linkfrom(CIProducts.TransactionIndividualAbstract.Document)
                            .attribute(CIProducts.TransactionIndividualAbstract.ID)
                            .as("transId")

                            .linkto(CISales.PositionSumAbstract.DocumentAbstractLink)
                            .linkfrom(CISales.Document2TransactionDocumentShadowAbstract.FromAbstractLink)
                            .linkto(CISales.Document2TransactionDocumentShadowAbstract.ToAbstractLink)
                            .linkfrom(CIProducts.TransactionIndividualAbstract.Document)
                            .linkto(CIProducts.TransactionIndividualAbstract.Product).instance()
                            .as("individualInstances")
                            .evaluate();
            if (eval.next()) {
                final ProductIndividual ind = eval.get("prodIndividual");
                LOG.info("    Product has individual: {}", ind);
                if (!ProductIndividual.NONE.equals(ind)) {
                    final Object individualInstances = eval.get("individualInstances");
                    LOG.info("    Related individual Products: {}", individualInstances);
                    if (individualInstances != null && individualInstances instanceof List) {
                        @SuppressWarnings("unchecked") final var instances = ((List<Instance>) individualInstances)
                                        .stream().filter(Objects::nonNull)
                                        .toList();
                        if (instances.size() == 1) {
                            final var print = EQL.builder().print(instances.get(0));
                            addSelect4Field(environment, print);
                            values = evaluate(environment, print);
                        } else if (instances.size() > 1) {
                            final Map<Long, Instance> treeMap = new TreeMap<>();
                            final var transactionIds = eval.<List<Long>>get("transId");
                            final var instIter = instances.iterator();
                            for (final var transactionId :  transactionIds) {
                                treeMap.put(transactionId, instIter.next());
                            }
                            final Integer positionNumber = eval.get(CISales.PositionSumAbstract.PositionNumber);
                            final var inst = treeMap.values().stream().toList().get(positionNumber - 1);
                            final var print = EQL.builder().print(inst);
                            addSelect4Field(environment, print);
                            values = evaluate(environment, print);
                        }
                    }
                }
            }
        }
        return values;
    }

    protected void addSelect4Field(final DataFetchingEnvironment environment,
                                   final Print print)
    {
        final GraphQLNamedType objectType = (GraphQLNamedType) environment.getFieldType();
        LOG.info("    objectType: {}", objectType);
        final Optional<ObjectDef> objectDefOpt = environment.getGraphQlContext().getOrEmpty(objectType.getName());
        LOG.info("    objectDef: {}", objectDefOpt.get());
        final var currentObjDef = objectDefOpt.get();
        for (final var selectedField : environment.getSelectionSet().getImmediateFields()) {
            LOG.info("    selectedField: {}", selectedField);
            final FieldDef chieldFieldDef = currentObjDef.getFields().get(selectedField.getName());
            if (StringUtils.isNotBlank(chieldFieldDef.getSelect())) {
                LOG.info("    adding: {}", chieldFieldDef.getSelect());
                print.select(chieldFieldDef.getSelect()).as(selectedField.getFullyQualifiedName());
            }
        }
    }

    protected Map<String, Object> evaluate(final DataFetchingEnvironment environment,
                                           final Print print)
        throws EFapsException
    {
        final var map = new HashMap<String, Object>();
        final var eval = print.evaluate();
        if (eval.next()) {
            for (final var selectedField : environment.getSelectionSet().getImmediateFields()) {
                map.put(selectedField.getName(), eval.get(selectedField.getFullyQualifiedName()));
            }
        }
        return map;
    }
}
