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
package org.neo4j.gds.approxmaxkcut;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.gds.BaseProcTest;
import org.neo4j.gds.GdsCypher;
import org.neo4j.gds.catalog.GraphProjectProc;
import org.neo4j.gds.catalog.GraphWriteNodePropertiesProc;
import org.neo4j.gds.core.loading.GraphStoreCatalog;
import org.neo4j.gds.extension.Neo4jGraph;
import org.neo4j.graphdb.QueryExecutionException;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ApproxMaxKCutStreamProcTest extends BaseProcTest {

    // The optimal max cut for this graph when k = 2 is:
    //     {a, b, c}, {d, e, f, g} if the graph is unweighted.
    //     {a, c}, {b, d, e, f, g} if the graph is weighted.
    @Neo4jGraph
    @Language("Cypher")
    private static final
    String DB_CYPHER =
        "CREATE" +
        "  (a:Label1)" +
        ", (b:Label1)" +
        ", (c:Label1)" +
        ", (d:Label1)" +
        ", (e:Label1)" +
        ", (f:Label1)" +
        ", (g:Label1)" +

        ", (a)-[:TYPE1 {weight: 81.0}]->(b)" +
        ", (a)-[:TYPE1 {weight: 7.0}]->(d)" +
        ", (b)-[:TYPE1 {weight: 1.0}]->(d)" +
        ", (b)-[:TYPE1 {weight: 1.0}]->(e)" +
        ", (b)-[:TYPE1 {weight: 1.0}]->(f)" +
        ", (b)-[:TYPE1 {weight: 1.0}]->(g)" +
        ", (c)-[:TYPE1 {weight: 45.0}]->(b)" +
        ", (c)-[:TYPE1 {weight: 3.0}]->(e)" +
        ", (d)-[:TYPE1 {weight: 3.0}]->(c)" +
        ", (d)-[:TYPE1 {weight: 1.0}]->(b)" +
        ", (e)-[:TYPE1 {weight: 1.0}]->(b)" +
        ", (f)-[:TYPE1 {weight: 3.0}]->(a)" +
        ", (f)-[:TYPE1 {weight: 1.0}]->(b)" +
        ", (g)-[:TYPE1 {weight: 1.0}]->(b)" +
        ", (g)-[:TYPE1 {weight: 4.0}]->(c)";

    static final String GRAPH_NAME = "myGraph";


    @BeforeEach
    void setupGraph() throws Exception {
        registerProcedures(
            ApproxMaxKCutStreamProc.class,
            GraphProjectProc.class,
            GraphWriteNodePropertiesProc.class
        );

        String createQuery = GdsCypher.call(GRAPH_NAME)
            .graphProject()
            .loadEverything()
            .yields();

        runQuery(createQuery);
    }

    @Test
    void testStream() {
        String streamQuery = GdsCypher.call(GRAPH_NAME)
            .algo("maxkcut")
            .streamMode()
            // Make sure we get a deterministic result.
            .addParameter("randomSeed", 1337L)
            .addParameter("concurrency", 1)
            .yields();

        Map<Long, Long> expected = Map.of(
            idFunction.of("a"), 1L,
            idFunction.of("b"), 1L,
            idFunction.of("c"), 1L,
            idFunction.of("d"), 0L,
            idFunction.of("e"), 0L,
            idFunction.of("f"), 0L,
            idFunction.of("g"), 0L
        );

        var rowCount = runQueryWithRowConsumer(streamQuery, row -> {
            long nodeId = row.getNumber("nodeId").longValue();
            long communityId = row.getNumber("communityId").longValue();
            assertThat(communityId).isEqualTo(expected.get(nodeId));
        });
        
        assertThat(rowCount).isEqualTo(expected.keySet().size());

    }

    static Stream<Arguments> communitySizeInputs() {
        return Stream.of(
                Arguments.of(Map.of("minCommunitySize", 1), Map.of(
                        "a", 1L,
                        "b", 1L,
                        "c", 1L,
                        "d", 0L,
                        "e", 0L,
                        "f", 0L,
                        "g", 0L
                )),
                Arguments.of(Map.of("minCommunitySize", 4), Map.of(
                        "d", 0L,
                        "e", 0L,
                        "f", 0L,
                        "g", 0L
                ))
        );
    }

    @ParameterizedTest
    @MethodSource("communitySizeInputs")
    void testStreamWithMinCommunitySize(Map<String, Long> parameter, Map<String, Long> expectedResult) {
        String streamQuery = GdsCypher.call(GRAPH_NAME)
            .algo("maxkcut")
            .streamMode()
            // Make sure we get a deterministic result.
            .addParameter("randomSeed", 1337L)
            .addParameter("concurrency", 1)
            .addAllParameters(parameter)
            .yields();

        var rowCount = runQueryWithRowConsumer(streamQuery, row -> {
            long nodeId = row.getNumber("nodeId").longValue();
            long communityId = row.getNumber("communityId").longValue();
            assertThat(communityId).isEqualTo(expectedResult.get(idToVariable.of(nodeId)));
        });

        assertThat(rowCount).isEqualTo(expectedResult.keySet().size());
    }

    // Min k-cut capabilities not exposed in API yet.
    @Disabled
    @Test
    void testIllegalMinCommunitySizesSum() {
        String streamQuery = GdsCypher.call(GRAPH_NAME)
            .algo("gds.alpha.maxkcut")
            .streamMode()
            .addParameter("minCommunitySizes", List.of(100, 100))
            .yields();

        assertThatExceptionOfType(QueryExecutionException.class)
            .isThrownBy(() -> runQuery(streamQuery))
            .withRootCauseInstanceOf(IllegalArgumentException.class)
            .withMessage("The sum of min community sizes is larger than half of the number of nodes in the graph: 200 > 3");
    }

    @AfterEach
    void clearStore() {
        GraphStoreCatalog.removeAllLoadedGraphs();
    }

}
