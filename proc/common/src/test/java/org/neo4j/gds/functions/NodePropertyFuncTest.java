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
package org.neo4j.gds.functions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.gds.BaseProcTest;
import org.neo4j.gds.GdsCypher;
import org.neo4j.gds.catalog.GraphProjectProc;
import org.neo4j.gds.core.loading.GraphStoreCatalog;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class NodePropertyFuncTest  extends BaseProcTest {

    private static final String DB_CYPHER =
        "CREATE " +
        "  (a:A { prop: 42.0, longListProp: [ 1, 2 ], doubleListProp: [ 0.1, 2.7 ] })" +
        ", (b:B { prop: 84.0, longListProp: [ 3, 4 ], doubleListProp: [ 0.3, 4.7 ] })";

    @BeforeEach
    void setUp() throws Exception {
        registerProcedures(GraphProjectProc.class);
        registerFunctions(NodePropertyFunc.class);
        runQuery(DB_CYPHER);
        runQuery(GdsCypher
            .call("testGraph")
            .graphProject()
            .withNodeLabel("A")
            .withNodeLabel("B")
            .withNodeProperty("prop")
            .withNodeProperty("longListProp")
            .withNodeProperty("doubleListProp")
            .withAnyRelationshipType()
            .yields());
    }

    @AfterEach
    void tearDown() {
        GraphStoreCatalog.removeAllLoadedGraphs();
    }

    @Test
    void shouldReturnNodeProperty() {
        String query = "MATCH (n) RETURN gds.util.nodeProperty('testGraph', id(n), 'prop') AS prop ORDER BY prop ASC";
        assertCypherResult(query, Arrays.asList(Map.of("prop", 42.0), Map.of("prop", 84.0)));
    }

    @Test
    void shouldReturnNodePropertyForLabel() {
        String query = "MATCH (n) RETURN gds.util.nodeProperty('testGraph', id(n), 'prop', 'A') AS prop ORDER BY prop ASC";
        var resultRecordWithNull = new HashMap<String, Object>();
        resultRecordWithNull.put("prop", null);
        assertCypherResult(query, Arrays.asList(Map.of("prop", 42.0), resultRecordWithNull));
    }

    @Test
    void shouldReturnLongArrayProperty() {
        String query = "MATCH (n) RETURN gds.util.nodeProperty('testGraph', id(n), 'longListProp') AS longListProp ORDER BY longListProp ASC";
        assertCypherResult(query, Arrays.asList(
            Map.of("longListProp", new long[] {1, 2}),
            Map.of("longListProp", new long[] {3, 4}))
        );
    }

    @Test
    void shouldReturnDoubleArrayProperty() {
        String query = "MATCH (n) RETURN gds.util.nodeProperty('testGraph', id(n), 'doubleListProp') AS doubleListProp ORDER BY doubleListProp ASC";
        assertCypherResult(query, Arrays.asList(
            Map.of("doubleListProp", new double[] {0.1, 2.7}),
            Map.of("doubleListProp", new double[] {0.3, 4.7}))
        );
    }

    @Test
    void failsOnNonExistingGraph() {
        String query = "MATCH (n) RETURN gds.util.nodeProperty('noGraph', id(n), 'prop') AS prop";
        assertError(query, "Graph with name `noGraph` does not exist on database `neo4j`.");
    }

    @Test
    void failsOnNonExistingNode() {
        String query = "MATCH (n) RETURN gds.util.nodeProperty('testGraph', 42, 'prop') AS prop";
        assertError(query, "Node id 42 does not exist.");
    }

    @Test
    void failsOnNonExistingProperty() {
        String query = "MATCH (n) RETURN gds.util.nodeProperty('testGraph', id(n), 'noProp') AS prop";
        assertError(query, "No node projection with property 'noProp' exists.");
    }

    @Test
    void failsOnNonExistingPropertyForLabel() {
        String query = "MATCH (n) RETURN gds.util.nodeProperty('testGraph', id(n), 'noProp', 'A') AS prop";
        assertError(query, "Node projection 'A' does not have property key 'noProp'. Available keys: ['doubleListProp', 'longListProp', 'prop'].");
    }
}
