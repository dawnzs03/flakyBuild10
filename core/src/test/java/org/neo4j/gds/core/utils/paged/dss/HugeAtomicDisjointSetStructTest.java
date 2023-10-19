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
package org.neo4j.gds.core.utils.paged.dss;

import com.carrotsearch.hppc.LongLongMap;
import com.carrotsearch.hppc.LongLongScatterMap;
import com.carrotsearch.hppc.cursors.LongLongCursor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HugeAtomicDisjointSetStructTest {

    private static final int CAPACITY = 7;
    private DisjointSetStruct struct;

    DisjointSetStruct newSet(int capacity) {
        return new HugeAtomicDisjointSetStruct(capacity, 4);
    }

    @BeforeEach
    final void setup() {
        struct = newSet(CAPACITY);
    }

    @Test
    final void testSetUnion() {
        // {0}{1}{2}{3}{4}{5}{6}
        assertFalse(connected(struct,0, 1));
        assertEquals(7, getSetSize(struct).size());
        assertEquals(0, struct.setIdOf(0));
        assertEquals(1, struct.setIdOf(1));
        assertEquals(2, struct.setIdOf(2));
        assertEquals(3, struct.setIdOf(3));
        assertEquals(4, struct.setIdOf(4));
        assertEquals(5, struct.setIdOf(5));
        assertEquals(6, struct.setIdOf(6));

        struct.union(0, 1);
        // {0,1}{2}{3}{4}{5}{6}
        assertTrue(connected(struct,0, 1));
        assertFalse(connected(struct,2, 3));
        assertEquals(6, getSetSize(struct).size());

        struct.union(2, 3);
        // {0,1}{2,3}{4}{5}{6}
        assertTrue(connected(struct, 2, 3));
        assertFalse(connected(struct, 0, 2));
        assertFalse(connected(struct, 0, 3));
        assertFalse(connected(struct, 1, 2));
        assertFalse(connected(struct, 1, 3));
        assertEquals(5, getSetSize(struct).size());

        struct.union(3, 0);
        // {0,1,2,3}{4}{5}{6}
        assertTrue(connected(struct, 0, 2));
        assertTrue(connected(struct, 0, 3));
        assertTrue(connected(struct, 1, 2));
        assertTrue(connected(struct, 1, 3));
        assertFalse(connected(struct, 4, 5));
        assertEquals(4, getSetSize(struct).size());

        struct.union(4, 5);
        // {0,1,2,3}{4,5}{6}
        assertTrue(connected(struct, 4, 5));
        assertFalse(connected(struct, 0, 4));
        assertEquals(3, getSetSize(struct).size());

        struct.union(0, 4);
        // {0,1,2,3,4,5}{6}
        assertTrue(connected(struct, 0, 4));
        assertTrue(connected(struct, 0, 5));
        assertTrue(connected(struct, 1, 4));
        assertTrue(connected(struct, 1, 5));
        assertTrue(connected(struct, 2, 4));
        assertTrue(connected(struct, 2, 5));
        assertTrue(connected(struct, 3, 4));
        assertTrue(connected(struct, 3, 5));
        assertTrue(connected(struct, 4, 5));
        assertFalse(connected(struct, 0, 6));
        assertFalse(connected(struct, 1, 6));
        assertFalse(connected(struct, 2, 6));
        assertFalse(connected(struct, 3, 6));
        assertFalse(connected(struct, 4, 6));
        assertFalse(connected(struct, 5, 6));

        assertEquals(struct.setIdOf(0), struct.setIdOf(1));
        assertEquals(struct.setIdOf(0), struct.setIdOf(2));
        assertEquals(struct.setIdOf(0), struct.setIdOf(3));
        assertEquals(struct.setIdOf(0), struct.setIdOf(4));
        assertEquals(struct.setIdOf(0), struct.setIdOf(5));
        assertNotEquals(struct.setIdOf(0), struct.setIdOf(6));

        final LongLongMap setSize = getSetSize(struct);
        assertEquals(2, setSize.size());
        for (LongLongCursor cursor : setSize) {
            assertTrue(cursor.value == 6 || cursor.value == 1);
        }
    }

    /**
     * Check if p and q belong to the same set.
     *
     * @param p a set item
     * @param q a set item
     * @return true if both items belong to the same set, false otherwise
     */
    private boolean connected(DisjointSetStruct struct, long p, long q) {
        return struct.sameSet(p, q);
    }

    /**
     * Compute the size of each set.
     *
     * @return a map which maps setId to setSize
     */
    private LongLongMap getSetSize(DisjointSetStruct struct) {
        final LongLongMap map = new LongLongScatterMap();
        for (long i = struct.size() - 1; i >= 0; i--) {
            map.addTo(struct.setIdOf(i), 1);
        }
        return map;
    }
}
