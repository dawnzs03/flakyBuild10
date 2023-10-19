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
package org.neo4j.gds.ml.core.tensor;

import java.util.Arrays;

public class FloatVector {
    private final float[] data;

    public FloatVector(int size) {
        this(new float[size]);
    }

    public FloatVector(float[] data) {
        this.data = data;
    }

    public float[] data() {
        return data;
    }

    public float innerProduct(FloatVector other) {
        float result = 0;
        for (int i = 0; i < data.length; i++) {
            result += data[i] * other.data[i];
        }
        return result;
    }

    @Override
    public String toString() {
        return Arrays.toString(data);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return Arrays.equals(data, ((FloatVector) other).data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }
}
