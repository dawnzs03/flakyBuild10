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
package org.neo4j.gds.paths.spanningtree;

import org.neo4j.gds.Orientation;
import org.neo4j.gds.RelationshipType;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.core.loading.SingleTypeRelationships;
import org.neo4j.gds.core.loading.construction.GraphFactory;
import org.neo4j.gds.core.utils.ProgressTimer;
import org.neo4j.gds.executor.AlgorithmSpec;
import org.neo4j.gds.executor.ComputationResultConsumer;
import org.neo4j.gds.executor.ExecutionContext;
import org.neo4j.gds.executor.GdsCallable;
import org.neo4j.gds.executor.NewConfigFunction;
import org.neo4j.gds.spanningtree.Prim;
import org.neo4j.gds.spanningtree.SpanningGraph;
import org.neo4j.gds.spanningtree.SpanningTree;
import org.neo4j.gds.spanningtree.SpanningTreeAlgorithmFactory;
import org.neo4j.gds.spanningtree.SpanningTreeMutateConfig;

import java.util.stream.Stream;

import static org.neo4j.gds.executor.ExecutionMode.MUTATE_RELATIONSHIP;

@GdsCallable(
    name = "gds.spanningTree.mutate",
    aliases = {"gds.beta.spanningTree.mutate"},
    description = SpanningTreeWriteProc.DESCRIPTION,
    executionMode = MUTATE_RELATIONSHIP
)
public class SpanningTreeMutateSpec implements AlgorithmSpec<Prim, SpanningTree, SpanningTreeMutateConfig, Stream<MutateResult>, SpanningTreeAlgorithmFactory<SpanningTreeMutateConfig>> {

    @Override
    public String name() {
        return "SpanningTreeMutate";
    }

    @Override
    public SpanningTreeAlgorithmFactory<SpanningTreeMutateConfig> algorithmFactory(ExecutionContext executionContext) {
        return new SpanningTreeAlgorithmFactory<>();
    }

    @Override
    public NewConfigFunction<SpanningTreeMutateConfig> newConfigFunction() {
        return (__, config) -> SpanningTreeMutateConfig.of(config);

    }

    public ComputationResultConsumer<Prim, SpanningTree, SpanningTreeMutateConfig, Stream<MutateResult>> computationResultConsumer() {

        return (computationResult, executionContext) -> {
            MutateResult.Builder builder = new MutateResult.Builder();

            if (computationResult.result().isEmpty()) {
                return Stream.of(builder.build());
            }

            Graph graph = computationResult.graph();
            SpanningTree spanningTree = computationResult.result().get();
            SpanningTreeMutateConfig config = computationResult.config();


            var mutateRelationshipType = RelationshipType.of(config.mutateRelationshipType());
            var relationshipsBuilder = GraphFactory
                .initRelationshipsBuilder()
                .relationshipType(mutateRelationshipType)
                .nodes(computationResult.graph())
                .addPropertyConfig(GraphFactory.PropertyConfig.builder().propertyKey(config.mutateProperty()).build())
                .orientation(Orientation.NATURAL)
                .build();

            builder.withEffectiveNodeCount(spanningTree.effectiveNodeCount());
            builder.withTotalWeight(spanningTree.totalWeight());

            SingleTypeRelationships relationships;

            try (ProgressTimer ignored = ProgressTimer.start(builder::withMutateMillis)) {

                var spanningGraph = new SpanningGraph(graph, spanningTree);
                spanningGraph.forEachNode(nodeId -> {
                        spanningGraph.forEachRelationship(nodeId, 1.0, (s, t, w) ->
                            {
                                relationshipsBuilder.addFromInternal(s, t, w);
                                return true;
                            }
                        );
                        return true;
                    }
                );

            }
            relationships = relationshipsBuilder.build();

            computationResult
                .graphStore()
                .addRelationshipType(relationships);

            builder.withComputeMillis(computationResult.computeMillis());
            builder.withPreProcessingMillis(computationResult.preProcessingMillis());
            builder.withRelationshipsWritten(spanningTree.effectiveNodeCount() - 1);
            builder.withConfig(config);
            return Stream.of(builder.build());
        };
    }
}
