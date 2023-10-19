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
package org.neo4j.gds.core.utils.warnings;

import org.neo4j.gds.core.utils.progress.tasks.Task;
import org.neo4j.values.storable.LocalTimeValue;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

public class UserLogEntry {
    public String taskName;
    public String message;
    public LocalTimeValue timeStarted;

    public UserLogEntry(Task task, String message) {
        this.taskName = task.description();
        this.message = message;
        this.timeStarted = LocalTimeValue.localTime(LocalTime.ofInstant(
            Instant.ofEpochMilli(task.startTime()),
            ZoneId.systemDefault()
        ));
    }

    public String getTaskName() {
        return taskName;
    }

    public String getMessage() {
        return message;
    }

    public LocalTimeValue getTimeStarted() {return timeStarted;}
}
