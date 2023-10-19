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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.gds.annotation.Configuration;
import org.neo4j.gds.api.DatabaseId;
import org.neo4j.gds.api.schema.GraphSchema;
import org.neo4j.gds.api.schema.MutableNodeSchema;
import org.neo4j.gds.core.CypherMapWrapper;
import org.neo4j.gds.core.huge.DirectIdMap;
import org.neo4j.gds.core.loading.Capabilities.WriteMode;
import org.neo4j.gds.core.loading.GraphStoreBuilder;
import org.neo4j.gds.core.loading.ImmutableNodes;
import org.neo4j.gds.core.loading.ImmutableStaticCapabilities;
import org.neo4j.gds.core.loading.RelationshipImportResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class WriteConfigTest {

    @ParameterizedTest
    @EnumSource(WriteMode.class)
    void validateGraphStoreCapabilities(WriteMode writeMode) {
        var config = CypherMapWrapper.empty();
        var testConfig = new TestWriteConfigImpl(config);

        var nodes = ImmutableNodes.builder()
            .idMap(new DirectIdMap(0))
            .schema(MutableNodeSchema.empty())
            .build();

        var testGraphStore = new GraphStoreBuilder()
            .databaseId(DatabaseId.from("neo4j"))
            .capabilities(ImmutableStaticCapabilities.of(writeMode))
            .schema(GraphSchema.mutable())
            .nodes(nodes)
            .relationshipImportResult(RelationshipImportResult.of(Map.of()))
            .concurrency(1)
            .build();

        var assertion = assertThatCode(() -> testConfig.validateGraphIsSuitableForWrite(
            testGraphStore,
            List.of(),
            List.of()
        ));

        if (testGraphStore.capabilities().canWriteToDatabase() || testGraphStore.capabilities().canWriteToRemoteDatabase()) {
            assertion.doesNotThrowAnyException();
        } else {
            assertion
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The provided graph does not support `write` execution mode.");
        }
    }

    @Test
    void shouldParseArrowConnectionInfo() {
        var bearerToken = UUID.randomUUID().toString();
        var cypherMap = CypherMapWrapper.create(Map.of(
           "arrowConnectionInfo",
            Map.of(
               "hostname", "localhost",
                "port", 4242,
                "bearerToken", bearerToken,
                "useEncryption", false
            )
        ));

        var config = new TestWriteConfigImpl(cypherMap);
        var arrowConnectionInfo = config.arrowConnectionInfo();
        assertThat(arrowConnectionInfo).isPresent();
        assertThat(arrowConnectionInfo.get().hostname()).isEqualTo("localhost");
        assertThat(arrowConnectionInfo.get().port()).isEqualTo(4242);
        assertThat(arrowConnectionInfo.get().bearerToken()).isEqualTo(bearerToken);
    }

    static Stream<Arguments> baseConfigs() {
        return Stream.of(
            Arguments.of(TestWriteConfigImpl
                .builder()
                .arrowConnectionInfo(Optional.of(ImmutableArrowConnectionInfo.of(
                    "localhost",
                    4242,
                    UUID.randomUUID().toString(),
                    false
                )))
                .concurrency(2)
                .build()),
            Arguments.of(TestWriteConfigImpl
                .builder()
                .concurrency(2)
                .build()
            )
        );
    }

    @ParameterizedTest
    @MethodSource("baseConfigs")
    void shouldCreateFromExistingConfig(TestWriteConfig baseConfig) {
        assertThatCode(() -> TestWriteConfigImpl.Builder.from(baseConfig).build()).doesNotThrowAnyException();
    }

    @Configuration
    interface TestWriteConfig extends WriteConfig {
    }
}
