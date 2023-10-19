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
package org.neo4j.gds.triangle.intersect;

import org.eclipse.collections.api.block.function.primitive.LongToLongFunction;
import org.jetbrains.annotations.Nullable;
import org.neo4j.annotations.service.ServiceProvider;
import org.neo4j.gds.api.AdjacencyCursor;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.core.huge.CompositeAdjacencyCursor;
import org.neo4j.gds.core.huge.CompositeAdjacencyList;
import org.neo4j.gds.core.huge.UnionGraph;

import java.util.function.LongToIntFunction;

public final class UnionGraphIntersect extends GraphIntersect<CompositeAdjacencyCursor> {

    private final LongToIntFunction degreeFunction;
    private final LongToLongFunction fromFilteredIdFunction;
    private final CompositeAdjacencyList compositeAdjacencyList;

    private UnionGraphIntersect(
        LongToIntFunction degreeFunction,
        LongToLongFunction fromFilteredIdFunction,
        CompositeAdjacencyList compositeAdjacencyList,
        long maxDegree
    ) {
        super(maxDegree);
        this.degreeFunction = degreeFunction;
        this.fromFilteredIdFunction = fromFilteredIdFunction;
        this.compositeAdjacencyList = compositeAdjacencyList;
    }

    @Override
    protected int degree(long nodeId) {
        return degreeFunction.applyAsInt(nodeId);
    }

    @Override
    protected CompositeAdjacencyCursor checkCursorInstance(AdjacencyCursor cursor) {
        return (CompositeAdjacencyCursor) cursor;
    }

    @Override
    protected CompositeAdjacencyCursor cursorForNode(@Nullable CompositeAdjacencyCursor reuse, long node, int degree) {
        return compositeAdjacencyList.adjacencyCursor(reuse, fromFilteredIdFunction.applyAsLong(node));
    }

    @ServiceProvider
    public static final class UnionGraphIntersectFactory implements RelationshipIntersectFactory {

        @Override
        public boolean canLoad(Graph graph) {
            if (graph instanceof UnionGraph) {
                return !((UnionGraph) graph).isNodeFilteredGraph();
            }
            return false;
        }

        @Override
        public UnionGraphIntersect load(Graph graph, RelationshipIntersectConfig config) {
            assert graph instanceof UnionGraph;
            var topology = ((UnionGraph) graph).relationshipTopology();
            return new UnionGraphIntersect(
                graph::degree,
                i -> i,
                topology,
                config.maxDegree()
            );
        }
    }

    @ServiceProvider
    public static final class NodeFilteredUnionGraphIntersectFactory implements RelationshipIntersectFactory {

        @Override
        public boolean canLoad(Graph graph) {
            if (graph instanceof UnionGraph) {
                return ((UnionGraph) graph).isNodeFilteredGraph();
            }
            return false;
        }

        @Override
        public UnionGraphIntersect load(Graph graph, RelationshipIntersectConfig config) {
            assert graph instanceof UnionGraph;
            var topology = ((UnionGraph) graph).relationshipTopology();
            return new UnionGraphIntersect(
                graph::degree,
                graph::toRootNodeId,
                topology,
                config.maxDegree()
            );
        }
    }
}
