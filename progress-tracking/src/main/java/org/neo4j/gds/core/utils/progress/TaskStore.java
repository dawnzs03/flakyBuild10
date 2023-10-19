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
package org.neo4j.gds.core.utils.progress;

import org.neo4j.gds.annotation.ValueClass;
import org.neo4j.gds.core.utils.progress.tasks.Task;

import java.util.Optional;
import java.util.stream.Stream;

public interface TaskStore {

    void store(String username, JobId jobId, Task task);

    void remove(String username, JobId jobId);

    Stream<UserTask> query();

    Stream<UserTask> query(JobId jobId);

    Stream<UserTask> query(String username);

    Optional<UserTask> query(String username, JobId jobId);

    boolean isEmpty();

    long taskCount();

    @ValueClass
    interface UserTask {
        String username();

        JobId jobId();

        Task task();
    }
}
