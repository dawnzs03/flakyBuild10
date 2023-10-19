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
package org.neo4j.gds.core.utils.shuffle;

import org.junit.jupiter.api.Test;
import org.neo4j.gds.core.utils.paged.HugeLongArray;

import java.util.Arrays;
import java.util.SplittableRandom;

import static org.assertj.core.api.Assertions.assertThat;

class ShuffleUtilTest {

    @Test
    void testShuffledHLA() {
        var random = new SplittableRandom(123L);
        var data = HugeLongArray.newArray(10);
        data.setAll(i -> i);
        ShuffleUtil.shuffleArray(data, random);
        assertThat(data.toArray()).containsExactly(7L, 4L, 2L, 3L, 5L, 8L, 1L, 0L, 9L, 6L);
    }

    @Test
    void testShuffledIntArray() {
        var random = new SplittableRandom(123L);
        var data = new int[10];
        Arrays.setAll(data, i->i);
        ShuffleUtil.shuffleArray(data, random);
        assertThat(data).containsExactly(8, 0, 7, 6, 2, 1, 3, 4, 9, 5);
    }

}
