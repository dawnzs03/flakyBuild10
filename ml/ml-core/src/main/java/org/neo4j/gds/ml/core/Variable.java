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
package org.neo4j.gds.ml.core;

import org.neo4j.gds.ml.core.tensor.Tensor;

public interface Variable<T extends Tensor<T>> {
    T apply(ComputationContext ctx);

    Tensor<?> gradient(Variable<?> parent, ComputationContext ctx);

    boolean requireGradient();

    Iterable<? extends Variable<?>> parents();

    int[] dimensions();

    int dimension(int i);

    /**
     * Renders the variable into a human readable representation.
     */
    default String render() {
        StringBuilder sb = new StringBuilder();
        render(sb, this, 0);
        return sb.toString();
    }

    static  <T extends Tensor<T>> void render(StringBuilder sb, Variable<T> variable, int depth) {

        sb.append("\t".repeat(Math.max(0, depth - 1)));

        if (depth > 0) {
            sb.append("|-- ");
        }

        sb.append(variable);
        sb.append(System.lineSeparator());

        variable.parents().forEach(component -> render(sb, component, depth + 1));
    }
}
