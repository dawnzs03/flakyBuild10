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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.neo4j.gds.utils.GdsFeatureToggles;
import org.neo4j.graphdb.QueryExecutionException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.neo4j.gds.utils.GdsFeatureToggles.ADJACENCY_PACKING_STRATEGY;
import static org.neo4j.gds.utils.GdsFeatureToggles.ADJACENCY_PACKING_STRATEGY_DEFAULT_SETTING;
import static org.neo4j.gds.utils.GdsFeatureToggles.ENABLE_ADJACENCY_COMPRESSION_MEMORY_TRACKING;
import static org.neo4j.gds.utils.GdsFeatureToggles.ENABLE_ARROW_DATABASE_IMPORT;
import static org.neo4j.gds.utils.GdsFeatureToggles.SKIP_ORPHANS;
import static org.neo4j.gds.utils.GdsFeatureToggles.USE_PACKED_ADJACENCY_LIST;
import static org.neo4j.gds.utils.GdsFeatureToggles.USE_PARTITIONED_SCAN;
import static org.neo4j.gds.utils.GdsFeatureToggles.USE_REORDERED_ADJACENCY_LIST;
import static org.neo4j.gds.utils.GdsFeatureToggles.USE_UNCOMPRESSED_ADJACENCY_LIST;

class FeatureToggleProcTest extends BaseProcTest {

    @BeforeEach
    void setUp() throws Exception {
        registerProcedures(FeatureToggleProc.class);
    }

    @Test
    void toggleSkipOrphanNodes() {
        var skipOrphanNodes = SKIP_ORPHANS.isEnabled();
        runQuery("CALL gds.features.importer.skipOrphanNodes($value)", Map.of("value", !skipOrphanNodes));
        assertEquals(!skipOrphanNodes, SKIP_ORPHANS.isEnabled());
        runQuery("CALL gds.features.importer.skipOrphanNodes($value)", Map.of("value", skipOrphanNodes));
        assertEquals(skipOrphanNodes, SKIP_ORPHANS.isEnabled());
    }

    @Test
    void resetSkipOrphanNodes() {
        SKIP_ORPHANS.reset();
        assertCypherResult(
            "CALL gds.features.importer.skipOrphanNodes.reset()",
            List.of(Map.of("enabled", false))
        );
        assertFalse(SKIP_ORPHANS.isEnabled());
    }

    @Test
    void toggleUsePartitionedScan() {
        var usePartitionedScan = USE_PARTITIONED_SCAN.isEnabled();
        runQuery("CALL gds.features.usePartitionedScan($value)", Map.of("value", !usePartitionedScan));
        assertEquals(!usePartitionedScan, USE_PARTITIONED_SCAN.isEnabled());
        runQuery("CALL gds.features.usePartitionedScan($value)", Map.of("value", usePartitionedScan));
        assertEquals(usePartitionedScan, USE_PARTITIONED_SCAN.isEnabled());
    }

    @Test
    void resetUsePartitionedScan() {
        USE_PARTITIONED_SCAN.reset();
        assertCypherResult(
            "CALL gds.features.usePartitionedScan.reset()",
            List.of(Map.of("enabled", false))
        );
        assertFalse(USE_PARTITIONED_SCAN.isEnabled());
    }

    @Test
    void toggleUseUncompressedAdjacencyList() {
        var useUncompressedAdjacencyList = USE_UNCOMPRESSED_ADJACENCY_LIST.isEnabled();
        runQuery(
            "CALL gds.features.useUncompressedAdjacencyList($value)",
            Map.of("value", !useUncompressedAdjacencyList)
        );
        assertEquals(!useUncompressedAdjacencyList, USE_UNCOMPRESSED_ADJACENCY_LIST.isEnabled());
        runQuery(
            "CALL gds.features.useUncompressedAdjacencyList($value)",
            Map.of("value", useUncompressedAdjacencyList)
        );
        assertEquals(useUncompressedAdjacencyList, USE_UNCOMPRESSED_ADJACENCY_LIST.isEnabled());
    }

    @Test
    void resetUseUncompressedAdjacencyList() {
        USE_UNCOMPRESSED_ADJACENCY_LIST.reset();
        assertCypherResult(
            "CALL gds.features.useUncompressedAdjacencyList.reset()",
            List.of(Map.of("enabled", false))
        );
        assertFalse(USE_UNCOMPRESSED_ADJACENCY_LIST.isEnabled());
    }
    
    @Test
    void toggleUsePackedAdjacencyList() {
        var usePackedAdjacencyList = USE_PACKED_ADJACENCY_LIST.isEnabled();
        runQuery(
            "CALL gds.features.usePackedAdjacencyList($value)",
            Map.of("value", !usePackedAdjacencyList)
        );
        assertEquals(!usePackedAdjacencyList, USE_PACKED_ADJACENCY_LIST.isEnabled());
        runQuery(
            "CALL gds.features.usePackedAdjacencyList($value)",
            Map.of("value", usePackedAdjacencyList)
        );
        assertEquals(usePackedAdjacencyList, USE_PACKED_ADJACENCY_LIST.isEnabled());
    }

    @Test
    void resetUsePackedAdjacencyList() {
        USE_PACKED_ADJACENCY_LIST.reset();
        assertCypherResult(
            "CALL gds.features.usePackedAdjacencyList.reset()",
            List.of(Map.of("enabled", false))
        );
        assertFalse(USE_PACKED_ADJACENCY_LIST.isEnabled());
    }

    @ParameterizedTest
    @EnumSource(value = GdsFeatureToggles.AdjacencyPackingStrategy.class)
    void toggleAdjacencyPackingStrategy(GdsFeatureToggles.AdjacencyPackingStrategy strategy) {
        USE_PACKED_ADJACENCY_LIST.enableAndRun(() -> {
            runQuery("CALL gds.features.adjacencyPackingStrategy($value)", Map.of("value", strategy.name()));
            assertThat(ADJACENCY_PACKING_STRATEGY.get()).isEqualTo(strategy);
        });
    }

    @Test
    void toggleAdjacencyPackingStrategyShouldFailIfAdjacencyPackingIsDisabled() {
        assertThatThrownBy(() -> runQuery("CALL gds.features.adjacencyPackingStrategy($value)", Map.of("value",
            GdsFeatureToggles.AdjacencyPackingStrategy.PACKED_TAIL.name()
        ))).rootCause()
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("packed adjacency list is disabled");
    }

    @Test
    void toggleAdjacencyPackingStrategyShouldFailIfStrategyIsUnsupported() {
        USE_PACKED_ADJACENCY_LIST.enableAndRun(() -> {
            assertThatThrownBy(() -> runQuery("CALL gds.features.adjacencyPackingStrategy($value)", Map.of("value", "gzip")))
                .rootCause()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid adjacency packing strategy")
                .hasMessageContaining("gzip")
                .hasMessageContaining(Arrays.toString(GdsFeatureToggles.AdjacencyPackingStrategy.values()));
        });
    }

    @ParameterizedTest
    @EnumSource(value = GdsFeatureToggles.AdjacencyPackingStrategy.class)
    void shouldResetAdjacencyPackingStrategy(GdsFeatureToggles.AdjacencyPackingStrategy strategy) {
        USE_PACKED_ADJACENCY_LIST.enableAndRun(() -> {
            runQuery("CALL gds.features.adjacencyPackingStrategy($value)", Map.of("value", strategy.name()));
            assertThat(ADJACENCY_PACKING_STRATEGY.get()).isEqualTo(strategy);
            assertCypherResult(
                "CALL gds.features.adjacencyPackingStrategy.reset()",
                List.of(Map.of("value", ADJACENCY_PACKING_STRATEGY_DEFAULT_SETTING.name()))
            );
            assertThat(ADJACENCY_PACKING_STRATEGY.get()).isEqualTo(ADJACENCY_PACKING_STRATEGY_DEFAULT_SETTING);
        });
    }

    @Test
    void toggleUseReorderedAdjacencyList() {
        var useReorderedAdjacencyList = USE_REORDERED_ADJACENCY_LIST.isEnabled();
        runQuery("CALL gds.features.useReorderedAdjacencyList($value)", Map.of("value", !useReorderedAdjacencyList));
        assertEquals(!useReorderedAdjacencyList, USE_REORDERED_ADJACENCY_LIST.isEnabled());
        runQuery("CALL gds.features.useReorderedAdjacencyList($value)", Map.of("value", useReorderedAdjacencyList));
        assertEquals(useReorderedAdjacencyList, USE_REORDERED_ADJACENCY_LIST.isEnabled());
    }

    @Test
    void resetUseReorderedAdjacencyList() {
        USE_REORDERED_ADJACENCY_LIST.reset();
        assertCypherResult(
            "CALL gds.features.useReorderedAdjacencyList.reset()",
            List.of(Map.of("enabled", false))
        );
        assertFalse(USE_REORDERED_ADJACENCY_LIST.isEnabled());
    }

    @Test
    void toggleEnableArrowDatabaseImport() {
        var enableArrowDatabaseImport = ENABLE_ARROW_DATABASE_IMPORT.isEnabled();
        runQuery("CALL gds.features.enableArrowDatabaseImport($value)", Map.of("value", !enableArrowDatabaseImport));
        assertEquals(!enableArrowDatabaseImport, ENABLE_ARROW_DATABASE_IMPORT.isEnabled());
        runQuery("CALL gds.features.enableArrowDatabaseImport($value)", Map.of("value", enableArrowDatabaseImport));
        assertEquals(enableArrowDatabaseImport, ENABLE_ARROW_DATABASE_IMPORT.isEnabled());
    }

    @Test
    void resetEnableArrowDatabaseImport() {
        ENABLE_ARROW_DATABASE_IMPORT.reset();
        assertCypherResult(
            "CALL gds.features.enableArrowDatabaseImport.reset()",
            List.of(Map.of("enabled", true))
        );
        assertTrue(ENABLE_ARROW_DATABASE_IMPORT.isEnabled());
    }

    @Test
    void togglePagesPerThread() {
        var pagesPerThread = GdsFeatureToggles.PAGES_PER_THREAD.get();
        runQuery("CALL gds.features.pagesPerThread($value)", Map.of("value", pagesPerThread + 1));
        assertEquals(pagesPerThread + 1, GdsFeatureToggles.PAGES_PER_THREAD.get());
        runQuery("CALL gds.features.pagesPerThread($value)", Map.of("value", pagesPerThread));
        assertEquals(pagesPerThread, GdsFeatureToggles.PAGES_PER_THREAD.get());
    }

    @Test
    void togglePagesPerThreadValidationForNegativeValue() {
        var pagesPerThread = GdsFeatureToggles.PAGES_PER_THREAD.get();
        var exception = assertThrows(
            QueryExecutionException.class,
            () -> runQuery("CALL gds.features.pagesPerThread($value)", Map.of("value", -42))
        );
        assertThat(exception)
            .hasRootCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Invalid value for pagesPerThread: -42, must be a non-zero, positive integer");
        assertEquals(pagesPerThread, GdsFeatureToggles.PAGES_PER_THREAD.get());
    }

    @Test
    void togglePagesPerThreadValidationForTooLargeValue() {
        var pagesPerThread = GdsFeatureToggles.PAGES_PER_THREAD.get();
        var exception = assertThrows(
            QueryExecutionException.class,
            () -> runQuery("CALL gds.features.pagesPerThread($value)", Map.of("value", 133713371337L))
        );
        assertThat(exception)
            .hasRootCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("Invalid value for pagesPerThread: 133713371337, must be a non-zero, positive integer");
        assertEquals(pagesPerThread, GdsFeatureToggles.PAGES_PER_THREAD.get());
    }

    @Test
    void resetPagesPerThread() {
        var defaultValue = GdsFeatureToggles.PAGES_PER_THREAD_DEFAULT_SETTING;
        GdsFeatureToggles.PAGES_PER_THREAD.set(defaultValue + 1);
        assertCypherResult(
            "CALL gds.features.pagesPerThread.reset()",
            List.of(Map.of("value", (long) defaultValue))
        );
        assertEquals(defaultValue, GdsFeatureToggles.PAGES_PER_THREAD.get());
    }

    @Test
    void toggleEnableAdjacencyCompressionMemoryTracking() {
        var enableAdjacencyCompressionMemoryTracking = ENABLE_ADJACENCY_COMPRESSION_MEMORY_TRACKING.isEnabled();
        runQuery("CALL gds.features.enableAdjacencyCompressionMemoryTracking($value)", Map.of("value", !enableAdjacencyCompressionMemoryTracking));
        assertEquals(!enableAdjacencyCompressionMemoryTracking, ENABLE_ADJACENCY_COMPRESSION_MEMORY_TRACKING.isEnabled());
        runQuery("CALL gds.features.enableAdjacencyCompressionMemoryTracking($value)", Map.of("value", enableAdjacencyCompressionMemoryTracking));
        assertEquals(enableAdjacencyCompressionMemoryTracking, ENABLE_ADJACENCY_COMPRESSION_MEMORY_TRACKING.isEnabled());
    }

    @Test
    void resetEnableAdjacencyCompressionMemoryTracking() {
        ENABLE_ADJACENCY_COMPRESSION_MEMORY_TRACKING.reset();
        assertCypherResult(
            "CALL gds.features.enableAdjacencyCompressionMemoryTracking.reset()",
            List.of(Map.of("enabled", false))
        );
        assertTrue(ENABLE_ADJACENCY_COMPRESSION_MEMORY_TRACKING.isDisabled());
    }
}
