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
package org.neo4j.gds.core.loading;

import org.neo4j.gds.NodeLabel;
import org.neo4j.gds.api.FilteredIdMap;
import org.neo4j.gds.api.IdMap;
import org.neo4j.gds.api.LabeledIdMap;

import java.util.List;
import java.util.OptionalLong;

public class FilteredLabeledIdMap extends LabeledIdMap implements FilteredIdMap {

    private final IdMap originalToRootIdMap;
    private final IdMap rootToFilteredIdMap;

    public FilteredLabeledIdMap(IdMap originalToRootIdMap, LabeledIdMap rootToFilteredIdMap) {
        super(rootToFilteredIdMap.labelInformation(), rootToFilteredIdMap.nodeCount());
        this.originalToRootIdMap = originalToRootIdMap;
        this.rootToFilteredIdMap = rootToFilteredIdMap;
    }

    @Override
    public String typeId() {
        return originalToRootIdMap.typeId();
    }

    @Override
    public OptionalLong rootNodeCount() {
        return originalToRootIdMap.rootNodeCount();
    }

    @Override
    public long toRootNodeId(long filteredNodeId) {
        return rootToFilteredIdMap.toOriginalNodeId(filteredNodeId);
    }

    @Override
    public long toFilteredNodeId(long rootNodeId) {
        return rootToFilteredIdMap.toMappedNodeId(rootNodeId);
    }

    @Override
    public long toOriginalNodeId(long filteredNodeId) {
        return originalToRootIdMap.toOriginalNodeId(rootToFilteredIdMap.toOriginalNodeId(filteredNodeId));
    }

    @Override
    public long toMappedNodeId(long originalNodeId) {
        return rootToFilteredIdMap.toMappedNodeId(originalToRootIdMap.toMappedNodeId(originalNodeId));
    }

    @Override
    public boolean containsOriginalId(long originalNodeId) {
        return rootToFilteredIdMap.containsOriginalId(originalToRootIdMap.toMappedNodeId(originalNodeId));
    }

    @Override
    public long highestOriginalId() {
        return originalToRootIdMap.highestOriginalId();
    }

    @Override
    public boolean containsRootNodeId(long rootNodeId) {
        return rootToFilteredIdMap.containsOriginalId(rootNodeId);
    }

    @Override
    public IdMap rootIdMap() {
        return originalToRootIdMap;
    }

    @Override
    public List<NodeLabel> nodeLabels(long filteredNodeId) {
        return originalToRootIdMap.nodeLabels(rootToFilteredIdMap.toOriginalNodeId(filteredNodeId));
    }

    @Override
    public void forEachNodeLabel(long filteredNodeId, NodeLabelConsumer consumer) {
        originalToRootIdMap.forEachNodeLabel(rootToFilteredIdMap.toOriginalNodeId(filteredNodeId), consumer);
    }

    @Override
    public boolean hasLabel(long filteredNodeId, NodeLabel label) {
        return originalToRootIdMap.hasLabel(rootToFilteredIdMap.toOriginalNodeId(filteredNodeId), label);
    }

    @Override
    public void addNodeLabel(NodeLabel nodeLabel) {
        originalToRootIdMap.addNodeLabel(nodeLabel);
    }

    @Override
    public final void addNodeIdToLabel(long filteredNodeId, NodeLabel nodeLabel) {
        originalToRootIdMap.addNodeIdToLabel(rootToFilteredIdMap.toOriginalNodeId(filteredNodeId), nodeLabel);
    }
}
