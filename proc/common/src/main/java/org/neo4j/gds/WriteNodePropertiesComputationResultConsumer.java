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
package org.neo4j.gds;

import org.neo4j.gds.api.PropertyState;
import org.neo4j.gds.api.schema.PropertySchema;
import org.neo4j.gds.config.AlgoBaseConfig;
import org.neo4j.gds.config.WritePropertyConfig;
import org.neo4j.gds.core.concurrency.Pools;
import org.neo4j.gds.core.loading.Capabilities.WriteMode;
import org.neo4j.gds.core.utils.ProgressTimer;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.core.utils.progress.tasks.TaskProgressTracker;
import org.neo4j.gds.core.write.NodeProperty;
import org.neo4j.gds.core.write.NodePropertyExporter;
import org.neo4j.gds.executor.ComputationResult;
import org.neo4j.gds.executor.ComputationResultConsumer;
import org.neo4j.gds.executor.ExecutionContext;
import org.neo4j.gds.result.AbstractResultBuilder;

import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.neo4j.gds.LoggingUtil.runWithExceptionLogging;
import static org.neo4j.gds.utils.StringFormatting.formatWithLocale;

public class WriteNodePropertiesComputationResultConsumer<ALGO extends Algorithm<ALGO_RESULT>, ALGO_RESULT, CONFIG extends WritePropertyConfig & AlgoBaseConfig, RESULT>
    implements ComputationResultConsumer<ALGO, ALGO_RESULT, CONFIG, Stream<RESULT>> {

    private final ResultBuilderFunction<ALGO, ALGO_RESULT, CONFIG, RESULT> resultBuilderFunction;
    private final WriteNodePropertyListFunction<ALGO, ALGO_RESULT, CONFIG> nodePropertyListFunction;
    private final String procedureName;

    public WriteNodePropertiesComputationResultConsumer(
        ResultBuilderFunction<ALGO, ALGO_RESULT, CONFIG, RESULT> resultBuilderFunction,
        WriteNodePropertyListFunction<ALGO, ALGO_RESULT, CONFIG> nodePropertyListFunction,
        String procedureName
    ) {
        this.resultBuilderFunction = resultBuilderFunction;
        this.nodePropertyListFunction = nodePropertyListFunction;
        this.procedureName = procedureName;
    }

    @Override
    public Stream<RESULT> consume(
        ComputationResult<ALGO, ALGO_RESULT, CONFIG> computationResult, ExecutionContext executionContext
    ) {
        return runWithExceptionLogging("Graph write failed", executionContext.log(), () -> {
            CONFIG config = computationResult.config();

            AbstractResultBuilder<RESULT> builder = resultBuilderFunction.apply(computationResult, executionContext)
                .withPreProcessingMillis(computationResult.preProcessingMillis())
                .withComputeMillis(computationResult.computeMillis())
                .withNodeCount(computationResult.graph().nodeCount())
                .withConfig(config);

            if (!computationResult.isGraphEmpty()) {
                writeToNeo(builder, computationResult, executionContext);
            }
            return Stream.of(builder.build());
        });
    }

    void writeToNeo(
        AbstractResultBuilder<?> resultBuilder,
        ComputationResult<ALGO, ALGO_RESULT, CONFIG> computationResult,
        ExecutionContext executionContext
    ) {
        try (ProgressTimer ignored = ProgressTimer.start(resultBuilder::withWriteMillis)) {
            var graph = computationResult.graph();
            var config = computationResult.config();
            var progressTracker = createProgressTracker(
                graph.nodeCount(),
                config.writeConcurrency(),
                executionContext
            );
            var writeMode = computationResult.graphStore().capabilities().writeMode();
            var nodePropertySchema = graph.schema().nodeSchema().unionProperties();
            var nodeProperties = nodePropertyListFunction.apply(computationResult);

            validatePropertiesCanBeWritten(
                writeMode,
                nodePropertySchema,
                nodeProperties,
                config.arrowConnectionInfo().isPresent()
            );

            var exporter = executionContext
                .nodePropertyExporterBuilder()
                .withIdMap(graph)
                .withTerminationFlag(computationResult.algorithm().terminationFlag)
                .withProgressTracker(progressTracker)
                .withArrowConnectionInfo(config.arrowConnectionInfo(), computationResult.graphStore().databaseId().databaseName())
                .parallel(Pools.DEFAULT, config.writeConcurrency())
                .build();

            try {
                exporter.write(nodeProperties);
            } finally {
                progressTracker.release();
            }

            resultBuilder.withNodeCount(computationResult.graph().nodeCount());
            resultBuilder.withNodePropertiesWritten(exporter.propertiesWritten());
        }
    }

    ProgressTracker createProgressTracker(
        long taskVolume,
        int writeConcurrency,
        ExecutionContext executionContext
    ) {
        return new TaskProgressTracker(
            NodePropertyExporter.baseTask(this.procedureName, taskVolume),
            executionContext.log(),
            writeConcurrency,
            executionContext.taskRegistryFactory()
        );
    }

    private static void validatePropertiesCanBeWritten(
        WriteMode writeMode,
        Map<String, PropertySchema> propertySchemas,
        Collection<NodeProperty> nodeProperties,
        boolean hasArrowConnectionInfo
    ) {
        if (writeMode == WriteMode.REMOTE && !hasArrowConnectionInfo) {
            throw new IllegalArgumentException("Missing arrow connection information");
        }
        if (writeMode == WriteMode.LOCAL && hasArrowConnectionInfo) {
            throw new IllegalArgumentException(
                "Arrow connection info was given although the write operation is targeting a local database");
        }

        var expectedPropertyState = expectedPropertyStateForWriteMode(writeMode);

        var unexpectedProperties = nodeProperties
            .stream()
            .filter(nodeProperty -> {
                var propertySchema = propertySchemas.get(nodeProperty.propertyKey());
                if (propertySchema == null) {
                    // We are executing an algorithm write mode and the property we are writing is
                    // not in the GraphStore, therefore we do not perform any more checks
                    return false;
                }
                var propertyState = propertySchema.state();
                return !expectedPropertyState.test(propertyState);
            })
            .map(nodeProperty -> formatWithLocale(
                "NodeProperty{propertyKey=%s, propertyState=%s}",
                nodeProperty.propertyKey(),
                propertySchemas.get(nodeProperty.propertyKey()).state()
            ))
            .collect(Collectors.toList());

        if (!unexpectedProperties.isEmpty()) {
            throw new IllegalStateException(formatWithLocale(
                "Expected all properties to be of state `%s` but some properties differ: %s",
                expectedPropertyState,
                unexpectedProperties
            ));
        }
    }

    private static Predicate<PropertyState> expectedPropertyStateForWriteMode(WriteMode writeMode) {
        switch (writeMode) {
            case LOCAL:
                // We need to allow persistent and transient as for example algorithms that support seeding will reuse a
                // mutated (transient) property to write back properties that are in fact backed by a database
                return state -> state == PropertyState.PERSISTENT || state == PropertyState.TRANSIENT;
            case REMOTE:
                // We allow transient properties for the same reason as above
                return state -> state == PropertyState.REMOTE || state == PropertyState.TRANSIENT;
            default:
                throw new IllegalStateException(formatWithLocale(
                    "Graph with write mode `%s` cannot write back to a database",
                    writeMode
                ));
        }
    }
}
