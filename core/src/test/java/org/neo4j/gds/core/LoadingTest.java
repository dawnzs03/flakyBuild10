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

import com.carrotsearch.hppc.LongArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.gds.BaseTest;
import org.neo4j.gds.StoreLoaderBuilder;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.core.concurrency.Pools;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class LoadingTest extends BaseTest {

    public static final String DB_CYPHER =
        "CREATE " +
        "  (a:Node {name:'a'})" +
        ", (b:Node {name:'b'})" +
        ", (c:Node {name:'c'})" +
        ", (d:Node2 {name:'d'})" +
        ", (e:Node2 {name:'e'})" +
        ", (a)-[:TYPE {prop:1}]->(b)" +
        ", (e)-[:TYPE {prop:2}]->(d)" +
        ", (d)-[:TYPE {prop:3}]->(c)" +
        ", (a)-[:TYPE {prop:4}]->(c)" +
        ", (a)-[:TYPE {prop:5}]->(d)" +
        ", (a)-[:TYPE2 {prop:6}]->(d)" +
        ", (b)-[:TYPE2 {prop:7}]->(e)" +
        ", (a)-[:TYPE2 {prop:8}]->(e)";

    @BeforeEach
    void setupGraphDb() {
        runQuery(DB_CYPHER);
    }

    @Test
    void testBasicLoading() {
        Graph graph = new StoreLoaderBuilder()
                .databaseService(db)
                .executorService(Pools.DEFAULT)
                .addNodeLabel("Node")
                .addRelationshipType("TYPE")
                .build()
                .graph();

        assertEquals(3, graph.nodeCount());

        assertEquals(2, graph.degree(0));
        assertEquals(0, graph.degree(1));
        assertEquals(0, graph.degree(2));

        checkRels(graph, 0, 1, 2);
        checkRels(graph, 1);
        checkRels(graph, 2);
    }

    private void checkRels(Graph graph, int node, long... expected) {
        Arrays.sort(expected);
        assertArrayEquals(expected, mkRels(graph, node));
    }

    private long[] mkRels(Graph graph, int node) {
        final LongArrayList rels = new LongArrayList();
        graph.forEachRelationship(node, (s, t) -> {
            rels.add(t);
            return true;
        });
        long[] ids = rels.toArray();
        Arrays.sort(ids);
        return ids;
    }
}
