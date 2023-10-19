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
package org.neo4j.gds.ml.core.functions;

import org.junit.jupiter.api.Test;
import org.neo4j.gds.ml.core.FiniteDifferenceTest;
import org.neo4j.gds.ml.core.Variable;
import org.neo4j.gds.ml.core.tensor.Scalar;
import org.neo4j.gds.ml.core.tensor.Vector;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ReluTest extends ComputationGraphBaseTest implements FiniteDifferenceTest {
    @Override
    public double epsilon() {
        return 1E-8;
    }

    @Test
    void shouldApproximateGradientScalarValued() {
        Weights<Scalar> weights = new Weights<>(new Scalar(0.1));

        finiteDifferenceShouldApproximateGradient(weights, new Relu<>(weights));
    }

    @Test
    void shouldApproximateGradient() {
        double[] vectorData = {-1, 5, 2};
        Weights<Vector> weights = new Weights<>(new Vector(vectorData));

        finiteDifferenceShouldApproximateGradient(weights, new ElementSum(List.of(new Relu<>(weights))));
    }

    @Test
    void considerSelfGradient() {
        Weights<Vector> weights = new Weights<>(new Vector(-1, 5, 2));
        var chainedRelu = new Sigmoid<>(new Relu<>(weights));

        finiteDifferenceShouldApproximateGradient(weights, new ElementSum(List.of(chainedRelu)));
    }

    @Test
    void shouldComputeRelu() {
        double[] vectorData = {14, -5, 36, 0};
        Constant<Vector> p = Constant.vector(vectorData);

        Variable<Vector> relu = new Relu<>(p);

        var expected = new Vector(14, 0.01 * -5, 36, 0);
        assertThat(ctx.forward(relu)).isEqualTo(expected);
    }

    @Test
    void returnsEmptyDataForEmptyVariable() {
        double[] vectorData = {};
        Constant<Vector> p = Constant.vector(vectorData);

        Variable<Vector> relu = new Relu<>(p);

        var expected = new Vector();
        assertThat(ctx.forward(relu)).isEqualTo(expected);
    }

}
