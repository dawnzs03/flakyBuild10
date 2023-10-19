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

import org.neo4j.gds.api.DatabaseId;
import org.neo4j.gds.api.GraphLoaderContext;
import org.neo4j.gds.api.ImmutableGraphLoaderContext;
import org.neo4j.gds.compat.GraphDatabaseApiProxy;
import org.neo4j.gds.config.BaseConfig;
import org.neo4j.gds.core.CypherMapAccess;
import org.neo4j.gds.core.Username;
import org.neo4j.gds.core.loading.GraphStoreCatalog;
import org.neo4j.gds.core.loading.GraphStoreWithConfig;
import org.neo4j.gds.core.utils.TerminationFlag;
import org.neo4j.gds.core.utils.progress.TaskRegistryFactory;
import org.neo4j.gds.core.utils.warnings.UserLogRegistryFactory;
import org.neo4j.gds.executor.ExecutionContext;
import org.neo4j.gds.executor.GraphStoreFromCatalogLoader;
import org.neo4j.gds.executor.ImmutableExecutionContext;
import org.neo4j.gds.executor.MemoryUsageValidator;
import org.neo4j.gds.transaction.DatabaseTransactionContext;
import org.neo4j.gds.transaction.EmptyTransactionContext;
import org.neo4j.gds.transaction.TransactionContext;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.internal.kernel.api.procs.ProcedureCallContext;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;

import java.util.Collection;
import java.util.function.Supplier;

import static org.neo4j.gds.utils.StringFormatting.formatWithLocale;

public abstract class BaseProc {

    public static final String ESTIMATE_DESCRIPTION = "Returns an estimation of the memory consumption for that procedure.";
    public static final String STATS_DESCRIPTION = "Executes the algorithm and returns result statistics without writing the result to Neo4j.";

    @Context
    public GraphDatabaseService databaseService;

    @Context
    public Log log;

    @Context
    public Transaction procedureTransaction;

    @Context
    public KernelTransaction transaction;

    @Context
    public ProcedureCallContext callContext;

    @Context
    public TaskRegistryFactory taskRegistryFactory;

    @Context
    public UserLogRegistryFactory userLogRegistryFactory;

    @Context
    public Username username = Username.EMPTY_USERNAME;

    protected String username() {
        return username.username();
    }

    protected GraphStoreWithConfig graphStoreFromCatalog(String graphName, BaseConfig config) {
        return GraphStoreFromCatalogLoader.graphStoreFromCatalog(
            graphName,
            config,
            username(),
            databaseId(),
            isGdsAdmin()
        );
    }

    public boolean isGdsAdmin() {
        if (transaction == null) {
            // No transaction available (likely we're in a test), no-one is admin here
            return false;
        }
        // this should be the same as the predefined role from enterprise-security
        // com.neo4j.server.security.enterprise.auth.plugin.api.PredefinedRoles.ADMIN
        String PREDEFINED_ADMIN_ROLE = "admin";
        return transaction.securityContext().roles().contains(PREDEFINED_ADMIN_ROLE);
    }

    protected final void runWithExceptionLogging(String message, Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.warn(message, e);
            throw e;
        }
    }

    protected final <R> R runWithExceptionLogging(String message, Supplier<R> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.warn(message, e);
            throw e;
        }
    }

    protected final void validateConfig(CypherMapAccess cypherConfig, BaseConfig config) {
        validateConfig(cypherConfig, config.configKeys());
    }

    protected final void validateConfig(CypherMapAccess cypherConfig, Collection<String> allowedKeys) {
        cypherConfig.requireOnlyKeysFrom(allowedKeys);
    }

    protected final void validateGraphName(String username, String graphName) {
        CypherMapAccess.failOnBlank("graphName", graphName);
        if (GraphStoreCatalog.exists(username, databaseId(), graphName)) {
            throw new IllegalArgumentException(formatWithLocale(
                "A graph with name '%s' already exists.",
                graphName
            ));
        }
    }

    protected GraphLoaderContext graphLoaderContext() {
        return ImmutableGraphLoaderContext.builder()
            .databaseId(databaseId())
            .dependencyResolver(GraphDatabaseApiProxy.dependencyResolver(databaseService))
            .transactionContext(transactionContext())
            .log(log)
            .taskRegistryFactory(taskRegistryFactory)
            .userLogRegistryFactory(userLogRegistryFactory)
            .terminationFlag(TerminationFlag.wrap(new TransactionTerminationMonitor(transaction)))
            .build();
    }

    public MemoryUsageValidator memoryUsageValidator() {
        return new MemoryUsageValidator(log, GraphDatabaseApiProxy.dependencyResolver(databaseService));
    }

    public ExecutionContext executionContext() {
        return databaseService == null
            ? ExecutionContext.EMPTY
            : ImmutableExecutionContext
                .builder()
                .databaseId(databaseId())
                .dependencyResolver(GraphDatabaseApiProxy.dependencyResolver(databaseService))
                .log(log)
                .returnColumns(new ProcedureCallContextReturnColumns(callContext))
                .userLogRegistryFactory(userLogRegistryFactory)
                .taskRegistryFactory(taskRegistryFactory)
                .username(username())
                .terminationMonitor(new TransactionTerminationMonitor(transaction))
                .closeableResourceRegistry(new TransactionCloseableResourceRegistry(transaction))
                .algorithmMetaDataSetter(new TransactionAlgorithmMetaDataSetter(transaction))
                .nodeLookup(new TransactionNodeLookup(transaction))
                .isGdsAdmin(transactionContext().isGdsAdmin())
                .build();
    }

    public TransactionContext transactionContext() {
        return databaseService == null
            ? EmptyTransactionContext.INSTANCE
            : DatabaseTransactionContext.of(databaseService, procedureTransaction);
    }

    private DatabaseId databaseId() {
        return DatabaseId.of(databaseService);
    }
}
