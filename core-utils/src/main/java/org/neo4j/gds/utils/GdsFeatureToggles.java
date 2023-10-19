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
package org.neo4j.gds.utils;

import org.apache.commons.text.CaseUtils;
import org.jetbrains.annotations.TestOnly;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public enum GdsFeatureToggles {

    SKIP_ORPHANS(false),
    USE_PARTITIONED_SCAN(false),
    USE_BIT_ID_MAP(true),
    USE_UNCOMPRESSED_ADJACENCY_LIST(false),
    USE_PACKED_ADJACENCY_LIST(false),
    USE_REORDERED_ADJACENCY_LIST(false),
    ENABLE_ARROW_DATABASE_IMPORT(true),
    // Makes sure end users algorithms don't fail due to errors in log tracking,
    // but keeps the option to find these failures when running tests.
    FAIL_ON_PROGRESS_TRACKER_ERRORS(false),
    ENABLE_ADJACENCY_COMPRESSION_MEMORY_TRACKING(false);

    public boolean isEnabled() {
        return current.get();
    }

    public boolean isDisabled() {
        return !isEnabled();
    }

    public boolean toggle(boolean value) {
        return current.getAndSet(value);
    }

    public void reset() {
        current.set(defaultValue);
    }

    @TestOnly
    public synchronized <E extends Exception> void enableAndRun(
        CheckedRunnable<E> code
    ) throws E {
        var before = toggle(true);
        try {
            code.checkedRun();
        } finally {
            toggle(before);
        }
    }

    @TestOnly
    public synchronized <E extends Exception> void disableAndRun(
        CheckedRunnable<E> code
    ) throws E {
        var before = toggle(false);
        try {
            code.checkedRun();
        } finally {
            toggle(before);
        }
    }

    private final AtomicBoolean current;
    private final boolean defaultValue;

    GdsFeatureToggles(boolean defaultValue) {
        this.defaultValue = defaultValue;
        this.current = new AtomicBoolean(
            booleanProperty(
                name(GdsFeatureToggles.class, CaseUtils.toCamelCase(name(), false, '_')),
                defaultValue
            )
        );
    }

    // How many pages per loading thread. Pages are being locked while written to, so
    // more pages allow for less contention, fewer pages allow for higher throughput.
    public static final int PAGES_PER_THREAD_DEFAULT_SETTING = 4;
    private static final int PAGES_PER_THREAD_FLAG = Integer.getInteger(
        name(GdsFeatureToggles.class, "pagesPerThread"),
        PAGES_PER_THREAD_DEFAULT_SETTING
    );
    public static final AtomicInteger PAGES_PER_THREAD = new AtomicInteger(PAGES_PER_THREAD_FLAG);


    // Determines the packing strategy when adjacency packing is used.
    public enum AdjacencyPackingStrategy {
        BLOCK_ALIGNED_TAIL,
        VAR_LONG_TAIL,
        PACKED_TAIL,
        INLINED_HEAD_PACKED_TAIL,
    }

    public static final AdjacencyPackingStrategy ADJACENCY_PACKING_STRATEGY_DEFAULT_SETTING = AdjacencyPackingStrategy.PACKED_TAIL;
    public static final AtomicReference<AdjacencyPackingStrategy> ADJACENCY_PACKING_STRATEGY =
        new AtomicReference<>(ADJACENCY_PACKING_STRATEGY_DEFAULT_SETTING);

    private static String name(Class<?> location, String name) {
        return location.getCanonicalName() + "." + name;
    }

    private static boolean booleanProperty(String flag, boolean defaultValue) {
        return parseBoolean(System.getProperty(flag), defaultValue);
    }

    private static boolean parseBoolean(String value, boolean defaultValue) {
        return defaultValue ? !"false".equalsIgnoreCase(value) : "true".equalsIgnoreCase(value);
    }
}
