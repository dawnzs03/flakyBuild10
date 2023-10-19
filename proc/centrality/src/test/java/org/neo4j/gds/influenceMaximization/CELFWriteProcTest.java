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
package org.neo4j.gds.influenceMaximization;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.gds.BaseProcTest;
import org.neo4j.gds.GdsCypher;
import org.neo4j.gds.Orientation;
import org.neo4j.gds.RelationshipProjection;
import org.neo4j.gds.catalog.GraphProjectProc;
import org.neo4j.gds.core.Aggregation;
import org.neo4j.gds.core.loading.GraphStoreCatalog;
import org.neo4j.gds.extension.Neo4jGraph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.assertj.core.api.InstanceOfAssertFactories.LONG;

/**
 *     (c)-----|
 *    /(d)\----|-|
 *   //(e)\\---|-|-|
 *  ///(f)\\\--|-|-|-|
 * ////   \\\\ | | | |
 * (a)     (b) | | | |
 * \\\\   //// | | | |
 *  \\\(g)///--| | | |
 *   \\(h)//-----| | |
 *    \(i)/--------| |
 *     (j)-----------|
 */
class CELFWriteProcTest extends BaseProcTest {

    private static final long NODE_COUNT = 10L;

    @Neo4jGraph
    private static final String DB_CYPHER =
        "CREATE " +
        "  (a:Node)" +
        ", (b:Node)" +
        ", (c:Node)" +
        ", (d:Node)" +
        ", (e:Node)" +
        ", (f:Node)" +
        ", (g:Node)" +
        ", (h:Node)" +
        ", (i:Node)" +
        ", (j:Node)" +

        ", (a)-[:RELATIONSHIP]->(c)" +
        ", (a)-[:RELATIONSHIP]->(d)" +
        ", (a)-[:RELATIONSHIP]->(e)" +
        ", (a)-[:RELATIONSHIP]->(f)" +
        ", (a)-[:RELATIONSHIP]->(g)" +
        ", (a)-[:RELATIONSHIP]->(h)" +
        ", (a)-[:RELATIONSHIP]->(i)" +
        ", (a)-[:RELATIONSHIP]->(j)" +

        ", (b)-[:RELATIONSHIP]->(c)" +
        ", (b)-[:RELATIONSHIP]->(d)" +
        ", (b)-[:RELATIONSHIP]->(e)" +
        ", (b)-[:RELATIONSHIP]->(f)" +
        ", (b)-[:RELATIONSHIP]->(g)" +
        ", (b)-[:RELATIONSHIP]->(h)" +
        ", (b)-[:RELATIONSHIP]->(i)" +
        ", (b)-[:RELATIONSHIP]->(j)" +

        ", (c)-[:RELATIONSHIP]->(g)" +
        ", (d)-[:RELATIONSHIP]->(h)" +
        ", (e)-[:RELATIONSHIP]->(i)" +
        ", (f)-[:RELATIONSHIP]->(j)";

    @BeforeEach
    void setup() throws Exception {
        registerProcedures(CELFWriteProc.class, GraphProjectProc.class);

        String graphCreateQuery = GdsCypher.call("celfGraph")
            .graphProject()
            .withNodeLabel("Node")
            .withRelationshipType(
                "RELATIONSHIP",
                RelationshipProjection.of(
                    "RELATIONSHIP",
                    Orientation.NATURAL,
                    Aggregation.DEFAULT
                )
            ).yields();

        runQuery(graphCreateQuery);
    }

    @AfterEach
    void shutdownGraph() {
        GraphStoreCatalog.removeAllLoadedGraphs();
    }

    @ParameterizedTest
    @ValueSource(strings = {"gds.beta.influenceMaximization.celf", "gds.influenceMaximization.celf"})
    void write(String tieredProcedure) {
        var cypher = GdsCypher.call("celfGraph")
            .algo(tieredProcedure)
            .writeMode()
            .addParameter("seedSetSize", 10)
            .addParameter("propagationProbability", 0.2)
            .addParameter("monteCarloSimulations", 10)
            .addParameter("writeProperty", "celf")
            .yields();

        var rowCount = runQueryWithRowConsumer(cypher, resultRow -> {
            assertThat(resultRow.getNumber("writeMillis"))
                .asInstanceOf(LONG)
                .isGreaterThanOrEqualTo(0L);
            assertThat(resultRow.getNumber("nodePropertiesWritten"))
                .asInstanceOf(LONG)
                .isEqualTo(NODE_COUNT);
        });

        assertThat(rowCount).isEqualTo(1);

        var influentialQueryRow = runQueryWithRowConsumer(
            "MATCH (n) WHERE n.celf > 0 RETURN count(n) AS influentialNodes",
            resultRow -> {
                assertThat(resultRow.getNumber("influentialNodes"))
                    .asInstanceOf(LONG)
                    .isEqualTo(10);
            }
        );

        assertThat(influentialQueryRow).isEqualTo(1);

        var nonInfluentialQueryRow = runQueryWithRowConsumer(
            "MATCH (n) WHERE n.celf = 0 RETURN count(n) AS notInfluentialNodes",
            resultRow -> {
                assertThat(resultRow.getNumber("notInfluentialNodes"))
                    .asInstanceOf(LONG)
                    .isEqualTo(NODE_COUNT - 10);
            }
        );

        assertThat(nonInfluentialQueryRow).isEqualTo(1);
    }


    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void writeSmallSets(int seedSetSize) {
        var cypher = GdsCypher.call("celfGraph")
            .algo("gds.influenceMaximization.celf")
            .writeMode()
            .addParameter("seedSetSize", seedSetSize)
            .addParameter("propagationProbability", 0.2)
            .addParameter("monteCarloSimulations", 10)
            .addParameter("writeProperty", "celf")
            .yields();

        var rowCount = runQueryWithRowConsumer(cypher, resultRow -> {
            assertThat(resultRow.getNumber("writeMillis"))
                .asInstanceOf(LONG)
                .isGreaterThanOrEqualTo(0L);
            assertThat(resultRow.getNumber("nodePropertiesWritten"))
                .asInstanceOf(LONG)
                .isEqualTo(NODE_COUNT);
        });

        assertThat(rowCount).isEqualTo(1);

        var influentialQueryRow = runQueryWithRowConsumer(
            "MATCH (n) WHERE n.celf > 0 RETURN count(n) AS influentialNodes",
            resultRow -> {
                assertThat(resultRow.getNumber("influentialNodes"))
                    .asInstanceOf(LONG)
                    .isEqualTo(seedSetSize);
            }
        );

        assertThat(influentialQueryRow).isEqualTo(1);

        var nonInfluentialQueryRow = runQueryWithRowConsumer(
            "MATCH (n) WHERE n.celf = 0 RETURN count(n) AS notInfluentialNodes",
            resultRow -> {
                assertThat(resultRow.getNumber("notInfluentialNodes"))
                    .asInstanceOf(LONG)
                    .isEqualTo(NODE_COUNT - seedSetSize);
            }
        );

        assertThat(nonInfluentialQueryRow).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"gds.beta.influenceMaximization.celf", "gds.influenceMaximization.celf"})
    void shouldCallMemoryEstimation(String tieredProcedure) {
        var query = GdsCypher.call("celfGraph")
            .algo(tieredProcedure)
            .estimationMode(GdsCypher.ExecutionModes.WRITE)
            .addParameter("seedSetSize", 5)
            .addParameter("writeProperty", "foo")
            .addParameter("propagationProbability", 0.2)
            .addParameter("monteCarloSimulations", 10)
            .yields();

        assertThatNoException().isThrownBy(() -> runQuery(query));
    }
}
