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

import org.neo4j.common.DependencyResolver;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.core.utils.progress.tasks.Task;
import org.neo4j.gds.core.utils.progress.tasks.Tasks;
import org.neo4j.internal.batchimport.Configuration;
import org.neo4j.internal.batchimport.staging.CoarseBoundedProgressExecutionMonitor;
import org.neo4j.internal.batchimport.staging.StageExecution;

import static org.neo4j.gds.utils.StringFormatting.formatWithLocale;


public final class ProgressTrackerExecutionMonitor extends CoarseBoundedProgressExecutionMonitor {

    private final ProgressTracker progressTracker;


    public static Task progressTask(GraphStore graphStore) {
        return Tasks.leaf(
            GraphStoreToDatabaseExporter.class.getSimpleName(),
            graphStore.nodes().nodeCount() + graphStore.relationshipCount()
        );
    }

    ProgressTrackerExecutionMonitor(
        GraphStore graphStore,
        ProgressTracker progressTracker,
        Configuration config
    ) {
        super(graphStore.nodeCount(), graphStore.relationshipCount(), config);
        this.progressTracker = progressTracker;
    }

    @Override
    public void initialize(DependencyResolver dependencyResolver) {
        this.progressTracker.beginSubTask();
        this.progressTracker.setVolume(this.total());
    }

    @Override
    public void start(StageExecution execution) {
        super.start(execution);
        progressTracker.logInfo(formatWithLocale("%s :: Start", execution.getStageName()));
    }

    @Override
    public void end(StageExecution execution, long totalTimeMillis) {
        super.end(execution, totalTimeMillis);
        progressTracker.logInfo(formatWithLocale("%s :: Finished", execution.getStageName()));
    }

    @Override
    protected void progress(long progress) {
        this.progressTracker.logProgress(progress);
    }

    @Override
    public void done(boolean successful, long totalTimeMillis, String additionalInformation) {
        super.done(successful, totalTimeMillis, additionalInformation);
        this.progressTracker.endSubTask();
        this.progressTracker.logInfo(additionalInformation);
    }
}
