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
package org.neo4j.gds.degree;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.gds.Orientation;
import org.neo4j.gds.TestProgressTracker;
import org.neo4j.gds.compat.Neo4jProxy;
import org.neo4j.gds.compat.TestLog;
import org.neo4j.gds.core.concurrency.Pools;
import org.neo4j.gds.core.utils.mem.MemoryRange;
import org.neo4j.gds.core.utils.paged.HugeDoubleArray;
import org.neo4j.gds.core.utils.progress.EmptyTaskRegistryFactory;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.extension.GdlExtension;
import org.neo4j.gds.extension.GdlGraph;
import org.neo4j.gds.extension.Inject;
import org.neo4j.gds.extension.TestGraph;
import org.neo4j.gds.mem.MemoryUsage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.neo4j.gds.TestSupport.assertMemoryEstimation;
import static org.neo4j.gds.TestSupport.crossArguments;
import static org.neo4j.gds.TestSupport.toArguments;
import static org.neo4j.gds.TestSupport.toArgumentsFlat;

@GdlExtension
final class DegreeCentralityTest {

    @GdlGraph
    private static final String DB_CYPHER =
        "CREATE" +
        "  (a:Label1)" +
        ", (b:Label1)" +
        ", (c:Label1)" +
        ", (d:Label1)" +
        ", (e:Label1)" +
        ", (f:Label1)" +
        ", (g:Label1)" +
        ", (h:Label1)" +
        ", (i:Label1)" +
        ", (j:Label1)" +

        ", (b)-[:TYPE1 {weight: 2.0}]->(c)" +

        ", (c)-[:TYPE1 {weight: 2.0}]->(b)" +

        ", (d)-[:TYPE1 {weight: 2.0}]->(a)" +
        ", (d)-[:TYPE1 {weight: 2.0}]->(b)" +

        ", (e)-[:TYPE1 {weight: 2.0}]->(b)" +
        ", (e)-[:TYPE1 {weight: 2.0}]->(d)" +
        ", (e)-[:TYPE1 {weight: 2.0}]->(f)" +

        ", (f)-[:TYPE1 {weight: 4.0}]->(b)" +
        ", (f)-[:TYPE1 {weight: -2.0}]->(e)";

    @Inject
    private TestGraph graph;

    static Stream<Arguments> degreeCentralityParameters() {
        return crossArguments(
            toArgumentsFlat(() ->
                Stream.of(
                    // Orientation NATURAL
                    List.of(
                        false,
                        Orientation.NATURAL,
                        Map.of("a", 0.0D, "b", 1.0D, "c", 1.0D, "d", 2.0D, "e", 3.0D, "f", 2.0D)
                    ),
                    List.of(
                        true,
                        Orientation.NATURAL,
                        Map.of("a", 0.0D, "b", 2.0D, "c", 2.0D, "d", 4.0D, "e", 6.0D, "f", 4.0D)
                    ),
                    // Orientation REVERSE
                    List.of(
                        false,
                        Orientation.REVERSE,
                        Map.of("a", 1.0D, "b", 4.0D, "c", 1.0D, "d", 1.0D, "e", 1.0D, "f", 1.0D)
                    ),
                    List.of(
                        true,
                        Orientation.REVERSE,
                        Map.of("a", 2.0D, "b", 10.0D, "c", 2.0D, "d", 2.0D, "e", 0.0D, "f", 2.0D)
                    ),
                    // Orientation UNDIRECTED
                    List.of(
                        false,
                        Orientation.UNDIRECTED,
                        Map.of("a", 1.0D, "b", 5.0D, "c", 2.0D, "d", 3.0D, "e", 4.0D, "f", 3.0D)
                    ),
                    List.of(
                        true,
                        Orientation.UNDIRECTED,
                        Map.of("a", 2.0D, "b", 12.0D, "c", 4.0D, "d", 6.0D, "e", 6.0D, "f", 6.0D)
                    )
                )
            ),
            toArguments(() -> Stream.of(1, 4))
        );
    }

    @ParameterizedTest
    @MethodSource("degreeCentralityParameters")
    void shouldComputeCorrectResults(boolean weighted, Orientation orientation, Map<String, Double> expected, int concurrency) {
        var configBuilder = ImmutableDegreeCentralityConfig.builder()
            .concurrency(concurrency)
            .orientation(orientation);

        if (weighted) {
            configBuilder.relationshipWeightProperty("weight");
        }

        // Permit the algo to use a smaller batch size to really run in parallel.
        if (concurrency > 1) {
            configBuilder.minBatchSize(1);
        }

        var config = configBuilder.build();

        var degreeCentrality = new DegreeCentrality(
            graph,
            Pools.DEFAULT,
            config,
            ProgressTracker.NULL_TRACKER
        );

        var degreeFunction = degreeCentrality.compute();
        expected.forEach((variable, expectedDegree) -> {
            long nodeId = graph.toMappedNodeId(variable);
            assertEquals(expectedDegree, degreeFunction.get(nodeId), 1E-6);
        });
    }

    static Stream<Arguments> configParamsAndExpectedMemory() {
        return Stream.of(
            Arguments.of(true, 1, MemoryUsage.sizeOfInstance(DegreeCentrality.class) + HugeDoubleArray.memoryEstimation(10_000L)),
            Arguments.of(true, 4, MemoryUsage.sizeOfInstance(DegreeCentrality.class) + HugeDoubleArray.memoryEstimation(10_000L)),
            Arguments.of(false, 1, MemoryUsage.sizeOfInstance(DegreeCentrality.class)),
            Arguments.of(false, 4, MemoryUsage.sizeOfInstance(DegreeCentrality.class))
        );
    }

    @ParameterizedTest
    @MethodSource("configParamsAndExpectedMemory")
    void testMemoryEstimation(boolean weighted, int concurrency, long expectedMemory) {
        var configBuilder = ImmutableDegreeCentralityConfig.builder();
        if (weighted) {
            configBuilder.relationshipWeightProperty("weight");
        }
        configBuilder.concurrency(concurrency);
        var config = configBuilder.build();
        assertMemoryEstimation(
            () -> new DegreeCentralityFactory<>().memoryEstimation(config),
            10_000L,
            concurrency,
            MemoryRange.of(expectedMemory)
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testProgressLogging(boolean weighted) {
        var configBuilder = ImmutableDegreeCentralityConfig.builder();
        if (weighted) {
            configBuilder.relationshipWeightProperty("weight");
        }
        var config = configBuilder.build();

        var progressTask = new DegreeCentralityFactory<>().progressTask(graph, config);
        var log = Neo4jProxy.testLog();;
        var progressTracker = new TestProgressTracker(progressTask, log, 1, EmptyTaskRegistryFactory.INSTANCE);
        var degreeCentrality = new DegreeCentrality(
            graph,
            Pools.DEFAULT,
            config,
            progressTracker
        );

        degreeCentrality.compute();
        List<AtomicLong> progresses = progressTracker.getProgresses();

        assertEquals(1, progresses.size());
        assertEquals(graph.nodeCount(), progresses.get(0).longValue());

        assertTrue(log.containsMessage(TestLog.INFO, ":: Start"));
        assertTrue(log.containsMessage(TestLog.INFO, ":: Finish"));
    }

    @ParameterizedTest
    @EnumSource(Orientation.class)
    void shouldSupportAllOrientations(Orientation orientation) {
        var config = ImmutableDegreeCentralityConfig
            .builder()
            .orientation(orientation)
            .build();

        var degreeCentrality = new DegreeCentrality(
            graph,
            Pools.DEFAULT,
            config,
            ProgressTracker.NULL_TRACKER
        );

        // should not throw
        degreeCentrality.compute();
    }
}
