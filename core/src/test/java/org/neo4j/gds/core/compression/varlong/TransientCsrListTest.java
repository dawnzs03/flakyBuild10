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
package org.neo4j.gds.core.compression.varlong;

import org.apache.commons.lang3.mutable.MutableLong;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.gds.Orientation;
import org.neo4j.gds.RelationshipType;
import org.neo4j.gds.TestSupport;
import org.neo4j.gds.api.AdjacencyCursor;
import org.neo4j.gds.api.AdjacencyList;
import org.neo4j.gds.api.IdMap;
import org.neo4j.gds.core.TestMethodRunner;
import org.neo4j.gds.core.compression.packed.PackedAdjacencyList;
import org.neo4j.gds.core.concurrency.Pools;
import org.neo4j.gds.core.loading.construction.GraphFactory;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.neo4j.gds.api.AdjacencyCursor.NOT_FOUND;
import static org.neo4j.gds.core.compression.varlong.AdjacencyDecompressingReader.CHUNK_SIZE;

class TransientCsrListTest {

    @ParameterizedTest
    @MethodSource("org.neo4j.gds.core.TestMethodRunner#adjacencyCompressions")
    void shouldPeekValues(TestMethodRunner runner) {
        runner.run(() -> {
            withAdjacencyCursor(new long[]{0, 1, 3, 3, 7}, adjacencyCursor -> {
                while (adjacencyCursor.hasNextVLong()) {
                    assertEquals(adjacencyCursor.peekVLong(), adjacencyCursor.nextVLong());
                }
            });
        });
    }

    @ParameterizedTest
    @MethodSource("org.neo4j.gds.core.TestMethodRunner#adjacencyCompressions")
    void shouldSkipUntilLargerValue(TestMethodRunner runner) {
        runner.run(() -> {
            withAdjacencyCursor(new long[]{0, 1, 2, 2, 3}, adjacencyCursor -> {
                assertEquals(2, adjacencyCursor.skipUntil(1));
                assertEquals(2, adjacencyCursor.nextVLong());
                assertEquals(3, adjacencyCursor.nextVLong());
                assertFalse(adjacencyCursor.hasNextVLong());
            });
        });
    }

    @ParameterizedTest
    @MethodSource("org.neo4j.gds.core.TestMethodRunner#adjacencyCompressions")
    void shouldAdvanceUntilEqualValue(TestMethodRunner runner) {
        runner.run(() -> {
            withAdjacencyCursor(new long[]{0, 1, 2, 2, 3}, adjacencyCursor -> {
                assertEquals(1, adjacencyCursor.advance(1));
                assertEquals(2, adjacencyCursor.nextVLong());
                assertEquals(2, adjacencyCursor.nextVLong());
                assertEquals(3, adjacencyCursor.nextVLong());
                assertFalse(adjacencyCursor.hasNextVLong());
            });
        });
    }

    @ParameterizedTest
    @MethodSource("org.neo4j.gds.core.TestMethodRunner#adjacencyCompressions")
    void advanceOutOfUpperBound(TestMethodRunner runner) {
        runner.run(() -> {
            withAdjacencyCursor(new long[]{0, 1, 2, 2, 3}, adjacencyCursor -> {
                assertThat(adjacencyCursor.advance(5)).isEqualTo(NOT_FOUND);
                assertFalse(adjacencyCursor.hasNextVLong());
            });

            withAdjacencyCursor(new long[]{0, 1, 2, 2, 3}, adjacencyCursor -> {
                assertThat(adjacencyCursor.advance(3)).isEqualTo(3);
                assertFalse(adjacencyCursor.hasNextVLong());
            });
        });
    }

    @ParameterizedTest
    @MethodSource("org.neo4j.gds.core.TestMethodRunner#adjacencyCompressions")
    void advanceOutOfLowerBound(TestMethodRunner runner) {
        runner.run(() -> {
            withAdjacencyCursor(new long[]{0, 1, 2, 2, 3}, adjacencyCursor -> {
                assertThat(adjacencyCursor.advance(1)).isEqualTo(1);
                assertTrue(adjacencyCursor.hasNextVLong());
            });
        });
    }

    @ParameterizedTest
    @MethodSource("org.neo4j.gds.core.TestMethodRunner#adjacencyCompressions")
    void skipUntilOutOfUpperBound(TestMethodRunner runner) {
        runner.run(() -> {
            withAdjacencyCursor(new long[]{0, 1, 2, 2, 3}, adjacencyCursor -> {
                assertThat(adjacencyCursor.skipUntil(5)).isEqualTo(NOT_FOUND);
                assertFalse(adjacencyCursor.hasNextVLong());
            });

            withAdjacencyCursor(new long[]{0, 1, 2, 2, 3}, adjacencyCursor -> {
                assertThat(adjacencyCursor.skipUntil(3)).isEqualTo(NOT_FOUND);
                assertFalse(adjacencyCursor.hasNextVLong());
            });
        });
    }

    @ParameterizedTest
    @MethodSource("org.neo4j.gds.core.TestMethodRunner#adjacencyCompressions")
    void skipUntilOutOfLowerBound(TestMethodRunner runner) {
        runner.run(() -> {
            withAdjacencyCursor(new long[]{0, 1, 2, 2, 3}, adjacencyCursor -> {
                assertThat(adjacencyCursor.skipUntil(1)).isEqualTo(2);
                assertTrue(adjacencyCursor.hasNextVLong());
            });
        });
    }

    @ParameterizedTest
    @MethodSource("org.neo4j.gds.core.TestMethodRunner#adjacencyCompressions")
    void shouldPeekAcrossBlocks(TestMethodRunner runner) {
        runner.run(() -> {
            int targetCount = 2 * CHUNK_SIZE;
            long[] targets = new long[targetCount + 1];
            Arrays.setAll(targets, i -> i);
            withAdjacencyCursor(targets, adjacencyCursor -> {
                int position = 0;
                while (adjacencyCursor.hasNextVLong() && position < targetCount) {
                    assertThat(adjacencyCursor.peekVLong()).isEqualTo(position);
                    assertThat(adjacencyCursor.nextVLong()).isEqualTo(position);
                    position++;
                }

                assertEquals(1, adjacencyCursor.remaining());
                assertEquals(targetCount, adjacencyCursor.peekVLong());
                assertEquals(targetCount, adjacencyCursor.peekVLong());
                assertEquals(targetCount, adjacencyCursor.nextVLong());
            });
        });
    }

    @ParameterizedTest
    @MethodSource("org.neo4j.gds.core.TestMethodRunner#adjacencyCompressions")
    void shouldNextAcrossBlocks(TestMethodRunner runner) {
        runner.run(() -> {
            int targetCount = 2 * CHUNK_SIZE;
            long[] targets = new long[targetCount + 1];
            Arrays.setAll(targets, i -> i);
            withAdjacencyCursor(targets, adjacencyCursor -> {
                int position = 0;
                while (adjacencyCursor.hasNextVLong() && position < CHUNK_SIZE) {
                    assertThat(adjacencyCursor.peekVLong()).isEqualTo(position);
                    assertThat(adjacencyCursor.nextVLong()).isEqualTo(position);
                    assertThat(adjacencyCursor.peekVLong()).isEqualTo(position + 1);
                    position++;
                }

                while (adjacencyCursor.hasNextVLong() && position < 2 * CHUNK_SIZE) {
                    assertThat(adjacencyCursor.peekVLong()).isEqualTo(position);
                    assertThat(adjacencyCursor.nextVLong()).isEqualTo(position);
                    position++;
                }

                assertEquals(1, adjacencyCursor.remaining());
                assertEquals(targetCount, adjacencyCursor.nextVLong());
            });
        });
    }

    @ParameterizedTest
    @MethodSource("org.neo4j.gds.core.TestMethodRunner#adjacencyCompressions")
    void advanceBy(TestMethodRunner runner) {
        runner.run(() -> {
            withAdjacencyCursor(new long[]{0, 1, 2, 3, 4}, adjacencyCursor -> {
                assertThat(adjacencyCursor.advanceBy(0)).isEqualTo(0);
                assertThat(adjacencyCursor.nextVLong()).isEqualTo(1);
                assertTrue(adjacencyCursor.hasNextVLong());
            });

            withAdjacencyCursor(new long[]{0, 1, 2, 3, 4}, adjacencyCursor -> {
                assertThat(adjacencyCursor.advanceBy(1)).isEqualTo(1);
                assertThat(adjacencyCursor.nextVLong()).isEqualTo(2);
                assertTrue(adjacencyCursor.hasNextVLong());
            });

            withAdjacencyCursor(new long[]{0, 1, 2, 3, 4}, adjacencyCursor -> {
                assertThat(adjacencyCursor.advanceBy(2)).isEqualTo(2);
                assertThat(adjacencyCursor.nextVLong()).isEqualTo(3);
                assertTrue(adjacencyCursor.hasNextVLong());
            });

            withAdjacencyCursor(new long[]{0, 1, 2, 3, 4}, adjacencyCursor -> {
                assertThat(adjacencyCursor.advanceBy(3)).isEqualTo(3);
                assertThat(adjacencyCursor.nextVLong()).isEqualTo(4);
                assertFalse(adjacencyCursor.hasNextVLong());
            });

            withAdjacencyCursor(new long[]{0, 1, 2, 3, 4}, adjacencyCursor -> {
                assertThat(adjacencyCursor.advanceBy(5)).isEqualTo(NOT_FOUND);
                assertFalse(adjacencyCursor.hasNextVLong());
            });
        });
    }

    @ParameterizedTest()
    @MethodSource("org.neo4j.gds.core.TestMethodRunner#adjacencyCompressions")
    void advanceByAcrossBlocks(TestMethodRunner runner) {
        runner.run(() -> {
            withAdjacencyCursor(2 * CHUNK_SIZE, adjacencyCursor -> {
                assertThat(adjacencyCursor.advanceBy(CHUNK_SIZE)).isEqualTo(CHUNK_SIZE);
                assertThat(adjacencyCursor.nextVLong()).isEqualTo(CHUNK_SIZE + 1);
            });

            withAdjacencyCursor(2 * CHUNK_SIZE, adjacencyCursor -> {
                assertThat(adjacencyCursor.nextVLong()).isEqualTo(0);
                assertThat(adjacencyCursor.advanceBy(CHUNK_SIZE)).isEqualTo(CHUNK_SIZE + 1);
                assertThat(adjacencyCursor.nextVLong()).isEqualTo(CHUNK_SIZE + 2);
            });

            withAdjacencyCursor(3 * CHUNK_SIZE, adjacencyCursor -> {
                assertThat(adjacencyCursor.advanceBy(2 * CHUNK_SIZE + CHUNK_SIZE / 2)).isEqualTo(2 * CHUNK_SIZE + CHUNK_SIZE / 2);
            });
        });
    }

    @ParameterizedTest
    @MethodSource("org.neo4j.gds.core.TestMethodRunner#adjacencyCompressions")
    void advanceByAcrossBlocksWithTail(TestMethodRunner runner) {
        runner.run(() -> {
            withAdjacencyCursor(CHUNK_SIZE + CHUNK_SIZE / 2, adjacencyCursor -> {
                assertThat(adjacencyCursor.advanceBy(CHUNK_SIZE)).isEqualTo(CHUNK_SIZE);
                assertThat(adjacencyCursor.nextVLong()).isEqualTo(CHUNK_SIZE + 1);
            });

            withAdjacencyCursor(CHUNK_SIZE + CHUNK_SIZE / 3, adjacencyCursor -> {
                assertThat(adjacencyCursor.nextVLong()).isEqualTo(0);
                assertThat(adjacencyCursor.advanceBy(CHUNK_SIZE)).isEqualTo(CHUNK_SIZE + 1);
                assertThat(adjacencyCursor.nextVLong()).isEqualTo(CHUNK_SIZE + 2);
            });

            withAdjacencyCursor(3 * CHUNK_SIZE - 2, adjacencyCursor -> {
                assertThat(adjacencyCursor.advanceBy(2 * CHUNK_SIZE + CHUNK_SIZE / 2)).isEqualTo(2 * CHUNK_SIZE + CHUNK_SIZE / 2);
            });
        });
    }

    @ParameterizedTest
    @MethodSource("org.neo4j.gds.core.TestMethodRunner#adjacencyCompressions")
    void shallowCopy(TestMethodRunner runner) {
        runner.run(() -> {
            withAdjacencyCursor(new long[]{0, 1, 2, 3, 4}, sourceCursor -> {
                assertThat(sourceCursor.nextVLong()).isEqualTo(0);
                withAdjacencyCursor(1, targetCursor -> {
                    targetCursor = sourceCursor.shallowCopy(targetCursor);
                    assertThat(targetCursor.nextVLong()).isEqualTo(1);
                    assertThat(targetCursor.nextVLong()).isEqualTo(2);
                    assertThat(targetCursor.nextVLong()).isEqualTo(3);
                    assertThat(targetCursor.nextVLong()).isEqualTo(4);
                    assertFalse(targetCursor.hasNextVLong());
                });
            });
        });
    }

    @ParameterizedTest
    @MethodSource("org.neo4j.gds.core.TestMethodRunner#adjacencyCompressions")
    void shallowCopyAcrossBlocks(TestMethodRunner runner) {
        runner.run(() -> {

            withAdjacencyCursor(3 * CHUNK_SIZE, sourceCursor -> {
                assertThat(sourceCursor.advanceBy(2 * CHUNK_SIZE + 1)).isEqualTo(2 * CHUNK_SIZE + 1);

                withAdjacencyCursor(1, targetCursor -> {
                    targetCursor = sourceCursor.shallowCopy(targetCursor);
                    assertThat(targetCursor.nextVLong()).isEqualTo(2 * CHUNK_SIZE + 2);
                    assertThat(targetCursor.nextVLong()).isEqualTo(2 * CHUNK_SIZE + 3);
                    assertThat(targetCursor.nextVLong()).isEqualTo(2 * CHUNK_SIZE + 4);
                    assertThat(targetCursor.nextVLong()).isEqualTo(2 * CHUNK_SIZE + 5);
                });
            });
        });
    }

    @ParameterizedTest
    @MethodSource("org.neo4j.gds.core.TestMethodRunner#adjacencyCompressions")
    void memoryInfo(TestMethodRunner runner) {
        runner.run(() -> {
            withAdjacencyList(3 * CHUNK_SIZE, adjacencyList -> {
                var memoryInfo = adjacencyList.memoryInfo();
                assertThat(memoryInfo.bytesTotal()).isPresent();
                assertThat(memoryInfo.bytesTotal().getAsLong()).isGreaterThan(0);
            });
        });
    }

    static Stream<Arguments> testRunnersAndDegrees() {
        return TestSupport.crossArguments(
            () -> TestMethodRunner.adjacencyCompressions().map(Arguments::of),
            () -> Stream.of(
                // creates 3 pages .. page0: 200_000, page1 (oversize): node count, page2: 200_000
                Arguments.of(200_000, 200_000),
                // creates 2 pages .. page0: 1337, page1 (oversize): node count, page0 (reuse): 42
                Arguments.of(1337, 42)
            )
        );
    }

    @ParameterizedTest
    @MethodSource("testRunnersAndDegrees")
    void shouldWorkWithVeryDenseNodes(TestMethodRunner runner, long firstDegree, long secondDegree) {
        runner.run(() -> {
            int nodeCount = 1_000_000;

            var nodesBuilder = GraphFactory.initNodesBuilder()
                .nodeCount(nodeCount)
                .maxOriginalId(nodeCount)
                .hasLabelInformation(false)
                .build();

            for (int i = 0; i < nodeCount; i++) {
                nodesBuilder.addNode(i);
            }

            var nodes = nodesBuilder.build();

            var relsBuilder = GraphFactory.initRelationshipsBuilder()
                .nodes(nodes.idMap())
                .relationshipType(RelationshipType.of("REL"))
                .orientation(Orientation.UNDIRECTED)
                .build();

            for (int i = 1; i <= firstDegree; i++) {
                relsBuilder.add(0, i);
            }

            for (int i = 2; i < nodeCount; i++) {
                relsBuilder.add(1, i);
            }

            for (int i = 3; i < secondDegree; i++) {
                relsBuilder.add(2, i + 100_000);
            }

            var rels = relsBuilder.build();

            var graph = GraphFactory.create(nodes.idMap(), rels);

            assertThat(graph.nodeCount()).isEqualTo(nodeCount);

            assertThat(graph.degree(0)).isEqualTo(firstDegree);
            assertThat(graph.degree(1)).isEqualTo(nodeCount - 1);
            assertThat(graph.degree(2)).isEqualTo(secondDegree - 1);


            var sum0 = new MutableLong(0);
            graph.forEachRelationship(0, (sourceNodeId, targetNodeId) -> {
                sum0.add(targetNodeId);
                return true;
            });
            assertThat(sum0.longValue()).isEqualTo(LongStream.rangeClosed(1, firstDegree).sum());


            var sum1 = new MutableLong(0);
            graph.forEachRelationship(1, (sourceNodeId, targetNodeId) -> {
                sum1.add(targetNodeId);
                return true;
            });
            assertThat(sum1.longValue()).isEqualTo(LongStream.range(2, nodeCount).sum());


            var sum2 = new MutableLong(0);
            graph.forEachRelationship(2, (sourceNodeId, targetNodeId) -> {
                sum2.add(targetNodeId);
                return true;
            });
            assertThat(sum2.longValue())
                .isEqualTo(LongStream.range(3, secondDegree).map(i -> i + 100_000).sum() + /* undirected from 1 */ 1);
        });
    }

    static void withAdjacencyCursor(int length, Consumer<AdjacencyCursor> consumer) {
        withAdjacencyCursor(IntStream.range(0, length).mapToLong(i -> i).toArray(), consumer);
    }

    static void withAdjacencyCursor(long[] targets, Consumer<AdjacencyCursor> consumer) {
        IdMap idMap = idMap(targets);
        long mappedNodeId = idMap.toMappedNodeId(targets[0]);

        withAdjacencyList(idMap, targets, adjacencyList -> {
            var adjacencyCursor = adjacencyList.adjacencyCursor(mappedNodeId);
            consumer.accept(adjacencyCursor);
        });
    }

    static void withAdjacencyList(int length, Consumer<AdjacencyList> consumer) {
        var targets = IntStream.range(0, length).mapToLong(i -> i).toArray();
        withAdjacencyList(idMap(targets), targets, consumer);
    }

    static void withAdjacencyList(IdMap idMap, long[] targets, Consumer<AdjacencyList> consumer) {
        var adjacencyList = adjacencyListFromTargets(idMap, targets);
        consumer.accept(adjacencyList);
        if (adjacencyList instanceof PackedAdjacencyList) {
            ((PackedAdjacencyList) adjacencyList).free();
        }
    }

    static AdjacencyList adjacencyListFromTargets(IdMap idMap, long[] targets) {
        long sourceNodeId = targets[0];

        var relationshipsBuilder = GraphFactory.initRelationshipsBuilder()
            .nodes(idMap)
            .relationshipType(RelationshipType.of("REL"))
            .concurrency(1)
            .executorService(Pools.DEFAULT)
            .build();

        Arrays.stream(targets).forEach(target -> relationshipsBuilder.add(sourceNodeId, target));

        return relationshipsBuilder.build().topology().adjacencyList();
    }

    private static IdMap idMap(long[] targets) {
        var nodesBuilder = GraphFactory.initNodesBuilder()
            .maxOriginalId(targets[targets.length - 1])
            .build();

        Arrays.stream(targets).forEach(nodesBuilder::addNode);

        return nodesBuilder.build().idMap();
    }
}
