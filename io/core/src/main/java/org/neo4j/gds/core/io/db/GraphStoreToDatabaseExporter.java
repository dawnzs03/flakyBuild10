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
package org.neo4j.gds.core.io.db;

import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.core.io.GraphStoreExporter;
import org.neo4j.gds.core.io.GraphStoreInput;
import org.neo4j.gds.core.io.NeoNodeProperties;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.logging.Log;

import java.util.Optional;

public final class GraphStoreToDatabaseExporter extends GraphStoreExporter<GraphStoreToDatabaseExporterConfig> {

    private final GdsParallelBatchImporter parallelBatchImporter;

    public static GraphStoreToDatabaseExporter of(
        GraphStore graphStore,
        GraphDatabaseService databaseService,
        GraphStoreToDatabaseExporterConfig config,
        Log log,
        ProgressTracker progressTracker
    ) {
        return of(graphStore, databaseService, config, Optional.empty(), log, progressTracker);
    }

    public static GraphStoreToDatabaseExporter of(
        GraphStore graphStore,
        GraphDatabaseService databaseService,
        GraphStoreToDatabaseExporterConfig config,
        Optional<NeoNodeProperties> neoNodeProperties,
        Log log,
        ProgressTracker progressTracker
    ) {
        return new GraphStoreToDatabaseExporter(graphStore, databaseService, config, neoNodeProperties, log, progressTracker);
    }

    private GraphStoreToDatabaseExporter(
        GraphStore graphStore,
        GraphDatabaseService databaseService,
        GraphStoreToDatabaseExporterConfig config,
        Optional<NeoNodeProperties> neoNodeProperties,
        Log log,
        ProgressTracker progressTracker
    ) {
        super(graphStore, config, neoNodeProperties, false);
        var executionMonitor = new ProgressTrackerExecutionMonitor(
            graphStore,
            progressTracker,
            config.toBatchImporterConfig()
        );
        this.parallelBatchImporter = GdsParallelBatchImporter.fromDb(databaseService, config, log, executionMonitor);
    }

    @Override
    public void export(GraphStoreInput graphStoreInput) {
        parallelBatchImporter.writeDatabase(graphStoreInput, false);
    }

    @Override
    protected IdMappingType idMappingType() {
        return IdMappingType.MAPPED;
    }
}
