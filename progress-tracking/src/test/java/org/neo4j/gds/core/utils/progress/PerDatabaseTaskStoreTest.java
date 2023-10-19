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

import org.junit.jupiter.api.Test;
import org.neo4j.gds.core.utils.progress.tasks.Tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PerDatabaseTaskStoreTest {

    @Test
    void shouldBeIdempotentOnRemove() {
        var taskStore = new PerDatabaseTaskStore();
        var jobId = new JobId();
        taskStore.store("", jobId, Tasks.leaf("leaf"));
        taskStore.remove("", jobId);
        assertDoesNotThrow(() -> taskStore.remove("", jobId));
    }

    @Test
    void shouldReturnEmptyResultWhenStoreIsEmpty() {
        assertThat(new PerDatabaseTaskStore().query(""))
            .isNotNull()
            .isEmpty();
    }

    @Test
    void shouldCountAcrossUsers() {
        var taskStore = new PerDatabaseTaskStore();
        taskStore.store("a", new JobId(), Tasks.leaf("v"));

        assertThat(taskStore.taskCount()).isEqualTo(1);

        taskStore.store("b", new JobId(), Tasks.leaf("x"));

        assertThat(taskStore.taskCount()).isEqualTo(2);

        taskStore.store("b", new JobId(), Tasks.leaf("y"));

        assertThat(taskStore.taskCount()).isEqualTo(3);
    }

    @Test
    void shouldQueryByUser() {
        var taskStore = new PerDatabaseTaskStore();
        taskStore.store("alice", new JobId("42"), Tasks.leaf("leaf"));
        taskStore.store("alice", new JobId("666"), Tasks.leaf("leaf"));
        taskStore.store("bob", new JobId("1337"), Tasks.leaf("other"));

        assertThat(taskStore.query("alice")).hasSize(2)
            .allMatch(task -> task.username().equals("alice"));

        assertThat(taskStore.query("alice", new JobId("42"))).isPresent()
            .get()
            .matches(task -> task.jobId().asString().equals("42"))
            .matches(task -> task.username().equals("alice"));
    }

    @Test
    void shouldQueryMultipleUsers() {
        var taskStore = new PerDatabaseTaskStore();
        taskStore.store("alice", new JobId("42"), Tasks.leaf("leaf"));
        taskStore.store("bob", new JobId("1337"), Tasks.leaf("other"));

        assertThat(taskStore.query()).hasSize(2);
        assertThat(taskStore.query(new JobId("42"))).hasSize(1);
        assertThat(taskStore.query(new JobId(""))).hasSize(0);
    }
}
