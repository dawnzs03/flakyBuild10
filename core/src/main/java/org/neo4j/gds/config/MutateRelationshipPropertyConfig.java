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

import org.jetbrains.annotations.Nullable;
import org.neo4j.gds.annotation.Configuration;

import static org.neo4j.gds.core.StringIdentifierValidations.emptyToNull;
import static org.neo4j.gds.core.StringIdentifierValidations.validateNoWhiteCharacter;

public interface MutateRelationshipPropertyConfig extends MutateConfig {

    String MUTATE_PROPERTY_KEY = "mutateProperty";

    @Configuration.ConvertWith(method = "validateProperty")
    @Configuration.Key(MUTATE_PROPERTY_KEY)
    String mutateProperty();

    static @Nullable String validateProperty(String input) {
        return validateNoWhiteCharacter(emptyToNull(input), "mutateProperty");
    }

}
