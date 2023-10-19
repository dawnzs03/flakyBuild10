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
package org.neo4j.gds.results;

import org.neo4j.gds.core.GraphDimensions;
import org.neo4j.gds.core.utils.mem.MemoryRange;
import org.neo4j.gds.core.utils.mem.MemoryTree;
import org.neo4j.gds.core.utils.mem.MemoryTreeWithDimensions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public class MemoryEstimateResult {
    public final String requiredMemory;
    public final String treeView;
    public final Map<String, Object> mapView;
    public final long bytesMin, bytesMax;
    public long nodeCount, relationshipCount;
    public final double heapPercentageMin;
    public final double heapPercentageMax;

    public MemoryEstimateResult(MemoryTreeWithDimensions memory) {
        this(memory.memoryTree, memory.graphDimensions);
    }

    private MemoryEstimateResult(MemoryTree memory, GraphDimensions dimensions) {
        this(memory.render(), memory.renderMap(), memory.memoryUsage(), dimensions);
    }

    private MemoryEstimateResult(
        String treeView,
        Map<String, Object> mapView,
        MemoryRange estimateMemoryUsage,
        GraphDimensions dimensions
    ) {
        long heapSize = Runtime.getRuntime().maxMemory();
        this.requiredMemory = estimateMemoryUsage.toString();
        this.treeView = treeView;
        this.mapView = mapView;
        this.bytesMin = estimateMemoryUsage.min;
        this.bytesMax = estimateMemoryUsage.max;
        this.heapPercentageMin = getPercentage(bytesMin, heapSize);
        this.heapPercentageMax = getPercentage(bytesMax, heapSize);
        this.nodeCount = dimensions.nodeCount();
        this.relationshipCount = dimensions.relCountUpperBound();
    }

    private double getPercentage(long requiredBytes, long heapSizeBytes) {
        if (heapSizeBytes == 0) {
            return Double.NaN;
        }
        return BigDecimal.valueOf(requiredBytes)
            .divide(BigDecimal.valueOf(heapSizeBytes), 1, RoundingMode.UP)
            .doubleValue();
    }
}
