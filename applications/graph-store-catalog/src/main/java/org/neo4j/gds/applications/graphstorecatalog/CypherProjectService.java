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
package org.neo4j.gds.applications.graphstorecatalog;

import org.neo4j.gds.api.DatabaseId;
import org.neo4j.gds.config.GraphProjectConfig;
import org.neo4j.gds.config.GraphProjectFromCypherConfig;
import org.neo4j.gds.core.loading.GraphProjectCypherResult;
import org.neo4j.gds.core.utils.TerminationFlag;
import org.neo4j.gds.core.utils.progress.TaskRegistryFactory;
import org.neo4j.gds.core.utils.warnings.UserLogRegistryFactory;
import org.neo4j.gds.results.MemoryEstimateResult;
import org.neo4j.gds.transaction.TransactionContext;

public class CypherProjectService {
    private final GenericProjectService<
        GraphProjectCypherResult,
        GraphProjectFromCypherConfig,
        GraphProjectCypherResult.Builder> genericProjectService;

    private final GraphProjectMemoryUsage graphProjectMemoryUsage;

    public CypherProjectService(
        GenericProjectService<
            GraphProjectCypherResult,
            GraphProjectFromCypherConfig,
            GraphProjectCypherResult.Builder> genericProjectService, GraphProjectMemoryUsage graphProjectMemoryUsage
    ) {
        this.genericProjectService = genericProjectService;
        this.graphProjectMemoryUsage = graphProjectMemoryUsage;
    }

    public GraphProjectCypherResult project(
        DatabaseId databaseId,
        TaskRegistryFactory taskRegistryFactory,
        TerminationFlag terminationFlag,
        TransactionContext transactionContext,
        UserLogRegistryFactory userLogRegistryFactory,
        GraphProjectFromCypherConfig configuration
    ) {
        return genericProjectService.project(
            databaseId,
            taskRegistryFactory,
            terminationFlag,
            transactionContext,
            userLogRegistryFactory,
            configuration
        );
    }

    public MemoryEstimateResult estimate(
        DatabaseId databaseId,
        TaskRegistryFactory taskRegistryFactory,
        TerminationFlag terminationFlag,
        TransactionContext transactionContext,
        UserLogRegistryFactory userLogRegistryFactory,
        GraphProjectConfig configuration
    ) {
        if (configuration.isFictitiousLoading()) return estimateButFictitiously(configuration);

        var memoryTreeWithDimensions = graphProjectMemoryUsage.getEstimate(
            databaseId,
            terminationFlag,
            transactionContext,
            taskRegistryFactory,
            userLogRegistryFactory,
            configuration
        );

        return new MemoryEstimateResult(memoryTreeWithDimensions);
    }

    /**
     * Public because EstimationCLI tests needs it. Should redesign something here I think
     */
    public MemoryEstimateResult estimateButFictitiously(GraphProjectConfig configuration) {
        var estimate = graphProjectMemoryUsage.getFictitiousEstimate(configuration);

        return new MemoryEstimateResult(estimate);
    }
}
