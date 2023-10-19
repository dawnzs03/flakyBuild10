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
package org.neo4j.gds.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.neo4j.gds.BaseTest;
import org.neo4j.gds.Orientation;
import org.neo4j.gds.PropertyMapping;
import org.neo4j.gds.StoreLoaderBuilder;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.api.RelationshipWithPropertyConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.neo4j.gds.utils.StringFormatting.formatWithLocale;

class RelationshipPropertiesImportTest extends BaseTest {

    private Graph graph;

    @Test
    void testPropertiesOfInterconnectedNodesWithOutgoing() {
        setup("CREATE (a:N),(b:N) CREATE (a)-[:R{w:1}]->(b),(b)-[:R{w:2}]->(a)", Orientation.NATURAL);

        checkProperties(0, Orientation.NATURAL, 1.0);
        checkProperties(1, Orientation.NATURAL, 2.0);
    }

    @Test
    void testPropertiesOfTriangledNodesWithOutgoing() {
        setup("CREATE (a:N),(b:N),(c:N) CREATE (a)-[:R{w:1}]->(b),(b)-[:R{w:2}]->(c),(c)-[:R{w:3}]->(a)", Orientation.NATURAL);

        checkProperties(0, Orientation.NATURAL, 1.0);
        checkProperties(1, Orientation.NATURAL, 2.0);
        checkProperties(2, Orientation.NATURAL, 3.0);
    }

    @Test
    void testPropertiesOfInterconnectedNodesWithIncoming() {
        setup("CREATE (a:N),(b:N) CREATE (a)-[:R{w:1}]->(b),(b)-[:R{w:2}]->(a)", Orientation.REVERSE);

        checkProperties(0, Orientation.REVERSE, 2.0);
        checkProperties(1, Orientation.REVERSE, 1.0);
    }

    @Test
    void testPropertiesOfTriangledNodesWithIncoming() {
        setup("CREATE (a:N),(b:N),(c:N) CREATE (a)-[:R{w:1}]->(b),(b)-[:R{w:2}]->(c),(c)-[:R{w:3}]->(a)", Orientation.REVERSE);

        checkProperties(0, Orientation.REVERSE, 3.0);
        checkProperties(1, Orientation.REVERSE, 1.0);
        checkProperties(2, Orientation.REVERSE, 2.0);
    }

    @Test
    void testPropertiesOfInterconnectedNodesWithUndirected() {
        setup("CREATE (a:N),(b:N) CREATE (a)-[:R{w:1}]->(b),(b)-[:R{w:2}]->(a)", Orientation.UNDIRECTED);

        checkProperties(0, Orientation.UNDIRECTED, 1.0, 2.0);
        checkProperties(1, Orientation.UNDIRECTED, 2.0, 1.0);
    }

    @Test
    void testPropertiesOfTriangledNodesWithUndirected() {
        setup("CREATE (a:N),(b:N),(c:N) CREATE (a)-[:R{w:1}]->(b),(b)-[:R{w:2}]->(c),(c)-[:R{w:3}]->(a)", Orientation.UNDIRECTED);

        checkProperties(0, Orientation.UNDIRECTED, 1.0, 3.0);
        checkProperties(1, Orientation.UNDIRECTED, 1.0, 2.0);
        checkProperties(2, Orientation.UNDIRECTED, 3.0, 2.0);
    }

    private void setup(String cypher, Orientation orientation) {
        runQuery(cypher);

        graph = new StoreLoaderBuilder()
            .databaseService(db)
            .globalOrientation(orientation)
            .addRelationshipProperty(PropertyMapping.of("w", 0.0))
            .build()
            .graph();
    }

    private void checkProperties(int nodeId, Orientation orientation, double... expecteds) {
        AtomicInteger i = new AtomicInteger();
        int limit = expecteds.length;
        List<Executable> assertions = new ArrayList<>();

        RelationshipWithPropertyConsumer consumer = (s, t, w) -> {
            String rel = formatWithLocale("(%d %s %d)", s, arrow(orientation), t);
            if (i.get() >= limit) {
                assertions.add(() -> assertFalse(
                    i.get() >= limit,
                    formatWithLocale("Unexpected relationship: %s = %.1f", rel, w)
                ));
                return false;
            }
            final int index = i.getAndIncrement();
            double expectedIterator = expecteds[index];
            assertions.add(() -> assertEquals(
                expectedIterator,
                w,
                1e-4,
                formatWithLocale("%s (WRI): %.1f != %.1f", rel, w, expectedIterator)
            ));
            return true;
        };

        graph.forEachRelationship(nodeId, Double.NaN, consumer);
        assertAll(assertions);
    }

    private static String arrow(Orientation orientation) {
        switch (orientation) {
            case NATURAL:
                return "->";
            case REVERSE:
                return "<-";
            case UNDIRECTED:
                return "<->";
            default:
                throw new IllegalArgumentException("Unknown orientation: " + orientation);
        }
    }
}
