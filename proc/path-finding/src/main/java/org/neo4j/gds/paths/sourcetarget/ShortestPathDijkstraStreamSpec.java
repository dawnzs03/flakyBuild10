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
package org.neo4j.gds.paths.sourcetarget;

import org.neo4j.gds.executor.AlgorithmSpec;
import org.neo4j.gds.executor.ComputationResultConsumer;
import org.neo4j.gds.executor.ExecutionContext;
import org.neo4j.gds.executor.GdsCallable;
import org.neo4j.gds.executor.NewConfigFunction;
import org.neo4j.gds.paths.ShortestPathStreamResultConsumer;
import org.neo4j.gds.paths.StreamResult;
import org.neo4j.gds.paths.dijkstra.Dijkstra;
import org.neo4j.gds.paths.dijkstra.DijkstraFactory;
import org.neo4j.gds.paths.dijkstra.PathFindingResult;
import org.neo4j.gds.paths.dijkstra.config.ShortestPathDijkstraStreamConfig;

import java.util.stream.Stream;

import static org.neo4j.gds.executor.ExecutionMode.STREAM;
import static org.neo4j.gds.paths.sourcetarget.ShortestPathDijkstraProc.DIJKSTRA_DESCRIPTION;

@GdsCallable(name = "gds.shortestPath.dijkstra.stream", description = DIJKSTRA_DESCRIPTION, executionMode = STREAM)
public class ShortestPathDijkstraStreamSpec implements AlgorithmSpec<Dijkstra, PathFindingResult, ShortestPathDijkstraStreamConfig, Stream<StreamResult>, DijkstraFactory.SourceTargetDijkstraFactory<ShortestPathDijkstraStreamConfig>> {

    @Override
    public String name() {
        return "DijkstraStream";
    }

    @Override
    public DijkstraFactory.SourceTargetDijkstraFactory<ShortestPathDijkstraStreamConfig> algorithmFactory(
        ExecutionContext executionContext
    ) {
        return new DijkstraFactory.SourceTargetDijkstraFactory<>();
    }

    @Override
    public NewConfigFunction<ShortestPathDijkstraStreamConfig> newConfigFunction() {
        return (___,config) -> ShortestPathDijkstraStreamConfig.of(config);
    }

    @Override
    public ComputationResultConsumer<Dijkstra, PathFindingResult, ShortestPathDijkstraStreamConfig, Stream<StreamResult>> computationResultConsumer() {
        return new ShortestPathStreamResultConsumer<>();
    }

    @Override
    public boolean releaseProgressTask() {
        return false;
    }
}
