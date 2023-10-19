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
package org.neo4j.gds.modularity;

import org.neo4j.gds.executor.AlgorithmSpec;
import org.neo4j.gds.executor.ComputationResultConsumer;
import org.neo4j.gds.executor.ExecutionContext;
import org.neo4j.gds.executor.GdsCallable;
import org.neo4j.gds.executor.NewConfigFunction;

import java.util.stream.Stream;

import static org.neo4j.gds.executor.ExecutionMode.STATS;
import static org.neo4j.gds.modularity.ModularityStreamProc.DESCRIPTION;

@GdsCallable(name = "gds.modularity.stats", aliases = {"gds.alpha.modularity.stats"}, description = DESCRIPTION, executionMode = STATS)
public class ModularityStatsSpec implements AlgorithmSpec<ModularityCalculator, ModularityResult, ModularityStatsConfig, Stream<StatsResult>, ModularityCalculatorFactory<ModularityStatsConfig>> {
    @Override
    public String name() {
        return "ModularityStats";
    }

    @Override
    public ModularityCalculatorFactory<ModularityStatsConfig> algorithmFactory(ExecutionContext executionContext) {
        return new ModularityCalculatorFactory<>();
    }

    @Override
    public NewConfigFunction<ModularityStatsConfig> newConfigFunction() {
        return (__, userInput) -> ModularityStatsConfig.of(userInput);
    }

    @Override
    public ComputationResultConsumer<ModularityCalculator, ModularityResult, ModularityStatsConfig, Stream<StatsResult>> computationResultConsumer() {
        return (computationResult, executionContext) -> {


            var config = computationResult.config();
            var statsBuilder = new StatsResult.StatsBuilder(executionContext.returnColumns(), config.concurrency());
            var result = computationResult.result()
                .orElseGet(ModularityResult::empty);

            var statsResult = statsBuilder
                .withModularity(result.totalModularity())
                .withCommunityCount(result.communityCount())
                .withRelationshipCount(computationResult.graph().relationshipCount())
                .withPreProcessingMillis(computationResult.preProcessingMillis())
                .withComputeMillis(computationResult.computeMillis())
                .withNodeCount(computationResult.graph().nodeCount())
                .withConfig(computationResult.config())
                .build();

            return Stream.of(statsResult);
        };
    }
}
