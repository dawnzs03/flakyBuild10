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
package org.neo4j.gds.core.huge;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.gds.NodeLabel;
import org.neo4j.gds.api.DefaultValue;
import org.neo4j.gds.api.FilteredIdMap;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.api.properties.nodes.LongNodePropertyValues;
import org.neo4j.gds.core.huge.FilteredNodePropertyValues.OriginalToFilteredNodePropertyValues;
import org.neo4j.gds.core.utils.paged.HugeLongArray;
import org.neo4j.gds.extension.GdlExtension;
import org.neo4j.gds.extension.GdlGraph;
import org.neo4j.gds.extension.Inject;
import org.neo4j.gds.extension.TestGraph;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@GdlExtension
class OriginalToFilteredNodePropertiesTest {

    @GdlGraph
    private static final String GDL =
        " (a:A { doubleArray: [1.0], longArray: [1L], floatArray: [1.0F] })" +
        " (b:B { doubleArray: [1.0, 2.22, 1.337], longArray: [1L, 222L, 1337L], floatArray: [1.0F, 2.22F, 1.337F] })" +
        " (c:Ignore)";

    @Inject
    private TestGraph graph;

    @Inject
    private GraphStore graphStore;

    @Test
    void testDoubleArray() {
        var filteredNodeProperties = new FilteredNodePropertyValues.OriginalToFilteredNodePropertyValues(
            graph.nodeProperties("doubleArray"),
            new NodeFilteredGraph(
                graph,
                (FilteredIdMap) graphStore.getGraph(Set.of(NodeLabel.of("A"), NodeLabel.of("B")))
            )
        );

        assertThat(filteredNodeProperties.doubleArrayValue(graph.toMappedNodeId("a"))).containsExactly(1D);
        assertThat(filteredNodeProperties.doubleArrayValue(graph.toMappedNodeId("b"))).containsExactly(
            new double[]{1D, 2.22D, 1.337D},
            Offset.offset(1e-5)
        );
    }

    @Test
    void testLongArray() {
        var filteredNodeProperties = new OriginalToFilteredNodePropertyValues(
            graph.nodeProperties("longArray"),
            new NodeFilteredGraph(
                graph,
                (FilteredIdMap) graphStore.getGraph(Set.of(NodeLabel.of("A"), NodeLabel.of("B")))
            )
        );

        assertThat(filteredNodeProperties.longArrayValue(graph.toMappedNodeId("a"))).containsExactly(1L);
        assertThat(filteredNodeProperties.longArrayValue(graph.toMappedNodeId("b"))).containsExactly(1L, 222L, 1337L);
    }

    @Test
    void testFloatArray() {
        var filteredNodeProperties = new FilteredNodePropertyValues.OriginalToFilteredNodePropertyValues(
            graph.nodeProperties("floatArray"),
            new NodeFilteredGraph(
                graph,
                (FilteredIdMap) graphStore.getGraph(Set.of(NodeLabel.of("A"), NodeLabel.of("B")))
            )
        );

        assertThat(filteredNodeProperties.floatArrayValue(graph.toMappedNodeId("a"))).containsExactly(1.0F);
        assertThat(filteredNodeProperties.floatArrayValue(graph.toMappedNodeId("b"))).containsExactly(1.0F, 2.22F, 1.337F);
    }

    @ParameterizedTest
    @MethodSource("org.neo4j.gds.core.huge.OriginalToFilteredNodePropertiesTest#expectedFilteredValue")
    void testReturnTheDefaultValueForNodesOutOfRange(String label, long expectedAValue, long expectedBValue) {
        NodeFilteredGraph nodeFilteredGraph = (NodeFilteredGraph) graphStore.getGraph(
            List.of(NodeLabel.of(label)),
            graphStore.relationshipTypes(),
            Optional.empty()
        );

        var propertyArray = HugeLongArray.newArray(1);
        propertyArray.setAll((i) -> 42L);

        var filteredNodeProperties = new OriginalToFilteredNodePropertyValues(
            LongNodePropertyValuesAdapter.create(propertyArray),
            nodeFilteredGraph
        );

        assertThat(filteredNodeProperties.longValue(graph.toMappedNodeId("a"))).isEqualTo(expectedAValue);
        assertThat(filteredNodeProperties.longValue(graph.toMappedNodeId("b"))).isEqualTo(expectedBValue);
    }

    static Stream<Arguments> expectedFilteredValue() {
        return Stream.of(
            Arguments.arguments("A", 42L, DefaultValue.LONG_DEFAULT_FALLBACK),
            Arguments.arguments("B", DefaultValue.LONG_DEFAULT_FALLBACK, 42L)
        );
    }

    /**
     * NodePropertyValues backed by HugeLongArray
     */
    // This is a test-only class with a single usage, it is okay to have it here instead of creating module dependencies
    private static final class LongNodePropertyValuesAdapter implements LongNodePropertyValues {

        private final HugeLongArray delegate;

        private LongNodePropertyValuesAdapter(HugeLongArray delegate) {
            this.delegate = delegate;
        }

        public static LongNodePropertyValues create(HugeLongArray delegate) {
            return new LongNodePropertyValuesAdapter(delegate);
        }

        @Override
        public long longValue(long nodeId) {
            return delegate.get(nodeId);
        }

        @Override
        public long nodeCount() {
            return delegate.size();
        }
    }

}
