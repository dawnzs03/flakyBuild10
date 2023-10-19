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
package org.neo4j.gds.doc;

import org.immutables.value.Value;
import org.neo4j.gds.annotation.ValueClass;
import org.neo4j.gds.doc.syntax.DocQuery;

import java.util.List;

/**
 * Query examples are specified in two sections:
 *
 * <pre>
 * [role=query-example]
 * --
 * .Header text goes here:
 * [source, cypher]
 * ----
 * MATCH (n) RETURN n.a AS a, n.b AS b
 * ----
 * .Results
 * [opts="header"]
 * |===
 * | a        | b
 * | "Alice"  | 7
 * | "Bob"    | 11
 * |===
 * --
 * </pre>
 *
 * The sections are turned into the query and assertions about the query result, respectively, and they then get turned
 * into a single test case.
 */
@ValueClass
public interface QueryExample extends DocQuery {

    String query();

    @Value.Default
    default List<String> resultColumns() {
        return List.of();
    }

    @Value.Default
    default List<List<String>> results() {
        return List.of();
    }

    @Value.Default
    default boolean assertResults() {
        return true;
    }


    @Value.Derived
    default boolean runAsOperator() {
        return !operator().equals("");
    }

    @Value.Default
    default String operator() {
        return "";
    }

    static ImmutableQueryExample.Builder builder() {
        return ImmutableQueryExample.builder();
    }
}
