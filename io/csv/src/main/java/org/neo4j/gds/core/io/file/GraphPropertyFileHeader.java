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
package org.neo4j.gds.core.io.file;

import org.immutables.value.Value;
import org.neo4j.gds.annotation.ValueClass;
import org.neo4j.gds.api.schema.PropertySchema;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

@ValueClass
public interface GraphPropertyFileHeader extends FileHeader<Map<String, PropertySchema>, PropertySchema> {

    HeaderProperty propertyMapping();

    @Value.Derived
    @Override
    default Set<HeaderProperty> propertyMappings() {
        return Set.of(propertyMapping());
    }

    @Override
    default Map<String, PropertySchema> schemaForIdentifier(Map<String, PropertySchema> propertySchema) {
        var graphPropertySchema = propertySchema.get(propertyMapping().propertyKey());
        return Map.of(graphPropertySchema.key(), graphPropertySchema);
    }

    static GraphPropertyFileHeader of(String[] headerLine) {
        if (headerLine.length != 1) {
            throw new IllegalArgumentException(
                "Graph property headers should contain exactly one property column, but got: "
                + Arrays.toString(headerLine)
            );
        }
        return ImmutableGraphPropertyFileHeader.of(HeaderProperty.parse(0, headerLine[0]));
    }
}
