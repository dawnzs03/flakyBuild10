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
package org.neo4j.gds.catalog;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.core.loading.CSRGraphStore;
import org.neo4j.gds.core.loading.GraphStoreCatalog;
import org.neo4j.gds.core.loading.GraphStoreWithConfig;
import org.neo4j.gds.core.loading.ImmutableCatalogRequest;
import org.neo4j.gds.executor.ExecutionContext;
import org.neo4j.gds.mem.MemoryUsage;
import org.openjdk.jol.info.GraphWalker;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public  final class GraphMemoryUsage {

    public final String graphName;
    public final String memoryUsage;
    public final long sizeInBytes;
    public final Map<String, Object> detailSizeInBytes;
    public final long nodeCount;
    public final long relationshipCount;
    static GraphMemoryUsage of(GraphStoreWithConfig graphStoreWithConfig){
        var totalSize = new MutableLong();
        var graphStore = graphStoreWithConfig.graphStore();
        var detailMemory = internalSizeOfGraph(graphStore, totalSize);

        var memoryUsage = MemoryUsage.humanReadable(totalSize.longValue());

        return new GraphMemoryUsage(
            graphStoreWithConfig.config().graphName(),
            memoryUsage,
            totalSize.longValue(),
            detailMemory,
            graphStore.nodeCount(),
            graphStore.relationshipCount()
        );
    }

    static GraphMemoryUsage of(String graphName, ExecutionContext executionContext) {
        graphName = Objects.requireNonNull(StringUtils.trimToNull(graphName), "graphName must not be empty");
        var graphStoreWithConfig = graphStoreFromCatalog(graphName,executionContext);
        return  of(graphStoreWithConfig);
    }

    private static final Pattern ADJ_DEGREES = Pattern.compile("^.relationships.table\\[\\d+].value.degrees.*$");
    private static final Pattern ADJ_LIST = Pattern.compile("^.relationships.table\\[\\d+].value.list.*$");
    private static final Pattern ADJ_OFFSETS = Pattern.compile("^.relationships.table\\[\\d+].value.offsets.*$");
    private static final Pattern DOT = Pattern.compile("\\.");
    private static final Object DUMMY = new Object();

    private static Map<String, Object> internalSizeOfGraph(GraphStore graphStore, MutableLong totalSize) {
        if (MemoryUsage.sizeOf(DUMMY) == -1L) {
            return Map.of();
        }
        var mappingSparseLongArray = new MutableLong();
        var mappingForward = new MutableLong();
        var mappingBackward = new MutableLong();
        var nodesTotal = new MutableLong();
        var adjacencyDegrees = new MutableLong();
        var adjacencyOffsets = new MutableLong();
        var adjacencyLists = new MutableLong();
        var relationshipsTotal = new MutableLong();

        var graphWalker = new GraphWalker(gpr -> {
            var size = gpr.size();
            var path = gpr.path();

            var firstField = DOT.splitAsStream(path)
                .skip(1)
                .findFirst()
                .orElse("");

            if ("nodes".equals(firstField)) {
                nodesTotal.add(size);
            } else if ("relationships".equals(firstField)) {
                relationshipsTotal.add(size);
            }

            if (path.startsWith(".nodes.sparseLongArray")) {
                mappingSparseLongArray.add(size);
            }
            if (path.startsWith(".nodes.graphIds")) {
                mappingForward.add(size);
            }
            if (path.startsWith(".nodes.nodeToGraphIds")) {
                mappingBackward.add(size);
            }
            if (ADJ_DEGREES.matcher(path).matches()) {
                adjacencyDegrees.add(size);
            }
            if (ADJ_LIST.matcher(path).matches()) {
                adjacencyLists.add(size);
            }
            if (ADJ_OFFSETS.matcher(path).matches()) {
                adjacencyOffsets.add(size);
            }

            totalSize.add(size);
        });

        graphWalker.walk(graphStore);

        var mappingTotal = mappingSparseLongArray.longValue() + mappingForward.longValue() + mappingBackward.longValue();
        var adjacencyTotal = adjacencyDegrees.longValue() + adjacencyOffsets.longValue() + adjacencyLists.longValue();

        var details = new HashMap<String, Object>();
        details.put("total", totalSize.longValue());
        details.put("nodes", Map.of(
            "sparseLongArray", mappingSparseLongArray.longValue(),
            "forwardMapping", mappingForward.longValue(),
            "backwardMapping", mappingBackward.longValue(),
            "mapping", mappingTotal,
            "total", nodesTotal.longValue()
        ));
        details.put("relationships", Map.of(
            "degrees", adjacencyDegrees.longValue(),
            "offsets", adjacencyOffsets.longValue(),
            "targetIds", adjacencyLists.longValue(),
            "adjacencyLists", adjacencyTotal,
            "total", relationshipsTotal.longValue()
        ));

        if (graphStore instanceof CSRGraphStore) {
            var adjacencyListDetails = new HashMap<String, Object>();
            var csrGraphStore = ((CSRGraphStore) graphStore);
            var unionGraph = csrGraphStore.getUnion();
            unionGraph.relationshipTopologies().forEach((relationshipType, adjacency) -> {
                var memoryInfo = adjacency.adjacencyList().memoryInfo();
                var listDetail = Map.of(
                    "pages", memoryInfo.pages(),
                    "bytesTotal", memoryInfo.bytesTotal().orElse(0),
                    "bytesOnHeap", memoryInfo.bytesOnHeap().orElse(0),
                    "bytesOffHeap", memoryInfo.bytesOffHeap().orElse(0),
                    "pageSizes", memoryInfo.pageSizes().toMap(),
                    "heapAllocations", memoryInfo.heapAllocations().toMap(),
                    "nativeAllocations", memoryInfo.nativeAllocations().toMap(),
                    "headerAllocations", memoryInfo.headerAllocations().toMap(),
                    "headerBits", memoryInfo.headerBits().toMap(),
                    "blockStatistics", memoryInfo.blockStatistics().map(stats -> {
                        Map<Object, Object> blockStats = new HashMap<>();
                        blockStats.put("blockCount", stats.blockCount());
                        blockStats.put("blockLengths", stats.blockLengths().toMap());
                        blockStats.put("indexOfMaxValue", stats.indexOfMaxValue().toMap());
                        blockStats.put("indexOfMinValue", stats.indexOfMinValue().toMap());
                        blockStats.put("maxBits", stats.maxBits().toMap());
                        blockStats.put("minBits", stats.minBits().toMap());
                        blockStats.put("meanBits", stats.meanBits().toMap());
                        blockStats.put("medianBits", stats.medianBits().toMap());
                        blockStats.put("stdDevBits", stats.stdDevBits().toMap());
                        blockStats.put("headTailDiffBits", stats.headTailDiffBits().toMap());
                        blockStats.put("bestMaxDiffBits", stats.bestMaxDiffBits().toMap());
                        blockStats.put("exceptions", stats.exceptions().toMap());
                        return blockStats;
                    }).orElse(Map.of())
                );
                adjacencyListDetails.put(relationshipType.name(), listDetail);
            });
            details.put("adjacencyLists", adjacencyListDetails);
        }
        return Collections.unmodifiableMap(details);
    }

    private static GraphStoreWithConfig graphStoreFromCatalog(String graphName, ExecutionContext executionContext) {
        var catalogRequest = ImmutableCatalogRequest.of(
            executionContext.databaseId().databaseName(),
            executionContext.username(),
            Optional.empty(),
            executionContext.isGdsAdmin()
        );
        return GraphStoreCatalog.get(catalogRequest, graphName);
    }

    GraphMemoryUsage(
        String graphName,
        String memoryUsage,
        long sizeInBytes,
        Map<String, Object> detailSizeInBytes,
        long nodeCount,
        long relationshipCount
    ) {
        this.graphName = graphName;
        this.memoryUsage = memoryUsage;
        this.sizeInBytes = sizeInBytes;
        this.detailSizeInBytes = detailSizeInBytes;
        this.nodeCount = nodeCount;
        this.relationshipCount = relationshipCount;
    }
}
