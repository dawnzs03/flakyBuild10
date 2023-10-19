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
package org.neo4j.gds.ml.core.tensor.operations;

import com.carrotsearch.hppc.predicates.FloatPredicate;

public final class FloatVectorOperations {

    private FloatVectorOperations() {}

    public static void addInPlace(float[] lhs, float[] rhs) {
        var length = Math.min(lhs.length, rhs.length);

        for (int i = 0; i < length; i++) {
            lhs[i] += rhs[i];
        }
    }

    public static void addWeightedInPlace(float[] lhs, float[] rhs, float weight) {
        var length = Math.min(lhs.length, rhs.length);

        for (int i = 0; i < length; i++) {
            lhs[i] += weight * rhs[i];
        }
    }

    public static void scale(float[] lhs, float scalar) {
        scale(lhs, scalar, lhs);
    }

    public static void scale(float[] lhs, float scalar, float[] out) {
        assert out.length == lhs.length;

        int length = lhs.length;
        for (int i = 0; i < length; i++) {
            out[i] = lhs[i] * scalar;
        }
    }

    public static float l2Norm(float[] data) {
        float sum = 0.0f;
        for (float value : data) {
            sum += value * value;
        }

        return (float) Math.sqrt(sum);
    }

    public static void l2Normalize(float[] array) {
        float euclideanLength = l2Norm(array);
        if (euclideanLength > 0) {
            scale(array, 1 / euclideanLength);
        }
    }

    public static boolean anyMatch(float[] vector, FloatPredicate predicate) {
        boolean anyMatch = false;
        for (float v : vector) {
            if (predicate.apply(v)) {
                anyMatch = true;
                break;
            }
        }
        return anyMatch;
    }
}
