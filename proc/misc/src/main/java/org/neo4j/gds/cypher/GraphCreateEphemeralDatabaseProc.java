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
package org.neo4j.gds.cypher;

import org.apache.commons.lang3.mutable.MutableLong;
import org.neo4j.common.Edition;
import org.neo4j.dbms.api.DatabaseManagementException;
import org.neo4j.gds.catalog.CatalogProc;
import org.neo4j.gds.compat.StorageEngineProxy;
import org.neo4j.gds.core.utils.ProgressTimer;
import org.neo4j.gds.executor.Preconditions;
import org.neo4j.gds.storageengine.InMemoryDatabaseCreator;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.stream.Stream;

import static org.neo4j.gds.utils.StringFormatting.formatWithLocale;
import static org.neo4j.procedure.Mode.READ;

public class GraphCreateEphemeralDatabaseProc extends CatalogProc {

    private static final String DESCRIPTION = "Creates an ephemeral database from a GDS graph.";

    @Procedure(name = "gds.ephemeral.database.create", mode = READ)
    @Description(DESCRIPTION)
    public Stream<CreateEphemeralDbResult> createInMemoryDatabase(
        @Name(value = "dbName") String dbName,
        @Name(value = "graphName") String graphName
    ) {
        Preconditions.check();
        validateGraphName(graphName);

        CreateEphemeralDbResult result = runWithExceptionLogging(
            "In-memory Cypher database creation failed",
            () -> {
                validateNeo4jEnterpriseEdition(databaseService);
                MutableLong createMillis = new MutableLong(0);
                try (ProgressTimer ignored = ProgressTimer.start(createMillis::setValue)) {
                    InMemoryDatabaseCreator.createDatabase(databaseService, username(), graphName, dbName);
                }

                return new CreateEphemeralDbResult(dbName, graphName, createMillis.getValue());
            }
        );

        return Stream.of(result);
    }

    @Procedure(name = "gds.alpha.create.cypherdb", mode = READ, deprecatedBy = "gds.ephemeral.database.create")
    @Description(DESCRIPTION)
    public Stream<CreateEphemeralDbResult> createDb(
        @Name(value = "dbName") String dbName,
        @Name(value = "graphName") String graphName
    ) {
        return createInMemoryDatabase(dbName, graphName);
    }

    @SuppressWarnings("unused")
    public static class CreateEphemeralDbResult {
        public final String dbName;
        public final String graphName;
        public final long createMillis;

        public CreateEphemeralDbResult(String dbName, String graphName, long createMillis) {
            this.dbName = dbName;
            this.graphName = graphName;
            this.createMillis = createMillis;
        }
    }

    static void validateNeo4jEnterpriseEdition(GraphDatabaseService databaseService) {
        var edition = StorageEngineProxy.dbmsEdition(databaseService);
        if (!(edition == Edition.ENTERPRISE)) {
            throw new DatabaseManagementException(formatWithLocale(
                "Requires Neo4j %s version, but found %s",
                Edition.ENTERPRISE,
                edition
            ));
        }
    }
}
