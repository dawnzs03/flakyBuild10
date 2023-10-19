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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.neo4j.kernel.internal.CustomVersionSetting;
import org.neo4j.kernel.internal.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class Neo4jVersionTest {
    private static final String CUSTOM_VERSION_SETTING = CustomVersionSetting.getConfigKey();

    @ParameterizedTest
    @CsvSource({
        "4.4.0-drop04, V_4_4",
        "4.4.0, V_4_4",
        "4.4.14, V_4_4",
        "5.1.0, V_5_1",
        "5.2.0, V_5_2",
        "5.1.0-dev, V_5_1",
        "5.2.0-dev, V_5_2",
        "5.2.0, V_5_2",
        "5.3.0, V_5_3",
        "5.4.0, V_5_4",
        "5.5.0, V_5_5",
        "5.6.0, V_5_6",
        "5.7.0, V_5_7",
        "5.8.0, V_5_8",
        "5.9.0, V_5_9",
        "5.10.0, V_5_10",
        "5.11.0, V_Dev",
    })
    void testParse(String input, Neo4jVersion expected) {
        assertEquals(expected.name(), Neo4jVersion.parse(input).name());
    }

    @ParameterizedTest
    @CsvSource({
        "dev",
        "4.3", // EOL
        "5.dev",
        "dev.5",
        "5.0", // 5.0 was never released to the public
        "dev.5.dev.1",
        "5",
        "6.0.0",
    })
    void testParseInvalid(String input) {
        assertThatThrownBy(() -> Neo4jVersion.parse(input))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("Cannot run on Neo4j Version " + input);
    }

    @Test
    void shouldNotRespectVersionOverride() {
        System.setProperty(Neo4jVersionTest.CUSTOM_VERSION_SETTING, "foobidoobie");
        assertNotEquals(Version.getNeo4jVersion(), Neo4jVersion.neo4jVersion());
    }

    @ParameterizedTest
    @CsvSource({
        "4.4.0-drop04, 4, 4",
        "4.4.14, 4, 4",
        "5.1.0, 5, 1",
        "5.2.0, 5, 2",
        "5.3.0, 5, 3",
        "5.4.0, 5, 4",
        "5.5.0, 5, 5",
        "5.6.0, 5, 6",
        "5.7.0, 5, 7",
        "5.8.0, 5, 8",
    })
    void semanticVersion(String input, int expectedMajor, int expectedMinor) {
        Neo4jVersion version = Neo4jVersion.parse(input);

        assertThat(version.semanticVersion()).isEqualTo(ImmutableMajorMinorVersion.of(expectedMajor, expectedMinor));
    }
}
