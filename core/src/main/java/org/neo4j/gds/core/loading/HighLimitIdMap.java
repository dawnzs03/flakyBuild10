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
import org.neo4j.gds.api.IdMapAdapter;
import org.neo4j.gds.core.utils.paged.ShardedLongLongMap;

import java.util.Collection;
import java.util.Optional;

public class HighLimitIdMap extends IdMapAdapter {

    private final ShardedLongLongMap highToLowIdSpace;

    public HighLimitIdMap(ShardedLongLongMap intermediateIdMap, IdMap internalIdMap) {
        super(internalIdMap);
        this.highToLowIdSpace = intermediateIdMap;
    }

    @Override
    public String typeId() {
        return HighLimitIdMapBuilder.ID + "-" + super.typeId();
    }

    @Override
    public long toOriginalNodeId(long mappedNodeId) {
        return highToLowIdSpace.toOriginalNodeId(super.toOriginalNodeId(mappedNodeId));
    }

    @Override
    public long toMappedNodeId(long originalNodeId) {
        var mappedNodeId = this.highToLowIdSpace.toMappedNodeId(originalNodeId);
        if (mappedNodeId == NOT_FOUND) {
            return NOT_FOUND;
        }
        return super.toMappedNodeId(mappedNodeId);
    }

    @Override
    public boolean containsOriginalId(long originalNodeId) {
        var mappedNodeId = this.highToLowIdSpace.toMappedNodeId(originalNodeId);
        if (mappedNodeId == NOT_FOUND) {
            return false;
        }
        return super.containsOriginalId(mappedNodeId);
    }

    @Override
    public long highestOriginalId() {
        return highToLowIdSpace.maxOriginalId();
    }

    @Override
    public Optional<FilteredIdMap> withFilteredLabels(Collection<NodeLabel> nodeLabels, int concurrency) {
        return super.withFilteredLabels(nodeLabels, concurrency)
            .map(filteredIdMap -> new FilteredHighLimitIdMap(this.highToLowIdSpace, filteredIdMap));
    }

    public static boolean isHighLimitIdMap(String typeId) {
        return typeId.startsWith(HighLimitIdMapBuilder.ID);
    }

    public static Optional<String> innerTypeId(String typeId) {
        var separatorIndex = typeId.indexOf('-');
        if (isHighLimitIdMap(typeId) && separatorIndex > 0 && separatorIndex < typeId.length() - 1) {
            var substring = typeId.substring(separatorIndex + 1);
            return substring.equals(HighLimitIdMapBuilder.ID) ? Optional.empty() : Optional.of(substring);
        }
        return Optional.empty();
    }

    static final class FilteredHighLimitIdMap extends HighLimitIdMap implements FilteredIdMap {

        private final FilteredIdMap filteredIdMap;

        FilteredHighLimitIdMap(ShardedLongLongMap intermediateIdMap, FilteredIdMap filteredIdMap) {
            super(intermediateIdMap, filteredIdMap);
            this.filteredIdMap = filteredIdMap;
        }

        @Override
        public long toFilteredNodeId(long rootNodeId) {
            return filteredIdMap.toFilteredNodeId(rootNodeId);
        }

        @Override
        public long toRootNodeId(long mappedNodeId) {
            return filteredIdMap.toRootNodeId(mappedNodeId);
        }

        @Override
        public boolean containsRootNodeId(long rootNodeId) {
            return filteredIdMap.containsRootNodeId(rootNodeId);
        }
    }
}
