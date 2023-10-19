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
import org.neo4j.gds.MutateComputationResultConsumer;
import org.neo4j.gds.Orientation;
import org.neo4j.gds.RelationshipType;
import org.neo4j.gds.config.AlgoBaseConfig;
import org.neo4j.gds.config.MutateRelationshipConfig;
import org.neo4j.gds.core.loading.SingleTypeRelationships;
import org.neo4j.gds.core.loading.construction.GraphFactory;
import org.neo4j.gds.executor.ComputationResult;
import org.neo4j.gds.executor.ExecutionContext;
import org.neo4j.gds.paths.dijkstra.PathFindingResult;
import org.neo4j.gds.result.AbstractResultBuilder;

import static org.neo4j.gds.paths.dijkstra.config.ShortestPathDijkstraWriteConfig.TOTAL_COST_KEY;

public class ShortestPathMutateResultConsumer<ALGO extends Algorithm<PathFindingResult>, CONFIG extends AlgoBaseConfig & MutateRelationshipConfig> extends MutateComputationResultConsumer<ALGO, PathFindingResult, CONFIG, MutateResult> {

    public ShortestPathMutateResultConsumer() {
        super((computationResult, executionContext) -> new MutateResult.Builder()
            .withPreProcessingMillis(computationResult.preProcessingMillis())
            .withComputeMillis(computationResult.computeMillis())
            .withConfig(computationResult.config()));
    }

    @Override
    protected void updateGraphStore(
        AbstractResultBuilder<?> resultBuilder,
        ComputationResult<ALGO, PathFindingResult, CONFIG> computationResult,
        ExecutionContext executionContext
    ) {
        var config = computationResult.config();

        var mutateRelationshipType = RelationshipType.of(config.mutateRelationshipType());

        var relationshipsBuilder = GraphFactory
            .initRelationshipsBuilder()
            .relationshipType(mutateRelationshipType)
            .nodes(computationResult.graph())
            .addPropertyConfig(GraphFactory.PropertyConfig.of(TOTAL_COST_KEY))
            .orientation(Orientation.NATURAL)
            .build();

        SingleTypeRelationships relationships;

        computationResult.result().ifPresent(result -> {
            result.forEachPath(pathResult -> {
                relationshipsBuilder.addFromInternal(
                    pathResult.sourceNode(),
                    pathResult.targetNode(),
                    pathResult.totalCost()
                );
            });
        });

        relationships = relationshipsBuilder.build();
        resultBuilder.withRelationshipsWritten(relationships.topology().elementCount());

        computationResult
            .graphStore()
            .addRelationshipType(relationships);
    }
}
