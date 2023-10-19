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
package org.neo4j.gds.paths;

import org.neo4j.gds.Algorithm;
import org.neo4j.gds.api.IdMap;
import org.neo4j.gds.api.nodeproperties.ValueType;
import org.neo4j.gds.config.AlgoBaseConfig;
import org.neo4j.gds.config.WriteRelationshipConfig;
import org.neo4j.gds.core.utils.ProgressTimer;
import org.neo4j.gds.core.utils.progress.tasks.TaskProgressTracker;
import org.neo4j.gds.core.write.ImmutableExportedRelationship;
import org.neo4j.gds.core.write.RelationshipStreamExporter;
import org.neo4j.gds.executor.ComputationResult;
import org.neo4j.gds.executor.ComputationResultConsumer;
import org.neo4j.gds.executor.ExecutionContext;
import org.neo4j.gds.paths.dijkstra.PathFindingResult;
import org.neo4j.gds.results.StandardWriteRelationshipsResult;
import org.neo4j.values.storable.Value;
import org.neo4j.values.storable.Values;

import java.util.List;
import java.util.stream.Stream;

import static org.neo4j.gds.LoggingUtil.runWithExceptionLogging;
import static org.neo4j.gds.paths.dijkstra.config.ShortestPathDijkstraWriteConfig.COSTS_KEY;
import static org.neo4j.gds.paths.dijkstra.config.ShortestPathDijkstraWriteConfig.NODE_IDS_KEY;
import static org.neo4j.gds.paths.dijkstra.config.ShortestPathDijkstraWriteConfig.TOTAL_COST_KEY;

public class ShortestPathWriteResultConsumer<ALGO extends Algorithm<PathFindingResult>, CONFIG extends AlgoBaseConfig & WriteRelationshipConfig & WritePathOptionsConfig> implements ComputationResultConsumer<ALGO, PathFindingResult, CONFIG, Stream<StandardWriteRelationshipsResult>> {

    @Override
    public Stream<StandardWriteRelationshipsResult> consume(
        ComputationResult<ALGO, PathFindingResult, CONFIG> computationResult, ExecutionContext executionContext
    ) {
        return runWithExceptionLogging("Write relationships failed", executionContext.log(), () -> {
            var config = computationResult.config();

            var resultBuilder = new StandardWriteRelationshipsResult.Builder()
                .withPreProcessingMillis(computationResult.preProcessingMillis())
                .withComputeMillis(computationResult.computeMillis())
                .withConfig(config);

            if (computationResult.result().isEmpty()) {
                return Stream.of(new StandardWriteRelationshipsResult(
                    computationResult.preProcessingMillis(),
                    0L,
                    0L,
                    0L,
                    0L,
                    config.toMap()
                ));
            }

            var algorithm = computationResult.algorithm();
            var result = computationResult.result().get();

            var writeRelationshipType = config.writeRelationshipType();

            boolean writeNodeIds = config.writeNodeIds();
            boolean writeCosts = config.writeCosts();

            var graph = computationResult.graph();

            var relationshipStream = result
                .mapPaths(pathResult -> ImmutableExportedRelationship.of(
                    pathResult.sourceNode(),
                    pathResult.targetNode(),
                    createValues(graph, pathResult, writeNodeIds, writeCosts)
                ));

            var progressTracker = new TaskProgressTracker(
                RelationshipStreamExporter.baseTask("Write shortest Paths"),
                executionContext.log(),
                1,
                executionContext.taskRegistryFactory()
            );

            // this is necessary in order to close the relationship stream which triggers
            // the progress tracker to close its root task
            executionContext.closeableResourceRegistry().register(relationshipStream, () -> {
                var exporter = executionContext.relationshipStreamExporterBuilder()
                    .withIdMappingOperator(computationResult.graph()::toOriginalNodeId)
                    .withRelationships(relationshipStream)
                    .withTerminationFlag(algorithm.getTerminationFlag())
                    .withProgressTracker(progressTracker)
                    .withArrowConnectionInfo(config.arrowConnectionInfo(), computationResult.graphStore().databaseId().databaseName())
                    .build();

                try (ProgressTimer ignored = ProgressTimer.start(resultBuilder::withWriteMillis)) {
                    resultBuilder.withRelationshipsWritten(exporter.write(
                        writeRelationshipType,
                        createKeys(writeNodeIds, writeCosts),
                        createTypes(writeNodeIds, writeCosts)
                    ));
                }
            });

            return Stream.of(resultBuilder.build());
        });
    }

    private List<String> createKeys(boolean writeNodeIds, boolean writeCosts) {
        if (writeNodeIds && writeCosts) {
            return List.of(
                TOTAL_COST_KEY,
                NODE_IDS_KEY,
                COSTS_KEY
            );
        }
        if (writeNodeIds) {
            return List.of(
                TOTAL_COST_KEY,
                NODE_IDS_KEY
            );
        }
        if (writeCosts) {
            return List.of(
                TOTAL_COST_KEY,
                COSTS_KEY
            );
        }
        return List.of(TOTAL_COST_KEY);
    }

    private List<ValueType> createTypes(boolean writeNodeIds, boolean writeCosts) {
        if (writeNodeIds && writeCosts) {
            return List.of(
                ValueType.DOUBLE,
                ValueType.LONG_ARRAY,
                ValueType.DOUBLE_ARRAY
            );
        }
        if (writeNodeIds) {
            return List.of(
                ValueType.DOUBLE,
                ValueType.LONG_ARRAY
            );
        }
        if (writeCosts) {
            return List.of(
                ValueType.DOUBLE,
                ValueType.DOUBLE_ARRAY
            );
        }
        return List.of(ValueType.DOUBLE);
    }

    private Value[] createValues(IdMap idMap, PathResult pathResult, boolean writeNodeIds, boolean writeCosts) {
        if (writeNodeIds && writeCosts) {
            return new Value[]{
                Values.doubleValue(pathResult.totalCost()),
                Values.longArray(toOriginalIds(idMap, pathResult.nodeIds())),
                Values.doubleArray(pathResult.costs())
            };
        }
        if (writeNodeIds) {
            return new Value[]{
                Values.doubleValue(pathResult.totalCost()),
                Values.longArray(toOriginalIds(idMap, pathResult.nodeIds())),
            };
        }
        if (writeCosts) {
            return new Value[]{
                Values.doubleValue(pathResult.totalCost()),
                Values.doubleArray(pathResult.costs())
            };
        }
        return new Value[]{
            Values.doubleValue(pathResult.totalCost()),
        };
    }

    // Replaces the ids in the given array with the original ids
    private long[] toOriginalIds(IdMap idMap, long[] internalIds) {
        for (int i = 0; i < internalIds.length; i++) {
            internalIds[i] = idMap.toOriginalNodeId(internalIds[i]);
        }
        return internalIds;
    }
}
