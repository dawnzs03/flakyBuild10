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
package org.neo4j.gds;

import org.neo4j.gds.api.IntBinaryPredicate;
import org.neo4j.gds.api.RelationshipConsumer;
import org.neo4j.gds.api.RelationshipWithPropertyConsumer;

/**
 * Warning: These conversions are not safe but will fail for very large graphs.
 * These are to be used by algorithms that migrate to the new SPI but are based on integers.
 * The same limitations apply for those algorithms as before, but failures for very large graphs will be contained in here.
 */
public interface Converters {

    static RelationshipConsumer longToIntConsumer(IntBinaryPredicate p) {
        return (sourceNodeId, targetNodeId) -> {
            int s = Math.toIntExact(sourceNodeId);
            int t = Math.toIntExact(targetNodeId);
            return p.test(s, t);
        };
    }

    static RelationshipWithPropertyConsumer longToIntConsumer(IntIntDoublePredicate p) {
        return ((sourceNodeId, targetNodeId, property) -> {
            int s = Math.toIntExact(sourceNodeId);
            int t = Math.toIntExact(targetNodeId);
            return p.test(s, t, property);
        });
    }

    @FunctionalInterface
    interface IntIntDoublePredicate {
        boolean test(int i, int j, double d);
    }

}
