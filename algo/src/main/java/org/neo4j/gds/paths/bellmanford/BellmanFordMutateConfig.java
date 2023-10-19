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
package org.neo4j.gds.paths.bellmanford;

import org.immutables.value.Value;
import org.neo4j.gds.annotation.Configuration;
import org.neo4j.gds.config.MutateRelationshipConfig;
import org.neo4j.gds.core.CypherMapWrapper;

@Configuration
public interface BellmanFordMutateConfig extends BellmanFordBaseConfig, MutateRelationshipConfig {

    default boolean mutateNegativeCycles() {
        return false;
    }

    @Override
    @Value.Derived
    @Configuration.Ignore
    default boolean trackNegativeCycles() {
        return mutateNegativeCycles();
    }


    static BellmanFordMutateConfig of(CypherMapWrapper userInput) {
        return new BellmanFordMutateConfigImpl(userInput);
    }

}
