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
package org.neo4j.gds.triangle;

import org.neo4j.gds.WriteNodePropertiesComputationResultConsumer;
import org.neo4j.gds.api.properties.nodes.EmptyLongNodePropertyValues;
import org.neo4j.gds.core.write.ImmutableNodeProperty;
import org.neo4j.gds.executor.AlgorithmSpec;
import org.neo4j.gds.executor.ComputationResult;
import org.neo4j.gds.executor.ComputationResultConsumer;
import org.neo4j.gds.executor.ExecutionContext;
import org.neo4j.gds.executor.GdsCallable;
import org.neo4j.gds.executor.NewConfigFunction;
import org.neo4j.gds.result.AbstractResultBuilder;

import java.util.List;
import java.util.stream.Stream;

import static org.neo4j.gds.executor.ExecutionMode.WRITE_NODE_PROPERTY;
import static org.neo4j.gds.triangle.TriangleCountCompanion.DESCRIPTION;

@GdsCallable(name = "gds.triangleCount.write", description = DESCRIPTION, executionMode = WRITE_NODE_PROPERTY)
public class TriangleCountWriteSpec implements AlgorithmSpec<IntersectingTriangleCount, TriangleCountResult, TriangleCountWriteConfig, Stream<TriangleCountWriteResult>, IntersectingTriangleCountFactory<TriangleCountWriteConfig>> {
    @Override
    public String name() {
        return "TriangleCountWrite";
    }

    @Override
    public IntersectingTriangleCountFactory<TriangleCountWriteConfig> algorithmFactory(ExecutionContext executionContext) {
        return new IntersectingTriangleCountFactory<>();
    }

    @Override
    public NewConfigFunction<TriangleCountWriteConfig> newConfigFunction() {
        return (___, config) -> TriangleCountWriteConfig.of(config);
    }

    @Override
    public ComputationResultConsumer<IntersectingTriangleCount, TriangleCountResult, TriangleCountWriteConfig, Stream<TriangleCountWriteResult>> computationResultConsumer() {
        return new WriteNodePropertiesComputationResultConsumer<>(
            this::resultBuilder,
            computationResult -> List.of(ImmutableNodeProperty.of(
                computationResult.config().writeProperty(),
                computationResult.result()
                    .map(TriangleCountResult::asNodeProperties)
                    .orElse(EmptyLongNodePropertyValues.INSTANCE)
            )),
            name()

        );
    }

    private AbstractResultBuilder<TriangleCountWriteResult> resultBuilder(
        ComputationResult<IntersectingTriangleCount, TriangleCountResult, TriangleCountWriteConfig> computationResult,
        ExecutionContext executionContext
    ) {
        var builder = new TriangleCountWriteResult.Builder();

        computationResult.result()
            .ifPresent(result -> builder.withGlobalTriangleCount(result.globalTriangles()));

        return builder;
    }
}
