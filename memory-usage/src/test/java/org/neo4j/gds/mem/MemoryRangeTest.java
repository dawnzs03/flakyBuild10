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
package org.neo4j.gds.mem;

import org.junit.jupiter.api.Test;
import org.neo4j.gds.core.utils.mem.MemoryRange;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

final class MemoryRangeTest {

    @Test
    void testFactoryMethods() {
        MemoryRange range;

        range = MemoryRange.of(42);
        assertEquals(42L, range.min);
        assertEquals(42L, range.max);

        range = MemoryRange.of(42, 1337);
        assertEquals(42L, range.min);
        assertEquals(1337L, range.max);
    }

    @Test
    void rangeMustNotBeNegative() {
        try {
            MemoryRange.of(-42);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("min range < 0: -42", e.getMessage());
        }
        try {
            MemoryRange.of(42, -1337);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("max range < 0: -1337", e.getMessage());
        }
    }

    @Test
    void minMustBeSmallerThanMax() {
        try {
            MemoryRange.of(1337, 42);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("max range < min: 42 < 1337", e.getMessage());
        }
    }

    @Test
    void emptyHasZeroMinMax() {
        MemoryRange empty = MemoryRange.empty();
        assertEquals(0L, empty.min);
        assertEquals(0L, empty.max);
    }

    @Test
    void isEmptyChecksForZeroMinMax() {
        MemoryRange empty = MemoryRange.empty();
        assertTrue(empty.isEmpty());

        empty = MemoryRange.of(0L);
        assertTrue(empty.isEmpty());

        empty = MemoryRange.of(0L, 0L);
        assertTrue(empty.isEmpty());
    }

    @Test
    void equalsChecksForValueEquality() {
        MemoryRange range1 = MemoryRange.of(42);
        MemoryRange range2 = MemoryRange.of(42);
        MemoryRange range3 = MemoryRange.of(42, 1337);

        assertEquals(range1, range2);
        assertEquals(range2, range1); // symmetric
        assertNotEquals(range1, range3);
    }

    @Test
    void addAddsTheirMinAndMaxValues() {
        MemoryRange range1 = MemoryRange.of(42);
        MemoryRange range2 = MemoryRange.of(1337);
        assertEquals(MemoryRange.of(42 + 1337), range1.add(range2));

        MemoryRange range3 = MemoryRange.of(42, 1337);
        assertEquals(MemoryRange.of(42 + 42, 1337 + 42), range3.add(range1));
        assertEquals(MemoryRange.of(42 + 42, 1337 + 1337), range3.add(range3));
    }

    @Test
    void additionLaws() {
        MemoryRange range1 = MemoryRange.of(42);
        MemoryRange range2 = MemoryRange.of(1337);
        MemoryRange range3 = MemoryRange.of(42, 1337);

        // Commutativity
        assertEquals(range1.add(range2), range2.add(range1));
        // Associativity
        assertEquals(range1.add(range2).add(range3), range1.add(range2.add(range3)));
        // Identity
        assertEquals(range1, range1.add(MemoryRange.empty()));
        assertEquals(range1, MemoryRange.empty().add(range1));
    }

    @Test
    void addFailsOnOverflow() {
        assertThrows(ArithmeticException.class, () -> MemoryRange.of(Long.MAX_VALUE).add(MemoryRange.of(42)));
    }

    @Test
    void timesMultipliesTheirMinAndMaxValues() {
        MemoryRange range = MemoryRange.of(42);
        assertEquals(MemoryRange.of(42 * 1337), range.times(1337));

        range = MemoryRange.of(42, 1337);
        assertEquals(MemoryRange.of(42 * 42, 1337 * 42), range.times(42));
        assertEquals(MemoryRange.of(42 * 1337, 1337 * 1337), range.times(1337));
    }

    @Test
    void multiplicationLaws() {
        MemoryRange range1 = MemoryRange.of(42);
        MemoryRange range2 = MemoryRange.of(1337);
        MemoryRange range3 = MemoryRange.of(42, 1337);

        // Commutativity
        assertEquals(range1.times(range2.min), range2.times(range1.min));
        // Associativity
        assertEquals(range1.times(range2.min).times(range3.min), range1.times(range2.times(range3.min).min));
        // Identity
        assertEquals(range1, range1.times(1));
        // Zero Property
        assertEquals(MemoryRange.empty(), range1.times(0));
        assertEquals(MemoryRange.empty(), MemoryRange.empty().times(42));
    }

    @Test
    void subtractFromMinAndMaxValues() {
        assertEquals(MemoryRange.of(42, 1337).subtract(41), MemoryRange.of(42 - 41, 1337 - 41));
    }

    @Test
    void subtractionLaws() {
        MemoryRange range = MemoryRange.of(42, 1337);

        // Commutativity
        assertEquals(range.subtract(10).subtract(1), range.subtract(1).subtract(10));
        // Associativity
        assertEquals(range.subtract(10).subtract(1), range.subtract(10 + 1));
        // Identity
        assertEquals(range, range.subtract(0));
    }

    @Test
    void throwOnInvalidSubtraction() {
        MemoryRange range = MemoryRange.of(42, 1337);

        assertThatThrownBy(() -> range.subtract(42 + 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("min range < 0: -1");
    }

    @Test
    void unionCombinesTwoRanges() {
        MemoryRange range1 = MemoryRange.of(42);
        MemoryRange range2 = MemoryRange.of(1337);
        MemoryRange range3 = MemoryRange.of(42, 1337);
        MemoryRange range4 = MemoryRange.of(4242, 13371337);
        assertEquals(MemoryRange.of(0L, 42L), range1.union(MemoryRange.empty()));
        assertEquals(MemoryRange.of(0L, 1337L), range2.union(MemoryRange.empty()));
        assertEquals(range3, range1.union(range2));
        assertEquals(MemoryRange.of(42, 13371337), range3.union(range4));
    }

    @Test
    void timesFailsOnOverflow() {
        assertThrows(ArithmeticException.class, () -> MemoryRange.of(Long.MAX_VALUE / 2).times(3));
    }

    @Test
    void toStringProducesSingleHumanReadableOutputIfMinEqualsMax() {
        assertEquals("42 Bytes", MemoryRange.of(42).toString());
        assertEquals("1337 Bytes", MemoryRange.of(1337).toString());
        assertEquals("54 KiB", MemoryRange.of(1337 * 42).toString());
        assertEquals("124 GiB", MemoryRange.of(133_742_133_742L).toString());
        assertEquals("8191 PiB", MemoryRange.of(Long.MAX_VALUE).toString());
    }

    @Test
    void toStringProducesHumanReadableRangeOutput() {
        assertEquals("[42 Bytes ... 1337 Bytes]", MemoryRange.of(42, 1337).toString());
        assertEquals("[54 KiB ... 124 GiB]", MemoryRange.of(1337 * 42, 133_742_133_742L).toString());
    }
}
