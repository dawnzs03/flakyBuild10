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
package org.neo4j.gds.config;

import org.neo4j.gds.NodeLabel;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.api.IdMap;
import org.neo4j.gds.utils.StringJoining;

import java.util.Collection;
import java.util.stream.Collectors;

import static org.neo4j.gds.utils.StringFormatting.formatWithLocale;

final class ConfigNodesValidations {

    private ConfigNodesValidations() {}

    static void validateNodes(
        GraphStore graphStore,
        Collection<Long> neoNodesToValidate,
        Collection<NodeLabel> filteredNodeLabels,
        String nodeDescription
    ) {
        if (!neoNodesToValidate.isEmpty()) {

            for (var neoNode : neoNodesToValidate) {

                if (neoNode < 0) {
                    throw new IllegalArgumentException(formatWithLocale(
                        "Negative node ids are not supported for the field `%s`",
                        nodeDescription
                    ));
                }
            }

            var missingNodes = neoNodesToValidate
                .stream()
                .filter(targetNode -> labelFilteredGraphNotContainsNode(
                    filteredNodeLabels,
                    graphStore.nodes(),
                    targetNode
                ))
                .map(Object::toString)
                .collect(Collectors.toList());

            if (!missingNodes.isEmpty()) {
                throw new IllegalArgumentException(formatWithLocale(
                    "%s nodes do not exist in the in-memory graph%s: %s",
                    nodeDescription,
                    nodeLabelFilterDescription(filteredNodeLabels, graphStore),
                    StringJoining.join(missingNodes)
                ));
            }

        }
    }

    static boolean labelFilteredGraphNotContainsNode(
        Collection<NodeLabel> filteredNodeLabels,
        IdMap idMap,
        long neoNodeId
    ) {
        var internalNodeId = idMap.safeToMappedNodeId(neoNodeId);
        return internalNodeId == IdMap.NOT_FOUND || idMap
            .nodeLabels(internalNodeId)
            .stream()
            .noneMatch(filteredNodeLabels::contains);
    }

    static String nodeLabelFilterDescription(Collection<NodeLabel> filteredNodeLabels, GraphStore graphStore) {
        return filteredNodeLabels.containsAll(graphStore.nodeLabels())
            ? ""
            : " for the labels " + StringJoining.join(filteredNodeLabels.stream().map(NodeLabel::name));
    }
}
