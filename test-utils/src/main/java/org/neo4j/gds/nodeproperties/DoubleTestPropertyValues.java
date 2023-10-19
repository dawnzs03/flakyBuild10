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
package org.neo4j.gds.nodeproperties;

import org.neo4j.gds.api.properties.nodes.DoubleNodePropertyValues;

import java.util.function.LongToDoubleFunction;

public final class DoubleTestPropertyValues implements DoubleNodePropertyValues {
    private final LongToDoubleFunction transformer;

    public DoubleTestPropertyValues(LongToDoubleFunction transformer) {this.transformer = transformer;}

    @Override
    public long nodeCount() {
        return 0;
    }

    @Override
    public double doubleValue(long nodeId) {
        return transformer.applyAsDouble(nodeId);
    }
}
