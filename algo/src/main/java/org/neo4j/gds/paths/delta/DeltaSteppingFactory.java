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
package org.neo4j.gds.paths.delta;

import org.neo4j.gds.GraphAlgorithmFactory;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.core.concurrency.Pools;
import org.neo4j.gds.core.utils.mem.MemoryEstimation;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.core.utils.progress.tasks.Task;
import org.neo4j.gds.core.utils.progress.tasks.Tasks;
import org.neo4j.gds.paths.delta.config.AllShortestPathsDeltaBaseConfig;

import java.util.List;

public class DeltaSteppingFactory<T extends AllShortestPathsDeltaBaseConfig> extends GraphAlgorithmFactory<DeltaStepping, T> {

    @Override
    public DeltaStepping build(
        Graph graph,
        T configuration,
        ProgressTracker progressTracker
    ) {
        return DeltaStepping.of(graph, configuration, Pools.DEFAULT, progressTracker);
    }

    @Override
    public String taskName() {
        return "DeltaStepping";
    }

    @Override
    public Task progressTask(Graph graphOrGraphStore, AllShortestPathsDeltaBaseConfig config) {
        return Tasks.iterativeOpen(
            taskName(),
            () -> List.of(
                Tasks.leaf(DeltaStepping.Phase.RELAX.name()),
                Tasks.leaf(DeltaStepping.Phase.SYNC.name())
            )
        );
    }

    @Override
    public MemoryEstimation memoryEstimation(T configuration) {
        return DeltaStepping.memoryEstimation(true);
    }
}
