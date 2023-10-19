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
package org.neo4j.gds.ml.core.samplers;

import com.carrotsearch.hppc.LongHashSet;
import org.apache.commons.lang3.mutable.MutableInt;
import org.neo4j.gds.api.RelationshipCursor;

import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/*
 * L Algorithm for uniform sampling exactly k elements from an input stream
 * https://richardstartin.github.io/posts/reservoir-sampling#algorithm-l
 *
 */
public class UniformSampler {

    private final SplittableRandom random;

    public UniformSampler(long randomSeed) {
        this.random = new SplittableRandom(randomSeed);
    }

    public LongStream sample(Stream<RelationshipCursor> relationshipCursorStream, long inputSize, int numberOfSamples) {
        return this.sample(
            relationshipCursorStream.mapToLong(RelationshipCursor::targetId),
            inputSize,
            numberOfSamples
        );
    }

    public LongStream sample(LongStream input, long lowerBoundInputLength, int numberOfSamples) {
        if ((double) numberOfSamples / lowerBoundInputLength < 0.5) {
            return sampleWithIndexes(input, lowerBoundInputLength, numberOfSamples);
        } else {
            return sampleWithReservoir(input, lowerBoundInputLength, numberOfSamples);
        }
    }

    public LongStream sampleWithReservoir(LongStream input, long lowerBoundInputLength, int numberOfSamples) {
        if (numberOfSamples == 0) {
            return LongStream.empty();
        }

        if (numberOfSamples >= lowerBoundInputLength) {
            return input;
        }

        long[] reservoir = new long[numberOfSamples];

        var inputIterator = input.iterator();

        for (int i = 0; i < numberOfSamples; i++) {
            reservoir[i] = inputIterator.nextLong();
        }

        var nextIdxToSample = numberOfSamples - 1;
        // `w` in original Algorithm L
        var skipFactor = computeSkipFactor(numberOfSamples);

        // compute first skip
        nextIdxToSample += computeNumberOfSkips(skipFactor);
        skipFactor *= computeSkipFactor(numberOfSamples);

        for (int idx = numberOfSamples; inputIterator.hasNext(); idx++) {
            var inputValue = inputIterator.nextLong();
            if (idx == nextIdxToSample) {
                reservoir[random.nextInt(numberOfSamples)] = inputValue;
                // compute next value
                nextIdxToSample += computeNumberOfSkips(skipFactor);
                skipFactor *= computeSkipFactor(numberOfSamples);
            }
        }

        return Arrays.stream(reservoir);
    }

    private double computeSkipFactor(int numberOfSamples) {
        return Math.exp(Math.log(random.nextDouble()) / numberOfSamples);
    }

    private long computeNumberOfSkips(double w) {
        return (long) (Math.log(random.nextDouble()) / Math.log(1 - w)) + 1;
    }

    public LongHashSet sampleUniqueNumbersHashSet(int m, long n) {
        if (m > n) {
            throw new IllegalArgumentException("Cannot sample more unique numbers than the range allows.");
        }
        var uniqueNumbers = new LongHashSet();
        if (n == m) {
            for (long i = 0; i < n; i++) {
                uniqueNumbers.add(i);
            }
            return uniqueNumbers;
        }

        while (uniqueNumbers.size() < m) {
            long randomNumber = random.nextLong(n);
            uniqueNumbers.add(randomNumber);
        }

        return uniqueNumbers;
    }

    public LongStream sampleWithIndexes(LongStream input, long lowerBoundInputLength, int numberOfSamples) {
        if (numberOfSamples == 0) {
            return LongStream.empty();
        }

        if (numberOfSamples >= lowerBoundInputLength) {
            return input;
        }

        var sampledIndexes = sampleUniqueNumbersHashSet(numberOfSamples,  lowerBoundInputLength);

        MutableInt counter = new MutableInt(0);
        return input.filter(value -> sampledIndexes.contains(counter.getAndIncrement()));
    }
}
