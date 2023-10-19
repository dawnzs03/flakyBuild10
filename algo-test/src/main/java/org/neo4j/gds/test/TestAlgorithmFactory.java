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
package org.neo4j.gds.test;

import org.neo4j.gds.GraphAlgorithmFactory;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.core.utils.mem.MemoryEstimation;
import org.neo4j.gds.core.utils.mem.MemoryEstimations;
import org.neo4j.gds.core.utils.mem.MemoryRange;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;

public class TestAlgorithmFactory<CONFIG extends TestConfig> extends GraphAlgorithmFactory<TestAlgorithm, CONFIG> {

    @Override
    public String taskName() {
        return "TestAlgorithm";
    }

    @Override
    public TestAlgorithm build(
        Graph graph,
        TestConfig configuration,
        ProgressTracker progressTracker
    ) {
        return new TestAlgorithm(
            graph,
            progressTracker,
            configuration.throwInCompute()
        );
    }

    @Override
    public MemoryEstimation memoryEstimation(TestConfig configuration) {
        return MemoryEstimations.builder(TestAlgorithm.class)
            .perNode("nodes", l -> l)
            .perGraphDimension("relationships", (dimensions, concurrency) -> MemoryRange.of(dimensions.relCountUpperBound()))
            .build();
    }
}
