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

public class ArrayPropertyExtractor implements ArrayFeatureExtractor {
    private final int dimension;
    private final Graph graph;
    private final String propertyKey;
    private final NodePropertyValues nodePropertyValues;

    ArrayPropertyExtractor(int dimension, Graph graph, String propertyKey) {
        this.dimension = dimension;
        this.graph = graph;
        this.propertyKey = propertyKey;
        this.nodePropertyValues = graph.nodeProperties(propertyKey);
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public double[] extract(long nodeId) {
        var propertyValue = nodePropertyValues.doubleArrayValue(nodeId);
        if (propertyValue == null) {
            throw new IllegalArgumentException(formatWithLocale(
                "Missing node property for property key `%s` on node with id `%s`. Consider using a default value in the property projection.",
                propertyKey,
                graph.toOriginalNodeId(nodeId)
            ));
        }
        if (propertyValue.length != dimension) {
            throw new IllegalArgumentException(formatWithLocale(
                "The property `%s` contains arrays of differing lengths `%s` and `%s`.",
                propertyKey,
                propertyValue.length,
                dimension
            ));
        }
        //TODO: this is slower than before. we should consider property validation to beginning of predict and train
        for (double val : propertyValue) {
            if (Double.isNaN(val)) {
                throw new IllegalArgumentException(
                    formatWithLocale("Node with ID `%d` has invalid feature property value NaN for property `%s`",
                        graph.toOriginalNodeId(nodeId),
                        propertyKey
                    )
                );
            }
        }
        return propertyValue;
    }
}
