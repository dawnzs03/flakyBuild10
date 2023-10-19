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
package org.neo4j.gds.paths.singlesource.dijkstra;

import org.neo4j.gds.BaseProc;
import org.neo4j.gds.core.write.RelationshipStreamExporterBuilder;
import org.neo4j.gds.executor.ExecutionContext;
import org.neo4j.gds.executor.MemoryEstimationExecutor;
import org.neo4j.gds.executor.ProcedureExecutor;
import org.neo4j.gds.executor.ProcedureExecutorSpec;
import org.neo4j.gds.paths.dijkstra.Dijkstra;
import org.neo4j.gds.paths.dijkstra.PathFindingResult;
import org.neo4j.gds.paths.dijkstra.config.AllShortestPathsDijkstraWriteConfig;
import org.neo4j.gds.results.MemoryEstimateResult;
import org.neo4j.gds.results.StandardWriteRelationshipsResult;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

import static org.neo4j.procedure.Mode.READ;
import static org.neo4j.procedure.Mode.WRITE;

public class AllShortestPathsDijkstraWriteProc extends BaseProc {

    @Context
    public RelationshipStreamExporterBuilder relationshipStreamExporterBuilder;

    @Procedure(name = "gds.allShortestPaths.dijkstra.write", mode = WRITE)
    @Description(Dijkstra.DESCRIPTION_SOURCE_TARGET)
    public Stream<StandardWriteRelationshipsResult> stream(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        var writeSpec = new AllShortestPathsDijkstraWriteSpec();
        var pipelineSpec = new ProcedureExecutorSpec<Dijkstra, PathFindingResult, AllShortestPathsDijkstraWriteConfig>();

        return new ProcedureExecutor<>(
            writeSpec,
            pipelineSpec,
            executionContext()
        ).compute(graphName, configuration);
    }

    @Procedure(name = "gds.allShortestPaths.dijkstra.write.estimate", mode = READ)
    @Description(ESTIMATE_DESCRIPTION)
    public Stream<MemoryEstimateResult> estimate(
        @Name(value = "graphNameOrConfiguration") Object graphNameOrConfiguration,
        @Name(value = "algoConfiguration") Map<String, Object> algoConfiguration
    ) {
        var writeSpec = new AllShortestPathsDijkstraWriteSpec();
        var pipelineSpec = new ProcedureExecutorSpec<Dijkstra, PathFindingResult, AllShortestPathsDijkstraWriteConfig>();

        return new MemoryEstimationExecutor<>(
            writeSpec,
            pipelineSpec,
            executionContext(),
            transactionContext()
        ).computeEstimate(graphNameOrConfiguration, algoConfiguration);
    }

    @Override
    public ExecutionContext executionContext() {
        return super.executionContext().withRelationshipStreamExporterBuilder(relationshipStreamExporterBuilder);
    }
}
