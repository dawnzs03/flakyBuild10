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
package org.neo4j.gds.catalog;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GraphListProcTest {
    @Test
    void shouldDelegateToFacade() {
        var facade = mock(GraphStoreCatalogProcedureFacade.class);
        var procedure = new GraphListProc(facade);

        var expectedResultStream = Stream.of(mock(GraphInfoWithHistogram.class));
        when(facade.listGraphs("some graph")).thenReturn(expectedResultStream);
        var actualResultStream = procedure.listGraphs("some graph");

        assertSame(expectedResultStream, actualResultStream);
    }
}
