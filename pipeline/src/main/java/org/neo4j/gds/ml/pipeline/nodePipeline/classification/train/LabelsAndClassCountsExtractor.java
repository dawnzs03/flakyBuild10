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
package org.neo4j.gds.ml.pipeline.nodePipeline.classification.train;

import org.neo4j.gds.annotation.ValueClass;
import org.neo4j.gds.api.properties.nodes.NodePropertyValues;
import org.neo4j.gds.collections.LongMultiSet;
import org.neo4j.gds.core.utils.paged.HugeIntArray;
import org.neo4j.gds.ml.core.subgraph.LocalIdMap;

public final class LabelsAndClassCountsExtractor {

    private LabelsAndClassCountsExtractor() {}

    static LabelsAndClassCounts extractLabelsAndClassCounts(
        NodePropertyValues targetNodeProperty,
        long nodeCount
    ) {
        var labels = HugeIntArray.newArray(nodeCount);
        var classCounts = extractClassCounts(targetNodeProperty, nodeCount);
        var localIdMap = LocalIdMap.ofSorted(classCounts.keys());

        for (long nodeId = 0; nodeId < nodeCount; nodeId++) {
            labels.set(nodeId, localIdMap.toMapped(targetNodeProperty.longValue(nodeId)));
        }
        return ImmutableLabelsAndClassCounts.of(labels, classCounts);
    }

    static LongMultiSet extractClassCounts(
        NodePropertyValues targetNodeProperty,
        long nodeCount
    ) {
        var classCounts = new LongMultiSet();
        for (long nodeId = 0; nodeId < nodeCount; nodeId++) {
            classCounts.add(targetNodeProperty.longValue(nodeId));
        }
        return classCounts;
    }

    @ValueClass
    interface LabelsAndClassCounts {
        HugeIntArray labels();

        LongMultiSet classCounts();
    }
}
