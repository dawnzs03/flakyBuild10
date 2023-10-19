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
package org.neo4j.gds.scaling;

import org.neo4j.gds.api.properties.nodes.DoubleArrayNodePropertyValues;
import org.neo4j.gds.api.properties.nodes.NodePropertyValues;
import org.neo4j.gds.core.utils.paged.HugeObjectArray;
import org.neo4j.gds.executor.ComputationResult;
import org.neo4j.gds.scaleproperties.ScaleProperties;
import org.neo4j.gds.scaleproperties.ScalePropertiesBaseConfig;

public final class ScalePropertiesProc {

    private ScalePropertiesProc() {}

    static final String SCALE_PROPERTIES_DESCRIPTION = "Scale node properties";

    static NodePropertyValues nodeProperties(
        ComputationResult<ScaleProperties, ScaleProperties.Result, ? extends ScalePropertiesBaseConfig> computationResult
    ) {
        var size = computationResult.graph().nodeCount();
        var scaledProperties = computationResult.result()
            .map(ScaleProperties.Result::scaledProperties)
            .orElseGet(() -> HugeObjectArray.newArray(double[].class, 0));

        return new ScaledNodePropertyValues(size, scaledProperties);
    }

    static NodePropertyValues nodeProperties(long size, HugeObjectArray<double[]> scaledProperties) {
        return new ScaledNodePropertyValues(size, scaledProperties);
    }

    static void validateLegacyScalers(ScalePropertiesBaseConfig config, boolean allowL1L2Scalers) {
        var specifiedScaler = config.scaler().type();
        if (!allowL1L2Scalers && (specifiedScaler.equals(L1Norm.TYPE) || specifiedScaler.equals(L2Norm.TYPE))) {
            ScalerFactory.throwForInvalidScaler(specifiedScaler);
        }
    }

    static final class ScaledNodePropertyValues implements DoubleArrayNodePropertyValues {

        private final long size;
        private final HugeObjectArray<double[]> scaledProperties;

        private ScaledNodePropertyValues(long size, HugeObjectArray<double[]> scaledProperties) {
            this.size = size;
            this.scaledProperties = scaledProperties;
        }

        @Override
        public long nodeCount() {
            return size;
        }

        @Override
        public double[] doubleArrayValue(long nodeId) {
            return scaledProperties.get(nodeId);
        }
    }
}
