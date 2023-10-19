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
package org.neo4j.gds.louvain;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.gds.NodeLabel;
import org.neo4j.gds.Orientation;
import org.neo4j.gds.RelationshipType;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.api.schema.Direction;
import org.neo4j.gds.beta.generator.RandomGraphGenerator;
import org.neo4j.gds.beta.generator.RelationshipDistribution;
import org.neo4j.gds.compat.Neo4jProxy;
import org.neo4j.gds.config.RandomGraphGeneratorConfig;
import org.neo4j.gds.core.Aggregation;
import org.neo4j.gds.core.GraphDimensions;
import org.neo4j.gds.core.ImmutableGraphDimensions;
import org.neo4j.gds.core.concurrency.Pools;
import org.neo4j.gds.core.huge.HugeGraph;
import org.neo4j.gds.core.utils.TerminationFlag;
import org.neo4j.gds.core.utils.mem.MemoryRange;
import org.neo4j.gds.core.utils.mem.MemoryTree;
import org.neo4j.gds.core.utils.paged.HugeLongArray;
import org.neo4j.gds.core.utils.progress.EmptyTaskRegistryFactory;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.core.utils.progress.tasks.TaskProgressTracker;
import org.neo4j.gds.extension.GdlExtension;
import org.neo4j.gds.extension.GdlGraph;
import org.neo4j.gds.extension.IdFunction;
import org.neo4j.gds.extension.Inject;
import org.neo4j.gds.modularity.ModularityCalculator;

import java.util.Map;
import java.util.Optional;
import java.util.function.LongUnaryOperator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.neo4j.gds.CommunityHelper.assertCommunities;
import static org.neo4j.gds.CommunityHelper.assertCommunitiesWithLabels;
import static org.neo4j.gds.TestSupport.assertMemoryEstimation;
import static org.neo4j.gds.TestSupport.ids;
import static org.neo4j.gds.compat.TestLog.INFO;
import static org.neo4j.gds.core.ProcedureConstants.TOLERANCE_DEFAULT;
import static org.neo4j.gds.graphbuilder.TransactionTerminationTestUtils.assertTerminates;

@GdlExtension
class LouvainTest {

    static ImmutableLouvainStreamConfig.Builder defaultConfigBuilder() {
        return ImmutableLouvainStreamConfig.builder()
            .maxLevels(10)
            .maxIterations(10)
            .tolerance(TOLERANCE_DEFAULT)
            .includeIntermediateCommunities(true)
            .concurrency(1);
    }

    @GdlGraph(orientation = Orientation.UNDIRECTED, idOffset = 0)
    private static final String DB_CYPHER =
        "CREATE" +
        "  (a:Node {seed: 1,seed2: -1})" +        // 0
        ", (b:Node {seed: 1,seed2: 10})" +        // 1
        ", (c:Node {seed: 1})" +        // 2
        ", (d:Node {seed: 1})" +        // 3
        ", (e:Node {seed: 1})" +        // 4
        ", (f:Node {seed: 1})" +        // 5
        ", (g:Node {seed: 2})" +        // 6
        ", (h:Node {seed: 2})" +        // 7
        ", (i:Node {seed: 2})" +        // 8
        ", (j:Node {seed: 42})" +       // 9
        ", (k:Node {seed: 42})" +       // 10
        ", (l:Node {seed: 42})" +       // 11
        ", (m:Node {seed: 42})" +       // 12
        ", (n:Node {seed: 42})" +       // 13
        ", (x:Node {seed: 1})" +        // 14
        ", (u:Some)" +
        ", (v:Other)" +
        ", (w:Label)" +

        ", (a)-[:TYPE_OUT {weight: 1.0}]->(b)" +
        ", (a)-[:TYPE_OUT {weight: 1.0}]->(d)" +
        ", (a)-[:TYPE_OUT {weight: 1.0}]->(f)" +
        ", (b)-[:TYPE_OUT {weight: 1.0}]->(d)" +
        ", (b)-[:TYPE_OUT {weight: 1.0}]->(x)" +
        ", (b)-[:TYPE_OUT {weight: 1.0}]->(g)" +
        ", (b)-[:TYPE_OUT {weight: 1.0}]->(e)" +
        ", (c)-[:TYPE_OUT {weight: 1.0}]->(x)" +
        ", (c)-[:TYPE_OUT {weight: 1.0}]->(f)" +
        ", (d)-[:TYPE_OUT {weight: 1.0}]->(k)" +
        ", (e)-[:TYPE_OUT {weight: 1.0}]->(x)" +
        ", (e)-[:TYPE_OUT {weight: 0.01}]->(f)" +
        ", (e)-[:TYPE_OUT {weight: 1.0}]->(h)" +
        ", (f)-[:TYPE_OUT {weight: 1.0}]->(g)" +
        ", (g)-[:TYPE_OUT {weight: 1.0}]->(h)" +
        ", (h)-[:TYPE_OUT {weight: 1.0}]->(i)" +
        ", (h)-[:TYPE_OUT {weight: 1.0}]->(j)" +
        ", (i)-[:TYPE_OUT {weight: 1.0}]->(k)" +
        ", (j)-[:TYPE_OUT {weight: 1.0}]->(k)" +
        ", (j)-[:TYPE_OUT {weight: 1.0}]->(m)" +
        ", (j)-[:TYPE_OUT {weight: 1.0}]->(n)" +
        ", (k)-[:TYPE_OUT {weight: 1.0}]->(m)" +
        ", (k)-[:TYPE_OUT {weight: 1.0}]->(l)" +
        ", (l)-[:TYPE_OUT {weight: 1.0}]->(n)" +
        ", (m)-[:TYPE_OUT {weight: 1.0}]->(n)";

    @Inject
    private GraphStore graphStore;

    @Inject
    private IdFunction idFunction;

    @Test
    void testUnweighted() {
        var graph = graphStore.getGraph(
            NodeLabel.listOf("Node"),
            RelationshipType.listOf("TYPE_OUT"),
            Optional.empty()
        );

        IdFunction mappedId = name -> graph.toMappedNodeId(idFunction.of(name));

        var config = defaultConfigBuilder().build();
        Louvain algorithm = new Louvain(
            graph,
            config,
            config.includeIntermediateCommunities(),
            config.maxLevels(),
            config.maxIterations(),
            config.tolerance(),
            config.concurrency(),
            ProgressTracker.NULL_TRACKER,
            Pools.DEFAULT

        );
        algorithm.setTerminationFlag(TerminationFlag.RUNNING_TRUE);

        var result = algorithm.compute();

        final HugeLongArray[] dendrogram = result.dendrogramManager().getAllDendrograms();
        final double[] modularities = result.modularities();

        assertCommunities(
            dendrogram[0],
            ids(mappedId, "a", "b", "d"),
            ids(mappedId, "c", "e", "f", "x"),
            ids(mappedId, "g", "h", "i"),
            ids(mappedId, "j", "k", "l", "m", "n")
        );

        assertCommunities(
            dendrogram[1],
            ids(mappedId, "a", "b", "c", "d", "e", "f", "x"),
            ids(mappedId, "g", "h", "i"),
            ids(mappedId, "j", "k", "l", "m", "n")
        );

        assertEquals(2, result.ranLevels());
        assertEquals(0.38, modularities[modularities.length - 1], 0.01);
    }

    @Test
    void testWeighted() {
        var graph = graphStore.getGraph(
            NodeLabel.listOf("Node"),
            RelationshipType.listOf("TYPE_OUT"),
            Optional.of("weight")
        );

        IdFunction mappedId = name -> graphStore.getGraph(NodeLabel.of("Node")).toMappedNodeId(idFunction.of(name));

        var config = defaultConfigBuilder().build();
        Louvain algorithm = new Louvain(
            graph,
            config,
            config.includeIntermediateCommunities(),
            config.maxLevels(),
            config.maxIterations(),
            config.tolerance(),
            config.concurrency(),
            ProgressTracker.NULL_TRACKER,
            Pools.DEFAULT

        );
        algorithm.setTerminationFlag(TerminationFlag.RUNNING_TRUE);

        var result = algorithm.compute();

        final HugeLongArray[] dendrogram = result.dendrogramManager().getAllDendrograms();
        final double[] modularities = result.modularities();

        assertCommunities(
            dendrogram[0],
            ids(mappedId, "a", "b", "d"),
            ids(mappedId, "c", "e", "x"),
            ids(mappedId, "f", "g"),
            ids(mappedId, "h", "i"),
            ids(mappedId, "j", "k", "l", "m", "n")
        );

        assertCommunities(
            dendrogram[1],
            ids(mappedId, "a", "b", "c", "d", "e", "f", "g", "x"),
            ids(mappedId, "h", "i", "j", "k", "l", "m", "n")
        );

        assertEquals(2, result.ranLevels());
        assertEquals(0.37, modularities[modularities.length - 1], 0.01);
    }

    @Test
    void testSeeded() {
        var graph = graphStore.getGraph(
            NodeLabel.listOf("Node"),
            RelationshipType.listOf("TYPE_OUT"),
            Optional.of("weight")
        );

        IdFunction mappedId = name -> graphStore.getGraph(NodeLabel.of("Node")).toMappedNodeId(idFunction.of(name));

        var config = defaultConfigBuilder().seedProperty("seed").build();
        Louvain algorithm = new Louvain(
            graph,
            config,
            config.includeIntermediateCommunities(),
            config.maxLevels(),
            config.maxIterations(),
            config.tolerance(),
            config.concurrency(),
            ProgressTracker.NULL_TRACKER,
            Pools.DEFAULT

        );
        algorithm.setTerminationFlag(TerminationFlag.RUNNING_TRUE);

        var result = algorithm.compute();

        final HugeLongArray[] dendrogram = result.dendrogramManager().getAllDendrograms();
        final double[] modularities = result.modularities();

        var expectedCommunitiesWithLabels = Map.of(
            1L, ids(mappedId, "a", "b", "c", "d", "e", "f", "x"),
            2L, ids(mappedId, "g", "h", "i"),
            42L, ids(mappedId, "j", "k", "l", "m", "n")
        );

        assertCommunitiesWithLabels(
            dendrogram[0],
            expectedCommunitiesWithLabels
        );

        assertEquals(1, result.ranLevels());
        assertEquals(0.38, modularities[modularities.length - 1], 0.01);
    }

    @Test
    void testTolerance() {
        var graph = graphStore.getGraph(
            NodeLabel.listOf("Node"),
            RelationshipType.listOf("TYPE_OUT"),
            Optional.empty()
        );

        var config =
            ImmutableLouvainStreamConfig.builder()
                .maxLevels(10)
                .maxIterations(10)
                .tolerance(2.0)
                .includeIntermediateCommunities(false)
                .concurrency(1)
                .build();

        Louvain algorithm = new Louvain(
            graph,
            config,
            config.includeIntermediateCommunities(),
            config.maxLevels(),
            config.maxIterations(),
            config.tolerance(),
            config.concurrency(),
            ProgressTracker.NULL_TRACKER,
            Pools.DEFAULT

        );
        algorithm.setTerminationFlag(TerminationFlag.RUNNING_TRUE);

        var result = algorithm.compute();
        
        assertEquals(1, result.ranLevels());
    }

    @Test
    void testMaxLevels() {
        var graph = graphStore.getGraph(
            NodeLabel.listOf("Node"),
            RelationshipType.listOf("TYPE_OUT"),
            Optional.empty()
        );

        var config =
            ImmutableLouvainStreamConfig.builder()
                .maxLevels(1)
                .maxIterations(10)
                .tolerance(TOLERANCE_DEFAULT)
                .includeIntermediateCommunities(false)
                .concurrency(1)
                .build();

        Louvain algorithm = new Louvain(
            graph,
            config,
            config.includeIntermediateCommunities(),
            config.maxLevels(),
            config.maxIterations(),
            config.tolerance(),
            config.concurrency(),
            ProgressTracker.NULL_TRACKER,
            Pools.DEFAULT
        );
        algorithm.setTerminationFlag(TerminationFlag.RUNNING_TRUE);

        var result = algorithm.compute();
        assertEquals(1, result.ranLevels());
    }

    static Stream<Arguments> memoryEstimationTuples() {
        return Stream.of(

            arguments(1, 1, true, 6414153, 23057712),
            arguments(1, 1, false, 6414153, 23057712),
            arguments(1, 10, true, 6414153, 30258072),
            arguments(1, 10, false, 6414153, 23857752),

            arguments(4, 1, true, 6417441, 29057976),
            arguments(4, 1, false, 6417441, 29057976),
            arguments(4, 10, true, 6417441, 36258336),
            arguments(4, 10, false, 6417441, 29858016),

            arguments(42, 1, true, 6459089, 105061320),
            arguments(42, 1, false, 6459089, 105061320),
            arguments(42, 10, true, 6459089, 112261680),
            arguments(42, 10, false, 6459089, 105861360)

        );
    }


    @ParameterizedTest
    @MethodSource("memoryEstimationTuples")
    void testMemoryEstimation(
        int concurrency,
        int levels,
        boolean includeIntermediateCommunities,
        long expectedMinBytes,
        long expectedMaxBytes
    ) {
        var nodeCount = 100_000L;
        var relCount = 500_000L;

        LouvainStreamConfig config = ImmutableLouvainStreamConfig.builder()
            .maxLevels(levels)
            .maxIterations(10)
            .tolerance(TOLERANCE_DEFAULT)
            .includeIntermediateCommunities(includeIntermediateCommunities)
            .concurrency(1)
            .build();

        assertMemoryEstimation(
            () -> new LouvainAlgorithmFactory<>().memoryEstimation(config),
            nodeCount,
            relCount,
            concurrency,
            MemoryRange.of(expectedMinBytes, expectedMaxBytes)
        );
    }

    @Test
    void testMemoryEstimationUsesOnlyOnePropertyForEachEntity() {
        ImmutableGraphDimensions.Builder dimensionsBuilder = ImmutableGraphDimensions.builder()
            .nodeCount(100_000L)
            .relCountUpperBound(500_000L);

        GraphDimensions dimensionsWithoutProperties = dimensionsBuilder.build();
        GraphDimensions dimensionsWithOneProperty = dimensionsBuilder
            .putRelationshipPropertyToken("foo", 1)
            .build();
        GraphDimensions dimensionsWithTwoProperties = dimensionsBuilder
            .putRelationshipPropertyToken("foo", 1)
            .putRelationshipPropertyToken("bar", 1)
            .build();

        LouvainStreamConfig config = ImmutableLouvainStreamConfig.builder()
            .maxLevels(1)
            .maxIterations(10)
            .tolerance(TOLERANCE_DEFAULT)
            .includeIntermediateCommunities(false)
            .concurrency(1)
            .build();

        MemoryTree memoryTree = new LouvainAlgorithmFactory<>()
            .memoryEstimation(config)
            .estimate(dimensionsWithoutProperties, 1);
        MemoryTree memoryTreeOneProperty = new LouvainAlgorithmFactory<>()
            .memoryEstimation(config)
            .estimate(dimensionsWithOneProperty, 1);
        MemoryTree memoryTreeTwoProperties = new LouvainAlgorithmFactory<>()
            .memoryEstimation(config)
            .estimate(dimensionsWithTwoProperties, 1);

        assertEquals(memoryTree.memoryUsage(), memoryTreeOneProperty.memoryUsage());
        assertEquals(memoryTreeOneProperty.memoryUsage(), memoryTreeTwoProperties.memoryUsage());
    }

    @Test
    void testCanBeInterruptedByTxCancellation() {
        HugeGraph graph = RandomGraphGenerator.builder()
            .nodeCount(100_000)
            .averageDegree(10)
            .relationshipDistribution(RelationshipDistribution.UNIFORM)
            .build()
            .generate();

        var config = defaultConfigBuilder().concurrency(2).build();
        assertTerminates((terminationFlag) ->
            {
                Louvain louvain = new Louvain(
                    graph,
                    config,
                    config.includeIntermediateCommunities(),
                    config.maxLevels(),
                    config.maxIterations(),
                    config.tolerance(),
                    config.concurrency(),
                    ProgressTracker.NULL_TRACKER,
                    Pools.DEFAULT

                );
                louvain.setTerminationFlag(terminationFlag);
                louvain.compute();
            }, 500, 1000
        );
    }

    @Test
    void testLogging() {
        var graph = graphStore.getGraph(
            NodeLabel.listOf("Node"),
            RelationshipType.listOf("TYPE_OUT"),
            Optional.empty()
        );

        var config = defaultConfigBuilder().build();

        var progressTask = new LouvainAlgorithmFactory<>().progressTask(graph, config);
        var log = Neo4jProxy.testLog();
        var progressTracker = new TaskProgressTracker(
            progressTask,
            log,
            config.concurrency(),
            EmptyTaskRegistryFactory.INSTANCE
        );

        Louvain louvain = new Louvain(
            graph,
            config,
            config.includeIntermediateCommunities(),
            config.maxLevels(),
            config.maxIterations(),
            config.tolerance(),
            config.concurrency(),
            progressTracker,
            Pools.DEFAULT

        );

        louvain.compute();

        assertTrue(log.containsMessage(INFO, ":: Start"));
        assertTrue(log.containsMessage(INFO, ":: Finished"));
    }

    @Test
    void shouldThrowOnNegativeSeed() {
        var graph = graphStore.getGraph(
            NodeLabel.listOf("Node"),
            RelationshipType.listOf("TYPE_OUT"),
            Optional.of("weight")
        );

        var config = defaultConfigBuilder().seedProperty("seed2").build();
        Louvain algorithm = new Louvain(
            graph,
            config,
            config.includeIntermediateCommunities(),
            config.maxLevels(),
            config.maxIterations(),
            config.tolerance(),
            config.concurrency(),
            ProgressTracker.NULL_TRACKER,
            Pools.DEFAULT

        );
        algorithm.setTerminationFlag(TerminationFlag.RUNNING_TRUE);

        assertThatThrownBy(algorithm::compute).hasMessageContaining("non-negative");

    }

    @Test
    void shouldGiveSameResultWithCalculator() {
        var myGraph = RandomGraphGenerator
            .builder()
            .nodeCount(1_000)
            .averageDegree(10)
            .relationshipDistribution(RelationshipDistribution.UNIFORM)
            .direction(Direction.UNDIRECTED)
            .allowSelfLoops(RandomGraphGeneratorConfig.AllowSelfLoops.YES)
            .aggregation(Aggregation.SINGLE)
            .seed(42)
            .build()
            .generate();

        var louvain = new Louvain(
            myGraph,
            ImmutableLouvainStreamConfig.builder().build(),
            false,
            10,
            10,
            TOLERANCE_DEFAULT,
            4,
            ProgressTracker.NULL_TRACKER,
            Pools.DEFAULT
        );

        var result = louvain.compute();
        assertThat(result.ranLevels()).isGreaterThan(1);
        LongUnaryOperator vToCommunity = v -> result.getCommunity(v);
        var modularityCalculator = ModularityCalculator.create(myGraph, vToCommunity, 4);
        double calculatedModularity = modularityCalculator.compute().totalModularity();
        assertThat(result.modularity()).isCloseTo(calculatedModularity, Offset.offset(1e-5));
    }
}
