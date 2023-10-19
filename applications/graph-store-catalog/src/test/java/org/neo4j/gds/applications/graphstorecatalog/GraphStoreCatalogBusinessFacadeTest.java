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
package org.neo4j.gds.applications.graphstorecatalog;

import org.junit.jupiter.api.Test;
import org.neo4j.gds.api.DatabaseId;
import org.neo4j.gds.api.GraphName;
import org.neo4j.gds.api.User;
import org.neo4j.gds.core.loading.GraphStoreCatalogService;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GraphStoreCatalogBusinessFacadeTest {
    @Test
    void shouldDetermineGraphExists() {
        var service = mock(GraphStoreCatalogService.class);
        var facade = new DefaultGraphStoreCatalogBusinessFacade(
            null,
            new GraphNameValidationService(),
            service,
            null,
            null,
            null,
            null,
            null
        );

        when(service.graphExists(
            new User("someUser", false),
            DatabaseId.from("someDatabase"),
            GraphName.parse("someGraph")
        )).thenReturn(true);
        var graphExists = facade.graphExists(new User("someUser", false), DatabaseId.from("someDatabase"), "someGraph");

        assertTrue(graphExists);
    }

    @Test
    void shouldDetermineGraphDoesNotExist() {
        var service = mock(GraphStoreCatalogService.class);
        var facade = new DefaultGraphStoreCatalogBusinessFacade(
            null,
            mock(GraphNameValidationService.class),
            service,
            null,
            null,
            null,
            null,
            null
        );

        when(service.graphExists(
            new User("someUser", false),
            DatabaseId.from("someDatabase"),
            GraphName.parse("someGraph")
        )).thenReturn(false);
        boolean graphExists = facade.graphExists(
            new User("someUser", false),
            DatabaseId.from("someDatabase"),
            "someGraph"
        );

        assertFalse(graphExists);
    }

    @Test
    void shouldValidateInputGraphName() {
        var service = mock(GraphStoreCatalogService.class);
        var facade = new DefaultGraphStoreCatalogBusinessFacade(
            null,
            new GraphNameValidationService(),
            service,
            null,
            null,
            null,
            null,
            null
        );

        assertThatThrownBy(
            () -> facade.graphExists(new User("someUser", false), DatabaseId.from("someDatabase"), "   ")
        ).hasMessage("`graphName` can not be null or blank, but it was `   `");
    }

    @Test
    void shouldUseStrictValidationForNativeProject() {
        var validationService = mock(GraphNameValidationService.class);
        var facade = new DefaultGraphStoreCatalogBusinessFacade(
            null,
            validationService,
            null,
            null,
            null,
            null,
            null,
            null
        );

        when(validationService.validateStrictly("   somegraph   ")).thenThrow(new IllegalArgumentException("whitespace!"));
        assertThatIllegalArgumentException().isThrownBy(
            () -> facade.nativeProject(
                null,
                null,
                null,
                null,
                null,
                null,
                "   somegraph   ",
                null,
                null,
                null
            )
        ).withMessage("whitespace!");
    }
}
