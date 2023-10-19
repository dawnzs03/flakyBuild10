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
package negative;

import org.neo4j.gds.annotation.Configuration;

import java.util.Collection;
import java.util.List;

@Configuration("InvalidGSValidationConfig")
public interface InvalidGSValidation {

    @Configuration.GraphStoreValidationCheck
    default void notThreeParams(
        List<String> graphStore,
        Collection<String> selectedLabels
    ) {
        assert true;
    }

    @Configuration.GraphStoreValidationCheck
    void notDefault(
        List<String> graphStore,
        Collection<String> selectedLabels,
        Collection<String> selectedRelationshipTypes
    );

    @Configuration.GraphStoreValidationCheck
    default int notVoid(
        List<String> graphStore,
        Collection<String> selectedLabels,
        Collection<String> selectedRelationshipTypes
    ) {
        return 1;
    }

    @Configuration.GraphStoreValidation
    default void graphStoreValidation(
        List<String> graphStore,
        Collection<String> selectedLabels,
        Collection<String> selectedRelationshipTypes
    ) {}
}
