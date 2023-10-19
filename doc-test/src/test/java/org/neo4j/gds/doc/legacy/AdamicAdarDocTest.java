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
package org.neo4j.gds.doc.legacy;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.gds.BaseProcTest;
import org.neo4j.gds.linkprediction.LinkPredictionFunc;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdamicAdarDocTest extends BaseProcTest {

    private static final String NL = System.lineSeparator();

    private static final String DB_CYPHER = "CREATE " +
        " (zhen:Person {name: 'Zhen'})," +
        " (praveena:Person {name: 'Praveena'})," +
        " (michael:Person {name: 'Michael'})," +
        " (arya:Person {name: 'Arya'})," +
        " (karin:Person {name: 'Karin'})," +

        " (zhen)-[:FRIENDS]->(arya)," +
        " (zhen)-[:FRIENDS]->(praveena)," +
        " (praveena)-[:WORKS_WITH]->(karin)," +
        " (praveena)-[:FRIENDS]->(michael)," +
        " (michael)-[:WORKS_WITH]->(karin)," +
        " (arya)-[:FRIENDS]->(karin)";

    @BeforeEach
    void setup() throws Exception {
        runQuery(DB_CYPHER);
        registerFunctions(LinkPredictionFunc.class);
    }

    @Test
    void functionOnAllRels() {
        @Language("Cypher")
        String query = " MATCH (p1:Person {name: 'Michael'})" +
                       " MATCH (p2:Person {name: 'Karin'})" +
                       " RETURN floor(gds.alpha.linkprediction.adamicAdar(p1, p2) * 1e11) / 1e11 AS score";

        String expectedString = "+---------------+" + NL +
                                "| score         |" + NL +
                                "+---------------+" + NL +
                                "| 0.91023922662 |" + NL +
                                "+---------------+" + NL +
                                "1 row" + NL;

        runQueryWithResultConsumer(query, result -> assertEquals(expectedString, result.resultAsString()));
    }

    @Test
    void functionOnFriendRels() {
        @Language("Cypher")
        String query = " MATCH (p1:Person {name: 'Michael'})" +
                       " MATCH (p2:Person {name: 'Karin'})" +
                       " RETURN gds.alpha.linkprediction.adamicAdar(p1, p2, {relationshipQuery: 'FRIENDS'}) AS score";

        String expectedString = "+-------+" + NL +
                                "| score |" + NL +
                                "+-------+" + NL +
                                "| 0.0   |" + NL +
                                "+-------+" + NL +
                                "1 row" + NL;

        runQueryWithResultConsumer(query, result -> assertEquals(expectedString, result.resultAsString()));
    }
}
