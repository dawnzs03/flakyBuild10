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
package org.neo4j.gds.conductance;

import org.immutables.value.Value;
import org.jetbrains.annotations.Nullable;
import org.neo4j.gds.NodeLabel;
import org.neo4j.gds.RelationshipType;
import org.neo4j.gds.annotation.Configuration;
import org.neo4j.gds.annotation.ValueClass;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.config.AlgoBaseConfig;
import org.neo4j.gds.config.RelationshipWeightConfig;
import org.neo4j.gds.utils.StringJoining;

import java.util.Collection;

import static org.neo4j.gds.core.StringIdentifierValidations.emptyToNull;
import static org.neo4j.gds.core.StringIdentifierValidations.validateNoWhiteCharacter;
import static org.neo4j.gds.utils.StringFormatting.formatWithLocale;

@ValueClass
@Configuration
@SuppressWarnings("immutables:subtype")
public interface ConductanceConfig extends AlgoBaseConfig, RelationshipWeightConfig {

    @Configuration.ConvertWith(method = "validatePropertyName")
    String communityProperty();

    static @Nullable String validatePropertyName(String input) {
        return validateNoWhiteCharacter(emptyToNull(input), "communityProperty");
    }

    @Configuration.GraphStoreValidationCheck
    @Value.Default
    default void communityPropertyValidation(
        GraphStore graphStore,
        Collection<NodeLabel> selectedLabels,
        Collection<RelationshipType> selectedRelationshipTypes
    ) {
        if (selectedLabels
            .stream()
            .anyMatch(label -> graphStore.nodePropertyKeys(label).contains(communityProperty()))) {
            return;
        }

        throw new IllegalArgumentException(formatWithLocale(
            "communityProperty `%s` is not present for any requested node labels. Requested labels: %s. Labels with `%1$s` present: %s",
            communityProperty(),
            StringJoining.join(selectedLabels.stream().map(NodeLabel::name)),
            StringJoining.join(graphStore
                .nodeLabels()
                .stream()
                .filter(label -> graphStore.nodePropertyKeys(label).contains(communityProperty()))
                .map(NodeLabel::name))
        ));
    }
}
