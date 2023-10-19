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
package org.neo4j.gds.api;

import java.util.stream.Stream;

public interface RelationshipIterator extends RelationshipPredicate {

    /**
     * Calls the given consumer function for every relationship of a given node.
     *
     * @param nodeId id of the node for which to iterate relationships
     * @param consumer relationship consumer function
     */
    void forEachRelationship(long nodeId, RelationshipConsumer consumer);

    /**
     * Calls the given consumer function for every relationship of a given node.
     * If the graph was loaded with a relationship property, the property value
     * of the relationship will be passed into the consumer, otherwise the given
     * fallback value will be used.
     *
     * @param nodeId id of the node for which to iterate relationships
     * @param fallbackValue value used as relationship property if no properties were loaded
     * @param consumer relationship consumer function
     */
    void forEachRelationship(long nodeId, double fallbackValue, RelationshipWithPropertyConsumer consumer);

    /**
     * Calls the given consumer for every inverse relationship of a given node.
     * Inverse relationships basically mirror the relationships in that graph.
     * For example, if `Graph.forEachRelationship(42)` returns `1337` then the
     * result of `forEachInverseRelationship(1337)` contains `42. For undirected
     * graphs, accessing the inverse relationships is never supported.
     * <p>
     * Note, that this the inverse index might not always be present. Check
     * {@link org.neo4j.gds.api.Graph#characteristics()} before calling this
     * method to verify that the graphs is inverse indexed.
     *
     * @param nodeId id of the node for which to iterate the inverse relationships
     * @param consumer relationship consumer function
     */
    void forEachInverseRelationship(long nodeId, RelationshipConsumer consumer);

    /**
     * Calls the given consumer for every inverse relationship of a given node.
     * If the graph was loaded with a relationship property, the property value
     * of the relationship will be passed into the consumer, otherwise the given
     * fallback value will be used.
     * <p>
     * Inverse relationships basically mirror the relationships in that graph.
     * For example, if `Graph.forEachRelationship(42)` returns `1337` then the
     * result of `forEachInverseRelationship(1337)` contains `42. For undirected
     * graphs, accessing the inverse relationships is never supported.
     * <p>
     * Note, that this the inverse index might not always be present. Check
     * {@link org.neo4j.gds.api.Graph#characteristics()} before calling this
     * method to verify that the graphs is inverse indexed.
     *
     * @param nodeId id of the node for which to iterate the inverse relationships
     * @param fallbackValue value used as relationship property if no properties were loaded
     * @param consumer relationship consumer function
     */
    void forEachInverseRelationship(long nodeId, double fallbackValue, RelationshipWithPropertyConsumer consumer);

    /**
     * Returns a stream which returns every relationship in the graph starting
     * from the given node id. The id space returned for source and target nodes is the
     * internal id space with respect to the graph this method was called on.
     * <p>
     * The object returned by the stream is a single instance of {@link org.neo4j.gds.api.RelationshipCursor}.
     * The same instance is used for every element in the stream and should thus not be
     * stored for later use but instead be consumed directly before consuming the next element of the stream.
     *
     * @param nodeId        id of the node for which to stream the relationships
     * @param fallbackValue value used as relationship property if no properties were loaded
     */
    Stream<RelationshipCursor> streamRelationships(long nodeId, double fallbackValue);

    /**
     * @return a copy of this iterator that reuses new cursors internally,
     *         so that iterations happen independent of other iterations.
     */
    default RelationshipIterator concurrentCopy() {
        return this;
    }

}
