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

import org.immutables.value.Value;
import org.neo4j.gds.annotation.Configuration;
import org.neo4j.gds.config.AlgoBaseConfig;
import org.neo4j.gds.config.FeaturePropertiesConfig;
import org.neo4j.gds.config.RandomSeedConfig;
import org.neo4j.gds.core.CypherMapWrapper;

import java.util.Map;
import java.util.Optional;

interface HashGNNConfig extends AlgoBaseConfig, FeaturePropertiesConfig, RandomSeedConfig {

    @Configuration.IntegerRange(min = 1)
    int iterations();

    @Configuration.IntegerRange(min = 1)
    int embeddingDensity();

    @Configuration.IntegerRange(min = 1)
    Optional<Integer> outputDimension();

    @Configuration.DoubleRange(min = 0)
    default double neighborInfluence() {
        return 1;
    }

    default boolean heterogeneous() {
        return false;
    }

    @Configuration.ToMapValue("org.neo4j.gds.embeddings.hashgnn.HashGNNConfig#toMapGenerateFeaturesConfig")
    @Configuration.ConvertWith(method = "parseGenerateFeaturesConfig", inverse = Configuration.ConvertWith.INVERSE_IS_TO_MAP)
    Optional<GenerateFeaturesConfig> generateFeatures();

    @Configuration.ToMapValue("org.neo4j.gds.embeddings.hashgnn.HashGNNConfig#toMapBinarizationConfig")
    @Configuration.ConvertWith(method = "parseBinarizationConfig", inverse = Configuration.ConvertWith.INVERSE_IS_TO_MAP)
    Optional<BinarizeFeaturesConfig> binarizeFeatures();

    @Value.Check
    default void validate() {
        if (!featureProperties().isEmpty() && generateFeatures().isPresent()) {
            throw new IllegalArgumentException("It is not allowed to use `generateFeatures` and have non-empty `featureProperties`.");
        }
        if (generateFeatures().isPresent()) return;
        if (featureProperties().isEmpty()) {
            throw new IllegalArgumentException("When `generateFeatures` is not given, `featureProperties` must be non-empty.");
        }
    }

    static BinarizeFeaturesConfig parseBinarizationConfig(Map<String, Object> parameter) {
        var cypherMapWrapper = CypherMapWrapper.create(parameter);
        var binarizeFeaturesConfig = new BinarizeFeaturesConfigImpl(cypherMapWrapper);
        cypherMapWrapper.requireOnlyKeysFrom(binarizeFeaturesConfig.configKeys());
        return binarizeFeaturesConfig;
    }

    static Map<String, Object> toMapBinarizationConfig(BinarizeFeaturesConfig config) {
        return config.toMap();
    }

    static GenerateFeaturesConfig parseGenerateFeaturesConfig(Map<String, Object> parameter) {
        var cypherMapWrapper = CypherMapWrapper.create(parameter);
        var generateFeaturesConfig = new GenerateFeaturesConfigImpl(cypherMapWrapper);
        cypherMapWrapper.requireOnlyKeysFrom(generateFeaturesConfig.configKeys());
        return generateFeaturesConfig;
    }

    static Map<String, Object> toMapGenerateFeaturesConfig(GenerateFeaturesConfig config) {
        return config.toMap();
    }

}
