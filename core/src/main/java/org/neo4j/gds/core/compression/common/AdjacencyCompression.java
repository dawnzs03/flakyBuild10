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
package org.neo4j.gds.core.compression.common;

import com.carrotsearch.hppc.sorting.IndirectSort;
import org.neo4j.gds.api.compress.AdjacencyCompressor;
import org.neo4j.gds.api.compress.LongArrayBuffer;
import org.neo4j.gds.core.Aggregation;
import org.neo4j.gds.core.utils.AscendingLongComparator;

import java.util.Arrays;

import static org.neo4j.gds.core.compression.common.VarLongDecoding.decodeDeltaVLongs;
import static org.neo4j.gds.core.compression.common.VarLongDecoding.unsafeDecodeDeltaVLongs;
import static org.neo4j.gds.core.compression.common.VarLongDecoding.unsafeDecodeVLongs;
import static org.neo4j.gds.core.compression.common.VarLongEncoding.encodeVLongs;
import static org.neo4j.gds.core.compression.common.VarLongEncoding.encodedVLongsSize;
import static org.neo4j.gds.core.compression.common.ZigZagLongDecoding.zigZagUncompress;

public final class AdjacencyCompression {

    /**
     * Decompress the given {@code array} into the given {@code into}.
     * After this, {@link org.neo4j.gds.api.compress.LongArrayBuffer#length} will reflect the number of decompressed
     * values
     * that are in the {@link org.neo4j.gds.api.compress.LongArrayBuffer#buffer}.
     */
    public static void zigZagUncompressFrom(
        LongArrayBuffer into,
        byte[] targets,
        int compressedValues,
        int limit,
        AdjacencyCompressor.ValueMapper mapper
    ) {
        into.ensureCapacity(compressedValues);
        zigZagUncompressFrom(into.buffer, targets, compressedValues, limit, mapper);
        into.length = compressedValues;
    }

    public static void zigZagUncompressFrom(
        long[] into,
        byte[] targets,
        int compressedValues,
        int limit,
        AdjacencyCompressor.ValueMapper mapper
    ) {
        assert into.length >= compressedValues;
        zigZagUncompress(targets, limit, into, mapper);
    }

    public static int applyDeltaEncoding(LongArrayBuffer data, Aggregation aggregation) {
        return data.length = applyDeltaEncoding(data.buffer, data.length, aggregation);
    }

    public static int applyDeltaEncoding(long[] data, int length, Aggregation aggregation) {
        Arrays.sort(data, 0, length);
        return deltaEncodeSortedValues(data, 0, length, aggregation);
    }

    // TODO: requires lots of additional memory ... inline indirect sort to make reuse of - to be created - buffers
    public static int applyDeltaEncoding(
        LongArrayBuffer data,
        long[][] weights,
        long[][] sortedWeights,
        Aggregation[] aggregations,
        boolean noAggregation
    ) {
        return data.length = applyDeltaEncoding(
            data.buffer,
            data.length,
            weights,
            sortedWeights,
            aggregations,
            noAggregation
        );
    }

    // TODO: requires lots of additional memory ... inline indirect sort to make reuse of - to be created - buffers
    public static int applyDeltaEncoding(
        long[] data,
        int length,
        long[][] unsortedWeights,
        long[][] sortedWeights,
        Aggregation[] aggregations,
        boolean noAggregation
    ) {
        int[] order = IndirectSort.mergesort(0, length, new AscendingLongComparator(data));

        long[] sortedValues = new long[length];

        length = applyDelta(
            order,
            data,
            sortedValues,
            unsortedWeights,
            sortedWeights,
            length,
            aggregations,
            noAggregation
        );

        System.arraycopy(sortedValues, 0, data, 0, length);

        return length;
    }

    public static byte[] ensureBufferSize(LongArrayBuffer data, byte[] out) {
        return ensureBufferSize(data.buffer, out, data.length);
    }

    static byte[] ensureBufferSize(long[] data, byte[] out, int length) {
        var requiredBytes = encodedVLongsSize(data, length);
        if (requiredBytes > out.length) {
            return new byte[requiredBytes];
        }
        return out;
    }

    public static int compress(LongArrayBuffer data, byte[] out) {
        return compress(data.buffer, out, data.length);
    }

    public static int compress(long[] data, byte[] out, int length) {
        return encodeVLongs(data, length, out, 0);
    }

    public static int compress(long[] data, int offset, int length, byte[] out) {
        return VarLongEncoding.encodeVLongs(data, offset, offset + length, out, 0);
    }

    public static long compress(long[] data, int offset, int length, long ptr) {
        return VarLongEncoding.encodeVLongs(data, offset, offset + length, ptr);
    }

    public static byte[] deltaEncodeAndCompress(long[] values, int offset, int length, Aggregation aggregation) {
        length = AdjacencyCompression.deltaEncodeSortedValues(values, offset, length, aggregation);
        var requiredBytes = VarLongEncoding.encodedVLongsSize(values, offset, length);
        var compressed = new byte[requiredBytes];
        compress(values, offset, length, compressed);
        return compressed;
    }

    public static long[] decompress(byte[] compressed, int numberOfValues) {
        long[] out = new long[numberOfValues];
        decodeDeltaVLongs(0L, compressed, 0, numberOfValues, out);
        return out;
    }

    public static long decompressAndPrefixSum(
        int numberOfValues,
        long previousValue,
        long ptr,
        long[] into,
        int offset
    ) {
        return unsafeDecodeDeltaVLongs(numberOfValues, previousValue, ptr, into, offset);
    }

    public static long decompress(int numberOfValues, long ptr, long[] into, int offset) {
        return unsafeDecodeVLongs(numberOfValues, ptr, into, offset);
    }

    public static int deltaEncodeSortedValues(long[] values, int offset, int length, Aggregation aggregation) {
        long value = values[offset], delta;
        int end = offset + length;
        int in = offset + 1, out = in;
        for (; in < end; ++in) {
            delta = values[in] - value;

            if (delta > 0L || aggregation == Aggregation.NONE) {
                value = values[in];
                values[out++] = delta;
            }

//            // branch-free alternative, I hope
//            values[out] = delta;
//
//            // for any non-zero x, the sign bit of x and -x is different,
//            // so the sign bit will be set to 1 in `x ^ -x`.
//            // Since 0 and -0 have the same representation, their sign bit is 0
//            // and the sign bit of `x ^ -x` will be 0 as well.
//            // Shifting all the bits by Long::BITS - 1 will give us only the sign bit.
//            // Or, `boolean b = (x != 0) ? 1 : 0`, but without a branch.
//            long increase = (delta ^ -delta) >> (Long.SIZE - 1);
//
//            // We also want to include the aggregation here, so we advance the out position.
//            // if the aggregation is NONE. Since we no longer expect Aggregation.DEFAULT here,
//            // we can compute the diff of the ordinals of the aggregation and Aggregation.NONE.
//            // The value will be 1 if the aggregation was *not* NONE, so we need to invert it.
//            long agg = aggregation.ordinal() - Aggregation.NONE.ordinal();
//            agg = (agg ^ -agg) >> (Long.SIZE - 1);
//            // 0: 1 - 0 = 1; 1: 1 - 1 = 0
//            agg = 1 - agg;
//
//            // compute `delta > 0L || aggregation == Aggregation.NONE`
//            // `delta > 0L`  |  `aggregation == Aggregation.NONE`  | increase ^ agg
//            //         true  |                              true   | 1 | 1 = 1
//            //         true  |                             false   | 1 | 0 = 1
//            //        false  |                              true   | 0 | 1 = 1
//            //        false  |                             false   | 0 | 0 = 0
//            increase |= agg;
//            out += increase;
        }
        return out;
    }

    /**
     * Decodes delta encoded values based on a given initial value.
     * <p>
     * A little faster due to manual unrolling. Inspired be Lemire.
     *
     * @param deltas delta encoded values
     * @param length number of values to decode
     * @param first the first decoded value
     *
     * @return the last decoded value
     */
    public static long deltaDecode(long[] deltas, int length, long first) {
        int bound = length / 4 * 4;
        int i = 0;
        if (bound >= 4) {
            for (; i < bound - 4; i += 4) {
                first = deltas[i] += first;
                first = deltas[i + 1] += first;
                first = deltas[i + 2] += first;
                first = deltas[i + 3] += first;
            }
        }

        for (; i < length; i++) {
            first = deltas[i] += first;
        }

        return first;
    }

    public static void prefixSumDeltaEncodedValues(long[] values, int length) {
        length = Math.min(values.length, length);
        long value = values[0];
        for (int idx = 1; idx < length; ++idx) {
            value = values[idx] += value;
        }
    }

    /**
     * Applies delta encoding to the given {@code values}.
     * Weights are not encoded.
     *
     * @param order           Ordered indices into {@code values} and {@code weights} for consuming these in ascending
     *                        value order.
     * @param values          Relationships represented by target node ID.
     * @param outValues       Sorted, delta-encoded and optionally aggregated relationships.
     * @param unsortedWeights Relationship properties by key, ordered by {@code order}.
     * @param sortedWeights   Sorted and optionally aggregated relationship properties.
     * @param length          Number of relationships (degree of source node) to process.
     * @param aggregations    Aggregations to apply to parallel edges. One per relationship property key in
     *                        {@code weights}.
     * @param noAggregation   Is true iff all aggregations are NONE.
     */
    private static int applyDelta(
        int[] order,
        long[] values,
        long[] outValues,
        long[][] unsortedWeights,
        long[][] sortedWeights,
        int length,
        Aggregation[] aggregations,
        boolean noAggregation
    ) {
        int firstSortIdx = order[0];
        long value = values[firstSortIdx];
        long delta;

        outValues[0] = values[firstSortIdx];
        for (int i = 0; i < unsortedWeights.length; i++) {
            sortedWeights[i][0] = unsortedWeights[i][firstSortIdx];
        }

        int in = 1, out = 1;
        for (; in < length; ++in) {
            final int sortIdx = order[in];
            delta = values[sortIdx] - value;
            value = values[sortIdx];

            if (delta > 0L || noAggregation) {
                for (int i = 0; i < unsortedWeights.length; i++) {
                    sortedWeights[i][out] = unsortedWeights[i][sortIdx];
                }
                outValues[out++] = delta;
            } else {
                for (int i = 0; i < unsortedWeights.length; i++) {
                    Aggregation aggregation = aggregations[i];
                    int existingIdx = out - 1;
                    long[] outWeight = sortedWeights[i];
                    double existingWeight = Double.longBitsToDouble(outWeight[existingIdx]);
                    double newWeight = Double.longBitsToDouble(unsortedWeights[i][sortIdx]);
                    newWeight = aggregation.merge(existingWeight, newWeight);
                    outWeight[existingIdx] = Double.doubleToLongBits(newWeight);
                }
            }
        }
        return out;
    }

    private AdjacencyCompression() {
    }
}
