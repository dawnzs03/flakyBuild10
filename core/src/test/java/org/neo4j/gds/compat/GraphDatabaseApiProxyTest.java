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
package org.neo4j.gds.compat;

import org.junit.jupiter.api.Test;
import org.neo4j.gds.BaseTest;

import static org.assertj.core.api.Assertions.assertThatCode;

class GraphDatabaseApiProxyTest extends BaseTest {

    @Test
    void shouldCloseProperly() {
        // if this test runs within a minute, it should fail

        // create a number of new transactions and close them
        for (int i = 0; i < 7; i++) {
            GraphDatabaseApiProxy.newKernelTransaction(db).close();
        }

        // now watch it *not* go boom 🎉
        assertThatCode(() -> {
            GraphDatabaseApiProxy.newKernelTransaction(db).close();
        }).doesNotThrowAnyException();
    }
}
