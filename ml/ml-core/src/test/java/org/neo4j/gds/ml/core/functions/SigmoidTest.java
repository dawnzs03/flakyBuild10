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


class SigmoidTest extends ComputationGraphBaseTest implements FiniteDifferenceTest {
    @Override
    public double epsilon() {
        return 1E-8;
    }

    @Test
    void shouldApproximateGradientScalarValued() {
        Weights<Scalar> weights = new Weights<>(new Scalar(0.1));
        finiteDifferenceShouldApproximateGradient(weights, new Sigmoid<>(weights));
    }

    @Test
    void shouldApproximateGradient() {
        double[] vectorData = {-1, 5, 2};
        Weights<Vector> weights = new Weights<>(new Vector(vectorData));

        finiteDifferenceShouldApproximateGradient(weights, new ElementSum(List.of(new Sigmoid<>(weights))));
    }

    @Test
    void shouldComputeSigmoid() {
        double[] vectorData = {14, 5, 36};
        Constant<Vector> p = Constant.vector(vectorData);

        Variable<Vector> sigmoid = new Sigmoid<>(p);

        var expected = new Vector(
            (1 / (1 + Math.exp(-14))),
            (1 / (1 + Math.exp(-5))),
            (1 / (1 + Math.exp(-36)))
        );

        assertThat(ctx.forward(sigmoid)).isEqualTo(expected);
    }

    @Test
    void returnsEmptyDataForEmptyVariable() {
        var emptyVector = new Vector(0);
        var constant = new Constant<>(emptyVector);

        Variable<Vector> sigmoid = new Sigmoid<>(constant);

        assertThat(ctx.forward(sigmoid)).isEqualTo(emptyVector);
    }

}
