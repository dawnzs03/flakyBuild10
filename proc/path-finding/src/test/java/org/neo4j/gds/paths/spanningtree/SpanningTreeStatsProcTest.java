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
package org.neo4j.gds.paths.spanningtree;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.gds.BaseProcTest;
import org.neo4j.gds.GdsCypher;
import org.neo4j.gds.Orientation;
import org.neo4j.gds.catalog.GraphProjectProc;
import org.neo4j.gds.extension.Neo4jGraph;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * a                a
 * 1 /   \ 2          /  \
 * /     \          /    \
 * b --3-- c        b      c
 * |       |   =>   |      |
 * 4       5        |      |
 * |       |        |      |
 * d --6-- e        d      e
 */
class SpanningTreeStatsProcTest extends BaseProcTest {

    @Neo4jGraph
    static final String DB_CYPHER = "CREATE(a:Node) " +
                                    "CREATE(b:Node) " +
                                    "CREATE(c:Node) " +
                                    "CREATE(d:Node) " +
                                    "CREATE(e:Node) " +
                                    "CREATE(z:Node) " +
                                    "CREATE (a)-[:TYPE {cost:1.0}]->(b) " +
                                    "CREATE (a)-[:TYPE {cost:2.0}]->(c) " +
                                    "CREATE (b)-[:TYPE {cost:3.0}]->(c) " +
                                    "CREATE (b)-[:TYPE {cost:4.0}]->(d) " +
                                    "CREATE (c)-[:TYPE {cost:5.0}]->(e) " +
                                    "CREATE (d)-[:TYPE {cost:6.0}]->(e)";

    @BeforeEach
    void setup() throws Exception {
        registerProcedures(SpanningTreeStatsProc.class, GraphProjectProc.class);
        var createQuery = GdsCypher.call(DEFAULT_GRAPH_NAME)
            .graphProject()
            .withAnyLabel()
            .withRelationshipType("TYPE", Orientation.UNDIRECTED)
            .withRelationshipProperty("cost")
            .yields();
        runQuery(createQuery);
    }

    private long getSourceNode() {
        return idFunction.of("a");
    }

    @ParameterizedTest
    @ValueSource(strings = {"gds.spanningTree", "gds.beta.spanningTree"})
    void testYields(String tieredProcedure) {
        String query = GdsCypher.call(DEFAULT_GRAPH_NAME)
            .algo(tieredProcedure)
            .statsMode()
            .addParameter("sourceNode", getSourceNode())
            .addParameter("relationshipWeightProperty", "cost")
            .yields(
                "preProcessingMillis",
                "computeMillis",
                "effectiveNodeCount",
                "totalWeight"
            );

        runQueryWithRowConsumer(
            query,
            res -> {
                assertThat(res.getNumber("effectiveNodeCount").longValue()).isEqualTo(5L);
                assertThat(res.getNumber("totalWeight").doubleValue()).isEqualTo(12.0);
                assertThat(res.getNumber("preProcessingMillis").longValue()).isGreaterThanOrEqualTo(0L);
                assertThat(res.getNumber("computeMillis").longValue()).isGreaterThanOrEqualTo(0L);
            }
        );

    }


}
