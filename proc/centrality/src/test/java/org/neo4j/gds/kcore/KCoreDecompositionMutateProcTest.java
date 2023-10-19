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
package org.neo4j.gds.kcore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.gds.BaseProcTest;
import org.neo4j.gds.GdsCypher;
import org.neo4j.gds.Orientation;
import org.neo4j.gds.TestSupport;
import org.neo4j.gds.api.DatabaseId;
import org.neo4j.gds.catalog.GraphProjectProc;
import org.neo4j.gds.core.loading.GraphStoreCatalog;
import org.neo4j.gds.extension.Neo4jGraph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LONG;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.neo4j.gds.TestSupport.fromGdl;


class KCoreDecompositionMutateProcTest extends BaseProcTest {

    @Neo4jGraph
    private static final String DB_CYPHER =
        "CREATE " +
        "  (z:node)," +
        "  (a:node)," +
        "  (b:node)," +
        "  (c:node)," +
        "  (d:node)," +
        "  (e:node)," +
        "  (f:node)," +
        "  (g:node)," +
        "  (h:node)," +

        "(a)-[:R]->(b)," +
        "(b)-[:R]->(c)," +
        "(c)-[:R]->(d)," +
        "(d)-[:R]->(e)," +
        "(e)-[:R]->(f)," +
        "(f)-[:R]->(g)," +
        "(g)-[:R]->(h)," +
        "(h)-[:R]->(c)";

    @BeforeEach
    void setup() throws Exception {
        registerProcedures(
            KCoreDecompositionMutateProc.class,
            GraphProjectProc.class
        );

        runQuery(
            GdsCypher.call("graph")
                .graphProject()
                .withAnyLabel()
                .withRelationshipType("R", Orientation.UNDIRECTED)
                .yields()
        );
    }

    @Test
    void shouldMutate(){

        String query="CALL gds.kcore.mutate('graph', {mutateProperty: 'coreValue'})";

        var rowCount = runQueryWithRowConsumer(query, row -> {

            assertThat(row.getNumber("preProcessingMillis"))
                .as("preProcessingMillis")
                .asInstanceOf(LONG)
                .isGreaterThan(-1L);

            assertThat(row.getNumber("computeMillis"))
                .as("computeMillis")
                .asInstanceOf(LONG)
                .isGreaterThan(-1L);

            assertThat(row.getNumber("postProcessingMillis"))
                .as("postProcessingMillis")
                .asInstanceOf(LONG)
                .isEqualTo(-1L);

            assertThat(row.getNumber("mutateMillis"))
                .as("mutateMillis")
                .asInstanceOf(LONG)
                .isGreaterThan(-1L);

            assertThat(row.getNumber("nodePropertiesWritten"))
                .as("nodePropertiesWritten")
                .asInstanceOf(LONG)
                .isEqualTo(9L);

            assertThat(row.getNumber("degeneracy"))
                .as("degeneracy")
                .asInstanceOf(LONG)
                .isEqualTo(2L);

            assertThat(row.get("configuration"))
                .as("configuration")
                .asInstanceOf(MAP)
                .isNotEmpty();
        });

        assertThat(rowCount)
            .as("`mutate` mode should always return one row")
            .isEqualTo(1);

        var actualGraph = GraphStoreCatalog.get(getUsername(), DatabaseId.of(db), "graph")
            .graphStore()
            .getUnion();

        TestSupport.assertGraphEquals(fromGdl(expectedMutatedGraph(), Orientation.UNDIRECTED), actualGraph);
    }

    private String expectedMutatedGraph() {
        return "CREATE " +
               "  (z {coreValue: 0})," +
               "  (a {coreValue: 1})," +
               "  (b {coreValue: 1})," +
               "  (c {coreValue: 2})," +
               "  (d {coreValue: 2})," +
               "  (e {coreValue: 2})," +
               "  (f {coreValue: 2})," +
               "  (g {coreValue: 2})," +
               "  (h {coreValue: 2})," +

               "(a)-[:R]->(b)," +
               "(b)-[:R]->(c)," +
               "(c)-[:R]->(d)," +
               "(d)-[:R]->(e)," +
               "(e)-[:R]->(f)," +
               "(f)-[:R]->(g)," +
               "(g)-[:R]->(h)," +
               "(h)-[:R]->(c)";
    }

    @Test
    void memoryEstimation() {
        String query="CALL gds.kcore.mutate.estimate({nodeCount: 100, relationshipCount: 200, nodeProjection: '*', relationshipProjection: '*'}, {mutateProperty: 'kcore'})";

        var rowCount = runQueryWithRowConsumer(query, row -> {
            assertThat(row.getNumber("bytesMin")).asInstanceOf(LONG).isGreaterThan(0L);
            assertThat(row.getNumber("bytesMax")).asInstanceOf(LONG).isGreaterThan(0L);
        });

        assertThat(rowCount)
            .as("`estimate` mode should always return one row")
            .isEqualTo(1);

    }
}
