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
import org.neo4j.gds.ml.core.tensor.Matrix;
import org.neo4j.gds.ml.core.tensor.Scalar;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConstantScaleTest extends ComputationGraphBaseTest implements FiniteDifferenceTest {
    @Test
    void testApply() {
        Weights<Matrix> matrix = new Weights<>(new Matrix(new double[]{1, 2, 3, 4}, 2, 2));
        double constant = 5.34D;
        Variable<Matrix> scaled = new ConstantScale<>(matrix, constant);

        assertThat(ctx.forward(scaled))
            .isEqualTo(new Matrix(new double[]{constant, 2 * constant, 3 * constant, 4 * constant}, 2, 2));
    }


    @Test
    void shouldApproximateGradient() {
        Weights<Matrix> matrix = new Weights<>(new Matrix(new double[]{1, 2, 3, 4}, 2, 2));
        double constant = 5.34D;
        finiteDifferenceShouldApproximateGradient(
            matrix,
            new ElementSum(List.of(new ConstantScale<>(matrix, constant)))
        );
    }

    @Test
    void render() {
        var parent = new Constant<>(new Scalar(1));
        assertThat(new ConstantScale<>(parent, 4).render()).isEqualTo(
            "ConstantScale: scale by 4.0, requireGradient: false" +
            System.lineSeparator() +
            "|-- Constant: Scalar: [1.0], requireGradient: false" +
            System.lineSeparator());
    }

}
