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

import org.neo4j.gds.core.utils.paged.HugeLongArray;

import java.util.Optional;
import java.util.SplittableRandom;

public final class ShuffleUtil {

    public static void shuffleArray(HugeLongArray data, SplittableRandom random) {
        for (long offset = 0; offset < data.size() - 1; offset++) {
            long swapWith = random.nextLong(offset, data.size());
            long tempValue = data.get(swapWith);
            data.set(swapWith, data.get(offset));
            data.set(offset, tempValue);
        }
    }

    public static void shuffleArray(int[] data, SplittableRandom random) {
        for (int offset = 0; offset < data.length - 1; offset++) {
            int swapWith = random.nextInt(offset, data.length);
            int tempValue = data[swapWith];
            data[swapWith] = data[offset];
            data[offset] = tempValue;
        }
    }

    public static SplittableRandom createRandomDataGenerator(Optional<Long> randomSeed) {
        return randomSeed.map(SplittableRandom::new).orElseGet(SplittableRandom::new);
    }

    private ShuffleUtil() {}
}
