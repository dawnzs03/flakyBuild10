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

import com.carrotsearch.hppc.IntArrayList;
import org.neo4j.gds.core.utils.mem.MemoryRange;

import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

import static org.neo4j.gds.mem.MemoryUsage.sizeOfInstance;
import static org.neo4j.gds.mem.MemoryUsage.sizeOfIntArray;
import static org.neo4j.gds.mem.MemoryUsage.sizeOfIntArrayList;

public class IntUniformSamplerByExclusion {
    private final IntUniformSamplerWithRetries samplerWithRetries;

    public IntUniformSamplerByExclusion(SplittableRandom rng) {
        this.samplerWithRetries = new IntUniformSamplerWithRetries(rng);
    }

    public static MemoryRange memoryEstimation(long numberOfSamples, long maxLowerBoundOnValidSamples) {
        var samplerWithRetriesEstimation =
            IntUniformSamplerWithRetries.memoryEstimation(Math.min(
            numberOfSamples,
            maxLowerBoundOnValidSamples - numberOfSamples
        )).union(IntUniformSamplerWithRetries.memoryEstimation(0));

        return samplerWithRetriesEstimation.add(
            MemoryRange.of(
                sizeOfInstance(IntUniformSamplerByExclusion.class) +
                sizeOfIntArray(numberOfSamples) +
                sizeOfIntArrayList(0),
                sizeOfInstance(IntUniformSamplerByExclusion.class) +
                sizeOfIntArray(numberOfSamples) +
                sizeOfIntArrayList(maxLowerBoundOnValidSamples)
            )
        );
    }

    /**
     * Sample number by excluding from the given range. This method is appropriate to call if the amount of
     * samples one wants is not much smaller than the amount of valid numbers we sample from.
     *
     * @return array of {@literal >=} max(k, lowerBoundOnValidSamplesInRange) unique samples
     */
    public int[] sample(
        final int inclusiveMin,
        final int exclusiveMax,
        int lowerBoundOnValidSamplesInRange,
        final int numberOfSamples,
        IntPredicate isInvalidSample
    ) {
        if (numberOfSamples >= lowerBoundOnValidSamplesInRange) {
            return IntStream.range(inclusiveMin, exclusiveMax).filter(l -> !isInvalidSample.test(l)).toArray();
        }

        final var validSampleSpace = new IntArrayList(lowerBoundOnValidSamplesInRange);
        for (int i = inclusiveMin; i < exclusiveMax; i++) {
            if (isInvalidSample.test(i)) continue;
            validSampleSpace.add(i);
        }

        assert validSampleSpace.size() >= numberOfSamples;

        final var samplesToRemove = samplerWithRetries.sample(
            0,
            validSampleSpace.size(),
            validSampleSpace.size(),
            validSampleSpace.size() - numberOfSamples,
            __ -> false
        );
        Arrays.sort(samplesToRemove);

        final var samples = new int[numberOfSamples];
        int count = 0;
        int nextIdxToKeep = 0;
        for (int nextIdxToRemove : samplesToRemove) {
            // Safe since samplesToRemove contain indices of an integer-indexed list
            int increment = nextIdxToRemove - nextIdxToKeep;
            System.arraycopy(validSampleSpace.buffer, nextIdxToKeep, samples, count, increment);
            nextIdxToKeep = nextIdxToRemove + 1;
            count += increment;
        }

        System.arraycopy(validSampleSpace.buffer, nextIdxToKeep, samples, count, numberOfSamples - count);

        return samples;
    }
}
