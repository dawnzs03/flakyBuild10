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
package org.neo4j.gds.core.cypher;

import org.neo4j.gds.api.GraphStoreWrapper;
import org.neo4j.gds.config.GraphProjectConfig;
import org.neo4j.gds.core.loading.CatalogRequest;
import org.neo4j.gds.core.loading.GraphStoreCatalog;

public final class CypherGraphStoreCatalogHelper {

    private CypherGraphStoreCatalogHelper() {}

    public static void setWrappedGraphStore(GraphProjectConfig config, GraphStoreWrapper graphStoreWrapper) {
        var catalogRequest = CatalogRequest.of(config.username(), graphStoreWrapper.databaseId());
        var graphName = config.graphName();
        var graphStore = GraphStoreCatalog.get(
            catalogRequest,
            graphName
        ).graphStore();

        if (graphStore != graphStoreWrapper.innerGraphStore()) {
            throw new IllegalArgumentException("Attempted to override a graph store with an incompatible graph store wrapper.");
        }

        GraphStoreCatalog.overwrite(config, graphStoreWrapper);
    }
}
