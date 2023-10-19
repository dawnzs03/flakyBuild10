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
package org.neo4j.gds.catalog;

import org.neo4j.gds.api.DatabaseId;
import org.neo4j.gds.api.ProcedureReturnColumns;
import org.neo4j.gds.api.User;
import org.neo4j.gds.applications.graphstorecatalog.GraphStoreCatalogBusinessFacade;
import org.neo4j.gds.core.loading.GraphProjectCypherResult;
import org.neo4j.gds.core.loading.GraphProjectNativeResult;
import org.neo4j.gds.core.loading.GraphProjectSubgraphResult;
import org.neo4j.gds.core.utils.warnings.UserLogEntry;
import org.neo4j.gds.logging.Log;
import org.neo4j.gds.results.MemoryEstimateResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.internal.kernel.api.security.SecurityContext;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * The top layer for the Neo4j integration side:
 * thin, dumb procedure stubs can have this context-injected and call exactly one method,
 * passing user input and an appropriate output marshaller.
 * <p>
 * The output marshaller determines how easy or not it will be to generate stubs, but let's take that another day.
 * <p>
 * Take the graph exists _function_: logically it is a quad of {"gds.graph.exists", READ, input string, output boolean},
 * everything else is details. Contrast with the graph-exists _procedure_, same as the function,
 * but output gets marshalled in a bespoke fashion.
 * <p>
 * Baby steps: we start here, extracting business logic, structuring marshalling,
 * getting a handle on parameters vs dependencies.
 * <p>
 * Note that we take in _only_ parameters. That's because everything else is a dependency (maybe not the best name),
 * but certainly something that is not necessary for the stubs to consider;
 * this facade will be an extension and can grab/ initialise/ resolve anything except the things the user passes in.
 * Username for example, turns out we resolve that, so no need to consider it a parameter.
 * Nice lovely decoupling innit when we can just focus on business logic.
 * <p>
 * Note (to self especially because I keep getting confused) that this is _request scoped_,
 * i.e. it gets newed up with a fresh @{@link org.neo4j.procedure.Context} all the time.
 */
public class GraphStoreCatalogProcedureFacade {
    // services
    private final DatabaseIdService databaseIdService;
    private final GraphDatabaseService graphDatabaseService;
    private final KernelTransactionService kernelTransactionService;
    private final Log log;
    private final ProcedureReturnColumns procedureReturnColumns;
    private final ProcedureTransactionService procedureTransactionService;
    private final SecurityContext securityContext;
    private final TaskRegistryFactoryService taskRegistryFactoryService;
    private final TerminationFlagService terminationFlagService;
    private final TransactionContextService transactionContextService;
    private final UserLogServices userLogServices;
    private final UserServices userServices;

    // business facade
    private final GraphStoreCatalogBusinessFacade businessFacade;

    public GraphStoreCatalogProcedureFacade(
        DatabaseIdService databaseIdService,
        GraphDatabaseService graphDatabaseService,
        KernelTransactionService kernelTransactionService,
        Log log,
        ProcedureReturnColumns procedureReturnColumns,
        ProcedureTransactionService procedureTransactionService,
        SecurityContext securityContext,
        TaskRegistryFactoryService taskRegistryFactoryService,
        TerminationFlagService terminationFlagService,
        TransactionContextService transactionContextService,
        UserLogServices userLogServices,
        UserServices userServices,
        GraphStoreCatalogBusinessFacade businessFacade
    ) {
        this.databaseIdService = databaseIdService;
        this.graphDatabaseService = graphDatabaseService;
        this.kernelTransactionService = kernelTransactionService;
        this.log = log;
        this.procedureReturnColumns = procedureReturnColumns;
        this.procedureTransactionService = procedureTransactionService;
        this.securityContext = securityContext;
        this.taskRegistryFactoryService = taskRegistryFactoryService;
        this.terminationFlagService = terminationFlagService;
        this.transactionContextService = transactionContextService;
        this.userLogServices = userLogServices;
        this.userServices = userServices;

        this.businessFacade = businessFacade;
    }

    /**
     * Discussion: this is used by two stubs, with different output marshalling functions.
     * <p>
     * We know we should test {@link #graphExists(String)} in isolation because combinatorials.
     * <p>
     * Do we test the output marshallers?
     * <p>
     * Well if we need confidence, not for just box ticking.
     * Neo4j Procedure Framework requires POJOs of a certain shape,
     * so there is scope for writing ridiculous amounts of code if you fancy ticking boxes.
     */
    @SuppressWarnings("WeakerAccess")
    public <RETURN_TYPE> RETURN_TYPE graphExists(String graphName, Function<Boolean, RETURN_TYPE> outputMarshaller) {
        var graphExists = graphExists(graphName);

        return outputMarshaller.apply(graphExists);
    }

    boolean graphExists(String graphName) {
        // stripping off Neo4j bits
        var user = user();
        var databaseId = databaseId();

        // no static access! we want to be able to test this stuff
        return businessFacade.graphExists(user, databaseId, graphName);
    }

    /**
     * Huh, we never did jobId filtering...
     */
    public Stream<UserLogEntry> queryUserLog(String jobId) {
        var userLogStore = userLogServices.getUserLogStore(databaseId());

        return userLogStore.query(user().getUsername());
    }

    /**
     * @param failIfMissing enable validation that graphs exist before dropping them
     * @param databaseName  optional override
     * @param username      optional override
     * @throws IllegalArgumentException if a database name was null or blank or not a String
     */
    public Stream<GraphInfo> dropGraph(
        Object graphNameOrListOfGraphNames,
        boolean failIfMissing,
        String databaseName,
        String username
    ) throws IllegalArgumentException {
        var databaseId = databaseId();
        var user = user();

        var results = businessFacade.dropGraph(
            graphNameOrListOfGraphNames,
            failIfMissing,
            databaseName,
            username,
            databaseId,
            user
        );

        // we convert here from domain type to Neo4j display type
        return results.stream().map(gswc -> GraphInfo.withoutMemoryUsage(
            gswc.config(),
            gswc.graphStore()
        ));
    }

    public Stream<GraphInfoWithHistogram> listGraphs(String graphName) {
        graphName = validateValue(graphName);

        var user = user();
        var displayDegreeDistribution = procedureReturnColumns.contains("degreeDistribution");
        var terminationFlag = terminationFlagService.terminationFlag(kernelTransactionService);

        var results = businessFacade.listGraphs(user, graphName, displayDegreeDistribution, terminationFlag);

        // we convert here from domain type to Neo4j display type
        var computeGraphSize = procedureReturnColumns.contains("memoryUsage")
            || procedureReturnColumns.contains("sizeInBytes");
        return results.stream().map(p -> GraphInfoWithHistogram.of(
            p.getLeft().config(),
            p.getLeft().graphStore(),
            p.getRight(),
            computeGraphSize
        ));
    }

    public Stream<GraphProjectNativeResult> nativeProject(
        String graphName,
        Object nodeProjection,
        Object relationshipProjection,
        Map<String, Object> configuration
    ) {
        var user = user();
        var databaseId = databaseId();

        var taskRegistryFactory = taskRegistryFactoryService.getTaskRegistryFactory(databaseId, user);
        var terminationFlag = terminationFlagService.terminationFlag(kernelTransactionService);
        var transactionContext = transactionContextService.transactionContext(
            graphDatabaseService,
            procedureTransactionService
        );
        var userLogRegistryFactory = userLogServices.getUserLogRegistryFactory(databaseId, user);

        var result = businessFacade.nativeProject(
            user,
            databaseId,
            taskRegistryFactory,
            terminationFlag,
            transactionContext,
            userLogRegistryFactory,
            graphName,
            nodeProjection,
            relationshipProjection,
            configuration
        );

        // the fact that it is a stream is just a Neo4j Procedure Framework convention
        return Stream.of(result);
    }

    public Stream<MemoryEstimateResult> estimateNativeProject(
        Object nodeProjection,
        Object relationshipProjection,
        Map<String, Object> configuration
    ) {
        var user = user();
        var databaseId = databaseId();

        var taskRegistryFactory = taskRegistryFactoryService.getTaskRegistryFactory(databaseId, user);
        var terminationFlag = terminationFlagService.terminationFlag(kernelTransactionService);
        var transactionContext = transactionContextService.transactionContext(
            graphDatabaseService,
            procedureTransactionService
        );
        var userLogRegistryFactory = userLogServices.getUserLogRegistryFactory(databaseId, user);

        var result = businessFacade.estimateNativeProject(
            databaseId,
            taskRegistryFactory,
            terminationFlag,
            transactionContext,
            userLogRegistryFactory,
            nodeProjection,
            relationshipProjection,
            configuration
        );

        return Stream.of(result);
    }

    public Stream<GraphProjectCypherResult> cypherProject(
        String graphName,
        String nodeQuery,
        String relationshipQuery,
        Map<String, Object> configuration
    ) {
        var user = user();
        var databaseId = databaseId();

        var taskRegistryFactory = taskRegistryFactoryService.getTaskRegistryFactory(databaseId, user);
        var terminationFlag = terminationFlagService.terminationFlag(kernelTransactionService);
        var transactionContext = transactionContextService.transactionContext(
            graphDatabaseService,
            procedureTransactionService
        );
        var userLogRegistryFactory = userLogServices.getUserLogRegistryFactory(databaseId, user);

        var result = businessFacade.cypherProject(
            user,
            databaseId,
            taskRegistryFactory,
            terminationFlag,
            transactionContext,
            userLogRegistryFactory,
            graphName,
            nodeQuery,
            relationshipQuery,
            configuration
        );

        return Stream.of(result);
    }

    public Stream<MemoryEstimateResult> estimateCypherProject(
        String nodeQuery,
        String relationshipQuery,
        Map<String, Object> configuration
    ) {
        var user = user();
        var databaseId = databaseId();

        var taskRegistryFactory = taskRegistryFactoryService.getTaskRegistryFactory(databaseId, user);
        var terminationFlag = terminationFlagService.terminationFlag(kernelTransactionService);
        var transactionContext = transactionContextService.transactionContext(
            graphDatabaseService,
            procedureTransactionService
        );
        var userLogRegistryFactory = userLogServices.getUserLogRegistryFactory(databaseId, user);

        var result = businessFacade.estimateCypherProject(
            databaseId,
            taskRegistryFactory,
            terminationFlag,
            transactionContext,
            userLogRegistryFactory,
            nodeQuery,
            relationshipQuery,
            configuration
        );

        return Stream.of(result);
    }

    public Stream<GraphProjectSubgraphResult> subGraphProject(
        String graphName,
        String originGraphName,
        String nodeFilter,
        String relationshipFilter,
        Map<String, Object> configuration
    ) {
        var user = user();
        var databaseId = databaseId();
        var taskRegistryFactory = taskRegistryFactoryService.getTaskRegistryFactory(databaseId, user);
        var userLogRegistryFactory = userLogServices.getUserLogRegistryFactory(databaseId, user);

        var result = businessFacade.subGraphProject(
            user,
            databaseId,
            taskRegistryFactory,
            userLogRegistryFactory,
            graphName,
            originGraphName,
            nodeFilter,
            relationshipFilter,
            configuration
        );

        return Stream.of(result);
    }

    /**
     * We have to potentially unstack the placeholder. This is purely a Neo4j Procedure framework concern.
     */
    private String validateValue(String graphName) {
        if (GraphCatalogProcedureConstants.NO_VALUE_PLACEHOLDER.equals(graphName)) return null;

        return graphName;
    }

    /**
     * We need to obtain the database id at this point in time so that we can send it down stream to business logic.
     * The database id is specific to the procedure call and/ or timing (note to self, figure out which it is).
     */
    private DatabaseId databaseId() {
        return databaseIdService.getDatabaseId(graphDatabaseService);
    }

    /**
     * The user here is request scoped, so we resolve it now and pass it down stream
     *
     * @return
     */
    private User user() {
        return userServices.getUser(securityContext);
    }
}
