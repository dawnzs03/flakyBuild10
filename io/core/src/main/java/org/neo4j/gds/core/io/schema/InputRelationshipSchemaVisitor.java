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
package org.neo4j.gds.core.io.schema;

import org.neo4j.gds.RelationshipType;
import org.neo4j.gds.api.schema.Direction;
import org.neo4j.gds.api.schema.RelationshipPropertySchema;
import org.neo4j.gds.core.Aggregation;

public interface InputRelationshipSchemaVisitor extends InputSchemaVisitor, RelationshipPropertySchema {

    boolean relationshipType(RelationshipType relationshipType);

    boolean aggregation(Aggregation aggregation);

    boolean direction(Direction direction);

    abstract class Adapter extends ElementSchemaVisitor implements InputRelationshipSchemaVisitor {
        @Override
        public boolean relationshipType(RelationshipType relationshipType) {
            return true;
        }

        @Override
        public boolean aggregation(Aggregation aggregation) {
            return true;
        }

        @Override
        public boolean direction(Direction direction) { return true; }
    }
}
