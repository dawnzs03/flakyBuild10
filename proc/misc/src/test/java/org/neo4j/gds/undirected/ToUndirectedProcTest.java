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
package org.neo4j.gds.undirected;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.gds.BaseProcTest;
import org.neo4j.gds.GdsCypher;
import org.neo4j.gds.Orientation;
import org.neo4j.gds.RelationshipType;
import org.neo4j.gds.catalog.GraphProjectProc;
import org.neo4j.gds.core.loading.GraphStoreCatalog;
import org.neo4j.gds.extension.Neo4jGraph;
import org.neo4j.graphdb.QueryExecutionException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.neo4j.gds.TestSupport.assertGraphEquals;
import static org.neo4j.gds.TestSupport.fromGdl;

class ToUndirectedProcTest extends BaseProcTest {

    @Neo4jGraph
    public static final String DB = "CREATE " +
                                    " (a:A) " +
                                    ",(b:B) " +
                                    ",(c:C) " +
                                    ",(a)-[:REL {prop1: 1.0}]->(b)" +
                                    ",(a)-[:REL {prop1: 2.0}]->(c)" +
                                    ",(b)-[:REL {prop1: 3.0}]->(c)";

    @BeforeEach
    void setup() throws Exception {
        registerProcedures(ToUndirectedProc.class, GraphProjectProc.class);

        runQuery(GdsCypher.call("graph")
            .graphProject()
            .withNodeLabel("A")
            .withNodeLabel("B")
            .withNodeLabel("C")
            .withRelationshipType("REL")
            .withRelationshipProperty("prop1")
            .yields()
        );

        runQuery(GdsCypher.call("undirected_graph")
            .graphProject()
            .withNodeLabel("A")
            .withNodeLabel("B")
            .withNodeLabel("C")
            .withRelationshipType("REL", Orientation.UNDIRECTED)
            .withRelationshipProperty("prop1")
            .yields()
        );
    }

    @Test
    void convertToUndirected() {
        String query = "CALL gds.beta.graph.relationships.toUndirected('graph', {relationshipType: 'REL', mutateRelationshipType: 'REL2'})";

        assertCypherResult(query, List.of(Map.of("inputRelationships", 3L,
            "relationshipsWritten", 6L,
            "mutateMillis", greaterThanOrEqualTo(0L),
            "preProcessingMillis",greaterThanOrEqualTo(0L),
            "computeMillis",greaterThanOrEqualTo(0L),
            "postProcessingMillis",instanceOf(Long.class),
            "configuration", instanceOf(Map.class))
        ));

        var gs = GraphStoreCatalog.get("", "neo4j", "graph");
        var graph = gs.graphStore().getGraph(RelationshipType.of("REL2"), Optional.of("prop1"));
        assertGraphEquals(
            fromGdl(DB.replace("REL", "REL2"), Orientation.UNDIRECTED),
            graph
        );
    }

    @Test
    void shouldFailIfMutateRelationshipTypeExists() {
        String query = "CALL gds.beta.graph.relationships.toUndirected('graph', {relationshipType: 'REL', mutateRelationshipType: 'REL'})";

        assertThatThrownBy(() -> runQuery(query))
            .isInstanceOf(QueryExecutionException.class)
            .rootCause()
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Relationship type `REL` already exists in the in-memory graph.");
    }

    @Test
    void shouldAllowStarIfStarWasProjected() {
        runQuery(GdsCypher.call("star_graph")
            .graphProject()
            .withAnyLabel()
            .withAnyRelationshipType()
            .withRelationshipType("REL")
            .withRelationshipProperty("prop1")
            .yields()
        );

        assertCypherResult(
            "CALL gds.beta.graph.relationships.toUndirected('star_graph', {relationshipType: '*', mutateRelationshipType: 'UNDIRECTED'}) YIELD relationshipsWritten",
            List.of(Map.of("relationshipsWritten", 6L))
        );
    }

    @Test
    void shouldFailForStarRelationshipTypeIfNotStarProjected() {
        String query = "CALL gds.beta.graph.relationships.toUndirected('graph', {relationshipType: '*', mutateRelationshipType: 'REL'})";

        assertThatThrownBy(() -> runQuery(query)).hasMessageContaining(
            "The 'relationshipType' parameter can only be '*' if '*' was projected. Available types are ['REL']."
        );
    }

    @Test
    void shouldFailIfRelationshipTypeDoesNotExists() {
        String query = "CALL gds.beta.graph.relationships.toUndirected('graph', {relationshipType: 'REL2', mutateRelationshipType: 'OTHER_REL'})";

        assertThatThrownBy(() -> runQuery(query))
            .isInstanceOf(QueryExecutionException.class)
            .hasRootCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage(
                "Could not find the specified `relationshipType` of ['REL2']. Available relationship types are ['REL'].");
    }

    @Test
    void shouldFailIfRelationshipTypeIsAlreadyUndirected() {
        String query = "CALL gds.beta.graph.relationships.toUndirected('undirected_graph', {relationshipType: 'REL', mutateRelationshipType: 'REL2'})";


        assertThatThrownBy(() -> runQuery(query))
            .rootCause()
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("The specified relationship type `REL` is already undirected.");
    }

    @Test
    void memoryEstimation() {
        String query = "CALL gds.beta.graph.relationships.toUndirected.estimate('graph', {relationshipType: 'REL', mutateRelationshipType: 'REL2'})";

        assertCypherResult(query, List.of(Map.of(
            "mapView", instanceOf(Map.class),
            "treeView", instanceOf(String.class),
            "bytesMax", 524648L,
            "heapPercentageMin", 0.1D,
            "nodeCount", 3L,
            "requiredMemory", "[256 KiB ... 512 KiB]",
            "bytesMin", 262368L,
            "heapPercentageMax", 0.1D,
            "relationshipCount", 3L
        )));
    }
}
