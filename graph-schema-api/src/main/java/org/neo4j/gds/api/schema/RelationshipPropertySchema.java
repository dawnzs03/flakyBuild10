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
package org.neo4j.gds.api.schema;

import org.immutables.value.Value;
import org.neo4j.gds.annotation.ValueClass;
import org.neo4j.gds.api.DefaultValue;
import org.neo4j.gds.api.PropertyState;
import org.neo4j.gds.api.nodeproperties.ValueType;
import org.neo4j.gds.core.Aggregation;

@ValueClass
@SuppressWarnings("immutables:subtype")
public interface RelationshipPropertySchema extends PropertySchema {

    Aggregation aggregation();

    static RelationshipPropertySchema of(String propertyKey, ValueType valueType) {
        return ImmutableRelationshipPropertySchema.of(
            propertyKey,
            valueType,
            valueType.fallbackValue(),
            PropertyState.PERSISTENT,
            Aggregation.DEFAULT
        );
    }

    static RelationshipPropertySchema of(String propertyKey, ValueType valueType, Aggregation aggregation) {
        return ImmutableRelationshipPropertySchema.of(
            propertyKey,
            valueType,
            valueType.fallbackValue(),
            PropertyState.PERSISTENT,
            aggregation
        );
    }

    static RelationshipPropertySchema of(
        String propertyKey,
        ValueType valueType,
        DefaultValue defaultValue,
        PropertyState propertyState,
        Aggregation aggregation
    ) {
        return ImmutableRelationshipPropertySchema.of(propertyKey, valueType, defaultValue, propertyState, aggregation);
    }

    @Value.Check
    default RelationshipPropertySchema normalize() {
        if (aggregation() == Aggregation.DEFAULT) {
            return ImmutableRelationshipPropertySchema
                .builder()
                .from(this)
                .aggregation(Aggregation.resolve(aggregation()))
                .build();
        }
        return this;
    }

}
