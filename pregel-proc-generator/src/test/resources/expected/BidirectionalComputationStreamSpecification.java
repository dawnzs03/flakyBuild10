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
import org.neo4j.gds.executor.ComputationResultConsumer;
import org.neo4j.gds.executor.ExecutionContext;
import org.neo4j.gds.executor.ExecutionMode;
import org.neo4j.gds.executor.GdsCallable;
import org.neo4j.gds.executor.NewConfigFunction;
import org.neo4j.gds.executor.validation.ValidationConfiguration;
import org.neo4j.gds.pregel.proc.PregelBaseProc;
import org.neo4j.gds.pregel.proc.PregelStreamComputationResultConsumer;
import org.neo4j.gds.pregel.proc.PregelStreamResult;

import java.util.stream.Stream;

@GdsCallable(
    name = "gds.pregel.bidirectionalTest.stream",
    executionMode = ExecutionMode.STREAM,
    description = "Bidirectional Test computation description"
)
@Generated("org.neo4j.gds.pregel.PregelProcessor")
public final class BidirectionalComputationStreamSpecification implements AlgorithmSpec<BidirectionalComputationAlgorithm, PregelResult, PregelProcedureConfig, Stream<PregelStreamResult>, BidirectionalComputationAlgorithmFactory> {

    @Override
    public String name() {
        return BidirectionalComputationAlgorithm.class.getSimpleName();
    }

    @Override
    public BidirectionalComputationAlgorithmFactory algorithmFactory(ExecutionContext executionContext) {
        return new BidirectionalComputationAlgorithmFactory();
    }

    @Override
    public NewConfigFunction<PregelProcedureConfig> newConfigFunction() {
        return (__, userInput) -> PregelProcedureConfig.of(userInput);
    }

    @Override
    public ComputationResultConsumer<BidirectionalComputationAlgorithm, PregelResult, PregelProcedureConfig, Stream<PregelStreamResult>> computationResultConsumer() {
        return new PregelStreamComputationResultConsumer<>();
    }

    @Override
    public ValidationConfiguration<PregelProcedureConfig> validationConfig(
        ExecutionContext executionContext) {
        return PregelBaseProc.ensureIndexValidation(executionContext.log(), executionContext.taskRegistryFactory());
    }
}
