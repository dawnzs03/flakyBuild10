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
package org.neo4j.gds.paths.yens;

import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.gds.TestProgressTracker;
import org.neo4j.gds.TestSupport;
import org.neo4j.gds.compat.Neo4jProxy;
import org.neo4j.gds.compat.TestLog;
import org.neo4j.gds.core.Aggregation;
import org.neo4j.gds.core.utils.mem.MemoryRange;
import org.neo4j.gds.core.utils.progress.EmptyTaskRegistryFactory;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.extension.GdlExtension;
import org.neo4j.gds.extension.GdlGraph;
import org.neo4j.gds.extension.IdFunction;
import org.neo4j.gds.extension.Inject;
import org.neo4j.gds.extension.TestGraph;
import org.neo4j.gds.paths.ImmutablePathResult;
import org.neo4j.gds.paths.PathResult;
import org.neo4j.gds.paths.yens.config.ImmutableShortestPathYensStreamConfig;
import org.s1ck.gdl.GDLHandler;
import org.s1ck.gdl.model.Edge;
import org.s1ck.gdl.model.Vertex;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.neo4j.gds.assertj.Extractors.removingThreadId;
import static org.neo4j.gds.assertj.Extractors.replaceTimings;

@GdlExtension
class YensTest {

    static ImmutableShortestPathYensStreamConfig.Builder defaultSourceTargetConfigBuilder(int concurrency) {
        return ImmutableShortestPathYensStreamConfig.builder()
            .concurrency(concurrency);
    }

    static Stream<Arguments> expectedMemoryEstimation() {
        return Stream.of(
            Arguments.of(1_000, 3, 1, 56_960L),
            Arguments.of(1_000, 3, 4, 227_744L),

            Arguments.of(1_000_000, 3, 1, 56_125_832L),
            Arguments.of(1_000_000, 3, 4, 224_503_232L),

            Arguments.of(1_000_000_000, 3, 1, 56_133_545_928L),
            Arguments.of(1_000_000_000, 3, 4, 224_534_183_616L)

        );
    }

    @ParameterizedTest
    @MethodSource("expectedMemoryEstimation")
    void shouldComputeMemoryEstimation(
        int nodeCount,
        int k,
        int concurrency,
        long expectedBytes
    ) {
        TestSupport.assertMemoryEstimation(
            () -> Yens.memoryEstimation(k, true),
            nodeCount,
            concurrency,
            MemoryRange.of(expectedBytes)
        );
    }

    // https://en.wikipedia.org/wiki/Yen%27s_algorithm#/media/File:Yen's_K-Shortest_Path_Algorithm,_K=3,_A_to_F.gif
    @GdlGraph(aggregation = Aggregation.SINGLE)
    private static final String DB_CYPHER =
        "CREATE" +
        "  (c:C {id: 0})" +
        ", (d:D {id: 1})" +
        ", (e:E {id: 2})" +
        ", (f:F {id: 3})" +
        ", (g:G {id: 4})" +
        ", (h:H {id: 5})" +
        ", (z:Z {id: 6})" +
        ", (c)-[:REL {cost: 3.0}]->(d)" +
        ", (c)-[:REL {cost: 2.0}]->(e)" +
        ", (d)-[:REL {cost: 4.0}]->(f)" +
        ", (e)-[:REL {cost: 1.0}]->(d)" +
        ", (e)-[:REL {cost: 2.0}]->(f)" +
        ", (e)-[:REL {cost: 3.0}]->(g)" +
        ", (f)-[:REL {cost: 2.0}]->(g)" +
        ", (f)-[:REL {cost: 1.0}]->(h)" +
        ", (g)-[:REL {cost: 2.0}]->(h)";

    @Inject
    private TestGraph graph;

    // Each input represents k paths that are expected to be returned by Yen's algorithm.
    // The first node in each path is the start node for the path search, the last node in
    // each path is the target node for each path search. The node property represents the
    // expected cost in the resulting path, the relationship property is the index of the
    // relationship that has been traversed.
    static Stream<List<String>> pathInput() {
        return Stream.of(
            List.of(
                "(c {cost: 0.0})-[{id: 1}]->(e {cost: 2.0})-[{id: 1}]->(f {cost: 4.0})-[{id: 1}]->(h {cost: 5.0})"
            ),
            List.of(
                "(c {cost: 0.0})-[{id: 1}]->(e {cost: 2.0})-[{id: 1}]->(f {cost: 4.0})-[{id: 1}]->(h {cost: 5.0})",
                "(c {cost: 0.0})-[{id: 1}]->(e {cost: 2.0})-[{id: 2}]->(g {cost: 5.0})-[{id: 0}]->(h {cost: 7.0})"
            ),
            List.of(
                "(c {cost: 0.0})-[{id: 1}]->(e {cost: 2.0})-[{id: 1}]->(f {cost: 4.0})-[{id: 1}]->(h {cost: 5.0})",
                "(c {cost: 0.0})-[{id: 1}]->(e {cost: 2.0})-[{id: 2}]->(g {cost: 5.0})-[{id: 0}]->(h {cost: 7.0})",
                "(c {cost: 0.0})-[{id: 0}]->(d {cost: 3.0})-[{id: 0}]->(f {cost: 7.0})-[{id: 1}]->(h {cost: 8.0})"
            ),
            List.of(
                "(c {cost: 0.0})-[{id: 1}]->(e {cost: 2.0})-[{id: 1}]->(f {cost: 4.0})-[{id: 1}]->(h {cost: 5.0})",
                "(c {cost: 0.0})-[{id: 1}]->(e {cost: 2.0})-[{id: 2}]->(g {cost: 5.0})-[{id: 0}]->(h {cost: 7.0})",
                "(c {cost: 0.0})-[{id: 0}]->(d {cost: 3.0})-[{id: 0}]->(f {cost: 7.0})-[{id: 1}]->(h {cost: 8.0})",
                "(c {cost: 0.0})-[{id: 1}]->(e {cost: 2.0})-[{id: 0}]->(d {cost: 3.0})-[{id: 0}]->(f {cost: 7.0})-[{id: 1}]->(h {cost: 8.0})"
            ),
            List.of(
                "(c {cost: 0.0})-[{id: 1}]->(e {cost: 2.0})-[{id: 1}]->(f {cost: 4.0})-[{id: 1}]->(h {cost: 5.0})",
                "(c {cost: 0.0})-[{id: 1}]->(e {cost: 2.0})-[{id: 2}]->(g {cost: 5.0})-[{id: 0}]->(h {cost: 7.0})",
                "(c {cost: 0.0})-[{id: 0}]->(d {cost: 3.0})-[{id: 0}]->(f {cost: 7.0})-[{id: 1}]->(h {cost: 8.0})",
                "(c {cost: 0.0})-[{id: 1}]->(e {cost: 2.0})-[{id: 0}]->(d {cost: 3.0})-[{id: 0}]->(f {cost: 7.0})-[{id: 1}]->(h {cost: 8.0})",
                "(c {cost: 0.0})-[{id: 1}]->(e {cost: 2.0})-[{id: 1}]->(f {cost: 4.0})-[{id: 0}]->(g {cost: 6.0})-[{id: 0}]->(h {cost: 8.0})"
            ),
            List.of(
                "(c {cost: 0.0})-[{id: 1}]->(e {cost: 2.0})-[{id: 1}]->(f {cost: 4.0})-[{id: 1}]->(h {cost: 5.0})",
                "(c {cost: 0.0})-[{id: 1}]->(e {cost: 2.0})-[{id: 2}]->(g {cost: 5.0})-[{id: 0}]->(h {cost: 7.0})",
                "(c {cost: 0.0})-[{id: 0}]->(d {cost: 3.0})-[{id: 0}]->(f {cost: 7.0})-[{id: 1}]->(h {cost: 8.0})",
                "(c {cost: 0.0})-[{id: 1}]->(e {cost: 2.0})-[{id: 0}]->(d {cost: 3.0})-[{id: 0}]->(f {cost: 7.0})-[{id: 1}]->(h {cost: 8.0})",
                "(c {cost: 0.0})-[{id: 1}]->(e {cost: 2.0})-[{id: 1}]->(f {cost: 4.0})-[{id: 0}]->(g {cost: 6.0})-[{id: 0}]->(h {cost: 8.0})",
                "(c {cost: 0.0})-[{id: 0}]->(d {cost: 3.0})-[{id: 0}]->(f {cost: 7.0})-[{id: 0}]->(g {cost: 9.0})-[{id: 0}]->(h {cost: 11.0})"
            ),
            List.of(
                "(c {cost: 0.0})-[{id: 1}]->(e {cost: 2.0})-[{id: 1}]->(f {cost: 4.0})-[{id: 1}]->(h {cost: 5.0})",
                "(c {cost: 0.0})-[{id: 1}]->(e {cost: 2.0})-[{id: 2}]->(g {cost: 5.0})-[{id: 0}]->(h {cost: 7.0})",
                "(c {cost: 0.0})-[{id: 0}]->(d {cost: 3.0})-[{id: 0}]->(f {cost: 7.0})-[{id: 1}]->(h {cost: 8.0})",
                "(c {cost: 0.0})-[{id: 1}]->(e {cost: 2.0})-[{id: 0}]->(d {cost: 3.0})-[{id: 0}]->(f {cost: 7.0})-[{id: 1}]->(h {cost: 8.0})",
                "(c {cost: 0.0})-[{id: 1}]->(e {cost: 2.0})-[{id: 1}]->(f {cost: 4.0})-[{id: 0}]->(g {cost: 6.0})-[{id: 0}]->(h {cost: 8.0})",
                "(c {cost: 0.0})-[{id: 0}]->(d {cost: 3.0})-[{id: 0}]->(f {cost: 7.0})-[{id: 0}]->(g {cost: 9.0})-[{id: 0}]->(h {cost: 11.0})",
                "(c {cost: 0.0})-[{id: 1}]->(e {cost: 2.0})-[{id: 0}]->(d {cost: 3.0})-[{id: 0}]->(f {cost: 7.0})-[{id: 0}]->(g {cost: 9.0})-[{id: 0}]->(h {cost: 11.0})"
            )
        );
    }

    @ParameterizedTest
    @MethodSource("pathInput")
    void compute(Collection<String> expectedPaths) {
        assertResult(graph, expectedPaths, false, 4);
    }

    @Test
    void shouldLogProgress() {
        int k = 3;

        var config = defaultSourceTargetConfigBuilder(1)
            .sourceNode(graph.toOriginalNodeId("c"))
            .targetNode(graph.toOriginalNodeId("h"))
            .k(k)
            .build();

        var progressTask = new YensFactory<>().progressTask(graph, config);
        var log = Neo4jProxy.testLog();
        var progressTracker = new TestProgressTracker(progressTask, log, 1, EmptyTaskRegistryFactory.INSTANCE);

        Yens.sourceTarget(graph, config, progressTracker)
            .compute()
            .pathSet();

        assertThat(log.getMessages(TestLog.INFO))
            .extracting(removingThreadId())
            .extracting(replaceTimings())
            .containsExactly(
                "Yens :: Start",
                "Yens :: Dijkstra :: Start",
                "Yens :: Dijkstra 22%",
                "Yens :: Dijkstra 55%",
                "Yens :: Dijkstra 66%",
                "Yens :: Dijkstra 88%",
                "Yens :: Dijkstra 100%",
                "Yens :: Dijkstra :: Finished",
                "Yens :: Path growing :: Start",
                "Yens :: Path growing 50%",
                "Yens :: Path growing 100%",
                "Yens :: Path growing :: Finished",
                "Yens :: Finished"
            );
    }

    @Test
    void shouldLogProgressIfNothingToDo() {
        int k = 3;

        var config = defaultSourceTargetConfigBuilder(1)
            .sourceNode(graph.toOriginalNodeId("z"))
            .targetNode(graph.toOriginalNodeId("h"))
            .k(k)
            .build();

        var progressTask = new YensFactory<>().progressTask(graph, config);
        var log = Neo4jProxy.testLog();
        var progressTracker = new TestProgressTracker(progressTask, log, 1, EmptyTaskRegistryFactory.INSTANCE);

        Yens.sourceTarget(graph, config, progressTracker)
            .compute()
            .pathSet();

        assertThat(log.getMessages(TestLog.INFO))
            .extracting(removingThreadId())
            .extracting(replaceTimings())
            .containsExactly(
                "Yens :: Start",
                "Yens :: Dijkstra :: Start",
                "Yens :: Dijkstra 100%",
                "Yens :: Dijkstra :: Finished",
                "Yens :: Finished"
            );
    }


    private static void assertResult(
        TestGraph graph,
        Collection<String> expectedPaths,
        boolean trackRelationships,
        int concurrency
    ) {
        var expectedPathResults = expectedPathResults(graph::toMappedNodeId, expectedPaths, trackRelationships);

        var firstResult = expectedPathResults
            .stream()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("At least one expected path must be provided"));

        if (!expectedPathResults
            .stream()
            .allMatch(p -> p.sourceNode() == firstResult.sourceNode() && p.targetNode() == firstResult.targetNode())) {
            throw new IllegalArgumentException("All expected paths must have the same source and target nodes.");
        }

        var config = defaultSourceTargetConfigBuilder(concurrency)
            .sourceNode(graph.toOriginalNodeId(firstResult.sourceNode()))
            .targetNode(graph.toOriginalNodeId(firstResult.targetNode()))
            .k(expectedPathResults.size())
            .build();

        var actualPathResults = Yens
            .sourceTarget(graph, config, ProgressTracker.NULL_TRACKER)
            .compute()
            .pathSet();

        assertThat(actualPathResults).containsExactlyInAnyOrderElementsOf(expectedPathResults);
    }

    @NotNull
    private static Set<PathResult> expectedPathResults(
        IdFunction idFunction,
        Collection<String> expectedPaths,
        boolean trackRelationships
    ) {
        var index = new MutableInt(0);
        return expectedPaths.stream()
            .map(expectedPath -> new GDLHandler.Builder()
                .setNextVertexId(variable -> variable
                    .map(idFunction::of)
                    .orElseThrow(() -> new IllegalArgumentException("Path must not contain anonymous nodes.")))
                .buildFromString(expectedPath)
            )
            .map(gdl -> {
                var sourceNode = gdl.getVertices().stream()
                    .filter(v -> gdl.getEdges().stream().allMatch(e -> e.getTargetVertexId() != v.getId()))
                    .findFirst()
                    .orElseThrow();

                var targetNode = gdl.getVertices().stream()
                    .filter(v -> gdl.getEdges().stream().allMatch(e -> e.getSourceVertexId() != v.getId()))
                    .findFirst()
                    .orElseThrow();

                int nodeCount = gdl.getVertices().size();

                var nodeIds = new long[nodeCount];
                var relationshipIds = new long[nodeCount - 1];
                var costs = new double[nodeCount];

                var nextNode = sourceNode;
                var j = 0;
                while (nextNode != targetNode) {
                    var edge = getEdgeBySourceId(gdl.getEdges(), nextNode.getId());
                    nodeIds[j] = nextNode.getId();
                    relationshipIds[j] = (int) edge.getProperties().get("id");
                    costs[j] = (float) nextNode.getProperties().get("cost");
                    nextNode = getVertexById(gdl.getVertices(), edge.getTargetVertexId());
                    j += 1;
                }

                nodeIds[j] = nextNode.getId();
                costs[j] = (float) nextNode.getProperties().get("cost");

                return ImmutablePathResult.builder()
                    .index(index.getAndIncrement())
                    .sourceNode(sourceNode.getId())
                    .targetNode(targetNode.getId())
                    .nodeIds(nodeIds)
                    .relationshipIds(trackRelationships ? relationshipIds : new long[0])
                    .costs(costs)
                    .build();
            })
            .collect(Collectors.toSet());
    }

    private static Vertex getVertexById(Collection<Vertex> vertices, long id) {
        return vertices.stream().filter(v -> v.getId() == id).findFirst().orElseThrow();
    }

    private static Edge getEdgeBySourceId(Collection<Edge> elements, long id) {
        return elements.stream().filter(e -> e.getSourceVertexId() == id).findFirst().orElseThrow();
    }

    @Nested
    @TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
    class MultiGraph {

        @GdlGraph
        private static final String DB_CYPHER =
            "CREATE" +
            "  (a { id: 0 })" +
            ", (b { id: 1 })" +
            ", (c { id: 2 })" +
            ", (d { id: 3 })" +
            ", (a)-[:REL { cost: 1.0 }]->(b)" +
            ", (a)-[:REL { cost: 2.0 }]->(b)" +
            ", (b)-[:REL { cost: 3.0 }]->(c)" +
            ", (b)-[:REL { cost: 4.0 }]->(c)" +
            ", (c)-[:REL { cost: 42.0 }]->(d)" +
            ", (c)-[:REL { cost: 42.0 }]->(d)";

        @Inject
        private TestGraph graph;

        Stream<List<String>> pathInput() {
            return Stream.of(
                List.of(
                    "(a {cost: 0.0})-[{id: 0}]->(b {cost: 1.0})"
                ),
                List.of(
                    "(a {cost: 0.0})-[{id: 0}]->(b {cost: 1.0})",
                    "(a {cost: 0.0})-[{id: 1}]->(b {cost: 2.0})"
                ),
                List.of(
                    "(a {cost: 0.0})-[{id: 0}]->(b {cost: 1.0})-[{id: 0}]->(c {cost: 4.0})",
                    "(a {cost: 0.0})-[{id: 1}]->(b {cost: 2.0})-[{id: 0}]->(c {cost: 5.0})",
                    "(a {cost: 0.0})-[{id: 0}]->(b {cost: 1.0})-[{id: 1}]->(c {cost: 5.0})"
                ),
                List.of(
                    "(a {cost: 0.0})-[{id: 0}]->(b {cost: 1.0})-[{id: 0}]->(c {cost: 4.0})-[{id: 0}]->(d {cost: 46.0})",
                    "(a {cost: 0.0})-[{id: 0}]->(b {cost: 1.0})-[{id: 0}]->(c {cost: 4.0})-[{id: 1}]->(d {cost: 46.0})"
                )
            );
        }

        @ParameterizedTest
        @MethodSource("pathInput")
        void compute(Collection<String> expectedPaths) {
            assertResult(graph, expectedPaths, true, 4);
        }
    }
}
