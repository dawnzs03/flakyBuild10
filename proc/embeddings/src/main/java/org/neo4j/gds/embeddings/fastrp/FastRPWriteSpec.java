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
package org.neo4j.gds.embeddings.fastrp;

import org.neo4j.gds.WriteNodePropertiesComputationResultConsumer;
import org.neo4j.gds.core.write.NodeProperty;
import org.neo4j.gds.executor.AlgorithmSpec;
import org.neo4j.gds.executor.ComputationResult;
import org.neo4j.gds.executor.ComputationResultConsumer;
import org.neo4j.gds.executor.ExecutionContext;
import org.neo4j.gds.executor.ExecutionMode;
import org.neo4j.gds.executor.GdsCallable;
import org.neo4j.gds.executor.NewConfigFunction;

import java.util.List;
import java.util.stream.Stream;

import static org.neo4j.gds.embeddings.fastrp.FastRPCompanion.DESCRIPTION;

@GdsCallable(name = "gds.fastRP.write", description = DESCRIPTION, executionMode = ExecutionMode.WRITE_NODE_PROPERTY)
public class FastRPWriteSpec implements AlgorithmSpec<FastRP, FastRP.FastRPResult, FastRPWriteConfig, Stream<WriteResult>, FastRPFactory<FastRPWriteConfig>> {
    @Override
    public String name() {
        return "FastRPWrite";
    }

    @Override
    public FastRPFactory<FastRPWriteConfig> algorithmFactory(ExecutionContext executionContext) {
        return new FastRPFactory<>();
    }

    @Override
    public NewConfigFunction<FastRPWriteConfig> newConfigFunction() {
        return (__, userInput) -> FastRPWriteConfig.of(userInput);
    }

    @Override
    public ComputationResultConsumer<FastRP, FastRP.FastRPResult, FastRPWriteConfig, Stream<WriteResult>> computationResultConsumer() {
        return new WriteNodePropertiesComputationResultConsumer<>(
            this::resultBuilder,
            computationResult -> List.of(NodeProperty.of(
                computationResult.config().writeProperty(),
                FastRPCompanion.nodeProperties(computationResult)
            )),
            name()
        );
    }

    private WriteResult.Builder resultBuilder(
        ComputationResult<FastRP, FastRP.FastRPResult, FastRPWriteConfig> computeResult,
        ExecutionContext executionContext
    ) {
        return new WriteResult.Builder();
    }
}
