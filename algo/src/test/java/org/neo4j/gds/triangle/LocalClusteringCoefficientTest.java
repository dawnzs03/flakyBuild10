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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.gds.Orientation;
import org.neo4j.gds.TestSupport;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.compat.Neo4jProxy;
import org.neo4j.gds.compat.TestLog;
import org.neo4j.gds.core.utils.progress.EmptyTaskRegistryFactory;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.core.utils.progress.tasks.TaskProgressTracker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.neo4j.gds.Orientation.UNDIRECTED;
import static org.neo4j.gds.utils.StringFormatting.formatWithLocale;

// TODO: Add tests for seeded property
class LocalClusteringCoefficientTest {

    private static Stream<Arguments> noTriangleQueries() {
        return Stream.of(
            Arguments.of(fromGdl("CREATE ()-[:T]->()-[:T]->()", UNDIRECTED), "line"),
            Arguments.of(fromGdl("CREATE (), (), ()", UNDIRECTED), "no rels"),
            Arguments.of(fromGdl("CREATE ()-[:T]->(), ()", UNDIRECTED), "one rel"),
            Arguments.of(fromGdl("CREATE (a1)-[:T]->()-[:T]->(a1), ()", UNDIRECTED), "back and forth")
        );
    }

    @MethodSource("noTriangleQueries")
    @ParameterizedTest(name = "{1}")
    void noTriangles(Graph graph, String ignoredName) {
        LocalClusteringCoefficient.Result result = compute(graph);

        assertEquals(0, result.averageClusteringCoefficient());
        assertEquals(3, result.localClusteringCoefficients().size());
        assertEquals(0.0, result.localClusteringCoefficients().get(0));
        assertEquals(0.0, result.localClusteringCoefficients().get(1));
        assertEquals(0.0, result.localClusteringCoefficients().get(2));
    }

    @ValueSource(ints = {1, 2, 4, 8, 100})
    @ParameterizedTest
    void independentTriangles(int nbrOfTriangles) {
        StringBuilder gdl = new StringBuilder("CREATE ");
        for (int i = 0; i < nbrOfTriangles; ++i) {
            gdl.append(formatWithLocale("(a%d)-[:T]->()-[:T]->()-[:T]->(a%d) ", i, i));
        }

        LocalClusteringCoefficient.Result result = compute(fromGdl(gdl.toString(), UNDIRECTED));

        assertEquals(1, result.averageClusteringCoefficient());
        assertEquals(3 * nbrOfTriangles, result.localClusteringCoefficients().size());
        for (int i = 0; i < result.localClusteringCoefficients().size(); ++i) {
            assertEquals(1.0, result.localClusteringCoefficients().get(i));
        }
    }

    @Test
    void clique5() {
        var graph = fromGdl(
            "CREATE " +
            " (a1)-[:T]->(a2), " +
            " (a1)-[:T]->(a3), " +
            " (a1)-[:T]->(a4), " +
            " (a1)-[:T]->(a5), " +
            " (a2)-[:T]->(a3), " +
            " (a2)-[:T]->(a4), " +
            " (a2)-[:T]->(a5), " +
            " (a3)-[:T]->(a4), " +
            " (a3)-[:T]->(a5), " +
            " (a4)-[:T]->(a5)",
            UNDIRECTED
        );

        LocalClusteringCoefficient.Result result = compute(graph);

        assertEquals(1, result.averageClusteringCoefficient());
        assertEquals(5, result.localClusteringCoefficients().size());
        for (int i = 0; i < result.localClusteringCoefficients().size(); ++i) {
            assertEquals(1.0, result.localClusteringCoefficients().get(i));
        }
    }

    @Test
    void twoAdjacentTriangles() {
        var graph = fromGdl(
            "CREATE " +
            "  (a)-[:T]->()-[:T]->()-[:T]->(a) " +
            ", (a)-[:T]->()-[:T]->()-[:T]->(a)",
            UNDIRECTED
        );

        LocalClusteringCoefficient.Result result = compute(graph);

        assertEquals(13.0 / 15.0, result.averageClusteringCoefficient(), 1e-10);
        assertEquals(5, result.localClusteringCoefficients().size());


        Collection<Double> localCCs = new ArrayList<>();
        for (int i = 0; i < result.localClusteringCoefficients().size(); ++i) {
            localCCs.add(result.localClusteringCoefficients().get(i));
        }

        Map<Double, Long> groupedCcs = localCCs
            .stream()
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        assertEquals(4, groupedCcs.get(1.0));
        assertEquals(1, groupedCcs.get(1.0 / 3));
    }

    @Test
    void twoTrianglesWithLine() {
        var graph = fromGdl(
            "CREATE " +
            "  (a)-[:T]->(b)-[:T]->(c)-[:T]->(a) " +
            ", (q)-[:T]->(r)-[:T]->(t)-[:T]->(q) " +
            ", (a)-[:T]->(q)",
            UNDIRECTED
        );

        LocalClusteringCoefficient.Result result = compute(graph);

        assertEquals(7.0 / 9.0, result.averageClusteringCoefficient(), 1e-10);
        assertEquals(6, result.localClusteringCoefficients().size());

        Collection<Double> localCCs = new ArrayList<>();
        for (int i = 0; i < result.localClusteringCoefficients().size(); ++i) {
            localCCs.add(result.localClusteringCoefficients().get(i));
        }

        Map<Double, Long> groupedCcs = localCCs
            .stream()
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        assertEquals(4, groupedCcs.get(1.0));
        assertEquals(2, groupedCcs.get(1.0 / 3));
    }

    @Test
    void selfLoop() {
        var graph = fromGdl("CREATE (a)-[:T]->(a)-[:T]->(a)-[:T]->(a)", UNDIRECTED);

        LocalClusteringCoefficient.Result result = compute(graph);

        assertEquals(0, result.averageClusteringCoefficient());
        assertEquals(1, result.localClusteringCoefficients().size());
        assertEquals(0.0, result.localClusteringCoefficients().get(0));
    }

    @Test
    void triangleWithSelfLoop() {
        // a self loop adds one to the degree
        var graph = fromGdl("CREATE (a)-[:T]->(b)-[:T]->(c)-[:T]->(a)-[:T]->(a)", UNDIRECTED);

        LocalClusteringCoefficient.Result result = compute(graph);

        assertEquals(7.0 / 9, result.averageClusteringCoefficient(), 1e-10);
        assertEquals(3, result.localClusteringCoefficients().size());
        assertEquals(1.0 / 3, result.localClusteringCoefficients().get(0));
        assertEquals(1.0, result.localClusteringCoefficients().get(1));
        assertEquals(1.0, result.localClusteringCoefficients().get(2));
    }

    @Test
    void triangleWithParallelRelationship() {
        var graph = fromGdl(
            "CREATE" +
            " (a)-[:T]->(b)-[:T]->(c)-[:T]->(a)" +
            ",(a)-[:T]->(b)",
            UNDIRECTED
        );

        LocalClusteringCoefficient.Result result = compute(graph);

        assertEquals(1.0, result.averageClusteringCoefficient());
        assertEquals(3, result.localClusteringCoefficients().size());
        assertEquals(1, result.localClusteringCoefficients().get(0));
        assertEquals(1, result.localClusteringCoefficients().get(1));
        assertEquals(1, result.localClusteringCoefficients().get(2));
    }

    @Test
    void triangleWithTwoParallelRelationships() {
        var graph = fromGdl(
            "CREATE" +
            " (a)-[:T]->(b)-[:T]->(c)-[:T]->(a)" +
            ",(a)-[:T]->(b)-[:T]->(c)",
            UNDIRECTED
        );

        LocalClusteringCoefficient.Result result = compute(graph);

        assertEquals(1.0, result.averageClusteringCoefficient());
        assertEquals(3, result.localClusteringCoefficients().size());
        assertEquals(1, result.localClusteringCoefficients().get(0));
        assertEquals(1, result.localClusteringCoefficients().get(1));
        assertEquals(1, result.localClusteringCoefficients().get(2));
    }

    @Test
    void parallelTriangles() {
        var graph = fromGdl(
            "CREATE" +
            " (a)-[:T]->(b)-[:T]->(c)-[:T]->(a)" +
            ",(a)-[:T]->(b)-[:T]->(c)-[:T]->(a)",
            UNDIRECTED
        );

        LocalClusteringCoefficient.Result result = compute(graph);

        assertEquals(1.0, result.averageClusteringCoefficient());
        assertEquals(3, result.localClusteringCoefficients().size());
        assertEquals(1, result.localClusteringCoefficients().get(0));
        assertEquals(1, result.localClusteringCoefficients().get(1));
        assertEquals(1, result.localClusteringCoefficients().get(2));
    }

    @Test
    void parallelTrianglesWithExtraParallelRelationship() {
        var graph = fromGdl(
            "CREATE" +
            " (a)-[:T]->(b)-[:T]->(c)-[:T]->(a)" +
            ",(a)-[:T]->(b)-[:T]->(c)-[:T]->(a)" +
            ",(a)-[:T]->(b)",
            UNDIRECTED
        );

        LocalClusteringCoefficient.Result result = compute(graph);

        assertEquals(1.0, result.averageClusteringCoefficient());
        assertEquals(3, result.localClusteringCoefficients().size());
        assertEquals(1, result.localClusteringCoefficients().get(0));
        assertEquals(1, result.localClusteringCoefficients().get(1));
        assertEquals(1, result.localClusteringCoefficients().get(2));
    }

    @Test
    void triangleWithParallelRelationshipAndExtraNode() {
        var graph = fromGdl(
            "CREATE" +
            " (a)-[:T]->(b)-[:T]->(c)-[:T]->(a)" +
            ",(a)-[:T]->(b)" +
            ",(d)-[:T]->(a)",
            UNDIRECTED
        );

        LocalClusteringCoefficient.Result result = compute(graph);

        assertEquals(4, result.localClusteringCoefficients().size());
        assertEquals(7.0 / 12, result.averageClusteringCoefficient(), 1e-10);
        assertEquals(1.0 / 3, result.localClusteringCoefficients().get(0), 1e-10); // a
        assertEquals(1, result.localClusteringCoefficients().get(1)); // b
        assertEquals(1, result.localClusteringCoefficients().get(2)); // c
        assertEquals(0, result.localClusteringCoefficients().get(3)); // d
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void progressLogging(boolean useSeed) {
        var graph = fromGdl(
            "CREATE" +
            " (a {triangles: 2})" +
            " (b {triangles: 2})" +
            " (c {triangles: 1})" +
            " (d {triangles: 1})" +
            " (a)-[:T]->(b)-[:T]->(c)-[:T]->(a)" +
            ",(a)-[:T]->(b)" +
            ",(d)-[:T]->(a)",
            UNDIRECTED
        );

        var factory = new LocalClusteringCoefficientFactory<>();
        var config = useSeed
            ? createConfig().seedProperty("triangles").build()
            : createConfig().build();

        var progressTask = factory.progressTask(graph, config);
        var log = Neo4jProxy.testLog();
        var progressTracker = new TaskProgressTracker(progressTask, log, 4, EmptyTaskRegistryFactory.INSTANCE);

        factory
            .build(graph, config, progressTracker)
            .compute();

        log.assertContainsMessage(TestLog.INFO, "LocalClusteringCoefficient :: Start");
        if (!useSeed) {
            log.assertContainsMessage(TestLog.INFO, "LocalClusteringCoefficient :: IntersectingTriangleCount :: Start");
            log.assertContainsMessage(TestLog.INFO, "LocalClusteringCoefficient :: IntersectingTriangleCount 25%");
            log.assertContainsMessage(TestLog.INFO, "LocalClusteringCoefficient :: IntersectingTriangleCount 50%");
            log.assertContainsMessage(TestLog.INFO, "LocalClusteringCoefficient :: IntersectingTriangleCount 75%");
            log.assertContainsMessage(TestLog.INFO, "LocalClusteringCoefficient :: IntersectingTriangleCount 100%");
            log.assertContainsMessage(TestLog.INFO, "LocalClusteringCoefficient :: IntersectingTriangleCount :: Finished");
        }
        log.assertContainsMessage(TestLog.INFO, "LocalClusteringCoefficient :: Calculate Local Clustering Coefficient :: Start");
        log.assertContainsMessage(TestLog.INFO, "LocalClusteringCoefficient :: Calculate Local Clustering Coefficient 25%");
        log.assertContainsMessage(TestLog.INFO, "LocalClusteringCoefficient :: Calculate Local Clustering Coefficient 50%");
        log.assertContainsMessage(TestLog.INFO, "LocalClusteringCoefficient :: Calculate Local Clustering Coefficient 75%");
        log.assertContainsMessage(TestLog.INFO, "LocalClusteringCoefficient :: Calculate Local Clustering Coefficient 100%");
        log.assertContainsMessage(TestLog.INFO, "LocalClusteringCoefficient :: Calculate Local Clustering Coefficient :: Finished");
        log.assertContainsMessage(TestLog.INFO, "LocalClusteringCoefficient :: Finished");
    }

    @Test
    void manyTrianglesAndOtherThings() {
        var graph = fromGdl(
            "CREATE" +
            " (a)-[:T]->(b)-[:T]->(b)-[:T]->(c)-[:T]->(a)" +
            ", (c)-[:T]->(d)-[:T]->(e)-[:T]->(f)-[:T]->(d)" +
            ", (f)-[:T]->(g)-[:T]->(h)-[:T]->(f)" +
            ", (h)-[:T]->(i)-[:T]->(j)-[:T]->(k)-[:T]->(e)" +
            ", (k)-[:T]->(l)" +
            ", (k)-[:T]->(m)-[:T]->(n)-[:T]->(j)" +
            ", (o)",
            UNDIRECTED
        );

        LocalClusteringCoefficient.Result result = compute(graph);

        assertEquals(4.0 / 15, result.averageClusteringCoefficient(), 1e-10);
        assertEquals(15, result.localClusteringCoefficients().size());
        assertEquals(1, result.localClusteringCoefficients().get(0)); // a
        assertEquals(1.0 / 3, result.localClusteringCoefficients().get(1)); // b
        assertEquals(1.0 / 3, result.localClusteringCoefficients().get(2)); // c
        assertEquals(1.0 / 3, result.localClusteringCoefficients().get(3)); // d
        assertEquals(1.0 / 3, result.localClusteringCoefficients().get(4)); // e
        assertEquals(1.0 / 3, result.localClusteringCoefficients().get(5)); // f
        assertEquals(1, result.localClusteringCoefficients().get(6)); // g
        assertEquals(1.0 / 3, result.localClusteringCoefficients().get(7)); // h
        assertEquals(0, result.localClusteringCoefficients().get(8)); // i
        assertEquals(0, result.localClusteringCoefficients().get(9)); // j
        assertEquals(0, result.localClusteringCoefficients().get(10)); // k
        assertEquals(0, result.localClusteringCoefficients().get(11)); // l
        assertEquals(0, result.localClusteringCoefficients().get(12)); // m
        assertEquals(0, result.localClusteringCoefficients().get(13)); // n
        assertEquals(0, result.localClusteringCoefficients().get(14)); // o
    }

    private LocalClusteringCoefficient.Result compute(Graph graph) {
        var localClusteringCoefficient = new LocalClusteringCoefficient(
            graph,
            createConfig().build(),
            ProgressTracker.NULL_TRACKER
        );
        return localClusteringCoefficient.compute();
    }

    private ImmutableLocalClusteringCoefficientBaseConfig.Builder createConfig() {
        return ImmutableLocalClusteringCoefficientBaseConfig.builder();
    }

    private static Graph fromGdl(String gdl, Orientation orientation) {
        return TestSupport.fromGdl(gdl, orientation).graph();
    }
}
