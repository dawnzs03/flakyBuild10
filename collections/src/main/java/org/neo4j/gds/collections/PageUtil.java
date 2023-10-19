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
package org.neo4j.gds.collections;

import org.neo4j.gds.mem.BitUtil;

public final class PageUtil {

    // Arrays larger than this have a higher risk of triggering a full GC
    // Prevents full GC more often as not so much consecutive memory is allocated in one go as
    // compared to a page shift of 30 or 32. See https://github.com/neo4j-contrib/neo4j-graph-algorithms/pull/859#discussion_r272262734.
    public static final int MAX_ARRAY_LENGTH = 1 << 28;

    public static final int PAGE_SIZE_4KB = 1 << 12;
    public static final int PAGE_SIZE_32KB = 1 << 15;

    public static int pageSizeFor(int pageSizeInBytes, int sizeOfElement) {
        assert BitUtil.isPowerOfTwo(sizeOfElement);
        return pageSizeInBytes >> Integer.numberOfTrailingZeros(sizeOfElement);
    }

    public static int numPagesFor(long capacity, int pageSize) {
        int pageShift = Integer.numberOfTrailingZeros(pageSize);
        int pageMask = pageSize - 1;
        return numPagesFor(capacity, pageShift, pageMask);
    }

    public static int numPagesFor(long capacity, int pageShift, long pageMask) {
        final long numPages = (capacity + pageMask) >>> pageShift;
        assert numPages <= Integer.MAX_VALUE : "pageSize=" + (pageMask + 1) + " is too small for such as capacity: " + capacity;
        return (int) numPages;
    }

    public static long capacityFor(int numPages, int pageShift) {
        return ((long) numPages) << pageShift;
    }

    public static int pageIndex(long index, int pageShift) {
        return (int) (index >>> pageShift);
    }

    public static int indexInPage(long index, int pageMask) {
        return (int) (index & (long) pageMask);
    }

    public static int indexInPage(long index, long pageMask) {
        return (int) (index & pageMask);
    }

    public static int exclusiveIndexOfPage(long index, int pageMask) {
        return 1 + (int) ((index - 1L) & pageMask);
    }


    private PageUtil() {
        throw new UnsupportedOperationException("No instances");
    }
}
