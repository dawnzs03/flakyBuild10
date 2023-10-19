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
package org.neo4j.gds.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class JobIdConfigTest {

    @Test
    void shouldAcceptValidJobId() {
        var configBuilder = JobIdConfigImpl.builder();

        configBuilder
            .jobId("df16706f-0fb7-4a85-bf1c-a2c6f3c1cf08")
            .build();

        configBuilder
            .jobId("banana-sweatshirt")
            .build();
    }

    @Test
    void shouldRejectInvalidJobId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> JobIdConfigImpl.builder().jobId(Long.valueOf(42L)).build()
        );
    }
}
