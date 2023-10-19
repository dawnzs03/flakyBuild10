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
package org.neo4j.gds.linkprediction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.gds.BaseProcTest;
import org.neo4j.graphdb.Result;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PreferentialAttachmentProcTest extends BaseProcTest {

    private static final String DB_CYPHER =
            "CREATE (mark:Person {name: 'Mark'})\n" +
            "CREATE (michael:Person {name: 'Michael'})\n" +
            "CREATE (praveena:Person {name: 'Praveena'})\n" +
            "CREATE (ryan:Person {name: 'Ryan'})\n" +
            "CREATE (karin:Person {name: 'Karin'})\n" +
            "CREATE (jennifer:Person {name: 'Jennifer'})\n" +
            "CREATE (elaine:Person {name: 'Elaine'})\n" +
            "CREATE (arya:Person {name: 'Arya'})\n" +

            "MERGE (elaine)-[:FRIENDS]-(karin)\n" +

            "MERGE (jennifer)-[:FRIENDS]-(ryan)\n" +
            "MERGE (jennifer)-[:FRIENDS]-(mark)\n" +
            "MERGE (jennifer)-[:FOLLOWS]->(praveena)\n" +
            "MERGE (jennifer)-[:WORKS_WITH]-(praveena)\n" +

            "MERGE (praveena)-[:FRIENDS]-(michael)\n" +
            "MERGE (praveena)-[:FOLLOWS]->(michael)";

    @BeforeEach
    void setUp() throws Exception {
        registerFunctions(LinkPredictionFunc.class);
        runQuery(DB_CYPHER);
    }

    @Test
    void sameNodesHaveDegreeSquared() {
        String controlQuery =
                "MATCH (p1:Person {name: 'Jennifer'})\n" +
                        "MATCH (p2:Person {name: 'Jennifer'})\n" +
                        "RETURN gds.alpha.linkprediction.preferentialAttachment(p1, p2) AS score, " +
                        "       16.0 AS cypherScore";

        Map<String, Object> node = runQuery(controlQuery, Result::next);
        assertEquals((Double) node.get("cypherScore"), (double) node.get("score"), 0.01);
    }


    @Test
    void oneIsolatedNode() {
        String controlQuery =
                "MATCH (p1:Person {name: 'Mark'})\n" +
                "MATCH (p2:Person {name: 'Arya'})\n" +
                "RETURN gds.alpha.linkprediction.preferentialAttachment(p1, p2) AS score, " +
                "       0.0 AS cypherScore";

        Map<String, Object> node = runQuery(controlQuery, Result::next);
        assertEquals((Double) node.get("cypherScore"), (double) node.get("score"), 0.01);
    }

    @Test
    void nodesOnlyLinkToEachOther() {
        String controlQuery =
                "MATCH (p1:Person {name: 'Karin'})\n" +
                        "MATCH (p2:Person {name: 'Elaine'})\n" +
                        "RETURN gds.alpha.linkprediction.preferentialAttachment(p1, p2) AS score, " +
                        "       1.0 AS cypherScore";

        Map<String, Object> node = runQuery(controlQuery, Result::next);
        assertEquals((Double) node.get("cypherScore"), (double) node.get("score"), 0.01);
    }

    @Test
    void multipleRelationshipsBetweenSameNodesAreIncluded() {
        String controlQuery =
                "MATCH (p1:Person {name: 'Praveena'})\n" +
                        "MATCH (p2:Person {name: 'Jennifer'})\n" +
                        "RETURN gds.alpha.linkprediction.preferentialAttachment(p1, p2) AS score, " +
                        "       16.0 AS cypherScore";

        Map<String, Object> node = runQuery(controlQuery, Result::next);
        assertEquals((Double) node.get("cypherScore"), (double) node.get("score"), 0.01);
    }

    @Test
    void multipleRelationshipsOfSpecificTypeBetweenSameNodesAreIncluded() {
        String controlQuery =
                "MATCH (p1:Person {name: 'Praveena'})\n" +
                        "MATCH (p2:Person {name: 'Jennifer'})\n" +
                        "RETURN gds.alpha.linkprediction.preferentialAttachment(p1, p2, {relationshipQuery: 'FRIENDS'}) AS score, " +
                        "      2.0 AS cypherScore";

        Map<String, Object> node = runQuery(controlQuery, Result::next);
        assertEquals((Double) node.get("cypherScore"), (double) node.get("score"), 0.01);
    }

    @Test
    void directionIsConsidered() {
        String controlQuery =
                "MATCH (p1:Person {name: 'Praveena'})\n" +
                        "MATCH (p2:Person {name: 'Jennifer'})\n" +
                        "RETURN gds.alpha.linkprediction.preferentialAttachment(p1, p2, " +
                        "      {relationshipQuery: 'FOLLOWS', direction: 'OUTGOING'}) AS score, " +
                        "      1.0 AS cypherScore";

        Map<String, Object> node = runQuery(controlQuery, Result::next);
        assertEquals((Double) node.get("cypherScore"), (double) node.get("score"), 0.01);
    }
}
