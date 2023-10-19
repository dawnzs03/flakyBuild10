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
package org.neo4j.gds.beta.pregel;

import org.immutables.value.Value;
import org.neo4j.gds.NodeLabel;
import org.neo4j.gds.RelationshipType;
import org.neo4j.gds.annotation.Configuration;
import org.neo4j.gds.annotation.ValueClass;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.config.MutateNodePropertyConfig;
import org.neo4j.gds.config.WritePropertyConfig;
import org.neo4j.gds.core.CypherMapWrapper;

import java.util.Collection;

@ValueClass
@Configuration
@SuppressWarnings("immutables:subtype")
public interface PregelProcedureConfig extends
    PregelConfig,
    WritePropertyConfig,
    MutateNodePropertyConfig {

    @Value.Default
    default String writeProperty() {
        return "";
    }

    @Value.Default
    default String mutateProperty() {
        return "";
    }

    @Override
    @Configuration.GraphStoreValidationCheck
    @Value.Default
    default void validateGraphIsSuitableForWrite(
        GraphStore graphStore,
        @SuppressWarnings("unused") Collection<NodeLabel> selectedLabels,
        @SuppressWarnings("unused") Collection<RelationshipType> selectedRelationshipTypes
    ) {
        // VN/IP: HACK!
        // Since we are using the same configuration for all the modes (`stream`, `write` and `mutate`) we check if the
        // graph is writable in all modes.
        // We only want to raise the error if the user actually doing writes,
        // which we assume is when there is a `writeProperty` present
        if (writeProperty().isBlank()) {
            return;
        }

        if (!graphStore.capabilities().canWriteToDatabase() && !graphStore.capabilities().canWriteToRemoteDatabase()) {
            throw new IllegalArgumentException("The provided graph does not support `write` execution mode.");
        }
    }


    static PregelProcedureConfig of(CypherMapWrapper userInput) {
        return new PregelProcedureConfigImpl(userInput);
    }
}
