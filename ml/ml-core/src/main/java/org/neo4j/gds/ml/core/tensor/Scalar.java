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

import org.neo4j.gds.ml.core.Dimensions;

public class Scalar extends Tensor<Scalar> {

    public Scalar(double value) {
        super(new double[] {value}, Dimensions.scalar());
    }

    @Override
    public Scalar createWithSameDimensions() {
        return new Scalar(0D);
    }

    @Override
    public Scalar copy() {
        return new Scalar(value());
    }

    @Override
    public Scalar add(Scalar b) {
        return new Scalar(value() + b.value());
    }

    @Override
    protected String shortDescription() {
        return "Scalar";
    }

    public double value() {
        return data[0];
    }

    public static long sizeInBytes() {
        return Tensor.sizeInBytes(Dimensions.scalar());
    }
}
