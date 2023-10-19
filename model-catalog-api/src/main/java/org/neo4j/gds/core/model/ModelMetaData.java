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
package org.neo4j.gds.core.model;

import org.immutables.value.Value;
import org.neo4j.gds.annotation.ValueClass;
import org.neo4j.gds.api.schema.GraphSchema;
import org.neo4j.gds.core.model.Model.CustomInfo;
import org.neo4j.gds.model.ModelConfig;

import java.time.ZonedDateTime;
import java.util.List;

import static org.neo4j.gds.core.model.Model.ALL_USERS;

@ValueClass
public interface ModelMetaData<CONFIG extends ModelConfig, INFO extends CustomInfo> {

    String creator();

    List<String> sharedWith();

    String name();

    String algoType();

    GraphSchema graphSchema();

    CONFIG trainConfig();

    ZonedDateTime creationTime();

    INFO customInfo();

    String gdsVersion();

    @Value.Derived
    default boolean isPublished() {
        return sharedWith().contains(ALL_USERS);
    }
}
