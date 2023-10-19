/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.gds.beta.filter;

import org.neo4j.gds.RelationshipType;
import org.neo4j.gds.api.CompositeRelationshipIterator;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.api.IdMap;
import org.neo4j.gds.beta.filter.expression.EvaluationContext;
import org.neo4j.gds.beta.filter.expression.Expression;
import org.neo4j.gds.core.Aggregation;
import org.neo4j.gds.core.concurrency.ParallelUtil;
import org.neo4j.gds.core.concurrency.RunWithConcurrency;
import org.neo4j.gds.core.loading.SingleTypeRelationships;
import org.neo4j.gds.core.loading.construction.GraphFactory;
import org.neo4j.gds.core.loading.construction.RelationshipsBuilder;
import org.neo4j.gds.core.utils.partition.Partition;
import org.neo4j.gds.core.utils.partition.PartitionUtils;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.neo4j.gds.api.AdjacencyCursor.NOT_FOUND;

public final class RelationshipsFilter {

    public static Map<RelationshipType, SingleTypeRelationships> filterRelationships(
        GraphStore graphStore,
        Expression expression,
        IdMap inputNodes,
        IdMap outputNodes,
        int concurrency,
        Map<String, Object> parameterMap,
        ExecutorService executorService,
        ProgressTracker progressTracker
    ) {
        Map<RelationshipType, SingleTypeRelationships> relationshipImportResults = new HashMap<>();

        progressTracker.beginSubTask();

        for (RelationshipType relType : graphStore.relationshipTypes()) {

            progressTracker.beginSubTask(graphStore.relationshipCount(relType));
            var outputRelationships = filterRelationshipType(
                graphStore,
                expression,
                inputNodes,
                outputNodes,
                relType,
                concurrency,
                parameterMap,
                executorService,
                progressTracker
            );

            // Drop relationship types that have been completely filtered out.
            if (outputRelationships.topology().elementCount() == 0) {
                progressTracker.endSubTask();
                continue;
            }

            relationshipImportResults.put(relType, outputRelationships);
            progressTracker.endSubTask();
        }

        progressTracker.endSubTask();

        return relationshipImportResults;
    }

    private static SingleTypeRelationships filterRelationshipType(
        GraphStore graphStore,
        Expression relationshipExpr,
        IdMap inputNodes,
        IdMap outputNodes,
        RelationshipType relType,
        int concurrency,
        Map<String, Object> parameterMap,
        ExecutorService executorService,
        ProgressTracker progressTracker
    ) {
        var propertyKeys = new ArrayList<>(graphStore.relationshipPropertyKeys(relType));

        var propertyConfigs = propertyKeys
            .stream()
            .map(
                propertyKey -> GraphFactory.PropertyConfig.of(
                    propertyKey,
                    Aggregation.NONE,
                    graphStore.relationshipPropertyValues(relType, propertyKey).defaultValue()
                )
            )
            .collect(Collectors.toList());

        var relationshipsBuilder = GraphFactory.initRelationshipsBuilder()
            .nodes(outputNodes)
            .relationshipType(relType)
            .concurrency(concurrency)
            .addAllPropertyConfigs(propertyConfigs)
            .indexInverse(graphStore.inverseIndexedRelationshipTypes().contains(relType))
            .build();

        var compositeIterator = graphStore.getCompositeRelationshipIterator(relType, propertyKeys);

        var propertyIndices = IntStream
            .range(0, propertyKeys.size())
            .boxed()
            .collect(Collectors.toMap(propertyKeys::get, Function.identity()));

        // computing the count is expected to be a lot cheaper than bad partitioning
        var relevantRelationshipsCount = concurrency > 1
            ? ParallelUtil.parallelStream(
                LongStream.range(0, outputNodes.nodeCount()),
                concurrency,
                stream -> stream.map(
                    id -> compositeIterator.degree(inputNodes.toMappedNodeId(outputNodes.toOriginalNodeId(id)))
                ).sum()
            )
            : graphStore.relationshipCount(relType);

        var relationshipFilterTasks = PartitionUtils.degreePartition(
            outputNodes.nodeCount(),
            relevantRelationshipsCount,
            nodeId -> compositeIterator.degree(inputNodes.toMappedNodeId(outputNodes.toOriginalNodeId(nodeId))),
            concurrency,
            partition -> new RelationshipFilterTask(
                partition,
                relationshipExpr,
                compositeIterator.concurrentCopy(),
                inputNodes,
                outputNodes,
                relationshipsBuilder,
                relType,
                parameterMap,
                propertyIndices,
                progressTracker
            ),
            Optional.empty()
        );

        RunWithConcurrency.builder()
            .concurrency(concurrency)
            .tasks(relationshipFilterTasks)
            .executor(executorService)
            .run();

        return relationshipsBuilder.build();
    }

    private RelationshipsFilter() {}

    private static final class RelationshipFilterTask implements Runnable {
        private final Partition partition;
        private final Expression expression;
        private final EvaluationContext.RelationshipEvaluationContext evaluationContext;
        private final ProgressTracker progressTracker;
        private final CompositeRelationshipIterator relationshipIterator;
        private final IdMap inputNodes;
        private final IdMap outputNodes;
        private final RelationshipsBuilder relationshipsBuilder;
        private final RelationshipType relType;

        private RelationshipFilterTask(
            Partition partition,
            Expression expression,
            CompositeRelationshipIterator relationshipIterator,
            IdMap inputNodes,
            IdMap outputNodes,
            RelationshipsBuilder relationshipsBuilder,
            RelationshipType relType,
            Map<String, Object> parameterMap,
            Map<String, Integer> propertyIndices,
            ProgressTracker progressTracker
        ) {
            this.partition = partition;
            this.expression = expression;
            this.relationshipIterator = relationshipIterator;
            this.inputNodes = inputNodes;
            this.outputNodes = outputNodes;
            this.relationshipsBuilder = relationshipsBuilder;
            this.relType = relType;
            this.evaluationContext = new EvaluationContext.RelationshipEvaluationContext(propertyIndices, parameterMap);
            this.progressTracker = progressTracker;
        }

        @Override
        public void run() {
            partition.consume(outputSource -> {
                var neoSource = outputNodes.toOriginalNodeId(outputSource);
                var inputSource = inputNodes.toMappedNodeId(neoSource);

                relationshipIterator.forEachRelationship(inputSource, (source, target, properties) -> {
                    var neoTarget = inputNodes.toOriginalNodeId(target);
                    var mappedTarget = outputNodes.toMappedNodeId(neoTarget);

                    if (mappedTarget != NOT_FOUND) {
                        evaluationContext.init(relType, properties);

                        if (expression.evaluate(evaluationContext) == Expression.TRUE) {
                            // TODO branching should happen somewhere else
                            if (properties.length == 0) {
                                relationshipsBuilder.add(neoSource, neoTarget);
                            } else if (properties.length == 1) {
                                relationshipsBuilder.add(neoSource, neoTarget, properties[0]);
                            } else {
                                relationshipsBuilder.add(neoSource, neoTarget, properties);
                            }
                        }
                    }

                    return true;
                });

                progressTracker.logProgress(relationshipIterator.degree(inputSource));
            });
        }
    }

}
