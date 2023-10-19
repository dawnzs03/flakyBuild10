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
package org.neo4j.gds.core.utils.paged;

import org.jetbrains.annotations.TestOnly;

public interface ReadOnlyHugeLongArray {
    long get(long index);

    long size();

    @TestOnly
    long[] toArray();

    static ReadOnlyHugeLongArray of(long... values) {
        return of(HugeLongArray.of(values));
    }

    static ReadOnlyHugeLongArray of(HugeLongArray hla) {
        return new ReadOnlyHugeLongArray() {
            @Override
            public long get(long index) {
                return hla.get(index);
            }

            @Override
            public long size() {
                return hla.size();
            }

            @Override
            public long[] toArray() {
                return hla.toArray();
            }
        };
    }
}
