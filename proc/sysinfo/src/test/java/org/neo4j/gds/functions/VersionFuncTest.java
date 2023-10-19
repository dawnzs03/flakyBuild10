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
package org.neo4j.gds.functions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.gds.BaseProcTest;
import org.neo4j.gds.BuildInfoProperties;

import java.io.IOException;
import java.util.List;
import java.util.Map;

class VersionFuncTest extends BaseProcTest {

    @BeforeEach
    void setup() throws Exception {
        registerFunctions(VersionFunc.class);
    }

    @Test
    void shouldReturnGradleVersion() throws IOException {
        assertCypherResult(
            "RETURN gds.version() AS v",
            List.of(Map.of("v", BuildInfoProperties.get().gdsVersion()))
        );
    }
}
