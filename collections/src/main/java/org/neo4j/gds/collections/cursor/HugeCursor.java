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
package org.neo4j.gds.collections.cursor;

import org.neo4j.gds.collections.PageUtil;

import static java.lang.reflect.Array.getLength;
import static org.neo4j.gds.mem.HugeArrays.PAGE_SHIFT;
import static org.neo4j.gds.mem.HugeArrays.PAGE_SIZE;
import static org.neo4j.gds.mem.HugeArrays.exclusiveIndexOfPage;
import static org.neo4j.gds.mem.HugeArrays.indexInPage;
import static org.neo4j.gds.mem.HugeArrays.pageIndex;

/**
 * View of data underlying an Huge array, accessible as slices of one or more primitive arrays.
 * The values are from {@code array[offset]} (inclusive) until {@code array[limit]} (exclusive).
 * The range might match the complete array, but that isn't guaranteed.
 * <p>
 * The {@code limit} parameter does not have the same meaning as the {@code length} parameter that is used in many methods that can operate on array slices.
 * The proper value would be {@code int length = limit - offset}.
 */
public abstract class HugeCursor<Array> implements AutoCloseable {

    /**
     * the base for the index to get the global index
     */
    public long base;
    /**
     * a slice of values currently being traversed
     */
    public Array array;
    /**
     * the offset into the array
     */
    public int offset;
    /**
     * the limit of the array, exclusive – the first index not to be contained
     */
    public int limit;

    HugeCursor() {
    }

    /**
     * Try to load the next page and return the success of this load.
     * Once the method returns {@code false}, this method will never return {@code true} again until the cursor is reset.
     * The cursor behavior is not defined and might be unusable and throw exceptions after this method returns {@code false}.
     *
     * @return true, iff the cursor is still valid on contains new data; false if there is no more data.
     */
    public abstract boolean next();

    /**
     * Releases the reference to the underlying array so that it might be garbage collected.
     * The cursor can never be used again after calling this method, doing so results in undefined behavior.
     */
    @Override
    public abstract void close();

    /**
     * initializes cursor from 0 to capacity
     */
    abstract void setRange();

    /**
     * initializes cursor from start to end
     */
    public abstract void setRange(long start, long end);


    public static final class SinglePageCursor<Array> extends HugeCursor<Array> {

        private boolean exhausted;

        public SinglePageCursor(final Array page) {
            super();
            this.array = page;
            this.base = 0L;
        }

        @Override
        void setRange() {
            setRange(0, getLength(array));
        }

        @Override
        public void setRange(final long start, final long end) {
            setRange((int) start, (int) end);
        }

        private void setRange(int start, int end) {
            exhausted = false;
            offset = start;
            limit = end;
        }

        @Override
        public final boolean next() {
            if (exhausted) {
                return false;
            }
            exhausted = true;
            return true;
        }

        @Override
        public void close() {
            array = null;
            limit = 0;
            exhausted = true;
        }
    }

    public static final class PagedCursor<Array> extends HugeCursor<Array> {

        private Array[] pages;
        private int pageIndex;
        private int fromPage;
        private int maxPage;
        private long capacity;
        private long end;

        public PagedCursor(Array[] pages) {
            super();
            this.setPages(pages);
        }

        public PagedCursor(long capacity, Array[] pages) {
            super();
            this.setPages(pages, capacity);
        }

        public void setPages(Array[] pages) {
            this.setPages(pages, PageUtil.capacityFor(pages.length, PAGE_SHIFT));
        }

        public void setPages(Array[] pages, long capacity) {
            this.capacity = capacity;
            this.pages = pages;
        }

        @Override
        void setRange() {
            setRange(0L, capacity);
        }

        @Override
        public void setRange(long start, long end) {
            fromPage = pageIndex(start);
            maxPage = pageIndex(end - 1L);
            pageIndex = fromPage - 1;
            this.end = end;
            base = (long) fromPage << PAGE_SHIFT;
            offset = indexInPage(start);
            limit = fromPage == maxPage ? exclusiveIndexOfPage(end) : PAGE_SIZE;
        }

        @Override
        public boolean next() {
            int current = ++pageIndex;
            if (current > maxPage) {
                return false;
            }
            array = pages[current];
            if (current == fromPage) {
                return true;
            }
            base += PAGE_SIZE;
            offset = 0;
            limit = current == maxPage ? exclusiveIndexOfPage(end) : getLength(array);
            return true;
        }

        @Override
        public void close() {
            array = null;
            pages = null;
            base = 0L;
            end = 0L;
            limit = 0;
            capacity = 0L;
            maxPage = -1;
            fromPage = -1;
            pageIndex = -1;
        }
    }
}
