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
package org.neo4j.gds.pagerank;

import org.neo4j.gds.WriteNodePropertiesComputationResultConsumer;
import org.neo4j.gds.api.properties.nodes.EmptyDoubleNodePropertyValues;
import org.neo4j.gds.api.properties.nodes.NodePropertyValuesAdapter;
import org.neo4j.gds.core.write.NodeProperty;
import org.neo4j.gds.executor.AlgorithmSpec;
import org.neo4j.gds.executor.ComputationResult;
import org.neo4j.gds.executor.ComputationResultConsumer;
import org.neo4j.gds.executor.ExecutionContext;
import org.neo4j.gds.executor.GdsCallable;
import org.neo4j.gds.executor.NewConfigFunction;
import org.neo4j.gds.result.AbstractResultBuilder;

import java.util.List;
import java.util.stream.Stream;

import static org.neo4j.gds.executor.ExecutionMode.WRITE_NODE_PROPERTY;
import static org.neo4j.gds.pagerank.PageRankProcCompanion.PAGE_RANK_DESCRIPTION;

@GdsCallable(name = "gds.pageRank.write", description = PAGE_RANK_DESCRIPTION, executionMode = WRITE_NODE_PROPERTY)
public class PageRankWriteSpec implements AlgorithmSpec<PageRankAlgorithm, PageRankResult,PageRankWriteConfig,Stream<WriteResult>,PageRankAlgorithmFactory<PageRankWriteConfig>> {

    @Override
    public String name() {
        return "PageRankWrite";
    }

    @Override
    public PageRankAlgorithmFactory<PageRankWriteConfig> algorithmFactory(ExecutionContext executionContext) {
        return new PageRankAlgorithmFactory<>();
    }

    @Override
    public NewConfigFunction<PageRankWriteConfig> newConfigFunction() {
        return (___,config) -> PageRankWriteConfig.of(config);
    }

    @Override
    public ComputationResultConsumer<PageRankAlgorithm, PageRankResult, PageRankWriteConfig, Stream<WriteResult>> computationResultConsumer() {
        return new WriteNodePropertiesComputationResultConsumer<>(
            this::resultBuilder,
            computationResult ->
                List.of(NodeProperty.of(
                    computationResult.config().writeProperty(),
                    computationResult.result()
                        .map(PageRankResult::scores)
                        .map(NodePropertyValuesAdapter::adapt)
                        .orElse(EmptyDoubleNodePropertyValues.INSTANCE)
                )),
            name()
        );
    }

    private AbstractResultBuilder<WriteResult> resultBuilder(
        ComputationResult<PageRankAlgorithm, PageRankResult, PageRankWriteConfig> computationResult,
        ExecutionContext executionContext
    ) {
        var builder = new WriteResult.Builder(
            executionContext.returnColumns(),
            computationResult.config().concurrency()
        );

        computationResult.result().ifPresent(result -> {
            builder
                .withDidConverge(result.didConverge())
                .withRanIterations(result.iterations())
                .withCentralityFunction(result.scores()::get)
                .withScalerVariant(computationResult.config().scaler());
        });

        return builder;
    }


}
