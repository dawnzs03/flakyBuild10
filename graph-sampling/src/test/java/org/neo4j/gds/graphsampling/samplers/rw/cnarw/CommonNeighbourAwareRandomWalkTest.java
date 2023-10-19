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
package org.neo4j.gds.graphsampling.samplers.rw.cnarw;

import org.assertj.core.data.Offset;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;
import org.neo4j.gds.NodeLabel;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.core.GraphDimensions;
import org.neo4j.gds.core.utils.mem.MemoryRange;
import org.neo4j.gds.core.utils.paged.HugeAtomicBitSet;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.extension.GdlExtension;
import org.neo4j.gds.extension.GdlGraph;
import org.neo4j.gds.extension.IdFunction;
import org.neo4j.gds.extension.Inject;
import org.neo4j.gds.extension.TestGraph;
import org.neo4j.gds.graphsampling.config.CommonNeighbourAwareRandomWalkConfig;
import org.neo4j.gds.graphsampling.config.CommonNeighbourAwareRandomWalkConfigImpl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.neo4j.gds.Orientation.NATURAL;
import static org.neo4j.gds.TestSupport.assertMemoryEstimation;
import static org.neo4j.gds.TestSupport.assertMemoryRange;
import static org.neo4j.gds.TestSupport.assertMemoryRangeIsClose;

@GdlExtension
class CommonNeighbourAwareRandomWalkTest {

    @GdlGraph(idOffset = 42)
    private static final String DB_CYPHER =
        "CREATE" +
        "  (x:Z {prop: 42})" +
        ", (x1:Z {prop: 43})" +
        ", (x2:Z {prop: 44})" +
        ", (x3:Z {prop: 45})" +
        ", (a:N {prop: 46})" +
        ", (b:N {prop: 47})" +
        ", (c:N {prop: 48, attr: 48})" +
        ", (d:N {prop: 49, attr: 48})" +
        ", (e:M {prop: 50, attr: 48})" +
        ", (f:M {prop: 51, attr: 48})" +
        ", (g:M {prop: 52})" +
        ", (h:M {prop: 53})" +
        ", (i:X {prop: 54})" +
        ", (j:M {prop: 55})" +
        ", (x)-[:R1 {distance: 0.0} ]->(x1)" +
        ", (x)-[:R1 {distance: 2.0} ]->(x2)" +
        ", (x)-[:R1 {distance: 200.0} ]->(x3)" +
        ", (e)-[:R1 {distance: 1.0} ]->(d)" +
        ", (i)-[:R1 {distance: 1.0} ]->(g)" +
        ", (a)-[:R1 {cost: 10.0, distance: 5.8}]->(b)" +
        ", (a)-[:R1 {cost: 10.0, distance: 4.8}]->(c)" +
        ", (c)-[:R1 {cost: 10.0, distance: 5.8}]->(d)" +
        ", (d)-[:R1 {cost:  4.2, distance: 2.6}]->(e)" +
        ", (e)-[:R1 {cost: 10.0, distance: 5.8}]->(f)" +
        ", (f)-[:R1 {cost: 10.0, distance: 9.9}]->(g)" +
        ", (h)-[:R2 {cost: 10.0, distance: 5.8}]->(i)";

    @Inject
    private GraphStore graphStore;

    @Inject
    private IdFunction idFunction;

    @GdlGraph(graphNamePrefix = "lollipop", idOffset = 42)
    private static final String lollipopGDL =
        "CREATE" +
        "  (xy:Z {prop: 42})" +
        ", (x1:Z {prop: 43})" +
        ", (x2:Z {prop: 44})" +
        ", (x3:Z {prop: 45})" +
        ", (x4:Z {prop: 46})" +
        ", (x5:Z {prop: 47})" +
        ", (y1:Z {prop: 48})" +
        ", (y2:Z {prop: 49})" +
        ",                    (xy)-[:REL]->(x1), (xy)-[:REL]->(x2), (xy)-[:REL]->(x3), (xy)-[:REL]->(x4), (xy)-[:REL]->(x5)" +
        ", (x1)-[:REL]->(xy),                    (x1)-[:REL]->(x2), (x1)-[:REL]->(x3), (x1)-[:REL]->(x4), (x1)-[:REL]->(x5)" +
        ", (x2)-[:REL]->(xy), (x2)-[:REL]->(x1),                    (x2)-[:REL]->(x3), (x2)-[:REL]->(x4), (x2)-[:REL]->(x5)" +
        ", (x3)-[:REL]->(xy), (x3)-[:REL]->(x1), (x3)-[:REL]->(x2),                    (x3)-[:REL]->(x4), (x3)-[:REL]->(x5)" +
        ", (x4)-[:REL]->(xy), (x4)-[:REL]->(x1), (x4)-[:REL]->(x2), (x4)-[:REL]->(x3),                    (x4)-[:REL]->(x5)" +
        ", (x5)-[:REL]->(xy), (x5)-[:REL]->(x1), (x5)-[:REL]->(x2), (x5)-[:REL]->(x3), (x5)-[:REL]->(x4)" +
        ", (xy)-[:REL]->(y1)" +
        ", (y1)-[:REL]->(y2)";

    @Inject
    private TestGraph lollipopGraph;

    @Inject
    private IdFunction lollipopIdFunction;

    @GdlGraph(graphNamePrefix = "natural", orientation = NATURAL)
    private static final String DB_CYPHER_NATURAL =
        "CREATE" +
        "  (a:Person)" +
        ", (b:Person)" +
        ", (c:Person)" +
        ", (d:Person)" +
        ", (i1:Item)" +
        ", (i2:Item)" +
        ", (i3:Item)" +
        ", (i4:Item)" +
        ", (a)-[:LIKES {prop: 1.0}]->(i1)" +
        ", (a)-[:LIKES {prop: 1.0}]->(i2)" +
        ", (a)-[:LIKES {prop: 2.0}]->(i3)" +
        ", (b)-[:LIKES {prop: 1.0}]->(i1)" +
        ", (b)-[:LIKES {prop: 1.0}]->(i2)" +
        ", (c)-[:LIKES {prop: 1.0}]->(i3)" +
        ", (d)-[:LIKES {prop: 0.5}]->(i1)" +
        ", (d)-[:LIKES {prop: 1.0}]->(i2)" +
        ", (d)-[:LIKES {prop: 1.0}]->(i3)";

    @GdlGraph(graphNamePrefix = "naturalUnion", orientation = NATURAL)
    private static final String DB_CYPHER_UNION =
        "CREATE" +
        "  (a:Person)" +
        ", (b:Person)" +
        ", (c:Person)" +
        ", (d:Person)" +
        ", (i1:Item)" +
        ", (i2:Item)" +
        ", (i3:Item)" +
        ", (i4:Item)" +
        ", (a)-[:LIKES3 {prop: 1.0}]->(i1)" +
        ", (a)-[:LIKES2 {prop: 1.0}]->(i2)" +
        ", (a)-[:LIKES1 {prop: 2.0}]->(i3)" +
        ", (b)-[:LIKES2 {prop: 1.0}]->(i1)" +
        ", (b)-[:LIKES1 {prop: 1.0}]->(i2)" +
        ", (c)-[:LIKES3 {prop: 1.0}]->(i3)" +
        ", (d)-[:LIKES2 {prop: 0.5}]->(i1)" +
        ", (d)-[:LIKES3 {prop: 1.0}]->(i2)" +
        ", (d)-[:LIKES1 {prop: 1.0}]->(i3)";

    @Inject
    private TestGraph naturalGraph;

    @Inject
    private IdFunction naturalIdFunction;

    @Inject
    private TestGraph naturalUnionGraph;


    Graph getGraph(CommonNeighbourAwareRandomWalkConfig config) {
        return graphStore.getGraph(
            config.nodeLabelIdentifiers(graphStore),
            config.internalRelationshipTypes(graphStore),
            config.relationshipWeightProperty()
        );
    }

    @Test
    void shouldSample() {
        var config = CommonNeighbourAwareRandomWalkConfigImpl.builder()
            .startNodes(List.of(idFunction.of("a")))
            .samplingRatio(0.5)
            .restartProbability(0.1)
            .build();

        var cnarw = new CommonNeighbourAwareRandomWalk(config);
        var graph = getGraph(config);
        var nodes = cnarw.compute(graph, ProgressTracker.NULL_TRACKER);

        assertThat(nodes.cardinality()).isEqualTo(7);

        assertThat(nodes.get(graph.toMappedNodeId(idFunction.of("a")))).isTrue();
        assertThat(nodes.get(graph.toMappedNodeId(idFunction.of("b")))).isTrue();
        assertThat(nodes.get(graph.toMappedNodeId(idFunction.of("c")))).isTrue();
        assertThat(nodes.get(graph.toMappedNodeId(idFunction.of("d")))).isTrue();
        assertThat(nodes.get(graph.toMappedNodeId(idFunction.of("e")))).isTrue();
        assertThat(nodes.get(graph.toMappedNodeId(idFunction.of("f")))).isTrue();
        assertThat(nodes.get(graph.toMappedNodeId(idFunction.of("g")))).isTrue();
    }

    @Test
    void shouldSampleLollipop() {
        var config = CommonNeighbourAwareRandomWalkConfigImpl.builder()
            .startNodes(List.of(lollipopIdFunction.of("xy")))
            .samplingRatio(0.375)
            .restartProbability(0.0001)
            .concurrency(1)
            .randomSeed(777L)
            .build();

        var cnarw = new CommonNeighbourAwareRandomWalk(config);
        var nodes = cnarw.compute(lollipopGraph, ProgressTracker.NULL_TRACKER);

        assertThat(nodes.cardinality()).isEqualTo(3);

        assertThat(nodes.get(lollipopGraph.toMappedNodeId(lollipopIdFunction.of("xy")))).isTrue();
        assertThat(nodes.get(lollipopGraph.toMappedNodeId(lollipopIdFunction.of("y1")))).isTrue();
        assertThat(nodes.get(lollipopGraph.toMappedNodeId(lollipopIdFunction.of("y2")))).isTrue();
    }

    @Test
    void shouldSampleLollipopSeveral() {
        double casesPassedY1 = 0;
        double casesPassedX1 = 0;
        var validCases = 0;

        for (long seed = 0; seed < 1000; seed++) {
            var config = CommonNeighbourAwareRandomWalkConfigImpl.builder()
                .startNodes(List.of(lollipopIdFunction.of("xy")))
                .samplingRatio(0.250)
                .restartProbability(0.0001)
                .concurrency(1)
                .randomSeed(seed)
                .build();

            var cnarw = new CommonNeighbourAwareRandomWalk(config);
            var nodes = cnarw.compute(lollipopGraph, ProgressTracker.NULL_TRACKER);

            assertThat(nodes.cardinality()).isEqualTo(2);

            validCases++;

            if (nodes.get(lollipopGraph.toMappedNodeId(lollipopIdFunction.of("y1")))) {
                casesPassedY1++;
            }
            if (nodes.get(lollipopGraph.toMappedNodeId(lollipopIdFunction.of("x1")))) {
                casesPassedX1++;
            }
        }
        assertThat(casesPassedY1 / validCases).isCloseTo(0.5, Offset.offset(0.015));
        assertThat(casesPassedX1 / validCases).isCloseTo(0.1, Offset.offset(0.015));
    }

    @Test
    void shouldSampleWeighted() {
        double casesPassed = 0;
        var validCases = 0;
        for (long seed = 0; seed < 250; seed++) {
            var config = CommonNeighbourAwareRandomWalkConfigImpl.builder()
                .startNodes(List.of(idFunction.of("x")))
                .relationshipWeightProperty("distance")
                .samplingRatio(0.22)
                .restartProbability(0.01)
                .randomSeed(seed)
                .concurrency(1)
                .build();

            var graph = getGraph(config);
            var cnarw = new CommonNeighbourAwareRandomWalk(config);
            var nodes = cnarw.compute(graph, ProgressTracker.NULL_TRACKER);
            if (cnarw.startNodesUsed().contains(idFunction.of("x1")) ||
                cnarw.startNodesUsed().contains(idFunction.of("x2"))) {
                continue;
            }
            validCases++;

            assertThat(nodes.cardinality()).isEqualTo(3L);
            assertThat(nodes.get(graph.toMappedNodeId(idFunction.of("x1")))).isFalse();

            if (nodes.get(graph.toMappedNodeId(idFunction.of("x"))) &&
                !nodes.get(graph.toMappedNodeId(idFunction.of("x2"))) &&
                nodes.get(graph.toMappedNodeId(idFunction.of("x3")))
            ) {
                casesPassed++;
            }
        }

        assertThat(casesPassed / validCases).isCloseTo(0.637, Offset.offset(0.001));
    }

    @Test
    void shouldSampleWeightedConcurrently() {

        var x = idFunction.of("x");
        var x1 = idFunction.of("x1");
        var x2 = idFunction.of("x2");
        var x3 = idFunction.of("x3");
        var config = CommonNeighbourAwareRandomWalkConfigImpl.builder()
            .startNodes(List.of(x))
            .relationshipWeightProperty("distance")
            .concurrency(4)
            .samplingRatio(0.22)
            .restartProbability(0.01).build();
        var graph = getGraph(config);

        double casesPassed = 0;
        long validCases = 0;
        for (int i = 0; i < 1000; i++) {
            var cnarw = new CommonNeighbourAwareRandomWalk(config);
            var result = cnarw.compute(graph, ProgressTracker.NULL_TRACKER);
            if (
                cnarw.startNodesUsed().contains(x1) ||
                cnarw.startNodesUsed().contains(x2)
            ) {
                continue;
            }
            validCases++;

            assertThat(result.cardinality()).isBetween(3L, 5L);
            assertThat(result.get(graph.toMappedNodeId(x1))).isFalse();

            if (
                result.get(graph.toMappedNodeId(x)) &&
                !result.get(graph.toMappedNodeId(x2)) &&
                result.get(graph.toMappedNodeId(x3))
            ) {
                casesPassed++;
            }
        }

        assertThat(casesPassed / validCases).isCloseTo(0.35, Offset.offset(0.3));
    }

    @Test
    void shouldSampleWithFiltering() {
        var config = CommonNeighbourAwareRandomWalkConfigImpl.builder()
            .startNodes(List.of(idFunction.of("e")))
            .nodeLabels(List.of("M", "X"))
            .relationshipTypes(List.of("R1"))
            .samplingRatio(0.5)
            .restartProbability(0.1)
            .build();

        var cnarw = new CommonNeighbourAwareRandomWalk(config);
        var graph = getGraph(config);
        var nodes = cnarw.compute(graph, ProgressTracker.NULL_TRACKER);

        assertThat(nodes.cardinality()).isEqualTo(3);

        assertThat(nodes.get(graph.toMappedNodeId(idFunction.of("e")))).isTrue();
        assertThat(nodes.get(graph.toMappedNodeId(idFunction.of("f")))).isTrue();
        assertThat(nodes.get(graph.toMappedNodeId(idFunction.of("g")))).isTrue();
    }

    @Test
    void shouldRestartOnDeadEnd() {
        var config = CommonNeighbourAwareRandomWalkConfigImpl.builder()
            .nodeLabels(List.of("Z"))
            .relationshipTypes(List.of("R1"))
            .startNodes(List.of(idFunction.of("x")))
            .samplingRatio(0.999999999)
            .restartProbability(0.0000000001)
            .build();

        var cnarw = new CommonNeighbourAwareRandomWalk(config);
        var graph = getGraph(config);
        var nodes = cnarw.compute(graph, ProgressTracker.NULL_TRACKER);

        assertThat(nodes.cardinality()).isEqualTo(4);

        assertThat(nodes.get(graph.toMappedNodeId(idFunction.of("x")))).isTrue();
        assertThat(nodes.get(graph.toMappedNodeId(idFunction.of("x1")))).isTrue();
        assertThat(nodes.get(graph.toMappedNodeId(idFunction.of("x2")))).isTrue();
        assertThat(nodes.get(graph.toMappedNodeId(idFunction.of("x3")))).isTrue();
    }

    @Test
    void shouldBeDeterministic() {
        var config = CommonNeighbourAwareRandomWalkConfigImpl.builder()
            .samplingRatio(0.5)
            .startNodes(List.of(idFunction.of("a")))
            .restartProbability(0.1)
            .concurrency(1)
            .randomSeed(42L)
            .build();

        var cnarw = new CommonNeighbourAwareRandomWalk(config);

        var nodes1 = cnarw.compute(getGraph(config), ProgressTracker.NULL_TRACKER);
        var nodes2 = cnarw.compute(getGraph(config), ProgressTracker.NULL_TRACKER);

        assertThat(nodes1.cardinality()).isEqualTo(nodes2.cardinality());
        for (int i = 0; i < nodes1.size(); i++) {
            assertThat(nodes1.get(i)).isEqualTo(nodes2.get(i));
        }
    }

    @Test
    void shouldNotExploreNewStartNode() {
        var config = CommonNeighbourAwareRandomWalkConfigImpl.builder()
            .startNodes(List.of(idFunction.of("x")))
            .samplingRatio(4.0 / graphStore.nodeCount() + 0.001)
            .restartProbability(0.1)
            .build();

        var cnarw = new CommonNeighbourAwareRandomWalk(config);
        var graph = getGraph(config);
        var nodes = cnarw.compute(graph, ProgressTracker.NULL_TRACKER);

        assertThat(nodes.cardinality()).isEqualTo(4);

        assertThat(nodes.get(graph.toMappedNodeId(idFunction.of("x")))).isTrue();
        assertThat(nodes.get(graph.toMappedNodeId(idFunction.of("x1")))).isTrue();
        assertThat(nodes.get(graph.toMappedNodeId(idFunction.of("x2")))).isTrue();
        assertThat(nodes.get(graph.toMappedNodeId(idFunction.of("x3")))).isTrue();
    }

    @Test
    void shouldExploreNewStartNode() {
        var config = CommonNeighbourAwareRandomWalkConfigImpl.builder()
            .startNodes(List.of(idFunction.of("x")))
            .samplingRatio(5.0 / graphStore.nodeCount() + 0.001)
            .restartProbability(0.1)
            .build();

        var cnarw = new CommonNeighbourAwareRandomWalk(config);
        var nodes = cnarw.compute(getGraph(config), ProgressTracker.NULL_TRACKER);

        assertThat(nodes.cardinality()).isGreaterThan(4);
    }

    @Test
    void shouldUseMultipleStartNodes() {
        var config = CommonNeighbourAwareRandomWalkConfigImpl.builder()
            .startNodes(List.of(idFunction.of("x"), idFunction.of("a"), idFunction.of("h"), idFunction.of("j")))
            .samplingRatio(1)
            .restartProbability(0.05)
            .build();

        var cnarw = new CommonNeighbourAwareRandomWalk(config);
        var nodes = cnarw.compute(getGraph(config), ProgressTracker.NULL_TRACKER);

        assertThat(nodes.cardinality()).isEqualTo(14);
    }

    @Test
    void shouldSampleWithStratification() {
        var config = CommonNeighbourAwareRandomWalkConfigImpl.builder()
            .startNodes(List.of(idFunction.of("a")))
            .samplingRatio(0.5)
            .restartProbability(0.1)
            .randomSeed(42L)
            .concurrency(1)
            .nodeLabelStratification(true)
            .build();

        var cnarw = new CommonNeighbourAwareRandomWalk(config);
        var graph = getGraph(config);
        var nodes = cnarw.compute(graph, ProgressTracker.NULL_TRACKER);

        assertThat(nodes.cardinality()).isEqualTo(8);

        var expectedLabelCounts = Map.of(
            Set.of(NodeLabel.of("X")), 1L,
            Set.of(NodeLabel.of("Z")), 2L,
            Set.of(NodeLabel.of("M")), 3L,
            Set.of(NodeLabel.of("N")), 2L
        );
        var labelCounts = new HashMap<Set<NodeLabel>, Long>();
        for (long nodeId = 0; nodeId < nodes.size(); nodeId++) {
            if (!nodes.get(nodeId)) {
                continue;
            }

            var labelSet = new HashSet<>(graph.nodeLabels(nodeId));
            labelCounts.put(labelSet, 1L + labelCounts.getOrDefault(labelSet, 0L));
        }

        assertThat(labelCounts).isEqualTo(expectedLabelCounts);
    }

    @Test
    void shouldComputeForUnionGraphs() {

        HugeAtomicBitSet result1, result2;
        var config = CommonNeighbourAwareRandomWalkConfigImpl.builder()
            .startNodes(List.of(naturalIdFunction.of("a")))
            .relationshipWeightProperty("prop")
            .samplingRatio(0.5)
            .restartProbability(0.1)
            .randomSeed(42L)
            .concurrency(1)
            .build();
        {
            var cnarw = new CommonNeighbourAwareRandomWalk(config);
            result1 = cnarw.compute(naturalGraph, ProgressTracker.NULL_TRACKER);
        }
        {
            var cnarw = new CommonNeighbourAwareRandomWalk(config);
            result2 = cnarw.compute(naturalUnionGraph, ProgressTracker.NULL_TRACKER);
        }

        assertThat(result1.isEmpty()).isFalse();
        assertEquals(result1.size(), result2.size());
        for (int i = 0; i < result1.size(); i++) {
            assertEquals(result1.get(i), result2.get(i));
        }
    }

    @Test
    void memoryEstimationNodeCount() {
        var config = CommonNeighbourAwareRandomWalkConfigImpl.builder()
            .samplingRatio(0.5)
            .build();

        MemoryRange mem1k = CommonNeighbourAwareRandomWalk.memoryEstimation(config).estimate(
            GraphDimensions.of(1000),
            -1
        ).memoryUsage();

        MemoryRange mem10k = CommonNeighbourAwareRandomWalk.memoryEstimation(config).estimate(
            GraphDimensions.of(10000),
            -1
        ).memoryUsage();

        assertMemoryRange(mem1k, 116_224L);
        assertMemoryRange(mem10k, 1_161_352L);

        assertMemoryRangeIsClose(mem10k, mem1k.times(10), Percentage.withPercentage(1));
    }

    @Test
    void memoryEstimationSamplingRatio() {
        var config01 = CommonNeighbourAwareRandomWalkConfigImpl.builder().samplingRatio(0.1).build();
        var config05 = CommonNeighbourAwareRandomWalkConfigImpl.builder().samplingRatio(0.5).build();
        var config09 = CommonNeighbourAwareRandomWalkConfigImpl.builder().samplingRatio(0.9).build();

        MemoryRange mem01 = CommonNeighbourAwareRandomWalk.memoryEstimation(config01).estimate(
            GraphDimensions.of(10000),
            -1
        ).memoryUsage();

        MemoryRange mem05 = CommonNeighbourAwareRandomWalk.memoryEstimation(config05).estimate(
            GraphDimensions.of(10000),
            -1
        ).memoryUsage();

        MemoryRange mem09 = CommonNeighbourAwareRandomWalk.memoryEstimation(config09).estimate(
            GraphDimensions.of(10000),
            -1
        ).memoryUsage();

        assertMemoryRange(mem01, 745_352L);
        assertMemoryRange(mem05, 1_161_352L);
        assertMemoryRange(mem09, 1_577_352);

        MemoryRange delta = mem05.elementWiseSubtract(mem01);
        MemoryRange twoDelta = mem09.elementWiseSubtract(mem01);
        assertMemoryRangeIsClose(twoDelta, delta.times(2), Percentage.withPercentage(1));
    }

    @Test
    void memoryEstimationStartNodes() {
        var config = CommonNeighbourAwareRandomWalkConfigImpl.builder()
            .samplingRatio(0.5)
            .startNodes(LongStream.range(0, 1000).boxed().collect(Collectors.toList()))
            .build();

        assertMemoryEstimation(
            CommonNeighbourAwareRandomWalk.memoryEstimation(config),
            GraphDimensions.of(1000),
            -1,
            MemoryRange.of(132_224L)
        );
    }

    @Test
    void memoryEstimationStartNodesScale() {
        var config1k = CommonNeighbourAwareRandomWalkConfigImpl.builder()
            .samplingRatio(0.5)
            .startNodes(LongStream.range(0, 1000).boxed().collect(Collectors.toList()))
            .build();

        var config3k = CommonNeighbourAwareRandomWalkConfigImpl.builder()
            .samplingRatio(0.5)
            .startNodes(LongStream.range(0, 3000).boxed().collect(Collectors.toList()))
            .build();

        var config5k = CommonNeighbourAwareRandomWalkConfigImpl.builder()
            .samplingRatio(0.5)
            .startNodes(LongStream.range(0, 5000).boxed().collect(Collectors.toList()))
            .build();

        MemoryRange mem1k = CommonNeighbourAwareRandomWalk.memoryEstimation(config1k).estimate(
            GraphDimensions.of(10000),
            -1
        ).memoryUsage();

        MemoryRange mem3k = CommonNeighbourAwareRandomWalk.memoryEstimation(config3k).estimate(
            GraphDimensions.of(10000),
            -1
        ).memoryUsage();

        MemoryRange mem5k = CommonNeighbourAwareRandomWalk.memoryEstimation(config5k).estimate(
            GraphDimensions.of(10000),
            -1
        ).memoryUsage();

        assertMemoryRange(mem1k, 1_177_352L);
        assertMemoryRange(mem3k, 1_209_352L);
        assertMemoryRange(mem5k, 1_241_352L);

        MemoryRange delta = mem3k.elementWiseSubtract(mem1k);
        MemoryRange twoDelta = mem5k.elementWiseSubtract(mem1k);
        assertMemoryRangeIsClose(twoDelta, delta.times(2), Percentage.withPercentage(1));
    }

    @Test
    void memoryEstimationConcurrency() {
        var config = CommonNeighbourAwareRandomWalkConfigImpl.builder()
            .samplingRatio(0.5)
            .concurrency(1)
            .build();

        assertMemoryEstimation(
            CommonNeighbourAwareRandomWalk.memoryEstimation(config),
            GraphDimensions.of(1000),
            -1,
            MemoryRange.of(32_224L)
        );
    }

    @Test
    void memoryEstimationWeights() {
        var config = CommonNeighbourAwareRandomWalkConfigImpl.builder()
            .samplingRatio(0.5)
            .relationshipWeightProperty("prop")
            .build();

        assertMemoryEstimation(
            CommonNeighbourAwareRandomWalk.memoryEstimation(config),
            GraphDimensions.of(1000),
            -1,
            MemoryRange.of(124_264L)
        );
    }
}
