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
package org.neo4j.gds.embeddings.hashgnn;

import org.junit.jupiter.api.Test;
import org.neo4j.gds.core.CypherMapWrapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HashGNNConfigTest {
    @Test
    void binarizationConfigCorrectType() {
        var config = HashGNNStreamConfigImpl
            .builder()
            .featureProperties(List.of("x"))
            .binarizeFeatures(Map.of("dimension", 100))
            .embeddingDensity(4)
            .iterations(100)
            .build();
        var map = config.toMap();
        assertThat(map.get("binarizeFeatures")).isInstanceOf(Map.class);
    }
    @Test
    void shouldNotAllowGeneratedAndFeatureProperties() {
        assertThatThrownBy(() -> {
            HashGNNStreamConfigImpl
                .builder()
                .featureProperties(List.of("x"))
                .generateFeatures(Map.of("dimension", 100, "densityLevel", 2))
                .embeddingDensity(4)
                .iterations(100)
                .build();
        }).hasMessage("It is not allowed to use `generateFeatures` and have non-empty `featureProperties`.");
    }

    @Test
    void requiresFeaturePropertiesIfNoGeneratedFeatures() {
        assertThatThrownBy(() -> {
            HashGNNStreamConfigImpl
                .builder()
                .embeddingDensity(4)
                .iterations(100)
                .build();
        }).hasMessage("When `generateFeatures` is not given, `featureProperties` must be non-empty.");
    }

    @Test
    void requiresDensityLevelAtMostDensity() {
        assertThatThrownBy(() -> {
            HashGNNStreamConfigImpl
                .builder()
                .embeddingDensity(4)
                .generateFeatures(Map.of("dimension", 4, "densityLevel", 5))
                .iterations(100)
                .build();
        }).hasMessage("Generate features requires `densityLevel` to be at most `dimension` but was 5 > 4.");
    }

    @Test
    void failsOnInvalidBinarizationKeys() {
        assertThatThrownBy(() -> {
            new HashGNNStreamConfigImpl(CypherMapWrapper.create(
                Map.of(
                    "mutateProperty", "foo",
                    "featureProperties", List.of("x"),
                    "binarizeFeatures", Map.of("dimension", 100, "treshold", 2.0),
                    "embeddingDensity", 4,
                    "iterations", 100
                )
            ));

        }).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unexpected configuration key: treshold (Did you mean [threshold]?)");
    }

    @Test
    void failsOnInvalidGenerateFeaturesKeys() {
        assertThatThrownBy(() -> {
            new HashGNNStreamConfigImpl(CypherMapWrapper.create(
                Map.of(
                    "generateFeatures", Map.of("dimension", 100, "densityElfen", 2),
                    "embeddingDensity", 4,
                    "iterations", 100
                )
            ));

        }).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No value specified for the mandatory configuration parameter `densityLevel` (a similar parameter exists: [densityElfen])");
    }
}
