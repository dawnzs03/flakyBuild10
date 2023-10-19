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
package org.neo4j.gds.leiden;

import org.neo4j.gds.GraphAlgorithmFactory;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.core.utils.mem.MemoryEstimation;
import org.neo4j.gds.core.utils.mem.MemoryEstimations;
import org.neo4j.gds.core.utils.paged.HugeDoubleArray;
import org.neo4j.gds.core.utils.paged.HugeLongArray;
import org.neo4j.gds.core.utils.progress.tasks.IterativeTask;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.core.utils.progress.tasks.Task;
import org.neo4j.gds.core.utils.progress.tasks.Tasks;

import java.util.List;
import java.util.Optional;

public class LeidenAlgorithmFactory<CONFIG extends LeidenBaseConfig> extends GraphAlgorithmFactory<Leiden, CONFIG> {
    @Override
    public Leiden build(Graph graph, CONFIG configuration, ProgressTracker progressTracker) {

        if (!graph.schema().isUndirected()) {
            throw new IllegalArgumentException(
                "The Leiden algorithm works only with undirected graphs. Please orient the edges properly");
        }
        var seedValues = Optional
            .ofNullable(configuration.seedProperty()).map(graph::nodeProperties).orElse(null);

        return new Leiden(
            graph,
            configuration.maxLevels(),
            configuration.gamma(),
            configuration.theta(),
            configuration.includeIntermediateCommunities(),
            configuration.randomSeed().orElse(0L),
            seedValues,
            configuration.tolerance(),
            configuration.concurrency(),
            progressTracker
        );
    }

    @Override
    public String taskName() {
        return "Leiden";
    }

    @Override
    public Task progressTask(Graph graph, CONFIG config) {

        var iterations = config.maxLevels();

        IterativeTask iterativeTasks = Tasks.iterativeDynamic(
            "Iteration",
            () ->
                List.of(
                    Tasks.leaf("Local Move", 1),
                    Tasks.leaf("Modularity Computation", graph.nodeCount()),
                    Tasks.leaf("Refinement", graph.nodeCount()),
                    Tasks.leaf("Aggregation", graph.nodeCount())
                ),
            iterations
        );
        var initilizationTask = Tasks.leaf("Initialization", graph.nodeCount());

        return Tasks.task("Leiden", initilizationTask, iterativeTasks);
    }

    @Override
    public MemoryEstimation memoryEstimation(CONFIG config) {
        var builder = MemoryEstimations.builder(Leiden.class)
            .perNode("local move communities", HugeLongArray::memoryEstimation)
            .perNode("local move node volumes", HugeDoubleArray::memoryEstimation)
            .perNode("local move community volumes", HugeDoubleArray::memoryEstimation)
            .perNode("current communities", HugeLongArray::memoryEstimation);
        if (config.seedProperty() != null) {
            builder.add("seeded communities", SeedCommunityManager.memoryEstimation());
        }
        builder
            .add("local move phase", LocalMovePhase.estimation())
            .add("modularity computation", ModularityComputer.estimation())
            .add("dendogram manager", LeidenDendrogramManager.memoryEstimation(
                config.includeIntermediateCommunities() ? config.maxLevels() : 1
            ))
            .add("refinement phase", RefinementPhase.memoryEstimation())
            .add("aggregation phase", GraphAggregationPhase.memoryEstimation())
            .add("post-aggregation phase", MemoryEstimations.builder()
                .perNode("next local move communities", HugeLongArray::memoryEstimation)
                .perNode("next local move node volumes", HugeDoubleArray::memoryEstimation)
                .perNode("next local move community volumes", HugeDoubleArray::memoryEstimation)
                .perNode("community to node map", HugeLongArray::memoryEstimation)
                .build()
            );
            return builder.build();
    }
}
