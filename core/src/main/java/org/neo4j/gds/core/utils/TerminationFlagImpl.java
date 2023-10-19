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
package org.neo4j.gds.core.utils;

import org.neo4j.gds.api.TerminationMonitor;

public class TerminationFlagImpl implements TerminationFlag {

    private final TerminationMonitor terminationMonitor;

    private long interval = 10_000;

    private volatile long lastCheck = 0;
    private volatile boolean running = true;

    public TerminationFlagImpl(TerminationMonitor terminationMonitor) {
        this.terminationMonitor = terminationMonitor;
    }

    TerminationFlagImpl withCheckInterval(long interval) {
        this.interval = interval;
        return this;
    }

    @Override
    public boolean running() {
        final long currentTime = System.currentTimeMillis();
        if (currentTime > lastCheck + interval) {
            if (terminationMonitor.isTerminated()) {
                running = false;
            }
            lastCheck = currentTime;
        }
        return running;
    }
}
