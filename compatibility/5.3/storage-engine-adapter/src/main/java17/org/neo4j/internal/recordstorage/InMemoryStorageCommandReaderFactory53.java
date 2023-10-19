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
package org.neo4j.internal.recordstorage;

import org.neo4j.kernel.KernelVersion;
import org.neo4j.storageengine.api.CommandReader;
import org.neo4j.storageengine.api.CommandReaderFactory;

public class InMemoryStorageCommandReaderFactory53 implements CommandReaderFactory {

    public static final CommandReaderFactory INSTANCE = new InMemoryStorageCommandReaderFactory53();

    @Override
    public CommandReader get(KernelVersion kernelVersion) {
        switch (kernelVersion) {
            case V4_2:
                return LogCommandSerializationV4_2.INSTANCE;
            case V4_3_D4:
                return LogCommandSerializationV4_3_D3.INSTANCE;
            case V5_0:
                return LogCommandSerializationV5_0.INSTANCE;
            default:
                throw new IllegalArgumentException("Unsupported kernel version " + kernelVersion);
        }
    }
}
