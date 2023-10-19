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

import org.neo4j.gds.ml.core.ComputationContext;
import org.neo4j.gds.ml.core.Variable;
import org.neo4j.gds.ml.core.tensor.Tensor;
// Leaky Relu
public class Relu<T extends Tensor<T>> extends SingleParentVariable<T, T> {

    private static final double ALPHA = 0.01;
    private double alpha;

    public Relu(Variable<T> parent, double alpha) {
        super(parent, parent.dimensions());
        this.alpha = alpha;
    }
    public Relu(Variable<T> parent) {
        this(parent, ALPHA);
    }

    @Override
    public T apply(ComputationContext ctx) {
        return ctx.data(parent).map(value -> (value > 0) ? value : (alpha * value));
    }

    @Override
    public T gradientForParent(ComputationContext ctx) {
        T gradient = ctx.data(parent).map(value -> value > 0 ? 1 : alpha);
        gradient.elementwiseProductMutate(ctx.gradient(this));
        return gradient;
    }
}
