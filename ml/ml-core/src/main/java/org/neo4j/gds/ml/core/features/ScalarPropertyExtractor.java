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
package org.neo4j.gds.ml.core.features;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.api.properties.nodes.NodePropertyValues;

import static org.neo4j.gds.utils.StringFormatting.formatWithLocale;

public class ScalarPropertyExtractor implements ScalarFeatureExtractor {
    private final Graph graph;
    private final String propertyKey;
    private final NodePropertyValues nodePropertyValues;

    ScalarPropertyExtractor(Graph graph, String propertyKey) {
        this.graph = graph;
        this.propertyKey = propertyKey;
        this.nodePropertyValues = graph.nodeProperties(propertyKey);
    }

    @Override
    public double extract(long nodeId) {
        var propertyValue = nodePropertyValues.doubleValue(nodeId);
        if (Double.isNaN(propertyValue)) {
            throw new IllegalArgumentException(formatWithLocale(
                "Node with ID `%d` has invalid feature property value `NaN` for property `%s`",
                graph.toOriginalNodeId(nodeId),
                propertyKey
            ));
        }
        return propertyValue;
    }
}
