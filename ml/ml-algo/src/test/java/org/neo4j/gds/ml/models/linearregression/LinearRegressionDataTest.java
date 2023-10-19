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
package org.neo4j.gds.ml.models.linearregression;

import org.junit.jupiter.api.Test;
import org.neo4j.gds.ml.core.tensor.Matrix;

import static org.assertj.core.api.Assertions.assertThat;

class LinearRegressionDataTest {

    @Test
    void initData() {
        int featureDimension = 4;
        LinearRegressionData data = LinearRegressionData.of(featureDimension);

        assertThat(data.bias().data().value()).isZero();
        assertThat(data.weights().data()).isEqualTo(Matrix.create(0, 1, featureDimension));
        assertThat(data.featureDimension()).isEqualTo(featureDimension);
    }
}
