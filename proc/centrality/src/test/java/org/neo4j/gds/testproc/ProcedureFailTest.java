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
package org.neo4j.gds.testproc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.gds.BaseProcTest;
import org.neo4j.gds.catalog.GraphProjectProc;
import org.neo4j.gds.extension.Neo4jGraph;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ProcedureFailTest extends BaseProcTest {

    @Neo4jGraph
    public static final String DB_CYPHER = "CREATE (a:N)";

    @BeforeEach
    void setup() throws Exception {
        registerProcedures(ProcedureThatFailsDuringTask.class, GraphProjectProc.class);
    }

    @Test
    void shouldFailWithIllegalStateException() {
        loadCompleteGraph(DEFAULT_GRAPH_NAME);
        assertThatThrownBy(() -> runQuery("CALL very.strange.procedure('" + DEFAULT_GRAPH_NAME + "', {})"))
            .getRootCause()
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Oops");
    }

}
