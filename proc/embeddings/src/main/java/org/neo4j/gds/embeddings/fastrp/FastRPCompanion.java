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
package org.neo4j.gds.embeddings.fastrp;

import org.neo4j.gds.api.properties.nodes.EmptyFloatArrayNodePropertyValues;
import org.neo4j.gds.api.properties.nodes.FloatArrayNodePropertyValues;
import org.neo4j.gds.api.properties.nodes.NodePropertyValues;
import org.neo4j.gds.core.utils.paged.HugeObjectArray;
import org.neo4j.gds.executor.ComputationResult;

final class FastRPCompanion {

    static final String DESCRIPTION = "Random Projection produces node embeddings via the fastrp algorithm";

    private FastRPCompanion() {}

    static <CONFIG extends FastRPBaseConfig> NodePropertyValues nodeProperties(ComputationResult<FastRP, FastRP.FastRPResult, CONFIG> computationResult) {
        return computationResult.result()
            .map(result -> (FloatArrayNodePropertyValues) new EmbeddingNodePropertyValues(result.embeddings()))
            .orElse(EmptyFloatArrayNodePropertyValues.INSTANCE);
    }

    static NodePropertyValues nodeProperties(FastRP.FastRPResult result) {
        return new EmbeddingNodePropertyValues(result.embeddings());
    }

    private static class EmbeddingNodePropertyValues implements FloatArrayNodePropertyValues {
        private final HugeObjectArray<float[]> embeddings;
        private final long nodeCount;

        EmbeddingNodePropertyValues(HugeObjectArray<float[]> embeddings) {
            this.embeddings = embeddings;
            nodeCount = embeddings.size();
        }

        @Override
        public float[] floatArrayValue(long nodeId) {
            return embeddings.get(nodeId);
        }

        @Override
        public long nodeCount() {
            return nodeCount;
        }
    }
}
