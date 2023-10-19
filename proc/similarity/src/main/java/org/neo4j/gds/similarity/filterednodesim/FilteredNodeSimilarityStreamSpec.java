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
package org.neo4j.gds.similarity.filterednodesim;

import org.neo4j.gds.executor.AlgorithmSpec;
import org.neo4j.gds.executor.ComputationResultConsumer;
import org.neo4j.gds.executor.ExecutionContext;
import org.neo4j.gds.executor.ExecutionMode;
import org.neo4j.gds.executor.GdsCallable;
import org.neo4j.gds.executor.NewConfigFunction;
import org.neo4j.gds.similarity.SimilarityResult;
import org.neo4j.gds.similarity.nodesim.NodeSimilarity;
import org.neo4j.gds.similarity.nodesim.NodeSimilarityResult;

import java.util.stream.Stream;

import static org.neo4j.gds.LoggingUtil.runWithExceptionLogging;
import static org.neo4j.gds.similarity.filterednodesim.FilteredNodeSimilarityStreamProc.DESCRIPTION;

@GdsCallable(name = "gds.nodeSimilarity.filtered.stream", aliases = {"gds.alpha.nodeSimilarity.filtered.stream"}, description = DESCRIPTION, executionMode = ExecutionMode.STREAM)
public class FilteredNodeSimilarityStreamSpec implements AlgorithmSpec<
    NodeSimilarity,
    NodeSimilarityResult,
    FilteredNodeSimilarityStreamConfig,
    Stream<SimilarityResult>,
    FilteredNodeSimilarityFactory<FilteredNodeSimilarityStreamConfig>
    > {

    @Override
    public String name() {
        return "FilteredNodeSimilarityStream";
    }

    @Override
    public FilteredNodeSimilarityFactory<FilteredNodeSimilarityStreamConfig> algorithmFactory(ExecutionContext executionContext) {
        return new FilteredNodeSimilarityFactory<>();
    }

    @Override
    public NewConfigFunction<FilteredNodeSimilarityStreamConfig> newConfigFunction() {
        return (__, config) -> FilteredNodeSimilarityStreamConfig.of(config);
    }

    @Override
    public ComputationResultConsumer<NodeSimilarity, NodeSimilarityResult, FilteredNodeSimilarityStreamConfig, Stream<SimilarityResult>> computationResultConsumer() {
        return (computationResult, executionContext) -> runWithExceptionLogging(
            "Result streaming failed",
            executionContext.log(),
            () -> computationResult.result()
                .map(result -> {
                    var graph = computationResult.graph();
                    var similarityResultStream = result.streamResult();
                    return similarityResultStream.map(internalSimilarityResult -> {
                        internalSimilarityResult.node1 = graph.toOriginalNodeId(internalSimilarityResult.node1);
                        internalSimilarityResult.node2 = graph.toOriginalNodeId(internalSimilarityResult.node2);

                        return internalSimilarityResult;
                    });
                }).orElseGet(Stream::empty)
        );
    }

}
