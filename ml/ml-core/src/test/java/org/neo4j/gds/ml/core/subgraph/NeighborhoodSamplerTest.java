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
package org.neo4j.gds.ml.core.subgraph;

import com.carrotsearch.hppc.LongLongHashMap;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.gds.extension.GdlExtension;
import org.neo4j.gds.extension.GdlGraph;
import org.neo4j.gds.extension.Inject;
import org.neo4j.gds.extension.TestGraph;
import org.neo4j.gds.gdl.GdlFactory;

import java.util.Random;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@GdlExtension
class NeighborhoodSamplerTest {

    @Nested
    class Unweighted {
        @GdlGraph
        private static final String GRAPH =
            "(x)-->(y)-->(z), " +
            "(a)-->(b)-->(c), " +
            "(x)-->(d)-->(e), " +
            "(a)-->(f) , " +
            "(a)-->(g), " +
            "(a)-->(h)";

        @Inject
        private TestGraph graph;

        @Test
        void shouldSampleSubsetOfNeighbors() {

            NeighborhoodSampler sampler = new NeighborhoodSampler(0L);
            int numberOfSamples = 3;
            var sample = sampler.sample(graph, graph.toMappedNodeId("a"), numberOfSamples);

            assertThat(sample)
                .isNotNull()
                .hasSize(numberOfSamples)
                .containsAnyOf(
                    graph.toMappedNodeId("b"),
                    graph.toMappedNodeId("f"),
                    graph.toMappedNodeId("g"),
                    graph.toMappedNodeId("h")
                )
                .doesNotContain( // does not contain negative neighbors
                    graph.toMappedNodeId("x"),
                    graph.toMappedNodeId("y"),
                    graph.toMappedNodeId("z"),
                    graph.toMappedNodeId("a"),
                    graph.toMappedNodeId("c"),
                    graph.toMappedNodeId("d"),
                    graph.toMappedNodeId("e")
                );
    }

        @Test
        void shouldSampleAllNeighborsWhenNumberOfSamplesAreGreater() {
            NeighborhoodSampler sampler = new NeighborhoodSampler(0L);
            int numberOfSamples = 19;
            var sample = sampler.sample(graph, graph.toMappedNodeId("a"), numberOfSamples);

            assertThat(sample)
                .isNotNull()
                .hasSize(4);
        }

        @Test
        void multiGraph() {
            var multiGraph = GdlFactory.of("(a)-->(b), (a)-->(b), (a)-->(b)").build().getUnion();

            NeighborhoodSampler sampler = new NeighborhoodSampler(42);
            var sample = sampler.sample(multiGraph, 0, 2).toArray();

            assertThat(sample).containsExactly(1, 1);
        }
    }

    @Nested
    class UniformSample {
        @GdlGraph
        private static final String GRAPH =
            "(a)-[:R]->(b), " +
            "(a)-[:R]->(c), " +
            "(a)-[:R]->(d), " +
            "(a)-[:R]->(e), " +
            "(a)-[:R]->(f), " +
            "(a)-[:R]->(g), " +
            "(a)-[:R]->(h), " +
            "(a)-[:R]->(i), " +
            "(a)-[:R]->(j), " +
            "(a)-[:R]->(k), " +
            "(a)-[:R]->(l), " +
            "(a)-[:R]->(m), " +
            "(a)-[:R]->(n), " +
            "(a)-[:R]->(o), " +
            "(a)-[:R]->(p), " +
            "(a)-[:R]->(q), " +
            "(a)-[:R]->(r), " +
            "(a)-[:R]->(s)";

        @Inject
        TestGraph graph;


        @Test
        void shouldSampleSubsetOfNeighbors() {
            var random = new Random(42);
            int numberOfSamples = 2;

            var sampledNodes = new LongLongHashMap();

            for (int i = 0; i < 20; i++) {
                NeighborhoodSampler sampler = new NeighborhoodSampler(random.nextLong());
                sampler.sample(graph, graph.toMappedNodeId("a"), numberOfSamples)
                    .forEach(nodeId -> sampledNodes.addTo(nodeId, 1));
            }
            
            assertThat(sampledNodes)
                .allSatisfy(entry -> assertThat(entry.value)
                    .withFailMessage("Sampled node with id %d %d times", entry.key, entry.value)
                    .isLessThan(10));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 4, 17, 99})
        void shouldSampleTheCorrectNumber(int numberOfSamples) {
            var random = new Random(42);
            var startNode = graph.toMappedNodeId("a");

            NeighborhoodSampler sampler = new NeighborhoodSampler(random.nextLong());
            var sample = sampler.sample(graph, startNode, numberOfSamples);

            var expectedSize = Math.min(graph.degree(startNode), numberOfSamples);

            assertThat(sample)
                .hasSize(expectedSize)
                .doesNotHaveDuplicates();
        }
    }

    @Nested
    class WeightedCase {

        @GdlGraph
        private static final String GRAPH =
            "(x)-[:R { weight: 8.0 }]->(y)-[:R { weight: 4.6 }]->(z), " +
            "(a)-[:R { weight: 1.0 }]->(b)-[:R { weight: 4.6 }]->(c), " +
            "(x)-[:R { weight: 3.0 }]->(d)-[:R { weight: 4.6 }]->(e), " +
            "(a)-[:R { weight: 99.0 }]->(f), " +
            "(a)-[:R { weight: 99.0 }]->(g), " +
            "(a)-[:R { weight: 1.0 }]->(g), " +
            "(a)-[:R { weight: 1.0 }]->(g), " +
            "(a)-[:R { weight: 1.0 }]->(b), " +
            "(a)-[:R { weight: 99.0 }]->(h)";

        @Inject
        TestGraph graph;

        @Test
        void shouldSampleSubsetOfNeighbors() {

            NeighborhoodSampler sampler = new NeighborhoodSampler(0L);
            int numberOfSamples = 3;
            var sample = sampler.sample(graph, graph.toMappedNodeId("a"), numberOfSamples);

            assertThat(sample)
                .isNotNull()
                .hasSize(numberOfSamples)
                .containsAnyOf(
                    graph.toMappedNodeId("b"),
                    graph.toMappedNodeId("f"),
                    graph.toMappedNodeId("g"),
                    graph.toMappedNodeId("h")
                )
                .doesNotContain( // does not contain negative neighbors
                    graph.toMappedNodeId("x"),
                    graph.toMappedNodeId("y"),
                    graph.toMappedNodeId("z"),
                    graph.toMappedNodeId("a"),
                    graph.toMappedNodeId("c"),
                    graph.toMappedNodeId("d"),
                    graph.toMappedNodeId("e")
                );
        }

        @Test
        void shouldUniformSamplefHighWeightNeighbors() {
            var random = new Random(42);
            int numberOfSamples = 2;

            var sampledNodes = new LongLongHashMap();

            var sampleTries = 1000;
            for (int i = 0; i < sampleTries; i++) {
                NeighborhoodSampler sampler = new NeighborhoodSampler(random.nextLong());
                sampler.sample(graph, graph.toMappedNodeId("a"), numberOfSamples)
                    .forEach(nodeId -> sampledNodes.addTo(nodeId, 1));
            }

            // f, g and h have an equal weight so they should be sampled equally
            // we expect b to be sampled nearly never
            // to avoid flaky tests we only test the extremes here
            var highWeightNeighbors = Stream.of("f", "g","h");

            highWeightNeighbors.forEach(name -> {
                var sampleAmount = sampledNodes.get(graph.toMappedNodeId(name));
                assertThat(sampleAmount)
                    .withFailMessage("Node %s considered to extreme: %d", name, sampleAmount)
                    // 1000x 2 samples -> 2000 samples / 3 (#high node-ids)
                    .isCloseTo(666, Offset.offset(40L)).isNotEqualTo(sampleTries);
            });
        }
    }
}
