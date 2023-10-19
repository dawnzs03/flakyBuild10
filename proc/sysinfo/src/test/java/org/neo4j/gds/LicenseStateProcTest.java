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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class LicenseStateProcTest extends BaseProcTest {

    @BeforeEach
    void setUp() throws Exception {
        registerProcedures(LicenseStateProc.class);
        registerFunctions(LicenseStateProc.class);
    }

    @Test
    void returnState() {
        assertCypherResult("CALL gds.license.state()", List.of(Map.of("isLicensed", false, "details", "No valid GDS license specified.")));
    }

    @Test
    void returnIsLicensed() {
        assertCypherResult("RETURN gds.isLicensed() AS isLicensed", List.of(Map.of("isLicensed", false)));
    }

}
