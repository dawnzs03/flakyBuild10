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
package org.neo4j.gds.paths.kspanningtree;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.api.properties.nodes.LongNodePropertyValues;
import org.neo4j.gds.core.utils.ProgressTimer;
import org.neo4j.gds.executor.AlgorithmSpec;
import org.neo4j.gds.executor.AlgorithmSpecProgressTrackerProvider;
import org.neo4j.gds.executor.ComputationResultConsumer;
import org.neo4j.gds.executor.ExecutionContext;
import org.neo4j.gds.executor.GdsCallable;
import org.neo4j.gds.executor.NewConfigFunction;
import org.neo4j.gds.kspanningtree.KSpanningTree;
import org.neo4j.gds.kspanningtree.KSpanningTreeAlgorithmFactory;
import org.neo4j.gds.kspanningtree.KSpanningTreeWriteConfig;
import org.neo4j.gds.spanningtree.SpanningTree;

import java.util.stream.Stream;

import static org.neo4j.gds.executor.ExecutionMode.WRITE_RELATIONSHIP;

@GdsCallable(name = "gds.alpha.kSpanningTree.write", description = KSpanningWriteTreeProc.DESCRIPTION, executionMode = WRITE_RELATIONSHIP)
public class KSpanningTreeWriteSpec implements AlgorithmSpec<KSpanningTree, SpanningTree, KSpanningTreeWriteConfig, Stream<KSpanningTreeWriteResult>, KSpanningTreeAlgorithmFactory<KSpanningTreeWriteConfig>> {

    @Override
    public String name() {
        return "KSpanningTreeWrite";
    }

    @Override
    public KSpanningTreeAlgorithmFactory<KSpanningTreeWriteConfig> algorithmFactory(ExecutionContext executionContext) {
        return new KSpanningTreeAlgorithmFactory<>();
    }

    @Override
    public NewConfigFunction<KSpanningTreeWriteConfig> newConfigFunction() {
        return (__, config) -> KSpanningTreeWriteConfig.of(config);
    }

    public ComputationResultConsumer<KSpanningTree, SpanningTree, KSpanningTreeWriteConfig, Stream<KSpanningTreeWriteResult>> computationResultConsumer() {

        return (computationResult, executionContext) -> {
            Graph graph = computationResult.graph();
            KSpanningTreeWriteConfig config = computationResult.config();
            KSpanningTree algorithm = computationResult.algorithm();
            KSpanningTreeWriteResult.Builder builder = new KSpanningTreeWriteResult.Builder();

            if (computationResult.result().isEmpty()) {
                return Stream.of(builder.build());
            }

            var spanningTree = computationResult.result().get();

            var properties = new SpanningTreeBackedNodePropertyValues(spanningTree, graph.nodeCount());

            builder.withEffectiveNodeCount(spanningTree.effectiveNodeCount());
            try (ProgressTimer ignored = ProgressTimer.start(builder::withWriteMillis)) {

                executionContext.nodePropertyExporterBuilder()
                    .withIdMap(graph)
                    .withTerminationFlag(algorithm.getTerminationFlag())
                    .withProgressTracker(AlgorithmSpecProgressTrackerProvider.createProgressTracker(
                        name(),
                        graph.nodeCount(),
                        config.writeConcurrency(),
                        executionContext
                    ))
                    .withArrowConnectionInfo(config.arrowConnectionInfo(), computationResult.graphStore().databaseId().databaseName())
                    .build()
                    .write(config.writeProperty(), properties);
            }
            builder.withComputeMillis(computationResult.computeMillis());
            builder.withPreProcessingMillis(computationResult.preProcessingMillis());
            builder.withConfig(config);
            return Stream.of(builder.build());
        };
    }

    private static class SpanningTreeBackedNodePropertyValues implements LongNodePropertyValues {
        private final SpanningTree spanningTree;
        private final long nodeCount;

        SpanningTreeBackedNodePropertyValues(
            SpanningTree spanningTree,
            long nodeCount
        ) {
            this.nodeCount = nodeCount;
            this.spanningTree = spanningTree;
        }

        @Override
        public long nodeCount() {
            return nodeCount;
        }

        @Override
        public long longValue(long nodeId) {
            return spanningTree.head(nodeId);
        }
    }
}
