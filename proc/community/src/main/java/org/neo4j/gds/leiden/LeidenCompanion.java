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
package org.neo4j.gds.leiden;

import org.neo4j.gds.CommunityProcCompanion;
import org.neo4j.gds.api.properties.nodes.EmptyLongArrayNodePropertyValues;
import org.neo4j.gds.api.properties.nodes.EmptyLongNodePropertyValues;
import org.neo4j.gds.api.properties.nodes.NodePropertyValues;
import org.neo4j.gds.executor.ComputationResult;
import org.neo4j.gds.nodeproperties.LongNodePropertyValuesAdapter;

final class LeidenCompanion {
    private LeidenCompanion() {}

    static <CONFIG extends LeidenBaseConfig> NodePropertyValues leidenNodeProperties(

        ComputationResult<Leiden, LeidenResult, CONFIG> computationResult,
        String resultProperty

    ) {
        if (computationResult.result().isEmpty()) {
            return EmptyLongArrayNodePropertyValues.INSTANCE;
        }

        var config = computationResult.config();
        var leidenResult = computationResult.result().get();


        if (config.includeIntermediateCommunities()) {
            return new IntermediateCommunityNodeProperties(
                computationResult.graph().nodeCount(),
                leidenResult.communities().size(),
                leidenResult::getIntermediateCommunities
            );
        } else {
            return getCommunities(computationResult, resultProperty);
        }
    }

    private static <CONFIG extends LeidenBaseConfig> NodePropertyValues getCommunities(
        ComputationResult<Leiden, LeidenResult, CONFIG> computationResult,
        String resultProperty
    ) {
        var config = computationResult.config();

        return CommunityProcCompanion.nodeProperties(
            config,
            resultProperty,
            computationResult.result()
                .map(LeidenResult::dendrogramManager)
                .map(LeidenDendrogramManager::getCurrent)
                .map(LongNodePropertyValuesAdapter::create)
                .orElse(EmptyLongNodePropertyValues.INSTANCE),
            () -> computationResult.graphStore().nodeProperty(config.seedProperty())
        );
    }
}
