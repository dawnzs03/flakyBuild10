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
package org.neo4j.gds.config;

import org.immutables.value.Value;
import org.neo4j.gds.annotation.Configuration;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("immutables:subtype")
public interface BaseConfig extends ToMapConvertible {

    String SUDO_KEY = "sudo";
    String LOG_PROGRESS_KEY = "logProgress";

    @Value.Parameter(false)
    @Configuration.Key("username")
    @Configuration.ConvertWith(method = "trim")
    Optional<String> usernameOverride();

    @Value.Default
    @Value.Parameter(false)
    @Configuration.Key(SUDO_KEY)
    default boolean sudo() {
        return false;
    }

    @Value.Default
    @Value.Parameter(false)
    @Configuration.Key(LOG_PROGRESS_KEY)
    default boolean logProgress() {
        return true;
    }

    @Configuration.CollectKeys
    @Value.Auxiliary
    @Value.Default
    @Value.Parameter(false)
    default Collection<String> configKeys() {
        return Collections.emptyList();
    }

    @Configuration.ToMap
    @Value.Auxiliary
    @Value.Derived
    default Map<String, Object> toMap() {
        return new HashMap<>();
    }

    static String trim(String input) {
        return input.trim();
    }
}
