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
package org.neo4j.gds;

import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.singletonMap;
import static org.neo4j.gds.compat.GraphDatabaseApiProxy.runQueryWithoutClosingTheResult;

public class ListProc {

    @Context
    public KernelTransaction transaction;

    private static final String PROCEDURE_QUERY =
        " SHOW PROCEDURES " +
        " YIELD name, signature, description " +
        " WHERE (name STARTS WITH 'algo.' OR name STARTS WITH 'gds.')" +
        " AND name <> 'gds.list'" +
        " AND ($name IS NULL OR name CONTAINS $name) " +
        " RETURN name, signature, description, 'procedure' AS type " +
        " ORDER BY name";

    private static final String FUNCTION_QUERY =
        " SHOW FUNCTIONS " +
        " YIELD name, signature, description " +
        " WHERE (name STARTS WITH 'algo.' OR name STARTS WITH 'gds.') AND ($name IS NULL OR name CONTAINS $name) " +
        " RETURN name, signature, description, 'function' AS type " +
        " ORDER BY name";

    private static final String DESCRIPTION = "CALL gds.list - lists all algorithm procedures, their description and signature";

    @Procedure("gds.list")
    @Description(DESCRIPTION)
    public Stream<ListResult> list(@Name(value = "name", defaultValue = "") String name) {
        return Stream.concat(
            runQueryWithoutClosingTheResult(transaction, PROCEDURE_QUERY, singletonMap("name", name)).stream(),
            runQueryWithoutClosingTheResult(transaction, FUNCTION_QUERY, singletonMap("name", name)).stream()
        ).map(ListResult::new);
    }

    @SuppressWarnings("unused")
    public static class ListResult {
        public String name;
        public String description;
        public String signature;
        public String type;

        public ListResult(Map<String, Object> row) {
            this.name = (String) row.get("name");
            this.description = (String) row.get("description");
            this.signature = (String) row.get("signature");
            this.type = (String) row.get("type");
        }
    }
}
