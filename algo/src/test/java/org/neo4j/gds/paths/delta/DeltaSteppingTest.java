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
package org.neo4j.gds.paths.delta;

import org.assertj.core.api.recursive.comparison.ComparingFields;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.gds.TestProgressTracker;
import org.neo4j.gds.TestSupport;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.api.schema.Direction;
import org.neo4j.gds.beta.generator.PropertyProducer;
import org.neo4j.gds.beta.generator.RandomGraphGeneratorBuilder;
import org.neo4j.gds.beta.generator.RelationshipDistribution;
import org.neo4j.gds.compat.Neo4jProxy;
import org.neo4j.gds.core.GraphDimensions;
import org.neo4j.gds.core.concurrency.Pools;
import org.neo4j.gds.core.utils.progress.EmptyTaskRegistryFactory;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.extension.GdlExtension;
import org.neo4j.gds.extension.GdlGraph;
import org.neo4j.gds.extension.IdFunction;
import org.neo4j.gds.extension.Inject;
import org.neo4j.gds.extension.TestGraph;
import org.neo4j.gds.paths.delta.config.ImmutableAllShortestPathsDeltaStreamConfig;
import org.neo4j.gds.paths.dijkstra.Dijkstra;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.neo4j.gds.assertj.Extractors.removingThreadId;
import static org.neo4j.gds.compat.TestLog.INFO;
import static org.neo4j.gds.paths.PathTestUtil.expected;

final class DeltaSteppingTest {

    // delta X concurrency X id-supplier
    public static Stream<Arguments> testParameters() {
        return TestSupport.crossArguments(
            () -> TestSupport.crossArguments(
                () -> DoubleStream.of(0.25, 0.5, 1, 2, 8).mapToObj(Arguments::of),
                () -> IntStream.of(1, 4).mapToObj(Arguments::of)
            ),
            () -> Stream.of(0L, 42L).map(Arguments::of)
        );
    }

    @ParameterizedTest
    @CsvSource({"10_000,100_000,260216,1280216", "100_000,1_000_000,2600216,12800216"})
    void memoryEstimation(long nodeCount, long relationshipCount, long expectedMin, long expectedMax) {
        var dimensions = GraphDimensions.builder().nodeCount(nodeCount).relCountUpperBound(relationshipCount).build();

        var estimation = DeltaStepping.memoryEstimation(true);

        var actual = estimation.estimate(dimensions, 4).memoryUsage();

        assertThat(actual.min).isEqualTo(expectedMin);
        assertThat(actual.max).isEqualTo(expectedMax);
    }

    @GdlExtension
    @Nested
    @TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
    class Graph1 {

        // https://en.wikipedia.org/wiki/Shortest_path_problem#/media/File:Shortest_path_with_direct_weights.svg
        @GdlGraph
        private static final String DB_CYPHER =
            "CREATE" +
                "  (a:A)" +
                ", (b:B)" +
                ", (c:C)" +
                ", (d:D)" +
                ", (e:E)" +
                ", (f:F)" +

                ", (a)-[:TYPE {cost: 4}]->(b)" +
                ", (a)-[:TYPE {cost: 2}]->(c)" +
                ", (b)-[:TYPE {cost: 5}]->(c)" +
                ", (b)-[:TYPE {cost: 10}]->(d)" +
                ", (c)-[:TYPE {cost: 3}]->(e)" +
                ", (d)-[:TYPE {cost: 11}]->(f)" +
                ", (e)-[:TYPE {cost: 4}]->(d)";

        @Inject
        Graph graph;

        @Inject
        IdFunction idFunction;

        @ParameterizedTest
        @MethodSource("org.neo4j.gds.paths.delta.DeltaSteppingTest#testParameters")
        void singleSource(double delta, int concurrency, long idOffset) {
            var graph = TestSupport.fromGdl(DB_CYPHER, idOffset);

            IdFunction idFunction = (String variable) -> graph.toMappedNodeId(graph.toOriginalNodeId(variable));

            var expected = Set.of(
                expected(idFunction, 0, new double[]{0.0}, "a"),
                expected(idFunction, 1, new double[]{0.0, 4.0}, "a", "b"),
                expected(idFunction, 2, new double[]{0.0, 2.0}, "a", "c"),
                expected(idFunction, 3, new double[]{0.0, 2.0, 5.0}, "a", "c", "e"),
                expected(idFunction, 4, new double[]{0.0, 2.0, 5.0, 9.0}, "a", "c", "e", "d"),
                expected(idFunction, 5, new double[]{0.0, 2.0, 5.0, 9.0, 20.0}, "a", "c", "e", "d", "f")
            );

            var sourceNode = graph.toOriginalNodeId("a");

            var config = ImmutableAllShortestPathsDeltaStreamConfig.builder()
                .concurrency(concurrency)
                .sourceNode(sourceNode)
                .delta(delta)
                .build();

            var paths = DeltaStepping
                .of(graph, config, Pools.DEFAULT, ProgressTracker.NULL_TRACKER)
                .compute()
                .pathSet();

            assertThat(paths)
                .usingRecursiveComparison()
                .withIntrospectionStrategy(ComparingFields.COMPARING_FIELDS)
                .ignoringFields("index")
                .isEqualTo(expected);
        }

        @ParameterizedTest
        @MethodSource("org.neo4j.gds.paths.delta.DeltaSteppingTest#testParameters")
        void singleSourceFromDisconnectedNode(double delta, int concurrency, long idOffset) {
            var graph = TestSupport.fromGdl(DB_CYPHER, idOffset);

            IdFunction idFunction = (String variable) -> graph.toMappedNodeId(graph.toOriginalNodeId(variable));

            var expected = Set.of(
                expected(idFunction, 0, new double[]{0.0}, "c"),
                expected(idFunction, 1, new double[]{0.0, 3.0}, "c", "e"),
                expected(idFunction, 2, new double[]{0.0, 3.0, 7.0}, "c", "e", "d"),
                expected(idFunction, 3, new double[]{0.0, 3.0, 7.0, 18.0}, "c", "e", "d", "f")
            );

            var sourceNode = graph.toOriginalNodeId("c");

            var config = ImmutableAllShortestPathsDeltaStreamConfig.builder()
                .concurrency(concurrency)
                .sourceNode(sourceNode)
                .delta(delta)
                .build();

            var paths = DeltaStepping
                .of(graph, config, Pools.DEFAULT, ProgressTracker.NULL_TRACKER)
                .compute()
                .pathSet();

            assertThat(paths)
                .usingRecursiveComparison()
                .withIntrospectionStrategy(ComparingFields.COMPARING_FIELDS)
                .ignoringFields("index")
                .isEqualTo(expected);
        }

        @Test
        void shouldLogProgress() {
            var config = ImmutableAllShortestPathsDeltaStreamConfig.builder()
                .concurrency(4)
                .sourceNode(idFunction.of("c"))
                .delta(5)
                .build();

            var progressTask = new DeltaSteppingFactory().progressTask(graph, config);
            var testLog = Neo4jProxy.testLog();
            var progressTracker = new TestProgressTracker(progressTask, testLog, 1, EmptyTaskRegistryFactory.INSTANCE);

            DeltaStepping.of(graph, config, Pools.DEFAULT, progressTracker)
                .compute()
                .pathSet();

            List<AtomicLong> progresses = progressTracker.getProgresses();
            assertEquals(7, progresses.size());

            var messagesInOrder = testLog.getMessages(INFO);

            assertThat(messagesInOrder)
                // avoid asserting on the thread id
                .extracting(removingThreadId())
                .hasSize(20)
                .containsSequence(
                    "DeltaStepping :: Start",
                    "DeltaStepping :: RELAX 1 :: Start",
                    "DeltaStepping :: RELAX 1 100%",
                    "DeltaStepping :: RELAX 1 :: Finished",
                    "DeltaStepping :: SYNC 1 :: Start",
                    "DeltaStepping :: SYNC 1 100%",
                    "DeltaStepping :: SYNC 1 :: Finished"
                )
                .containsSequence(
                    "DeltaStepping :: RELAX 3 :: Start",
                    "DeltaStepping :: RELAX 3 100%",
                    "DeltaStepping :: RELAX 3 :: Finished",
                    "DeltaStepping :: SYNC 3 :: Start",
                    "DeltaStepping :: SYNC 3 100%",
                    "DeltaStepping :: SYNC 3 :: Finished",
                    "DeltaStepping :: Finished"
                );
        }

    }

    @Nested
    class Graph2 {

        // https://www.cise.ufl.edu/~sahni/cop3530/slides/lec326.pdf without relationship id 14
        private static final String DB_CYPHER2 =
            "CREATE" +
                "  (n1:Label)" +
                ", (n2:Label)" +
                ", (n3:Label)" +
                ", (n4:Label)" +
                ", (n5:Label)" +
                ", (n6:Label)" +
                ", (n7:Label)" +

                ", (n1)-[:TYPE {cost: 6}]->(n2)" +
                ", (n1)-[:TYPE {cost: 2}]->(n3)" +
                ", (n1)-[:TYPE {cost: 16}]->(n4)" +
                ", (n2)-[:TYPE {cost: 4}]->(n5)" +
                ", (n2)-[:TYPE {cost: 5}]->(n4)" +
                ", (n3)-[:TYPE {cost: 7}]->(n2)" +
                ", (n3)-[:TYPE {cost: 3}]->(n5)" +
                ", (n3)-[:TYPE {cost: 8}]->(n6)" +
                ", (n4)-[:TYPE {cost: 7}]->(n3)" +
                ", (n5)-[:TYPE {cost: 4}]->(n4)" +
                ", (n5)-[:TYPE {cost: 10}]->(n7)" +
                ", (n6)-[:TYPE {cost: 1}]->(n7)";

        @ParameterizedTest
        @MethodSource("org.neo4j.gds.paths.delta.DeltaSteppingTest#testParameters")
        void singleSource(double delta, int concurrency, long idOffset) {
            var graph = TestSupport.fromGdl(DB_CYPHER2, idOffset);

            IdFunction idFunction = (String variable) -> graph.toMappedNodeId(graph.toOriginalNodeId(variable));

            var expected = Set.of(
                expected(idFunction, 0, new double[]{0.0}, "n1"),
                expected(idFunction, 1, new double[]{0.0, 2.0}, "n1", "n3"),
                expected(idFunction, 2, new double[]{0.0, 2.0, 5.0}, "n1", "n3", "n5"),
                expected(idFunction, 3, new double[]{0.0, 6.0}, "n1", "n2"),
                expected(idFunction, 4, new double[]{0.0, 2.0, 5.0, 9.0}, "n1", "n3", "n5", "n4"),
                expected(idFunction, 5, new double[]{0.0, 2.0, 10.0}, "n1", "n3", "n6"),
                expected(idFunction, 6, new double[]{0.0, 2.0, 10.0, 11.0}, "n1", "n3", "n6", "n7")
            );

            var sourceNode = graph.toOriginalNodeId("n1");

            var config = ImmutableAllShortestPathsDeltaStreamConfig.builder()
                .concurrency(concurrency)
                .sourceNode(sourceNode)
                .delta(delta)
                .build();

            var paths = DeltaStepping
                .of(graph, config, Pools.DEFAULT, ProgressTracker.NULL_TRACKER)
                .compute()
                .pathSet();

            assertThat(paths)
                .usingRecursiveComparison()
                .withIntrospectionStrategy(ComparingFields.COMPARING_FIELDS)
                .ignoringFields("index")
                .isEqualTo(expected);
        }
    }

    @GdlExtension
    @Nested
    @TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
    class Graph3 {
        @GdlGraph
        private static final String DB_CYPHER =
            "CREATE" +
                "  (a:A)" +
                ", (b:B)" +
                ", (c:C)" +
                ", (d:D)" +
                ", (e:E)" +
                ", (f:F)" +

                ", (a)-[:TYPE]->(b)" +
                ", (a)-[:TYPE]->(c)" +
                ", (b)-[:TYPE]->(c)" +
                ", (b)-[:TYPE]->(d)" +
                ", (c)-[:TYPE]->(e)" +
                ", (d)-[:TYPE]->(f)" +
                ", (e)-[:TYPE]->(d)";

        @Inject
        TestGraph graph;

        @ParameterizedTest
        @MethodSource("org.neo4j.gds.paths.delta.DeltaSteppingTest#testParameters")
        void singleSource(double delta, int concurrency, long idOffset) {
            IdFunction idFunction = graph::toMappedNodeId;

            var expected = Set.of(
                expected(idFunction, 0, new double[]{0.0}, "a"),
                expected(idFunction, 1, new double[]{0.0, 1.0}, "a", "b"),
                expected(idFunction, 2, new double[]{0.0, 1.0}, "a", "c"),
                expected(idFunction, 2, new double[]{0.0, 1.0, 2.0}, "a", "b", "d"),
                expected(idFunction, 3, new double[]{0.0, 1.0, 2.0}, "a", "c", "e"),
                expected(idFunction, 5, new double[]{0.0, 1.0, 2.0, 3.0}, "a", "b", "d", "f")
            );

            var sourceNode = graph.toOriginalNodeId("a");

            var config = ImmutableAllShortestPathsDeltaStreamConfig.builder()
                .concurrency(concurrency)
                .sourceNode(sourceNode)
                .delta(delta)
                .build();

            var paths = DeltaStepping
                .of(graph, config, Pools.DEFAULT, ProgressTracker.NULL_TRACKER)
                .compute()
                .pathSet();

            assertThat(paths)
                .usingRecursiveComparison()
                .withIntrospectionStrategy(ComparingFields.COMPARING_FIELDS)
                .ignoringFields("index")
                .isEqualTo(expected);
        }
    }

    private DeltaSteppingTest() {}

    @Test
    void shouldGiveSameResultsAsDijkstra() {
        int nodeCount = 3_000;
        long seed = 42L;
        long start = 42;
        int concurrency = 4;
        var newGraph = new RandomGraphGeneratorBuilder()
            .direction(Direction.DIRECTED)
            .averageDegree(10)
            .relationshipDistribution(RelationshipDistribution.POWER_LAW)
            .relationshipPropertyProducer(PropertyProducer.randomDouble("foo", 1, 10))
            .nodeCount(nodeCount)
            .seed(seed)
            .build()
            .generate();

        var config = ImmutableAllShortestPathsDeltaStreamConfig.builder()
            .concurrency(concurrency)
            .sourceNode(start)
            .trackRelationships(true)
            .build();
        var deltaStepping = DeltaStepping.of(
            newGraph,
            config,
            Pools.DEFAULT,
            ProgressTracker.NULL_TRACKER
        ).compute();

        var dijkstraAlgo = Dijkstra
            .singleSource(newGraph, config, Optional.empty(), ProgressTracker.NULL_TRACKER)
            .compute();

        double[] delta = new double[nodeCount];
        double[] djikstra = new double[nodeCount];

        double deltaSum = 0;
        double dijkstraSum = 0;

        for (var path : deltaStepping.pathSet()) {
            delta[(int) path.targetNode()] = path.totalCost();
        }

        for (var path : dijkstraAlgo.pathSet()) {
            djikstra[(int) path.targetNode()] = path.totalCost();
        }
        for (int i = 0; i < nodeCount; ++i) {
            deltaSum += delta[i];
            dijkstraSum += djikstra[i];
            assertThat(djikstra[i]).isCloseTo(delta[i], Offset.offset(1e-5));

        }
        assertThat(deltaSum).isCloseTo(dijkstraSum, Offset.offset(1e-5));

    }

}
