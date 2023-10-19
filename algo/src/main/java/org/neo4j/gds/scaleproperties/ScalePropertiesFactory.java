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
package org.neo4j.gds.scaleproperties;

import org.neo4j.gds.GraphAlgorithmFactory;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.core.concurrency.Pools;
import org.neo4j.gds.core.utils.mem.MemoryEstimation;
import org.neo4j.gds.core.utils.mem.MemoryEstimations;
import org.neo4j.gds.core.utils.mem.MemoryRange;
import org.neo4j.gds.core.utils.paged.HugeObjectArray;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.core.utils.progress.tasks.Task;
import org.neo4j.gds.core.utils.progress.tasks.Tasks;
import org.neo4j.gds.mem.MemoryUsage;

public final class ScalePropertiesFactory<CONFIG extends ScalePropertiesBaseConfig> extends GraphAlgorithmFactory<ScaleProperties, CONFIG> {

    private static final int ESTIMATED_DIMENSION_PER_PROPERTY = 128;

    public ScalePropertiesFactory() {
        super();
    }

    @Override
    public String taskName() {
        return "ScaleProperties";
    }

    @Override
    public Task progressTask(Graph graph, CONFIG config) {
        int totalPropertyDimension = config
            .nodeProperties()
            .stream()
            .map(graph::nodeProperties)
            .mapToInt(p -> p.dimension().orElseThrow(/* already validated in config */))
            .sum();
        return Tasks.task(
            taskName(),
            Tasks.leaf("Prepare scalers", graph.nodeCount() * totalPropertyDimension),
            Tasks.leaf("Scale properties", graph.nodeCount() * totalPropertyDimension)
        );
    }

    @Override
    public ScaleProperties build(
        Graph graph,
        CONFIG configuration,
        ProgressTracker progressTracker
    ) {
        return new ScaleProperties(
            graph,
            configuration,
            progressTracker,
            Pools.DEFAULT
        );
    }

    @Override
    public MemoryEstimation memoryEstimation(CONFIG configuration) {
        MemoryEstimations.Builder builder = MemoryEstimations.builder("Scale properties");

        builder.perGraphDimension("Scaled properties", (graphDimensions, concurrency) -> {
                int totalPropertyDimension = configuration
                    .nodeProperties()
                    .stream()
                    .mapToInt(p -> graphDimensions
                        .nodePropertyDimensions()
                        .get(p)
                        .orElse(ESTIMATED_DIMENSION_PER_PROPERTY))
                    .sum();

                return MemoryRange.of(HugeObjectArray.memoryEstimation(
                    graphDimensions.nodeCount(),
                    MemoryUsage.sizeOfDoubleArray(totalPropertyDimension)
                ));
            }
        );
        return builder.build();
    }
}
