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

import org.neo4j.gds.ml.core.AbstractVariable;
import org.neo4j.gds.ml.core.ComputationContext;
import org.neo4j.gds.ml.core.Variable;
import org.neo4j.gds.ml.core.tensor.Matrix;
import org.neo4j.gds.ml.core.tensor.Tensor;
import org.neo4j.gds.ml.core.tensor.Vector;

import java.util.List;

import static org.neo4j.gds.ml.core.Dimensions.COLUMNS_INDEX;
import static org.neo4j.gds.ml.core.Dimensions.ROWS_INDEX;
import static org.neo4j.gds.utils.StringFormatting.formatWithLocale;

public class MatrixVectorSum extends AbstractVariable<Matrix> {

    private final Variable<Matrix> matrix;
    private final Variable<Vector> vector;

    public MatrixVectorSum(Variable<Matrix> matrix, Variable<Vector> vector) {
        super(List.of(matrix, vector), matrix.dimensions());
        assert matrix.dimension(COLUMNS_INDEX) == vector.dimension(ROWS_INDEX) : formatWithLocale(
            "Cannot broadcast vector with length %d to a matrix with %d columns",
            vector.dimension(ROWS_INDEX),
            matrix.dimension(COLUMNS_INDEX)
        );
        this.matrix = matrix;
        this.vector = vector;
    }

    @Override
    public Matrix apply(ComputationContext ctx) {
        return ctx.data(matrix).sumBroadcastColumnWise(ctx.data(vector));
    }

    @Override
    public Tensor<?> gradient(Variable<?> parent, ComputationContext ctx) {
        if (parent == matrix) {
            return ctx.gradient(this);
        } else {
            return ctx.gradient(this).sumPerColumn();
        }
    }
}
