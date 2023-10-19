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
package org.neo4j.gds.leiden;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.gds.BaseProcTest;
import org.neo4j.gds.GdsCypher;
import org.neo4j.gds.Orientation;
import org.neo4j.gds.catalog.GraphProjectProc;
import org.neo4j.gds.extension.Neo4jGraph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.InstanceOfAssertFactories.DOUBLE;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.assertj.core.api.InstanceOfAssertFactories.LONG;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;

class LeidenStatsProcTest extends BaseProcTest {

    @Neo4jGraph
    private static final String DB_CYPHER =
        "CREATE " +
        "  (a0:Node)," +
        "  (a1:Node)," +
        "  (a2:Node)," +
        "  (a3:Node)," +
        "  (a4:Node)," +
        "  (a5:Node)," +
        "  (a6:Node)," +
        "  (a7:Node)," +
        "  (a0)-[:R {weight: 1.0}]->(a1)," +
        "  (a0)-[:R {weight: 1.0}]->(a2)," +
        "  (a0)-[:R {weight: 1.0}]->(a3)," +
        "  (a0)-[:R {weight: 1.0}]->(a4)," +
        "  (a2)-[:R {weight: 1.0}]->(a3)," +
        "  (a2)-[:R {weight: 1.0}]->(a4)," +
        "  (a3)-[:R {weight: 1.0}]->(a4)," +
        "  (a1)-[:R {weight: 1.0}]->(a5)," +
        "  (a1)-[:R {weight: 1.0}]->(a6)," +
        "  (a1)-[:R {weight: 1.0}]->(a7)," +
        "  (a5)-[:R {weight: 1.0}]->(a6)," +
        "  (a5)-[:R {weight: 1.0}]->(a7)," +
        "  (a6)-[:R {weight: 1.0}]->(a7)";

    @BeforeEach
    void setUp() throws Exception {
        registerProcedures(
            GraphProjectProc.class,
            LeidenStatsProc.class
        );

        var projectQuery = GdsCypher.call("leiden").graphProject().loadEverything(Orientation.UNDIRECTED).yields();
        runQuery(projectQuery);
    }

    @Test
    void stats() {

        var query = "CALL gds.leiden.stats('leiden')";

        runQuery(query, result -> {
            assertThat(result.columns())
                .containsExactlyInAnyOrder(
                    "ranLevels",
                    "didConverge",
                    "communityDistribution",
                    "preProcessingMillis",
                    "computeMillis",
                    "nodeCount",
                    "communityCount",
                    "postProcessingMillis",
                    "modularity",
                    "modularities",
                    "configuration"
                );

            while (result.hasNext()) {
                var resultRow = result.next();
                assertThat(resultRow.get("communityDistribution"))
                    .isNotNull()
                    .asInstanceOf(MAP)
                    .isNotEmpty();

                assertThat(resultRow.get("nodeCount"))
                    .asInstanceOf(LONG)
                    .isEqualTo(8);
                assertThat(resultRow.get("communityCount"))
                    .asInstanceOf(LONG)
                    .isGreaterThanOrEqualTo(1);
                assertThat(resultRow.get("ranLevels"))
                    .asInstanceOf(LONG)
                    .isGreaterThanOrEqualTo(1);
                assertThat(resultRow.get("didConverge"))
                    .isInstanceOf(Boolean.class);
                assertThat(resultRow.get("modularities"))
                    .asInstanceOf(LIST).hasSize((int) (long) resultRow.get("ranLevels"));
                assertThat(resultRow.get("modularity"))
                    .asInstanceOf(DOUBLE);
                assertThat(resultRow.get("preProcessingMillis"))
                    .asInstanceOf(LONG)
                    .isGreaterThanOrEqualTo(0);
                assertThat(resultRow.get("computeMillis"))
                    .asInstanceOf(LONG)
                    .isGreaterThanOrEqualTo(0);
                assertThat(resultRow.get("postProcessingMillis"))
                    .asInstanceOf(LONG)
                    .isGreaterThanOrEqualTo(0);

                assertThat(resultRow.get("configuration"))
                    .isNotNull()
                    .asInstanceOf(MAP)
                    .isNotEmpty();
            }

            return true;
        });
    }

    @Test
    void shouldEstimateMemory() {
        var query = "CALL gds.leiden.stats.estimate('leiden', {" +
                    "   includeIntermediateCommunities: true" +
                    "})";
        assertThatNoException().isThrownBy(() -> runQuery(query));
    }
}
