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
package org.neo4j.gds.pregel.cc;

import javax.annotation.processing.Generated;

import org.neo4j.gds.beta.pregel.PregelProcedureConfig;
import org.neo4j.gds.beta.pregel.PregelResult;
import org.neo4j.gds.executor.AlgorithmSpec;
import org.neo4j.gds.executor.ExecutionContext;
import org.neo4j.gds.executor.ExecutionMode;
import org.neo4j.gds.executor.GdsCallable;
import org.neo4j.gds.executor.NewConfigFunction;
import org.neo4j.gds.pregel.proc.PregelWriteComputationResultConsumer;
import org.neo4j.gds.pregel.proc.PregelWriteResult;

import java.util.Stream;

@GdsCallable(
    name = "gds.pregel.test.write",
    executionMode = ExecutionMode.WRITE_NODE_PROPERTY,
    description = "Test computation description"
)
@Generated("org.neo4j.gds.pregel.PregelProcessor")
public final class ComputationWriteSpecification implements AlgorithmSpec<ComputationAlgorithm, PregelResult, PregelProcedureConfig, Stream<PregelWriteResult>, ComputationAlgorithmFactory> {

    @Override
    public String name() {
        return ComputationAlgorithm.class.getSimpleName();
    }

    @Override
    public ComputationAlgorithmFactory algorithmFactory(ExecutionContext executionContext) {
        return new ComputationAlgorithmFactory();
    }

    @Override
    public NewConfigFunction<PregelProcedureConfig> newConfigFunction() {
        return (__, userInput) -> PregelProcedureConfig.of(userInput);
    }

    @Override
    public PregelWriteComputationResultConsumer<ComputationAlgorithm, PregelProcedureConfig> computationResultConsumer() {
        return new PregelWriteComputationResultConsumer<>();
    }
}
