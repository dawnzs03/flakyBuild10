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
package org.neo4j.gds.executor;

import org.neo4j.gds.NodeLabel;
import org.neo4j.gds.RelationshipType;
import org.neo4j.gds.api.DatabaseId;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.config.AlgoBaseConfig;
import org.neo4j.gds.config.BaseConfig;
import org.neo4j.gds.config.ElementTypeValidator;
import org.neo4j.gds.config.GraphProjectConfig;
import org.neo4j.gds.core.DimensionsMap;
import org.neo4j.gds.core.GraphDimensions;
import org.neo4j.gds.core.ImmutableGraphDimensions;
import org.neo4j.gds.core.loading.GraphStoreCatalog;
import org.neo4j.gds.core.loading.GraphStoreWithConfig;
import org.neo4j.gds.core.loading.ImmutableCatalogRequest;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class GraphStoreFromCatalogLoader implements GraphStoreLoader {

    private final AlgoBaseConfig config;
    private final GraphStore graphStore;
    private final GraphProjectConfig graphProjectConfig;

    public GraphStoreFromCatalogLoader(
        String graphName,
        AlgoBaseConfig config,
        String username,
        DatabaseId databaseId,
        boolean isGdsAdmin
    ) {
        this.config = config;
        var graphStoreWithConfig = graphStoreFromCatalog(graphName, config, username, databaseId, isGdsAdmin);
        this.graphStore = graphStoreWithConfig.graphStore();
        this.graphProjectConfig = graphStoreWithConfig.config();
    }

    @Override
    public GraphProjectConfig graphProjectConfig() {
        return this.graphProjectConfig;
    }

    @Override
    public GraphStore graphStore() {
        return this.graphStore;
    }

    @Override
    public GraphDimensions graphDimensions() {
        var graphStore = graphStore();

        Collection<NodeLabel> labelFilter = ElementTypeValidator.resolve(graphStore, config.nodeLabels());
        Collection<RelationshipType> typeFilter = ElementTypeValidator.resolveTypes(
            graphStore,
            config.relationshipTypes()
        );

        // validate the filters here as well as the other validation happens after the memory estimation
        config.graphStoreValidation(graphStore, labelFilter, typeFilter);

        Graph filteredGraph = graphStore.getGraph(labelFilter, typeFilter, Optional.empty());
        long relCount = filteredGraph.relationshipCount();

        var relationshipTypeTokens = new HashMap<String, Integer>();
        var i = 0;
        for (String key : graphStore.relationshipPropertyKeys()) {
            relationshipTypeTokens.put(key, i++);
        }

        var nodePropertyDimensions = filteredGraph
            .availableNodeProperties()
            .stream()
            .collect(Collectors.toMap(
                Function.identity(),
                property -> filteredGraph
                    .nodeProperties(property)
                    .dimension()
            ));

        return ImmutableGraphDimensions.builder()
            .nodeCount(filteredGraph.nodeCount())
            .relationshipCounts(filteredGraphRelationshipCounts(typeFilter.stream(), filteredGraph))
            .relCountUpperBound(relCount)
            .relationshipPropertyTokens(relationshipTypeTokens)
            .nodePropertyDimensions(new DimensionsMap(nodePropertyDimensions))
            .build();
    }

    public static GraphStoreWithConfig graphStoreFromCatalog(
        String graphName,
        BaseConfig config,
        String username,
        DatabaseId databaseId,
        boolean isGdsAdmin
    ) {
        var request = ImmutableCatalogRequest.of(
            databaseId.databaseName(),
            username,
            config.usernameOverride(),
            isGdsAdmin
        );
        return GraphStoreCatalog.get(request, graphName);
    }

    private static Map<RelationshipType, Long> filteredGraphRelationshipCounts(
        Stream<RelationshipType> typeFilter,
        Graph filteredGraph
    ) {
        var relCount = filteredGraph.relationshipCount();
        return Stream.concat(typeFilter, Stream.of(RelationshipType.ALL_RELATIONSHIPS))
            .distinct()
            .collect(Collectors.toMap(
                    Function.identity(),
                    key -> key == RelationshipType.ALL_RELATIONSHIPS
                        ? relCount
                        : filteredGraph
                            .relationshipTypeFilteredGraph(Set.of(key))
                            .relationshipCount()
                )
            );
    }
}
