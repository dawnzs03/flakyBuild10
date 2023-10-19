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
package org.neo4j.gds.scaling;

import org.neo4j.gds.BaseProc;
import org.neo4j.gds.executor.MemoryEstimationExecutor;
import org.neo4j.gds.executor.ProcedureExecutor;
import org.neo4j.gds.results.MemoryEstimateResult;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.neo4j.gds.scaling.ScalePropertiesProc.SCALE_PROPERTIES_DESCRIPTION;
import static org.neo4j.procedure.Mode.READ;

public class ScalePropertiesStreamProc extends BaseProc {

    @Procedure("gds.scaleProperties.stream")
    @Description(SCALE_PROPERTIES_DESCRIPTION)
    public Stream<Result> stream(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        return new ProcedureExecutor<>(
            new ScalePropertiesStreamSpec(),
            executionContext()
        ).compute(graphName, configuration);
    }

    @Procedure(value = "gds.scaleProperties.stream.estimate", mode = READ)
    @Description(ESTIMATE_DESCRIPTION)
    public Stream<MemoryEstimateResult> estimate(
        @Name(value = "graphNameOrConfiguration") Object graphName,
        @Name(value = "algoConfiguration") Map<String, Object> configuration
    ) {
        var spec = new ScalePropertiesStreamSpec();

        return new MemoryEstimationExecutor<>(
            spec,
            executionContext(),
            transactionContext()
        ).computeEstimate(graphName, configuration);
    }

    @Procedure(value = "gds.alpha.scaleProperties.stream", deprecatedBy = "gds.scaleProperties.stream")
    @Description(SCALE_PROPERTIES_DESCRIPTION)
    public Stream<Result> alphaStream(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        var spec = new ScalePropertiesStreamSpec();
        spec.setAllowL1L2Scalers(true);
        return new ProcedureExecutor<>(
            spec,
            executionContext()
        ).compute(graphName, configuration);
    }

    public static class Result {
        public final long nodeId;
        public final List<Double> scaledProperty;

        public Result(long nodeId, double[] scaledProperty) {
            this.nodeId = nodeId;
            this.scaledProperty = Arrays.stream(scaledProperty).boxed().collect(Collectors.toList());
        }
    }
}
