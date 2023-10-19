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
package org.neo4j.gds.paths.steiner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.gds.BaseProcTest;
import org.neo4j.gds.GdsCypher;
import org.neo4j.gds.Orientation;
import org.neo4j.gds.catalog.GraphProjectProc;
import org.neo4j.gds.extension.Neo4jGraph;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SteinerTreeStatsProcTest extends BaseProcTest {

    @Neo4jGraph
    static final String DB_CYPHER = "CREATE(a:Node) " +
                                    "CREATE(b:Node) " +
                                    "CREATE(c:Node) " +
                                    "CREATE (a)-[:TYPE {cost:1.0}]->(b) ";

    @BeforeEach
    void setup() throws Exception {
        registerProcedures(SteinerTreeStatsProc.class, GraphProjectProc.class);
        var createQuery = GdsCypher.call(DEFAULT_GRAPH_NAME)
            .graphProject()
            .withAnyLabel()
            .withRelationshipType("TYPE", Orientation.NATURAL)
            .withRelationshipProperty("cost")
            .yields();
        runQuery(createQuery);
    }


    private long getSourceNode() {
        return idFunction.of("a");
    }

    private long getTerminal() {
        return idFunction.of("b");
    }

    @ParameterizedTest
    @ValueSource(strings = {"gds.steinerTree", "gds.beta.steinerTree"})
    void testYields(String procedure) {
        String query = GdsCypher.call(DEFAULT_GRAPH_NAME)
            .algo(procedure)
            .statsMode()
            .addParameter("sourceNode", getSourceNode())
            .addParameter("targetNodes", List.of(getTerminal()))
            .addParameter("relationshipWeightProperty", "cost")
            .yields(
                "preProcessingMillis",
                "computeMillis",
                "effectiveNodeCount",
                "totalWeight",
                "effectiveTargetNodesCount"
            );
        var rowCount = runQueryWithRowConsumer(
            query,
            res -> {
                assertThat(res.getNumber("effectiveNodeCount").longValue()).isEqualTo(2L);
                assertThat(res.getNumber("effectiveTargetNodesCount").doubleValue()).isEqualTo(1.0);
                assertThat(res.getNumber("totalWeight").doubleValue()).isEqualTo(1.0);
                assertThat(res.getNumber("preProcessingMillis").longValue()).isGreaterThanOrEqualTo(0L);
                assertThat(res.getNumber("computeMillis").longValue()).isGreaterThanOrEqualTo(0L);
            }
        );
        assertThat(rowCount).isEqualTo(1L);
    }
}
