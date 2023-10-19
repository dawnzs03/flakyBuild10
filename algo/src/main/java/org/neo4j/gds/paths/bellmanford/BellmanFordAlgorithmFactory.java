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
package org.neo4j.gds.paths.bellmanford;

import org.neo4j.gds.GraphAlgorithmFactory;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.core.utils.mem.MemoryEstimation;
import org.neo4j.gds.core.utils.mem.MemoryEstimations;
import org.neo4j.gds.core.utils.paged.HugeAtomicBitSet;
import org.neo4j.gds.core.utils.paged.HugeLongArray;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.core.utils.progress.tasks.Task;
import org.neo4j.gds.core.utils.progress.tasks.Tasks;

import java.util.List;

public class BellmanFordAlgorithmFactory<CONFIG extends BellmanFordBaseConfig> extends GraphAlgorithmFactory<BellmanFord, CONFIG> {

    @Override
    public BellmanFord build(Graph graphOrGraphStore, CONFIG configuration, ProgressTracker progressTracker) {
        return new BellmanFord(
            graphOrGraphStore,
            progressTracker,
            graphOrGraphStore.toMappedNodeId(configuration.sourceNode()),
            configuration.trackNegativeCycles(),
            configuration.trackPaths(),
            configuration.concurrency()
        );
    }

    @Override
    public String taskName() {
        return "BellmanFord";
    }

    @Override
    public Task progressTask(Graph graphOrGraphStore, BellmanFordBaseConfig config) {
        return Tasks.iterativeOpen(
            taskName(),
            () -> List.of(
                Tasks.leaf("Relax"),
                Tasks.leaf("Sync")
            )
        );
    }

    @Override
    public MemoryEstimation memoryEstimation(CONFIG configuration) {
        var builder = MemoryEstimations.builder(BellmanFord.class)
            .perNode("frontier", HugeLongArray::memoryEstimation)
            .perNode("validBitset", HugeAtomicBitSet::memoryEstimation)
            .add(DistanceTracker.memoryEstimation())
            .perThread("BellmanFordTask", BellmanFordTask.memoryEstimation());

        if(configuration.trackNegativeCycles()) {
            builder.perNode("negativeCyclesVertices", HugeLongArray::memoryEstimation);
        }

        return builder.build();
    }
}
