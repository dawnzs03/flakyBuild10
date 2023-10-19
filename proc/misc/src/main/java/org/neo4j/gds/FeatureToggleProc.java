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
package org.neo4j.gds;

import org.neo4j.gds.utils.GdsFeatureToggles;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Internal;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.neo4j.gds.utils.StringFormatting.formatWithLocale;
import static org.neo4j.gds.utils.StringFormatting.toUpperCaseWithLocale;

/**
 * General heap of feature toggles we have and procedures to toggle them
 * Please make sure to use the `gds.features.` prefix so that the
 * toggle procedure will be excluded from gds.list
 */
public final class FeatureToggleProc {

    @Internal
    @Procedure("gds.features.importer.skipOrphanNodes")
    @Description("Toggle whether orphan nodes should be skipped during import.")
    public void skipOrphanNodes(@Name(value = "skipOrphanNodes") boolean skipOrphanNodes) {
        GdsFeatureToggles.SKIP_ORPHANS.toggle(skipOrphanNodes);
    }

    @Internal
    @Procedure("gds.features.importer.skipOrphanNodes.reset")
    @Description("Set the behavior of whether to skip orphan nodes to the default. That value is returned.")
    public Stream<FeatureState> resetSkipOrphanNodes() {
        GdsFeatureToggles.SKIP_ORPHANS.reset();
        return Stream.of(new FeatureState(GdsFeatureToggles.SKIP_ORPHANS.isEnabled()));
    }

    @Internal
    @Procedure("gds.features.usePartitionedScan")
    @Description("Toggle whether the new partitioned scan API should be used.")
    public void usePartitionedScan(@Name(value = "usePartitionedScan") boolean usePartitionedScan) {
        GdsFeatureToggles.USE_PARTITIONED_SCAN.toggle(usePartitionedScan);
    }

    @Internal
    @Procedure("gds.features.usePartitionedScan.reset")
    @Description("Set the behavior of whether to use the new partitioned scan API to the default. That value is returned.")
    public Stream<FeatureState> resetUsePartitionedScan() {
        GdsFeatureToggles.USE_PARTITIONED_SCAN.reset();
        return Stream.of(new FeatureState(GdsFeatureToggles.USE_PARTITIONED_SCAN.isEnabled()));
    }

    @Internal
    @Procedure("gds.features.pagesPerThread")
    @Description("Toggle how many pages per thread are being used by the loader.")
    public void pagesPerThread(@Name(value = "pagesPerThread") long pagesPerThread) {
        if (pagesPerThread <= 0 || pagesPerThread > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(formatWithLocale(
                "Invalid value for pagesPerThread: %d, must be a non-zero, positive integer",
                pagesPerThread
            ));
        }
        GdsFeatureToggles.PAGES_PER_THREAD.set((int) pagesPerThread);
    }

    @Internal
    @Procedure("gds.features.useUncompressedAdjacencyList")
    @Description("Toggle whether the adjacency list should be stored uncompressed during graph creation.")
    public void useUncompressedAdjacencyList(@Name(value = "useUncompressedAdjacencyList") boolean useUncompressedAdjacencyList) {
        GdsFeatureToggles.USE_UNCOMPRESSED_ADJACENCY_LIST.toggle(useUncompressedAdjacencyList);
    }

    @Internal
    @Procedure("gds.features.useUncompressedAdjacencyList.reset")
    @Description("Set the default behaviour of whether to store uncompressed adjacency lists during graph creation. That value is returned.")
    public Stream<FeatureState> resetUseUncompressedAdjacencyList() {
        GdsFeatureToggles.USE_UNCOMPRESSED_ADJACENCY_LIST.reset();
        return Stream.of(new FeatureState(GdsFeatureToggles.USE_UNCOMPRESSED_ADJACENCY_LIST.isEnabled()));
    }

    @Internal
    @Procedure("gds.features.usePackedAdjacencyList")
    @Description("Toggle whether the adjacency list should use bit packing compression during graph creation.")
    public void usePackedAdjacencyList(@Name(value = "usePackedAdjacencyList") boolean usePackedAdjacencyList) {
        GdsFeatureToggles.USE_PACKED_ADJACENCY_LIST.toggle(usePackedAdjacencyList);
    }

    @Internal
    @Procedure("gds.features.usePackedAdjacencyList.reset")
    @Description("Set the default behaviour of whether to use bit packing compression for adjacency lists during graph creation. That value is returned.")
    public Stream<FeatureState> resetUsePackedAdjacencyList() {
        GdsFeatureToggles.USE_PACKED_ADJACENCY_LIST.reset();
        return Stream.of(new FeatureState(GdsFeatureToggles.USE_PACKED_ADJACENCY_LIST.isEnabled()));
    }

    @Internal
    @Procedure("gds.features.adjacencyPackingStrategy")
    @Description("If `usePackedAdjacencyList` is enabled, this function allows setting the implementation strategy.")
    public void adjacencyPackingStrategy(@Name(value = "adjacencyPackingStrategy") String adjacencyPackingStrategy) {
        if (GdsFeatureToggles.USE_PACKED_ADJACENCY_LIST.isEnabled()) {
            try {
                var strategy = GdsFeatureToggles.AdjacencyPackingStrategy.valueOf(toUpperCaseWithLocale(
                    adjacencyPackingStrategy));
                GdsFeatureToggles.ADJACENCY_PACKING_STRATEGY.set(strategy);
            } catch (IllegalArgumentException iae) {
                throw new IllegalArgumentException(formatWithLocale(
                    "Invalid adjacency packing strategy: %s, must be one of %s",
                    adjacencyPackingStrategy,
                    Arrays.toString(GdsFeatureToggles.AdjacencyPackingStrategy.values())
                ));
            }
        } else {
            throw new IllegalStateException("Cannot set adjacency packing strategy when packed adjacency list is disabled.");
        }
    }

    @Internal
    @Procedure("gds.features.adjacencyPackingStrategy.reset")
    @Description("If `usePackedAdjacencyList` is enabled, this function resets the implementation strategy to the default.")
    public Stream<FeatureStringValue> resetAdjacencyPackingStrategy() {
        if (GdsFeatureToggles.USE_PACKED_ADJACENCY_LIST.isEnabled()) {
            GdsFeatureToggles.ADJACENCY_PACKING_STRATEGY.set(GdsFeatureToggles.ADJACENCY_PACKING_STRATEGY_DEFAULT_SETTING);
            return Stream.of(new FeatureStringValue(GdsFeatureToggles.ADJACENCY_PACKING_STRATEGY.get().name()));
        } else {
            throw new IllegalStateException("Cannot reset adjacency packing strategy when packed adjacency list is disabled.");
        }
    }

    @Internal
    @Procedure("gds.features.useReorderedAdjacencyList")
    @Description("Toggle whether the adjacency list should be reordered during graph creation.")
    public void useReorderedAdjacencyList(@Name(value = "useReorderedAdjacencyList") boolean useReorderedAdjacencyList) {
        GdsFeatureToggles.USE_REORDERED_ADJACENCY_LIST.toggle(useReorderedAdjacencyList);
    }

    @Internal
    @Procedure("gds.features.useReorderedAdjacencyList.reset")
    @Description("Set the default behaviour of whether to reorder adjacency lists during graph creation. That value is returned.")
    public Stream<FeatureState> resetUseReorderedAdjacencyList() {
        GdsFeatureToggles.USE_REORDERED_ADJACENCY_LIST.reset();
        return Stream.of(new FeatureState(GdsFeatureToggles.USE_REORDERED_ADJACENCY_LIST.isEnabled()));
    }

    @Internal
    @Procedure("gds.features.enableArrowDatabaseImport")
    @Description("Enables support for importing Neo4j databases via the GDS Arrow Flight Server.")
    public void enableArrowDatabaseImport(@Name(value = "useReorderedAdjacencyList") boolean useReorderedAdjacencyList) {
        GdsFeatureToggles.ENABLE_ARROW_DATABASE_IMPORT.toggle(useReorderedAdjacencyList);
    }

    @Internal
    @Procedure("gds.features.enableArrowDatabaseImport.reset")
    @Description("Sets the default behaviour for enabling Neo4j database import via the GDS Arrow Flight Server. That value is returned.")
    public Stream<FeatureState> resetEnableArrowDatabaseImport() {
        GdsFeatureToggles.ENABLE_ARROW_DATABASE_IMPORT.reset();
        return Stream.of(new FeatureState(GdsFeatureToggles.ENABLE_ARROW_DATABASE_IMPORT.isEnabled()));
    }

    @Internal
    @Procedure("gds.features.pagesPerThread.reset")
    @Description("Set the value of pages per thread to the default. That value is returned.")
    public Stream<FeatureLongValue> resetPagesPerThread() {
        GdsFeatureToggles.PAGES_PER_THREAD.set(GdsFeatureToggles.PAGES_PER_THREAD_DEFAULT_SETTING);
        return Stream.of(new FeatureLongValue(GdsFeatureToggles.PAGES_PER_THREAD_DEFAULT_SETTING));
    }

    @Internal
    @Procedure("gds.features.enableAdjacencyCompressionMemoryTracking")
    @Description("Enables memory tracking during the construction of an adjacency list.")
    public void enableAdjacencyCompressionMemoryTracking(@Name(value = "enableAdjacencyCompressionMemoryTracking") boolean enableAdjacencyCompressionMemoryTracking) {
        GdsFeatureToggles.ENABLE_ADJACENCY_COMPRESSION_MEMORY_TRACKING.toggle(enableAdjacencyCompressionMemoryTracking);
    }

    @Internal
    @Procedure("gds.features.enableAdjacencyCompressionMemoryTracking.reset")
    @Description("Sets the default behaviour for enabling memory tracking during the construction of an adjacency list. That value is returned.")
    public Stream<FeatureState> resetEnableAdjacencyCompressionMemoryTracking() {
        GdsFeatureToggles.ENABLE_ADJACENCY_COMPRESSION_MEMORY_TRACKING.reset();
        return Stream.of(new FeatureState(GdsFeatureToggles.ENABLE_ADJACENCY_COMPRESSION_MEMORY_TRACKING.isEnabled()));
    }

    @SuppressWarnings("unused")
    public static final class FeatureState {
        public final boolean enabled;

        public FeatureState(boolean enabled) {
            this.enabled = enabled;
        }
    }

    @SuppressWarnings("unused")
    public static final class FeatureLongValue {
        public final long value;

        FeatureLongValue(long value) {
            this.value = value;
        }
    }

    public static final class FeatureStringValue {
        public final String value;

        FeatureStringValue(String value) {
            this.value = value;
        }
    }
}
