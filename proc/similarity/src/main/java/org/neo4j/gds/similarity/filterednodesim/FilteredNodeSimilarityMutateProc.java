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
package org.neo4j.gds.similarity.filterednodesim;

import org.neo4j.gds.BaseProc;
import org.neo4j.gds.executor.MemoryEstimationExecutor;
import org.neo4j.gds.executor.ProcedureExecutor;
import org.neo4j.gds.results.MemoryEstimateResult;
import org.neo4j.gds.similarity.SimilarityMutateResult;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Internal;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

import static org.neo4j.gds.similarity.filterednodesim.FilteredNodeSimilarityStreamProc.DESCRIPTION;
import static org.neo4j.procedure.Mode.READ;

public class FilteredNodeSimilarityMutateProc  extends BaseProc {

    @Procedure(name = "gds.nodeSimilarity.filtered.mutate", mode = READ)
    @Description(DESCRIPTION)
    public Stream<SimilarityMutateResult> mutate(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        return new ProcedureExecutor<>(
            new FilteredNodeSimilarityMutateSpec(),
            executionContext()
        ).compute(graphName, configuration);
    }

    @Procedure(name = "gds.nodeSimilarity.filtered.mutate.estimate", mode = READ)
    @Description(ESTIMATE_DESCRIPTION)
    public Stream<MemoryEstimateResult> estimate(
        @Name(value = "graphNameOrConfiguration") Object graphNameOrConfiguration,
        @Name(value = "algoConfiguration") Map<String, Object> algoConfiguration
    ) {
        return new MemoryEstimationExecutor<>(
            new FilteredNodeSimilarityMutateSpec(),
            executionContext(),
            transactionContext()
        ).computeEstimate(graphNameOrConfiguration, algoConfiguration);
    }

    @Deprecated(forRemoval = true)
    @Internal
    @Procedure(name = "gds.alpha.nodeSimilarity.filtered.mutate", mode = READ, deprecatedBy = "gds.nodeSimilarity.filtered.mutate")
    @Description(DESCRIPTION)
    public Stream<SimilarityMutateResult> mutateAlpha(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        executionContext()
            .log()
            .warn("Procedure `gds.alpha.nodeSimilarity.filtered.mutate` has been deprecated, please use `gds.nodeSimilarity.filtered.mutate`.");

        return mutate(graphName, configuration);
    }

    @Deprecated(forRemoval = true)
    @Internal
    @Procedure(name = "gds.alpha.nodeSimilarity.filtered.mutate.estimate", mode = READ, deprecatedBy = "gds.nodeSimilarity.filtered.mutate.estimate")
    @Description(ESTIMATE_DESCRIPTION)
    public Stream<MemoryEstimateResult> estimateAlpha(
        @Name(value = "graphNameOrConfiguration") Object graphNameOrConfiguration,
        @Name(value = "algoConfiguration") Map<String, Object> algoConfiguration
    ) {
        executionContext()
            .log()
            .warn("Procedure `gds.alpha.nodeSimilarity.filtered.mutate.estimate` has been deprecated, please use `gds.nodeSimilarity.filtered.mutate.estimate`.");

        return estimate(graphNameOrConfiguration, algoConfiguration);
    }
}
