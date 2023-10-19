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
package org.neo4j.gds;

import org.junit.jupiter.api.Test;
import org.neo4j.gds.core.Aggregation;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PropertyMappingsTest {

    @Test
    void testFromObjectWithShorthandPropertyMapping() {
        PropertyMappings mappings = PropertyMappings.fromObject(Collections.singletonMap("foo", (Object) "bar"));
        assertEquals(mappings.numberOfMappings(), 1);

        PropertyMapping propertyMapping = mappings.mappings().get(0);
        assertEquals(propertyMapping.propertyKey(), "foo");
        assertEquals(propertyMapping.neoPropertyKey(), "bar");
    }

    @Test
    void testFromObjectWithMultiplePropertyMappings() {
        var propertyProjections = new LinkedHashMap<>();
        propertyProjections.put(
            "total_usd", Map.of(
                "property", "usd",
                "aggregation", "MIN",
                "defaultValue", 42.0
            )
        );
        propertyProjections.put("transaction_count", Map.of(
                "property", "usd",
                "aggregation", "SUM"
            )
        );

        var mappings = PropertyMappings.fromObject(propertyProjections);
        assertEquals(mappings.numberOfMappings(), 2);

        Iterator<PropertyMapping> mappingIterator = mappings.iterator();
        PropertyMapping totalUsdMapping = mappingIterator.next();
        assertEquals(totalUsdMapping.propertyKey(), "total_usd");
        assertEquals(totalUsdMapping.neoPropertyKey(), "usd");
        assertEquals(totalUsdMapping.aggregation(), Aggregation.MIN);
        assertEquals(totalUsdMapping.defaultValue().doubleValue(), 42.0);

        PropertyMapping transactionCountMapping = mappingIterator.next();
        assertEquals(transactionCountMapping.propertyKey(), "transaction_count");
        assertEquals(transactionCountMapping.neoPropertyKey(), "usd");
        assertEquals(transactionCountMapping.aggregation(), Aggregation.SUM);
        assertEquals(transactionCountMapping.defaultValue().doubleValue(), Double.NaN);
    }

    @Test
    void failsOnNonStringOrMapInput() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> PropertyMappings.fromObject(5));

        assertThat(ex.getMessage(), containsString("Expected String or Map for property mappings. Got Integer"));
    }

}
