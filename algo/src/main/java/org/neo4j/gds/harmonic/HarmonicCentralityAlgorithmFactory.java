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
package org.neo4j.gds.harmonic;

import org.neo4j.gds.GraphAlgorithmFactory;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.core.concurrency.Pools;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;

public class HarmonicCentralityAlgorithmFactory<CONFIG extends HarmonicCentralityBaseConfig> extends GraphAlgorithmFactory<HarmonicCentrality, CONFIG> {
    @Override
    public String taskName() {
        return "HarmonicCentrality";
    }

    @Override
    public HarmonicCentrality build(
        Graph graph,
        HarmonicCentralityBaseConfig configuration,
        ProgressTracker progressTracker
    ) {
        return new HarmonicCentrality(
            graph,
            configuration.concurrency(),
            Pools.DEFAULT,
            progressTracker
        );
    }
}
