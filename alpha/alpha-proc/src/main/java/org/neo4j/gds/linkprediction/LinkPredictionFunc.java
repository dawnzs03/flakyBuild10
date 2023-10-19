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
package org.neo4j.gds.linkprediction;

import org.neo4j.gds.BaseProc;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.neo4j.gds.config.GraphProjectFromCypherConfig.RELATIONSHIP_QUERY_KEY;
import static org.neo4j.gds.core.ProcedureConstants.DIRECTION_KEY;

public class LinkPredictionFunc extends BaseProc {

    @UserFunction("gds.alpha.linkprediction.adamicAdar")
    @Description("Given two nodes, calculate Adamic Adar similarity")
    public double adamicAdarSimilarity(@Name("node1") Node node1, @Name("node2") Node node2,
                                       @Name(value = "config", defaultValue = "{}") Map<String, Object> config) {
        // https://en.wikipedia.org/wiki/Adamic/Adar_index

        if (node1 == null || node2 == null) {
            throw new RuntimeException("Nodes must not be null");
        }

        RelationshipType relationshipType = getRelationshipType(config);
        Direction direction = getDirection(config);

        Set<Node> neighbors = new NeighborsFinder().findCommonNeighbors(node1, node2, relationshipType, direction);
        return neighbors.stream().mapToDouble(nb -> 1.0 / Math.log(degree(nb, relationshipType, direction))).sum();
    }

    @UserFunction("gds.alpha.linkprediction.resourceAllocation")
    @Description("Given two nodes, calculate Resource Allocation similarity")
    public double resourceAllocationSimilarity(@Name("node1") Node node1, @Name("node2") Node node2,
                                               @Name(value = "config", defaultValue = "{}") Map<String, Object> config) {
        // https://arxiv.org/pdf/0901.0553.pdf

        if (node1 == null || node2 == null) {
            throw new RuntimeException("Nodes must not be null");
        }

        RelationshipType relationshipType = getRelationshipType(config);
        Direction direction = getDirection(config);

        Set<Node> neighbors = new NeighborsFinder().findCommonNeighbors(node1, node2, relationshipType, direction);
        return neighbors.stream().mapToDouble(nb -> 1.0 / degree(nb, relationshipType, direction)).sum();
    }

    @UserFunction("gds.alpha.linkprediction.commonNeighbors")
    @Description("Given two nodes, returns the number of common neighbors")
    public double commonNeighbors(@Name("node1") Node node1, @Name("node2") Node node2,
                                               @Name(value = "config", defaultValue = "{}") Map<String, Object> config) {
        if (node1 == null || node2 == null) {
            throw new RuntimeException("Nodes must not be null");
        }

        RelationshipType relationshipType = getRelationshipType(config);
        Direction direction = getDirection(config);

        Set<Node> neighbors = new NeighborsFinder().findCommonNeighbors(node1, node2, relationshipType, direction);
        return neighbors.size();
    }

    @UserFunction("gds.alpha.linkprediction.preferentialAttachment")
    @Description("Given two nodes, calculate Preferential Attachment")
    public double preferentialAttachment(@Name("node1") Node node1, @Name("node2") Node node2,
                                       @Name(value = "config", defaultValue = "{}") Map<String, Object> config) {
        if (node1 == null || node2 == null) {
            throw new RuntimeException("Nodes must not be null");
        }

        RelationshipType relationshipType = getRelationshipType(config);
        Direction direction = getDirection(config);

        return degree(node1, relationshipType, direction) * degree(node2, relationshipType, direction);
    }

    @UserFunction("gds.alpha.linkprediction.totalNeighbors")
    @Description("Given two nodes, calculate Total Neighbors")
    public double totalNeighbors(@Name("node1") Node node1, @Name("node2") Node node2,
                                         @Name(value = "config", defaultValue = "{}") Map<String, Object> config) {
        RelationshipType relationshipType = getRelationshipType(config);
        Direction direction = getDirection(config);

        NeighborsFinder neighborsFinder = new NeighborsFinder();
        return neighborsFinder.findNeighbors(node1, node2, relationshipType, direction).size();
    }

    @UserFunction("gds.alpha.linkprediction.sameCommunity")
    @Description("Given two nodes, indicates if they have the same community")
    public double sameCommunity(@Name("node1") Node node1, @Name("node2") Node node2,
                                 @Name(value = "communityProperty", defaultValue = "community") String communityProperty) {
        if(!node1.hasProperty(communityProperty) || !node2.hasProperty(communityProperty)) {
            return 0.0;        }

        return node1.getProperty(communityProperty).equals(node2.getProperty(communityProperty)) ? 1.0 : 0.0;
    }

    private int degree(Node node, RelationshipType relationshipType, Direction direction) {
        return relationshipType == null ? node.getDegree(direction) : node.getDegree(relationshipType, direction);
    }

    private RelationshipType getRelationshipType(Map<String, Object> config) {
        return config.getOrDefault(RELATIONSHIP_QUERY_KEY, null) == null
            ? null
            : RelationshipType.withName((String) config.get(RELATIONSHIP_QUERY_KEY));
    }

    private Direction getDirection(Map<String, Object> config) {
        return Directions.fromString((String) config.getOrDefault(DIRECTION_KEY, Direction.BOTH.name()));
    }

    /**
     * Utility class for converting string representation used in cypher queries
     * to neo4j kernel api Direction type.
     *
     *
     * <p>
     *      String parsing is case insensitive!
     * <div>
     *     <strong>OUTGOING</strong>
     *     <ul>
     *         <li>&gt;</li>
     *         <li>o</li>
     *         <li>out</li>
     *         <li>outgoing</li>
     *     </ul>
     *     <strong>INCOMING</strong>
     *     <ul>
     *         <li>&lt;</li>
     *         <li>i</li>
     *         <li>in</li>
     *         <li>incoming</li>
     *     </ul>
     *     <strong>BOTH</strong>
     *     <ul>
     *         <li>&lt;&gt;</li>
     *         <li>b</li>
     *         <li>both</li>
     *     </ul>
     * </div>
     */
    static final class Directions {

        static final Direction DEFAULT_DIRECTION = Direction.OUTGOING;

        private Directions() {}

        static Direction fromString(String directionString) {
            return fromString(directionString, DEFAULT_DIRECTION);
        }

        static Direction fromString(String directionString, Direction defaultDirection) {

            if (null == directionString) {
                return defaultDirection;
            }

            switch (directionString.toLowerCase(Locale.ENGLISH)) {

                case "outgoing":
                case "out":
                case "o":
                case ">":
                    return Direction.OUTGOING;

                case "incoming":
                case "in":
                case "i":
                case "<":
                    return Direction.INCOMING;

                case "both":
                case "b":
                case "<>":
                    return Direction.BOTH;

                default:
                    return defaultDirection;
            }
        }

        public static String toString(Direction direction) {
            return direction.name();
        }
    }
}
